/**
 * Copyright 2026 SkillTree
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
package skills.intTests.userActions

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import skills.auth.adminAttribution.AdminAttributionLoggingFilter
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService

class AdminAttributionDisabledSpec extends DefaultIntSpec {
    def 'disabled attribution ignores a header from a non-allowlisted requester and produces no new logs'() {
        given:
        SkillsService requester = createService(getRandomUsers(1).first())
        Map project = SkillsFactory.createProject()
        requester.createProject(project)
        HttpHeaders headers = new HttpHeaders()
        headers.set('X-SkillTree-Acting-User', 'unknown-attribution-user')
        Logger logger = LoggerFactory.getLogger(AdminAttributionLoggingFilter) as Logger
        ListAppender<ILoggingEvent> appender = new ListAppender<ILoggingEvent>()
        appender.start()
        logger.addAppender(appender)

        when:
        ResponseEntity<Map> response = requester.requestWithHeaders("/admin/projects/${project.projectId}",
                HttpMethod.POST, project + [name: 'Ordinary edit'], headers)
        Map edit = requester.getUserActionsForProject(project.projectId).data.find { Map row -> row.action == 'Edit' }

        then:
        response.statusCode == HttpStatus.OK
        requester.getProject(project.projectId).name == 'Ordinary edit'
        edit.userId == requester.userName.toLowerCase(Locale.ROOT)
        !requester.getProjectUserActionAttributes(project.projectId, edit.id as Long).containsKey('requestingSystemUserId')
        appender.list.empty

        cleanup:
        logger.detachAppender(appender)
        appender.stop()
    }
}
