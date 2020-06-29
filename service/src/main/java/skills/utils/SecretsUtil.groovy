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
package skills.utils

import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils

@Slf4j
class SecretsUtil {
    final String DB_PW_FILE_KEY = 'skills.db.password.file'
    final String DB_PW_PROP_KEY = 'spring.datasource.password'

    final String KS_PW_FILE_KEY = 'skills.keystore.password.file'
    final String KS_CLIENT_PW_PROP_KEY = 'javax.net.ssl.keyStorePassword'
    final String KS_SERVER_PW_PROP_KEY = 'server.ssl.key-store-password'

    final String TS_PW_FILE_KEY = 'skills.truststore.password.file'
    final String TS_CLIENT_PW_PROP_KEY = 'javax.net.ssl.trustStorePassword'
    final String TS_SERVER_PW_PROP_KEY = 'server.ssl.trust-store-password'

    void updateSecrets() {
        log.info("Checking for external secrets...")
        String dbPasswordFile = System.getProperty(DB_PW_FILE_KEY)
        if (dbPasswordFile) {
            log.info("Setting database password using file [$dbPasswordFile]")
            System.setProperty(DB_PW_PROP_KEY, getTextFromFile(dbPasswordFile))
        }

        String ksPasswordFile = System.getProperty(KS_PW_FILE_KEY)
        if (ksPasswordFile) {
            log.info("Setting keystore password using file [$ksPasswordFile]")
            System.setProperty(KS_CLIENT_PW_PROP_KEY, getTextFromFile(ksPasswordFile))
            System.setProperty(KS_SERVER_PW_PROP_KEY, getTextFromFile(ksPasswordFile))
        }

        String tsPasswordFile = System.getProperty(TS_PW_FILE_KEY)
        if (tsPasswordFile) {
            log.info("Setting keystore password using file [$tsPasswordFile]")
            System.setProperty(TS_CLIENT_PW_PROP_KEY, getTextFromFile(tsPasswordFile))
            System.setProperty(TS_SERVER_PW_PROP_KEY, getTextFromFile(tsPasswordFile))
        }
    }

    private String getTextFromFile(String filename) {
        return StringUtils.strip(new File(filename).text)
    }
}
