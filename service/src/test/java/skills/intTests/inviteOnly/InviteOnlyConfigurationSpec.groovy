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

import org.springframework.core.io.ClassPathResource
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.storage.model.auth.RoleName

class InviteOnlyConfigurationSpec extends DefaultIntSpec {

    def "project cannot be configured as invite only if discoverable"() {
        def proj = SkillsFactory.createProject(99)
        def subj = SkillsFactory.createSubject(99)
        def skill = SkillsFactory.createSkill(99, 1)
        skill.pointIncrement = 200

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skill)

        skillsService.changeSetting(proj.projectId, "production.mode.enabled", [projectId: proj.projectId, setting: "production.mode.enabled", value: "true"])

        when:
        skillsService.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])

        then:
        def err = thrown(SkillsClientException)
        err.message.contains("explanation:invite_only can only be enabled if production.mode.enabled is false")
    }

    def "project cannot be configured as discoverable if already invite only"() {
        def proj = SkillsFactory.createProject(99)
        def subj = SkillsFactory.createSubject(99)
        def skill = SkillsFactory.createSkill(99, 1)
        skill.pointIncrement = 200

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skill)

        skillsService.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])

        when:
        skillsService.changeSetting(proj.projectId, "production.mode.enabled", [projectId: proj.projectId, setting: "production.mode.enabled", value: "true"])

        then:
        def err = thrown(SkillsClientException)
        err.message.contains("explanation:production.mode.enabled can only be enabled if invite_only is false")
    }

    def "invite only project can access appropriate API functions as project admin"() {
        def proj = SkillsFactory.createProject(1)
        skillsService.createProject(proj)
        ClassPathResource resource = new ClassPathResource("/dot2.png")
        def file = resource.getFile()
        skillsService.uploadIcon([projectId:(proj.projectId)], file)
        skillsService.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])

        def subj = SkillsFactory.createSubject(1, 1)
        def skill = SkillsFactory.createSkill(1, 1, 1)
        skill.pointIncrement = 200

        skillsService.createSubject(subj)
        skillsService.createSkill(skill)

        when:
        def projectName = skillsService.lookupMyProjectName(proj.projectId)
        def subjectDescriptions = skillsService.getSubjectDescriptions(proj.projectId, subj.subjectId)
        def userLevels = skillsService.getUserLevel(proj.projectId)
        def myProgress = skillsService.getMyProgressSummary()
        def usersPerLevel = skillsService.getUsersPerLevel(proj.projectId)
        def summary = skillsService.getSkillSummary(skillsService.userName, proj.projectId)
        def otherSummary = skillsService.getSkillSummary(skillsService.userName, proj.projectId, subj.subjectId)
        def customIconCss = skillsService.getCustomIconCssForProject(proj.projectId)

        then:
        projectName
        subjectDescriptions
        userLevels == 0
        myProgress
        usersPerLevel
        summary
        otherSummary
        customIconCss
    }

    def "invite only project can access appropriate API functions as a project approver"() {
        def user1Service = createService(getRandomUsers(1, true)[0])
        def proj = SkillsFactory.createProject(1)
        skillsService.createProject(proj)
        ClassPathResource resource = new ClassPathResource("/dot2.png")
        def file = resource.getFile()
        skillsService.uploadIcon([projectId:(proj.projectId)], file)
        skillsService.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])
        skillsService.addUserRole(user1Service.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())

        def subj = SkillsFactory.createSubject(1, 1)
        def skill = SkillsFactory.createSkill(1, 1, 1)
        skill.pointIncrement = 200

        skillsService.createSubject(subj)
        skillsService.createSkill(skill)

        when:
        def projectName = user1Service.lookupMyProjectName(proj.projectId)
        def subjectDescriptions = user1Service.getSubjectDescriptions(proj.projectId, subj.subjectId)
        def userLevels = user1Service.getUserLevel(proj.projectId)
        def myProgress = user1Service.getMyProgressSummary()
        def usersPerLevel = user1Service.getUsersPerLevel(proj.projectId)
        def summary = user1Service.getSkillSummary(user1Service.userName, proj.projectId)
        def otherSummary = user1Service.getSkillSummary(user1Service.userName, proj.projectId, subj.subjectId)
        def customIconCss = user1Service.getCustomIconCssForProject(proj.projectId)

        then:
        projectName
        subjectDescriptions
        userLevels == 0
        myProgress
        usersPerLevel
        summary
        otherSummary
        customIconCss
    }
}
