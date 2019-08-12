package skills.controller.request.model

import groovy.transform.Canonical

@Canonical
class SubjectRequest {
    String subjectId
    String name
    String description
    String iconClass
}
