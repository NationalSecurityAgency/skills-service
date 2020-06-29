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

    static class H2_IN_MEMORY implements Condition {
        @Override
        boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String dbSource = context.environment.getProperty(PROP_DB_URL)
            // default to H2 is url is not provided
            if (!dbSource || dbSource.toLowerCase().contains("jdbc:h2:mem")) {

                return true
            }
            return false;
        }
    }
}
