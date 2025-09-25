package skills.auth.openai

import groovy.util.logging.Slf4j
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Component
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

@Slf4j
@Component
class SslWebClientConfig {

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

    @Value('#{"${skills.openai.stream.closeNotifyFlushTimeout:5000}"}')
    Integer closeNotifyFlushTimeout = 5000

    @Value('#{"${skills.openai.stream.closeNotifyReadTimeout:5000}"}')
    Integer closeNotifyReadTimeout = 5000

    WebClient createWebClient() throws Exception {
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

        // Create WebClient with the configured HttpClient
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer ${openAiKey}")
                .build();
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