package skills.service.controller.request.model

import groovy.transform.Canonical

@Canonical
class ProjectRequest {
    Integer id
    String projectId
    String name
}
