package skills.services

import groovy.transform.Memoized
import groovy.util.logging.Slf4j
import org.apache.commons.io.FileUtils
import org.apache.maven.artifact.versioning.ComparableVersion
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service

@Service
@Slf4j
class VersionService {
    @Autowired
    ProjectErrorService projectErrorService;

    @Value("classpath:client-version.json")
    Resource resourceFile;

    @Memoized
    public String getCurrentVersion() {
        File file;
        String data = "";

        try {
            file = resourceFile.getFile();
            data = FileUtils.readFileToString(file, "UTF-8");
        }
        catch(Exception e) {
            log.info("Failed to read file");
        }

        return data;
    }

    public void compareClientVersions(String userVersion, String projectId) {
        String currentVersion = getCurrentVersion();

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
