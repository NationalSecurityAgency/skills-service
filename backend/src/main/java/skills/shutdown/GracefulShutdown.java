package skills.shutdown;

import org.apache.catalina.connector.Connector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class GracefulShutdown implements TomcatConnectorCustomizer, ApplicationListener<ContextClosedEvent> {

    private static final Logger log = LoggerFactory.getLogger(GracefulShutdown.class);

    private final int timeoutSecs;

    public GracefulShutdown(int timeoutSecs) {
        this.timeoutSecs = timeoutSecs;
    }

    private volatile Connector connector;

    @Override
    public void customize(Connector connector) {
        this.connector = connector;
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        this.connector.pause();
        Executor executor = this.connector.getProtocolHandler().getExecutor();
        if (executor instanceof ThreadPoolExecutor) {
            try {
                ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executor;
                threadPoolExecutor.shutdown();
                log.info("Shutting down. Will wait up to {} seconds", timeoutSecs);
                if (!threadPoolExecutor.awaitTermination(timeoutSecs, TimeUnit.SECONDS)) {
                    log.warn("Tomcat thread pool did not shut down gracefully within ["
                            + timeoutSecs + "] seconds. Proceeding with forceful shutdown");

                    threadPoolExecutor.shutdownNow();

                    if (!threadPoolExecutor.awaitTermination(timeoutSecs, TimeUnit.SECONDS)) {
                        log.error("Tomcat thread pool did not terminate");
                    }
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

}
