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
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.request.model.ActionPatchRequest
import skills.controller.request.model.BadgeRequest
import skills.controller.result.model.BadgeResult
import skills.services.*
import skills.storage.model.ProjDef
import skills.storage.model.SkillDef
import skills.storage.model.SkillDefWithExtra
import skills.storage.model.SkillRelDef
import skills.storage.accessors.ProjDefAccessor
import skills.storage.model.UserAchievement
import skills.storage.repos.GlobalBadgeLevelDefRepo
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.SkillDefWithExtraRepo
import skills.storage.repos.SkillRelDefRepo
import skills.storage.repos.UserAchievedLevelRepo
import skills.storage.repos.nativeSql.NativeQueriesRepo
import skills.utils.InputSanitizer
import skills.utils.Props

@Service
@Slf4j
class BadgeAdminService {

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
    RuleSetDefGraphService ruleSetDefGraphService

    @Autowired
    DisplayOrderService displayOrderService

    @Autowired
    SkillsAdminService skillsAdminService

    @Autowired
    NativeQueriesRepo nativeQueriesRepo

    @Autowired
    UserAchievedLevelRepo achievedLevelRepo

    @Autowired
    GlobalBadgeLevelDefRepo globalBadgeLevelDefRepo

    @Autowired
    SkillRelDefRepo skillRelDefRepo

    @Transactional()
    void saveBadge(String projectId, String originalBadgeId, BadgeRequest badgeRequest, SkillDef.ContainerType type = SkillDef.ContainerType.Badge, boolean performCustomValidation=true) {
        CustomValidationResult customValidationResult = customValidator.validate(badgeRequest)
        if(performCustomValidation && !customValidationResult.valid){
            String msg = "Custom validation failed: msg=[${customValidationResult.msg}], type=[badge], badgeId=[${badgeRequest.badgeId}], badgeName=[${badgeRequest.name}], description=[${badgeRequest.description}]"
            throw new SkillException(msg)
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

        if (skillDefinition) {
            if (StringUtils.isNotBlank(badgeRequest.enabled) && !StringUtils.equals(badgeRequest.enabled, Boolean.TRUE.toString())){
                throw new SkillException("Once a Badge has been published, the only allowable value for enabled is [${Boolean.TRUE.toString()}]", projectId, null, ErrorCode.BadParam)
            }
            //TODO: if previous value of enable was false and badgeRequest.enable is true
            //then we need to trigger evaluation of user dependencies and award achievements
            //...wait...how does this work with notifications? Users not logged in wouldn't know that
            //they were awarded the badge would they?
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

            if (StringUtils.isNotEmpty(badgeRequest.enabled) && !StringUtils.equals(badgeRequest.enabled, Boolean.FALSE.toString())) {
                throw new SkillException("The only allowable value for enabled is [${Boolean.FALSE.toString()}] on Badge creation", projectId, null, ErrorCode.BadParam)
            }

            badgeRequest.enabled = Boolean.FALSE.toString()

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
                    projDef: projDef,
                    displayOrder: displayOrder,
                    helpUrl: badgeRequest.helpUrl,
                    enabled: badgeRequest.enabled
            )
            log.debug("Saving [{}]", skillDefinition)
        }

        SkillDefWithExtra savedSkill

        DataIntegrityExceptionHandlers.badgeDataIntegrityViolationExceptionHandler.handle(projectId) {
            savedSkill = skillDefWithExtraRepo.save(skillDefinition)
        }

        if (identifyEligibleUsers) {
            identifyUsersMeetingBadgeRequirements(projectId, originalBadgeId, savedSkill.startDate, savedSkill.endDate)?.each {
                saveBadgeAchievement(it, projectId, savedSkill.skillId, savedSkill.id, Boolean.FALSE.toString())
            }
        }

        log.debug("Saved [{}]", savedSkill)
    }

    private List<String> identifyUsersMeetingBadgeRequirements(String projectId, String badgeId, Date startDate, Date endDate){
        List<String> userIds
        if(projectId == null) {
            userIds = nativeQueriesRepo.findUsersEligbleForGlobalBadge(badgeId,
                    countNumberOfRequiredSkills(badgeId),
                    countNumberOfRequiredLevels(badgeId),
                    startDate,
                    endDate)
        } else {
            userIds = nativeQueriesRepo.findUsersEligibleForBadge(projectId, badgeId, startDate, endDate)
        }

        return userIds
    }

    @Transactional
    public void saveBadgeAchievement(String userId, String projectId, String badgeId, Integer badgeRefId, String notified=Boolean.TRUE.toString()) {
        UserAchievement groupAchievement = new UserAchievement(userId: userId.toLowerCase(),
                projectId: projectId,
                skillId: badgeId,
                skillRefId: badgeRefId,
                notified: notified)
        achievedLevelRepo.save(groupAchievement)
    }


    @Transactional
    void deleteBadge(String projectId, String badgeId, SkillDef.ContainerType type = SkillDef.ContainerType.Badge) {
        log.debug("Deleting badge with project id [{}] and badge id [{}]", projectId, badgeId)
        SkillDef badgeDefinition = skillDefRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(projectId, badgeId, type)
        assert badgeDefinition, "DELETE FAILED -> no badge with project id [$projectId] and badge id [$badgeId]"
        assert badgeDefinition.type == type

        ruleSetDefGraphService.deleteSkillWithItsDescendants(badgeDefinition)

        // reset display order attribute - make sure the order is continuous - 0...N
        ProjDef projDef
        if (type == SkillDef.ContainerType.Badge) {
            projDef = projDefAccessor.getProjDef(projectId)
        }
        List<SkillDef> badges = getBadgesInternal(projDef, type)
        badges = badges?.findAll({ it.id != badgeDefinition.id }) // need to remove because of JPA level caching?
        displayOrderService.resetDisplayOrder(badges)
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
        return convertToBadge(skillDef, true)
    }

    @Transactional
    void setBadgeDisplayOrder(String projectId, String badgeId, ActionPatchRequest badgePatchRequest) {
        lockingService.lockProject(projectId)
        ProjDef projDef = projDefAccessor.getProjDef(projectId)
        displayOrderService.updateDisplayOrder(badgeId, projDef.badges, badgePatchRequest)
    }

    @Transactional()
    void addSkillToBadge(String projectId, String badgeId, String skillid) {
        ruleSetDefGraphService.assignGraphRelationship(projectId, badgeId, SkillDef.ContainerType.Badge, skillid, SkillRelDef.RelationshipType.BadgeRequirement)
    }

    @Transactional()
    void removeSkillFromBadge(String projectId, String badgeId, String skillid) {
        ruleSetDefGraphService.removeGraphRelationship(projectId, badgeId, SkillDef.ContainerType.Badge,
                projectId, skillid, SkillRelDef.RelationshipType.BadgeRequirement)
    }

    @Transactional(readOnly = true)
    boolean existsByBadgeName(String projectId, String subjectName) {
        return skillDefRepo.existsByProjectIdAndNameAndTypeAllIgnoreCase(projectId, subjectName, SkillDef.ContainerType.Badge)
    }

    @Profile
    private BadgeResult convertToBadge(SkillDefWithExtra skillDef, boolean loadRequiredSkills = false) {
        BadgeResult res = new BadgeResult(
                badgeId: skillDef.skillId,
                projectId: skillDef.projectId,
                name: skillDef.name,
                description: InputSanitizer.unsanitizeForMarkdown(skillDef.description),
                displayOrder: skillDef.displayOrder,
                iconClass: skillDef.iconClass,
                startDate: skillDef.startDate,
                endDate: skillDef.endDate,
                helpUrl: skillDef.helpUrl,
                enabled: skillDef.enabled,
        )

        if (loadRequiredSkills) {
            List<SkillDef> dependentSkills = skillDefRepo.findChildSkillsByIdAndRelationshipType(skillDef.id, SkillRelDef.RelationshipType.BadgeRequirement)
            res.requiredSkills = dependentSkills?.collect { skillsAdminService.convertToSkillDefRes(it) }
            res.numSkills = dependentSkills ? dependentSkills.size() : 0
            res.totalPoints = dependentSkills ? dependentSkills?.collect({ it.totalPoints })?.sum() : 0
        } else {
            res.numSkills = skillDefRepo.countChildSkillsByIdAndRelationshipType(skillDef.id, SkillRelDef.RelationshipType.BadgeRequirement)
            if (res.numSkills > 0) {
                res.totalPoints = skillDefRepo.sumChildSkillsTotalPointsBySkillAndRelationshipType(skillDef.id, SkillRelDef.RelationshipType.BadgeRequirement)
            } else {
                res.totalPoints = 0
            }
        }
        return res
    }

    private Integer getBadgeDisplayOrder(ProjDef projDef, SkillDef.ContainerType type) {
        Integer lastDisplayOrder = getBadgesInternal(projDef, type)?.collect({ it.displayOrder })?.max()
        int displayOrder = lastDisplayOrder != null ? lastDisplayOrder + 1 : 0
        return displayOrder
    }

    private List<SkillDef> getBadgesInternal(ProjDef projDef, SkillDef.ContainerType type) {
        List<SkillDef> badges
        if (type == SkillDef.ContainerType.GlobalBadge) {
            badges = skillDefRepo.findAllByProjectIdAndType(null, SkillDef.ContainerType.GlobalBadge)
        } else {
            badges  = projDef.badges
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
}
