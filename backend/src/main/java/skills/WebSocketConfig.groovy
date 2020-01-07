package skills

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value('#{"${skills.websocket.enableStompBrokerRelay:false}"}')
    Boolean enableStompBrokerRelay

    @Value('#{"${skills.websocket.relayHost:skills-stomp-broker}"}')
    String relayHost

    @Value('#{"${skills.websocket.relayPort:61613}"}')
    Integer relayPort

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
                .withSockJS();
    }
}
