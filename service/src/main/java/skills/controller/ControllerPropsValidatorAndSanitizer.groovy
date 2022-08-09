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
package skills.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.PublicProps
import skills.controller.exceptions.SkillsValidator
import skills.controller.request.model.ProjectRequest
import skills.services.IdFormatValidator
import skills.utils.InputSanitizer

@Component
class ControllerPropsValidatorAndSanitizer {

    @Autowired
    PublicPropsBasedValidator propsBasedValidator

    String validateAndSanitizeProjectId(String projectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        IdFormatValidator.validate(projectId)

        propsBasedValidator.validateMaxStrLength(PublicProps.UiProp.maxIdLength, "Project Id", projectId)
        propsBasedValidator.validateMinStrLength(PublicProps.UiProp.minIdLength, "Project Id", projectId)

        return InputSanitizer.sanitize(projectId)
    }

    ProjectRequest validateAndSanitizeProjectRequest(ProjectRequest projectRequest) {
        SkillsValidator.isNotBlank(projectRequest.projectId, "Project Id")
        SkillsValidator.isNotBlank(projectRequest.name, " Name")

        IdFormatValidator.validate(projectRequest.projectId)

        propsBasedValidator.validateProjectRequest(projectRequest)

        projectRequest.name = InputSanitizer.sanitize(projectRequest.name)?.trim()
        projectRequest.projectId = InputSanitizer.sanitize(projectRequest.projectId)
        return projectRequest
    }

}
