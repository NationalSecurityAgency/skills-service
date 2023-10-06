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
import io.netty.handler.ssl.SslContextBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.messaging.simp.config.StompBrokerRelayRegistration
import org.springframework.messaging.simp.stomp.StompBrokerRelayMessageHandler
import org.springframework.messaging.simp.stomp.StompReactorNettyCodec
import org.springframework.messaging.simp.stomp.StompTcpConnectionHandler
import org.springframework.messaging.tcp.ReconnectStrategy
import org.springframework.messaging.tcp.TcpConnectionHandler
import org.springframework.messaging.tcp.reactor.ReactorNettyCodec
import org.springframework.messaging.tcp.reactor.ReactorNettyTcpClient
import org.springframework.util.ResourceUtils
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer
import reactor.netty.tcp.TcpClient
import skills.auth.AuthMode

import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.TrustManagerFactory
import java.security.KeyStore
import java.util.concurrent.CompletableFuture
import java.util.function.Function

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

    @Value('#{"${skills.websocket.enableRelayTls:false}"}')
    Boolean enableRelayTls

    @Value('${skills.authorization.authMode:#{T(skills.auth.AuthMode).DEFAULT_AUTH_MODE}}')
    AuthMode authMode

    @Autowired
    ChainedChannelInterceptor chainedChannelInterceptor


    @Override
    void configureMessageBroker(MessageBrokerRegistry registry) {
        if (enableStompBrokerRelay) {
            StompBrokerRelayRegistration stompBrokerRelayRegistration = registry.enableStompBrokerRelay('/topic', '/queue')
                    .setRelayHost(relayHost)
                    .setRelayPort(relayPort)
                    .setUserRegistryBroadcast('/topic/registry')
                    .setUserDestinationBroadcast('/topic/unresolved-user-destination')

             if (enableRelayTls) {
                 ReactorNettyTcpClient<byte[]> tcpClient = new SkillsReactorNettyTcpClient<>((configurer) -> configurer
                         .host(relayHost)
                         .port(relayPort)
                         .secure((spec) -> {
                             String keyStoreLocation = System.getProperty("javax.net.ssl.keyStore");
                             String keyStorePassword = System.getProperty("javax.net.ssl.keyStorePassword");
                             String trustStoreLocation = System.getProperty("javax.net.ssl.trustStore");
                             String trustStorePassword = System.getProperty("javax.net.ssl.trustStorePassword");

                             KeyStore keyStore = KeyStore.getInstance("JKS");
                             keyStore.load(new FileInputStream(ResourceUtils.getFile(keyStoreLocation)), keyStorePassword.toCharArray());

                             // Set up key manager factory to use our key store
                             KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                             keyManagerFactory.init(keyStore, keyStorePassword.toCharArray());

                             // truststore
                             KeyStore trustStore = KeyStore.getInstance("JKS");
                             trustStore.load(new FileInputStream((ResourceUtils.getFile(trustStoreLocation))), trustStorePassword.toCharArray());

                             TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                             trustManagerFactory.init(trustStore);

                             spec.sslContext(SslContextBuilder.forClient()
                                     .keyManager(keyManagerFactory)
                                     .trustManager(trustManagerFactory)
                                     .build());
                         }), new StompReactorNettyCodec())
                 stompBrokerRelayRegistration.setTcpClient(tcpClient)
             }
        } else {
            registry.enableSimpleBroker('/topic', '/queue')
        }
        registry.setApplicationDestinationPrefixes('/app')
    }

    @Override
    void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint('/skills-websocket')
                .setAllowedOriginPatterns("*")
                .withSockJS()
    }

    @Override
    void configureClientInboundChannel(ChannelRegistration registration) {
        if(chainedChannelInterceptor) {
            registration.interceptors(chainedChannelInterceptor)
        }
    }

    static class SkillsReactorNettyTcpClient<P> extends ReactorNettyTcpClient<P> {

        SkillsReactorNettyTcpClient(Function<TcpClient, TcpClient> clientConfigurer, ReactorNettyCodec<P> codec) {
            super(clientConfigurer, codec)
        }

        @Override
        CompletableFuture<Void> connectAsync(TcpConnectionHandler<P> handler) {
            removeAuthHeaders(handler)
            return super.connectAsync(handler)
        }

        @Override
        CompletableFuture<Void> connectAsync(TcpConnectionHandler<P> handler, ReconnectStrategy strategy) {
            removeAuthHeaders(handler)
            return super.connectAsync(handler, strategy)
        }

        private void removeAuthHeaders(TcpConnectionHandler<P> handler) {
            if (handler instanceof StompTcpConnectionHandler && handler.getSessionId().equals(StompBrokerRelayMessageHandler.SYSTEM_SESSION_ID)) {
                handler.getConnectHeaders().removeNativeHeader('login')
                handler.getConnectHeaders().removeNativeHeader('passcode')
            }
        }
    }

}
