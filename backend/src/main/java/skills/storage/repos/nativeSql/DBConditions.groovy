package skills.storage.repos.nativeSql

import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.type.AnnotatedTypeMetadata

class DBConditions {
    private abstract static class ConditionBase implements Condition {
        abstract String getContainsValue()
        @Override
        boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String dbSource = context.environment.getProperty("spring.datasource.url")
            if( dbSource.contains(containsValue)){
                return true
            }
            return false;
        }
    }

    static class PostgresQL extends ConditionBase {
        @Override
        String getContainsValue() { return "postgresql" }
    }

    static class MySQL extends ConditionBase {
        @Override
        String getContainsValue() { return "mysql" }
    }
}
