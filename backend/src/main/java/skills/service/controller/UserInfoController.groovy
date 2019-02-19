package skills.service.controller

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import skills.service.auth.AuthMode
import skills.service.auth.SkillsAuthorizationException

//import skills.service.auth.DistinguishedNameLoader
import skills.service.auth.UserInfo
import skills.service.auth.UserInfoService
import skills.storage.model.auth.User
import skills.storage.repos.UserRepo

@RestController
@RequestMapping("/app")
@Slf4j
class UserInfoController {

    @Autowired
    UserInfoService userInfoService

    @Autowired
    UserRepo userRepo

    @Value('#{securityConfig.authMode}}')
    AuthMode authMode = AuthMode.DEFAULT_AUTH_MODE

//    @Autowired
//    DistinguishedNameLoader suggestionLoader

    static class UserInfoRes {
        String userId
        String first
        String last
    }

    @RequestMapping(value = "/userInfo", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    def getUserInfo() {
        UserInfoRes res
        UserInfo currentUser = userInfoService.getCurrentUser()
        if (currentUser) {
            res  = new UserInfoRes(userId: currentUser.username, first: currentUser.firstName, last: currentUser.lastName)
        } else if (authMode == AuthMode.PKI) {
            throw new SkillsAuthorizationException('Unauthenticated user while using PKI Authorization Mode')
        }
        return res
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

    @RequestMapping(value = "/users/suggestDns/{dnPrefix}", method = RequestMethod.GET, produces = "application/json")
    List<String> suggestDns(@PathVariable("dnPrefix") String dnPrefix) {

        // TODO - fix me
//        return suggestionLoader.suggestDns(dnPrefix)
    }

    @RequestMapping(value="/users/validDn/{dn}", method =  RequestMethod.GET, produces = "application/json")
    Boolean isValidDn(@PathVariable("dn") String dn){
        // TODO - fix me
//        return suggestionLoader.isValidDn(dn)
    }

    @RequestMapping(value = "/users/suggestUsers/{query}", method = RequestMethod.GET, produces = "application/json")
    List<UserInfoRes> suggestExistngPortalUsers(@PathVariable("query") String query) {
        List<User> matchingUsers = userRepo.getUserByUserIdOrPropWildcard(query, new PageRequest(0, 10))
        List<UserInfoRes> results = matchingUsers.collect { new UserInfoRes(userId: it.userId, first: it.userProps.find {it.name == 'firstName'}.value, last: it.userProps.find {it.name == 'lastName'}.value) }

        return results
    }

    @RequestMapping(value="/users/validExistingUserId/{userId}", method =  RequestMethod.GET, produces = "application/json")
    Boolean isValidExistingPortalUserId(@PathVariable("userId") String userId){
        return userRepo.findByUserId(userId) != null
    }
}
