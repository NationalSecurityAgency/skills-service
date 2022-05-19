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

import groovy.util.logging.Slf4j
import org.reflections.Reflections
import org.springframework.beans.factory.annotation.Value
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

    @Value('#{"${server.servlet.context-path:}"}')
    String contextPath

    Pattern skillEventUrl

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
                    if (contextPath) {
                        url = slashAwareJoin(contextPath, url)
                    }

                    log.info("allowing PUT/POST access to ${url}")
                    allowedUrls.add("(?:${url})")
                }
            }
        }

        String urlPattern = allowedUrls.join("|")
        allowedMutationUrls = Pattern.compile(urlPattern)
        log.debug("POST and PUT urls will be checked against [{}]", allowedMutationUrls.pattern())

        String skillEventRequest = "/api/projects/([^/]+)/skills/([^/]+)"
        if (contextPath) {
            skillEventRequest = slashAwareJoin(contextPath, skillEventRequest)
        }

        skillEventUrl = Pattern.compile(skillEventRequest)
    }

    public QueuedSkillEvent isSkillEventReport(String path, HttpMethod method) {
        Matcher matcher = skillEventUrl.matcher(path)
        if ((method == HttpMethod.POST || HttpMethod.PUT) && matcher.find()) {
            String projectId = matcher.group(1)
            String skillId = matcher.group(2)
            log.debug("request is reporting a skill event for [{}]-[{}]", projectId, skillId)
            QueuedSkillEvent queuedSkillEvent = new QueuedSkillEvent(projectId: projectId, skillId: skillId, requestTime: new Date())
            return queuedSkillEvent
        } else {
            return null
        }
    }

    public boolean isUrlAllowed(String path, HttpMethod method) {
        log.info("checking if http ${method} against [${path}] is allowed")
        if (HttpMethod.PUT == method || HttpMethod.POST == method || HttpMethod.DELETE == method) {
            Matcher matcher = allowedMutationUrls.matcher(path)
            return matcher.find()
        }
        return true
    }

    private static String getUrl(RequestMapping requestMapping) {
        String value = requestMapping.value().join("/")
        if (value == null) {
            value = requestMapping.path().join("/")
        }
        return value
    }

    private static String slashAwareJoin(String one, String two) {
        if ((one.endsWith("/") && !two.startsWith("/")) || (!one.endsWith("/") && two.startsWith("/"))) {
            return one+two
        } else if (one.endsWith("/") && two.startsWith("/")) {
             return one.substring(0, one.length()-1)+two
        } else {
            return "${one}/${two}"
        }
    }

}
