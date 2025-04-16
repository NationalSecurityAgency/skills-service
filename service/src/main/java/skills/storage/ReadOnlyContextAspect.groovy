/**
 * Copyright 2025 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package skills.storage

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam

import java.lang.reflect.Method
import java.lang.reflect.Parameter

@Aspect
@Component
@Slf4j
@CompileStatic
class ReadOnlyContextAspect {

    @Around("@annotation(org.springframework.web.bind.annotation.RequestMapping)")
    Object readOnlyContextForHttpGetMethods(ProceedingJoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod()
        RequestMapping requestMapping = method.getAnnotation(RequestMapping.class)
        RequestMethod[] endpointsMethods = requestMapping.method()
        boolean isReadOnlyEligible = ReadOnlyEligible.isReadOnlyEligible()
        if (!isReadOnlyEligible) {
            return joinPoint.proceed();
        }

        boolean isGetMethod = endpointsMethods && endpointsMethods.size() == 1 && endpointsMethods[0] == RequestMethod.GET
        if (!isGetMethod) {
            return joinPoint.proceed();
        }

        boolean hasUserIdParam = hasUserIdRequestParam(method, joinPoint.args)
        if (hasUserIdParam) {
            return joinPoint.proceed();
        }

        log.debug('Setting read only context for [{}] method', method.name)
        ReadOnlyDataSourceContext.start()
        try {
            return joinPoint.proceed();
        } finally {
            ReadOnlyDataSourceContext.end()
        }
    }

    private static boolean hasUserIdRequestParam(Method method, Object [] args) {
        Parameter[] parameters = method.getParameters()
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i]
            Object arg = args[i]
            RequestParam requestParam = parameter.getAnnotation(RequestParam.class)
            boolean isUserIdParam = requestParam && requestParam.name() == 'userId'
            if (isUserIdParam) {
                boolean isUserIdIsSupplied = arg instanceof String && StringUtils.isNotBlank((String) arg)
                return isUserIdIsSupplied
            }
        }
        return false
    }
}
