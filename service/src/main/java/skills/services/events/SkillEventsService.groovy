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
package skills.services.events

import callStack.profiler.Profile
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import skills.utils.MatomoReporter
import skills.utils.MetricsLogger

@Service
@CompileStatic
@Slf4j
class SkillEventsService {

    @Autowired
    SkillEventPublisher skillEventPublisher

    @Autowired
    MetricsLogger metricsLogger;

    @Autowired
    MatomoReporter matomoReporter;

    @Autowired
    SkillEventsTransactionalService skillEventsTransactionalService

    static class AppliedCheckRes {
        boolean skillApplied = true
        String explanation
    }

    static class SkillApprovalParams {
        boolean disableChecks = false
        boolean isFromPassingQuiz = false
        String approvalRequestedMsg
        boolean forAnotherUser = false
        boolean doNotRequireApproval = false

        SkillApprovalParams(){}

        SkillApprovalParams(String approvalRequestedMsg) {
            this.approvalRequestedMsg = approvalRequestedMsg
        }
        void setForAnotherUser(boolean forAnotherUser) {
            this.forAnotherUser = forAnotherUser
        }
        void setDoNotRequireApproval(boolean doNotRequireApproval) {
            this.doNotRequireApproval = doNotRequireApproval
        }
    }
    static SkillApprovalParams defaultSkillApprovalParams = new SkillApprovalParams()

    @Profile
    SkillEventResult reportSkill(String projectId, String skillId, String userId, Boolean notifyIfNotApplied, Date incomingSkillDate, SkillApprovalParams skillApprovalParams = defaultSkillApprovalParams) {
        SkillEventResult result = skillEventsTransactionalService.reportSkillInternal(projectId, skillId, userId, incomingSkillDate, skillApprovalParams)
        if (notifyIfNotApplied || result.skillApplied) {
            skillEventPublisher.publishSkillUpdate(result, userId)
        }
        metricsLogger.logSkillReported(userId, result)
        matomoReporter.reportSkill(userId, result.projectId, result.skillId)
        return result
    }

    @Async
    void identifyPendingNotifications(String userId) {
        skillEventsTransactionalService.notifyUserOfAchievements(userId)
    }

}
