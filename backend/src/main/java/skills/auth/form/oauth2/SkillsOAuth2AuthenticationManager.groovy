package skills.auth.form.oauth2

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Conditional
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.client.resource.OAuth2AccessDeniedException
import org.springframework.security.oauth2.provider.OAuth2Authentication
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationManager
import org.springframework.security.oauth2.provider.token.DefaultTokenServices
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

import javax.servlet.http.HttpServletRequest

@Component('skillsOAuth2AuthManager')
@Conditional(skills.auth.SecurityConfiguration.FormAuth)
@Slf4j
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
            String projectId = skills.auth.AuthUtils.getProjectIdFromRequest(servletRequest)
            auth = oAuthUtils.convertToSkillsAuth(auth)
            if (projectId && auth && auth.principal instanceof skills.auth.UserInfo) {
                String proxyingSystemId = auth.principal.proxyingSystemId
                if (projectId != proxyingSystemId) {
                    throw new OAuth2AccessDeniedException("Invalid token - proxyingSystemId [${proxyingSystemId}] does not match resource projectId [${projectId}]");
                }
            }
        }
        return auth
    }

    HttpServletRequest getServletRequest() {
        HttpServletRequest httpServletRequest
        try {
            ServletRequestAttributes currentRequestAttributes = RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes
            httpServletRequest = currentRequestAttributes.getRequest()
        } catch (Exception e) {
            log.warn("Unable to current request attributes. Error Recieved [$e]")
        }
        return httpServletRequest
    }
}
