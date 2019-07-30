package skills.controller.result.model

import skills.storage.model.SkillDef

class SkillDefPartialRes extends SkillDefSkinnyRes{
    Integer pointIncrement

    // Time Window - in minute; 0 means that the action can be performed right away
    Integer pointIncrementInterval
    // Max Occurrences Within Window;
    Integer numMaxOccurrencesIncrementInterval

    Integer numPerformToCompletion
    Integer totalPoints

    SkillDef.ContainerType type

    Date updated

    int numUsers
}
