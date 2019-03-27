package skills.service.auth.form

import groovy.util.logging.Slf4j
import org.apache.commons.lang3.Validate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.*
import skills.service.auth.SecurityConfiguration
import skills.service.auth.UserAuthService
import skills.service.auth.UserInfo
import skills.service.auth.UserSkillsGrantedAuthority
import skills.service.controller.exceptions.SkillsValidator
import skills.service.controller.result.model.OAuth2Provider
import skills.storage.model.auth.RoleName
import skills.storage.model.auth.UserRole

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Conditional(SecurityConfiguration.FormAuth)
@RestController
@RequestMapping("/")
@Slf4j
class CreateAccountController {

    @Autowired
    UserAuthService userAuthService

    @Autowired
    PasswordEncoder passwordEncoder

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    OAuth2ProviderProperties oAuth2ProviderProperties

    @PutMapping("createAccount")
    void createAppUser(@RequestBody UserInfo userInfo, HttpServletResponse response) {
        String password = userInfo.password
        userInfo.password = passwordEncoder.encode(password)
        if (!userInfo.username) {
            userInfo.username = userInfo.email
        }
        userInfo = userAuthService.createUser(userInfo)
        userAuthService.autologin(userInfo, password)
    }

    @PutMapping("createRootAccount")
    void createRootUser(@RequestBody UserInfo userInfo, HttpServletResponse response) {
        SkillsValidator.isTrue(!userAuthService.rootExists(), 'A root user already exists! Granting additional root privileges requires a root user to grant them!')
        String password = userInfo.password
        userInfo.password = passwordEncoder.encode(password)
        if (!userInfo.username) {
            userInfo.username = userInfo.email
        }
        userInfo.authorities = [new UserSkillsGrantedAuthority(new UserRole(
                roleName: RoleName.ROLE_SUPER_DUPER_USER
        ))]
        userAuthService.createUser(userInfo)
    }

    @PostMapping('grantFirstRoot')
    void grantFirstRoot(HttpServletRequest request) {
        SkillsValidator.isTrue(!userAuthService.rootExists(), 'A root user already exists! Granting additional root privileges requires a root user to grant them!')
        SkillsValidator.isNotNull(request.getUserPrincipal(), 'Granting the first root user is only available in PKI mode, but it looks like the request was not made by an authenticated account!')
        userAuthService.grantRoot(request.getUserPrincipal().name)
    }

    @GetMapping('userExists/{user}')
    boolean userExists(@PathVariable('user') String user) {
        return userAuthService.userExists(user)
    }

    @GetMapping("/app/oAuthProviders")
    List<OAuth2Provider> getOAuthProviders() {
        List<OAuth2Provider> providers = []
        clientRegistrationRepository.iterator().each { ClientRegistration clientRegistration ->
            OAuth2Provider oAuth2Provider = oAuth2ProviderProperties.registration.get(clientRegistration.registrationId)
            oAuth2Provider.registrationId = oAuth2Provider.registrationId ?: clientRegistration.registrationId
            oAuth2Provider.clientName = oAuth2Provider.clientName ?: clientRegistration.clientName
            providers.add(oAuth2Provider)
        }
        return providers
    }

    @Component
    @Configuration
    @ConfigurationProperties(prefix = "spring.security.oauth2.client")
    static class OAuth2ProviderProperties {
        final Map<String, OAuth2Provider> registration = new HashMap<>();
    }
}
