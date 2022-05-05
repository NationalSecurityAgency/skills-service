package skills.dbupgrade

import groovy.util.logging.Slf4j
import org.reflections.Reflections
import org.springframework.context.annotation.Conditional
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

import javax.annotation.PostConstruct
import java.lang.annotation.Annotation
import java.lang.reflect.Method
import java.util.regex.Matcher
import java.util.regex.Pattern

@Slf4j
@Component
@Conditional(DBUpgrade.InProgress)
class UpgradeSafeUrlDecider {

    private Pattern allowedMutationUrls

    @PostConstruct
    public void init() {
        Reflections reflections = new Reflections("skills.controller")
        Set<Class<?>> classes = reflections.getTypesAnnotatedWith(RestController)

        List<String> allowedUrls = []
        for (Class<?> clazz : classes) {
            RequestMapping rca = clazz.getDeclaredAnnotation(RequestMapping)
            String possibleRootUrl = ""
            if (rca != null) {
                possibleRootUrl = getUrl(rca)
            }

            Method[] methods = clazz.getDeclaredMethods()
            for (Method m : methods) {
                Annotation[] annotations = m.getDeclaredAnnotations()
                boolean isUpgradeSafe = false
                RequestMapping urlMapping
                for (Annotation annotation : annotations) {
                    if (annotation instanceof DBUpgradeSafe) {
                        isUpgradeSafe = true
                    } else if (annotation instanceof RequestMapping) {
                        urlMapping = annotation
                    }
                }

                if (isUpgradeSafe && urlMapping) {
                    String url = getUrl(urlMapping)
                    url = possibleRootUrl+url
                    url = url.replaceAll("[*]{1,2}", "[^/]+")
                    url = url.replaceAll("\\{[^}]+\\}", "[^/]+")
                    url = url.replaceAll("[?]", "[^/]")
                    url = "(?:${url})"
                    log.info("allowing PUT/POST access to ${url}")
                    allowedUrls.add("(?:${url})")
                }
            }
        }

        String urlPattern = allowedUrls.join("|")
        allowedMutationUrls = Pattern.compile(urlPattern)
    }

    public boolean isUrlAllowed(String path, String requestMethod) {
        HttpMethod method = HttpMethod.resolve(requestMethod)
        if (HttpMethod.PUT == method || HttpMethod.POST == method) {
            Matcher matcher = allowedMutationUrls.matcher(path)
            return matcher.find() //TODO: is this sufficient? do we need to account for context or subpaths?
        }
        return true
    }

    private static String getUrl(RequestMapping requestMapping) {
        String value = requestMapping.value()
        if (value == null) {
            value = requestMapping.path()
        }
        return value
    }

}
