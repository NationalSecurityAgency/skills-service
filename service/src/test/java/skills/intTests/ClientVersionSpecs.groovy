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
package skills.intTests

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import skills.controller.result.model.ProjectError
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.services.ProjectErrorService

class ClientVersionSpecs extends DefaultIntSpec {

    @Autowired
    ProjectErrorService projectErrorService;

    def "report skills-client version"() {
        def proj = SkillsFactory.createProject(1)
        skillsService.createProject(proj)
        when:
        def result = skillsService.reportClientVersion(proj.projectId, "@skilltree/skills-client-fake-1.0.0")
        then:
        result
        result.statusCode == HttpStatus.OK
        result.body.success == true
    }

    def "out of date version logs an error"() {
        def proj = SkillsFactory.createProject(1)
        skillsService.createProject(proj)
        when:
        def result = skillsService.reportClientVersion(proj.projectId, "@skilltree/skills-client-fake-1.0.0")
        then:
        PageRequest pageRequest = PageRequest.of(1 - 1, 10)
        projectErrorService.countOfErrorsForProject(proj.projectId) == 1
        ProjectError err = projectErrorService.getAllErrorsForProject(proj.projectId, pageRequest).getData().get(0)
        err.error.contains("The version used (@skilltree/skills-client-fake-1.0.0) is out of date")
        err.errorType == "VersionOutOfDate"
    }
}
