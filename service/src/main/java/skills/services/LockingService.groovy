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
        SkillsDBLock res = skillsDBLockRepo.findByLock("global_settings_lock")
        assert res
        return res
    }

    SkillsDBLock lockProjects() {
        SkillsDBLock res = skillsDBLockRepo.findByLock("projects_lock")
        assert res
        return res
    }

    SkillsDBLock lockQuizDefs() {
        SkillsDBLock res = skillsDBLockRepo.findByLock("quizDefs_lock")
        assert res
        return res
    }

    SkillsDBLock lockAdminGroupDefs() {
        SkillsDBLock res = skillsDBLockRepo.findByLock("adminGroupDefs_lock")
        assert res
        return res
    }

    SkillsDBLock lockGlobalBadges() {
        SkillsDBLock res = skillsDBLockRepo.findByLock("global_badges_lock")
        assert res
        return res
    }

    SkillsDBLock lockEventCompaction() {
        SkillsDBLock res = skillsDBLockRepo.findByLock("event_compaction_lock")
        assert res
        return res
    }

    SkillsDBLock lockForNotifying() {
        SkillsDBLock res = skillsDBLockRepo.findByLock("notifier_lock")
        assert res
        return res
    }

    SkillsDBLock lockForReplayingSkillEvents() {
        SkillsDBLock res = skillsDBLockRepo.findByLock("upgrade_replay_skill_events")
        assert res
        return res
    }

    SkillsDBLock lockForUpdatingCatalogSkills() {
        SkillsDBLock res = skillsDBLockRepo.findByLock('catalog_skill_update_lock')
        assert res
        return res
    }

    @Profile
    Integer lockProject(String projectId) {
        assert projectId
        return skillsDBLockRepo.findByProjectIdIgnoreCase(projectId)
    }

    @Profile
    Integer lockQuizDef(String quizId) {
        assert quizId
        return skillsDBLockRepo.findByQuizDefIdIgnoreCase(quizId)
    }

    /**
     * PESSIMISTIC_WRITE Lock - returns user's db id
     */
    Integer lockUser(String userId) {
        assert userId
        return skillsDBLockRepo.findUserAttrsByUserId(userId?.toLowerCase())
    }

    SkillsDBLock lockForUserCreateOrUpdate(String userId) {
        String key = "update_" + userId
        SkillsDBLock lock = postgresQlNativeRepo.insertLockOrSelectExisting(key)
        return lock
    }

    SkillsDBLock lockForProjectExpiration() {
        SkillsDBLock res = skillsDBLockRepo.findByLock("project_expiration_lock")
        assert res
        return res
    }

    SkillsDBLock lockForCreateOrUpdateUser() {
        SkillsDBLock res = skillsDBLockRepo.findByLock("create_or_update_user")
        assert res
        return res
    }

    SkillsDBLock lockForSkillReporting(String userId, String projectId) {
        String key = "reportSkill_" + userId + projectId
        SkillsDBLock lock = postgresQlNativeRepo.insertLockOrSelectExisting(key)
        return lock
    }

    SkillsDBLock lockUserQuizAttempt(Integer quizAttemptId) {
        String key = "quizAttempt_" + quizAttemptId
        SkillsDBLock lock = postgresQlNativeRepo.insertLockOrSelectExisting(key)
        return lock
    }

    @Transactional
    void deleteLocksOlderThan(Date date) {
        skillsDBLockRepo.deleteByCreatedBeforeAndExpires(date)
    }
}
