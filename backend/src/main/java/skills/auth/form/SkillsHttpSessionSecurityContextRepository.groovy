package skills.auth.form

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.oauth2.provider.OAuth2Authentication
import org.springframework.security.web.context.HttpRequestResponseHolder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.security.web.context.NullSecurityContextRepository
import skills.auth.form.oauth2.OAuthUtils

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * this class is responsible for converting OAuth2 authenticated principal's to UserInfo objects
 * and storing them in the SecurityContextHolder.  It also reload the users granted_authorities
 * on each request.
 */
@Slf4j
class SkillsHttpSessionSecurityContextRepository extends HttpSessionSecurityContextRepository {

    @Autowired
    OAuthUtils oAuthUtils

    @Autowired
    skills.auth.UserAuthService userAuthService

    private NullSecurityContextRepository nullSecurityContextRepository = new NullSecurityContextRepository()

    @Override
    synchronized
    SecurityContext loadContext(HttpRequestResponseHolder requestResponseHolder) {
        SecurityContext context
        if (isProxyiedAuth(SecurityContextHolder.getContext(), requestResponseHolder.request)) {
            // do not store proxied authentication on the HttpSession
            context = nullSecurityContextRepository.loadContext(requestResponseHolder)
        } else {
            // Let the parent class actually get the SecurityContext from the HTTPSession first.
            context = super.loadContext(requestResponseHolder)

            Authentication auth = context.getAuthentication()
            if (auth && auth instanceof OAuth2Authentication) {
                // OAuth2Authentication is used when then the OAuth2 client uses the client_credentials grant_type we
                // look for a custom "proxy_user" field where the trusted client must specify the skills user that the
                // request is being performed on behalf of.  The proxy_user field is required for client_credentials grant_type
                auth = oAuthUtils.convertToSkillsAuth(auth)
            } else if (auth && auth instanceof OAuth2AuthenticationToken && auth.principal instanceof OAuth2User) {
                auth = oAuthUtils.convertToSkillsAuth(auth)
            } else if (auth && auth.principal instanceof skills.auth.UserInfo) {
                // reload the granted_authorities for this skills user if loaded from the HTTP Session)
                skills.auth.UserInfo userInfo = auth.principal
                userInfo.authorities = userAuthService.loadAuthorities(userInfo.username)
                auth = new UsernamePasswordAuthenticationToken(userInfo, auth.credentials, userInfo.authorities)
            }

            context.setAuthentication(auth)
        }
        return context
    }

    @Override
    void saveContext(SecurityContext context, HttpServletRequest request, HttpServletResponse response) {
        if (isProxyiedAuth(context, request)) {
            // do not store proxied authentication on the HttpSession
            nullSecurityContextRepository.saveContext(context, request, response)
        } else {
            super.saveContext(context, request, response)
        }
    }

    private boolean isProxyiedAuth(SecurityContext context, HttpServletRequest request) {
        Authentication auth = context.authentication
        return (auth &&
                (auth instanceof OAuth2Authentication ||
                        (auth.principal &&
                                auth.principal instanceof skills.auth.UserInfo &&
                                auth.principal.isProxied()
                        )
                )
        ) || oAuthUtils.oAuthRequestedMatcher.matches(request)
    }
}
