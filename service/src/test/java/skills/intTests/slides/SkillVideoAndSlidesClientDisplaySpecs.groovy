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
package skills.intTests.slides

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.jdbc.core.JdbcTemplate
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsService
import skills.services.admin.skillReuse.SkillReuseIdUtil
import skills.services.attributes.ExpirationAttrs
import skills.services.settings.Settings
import skills.storage.model.SkillAttributesDef
import skills.storage.model.SkillAttributesDef.SkillAttributesType
import skills.storage.model.SkillDef
import skills.storage.repos.SkillAttributesDefRepo
import skills.tasks.config.TaskConfig
import spock.lang.Unroll
import spock.util.concurrent.PollingConditions

import java.nio.file.Files
import java.time.Instant
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

import static skills.intTests.utils.SkillsFactory.*

class SkillVideoAndSlidesClientDisplaySpecs extends DefaultIntSpec {

    private static final String VIDEO_URL = 'https://example.org/training.mp4'
    private static final String TAG_ID = 'trainingmedia'

    @Autowired
    TaskConfig taskConfig

    @Autowired
    SkillAttributesDefRepo skillAttributesDefRepo

    @Autowired
    JdbcTemplate jdbcTemplate

    def "concurrent first-time #attributeType saves create exactly one attribute row"() {
        setup:
        List<String> users = getRandomUsers(2)
        List<SkillsService> clients = users.collect { String user -> createService(user) }
        Map project = createProject(1)
        Map subject = createSubject(1, 1)
        Map skill = createSkills(1, 1, 1, 100)[0]
        clients[0].createProjectAndSubjectAndSkills(project, subject, [skill])
        clients[0].addProjectAdmin(project.projectId, users[1])
        Integer skillRefId = skillDefRepo.getIdByProjectIdAndSkillIdAndType(project.projectId, skill.skillId, SkillDef.ContainerType.Skill)
        assert skillAttributesDefRepo.findAllBySkillRefId(skillRefId).isEmpty()

        ExecutorService executor = Executors.newFixedThreadPool(2)
        List<Future<Map>> requests = []
        List<String> urls = ['https://example.org/first', 'https://example.org/second']

        when:
        // Hold the real project lock in a test transaction. Both HTTP
        // requests must queue behind it before either may create an attribute.
        runInTransaction {
            lockingService.lockProject(project.projectId)
            Integer lockHolderPid = jdbcTemplate.queryForObject('select pg_backend_pid()', Integer)
            requests = (0..1).collect { int index ->
                executor.submit({ ->
                    if (attributeType == SkillAttributesType.Video) {
                        return clients[index].saveSkillVideoAttributes(project.projectId, skill.skillId, [videoUrl: urls[index]]) as Map
                    }
                    return clients[index].saveSlidesAttributes(project.projectId, skill.skillId, [url: urls[index]]) as Map
                } as Callable<Map>)
            }
            // PostgreSQL can queue the second waiter behind the first waiter rather
            // than directly behind the holder. Follow the whole blocking chain.
            // pg_locks exposes live lock state without pg_stat_activity's cached
            // statistics snapshot inside this coordinating transaction.
            // SQL is only for lock observation; fixture and attribute checks use
            // HTTP endpoints and repositories.
            new PollingConditions(timeout: 10).eventually {
                assert jdbcTemplate.queryForObject('''
                    with recursive blocked(pid) as (
                        select pid from pg_locks where ? = any(pg_blocking_pids(pid))
                        union
                        select locks.pid from pg_locks locks
                            join blocked on blocked.pid = any(pg_blocking_pids(locks.pid))
                    )
                    select count(*) from blocked
                ''', Integer, lockHolderPid) == 2
            }
            assert requests.every { Future<Map> request -> !request.isDone() }
            assert skillAttributesDefRepo.findAllBySkillRefId(skillRefId).isEmpty()
            return null
        }
        // Exiting the transaction releases the coordinating lock (rollback also
        // releases it if any synchronization assertion fails).
        List<Map> responses = requests.collect { Future<Map> request -> request.get(60, TimeUnit.SECONDS) }
        List<SkillAttributesDef> rows = skillAttributesDefRepo.findAllBySkillRefId(skillRefId).findAll { SkillAttributesDef row -> row.type == attributeType }

        then:
        responses.every { Map response -> response.success }
        responses.collect { Map response -> response[urlField] } == urls
        rows.size() == 1

        and: 'both admins can read the resulting settings through the endpoint'
        List<Map> saved = clients.collect { SkillsService client ->
            attributeType == SkillAttributesType.Video ?
                    client.getSkillVideoAttributes(project.projectId, skill.skillId) :
                    client.getSlidesAttributes(project.projectId, skill.skillId)
        }
        saved[0][urlField] in urls
        saved[1][urlField] == saved[0][urlField]

        cleanup:
        executor?.shutdown()
        if (executor) {
            assert executor.awaitTermination(70, TimeUnit.SECONDS): 'Concurrent requests did not finish during cleanup'
        }

        where:
        attributeType             | urlField
        SkillAttributesType.Video | 'videoUrl'
        SkillAttributesType.Slides | 'url'
    }

    @Unroll
    def "#endpoint summary lists a skill with video and PDF slides once (grouped=#grouped)"() {
        given:
        String fixtureEndpoint = endpoint in ['badges overview', 'my badges'] ? 'global badge' :
                endpoint == 'tags overview' ? 'tag' : endpoint
        Map fixture = createMediaFixture(fixtureEndpoint, grouped)
        SkillsService learner = fixture.learner
        if (endpoint == 'tags overview') {
            skillsService.addSkill(fixture.skills.first(), learner.userName)
        }
        if (endpoint == 'my badges') {
            String productionMode = Settings.PRODUCTION_MODE.settingName
            skillsService.changeSetting(fixture.project.projectId, productionMode,
                    [projectId: fixture.project.projectId, setting: productionMode, value: 'true'])
            learner.addMyProject(fixture.project.projectId)
        }

        when:
        Map summary = loadSummary(learner, fixture, endpoint)
        List<Map> displayedSkills = summary.skills
        if (grouped && endpoint == 'subject') {
            assert displayedSkills*.skillId == [fixture.group.skillId]
            displayedSkills = displayedSkills.first().children
        }

        then:
        // The fixture also includes video-only, slides-only, and no-media controls.
        if (endpoint == 'tags overview') {
            // Overview entries omit skill IDs, so verify their count and point values.
            assert displayedSkills.size() == 4
            assert displayedSkills*.points.sort() == [0, 0, 0, 100]
            assert displayedSkills.every { Map skill -> skill.totalPoints == 100 }
            assert displayedSkills*.points.sum() == 100
            assert displayedSkills*.totalPoints.sum() == 400
            assert summary.skillsAchieved == 1
        } else {
            assert displayedSkills*.skillId.sort() == fixture.skills*.skillId.sort()
            assert displayedSkills.every { Map skill -> skill.points == 0 && skill.totalPoints == 100 }
        }
        if (endpoint in ['subject', 'group']) {
            assert summary.totalPoints == 400
            assert summary.totalSkills == 4
        } else if (endpoint in ['tag', 'tags overview']) {
            assert summary.totalSkills == 4
        } else {
            assert summary.numTotalSkills == 4
        }
        if (endpoint in ['global badge', 'badges overview', 'my badges']) {
            assert summary.projectLevelsAndSkillsSummaries.size() == 1
            assert summary.projectLevelsAndSkillsSummaries.first().projectId == fixture.project.projectId
            assert summary.projectLevelsAndSkillsSummaries.first().skills*.skillId.sort() == fixture.skills*.skillId.sort()
        }

        where:
        endpoint          | grouped
        'subject'         | false
        'subject'         | true
        'group'           | true
        'badge'           | false
        'tag'             | false
        'global badge'    | false
        'tags overview'   | false
        'badges overview' | false
        'my badges'       | false
    }

    @Unroll
    def "#endpoint descriptions return video and PDF slides on one skill (grouped=#grouped)"() {
        given:
        Map fixture = createMediaFixture(endpoint, grouped)
        SkillsService learner = fixture.learner

        when:
        List<Map> descriptions = loadDescriptions(learner, fixture, endpoint)

        then:
        List<String> expectedIds = fixture.skills.collect { Map skill -> skill.skillId }
        if (grouped && endpoint == 'subject') {
            expectedIds.add(fixture.group.skillId)
        }
        descriptions*.skillId.sort() == expectedIds.sort()
        fixture.skills.eachWithIndex { Map skill, int index ->
            Map description = descriptions.find { Map item -> item.skillId == skill.skillId }
            assert description.description == skill.description
            assert description.href == skill.helpUrl
            assertMediaSummaries(learner, description, index)
        }

        where:
        endpoint  | grouped
        'subject' | false
        'subject' | true
        'group'   | true
        'badge'   | false
        'tag'     | false
    }

    @Unroll
    def "#endpoint summary retains expiration metadata when a skill also has video and slides"() {
        given:
        Map fixture = createMediaFixture(endpoint, false)
        SkillsService learner = fixture.learner
        Date nextExpiration = Date.parse('yyyy-MM-dd', '2100-01-15')
        skillsService.saveSkillExpirationAttributes(fixture.project.projectId, fixture.skills.first().skillId,
                [expirationType: ExpirationAttrs.YEARLY, every: 1, monthlyDay: 15, nextExpirationDate: nextExpiration])
        Instant expectedExpiration = taskConfig.getExpireUserAchievementsTaskExecutionTime(nextExpiration.toInstant())

        when:
        Map summary = loadSummary(learner, fixture, endpoint)

        then:
        summary.skills*.skillId.sort() == fixture.skills*.skillId.sort()
        Instant.parse(summary.skills.find { Map skill -> skill.skillId == fixture.skills.first().skillId }.expirationDate) == expectedExpiration
        summary.skills.findAll { Map skill -> skill.skillId != fixture.skills.first().skillId }.every { Map skill ->
            skill.expirationDate == null
        }

        where:
        endpoint << ['subject', 'global badge']
    }

    @Unroll
    def "#copyType skill descriptions retain both media configurations without duplicates (grouped=#grouped)"() {
        given:
        Map fixture = createMediaFixture('subject', false)
        SkillsService learner = fixture.learner
        Map targetProject = copyType == 'imported' ? createProject(2) : fixture.project
        Map targetSubject = copyType == 'imported' ? createSubject(2, 1) : createSubject(1, 2)
        Map targetGroup = createSkillsGroup(copyType == 'imported' ? 2 : 1, copyType == 'imported' ? 1 : 2, 50)
        List<String> sourceSkillIds = fixture.skills*.skillId
        List<String> expectedIds
        if (copyType == 'imported') {
            skillsService.bulkExportSkillsToCatalog(fixture.project.projectId, sourceSkillIds)
            skillsService.createProjectAndSubjectAndSkills(targetProject, targetSubject, grouped ? [targetGroup] : [])
            List<Map> skillsToImport = fixture.skills.collect { Map skill -> [projectId: skill.projectId, skillId: skill.skillId] }
            if (grouped) {
                skillsService.bulkImportSkillsIntoGroupFromCatalogAndFinalize(targetProject.projectId,
                        targetSubject.subjectId, targetGroup.skillId, skillsToImport)
            } else {
                skillsService.bulkImportSkillsFromCatalogAndFinalize(targetProject.projectId, targetSubject.subjectId, skillsToImport)
            }
            expectedIds = sourceSkillIds
        } else {
            skillsService.createSubject(targetSubject)
            if (grouped) {
                skillsService.createSkill(targetGroup)
            }
            skillsService.reuseSkills(targetProject.projectId, sourceSkillIds, targetSubject.subjectId, grouped ? targetGroup.skillId : null)
            expectedIds = sourceSkillIds.collect { String skillId -> SkillReuseIdUtil.addTag(skillId, 0) }
        }

        when:
        List<Map> descriptions = learner.getSubjectDescriptions(targetProject.projectId, targetSubject.subjectId)

        then:
        descriptions*.skillId.sort() == (expectedIds + (grouped ? [targetGroup.skillId] : [])).sort()
        expectedIds.eachWithIndex { String skillId, int index ->
            Map description = descriptions.find { Map item -> item.skillId == skillId }
            assertMediaSummaries(learner, description, index)
        }

        where:
        copyType   | grouped
        'reused'   | false
        'reused'   | true
        'imported' | false
        'imported' | true
    }

    def "single skill summaries retain both media configurations"() {
        given:
        Map fixture = createMediaFixture('subject', false)
        SkillsService learner = fixture.learner

        when:
        Map summary = learner.getSingleSkillSummaryForCurrentUser(fixture.project.projectId, fixture.skills.first().skillId)
        Map subjectScopedSummary = skillsService.getSingleSkillSummaryWithSubject(learner.userName,
                fixture.project.projectId, fixture.subject.subjectId, fixture.skills.first().skillId)

        then:
        summary.skillId == fixture.skills.first().skillId
        subjectScopedSummary.skillId == fixture.skills.first().skillId
        assertMediaSummaries(learner, summary, 0)
        assertMediaSummaries(learner, subjectScopedSummary, 0)
    }

    def "global badge descriptions do not duplicate a skill with both media configurations"() {
        given:
        Map fixture = createMediaFixture('global badge', false)
        SkillsService learner = fixture.learner

        when:
        List<Map> descriptions = learner.getBadgeDescriptions(fixture.project.projectId, fixture.badge.badgeId, true)

        then:
        // Global badge descriptions use a different query, which currently omits media attributes.
        descriptions*.skillId.sort() == fixture.skills*.skillId.sort()
        fixture.skills.each { Map skill ->
            assert descriptions.find { Map item -> item.skillId == skill.skillId }.description == skill.description
        }
    }

    private Map createMediaFixture(String endpoint, boolean grouped) {
        Map project = createProject(1)
        Map subject = createSubject(1, 1)
        List<Map> skills = createSkills(4, 1, 1, 100)
        Map group = createSkillsGroup(1, 1, 50)
        if (grouped) {
            skillsService.createProjectAndSubjectAndSkills(project, subject, [group])
            skills.each { Map skill -> skillsService.assignSkillToSkillsGroup(group.skillId, skill) }
        } else {
            skillsService.createProjectAndSubjectAndSkills(project, subject, skills)
        }

        Resource pdfSlides = new ClassPathResource('/testSlides/test-slides-1.pdf')
        [0, 1].each { int index ->
            skillsService.saveSkillVideoAttributes(project.projectId, skills[index].skillId,
                    [videoUrl: VIDEO_URL, captions: 'training captions', transcript: 'training transcript'])
        }
        [0, 2].each { int index ->
            skillsService.saveSlidesAttributes(project.projectId, skills[index].skillId, [file: pdfSlides, width: 111])
        }

        Map badge = createBadge(1, 1)
        if (endpoint == 'badge') {
            skillsService.createBadge(badge)
            skills.each { Map skill ->
                skillsService.assignSkillToBadge(projectId: project.projectId, badgeId: badge.badgeId, skillId: skill.skillId)
            }
            badge.enabled = 'true'
            skillsService.createBadge(badge)
        } else if (endpoint == 'global badge') {
            skillsService.createGlobalBadge(badge)
            skills.each { Map skill -> skillsService.assignSkillToGlobalBadge(project.projectId, badge.badgeId, skill.skillId) }
            badge.enabled = 'true'
            skillsService.updateGlobalBadge(badge)
        } else if (endpoint == 'tag') {
            skillsService.addTagToSkills(project.projectId, skills*.skillId, 'Training Media', TAG_ID)
        }

        SkillsService learner = createService(getRandomUsers(1).first())
        return [project: project, subject: subject, skills: skills, group: group, badge: badge, learner: learner]
    }

    private static Map loadSummary(SkillsService learner, Map fixture, String endpoint) {
        String projectId = fixture.project.projectId
        switch (endpoint) {
            case 'subject':
                return learner.getSkillSummary(null, projectId, fixture.subject.subjectId)
            case 'group':
                return learner.getSkillsGroupSummary(projectId, fixture.group.skillId)
            case 'badge':
                return learner.getBadgeSummary(null, projectId, fixture.badge.badgeId)
            case 'global badge':
                return learner.getBadgeSummary(null, projectId, fixture.badge.badgeId, -1, true)
            case 'tag':
                return learner.getSkillTagSummary(null, projectId, TAG_ID)
            case 'tags overview':
                List<Map> tags = learner.getSkillTagSummariesForProject(null, projectId)
                assert tags*.tagId == [TAG_ID]
                return tags.first()
            case 'badges overview':
            case 'my badges':
                List<Map> badges = endpoint == 'badges overview' ? learner.getBadgesSummary(null, projectId) : learner.getMyProgressBadges()
                assert badges*.badgeId == [fixture.badge.badgeId]
                return badges.first()
            default:
                throw new IllegalArgumentException("Unknown endpoint: ${endpoint}")
        }
    }

    private static List<Map> loadDescriptions(SkillsService learner, Map fixture, String endpoint) {
        String projectId = fixture.project.projectId
        switch (endpoint) {
            case 'subject':
                return learner.getSubjectDescriptions(projectId, fixture.subject.subjectId)
            case 'group':
                return learner.getGroupDescriptions(projectId, fixture.group.skillId)
            case 'badge':
                return learner.getBadgeDescriptions(projectId, fixture.badge.badgeId)
            case 'tag':
                return learner.getSkillTagDescriptions(projectId, TAG_ID)
            default:
                throw new IllegalArgumentException("Unknown endpoint: ${endpoint}")
        }
    }

    private static boolean assertMediaSummaries(SkillsService learner, Map skill, int index) {
        if (index in [0, 1]) {
            assert skill.videoSummary.videoUrl == VIDEO_URL
            assert skill.videoSummary.hasCaptions
            assert skill.videoSummary.hasTranscript
        } else {
            assert skill.videoSummary == null
        }
        if (index in [0, 2]) {
            assert skill.slidesSummary.type == 'application/pdf'
            assert skill.slidesSummary.width == 111.0
            Resource pdfSlides = new ClassPathResource('/testSlides/test-slides-1.pdf')
            assert learner.downloadAttachment(skill.slidesSummary.url).file.bytes == Files.readAllBytes(pdfSlides.getFile().toPath())
        } else {
            assert skill.slidesSummary == null
        }
        return true
    }
}
