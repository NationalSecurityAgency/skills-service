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
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionTemplate
import skills.intTests.utils.DefaultIntSpec
import skills.storage.model.SkillsDBLock

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class LockingServiceSpec extends DefaultIntSpec {

    @Autowired
    LockingService lockingService

    @Autowired
    private PlatformTransactionManager transactionManager;

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

    @Transactional
    def "lock project expiration"() {
        when:
        SkillsDBLock lock = lockingService.lockForProjectExpiration()

        then:
        lock
    }

    @Transactional
    def "lock for user project"() {
        when:
        SkillsDBLock lock = lockingService.lockForImportedSkillPropagation("user", "project")

        then:
        lock
    }

    @Transactional
    def "lock for skill reporting"() {
        when:
        SkillsDBLock lock = lockingService.lockForSkillReporting("user", "project")

        then:
        lock
    }

    def "test concurrent locks for user project"() {
        ExecutorService service = Executors.newFixedThreadPool(2)
        AtomicLong t1Start = new AtomicLong()
        AtomicLong t2Start = new AtomicLong()

        long sleepTime = 2500

        Runnable longRunning = new Runnable() {
            @Override
            void run() {
                TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager)
                transactionTemplate.setPropagationBehaviorName("PROPAGATION_REQUIRES_NEW")
                transactionTemplate.execute(new TransactionCallback<Boolean>() {
                    @Override
                    Boolean doInTransaction(TransactionStatus status) {
                        SkillsDBLock lock = lockingService.lockForImportedSkillPropagation("aUser", "aProject")
                        t1Start.set(System.currentTimeMillis())
                        Thread.currentThread().sleep(sleepTime)
                        return true;
                    }
                })
            }
        }

        Runnable immediate = new Runnable() {
            @Override
            void run() {
                TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager)
                transactionTemplate.setPropagationBehaviorName("PROPAGATION_REQUIRES_NEW")
                boolean result = transactionTemplate.execute(new TransactionCallback<Boolean>() {
                    @Override
                    Boolean doInTransaction(TransactionStatus status) {
                            try {
                                SkillsDBLock lock = lockingService.lockForImportedSkillPropagation("aUser", "aProject")
                                t2Start.set(System.currentTimeMillis())
                                return true
                            } catch (e) {
                                e.printStackTrace()
                            }
                    }
                })
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
