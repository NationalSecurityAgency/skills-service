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
package skills.intTests.clientDisplay

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory

class SkillsDescriptionSpec extends DefaultIntSpec {

    void "save for the description with <del> should not remove the tag"(){
        skillsService.createProject(SkillsFactory.createProject(1))
        skillsService.createSubject(SkillsFactory.createSubject(1, 1))

        def skill = SkillsFactory.createSkill(1, 1, 1)
        String desc = "<em><del>(U) one **two** three</del></em>"
        skill.description = desc
        skillsService.createSkill(skill)

        when:
        def res = skillsService.getSkill(skill)
        then:
        res.description == desc
    }

    void "result should be empty if there are not descriptions at all"(){
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj1 = SkillsFactory.createSubject(1, 1)
        def proj1_subj2 = SkillsFactory.createSubject(1, 2)
        List<Map> proj1_subj1_skills = SkillsFactory.createSkills(3, 1, 1)
        List<Map> proj1_subj2_skills = SkillsFactory.createSkills(3, 1, 2)

        proj1_subj2_skills.each {
            it.description = null
            it.helpUrl = null
        }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj1)
        skillsService.createSkills(proj1_subj1_skills)
        skillsService.createSubject(proj1_subj2)
        skillsService.createSkills(proj1_subj2_skills)

        when:
        def res = skillsService.getSubjectDescriptions(proj1.projectId, proj1_subj2.subjectId)
        then:
        res.each {
            assert !it.description
            assert !it.href
        }
    }

    void "achievedOn is populated for the achieved skills under a subject"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj1 = SkillsFactory.createSubject(1, 1)
        def proj1_subj2 = SkillsFactory.createSubject(1, 2)
        List<Map> proj1_subj1_skills = SkillsFactory.createSkills(3, 1, 1, 100)
        List<Map> proj1_subj2_skills = SkillsFactory.createSkills(3, 1, 2)

        proj1_subj2_skills.each {
            it.description = null
            it.helpUrl = null
        }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj1)
        skillsService.createSkills(proj1_subj1_skills)
        skillsService.createSubject(proj1_subj2)
        skillsService.createSkills(proj1_subj2_skills)

        String user = "user1"
        def addSkillRes = skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_subj1_skills[0].skillId], user, new Date())
        when:
        def res = skillsService.getSubjectDescriptions(proj1.projectId, proj1_subj2.subjectId, user)
        def res1 = skillsService.getSubjectDescriptions(proj1.projectId, proj1_subj1.subjectId, user)
        then:
        addSkillRes.body.completed.find { it.type == "Skill" }
        res1.find({ it.achievedOn }).skillId == proj1_subj1_skills[0].skillId
        res1.findAll { it.skillId != proj1_subj1_skills[0].skillId }.each {
            assert !it.achievedOn
        }
        res.each {
            assert !it.achievedOn
        }
    }

    void "achievedOn is populated for the achieved skills under a badge"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj1 = SkillsFactory.createSubject(1, 1)
        def proj1_subj2 = SkillsFactory.createSubject(1, 2)
        List<Map> proj1_subj1_skills = SkillsFactory.createSkills(3, 1, 1, 100)
        List<Map> proj1_subj2_skills = SkillsFactory.createSkills(3, 1, 2)

        proj1_subj2_skills.each {
            it.description = null
            it.helpUrl = null
        }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj1)
        skillsService.createSkills(proj1_subj1_skills)
        skillsService.createSubject(proj1_subj2)
        skillsService.createSkills(proj1_subj2_skills)

        Map badge1 = SkillsFactory.createBadge(1, 1)
        skillsService.createBadge(badge1)
        proj1_subj1_skills.each {
            skillsService.assignSkillToBadge(proj1.projectId, badge1.badgeId, it.skillId)
        }

        String user = "user1"
        def addSkillRes = skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_subj1_skills[0].skillId], user, new Date())
        when:
        def res1 = skillsService.getBadgeDescriptions(proj1.projectId, badge1.badgeId, false, user)
        then:
        addSkillRes.body.completed.find { it.type == "Skill" }
        res1.find({ it.achievedOn }).skillId == proj1_subj1_skills[0].skillId
        res1.findAll { it.skillId != proj1_subj1_skills[0].skillId }.each {
            assert !it.achievedOn
        }
    }

    void "achievedOn is populated for the achieved skills under a global badge"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj1 = SkillsFactory.createSubject(1, 1)
        def proj1_subj2 = SkillsFactory.createSubject(1, 2)
        List<Map> proj1_subj1_skills = SkillsFactory.createSkills(3, 1, 1, 100)
        List<Map> proj1_subj2_skills = SkillsFactory.createSkills(3, 1, 2)

        proj1_subj2_skills.each {
            it.description = null
            it.helpUrl = null
        }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj1)
        skillsService.createSkills(proj1_subj1_skills)
        skillsService.createSubject(proj1_subj2)
        skillsService.createSkills(proj1_subj2_skills)

        Map badge1 = SkillsFactory.createBadge(1, 1)
        skillsService.createGlobalBadge(badge1)
        proj1_subj1_skills.each {
            skillsService.assignSkillToGlobalBadge([projectId: proj1.projectId, badgeId: badge1.badgeId, skillId: it.skillId])
        }

        String user = "user1"
        def addSkillRes = skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_subj1_skills[0].skillId], user, new Date())
        when:
        def res1 = skillsService.getBadgeDescriptions(proj1.projectId, badge1.badgeId, true, user)
        then:
        addSkillRes.body.completed.find { it.type == "Skill" }
        res1.find({ it.achievedOn }).skillId == proj1_subj1_skills[0].skillId
        res1.findAll { it.skillId != proj1_subj1_skills[0].skillId }.each {
            assert !it.achievedOn
        }
    }

    void "subject has no skills - no descriptions for you!"(){
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj1 = SkillsFactory.createSubject(1, 1)
        def proj1_subj2 = SkillsFactory.createSubject(1, 2)
        List<Map> proj1_subj1_skills = SkillsFactory.createSkills(3, 1, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj1)
        skillsService.createSkills(proj1_subj1_skills)
        skillsService.createSubject(proj1_subj2)

        when:
        def res = skillsService.getSubjectDescriptions(proj1.projectId, proj1_subj2.subjectId)
        then:
        !res
    }

    void "get descriptions for a subject"(){
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj1 = SkillsFactory.createSubject(1, 1)
        def proj1_subj2 = SkillsFactory.createSubject(1, 2)
        List<Map> proj1_subj1_skills = SkillsFactory.createSkills(3, 1, 1)
        List<Map> proj1_subj2_skills = SkillsFactory.createSkills(3, 1, 2)

        proj1_subj2_skills.each {
            it.description = "Desc [${it.skillId}]".toString()
            it.helpUrl = "http://${it.skillId}".toString()
        }

        proj1_subj2_skills[1].description = null

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj1)
        skillsService.createSkills(proj1_subj1_skills)
        skillsService.createSubject(proj1_subj2)
        skillsService.createSkills(proj1_subj2_skills)

        when:
        def res = skillsService.getSubjectDescriptions(proj1.projectId, proj1_subj2.subjectId).sort { it.skillId }
        then:
        res[0].description == "Desc [${proj1_subj2_skills[0].skillId}]".toString()
        res[0].href == "http://${proj1_subj2_skills[0].skillId}".toString()

        !res[1].description
        res[1].href == "http://${proj1_subj2_skills[1].skillId}".toString()

        res[2].description == "Desc [${proj1_subj2_skills[2].skillId}]".toString()
        res[2].href == "http://${proj1_subj2_skills[2].skillId}".toString()
    }

    void "descriptions should respect root url settings"(){
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj1 = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_subj1_skills = SkillsFactory.createSkills(3, 1, 1)

        proj1_subj1_skills.each {
            it.description = "Desc [${it.skillId}]".toString()
            it.helpUrl = "/${it.skillId}".toString()
        }


        skillsService.createProject(proj1)
        skillsService.changeSetting(proj1.projectId, "help.url.root", [projectId: proj1.projectId, setting: 'help.url.root', value:"https://fakeurl.foo"])
        skillsService.createSubject(proj1_subj1)
        skillsService.createSkills(proj1_subj1_skills)

        when:
        def res = skillsService.getSubjectDescriptions(proj1.projectId, proj1_subj1.subjectId).sort { it.skillId }
        then:
        res.each {
            assert it.href.startsWith("https://fakeurl.foo")
        }
    }

    void "badge has no skills - no descriptions for you!"(){
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj1 = SkillsFactory.createSubject(1, 1)
        def proj1_subj2 = SkillsFactory.createSubject(1, 2)
        List<Map> proj1_subj1_skills = SkillsFactory.createSkills(3, 1, 1)

        Map badge1 = SkillsFactory.createBadge(1, 1 )

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj1)
        skillsService.createSkills(proj1_subj1_skills)
        skillsService.createSubject(proj1_subj2)
        skillsService.createBadge(badge1)

        when:
        def res = skillsService.getBadgeDescriptions(proj1.projectId, badge1.badgeId)
        then:
        !res
    }

    void "global badge has no skills - no descriptions for you!"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj1 = SkillsFactory.createSubject(1, 1)
        def proj1_subj2 = SkillsFactory.createSubject(1, 2)
        List<Map> proj1_subj1_skills = SkillsFactory.createSkills(3, 1, 1)

        Map badge1 = SkillsFactory.createBadge(1, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj1)
        skillsService.createSkills(proj1_subj1_skills)
        skillsService.createSubject(proj1_subj2)
        skillsService.createGlobalBadge(badge1)
        skillsService.assignProjectLevelToGlobalBadge(projectId: proj1.projectId, badgeId: badge1.badgeId, level: "1")

        when:
        def res = skillsService.getBadgeDescriptions(proj1.projectId, badge1.badgeId, true)
        then:
        !res
    }

    void "get descriptions for a badge"(){
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj1 = SkillsFactory.createSubject(1, 1)
        def proj1_subj2 = SkillsFactory.createSubject(1, 2)
        List<Map> proj1_subj1_skills = SkillsFactory.createSkills(3, 1, 1)
        List<Map> proj1_subj2_skills = SkillsFactory.createSkills(3, 1, 2)

        proj1_subj2_skills.each {
            it.description = "Desc [${it.skillId}]".toString()
            it.helpUrl = "http://${it.skillId}".toString()
        }

        proj1_subj2_skills[1].description = null

        Map badge1 = SkillsFactory.createBadge(1, 1 )
        Map badge2 = SkillsFactory.createBadge(1, 2)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj1)
        skillsService.createSkills(proj1_subj1_skills)
        skillsService.createSubject(proj1_subj2)
        skillsService.createSkills(proj1_subj2_skills)

        skillsService.createBadge(badge1)
        skillsService.createBadge(badge2)

        proj1_subj1_skills.each {
            skillsService.assignSkillToBadge(proj1.projectId, badge1.badgeId, it.skillId)
        }

        proj1_subj2_skills.each {
            skillsService.assignSkillToBadge(proj1.projectId, badge2.badgeId, it.skillId)
        }

        when:
        def res = skillsService.getBadgeDescriptions(proj1.projectId, badge2.badgeId).sort { it.skillId }
        then:
        res[0].description == "Desc [${proj1_subj2_skills[0].skillId}]".toString()
        res[0].href == "http://${proj1_subj2_skills[0].skillId}".toString()

        !res[1].description
        res[1].href == "http://${proj1_subj2_skills[1].skillId}".toString()

        res[2].description == "Desc [${proj1_subj2_skills[2].skillId}]".toString()
        res[2].href == "http://${proj1_subj2_skills[2].skillId}".toString()
    }

    void "get descriptions for a global badge"(){
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj1 = SkillsFactory.createSubject(1, 1)
        def proj1_subj2 = SkillsFactory.createSubject(1, 2)
        List<Map> proj1_subj1_skills = SkillsFactory.createSkills(3, 1, 1)
        List<Map> proj1_subj2_skills = SkillsFactory.createSkills(3, 1, 2)

        proj1_subj2_skills.each {
            it.description = "Desc [${it.skillId}]".toString()
            it.helpUrl = "http://${it.skillId}".toString()
        }

        proj1_subj2_skills[1].description = null

        Map badge1 = SkillsFactory.createBadge(1, 1 )
        Map badge2 = SkillsFactory.createBadge(1, 2)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj1)
        skillsService.createSkills(proj1_subj1_skills)
        skillsService.createSubject(proj1_subj2)
        skillsService.createSkills(proj1_subj2_skills)

        skillsService.createGlobalBadge(badge1)
        skillsService.createGlobalBadge(badge2)

        proj1_subj1_skills.each {
            skillsService.assignSkillToGlobalBadge([projectId: proj1.projectId, badgeId: badge1.badgeId, skillId: it.skillId])
        }

        proj1_subj2_skills.each {
            skillsService.assignSkillToGlobalBadge([projectId: proj1.projectId, badgeId: badge2.badgeId, skillId: it.skillId])
        }

        when:
        def res = skillsService.getBadgeDescriptions(proj1.projectId, badge2.badgeId, true).sort { it.skillId }
        then:
        res[0].description == "Desc [${proj1_subj2_skills[0].skillId}]".toString()
        res[0].href == "http://${proj1_subj2_skills[0].skillId}".toString()

        !res[1].description
        res[1].href == "http://${proj1_subj2_skills[1].skillId}".toString()

        res[2].description == "Desc [${proj1_subj2_skills[2].skillId}]".toString()
        res[2].href == "http://${proj1_subj2_skills[2].skillId}".toString()
    }

    void "get descriptions for a SkillsGroup"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj1 = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_subj1_skills = SkillsFactory.createSkills(7, 1, 1)
        def regularSkills = proj1_subj1_skills.subList(0, 3)
        def skillsGroup = proj1_subj1_skills[3]
        skillsGroup.type = 'SkillsGroup'
        def childSkills = proj1_subj1_skills.subList(4, 7)

        proj1_subj1_skills.each {
            it.description = "Desc [${it.skillId}]".toString()
            it.helpUrl = "http://${it.skillId}".toString()
        }

        proj1_subj1_skills[1].description = null
        proj1_subj1_skills[5].description = null

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj1)
        skillsService.createSkills(regularSkills)
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId
        childSkills.each { skill ->
            skillsService.assignSkillToSkillsGroup(skillsGroupId, skill)
        }
        skillsGroup.enabled = 'true'
        skillsService.updateSkill(skillsGroup, null)

        when:
        def res = skillsService.getSubjectDescriptions(proj1.projectId, proj1_subj1.subjectId).sort { it.skillId }

        then:
        res.size() == 7

        res[0].description == "Desc [${proj1_subj1_skills[0].skillId}]".toString()
        res[0].href == "http://${proj1_subj1_skills[0].skillId}".toString()

        !res[1].description
        res[1].href == "http://${proj1_subj1_skills[1].skillId}".toString()

        res[2].description == "Desc [${proj1_subj1_skills[2].skillId}]".toString()
        res[2].href == "http://${proj1_subj1_skills[2].skillId}".toString()

        res[3].description == "Desc [${proj1_subj1_skills[3].skillId}]".toString()
        res[3].href == "http://${proj1_subj1_skills[3].skillId}".toString()

        res[4].description == "Desc [${proj1_subj1_skills[4].skillId}]".toString()
        res[4].href == "http://${proj1_subj1_skills[4].skillId}".toString()

        !res[5].description
        res[5].href == "http://${proj1_subj1_skills[5].skillId}".toString()

        res[6].description == "Desc [${proj1_subj1_skills[6].skillId}]".toString()
        res[6].href == "http://${proj1_subj1_skills[6].skillId}".toString()
    }

    void "disabled SkillsGroup's descriptions are not included"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj1 = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_subj1_skills = SkillsFactory.createSkills(7, 1, 1)
        def regularSkills = proj1_subj1_skills.subList(0, 3)
        def skillsGroup = proj1_subj1_skills[3]
        skillsGroup.type = 'SkillsGroup'
        skillsGroup.enabled = 'false'
        def childSkills = proj1_subj1_skills.subList(4, 7)

        proj1_subj1_skills.each {
            it.description = "Desc [${it.skillId}]".toString()
            it.helpUrl = "http://${it.skillId}".toString()
        }

        proj1_subj1_skills[1].description = null
        proj1_subj1_skills[5].description = null

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj1)
        skillsService.createSkills(regularSkills)
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId
        childSkills.each { skill ->
            skill.enabled = 'false'
            skillsService.assignSkillToSkillsGroup(skillsGroupId, skill)
        }

        when:
        def res = skillsService.getSubjectDescriptions(proj1.projectId, proj1_subj1.subjectId).sort { it.skillId }

        then:
        res.size() == 3

        res[0].description == "Desc [${proj1_subj1_skills[0].skillId}]".toString()
        res[0].href == "http://${proj1_subj1_skills[0].skillId}".toString()

        !res[1].description
        res[1].href == "http://${proj1_subj1_skills[1].skillId}".toString()

        res[2].description == "Desc [${proj1_subj1_skills[2].skillId}]".toString()
        res[2].href == "http://${proj1_subj1_skills[2].skillId}".toString()
    }

    void "badge's skills have no descriptions"(){
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj1 = SkillsFactory.createSubject(1, 1)
        def proj1_subj2 = SkillsFactory.createSubject(1, 2)
        List<Map> proj1_subj1_skills = SkillsFactory.createSkills(3, 1, 1)
        List<Map> proj1_subj2_skills = SkillsFactory.createSkills(3, 1, 2)

        proj1_subj2_skills.each {
            it.description = null
            it.helpUrl = null
        }

        proj1_subj2_skills[1].description = null

        Map badge1 = SkillsFactory.createBadge(1, 1 )
        Map badge2 = SkillsFactory.createBadge(1, 2)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj1)
        skillsService.createSkills(proj1_subj1_skills)
        skillsService.createSubject(proj1_subj2)
        skillsService.createSkills(proj1_subj2_skills)

        skillsService.createBadge(badge1)
        skillsService.createBadge(badge2)

        proj1_subj1_skills.each {
            skillsService.assignSkillToBadge(proj1.projectId, badge1.badgeId, it.skillId)
        }

        proj1_subj2_skills.each {
            skillsService.assignSkillToBadge(proj1.projectId, badge2.badgeId, it.skillId)
        }

        when:
        def res = skillsService.getBadgeDescriptions(proj1.projectId, badge2.badgeId)
        then:
        res.each {
            assert !it.description
            assert !it.href
        }
    }
}
