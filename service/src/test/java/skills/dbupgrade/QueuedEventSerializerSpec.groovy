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
import skills.controller.exceptions.SkillException
import skills.controller.request.model.SkillEventRequest
import spock.lang.IgnoreRest
import spock.lang.Specification

import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

class QueuedEventSerializerSpec extends Specification {

    def "serializes queued events"() {
        FileSystem fs = Jimfs.newFileSystem(Configuration.unix())
        Path processingDirectory = fs.getPath("/processDir")
        Files.createDirectory(processingDirectory)

        BlockingQueue<QueuedSkillEvent> queue = new LinkedBlockingQueue<>()
        QueuedEventSerializer serializer = new QueuedEventSerializer("queue_file", ".jsonsequence", processingDirectory, queue)
        serializer.start()

        when:
        QueuedSkillEvent queuedSkillEvent1 = new QueuedSkillEvent(projectId: "foo", skillId: "bar", userId: "baz", requestTime: new Date(), skillEventRequest: new SkillEventRequest(userId: "anotherUser"))
        QueuedSkillEvent queuedSkillEvent2 = new QueuedSkillEvent(projectId: "fff", skillId: "bbb", userId: "zzz", requestTime: new Date(), skillEventRequest: new SkillEventRequest(userId: "uuu"))
        queue.add(queuedSkillEvent1)
        queue.add(queuedSkillEvent2)
        while (!queue.isEmpty()) {
            Thread.currentThread().sleep(10)
        }
        serializer.close()

        then:
        Files.isReadable(processingDirectory.resolve("queue_file.jsonsequence"))
        Files.size(processingDirectory.resolve("queue_file.jsonsequence")) > 0
        Files.readAllLines(processingDirectory.resolve("queue_file.jsonsequence")).size() == 1
        Files.readAllLines(processingDirectory.resolve("queue_file.jsonsequence"))[0].contains("zzz")
    }

    def "creates new file if file already exists in configured directory"() {
        FileSystem fs = Jimfs.newFileSystem(Configuration.unix())
        Path processingDirectory = fs.getPath("/processDir")
        Files.createDirectory(processingDirectory)
        Path existing = processingDirectory.resolve("queue_file.jsonsequence")
        Files.createFile(existing)

        BlockingQueue<QueuedSkillEvent> queue = new LinkedBlockingQueue<>()
        QueuedEventSerializer serializer = new QueuedEventSerializer("queue_file", ".jsonsequence", processingDirectory, queue)
        serializer.start()

        when:
        QueuedSkillEvent queuedSkillEvent1 = new QueuedSkillEvent(projectId: "foo", skillId: "bar", userId: "baz", requestTime: new Date(), skillEventRequest: new SkillEventRequest(userId: "anotherUser"))
        QueuedSkillEvent queuedSkillEvent2 = new QueuedSkillEvent(projectId: "fff", skillId: "bbb", userId: "zzz", requestTime: new Date(), skillEventRequest: new SkillEventRequest(userId: "uuu"))
        queue.add(queuedSkillEvent1)
        queue.add(queuedSkillEvent2)
        while (!queue.isEmpty()) {
            Thread.currentThread().sleep(10)
        }
        serializer.close()

        then:
        Files.isReadable(processingDirectory.resolve("queue_file.1.jsonsequence"))
        Files.size(processingDirectory.resolve("queue_file.1.jsonsequence")) > 0
        Files.readAllLines(processingDirectory.resolve("queue_file.1.jsonsequence")).size() == 1
        Files.readAllLines(processingDirectory.resolve("queue_file.1.jsonsequence"))[0].contains("zzz")
    }

    def "picks non-conflicting file number if multiple file exist in configured directory"() {
        FileSystem fs = Jimfs.newFileSystem(Configuration.unix())
        Path processingDirectory = fs.getPath("/processDir")
        Files.createDirectory(processingDirectory)
        Path existing = processingDirectory.resolve("queue_file.jsonsequence")
        Files.createFile(existing)

        Path existing2 = processingDirectory.resolve("queue_file.1.jsonsequence")
        Files.createFile(existing2)
        Path existing3 = processingDirectory.resolve("queue_file.9.jsonsequence")
        Files.createFile(existing3)
        Path existing4 = processingDirectory.resolve("queue_file.11.jsonsequence")
        Files.createFile(existing4)

        BlockingQueue<QueuedSkillEvent> queue = new LinkedBlockingQueue<>()
        QueuedEventSerializer serializer = new QueuedEventSerializer("queue_file", ".jsonsequence", processingDirectory, queue)
        serializer.start()

        when:
        QueuedSkillEvent queuedSkillEvent1 = new QueuedSkillEvent(projectId: "foo", skillId: "bar", userId: "baz", requestTime: new Date(), skillEventRequest: new SkillEventRequest(userId: "anotherUser"))
        QueuedSkillEvent queuedSkillEvent2 = new QueuedSkillEvent(projectId: "fff", skillId: "bbb", userId: "zzz", requestTime: new Date(), skillEventRequest: new SkillEventRequest(userId: "uuu"))
        queue.add(queuedSkillEvent1)
        queue.add(queuedSkillEvent2)
        while (!queue.isEmpty()) {
            Thread.currentThread().sleep(10)
        }
        serializer.close()

        then:
        Files.isReadable(processingDirectory.resolve("queue_file.12.jsonsequence"))
        Files.size(processingDirectory.resolve("queue_file.12.jsonsequence")) > 0
        Files.readAllLines(processingDirectory.resolve("queue_file.12.jsonsequence")).size() == 1
        Files.readAllLines(processingDirectory.resolve("queue_file.12.jsonsequence"))[0].contains("zzz")
    }

    def "fails if base directory does not exist" () {
        FileSystem fs = Jimfs.newFileSystem(Configuration.unix())
        Path processingDirectory = fs.getPath("/processDir")

        BlockingQueue<QueuedSkillEvent> queue = new LinkedBlockingQueue<>()

        when:
        QueuedEventSerializer serializer = new QueuedEventSerializer("queue_file", ".jsonsequence", processingDirectory, queue)

        then:
        thrown(SkillException)
    }

    def "fails base name not provided" () {
        FileSystem fs = Jimfs.newFileSystem(Configuration.unix())
        Path processingDirectory = fs.getPath("/processDir")
        Files.createDirectory(processingDirectory)
        BlockingQueue<QueuedSkillEvent> queue = new LinkedBlockingQueue<>()

        when:
        QueuedEventSerializer serializer = new QueuedEventSerializer(null, ".jsonsequence", processingDirectory, queue)

        then:
        thrown(SkillException)
    }

    def "fails extension not provided" () {
        FileSystem fs = Jimfs.newFileSystem(Configuration.unix())
        Path processingDirectory = fs.getPath("/processDir")
        Files.createDirectory(processingDirectory)
        BlockingQueue<QueuedSkillEvent> queue = new LinkedBlockingQueue<>()

        when:
        QueuedEventSerializer serializer = new QueuedEventSerializer("baseName", "", processingDirectory, queue)

        then:
        thrown(SkillException)
    }

    def "fails queue not provided" () {
        FileSystem fs = Jimfs.newFileSystem(Configuration.unix())
        Path processingDirectory = fs.getPath("/processDir")
        Files.createDirectory(processingDirectory)
        BlockingQueue<QueuedSkillEvent> queue = new LinkedBlockingQueue<>()

        when:
        QueuedEventSerializer serializer = new QueuedEventSerializer("baseName", "jsonsequence", processingDirectory, null)

        then:
        thrown(SkillException)
    }
}
