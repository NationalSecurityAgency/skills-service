package skills.dbupgrade

import com.google.common.collect.Sets
import groovy.util.logging.Slf4j
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
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import skills.auth.UserInfo
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.request.model.SkillEventRequest
import skills.services.events.SkillEventResult

import javax.annotation.PostConstruct
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Slf4j
@Component
@Order()
@Conditional(DBUpgrade.InProgress)
class UpgradeInProgressFilter extends OncePerRequestFilter {

    private static Set<HttpMethod> ALLOWED_METHODS = Sets.newHashSet(HttpMethod.GET, HttpMethod.OPTIONS)
    public static final String ANONYMOUS_USER = "anonymousUser"

    @Autowired
    UpgradeSafeUrlDecider safeUrlDecider

    @Autowired
    ReportedSkillEventQueue skillEventQueue

    @Autowired
    List<HttpMessageConverter> configuredMessageConverters

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        final Authentication auth = SecurityContextHolder.getContext()?.getAuthentication()

        final HttpMethod method = HttpMethod.resolve(request.getMethod())
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

        UserInfo userInfo = (UserInfo)auth.getPrincipal()

        QueuedSkillEvent queuedSkillEvent = safeUrlDecider.isSkillEventReport(uri, method)
        if (queuedSkillEvent) {
            SkillEventRequest skillEventRequest = readEventRequest(request)
            queuedSkillEvent.skillEventRequest = skillEventRequest
            queuedSkillEvent.userId = userInfo.username
            skillEventQueue.queueEvent(queuedSkillEvent)
            SkillEventResult eventResult = new SkillEventResult()
            eventResult.projectId = queuedSkillEvent.projectId
            eventResult.skillId = queuedSkillEvent.skillId
            eventResult.skillApplied = false
            eventResult.explanation = "A database upgrade is currently in progress. This Skill Event Request has been queued for future application."
            writeResponse(response, eventResult)
        } else if (safeUrlDecider.isUrlAllowed(uri, method)) {
            log.info("request [{}] has been annotated as DBUpgradeSafe and is allowed while db upgrade is in progress", uri)
            filterChain.doFilter(request, response)
        } else {
            throw new SkillException("${uri} is not allowed, A database upgrade is currently in progress, no training profile modifications are allowed at this time", ErrorCode.DbUpgradeInProgress)
        }
    }

    private SkillEventRequest readEventRequest(HttpServletRequest request) {
        ServletServerHttpRequest serverHttpRequest = new ServletServerHttpRequest(request)
        MediaType mediaType = serverHttpRequest.getHeaders().getContentType()

        for (HttpMessageConverter messageConverter : configuredMessageConverters) {
            if (messageConverter.canRead(SkillEventRequest, mediaType)) {
                SkillEventRequest skillEventRequest = (SkillEventRequest)messageConverter.read(SkillEventRequest, serverHttpRequest)
                return skillEventRequest
            }
        }
    }

    private void writeResponse(HttpServletResponse response, SkillEventResult eventResult) {
        ServletServerHttpResponse serverHttpResponse = new ServletServerHttpResponse(response)
        List<MediaType> acceptTypes = serverHttpResponse.getHeaders().getAccept()
        for (MediaType accept : acceptTypes) {
            for (HttpMessageConverter messageConverter : configuredMessageConverters) {
                if (messageConverter.canWrite(SkillEventResult, accept)) {
                    messageConverter.write(eventResult, accept, serverHttpResponse)
                    return
                }
            }
        }
    }


}
