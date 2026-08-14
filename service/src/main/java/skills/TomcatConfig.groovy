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

import org.apache.catalina.connector.Connector
import org.apache.coyote.http2.Http2Protocol
import org.apache.tomcat.util.http.Rfc6265CookieProcessor
import org.apache.tomcat.util.http.SameSiteCookies
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.tomcat.TomcatConnectorCustomizer
import org.springframework.boot.tomcat.TomcatContextCustomizer
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.annotation.Configuration

@Configuration
class TomcatConfig implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {

    @Value('#{"${server.tomcat.accesslog.enabled:false}"}')
    boolean enabledAccessLog

    @Value('#{"${server.tomcat.disableOverheadThresholds:false}"}')
    boolean disableOverheadThresholds

    // this will force the SameSite=None attribute to be present on the Set-Cookie header.
    // Note that the SameSite=None attribute also requires the Secure attribute to be present
    // setting the property to true is useful when running the skills-service spring boot
    // container in http mode (`server.ssl.enabled=false`) but running behind a https proxy
    @Value('#{"${skills.config.forceSameSiteNoneCookie:false}"}')
    boolean forceSameSiteNoneCookie

    @Override
    void customize(TomcatServletWebServerFactory factory) {
        if (forceSameSiteNoneCookie) {
            factory.addContextCustomizers(new TomcatContextCustomizer() {
                @Override
                void customize(org.apache.catalina.Context context) {
                    Rfc6265CookieProcessor cp = new Rfc6265CookieProcessor()
                    cp.setSameSiteCookies(SameSiteCookies.NONE.getValue())
                    context.setCookieProcessor(cp)
                }
            })
        }

        if (disableOverheadThresholds) {
            factory.addConnectorCustomizers(new TomcatConnectorCustomizer() {
                @Override
                void customize(Connector connector) {
                    Http2Protocol http2Protocol = new Http2Protocol()
                    http2Protocol.setOverheadCountFactor(0)
                    http2Protocol.setOverheadDataThreshold(0)
                    http2Protocol.setOverheadContinuationThreshold(0)
                    http2Protocol.setOverheadWindowUpdateThreshold(0)
                    connector.addUpgradeProtocol(http2Protocol)
                }
            })
        }
    }

}
