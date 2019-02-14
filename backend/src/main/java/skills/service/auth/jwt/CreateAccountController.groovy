package skills.service.auth.jwt

import groovy.util.logging.Slf4j
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
import skills.service.controller.result.model.OAuth2Provider

import javax.servlet.http.HttpServletResponse

@Conditional(SecurityConfiguration.JwtCondition)
@RestController
@RequestMapping("/")
@Slf4j
class CreateAccountController {

    @Autowired
    UserAuthService userAuthService

    @Autowired
    PasswordEncoder passwordEncoder

    @Autowired
    JwtHelper jwtHelper

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    OAuth2ProviderProperties oAuth2ProviderProperties

    @PutMapping("createAccount")
    void createAppUser(@RequestBody UserInfo userInfo, HttpServletResponse response) {
        userInfo.password = passwordEncoder.encode(userInfo.password)
        userInfo.username = userInfo.email
        userInfo = userAuthService.createUser(userInfo)
        jwtHelper.addToken(response, userInfo)
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

    @GetMapping("/test")
    String testEndpoint() {
        return "TEST ENDPOINT!!!"
    }

    @Component
    @Configuration
    @ConfigurationProperties(prefix = "spring.security.oauth2.client")
    static class OAuth2ProviderProperties {
        final Map<String, OAuth2Provider> registration = new HashMap<>();
    }
}
