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
package skills.intTests.webNotifications


import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.storage.model.WebNotification
import skills.storage.repos.WebNotificationsRepo

class CreateNotificationsAsRootSpecs extends DefaultIntSpec {

    @Autowired
    WebNotificationsRepo webNotificationsRepo

    String datePattern = "yyyy-MM-dd'T'HH:mm:ss"
    Closure formatDate = { Date theDate -> theDate.format(datePattern) }
    Closure parseDate = { String theDate -> Date.parse(datePattern, theDate) }

    def setup() {
        webNotificationsRepo.deleteAll()
    }

    def "create notifications"() {
        List<SkillsService> users = getRandomUsers(3).collect { createService(it) }

        List notifications = [
                [
                        notifiedOn: new Date(),
                        showUntil: new Date() + 30,
                        title: "1 - Global 1",
                        notification: "G1 - message",
                ],
               [
                        notifiedOn: new Date()-1,
                        showUntil: new Date() + 30,
                        title: "2 - Global 2",
                        notification: "G2 - message",
                ],
               [
                        notifiedOn: new Date()-2,
                        showUntil: new Date() + 30,
                        title: "3 - User 1 - 1",
                        notification: "U1-1 - message",
                        userId: users[0].userName
                ],
               [
                        notifiedOn: new Date()-3,
                        showUntil: new Date() + 30,
                        title: "4 - User 1 - 2",
                        notification: "U1-2 - message",
                        userId: users[0].userName
                ],
               [
                        notifiedOn: new Date()-4,
                        showUntil: new Date() + 30,
                        title: "5 - User 3 - 1",
                        notification: "U3-1 - message",
                        userId: users[2].userName
                ],
        ]
        def rootUser = createRootSkillService()

        when:

        notifications.each {
            rootUser.createWebNotificationAsRoot(it)
        }

        def u1Notifs = users[0].getWebNotifications()
        def u2Notifs = users[1].getWebNotifications()
        def u3Notifs = users[2].getWebNotifications()

        then:
        u1Notifs.title == notifications[0..3].title
        u1Notifs.notification == notifications[0..3].notification
        u1Notifs.notifiedOn.collect { formatDate(parseDate(it)) } == notifications[0..3].notifiedOn.collect { formatDate(it) }

        // just global notifications
        u2Notifs.title == notifications[0..1].title
        u2Notifs.notification == notifications[0..1].notification
        u2Notifs.notifiedOn.collect { formatDate(parseDate(it)) } == notifications[0..1].notifiedOn.collect { formatDate(it) }

        u3Notifs.title == [ notifications[0..1].title, notifications[4].title].flatten()
        u3Notifs.notification == [ notifications[0..1].notification, notifications[4].notification].flatten()
        u3Notifs.notifiedOn.collect { formatDate(parseDate(it)) }  == [ notifications[0].notifiedOn, notifications[1].notifiedOn, notifications[4].notifiedOn].collect { formatDate(it) }
    }

    def "only root role can create notifications"() {
        List<SkillsService> users = getRandomUsers(3).collect { createService(it) }

        when:
        users[0].createWebNotificationAsRoot([
                notifiedOn: new Date(),
                showUntil: new Date() + 30,
                title: "1 - Global 1",
                notification: "G1 - message",
        ])

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.FORBIDDEN
    }

}


