package skills

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
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
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer
import skills.auth.AuthMode
import skills.auth.form.oauth2.SkillsOAuth2AuthenticationManager

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletRequestWrapper
import javax.servlet.http.HttpSession
import java.lang.reflect.Proxy

@Configuration
@Slf4j
@Order(-2147483549)  // Ordered.HIGHEST_PRECEDENCE + 99 (see https://github.com/spring-projects/spring-framework/blob/master/src/docs/asciidoc/web/websocket.adoc#token-authentication)
@EnableWebSocketMessageBroker
class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    static final String AUTHORIZATION = 'Authorization'

    @Value('#{"${skills.websocket.enableStompBrokerRelay:false}"}')
    Boolean enableStompBrokerRelay

    @Value('#{"${skills.websocket.relayHost:skills-stomp-broker}"}')
    String relayHost

    @Value('#{"${skills.websocket.relayPort:61613}"}')
    Integer relayPort

    // injected by the SkillsOAuth2AuthenticationManager itself (only when using SecurityMode.FormAuth)
    SkillsOAuth2AuthenticationManager oAuth2AuthenticationManager

    @Value('${skills.authorization.authMode:#{T(skills.auth.AuthMode).DEFAULT_AUTH_MODE}}')
    AuthMode authMode

    @Override
    void configureMessageBroker(MessageBrokerRegistry registry) {
        if (enableStompBrokerRelay) {
            registry.enableStompBrokerRelay('/topic', '/queue')
                    .setRelayHost(relayHost)
                    .setRelayPort(relayPort)
                    .setUserRegistryBroadcast('/topic/registry')
                    .setUserDestinationBroadcast('/topic/unresolved-user-destination')
        } else {
            registry.enableSimpleBroker('/topic', '/queue')
        }
        registry.setApplicationDestinationPrefixes('/app')
    }

    @Override
    void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint('/skills-websocket')
                .setAllowedOrigins("*")
                .withSockJS()
    }

    @Override
    void configureClientInboundChannel(ChannelRegistration registration) {
        if (authMode == AuthMode.FORM) { // only injected when using SecurityMode.FormAuth
            log.info('Initializing websocket registration interceptor.')
            registration.interceptors(new ChannelInterceptor() {
                TokenExtractor tokenExtractor = new BearerTokenExtractor()
                AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource = new OAuth2AuthenticationDetailsSource();

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
            })
        }
    }

    static class WebSocketHttpServletRequest extends HttpServletRequestWrapper {
        private static final HttpServletRequest UNSUPPORTED_REQUEST = (HttpServletRequest) Proxy
                .newProxyInstance(WebSocketHttpServletRequest.class.getClassLoader(),
                        [ HttpServletRequest.class ] as Class<?>[],
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
