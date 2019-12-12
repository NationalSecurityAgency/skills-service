package skills

import ch.qos.logback.access.pattern.AccessConverter
import ch.qos.logback.access.spi.IAccessEvent
import ch.qos.logback.access.tomcat.LogbackValve
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.annotation.Configuration

import java.security.cert.X509Certificate

@Configuration
class TomcatConfig implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {

    @Override
    void customize(TomcatServletWebServerFactory factory) {
        factory.addContextValves(new LogbackValve())
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
