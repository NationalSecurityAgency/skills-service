package skills.services

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import skills.storage.model.SkillDef
import skills.storage.model.SkillRelDef
import skills.storage.model.SkillRelDef.RelationshipType
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.SkillRelDefRepo

@Service
@Slf4j
class RuleSetDefGraphService {

    @Autowired
    SkillRelDefRepo skillRelDefRepo

    @Autowired
    SkillDefRepo skillDefRepo

    SkillDef getParentSkill(SkillDef skillDef) {
        List<SkillRelDef> parents = skillRelDefRepo.findAllByChildAndType(skillDef, RelationshipType.RuleSetDefinition)
        // assume that I only have one parent
        SkillDef parent = parents.first().parent
        return parent
    }

    List<SkillDef> getChildrenSkills(SkillDef skillDef) {
        getChildrenSkills(skillDef, RelationshipType.RuleSetDefinition)
    }

    List<SkillDef> getChildrenSkills(SkillDef skillDef, RelationshipType relationshipType) {
        return skillRelDefRepo.getChildren(skillDef.projectId, skillDef.skillId, relationshipType)
    }

    void deleteSkillWithItsDescendants(SkillDef skillDef) {
        List<SkillDef> toDelete = []

        List<SkillDef> currentChildren = getChildrenSkills(skillDef)
        while (currentChildren) {
            toDelete.addAll(currentChildren)
            currentChildren = currentChildren?.collect {
                getChildrenSkills(it)
            }?.flatten()
        }
        toDelete.add(skillDef)
        log.debug("Deleting [{}] skill definitions (descendants + me) under [{}]", toDelete.size(), skillDef.skillId)
        skillDefRepo.deleteAll(toDelete)
    }
}
