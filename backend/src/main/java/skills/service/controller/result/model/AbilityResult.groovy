package skills.service.controller.result.model

import groovy.transform.Canonical

@Canonical
class AbilityResult {
    Integer id
    String abilityId
    String projectId
    String name
    String description
    int displayOrder
    int totalPoints
}
