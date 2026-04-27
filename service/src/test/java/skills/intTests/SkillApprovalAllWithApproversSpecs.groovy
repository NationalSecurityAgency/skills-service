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
import skills.intTests.utils.SkillsClientException
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

    void "approvers based on various configurations"() {
        List<SkillsService> allUsers = getRandomUsers(15).collect { createService(it) }
        List<SkillsService> admins = allUsers[0..8]
        List<SkillsService> users = allUsers[9..14]
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(6, 1, 1, 100)
        skills.each {
            it.selfReportingType = SkillDef.SelfReportingType.Approval
        }

        admins[0].createProjectAndSubjectAndSkills(proj, subj, skills)

        def subj1 = SkillsFactory.createSubject(1, 2)
        def skills1 = SkillsFactory.createSkills(6, 1, 2, 100)
        skills1.each {
            it.selfReportingType = SkillDef.SelfReportingType.Approval
        }

        admins[0].createProjectAndSubjectAndSkills(null, subj1, skills1)

        admins[0].addProjectAdmin(proj.projectId, admins[1].userName)
        admins[0].addUserRole(admins[2].userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())
        admins[0].addUserRole(admins[3].userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())
        admins[0].addUserRole(admins[4].userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())
        admins[0].addUserRole(admins[5].userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())
        admins[0].addUserRole(admins[6].userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())
        admins[0].addUserRole(admins[7].userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())

        String userTagKey = "KeY1"
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(users[0].userName, userTagKey, ["aBcD"])
        rootUser.saveUserTag(users[1].userName, userTagKey, ["BcDe"])
        rootUser.saveUserTag(users[2].userName, userTagKey, ["abcX"])
        rootUser.saveUserTag(users[3].userName, userTagKey, ["fgh"])

        admins[0].configureApproverForUserTag(proj.projectId, admins[0].userName, userTagKey.toLowerCase(), "AbC")
        admins[0].configureApproverForUser(proj.projectId, admins[0].userName, users[0].userName)
        admins[0].configureApproverForUser(proj.projectId, admins[0].userName, users[1].userName)
        admins[0].configureApproverForSkillId(proj.projectId, admins[0].userName, skills[0].skillId.toString())

        admins[0].configureApproverForSkillId(proj.projectId, admins[1].userName, skills[1].skillId.toString())
        admins[0].configureApproverForUserTag(proj.projectId, admins[1].userName, userTagKey.toLowerCase(), "AbCX")

        admins[0].configureApproverForUser(proj.projectId, admins[2].userName, users[2].userName)
        admins[0].configureApproverForUserTag(proj.projectId, admins[2].userName, userTagKey.toLowerCase(), "AbCX")

        admins[0].configureApproverForUser(proj.projectId, admins[3].userName, users[2].userName)
        admins[0].configureApproverForSkillId(proj.projectId, admins[3].userName, skills[1].skillId.toString())

        admins[0].configureApproverForSkillId(proj.projectId, admins[4].userName, skills[2].skillId.toString())
        admins[0].configureApproverForUserTag(proj.projectId, admins[5].userName, userTagKey.toLowerCase(), "fg")

        admins[0].configureApproverForUser(proj.projectId, admins[6].userName, users[4].userName)

        // keep in mind that they will be returned in the reverse order from getApprovals
        users[5].addSkill([projectId: proj.projectId, skillId: skills[4].skillId])
        users[4].addSkill([projectId: proj.projectId, skillId: skills[3].skillId])
        users[3].addSkill([projectId: proj.projectId, skillId: skills[2].skillId])
        users[2].addSkill([projectId: proj.projectId, skillId: skills[1].skillId])
        users[1].addSkill([projectId: proj.projectId, skillId: skills[0].skillId])
        users[0].addSkill([projectId: proj.projectId, skillId: skills[0].skillId])

        when:
        def res = admins[0].getApprovals(proj.projectId, 5, 1, 'requestedOn', false, '', '', true)
        def res_page1 = admins[0].getApprovals(proj.projectId, 5, 2, 'requestedOn', false, '', '', true)

        then:
        res.data.size() == 5
        res.count == 6
        res_page1.data.size() == 1
        res_page1.count == 6

        res.data[0].skillId == skills[0].skillId
        res.data[0].userId == users[0].userName
        res.data[0].configuredApprovers.approverUserId.sort() == [admins[0].userName].sort()
        res.data[0].configuredApprovers.configuredTypes.collect { it.sort() } == [[userTagKey.toLowerCase(), 'User', 'Skill'].sort()]

        res.data[1].skillId == skills[0].skillId
        res.data[1].userId == users[1].userName
        res.data[1].configuredApprovers.approverUserId.sort() == [admins[0].userName].sort()
        res.data[1].configuredApprovers.configuredTypes.collect { it.sort() } == [['User', 'Skill'].sort()]

        res.data[2].skillId == skills[1].skillId
        res.data[2].userId == users[2].userName
        res.data[2].configuredApprovers.approverUserId.sort() == [admins[0].userName, admins[1].userName, admins[2].userName, admins[3].userName].sort()
        res.data[2].configuredApprovers.find { it.approverUserId == admins[0].userName}.configuredTypes.sort() == [userTagKey.toLowerCase()].sort()
        res.data[2].configuredApprovers.find { it.approverUserId == admins[1].userName}.configuredTypes.sort() == [userTagKey.toLowerCase(), 'Skill'].sort()
        res.data[2].configuredApprovers.find { it.approverUserId == admins[2].userName}.configuredTypes.sort() == [userTagKey.toLowerCase(), 'User'].sort()
        res.data[2].configuredApprovers.find { it.approverUserId == admins[3].userName}.configuredTypes.sort() == ['Skill', 'User'].sort()

        res.data[3].skillId == skills[2].skillId
        res.data[3].userId == users[3].userName
        res.data[3].configuredApprovers.approverUserId.sort() == [admins[4].userName, admins[5].userName].sort()
        res.data[3].configuredApprovers.find { it.approverUserId == admins[4].userName}.configuredTypes.sort() == ['Skill'].sort()
        res.data[3].configuredApprovers.find { it.approverUserId == admins[5].userName}.configuredTypes.sort() == [userTagKey.toLowerCase()].sort()

        res.data[4].skillId == skills[3].skillId
        res.data[4].userId == users[4].userName
        res.data[4].configuredApprovers.approverUserId.sort() == [admins[6].userName].sort()
        res.data[4].configuredApprovers.find { it.approverUserId == admins[6].userName}.configuredTypes.sort() == ['User'].sort()

        res_page1.data[0].skillId == skills[4].skillId
        res_page1.data[0].userId == users[5].userName
        res_page1.data[0].configuredApprovers.approverUserId.sort() == [admins[7].userName].sort()
        res_page1.data[0].configuredApprovers.configuredTypes == [['Fallback']]
    }

    void "only project admin and root roles can get all requests with approvers"() {
        SkillsService root = createRootSkillService()

        List<SkillsService> allUsers = getRandomUsers(5).collect { createService(it) }
        SkillsService admin = allUsers[0]
        SkillsService approver = allUsers[1]
        SkillsService admin1 = allUsers[2]
        List<SkillsService> users = allUsers[3..4]
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(3, 1, 1, 100)
        skills.each {
            it.selfReportingType = SkillDef.SelfReportingType.Approval
        }
        admin.createProjectAndSubjectAndSkills(proj, subj, skills)

        admin.addProjectAdmin(proj.projectId, admin1.userName)
        admin.addUserRole(approver.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())

        users[0].addSkill([projectId: proj.projectId, skillId: skills[0].skillId])
        users[1].addSkill([projectId: proj.projectId, skillId: skills[1].skillId])

        admin.configureApproverForSkillId(proj.projectId, approver.userName, skills[1].skillId.toString())

        def res = admin.getApprovals(proj.projectId, 5, 1, 'requestedOn', false, '', '', true)
        assert res.data.size() == 2

        def res1 = admin1.getApprovals(proj.projectId, 5, 1, 'requestedOn', false, '', '', true)
        assert res1.data.size() == 2

        def res2 = root.getApprovals(proj.projectId, 5, 1, 'requestedOn', false, '', '', true)
        assert res2.data.size() == 2

        // approver with allRequest=false
        def res3 = approver.getApprovals(proj.projectId, 5, 1, 'requestedOn', false, '', '', false)
        assert res3.data.size() == 1 // only request assigned to this approver

        when:
        // approver with allRequest=true
        approver.getApprovals(proj.projectId, 5, 1, 'requestedOn', false, '', '', true)

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.resBody.contains("Only admins can view all requests")
    }

}
