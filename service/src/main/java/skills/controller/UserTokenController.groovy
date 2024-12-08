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
package skills.controller

import groovy.util.logging.Slf4j
import jakarta.annotation.PostConstruct
import jakarta.servlet.*
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpSession
import jakarta.servlet.http.HttpUpgradeHandler
import jakarta.servlet.http.Part
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Conditional
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.web.OAuth2TokenEndpointFilter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.bind.annotation.*
import skills.auth.SecurityMode
import skills.auth.UserInfoService
import skills.auth.form.oauth2.AuthorizationServerConfig
import skills.services.inception.InceptionProjectService

import java.security.Principal

@Conditional(SecurityMode.FormOrSAML2Auth)
@RestController
@skills.profile.EnableCallStackProf
@Slf4j
class UserTokenController {

    @Autowired
    @Qualifier('authServerSecurityFilterChain')
    SecurityFilterChain authServerSecurityFilterChain

    @Autowired
    RegisteredClientRepository registeredClientRepository

    @Autowired
    InMemoryOAuth2AuthorizedClientService authorizedClientService

    @Autowired
    private UserInfoService userInfoService

    private Map<String, TokenFilterChain> tokenFilterChainMap = [:]

    public static final String INCEPTION_USER_TOKEN_ENDPOINT = '/app/projects/Inception/users/{userId}/token'
    public static final String CURRENT_USER_TOKEN_ENDPOINT = '/api/projects/{projectId}/token'
    public static final String PROJECT_USER_TOKEN_ENDPOINT = '/admin/projects/{projectId}/token/{userId}'

    @PostConstruct
    void init() {
        List<String> tokenEndpoints = [INCEPTION_USER_TOKEN_ENDPOINT, CURRENT_USER_TOKEN_ENDPOINT, PROJECT_USER_TOKEN_ENDPOINT]
        tokenEndpoints.each { endpoint ->
            tokenFilterChainMap.put(endpoint, getTokenEndpointFilterChain(endpoint))
        }
    }

    private TokenFilterChain getTokenEndpointFilterChain(String endpoint) {
        def authServerContextFilter = authServerSecurityFilterChain.getFilters().find { it.class.simpleName == 'AuthorizationServerContextFilter' }
        OAuth2TokenEndpointFilter existingTokenEndpointFilter = authServerSecurityFilterChain.getFilters().find { it.class == OAuth2TokenEndpointFilter }
        OAuth2TokenEndpointFilter currentTokenEndpointFilter = new OAuth2TokenEndpointFilter(existingTokenEndpointFilter.authenticationManager, endpoint)
        return new TokenFilterChain([authServerContextFilter, currentTokenEndpointFilter])
    }

    private void setOAuth2ClientAuthenticationToken(String clientId) {
        RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId)
        assert registeredClient
        OAuth2ClientAuthenticationToken clientAuth = new OAuth2ClientAuthenticationToken(registeredClient, ClientAuthenticationMethod.CLIENT_SECRET_BASIC, registeredClient.clientSecret)
        clientAuth.authenticated = true
        SecurityContextHolder.clearContext()
        SecurityContextHolder.getContext().setAuthentication(clientAuth)
    }

    /**
     * token for inception
     * @param userId
     * @return
     */
    @RequestMapping(value = INCEPTION_USER_TOKEN_ENDPOINT, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    void getInceptionUserToken(HttpServletRequest request, HttpServletResponse response, @PathVariable("userId") String userId) {
        setOAuth2ClientAuthenticationToken(InceptionProjectService.inceptionProjectId)
        tokenFilterChainMap.get(INCEPTION_USER_TOKEN_ENDPOINT).init().doFilter(new TokenServletRequestWrapper(request, userId), response)
    }

    /**
     * token for current user already authenticated FORM auth (via third party OAuth2 provider (eg, google, gitlab, etc.),
     * or username/password)
     * @param projectId
     * @return
     */
    @RequestMapping(value = CURRENT_USER_TOKEN_ENDPOINT, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @CrossOrigin(allowCredentials = 'true', originPatterns = ['*'])
    void getSelfUserToken(HttpServletRequest request, HttpServletResponse response, @PathVariable("projectId") String projectId) {
        Object authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId
        if (authentication.credentials instanceof OAuth2AuthenticationToken && authentication.isAuthenticated()) {
            OAuth2AuthenticationToken oAuth2AuthenticationToken = authentication.credentials
            String oauthProvider = oAuth2AuthenticationToken.authorizedClientRegistrationId
            userId = authentication.name
            log.debug("Creating self-proxy OAuth Token for current user [{}] and project [{}], authenticated via [{}] OAuth provider", userId, projectId, oauthProvider)
        } else {
            userId = userInfoService.currentUserId
            log.debug("Creating self-proxy OAuth Token for current user [{}] and project [{}]", userId, projectId)
        }
        setOAuth2ClientAuthenticationToken(projectId)
        tokenFilterChainMap.get(CURRENT_USER_TOKEN_ENDPOINT).init().doFilter(new TokenServletRequestWrapper(request, userId), response)
    }

    /**
     * utilized by client-display within a project that previews that project's points
     * @param projectId
     * @param userId
     * @return
     */
    @RequestMapping(value = PROJECT_USER_TOKEN_ENDPOINT, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    void getUserToken(HttpServletRequest request, HttpServletResponse response, @PathVariable("projectId") String projectId, @PathVariable("userId") String userId) {
        setOAuth2ClientAuthenticationToken(projectId)
        tokenFilterChainMap.get(PROJECT_USER_TOKEN_ENDPOINT).init().doFilter(new TokenServletRequestWrapper(request, userId), response)
    }

    static class TokenFilterChain implements FilterChain {
        private int currentPosition = 0
        List<Filter> filters

        TokenFilterChain(List<Filter> filters) {
            this.filters = filters
        }

        TokenFilterChain init() {
            this.currentPosition = 0
            return this
        }

        @Override
        void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
            if (currentPosition < filters.size()) {
                currentPosition++
                filters.get(currentPosition-1).doFilter(servletRequest, servletResponse, this)
            }
        }
    }

    static class TokenServletRequestWrapper extends ServletRequestWrapper implements HttpServletRequest {
        final String proxyUser

        @Delegate
        final HttpServletRequest request

        TokenServletRequestWrapper(HttpServletRequest request, String proxyUser) {
            super(request)
            this.request = request
            this.proxyUser = proxyUser
        }

        @Override
        String getParameter(String name) {
            return getParameterMap().get(name)?.first()
        }

        @Override
        Map<String, String[]> getParameterMap() {
            Map<String, String[]> parameterMap = [:]
            parameterMap.put(OAuth2ParameterNames.GRANT_TYPE, [AuthorizationGrantType.CLIENT_CREDENTIALS.getValue()] as String[])
            parameterMap.put(AuthorizationServerConfig.SKILLS_PROXY_USER, [proxyUser] as String[])
            parameterMap.putAll(super.getParameterMap())
            return parameterMap
        }

        @Override
        Enumeration<String> getParameterNames() {
            return Collections.enumeration(getParameterMap().keySet())
        }

        @Override
        String[] getParameterValues(String name) {
            return getParameterMap().get(name) as String[]
        }

        @Override
        String getAuthType() {
            return request.getAuthType()
        }

        @Override
        Cookie[] getCookies() {
            return request.getCookies()
        }

        @Override
        long getDateHeader(String name) {
            return request.getDateHeader(name)
        }

        @Override
        String getHeader(String name) {
            return request.getHeader(name)
        }

        @Override
        Enumeration<String> getHeaders(String name) {
            return request.getHeaders(name)
        }

        @Override
        Enumeration<String> getHeaderNames() {
            return request.getHeaderNames()
        }

        @Override
        int getIntHeader(String name) {
            return request.getIntHeader(name)
        }

        @Override
        String getMethod() {
            return HttpMethod.POST.toString() //request.getMethod()
        }

        @Override
        String getPathInfo() {
            return request.getPathInfo()
        }

        @Override
        String getPathTranslated() {
            return request.getPathTranslated()
        }

        @Override
        String getContextPath() {
            return request.getContextPath()
        }

        @Override
        String getQueryString() {
            return request.getQueryString()
        }

        @Override
        String getRemoteUser() {
            return request.getRemoteUser()
        }

        @Override
        boolean isUserInRole(String role) {
            return request.isUserInRole(role)
        }

        @Override
        Principal getUserPrincipal() {
            return request.getUserPrincipal()
        }

        @Override
        String getRequestedSessionId() {
            return request.getRequestedSessionId()
        }

        @Override
        String getRequestURI() {
            return request.getRequestURI()
        }

        @Override
        StringBuffer getRequestURL() {
            return request.getRequestURL()
        }

        @Override
        String getServletPath() {
            return request.getServletPath()
        }

        @Override
        HttpSession getSession(boolean create) {
            return request.getSession(create)
        }

        @Override
        HttpSession getSession() {
            return request.getSession()
        }

        @Override
        String changeSessionId() {
            return request.changeSessionId()
        }

        @Override
        boolean isRequestedSessionIdValid() {
            return request.isRequestedSessionIdValid()
        }

        @Override
        boolean isRequestedSessionIdFromCookie() {
            return request.isRequestedSessionIdFromCookie()
        }

        @Override
        boolean isRequestedSessionIdFromURL() {
            return request.isRequestedSessionIdFromURL()
        }

        @Override
        boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
            return request.authenticate(response)
        }

        @Override
        void login(String username, String password) throws ServletException {
            request.login(username, password)
        }

        @Override
        void logout() throws ServletException {
            request.logout()
        }

        @Override
        Collection<Part> getParts() throws IOException, ServletException {
            return request.getParts()
        }

        @Override
        Part getPart(String name) throws IOException, ServletException {
            return request.getPart(name)
        }

        @Override
        <T extends HttpUpgradeHandler> T upgrade(Class<T> httpUpgradeHandlerClass) throws IOException, ServletException {
            return request.upgrade(httpUpgradeHandlerClass)
        }
    }
}
