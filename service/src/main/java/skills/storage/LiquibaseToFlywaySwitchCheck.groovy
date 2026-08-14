/**
 * Copyright 2026 SkillTree
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
package skills.storage

import groovy.util.logging.Slf4j
import org.flywaydb.core.api.callback.Callback
import org.flywaydb.core.api.callback.Context
import org.flywaydb.core.api.callback.Event
import org.springframework.stereotype.Component
import java.sql.Connection

import java.sql.DatabaseMetaData

@Component
@Slf4j
class LiquibaseToFlywaySwitchCheck  implements Callback {

    @Override
    boolean supports(Event event, Context context) {
        event == Event.BEFORE_MIGRATE
    }

    @Override
    boolean canHandleInTransaction(Event event, Context context) {
        true
    }
    @Override
    String getCallbackName(){
        return LiquibaseToFlywaySwitchCheck.simpleName.class
    }

    @Override
    void handle(Event event, Context context) {
        Connection connection = context.connection
        if (hasLiquibaseChangelog(connection)) {
            validateLatestMd5SynPresent(connection)
        }
    }

    private boolean hasLiquibaseChangelog(Connection connection) {
        boolean tableExists = false

        // Check if the table exists using JDBC database metadata to avoid SQL syntax errors
        DatabaseMetaData metaData = connection.metaData
        metaData.getTables(null, null, "databasechangelog", null).withCloseable { resultSet ->
            if (resultSet.next()) {
                tableExists = true
            }
        }

        return tableExists
    }

    private void validateLatestMd5SynPresent(Connection connection) {
        boolean md5SumFound = false
        String targetMd5 = '9:4c6a97ae6c88e5aaf12105f246b0c21a'

        // Using a parameterized PreparedStatement to prevent SQL injection vulnerabilities
        String query = "SELECT 1 FROM databasechangelog WHERE md5sum = ?"

        connection.prepareStatement(query).withCloseable { preparedStatement ->
            preparedStatement.setString(1, targetMd5)

            preparedStatement.executeQuery().withCloseable { resultSet ->
                if (resultSet.next()) {
                    md5SumFound = true
                }
            }
        }

        // Throw an exception if the table was present but the exact checksum row was missing
        if (!md5SumFound) {
            String message = "In the 5.0 major release, the Liquibase database migration library was replaced with Flyway. To ensure a proper database schema migration, you must first upgrade to the 4.6 release before upgrading to 5.0"
            log.error(message)
            throw new IllegalStateException(message)
        }
        log.info("Pre-Check Success: Required md5sum row verified.", callbackName)
    }

}
