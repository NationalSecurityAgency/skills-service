package skills.service.controller.filters;

import groovy.util.logging.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Component
@Order(SecurityProperties.DEFAULT_FILTER_ORDER-1)
@Slf4j
public class VueEntryPointFilter implements Filter {

    @Autowired
    private VueEntryPointFilterUtils vueEntryPointFilterUtils;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        String requestUri = httpServletRequest.getRequestURI();
        if (vueEntryPointFilterUtils.isFrontendResource(requestUri)) {
            // frontend resource, forward to the UI for vue-js to handle
            httpServletRequest.getRequestDispatcher("/").forward(request, response);
        } else {
            // backend resource, continue with the filter chain
            filterChain.doFilter(request, response);
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException { }

    @Override
    public void destroy() { }

}
