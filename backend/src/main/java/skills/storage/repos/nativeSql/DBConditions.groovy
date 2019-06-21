package skills.storage.repos.nativeSql

import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.type.AnnotatedTypeMetadata

class DBConditions {

    final static String PROP_DB_URL = "spring.datasource.url"
    private abstract static class ConditionBase implements Condition {
        abstract String getContainsValue()

        @Override
        boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String dbSource = context.environment.getProperty(PROP_DB_URL)
            if (dbSource && dbSource.toLowerCase().contains(containsValue.toLowerCase())) {
                return true
            }
            return false;
        }
    }

    static class PostgresQL extends ConditionBase {
        @Override
        String getContainsValue() { return "jdbc:postgresql" }
    }

    static class MySQL extends ConditionBase {
        @Override
        String getContainsValue() { return "jdbc:mysql" }
    }

    static class H2 implements Condition {
        @Override
        boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String dbSource = context.environment.getProperty(PROP_DB_URL)
            // default to H2 is url is not provided
            if (!dbSource || dbSource.toLowerCase().contains("jdbc:h2")) {

                return true
            }
            return false;
        }
    }
}
