package skills.service.auth

import groovy.util.logging.Slf4j

import javax.servlet.http.HttpServletRequest

@Slf4j
class AuthUtils {
    static final String PROJECT_ID_PATTERN = /\/\S+\/projects\/([^\/]+).*$/

    static String getProjectIdFromRequest(HttpServletRequest servletRequest) {
        String projectId
        String servletPath = servletRequest.getServletPath()
        def matcher = (servletPath =~ PROJECT_ID_PATTERN)
        if (matcher.matches()) {
            if (matcher.hasGroup()) {
                projectId = matcher.group(1)
            } else {
                log.warn("no projectId found for endpoint [$servletPath]?")
            }
        }
        return projectId
    }
}
