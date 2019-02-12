package skills.service.controller.request.model

class SkillDefForDependencyRes {

    Integer id

    String name
    String skillId

    String projectId

    int totalPoints

    // will only be non-null for cross-project shared skills
    String otherProjectName
    String otherProjectId
}
