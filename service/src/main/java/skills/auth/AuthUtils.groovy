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

    static String getProjectIdFromRequest(HttpServletRequest servletRequest) {
       return this.getIdFromRequest(servletRequest, PROJECT_ID_PATTERN, "projectId")
    }

    static String getQuizIdFromApiRequest(HttpServletRequest servletRequest) {
        return this.getIdFromRequest(servletRequest, QUIZ_ID_PATTERN_API, "quizId")
    }

    static String getQuizIdFromRequest(HttpServletRequest servletRequest) {
        String quizId = this.getIdFromRequest(servletRequest, QUIZ_ID_PATTERN, "quizId")
        if (!quizId) {
            quizId = this.getIdFromRequest(servletRequest, QUIZ_ID_PATTERN_API, "quizId")
        }

        return quizId
    }

    static String getAdminGroupIdFromRequest(HttpServletRequest servletRequest) {
        return this.getIdFromRequest(servletRequest, ADMIN_GROUP_ID_PATTERN, "adminGroupId")
    }

    static String getGlobalBadgeIdFromRequest(HttpServletRequest servletRequest) {
        return this.getIdFromRequest(servletRequest, GLOBAL_BADGE_ID_PATTERN, "globalBadgeId")
    }

    private static String getIdFromRequest(HttpServletRequest servletRequest, Pattern pattern, String label) {
        String res
        if (servletRequest) {
            String servletPath = servletRequest.getServletPath()
            Matcher matcher = pattern.matcher(servletPath)
            if (matcher.matches()) {
                if (matcher.hasGroup()) {
                    if (pattern.toString() == PROJECT_ID_PATTERN.toString()) {
                        res = matcher.group(2)
                    } else {
                        res = matcher.group(1)
                    }

                } else {
                    log.warn("no {} found for endpoint [{}]?", label, servletRequest)
                }
            }
        }
        return res
    }

    static boolean isSelfReportApproveOrRejectEndpoint(HttpServletRequest servletRequest) {
        if (servletRequest) {
            String servletPath = servletRequest.getServletPath()
            return PROJECT_SELF_REPORT_APPROVE_OR_REJECT_PATTERN.matcher(servletPath).matches()
        }
        return false
    }

    static boolean isSelfReportApproverConfEndpoint(HttpServletRequest servletRequest) {
        if (servletRequest) {
            String servletPath = servletRequest.getServletPath()
            return PROJECT_SELF_REPORT_APPROVER_CONF_PATTERN.matcher(servletPath).matches()
        }
        return false
    }

    static boolean isSelfReportEmailSubscriptionEndpoint(HttpServletRequest servletRequest) {
        if (servletRequest) {
            String servletPath = servletRequest.getServletPath()
            return PROJECT_SELF_REPORT_EMAIL_UNSUB_CONF_PATTERN.matcher(servletPath).matches()
        }
        return false
    }

    static boolean isDashboardActionsEndpoint(HttpServletRequest servletRequest) {
        if (servletRequest) {
            String servletPath = servletRequest.getServletPath()
            return PROJECT_DASHBOARD_ACTIONS_PATTERN.matcher(servletPath).matches()
        }
        return false
    }


}
