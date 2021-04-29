/**
 * Copyright 2021 SkillTree
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

import ch.qos.logback.classic.spi.ILoggingEvent
import skills.auth.UserInfoService
import skills.controller.request.model.SkillEventRequest
import skills.services.events.SkillEventsService
import skills.utils.LoggerHelper
import skills.utils.WaitFor
import spock.lang.Specification

class UserSkillsControllerSpec extends Specification {

    def "isRetry is logged properly when true"() {

        setup:
        LoggerHelper loggerHelper = new LoggerHelper(UserSkillsController.class)
        SkillEventsService skillsManagementFacade = Mock()
        UserInfoService userInfoService = Mock()
        userInfoService.getUserName(_, _) >> 'user1'
        UserSkillsController userSkillsController = new UserSkillsController(userInfoService: userInfoService, skillsManagementFacade: skillsManagementFacade)

        when:
        userSkillsController.addSkill('project1', 'skill1', new SkillEventRequest(
                userId: 'user1',
                timestamp: 1234l,
                notifyIfSkillNotApplied: false,
                isRetry: true,
        ))
        WaitFor.wait { loggerHelper.hasLogMsgStartsWith("ReportSkill (ProjectId=") }
        List<ILoggingEvent> logsList = loggerHelper.logEvents;

        then:
        logsList.find { it.formattedMessage.contains("IsRetry=[true]") }

        cleanup:
        loggerHelper.stop()
    }

    def "isRetry is logged properly when false"() {

        setup:
        LoggerHelper loggerHelper = new LoggerHelper(UserSkillsController.class)
        SkillEventsService skillsManagementFacade = Mock()
        UserInfoService userInfoService = Mock()
        userInfoService.getUserName(_, _) >> 'user1'
        UserSkillsController userSkillsController = new UserSkillsController(userInfoService: userInfoService, skillsManagementFacade: skillsManagementFacade)

        when:
        userSkillsController.addSkill('project1', 'skill1', new SkillEventRequest(
                userId: 'user1',
                timestamp: 1234l,
                notifyIfSkillNotApplied: false,
                isRetry: false,
        ))
        WaitFor.wait { loggerHelper.hasLogMsgStartsWith("ReportSkill (ProjectId=") }
        List<ILoggingEvent> logsList = loggerHelper.logEvents;

        then:
        logsList.find { it.formattedMessage.contains("IsRetry=[false]") }

        cleanup:
        loggerHelper.stop()
    }
}