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
package skills.dbupgrade

import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.type.AnnotatedTypeMetadata
import skills.auth.AuthMode

class DBUpgrade {

    static final String DB_UPGRADE_PROPERTY = 'skills.config.db-upgrade-in-progress'
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
