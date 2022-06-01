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
import spock.lang.Specification

import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path

class CheckPointerSpec extends Specification {

    def "writes checkpoint file" () {
        FileSystem fs = Jimfs.newFileSystem(Configuration.unix())
        Path processingDirectory = fs.getPath("/processDir")
        Files.createDirectory(processingDirectory)

        when:
        CheckPointer checkPointer = new CheckPointer(processingDirectory, "file-being-processed.jsonsequence")
        final int startAt = checkPointer.getStartAt()
        final int initialLastRead = checkPointer.getLastReadRecord()
        checkPointer.recordRecord(5)
        final int lastRead = checkPointer.getLastReadRecord()
        checkPointer.close()

        then:
        Files.isReadable(fs.getPath("/processDir/file-being-processed.jsonsequence.checkpoint"))
        startAt == 0
        initialLastRead == 0
        lastRead == 0
    }

    def "removes checkpoint file on cleanup" () {
        FileSystem fs = Jimfs.newFileSystem(Configuration.unix())
        Path processingDirectory = fs.getPath("/processDir")
        Files.createDirectory(processingDirectory)

        when:
        CheckPointer checkPointer = new CheckPointer(processingDirectory, "file-being-processed.jsonsequence")
        final int startAt = checkPointer.getStartAt()
        final int initialLastRead = checkPointer.getLastReadRecord()
        checkPointer.recordRecord(5)
        final int lastRead = checkPointer.getLastReadRecord()
        checkPointer.cleanup()

        then:
        !Files.isReadable(fs.getPath("/processDir/file-being-processed.jsonsequence.checkpoint"))
        startAt == 0
        initialLastRead == 0
        lastRead == 0
    }

    def "checkpoint file can be re-read" () {
        FileSystem fs = Jimfs.newFileSystem(Configuration.unix())
        Path processingDirectory = fs.getPath("/processDir")
        Files.createDirectory(processingDirectory)

        when:
        CheckPointer checkPointer = new CheckPointer(processingDirectory, "file-being-processed.jsonsequence")
        checkPointer.recordRecord(Integer.MAX_VALUE-1)
        checkPointer = new CheckPointer(processingDirectory, "file-being-processed.jsonsequence")
        int startAt = checkPointer.getStartAt()
        checkPointer.close()

        then:
        Files.isReadable(fs.getPath("/processDir/file-being-processed.jsonsequence.checkpoint"))
        startAt == Integer.MAX_VALUE-1
    }
}
