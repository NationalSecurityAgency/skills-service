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
package skills.services.admin

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.storage.model.ProjDef
import skills.storage.repos.ProjDefRepo

@Component
class ServiceValidatorHelper {

    @Autowired
    ProjDefRepo projDefRepo

    void validateProjectIdDoesNotExist(String projectId) {
        ProjDef idExist = projDefRepo.findByProjectIdIgnoreCase(projectId)
        if (idExist) {
            throw new SkillException("Project with id [${projectId}] already exists! Sorry!", projectId, null, ErrorCode.ConstraintViolation)
        }
    }

    void validateProjectNameDoesNotExist(String projectName, String projectId) {
        ProjDef nameExist = projDefRepo.findByNameIgnoreCase(projectName)
        if (nameExist) {
            throw new SkillException("Project with name [${projectName}] already exists! Sorry!", projectId, null, ErrorCode.ConstraintViolation)
        }
    }

}
