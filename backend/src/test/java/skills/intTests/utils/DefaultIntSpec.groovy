package skills.intTests.utils

import groovy.util.logging.Slf4j
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import skills.SpringBootApp
import spock.lang.Specification

@Slf4j
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT, classes = SpringBootApp)
class DefaultIntSpec extends Specification {

    static {
        // must call in the main method and not in @PostConstruct method as H2 jdbc driver will cache timezone prior @PostConstruct method is called
        // alternatively we could pass in -Duser.timezone=UTC
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    };

    SkillsService skillsService

    @LocalServerPort
    int localPort

    def setup() {
        skillsService = createService()
        skillsService.deleteAllMyProjects()
    }

    SkillsService createService(
            String username = "skills@skills.org",
            String password = "p@ssw0rd",
            String firstName = "Skills",
            String lastName = "Test",
            String url = "http://localhost:${localPort}".toString()){
        new SkillsService(username, password, firstName, lastName, url)
    }
}
