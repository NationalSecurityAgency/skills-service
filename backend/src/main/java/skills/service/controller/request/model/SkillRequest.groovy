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
    int numPerformToCompletion

    int version = 0

    String description

    String helpUrl
}
