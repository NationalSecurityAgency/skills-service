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
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectWriter
import com.fasterxml.jackson.databind.SequenceWriter
import com.fasterxml.jackson.databind.json.JsonMapper
import groovy.util.logging.Slf4j
import org.apache.commons.io.FileUtils
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.PathResource
import org.springframework.core.io.WritableResource
import skills.SpringBootApp
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import spock.lang.Shared

@Slf4j
@SpringBootTest(properties = [
        'skills.config.db-upgrade-in-progress=false',
        'skills.queued-event-path=./target',
        'skills.queued-event-path.commit-every-n-records=5'
], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = SpringBootApp)
class ReplayWALEventsIT extends DefaultIntSpec {
    @Shared
    File dir = new File('./target')

    def cleanup() {
        List<File> files = dir.listFiles().findAll({ it.name.endsWith('.jsonsequence') })
        files.each {
            log.info("deleting ${it}")
            FileUtils.forceDelete(it)
        }
    }

    def "replay events from multiple files"() {
        Map proj = SkillsFactory.createProject()
        Map subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(2, 1, 1, 100)

        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

        PathResource path = new PathResource("./target/queued_skill_events-file1.jsonsequence")
        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(((WritableResource) path).getOutputStream(), "UTF-8"))
        ObjectWriter objectWriter = new ObjectMapper().writerFor(QueuedSkillEvent)
        SequenceWriter sequenceWriter = objectWriter.writeValues(bufferedWriter)

        QueuedSkillEvent queuedSkillEvent = new QueuedSkillEvent(
                projectId: proj.projectId,
                skillId: skills[0].skillId,
                userId: 'user1',
        )
        sequenceWriter.write(queuedSkillEvent)
        sequenceWriter.close()

        when:
        true

        then:
        true
    }

    private  List<QueuedSkillEvent> getEvents(String text) {
        JsonMapper jsonMapper = new JsonMapper()
        List<QueuedSkillEvent> res = []
        MappingIterator<QueuedSkillEvent> itr = jsonMapper.readerFor(QueuedSkillEvent).readValues(text)
        while (itr.hasNext()) {
            QueuedSkillEvent queuedSkillEvent = itr.nextValue()
            res.add(queuedSkillEvent)
        }
        itr.close()

        return res
    }

}
