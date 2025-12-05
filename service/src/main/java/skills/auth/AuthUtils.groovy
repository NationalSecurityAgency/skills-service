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
                res = servletRequest.getServletPath()
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
