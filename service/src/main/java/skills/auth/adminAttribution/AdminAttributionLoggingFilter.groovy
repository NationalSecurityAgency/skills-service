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

import groovy.util.logging.Slf4j
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.web.filter.OncePerRequestFilter

@Slf4j
class AdminAttributionLoggingFilter extends OncePerRequestFilter {
    private final AdminAttributionProperties properties
    private final RequestMatcher adminMatcher = PathPatternRequestMatcher.pathPattern('/admin/**')

    AdminAttributionLoggingFilter(AdminAttributionProperties properties) {
        this.properties = properties
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.enabled || !adminMatcher.matches(request) || request.getHeader(properties.headerName) == null
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {
        boolean failed = false
        try {
            filterChain.doFilter(request, response)
        } catch (Exception exception) {
            failed = true
            throw exception
        } finally {
            AdminAttribution attribution = request.getAttribute(AdminAttribution.REQUEST_ATTRIBUTE) as AdminAttribution
            Authentication authentication = SecurityContextHolder.context.authentication
            String requester = request.getAttribute(AdminAttributionInterceptor.REQUESTER_ATTRIBUTE) as String
            requester = requester ?: (authentication?.authenticated && authentication.name != 'anonymousUser' ? authentication.name : null)
            int status = failed ? HttpServletResponse.SC_INTERNAL_SERVER_ERROR : response.status
            String outcome = status >= 400 ? 'rejected' : (attribution ? 'applied' : 'ignored_unscoped')
            log.info('Admin attribution request userId=[{}] requestingSystemUserId=[{}] claimedActingUserId=[{}] method=[{}] path=[{}] resource=[{}] attributionStatus=[{}] httpStatus=[{}]',
                    safe(attribution?.actingUserId ?: requester), safe(attribution?.requestingSystemUserId ?: requester),
                    safe(attribution ? null : request.getHeader(properties.headerName)), safe(request.method), safe(request.requestURI),
                    safe(request.getAttribute(AdminAttributionInterceptor.RESOURCE_ATTRIBUTE)), outcome, status)
        }
    }

    private static String safe(Object value) {
        String text = value?.toString()?.replaceAll(/[\p{Cntrl}\[\]]/, '_') ?: '-'
        return text.take(512)
    }
}
