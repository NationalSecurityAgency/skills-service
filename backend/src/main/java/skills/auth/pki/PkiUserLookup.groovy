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

    @Value('${skills.authorization.userInfoHealthCheckUri}')
    String userInfoHealthCheckUri

    @Profile
    UserInfo lookupUserDn(String dn) {
        UserInfo userInfo = restTemplate.getForObject(userInfoUri, UserInfo, dn)
        validate(userInfo, dn)
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
            validate(userInfo, query)
        }
        return matches
    }

    @Profile
    boolean isServiceAvailable() {
        return restTemplate.getForObject(userInfoHealthCheckUri, Status).status == Status.STATUS.UP
    }

    private void validate(UserInfo userInfo, String requestValue) {
        if (!userInfo) {
            throw new SkillException("User info service does not have key [${requestValue}]")
        }

        if (!userInfo?.username) {
            throw new SkillException("User info service result must contain username. request value=[${requestValue}]")
        }
        if (!userInfo?.usernameForDisplay) {
            throw new SkillException("User info service result must contain usernameForDisplay. request value=[${requestValue}]")
        }
    }

    static class Status {
        enum STATUS { UP }
        STATUS status
    }
}
