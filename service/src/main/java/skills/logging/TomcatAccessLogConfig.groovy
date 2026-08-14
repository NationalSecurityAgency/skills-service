/**
 * Copyright 2026 SkillTree
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
package skills.logging

import groovy.util.logging.Slf4j
import org.apache.catalina.valves.AccessLogValve
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@Slf4j
// 1. Mandatory requirement: General Tomcat access logging must be globally enabled
@ConditionalOnProperty(name = "server.tomcat.accesslog.enabled", havingValue = "true", matchIfMissing = false)
class TomcatAccessLogConfig {

    @Value('${server.tomcat.accesslog.pattern:access.skills-service [%{callerUserId}r] %h %l %u %t "%r" %s %b %D "%{Referer}i" "%{User-Agent}i" %D}')
    String accessLogFormat

    /**
     * Custom stdout customizer bean.
     * Only gets created if 'server.tomcat.accesslog.stdout.enabled' is explicitly true.
     */
    @Bean
    @ConditionalOnProperty(name = "server.tomcat.accesslog.stdout.enabled", havingValue = "true", matchIfMissing = false)
    WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return { TomcatServletWebServerFactory factory ->
            // Use Groovy's anonymous inner class constructor or closure coercion
            def valve = new AccessLogValve() {
                @Override
                void log(CharArrayWriter charArrayWriter) {
                    println(charArrayWriter.toString())
                }
            }

            valve.setPattern(accessLogFormat)
            factory.addEngineValves(valve)
        } as WebServerFactoryCustomizer
    }
}
