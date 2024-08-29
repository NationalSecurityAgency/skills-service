/**
 * Copyright 2024 SkillTree
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
package skills.dbupgrade.s3

import groovy.util.logging.Slf4j
import io.awspring.cloud.s3.S3PathMatchingResourcePatternResolver
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Conditional
import org.springframework.core.io.Resource
import org.springframework.core.io.support.ResourcePatternResolver
import org.springframework.stereotype.Component
import skills.controller.AddSkillHelper
import skills.dbupgrade.EventsResourceProcessor
import skills.dbupgrade.ReportedSkillEventQueue
import skills.services.LockingService
import software.amazon.awssdk.services.s3.S3Client

@Slf4j
@Component
@Conditional(S3QueuedEventPathCondition)
class S3SerializedEventsReader {

    private ResourcePatternResolver resourcePatternResolver;

    @Autowired
    AddSkillHelper addSkillHelper

    S3Client s3Client
    ApplicationContext applicationContext

    @Autowired
    EventsResourceProcessor eventsResourceProcessor

    @Autowired
    LockingService lockingService

    @Autowired
    void setupResolver(S3Client s3Client, ApplicationContext applicationContext) {
        this.resourcePatternResolver = new S3PathMatchingResourcePatternResolver(s3Client, applicationContext);
        this.s3Client = s3Client
        this.applicationContext = applicationContext
    }

    private String getBucketName(String queuedEventFileDir, Resource resource) {
        String justBucketName = queuedEventFileDir.replace("s3://", "")
        List<String> fileNameSplit = resource.filename.split('/').toList()
        if (fileNameSplit.size() > 1) {
            fileNameSplit.subList(0, fileNameSplit.size() - 1).each {
                justBucketName = justBucketName.replaceFirst(/\/?${it}\/?/, "")
            }
        }

        return justBucketName
    }

    @Transactional()
    void readAndProcess(String queuedEventFileDir) throws IOException {
        lockingService.lockForReplayingSkillEvents()
        Resource[] allTxtFilesInFolder = this.resourcePatternResolver.getResources("${queuedEventFileDir}/*.${ReportedSkillEventQueue.FILE_EXT}".toString());
        allTxtFilesInFolder.each { Resource resource ->
            log.info("processing queued skill event file [${resource.filename}]")
            String justBucketName = getBucketName(queuedEventFileDir, resource)
            eventsResourceProcessor.processFile(resource)

            log.info("Deleting S3 object [${resource.filename}] from [${justBucketName}]")
            s3Client.deleteObject(request -> request.bucket(justBucketName).key(resource.filename))
        }
    }
}
