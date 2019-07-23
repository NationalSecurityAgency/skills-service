package skills.controller.request.model

import groovy.transform.Canonical

@Canonical
class AccessRequest {
    String projectName
    String projectDescription
}
