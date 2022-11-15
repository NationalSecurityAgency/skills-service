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

import groovy.util.logging.Slf4j
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import skills.storage.model.auth.UserRole

import javax.servlet.http.HttpServletRequest

@Slf4j
@Component
class ApproverRoleDecider {

    boolean shouldGrantApproverRole(HttpServletRequest servletRequest, UserRole userRole) {
        return isAllowedProject(servletRequest, userRole) &&
                (
                        isSupportedHttpMethod(servletRequest)
                        || isSelfReportAction(servletRequest)
                        || isSelfReportEmailSubscriptionAction(servletRequest)
                )
    }

    private boolean isAllowedProject(HttpServletRequest servletRequest, UserRole userRole) {
        String projectId = AuthUtils.getProjectIdFromRequest(servletRequest)
        return projectId && projectId.equalsIgnoreCase(userRole.projectId)
    }

    private boolean isSupportedHttpMethod(HttpServletRequest servletRequest) {
        String method = servletRequest.method
        boolean isGetApprovalConfEndpoint = AuthUtils.isSelfReportApproverConfEndpoint(servletRequest)
        return method && method == HttpMethod.GET.toString() && !isGetApprovalConfEndpoint
    }

    private boolean isSelfReportAction(HttpServletRequest servletRequest) {
        String method = servletRequest.method
        boolean isRightMethod = method && (method == HttpMethod.POST.toString() || method == HttpMethod.PUT.toString())
        return isRightMethod && AuthUtils.isSelfReportApproveOrRejectEndpoint(servletRequest)
    }

    private boolean isSelfReportEmailSubscriptionAction(HttpServletRequest servletRequest) {
        String method = servletRequest.method
        boolean isRightMethod = method && (method == HttpMethod.POST.toString() || method == HttpMethod.PUT.toString())
        return isRightMethod && AuthUtils.isSelfReportEmailSubscriptionEndpoint(servletRequest)
    }
}
