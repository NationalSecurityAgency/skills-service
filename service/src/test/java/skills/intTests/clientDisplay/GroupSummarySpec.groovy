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
package skills.intTests.clientDisplay


import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.services.admin.skillReuse.SkillReuseIdUtil

class GroupSummarySpec extends DefaultIntSpec {

    def "get group summary for users with various progress in the group"() {
        List<SkillsService> users = getRandomUsers(5).collect { createService(it) }

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(8, 1, 1)
        proj1_skills.each { it.numPerformToCompletion = 2; it.pointIncrementInterval = 0; it.description = "blah1" }
        def group1 = SkillsFactory.createSkillsGroup(1, 1, 10)
        def group2 = SkillsFactory.createSkillsGroup(1, 1, 11)
        skillsService.createProjectAndSubjectAndSkills(proj1, proj1_subj, [proj1_skills[0], group1, group2, proj1_skills[7]])
        skillsService.assignSkillToSkillsGroup(group1.skillId.toString(), proj1_skills[1])
        skillsService.assignSkillToSkillsGroup(group1.skillId.toString(), proj1_skills[2])
        skillsService.assignSkillToSkillsGroup(group1.skillId.toString(), proj1_skills[3])
        skillsService.assignSkillToSkillsGroup(group1.skillId.toString(), proj1_skills[4])
        skillsService.assignSkillToSkillsGroup(group2.skillId.toString(), proj1_skills[5])
        skillsService.assignSkillToSkillsGroup(group2.skillId.toString(), proj1_skills[6])

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 1)
        List<Map> proj2_skills = SkillsFactory.createSkills(8, 2, 1)
        proj2_skills.each { it.numPerformToCompletion = 2; it.pointIncrementInterval = 0 }
        def p2_group1 = SkillsFactory.createSkillsGroup(2, 1, 10)
        def p2_group2 = SkillsFactory.createSkillsGroup(2, 1, 11)
        skillsService.createProjectAndSubjectAndSkills(proj2, proj2_subj, [proj2_skills[0], p2_group1, p2_group2, proj2_skills[7]])
        skillsService.assignSkillToSkillsGroup(p2_group1.skillId.toString(), proj2_skills[1])
        skillsService.assignSkillToSkillsGroup(p2_group1.skillId.toString(), proj2_skills[2])
        skillsService.assignSkillToSkillsGroup(p2_group2.skillId.toString(), proj2_skills[3])
        skillsService.assignSkillToSkillsGroup(p2_group2.skillId.toString(), proj2_skills[4])
        skillsService.assignSkillToSkillsGroup(p2_group2.skillId.toString(), proj2_skills[5])
        skillsService.assignSkillToSkillsGroup(p2_group2.skillId.toString(), proj2_skills[6])

        users[0].addSkill(proj1_skills[0])
        users[0].addSkill(proj1_skills[0])
        users[0].addSkill(proj1_skills[1])
        users[0].addSkill(proj1_skills[1])
        users[0].addSkill(proj1_skills[2])
        users[0].addSkill(proj1_skills[3])
        users[0].addSkill(proj1_skills[5])
        users[0].addSkill(proj1_skills[5])
        users[0].addSkill(proj1_skills[6])
        users[0].addSkill(proj1_skills[6])
        users[0].addSkill(proj2_skills[5])
        users[0].addSkill(proj2_skills[6])

        // User[1] fully achieves group2 and partially achieves group1
        users[1].addSkill(proj1_skills[0])
        users[1].addSkill(proj1_skills[0])
        users[1].addSkill(proj1_skills[1])
        users[1].addSkill(proj1_skills[1])
        users[1].addSkill(proj1_skills[2])
        users[1].addSkill(proj1_skills[3])
        users[1].addSkill(proj1_skills[4])
        users[1].addSkill(proj1_skills[5])
        users[1].addSkill(proj1_skills[6])
        users[1].addSkill(proj2_skills[3])
        users[1].addSkill(proj2_skills[3])
        users[1].addSkill(proj2_skills[4])
        users[1].addSkill(proj2_skills[5])
        users[1].addSkill(proj2_skills[5])
        users[1].addSkill(proj2_skills[6])
        users[1].addSkill(proj2_skills[6])

        when:
        def uiResGroup1 = users[0].getSkillsGroupSummary(proj1.projectId, group1.skillId.toString())
        def uiResGroup1AsAdmin = skillsService.getSkillsGroupSummary(proj1.projectId, group1.skillId.toString(), users[0].userName)
        def uiResGroup2 = users[0].getSkillsGroupSummary(proj1.projectId, group2.skillId.toString())
        def uiResP2Group1 = users[0].getSkillsGroupSummary(proj2.projectId, p2_group1.skillId.toString())
        def uiResP2Group2 = users[0].getSkillsGroupSummary(proj2.projectId, p2_group2.skillId.toString())
        def uiResUser1Group1 = users[1].getSkillsGroupSummary(proj1.projectId, group1.skillId.toString())
        def uiResUser1Group2 = users[1].getSkillsGroupSummary(proj1.projectId, group2.skillId.toString())
        def uiResUser1P2Group1 = users[1].getSkillsGroupSummary(proj2.projectId, p2_group1.skillId.toString())
        def uiResUser1P2Group2 = users[1].getSkillsGroupSummary(proj2.projectId, p2_group2.skillId.toString())

        then:
        uiResGroup1.group == group1.name.toString()
        uiResGroup1.groupId == group1.skillId.toString()
        uiResGroup1.totalSkills == 4
        uiResGroup1.skillsAchieved == 1
        uiResGroup1.skills.skillId == proj1_skills[1..4].skillId
        uiResGroup1.skills.skill == proj1_skills[1..4].name
        uiResGroup1.skills.totalPoints == [20, 20, 20, 20]
        uiResGroup1.skills.points == [20, 10, 10, 0]
        uiResGroup1.skills.todaysPoints == [20, 10, 10, 0]
        uiResGroup1.skills.type == ['Skill', 'Skill', 'Skill', 'Skill']
        uiResGroup1.skills.pointIncrement == [10, 10, 10, 10]
        uiResGroup1.skills.description == [null, null, null, null]
        uiResGroup1.skills.groupSkillId ==  [group1.skillId.toString(), group1.skillId.toString(), group1.skillId.toString(), group1.skillId.toString()]

        uiResGroup1AsAdmin.group == group1.name.toString()
        uiResGroup1AsAdmin.groupId == group1.skillId.toString()
        uiResGroup1AsAdmin.totalSkills == 4
        uiResGroup1AsAdmin.skillsAchieved == 1
        uiResGroup1AsAdmin.skills.skillId == proj1_skills[1..4].skillId
        uiResGroup1AsAdmin.skills.skill == proj1_skills[1..4].name
        uiResGroup1AsAdmin.skills.totalPoints == [20, 20, 20, 20]
        uiResGroup1AsAdmin.skills.points == [20, 10, 10, 0]
        uiResGroup1AsAdmin.skills.todaysPoints == [20, 10, 10, 0]
        uiResGroup1AsAdmin.skills.type == ['Skill', 'Skill', 'Skill', 'Skill']
        uiResGroup1AsAdmin.skills.pointIncrement == [10, 10, 10, 10]
        uiResGroup1AsAdmin.skills.description == [null, null, null, null]

        uiResGroup2.group == group2.name.toString()
        uiResGroup2.groupId == group2.skillId.toString()
        uiResGroup2.totalSkills == 2
        uiResGroup2.skillsAchieved == 2
        uiResGroup2.skills.skillId == proj2_skills[5..6].skillId
        uiResGroup2.skills.skill == proj2_skills[5..6].name
        uiResGroup2.skills.totalPoints == [20, 20]
        uiResGroup2.skills.points == [20, 20]
        uiResGroup2.skills.todaysPoints == [20, 20]
        uiResGroup2.skills.type == ['Skill', 'Skill']
        uiResGroup2.skills.pointIncrement == [10, 10]
        uiResGroup2.skills.description == [null, null]
        uiResGroup2.skills.groupSkillId ==  [group2.skillId.toString(), group2.skillId.toString()]

        uiResP2Group1.group == p2_group1.name.toString()
        uiResP2Group1.groupId == p2_group1.skillId.toString()
        uiResP2Group1.totalSkills == 2
        uiResP2Group1.skillsAchieved == 0
        uiResP2Group1.skills.skillId == proj2_skills[1..2].skillId
        uiResP2Group1.skills.skill == proj2_skills[1..2].name
        uiResP2Group1.skills.totalPoints == [20, 20]
        uiResP2Group1.skills.points == [0, 0]
        uiResP2Group1.skills.todaysPoints == [0, 0]
        uiResP2Group1.skills.type == ['Skill', 'Skill']
        uiResP2Group1.skills.pointIncrement == [10, 10]
        uiResP2Group1.skills.description == [null, null]

        uiResP2Group2.group == p2_group2.name.toString()
        uiResP2Group2.groupId == p2_group2.skillId.toString()
        uiResP2Group2.totalSkills == 4
        uiResP2Group2.skillsAchieved == 0
        uiResP2Group2.skills.skillId == proj2_skills[3..6].skillId
        uiResP2Group2.skills.skill == proj2_skills[3..6].name
        uiResP2Group2.skills.totalPoints == [20, 20, 20, 20]
        uiResP2Group2.skills.points == [0, 0, 10, 10]
        uiResP2Group2.skills.todaysPoints == [0, 0, 10, 10]
        uiResP2Group2.skills.type == ['Skill', 'Skill', 'Skill', 'Skill']
        uiResP2Group2.skills.pointIncrement == [10, 10, 10, 10]
        uiResP2Group2.skills.description == [null, null, null, null]

        // User[1] results - fully achieved group2, partially achieved group1
        uiResUser1Group1.group == group1.name.toString()
        uiResUser1Group1.groupId == group1.skillId.toString()
        uiResUser1Group1.totalSkills == 4
        uiResUser1Group1.skillsAchieved == 1
        uiResUser1Group1.skills.skillId == proj1_skills[1..4].skillId
        uiResUser1Group1.skills.skill == proj1_skills[1..4].name
        uiResUser1Group1.skills.totalPoints == [20, 20, 20, 20]
        uiResUser1Group1.skills.points == [20, 10, 10, 10]
        uiResUser1Group1.skills.todaysPoints == [20, 10, 10, 10]
        uiResUser1Group1.skills.type == ['Skill', 'Skill', 'Skill', 'Skill']
        uiResUser1Group1.skills.pointIncrement == [10, 10, 10, 10]
        uiResUser1Group1.skills.description == [null, null, null, null]

        uiResUser1Group2.group == group2.name.toString()
        uiResUser1Group2.groupId == group2.skillId.toString()
        uiResUser1Group2.totalSkills == 2
        uiResUser1Group2.skillsAchieved == 0
        uiResUser1Group2.skills.skillId == proj1_skills[5..6].skillId
        uiResUser1Group2.skills.skill == proj1_skills[5..6].name
        uiResUser1Group2.skills.totalPoints == [20, 20]
        uiResUser1Group2.skills.points == [10, 10]
        uiResUser1Group2.skills.todaysPoints == [10, 10]
        uiResUser1Group2.skills.type == ['Skill', 'Skill']
        uiResUser1Group2.skills.pointIncrement == [10, 10]
        uiResUser1Group2.skills.description == [null, null]

        uiResUser1P2Group1.group == p2_group1.name.toString()
        uiResUser1P2Group1.groupId == p2_group1.skillId.toString()
        uiResUser1P2Group1.totalSkills == 2
        uiResUser1P2Group1.skillsAchieved == 0
        uiResUser1P2Group1.skills.skillId == proj2_skills[1..2].skillId
        uiResUser1P2Group1.skills.skill == proj1_skills[1..2].name
        uiResUser1P2Group1.skills.totalPoints == [20, 20]
        uiResUser1P2Group1.skills.points == [0, 0]
        uiResUser1P2Group1.skills.todaysPoints == [0, 0]
        uiResUser1P2Group1.skills.type == ['Skill', 'Skill']
        uiResUser1P2Group1.skills.pointIncrement == [10, 10]
        uiResUser1P2Group1.skills.description == [null, null]

        uiResUser1P2Group2.group == p2_group2.name.toString()
        uiResUser1P2Group2.groupId == p2_group2.skillId.toString()
        uiResUser1P2Group2.totalSkills == 4
        uiResUser1P2Group2.skillsAchieved == 3
        uiResUser1P2Group2.skills.skillId == proj1_skills[3..6].skillId
        uiResUser1P2Group2.skills.skill == proj1_skills[3..6].name
        uiResUser1P2Group2.skills.totalPoints == [20, 20, 20, 20]
        uiResUser1P2Group2.skills.points == [20, 10, 20, 20]
        uiResUser1P2Group2.skills.todaysPoints == [20, 10, 20, 20]
        uiResUser1P2Group2.skills.type == ['Skill', 'Skill', 'Skill', 'Skill']
        uiResUser1P2Group2.skills.pointIncrement == [10, 10, 10, 10]
        uiResUser1P2Group2.skills.description == [null, null, null, null]
    }

    def "empty group"() {
        List<SkillsService> users = getRandomUsers(5).collect { createService(it) }

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(8, 1, 1)
        proj1_skills.each { it.numPerformToCompletion = 2; it.pointIncrementInterval = 0; it.description = "blah1" }
        def group1 = SkillsFactory.createSkillsGroup(1, 1, 10)
        def group2 = SkillsFactory.createSkillsGroup(1, 1, 11)
        skillsService.createProjectAndSubjectAndSkills(proj1, proj1_subj, [proj1_skills[0], group1, group2, proj1_skills[7]])
        skillsService.assignSkillToSkillsGroup(group1.skillId.toString(), proj1_skills[1])
        skillsService.assignSkillToSkillsGroup(group1.skillId.toString(), proj1_skills[2])

        when:
        def uiResGroup2 = users[0].getSkillsGroupSummary(proj1.projectId, group2.skillId.toString())

        then:
        uiResGroup2.group == group2.name.toString()
        uiResGroup2.groupId == group2.skillId.toString()
        uiResGroup2.totalSkills == 0
        uiResGroup2.skillsAchieved == 0
        !uiResGroup2.skills
    }

    def "only admin is allowed to get group summary for another user"() {
        List<SkillsService> users = getRandomUsers(5).collect { createService(it) }

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(8, 1, 1)
        proj1_skills.each { it.numPerformToCompletion = 2; it.pointIncrementInterval = 0; it.description = "blah1" }
        def group1 = SkillsFactory.createSkillsGroup(1, 1, 10)
        def group2 = SkillsFactory.createSkillsGroup(1, 1, 11)
        skillsService.createProjectAndSubjectAndSkills(proj1, proj1_subj, [proj1_skills[0], group1, group2, proj1_skills[7]])
        skillsService.assignSkillToSkillsGroup(group1.skillId.toString(), proj1_skills[1])
        skillsService.assignSkillToSkillsGroup(group1.skillId.toString(), proj1_skills[2])

        when:
        users[0].getSkillsGroupSummary(proj1.projectId, group2.skillId.toString(), users[1].userName)

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.resBody.contains("AccessDenied")
    }

    def "catalog skills under a group" () {
        List<SkillsService> users = getRandomUsers(5).collect { createService(it) }
        
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(8, 1, 1)
        proj1_skills.each { it.numPerformToCompletion = 2; it.pointIncrementInterval = 0; it.description = "blah1" }
        skillsService.createProjectAndSubjectAndSkills(proj1, proj1_subj, proj1_skills)
        proj1_skills.each { skillsService.exportSkillToCatalog(it.projectId.toString(), it.skillId.toString()) }

        users[0].addSkill(proj1_skills[0])
        users[0].addSkill(proj1_skills[0])
        users[0].addSkill(proj1_skills[1])
        users[0].addSkill(proj1_skills[1])
        users[0].addSkill(proj1_skills[2])
        users[0].addSkill(proj1_skills[2])
        users[0].addSkill(proj1_skills[3])
        users[0].addSkill(proj1_skills[3])
        users[0].addSkill(proj1_skills[4])
        users[0].addSkill(proj1_skills[5])
        users[0].addSkill(proj1_skills[5])
        users[0].addSkill(proj1_skills[6])
        users[0].addSkill(proj1_skills[6])
       
        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 1)
        List<Map> proj2_skills = SkillsFactory.createSkills(8, 2, 1)
        proj2_skills.each { it.numPerformToCompletion = 2; it.pointIncrementInterval = 0; it.pointIncrement = 100 }
        def p2_group1 = SkillsFactory.createSkillsGroup(2, 1, 10)
        def p2_group2 = SkillsFactory.createSkillsGroup(2, 1, 11)
        skillsService.createProjectAndSubjectAndSkills(proj2, proj2_subj, [proj2_skills[0], p2_group1, p2_group2, proj2_skills[7]])
        skillsService.assignSkillToSkillsGroup(p2_group1.skillId.toString(), proj2_skills[1])

        users[0].addSkill(proj2_skills[1])
        users[0].addSkill(proj2_skills[1])

        skillsService.bulkImportSkillsIntoGroupFromCatalog(proj2.projectId, proj2_subj.subjectId, p2_group1.skillId.toString(), proj1_skills[2..4])
        
        when:
        def res1 = users[0].getSkillsGroupSummary(proj2.projectId, p2_group1.skillId.toString())
        skillsService.finalizeSkillsImportFromCatalog(proj2.projectId)
        def res2 = users[0].getSkillsGroupSummary(proj2.projectId, p2_group1.skillId.toString())
        then:
        res1.group == p2_group1.name.toString()
        res1.groupId == p2_group1.skillId.toString()
        res1.totalSkills == 1
        res1.skillsAchieved == 1
        res1.skills.skillId == proj2_skills[1..1].skillId
        res1.skills.skill == proj2_skills[1..1].name
        res1.skills.totalPoints == [200]
        res1.skills.points == [200]
        res1.skills.todaysPoints == [200]
        res1.skills.type == ['Skill']
        res1.skills.pointIncrement == [100]
        res1.skills.description == [null]
        res1.skills.groupSkillId ==  [p2_group1.skillId.toString()]

        res2.group == p2_group1.name.toString()
        res2.groupId == p2_group1.skillId.toString()
        res2.totalSkills == 4
        res2.skillsAchieved == 3
        res2.skills.skillId == [proj2_skills[1], proj1_skills[2], proj1_skills[3], proj1_skills[4]].skillId
        res2.skills.skill == [proj2_skills[1], proj1_skills[2], proj1_skills[3], proj1_skills[4]].name
        res2.skills.totalPoints == [200, 20, 20, 20]
        res2.skills.points == [200, 20, 20, 10]
        res2.skills.todaysPoints == [200, 20, 20, 10]
        res2.skills.type == ['Skill', 'Skill', 'Skill', 'Skill']
        res2.skills.pointIncrement == [100, 10, 10, 10]
        res2.skills.description == [null, null, null, null]
        res2.skills.groupSkillId ==  [p2_group1.skillId.toString(), p2_group1.skillId.toString(), p2_group1.skillId.toString(), p2_group1.skillId.toString()]
    }

    def "reused skills under a group" () {
        List<SkillsService> users = getRandomUsers(5).collect { createService(it) }

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(8, 1, 1)
        proj1_skills.each { it.numPerformToCompletion = 2; it.pointIncrementInterval = 0; it.description = "blah1" }
        def p1_group1 = SkillsFactory.createSkillsGroup(1, 1, 10)
        def p1_group2 = SkillsFactory.createSkillsGroup(1, 1, 11)
        skillsService.createProjectAndSubjectAndSkills(proj1, proj1_subj, [proj1_skills, [p1_group1, p1_group2]].flatten())

        skillsService.reuseSkills(proj1.projectId.toString(), proj1_skills[1..4].skillId, proj1_subj.subjectId.toString(), p1_group1.skillId.toString())

        users[0].addSkill(proj1_skills[0])
        users[0].addSkill(proj1_skills[0])
        users[0].addSkill(proj1_skills[1])
        users[0].addSkill(proj1_skills[1])
        users[0].addSkill(proj1_skills[2])
        users[0].addSkill(proj1_skills[2])
        users[0].addSkill(proj1_skills[3])
        users[0].addSkill(proj1_skills[3])
        users[0].addSkill(proj1_skills[4])
        users[0].addSkill(proj1_skills[5])
        users[0].addSkill(proj1_skills[5])
        users[0].addSkill(proj1_skills[6])
        users[0].addSkill(proj1_skills[6])

        skillsService.waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        when:
        def res1 = users[0].getSkillsGroupSummary(proj1.projectId, p1_group1.skillId.toString())
        then:
        res1.group == p1_group1.name.toString()
        res1.groupId == p1_group1.skillId.toString()
        res1.totalSkills == 4
        res1.skillsAchieved == 3
        res1.skills.skillId == proj1_skills[1..4].skillId.collect { SkillReuseIdUtil.addTag(it, 0)}
        res1.skills.skill == proj1_skills[1..4].name
        res1.skills.totalPoints == [20, 20, 20, 20]
        res1.skills.points == [20, 20, 20,10]
        res1.skills.todaysPoints == [20, 20, 20, 10]
        res1.skills.type == ['Skill', 'Skill', 'Skill', 'Skill']
        res1.skills.pointIncrement == [10, 10, 10, 10]
        res1.skills.description == [null, null, null, null]
        res1.skills.groupSkillId ==  [p1_group1.skillId.toString(), p1_group1.skillId.toString(), p1_group1.skillId.toString(), p1_group1.skillId.toString()]
    }

    def "disabled skills under a group" () {
        List<SkillsService> users = getRandomUsers(5).collect { createService(it) }

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(8, 1, 1)
        proj1_skills.each { it.numPerformToCompletion = 2; it.pointIncrementInterval = 0; it.description = "blah1"; it.pointIncrement = 100 }
        def p1_group1 = SkillsFactory.createSkillsGroup(1, 1, 10)
        def p1_group2 = SkillsFactory.createSkillsGroup(1, 1, 11)
        skillsService.createProjectAndSubjectAndSkills(proj1, proj1_subj, [p1_group1, p1_group2])

        proj1_skills[0].enabled = false
        proj1_skills[5].enabled = false
        proj1_skills[6].enabled = false
        proj1_skills[7].enabled = false
        proj1_skills.each {
            skillsService.assignSkillToSkillsGroup(p1_group1.skillId.toString(), it)
        }

        users[0].addSkill(proj1_skills[1])
        users[0].addSkill(proj1_skills[1])
        users[0].addSkill(proj1_skills[2])
        users[0].addSkill(proj1_skills[2])
        users[0].addSkill(proj1_skills[3])
        users[0].addSkill(proj1_skills[3])
        users[0].addSkill(proj1_skills[4])

        when:
        def res1 = users[0].getSkillsGroupSummary(proj1.projectId, p1_group1.skillId.toString())
        then:
        res1.group == p1_group1.name.toString()
        res1.groupId == p1_group1.skillId.toString()
        res1.totalSkills == 4
        res1.skillsAchieved == 3
        res1.skills.skillId == proj1_skills[1..4].skillId
        res1.skills.skill == proj1_skills[1..4].name
        res1.skills.totalPoints == [200, 200, 200, 200]
        res1.skills.points == [200, 200, 200, 100]
        res1.skills.todaysPoints == [200, 200, 200, 100]
        res1.skills.type == ['Skill', 'Skill', 'Skill', 'Skill']
        res1.skills.pointIncrement == [100, 100, 100, 100]
        res1.skills.description == [null, null, null, null]
        res1.skills.groupSkillId ==  [p1_group1.skillId.toString(), p1_group1.skillId.toString(), p1_group1.skillId.toString(), p1_group1.skillId.toString()]
    }

    def "learning path under a group" () {
        List<SkillsService> users = getRandomUsers(5).collect { createService(it) }

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(8, 1, 1)
        proj1_skills.each { it.numPerformToCompletion = 2; it.pointIncrementInterval = 0; it.description = "blah1" }
        def p1_group1 = SkillsFactory.createSkillsGroup(1, 1, 10)
        def p1_group2 = SkillsFactory.createSkillsGroup(1, 1, 11)
        skillsService.createProjectAndSubjectAndSkills(proj1, proj1_subj, [proj1_skills[0], p1_group1, p1_group2])

        proj1_skills[1..4].each {
            skillsService.assignSkillToSkillsGroup(p1_group1.skillId.toString(), it)
        }

        users[0].addSkill(proj1_skills[0])
        users[0].addSkill(proj1_skills[0])
        users[0].addSkill(proj1_skills[1])
        users[0].addSkill(proj1_skills[1])
        users[0].addSkill(proj1_skills[2])
        users[0].addSkill(proj1_skills[2])
        users[0].addSkill(proj1_skills[3])
        users[0].addSkill(proj1_skills[3])
        users[0].addSkill(proj1_skills[4])

        skillsService.addLearningPathPrerequisite(proj1.projectId, proj1_skills[0].skillId.toString(), proj1.projectId, proj1_skills[1].skillId.toString())
        skillsService.addLearningPathPrerequisite(proj1.projectId, proj1_skills[1].skillId.toString(), proj1.projectId, proj1_skills[2].skillId.toString())
        skillsService.addLearningPathPrerequisite(proj1.projectId, proj1_skills[2].skillId.toString(), proj1.projectId, proj1_skills[3].skillId.toString())
        skillsService.addLearningPathPrerequisite(proj1.projectId, proj1_skills[2].skillId.toString(), proj1.projectId, proj1_skills[4].skillId.toString())

        when:
        def res1 = users[0].getSkillsGroupSummary(proj1.projectId, p1_group1.skillId.toString())
        then:
        res1.group == p1_group1.name.toString()
        res1.groupId == p1_group1.skillId.toString()
        res1.totalSkills == 4
        res1.skillsAchieved == 3
        res1.skills.skillId == proj1_skills[1..4].skillId
        res1.skills.skill == proj1_skills[1..4].name
        res1.skills.totalPoints == [20, 20, 20, 20]
        res1.skills.points == [20, 20, 20,10]
        res1.skills.todaysPoints == [20, 20, 20, 10]
        res1.skills.type == ['Skill', 'Skill', 'Skill', 'Skill']
        res1.skills.pointIncrement == [10, 10, 10, 10]
        res1.skills.description == [null, null, null, null]
        res1.skills.groupSkillId ==  [p1_group1.skillId.toString(), p1_group1.skillId.toString(), p1_group1.skillId.toString(), p1_group1.skillId.toString()]
        res1.skills.dependencyInfo == [
                [numDirectDependents:1, achieved:true],
                [numDirectDependents:2, achieved:false],
                null,
                null
        ]
    }

    // badges
    // self report such as quizzes?
}
