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
package skills.utils

import callStack.profiler.Profile
import callStack.utils.CachedThreadPool
import callStack.utils.ThreadPoolUtils
import groovy.transform.Canonical
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import skills.auth.UserInfo
import skills.auth.UserInfoService

import javax.annotation.PostConstruct

@Component
@Slf4j
class MetricsLogger {

    @Value('#{"${skills.external.metrics.enabled:false}"}')
    Boolean enabled

    @Value('${skills.external.metrics.endpoint.url:#{null}}')
    String endpointUrl

    @Value('#{"${skills.external.metrics.minNumOfThreads:3}"}')
    Integer minNumOfThreads

    @Value('#{"${skills.external.metrics.maxNumOfThreads:10}"}')
    Integer maxNumOfThreads

    @Autowired
    UserInfoService userInfoService

    private RestTemplate restTemplate
    private CachedThreadPool pool

    @PostConstruct
    void init() {
        if (enabled) {
            log.info("Enabling external tool reporting to endpoint [{}]", endpointUrl)
            restTemplate = new RestTemplate();
            pool = new CachedThreadPool('metrics-logger', minNumOfThreads, maxNumOfThreads)
        }
    }

    @Profile
    void log(Map<String, String> attributes = [:]) {
        if (enabled) {
            // user attributes must be obtained from the current thread
            attributes.putAll(getUserAttributes())

            // report to external service in a separate thread
            pool.submit([ThreadPoolUtils.callable {
                MetricsMessage message = new MetricsMessage(attributes)
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<MetricsMessage> entity = new HttpEntity<>(message, headers);
                try {
                    restTemplate.put(endpointUrl, entity);
                } catch(Exception ex) {
                    log.error("Unable to report to external metrics service", ex)
                }
            }])
        }
    }

    Map<String, String> getUserAttributes() {
        Map<String, String> userAttributes = [:]
        UserInfo userInfo = userInfoService.getCurrentUser()
        if (userInfo) {
            if (StringUtils.isNotBlank(userInfo.userDn)) {
                userAttributes.put('currentUserDn', userInfo.userDn)
            } else if (StringUtils.isNotBlank(userInfo.email)) {
                userAttributes.put('currentUserEmail', userInfo.email)
            }
        }
        return userAttributes
    }

    @Canonical
    static class MetricsMessage {
        Map<String, String> attributes = [:]
    }
}
