/**
 * Copyright 2022 SkillTree
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
package skills.intTests.inviteOnly

import org.springframework.http.HttpStatus
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.utils.WaitFor

class InviteOnlyAccessSpec extends InviteOnlyBaseSpec {

    def "cannot access a project that has been configured for invite only without accepting invite"() {
        def proj = SkillsFactory.createProject(99)
        def subj = SkillsFactory.createSubject(99)
        def skill = SkillsFactory.createSkill(99, 1)
        skill.pointIncrement = 200

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skill)

        when:

        skillsService.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])
        def user = getRandomUsers(1, true)[0]
        def newService = createService(user)

        newService.apiGetUserLevelForProject(proj.projectId, user)

        then:
        def err = thrown(SkillsClientException)
        err.httpStatus == HttpStatus.FORBIDDEN
    }

    def "cannot report a skill event for a project that has been configured for invite only without accepting invite"() {
        def proj = SkillsFactory.createProject(99)
        def subj = SkillsFactory.createSubject(99)
        def skill = SkillsFactory.createSkill(99, 1)
        skill.pointIncrement = 200

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skill)

        when:

        skillsService.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])
        def user = getRandomUsers(1, true)[0]
        def newService = createService(user)

        newService.addSkill([projectId: proj.projectId, skillId: skill.skillId])

        then:
        def err = thrown(SkillsClientException)
        err.httpStatus == HttpStatus.FORBIDDEN
    }

    def "can access invite only project after accepting invite"() {
        def proj = SkillsFactory.createProject(99)
        def subj = SkillsFactory.createSubject(99)
        def skill = SkillsFactory.createSkill(99, 1)
        skill.pointIncrement = 200

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skill)
        skillsService.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])

        def user = getRandomUsers(1, true)[0]
        def newService = createService(user)

        when:
        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT5M", recipients: ["someemail@email.foo"]])
        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }

        def email = greenMail.getReceivedMessages()[0]
        def invite = extractInviteFromEmail(email.content)

        newService.joinProject(proj.projectId, invite)

        def res = newService.apiGetUserLevelForProject(proj.projectId, null)

        then:
        res == 0
    }

    def "can report a skill event for a project that has been configured for invite only without accepting invite"() {
        def proj = SkillsFactory.createProject(99)
        def subj = SkillsFactory.createSubject(99)
        def skill = SkillsFactory.createSkill(99, 1)
        skill.pointIncrement = 200

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skill)
        skillsService.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])

        def user = getRandomUsers(1, true)[0]
        def newService = createService(user)

        when:
        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT5M", recipients: ["someemail@email.foo"]])
        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }

        def email = greenMail.getReceivedMessages()[0]
        def invite = extractInviteFromEmail(email.content)

        newService.joinProject(proj.projectId, invite)
        def res = newService.addSkill([projectId: proj.projectId, skillId: skill.skillId])

        then:
        res
        res.statusCode == HttpStatus.OK
        res.body.success == true
    }

    def "cannot access invite only project after access is revoked"() {
        def proj = SkillsFactory.createProject(99)
        def subj = SkillsFactory.createSubject(99)
        def skill = SkillsFactory.createSkill(99, 1)
        skill.pointIncrement = 200

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skill)
        skillsService.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])

        def user = getRandomUsers(1, true)[0]
        def newService = createService(user)

        when:
        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT5M", recipients: ["someemail@email.foo"]])
        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }

        def email = greenMail.getReceivedMessages()[0]
        def invite = extractInviteFromEmail(email.content)

        newService.joinProject(proj.projectId, invite)

        def res = newService.apiGetUserLevelForProject(proj.projectId, null)
        //ROLE_PRIVATE_PROJECT_USER
        skillsService.revokeInviteOnlyProjectAccess(proj.projectId, user)
        def res2 = newService.apiGetUserLevelForProject(proj.projectId, null)

        then:
        res == 0
        def err = thrown(SkillsClientException)
        err.httpStatus == HttpStatus.FORBIDDEN
    }


}
