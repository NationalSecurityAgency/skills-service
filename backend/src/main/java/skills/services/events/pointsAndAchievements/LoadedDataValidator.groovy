package skills.services.events.pointsAndAchievements

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import skills.controller.exceptions.SkillException
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
            throw new SkillException("Insufficient project points, skill achievement is disallowed", loadedData.projectId)
        }

        loadedData.parentDefs.each { SkillEventsSupportRepo.TinySkillDef parentSkillDef ->
            if (parentSkillDef.type == SkillDef.ContainerType.Subject) {
                if (parentSkillDef.totalPoints < minimumSubjectPoints) {
                    throw new SkillException("Insufficient Subject points, skill achievement is disallowed", parentSkillDef.skillId)
                }
            }
        }
    }
}
