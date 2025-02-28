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
package skills.dbupgrade

import com.google.common.collect.Sets
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Conditional
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.http.server.ServletServerHttpResponse
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import skills.auth.UserInfo
import skills.controller.exceptions.ErrorCode
import skills.controller.request.model.SkillEventRequest
import skills.services.events.SkillEventResult

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.util.concurrent.TimeUnit

@Slf4j
@Component
@Order()
@Conditional(DBUpgrade.InProgress)
class UpgradeInProgressFilter extends OncePerRequestFilter {

    private static final Set<HttpMethod> ALLOWED_METHODS = Sets.newHashSet(HttpMethod.GET, HttpMethod.OPTIONS)
    private static final MediaType DEFAULT_MEDIA_TYPE = MediaType.APPLICATION_JSON
    public static final String ANONYMOUS_USER = "anonymousUser"


    @Autowired
    UpgradeSafeUrlDecider safeUrlDecider

    @Autowired
    ReportedSkillEventQueue skillEventQueue

    @Autowired
    List<HttpMessageConverter> configuredMessageConverters

    static class DbUpgradeErrBody {
        String explanation
        String errorCode = ErrorCode.DbUpgradeInProgress.toString()
        boolean success = false
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        final Authentication auth = SecurityContextHolder.getContext()?.getAuthentication()

        final HttpMethod method = HttpMethod.valueOf(request.getMethod())
        final String uri = request.getRequestURI()

        if (!auth?.isAuthenticated() || ANONYMOUS_USER.equals(auth.getPrincipal())) {
            log.warn("request for [{}] is not authenticated, UpgradeInProgressFilter cannot process", uri)
            filterChain.doFilter(request, response)
            return
        }

        if (ALLOWED_METHODS.contains(method)) {
            log.debug("request [{}] is allowed while db upgrade is in progress", uri)
            filterChain.doFilter(request, response)
            return
        }

        Object principal = auth.getPrincipal()
        log.debug("extracted principal type of [{}]", principal?.getClass())
        UserInfo userInfo = null
        if (principal instanceof UserInfo) {
            userInfo = (UserInfo)principal
        } else if (principal instanceof User) {
            userInfo = new DelegatingUserInfo((User)principal)
        } else {
            log.error("Unrecognized Authentication principal [{}]", principal.getClass())
        }

        QueuedSkillEvent queuedSkillEvent = safeUrlDecider.isSkillEventReport(uri, method)
        ServletServerHttpRequest serverHttpRequest = new ServletServerHttpRequest(request)
        if (queuedSkillEvent) {
            SkillEventRequest skillEventRequest = readEventRequest(serverHttpRequest)
            // id type does not make sense if userId is not provided
            if (skillEventRequest && StringUtils.isBlank(skillEventRequest?.userId)) {
                skillEventRequest.idType = null
            }
            queuedSkillEvent.skillEventRequest = skillEventRequest
            queuedSkillEvent.userId = userInfo?.username
            if (userInfo?.userDn) {
                queuedSkillEvent.userId = userInfo?.userDn
            }
            skillEventQueue.queueEvent(queuedSkillEvent)
            SkillEventResult eventResult = new SkillEventResult()
            eventResult.projectId = queuedSkillEvent.projectId
            eventResult.skillId = queuedSkillEvent.skillId
            eventResult.skillApplied = false
            eventResult.explanation = "A database upgrade is currently in progress. This Skill Event Request has been queued for future application."
            boolean success = writeResponse(response, serverHttpRequest, SkillEventResult.class, eventResult)
            if (!success) {
                log.error("unable to write response to client request for [{}] with accept headers of [{}]", uri, serverHttpRequest.getHeaders().getAccept())
            }
        } else if (safeUrlDecider.isUrlAllowed(uri, method)) {
            log.info("[{}] request [{}] has been annotated as DBUpgradeSafe and is allowed while db upgrade is in progress", method, uri)
            filterChain.doFilter(request, response)
        } else {
            log.info("POST/PUT/DELETE request to [{}] is not allowed, user [{}], database upgrade is currently in progress", uri, userInfo?.username)
            DbUpgradeErrBody basicErrBody = new DbUpgradeErrBody(explanation: "A database upgrade is currently in progress, no training profile modifications are allowed at this time.")
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
            boolean success = writeResponse(response, serverHttpRequest, DbUpgradeErrBody.class, basicErrBody)
            if (!success) {
                log.error("unable to write response to client request for [{}] with accept headers of [{}]", uri, serverHttpRequest.getHeaders().getAccept())
            }
            return;
        }
    }

    private SkillEventRequest readEventRequest(ServletServerHttpRequest serverHttpRequest) {
        MediaType mediaType = serverHttpRequest.getHeaders().getContentType()
        if (serverHttpRequest?.getHeaders() && serverHttpRequest.getHeaders().getContentLength() > 0) {
            for (HttpMessageConverter messageConverter : configuredMessageConverters) {
                if (messageConverter.canRead(SkillEventRequest, mediaType)) {
                    SkillEventRequest skillEventRequest = (SkillEventRequest) messageConverter.read(SkillEventRequest, serverHttpRequest)
                    return skillEventRequest
                }
            }
        }
        return null
    }

    private boolean writeResponse(HttpServletResponse response, ServletServerHttpRequest serverHttpRequest, Class clazz, Object eventResult) {
        ServletServerHttpResponse serverHttpResponse = new ServletServerHttpResponse(response)
        List<MediaType> acceptTypes = serverHttpRequest.getHeaders().getAccept()
        if (!acceptTypes){
            acceptTypes = [DEFAULT_MEDIA_TYPE]
        }

        for (MediaType accept : acceptTypes) {
            for (HttpMessageConverter messageConverter : configuredMessageConverters) {
                if (messageConverter.canWrite(clazz, accept)) {
                    messageConverter.write(eventResult, accept, serverHttpResponse)
                    return true
                }
            }
        }
        return false
    }


}
