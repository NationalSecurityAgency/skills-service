package skills.controller.result.model

import groovy.transform.Canonical

@Canonical
class SubjectResult {

    String subjectId

    String projectId

    String name

    int totalPoints

    String description

    int numSkills
    int numUsers
    int pointsPercentage

    int displayOrder

    String iconClass
}
