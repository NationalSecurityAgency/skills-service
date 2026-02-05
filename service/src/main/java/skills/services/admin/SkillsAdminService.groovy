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
import org.apache.commons.lang3.StringUtils
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
import skills.controller.request.model.SkillImportRequest
import skills.controller.request.model.SkillRequest
import skills.controller.result.model.SkillDefPartialRes
import skills.controller.result.model.SkillDefRes
import skills.controller.result.model.SkillDefSkinnyRes
import skills.controller.result.model.SkillTagRes
import skills.services.*
import skills.services.admin.skillReuse.SkillReuseIdUtil
import skills.services.attributes.ExpirationAttrs
import skills.services.attributes.SkillAttributeService
import skills.services.quiz.QuizToSkillService
import skills.services.userActions.DashboardAction
import skills.services.userActions.DashboardItem
import skills.services.userActions.UserActionInfo
import skills.services.userActions.UserActionsHistoryService
import skills.skillLoading.SkillsLoader
import skills.storage.accessors.SkillDefAccessor
import skills.storage.model.*
import skills.storage.model.SkillDef.SelfReportingType
import skills.storage.repos.*
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
    QuizToSkillService quizToSkillService

    @Autowired
    QuizToSkillDefRepo quizToSkillDefRepo

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

    @Autowired
    SkillCatalogService skillCatalogService

    @Autowired
    SkillCatalogFinalizationService skillCatalogFinalizationService

    @Autowired
    SkillsGroupAdminService skillsGroupAdminService

    @Autowired
    SkillTagService skillTagService

    @Autowired
    ProjDefRepo projDefRepo

    @Autowired
    AttachmentService attachmentService

    @Autowired
    SkillAttributesDefRepo skillAttributesDefRepo

    @Autowired
    UserActionsHistoryService userActionsHistoryService

    @Autowired
    SkillAttributeService skillAttributeService

    @Autowired
    private SkillsLoader skillsLoader;

    protected static class SaveSkillTmpRes {
        // because of the skill re-use it could be imported but NOT available in the catalog
        boolean isImportedByOtherProjects = false
        String projectId
        String skillId
        Integer skillRefId
    }

    @Transactional()
    @Profile
    SaveSkillTmpRes saveSkill(String originalSkillId, SkillRequest skillRequest, boolean performCustomValidation=true, String groupId=null, boolean validateVideoAttrs = true) {
        lockingService.lockProject(skillRequest.projectId)

        validateSkillVersion(skillRequest)

        if (performCustomValidation) {
            final CustomValidationResult customValidationResult = customValidator.validate(skillRequest)
            if (!customValidationResult.valid) {
                String msg = "Custom validation failed: msg=[${customValidationResult.msg}], type=[skill], skillId=[${skillRequest.skillId}], name=[${skillRequest.name}], description=[${skillRequest.description}]"
                throw new SkillException(msg, skillRequest.projectId, skillRequest.skillId, ErrorCode.ParagraphValidationFailed)
            }
        }

        SkillDefWithExtra skillDefinition = skillDefWithExtraRepo.findByProjectIdAndSkillIdIgnoreCaseAndTypeIn(skillRequest.projectId, originalSkillId, [SkillDef.ContainerType.Skill, SkillDef.ContainerType.SkillsGroup])
        validateSelfReportVideo(skillRequest, skillDefinition, validateVideoAttrs)
        if (!skillDefinition || !skillDefinition.skillId.equalsIgnoreCase(skillRequest.skillId)) {
            SkillDef idExists = skillDefRepo.findByProjectIdAndSkillIdIgnoreCaseAndTypeIn(skillRequest.projectId, skillRequest.skillId, [SkillDef.ContainerType.Skill, SkillDef.ContainerType.SkillsGroup])
            if (idExists) {
                throw new SkillException("Skill with id [${skillRequest.skillId}] already exists! Sorry!", skillRequest.projectId, null, ErrorCode.ConstraintViolation)
            }
        }
        if (skillDefinition && skillDefinition.type.toString() != skillRequest.type) {
            throw new SkillException("Skill with id [${skillRequest.skillId}] with type [${skillDefinition.type}] already exists! Requested to create skill with type of [${skillRequest.type}]", skillRequest.projectId, skillDefinition.skillId, ErrorCode.ConstraintViolation)
        }

        if (!skillDefinition || !skillDefinition.name.equalsIgnoreCase(skillRequest.name)) {
            SkillDef nameExists = skillDefRepo.findByProjectIdAndNameIgnoreCaseAndType(skillRequest.projectId, skillRequest.name, SkillDef.ContainerType.Skill)
            if (nameExists) {
                throw new SkillException("Skill with name [${skillRequest.name}] already exists! Sorry!", skillRequest.projectId, null, ErrorCode.ConstraintViolation)
            }
        }
        if (skillDefinition && !groupId) {
            groupId = skillDefinition.groupId
        }

        boolean shouldRebuildScores = false
        boolean updateUserPoints = false
        int pointIncrementDelta = 0
        int occurrencesDelta = 0
        int numSkillsRequiredDelta = 0

        final SkillDef.ContainerType skillType = skillRequest.type ? SkillDef.ContainerType.valueOf(skillRequest.type) : SkillDef.ContainerType.Skill;
        final boolean isEdit = skillDefinition
        final boolean isSkillsGroup = skillType == SkillDef.ContainerType.SkillsGroup
        final boolean isSkillsGroupChild = StringUtils.isNotBlank(groupId)
        int totalPointsRequested = isSkillsGroup ? 0 : skillRequest.pointIncrement * skillRequest.numPerformToCompletion
        final int incrementRequested = isSkillsGroup ? 0 : skillRequest.pointIncrement
        final int currentOccurrences = isEdit && !isSkillsGroup ? (skillDefinition.totalPoints / skillDefinition.pointIncrement) : -1
        final SelfReportingType selfReportingType = skillRequest.selfReportingType && !isSkillsGroup ? SkillDef.SelfReportingType.valueOf(skillRequest.selfReportingType) : null;
        final isCurrentlyEnabled = Boolean.valueOf(skillDefinition?.enabled)
        final boolean isEnabledSkillInRequest = Boolean.valueOf(skillRequest.enabled)
        final boolean isJustificationRequiredInRequest = Boolean.valueOf(skillRequest.justificationRequired)
        final boolean isSkillCatalogImport = skillRequest instanceof SkillImportRequest;
        final boolean isReplicationRequest = skillRequest instanceof ReplicatedSkillUpdateRequest
        String description = skillRequest.description

        SkillDef skillsGroupSkillDef = null
        List<SkillDef> groupChildSkills = null
        String parentSkillId = skillRequest.subjectId
        SkillDef subject = skillDefRepo.findByProjectIdAndSkillIdAndType(skillRequest.projectId, parentSkillId, SkillDef.ContainerType.Subject)
        if (!subject) {
            throw new SkillException("Subject [${parentSkillId}] does not exist", skillRequest.projectId, skillRequest.skillId, ErrorCode.BadParam)
        }
        final isSubjectEnabled = Boolean.valueOf(subject?.enabled)
        if (!isSubjectEnabled && isEnabledSkillInRequest) {
            throw new SkillException("Cannot enable Skill [${originalSkillId}] because it's Subject [${parentSkillId}] is disabled", skillRequest.projectId, skillRequest.skillId, ErrorCode.BadParam)
        }
        if (isSkillsGroupChild) {
            // need to validate skills group
            if (!skillsGroupSkillDef) {
                skillsGroupSkillDef = skillDefRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(skillRequest.projectId, groupId, SkillDef.ContainerType.SkillsGroup)
            }
            if (!Boolean.valueOf(skillsGroupSkillDef?.enabled) && isEnabledSkillInRequest) {
                throw new SkillException("Cannot enable Skill [${originalSkillId}] because it's SkillsGroup [${groupId}] is disabled", skillRequest.projectId, skillRequest.skillId, ErrorCode.BadParam)
            }
        }
        if (isEdit) {
            validateImportedSkillUpdate(skillRequest, skillDefinition)
            // for updates, use the existing value if it is not set on the skillRequest (null or empty String)
            if (StringUtils.isBlank(skillRequest.enabled)) {
                skillRequest.enabled = skillDefinition.enabled
            }
            if (StringUtils.isBlank(skillRequest.justificationRequired)) {
                skillRequest.justificationRequired = skillDefinition.justificationRequired
            }
            if (isEdit && isCurrentlyEnabled && !isEnabledSkillInRequest) {
                throw new SkillException("Skill [${originalSkillId}] has already been enabled and cannot be disabled.", skillRequest.projectId, null, ErrorCode.BadParam)
            }

            if (isSkillsGroup) {
                // need to update total points for the group
                groupChildSkills = skillsGroupAdminService.validateSkillsGroupAndReturnChildren(skillRequest, skillDefinition)
                boolean enabledGroup = !isCurrentlyEnabled && isEnabledSkillInRequest
                if (enabledGroup) {
                    groupChildSkills.findAll {it.copiedFrom == null }.each {
                        it.enabled = true
                    }
                }
                totalPointsRequested = skillsGroupAdminService.getGroupTotalPoints(groupChildSkills)

                if (isEnabledSkillInRequest && skillDefinition.numSkillsRequired != skillRequest.numSkillsRequired) {
                    int currentNumSkillsRequired = skillDefinition.numSkillsRequired == -1 ? groupChildSkills.size() : skillDefinition.numSkillsRequired
                    int requestedNumSkillsRequired = skillRequest.numSkillsRequired == -1 ? groupChildSkills.size() : skillRequest.numSkillsRequired
                    numSkillsRequiredDelta = requestedNumSkillsRequired - currentNumSkillsRequired
                }

                boolean skillIdChanged = skillDefinition.skillId != skillRequest.skillId
                if (skillIdChanged) {
                    // need to update child skills since group skillId changed
                    groupChildSkills.each {
                        it.groupId = skillRequest.skillId
                    }
                }
                if (skillIdChanged || enabledGroup) {
                    skillDefRepo.saveAll(groupChildSkills)
                }
            }
            shouldRebuildScores = skillDefinition.totalPoints != totalPointsRequested || (!Boolean.valueOf(skillDefinition.enabled) && isEnabledSkillInRequest)
            occurrencesDelta = isSkillsGroup ? 0 : skillRequest.numPerformToCompletion - currentOccurrences
            updateUserPoints = shouldRebuildScores || occurrencesDelta != 0
            pointIncrementDelta = isSkillsGroup ? 0 : incrementRequested - skillDefinition.pointIncrement

            if (isReplicationRequest) {

                // point increment is mutated in case of re-used skills
                if (!SkillReuseIdUtil.isTagged(skillRequest.skillId)) {
                    //once a skill has been imported into a project, we don't want to update that imported
                    //version's point increment as importing users are allowed to scale the total points
                    //to be in line with their project's point layout. However, we do need to update the number of occurrences
                    //on imported skills if that changes on the original exported skill
                    skillRequest.pointIncrement = skillDefinition.pointIncrement
                    pointIncrementDelta = 0
                }
                totalPointsRequested = skillRequest.pointIncrement * skillRequest.numPerformToCompletion

                def finalizeState = skillCatalogFinalizationService.getCurrentState(skillRequest.projectId)
                if (finalizeState != SkillCatalogFinalizationService.FinalizeState.COMPLETED) {
                    skillRequest.enabled = isCurrentlyEnabled
                }
            }

            if (skillDefinition.selfReportingType == SelfReportingType.Quiz && skillRequest.selfReportingType?.toLowerCase() != SelfReportingType.Quiz.toString().toLowerCase()) {
                quizToSkillService.removeQuizToSkillAssignment(skillDefinition)
            }

            Props.copy(skillRequest, skillDefinition, "childSkills", 'version', 'selfReportType')

            skillApprovalService.modifyApprovalsWhenSelfReportingTypeChanged(skillDefinition, selfReportingType)
            skillDefinition.selfReportingType = selfReportingType;

            skillDefinition.totalPoints = totalPointsRequested
        } else {
            createdResourceLimitsValidator.validateNumSkillsCreated(subject)

            Integer highestDisplayOrder = skillDefRepo.calculateChildSkillsHighestDisplayOrder(skillRequest.projectId, groupId ?: parentSkillId)
            int displayOrder = highestDisplayOrder == null ? 1 : highestDisplayOrder + 1
            String enabled = isEnabledSkillInRequest.toString()
            String justificationRequired = isJustificationRequiredInRequest.toString()
            if (isSkillsGroupChild) {
                skillsGroupSkillDef = skillDefRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(skillRequest.projectId, groupId, SkillDef.ContainerType.SkillsGroup)
                if (!skillsGroupSkillDef) {
                    throw new SkillException("[${groupId}] groupId was not found.".toString(), skillRequest.projectId, skillRequest.skillId, ErrorCode.BadParam)
                }
            }

            if (!isSkillCatalogImport) {
                Closure<Boolean> alreadyExistLookup = { String uuid ->
                    return skillDefWithExtraRepo.otherSkillsExistInProjectWithAttachmentUUID(skillRequest.projectId, skillRequest.skillId, uuid)
                }
                description = attachmentService.copyAttachmentsForIncomingDescription(description, skillRequest.projectId, skillRequest.skillId, null, alreadyExistLookup)
            }

            skillDefinition = new SkillDefWithExtra(
                    skillId: skillRequest.skillId,
                    projectId: skillRequest.projectId,
                    name: skillRequest.name,
                    pointIncrement: skillRequest.pointIncrement,
                    pointIncrementInterval: skillRequest.pointIncrementInterval,
                    numMaxOccurrencesIncrementInterval: skillRequest.numMaxOccurrencesIncrementInterval,
                    totalPoints: totalPointsRequested,
                    description: description,
                    helpUrl: skillRequest.helpUrl,
                    displayOrder: displayOrder,
                    type: skillType,
                    version: skillRequest.version,
                    selfReportingType: selfReportingType,
                    numSkillsRequired: skillRequest.numSkillsRequired,
                    enabled: enabled,
                    groupId: groupId,
                    justificationRequired: justificationRequired,
                    iconClass: skillRequest.iconClass
            )

            if (isSkillCatalogImport) {
                skillDefinition.copiedFrom = skillRequest.copiedFrom
                skillDefinition.readOnly = skillRequest.readOnly
                skillDefinition.copiedFromProjectId = skillRequest.copiedFromProjectId
            }
            log.debug("Saving [{}]", skillDefinition)
            shouldRebuildScores = isEnabledSkillInRequest
        }

        SkillDefWithExtra tempSaved
        DataIntegrityExceptionHandlers.skillDataIntegrityViolationExceptionHandler.handle(skillRequest.projectId, skillRequest.skillId) {
            //tempSaved has the correct value
            tempSaved = skillDefWithExtraRepo.save(skillDefinition)
        }
        SkillDef savedSkill = skillDefRepo.findByProjectIdAndSkillIdAndType(skillRequest.projectId, skillRequest.skillId, skillType)
        validateThatSkillWasSaved(skillDefinition, savedSkill)
        if (isSkillsGroup) {
            skillsGroupSkillDef = savedSkill
        }

        if (!isEdit) {
            if (isSkillsGroupChild) {
                skillsGroupAdminService.addSkillToSkillsGroup(savedSkill.projectId, groupId, savedSkill.skillId)
                ruleSetDefGraphService.assignGraphRelationship(savedSkill.projectId, subject.skillId, SkillDef.ContainerType.Subject, savedSkill.skillId, SkillRelDef.RelationshipType.GroupSkillToSubject)
            } else {
                assignToParent(skillRequest, savedSkill, subject)
            }
        }
        if (!isSkillCatalogImport) {
            attachmentService.updateAttachmentsAttrsBasedOnUuidsInMarkdown(description, savedSkill.projectId, null, savedSkill.skillId)
        }

        if (isSkillsGroupChild) {
            // need to validate skills group
            if (!skillsGroupSkillDef) {
                skillsGroupSkillDef = skillDefRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(skillRequest.projectId, groupId, SkillDef.ContainerType.SkillsGroup)
            }
            groupChildSkills = skillsGroupAdminService.validateSkillsGroupAndReturnChildren(skillsGroupSkillDef.numSkillsRequired, skillsGroupSkillDef.id)
        }

        if (shouldRebuildScores) {
            log.debug("Rebuilding scores for [${}]", savedSkill.skillId)
            ruleSetDefinitionScoreUpdater.updateFromLeaf(savedSkill)
        }

        SaveSkillTmpRes saveSkillTmpRes = new SaveSkillTmpRes(projectId: savedSkill.projectId, skillId: savedSkill.skillId, skillRefId: savedSkill.id)
        if (isEdit) {
            if (isEnabledSkillInRequest) {
                updatePointsAndAchievements(
                        savedSkill,
                        skillRequest.subjectId,
                        pointIncrementDelta,
                        occurrencesDelta,
                        currentOccurrences,
                        numSkillsRequiredDelta,
                        skillsGroupSkillDef,
                        groupChildSkills
                )
            }
            saveSkillTmpRes.isImportedByOtherProjects = skillDefRepo.isCatalogSkillImportedByOtherProjects(savedSkill.id)
        }

        if (selfReportingType == SkillDef.SelfReportingType.Quiz) {
            quizToSkillService.handleQuizToSkillRelationship(savedSkill, skillRequest)
        }

        if (!isReplicationRequest && !isSkillCatalogImport) {
            DashboardAction dashboardAction = isEdit ? DashboardAction.Edit : DashboardAction.Create
            userActionsHistoryService.saveUserAction(new UserActionInfo(
                    action: dashboardAction,
                    item: isSkillsGroup ? DashboardItem.SkillsGroup : DashboardItem.Skill,
                    actionAttributes: tempSaved,
                    itemId: savedSkill.skillId,
                    itemRefId: savedSkill.id,
                    projectId: savedSkill.projectId,
            ))
        }
        log.debug("Saved [{}]", savedSkill)
        return saveSkillTmpRes
    }

    /**
     * Transitive relationships appear to confuse hibernate cache - if the same
     * object was loaded earlier in the session via transitive relationship the hibernate fails
     * to recognize the dirty object and doesn't generate sql to save the updates
     * fail safe: compare attributes to validate that skill was actually persisted
     */
    private void validateThatSkillWasSaved(SkillDefWithExtra toSaved, SkillDef saved) {
        assert toSaved.pointIncrement == saved.pointIncrement
        assert toSaved.skillId == saved.skillId
        assert toSaved.projectId == saved.projectId
        assert toSaved.totalPoints == saved.totalPoints
        assert toSaved.version == saved.version
        assert toSaved.name == saved.name
        assert toSaved.enabled == saved.enabled
    }

    private void validateImportedSkillUpdate(SkillRequest skillRequest, SkillDefWithExtra skillDefinition) {
        if (skillDefinition.readOnly && !(skillRequest instanceof ReplicatedSkillUpdateRequest)) {
            // update is only allowed for change in pointIncrement
            boolean isAllowed =
                    (skillRequest.name == skillDefinition.name) &&
                            (skillRequest.pointIncrementInterval == skillDefinition.pointIncrementInterval) &&
                            (skillRequest.numMaxOccurrencesIncrementInterval == skillDefinition.numMaxOccurrencesIncrementInterval) &&
                            (skillRequest.numPerformToCompletion == (skillDefinition.totalPoints / skillDefinition.pointIncrement)) &&
                            (skillRequest.version == skillDefinition.version) &&
                            (!skillRequest.description || skillRequest.description == skillDefinition.description) &&
                            (!skillRequest.helpUrl || skillRequest.helpUrl == skillDefinition.helpUrl) &&
                            (!skillRequest.selfReportingType || skillRequest.selfReportingType == skillDefinition.selfReportingType?.toString()) &&
                            (skillRequest.enabled == skillDefinition.enabled)
            if (!isAllowed) {
                throw new SkillException("Skill with id [${skillRequest.skillId}] has been imported from the Global Catalog and only pointIncrement can be altered", skillRequest.projectId, skillRequest.skillId, ErrorCode.ReadOnlySkill)
            }
        }
    }

    @Profile
    private void updatePointsAndAchievements(SkillDef savedSkill, String subjectId, int pointIncrementDelta, int occurrencesDelta, int currentOccurrences, int numSkillsRequiredDelta, SkillDef skillsGroupSkillDef, List<SkillDef> groupChildSkills) {
        if (savedSkill.type != SkillDef.ContainerType.SkillsGroup) {

            int newOccurrences = savedSkill.totalPoints / savedSkill.pointIncrement

            // order is CRITICAL HERE
            // must update point increment first then deal with changes in the occurrences;
            // changes in the occurrences will use the newly updated point increment
            if (pointIncrementDelta != 0 && occurrencesDelta >= 0) {
                userPointsManagement.adjustUserPointsAfterModification(savedSkill)
            }

            if (occurrencesDelta < 0) {
                // order is CRITICAL HERE
                // Must remove UserPerformedSkill events before updating points
                userPointsManagement.removeExtraEntriesOfUserPerformedSkillByUser(savedSkill.projectId, savedSkill.skillId, currentOccurrences + occurrencesDelta)
                userPointsManagement.adjustUserPointsAfterModification(savedSkill)
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

        if ((occurrencesDelta < 0 || numSkillsRequiredDelta < 0) && skillsGroupSkillDef && Boolean.valueOf(skillsGroupSkillDef.enabled)) {
            int numSkillsRequired = skillsGroupSkillDef.numSkillsRequired == -1 ? groupChildSkills.size() : skillsGroupSkillDef.numSkillsRequired
            userPointsManagement.insertUserAchievementWhenDecreaseOfSkillsRequiredCausesUsersToAchieve(savedSkill.projectId, skillsGroupSkillDef.skillId, skillsGroupSkillDef.id, groupChildSkills.collect {it.skillId}, numSkillsRequired)
        }

        if (pointIncrementDelta != 0 || occurrencesDelta < 0) {
            SkillDef subject = skillDefRepo.findByProjectIdAndSkillIdAndType(savedSkill.projectId, subjectId, SkillDef.ContainerType.Subject)
            userPointsManagement.identifyAndAddLevelAchievements(subject)
        }
        log.debug("Saved [{}]", savedSkill)
    }

    @Transactional
    void deleteSkill(String projectId, String skillId, boolean trackUserActionHistory = true) {
        log.debug("Deleting skill with project id [{}] and skill id [{}]", projectId, skillId)
        SkillDef skillDefinition = skillDefRepo.findByProjectIdAndSkillIdIgnoreCaseAndTypeIn(projectId, skillId, [SkillDef.ContainerType.Skill, SkillDef.ContainerType.SkillsGroup])
        assert skillDefinition, "DELETE FAILED -> no skill with project find with projectId=[$projectId], skillId=[$skillId]"

        if (globalBadgesService.isSkillUsedInGlobalBadge(skillDefinition)) {
            throw new SkillException("Skill with id [${skillId}] cannot be deleted as it is currently referenced by one or more global badges")
        }
        SkillDef parentSkill = ruleSetDefGraphService.getParentSkill(skillDefinition)
        SkillDef subject = ruleSetDefGraphService.getMySubjectParent(skillDefinition.id)
        if (skillDefinition.type == SkillDef.ContainerType.SkillsGroup) {
            List<SkillDef> childSkills = ruleSetDefGraphService.getChildrenSkills(skillDefinition, [SkillRelDef.RelationshipType.SkillsGroupRequirement])
            childSkills.each {
                removeCatalogImportedSkills(it)
            }
        } else {
            removeCatalogImportedSkills(skillDefinition)
        }

        // this MUST happen before the skill was removed as sql relies on the skill to exist
        userPointsManagement.handleSkillRemoval(skillDefinition, subject)

        //identify any badges that this skill belonged to and award the badge if any users now qualify for this badge
        List<SkillDef> badges = findAllBadgesSkillBelongsTo(skillDefinition.skillId)

        ruleSetDefGraphService.deleteSkillWithItsDescendants(skillDefinition)
        log.debug("Deleted skill [{}]", skillDefinition.skillId)

        // this MUST happen after the skill was removed as sql relies on the skill to be gone
        ruleSetDefinitionScoreUpdater.updateSubjectSkillDef(subject)
        ruleSetDefinitionScoreUpdater.updateProjDef(subject.projectId)
        userPointsManagement.identifyAndAddLevelAchievements(subject)

        // make sure skills group is still valid and update group's totalPoints
        if (skillDefinition.groupId) {
            assert parentSkill.type == SkillDef.ContainerType.SkillsGroup
            List<SkillDef> children = skillsGroupAdminService.getSkillsGroupChildSkills(parentSkill.id)
            if (children.size() == parentSkill.numSkillsRequired) {
                parentSkill.numSkillsRequired = -1
            }
            parentSkill.totalPoints = skillsGroupAdminService.getGroupTotalPoints(children)
            DataIntegrityExceptionHandlers.skillDataIntegrityViolationExceptionHandler.handle(projectId, skillId) {
                skillDefWithExtraRepo.save(parentSkill)
            }

            if (children) {
                int numSkillsRequired = parentSkill.numSkillsRequired == -1 ? children.size() : parentSkill.numSkillsRequired
                userPointsManagement.insertUserAchievementWhenDecreaseOfSkillsRequiredCausesUsersToAchieve(projectId, parentSkill.skillId, parentSkill.id, children.collect { it.skillId }, numSkillsRequired)
            }
        }

        badges?.each {
            badgeAdminService.awardBadgeToUsersMeetingRequirements(it)
        }

        List<SkillDef> siblings = ruleSetDefGraphService.getChildrenSkills(parentSkill, [SkillRelDef.RelationshipType.RuleSetDefinition, SkillRelDef.RelationshipType.SkillsGroupRequirement])
        displayOrderService.resetDisplayOrder(siblings)

        if (trackUserActionHistory) {
            boolean isReusedSkill = SkillReuseIdUtil.isTagged(skillDefinition.skillId)
            userActionsHistoryService.saveUserAction(new UserActionInfo(
                    action: isReusedSkill ? DashboardAction.StopInProjectReuse : DashboardAction.Delete,
                    item: skillDefinition.type == SkillDef.ContainerType.SkillsGroup ? DashboardItem.SkillsGroup : DashboardItem.Skill,
                    itemId: skillDefinition.skillId,
                    projectId: skillDefinition.projectId,
            ))
        }
    }

    @Profile
    void removeCatalogImportedSkills(SkillDef skillDefinition) {
        List<SkillDefWithExtra> related = skillDefWithExtraRepo.findSkillsCopiedFrom(skillDefinition.id)
        log.info("catalog skill is being deleted, deleting [{}] copies imported into other projects", related?.size())
        related?.each {
            deleteSkill(it.projectId, it.skillId, false)
        }
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
    Integer findMaxVersionByProjectId(String projectId) {
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

        List<SkillDefPartial> res
        if (type == SkillDef.ContainerType.GlobalBadge) {
            res = skillRelDefRepo.getGlobalChildrenPartial(parent.skillId, relationshipType)
        } else {
            res = skillRelDefRepo.getChildrenPartial(parent.projectId, parent.skillId, relationshipType)
        }
        // global badges will have a null projectId
        Boolean projectHasSkillTags = projectId ? skillDefRepo.doesProjectHaveSkillTags(projectId) as boolean : false
        return res.collect { convertToSkillDefPartialRes(it, projectHasSkillTags) }.sort({ it.displayOrder })
    }

    /**
     * Return sthe skills for a subject to include a status indiciating whether or not the skill has been exported to the skill catalog
     * @param projectId projectId
     * @param subjectId subjectId
     * @param includeGroupChildren - if true, will include the skills that fall underneath Skill Groups as opposed to just the skill representing the group
     * @return
     */
    @Transactional(readOnly = true)
    List<SkillDefPartialRes> getSkillsForSubjectWithCatalogStatus(String projectId, String subjectId, boolean includeGroupChildren=false) {
        SkillDef subject = skillDefRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(projectId, subjectId, SkillDef.ContainerType.Subject)
        if (!subject) {
            ErrorCode code = ErrorCode.SubjectNotFound
            throw new SkillException("Subject [${subjectId}] doesn't exist.", projectId, null, code)
        }

        List<SkillRelDef.RelationshipType> relationshipTypes = includeGroupChildren ?
                [SkillRelDef.RelationshipType.RuleSetDefinition, SkillRelDef.RelationshipType.GroupSkillToSubject] :
                [SkillRelDef.RelationshipType.RuleSetDefinition]
        List<SkillDefPartial> res = skillRelDefRepo.getSkillsWithCatalogStatus(projectId, subject.skillId, relationshipTypes)

        Boolean projectHasSkillTags = skillDefRepo.doesProjectHaveSkillTags(projectId) as boolean
        return res.collect { convertToSkillDefPartialRes(it, projectHasSkillTags) }.sort({ it.displayOrder })
    }

    @Transactional(readOnly = true)
    List<SkillDefSkinnyRes> getSkinnySkills(String projectId, String skillNameQuery, boolean excludeImportedSkills = false, boolean includeDisabled = false) {
        List<SkillDefSkinny> data = loadSkinnySkills(projectId, skillNameQuery, excludeImportedSkills, includeDisabled)
        List<SkillDefSkinnyRes> res = data.collect { convertToSkillDefSkinnyRes(it) }?.sort({ it.skillId })

        // do not hit on the reuse tag
        if (StringUtils.isNoneBlank(skillNameQuery)) {
            Boolean hasReusedSkills = res.find { it.isReused }
            if (hasReusedSkills) {
                res = res.findAll { it.name.toUpperCase().contains(skillNameQuery.toUpperCase()) }
            }
        }
        return res
    }

    @Transactional(readOnly = true)
    List<SkillDefSkinnyRes> getSkinnySkillsAndBadges(String projectId, String skillNameQuery, boolean excludeImportedSkills = false, boolean includeDisabled = false) {
        List<SkillDefSkinny> data = loadSkinnySkillsAndBadges(projectId, skillNameQuery, excludeImportedSkills, includeDisabled)
        List<SkillDefSkinnyRes> res = data.collect { convertToSkillDefSkinnyRes(it) }?.sort({ it.skillId })

        // do not hit on the reuse tag
        if (StringUtils.isNoneBlank(skillNameQuery)) {
            Boolean hasReusedSkills = res.find { it.isReused }
            if (hasReusedSkills) {
                res = res.findAll { it.name.toUpperCase().contains(skillNameQuery.toUpperCase()) }
            }
        }
        return res
    }

    @Transactional(readOnly = true)
    SkillDefSkinnyRes getSkinnySkill(String projectId, String skillId) {
        SkillDefSkinny data = loadSkinnySkill(projectId, skillId)
        if (!data) {
            throw new SkillException("Skill [${skillId}] doesn't exist.", projectId, null, ErrorCode.SkillNotFound)
        }
        SkillDefSkinnyRes res = convertToSkillDefSkinnyRes(data)
        return res
    }

    @Transactional(readOnly = true)
    SkillDefRes getSkill(String projectId, String subjectId, String skillId) {
        SkillDefWithExtra res = skillDefWithExtraRepo.findByProjectIdAndSkillIdIgnoreCaseAndTypeIn(projectId, skillId, [SkillDef.ContainerType.Skill, SkillDef.ContainerType.SkillsGroup])
        if (!res) {
            throw new SkillException("Skill [${skillId}] doesn't exist.", projectId, null, ErrorCode.SkillNotFound)
        }

        SkillDefRes finalRes = convertToSkillDefRes(res)
        finalRes.subjectId = subjectId
        finalRes.sharedToCatalog = skillCatalogService.isAvailableInCatalog(res.projectId, res.skillId)
        if (finalRes.copiedFromProjectId) {
            finalRes.copiedFromProjectName = projDefRepo.getProjectName(finalRes.copiedFromProjectId)?.projectName
        }

        finalRes.thisSkillWasReusedElsewhere = skillDefRepo.wasThisSkillReusedElsewhere(res.id)

        DisplayOrderRes orderInfo = skillsLoader.getSkillOrderStats(projectId, subjectId, skillId)
        if(orderInfo) {
            finalRes.prevSkillId = orderInfo.previousSkillId
            finalRes.nextSkillId = orderInfo.nextSkillId
            finalRes.totalSkills = orderInfo.totalCount
            finalRes.orderInGroup = orderInfo.overallOrder
        }

        String videoUrl = skillAttributesDefRepo.getVideoUrlBySkillRefId(res.id)
        finalRes.hasVideoConfigured = StringUtils.isNotBlank(videoUrl)
        return finalRes
    }

    @Transactional(readOnly = true)
    boolean existsBySkillName(String projectId, String skillName) {
        return skillDefRepo.existsByProjectIdAndNameAndTypeInAllIgnoreCase(projectId, skillName, [SkillDef.ContainerType.Skill, SkillDef.ContainerType.SkillsGroup])
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
        List<SkillRelDef.RelationshipType> supportedRelTypes = [SkillRelDef.RelationshipType.RuleSetDefinition, SkillRelDef.RelationshipType.SkillsGroupRequirement]
        switch (patchRequest.action) {
            case ActionPatchRequest.ActionType.DisplayOrderDown:
                List<SkillDef> foundSkills = skillDefRepo.findNextSkillDefs(projectId, parent.skillId, moveMe.displayOrder, supportedRelTypes, PageRequest.of(0, 1))
                switchWith = foundSkills ? foundSkills?.first() : null
                break;
            case ActionPatchRequest.ActionType.DisplayOrderUp:
                List<SkillDef> foundSkills = skillDefRepo.findPreviousSkillDefs(projectId, parent.skillId, moveMe.displayOrder, supportedRelTypes, PageRequest.of(0, 1))
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
        res.name = InputSanitizer.unsanitizeName(res.name)
        return res
    }


    @Profile
    private SkillDefRes convertToSkillDefRes(SkillDefWithExtra skillDef) {
        SkillDefRes res = new SkillDefRes()
        Props.copy(skillDef, res)
        res.enabled = skillDef.enabled == "true" ? true : false
        res.iconClass = skillDef.iconClass
        res.justificationRequired = Boolean.valueOf(skillDef.justificationRequired)
        res.description = InputSanitizer.unsanitizeForMarkdown(res.description)
        res.helpUrl = InputSanitizer.unsanitizeUrl(res.helpUrl)
        if (skillDef.type == SkillDef.ContainerType.Skill) {
            res.numPerformToCompletion = skillDef.totalPoints / res.pointIncrement
        }
        if (skillDef.type == SkillDef.ContainerType.SkillsGroup) {
            List<SkillDef> groupChildSkills = skillsGroupAdminService.getSkillsGroupChildSkills(skillDef.getId())
            res.numSkillsInGroup = groupChildSkills.size()
            res.numSelfReportSkills = groupChildSkills.count({ it.selfReportingType })?.intValue()
            res.enabled = Boolean.valueOf(skillDef.enabled)
        }
        if (skillDef.groupId) {
            SkillDefWithExtra skillsGroup = skillsGroupAdminService.getSkillsGroup(skillDef.projectId, skillDef.groupId)
            if (!skillsGroup) {
                throw new SkillException("Failed to find skill's group with groupId=[${skillDef.groupId}]", skillDef.projectId, skillDef.skillId)
            }
            res.enabled = Boolean.valueOf(skillDef.enabled)
            res.groupName = InputSanitizer.unsanitizeName(skillsGroup.name)
            res.groupId = skillsGroup.skillId
            res.groupEnabled = Boolean.valueOf(skillsGroup.enabled)
        }
        if (skillDef.selfReportingType == SelfReportingType.Quiz) {
            QuizToSkillDefRepo.QuizNameAndId quizIdAndName = quizToSkillService.getQuizIdForSkillRefId(skillDef.copiedFrom ?: skillDef.id)
            res.quizId = quizIdAndName.getQuizId()
            res.quizName = quizIdAndName.getQuizName()
            res.quizType = quizIdAndName.getQuizType()
        }
        res.name = InputSanitizer.unsanitizeName(res.name)
        res.reusedSkill = SkillReuseIdUtil.isTagged(res.skillId)
        res.name = SkillReuseIdUtil.removeTag(res.name)

        skillTagService.getTagsForSkill(skillDef.id)?.each { tag ->
            res.tags.push(new SkillTagRes(tagId: tag.tagId, tagValue: tag.tagValue))
        }
        return res
    }

    @CompileStatic
    @Profile
    private SkillDefSkinnyRes convertToSkillDefSkinnyRes(SkillDefSkinny skinny) {
        String unsanitizedName = InputSanitizer.unsanitizeName(skinny.name)
        String groupName = skinny.groupId ? skillDefAccessor.getSkillDef(skinny.projectId, skinny.groupId).name : null
        SkillDefSkinnyRes res = new SkillDefSkinnyRes(
                skillId: skinny.skillId,
                projectId: skinny.projectId,
                name: SkillReuseIdUtil.removeTag(unsanitizedName),
                subjectId: skinny.subjectSkillId,
                subjectName: InputSanitizer.unsanitizeName(skinny.subjectName),
                version: skinny.version,
                displayOrder: skinny.displayOrder,
                created: skinny.created,
                totalPoints: skinny.totalPoints,
                isReused: SkillReuseIdUtil.isTagged(skinny.skillId),
                groupName: InputSanitizer.unsanitizeName(groupName),
                groupId: skinny.groupId,
                type: skinny.type
        )
        return res;
    }

    @CompileStatic
    @Profile
    SkillDefPartialRes convertToSkillDefPartialRes(SkillDefPartial partial, boolean loadTags = false, boolean loadNumUsers = false) {
        boolean reusedSkill = SkillReuseIdUtil.isTagged(partial.skillId)
        String unsanitizeName = InputSanitizer.unsanitizeName(partial.name)
        List<SimpleBadgeRes> badges = []
        if(partial.hasBadges) {
            badges = skillDefRepo.findAllBadgesForSkill([partial.skillId], partial.projectId)
        }

        SkillDefPartialRes res = new SkillDefPartialRes(
                skillId: partial.skillId,
                projectId: partial.projectId,
                name: reusedSkill ? SkillReuseIdUtil.removeTag(unsanitizeName) : unsanitizeName,
                subjectId: partial.subjectSkillId,
                subjectName: InputSanitizer.unsanitizeName(partial.subjectName),
                groupId: partial.groupId,
                groupName: InputSanitizer.unsanitizeName(partial.groupName),
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
                selfReportingType: partial.getSelfReportingType(),
                numSkillsRequired: partial.getNumSkillsRequired(),
                enabled: Boolean.valueOf(partial.enabled),
                readOnly: Boolean.valueOf(partial.readOnly),
                copiedFromProjectId: partial.copiedFromProjectId,
                copiedFromProjectName: InputSanitizer.unsanitizeName(partial.copiedFromProjectName),
                sharedToCatalog: partial.sharedToCatalog,
                reusedSkill: reusedSkill,
                quizId: partial.getQuizId(),
                quizName: partial.getQuizName(),
                quizType: partial.getQuizType(),
                iconClass: partial.iconClass,
                badges: badges,
        )

        if (partial.skillType == SkillDef.ContainerType.Skill) {
            res.numPerformToCompletion = (Integer) (res.totalPoints / res.pointIncrement)
        }
        res.totalPoints = partial.totalPoints
        res.numMaxOccurrencesIncrementInterval = partial.numMaxOccurrencesIncrementInterval

        if (loadNumUsers) {
            res.numUsers = calculateDistinctUsersForSkill((SkillDefPartial)partial)
        }

        if (loadTags) {
            skillTagService.getTagsForSkill(partial.id)?.each { tag ->
                res.tags.push(new SkillTagRes(tagId: tag.tagId, tagValue: tag.tagValue))
            }
        }



        if (partial.skillType == SkillDef.ContainerType.SkillsGroup) {
            List<SkillDef> groupChildSkills = skillsGroupAdminService.getSkillsGroupChildSkills(partial.getId())
            res.numSkillsInGroup = groupChildSkills.size()
            res.numSelfReportSkills = groupChildSkills.count( {it.selfReportingType })?.intValue()
        }

        ExpirationAttrs expirationAttrs = skillAttributeService.getExpirationAttrs(partial.projectId, partial.skillId)
        if (expirationAttrs) {
            res.expirationType = expirationAttrs.expirationType
            res.every = expirationAttrs.every
            res.monthlyDay = expirationAttrs.monthlyDay
            res.nextExpirationDate = expirationAttrs.nextExpirationDate
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
    private int calculateDistinctUsersForSkill(SkillDefPartial partial) {
        skillDefRepo.calculateDistinctUsersForASingleSkill(partial.projectId, partial.skillId)
    }

    @Profile
    private List<SkillDefSkinny> loadSkinnySkills(String projectId, String skillNameQuery, boolean excludeImportedSkills = false, boolean includeDisabled = false) {
        skillDefRepo.findAllSkinnySelectByProjectIdAndType(projectId, SkillDef.ContainerType.Skill, skillNameQuery, (!excludeImportedSkills).toString(), includeDisabled.toString())
    }

    @Profile
    private List<SkillDefSkinny> loadSkinnySkillsAndBadges(String projectId, String skillNameQuery, boolean excludeImportedSkills = false, boolean includeDisabled = false) {
        skillDefRepo.findAllSkinnySkillsAndBadgesSelectByProjectId(projectId, skillNameQuery, (!excludeImportedSkills).toString(), includeDisabled.toString())
    }

    @Profile
    private SkillDefSkinny loadSkinnySkill(String projectId, String skillId) {
        return skillDefRepo.getSkinnySkill(projectId, skillId)
    }

    @Profile
    private List<SkillDefSkinny> loadSkinnySkills(String projectId) {
        this.loadSkinnySkills(projectId, '')
    }

    @Profile
    private void validateSkillVersion(SkillRequest skillRequest) {
        Integer latestSkillVersion = findMaxVersionByProjectId(skillRequest.projectId) ?: 0
        if (skillRequest.version > (latestSkillVersion + 1)) {
            throw new SkillException("Latest skill version is [${latestSkillVersion}]; max supported version is latest+1 but provided [${skillRequest.version}] version", skillRequest.projectId, skillRequest.skillId, skills.controller.exceptions.ErrorCode.BadParam)
        }
    }

    @Profile
    private void validateSelfReportVideo(SkillRequest skillRequest, SkillDefWithExtra existingSkillDefinition, boolean validateVideoAttrs) {
        if (skillRequest.selfReportingType == SelfReportingType.Video.toString()) {
            if (validateVideoAttrs) {
                if (!existingSkillDefinition) {
                    throw new SkillException("selfReportingType=Video is not allowed when creating a new skill", skillRequest.projectId, skillRequest.skillId)
                }
                Integer idToValidate = existingSkillDefinition.copiedFrom ?: existingSkillDefinition.id
                String videoUrl = skillAttributesDefRepo.getVideoUrlBySkillRefId(idToValidate)
                if (StringUtils.isBlank(videoUrl)) {
                    throw new SkillException("Video URL must be configured prior to attempting to set selfReportingType=Video", existingSkillDefinition.projectId, existingSkillDefinition.skillId)
                }
            }
        }
    }

    @Profile
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
