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

import groovy.util.logging.Slf4j
import jakarta.annotation.PostConstruct
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.UIConfigProperties
import skills.controller.exceptions.QuizValidator
import skills.controller.exceptions.SkillsValidator
import skills.controller.result.model.EnableUserCommunityValidationRes
import skills.controller.result.model.UserRoleRes
import skills.quizLoading.QuizSettings
import skills.services.AccessSettingsStorageService
import skills.services.settings.Settings
import skills.services.settings.SettingsDataAccessor
import skills.storage.model.AdminGroupDef
import skills.storage.model.ProjDef
import skills.storage.model.QuizDef
import skills.storage.model.UserTag
import skills.storage.model.auth.UserRole
import skills.storage.repos.*

import java.util.regex.Pattern

@Service
@Slf4j
class UserCommunityService {

    @Autowired
    UserTagRepo userTagRepo

    @Autowired
    UIConfigProperties uiConfigProperties

    @Autowired
    SettingsDataAccessor settingsDataAccessor

    @Autowired
    QuizSettingsRepo quizSettingsRepo

    @Autowired
    UserRoleRepo userRoleRepo

    @Autowired
    UserAttrsRepo userAttrsRepo

    @Autowired
    ExportedSkillRepo exportedSkillRepo

    @Autowired
    ProjDefRepo projDefRepo

    @Autowired
    QuizDefRepo quizDefRepo

    @Autowired
    QuizToSkillDefRepo quizToSkillDefRepo

    @Autowired
    SkillShareDefRepo skillShareDefRepo

    @Autowired
    SkillRelDefRepo skillRelDefRepo

    @Autowired
    AdminGroupDefRepo adminGroupDefRepo

    @Autowired
    AccessSettingsStorageService accessSettingsStorageService

    String userCommunityUserTagKey
    String userCommunityUserTagValue
    String defaultUserCommunityName
    String restrictedUserCommunityName

    private static final Pattern COMMUNITY_DESCRIPTOR = ~/(?i)\{\{\s?community.descriptor\s?\}\}/
    private static final Pattern PROJECT_COMMUNITY_DESCRIPTOR = ~/(?i)\{\{\s?community.project.descriptor\s?\}\}/

    @PostConstruct
    void init() {
        this.userCommunityUserTagKey = uiConfigProperties.ui.userCommunityUserTagKey
        this.userCommunityUserTagValue = uiConfigProperties.ui.userCommunityUserTagValue
        this.defaultUserCommunityName = uiConfigProperties.ui.defaultCommunityDescriptor;
        this.restrictedUserCommunityName = uiConfigProperties.ui.userCommunityRestrictedDescriptor;
    }

    Boolean isUserCommunityConfigured() {
        return this.userCommunityUserTagKey && this.userCommunityUserTagValue
    }

    Boolean isUserCommunityMember(String userId) {
        Boolean belongsToUserCommunity = false
        if (isUserCommunityConfigured() && StringUtils.isNotBlank(userId)) {
            List<UserTag> userTags = userTagRepo.findAllByUserIdAndKey(userId, userCommunityUserTagKey)
            belongsToUserCommunity = userTags?.find { it?.value == userCommunityUserTagValue }
        }
        return belongsToUserCommunity as Boolean
    }

    EnableUserCommunityValidationRes validateAdminGroupForCommunity(String adminGroupId) {
        EnableUserCommunityValidationRes res = new EnableUserCommunityValidationRes(isAllowed: true, unmetRequirements: [])
        userRoleRepo.findProjectIdsByAdminGroupId(adminGroupId).each { projectId ->
            res = validateProjectForCommunity(projectId, res)
        }
        List<UserRoleRes> allAdminGroupMembers = accessSettingsStorageService.findAllAdminGroupMembers(adminGroupId)
        if (allAdminGroupMembers) {
            List<UserRoleRes> unique = allAdminGroupMembers.unique { it.userId }
            unique.each { UserRoleRes userWithRole ->
                if (!isUserCommunityMember(userWithRole.userId)) {
                    String userIdForDisplay = userWithRole.userIdForDisplay

                    res.isAllowed = false
                    res.unmetRequirements.add("Has existing ${userIdForDisplay} user that is not authorized".toString())
                }
            }
        }

        return res
    }

    EnableUserCommunityValidationRes validateProjectForCommunity(String projId, EnableUserCommunityValidationRes existingValidationRes = null) {
        EnableUserCommunityValidationRes res = existingValidationRes ? existingValidationRes : new EnableUserCommunityValidationRes(isAllowed: true, unmetRequirements: [])

        // only applicable if project already exist; also normalizes project ids case
        ProjDef projDef = projDefRepo.findByProjectIdIgnoreCase(projId)
        if (projDef) {
            List<UserRole> allRoles = userRoleRepo.findAllByProjectIdIgnoreCase(projDef.projectId)
            checkAllUsersAreUCMembers(allRoles, res)

            if (exportedSkillRepo.countSkillsExportedByProject(projDef.projectId) > 0) {
                res.isAllowed = false
                res.unmetRequirements.add("Has skill(s) that have been exported to the Skills Catalog")
            }

            if (skillShareDefRepo.countNumSkillsSharedByProjectId(projDef.projectId) > 0) {
                res.isAllowed = false
                res.unmetRequirements.add("Has skill(s) that have been shared for cross-project dependencies")
            }

            if (skillRelDefRepo.belongsToGlobalBadge(projDef.projectId)) {
                res.isAllowed = false
                res.unmetRequirements.add("This project is part of one or more Global Badges")
            }

            if(adminGroupDefRepo.doesAdminGroupContainNonUserCommunityProject(projDef.projectId)) {
                res.isAllowed = false
                res.unmetRequirements.add("This project is part of one or more Admin Groups that has not enabled user community protection")
            }
        }
        return res;
    }

    EnableUserCommunityValidationRes validateQuizForCommunity(String quizId, EnableUserCommunityValidationRes existingValidationRes = null) {
        EnableUserCommunityValidationRes res = existingValidationRes ? existingValidationRes : new EnableUserCommunityValidationRes(isAllowed: true, unmetRequirements: [])

        // only applicable if project already exist; also normalizes project ids case
        QuizDef quizDef = quizDefRepo.findByQuizIdIgnoreCase(quizId)
        if (quizDef) {
            List<UserRole> allRoles = userRoleRepo.findAllByQuizIdIgnoreCase(quizDef.quizId)
            if(adminGroupDefRepo.doesAdminGroupContainNonUserCommunityQuiz(quizDef.quizId)) {
                res.isAllowed = false
                res.unmetRequirements.add("This quiz is part of one or more Admin Groups that do no have ${getCommunityNameBasedOnConfAndItemStatus(true)} permission".toString())
            } else {
                checkAllUsersAreUCMembers(allRoles, res)
            }

            List<String> nonCommunityProjects = quizToSkillDefRepo.getNonCommunityProjectsThatThisQuizIsLinkedTo(quizDef.id)?.sort()
            if (nonCommunityProjects) {
                res.isAllowed = false
                res.unmetRequirements.add("This quiz is linked to the following project(s) that do not have ${getCommunityNameBasedOnConfAndItemStatus(true)} permission: ${nonCommunityProjects.join(", ")}".toString())
            }
        }
        return res;
    }

    private void checkAllUsersAreUCMembers(List<UserRole> roles, EnableUserCommunityValidationRes res) {
        if (roles) {
            List<UserRole> unique = roles.unique { it.userId }
            unique.each { UserRole userWithRole ->
                if (!isUserCommunityMember(userWithRole.userId)) {
                    String userIdForDisplay = userAttrsRepo.findByUserIdIgnoreCase(userWithRole.userId).userIdForDisplay

                    res.isAllowed = false
                    res.unmetRequirements.add("Has existing ${userIdForDisplay} user that is not authorized".toString())
                }
            }
        }
    }


    /**
     * Checks if the specified adminGroupId is configured as a user community only admin group
     * @param adminGroupId - not null
     * @return true if the adminGroupId exists and has been configured as a user community only admin group
     */
    @Transactional(readOnly = true)
    boolean isUserCommunityOnlyAdminGroup(String adminGroupId) {
        SkillsValidator.isNotBlank(adminGroupId, "adminGroupId")
        AdminGroupDef adminGroupDef = adminGroupDefRepo.findByAdminGroupIdIgnoreCase(adminGroupId)
        return adminGroupDef && adminGroupDef.protectedCommunityEnabled
    }

    /**
     * Checks if the specified projectId is configured as a user community only project
     * @param projectId - not null
     * @return true if the project exists and has been configured as a user community only project
     */
    @Transactional(readOnly = true)
    boolean isUserCommunityOnlyProject(String projectId) {
        SkillsValidator.isNotBlank(projectId, "projectId")
        return settingsDataAccessor.getProjectSetting(projectId, Settings.USER_COMMUNITY_ONLY_PROJECT.settingName)?.isEnabled()
    }

    /**
     * Checks if the specified projectId is configured as a user community only project
     * @param projectId - not null
     * @return true if the project exists and has been configured as a user community only project
     */
    @Transactional(readOnly = true)
    boolean isUserCommunityOnlyQuiz(String quizId) {
        QuizValidator.isNotBlank(quizId, "quizId")
        return quizSettingsRepo.findBySettingAndQuizId(QuizSettings.UserCommunityOnlyQuiz.setting, quizId)?.isEnabled()
    }

    /**
     * Checks if the specified quizId is configured as a user community only quiz
     * @param quizId - not null
     * @return true if the project exists and has been configured as a user community only project
     */
    @Transactional(readOnly = true)
    boolean isUserCommunityOnlyQuiz(Integer quizRefId) {
        SkillsValidator.isNotNull(quizRefId, "quizRefId")
        return quizSettingsRepo.findBySettingAndQuizRefId(QuizSettings.UserCommunityOnlyQuiz.setting, quizRefId)?.isEnabled()
    }

    @Transactional(readOnly = true)
    String getProjectUserCommunity(String projectId) {
       return getCommunityNameBasedOnConfAndItemStatus(isUserCommunityOnlyProject(projectId))
    }

    @Transactional(readOnly = true)
    String getQuizUserCommunity(String quizId) {
        return getCommunityNameBasedOnConfAndItemStatus(isUserCommunityOnlyQuiz(quizId))
    }

    String getCommunityNameBasedOnConfAndItemStatus(Boolean isUserCommunityOnlyItem) {
        if (!restrictedUserCommunityName || isUserCommunityOnlyItem == null) {
            return null
        }
        return isUserCommunityOnlyItem ? restrictedUserCommunityName : defaultUserCommunityName;
    }

    Boolean containsProjectUserCommunityDescriptorVar(String text) {
        return text =~ PROJECT_COMMUNITY_DESCRIPTOR
    }

    String replaceProjectDescriptorVar(String value, String communityHeaderDescriptor) {
        if (value && containsProjectUserCommunityDescriptorVar(value)) {
            assert communityHeaderDescriptor, "Project User Community Header variable found in header/footer, but no replace value found in encodedParams"
            return PROJECT_COMMUNITY_DESCRIPTOR.matcher(value).replaceAll(communityHeaderDescriptor)
        } else {
            return value
        }
    }
}
