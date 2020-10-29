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
import org.apache.http.client.HttpClient
import org.apache.http.conn.ssl.AllowAllHostnameVerifier
import org.apache.http.impl.client.HttpClientBuilder
import org.springframework.http.*
import org.springframework.http.client.*
import org.springframework.http.converter.GenericHttpMessageConverter
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.ResponseErrorHandler
import org.springframework.web.client.RestTemplate

import java.lang.reflect.Type
import java.nio.charset.Charset

@Slf4j
class RestTemplateWrapper extends RestTemplate {

    static final String AUTH_HEADER = 'Authorization'

    private final RestTemplate restTemplate

    private boolean authenticated = false
    String authenticationToken

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
        private String cookie;

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
            if (cookie != null) {
                request.getHeaders().add(HttpHeaders.COOKIE, cookie);
            }
            ClientHttpResponse response = execution.execute(request, body);

            if (cookie == null) {
                cookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
                log.debug("Setting cookie to [{}]", cookie)
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
        return HttpClientBuilder.create()
                .setSSLHostnameVerifier(new AllowAllHostnameVerifier())
                .build()
    }

    void auth(String skillsServiceUrl, String username, String password, String firstName, lastName) {
        if(!this.pkiAuth) {
            boolean accountCreated = createAccount(skillsServiceUrl, username, password, firstName, lastName)
            if (!accountCreated) {
                HttpHeaders headers = new HttpHeaders()
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED)
                MultiValueMap<String, String> params = new LinkedMultiValueMap<>()
                params.add('username', username)
                params.add('password', password)

                HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers)
                ResponseEntity<String> response = restTemplate.postForEntity(skillsServiceUrl + '/performLogin', request, String.class)

                assert response.statusCode == HttpStatus.OK, 'authentication failed: ' + response.statusCode

                authenticationToken = response.getHeaders().getFirst(AUTH_HEADER)
//        assert authenticationToken, 'no authentication token was provided!'
            }
        }
        authenticated = true
    }
    private boolean createAccount(String skillsServiceUrl, String username, String password, String firstName, String lastName) {
        boolean accountCreated = false
        ResponseEntity<String> userExistsResponse = restTemplate.getForEntity("${skillsServiceUrl}/app/users/validExistingDashboardUserId/{userId}", String, username)
        boolean userExists = Boolean.valueOf(userExistsResponse.body)
        if (!userExists) {
            Map<String, String> userInfo = [
                    firstName: firstName,
                    lastName : lastName,
                    email    : username,
                    password : password,
            ]
            ResponseEntity response = putForEntity(skillsServiceUrl + '/createAccount', userInfo)
            if ( response.statusCode != HttpStatus.OK) {
                throw new SkillsClientException(response.body, skillsServiceUrl, response.statusCode)
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
    <T> ResponseEntity<T> patchForObject(String url, Object request, Class<T> response, Object... uriVariables) {
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
