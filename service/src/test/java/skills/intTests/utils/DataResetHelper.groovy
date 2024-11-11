/**
 * Copyright 2024 SkillTree
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
package skills.intTests.utils

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import skills.services.LevelDefinitionStorageService
import skills.services.LockingService
import skills.storage.repos.AdminGroupDefRepo
import skills.storage.repos.ClientPrefRepo
import skills.storage.repos.NotificationsRepo
import skills.storage.repos.ProjDefRepo
import skills.storage.repos.QuizDefRepo
import skills.storage.repos.SettingRepo
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.UserAchievedLevelRepo
import skills.storage.repos.UserActionsHistoryRepo
import skills.storage.repos.UserAttrsRepo
import skills.storage.repos.UserEventsRepo
import skills.storage.repos.UserPerformedSkillRepo
import skills.storage.repos.UserPointsRepo
import skills.utils.RetryUtil

@Component
class DataResetHelper {

    @Autowired
    ProjDefRepo projDefRepo

    @Autowired
    UserAttrsRepo userAttrsRepo

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    SettingRepo settingRepo

    @Autowired
    NotificationsRepo notificationsRepo

    @Autowired
    QuizDefRepo quizDefRepo

    @PersistenceContext
    EntityManager entityManager

    @Autowired
    TransactionHelper transactionHelper

    @Autowired
    UserActionsHistoryRepo userActionsHistoryRepo

    @Autowired
    AdminGroupDefRepo adminGroupDefRepo

    @Autowired
    WaitForAsyncTasksCompletion waitForAsyncTasksCompletion

    void resetData() {
        /**
         * deleting projects and users will wipe the entire db clean due to cascading
         */
        projDefRepo.deleteAll()
        quizDefRepo.deleteAll()
        adminGroupDefRepo.deleteAll()
        userAttrsRepo.deleteAll()
        // global badges don't have references to a project so must delete those manually
        skillDefRepo.deleteAll()
        // notificationsRepo no longer has a fk to users so must delete explicitly
        RetryUtil.withRetry(3, {
            notificationsRepo.deleteAll()
        })

        settingRepo.findAll().each {
            if (!it.settingGroup?.startsWith("public_")) {
                settingRepo.delete(it)
            }
        }

        deleteAllAttachments()

        userActionsHistoryRepo.deleteAll()

        waitForAsyncTasksCompletion.clearScheduledTaskTable()
    }

    int deleteAllAttachments() {
        return transactionHelper.doInTransaction {
            entityManager.createQuery("DELETE from Attachment").executeUpdate()
        }
    }
}
