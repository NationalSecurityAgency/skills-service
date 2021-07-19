package skills.services.events.pointsAndAchievements

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillExceptionBuilder

@Component
@Slf4j
@CompileStatic
class InsufficientPointsValidator {

    @Value('#{"${skills.config.ui.minimumSubjectPoints}"}')
    int minimumSubjectPoints

    @Value('#{"${skills.config.ui.minimumProjectPoints}"}')
    int minimumProjectPoints

    void validateProjectPoints(int projDefPoints, String projectId, String userId = null) {
        if (projDefPoints < minimumProjectPoints) {
            SkillExceptionBuilder builder = new SkillExceptionBuilder()
                    .msg("Insufficient project points, skill achievement is disallowed")
                    .projectId(projectId)
                    .errorCode(ErrorCode.InsufficientProjectPoints)
            if (userId) {
                builder.userId(userId)
            }
            throw builder.build()
        }
    }

    void validateSubjectPoints(int subjectDefPoints, String projectId, String userId = null) {
        if (subjectDefPoints < minimumSubjectPoints) {
            SkillExceptionBuilder builder = new SkillExceptionBuilder()
                    .msg("Insufficient Subject points, skill achievement is disallowed")
                    .projectId(projectId)
                    .errorCode(ErrorCode.InsufficientSubjectPoints)
            if (userId) {
                builder.userId(userId)
            }
            throw builder.build()
        }
    }
}
