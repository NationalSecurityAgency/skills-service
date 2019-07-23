package skills.service.profile

import callStack.profiler.CProf
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.CodeSignature
import org.springframework.stereotype.Component

@Aspect
@Component
@Slf4j
@CompileStatic
class CallStackProfAspect {

    @Around("@within(EnableCallStackProf) || @annotation(EnableCallStackProf)")
    Object profile(ProceedingJoinPoint joinPoint) {
        Object retVal
        CProf.clear()
        String profileName = getProfileName(joinPoint)
        CProf.start(profileName)
        try {
            retVal = joinPoint.proceed();
        } finally {
            CProf.stop(profileName)
        }

        log.info("Service Profiling:\n{}", CProf.prettyPrint())
        return retVal
    }

    private String getProfileName(ProceedingJoinPoint joinPoint) {
        StringBuilder builder = new StringBuilder()
        CodeSignature codeSignature = (CodeSignature)joinPoint.getSignature()
        builder.append(codeSignature.name)
        builder.append("(")

        String [] parameterNames = codeSignature.getParameterNames()
        for (int i = 0; i < codeSignature.parameterNames.length; i++) {
            if (i > 0) {
                builder.append(",")
            }
            String key = parameterNames[i]
            String value = joinPoint.getArgs()[i]
            builder.append(key)
            builder.append("=")
            builder.append(value)
        }

        builder.append(")")
        return  builder.toString()
    }

}
