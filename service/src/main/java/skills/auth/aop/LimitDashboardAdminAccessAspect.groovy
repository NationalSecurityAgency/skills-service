/**
 * Copyright 2024 SkillTree
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
package skills.auth.aop

import groovy.util.logging.Slf4j
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.Signature
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.support.MessageSourceAccessor
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.SpringSecurityMessageSource
import org.springframework.stereotype.Component
import skills.auth.UserInfo
import skills.auth.UserInfoService
import skills.auth.UserSkillsGrantedAuthority
import skills.storage.model.auth.RoleName

@Aspect
@Component
@Slf4j
@ConditionalOnProperty(
        name = "skills.config.ui.limitAdminAccess",
        havingValue = "true")
class LimitDashboardAdminAccessAspect {

    @Autowired
    private UserInfoService userInfoService;

    private MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();

    @Around(value = "@within(LimitDashboardAccess) && !@annotation(ExcludeFromLimitDashboardAccess)")
    Object authorizeAdmin(ProceedingJoinPoint joinPoint) throws Throwable {
        UserInfo userInfo = userInfoService.getCurrentUser()

        Collection<? extends GrantedAuthority> grantedAuthorities = userInfo.getAuthorities()
        GrantedAuthority foundAuthority = grantedAuthorities?.find({ it instanceof UserSkillsGrantedAuthority && hasAdminAccessRole(it) })
        if (foundAuthority) {
            return joinPoint.proceed();
        }

        String uid = userInfo?.userDn ?: userInfo?.username

        Signature signature = joinPoint.getSignature();
        if (log.isDebugEnabled()) {
            log.trace("Access is denied to [{}] for userName=[{}]", signature.name, uid);
        }
        throw new AccessDeniedException(messages.getMessage(
                "AbstractAccessDecisionManager.accessDenied", "Access is denied to [${signature.name}] for user=[${uid}]"));
    }

    private boolean hasAdminAccessRole(UserSkillsGrantedAuthority authority) {
        RoleName userRole = authority.getRole().roleName
        return userRole == RoleName.ROLE_DASHBOARD_ADMIN_ACCESS || userRole == RoleName.ROLE_SUPER_DUPER_USER
    }
}
