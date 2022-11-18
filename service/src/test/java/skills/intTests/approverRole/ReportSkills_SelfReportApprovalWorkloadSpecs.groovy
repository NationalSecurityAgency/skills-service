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
        List<EmailUtils.EmailRes> emails = WaitFor.waitAndCollectEmails(greenMail, expectedEmails.size() )

        then:
        emails.collect {it.recipients[0] }.sort() == expectedEmails.sort()
    }

    def "no conf at all - notify all admins and approvers - approver unsubscribed"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSelfReportSkills(5,)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

        List<SkillsService> approvers = createAdditionalApprovers(proj, 2)
        approvers[1].unsubscribeFromSelfApprovalRequestEmails(proj.projectId)

        List<String> expectedEmails = getEmails([approvers[0], skillsService])
        when:
        assert skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], "userA").body.explanation == "Skill was submitted for approval"
        List<EmailUtils.EmailRes> emails = WaitFor.waitAndCollectEmails(greenMail, expectedEmails.size() )

        then:
        emails.collect {it.recipients[0] }.sort() == expectedEmails.sort()
    }

    def "no conf at all - notify all admins and approvers - approver unsubscribed then re-subscribed"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSelfReportSkills(5,)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

        List<SkillsService> approvers = createAdditionalApprovers(proj, 2)
        approvers[1].unsubscribeFromSelfApprovalRequestEmails(proj.projectId)
        approvers[1].subscribeToSelfApprovalRequestEmails(proj.projectId)

        List<String> expectedEmails = getEmails(approvers, skillsService)
        when:
        assert skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], "userA").body.explanation == "Skill was submitted for approval"
        List<EmailUtils.EmailRes> emails = WaitFor.waitAndCollectEmails(greenMail, expectedEmails.size() )

        then:
        emails.collect {it.recipients[0] }.sort() == expectedEmails.sort()
    }

    def "no conf at all - all admins and approvers unsubscribed"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSelfReportSkills(5,)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

        List<SkillsService> approvers = createAdditionalApprovers(proj, 2)
        approvers[0].unsubscribeFromSelfApprovalRequestEmails(proj.projectId)
        approvers[1].unsubscribeFromSelfApprovalRequestEmails(proj.projectId)
        skillsService.unsubscribeFromSelfApprovalRequestEmails(proj.projectId)

        when:
        assert skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], "userA").body.explanation == "Skill was submitted for approval"
        Thread.sleep(1000)
        List<EmailUtils.EmailRes> emails = WaitFor.waitAndCollectEmails(greenMail, 0 )

        then:
        !emails
    }

    def "1 explicit fallback approver"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSelfReportSkills(5,)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

        List<SkillsService> approvers = createAdditionalApprovers(proj, 3)
        skillsService.configureFallbackApprover(proj.projectId, approvers[1].userName)
        List<String> expectedEmails = getEmails([approvers[1]])
        when:
        assert skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], "userA").body.explanation == "Skill was submitted for approval"
        List<EmailUtils.EmailRes> emails = WaitFor.waitAndCollectEmails(greenMail, expectedEmails.size() )

        then:
        emails.collect {it.recipients[0] }.sort() == expectedEmails.sort()
    }

    def "2 explicit fallback approver"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSelfReportSkills(5,)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

        List<SkillsService> approvers = createAdditionalApprovers(proj, 4)
        skillsService.configureFallbackApprover(proj.projectId, approvers[1].userName)
        skillsService.configureFallbackApprover(proj.projectId, approvers[3].userName)
        List<String> expectedEmails = getEmails([approvers[1], approvers[3]])
        when:
        assert skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], "userA").body.explanation == "Skill was submitted for approval"
        List<EmailUtils.EmailRes> emails = WaitFor.waitAndCollectEmails(greenMail, expectedEmails.size() )

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
        List<EmailUtils.EmailRes> emails = WaitFor.waitAndCollectEmails(greenMail, expectedEmails.size() )

        then:
        emails.collect {it.recipients[0] }.sort() == expectedEmails.sort()
    }

    def "skills are configured for multiple approvers"() {
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
        List<EmailUtils.EmailRes> emails = WaitFor.waitAndCollectEmails(greenMail, expectedEmails.size() )

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
        List<EmailUtils.EmailRes> emails = WaitFor.waitAndCollectEmails(greenMail, expectedEmails.size() )

        then:
        emails.collect {it.recipients[0] }.sort() == expectedEmails.sort()
    }

    def "1 skill is configured - notify matched implicit fallback admins and approvers"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSelfReportSkills(5,)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

        List<SkillsService> approvers = createAdditionalApprovers(proj, 2)
        skillsService.configureApproverForSkillId(proj.projectId, approvers[0].userName, skills[0].skillId)

        List<String> expectedEmails = getEmails([approvers[1], skillsService])
        when:
        assert skillsService.addSkill([projectId: proj.projectId, skillId: skills[1].skillId], "userA").body.explanation == "Skill was submitted for approval"
        List<EmailUtils.EmailRes> emails = WaitFor.waitAndCollectEmails(greenMail, expectedEmails.size() )

        then:
        emails.collect {it.recipients[0] }.sort() == expectedEmails.sort()
    }

    def "1 skill is configured - notify matched explicit fallback approvers"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSelfReportSkills(5,)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

        List<SkillsService> approvers = createAdditionalApprovers(proj, 2)
        skillsService.configureApproverForSkillId(proj.projectId, approvers[0].userName, skills[0].skillId)

        skillsService.configureFallbackApprover(proj.projectId, approvers[1].userName)

        List<String> expectedEmails = getEmails([approvers[1]])
        when:
        assert skillsService.addSkill([projectId: proj.projectId, skillId: skills[1].skillId], "userA").body.explanation == "Skill was submitted for approval"
        List<EmailUtils.EmailRes> emails = WaitFor.waitAndCollectEmails(greenMail, expectedEmails.size() )

        then:
        emails.collect {it.recipients[0] }.sort() == expectedEmails.sort()
    }

    def "1 skill is configured - notify matched explicit fallback admin and approvers"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSelfReportSkills(5,)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

        List<SkillsService> approvers = createAdditionalApprovers(proj, 2)
        skillsService.configureApproverForSkillId(proj.projectId, approvers[0].userName, skills[0].skillId)

        skillsService.configureFallbackApprover(proj.projectId, approvers[1].userName)
        skillsService.configureFallbackApprover(proj.projectId, skillsService.userName)

        List<String> expectedEmails = getEmails([approvers[1], skillsService])
        when:
        assert skillsService.addSkill([projectId: proj.projectId, skillId: skills[1].skillId], "userA").body.explanation == "Skill was submitted for approval"
        List<EmailUtils.EmailRes> emails = WaitFor.waitAndCollectEmails(greenMail, expectedEmails.size() )

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
        List<EmailUtils.EmailRes> emails = WaitFor.waitAndCollectEmails(greenMail, expectedEmails.size() )

        then:
        emails.collect {it.recipients[0] }.sort() == expectedEmails.sort()
    }

    def "users are configured for multiple approvers"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSelfReportSkills(5,)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
        def subj2 = SkillsFactory.createSubject(1, 2)
        def subj2_skills = SkillsFactory.createSkills(3, 1, 2, 100)
        skillsService.createSubject(subj2)
        skillsService.createSkills(subj2_skills)

        List<String> randomUsers = getRandomUsers(6, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])
        def (String userA, String userB, String userC) = [randomUsers[3], randomUsers[4], randomUsers[5]]
        List<SkillsService> approvers = createAdditionalApprovers(proj, 3, randomUsers)
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[0].skillId], userA)
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[0].skillId], userB)
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[0].skillId], userC)

        // 1st approver
        skillsService.configureApproverForUser(proj.projectId, approvers[0].userName, userA)
        // 2nd approver
        skillsService.configureApproverForUser(proj.projectId, approvers[1].userName, userA)
        skillsService.configureApproverForUser(proj.projectId, approvers[1].userName, userC)
        // 3rd approver
        skillsService.configureApproverForUser(proj.projectId, approvers[2].userName, userB)
        skillsService.configureApproverForUser(proj.projectId, approvers[2].userName, userC)

        List<String> expectedEmails = getEmails([approvers[0], approvers[1]])
        when:
        assert skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userA).body.explanation == "Skill was submitted for approval"
        List<EmailUtils.EmailRes> emails = WaitFor.waitAndCollectEmails(greenMail, expectedEmails.size() )

        then:
        emails.collect {it.recipients[0] }.sort() == expectedEmails.sort()
    }

    def "1 user is configured - notify matched implicit fallback admins and approvers"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSelfReportSkills(5,)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
        def subj2 = SkillsFactory.createSubject(1, 2)
        def subj2_skills = SkillsFactory.createSkills(3, 1, 2, 100)
        skillsService.createSubject(subj2)
        skillsService.createSkills(subj2_skills)

        List<String> randomUsers = getRandomUsers(6, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])
        def (String userA, String userB) = [randomUsers[2], randomUsers[3]]
        List<SkillsService> approvers = createAdditionalApprovers(proj, 2, randomUsers)
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[0].skillId], userA)
        skillsService.configureApproverForUser(proj.projectId, approvers[0].userName, userA)

        List<String> expectedEmails = getEmails([approvers[1], skillsService])
        when:
        assert skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userB).body.explanation == "Skill was submitted for approval"
        List<EmailUtils.EmailRes> emails = WaitFor.waitAndCollectEmails(greenMail, expectedEmails.size() )

        then:
        emails.collect {it.recipients[0] }.sort() == expectedEmails.sort()
    }

    def "1 user is configured - notify matched explicit fallback admins and approvers"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSelfReportSkills(5,)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
        def subj2 = SkillsFactory.createSubject(1, 2)
        def subj2_skills = SkillsFactory.createSkills(3, 1, 2, 100)
        skillsService.createSubject(subj2)
        skillsService.createSkills(subj2_skills)

        List<String> randomUsers = getRandomUsers(6, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])
        def (String userA, String userB) = [randomUsers[2], randomUsers[3]]
        List<SkillsService> approvers = createAdditionalApprovers(proj, 2, randomUsers)
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[0].skillId], userA)
        skillsService.configureApproverForUser(proj.projectId, approvers[0].userName, userA)
        skillsService.configureFallbackApprover(proj.projectId, approvers[1].userName)
        skillsService.configureFallbackApprover(proj.projectId, skillsService.userName)

        List<String> expectedEmails = getEmails([approvers[1], skillsService])
        when:
        assert skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userB).body.explanation == "Skill was submitted for approval"
        List<EmailUtils.EmailRes> emails = WaitFor.waitAndCollectEmails(greenMail, expectedEmails.size() )

        then:
        emails.collect {it.recipients[0] }.sort() == expectedEmails.sort()
    }

    def "1 user is configured - notify matched explicit fallback approvers"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSelfReportSkills(5,)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
        def subj2 = SkillsFactory.createSubject(1, 2)
        def subj2_skills = SkillsFactory.createSkills(3, 1, 2, 100)
        skillsService.createSubject(subj2)
        skillsService.createSkills(subj2_skills)

        List<String> randomUsers = getRandomUsers(6, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])
        def (String userA, String userB) = [randomUsers[2], randomUsers[3]]
        List<SkillsService> approvers = createAdditionalApprovers(proj, 2, randomUsers)
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[0].skillId], userA)
        skillsService.configureApproverForUser(proj.projectId, approvers[0].userName, userA)
        skillsService.configureFallbackApprover(proj.projectId, approvers[1].userName)

        List<String> expectedEmails = getEmails([approvers[1]])
        when:
        assert skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userB).body.explanation == "Skill was submitted for approval"
        List<EmailUtils.EmailRes> emails = WaitFor.waitAndCollectEmails(greenMail, expectedEmails.size() )

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
        List<EmailUtils.EmailRes> emails = WaitFor.waitAndCollectEmails(greenMail, expectedEmails.size() )

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

        List<String> randomUsers = getRandomUsers(6, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])
        def (String userA, String userB) = [randomUsers[2], randomUsers[3]]

        String userTagKey = "key1"
        List<SkillsService> approvers = createAdditionalApprovers(proj, 2, randomUsers)
        skillsService.configureApproverForUserTag(proj.projectId, approvers[0].userName, userTagKey, "Ab")

        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[0].skillId], userA)

        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(userA, userTagKey, ["aBcD"])
        rootUser.saveUserTag(userA, userTagKey, ["efgh"])

        List<String> expectedEmails = getEmails([approvers[0]])
        when:
        assert skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userA).body.explanation == "Skill was submitted for approval"
        List<EmailUtils.EmailRes> emails = WaitFor.waitAndCollectEmails(greenMail, expectedEmails.size() )

        then:
        emails.collect {it.recipients[0] }.sort() == expectedEmails.sort()
    }

    def "user tags are configured to match multiple approvers - match starts with"() {
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
        List<EmailUtils.EmailRes> emails = WaitFor.waitAndCollectEmails(greenMail, expectedEmails.size() )

        then:
        emails.collect {it.recipients[0] }.sort() == expectedEmails.sort()
    }

    def "1 user tag is configured - notify implicit fallback approvers"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSelfReportSkills(5,)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
        def subj2 = SkillsFactory.createSubject(1, 2)
        def subj2_skills = SkillsFactory.createSkills(3, 1, 2, 100)
        skillsService.createSubject(subj2)
        skillsService.createSkills(subj2_skills)

        List<String> randomUsers = getRandomUsers(6, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])
        def (String userA, String userB) = [randomUsers[2], randomUsers[3]]

        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[0].skillId], userA)

        SkillsService rootUser = createRootSkillService()
        String userTagKey = "key1"
        rootUser.saveUserTag(userA, userTagKey, ["abcd"])
        rootUser.saveUserTag(userA, userTagKey, ["efgh"])

        List<SkillsService> approvers = createAdditionalApprovers(proj, 2, randomUsers)
        skillsService.configureApproverForUserTag(proj.projectId, approvers[0].userName, userTagKey, "abcd")

        List<String> expectedEmails = getEmails([approvers[1], skillsService])
        when:
        assert skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userB).body.explanation == "Skill was submitted for approval"
        List<EmailUtils.EmailRes> emails = WaitFor.waitAndCollectEmails(greenMail, expectedEmails.size() )

        then:
        emails.collect {it.recipients[0] }.sort() == expectedEmails.sort()
    }

    def "1 user tag is configured - notify explicit fallback approvers and admins"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSelfReportSkills(5,)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
        def subj2 = SkillsFactory.createSubject(1, 2)
        def subj2_skills = SkillsFactory.createSkills(3, 1, 2, 100)
        skillsService.createSubject(subj2)
        skillsService.createSkills(subj2_skills)

        List<String> randomUsers = getRandomUsers(6, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])
        def (String userA, String userB) = [randomUsers[2], randomUsers[3]]

        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[0].skillId], userA)

        SkillsService rootUser = createRootSkillService()
        String userTagKey = "key1"
        rootUser.saveUserTag(userA, userTagKey, ["abcd"])
        rootUser.saveUserTag(userA, userTagKey, ["efgh"])


        List<SkillsService> approvers = createAdditionalApprovers(proj, 2, randomUsers)
        skillsService.configureApproverForUserTag(proj.projectId, approvers[0].userName, userTagKey, "abcd")
        skillsService.configureFallbackApprover(proj.projectId, approvers[1].userName)
        skillsService.configureFallbackApprover(proj.projectId, skillsService.userName)

        List<String> expectedEmails = getEmails([approvers[1], skillsService])
        when:
        assert skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userB).body.explanation == "Skill was submitted for approval"
        List<EmailUtils.EmailRes> emails = WaitFor.waitAndCollectEmails(greenMail, expectedEmails.size() )

        then:
        emails.collect {it.recipients[0] }.sort() == expectedEmails.sort()
    }

    def "1 user tag is configured - notify explicit fallback admin"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSelfReportSkills(5,)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
        def subj2 = SkillsFactory.createSubject(1, 2)
        def subj2_skills = SkillsFactory.createSkills(3, 1, 2, 100)
        skillsService.createSubject(subj2)
        skillsService.createSkills(subj2_skills)

        List<String> randomUsers = getRandomUsers(6, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])
        def (String userA, String userB) = [randomUsers[2], randomUsers[3]]

        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[0].skillId], userA)

        SkillsService rootUser = createRootSkillService()
        String userTagKey = "key1"
        rootUser.saveUserTag(userA, userTagKey, ["abcd"])
        rootUser.saveUserTag(userA, userTagKey, ["efgh"])

        List<SkillsService> approvers = createAdditionalApprovers(proj, 2, randomUsers)
        skillsService.configureApproverForUserTag(proj.projectId, approvers[0].userName, userTagKey, "abcd")
        skillsService.configureFallbackApprover(proj.projectId, skillsService.userName)

        List<String> expectedEmails = getEmails([skillsService])
        when:
        assert skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userB).body.explanation == "Skill was submitted for approval"
        List<EmailUtils.EmailRes> emails = WaitFor.waitAndCollectEmails(greenMail, expectedEmails.size() )

        then:
        emails.collect {it.recipients[0] }.sort() == expectedEmails.sort()
    }

    def "multiple approvers are matched based on different configs"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSelfReportSkills(5,)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
        def subj2 = SkillsFactory.createSubject(1, 2)
        def subj2_skills = SkillsFactory.createSkills(3, 1, 2, 100)
        skillsService.createSubject(subj2)
        skillsService.createSkills(subj2_skills)

        List<String> randomUsers = getRandomUsers(8, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])
        def (String userA, String userB) = [randomUsers[5], randomUsers[6]]

        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[0].skillId], userA)
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[0].skillId], userB)

        SkillsService rootUser = createRootSkillService()
        String userTagKey = "key1"
        rootUser.saveUserTag(userA, userTagKey, ["aBcD"])
        rootUser.saveUserTag(userA, userTagKey, ["efgh"])

        List<SkillsService> approvers = createAdditionalApprovers(proj, 5, randomUsers)
        // approver 1
        skillsService.configureApproverForUserTag(proj.projectId, approvers[0].userName, userTagKey, "Ab")
        // approver 2
        skillsService.configureApproverForSkillId(proj.projectId, approvers[1].userName, skills[0].skillId)
        // approver 3
        skillsService.configureApproverForUser(proj.projectId, approvers[2].userName, userA)
        // approver 4 - wont match anything
        skillsService.configureApproverForUserTag(proj.projectId, approvers[3].userName, userTagKey, "A1b")
        skillsService.configureApproverForSkillId(proj.projectId, approvers[3].userName, skills[1].skillId)
        skillsService.configureApproverForUser(proj.projectId, approvers[3].userName, userB)


        List<String> expectedEmails = getEmails([approvers[0], approvers[1], approvers[2]])
        when:
        assert skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userA).body.explanation == "Skill was submitted for approval"
        List<EmailUtils.EmailRes> emails = WaitFor.waitAndCollectEmails(greenMail, expectedEmails.size() )

        then:
        emails.collect {it.recipients[0] }.sort() == expectedEmails.sort()
    }

    def "multiple approvers are matched based on different configs where each approver matches on all the config options"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSelfReportSkills(5,)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
        def subj2 = SkillsFactory.createSubject(1, 2)
        def subj2_skills = SkillsFactory.createSkills(3, 1, 2, 100)
        skillsService.createSubject(subj2)
        skillsService.createSkills(subj2_skills)

        List<String> randomUsers = getRandomUsers(8, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])
        def (String userA, String userB) = [randomUsers[5], randomUsers[6]]

        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[0].skillId], userA)
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[0].skillId], userB)

        SkillsService rootUser = createRootSkillService()
        String userTagKey = "key1"
        rootUser.saveUserTag(userA, userTagKey, ["aBcD"])
        rootUser.saveUserTag(userA, userTagKey, ["efgh"])

        List<SkillsService> approvers = createAdditionalApprovers(proj, 5, randomUsers)
        // approver 1
        skillsService.configureApproverForUserTag(proj.projectId, approvers[0].userName, userTagKey, "Ab")
        skillsService.configureApproverForSkillId(proj.projectId, approvers[0].userName, skills[0].skillId)
        skillsService.configureApproverForUser(proj.projectId, approvers[0].userName, userA)
        // approver 2
        skillsService.configureApproverForUserTag(proj.projectId, approvers[1].userName, userTagKey, "A")
        skillsService.configureApproverForSkillId(proj.projectId, approvers[1].userName, skills[0].skillId)
        skillsService.configureApproverForUser(proj.projectId, approvers[1].userName, userA)
        // approver 3
        skillsService.configureApproverForUserTag(proj.projectId, approvers[2].userName, userTagKey, "Abc")
        skillsService.configureApproverForSkillId(proj.projectId, approvers[2].userName, skills[0].skillId)
        skillsService.configureApproverForUser(proj.projectId, approvers[2].userName, userA)

        // approver 4 - wont match anything
        skillsService.configureApproverForUserTag(proj.projectId, approvers[3].userName, userTagKey, "A1b")
        skillsService.configureApproverForSkillId(proj.projectId, approvers[3].userName, skills[1].skillId)
        skillsService.configureApproverForUser(proj.projectId, approvers[3].userName, userB)

        List<String> expectedEmails = getEmails([approvers[0], approvers[1], approvers[2]])
        when:
        assert skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userA).body.explanation == "Skill was submitted for approval"
        List<EmailUtils.EmailRes> emails = WaitFor.waitAndCollectEmails(greenMail, expectedEmails.size() )

        then:
        emails.collect {it.recipients[0] }.sort() == expectedEmails.sort()
    }

    def "multiple approvers are matched based on different configs - only 2 match"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSelfReportSkills(5,)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
        def subj2 = SkillsFactory.createSubject(1, 2)
        def subj2_skills = SkillsFactory.createSkills(3, 1, 2, 100)
        skillsService.createSubject(subj2)
        skillsService.createSkills(subj2_skills)

        List<String> randomUsers = getRandomUsers(8, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])
        def (String userA, String userB) = [randomUsers[5], randomUsers[6]]

        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[0].skillId], userA)
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[0].skillId], userB)

        SkillsService rootUser = createRootSkillService()
        String userTagKey = "key1"
        rootUser.saveUserTag(userA, userTagKey, ["aBcD"])
        rootUser.saveUserTag(userA, userTagKey, ["efgh"])

        List<SkillsService> approvers = createAdditionalApprovers(proj, 5, randomUsers)
        // approver 1
        skillsService.configureApproverForUserTag(proj.projectId, approvers[0].userName, userTagKey, "Ab")
        // approver 2
        skillsService.configureApproverForSkillId(proj.projectId, approvers[1].userName, skills[0].skillId)
        // approver 3
        skillsService.configureApproverForUser(proj.projectId, approvers[2].userName, userB)
        // approver 4 - wont match anything
        skillsService.configureApproverForUserTag(proj.projectId, approvers[3].userName, userTagKey, "A1b")
        skillsService.configureApproverForSkillId(proj.projectId, approvers[3].userName, skills[1].skillId)
        skillsService.configureApproverForUser(proj.projectId, approvers[3].userName, userB)

        List<String> expectedEmails = getEmails([approvers[0], approvers[1]])
        when:
        assert skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userA).body.explanation == "Skill was submitted for approval"
        List<EmailUtils.EmailRes> emails = WaitFor.waitAndCollectEmails(greenMail, expectedEmails.size() )

        then:
        emails.collect {it.recipients[0] }.sort() == expectedEmails.sort()
    }

    def "one approver is matched based on different configs"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSelfReportSkills(5,)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
        def subj2 = SkillsFactory.createSubject(1, 2)
        def subj2_skills = SkillsFactory.createSkills(3, 1, 2, 100)
        skillsService.createSubject(subj2)
        skillsService.createSkills(subj2_skills)

        List<String> randomUsers = getRandomUsers(8, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])
        def (String userA, String userB) = [randomUsers[5], randomUsers[6]]

        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[0].skillId], userA)
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[0].skillId], userB)

        SkillsService rootUser = createRootSkillService()
        String userTagKey = "key1"
        rootUser.saveUserTag(userA, userTagKey, ["aBcD"])
        rootUser.saveUserTag(userA, userTagKey, ["efgh"])

        List<SkillsService> approvers = createAdditionalApprovers(proj, 5, randomUsers)
        // approver 1
        skillsService.configureApproverForUserTag(proj.projectId, approvers[0].userName, userTagKey, "Ab")
        // approver 2
        skillsService.configureApproverForSkillId(proj.projectId, approvers[1].userName, skills[1].skillId)
        // approver 3
        skillsService.configureApproverForUser(proj.projectId, approvers[2].userName, userB)
        // approver 4 - wont match anything
        skillsService.configureApproverForUserTag(proj.projectId, approvers[3].userName, userTagKey, "A1b")
        skillsService.configureApproverForSkillId(proj.projectId, approvers[3].userName, skills[1].skillId)
        skillsService.configureApproverForUser(proj.projectId, approvers[3].userName, userB)

        List<String> expectedEmails = getEmails([approvers[0]])
        when:
        assert skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userA).body.explanation == "Skill was submitted for approval"
        List<EmailUtils.EmailRes> emails = WaitFor.waitAndCollectEmails(greenMail, expectedEmails.size() )

        then:
        emails.collect {it.recipients[0] }.sort() == expectedEmails.sort()
    }

    def "implicit fallback approvers when multiple configs did not match"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSelfReportSkills(5,)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
        def subj2 = SkillsFactory.createSubject(1, 2)
        def subj2_skills = SkillsFactory.createSkills(3, 1, 2, 100)
        skillsService.createSubject(subj2)
        skillsService.createSkills(subj2_skills)

        List<String> randomUsers = getRandomUsers(8, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])
        def (String userA, String userB) = [randomUsers[5], randomUsers[6]]

        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[0].skillId], userA)
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[0].skillId], userB)

        SkillsService rootUser = createRootSkillService()
        String userTagKey = "key1"
        rootUser.saveUserTag(userA, userTagKey, ["aBcD"])
        rootUser.saveUserTag(userA, userTagKey, ["efgh"])

        List<SkillsService> approvers = createAdditionalApprovers(proj, 5, randomUsers)
        // approver 1
        skillsService.configureApproverForUserTag(proj.projectId, approvers[0].userName, userTagKey, "A1b")
        // approver 2
        skillsService.configureApproverForSkillId(proj.projectId, approvers[1].userName, skills[1].skillId)
        // approver 3
        skillsService.configureApproverForUser(proj.projectId, approvers[2].userName, userB)
        // approver 4 - wont match anything
        skillsService.configureApproverForUserTag(proj.projectId, approvers[3].userName, userTagKey, "A1b")
        skillsService.configureApproverForSkillId(proj.projectId, approvers[3].userName, skills[1].skillId)
        skillsService.configureApproverForUser(proj.projectId, approvers[3].userName, userB)

        List<String> expectedEmails = getEmails([approvers[4], skillsService])
        when:
        assert skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userA).body.explanation == "Skill was submitted for approval"
        List<EmailUtils.EmailRes> emails = WaitFor.waitAndCollectEmails(greenMail, expectedEmails.size() )

        then:
        emails.collect {it.recipients[0] }.sort() == expectedEmails.sort()
    }

    def "explicit fallback multiple approvers when multiple configs did not match"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSelfReportSkills(5,)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
        def subj2 = SkillsFactory.createSubject(1, 2)
        def subj2_skills = SkillsFactory.createSkills(3, 1, 2, 100)
        skillsService.createSubject(subj2)
        skillsService.createSkills(subj2_skills)

        List<String> randomUsers = getRandomUsers(8, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])
        def (String userA, String userB) = [randomUsers[5], randomUsers[6]]

        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[0].skillId], userA)
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[0].skillId], userB)

        SkillsService rootUser = createRootSkillService()
        String userTagKey = "key1"
        rootUser.saveUserTag(userA, userTagKey, ["aBcD"])
        rootUser.saveUserTag(userA, userTagKey, ["efgh"])

        List<SkillsService> approvers = createAdditionalApprovers(proj, 5, randomUsers)
        // approver 1
        skillsService.configureApproverForUserTag(proj.projectId, approvers[0].userName, userTagKey, "A1b")
        // approver 2
        skillsService.configureApproverForSkillId(proj.projectId, approvers[1].userName, skills[1].skillId)
        // approver 3
        skillsService.configureApproverForUser(proj.projectId, approvers[2].userName, userB)
        // approver 4 - wont match anything
        skillsService.configureApproverForUserTag(proj.projectId, approvers[3].userName, userTagKey, "A1b")
        skillsService.configureApproverForSkillId(proj.projectId, approvers[3].userName, skills[1].skillId)
        skillsService.configureApproverForUser(proj.projectId, approvers[3].userName, userB)

        skillsService.configureFallbackApprover(proj.projectId, approvers[4].userName)
        skillsService.configureFallbackApprover(proj.projectId, skillsService.userName)

        List<String> expectedEmails = getEmails([approvers[4], skillsService])
        when:
        assert skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userA).body.explanation == "Skill was submitted for approval"
        List<EmailUtils.EmailRes> emails = WaitFor.waitAndCollectEmails(greenMail, expectedEmails.size() )

        then:
        emails.collect {it.recipients[0] }.sort() == expectedEmails.sort()
    }


    def "explicit fallback single approver when multiple configs did not match"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSelfReportSkills(5,)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
        def subj2 = SkillsFactory.createSubject(1, 2)
        def subj2_skills = SkillsFactory.createSkills(3, 1, 2, 100)
        skillsService.createSubject(subj2)
        skillsService.createSkills(subj2_skills)

        List<String> randomUsers = getRandomUsers(8, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])
        def (String userA, String userB) = [randomUsers[5], randomUsers[6]]

        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[0].skillId], userA)
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[0].skillId], userB)

        SkillsService rootUser = createRootSkillService()
        String userTagKey = "key1"
        rootUser.saveUserTag(userA, userTagKey, ["aBcD"])
        rootUser.saveUserTag(userA, userTagKey, ["efgh"])

        List<SkillsService> approvers = createAdditionalApprovers(proj, 5, randomUsers)
        // approver 1
        skillsService.configureApproverForUserTag(proj.projectId, approvers[0].userName, userTagKey, "A1b")
        // approver 2
        skillsService.configureApproverForSkillId(proj.projectId, approvers[1].userName, skills[1].skillId)
        // approver 3
        skillsService.configureApproverForUser(proj.projectId, approvers[2].userName, userB)
        // approver 4 - wont match anything
        skillsService.configureApproverForUserTag(proj.projectId, approvers[3].userName, userTagKey, "A1b")
        skillsService.configureApproverForSkillId(proj.projectId, approvers[3].userName, skills[1].skillId)
        skillsService.configureApproverForUser(proj.projectId, approvers[3].userName, userB)

        skillsService.configureFallbackApprover(proj.projectId, approvers[4].userName)

        List<String> expectedEmails = getEmails([approvers[4]])
        when:
        assert skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userA).body.explanation == "Skill was submitted for approval"
        List<EmailUtils.EmailRes> emails = WaitFor.waitAndCollectEmails(greenMail, expectedEmails.size() )

        then:
        emails.collect {it.recipients[0] }.sort() == expectedEmails.sort()
    }

    private List<String> getEmails(List<SkillsService> services, SkillsService ... additionalServices) {
        List<SkillsService> all = additionalServices ? [services, additionalServices].flatten() : services
        return all.collect { userAttrsRepo.findByUserId(it.userName).email }
    }

    private List createAdditionalApprovers(def proj, int numUsers, List<String> poolOfRandomUsers = null) {
        List<String> users
        if (poolOfRandomUsers) {
            users = poolOfRandomUsers[0..(numUsers-1)]
        } else {
            users = getRandomUsers(numUsers, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])
        }

        List res = users.collect {
            def userService = createService(it)
            skillsService.addUserRole(userService.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())
            return userService
        }
        // email is sent when approver role is added
        WaitFor.wait { greenMail.getReceivedMessages().size() == numUsers }
        greenMail.purgeEmailFromAllMailboxes()
        return res
    }


}
