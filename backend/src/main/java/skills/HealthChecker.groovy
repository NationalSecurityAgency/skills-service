package skills

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import skills.auth.AuthMode
import skills.auth.pki.PkiUserLookup
import skills.storage.repos.ProjDefRepo

import javax.annotation.PostConstruct

@Slf4j
@Component
class HealthChecker {

    @Value('#{securityConfig.authMode}}')
    skills.auth.AuthMode authMode = skills.auth.AuthMode.DEFAULT_AUTH_MODE

    @Autowired
    ProjDefRepo projDefRepo

    @Autowired(required = false)
    PkiUserLookup pkiUserLookup

    @PostConstruct
    void checkRequiredServices() {
        log.info("Running initialization check...")
        log.info("Verifying database access...")
        projDefRepo.findByProjectId("")
        log.info("done.")

        if (authMode == AuthMode.PKI) {
            log.info("Checking if PKI user service is available...")
            boolean userServiceAvailable = pkiUserLookup.isServiceAvailable()
            log.info("userServiceAvailable [${userServiceAvailable}]")
        }
    }
}
