package skills.controller.result.model

import skills.storage.model.SkillDef

class SkillDefGraphRes {
    Integer id
    String name
    String skillId
    String projectId
    Integer pointIncrement
    Integer totalPoints
    SkillDef.ContainerType type
}
