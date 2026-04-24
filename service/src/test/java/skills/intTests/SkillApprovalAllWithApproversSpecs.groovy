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
package skills.intTests

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.storage.model.SkillDef
import skills.storage.model.auth.RoleName

class SkillApprovalAllWithApproversSpecs extends DefaultIntSpec {

    void "just fallback approvers"() {
        List<SkillsService> allUsers = getRandomUsers(5).collect { createService(it) }
        List<SkillsService> admins = allUsers[0..2]
        List<SkillsService> users = allUsers[3..4]
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].numPerformToCompletion = 200
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval

        admins[0].createProject(proj)
        admins[0].createSubject(subj)
        admins[0].createSkills(skills)

        def subj1 = SkillsFactory.createSubject(1, 2)
        def skills1 = SkillsFactory.createSkills(1, 1, 2)
        skills1[0].pointIncrement = 200
        skills1[0].numPerformToCompletion = 200
        skills1[0].selfReportingType = SkillDef.SelfReportingType.Approval

        admins[0].createSubject(subj1)
        admins[0].createSkills(skills1)

        admins[0].addProjectAdmin(proj.projectId, admins[1].userName)
        admins[0].addUserRole(admins[2].userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())

        users[0].addSkill([projectId: proj.projectId, skillId: skills[0].skillId])
        users[1].addSkill([projectId: proj.projectId, skillId: skills1[0].skillId])

        when:
        def res = admins[0].getApprovals(proj.projectId, 5, 1, 'requestedOn', false, '', '', true)

        admins[0].configureFallbackApprover(proj.projectId, admins[1].userName)
        def res_1Fallback = admins[0].getApprovals(proj.projectId, 5, 1, 'requestedOn', false, '', '', true)

        admins[0].configureFallbackApprover(proj.projectId, admins[2].userName)
        def res_2Fallback = admins[0].getApprovals(proj.projectId, 5, 1, 'requestedOn', false, '', '', true)

        then:
        res.data.size() == 2
        res.data[0].configuredApprovers.approverUserId.sort() == [admins[0].userName, admins[1].userName, admins[2].userName].sort()
        res.data[0].configuredApprovers.configuredTypes == [['Fallback'], ['Fallback'], ['Fallback']]
        res.data[1].configuredApprovers.approverUserId.sort() == [admins[0].userName, admins[1].userName, admins[2].userName].sort()
        res.data[1].configuredApprovers.configuredTypes == [['Fallback'], ['Fallback'], ['Fallback']]

        res_1Fallback.data.size() == 2
        res_1Fallback.data[0].configuredApprovers.approverUserId == [admins[1].userName]
        res_1Fallback.data[0].configuredApprovers.configuredTypes == [['Fallback']]
        res_1Fallback.data[1].configuredApprovers.approverUserId == [admins[1].userName]
        res_1Fallback.data[1].configuredApprovers.configuredTypes == [['Fallback']]

        res_2Fallback.data.size() == 2
        res_2Fallback.data[0].configuredApprovers.approverUserId.sort() == [admins[1].userName, admins[2].userName,].sort()
        res_2Fallback.data[0].configuredApprovers.configuredTypes == [['Fallback'], ['Fallback']]
        res_2Fallback.data[1].configuredApprovers.approverUserId.sort() == [admins[1].userName, admins[2].userName,].sort()
        res_2Fallback.data[1].configuredApprovers.configuredTypes == [['Fallback'], ['Fallback']]
    }

    void "approver based on skill conf"() {
        List<SkillsService> allUsers = getRandomUsers(5).collect { createService(it) }
        List<SkillsService> admins = allUsers[0..2]
        List<SkillsService> users = allUsers[3..4]
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(3, 1, 1, 100)
        skills.each {
            it.selfReportingType = SkillDef.SelfReportingType.Approval
        }

        admins[0].createProject(proj)
        admins[0].createSubject(subj)
        admins[0].createSkills(skills)

        def subj1 = SkillsFactory.createSubject(1, 2)
        def skills1 = SkillsFactory.createSkills(3, 1, 2, 100)
        skills1.each {
            it.selfReportingType = SkillDef.SelfReportingType.Approval
        }

        admins[0].createSubject(subj1)
        admins[0].createSkills(skills1)

        admins[0].addProjectAdmin(proj.projectId, admins[1].userName)
        admins[0].addUserRole(admins[2].userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())

        users[0].addSkill([projectId: proj.projectId, skillId: skills[0].skillId])
        users[0].addSkill([projectId: proj.projectId, skillId: skills1[0].skillId])
        users[0].addSkill([projectId: proj.projectId, skillId: skills1[1].skillId])
        users[1].addSkill([projectId: proj.projectId, skillId: skills1[0].skillId])

        admins[0].configureApproverForSkillId(proj.projectId, admins[1].userName, skills1[1].skillId)

        when:
        def res = admins[0].getApprovals(proj.projectId, 5, 1, 'requestedOn', false, '', '', true)

        admins[0].configureApproverForSkillId(proj.projectId, admins[0].userName, skills1[1].skillId)
        def res_1 = admins[0].getApprovals(proj.projectId, 5, 1, 'requestedOn', false, '', '', true)

        admins[0].configureApproverForSkillId(proj.projectId, admins[0].userName, skills1[0].skillId)
        def res_2 = admins[0].getApprovals(proj.projectId, 5, 1, 'requestedOn', false, '', '', true)
        then:
        res.data.size() == 4
        res.data[0].skillId == skills1[0].skillId
        res.data[0].userId == users[1].userName
        res.data[0].configuredApprovers.approverUserId.sort() == [admins[0].userName, admins[2].userName].sort()
        res.data[0].configuredApprovers.configuredTypes == [['Fallback'], ['Fallback']]

        res.data[1].skillId == skills1[1].skillId
        res.data[1].userId == users[0].userName
        res.data[1].configuredApprovers.approverUserId.sort() == [admins[1].userName].sort()
        res.data[1].configuredApprovers.configuredTypes == [['Skill']]

        res.data[2].skillId == skills1[0].skillId
        res.data[2].userId == users[0].userName
        res.data[2].configuredApprovers.approverUserId.sort() == [admins[0].userName, admins[2].userName].sort()
        res.data[2].configuredApprovers.configuredTypes == [['Fallback'], ['Fallback']]

        res.data[3].skillId == skills[0].skillId
        res.data[3].userId == users[0].userName
        res.data[3].configuredApprovers.approverUserId.sort() == [admins[0].userName, admins[2].userName].sort()
        res.data[3].configuredApprovers.configuredTypes == [['Fallback'], ['Fallback']]

        res_1.data.size() == 4
        res_1.data[0].skillId == skills1[0].skillId
        res_1.data[0].userId == users[1].userName
        res_1.data[0].configuredApprovers.approverUserId.sort() == [admins[2].userName].sort()
        res_1.data[0].configuredApprovers.configuredTypes == [['Fallback']]

        res_1.data[1].skillId == skills1[1].skillId
        res_1.data[1].userId == users[0].userName
        res_1.data[1].configuredApprovers.approverUserId.sort() == [admins[0].userName, admins[1].userName].sort()
        res_1.data[1].configuredApprovers.configuredTypes == [['Skill'], ['Skill']]

        res_1.data[2].skillId == skills1[0].skillId
        res_1.data[2].userId == users[0].userName
        res_1.data[2].configuredApprovers.approverUserId.sort() == [admins[2].userName].sort()
        res_1.data[2].configuredApprovers.configuredTypes == [['Fallback']]

        res_1.data[3].skillId == skills[0].skillId
        res_1.data[3].userId == users[0].userName
        res_1.data[3].configuredApprovers.approverUserId.sort() == [admins[2].userName].sort()
        res_1.data[3].configuredApprovers.configuredTypes == [['Fallback']]


        res_2.data.size() == 4
        res_2.data[0].skillId == skills1[0].skillId
        res_2.data[0].userId == users[1].userName
        res_2.data[0].configuredApprovers.approverUserId.sort() == [admins[0].userName].sort()
        res_2.data[0].configuredApprovers.configuredTypes == [['Skill']]

        res_2.data[1].skillId == skills1[1].skillId
        res_2.data[1].userId == users[0].userName
        res_2.data[1].configuredApprovers.approverUserId.sort() == [admins[0].userName, admins[1].userName].sort()
        res_2.data[1].configuredApprovers.configuredTypes == [['Skill'], ['Skill']]

        res_2.data[2].skillId == skills1[0].skillId
        res_2.data[2].userId == users[0].userName
        res_2.data[2].configuredApprovers.approverUserId.sort() == [admins[0].userName].sort()
        res_2.data[2].configuredApprovers.configuredTypes == [['Skill']]

        res_2.data[3].skillId == skills[0].skillId
        res_2.data[3].userId == users[0].userName
        res_2.data[3].configuredApprovers.approverUserId.sort() == [admins[2].userName].sort()
        res_2.data[3].configuredApprovers.configuredTypes == [['Fallback']]
    }

    void "approver based on user conf"() {
        List<SkillsService> allUsers = getRandomUsers(5).collect { createService(it) }
        List<SkillsService> admins = allUsers[0..2]
        List<SkillsService> users = allUsers[3..4]
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(3, 1, 1, 100)
        skills.each {
            it.selfReportingType = SkillDef.SelfReportingType.Approval
        }

        admins[0].createProject(proj)
        admins[0].createSubject(subj)
        admins[0].createSkills(skills)

        def subj1 = SkillsFactory.createSubject(1, 2)
        def skills1 = SkillsFactory.createSkills(3, 1, 2, 100)
        skills1.each {
            it.selfReportingType = SkillDef.SelfReportingType.Approval
        }

        admins[0].createSubject(subj1)
        admins[0].createSkills(skills1)

        admins[0].addProjectAdmin(proj.projectId, admins[1].userName)
        admins[0].addUserRole(admins[2].userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())

        users[0].addSkill([projectId: proj.projectId, skillId: skills[0].skillId])
        users[0].addSkill([projectId: proj.projectId, skillId: skills1[0].skillId])
        users[0].addSkill([projectId: proj.projectId, skillId: skills1[1].skillId])
        users[1].addSkill([projectId: proj.projectId, skillId: skills1[0].skillId])

        admins[0].configureApproverForUser(proj.projectId, admins[1].userName, users[1].userName)

        when:
        def res = admins[0].getApprovals(proj.projectId, 5, 1, 'requestedOn', false, '', '', true)

        admins[0].configureApproverForUser(proj.projectId, admins[0].userName, users[1].userName)
        def res_1 = admins[0].getApprovals(proj.projectId, 5, 1, 'requestedOn', false, '', '', true)

        admins[0].configureApproverForUser(proj.projectId, admins[0].userName, users[0].userName)
        def res_2 = admins[0].getApprovals(proj.projectId, 5, 1, 'requestedOn', false, '', '', true)

        then:
        res.data.size() == 4
        res.data[0].skillId == skills1[0].skillId
        res.data[0].userId == users[1].userName
        res.data[0].configuredApprovers.approverUserId.sort() == [admins[1].userName].sort()
        res.data[0].configuredApprovers.configuredTypes == [['User']]

        res.data[1].skillId == skills1[1].skillId
        res.data[1].userId == users[0].userName
        res.data[1].configuredApprovers.approverUserId.sort() == [admins[0].userName, admins[2].userName].sort()
        res.data[1].configuredApprovers.configuredTypes == [['Fallback'], ['Fallback']]

        res.data[2].skillId == skills1[0].skillId
        res.data[2].userId == users[0].userName
        res.data[2].configuredApprovers.approverUserId.sort() == [admins[0].userName, admins[2].userName].sort()
        res.data[2].configuredApprovers.configuredTypes == [['Fallback'], ['Fallback']]

        res.data[3].skillId == skills[0].skillId
        res.data[3].userId == users[0].userName
        res.data[3].configuredApprovers.approverUserId.sort() == [admins[0].userName, admins[2].userName].sort()
        res.data[3].configuredApprovers.configuredTypes == [['Fallback'], ['Fallback']]

        res_1.data.size() == 4
        res_1.data[0].skillId == skills1[0].skillId
        res_1.data[0].userId == users[1].userName
        res_1.data[0].configuredApprovers.approverUserId.sort() == [admins[0].userName, admins[1].userName].sort()
        res_1.data[0].configuredApprovers.configuredTypes == [['User'], ['User']]

        res_1.data[1].skillId == skills1[1].skillId
        res_1.data[1].userId == users[0].userName
        res_1.data[1].configuredApprovers.approverUserId.sort() == [admins[2].userName].sort()
        res_1.data[1].configuredApprovers.configuredTypes == [['Fallback']]

        res_1.data[2].skillId == skills1[0].skillId
        res_1.data[2].userId == users[0].userName
        res_1.data[2].configuredApprovers.approverUserId.sort() == [admins[2].userName].sort()
        res_1.data[2].configuredApprovers.configuredTypes == [['Fallback']]

        res_1.data[3].skillId == skills[0].skillId
        res_1.data[3].userId == users[0].userName
        res_1.data[3].configuredApprovers.approverUserId.sort() == [admins[2].userName].sort()
        res_1.data[3].configuredApprovers.configuredTypes == [['Fallback']]

        res_2.data.size() == 4
        res_2.data[0].skillId == skills1[0].skillId
        res_2.data[0].userId == users[1].userName
        res_2.data[0].configuredApprovers.approverUserId.sort() == [admins[0].userName, admins[1].userName].sort()
        res_2.data[0].configuredApprovers.configuredTypes == [['User'], ['User']]

        res_2.data[1].skillId == skills1[1].skillId
        res_2.data[1].userId == users[0].userName
        res_2.data[1].configuredApprovers.approverUserId.sort() == [admins[0].userName].sort()
        res_2.data[1].configuredApprovers.configuredTypes == [['User']]

        res_2.data[2].skillId == skills1[0].skillId
        res_2.data[2].userId == users[0].userName
        res_2.data[2].configuredApprovers.approverUserId.sort() == [admins[0].userName].sort()
        res_2.data[2].configuredApprovers.configuredTypes == [['User']]

        res_2.data[3].skillId == skills[0].skillId
        res_2.data[3].userId == users[0].userName
        res_2.data[3].configuredApprovers.approverUserId.sort() == [admins[0].userName].sort()
        res_2.data[3].configuredApprovers.configuredTypes == [['User']]
    }

    void "approver based on tag conf"() {
        List<SkillsService> allUsers = getRandomUsers(5).collect { createService(it) }
        List<SkillsService> admins = allUsers[0..2]
        List<SkillsService> users = allUsers[3..4]
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(3, 1, 1, 100)
        skills.each {
            it.selfReportingType = SkillDef.SelfReportingType.Approval
        }

        admins[0].createProject(proj)
        admins[0].createSubject(subj)
        admins[0].createSkills(skills)

        def subj1 = SkillsFactory.createSubject(1, 2)
        def skills1 = SkillsFactory.createSkills(3, 1, 2, 100)
        skills1.each {
            it.selfReportingType = SkillDef.SelfReportingType.Approval
        }

        admins[0].createSubject(subj1)
        admins[0].createSkills(skills1)

        admins[0].addProjectAdmin(proj.projectId, admins[1].userName)
        admins[0].addUserRole(admins[2].userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())

        users[0].addSkill([projectId: proj.projectId, skillId: skills[0].skillId])
        users[0].addSkill([projectId: proj.projectId, skillId: skills1[0].skillId])
        users[0].addSkill([projectId: proj.projectId, skillId: skills1[1].skillId])
        users[1].addSkill([projectId: proj.projectId, skillId: skills1[0].skillId])

        String userTagKey = "KeY1"
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(users[0].userName, userTagKey, ["Bcdd"])
        rootUser.saveUserTag(users[1].userName, userTagKey, ["aBcD"])

        admins[0].configureApproverForUserTag(proj.projectId, admins[1].userName, userTagKey.toLowerCase(), "AbC")

        when:
        def res = admins[0].getApprovals(proj.projectId, 5, 1, 'requestedOn', false, '', '', true)

        admins[0].configureApproverForUserTag(proj.projectId, admins[0].userName, userTagKey.toLowerCase(), "A")
        def res_1 = admins[0].getApprovals(proj.projectId, 5, 1, 'requestedOn', false, '', '', true)

        admins[0].configureApproverForUserTag(proj.projectId, admins[0].userName, userTagKey.toLowerCase(), "Bcdd")
        def res_2 = admins[0].getApprovals(proj.projectId, 5, 1, 'requestedOn', false, '', '', true)

        then:
        res.data.size() == 4
        res.data[0].skillId == skills1[0].skillId
        res.data[0].userId == users[1].userName
        res.data[0].configuredApprovers.approverUserId.sort() == [admins[1].userName].sort()
        res.data[0].configuredApprovers.configuredTypes == [[userTagKey.toLowerCase()]]

        res.data[1].skillId == skills1[1].skillId
        res.data[1].userId == users[0].userName
        res.data[1].configuredApprovers.approverUserId.sort() == [admins[0].userName, admins[2].userName].sort()
        res.data[1].configuredApprovers.configuredTypes == [['Fallback'], ['Fallback']]

        res.data[2].skillId == skills1[0].skillId
        res.data[2].userId == users[0].userName
        res.data[2].configuredApprovers.approverUserId.sort() == [admins[0].userName, admins[2].userName].sort()
        res.data[2].configuredApprovers.configuredTypes == [['Fallback'], ['Fallback']]

        res.data[3].skillId == skills[0].skillId
        res.data[3].userId == users[0].userName
        res.data[3].configuredApprovers.approverUserId.sort() == [admins[0].userName, admins[2].userName].sort()
        res.data[3].configuredApprovers.configuredTypes == [['Fallback'], ['Fallback']]

        res_1.data.size() == 4
        res_1.data[0].skillId == skills1[0].skillId
        res_1.data[0].userId == users[1].userName
        res_1.data[0].configuredApprovers.approverUserId.sort() == [admins[0].userName, admins[1].userName].sort()
        res_1.data[0].configuredApprovers.configuredTypes == [[userTagKey.toLowerCase()], [userTagKey.toLowerCase()]]

        res_1.data[1].skillId == skills1[1].skillId
        res_1.data[1].userId == users[0].userName
        res_1.data[1].configuredApprovers.approverUserId.sort() == [admins[2].userName].sort()
        res_1.data[1].configuredApprovers.configuredTypes == [['Fallback']]

        res_1.data[2].skillId == skills1[0].skillId
        res_1.data[2].userId == users[0].userName
        res_1.data[2].configuredApprovers.approverUserId.sort() == [admins[2].userName].sort()
        res_1.data[2].configuredApprovers.configuredTypes == [['Fallback']]

        res_1.data[3].skillId == skills[0].skillId
        res_1.data[3].userId == users[0].userName
        res_1.data[3].configuredApprovers.approverUserId.sort() == [admins[2].userName].sort()
        res_1.data[3].configuredApprovers.configuredTypes == [['Fallback']]

        res_2.data.size() == 4
        res_2.data[0].skillId == skills1[0].skillId
        res_2.data[0].userId == users[1].userName
        res_2.data[0].configuredApprovers.approverUserId.sort() == [admins[0].userName, admins[1].userName].sort()
        res_2.data[0].configuredApprovers.configuredTypes == [[userTagKey.toLowerCase()], [userTagKey.toLowerCase()]]

        res_2.data[1].skillId == skills1[1].skillId
        res_2.data[1].userId == users[0].userName
        res_2.data[1].configuredApprovers.approverUserId.sort() == [admins[0].userName].sort()
        res_2.data[1].configuredApprovers.configuredTypes == [[userTagKey.toLowerCase()]]

        res_2.data[2].skillId == skills1[0].skillId
        res_2.data[2].userId == users[0].userName
        res_2.data[2].configuredApprovers.approverUserId.sort() == [admins[0].userName].sort()
        res_2.data[2].configuredApprovers.configuredTypes == [[userTagKey.toLowerCase()]]

        res_2.data[3].skillId == skills[0].skillId
        res_2.data[3].userId == users[0].userName
        res_2.data[3].configuredApprovers.approverUserId.sort() == [admins[0].userName].sort()
        res_2.data[3].configuredApprovers.configuredTypes == [[userTagKey.toLowerCase()]]
    }

}
