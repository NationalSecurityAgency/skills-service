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


import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectWriter
import com.fasterxml.jackson.databind.SequenceWriter
import groovy.json.JsonOutput
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.PathResource
import org.springframework.core.io.WritableResource
import skills.SpringBootApp
import skills.auth.UserInfoService
import skills.controller.request.model.SkillEventRequest
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.storage.model.UserAttrs
import skills.utils.WaitFor
import spock.lang.IgnoreIf
import spock.lang.Shared

@Slf4j
@SpringBootTest(properties = [
        'skills.config.db-upgrade-in-progress=false',
        'skills.queued-event-path=./target',
        'skills.queued-event-path.commit-every-n-records=5',
        'skills.authorization.userInfoUri=https://localhost:8186/userInfo?dn={dn}',
        'skills.authorization.userQueryUri=https://localhost:8186/userQuery?query={query}',
        'skills.authorization.userInfoHealthCheckUri=https://localhost:8186/status'
], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = SpringBootApp)
class ReplayWALEventsIT extends DefaultIntSpec {
    @Shared
    File dir = new File('./target')

    SkillsService rootUser
    Map proj
    def skills
    def setup(){
        rootUser = createRootSkillService("rootUser")
        proj = SkillsFactory.createProject()
        Map subj = SkillsFactory.createSubject()
        skills = SkillsFactory.createSkills(2, 1, 1, 100)

        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
    }

    def "replay an event"() {
        createFileWithEvents(1, 1)

        int numRecordsToExpect = 1
        when:
        rootUser.runReplayEventsAfterUpgrade()
        WaitFor.wait { skillsService.getProjectUsers(proj.projectId).data.size() == numRecordsToExpect }
        def projectUsers = skillsService.getProjectUsers(proj.projectId)
        println JsonOutput.toJson(projectUsers)
        then:
        projectUsers.data.size() == numRecordsToExpect
        projectUsers.data[0].userId == 'user1'
    }

    def "replay events from multiple files"() {
        createFileWithEvents(1, 5)
        createFileWithEvents(2, 3, 6)

        int numRecordsToExpect = 8
        when:
        rootUser.runReplayEventsAfterUpgrade()
        WaitFor.wait { skillsService.getProjectUsers(proj.projectId).data.size() == numRecordsToExpect }
        def projectUsers = skillsService.getProjectUsers(proj.projectId)
        then:
        projectUsers.data.size() == numRecordsToExpect
        projectUsers.data.userId.sort() == ['user1', 'user2', 'user3', 'user4', 'user5', 'user6', 'user7', 'user8'].sort()
    }

    def "some events will fail"() {
        PathResource file1 = new PathResource("./target/queued_skill_events-file1.jsonsequence")
        SequenceWriter sequenceWriter = createSequenceWriter(file1)
        sequenceWriter.write(new QueuedSkillEvent(
                projectId: proj.projectId, skillId: skills[0].skillId,
                userId: "user1", requestTime: new Date()
        ))
        sequenceWriter.write(new QueuedSkillEvent(
                projectId: proj.projectId, skillId: skills[0].skillId,
                userId: "user2", requestTime: null, // causes an error
        ))
        sequenceWriter.write(new QueuedSkillEvent(
                projectId: proj.projectId, skillId: skills[0].skillId,
                userId: "user3", requestTime: new Date()
        ))
        sequenceWriter.close()

        int numRecordsToExpect = 2
        when:
        rootUser.runReplayEventsAfterUpgrade()
        WaitFor.wait { skillsService.getProjectUsers(proj.projectId).data.size() == numRecordsToExpect }
        def projectUsers = skillsService.getProjectUsers(proj.projectId)
        then:
        projectUsers.data.size() == numRecordsToExpect
        projectUsers.data.userId.sort() == ['user1', 'user3'].sort()
    }

    @IgnoreIf({ env["SPRING_PROFILES_ACTIVE"] != "pki" })
    def "pki mode - some events with empty userId have incorrect ID type"() {
        List<String> users = getRandomUsers(3)
        List<SkillsService> usersServices = users.collect {
            createService(it)
        }
        List<UserAttrs> userAttrs = users.collect {
            userAttrsRepo.findByUserIdIgnoreCase(it)
        }

        PathResource file1 = new PathResource("./target/queued_skill_events-file2.jsonsequence")
        SequenceWriter sequenceWriter = createSequenceWriter(file1)
        sequenceWriter.write(new QueuedSkillEvent(
                projectId: proj.projectId, skillId: skills[0].skillId,
                userId: userAttrs[0].dn, requestTime: new Date(),
                skillEventRequest: new SkillEventRequest(
                        userId: null,
                        idType: UserInfoService.ID_IDTYPE,
                        timestamp: new Date().getTime()
                )
        ))
        sequenceWriter.write(new QueuedSkillEvent(
                projectId: proj.projectId, skillId: skills[0].skillId,
                userId: userAttrs[1].dn, requestTime: new Date(),
                skillEventRequest: new SkillEventRequest(
                        userId: userAttrs[2].dn,
                        idType: UserInfoService.DN_IDTYPE,
                        timestamp: new Date().getTime()
                )
        ))
        sequenceWriter.close()

        int numRecordsToExpect = 2
        SkillsService validRootUser = createService(users[2])
        rootUser.addRootRole(validRootUser.userName)
        when:
        validRootUser.runReplayEventsAfterUpgrade()
        WaitFor.wait { skillsService.getProjectUsers(proj.projectId).data.size() == numRecordsToExpect }
        def projectUsers = skillsService.getProjectUsers(proj.projectId)
        then:
        projectUsers.data.size() == numRecordsToExpect
        projectUsers.data.userId.sort() == [userAttrs[0].userId, userAttrs[2].userId].sort()
    }

    @IgnoreIf({ env["SPRING_PROFILES_ACTIVE"] != "pki" })
    def "pki mode - events with idType of 'ID' work correctly"() {
        List<String> users = getRandomUsers(3)
        List<SkillsService> usersServices = users.collect {
            createService(it)
        }
        List<UserAttrs> userAttrs = users.collect {
            userAttrsRepo.findByUserIdIgnoreCase(it)
        }

        PathResource file1 = new PathResource("./target/queued_skill_events-file3.jsonsequence")
        SequenceWriter sequenceWriter = createSequenceWriter(file1)
        sequenceWriter.write(new QueuedSkillEvent(
                projectId: proj.projectId, skillId: skills[0].skillId,
                userId: userAttrs[0].dn, requestTime: new Date(),
                skillEventRequest: new SkillEventRequest(
                        userId: userAttrs[1].userId,
                        idType: UserInfoService.ID_IDTYPE,
                        timestamp: new Date().getTime()
                )
        ))
        sequenceWriter.write(new QueuedSkillEvent(
                projectId: proj.projectId, skillId: skills[0].skillId,
                userId: userAttrs[0].dn, requestTime: new Date(),
                skillEventRequest: new SkillEventRequest(
                        userId: userAttrs[2].userId,
                        idType: UserInfoService.ID_IDTYPE,
                        timestamp: new Date().getTime()
                )
        ))
        sequenceWriter.close()

        int numRecordsToExpect = 2
        SkillsService validRootUser = createService(users[2])
        rootUser.addRootRole(validRootUser.userName)
        when:
        validRootUser.runReplayEventsAfterUpgrade()
        WaitFor.wait { skillsService.getProjectUsers(proj.projectId).data.size() == numRecordsToExpect }
        def projectUsers = skillsService.getProjectUsers(proj.projectId)
        then:
        projectUsers.data.size() == numRecordsToExpect
        projectUsers.data.userId.sort() == [userAttrs[1].userId, userAttrs[2].userId].sort()
    }

    @IgnoreIf({ env["SPRING_PROFILES_ACTIVE"] != "pki" })
    def "pki mode - when id type is not correct simply skip the event"() {
        List<String> users = getRandomUsers(4)
        List<SkillsService> usersServices = users.collect {
            createService(it)
        }
        List<UserAttrs> userAttrs = users.collect {
            userAttrsRepo.findByUserIdIgnoreCase(it)
        }

        PathResource file1 = new PathResource("./target/queued_skill_events-file3.jsonsequence")
        SequenceWriter sequenceWriter = createSequenceWriter(file1)
        sequenceWriter.write(new QueuedSkillEvent(
                projectId: proj.projectId, skillId: skills[0].skillId,
                userId: userAttrs[0].dn, requestTime: new Date(),
                skillEventRequest: new SkillEventRequest(
                        userId: userAttrs[1].userId,
                        idType: UserInfoService.ID_IDTYPE,
                        timestamp: new Date().getTime()
                )
        ))
        sequenceWriter.write(new QueuedSkillEvent(
                projectId: proj.projectId, skillId: skills[0].skillId,
                userId: userAttrs[0].dn, requestTime: new Date(),
                skillEventRequest: new SkillEventRequest(
                        userId: userAttrs[2].dn,
                        idType: UserInfoService.ID_IDTYPE,
                        timestamp: new Date().getTime()
                )
        ))
        sequenceWriter.write(new QueuedSkillEvent(
                projectId: proj.projectId, skillId: skills[0].skillId,
                userId: userAttrs[0].dn, requestTime: new Date(),
                skillEventRequest: new SkillEventRequest(
                        userId: userAttrs[3].userId,
                        idType: UserInfoService.ID_IDTYPE,
                        timestamp: new Date().getTime()
                )
        ))
        sequenceWriter.close()

        int numRecordsToExpect = 2
        SkillsService validRootUser = createService(users[2])
        rootUser.addRootRole(validRootUser.userName)
        when:
        validRootUser.runReplayEventsAfterUpgrade()
        WaitFor.wait { skillsService.getProjectUsers(proj.projectId).data.size() == numRecordsToExpect }
        def projectUsers = skillsService.getProjectUsers(proj.projectId)
        then:
        projectUsers.data.size() == numRecordsToExpect
        projectUsers.data.userId.sort() == [userAttrs[1].userId, userAttrs[3].userId].sort()
    }

    @Autowired
    DBCheckPointer dbCheckPointer

    def "checkpoint is respected"() {
        PathResource file1 = createFileWithEvents(1, 5)
        PathResource file2 = createFileWithEvents(2, 5, 6)
        PathResource file3 = createFileWithEvents(3, 3, 11)

        dbCheckPointer.recordRecord(file1.getFile().name, 1)
        dbCheckPointer.recordRecord(file2.getFile().name, 2)

        int numRecordsToExpect = 8
        when:
        rootUser.runReplayEventsAfterUpgrade()
        WaitFor.wait { skillsService.getProjectUsers(proj.projectId).data.size() == numRecordsToExpect }
        def projectUsers = skillsService.getProjectUsers(proj.projectId)
        then:
        projectUsers.data.size() == numRecordsToExpect
        projectUsers.data.userId.sort() == ['user3', 'user4', 'user5', 'user9', 'user10', 'user11', 'user12', 'user13'].sort()
    }

    private PathResource createFileWithEvents(int fileNum, int numEvents, int userIdStart = 1) {
        PathResource path = new PathResource("./target/queued_skill_events-file${fileNum}.jsonsequence")
        SequenceWriter sequenceWriter = createSequenceWriter(path)

        numEvents.times {
            QueuedSkillEvent queuedSkillEvent = new QueuedSkillEvent(
                    projectId: proj.projectId,
                    skillId: skills[0].skillId,
                    userId: "user${it + userIdStart}",
                    requestTime: new Date(),
            )
            sequenceWriter.write(queuedSkillEvent)
        }
        sequenceWriter.close()

        return path
    }

    private void createFileWithEvents(PathResource path, int numEvents, int userIdStart = 1) {
        SequenceWriter sequenceWriter = createSequenceWriter(path)

        numEvents.times {
            QueuedSkillEvent queuedSkillEvent = new QueuedSkillEvent(
                    projectId: proj.projectId,
                    skillId: skills[0].skillId,
                    userId: "user${it + userIdStart}",
                    requestTime: new Date(),
            )
            sequenceWriter.write(queuedSkillEvent)
        }
        sequenceWriter.close()
    }

    private SequenceWriter createSequenceWriter(PathResource path) {
        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(((WritableResource) path).getOutputStream(), "UTF-8"))
        ObjectWriter objectWriter = new ObjectMapper().writerFor(QueuedSkillEvent)
        SequenceWriter sequenceWriter = objectWriter.writeValues(bufferedWriter)
        return sequenceWriter
    }

}
