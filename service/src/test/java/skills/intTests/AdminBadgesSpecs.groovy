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
package skills.intTests

import groovy.json.JsonOutput
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory

import static skills.intTests.utils.SkillsFactory.createProject
import static skills.intTests.utils.SkillsFactory.createSkills
import static skills.intTests.utils.SkillsFactory.createSubject

class AdminBadgesSpecs extends DefaultIntSpec {

    void "get badge that have skills assigned"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(4)
        def badge = SkillsFactory.createBadge()

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)
        skillsService.addLearningPathPrerequisite(proj.projectId, skills.get(0).skillId, skills.get(1).skillId)
        skillsService.addLearningPathPrerequisite(proj.projectId, skills.get(0).skillId, skills.get(2).skillId)

        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills.get(0).skillId])
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills.get(3).skillId])

        when:
        def res = skillsService.getBadge(badge)

        then:
        res
        res.numSkills == 2
        res.requiredSkills.size() == 2
        res.requiredSkills.collect { it.skillId }.sort() == ["skill1", "skill4"]
        res.totalPoints == 20
        res.badgeId == badge.badgeId
        res.name == badge.name
        res.projectId == proj.projectId
    }

    void "assign skills to inactive badge"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(4)
        def badge = SkillsFactory.createBadge()

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)
        skillsService.addLearningPathPrerequisite(proj.projectId, skills.get(0).skillId, skills.get(1).skillId)
        skillsService.addLearningPathPrerequisite(proj.projectId, skills.get(0).skillId, skills.get(2).skillId)

        badge.enabled = false
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills.get(0).skillId])
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills.get(3).skillId])

        when:
        def res = skillsService.getBadge(badge)

        then:
        res
        res.numSkills == 2
        res.requiredSkills.size() == 2
        res.requiredSkills.collect { it.skillId }.sort() == ["skill1", "skill4"]
        res.totalPoints == 20
        res.badgeId == badge.badgeId
        res.name == badge.name
        res.projectId == proj.projectId
        res.enabled == 'false'
    }

    void "remove skills from a badge"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(4)
        def badge = SkillsFactory.createBadge()

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills.get(0).skillId])
        Map badgeSkillDeclaration = [projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills.get(3).skillId]
        skillsService.assignSkillToBadge(badgeSkillDeclaration)

        when:
        def res = skillsService.getBadge(badge)
        skillsService.removeSkillFromBadge(badgeSkillDeclaration)
        def resAfterDeletion = skillsService.getBadge(badge)
        then:
        res
        res.numSkills == 2
        res.requiredSkills.collect { it.skillId }.sort() == ["skill1", "skill4"]

        resAfterDeletion
        resAfterDeletion.numSkills == 1
        resAfterDeletion.requiredSkills.collect { it.skillId }.sort() == ["skill1"]
    }

    void "removing last unachieved skill from a badge achieves that badge"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(4, 1, 1, 100)
        def badge = SkillsFactory.createBadge()

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills.get(0).skillId])
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills.get(1).skillId])
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills.get(2).skillId])
        badge.enabled = true
        skillsService.createBadge(badge)

        List<String> users = getRandomUsers(2)
        skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], users[0], new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(2).skillId], users[0], new Date())

        skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], users[1], new Date())

        when:
        def u1Summary_t0 = skillsService.getBadgeSummary(users[0], proj.projectId, badge.badgeId)
        skillsService.removeSkillFromBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills.get(1).skillId])
        def u1Summary_t1 = skillsService.getBadgeSummary(users[0], proj.projectId, badge.badgeId)
        def u2Summary_t1 = skillsService.getBadgeSummary(users[1], proj.projectId, badge.badgeId)
        then:
        u1Summary_t0.skills.size() == 3
        u1Summary_t0.skills[0].points == u1Summary_t0.skills[0].totalPoints
        u1Summary_t0.skills[1].points < u1Summary_t0.skills[1].totalPoints
        u1Summary_t0.skills[2].points == u1Summary_t0.skills[2].totalPoints
        !u1Summary_t0.badgeAchieved

        u1Summary_t1.skills.size() == 2
        u1Summary_t1.skills[0].points == u1Summary_t0.skills[0].totalPoints
        u1Summary_t1.skills[1].points == u1Summary_t0.skills[1].totalPoints
        u1Summary_t1.badgeAchieved

        u2Summary_t1.skills.size() == 2
        u2Summary_t1.skills[0].points == u1Summary_t0.skills[0].totalPoints
        u2Summary_t1.skills[1].points < u1Summary_t0.skills[1].totalPoints
        !u2Summary_t1.badgeAchieved
    }

    void "delete last unachieved skill that was assigned to a badge - achieves that badge"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(4, 1, 1, 100)
        def badge = SkillsFactory.createBadge()

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills.get(0).skillId])
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills.get(1).skillId])
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills.get(2).skillId])
        badge.enabled = true
        skillsService.createBadge(badge)

        List<String> users = getRandomUsers(2)
        skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], users[0], new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(2).skillId], users[0], new Date())

        skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], users[1], new Date())

        when:
        def u1Summary_t0 = skillsService.getBadgeSummary(users[0], proj.projectId, badge.badgeId)
        skillsService.deleteSkill(skills.get(1))
        def u1Summary_t1 = skillsService.getBadgeSummary(users[0], proj.projectId, badge.badgeId)
        def u2Summary_t1 = skillsService.getBadgeSummary(users[1], proj.projectId, badge.badgeId)
        then:
        u1Summary_t0.skills.size() == 3
        u1Summary_t0.skills[0].points == u1Summary_t0.skills[0].totalPoints
        u1Summary_t0.skills[1].points < u1Summary_t0.skills[1].totalPoints
        u1Summary_t0.skills[2].points == u1Summary_t0.skills[2].totalPoints
        !u1Summary_t0.badgeAchieved

        u1Summary_t1.skills.size() == 2
        u1Summary_t1.skills[0].points == u1Summary_t0.skills[0].totalPoints
        u1Summary_t1.skills[1].points == u1Summary_t0.skills[1].totalPoints
        u1Summary_t1.badgeAchieved

        u2Summary_t1.skills.size() == 2
        u2Summary_t1.skills[0].points == u1Summary_t0.skills[0].totalPoints
        u2Summary_t1.skills[1].points < u1Summary_t0.skills[1].totalPoints
        !u2Summary_t1.badgeAchieved
    }

    void "remove badge"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(4)
        def badge = SkillsFactory.createBadge()
        def badge1 = SkillsFactory.createBadge(1, 2)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        skillsService.createBadge(badge)
        skillsService.createBadge(badge1)

        when:
        def res = skillsService.getBadges(proj.projectId)
        skillsService.removeBadge(badge1)
        def resAfterDeletion = skillsService.getBadges(proj.projectId)
        then:
        res
        res.collect { it.badgeId }.sort() == [badge.badgeId, badge1.badgeId].sort()

        resAfterDeletion
        resAfterDeletion.collect { it.badgeId } == [badge.badgeId]
    }

    void "cannot disable a badge after it has been enabled"(){
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(4)
        def badge = SkillsFactory.createBadge()
        badge.enabled = 'true'

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge(projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills.get(0).skillId)
        skillsService.updateBadge(badge, badge.badgeId)  // can only enable after initial creation

        when:
        badge = skillsService.getBadge(badge)
        badge.enabled = 'false'
        skillsService.updateBadge(badge, badge.badgeId)

        then:
        Exception ex = thrown()
        ex.getMessage().contains("Once a Badge has been published, the only allowable value for enabled is [true]")
    }

    def "cannot enable badge with no skills"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(4)
        def badge = SkillsFactory.createBadge()
        badge.enabled = 'false'

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        when:
        skillsService.createBadge(badge)
        badge.enabled = 'true'
        skillsService.createBadge(badge)

        then:
        def ex = thrown(Exception)
    }

    def "cannot add skill to a badge that will cause circular learning path: skill -> badge"() {
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

        when:
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge3.badgeId, skillId: p1Skills[6].skillId])

        then:
        def ex = thrown(Exception)
        ex.message.contains("errorCode:LearningPathViolation")
        ex.message.contains("Adding skill [skill7] to badge [badge3] violates the Learning Path. Reason: Badge [Test Badge 3] has skill [Test Skill 7] which already exists on the Learning Path")
    }

    def "cannot add skill to a badge that will cause circular learning path: badge -> skill"() {
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

        skillsService.addLearningPathPrerequisite(p1.projectId, badge3.badgeId, badge1.badgeId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge3.badgeId, badge2.badgeId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[7].skillId, badge3.badgeId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[6].skillId, p1Skills[7].skillId)

        when:
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge3.badgeId, skillId: p1Skills[6].skillId])

        then:
        def ex = thrown(Exception)
        ex.message.contains("errorCode:LearningPathViolation")
        ex.message.contains("Adding skill [skill7] to badge [badge3] violates the Learning Path. Reason: Discovered circular prerequisite [Skill:skill8 -> Badge:badge3(Skill:skill7) -> Skill:skill8]")
    }

    def "cannot add skill to a badge that will cause circular learning path because same skill is a part of another badge in the learning path"() {
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

        skillsService.addLearningPathPrerequisite(p1.projectId, badge2.badgeId, badge1.badgeId)

        when:
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[3].skillId])

        then:
        def ex = thrown(Exception)
        ex.message.contains("errorCode:LearningPathViolation")
        ex.message.contains( "Adding skill [skill4] to badge [badge1] violates the Learning Path. Reason: Multiple badges on the same Learning path cannot have overlapping skills. Both badge [Test Badge 1] and [Test Badge 2] badge have [Test Skill 4] skill")
    }

    def "cannot add skill to a badge that will cause circular learning path because same skill is a part of another badge in the learning path: badge (skill added here) -> skill -> badge"() {
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

        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[9].skillId, badge1.badgeId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge2.badgeId, p1Skills[9].skillId)

        when:
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[3].skillId])

        then:
        def ex = thrown(Exception)
        ex.message.contains("errorCode:LearningPathViolation")
        ex.message.contains("Adding skill [skill4] to badge [badge1] violates the Learning Path. Reason: Multiple badges on the same Learning path cannot have overlapping skills. Both badge [Test Badge 1] and [Test Badge 2] badge have [Test Skill 4] skill.")
    }

    def "can add bonus award attributes to a badge"() {
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

        def badgeResult = skillsService.getBadge(p1.projectId, badge1.badgeId)
        assert badgeResult.awardAttrs == [ name: null, iconClass: null, numMinutes: null]

        badgeResult.awardAttrs = [ name: 'Test Badge', iconClass: 'abc', numMinutes: 60 ]

        when:
        skillsService.updateBadge(badgeResult, badge1.badgeId)
        def updatedBadgeResult = skillsService.getBadge(p1.projectId, badge1.badgeId)

        then:
        updatedBadgeResult.awardAttrs
        updatedBadgeResult.awardAttrs.name == "Test Badge"
        updatedBadgeResult.awardAttrs.iconClass == "abc"
        updatedBadgeResult.awardAttrs.numMinutes == 60

    }

    def "can remove bonus award attributes from a badge"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(10, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge1 = SkillsFactory.createBadge(1, 1)
        skillsService.createBadge(badge1)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[0].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[1].skillId])
        badge1.enabled = true
        badge1.awardAttrs = [ name: 'Test Badge', iconClass: 'abc', numMinutes: 60 ]
        skillsService.createBadge(badge1)

        def badgeResult = skillsService.getBadge(p1.projectId, badge1.badgeId)
        assert badgeResult.awardAttrs == [ name: 'Test Badge', iconClass: 'abc', numMinutes: 60 ]

        badgeResult.awardAttrs = [ name: null, iconClass: null, numMinutes: null ];

        when:
        skillsService.updateBadge(badgeResult, badge1.badgeId)
        def updatedBadgeResult = skillsService.getBadge(p1.projectId, badge1.badgeId)

        then:
        updatedBadgeResult.awardAttrs
        updatedBadgeResult.awardAttrs.name == null
        updatedBadgeResult.awardAttrs.iconClass == null
        updatedBadgeResult.awardAttrs.numMinutes == null
    }

    def "can edit bonus award attributes for a badge"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(10, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge1 = SkillsFactory.createBadge(1, 1)
        skillsService.createBadge(badge1)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[0].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[1].skillId])
        badge1.enabled = true
        badge1.awardAttrs = [ name: 'Test Badge', iconClass: 'abc', numMinutes: 60 ]
        skillsService.createBadge(badge1)

        def badgeResult = skillsService.getBadge(p1.projectId, badge1.badgeId)
        assert badgeResult.awardAttrs == [ name: 'Test Badge', iconClass: 'abc', numMinutes: 60 ]

        badgeResult.awardAttrs = [ name: 'Speedy Finish Award', iconClass: 'def', numMinutes: 600 ];

        when:
        skillsService.updateBadge(badgeResult, badge1.badgeId)
        def updatedBadgeResult = skillsService.getBadge(p1.projectId, badge1.badgeId)

        then:
        updatedBadgeResult.awardAttrs
        updatedBadgeResult.awardAttrs.name == "Speedy Finish Award"
        updatedBadgeResult.awardAttrs.iconClass == "def"
        updatedBadgeResult.awardAttrs.numMinutes == 600
    }

    def "can not exceed maximum minutes for badge"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(10, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge1 = SkillsFactory.createBadge(1, 1)
        badge1.awardAttrs = [ name: 'Test Badge', iconClass: 'abc', numMinutes: 1000000]

        when:
        skillsService.createBadge(badge1)

        then:
        def ex = thrown(Exception)
        ex.message.contains("numMinutes must be <= 525600")

    }

    void "can not remove last skill from an active badge"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(4)
        def badge = SkillsFactory.createBadge()

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills.get(0).skillId])
        badge.enabled = true
        skillsService.createBadge(badge)

        when:
        skillsService.removeSkillFromBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills.get(0).skillId])

        then:
        SkillsClientException e = thrown()
        e.message.contains("Can not remove skill from badge [badge1] as it is live with only a single skill")
    }

    void "can remove last skill from an inactive badge"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(4)
        def badge = SkillsFactory.createBadge()

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills.get(0).skillId])
        badge.enabled = false
        skillsService.createBadge(badge)

        when:
        def res = skillsService.getBadge(badge)
        skillsService.removeSkillFromBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills.get(0).skillId])
        def resAfterRemoval = skillsService.getBadge(badge)

        then:
        res.numSkills == 1
        resAfterRemoval.numSkills == 0
    }
}
