package skills.service.controller.request.model

import groovy.transform.Canonical

@Canonical
class EditLevelRequest {
    String name
    String iconClass
    int percent = Integer.MIN_VALUE
    int pointsFrom = Integer.MIN_VALUE
    int pointsTo = Integer.MIN_VALUE
}
