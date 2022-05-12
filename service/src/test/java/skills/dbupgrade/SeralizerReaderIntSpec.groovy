package skills.dbupgrade

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import skills.controller.AddSkillHelper
import skills.controller.request.model.SkillEventRequest
import spock.lang.Specification

import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

class SeralizerReaderIntSpec extends Specification {

    def "queued event reader can process events serialized by writer"() {
        FileSystem fs = Jimfs.newFileSystem(Configuration.unix())
        Path processingDirectory = fs.getPath("/processDir")
        Files.createDirectory(processingDirectory)

        BlockingQueue<QueuedSkillEvent> queue = new LinkedBlockingQueue<>()
        QueuedEventSerializer serializer = new QueuedEventSerializer("queue_file", ".jsonsequence", processingDirectory, queue)
        serializer.start()

        def addSkillHelper = Mock(AddSkillHelper)

        when:
        QueuedSkillEvent queuedSkillEvent1 = new QueuedSkillEvent(projectId: "foo", skillId: "bar", userId: "baz", requestTime: new Date(), skillEventRequest: new SkillEventRequest(userId: "anotherUser"))
        QueuedSkillEvent queuedSkillEvent2 = new QueuedSkillEvent(projectId: "fff", skillId: "bbb", userId: "zzz", requestTime: new Date(), skillEventRequest: new SkillEventRequest(userId: "uuu"))
        queue.add(queuedSkillEvent1)
        queue.add(queuedSkillEvent2)
        while (!queue.isEmpty()) {
            Thread.currentThread().sleep(10)
        }
        serializer.close()

        SerializedEventReader serializedEventReader = new SerializedEventReader(processingDirectory, ".jsonsequence", addSkillHelper)
        serializedEventReader.run()

        then:
        1 * addSkillHelper.addSkill("foo", "bar", { SkillEventRequest ser ->
            ser.userId == "anotherUser"
        })
        1 * addSkillHelper.addSkill("fff", "bbb", { SkillEventRequest ser ->
            ser.userId == "uuu"
        })

    }
}
