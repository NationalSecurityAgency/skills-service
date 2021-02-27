/**
 * Copyright 2020 SkillTree
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
package skills.auth.aop;

import callStack.profiler.Profile;
import groovy.util.logging.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.CodeSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.stereotype.Component;
import skills.auth.UserInfo;
import skills.auth.UserInfoService;
import skills.auth.UserSkillsGrantedAuthority;
import skills.controller.request.model.SkillEventRequest;
import skills.storage.model.auth.RoleName;

import java.util.Collection;

@Aspect
@Component
@Slf4j
class AuthorizationAspect {
    private Logger log = LoggerFactory.getLogger(AuthorizationAspect.class);
    private static final String USER_ID_PARAM = "userIdParam";
    private static final String SKILL_EVENT_REQUEST_PARAM = "skillEventRequest";

    private MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    skills.auth.UserAuthService userAuthService;

    @Around(value = "@within(AdminUsersOnlyWhenUserIdSupplied) || @annotation(AdminUsersOnlyWhenUserIdSupplied)")
    Object authorizeAdmin(ProceedingJoinPoint joinPoint) throws Throwable {
        String userIdProvided = getUserIdParam(joinPoint);
        if (StringUtils.isNotBlank(userIdProvided)) {
            UserInfo userInfo = userInfoService.getCurrentUser();
            checkAdminAccess(userInfo);
        }
        return joinPoint.proceed();
    }

    @Profile
    private void checkAdminAccess(UserInfo userInfo) {
        Collection<GrantedAuthority> authorities = !userInfo.isProxied() ? userInfo.getAuthorities() : userAuthService.loadAuthorities(userInfo.getUsername());
        boolean foundAdmin = false;
        if (authorities != null) {
            for (GrantedAuthority grantedAuthority : authorities) {
                UserSkillsGrantedAuthority userSkillsGrantedAuthority = (UserSkillsGrantedAuthority) grantedAuthority;
                RoleName roleName = userSkillsGrantedAuthority.getRole().getRoleName();
                if (roleName.equals(RoleName.ROLE_PROJECT_ADMIN) || roleName.equals(RoleName.ROLE_SUPER_DUPER_USER)) {
                    foundAdmin = true;
                    break;
                }
            }
        }

        if (!foundAdmin) {
            log.trace("Access is denied for userName=[{}]", userInfo.getUsername());
            throw new AccessDeniedException(messages.getMessage(
                    "AbstractAccessDecisionManager.accessDenied", "Access is denied"));
        }
    }

    @Profile
    private String getUserIdParam(ProceedingJoinPoint joinPoint) {
        String userId = null;
        validateJoinPoint(joinPoint);
        CodeSignature codeSignature = (CodeSignature) joinPoint.getSignature();
        String[] parameterNames = codeSignature.getParameterNames();
        paramLoop:
        for (int i = 0; i < parameterNames.length; i++) {
            String paramName = parameterNames[i];
            if (USER_ID_PARAM.equalsIgnoreCase(paramName)) {
                Object[] args = joinPoint.getArgs();
                userId = (String) args[i];
                break paramLoop;
            } else if (SKILL_EVENT_REQUEST_PARAM.equalsIgnoreCase(paramName)) {
                Object[] args = joinPoint.getArgs();
                if (args != null && args.length > i && args[i] != null) {
                    SkillEventRequest request = (SkillEventRequest) args[i];
                    userId = request.getUserId();
                }
            }
        }
        return userId;
    }

    /**
     * This looks like a Spring Framework bug as a NullPointerException
     * was observed on joinPoint.getSignature() on several occasions now.
     * We've tried to replicate the issue but without any luck.
     * This check is added in hopes to shed some light if the issue happens again in the future.
     */
    private void validateJoinPoint(ProceedingJoinPoint joinPoint) {
        if (joinPoint == null) {
            String username = "";
            if (userInfoService != null){
                UserInfo userInfo = userInfoService.getCurrentUser();
                if (userInfo != null){
                    username = userInfo.getUsername();
                }
            }
            throw new IllegalArgumentException("Provided ProceedingJoinPoint was null. How can that be??? Username=[" + username + "]");
        }
    }
}
