package skills.service.skillLoading.model

class SkillSummary  {

    String skillId
    String skill

    int pointIncrement

    int points
    int totalPoints
    int todaysPoints

    SkillDescription description

    SkillDependencySummary dependencyInfo
}
