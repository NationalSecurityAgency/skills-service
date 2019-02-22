package skills.service.controller.filters

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

import javax.servlet.*
import javax.servlet.http.HttpServletRequest

@CompileStatic
@Component
@Order(1)
@Slf4j
class VueEntryPointFilter implements Filter {

    @Value('#{"${skills.vue.entry.backend.resources:/api,/admin,/app,/server,/static,/favicon.ico,/icons,/performLogin,/createAccount,/oauth,/logout}".split(",")}')
    private List<String> backendResources

    @Override
    void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request
        String requestUri = httpServletRequest.getRequestURI()
        if(isFrontendResource(requestUri) ) {
            // frontend resource, forward to the UI for vue-js to handle
            httpServletRequest.getRequestDispatcher("/").forward(request,response)
        } else {
            // backend resource, continue with the filter chain
            filterChain.doFilter(request,response)
        }
    }

    boolean isFrontendResource(String pathInfo) {
        return !isBackendResource(pathInfo)
    }

    boolean isBackendResource(String pathInfo) {
        return backendResources ? backendResources.find { String ignoreUrl ->
            return pathInfo.startsWith(ignoreUrl)
        } : false
    }

    @Override
    void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    void destroy() {
    }

}
