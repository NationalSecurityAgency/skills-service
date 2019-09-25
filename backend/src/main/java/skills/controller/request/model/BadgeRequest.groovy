package skills.controller.request.model

import groovy.transform.Canonical

@Canonical
class BadgeRequest {
    String badgeId
    String name
    String description
    String iconClass

    Date startDate
    Date endDate

    String helpUrl
}
