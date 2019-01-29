package skills.service.auth.jwt;

import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.web.authentication.preauth.x509.X509AuthenticationFilter;

public class X509PostProcessor implements ObjectPostProcessor<X509AuthenticationFilter> {
    @Override
    public <O extends X509AuthenticationFilter> O postProcess(O x509) {
        x509.setCheckForPrincipalChanges(true);
        return x509;
    }
}
