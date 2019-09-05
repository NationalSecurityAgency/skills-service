package skills.services

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import skills.auth.UserInfo
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException

import static skills.controller.exceptions.SkillException.NA

@Component
class UserInfoValidator {

    @Value('#{"${skills.config.ui.maxFirstNameLength}"}')
    int maxFirstNameLength

    @Value('#{"${skills.config.ui.maxLastNameLength}"}')
    int maxLastNameLength

    @Value('#{"${skills.config.ui.maxNicknameLength}"}')
    int maxNicknameLength

    void validate(UserInfo userInfo) {
        if (!userInfo.firstName || userInfo.firstName.length() > maxFirstNameLength) {
            throw new SkillException("First Name is required and can be no longer than 30 characters", NA, NA, ErrorCode.BadParam)
        }
        if (!userInfo.lastName || userInfo.lastName.length() > maxLastNameLength) {
            throw new SkillException("Last Name is required and can be no longer than 30 characters", NA, NA, ErrorCode.BadParam)
        }
        // nickname by default is "firstName lastName"
        if (userInfo.nickname && userInfo.nickname.length() > maxNicknameLength) {
            throw new SkillException("Nickname cannot be over 70 characters", NA, NA, ErrorCode.BadParam)
        }
    }

}
