package skills.services.events.pointsAndAchievements

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import skills.controller.exceptions.SkillException
import skills.controller.exceptions.SkillExceptionBuilder
import skills.storage.model.SkillDef
import skills.storage.repos.SkillEventsSupportRepo

@Component
@Slf4j
@CompileStatic
class LoadedDataValidator {

    @Value('#{"${skills.config.ui.minimumSubjectPoints}"}')
    int minimumSubjectPoints

    @Value('#{"${skills.config.ui.minimumProjectPoints}"}')
    int minimumProjectPoints

    void validate(LoadedData loadedData) {
        if (loadedData.tinyProjectDef.totalPoints < minimumProjectPoints) {
            throw new SkillExceptionBuilder()
                .msg("Insufficient project points, skill achievement is disallowed")
                .projectId(loadedData.projectId)
                .userId(loadedData.userId)
                .build()
        }

        loadedData.parentDefs.each { SkillEventsSupportRepo.TinySkillDef parentSkillDef ->
            if (parentSkillDef.type == SkillDef.ContainerType.Subject) {
                if (parentSkillDef.totalPoints < minimumSubjectPoints) {
                    throw new SkillExceptionBuilder()
                            .msg("Insufficient Subject points, skill achievement is disallowed")
                            .projectId(loadedData.projectId)
                            .userId(loadedData.userId)
                            .build()
                }
            }
        }
    }
}
