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
import skills.services.settings.ClientPrefKey
import skills.storage.model.ClientPref
import skills.storage.model.SkillDef

class ClientDisplaySubjSummarySpec extends DefaultIntSpec {

    def "load subject summary"(){
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        proj1_subj.helpUrl = "http://foo.org"
        proj1_subj.description = "This is a description"
        List<Map> allSkills = SkillsFactory.createSkills(10, 1, 1)
        List<Map> proj1_skills = allSkills[0..2]

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        // skills group1 - enabled
        def skillsGroup1 = allSkills[3]
        skillsGroup1.type = 'SkillsGroup'
        skillsService.createSkill(skillsGroup1)
        String skillsGroup1Id = skillsGroup1.skillId
        def group1Children = allSkills[4..6]
        group1Children.each { skill ->
            skillsService.assignSkillToSkillsGroup(skillsGroup1Id, skill)
        }
        skillsGroup1.numSkillsRequired = 2
        skillsService.updateSkill(skillsGroup1, null)

        def skillsGroup2 = allSkills[7]
        skillsGroup2.type = 'SkillsGroup'
        skillsGroup2.enabled = 'false'
        skillsService.createSkill(skillsGroup2)
        String skillsGroup2Id = skillsGroup2.skillId
        def group2Children = allSkills[8..9]
        group2Children.each { skill ->
            skill.enabled = 'false'
            skillsService.assignSkillToSkillsGroup(skillsGroup2Id, skill)
        }

        when:
        def summary = skillsService.getSkillSummary("user1", proj1.projectId, proj1_subj.subjectId)
        then:
        summary.skillsLevel == 0
        summary.skills.size() == 4
        summary.skills[0..2].every { it.type == 'Skill' && it.maxOccurrencesWithinIncrementInterval == 1 && it.totalPoints == 10 }
        summary.skills[3].type == 'SkillsGroup'
        summary.skills[3].enabled == 'true'
        summary.skills[3].numSkillsRequired == 2
        summary.skills[3].totalPoints == 30  // 2 of 3 skills required, but group totalPoints is sum of all skills (30)
        summary.skills[3].children
        summary.skills[3].children.size() == 3
        summary.skills[3].children.every { it.type == 'Skill' && it.totalPoints == 10 }

        summary.description == "This is a description"
        summary.helpUrl == "http://foo.org"
    }

    def "load subject summary, no skills"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        proj1_subj.helpUrl = "http://foo.org"
        proj1_subj.description = "This is a description"
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        when:
        def summary = skillsService.getSkillSummary("user1", proj1.projectId, proj1_subj.subjectId, -1, false)
        then:
        summary.skills.size() == 0
        summary.description == "This is a description"
        summary.helpUrl == "http://foo.org"
    }

    def "return extra fields for the catalog imported skills"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills[0].pointIncrement = 100

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)
        skillsService.exportSkillToCatalog(proj1.projectId, proj1_skills[0].skillId)
        skillsService.exportSkillToCatalog(proj1.projectId, proj1_skills[1].skillId)
        skillsService.exportSkillToCatalog(proj1.projectId, proj1_skills[2].skillId)

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 2)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 2)

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)
        skillsService.importSkillFromCatalog(proj2.projectId, proj2_subj.subjectId, proj1.projectId, proj1_skills[0].skillId)
        skillsService.createSkills(proj2_skills)
        skillsService.importSkillFromCatalog(proj2.projectId, proj2_subj.subjectId, proj1.projectId, proj1_skills[1].skillId)

        def proj3 = SkillsFactory.createProject(3)
        def proj3_subj = SkillsFactory.createSubject(3, 3)
        List<Map> proj3_skills = SkillsFactory.createSkills(2, 3, 3, 100)

        skillsService.createProject(proj3)
        skillsService.createSubject(proj3_subj)
        skillsService.createSkills(proj3_skills)
        skillsService.exportSkillToCatalog(proj3.projectId, proj3_skills[0].skillId)
        skillsService.exportSkillToCatalog(proj3.projectId, proj3_skills[1].skillId)

        skillsService.bulkImportSkillsFromCatalogAndFinalize(proj2.projectId, proj2_subj.subjectId, [
                [projectId: proj3.projectId, skillId: proj3_skills[0].skillId],
                [projectId: proj1.projectId, skillId: proj1_skills[2].skillId],
                [projectId: proj3.projectId, skillId: proj3_skills[1].skillId],
                ])

        when:
        def summary = skillsService.getSkillSummary("user1", proj2.projectId, proj2_subj.subjectId)

        then:
        summary
        summary.skills.collect { it.skillId } == ['skill1', 'skill1subj2', 'skill2subj2', 'skill2',  'skill1subj3', 'skill3',  'skill2subj3']
        summary.skills.collect { it.copiedFromProjectId } == ["TestProject1", null, null, "TestProject1", "TestProject3", "TestProject1", "TestProject3"]
        summary.skills.collect { it.copiedFromProjectName } == ["Test Project#1", null, null, "Test Project#1", "Test Project#3", "Test Project#1", "Test Project#3"]
    }

    def "subject's points and today's points"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(5, 1, 1, 200)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        String userId = getRandomUsers(1)[0]
        when:
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId, new Date() - 2)
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId], userId, new Date() - 1)
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(2).skillId], userId, new Date()) // today

        def summary = skillsService.getSkillSummary(userId, proj1.projectId, proj1_subj.subjectId, -1, false)
        def summary1 = skillsService.getSkillSummary(userId, proj1.projectId, proj1_subj.subjectId, -1, true)
        then:
        summary.points == 600
        summary.todaysPoints == 200

        summary1.points == 600
        summary1.todaysPoints == 200
    }


    def "subject's points and today's points are calculated for group's skills"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skillsGroup = SkillsFactory.createSkillsGroup()
        def allSkills = SkillsFactory.createSkills(4) // first one is group
        allSkills[1].pointIncrement = 100
        allSkills[2].pointIncrement = 100
        allSkills[3].pointIncrement = 100

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId
        skillsService.assignSkillToSkillsGroup(skillsGroupId, allSkills[1])
        skillsService.assignSkillToSkillsGroup(skillsGroupId, allSkills[2])
        skillsService.createSkill(allSkills[3])

        skillsGroup.enabled = 'true'
        skillsService.updateSkill(skillsGroup, null)

        String userId = getRandomUsers(1)[0]
        when:
        skillsService.addSkill([projectId: proj.projectId, skillId: allSkills[1].skillId], userId, new Date() - 1)
        skillsService.addSkill([projectId: proj.projectId, skillId: allSkills[2].skillId], userId, new Date()) // today

        def summary = skillsService.getSkillSummary(userId, proj.projectId, subj.subjectId, -1, false)
        def summary1 = skillsService.getSkillSummary(userId, proj.projectId, subj.subjectId, -1, true)
        then:
        summary.points == 200
        summary.todaysPoints == 100

        summary1.points == 200
        summary1.todaysPoints == 100
    }

    def "group descriptions always return when group-descriptions setting is enabled"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skillsGroup = SkillsFactory.createSkillsGroup()
        def skillsGroup2 = SkillsFactory.createSkillsGroup(1, 1, 22)
        def skillsGroup3 = SkillsFactory.createSkillsGroup(1, 1, 23)
        skillsGroup.description = 'Test description for skill'
        skillsGroup2.description = null
        skillsGroup3.description = 'Group 3 desc'
        def allSkills = SkillsFactory.createSkills(6) // first one is group
        allSkills[1].pointIncrement = 100
        allSkills[2].pointIncrement = 100
        allSkills[3].pointIncrement = 100

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId
        skillsService.assignSkillToSkillsGroup(skillsGroupId, allSkills[1])
        skillsService.assignSkillToSkillsGroup(skillsGroupId, allSkills[2])
        skillsService.createSkill(allSkills[3])

        skillsService.createSkill(skillsGroup2)
        skillsService.assignSkillToSkillsGroup(skillsGroup2.skillId, allSkills[4])
        skillsService.createSkill(skillsGroup3)
        skillsService.assignSkillToSkillsGroup(skillsGroup3.skillId, allSkills[5])

        String userId = getRandomUsers(1)[0]
        when:
        def summary = skillsService.getSkillSummary(userId, proj.projectId, subj.subjectId, -1, true)
        def group_t0 = summary.skills.find({ it -> it.skillId == skillsGroup.skillId })
        def group2_t0 = summary.skills.find({ it -> it.skillId == skillsGroup2.skillId })
        def group3_t0 = summary.skills.find({ it -> it.skillId == skillsGroup3.skillId })

        skillsService.addOrUpdateProjectSetting(proj.projectId, 'group-descriptions', 'true')
        def summary1 = skillsService.getSkillSummary(userId, proj.projectId, subj.subjectId, -1, true)
        def group_t1 = summary1.skills.find({ it -> it.skillId == skillsGroup.skillId })
        def group2_t1 = summary1.skills.find({ it -> it.skillId == skillsGroup2.skillId })
        def group3_t1 = summary1.skills.find({ it -> it.skillId == skillsGroup3.skillId })

        then:
        group_t0.description == null
        group_t1.description.description == 'Test description for skill'

        group2_t0.description == null
        group2_t1.description == null

        group3_t0.description == null
        group3_t1.description.description == 'Group 3 desc'
    }

    def "last skill viewed"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skillsGroup = SkillsFactory.createSkillsGroup()
        def allSkills = SkillsFactory.createSkills(6) // first one is group

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId
        skillsService.assignSkillToSkillsGroup(skillsGroupId, allSkills[1])
        skillsService.assignSkillToSkillsGroup(skillsGroupId, allSkills[2])
        skillsService.createSkill(allSkills[3])

        List<String> users = getRandomUsers(2)
        String userId = users[0]
        String userId1 = users[1]
        SkillsService user1Service = createService(userId)
        SkillsService user2Service = createService(userId1)
        when:
        user2Service.documentVisitedSkillId(proj.projectId, allSkills[3].skillId)
        def summary = skillsService.getSkillSummary(userId, proj.projectId, subj.subjectId, -1, true)
        user2Service.documentVisitedSkillId(proj.projectId, allSkills[1].skillId)
        user1Service.documentVisitedSkillId(proj.projectId, allSkills[3].skillId)
        def summary_t1 = skillsService.getSkillSummary(userId, proj.projectId, subj.subjectId, -1, true)
        user2Service.documentVisitedSkillId(proj.projectId, allSkills[3].skillId)
        user1Service.documentVisitedSkillId(proj.projectId, allSkills[1].skillId)
        def summary_t2 = skillsService.getSkillSummary(userId, proj.projectId, subj.subjectId, -1, true)
        user2Service.documentVisitedSkillId(proj.projectId, allSkills[1].skillId)
        user1Service.documentVisitedSkillId(proj.projectId, allSkills[2].skillId)
        def summary_t3 = skillsService.getSkillSummary(userId, proj.projectId, subj.subjectId, -1, true)

        then:
        summary.skills[0].children.isLastViewed == [false, false]
        summary.skills[1].isLastViewed == false

        summary_t1.skills[0].children.isLastViewed == [false, false]
        summary_t1.skills[1].isLastViewed == true

        summary_t2.skills[0].children.isLastViewed == [true, false]
        summary_t2.skills[1].isLastViewed == false

        summary_t3.skills[0].children.isLastViewed == [false, true]
        summary_t3.skills[1].isLastViewed == false

    }

    def "extra entries are cleaned up if more than 1 ClientPref is present for ClientPrefKey.LAST_VIEWED_SKILL"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skillsGroup = SkillsFactory.createSkillsGroup()
        def allSkills = SkillsFactory.createSkills(6) // first one is group

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId
        skillsService.assignSkillToSkillsGroup(skillsGroupId, allSkills[1])
        skillsService.assignSkillToSkillsGroup(skillsGroupId, allSkills[2])
        skillsService.createSkill(allSkills[3])

        List<String> users = getRandomUsers(2)
        String userId = users[0]
        SkillsService user1Service = createService(userId)

        clientPrefRepo.save(new ClientPref(key: ClientPrefKey.LastViewedSkill, value: allSkills[0].skillId, projectId: proj.projectId, userId: userId))
        Thread.sleep(1000)
        clientPrefRepo.save(new ClientPref(key: ClientPrefKey.LastViewedSkill, value: allSkills[1].skillId, projectId: proj.projectId, userId: userId))
        Thread.sleep(1000)
        clientPrefRepo.save(new ClientPref(key: ClientPrefKey.LastViewedSkill, value: allSkills[2].skillId, projectId: proj.projectId, userId: userId))


        when:
        assert clientPrefRepo.findAll().collect { it.value } == [allSkills[0].skillId, allSkills[1].skillId, allSkills[2].skillId]
        user1Service.documentVisitedSkillId(proj.projectId, allSkills[3].skillId)

        then:
        // only the latest is kept
        clientPrefRepo.findAll().collect { it.value } == [allSkills[3].skillId]
    }

    def "load subject summary with approvals"(){
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> allSkills = SkillsFactory.createSkills(2, 1, 1)
        allSkills[0].pointIncrement = 200
        allSkills[0].numPerformToCompletion = 200
        allSkills[0].selfReportingType = SkillDef.SelfReportingType.Approval

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(allSkills)

        List<String> users = getRandomUsers(1)
        def requestedDate = new Date()
        skillsService.addSkill([projectId: proj1.projectId, skillId: allSkills[0].skillId], users.first(), requestedDate, "Please approve this 1!")

        when:
        def summary = skillsService.getSkillSummary(users.first(), proj1.projectId, proj1_subj.subjectId)
        then:
        summary.skills.size() == 2
        summary.skills[0].selfReporting.requestedOn == requestedDate.time
        !summary.skills[1].selfReporting.enabled
    }

    def "load subject summary with badges"(){
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        proj1_subj.helpUrl = "http://foo.org"
        proj1_subj.description = "This is a description"
        List<Map> allSkills = SkillsFactory.createSkills(3, 1, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(allSkills)

        Map badge2 = SkillsFactory.createBadge(1, 2)
        skillsService.createBadge(badge2)
        skillsService.assignSkillToBadge(proj1.projectId, badge2.badgeId, allSkills[0].skillId)
        badge2.enabled = true
        skillsService.updateBadge(badge2, badge2.badgeId)

        Map badge1 = SkillsFactory.createBadge(1, 1)
        skillsService.createBadge(badge1)
        allSkills.each {
            skillsService.assignSkillToBadge(proj1.projectId, badge1.badgeId, it.skillId)
        }
        badge1.enabled = true
        skillsService.updateBadge(badge1, badge1.badgeId)

        when:
        def summary = skillsService.getSkillSummary("user1", proj1.projectId, proj1_subj.subjectId)
        then:
        summary.skills.size() == 3
        summary.skills[0].badges.size() == 2
        summary.skills[0].badges[0].badgeId == badge1.badgeId
        summary.skills[0].badges[1].badgeId == badge2.badgeId
        summary.skills[1..2].every { it.badges.size() == 1 && it.badges[0].badgeId == badge1.badgeId }
    }

    def "load subject summary with badges in a group"(){
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        proj1_subj.helpUrl = "http://foo.org"
        proj1_subj.description = "This is a description"
        List<Map> allSkills = SkillsFactory.createSkills(6, 1, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(allSkills[0..2])

        Map badge1 = SkillsFactory.createBadge(1, 1)
        skillsService.createBadge(badge1)
        allSkills[0..2].each {
            skillsService.assignSkillToBadge(proj1.projectId, badge1.badgeId, it.skillId)
        }
        badge1.enabled = true
        skillsService.updateBadge(badge1, badge1.badgeId)

        def skillsGroup1 = allSkills[3]
        skillsGroup1.type = 'SkillsGroup'
        skillsService.createSkill(skillsGroup1)
        String skillsGroup1Id = skillsGroup1.skillId
        def group1Children = allSkills[4..5]
        group1Children.each { skill ->
            skillsService.assignSkillToSkillsGroup(skillsGroup1Id, skill)
            skillsService.assignSkillToBadge(proj1.projectId, badge1.badgeId, skill.skillId)
        }

        when:
        def summary = skillsService.getSkillSummary("user1", proj1.projectId, proj1_subj.subjectId)
        then:
        summary.skills.size() == 4
        summary.skills[0].badges.size() == 1
        summary.skills[0].badges[0].badgeId == badge1.badgeId
        summary.skills[3].children.size() == 2
        summary.skills[3].children[0].badges.size() == 1
        summary.skills[3].children[1].badges.size() == 1
        summary.skills[3].children[0].badges[0].badgeId == badge1.badgeId
        summary.skills[3].children[1].badges[0].badgeId == badge1.badgeId
        summary.skills[1..2].every { it.badges.size() == 1 && it.badges[0].badgeId == badge1.badgeId }

    }

    def "load subject summary with badges, does not load disabled badges"(){
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        proj1_subj.helpUrl = "http://foo.org"
        proj1_subj.description = "This is a description"
        List<Map> allSkills = SkillsFactory.createSkills(3, 1, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(allSkills)

        Map badge1 = SkillsFactory.createBadge(1, 1)
        badge1.enabled = true

        skillsService.createBadge(badge1)
        allSkills.each {
            skillsService.assignSkillToBadge(proj1.projectId, badge1.badgeId, it.skillId)
        }
        skillsService.updateBadge(badge1, badge1.badgeId)

        Map badge2 = SkillsFactory.createBadge(1, 2)
        skillsService.createBadge(badge2)
        skillsService.assignSkillToBadge(proj1.projectId, badge2.badgeId, allSkills[0].skillId)

        when:
        def summary = skillsService.getSkillSummary("user1", proj1.projectId, proj1_subj.subjectId)
        then:
        summary.skills.size() == 3
        summary.skills[0].badges.size() == 1

    }

    def "skill tags are returned for skills and groups"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> allSkills = SkillsFactory.createSkills(7, 1, 1)
        List<Map> proj1_skills = allSkills[0..2]

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        def skillsGroup1 = allSkills[3]
        skillsGroup1.type = 'SkillsGroup'
        skillsService.createSkill(skillsGroup1)
        String skillsGroup1Id = skillsGroup1.skillId
        def group1Children = allSkills[4..6]
        group1Children.each { skill ->
            skillsService.assignSkillToSkillsGroup(skillsGroup1Id, skill)
        }
        skillsGroup1.numSkillsRequired = 2
        skillsService.updateSkill(skillsGroup1, null)

        List<String> regSkillids = proj1_skills.collect { it.skillId }
        List<String> childSkillids = group1Children.collect { it.skillId }
        List<String> nonGroupSkillids = regSkillids + childSkillids

        skillsService.addTagToSkills(proj1.projectId, regSkillids, "Regular Skill")
        skillsService.addTagToSkills(proj1.projectId, childSkillids, "Group Child Skill")
        skillsService.addTagToSkills(proj1.projectId, nonGroupSkillids, "A Skill")

        when:
        def summary = skillsService.getSkillSummary("user1", proj1.projectId, proj1_subj.subjectId)
        then:
        summary.skillsLevel == 0
        summary.skills.size() == 4
        summary.skills[0..2].every { it.tags.collect { it.tagValue}.contains('A Skill') }
        summary.skills[0..2].every { it.tags.collect { it.tagValue}.contains('Regular Skill') }
        !summary.skills[0..2].any { it.tags.collect { it.tagValue}.contains('Group Child Skill') }
        summary.skills[3].type == 'SkillsGroup'
        summary.skills[3].children
        summary.skills[3].children.size() == 3
        summary.skills[3].children.every { it.tags.collect { it.tagValue}.contains('A Skill') }
        summary.skills[3].children.every { it.tags.collect { it.tagValue}.contains('Group Child Skill') }
        !summary.skills[3].children.any { it.tags.collect { it.tagValue}.contains('Regular Skill') }
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
        def res1 = skillsService.getSkillSummary(user, proj1.projectId, proj1_subj1.subjectId).skills.sort { it.skillId }
        def res2 = skillsService.getSkillSummary(user, proj1.projectId, proj1_subj2.subjectId).skills.sort { it.skillId }

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
        !res1.get(0).selfReporting.message

        res1.get(1).skillId == proj1_subj1_skills[1].skillId
        res1.get(1).selfReporting.enabled
        res1.get(1).selfReporting.type == SkillDef.SelfReportingType.Approval.toString()
        res1.get(1).selfReporting.requestedOn == date1.time
        !res1.get(1).selfReporting.rejectedOn
        !res1.get(1).selfReporting.message

        res1.get(2).skillId == proj1_subj1_skills[2].skillId
        !res1.get(2).selfReporting.enabled
        !res1.get(2).selfReporting.type
        !res1.get(2).selfReporting.requestedOn
        !res1.get(2).selfReporting.rejectedOn
        !res1.get(2).selfReporting.message

        res1.get(3).skillId == proj1_subj1_skills[3].skillId
        res1.get(3).selfReporting.enabled
        res1.get(3).selfReporting.type == SkillDef.SelfReportingType.Approval.toString()
        res1.get(3).selfReporting.requestedOn == date2.time
        new Date(res1.get(3).selfReporting.rejectedOn).format('yyyy-MM-dd') == new Date().format('yyyy-MM-dd')
        res1.get(3).selfReporting.message == "rejection message"

        res2.size() == 3
        res2.get(0).skillId == proj1_subj2_skills[0].skillId
        !res2.get(0).selfReporting.enabled
        !res2.get(0).selfReporting.type
        !res2.get(0).selfReporting.requestedOn
        !res2.get(0).selfReporting.rejectedOn
        !res2.get(0).selfReporting.message

        res2.get(1).skillId == proj1_subj2_skills[1].skillId
        res2.get(1).selfReporting.enabled
        res2.get(1).selfReporting.type == SkillDef.SelfReportingType.HonorSystem.toString()
        !res2.get(1).selfReporting.requestedOn
        !res2.get(1).selfReporting.rejectedOn
        !res2.get(1).selfReporting.message

        res2.get(2).skillId == proj1_subj2_skills[2].skillId
        res2.get(2).selfReporting.enabled
        res2.get(2).selfReporting.type == SkillDef.SelfReportingType.Approval.toString()
        !res2.get(2).selfReporting.requestedOn
        !res2.get(2).selfReporting.rejectedOn
        !res2.get(2).selfReporting.message
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

        def res1 = skillsService.getSkillSummary(users[0], proj.projectId, subj.subjectId).skills.sort { it.skillId }

        then:
        approvalsHistoryUser1.totalCount == 5

        res1.size() == 5
        res1.get(0).skillId == skills[0].skillId
        res1.get(0).selfReporting.enabled
        res1.get(0).selfReporting.message == 'last rejection'
        res1.get(0).selfReporting.requestedOn == dates[0].time
        res1.get(0).selfReporting.rejectedOn

        res1.get(1).skillId == skills[1].skillId
        res1.get(1).selfReporting.enabled
        !res1.get(1).selfReporting.message
        !res1.get(1).selfReporting.rejectedOn

        res1.get(2).skillId == skills[2].skillId
        res1.get(2).selfReporting.enabled
        !res1.get(2).selfReporting.message
        !res1.get(2).selfReporting.rejectedOn

        res1.get(3).skillId == skills[3].skillId
        res1.get(3).selfReporting.enabled
        !res1.get(3).selfReporting.message
        !res1.get(3).selfReporting.rejectedOn

        res1.get(4).skillId == skills[4].skillId
        res1.get(4).selfReporting.enabled
        !res1.get(4).selfReporting.message
        !res1.get(4).selfReporting.rejectedOn
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
        def res1 = skillsService.getSkillSummary(users[0], proj.projectId, subj.subjectId).skills.sort { it.skillId }

        then:
        approvalsHistoryUser1.totalCount == 3

        res1.size() == 4
        res1.get(0).skillId == skills[0].skillId
        res1.get(0).selfReporting.enabled
        !res1.get(0).selfReporting.message
        res1.get(0).selfReporting.requestedOn == dates[0].time
        !res1.get(0).selfReporting.rejectedOn

        res1.get(1).skillId == skills[1].skillId
        res1.get(1).selfReporting.enabled
        !res1.get(1).selfReporting.message
        !res1.get(1).selfReporting.rejectedOn
        !res1.get(1).selfReporting.requestedOn


        res1.get(2).skillId == skills[2].skillId
        res1.get(2).selfReporting.enabled
        !res1.get(2).selfReporting.message
        !res1.get(2).selfReporting.rejectedOn
        !res1.get(2).selfReporting.requestedOn

        res1.get(3).skillId == skills[3].skillId
        res1.get(3).selfReporting.enabled
        !res1.get(3).selfReporting.message
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
        skillsService.approve(proj.projectId, approvals.data.collect { it.id }, 'approval message!')

        def approvalsHistoryUser1 = skillsService.getApprovalsHistory(proj.projectId, 10, 1, 'requestedOn', false, '',  '', '')
        def res1 = skillsService.getSkillSummary(users[0], proj.projectId, subj.subjectId).skills.sort { it.skillId }

        then:
        approvalsHistoryUser1.totalCount == 4

        res1.size() == 4
        res1.get(0).skillId == skills[0].skillId
        res1.get(0).selfReporting.enabled
        res1.get(0).selfReporting.message == 'approval message!'
        !res1.get(0).selfReporting.rejectedOn

        res1.get(1).skillId == skills[1].skillId
        res1.get(1).selfReporting.enabled
        !res1.get(1).selfReporting.message
        !res1.get(1).selfReporting.rejectedOn

        res1.get(2).skillId == skills[2].skillId
        res1.get(2).selfReporting.enabled
        !res1.get(2).selfReporting.message
        !res1.get(2).selfReporting.rejectedOn

        res1.get(3).skillId == skills[3].skillId
        res1.get(3).selfReporting.enabled
        !res1.get(3).selfReporting.message
        !res1.get(3).selfReporting.rejectedOn
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
//        def res1 = skillsService.getSubjectDescriptions(proj.projectId, subj.subjectId, users[0]).sort { it.skillId }
        def res1 = skillsService.getSkillSummary(users[0], proj.projectId, subj.subjectId).skills.sort { it.skillId }

        skillsService.removeRejectionFromView(proj.projectId, rejectionIds[1], users[0])
        def approvalsHistoryUser2 = skillsService.getApprovalsHistory(proj.projectId, 10, 1, 'requestedOn', false, '',  '', '')
//        def res2 = skillsService.getSubjectDescriptions(proj.projectId, subj.subjectId, users[0]).sort { it.skillId }
        def res2 = skillsService.getSkillSummary(users[0], proj.projectId, subj.subjectId).skills.sort { it.skillId }

        then:
        approvalsHistoryUser1.totalCount == 4

        res1.size() == 4
        res1.each {
            assert it.selfReporting.enabled
            assert it.selfReporting.requestedOn
            assert it.selfReporting.rejectedOn
        }
        res1.collect { it.selfReporting.message } == ["reject skill1", "reject skill2", "reject skill3", "reject skill4"]

        approvalsHistoryUser2.totalCount == 4
        res2.size() == 4
        res2.collect { it.selfReporting.message } == ["reject skill1", "reject skill2", "reject skill3", "reject skill4"]
        res2.each {
            assert it.selfReporting.enabled
        }
        res2[0].selfReporting.rejectedOn
        res2[1].selfReporting.rejectedOn
        res2[2].selfReporting.rejectedOn
        res2[3].selfReporting.rejectedOn

        res2[0].selfReporting.requestedOn
        res2[1].selfReporting.requestedOn
        res2[2].selfReporting.requestedOn
        res2[3].selfReporting.requestedOn
    }

    def "when a self-reporting skill has a history of approvals only load the latest approval info - reject, re-request, approve"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
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
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], dates[3], "will reject")
        def approvals = skillsService.getApprovals(proj.projectId, 7, 1, 'requestedOn', false)
        assert approvals.data.size() == 1
        skillsService.rejectSkillApprovals(proj.projectId, approvals.data.collect { it.id })

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], dates[2], "approve 2")
        approvals = skillsService.getApprovals(proj.projectId, 7, 1, 'requestedOn', false)
        assert approvals.data.size() == 1

        def approvalsHistoryUser1 = skillsService.getApprovalsHistory(proj.projectId, 10, 1, 'requestedOn', false, '',  '', '')
        def res1 = skillsService.getSkillSummary(users[0], proj.projectId, subj.subjectId).skills.sort { it.skillId }

        then:
        approvalsHistoryUser1.totalCount == 1

        res1.size() == 1
        res1.get(0).skillId == skills[0].skillId
        res1.get(0).selfReporting.enabled
        !res1.get(0).selfReporting.message
        !res1.get(0).selfReporting.rejectedOn
        res1.get(0).selfReporting.requestedOn == dates[2].time
    }
}
