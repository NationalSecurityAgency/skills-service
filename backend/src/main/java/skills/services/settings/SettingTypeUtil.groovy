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
package skills.services.settings

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import skills.controller.exceptions.SkillException
import skills.controller.request.model.GlobalSettingsRequest
import skills.controller.request.model.ProjectSettingsRequest
import skills.controller.request.model.SettingsRequest
import skills.controller.request.model.UserProjectSettingsRequest
import skills.controller.request.model.UserSettingsRequest
import skills.storage.model.Setting.SettingType

@Slf4j
@CompileStatic
class SettingTypeUtil {

    static SettingType getType(SettingsRequest request) {
        if(request instanceof UserProjectSettingsRequest){
            return SettingType.UserProject
        } else if(request instanceof UserSettingsRequest){
            return SettingType.User
        } else if(request instanceof GlobalSettingsRequest){
            return SettingType.Global
        } else if(request instanceof ProjectSettingsRequest){
            return SettingType.Project
        } else{
            log.error("unable SettingRequest [${request.getClass()}]")
            throw new SkillException("Unrecognized Setting type")
        }
    }
}
