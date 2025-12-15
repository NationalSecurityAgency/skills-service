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
package skills.auth.openai


import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.annotation.Order
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.authorization.AuthorizationManager
import org.springframework.security.core.Authentication
import org.springframework.security.web.access.intercept.RequestAuthorizationContext
import org.springframework.stereotype.Component
import skills.auth.UserInfoService
import skills.storage.model.auth.RoleName
import skills.storage.model.auth.UserRole
import skills.storage.repos.UserRoleRepo

import java.util.function.Supplier

@CompileStatic
@Slf4j
@Component
@Order(101)
class OpenAIAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    @Autowired
    UserInfoService userInfoService

    @Autowired
    UserRoleRepo userRoleRepo

    @Override
    AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext authorizationContext) {
        HttpServletRequest request = authorizationContext?.getRequest()
        log.trace("Auth check [{}], userInfoService?.currentUser [{}]", request?.getRequestURI(), userInfoService?.currentUser)

        List<UserRole> userRoles = userRoleRepo.findAllByUserId(userInfoService?.currentUser?.username)
        log.trace("Username: [{}], roles: [{}]", userInfoService?.currentUser?.username, userRoles)

        List<RoleName> mustHaveOneOfThese = [RoleName.ROLE_SUPER_DUPER_USER, RoleName.ROLE_PROJECT_ADMIN, RoleName.ROLE_QUIZ_ADMIN]
        List<RoleName> userRoleNames = userRoles.collect { it.roleName }

        boolean hasOneOfThese = mustHaveOneOfThese.any { userRoleNames.contains(it) }
        return new AuthorizationDecision(hasOneOfThese)
    }
}
