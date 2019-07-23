package skills.controller.request.model

import groovy.transform.Canonical

@Canonical
class SubjectRequest {
    Integer id
    String subjectId
    String name
    String description
    String iconClass
}
