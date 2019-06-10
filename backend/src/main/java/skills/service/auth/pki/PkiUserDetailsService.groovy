package skills.service.auth.pki

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import org.springframework.web.client.RestTemplate
import skills.service.auth.SkillsAuthorizationException
import skills.service.auth.UserAuthService
import skills.service.auth.UserInfo

import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import javax.transaction.Transactional

@Slf4j
class PkiUserDetailsService implements UserDetailsService, AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {

    @Value('${skills.authorization.userInfoUri}')
    String userInfoUri

    @Autowired
    UserAuthService userAuthService

    @PersistenceContext
    protected EntityManager em

    RestTemplate restTemplate = new RestTemplate()

    @Override
    @Transactional
    UserDetails loadUserByUsername(String dn) throws UsernameNotFoundException {
        UserInfo userInfo
        try {
            userInfo = restTemplate.getForObject(userInfoUri, UserInfo, dn)
            if (userInfo) {
                UserInfo existingUserInfo = userAuthService.loadByUserId(userInfo.username)
                if (existingUserInfo) {
                    userInfo.password = existingUserInfo.password
                    userInfo.nickname = existingUserInfo.nickname
                } else {
                    userInfo.password = 'PKI_AUTHENTICATED'
                }

                // update user properties and load user roles, or create the account if this is the first time the user has connected
                if (!em.isJoinedToTransaction()) {
                    em.joinTransaction()
                }
                userInfo = userAuthService.createOrUpdateUser(userInfo)
            } else {
                throw new SkillsAuthorizationException("Unknown user [$dn]")
            }
        } catch (Exception e) {
            log.error("Error occurred looking up user info for DN [${dn}]", e)
            throw new BadCredentialsException("Unable to retrieve user info for [${dn}]", e)
        }
        return userInfo
    }

    @Override
    UserDetails loadUserDetails(PreAuthenticatedAuthenticationToken token) throws UsernameNotFoundException {
        return loadUserByUsername(token.getName())
    }
}
