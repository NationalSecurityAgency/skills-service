/**
 * Copyright 2020 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package skills.services.admin

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.UIConfigProperties
import skills.controller.request.model.GlobalSettingsRequest
import skills.controller.result.model.ExpiredSkillRes
import skills.controller.result.model.SettingsResult
import skills.controller.result.model.TableResult
import skills.notify.Notifier
import skills.services.FeatureService
import skills.services.LockingService
import skills.services.SkillEventAdminService
import skills.services.attributes.ExpirationAttrs
import skills.services.attributes.SkillAttributeService
import skills.services.settings.SettingsService
import skills.storage.model.Notification
import skills.storage.model.SkillAttributesDef
import skills.storage.model.UserAchievement
import skills.storage.repos.ExpiredUserAchievementRepo
import skills.storage.repos.SkillAttributesDefRepo
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.UserAchievedLevelRepo

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

import static java.time.temporal.TemporalAdjusters.lastDayOfMonth
import static skills.storage.model.SkillAttributesDef.SkillAttributesType.AchievementExpiration

@Service
@Slf4j
class UserAchievementExpirationService {

    public static final String SCHEDULED_SETTING_GROUP = "scheduled"
    public static final String SKILL_EXPIRATION_LAST_RUN_DATE = "skill_expiration_last_run"

    static final String EXPIRATION_WARNING_NOTIFICATION_SENT = "EXPIRATION_WARNING_NOTIFICATION_SENT"

    @Autowired
    SkillAttributesDefRepo skillAttributesDefRepo

    @Autowired
    SkillAttributeService skillAttributeService

    @Autowired
    SkillEventAdminService skillEventAdminService

    @Autowired
    ExpiredUserAchievementRepo expiredUserAchievementRepo

    @Autowired
    UserAchievedLevelRepo userAchievementRepo

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    FeatureService featureService

    @Autowired
    SettingsService settingService

    @Autowired
    LockingService lockingService

    @Autowired
    Notifier notifier

    @Autowired
    UIConfigProperties uiConfigProperties

    @Value('#{"${skills.config.dailySkillExpirationNotificationThreshold:0.1}"}')
    Double dailySkillExpirationNotificationThreshold = 0.1


    TableResult findAllExpiredAchievements(String projectId, String userId, String skillName, PageRequest pageRequest) {
        Page<ExpiredSkillRes> results = expiredUserAchievementRepo.findAllExpiredAchievements(projectId, userId, skillName, pageRequest)
        def totalExpirations = results.getTotalElements()
        def data = results.getContent();
        return new TableResult(data: data, count: data.size(), totalCount: totalExpirations)
    }

    @Transactional
    void expireSkillsAndNotify() {
            lockingService.lockForSkillExpiration()

            final LocalDate todayLd = LocalDate.now()
            final String today = todayLd.format(DateTimeFormatter.BASIC_ISO_DATE)

            SettingsResult expirationLastRan = settingService.getGlobalSetting(SKILL_EXPIRATION_LAST_RUN_DATE, SCHEDULED_SETTING_GROUP)

            if (expirationLastRan && !LocalDate.parse(expirationLastRan.getValue(), DateTimeFormatter.BASIC_ISO_DATE).isBefore(todayLd)) {
                log.info("skill expiration was already run today (potentially by another node), will not run again today")
                return
            }

            try {
                log.info("Checking for expiring user achievements.")
                List<SkillAttributesDef> skillAttributesDefList = getSkillAttributesForExpirationCheck()
                for (SkillAttributesDef skillAttributesDef: skillAttributesDefList) {
                    try {
                        checkAndExpireIfNecessary(skillAttributesDef)
                    } catch (Exception ex) {
                        log.error("Unexpected error expiring skill - ${skillAttributesDef}", ex)
                    }
                }
            } finally {
                GlobalSettingsRequest lastRunSettingRequest = new GlobalSettingsRequest()
                lastRunSettingRequest.value = today
                lastRunSettingRequest.setting = SKILL_EXPIRATION_LAST_RUN_DATE
                lastRunSettingRequest.settingGroup = SCHEDULED_SETTING_GROUP
                settingService.saveSetting(lastRunSettingRequest)
            }
    }

    @Transactional(readOnly = true)
    List<SkillAttributesDef> getSkillAttributesForExpirationCheck() {
        return skillAttributesDefRepo.findAllByType(AchievementExpiration)
    }

    @Transactional
    void checkAndExpireIfNecessary(SkillAttributesDef skillAttributesDef) {
        LocalDateTime now = LocalDateTime.now()
        ExpirationAttrs expirationAttrs = skillAttributeService.convertAttrs(skillAttributesDef, ExpirationAttrs)
        if (expirationAttrs.expirationType == ExpirationAttrs.YEARLY || expirationAttrs.expirationType == ExpirationAttrs.MONTHLY) {
            LocalDateTime nextExpirationDate = expirationAttrs.nextExpirationDate.toLocalDateTime()
            if (nextExpirationDate.isBefore(now)) {
                List<String> usersWithExpiredAchievements = []
                if (expirationAttrs.emailNotificationsEnabled) {
                    // if notification is enabled, get users with expired achievements before moving them to expired
                    usersWithExpiredAchievements = expiredUserAchievementRepo.findUserIdsWithSkillRefId(skillAttributesDef.skillRefId)
                }
                expireAchievementsForSkill(skillAttributesDef.skillRefId)

                // update nextExpirationDate
                expirationAttrs.nextExpirationDate = getNextExpirationDate(expirationAttrs)?.toDate()
                skillAttributesDef.attributes = skillAttributeService.mapper.writeValueAsString(expirationAttrs)
                skillAttributesDefRepo.save(skillAttributesDef)

                // notify users about expired achievements
                if (expirationAttrs.emailNotificationsEnabled && featureService.isEmailServiceFeatureEnabled()) {
                    SkillDefRepo.SkillProjectAndSubjectIdsAndNames skillProjectAndSubjectIdsAndNames = skillDefRepo.getSkillProjectAndSubjectIdsAndNamesBySkillRefId(skillAttributesDef.skillRefId)
                    usersWithExpiredAchievements.each { userId ->
                        String publicUrl = featureService.getPublicUrl()
                        Notifier.NotificationRequest request = new Notifier.NotificationRequest(
                                userIds: [userId],
                                type: Notification.Type.SkillExpiration.toString(),
                                keyValParams: [
                                        skillName        : skillProjectAndSubjectIdsAndNames.skillName,
                                        projectName      : skillProjectAndSubjectIdsAndNames.projectName,
                                        expirationType   : expirationAttrs.expirationType,
                                        expirationDate   : nextExpirationDate.format(DateTimeFormatter.ISO_DATE),
                                        skillTrainingUrl : "${publicUrl}progress-and-rankings/projects/${skillProjectAndSubjectIdsAndNames.projectId}/subjects/${skillProjectAndSubjectIdsAndNames.subjectId}/skills/${skillProjectAndSubjectIdsAndNames.skillId}",
                                        communityHeaderDescriptor: uiConfigProperties.ui.defaultCommunityDescriptor
                                ]
                        )
                        log.info("Sending skill expiration notification to user [${userId}] for skill [${skillProjectAndSubjectIdsAndNames.skillId}]")
                        notifier.sendNotification(request)
                    }
                }
            }
        } else if (expirationAttrs.expirationType == ExpirationAttrs.DAILY) {
            LocalDateTime achievedOnOlderThan = now.minusDays(expirationAttrs.every)
            expireAchievementsForSkillAchievedBefore(skillAttributesDef.skillRefId, achievedOnOlderThan?.toDate())

            // notify users about expiring achievements
            if (expirationAttrs.emailNotificationsEnabled && featureService.isEmailServiceFeatureEnabled()) {
                SkillDefRepo.SkillProjectAndSubjectIdsAndNames skillProjectAndSubjectIdsAndNames = skillDefRepo.getSkillProjectAndSubjectIdsAndNamesBySkillRefId(skillAttributesDef.skillRefId)
                // For daily expiration, find achievements that will expire within N days
                int days = calculateNotificationDays(expirationAttrs.every)
                LocalDateTime expirationThreshold = achievedOnOlderThan.plusDays(days)
                List<UserAchievement> achievementsThatWillExpire = expiredUserAchievementRepo.findUserAchievementsBySkillRefIdWithMostRecentUserPerformedSkillBefore(
                    skillAttributesDef.skillRefId, expirationThreshold.toDate()
                )
                achievementsThatWillExpire.each { achievement ->
                    if (achievement.expirationNotificationState != EXPIRATION_WARNING_NOTIFICATION_SENT) {
                        String publicUrl = featureService.getPublicUrl()
                        LocalDateTime retentionDeadline = achievedOnOlderThan.plusDays(expirationAttrs.every)
                        int daysUntilExpiration = Math.abs(ChronoUnit.DAYS.between(now, retentionDeadline))
                        
                        Notifier.NotificationRequest request = new Notifier.NotificationRequest(
                                userIds: [achievement.userId],
                                type: Notification.Type.SkillDailyExpirationWarning.toString(),
                                keyValParams: [
                                        skillName           : skillProjectAndSubjectIdsAndNames.skillName,
                                        projectName         : skillProjectAndSubjectIdsAndNames.projectName,
                                        retentionDeadline   : retentionDeadline.format(DateTimeFormatter.ISO_DATE),
                                        daysUntilExpiration : "${daysUntilExpiration}",
                                        skillTrainingUrl    : "${publicUrl}progress-and-rankings/projects/${skillProjectAndSubjectIdsAndNames.projectId}/subjects/${skillProjectAndSubjectIdsAndNames.subjectId}/skills/${skillProjectAndSubjectIdsAndNames.skillId}",
                                        communityHeaderDescriptor: uiConfigProperties.ui.defaultCommunityDescriptor
                                ]
                        )
                        log.info("Sending daily skill expiration warning to user [${achievement.userId}] for skill [${achievement.skillId}]")
                        notifier.sendNotification(request)

                        achievement.expirationNotificationState = EXPIRATION_WARNING_NOTIFICATION_SENT
                        userAchievementRepo.save(achievement)
                    }
                }
            }
        } else if (expirationAttrs.expirationType != ExpirationAttrs.NEVER) {
            log.error("Unexpected expirationType [${expirationAttrs?.expirationType}] - ${expirationAttrs}")
        }
    }

    private void expireAchievementsForSkill(Integer skillRefId) {
        // move user_achievements for skillRefId to expired_user_achievements
        expiredUserAchievementRepo.expireAchievementsForSkill(skillRefId)

        // remove all skill events for this skill
        skillEventAdminService.deleteAllSkillEventsForSkill(skillRefId)
    }

    private void expireAchievementsForSkillAchievedBefore(Integer skillRefId, Date expirationDate) {
        // find any UserAchievement's for this skill where the most recent associated UserPerformedSkill.performedOn is older than the expiration date
        List<UserAchievement> expiredUserAchievements = expiredUserAchievementRepo.findUserAchievementsBySkillRefIdWithMostRecentUserPerformedSkillBefore(skillRefId, expirationDate)

        expiredUserAchievements.each { ua ->
            // move this user_achievement record to the expired_user_achievements table
            expiredUserAchievementRepo.expireAchievementById(ua.id)
            // remove all skill events for this skill and user
            skillEventAdminService.deleteAllSkillEventsForSkillAndUser(skillRefId, ua.userId)
        }
    }

    private LocalDateTime getNextExpirationDate(ExpirationAttrs expirationAttrs) {
        LocalDateTime nextExpirationDate
        LocalDateTime existingNextExpDate = expirationAttrs.nextExpirationDate.toLocalDateTime()
        if (expirationAttrs.expirationType == ExpirationAttrs.YEARLY) {
            nextExpirationDate = existingNextExpDate.plusYears(expirationAttrs.every)
        } else if (expirationAttrs.expirationType == ExpirationAttrs.MONTHLY) {
            nextExpirationDate = existingNextExpDate.plusMonths(expirationAttrs.every)
            if (expirationAttrs.monthlyDay == ExpirationAttrs.LAST_DAY_OF_MONTH) {
                nextExpirationDate = nextExpirationDate.with (lastDayOfMonth())
            }
        }
        nextExpirationDate
    }

    private int calculateNotificationDays(int daysBeforeExpiring) {
        int notificationDays = (int) Math.round(daysBeforeExpiring * dailySkillExpirationNotificationThreshold)
        return Math.max(1, Math.min(7, notificationDays))
    }
}
