package skills.dbupgrade

import skills.controller.request.model.SkillEventRequest

class QueuedSkillEvent {
    String projectId
    String skillId
    String userId
    SkillEventRequest skillEventRequest
}
