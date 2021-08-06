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
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import skills.controller.request.model.ContactUsersRequest
import skills.controller.request.model.QueryUsersCriteriaRequest
import skills.controller.request.model.SubjectLevelQueryRequest
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.EmailUtils
import skills.intTests.utils.SkillsFactory
import skills.services.ContactUsersService
import skills.services.UserAttrsService
import skills.utils.WaitFor
import spock.lang.IgnoreRest

import java.util.stream.Stream

class ContactUsersServiceSpec extends DefaultIntSpec {

    @Autowired
    ContactUsersService contactUsersService

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    UserAttrsService userAttrsService

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
        Long numUsers = skillsService.countProjectUsers(proj.projectId, false, [skill1.skillId, skill2.skillId, skill3.skillId], null, [[subjectId: subj3.subjectId, level: 1]], 2)
        Long achievedNotAchieved = skillsService.countProjectUsers(proj.projectId, false, [skill6.skillId], [skill5.skillId])
        Long onlyFullyAchieved = skillsService.countProjectUsers(proj.projectId, false, [skill6.skillId])
        Long badgeAchievedNotSkill = skillsService.countProjectUsers(proj.projectId, false, [badge.badgeId], [skill5.skillId])
        Long twoAchievedSkills = skillsService.countProjectUsers(proj.projectId, false, [skill2.skillId, skill3.skillId])
        Long allUsers = skillsService.countProjectUsers(proj.projectId, true)
        Long subjectLevelNotSkills = skillsService.countProjectUsers(proj.projectId, false, null, [skill5.skillId, skill6.skillId], [[subjectId: subj4.subjectId, level: 3]])
        Long notAchieved = skillsService.countProjectUsers(proj.projectId, false, null, [skill1.skillId, skill2.skillId, skill3.skillId, skill6.skillId])

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
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager)

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
        List<String> projectLevelSubjectLevelSkillsUsers = []
        transactionTemplate.execute({
            contactUsersService.retrieveMatchingUserIds(projectLevelSubjectLevelThreeSkills).forEach({projectLevelSubjectLevelSkillsUsers += it})
        })

        List<String> achievedNotAchieved = []
        transactionTemplate.execute({
            contactUsersService.retrieveMatchingUserIds(skillNotSkill).forEach({achievedNotAchieved += it})
        })

        List<String> onlyFullyAchieved = []
        transactionTemplate.execute({
            contactUsersService.retrieveMatchingUserIds(onlyFullyAchievedShouldCount).forEach({onlyFullyAchieved += it})
        })

        List<String> badgeAchievedNotSkill = []
        transactionTemplate.execute({
            contactUsersService.retrieveMatchingUserIds(badgeNotSkill).forEach({badgeAchievedNotSkill += it})
        })

        List<String> twoAchievedSkills = []
        transactionTemplate.execute({
            contactUsersService.retrieveMatchingUserIds(twoSkills).forEach({twoAchievedSkills += it})
        })

        List<String> allUsers = []
        transactionTemplate.execute({
            contactUsersService.retrieveMatchingUserIds(allProjectUsers).forEach({allUsers +=it})
        })

        List<String> subjectLevelNotSkills = []
        transactionTemplate.execute({
            contactUsersService.retrieveMatchingUserIds(subjectLevelButNotSkillsInSubject).forEach({subjectLevelNotSkills += it})
        })

        List<String> notAchieved = []
        transactionTemplate.execute({
            contactUsersService.retrieveMatchingUserIds(notSkills).forEach({notAchieved += it})
        })


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


        String emailSubject = "The Subject"
        String emailBody = """# The Body
* one item
* two items
        """

        String user2Email = userAttrsService.findByUserId(users[2].toLowerCase())?.email
        String user3Email = userAttrsService.findByUserId(users[3].toLowerCase())?.email

        when:
        skillsService.contactProjectUsers(proj.projectId, emailSubject, emailBody, false, [skill6.skillId])

        assert WaitFor.wait { greenMail.getReceivedMessages().size() >= 2 }

        def messages = EmailUtils.getEmails(greenMail)

        then:
        messages.find { it.recipients.size() == 1 && it.recipients[0].contains(user2Email) }
        messages.find { it.recipients.size() == 1 && it.recipients[0].contains(user3Email) }
        messages[0].html.replaceAll('\r\n', '\n') == '''<!--
Copyright 2020 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->
<!DOCTYPE html>
<html   lang="en"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.thymeleaf.org http://www.thymeleaf.org">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
</head>
<body class="overall-container">

<h1>The Body</h1>
<ul>
<li>one item</li>
<li>two items</li>
</ul>


</body>
</html>'''.replaceAll('\r\n', '\n')

        messages[1].html.replaceAll('\r\n', '\n') == '''<!--
Copyright 2020 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->
<!DOCTYPE html>
<html   lang="en"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.thymeleaf.org http://www.thymeleaf.org">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
</head>
<body class="overall-container">

<h1>The Body</h1>
<ul>
<li>one item</li>
<li>two items</li>
</ul>


</body>
</html>'''.replaceAll('\r\n', '\n')
    }

    def "preview email"() {
        def users = getRandomUsers(1, true)
        def service = createService(users[0])

        def proj = SkillsFactory.createProject(1)
        service.createProject(proj)

        when:
        service.previewEmail(proj.projectId,"a subject", "**body**")

        assert WaitFor.wait { greenMail.getReceivedMessages().size() >= 1 }
        def messages = EmailUtils.getEmails(greenMail)

        then:
        messages.size() == 1
        messages[0].recipients.find { it.contains(users[0]) }
        messages[0].html.replaceAll('\r\n', '\n') == '''<!--
Copyright 2020 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->
<!DOCTYPE html>
<html   lang="en"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.thymeleaf.org http://www.thymeleaf.org">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
</head>
<body class="overall-container">

<p><strong>body</strong></p>


</body>
</html>'''
    }


}
