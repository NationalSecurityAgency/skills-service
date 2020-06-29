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
package skills.intTests.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import skills.SpringBootApp;

import javax.net.ssl.HttpsURLConnection;
import java.util.TimeZone;

public class SpringBootAppManager {
    static Logger log = LoggerFactory.getLogger(SpringBootAppManager.class);
    static ConfigurableApplicationContext springBootApp;
    static final String DISABLE_HOSTNAME_VERIFIER_PROP = "skills.disableHostnameVerifier";

    static void start() {
        // must call in the main method and not in @PostConstruct method as H2 jdbc driver will cache timezone prior @PostConstruct method is called
        // alternatively we could pass in -Duser.timezone=UTC
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        boolean disableHostnameVerifier = Boolean.parseBoolean(System.getProperty(DISABLE_HOSTNAME_VERIFIER_PROP));
        if (disableHostnameVerifier) {
            HttpsURLConnection.setDefaultHostnameVerifier((s, sslSession) -> true);
        }

        if (springBootApp == null) {
            log.info("Staring Spring boot app...");
            try {
                springBootApp = SpringApplication.run(SpringBootApp.class);
                log.info("Stared Spring boot app...");
            } catch (Throwable t) {
                log.error("Failed to start app", t);
            }
        }
    }
}
