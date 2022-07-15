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
package skills.notify


import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.EmailUtils
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.services.ProjectErrorService
import skills.storage.model.ProjectError
import skills.storage.model.SkillDef
import skills.storage.repos.NotificationsRepo
import skills.storage.repos.UserAttrsRepo
import skills.utils.WaitFor

@Slf4j
class SelfReportApprovalRequestEmailNotificationSpecs extends DefaultIntSpec {

    @Autowired
    EmailNotifier emailNotifier

    @Autowired
    NotificationsRepo notificationsRepo

    @Autowired
    UserAttrsRepo userAttrsRepo

    @Autowired
    ProjectErrorService projectErrorService

    String email

    def setup() {
        startEmailServer()

        // little trick to force PKI-based runs to create UserAttrs record
        skillsService.isRoot()
        email = userAttrsRepo.findByUserId(skillsService.userName).email
        assert email
    }

    def "project admins who are unsubscribed do not receive emails"() {

        def users = getRandomUsers(6, true)

        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        def skill = SkillsFactory.createSkill(1, 1, 1)
        skill.pointIncrement = 200
        skill.selfReportingType = SkillDef.SelfReportingType.Approval.toString()

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skill)

        def userToReport = users.pop()

        List<SkillsService> userSpecificServiceInstances = []

        users.each {
            def tmpService = createService(it)
            userSpecificServiceInstances << tmpService
        }

        userSpecificServiceInstances.each {
            skillsService.addProjectAdmin(proj.projectId, it.userName)
        }

        WaitFor.wait { greenMail.getReceivedMessages().size() == userSpecificServiceInstances.size() }
        greenMail.purgeEmailFromAllMailboxes()

        //unsubscribe the first 4 users
        userSpecificServiceInstances.subList(0, 4).each {
            it.unsubscribeFromSelfApprovalRequestEmails(proj.projectId)
            assert it.isSubscribedToSelfApprovalRequestEmails(proj.projectId) === false
        }

        //re-subscribe the 4th user
        userSpecificServiceInstances[3].subscribeToSelfApprovalRequestEmails(proj.projectId)

        //user 4 was resubscribed, user 5 should default to subscribed in the absense of an explicit setting
        userSpecificServiceInstances.subList(3, 5).each {
            assert it.isSubscribedToSelfApprovalRequestEmails(proj.projectId) === true
        }

        skillsService.addSkill([projectId: proj.projectId, skillId: skill.skillId], userToReport, new Date(), "Please approve this")

        when:
        assert WaitFor.wait { greenMail.getReceivedMessages().size() > 2 }

        then:
        greenMail.getReceivedMessages().length == 3
        Set<String> allUniqueRecipients = EmailUtils.getAllUniqueRecipients(greenMail)
        allUniqueRecipients.find {it.contains(users[3])}
        allUniqueRecipients.find {it.contains(users[4])}
        !allUniqueRecipients.find {it.contains(users[0])}
        !allUniqueRecipients.find {it.contains(users[1])}
        !allUniqueRecipients.find {it.contains(users[2])}
    }

    def "Project Error is created if a project has no subscribed project administrators"() {
        def users = getRandomUsers(6, true)

        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        def skill = SkillsFactory.createSkill(1, 1, 1)
        skill.pointIncrement = 200
        skill.selfReportingType = SkillDef.SelfReportingType.Approval.toString()

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skill)

        def userToReport = users.pop()

        List<SkillsService> userSpecificServiceInstances = []

        users.each {
            def tmpService = createService(it)
            userSpecificServiceInstances << tmpService
        }

        userSpecificServiceInstances.each {
            skillsService.addProjectAdmin(proj.projectId, it.userName)
        }

        //unsubscribe all project administrators
        userSpecificServiceInstances.each {
            it.unsubscribeFromSelfApprovalRequestEmails(proj.projectId)
            assert it.isSubscribedToSelfApprovalRequestEmails(proj.projectId) === false
        }
        skillsService.unsubscribeFromSelfApprovalRequestEmails(proj.projectId)

        when:
        skillsService.addSkill([projectId: proj.projectId, skillId: skill.skillId], userToReport, new Date(), "Please approve this")

        then:
        projectErrorService.countOfErrorsForProject(proj.projectId) == 1
        projectErrorService.getAllErrorsForProject(proj.projectId, PageRequest.of(0, 10)).data[0].errorType == ProjectError.ErrorType.NoEmailableApprovers.toString()
    }

}
