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
package skills.intTests.utils

import groovy.util.logging.Slf4j
import jakarta.annotation.PostConstruct
import org.apache.hc.client5.http.classic.HttpClient
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Conditional
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.util.ResourceUtils
import org.springframework.web.client.RestTemplate
import skills.auth.SecurityMode
import skills.auth.pki.HttpClientRestTemplateConfig

import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import java.security.KeyStore

@Slf4j
@Conditional(SecurityMode.PkiAuth)
@Component
class SystemSSLConfiguration {

    @Value('#{"${server.ssl.key-store}"}')
    String keyStore
    @Value('#{"${server.ssl.key-store-password}"}')
    String keyStorePassword
    @Value('#{"${server.ssl.keyStoreType}"}')
    String keyStoreType
    @Value('#{"${server.ssl.trust-store}"}')
    String trustStore
    @Value('#{"${server.ssl.trust-store-password}"}')
    String trustStorePassword
    @Value('#{"${server.ssl.trustStoreType}"}')
    String trustStoreType

    @Autowired
    HttpClientRestTemplateConfig rtc

    @Autowired
    RestTemplate restTemplate

    @PostConstruct
    void init() {
        if (keyStore) {
            log.info("Setting system ssl properties for integration tests")
            File ksFile = ResourceUtils.getFile(keyStore)
            System.setProperty("javax.net.ssl.keyStore", ksFile.getPath())
            System.setProperty("javax.net.ssl.keyStorePassword", keyStorePassword)
            System.setProperty("javax.net.ssl.keyStoreType", keyStoreType)
            File trustFile = ResourceUtils.getFile(trustStore)
            System.setProperty("javax.net.ssl.trustStore", trustFile.getPath())
            System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword)
            System.setProperty("javax.net.ssl.trustStoreType", trustStoreType)

            // override any existing default SSLContext
            KeyStore keyStoreObj = KeyStore.getInstance("JKS");
            keyStoreObj.load(new FileInputStream(ksFile), keyStorePassword.toCharArray());

            // Set up key manager factory to use our key store
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStoreObj, keyStorePassword.toCharArray());

            // truststore
            KeyStore trustStoreObj = KeyStore.getInstance("JKS");
            trustStoreObj.load(new FileInputStream(trustFile), trustStorePassword.toCharArray());

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStoreObj);

            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
            SSLContext.setDefault(ctx);

            HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());
            HttpClient httpClient = HttpClients.custom()
                    .useSystemProperties()
                    .setConnectionManager(rtc.createPoolingHttpClientConnectionManager())
                    .setDefaultRequestConfig(rtc.requestConfig())
                    .build()
            HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory()
            requestFactory.setHttpClient(httpClient)
            restTemplate.setRequestFactory(requestFactory)
        }
    }
}
