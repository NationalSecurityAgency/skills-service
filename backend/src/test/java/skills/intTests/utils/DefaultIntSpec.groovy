package skills.intTests.utils

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import skills.SpringBootApp
import skills.storage.repos.ProjDefRepo
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.UserAttrsRepo
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

    @Autowired
    ProjDefRepo projDefRepo

    @Autowired
    UserAttrsRepo userAttrsRepo

    @Autowired
    SkillDefRepo skillDefRepo

    def setup() {
        /**
         * deleting projects and users will wipe the entire db clean due to cascading
         */
        projDefRepo.deleteAll()
        userAttrsRepo.deleteAll()
        // global badges don't have references to a project so must delete those manually
        skillDefRepo.deleteAll()

        skillsService = createService()
    }

    SkillsService createService(
            String username = "skills@skills.org",
            String password = "p@ssw0rd",
            String firstName = "Skills",
            String lastName = "Test",
            String url = "http://localhost:${localPort}".toString()){
        new SkillsService(username, password, firstName, lastName, url)
    }

    SkillsService createSupervisor(){
        String ultimateRoot = 'jh@dojo.com'
        SkillsService rootSkillsService = createService(ultimateRoot, 'aaaaaaaa')
        rootSkillsService.grantRoot()
        String supervisorUserId = 'foo@bar.com'
        SkillsService supervisorService = createService(supervisorUserId)
        rootSkillsService.grantSupervisorRole(supervisorUserId)
        return supervisorService
    }
}
