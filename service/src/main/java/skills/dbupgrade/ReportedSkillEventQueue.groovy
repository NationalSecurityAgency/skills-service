package skills.dbupgrade

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectWriter
import com.fasterxml.jackson.databind.SequenceWriter
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue

@Component
class ReportedSkillEventQueue {

    @Value('#{"${skills.skills.db-upgrade-in-progress:false}"}')
    String dbUpgradeInProgress

    LinkedBlockingQueue<QueuedSkillEvent> internalQueuedRequests
    ExecutorService fileWriterService
    SequenceWriter sequenceWriter

    @PostConstruct
    public void init() {
        Boolean upgrading = Boolean.valueOf(dbUpgradeInProgress)
        if (upgrading) {
            fileWriterService = Executors.newFixedThreadPool(1)
            //config for location of written file
            //reader
            ObjectWriter writer = new ObjectMapper().writerFor(QueuedSkillEvent)
            try(sequenceWriter = writer.writeValues())
        }
    }

}
