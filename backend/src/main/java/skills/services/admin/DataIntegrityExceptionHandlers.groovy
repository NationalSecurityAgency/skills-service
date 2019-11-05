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
