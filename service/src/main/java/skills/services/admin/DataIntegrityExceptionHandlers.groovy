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


import skills.controller.exceptions.DataIntegrityViolationExceptionHandler

class DataIntegrityExceptionHandlers {

    static DataIntegrityViolationExceptionHandler dataIntegrityViolationExceptionHandler =
            new DataIntegrityViolationExceptionHandler([
                    "index_project_definition_name" : "Provided project name already exist.",
                    "index_project_definition_project_id": "Provided project id already exist.",
            ])

    static DataIntegrityViolationExceptionHandler subjectDataIntegrityViolationExceptionHandler = crateSkillDefBasedDataIntegrityViolationExceptionHandler("subject")
    static DataIntegrityViolationExceptionHandler badgeDataIntegrityViolationExceptionHandler = crateSkillDefBasedDataIntegrityViolationExceptionHandler("badge")
    static DataIntegrityViolationExceptionHandler skillDataIntegrityViolationExceptionHandler = crateSkillDefBasedDataIntegrityViolationExceptionHandler("skill")
    private static DataIntegrityViolationExceptionHandler crateSkillDefBasedDataIntegrityViolationExceptionHandler(String type) {
        new DataIntegrityViolationExceptionHandler([
                "index_skill_definition_project_id_skill_id" : "Provided ${type} id already exist.".toString(),
                "index_skill_definition_project_id_name": "Provided ${type} name already exist.".toString(),
                "index_skill_definition_project_id_skill_id_type" : "Provided ${type} id already exist.".toString(),
                "index_skill_definition_project_id_name_type": "Provided ${type} name already exist.".toString(),
                "index_global_badge_level_definition_proj_skill_level" : "Provided project already has a level assigned for this global badge.",
                "index_skill_relationship_definition_parent_child_type": "Provided skill id has already been added to this global badge.",
        ])
    }
}
