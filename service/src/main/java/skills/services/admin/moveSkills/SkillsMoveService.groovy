package skills.services.admin.moveSkills

import callStack.profiler.Profile
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.exceptions.SkillException
import skills.controller.request.model.SkillsActionRequest
import skills.services.RuleSetDefGraphService
import skills.services.admin.SkillCatalogTransactionalAccessor
import skills.storage.accessors.SkillDefAccessor
import skills.storage.model.SkillDef
import skills.storage.model.SkillRelDef
import skills.storage.repos.SkillRelDefRepo

@Service
@Slf4j
class SkillsMoveService {

    @Autowired
    SkillRelDefRepo skillRelDefRepo

    @Autowired
    RuleSetDefGraphService ruleSetDefGraphService

    @Autowired
    SkillDefAccessor skillDefAccessor

    @Autowired
    SkillCatalogTransactionalAccessor skillCatalogTransactionalAccessor

    @Transactional
    @Profile
    void moveSkills(String projectId, SkillsActionRequest skillReuseRequest) {
        // validate
//        validateParentIsNotDestination(projectId, skillReuseRequest)
//        validateNoAlreadyReusedInDestination(skillReuseRequest, projectId)
//        validateNotInFinalizationState(projectId)
//        validateFinalizationIsNotPending(projectId)
//        validateSkillsHaveNoDeps(projectId, skillReuseRequest)

        SkillDef origParentSkill
        skillReuseRequest.skillIds.each { String skillId ->
            SkillDef skillToMove = skillDefAccessor.getSkillDef(projectId, skillId)
            SkillDef parentSkill = ruleSetDefGraphService.getParentSkill(skillToMove.id)
            if (origParentSkill && origParentSkill.skillId != parentSkill.skillId) {
                throw new SkillException("All moved skills must come from the same parent. But 2 parents were found: [${origParentSkill.skillId}] and [${parentSkill.skillId}] ", projectId)
            }
            origParentSkill = parentSkill
            if (parentSkill.type == SkillDef.ContainerType.SkillsGroup) {
                ruleSetDefGraphService.removeGraphRelationship(projectId, parentSkill.skillId, SkillDef.ContainerType.SkillsGroup, projectId, skillId, SkillRelDef.RelationshipType.SkillsGroupRequirement)
            } else {
                ruleSetDefGraphService.removeGraphRelationship(projectId, parentSkill.skillId, SkillDef.ContainerType.Subject, projectId, skillId, SkillRelDef.RelationshipType.RuleSetDefinition)
            }

            if (skillReuseRequest.groupId) {
                ruleSetDefGraphService.assignGraphRelationship(projectId, skillReuseRequest.groupId, SkillDef.ContainerType.SkillsGroup, projectId, skillId, SkillRelDef.RelationshipType.SkillsGroupRequirement)
            } else {
                ruleSetDefGraphService.assignGraphRelationship(projectId, skillReuseRequest.subjectId, SkillDef.ContainerType.Subject, projectId, skillId, SkillRelDef.RelationshipType.RuleSetDefinition)
            }
        }

        SkillDef destSubj
        if (skillReuseRequest.groupId) {
            SkillDef group = skillDefAccessor.getSkillDef(projectId, skillReuseRequest.groupId)
            skillCatalogTransactionalAccessor.updateGroupTotalPoints(projectId, skillReuseRequest.groupId)
            destSubj = ruleSetDefGraphService.getParentSkill(group.id)
            skillCatalogTransactionalAccessor.updateSubjectTotalPoints(projectId, subject.skillId)
        } else {
            destSubj = skillDefAccessor.getSkillDef(projectId, skillReuseRequest.subjectId, [SkillDef.ContainerType.Subject])
            skillCatalogTransactionalAccessor.updateSubjectTotalPoints(projectId, subject.skillId)
        }

        // optimization - handle the case where skill was moved from a group to its parent subject
        if (destSubj.skillId != origParentSkill.skillId) {
            skillCatalogTransactionalAccessor.updateSubjectTotalPoints(projectId, origParentSkill.skillId)
        }

    }

}
