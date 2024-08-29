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
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import skills.controller.AddSkillHelper
import skills.dbupgrade.s3.DiskBufferingS3OutputStreamProviderConfigurer
import skills.dbupgrade.s3.S3SerializedEventsReader

import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
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

    @Autowired(required = false)
    DiskBufferingS3OutputStreamProviderConfigurer s3ClientProvider

    @Autowired(required = false)
    S3SerializedEventsReader s3SerializedEventsReader

    @Autowired
    FsSerializedEventReader fsSerializedEventReader

    @PostConstruct
    void init() {
        Boolean upgrading = Boolean.valueOf(dbUpgradeInProgress)
        if (upgrading) {
            log.info("Started in upgrade mode, storing events in a WAL")
            internalQueuedRequests = new LinkedBlockingQueue<>()
            queueWriter = new QueuedEventSerializer(applicationContext, queuedEventFileDir, internalQueuedRequests, reopenFileEveryNRecords)
            queueWriter.start()
        }
    }

    void replayEvents() {
        if (queuedEventFileDir.startsWith("s3:/")) {
            s3SerializedEventsReader.readAndProcess(queuedEventFileDir)
        } else {
            fsSerializedEventReader.readAndProcess(queuedEventFileDir)
        }
    }

    void queueEvent(QueuedSkillEvent queuedSkillEvent) {
        internalQueuedRequests?.put(queuedSkillEvent)
    }

    @PreDestroy
    public void close() {
        if (queueWriter) {
            queueWriter.close()
        }
    }

}
