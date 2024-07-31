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

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import skills.auth.UserInfo
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.request.model.SkillEventRequest
import skills.services.events.SkillEventResult
import spock.lang.IgnoreRest
import spock.lang.Specification

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletResponse

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*

class UpgradeInProgressFilterSpec extends Specification {

    def "requests to add skill events are queued"() {
        def upgradeSafeUrlDecider = Mock(UpgradeSafeUrlDecider)
        def skillEventQueue = Mock(ReportedSkillEventQueue)
        def mockMessageConverter = Mock(HttpMessageConverter)
        def mockResponse = Mock(HttpServletResponse )
        def mockFilterChain = Mock(FilterChain)
        def authentication = Mock(Authentication)
        def securityContext = Mock(SecurityContext)
        def userInfo = Mock(UserInfo)


        //need to properly mock the HttpServletRequest headers not the spring class as that's created after we run

        securityContext.getAuthentication() >> authentication
        authentication.isAuthenticated() >> true
        authentication.getPrincipal() >> userInfo
        userInfo.getUsername() >> "userMakingRequest"

        SecurityContextHolder.setContext(securityContext)

        Date requestTime = new Date()
        QueuedSkillEvent queuedSkillEvent = new QueuedSkillEvent(projectId: "foo", skillId: "skill", requestTime: requestTime)

        MockHttpServletRequest mockRequest = post("/projects/foo/skills/skill")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{fake}").buildRequest()

        when:
        UpgradeInProgressFilter progressFilter = new UpgradeInProgressFilter()
        progressFilter.configuredMessageConverters = [mockMessageConverter]
        progressFilter.safeUrlDecider = upgradeSafeUrlDecider
        progressFilter.skillEventQueue = skillEventQueue

        progressFilter.doFilter(mockRequest, mockResponse, mockFilterChain)

        then:
        1 * upgradeSafeUrlDecider.isSkillEventReport("/projects/foo/skills/skill", HttpMethod.POST) >> queuedSkillEvent
        1 * mockMessageConverter.canRead(SkillEventRequest, MediaType.APPLICATION_JSON) >> true
        1 * mockMessageConverter.read(SkillEventRequest, _) >> new SkillEventRequest(userId: "aUser")
        1 * skillEventQueue.queueEvent({ QueuedSkillEvent qe ->
            qe.userId == "userMakingRequest" &&
            qe.skillId == "skill" &&
            qe.projectId == "foo"
            qe.skillEventRequest.userId == "aUser" &&
            qe.requestTime == requestTime
        })
        1 * mockMessageConverter.canWrite(SkillEventResult, _) >> true
        1 * mockMessageConverter.write({ SkillEventResult ser ->
            ser.projectId == "foo" &&
            ser.skillId == "skill" &&
            !ser.skillApplied &&
            ser.explanation == "A database upgrade is currently in progress. This Skill Event Request has been queued for future application."
        }, _, _)
    }

    def "GET requests are ignored by the filter"() {
        def upgradeSafeUrlDecider = Mock(UpgradeSafeUrlDecider)
        def skillEventQueue = Mock(ReportedSkillEventQueue)
        def mockMessageConverter = Mock(HttpMessageConverter)
        def mockResponse = Mock(HttpServletResponse )
        def mockFilterChain = Mock(FilterChain)
        def authentication = Mock(Authentication)
        def securityContext = Mock(SecurityContext)
        def userInfo = Mock(UserInfo)

        securityContext.getAuthentication() >> authentication
        authentication.isAuthenticated() >> true
        authentication.getPrincipal() >> userInfo
        userInfo.getUsername() >> "userMakingRequest"

        SecurityContextHolder.setContext(securityContext)


        MockHttpServletRequest mockRequest = get("/projects/foo")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).buildRequest()

        when:
        UpgradeInProgressFilter progressFilter = new UpgradeInProgressFilter()
        progressFilter.configuredMessageConverters = [mockMessageConverter]
        progressFilter.safeUrlDecider = upgradeSafeUrlDecider
        progressFilter.skillEventQueue = skillEventQueue

        progressFilter.doFilter(mockRequest, mockResponse, mockFilterChain)

        then:
        1 * mockFilterChain.doFilter(mockRequest, mockResponse)
    }

    def "OPTIONS requests are ignored by the filter"() {
        def upgradeSafeUrlDecider = Mock(UpgradeSafeUrlDecider)
        def skillEventQueue = Mock(ReportedSkillEventQueue)
        def mockMessageConverter = Mock(HttpMessageConverter)
        def mockResponse = Mock(HttpServletResponse )
        def mockFilterChain = Mock(FilterChain)
        def authentication = Mock(Authentication)
        def securityContext = Mock(SecurityContext)
        def userInfo = Mock(UserInfo)

        securityContext.getAuthentication() >> authentication
        authentication.isAuthenticated() >> true
        authentication.getPrincipal() >> userInfo
        userInfo.getUsername() >> "userMakingRequest"

        SecurityContextHolder.setContext(securityContext)


        MockHttpServletRequest mockRequest = options("/projects/foo")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).buildRequest()

        when:
        UpgradeInProgressFilter progressFilter = new UpgradeInProgressFilter()
        progressFilter.configuredMessageConverters = [mockMessageConverter]
        progressFilter.safeUrlDecider = upgradeSafeUrlDecider
        progressFilter.skillEventQueue = skillEventQueue

        progressFilter.doFilter(mockRequest, mockResponse, mockFilterChain)

        then:
        1 * mockFilterChain.doFilter(mockRequest, mockResponse)
    }

    def "unauthenticated requests are ignored by the filter"() {
        def upgradeSafeUrlDecider = Mock(UpgradeSafeUrlDecider)
        def skillEventQueue = Mock(ReportedSkillEventQueue)
        def mockMessageConverter = Mock(HttpMessageConverter)
        def mockResponse = Mock(HttpServletResponse )
        def mockFilterChain = Mock(FilterChain)
        def authentication = Mock(Authentication)
        def securityContext = Mock(SecurityContext)

        securityContext.getAuthentication() >> authentication
        authentication.isAuthenticated() >> false

        SecurityContextHolder.setContext(securityContext)


        MockHttpServletRequest mockRequest = get("/projects/foo")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).buildRequest()

        when:
        UpgradeInProgressFilter progressFilter = new UpgradeInProgressFilter()
        progressFilter.configuredMessageConverters = [mockMessageConverter]
        progressFilter.safeUrlDecider = upgradeSafeUrlDecider
        progressFilter.skillEventQueue = skillEventQueue

        progressFilter.doFilter(mockRequest, mockResponse, mockFilterChain)

        then:
        1 * mockFilterChain.doFilter(mockRequest, mockResponse)
    }

    def "anonymous requests are ignored by the filter"() {
        def upgradeSafeUrlDecider = Mock(UpgradeSafeUrlDecider)
        def skillEventQueue = Mock(ReportedSkillEventQueue)
        def mockMessageConverter = Mock(HttpMessageConverter)
        def mockResponse = Mock(HttpServletResponse )
        def mockFilterChain = Mock(FilterChain)
        def authentication = Mock(Authentication)
        def securityContext = Mock(SecurityContext)
        def userInfo = Mock(UserInfo)
        def mockHeaders = Mock(HttpHeaders)

        securityContext.getAuthentication() >> authentication
        authentication.isAuthenticated() >> true
        authentication.getPrincipal() >> "anonymousUser"

        SecurityContextHolder.setContext(securityContext)


        MockHttpServletRequest mockRequest = post("/projects/foo")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).buildRequest()

        when:
        UpgradeInProgressFilter progressFilter = new UpgradeInProgressFilter()
        progressFilter.configuredMessageConverters = [mockMessageConverter]
        progressFilter.safeUrlDecider = upgradeSafeUrlDecider
        progressFilter.skillEventQueue = skillEventQueue

        progressFilter.doFilter(mockRequest, mockResponse, mockFilterChain)

        then:
        1 * mockFilterChain.doFilter(mockRequest, mockResponse)
    }

    def "safe POST/PUT methods are allowed"() {
        def upgradeSafeUrlDecider = Mock(UpgradeSafeUrlDecider)
        def skillEventQueue = Mock(ReportedSkillEventQueue)
        def mockMessageConverter = Mock(HttpMessageConverter)
        def mockResponse = Mock(HttpServletResponse )
        def mockFilterChain = Mock(FilterChain)
        def authentication = Mock(Authentication)
        def securityContext = Mock(SecurityContext)
        def userInfo = Mock(UserInfo)

        securityContext.getAuthentication() >> authentication
        authentication.isAuthenticated() >> true
        authentication.getPrincipal() >> userInfo
        userInfo.getUsername() >> "userMakingRequest"

        SecurityContextHolder.setContext(securityContext)

        MockHttpServletRequest mockRequest = post("/random")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{fake}").buildRequest()

        when:
        UpgradeInProgressFilter progressFilter = new UpgradeInProgressFilter()
        progressFilter.configuredMessageConverters = [mockMessageConverter]
        progressFilter.safeUrlDecider = upgradeSafeUrlDecider
        progressFilter.skillEventQueue = skillEventQueue

        progressFilter.doFilter(mockRequest, mockResponse, mockFilterChain)

        then:
        1 * upgradeSafeUrlDecider.isSkillEventReport("/random", HttpMethod.POST) >> null
        1 * upgradeSafeUrlDecider.isUrlAllowed("/random", HttpMethod.POST) >> true
        1 * mockFilterChain.doFilter(mockRequest, mockResponse)
    }

    def "POST methods not allowed unless annotated as safe"() {
        def upgradeSafeUrlDecider = Mock(UpgradeSafeUrlDecider)
        def skillEventQueue = Mock(ReportedSkillEventQueue)
        def mockMessageConverter = Mock(HttpMessageConverter)
        def mockResponse = new MockHttpServletResponse()
        def mockFilterChain = Mock(FilterChain)
        def authentication = Mock(Authentication)
        def securityContext = Mock(SecurityContext)
        def userInfo = Mock(UserInfo)
        def mockHeaders = Mock(HttpHeaders)

        securityContext.getAuthentication() >> authentication
        authentication.isAuthenticated() >> true
        authentication.getPrincipal() >> userInfo
        userInfo.getUsername() >> "userMakingRequest"

        upgradeSafeUrlDecider.isSkillEventReport("/random", HttpMethod.POST) >> null
        upgradeSafeUrlDecider.isUrlAllowed("/random", HttpMethod.POST) >> false

        SecurityContextHolder.setContext(securityContext)

        MockHttpServletRequest mockRequest = post("/random")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{fake}").buildRequest()

        when:
        UpgradeInProgressFilter progressFilter = new UpgradeInProgressFilter()
        progressFilter.configuredMessageConverters = [mockMessageConverter]
        progressFilter.safeUrlDecider = upgradeSafeUrlDecider
        progressFilter.skillEventQueue = skillEventQueue

        progressFilter.doFilter(mockRequest, mockResponse, mockFilterChain)

        then:
        1 * mockMessageConverter.canWrite(UpgradeInProgressFilter.DbUpgradeErrBody.class, MediaType.APPLICATION_JSON) >> true
        1 * mockMessageConverter.write({ UpgradeInProgressFilter.DbUpgradeErrBody it ->
            it.errorCode == ErrorCode.DbUpgradeInProgress.toString() &&
            it.explanation == "A database upgrade is currently in progress, no training profile modifications are allowed at this time."
        }, MediaType.APPLICATION_JSON, _)
    }

    def "PUT methods not allowed unless annotated as safe"() {
        def upgradeSafeUrlDecider = Mock(UpgradeSafeUrlDecider)
        def skillEventQueue = Mock(ReportedSkillEventQueue)
        def mockMessageConverter = Mock(HttpMessageConverter)
        def mockResponse = new MockHttpServletResponse()
        def mockFilterChain = Mock(FilterChain)
        def authentication = Mock(Authentication)
        def securityContext = Mock(SecurityContext)
        def userInfo = Mock(UserInfo)
        def mockHeaders = Mock(HttpHeaders)

        securityContext.getAuthentication() >> authentication
        authentication.isAuthenticated() >> true
        authentication.getPrincipal() >> userInfo
        userInfo.getUsername() >> "userMakingRequest"

        upgradeSafeUrlDecider.isSkillEventReport("/random", HttpMethod.PUT) >> null
        upgradeSafeUrlDecider.isUrlAllowed("/random", HttpMethod.PUT) >> false

        SecurityContextHolder.setContext(securityContext)

        MockHttpServletRequest mockRequest = post("/random")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{fake}").buildRequest()

        when:
        UpgradeInProgressFilter progressFilter = new UpgradeInProgressFilter()
        progressFilter.configuredMessageConverters = [mockMessageConverter]
        progressFilter.safeUrlDecider = upgradeSafeUrlDecider
        progressFilter.skillEventQueue = skillEventQueue

        progressFilter.doFilter(mockRequest, mockResponse, mockFilterChain)

        then:
        1 * mockMessageConverter.canWrite(UpgradeInProgressFilter.DbUpgradeErrBody.class, MediaType.APPLICATION_JSON) >> true
        1 * mockMessageConverter.write({ UpgradeInProgressFilter.DbUpgradeErrBody it ->
            it.errorCode == ErrorCode.DbUpgradeInProgress.toString() &&
                    it.explanation == "A database upgrade is currently in progress, no training profile modifications are allowed at this time."
        }, MediaType.APPLICATION_JSON, _)
    }

    def "PATCH methods not allowed unless annotated as safe"() {
        def upgradeSafeUrlDecider = Mock(UpgradeSafeUrlDecider)
        def skillEventQueue = Mock(ReportedSkillEventQueue)
        def mockMessageConverter = Mock(HttpMessageConverter)
        def mockResponse = new MockHttpServletResponse()
        def mockFilterChain = Mock(FilterChain)
        def authentication = Mock(Authentication)
        def securityContext = Mock(SecurityContext)
        def userInfo = Mock(UserInfo)
        def mockHeaders = Mock(HttpHeaders)

        securityContext.getAuthentication() >> authentication
        authentication.isAuthenticated() >> true
        authentication.getPrincipal() >> userInfo
        userInfo.getUsername() >> "userMakingRequest"

        upgradeSafeUrlDecider.isSkillEventReport("/random", HttpMethod.PATCH) >> null
        upgradeSafeUrlDecider.isUrlAllowed("/random", HttpMethod.PATCH) >> false

        SecurityContextHolder.setContext(securityContext)

        MockHttpServletRequest mockRequest = post("/random")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{fake}").buildRequest()

        when:
        UpgradeInProgressFilter progressFilter = new UpgradeInProgressFilter()
        progressFilter.configuredMessageConverters = [mockMessageConverter]
        progressFilter.safeUrlDecider = upgradeSafeUrlDecider
        progressFilter.skillEventQueue = skillEventQueue

        progressFilter.doFilter(mockRequest, mockResponse, mockFilterChain)

        then:
        1 * mockMessageConverter.canWrite(UpgradeInProgressFilter.DbUpgradeErrBody.class, MediaType.APPLICATION_JSON) >> true
        1 * mockMessageConverter.write({ UpgradeInProgressFilter.DbUpgradeErrBody it ->
            it.errorCode == ErrorCode.DbUpgradeInProgress.toString() &&
                    it.explanation == "A database upgrade is currently in progress, no training profile modifications are allowed at this time."
        }, MediaType.APPLICATION_JSON, _)
    }

}
