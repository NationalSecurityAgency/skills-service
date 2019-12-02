package skills.shutdown;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class GracefulShutdownBeans {

    @Value("${skills.gracefulShutdown.timeoutSecs}")
    private int timeoutSecs;

    @Bean
    public GracefulShutdown gracefulShutdown() {
        return new GracefulShutdown(timeoutSecs);
    }

    @Bean
    public ConfigurableServletWebServerFactory webServerFactory(final GracefulShutdown gracefulShutdown) {
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
        factory.addConnectorCustomizers(gracefulShutdown);
        return factory;
    }
}
