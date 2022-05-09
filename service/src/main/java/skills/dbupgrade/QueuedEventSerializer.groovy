package skills.dbupgrade

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectWriter
import com.fasterxml.jackson.databind.SequenceWriter
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Value
import skills.controller.exceptions.SkillException

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.regex.Matcher
import java.util.regex.Pattern

@Slf4j
class QueuedEventSerializer implements AutoCloseable{

    String extension
    String baseName
    BlockingQueue<QueuedSkillEvent> queuedSkillEvents
    Path queuedEventDir
    ExecutorService fileWriterService
    SequenceWriter sequenceWriter

    public QueuedEventSerializer(String baseName, String extension, Path queuedEventDir, BlockingQueue<QueuedSkillEvent> queuedSkillEvents) {
        this.baseName = baseName
        this.extension = extension
        this.queuedEventDir = queuedEventDir
        this.queuedSkillEvents = queuedSkillEvents

        if (!Files.isDirectory(queuedEventDir)) {
            throw new SkillException("[${queuedEventDir}] does not exist or is not a directory")
        }

        fileWriterService = Executors.newFixedThreadPool(1)
    }

    public void start() {
        ObjectWriter writer = new ObjectMapper().writerFor(QueuedSkillEvent)
        Path outputFile = getNextFile(queuedEventDir)

        log.info("writing queued skill events to [{}]", outputFile)
        sequenceWriter = writer.writeValues(Files.newBufferedWriter(outputFile, StandardOpenOption.APPEND, StandardOpenOption.CREATE))

        fileWriterService.submit(new Runnable() {
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        QueuedSkillEvent queuedSkillEvent = queuedSkillEvents.take()
                        log.info("writing queued skill event to SequenceWriter")
                        sequenceWriter.write(queuedSkillEvent)
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt()
                    }
                }
            }
        })
    }

    @Override
    void close() throws Exception {
        if (fileWriterService) {
            fileWriterService.shutdownNow()
            fileWriterService.awaitTermination(30, TimeUnit.SECONDS)
        }
        if (sequenceWriter) {
            sequenceWriter.close()
        }
    }

    private Path getNextFile(Path rootDir) {
        Pattern numberedFiles = Pattern.compile("${baseName}(\\.\\d+)?\\.${extension}")
        int maxFileCount = 0

        Files.list(rootDir).each {
            String name = it.getFileName().toString()
            Matcher match = numberedFiles.matcher(name)
            if (match.matches()) {
                if (match.groupCount() > 0 && match.group(1) != null) {
                    maxFileCount = Math.max(maxFileCount+1, Integer.valueOf(match.group(1)))
                } else {
                    maxFileCount++
                }
            }
        }

        String finalName = "${baseName}.${extension}"
        if (maxFileCount > 0) {
            finalName = "${baseName}.${maxFileCount}.${extension}"
        }

        return rootDir.resolve(finalName)
    }
}
