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
    GLOBAL_PUBLIC_URL("public_url"),
    GLOBAL_RESET_TOKEN_EXPIRATION("password_reset_token_expiration"),
    GLOBAL_FROM_EMAIL("from_email");

    private String settingName;

    private Settings(String settingName){
        this.settingName = settingName;
    }

    public String getSettingName(){
        return this.settingName;
    }
}
