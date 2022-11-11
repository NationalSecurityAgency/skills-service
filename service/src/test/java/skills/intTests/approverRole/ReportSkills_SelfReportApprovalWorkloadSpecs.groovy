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
package skills.intTests.approverRole

import groovy.util.logging.Slf4j
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.EmailUtils
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.storage.model.auth.RoleName
import skills.utils.WaitFor

@Slf4j
class ReportSkills_SelfReportApprovalWorkloadSpecs extends DefaultIntSpec {

    def setup() {
        startEmailServer()
    }

    def "no conf at all - notify all admins and approvers"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSelfReportSkills(5,)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

        List<SkillsService> approvers = createAdditionalApprovers(proj, 2)
        List<String> expectedEmails = getEmails(approvers, skillsService)
        when:
        assert skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], "userA").body.explanation == "Skill was submitted for approval"
        List<EmailUtils.EmailRes> emails = waitAndCollect(expectedEmails.size() )

        then:
        emails.collect {it.recipients[0] }.sort() == expectedEmails.sort()
    }

    def "1 skill is configured - notify matched approver"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSelfReportSkills(5,)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

        List<SkillsService> approvers = createAdditionalApprovers(proj, 2)
        skillsService.configureApproverForSkillId(proj.projectId, approvers[0].userName, skills[0].skillId)

        List<String> expectedEmails = getEmails([approvers[0]])
        when:
        assert skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], "userA").body.explanation == "Skill was submitted for approval"
        List<EmailUtils.EmailRes> emails = waitAndCollect(expectedEmails.size() )

        then:
        emails.collect {it.recipients[0] }.sort() == expectedEmails.sort()
    }

    def "1 skill is configured for multiple approvers"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSelfReportSkills(5,)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

        List<SkillsService> approvers = createAdditionalApprovers(proj, 3)
        // approver 1
        skillsService.configureApproverForSkillId(proj.projectId, approvers[0].userName, skills[0].skillId)
        skillsService.configureApproverForSkillId(proj.projectId, approvers[0].userName, skills[1].skillId)
        // approver 2
        skillsService.configureApproverForSkillId(proj.projectId, approvers[1].userName, skills[0].skillId)
        skillsService.configureApproverForSkillId(proj.projectId, approvers[1].userName, skills[2].skillId)
        // approver 3
        skillsService.configureApproverForSkillId(proj.projectId, approvers[2].userName, skills[1].skillId)
        skillsService.configureApproverForSkillId(proj.projectId, approvers[2].userName, skills[2].skillId)
        skillsService.configureApproverForSkillId(proj.projectId, approvers[2].userName, skills[3].skillId)
        skillsService.configureApproverForSkillId(proj.projectId, approvers[2].userName, skills[4].skillId)

        List<String> expectedEmails = getEmails([approvers[0], approvers[1]])
        when:
        assert skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], "userA").body.explanation == "Skill was submitted for approval"
        List<EmailUtils.EmailRes> emails = waitAndCollect(expectedEmails.size() )

        then:
        emails.collect {it.recipients[0] }.sort() == expectedEmails.sort()
    }

    def "1 skill is configured - notify matched admin"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSelfReportSkills(5,)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

        List<SkillsService> approvers = createAdditionalApprovers(proj, 2)
        skillsService.configureApproverForSkillId(proj.projectId, skillsService.userName, skills[0].skillId)

        List<String> expectedEmails = getEmails([skillsService])
        when:
        assert skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], "userA").body.explanation == "Skill was submitted for approval"
        List<EmailUtils.EmailRes> emails = waitAndCollect(expectedEmails.size() )

        then:
        emails.collect {it.recipients[0] }.sort() == expectedEmails.sort()
    }

    def "1 skill is configured - notify matched fallback admins and approvers"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSelfReportSkills(5,)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

        List<SkillsService> approvers = createAdditionalApprovers(proj, 2)
        skillsService.configureApproverForSkillId(proj.projectId, approvers[0].userName, skills[0].skillId)

        List<String> expectedEmails = getEmails([approvers[1], skillsService])
        when:
        assert skillsService.addSkill([projectId: proj.projectId, skillId: skills[1].skillId], "userA").body.explanation == "Skill was submitted for approval"
        List<EmailUtils.EmailRes> emails = waitAndCollect(expectedEmails.size() )

        then:
        emails.collect {it.recipients[0] }.sort() == expectedEmails.sort()
    }

    def "1 user is configured - notify matched approver"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSelfReportSkills(5,)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
        def subj2 = SkillsFactory.createSubject(1, 2)
        def subj2_skills = SkillsFactory.createSkills(3, 1, 2, 100)
        skillsService.createSubject(subj2)
        skillsService.createSkills(subj2_skills)

        List<SkillsService> approvers = createAdditionalApprovers(proj, 2)
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[0].skillId], "userA")
        skillsService.configureApproverForUser(proj.projectId, approvers[0].userName, "userA")

        List<String> expectedEmails = getEmails([approvers[0]])
        when:
        assert skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], "userA").body.explanation == "Skill was submitted for approval"
        List<EmailUtils.EmailRes> emails = waitAndCollect(expectedEmails.size() )

        then:
        emails.collect {it.recipients[0] }.sort() == expectedEmails.sort()
    }

    def "1 user is configured for multiple approvers"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSelfReportSkills(5,)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
        def subj2 = SkillsFactory.createSubject(1, 2)
        def subj2_skills = SkillsFactory.createSkills(3, 1, 2, 100)
        skillsService.createSubject(subj2)
        skillsService.createSkills(subj2_skills)

        List<SkillsService> approvers = createAdditionalApprovers(proj, 3)
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[0].skillId], "userA")
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[0].skillId], "userB")
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[0].skillId], "userC")

        // 1st approver
        skillsService.configureApproverForUser(proj.projectId, approvers[0].userName, "userA")
        // 2nd approver
        skillsService.configureApproverForUser(proj.projectId, approvers[1].userName, "userA")
        skillsService.configureApproverForUser(proj.projectId, approvers[1].userName, "userC")
        // 3rd approver
        skillsService.configureApproverForUser(proj.projectId, approvers[2].userName, "userB")
        skillsService.configureApproverForUser(proj.projectId, approvers[2].userName, "userC")

        List<String> expectedEmails = getEmails([approvers[0], approvers[1]])
        when:
        assert skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], "userA").body.explanation == "Skill was submitted for approval"
        List<EmailUtils.EmailRes> emails = waitAndCollect(expectedEmails.size() )

        then:
        emails.collect {it.recipients[0] }.sort() == expectedEmails.sort()
    }

    def "1 user is configured - notify matched fallback admins and approvers"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSelfReportSkills(5,)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
        def subj2 = SkillsFactory.createSubject(1, 2)
        def subj2_skills = SkillsFactory.createSkills(3, 1, 2, 100)
        skillsService.createSubject(subj2)
        skillsService.createSkills(subj2_skills)

        List<SkillsService> approvers = createAdditionalApprovers(proj, 2)
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[0].skillId], "userA")
        skillsService.configureApproverForUser(proj.projectId, approvers[0].userName, "userA")

        List<String> expectedEmails = getEmails([approvers[1], skillsService])
        when:
        assert skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], "userB").body.explanation == "Skill was submitted for approval"
        List<EmailUtils.EmailRes> emails = waitAndCollect(expectedEmails.size() )

        then:
        emails.collect {it.recipients[0] }.sort() == expectedEmails.sort()
    }

    def "1 user tag is configured - notify matched approver"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSelfReportSkills(5,)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
        def subj2 = SkillsFactory.createSubject(1, 2)
        def subj2_skills = SkillsFactory.createSkills(3, 1, 2, 100)
        skillsService.createSubject(subj2)
        skillsService.createSkills(subj2_skills)

        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[0].skillId], "userA")

        SkillsService rootUser = createRootSkillService()
        String userTagKey = "key1"
        rootUser.saveUserTag("userA", userTagKey, ["aBcD"])
        rootUser.saveUserTag("userA", userTagKey, ["efgh"])

        List<SkillsService> approvers = createAdditionalApprovers(proj, 2)
        skillsService.configureApproverForUserTag(proj.projectId, approvers[0].userName, userTagKey, "AbCd")

        List<String> expectedEmails = getEmails([approvers[0]])
        when:
        assert skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], "userA").body.explanation == "Skill was submitted for approval"
        List<EmailUtils.EmailRes> emails = waitAndCollect(expectedEmails.size() )

        then:
        emails.collect {it.recipients[0] }.sort() == expectedEmails.sort()
    }

    def "1 user tag is configured - notify matched approver - match starts with"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSelfReportSkills(5,)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
        def subj2 = SkillsFactory.createSubject(1, 2)
        def subj2_skills = SkillsFactory.createSkills(3, 1, 2, 100)
        skillsService.createSubject(subj2)
        skillsService.createSkills(subj2_skills)

        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[0].skillId], "userA")

        SkillsService rootUser = createRootSkillService()
        String userTagKey = "key1"
        rootUser.saveUserTag("userA", userTagKey, ["aBcD"])
        rootUser.saveUserTag("userA", userTagKey, ["efgh"])

        List<SkillsService> approvers = createAdditionalApprovers(proj, 2)
        skillsService.configureApproverForUserTag(proj.projectId, approvers[0].userName, userTagKey, "Ab")

        List<String> expectedEmails = getEmails([approvers[0]])
        when:
        assert skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], "userA").body.explanation == "Skill was submitted for approval"
        List<EmailUtils.EmailRes> emails = waitAndCollect(expectedEmails.size() )

        then:
        emails.collect {it.recipients[0] }.sort() == expectedEmails.sort()
    }

    def "1 user tag is configured to match multiple approvers - match starts with"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSelfReportSkills(5,)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
        def subj2 = SkillsFactory.createSubject(1, 2)
        def subj2_skills = SkillsFactory.createSkills(3, 1, 2, 100)
        skillsService.createSubject(subj2)
        skillsService.createSkills(subj2_skills)

        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[0].skillId], "userA")

        SkillsService rootUser = createRootSkillService()
        String userTagKey = "key1"
        rootUser.saveUserTag("userA", userTagKey, ["aBcD"])
        rootUser.saveUserTag("userA", userTagKey, ["efgh"])

        List<SkillsService> approvers = createAdditionalApprovers(proj, 3)
        // approver 1
        skillsService.configureApproverForUserTag(proj.projectId, approvers[0].userName, userTagKey, "Ab")
        skillsService.configureApproverForUserTag(proj.projectId, approvers[0].userName, userTagKey, "efgh")
        // approver 2
        skillsService.configureApproverForUserTag(proj.projectId, approvers[1].userName, userTagKey, "aBc")
        skillsService.configureApproverForUserTag(proj.projectId, approvers[1].userName, userTagKey, "deaef")
        skillsService.configureApproverForUserTag(proj.projectId, approvers[1].userName, userTagKey, "B")
        // approver 3
        skillsService.configureApproverForUserTag(proj.projectId, approvers[2].userName, userTagKey, "deaef")
        skillsService.configureApproverForUserTag(proj.projectId, approvers[2].userName, userTagKey, "B")

        List<String> expectedEmails = getEmails([approvers[0], approvers[1]])
        when:
        assert skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], "userA").body.explanation == "Skill was submitted for approval"
        List<EmailUtils.EmailRes> emails = waitAndCollect(expectedEmails.size() )

        then:
        emails.collect {it.recipients[0] }.sort() == expectedEmails.sort()
    }

    def "1 user tag is configured - notify fallback approvers"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSelfReportSkills(5,)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
        def subj2 = SkillsFactory.createSubject(1, 2)
        def subj2_skills = SkillsFactory.createSkills(3, 1, 2, 100)
        skillsService.createSubject(subj2)
        skillsService.createSkills(subj2_skills)

        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[0].skillId], "userA")

        SkillsService rootUser = createRootSkillService()
        String userTagKey = "key1"
        rootUser.saveUserTag("userA", userTagKey, ["abcd"])
        rootUser.saveUserTag("userA", userTagKey, ["efgh"])

        List<SkillsService> approvers = createAdditionalApprovers(proj, 2)
        skillsService.configureApproverForUserTag(proj.projectId, approvers[0].userName, userTagKey, "abcd")

        List<String> expectedEmails = getEmails([approvers[1], skillsService])
        when:
        assert skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], "userB").body.explanation == "Skill was submitted for approval"
        List<EmailUtils.EmailRes> emails = waitAndCollect(expectedEmails.size() )

        then:
        emails.collect {it.recipients[0] }.sort() == expectedEmails.sort()
    }

    private List<EmailUtils.EmailRes> waitAndCollect(int expectedNumEmails) {
        WaitFor.wait { greenMail.getReceivedMessages().size() == expectedNumEmails }
        if( greenMail.getReceivedMessages().size() != expectedNumEmails) {
            String emails = greenMail.getReceivedMessages().collect {"${it.from}: ${it.subject}" }.join("\n")
            log.error("Number of emails were different. Actual emails:\n {}", emails)

            assert greenMail.getReceivedMessages().size() != expectedNumEmails
        }
        // wait an additional 500ms in case additional and rogue emails arrive
        Thread.sleep(500)
        List<EmailUtils.EmailRes> emails = EmailUtils.getEmails(greenMail)
        return emails
    }

    private List<String> getEmails(List<SkillsService> services, SkillsService ... additionalServices) {
        List<SkillsService> all = additionalServices ? [services, additionalServices].flatten() : services
        return all.collect { userAttrsRepo.findByUserId(it.userName).email }
    }

    private List createAdditionalApprovers(def proj, int numUsers) {
        List<String> users = getRandomUsers(numUsers, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])
        List res = users.collect {
            def userService = createService(it)
            skillsService.addUserRole(userService.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())
            return userService
        }
        //  will need this once the 'emailing when approver role is added' was implemented
        //  WaitFor.wait { greenMail.getReceivedMessages().size() == 2 }
        //  greenMail.purgeEmailFromAllMailboxes()
        return res
    }


}
