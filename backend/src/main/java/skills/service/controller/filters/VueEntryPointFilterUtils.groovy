package skills.service.controller.filters

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class VueEntryPointFilterUtils {
    @Value('#{"${skills.vue.entry.backend.resources:/api,/admin,/app,/server,/static,/favicon.ico,/icons,/performLogin,/createAccount,/createRootAccount,/oauth,/logout,/bootstrap}".split(",")}')
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
