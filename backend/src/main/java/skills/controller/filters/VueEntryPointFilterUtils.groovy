package skills.controller.filters

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class VueEntryPointFilterUtils {
    @Value('#{"${skills.vue.entry.backend.resources:/api,/admin,/app,/static,/clientDisplay,/favicon.ico,/skills.ico,/icons,/performLogin,/createAccount,/createRootAccount,/grantFirstRoot,/userExists,/oauth,/login,/logout,/bootstrap,/root}".split(",")}')
    private List<String> backendResources

    boolean isFrontendResource(String pathInfo) {
        return !isBackendResource(pathInfo)
    }

    boolean isBackendResource(String pathInfo) {
        return backendResources ? backendResources.find { String ignoreUrl ->
            return pathInfo.startsWith(ignoreUrl)
        } : false
    }
}
