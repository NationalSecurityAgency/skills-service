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
package skills.auth

import groovy.util.logging.Slf4j

import javax.servlet.http.HttpServletRequest

@Slf4j
class AuthUtils {
    static final String PROJECT_ID_PATTERN = /\/\S+?\/projects\/([^\/]+).*$/

    static String getProjectIdFromRequest(HttpServletRequest servletRequest) {
        String projectId
        if (servletRequest) {
            String servletPath = servletRequest.getServletPath()
            def matcher = (servletPath =~ PROJECT_ID_PATTERN)
            if (matcher.matches()) {
                if (matcher.hasGroup()) {
                    projectId = matcher.group(1)
                } else {
                    log.warn("no projectId found for endpoint [$servletPath]?")
                }
            }
        }
        return projectId
    }
}
