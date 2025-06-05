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
package skills.controller

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import skills.controller.result.model.RequestResult
import skills.profile.EnableCallStackProf
import skills.services.webNotifications.WebNotificationRes
import skills.services.webNotifications.WebNotificationsService

@RestController
@RequestMapping("/api")
@Slf4j
@EnableCallStackProf
class WebNotificationsController {

    @Autowired
    WebNotificationsService webNotificationsService

    @GetMapping('/webNotifications')
    List<WebNotificationRes> getWebNotificationsForCurrentUser() {
        return webNotificationsService.getWebNotificationsForCurrentUser()
    }

    @PostMapping('/webNotifications/{notificationId}/dismiss')
    RequestResult getWebNotificationsForCurrentUser(@PathVariable("notificationId") Integer notificationId) {
        webNotificationsService.dismissNotification(notificationId)
        return RequestResult.success()
    }

    @PostMapping('/webNotifications/dismissAll')
    RequestResult dismissAllWebNotifications() {
        webNotificationsService.dismissAllNotificationsForCurrentUser()
        return RequestResult.success()
    }
}
