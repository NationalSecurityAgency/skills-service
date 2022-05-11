package skills.dbupgrade

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectWriter
import com.fasterxml.jackson.databind.SequenceWriter
import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import skills.controller.AddSkillHelper
import skills.controller.exceptions.SkillException
import skills.controller.request.model.SkillEventRequest
import spock.lang.Specification

import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class SerializedEventReaderSpec extends Specification {

    def "reads queued events and submits them"() {
        FileSystem fs = Jimfs.newFileSystem(Configuration.unix())
        Path processingDirectory = fs.getPath("/processDir")
        Files.createDirectory(processingDirectory)
        Path firstFile = processingDirectory.resolve("afile.jsonsequence")
        ObjectWriter writer = new ObjectMapper().writerFor(QueuedSkillEvent)
        SequenceWriter sequenceWriter = writer.writeValues(Files.newBufferedWriter(firstFile, StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE))
        sequenceWriter.write(new QueuedSkillEvent(projectId: "myProject", skillId: "mySkill", userId: "user1", requestTime: new Date(), skillEventRequest: new SkillEventRequest()))
        sequenceWriter.close()

        Path secondFile = processingDirectory.resolve("bfile.jsonsequence")
        sequenceWriter = writer.writeValues(Files.newBufferedWriter(secondFile, StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE))
        sequenceWriter.write(new QueuedSkillEvent(projectId: "yourProject", skillId: "yourSkill", requestTime: new Date(), userId: "user2", skillEventRequest: new SkillEventRequest(userId: "user3")))
        sequenceWriter.close()

        def addSkillHelper = Mock(AddSkillHelper)

        when:
        SerializedEventReader serializedEventReader = new SerializedEventReader(processingDirectory, ".jsonsequence", addSkillHelper)
        serializedEventReader.run()

        then:
        1 * addSkillHelper.addSkill("myProject", "mySkill", { SkillEventRequest ser ->
            ser.userId == "user1"
        })
        1 * addSkillHelper.addSkill("yourProject", "yourSkill", { SkillEventRequest ser ->
            ser.userId == "user3"
        })
    }

    def "configured directory doesn't have to exist"() {
        def addSkillHelper = Mock(AddSkillHelper)
        FileSystem fs = Jimfs.newFileSystem(Configuration.unix())
        Path processingDirectory = fs.getPath("/processDir")

        when:
        SerializedEventReader serializedEventReader = new SerializedEventReader(processingDirectory, ".jsonsequence", addSkillHelper)
        serializedEventReader.run()

        then:
        notThrown(Exception)
    }

    def "configured directory can be empty"() {
        def addSkillHelper = Mock(AddSkillHelper)
        FileSystem fs = Jimfs.newFileSystem(Configuration.unix())
        Path processingDirectory = fs.getPath("/processDir")
        Files.createDirectory(processingDirectory)

        when:
        SerializedEventReader serializedEventReader = new SerializedEventReader(processingDirectory, ".jsonsequence", addSkillHelper)
        serializedEventReader.run()

        then:
        notThrown(Exception)
    }

    def "configured directory can contain files that don't match the configured file extension"() {
        def addSkillHelper = Mock(AddSkillHelper)
        FileSystem fs = Jimfs.newFileSystem(Configuration.unix())
        Path processingDirectory = fs.getPath("/processDir")
        Files.createDirectory(processingDirectory)
        Path newFile = processingDirectory.resolve("junkFile.foo")
        PrintWriter pw = new PrintWriter(Files.newBufferedWriter(newFile))
        pw.println("{:garbage that isn't valid json")
        pw.close()

        when:
        SerializedEventReader serializedEventReader = new SerializedEventReader(processingDirectory, ".jsonsequence", addSkillHelper)
        serializedEventReader.run()

        then:
        notThrown(Exception)
    }

    def "fails of extension is not specified"() {
        def addSkillHelper = Mock(AddSkillHelper)
        FileSystem fs = Jimfs.newFileSystem(Configuration.unix())
        Path processingDirectory = fs.getPath("/processDir")
        Files.createDirectory(processingDirectory)

        when:
        new SerializedEventReader(processingDirectory, "", addSkillHelper)

        then:
        thrown(SkillException)
    }

    def "fails of add skill helper is not specified"() {
        def addSkillHelper = null
        FileSystem fs = Jimfs.newFileSystem(Configuration.unix())
        Path processingDirectory = fs.getPath("/processDir")
        Files.createDirectory(processingDirectory)

        when:
        new SerializedEventReader(processingDirectory, ".jsonseqeuence", addSkillHelper)

        then:
        thrown(SkillException)
    }
}
