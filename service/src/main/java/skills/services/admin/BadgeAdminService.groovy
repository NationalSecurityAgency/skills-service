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
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.auth.UserInfoService
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.request.model.ActionPatchRequest
import skills.controller.request.model.BadgeRequest
import skills.controller.request.model.SkillSettingsRequest
import skills.controller.result.model.BadgeResult
import skills.controller.result.model.DependencyCheckResult
import skills.controller.result.model.SkillDefGraphRes
import skills.controller.result.model.SkillsGraphRes
import skills.services.*
import skills.services.attributes.SkillAttributeService
import skills.services.settings.Settings
import skills.services.settings.SettingsService
import skills.services.userActions.DashboardAction
import skills.services.userActions.DashboardItem
import skills.services.userActions.UserActionInfo
import skills.services.userActions.UserActionsHistoryService
import skills.storage.accessors.ProjDefAccessor
import skills.storage.accessors.SkillDefAccessor
import skills.storage.model.*
import skills.storage.model.auth.RoleName
import skills.storage.repos.*
import skills.storage.repos.nativeSql.PostgresQlNativeRepo
import skills.utils.InputSanitizer
import skills.utils.Props

@Service
@Slf4j
class BadgeAdminService {

    @Value('#{"${skills.circularLearningPathChecker.maxIterations:1000}"}')
    int circularLearningPathCheckerMaxIterations

    @Autowired
    LockingService lockingService

    @Autowired
    CustomValidator customValidator

    @Autowired
    CreatedResourceLimitsValidator createdResourceLimitsValidator

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    SkillDefWithExtraRepo skillDefWithExtraRepo

    @Autowired
    ProjDefAccessor projDefAccessor

    @Autowired
    SkillDefAccessor skillDefAccessor

    @Autowired
    SkillsDepsService skillsDepsService

    @Autowired
    RuleSetDefGraphService ruleSetDefGraphService

    @Autowired
    DisplayOrderService displayOrderService

    @Autowired
    SkillsAdminService skillsAdminService

    @Autowired
    PostgresQlNativeRepo PostgresQlNativeRepo

    @Autowired
    UserAchievedLevelRepo achievedLevelRepo

    @Autowired
    GlobalBadgeLevelDefRepo globalBadgeLevelDefRepo

    @Autowired
    SkillRelDefRepo skillRelDefRepo

    @Autowired
    AttachmentService attachmentService

    @Autowired
    SkillAttributeService skillAttributeService

    @Autowired
    UserActionsHistoryService userActionsHistoryService

    @Autowired
    AccessSettingsStorageService accessSettingsStorageService

    @Autowired
    UserInfoService userInfoService

    @Autowired
    SettingsService settingsService

    @Transactional()
    void saveBadge(String projectId, String originalBadgeId, BadgeRequest badgeRequest, SkillDef.ContainerType type = SkillDef.ContainerType.Badge, boolean performCustomValidation=true) {
        if (performCustomValidation && projectId) {
            CustomValidationResult customValidationResult = customValidator.validate(badgeRequest, projectId)
            if (!customValidationResult.valid) {
                String msg = "Custom validation failed: msg=[${customValidationResult.msg}], type=[badge], badgeId=[${badgeRequest.badgeId}], badgeName=[${badgeRequest.name}], description=[${badgeRequest.description}]"
                throw new SkillException(msg)
            }
        }

        // project id will be null for global badges
        if (projectId) {
            lockingService.lockProject(projectId)
        } else {
            lockingService.lockGlobalBadges()
        }

        SkillDefWithExtra skillDefinition = skillDefWithExtraRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(projectId, originalBadgeId, type)

        if (!skillDefinition || !skillDefinition.skillId.equalsIgnoreCase(badgeRequest.badgeId)) {
            SkillDef idExists = skillDefRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(projectId, badgeRequest.badgeId, type)
            if (idExists) {
                throw new SkillException("Badge with id [${badgeRequest.badgeId}] already exists! Sorry!", projectId, null, ErrorCode.ConstraintViolation)
            }
        }
        if (!skillDefinition || !skillDefinition.name.equalsIgnoreCase(badgeRequest.name)) {
            SkillDef nameExists = skillDefRepo.findByProjectIdAndNameIgnoreCaseAndType(projectId, badgeRequest.name, type)
            if (nameExists) {
                throw new SkillException("Badge with name [${badgeRequest.name}] already exists! Sorry!", projectId, null, ErrorCode.ConstraintViolation)
            }
        }

        boolean identifyEligibleUsers = false
        final boolean isEdit = skillDefinition
        final boolean isIdUpdate = skillDefinition && !skillDefinition.skillId.equalsIgnoreCase(badgeRequest.badgeId)

        if (isEdit) {
            String existingEnabled = skillDefinition.enabled;
            // for updates, use the existing value if it is not set on the badgeRequest (null or empty String)
            if (StringUtils.isBlank(badgeRequest.enabled)) {
                badgeRequest.enabled = existingEnabled
            }
            if (StringUtils.isNotBlank(existingEnabled) && StringUtils.equals(existingEnabled, Boolean.TRUE.toString()) && StringUtils.equals(badgeRequest.enabled, Boolean.FALSE.toString())){
                throw new SkillException("Once a Badge has been published, the only allowable value for enabled is [${Boolean.TRUE.toString()}]", projectId, null, ErrorCode.BadParam)
            }
            if (!Boolean.valueOf(skillDefinition.enabled) && Boolean.valueOf(badgeRequest.enabled)) {
                identifyEligibleUsers = true
            }
            Props.copy(badgeRequest, skillDefinition)
            skillDefinition.skillId = badgeRequest.badgeId
        } else {
            ProjDef projDef
            if (type == SkillDef.ContainerType.Badge) {
                projDef = projDefAccessor.getProjDef(projectId)
                createdResourceLimitsValidator.validateNumBadgesCreated(projectId)
            }

            int displayOrder = getBadgeDisplayOrder(projDef, type)

            skillDefinition = new SkillDefWithExtra(
                    type: type,
                    projectId: projectId,
                    skillId: badgeRequest.badgeId,
                    name: badgeRequest?.name,
                    description: badgeRequest?.description,
                    iconClass: badgeRequest?.iconClass ?: "fa fa-question-circle",
                    startDate: badgeRequest.startDate,
                    endDate: badgeRequest.endDate,
                    projRefId: projDef?.id,
                    displayOrder: displayOrder,
                    helpUrl: badgeRequest.helpUrl,
                    enabled: Boolean.FALSE.toString()
            )
            log.debug("Saving [{}]", skillDefinition)
        }

        SkillDefWithExtra savedSkill

        DataIntegrityExceptionHandlers.badgeDataIntegrityViolationExceptionHandler.handle(projectId) {
            savedSkill = skillDefWithExtraRepo.saveAndFlush(skillDefinition)
        }
        if (savedSkill && type == SkillDef.ContainerType.GlobalBadge && !isEdit) {
            String userId = userInfoService.getCurrentUserId()
            accessSettingsStorageService.addGlobalBadgeAdminUserRoleForUser(userId, savedSkill.skillId, RoleName.ROLE_GLOBAL_BADGE_ADMIN)
        }
        if (savedSkill && type == SkillDef.ContainerType.GlobalBadge && isEdit && isIdUpdate) {
            accessSettingsStorageService.updateGlobalBadgeIdForBadgeAdmins(originalBadgeId, savedSkill.skillId)
        }

        if (savedSkill && type == SkillDef.ContainerType.GlobalBadge && badgeRequest.enableProtectedUserCommunity) {
            settingsService.saveSetting(new SkillSettingsRequest(skillRefId: savedSkill.id, setting: Settings.USER_COMMUNITY_ONLY_PROJECT.settingName, value: Boolean.TRUE.toString()))
        }

        attachmentService.updateAttachmentsAttrsBasedOnUuidsInMarkdown(savedSkill?.description, savedSkill.projectId, null, savedSkill.skillId)

        if(savedSkill && badgeRequest.awardAttrs && type == SkillDef.ContainerType.Badge) {
            skillAttributeService.saveBadgeBonusAwardAttrs(projectId, badgeRequest.badgeId, badgeRequest.awardAttrs)
        }

        if (identifyEligibleUsers) {
            // validate that badge has skills, if not, throw an exception. Can't enable an empty badge
            boolean canEnable = allowEnablingBadge(savedSkill)
            if (!canEnable) {
                String msg = "Badge must have Skills before it can be published"
                if (SkillDef.ContainerType.GlobalBadge == savedSkill.type) {
                    msg = "Badge must have Skills or Project Levels before it can be published"
                }
                throw new SkillException(msg, projectId, savedSkill.skillId, ErrorCode.EmptyBadgeNotAllowed)
            }
            awardBadgeToUsersMeetingRequirements(savedSkill)
        }

        saveUserDashboardAction(savedSkill, badgeRequest, isEdit, type == SkillDef.ContainerType.GlobalBadge)
        log.debug("Saved [{}]", savedSkill)
    }

    @Profile
    private void saveUserDashboardAction(SkillDefWithExtra savedSkill, BadgeRequest badgeRequest, boolean isEdit, boolean isGlobalBadge) {
        Map actionAttributes = [:]
        Closure addAttributes = { Object obj, String prependToKey = null ->
            obj.properties
                    .findAll { key, val -> val instanceof String || val instanceof Number || val instanceof Date }
                    .each { key, val ->
                        String newKey = prependToKey ? "${prependToKey}:${key}" : key
                        actionAttributes[newKey] = val
                    }
        }
        addAttributes(savedSkill)
        if (badgeRequest.awardAttrs) {
            addAttributes(badgeRequest.awardAttrs, "BonusAward")
        }

        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: isEdit ? DashboardAction.Edit : DashboardAction.Create,
                item: isGlobalBadge ? DashboardItem.GlobalBadge : DashboardItem.Badge,
                actionAttributes: actionAttributes,
                itemId: savedSkill.skillId,
                itemRefId: savedSkill.id,
                projectId: savedSkill.projectId,
        ))
    }

    @Transactional
    public void awardBadgeToUsersMeetingRequirements(SkillDefParent badge) {
        if(!badge.projectId) {
            PostgresQlNativeRepo.addGlobalBadgeAchievementForEligibleUsers(badge.skillId,
                    badge.id,
                    Boolean.FALSE,
                    countNumberOfRequiredSkills(badge.skillId),
                    countNumberOfRequiredLevels(badge.skillId),
                    badge.startDate,
                    badge.endDate)
        } else {
            PostgresQlNativeRepo.addBadgeAchievementForEligibleUsers(badge.projectId,
                    badge.skillId,
                    badge.id,
                    Boolean.FALSE,
                    badge.startDate,
                    badge.endDate)
        }
    }

    @Transactional
    void deleteBadge(String projectId, String badgeId, SkillDef.ContainerType type = SkillDef.ContainerType.Badge) {
        log.debug("Deleting badge with project id [{}] and badge id [{}]", projectId, badgeId)
        SkillDef badgeDefinition = skillDefRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(projectId, badgeId, type)
        assert badgeDefinition, "DELETE FAILED -> no badge with project id [$projectId] and badge id [$badgeId]"
        assert badgeDefinition.type == type

        ruleSetDefGraphService.deleteSkillWithItsDescendants(badgeDefinition)

        if (projectId == null) {
            attachmentService.deleteGlobalBadgeAttachments(badgeId)

            accessSettingsStorageService.deleteGlobalBadgeUserRoles(badgeId)
        }

        // reset display order attribute - make sure the order is continuous - 0...N
        ProjDef projDef
        if (type == SkillDef.ContainerType.Badge) {
            projDef = projDefAccessor.getProjDef(projectId)
        }
        List<SkillDef> badges = getBadgesInternal(projDef, type)
        badges = badges?.findAll({ it.id != badgeDefinition.id }) // need to remove because of JPA level caching?
        displayOrderService.resetDisplayOrder(badges)

        boolean isGlobalBadge = type == SkillDef.ContainerType.GlobalBadge
        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: DashboardAction.Delete,
                item: isGlobalBadge ? DashboardItem.GlobalBadge : DashboardItem.Badge,
                itemId: badgeDefinition.skillId,
                projectId: badgeDefinition.projectId,
        ))
        log.debug("Deleted badge with id [{}]", badgeDefinition)
    }

    @Transactional(readOnly = true)
    List<BadgeResult> getBadges(String projectId) {
        List<SkillDefWithExtra> badges = skillDefWithExtraRepo.findAllByProjectIdAndType(projectId, SkillDef.ContainerType.Badge)
        List<BadgeResult> res = badges.collect { convertToBadge(it) }
        return res?.sort({ it.displayOrder })
    }

    @Transactional(readOnly = true)
    BadgeResult getBadge(String projectId, String badgeId) {
        SkillDefWithExtra skillDef = skillDefWithExtraRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(projectId, badgeId, SkillDef.ContainerType.Badge)
        if (!skillDef) {
            throw new SkillException("Badge [${badgeId}] doesn't exist.", projectId, null, ErrorCode.BadgeNotFound)
        }
        return convertToBadge(skillDef, true)
    }

    @Transactional
    void setBadgeDisplayOrder(String projectId, String badgeId, ActionPatchRequest badgePatchRequest) {
        lockingService.lockProject(projectId)
        ProjDef projDef = projDefAccessor.getProjDef(projectId)
        if(ActionPatchRequest.ActionType.NewDisplayOrderIndex == badgePatchRequest.action) {
            List<SkillDef> badges = skillDefRepo.findAllByProjectIdAndType(projDef.projectId, SkillDef.ContainerType.Badge)
            displayOrderService.updateDisplayOrderByUsingNewIndex(badgeId, badges, badgePatchRequest)
        }
    }

    @Transactional()
    @Profile
    void addSkillToBadge(String projectId, String badgeId, String skillid) {
        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: DashboardAction.AssignSkill,
                item: DashboardItem.Badge,
                actionAttributes: [
                        skillId: skillid,
                        badgeId: badgeId,
                ],
                itemId: badgeId,
                projectId: projectId,
        ))
        ruleSetDefGraphService.assignGraphRelationship(projectId, badgeId, SkillDef.ContainerType.Badge, skillid, SkillRelDef.RelationshipType.BadgeRequirement, true)
        validateAgainstLearningPath(projectId, badgeId, skillid)
    }

    @Transactional()
    @Profile
    void addSkillsToBadge(String projectId, String badgeId, List<String> skillIds) {
        skillIds.each {skillid ->
            ruleSetDefGraphService.assignGraphRelationship(projectId, badgeId, SkillDef.ContainerType.Badge, skillid, SkillRelDef.RelationshipType.BadgeRequirement, true)
            validateAgainstLearningPath(projectId, badgeId, skillid)

            userActionsHistoryService.saveUserAction(new UserActionInfo(
                    action: DashboardAction.AssignSkill,
                    item: DashboardItem.Badge,
                    actionAttributes: [
                        skillId: skillid,
                        badgeId: badgeId,
                    ],
                    itemId: badgeId,
                    projectId: projectId,
            ))
        }
    }

    @Profile
    private void validateAgainstLearningPath(String projectId, String badgeId, String skillid) {
        SkillDef badge = skillDefAccessor.getSkillDef(projectId, badgeId, [SkillDef.ContainerType.Badge])
        SkillsGraphRes existingGraph = skillsDepsService.getDependentSkillsGraph(projectId)
        List<CircularLearningPathChecker.BadgeAndSkills> badgeAndSkills = skillsDepsService.loadBadgeSkills(projectId)
        if (existingGraph.nodes) {
            List<SkillDefGraphRes> badgeNodes = existingGraph.nodes.findAll { it.skillId == badge.skillId }
            if (badgeNodes) {
                for (SkillDefGraphRes badgeNode : badgeNodes) {
                    List<SkillsGraphRes.Edge> edgesToCheck = existingGraph.edges.findAll { it.fromId == badgeNode.id || it.toId == badgeNode.id }
                    for (SkillsGraphRes.Edge edge : edgesToCheck) {
                        SkillDefGraphRes skillDefItem = existingGraph.nodes.find { it.id == edge.fromId }
                        SkillDefGraphRes prereqDefItem = existingGraph.nodes.find { it.id == edge.toId }
                        SkillDef skillDef = skillDefItem.skillId == badge.skillId ? badge : skillDefAccessor.getSkillDef(projectId, skillDefItem.skillId, [skillDefItem.type])
                        SkillDef prreqDef = prereqDefItem.skillId == badge.skillId ? badge : skillDefAccessor.getSkillDef(projectId, prereqDefItem.skillId, [prereqDefItem.type])
                        CircularLearningPathChecker circularLearningPathChecker = new CircularLearningPathChecker(
                                circularLearningPathCheckerMaxIterations: circularLearningPathCheckerMaxIterations,
                                badgeAndSkills: badgeAndSkills,
                                skillDef: skillDef, prereqSkillDef: prreqDef, existingGraph: existingGraph, performAlreadyExistCheck: false)
                        DependencyCheckResult dependencyCheckResult = circularLearningPathChecker.check()
                        if (!dependencyCheckResult.possible) {
                            String msg = "Adding skill [${skillid}] to badge [${badge.skillId}] violates the Learning Path. Reason: ${dependencyCheckResult.reason}"
                            throw new SkillException(msg, projectId, skillid, ErrorCode.LearningPathViolation)
                        }
                    }
                }
            }
        }
    }

    @Transactional()
    void removeSkillFromBadge(String projectId, String badgeId, String skillid) {
        //validate that removing this skill would not result in an empty badge
        SkillDefWithExtra badge = skillDefWithExtraRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(projectId, badgeId, SkillDef.ContainerType.Badge)

        if (!badge) {
            throw new SkillException("Badge [${badgeId}] does not exist", projectId, badgeId, ErrorCode.BadgeNotFound)
        }

        long numSkills = skillDefRepo.countChildSkillsByIdAndRelationshipTypeAndEnabled(badge.id, SkillRelDef.RelationshipType.BadgeRequirement, "true")

        if(badge.enabled == 'true' && numSkills == 1) {
            throw new SkillException("Can not remove skill from badge [${badgeId}] as it is live with only a single skill")
        }

        ruleSetDefGraphService.removeGraphRelationship(projectId, badgeId, SkillDef.ContainerType.Badge,
                projectId, skillid, SkillRelDef.RelationshipType.BadgeRequirement)

        awardBadgeToUsersMeetingRequirements(badge)

        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: DashboardAction.RemoveSkillAssignment,
                item: DashboardItem.Badge,
                actionAttributes: [
                        skillId: skillid,
                        badgeId: badgeId,
                ],
                itemId: badgeId,
                projectId: projectId,
        ))
    }

    @Transactional(readOnly = true)
    boolean existsByBadgeName(String projectId, String subjectName) {
        return skillDefRepo.existsByProjectIdAndNameAndTypeAllIgnoreCase(projectId, subjectName, SkillDef.ContainerType.Badge)
    }

    @Profile
    private BadgeResult convertToBadge(SkillDefWithExtra skillDef, boolean loadRequiredSkills = false) {
        def awardAttributes = skillAttributeService.getBonusAwardAttrs(skillDef.projectId, skillDef.skillId)
        BadgeResult res = new BadgeResult(
                badgeId: skillDef.skillId,
                projectId: skillDef.projectId,
                name: InputSanitizer.unsanitizeName(skillDef.name),
                description: InputSanitizer.unsanitizeForMarkdown(skillDef.description),
                displayOrder: skillDef.displayOrder,
                iconClass: skillDef.iconClass,
                startDate: skillDef.startDate,
                endDate: skillDef.endDate,
                helpUrl: InputSanitizer.unsanitizeUrl(skillDef.helpUrl),
                enabled: skillDef.enabled,
                awardAttrs: awardAttributes
        )

        if (loadRequiredSkills) {
            List<SkillDef> dependentSkills = getRequiredBadgeSkills(skillDef.id)
            res.requiredSkills = dependentSkills?.collect { skillsAdminService.convertToSkillDefRes(it) }
            res.numSkills = dependentSkills ? dependentSkills.size() : 0
            res.totalPoints = dependentSkills ? dependentSkills?.collect({ it.totalPoints })?.sum() : 0
        } else {
            res.numSkills = skillDefRepo.countChildSkillsByIdAndRelationshipTypeAndEnabled(skillDef.id, SkillRelDef.RelationshipType.BadgeRequirement, "true")
            if (res.numSkills > 0) {
                res.totalPoints = skillDefRepo.sumChildSkillsTotalPointsBySkillAndRelationshipType(skillDef.id, SkillRelDef.RelationshipType.BadgeRequirement)
            } else {
                res.totalPoints = 0
            }
        }
        return res
    }

    private List<SkillDef> getRequiredBadgeSkills(Integer badgeId) {
        return skillDefRepo.findChildSkillsByIdAndRelationshipType(badgeId, SkillRelDef.RelationshipType.BadgeRequirement)
    }

    private Integer getBadgeDisplayOrder(ProjDef projDef, SkillDef.ContainerType type) {
        Integer lastDisplayOrder = (type == SkillDef.ContainerType.GlobalBadge) ?
                skillDefRepo.getMaxDisplayOrderByTypeAndProjectIdIsNull(SkillDef.ContainerType.GlobalBadge) :
                skillDefRepo.getMaxDisplayOrderByProjectIdAndType(projDef.projectId, SkillDef.ContainerType.Badge)
        int displayOrder = lastDisplayOrder != null ? lastDisplayOrder + 1 : 1
        return displayOrder
    }

    private List<SkillDef> getBadgesInternal(ProjDef projDef, SkillDef.ContainerType type) {
        List<SkillDef> badges
        if (type == SkillDef.ContainerType.GlobalBadge) {
            badges = skillDefRepo.findAllByProjectIdAndType(null, SkillDef.ContainerType.GlobalBadge)
        } else {
            badges = skillDefRepo.findAllByProjectIdAndType(projDef.projectId, SkillDef.ContainerType.Badge)
        }
        return badges
    }

    private Integer countNumberOfRequiredSkills(String badgeId){
        Integer badgeSkillCount = skillRelDefRepo.getGlobalBadgeSkillCount(badgeId)
        return badgeSkillCount
    }

    private Integer countNumberOfRequiredLevels(String badgeId){
        Integer badgeLevelCount =  globalBadgeLevelDefRepo.countByBadgeId(badgeId)
        return badgeLevelCount
    }

    private boolean allowEnablingBadge(SkillDefWithExtra badge) {
        boolean valid = false;

        boolean hasSKills = false
        List<SkillDef> badgeSkills = getRequiredBadgeSkills(badge.id)
        if (badgeSkills?.size() > 0) {
            hasSKills = true
            valid = hasSKills
        }

        if (SkillDef.ContainerType.GlobalBadge == badge.type) {
            List<GlobalBadgeLevelDef> projectLevels = globalBadgeLevelDefRepo.findAllByBadgeId(badge.skillId)
            valid = hasSKills || projectLevels?.size() > 0
        }

        return valid
    }
}
