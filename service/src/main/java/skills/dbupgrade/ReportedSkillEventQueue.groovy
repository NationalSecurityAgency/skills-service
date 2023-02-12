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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import skills.controller.AddSkillHelper
import skills.controller.exceptions.SkillException

import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import java.nio.file.Files
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

    BlockingQueue<QueuedSkillEvent> internalQueuedRequests

    @Autowired
    AddSkillHelper addSkillHelper

    QueuedEventSerializer queueWriter

    @PostConstruct
    public void init() {
        Boolean upgrading = Boolean.valueOf(dbUpgradeInProgress)
        if (upgrading) {
            Path outDir = Paths.get(queuedEventFileDir)
            if (!Files.isDirectory(outDir)) {
                throw new SkillException("[${queuedEventFileDir}] does not exist or is not a directory")
            }

            internalQueuedRequests = new LinkedBlockingQueue<>()
            queueWriter = new QueuedEventSerializer(BASE_NAME, FILE_EXT, outDir, internalQueuedRequests)
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
