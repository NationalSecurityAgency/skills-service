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
package skills.services.adminGroup

import callStack.profiler.Profile
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.PublicProps
import skills.auth.UserInfo
import skills.auth.UserInfoService
import skills.controller.PublicPropsBasedValidator
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.exceptions.SkillsValidator
import skills.controller.request.model.AdminGroupDefRequest
import skills.controller.result.model.AdminGroupDefResult
import skills.controller.result.model.AdminGroupGlobalBadgeResult
import skills.controller.result.model.AdminGroupProjectResult
import skills.controller.result.model.AdminGroupQuizResult
import skills.controller.result.model.EnableUserCommunityValidationRes
import skills.controller.result.model.GlobalBadgeResult
import skills.controller.result.model.ProjectResult
import skills.controller.result.model.QuizDefResult
import skills.services.*
import skills.services.admin.DataIntegrityExceptionHandlers
import skills.services.admin.ProjAdminService
import skills.services.admin.ServiceValidatorHelper
import skills.services.admin.UserCommunityService
import skills.services.quiz.QuizDefService
import skills.services.userActions.DashboardAction
import skills.services.userActions.DashboardItem
import skills.services.userActions.UserActionInfo
import skills.services.userActions.UserActionsHistoryService
import skills.storage.model.AdminGroupDef
import skills.storage.model.auth.RoleName
import skills.storage.repos.AdminGroupDefRepo

import skills.storage.repos.UserRoleRepo
import skills.utils.InputSanitizer
import skills.utils.Props

@Service
@Slf4j
class AdminGroupService {

    @Autowired
    AdminGroupRoleService adminGroupRoleService

    @Autowired
    UserInfoService userInfoService

    @Autowired
    UserCommunityService userCommunityService

    @Autowired
    ProjAdminService projAdminService

    @Autowired
    GlobalBadgesService globalBadgesService

    @Autowired
    QuizDefService quizDefService

    @Autowired
    AdminGroupDefRepo adminGroupDefRepo

    @Autowired
    UserRoleRepo userRoleRepository

    @Autowired
    LockingService lockingService

    @Autowired
    CustomValidator customValidator

    @Autowired
    ServiceValidatorHelper serviceValidatorHelper

    @Autowired
    AccessSettingsStorageService accessSettingsStorageService

    @Autowired
    PublicPropsBasedValidator propsBasedValidator

    @Autowired
    UserActionsHistoryService userActionsHistoryService

    @Transactional(readOnly = true)
    List<AdminGroupDefResult> getCurrentUsersAdminGroupDefs() {
        UserInfo userInfo = userInfoService.currentUser
        String userId = userInfo.username?.toLowerCase()
        List<AdminGroupDefResult> res = []

        List<AdminGroupDefRepo.AdminGroupDefSummaryRes> fromDb = adminGroupDefRepo.getAdminGroupDefSummariesByUser(userId)
        if (fromDb) {
            fromDb = fromDb.sort { a, b -> b.created <=> a.created }
            res.addAll(fromDb.collect { convert(it) })
        }
        return res
    }

    @Transactional(readOnly = true)
    AdminGroupDefResult getAdminGroupDef(String adminGroupId) {
        assert adminGroupId
        return convert(adminGroupDefRepo.getAdminGroupDefSummary(adminGroupId))
    }

    @Transactional(readOnly = true)
    List<AdminGroupDefResult> getAdminGroupsForProject(String projectId) {
        List<AdminGroupDefResult> res = []
        List<AdminGroupDefRepo.AdminGroupDefSummaryRes> fromDb = adminGroupDefRepo.getAdminGroupDefSummariesByProjectId(projectId)
        if (fromDb) {
            fromDb = fromDb.sort { a, b -> b.created <=> a.created }
            res.addAll(fromDb.collect { convert(it, true) })
        }
        return res
    }

    @Transactional(readOnly = true)
    List<AdminGroupDefResult> getAdminGroupsForQuiz(String quizId) {
        List<AdminGroupDefResult> res = []
        List<AdminGroupDefRepo.AdminGroupDefSummaryRes> fromDb = adminGroupDefRepo.getAdminGroupDefSummariesByQuizId(quizId)
        if (fromDb) {
            fromDb = fromDb.sort { a, b -> b.created <=> a.created }
            res.addAll(fromDb.collect { convert(it, true) })
        }
        return res
    }

    @Transactional(readOnly = true)
    List<AdminGroupDefResult> getAdminGroupsForGlobalBadge(String badgeId) {
        List<AdminGroupDefResult> res = []
        List<AdminGroupDefRepo.AdminGroupDefSummaryRes> fromDb = adminGroupDefRepo.getAdminGroupDefSummariesByGlobalBadgeId(badgeId)
        if (fromDb) {
            fromDb = fromDb.sort { a, b -> b.created <=> a.created }
            res.addAll(fromDb.collect { convert(it, true) })
        }
        return res
    }

    @Transactional()
    AdminGroupDefResult saveAdminGroupDef(Boolean isEdit, AdminGroupDefRequest adminGroupDefRequest, String userIdParam = null) {
        adminGroupDefRequest.adminGroupId = InputSanitizer.sanitize(adminGroupDefRequest.adminGroupId)
        adminGroupDefRequest.name = InputSanitizer.sanitize(adminGroupDefRequest.name)?.trim()
        validateAdminGroupDefRequest(adminGroupDefRequest)
        validateUserCommunityProps(adminGroupDefRequest)

        lockingService.lockAdminGroupDefs()

        AdminGroupDef adminGroupDef
        if (isEdit) {
            adminGroupDef = adminGroupDefRepo.findByAdminGroupIdIgnoreCase(adminGroupDefRequest.adminGroupId)
            adminGroupDef.name = adminGroupDefRequest.name
            adminGroupDef.protectedCommunityEnabled = adminGroupDefRequest.enableProtectedUserCommunity
            log.debug("Updated admin group [{}]", adminGroupDef)
        } else {
            serviceValidatorHelper.validateAdminGroupIdDoesNotExist(adminGroupDefRequest.adminGroupId)
            serviceValidatorHelper.validateAdminGroupNameDoesNotExist(adminGroupDefRequest.name, adminGroupDefRequest.adminGroupId)
            adminGroupDef = new AdminGroupDef(
                    adminGroupId: adminGroupDefRequest.adminGroupId,
                    name: adminGroupDefRequest.name,
                    protectedCommunityEnabled: adminGroupDefRequest.enableProtectedUserCommunity
            )

            String userId = userIdParam ?: userInfoService.getCurrentUserId()
            DataIntegrityExceptionHandlers.dataIntegrityViolationExceptionHandler.handle(null, null, adminGroupDef.adminGroupId) {
                adminGroupDef = adminGroupDefRepo.save(adminGroupDef)
            }
            accessSettingsStorageService.addAdminGroupDefUserRoleForUser(userId, adminGroupDef.adminGroupId, RoleName.ROLE_ADMIN_GROUP_OWNER)
            log.debug("Created new admin group [{}]", adminGroupDef)
        }
        log.debug("Saved [{}]", adminGroupDef)
        userActionsHistoryService.saveUserAction(new UserActionInfo(
            action: isEdit ? DashboardAction.Edit : DashboardAction.Create,
            item: DashboardItem.AdminGroup,
            actionAttributes: adminGroupDef,
            itemId: adminGroupDef.adminGroupId,
            itemRefId: adminGroupDef.id,
        ))

        return convert(adminGroupDefRepo.getAdminGroupDefSummary(adminGroupDef.adminGroupId))
    }

    @Profile
    private void validateUserCommunityProps(AdminGroupDefRequest adminGroupDefRequest) {
        String adminGroupId = adminGroupDefRequest.adminGroupId
        if (adminGroupDefRequest.enableProtectedUserCommunity != null) {
            if (adminGroupDefRequest.enableProtectedUserCommunity) {
                String userId = userInfoService.currentUserId
                if (!userCommunityService.isUserCommunityMember(userId)) {
                    throw new SkillException("User [${userId}] is not allowed to set [enableProtectedUserCommunity] to true adminGroupId [${adminGroupId}]", null, null, ErrorCode.AccessDenied)
                }

                EnableUserCommunityValidationRes enableProjValidationRes = userCommunityService.validateAdminGroupForCommunity(adminGroupId)
                if (!enableProjValidationRes.isAllowed) {
                    String reasons = enableProjValidationRes.unmetRequirements.join("\n")
                    throw new SkillException("Not Allowed to set [enableProtectedUserCommunity] to true for adminGroupId [${adminGroupId}]. Reasons are:\n${reasons}", null, null, ErrorCode.AccessDenied)
                }
            } else {
                SkillsValidator.isTrue(!userCommunityService.isUserCommunityOnlyAdminGroup(adminGroupId), "Once admin group [enableProtectedUserCommunity=true] it cannot be flipped to false.  adminGroupId [${adminGroupId}]")
            }
        }
    }

    @Transactional()
    void deleteAdminGroup(String adminGroupId) {
        AdminGroupDef adminGroupDef = findAdminGroupDef(adminGroupId)
        userRoleRepository.findProjectIdsByAdminGroupId(adminGroupId).each { projectId ->
            adminGroupRoleService.removeProjectFromAdminGroup(adminGroupId, projectId)
        }
        userRoleRepository.findQuizIdsByAdminGroupId(adminGroupId).each { quizId ->
            adminGroupRoleService.removeQuizFromAdminGroup(adminGroupId, quizId)
        }
        adminGroupDefRepo.deleteByAdminGroupIdIgnoreCase(adminGroupDef.adminGroupId)
        log.debug("Deleted admin group with id [{}].", adminGroupId)

        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: DashboardAction.Delete,
                item: DashboardItem.AdminGroup,
                itemId: adminGroupDef.adminGroupId,
        ))
    }

    @Transactional(readOnly = true)
    AdminGroupProjectResult getAdminGroupProjects(String adminGroupId) {
        List<ProjectResult> projects = projAdminService.getProjects()
        List<String> assignedProjectIds = userRoleRepository.findProjectIdsByAdminGroupId(adminGroupId)
        return new AdminGroupProjectResult(
                adminGroupId: adminGroupId,
                availableProjects: projects.findAll { it.projectId !in assignedProjectIds },
                assignedProjects: projects.findAll { it.projectId in assignedProjectIds },)
    }

    @Transactional(readOnly = true)
    AdminGroupQuizResult getAdminGroupQuizzesAndSurveys(String adminGroupId) {
        List<QuizDefResult> userQuizzes = quizDefService.getCurrentUsersQuizDefs()
        List<String> assignedQuizIds = userRoleRepository.findQuizIdsByAdminGroupId(adminGroupId)
        return new AdminGroupQuizResult(adminGroupId: adminGroupId,
                availableQuizzes: userQuizzes.findAll { QuizDefResult it -> (it.quizId !in assignedQuizIds) },
                assignedQuizzes: userQuizzes.findAll { QuizDefResult it -> (it.quizId in assignedQuizIds) }
        )
    }

    @Transactional(readOnly = true)
    AdminGroupGlobalBadgeResult getAdminGroupGlobalBadges(String adminGroupId) {
        List<GlobalBadgeResult> globalBadges = globalBadgesService.getBadgesForUser()

        List<String> assignedBadgeIds = userRoleRepository.findGlobalBadgeIdsByAdminGroupId(adminGroupId)
        return new AdminGroupGlobalBadgeResult(
                adminGroupId: adminGroupId,
                availableGlobalBadges: globalBadges.findAll { it.badgeId !in assignedBadgeIds },
                assignedGlobalBadges: globalBadges.findAll { it.badgeId in assignedBadgeIds },)
    }

    @Transactional(readOnly = true)
    boolean existsByAdminGroupId(String adminGroupId) {
        return adminGroupDefRepo.existsByAdminGroupIdIgnoreCase(adminGroupId)
    }

    @Transactional(readOnly = true)
    boolean existsByAdminGroupName(String adminGroupName) {
        return adminGroupDefRepo.existsByNameIgnoreCase(adminGroupName)
    }

    @Transactional(readOnly = true)
    EnableUserCommunityValidationRes validateAdminGroupForEnablingCommunity(String adminGroupId) {
        return userCommunityService.validateAdminGroupForCommunity(adminGroupId)
    }

    private AdminGroupDef findAdminGroupDef(String adminGroupId) {
        AdminGroupDef adminGroupDef = adminGroupDefRepo.findByAdminGroupIdIgnoreCase(adminGroupId)
        if (!adminGroupDef) {
            throw new SkillException("Failed to find admin group with id [${adminGroupId}].", ErrorCode.BadParam)
        }
        return adminGroupDef
    }

    private void validateAdminGroupDefRequest(AdminGroupDefRequest adminGroupDefRequest) {
        String adminGroupId = adminGroupDefRequest.adminGroupId
        IdFormatValidator.validate(adminGroupId)
        propsBasedValidator.validateMaxStrLength(PublicProps.UiProp.maxIdLength, "adminGroupId", adminGroupId)
        propsBasedValidator.validateMinStrLength(PublicProps.UiProp.minIdLength, "adminGroupId", adminGroupId)

        propsBasedValidator.validateMaxStrLength(PublicProps.UiProp.maxQuizNameLength, "Admin Group Name", adminGroupDefRequest.name)
        propsBasedValidator.validateMinStrLength(PublicProps.UiProp.minNameLength, "Admin Group Name", adminGroupDefRequest.name)


        if (!adminGroupDefRequest?.name) {
            throw new SkillException("Admin Group name was not provided.", ErrorCode.BadParam)
        }

        CustomValidationResult customValidationResult = customValidator.validate(adminGroupDefRequest)
        if (!customValidationResult.valid) {
            throw new SkillException(customValidationResult.msg, ErrorCode.BadParam)
        }
    }

    private AdminGroupDefResult convert(AdminGroupDefRepo.AdminGroupDefSummaryRes adminGroupDefSummaryResult, Boolean includedMemberRoles = false) {
        AdminGroupDefResult result = Props.copy(adminGroupDefSummaryResult, new AdminGroupDefResult())
        result.userCommunity = userCommunityService.getCommunityNameBasedOnConfAndItemStatus(adminGroupDefSummaryResult.getProtectedCommunityEnabled())
        if (includedMemberRoles) {
            result.allMembers = adminGroupRoleService.getAdminGroupMemberUserRoles(adminGroupDefSummaryResult.adminGroupId)
        }
        return result
    }
}
