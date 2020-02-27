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
package skills

import ch.qos.logback.access.pattern.AccessConverter
import ch.qos.logback.access.spi.IAccessEvent
import ch.qos.logback.access.tomcat.LogbackValve
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.annotation.Configuration

import java.security.cert.X509Certificate

@Configuration
class TomcatConfig implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {

    @Value('#{"${server.tomcat.accesslog.enabled:false}"}')
    boolean enabledAccessLog

    @Override
    void customize(TomcatServletWebServerFactory factory) {
        if (enabledAccessLog) {
            factory.addContextValves(new LogbackValve())
        }
    }

    static class DnConverter extends AccessConverter {

        static final String CERT_HEADER = 'javax.servlet.request.X509Certificate'

        @Override
        String convert(IAccessEvent accessEvent) {
            String dn = getSubjectDN(accessEvent)
            if (dn == null) {
                return IAccessEvent.NA;
            } else {
                return dn
            }
        }

        private String getSubjectDN(IAccessEvent accessEvent) {
            def certificateAttr = accessEvent.getRequest().getAttribute(CERT_HEADER)
            if (certificateAttr instanceof X509Certificate) {
                return certificateAttr?.getSubjectDN()?.name
            } else if (certificateAttr instanceof X509Certificate[]) {
                // use the first one
                if (certificateAttr.length > 0) {
                    return certificateAttr[0]?.getSubjectDN()?.name
                }
            }
        }
    }
}
