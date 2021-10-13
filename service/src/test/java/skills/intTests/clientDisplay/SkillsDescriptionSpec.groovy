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

import groovy.json.JsonOutput
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.storage.model.SkillDef

class SkillsDescriptionSpec extends DefaultIntSpec {

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
        SkillsService supervisorService = createSupervisor()
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
        supervisorService.createGlobalBadge(badge1)
        proj1_subj1_skills.each {
            supervisorService.assignSkillToGlobalBadge([projectId: proj1.projectId, badgeId: badge1.badgeId, skillId: it.skillId])
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
        SkillsService supervisorService = createSupervisor()

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj1 = SkillsFactory.createSubject(1, 1)
        def proj1_subj2 = SkillsFactory.createSubject(1, 2)
        List<Map> proj1_subj1_skills = SkillsFactory.createSkills(3, 1, 1)

        Map badge1 = SkillsFactory.createBadge(1, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj1)
        skillsService.createSkills(proj1_subj1_skills)
        skillsService.createSubject(proj1_subj2)
        supervisorService.createGlobalBadge(badge1)

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
        SkillsService supervisorService = createSupervisor()

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

        supervisorService.createGlobalBadge(badge1)
        supervisorService.createGlobalBadge(badge2)

        proj1_subj1_skills.each {
            supervisorService.assignSkillToGlobalBadge([projectId: proj1.projectId, badgeId: badge1.badgeId, skillId: it.skillId])
        }

        proj1_subj2_skills.each {
            supervisorService.assignSkillToGlobalBadge([projectId: proj1.projectId, badgeId: badge2.badgeId, skillId: it.skillId])
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
        skillsGroup.enabled = 'true'
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

    void "self reporting info"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj1 = SkillsFactory.createSubject(1, 1)
        def proj1_subj2 = SkillsFactory.createSubject(1, 2)
        List<Map> proj1_subj1_skills = SkillsFactory.createSkills(4, 1, 1, 100)
        List<Map> proj1_subj2_skills = SkillsFactory.createSkills(3, 1, 2)

        proj1_subj1_skills[0].selfReportingType = 'HonorSystem'
        proj1_subj1_skills[1].selfReportingType = 'Approval'
        proj1_subj1_skills[3].selfReportingType = 'Approval'

        proj1_subj2_skills[1].selfReportingType = 'HonorSystem'
        proj1_subj2_skills[2].selfReportingType = 'Approval'

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
        Date date = new Date()
        Date date1 = new Date() - 1
        Date date2 = new Date() - 2
        def addSkillRes = skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_subj1_skills[0].skillId], user, date)
        def addSkillRes1 = skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_subj1_skills[1].skillId], user, date1)
        def addSkillRes2 = skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_subj1_skills[3].skillId], user, date2)
        def approvalsEndpointRes = skillsService.getApprovals(proj1.projectId, 5, 1, 'requestedOn', false)
        List<Integer> ids = approvalsEndpointRes.data.findAll { it.skillId == proj1_subj1_skills[3].skillId }.collect { it.id }
        skillsService.rejectSkillApprovals(proj1.projectId, ids, "rejection message")

        when:
        def res1 = skillsService.getSubjectDescriptions(proj1.projectId, proj1_subj1.subjectId, user).sort { it.skillId }
        def res2 = skillsService.getSubjectDescriptions(proj1.projectId, proj1_subj2.subjectId, user).sort { it.skillId }

        println JsonOutput.prettyPrint(JsonOutput.toJson(res1))
        println JsonOutput.prettyPrint(JsonOutput.toJson(res2))
        then:
        addSkillRes.body.skillApplied
        !addSkillRes1.body.skillApplied
        !addSkillRes2.body.skillApplied

        res1.size() == 4
        res1.get(0).skillId == proj1_subj1_skills[0].skillId
        res1.get(0).selfReporting.enabled
        res1.get(0).selfReporting.type == SkillDef.SelfReportingType.HonorSystem.toString()
        !res1.get(0).selfReporting.requestedOn
        !res1.get(0).selfReporting.rejectedOn
        !res1.get(0).selfReporting.rejectionMsg

        res1.get(1).skillId == proj1_subj1_skills[1].skillId
        res1.get(1).selfReporting.enabled
        res1.get(1).selfReporting.type == SkillDef.SelfReportingType.Approval.toString()
        res1.get(1).selfReporting.requestedOn == date1.time
        !res1.get(1).selfReporting.rejectedOn
        !res1.get(1).selfReporting.rejectionMsg

        res1.get(2).skillId == proj1_subj1_skills[2].skillId
        !res1.get(2).selfReporting.enabled
        !res1.get(2).selfReporting.type
        !res1.get(2).selfReporting.requestedOn
        !res1.get(2).selfReporting.rejectedOn
        !res1.get(2).selfReporting.rejectionMsg

        res1.get(3).skillId == proj1_subj1_skills[3].skillId
        res1.get(3).selfReporting.enabled
        res1.get(3).selfReporting.type == SkillDef.SelfReportingType.Approval.toString()
        res1.get(3).selfReporting.requestedOn == date2.time
        new Date(res1.get(3).selfReporting.rejectedOn).format('yyyy-MM-dd') == new Date().format('yyyy-MM-dd')
        res1.get(3).selfReporting.rejectionMsg == "rejection message"

        res2.size() == 3
        res2.get(0).skillId == proj1_subj2_skills[0].skillId
        !res2.get(0).selfReporting.enabled
        !res2.get(0).selfReporting.type
        !res2.get(0).selfReporting.requestedOn
        !res2.get(0).selfReporting.rejectedOn
        !res2.get(0).selfReporting.rejectionMsg

        res2.get(1).skillId == proj1_subj2_skills[1].skillId
        res2.get(1).selfReporting.enabled
        res2.get(1).selfReporting.type == SkillDef.SelfReportingType.HonorSystem.toString()
        !res2.get(1).selfReporting.requestedOn
        !res2.get(1).selfReporting.rejectedOn
        !res2.get(1).selfReporting.rejectionMsg

        res2.get(2).skillId == proj1_subj2_skills[2].skillId
        res2.get(2).selfReporting.enabled
        res2.get(2).selfReporting.type == SkillDef.SelfReportingType.Approval.toString()
        !res2.get(2).selfReporting.requestedOn
        !res2.get(2).selfReporting.rejectedOn
        !res2.get(2).selfReporting.rejectionMsg
    }

    def "when a self-reporting skill has a history of approvals only load the latest approval info - latest rejection"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(5,)
        skills.each {
            it.pointIncrement = 200
            it.numPerformToCompletion = 200
            it.selfReportingType = SkillDef.SelfReportingType.Approval
        }

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        List<Date> dates = (0..5).collect { new Date() - it }
        List<String> users = getRandomUsers(2)

        when:
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], dates[3], "approve 1")
        def approvals = skillsService.getApprovals(proj.projectId, 7, 1, 'requestedOn', false)
        skillsService.approve(proj.projectId, approvals.data.collect { it.id })

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], dates[2], "reject 1")
        approvals = skillsService.getApprovals(proj.projectId, 7, 1, 'requestedOn', false)
        skillsService.rejectSkillApprovals(proj.projectId, approvals.data.collect { it.id })

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], dates[1], "approve 2")
        approvals = skillsService.getApprovals(proj.projectId, 7, 1, 'requestedOn', false)
        skillsService.approve(proj.projectId, approvals.data.collect { it.id })

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], dates[0], "reject 2")
        approvals = skillsService.getApprovals(proj.projectId, 7, 1, 'requestedOn', false)
        skillsService.rejectSkillApprovals(proj.projectId, approvals.data.collect { it.id }, 'last rejection')

        // 2nd skill
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[1].skillId], users[0], dates[3], "approve 1")
        approvals = skillsService.getApprovals(proj.projectId, 7, 1, 'requestedOn', false)
        skillsService.approve(proj.projectId, approvals.data.collect { it.id })


        def approvalsHistoryUser1 = skillsService.getApprovalsHistory(proj.projectId, 10, 1, 'requestedOn', false, '',  '', '')

        def res1 = skillsService.getSubjectDescriptions(proj.projectId, subj.subjectId, users[0]).sort { it.skillId }

        then:
        approvalsHistoryUser1.totalCount == 5

        res1.size() == 5
        res1.get(0).skillId == skills[0].skillId
        res1.get(0).selfReporting.enabled
        res1.get(0).selfReporting.rejectionMsg == 'last rejection'
        res1.get(0).selfReporting.requestedOn == dates[0].time
        res1.get(0).selfReporting.rejectedOn

        res1.get(1).skillId == skills[1].skillId
        res1.get(1).selfReporting.enabled
        !res1.get(1).selfReporting.rejectionMsg
        !res1.get(1).selfReporting.rejectedOn
        !res1.get(1).selfReporting.requestedOn


        res1.get(2).skillId == skills[2].skillId
        res1.get(2).selfReporting.enabled
        !res1.get(2).selfReporting.rejectionMsg
        !res1.get(2).selfReporting.rejectedOn
        !res1.get(2).selfReporting.requestedOn

        res1.get(3).skillId == skills[3].skillId
        res1.get(3).selfReporting.enabled
        !res1.get(3).selfReporting.rejectionMsg
        !res1.get(3).selfReporting.rejectedOn
        !res1.get(3).selfReporting.requestedOn

        res1.get(4).skillId == skills[4].skillId
        res1.get(4).selfReporting.enabled
        !res1.get(4).selfReporting.rejectionMsg
        !res1.get(4).selfReporting.rejectedOn
        !res1.get(4).selfReporting.requestedOn
    }

    def "when a self-reporting skill has a history of approvals only load the latest approval info - latest approval request"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(4,)
        skills.each {
            it.pointIncrement = 200
            it.numPerformToCompletion = 200
            it.selfReportingType = SkillDef.SelfReportingType.Approval
        }

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        List<Date> dates = (0..5).collect { new Date() - it }
        List<String> users = getRandomUsers(2)

        when:
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], dates[3], "approve 1")
        def approvals = skillsService.getApprovals(proj.projectId, 7, 1, 'requestedOn', false)
        skillsService.approve(proj.projectId, approvals.data.collect { it.id })

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], dates[2], "reject 1")
        approvals = skillsService.getApprovals(proj.projectId, 7, 1, 'requestedOn', false)
        skillsService.rejectSkillApprovals(proj.projectId, approvals.data.collect { it.id })

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], dates[1], "approve 2")
        approvals = skillsService.getApprovals(proj.projectId, 7, 1, 'requestedOn', false)
        skillsService.approve(proj.projectId, approvals.data.collect { it.id })

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], dates[0], "approve 3")

        def approvalsHistoryUser1 = skillsService.getApprovalsHistory(proj.projectId, 10, 1, 'requestedOn', false, '',  '', '')
        def res1 = skillsService.getSubjectDescriptions(proj.projectId, subj.subjectId, users[0]).sort { it.skillId }

        then:
        approvalsHistoryUser1.totalCount == 3

        res1.size() == 4
        res1.get(0).skillId == skills[0].skillId
        res1.get(0).selfReporting.enabled
        !res1.get(0).selfReporting.rejectionMsg
        res1.get(0).selfReporting.requestedOn == dates[0].time
        !res1.get(0).selfReporting.rejectedOn

        res1.get(1).skillId == skills[1].skillId
        res1.get(1).selfReporting.enabled
        !res1.get(1).selfReporting.rejectionMsg
        !res1.get(1).selfReporting.rejectedOn
        !res1.get(1).selfReporting.requestedOn


        res1.get(2).skillId == skills[2].skillId
        res1.get(2).selfReporting.enabled
        !res1.get(2).selfReporting.rejectionMsg
        !res1.get(2).selfReporting.rejectedOn
        !res1.get(2).selfReporting.requestedOn

        res1.get(3).skillId == skills[3].skillId
        res1.get(3).selfReporting.enabled
        !res1.get(3).selfReporting.rejectionMsg
        !res1.get(3).selfReporting.rejectedOn
        !res1.get(3).selfReporting.requestedOn
    }


    def "when a self-reporting skill has a history of approvals only load the latest approval info - latest request was approved"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(4,)
        skills.each {
            it.pointIncrement = 200
            it.numPerformToCompletion = 200
            it.selfReportingType = SkillDef.SelfReportingType.Approval
        }

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        List<Date> dates = (0..5).collect { new Date() - it }
        List<String> users = getRandomUsers(2)

        when:
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], dates[3], "approve 1")
        def approvals = skillsService.getApprovals(proj.projectId, 7, 1, 'requestedOn', false)
        skillsService.approve(proj.projectId, approvals.data.collect { it.id })

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], dates[2], "reject 1")
        approvals = skillsService.getApprovals(proj.projectId, 7, 1, 'requestedOn', false)
        skillsService.rejectSkillApprovals(proj.projectId, approvals.data.collect { it.id })

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], dates[1], "approve 2")
        approvals = skillsService.getApprovals(proj.projectId, 7, 1, 'requestedOn', false)
        skillsService.approve(proj.projectId, approvals.data.collect { it.id })

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], dates[0], "approve 3")
        approvals = skillsService.getApprovals(proj.projectId, 7, 1, 'requestedOn', false)
        skillsService.approve(proj.projectId, approvals.data.collect { it.id })

        def approvalsHistoryUser1 = skillsService.getApprovalsHistory(proj.projectId, 10, 1, 'requestedOn', false, '',  '', '')
        def res1 = skillsService.getSubjectDescriptions(proj.projectId, subj.subjectId, users[0]).sort { it.skillId }

        then:
        approvalsHistoryUser1.totalCount == 4

        res1.size() == 4
        res1.get(0).skillId == skills[0].skillId
        res1.get(0).selfReporting.enabled
        !res1.get(0).selfReporting.rejectionMsg
        !res1.get(0).selfReporting.requestedOn
        !res1.get(0).selfReporting.rejectedOn

        res1.get(1).skillId == skills[1].skillId
        res1.get(1).selfReporting.enabled
        !res1.get(1).selfReporting.rejectionMsg
        !res1.get(1).selfReporting.rejectedOn
        !res1.get(1).selfReporting.requestedOn


        res1.get(2).skillId == skills[2].skillId
        res1.get(2).selfReporting.enabled
        !res1.get(2).selfReporting.rejectionMsg
        !res1.get(2).selfReporting.rejectedOn
        !res1.get(2).selfReporting.requestedOn

        res1.get(3).skillId == skills[3].skillId
        res1.get(3).selfReporting.enabled
        !res1.get(3).selfReporting.rejectionMsg
        !res1.get(3).selfReporting.rejectedOn
        !res1.get(3).selfReporting.requestedOn
    }

    def "all requests are denied - user acknowledges one denial"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(4,)
        skills.each {
            it.pointIncrement = 200
            it.numPerformToCompletion = 200
            it.selfReportingType = SkillDef.SelfReportingType.Approval
        }

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        List<Date> dates = (0..5).collect { new Date() - it }
        List<String> users = getRandomUsers(2)

        List<Integer> rejectionIds = []
        when:
        // deny all
        skills.each {
            skillsService.addSkill([projectId: proj.projectId, skillId: it.skillId], users[0], dates[3], "approve 1")
            def approvals = skillsService.getApprovals(proj.projectId, 7, 1, 'requestedOn', false)
            rejectionIds.add(approvals.data[0].id)
            skillsService.rejectSkillApprovals(proj.projectId, approvals.data.collect { it.id }, "reject ${it.skillId}")
        }

        def approvalsHistoryUser1 = skillsService.getApprovalsHistory(proj.projectId, 10, 1, 'requestedOn', false, '',  '', '')
        def res1 = skillsService.getSubjectDescriptions(proj.projectId, subj.subjectId, users[0]).sort { it.skillId }

        skillsService.removeRejectionFromView(proj.projectId, rejectionIds[1], users[0])
        def approvalsHistoryUser2 = skillsService.getApprovalsHistory(proj.projectId, 10, 1, 'requestedOn', false, '',  '', '')
        def res2 = skillsService.getSubjectDescriptions(proj.projectId, subj.subjectId, users[0]).sort { it.skillId }

        then:
        approvalsHistoryUser1.totalCount == 4

        res1.size() == 4
        res1.each {
            assert it.selfReporting.enabled
            assert it.selfReporting.requestedOn
            assert it.selfReporting.rejectedOn
        }
        res1.collect { it.selfReporting.rejectionMsg } == ["reject skill1", "reject skill2", "reject skill3", "reject skill4"]

        approvalsHistoryUser2.totalCount == 4
        res2.size() == 4
        res2.collect { it.selfReporting.rejectionMsg } == ["reject skill1", null, "reject skill3", "reject skill4"]
        res2.each {
            assert it.selfReporting.enabled
        }
        res2[0].selfReporting.rejectedOn
        !res2[1].selfReporting.rejectedOn
        res2[2].selfReporting.rejectedOn
        res2[3].selfReporting.rejectedOn

        res2[0].selfReporting.requestedOn
        !res2[1].selfReporting.requestedOn
        res2[2].selfReporting.requestedOn
        res2[3].selfReporting.requestedOn
    }

}
