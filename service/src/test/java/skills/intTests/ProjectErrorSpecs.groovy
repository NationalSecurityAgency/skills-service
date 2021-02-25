/**
 * Copyright 2021 SkillTree
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

class ProjectErrorSpecs extends DefaultIntSpec {

    def "error is recorded when invalid skill is reported"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skill = SkillsFactory.createSkill(1,1,1,0,5,0,50)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skill)

        when:

        skillsService.addSkill(skill, getRandomUsers(0).first())
        try {
            skillsService.addSkill([projectId: proj.projectId, skillId: "notASkill"], getRandomUsers(1).first())
        } catch (SkillsClientException skillsClientException) {
            //expected to fail
        }

        def errors = skillsService.getProjectErrors(proj.projectId, 10, 1, 'lastSeen', false)

        then:
        errors
        errors.totalCount == 1
        errors.data.size() == 1
        errors.data[0].projectId == proj.projectId
        errors.data[0].errorType == "SkillNotFound"
        errors.data[0].error == "notASkill"
        errors.data[0].count == 1
    }

    def "delete all errors"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skill = SkillsFactory.createSkill(1,1,1,0,5,0,50)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skill)

        when:

        skillsService.addSkill(skill, getRandomUsers(0).first())
        try {
            skillsService.addSkill([projectId: proj.projectId, skillId: "notASkill"], getRandomUsers(1).first())
        } catch (SkillsClientException skillsClientException) {
            //expected to fail
        }

        try {
            skillsService.addSkill([projectId: proj.projectId, skillId: "this is not a skill id"], getRandomUsers(1).first())
        } catch (SkillsClientException skillsClientException) {
            //expected to fail
        }

        def errorsBeforeDelete = skillsService.getProjectErrors(proj.projectId, 10, 1, "lastSeen", false)
        skillsService.deleteAllProjectErrors(proj.projectId)
        def errorsAfterDelete = skillsService.getProjectErrors(proj.projectId, 10, 1, "lastSeen", false)

        then:
        errorsBeforeDelete.totalCount == 2
        errorsBeforeDelete.data.size() == 2
        errorsAfterDelete.data.size() == 0
        errorsAfterDelete.totalCount == 0
    }

    def "delete one specific error"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skill = SkillsFactory.createSkill(1,1,1,0,5,0,50)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skill)

        when:

        skillsService.addSkill(skill, getRandomUsers(0).first())
        try {
            skillsService.addSkill([projectId: proj.projectId, skillId: "notASkill"], getRandomUsers(1).first())
        } catch (SkillsClientException skillsClientException) {
            //expected to fail
        }

        try {
            skillsService.addSkill([projectId: proj.projectId, skillId: "this is not a skill id"], getRandomUsers(1).first())
        } catch (SkillsClientException skillsClientException) {
            //expected to fail
        }

        def errorsBeforeDelete = skillsService.getProjectErrors(proj.projectId, 10, 1, "lastSeen", false)
        skillsService.deleteSpecificProjectError(proj.projectId, "SkillNotFound", "this is not a skill id")
        def errorsAfterDelete = skillsService.getProjectErrors(proj.projectId, 10, 1, "lastSeen", false)

        then:
        errorsBeforeDelete.totalCount == 2
        errorsBeforeDelete.data.size() == 2
        errorsAfterDelete.totalCount == 1
        errorsAfterDelete.data.size() == 1
    }

}
