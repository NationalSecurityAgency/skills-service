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
package skills.controller.filters

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class VueEntryPointFilterUtils {
    private final List<String> backendResources =
            Collections.unmodifiableList("/api,/admin,/app,/static,/clientDisplay,/favicon.ico,/skills.ico,/icons,/performLogin,/createAccount,/createRootAccount,/grantFirstRoot,/userExists,/oauth,/login,/logout,/root,/supervisor,/public,/skills-websocket,/resetPassword,/performPasswordReset,/metrics/global".split(",").toList())

    boolean isFrontendResource(String pathInfo) {
        return !isBackendResource(pathInfo)
    }

    boolean isBackendResource(String pathInfo) {
        return backendResources ? backendResources.find { String ignoreUrl ->
            return pathInfo.startsWith(ignoreUrl)
        } : false
    }
}
