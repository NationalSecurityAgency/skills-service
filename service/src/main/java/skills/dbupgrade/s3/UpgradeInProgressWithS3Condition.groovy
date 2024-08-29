package skills.dbupgrade.s3

import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.type.AnnotatedTypeMetadata

class UpgradeInProgressWithS3Condition implements Condition {

    @Override
    boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        ConfigurableEnvironment environment = context.getEnvironment();
        String eventPath = environment.getProperty("skills.queued-event-path");
        String upgradeInProgress = environment.getProperty("skills.config.db-upgrade-in-progress");
        return eventPath?.startsWith("s3:/") && upgradeInProgress?.toLowerCase()?.equals("true")
    }
}