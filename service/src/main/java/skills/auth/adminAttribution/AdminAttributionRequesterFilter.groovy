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

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.web.filter.OncePerRequestFilter
import skills.auth.UserInfo
import skills.auth.UserInfoService

class AdminAttributionRequesterFilter extends OncePerRequestFilter {
    private final AdminAttributionProperties properties
    private final UserInfoService userInfoService
    private final RequestMatcher adminMatcher = PathPatternRequestMatcher.pathPattern('/admin/**')

    AdminAttributionRequesterFilter(AdminAttributionProperties properties, UserInfoService userInfoService) {
        this.properties = properties
        this.userInfoService = userInfoService
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.enabled || !adminMatcher.matches(request) || request.getHeader(properties.headerName) == null
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {
        // Enforce the allowlist before MVC, including unmatched and unscoped admin routes.
        Authentication authentication = SecurityContextHolder.context.authentication
        UserInfo requester = userInfoService.currentUser
        request.setAttribute(AdminAttributionInterceptor.REQUESTER_ATTRIBUTE, requester?.username ?: '-')
        if (!authentication?.authenticated || !requester || requester.proxied ||
                !properties.allowedRequesters.contains(requester.username.toLowerCase(Locale.ROOT))) {
            throw new AccessDeniedException('Requester is not authorized to provide admin attribution')
        }
        filterChain.doFilter(request, response)
    }
}
