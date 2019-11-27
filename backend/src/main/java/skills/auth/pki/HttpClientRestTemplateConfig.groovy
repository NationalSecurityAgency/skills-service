package skills.auth.pki

import groovy.util.logging.Slf4j
import org.apache.http.client.HttpClient
import org.apache.http.client.config.RequestConfig
import org.apache.http.config.RegistryBuilder
import org.apache.http.conn.socket.PlainConnectionSocketFactory
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.apache.http.ssl.SSLContexts
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestTemplate

import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext

/**
 * This Configuration exposes a RestTemplate the uses a apache HttpClient 4.5.  This RestTemplate can be @Autowrired
 * if you need an of the HttpClient customization, but is not currently being used.
 */
@Slf4j
@Configuration
class HttpClientRestTemplateConfig {

    @Value('${skills.authorization.userInfoUri}')
    String userInfoUri

    @Autowired
    HttpClientConfig httpClientConfig

    @Configuration
    @ConfigurationProperties(prefix = 'skills.user-info-service.connection')
    static class HttpClientConfig {
        Integer maxTotal = 20
        Integer defaultMaxPerRoute = 20
        Integer connectionRequestTimeout = 2000
        Integer connectTimeout = 2000
        Integer socketTimeout = 2000
    }

    @Bean
    PoolingHttpClientConnectionManager poolingHttpClientConnectionManager() {
        SSLContext sslContext = SSLContexts.createSystemDefault()
        HostnameVerifier allowAllHosts = new NoopHostnameVerifier();
        SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(
                sslContext,
                ['TLSv1.2'] as String[],
                null,
                allowAllHosts);

        PoolingHttpClientConnectionManager result =
                new PoolingHttpClientConnectionManager(RegistryBuilder.create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", sslConnectionSocketFactory).build())

        result.setMaxTotal(this.httpClientConfig.getMaxTotal())
        result.setDefaultMaxPerRoute(this.httpClientConfig.getDefaultMaxPerRoute())
        return result
    }

    @Bean
    RequestConfig requestConfig() {
        RequestConfig result = RequestConfig.custom()
                .setConnectionRequestTimeout(httpClientConfig.connectionRequestTimeout)
                .setConnectTimeout(httpClientConfig.connectTimeout)
                .setSocketTimeout(httpClientConfig.socketTimeout)
                .build()
        return result
    }

    @Bean
    CloseableHttpClient httpClient(PoolingHttpClientConnectionManager poolingHttpClientConnectionManager, RequestConfig requestConfig) {
        CloseableHttpClient result = HttpClients.custom()
                .setSSLHostnameVerifier(new NoopHostnameVerifier())
                .useSystemProperties()
                .setConnectionManager(poolingHttpClientConnectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build()
        return result
    }

    @Bean
    RestTemplate restTemplate(HttpClient httpClient) {
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory()
        requestFactory.setHttpClient(httpClient)
        return new RestTemplate(requestFactory)
    }
}
