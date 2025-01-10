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
package skills.intTests.community

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.storage.model.SkillDef

import static skills.intTests.utils.SkillsFactory.createProject

class DescriptionValidatorCommunitySpecs extends DefaultIntSpec {

    String notValidDefault = "has jabberwocky"
    String notValidProtectedCommunity = "has divinedragon"

    def "description validator for community without projectId"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        when:
        def defaultResValid = skillsService.checkCustomDescriptionValidation(notValidProtectedCommunity)
        def defaultResInValid = skillsService.checkCustomDescriptionValidation(notValidDefault)

        then:
        defaultResValid.body.valid
        !defaultResInValid.body.valid
        defaultResInValid.body.msg == "paragraphs may not contain jabberwocky"
    }

    def "description validator for community with projectId"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def p1 = createProject(1)
        p1.enableProtectedUserCommunity = true
        pristineDragonsUser.createProject(p1)

        def p2 = createProject(2)
        pristineDragonsUser.createProject(p2)

        when:
        def communityValid = pristineDragonsUser.checkCustomDescriptionValidation(notValidDefault, p1.projectId)
        def communityInvalidValid = pristineDragonsUser.checkCustomDescriptionValidation(notValidProtectedCommunity, p1.projectId)

        def communityValidP2 = pristineDragonsUser.checkCustomDescriptionValidation(notValidProtectedCommunity, p2.projectId)
        def communityInvalidValidP2 = pristineDragonsUser.checkCustomDescriptionValidation(notValidDefault, p2.projectId)
        then:
        communityValid.body.valid
        !communityInvalidValid.body.valid
        communityInvalidValid.body.msg == "May not contain divinedragon word"

        communityValidP2.body.valid
        !communityInvalidValidP2.body.valid
        communityInvalidValidP2.body.msg == "paragraphs may not contain jabberwocky"
    }

    def "description validator for community with useProtectedCommunityValidator"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def p1 = createProject(1)
        p1.enableProtectedUserCommunity = true
        pristineDragonsUser.createProject(p1)

        when:
        def communityValid = pristineDragonsUser.checkCustomDescriptionValidation(notValidDefault, null, true)
        def communityInvalidValid = pristineDragonsUser.checkCustomDescriptionValidation(notValidProtectedCommunity, null, true)

        def defaultResValid = pristineDragonsUser.checkCustomDescriptionValidation(notValidProtectedCommunity, null, false)
        def defaultResInvalid = pristineDragonsUser.checkCustomDescriptionValidation(notValidDefault, null, false)
        then:
        communityValid.body.valid
        !communityInvalidValid.body.valid
        communityInvalidValid.body.msg == "May not contain divinedragon word"

        defaultResValid.body.valid
        !defaultResInvalid.body.valid
        defaultResInvalid.body.msg == "paragraphs may not contain jabberwocky"
    }

    def "description validator for community with projectId overrides useProtectedCommunityValidator"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def p1 = createProject(1)
        p1.enableProtectedUserCommunity = true
        pristineDragonsUser.createProject(p1)

        def p2 = createProject(2)
        pristineDragonsUser.createProject(p2)

        when:
        def communityValid = pristineDragonsUser.checkCustomDescriptionValidation(notValidDefault, p1.projectId, true)
        def communityInvalidValid = pristineDragonsUser.checkCustomDescriptionValidation(notValidProtectedCommunity, p1.projectId, true)

        def communityValidP2 = pristineDragonsUser.checkCustomDescriptionValidation(notValidProtectedCommunity, p2.projectId, true)
        def communityInvalidValidP2 = pristineDragonsUser.checkCustomDescriptionValidation(notValidDefault, p2.projectId,true)
        then:
        communityValid.body.valid
        !communityInvalidValid.body.valid
        communityInvalidValid.body.msg == "May not contain divinedragon word"

        communityValidP2.body.valid
        !communityInvalidValidP2.body.valid
        communityInvalidValidP2.body.msg == "paragraphs may not contain jabberwocky"
    }

    def "project paragraph custom validation on create"(){
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def proj = SkillsFactory.createProject()
        proj.enableProtectedUserCommunity = true
        proj.description = notValidProtectedCommunity

        when:
        pristineDragonsUser.createProject(proj)

        then:
        def exception = thrown(SkillsClientException)
        exception.message.contains("May not contain divinedragon word")
    }

    def "project paragraph custom validation on update"(){
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def proj = SkillsFactory.createProject()
        proj.enableProtectedUserCommunity = true
        proj.description = notValidDefault

        when:
        def communityValid = pristineDragonsUser.createProject(proj)

        proj.description = notValidProtectedCommunity
        pristineDragonsUser.updateProject(proj)

        then:
        def exception = thrown(SkillsClientException)
        exception.message.contains("May not contain divinedragon word")

        communityValid.body.success
    }

    def "subject paragraph custom validation on create"(){
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def proj = SkillsFactory.createProject()
        proj.enableProtectedUserCommunity = true
        pristineDragonsUser.createProject(proj)

        when:
        def subj = SkillsFactory.createSubject()
        subj.description = notValidProtectedCommunity
        def communityValid = pristineDragonsUser.createSubject(subj)

        then:
        def exception = thrown(SkillsClientException)
        exception.message.contains("May not contain divinedragon word")
    }

    def "subject paragraph custom validation on update"(){
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def proj = SkillsFactory.createProject()
        proj.enableProtectedUserCommunity = true
        pristineDragonsUser.createProject(proj)

        when:
        def subj = SkillsFactory.createSubject()
        subj.description = notValidDefault
        def communityValid = pristineDragonsUser.createSubject(subj)

        subj.description = notValidProtectedCommunity
        pristineDragonsUser.updateSubject(subj)

        then:
        def exception = thrown(SkillsClientException)
        exception.message.contains("May not contain divinedragon word")

        communityValid.body.success
    }

    def "skill paragraph custom validation on create"(){
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def proj = SkillsFactory.createProject()
        proj.enableProtectedUserCommunity = true
        pristineDragonsUser.createProject(proj)

        def subj = SkillsFactory.createSubject()
        pristineDragonsUser.createSubject(subj)

        when:
        def skill = SkillsFactory.createSkill()
        skill.description = notValidProtectedCommunity
        def communityValid = pristineDragonsUser.createSkill(skill)

        then:
        def exception = thrown(SkillsClientException)
        exception.message.contains("May not contain divinedragon word")
    }

    def "skill paragraph custom validation on update"(){
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def proj = SkillsFactory.createProject()
        proj.enableProtectedUserCommunity = true
        pristineDragonsUser.createProject(proj)

        def subj = SkillsFactory.createSubject()
        pristineDragonsUser.createSubject(subj)

        when:
        def skill = SkillsFactory.createSkill()
        skill.description = notValidDefault
        def communityValid = pristineDragonsUser.createSkill(skill)

        skill.description = notValidProtectedCommunity
        pristineDragonsUser.updateSkill(skill)

        then:
        def exception = thrown(SkillsClientException)
        exception.message.contains("May not contain divinedragon word")

        communityValid.body.success
    }

    def "badge paragraph custom validation create"(){
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def proj = SkillsFactory.createProject()
        proj.enableProtectedUserCommunity = true
        pristineDragonsUser.createProject(proj)

        when:
        def badge = SkillsFactory.createBadge()
        badge.description = notValidProtectedCommunity
        def communityValid = pristineDragonsUser.createBadge(badge)

        then:
        def exception = thrown(SkillsClientException)
        exception.message.contains("May not contain divinedragon word")
    }

    def "badge paragraph custom validation update"(){
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def proj = SkillsFactory.createProject()
        proj.enableProtectedUserCommunity = true
        pristineDragonsUser.createProject(proj)

        when:
        def badge = SkillsFactory.createBadge()
        badge.description = notValidDefault
        def communityValid = pristineDragonsUser.createBadge(badge)

        badge.description = notValidProtectedCommunity
        pristineDragonsUser.updateBadge(badge)

        then:
        def exception = thrown(SkillsClientException)
        exception.message.contains("May not contain divinedragon word")

        communityValid.body.success
    }

    def "skill approval message custom validation UC protected"(){
        List<String> users = getRandomUsers(2)
        Date date = new Date() - 60

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def proj = SkillsFactory.createProject()
        proj.enableProtectedUserCommunity = true
        pristineDragonsUser.createProject(proj)

        def subj = SkillsFactory.createSubject()
        pristineDragonsUser.createSubject(subj)

        def skill = SkillsFactory.createSkill()
        skill.pointIncrement = 200
        skill.selfReportingType = SkillDef.SelfReportingType.Approval
        pristineDragonsUser.createSkill(skill)

        when:
        pristineDragonsUser.addSkill([projectId: proj.projectId, skillId: skill.skillId], users[0], date, "Please approve this!")
        def approvalsEndpointRes = pristineDragonsUser.getApprovals(proj.projectId, 5, 1, 'requestedOn', false)

        List approvals = approvalsEndpointRes.data.sort({ it.userId })

        pristineDragonsUser.approve(proj.projectId, [approvals[0].id], notValidProtectedCommunity)

        then:
        def exception = thrown(SkillsClientException)
        exception.message.contains("May not contain divinedragon word")
    }

    def "skill approval message custom validation default protected"(){
        List<String> users = getRandomUsers(2)
        Date date = new Date() - 60

        def proj = SkillsFactory.createProject()
        skillsService.createProject(proj)

        def subj = SkillsFactory.createSubject()
        skillsService.createSubject(subj)

        def skill = SkillsFactory.createSkill()
        skill.pointIncrement = 200
        skill.selfReportingType = SkillDef.SelfReportingType.Approval
        skillsService.createSkill(skill)

        when:
        skillsService.addSkill([projectId: proj.projectId, skillId: skill.skillId], users[0], date, "Please approve this!")
        def approvalsEndpointRes = skillsService.getApprovals(proj.projectId, 5, 1, 'requestedOn', false)

        List approvals = approvalsEndpointRes.data.sort({ it.userId })

        skillsService.approve(proj.projectId, [approvals[0].id], notValidDefault)

        then:
        def exception = thrown(SkillsClientException)
        exception.message.contains("may not contain jabberwocky")
    }

    def "only community member can call description validator for community with useProtectedCommunityValidator"() {
        when:
        skillsService.checkCustomDescriptionValidation(notValidDefault, null, true)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("User [${skillsService.userName}] is not allowed to validate using user community validation")
    }

    def "only community member can call description validator for community with projectId that belongs to that community"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def p1 = createProject(1)
        p1.enableProtectedUserCommunity = true
        pristineDragonsUser.createProject(p1)

        when:
        skillsService.checkCustomDescriptionValidation(notValidDefault, p1.projectId)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("User [${skillsService.userName}] is not allowed to validate using user community validation")
    }

}
