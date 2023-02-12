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

package skills.services

import groovy.transform.Memoized
import groovy.util.logging.Slf4j
import org.apache.commons.io.FileUtils
import org.apache.maven.artifact.versioning.ComparableVersion
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service

import jakarta.annotation.PostConstruct

@Service
@Slf4j
class VersionService {
    @Autowired
    ProjectErrorService projectErrorService;

    @Value("classpath:client-version")
    Resource resourceFile;

    String currentVersion;

    @PostConstruct
    void init() {
        currentVersion = loadCurrentVersionFile();
    }

    @Memoized
    public String loadCurrentVersionFile() {
        InputStream file;
        String data = "";

        try {
            file = resourceFile.getInputStream();
            data = file.getText("UTF-8");
            log.info("Successfully loaded the client-version file (version ${data})");
        }
        catch(IOException e) {
            log.error("Failed to read the client-version file", e);
            throw e;
        }

        return data;
    }

    public void compareClientVersions(String userVersion, String projectId) {
        if(!currentVersion.isBlank()) {
            if(!userVersion.equals(currentVersion)) {
                String [] currentVersionInfo = currentVersion.split("-");
                String [] userVersionInfo = userVersion.split("-");

                // Compare version numbers
                ComparableVersion current = new ComparableVersion(currentVersionInfo[3]);
                ComparableVersion user = new ComparableVersion(userVersionInfo[3]);

                if(user.compareTo(current) < 0) {
                    projectErrorService.clientVersionOutOfDate(projectId, userVersion, currentVersionInfo[3]);
                }

            }
        }
    }
}
