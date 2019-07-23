package skills.auth.pki

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate

class PkiUserLookup {

    RestTemplate restTemplate = new RestTemplate()

    @Value('${skills.authorization.userInfoUri}')
    String userInfoUri

    @Value('${skills.authorization.userQueryUri}')
    String userQueryUri

    skills.auth.UserInfo lookupUserDn(String dn) {
        return restTemplate.getForObject(userInfoUri, skills.auth.UserInfo, dn)
    }

    List<skills.auth.UserInfo> suggestUsers(String query) {
        ResponseEntity<List<skills.auth.UserInfo>> response = restTemplate.exchange(
                userQueryUri,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<skills.auth.UserInfo>>(){},
                query)
        List<skills.auth.UserInfo> matches = response.getBody()
        return matches
    }
}
