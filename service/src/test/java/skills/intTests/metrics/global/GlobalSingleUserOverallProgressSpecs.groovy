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
package skills.intTests.metrics.global

import groovy.transform.Canonical
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService

class GlobalSingleUserOverallProgressSpecs extends DefaultIntSpec {
    List<SkillsService> admins
    List<SkillsService> users
    List<Map> p1Skills
    List<Map> p2Skills
    List<Map> p3Skills

    def setup() {
        List<String> userNames = getRandomUsers(13)
        admins = userNames[0..2].collect { createService(it) }
        users = userNames[3..12].collect { createService(it) }

        // create projects and skills
        p1Skills = createProject(1, 5, 6, admins[0])
        p2Skills = createProject(2, 4, 3, admins[0])
        p3Skills = createProject(3, 6, 4, admins[0])

        admins[0].addProjectAdmin(p2Skills[1].projectId, admins[1].userName)
        admins[0].addProjectAdmin(p3Skills[1].projectId, admins[1].userName)

        admins[0].addProjectAdmin(p3Skills[1].projectId, admins[2].userName)

        def p1_badge1 = createBadge(1, 10, [p1Skills[2]], admins[0])
        def p1_badge2 = createBadge(1, 11, [p1Skills[6]], admins[0])
        def p2_badge1 = createBadge(2, 12, [p2Skills[2]], admins[0])

        // report skills
        assert users[0].addSkill(p1Skills[0]).body.skillApplied
        assert users[0].addSkill(p1Skills[1]).body.skillApplied
        assert users[0].addSkill(p1Skills[2]).body.skillApplied
        assert users[0].addSkill(p1Skills[3]).body.skillApplied

        assert users[1].addSkill(p1Skills[0]).body.skillApplied

        assert users[2].addSkill(p1Skills[0]).body.skillApplied
        assert users[2].addSkill(p2Skills[0]).body.skillApplied

        assert users[3].addSkill(p1Skills[0]).body.skillApplied
        assert users[3].addSkill(p1Skills[1]).body.skillApplied
        assert users[3].addSkill(p2Skills[0]).body.skillApplied
        assert users[3].addSkill(p2Skills[1]).body.skillApplied

        assert users[4].addSkill(p1Skills[0]).body.skillApplied
        assert users[4].addSkill(p1Skills[1]).body.skillApplied
        assert users[4].addSkill(p1Skills[2]).body.skillApplied
        assert users[4].addSkill(p1Skills[3]).body.skillApplied
        assert users[4].addSkill(p2Skills[0]).body.skillApplied
        assert users[4].addSkill(p2Skills[1]).body.skillApplied
        assert users[4].addSkill(p2Skills[2]).body.skillApplied
        assert users[4].addSkill(p2Skills[3]).body.skillApplied
    }

    def "get single user progress - user with achievements across multiple projects"() {
        when:
        def res = admins[0].getGlobalSingleUserProgressMetrics(users[4].userName)
        def res_viaAdmin2 = admins[1].getGlobalSingleUserProgressMetrics(users[4].userName)
        def res_viaAdmin3 = admins[2].getGlobalSingleUserProgressMetrics(users[4].userName)
        then:
        res.projectsProgress.size() == 2

        assertMetric(res.projectsProgress[0], new Metric(
                projectId: "TestProject1", projectName: "Test Project#1",
                numSkills: 11, projectTotalPoints: 1100, numProjectLevels: 5, numBadges: 2, points: 400,
                numAchievedSkills: 4, numAchievedBadges: 1, achievedProjLevel: 2
        ))

        assertMetric(res.projectsProgress[1], new Metric(
                projectId: "TestProject2", projectName: "Test Project#2",
                numSkills: 7, projectTotalPoints: 700, numProjectLevels: 5, numBadges: 1, points: 400,
                numAchievedSkills: 4, numAchievedBadges: 1, achievedProjLevel: 3
        ))

        res_viaAdmin2.projectsProgress.size() == 1
        assertMetric(res_viaAdmin2.projectsProgress[0], new Metric(
                projectId: "TestProject2", projectName: "Test Project#2",
                numSkills: 7, projectTotalPoints: 700, numProjectLevels: 5, numBadges: 1, points: 400,
                numAchievedSkills: 4, numAchievedBadges: 1, achievedProjLevel: 3
        ))

        res_viaAdmin3.projectsProgress == []
    }

    def "get single user progress - user with single project"() {
        when:
        def res = admins[0].getGlobalSingleUserProgressMetrics(users[1].userName)
        then:
        res.projectsProgress.size() == 1
        
        assertMetric(res.projectsProgress[0], new Metric(
            projectId: "TestProject1",
            projectName: "Test Project#1",
            numSkills: 11,
            projectTotalPoints: 1100,
            numProjectLevels: 5,
            numBadges: 2,
            points: 100,
            numAchievedSkills: 1,
            numAchievedBadges: 0,
            achievedProjLevel: 0
        ))
    }

    def "get single user progress - user with no achievements"() {
        when:
        def res = admins[0].getGlobalSingleUserProgressMetrics(users[9].userName)
        def res1 = admins[0].getGlobalSingleUserProgressMetrics("nonExistentUser")
        then:
        res.projectsProgress.size() == 0
        res1.projectsProgress.size() == 0
    }

    def "get single user progress - validate timestamps"() {
        when:
        def res = admins[0].getGlobalSingleUserProgressMetrics(users[4].userName)
        then:
        res.projectsProgress.each { project ->
            assert project.updated != null
            assert project.updated instanceof String
            // Validate that the timestamp can be parsed as a valid ISO datetime
            Date.parse("yyyy-MM-dd'T'HH:mm:ss", project.updated)
        }
    }

    // helper methods
    private createProject(int projNum, int numSkillsForFirstSubj = 5, int numSkillsSecondSubj = 0, SkillsService serviceToUse = null) {
        serviceToUse = serviceToUse ?: skillsService
        List skillsRes = []
        def p1 = SkillsFactory.createProject(projNum)
        def p1_sub1 = SkillsFactory.createSubject(projNum, 1)
        def p1_sks1 = SkillsFactory.createSkills(numSkillsForFirstSubj, projNum, 1, 100)
        skillsRes.addAll(p1_sks1)
        serviceToUse.createProjectAndSubjectAndSkills(p1, p1_sub1, p1_sks1)
        if (numSkillsSecondSubj > 0) {
            def p1_sub2 = SkillsFactory.createSubject(projNum, 2)
            def p1_sks2 = SkillsFactory.createSkills(numSkillsSecondSubj, projNum, 2, 100)
            skillsRes.addAll(p1_sks2)
            serviceToUse.createProjectAndSubjectAndSkills(null, p1_sub2, p1_sks2)
        }
        return skillsRes
    }

    private createBadge(int projNum, int badgeNum, List skills, SkillsService serviceToUse = null) {
        serviceToUse = serviceToUse ?: skillsService
        def badgeREs = SkillsFactory.createBadge(projNum, badgeNum)
        serviceToUse.createBadge(badgeREs)
        skills.each {
            serviceToUse.assignSkillToBadge(it.projectId, badgeREs.badgeId, it.skillId)
        }
        badgeREs.enabled = true
        serviceToUse.updateBadge(badgeREs)

        return badgeREs
    }

    @Canonical
    private static class Metric {
        String projectId
        String projectName
        Integer numSkills
        Integer projectTotalPoints
        Integer numProjectLevels
        Integer numBadges
        Integer points

        Integer numAchievedSkills
        Integer numAchievedBadges
        Integer achievedProjLevel
    }
    private void assertMetric(def row, Metric metric) {
        assert row.projectId == metric.projectId
        assert row.projectName == metric.projectName
        assert row.numSkills == metric.numSkills
        assert row.projectTotalPoints == metric.projectTotalPoints
        assert row.numProjectLevels == metric.numProjectLevels
        assert row.numBadges == metric.numBadges
        assert row.points == metric.points
        assert row.numAchievedSkills == metric.numAchievedSkills
        assert row.numAchievedBadges == metric.numAchievedBadges
        assert row.achievedProjLevel == metric.achievedProjLevel
    }
}