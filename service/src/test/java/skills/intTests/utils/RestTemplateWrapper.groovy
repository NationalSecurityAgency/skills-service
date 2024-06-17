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
import org.apache.hc.client5.http.classic.HttpClient
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory
import org.apache.hc.core5.http.config.RegistryBuilder
import org.apache.hc.core5.ssl.SSLContexts
import org.springframework.http.*
import org.springframework.http.client.*
import org.springframework.http.converter.GenericHttpMessageConverter
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.lang.Nullable
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.ResponseErrorHandler
import org.springframework.web.client.RestTemplate

import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import java.lang.reflect.Type
import java.nio.charset.Charset

@Slf4j
class RestTemplateWrapper extends RestTemplate {

    static final String AUTH_HEADER = 'Authorization'

    private final RestTemplate restTemplate

    private boolean authenticated = false
    String authenticationToken
    ResponseEntity<String> authResponse

    private pkiAuth = false

    RestTemplateWrapper() {
        this(new RestTemplate())
    }

    RestTemplateWrapper(RestTemplate restTemplate, boolean pkiAuth=false) {
        this.pkiAuth = pkiAuth
        this.restTemplate = restTemplate
        setupRestTemplate()
        List<ClientHttpRequestInterceptor> interceptors = [new StatefulRestTemplateInterceptor()]
        this.restTemplate.setInterceptors(interceptors)
    }

    /***
     * Need for load balancer support as it uses cookies to keep track which server currently connected to
     */
    static class StatefulRestTemplateInterceptor implements ClientHttpRequestInterceptor {
        private List<String> cookies;
        private String xsrfToken;

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

            HttpHeaders requstHeaders = request.getHeaders()
            if (cookies) {
                requstHeaders.addAll(HttpHeaders.COOKIE, cookies);
            }
            if (xsrfToken != null) {
                requstHeaders.add("X-XSRF-TOKEN" , xsrfToken);
            }
            log.debug("REQUEST: [{}], headers [{}]", request.URI, request.headers)
            ClientHttpResponse response = execution.execute(request, body);

            HttpHeaders headers = response.getHeaders();

            List<String> returnedCookies = headers.getOrEmpty(HttpHeaders.SET_COOKIE)
            if (returnedCookies && cookies == null) {
                cookies = returnedCookies
                log.info("Setting cookies to {}", returnedCookies)
                printf "Setting cookies to ${returnedCookies}"
            }
            if (returnedCookies && !xsrfToken) {
                String cookieXSRF  = returnedCookies.find { it.startsWith("XSRF-TOKEN=") }
                if (cookieXSRF) {
                    xsrfToken = (cookieXSRF =~ /XSRF-TOKEN=([^;]*)/)[0][1]
                    log.debug("Response: [{}], set xsrfToken to [{}]", request.URI, xsrfToken)
                }
            }
            return response;
        }
    }

    private void setupRestTemplate() {
        if(!this.pkiAuth) {
            def requestFactory = getHttpRequestFactory()
            restTemplate.setRequestFactory(requestFactory)
        }

        restTemplate.setErrorHandler(new ResponseErrorHandler() {
            @Override
            boolean hasError(ClientHttpResponse clientHttpResponse) throws IOException {
                return false
            }

            @Override
            void handleError(ClientHttpResponse clientHttpResponse) throws IOException {
            }
        })
    }

    ClientHttpRequestFactory getHttpRequestFactory() {
        HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory()
        clientHttpRequestFactory.setHttpClient(getHttpClient())
        return clientHttpRequestFactory
    }

    HttpClient getHttpClient() {
        return HttpClients.custom()
                .useSystemProperties()
                .setConnectionManager(poolingHttpClientConnectionManager())
                .disableAutomaticRetries()
                .build()
    }

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

        return result
    }

    void auth(String skillsServiceUrl, String username, String password, String firstName, String lastName, String email=null) {
        if(!this.pkiAuth) {
            boolean accountCreated = createAccount(skillsServiceUrl, username, password, firstName, lastName, email)
            if (!accountCreated) {
                HttpHeaders headers = new HttpHeaders()
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED)
                MultiValueMap<String, String> params = new LinkedMultiValueMap<>()
                params.add('username', username)
                params.add('password', password)

                HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers)
                authResponse = restTemplate.postForEntity(skillsServiceUrl + '/performLogin', request, String.class)

                assert authResponse.statusCode == HttpStatus.OK, 'authentication failed: ' + authResponse.statusCode

                authenticationToken = authResponse.getHeaders().getFirst(AUTH_HEADER)
            }
        }
        authenticated = true
    }
    private boolean createAccount(String skillsServiceUrl, String username, String password, String firstName, String lastName, String email=null) {
        boolean accountCreated = false
        ResponseEntity<String> userExistsResponse = restTemplate.getForEntity("${skillsServiceUrl}/app/users/validExistingDashboardUserId/{userId}", String, username)
        boolean userExists = Boolean.valueOf(userExistsResponse.body)
        if (!userExists) {
            Map<String, String> userInfo = [
                    firstName: firstName,
                    lastName : lastName,
                    email    : username,
                    password : password,
                    usernameForDisplay: "$username for display".toString()
            ]

            if (email != null) {
                userInfo.email = email
                if (email != username) {
                    userInfo.username = username
                }
            }

            ResponseEntity response = putForEntity(skillsServiceUrl + '/createAccount', userInfo)
            if ( response.statusCode != HttpStatus.OK) {
                throw new SkillsClientException((String)response.body, skillsServiceUrl, (HttpStatus)response.statusCode)
            }
            accountCreated = true
        }
        return accountCreated
    }

    @Override
    void delete(String url, Object... uriVariables) {
        ResponseEntity responseEntity = restTemplate.exchange(url, HttpMethod.DELETE, getAuthEntity(), String.class)
        if(!responseEntity.statusCode.is2xxSuccessful()){
            throw HttpClientErrorException.create(responseEntity.statusCode,
                    responseEntity.statusCode.toString(),
                    responseEntity.headers,
                    responseEntity.getBody().bytes,
                    Charset.forName("UTF-8"))
        }
    }

    @Override
    <T> ResponseEntity<T> getForEntity(String url, Class<T> response, Object... uriVariables) {
        return restTemplate.exchange(url, HttpMethod.GET, getAuthEntity(), response)
    }

    ResponseEntity putForEntity(String url, Object request, Object... uriVariables) {
        return restTemplate.exchange(url, HttpMethod.PUT, getAuthEntity(String.class, request), String.class, uriVariables)
    }

    @Override
    <T> ResponseEntity<T> postForEntity(String url, Object request, Class<T> response, Object... uriVariables) {
        return restTemplate.exchange(url, HttpMethod.POST, getAuthEntity(String.class, request), response)
    }

    @Override
    <T> T patchForObject(String url, @Nullable Object request, Class<T> response, Object... uriVariables) {
        return restTemplate.exchange(url, HttpMethod.PATCH, getAuthEntity(String.class, request), response)
    }

    ResponseEntity deleteForEntity(String url, Object request, Object... uriVariables) {
        return restTemplate.exchange(url, HttpMethod.DELETE, getAuthEntity(String.class, request), String.class, uriVariables)
    }

    HttpEntity<?> getAuthEntity(Class<?> responseType = String.class, Object body = new LinkedMultiValueMap<>(), HttpHeaders headers = new HttpHeaders()) {
        if (body instanceof HttpEntity) {
            HttpEntity entity = (HttpEntity) body
            headers.addAll(entity.headers)
            body = entity.body
        } else {
            addAcceptContent(headers, responseType)
        }
        addAuthHeader(headers, responseType)
        return new HttpEntity<Object>(body, headers)
    }

    private void addAuthHeader(HttpHeaders headers, Class<?> responseType) {
        if (authenticationToken) {
            headers.set(AUTH_HEADER, authenticationToken)
        }
    }

    void addAcceptContent(HttpHeaders headers, Class<?> responseType) {
        List<MediaType> allSupportedMediaTypes = restTemplate.getMessageConverters()
                .findAll {this.canReadResponse(responseType, it)}
                .collect {getSupportedMediaTypes(it)}
                .flatten()
                .unique()
        allSupportedMediaTypes.sort(MediaType.SPECIFICITY_COMPARATOR)
        headers.setAccept(allSupportedMediaTypes)
    }

    private boolean canReadResponse(Type responseType, HttpMessageConverter<?> converter) {
        Class<?> responseClass = (responseType instanceof Class ? (Class<?>) responseType : null)
        if (responseClass != null) {
            return converter.canRead(responseClass, null)
        }
        else if (converter instanceof GenericHttpMessageConverter) {
            GenericHttpMessageConverter<?> genericConverter = (GenericHttpMessageConverter<?>) converter
            return genericConverter.canRead(responseType, null, null)
        }
        return false
    }
    private List<MediaType> getSupportedMediaTypes(HttpMessageConverter<?> messageConverter) {
        return messageConverter.getSupportedMediaTypes()
                .findAll {it.getCharset() != null}
                .collect {new MediaType(it.getType(), it.getSubtype())}
    }
}
