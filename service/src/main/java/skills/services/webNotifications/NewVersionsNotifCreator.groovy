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
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.storage.model.WebNotification
import skills.storage.repos.WebNotificationsRepo

@Component
@Slf4j
class NewVersionsNotifCreator {

    @Autowired
    WebNotificationsRepo webNotificationsRepo

    private final List<WebNotification> newVersionsNotifications = [
            new WebNotification(
                    notifiedOn: new Date(),
                    lookupId: "new-versions-1",
                    title: "SkillTree 3.6 Version Released",
                    notification: """- Draft mode for subject/skill creation
- Audio/video in quizzes and surveys
- Context-aware contact system
- Keyboard shortcuts for training navigation
- Screen reader-friendly headings
- [Learn More](https://skilltreeplatform.dev/dashboard/user-guide/projects.html#invite-only)""",
            )
    ]


    @PostConstruct
    void createNewVersionsNotification() {
        newVersionsNotifications.each {
            List<WebNotification> existingNotifications = webNotificationsRepo.findAllByLookupId(it.lookupId)
            if (!existingNotifications) {
                log.info("Adding new version notification: {}", it)
                webNotificationsRepo.save(it)
            }
        }
    }

}
