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
package skills.services.webNotifications

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.auth.UserInfoService
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.storage.model.WebNotification
import skills.storage.model.WebNotificationAck
import skills.storage.repos.WebNotificationsAckRepo
import skills.storage.repos.WebNotificationsRepo

import java.util.stream.Stream

@Service
@Slf4j
class WebNotificationsService {

    @Autowired
    UserInfoService userInfoService

    @Autowired
    WebNotificationsRepo webNotificationsRepo

    @Autowired
    WebNotificationsAckRepo webNotificationsAckRepo

    @Value('#{"${skills.config.webNotifications.maxNumToReturn:20}"}')
    private int maxNumWebNotificationsToReturn

    @Transactional(readOnly = true)
    List<WebNotificationRes> getWebNotificationsForCurrentUser() {
        String currentUser = userInfoService.currentUserId
        PageRequest latestPlease = PageRequest.of(0, maxNumWebNotificationsToReturn, Sort.by(Sort.Direction.DESC, "notifiedOn"))
        List<WebNotification> webNotifications = webNotificationsRepo.findUsersNotifications(currentUser, latestPlease)
        return webNotifications.collect { convert(it) }
    }

    private static WebNotificationRes convert(WebNotification webNotification) {
        return new WebNotificationRes(
                id: webNotification.id,
                notifiedOn: webNotification.notifiedOn,
                title: webNotification.title,
                notification: webNotification.notification
        )
    }

    @Transactional
    void dismissNotification(Integer notificationId) {
        Optional<WebNotification> webNotificationOptional = webNotificationsRepo.findById(notificationId)
        if (webNotificationOptional.isEmpty()) {
            throw new SkillException("Failed to find web notification with id [${notificationId}]", ErrorCode.BadParam)
        }
        WebNotification webNotification = webNotificationOptional.get()
        String currentUser = userInfoService.currentUserId
        if (webNotification.userId) {
            if (webNotification.userId != currentUser) {
                throw new SkillException("User [${currentUser}] is not allowed to dismiss notification [${notificationId}]", ErrorCode.BadParam)
            }
        }

        List<WebNotificationAck> existing = webNotificationsAckRepo.findAllByUserIdAndWebNotificationsRefId(currentUser, notificationId)
        if (!existing) {
            WebNotificationAck webNotificationAck = new WebNotificationAck(userId: currentUser, webNotificationsRefId: notificationId)
            webNotificationsAckRepo.save(webNotificationAck)
        }
    }

    @Transactional
    void dismissAllNotificationsForCurrentUser() {
        String currentUser = userInfoService.currentUserId
        webNotificationsRepo.findAllUsersNotifications(currentUser).withCloseable { Stream<WebNotification> notifications ->
            notifications.each { webNotification ->
                webNotificationsAckRepo.save(new WebNotificationAck(userId: currentUser, webNotificationsRefId: webNotification.id))
            }
        }
    }
}
