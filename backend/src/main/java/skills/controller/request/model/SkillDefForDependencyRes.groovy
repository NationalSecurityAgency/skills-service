package skills.controller.request.model

class SkillDefForDependencyRes {

    String name
    String skillId

    String projectId

    int version

    // will only be non-null for cross-project shared skills
    String otherProjectName
    String otherProjectId
}
