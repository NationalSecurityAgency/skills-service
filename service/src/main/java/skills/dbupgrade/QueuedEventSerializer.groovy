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
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Value
import skills.controller.exceptions.SkillException
import skills.controller.exceptions.SkillsValidator

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
        if (StringUtils.isEmpty(baseName)) {
            throw new SkillException("baseName is required")
        }
        if (StringUtils.isEmpty(extension)) {
            throw new SkillException("extension is required")
        }
        if (!Files.isDirectory(queuedEventDir)) {
            throw new SkillException("[${queuedEventDir}] does not exist or is not a directory")
        }
        if (queuedSkillEvents == null) {
            throw new SkillException("queuedSkillEvents is required")
        }
        this.baseName = baseName
        this.extension = extension.startsWith(".") ? extension.subSequence(1, extension.length()) : extension
        this.queuedEventDir = queuedEventDir
        this.queuedSkillEvents = queuedSkillEvents



        fileWriterService = Executors.newFixedThreadPool(1)
    }

    public void start() {
        ObjectWriter writer = new ObjectMapper().writerFor(QueuedSkillEvent)
        Path outputFile = getNextFile(queuedEventDir)

        log.info("writing queued skill events to [{}]", outputFile)
        sequenceWriter = writer.writeValues(Files.newBufferedWriter(outputFile,  StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE))

        fileWriterService.submit(new Runnable() {
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        QueuedSkillEvent queuedSkillEvent = queuedSkillEvents.take()
                        log.trace("writing queued skill event to SequenceWriter")
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
            fileWriterService.awaitTermination(5, TimeUnit.SECONDS)
        }
        if (sequenceWriter) {
            sequenceWriter.close()
        }
    }

    private Path getNextFile(Path rootDir) {
        Pattern numberedFiles = Pattern.compile("${baseName}(?:\\.(\\d+))?\\.${extension}")
        int currentFileCount = 0
        boolean existing = false

        Files.list(rootDir).each {
            String name = it.getFileName().toString()
            Matcher match = numberedFiles.matcher(name)
            if (match.matches()) {
                if (match.groupCount() > 0 && match.group(1) != null) {
                    currentFileCount = Math.max(currentFileCount, Integer.valueOf(match.group(1)))
                }
                existing = true
            }
        }

        String finalName = "${baseName}.${extension}"
        if (existing) {
            finalName = "${baseName}.${++currentFileCount}.${extension}"
        }

        return rootDir.resolve(finalName)
    }
}
