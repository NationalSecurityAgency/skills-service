package skills.utils

import groovy.util.logging.Slf4j
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.reflect.CodeSignature
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.support.MessageSourceAccessor
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.SpringSecurityMessageSource
import org.springframework.stereotype.Component
import skills.auth.UserInfo
import skills.auth.UserInfoService
import skills.auth.UserSkillsGrantedAuthority
import skills.storage.model.auth.RoleName

@Aspect
@Component
@Slf4j
class ArtificialDelayAspect {

    @Value('#{"${skills.artificialDelay.enabled:false}"}')
    private Boolean enabled;


    @Before(value='@annotation(artificialDelay)')
    def introduceDelay(JoinPoint jp, ArtificialDelay artificialDelay) {
        if(enabled) {
            Thread.sleep(artificialDelay.delay())
        }
    }

}
