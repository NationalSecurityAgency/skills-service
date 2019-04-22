package skills.service.skillLoading.model

class SkillDependencyInfo {

    List<SkillRelationshipItem> dependencies

    static class SkillRelationshipItem {
        String projectId
        String projectName
        String skillId
        String skillName
    }

    static class SkillRelationship {
        SkillRelationshipItem skill
        SkillRelationshipItem dependsOn
        Boolean achieved
        Boolean crossProject = false
    }
}
