package skills.service.controller.request.model

import groovy.transform.Canonical

@Canonical
class SkillRequest {

    Integer id

    String skillId


    // optional
    String subjectId

    String projectId

    String name

    int pointIncrement
    int pointIncrementInterval
    int maxSkillAchievedCount
    int totalPoints

    String description

    String helpUrl

    List<String> dependentSkillsIds
    List<String> skillRecommendationIds
}
