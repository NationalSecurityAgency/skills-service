package skills.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Conditional
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.oauth2.common.OAuth2AccessToken
import org.springframework.security.oauth2.provider.endpoint.TokenEndpoint
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import skills.auth.SecurityMode
import skills.services.InceptionProjectService

@Conditional(SecurityMode.FormAuth)
@RestController
@skills.profile.EnableCallStackProf
class UserTokenController {

    @Autowired
    TokenEndpoint tokenEndpoint

    /**
     * token for inception
     * @param userId
     * @return
     */
    @RequestMapping(value = "/app/projects/Inception/users/{userId}/token", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<OAuth2AccessToken> getUserToken(@PathVariable("userId") String userId) {
        return createToken(InceptionProjectService.inceptionProjectId, userId)
    }


    /**
     * utilized by client-display within a project that previews that project's points
     * @param projectId
     * @param userId
     * @return
     */
    @RequestMapping(value = "/admin/projects/{projectId}/token/{userId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<OAuth2AccessToken> getUserToken(@PathVariable("projectId") String projectId, @PathVariable("userId") String userId) {
        return createToken(projectId, userId)
    }

    private ResponseEntity<OAuth2AccessToken> createToken(String projectId, String userId) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        skills.controller.exceptions.SkillsValidator.isNotBlank(userId, "User Id")

        UsernamePasswordAuthenticationToken principal = new UsernamePasswordAuthenticationToken(projectId, null, [])
        Map<String, String> parameters = [grant_type: 'client_credentials', proxy_user: userId]
        return tokenEndpoint.postAccessToken(principal, parameters)
    }
}
