/**
 * Copyright 2021 SkillTree
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
import org.springframework.transaction.annotation.Transactional
import skills.intTests.utils.DefaultIntSpec
import skills.storage.model.SkillsDBLock

class LockingServiceSpec extends DefaultIntSpec {

    @Autowired
    LockingService lockingService

    @Transactional
    def "lock global settings"() {
        when:
        SkillsDBLock lock = lockingService.lockGlobalSettings()

        then:
        lock
    }

    @Transactional
    def "lock projects"() {
        when:
        SkillsDBLock lock = lockingService.lockProjects()

        then:
        lock
    }

    @Transactional
    def "lock global badges"() {
        when:
        SkillsDBLock lock = lockingService.lockGlobalBadges()

        then:
        lock
    }

    @Transactional
    def "lock event compaction"() {
        when:
        SkillsDBLock lock = lockingService.lockEventCompaction()

        then:
        lock
    }

}
