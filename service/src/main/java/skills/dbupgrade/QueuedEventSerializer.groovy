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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectWriter
import com.fasterxml.jackson.databind.SequenceWriter
import groovy.json.JsonOutput
import groovy.util.logging.Slf4j
import org.springframework.context.ApplicationContext
import org.springframework.core.io.PathResource
import org.springframework.core.io.Resource
import org.springframework.core.io.WritableResource
import skills.controller.exceptions.SkillException

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Slf4j
class QueuedEventSerializer implements AutoCloseable{

    BlockingQueue<QueuedSkillEvent> queuedSkillEvents
    ExecutorService fileWriterService
    SequenceWriter sequenceWriter
    Resource eventsDestination
    Integer reopenFileEveryNRecords
    String queuedEventFileDir
    ApplicationContext applicationContext

    private int currentNumRecords = 0

    QueuedEventSerializer(ApplicationContext applicationContext, String queuedEventFileDir, BlockingQueue<QueuedSkillEvent> queuedSkillEvents, Integer reopenFileEveryNRecords) {
        if (queuedSkillEvents == null) {
            throw new SkillException("queuedSkillEvents is required")
        }
        this.applicationContext = applicationContext
        this.queuedEventFileDir = queuedEventFileDir
        this.reopenFileEveryNRecords = reopenFileEveryNRecords

        fileWriterService = Executors.newFixedThreadPool(1)

        this.eventsDestination = getOutputResource()
        this.queuedSkillEvents = queuedSkillEvents
    }

    private SequenceWriter openSequenceWriter() {
        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(((WritableResource) eventsDestination).getOutputStream(), "UTF-8"))
        ObjectWriter objectWriter = new ObjectMapper().writerFor(QueuedSkillEvent)
        return objectWriter.writeValues(bufferedWriter)
    }

    private Resource getOutputResource() {
        if (queuedEventFileDir == null) {
            throw new SkillException("outputDirPath is required")
        }
        Resource res = queuedEventFileDir.startsWith("s3:") ? getS3OutputResource() : getLocalFileOutputResource()
        log.info("Opened output path [{}]", res.filename)
        return res
    }

    private Resource getS3OutputResource() {
        String s3FilePath = "${queuedEventFileDir}/${createFileName()}"
        log.info("initializing s3 output path [{}]", s3FilePath)
        return applicationContext.getResource(s3FilePath)
    }

    private Resource getLocalFileOutputResource() {
        Path queuedEventDir = Paths.get(queuedEventFileDir)
        if (!Files.isDirectory(queuedEventDir)) {
            throw new SkillException("[${queuedEventFileDir}] does not exist or is not a directory")
        }
        Path outputFile = queuedEventDir.resolve(createFileName())
        Files.createFile(outputFile);
        return new PathResource(outputFile)
    }
    private String createFileName() {
        return "${ReportedSkillEventQueue.BASE_NAME}-${UUID.randomUUID().toString()}.${ReportedSkillEventQueue.FILE_EXT}"
    }

    void start() {
        sequenceWriter = openSequenceWriter()
        fileWriterService.submit(new Runnable() {
            void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        QueuedSkillEvent queuedSkillEvent = queuedSkillEvents.take()
                        if (log.isTraceEnabled()) {
                            log.trace("writing queued skill event to SequenceWriter [{}]", JsonOutput.toJson(queuedSkillEvent))
                        }
                        if (queuedSkillEvent.projectId && queuedSkillEvent.userId && queuedSkillEvent.skillId) {
                            sequenceWriter.write(queuedSkillEvent)
                            currentNumRecords++
                            if (currentNumRecords % reopenFileEveryNRecords == 0) {
                                log.info("Closing [${eventsDestination.filename}]")
                                sequenceWriter.close()
                                eventsDestination = getOutputResource()
                                sequenceWriter = openSequenceWriter()
                            }
                        }
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt()
                    } catch (Throwable e) {
                        log.error("Failed while writing queued skill events", e)
                    }
                }
            }
        })
    }

    @Override
    void close() throws Exception {
        if (fileWriterService) {
            fileWriterService.shutdownNow()
            fileWriterService.awaitTermination(5, TimeUnit.SECONDS)
        }
        if (sequenceWriter) {
            log.info("finished writing queued events [${eventsDestination.filename}]")
            sequenceWriter.close()
        }
    }

}
