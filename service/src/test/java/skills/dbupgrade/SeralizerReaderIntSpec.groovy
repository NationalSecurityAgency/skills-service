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

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import skills.controller.AddSkillHelper
import skills.controller.request.model.SkillEventRequest
import skills.utils.WaitFor
import spock.lang.Specification

import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

class SeralizerReaderIntSpec extends Specification {

    def "SerializedEventReader can process events written by QueuedEventSerializer"() {
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
        WaitFor.wait(750, {
            return Files.list(processingDirectory).findFirst().isEmpty()
        })

        then:
        1 * addSkillHelper.addSkill("foo", "bar", { SkillEventRequest ser ->
            ser.userId == "anotherUser"
        })
        1 * addSkillHelper.addSkill("fff", "bbb", { SkillEventRequest ser ->
            ser.userId == "uuu"
        })

    }
}
