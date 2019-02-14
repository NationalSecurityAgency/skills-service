package skills.service.auth.jwt

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.oauth2.provider.OAuth2Authentication
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails
import org.springframework.security.web.context.HttpRequestResponseHolder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.web.servlet.HandlerInterceptor
import skills.service.auth.SkillsAuthorizationException
import skills.service.auth.UserAuthService
import skills.service.auth.UserInfo

import javax.annotation.PostConstruct
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.transaction.Transactional

/**
 * this class is responsible for converting OAuth2 authenticated principal's to UserInfo objects
 * and storing them in the SecurityContextHolder.  It also reload the users granted_authorities
 * on each request.
 */
@Slf4j
class SkillsHttpSessionSecurityContextRepository extends HttpSessionSecurityContextRepository {

    @Autowired
    OAuth2UserConverterService userConverter

    @Autowired
    UserAuthService userAuthService

    @PersistenceContext
    protected EntityManager em

    @Override
    @Transactional
    SecurityContext loadContext(HttpRequestResponseHolder requestResponseHolder) {

        // Let the parent class actually get the SecurityContext from the HTTPSession first.
        SecurityContext context = super.loadContext(requestResponseHolder)

        Authentication auth = context.getAuthentication()
        if (!em.isJoinedToTransaction()) {
            em.joinTransaction()
        }

//        Authentication auth = SecurityContextHolder.getContext().getAuthentication()
        if (auth instanceof OAuth2Authentication) {
            // OAuth2Authentication is used when then the OAuth2 client uses the client_credentials grant_type we
            // look for a custom "proxy_user" field where the trusted client must specify the skills user that the
            // request is being performed on behalf of.  The proxy_user field is required for client_credentials grant_type
            OAuth2AuthenticationDetails oauthDetails = (OAuth2AuthenticationDetails) auth.getDetails()
            Map claims = oauthDetails.getDecodedDetails()
            if (claims && claims.containsKey('proxy-user')) {
                String proxyUserId = claims.get('proxy-user')
                if (!proxyUserId) {
                    throw new SkillsAuthorizationException("client_credentials grant_type must specify proxy-user field for ")
                }
                log.info("Loading proxyUser [${proxyUserId}]")
                UserInfo currentUser = new UserInfo(
                        username: proxyUserId,
                        proxied: true,
                        proxyingSystemId: auth.principal
                )
                // Create new Authentication using UserInfo
                auth = new UsernamePasswordAuthenticationToken(currentUser, null, currentUser.authorities)
            }
        } else if (auth && auth instanceof OAuth2AuthenticationToken && auth.principal instanceof OAuth2User) {
            String clientId = auth.authorizedClientRegistrationId
            OAuth2User oAuth2User = auth.principal
            // convert to UserInfo using configured converter for registrationId (fail if none available)
            UserInfo currentUser = userConverter.convert(clientId, oAuth2User)

            // also create/update the UserInfo in the database.
            currentUser = userAuthService.createOrUpdateUser(currentUser)

            // Create new Authentication using UserInfo
            auth = new UsernamePasswordAuthenticationToken(currentUser, null, currentUser.authorities)
        } else if (auth && auth.principal instanceof UserInfo) {
            // reload the granted_authorities for this skills user
            UserInfo userInfo = auth.principal
            userInfo.authorities = userAuthService.loadAuthorities(userInfo.username)
        }

//        SecurityContextHolder.getContext().setAuthentication(auth)
        context.setAuthentication(auth)

        return context
    }
}
