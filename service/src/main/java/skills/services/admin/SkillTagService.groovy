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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.request.model.ActionPatchRequest
import skills.controller.request.model.SkillsTagRequest
import skills.controller.result.model.BadgeResult
import skills.services.*
import skills.storage.accessors.ProjDefAccessor
import skills.storage.model.*
import skills.storage.repos.*
import skills.storage.repos.nativeSql.NativeQueriesRepo
import skills.utils.InputSanitizer

@Service
@Slf4j
class SkillTagService {

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
    void addTag(String projectId, SkillsTagRequest skillsTagRequest) {
        lockingService.lockProject(projectId)
        SkillDef.ContainerType type = SkillDef.ContainerType.Tag
        SkillDefWithExtra skillDefinition = skillDefWithExtraRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(projectId, skillsTagRequest.tagId, type)

        if (!skillDefinition) {
            ProjDef projDef = projDefAccessor.getProjDef(projectId)
            skillDefinition = new SkillDefWithExtra(
                    type: type,
                    projectId: projectId,
                    skillId: skillsTagRequest.tagId,
                    name: skillsTagRequest.tagValue,
                    projRefId: projDef.id,
                    displayOrder: 0,
                    enabled: Boolean.TRUE.toString()
            )
            log.debug("Saving [{}]", skillDefinition)
            SkillDefWithExtra savedSkill
            DataIntegrityExceptionHandlers.tagDataIntegrityViolationExceptionHandler.handle(projectId) {
                savedSkill = skillDefWithExtraRepo.save(skillDefinition)
            }
            log.debug("Saved [{}]", savedSkill)
        }

        List<String> existingTaggedSkillIds = getSkillsForTag(projectId, skillsTagRequest.tagId)?.collect { it.skillId }
        skillsTagRequest.skillIds.each { skillId ->
            if (!existingTaggedSkillIds.find { it == skillId}) {
                ruleSetDefGraphService.assignGraphRelationship(projectId, skillDefinition.skillId, SkillDef.ContainerType.Tag, skillId, SkillRelDef.RelationshipType.Tag)
            }
        }
        log.debug("Added [{}] tag to skills [{}]", skillsTagRequest.tagValue, skillsTagRequest.skillIds)
    }

    @Transactional
    List<SkillTag> getTagsForProject(String projectId) {
        skillDefRepo.getTagsForProject(projectId)
    }

    @Transactional
    List<SkillTag> getTagsForSkill(Integer skillRefId) {
        skillDefRepo.getTagsForSkill(skillRefId)
    }

    @Transactional
    List<SkillDefPartial> getSkillsForTag(String projectId, String tagId) {
        return skillRelDefRepo.getChildrenPartial(projectId, tagId, SkillRelDef.RelationshipType.Tag)
    }

    @Transactional
    List<SkillTag> getTagsForSkills(String projectId, List<String> skillIds) {
        skillDefRepo.getTagsForSkills(projectId, skillIds)
    }

    @Transactional
    void deleteTagForSkills(String projectId, SkillsTagRequest skillsTagRequest) {
//        skillDefRepo.deleteTagsForSkills(projectId, skillsTagRequest.tagId, skillsTagRequest.skillIds)
        //validate that removing this skill would not result in an empty badge
        String tagId = skillsTagRequest.tagId
        SkillDefWithExtra tag = skillDefWithExtraRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(projectId, tagId, SkillDef.ContainerType.Tag)

        if (!tag) {
            throw new SkillException("Tag [${tagId}] does not exist", projectId, tagId, ErrorCode.TagNotFound)
        }

        skillsTagRequest.skillIds.each {skillId ->
            ruleSetDefGraphService.removeGraphRelationship(projectId, tagId, SkillDef.ContainerType.Tag,
                    projectId, skillId, SkillRelDef.RelationshipType.Tag)
        }

        Integer skillsWithTag = skillRelDefRepo.getSkillWithTagCount(tagId)
        if (skillsWithTag <= 0) {
            log.info("Tag [${tagId}] removed from all skills, removing tag definition")
            skillDefWithExtraRepo.delete(tag)
        }
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
    void addSkillToBadge(String projectId, String badgeId, String skillid) {
        ruleSetDefGraphService.assignGraphRelationship(projectId, badgeId, SkillDef.ContainerType.Badge, skillid, SkillRelDef.RelationshipType.BadgeRequirement, true)
    }

    @Transactional()
    void removeSkillFromBadge(String projectId, String badgeId, String skillid) {
        //validate that removing this skill would not result in an empty badge
        SkillDefWithExtra badge = skillDefWithExtraRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(projectId, badgeId, SkillDef.ContainerType.Badge)

        if (!badge) {
            throw new SkillException("Badge [${badgeId}] does not exist", projectId, badgeId, ErrorCode.BadgeNotFound)
        }

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
                name: InputSanitizer.unsanitizeName(skillDef.name),
                description: InputSanitizer.unsanitizeForMarkdown(skillDef.description),
                displayOrder: skillDef.displayOrder,
                iconClass: skillDef.iconClass,
                startDate: skillDef.startDate,
                endDate: skillDef.endDate,
                helpUrl: InputSanitizer.unsanitizeUrl(skillDef.helpUrl),
                enabled: skillDef.enabled,
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
