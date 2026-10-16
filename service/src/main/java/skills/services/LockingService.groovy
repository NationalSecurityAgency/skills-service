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
package skills.services

import callStack.profiler.Profile
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.storage.model.SkillsDBLock
import skills.storage.repos.SkillsDBLockRepo
import skills.storage.repos.nativeSql.PostgresQlNativeRepo

@Slf4j
@Service
@CompileStatic
class LockingService {

    @Autowired
    SkillsDBLockRepo skillsDBLockRepo

    @Autowired
    PostgresQlNativeRepo postgresQlNativeRepo

    SkillsDBLock lockGlobalSettings() {
        long started = logLockRequest('lockGlobalSettings', 'global_settings_lock')
        SkillsDBLock res = skillsDBLockRepo.findByLock("global_settings_lock")
        assert res
        logLockResult('lockGlobalSettings', 'global_settings_lock', started, res)
        return res
    }

    SkillsDBLock lockProjects() {
        long started = logLockRequest('lockProjects', 'projects_lock')
        SkillsDBLock res = skillsDBLockRepo.findByLock("projects_lock")
        assert res
        logLockResult('lockProjects', 'projects_lock', started, res)
        return res
    }

    SkillsDBLock lockQuizDefs() {
        long started = logLockRequest('lockQuizDefs', 'quizDefs_lock')
        SkillsDBLock res = skillsDBLockRepo.findByLock("quizDefs_lock")
        assert res
        logLockResult('lockQuizDefs', 'quizDefs_lock', started, res)
        return res
    }

    SkillsDBLock lockAdminGroupDefs() {
        long started = logLockRequest('lockAdminGroupDefs', 'adminGroupDefs_lock')
        SkillsDBLock res = skillsDBLockRepo.findByLock("adminGroupDefs_lock")
        assert res
        logLockResult('lockAdminGroupDefs', 'adminGroupDefs_lock', started, res)
        return res
    }

    SkillsDBLock lockGlobalBadges() {
        long started = logLockRequest('lockGlobalBadges', 'global_badges_lock')
        SkillsDBLock res = skillsDBLockRepo.findByLock("global_badges_lock")
        assert res
        logLockResult('lockGlobalBadges', 'global_badges_lock', started, res)
        return res
    }

    SkillsDBLock lockEventCompaction() {
        long started = logLockRequest('lockEventCompaction', 'event_compaction_lock')
        SkillsDBLock res = skillsDBLockRepo.findByLock("event_compaction_lock")
        assert res
        logLockResult('lockEventCompaction', 'event_compaction_lock', started, res)
        return res
    }

    SkillsDBLock lockForNotifying() {
        long started = logLockRequest('lockForNotifying', 'notifier_lock')
        SkillsDBLock res = skillsDBLockRepo.findByLock("notifier_lock")
        assert res
        logLockResult('lockForNotifying', 'notifier_lock', started, res)
        return res
    }

    SkillsDBLock lockForReplayingSkillEvents() {
        long started = logLockRequest('lockForReplayingSkillEvents', 'upgrade_replay_skill_events')
        SkillsDBLock res = skillsDBLockRepo.findByLock("upgrade_replay_skill_events")
        assert res
        logLockResult('lockForReplayingSkillEvents', 'upgrade_replay_skill_events', started, res)
        return res
    }

    SkillsDBLock lockForUpdatingCatalogSkills() {
        long started = logLockRequest('lockForUpdatingCatalogSkills', 'catalog_skill_update_lock')
        SkillsDBLock res = skillsDBLockRepo.findByLock('catalog_skill_update_lock')
        assert res
        logLockResult('lockForUpdatingCatalogSkills', 'catalog_skill_update_lock', started, res)
        return res
    }

    @Profile
    Integer lockProject(String projectId) {
        assert projectId
        long started = logLockRequest('lockProject', projectId)
        Integer result = skillsDBLockRepo.findByProjectIdIgnoreCase(projectId)
        logLockResult('lockProject', projectId, started, result)
        return result
    }

    @Profile
    Integer lockQuizDef(String quizId) {
        assert quizId
        long started = logLockRequest('lockQuizDef', quizId)
        Integer result = skillsDBLockRepo.findByQuizDefIdIgnoreCase(quizId)
        logLockResult('lockQuizDef', quizId, started, result)
        return result
    }

    /**
     * PESSIMISTIC_WRITE Lock - returns user's db id
     */
    Integer lockUser(String userId) {
        assert userId
        long started = logLockRequest('lockUser', userId)
        Integer result = skillsDBLockRepo.findUserAttrsByUserId(userId?.toLowerCase())
        logLockResult('lockUser', userId, started, result)
        return result
    }

    SkillsDBLock lockForUserCreateOrUpdate(String userId) {
        String key = "update_" + userId
        long started = logLockRequest('lockForUserCreateOrUpdate', key)
        SkillsDBLock lock = postgresQlNativeRepo.insertLockOrSelectExisting(key)
        logLockResult('lockForUserCreateOrUpdate', key, started, lock)
        return lock
    }

    SkillsDBLock lockForProjectExpiration() {
        long started = logLockRequest('lockForProjectExpiration', 'project_expiration_lock')
        SkillsDBLock res = skillsDBLockRepo.findByLock("project_expiration_lock")
        assert res
        logLockResult('lockForProjectExpiration', 'project_expiration_lock', started, res)
        return res
    }

    SkillsDBLock lockForSkillExpiration() {
        long started = logLockRequest('lockForSkillExpiration', 'skill_expiration_lock')
        SkillsDBLock res = skillsDBLockRepo.findByLock("skill_expiration_lock")
        assert res
        logLockResult('lockForSkillExpiration', 'skill_expiration_lock', started, res)
        return res
    }

    SkillsDBLock lockForCreateOrUpdateUser() {
        long started = logLockRequest('lockForCreateOrUpdateUser', 'create_or_update_user')
        SkillsDBLock res = skillsDBLockRepo.findByLock("create_or_update_user")
        assert res
        logLockResult('lockForCreateOrUpdateUser', 'create_or_update_user', started, res)
        return res
    }

    SkillsDBLock lockForSkillReporting(String userId, String projectId) {
        String key = "reportSkill_" + userId + projectId
        long started = logLockRequest('lockForSkillReporting', key)
        SkillsDBLock lock = postgresQlNativeRepo.insertLockOrSelectExisting(key)
        logLockResult('lockForSkillReporting', key, started, lock)
        return lock
    }

    SkillsDBLock lockCatalogActivation(String projectId) {
        String key = "catalogActivation_${projectId}".toString()
        long started = logLockRequest('lockCatalogActivation', key)
        SkillsDBLock lock = postgresQlNativeRepo.insertLockOrSelectExisting(key)
        logLockResult('lockCatalogActivation', key, started, lock)
        return lock
    }

    SkillsDBLock lockUserQuizAttempt(Integer quizAttemptId) {
        String key = "quizAttempt_" + quizAttemptId
        long started = logLockRequest('lockUserQuizAttempt', key)
        SkillsDBLock lock = postgresQlNativeRepo.insertLockOrSelectExisting(key)
        logLockResult('lockUserQuizAttempt', key, started, lock)
        return lock
    }

    @Transactional
    void deleteLocksOlderThan(Date date) {
        log.debug('Deleting expired lock records created before [{}]', date)
        skillsDBLockRepo.deleteByCreatedBeforeAndExpires(date)
        log.debug('Completed deletion of expired lock records created before [{}]', date)
    }

    private long logLockRequest(String operation, String key) {
        if (!log.isDebugEnabled()) {
            return 0L
        }
        log.debug('Requesting database lock: operation=[{}], key=[{}]', operation, key)
        return System.nanoTime()
    }

    // Acquisition completes here; the surrounding transaction controls lock release.
    private void logLockResult(String operation, String key, long started, Object result) {
        if (log.isDebugEnabled() && started != 0L) {
            double elapsedMs = (System.nanoTime() - started) / 1_000_000d
            if (result != null) {
                log.debug('Acquired database lock: operation=[{}], key=[{}], elapsedMs=[{}]', operation, key, elapsedMs)
            } else {
                log.debug('Database lock lookup found no matching row: operation=[{}], key=[{}], elapsedMs=[{}]', operation, key, elapsedMs)
            }
        }
    }
}
