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
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.result.model.ExpiredSkillRes
import skills.controller.result.model.TableResult
import skills.services.SkillEventAdminService
import skills.services.attributes.ExpirationAttrs
import skills.services.attributes.SkillAttributeService
import skills.storage.model.ExpiredUserAchievement
import skills.storage.model.SkillAttributesDef
import skills.storage.model.UserAchievement
import skills.storage.repos.ExpiredUserAchievementRepo
import skills.storage.repos.SkillAttributesDefRepo

import java.time.LocalDateTime

import static java.time.temporal.TemporalAdjusters.lastDayOfMonth
import static skills.storage.model.SkillAttributesDef.SkillAttributesType.AchievementExpiration

@Service
@Slf4j
class UserAchievementExpirationService {

    @Autowired
    SkillAttributesDefRepo skillAttributesDefRepo

    @Autowired
    SkillAttributeService skillAttributeService

    @Autowired
    SkillEventAdminService skillEventAdminService

    @Autowired
    ExpiredUserAchievementRepo expiredUserAchievementRepo

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

    @Transactional
    void checkAndExpireIfNecessary(SkillAttributesDef skillAttributesDef) {
        LocalDateTime now = LocalDateTime.now()
        ExpirationAttrs expirationAttrs = skillAttributeService.convertAttrs(skillAttributesDef, ExpirationAttrs)
        if (expirationAttrs.expirationType == ExpirationAttrs.YEARLY || expirationAttrs.expirationType == ExpirationAttrs.MONTHLY) {
            LocalDateTime nextExpirationDate = expirationAttrs.nextExpirationDate.toLocalDateTime()
            if (nextExpirationDate.isBefore(now)) {
                expireAchievementsForSkill(skillAttributesDef.skillRefId)

                // update nextExpirationDate
                expirationAttrs.nextExpirationDate = getNextExpirationDate(expirationAttrs)?.toDate()
                skillAttributesDef.attributes = skillAttributeService.mapper.writeValueAsString(expirationAttrs)
                skillAttributesDefRepo.save(skillAttributesDef)
            }
        } else if (expirationAttrs.expirationType == ExpirationAttrs.DAILY) {
            LocalDateTime achievedOnOlderThan = now.minusDays(expirationAttrs.every)
            expireAchievementsForSkillAchievedBefore(skillAttributesDef.skillRefId, achievedOnOlderThan?.toDate())
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
}
