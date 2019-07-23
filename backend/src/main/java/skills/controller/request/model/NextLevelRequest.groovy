package skills.controller.request.model

import groovy.transform.Canonical

@Canonical
class NextLevelRequest {
    Integer percent = null
    Integer points = null
    String name
    String iconClass
}
