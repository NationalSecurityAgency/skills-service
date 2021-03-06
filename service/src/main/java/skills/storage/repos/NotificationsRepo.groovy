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
package skills.storage.repos

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import skills.storage.model.Notification

import java.util.stream.Stream

interface NotificationsRepo extends JpaRepository<Notification, Integer> {

    @Query('''select n from 
            Notification n, User u 
                left join Setting s on (s.userRefId = u.id and s.settingGroup = 'user.prefs' and s.setting='email_pref' and s.type='User') 
            where 
                n.failedCount = 0 and 
                n.userId = u.userId and 
                (s.id is null or s.value = 'immediate')
        ''')
    Stream<Notification> streamNewNotifications();

    @Query('''select n from 
            Notification n, User u, Setting s 
            where 
                s.userRefId = u.id and 
                s.settingGroup = 'user.prefs' and 
                s.setting='email_pref' and 
                s.type='User' and
                n.failedCount = 0 and 
                n.userId = u.userId and 
                s.value = 'dailyDigest'
            order by u.userId
        ''')
    Stream<Notification> streamDigestNotifications();

    @Query("select n from Notification n where n.failedCount > 0")
    Stream<Notification> streamFailedNotifications();
}
