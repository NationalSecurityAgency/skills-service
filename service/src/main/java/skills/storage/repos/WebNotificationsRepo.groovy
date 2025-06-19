/**
 * Copyright 2025 SkillTree
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
package skills.storage.repos

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import skills.storage.model.WebNotification

import java.util.stream.Stream

interface WebNotificationsRepo extends JpaRepository<WebNotification, Integer> {

    @Query("""select n from WebNotification n 
        where (n.userId = :userId or n.userId is null)
            and (n.showUntil > CURRENT_TIMESTAMP or n.showUntil is null) 
            and n.id not in (select a.webNotificationsRefId from WebNotificationAck a where a.userId = :userId)""")
    List<WebNotification> findUsersNotifications(String userId, Pageable pageRequest)

    @Query("""select n from WebNotification n 
        where (n.userId = :userId or n.userId is null) 
            and (n.showUntil > CURRENT_TIMESTAMP or n.showUntil is null)
            and n.id not in (select a.webNotificationsRefId from WebNotificationAck a where a.userId = :userId)""")
    Stream<WebNotification> findAllUsersNotifications(String userId)

    List<WebNotification> findAllByLookupId(String lookupId)

}
