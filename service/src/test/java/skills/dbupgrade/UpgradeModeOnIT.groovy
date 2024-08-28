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
import com.fasterxml.jackson.databind.json.JsonMapper
import groovy.util.logging.Slf4j
import org.apache.commons.io.FileUtils
import org.springframework.boot.test.context.SpringBootTest
import skills.SpringBootApp
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsService
import spock.lang.Shared

@Slf4j
@SpringBootTest(properties = [
        'skills.config.db-upgrade-in-progress=true',
        'skills.queued-event-path=./target',
        'skills.queued-event-path.commit-every-n-records=5'
], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = SpringBootApp)
class UpgradeModeOnIT extends DefaultIntSpec {
    @Shared
    File dir = new File('./target')

    def cleanup() {
        List<File> files = dir.listFiles().findAll({ it.name.endsWith('.jsonsequence') })
        files.each {
            log.info("deleting ${it}")
            FileUtils.forceDelete(it)
        }
    }

    def "store skill events in WAL when in upgrade mode"() {
        when:
        skillsService.addSkill(['projectId': 'Proj1', skillId: 'CreateProject'], 'user1', new Date())
        SkillsService user2 = createService('user2')
        user2.addSkill(['projectId': 'Proj2', skillId: 'CreateSubject'], 'user2', new Date())

        4.times {
            user2.addSkill(['projectId': 'Proj3', skillId: "Skill${it}"], 'user2', new Date())
        }

        List<File> files = dir.listFiles().findAll({ it.name.endsWith('.jsonsequence') }).sort {it.lastModified() }
        List<QueuedSkillEvent> file1SkillEvents = getEvents(files[0].text)
        List<QueuedSkillEvent> file2SkillEvents = getEvents(files[1].text)

        then:
        files.size() == 2
        file1SkillEvents.size() == 5
        file1SkillEvents[0].skillId == 'CreateProject'
        file1SkillEvents[0].projectId == 'Proj1'
        file1SkillEvents[0].userId == skillsService.userName

        file1SkillEvents[1].skillId == 'CreateSubject'
        file1SkillEvents[1].projectId == 'Proj2'
        file1SkillEvents[1].userId == user2.userName

        file1SkillEvents[2].skillId == 'Skill0'
        file1SkillEvents[2].projectId == 'Proj3'
        file1SkillEvents[2].userId == user2.userName

        file1SkillEvents[3].skillId == 'Skill1'
        file1SkillEvents[3].projectId == 'Proj3'
        file1SkillEvents[3].userId == user2.userName

        file1SkillEvents[4].skillId == 'Skill2'
        file1SkillEvents[4].projectId == 'Proj3'
        file1SkillEvents[4].userId == user2.userName

        file2SkillEvents.size() == 1
        file2SkillEvents[0].skillId == 'Skill3'
        file2SkillEvents[0].projectId == 'Proj3'
        file2SkillEvents[0].userId == user2.userName
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
