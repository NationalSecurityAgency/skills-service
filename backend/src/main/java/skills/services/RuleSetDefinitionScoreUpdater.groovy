package skills.services

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import skills.storage.model.ProjDef
import skills.storage.model.SkillDef
import skills.storage.model.SkillRelDef
import skills.storage.repos.ProjDefRepo
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.SkillRelDefRepo


@Service
@Slf4j
class RuleSetDefinitionScoreUpdater {

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    SkillRelDefRepo skillRelDefRepo

    @Autowired
    ProjDefRepo projDefRepo

    void updateFromLeaf(SkillDef skillDef) {

        List<SkillRelDef> childRels = skillRelDefRepo.findAllByParentAndType(skillDef, SkillRelDef.RelationshipType.RuleSetDefinition)

        if (childRels) {
            int total = childRels.collect({ it.child.totalPoints }).sum()
            skillDef.totalPoints = total
            skillDefRepo.save(skillDef)
        }

        List<SkillRelDef> parents = skillRelDefRepo.findAllByChildAndType(skillDef, SkillRelDef.RelationshipType.RuleSetDefinition)
        parents?.each {
            updateFromLeaf(it.parent)
        }

        if (skillDef.projDef) {
            updateForProjectByOnlyConsideringImmediateChildren(skillDef.projDef)
        }
    }

    void skillToBeRemoved(SkillDef skillDef) {
        List<SkillRelDef> parents = skillRelDefRepo.findAllByChildAndType(skillDef, SkillRelDef.RelationshipType.RuleSetDefinition)
        parents?.each {
            walkUpAndSubtractFromTotal(it.parent, skillDef.totalPoints)
        }
        if(SkillDef.ContainerType.Skill == skillDef.type){
            ProjDef projDef = projDefRepo.findByProjectId(skillDef.projectId)
            projDef.totalPoints -= skillDef.totalPoints
        }
    }

    private void walkUpAndSubtractFromTotal(SkillDef skillDef, int pointsToSubtract) {
        skillDef.totalPoints = skillDef.totalPoints - pointsToSubtract
        skillDefRepo.save(skillDef)

        List<SkillRelDef> parents = skillRelDefRepo.findAllByChildAndType(skillDef, SkillRelDef.RelationshipType.RuleSetDefinition)
        parents?.each {
            walkUpAndSubtractFromTotal(it.parent, pointsToSubtract)
        }
    }

    void updateForProjectByOnlyConsideringImmediateChildren(ProjDef projDef) {
        List<SkillDef> subjects = skillDefRepo.findAllByProjectIdAndType(projDef.projectId, SkillDef.ContainerType.Subject)
        if (subjects) {
            int total = subjects.collect({ it.totalPoints }).sum()
            projDef.totalPoints = total
            projDefRepo.save(projDef)
        }
    }
}
