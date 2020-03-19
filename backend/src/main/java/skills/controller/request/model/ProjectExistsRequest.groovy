package skills.controller.request.model

import groovy.transform.Canonical

@Canonical
class ProjectExistsRequest {
    String projectId
    String name
}
