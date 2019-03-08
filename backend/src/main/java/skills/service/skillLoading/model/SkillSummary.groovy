package skills.service.skillLoading.model

import skills.service.skillLoading.SubjectDataLoader

class SkillSummary {

    String skillId
    String skill

    int pointIncrement

    int points
    int totalPoints
    int todaysPoints


    SkillDescription description

    List<SkillSummary> children
    List<SkillSummary> mustAchieveTheseFirst
    SkillDependencySummary dependencyInfo
}
