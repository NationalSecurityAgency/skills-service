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

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.auth.UserInfoService
import skills.auth.UserNameService
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.result.model.GlobalBadgeResult
import skills.controller.result.model.UserRoleRes
import skills.services.admin.UserCommunityService
import skills.services.userActions.DashboardAction
import skills.services.userActions.DashboardItem
import skills.services.userActions.UserActionInfo
import skills.services.userActions.UserActionsHistoryService
import skills.storage.model.UserAttrs
import skills.storage.model.auth.RoleName
import skills.storage.repos.UserAttrsRepo
import skills.storage.repos.UserRoleRepo

@Service
@Slf4j
class GlobalBadgeRoleService {
    private static Set<RoleName> globalBadgeSupportedRoles = [RoleName.ROLE_GLOBAL_BADGE_ADMIN].toSet()

    @Autowired
    AccessSettingsStorageService accessSettingsStorageService

    @Autowired
    UserInfoService userInfoService

    @Autowired
    GlobalBadgesService globalBadgesService

    @Autowired
    UserNameService userNameService

    @Autowired
    UserRoleRepo userRoleRepo

    @Autowired
    UserActionsHistoryService userActionsHistoryService

    @Autowired
    UserAttrsRepo userAttrsRepo

    @Autowired
    UserCommunityService userCommunityService

    @Transactional(readOnly = true)
    List<UserRoleRes> getGlobalBadgeAdminUserRoles(String badgeId) {
        GlobalBadgeResult badgeResult = findGlobalBadgeResult(badgeId)
        return accessSettingsStorageService.findAllGlobalBadgeAdminRoles(badgeResult.badgeId)
    }
    
    @Transactional
    void addGlobalBadgeAdminRole(String userIdParam, String badgeId, RoleName roleName, String adminGroupId = null) {
        GlobalBadgeResult badgeResult = findGlobalBadgeResult(badgeId)
        ensureValidRole(roleName, badgeResult.badgeId)
        String userId = userNameService.normalizeUserId(userIdParam)
        String currentUser = userInfoService.getCurrentUserId()
        if (currentUser?.toLowerCase() == userId?.toLowerCase()) {
            throw new SkillException("Cannot add roles to myself. userId=[${userId}]", ErrorCode.AccessDenied)
        }
        Boolean addingAsLocalAdmin = adminGroupId == null && roleName == RoleName.ROLE_GLOBAL_BADGE_ADMIN;
        if (addingAsLocalAdmin && userRoleRepo.isUserGlobalBadgeAdmin(userId, badgeId)) {
            throw new SkillException("User is already part of an Admin Group and cannot be added as a local admin. userId=[${userId}]", ErrorCode.AccessDenied)
        }
        accessSettingsStorageService.addGlobalBadgeAdminUserRoleForUser(userId, badgeResult.badgeId, RoleName.ROLE_GLOBAL_BADGE_ADMIN, adminGroupId)

        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(userId)
        String userIdForDisplay = userAttrs?.userIdForDisplay ?: userId
        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: DashboardAction.Create, item: DashboardItem.UserRole,
                itemId: userIdForDisplay,
                actionAttributes: [
                        userRole: roleName,
                        badgeName: badgeResult.name,
                        badgeId: badgeResult.badgeId,
                ]
        ))
    }

    @Transactional
    void deleteGlobalBadgeAdminRole(String userIdParam, String badgeId, RoleName roleName) {
        GlobalBadgeResult badgeResult = findGlobalBadgeResult(badgeId)
        ensureValidRole(roleName, badgeResult.badgeId)
        String userId = userIdParam?.toLowerCase()
        String currentUser = userInfoService.getCurrentUserId()
        if (currentUser?.toLowerCase() == userId?.toLowerCase()) {
            throw new SkillException("Cannot remove roles from yourself. userId=[${userId}], globalBadgeName=[${badgeResult.name}]", ErrorCode.AccessDenied)
        }
        accessSettingsStorageService.deleteGlobalBadgeAdminUserRole(userId, badgeResult.badgeId, roleName)

        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(userId)
        String userIdForDisplay = userAttrs?.userIdForDisplay ?: userId
        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: DashboardAction.Delete, item: DashboardItem.UserRole,
                itemId: userIdForDisplay,
                actionAttributes: [
                        userRole: roleName,
                        globalBadgeName: badgeResult.name,
                        badgeId: badgeResult.badgeId,
                ]
        ))
    }

    static void ensureValidRole(RoleName roleName, String badgeId) {
        if (!globalBadgeSupportedRoles.contains(roleName)) {
            throw new SkillException("Provided [${roleName}] is not a admin group role, admin group id [${badgeId}].", ErrorCode.BadParam)
        }
    }

    private GlobalBadgeResult findGlobalBadgeResult(String globalBadgeId) {
        GlobalBadgeResult globalBadgeDef = globalBadgesService.getBadge(globalBadgeId)
        if (!globalBadgeDef) {
            throw new SkillException("Failed to find global badge with id [${globalBadgeId}].", ErrorCode.BadParam)
        }
        return globalBadgeDef
    }
}
