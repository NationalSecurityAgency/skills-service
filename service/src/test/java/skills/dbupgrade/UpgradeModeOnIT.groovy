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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import skills.SpringBootApp
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsService
import skills.storage.model.UserAttrs
import skills.storage.repos.UserAttrsRepo
import spock.lang.Shared

@Slf4j
@SpringBootTest(properties = [
        'server.port=8093',
        'skills.config.db-upgrade-in-progress=true',
        'skills.queued-event-path=./target',
        'skills.queued-event-path.commit-every-n-records=5',
        'skills.authorization.userInfoUri=https://localhost:8185/userInfo?dn={dn}',
        'skills.authorization.userQueryUri=https://localhost:8185/userQuery?query={query}',
        'skills.authorization.userInfoHealthCheckUri=https://localhost:8185/status'
], webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = SpringBootApp)
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

    @Autowired
    UserAttrsRepo userAttrsRepo

    def "store skill events in WAL when in upgrade mode"() {
        List<String> users = getRandomUsers(2)
        when:
        SkillsService user1 = createService(users[0]) // will fill in DN in case of PKI MOde
        SkillsService user2 = createService(users[1])
        skillsService.addSkill(['projectId': 'Proj1', skillId: 'CreateProject'], users[0], new Date())
        skillsService.addSkill(['projectId': 'Proj2', skillId: 'CreateSubject'], users[1], new Date())

        4.times {
            skillsService.addSkill(['projectId': 'Proj3', skillId: "Skill${it}"], users[1], new Date())
        }

        List<File> files = dir.listFiles().findAll({ it.name.endsWith('.jsonsequence') }).sort {it.lastModified() }
        List<QueuedSkillEvent> file1SkillEvents = getEvents(files[0].text)
        List<QueuedSkillEvent> file2SkillEvents = getEvents(files[1].text)

        UserAttrs user1Attrs = userAttrsRepo.findByUserIdIgnoreCase(users[0])
        UserAttrs user2Attrs = userAttrsRepo.findByUserIdIgnoreCase(users[1])

        then:
        files.size() == 2
        file1SkillEvents.size() == 5
        file1SkillEvents[0].skillId == 'CreateProject'
        file1SkillEvents[0].projectId == 'Proj1'
        file1SkillEvents[0].userId == user1Attrs.dn ?: user1Attrs.userId

        file1SkillEvents[1].skillId == 'CreateSubject'
        file1SkillEvents[1].projectId == 'Proj2'
        file1SkillEvents[1].userId == user2Attrs.dn ?: user2Attrs.userId

        file1SkillEvents[2].skillId == 'Skill0'
        file1SkillEvents[2].projectId == 'Proj3'
        file1SkillEvents[2].userId == user2Attrs.dn ?: user2Attrs.userId

        file1SkillEvents[3].skillId == 'Skill1'
        file1SkillEvents[3].projectId == 'Proj3'
        file1SkillEvents[3].userId == user2Attrs.dn ?: user2Attrs.userId

        file1SkillEvents[4].skillId == 'Skill2'
        file1SkillEvents[4].projectId == 'Proj3'
        file1SkillEvents[4].userId == user2Attrs.dn ?: user2Attrs.userId

        file2SkillEvents.size() == 1
        file2SkillEvents[0].skillId == 'Skill3'
        file2SkillEvents[0].projectId == 'Proj3'
        file2SkillEvents[0].userId == user2Attrs.dn ?: user2Attrs.userId
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
