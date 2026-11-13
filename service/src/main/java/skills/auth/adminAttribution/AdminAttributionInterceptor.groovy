/**
 * Copyright 2026 SkillTree
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
package skills.auth.adminAttribution

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authorization.AuthorizationResult
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.HandlerMapping
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import skills.auth.AuthUtils
import skills.auth.UserAuthService
import skills.auth.UserInfo
import skills.auth.UserInfoService
import skills.auth.userCommunity.UserCommunityAuthorizationManager
import skills.controller.exceptions.SkillsValidator

@Configuration
class AdminAttributionInterceptor implements HandlerInterceptor, WebMvcConfigurer {
    static final String RESOURCE_ATTRIBUTE = 'adminAttributionResource'
    static final String REQUESTER_ATTRIBUTE = 'adminAttributionRequester'
    static final int MAX_USER_ID_LENGTH = 255

    @Autowired
    AdminAttributionProperties properties

    @Autowired
    UserInfoService userInfoService

    @Autowired
    UserAuthService userAuthService

    @Autowired
    UserCommunityAuthorizationManager userCommunityAuthorizationManager

    @Override
    void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this).addPathPatterns('/admin', '/admin/**')
    }

    @Override
    boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!properties.enabled || request.getHeader(properties.headerName) == null) {
            return true
        }

        // The security-chain filter already enforced the allowlist, including unscoped routes.
        UserInfo requester = userInfoService.currentUser

        Map<String, String> variables = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) as Map<String, String>
        String projectId = variables?.get('projectId') ?: AuthUtils.getProjectIdForProtection(request)
        String quizId = variables?.get('quizId')
        // Ambiguous and non-project/quiz routes keep their existing attribution.
        if ((!projectId && !quizId) || (projectId && quizId)) {
            return true
        }
        request.setAttribute(RESOURCE_ATTRIBUTE, projectId ? "project:${projectId}" : "quiz:${quizId}")

        List<String> values = Collections.list(request.getHeaders(properties.headerName))
        SkillsValidator.isTrue(values.size() == 1, 'Admin attribution header must contain exactly one user ID', projectId)
        String actingUserId = values.first().trim()
        SkillsValidator.isTrue(actingUserId.length() <= MAX_USER_ID_LENGTH && !(actingUserId =~ /[\p{Cntrl},]/),
                'Admin attribution header must contain a single valid user ID of at most 255 characters', projectId)
        SkillsValidator.isNotBlank(actingUserId, 'Admin attribution user ID', projectId)

        if (!isAdmin(requester, projectId, quizId)) {
            throw new AccessDeniedException('Requester must be an administrator of the target resource')
        }
        // Read-only lookup: header claims must never create users or refresh the security context.
        UserInfo actingUser = userAuthService.loadByUserId(actingUserId.toLowerCase(Locale.ROOT))
        if (!actingUser || !isAdmin(actingUser, projectId, quizId)) {
            throw new AccessDeniedException('Attributed user must be an administrator of the target resource')
        }
        if (!actingUser.enabled || !actingUser.accountNonExpired || !actingUser.accountNonLocked || !actingUser.credentialsNonExpired) {
            throw new AccessDeniedException('Attributed user account is not eligible to access the target resource')
        }
        // Reuse the community policy with the resolved target (including related groups/badges).
        // No security-context replacement or credentials are needed, and root cannot bypass protection.
        AuthorizationResult communityDecision = userCommunityAuthorizationManager.authorizeUserForResources(
                actingUser.username, projectId, quizId, variables?.get('adminGroupId'), variables?.get('badgeId'))
        if (communityDecision != null && !communityDecision.granted) {
            throw new AccessDeniedException('Attributed user is not eligible to access the protected community')
        }
        request.setAttribute(AdminAttribution.REQUEST_ATTRIBUTE, new AdminAttribution(
                actingUserId: actingUser.username,
                requestingSystemUserId: requester.username,
                projectId: projectId,
                quizId: quizId))
        return true
    }

    private boolean isAdmin(UserInfo user, String projectId, String quizId) {
        return projectId ? userAuthService.isUserProjectAdmin(user, projectId) : userAuthService.isUserQuizAdmin(user, quizId)
    }
}
