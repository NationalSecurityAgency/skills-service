package skills.service.auth.jwt

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.orm.jpa.EntityManagerHolder
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.support.TransactionSynchronizationManager
import skills.service.auth.UserAuthService
import skills.service.auth.UserInfo
import skills.storage.model.auth.RoleName
import skills.storage.model.auth.User
import skills.storage.model.auth.UserProp
import skills.storage.model.auth.UserRole
import skills.storage.repos.UserRepo

import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import javax.persistence.PersistenceContext
import javax.transaction.Transactional

@Service
class SkillsOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    @Autowired
    OAuth2UserConverterService userConverter

    @Autowired
    UserAuthService userAuthService

    @Autowired
    UserRepo userRepository

    @Autowired
    DataSourceTransactionManager transactionManager

    @PersistenceContext
    protected EntityManager em;

    OAuth2UserService delegate = new DefaultOAuth2UserService()

    @Transactional()
    OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        try {
//            EntityManagerHolder emHolder = new EntityManagerHolder(em);
//            TransactionSynchronizationManager.bindResource(em.getEntityManagerFactory(), emHolder);
            OAuth2User oAuth2User = delegate.loadUser(userRequest)
            String clientId = userRequest.clientRegistration.registrationId

            UserInfo currentUser = userConverter.convert(clientId, oAuth2User)
//        userAuthService.createUser(currentUser)
            User user = new User(
                    userId: currentUser.username?.toLowerCase(),
                    password: currentUser.password,
                    roles: [new UserRole(userId: currentUser.username?.toLowerCase(), roleName: RoleName.ROLE_APP_USER)],
                    userProps: [
                            new UserProp(name: 'DN', value: currentUser.userDn ?: ""),
                            new UserProp(name: 'email', value: currentUser.email ?: ""),
                            new UserProp(name: 'firstName', value: currentUser.firstName ?: ""),
                            new UserProp(name: 'lastName', value: currentUser.lastName ?: ""),
                    ]
            )
            userRepository.save(user)

            Authentication newAuth = new UsernamePasswordAuthenticationToken(currentUser, null, currentUser.authorities)

            SecurityContextHolder.getContext().setAuthentication(newAuth)
            // check database, load user if exists (and update?), if not exists then create
            return oAuth2User
        } finally {
//            TransactionSynchronizationManager.unbindResource(em.getEntityManagerFactory());
        }
    }
}
