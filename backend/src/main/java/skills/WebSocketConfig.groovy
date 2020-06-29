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
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer
import skills.auth.AuthMode

@Configuration
@Slf4j
@Order(-2147483549)  // Ordered.HIGHEST_PRECEDENCE + 99 (see https://github.com/spring-projects/spring-framework/blob/master/src/docs/asciidoc/web/websocket.adoc#token-authentication)
@EnableWebSocketMessageBroker
class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value('#{"${skills.websocket.enableStompBrokerRelay:false}"}')
    Boolean enableStompBrokerRelay

    @Value('#{"${skills.websocket.relayHost:skills-stomp-broker}"}')
    String relayHost

    @Value('#{"${skills.websocket.relayPort:61613}"}')
    Integer relayPort

    @Value('${skills.authorization.authMode:#{T(skills.auth.AuthMode).DEFAULT_AUTH_MODE}}')
    AuthMode authMode

    @Autowired
    ChainedChannelInterceptor chainedChannelInterceptor


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
        if(chainedChannelInterceptor) {
            registration.interceptors(chainedChannelInterceptor)
        }
    }

}
