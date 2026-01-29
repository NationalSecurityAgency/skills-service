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
import org.apache.hc.client5.http.classic.HttpClient
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory
import org.apache.hc.core5.http.config.RegistryBuilder
import org.apache.hc.core5.ssl.SSLContexts
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestTemplate

import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import java.util.concurrent.TimeUnit

@Slf4j
@Configuration
class OpenAIRestTemplateConfig {

    @Autowired
    OpenAIClientConfig openAIClientConfig

    @Bean(name = "openAIRestTemplate")
    RestTemplate openAIRestTemplate(OpenAIClientConfig openAIClientConfig) {
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(openAIClientHttpClient(openAIClientConfig))
        return new RestTemplate(requestFactory)
    }

    @Bean
    HttpClient openAIClientHttpClient(OpenAIClientConfig config) {
        SSLContext sslContext = SSLContexts.createSystemDefault()
        HostnameVerifier allowAllHosts = new NoopHostnameVerifier();
        SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(
                sslContext,
                ['TLSv1.2', 'TLSv1.3'] as String[],
                null,
                allowAllHosts
        )

        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(
                RegistryBuilder.create()
                        .register("http", PlainConnectionSocketFactory.getSocketFactory())
                        .register("https", sslConnectionSocketFactory)
                        .build()
        )
        connectionManager.maxTotal = config.maxTotal
        connectionManager.defaultMaxPerRoute = config.defaultMaxPerRoute

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(config.connectionRequestTimeout, TimeUnit.MILLISECONDS)
                .setConnectTimeout(config.connectTimeout, TimeUnit.MILLISECONDS)
                .setResponseTimeout(config.socketTimeout, TimeUnit.MILLISECONDS)
                .build()

        return HttpClientBuilder.create()
                .useSystemProperties()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build()
    }

    @Configuration
    @ConfigurationProperties(prefix = 'skills.openai.http')
    static class OpenAIClientConfig {
        Integer maxTotal = 20
        Integer defaultMaxPerRoute = 20
        Long connectionRequestTimeout = 5000
        Long connectTimeout = 5000
        Long socketTimeout = 30000
    }
}