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
package skills.services.settings;

public enum Settings {
    LEVEL_AS_POINTS("level.points.enabled"),
    PRODUCTION_MODE("production.mode.enabled"),
    EXPIRING_UNUSED("expiration.expiring.unused"),
    GLOBAL_RESET_TOKEN_EXPIRATION("password_reset_token_expiration"),
    GLOBAL_CUSTOM_HEADER("custom_header"),
    GLOBAL_CUSTOM_FOOTER("custom_footer"),
    GLOBAL_USER_AGREEMENT("user_agreement"),
    GLOBAL_USER_AGREEMENT_VERSION("user_agreement_version"),
    USER_VIEWED_USER_AGREEMENT("viewed_user_agreement"),
    INVITE_ONLY_PROJECT("invite_only"),
    GROUP_DESCRIPTIONS("group-descriptions"),
    GROUP_INFO_ON_SKILL_PAGE("group-info-on-skill-page"),
    SHOW_PROJECT_DESCRIPTION_EVERYWHERE("show_project_description_everywhere"),
    DISABLE_SKILLS_DISPLAY_ACHIEVEMENTS_CELEBRATIONS("skills-display-achievement-celebration-disabled"),
    USER_PROJECT_ROLE("user_project_role"),
    USER_COMMUNITY_ONLY_PROJECT("user_community"),
    PROJECT_COMMUNITY_VALUE("project_community_value"),
    PROJECT_PROTECTION("project-deletion-protection");


    private String settingName;

    private Settings(String settingName){
        this.settingName = settingName;
    }

    public String getSettingName(){
        return this.settingName;
    }
}
