package skills.utils

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@CompileStatic
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
