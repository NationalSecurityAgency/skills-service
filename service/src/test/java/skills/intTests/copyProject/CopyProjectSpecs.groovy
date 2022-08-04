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
package skills.intTests.copyProject


import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.services.admin.skillReuse.SkillReuseIdUtil

import static skills.intTests.utils.SkillsFactory.*

class CopyProjectSpecs extends DefaultIntSpec {

    def "copy project with majority of features utilized"() {
        def p1 = createProject(1)
        skillsService.createProject(p1)

        // edit levels
        def levels = skillsService.getLevels(p1.projectId, null).sort() { it.level }
        levels.each { def levelToEdit ->
            levelToEdit.percent = levelToEdit.percent + 1
            skillsService.editLevel(p1.projectId, null, levelToEdit.level as String, levelToEdit)
        }
        def projLevels = skillsService.getLevels(p1.projectId, null).sort() { it.level }

        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(10, 1, 1, 100)
        def group1 = createSkillsGroup(1, 1, 22)
        def group2 = createSkillsGroup(1, 1, 33)
        skillsService.createSubject(p1subj1)
        skillsService.createSkills([p1Skills[0..2], group1, group2].flatten())

        p1Skills[3..7].each {
            skillsService.assignSkillToSkillsGroup(group1.skillId, it)
        }
        p1Skills[8..9].each {
            skillsService.assignSkillToSkillsGroup(group2.skillId, it)
        }

        def p1subj2 = createSubject(1, 2)
        def p1SkillsSubj2 = createSkills(5, 1, 2, 22)
        def group3 = createSkillsGroup(1, 2, 33)
        skillsService.createSubject(p1subj2)

        // edit subject
        def subj1Level = skillsService.getLevels(p1.projectId, p1subj2.subjectId).sort() { it.level }
        subj1Level.each { def levelToEdit ->
            levelToEdit.percent = levelToEdit.percent + 2
            skillsService.editLevel(p1.projectId, p1subj1.subjectId, levelToEdit.level as String, levelToEdit)
        }

        skillsService.createSkills([p1SkillsSubj2, group3].flatten())

        def badge = SkillsFactory.createBadge(1, 50)
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge(p1.projectId, badge.badgeId, p1Skills[0].skillId)
        skillsService.assignSkillToBadge(p1.projectId, badge.badgeId, p1Skills[1].skillId)
        skillsService.assignSkillToBadge(p1.projectId, badge.badgeId, p1SkillsSubj2[1].skillId)

        skillsService.assignDependency([projectId: p1.projectId, skillId: p1Skills.get(0).skillId, dependentSkillId: p1Skills.get(2).skillId])
        skillsService.assignDependency([projectId: p1.projectId, skillId: p1Skills.get(2).skillId, dependentSkillId: p1SkillsSubj2.get(2).skillId])

        skillsService.reuseSkills(p1.projectId, [p1Skills[1].skillId], p1subj2.subjectId)
        skillsService.reuseSkills(p1.projectId, [p1Skills[7].skillId], p1subj2.subjectId, group3.skillId)

        def subj1UpdatedLevels = skillsService.getLevels(p1.projectId, p1subj1.subjectId).sort() { it.level }
        def subj2UpdatedLevels = skillsService.getLevels(p1.projectId, p1subj2.subjectId).sort() { it.level }

        when:
        def projToCopy = createProject(2)
        skillsService.copyProject(p1.projectId, projToCopy)

        def copiedProjStats = skillsService.getProject(projToCopy.projectId)
        def copiedSubject1 = skillsService.getSubject([subjectId: p1subj1.subjectId, projectId: projToCopy.projectId])
        def copiedSubj1Skills = skillsService.getSkillsForSubject(projToCopy.projectId, p1subj1.subjectId)
        def copiedGroup1Skills = skillsService.getSkillsForGroup(projToCopy.projectId, group1.skillId)
        def copiedGroup2Skills = skillsService.getSkillsForGroup(projToCopy.projectId, group2.skillId)

        def copiedSubject2 = skillsService.getSubject([subjectId: p1subj2.subjectId, projectId: projToCopy.projectId])
        def copiedSubj2Skills = skillsService.getSkillsForSubject(projToCopy.projectId, p1subj2.subjectId)
        def copiedGroup3Skills = skillsService.getSkillsForGroup(projToCopy.projectId, group3.skillId)

        def copiedBadge = skillsService.getBadge(projToCopy.projectId, badge.badgeId)
        def copiedDeps = skillsService.getDependencyGraph(projToCopy.projectId)

        def copiedProjLevels = skillsService.getLevels(projToCopy.projectId, null).sort() { it.level }
        def copiedSubj1UpdatedLevels = skillsService.getLevels(p1.projectId, p1subj1.subjectId).sort() { it.level }
        def copiedSubj2UpdatedLevels = skillsService.getLevels(p1.projectId, p1subj2.subjectId).sort() { it.level }

        then:
        copiedProjStats.name == projToCopy.name
        copiedProjStats.numSubjects == 2
        copiedProjStats.numSkills == 15
        copiedProjStats.totalPoints == (100 * 10) + (5 * 22)
        copiedProjStats.numSkillsReused == 2
        copiedProjStats.totalPointsReused == 200

        copiedSubject1.name == p1subj1.name
        copiedSubject1.subjectId == p1subj1.subjectId
        copiedSubject1.numGroups == 2
        copiedSubject1.numSkills == 10
        copiedSubject1.totalPoints == (100 * 10)
        copiedSubject1.numSkillsReused == 0
        copiedSubject1.totalPointsReused == 0

        copiedSubj1Skills.skillId == [p1Skills[0..2], group1, group2].flatten().skillId
        copiedGroup1Skills.skillId == p1Skills[3..7].skillId
        copiedGroup2Skills.skillId == p1Skills[8..9].skillId

        copiedSubject2.name == p1subj2.name
        copiedSubject2.subjectId == p1subj2.subjectId
        copiedSubject2.numGroups == 1
        copiedSubject2.numSkills == 5
        copiedSubject2.totalPoints == (22 * 5)
        copiedSubject2.numSkillsReused == 2
        copiedSubject2.totalPointsReused == 200

        copiedSubj2Skills.skillId == [[p1SkillsSubj2, group3].flatten().skillId, SkillReuseIdUtil.addTag(p1Skills[1].skillId, 0)].flatten()
        copiedGroup3Skills.skillId == [SkillReuseIdUtil.addTag(p1Skills[7].skillId, 0)]

        copiedBadge.name == badge.name
        copiedBadge.projectId == projToCopy.projectId
        copiedBadge.badgeId == badge.badgeId
        copiedBadge.totalPoints == (100 * 2) + (22 * 1)
        copiedBadge.numSkills == 3

        validateGraph(copiedDeps, [
                new Edge(from: p1Skills[0].skillId, to: p1Skills[2].skillId),
                new Edge(from: p1Skills[2].skillId, to: p1SkillsSubj2[2].skillId),
        ])

        copiedProjLevels.percent == projLevels.percent

        copiedSubj1UpdatedLevels.percent == subj1UpdatedLevels.percent
        copiedSubj2UpdatedLevels.percent == subj2UpdatedLevels.percent
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
