package skills.controller.filters

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class VueEntryPointFilterUtils {
    private final List<String> backendResources =
            Collections.unmodifiableList("/api,/admin,/app,/static,/clientDisplay,/favicon.ico,/skills.ico,/icons,/performLogin,/createAccount,/createRootAccount,/grantFirstRoot,/userExists,/oauth,/login,/logout,/bootstrap,/root,/supervisor,/public,/metrics,/skills-websocket".split(",").toList())

    boolean isFrontendResource(String pathInfo) {
        return !isBackendResource(pathInfo)
    }

    boolean isBackendResource(String pathInfo) {
        return backendResources ? backendResources.find { String ignoreUrl ->
            return pathInfo.startsWith(ignoreUrl)
        } : false
    }
}
