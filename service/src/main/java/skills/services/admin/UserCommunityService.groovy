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
import skills.controller.exceptions.SkillsValidator
import skills.controller.result.model.EnableProjValidationRes
import skills.services.settings.Settings
import skills.services.settings.SettingsDataAccessor
import skills.storage.model.ProjDef
import skills.storage.model.UserTag
import skills.storage.model.auth.UserRole
import skills.storage.repos.ExportedSkillRepo
import skills.storage.repos.ProjDefRepo
import skills.storage.repos.SkillRelDefRepo
import skills.storage.repos.SkillShareDefRepo
import skills.storage.repos.UserAttrsRepo
import skills.storage.repos.UserRoleRepo
import skills.storage.repos.UserTagRepo

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
    UserRoleRepo userRoleRepo

    @Autowired
    UserAttrsRepo userAttrsRepo

    @Autowired
    ExportedSkillRepo exportedSkillRepo

    @Autowired
    ProjDefRepo projDefRepo

    @Autowired
    SkillShareDefRepo skillShareDefRepo

    @Autowired
    SkillRelDefRepo skillRelDefRepo

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

    EnableProjValidationRes validateProjectForCommunity(String projId) {
        EnableProjValidationRes res = new EnableProjValidationRes(isAllowed: true, unmetRequirements: [])

        // only applicable if project already exist; also normalizes project ids case
        ProjDef projDef = projDefRepo.findByProjectIdIgnoreCase(projId)
        if (projDef) {
            List<UserRole> allRoles = userRoleRepo.findAllByProjectIdIgnoreCase(projDef.projectId)
            if (allRoles) {
                List<UserRole> unique = allRoles.unique { it.userId }
                unique.each { UserRole userWithRole ->
                    if (!isUserCommunityMember(userWithRole.userId)) {
                        String userIdForDisplay = userAttrsRepo.findByUserIdIgnoreCase(userWithRole.userId).userIdForDisplay

                        res.isAllowed = false
                        res.unmetRequirements.add("Has existing ${userIdForDisplay} user that is not authorized".toString())
                    }
                }
            }

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
        }
        return res;
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

    @Transactional(readOnly = true)
    String getProjectUserCommunity(String projectId) {
       return getCommunityNameBasedProjConfStatus(isUserCommunityOnlyProject(projectId))
    }


    String getCommunityNameBasedProjConfStatus(Boolean isUserCommunityOnlyProject) {
        if (!restrictedUserCommunityName || isUserCommunityOnlyProject == null) {
            return null
        }
        return isUserCommunityOnlyProject ? restrictedUserCommunityName : defaultUserCommunityName;
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
