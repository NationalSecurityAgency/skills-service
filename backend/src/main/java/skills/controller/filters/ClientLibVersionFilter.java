package skills.controller.filters;


import groovy.util.logging.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

@Component
@Order(SecurityProperties.DEFAULT_FILTER_ORDER - 2)
@Slf4j
public class ClientLibVersionFilter extends OncePerRequestFilter {

    @Value("${skills.clientLibVersion}")
    String clientLibVersion;

    private static String HEADER_SKILLS_CLIENT_LIB_VERSION = "Skills-Client-Lib-Version".toLowerCase();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        response.addHeader(HEADER_SKILLS_CLIENT_LIB_VERSION, clientLibVersion);
        response.addHeader("Access-Control-Expose-Headers", HEADER_SKILLS_CLIENT_LIB_VERSION);

        filterChain.doFilter(request, response);
    }

}
