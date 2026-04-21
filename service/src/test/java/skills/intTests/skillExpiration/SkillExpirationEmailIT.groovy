/**
 * Copyright 2026 SkillTree
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
package skills.intTests.skillExpiration

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import skills.SpringBootApp
import skills.intTests.inviteOnly.InviteOnlyBaseSpec
import skills.intTests.utils.EmailUtils
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.services.admin.UserAchievementExpirationService
import skills.services.attributes.ExpirationAttrs
import skills.services.attributes.SkillAttributeService
import skills.settings.EmailSettingsService
import skills.storage.model.SkillAttributesDef
import skills.storage.model.UserAchievement
import skills.storage.model.UserAttrs
import skills.tasks.config.TaskConfig
import skills.tasks.executors.ExpireUserAchievementsTaskExecutor
import skills.utils.WaitFor

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.regex.Pattern

import static skills.services.admin.UserAchievementExpirationService.getEXPIRATION_WARNING_NOTIFICATION_SENT

@SpringBootTest(properties = ['skills.config.expirationGracePeriod=0'],
        webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, classes = SpringBootApp)
class SkillExpirationEmailIT extends InviteOnlyBaseSpec {

    @Autowired
    UserAchievementExpirationService userAchievementExpirationService

    @Autowired
    ExpireUserAchievementsTaskExecutor expireUserAchievementsTaskExecutor

    @Autowired
    SkillAttributeService skillAttributeService

    @Autowired
    TaskConfig taskConfig

    @Autowired
    JdbcTemplate jdbcTemplate

    def "send expiration warning email notifications for DAILY"() {
        Map settingRequest = [
                settingGroup : EmailSettingsService.settingsGroup,
                setting : EmailSettingsService.htmlHeader,
                value : 'For {{ community.descriptor }} Only'
        ]
        SkillsService rootSkillsService = createRootSkillService()
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)
        settingRequest.setting = EmailSettingsService.plaintextHeader
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)
        settingRequest.setting = EmailSettingsService.htmlFooter
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)
        settingRequest.setting = EmailSettingsService.plaintextFooter
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)

        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(3, 1, 1, 100)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        String userId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID]).first()
        Date achievementDate = new Date() - 45 // 45 days ago

        // Configure skill expiration
        skillsService.saveSkillExpirationAttributes(proj.projectId, skills[0].skillId.toString(), [
                expirationType: ExpirationAttrs.DAILY,
                every: 47,
                emailNotificationsEnabled: true
        ])

        // Create user achievement
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId.toString()], userId, achievementDate)

        // Set expiration notification state to NONE
        UserAchievement achievement = userAchievedRepo.findAllByUserIdAndSkillId(userId, skills[0].skillId.toString())[0]
        achievement.expirationNotificationState = 'NONE'
        userAchievedRepo.save(achievement)

        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(userId)

        LocalDateTime now = LocalDateTime.now()
        LocalDateTime retentionDeadline = LocalDateTime.ofInstant(taskConfig.getExpireUserAchievementsTaskExecutionTime((achievementDate + 47).toInstant()), ZoneId.systemDefault())
        int daysUntilExpiration = Math.abs(ChronoUnit.DAYS.between(now, retentionDeadline))

        when:
        expireUserAchievementsTaskExecutor.removeExpiredUserAchievements()

        assert WaitFor.wait { greenMail.getReceivedMessages().size() == 1 }
        List<EmailUtils.EmailRes> emails = EmailUtils.getEmails(greenMail)

        Pattern plaintTextMatch = ~/(?s)For All Dragons Only.*Hello.*This is a reminder that your achievement for the skill ${skills[0].name} in project ${proj.name} is approaching its expiration deadline.*Skill Details:.*- Skill: ${skills[0].name}.*- Project: ${proj.name}.*- Retention Deadline: \d{4}-\d{2}-\d{2}.*- Days Until Expiration: ${daysUntilExpiration}.*For All Dragons Only/

        Pattern h1 = ~/(?s)For All Dragons Only.*<h1>Action Required: Retain Your Skill Achievement<\/h1>.+?/
        Pattern p1 = ~/(?s)<p>This is a reminder that your achievement for the skill <strong>${skills[0].name}<\/strong> in project <strong>${proj.name}<\/strong> is approaching its expiration deadline.<\/p>.+?/
        Pattern p2 = ~/(?s)<strong>Skill:<\/strong>.*${skills[0].name}.*<strong>Project:<\/strong>.*${proj.name}.*<strong>Retention Deadline:<\/strong>.*${retentionDeadline.format(DateTimeFormatter.ofPattern('yyyy-MM-dd'))}/

        then:
        emails.size() == 1
        emails[0].recipients == [userAttrs.email]
        emails[0].subj == "Skill Achievement Expiring Soon"
        plaintTextMatch.matcher(emails[0].plainText).find()
        h1.matcher(emails[0].html).find()
        p1.matcher(emails[0].html).find()
        p2.matcher(emails[0].html).find()

        // Verify notification state was updated
        UserAchievement updatedAchievement = userAchievedRepo.findAllByUserIdAndSkillId(userId, skills[0].skillId.toString())[0]
        updatedAchievement.expirationNotificationState == EXPIRATION_WARNING_NOTIFICATION_SENT
    }

    def "send expiration warning email notifications for user community protected project"() {
        Map settingRequest = [
                settingGroup : EmailSettingsService.settingsGroup,
                setting : EmailSettingsService.htmlHeader,
                value : 'For {{ community.descriptor }} Only'
        ]
        SkillsService rootSkillsService = createRootSkillService()
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)
        settingRequest.setting = EmailSettingsService.plaintextHeader
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)
        settingRequest.setting = EmailSettingsService.htmlFooter
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)
        settingRequest.setting = EmailSettingsService.plaintextFooter
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)

        List<String> users = getRandomUsers(2, false, ['skills@skills.org', DEFAULT_ROOT_USER_ID])
        SkillsService divineDragonUser1SkillsService = createService(users.first())
        SkillsService divineDragonUser2SkillsService = createService(users.last())
        divineDragonUser1SkillsService.getPublicConfigs() // will create user_attrs entry
        divineDragonUser2SkillsService.getPublicConfigs() // will create user_attrs entry
        rootSkillsService.saveUserTag(divineDragonUser1SkillsService.userName, 'dragons', ['DivineDragon'])
        rootSkillsService.saveUserTag(divineDragonUser2SkillsService.userName, 'dragons', ['DivineDragon'])

        def proj = SkillsFactory.createProject()
        proj.enableProtectedUserCommunity = true
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(3, 1, 1, 100)

        divineDragonUser1SkillsService.createProject(proj)
        divineDragonUser1SkillsService.createSubject(subj)
        divineDragonUser1SkillsService.createSkills(skills)

        String userId = divineDragonUser1SkillsService.userName
        Date achievementDate = new Date() - 6 // 6 days ago

        // Configure skill expiration
        divineDragonUser1SkillsService.saveSkillExpirationAttributes(proj.projectId, skills[0].skillId.toString(), [
                expirationType: ExpirationAttrs.DAILY,
                every: 7,
                emailNotificationsEnabled: true
        ])

        // Create user achievement
        divineDragonUser1SkillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId.toString()], userId, achievementDate)

        // Set expiration notification state to NONE
        UserAchievement achievement = userAchievedRepo.findAllByUserIdAndSkillId(userId, skills[0].skillId.toString())[0]
        achievement.expirationNotificationState = 'NONE'
        userAchievedRepo.save(achievement)

        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(userId)

        when:
        expireUserAchievementsTaskExecutor.removeExpiredUserAchievements()

        assert WaitFor.wait { greenMail.getReceivedMessages().size() == 1 }
        List<EmailUtils.EmailRes> emails = EmailUtils.getEmails(greenMail)

        Pattern plaintTextMatch = ~/(?s)For All Dragons Only.*Hello.*This is a reminder that your achievement for the skill ${skills[0].name} in project ${proj.name} is approaching its expiration deadline.*Skill Details:.*- Skill: ${skills[0].name}.*- Project: ${proj.name}.*- Retention Deadline: \d{4}-\d{2}-\d{2}.*- Days Until Expiration:.*For All Dragons Only/

        Pattern h1 = ~/(?s)For All Dragons Only.*<h1>Action Required: Retain Your Skill Achievement<\/h1>.+?/
        Pattern p1 = ~/(?s)<p>This is a reminder that your achievement for the skill <strong>${skills[0].name}<\/strong> in project <strong>${proj.name}<\/strong> is approaching its expiration deadline.<\/p>.+?/
        Pattern p2 = ~/(?s)<strong>Skill:<\/strong>.*${skills[0].name}.*<strong>Project:<\/strong>.*${proj.name}.*<strong>Retention Deadline:<\/strong>.*\d{4}-\d{2}-\d{2}/

        then:
        emails.size() == 1
        emails[0].recipients == [userAttrs.email]
        emails[0].subj == "Skill Achievement Expiring Soon"
        plaintTextMatch.matcher(emails[0].plainText).find()
        h1.matcher(emails[0].html).find()
        p1.matcher(emails[0].html).find()
        p2.matcher(emails[0].html).find()

        // Verify notification state was updated
        UserAchievement updatedAchievement = userAchievedRepo.findAllByUserIdAndSkillId(userId, skills[0].skillId.toString())[0]
        updatedAchievement.expirationNotificationState == EXPIRATION_WARNING_NOTIFICATION_SENT
    }

    def "do not send expiration warning email notifications for user community protected project if user is not a member of the protected community any more"() {
        Map settingRequest = [
                settingGroup : EmailSettingsService.settingsGroup,
                setting : EmailSettingsService.htmlHeader,
                value : 'For {{ community.descriptor }} Only'
        ]
        SkillsService rootSkillsService = createRootSkillService()
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)
        settingRequest.setting = EmailSettingsService.plaintextHeader
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)
        settingRequest.setting = EmailSettingsService.htmlFooter
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)
        settingRequest.setting = EmailSettingsService.plaintextFooter
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)

        List<String> users = getRandomUsers(2, false, ['skills@skills.org', DEFAULT_ROOT_USER_ID])
        SkillsService divineDragonUser1SkillsService = createService(users.first())
        SkillsService divineDragonUser2SkillsService = createService(users.last())
        divineDragonUser1SkillsService.getPublicConfigs() // will create user_attrs entry
        divineDragonUser2SkillsService.getPublicConfigs() // will create user_attrs entry
        rootSkillsService.saveUserTag(divineDragonUser1SkillsService.userName, 'dragons', ['DivineDragon'])
        rootSkillsService.saveUserTag(divineDragonUser2SkillsService.userName, 'dragons', ['DivineDragon'])

        def proj = SkillsFactory.createProject()
        proj.enableProtectedUserCommunity = true
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(3, 1, 1, 100)

        divineDragonUser1SkillsService.createProject(proj)
        divineDragonUser1SkillsService.createSubject(subj)
        divineDragonUser1SkillsService.createSkills(skills)

        String userId = divineDragonUser1SkillsService.userName
        Date achievementDate = new Date() - 6 // 6 days ago

        // Configure skill expiration
        divineDragonUser1SkillsService.saveSkillExpirationAttributes(proj.projectId, skills[0].skillId.toString(), [
                expirationType: ExpirationAttrs.DAILY,
                every: 7,
                emailNotificationsEnabled: true
        ])

        // Create user achievement
        divineDragonUser1SkillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId.toString()], userId, achievementDate)

        // Set expiration notification state to NONE
        UserAchievement achievement = userAchievedRepo.findAllByUserIdAndSkillId(userId, skills[0].skillId.toString())[0]
        achievement.expirationNotificationState = 'NONE'
        userAchievedRepo.save(achievement)

        jdbcTemplate.execute("delete from user_tags where user_id='${userId}' and key='dragons'")

        when:
        expireUserAchievementsTaskExecutor.removeExpiredUserAchievements()

        then:
        greenMail.getReceivedMessages().size() == 0
    }

    def "send skill expired email notifications for user community protected project"() {
        Map settingRequest = [
                settingGroup : EmailSettingsService.settingsGroup,
                setting : EmailSettingsService.htmlHeader,
                value : 'For {{ community.descriptor }} Only'
        ]
        SkillsService rootSkillsService = createRootSkillService()
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)
        settingRequest.setting = EmailSettingsService.plaintextHeader
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)
        settingRequest.setting = EmailSettingsService.htmlFooter
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)
        settingRequest.setting = EmailSettingsService.plaintextFooter
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)


        List<String> users = getRandomUsers(2, false, ['skills@skills.org', DEFAULT_ROOT_USER_ID])
        SkillsService divineDragonUser1SkillsService = createService(users.first())
        SkillsService divineDragonUser2SkillsService = createService(users.last())
        divineDragonUser1SkillsService.getPublicConfigs() // will create user_attrs entry
        divineDragonUser2SkillsService.getPublicConfigs() // will create user_attrs entry
        rootSkillsService.saveUserTag(divineDragonUser1SkillsService.userName, 'dragons', ['DivineDragon'])
        rootSkillsService.saveUserTag(divineDragonUser2SkillsService.userName, 'dragons', ['DivineDragon'])

        def proj = SkillsFactory.createProject()
        proj.enableProtectedUserCommunity = true
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(3, 1, 1, 100)

        divineDragonUser1SkillsService.createProject(proj)
        divineDragonUser1SkillsService.createSubject(subj)
        divineDragonUser1SkillsService.createSkills(skills)

        String userId = divineDragonUser1SkillsService.userName
        Date achievementDate = new Date() - 10 // 10 days ago

        // Configure skill expiration
        LocalDateTime expirationDate = (new Date()-1).toLocalDateTime() // yesterday
        divineDragonUser1SkillsService.saveSkillExpirationAttributes(proj.projectId, skills[0].skillId.toString(), [
                expirationType: ExpirationAttrs.MONTHLY,
                every: 1,
                monthlyDay: expirationDate.dayOfMonth,
                nextExpirationDate: expirationDate.toDate(),
                emailNotificationsEnabled: true
        ])

        // Create user achievement
        divineDragonUser1SkillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId.toString()], userId, achievementDate)

        // Set expiration notification state to EXPIRATION_WARNING_NOTIFICATION_SENT
        UserAchievement achievement = userAchievedRepo.findAllByUserIdAndSkillId(userId, skills[0].skillId.toString())[0]
        achievement.expirationNotificationState = EXPIRATION_WARNING_NOTIFICATION_SENT
        userAchievedRepo.save(achievement)

        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(userId)

        when:
        expireUserAchievementsTaskExecutor.removeExpiredUserAchievements()

        assert WaitFor.wait { greenMail.getReceivedMessages().size() == 1 }
        List<EmailUtils.EmailRes> emails = EmailUtils.getEmails(greenMail)

        Pattern plaintTextMatch = ~/(?s)For All Dragons Only.*Hello.*We're writing to inform you that your achievement for the skill ${skills[0].name} in project ${proj.name} has expired.*Expiration Details:.*- Skill: ${skills[0].name}.*- Project: ${proj.name}.*- Expired On: \d{4}-\d{2}-\d{2}.*For All Dragons Only/

        Pattern h1 = ~/(?s)For All Dragons Only.*<h1>Your Skill Achievement Has Expired<\/h1>.+?/
        Pattern p1 = ~/(?s)<p>We're writing to inform you that your achievement for the skill <strong>${skills[0].name}<\/strong> in project <strong>${proj.name}<\/strong> has expired.<\/p>.+?/
        Pattern p2 = ~/(?s)<strong>Skill:<\/strong>.*${skills[0].name}.*<strong>Project:<\/strong>.*${proj.name}.*<strong>Expired On:<\/strong>.*\d{4}-\d{2}-\d{2}/

        then:
        emails.size() == 1
        emails[0].recipients == [userAttrs.email]
        emails[0].subj == "Your Skill Achievement Has Expired"
        plaintTextMatch.matcher(emails[0].plainText).find()
        h1.matcher(emails[0].html).find()
        p1.matcher(emails[0].html).find()
        p2.matcher(emails[0].html).find()

        // Verify notification state was updated
        UserAchievement updatedAchievement = userAchievedRepo.findAllByUserIdAndSkillId(userId, skills[0].skillId.toString())[0]
    }

    def "do not send skill expired email notifications for user community protected project if user is not a member of the protected community any more"() {
        Map settingRequest = [
                settingGroup : EmailSettingsService.settingsGroup,
                setting : EmailSettingsService.htmlHeader,
                value : 'For {{ community.descriptor }} Only'
        ]
        SkillsService rootSkillsService = createRootSkillService()
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)
        settingRequest.setting = EmailSettingsService.plaintextHeader
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)
        settingRequest.setting = EmailSettingsService.htmlFooter
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)
        settingRequest.setting = EmailSettingsService.plaintextFooter
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)

        List<String> users = getRandomUsers(2, false, ['skills@skills.org', DEFAULT_ROOT_USER_ID])
        SkillsService divineDragonUser1SkillsService = createService(users.first())
        SkillsService divineDragonUser2SkillsService = createService(users.last())
        divineDragonUser1SkillsService.getPublicConfigs() // will create user_attrs entry
        divineDragonUser2SkillsService.getPublicConfigs() // will create user_attrs entry
        rootSkillsService.saveUserTag(divineDragonUser1SkillsService.userName, 'dragons', ['DivineDragon'])
        rootSkillsService.saveUserTag(divineDragonUser2SkillsService.userName, 'dragons', ['DivineDragon'])

        def proj = SkillsFactory.createProject()
        proj.enableProtectedUserCommunity = true
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(3, 1, 1, 100)

        divineDragonUser1SkillsService.createProject(proj)
        divineDragonUser1SkillsService.createSubject(subj)
        divineDragonUser1SkillsService.createSkills(skills)

        String userId = divineDragonUser1SkillsService.userName
        Date achievementDate = new Date() - 6 // 6 days ago

        // Configure skill expiration
        LocalDateTime expirationDate = (new Date()-1).toLocalDateTime() // yesterday
        divineDragonUser1SkillsService.saveSkillExpirationAttributes(proj.projectId, skills[0].skillId.toString(), [
                expirationType: ExpirationAttrs.YEARLY,
                every: 1,
                monthlyDay: expirationDate.dayOfMonth,
                nextExpirationDate: expirationDate.toDate(),
                emailNotificationsEnabled: true
        ])

        // Create user achievement
        divineDragonUser1SkillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId.toString()], userId, achievementDate)

        // Set expiration notification state to NONE
        UserAchievement achievement = userAchievedRepo.findAllByUserIdAndSkillId(userId, skills[0].skillId.toString())[0]
        achievement.expirationNotificationState = 'NONE'
        userAchievedRepo.save(achievement)

        jdbcTemplate.execute("delete from user_tags where user_id='${userId}' and key='dragons'")

        when:
        expireUserAchievementsTaskExecutor.removeExpiredUserAchievements()

        then:
        greenMail.getReceivedMessages().size() == 0
    }

    def "send skill expired email notifications for YEARLY"() {
        Map settingRequest = [
                settingGroup : EmailSettingsService.settingsGroup,
                setting : EmailSettingsService.htmlHeader,
                value : 'For {{ community.descriptor }} Only'
        ]
        SkillsService rootSkillsService = createRootSkillService()
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)
        settingRequest.setting = EmailSettingsService.plaintextHeader
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)
        settingRequest.setting = EmailSettingsService.htmlFooter
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)
        settingRequest.setting = EmailSettingsService.plaintextFooter
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)

        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(3, 1, 1, 100)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        String userId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID]).first()
        Date achievementDate = new Date() - 10 // 10 days ago

        // Configure skill expiration
        LocalDateTime expirationDate = (new Date()-1).toLocalDateTime() // yesterday
        skillsService.saveSkillExpirationAttributes(proj.projectId, skills[0].skillId.toString(), [
                expirationType: ExpirationAttrs.YEARLY,
                every: 1,
                monthlyDay: expirationDate.dayOfMonth,
                nextExpirationDate: expirationDate.toDate(),
                emailNotificationsEnabled: true
        ])

        // Create user achievement
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId.toString()], userId, achievementDate)

        // Set expiration notification state to EXPIRATION_WARNING_NOTIFICATION_SENT
        UserAchievement achievement = userAchievedRepo.findAllByUserIdAndSkillId(userId, skills[0].skillId.toString())[0]
        achievement.expirationNotificationState = EXPIRATION_WARNING_NOTIFICATION_SENT
        userAchievedRepo.save(achievement)

        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(userId)

        when:
        expireUserAchievementsTaskExecutor.removeExpiredUserAchievements()

        assert WaitFor.wait { greenMail.getReceivedMessages().size() == 1 }
        List<EmailUtils.EmailRes> emails = EmailUtils.getEmails(greenMail)

        Pattern plaintTextMatch = ~/(?s)For All Dragons Only.*Hello.*We're writing to inform you that your achievement for the skill ${skills[0].name} in project ${proj.name} has expired.*Expiration Details:.*- Skill: ${skills[0].name}.*- Project: ${proj.name}.*- Expired On: ${expirationDate.format(DateTimeFormatter.ofPattern('yyyy-MM-dd'))}.*For All Dragons Only/

        Pattern h1 = ~/(?s)For All Dragons Only.*<h1>Your Skill Achievement Has Expired<\/h1>.+?/
        Pattern p1 = ~/(?s)<p>We're writing to inform you that your achievement for the skill <strong>${skills[0].name}<\/strong> in project <strong>${proj.name}<\/strong> has expired.<\/p>.+?/
        Pattern p2 = ~/(?s)<strong>Skill:<\/strong>.*${skills[0].name}.*<strong>Project:<\/strong>.*${proj.name}.*<strong>Expired On:<\/strong>.*${expirationDate.format(DateTimeFormatter.ofPattern('yyyy-MM-dd'))}/

        then:
        emails.size() == 1
        emails[0].recipients == [userAttrs.email]
        emails[0].subj == "Your Skill Achievement Has Expired"
        plaintTextMatch.matcher(emails[0].plainText).find()
        h1.matcher(emails[0].html).find()
        p1.matcher(emails[0].html).find()
        p2.matcher(emails[0].html).find()

        // Verify notification state was updated
        UserAchievement updatedAchievement = userAchievedRepo.findAllByUserIdAndSkillId(userId, skills[0].skillId.toString())[0]
    }

    def "send skill expired email notifications for MONTHLY"() {
        Map settingRequest = [
                settingGroup : EmailSettingsService.settingsGroup,
                setting : EmailSettingsService.htmlHeader,
                value : 'For {{ community.descriptor }} Only'
        ]
        SkillsService rootSkillsService = createRootSkillService()
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)
        settingRequest.setting = EmailSettingsService.plaintextHeader
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)
        settingRequest.setting = EmailSettingsService.htmlFooter
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)
        settingRequest.setting = EmailSettingsService.plaintextFooter
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)

        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(3, 1, 1, 100)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        String userId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID]).first()
        Date achievementDate = new Date() - 10 // 10 days ago

        // Configure skill expiration
        LocalDateTime expirationDate = (new Date()-1).toLocalDateTime() // yesterday
        skillsService.saveSkillExpirationAttributes(proj.projectId, skills[0].skillId.toString(), [
                expirationType: ExpirationAttrs.MONTHLY,
                every: 1,
                monthlyDay: expirationDate.dayOfMonth,
                nextExpirationDate: expirationDate.toDate(),
                emailNotificationsEnabled: true
        ])

        // Create user achievement
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId.toString()], userId, achievementDate)

        // Set expiration notification state to EXPIRATION_WARNING_NOTIFICATION_SENT
        UserAchievement achievement = userAchievedRepo.findAllByUserIdAndSkillId(userId, skills[0].skillId.toString())[0]
        achievement.expirationNotificationState = EXPIRATION_WARNING_NOTIFICATION_SENT
        userAchievedRepo.save(achievement)

        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(userId)

        when:
        expireUserAchievementsTaskExecutor.removeExpiredUserAchievements()

        assert WaitFor.wait { greenMail.getReceivedMessages().size() == 1 }
        List<EmailUtils.EmailRes> emails = EmailUtils.getEmails(greenMail)

        Pattern plaintTextMatch = ~/(?s)For All Dragons Only.*Hello.*We're writing to inform you that your achievement for the skill ${skills[0].name} in project ${proj.name} has expired.*Expiration Details:.*- Skill: ${skills[0].name}.*- Project: ${proj.name}.*- Expired On: ${expirationDate.format(DateTimeFormatter.ofPattern('yyyy-MM-dd'))}.*For All Dragons Only/

        Pattern h1 = ~/(?s)For All Dragons Only.*<h1>Your Skill Achievement Has Expired<\/h1>.+?/
        Pattern p1 = ~/(?s)<p>We're writing to inform you that your achievement for the skill <strong>${skills[0].name}<\/strong> in project <strong>${proj.name}<\/strong> has expired.<\/p>.+?/
        Pattern p2 = ~/(?s)<strong>Skill:<\/strong>.*${skills[0].name}.*<strong>Project:<\/strong>.*${proj.name}.*<strong>Expired On:<\/strong>.*${expirationDate.format(DateTimeFormatter.ofPattern('yyyy-MM-dd'))}/

        then:
        emails.size() == 1
        emails[0].recipients == [userAttrs.email]
        emails[0].subj == "Your Skill Achievement Has Expired"
        plaintTextMatch.matcher(emails[0].plainText).find()
        h1.matcher(emails[0].html).find()
        p1.matcher(emails[0].html).find()
        p2.matcher(emails[0].html).find()

        // Verify notification state was updated
        UserAchievement updatedAchievement = userAchievedRepo.findAllByUserIdAndSkillId(userId, skills[0].skillId.toString())[0]
    }

    def "do not send skill expired email notifications for DAILY"() {

        Map settingRequest = [
                settingGroup : EmailSettingsService.settingsGroup,
                setting : EmailSettingsService.htmlHeader,
                value : 'For {{ community.descriptor }} Only'
        ]
        SkillsService rootSkillsService = createRootSkillService()
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)
        settingRequest.setting = EmailSettingsService.plaintextHeader
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)
        settingRequest.setting = EmailSettingsService.htmlFooter
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)
        settingRequest.setting = EmailSettingsService.plaintextFooter
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)

        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(3, 1, 1, 100)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        String userId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID]).first()
        Date achievementDate = new Date() - 6 // 6 days ago

        // Configure skill expiration
        skillsService.saveSkillExpirationAttributes(proj.projectId, skills[0].skillId.toString(), [
                expirationType: ExpirationAttrs.DAILY,
                every: 7,
                emailNotificationsEnabled: true
        ])

        // Create user achievement
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId.toString()], userId, achievementDate)

        // Set expiration notification state to NONE
        UserAchievement achievement = userAchievedRepo.findAllByUserIdAndSkillId(userId, skills[0].skillId.toString())[0]
        achievement.expirationNotificationState = 'NONE'
        userAchievedRepo.save(achievement)

        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(userId)

        when:
        expireUserAchievementsTaskExecutor.removeExpiredUserAchievements()

        then:
        greenMail.getReceivedMessages().size() == 0
    }

    def "do not send email if notifications are disabled"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(3, 1, 1, 100)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        String userId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID]).first()
        Date achievementDate = new Date() - 6

        // Configure skill expiration with email notifications disabled
        skillsService.saveSkillExpirationAttributes(proj.projectId, skills[0].skillId.toString(), [
                expirationType: ExpirationAttrs.DAILY,
                every: 7,
                emailNotificationsEnabled: false
        ])

        // Create user achievement
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId.toString()], userId, achievementDate)

        when:
        expireUserAchievementsTaskExecutor.removeExpiredUserAchievements()

        then:
        greenMail.getReceivedMessages().size() == 0
    }

    def "do not send duplicate warning emails"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(3, 1, 1, 100)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        String userId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID]).first()
        Date achievementDate = new Date() - 6

        // Configure skill expiration
        skillsService.saveSkillExpirationAttributes(proj.projectId, skills[0].skillId.toString(), [
                expirationType: ExpirationAttrs.DAILY,
                every: 7,
                emailNotificationsEnabled: true
        ])

        // Create user achievement
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId.toString()], userId, achievementDate)

        // Set expiration notification state to NOTIFIED (already sent)
        UserAchievement achievement = userAchievedRepo.findAllByUserIdAndSkillId(userId, skills[0].skillId.toString())[0]
        achievement.expirationNotificationState = EXPIRATION_WARNING_NOTIFICATION_SENT
        userAchievedRepo.save(achievement)

        when:
        expireUserAchievementsTaskExecutor.removeExpiredUserAchievements()

        then:
        greenMail.getReceivedMessages().size() == 0
    }

    def "do not send duplicate expired emails"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(3, 1, 1, 100)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        String userId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID]).first()
        Date achievementDate = new Date() - 10

        // Configure skill expiration
        skillsService.saveSkillExpirationAttributes(proj.projectId, skills[0].skillId.toString(), [
                expirationType: ExpirationAttrs.DAILY,
                every: 7,
                emailNotificationsEnabled: true
        ])

        // Create user achievement
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId.toString()], userId, achievementDate)

        // Set expiration notification state to EXPIRED_SENT (already sent)
        UserAchievement achievement = userAchievedRepo.findAllByUserIdAndSkillId(userId, skills[0].skillId.toString())[0]
        achievement.expirationNotificationState = 'EXPIRED_SENT'
        userAchievedRepo.save(achievement)

        when:
        expireUserAchievementsTaskExecutor.removeExpiredUserAchievements()

        then:
        greenMail.getReceivedMessages().size() == 0
    }

    def "handle multiple expiring achievements for same user"() {
        Map settingRequest = [
                settingGroup : EmailSettingsService.settingsGroup,
                setting : EmailSettingsService.htmlHeader,
                value : 'For {{ community.descriptor }} Only'
        ]
        SkillsService rootSkillsService = createRootSkillService()
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)
        settingRequest.setting = EmailSettingsService.plaintextHeader
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)
        settingRequest.setting = EmailSettingsService.htmlFooter
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)
        settingRequest.setting = EmailSettingsService.plaintextFooter
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)

        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(3, 1, 1, 100)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        String userId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID]).first()
        Date achievementDate = new Date() - 6

        // Configure multiple skills for expiration
        skills.each { skill ->
            skillsService.saveSkillExpirationAttributes(proj.projectId, skill.skillId, [
                    expirationType: ExpirationAttrs.DAILY,
                    every: 7,
                    emailNotificationsEnabled: true
            ])
            skillsService.addSkill([projectId: proj.projectId, skillId: skill.skillId], userId, achievementDate)
            
            UserAchievement achievement = userAchievedRepo.findAllByUserIdAndSkillId(userId, skill.skillId)[0]
            achievement.expirationNotificationState = 'NONE'
            userAchievedRepo.save(achievement)
        }

        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(userId)

        when:
        expireUserAchievementsTaskExecutor.removeExpiredUserAchievements()

        assert WaitFor.wait { greenMail.getReceivedMessages().size() == 3 }
        List<EmailUtils.EmailRes> emails = EmailUtils.getEmails(greenMail)

        then:
        emails.size() == 3
        emails.every { it.recipients == [userAttrs.email] }
        emails.every { it.subj == "Skill Achievement Expiring Soon" }
        
        // Verify all notification states were updated
        skills.each { skill ->
            UserAchievement updatedAchievement = userAchievedRepo.findAllByUserIdAndSkillId(userId, skill.skillId)[0]
            updatedAchievement.expirationNotificationState == EXPIRATION_WARNING_NOTIFICATION_SENT
        }
    }

    def "handle multiple users with some hitting expiration threshold and others not"() {
        Map settingRequest = [
                settingGroup : EmailSettingsService.settingsGroup,
                setting : EmailSettingsService.htmlHeader,
                value : 'For {{ community.descriptor }} Only'
        ]
        SkillsService rootSkillsService = createRootSkillService()
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)
        settingRequest.setting = EmailSettingsService.plaintextHeader
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)
        settingRequest.setting = EmailSettingsService.htmlFooter
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)
        settingRequest.setting = EmailSettingsService.plaintextFooter
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)

        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(2, 1, 1, 100)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        // Create multiple users with different achievement dates
        List<String> userIds = getRandomUsers(4, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])
        
        // User 1: Achievement 6 days ago - should get email (within 7-day threshold)
        Date user1Date = new Date() - 6
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userIds[0], user1Date)
        
        // User 2: Achievement 8 days ago - should not get email (outside 7-day threshold)
        Date user2Date = new Date() - 8
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userIds[1], user2Date)
        
        // User 3: Achievement 6 days ago - should get email (within 7-day threshold)
        Date user3Date = new Date() - 6
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[1].skillId], userIds[2], user3Date)
        
        // User 4: Achievement 10 days ago - should not get email (outside 7-day threshold)
        Date user4Date = new Date() - 10
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[1].skillId], userIds[3], user4Date)

        // Configure skills for expiration with 7-day warning threshold
        skills.each { skill ->
            skillsService.saveSkillExpirationAttributes(proj.projectId, skill.skillId, [
                    expirationType: ExpirationAttrs.DAILY,
                    every: 7,
                    emailNotificationsEnabled: true
            ])
            
            // Reset notification states to NONE
            userIds.each { userId ->
                UserAchievement achievement = userAchievedRepo.findAllByUserIdAndSkillId(userId, skill.skillId)[0]
                if (achievement) {
                    achievement.expirationNotificationState = 'NONE'
                    userAchievedRepo.save(achievement)
                }
            }
        }

        List<UserAttrs> userAttrs = userIds.collect { userId -> 
            userAttrsRepo.findByUserIdIgnoreCase(userId)
        }

        when:
        expireUserAchievementsTaskExecutor.removeExpiredUserAchievements()

        assert WaitFor.wait { greenMail.getReceivedMessages().size() == 2 }
        List<EmailUtils.EmailRes> emails = EmailUtils.getEmails(greenMail)

        then:
        emails.size() == 2
        
        // Verify users 1 and 3 received emails (within threshold)
        emails.any { it.recipients == [userAttrs[0].email] } // User 1 should get email
        emails.any { it.recipients == [userAttrs[2].email] } // User 3 should get email
        
        // Verify users 2 and 4 did not receive emails (outside threshold)
        !emails.any { it.recipients == [userAttrs[1].email] } // User 2 should not get email
        !emails.any { it.recipients == [userAttrs[3].email] } // User 4 should not get email
        
        emails.every { it.subj == "Skill Achievement Expiring Soon" }
        
        // Verify notification states were updated correctly
        UserAchievement user1Achievement = userAchievedRepo.findAllByUserIdAndSkillId(userIds[0], skills[0].skillId)[0]
        user1Achievement.expirationNotificationState == EXPIRATION_WARNING_NOTIFICATION_SENT
        
        !userAchievedRepo.findAllByUserIdAndSkillId(userIds[1], skills[0].skillId)[0]  // this user achievement was expired

        UserAchievement user3Achievement = userAchievedRepo.findAllByUserIdAndSkillId(userIds[2], skills[1].skillId)[0]
        user3Achievement.expirationNotificationState == EXPIRATION_WARNING_NOTIFICATION_SENT
        
        !userAchievedRepo.findAllByUserIdAndSkillId(userIds[3], skills[1].skillId)[0]  // this user achievement was expired
    }

    def "handle failures in isolation and do not fail the entire batch"() {
        Map settingRequest = [
                settingGroup : EmailSettingsService.settingsGroup,
                setting : EmailSettingsService.htmlHeader,
                value : 'For {{ community.descriptor }} Only'
        ]
        SkillsService rootSkillsService = createRootSkillService()
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)
        settingRequest.setting = EmailSettingsService.plaintextHeader
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)
        settingRequest.setting = EmailSettingsService.htmlFooter
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)
        settingRequest.setting = EmailSettingsService.plaintextFooter
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)

        def proj1 = SkillsFactory.createProject(1)
        def proj2 = SkillsFactory.createProject(2)
        def subj1 = SkillsFactory.createSubject(1, 1)
        def subj2 = SkillsFactory.createSubject(2, 2)

        def skill1 = SkillsFactory.createSkill(1, 1, 1, 1, 1, 0, 100)
        def skill2 = SkillsFactory.createSkill(1, 1, 2, 1, 1, 0, 100)
        def skill3_imported = SkillsFactory.createSkill(2, 2, 3, 1, 1, 0, 100)
        def skill4 = SkillsFactory.createSkill(1, 1, 4, 1, 1, 0, 100)
        def skill5 = SkillsFactory.createSkill(1, 1, 5, 1, 1, 0, 100)

        skillsService.createProject(proj1)
        skillsService.createProject(proj2)
        skillsService.createSubject(subj1)
        skillsService.createSubject(subj2)
        skillsService.createSkill(skill1)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3_imported)
        skillsService.createSkill(skill4)
        skillsService.createSkill(skill5)

        skillsService.exportSkillToCatalog(proj2.projectId, skill3_imported.skillId)
        skillsService.importSkillFromCatalog(proj1.projectId, subj1.subjectId, proj2.projectId, skill3_imported.skillId)
        skillsService.finalizeSkillsImportFromCatalog(proj1.projectId, true)

        String userId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID]).first()
        Date date = new Date()
        skillsService.addSkill([projectId: proj1.projectId, skillId: skill1.skillId], userId, date)
        skillsService.addSkill([projectId: proj1.projectId, skillId: skill2.skillId], userId, date)
        skillsService.addSkill([projectId: proj2.projectId, skillId: skill3_imported.skillId], userId, date)
        skillsService.addSkill([projectId: proj1.projectId, skillId: skill4.skillId], userId, date)
        skillsService.addSkill([projectId: proj1.projectId, skillId: skill5.skillId], userId, date)

        // bypass endpoint validation for testing purposes, show never get in this state though
        LocalDateTime expirationDate = (new Date() - 1).toLocalDateTime() // yesterday
        skillAttributeService.saveAttrs(proj1.projectId, skill3_imported.skillId, SkillAttributesDef.SkillAttributesType.AchievementExpiration, new ExpirationAttrs(
                expirationType    : ExpirationAttrs.YEARLY,
                every             : 1,
                monthlyDay        : expirationDate.dayOfMonth,
                nextExpirationDate: expirationDate.toDate(),
        ))
        def nonImportedSkills = [skill1, skill2, skill4, skill5]
        nonImportedSkills.each { skill ->
            skillsService.saveSkillExpirationAttributes(proj1.projectId, skill.skillId.toString(), [
                    expirationType: ExpirationAttrs.YEARLY,
                    every: 1,
                    monthlyDay: expirationDate.dayOfMonth,
                    nextExpirationDate: expirationDate.toDate(),
                    emailNotificationsEnabled: true
            ])
        }

        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(userId)

        when:
        expireUserAchievementsTaskExecutor.removeExpiredUserAchievements()

        assert WaitFor.wait { greenMail.getReceivedMessages().size() == 4 }
        List<EmailUtils.EmailRes> emails = EmailUtils.getEmails(greenMail)

        then:
        emails.size() == 4
        emails.every { it.recipients == [userAttrs.email] }
        emails.every { it.subj == "Your Skill Achievement Has Expired" }

        // Verify all non-imported skills notifications were sent
        nonImportedSkills.each { skill ->
            emails.find { it.html.contains(skill.name) }
        }

        // verify imported skill notifications were not sent
        !emails.find { it.html.contains(skill3_imported.name) }
        notificationsRepo.count() == 0
    }

    def "do send skill expired emails for invite only project for permitted user"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(3, 1, 1, 100)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)
        skillsService.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])

        String userId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID]).first()
        def newService = createService(userId)
        Date achievementDate = new Date() - 10 // 10 days ago

        // Configure skill expiration
        LocalDateTime expirationDate = (new Date()-1).toLocalDateTime() // yesterday
        skillsService.saveSkillExpirationAttributes(proj.projectId, skills[0].skillId.toString(), [
                expirationType: ExpirationAttrs.MONTHLY,
                every: 1,
                monthlyDay: expirationDate.dayOfMonth,
                nextExpirationDate: expirationDate.toDate(),
                emailNotificationsEnabled: true
        ])

        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT5M", recipients: ["someemail@email.foo"]])
        WaitFor.wait {
            greenMail.getReceivedMessages().length > 0
        }

        def email = EmailUtils.getEmail(greenMail, 0)
        def invite = extractInviteFromEmail(email.html)
        greenMail.reset()

        newService.joinProject(proj.projectId, invite)

        // Create user achievement
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId.toString()], userId, achievementDate)

        when:
        expireUserAchievementsTaskExecutor.removeExpiredUserAchievements()

        assert WaitFor.wait { greenMail.getReceivedMessages().size() == 1 }
        List<EmailUtils.EmailRes> emails = EmailUtils.getEmails(greenMail)

        then:
        emails.size() == 1
        emails[0].subj == "Your Skill Achievement Has Expired"
        emails[0].html.contains(skills[0].name)
    }

    def "do not send skill expired emails for invite only project after user access is revoked"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(3, 1, 1, 100)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)
        skillsService.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])

        String userId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID]).first()
        def newService = createService(userId)
        Date achievementDate = new Date() - 10 // 10 days ago

        // Configure skill expiration
        LocalDateTime expirationDate = (new Date()-1).toLocalDateTime() // yesterday
        skillsService.saveSkillExpirationAttributes(proj.projectId, skills[0].skillId.toString(), [
                expirationType: ExpirationAttrs.MONTHLY,
                every: 1,
                monthlyDay: expirationDate.dayOfMonth,
                nextExpirationDate: expirationDate.toDate(),
                emailNotificationsEnabled: true
        ])

        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT5M", recipients: ["someemail@email.foo"]])
        WaitFor.wait {
            greenMail.getReceivedMessages().length > 0
        }

        def email = EmailUtils.getEmail(greenMail, 0)
        def invite = extractInviteFromEmail(email.html)
        greenMail.reset()

        newService.joinProject(proj.projectId, invite)

        // Create user achievement
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId.toString()], userId, achievementDate)

        skillsService.revokeInviteOnlyProjectAccess(proj.projectId, userId)

        when:
        expireUserAchievementsTaskExecutor.removeExpiredUserAchievements()

        then:
        greenMail.getReceivedMessages().size() == 0
    }
}
