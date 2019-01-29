package skills.service.controller.request.model

import groovy.transform.Canonical

@Canonical
class ActionPatchRequest {
    static enum ActionType { DisplayOrderUp, DisplayOrderDown}
    ActionType action
}
