/**
 * Copyright 2024 SkillTree
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
package skills.intTests.copySubject

import groovy.json.JsonOutput
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import skills.intTests.copyProject.CopyIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.storage.model.Attachment
import skills.storage.model.QuizDefParent
import skills.storage.model.SkillDef
import skills.storage.model.auth.RoleName
import skills.utils.GroovyToJavaByteUtils

import static skills.intTests.utils.SkillsFactory.*

class CopySubjectValidationSpecs extends CopyIntSpec {

    def "validate subject name is unique - name lowercase"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [])

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        p2subj1.subjectId = 'someOther'
        p2subj1.name = p2subj1.name.toLowerCase()
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [])

        when:
        skillsService.copySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Subject with name [${p1subj1.name}] already exists")
    }

    def "validate subject name is unique - name uppercase"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [])

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        p2subj1.subjectId = 'someOther'
        p2subj1.name = p2subj1.name.toUpperCase()
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [])

        when:
        skillsService.copySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Subject with name [${p1subj1.name}] already exists")
    }

    def "validate subject id is unique - id lowercase"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [])

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        p2subj1.name = 'Other name'
        p2subj1.subjectId = p2subj1.subjectId.toLowerCase()
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [])

        when:
        skillsService.copySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Id [${p1subj1.subjectId}] already exists in project [${p2.projectId}]")
    }

    def "validate subject id is unique - id uppercase"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [])

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        p2subj1.name = 'Other name'
        p2subj1.subjectId = p2subj1.subjectId.toUpperCase()
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [])

        when:
        skillsService.copySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Id [${p1subj1.subjectId}] already exists in project [${p2.projectId}]")
    }

    def "validate subject id is unique due to an existing skill id"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [])

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 2)
        def p2skill1 = createSkill(2, 2, 1)
        p2skill1.skillId = p1subj1.subjectId
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [p2skill1])

        when:
        skillsService.copySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Id [${p1subj1.subjectId}] already exists in project [${p2.projectId}]")
    }

    def "validate that there is no skill id collisions"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Subj1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Subj1Skills)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 2)
        def p2Subj1Skills = createSkills(3, 2, 2, 100)
        p2Subj1Skills[1].skillId = p1Subj1Skills[2].skillId
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, p2Subj1Skills)

        when:
        skillsService.copySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("ID [${p1Subj1Skills[2].skillId}] already exists in the project [${p2.projectId}]")
    }

    def "validate that there is no skill name collisions"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Subj1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Subj1Skills)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 2)
        def p2Subj1Skills = createSkills(3, 2, 2, 100)
        p2Subj1Skills[1].name = p1Subj1Skills[0].name
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, p2Subj1Skills)

        when:
        skillsService.copySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Skill with name [${p2Subj1Skills[1].name}] already exists in the project [${p2.projectId}]")
    }

    def "validate orig proj exist"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [])

        def p2 = createProject(2)
        skillsService.createProject(p2)

        when:
        skillsService.copySubjectDefIntoAnotherProject(p1.projectId + "a", p1subj1.subjectId, p2.projectId)
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.httpStatus == HttpStatus.FORBIDDEN
    }

    def "validate dest proj exist"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [])

        def p2 = createProject(2)
        skillsService.createProject(p2)

        when:
        String badProjectId = p2.projectId + "a"
        skillsService.copySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, badProjectId)
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Project with id [${badProjectId}] does not exist")
    }

    def "validate subject exist"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [])

        def p2 = createProject(2)
        skillsService.createProject(p2)

        when:
        skillsService.copySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId + "a", p2.projectId )
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Subject with id [${p1subj1.subjectId + "a"}] does not exist")
    }

    def "validate subject id is provided"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def p2 = createProject(2)
        skillsService.createProject(p2)

        when:
        skillsService.copySubjectDefIntoAnotherProject(p1.projectId, p1Skills[0].skillId, p2.projectId)
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Provided id [${p1Skills[0].skillId}] is not for a subject")
    }

    def "user with approver role for the destination project does not have permission to copy"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        SkillsService otherUser = createService(getRandomUsers(1).first())
        def p2 = createProject(2)
        otherUser.createProject(p2)
        otherUser.addUserRole(skillsService.userName, p2.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())

        when:
        skillsService.copySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("User [${skillsService.userName}] is not an admin for destination project [${p2.projectId}]")
    }

    def "must be an admin of destination project to copy"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        SkillsService otherUser = createService(getRandomUsers(1).first())
        def p2 = createProject(2)
        otherUser.createProject(p2)

        when:
        skillsService.copySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("User [${skillsService.userName}] is not an admin for destination project [${p2.projectId}]")
    }

}
