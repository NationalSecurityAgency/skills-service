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
package skills.intTests.dependentSkills

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsFactory
import skills.storage.model.SkillDef

import static skills.intTests.utils.SkillsFactory.*

class ReportSkills_DependentSkillsSpecs extends DefaultIntSpec {

    List<String> sampleUserIds // loaded from system props

    def setup(){
        skillsService.deleteProjectIfExist(SkillsFactory.defaultProjId)
        sampleUserIds = System.getProperty("sampleUserIds", "tom|||dick|||harry")?.split("\\|\\|\\|").sort()
    }

    def "do not give credit if dependency was not fulfilled: learning path of skill -> skill"(){
        List<Map> skills = SkillsFactory.createSkills(2)

        when:
        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkills(skills)

        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(1).skillId, skills.get(0).skillId)

        def res = skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(1).skillId])

        then:
        !res.body.skillApplied
        res.body.explanation == "Not all dependent skills have been achieved. Missing achievements for 1 out of 1. Waiting on completion of [TestProject1:skill1]."
    }

    def "do not give credit if only some dependencies were fulfilled  [skill0, skill1, skill2] -> skill3 "(){
        List<Map> skills = SkillsFactory.createSkills(4)
        skills.each{
            it.pointIncrement = 25
        }

        when:
        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkills(skills)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(3).skillId, skills.get(0).skillId)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(3).skillId, skills.get(1).skillId)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(3).skillId, skills.get(2).skillId)

        def resSkill1 = skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId])
        def resSkill3 = skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(2).skillId])
        def resSkill4 = skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(3).skillId])

        then:
        resSkill1.body.skillApplied
        resSkill3.body.skillApplied

        !resSkill4.body.skillApplied
        resSkill4.body.explanation == "Not all dependent skills have been achieved. Missing achievements for 1 out of 3. Waiting on completion of [TestProject1:skill2]."
    }

    def "do not give credit if dependency was not fulfilled: learning path of skill -> badge"(){
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(10, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge1 = SkillsFactory.createBadge(1, 1)
        skillsService.createBadge(badge1)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[0].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[1].skillId])
        badge1.enabled = true
        skillsService.createBadge(badge1)

        // skill2 -> skill3 -> badge1 -> skill4
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[3].skillId, p1Skills[2].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge1.badgeId, p1Skills[3].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[4].skillId, badge1.badgeId)

        when:
        def res = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[0].skillId])
        then:
        !res.body.skillApplied
        res.body.explanation == "Not all dependent skills have been achieved. Missing achievements for 1 out of 1. Waiting on completion of [TestProject1:${p1Skills[3].skillId}]."
    }

    def "do not give credit if dependency was only partially fulfilled: learning path of [skill2, skill3, skill4] -> badge"(){
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(10, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge1 = SkillsFactory.createBadge(1, 1)
        skillsService.createBadge(badge1)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[0].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[1].skillId])
        badge1.enabled = true
        skillsService.createBadge(badge1)

        skillsService.addLearningPathPrerequisite(p1.projectId, badge1.badgeId, p1Skills[2].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge1.badgeId, p1Skills[3].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge1.badgeId, p1Skills[4].skillId)


        when:
        def res1 = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[3].skillId])
        def res2 = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[0].skillId])
        then:
        res1.body.skillApplied
        !res2.body.skillApplied
        res2.body.explanation == "Not all dependent skills have been achieved. Missing achievements for 2 out of 3. Waiting on completion of [TestProject1:${p1Skills[2].skillId}, TestProject1:${p1Skills[4].skillId}]."
    }

    def "give credit if dependency are fulfilled: learning path of [skill2, skill3, skill4] -> badge"(){
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(10, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge1 = SkillsFactory.createBadge(1, 1)
        skillsService.createBadge(badge1)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[0].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[1].skillId])
        badge1.enabled = true
        skillsService.createBadge(badge1)

        skillsService.addLearningPathPrerequisite(p1.projectId, badge1.badgeId, p1Skills[2].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge1.badgeId, p1Skills[3].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge1.badgeId, p1Skills[4].skillId)

        when:
        def res1 = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[2].skillId])
        def res2 = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[3].skillId])
        def res3 = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[4].skillId])
        def res4 = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[0].skillId])
        then:
        res1.body.skillApplied
        res2.body.skillApplied
        res3.body.skillApplied
        res4.body.skillApplied
    }

    def "do not give credit if dependency was not fulfilled: learning path of badge -> skill"(){
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(10, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge1 = SkillsFactory.createBadge(1, 1)
        skillsService.createBadge(badge1)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[0].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[1].skillId])
        badge1.enabled = true
        skillsService.createBadge(badge1)

        // skill2 -> skill3 -> badge1 -> skill4
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[3].skillId, p1Skills[2].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge1.badgeId, p1Skills[3].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[4].skillId, badge1.badgeId)

        when:
        def res = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[4].skillId])
        then:
        !res.body.skillApplied
        res.body.explanation == "Not all dependent skills have been achieved. Missing achievements for 1 out of 1. Waiting on completion of [TestProject1:${badge1.badgeId}]."
    }

    def "do not give credit if dependency was not fulfilled: learning path of [badge1, badge2] -> skill"(){
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(10, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge1 = SkillsFactory.createBadge(1, 1)
        skillsService.createBadge(badge1)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[0].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[1].skillId])
        badge1.enabled = true
        skillsService.createBadge(badge1)

        def badge2 = SkillsFactory.createBadge(1, 2)
        skillsService.createBadge(badge2)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[8].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[9].skillId])
        badge2.enabled = true
        skillsService.createBadge(badge2)

        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[3].skillId, p1Skills[2].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge1.badgeId, p1Skills[3].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[4].skillId, badge1.badgeId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[4].skillId, badge2.badgeId)

        when:
        def res = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[4].skillId])
        then:
        !res.body.skillApplied
        res.body.explanation == "Not all dependent skills have been achieved. Missing achievements for 2 out of 2. Waiting on completion of [TestProject1:${badge1.badgeId}, TestProject1:${badge2.badgeId}]."
    }

    def "do not give credit if dependency was not fulfilled: learning path of [badge1, badge2, skill] -> skill"(){
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(10, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge1 = SkillsFactory.createBadge(1, 1)
        skillsService.createBadge(badge1)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[0].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[1].skillId])
        badge1.enabled = true
        skillsService.createBadge(badge1)

        def badge2 = SkillsFactory.createBadge(1, 2)
        skillsService.createBadge(badge2)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[2].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[3].skillId])
        badge2.enabled = true
        skillsService.createBadge(badge2)

        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[5].skillId, p1Skills[4].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge1.badgeId, p1Skills[5].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[6].skillId, badge1.badgeId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[6].skillId, badge2.badgeId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[6].skillId, p1Skills[7].skillId)

        when:
        def res = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[6].skillId])
        then:
        !res.body.skillApplied
        res.body.explanation == "Not all dependent skills have been achieved. Missing achievements for 3 out of 3. Waiting on completion of [TestProject1:${badge1.badgeId}, TestProject1:${badge2.badgeId}, TestProject1:${p1Skills[7].skillId}]."
    }

    def "do not give credit if dependency was only partially fulfilled: learning path of [badge1-done, badge2, skill] -> skill"(){
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(10, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge1 = SkillsFactory.createBadge(1, 1)
        skillsService.createBadge(badge1)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[0].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[1].skillId])
        badge1.enabled = true
        skillsService.createBadge(badge1)

        def badge2 = SkillsFactory.createBadge(1, 2)
        skillsService.createBadge(badge2)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[2].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[3].skillId])
        badge2.enabled = true
        skillsService.createBadge(badge2)

        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[5].skillId, p1Skills[4].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge1.badgeId, p1Skills[5].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[6].skillId, badge1.badgeId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[6].skillId, badge2.badgeId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[6].skillId, p1Skills[7].skillId)

        when:
        def res1 = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[4].skillId])
        def res2 = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[5].skillId])
        def res3 = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[0].skillId])
        def res4 = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[1].skillId])
        def res5 = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[6].skillId])
        then:
        res1.body.skillApplied
        res2.body.skillApplied
        res3.body.skillApplied
        res4.body.skillApplied
        res5.body.explanation == "Not all dependent skills have been achieved. Missing achievements for 2 out of 3. Waiting on completion of [TestProject1:${badge2.badgeId}, TestProject1:${p1Skills[7].skillId}]."
    }

    def "do not give credit if dependency was only partially fulfilled: learning path of [badge1-done, badge2, skill-done] -> skill"(){
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(10, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge1 = SkillsFactory.createBadge(1, 1)
        skillsService.createBadge(badge1)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[0].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[1].skillId])
        badge1.enabled = true
        skillsService.createBadge(badge1)

        def badge2 = SkillsFactory.createBadge(1, 2)
        skillsService.createBadge(badge2)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[2].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[3].skillId])
        badge2.enabled = true
        skillsService.createBadge(badge2)

        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[5].skillId, p1Skills[4].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge1.badgeId, p1Skills[5].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[6].skillId, badge1.badgeId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[6].skillId, badge2.badgeId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[6].skillId, p1Skills[7].skillId)

        when:
        def res1 = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[4].skillId])
        def res2 = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[5].skillId])
        def res3 = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[0].skillId])
        def res4 = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[1].skillId])
        def res5 = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[7].skillId])
        def res6 = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[6].skillId])
        then:
        res1.body.skillApplied
        res2.body.skillApplied
        res3.body.skillApplied
        res4.body.skillApplied
        res5.body.skillApplied
        !res6.body.skillApplied
        res6.body.explanation == "Not all dependent skills have been achieved. Missing achievements for 1 out of 3. Waiting on completion of [TestProject1:${badge2.badgeId}]."
    }

    def "give credit if dependency was fulfilled: learning path of [badge1-done, badge2-done, skill-done] -> skill"(){
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(10, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge1 = SkillsFactory.createBadge(1, 1)
        skillsService.createBadge(badge1)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[0].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[1].skillId])
        badge1.enabled = true
        skillsService.createBadge(badge1)

        def badge2 = SkillsFactory.createBadge(1, 2)
        skillsService.createBadge(badge2)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[2].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[3].skillId])
        badge2.enabled = true
        skillsService.createBadge(badge2)

        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[5].skillId, p1Skills[4].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge1.badgeId, p1Skills[5].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[6].skillId, badge1.badgeId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[6].skillId, badge2.badgeId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[6].skillId, p1Skills[7].skillId)

        when:
        def res1 = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[4].skillId])
        def res2 = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[5].skillId])
        def res3 = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[0].skillId])
        def res4 = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[1].skillId])
        def res5 = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[7].skillId])
        def res6 = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[2].skillId])
        def res7 = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[3].skillId])

        def res8 = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[6].skillId])
        then:
        res1.body.skillApplied
        res2.body.skillApplied
        res3.body.skillApplied
        res4.body.skillApplied
        res5.body.skillApplied
        res6.body.skillApplied
        res7.body.skillApplied
    }

    def "do not give credit if dependency was not fulfilled: learning path of badge -> badge"(){
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(10, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge1 = SkillsFactory.createBadge(1, 1)
        skillsService.createBadge(badge1)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[0].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[1].skillId])
        badge1.enabled = true
        skillsService.createBadge(badge1)

        def badge2 = SkillsFactory.createBadge(1, 2)
        skillsService.createBadge(badge2)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[2].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[3].skillId])
        badge2.enabled = true
        skillsService.createBadge(badge2)

        // skill4 -> skill5 -> badge1 -> badge2
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[5].skillId, p1Skills[4].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge1.badgeId, p1Skills[5].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge2.badgeId, badge1.badgeId)

        when:
        def res = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[3].skillId])
        then:
        !res.body.skillApplied
        res.body.explanation == "Not all dependent skills have been achieved. Missing achievements for 1 out of 1. Waiting on completion of [TestProject1:${badge1.badgeId}]."
    }

    def "do not give credit if dependency was not fulfilled: learning path of [badge1, badge2] -> badge3"(){
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(10, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge1 = SkillsFactory.createBadge(1, 1)
        skillsService.createBadge(badge1)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[0].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[1].skillId])
        badge1.enabled = true
        skillsService.createBadge(badge1)

        def badge2 = SkillsFactory.createBadge(1, 2)
        skillsService.createBadge(badge2)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[2].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[3].skillId])
        badge2.enabled = true
        skillsService.createBadge(badge2)

        def badge3 = SkillsFactory.createBadge(1, 3)
        skillsService.createBadge(badge3)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge3.badgeId, skillId: p1Skills[4].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge3.badgeId, skillId: p1Skills[5].skillId])
        badge3.enabled = true
        skillsService.createBadge(badge3)

        // skill4 -> skill5 -> badge1 -> badge2
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[7].skillId, p1Skills[6].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge1.badgeId, p1Skills[7].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge3.badgeId, badge1.badgeId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge3.badgeId, badge2.badgeId)

        when:
        def res = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[4].skillId])
        then:
        !res.body.skillApplied
        res.body.explanation == "Not all dependent skills have been achieved. Missing achievements for 2 out of 2. Waiting on completion of [TestProject1:${badge1.badgeId}, TestProject1:${badge2.badgeId}]."
    }

    def "do not give credit if dependency was not fulfilled: learning path of [badge1, badge2, skill] -> badge3"(){
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(10, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge1 = SkillsFactory.createBadge(1, 1)
        skillsService.createBadge(badge1)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[0].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[1].skillId])
        badge1.enabled = true
        skillsService.createBadge(badge1)

        def badge2 = SkillsFactory.createBadge(1, 2)
        skillsService.createBadge(badge2)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[2].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[3].skillId])
        badge2.enabled = true
        skillsService.createBadge(badge2)

        def badge3 = SkillsFactory.createBadge(1, 3)
        skillsService.createBadge(badge3)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge3.badgeId, skillId: p1Skills[4].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge3.badgeId, skillId: p1Skills[5].skillId])
        badge3.enabled = true
        skillsService.createBadge(badge3)

        // skill4 -> skill5 -> badge1 -> badge2
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[7].skillId, p1Skills[6].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge1.badgeId, p1Skills[7].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge3.badgeId, badge1.badgeId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge3.badgeId, badge2.badgeId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge3.badgeId, p1Skills[9].skillId)

        when:
        def res = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[4].skillId])
        then:
        !res.body.skillApplied
        res.body.explanation == "Not all dependent skills have been achieved. Missing achievements for 3 out of 3. Waiting on completion of [TestProject1:${badge1.badgeId}, TestProject1:${badge2.badgeId}, TestProject1:${p1Skills[9].skillId}]."
    }

    def "do not give credit if dependency was partially fulfilled: learning path of [badge1-done, badge2, skill] -> badge3"(){
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(10, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge1 = SkillsFactory.createBadge(1, 1)
        skillsService.createBadge(badge1)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[0].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[1].skillId])
        badge1.enabled = true
        skillsService.createBadge(badge1)

        def badge2 = SkillsFactory.createBadge(1, 2)
        skillsService.createBadge(badge2)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[2].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[3].skillId])
        badge2.enabled = true
        skillsService.createBadge(badge2)

        def badge3 = SkillsFactory.createBadge(1, 3)
        skillsService.createBadge(badge3)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge3.badgeId, skillId: p1Skills[4].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge3.badgeId, skillId: p1Skills[5].skillId])
        badge3.enabled = true
        skillsService.createBadge(badge3)

        // skill4 -> skill5 -> badge1 -> badge2
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[7].skillId, p1Skills[6].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge1.badgeId, p1Skills[7].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge3.badgeId, badge1.badgeId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge3.badgeId, badge2.badgeId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge3.badgeId, p1Skills[9].skillId)

        when:
        def res1 = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[6].skillId])
        def res2 = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[7].skillId])
        def res3 = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[0].skillId])
        def res4 = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[1].skillId])

        def resLast = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[4].skillId])
        then:
        res1.body.skillApplied
        res2.body.skillApplied
        res3.body.skillApplied
        res4.body.skillApplied

        !resLast.body.skillApplied
        resLast.body.explanation == "Not all dependent skills have been achieved. Missing achievements for 2 out of 3. Waiting on completion of [TestProject1:${badge2.badgeId}, TestProject1:${p1Skills[9].skillId}]."
    }

    def "do not give credit if dependency was partially fulfilled: learning path of [badge1-done, badge2, skill-done] -> badge3"(){
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(10, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge1 = SkillsFactory.createBadge(1, 1)
        skillsService.createBadge(badge1)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[0].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[1].skillId])
        badge1.enabled = true
        skillsService.createBadge(badge1)

        def badge2 = SkillsFactory.createBadge(1, 2)
        skillsService.createBadge(badge2)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[2].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[3].skillId])
        badge2.enabled = true
        skillsService.createBadge(badge2)

        def badge3 = SkillsFactory.createBadge(1, 3)
        skillsService.createBadge(badge3)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge3.badgeId, skillId: p1Skills[4].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge3.badgeId, skillId: p1Skills[5].skillId])
        badge3.enabled = true
        skillsService.createBadge(badge3)

        // skill4 -> skill5 -> badge1 -> badge2
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[7].skillId, p1Skills[6].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge1.badgeId, p1Skills[7].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge3.badgeId, badge1.badgeId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge3.badgeId, badge2.badgeId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge3.badgeId, p1Skills[9].skillId)

        when:
        def res1 = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[6].skillId])
        def res2 = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[7].skillId])
        def res3 = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[0].skillId])
        def res4 = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[1].skillId])
        def res5 = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[9].skillId])

        def resLast = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[4].skillId])
        then:
        res1.body.skillApplied
        res2.body.skillApplied
        res3.body.skillApplied
        res4.body.skillApplied
        res5.body.skillApplied

        !resLast.body.skillApplied
        resLast.body.explanation == "Not all dependent skills have been achieved. Missing achievements for 1 out of 3. Waiting on completion of [TestProject1:${badge2.badgeId}]."
    }

    def "credit if dependency was fulfilled: learning path of [badge1, badge2, skill] -> badge3"(){
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(10, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge1 = SkillsFactory.createBadge(1, 1)
        skillsService.createBadge(badge1)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[0].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[1].skillId])
        badge1.enabled = true
        skillsService.createBadge(badge1)

        def badge2 = SkillsFactory.createBadge(1, 2)
        skillsService.createBadge(badge2)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[2].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[3].skillId])
        badge2.enabled = true
        skillsService.createBadge(badge2)

        def badge3 = SkillsFactory.createBadge(1, 3)
        skillsService.createBadge(badge3)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge3.badgeId, skillId: p1Skills[4].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge3.badgeId, skillId: p1Skills[5].skillId])
        badge3.enabled = true
        skillsService.createBadge(badge3)

        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[7].skillId, p1Skills[6].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge1.badgeId, p1Skills[7].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge3.badgeId, badge1.badgeId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge3.badgeId, badge2.badgeId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge3.badgeId, p1Skills[9].skillId)

        when:
        def res1 = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[6].skillId])
        def res2 = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[7].skillId])
        def res3 = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[0].skillId])
        def res4 = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[1].skillId])
        def res5 = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[9].skillId])
        def res6 = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[2].skillId])
        def res7 = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[3].skillId])


        def resLast = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[4].skillId])
        then:
        res1.body.skillApplied
        res2.body.skillApplied
        res3.body.skillApplied
        res4.body.skillApplied
        res5.body.skillApplied
        res6.body.skillApplied
        res7.body.skillApplied

        resLast.body.skillApplied
    }

    def "give credit if dependency was fulfilled"(){
        List<Map> skills = SkillsFactory.createSkills(2)
        skills.each{
            it.pointIncrement = 50
        }

        when:
        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkills(skills)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(1).skillId, skills.get(0).skillId)

        def res0 = skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId])
        def res = skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(1).skillId])

        then:
        res0.body.skillApplied
        res0.body.explanation == "Skill event was applied"

        res.body.skillApplied
        res.body.explanation == "Skill event was applied"
    }

    def "make sure that other users achievements don't affect requested users"(){
        List<Map> skills = SkillsFactory.createSkills(2)
        skills.each{
            it.pointIncrement = 50
        }

        when:
        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkills(skills)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(1).skillId, skills.get(0).skillId)


        def res0 = skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId], sampleUserIds.get(0), new Date())
        def res1 = skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(1).skillId],sampleUserIds.get(1), new Date())
        def res2 = skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(1).skillId], sampleUserIds.get(0), new Date())

        then:
        res0.body.skillApplied
        res0.body.explanation == "Skill event was applied"

        !res1.body.skillApplied
        res1.body.explanation == "Not all dependent skills have been achieved. Missing achievements for 1 out of 1. Waiting on completion of [TestProject1:skill1]."

        res2.body.skillApplied
        res2.body.explanation == "Skill event was applied"
    }

    def "give credit if all dependencies were fulfilled"(){
        List<Map> skills = SkillsFactory.createSkills(4)
        skills.each{
            it.pointIncrement = 25
        }

        when:
        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkills(skills)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(3).skillId, skills.get(0).skillId)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(3).skillId, skills.get(1).skillId)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(3).skillId, skills.get(2).skillId)

        def resSkill1 = skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId])
        def resSkill3 = skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(2).skillId])
        def resSkill2 = skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(1).skillId])
        def resSkill4 = skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(3).skillId])

        then:
        resSkill1.body.skillApplied
        resSkill3.body.skillApplied
        resSkill2.body.skillApplied
        resSkill4.body.skillApplied
    }

    def "give credit for passed quiz skill after dependency is fulfilled: learning path of non-quiz-skill -> quiz-skill-done"(){
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)

        def skillWithQuiz = createSkill(1, 1, 1, 1, 1, 480, 200)
        skillWithQuiz.selfReportingType = SkillDef.SelfReportingType.Quiz
        skillWithQuiz.quizId = quiz.quizId

        def skillWithoutQuiz = createSkill(1, 1, 2, 1, 1, 480, 200)

        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [skillWithQuiz, skillWithoutQuiz])

        // skill2 -> skill1 (quiz skill)
        skillsService.addLearningPathPrerequisite(p1.projectId, skillWithQuiz.skillId, skillWithoutQuiz.skillId)

        when:
        String user = getRandomUsers(1)[0]
        def quizRes = runQuiz(user, quiz, true)
        def res = skillsService.addSkill([projectId: p1.projectId, skillId: skillWithoutQuiz.skillId], user)

        then:
        quizRes
        quizRes.passed
        quizRes.associatedSkillResults && quizRes.associatedSkillResults.size() == 1
        !quizRes.associatedSkillResults[0].skillApplied
        quizRes.associatedSkillResults[0].explanation == "Not all dependent skills have been achieved. Missing achievements for 1 out of 1. Waiting on completion of [TestProject1:${skillWithoutQuiz.skillId}]."
        res.body.skillApplied
        res.body.explanation == "Skill event was applied"
        res.body.completed && res.body.completed.find { it.id == skillWithQuiz.skillId }
    }

    def "give credit for passed quiz skill after *all* dependencies are fulfilled: learning path of [skill1-done, skill2-done] -> skill3 [quiz-skill-done]"(){
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(2, 1, 1, 100)
        def skillWithQuiz = createSkill(1, 1, 3, 1, 1, 480, 200)
        skillWithQuiz.selfReportingType = SkillDef.SelfReportingType.Quiz
        skillWithQuiz.quizId = quiz.quizId

        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills + skillWithQuiz)

        // [skill1, skill2] -> skill3 (quiz skill)
        skillsService.addLearningPathPrerequisite(p1.projectId, skillWithQuiz.skillId, p1Skills[0].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, skillWithQuiz.skillId, p1Skills[1].skillId)

        when:
        String user = getRandomUsers(1)[0]
        def quizRes = runQuiz(user, quiz, true)
        def res1 = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[0].skillId], user)
        def res2 = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[1].skillId], user)

        then:
        quizRes
        quizRes.passed
        quizRes.associatedSkillResults && quizRes.associatedSkillResults.size() == 1
        !quizRes.associatedSkillResults[0].skillApplied
        quizRes.associatedSkillResults[0].explanation == "Not all dependent skills have been achieved. Missing achievements for 2 out of 2. Waiting on completion of [TestProject1:skill1, TestProject1:skill2]."
        res1.body.skillApplied
        res1.body.explanation == "Skill event was applied"
        res1.body.completed && !res1.body.completed.find { it.id == skillWithQuiz.skillId }
        res2.body.skillApplied
        res2.body.explanation == "Skill event was applied"
        res2.body.completed && res2.body.completed.find { it.id == skillWithQuiz.skillId }
    }

    def "do not give credit for passed quiz skill if dependency was partially fulfilled: learning path of [skill1-done, skill2] -> skill3 [quiz-skill-done]"(){
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(2, 1, 1, 100)
        def skillWithQuiz = createSkill(1, 1, 3, 1, 1, 480, 200)
        skillWithQuiz.selfReportingType = SkillDef.SelfReportingType.Quiz
        skillWithQuiz.quizId = quiz.quizId

        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills + skillWithQuiz)

        // [skill1, skill2] -> skill3 (quiz skill)
        skillsService.addLearningPathPrerequisite(p1.projectId, skillWithQuiz.skillId, p1Skills[0].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, skillWithQuiz.skillId, p1Skills[1].skillId)

        when:
        String user = getRandomUsers(1)[0]
        def quizRes = runQuiz(user, quiz, true)
        def res1 = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[0].skillId], user)

        then:
        quizRes
        quizRes.passed
        quizRes.associatedSkillResults && quizRes.associatedSkillResults.size() == 1
        !quizRes.associatedSkillResults[0].skillApplied
        quizRes.associatedSkillResults[0].explanation == "Not all dependent skills have been achieved. Missing achievements for 2 out of 2. Waiting on completion of [TestProject1:skill1, TestProject1:skill2]."
        res1.body.skillApplied
        res1.body.explanation == "Skill event was applied"
        res1.body.completed && !res1.body.completed.find { it.id == skillWithQuiz.skillId }
    }

    def "give credit for passed quiz skill after dependency is fulfilled: learning path of badge1 -> skill3 [quiz-skill-done]"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(2, 1, 1, 100)
        def skillWithQuiz = createSkill(1, 1, 3, 1, 1, 480, 200)
        skillWithQuiz.selfReportingType = SkillDef.SelfReportingType.Quiz
        skillWithQuiz.quizId = quiz.quizId

        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills + skillWithQuiz)

        def badge1 = SkillsFactory.createBadge(1, 1)
        skillsService.createBadge(badge1)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[0].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[1].skillId])
        badge1.enabled = true
        skillsService.createBadge(badge1)

        // badge1 (skill1 & skill2) -> skill3 (quiz skill)
        skillsService.addLearningPathPrerequisite(p1.projectId, skillWithQuiz.skillId, badge1.badgeId)

        when:
        String user = getRandomUsers(1)[0]
        def quizRes = runQuiz(user, quiz, true)
        def res1 = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[0].skillId], user)
        def res2 = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[1].skillId], user)

        then:
        quizRes
        quizRes.passed
        quizRes.associatedSkillResults && quizRes.associatedSkillResults.size() == 1
        !quizRes.associatedSkillResults[0].skillApplied
        quizRes.associatedSkillResults[0].explanation == "Not all dependent skills have been achieved. Missing achievements for 1 out of 1. Waiting on completion of [TestProject1:${badge1.badgeId}]."
        res1.body.skillApplied
        res1.body.explanation == "Skill event was applied"
        res1.body.completed && !res1.body.completed.find { it.id == badge1.badgeId } && !res1.body.completed.find { it.id == skillWithQuiz.skillId }
        res2.body.skillApplied
        res2.body.explanation == "Skill event was applied"
        res2.body.completed && res2.body.completed.find { it.id == badge1.badgeId } && res2.body.completed.find { it.id == skillWithQuiz.skillId }
    }

    def "do not give credit for passed quiz skill if badge dependency was partially fulfilled: learning path of [skill1-done, skill2] -> skill3 [quiz-skill-done]"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(2, 1, 1, 100)
        def skillWithQuiz = createSkill(1, 1, 3, 1, 1, 480, 200)
        skillWithQuiz.selfReportingType = SkillDef.SelfReportingType.Quiz
        skillWithQuiz.quizId = quiz.quizId

        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills + skillWithQuiz)

        def badge1 = SkillsFactory.createBadge(1, 1)
        skillsService.createBadge(badge1)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[0].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[1].skillId])
        badge1.enabled = true
        skillsService.createBadge(badge1)

        // badge1 (skill1 & skill2) -> skill3 (quiz skill)
        skillsService.addLearningPathPrerequisite(p1.projectId, skillWithQuiz.skillId, badge1.badgeId)

        when:
        String user = getRandomUsers(1)[0]
        def quizRes = runQuiz(user, quiz, true)
        def res1 = skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[0].skillId], user)

        then:
        quizRes
        quizRes.passed
        quizRes.associatedSkillResults && quizRes.associatedSkillResults.size() == 1
        !quizRes.associatedSkillResults[0].skillApplied
        quizRes.associatedSkillResults[0].explanation == "Not all dependent skills have been achieved. Missing achievements for 1 out of 1. Waiting on completion of [TestProject1:${badge1.badgeId}]."
        res1.body.skillApplied
        res1.body.explanation == "Skill event was applied"
        res1.body.completed && !res1.body.completed.find { it.id == badge1.badgeId } && !res1.body.completed.find { it.id == skillWithQuiz.skillId }
    }

    def runQuiz(String userId, def quiz, boolean pass) {
        def quizAttempt =  skillsService.startQuizAttemptForUserId(quiz.quizId, userId).body
        skillsService.reportQuizAnswerForUserId(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, userId)
        skillsService.reportQuizAnswerForUserId(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[pass ? 0 : 1].id, userId)
        return skillsService.completeQuizAttemptForUserId(quiz.quizId, quizAttempt.id, userId).body
    }
}
