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
package skills.websocket

import groovy.util.logging.Slf4j
import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Lazy
import org.springframework.core.annotation.Order
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver
import org.springframework.security.web.FilterInvocation
import org.springframework.stereotype.Component

//import org.springframework.security.oauth2.provider.authentication.BearerTokenExtractor
//import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails
//import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetailsSource
//import org.springframework.security.oauth2.provider.authentication.TokenExtractor

import skills.auth.SecurityMode
import skills.auth.form.oauth2.AuthorizationServerConfig

//import skills.auth.form.oauth2.SkillsOAuth2AuthenticationManager

import skills.auth.form.oauth2.ResourceServerConfig

import java.lang.reflect.Proxy

@Qualifier("WebSocketConfig")
@Lazy
@Slf4j
@Component
@Conditional(SecurityMode.FormAuth)
@Order(-100)
class FormAuthenticationChannelInterceptor implements ChannelInterceptor {

    static final String AUTHORIZATION = 'Authorization'

    @Autowired
    JwtDecoder jwtDecoder

    @Autowired
    @Lazy
    ResourceServerConfig.SkillsJwtAuthenticationConverter skillsJwtAuthenticationConverter

    private JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter()

    @PostConstruct
    void init() {
        log.info("Registering FormAuthenticationChannelInterceptor")
        jwtAuthenticationConverter.setPrincipalClaimName(AuthorizationServerConfig.SKILLS_PROXY_USER)
    }

    @Override
    Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class)
        if (StompCommand.CONNECT == accessor.getCommand()) {
            List<String> authHeaders = accessor.getNativeHeader(AUTHORIZATION)
            if (authHeaders && authHeaders.size() == 1) {
                log.debug("Found Authorization headers on websocket connection: [{}]", authHeaders)
                WebSocketHttpServletRequest request = new WebSocketHttpServletRequest(headers: [(AUTHORIZATION): authHeaders])
                String token = new DefaultBearerTokenResolver().resolve(request)
                Authentication authResult = skillsJwtAuthenticationConverter.convert(jwtDecoder.decode(token))
                if (authResult?.authenticated) {
                    log.debug("Setting OAuth user [{}] on websocket connection", authResult)
                    accessor.setUser(authResult)
                }
            }
        }
        return message
    }

    static class WebSocketHttpServletRequest extends HttpServletRequestWrapper {
        private static final HttpServletRequest UNSUPPORTED_REQUEST = (HttpServletRequest) Proxy
                .newProxyInstance(WebSocketHttpServletRequest.class.getClassLoader(),
                        [HttpServletRequest.class] as Class<?>[],
                        new FilterInvocation.UnsupportedOperationExceptionInvocationHandler())

        Map<String, List<String>> headers = [:]

        WebSocketHttpServletRequest() { super(UNSUPPORTED_REQUEST) }

        @Override
        String getMethod() {
            return "NA"
        }

        @Override
        String getHeader(String name) {
            return headers.get(name)?.first()
        }

    }
}
