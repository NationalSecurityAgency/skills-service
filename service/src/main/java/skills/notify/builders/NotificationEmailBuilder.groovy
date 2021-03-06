package skills.notify.builders


import skills.storage.model.Notification

interface NotificationEmailBuilder {

    static class Res {
        String subject
        String plainText
        String html
    }

    String getId()

    Res build(Notification notification)

    Map<String,Object> buildDigestParams(List<Notification> notifications)
    String buildDigestPlainText(List<Notification> notifications)

}
