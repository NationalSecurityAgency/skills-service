/**
 * Copyright 2026 SkillTree
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
package skills.intTests.crossProject

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.storage.model.SkillDef
import spock.util.concurrent.PollingConditions

import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

class SharedSkillsConcurrencySpec extends DefaultIntSpec {

    @Autowired
    JdbcTemplate jdbcTemplate

    def "concurrent sharing serializes conflict checks - first=#firstDestination second=#secondDestination"() {
        setup:
        List<String> users = getRandomUsers(2)
        List<SkillsService> clients = users.collect { String user -> createService(user) }
        SkillsService owner = clients[0]
        (1..3).each { int project -> owner.createProject(SkillsFactory.createProject(project)) }
        Map subject = SkillsFactory.createSubject(1, 1)
        Map skill = SkillsFactory.createSkill(1, 1, 1)
        owner.createSubject(subject)
        owner.createSkill(skill)
        owner.addProjectAdmin(skill.projectId, users[1])
        Integer skillRefId = skillDefRepo.getIdByProjectIdAndSkillIdAndType(skill.projectId, skill.skillId, SkillDef.ContainerType.Skill)
        ExecutorService executor = Executors.newFixedThreadPool(2)
        List<Future<Map>> requests = []

        when:
        runInTransaction {
            skillDefRepo.findByIdForUpdate(skillRefId)
            Integer lockHolderPid = jdbcTemplate.queryForObject('select pg_backend_pid()', Integer)
            requests.add(submitShare(executor, clients[0], skill, firstDestination))
            waitForBlockedRequests(lockHolderPid, 1)
            requests.add(submitShare(executor, clients[1], skill, secondDestination))
            waitForBlockedRequests(lockHolderPid, 2)
            assert requests.every { Future<Map> request -> !request.isDone() }
            return null
        }
        List<Map> responses = requests.collect { Future<Map> request -> request.get(60, TimeUnit.SECONDS) }
        Map outgoing = owner.getSharedSkills(skill.projectId)

        then:
        responses[0].success
        responses[1].success == secondSucceeds
        if (!secondSucceeds) {
            assert responses[1].httpStatus == HttpStatus.BAD_REQUEST
            assert responses[1].error.errorCode == 'AccessDenied'
            assert responses[1].error.explanation.contains(firstDestination == 'ALL_SKILLS_PROJECTS'
                    ? 'This skill is already shared with All Projects.'
                    : 'This skill is already shared with project Test Project#2.')
        }
        outgoing.totalCount == (secondSucceeds ? 2 : 1)
        outgoing.data.collect { Map row -> [row.skillId, row.projectId, row.sharedWithAllProjects] }.sort { List row -> row[1] ?: '' } == expectedRows

        and: 'incoming pages and single-result sharing lookups remain usable'
        Map incoming = owner.getSharedWithMeSkills('TestProject2')
        incoming.totalCount == 1
        incoming.data.collect { Map row -> [row.skillId, row.projectId] } == [['skill1', 'TestProject1']]
        Map lookup = clients[1].isSkillShared(skill.projectId, skill.skillId, 'TestProject2')
        lookup.data
        lookup.explanation == (firstDestination == 'ALL_SKILLS_PROJECTS'
                ? 'This skill is already shared with All Projects.'
                : 'This skill is already shared with project Test Project#2.')

        when: 'the resulting share is removed through the endpoint'
        owner.deleteShared(skill.projectId, skill.skillId, firstDestination)

        then:
        owner.getSharedSkills(skill.projectId).totalCount == (secondSucceeds ? 1 : 0)
        owner.getSharedWithMeSkills('TestProject2').totalCount == 0

        cleanup:
        executor?.shutdown()
        if (executor) {
            assert executor.awaitTermination(70, TimeUnit.SECONDS): 'Concurrent share requests did not finish during cleanup'
        }

        where:
        firstDestination     | secondDestination    | secondSucceeds | expectedRows
        'ALL_SKILLS_PROJECTS' | 'ALL_SKILLS_PROJECTS' | false          | [['skill1', null, true]]
        'ALL_SKILLS_PROJECTS' | 'TestProject2'       | false          | [['skill1', null, true]]
        'TestProject2'       | 'ALL_SKILLS_PROJECTS' | false          | [['skill1', 'TestProject2', false]]
        'TestProject2'       | 'TestProject2'       | false          | [['skill1', 'TestProject2', false]]
        'TestProject2'       | 'TestProject3'       | true           | [['skill1', 'TestProject2', false], ['skill1', 'TestProject3', false]]
    }

    def "sharing a different skill does not wait for a locked skill"() {
        setup:
        List<String> users = getRandomUsers(2)
        List<SkillsService> clients = users.collect { String user -> createService(user) }
        SkillsService owner = clients[0]
        owner.createProject(SkillsFactory.createProject(1))
        owner.createProject(SkillsFactory.createProject(2))
        owner.createSubject(SkillsFactory.createSubject(1, 1))
        List<Map> skills = SkillsFactory.createSkills(2, 1, 1)
        owner.createSkills(skills)
        owner.addProjectAdmin('TestProject1', users[1])
        Integer skillRefId = skillDefRepo.getIdByProjectIdAndSkillIdAndType('TestProject1', 'skill1', SkillDef.ContainerType.Skill)
        ExecutorService executor = Executors.newFixedThreadPool(2)
        Future<Map> blockedRequest
        Future<Map> independentRequest

        when:
        runInTransaction {
            skillDefRepo.findByIdForUpdate(skillRefId)
            Integer lockHolderPid = jdbcTemplate.queryForObject('select pg_backend_pid()', Integer)
            blockedRequest = submitShare(executor, clients[0], skills[0], 'TestProject2')
            waitForBlockedRequests(lockHolderPid, 1)
            independentRequest = submitShare(executor, clients[1], skills[1], 'TestProject2')
            assert independentRequest.get(10, TimeUnit.SECONDS).success
            assert !blockedRequest.isDone()
            Map outgoing = owner.getSharedSkills('TestProject1')
            assert outgoing.totalCount == 1
            assert outgoing.data*.skillId == ['skill2']
            return null
        }

        then:
        blockedRequest.get(60, TimeUnit.SECONDS).success
        Map incoming = owner.getSharedWithMeSkills('TestProject2')
        incoming.totalCount == 2
        incoming.data*.skillId == ['skill1', 'skill2']

        cleanup:
        executor?.shutdown()
        if (executor) {
            assert executor.awaitTermination(70, TimeUnit.SECONDS): 'Concurrent share requests did not finish during cleanup'
        }
    }

    private static Future<Map> submitShare(ExecutorService executor, SkillsService client, Map skill, String destination) {
        return executor.submit({ ->
            try {
                client.shareSkill(skill.projectId, skill.skillId, destination)
                return [success: true]
            } catch (SkillsClientException exception) {
                return [success: false, httpStatus: exception.httpStatus, error: exception.res.body]
            }
        } as Callable<Map>)
    }

    private void waitForBlockedRequests(Integer lockHolderPid, int expectedCount) {
        // Follow the live PostgreSQL blocking chain: a waiter can block another
        // waiter. SQL is only used to observe locks, not to read or create shares.
        new PollingConditions(timeout: 10).eventually {
            assert jdbcTemplate.queryForObject('''
                with recursive blocked(pid) as (
                    select pid from pg_locks where ? = any(pg_blocking_pids(pid))
                    union
                    select locks.pid from pg_locks locks
                        join blocked on blocked.pid = any(pg_blocking_pids(locks.pid))
                )
                select count(*) from blocked
            ''', Integer, lockHolderPid) == expectedCount
        }
    }
}
