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
package skills.intTests.crossProject

import groovy.time.TimeCategory
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory

class CrossProjectClientDisplaySpec extends DefaultIntSpec {

    def "this project's and cross-project's dependency must be achieved"() {
        String user = "user1"

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(4, 1, 1)
        proj1_skills.each{
            it.pointIncrement = 25
        }

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 2)
        List<Map> proj2_skills = SkillsFactory.createSkills(4, 2, 2)
        proj2_skills.each{
            it.pointIncrement = 25
        }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)
        skillsService.createSkills(proj2_skills)

        List addedSkills = []

        when:
        // cross project dependency
        skillsService.shareSkill(proj1.projectId, proj1_skills.get(0).skillId, proj2.projectId)
        skillsService.assignDependency([projectId         : proj2.projectId, skillId: proj2_skills.get(0).skillId,
                                        dependentProjectId: proj1.projectId, dependentSkillId: proj1_skills.get(0).skillId,])

        // this project dependencies
        skillsService.assignDependency([projectId: proj2.projectId, skillId: proj2_skills.get(0).skillId, dependentSkillId: proj2_skills.get(1).skillId])

        def clientDisplay1 = skillsService.getSkillDependencyInfo(user, proj2.projectId, proj2_skills.get(0).skillId)
        addedSkills << skillsService.addSkill([projectId: proj2.projectId, skillId: proj2_skills.get(1).skillId], user, new Date())
        def clientDisplay2 = skillsService.getSkillDependencyInfo(user, proj2.projectId, proj2_skills.get(0).skillId)
        addedSkills << skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], user, new Date())
        def clientDisplay3 = skillsService.getSkillDependencyInfo(user, proj2.projectId, proj2_skills.get(0).skillId)

        then:
        addedSkills.each {
            assert it.body.skillApplied
        }

        clientDisplay1.dependencies.size() == 2

        def skill1A = clientDisplay1.dependencies.find { it.dependsOn.skillId == "skill1" && it.dependsOn.projectId == "TestProject1" }
        !skill1A.achieved
        def skill2A = clientDisplay1.dependencies.find { it.dependsOn.skillId == proj2_skills.get(1).skillId && it.dependsOn.projectId == "TestProject2" }
        !skill2A.achieved

        def skill1B = clientDisplay2.dependencies.find { it.dependsOn.skillId == "skill1" && it.dependsOn.projectId == "TestProject1" }
        !skill1B.achieved
        def skill2B = clientDisplay2.dependencies.find { it.dependsOn.skillId == proj2_skills.get(1).skillId && it.dependsOn.projectId == "TestProject2" }
        skill2B.achieved

        def skill1C = clientDisplay3.dependencies.find { it.dependsOn.skillId == "skill1" && it.dependsOn.projectId == "TestProject1" }
        skill1C.achieved
        def skill2C = clientDisplay3.dependencies.find { it.dependsOn.skillId == proj2_skills.get(1).skillId && it.dependsOn.projectId == "TestProject2" }
        skill2C.achieved
    }


    def "this projects and cross-project skills have transitive dependencies"() {
        String user = "user1"

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(4, 1, 1)
        proj1_skills.each{
            it.pointIncrement = 25
        }

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 2)
        List<Map> proj2_skills = SkillsFactory.createSkills(4, 2, 2)
        proj2_skills.each{
            it.pointIncrement = 25
        }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)
        skillsService.createSkills(proj2_skills)

        List addedSkills = []

        when:

        // other project deps
        skillsService.assignDependency([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId, dependentSkillId: proj1_skills.get(1).skillId])
        skillsService.assignDependency([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId, dependentSkillId: proj1_skills.get(2).skillId])
        skillsService.assignDependency([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId, dependentSkillId: proj1_skills.get(3).skillId])

        // cross project dependency
        skillsService.shareSkill(proj1.projectId, proj1_skills.get(0).skillId, proj2.projectId)
        skillsService.assignDependency([projectId         : proj2.projectId, skillId: proj2_skills.get(0).skillId,
                                        dependentProjectId: proj1.projectId, dependentSkillId: proj1_skills.get(0).skillId,])

        // this project dependencies
        skillsService.assignDependency([projectId: proj2.projectId, skillId: proj2_skills.get(0).skillId, dependentSkillId: proj2_skills.get(1).skillId])

        def clientDisplay1 = skillsService.getSkillDependencyInfo(user, proj2.projectId, proj2_skills.get(0).skillId)
        addedSkills << skillsService.addSkill([projectId: proj2.projectId, skillId: proj2_skills.get(1).skillId], user, new Date())
        def clientDisplay2 = skillsService.getSkillDependencyInfo(user, proj2.projectId, proj2_skills.get(0).skillId)
        addedSkills << skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(3).skillId], user, new Date())
        def clientDisplay3 = skillsService.getSkillDependencyInfo(user, proj2.projectId, proj2_skills.get(0).skillId)
        addedSkills << skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(2).skillId], user, new Date())
        addedSkills << skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId], user, new Date())
        addedSkills << skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], user, new Date())
        def clientDisplay4 = skillsService.getSkillDependencyInfo(user, proj2.projectId, proj2_skills.get(0).skillId)

        then:
        addedSkills.each {
            assert it.body.skillApplied
        }

        clientDisplay1.dependencies.size() == 2

        def skill1A = clientDisplay1.dependencies.find { it.dependsOn.skillId == "skill1" && it.dependsOn.projectId == "TestProject1" }
        !skill1A.achieved
        def skill2A = clientDisplay1.dependencies.find { it.dependsOn.skillId == proj2_skills.get(1).skillId && it.dependsOn.projectId == "TestProject2" }
        !skill2A.achieved

        def skill1B = clientDisplay2.dependencies.find { it.dependsOn.skillId == "skill1" && it.dependsOn.projectId == "TestProject1" }
        !skill1B.achieved
        def skill2B = clientDisplay2.dependencies.find { it.dependsOn.skillId == proj2_skills.get(1).skillId && it.dependsOn.projectId == "TestProject2" }
        skill2B.achieved

        def skill1C = clientDisplay3.dependencies.find { it.dependsOn.skillId == "skill1" && it.dependsOn.projectId == "TestProject1" }
        !skill1C.achieved
        def skill2C = clientDisplay3.dependencies.find { it.dependsOn.skillId == proj2_skills.get(1).skillId && it.dependsOn.projectId == "TestProject2" }
        skill2C.achieved

        def skill1D = clientDisplay4.dependencies.find { it.dependsOn.skillId == "skill1" && it.dependsOn.projectId == "TestProject1" }
        skill1D.achieved
        def skill2D = clientDisplay3.dependencies.find { it.dependsOn.skillId == proj2_skills.get(1).skillId && it.dependsOn.projectId == "TestProject2" }
        skill2D.achieved
    }

    def "load cross-project summary for a given skill"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 2)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 2)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)
        skillsService.createSkills(proj2_skills)


        when:
        skillsService.shareSkill(proj1.projectId, proj1_skills.get(1).skillId, proj2.projectId)
        skillsService.assignDependency([projectId         : proj2.projectId, skillId: proj2_skills.get(0).skillId,
                                        dependentProjectId: proj1.projectId, dependentSkillId: proj1_skills.get(1).skillId,])
        def summary = skillsService.getCrossProjectSkillSummary("user1", proj2.projectId, proj1.projectId, proj1_skills.get(1).skillId)

        then:
        summary.crossProject
        summary.projectId == proj1.projectId
        summary.projectName == proj1.name
        summary.skillId == proj1_skills.get(1).skillId
        summary.skill == proj1_skills.get(1).name
        summary.pointIncrement == proj1_skills.get(1).pointIncrement
        summary.totalPoints == proj1_skills.get(1).pointIncrement * proj1_skills.get(1).numPerformToCompletion
        summary.points == 0
        summary.todaysPoints == 0
        summary.description.description == "This skill [skill2] belongs to project [TestProject1]"
    }

    def "load cross-project summary for a given skill when shared with ALL projects"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 2)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 2)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)
        skillsService.createSkills(proj2_skills)


        when:
        skillsService.shareSkill(proj1.projectId, proj1_skills.get(1).skillId, 'ALL_SKILLS_PROJECTS')
        skillsService.assignDependency([projectId         : proj2.projectId, skillId: proj2_skills.get(0).skillId,
                                        dependentProjectId: proj1.projectId, dependentSkillId: proj1_skills.get(1).skillId,])
        def summary = skillsService.getCrossProjectSkillSummary("user1", proj2.projectId, proj1.projectId, proj1_skills.get(1).skillId)

        then:
        summary.crossProject
        summary.projectId == proj1.projectId
        summary.projectName == proj1.name
        summary.skillId == proj1_skills.get(1).skillId
        summary.skill == proj1_skills.get(1).name
        summary.pointIncrement == proj1_skills.get(1).pointIncrement
        summary.totalPoints == proj1_skills.get(1).pointIncrement * proj1_skills.get(1).numPerformToCompletion
        summary.points == 0
        summary.todaysPoints == 0
        summary.description.description == "This skill [skill2] belongs to project [TestProject1]"
    }

    def "load cross-project summary for a given skill with some point achieved"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.each {
            it.numPerformToCompletion = 5
        }

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 2)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 2)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)
        skillsService.createSkills(proj2_skills)

        String userId = "user1"
        Date yesterday = use(TimeCategory) { return 1.day.ago }
        when:
        skillsService.shareSkill(proj1.projectId, proj1_skills.get(1).skillId, proj2.projectId)
        skillsService.assignDependency([projectId         : proj2.projectId, skillId: proj2_skills.get(0).skillId,
                                        dependentProjectId: proj1.projectId, dependentSkillId: proj1_skills.get(1).skillId,])
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId], userId, new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId], userId, yesterday)

        def summary = skillsService.getCrossProjectSkillSummary(userId, proj2.projectId, proj1.projectId, proj1_skills.get(1).skillId)

        then:
        summary.crossProject
        summary.projectId == proj1.projectId
        summary.projectName == proj1.name
        summary.skillId == proj1_skills.get(1).skillId
        summary.skill == proj1_skills.get(1).name
        summary.pointIncrement == proj1_skills.get(1).pointIncrement
        summary.totalPoints == proj1_skills.get(1).pointIncrement * proj1_skills.get(1).numPerformToCompletion
        summary.points == 20
        summary.todaysPoints == 10
    }


    def "load cross-project summary for a given skill with some point achieved shared with ALL projects"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.each {
            it.numPerformToCompletion = 5
        }

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 2)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 2)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)
        skillsService.createSkills(proj2_skills)

        String userId = "user1"
        Date yesterday = use(TimeCategory) { return 1.day.ago }
        when:
        skillsService.shareSkill(proj1.projectId, proj1_skills.get(1).skillId, 'ALL_SKILLS_PROJECTS')
        skillsService.assignDependency([projectId         : proj2.projectId, skillId: proj2_skills.get(0).skillId,
                                        dependentProjectId: proj1.projectId, dependentSkillId: proj1_skills.get(1).skillId,])
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId], userId, new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId], userId, yesterday)

        def summary = skillsService.getCrossProjectSkillSummary(userId, proj2.projectId, proj1.projectId, proj1_skills.get(1).skillId)

        then:
        summary.crossProject
        summary.projectId == proj1.projectId
        summary.projectName == proj1.name
        summary.skillId == proj1_skills.get(1).skillId
        summary.skill == proj1_skills.get(1).name
        summary.pointIncrement == proj1_skills.get(1).pointIncrement
        summary.totalPoints == proj1_skills.get(1).pointIncrement * proj1_skills.get(1).numPerformToCompletion
        summary.points == 20
        summary.todaysPoints == 10
    }


    def "not allowed to load summaries for non-shared skill from other projects"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.each {
            it.numPerformToCompletion = 5
        }

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 2)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 2)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)
        skillsService.createSkills(proj2_skills)

        String userId = "user1"
        Date yesterday = use(TimeCategory) { return 1.day.ago }
        when:
        skillsService.shareSkill(proj1.projectId, proj1_skills.get(1).skillId, proj2.projectId)
        skillsService.assignDependency([projectId         : proj2.projectId, skillId: proj2_skills.get(0).skillId,
                                        dependentProjectId: proj1.projectId, dependentSkillId: proj1_skills.get(1).skillId,])

        skillsService.getCrossProjectSkillSummary(userId, proj2.projectId, proj1.projectId, proj1_skills.get(0).skillId)

        then:
        thrown(SkillsClientException)
    }

    def "do not show follow-on dependency info for cross-project skill"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 2)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 2)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)
        skillsService.createSkills(proj2_skills)

        when:
        skillsService.shareSkill(proj1.projectId, proj1_skills.get(1).skillId, proj2.projectId)
        skillsService.assignDependency([projectId         : proj2.projectId, skillId: proj2_skills.get(0).skillId,
                                        dependentProjectId: proj1.projectId, dependentSkillId: proj1_skills.get(1).skillId,])
        skillsService.assignDependency([projectId         : proj1.projectId, skillId: proj1_skills.get(1).skillId,
                                        dependentSkillId: proj1_skills.get(0).skillId,])
        def summary = skillsService.getCrossProjectSkillSummary("user1", proj2.projectId, proj1.projectId, proj1_skills.get(1).skillId)

        then:
        summary.crossProject
        summary.projectId == proj1.projectId
        summary.skillId == proj1_skills.get(1).skillId
        !summary.dependencyInfo
    }


}
