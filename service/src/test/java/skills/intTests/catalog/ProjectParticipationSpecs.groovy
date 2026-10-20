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
package skills.intTests.catalog

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.http.HttpStatus
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpEntity
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import spock.lang.IgnoreIf
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.services.ProjectParticipationService
import skills.storage.model.ProjectParticipation
import skills.storage.model.ProjectParticipation.BackfillStatus
import skills.storage.repos.ProjectParticipationRepo

import static skills.intTests.utils.SkillsFactory.*

class ProjectParticipationSpecs extends CatalogIntSpec {
    private String learnerId
    private String otherLearnerId
    private String ownerId

    def setup() {
        (learnerId, otherLearnerId, ownerId) = getRandomUsers(3)
    }

    @Autowired ProjectParticipationRepo participationRepo
    @Autowired ProjectParticipationService participationService
    @Autowired JdbcTemplate jdbcTemplate
    @Autowired PlatformTransactionManager transactionManager

    def "initialization records lifetime membership and repeat calls advance updated while preserving created"() {
        Map project = createProject(1)
        skillsService.createProject(project)
        SkillsService learner = createService(learnerId)

        when:
        Map result = learner.initializeProjectProgress(project.projectId.toUpperCase())
        ProjectParticipation first = participationRepo.findByProjectIdAndUserId(project.projectId, learnerId)
        Map repeated = learner.initializeProjectProgress(project.projectId)
        ProjectParticipation second = participationRepo.findByProjectIdAndUserId(project.projectId, learnerId)

        then:
        result.ready
        result.backfillStatus == 'NOT_NEEDED'
        repeated == result
        first.created
        first.updated
        second.id == first.id
        second.created == first.created
        second.updated > first.updated
        participationService.isEligible(project.projectId, learnerId)

        when:
        skillsService.deleteProject(project.projectId)
        then:
        !participationRepo.findById(first.id).present
    }

    def "ordinary reads do not enroll and invalid projects or target overrides leave no participation"() {
        Map project = createProject(1)
        Map subject = createSubject(1, 1)
        skillsService.createProject(project)
        skillsService.createSubject(subject)
        SkillsService learner = createService(learnerId)

        when:
        learner.getSkillsSummaryForCurrentUser(project.projectId)
        learner.getUserLevel(project.projectId)
        learner.getCustomIconCssForProject(project.projectId)
        learner.headSkillsSummaryForCurrentUser(project.projectId)
        then:
        !participationRepo.existsByProjectIdAndUserId(project.projectId, learnerId)

        when:
        learner.initializeProjectProgress(project.projectId, [userId: 'someoneElse'])
        then:
        thrown(SkillsClientException)
        !participationRepo.existsByProjectIdAndUserId(project.projectId, learnerId)

        when:
        learner.initializeProjectProgress('missingProject')
        then:
        SkillsClientException missingProjectError = thrown()
        missingProjectError.message.contains('errorCode:ProjectNotFound')
        !participationRepo.existsByProjectIdAndUserId('missingProject', learnerId)

        when:
        learner.initializeProjectProgress(project.projectId)
        ProjectParticipation first = participationRepo.findByProjectIdAndUserId(project.projectId, learnerId)
        learner.getSkillsSummaryForCurrentUser(project.projectId)
        learner.getUserLevel(project.projectId)
        learner.headSkillsSummaryForCurrentUser(project.projectId)
        then:
        participationRepo.findById(first.id).get().updated == first.updated
    }

    @IgnoreIf({env.SPRING_PROFILES_ACTIVE == 'pki'})
    def "unauthenticated and invalid CSRF requests do not enroll"() {
        Map project = createProject(1)
        skillsService.createProject(project)
        SkillsService learner = createService(learnerId)
        String url = "${learner.wsHelper.skillsService}/api/projects/${project.projectId}/progress/initialize"

        when:
        new RestTemplate().postForEntity(url, [:], String)
        then:
        HttpStatusCodeException unauthenticated = thrown()
        unauthenticated.statusCode.value() in [401, 403]
        !participationRepo.existsByProjectIdAndUserId(project.projectId, learnerId)

        when:
        HttpHeaders headers = new HttpHeaders()
        headers.set('X-XSRF-TOKEN', 'invalid')
        ResponseEntity<String> csrf = learner.wsHelper.restTemplateWrapper.postForEntity(url, new HttpEntity([:], headers), String)
        then:
        csrf.statusCode == HttpStatus.FORBIDDEN
        !participationRepo.existsByProjectIdAndUserId(project.projectId, learnerId)
    }

    def "local events qualify without participation but source events and imported credit do not"() {
        Map source = createProject(1)
        Map sourceSubject = createSubject(1, 1)
        Map skill = createSkill(1, 1, 1, 0, 5, 0, 250)
        Map destination = createProject(2)
        Map destinationSubject = createSubject(2, 1)
        skillsService.createProjectAndSubjectAndSkills(source, sourceSubject, [skill])
        skillsService.createProject(destination)
        skillsService.createSubject(destinationSubject)
        skillsService.addSkill([projectId: source.projectId, skillId: skill.skillId], otherLearnerId)
        skillsService.exportSkillToCatalog(source.projectId, skill.skillId)
        skillsService.bulkImportSkillsFromCatalogAndFinalize(destination.projectId, destinationSubject.subjectId,
                [[projectId: source.projectId, skillId: skill.skillId]])

        expect:
        participationService.isEligible(source.projectId, otherLearnerId)
        !participationRepo.existsByProjectIdAndUserId(source.projectId, otherLearnerId)
        !participationService.isEligible(destination.projectId, otherLearnerId)

        when:
        SkillsService learner = createService(learnerId)
        Map initialized = learner.initializeProjectProgress(destination.projectId)
        then:
        initialized.ready
        initialized.backfillStatus == 'COMPLETED'
        participationRepo.findByProjectIdAndUserId(destination.projectId, learnerId).backfillStatus == BackfillStatus.COMPLETED
    }

    def "activity touches advance updated without overwriting pending status or created"() {
        Map project = createProject(1)
        skillsService.createProject(project)
        SkillsService learner = createService(learnerId)
        learner.initializeProjectProgress(project.projectId)
        ProjectParticipation row = participationRepo.findByProjectIdAndUserId(project.projectId, learnerId)
        Date created = row.created
        row.backfillStatus = BackfillStatus.PENDING
        participationRepo.save(row)

        when:
        learner.initializeProjectProgress(project.projectId)
        then:
        participationRepo.readStatus(project.projectId, learnerId) == BackfillStatus.PENDING
        ProjectParticipation after = participationRepo.findById(row.id).get()
        after.backfillStatus == BackfillStatus.PENDING
        after.created == created
        after.updated > row.updated
    }

    def "initialization does not move future activity timestamps backwards"() {
        Map project = createProject(1)
        skillsService.createProject(project)
        SkillsService learner = createService(learnerId)
        learner.initializeProjectProgress(project.projectId)
        ProjectParticipation first = participationRepo.findByProjectIdAndUserId(project.projectId, learnerId)
        jdbcTemplate.update("update project_participation set updated = updated + interval '2 hours' where id = ?", first.id)
        ProjectParticipation future = participationRepo.findById(first.id).get()

        when:
        learner.initializeProjectProgress(project.projectId)
        then:
        participationRepo.findById(first.id).get().updated == future.updated
    }

    def "concurrent initialization requests create one committed record"() {
        Map project = createProject(1)
        skillsService.createProject(project)
        List<SkillsService> clients = (1..4).collect { createService(learnerId) }
        ExecutorService executor = Executors.newFixedThreadPool(4)
        CountDownLatch start = new CountDownLatch(1)

        when:
        List<Future<Map>> futures = clients.collect { SkillsService client -> executor.submit({
            assert start.await(10, TimeUnit.SECONDS)
            client.initializeProjectProgress(project.projectId)
        } as Callable<Map>) }
        start.countDown()
        List<Map> results = futures.collect { Future<Map> future -> future.get(20, TimeUnit.SECONDS) }
        then:
        results.every { Map result -> result.ready && result.backfillStatus == 'NOT_NEEDED' }
        jdbcTemplate.queryForObject('select count(*) from project_participation where project_id = ? and user_id = ?',
                Integer, project.projectId, learnerId) == 1

        cleanup:
        executor.shutdownNow()
    }

    def "progress deletion preserves membership and actual user deletion cascades"() {
        Map project = createProject(1)
        Map subject = createSubject(1, 1)
        Map skill = createSkill(1, 1, 1, 0, 5, 0, 250)
        skillsService.createProjectAndSubjectAndSkills(project, subject, [skill])
        SkillsService learner = createService(learnerId)
        learner.initializeProjectProgress(project.projectId)
        skillsService.addSkill(skill, learnerId)
        ProjectParticipation before = participationRepo.findByProjectIdAndUserId(project.projectId, learnerId)

        when:
        skillsService.deleteAllSkillEvents([projectId: project.projectId, userId: learnerId])
        then:
        participationRepo.findById(before.id).get().created == before.created
        participationService.isEligible(project.projectId, learnerId)

        when:
        jdbcTemplate.update('delete from user_attrs where user_id = ?', learnerId)
        then:
        !participationRepo.findById(before.id).present
    }

    def "participation writes roll back with their transaction"() {
        Map project = createProject(1)
        skillsService.createProject(project)
        createService(learnerId)

        when:
        new TransactionTemplate(transactionManager).execute {
            participationService.initialize(project.projectId, learnerId)
            throw new IllegalStateException('rollback')
        }
        then:
        thrown(IllegalStateException)
        !participationRepo.existsByProjectIdAndUserId(project.projectId, learnerId)
    }

    def "invite-only access is checked before recording participation"() {
        Map project = createProject(1)
        skillsService.createProject(project)
        skillsService.changeSetting(project.projectId, 'invite_only', [projectId: project.projectId, setting: 'invite_only', value: 'true'])
        SkillsService learner = createService(learnerId)

        when:
        learner.initializeProjectProgress(project.projectId)
        then:
        SkillsClientException error = thrown()
        error.httpStatus == HttpStatus.FORBIDDEN
        !participationRepo.existsByProjectIdAndUserId(project.projectId, learnerId)
    }

    def "initialization rejects identity overrides in query parameters and request bodies"() {
        Map project = createProject(1)
        skillsService.createProject(project)
        SkillsService learner = createService(learnerId)

        when:
        learner.initializeProjectProgress(project.projectId, params, body)
        then:
        SkillsClientException error = thrown()
        error.httpStatus == HttpStatus.BAD_REQUEST
        error.message.contains('errorCode:BadParam')
        error.message.contains(expectedMessage)
        !participationRepo.existsByProjectIdAndUserId(project.projectId, learnerId)
        !participationRepo.existsByProjectIdAndUserId(project.projectId, 'inspected@skills.org')

        where:
        params                          | body                            | expectedMessage
        [userId: 'inspected@skills.org'] | [:]                             | 'userId must not be supplied'
        [userId: '']                    | [:]                             | 'userId must not be supplied'
        [idType: 'ID']                  | [:]                             | 'idType must not be supplied'
        [idType: '']                    | [:]                             | 'idType must not be supplied'
        [:]                            | [userId: 'inspected@skills.org'] | 'Progress initialization does not accept request body fields'
        [:]                            | [idType: 'ID']                  | 'Progress initialization does not accept request body fields'
    }

    def "participation and activity are isolated by project and authenticated learner"() {
        List<Map> projects = [createProject(1), createProject(2)]
        projects.each { Map project -> skillsService.createProject(project) }
        SkillsService learner = createService(learnerId)
        SkillsService otherLearner = createService(otherLearnerId)

        when:
        learner.initializeProjectProgress(projects[0].projectId)
        ProjectParticipation first = participationRepo.findByProjectIdAndUserId(projects[0].projectId, learner.userName)
        otherLearner.initializeProjectProgress(projects[1].projectId)
        then:
        !participationService.isEligible(projects[1].projectId, learner.userName)
        !participationService.isEligible(projects[0].projectId, otherLearner.userName)
        participationRepo.findById(first.id).get().updated == first.updated

        when:
        learner.initializeProjectProgress(projects[1].projectId)
        then:
        participationService.isEligible(projects[1].projectId, learner.userName)
        participationRepo.findById(first.id).get().updated == first.updated
    }

    def "community access is checked before recording participation"() {
        SkillsService owner = createService(ownerId)
        SkillsService root = createRootSkillService()
        root.saveUserTag(owner.userName, 'dragons', ['DivineDragon'])
        Map project = createProject(1)
        project.enableProtectedUserCommunity = true
        owner.createProject(project)
        SkillsService learner = createService(learnerId)

        when:
        learner.initializeProjectProgress(project.projectId)
        then:
        SkillsClientException error = thrown()
        error.httpStatus == HttpStatus.FORBIDDEN
        !participationRepo.existsByProjectIdAndUserId(project.projectId, learnerId)
    }

    @IgnoreIf({env.SPRING_PROFILES_ACTIVE == 'pki'})
    def "trusted embedded identity enrolls proxy learner only in its authorized project"() {
        Map project = createProject(1)
        Map other = createProject(2)
        skillsService.createProject(project)
        skillsService.createProject(other)
        skillsService.setProxyCredentials(project.projectId, skillsService.getClientSecret(project.projectId))

        when:
        Map result = skillsService.initializeProjectProgressAsProxy(project.projectId, learnerId.toUpperCase(Locale.ROOT))
        then:
        result.ready
        participationRepo.existsByProjectIdAndUserId(project.projectId, learnerId)
        !participationRepo.existsByProjectIdAndUserId(project.projectId, skillsService.userName)

        when:
        skillsService.initializeProjectProgressAsProxy(other.projectId, learnerId.toUpperCase(Locale.ROOT))
        then:
        SkillsClientException error = thrown()
        error.httpStatus == HttpStatus.FORBIDDEN
        !participationRepo.existsByProjectIdAndUserId(other.projectId, learnerId)
    }
}
