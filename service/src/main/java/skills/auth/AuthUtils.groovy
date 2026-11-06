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
package skills.auth

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpMethod
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher
import org.springframework.security.web.util.matcher.OrRequestMatcher
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import skills.storage.model.auth.UserRole

import java.util.regex.Matcher
import java.util.regex.Pattern

@Slf4j
@CompileStatic
class AuthUtils {
    static final Pattern PROJECT_ID_PATTERN = Pattern.compile("/\\S+?/(myprojects|projects)/([^/]+).*\$")
    static final Pattern QUIZ_ID_PATTERN = Pattern.compile("^/admin/quiz-definitions/([^/]+).*\$")
    static final Pattern ADMIN_GROUP_ID_PATTERN = Pattern.compile("^/admin/admin-group-definitions/([^/]+).*\$")
    static final Pattern QUIZ_ID_PATTERN_API = Pattern.compile("^/api/quizzes/([^/]+).*\$")
    static final Pattern GLOBAL_BADGE_ID_PATTERN = Pattern.compile("^/admin/badges/([^/]+).*\$")

    // The two cross-project summary routes load the second project's skill. Other
    // project routes (including copy and sharing) are scoped to the source project.
    private static final List<RequestMatcher> PROJECT_PROTECTION_MATCHERS = [
            '/api/projects/{contextProjectId}/projects/{projectId}/skills/{skillId}/summary',
            '/api/projects/{contextProjectId}/projects/{projectId}/subjects/{subjectId}/skills/{skillId}/summary',
            '/api/projects/{projectId}/**',
            '/api/myprojects/{projectId}/**',
            '/app/projects/{projectId}/**',
            '/app/users/projects/{projectId}/**',
            '/admin/projects/{projectId}/**',
            '/admin/badges/{badgeId}/projects/{projectId}/skills/{skillId}',
            '/admin/badges/{badgeId}/projects/{projectId}/level/**',
            '/admin/admin-group-definitions/{adminGroupId}/projects/{projectId}',
    ].collect { String pattern -> (RequestMatcher) PathPatternRequestMatcher.pathPattern(pattern) }

    private static final RequestMatcher RESOURCE_CREATION_MATCHER = new OrRequestMatcher([
            '/app/projects/{id}',
            '/app/quiz-definitions/{id}',
            '/app/admin-group-definitions/{id}',
            '/app/badges/{id}',
    ].collect { String pattern -> (RequestMatcher) PathPatternRequestMatcher.pathPattern(pattern) })

    static String getProjectIdForProtection(HttpServletRequest request) {
        return getPathVariable(request, 'projectId', PROJECT_PROTECTION_MATCHERS)
    }

    static String getPathVariable(HttpServletRequest request, String variable, List<RequestMatcher> matchers) {
        // Use the same parsed, context-relative path and segment decoding as MVC.
        // ServletRequestPathUtils (used by these matchers) never includes the query.
        for (RequestMatcher matcher : matchers) {
            RequestMatcher.MatchResult result = matcher.matcher(request)
            if (result.isMatch()) {
                String id = result.variables.get(variable)
                // Do not turn a malformed resource ID into an abstention. In
                // particular, the service validators reject the literal "null".
                if (!id?.trim() || id.trim().equalsIgnoreCase('null')) {
                    // These exact creation routes validate the path ID before
                    // accessing a resource. Retain their structured BadParam
                    // responses, but never skip protection on resource-access routes.
                    if (request.method in ['POST', 'PUT'] && RESOURCE_CREATION_MATCHER.matches(request)) {
                        return null
                    }
                    throw new AccessDeniedException('Access is denied')
                }
                return id
            }
        }
        return null
    }

    // Example: /admin/projects/{projectId}/approvals/approve
    // Example: /admin/projects/{projectId}/approvals/reject
    static final Pattern PROJECT_SELF_REPORT_APPROVE_OR_REJECT_PATTERN = Pattern.compile("^/admin/projects/[^/]+/approvals/(?:approve|reject)\$")

    // Example: /admin/projects/{projectId}/approverConf
    static final Pattern PROJECT_SELF_REPORT_APPROVER_CONF_PATTERN = Pattern.compile("^/admin/projects/[^/]+/approverConf\$")

    // Example: /projects/{projectId}/approvalEmails/unsubscribe
    static final Pattern PROJECT_SELF_REPORT_EMAIL_UNSUB_CONF_PATTERN = Pattern.compile("^/admin/projects/[^/]+/approvalEmails/(?:unsubscribe|subscribe)\$")

    // Example: /admin/projects/{projectId}/dashboardActions
    static final Pattern PROJECT_DASHBOARD_ACTIONS_PATTERN = Pattern.compile("^/admin/projects/[^/]+/dashboardActions.*\$")

    static String getProjectIdFromPath(String servletPath) {
       return getIdFromServletPath(servletPath, PROJECT_ID_PATTERN, "projectId")
    }

    static String getQuizIdFromApiPath(String servletPath) {
        return getIdFromServletPath(servletPath, QUIZ_ID_PATTERN_API, "quizId")
    }

    static String getQuizIdFromPath(String servletPath) {
        String quizId = getIdFromServletPath(servletPath, QUIZ_ID_PATTERN, "quizId")
        if (!quizId) {
            quizId = getIdFromServletPath(servletPath, QUIZ_ID_PATTERN_API, "quizId")
        }

        return quizId
    }

    static String getAdminGroupIdFromPath(String servletPath) {
        return getIdFromServletPath(servletPath, ADMIN_GROUP_ID_PATTERN, "adminGroupId")
    }

    static String getGlobalBadgeIdFromPath(String servletPath) {
        return getIdFromServletPath(servletPath, GLOBAL_BADGE_ID_PATTERN, "globalBadgeId")
    }

    static RequestAttributes getRequestAttributes() {
        RequestAttributes res = new RequestAttributes()
        HttpServletRequest servletRequest = getServletRequest()
        res.requestPath = getRequestPath(servletRequest)
        res.httpMethod = getRequestMethod(servletRequest)
        res.projectId = getProjectIdFromPath(res.requestPath)
        res.quizId = getQuizIdFromPath(res.requestPath)
        res.quizIdUnderApiEndpoint = getQuizIdFromApiPath(res.requestPath)
        res.adminGroupId = getAdminGroupIdFromPath(res.requestPath)
        res.globalBadgeId = getGlobalBadgeIdFromPath(res.requestPath)
        return res
    }

    static String getRequestPath(HttpServletRequest servletRequest) {
        String res = null
        if (servletRequest) {
            if (log.isTraceEnabled()) {
                log.trace("${Thread.currentThread().name} - getRequestPath")
            }
            try {
                res = servletRequest.getServletPath() + (servletRequest.getPathInfo() ?: '')
            } catch (Exception e) {
                log.error("getRequestPath failed", e)
            }
        }
        return res
    }

    static String getRequestMethod(HttpServletRequest servletRequest) {
        String res = null
        if (servletRequest) {
            if (log.isTraceEnabled()) {
                log.trace("${Thread.currentThread().name} - getRequestMethod")
            }
            try {
                res = servletRequest.getMethod()
            } catch (Exception e) {
                log.error("getMethod failed", e)
            }
        }
        return res
    }

    private static String getIdFromServletPath(String servletPath, Pattern pattern, String label) {
        String res = null
        if (servletPath) {
            Matcher matcher = pattern.matcher(servletPath)
            if (matcher.matches()) {
                if (matcher.hasGroup()) {
                    if (pattern.toString() == PROJECT_ID_PATTERN.toString()) {
                        res = matcher.group(2)
                    } else {
                        res = matcher.group(1)
                    }

                } else {
                    log.warn("no {} found for endpoint [{}]?", label, servletPath)
                }
            }
        }
        return res
    }

    static HttpServletRequest getServletRequest() {
        HttpServletRequest httpServletRequest = null
        try {
            ServletRequestAttributes currentRequestAttributes = RequestContextHolder.getRequestAttributes() as ServletRequestAttributes
            httpServletRequest = currentRequestAttributes?.getRequest()
        } catch (Exception e) {
            log.warn("Unable to access current HttpServletRequest. Error Recieved [$e]", e)
        }
        return httpServletRequest
    }

    static HttpServletResponse getServletResponse() {
        HttpServletResponse httpServletResponse = null
        try {
            ServletRequestAttributes currentRequestAttributes = RequestContextHolder.getRequestAttributes() as ServletRequestAttributes
            httpServletResponse = currentRequestAttributes?.getResponse()
        } catch (Exception e) {
            log.warn("Unable to access current HttpServletResponse. Error Recieved [$e]", e)
        }
        return httpServletResponse
    }

    static class RequestAttributes {
        String requestPath
        String httpMethod
        String projectId
        String quizId
        String quizIdUnderApiEndpoint
        String adminGroupId
        String globalBadgeId

        boolean isSelfReportApproveOrRejectEndpoint() {
            if (requestPath) {
                return PROJECT_SELF_REPORT_APPROVE_OR_REJECT_PATTERN.matcher(requestPath).matches()
            }
            return false
        }

        boolean isSelfReportApproverConfEndpoint() {
            if (requestPath) {
                return PROJECT_SELF_REPORT_APPROVER_CONF_PATTERN.matcher(requestPath).matches()
            }
            return false
        }

        boolean isSelfReportEmailSubscriptionEndpoint() {
            if (requestPath) {
                return PROJECT_SELF_REPORT_EMAIL_UNSUB_CONF_PATTERN.matcher(requestPath).matches()
            }
            return false
        }

        boolean isDashboardActionsEndpoint() {
            if (requestPath) {
                return PROJECT_DASHBOARD_ACTIONS_PATTERN.matcher(requestPath).matches()
            }
            return false
        }

        boolean isAllowedProject(UserRole userRole) {
            return projectId && projectId.equalsIgnoreCase(userRole.projectId)
        }

        boolean isSupportedHttpMethod() {
            boolean isGetApprovalConfEndpoint = isSelfReportApproverConfEndpoint()
            return httpMethod && httpMethod == HttpMethod.GET.toString() && !isGetApprovalConfEndpoint
        }

        boolean isSelfReportAction() {
            boolean isRightMethod = httpMethod && (httpMethod == HttpMethod.POST.toString() || httpMethod == HttpMethod.PUT.toString())
            return isRightMethod && isSelfReportApproveOrRejectEndpoint()
        }

        boolean isSelfReportEmailSubscriptionAction() {
            boolean isRightMethod = httpMethod && (httpMethod == HttpMethod.POST.toString() || httpMethod == HttpMethod.PUT.toString())
            return isRightMethod && isSelfReportEmailSubscriptionEndpoint()
        }
    }

}
