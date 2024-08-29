package skills.dbupgrade.s3

import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.type.AnnotatedTypeMetadata

class S3QueuedEventPathCondition implements Condition {

    @Override
    boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        ConfigurableEnvironment environment = context.getEnvironment();
        String eventPath = environment.getProperty("skills.queued-event-path");
        println "----------------------------------------"
        boolean res =  eventPath?.startsWith("s3:/")
        println "${eventPath} -> ${res}"
        return res
    }
}