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
import skills.controller.request.model.PointSyncPatchRequest
import skills.controller.request.model.SkillRequest
import skills.controller.result.model.SkillDefPartialRes
import skills.controller.result.model.SkillDefRes
import skills.controller.result.model.SkillDefSkinnyRes
import skills.services.*
import skills.storage.accessors.SkillDefAccessor
import skills.storage.model.QueuedSkillUpdate
import skills.storage.model.SkillDef
import skills.storage.model.SkillDef.SelfReportingType
import skills.storage.model.SkillDefWithExtra
import skills.storage.model.SkillRelDef
import skills.storage.repos.ProjDefRepo
import skills.storage.repos.QueuedSkillUpdateRepo
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

    @Autowired
    SkillCatalogService skillCatalogService

    @Autowired
    QueuedSkillUpdateRepo queuedSkillUpdateRepo

    @Autowired
    SkillsGroupAdminService skillsGroupAdminService

    @Autowired
    ProjDefRepo projDefRepo

    @Transactional
    void syncSkillPointsForSkillsGroup(String projectId,
                                       String subjectId,
                                       String groupId,
                                       PointSyncPatchRequest patchRequest) {
        lockingService.lockProject(projectId)

        SkillDef skillsGroupSkillDef = skillDefRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(projectId, groupId, SkillDef.ContainerType.SkillsGroup)
        List<SkillDef> groupChildSkills = skillsGroupAdminService.getSkillsGroupChildSkills(skillsGroupSkillDef.id)

        final int totalPointsRequested = patchRequest.pointIncrement * patchRequest.numPerformToCompletion
        final int pointIncrement = patchRequest.pointIncrement

        def origAttrs = [:]
        groupChildSkills.each {childSkillDef ->
            origAttrs.put(childSkillDef, [childSkillDef.totalPoints, childSkillDef.pointIncrement])
            childSkillDef.pointIncrement = pointIncrement
            childSkillDef.totalPoints = totalPointsRequested
            DataIntegrityExceptionHandlers.skillDataIntegrityViolationExceptionHandler.handle(projectId, childSkillDef.skillId) {
                skillDefWithExtraRepo.save(childSkillDef)
            }
        }

        // if enabled, validate and update points and achievements
        if (Boolean.valueOf(skillsGroupSkillDef.enabled)) {
            // need to validate skills group
            skillsGroupAdminService.validateSkillsGroupAndReturnChildren(skillsGroupSkillDef.numSkillsRequired, true, skillsGroupSkillDef.id)
            groupChildSkills.each {childSkillDef ->
                final int incrementRequested = pointIncrement
                final int currentOccurrences = (origAttrs.get(childSkillDef)[0] / origAttrs.get(childSkillDef)[1])
                final int occurrencesDelta = patchRequest.numPerformToCompletion - currentOccurrences
                final int pointIncrementDelta = incrementRequested - pointIncrement
                log.debug("Rebuilding scores for [${}]", childSkillDef.skillId)
                ruleSetDefinitionScoreUpdater.updateFromLeaf(childSkillDef)
                updatePointsAndAchievements(childSkillDef, subjectId, pointIncrementDelta, occurrencesDelta, currentOccurrences, 0, skillsGroupSkillDef, groupChildSkills)
            }
        }
    }

    @Transactional()
    void saveSkill(String originalSkillId, SkillRequest skillRequest, boolean performCustomValidation=true, String groupId=null) {
        lockingService.lockProject(skillRequest.projectId)

        validateSkillVersion(skillRequest)

        if (performCustomValidation) {
            final CustomValidationResult customValidationResult = customValidator.validate(skillRequest)
            if (!customValidationResult.valid) {
                String msg = "Custom validation failed: msg=[${customValidationResult.msg}], type=[skill], skillId=[${skillRequest.skillId}], name=[${skillRequest.name}], description=[${skillRequest.description}]"
                throw new SkillException(msg)
            }
        }

        SkillDefWithExtra skillDefinition = skillDefWithExtraRepo.findByProjectIdAndSkillIdIgnoreCaseAndTypeIn(skillRequest.projectId, originalSkillId, [SkillDef.ContainerType.Skill, SkillDef.ContainerType.SkillsGroup])

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

        SkillDef subject = null
        SkillDef skillsGroupSkillDef = null
        List<SkillDef> groupChildSkills = null
        if (isEdit) {
            if (skillDefinition.readOnly && !(skillRequest instanceof ReplicatedSkillUpdateRequest)) {
                throw new SkillException("Skill with id [${skillRequest.skillId}] has been imported from the Global Catalog and cannot be altered", skillRequest.projectId, skillRequest.skillId, ErrorCode.ReadOnlySkill)
            }
            // for updates, use the existing value if it is not set on the skillRequest (null or empty String)
            if (StringUtils.isBlank(skillRequest.enabled)) {
                skillRequest.enabled = skillDefinition.enabled
            }
            if (isSkillsGroup) {
                // need to update total points for the group
                groupChildSkills = skillsGroupAdminService.validateSkillsGroupAndReturnChildren(skillRequest, skillDefinition)
                totalPointsRequested = skillsGroupAdminService.getGroupTotalPoints(groupChildSkills, skillRequest.numSkillsRequired)

                if (Boolean.valueOf(skillRequest.enabled) && skillDefinition.numSkillsRequired != skillRequest.numSkillsRequired) {
                    int currentNumSkillsRequired = skillDefinition.numSkillsRequired == -1 ? groupChildSkills.size() : skillDefinition.numSkillsRequired
                    int requestedNumSkillsRequired = skillRequest.numSkillsRequired == -1 ? groupChildSkills.size() : skillRequest.numSkillsRequired
                    numSkillsRequiredDelta = requestedNumSkillsRequired - currentNumSkillsRequired
                }

                if (Boolean.valueOf(skillDefinition.enabled) != Boolean.valueOf(skillRequest.enabled)) {
                    // enabling or disabling, need to update child skills enabled to match the group value
                    groupChildSkills.each { it.enabled = skillRequest.enabled }
                    skillDefRepo.saveAll(groupChildSkills)
                }
            }
            shouldRebuildScores = skillDefinition.totalPoints != totalPointsRequested || (!Boolean.valueOf(skillDefinition.enabled) && Boolean.valueOf(skillRequest.enabled))
            occurrencesDelta = isSkillsGroup ? 0 : skillRequest.numPerformToCompletion - currentOccurrences
            updateUserPoints = shouldRebuildScores || occurrencesDelta != 0
            pointIncrementDelta = isSkillsGroup ? 0 : incrementRequested - skillDefinition.pointIncrement

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

            Integer highestDisplayOrder = skillDefRepo.calculateChildSkillsHighestDisplayOrder(skillRequest.projectId, groupId ?: parentSkillId)
            int displayOrder = highestDisplayOrder == null ? 1 : highestDisplayOrder + 1
            String enabled = isSkillsGroup ? Boolean.FALSE.toString() : Boolean.TRUE.toString()
            if (isSkillsGroupChild) {
                skillsGroupSkillDef = skillDefRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(skillRequest.projectId, groupId, SkillDef.ContainerType.SkillsGroup)
                if (!skillsGroupSkillDef) {
                    throw new SkillException("[${groupId}] groupId was not found.".toString(), skillRequest.projectId, skillRequest.skillId, ErrorCode.BadParam)
                }
                enabled = skillsGroupSkillDef.enabled
            }
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
                    type: skillType,
                    version: skillRequest.version,
                    selfReportingType: selfReportingType,
                    numSkillsRequired: skillRequest.numSkillsRequired,
                    enabled: enabled,
                    groupId: groupId,
            )

            if (skillRequest instanceof SkillImportRequest) {
                skillDefinition.copiedFrom = skillRequest.copiedFrom
                skillDefinition.readOnly = skillRequest.readOnly
                skillDefinition.copiedFromProjectId = skillRequest.copiedFromProjectId
            }
            log.debug("Saving [{}]", skillDefinition)
            shouldRebuildScores = true
        }

        DataIntegrityExceptionHandlers.skillDataIntegrityViolationExceptionHandler.handle(skillRequest.projectId, skillRequest.skillId) {
            skillDefWithExtraRepo.save(skillDefinition)
        }

        SkillDef savedSkill = skillDefRepo.findByProjectIdAndSkillIdAndType(skillRequest.projectId, skillRequest.skillId, skillType)
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

        if (isSkillsGroupChild) {
            // need to validate skills group
            if (!skillsGroupSkillDef) {
                skillsGroupSkillDef = skillDefRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(skillRequest.projectId, groupId, SkillDef.ContainerType.SkillsGroup)
            }
            groupChildSkills = skillsGroupAdminService.validateSkillsGroupAndReturnChildren(skillsGroupSkillDef.numSkillsRequired, Boolean.valueOf(skillsGroupSkillDef.enabled), skillsGroupSkillDef.id)
        }

        if (shouldRebuildScores) {
            log.debug("Rebuilding scores for [${}]", savedSkill.skillId)
            ruleSetDefinitionScoreUpdater.updateFromLeaf(savedSkill)
        }

        if (isEdit) {
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
            if (skillCatalogService.isAvailableInCatalog(savedSkill.projectId, savedSkill.skillId)) {
                SkillDefWithExtra extra = skillDefWithExtraRepo.findById(savedSkill.id).get()
                QueuedSkillUpdate queuedSkillUpdate = new QueuedSkillUpdate(skill:  extra, isCatalogSkill: true)
                queuedSkillUpdateRepo.save(queuedSkillUpdate)
            }
        }

        log.debug("Saved [{}]", savedSkill)
    }

    private void updatePointsAndAchievements(SkillDef savedSkill, String subjectId, int pointIncrementDelta, int occurrencesDelta, int currentOccurrences, int numSkillsRequiredDelta, SkillDef skillsGroupSkillDef, List<SkillDef> groupChildSkills) {
        if (savedSkill.type != SkillDef.ContainerType.SkillsGroup) {
            // order is CRITICAL HERE
            // must update point increment first then deal with changes in the occurrences;
            // changes in the occurrences will use the newly updated point increment
            if (pointIncrementDelta != 0) {
                userPointsManagement.handlePointIncrementUpdate(savedSkill.projectId, subjectId, savedSkill.skillId, pointIncrementDelta)
            }
            int newOccurrences = savedSkill.totalPoints / savedSkill.pointIncrement
            if (occurrencesDelta < 0) {
                // order is CRITICAL HERE
                // Must update points prior removal of UserPerformedSkill events as the removal relies on the existence of those extra events
                userPointsManagement.updatePointsWhenOccurrencesAreDecreased(savedSkill.projectId, subjectId, savedSkill.skillId, savedSkill.pointIncrement, newOccurrences)
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

        if ((occurrencesDelta < 0 || numSkillsRequiredDelta < 0) && skillsGroupSkillDef && Boolean.valueOf(skillsGroupSkillDef.enabled)) {
            int numSkillsRequired = skillsGroupSkillDef.numSkillsRequired == -1 ? groupChildSkills.size() : skillsGroupSkillDef.numSkillsRequired
            userPointsManagement.insertUserAchievementWhenDecreaseOfSkillsRequiredCausesUsersToAchieve(savedSkill.projectId, skillsGroupSkillDef.skillId, skillsGroupSkillDef.id, groupChildSkills.collect {it.skillId}, numSkillsRequired)
        }

        if (pointIncrementDelta < 0 || occurrencesDelta < 0) {
            SkillDef subject = skillDefRepo.findByProjectIdAndSkillIdAndType(savedSkill.projectId, subjectId, SkillDef.ContainerType.Subject)
            userPointsManagement.identifyAndAddLevelAchievements(subject)
        }
        log.debug("Saved [{}]", savedSkill)
    }

    @Transactional
    void deleteSkill(String projectId, String subjectId, String skillId) {
        log.debug("Deleting skill with project id [{}] and subject id [{}] and skill id [{}]", projectId, subjectId, skillId)
        SkillDef skillDefinition = skillDefRepo.findByProjectIdAndSkillIdIgnoreCaseAndTypeIn(projectId, skillId, [SkillDef.ContainerType.Skill, SkillDef.ContainerType.SkillsGroup])
        assert skillDefinition, "DELETE FAILED -> no skill with project find with projectId=[$projectId], subjectId=[$subjectId], skillId=[$skillId]"

        if (globalBadgesService.isSkillUsedInGlobalBadge(skillDefinition)) {
            throw new SkillException("Skill with id [${skillId}] cannot be deleted as it is currently referenced by one or more global badges")
        }
        SkillDef parentSkill = ruleSetDefGraphService.getParentSkill(skillDefinition)
        SkillDef subject
        if (parentSkill.type == SkillDef.ContainerType.Subject) {
            subject = parentSkill
        } else if (parentSkill.type == SkillDef.ContainerType.SkillsGroup) {
            subject = skillDefRepo.findByProjectIdAndSkillIdAndType(projectId, subjectId, SkillDef.ContainerType.Subject)
        } else {
            throw new SkillException("Unexpected parent type [${parentSkill.type}]")
        }

        //we need to check to see if this skill belongs to any badges, if so we need to look for any users who now qualify
        //for those badges
        ruleSetDefinitionScoreUpdater.skillToBeRemoved(skillDefinition)

        // this MUST happen before the skill was removed as sql relies on the skill to exist
        userPointsManagement.handleSkillRemoval(skillDefinition, subject)

        //identify any badges that this skill belonged to and award the badge if any users now qualify for this badge
        List<SkillDef> badges = findAllBadgesSkillBelongsTo(skillDefinition.skillId)

        ruleSetDefGraphService.deleteSkillWithItsDescendants(skillDefinition)
        log.debug("Deleted skill [{}]", skillDefinition.skillId)

        // this MUST happen after the skill was removed as sql relies on the skill to be gone
        userPointsManagement.identifyAndAddLevelAchievements(subject)

        // make sure skills group is still valid and update group's totalPoints
        if (skillDefinition.groupId) {
            assert parentSkill.type == SkillDef.ContainerType.SkillsGroup
            List<SkillDef> children = skillsGroupAdminService.validateCanDeleteChildSkillAndReturnChildren(parentSkill)
            if (children.size() == parentSkill.numSkillsRequired) {
                parentSkill.numSkillsRequired = -1
            }
            parentSkill.totalPoints = skillsGroupAdminService.getGroupTotalPoints(children, parentSkill.numSkillsRequired)
            DataIntegrityExceptionHandlers.skillDataIntegrityViolationExceptionHandler.handle(projectId, skillId) {
                skillDefWithExtraRepo.save(parentSkill)
            }

            if (Boolean.valueOf(parentSkill.enabled)) {
                int numSkillsRequired = parentSkill.numSkillsRequired == -1 ? children.size() : parentSkill.numSkillsRequired
                userPointsManagement.insertUserAchievementWhenDecreaseOfSkillsRequiredCausesUsersToAchieve(projectId, parentSkill.skillId, parentSkill.id, children.collect { it.skillId }, numSkillsRequired)
            }
        }

        badges?.each {
            badgeAdminService.awardBadgeToUsersMeetingRequirements(it)
        }

        List<SkillDef> siblings = ruleSetDefGraphService.getChildrenSkills(parentSkill, [SkillRelDef.RelationshipType.RuleSetDefinition, SkillRelDef.RelationshipType.SkillsGroupRequirement])
        displayOrderService.resetDisplayOrder(siblings)

        if (skillCatalogService.isAvailableInCatalog(skillDefinition)) {
            List<SkillDef> related = skillCatalogService.getRelatedSkills(skillDefinition)
            log.info("catalog skill is being deleted, deleting [{}] copies imported into other projects", related?.size())
            //TODO: move this to an async process
            related?.each {
                SkillDef subj = skillRelDefRepo.findAllByChildAndType(it, SkillRelDef.RelationshipType.RuleSetDefinition)
                deleteSkill(it.projectId, subj.skillId, it.skillId)
            }
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
    List<SkillDefPartialRes> getSkillsForSubjectWithCatalogStatus(String projectId, String subjectId) {
        SkillDef subject = skillDefRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(projectId, subjectId, SkillDef.ContainerType.Subject)
        if (!subject) {
            ErrorCode code = ErrorCode.SubjectNotFound
            throw new SkillException("Subject [${subjectId}] doesn't exist.", projectId, null, code)
        }

        List<SkillDefRepo.SkillDefPartial> res
        res = skillRelDefRepo.getSkillsWithCatalogStatus(projectId, subject.skillId)
        return res.collect { convertToSkillDefPartialRes(it) }.sort({ it.displayOrder })
    }

    @Transactional(readOnly = true)
    List<SkillDefSkinnyRes> getSkinnySkills(String projectId, String skillNameQuery) {
        List<SkillDefRepo.SkillDefSkinny> data = loadSkinnySkills(projectId, skillNameQuery)
        List<SkillDefPartialRes> res = data.collect { convertToSkillDefSkinnyRes(it) }?.sort({ it.skillId })
        return res
    }

    @Transactional(readOnly = true)
    SkillDefRes getSkill(String projectId, String subjectId, String skillId) {
        SkillDefWithExtra res = skillDefWithExtraRepo.findByProjectIdAndSkillIdIgnoreCaseAndTypeIn(projectId, skillId, [SkillDef.ContainerType.Skill, SkillDef.ContainerType.SkillsGroup])
        if (!res) {
            throw new SkillException("Skill [${skillId}] doesn't exist.", projectId, null, ErrorCode.SkillNotFound)
        }

        SkillDefRes finalRes = convertToSkillDefRes(res)
        finalRes.sharedToCatalog = skillCatalogService.isAvailableInCatalog(res.projectId, res.skillId)
        if (finalRes.copiedFromProjectId) {
            finalRes.copiedFromProjectName = projDefRepo.getProjectName(finalRes.copiedFromProjectId)?.projectName
        }
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
        return res
    }


    @Profile
    private SkillDefRes convertToSkillDefRes(SkillDefWithExtra skillDef) {
        SkillDefRes res = new SkillDefRes()
        Props.copy(skillDef, res)
        res.enabled = skillDef.enabled == "true" ? true : false
        res.description = InputSanitizer.unsanitizeForMarkdown(res.description)
        res.helpUrl = InputSanitizer.unsanitizeUrl(res.helpUrl)
        if (skillDef.type == SkillDef.ContainerType.Skill) {
            res.numPerformToCompletion = skillDef.totalPoints / res.pointIncrement
        }
        if (skillDef.type == SkillDef.ContainerType.SkillsGroup) {
            List<SkillDef> groupChildSkills = skillsGroupAdminService.getSkillsGroupChildSkills(skillDef.getId())
            res.numSkillsInGroup = groupChildSkills.size()
            res.numSelfReportSkills = groupChildSkills.count( {it.selfReportingType })?.intValue()
            res.enabled = Boolean.valueOf(skillDef.enabled)
        }
        if (skillDef.groupId) {
            SkillDefWithExtra skillsGroup = skillsGroupAdminService.getSkillsGroup(skillDef.projectId, skillDef.groupId)
            res.enabled = Boolean.valueOf(skillDef.enabled)
            res.groupName = skillsGroup.name
            res.groupId = skillsGroup.skillId
        }

        return res
    }

    @CompileStatic
    @Profile
    private SkillDefSkinnyRes convertToSkillDefSkinnyRes(SkillDefRepo.SkillDefSkinny skinny) {
        SkillDefSkinnyRes res = new SkillDefSkinnyRes(
                skillId: skinny.skillId,
                projectId: skinny.projectId,
                name: skinny.name,
                subjectId: skinny.subjectSkillId,
                subjectName: skinny.subjectName,
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
                subjectId: partial.subjectSkillId,
                subjectName: partial.subjectName,
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
                readOnly: partial.readOnly,
                copiedFromProjectId: partial.copiedFromProjectId,
                copiedFromProjectName: partial.copiedFromProjectName,
                sharedToCatalog: partial.sharedToCatalog
        )

        if (partial.skillType == SkillDef.ContainerType.Skill) {
            res.numPerformToCompletion = (Integer) (res.totalPoints / res.pointIncrement)
        }
        res.totalPoints = partial.totalPoints
        res.numMaxOccurrencesIncrementInterval = partial.numMaxOccurrencesIncrementInterval

        if (loadNumUsers) {
            res.numUsers = calculateDistinctUsersForSkill((SkillDefRepo.SkillDefPartial)partial)
        }

        if (partial.skillType == SkillDef.ContainerType.SkillsGroup) {
            List<SkillDef> groupChildSkills = skillsGroupAdminService.getSkillsGroupChildSkills(partial.getId())
            res.numSkillsInGroup = groupChildSkills.size()
            res.numSelfReportSkills = groupChildSkills.count( {it.selfReportingType })?.intValue()
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
    private List<SkillDefRepo.SkillDefSkinny> loadSkinnySkills(String projectId, String skillNameQuery) {
        skillDefRepo.findAllSkinnySelectByProjectIdAndType(projectId, SkillDef.ContainerType.Skill, skillNameQuery)
    }

    @Profile
    private List<SkillDefRepo.SkillDefSkinny> loadSkinnySkills(String projectId) {
        this.loadSkinnySkills(projectId, '')
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
