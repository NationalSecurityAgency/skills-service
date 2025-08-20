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
package skills.services

import callStack.profiler.Profile
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.auth.UserInfo
import skills.auth.UserInfoService
import skills.auth.UserSkillsGrantedAuthority
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.exceptions.SkillsValidator
import skills.controller.request.model.ActionPatchRequest
import skills.controller.request.model.BadgeRequest
import skills.controller.result.model.GlobalBadgeLevelRes
import skills.controller.result.model.GlobalBadgeResult
import skills.controller.result.model.ProjectResult
import skills.controller.result.model.SkillDefPartialRes
import skills.services.admin.*
import skills.services.admin.skillReuse.SkillReuseIdUtil
import skills.services.inception.InceptionProjectService
import skills.services.settings.SettingsService
import skills.services.userActions.DashboardAction
import skills.services.userActions.DashboardItem
import skills.services.userActions.UserActionInfo
import skills.services.userActions.UserActionsHistoryService
import skills.storage.accessors.SkillDefAccessor
import skills.storage.model.*
import skills.storage.model.SkillRelDef.RelationshipType
import skills.storage.model.auth.RoleName
import skills.storage.repos.*
import skills.utils.InputSanitizer

import static skills.storage.model.SkillDef.ContainerType

@Service
@Slf4j
class GlobalBadgesService {

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    SkillDefWithExtraRepo skillDefWithExtraRepo

    @Autowired
    SkillRelDefRepo skillRelDefRepo

    @Autowired
    LevelDefinitionStorageService levelDefService

    @Autowired
    LevelDefRepo levelDefinitionRepository

    @Autowired
    RuleSetDefinitionScoreUpdater ruleSetDefinitionScoreUpdater

    @Autowired
    UserAchievementsAndPointsManagement userPointsManagement

    @Autowired
    RuleSetDefGraphService ruleSetDefGraphService

    @Autowired
    SkillShareDefRepo skillShareDefRepo

    @Autowired
    GlobalBadgeLevelDefRepo globalBadgeLevelDefRepo

    @Autowired
    SettingsService settingsService

    @Autowired
    ProjectSortingService sortingService

    @Autowired
    ProjDefRepo projDefRepo

    @Autowired
    BadgeAdminService badgeAdminService

    @Autowired
    LockingService lockingService

    @Autowired
    DisplayOrderService displayOrderService

    @Autowired
    SkillsAdminService skillsAdminService

    @Autowired
    SkillsDepsService skillsDepsService

    @Autowired
    UserAchievedLevelRepo achievedLevelRepo

    @Autowired
    SkillDefAccessor skillDefAccessor

    @Autowired
    UserCommunityService userCommunityService

    @Autowired
    UserActionsHistoryService userActionsHistoryService

    @Autowired
    ProjAdminService projAdminService

    @Autowired
    UserInfoService userInfoService

    @Autowired
    UserRoleRepo userRoleRepo

    @Autowired
    InviteOnlyProjectService inviteOnlyProjectService

    @Transactional()
    void saveBadge(String originalBadgeId, BadgeRequest badgeRequest) {
        badgeAdminService.saveBadge(null, originalBadgeId, badgeRequest, ContainerType.GlobalBadge)
    }
    @Transactional(readOnly = true)
    boolean existsByBadgeName(String subjectName) {
        return skillDefRepo.existsByProjectIdAndNameAndTypeAllIgnoreCase(null, subjectName, ContainerType.GlobalBadge)
    }

    @Transactional(readOnly = true)
    boolean existsByBadgeId(String skillId) {
        return skillDefRepo.existsByProjectIdAndSkillIdAllIgnoreCase(null, skillId)
    }

    @Transactional()
    void addSkillToBadge(String badgeId, String projectId, String skillId) {
        validateUserIsAndAdminOfProj(projectId)
        SkillDef skillDef = skillDefAccessor.getSkillDef(projectId, skillId)
        SkillsValidator.isTrue(!skillId.toUpperCase().contains(SkillReuseIdUtil.REUSE_TAG.toUpperCase()), "Skill ID must not contain reuse tag", projectId, skillId)
        SkillsValidator.isTrue(!skillDef.readOnly, "Imported Skills may not be added as Global Badge Dependencies", projectId, skillId)

        if (userCommunityService.isUserCommunityOnlyProject(projectId)) {
            throw new SkillException("Projects with the community protection are not allowed to be added to a Global Badge", projectId, skillId, ErrorCode.AccessDenied)
        }

        if (inviteOnlyProjectService.isInviteOnlyProject(projectId)) {
            throw new SkillException("Projects with the private invitation only setting are not allowed to be added to a Global Badge", projectId, skillId, ErrorCode.AccessDenied)
        }

        assignGraphRelationship(badgeId, ContainerType.GlobalBadge, projectId, skillId, RelationshipType.BadgeRequirement)

        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: DashboardAction.AssignSkill,
                item: DashboardItem.GlobalBadge,
                actionAttributes: [
                        badgeId: badgeId,
                        projectId: projectId,
                        skillId: skillId,
                ],
                itemId: badgeId,
        ))
    }

    @Transactional()
    void addProjectLevelToBadge(String badgeId, String projectId, Integer level) {
        validateUserIsAndAdminOfProj(projectId)
        SkillDefWithExtra badgeSkillDef = skillDefWithExtraRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(null, badgeId, ContainerType.GlobalBadge)
        if (!badgeSkillDef) {
            throw new SkillException("Failed to find global badge [${badgeId}]")
        }
        ProjDef projDef = projDefRepo.findByProjectId(projectId)
        if (!projDef) {
            throw new SkillException("Failed to find project [${projectId}]", projectId)
        }
        if (userCommunityService.isUserCommunityOnlyProject(projectId)) {
            throw new SkillException("Projects with the community protection are not allowed to be added to a Global Badge", projectId, null, ErrorCode.AccessDenied)
        }
        if (inviteOnlyProjectService.isInviteOnlyProject(projectId)) {
            throw new SkillException("Projects with the private invitation only setting are not allowed to be added to a Global Badge", projectId, null, ErrorCode.AccessDenied)
        }

        List<LevelDef> projectLevels = levelDefinitionRepository.findAllByProjectRefId(projDef.id)
        projectLevels.sort({it.level})

        LevelDef toAdd = projectLevels.find { it.level == level }
        if (!toAdd) {
            throw new SkillException("Failed to find level [${level}]", projectId)
        }

        GlobalBadgeLevelDef globalBadgeLevelDef = new GlobalBadgeLevelDef(
                levelRefId: toAdd.id, level: level, projectRefId: projDef.id, projectId: projectId,
                projectName: projDef.name, badgeRefId: badgeSkillDef.id, badgeId: badgeId
        )

        DataIntegrityExceptionHandlers.dataIntegrityViolationExceptionHandler.handle(null) {
            globalBadgeLevelDefRepo.save(globalBadgeLevelDef)
        }

        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: DashboardAction.AssignLevel,
                item: DashboardItem.GlobalBadge,
                actionAttributes: [
                        badgeId: badgeId,
                        projectId: projectId,
                        level: level,
                ],
                itemId: badgeId,
        ))
    }

    @Transactional()
    void changeProjectLevelOnBadge(String badgeId, String projectId, Integer existingLevel, Integer newLevel) {
        SkillDefWithExtra badgeSkillDef = skillDefWithExtraRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(null, badgeId, ContainerType.GlobalBadge)
        if (!badgeSkillDef) {
            throw new SkillException("Failed to find global badge [${badgeId}]")
        }
        ProjDef projDef = projDefRepo.findByProjectId(projectId)
        if (!projDef) {
            throw new SkillException("Failed to find project [${projectId}]", projectId)
        }
        List<LevelDef> projectLevels = levelDefinitionRepository.findAllByProjectRefId(projDef.id)
        projectLevels.sort({it.level})

        LevelDef toAdd = projectLevels.find { it.level == newLevel }
        if (!toAdd) {
            throw new SkillException("Failed to find level [${newLevel}]", projectId)
        }

        GlobalBadgeLevelDef existing = globalBadgeLevelDefRepo.findByBadgeIdAndProjectIdAndLevel(badgeId, projectId, existingLevel)
        log.debug("changing project level on global badge [${badgeId}] from [${projectId}-${existingLevel}] to [${projectId}-${newLevel}]")
        existing.level = toAdd.level

        DataIntegrityExceptionHandlers.dataIntegrityViolationExceptionHandler.handle(null) {
            globalBadgeLevelDefRepo.save(existing)
            badgeAdminService.awardBadgeToUsersMeetingRequirements(badgeSkillDef)
        }

        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: DashboardAction.AssignLevel,
                item: DashboardItem.GlobalBadge,
                actionAttributes: [
                        badgeId: badgeId,
                        projectId: projectId,
                        level: newLevel,
                        previousLevel: existingLevel,
                ],
                itemId: badgeId,
        ))
    }


    @Transactional()
    void removeProjectLevelFromBadge(String badgeId, projectId, Integer level) {
        GlobalBadgeLevelDef globalBadgeLevelDef = globalBadgeLevelDefRepo.findByBadgeIdAndProjectIdAndLevel(badgeId, projectId, level)
        if (!globalBadgeLevelDef) {
            throw new SkillException("Failed to find global badge project level for badge [${badgeId}], project [${projectId}] and level [${level}]", projectId, badgeId)
        }

        globalBadgeLevelDefRepo.delete(globalBadgeLevelDef)

        SkillDef badgeSkillDef = skillDefRepo.findGlobalBadgeByBadgeId(badgeId)
        badgeAdminService.awardBadgeToUsersMeetingRequirements(badgeSkillDef)

        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: DashboardAction.RemoveLevelAssignment,
                item: DashboardItem.GlobalBadge,
                actionAttributes: [
                        badgeId: badgeId,
                        projectId: projectId,
                        level: level,
                ],
                itemId: badgeId,
        ))
    }

    @Transactional(readOnly = true)
    List<GlobalBadgeLevelRes> getGlobalBadgeLevels(String badgeId) {
        List<GlobalBadgeLevelDef> globalBadgeLevelDefs = globalBadgeLevelDefRepo.findAllByBadgeId(badgeId)
        return globalBadgeLevelDefs.collect { new GlobalBadgeLevelRes(
                badgeId: it.badgeId,
                projectId: it.projectId,
                projectName: InputSanitizer.unsanitizeName(it.projectName),
                level: it.level
        ) }
    }

    @Transactional()
    void removeSkillFromBadge(String badgeId, String projectId, String skillId) {
        removeGraphRelationship(badgeId, ContainerType.GlobalBadge, projectId, skillId, RelationshipType.BadgeRequirement)

        SkillDef badgeSkillDef = skillDefRepo.findGlobalBadgeByBadgeId(badgeId)
        badgeAdminService.awardBadgeToUsersMeetingRequirements(badgeSkillDef)

        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: DashboardAction.RemoveSkillAssignment,
                item: DashboardItem.GlobalBadge,
                actionAttributes: [
                        badgeId: badgeId,
                        projectId: projectId,
                        skillId: skillId,
                ],
                itemId: badgeId,
        ))
    }

    @Transactional
    void assignGraphRelationship(String badgeSkillId, ContainerType skillType, String projectId,
                                 String relationshipSkillId, RelationshipType relationshipType) {
        ruleSetDefGraphService.assignGraphRelationship(null, badgeSkillId, skillType, projectId, relationshipSkillId, relationshipType)
    }

    @Transactional
    void removeGraphRelationship(String skillId, ContainerType skillType, String projectId,
                                 String relationshipSkillId, RelationshipType relationshipType){
        ruleSetDefGraphService.removeGraphRelationship(null, skillId, skillType, projectId, relationshipSkillId, relationshipType)
    }

    @Transactional
    void deleteBadge(String badgeId) {
        badgeAdminService.deleteBadge(null, badgeId, ContainerType.GlobalBadge)
    }

    @Transactional(readOnly = true)
    List<GlobalBadgeResult> getBadgesForUser() {
        UserInfo userInfo = userInfoService.currentUser
        String userId = userInfo.username?.toLowerCase()
        List<SkillDefWithExtra> badges = skillDefWithExtraRepo.findGlobalBadgesForAdmin(userId)
        List<GlobalBadgeResult> res = badges.collect { convertToBadge(it, true) }
        return res?.sort({ it.displayOrder })
    }

    @Transactional(readOnly = true)
    GlobalBadgeResult getBadge(String badgeId) {
        SkillDefWithExtra skillDef = skillDefWithExtraRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(null, badgeId, ContainerType.GlobalBadge)
        if (skillDef) {
            return convertToBadge(skillDef, true)
        }
    }

    @Transactional
    void setBadgeDisplayOrder(String badgeId, ActionPatchRequest badgePatchRequest) {
        lockingService.lockGlobalBadges()
        List<SkillDef> badges = skillDefRepo.findAllByProjectIdAndType(null,  ContainerType.GlobalBadge)
        if(ActionPatchRequest.ActionType.NewDisplayOrderIndex == badgePatchRequest.action) {
            displayOrderService.updateDisplayOrderByUsingNewIndex(badgeId, badges, badgePatchRequest)
        }
    }

    @Transactional(readOnly = true)
    AvailableSkillsResult getAvailableSkillsForGlobalBadge(String badgeId, String query) {
        List<String> projectIds = projAdminService.getProjects()?.collect { it.projectId }
        List<SkillDefPartial> allSkillDefs = skillDefRepo.findAllByTypeAndNameLikeNoImportedUCOrInviteOnlySkills(ContainerType.Skill, query, projectIds)
        Set<String> existingBadgeSkillIds = getSkillsForBadge(badgeId).collect { "${it.projectId}${it.skillId}" }
        List<SkillDefPartial> suggestedSkillDefs = allSkillDefs.findAll { !("${it.projectId}${it.skillId}" in existingBadgeSkillIds) &&  it.projectId != InceptionProjectService.inceptionProjectId }
        AvailableSkillsResult res = new AvailableSkillsResult()
        if (suggestedSkillDefs) {
            res.totalAvailable = suggestedSkillDefs.size()
            res.suggestedSkills = suggestedSkillDefs.sort().take(10).collect { skillsAdminService.convertToSkillDefPartialRes(it) }
        }
        return res
    }

    @Transactional(readOnly = true)
    List<SkillDefPartialRes> getSkillsForBadge(String badgeId) {
        return skillsAdminService.getSkillsByProjectSkillAndType(null, badgeId, ContainerType.GlobalBadge, RelationshipType.BadgeRequirement)
    }


    @Transactional(readOnly = true)
    AvailableProjectResult getAvailableProjectsForBadge(String badgeId, String query) {
        List<String> projectIds = projAdminService.getProjects()?.collect { it.projectId }?.findAll { !inviteOnlyProjectService.isInviteOnlyProject(it) } ?: []
        List<String> notThese = globalBadgeLevelDefRepo.findAllByBadgeId(badgeId)?.collect { it.projectId }?.unique() ?: []

        notThese << InceptionProjectService.inceptionProjectId
        projectIds = projectIds - notThese

        AvailableProjectResult available = new AvailableProjectResult()
        int count = projDefRepo.countAllByNameLikeAndProjectIdIn(query, projectIds)
        if (count > 0) {
            Integer pageNo = 0;
            Integer pageSize = 10;
            String sortBy = "name";
            Pageable paging = PageRequest.of(pageNo, pageSize, Sort.by(sortBy).ascending());
            def byNameLike = projDefRepo.findAllByNameLikeAndProjectIdIn(query, projectIds, paging)

            def converted = byNameLike.collect { ProjDef definition ->
                new ProjectResult(
                        projectId: definition.projectId, name: InputSanitizer.unsanitizeName(definition.name), totalPoints: definition.totalPoints,
                        displayOrder: 0,
                )
            }
            available.totalAvailable = count
            available.projects = converted
        }
        return available
    }

    @Transactional(readOnly = true)
    List<Integer> globalBadgesSkillIsUsedIn(String projectId, String skillId) {
        SkillDef skillDef = skillDefRepo.findByProjectIdAndSkillIdAndTypeIn(projectId, skillId, [ContainerType.Skill, ContainerType.SkillsGroup])
        List<Integer> globalBadgeIds = skillRelDefRepo.getGlobalBadgeIdsForSkill(skillDef.id)
        globalBadgeIds.addAll(skillRelDefRepo.getGlobalBadgeLevelIdsForSkill(projectId))
        return globalBadgeIds
    }

    @Transactional(readOnly = true)
    boolean isSkillUsedInGlobalBadge(String projectId, String skillId) {
        SkillDef skillDef = skillDefRepo.findByProjectIdAndSkillIdAndTypeIn(projectId, skillId, [ContainerType.Skill, ContainerType.SkillsGroup])
        assert skillDef, "Skill [${skillId}] for project [${projectId}] does not exist"
        return isSkillUsedInGlobalBadge(skillDef)
    }

    @Transactional(readOnly = true)
    boolean isSkillUsedInGlobalBadge(SkillDef skillDef) {
        int numProjectSkillsUsedInGlobalBadge = skillRelDefRepo.getSkillUsedInGlobalBadgeCount(skillDef.id)
        return numProjectSkillsUsedInGlobalBadge > 0
    }

    @Transactional(readOnly = true)
    boolean isSubjectUsedInGlobalBadge(String projectId, String skillId) {
        SkillDef subjectSkillDef = skillDefRepo.findByProjectIdAndSkillIdAndType(projectId, skillId, ContainerType.Subject)
        assert subjectSkillDef, "Skill [${skillId}] for project [${projectId}] does not exist"
        return isSubjectUsedInGlobalBadge(subjectSkillDef)
    }

    @Transactional(readOnly = true)
    boolean isSubjectUsedInGlobalBadge(SkillDef skillDef) {
        int numProjectSkillsUsedInGlobalBadge = skillRelDefRepo.getSkillsFromSubjectUsedInGlobalBadgeCount(skillDef.skillId)
        return numProjectSkillsUsedInGlobalBadge > 0
    }

    @Transactional(readOnly = true)
    boolean isProjectLevelUsedInGlobalBadge(String projectId, Integer level) {
        int numberOfLevels = globalBadgeLevelDefRepo.countByProjectIdAndLevel(projectId, level)
        return numberOfLevels > 0
    }

    @Transactional(readOnly = true)
    boolean isProjectUsedInGlobalBadge(String projectId) {
        int numberOfLevels = globalBadgeLevelDefRepo.countByProjectId(projectId)
        if (numberOfLevels > 0) {
            return true
        }
        int numProjectSkillsUsedInGlobalBadge = skillRelDefRepo.getProjectUsedInGlobalBadgeCount(projectId)
        return numProjectSkillsUsedInGlobalBadge > 0
    }

    @Profile
    private GlobalBadgeResult convertToBadge(SkillDefWithExtra skillDef, boolean loadRequiredSkills = false) {
        GlobalBadgeResult res = new GlobalBadgeResult(
                badgeId: skillDef.skillId,
                name: InputSanitizer.unsanitizeName(skillDef.name),
                description: skillDef.description,
                displayOrder: skillDef.displayOrder,
                iconClass: skillDef.iconClass,
                startDate: skillDef.startDate,
                endDate: skillDef.endDate,
                helpUrl: skillDef.helpUrl,
                enabled: skillDef.enabled
        )

        if (loadRequiredSkills) {
            Set<String> uniqueProjectIds = []
            List<SkillDef> dependentSkills = skillDefRepo.findChildSkillsByIdAndRelationshipType(skillDef.id, SkillRelDef.RelationshipType.BadgeRequirement)
            uniqueProjectIds.addAll(dependentSkills*.projectId)
            res.requiredSkills = dependentSkills?.collect { skillsDepsService.convertToSkillDefRes(it) }
            res.numSkills = dependentSkills ? dependentSkills.size() : 0
            res.totalPoints = dependentSkills ? dependentSkills?.collect({ it.totalPoints })?.sum() : 0
            res.requiredProjectLevels = getGlobalBadgeLevels(skillDef.skillId)
            uniqueProjectIds.addAll(res.requiredProjectLevels*.projectId)
            res.uniqueProjectCount = uniqueProjectIds.size()
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

    @Profile
    private void validateUserIsAndAdminOfProj(String projectId) {
        UserInfo userInfo = userInfoService.currentUser
        boolean isRoot = userInfo.authorities?.find() {
            it instanceof UserSkillsGrantedAuthority && RoleName.ROLE_SUPER_DUPER_USER == it.role?.roleName
        }
        if (!isRoot) {
            Boolean isAdminForOtherProject = userRoleRepo.isUserProjectAdmin(userInfo.username, projectId)
            if (!isAdminForOtherProject) {
                throw new AccessDeniedException("User [${userInfo.username}] is not an admin for project [${projectId}]")
            }
        }
    }

    static class AvailableSkillsResult {
        int totalAvailable = 0
        List<SkillDefPartialRes> suggestedSkills = []
    }

    static class AvailableProjectResult {
        int totalAvailable = 0
        List<ProjectResult> projects = []
    }
}
