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
import org.springframework.core.io.PathResource
import org.springframework.stereotype.Component

import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Slf4j
@Component
class FsSerializedEventReader {

    @Autowired
    EventsResourceProcessor eventsResourceProcessor

    @Value('#{"${skills.queued-event.num-threads-to-process:5}"}')
    Integer numThreadsToProcess

    String fileExtension = ReportedSkillEventQueue.FILE_EXT

    void readAndProcess(String queuedEventFileDir) {
        Path fileDir = Paths.get(queuedEventFileDir)

        if (Files.isDirectory(fileDir)) {
            List<Path> queuedEventFiles = getQueuedEventFiles(fileDir)
            if (queuedEventFiles) {
                log.info("processing queued skill event files [${queuedEventFiles.join(', ')}] with [${numThreadsToProcess}] threads")
                ExecutorService executor = Executors.newFixedThreadPool(numThreadsToProcess)
                try {
                    queuedEventFiles.forEach({ Path pathToProcess ->
                        executor.submit {
                            log.debug("identified queued event file [{}] to process", pathToProcess.getFileName())
                            eventsResourceProcessor.processFile(new PathResource(pathToProcess))
                            Files.delete(pathToProcess)
                        }
                    })
                } finally {
                    executor.shutdown()
                }
            }
        }
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
