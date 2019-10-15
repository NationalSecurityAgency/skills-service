package skills.auth.pki

import callStack.profiler.Profile
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Conditional
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import skills.auth.SecurityMode
import skills.auth.UserInfo
import skills.controller.exceptions.SkillException

@Component
@Conditional(SecurityMode.PkiAuth)
class PkiUserLookup {

    RestTemplate restTemplate = new RestTemplate()

    @Value('${skills.authorization.userInfoUri}')
    String userInfoUri

    @Value('${skills.authorization.userQueryUri}')
    String userQueryUri

    @Profile
    UserInfo lookupUserDn(String dn) {
        UserInfo userInfo = restTemplate.getForObject(userInfoUri, UserInfo, dn)
        validate(userInfo)
        return userInfo
    }

    @Profile
    List<UserInfo> suggestUsers(String query) {
        ResponseEntity<List<UserInfo>> response = restTemplate.exchange(
                userQueryUri,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<UserInfo>>(){},
                query)
        List<UserInfo> matches = response.getBody()
        for (UserInfo userInfo : matches) {
            validate(userInfo)
        }
        return matches
    }

    private void validate(UserInfo userInfo, String requestValue) {
        if (!userInfo.username) {
            throw new SkillException("User info service result must contain username. request value=[${requestValue}]")
        }
        if (!userInfo.usernameForDisplay) {
            throw new SkillException("User info service result must contain usernameForDisplay. request value=[${requestValue}]")
        }
    }
}
