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
import jakarta.transaction.Transactional
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import skills.storage.model.WebNotification
import skills.storage.repos.WebNotificationsRepo

@Component
@Slf4j
class NewVersionsNotifCreator {

    @Value('#{"${skills.config.ui.docsHost}"}')
    String docsRootHost

    @Value('${skills.config.ui.enableOpenAIIntegration:#{false}}')
    Boolean enableOpenAIIntegration

    @Autowired
    WebNotificationsRepo webNotificationsRepo

    private static class WebNotifWrapper {
        WebNotification notif
        Date releaseNoLaterThan
    }

    @PostConstruct
    @Transactional
    void createNewVersionsNotification() {
        assert docsRootHost // must have it configured
        List<WebNotifWrapper> newVersionsNotifications = [
                new WebNotifWrapper(
                        releaseNoLaterThan: Date.parse('yyyy-MM-dd', '2026-02-12'),
                        notif: new WebNotification(
                                notifiedOn: new Date(),
                                showUntil: Date.parse('yyyy-MM-dd', '2026-03-12'),
                                lookupId: "new-versions-5",
                                title: "Version 4.0 Released",
                                notification:
                                        enableOpenAIIntegration ? """
### ✨ AI-Powered Content Generation
- **AI Assistant Integration**: Streamline your content creation with our new AI Assistant, now available for generating high-quality skill descriptions and quiz questions
- **Smart Quiz Creation**: Automatically generate complete quizzes for skills based on their descriptions, complete with multiple-choice questions and answers
- [Learn More]({{docsRootHost}}/release-notes/skills-service.html)""" : """
- **Improved Learning Path Display**: Better handling of large skill sets with optimized rendering and navigation as well as full-screen display option
- **Improved Skill Navigation**: Quick access to adjacent skills with Previous and Next buttons in the Admin Skill Details page
- [Learn More]({{docsRootHost}}/release-notes/skills-service.html)""")
                ),
                new WebNotifWrapper(
                        releaseNoLaterThan: Date.parse('yyyy-MM-dd', '2025-10-12'),
                        notif: new WebNotification(
                                notifiedOn: new Date(),
                                showUntil: Date.parse('yyyy-MM-dd', '2025-11-01'),
                                lookupId: "new-versions-4",
                                title: "Version 3.9 Released",
                                notification: """- Support for PDF slide decks in quizzes and surveys
- Enhanced Metric Page with time range filters
- [Learn More]({{docsRootHost}}/release-notes/skills-service.html)""")
                ),
                new WebNotifWrapper(
                        releaseNoLaterThan: Date.parse('yyyy-MM-dd', '2025-09-12'),
                        notif: new WebNotification(
                                notifiedOn: new Date(),
                                showUntil: Date.parse('yyyy-MM-dd', '2025-10-01'),
                                lookupId: "new-versions-3",
                                title: "Version 3.8 Released",
                                notification: """- Upload PDF slide decks for skills to enhance training
- Create Global Badges with skills/levels from any admin-accessible project
- Customize skill icons, just like subjects and badges
- [Learn More]({{docsRootHost}}/release-notes/skills-service.html)""")
                ),
                new WebNotifWrapper(
                        releaseNoLaterThan: Date.parse('yyyy-MM-dd', '2025-07-12'),
                        notif: new WebNotification(
                                notifiedOn: new Date(),
                                showUntil: Date.parse('yyyy-MM-dd', '2025-09-01'),
                                lookupId: "new-versions-2",
                                title: "Version 3.7 Released",
                                notification: """- A training-wide search with a click or keyboard shortcut
- Ability to easily share non-catalog projects
- Unobtrusive alerts for updates and new features
- Filter quiz and survey results by completion date range
- [Learn More]({{docsRootHost}}/release-notes/skills-service.html)""")
                ),
                new WebNotifWrapper(
                        releaseNoLaterThan: Date.parse('yyyy-MM-dd', '2025-05-28'),
                        notif: new WebNotification(
                                notifiedOn: new Date(),
                                showUntil: Date.parse('yyyy-MM-dd', '2025-08-01'),
                                lookupId: "new-versions-1",
                                title: "Version 3.6 Released",
                                notification: """- Skill and Subject draft mode until fully configured
- Audio and video support for quiz and survey questions
- Context-aware contact process
- 1,000+ additional Font Awesome icons
- [Learn More]({{docsRootHost}}/release-notes/skills-service.html)""")
                )
        ]

        List<String> lookupIds = newVersionsNotifications.collect { it.notif.lookupId }
        boolean lookupIdsUnique = lookupIds.toSet().size() == lookupIds.size()
        if (!lookupIdsUnique) {
            throw new IllegalStateException("lookupIds must be unique: ${lookupIds}")
        }

        List<WebNotifWrapper> notificationsToAdd = newVersionsNotifications.findAll {
            !it.notif.showUntil || it.notif.showUntil > new Date()
        }
        notificationsToAdd.each {WebNotifWrapper wrap ->
            WebNotification webNotification = wrap.notif
            if (wrap.releaseNoLaterThan < wrap.notif.notifiedOn) {
                webNotification.notifiedOn = wrap.releaseNoLaterThan
            }
            webNotification.notification = webNotification.notification.replaceAll("\\{\\{docsRootHost\\}\\}", docsRootHost)
            List<WebNotification> existingNotifications = webNotificationsRepo.findAllByLookupId(webNotification.lookupId)
            if (!existingNotifications) {
                log.info("Adding new version notification: {}", webNotification)
                webNotificationsRepo.save(webNotification)
            }
        }
    }

}
