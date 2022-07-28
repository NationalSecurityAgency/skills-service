/**
 * Copyright 2020 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package skills.services.admin.moveSkills

import callStack.profiler.Profile
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.request.model.SkillsActionRequest
import skills.services.RuleSetDefGraphService
import skills.services.UserAchievementsAndPointsManagement
import skills.services.admin.SkillCatalogFinalizationService
import skills.services.admin.SkillCatalogTransactionalAccessor
import skills.storage.accessors.SkillDefAccessor
import skills.storage.model.SkillDef
import skills.storage.model.SkillRelDef
import skills.storage.repos.SkillRelDefRepo
import skills.storage.repos.UserPointsRepo

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

    @Autowired
    SkillCatalogFinalizationService skillCatalogFinalizationService

    @Autowired
    UserPointsRepo userPointsRepo

    @Autowired
    UserAchievementsAndPointsManagement userAchievementsAndPointsManagement

    @Transactional
    @Profile
    void moveSkills(String projectId, SkillsActionRequest skillReuseRequest) {
        // validate
        skillCatalogFinalizationService.validateNotInFinalizationState(projectId, "Cannot move skills while finalization is running")
        skillCatalogFinalizationService.validateFinalizationIsNotPending(projectId, "Cannot move skills while finalization is pending")

        SkillDef origParentSkill = moveDefinitionToDestParent(projectId, skillReuseRequest)
        SkillDef destSubj = updateDestDefinitionPoints(projectId, skillReuseRequest)
        updateOrigDefinitionPoints(projectId, origParentSkill, destSubj)

        SkillDef origSubj = origParentSkill.type == SkillDef.ContainerType.SkillsGroup ? ruleSetDefGraphService.getParentSkill(origParentSkill.id) : origParentSkill
        userPointsRepo.removeSubjectUserPointsForNonExistentSkillDef(projectId, origSubj.skillId)
        skillCatalogTransactionalAccessor.updateUserPointsForSubject(projectId, origSubj.skillId)

        skillCatalogTransactionalAccessor.createSubjectUserPointsForTheNewUsers(projectId, destSubj.skillId)
        skillCatalogTransactionalAccessor.updateUserPointsForSubject(projectId, destSubj.skillId)

        userAchievementsAndPointsManagement.removeSubjectLevelAchievementsIfUsersDoNotQualify(origSubj)
        skillCatalogTransactionalAccessor.identifyAndAddSubjectLevelAchievements(origSubj.projectId, origSubj.skillId)

//        userAchievementsAndPointsManagement.removeSubjectLevelAchievementsIfUsersDoNotQualify(destSubj)
        skillCatalogTransactionalAccessor.identifyAndAddSubjectLevelAchievements(destSubj.projectId, destSubj.skillId)
    }

    @Profile
    private SkillDef moveDefinitionToDestParent(String projectId, SkillsActionRequest skillReuseRequest) {
        String parentSkillId = skillReuseRequest.groupId ?: skillReuseRequest.subjectId
        boolean isGroupDest = skillReuseRequest.groupId

        SkillDef origParentSkill
        skillReuseRequest.skillIds.each { String skillId ->
            SkillDef skillToMove = skillDefAccessor.getSkillDef(projectId, skillId)
            SkillDef parentSkill = ruleSetDefGraphService.getParentSkill(skillToMove.id)
            if (origParentSkill && origParentSkill.skillId != parentSkill.skillId) {
                throw new SkillException("All moved skills must come from the same parent. But 2 parents were found: [${origParentSkill.skillId}] and [${parentSkill.skillId}] ", projectId, skillId, ErrorCode.BadParam)
            }
            if (parentSkill.skillId == parentSkillId) {
                throw new SkillException("Skill with id [$skillId] already exist under [$parentSkillId]", projectId, skillId, ErrorCode.BadParam)
            }
            origParentSkill = parentSkill

            SkillDef subject = (parentSkill.type == SkillDef.ContainerType.SkillsGroup) ? ruleSetDefGraphService.getParentSkill(parentSkill.id) : parentSkill

            boolean isGroupOrig = parentSkill.type == SkillDef.ContainerType.SkillsGroup
            boolean stayingInTheSameSubject = skillReuseRequest.subjectId == subject.skillId
            boolean moveSkillBetweenGroupsInTheSameSubject = stayingInTheSameSubject && isGroupOrig && isGroupDest

            // handle original
            if (isGroupOrig) {
                ruleSetDefGraphService.removeGraphRelationship(projectId, parentSkill.skillId, SkillDef.ContainerType.SkillsGroup, projectId, skillId, SkillRelDef.RelationshipType.SkillsGroupRequirement)
                if (!moveSkillBetweenGroupsInTheSameSubject) {
                    ruleSetDefGraphService.removeGraphRelationship(projectId, subject.skillId, SkillDef.ContainerType.Subject, projectId, skillId, SkillRelDef.RelationshipType.GroupSkillToSubject)
                }
            } else {
                ruleSetDefGraphService.removeGraphRelationship(projectId, parentSkill.skillId, SkillDef.ContainerType.Subject, projectId, skillId, SkillRelDef.RelationshipType.RuleSetDefinition)
            }

            // handle destination
            if (isGroupDest) {
                ruleSetDefGraphService.assignGraphRelationship(projectId, skillReuseRequest.groupId, SkillDef.ContainerType.SkillsGroup, projectId, skillId, SkillRelDef.RelationshipType.SkillsGroupRequirement)
                if (!moveSkillBetweenGroupsInTheSameSubject) {
                    ruleSetDefGraphService.assignGraphRelationship(projectId, skillReuseRequest.subjectId, SkillDef.ContainerType.Subject, projectId, skillId, SkillRelDef.RelationshipType.GroupSkillToSubject)
                }
            } else {
                ruleSetDefGraphService.assignGraphRelationship(projectId, skillReuseRequest.subjectId, SkillDef.ContainerType.Subject, projectId, skillId, SkillRelDef.RelationshipType.RuleSetDefinition)
            }
        }
        return origParentSkill
    }

    @Profile
    private void updateOrigDefinitionPoints(String projectId, SkillDef origParentSkill, SkillDef destSubj) {
        // update points for the origination group and/or subject
        // optimization - handle the case where skill was moved from a group to its parent subject
        if (destSubj.skillId != origParentSkill.skillId) {
            if (origParentSkill.type == SkillDef.ContainerType.SkillsGroup) {
                skillCatalogTransactionalAccessor.updateGroupTotalPoints(projectId, origParentSkill.skillId)
                SkillDef subject = ruleSetDefGraphService.getParentSkill(origParentSkill.id)
                skillCatalogTransactionalAccessor.updateSubjectTotalPoints(projectId, subject.skillId)
            } else {
                skillCatalogTransactionalAccessor.updateSubjectTotalPoints(projectId, origParentSkill.skillId)
            }
        }
    }

    @Profile
    private SkillDef updateDestDefinitionPoints(String projectId, SkillsActionRequest skillReuseRequest) {
        boolean isGroupDest = skillReuseRequest.groupId

        SkillDef destSubj
        if (isGroupDest) {
            SkillDef group = skillDefAccessor.getSkillDef(projectId, skillReuseRequest.groupId)
            skillCatalogTransactionalAccessor.updateGroupTotalPoints(projectId, skillReuseRequest.groupId)
            destSubj = ruleSetDefGraphService.getParentSkill(group.id)
            skillCatalogTransactionalAccessor.updateSubjectTotalPoints(projectId, destSubj.skillId)
        } else {
            destSubj = skillDefAccessor.getSkillDef(projectId, skillReuseRequest.subjectId, [SkillDef.ContainerType.Subject])
            skillCatalogTransactionalAccessor.updateSubjectTotalPoints(projectId, destSubj.skillId)
        }
        return destSubj
    }

}
