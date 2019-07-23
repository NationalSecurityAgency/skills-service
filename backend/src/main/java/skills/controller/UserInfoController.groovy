package skills.controller

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import skills.services.UserAdminService
import skills.storage.model.auth.User
import skills.storage.repos.UserRepo

@RestController
@RequestMapping("/app")
@Slf4j
@skills.profile.EnableCallStackProf
class UserInfoController {

    @Autowired
    skills.auth.UserInfoService userInfoService

    @Autowired
    skills.auth.UserAuthService userAuthService

    @Autowired
    UserRepo userRepo

    @Value('#{securityConfig.authMode}}')
    skills.auth.AuthMode authMode = skills.auth.AuthMode.DEFAULT_AUTH_MODE

    @Autowired
    UserAdminService userAdminService

    @Autowired(required = false)
    skills.auth.pki.PkiUserLookup pkiUserLookup


    @RequestMapping(value = "/userInfo", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    def getUserInfo() {
        def res = 'null'
        skills.auth.UserInfo currentUser = userInfoService.getCurrentUser()
        if (currentUser) {
            res = new skills.controller.result.model.UserInfoRes(currentUser)
        } else if (authMode == skills.auth.AuthMode.PKI) {
            throw new skills.auth.SkillsAuthorizationException('Unauthenticated user while using PKI Authorization Mode')
        }
        return res
    }

    @RequestMapping(value = "/userInfo", method = [RequestMethod.POST, RequestMethod.PUT], produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    skills.controller.result.model.RequestResult updateUserInfo(@RequestBody skills.controller.result.model.UserInfoRes userInfoReq) {
        skills.auth.UserInfo currentUser = userInfoService.getCurrentUser()
        if (currentUser) {
            if (userInfoReq.first) {
                currentUser.firstName = userInfoReq.first
            }
            if (userInfoReq.last) {
                currentUser.lastName = userInfoReq.last
            }
            currentUser.nickname = userInfoReq.nickname
            userAuthService.createOrUpdateUser(currentUser)
        } else if (authMode == skills.auth.AuthMode.PKI) {
            throw new skills.auth.SkillsAuthorizationException('Unauthenticated user while using PKI Authorization Mode')
        }
        return new skills.controller.result.model.RequestResult(success: true)
    }

    @RequestMapping(value = "/userInfo/hasRole/{role}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    boolean hasRole(@PathVariable("role") String role) {
        role = role.toLowerCase()
        skills.auth.UserInfo currentUser = userInfoService.getCurrentUser()
        return currentUser.authorities.find { it.authority.toLowerCase() == role }
    }

    @RequestMapping(value = "/userInfo/hasAnyRole/{roles}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    boolean hasAnyRole(@PathVariable("roles") List<String> roles) {
        boolean foundRole = false
        roleCheckLoop:
        for (String role : roles) {
            if (hasRole(role)) {
                foundRole = true
                break roleCheckLoop
            }
        }
        return foundRole
    }

    @RequestMapping(value = "/users/suggestDashboardUsers/{query}", method = RequestMethod.GET, produces = "application/json")
    List<skills.controller.result.model.UserInfoRes> suggestExistingDashboardUsers(@PathVariable("query") String query,
                                                                                   @RequestParam(required = false) boolean includeSelf) {
        List<User> matchingUsers = userRepo.getUserByUserIdOrPropWildcard(query, new PageRequest(0, 6))
        List<skills.controller.result.model.UserInfoRes> results = matchingUsers.collect { new skills.controller.result.model.UserInfoRes(it) }
        if (!includeSelf) {
            String currentUserId = userInfoService.currentUser.username
            results = results.findAll { it.userId != currentUserId }
        }
        return results.take(5)
    }

    @RequestMapping(value = "/users/validExistingDashboardUserId/{userId}", method = RequestMethod.GET, produces = "application/json")
    Boolean isValidExistingDashboardUserId(@PathVariable("userId") String userId) {
        return userRepo.findByUserIdIgnoreCase(userId) != null
    }

    @RequestMapping(value = "/users/projects/{projectId}/suggestClientUsers/{query}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    List<skills.controller.result.model.UserInfoRes> suggestExistingClientUsersForProject(@PathVariable("projectId") String projectId, @PathVariable("query") String query) {
        return userAdminService.suggestUsersForProject(projectId, query, new PageRequest(0, 5)).collect { new skills.controller.result.model.UserInfoRes(userId: it) }
    }

    @RequestMapping(value = "/users/suggestClientUsers/{query}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    List<skills.controller.result.model.UserInfoRes> suggestExistingClientUsers(@PathVariable("query") String query) {
        return userAdminService.suggestUsers(query, new PageRequest(0, 5)).collect { new skills.controller.result.model.UserInfoRes(userId: it) }
    }

    @RequestMapping(value = "/users/projects/{projectId}/validExistingClientUserId/{userId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    Boolean isValidExistingClientUserIdForProject(@PathVariable("projectId") String projectId, @PathVariable("userId") String userId) {
        return userAdminService.isValidExistingUserIdForProject(projectId, userId)
    }

    @RequestMapping(value = "/users/validExistingClientUserId/{userId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    Boolean isValidExistingClientUserId(@PathVariable("userId") String userId) {
        return userAdminService.isValidExistingUserId(userId)
    }

    @RequestMapping(value = "/users/suggestPkiUsers/{query}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    List<skills.controller.result.model.UserInfoRes> suggestExistingPkiUsers(@PathVariable("query") String query) {
        return pkiUserLookup?.suggestUsers(query)?.take(5).collect { new skills.controller.result.model.UserInfoRes(it) }
    }

}

