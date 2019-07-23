package skills.controller.request.model

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
    int pointIncrementInterval = 60 * 8
    // Max Occurrences Within Window
    int numMaxOccurrencesIncrementInterval = 1
    int numPerformToCompletion

    int version = 0

    String description

    String helpUrl
}
