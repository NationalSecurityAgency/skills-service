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
package skills.intTests.copyProject

import org.springframework.beans.factory.annotation.Value
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import spock.lang.IgnoreIf

import static skills.intTests.utils.SkillsFactory.createProject

class CopyProjectValidationSpecs extends DefaultIntSpec {

    @Value('#{"${skills.config.ui.maxProjectsPerAdmin}"}')
    int maxProjectsPerUser

    def "validate project name is unique"() {
        def p1 = createProject(1)
        skillsService.createProject(p1)
        when:
        def projToCopy = createProject(2)
        projToCopy.name = p1.name
        skillsService.copyProject(p1.projectId, projToCopy)
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Project with name [${p1.name}] already exists")
    }

    def "validate project id is unique"() {
        def p1 = createProject(1)
        skillsService.createProject(p1)
        when:
        def projToCopy = createProject(2)
        projToCopy.projectId = p1.projectId
        skillsService.copyProject(p1.projectId, projToCopy)
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Project with id [${p1.projectId}] already exists")
    }

    // pki throws  403 – Forbidden instead
    @IgnoreIf({ env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "validate project to copy exist"() {
        def p1 = createProject(1)
        when:
        def projToCopy = createProject(2)
        projToCopy.name = p1.name
        skillsService.copyProject(p1.projectId, projToCopy)
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("You do not have permission to view/manage this Project OR this Project does not exist")
    }

    def "validate payload has a name"() {
        def p1 = createProject(1)
        skillsService.createProject(p1)
        when:
        def projToCopy = [projectId: "newId", nameNot: "new name"]
        skillsService.copyProject(p1.projectId, projToCopy)
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Name was not provided")
    }

    def "validate payload has a projectId"() {
        def p1 = createProject(1)
        skillsService.createProject(p1)
        when:
        def projToCopy = [projectIdNot: "newId", name: "new name"]
        skillsService.copyProject(p1.projectId, projToCopy)
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Project Id was not provided")
    }

    def "project name custom validation"() {
        def p1 = createProject(1)
        skillsService.createProject(p1)
        when:
        def projToCopy = [projectId: "Proj42", name: "Jabberwocky project"]
        skillsService.copyProject(p1.projectId, projToCopy)
        then:
        def exception = thrown(SkillsClientException)
        exception.message.contains("names may not contain jabberwocky")
    }

    def "validate user cannot create more than configured max projects"() {
        def lastProj
        (1..maxProjectsPerUser).each {
            lastProj = createProject(it)
            skillsService.createProject(lastProj)
        }
        when:
        def projToCopy = createProject(777)
        skillsService.copyProject(lastProj.projectId, projToCopy)
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Each user is limited to [${maxProjectsPerUser}] Project")
    }
}
