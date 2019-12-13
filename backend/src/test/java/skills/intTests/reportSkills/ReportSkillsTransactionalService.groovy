package skills.intTests.reportSkills

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import skills.auth.UserInfoService
import skills.controller.request.model.SkillEventRequest
import skills.services.events.SkillEventResult
import skills.services.events.SkillEventsService

@Component
class ReportSkillsTransactionalService {

    @Autowired
    SkillEventsService skillEventsService

    @Autowired
    UserInfoService userInfoService

    @Transactional
    SkillEventResult reportSkill(String projectId, String skillId, SkillEventRequest skillEventRequest, boolean shouldThrow) {
        println "reportSkill contorller ${org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()}"
        SkillEventResult res = skillEventsService.reportSkill(projectId, skillId, userInfoService.getUserName(skillEventRequest.userId), new Date(skillEventRequest.timestamp))
        if (shouldThrow) {
            throw new RuntimeException("Throw exception so transaction would be rolled back")
        }
        res
    }
}
