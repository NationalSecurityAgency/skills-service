package skills.controller.request.model

import groovy.transform.Canonical

@Canonical
class GlobalBadgeRequest extends BadgeRequest {

    Boolean enableProtectedUserCommunity = false
}
