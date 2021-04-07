package skills.services


import skills.auth.UserInfo
import skills.controller.request.model.UserSettingsRequest
import skills.services.settings.SettingsService
import skills.storage.repos.UserRepo
import spock.lang.Specification

class AccessSettingsStorageServiceSpec extends Specification {

    def "CreateAppUser"() {
        UserInfo userInfo = new UserInfo(
                firstName: 'Joe',
                lastName: 'Schmo',
                email: 'joe@schmo.com',
                username: 'jschmo',
                password: 'password',
        )

        UserInfoValidator userInfoValidator = Mock()
        UserAttrsService userAttrsService = Mock()
        UserRepo userRepo = Mock()
        SettingsService settingsService = Mock()
        String landingPage = 'home'
        AccessSettingsStorageService accessSettingsStorageService = new AccessSettingsStorageService(
                userInfoValidator: userInfoValidator,
                userAttrsService: userAttrsService,
                userRepository: userRepo,
                settingsService: settingsService,
                defaultLandingPage: landingPage,
        )

        accessSettingsStorageService.settingsService = settingsService

        when:
        AccessSettingsStorageService.UserAndUserAttrsHolder user = accessSettingsStorageService.createAppUser(userInfo, false)

        then:
        user
        1 * settingsService.saveSetting(new UserSettingsRequest(
                settingGroup: AccessSettingsStorageService.USER_PREFS_GROUP,
                setting: AccessSettingsStorageService.HOME_PAGE_PREF,
                value: landingPage
        ), _)
    }
}
