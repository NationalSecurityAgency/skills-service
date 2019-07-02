package skills.service.skillLoading.model

class SkillSummary  {

    String projectId
    String projectName

    String skillId
    String skill

    int pointIncrement
    int pointIncrementInterval
    int maxOccurrencesWithinIncrementInterval

    int points
    int totalPoints
    int todaysPoints

    SkillDescription description

    SkillDependencySummary dependencyInfo

    boolean crossProject
}
