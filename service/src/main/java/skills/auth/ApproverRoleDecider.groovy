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
        return isAllowedProject(servletRequest, userRole) && (isSupportedHttpMethod(servletRequest) || isSelfReportAction(servletRequest))
    }

    private boolean isAllowedProject(HttpServletRequest servletRequest, UserRole userRole) {
        String projectId = AuthUtils.getProjectIdFromRequest(servletRequest)
        return projectId && projectId.equalsIgnoreCase(userRole.projectId)
    }

    private boolean isSupportedHttpMethod(HttpServletRequest servletRequest) {
        String method = servletRequest.method
        return method && method == HttpMethod.GET.toString()
    }

    private boolean isSelfReportAction(HttpServletRequest servletRequest) {
        String method = servletRequest.method
        boolean isRightMethod = method && (method == HttpMethod.POST.toString() || method == HttpMethod.PUT.toString())
        return isRightMethod && AuthUtils.isSelfReportApproveOrRejectEndpoint(servletRequest)
    }
}
