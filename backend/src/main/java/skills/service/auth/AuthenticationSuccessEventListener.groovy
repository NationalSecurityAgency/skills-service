package skills.service.auth

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationListener
import org.springframework.security.authentication.event.AuthenticationSuccessEvent
import org.springframework.security.web.authentication.WebAuthenticationDetails
import org.springframework.stereotype.Component

@Component
class AuthenticationSuccessEventListener implements ApplicationListener<AuthenticationSuccessEvent> {

//    @Autowired
//    private LoginAttemptService loginAttemptService;

    public void onApplicationEvent(AuthenticationSuccessEvent e) {
//        WebAuthenticationDetails auth = (WebAuthenticationDetails)
//        e.getAuthentication().getDetails();

        println "\n\nAuthentication SUCCSES!!!!!\n\n"

//        loginAttemptService.loginSucceeded(auth.getRemoteAddress());
    }
}
