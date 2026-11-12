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

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import skills.SpringBootApp
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.storage.model.auth.RoleName

@SpringBootTest(properties = ['skills.config.ui.userPageTagsToDisplay=organization/Org|department/Department|organization/Org|missingLabel|emptyLabel/|/emptyKey|extra/label/part| |'],
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = SpringBootApp)
class UserPageTagsIT extends DefaultIntSpec {

    Map project
    Map skill
    SkillsService learner
    SkillsService rootUser

    def setup() {
        learner = createService(getRandomUsers(1).first())
        rootUser = createRootSkillService()
        project = SkillsFactory.createProject(1)
        skillsService.createProject(project)
        skillsService.createSubject(SkillsFactory.createSubject(1, 1))
        List<Map> projectSkills = SkillsFactory.createSkills(3, 1, 1, 40)
        skillsService.createSkills(projectSkills)
        skill = projectSkills.first()
        rootUser.saveUserTag(learner.userName, 'organization', ['Org A', 'Org B'])
        rootUser.saveUserTag(learner.userName, 'department', ['Department A'])
        rootUser.saveUserTag(learner.userName, 'filteredTag', ['Filtered value'])
        rootUser.saveUserTag(learner.userName, 'Organization', ['wrong case'])
        ['missingLabel', 'emptyLabel', 'extra'].each { String key ->
            rootUser.saveUserTag(learner.userName, key, ['malformed entry'])
        }
    }

    def "project admin reporting for a target establishes participation but only exact allowlisted keys are disclosed"() {
        when:
        skillsService.getUserTags(project.projectId, learner.userName)

        then:
        SkillsClientException exception = thrown(SkillsClientException)
        exception.httpStatus == HttpStatus.BAD_REQUEST
        exception.resBody.contains('"errorCode":"BadParam"')
        exception.resBody.contains("User [${learner.userName.toLowerCase()}] is not an existing user of project [${project.projectId}]")

        when:
        skillsService.addSkill(skill, learner.userName)
        List<Map> tags = skillsService.getUserTags(project.projectId, learner.userName.toUpperCase())

        then:
        tags.sort { Map tag -> "${tag.key}:${tag.value}" } == [
                [key: 'department', value: 'Department A'],
                [key: 'organization', value: 'Org A'],
                [key: 'organization', value: 'Org B'],
        ]

        and: 'root access uses the same disclosure allowlist'
        rootUser.getUserTags(project.projectId, learner.userName).sort { Map tag -> "${tag.key}:${tag.value}" } == tags
    }

    def "an admin of another project cannot retrieve tags for this project's user"() {
        SkillsService otherAdmin = createService(getRandomUsers(1).first())
        otherAdmin.createProject(SkillsFactory.createProject(2))
        skillsService.addSkill(skill, learner.userName)

        when:
        otherAdmin.getUserTags(project.projectId, learner.userName)

        then:
        SkillsClientException exception = thrown(SkillsClientException)
        exception.httpStatus == HttpStatus.FORBIDDEN
    }

    def "a user with participation only in another project cannot have their tags retrieved"() {
        Map otherProject = SkillsFactory.createProject(2)
        skillsService.createProject(otherProject)
        skillsService.createSubject(SkillsFactory.createSubject(2, 1))
        List<Map> otherSkills = SkillsFactory.createSkills(3, 2, 1, 40)
        skillsService.createSkills(otherSkills)
        Map otherSkill = otherSkills.first()
        skillsService.addSkill(otherSkill, learner.userName)

        when:
        skillsService.getUserTags(project.projectId, learner.userName)

        then:
        SkillsClientException exception = thrown(SkillsClientException)
        exception.httpStatus == HttpStatus.BAD_REQUEST
        exception.resBody.contains('"errorCode":"BadParam"')
        exception.resBody.contains("User [${learner.userName.toLowerCase()}] is not an existing user of project [${project.projectId}]")
    }

    def "approver-only user can retrieve exact allowlisted tags for a learner in their project"() {
        SkillsService approver = createService(getRandomUsers(1).first())
        skillsService.addUserRole(approver.userName, project.projectId, RoleName.ROLE_PROJECT_APPROVER.name())
        skillsService.addSkill(skill, learner.userName)

        when:
        List<Map> tags = approver.getUserTags(project.projectId, learner.userName)

        then:
        tags.sort { Map tag -> "${tag.key}:${tag.value}" } == [
                [key: 'department', value: 'Department A'],
                [key: 'organization', value: 'Org A'],
                [key: 'organization', value: 'Org B'],
        ]
    }

    def "approver-only user of another project cannot retrieve tags for this project's learner"() {
        SkillsService approver = createService(getRandomUsers(1).first())
        Map otherProject = SkillsFactory.createProject(2)
        skillsService.createProject(otherProject)
        skillsService.addUserRole(approver.userName, otherProject.projectId, RoleName.ROLE_PROJECT_APPROVER.name())
        skillsService.addSkill(skill, learner.userName)

        when:
        approver.getUserTags(project.projectId, learner.userName)

        then:
        SkillsClientException exception = thrown(SkillsClientException)
        exception.httpStatus == HttpStatus.FORBIDDEN
    }

    def "ordinary learner cannot retrieve even their own allowlisted tags"() {
        skillsService.addSkill(skill, learner.userName)

        when:
        learner.getUserTags(project.projectId, learner.userName)

        then:
        SkillsClientException exception = thrown(SkillsClientException)
        exception.httpStatus == HttpStatus.FORBIDDEN
    }

}
