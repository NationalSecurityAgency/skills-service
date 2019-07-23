package skills.controller.request.model

import groovy.transform.Canonical

@Canonical
class AbilityRequest {
    Integer id
    String name
    String description
    String abilityType
}
