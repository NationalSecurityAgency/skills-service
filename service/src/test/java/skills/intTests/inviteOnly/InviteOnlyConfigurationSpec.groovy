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

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory

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
}
