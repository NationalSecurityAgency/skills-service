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
package skills.services.admin

import callStack.profiler.Profile
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.request.model.ActionPatchRequest
import skills.controller.request.model.SkillRequest
import skills.controller.result.model.SkillDefPartialRes
import skills.controller.result.model.SkillDefRes
import skills.controller.result.model.SkillDefSkinnyRes
import skills.services.*
import skills.storage.accessors.SkillDefAccessor
import skills.storage.model.SkillDef
import skills.storage.model.SkillDef.SelfReportingType
import skills.storage.model.SkillDefWithExtra
import skills.storage.model.SkillRelDef
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.SkillDefWithExtraRepo
import skills.storage.repos.SkillRelDefRepo
import skills.storage.repos.UserPointsRepo
import skills.utils.InputSanitizer
import skills.utils.Props

@Service
@Slf4j
class SkillsAdminService {

    @Autowired
    LockingService lockingService

    @Autowired
    CustomValidator customValidator

    @Autowired
    CreatedResourceLimitsValidator createdResourceLimitsValidator

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    SkillRelDefRepo skillRelDefRepo

    @Autowired
    SkillDefWithExtraRepo skillDefWithExtraRepo

    @Autowired
    GlobalBadgesService globalBadgesService

    @Autowired
    UserAchievementsAndPointsManagement userPointsManagement

    @Autowired
    RuleSetDefGraphService ruleSetDefGraphService

    @Autowired
    DisplayOrderService displayOrderService

    @Autowired
    UserPointsRepo userPointsRepo

    @Autowired
    RuleSetDefinitionScoreUpdater ruleSetDefinitionScoreUpdater

    @Autowired
    SkillDefAccessor skillDefAccessor

    @Autowired
    BadgeAdminService badgeAdminService

    @Autowired
    @Lazy
    SkillApprovalService skillApprovalService

    @Transactional()
    void saveSkill(String originalSkillId, SkillRequest skillRequest, boolean performCustomValidation=true) {
        lockingService.lockProject(skillRequest.projectId)

        validateSkillVersion(skillRequest)

        final CustomValidationResult customValidationResult = customValidator.validate(skillRequest)
        if(performCustomValidation && !customValidationResult.valid){
            String msg = "Custom validation failed: msg=[${customValidationResult.msg}], type=[skill], skillId=[${skillRequest.skillId}], name=[${skillRequest.name}], description=[${skillRequest.description}]"
            throw new SkillException(msg)
        }

        SkillDefWithExtra skillDefinition = skillDefWithExtraRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(skillRequest.projectId, originalSkillId, SkillDef.ContainerType.Skill)

        if (!skillDefinition || !skillDefinition.skillId.equalsIgnoreCase(skillRequest.skillId)) {
            SkillDef idExists = skillDefRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(skillRequest.projectId, skillRequest.skillId, SkillDef.ContainerType.Skill)
            if (idExists) {
                throw new SkillException("Skill with id [${skillRequest.skillId}] already exists! Sorry!", skillRequest.projectId, null, ErrorCode.ConstraintViolation)
            }
        }

        if (!skillDefinition || !skillDefinition.name.equalsIgnoreCase(skillRequest.name)) {
            SkillDef nameExists = skillDefRepo.findByProjectIdAndNameIgnoreCaseAndType(skillRequest.projectId, skillRequest.name, SkillDef.ContainerType.Skill)
            if (nameExists) {
                throw new SkillException("Skill with name [${skillRequest.name}] already exists! Sorry!", skillRequest.projectId, null, ErrorCode.ConstraintViolation)
            }
        }

        boolean shouldRebuildScores
        boolean updateUserPoints
        int pointIncrementDelta
        int occurrencesDelta

        final boolean isEdit = skillDefinition
        final int totalPointsRequested = skillRequest.pointIncrement * skillRequest.numPerformToCompletion
        final int incrementRequested = skillRequest.pointIncrement
        final int currentOccurrences = isEdit ? (skillDefinition.totalPoints / skillDefinition.pointIncrement) : -1
        final SelfReportingType selfReportingType =  skillRequest.selfReportType ? SkillDef.SelfReportingType.valueOf(skillRequest.selfReportType) : null;

        SkillDef subject = null
        if (isEdit) {
            shouldRebuildScores = skillDefinition.totalPoints != totalPointsRequested
            occurrencesDelta = skillRequest.numPerformToCompletion - currentOccurrences
            updateUserPoints = shouldRebuildScores || occurrencesDelta != 0
            pointIncrementDelta = incrementRequested - skillDefinition.pointIncrement

            Props.copy(skillRequest, skillDefinition, "childSkills", 'version', 'selfReportType')

            skillApprovalService.modifyApprovalsWhenSelfReportingTypeChanged(skillDefinition, selfReportingType)
            skillDefinition.selfReportingType = selfReportingType;

            //totalPoints is not a prop on skillRequest, it is a calculated value so we
            //need to manually update it in the case of edits.
            skillDefinition.totalPoints = totalPointsRequested

        } else {
            String parentSkillId = skillRequest.subjectId
            subject = skillDefRepo.findByProjectIdAndSkillIdAndType(skillRequest.projectId, parentSkillId, SkillDef.ContainerType.Subject)
            assert subject, "Subject [${parentSkillId}] does not exist"

            createdResourceLimitsValidator.validateNumSkillsCreated(subject)

            Integer highestDisplayOrder = skillDefRepo.calculateChildSkillsHighestDisplayOrder(skillRequest.projectId, parentSkillId)
            int displayOrder = highestDisplayOrder == null ? 1 : highestDisplayOrder + 1
            skillDefinition = new SkillDefWithExtra(
                    skillId: skillRequest.skillId,
                    projectId: skillRequest.projectId,
                    name: skillRequest.name,
                    pointIncrement: skillRequest.pointIncrement,
                    pointIncrementInterval: skillRequest.pointIncrementInterval,
                    numMaxOccurrencesIncrementInterval: skillRequest.numMaxOccurrencesIncrementInterval,
                    totalPoints: totalPointsRequested,
                    description: skillRequest.description,
                    helpUrl: skillRequest.helpUrl,
                    displayOrder: displayOrder,
                    type: SkillDef.ContainerType.Skill,
                    version: skillRequest.version,
                    selfReportingType: selfReportingType,
            )
            log.debug("Saving [{}]", skillDefinition)
            shouldRebuildScores = true
        }

        DataIntegrityExceptionHandlers.skillDataIntegrityViolationExceptionHandler.handle(skillRequest.projectId, skillRequest.skillId) {
            skillDefWithExtraRepo.save(skillDefinition)
        }

        SkillDef savedSkill = skillDefRepo.findByProjectIdAndSkillIdAndType(skillRequest.projectId, skillRequest.skillId, SkillDef.ContainerType.Skill)

        if (!isEdit) {
            assignToParent(skillRequest, savedSkill, subject)
        }

        if (shouldRebuildScores) {
            log.debug("Rebuilding scores for [${}]", savedSkill.skillId)
            ruleSetDefinitionScoreUpdater.updateFromLeaf(savedSkill)
        }

        if (isEdit) {
            // order is CRITICAL HERE
            // must update point increment first then deal with changes in the occurrences;
            // changes in the occurrences will use the newly updated point increment
            if (pointIncrementDelta != 0) {
                userPointsManagement.handlePointIncrementUpdate(savedSkill.projectId, skillRequest.subjectId, savedSkill.skillId, pointIncrementDelta)
            }
            int newOccurrences = savedSkill.totalPoints / savedSkill.pointIncrement
            if (occurrencesDelta < 0) {
                // order is CRITICAL HERE
                // Must update points prior removal of UserPerformedSkill events as the removal relies on the existence of those extra events
                userPointsManagement.updatePointsWhenOccurrencesAreDecreased(savedSkill.projectId, skillRequest.subjectId, savedSkill.skillId, savedSkill.pointIncrement, newOccurrences)
                userPointsManagement.removeExtraEntriesOfUserPerformedSkillByUser(savedSkill.projectId, savedSkill.skillId, currentOccurrences + occurrencesDelta)
                //identify what badge (or badges) this skill belongs to.
                //if any, look for users who qualify for the badge now after this change is persisted See BadgeAdminService.identifyUsersMeetingBadgeRequirements
                userPointsManagement.insertUserAchievementWhenDecreaseOfOccurrencesCausesUsersToAchieve(savedSkill.projectId, savedSkill.skillId, savedSkill.id, newOccurrences)

                //identify any badges that this skill belongs to and award the badge if any users now qualify for this badge
                List<SkillDef> badges = findAllBadgesSkillBelongsTo(savedSkill.skillId)
                badges?.each {
                    badgeAdminService.awardBadgeToUsersMeetingRequirements(it)
                }

            } else if (occurrencesDelta > 0) {
                userPointsManagement.removeUserAchievementsThatDoNotMeetNewNumberOfOccurrences(savedSkill.projectId, savedSkill.skillId, newOccurrences)
            }
        }

        log.debug("Saved [{}]", savedSkill)
    }

    @Transactional
    void deleteSkill(String projectId, String subjectId, String skillId) {
        log.debug("Deleting skill with project id [{}] and subject id [{}] and skill id [{}]", projectId, subjectId, skillId)
        SkillDef skillDefinition = skillDefRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(projectId, skillId, SkillDef.ContainerType.Skill)
        assert skillDefinition, "DELETE FAILED -> no skill with project find with projectId=[$projectId], subjectId=[$subjectId], skillId=[$skillId]"

        if (globalBadgesService.isSkillUsedInGlobalBadge(skillDefinition)) {
            throw new SkillException("Skill with id [${skillId}] cannot be deleted as it is currently referenced by one or more global badges")
        }

        SkillDef parentSkill = ruleSetDefGraphService.getParentSkill(skillDefinition)

        //we need to check to see if this skill belongs to any badges, if so we need to look for any users who now qualify
        //for those badges
        ruleSetDefinitionScoreUpdater.skillToBeRemoved(skillDefinition)
        userPointsManagement.handleSkillRemoval(skillDefinition)

        //identify any badges that this skill belonged to and award the badge if any users now qualify for this badge
        List<SkillDef> badges = findAllBadgesSkillBelongsTo(skillDefinition.skillId)

        ruleSetDefGraphService.deleteSkillWithItsDescendants(skillDefinition)
        log.debug("Deleted skill [{}]", skillDefinition.skillId)

        badges?.each {
            badgeAdminService.awardBadgeToUsersMeetingRequirements(it)
        }

        List<SkillDef> siblings = ruleSetDefGraphService.getChildrenSkills(parentSkill)
        displayOrderService.resetDisplayOrder(siblings)
    }

    @Transactional(readOnly = true)
    private List<SkillDef> findAllBadgesSkillBelongsTo(String skillId, boolean includeGlobal = true) {
        List<SkillDef> badges = []
        def res = skillRelDefRepo.findAllChildrenByChildSkillIdAndRelationshipTypeAndParentType(skillId,
                SkillRelDef.RelationshipType.BadgeRequirement,
                SkillDef.ContainerType.Badge)
        if (res) {
            badges.addAll(res)
        }

        if (includeGlobal) {
            res = skillRelDefRepo.findAllChildrenByChildSkillIdAndRelationshipTypeAndParentType(skillId,
                    SkillRelDef.RelationshipType.BadgeRequirement,
                    SkillDef.ContainerType.GlobalBadge)
            if (res) {
                badges.addAll(res)
            }
        }

        return badges
    }

    @Transactional(readOnly = true)
    Integer findLatestSkillVersion(String projectId) {
        return skillDefRepo.findMaxVersionByProjectId(projectId)
    }

    @Transactional(readOnly = true)
    List<SkillDefPartialRes> getSkills(String projectId, String subjectId) {
        return getSkillsByProjectSkillAndType(projectId, subjectId, SkillDef.ContainerType.Subject, SkillRelDef.RelationshipType.RuleSetDefinition)
    }

    def errorCodeMapping = [(SkillDef.ContainerType.Badge) : ErrorCode.BadgeNotFound,
                            (SkillDef.ContainerType.GlobalBadge) : ErrorCode.BadgeNotFound,
                            (SkillDef.ContainerType.Subject) : ErrorCode.SubjectNotFound,
                            (SkillDef.ContainerType.Skill) : ErrorCode.SkillNotFound]

    @Transactional(readOnly = true)
    List<SkillDefPartialRes> getSkillsByProjectSkillAndType(String projectId, String skillId, SkillDef.ContainerType type, SkillRelDef.RelationshipType relationshipType) {
        SkillDef parent = skillDefRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(projectId, skillId, type)
        if (!parent) {
            ErrorCode code = errorCodeMapping.get(type)
            throw new SkillException("${type} [${skillId}] doesn't exist.", projectId, null, code)
        }

        List<SkillDefRepo.SkillDefPartial> res
        if (type == SkillDef.ContainerType.GlobalBadge) {
            res = skillRelDefRepo.getGlobalChildrenPartial(parent.skillId, relationshipType)
        } else {
            res = skillRelDefRepo.getChildrenPartial(parent.projectId, parent.skillId, relationshipType)
        }
        return res.collect { convertToSkillDefPartialRes(it) }.sort({ it.displayOrder })
    }

    @Transactional(readOnly = true)
    List<SkillDefSkinnyRes> getSkinnySkills(String projectId) {
        List<SkillDefRepo.SkillDefSkinny> data = loadSkinnySkills(projectId)
        List<SkillDefPartialRes> res = data.collect { convertToSkillDefSkinnyRes(it) }
        return res
    }

    @Transactional(readOnly = true)
    SkillDefRes getSkill(String projectId, String subjectId, String skillId) {
        SkillDefWithExtra res = skillDefWithExtraRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(projectId, skillId, SkillDef.ContainerType.Skill)
        if (!res) {
            throw new SkillException("Skill [${skillId}] doesn't exist.", projectId, null, ErrorCode.SkillNotFound)
        }

        SkillDefRes finalRes = convertToSkillDefRes(res)
        return finalRes
    }

    @Transactional(readOnly = true)
    boolean existsBySkillName(String projectId, String skillName) {
        return skillDefRepo.existsByProjectIdAndNameAndTypeAllIgnoreCase(projectId, skillName, SkillDef.ContainerType.Skill)
    }

    @Transactional
    List<SkillDefPartialRes> getSkillsForBadge(String projectId, String badgeId) {
        return getSkillsByProjectSkillAndType(projectId, badgeId, SkillDef.ContainerType.Badge, SkillRelDef.RelationshipType.BadgeRequirement)
    }


    @Transactional
    SkillDef updateSkillDisplayOrder(@PathVariable("projectId") String projectId,
                                     @PathVariable("subjectId") String subjectId,
                                     @PathVariable("skillId") String skillId,
                                     @RequestBody ActionPatchRequest patchRequest) {
        lockingService.lockProject(projectId)

        SkillDef moveMe = skillDefAccessor.getSkillDef(projectId, skillId)
        SkillDef parent = ruleSetDefGraphService.getParentSkill(moveMe)

        SkillDef switchWith
        switch (patchRequest.action) {
            case ActionPatchRequest.ActionType.DisplayOrderDown:
                List<SkillDef> foundSkills = skillDefRepo.findNextSkillDefs(projectId, parent.skillId, moveMe.displayOrder, SkillRelDef.RelationshipType.RuleSetDefinition, PageRequest.of(0, 1))
                switchWith = foundSkills ? foundSkills?.first() : null
                break;
            case ActionPatchRequest.ActionType.DisplayOrderUp:
                List<SkillDef> foundSkills = skillDefRepo.findPreviousSkillDefs(projectId, parent.skillId, moveMe.displayOrder, SkillRelDef.RelationshipType.RuleSetDefinition, PageRequest.of(0, 1))
                switchWith = foundSkills ? foundSkills?.first() : null
                break;
            default:
                throw new SkillException("Unknown action ${patchRequest.action}", projectId, skillId)
        }

        if (!switchWith) {
            throw new SkillException("Failed to find skill to switch with [${moveMe.skillId}] for action [$patchRequest.action]", projectId, skillId)
        }
        assert switchWith.skillId != moveMe.skillId

        int switchWithDisplayOrderTmp = moveMe.displayOrder
        moveMe.displayOrder = switchWith.displayOrder
        switchWith.displayOrder = switchWithDisplayOrderTmp
        skillDefRepo.save(moveMe)
        skillDefRepo.save(switchWith)

        return switchWith
    }

    @Profile
    private SkillDefRes convertToSkillDefRes(SkillDef skillDef) {
        skills.controller.result.model.SkillDefRes res = new skills.controller.result.model.SkillDefRes()
        Props.copy(skillDef, res)
        res.numPerformToCompletion = skillDef.totalPoints / res.pointIncrement
        return res
    }


    @Profile
    private SkillDefRes convertToSkillDefRes(SkillDefWithExtra skillDef) {
        SkillDefRes res = new SkillDefRes()
        Props.copy(skillDef, res)
        res.description = InputSanitizer.unsanitizeForMarkdown(res.description)
        res.numPerformToCompletion = skillDef.totalPoints / res.pointIncrement

        return res
    }

    @CompileStatic
    @Profile
    private SkillDefSkinnyRes convertToSkillDefSkinnyRes(SkillDefRepo.SkillDefSkinny skinny) {
        SkillDefSkinnyRes res = new SkillDefSkinnyRes(
                skillId: skinny.skillId,
                projectId: skinny.projectId,
                name: skinny.name,
                version: skinny.version,
                displayOrder: skinny.displayOrder,
                created: skinny.created,
                totalPoints: skinny.totalPoints,
        )
        return res;
    }

    @CompileStatic
    @Profile
    private SkillDefPartialRes convertToSkillDefPartialRes(SkillDefRepo.SkillDefPartial partial, boolean loadNumUsers = false) {
        SkillDefPartialRes res = new SkillDefPartialRes(
                skillId: partial.skillId,
                projectId: partial.projectId,
                name: partial.name,
                pointIncrement: partial.pointIncrement,
                pointIncrementInterval: partial.pointIncrementInterval,
                numMaxOccurrencesIncrementInterval: partial.numMaxOccurrencesIncrementInterval,
                numPerformToCompletion: partial.numMaxOccurrencesIncrementInterval,
                totalPoints: partial.totalPoints,
                version: partial.version,
                type: partial.skillType,
                displayOrder: partial.displayOrder,
                created: partial.created,
                updated: partial.updated,
                selfReportingType: partial.getSelfReportingType()
        )

        res.numPerformToCompletion = (Integer)(res.totalPoints / res.pointIncrement)
        res.totalPoints = partial.totalPoints
        res.numMaxOccurrencesIncrementInterval = partial.numMaxOccurrencesIncrementInterval

        if (loadNumUsers) {
            res.numUsers = calculateDistinctUsersForSkill((SkillDefRepo.SkillDefPartial)partial)
        }

        return res;
    }

    @Transactional()
    List<Integer> getUniqueSkillVersionList(String projectId) {
        skillDefRepo.getUniqueVersionList(projectId)
    }

    @Transactional(readOnly = true)
    boolean existsBySkillId(String projectId, String skillId) {
        return skillDefRepo.existsByProjectIdIgnoreCaseAndSkillId(projectId, skillId)
    }

    @Profile
    private int calculateDistinctUsersForSkill(SkillDefRepo.SkillDefPartial partial) {
        skillDefRepo.calculateDistinctUsersForASingleSkill(partial.projectId, partial.skillId)
    }

    @Profile
    private List<SkillDefRepo.SkillDefSkinny> loadSkinnySkills(String projectId) {
        skillDefRepo.findAllSkinnySelectByProjectIdAndType(projectId, SkillDef.ContainerType.Skill)
    }

    private void validateSkillVersion(SkillRequest skillRequest){
        int latestSkillVersion = findLatestSkillVersion(skillRequest.projectId)
        if (skillRequest.version > (latestSkillVersion + 1)) {
            throw new SkillException("Latest skill version is [${latestSkillVersion}]; max supported version is latest+1 but provided [${skillRequest.version}] version", skillRequest.projectId, skillRequest.skillId, skills.controller.exceptions.ErrorCode.BadParam)
        }
    }

    private void assignToParent(SkillRequest skillRequest, SkillDef savedSkill, SkillDef parent=null) {
        String parentSkillId = skillRequest.subjectId
        SkillDef.ContainerType containerType = SkillDef.ContainerType.Subject

        if(parent == null) {
            parent = skillDefRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(skillRequest.projectId, parentSkillId, containerType)
        }

        if (!parent) {
            throw new SkillException("Requested parent skill id [${parentSkillId}] doesn't exist for type [${containerType}].", skillRequest.projectId, skillRequest.skillId)
        }

        SkillRelDef relDef = new SkillRelDef(parent: parent, child: savedSkill, type: SkillRelDef.RelationshipType.RuleSetDefinition)
        skillRelDefRepo.save(relDef)
    }

}
