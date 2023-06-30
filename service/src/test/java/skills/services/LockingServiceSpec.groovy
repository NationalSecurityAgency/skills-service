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
import skills.intTests.utils.DefaultIntSpec
import skills.storage.model.SkillsDBLock

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class LockingServiceSpec extends DefaultIntSpec {

    @Autowired
    LockingService lockingService

    def "lock global settings"() {
        when:
        SkillsDBLock lock = runInTransaction { lockingService.lockGlobalSettings() }

        then:
        lock
    }

    def "lock projects"() {
        when:
        SkillsDBLock lock = runInTransaction { lockingService.lockProjects() }

        then:
        lock
    }

    def "lock global badges"() {
        when:
        SkillsDBLock lock = runInTransaction { lockingService.lockGlobalBadges() }

        then:
        lock
    }

    def "lock event compaction"() {
        when:
        SkillsDBLock lock = runInTransaction { lockingService.lockEventCompaction() }

        then:
        lock
    }

    def "lock project expiration"() {
        when:
        SkillsDBLock lock = runInTransaction { lockingService.lockForProjectExpiration() }

        then:
        lock
    }

    def "lock for skill reporting"() {
        when:
        SkillsDBLock lock = runInTransaction { lockingService.lockForSkillReporting("user", "project") }

        then:
        lock.lock == "reportSkill_userproject"
    }

    def "test concurrent locks for user project"() {
        ExecutorService service = Executors.newFixedThreadPool(2)
        AtomicLong t1Start = new AtomicLong()
        AtomicLong t2Start = new AtomicLong()

        long sleepTime = 2500

        Runnable longRunning = new Runnable() {
            @Override
            void run() {
                runInTransaction {
                    SkillsDBLock lock = lockingService.lockForSkillReporting("aUser", "aProject")
                    t1Start.set(System.currentTimeMillis())
                    Thread.currentThread().sleep(sleepTime)
                    return true;
                }
            }
        }

        Runnable immediate = new Runnable() {
            @Override
            void run() {
                boolean result =runInTransaction {
                    try {
                        SkillsDBLock lock = lockingService.lockForSkillReporting("aUser", "aProject")
                        t2Start.set(System.currentTimeMillis())
                        return true
                    } catch (e) {
                        e.printStackTrace()
                    }
                }
                assert result
            }
        }

        when:

        service.submit(longRunning)
        Thread.currentThread().sleep(50)
        service.submit(immediate)
        service.shutdown()
        service.awaitTermination(sleepTime*4, TimeUnit.MILLISECONDS)

        then:
        service.isTerminated()
        t2Start.get() > t1Start.get()+sleepTime
    }


}
