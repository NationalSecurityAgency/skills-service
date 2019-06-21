package skills.storage.repos.nativeSql

import skills.storage.model.SkillDef

interface NativeQueriesRepo {
    void decrementPointsForDeletedSkill(String projectId, String deletedSkillId, String parentSubjectSkillId)

    void updateOverallScoresBySummingUpAllChildSubjects(String projectId, SkillDef.ContainerType subjectType)

    List<GraphRelWithAchievement> getDependencyGraphWithAchievedIndicator(String projectId, String skillId, String userId)
}

