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

import org.junit.Ignore
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import spock.lang.IgnoreRest

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
        skillsService.assignDependency([projectId: p1.projectId, skillId: p1Skills[1].skillId, dependentSkillId: p1Skills[0].skillId])
        skillsService.assignDependency([projectId: p1.projectId, skillId: p1Skills[0].skillId, dependentSkillId: p1Skills[2].skillId])

        def skill0Graph_before = skillsService.getDependencyGraph(p1.projectId, p1Skills[0].skillId)
        def skill1Graph_before = skillsService.getDependencyGraph(p1.projectId, p1Skills[1].skillId)

        when:
        skillsService.moveSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId, p1subj2g1.skillId)

        def skill0Graph = skillsService.getDependencyGraph(p1.projectId, p1Skills[0].skillId)
        def skill1Graph = skillsService.getDependencyGraph(p1.projectId, p1Skills[1].skillId)

        then:
        validateGraph(skill0Graph_before, [
                new Edge(from: p1Skills[0].skillId, to: p1Skills[2].skillId)
        ])
        validateGraph(skill1Graph_before, [
                new Edge(from: p1Skills[1].skillId, to: p1Skills[0].skillId),
                new Edge(from: p1Skills[0].skillId, to: p1Skills[2].skillId),
        ])

        validateGraph(skill0Graph, [
                new Edge(from: p1Skills[0].skillId, to: p1Skills[2].skillId)
        ])
        validateGraph(skill1Graph, [
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
        skillsService.assignDependency([projectId: p1.projectId, skillId: p1Skills[1].skillId, dependentSkillId: p1Skills[0].skillId])
        skillsService.assignDependency([projectId: p1.projectId, skillId: p1Skills[0].skillId, dependentSkillId: p1Skills[2].skillId])

        def skill0Graph_before = skillsService.getDependencyGraph(p1.projectId, p1Skills[0].skillId)
        def skill1Graph_before = skillsService.getDependencyGraph(p1.projectId, p1Skills[1].skillId)

        when:
        skillsService.moveSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId)

        def skill0Graph = skillsService.getDependencyGraph(p1.projectId, p1Skills[0].skillId)
        def skill1Graph = skillsService.getDependencyGraph(p1.projectId, p1Skills[1].skillId)

        then:
        validateGraph(skill0Graph_before, [
                new Edge(from: p1Skills[0].skillId, to: p1Skills[2].skillId)
        ])
        validateGraph(skill1Graph_before, [
                new Edge(from: p1Skills[1].skillId, to: p1Skills[0].skillId),
                new Edge(from: p1Skills[0].skillId, to: p1Skills[2].skillId),
        ])

        validateGraph(skill0Graph, [
                new Edge(from: p1Skills[0].skillId, to: p1Skills[2].skillId)
        ])
        validateGraph(skill1Graph, [
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
        }
    }
}
