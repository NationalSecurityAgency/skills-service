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
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Conditional
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import skills.auth.SecurityMode
import skills.auth.UserAuthService
import skills.auth.UserInfo
import skills.controller.exceptions.SkillException

import java.util.concurrent.TimeUnit

@Component
@Conditional(SecurityMode.PkiAuth)
@Slf4j
class PkiUserLookup {

//    use @Autowired if you want to utilize apache HttpClient (see HttpClientRestTemplateConfig)
    @Autowired
    RestTemplate restTemplate

    @Value('${skills.authorization.userInfoUri}')
    String userInfoUri

    @Value('${skills.authorization.userQueryUri}')
    String userQueryUri

    @Value('${skills.authorization.suggestOptionParam:""}')
    String suggestOptionParam

    @Value('${skills.authorization.userInfoHealthCheckUri}')
    String userInfoHealthCheckUri

    //  24 hours = 86400000 milliseconds = 24 * 60 * 60 * 1000
    @Value('#{"${skills.authorization.userInfoCache.expiration.milliseconds:86400000}"}')
    Long cacheExpirationInMs

    @Value('#{"${skills.authorization.userInfoCache.maxSize:10000}"}')
    Long cacheMaxSize

    @Autowired
    UserAuthService userAuthService

    LoadingCache userInfoCache

    private Timer timer

    @PostConstruct
    void configureCache() {
        userInfoCache = CacheBuilder.newBuilder().expireAfterWrite(cacheExpirationInMs, TimeUnit.MILLISECONDS).maximumSize(cacheMaxSize).recordStats().build(new CacheLoader<String, UserInfo>() {
            @Override
            UserInfo load(String dn) throws Exception {
                String issuerDn = getIssuerDn()
                try {
                    UserInfo userInfo = restTemplate.getForObject(userInfoUri, UserInfo, dn, issuerDn)
                    validate(userInfo, dn)
                    createOrUpdateUser(userInfo)
                    return userInfo
                } catch (Throwable t) {
                    log.error("Failed while calling [${userInfoUri}], dn=[${dn}], issuerDn=[${issuerDn}]", t)
                    throw t
                }
            }
        })

        if (log.isDebugEnabled()) {
            timer = new Timer()
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                void run() {
                    printCacheStats()
                }
            }, TimeUnit.MINUTES.toMillis(5), TimeUnit.MINUTES.toMillis(5))
        }
    }

    @PreDestroy
    void destroy(){
        if (timer != null) {
            timer.cancel()
            timer.purge()
        }
    }

    @Profile
    UserInfo lookupUserDn(String dn) {
        UserInfo userInfo = userInfoCache.getUnchecked(dn)
        return userInfo
    }

    boolean isUserDnCurrentlyCached(String dn) {
        return userInfoCache.asMap().containsKey(dn)
    }

    void printCacheStats() {
        CacheStats stats = userInfoCache.stats()
        log.debug("\n\nCacheStats: \n${stats}\n\n")
    }

    void invalidateEntireCache() {
        userInfoCache.invalidateAll()
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
        return restTemplate.getForObject(userInfoHealthCheckUri, Status).status == Status.STATUS.OK
    }

    private void validate(UserInfo userInfo, String requestValue) {
        String errMsg = null
        if (!userInfo) {
            errMsg = "User info service does not have key [${requestValue}]"
        } else if (!userInfo?.username) {
            errMsg = "User info service result must contain username. request value=[${requestValue}]"
        } else if (!userInfo?.usernameForDisplay) {
            errMsg = "User info service result must contain usernameForDisplay. request value=[${requestValue}]"
        }

        if (errMsg != null) {
            def e = new SkillException(errMsg)
            e.doNotRetry = true
            throw e
        }
    }

    private String getIssuerDn() {
        return ExtractCertUtil.getIssuerDn(getServletRequest())
    }

    private HttpServletRequest getServletRequest() {
        HttpServletRequest httpServletRequest
        try {
            ServletRequestAttributes currentRequestAttributes = RequestContextHolder.getRequestAttributes() as ServletRequestAttributes
            httpServletRequest = currentRequestAttributes?.getRequest()
        } catch (Exception e) {
            log.warn("Unable to access current HttpServletRequest. Error Recieved [$e]", e)
        }
        return httpServletRequest
    }

    void createOrUpdateUser(UserInfo userInfo) {
        try {
            userAuthService.createOrUpdateAppUser(userInfo)
        } catch (Throwable e) {
            log.error("Error during createOrUpdateUser", e);
            SkillException sk = new SkillException("Failed to createOrUpdateUser user info for [${userInfo.username}]")
            sk.doNotRetry = true
            throw sk
        }
    }

    static class Status {
        enum STATUS { OK }
        STATUS status
    }
}
