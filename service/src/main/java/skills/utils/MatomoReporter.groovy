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
package skills.utils

import callStack.profiler.Profile
import callStack.utils.CachedThreadPool
import callStack.utils.ThreadPoolUtils
import groovy.transform.Canonical
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import skills.auth.AuthMode
import skills.auth.UserInfoService
import skills.auth.pki.PkiUserLookup
import skills.services.inception.InceptionProjectService
import skills.storage.repos.UserAttrsRepo

import java.util.concurrent.RejectedExecutionException

@Component
@Slf4j
class MatomoReporter {

    @Value('#{"${skills.matomo.enableSkillApiUsage:false}"}')
    Boolean enableSkillApiUsageToMatomo

    @Value('${skills.config.ui.matomoUrl:#{null}}')
    String matomoUrl

    @Value('${skills.config.ui.matomoSiteId:#{null}}')
    String matomoSiteId

    @Value('#{"${skills.matomo.endpoint:/matomo.php}"}')
    String matomoEndpoint

    @Value('${skills.matomo.rec:1}')
    Integer matomoRec

    @Value('#{"${skills.matomo.minNumOfThreads:3}"}')
    Integer minNumOfThreads

    @Value('#{"${skills.matomo.maxNumOfThreads:10}"}')
    Integer maxNumOfThreads

    @Value('#{"${skills.matomo.actionRootName:Report Skill}"}')
    String actionRootName

    @Value('#{"${skills.matomo.queueCapacity:20000}"}')
    int queueCapacity

    @Value('${skills.authorization.authMode:#{T(skills.auth.AuthMode).DEFAULT_AUTH_MODE}}')
    AuthMode authMode

    @Autowired(required = false)
    PkiUserLookup pkiUserLookup

    @Autowired
    UserInfoService userInfoService

    @Autowired
    UserAttrsRepo userAttrsRepo

    private RestTemplate restTemplate
    private CachedThreadPool pool
    private String matomoUrlWithEndpoint

    @PostConstruct
    void init() {
        if (isEnabled()) {
            restTemplate = new RestTemplate();
            pool = new CachedThreadPool('matomo-reporter', minNumOfThreads, maxNumOfThreads, queueCapacity)
            matomoUrlWithEndpoint = matomoUrl.replaceAll('/+$', '') + '/' + matomoEndpoint.replaceAll('^/+', '')
            log.info("MatomoReporter is enabled: " +
                    "endpoint=[${matomoUrlWithEndpoint}], " +
                    "site id=[${matomoSiteId}], " +
                    "min threads=[${minNumOfThreads}], " +
                    "max threads=[${maxNumOfThreads}], " +
                    "queue capacity=[${queueCapacity}]")
        }
    }

    boolean isEnabled() {
        return enableSkillApiUsageToMatomo && matomoUrl
    }

    @Canonical
    @ToString(includeNames = true)
    static class LocalRequestInfo {
        String scheme
        String localName
        String referer
        String userAgent
        String acceptLanguage
        String uid
        String projectId
        String skillId
    }

    private static HttpServletRequest getServletRequest() {
        try {
            ServletRequestAttributes currentRequestAttributes = RequestContextHolder.getRequestAttributes() as ServletRequestAttributes
            return currentRequestAttributes?.getRequest()
        } catch (Exception e) {
            log.warn("Unable to access current HttpServletRequest. Error Recieved [$e]. Will not be able to accurately report to Matomo.", e)
        }
        return null
    }

    @Profile
    void reportSkill(String userId, String projectId, String skillId) {
        if (!isEnabled()) {
            return
        }
        HttpServletRequest request = getServletRequest()

        if (log.isTraceEnabled()) {
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                log.trace("Header: [{}] = [{}]", headerName, request.getHeader(headerName));
            }
        }

        String uid = (authMode == AuthMode.PKI) ? userAttrsRepo.findByUserIdIgnoreCase(userId).dn : userId
        LocalRequestInfo requestInfo = new LocalRequestInfo (
                scheme: request?.getScheme(),
                localName: request?.getLocalName(),
                referer: request?.getHeader(HttpHeaders.REFERER),
                userAgent: request?.getHeader(HttpHeaders.USER_AGENT),
                acceptLanguage: request?.getHeader(HttpHeaders.ACCEPT_LANGUAGE),
                uid: uid,
                projectId: projectId,
                skillId: skillId
        )

        try {
            pool.submit([ThreadPoolUtils.callable {
                sendToMatomo(requestInfo)
            }])
        } catch (RejectedExecutionException ree) {
            log.error("Queue is full with [${queueCapacity}] items. Request was rejected for: ${requestInfo.toString()}, err msg: ${ree.message}")
        }
    }

    private void sendToMatomo(LocalRequestInfo requestInfo) {

        if (InceptionProjectService.inceptionProjectId.equalsIgnoreCase(requestInfo.projectId)) {
            log.trace("Skipping Matomo tracking for Inception project")
            return
        }

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>()
        formData.add("idsite", matomoSiteId?.toString())
        formData.add("rec", matomoRec?.toString())
        formData.add("uid", requestInfo.uid)

        String urlParam = "${requestInfo.scheme}://${requestInfo.localName}/api/projects/${requestInfo.projectId}/skills/${requestInfo.skillId}".toString()
        formData.add("url", urlParam)
        formData.add("action_name", "${actionRootName} / ${requestInfo.projectId} / ${requestInfo.skillId}".toString())

        formData.add("urlref", requestInfo.referer)
        formData.add("ua", requestInfo.userAgent)
        formData.add("lang", requestInfo.acceptLanguage)

        HttpHeaders headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED)
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(formData, headers)
        try {
            if (log.isDebugEnabled()) {
                long startTime = System.currentTimeMillis();
                restTemplate.exchange(matomoUrlWithEndpoint, HttpMethod.POST, entity, Void.class)
                long duration = System.currentTimeMillis() - startTime;
                log.debug("Matomo request to [{}] completed in [{}] ms, with formData {}", matomoUrlWithEndpoint, duration, formData);
            } else {
                restTemplate.exchange(matomoUrlWithEndpoint, HttpMethod.POST, entity, Void.class)
                log.debug("Matomo request to [{}] with formData {}", matomoUrlWithEndpoint, formData);
            }
        } catch (Exception ex) {
            log.error("Unable to report to Matomo", ex)
        }
    }
}
