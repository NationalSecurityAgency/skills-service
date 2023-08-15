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
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.services.SkillEventAdminService
import skills.services.attributes.ExpirationAttrs
import skills.services.attributes.SkillAttributeService
import skills.storage.model.SkillAttributesDef
import skills.storage.repos.SkillAttributesDefRepo
import skills.storage.repos.UserAchievedLevelRepo

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
    UserAchievedLevelRepo userAchievedLevelRepo

    @Transactional()
    void removeExpiredUserAchievements() {
        List<SkillAttributesDef> skillAttributesDefList = skillAttributesDefRepo.findAllByType(AchievementExpiration)
        skillAttributesDefList.each {skillAttributesDef ->
            try {
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
//                } else if (expirationAttrs.expirationType == ExpirationAttrs.DAILY) {
//                    LocalDateTime achievedOnOlderThan = now.minusDays(expirationAttrs.every)
//                    expireAchievementsForSkillAchievedBefore(skillAttributesDef.skillRefId, achievedOnOlderThan?.toDate())
                } else if (expirationAttrs.expirationType != ExpirationAttrs.NEVER) {
                    log.error("Unexpected expirationType [${expirationAttrs?.expirationType}] - ${expirationAttrs}")
                }
            } catch (Exception ex) {
                log.error("Unexpected error expiring skill - ${skillAttributesDef}", ex)
            }
        }
    }

    private void expireAchievementsForSkill(Integer skillRefId) {
        // move user_achievements for skillRefId to expired_user_achievements
        userAchievedLevelRepo.expireAchievementsForSkill(skillRefId)

        // remove all skill events for this skill
        skillEventAdminService.deleteAllSkillEventsForSkill(skillRefId)
    }

    private void expireAchievementsForSkillAchievedBefore(Integer skillRefId, Date expirationDate) {
        // move user_achievements for skillRefId where achieved_on < expirationDate to expired_user_achievements
        userAchievedLevelRepo.expireAchievementsForSkillAchievedBefore(skillRefId, expirationDate)

        // remove all skill events for this skill performed before the expiration date
        skillEventAdminService.deleteAllSkillEventsForSkillPerformedBefore(skillRefId, expirationDate)
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
