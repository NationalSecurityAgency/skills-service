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
