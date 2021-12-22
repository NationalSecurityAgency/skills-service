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

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import spock.lang.IgnoreRest

class CrossProjectDepsAndAchievementsSpec extends DefaultIntSpec {

    def "cross-project dependency must be achieved first"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.each{
            it.pointIncrement = 40
        }

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 2)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 2)
        proj2_skills.each{
            it.pointIncrement = 50
        }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)
        skillsService.createSkills(proj2_skills)

        skillsService.shareSkill(proj1.projectId, proj1_skills.get(0).skillId, proj2.projectId)

        when:
        skillsService.assignDependency([projectId         : proj2.projectId, skillId: proj2_skills.get(0).skillId,
                                        dependentProjectId: proj1.projectId, dependentSkillId: proj1_skills.get(0).skillId,])
        def res1 = skillsService.addSkill([projectId: proj2.projectId, skillId: proj2_skills.get(0).skillId])
        def res2 = skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId]) // achieve non-dependent skill
        def res3 = skillsService.addSkill([projectId: proj2.projectId, skillId: proj2_skills.get(0).skillId])
        def res4 = skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId])
        def res5 = skillsService.addSkill([projectId: proj2.projectId, skillId: proj2_skills.get(0).skillId])
        then:
        !res1.body.skillApplied
        res1.body.explanation == "Not all dependent skills have been achieved. Missing achievements for 1 out of 1. Waiting on completion of [TestProject1:skill1]."

        res2.body.skillApplied
        res2.body.explanation == "Skill event was applied"

        !res3.body.skillApplied
        res3.body.explanation == "Not all dependent skills have been achieved. Missing achievements for 1 out of 1. Waiting on completion of [TestProject1:skill1]."

        res4.body.skillApplied
        res5.body.explanation == "Skill event was applied"

        res5.body.skillApplied
        res5.body.explanation == "Skill event was applied"
    }


    def "transitive cross-project dependency must be achieved first"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(4, 1, 1)
        proj1_skills.each{
            it.pointIncrement = 25
        }

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 2)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 2)
        proj2_skills.each{
            it.pointIncrement = 50
        }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)
        skillsService.createSkills(proj2_skills)

        when:
        // project 1 internal dependencies
        skillsService.assignDependency([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId, dependentSkillId: proj1_skills.get(1).skillId])
        skillsService.assignDependency([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId, dependentSkillId: proj1_skills.get(2).skillId])
        skillsService.assignDependency([projectId: proj1.projectId, skillId: proj1_skills.get(2).skillId, dependentSkillId: proj1_skills.get(3).skillId])

        // cross project dependency
        skillsService.shareSkill(proj1.projectId, proj1_skills.get(0).skillId, proj2.projectId)
        skillsService.assignDependency([projectId         : proj2.projectId, skillId: proj2_skills.get(0).skillId,
                                        dependentProjectId: proj1.projectId, dependentSkillId: proj1_skills.get(0).skillId,])


        def res1 = skillsService.addSkill([projectId: proj2.projectId, skillId: proj2_skills.get(0).skillId])
        def res2 = skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(3).skillId])
        def res1A = skillsService.addSkill([projectId: proj2.projectId, skillId: proj2_skills.get(0).skillId])

        def res3 = skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(2).skillId])
        def res1B = skillsService.addSkill([projectId: proj2.projectId, skillId: proj2_skills.get(0).skillId])

        def res4 = skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId])
        def res1C = skillsService.addSkill([projectId: proj2.projectId, skillId: proj2_skills.get(0).skillId])

        def res5 = skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId])
        def res1D = skillsService.addSkill([projectId: proj2.projectId, skillId: proj2_skills.get(0).skillId])

        then:
        !res1.body.skillApplied
        res1.body.explanation == "Not all dependent skills have been achieved. Missing achievements for 1 out of 1. Waiting on completion of [TestProject1:skill1]."

        res2.body.skillApplied
        res2.body.explanation == "Skill event was applied"

        !res1A.body.skillApplied
        res1A.body.explanation == "Not all dependent skills have been achieved. Missing achievements for 1 out of 1. Waiting on completion of [TestProject1:skill1]."

        res3.body.skillApplied
        res3.body.explanation == "Skill event was applied"

        !res1B.body.skillApplied
        res1B.body.explanation == "Not all dependent skills have been achieved. Missing achievements for 1 out of 1. Waiting on completion of [TestProject1:skill1]."

        res4.body.skillApplied
        res4.body.explanation == "Skill event was applied"

        !res1C.body.skillApplied
        res1C.body.explanation == "Not all dependent skills have been achieved. Missing achievements for 1 out of 1. Waiting on completion of [TestProject1:skill1]."

        res5.body.skillApplied
        res5.body.explanation == "Skill event was applied"

        res1D.body.skillApplied
        res1D.body.explanation == "Skill event was applied"
    }

    def "this projects then transitive cross-project dependency must be achieved first"() {
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
            it.pointIncrement =  25
        }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)
        skillsService.createSkills(proj2_skills)

        when:
        // project 1 internal dependencies
        skillsService.assignDependency([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId, dependentSkillId: proj1_skills.get(1).skillId])
        skillsService.assignDependency([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId, dependentSkillId: proj1_skills.get(2).skillId])
        skillsService.assignDependency([projectId: proj1.projectId, skillId: proj1_skills.get(2).skillId, dependentSkillId: proj1_skills.get(3).skillId])

        // cross project dependency
        skillsService.shareSkill(proj1.projectId, proj1_skills.get(0).skillId, proj2.projectId)
        skillsService.assignDependency([projectId         : proj2.projectId, skillId: proj2_skills.get(0).skillId,
                                        dependentProjectId: proj1.projectId, dependentSkillId: proj1_skills.get(0).skillId,])

        // this project dependencies
        skillsService.assignDependency([projectId: proj2.projectId, skillId: proj2_skills.get(0).skillId, dependentSkillId: proj2_skills.get(1).skillId])


        def res1 = skillsService.addSkill([projectId: proj2.projectId, skillId: proj2_skills.get(0).skillId])

        def res2 = skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(3).skillId])
        def res1A = skillsService.addSkill([projectId: proj2.projectId, skillId: proj2_skills.get(0).skillId])

        def res3 = skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(2).skillId])
        def res1B = skillsService.addSkill([projectId: proj2.projectId, skillId: proj2_skills.get(0).skillId])

        def res4 = skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId])
        def res1C = skillsService.addSkill([projectId: proj2.projectId, skillId: proj2_skills.get(0).skillId])

        def res5 = skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId])
        def res1D = skillsService.addSkill([projectId: proj2.projectId, skillId: proj2_skills.get(0).skillId])

        def res6 = skillsService.addSkill([projectId: proj2.projectId, skillId: proj2_skills.get(1).skillId])
        def res1E = skillsService.addSkill([projectId: proj2.projectId, skillId: proj2_skills.get(0).skillId])

        then:
        !res1.body.skillApplied
        res1.body.explanation == "Not all dependent skills have been achieved. Missing achievements for 2 out of 2. Waiting on completion of [TestProject1:skill1, TestProject2:skill2subj2]."

        res2.body.skillApplied
        res2.body.explanation == "Skill event was applied"

        !res1A.body.skillApplied
        res1A.body.explanation == "Not all dependent skills have been achieved. Missing achievements for 2 out of 2. Waiting on completion of [TestProject1:skill1, TestProject2:skill2subj2]."

        res3.body.skillApplied
        res3.body.explanation == "Skill event was applied"

        !res1B.body.skillApplied
        res1B.body.explanation == "Not all dependent skills have been achieved. Missing achievements for 2 out of 2. Waiting on completion of [TestProject1:skill1, TestProject2:skill2subj2]."

        res4.body.skillApplied
        res4.body.explanation == "Skill event was applied"

        !res1C.body.skillApplied
        res1C.body.explanation == "Not all dependent skills have been achieved. Missing achievements for 2 out of 2. Waiting on completion of [TestProject1:skill1, TestProject2:skill2subj2]."

        res5.body.skillApplied
        res5.body.explanation == "Skill event was applied"

        !res1D.body.skillApplied
        res1D.body.explanation == "Not all dependent skills have been achieved. Missing achievements for 1 out of 2. Waiting on completion of [TestProject2:skill2subj2]."

        res6.body.skillApplied
        res6.body.explanation == "Skill event was applied"

        res1E.body.skillApplied
        res1E.body.explanation == "Skill event was applied"
    }

    def "remove cross-project dependency"() {
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

        skillsService.shareSkill(proj2.projectId, proj2_skills.get(0).skillId, proj1.projectId)

        skillsService.assignDependency([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId, dependentSkillId: proj1_skills.get(1).skillId])

        Map crossProjectDep = [projectId         : proj1.projectId, skillId: proj1_skills.get(0).skillId,
                               dependentProjectId: proj2.projectId, dependentSkillId: proj2_skills.get(0).skillId,]
        skillsService.assignDependency(crossProjectDep)

        when:
        def graph = skillsService.getDependencyGraph(proj1.projectId)
        skillsService.removeDependency(crossProjectDep)
        def graphAfterRemoval = skillsService.getDependencyGraph(proj1.projectId)

        then:
        graph.nodes.collect { it.skillId }.sort() == [proj1_skills.get(0).skillId, proj1_skills.get(1).skillId, proj2_skills.get(0).skillId,].sort()

        graph.edges.size() == 2
        graph.edges.collect { it.toId }.sort() == [
                graph.nodes.find{ it.skillId == proj1_skills.get(1).skillId}.id,
                graph.nodes.find{ it.skillId == proj2_skills.get(0).skillId}.id
        ].sort()

        graphAfterRemoval.nodes.collect { it.skillId }.sort() == [proj1_skills.get(0).skillId, proj1_skills.get(1).skillId].sort()
        graphAfterRemoval.edges.size() == 1
        graphAfterRemoval.edges.collect { it.toId }.sort() == [
                graphAfterRemoval.nodes.find{ it.skillId == proj1_skills.get(1).skillId}.id,
        ]
    }

}
