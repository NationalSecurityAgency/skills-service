/**
 * Copyright 2021 SkillTree
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
package skills.intTests

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionTemplate
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.EmailUtils
import skills.intTests.utils.SkillsFactory
import skills.services.ProjectErrorService
import skills.services.ProjectExpirationService
import skills.services.UserEventService
import skills.services.settings.SettingsService
import skills.storage.model.UserAttrs
import skills.storage.repos.SkillDefRepo
import skills.utils.WaitFor
import spock.lang.IgnoreRest

import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class UnusedProjectExpirationSpecs extends DefaultIntSpec{

    @Autowired
    SettingsService settingsService

    @Autowired
    ProjectExpirationService expirationService

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    UserEventService userEventService

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    ProjectErrorService errorService

    def setup() {
        startEmailServer()
    }

    def "Projects older than max unused are flagged for removal"() {
        def proj1 = SkillsFactory.createProject(1)
        skillsService.createProject(proj1)

        def proj3 = SkillsFactory.createProject(3)
        skillsService.createProject(proj3)
        def subj = SkillsFactory.createSubject(3)
        skillsService.createSubject(subj)
        def skills = SkillsFactory.createSkills(15, 3)
        skillsService.createSkills(skills)


        def proj4 = SkillsFactory.createProject(4)
        skillsService.createProject(proj4)
        def proj4_subj = SkillsFactory.createSubject(4)
        skillsService.createSubject(proj4_subj)

        def proj5 = SkillsFactory.createProject(5)
        skillsService.createProject(proj5)

        def proj6 = SkillsFactory.createProject(6)
        skillsService.createProject(proj6)
        def proj6_subj = SkillsFactory.createSubject(6, 1)
        skillsService.createSubject(proj6_subj)
        def proj6_skills = SkillsFactory.createSkills(5, 6)
        skillsService.createSkills(proj6_skills)

        def proj7 = SkillsFactory.createProject(7)
        skillsService.createProject(proj7)

        def proj8 = SkillsFactory.createProject(8)
        skillsService.createProject(proj8)

        def proj9 = SkillsFactory.createProject(9)
        skillsService.createProject(proj9)

        Thread.sleep(TimeUnit.SECONDS.toMillis(1))

        Date cutoff = new Date()

        Thread.sleep(TimeUnit.SECONDS.toMillis(1))
        def user = getRandomUsers(1)[0]

        //a user event after the cutoff should prevent the project from being flagged
        //because we can't report a future skill here so that the user_event will prevent this project from being identified as not touched
        //(because the user_event event_time is normalized to the start of the day so it would be less than cutoff)
        //we have to cheat ane explicitly add an event directly, without going through the add skill flow
        skillsService.addSkill([projectId: proj3.projectId, skillId: skills[0].skillId], user, new Date())
        Integer refId = skillDefRepo.findByProjectIdAndSkillId(proj3.projectId, skills[0].skillId).id
        userEventService.recordEvent(proj3.projectId, refId, user, new Date().plus(1))

        //project created after the cutoff should not be flagged
        def proj2 = SkillsFactory.createProject(2)
        skillsService.createProject(proj2)

        //project structure modified after cutoff should prevent being flagged
        def proj4_skills = SkillsFactory.createSkills(1, 4)
        skillsService.createSkills(proj4_skills)

        def proj8_subj = SkillsFactory.createSubject(8)
        skillsService.createSubject(proj8_subj)

        def proj9_badge = SkillsFactory.createBadge(9)
        skillsService.createBadge(proj9_badge)

        //edit to project itself after cutoff should prevent being flagged
        def p5 = skillsService.getProject(proj5.projectId)
        def originalProjectId = p5.projectId
        p5.name = "New Name 1,000"
        skillsService.updateProject(p5, originalProjectId)

        //a relationship added after the cutoff should prevent the project from being flagged
        skillsService.assignDependency([projectId         : proj6.projectId, skillId: proj6_skills.get(0).skillId,
                                        dependentSkillId: proj6_skills.get(1).skillId,])

        //a project error after the cutoff date should prevent the project from being flagged
        errorService.invalidSkillReported(proj7.projectId, "fake_id")

        when:
        expirationService.flagOldProjects(cutoff)

        then:
        settingsService.getProjectSetting(proj1.projectId, "expiration.expiring.unused", "expiration")
        !settingsService.getProjectSetting(proj2.projectId, "expiration.expiring.unused", "expiration")
        !settingsService.getProjectSetting(proj3.projectId, "expiration.expiring.unused", "expiration")
        !settingsService.getProjectSetting(proj4.projectId, "expiration.expiring.unused", "expiration")
        !settingsService.getProjectSetting(proj5.projectId, "expiration.expiring.unused", "expiration")
        !settingsService.getProjectSetting(proj6.projectId, "expiration.expiring.unused", "expiration")
        !settingsService.getProjectSetting(proj7.projectId, "expiration.expiring.unused", "expiration")
        !settingsService.getProjectSetting(proj8.projectId, "expiration.expiring.unused", "expiration")
        !settingsService.getProjectSetting(proj9.projectId, "expiration.expiring.unused", "expiration")
    }

    def "Projects flagged for removal are deleted"() {
        def proj1 = SkillsFactory.createProject(1)
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager)

        skillsService.createProject(proj1)
        Date flagOn = new Date();
        Boolean result = transactionTemplate.execute(new TransactionCallback<Boolean>() {
            @Override
            Boolean doInTransaction(TransactionStatus transactionStatus) {
                expirationService.flagOldProjects(flagOn)
                return Boolean.TRUE
            }
        })

        when:
        assert result
        Date gracePeriodCutoff = flagOn.toLocalDateTime().plusSeconds(5).toDate()
        //wait for grace period to expire
        Thread.sleep(TimeUnit.SECONDS.toMillis(6))
        expirationService.deleteUnusedProjects(gracePeriodCutoff)

        then:
        skillsService.projectIdExists(["projectId":proj1.projectId]).body == false
    }

    def "notify users of projects within grace period"() {
        def proj1 = SkillsFactory.createProject(1)

        skillsService.createProject(proj1)
        Date flagForExpiration = new Date()
        expirationService.flagOldProjects(flagForExpiration)

        String otherUser = getRandomUsers(1).first()

        createService(otherUser)
        skillsService.addProjectAdmin(proj1.projectId, otherUser)

        UserAttrs projectAdminUserAttrs = userAttrsRepo.findByUserId(skillsService.userName)
        UserAttrs otherProjectAdminUserAttrs = userAttrsRepo.findByUserId(otherUser)
        UserAttrs rootUserUserAttrs = userAttrsRepo.findByUserId(DEFAULT_ROOT_USER_ID.toLowerCase())

        when:
        expirationService.notifyGracePeriodProjectAdmins(flagForExpiration.minus(1))
        assert WaitFor.wait { greenMail.getReceivedMessages().size() > 2 }
        List<EmailUtils.EmailRes> emails = EmailUtils.getEmails(greenMail)

        Pattern plaintTextMatch = ~/(?s)Your SkillTree Project Inception, created on \d{4}-\d{2}-\d{2} hasn't been used in at least \d+ days?.*If you take no action, Project Inception will be deleted in \d+ days? \(\d{4}-\d{2}-\d{2}\).*If you wish to stop receiving these emails, visit http:\/\/localhost:\d+\/administrator\/projects\/Inception in the SkillTree dashboard and click the 'Keep' button or delete your Project.*/

        Pattern h1 = ~/(?s)<h1>Your SkillTree Project Isn't Being Used!<\/h1>.+?/
        Pattern p1 = ~/(?s)<p>Your SkillTree Project Test Project#1, created on \d{4}-\d{2}-\d{2} hasn't been used in at least \d+ days?.<\/p>.+?/
        Pattern p2 = ~/(?s)<p>If you take no action, Project Test Project#1 will be deleted in \d+ days? \(\d{4}-\d{2}-\d{2}\).<\/p>.+?/
        Pattern p3 = ~/(?s)<p>If you wish to stop receiving these emails, visit <a href="http:\/\/localhost:\d+\/administrator\/projects\/TestProject1">Test Project#1<\/a> in the SkillTree dashboard and click the 'Keep' button or delete your Project.<\/p>.*/


        then:
        emails.size() == 3
        emails.collect {it.recipients[0] }.sort() == [rootUserUserAttrs.email, projectAdminUserAttrs.email, otherProjectAdminUserAttrs.email].sort()
        emails.find { it.subj == "SkillTree Project is expiring!" }
        emails.find {it.recipients == [otherProjectAdminUserAttrs.email]}
        emails.find { plaintTextMatch.matcher(it.plainText).find() }
        emails.findAll {h1.matcher(it.html).find() }?.size() == 3
        emails.find { p1.matcher(it.html).find() }
        emails.find { p2.matcher(it.html).find() }
        emails.find { p3.matcher(it.html).find() }
    }

}
