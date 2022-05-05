package skills.dbupgrade

import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.type.AnnotatedTypeMetadata
import skills.auth.AuthMode

class DBUpgrade {

    static final String DB_UPGRADE_PROPERTY = 'skills.db-upgrade-in-progress'
    static final String DEFAULT = "false"

    static class InProgress implements Condition {
        @Override
        boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String upgradeInProgress = context.environment.getProperty(DB_UPGRADE_PROPERTY)?.toUpperCase()?.trim() ?: DEFAULT
            return Boolean.valueOf(upgradeInProgress) == Boolean.TRUE
        }
    }

    static class NotInProgress implements Condition {
        @Override
        boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String upgradeInProgress = context.environment.getProperty(DB_UPGRADE_PROPERTY)?.toUpperCase()?.trim() ?: DEFAULT
            return Boolean.valueOf(upgradeInProgress) == Boolean.FALSE
        }
    }
}
