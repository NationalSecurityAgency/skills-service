package skills.dbupgrade

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectWriter
import com.fasterxml.jackson.databind.SequenceWriter
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import skills.controller.exceptions.SkillException

import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import java.nio.file.FileVisitOption
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.stream.Stream

@Slf4j
@Component
class ReportedSkillEventQueue implements Closeable{

    static final String FILE_EXT = "jsonsequence"
    static final String BASE_NAME = "queued_skill_events"
    static final String CHECKPOINT_FILE_EXT = "checkpoint"

    @Value('#{"${skills.db-upgrade-in-progress:false}"}')
    String dbUpgradeInProgress

    @Value('#{"${skills.queued-event-path:/tmp}"}')
    String queuedEventFileDir

    //could make this array queue if we wanted to protect memory
    LinkedBlockingQueue<QueuedSkillEvent> internalQueuedRequests
    ExecutorService fileWriterService

    @PostConstruct
    public void init() {
        Boolean upgrading = Boolean.valueOf(dbUpgradeInProgress)
        if (upgrading) {
            Path outDir = Paths.get(queuedEventFileDir)
            if (!Files.isDirectory(outDir)) {
                throw new SkillException("[${queuedEventFileDir}] does not exist or is not a directory")
            }

            internalQueuedRequests = new LinkedBlockingQueue<>()
            fileWriterService = Executors.newFixedThreadPool(1)
            ObjectWriter writer = new ObjectMapper().writerFor(QueuedSkillEvent)
            fileWriterService.submit(new Runnable() {
                public void run() {
                    Path outputFile = Files.createFile(outDir.resolve("${BASE_NAME}.${FILE_EXT}"))
                    log.info("writing queued skill events to [{}]", outputFile)
                    try (SequenceWriter sequenceWriter = writer.writeValues(Files.newBufferedWriter(outputFile, StandardOpenOption.APPEND, StandardOpenOption.CREATE))) {
                        while(!Thread.currentThread().isInterrupted()) {
                            QueuedSkillEvent queuedSkillEvent = internalQueuedRequests.poll()
                            sequenceWriter.write(queuedSkillEvent)
                        }
                    }
                }
            })
        } else {
            //TODO
            //have to recover any files that exist in the dir
            //checkpoint each record as read to allow
            Path outDir = Paths.get(queuedEventFileDir)
            if (Files.isDirectory(outDir)) {
                Stream<Path> files = Files.walk(outDir, 1, FileVisitOption.FOLLOW_LINKS)
                Stream<Path> queuedEventFiles = files.filter({it.toString().endsWithIgnoreCase(FILE_EXT)})
                queuedEventFiles.forEach({
                    throw new UnsupportedOperationException("processing queued event files is not implemented yet")
                })
            }
        }
    }

    public void queueEvent(QueuedSkillEvent queuedSkillEvent) {
        internalQueuedRequests.put(queuedSkillEvent)
    }

    @PreDestroy
    public void close() {
        if (Boolean.valueOf(dbUpgradeInProgress)) {
            fileWriterService.shutdown()
            fileWriterService.awaitTermination(30, TimeUnit.SECONDS)
        }
    }

}
