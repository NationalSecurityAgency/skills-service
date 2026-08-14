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
package skills.logging

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

import java.security.cert.X509Certificate

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
// Safely binds user IDs to request contexts whenever access logs are active (file or stdout)
@ConditionalOnProperty(name = "server.tomcat.accesslog.enabled", havingValue = "true", matchIfMissing = false)
class AccessLogIdentityFilter extends OncePerRequestFilter {

    static final String CERT_HEADER = 'jakarta.servlet.request.X509Certificate'

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // 1. Resolve identity: DN in pki mode, userId in pass-mode
        String identity = getSubjectDN(request) ?: SecurityContextHolder.getContext()?.getAuthentication()?.getName() ?: "-"

        // 2. Bind it to the request attribute for Tomcat's AccessLogValve to see
        request.setAttribute("callerUserId", identity)

        filterChain.doFilter(request, response)
    }

    private String getSubjectDN(HttpServletRequest request) {
        def certificateAttr = request.getAttribute(CERT_HEADER)
        if (certificateAttr instanceof X509Certificate) {
            return certificateAttr?.getSubjectX500Principal()?.name
        } else if (certificateAttr instanceof X509Certificate[]) {
            // use the first one
            if (certificateAttr.length > 0) {
                return certificateAttr[0]?.getSubjectX500Principal()?.name
            }
        }
        return null
    }
}