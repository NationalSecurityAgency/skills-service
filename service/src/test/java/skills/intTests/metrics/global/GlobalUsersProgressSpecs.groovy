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


import skills.intTests.utils.*

class GlobalUsersProgressSpecs extends DefaultIntSpec {

    def "get empty global users progress" () {
        when:
        def res = skillsService.getGlobalUserProgressMetrics()
        then:
        res.numTotalProjects == 0
        res.numTotalSkills == 0
        res.numTotalBadges == 0
        res.numTotalProjectBadges == 0
        res.numTotalGlobalBadges == 0
        res.numTotalQuizzes == 0
        res.numTotalSurveys == 0
        res.numTotalMetricItems == 0
        res.metricItemsPage == []
    }

    def "do not return disabled skills" () {
        List<SkillsService> users = getRandomUsers(2).collect { createService(it)}
        def p1 = SkillsFactory.createProject(1)
        def p1_sub1 = SkillsFactory.createSubject(1, 1)
        def p1_sks1 = SkillsFactory.createSkills(5, 1, 1, 100)
        p1_sks1[0].enabled = false
        p1_sks1[1].enabled = false
        p1_sks1[2].enabled = false
        users[0].createProjectAndSubjectAndSkills(p1, p1_sub1, p1_sks1)
        when:
        def res = users[0].getGlobalUserProgressMetrics()
        then:
        res.numTotalProjects == 1
        res.numTotalSkills == 2
        res.numTotalBadges == 0
        res.numTotalProjectBadges == 0
        res.numTotalGlobalBadges == 0
        res.numTotalQuizzes == 0
        res.numTotalSurveys == 0
    }

    def "do not return disabled badges" () {
        List<SkillsService> users = getRandomUsers(2).collect { createService(it)}
        def p1 = SkillsFactory.createProject(1)
        def p1_sub1 = SkillsFactory.createSubject(1, 1)
        def p1_sks1 = SkillsFactory.createSkills(5, 1, 1, 100)
        users[0].createProjectAndSubjectAndSkills(p1, p1_sub1, p1_sks1)

        def p1_bad1 = SkillsFactory.createBadge(1, 10)
        users[0].createBadge(p1_bad1)
        users[0].assignSkillToBadge([projectId: p1.projectId, badgeId: p1_bad1.badgeId, skillId: p1_sks1[0].skillId])

        when:
        def res = users[0].getGlobalUserProgressMetrics()
        then:
        res.numTotalProjects == 1
        res.numTotalSkills == 5
        res.numTotalBadges == 0
        res.numTotalProjectBadges == 0
        res.numTotalGlobalBadges == 0
        res.numTotalQuizzes == 0
        res.numTotalSurveys == 0
    }

    def "do not return disabled global badges" () {
        List<SkillsService> users = getRandomUsers(2).collect { createService(it)}
        def p1 = SkillsFactory.createProject(1)
        def p1_sub1 = SkillsFactory.createSubject(1, 1)
        def p1_sks1 = SkillsFactory.createSkills(5, 1, 1, 100)
        users[0].createProjectAndSubjectAndSkills(p1, p1_sub1, p1_sks1)

        def p1_bad1 = SkillsFactory.createBadge(1, 10)
        users[0].createGlobalBadge(p1_bad1)
        users[0].assignSkillToGlobalBadge([projectId: p1.projectId, badgeId: p1_bad1.badgeId, skillId: p1_sks1[0].skillId])

        when:
        def res = users[0].getGlobalUserProgressMetrics()
        then:
        res.numTotalProjects == 1
        res.numTotalSkills == 5
        res.numTotalBadges == 0
        res.numTotalProjectBadges == 0
        res.numTotalGlobalBadges == 0
        res.numTotalQuizzes == 0
        res.numTotalSurveys == 0
    }

}

