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
package skills.auth.requestMatchers

import jakarta.servlet.http.HttpServletRequest
import org.apache.commons.lang3.StringUtils
import org.springframework.security.web.util.matcher.RequestMatcher

class ContainsStrRequestMatcher implements RequestMatcher {
    String matchInternal

    ContainsStrRequestMatcher(String match) {
        if (StringUtils.isBlank(match)) {
            throw new IllegalArgumentException('match must not be blank')
        }
        matchInternal = match.toLowerCase()
    }

    @Override
    boolean matches(HttpServletRequest request) {
        String requestUri = request.getRequestURI()?.toLowerCase() ?: ''
        return requestUri.contains(matchInternal)
    }
}