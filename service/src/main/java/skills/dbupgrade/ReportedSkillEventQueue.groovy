package skills.dbupgrade

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import skills.controller.AddSkillHelper
import skills.controller.exceptions.SkillException

import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
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

    @Value('#{"${skills.db-upgrade-in-progress:false}"}')
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
