package skills.service.auth

import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.WebAttributes
import org.springframework.stereotype.Component
import org.springframework.web.cors.CorsUtils
import org.springframework.web.filter.GenericFilterBean
import skills.storage.model.auth.RoleName

import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest

@CompileStatic
//@Component
class CorsPreAuthenticatedProcessingFilter extends GenericFilterBean {

    static final String CORS_ONLY_RESOURCE_MESSAGE = 'This resource is only available Cross-origin resource sharings (CORS) compliant request'

    @Autowired
    UserInfoService userInfoService

    @Autowired
    SkillsCorsConfigurationSource corsConfigurationSource

    @Override
    void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request
        if (canAccess(httpServletRequest)) {
            chain.doFilter(request, response)
        } else {
            CorsOnlyResourceException corsOnlyResourceException = new CorsOnlyResourceException(CORS_ONLY_RESOURCE_MESSAGE)
            unsuccessfulAuthentication(httpServletRequest, corsOnlyResourceException)
        }
    }

    private boolean canAccess(HttpServletRequest servletRequest) {
        boolean canAccess = false
        if (hasRole(RoleName.ROLE_PROJECT_ADMIN) || hasRole(RoleName.ROLE_SUPER_DUPER_USER)) {
            canAccess = true
        } else if (!corsConfigurationSource.isSkillsCorsResource(servletRequest)) {
            canAccess = true
        } else if (isCorsCompliantRequest(servletRequest)) {
            canAccess = true
        }
        return canAccess
    }

    private boolean hasRole(RoleName role) {
        boolean hasRole = false
        UserInfo currentUser = userInfoService.getCurrentUser()
        if (currentUser) {
            hasRole = currentUser.authorities.find { it.authority.toLowerCase() == role.name().toLowerCase() }
        }
        return hasRole
    }

    private boolean isCorsCompliantRequest(HttpServletRequest request) {
        return CorsUtils.isCorsRequest(request) || CorsUtils.isPreFlightRequest(request)
    }

    /**
     * Taken from AbstractPreAuthenticatedProcessingFilter
     *
     * Ensures the authentication object in the secure context is set to null when
     * authentication fails.
     * <p>
     * Caches the failure exception as a request attribute
     */
    private void unsuccessfulAuthentication(HttpServletRequest request, AuthenticationException failed) throws IOException, ServletException {
        SecurityContextHolder.clearContext()

        if (logger.isDebugEnabled()) {
            logger.debug("Cleared security context due to exception", failed)
        }
        request.setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, failed)
        throw failed
    }

    static class CorsOnlyResourceException extends AuthenticationException {
        CorsOnlyResourceException(String msg) {
            super(msg)
        }
    }
}
