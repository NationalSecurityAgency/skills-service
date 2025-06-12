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

class DismissNotificationsSpecs extends DefaultIntSpec {

    @Autowired
    WebNotificationsRepo webNotificationsRepo

    def setup() {
        webNotificationsRepo.deleteAll()
    }

    def "dismiss a single notification"() {
        List<SkillsService> users = getRandomUsers(3).collect { createService(it) }

        List<WebNotification> notifications = [
                new WebNotification(
                        notifiedOn: new Date(),
                        showUntil: new Date() + 30,
                        lookupId: "global-1",
                        title: "1 - Global 1",
                        notification: "G1 - message",
                ),
                new WebNotification(
                        notifiedOn: new Date()-1,
                        showUntil: new Date() + 30,
                        lookupId: "global-2",
                        title: "2 - Global 2",
                        notification: "G2 - message",
                ),
                new WebNotification(
                        notifiedOn: new Date()-2,
                        showUntil: new Date() + 30,
                        lookupId: "user1-1",
                        title: "3 - User 1 - 1",
                        notification: "U1-1 - message",
                        userId: users[0].userName
                ),
                new WebNotification(
                        notifiedOn: new Date()-3,
                        showUntil: new Date() + 30,
                        lookupId: "user1-2",
                        title: "4 - User 1 - 2",
                        notification: "U1-2 - message",
                        userId: users[0].userName
                ),
                new WebNotification(
                        notifiedOn: new Date()-4,
                        showUntil: new Date() + 30,
                        lookupId: "user3-1",
                        title: "5 - User 3 - 1",
                        notification: "U3-1 - message",
                        userId: users[2].userName
                ),
        ]
        webNotificationsRepo.saveAll(notifications)
        when:
        def u1Notifs_t0 = users[0].getWebNotifications()
        def u2Notifs_t0 = users[1].getWebNotifications()
        def u3Notifs_t0 = users[2].getWebNotifications()

        users[0].dismissWebNotification(u1Notifs_t0[1].id)

        def u1Notifs_t1 = users[0].getWebNotifications()
        def u2Notifs_t1 = users[1].getWebNotifications()
        def u3Notifs_t1 = users[2].getWebNotifications()

        then:
        u1Notifs_t0.title == notifications[0..3].title
        u2Notifs_t0.title == notifications[0..1].title
        u3Notifs_t0.title == [ notifications[0..1].title, notifications[4].title].flatten()

        // after dismissal
        u1Notifs_t1.title == [ notifications[0].title, notifications[2].title, notifications[3].title]
        u2Notifs_t1.title == notifications[0..1].title
        u3Notifs_t1.title == [ notifications[0..1].title, notifications[4].title].flatten()
    }

    def "dismiss all notification"() {
        List<SkillsService> users = getRandomUsers(3).collect { createService(it) }

        List<WebNotification> notifications = [
                new WebNotification(
                        notifiedOn: new Date(),
                        showUntil: new Date() + 30,
                        lookupId: "global-1",
                        title: "1 - Global 1",
                        notification: "G1 - message",
                ),
                new WebNotification(
                        notifiedOn: new Date()-1,
                        showUntil: new Date() + 30,
                        lookupId: "global-2",
                        title: "2 - Global 2",
                        notification: "G2 - message",
                ),
                new WebNotification(
                        notifiedOn: new Date()-2,
                        showUntil: new Date() + 30,
                        lookupId: "user1-1",
                        title: "3 - User 1 - 1",
                        notification: "U1-1 - message",
                        userId: users[0].userName
                ),
                new WebNotification(
                        notifiedOn: new Date()-3,
                        showUntil: new Date() + 30,
                        lookupId: "user1-2",
                        title: "4 - User 1 - 2",
                        notification: "U1-2 - message",
                        userId: users[0].userName
                ),
                new WebNotification(
                        notifiedOn: new Date()-4,
                        showUntil: new Date() + 30,
                        lookupId: "user3-1",
                        title: "5 - User 3 - 1",
                        notification: "U3-1 - message",
                        userId: users[2].userName
                ),
        ]
        webNotificationsRepo.saveAll(notifications)
        when:
        def u1Notifs_t0 = users[0].getWebNotifications()
        def u2Notifs_t0 = users[1].getWebNotifications()
        def u3Notifs_t0 = users[2].getWebNotifications()

        users[0].dismissAllWebNotifications()

        def u1Notifs_t1 = users[0].getWebNotifications()
        def u2Notifs_t1 = users[1].getWebNotifications()
        def u3Notifs_t1 = users[2].getWebNotifications()

        then:
        u1Notifs_t0.title == notifications[0..3].title
        u2Notifs_t0.title == notifications[0..1].title
        u3Notifs_t0.title == [ notifications[0..1].title, notifications[4].title].flatten()

        // after dismissal
        !u1Notifs_t1
        u2Notifs_t1.title == notifications[0..1].title
        u3Notifs_t1.title == [ notifications[0..1].title, notifications[4].title].flatten()
    }

    def "dismiss all notification even if they exceed the default page size"() {
        List<SkillsService> users = getRandomUsers(3).collect { createService(it) }

        List<WebNotification> notifications = (0..30).collect {
            new WebNotification(
                    notifiedOn: new Date()-(10+it),
                    showUntil: new Date()+10,
                    lookupId: "global-${it}",
                    title: "${StringUtils.leftPad(it.toString(), 2, '0')} - Global 1",
                    notification: "G${it} - message",
            )
        }
        notifications.addAll([
                new WebNotification(
                        notifiedOn: new Date(),
                        showUntil: new Date()+2,
                        lookupId: "user1-1",
                        title: "3 - User 1 - 1",
                        notification: "U1-1 - message",
                        userId: users[0].userName
                ),
                new WebNotification(
                        notifiedOn: new Date()-1,
                        showUntil: new Date() + 30,
                        lookupId: "user1-2",
                        title: "4 - User 1 - 2",
                        notification: "U1-2 - message",
                        userId: users[0].userName
                ),
                new WebNotification(
                        notifiedOn: new Date(),
                        showUntil: new Date()+10,
                        lookupId: "user3-1",
                        title: "5 - User 3 - 1",
                        notification: "U3-1 - message",
                        userId: users[2].userName
                ),
        ])
        webNotificationsRepo.saveAll(notifications)
        when:
        def u1Notifs = users[0].getWebNotifications()
        def u2Notifs = users[1].getWebNotifications()
        def u3Notifs = users[2].getWebNotifications()

        users[0].dismissAllWebNotifications()

        def u1Notifs_t1 = users[0].getWebNotifications()
        def u2Notifs_t1 = users[1].getWebNotifications()
        def u3Notifs_t1 = users[2].getWebNotifications()

        then:
        u1Notifs.size() == 20
        u1Notifs.title == [ notifications[31].title, notifications[32].title, notifications[0..17].title].flatten()
        u1Notifs.notification == [ notifications[31].notification, notifications[32].notification, notifications[0..17].notification].flatten()

        u2Notifs.size() == 20
        u2Notifs.title == notifications[0..19].title
        u2Notifs.notification == notifications[0..19].notification

        u3Notifs.size() == 20
        u3Notifs.title == [ notifications[33].title, notifications[0..18].title].flatten()
        u3Notifs.notification == [ notifications[33].notification, notifications[0..18].notification].flatten()

        !u1Notifs_t1

        u2Notifs_t1.size() == 20
        u2Notifs_t1.title == notifications[0..19].title
        u2Notifs_t1.notification == notifications[0..19].notification

        u3Notifs_t1.size() == 20
        u3Notifs_t1.title == [ notifications[33].title, notifications[0..18].title].flatten()
        u3Notifs_t1.notification == [ notifications[33].notification, notifications[0..18].notification].flatten()
    }

    def "not allowed to dismiss others notifications"() {
        List<SkillsService> users = getRandomUsers(3).collect { createService(it) }

        List<WebNotification> notifications = [
                new WebNotification(
                        notifiedOn: new Date(),
                        showUntil: new Date() + 30,
                        lookupId: "global-1",
                        title: "1 - Global 1",
                        notification: "G1 - message",
                ),
                new WebNotification(
                        notifiedOn: new Date()-1,
                        showUntil: new Date() + 30,
                        lookupId: "global-2",
                        title: "2 - Global 2",
                        notification: "G2 - message",
                ),
                new WebNotification(
                        notifiedOn: new Date()-2,
                        showUntil: new Date() + 30,
                        lookupId: "user1-1",
                        title: "3 - User 1 - 1",
                        notification: "U1-1 - message",
                        userId: users[0].userName
                ),
                new WebNotification(
                        notifiedOn: new Date()-3,
                        showUntil: new Date() + 30,
                        lookupId: "user1-2",
                        title: "4 - User 1 - 2",
                        notification: "U1-2 - message",
                        userId: users[0].userName
                ),
                new WebNotification(
                        notifiedOn: new Date()-4,
                        showUntil: new Date() + 30,
                        lookupId: "user3-1",
                        title: "5 - User 3 - 1",
                        notification: "U3-1 - message",
                        userId: users[2].userName
                ),
        ]
        webNotificationsRepo.saveAll(notifications)
        when:
        def u3Notifs = users[2].getWebNotifications()
        users[0].dismissWebNotification(u3Notifs[2].id)

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.BAD_REQUEST
        e.message.contains("User [${users[0].userName}] is not allowed to dismiss notification [${u3Notifs[2].id}]")
    }

    def "can't dismiss what doesn't exist"() {
        List<SkillsService> users = getRandomUsers(3).collect { createService(it) }

        when:
        users[0].dismissWebNotification(1)

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.BAD_REQUEST
        e.message.contains("Failed to find web notification with id [1]")
    }

}


