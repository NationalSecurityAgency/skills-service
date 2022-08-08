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
import org.springframework.http.HttpStatus
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.services.VersionService

class ClientVersionSpecs extends DefaultIntSpec {

    @Autowired
    VersionService versionService;

    String currentVersion

    def setup() {
        currentVersion = versionService.getCurrentVersion();
    }

    def "report current skills-client version"() {
        def proj = SkillsFactory.createProject(1)
        skillsService.createProject(proj)

        when:
        def result = skillsService.reportClientVersion(proj.projectId, currentVersion)

        then:
        result
        result.statusCode == HttpStatus.OK
        result.body.success == true
        def errors = skillsService.getProjectErrors(proj.projectId, 10, 1, "errorType", true)
        errors.count == 0
    }

    def "out of date version logs an error"() {
        def proj = SkillsFactory.createProject(1)
        skillsService.createProject(proj)
        when:
        def result = skillsService.reportClientVersion(proj.projectId, "@skilltree/skills-client-fake-1.0.0")

        then:
        def errors = skillsService.getProjectErrors(proj.projectId, 10, 1, "errorType", true)
        errors.count == 1
        errors.data[0].errorType == "VersionOutOfDate"
        errors.data[0].error.contains("The version used (@skilltree/skills-client-fake-1.0.0) is out of date")
    }

    def "Reporting same out of date version multiple times only logs one error"() {
        def proj = SkillsFactory.createProject(1)
        skillsService.createProject(proj)

        when:
        def result = skillsService.reportClientVersion(proj.projectId, "@skilltree/skills-client-fake-1.0.0")
        result = skillsService.reportClientVersion(proj.projectId, "@skilltree/skills-client-fake-1.0.0")
        result = skillsService.reportClientVersion(proj.projectId, "@skilltree/skills-client-fake-1.0.0")

        then:
        def errors = skillsService.getProjectErrors(proj.projectId, 10, 1, "errorType", true)
        errors.count == 1
    }

    def "Reporting multiple different out of date versions produce multiple errors"() {
        def proj = SkillsFactory.createProject(1)
        skillsService.createProject(proj)

        when:
        def result = skillsService.reportClientVersion(proj.projectId, "@skilltree/skills-client-fake-1.0.0")
        result = skillsService.reportClientVersion(proj.projectId, "@skilltree/skills-client-fake-1.2.3")
        result = skillsService.reportClientVersion(proj.projectId, "@skilltree/skills-client-framework-1.0.0")
        result = skillsService.reportClientVersion(proj.projectId, currentVersion)

        then:
        def errors = skillsService.getProjectErrors(proj.projectId, 10, 1, "errorType", true)
        errors.count == 3
    }
}
