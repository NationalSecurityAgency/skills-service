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
package skills.storage

import org.h2.tools.Server
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Component
import skills.storage.repos.nativeSql.DBConditions

import java.sql.SQLException

@Conditional(DBConditions.H2_IN_MEMORY)
@Component
class InMemoryH2 {

    @Value('#{"${skills.h2.port:9090}"}')
    String tcpPort

    @Bean(initMethod = "start", destroyMethod = "stop")
    Server inMemoryH2DatabaseServer() throws SQLException {
        return Server.createTcpServer(
                "-tcp", "-tcpAllowOthers", "-tcpPort", tcpPort);
    }
}
