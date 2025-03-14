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

import com.fasterxml.jackson.annotation.JsonProperty
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.apache.hc.client5.http.classic.HttpClient
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory
import org.apache.hc.client5.http.ssl.TrustAllStrategy
import org.apache.hc.core5.http.config.RegistryBuilder
import org.apache.hc.core5.ssl.SSLContexts
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.*
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.http.client.support.BasicAuthenticationInterceptor
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate

import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import java.security.KeyStore

@Slf4j
class WSHelper {

    String skillsService = "http://localhost:8080"
    RestTemplateWrapper restTemplateWrapper
    RestTemplate oAuthRestTemplate
    JsonSlurper jsonSlurper = new JsonSlurper()

    String username = "skills@skills.org"
    String password = "p@ssw0rd"

    String firstName = 'Skills'
    String lastName = 'Test'
    String email = null

    CertificateRegistry certificateRegistry

    WSHelper init(boolean pkiAuth=false) {

        RestTemplate restTemplate

        if (pkiAuth) {
            Resource resource = certificateRegistry.getCertificate(username)
            assert resource != null : "No certificate found for ${username}"
            KeyStore keyStore = KeyStore.getInstance("PKCS12")
            keyStore.load(resource.getInputStream(), "skillspass".toCharArray())

            KeyStore trustStore = KeyStore.getInstance("JKS")
            trustStore.load(new ClassPathResource("/certs/truststore.jks").getInputStream(), "skillspass".toCharArray())
            SSLContext sslContext = SSLContexts.custom()
                    .loadTrustMaterial(trustStore, TrustAllStrategy.INSTANCE)
                    .loadKeyMaterial(keyStore, "skillspass".toCharArray()).build()
            HostnameVerifier allowAllHosts = new NoopHostnameVerifier();
            SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(                sslContext,
                    ['TLSv1.2'] as String[],
                    null,
                    allowAllHosts)
            PoolingHttpClientConnectionManager connectionManager =
                    new PoolingHttpClientConnectionManager(RegistryBuilder.create()
                            .register("http", PlainConnectionSocketFactory.getSocketFactory())
                            .register("https", sslConnectionSocketFactory).build())
            HttpClient client = HttpClients.custom()
                    .setConnectionManager(connectionManager)
                    .build()
            HttpComponentsClientHttpRequestFactory requestFactory = new  HttpComponentsClientHttpRequestFactory(client)
            restTemplate = new RestTemplate(requestFactory)
        } else {
            restTemplate = new RestTemplate()
        }

        restTemplateWrapper = new RestTemplateWrapper(restTemplate, pkiAuth)
        oAuthRestTemplate = restTemplate
        restTemplateWrapper.auth(skillsService, username, password, firstName, lastName, email)
        return this
    }

    void setProxyCredentials(String clientId, String secretCode) {
        List<ClientHttpRequestInterceptor> existingInterceptors = oAuthRestTemplate.getInterceptors()
        existingInterceptors.add(new BasicAuthenticationInterceptor(clientId, secretCode))
        oAuthRestTemplate.setInterceptors(existingInterceptors)
    }

    def appPut(String endpoint, def params) {
        put(endpoint, "app", params)
    }

    def appPost(String endpoint, def params) {
        def res = post(endpoint, "app", params)
        return res
    }

    def appGet(String endpoint, Map params = null) {
        return get(endpoint, "app", params)
    }

    def rootContextGet(String endpoint, Map params = null) {
        return get(endpoint, "", params)
    }

    def adminPut(String endpoint, def params = null) {
        put(endpoint, "admin", params)
    }

    def supervisorGet(String endpoint, def params = null) {
        get(endpoint, "supervisor", params)
    }

    def supervisorPut(String endpoint, def params = null) {
        put(endpoint, "supervisor", params)
    }
    def supervisorDelete(String endpoint, def params = null) {
        delete(endpoint, "supervisor", params)
    }

    def adminPost(String endpoint, def params, boolean throwExceptionOnFailure = true) {
        post(endpoint, "admin", params, HttpStatus.OK, throwExceptionOnFailure )
    }

    def supervisorPost(String endpoint, def params, boolean throwExceptionOnFailure = true) {
        post(endpoint, "supervisor", params, HttpStatus.OK, throwExceptionOnFailure )
    }

    def adminDelete(String endpoint, def params = null) {
        String url = "${skillsService}/admin${endpoint}${getUrlFromParams(params)}"
        delete(endpoint, "admin", params)
    }

    def apiDelete(String endpoint, def params = null) {
        String url = "${skillsService}/admin${endpoint}${getUrlFromParams(params)}"
        delete(endpoint, "api", params)
    }

    def adminGet(String endpoint, Map params = null) {
        return get(endpoint, "admin", params)
    }

    def adminUpload(String endpoint, Map params = null, boolean throwException = false) {
        String url = "${skillsService}/admin${endpoint}"
        log.info("MULTIPART POST: {}", url)
        return multipartPost(url, params, throwException)
    }

    def apiUpload(String endpoint, Map params = null) {
        String url = "${skillsService}/api${endpoint}"
        log.info("MULTIPART POST: {}", url)
        return multipartPost(url, params)
    }

    def supervisorUpload(String endpoint, Map params = null) {
        String url = "${skillsService}/supervisor${endpoint}"
        log.info("MULTIPART POST: {}", url)
        return multipartPost(url, params)
    }

    def supervisorPatch(String endpoint, def params, boolean throwExceptionOnFailure = true, MediaType mediaType = MediaType.APPLICATION_JSON) {
        patch(endpoint, "supervisor", params, HttpStatus.OK, throwExceptionOnFailure, mediaType)
    }

    def adminPatch(String endpoint, def params, boolean throwExceptionOnFailure = true, MediaType mediaType = MediaType.APPLICATION_JSON) {
        patch(endpoint, "admin", params, HttpStatus.OK, throwExceptionOnFailure, mediaType)
    }

    def rootGet(String endpoint) {
        return get(endpoint, 'root', null)
    }

    def rootPost(String endpoint, Map params = null) {
        return post(endpoint, 'root', params)
    }

    def rootPut(String endpoint, Map params = null) {
        return put(endpoint, 'root', params)
    }

    def rootDelete(String endpoint, Map params = null) {
        return delete(endpoint, 'root', params)
    }
    def createRootAccount(Map<String, String> userInfo) {
        return put('/createRootAccount', '', userInfo)
    }

    def grantRoot() {
        return post('/grantFirstRoot', '', null)
    }

    def isFeatureEnabled(String featureName) {
        return get('/isFeatureSupported', 'public', [feature: featureName])
    }

    def serverPut(String endpoint, def params) {
        put(endpoint, "server", params)
    }

    def serverPost(String endpoint, def params) {
       return post(endpoint, "server", params)
    }

    def proxyApiGet(String token, String endpoint, def params=null) {
        this.restTemplateWrapper.authenticationToken = "Bearer ${token}"
        def result = get(endpoint, "api", params)
        this.restTemplateWrapper.authenticationToken = null
        return result
    }

    def proxyApiPost(String token, String endpoint, def params) {
        this.restTemplateWrapper.authenticationToken = "Bearer ${token}"
        def result = post(endpoint, "api", params)
        this.restTemplateWrapper.authenticationToken = null
        return result
    }

    def proxyApiPut(String token, String endpoint, def params) {
        this.restTemplateWrapper.authenticationToken = "Bearer ${token}"
        def result = put(endpoint, "api", params)
        this.restTemplateWrapper.authenticationToken = null
        return result
    }

    def apiPost(String endpoint, def params = null) {
        return post(endpoint, "api", params)
    }

    def apiPut(String endpoint, def params) {
        return put(endpoint, "api", params)
    }

    def apiGet(String endpoint, Map params = null) {
        return get(endpoint, "api", params)
    }

    String getTokenForUser(String userId, boolean includeGrantType=true, boolean includeProxyUser=true) {
        log.info("Getting token for user [$userId]")
        String tokenUrl = "${skillsService}/oauth/token"
        HttpHeaders headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED)

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>()
        if (includeGrantType) {
            body.add('grant_type', 'client_credentials')
        } else {
            // do not include grant_type attribute for testing purposes
            log.warn("not including grant_type attribute")
        }

        if (includeProxyUser) {
            body.add('proxy_user', userId)
        } else {
            // do not include proxy_user attribute for testing purposes
            log.warn("not including proxy_user attribute")
        }

        ResponseEntity<OAuth2Response> responseEntity = oAuthRestTemplate.postForEntity(tokenUrl, new HttpEntity<>(body, headers), OAuth2Response)

        return responseEntity.body.accessToken
    }

    ResponseEntity<Resource> getResource(String endpoint, Map params=null) {
        String url = "${skillsService}/${endpoint}${getUrlFromParams(params)}"
        ResponseEntity<Resource> responseEntity = restTemplateWrapper.getForEntity(url, Resource)
        String resBody = responseEntity.body
        if(!resBody || (responseEntity.statusCode != HttpStatus.OK && responseEntity.statusCode != HttpStatus.PARTIAL_CONTENT)){
            String msg = "Bad request for [$url] code=${responseEntity.statusCode}"
            log.error(msg)
            SkillsClientException e = new SkillsClientException(msg, url, responseEntity.statusCode)
            e.resBody = resBody
            throw e
        }
        return responseEntity
    }

    public ResponseEntity<String> rawGet(String endpoint, def params) {
        String url = "${skillsService}/${endpoint}${getUrlFromParams(params)}"
        ResponseEntity<String> responseEntity = restTemplateWrapper.getForEntity(url, String)
        return responseEntity
    }

    def rawPost(String endpoint, def params, HttpStatus expectedStatus = HttpStatus.OK, boolean throwExceptionOnFailure = true) {
        String url = "${skillsService}/${endpoint}"
        ResponseEntity<String> responseEntity = restTemplateWrapper.postForEntity(url, params, String)
        return getResultFromEntity(url, responseEntity, expectedStatus, throwExceptionOnFailure)
    }

    private def put(String endpoint, String type, def params, HttpStatus expectedStatus = HttpStatus.OK, boolean throwExceptionOnFailure = true) {
        String url = "${skillsService}/${type}${endpoint}"
        log.info("PUT: {}, params={}", url, params)
        ResponseEntity<String> responseEntity = restTemplateWrapper.putForEntity(url, params)
        return getResultFromEntity(url, responseEntity, expectedStatus, throwExceptionOnFailure)
    }

    private def post(String endpoint, String type, def params, HttpStatus expectedStatus = HttpStatus.OK, boolean throwExceptionOnFailure = true) {
        String url = "${skillsService}/${type}${endpoint}".toString()
//        log.info("POST: {}, params={}", url, params)
        ResponseEntity<String> responseEntity = restTemplateWrapper.postForEntity(url, params, String)
        return getResultFromEntity(url, responseEntity, expectedStatus, throwExceptionOnFailure)
    }

    private def patch(String endpoint, String type, def params, HttpStatus expectedStatus = HttpStatus.OK, boolean throwExceptionOnFailure = true, MediaType mediaType= MediaType.APPLICATION_JSON) {
        String url = "${skillsService}/${type}${endpoint}".toString()
        log.info("PATCH: {}, params={}", url, params)
        HttpHeaders headers = new HttpHeaders()
        headers.setContentType(mediaType)
        HttpEntity entity = new HttpEntity(params, headers)
        ResponseEntity<String> responseEntity = restTemplateWrapper.patchForObject(url, entity, String)
        return getResultFromEntity(url, responseEntity, expectedStatus, throwExceptionOnFailure)
    }

    private def delete(String endpoint, String type, def params, HttpStatus expectedStatus = HttpStatus.OK, boolean throwExceptionOnFailure = true) {
        String url = "${skillsService}/${type}${endpoint}".toString()
        log.info("DELETE: {}, params={}", url, params)
        ResponseEntity<String> responseEntity = restTemplateWrapper.deleteForEntity(url, params, String)
        return getResultFromEntity(url, responseEntity, expectedStatus, throwExceptionOnFailure)
    }

    private def getResultFromEntity(String url, ResponseEntity<String> responseEntity, HttpStatus expectedStatus, boolean throwExceptionOnFailure) {

        String resBody = responseEntity.body
//        if(!resBody){
//            throw new IllegalArgumentException("Bad request for [$url], params=$params, code=${responseEntity.statusCode}")
//        }

        def res = ['statusCode': responseEntity.statusCode]
        if (responseEntity.statusCode.is2xxSuccessful()) {
            if(resBody) {
                log.info("  Result:\n {}", JsonOutput.prettyPrint(resBody))
                res['body'] = jsonSlurper.parseText(resBody)
            }else{
                log.info("request was successful but empty response body returned")
            }
            res['success'] = true
        } else {
            try {
                res['body'] = jsonSlurper.parseText(resBody)
            } catch (Exception e) {
                res['body'] = resBody
            }
            if (responseEntity.headers) {
                res.putAll(responseEntity.headers.toSingleValueMap())
            }

            if (throwExceptionOnFailure) {
                String msg = "Bad request for [$url]. Res: ${res['body']} code=${responseEntity.statusCode}"
                log.error(msg)
                SkillsClientException ex = new SkillsClientException(msg, url, responseEntity.statusCode)
                ex.res = res
                throw ex
//                throw new IllegalStateException("Request [$url] failed. Res: ${res['body']}".toString())
            }

            res['success'] = false
        }
        if (responseEntity.statusCode != expectedStatus) {
            log.error('Failed with {} code. {}', responseEntity.statusCode, res)
        }

        return res
    }

    private def get(String endpoint, String type, def params, boolean isResJson = true) {
        String url = "${skillsService}/${type}${endpoint}${getUrlFromParams(params)}"
        log.info("GET: {}", url)
        ResponseEntity<String> responseEntity = restTemplateWrapper.getForEntity(url, String)

        // every response must set client lib version
        assert responseEntity.headers.get("skills-client-lib-version")

        String resBody = responseEntity.body
        if((!resBody && isResJson) || responseEntity.statusCode != HttpStatus.OK){
            String msg = "Bad request for [$url] code=${responseEntity.statusCode}"
            log.error(msg)
            SkillsClientException e = new SkillsClientException(msg, url, responseEntity.statusCode)
            e.resBody = resBody
            throw e
        }
        def res = resBody
        if (isResJson) {
            log.info("  Result:\n {}", JsonOutput.prettyPrint(resBody))
            res = jsonSlurper.parseText(resBody)
        }
        return res
    }

    private def multipartPost(String endpoint, Map params, boolean throwException = false){
        HttpHeaders headers = new HttpHeaders()
        headers.setContentType(MediaType.MULTIPART_FORM_DATA)

        //TEMP



        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>()
        params.each { key, val ->
            if (val instanceof File) {
                body.add(key.toString(), new FileSystemResource(val))
            } else {
                body.add(key.toString(), val)
            }
        }

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers)

        ResponseEntity<String> responseEntity = restTemplateWrapper.postForEntity(endpoint, requestEntity, String)
        String resBody = responseEntity.body

        log.info("  Result:\n {}", JsonOutput.prettyPrint(resBody))

        def res = jsonSlurper.parseText(resBody)
        if(responseEntity.statusCode == HttpStatus.OK) {
            res['success' ] = true
        } else {
            res['success' ] = false
            if (throwException) {
                throw new SkillsClientException(resBody, endpoint, responseEntity.statusCode)
            }
        }

        return res
    }

    private String getUrlFromParams(Map params) {
        String url = ""
        if (params) {
            url += "?"
            url += params.entrySet().collect({ "${it.key}=${it.value}" }).join("&")
        }
        return url
    }

    static class OAuth2Response {
        @JsonProperty("access_token")
        String accessToken
        @JsonProperty("token_type")
        String tokenType
        @JsonProperty("expires_in")
        Long expiresIn
        String scope
        @JsonProperty("proxy_user")
        String proxyUser
        String jti
    }
}
