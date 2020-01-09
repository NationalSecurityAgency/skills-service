package skills.storage

import org.h2.tools.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Component
import skills.storage.repos.nativeSql.DBConditions

import java.sql.SQLException

@Conditional(DBConditions.H2_IN_MEMORY)
@Component
class InMemoryH2 {

    @Bean(initMethod = "start", destroyMethod = "stop")
    Server inMemoryH2DatabaseServer() throws SQLException {
        return Server.createTcpServer(
                "-tcp", "-tcpAllowOthers", "-tcpPort", "9090");
    }
}
