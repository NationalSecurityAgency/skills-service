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

import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import skills.storage.model.ProjDef
import skills.storage.model.SkillsDBLock
import skills.storage.model.UserAttrs
import skills.storage.model.UserPoints
import skills.storage.repos.SkillsDBLockRepo

@Service
@CompileStatic
class LockingService {

    @Autowired
    SkillsDBLockRepo skillsDBLockRepo

    SkillsDBLock lockGlobalSettings() {
        return skillsDBLockRepo.findByLock("global_settings_lock")
    }

    SkillsDBLock lockProjects() {
        return skillsDBLockRepo.findByLock("projects_lock")
    }

    SkillsDBLock lockGlobalBadges() {
        return skillsDBLockRepo.findByLock("global_badges_lock")
    }

    SkillsDBLock lockEventCompaction() {
        return skillsDBLockRepo.findByLock("event_compaction_lock")
    }

    SkillsDBLock lockForNotifying() {
        return skillsDBLockRepo.findByLock("notifier_lock")
    }

    SkillsDBLock lockForUpdatingCatalogSkills() {
        return skillsDBLockRepo.findByLock('catalog_skill_update_lock')
    }

    ProjDef lockProject(String projectId) {
        assert projectId
        return skillsDBLockRepo.findByProjectIdIgnoreCase(projectId)
    }

    /**
     * PESSIMISTIC_WRITE Lock - returns user's db id
     */
    Integer lockUser(String userId) {
        assert userId
        return skillsDBLockRepo.findUserAttrsByUserId(userId?.toLowerCase())
    }

    SkillsDBLock lockForProjectExpiration() {
        return skillsDBLockRepo.findByLock("project_expiration_lock")
    }

    SkillsDBLock lockForCreateOrUpdateUser() {
        return skillsDBLockRepo.findByLock("create_or_update_user")
    }

}
