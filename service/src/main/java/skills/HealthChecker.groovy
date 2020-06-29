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
        log.debug("Running initialization check...")
        log.debug("Verifying database access...")
        projDefRepo.findByProjectId("")
        log.debug("done.")

        if (authMode == AuthMode.PKI) {
            log.debug("Checking if PKI user service is available...")
            boolean userServiceAvailable = pkiUserLookup.isServiceAvailable()
            log.debug("userServiceAvailable [${userServiceAvailable}]")
        }
    }
}
