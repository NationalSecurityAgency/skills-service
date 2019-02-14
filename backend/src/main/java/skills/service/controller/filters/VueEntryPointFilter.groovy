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

    @Value('#{"${skills.vue.entry.blacklist:/api,/admin,/app,/server,/static,/favicon.ico,/icons,/performLogin,/createAccount,/oauth,/test}".split(",")}')
    private List<String> urlBlacklist

    @Override
    void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request
        String requestUri = httpServletRequest.getRequestURI()
        if(!isBlacklistUrl(requestUri) ) {
            httpServletRequest.getRequestDispatcher("/").forward(request,response)
        } else {
            filterChain.doFilter(request,response)
        }
    }

    boolean isBlacklistUrl(String pathInfo) {
        return urlBlacklist ? urlBlacklist.find { String ignoreUrl ->
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
