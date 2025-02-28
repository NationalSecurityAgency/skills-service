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

import skills.controller.result.model.DependencyCheckResult
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory

import static skills.intTests.utils.SkillsFactory.*

class LearningPathValidationEndpointSpecs extends DefaultIntSpec {

    def "skill1 -> skill2 -> skill1 circular dep"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(5, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[0].skillId, p1.projectId, p1Skills[2].skillId)
        when:
        def result = skillsService.vadlidateLearningPathPrerequisite(p1.projectId, p1Skills[2].skillId, p1.projectId, p1Skills[0].skillId)
        then:
        result.possible == false
        result.failureType == DependencyCheckResult.FailureType.CircularLearningPath.toString()
        !result.violatingSkillInBadgeId
        !result.violatingSkillInBadgeName
    }

    def "skill1 -> skill2 -> skill3 -> skill1 circular dep"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(5, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[1].skillId, p1.projectId, p1Skills[2].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[0].skillId, p1.projectId, p1Skills[1].skillId)
        when:
        def result = skillsService.vadlidateLearningPathPrerequisite(p1.projectId, p1Skills[2].skillId, p1.projectId, p1Skills[0].skillId)
        then:
        result.possible == false
        result.failureType == DependencyCheckResult.FailureType.CircularLearningPath.toString()
        !result.violatingSkillInBadgeId
        !result.violatingSkillInBadgeName
    }

    def "skill0 -> skill1 -> [this connection added] -> skill2 -> skill3 -> skill0 circular dep"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(5, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[1].skillId, p1.projectId, p1Skills[0].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[3].skillId, p1.projectId, p1Skills[2].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[0].skillId, p1.projectId, p1Skills[3].skillId)
        when:
        def result = skillsService.vadlidateLearningPathPrerequisite(p1.projectId, p1Skills[2].skillId, p1.projectId, p1Skills[1].skillId)
        then:
        result.possible == false
        result.failureType == DependencyCheckResult.FailureType.CircularLearningPath.toString()
        !result.violatingSkillInBadgeId
        !result.violatingSkillInBadgeName
        result.reason == "Discovered circular prerequisite [Skill:skill3 -> Skill:skill2 -> Skill:skill1 -> Skill:skill4 -> Skill:skill3]"
    }

    def "badge(skill1) -> skill1 learning path: badge cannot contain the skill"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(5, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge = SkillsFactory.createBadge()
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge.badgeId, skillId: p1Skills[0].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge.badgeId, skillId: p1Skills[1].skillId])
        badge.enabled = true
        skillsService.createBadge(badge)

        when:
        def result = skillsService.vadlidateLearningPathPrerequisite(p1.projectId, p1Skills[0].skillId, p1.projectId, badge.badgeId)
        then:
        result.possible == false
        result.failureType == DependencyCheckResult.FailureType.CircularLearningPath.toString()
        result.violatingSkillInBadgeId == badge.badgeId
        result.violatingSkillInBadgeName == badge.name
    }

    def "skill1 -> badge(skill2) -> skill1 learning path: badge cannot contain the skill"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(5, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge = SkillsFactory.createBadge()
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge.badgeId, skillId: p1Skills[0].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge.badgeId, skillId: p1Skills[1].skillId])
        badge.enabled = true
        skillsService.createBadge(badge)

        skillsService.addLearningPathPrerequisite(p1.projectId, badge.badgeId, p1.projectId, p1Skills[2].skillId)
        when:
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[2].skillId, p1.projectId, badge.badgeId)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.getMessage().contains("Discovered circular prerequisite [Skill:${p1Skills[2].skillId} -> Badge:${badge.badgeId} -> Skill:${p1Skills[2].skillId}]")
    }

    def "badge(skill2) -> skill1 -> badge(skill2) learning path: badge cannot contain the skill"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(5, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge = SkillsFactory.createBadge()
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge.badgeId, skillId: p1Skills[0].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge.badgeId, skillId: p1Skills[1].skillId])
        badge.enabled = true
        skillsService.createBadge(badge)

        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[2].skillId, p1.projectId, badge.badgeId)
        when:
        def result = skillsService.vadlidateLearningPathPrerequisite(p1.projectId, badge.badgeId, p1.projectId, p1Skills[2].skillId)
        then:
        result.possible == false
        result.failureType == DependencyCheckResult.FailureType.CircularLearningPath.toString()
        !result.violatingSkillInBadgeId
        !result.violatingSkillInBadgeName
    }

    def "[badge1, badge2] -> [badge3, badge4] -> skill1 -> badge2) learning path: badge cannot contain the skill"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(5, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge1 = SkillsFactory.createBadge(1, 1)
        skillsService.createBadge(badge1)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[0].skillId])
        badge1.enabled = true
        skillsService.createBadge(badge1)

        def badge2 = SkillsFactory.createBadge(1, 2)
        skillsService.createBadge(badge2)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[1].skillId])
        badge2.enabled = true
        skillsService.createBadge(badge2)

        def badge3 = SkillsFactory.createBadge(1, 3)
        skillsService.createBadge(badge3)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge3.badgeId, skillId: p1Skills[2].skillId])
        badge3.enabled = true
        skillsService.createBadge(badge3)

        def badge4 = SkillsFactory.createBadge(1, 4)
        skillsService.createBadge(badge4)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge4.badgeId, skillId: p1Skills[3].skillId])
        badge4.enabled = true
        skillsService.createBadge(badge4)

        skillsService.addLearningPathPrerequisite(p1.projectId, badge3.badgeId, p1.projectId, badge1.badgeId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge4.badgeId, p1.projectId, badge1.badgeId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge3.badgeId, p1.projectId, badge2.badgeId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge4.badgeId, p1.projectId, badge2.badgeId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[4].skillId, p1.projectId, badge3.badgeId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[4].skillId, p1.projectId, badge4.badgeId)

        when:
        def result = skillsService.vadlidateLearningPathPrerequisite(p1.projectId, badge2.badgeId, p1.projectId, p1Skills[4].skillId)
        then:
        result.possible == false
        result.failureType == DependencyCheckResult.FailureType.CircularLearningPath.toString()
        !result.violatingSkillInBadgeId
        !result.violatingSkillInBadgeName
    }

    def "badge1 -> badge2: two badges cannot contain the same skill in the same learning path"() {
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
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[1].skillId]) // same skill
        badge2.enabled = true
        skillsService.createBadge(badge2)

        when:
        def result = skillsService.vadlidateLearningPathPrerequisite(p1.projectId, badge2.badgeId, p1.projectId, badge1.badgeId)
        then:
        result.possible == false
        result.failureType == DependencyCheckResult.FailureType.BadgeOverlappingSkills.toString()
        result.violatingSkillInBadgeId == badge1.badgeId
        result.violatingSkillInBadgeName == badge1.name
        result.violatingSkillId == p1Skills[1].skillId
        result.violatingSkillName == p1Skills[1].name
    }

    def "skill3 -> skill4 -> badge1 -> skill5 -> skill6 -> badge2: two badges cannot contain the same skill in the same learning path"() {
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
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[1].skillId]) // same skill
        badge2.enabled = true
        skillsService.createBadge(badge2)

        // skill3 -> skill4 -> badge1 -> skill5 -> skill6 -> badge2
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[4].skillId, p1.projectId, p1Skills[3].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge1.badgeId, p1.projectId, p1Skills[4].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[5].skillId, p1.projectId, badge1.badgeId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[6].skillId, p1.projectId, p1Skills[5].skillId)
        when:
        def result = skillsService.vadlidateLearningPathPrerequisite(p1.projectId, badge2.badgeId, p1.projectId, p1Skills[6].skillId)
        then:
        result.possible == false
        result.failureType == DependencyCheckResult.FailureType.BadgeOverlappingSkills.toString()
        result.violatingSkillInBadgeId == badge1.badgeId
        result.violatingSkillInBadgeName == badge1.name
        result.violatingSkillId == p1Skills[1].skillId
        result.violatingSkillName == p1Skills[1].name
    }

    def "skill3 -> skill4 -> badge1 -> skill5 -> skill6 -> badge2: Cannot add a badge that already has one of its skills on the learning path"() {
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
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[4].skillId]) // this skill already on the path
        badge2.enabled = true
        skillsService.createBadge(badge2)

        // skill3 -> skill4 -> badge1 -> skill5 -> skill6 -> badge2
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[4].skillId, p1.projectId, p1Skills[3].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge1.badgeId, p1.projectId, p1Skills[4].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[5].skillId, p1.projectId, badge1.badgeId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[6].skillId, p1.projectId, p1Skills[5].skillId)
        when:
        def result = skillsService.vadlidateLearningPathPrerequisite(p1.projectId, badge2.badgeId, p1.projectId, p1Skills[6].skillId)
        then:
        result.possible == false
        result.failureType == DependencyCheckResult.FailureType.BadgeSkillIsAlreadyOnPath.toString()
        !result.violatingSkillInBadgeId
        !result.violatingSkillInBadgeName
        result.violatingSkillId == p1Skills[4].skillId
        result.violatingSkillName == p1Skills[4].name
        result.reason == "Badge [${badge2.name}] has skill [${p1Skills[4].name}] which already exists on the Learning Path."
    }

    def "skill3 -> skill4 -> badge1 -> skill5 -> [adding this link] -> skill6 -> badge2: Cannot add a badge that already has one of its skills on the learning path"() {
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
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[4].skillId]) // this skill already on the path
        badge2.enabled = true
        skillsService.createBadge(badge2)

        // skill3 -> skill4 -> badge1 -> skill5 -> skill6 -> badge2
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[4].skillId, p1.projectId, p1Skills[3].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge1.badgeId, p1.projectId, p1Skills[4].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[5].skillId, p1.projectId, badge1.badgeId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge2.badgeId, p1.projectId, p1Skills[6].skillId)
        when:
        def result = skillsService.vadlidateLearningPathPrerequisite(p1.projectId, p1Skills[6].skillId, p1.projectId, p1Skills[5].skillId)
        then:
        result.possible == false
        result.failureType == DependencyCheckResult.FailureType.BadgeSkillIsAlreadyOnPath.toString()
        !result.violatingSkillInBadgeId
        !result.violatingSkillInBadgeName
        result.violatingSkillId == p1Skills[4].skillId
        result.violatingSkillName == p1Skills[4].name
        result.reason == "Badge [${badge2.name}] has skill [${p1Skills[4].name}] which already exists on the Learning Path."
    }

    def "skill3 -> skill4 -> badge1 -> skill5 -> skill6 -> badge2: Cannot add a learning path item that already exist"() {
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
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[4].skillId]) // this skill already on the path
        badge2.enabled = true
        skillsService.createBadge(badge2)

        // skill3 -> skill4 -> badge1 -> skill5 -> skill6 -> badge2
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[4].skillId, p1.projectId, p1Skills[3].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge1.badgeId, p1.projectId, p1Skills[4].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[5].skillId, p1.projectId, badge1.badgeId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[6].skillId, p1.projectId, p1Skills[5].skillId)
        when:
        def result = skillsService.vadlidateLearningPathPrerequisite(p1.projectId, p1Skills[5].skillId, p1.projectId, badge1.badgeId)
        then:
        result.possible == false
        result.failureType == DependencyCheckResult.FailureType.AlreadyExist.toString()
        !result.violatingSkillInBadgeId
        !result.violatingSkillInBadgeName
        result.violatingSkillId == badge1.badgeId
        result.violatingSkillName == badge1.name
        result.reason == "Learning path from [${badge1.name}] to [${p1Skills[5].name}] already exists."
    }

    def "badge -> [adding this learning path item] -> skill -> badge: 2 badges must not have overlapping skills"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(10, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge1 = SkillsFactory.createBadge(1, 1)
        skillsService.createBadge(badge1)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[0].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[1].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[3].skillId])
        badge1.enabled = true
        skillsService.createBadge(badge1)

        def badge2 = SkillsFactory.createBadge(1, 2)
        skillsService.createBadge(badge2)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[2].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[3].skillId])
        badge2.enabled = true
        skillsService.createBadge(badge2)

        skillsService.addLearningPathPrerequisite(p1.projectId, badge2.badgeId, p1Skills[9].skillId)

        when:
        def result = skillsService.vadlidateLearningPathPrerequisite(p1.projectId, p1Skills[9].skillId, p1.projectId, badge1.badgeId)
        then:
        result.possible == false
        result.failureType == DependencyCheckResult.FailureType.BadgeOverlappingSkills.toString()
        result.violatingSkillInBadgeId == badge1.badgeId
        result.violatingSkillInBadgeName == badge1.name
        result.violatingSkillId == p1Skills[3].skillId
        result.violatingSkillName == p1Skills[3].name
        result.reason == "Multiple badges on the same Learning path cannot have overlapping skills. Both badge [Test Badge 1] and [Test Badge 2] badge have [${p1Skills[3].name}] skill."
    }

    def "badge -> skill -> [adding this learning path item] -> skill -> badge: cannot add the same skill twice"() {
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

        skillsService.addLearningPathPrerequisite(p1.projectId, badge1.badgeId, p1Skills[9].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[8].skillId, badge1.badgeId)

        when:
        def result = skillsService.vadlidateLearningPathPrerequisite(p1.projectId, p1Skills[9].skillId, p1.projectId, p1Skills[8].skillId)
        then:
        result.possible == false
        result.failureType == DependencyCheckResult.FailureType.CircularLearningPath.toString()
        !result.violatingSkillInBadgeId
        !result.violatingSkillInBadgeName
        !result.violatingSkillId
        !result.violatingSkillName
        result.reason == "Discovered circular prerequisite [Skill:skill10 -> Skill:skill9 -> Badge:badge1 -> Skill:skill10]"
    }

    def "skill -> badge -> [adding this learning path item] -> skill -> badge: 2 badges must not have overlapping skills"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(10, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge1 = SkillsFactory.createBadge(1, 1)
        skillsService.createBadge(badge1)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[0].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[1].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[3].skillId])
        badge1.enabled = true
        skillsService.createBadge(badge1)

        def badge2 = SkillsFactory.createBadge(1, 2)
        skillsService.createBadge(badge2)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[2].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[3].skillId])
        badge2.enabled = true
        skillsService.createBadge(badge2)

        skillsService.addLearningPathPrerequisite(p1.projectId, badge2.badgeId, p1Skills[9].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[8].skillId, badge2.badgeId)

        when:
        def result = skillsService.vadlidateLearningPathPrerequisite(p1.projectId, p1Skills[9].skillId, p1.projectId, badge1.badgeId)
        then:
        result.possible == false
        result.failureType == DependencyCheckResult.FailureType.BadgeOverlappingSkills.toString()
        result.violatingSkillInBadgeId == badge1.badgeId
        result.violatingSkillInBadgeName == badge1.name
        result.violatingSkillId == p1Skills[3].skillId
        result.violatingSkillName == p1Skills[3].name
        result.reason == "Multiple badges on the same Learning path cannot have overlapping skills. Both badge [Test Badge 1] and [Test Badge 2] badge have [${p1Skills[3].name}] skill."
    }

    def "skills exported to the catalog cannot have a prerequisite"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(10, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        skillsService.exportSkillToCatalog(p1.projectId, p1Skills[0].skillId)
        when:
        def result = skillsService.vadlidateLearningPathPrerequisite(p1.projectId, p1Skills[0].skillId, p1.projectId, p1Skills[1].skillId)
        then:
        result.possible == false
        result.failureType == DependencyCheckResult.FailureType.SkillInCatalog.toString()
        !result.violatingSkillInBadgeId
        !result.violatingSkillInBadgeName
        !result.violatingSkillId
        !result.violatingSkillName
        result.reason == "Skill [${p1Skills[0].skillId}] was exported to the Skills Catalog. A skill in the catalog cannot have prerequisites on the learning path."
    }

    def "re-used skills cannot have a prerequisite"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(10, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)

        skillsService.reuseSkillInAnotherSubject(p1.projectId, p1Skills[0].skillId, p1subj2.subjectId)
        when:
        def result = skillsService.vadlidateLearningPathPrerequisite(p1.projectId, p1Skills[0].skillId, p1.projectId, p1Skills[1].skillId)
        then:
        result.possible == false
        result.failureType == DependencyCheckResult.FailureType.ReusedSkill.toString()
        !result.violatingSkillInBadgeId
        !result.violatingSkillInBadgeName
        !result.violatingSkillId
        !result.violatingSkillName
        result.reason == "Skill [${p1Skills[0].skillId}] was reused in another subject or group and cannot have prerequisites in the learning path."
    }

    def "assigning dependent skills validates versions of the skills"() {
        List<Map> skills = SkillsFactory.createSkillsWithDifferentVersions([0, 0, 1])

        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkill(skills.get(0))
        skillsService.createSkill(skills.get(1))
        skillsService.createSkill(skills.get(2))

        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(1).skillId, SkillsFactory.defaultProjId, skills.get(0).skillId)
        when:
        def result = skillsService.vadlidateLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(1).skillId, SkillsFactory.defaultProjId, skills.get(2).skillId)

        then:
        result.possible == false
        result.failureType == DependencyCheckResult.FailureType.SkillVersion.toString()
        !result.violatingSkillInBadgeId
        !result.violatingSkillInBadgeName
        !result.violatingSkillId
        !result.violatingSkillName
        result.reason == "Not allowed to depend on skill with a later version. Skill [ID:skill2, version 0] can not depend on [ID:skill3, version 1]"
    }

    def "attempt to depend on skill that was not shared"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 2)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 2)

        skillsService.createProjectAndSubjectAndSkills(proj1, proj1_subj, proj1_skills)
        skillsService.createProjectAndSubjectAndSkills(proj2, proj2_subj, proj2_skills)

        when:
        def result = skillsService.vadlidateLearningPathPrerequisite(proj2.projectId, proj2_skills.get(0).skillId, proj1.projectId, proj1_skills.get(0).skillId)

        then:
        result.possible == false
        result.failureType == DependencyCheckResult.FailureType.NotEligible.toString()
        !result.violatingSkillInBadgeId
        !result.violatingSkillInBadgeName
        !result.violatingSkillId
        !result.violatingSkillName
        result.reason =="Skill [TestProject1:skill1] is not shared (or does not exist) to [TestProject2] project"
    }

    def "allow shared cross-project skills to the learning path even it happens to have the same skill-id is one of the existing skills on the path"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(5, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def p2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 1)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 1)
        skillsService.createProjectAndSubjectAndSkills(p2, proj2_subj, proj2_skills)
        skillsService.shareSkill(p2.projectId, proj2_skills[0].skillId, p1.projectId)

        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[0].skillId, p1.projectId, p1Skills[2].skillId)
        when:
        def result = skillsService.vadlidateLearningPathPrerequisite(p1.projectId, p1Skills[2].skillId, p2.projectId, proj2_skills[0].skillId)
        then:
        proj2_skills[0].skillId == p1Skills[0].skillId
        result.possible == true
    }

    def "badge[skill1] -> badge (skill 2) + skill2 -> skill1 circular dependency"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(5, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge1 = SkillsFactory.createBadge(1, 1)
        skillsService.createBadge(badge1)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[0].skillId])
        badge1.enabled = true
        skillsService.createBadge(badge1)

        def badge2 = SkillsFactory.createBadge(1, 2)
        skillsService.createBadge(badge2)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[1].skillId])
        badge2.enabled = true
        skillsService.createBadge(badge2)

        skillsService.addLearningPathPrerequisite(p1.projectId, badge1.badgeId, p1.projectId, badge2.badgeId)
        when:
        def result = skillsService.vadlidateLearningPathPrerequisite(p1.projectId, p1Skills[1].skillId, p1.projectId, p1Skills[0].skillId)
        then:
        result.possible == false
        result.failureType == DependencyCheckResult.FailureType.CircularLearningPath.toString()
        result.violatingSkillInBadgeId == badge2.badgeId
        result.violatingSkillInBadgeName == badge2.name
        result.reason == "Discovered circular prerequisite [Skill:skill2 -> Skill:skill1 -> Badge:badge1 -> Badge:badge2(Skill:skill2)]"
    }

    def "skill2 -> skill1 + badge[skill1] -> badge[skill 2] circular dependency"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(5, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge1 = SkillsFactory.createBadge(1, 1)
        skillsService.createBadge(badge1)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[0].skillId])
        badge1.enabled = true
        skillsService.createBadge(badge1)

        def badge2 = SkillsFactory.createBadge(1, 2)
        skillsService.createBadge(badge2)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[1].skillId])
        badge2.enabled = true
        skillsService.createBadge(badge2)

        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[1].skillId, p1.projectId, p1Skills[0].skillId)
        when:
        def result = skillsService.vadlidateLearningPathPrerequisite(p1.projectId, badge1.badgeId, p1.projectId, badge2.badgeId)
        then:
        result.possible == false
        result.failureType == DependencyCheckResult.FailureType.BadgeSkillIsAlreadyOnPath.toString()
        !result.violatingSkillInBadgeId
        !result.violatingSkillInBadgeName
        result.violatingSkillId == p1Skills[0].skillId
        result.violatingSkillName == p1Skills[0].name
        result.reason == "Badge [Test Badge 1] has skill [Test Skill 1] which already exists on the Learning Path."
    }

    def "not able to add badge(skill1)-> skill3 -> skill1"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(5, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge = SkillsFactory.createBadge()
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge.badgeId, skillId: p1Skills[0].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge.badgeId, skillId: p1Skills[1].skillId])
        badge.enabled = true
        skillsService.createBadge(badge)

        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[2].skillId, p1.projectId, badge.badgeId)
        when:
        def result = skillsService.vadlidateLearningPathPrerequisite(p1.projectId, p1Skills[0].skillId, p1.projectId,  p1Skills[2].skillId)

        then:
        result.possible == false
        result.failureType == DependencyCheckResult.FailureType.CircularLearningPath.toString()
        result.violatingSkillInBadgeId == badge.badgeId
        result.violatingSkillInBadgeName == badge.name
        !result.violatingSkillId
        !result.violatingSkillName
        result.reason == "Discovered circular prerequisite [Skill:skill1 -> Skill:skill3 -> Badge:badge1(Skill:skill1)]"
    }

    def "not able to add badge(skill1)-> skill3 -> skill1; badge->skill learning path item is added second"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(5, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge = SkillsFactory.createBadge()
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge.badgeId, skillId: p1Skills[0].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge.badgeId, skillId: p1Skills[1].skillId])
        badge.enabled = true
        skillsService.createBadge(badge)

        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[0].skillId, p1.projectId,  p1Skills[2].skillId)
        when:
        def result = skillsService.vadlidateLearningPathPrerequisite(p1.projectId, p1Skills[2].skillId, p1.projectId, badge.badgeId)

        then:
        result.possible == false
        result.failureType == DependencyCheckResult.FailureType.CircularLearningPath.toString()
        !result.violatingSkillInBadgeId
        !result.violatingSkillInBadgeName
        !result.violatingSkillId
        !result.violatingSkillName
        result.reason == "Discovered circular prerequisite [Skill:skill3 -> Badge:badge1(Skill:skill1) -> Skill:skill3]"
    }

    def "not able to add skill1 -> skill3 -> badge[skill1]"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(5, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge = SkillsFactory.createBadge()
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge.badgeId, skillId: p1Skills[0].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge.badgeId, skillId: p1Skills[1].skillId])
        badge.enabled = true
        skillsService.createBadge(badge)

        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[0].skillId, p1.projectId,  p1Skills[2].skillId)

        when:
        def result = skillsService.vadlidateLearningPathPrerequisite(p1.projectId, p1Skills[2].skillId,  p1.projectId, badge.badgeId)

        then:
        result.possible == false
        result.failureType == DependencyCheckResult.FailureType.CircularLearningPath.toString()
        !result.violatingSkillInBadgeId
        !result.violatingSkillInBadgeName
        !result.violatingSkillId
        !result.violatingSkillName
        result.reason == "Discovered circular prerequisite [Skill:skill3 -> Badge:badge1(Skill:skill1) -> Skill:skill3]"
    }

    def "not able to add skill1 -> skill3 -> badge(skill1); skill1->skill3 route is added second"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(5, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge = SkillsFactory.createBadge()
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge.badgeId, skillId: p1Skills[0].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge.badgeId, skillId: p1Skills[1].skillId])
        badge.enabled = true
        skillsService.createBadge(badge)

        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[2].skillId,  p1.projectId, badge.badgeId)

        when:
        def result = skillsService.vadlidateLearningPathPrerequisite(p1.projectId, p1Skills[0].skillId, p1.projectId,  p1Skills[2].skillId)

        then:
        result.possible == false
        result.failureType == DependencyCheckResult.FailureType.CircularLearningPath.toString()
        result.violatingSkillInBadgeId == badge.badgeId
        result.violatingSkillInBadgeName == badge.name
        !result.violatingSkillId
        !result.violatingSkillName
        result.reason == "Discovered circular prerequisite [Skill:skill1 -> Skill:skill3 -> Badge:badge1(Skill:skill1)]"
    }

    def "skills can be belong to multiple badges"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(4, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge1 = SkillsFactory.createBadge(1, 1)
        skillsService.createBadge(badge1)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[2].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[3].skillId])

        def badge2 = SkillsFactory.createBadge(1, 2)
        skillsService.createBadge(badge2)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[0].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[3].skillId])

        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[3].skillId,  p1Skills[2].skillId)

        when:
        def result = skillsService.vadlidateLearningPathPrerequisite(p1.projectId, p1Skills[1].skillId, p1.projectId,  p1Skills[0].skillId)

        then:
        result.possible == true
    }

    def "skill-only learning path shall work even if skills belong to badges that theoretically would cause circular path if any of those badges were on the learning path"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(6, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge1 = createBadge(1, 1)
        skillsService.createBadge(badge1)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[1].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[2].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[5].skillId])
        badge1.enabled = true
        skillsService.createBadge(badge1)

        def badge2 = createBadge(1, 2)
        skillsService.createBadge(badge2)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[3].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[4].skillId])
        badge2.enabled = true
        skillsService.createBadge(badge2)

        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[2].skillId, p1Skills[3].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[4].skillId, p1Skills[5].skillId)

        when:
        def result = skillsService.vadlidateLearningPathPrerequisite(p1.projectId, p1Skills[0].skillId, p1.projectId,  p1Skills[1].skillId)

        then:
        result.possible == true
    }
}
