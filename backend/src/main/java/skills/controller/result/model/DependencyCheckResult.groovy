package skills.controller.result.model

class DependencyCheckResult {
    String skillId
    String dependentSkillId

    boolean possible = true

    String reason
}
