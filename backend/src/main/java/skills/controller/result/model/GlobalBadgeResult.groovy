package skills.controller.result.model


import groovy.transform.Canonical

@Canonical
class GlobalBadgeResult extends BadgeResult {

    List<LevelDefinitionRes> requiredProjectLevels = []
}
