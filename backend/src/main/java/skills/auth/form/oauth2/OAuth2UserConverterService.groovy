package skills.auth.form.oauth2

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Conditional
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Component

import javax.annotation.Resource

@Component
@Conditional(skills.auth.SecurityConfiguration.FormAuth)
class OAuth2UserConverterService {

    @Autowired
    skills.auth.UserAuthService userAuthService

    @Resource(name='oauth2UserConverters')
    Map<String, OAuth2UserConverter> lookup = [:]

    skills.auth.UserInfo convert(String clientId, OAuth2User oAuth2User) {
        skills.auth.UserInfo userInfo
        OAuth2UserConverter converter = lookup.get(clientId.toLowerCase())
        if (converter) {
            userInfo = converter.convert(clientId, oAuth2User)
        } else {
            throw new skills.auth.SkillsAuthorizationException("No OAuth2UserConverter configured for clientId [${clientId}]")
        }
        return userInfo
    }

    static interface OAuth2UserConverter {
        String getClientId()
        skills.auth.UserInfo convert(String clientId, OAuth2User oAuth2User)
    }

    static class GitHubUserConverter implements OAuth2UserConverter {
        static final String NAME = 'name'
        static final String EMAIL = 'email'

        String clientId = 'github'

        @Override
        skills.auth.UserInfo convert(String clientId, OAuth2User oAuth2User) {
            String username = oAuth2User.getName()
            assert username, "Error getting name attribute of oAuth2User [${oAuth2User}] from clientId [$clientId]"
            String email =  oAuth2User.attributes.get(EMAIL)
            if (!email) {
                throw new skills.auth.SkillsAuthorizationException("Email must be available in your public Github profile")
            }
            String name = oAuth2User.attributes.get(NAME)
            if (!name) {
                throw new skills.auth.SkillsAuthorizationException("Name must be available in your public Github profile")
            }
            String firstName = name?.tokenize()?.first()
            List tokens = name?.tokenize()
            tokens?.pop()
            String lastName = tokens?.join(' ')
            return new skills.auth.UserInfo(
                    username: "${username}-${clientId}",
                    email:email,
                    firstName: firstName,
                    lastName: lastName,
            )
        }
    }

    static class GoogleUserConverter implements OAuth2UserConverter {
        static final String FIRST_NAME = 'given_name'
        static final String LAST_NAME = 'family_name'
        static final String EMAIL = 'email'

        String clientId = 'google'

        @Override
        skills.auth.UserInfo convert(String clientId, OAuth2User oAuth2User) {
            String username = oAuth2User.getName()
            assert username, "Error getting name attribute of oAuth2User [${oAuth2User}] from clientId [$clientId]"
            String firstName =  oAuth2User.attributes.get(FIRST_NAME)
            String lastName =  oAuth2User.attributes.get(LAST_NAME)
            String email =  oAuth2User.attributes.get(EMAIL)
            assert firstName && lastName && email, "First Name [$firstName], Last Name [$lastName], and email [$email] must be available in your public Google profile"

            return new skills.auth.UserInfo(
                    username: "${username}-${clientId}",
                    email:email,
                    firstName: firstName,
                    lastName: lastName,
            )
        }
    }
}
