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
import skills.controller.request.model.ContactUsersRequest
import skills.controller.request.model.QueryUsersCriteriaRequest
import skills.controller.request.model.SubjectLevelQueryRequest
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.EmailUtils
import skills.intTests.utils.SkillsFactory
import skills.services.ContactUsersService
import skills.utils.WaitFor
import spock.lang.IgnoreRest

class ContactUsersServiceSpec extends DefaultIntSpec {

    @Autowired
    ContactUsersService contactUsersService

    def setup() {
        startEmailServer()
    }

    def "test count query"(){
        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        def subj2 = SkillsFactory.createSubject(1, 2)
        def subj3 = SkillsFactory.createSubject(1, 3)
        def subj4 = SkillsFactory.createSubject(1, 4)

        def badge = SkillsFactory.createBadge()
        badge.enabled = true

        Map skill1 = [projectId: proj.projectId, subjectId: subj.subjectId, skillId: "skill1", name  : "Test Skill 1", type: "Skill",
                      pointIncrement: 100, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]

        Map skill2 = [projectId: proj.projectId, subjectId: subj2.subjectId, skillId: "skill2", name  : "Test Skill 2", type: "Skill",
                      pointIncrement: 100, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]

        Map skill3 = [projectId: proj.projectId, subjectId: subj3.subjectId, skillId: "skill3", name  : "Test Skill 3", type: "Skill",
                      pointIncrement: 100, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]

        Map skill4 = [projectId: proj.projectId, subjectId: subj3.subjectId, skillId: "skill4", name  : "Test Skill 4", type: "Skill",
                      pointIncrement: 100, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]


        Map skill5 = [projectId: proj.projectId, subjectId: subj4.subjectId, skillId: "skill5", name  : "Test Skill 5", type: "Skill",
                      pointIncrement: 50, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]

        Map skill6 = [projectId: proj.projectId, subjectId: subj4.subjectId, skillId: "skill6", name  : "Test Skill 6", type: "Skill",
                      pointIncrement: 50, numPerformToCompletion: 2, pointIncrementInterval: 0, numMaxOccurrencesIncrementInterval: 1]

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSubject(subj2)
        skillsService.createSubject(subj3)
        skillsService.createSubject(subj4)
        skillsService.createSkill(skill1)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)
        skillsService.createSkill(skill5)
        skillsService.createSkill(skill6)
        skillsService.createBadge(badge)

        [skill2.skillId, skill3.skillId, skill6.skillId].each {
            skillsService.assignSkillToBadge(proj.projectId, badge.badgeId, it)
        }

        def users = getRandomUsers(7)

        skillsService.addSkill(skill1, users[0])
        skillsService.addSkill(skill2, users[0])
        skillsService.addSkill(skill3, users[0])

        skillsService.addSkill(skill2, users[1])

        skillsService.addSkill(skill2, users[2])
        skillsService.addSkill(skill3, users[2])
        skillsService.addSkill(skill5, users[2])
        skillsService.addSkill(skill6, users[2])
        skillsService.addSkill(skill6, users[2])

        skillsService.addSkill(skill2, users[3])
        skillsService.addSkill(skill3, users[3])
        skillsService.addSkill(skill6, users[3])
        skillsService.addSkill(skill6, users[3])

        skillsService.addSkill(skill6, users[4])
        skillsService.addSkill(skill6, users[5])
        skillsService.addSkill(skill6, users[6])

        QueryUsersCriteriaRequest projectLevelSubjectLevelThreeSkills = new QueryUsersCriteriaRequest()
        projectLevelSubjectLevelThreeSkills.projectId = proj.projectId
        projectLevelSubjectLevelThreeSkills.projectLevel = 2
        projectLevelSubjectLevelThreeSkills.achievedSkillIds = [skill1.skillId, skill2.skillId, skill3.skillId]
        projectLevelSubjectLevelThreeSkills.subjectLevels = [new SubjectLevelQueryRequest(subjectId: subj3.subjectId, level: 1)]

        QueryUsersCriteriaRequest skillNotSkill = new QueryUsersCriteriaRequest()
        skillNotSkill.projectId = proj.projectId
        skillNotSkill.achievedSkillIds = [skill6.skillId]
        skillNotSkill.notAchievedSkillIds = [skill5.skillId]

        QueryUsersCriteriaRequest onlyFullyAchievedShouldCount = new QueryUsersCriteriaRequest()
        onlyFullyAchievedShouldCount.projectId = proj.projectId
        onlyFullyAchievedShouldCount.achievedSkillIds = [skill6.skillId]

        QueryUsersCriteriaRequest badgeNotSkill = new QueryUsersCriteriaRequest()
        badgeNotSkill.projectId = proj.projectId
        badgeNotSkill.badgeIds = [badge.badgeId]
        badgeNotSkill.notAchievedSkillIds = [skill5.skillId]

        QueryUsersCriteriaRequest twoSkills = new QueryUsersCriteriaRequest()
        twoSkills.projectId = proj.projectId
        twoSkills.achievedSkillIds = [skill2.skillId, skill3.skillId]

        QueryUsersCriteriaRequest allProjectUsers = new QueryUsersCriteriaRequest()
        allProjectUsers.projectId = proj.projectId
        allProjectUsers.allProjectUsers = true

        QueryUsersCriteriaRequest subjectLevelButNotSkillsInSubject = new QueryUsersCriteriaRequest()
        subjectLevelButNotSkillsInSubject.projectId = proj.projectId
        subjectLevelButNotSkillsInSubject.subjectLevels = [new SubjectLevelQueryRequest(subjectId: subj4.subjectId, level: 3)]
        subjectLevelButNotSkillsInSubject.notAchievedSkillIds = [skill5.skillId, skill6.skillId]

        QueryUsersCriteriaRequest notSkills = new QueryUsersCriteriaRequest()
        notSkills.projectId = proj.projectId
        notSkills.notAchievedSkillIds = [skill1.skillId, skill2.skillId, skill3.skillId, skill6.skillId]

        when:
        Long numUsers = contactUsersService.countMatchingUsers(projectLevelSubjectLevelThreeSkills)
        Long achievedNotAchieved = contactUsersService.countMatchingUsers(skillNotSkill)
        Long onlyFullyAchieved = contactUsersService.countMatchingUsers(onlyFullyAchievedShouldCount)
        Long badgeAchievedNotSkill = contactUsersService.countMatchingUsers(badgeNotSkill)
        Long twoAchievedSkills = contactUsersService.countMatchingUsers(twoSkills)
        Long allUsers = contactUsersService.countMatchingUsers(allProjectUsers)
        Long subjectLevelNotSkills = contactUsersService.countMatchingUsers(subjectLevelButNotSkillsInSubject)
        Long notAchieved = contactUsersService.countMatchingUsers(notSkills)

        then:
        numUsers == 1
        achievedNotAchieved == 1
        onlyFullyAchieved == 2
        badgeAchievedNotSkill == 1
        twoAchievedSkills == 3
        allUsers == 7
        subjectLevelNotSkills == 0
        notAchieved == 3
    }

    def "test user retrieval query"() {
        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        def subj2 = SkillsFactory.createSubject(1, 2)
        def subj3 = SkillsFactory.createSubject(1, 3)
        def subj4 = SkillsFactory.createSubject(1, 4)

        def badge = SkillsFactory.createBadge()
        badge.enabled = true

        Map skill1 = [projectId: proj.projectId, subjectId: subj.subjectId, skillId: "skill1", name  : "Test Skill 1", type: "Skill",
                      pointIncrement: 100, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]

        Map skill2 = [projectId: proj.projectId, subjectId: subj2.subjectId, skillId: "skill2", name  : "Test Skill 2", type: "Skill",
                      pointIncrement: 100, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]

        Map skill3 = [projectId: proj.projectId, subjectId: subj3.subjectId, skillId: "skill3", name  : "Test Skill 3", type: "Skill",
                      pointIncrement: 100, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]

        Map skill4 = [projectId: proj.projectId, subjectId: subj3.subjectId, skillId: "skill4", name  : "Test Skill 4", type: "Skill",
                      pointIncrement: 100, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]


        Map skill5 = [projectId: proj.projectId, subjectId: subj4.subjectId, skillId: "skill5", name  : "Test Skill 5", type: "Skill",
                      pointIncrement: 50, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]

        Map skill6 = [projectId: proj.projectId, subjectId: subj4.subjectId, skillId: "skill6", name  : "Test Skill 6", type: "Skill",
                      pointIncrement: 50, numPerformToCompletion: 2, pointIncrementInterval: 0, numMaxOccurrencesIncrementInterval: 1]

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSubject(subj2)
        skillsService.createSubject(subj3)
        skillsService.createSubject(subj4)
        skillsService.createSkill(skill1)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)
        skillsService.createSkill(skill5)
        skillsService.createSkill(skill6)
        skillsService.createBadge(badge)

        [skill2.skillId, skill3.skillId, skill6.skillId].each {
            skillsService.assignSkillToBadge(proj.projectId, badge.badgeId, it)
        }

        def users = getRandomUsers(7)

        skillsService.addSkill(skill1, users[0])
        skillsService.addSkill(skill2, users[0])
        skillsService.addSkill(skill3, users[0])

        skillsService.addSkill(skill2, users[1])

        skillsService.addSkill(skill2, users[2])
        skillsService.addSkill(skill3, users[2])
        skillsService.addSkill(skill5, users[2])
        skillsService.addSkill(skill6, users[2])
        skillsService.addSkill(skill6, users[2])

        skillsService.addSkill(skill2, users[3])
        skillsService.addSkill(skill3, users[3])
        skillsService.addSkill(skill6, users[3])
        skillsService.addSkill(skill6, users[3])

        skillsService.addSkill(skill6, users[4])
        skillsService.addSkill(skill6, users[5])
        skillsService.addSkill(skill6, users[6])

        QueryUsersCriteriaRequest projectLevelSubjectLevelThreeSkills = new QueryUsersCriteriaRequest()
        projectLevelSubjectLevelThreeSkills.projectId = proj.projectId
        projectLevelSubjectLevelThreeSkills.projectLevel = 2
        projectLevelSubjectLevelThreeSkills.achievedSkillIds = [skill1.skillId, skill2.skillId, skill3.skillId]
        projectLevelSubjectLevelThreeSkills.subjectLevels = [new SubjectLevelQueryRequest(subjectId: subj3.subjectId, level: 1)]

        QueryUsersCriteriaRequest skillNotSkill = new QueryUsersCriteriaRequest()
        skillNotSkill.projectId = proj.projectId
        skillNotSkill.achievedSkillIds = [skill6.skillId]
        skillNotSkill.notAchievedSkillIds = [skill5.skillId]

        QueryUsersCriteriaRequest onlyFullyAchievedShouldCount = new QueryUsersCriteriaRequest()
        onlyFullyAchievedShouldCount.projectId = proj.projectId
        onlyFullyAchievedShouldCount.achievedSkillIds = [skill6.skillId]

        QueryUsersCriteriaRequest badgeNotSkill = new QueryUsersCriteriaRequest()
        badgeNotSkill.projectId = proj.projectId
        badgeNotSkill.badgeIds = [badge.badgeId]
        badgeNotSkill.notAchievedSkillIds = [skill5.skillId]

        QueryUsersCriteriaRequest twoSkills = new QueryUsersCriteriaRequest()
        twoSkills.projectId = proj.projectId
        twoSkills.achievedSkillIds = [skill2.skillId, skill3.skillId]

        QueryUsersCriteriaRequest allProjectUsers = new QueryUsersCriteriaRequest()
        allProjectUsers.projectId = proj.projectId
        allProjectUsers.allProjectUsers = true

        QueryUsersCriteriaRequest subjectLevelButNotSkillsInSubject = new QueryUsersCriteriaRequest()
        subjectLevelButNotSkillsInSubject.projectId = proj.projectId
        subjectLevelButNotSkillsInSubject.subjectLevels = [new SubjectLevelQueryRequest(subjectId: subj4.subjectId, level: 3)]
        subjectLevelButNotSkillsInSubject.notAchievedSkillIds = [skill5.skillId, skill6.skillId]

        QueryUsersCriteriaRequest notSkills = new QueryUsersCriteriaRequest()
        notSkills.projectId = proj.projectId
        notSkills.notAchievedSkillIds = [skill1.skillId, skill2.skillId, skill3.skillId, skill6.skillId]


        when:
        List<String> projectLevelSubjectLevelSkillsUsers = contactUsersService.retrieveMatchingUserIds(projectLevelSubjectLevelThreeSkills)
        List<String> achievedNotAchieved = contactUsersService.retrieveMatchingUserIds(skillNotSkill)
        List<String> onlyFullyAchieved = contactUsersService.retrieveMatchingUserIds(onlyFullyAchievedShouldCount)
        List<String> badgeAchievedNotSkill = contactUsersService.retrieveMatchingUserIds(badgeNotSkill)
        List<String> twoAchievedSkills = contactUsersService.retrieveMatchingUserIds(twoSkills)
        List<String> allUsers = contactUsersService.retrieveMatchingUserIds(allProjectUsers)
        List<String> subjectLevelNotSkills = contactUsersService.retrieveMatchingUserIds(subjectLevelButNotSkillsInSubject)
        List<String> notAchieved = contactUsersService.retrieveMatchingUserIds(notSkills)

        then:
        projectLevelSubjectLevelSkillsUsers.size() == 1
        projectLevelSubjectLevelSkillsUsers[0] == users[0]
        achievedNotAchieved.size() == 1
        achievedNotAchieved[0] == users[3]
        onlyFullyAchieved.size() == 2
        onlyFullyAchieved.sort() == [users[2], users[3]].sort()
        badgeAchievedNotSkill.size() == 1
        badgeAchievedNotSkill[0] == users[3]
        twoAchievedSkills.size() == 3
        twoAchievedSkills.sort() == [users[0], users[2], users[3]].sort()
        allUsers.size() == 7
        allUsers.sort() == users.sort(false)
        subjectLevelNotSkills.size() == 0
        notAchieved.size() == 3
        notAchieved.sort() == [users[4], users[5], users[6]].sort()
    }

    def "test email"() {
        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        def subj2 = SkillsFactory.createSubject(1, 2)
        def subj3 = SkillsFactory.createSubject(1, 3)
        def subj4 = SkillsFactory.createSubject(1, 4)

        def badge = SkillsFactory.createBadge()
        badge.enabled = true

        Map skill1 = [projectId: proj.projectId, subjectId: subj.subjectId, skillId: "skill1", name  : "Test Skill 1", type: "Skill",
                      pointIncrement: 100, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]

        Map skill2 = [projectId: proj.projectId, subjectId: subj2.subjectId, skillId: "skill2", name  : "Test Skill 2", type: "Skill",
                      pointIncrement: 100, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]

        Map skill3 = [projectId: proj.projectId, subjectId: subj3.subjectId, skillId: "skill3", name  : "Test Skill 3", type: "Skill",
                      pointIncrement: 100, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]

        Map skill4 = [projectId: proj.projectId, subjectId: subj3.subjectId, skillId: "skill4", name  : "Test Skill 4", type: "Skill",
                      pointIncrement: 100, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]


        Map skill5 = [projectId: proj.projectId, subjectId: subj4.subjectId, skillId: "skill5", name  : "Test Skill 5", type: "Skill",
                      pointIncrement: 50, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]

        Map skill6 = [projectId: proj.projectId, subjectId: subj4.subjectId, skillId: "skill6", name  : "Test Skill 6", type: "Skill",
                      pointIncrement: 50, numPerformToCompletion: 2, pointIncrementInterval: 0, numMaxOccurrencesIncrementInterval: 1]

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSubject(subj2)
        skillsService.createSubject(subj3)
        skillsService.createSubject(subj4)
        skillsService.createSkill(skill1)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)
        skillsService.createSkill(skill5)
        skillsService.createSkill(skill6)
        skillsService.createBadge(badge)

        [skill2.skillId, skill3.skillId, skill6.skillId].each {
            skillsService.assignSkillToBadge(proj.projectId, badge.badgeId, it)
        }

        def users = getRandomUsers(7, true)

        skillsService.addSkill(skill1, users[0])
        skillsService.addSkill(skill2, users[0])
        skillsService.addSkill(skill3, users[0])

        skillsService.addSkill(skill2, users[1])

        skillsService.addSkill(skill2, users[2])
        skillsService.addSkill(skill3, users[2])
        skillsService.addSkill(skill5, users[2])
        skillsService.addSkill(skill6, users[2])
        skillsService.addSkill(skill6, users[2])

        skillsService.addSkill(skill2, users[3])
        skillsService.addSkill(skill3, users[3])
        skillsService.addSkill(skill6, users[3])
        skillsService.addSkill(skill6, users[3])

        skillsService.addSkill(skill6, users[4])
        skillsService.addSkill(skill6, users[5])
        skillsService.addSkill(skill6, users[6])

        QueryUsersCriteriaRequest queryUsersCriteriaRequest = new QueryUsersCriteriaRequest()
        queryUsersCriteriaRequest.projectId = proj.projectId
        queryUsersCriteriaRequest.achievedSkillIds = [skill6.skillId]


        ContactUsersRequest cur = new ContactUsersRequest(queryCriteria: queryUsersCriteriaRequest)
        cur.emailSubject = "The Subject"
        cur.emailBody = "The Body"

        when:
        contactUsersService.contactUsers(cur)
        assert WaitFor.wait { greenMail.getReceivedMessages().size() >= 2 }

        def messages = EmailUtils.getEmails(greenMail)

        then:
        messages.find { it.recipients.size() == 1 && it.recipients[0].contains(users[2]) }
        messages.find { it.recipients.size() == 1 && it.recipients[0].contains(users[3]) }
        messages.findAll { it.subj == "The Subject"}.size() == 2
    }


}
