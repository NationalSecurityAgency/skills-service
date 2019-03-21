package skills.service.auth.form.oauth2

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.provider.OAuth2Authentication
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationManager
import org.springframework.security.oauth2.provider.token.DefaultTokenServices
import org.springframework.stereotype.Component

@Component('skillsOAuth2AuthManager')
class SkillsOAuth2AuthenticationManager extends OAuth2AuthenticationManager {

    @Autowired
    OAuthUtils oAuthUtils

    SkillsOAuth2AuthenticationManager(DefaultTokenServices tokenServices) {
        setTokenServices(tokenServices)
    }

    @Override
    Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Authentication auth = super.authenticate(authentication)
        if (auth.isAuthenticated() && auth instanceof OAuth2Authentication) {
            auth = oAuthUtils.convertToSkillsAuth(auth)
        }
        return auth
    }
}