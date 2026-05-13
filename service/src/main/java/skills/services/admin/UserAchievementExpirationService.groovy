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
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import skills.UIConfigProperties
import skills.auth.UserAuthService
import skills.controller.result.model.ExpiredSkillRes
import skills.controller.result.model.TableResult
import skills.notify.Notifier
import skills.services.FeatureService
import skills.services.SkillEventAdminService
import skills.services.attributes.ExpirationAttrs
import skills.services.attributes.SkillAttributeService
import skills.storage.model.Notification
import skills.storage.model.SkillAttributesDef
import skills.storage.model.UserAchievement
import skills.storage.model.UserPerformedSkill
import skills.storage.model.auth.RoleName
import skills.storage.model.auth.UserRole
import skills.storage.repos.*
import skills.tasks.config.TaskConfig

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

import static java.time.temporal.TemporalAdjusters.lastDayOfMonth
import static skills.storage.model.SkillAttributesDef.SkillAttributesType.AchievementExpiration

@Service
@Slf4j
class UserAchievementExpirationService {

    static final String EXPIRATION_WARNING_NOTIFICATION_SENT = "EXPIRATION_WARNING_NOTIFICATION_SENT"

    @Autowired
    SkillAttributesDefRepo skillAttributesDefRepo

    @Autowired
    SkillAttributeService skillAttributeService

    @Autowired
    SkillEventAdminService skillEventAdminService

    @Autowired
    InviteOnlyProjectService inviteOnlyProjectService

    @Autowired
    @Lazy
    UserAuthService userAuthService

    @Autowired
    @Lazy
    UserCommunityService userCommunityService

    @Autowired
    UserRoleRepo userRoleRepo

    @Autowired
    ExpiredUserAchievementRepo expiredUserAchievementRepo

    @Autowired
    UserAchievedLevelRepo userAchievementRepo

    @Autowired
    UserPerformedSkillRepo userPerformedSkillRepo

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    TaskConfig taskConfig

    @Autowired
    FeatureService featureService

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

    @Transactional(readOnly = true)
    List<SkillAttributesDef> getSkillAttributesForExpirationCheck() {
        return skillAttributesDefRepo.findAllByType(AchievementExpiration)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void checkAndExpireIfNecessary(SkillAttributesDef skillAttributesDef) {
        LocalDateTime now = LocalDateTime.now()
        ExpirationAttrs expirationAttrs = skillAttributeService.convertAttrs(skillAttributesDef, ExpirationAttrs)
        log.info("Checking expiration for skill: ${skillAttributesDef.skillRefId}, expiration attributes: ${expirationAttrs}")
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
                log.info("Expired achievements for skill: ${skillAttributesDef.skillRefId}, next expiration date: ${expirationAttrs.nextExpirationDate}")

                // notify users about expired achievements
                if (expirationAttrs.emailNotificationsEnabled && featureService.isEmailServiceFeatureEnabled()) {
                    SkillDefRepo.SkillProjectAndSubjectIdsAndNames skillProjectAndSubjectIdsAndNames = skillDefRepo.getSkillProjectAndSubjectIdsAndNamesBySkillRefId(skillAttributesDef.skillRefId)
                    log.info("Sending expiration emails for [${usersWithExpiredAchievements.size()}] users for skill: ${skillAttributesDef.skillRefId}")
                    usersWithExpiredAchievements.each { userId ->
                        if (!isUserStillPermitted(skillProjectAndSubjectIdsAndNames.projectId, userId)) {
                            return
                        }
                        String publicUrl = featureService.getPublicUrl()
                        Notifier.NotificationRequest request = new Notifier.NotificationRequest(
                                userIds: [userId],
                                type: Notification.Type.SkillExpiration.toString(),
                                keyValParams: [
                                        skillName        : skillProjectAndSubjectIdsAndNames.skillName,
                                        projectName      : skillProjectAndSubjectIdsAndNames.projectName,
                                        expirationType   : expirationAttrs.expirationType,
                                        expirationDate   : formatWithOrdinal(nextExpirationDate),
                                        skillTrainingUrl : buildSkillTrainingUrl(publicUrl, skillProjectAndSubjectIdsAndNames),
                                        contactProjectUrl: "${publicUrl}progress-and-rankings/projects/${skillProjectAndSubjectIdsAndNames.projectId}?openContact=true",
                                        communityHeaderDescriptor: uiConfigProperties.ui.defaultCommunityDescriptor
                                ]
                        )
                        log.info("Sending skill expiration notification to user [${userId}] for skill [${skillProjectAndSubjectIdsAndNames.skillId}] - expiring on [${nextExpirationDate.format(DateTimeFormatter.ISO_DATE)}]")
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
                log.info("Sending warning emails for [${achievementsThatWillExpire.size()}] achievements for skill: ${skillAttributesDef.skillRefId}")
                achievementsThatWillExpire.each { UserAchievement achievement ->
                    if (achievement.expirationNotificationState != EXPIRATION_WARNING_NOTIFICATION_SENT && isUserStillPermitted(achievement.projectId, achievement.userId)) {
                        String publicUrl = featureService.getPublicUrl()
                        UserPerformedSkill mostRecentUPS = userPerformedSkillRepo.findTopBySkillRefIdAndUserIdOrderByPerformedOnDesc(skillAttributesDef.skillRefId, achievement.userId)
                        LocalDateTime retentionDeadline = LocalDateTime.ofInstant((mostRecentUPS.performedOn + expirationAttrs.every).toInstant(), ZoneId.systemDefault())
                        Notifier.NotificationRequest request = new Notifier.NotificationRequest(
                                userIds: [achievement.userId],
                                type: Notification.Type.SkillDailyExpirationWarning.toString(),
                                keyValParams: [
                                        skillName           : skillProjectAndSubjectIdsAndNames.skillName,
                                        projectName         : skillProjectAndSubjectIdsAndNames.projectName,
                                        retentionDeadline   : formatWithOrdinal(retentionDeadline),
                                        skillTrainingUrl    : buildSkillTrainingUrl(publicUrl, skillProjectAndSubjectIdsAndNames),
                                        contactProjectUrl   : "${publicUrl}progress-and-rankings/projects/${skillProjectAndSubjectIdsAndNames.projectId}?openContact=true",
                                        communityHeaderDescriptor: uiConfigProperties.ui.defaultCommunityDescriptor
                                ]
                        )
                        log.info("Sending daily skill expiration warning to user [${achievement.userId}] for skill [${achievement.skillId}] - expiring on [${retentionDeadline.format(DateTimeFormatter.ISO_DATE)}]")
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

        log.info("Expiring [${expiredUserAchievements.size()}] achievements for skill: ${skillRefId}, expiration date: ${expirationDate}")
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

    private Boolean isUserStillPermitted(String projectId, String userId) {
        Boolean isInviteOnly = inviteOnlyProjectService.isInviteOnlyProject(projectId)
        if (isInviteOnly) {
            List<UserRole> userRoles = userRoleRepo.findAllByUserId(userId?.toLowerCase())
            Boolean isAuthorized = userRoles.find { it.roleName == RoleName.ROLE_SUPER_DUPER_USER || (it.projectId  && projectId.equalsIgnoreCase(projectId) && it.roleName in [RoleName.ROLE_PRIVATE_PROJECT_USER, RoleName.ROLE_PROJECT_ADMIN, RoleName.ROLE_PROJECT_APPROVER])}
            if (!isAuthorized) {
                log.warn("User [${userId}] is not authorized to access private project [${projectId}]")
                return false
            }
        }

        Boolean isUserCommunityOnlyProject = userCommunityService.isUserCommunityOnlyProject(projectId)
        Boolean belongsToUserCommunity = userCommunityService.isUserCommunityMember(userId)
        if (isUserCommunityOnlyProject && !belongsToUserCommunity) {
            log.warn("User [${userId}] is not a member of the user community for project [${projectId}]")
            return false
        }
        return true
    }

    private static String buildSkillTrainingUrl(String publicUrl, SkillDefRepo.SkillProjectAndSubjectIdsAndNames skillInfo) {
        String baseUrl = "${publicUrl}progress-and-rankings/projects/${skillInfo.projectId}/subjects/${skillInfo.subjectId}"
        if (skillInfo.groupId) {
            return "${baseUrl}/groups/${skillInfo.groupId}/skills/${skillInfo.skillId}"
        } else {
            return "${baseUrl}/skills/${skillInfo.skillId}"
        }
    }

    static String formatWithOrdinal(LocalDateTime dateTime) {
        int day = dateTime.dayOfMonth
        String suffix
        if (day >= 11 && day <= 13) {
            suffix = "th"
        } else {
            switch (day % 10) {
                case 1: suffix = "st"; break
                case 2: suffix = "nd"; break
                case 3: suffix = "rd"; break
                default: suffix = "th"; break
            }
        }
        return dateTime.format(DateTimeFormatter.ofPattern("MMMM d'" + suffix + "', yyyy"))
    }

}
