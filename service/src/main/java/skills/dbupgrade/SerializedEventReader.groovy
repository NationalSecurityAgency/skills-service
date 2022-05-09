package skills.dbupgrade

import com.fasterxml.jackson.databind.MappingIterator
import com.fasterxml.jackson.databind.json.JsonMapper
import com.google.common.collect.Streams
import groovy.util.logging.Slf4j
import skills.controller.AddSkillHelper
import skills.controller.request.model.SkillEventRequest

import java.nio.file.FileVisitOption
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.stream.Stream

@Slf4j
class SerializedEventReader {
    static final String CHECKPOINT_FILE_EXT = "checkpoint"
    static final int STATUS_INTERVAL = 1000

    AddSkillHelper addSkillHelper
    Path fileDir
    String fileExtension

    public SerializedEventReader(Path fileDir, String fileExtension, AddSkillHelper addSkillHelper) {
        this.addSkillHelper = addSkillHelper
        this.fileDir = fileDir
        this.fileExtension = fileExtension
    }

    public void run() {
        if (Files.isDirectory(fileDir)) {
            Stream<Path> files = Files.walk(fileDir, 1, FileVisitOption.FOLLOW_LINKS)
            Stream<Path> queuedEventFiles = files.filter({ it.toString().endsWithIgnoreCase(fileExtension) })

            JsonMapper jsonMapper = new JsonMapper()
            queuedEventFiles.forEach({
                processFile(it, jsonMapper)
            })
        }
    }

    private void processFile(Path file, JsonMapper jsonMapper) {
        //move checkpoint handling to it's own utility class
        Path checkpointFile = fileDir.resolve("${file.getFileName()}.${CHECKPOINT_FILE_EXT}")
        int startAt = 0
        if (Files.isReadable(checkpointFile)) {
            Optional<String> lastLine = Streams.findLast(Files.lines(checkpointFile))
            lastLine.ifPresent({
                startAt = Integer.valueOf(it)
                log.info("reading events from [${checkpointFile}] was interrupted, starting at event [${startAt}]")
            })
        }

        log.info("processing queued skill event file [${file}]")
        try (MappingIterator<QueuedSkillEvent> itr = jsonMapper.readerFor(QueuedSkillEvent).readValues(Files.newBufferedReader(file));
             BufferedWriter checkPointer = Files.newBufferedWriter(checkpointFile, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {

            int i = 0
            while (itr.hasNext()) {
                checkPointer.writeLine("$i")
                checkPointer.flush()
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
                }

                if (i % STATUS_INTERVAL == 0 && i > 0) {
                    log.info("recovered [$i] events from [${file}] so far")
                }

            }
        }
        Files.delete(file)
        Files.delete(checkpointFile)
    }
}
