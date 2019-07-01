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
    // Time Window - in minute; 0 means that the action can be performed right away
    int pointIncrementInterval
    // Max Occurrences Within Window
    int numMaxOccurrencesIncrementInterval
    int numPerformToCompletion

    int version = 0

    String description

    String helpUrl
}
