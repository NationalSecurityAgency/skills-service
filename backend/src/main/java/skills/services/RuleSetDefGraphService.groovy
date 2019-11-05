package skills.services

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.exceptions.SkillException
import skills.storage.model.SkillDef
import skills.storage.model.SkillRelDef
import skills.storage.model.SkillRelDef.RelationshipType
import skills.storage.repos.SkillDefAccessor
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.SkillRelDefRepo

@Service
@Slf4j
class RuleSetDefGraphService {

    @Autowired
    SkillRelDefRepo skillRelDefRepo

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    SkillDefAccessor skillDefAccessor

    @Transactional
    SkillDef getParentSkill(SkillDef skillDef) {
        List<SkillRelDef> parents = skillRelDefRepo.findAllByChildAndType(skillDef, RelationshipType.RuleSetDefinition)
        // assume that I only have one parent
        SkillDef parent = parents.first().parent
        return parent
    }

    @Transactional
    List<SkillDef> getChildrenSkills(SkillDef skillDef) {
        getChildrenSkills(skillDef, RelationshipType.RuleSetDefinition)
    }

    @Transactional
    List<SkillDef> getChildrenSkills(SkillDef skillDef, RelationshipType relationshipType) {
        return skillRelDefRepo.getChildren(skillDef.projectId, skillDef.skillId, relationshipType)
    }

    @Transactional
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

    @Transactional
    void assignGraphRelationship(String projectId, String skillId, SkillDef.ContainerType skillType,
                                 String relationshipSkillId, RelationshipType relationshipType) {
        assignGraphRelationship(projectId, skillId, skillType, projectId, relationshipSkillId, relationshipType)
    }
    @Transactional
    void assignGraphRelationship(String projectId, String skillId, SkillDef.ContainerType skillType,
                                 String relationshipProjectId, String relationshipSkillId, RelationshipType relationshipType) {
        SkillDef skill1 = skillDefAccessor.getSkillDef(projectId, skillId, skillType)
        SkillDef skill2 = skillDefAccessor.getSkillDef(relationshipProjectId, relationshipSkillId)
        skillRelDefRepo.save(new SkillRelDef(parent: skill1, child: skill2, type: relationshipType))
    }

    @Transactional
    void removeGraphRelationship(String projectId, String skillId, SkillDef.ContainerType skillType,
                                 String relationshipProjectId, String relationshipSkillId, RelationshipType relationshipType){
        SkillDef skill1 = skillDefAccessor.getSkillDef(projectId, skillId, skillType)
        SkillDef skill2 = skillDefAccessor.getSkillDef(relationshipProjectId, relationshipSkillId)
        SkillRelDef relDef = skillRelDefRepo.findByChildAndParentAndType(skill2, skill1, relationshipType)
        if (!relDef) {
            throw new SkillException("Failed to find relationship [$relationshipType] between [$skillId] and [$relationshipSkillId] for [$projectId]", projectId, skillId)
        }
        skillRelDefRepo.delete(relDef)
    }
}
