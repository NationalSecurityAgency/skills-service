package skills.service.controller

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import skills.service.auth.AuthMode
import skills.service.auth.SkillsAuthorizationException
import skills.service.auth.UserAuthService
import skills.service.auth.UserInfo
import skills.service.auth.UserInfoService
import skills.service.datastore.services.UserAdminService
import skills.storage.model.auth.User
import skills.storage.repos.UserRepo

@RestController
@RequestMapping("/app")
@Slf4j
class UserInfoController {

    @Autowired
    UserInfoService userInfoService

    @Autowired
    UserAuthService userAuthService

    @Autowired
    UserRepo userRepo

    @Value('#{securityConfig.authMode}}')
    AuthMode authMode = AuthMode.DEFAULT_AUTH_MODE

    @Autowired
    UserAdminService userAdminService

    static class UserInfoRes {
        String userId
        String first
        String last
        String nickname
    }

    @RequestMapping(value = "/userInfo", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    def getUserInfo() {
        def res = 'null'
        UserInfo currentUser = userInfoService.getCurrentUser()
        if (currentUser) {
            res = convertToUserinfoRes(currentUser)
        } else if (authMode == AuthMode.PKI) {
            throw new SkillsAuthorizationException('Unauthenticated user while using PKI Authorization Mode')
        }
        return res
    }

    @RequestMapping(value = "/userInfo", method = [RequestMethod.POST, RequestMethod.PUT], produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    UserInfoRes updateUserInfo(@RequestBody UserInfoRes userInfoReq) {
        UserInfoRes res
        UserInfo currentUser = userInfoService.getCurrentUser()
        if (currentUser) {
            if (userInfoReq.first) {
                currentUser.firstName = userInfoReq.first
            }
            if (userInfoReq.last) {
                currentUser.lastName = userInfoReq.last
            }
            currentUser.nickname = userInfoReq.nickname
            currentUser = userAuthService.createOrUpdateUser(currentUser)
            res  = convertToUserinfoRes(currentUser)
        } else if (authMode == AuthMode.PKI) {
            throw new SkillsAuthorizationException('Unauthenticated user while using PKI Authorization Mode')
        }
        return res
    }

    UserInfoRes convertToUserinfoRes(UserInfo userInfo) {
        return new UserInfoRes(
                userId: userInfo.username,
                first: userInfo.firstName,
                last: userInfo.lastName,
                nickname: userInfo.nickname
        )
    }

    @RequestMapping(value = "/userInfo/hasRole/{role}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    boolean hasRole(@PathVariable("role") String role) {
        role = role.toLowerCase()
        UserInfo currentUser = userInfoService.getCurrentUser()
        return currentUser.authorities.find {it.authority.toLowerCase() == role}
    }

    @RequestMapping(value = "/userInfo/hasAnyRole/{roles}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    boolean hasAnyRole(@PathVariable("roles") List<String> roles) {
        boolean foundRole = false
        roleCheckLoop: for (String role : roles) {
            if (hasRole(role)) {
                foundRole = true
                break roleCheckLoop
            }
        }
        return foundRole
    }

    @RequestMapping(value = "/users/suggestDashboardUsers/{query}", method = RequestMethod.GET, produces = "application/json")
    List<String> suggestExistingDashboardUsers(@PathVariable("query") String query,
                                               @RequestParam(required = false) boolean includeSelf) {
        List<User> matchingUsers = userRepo.getUserByUserIdOrPropWildcard(query, new PageRequest(0, 6))
        List<String> results = matchingUsers.collect { it.userId }
        if (!includeSelf) {
            String currentUserId = userInfoService.currentUser.username
            results = results.findAll { it != currentUserId }
        }
        return results.take(5)
    }

    @RequestMapping(value="/users/validExistingDashboardUserId/{userId}", method =  RequestMethod.GET, produces = "application/json")
    Boolean isValidExistingDashboardUserId(@PathVariable("userId") String userId){
        return userRepo.findByUserId(userId) != null
    }

    @RequestMapping(value = "/users/projects/{projectId}/suggestClientUsers/{query}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    List<String> suggestExistingClientUsersForProject(@PathVariable("projectId") String projectId, @PathVariable("query") String query) {
        return userAdminService.suggestUsersForProject(projectId, query, new PageRequest(0, 5))
    }

    @RequestMapping(value = "/users/suggestClientUsers/{query}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    List<String> suggestExistingClientUsers(@PathVariable("query") String query) {
        return userAdminService.suggestUsers(query, new PageRequest(0, 5))
    }

    @RequestMapping(value = "/users/projects/{projectId}/validExistingClientUserId/{userId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    Boolean isValidExistingClientUserIdForProject(@PathVariable("projectId") String projectId, @PathVariable("userId") String userId) {
        return userAdminService.isValidExistingUserIdForProject(projectId, userId)
    }

    @RequestMapping(value = "/users/validExistingClientUserId/{userId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    Boolean isValidExistingClientUserId(@PathVariable("userId") String userId) {
        return userAdminService.isValidExistingUserId(userId)
    }

}
