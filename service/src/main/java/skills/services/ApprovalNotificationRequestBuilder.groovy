package skills.services

import org.thymeleaf.context.Context
import skills.notify.Notifier
import skills.storage.repos.SkillEventsSupportRepo

class ApprovalNotificationRequestBuilder {

    String userRequesting
    String requestMsg
    SkillEventsSupportRepo.SkillDefMin skillDef
    List<String> userIds
    String publicUrl

    Notifier.NotificationRequest build() {
        assert userRequesting
        assert requestMsg
        assert skillDef
        assert userIds

        new Notifier.NotificationRequest(
                userIds: userIds,
                subject: "SkillTree Points Requested",
                plainTextBody: buildBody(),
                thymeleafTemplate: "skill_approval_request.html",
                thymeleafTemplateContext: getThymeleafContext(),
        )
    }

    private Context getThymeleafContext() {
        Context templateContext = new Context()
        templateContext.setVariable("userRequesting", userRequesting)
        templateContext.setVariable("numPoints", numPoints())
        templateContext.setVariable("skillName", skillDef.name)
        templateContext.setVariable("publicUrl", publicUrl)
        templateContext.setVariable("skillId", skillDef.skillId)
        templateContext.setVariable("requestMsg", requestMsg)
        templateContext.setVariable("projectId", skillDef.projectId)

        return templateContext
    }

    private String buildBody() {
        return "User ${userRequesting} requested ${numPoints()} points for '${skillDef.name}' skill.\n" +
                "\n   Approval URL: ${publicUrl}projects/${skillDef.projectId}/self-reporting" +
                "\n   User Requested: ${userRequesting}" +
                "\n   Skill: ${skillDef.name} (${skillDef.skillId})" +
                "\n   Number of Points: ${numPoints()}" +
                "\n   Request Message: ${requestMsg}" +
                        "\n" +
                        "\nAs an approver for '${skillDef.projectId}' project you can approve or reject this request." +
                        "\n\n" +
                        "\nAlways yours," +
                        "\nSkillTree Bot"

    }

    private String numPoints() {
        return String.format("%,d", skillDef.pointIncrement)
    }

}
