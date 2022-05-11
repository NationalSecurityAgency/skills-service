package skills.dbupgrade

import com.fasterxml.jackson.databind.MappingIterator
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

            JsonMapper jsonMapper = new JsonMapper()
            queuedEventFiles.forEach({
                log.debug("identified queued event file [{}] to process", it.getFileName())
                processFile(it, jsonMapper)
            })
        }
    }

    private void processFile(Path file, JsonMapper jsonMapper) {
        log.info("processing queued skill event file [${file}]")

        CheckPointer checkPointer = new CheckPointer(fileDir, file.getFileName().toString())
        try (MappingIterator<QueuedSkillEvent> itr = jsonMapper.readerFor(QueuedSkillEvent).readValues(Files.newBufferedReader(file))) {
            int startAt = checkPointer.getLastReadRecord()
            int i = 0
            while (itr.hasNext()) {
                checkPointer.recordRecord(i)
                i++
                QueuedSkillEvent queuedSkillEvent = itr.nextValue()
                if (i >= startAt) {
                    SkillEventRequest skr = queuedSkillEvent.skillEventRequest
                    if (!skr) {
                        skr = new SkillEventRequest(userId: queuedSkillEvent.userId)
                        skr.timestamp = queuedSkillEvent.requestTime.getTime()
                    } else if (!skr.userId) {
                        skr.userId = queuedSkillEvent.userId
                    }
                    addSkillHelper.addSkill(queuedSkillEvent.projectId, queuedSkillEvent.skillId, skr)
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
        final ExtensionFileVisitor jsonSequenceFilesVisitor = new ExtensionFileVisitor(fileExtension)
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
