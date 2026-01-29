/**
 * Copyright 2025 SkillTree
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
package skills.services.openai

import groovy.util.logging.Slf4j
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider

import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import java.security.KeyStore
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.time.Duration

@Configuration
@Slf4j
class OpenAIChatConfig {

    @Value('#{"${skills.openai.key:null}"}')
    String openAiKey

    @Value('#{"${skills.disableHostnameVerifier:false}"}')
    Boolean disableHostnameVerification = false

    @Value('#{"${skills.openai.stream.maxConnections:500}"}')
    Integer maxConnections = 500

    @Value('#{"${skills.openai.stream.maxIdleTime:20}"}')
    Integer maxIdleTime = 20

    @Value('#{"${skills.openai.stream.maxLifeTime:60}"}')
    Integer maxLifeTime = 60

    @Value('#{"${skills.openai.stream.handshakeTimeout:30000}"}') // 30 seconds
    Integer handshakeTimeout = 30000

    @Value('#{"${skills.openai.stream.responseTimeout:60}"}')
    Integer responseTimeout = 60

    @Value('#{"${skills.openai.stream.stream-usage:true}"}')
    Boolean streamUsage

    @Value('#{"${skills.openai.stream.closeNotifyFlushTimeout:5000}"}')
    Integer closeNotifyFlushTimeout = 5000

    @Value('#{"${skills.openai.stream.closeNotifyReadTimeout:5000}"}')
    Integer closeNotifyReadTimeout = 5000

    @Value('#{"${skills.openai.host}"}')
    String aiHost

    @Value('#{"${skills.openai.completionsEndpoint:/v1/chat/completions}"}')
    String completionsEndpoint

    @Bean
    OpenAiChatModel openAiChatModel(WebClient.Builder webClientBuilder) {
        if (!aiHost) {
            log.debug("skills.openai.host is not configured")
            return null
        }
        // Get system properties
        String keyStorePath = System.getProperty("javax.net.ssl.keyStore");
        String keyStorePassword = System.getProperty("javax.net.ssl.keyStorePassword");
        String keyStoreType = System.getProperty("javax.net.ssl.keyStoreType", "JKS");
        String trustStorePath = System.getProperty("javax.net.ssl.trustStore");
        String trustStorePassword = System.getProperty("javax.net.ssl.trustStorePassword");
        String trustStoreType = System.getProperty("javax.net.ssl.trustStoreType", "JKS");

        ConnectionProvider connectionProvider = ConnectionProvider.builder("OpenAIConnections")
                .maxConnections(maxConnections)
                .maxIdleTime(Duration.ofSeconds(maxIdleTime))
                .maxLifeTime(Duration.ofSeconds(maxLifeTime))
                .build()

        HttpClient httpClient =  HttpClient.create(connectionProvider)
                .responseTimeout(Duration.ofSeconds(responseTimeout))
        if (keyStorePath) {
            assert keyStorePath && keyStorePassword, "useKeystore set to true for JWT, but missing keystore resource and/or password"
            assert trustStorePath && trustStorePassword, "useKeystore set to true for JWT, but missing truststore resource and/or password"

            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            try (FileInputStream keyStoreStream = new FileInputStream(keyStorePath)) {
                keyStore.load(keyStoreStream, keyStorePassword.toCharArray());
            }

            // Load TrustStore
            KeyStore trustStore = KeyStore.getInstance(trustStoreType);
            try (FileInputStream trustStoreStream = new FileInputStream(trustStorePath)) {
                trustStore.load(trustStoreStream, trustStorePassword.toCharArray());
            }

            // Set up KeyManagerFactory
            KeyManagerFactory keyManagerFactory = KeyManagerFactory
                    .getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, keyStorePassword.toCharArray());

            // Set up TrustManagerFactory
            TrustManagerFactory trustManagerFactory = TrustManagerFactory
                    .getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);

            // Configure SslContext
            SslContextBuilder sslContextBuilder = SslContextBuilder
                    .forClient()
                    .keyManager(keyManagerFactory)
                    .trustManager(getTrustedManager(trustManagerFactory));

            // Create HttpClient with SSL configuration
            SslContext sslContext = sslContextBuilder.build();
            httpClient = httpClient.secure(spec -> spec.sslContext(sslContext)
                    .handshakeTimeout(Duration.ofMillis(handshakeTimeout))
                    .closeNotifyFlushTimeout(Duration.ofMillis(closeNotifyFlushTimeout))
                    .closeNotifyReadTimeout(Duration.ofMillis(closeNotifyReadTimeout))
            );
        }

        webClientBuilder = webClientBuilder.clientConnector(new ReactorClientHttpConnector(httpClient))
        OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey(openAiKey)
                .baseUrl(aiHost)
                .completionsPath(completionsEndpoint)
                .webClientBuilder(webClientBuilder)
                .build();
        OpenAiChatOptions openAiChatOptions = OpenAiChatOptions.builder()
                .streamUsage(streamUsage)
                .build();

        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(openAiChatOptions)
                .build()
    }

    private X509TrustManager getTrustedManager(TrustManagerFactory tmf) {
        X509TrustManager defaultTm = tmf.getTrustManagers().find { it instanceof X509TrustManager } as X509TrustManager

        if (!disableHostnameVerification) {
            return defaultTm
        }

        return new X509TrustManager() {
            @Override
            void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                defaultTm.checkClientTrusted(chain, authType)
            }

            @Override
            void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                // This is where we skip hostname verification
                // but still do certificate chain validation
                try {
                    defaultTm.checkServerTrusted(chain, authType)
                } catch (CertificateException e) {
                    if (!e.message?.contains("No subject alternative names present")) {
                        throw e
                    }
                    // Ignore hostname verification errors
                }
            }

            @Override
            X509Certificate[] getAcceptedIssuers() {
                return defaultTm.acceptedIssuers
            }
        }
    }
}
