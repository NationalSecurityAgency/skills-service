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
package skills.intTests.community.globalBadge

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService

import static skills.intTests.utils.SkillsFactory.*

class CommunityAndGlobalBadgeSpecs extends DefaultIntSpec {

    def "UC projects are not allowed to be added to a non UC Global Badge - skill assigned"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])
        rootUser.saveUserTag(rootUser.userName, 'dragons', ['DivineDragon'])

        def p1 = createProject(1)
        p1.enableProtectedUserCommunity = true
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        pristineDragonsUser.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge1 = createBadge(1)
        badge1.enableProtectedUserCommunity = false
        rootUser.createGlobalBadge(badge1)

        when:
        rootUser.assignSkillToGlobalBadge(projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[0].skillId)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("Projects with community protection can only be added to a Global Badge with community protection")
    }

    def "UC projects are not allowed to be added to a non UC Global Badge - level assigned"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])
        rootUser.saveUserTag(rootUser.userName, 'dragons', ['DivineDragon'])

        def p1 = createProject(1)
        p1.enableProtectedUserCommunity = true
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        pristineDragonsUser.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge1 = createBadge(1)
        badge1.enableProtectedUserCommunity = false
        rootUser.createGlobalBadge(badge1)

        when:
        rootUser.assignProjectLevelToGlobalBadge(projectId: p1.projectId, badgeId: badge1.badgeId, level: "1")
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("Projects with community protection can only be added to a Global Badge with community protection")
    }

    def "non UC projects are not allowed to be added to a UC Global Badge - skill assigned"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])
        rootUser.saveUserTag(rootUser.userName, 'dragons', ['DivineDragon'])

        def p1 = createProject(1)
        p1.enableProtectedUserCommunity = false
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        pristineDragonsUser.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge1 = createBadge(1)
        badge1.enableProtectedUserCommunity = true
        rootUser.createGlobalBadge(badge1)

        when:
        rootUser.assignSkillToGlobalBadge(projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[0].skillId)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("Projects without community protection can not be added to a Global Badge with community protection")
    }

    def "non UC projects are not allowed to be added to a UC Global Badge - level assigned"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])
        rootUser.saveUserTag(rootUser.userName, 'dragons', ['DivineDragon'])

        def p1 = createProject(1)
        p1.enableProtectedUserCommunity = false
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        pristineDragonsUser.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge1 = createBadge(1)
        badge1.enableProtectedUserCommunity = true
        rootUser.createGlobalBadge(badge1)

        when:
        rootUser.assignProjectLevelToGlobalBadge(projectId: p1.projectId, badgeId: badge1.badgeId, level: "1")
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("Projects without community protection can not be added to a Global Badge with community protection")
    }

    def "non UC projects are allowed to be added to a non UC Global Badge - skill assigned"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge1 = createBadge(1, 1)
        skillsService.createGlobalBadge(badge1)

        when:
        skillsService.assignSkillToGlobalBadge(projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[0].skillId)

        def res = skillsService.getGlobalBadge(badgeId)

        then:
        res
        res.numSkills == 1
        res.requiredSkills.size() == 1
        res.requiredProjectLevels.size() == 0
    }

    def "non UC projects are allowed to be added to a non UC Global Badge - level assigned"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge1 = createBadge(1, 1)
        skillsService.createGlobalBadge(badge1)

        when:
        skillsService.assignProjectLevelToGlobalBadge(projectId: p1.projectId, badgeId: badge1.badgeId, level: "1")

        def res = skillsService.getGlobalBadge(badgeId)

        then:
        res
        res.numSkills == 0
        res.requiredSkills.size() == 0
        res.requiredProjectLevels.size() == 1
    }

    def "UC projects are allowed to be added to a UC Global Badge - skill assigned"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        p1.enableProtectedUserCommunity = true
        pristineDragonsUser.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge1 = createBadge(1, 1)
        badge1.enableProtectedUserCommunity = true
        pristineDragonsUser.createGlobalBadge(badge1)

        when:
        pristineDragonsUser.assignSkillToGlobalBadge(projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[0].skillId)

        def res = pristineDragonsUser.getGlobalBadge(badgeId)

        then:
        res
        res.numSkills == 1
        res.requiredSkills.size() == 1
        res.requiredProjectLevels.size() == 0
    }

    def "UC projects are allowed to be added to a UC Global Badge - level assigned"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        p1.enableProtectedUserCommunity = true
        pristineDragonsUser.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge1 = createBadge(1, 1)
        badge1.enableProtectedUserCommunity = true
        pristineDragonsUser.createGlobalBadge(badge1)

        when:
        pristineDragonsUser.assignProjectLevelToGlobalBadge(projectId: p1.projectId, badgeId: badge1.badgeId, level: "1")

        def res = pristineDragonsUser.getGlobalBadge(badgeId)

        then:
        res
        res.numSkills == 0
        res.requiredSkills.size() == 0
        res.requiredProjectLevels.size() == 1
    }

    def "cannot enable community for a project if it belongs to a non UC Global badge - skill assigned"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        pristineDragonsUser.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2Skills = createSkills(3, 2, 1, 100, 5)
        pristineDragonsUser.createProjectAndSubjectAndSkills(p2, p2subj1, p2Skills)

        def badge1 = createBadge(1, 1)
        def badge2 = createBadge(2, 2)
        rootUser.createGlobalBadge(badge1)
        rootUser.assignSkillToGlobalBadge(projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[0].skillId)

        rootUser.createGlobalBadge(badge2)
        rootUser.assignProjectLevelToGlobalBadge(projectId: p2.projectId, badgeId: badge2.badgeId, level: "1")

        p1.enableProtectedUserCommunity = true
        when:
        pristineDragonsUser.updateProject(p1)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.getMessage().contains("Not Allowed to set [enableProtectedUserCommunity] to true")
        e.message.contains("This project is part of one or more Global Badges that has not enabled user community protection")
    }

    def "cannot enable community for a project if it belongs to a non UC Global badge - level assigned"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        pristineDragonsUser.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2Skills = createSkills(3, 2, 1, 100, 5)
        pristineDragonsUser.createProjectAndSubjectAndSkills(p2, p2subj1, p2Skills)

        def badge1 = createBadge(1, 1)
        def badge2 = createBadge(2, 2)
        rootUser.createGlobalBadge(badge1)
        rootUser.assignSkillToGlobalBadge(projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[0].skillId)

        rootUser.createGlobalBadge(badge2)
        rootUser.assignProjectLevelToGlobalBadge(projectId: p2.projectId, badgeId: badge2.badgeId, level: "1")

        p2.enableProtectedUserCommunity = true
        when:
        pristineDragonsUser.updateProject(p2)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.getMessage().contains("Not Allowed to set [enableProtectedUserCommunity] to true")
        e.message.contains("This project is part of one or more Global Badges that has not enabled user community protection")
    }

    def "available skills for global badges endpoint does NOT return skills from UC protected projects"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])
        rootUser.saveUserTag(rootUser.userName, 'dragons', ['DivineDragon'])

        def p1 = createProject(1)
        p1.enableProtectedUserCommunity = true
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        p1Skills[0].skillId = "ProtectedSkill1"
        p1Skills[1].skillId = "ProtectedSkill2"
        p1Skills[2].skillId = "ProtectedSkill3"
        pristineDragonsUser.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2Skills = createSkills(3, 2, 1, 100, 5)
        p2Skills[0].skillId = "Skill1"
        p2Skills[1].skillId = "Skill2"
        p2Skills[2].skillId = "Skill3"
        pristineDragonsUser.createProjectAndSubjectAndSkills(p2, p2subj1, p2Skills)

        rootUser.pinProject(p1.projectId)
        rootUser.pinProject(p2.projectId)

        def badge1 = createBadge(1)
        def badge2 = createBadge(2)
        rootUser.createGlobalBadge(badge1)

        when:
        def res = rootUser.getAvailableSkillsForGlobalBadge(badge1.badgeId, "")
        def res1 = rootUser.getAvailableSkillsForGlobalBadge(badge1.badgeId, "2")

        def res2 = rootUser.getAvailableProjectsForGlobalBadge(badge1.badgeId, "")
        def res3 = rootUser.getAvailableProjectsForGlobalBadge(badge1.badgeId, "1")
        then:
        res.suggestedSkills.skillId.sort() == p2Skills.skillId.sort()
        res.totalAvailable == 3

        res1.suggestedSkills.skillId.sort() == [p2Skills[1].skillId]
        res1.totalAvailable == 1

        res2.projects.projectId == [p2.projectId]
        res2.totalAvailable == 1

        res3.projects == []
        res3.totalAvailable == 0
    }

    def "once community is enabled it cannot be disabled"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def b1 = createBadge(1)
        b1.enableProtectedUserCommunity = true
        pristineDragonsUser.createGlobalBadge(b1)
        when:
        b1.enableProtectedUserCommunity = false
        pristineDragonsUser.updateGlobalBadge(b1, b1.badgeId)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.getMessage().contains("Once global badge [enableProtectedUserCommunity=true] it cannot be flipped to false. badgeId: [${b1.badgeId}]")
        e.getMessage().contains("badgeId: [${b1.badgeId}]")
    }
}
