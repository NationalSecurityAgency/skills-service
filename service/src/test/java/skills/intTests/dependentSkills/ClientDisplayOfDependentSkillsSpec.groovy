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
import skills.intTests.utils.SkillsFactory

class ClientDisplayOfDependentSkillsSpec extends DefaultIntSpec {

    def "sometime there just no dependencies"() {
        String userId = "user1"
        List<Map> skills = SkillsFactory.createSkills(2)
        def subject = SkillsFactory.createSubject()

        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(subject)
        skillsService.createSkills(skills)

        when:
        def skillSummary = skillsService.getSkillSummary(userId, SkillsFactory.defaultProjId, subject.subjectId)

        then:
        skillSummary.skills.size() == 2

        def skill1 = skillSummary.skills.find { it.skillId == "skill1" }
        !skill1.dependencyInfo

        def skill2 = skillSummary.skills.find { it.skillId == "skill2" }
        !skill2.dependencyInfo
    }

    def "indicate that skill requires achievement of dependency before it can actually be performed"() {
        String userId = "user1"
        List<Map> skills = SkillsFactory.createSkills(2)
        def subject = SkillsFactory.createSubject()

        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(subject)
        skillsService.createSkills(skills)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(0).skillId, skills.get(1).skillId)

        when:
        def skillSummary = skillsService.getSkillSummary(userId, SkillsFactory.defaultProjId, subject.subjectId)

        then:
        skillSummary.skills.size() == 2

        def skill1 = skillSummary.skills.find { it.skillId == "skill1" }
        skill1.dependencyInfo.numDirectDependents == 1
        !skill1.dependencyInfo.achieved

        def skill2 = skillSummary.skills.find { it.skillId == "skill2" }
        !skill2.dependencyInfo
    }

    def "document achievement of dependent skill(s)"() {
        String userId = "user1"
        List<Map> skills = SkillsFactory.createSkills(2)
        skills.each{
            it.pointIncrement = 50
        }
        def subject = SkillsFactory.createSubject()

        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(subject)
        skillsService.createSkills(skills)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(0).skillId, skills.get(1).skillId)
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(1).skillId], userId, new Date())

        when:
        def skillSummary = skillsService.getSkillSummary(userId, SkillsFactory.defaultProjId, subject.subjectId)

        then:
        skillSummary.skills.size() == 2

        def skill1 = skillSummary.skills.find { it.skillId == "skill1" }
        skill1.dependencyInfo.numDirectDependents == 1
        skill1.dependencyInfo.achieved

        def skill2 = skillSummary.skills.find { it.skillId == "skill2" }
        !skill2.dependencyInfo
    }

    def "all skills must be achieved before achieved flag is set to true"() {
        String userId = "user1"
        List<Map> skills = SkillsFactory.createSkills(3)
        skills.each{
            it.pointIncrement = 40
        }
        def subject = SkillsFactory.createSubject()

        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(subject)
        skillsService.createSkills(skills)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(0).skillId, skills.get(1).skillId)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(0).skillId, skills.get(2).skillId)
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(1).skillId], userId, new Date())

        when:
        def skillSummary = skillsService.getSkillSummary(userId, SkillsFactory.defaultProjId, subject.subjectId)

        then:
        skillSummary.skills.size() == 3

        def skill1 = skillSummary.skills.find { it.skillId == "skill1" }
        skill1.dependencyInfo.numDirectDependents == 2
        !skill1.dependencyInfo.achieved

        def skill2 = skillSummary.skills.find { it.skillId == "skill2" }
        !skill2.dependencyInfo

        def skill3 = skillSummary.skills.find { it.skillId == "skill3" }
        !skill3.dependencyInfo
    }

    def "all dependents have been achieved so achieved flag should be set to true"() {
        String userId = "user1"
        List<Map> skills = SkillsFactory.createSkills(3)
        skills.each{
            it.pointIncrement = 40
        }
        def subject = SkillsFactory.createSubject()

        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(subject)
        skillsService.createSkills(skills)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(0).skillId, skills.get(1).skillId)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(0).skillId, skills.get(2).skillId)
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(1).skillId], userId, new Date())
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(2).skillId], userId, new Date())

        when:
        def skillSummary = skillsService.getSkillSummary(userId, SkillsFactory.defaultProjId, subject.subjectId)

        then:
        skillSummary.skills.size() == 3

        def skill1 = skillSummary.skills.find { it.skillId == "skill1" }
        skill1.dependencyInfo.numDirectDependents == 2
        skill1.dependencyInfo.achieved

        def skill2 = skillSummary.skills.find { it.skillId == "skill2" }
        !skill2.dependencyInfo

        def skill3 = skillSummary.skills.find { it.skillId == "skill3" }
        !skill3.dependencyInfo
    }

    def "not achieved if transitive dependency is achieved but not direct dependency"() {
        String userId = "user1"
        List<Map> skills = SkillsFactory.createSkills(3)
        skills.each{
            it.pointIncrement = 40
        }
        def subject = SkillsFactory.createSubject()

        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(subject)
        skillsService.createSkills(skills)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(0).skillId, skills.get(1).skillId)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(1).skillId, skills.get(2).skillId)
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(2).skillId], userId, new Date())

        when:
        def skillSummary = skillsService.getSkillSummary(userId, SkillsFactory.defaultProjId, subject.subjectId)

        then:
        skillSummary.skills.size() == 3

        def skill1 = skillSummary.skills.find { it.skillId == "skill1" }
        skill1.dependencyInfo.numDirectDependents == 1
        !skill1.dependencyInfo.achieved

        def skill2 = skillSummary.skills.find { it.skillId == "skill2" }
        skill2.dependencyInfo.numDirectDependents == 1
        skill2.dependencyInfo.achieved

        def skill3 = skillSummary.skills.find { it.skillId == "skill3" }
        !skill3.dependencyInfo
    }


    def "there are no dependency info for a skill"() {
        String userId = "user1"
        List<Map> skills = SkillsFactory.createSkills(3)
        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkills(skills)

        when:
        def dependencyInfo = skillsService.getSkillDependencyInfo(userId, SkillsFactory.defaultProjId, skills.get(0).skillId)

        then:
        !dependencyInfo.dependencies
    }

    def "show dependency info for 1 skill"() {
        String userId = "user1"
        List<Map> skills = SkillsFactory.createSkills(3)
        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkills(skills)

        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(0).skillId, skills.get(1).skillId)

        when:
        def dependencyInfo = skillsService.getSkillDependencyInfo(userId, SkillsFactory.defaultProjId, skills.get(0).skillId)

        then:
        dependencyInfo.dependencies.size() == 1
        def skill = dependencyInfo.dependencies.get(0)
        skill.dependsOn.skillId == "skill2"
        !skill.achieved
    }

    def "show dependency info for multiple skill"() {
        String userId = "user1"
        List<Map> skills = SkillsFactory.createSkills(4)
        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkills(skills)

        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(0).skillId, skills.get(1).skillId)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(0).skillId, skills.get(2).skillId)

        when:
        def dependencyInfo = skillsService.getSkillDependencyInfo(userId, SkillsFactory.defaultProjId, skills.get(0).skillId)

        then:
        dependencyInfo.dependencies.size() == 2
        def skill1 = dependencyInfo.dependencies.get(0)
        skill1.dependsOn.skillId == "skill2"
        !skill1.achieved

        def skill2 = dependencyInfo.dependencies.get(1)
        skill2.dependsOn.skillId == "skill3"
        !skill2.achieved
    }

    def "show dependency info for multiple skill from different subjects"() {
        String userId = "user1"
        List<Map> skills = SkillsFactory.createSkills(4)
        def subj1 = SkillsFactory.createSubject()
        def subj2 = SkillsFactory.createSubject(1, 2)
        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(subj1)
        skillsService.createSkills(skills)
        skillsService.createSubject(subj2)
        List<Map> skills_subj2 = SkillsFactory.createSkills(4, 1, 2)
        skillsService.createSkills(skills_subj2)

        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(0).skillId, skills.get(1).skillId)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(0).skillId, skills_subj2.get(1).skillId)

        when:
        def dependencyInfo = skillsService.getSkillDependencyInfo(userId, SkillsFactory.defaultProjId, skills.get(0).skillId)

        then:
        dependencyInfo.dependencies.size() == 2
        def skill1 = dependencyInfo.dependencies.get(0)
        skill1.dependsOn.skillId == "skill2"
        skill1.dependsOn.subjectId == subj1.subjectId
        !skill1.achieved

        def skill2 = dependencyInfo.dependencies.get(1)
        skill2.dependsOn.skillId == "skill2subj2"
        skill2.dependsOn.subjectId == subj2.subjectId
        !skill2.achieved
    }

    def "show dependency info for skills with transitive dependencies"() {
        String userId = "user1"
        List<Map> skills = SkillsFactory.createSkills(10)
        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkills(skills)

        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(0).skillId, skills.get(1).skillId)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(1).skillId, skills.get(2).skillId)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(1).skillId, skills.get(3).skillId)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(1).skillId, skills.get(4).skillId)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(4).skillId, skills.get(9).skillId)

        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(0).skillId, skills.get(5).skillId)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(5).skillId, skills.get(6).skillId)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(5).skillId, skills.get(7).skillId)

        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(0).skillId, skills.get(8).skillId)

        when:
        def dependencyInfo = skillsService.getSkillDependencyInfo(userId, SkillsFactory.defaultProjId, skills.get(0).skillId)

        then:
        dependencyInfo.dependencies.size() == 9
        dependencyInfo.dependencies.find { it.dependsOn.skillId == "skill2" }.skill.skillId == "skill1"
        dependencyInfo.dependencies.find { it.dependsOn.skillId == "skill3" }.skill.skillId == "skill2"
        dependencyInfo.dependencies.find { it.dependsOn.skillId == "skill4" }.skill.skillId == "skill2"
        dependencyInfo.dependencies.find { it.dependsOn.skillId == "skill5" }.skill.skillId == "skill2"
        dependencyInfo.dependencies.find { it.dependsOn.skillId == "skill6" }.skill.skillId == "skill1"
        dependencyInfo.dependencies.find { it.dependsOn.skillId == "skill7" }.skill.skillId == "skill6"
        dependencyInfo.dependencies.find { it.dependsOn.skillId == "skill8" }.skill.skillId == "skill6"
        dependencyInfo.dependencies.find { it.dependsOn.skillId == "skill9" }.skill.skillId == "skill1"
        dependencyInfo.dependencies.find { it.dependsOn.skillId == "skill10" }.skill.skillId == "skill5"

        dependencyInfo.dependencies.each {
            assert !it.achieved
            assert it.skill.projectId == SkillsFactory.defaultProjId
            assert it.dependsOn.projectId == SkillsFactory.defaultProjId
        }
    }


    def "show dependency info for skills with transitive dependencies - user fully achieved all skills"() {
        String userId = "user1"
        List<Map> skills = SkillsFactory.createSkills(10)
        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkills(skills)

        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(0).skillId, skills.get(1).skillId)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(1).skillId, skills.get(2).skillId)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(1).skillId, skills.get(3).skillId)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(1).skillId, skills.get(4).skillId)

        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(0).skillId, skills.get(5).skillId)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(5).skillId, skills.get(6).skillId)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(5).skillId, skills.get(7).skillId)

        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(0).skillId, skills.get(8).skillId)

        skills.reverse().each {
            skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: it.skillId], userId, new Date())
        }

        when:
        def dependencyInfo = skillsService.getSkillDependencyInfo(userId, SkillsFactory.defaultProjId, skills.get(0).skillId)

        then:
        dependencyInfo.dependencies.size() == 8
        dependencyInfo.dependencies.find { it.dependsOn.skillId == "skill2" }.skill.skillId == "skill1"
        dependencyInfo.dependencies.find { it.dependsOn.skillId == "skill3" }.skill.skillId == "skill2"
        dependencyInfo.dependencies.find { it.dependsOn.skillId == "skill4" }.skill.skillId == "skill2"
        dependencyInfo.dependencies.find { it.dependsOn.skillId == "skill5" }.skill.skillId == "skill2"
        dependencyInfo.dependencies.find { it.dependsOn.skillId == "skill6" }.skill.skillId == "skill1"
        dependencyInfo.dependencies.find { it.dependsOn.skillId == "skill7" }.skill.skillId == "skill6"
        dependencyInfo.dependencies.find { it.dependsOn.skillId == "skill8" }.skill.skillId == "skill6"
        dependencyInfo.dependencies.find { it.dependsOn.skillId == "skill9" }.skill.skillId == "skill1"

        dependencyInfo.dependencies.each {
            assert it.achieved
            assert it.skill.projectId == SkillsFactory.defaultProjId
            assert it.dependsOn.projectId == SkillsFactory.defaultProjId
        }
    }

    def "show dependency info for skills with transitive dependencies - some transitive skills complete"() {
        String userId = "user1"
        List<Map> skills = SkillsFactory.createSkills(10)
        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkills(skills)

        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(0).skillId, skills.get(1).skillId)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(1).skillId, skills.get(2).skillId)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(1).skillId, skills.get(3).skillId)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(1).skillId, skills.get(4).skillId)

        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(0).skillId, skills.get(5).skillId)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(5).skillId, skills.get(6).skillId)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(5).skillId, skills.get(7).skillId)

        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(0).skillId, skills.get(8).skillId)

        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(3).skillId], userId, new Date())
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(4).skillId], userId, new Date())
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(7).skillId], userId, new Date())

        when:
        def dependencyInfo = skillsService.getSkillDependencyInfo(userId, SkillsFactory.defaultProjId, skills.get(0).skillId)

        then:
        dependencyInfo.dependencies.size() == 8
        dependencyInfo.dependencies.find { it.dependsOn.skillId == "skill2" }.skill.skillId == "skill1"
        dependencyInfo.dependencies.find { it.dependsOn.skillId == "skill3" }.skill.skillId == "skill2"
        dependencyInfo.dependencies.find { it.dependsOn.skillId == "skill4" }.skill.skillId == "skill2"
        dependencyInfo.dependencies.find { it.dependsOn.skillId == "skill5" }.skill.skillId == "skill2"
        dependencyInfo.dependencies.find { it.dependsOn.skillId == "skill6" }.skill.skillId == "skill1"
        dependencyInfo.dependencies.find { it.dependsOn.skillId == "skill7" }.skill.skillId == "skill6"
        dependencyInfo.dependencies.find { it.dependsOn.skillId == "skill8" }.skill.skillId == "skill6"
        dependencyInfo.dependencies.find { it.dependsOn.skillId == "skill9" }.skill.skillId == "skill1"

        !dependencyInfo.dependencies.find { it.dependsOn.skillId == "skill2" }.achieved
        !dependencyInfo.dependencies.find { it.dependsOn.skillId == "skill3" }.achieved
        dependencyInfo.dependencies.find { it.dependsOn.skillId == "skill4" }.achieved
        dependencyInfo.dependencies.find { it.dependsOn.skillId == "skill5" }.achieved
        !dependencyInfo.dependencies.find { it.dependsOn.skillId == "skill6" }.achieved
        !dependencyInfo.dependencies.find { it.dependsOn.skillId == "skill7" }.achieved
        dependencyInfo.dependencies.find { it.dependsOn.skillId == "skill8" }.achieved
        !dependencyInfo.dependencies.find { it.dependsOn.skillId == "skill9" }.achieved


        dependencyInfo.dependencies.each {
            assert it.skill.projectId == SkillsFactory.defaultProjId
            assert it.dependsOn.projectId == SkillsFactory.defaultProjId
        }
    }

    def "show dependency info for skills with transitive dependencies - partially completed"() {
        String userId = "user1"
        List<Map> skills = SkillsFactory.createSkills(10)
        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkills(skills)

        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(0).skillId, skills.get(1).skillId)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(1).skillId, skills.get(2).skillId)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(1).skillId, skills.get(3).skillId)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(1).skillId, skills.get(4).skillId)

        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(0).skillId, skills.get(5).skillId)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(5).skillId, skills.get(6).skillId)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(5).skillId, skills.get(7).skillId)

        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(0).skillId, skills.get(8).skillId)

        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(3).skillId], userId, new Date())
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(4).skillId], userId, new Date())
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(6).skillId], userId, new Date())
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(7).skillId], userId, new Date())
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(5).skillId], userId, new Date())

        when:
        def dependencyInfo = skillsService.getSkillDependencyInfo(userId, SkillsFactory.defaultProjId, skills.get(0).skillId)

        then:
        dependencyInfo.dependencies.size() == 8
        dependencyInfo.dependencies.find { it.dependsOn.skillId == "skill2" }.skill.skillId == "skill1"
        dependencyInfo.dependencies.find { it.dependsOn.skillId == "skill3" }.skill.skillId == "skill2"
        dependencyInfo.dependencies.find { it.dependsOn.skillId == "skill4" }.skill.skillId == "skill2"
        dependencyInfo.dependencies.find { it.dependsOn.skillId == "skill5" }.skill.skillId == "skill2"
        dependencyInfo.dependencies.find { it.dependsOn.skillId == "skill6" }.skill.skillId == "skill1"
        dependencyInfo.dependencies.find { it.dependsOn.skillId == "skill7" }.skill.skillId == "skill6"
        dependencyInfo.dependencies.find { it.dependsOn.skillId == "skill8" }.skill.skillId == "skill6"
        dependencyInfo.dependencies.find { it.dependsOn.skillId == "skill9" }.skill.skillId == "skill1"

        !dependencyInfo.dependencies.find { it.dependsOn.skillId == "skill2" }.achieved
        !dependencyInfo.dependencies.find { it.dependsOn.skillId == "skill3" }.achieved
        dependencyInfo.dependencies.find { it.dependsOn.skillId == "skill4" }.achieved
        dependencyInfo.dependencies.find { it.dependsOn.skillId == "skill5" }.achieved
        dependencyInfo.dependencies.find { it.dependsOn.skillId == "skill6" }.achieved
        dependencyInfo.dependencies.find { it.dependsOn.skillId == "skill7" }.achieved
        dependencyInfo.dependencies.find { it.dependsOn.skillId == "skill8" }.achieved
        !dependencyInfo.dependencies.find { it.dependsOn.skillId == "skill9" }.achieved


        dependencyInfo.dependencies.each {
            assert it.skill.projectId == SkillsFactory.defaultProjId
            assert it.dependsOn.projectId == SkillsFactory.defaultProjId
        }
    }

    def "skill can depend on a badge"() {
        String userId = "user1"
        List<Map> skills = SkillsFactory.createSkills(2)
        skills.each{
            it.pointIncrement = 50
        }
        def subject = SkillsFactory.createSubject()

        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(subject)
        skillsService.createSkills(skills)

        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId], userId, new Date())
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(1).skillId], userId, new Date())

        def badge = SkillsFactory.createBadge()
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge([projectId: SkillsFactory.defaultProjId, badgeId: badge.badgeId, skillId: skills.get(0).skillId])
        badge.enabled = true
        skillsService.createBadge(badge)

        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(1).skillId, badge.badgeId)

        when:
        def skillSummary = skillsService.getSkillSummary(userId, SkillsFactory.defaultProjId, subject.subjectId)

        then:
        skillSummary.skills.size() == 2

        def skill1 = skillSummary.skills.find { it.skillId == "skill1" }
        !skill1.dependencyInfo
        !skill1.badgeDependencyInfo

        def skill2 = skillSummary.skills.find { it.skillId == "skill2" }
        skill2.dependencyInfo.numDirectDependents == 1
        skill2.dependencyInfo.achieved

    }

    def "badge can depend on a skill"() {
        String userId = "user1"
        List<Map> skills = SkillsFactory.createSkills(2)
        skills.each{
            it.pointIncrement = 50
        }
        def subject = SkillsFactory.createSubject()

        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(subject)
        skillsService.createSkills(skills)

        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId], userId, new Date())
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(1).skillId], userId, new Date())

        def badge = SkillsFactory.createBadge()
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge([projectId: SkillsFactory.defaultProjId, badgeId: badge.badgeId, skillId: skills.get(0).skillId])
        badge.enabled = true
        skillsService.createBadge(badge)

        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, badge.badgeId, skills.get(1).skillId)

        when:
        def skillSummary = skillsService.getSkillSummary(userId, SkillsFactory.defaultProjId, subject.subjectId)

        then:
        skillSummary.skills.size() == 2

        def skill1 = skillSummary.skills.find { it.skillId == "skill1" }
        skill1.badgeDependencyInfo[0].numDirectDependents == 1
        skill1.badgeDependencyInfo[0].achieved

        def skill2 = skillSummary.skills.find { it.skillId == "skill2" }
        !skill2.dependencyInfo

    }

    def "badge can depend on a badge"() {
        String userId = "user1"
        List<Map> skills = SkillsFactory.createSkills(2)
        skills.each{
            it.pointIncrement = 50
        }
        def subject = SkillsFactory.createSubject()

        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(subject)
        skillsService.createSkills(skills)

        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId], userId, new Date())
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(1).skillId], userId, new Date())

        def badge = SkillsFactory.createBadge()
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge([projectId: SkillsFactory.defaultProjId, badgeId: badge.badgeId, skillId: skills.get(0).skillId])
        badge.enabled = true
        skillsService.createBadge(badge)

        def badge2 = SkillsFactory.createBadge(1, 2)
        skillsService.createBadge(badge2)
        skillsService.assignSkillToBadge([projectId: SkillsFactory.defaultProjId, badgeId: badge2.badgeId, skillId: skills.get(1).skillId])
        badge2.enabled = true
        skillsService.createBadge(badge2)

        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, badge.badgeId, badge2.badgeId)

        when:
        def skillSummary = skillsService.getSkillSummary(userId, SkillsFactory.defaultProjId, subject.subjectId)

        then:
        skillSummary.skills.size() == 2

        def skill1 = skillSummary.skills.find { it.skillId == "skill1" }
        skill1.badgeDependencyInfo[0].numDirectDependents == 1
        skill1.badgeDependencyInfo[0].achieved

        def skill2 = skillSummary.skills.find { it.skillId == "skill2" }
        !skill2.dependencyInfo

    }
}
