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
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.client.RestTemplate
import skills.auth.UserInfo
import skills.auth.UserInfoService
import skills.controller.request.model.PageVisitRequest
import skills.controller.result.model.RequestResult
import skills.services.events.SkillEventResult

import jakarta.annotation.PostConstruct

import java.util.concurrent.RejectedExecutionException

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

    @Value('${skills.config.ui.enablePageVisitReporting:#{false}}')
    Boolean enablePageVisitReporting

    @Value('#{"${skills.external.metrics.queueCapacity:1000}"}')
    int queueCapacity

    @Autowired
    UserInfoService userInfoService

    private RestTemplate restTemplate
    private CachedThreadPool pool

    static enum MetricType {
        PageVisit, SkillReported
    }

    @PostConstruct
    void init() {
        if (enabled) {
            log.info("Enabling external tool reporting to endpoint [{}]", endpointUrl)
            assert endpointUrl
            restTemplate = new RestTemplate();
            pool = new CachedThreadPool('metrics-logger', minNumOfThreads, maxNumOfThreads, queueCapacity)
            if (enablePageVisitReporting) {
                log.info("Page visit reporting is enabled")
            }
        }
    }

    @Profile
    void logSkillReported(String userId, SkillEventResult result) {
        log([
                'type'           : MetricType.SkillReported.toString(),
                'skillId'        : result.skillId,
                'projectId'      : result.projectId,
                'requestedUserId': userId,
                'selfReported'   : StringUtils.isNotEmpty(result.selfReportType).toString(),
                'selfReportType' : result.selfReportType,
        ])
    }

    @Profile
    void logPageVisit(PageVisitRequest pageVisitRequest) {
        if (enabled && enablePageVisitReporting) {
            String userId = userInfoService.getUserName(null)
            log([
                    'type'               : MetricType.PageVisit.toString(),
                    'path'               : pageVisitRequest.path,
                    'fullPath'           : pageVisitRequest.fullPath,
                    'hostname'           : pageVisitRequest.hostname,
                    'port'               : pageVisitRequest.port?.toString(),
                    'protocol'           : pageVisitRequest.protocol,
                    'requestedUserId'    : userId,
                    'isFromSkillsDisplay': pageVisitRequest.skillDisplay?.toString(),
                    'projectId'          : pageVisitRequest.projectId,
            ])
        }
    }

    @Profile
    private void log(Map<String, String> attributes = [:]) {
        if (enabled) {
            // user attributes must be obtained from the current thread
            attributes.putAll(getUserAttributes())

            try {
                // report to external service in a separate thread
                pool.submit([ThreadPoolUtils.callable {
                    MetricsMessage message = new MetricsMessage(attributes)
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    HttpEntity<MetricsMessage> entity = new HttpEntity<>(message, headers);
                    try {
                        if (log.isDebugEnabled()) {
                            long startTime = System.currentTimeMillis();
                            restTemplate.put(endpointUrl, entity);
                            long duration = System.currentTimeMillis() - startTime;
                            log.debug("Metrics request to [{}] completed in [{}] ms, with attributes {}", endpointUrl, duration, attributes);
                        } else {
                            restTemplate.put(endpointUrl, entity);
                        }
                    } catch (Exception ex) {
                        log.error("Unable to report to external metrics service", ex)
                    }
                }])
            } catch (RejectedExecutionException ree) {
                log.error("Queue is full with [${queueCapacity}] items. Request was rejected for: ${attributes}, err msg: ${ree.message}")
            }
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
