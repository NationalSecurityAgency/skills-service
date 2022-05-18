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

import com.fasterxml.jackson.databind.MappingIterator
import com.fasterxml.jackson.databind.ObjectWriter
import com.fasterxml.jackson.databind.json.JsonMapper
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import skills.controller.AddSkillHelper
import skills.controller.exceptions.SkillException
import skills.controller.request.model.SkillEventRequest

import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes

@Slf4j
class SerializedEventReader {

    static final int STATUS_INTERVAL = 1000

    AddSkillHelper addSkillHelper
    Path fileDir
    String fileExtension

    public SerializedEventReader(Path fileDir, String fileExtension, AddSkillHelper addSkillHelper) {
        if (fileDir == null) {
            throw new SkillException("fileDir is required")
        }
        if (StringUtils.isEmpty(fileExtension)) {
            throw new SkillException("fileExtension is required")
        }
        if (addSkillHelper == null) {
            throw new SkillException("addSkillHelper is required")
        }
        if (fileExtension.startsWith(".")) {
            fileExtension = fileExtension.substring(1, fileExtension.length())
        }
        this.addSkillHelper = addSkillHelper
        this.fileDir = fileDir
        this.fileExtension = fileExtension
    }

    public void run() {
        if (Files.isDirectory(fileDir)) {
            List<Path> queuedEventFiles = getQueuedEventFiles(fileDir)

            if (queuedEventFiles) {
                Thread.start {
                    JsonMapper jsonMapper = new JsonMapper()
                    queuedEventFiles.forEach({
                        log.debug("identified queued event file [{}] to process", it.getFileName())
                        processFile(it, jsonMapper)
                    })
                }
            }
        }
    }

    private void processFile(Path file, JsonMapper jsonMapper) {
        log.info("processing queued skill event file [${file}]")
        ObjectWriter errorSerializer = jsonMapper.writerFor(QueuedSkillEvent)
        CheckPointer checkPointer = new CheckPointer(fileDir, file.getFileName().toString())
        try (MappingIterator<QueuedSkillEvent> itr = jsonMapper.readerFor(QueuedSkillEvent).readValues(Files.newBufferedReader(file))) {
            int startAt = checkPointer.getLastReadRecord()
            int i = 0
            while (itr.hasNext()) {
                checkPointer.recordRecord(i)
                i++
                QueuedSkillEvent queuedSkillEvent = itr.nextValue()
                if (i >= startAt) {
                    try {
                        SkillEventRequest skr = queuedSkillEvent.skillEventRequest
                        if (!skr) {
                            skr = new SkillEventRequest(userId: queuedSkillEvent.userId)
                            skr.timestamp = queuedSkillEvent.requestTime.getTime()
                        } else if (!skr.userId) {
                            skr.userId = queuedSkillEvent.userId
                        }
                        addSkillHelper.addSkill(queuedSkillEvent.projectId, queuedSkillEvent.skillId, skr)
                    } catch (Exception e) {
                        String asStr = errorSerializer.writeValueAsString(queuedSkillEvent)
                        log.error("unable to add queued event [$asStr]", e)
                    }
                } else {
                    log.debug("skipping record [{}], last record read before shutdown was [{}]", i, startAt)
                }

                if (i % STATUS_INTERVAL == 0 && i > 0) {
                    log.info("recovered [$i] events from [${file}] so far")
                }
            }
        }

        Files.delete(file)
        checkPointer.cleanup()
    }

    private List<Path> getQueuedEventFiles(Path root) {
        final var jsonSequenceFilesVisitor = new ExtensionFileVisitor(fileExtension)
        Files.walkFileTree(root, [].toSet(), 1, jsonSequenceFilesVisitor)
        return jsonSequenceFilesVisitor.matchedFiles
    }

    private static class ExtensionFileVisitor extends SimpleFileVisitor<Path> {
        private final PathMatcher pathMatcher
        List<Path> matchedFiles = []

        public ExtensionFileVisitor(String extension) {
            pathMatcher = FileSystems.getDefault().getPathMatcher("glob:*.${extension}")
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                throws IOException {
            if (pathMatcher.matches(file.getFileName())) {
                matchedFiles << file
            }
            return FileVisitResult.CONTINUE
        }
    }
}
