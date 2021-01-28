package skills.utils

import callStack.profiler.Profile
import callStack.utils.CachedThreadPool
import callStack.utils.ThreadPoolUtils
import groovy.transform.Canonical
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import skills.auth.UserInfo
import skills.auth.UserInfoService

import javax.annotation.PostConstruct

@Component
@Slf4j
class ToolMetricsLogger {

    @Value('#{"${skills.external.metrics.enabled:false}"}')
    Boolean enabled

    @Value('${skills.external.metrics.endpoint.url:#{null}}')
    String endpointUrl

    @Value('#{"${skills.external.metrics.minNumOfThreads:3}"}')
    Integer minNumOfThreads

    @Value('#{"${skills.external.metrics.maxNumOfThreads:10}"}')
    Integer maxNumOfThreads

    @Autowired
    UserInfoService userInfoService

    private RestTemplate restTemplate
    private CachedThreadPool pool

    @PostConstruct
    void init() {
        if (enabled) {
            log.info("Enabling external tool reporting to endpoint [{}]", endpointUrl)
            restTemplate = new RestTemplate();
            pool = new CachedThreadPool('tool-metrics', minNumOfThreads, maxNumOfThreads)
        }
    }

    @Profile
    void log(String group, Map<String, String> attributes = [:]) {
        if (enabled) {
            // user attributes must be obtained from the current thread
            attributes.putAll(getUserAttributes())

            // report to external service in a separate thread
            pool.submit([ThreadPoolUtils.callable {
                TMMessage message = new TMMessage(group, attributes)
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<TMMessage> entity = new HttpEntity<>(message, headers);
                try {
                    restTemplate.put(endpointUrl, entity);
                } catch(Exception ex) {
                    log.error("Unable to report to external metrics service", ex)
                }
            }])
        }
    }

    Map<String, String> getUserAttributes() {
        Map<String, String> userAttributes = [:]
        UserInfo userInfo = userInfoService.getCurrentUser()
        if (userInfo) {
            if (StringUtils.isNotBlank(userInfo.userDn)) {
                userAttributes.put('DN', userInfo.userDn)
            } else if (StringUtils.isNotBlank(userInfo.email)) {
                userAttributes.put('email', userInfo.email)
            }
        }
        return userAttributes
    }

    @Canonical
    static class TMMessage {
        String group
        Map<String, String> attributes = [:]
    }
}
