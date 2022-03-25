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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.annotation.Transactional
import skills.intTests.utils.DefaultIntSpec
import skills.storage.model.SkillsDBLock
import skills.storage.repos.SkillsDBLockRepo
import skills.storage.repos.nativeSql.NativeQueriesRepo

class ScheduledDbLockCleanupSpec extends DefaultIntSpec {

    @Autowired
    ScheduledDbLockCleanup scheduledDbLockCleanup

    @Autowired
    LockingService lockingService

    @Autowired
    SkillsDBLockRepo skillsDBLockRepo

    @Autowired
    JdbcTemplate jdbcTemplate

    @Autowired
    ScheduledUserTokenCleanup cleanup

    def "test ScheduledDbLockCleanup"() {
        SkillsDBLock lock = createExpiringLock()
        assert skillsDBLockRepo.findById(lock.id).isPresent()

        when:
        scheduledDbLockCleanup.cleanupExpiredLocks()

        then:
        !skillsDBLockRepo.findById(lock.id).isPresent()
    }

    @Transactional
    SkillsDBLock createExpiringLock() {
        SkillsDBLock lock =  lockingService.lockForUserProject("user", "project")
        jdbcTemplate.execute("update skills_db_locks set created = '${(new Date()-15).format("yyyy-MM-dd")}'")
        return lock
    }
}
