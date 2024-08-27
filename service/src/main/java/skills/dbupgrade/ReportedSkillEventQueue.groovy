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
package skills.dbupgrade

import groovy.util.logging.Slf4j
import io.awspring.cloud.s3.DiskBufferingS3OutputStreamProvider
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.context.annotation.Conditional
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.type.AnnotatedTypeMetadata
import org.springframework.stereotype.Component
import skills.controller.AddSkillHelper
import software.amazon.awssdk.services.s3.S3Client

import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

@Slf4j
@Component
class ReportedSkillEventQueue implements Closeable {

    public static final String FILE_EXT = "jsonsequence"
    static final String BASE_NAME = "queued_skill_events"

    @Value('#{"${skills.config.db-upgrade-in-progress:false}"}')
    String dbUpgradeInProgress

    @Value('#{"${skills.queued-event-path:/tmp}"}')
    String queuedEventFileDir

    @Value('#{"${skills.queued-event-path.commit-every-n-records:100}"}')
    Integer reopenFileEveryNRecords

    BlockingQueue<QueuedSkillEvent> internalQueuedRequests

    @Autowired
    AddSkillHelper addSkillHelper

    @Autowired
    ApplicationContext applicationContext

    QueuedEventSerializer queueWriter

    S3Client s3Client;
    /**
     *  from awspring docs: you can use io.awspring.cloud.s3.DiskBufferingS3OutputStream by defining
     *  a bean of type DiskBufferingS3OutputStreamProvider which will override the default output stream provider.
     *  With DiskBufferingS3OutputStream when data is written to the resource,
     *  first it is stored on the disk in a tmp directory in the OS.
     * @return
     */
    static class S3EventStorageUtilized implements Condition {
        @Override
        boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            ConfigurableEnvironment environment = context.getEnvironment();
            String eventPath = environment.getProperty("skills.queued-event-path");
            String dbInProgress = environment.getProperty("skills.config.db-upgrade-in-progress");
            return eventPath.startsWith("s3:/") && dbInProgress?.toLowerCase()?.equals("true");
        }
    }
    @Bean
    @Conditional(S3EventStorageUtilized)
    DiskBufferingS3OutputStreamProvider getDiskBufferingS3OutputStreamProvider() {
        log.info("Using DiskBufferingS3OutputStreamProvider")
        return new DiskBufferingS3OutputStreamProvider((S3Client)s3Client, null)
    }

    ReportedSkillEventQueue(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @PostConstruct
    public void init() {
        Boolean upgrading = Boolean.valueOf(dbUpgradeInProgress)
        if (upgrading) {
            internalQueuedRequests = new LinkedBlockingQueue<>()
            queueWriter = new QueuedEventSerializer(applicationContext, queuedEventFileDir, internalQueuedRequests, reopenFileEveryNRecords)
            queueWriter.start()
        } else {
            Path outDir = Paths.get(queuedEventFileDir)
            SerializedEventReader serializedEventReader = new SerializedEventReader(outDir, FILE_EXT, addSkillHelper)
            serializedEventReader.run()
        }
    }

    public void queueEvent(QueuedSkillEvent queuedSkillEvent) {
        internalQueuedRequests?.put(queuedSkillEvent)
    }

    @PreDestroy
    public void close() {
        if (queueWriter) {
            queueWriter.close()
        }
    }

}
