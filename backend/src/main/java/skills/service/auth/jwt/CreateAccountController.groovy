package skills.service.auth.jwt

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Conditional
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import skills.service.auth.SecurityConfiguration
import skills.service.auth.UserAuthService
import skills.service.auth.UserInfo

import javax.servlet.http.HttpServletResponse

@Conditional(SecurityConfiguration.JwtCondition)
@RestController
@RequestMapping("/")
@Slf4j
class CreateAccountController {

    @Autowired
    UserAuthService userAuthService

    @Autowired
    PasswordEncoder passwordEncoder

    @Autowired
    JwtHelper jwtHelper

    @PutMapping("createAccount")
    void createAppUser(@RequestBody UserInfo userInfo, HttpServletResponse response) {
        userInfo.password = passwordEncoder.encode(userInfo.password)
        userInfo.username = userInfo.email
        userInfo = userAuthService.createUser(userInfo)
        jwtHelper.addToken(response, userInfo)
    }
}
