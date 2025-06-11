package skills.controller.request.model

class WebNotificationRequest {

    // null for global notifications
    String userId

    Date notifiedOn
    Date showUntil
    String title
    String notification
}
