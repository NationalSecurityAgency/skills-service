package skills.service.auth.pki

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import skills.service.auth.UserInfo

class PkiUserLookup {

    RestTemplate restTemplate = new RestTemplate()

    @Value('${skills.authorization.userInfoUri}')
    String userInfoUri

    @Value('${skills.authorization.userQueryUri}')
    String userQueryUri

    UserInfo lookupUserDn(String dn) {
        return restTemplate.getForObject(userInfoUri, UserInfo, dn)
    }

    List<UserInfo> suggestUsers(String query) {
        ResponseEntity<List<UserInfo>> response = restTemplate.exchange(
                userQueryUri,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<UserInfo>>(){},
                query)
        List<UserInfo> matches = response.getBody()
        return matches
    }
}
