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
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.http.okhttp.OpenAiHttpClientBuilderCustomizer
import org.springframework.ai.openai.http.okhttp.SpringAiOpenAiHttpClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

import javax.net.ssl.*
import java.security.KeyStore
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.time.Duration

@Configuration
@Slf4j
class OpenAIChatConfig {

    @Value('${skills.openai.key:#{null}}')
    String openAiKey

    @Value('#{"${skills.disableHostnameVerifier:false}"}')
    Boolean disableHostnameVerification = false

    @Value('#{"${skills.openai.stream.maxConnections:500}"}')
    Integer maxConnections = 500

    @Value('#{"${skills.openai.stream.maxIdleTime:20}"}')
    Integer maxIdleTime = 20

    @Value('#{"${skills.openai.stream.maxLifeTime:60}"}')
    Integer maxLifeTime = 60

    @Value('#{"${skills.openai.options.timeoutInSecs:60}"}') // 60 seconds
    Integer timeoutInSecs = 60

    @Value('#{"${skills.openai.stream.stream-usage:true}"}')
    Boolean streamUsage

    @Value('#{"${skills.openai.host}"}')
    String aiHost

    /**
     * 1. The HttpClient Customizer Bean.
     * Conditionally loaded based on the primary 2-way SSL property toggle.
     */
    @Bean
    @ConditionalOnProperty(prefix = 'skills.openai.ssl.two-way', name = 'enabled', havingValue = 'true', matchIfMissing = false)
    OpenAiHttpClientBuilderCustomizer mutualTlsCustomizer() {
        if (!aiHost) {
            log.debug("skills.openai.host is not configured")
            return null
        }

        String keyStorePath = System.getProperty("javax.net.ssl.keyStore")
        String keyStorePassword = System.getProperty("javax.net.ssl.keyStorePassword")
        String keyStoreType = System.getProperty("javax.net.ssl.keyStoreType", "JKS")
        String trustStorePath = System.getProperty("javax.net.ssl.trustStore")
        String trustStorePassword = System.getProperty("javax.net.ssl.trustStorePassword")
        String trustStoreType = System.getProperty("javax.net.ssl.trustStoreType", "JKS")

        assert keyStorePath && keyStorePassword, "skills.openai.ssl.two-way.enabled set to true, but missing keystore resource and/or password"
        assert trustStorePath && trustStorePassword, "skills.openai.ssl.two-way.enabled set to true, but missing truststore resource and/or password"

        log.info("Loading mutualTlsCustomizer with keyStore=[{}] and trustStorePath=[{}]", keyStorePath, trustStorePath)

        return { SpringAiOpenAiHttpClient.Builder builder ->
            try {
                // 1. Load Client KeyStore (For Client Certificate / Private Key)
                KeyStore keyStore = KeyStore.getInstance(keyStoreType)
                try (FileInputStream keyStoreStream = new FileInputStream(keyStorePath)) {
                    keyStore.load(keyStoreStream, keyStorePassword.toCharArray())
                }
                KeyManagerFactory keyManagerFactory = KeyManagerFactory
                        .getInstance(KeyManagerFactory.getDefaultAlgorithm())
                keyManagerFactory.init(keyStore, keyStorePassword.toCharArray())

                // 2. Load TrustStore (To validate OpenAI/Gateway server certificate)
                KeyStore trustStore = KeyStore.getInstance(trustStoreType)
                try (FileInputStream trustStoreStream = new FileInputStream(trustStorePath)) {
                    trustStore.load(trustStoreStream, trustStorePassword.toCharArray())
                }
                TrustManagerFactory trustManagerFactory = TrustManagerFactory
                        .getInstance(TrustManagerFactory.getDefaultAlgorithm())
                trustManagerFactory.init(trustStore)


                // 3. Initialize SSLContext
                X509TrustManager trustManager = getTrustedManager(trustManagerFactory)
                SSLContext sslContext = SSLContext.getInstance("TLS")
                sslContext.init(keyManagerFactory.keyManagers, [trustManager] as X509TrustManager[], null)

                // 4. Inject into OkHttp Builder wrapper
                builder.sslSocketFactory(sslContext.socketFactory)
                builder.trustManager(trustManager)

                // 5. Optionally bypass hostname verification
                if (disableHostnameVerification) {
                    // This closure matches the HostnameVerifier functional interface (boolean verify(String hostname, SSLSession session))
                    builder.hostnameVerifier({ String hostname, javax.net.ssl.SSLSession session -> true } as HostnameVerifier)

                    log.warn("disabled hostname verification")
                }

            } catch (Exception e) {
                throw new IllegalStateException('Failed to configure 2-way SSL for OpenAI Client', e)
            }
        } as OpenAiHttpClientBuilderCustomizer
    }


    /**
     * 2. The Manually Configured Chat Model Bean.
     * Accepts the optional 'mutualTlsCustomizer' if it exists in the ApplicationContext.
     */
    @Bean
    @ConditionalOnProperty(prefix = 'skills.openai', name = 'enabled', havingValue = 'true', matchIfMissing = true)
    OpenAiChatModel openAiChatModel(Optional<OpenAiHttpClientBuilderCustomizer> customizerOptional) {
        if (!aiHost) {
            log.debug("skills.openai.host is not configured")
            return null
        }
        log.info("OpenAiChatModel: aiHost=[{}], streamUsage=[{}], timeoutInSecs=[{}]", aiHost, streamUsage, timeoutInSecs)

        // Define default runtime options
        OpenAiChatOptions chatOptions = OpenAiChatOptions.builder()
                .baseUrl(aiHost)
                .apiKey(openAiKey ?: 'NoKeyProvided')
                .streamUsage(streamUsage)
                .timeout(Duration.ofSeconds(timeoutInSecs))
                .build()
        // Construct the model via its builder pattern
        OpenAiChatModel.Builder modelBuilder = OpenAiChatModel.builder().options(chatOptions)

        // Bind the 2-way SSL customizer directly if it was created/enabled
        customizerOptional.ifPresent { customizer ->
            modelBuilder.httpClientBuilderCustomizer(customizer)
        }

        return modelBuilder.build()
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
