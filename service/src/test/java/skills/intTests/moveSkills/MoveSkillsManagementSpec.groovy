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
package skills.intTests.moveSkills

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory

import static skills.intTests.utils.SkillsFactory.*

class MoveSkillsManagementSpec extends DefaultIntSpec {

    def "move skill from subject into another subject"() {
        def p1 = createProject(1)

        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)

        when:
        skillsService.moveSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId)

        def projStat = skillsService.getProject(p1.projectId)

        def subj1Skills = skillsService.getSkillsForSubject(p1.projectId, p1subj1.subjectId)
        def subj2Skills = skillsService.getSkillsForSubject(p1.projectId, p1subj2.subjectId)

        def subj1Stats = skillsService.getSubject(p1subj1)
        def subj2Stats = skillsService.getSubject(p1subj2)

        def skillAdminInfo = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj2.subjectId, skillId: p1Skills[0].skillId])
        then:
        projStat.numSubjects == 2
        projStat.numSkills == 3
        projStat.totalPoints == 300
        projStat.numSkillsReused == 0
        projStat.totalPointsReused == 0

        subj1Skills.skillId == [p1Skills[1].skillId, p1Skills[2].skillId]

        subj2Skills.size() == 1
        subj2Skills[0].skillId == p1Skills[0].skillId
        subj2Skills[0].name == p1Skills[0].name
        !subj2Skills[0].reusedSkill
        subj2Skills[0].totalPoints == 100

        subj1Stats.numSkills == 2
        subj1Stats.totalPoints == 200
        subj1Stats.numSkillsReused == 0
        subj1Stats.totalPointsReused == 0

        subj2Stats.numSkills == 1
        subj2Stats.totalPoints == 100
        subj2Stats.numSkillsReused == 0
        subj2Stats.totalPointsReused == 0

        !skillAdminInfo.reusedSkill
        skillAdminInfo.skillId == p1Skills[0].skillId
        skillAdminInfo.name == p1Skills[0].name
    }

    def "move skill from group into another subject"() {
        def p1 = createProject(1)

        def p1subj1 = createSubject(1, 1)
        def p1subj1g1 = createSkillsGroup(1, 1, 8)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [])
        skillsService.createSkill(p1subj1g1)
        def p1Skills = createSkills(3, 1, 1, 100)
        p1Skills.each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }

        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)

        when:
        skillsService.moveSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId)

        def projStat = skillsService.getProject(p1.projectId)

        def subj1Skills = skillsService.getSkillsForSubject(p1.projectId, p1subj1.subjectId)
        def origGroup1Skills = skillsService.getSkillsForGroup(p1.projectId, p1subj1g1.skillId)
        def subj2Skills = skillsService.getSkillsForSubject(p1.projectId, p1subj2.subjectId)

        def subj1Stats = skillsService.getSubject(p1subj1)
        def subj2Stats = skillsService.getSubject(p1subj2)

        def skillAdminInfo = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj2.subjectId, skillId: p1Skills[0].skillId])
        then:
        projStat.numSubjects == 2
        projStat.numSkills == 3
        projStat.totalPoints == 300
        projStat.numSkillsReused == 0
        projStat.totalPointsReused == 0

        subj1Skills.skillId == [p1subj1g1.skillId]
        origGroup1Skills.skillId == [p1Skills[1].skillId, p1Skills[2].skillId]

        subj2Skills.size() == 1
        subj2Skills[0].skillId == p1Skills[0].skillId
        subj2Skills[0].name == p1Skills[0].name
        !subj2Skills[0].reusedSkill
        subj2Skills[0].totalPoints == 100

        subj2Stats.numSkills == 1
        subj2Stats.totalPoints == 100
        subj2Stats.numSkillsReused == 0
        subj2Stats.totalPointsReused == 0

        subj1Stats.numSkills == 2
        subj1Stats.totalPoints == 200
        subj1Stats.numSkillsReused == 0
        subj1Stats.totalPointsReused == 0

        !skillAdminInfo.reusedSkill
        skillAdminInfo.skillId == p1Skills[0].skillId
        skillAdminInfo.name == p1Skills[0].name

        skillDefRepo.findByProjectIdAndSkillId(p1.projectId, p1subj1g1.skillId).totalPoints == 200
    }

    def "move skill from subject into a group under another subject"() {
        def p1 = createProject(1)

        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)
        def p1subj2g1 = createSkillsGroup(1, 2, 8)
        skillsService.createSkill(p1subj2g1)

        when:
        skillsService.moveSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId, p1subj2g1.skillId)

        def projStat = skillsService.getProject(p1.projectId)

        def subj1Skills = skillsService.getSkillsForSubject(p1.projectId, p1subj1.subjectId)
        def subj2Skills = skillsService.getSkillsForSubject(p1.projectId, p1subj2.subjectId)
        def subj2G1Skills = skillsService.getSkillsForGroup(p1.projectId, p1subj2g1.skillId)

        def subj1Stats = skillsService.getSubject(p1subj1)
        def subj2Stats = skillsService.getSubject(p1subj2)

        def skillAdminInfo = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj2.subjectId, skillId: p1Skills[0].skillId])
        then:
        projStat.numSubjects == 2
        projStat.numSkills == 3
        projStat.totalPoints == 300
        projStat.numSkillsReused == 0
        projStat.totalPointsReused == 0

        subj1Skills.skillId == [p1Skills[1].skillId, p1Skills[2].skillId]

        subj2Skills.skillId == [p1subj2g1.skillId]

        subj2G1Skills.skillId == [p1Skills[0].skillId]
        subj2G1Skills[0].name == p1Skills[0].name
        subj2G1Skills[0].totalPoints == 100

        subj1Stats.numSkills == 2
        subj1Stats.totalPoints == 200
        subj1Stats.numSkillsReused == 0
        subj1Stats.totalPointsReused == 0

        subj2Stats.numSkills == 1
        subj2Stats.totalPoints == 100
        subj2Stats.numSkillsReused == 0
        subj2Stats.totalPointsReused == 0

        !skillAdminInfo.reusedSkill
        skillAdminInfo.skillId == p1Skills[0].skillId
        skillAdminInfo.name == p1Skills[0].name
    }

    def "move skill from group into a group under another subject"() {
        def p1 = createProject(1)

        def p1subj1 = createSubject(1, 1)
        def p1subj1g1 = createSkillsGroup(1, 1, 8)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [])
        skillsService.createSkill(p1subj1g1)
        def p1Skills = createSkills(3, 1, 1, 100)
        p1Skills.each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }

        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)
        def p1subj2g1 = createSkillsGroup(1, 2, 8)
        skillsService.createSkill(p1subj2g1)

        when:
        skillsService.moveSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId, p1subj2g1.skillId)

        def projStat = skillsService.getProject(p1.projectId)

        def subj1Skills = skillsService.getSkillsForSubject(p1.projectId, p1subj1.subjectId)
        def subj1G1Skills = skillsService.getSkillsForGroup(p1.projectId, p1subj1g1.skillId)
        def subj2Skills = skillsService.getSkillsForSubject(p1.projectId, p1subj2.subjectId)
        def subj2G1Skills = skillsService.getSkillsForGroup(p1.projectId, p1subj2g1.skillId)

        def subj1Stats = skillsService.getSubject(p1subj1)
        def subj2Stats = skillsService.getSubject(p1subj2)

        def skillAdminInfo = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj2.subjectId, skillId: p1Skills[0].skillId])
        then:
        projStat.numSubjects == 2
        projStat.numSkills == 3
        projStat.totalPoints == 300
        projStat.numSkillsReused == 0
        projStat.totalPointsReused == 0

        subj1Skills.skillId == [p1subj1g1.skillId]
        subj1G1Skills.skillId == [p1Skills[1].skillId, p1Skills[2].skillId]

        subj2Skills.skillId == [p1subj2g1.skillId]

        subj2G1Skills.skillId == [p1Skills[0].skillId]
        subj2G1Skills[0].name == p1Skills[0].name
        subj2G1Skills[0].totalPoints == 100

        subj1Stats.numSkills == 2
        subj1Stats.totalPoints == 200
        subj1Stats.numSkillsReused == 0
        subj1Stats.totalPointsReused == 0

        subj2Stats.numSkills == 1
        subj2Stats.totalPoints == 100
        subj2Stats.numSkillsReused == 0
        subj2Stats.totalPointsReused == 0

        !skillAdminInfo.reusedSkill
        skillAdminInfo.skillId == p1Skills[0].skillId
        skillAdminInfo.name == p1Skills[0].name
        skillAdminInfo.groupId == p1subj2g1.skillId
    }

    def "move skill from group into a group under the same subject"() {
        def p1 = createProject(1)

        def p1subj1 = createSubject(1, 1)
        def p1subj1g1 = createSkillsGroup(1, 1, 8)
        def p1subj1g2 = createSkillsGroup(1, 1, 9)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [p1subj1g1, p1subj1g2])
        def p1Skills = createSkills(3, 1, 1, 100)
        p1Skills.each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }

        when:
        skillsService.moveSkills(p1.projectId, [p1Skills[0].skillId], p1subj1g1.subjectId, p1subj1g2.skillId)

        def projStat = skillsService.getProject(p1.projectId)

        def subj1Skills = skillsService.getSkillsForSubject(p1.projectId, p1subj1.subjectId)
        def subj1G1Skills = skillsService.getSkillsForGroup(p1.projectId, p1subj1g1.skillId)
        def subj1G2Skills = skillsService.getSkillsForGroup(p1.projectId, p1subj1g2.skillId)

        def subj1Stats = skillsService.getSubject(p1subj1)

        def skillAdminInfo = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        then:
        projStat.numSubjects == 1
        projStat.numSkills == 3
        projStat.totalPoints == 300
        projStat.numSkillsReused == 0
        projStat.totalPointsReused == 0

        subj1Skills.skillId == [p1subj1g1.skillId, p1subj1g2.skillId]
        subj1G1Skills.skillId == [p1Skills[1].skillId, p1Skills[2].skillId]

        subj1G2Skills.skillId == [p1Skills[0].skillId]
        subj1G2Skills[0].name == p1Skills[0].name
        subj1G2Skills[0].totalPoints == 100

        subj1Stats.numSkills == 3
        subj1Stats.totalPoints == 300
        subj1Stats.numSkillsReused == 0
        subj1Stats.totalPointsReused == 0

        !skillAdminInfo.reusedSkill
        skillAdminInfo.skillId == p1Skills[0].skillId
        skillAdminInfo.name == p1Skills[0].name
        skillAdminInfo.groupId == p1subj1g2.skillId
    }

    def "move skill from group into a parent subject"() {
        def p1 = createProject(1)

        def p1subj1 = createSubject(1, 1)
        def p1subj1g1 = createSkillsGroup(1, 1, 8)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [p1subj1g1])
        def p1Skills = createSkills(3, 1, 1, 100)
        p1Skills.each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }

        when:
        skillsService.moveSkills(p1.projectId, [p1Skills[0].skillId], p1subj1.subjectId)

        def projStat = skillsService.getProject(p1.projectId)

        def subj1Skills = skillsService.getSkillsForSubject(p1.projectId, p1subj1.subjectId)
        def subj1G1Skills = skillsService.getSkillsForGroup(p1.projectId, p1subj1g1.skillId)

        def subj1Stats = skillsService.getSubject(p1subj1)

        def skillAdminInfo = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        then:
        projStat.numSubjects == 1
        projStat.numSkills == 3
        projStat.totalPoints == 300
        projStat.numSkillsReused == 0
        projStat.totalPointsReused == 0

        subj1Skills.skillId == [p1subj1g1.skillId, p1Skills[0].skillId]
        subj1G1Skills.skillId == [p1Skills[1].skillId, p1Skills[2].skillId]

        subj1Stats.numSkills == 3
        subj1Stats.totalPoints == 300
        subj1Stats.numSkillsReused == 0
        subj1Stats.totalPointsReused == 0

        !skillAdminInfo.reusedSkill
        skillAdminInfo.skillId == p1Skills[0].skillId
        skillAdminInfo.name == p1Skills[0].name
        !skillAdminInfo.groupId
    }

    def "move skill from subject into a child group"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj1g1 = createSkillsGroup(1, 1, 8)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [p1subj1g1, p1Skills].flatten())

        when:
        skillsService.moveSkills(p1.projectId, [p1Skills[0].skillId], p1subj1.subjectId, p1subj1g1.skillId)

        def projStat = skillsService.getProject(p1.projectId)

        def subj1Skills = skillsService.getSkillsForSubject(p1.projectId, p1subj1.subjectId)
        def subj1G1Skills = skillsService.getSkillsForGroup(p1.projectId, p1subj1g1.skillId)

        def subj1Stats = skillsService.getSubject(p1subj1)

        def skillAdminInfo = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        then:
        projStat.numSubjects == 1
        projStat.numSkills == 3
        projStat.totalPoints == 300
        projStat.numSkillsReused == 0
        projStat.totalPointsReused == 0

        subj1Skills.skillId == [p1subj1g1.skillId, p1Skills[1].skillId, p1Skills[2].skillId]
        subj1G1Skills.skillId == [p1Skills[0].skillId]

        subj1Stats.numSkills == 3
        subj1Stats.totalPoints == 300
        subj1Stats.numSkillsReused == 0
        subj1Stats.totalPointsReused == 0

        !skillAdminInfo.reusedSkill
        skillAdminInfo.skillId == p1Skills[0].skillId
        skillAdminInfo.name == p1Skills[0].name
        skillAdminInfo.groupId == p1subj1g1.skillId
    }

    def "move multiple skills from subject into another subject"() {
        def p1 = createProject(1)

        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)

        when:
        skillsService.moveSkills(p1.projectId, [p1Skills[0].skillId, p1Skills[2].skillId], p1subj2.subjectId)

        def projStat = skillsService.getProject(p1.projectId)

        def subj1Skills = skillsService.getSkillsForSubject(p1.projectId, p1subj1.subjectId)
        def subj2Skills = skillsService.getSkillsForSubject(p1.projectId, p1subj2.subjectId)

        def subj1Stats = skillsService.getSubject(p1subj1)
        def subj2Stats = skillsService.getSubject(p1subj2)

        def skill1AdminInfo = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj2.subjectId, skillId: p1Skills[0].skillId])
        def skill2AdminInfo = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[1].skillId])
        def skill3AdminInfo = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj2.subjectId, skillId: p1Skills[2].skillId])
        then:
        projStat.numSubjects == 2
        projStat.numSkills == 3
        projStat.totalPoints == 300
        projStat.numSkillsReused == 0
        projStat.totalPointsReused == 0

        subj1Skills.skillId == [p1Skills[1].skillId]
        subj2Skills.skillId == [p1Skills[0].skillId, p1Skills[2].skillId]
        subj2Skills.name == [p1Skills[0].name, p1Skills[2].name]
        subj2Skills.totalPoints == [100, 100]

        subj1Stats.numSkills == 1
        subj1Stats.totalPoints == 100

        subj2Stats.numSkills == 2
        subj2Stats.totalPoints == 200

        skill1AdminInfo.skillId == p1Skills[0].skillId
        skill2AdminInfo.skillId == p1Skills[1].skillId
        skill3AdminInfo.skillId == p1Skills[2].skillId
    }

    def "move multiple skills from group into a group under another subject"() {
        def p1 = createProject(1)

        def p1subj1 = createSubject(1, 1)
        def p1subj1g1 = createSkillsGroup(1, 1, 8)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [])
        skillsService.createSkill(p1subj1g1)
        def p1Skills = createSkills(3, 1, 1, 100)
        p1Skills.each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }

        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)
        def p1subj2g1 = createSkillsGroup(1, 2, 8)
        skillsService.createSkill(p1subj2g1)

        when:
        skillsService.moveSkills(p1.projectId, [p1Skills[0].skillId, p1Skills[2].skillId], p1subj2.subjectId, p1subj2g1.skillId)

        def projStat = skillsService.getProject(p1.projectId)

        def subj1Skills = skillsService.getSkillsForSubject(p1.projectId, p1subj1.subjectId)
        def subj1G1Skills = skillsService.getSkillsForGroup(p1.projectId, p1subj1g1.skillId)
        def subj2Skills = skillsService.getSkillsForSubject(p1.projectId, p1subj2.subjectId)
        def subj2G1Skills = skillsService.getSkillsForGroup(p1.projectId, p1subj2g1.skillId)

        def subj1Stats = skillsService.getSubject(p1subj1)
        def subj2Stats = skillsService.getSubject(p1subj2)

        def skill1AdminInfo = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj2.subjectId, skillId: p1Skills[0].skillId])
        def skill2AdminInfo = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[1].skillId])
        def skill3AdminInfo = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj2.subjectId, skillId: p1Skills[2].skillId])
        then:
        projStat.numSubjects == 2
        projStat.numSkills == 3
        projStat.totalPoints == 300
        projStat.numSkillsReused == 0
        projStat.totalPointsReused == 0

        subj1Skills.skillId == [p1subj1g1.skillId]
        subj1G1Skills.skillId == [p1Skills[1].skillId]

        subj2Skills.skillId == [p1subj2g1.skillId]

        subj2G1Skills.skillId == [p1Skills[0].skillId, p1Skills[2].skillId]
        subj2G1Skills.name == [p1Skills[0].name, p1Skills[2].name]
        subj2G1Skills.totalPoints == [100, 100]

        subj1Stats.numSkills == 1
        subj1Stats.totalPoints == 100

        subj2Stats.numSkills == 2
        subj2Stats.totalPoints == 200

        skill1AdminInfo.skillId == p1Skills[0].skillId
        skill2AdminInfo.skillId == p1Skills[1].skillId
        skill3AdminInfo.skillId == p1Skills[2].skillId
    }

    def "skill dependencies are retained after the move from group into a group under another subject"() {
        def p1 = createProject(1)

        def p1subj1 = createSubject(1, 1)
        def p1subj1g1 = createSkillsGroup(1, 1, 8)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [])
        skillsService.createSkill(p1subj1g1)
        def p1Skills = createSkills(3, 1, 1, 100)
        p1Skills.each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }

        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)
        def p1subj2g1 = createSkillsGroup(1, 2, 8)
        skillsService.createSkill(p1subj2g1)

        // add moved skill in the middle of a graph chain
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[1].skillId, p1Skills[0].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[0].skillId, p1Skills[2].skillId)

        def graph_before = skillsService.getDependencyGraph(p1.projectId)

        when:
        skillsService.moveSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId, p1subj2g1.skillId)

        def graph_after = skillsService.getDependencyGraph(p1.projectId)

        then:
        validateGraph(graph_before, [
                new Edge(from: p1Skills[1].skillId, to: p1Skills[0].skillId),
                new Edge(from: p1Skills[0].skillId, to: p1Skills[2].skillId),
        ])

        validateGraph(graph_after, [
                new Edge(from: p1Skills[1].skillId, to: p1Skills[0].skillId),
                new Edge(from: p1Skills[0].skillId, to: p1Skills[2].skillId),
        ])
    }

    def "skill dependencies are retained after the move from subject into another subject"() {
        def p1 = createProject(1)

        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)

        // add moved skill in the middle of a graph chain
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[1].skillId, p1Skills[0].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[0].skillId, p1Skills[2].skillId)

        def graphBefore = skillsService.getDependencyGraph(p1.projectId)

        when:
        skillsService.moveSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId)

        def graph = skillsService.getDependencyGraph(p1.projectId)

        then:
        validateGraph(graphBefore, [
                new Edge(from: p1Skills[1].skillId, to: p1Skills[0].skillId),
                new Edge(from: p1Skills[0].skillId, to: p1Skills[2].skillId),
        ])

        validateGraph(graph, [
                new Edge(from: p1Skills[1].skillId, to: p1Skills[0].skillId),
                new Edge(from: p1Skills[0].skillId, to: p1Skills[2].skillId),
        ])
    }

    static class Edge {
        String from
        String to
    }

    private void validateGraph(def graph, List<Edge> expectedGraphRel) {
        def skill0IdMap0_before = graph.nodes.collectEntries { [it.skillId, it.id] }
        assert graph.edges.collect { "${it.fromId}->${it.toId}" }.sort() == expectedGraphRel.collect {
            "${skill0IdMap0_before.get(it.from)}->${skill0IdMap0_before.get(it.to)}"
        }.sort()
    }

    def "badge skills are retained after the move from subject into another subject"() {
        def p1 = createProject(1)

        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)

        def badge = createBadge(1, 1)
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge(p1.projectId, badge.badgeId, p1Skills[0].skillId)
        skillsService.assignSkillToBadge(p1.projectId, badge.badgeId, p1Skills[1].skillId)
        badge.enabled = true
        skillsService.updateBadge(badge, badge.badgeId)

        String userId = getRandomUsers(1)[0]
        when:
        def badgeSummaryBefore = skillsService.getBadgeSummary(userId, p1.projectId, badge.badgeId)
        skillsService.moveSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId)
        def badgeSummaryAfter = skillsService.getBadgeSummary(userId, p1.projectId, badge.badgeId)
        then:
        badgeSummaryBefore.numTotalSkills == 2
        badgeSummaryBefore.skills.skillId == [p1Skills[0].skillId, p1Skills[1].skillId]
        badgeSummaryAfter.numTotalSkills == 2
        badgeSummaryAfter.skills.skillId == [p1Skills[0].skillId, p1Skills[1].skillId]
    }

    def "badge skills are retained after the move from group into a group under another subject"() {
        def p1 = createProject(1)

        def p1subj1 = createSubject(1, 1)
        def p1subj1g1 = createSkillsGroup(1, 1, 8)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [])
        skillsService.createSkill(p1subj1g1)
        def p1Skills = createSkills(3, 1, 1, 100)
        p1Skills.each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }

        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)
        def p1subj2g1 = createSkillsGroup(1, 2, 8)
        skillsService.createSkill(p1subj2g1)

        def badge = createBadge(1, 1)
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge(p1.projectId, badge.badgeId, p1Skills[0].skillId)
        skillsService.assignSkillToBadge(p1.projectId, badge.badgeId, p1Skills[1].skillId)
        badge.enabled = true
        skillsService.updateBadge(badge, badge.badgeId)

        String userId = getRandomUsers(1)[0]
        when:
        def badgeSummaryBefore = skillsService.getBadgeSummary(userId, p1.projectId, badge.badgeId)
        skillsService.moveSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId)
        def badgeSummaryAfter = skillsService.getBadgeSummary(userId, p1.projectId, badge.badgeId)
        then:
        badgeSummaryBefore.numTotalSkills == 2
        badgeSummaryBefore.skills.skillId == [p1Skills[0].skillId, p1Skills[1].skillId]
        badgeSummaryAfter.numTotalSkills == 2
        badgeSummaryAfter.skills.skillId == [p1Skills[0].skillId, p1Skills[1].skillId]
    }

    def "cross-project deps are retained after the skill is moved from subject to another subject"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        def proj1_subj2 = SkillsFactory.createSubject(1, 2)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj1, proj1_subj, proj1_skills)
        skillsService.createSubject(proj1_subj2)

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 3)
        def proj2_subj2 = SkillsFactory.createSubject(2, 4)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 3)
        skillsService.createProjectAndSubjectAndSkills(proj2, proj2_subj, proj2_skills)
        skillsService.createSubject(proj2_subj2)

        skillsService.shareSkill(proj1.projectId, proj1_skills.get(0).skillId, proj2.projectId)
        skillsService.addLearningPathPrerequisite(proj2.projectId, proj2_skills.get(0).skillId, proj1.projectId, proj1_skills.get(0).skillId)

        String user = getRandomUsers(1)[0]
        when:
        def deps_t0 = skillsService.getSkillDependencyInfo(user, proj2.projectId, proj2_skills.get(0).skillId)
        skillsService.moveSkills(proj1.projectId, [proj1_skills[0].skillId], proj1_subj2.subjectId)
        def deps_t1 = skillsService.getSkillDependencyInfo(user, proj2.projectId, proj2_skills.get(0).skillId)
        skillsService.moveSkills(proj2.projectId, [proj2_skills[0].skillId], proj2_subj2.subjectId)
        def deps_t2 = skillsService.getSkillDependencyInfo(user, proj2.projectId, proj2_skills.get(0).skillId)
        then:
        deps_t0.dependencies.dependsOn.skillId == [proj1_skills.get(0).skillId]
        deps_t0.dependencies.dependsOn.projectId == [proj1_skills.get(0).projectId]

        deps_t1.dependencies.dependsOn.skillId == [proj1_skills.get(0).skillId]
        deps_t1.dependencies.dependsOn.projectId == [proj1_skills.get(0).projectId]

        deps_t2.dependencies.dependsOn.skillId == [proj1_skills.get(0).skillId]
        deps_t2.dependencies.dependsOn.projectId == [proj1_skills.get(0).projectId]
    }

    def "cross-project deps are retained after the skill is moved from group to another group"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        def proj1_subj2 = SkillsFactory.createSubject(1, 2)
        def proj1_subj1_group1 = SkillsFactory.createSkillsGroup(1, 1, 11)
        def proj1_subj2_group2 = SkillsFactory.createSkillsGroup(1, 2, 22)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj1, proj1_subj, [proj1_subj1_group1])
        proj1_skills.each {
            skillsService.assignSkillToSkillsGroup(proj1_subj1_group1.skillId, it)
        }
        skillsService.createSubject(proj1_subj2)
        skillsService.createSkill(proj1_subj2_group2)

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 3)
        def proj2_subj2 = SkillsFactory.createSubject(2, 4)
        def proj2_subj1_group1 = SkillsFactory.createSkillsGroup(2, 3, 33)
        def proj2_subj2_group2 = SkillsFactory.createSkillsGroup(2, 4, 44)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 3)
        skillsService.createProjectAndSubjectAndSkills(proj2, proj2_subj, [proj2_subj1_group1])
        proj2_skills.each {
            skillsService.assignSkillToSkillsGroup(proj2_subj1_group1.skillId, it)
        }
        skillsService.createSubject(proj2_subj2)
        skillsService.createSkill(proj2_subj2_group2)

        skillsService.shareSkill(proj1.projectId, proj1_skills.get(0).skillId, proj2.projectId)
        skillsService.addLearningPathPrerequisite(proj2.projectId, proj2_skills.get(0).skillId, proj1.projectId, proj1_skills.get(0).skillId)

        String user = getRandomUsers(1)[0]
        when:
        def deps_t0 = skillsService.getSkillDependencyInfo(user, proj2.projectId, proj2_skills.get(0).skillId)
        skillsService.moveSkills(proj1.projectId, [proj1_skills[0].skillId], proj1_subj2.subjectId, proj1_subj2_group2.skillId)
        def deps_t1 = skillsService.getSkillDependencyInfo(user, proj2.projectId, proj2_skills.get(0).skillId)
        skillsService.moveSkills(proj2.projectId, [proj2_skills[0].skillId], proj2_subj2.subjectId, proj2_subj2_group2.skillId)
        def deps_t2 = skillsService.getSkillDependencyInfo(user, proj2.projectId, proj2_skills.get(0).skillId)
        then:
        deps_t0.dependencies.dependsOn.skillId == [proj1_skills.get(0).skillId]
        deps_t0.dependencies.dependsOn.projectId == [proj1_skills.get(0).projectId]

        deps_t1.dependencies.dependsOn.skillId == [proj1_skills.get(0).skillId]
        deps_t1.dependencies.dependsOn.projectId == [proj1_skills.get(0).projectId]

        deps_t2.dependencies.dependsOn.skillId == [proj1_skills.get(0).skillId]
        deps_t2.dependencies.dependsOn.projectId == [proj1_skills.get(0).projectId]
    }

    def "skill exported to the catalog continues to work after skills is moved between groups"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1skillsGroup1 = SkillsFactory.createSkillsGroup(1, 1, 25)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [p1skillsGroup1])
        p1Skills.each { skillsService.assignSkillToSkillsGroup(p1skillsGroup1.skillId, it) }
        def p1subj2 = createSubject(1, 2)
        def p1skillsGroup2 = SkillsFactory.createSkillsGroup(1, 2, 50)
        skillsService.createSubject(p1subj2)
        skillsService.createSkill(p1skillsGroup2)
        skillsService.bulkExportSkillsToCatalog(p1.projectId, p1Skills.collect { it.skillId })

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 2)
        def p2skillsGroup = SkillsFactory.createSkillsGroup(2, 2, 5)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [p2skillsGroup])
        def p2Skills = createSkills(2, 2, 2, 100)
        p2Skills.each { skillsService.assignSkillToSkillsGroup(p2skillsGroup.skillId, it) }

        skillsService.bulkImportSkillsIntoGroupFromCatalogAndFinalize(p2.projectId, p2subj1.subjectId, p2skillsGroup.skillId,
                [[projectId: p1.projectId, skillId: p1Skills[0].skillId]])

        when:
        def p2skillsGroupSkills_t1 = skillsService.getSkillsForGroup(p2.projectId, p2skillsGroup.skillId)
        def exportedSkills_t1 = skillsService.getExportedSkills(p1.projectId, 10, 1, "skillName", true)
        skillsService.moveSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId, p1skillsGroup2.skillId)

        def p2skillsGroupSkills_t2 = skillsService.getSkillsForGroup(p2.projectId, p2skillsGroup.skillId)
        def exportedSkills_t2 = skillsService.getExportedSkills(p1.projectId, 10, 1, "skillName", true)

        p1Skills[0].subjectId = p1subj2.subjectId
        p1Skills[0].name = "What a cool name"
        skillsService.createSkill(p1Skills[0])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        def updatedImportedSkill = skillsService.getSkill([projectId: p2.projectId, subjectId: p2subj1.subjectId, skillId: p1Skills[0].skillId])

        then:
        p2skillsGroupSkills_t1.skillId == [p2Skills[0].skillId, p2Skills[1].skillId, p1Skills[0].skillId]
        p2skillsGroupSkills_t2.skillId == [p2Skills[0].skillId, p2Skills[1].skillId, p1Skills[0].skillId]

        exportedSkills_t1.data.skillId == [p1Skills[0].skillId, p1Skills[1].skillId, p1Skills[2].skillId]
        exportedSkills_t2.data.skillId == [p1Skills[0].skillId, p1Skills[1].skillId, p1Skills[2].skillId]

        updatedImportedSkill.name == "What a cool name"
    }

    def "skill exported to the catalog continues to work after skills is moved between subjects"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)
        skillsService.bulkExportSkillsToCatalog(p1.projectId, p1Skills.collect { it.skillId })

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 2)
        def p2skillsGroup = SkillsFactory.createSkillsGroup(2, 2, 5)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [p2skillsGroup])
        def p2Skills = createSkills(2, 2, 2, 100)
        p2Skills.each { skillsService.assignSkillToSkillsGroup(p2skillsGroup.skillId, it) }

        skillsService.bulkImportSkillsIntoGroupFromCatalogAndFinalize(p2.projectId, p2subj1.subjectId, p2skillsGroup.skillId,
                [[projectId: p1.projectId, skillId: p1Skills[0].skillId]])

        when:
        def p2skillsGroupSkills_t1 = skillsService.getSkillsForGroup(p2.projectId, p2skillsGroup.skillId)
        def exportedSkills_t1 = skillsService.getExportedSkills(p1.projectId, 10, 1, "skillName", true)
        skillsService.moveSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId)

        def p2skillsGroupSkills_t2 = skillsService.getSkillsForGroup(p2.projectId, p2skillsGroup.skillId)
        def exportedSkills_t2 = skillsService.getExportedSkills(p1.projectId, 10, 1, "skillName", true)

        p1Skills[0].subjectId = p1subj2.subjectId
        p1Skills[0].name = "What a cool name"
        skillsService.createSkill(p1Skills[0])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        def updatedImportedSkill = skillsService.getSkill([projectId: p2.projectId, subjectId: p2subj1.subjectId, skillId: p1Skills[0].skillId])

        then:
        p2skillsGroupSkills_t1.skillId == [p2Skills[0].skillId, p2Skills[1].skillId, p1Skills[0].skillId]
        p2skillsGroupSkills_t2.skillId == [p2Skills[0].skillId, p2Skills[1].skillId, p1Skills[0].skillId]

        exportedSkills_t1.data.skillId == [p1Skills[0].skillId, p1Skills[1].skillId, p1Skills[2].skillId]
        exportedSkills_t2.data.skillId == [p1Skills[0].skillId, p1Skills[1].skillId, p1Skills[2].skillId]

        updatedImportedSkill.name == "What a cool name"
    }

    def "display order is updated after skills moved from subject into another subject"() {
        def p1 = createProject(1)

        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(5, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)
        def p1SkillsSubj2 = createSkills(2, 1, 2, 100)
        skillsService.createSkills(p1SkillsSubj2)

        when:
        skillsService.moveSkills(p1.projectId, [p1Skills[1].skillId, p1Skills[2].skillId], p1subj2.subjectId)
        def subj1Skills = skillsService.getSkillsForSubject(p1.projectId, p1subj1.subjectId).sort { it.displayOrder }
        def subj2Skills = skillsService.getSkillsForSubject(p1.projectId, p1subj2.subjectId).sort { it.displayOrder }

        then:
        subj1Skills.skillId == [p1Skills[0].skillId, p1Skills[3].skillId, p1Skills[4].skillId]
        subj1Skills.displayOrder == [1, 2, 3]

        subj2Skills.skillId == [p1SkillsSubj2[0].skillId, p1SkillsSubj2[1].skillId, p1Skills[1].skillId, p1Skills[2].skillId]
        subj2Skills.displayOrder == [1, 2, 3, 4]

    }

    def "display order is updated after skills moved from group into a group under another subject"() {
        def p1 = createProject(1)

        def p1subj1 = createSubject(1, 1)
        def p1subj1g1 = createSkillsGroup(1, 1, 8)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [])
        skillsService.createSkill(p1subj1g1)
        def p1Skills = createSkills(5, 1, 1, 100)
        p1Skills.each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }

        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)
        def p1subj2g1 = createSkillsGroup(1, 2, 8)
        skillsService.createSkill(p1subj2g1)
        def p1SkillsGroup2 = createSkills(2, 1, 2, 100)
        p1SkillsGroup2.each {
            skillsService.assignSkillToSkillsGroup(p1subj2g1.skillId, it)
        }

        when:
        skillsService.moveSkills(p1.projectId, [p1Skills[1].skillId, p1Skills[2].skillId], p1subj2.subjectId, p1subj2g1.skillId)

        then:
        def group1Skills = skillsService.getSkillsForGroup(p1.projectId, p1subj1g1.skillId).sort { it.displayOrder }
        def group2Skills = skillsService.getSkillsForGroup(p1.projectId, p1subj2g1.skillId).sort { it.displayOrder }

        then:
        group1Skills.skillId == [p1Skills[0].skillId, p1Skills[3].skillId, p1Skills[4].skillId]
        group1Skills.displayOrder == [1, 2, 3]

        group2Skills.skillId == [p1SkillsGroup2[0].skillId, p1SkillsGroup2[1].skillId, p1Skills[1].skillId, p1Skills[2].skillId]
        group2Skills.displayOrder == [1, 2, 3, 4]
    }

    def "when skills moved out of a group AND (numSkillsRequired == # remaining); then reset numSkillsRequired to ALL"() {
        def p1 = createProject(1)

        def p1subj1 = createSubject(1, 1)
        def p1subj1g1 = createSkillsGroup(1, 1, 8)
        def p1subj1g2 = createSkillsGroup(1, 1, 9)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [p1subj1g1, p1subj1g2])
        def p1Skills = createSkills(3, 1, 1, 100)
        p1Skills.each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }
        p1subj1g1.numSkillsRequired = 2
        skillsService.createSkill(p1subj1g1)

        when:
        skillsService.moveSkills(p1.projectId, [p1Skills[0].skillId], p1subj1g1.subjectId, p1subj1g2.skillId)
        def subj1Skills = skillsService.getSkillsForSubject(p1.projectId, p1subj1.subjectId)
        def group = subj1Skills.find { it.skillId == p1subj1g1.skillId }
        then:
        group.numSkillsRequired == -1
        group.numSkillsInGroup == 2
    }

    def "when skills moved out of a group AND (numSkillsRequired > # remaining); then reset numSkillsRequired to ALL"() {
        def p1 = createProject(1)

        def p1subj1 = createSubject(1, 1)
        def p1subj1g1 = createSkillsGroup(1, 1, 8)
        def p1subj1g2 = createSkillsGroup(1, 1, 9)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [p1subj1g1, p1subj1g2])
        def p1Skills = createSkills(3, 1, 1, 100)
        p1Skills.each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }
        p1subj1g1.numSkillsRequired = 2
        skillsService.createSkill(p1subj1g1)

        when:
        skillsService.moveSkills(p1.projectId, [p1Skills[0].skillId, p1Skills[1].skillId], p1subj1g1.subjectId, p1subj1g2.skillId)
        def subj1Skills = skillsService.getSkillsForSubject(p1.projectId, p1subj1.subjectId)
        def group = subj1Skills.find { it.skillId == p1subj1g1.skillId }
        then:
        group.numSkillsRequired == -1
        group.numSkillsInGroup == 1
    }

    def "when all skills moved out of a group then reset numSkillsRequired to ALL"() {
        def p1 = createProject(1)

        def p1subj1 = createSubject(1, 1)
        def p1subj1g1 = createSkillsGroup(1, 1, 8)
        def p1subj1g2 = createSkillsGroup(1, 1, 9)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [p1subj1g1, p1subj1g2])
        def p1Skills = createSkills(3, 1, 1, 100)
        p1Skills.each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }
        p1subj1g1.numSkillsRequired = 2
        skillsService.createSkill(p1subj1g1)

        when:
        skillsService.moveSkills(p1.projectId, [p1Skills[0].skillId, p1Skills[1].skillId, p1Skills[2].skillId], p1subj1g1.subjectId, p1subj1g2.skillId)
        def subj1Skills = skillsService.getSkillsForSubject(p1.projectId, p1subj1.subjectId)
        def group = subj1Skills.find { it.skillId == p1subj1g1.skillId }
        then:
        group.numSkillsRequired == -1
        group.numSkillsInGroup == 0
    }

    def "when skills moved out of a group AND (numSkillsRequired < # remaining); then preserve numSkillsRequired"() {
        def p1 = createProject(1)

        def p1subj1 = createSubject(1, 1)
        def p1subj1g1 = createSkillsGroup(1, 1, 8)
        def p1subj1g2 = createSkillsGroup(1, 1, 9)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [p1subj1g1, p1subj1g2])
        def p1Skills = createSkills(4, 1, 1, 100)
        p1Skills.each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }
        p1subj1g1.numSkillsRequired = 2
        skillsService.createSkill(p1subj1g1)

        when:
        skillsService.moveSkills(p1.projectId, [p1Skills[0].skillId], p1subj1g1.subjectId, p1subj1g2.skillId)
        def subj1Skills = skillsService.getSkillsForSubject(p1.projectId, p1subj1.subjectId)
        def group = subj1Skills.find { it.skillId == p1subj1g1.skillId }
        then:
        group.numSkillsRequired == 2
        group.numSkillsInGroup == 3
    }

    def "move disabled skill from subject into another subject"() {
        def p1 = createProject(1)

        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        p1Skills[0].enabled = false
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)

        when:
        skillsService.moveSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId)

        def projStat = skillsService.getProject(p1.projectId)

        def subj1Skills = skillsService.getSkillsForSubject(p1.projectId, p1subj1.subjectId)
        def subj2Skills = skillsService.getSkillsForSubject(p1.projectId, p1subj2.subjectId)

        def subj1Stats = skillsService.getSubject(p1subj1)
        def subj2Stats = skillsService.getSubject(p1subj2)

        def skillAdminInfo = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj2.subjectId, skillId: p1Skills[0].skillId])
        then:
        projStat.numSubjects == 2
        projStat.numSkills == 2
        projStat.numSkillsDisabled == 1
        projStat.totalPoints == 200
        projStat.numSkillsReused == 0
        projStat.totalPointsReused == 0

        subj1Skills.skillId == [p1Skills[1].skillId, p1Skills[2].skillId]

        subj2Skills.size() == 1
        subj2Skills[0].skillId == p1Skills[0].skillId
        subj2Skills[0].name == p1Skills[0].name
        !subj2Skills[0].reusedSkill
        subj2Skills[0].totalPoints == 100

        subj1Stats.numSkills == 2
        subj1Stats.numSkillsDisabled == 0
        subj1Stats.totalPoints == 200
        subj1Stats.numSkillsReused == 0
        subj1Stats.totalPointsReused == 0

        subj2Stats.numSkills == 0
        subj2Stats.numSkillsDisabled == 1
        subj2Stats.totalPoints == 0
        subj2Stats.numSkillsReused == 0
        subj2Stats.totalPointsReused == 0

        !skillAdminInfo.reusedSkill
        skillAdminInfo.skillId == p1Skills[0].skillId
        skillAdminInfo.name == p1Skills[0].name
    }

}
