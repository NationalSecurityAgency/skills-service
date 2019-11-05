package skills.storage.repos

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import skills.controller.exceptions.SkillException
import skills.storage.model.SkillDef

@Service
class SkillDefAccessor {

    @Autowired
    SkillDefRepo skillDefRepo

    SkillDef getSkillDef(String projectId, String skillId, SkillDef.ContainerType containerType = SkillDef.ContainerType.Skill) {
        SkillDef skillDef = skillDefRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(projectId, skillId, containerType)
        if (!skillDef) {
            throw new SkillException("Failed to find skillId [$skillId] for [$projectId] with type [${containerType}]", projectId, skillId)
        }
        return skillDef
    }

}
