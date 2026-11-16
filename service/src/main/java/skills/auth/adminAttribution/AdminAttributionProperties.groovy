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

import jakarta.annotation.PostConstruct
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = 'skills.authorization.admin-attribution')
class AdminAttributionProperties {
    // Credentials, session/CSRF tokens, and authentication challenges are not attribution headers.
    private static final Set<String> RESERVED_HEADER_NAMES = [
            'authorization', 'proxy-authorization', 'cookie', 'set-cookie',
            'www-authenticate', 'proxy-authenticate', 'authentication-info', 'proxy-authentication-info',
            'x-api-key', 'api-key', 'x-goog-api-key', 'x-amz-security-token',
            'x-auth-token', 'x-access-token', 'x-session-id', 'x-session-token',
            'x-xsrf-token', 'x-csrf-token',
    ] as Set<String>

    boolean enabled = false
    String headerName = 'X-SkillTree-Acting-User'
    Set<String> allowedRequesters = [] as Set

    @PostConstruct
    void validate() {
        if (enabled && (!headerName || !(headerName ==~ /[!#$%&'*+.^_`|~0-9A-Za-z-]+/))) {
            throw new IllegalArgumentException('skills.authorization.adminAttribution.headerName must be a valid HTTP header name')
        }
        if (headerName && RESERVED_HEADER_NAMES.contains(headerName.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException('skills.authorization.adminAttribution.headerName must not be a sensitive or reserved header name')
        }
        allowedRequesters = (allowedRequesters ?: []).collect { String userId ->
            if (!userId?.trim()) {
                throw new IllegalArgumentException('skills.authorization.adminAttribution.allowedRequesters must not contain blank user IDs')
            }
            userId.trim().toLowerCase(Locale.ROOT)
        } as Set<String>
    }
}
