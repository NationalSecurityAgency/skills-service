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
package skills.auth.pki

import callStack.profiler.Profile
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.CacheStats
import com.google.common.cache.LoadingCache
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Conditional
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import skills.auth.SecurityMode
import skills.auth.UserInfo
import skills.controller.exceptions.SkillException

import javax.annotation.PostConstruct
import java.util.concurrent.TimeUnit

@Component
@Conditional(SecurityMode.PkiAuth)
@Slf4j
class PkiUserLookup {

//    use @Autowired if you want to utilize apache HttpClient (see HttpClientRestTemplateConfig)
    RestTemplate restTemplate = new RestTemplate()

    @Value('${skills.authorization.userInfoUri}')
    String userInfoUri

    @Value('${skills.authorization.userQueryUri}')
    String userQueryUri

    @Value('${skills.authorization.suggestOptionParam:""}')
    String suggestOptionParam

    @Value('${skills.authorization.userInfoHealthCheckUri}')
    String userInfoHealthCheckUri

    @Value('#{"${skills.authorization.userInfoCache.expiration.hours:24}"}')
    Long cacheExpirationHours

    @Value('#{"${skills.authorization.userInfoCache.maxSize:10000}"}')
    Long cacheMaxSize

    LoadingCache userInfoCache

    @PostConstruct
    void configureCache() {
        userInfoCache = CacheBuilder.newBuilder().expireAfterWrite(cacheExpirationHours, TimeUnit.HOURS).maximumSize(cacheMaxSize).recordStats().build(new CacheLoader<String, UserInfo>() {
            @Override
            UserInfo load(String dn) throws Exception {
                UserInfo userInfo = restTemplate.getForObject(userInfoUri, UserInfo, dn)
                validate(userInfo, dn)
                return userInfo
            }
        })
    }

    @Profile
    UserInfo lookupUserDn(String dn) {
        UserInfo userInfo = userInfoCache.getUnchecked(dn)
        return userInfo
    }

    void printCacheStats() {
        CacheStats stats = userInfoCache.stats()
        log.info("\n\nCacheStats: \n${stats}\n\n")
    }

    @Profile
    List<UserInfo> suggestUsers(String query, String suggestOption) {
        String uri = userQueryUri
        if(suggestOption && suggestOptionParam) {
            if (uri.endsWith("/")){
                uri = uri.substring(0, uri.length()-1)
            }
            if (uri.contains("?")){
                uri += "&$suggestOptionParam=$suggestOption"
            } else {
                uri += "?$suggestOptionParam=$suggestOption"
            }
        }

        ResponseEntity<List<UserInfo>> response = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<UserInfo>>(){},
                query)
        List<UserInfo> matches = response.getBody()
        for (UserInfo userInfo : matches) {
            validate(userInfo, query)
        }
        return matches
    }

    @Profile
    boolean isServiceAvailable() {
        return restTemplate.getForObject(userInfoHealthCheckUri, Status).status == Status.STATUS.UP
    }

    private void validate(UserInfo userInfo, String requestValue) {
        String errMsg = null
        if (!userInfo) {
            errMsg = "User info service does not have key [${requestValue}]"
        }

        if (!userInfo?.username) {
            errMsg = "User info service result must contain username. request value=[${requestValue}]"
        }
        if (!userInfo?.usernameForDisplay) {
            errMsg = "User info service result must contain usernameForDisplay. request value=[${requestValue}]"
        }

        if (errMsg != null) {
            def e = new SkillException(errMsg)
            e.doNotRetry = true
            throw e
        }
    }

    static class Status {
        enum STATUS { UP }
        STATUS status
    }
}
