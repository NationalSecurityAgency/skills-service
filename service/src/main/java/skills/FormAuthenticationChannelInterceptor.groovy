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
package skills

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.DependsOn
import org.springframework.context.annotation.Lazy
import org.springframework.core.annotation.Order
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.AuthenticationDetailsSource
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.provider.authentication.BearerTokenExtractor
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetailsSource
import org.springframework.security.oauth2.provider.authentication.TokenExtractor
import org.springframework.security.web.UnsupportedOperationExceptionInvocationHandler
import org.springframework.stereotype.Component
import skills.auth.SecurityMode
import skills.auth.form.oauth2.SkillsOAuth2AuthenticationManager

import javax.annotation.PostConstruct
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletRequestWrapper
import javax.servlet.http.HttpSession
import java.lang.reflect.Proxy

@Qualifier("WebSocketConfig")
@Lazy
@Slf4j
@Component
@Conditional(SecurityMode.FormAuth)
@Order(-100)
class FormAuthenticationChannelInterceptor implements ChannelInterceptor {

    static final String AUTHORIZATION = 'Authorization'

    TokenExtractor tokenExtractor
    AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource

    SkillsOAuth2AuthenticationManager oAuth2AuthenticationManager

    @PostConstruct
    public void init() {
        log.info("Registering FormAuthenticationChannelInterceptor")
        tokenExtractor = new BearerTokenExtractor()
        authenticationDetailsSource = new OAuth2AuthenticationDetailsSource();
    }

    @Autowired
    void setoAuth2AuthenticationManager(SkillsOAuth2AuthenticationManager oAuth2AuthenticationManager) {
        this.oAuth2AuthenticationManager = oAuth2AuthenticationManager
    }

    @Override
    Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class)
        if (StompCommand.CONNECT == accessor.getCommand()) {
            List<String> authHeaders = accessor.getNativeHeader(AUTHORIZATION)
            if (authHeaders) {
                log.debug("Found Authorization headers on websocket connection: [{}]", authHeaders)
                WebSocketHttpServletRequest request = new WebSocketHttpServletRequest(headers: [(AUTHORIZATION): Collections.enumeration(authHeaders)])
                Authentication authentication = tokenExtractor.extract(request)
                if (authentication) {
                    request.setAttribute(OAuth2AuthenticationDetails.ACCESS_TOKEN_VALUE, authentication.getPrincipal());
                    if (authentication instanceof AbstractAuthenticationToken) {
                        AbstractAuthenticationToken needsDetails = (AbstractAuthenticationToken) authentication;
                        needsDetails.setDetails(authenticationDetailsSource.buildDetails(request));
                    }
                    Authentication authResult = oAuth2AuthenticationManager.authenticate(authentication);
                    if (authResult.authenticated) {
                        log.debug("Setting OAuth user [{}] on websocket connection", authResult)
                        accessor.setUser(authResult)
                    }
                }
            }
        }
        return message
    }

    static class WebSocketHttpServletRequest extends HttpServletRequestWrapper {
        private static final HttpServletRequest UNSUPPORTED_REQUEST = (HttpServletRequest) Proxy
                .newProxyInstance(WebSocketHttpServletRequest.class.getClassLoader(),
                        [HttpServletRequest.class] as Class<?>[],
                        new UnsupportedOperationExceptionInvocationHandler())

        String remoteAddr
        Map<String, Object> attributes = [:]
        Map<String, Enumeration<String>> headers = [:]

        WebSocketHttpServletRequest() { super(UNSUPPORTED_REQUEST) }

        Object getAttribute(String attributeName) { return attributes.get(attributeName) }

        void setAttribute(String name, Object o) { attributes.put(name, o) }

        Enumeration<String> getHeaders(String name) { return headers.get(name) }

        HttpSession getSession(boolean create) { return null }
    }
}
