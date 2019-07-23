package skills.auth

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Required
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import skills.storage.model.auth.AllowedOrigin
import skills.storage.repos.AllowedOriginRepo

import javax.servlet.http.HttpServletRequest

@Slf4j
@CompileStatic
//@Component
class SkillsCorsConfigurationSource implements CorsConfigurationSource {
    // regex used for testing if CORS configuration should be supplied
    @Value('${skills.authorization.cors.pathPattern:^\\/api\\/.+}')
    String pathPattern
    @Required void setPathPattern(String pathPattern) { this.pathPattern = pathPattern }

    @Autowired
    AllowedOriginRepo allowedOriginRepository

    @Override
    CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
        CorsConfiguration corsConfiguration
        if (isSkillsCorsResource(request)) {
            String projectId = getProjectId(request)
            List<AllowedOrigin> allowedOrigins = allowedOriginRepository.findAllByProjectId(projectId)

            corsConfiguration = new CorsConfiguration(
                    allowCredentials: true,
                    allowedHeaders: [CorsConfiguration.ALL],
                    allowedMethods: [CorsConfiguration.ALL],
                    allowedOrigins: allowedOrigins.collect {it.allowedOrigin}
            )
        }
        return corsConfiguration
    }

    boolean isSkillsCorsResource(HttpServletRequest request) {
        return request.getServletPath() ==~ pathPattern
    }

    private String getProjectId(HttpServletRequest request) {
        String projectId
        String servletPath = request.servletPath
        def matcher = (servletPath =~ /\/api\/projects\/([^\/]+).*$/)
        if (matcher.matches()) {
            if (matcher.hasGroup()) {
                projectId = matcher.group(1)
            }
        }
        if (!projectId) {
            String msg = "Unable to get projectId from servletPath [$servletPath]"
            log.error(msg)
            throw new RuntimeException(msg);
        }
        return projectId
    }
}
