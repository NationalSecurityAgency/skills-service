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
import skills.services.settings.Settings
import skills.services.settings.SettingsDataAccessor
import skills.storage.model.UserTag
import skills.storage.repos.UserTagRepo

@Service
@Slf4j
class UserCommunityService {

    @Autowired
    UserTagRepo userTagRepo

    @Autowired
    UIConfigProperties uiConfigProperties

    @Autowired
    SettingsDataAccessor settingsDataAccessor

    String userCommunityUserTagKey
    String userCommunityUserTagValue

    @PostConstruct
    void init() {
        this.userCommunityUserTagKey = uiConfigProperties.ui.userCommunityUserTagKey
        this.userCommunityUserTagValue = uiConfigProperties.ui.userCommunityUserTagValue
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
        return belongsToUserCommunity
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

}
