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
package skills.skillLoading

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import skills.controller.exceptions.SkillException
import skills.skillLoading.model.*
import skills.storage.model.*
import skills.storage.repos.*

import static skills.storage.model.SkillDef.SelfReportingType
import static skills.storage.repos.SkillApprovalRepo.*

@Component
@CompileStatic
@Slf4j
class ApprovalHistoryLoader {

    static final String REQUESTED = "Approval Requested"
//    static final String PENDING = "Pending Approval"
    static final String APPROVED = "Approved"
    static final String REJECTED = "Rejected"
    static final String AWAITING_GRADING = "Awaiting Grading"
    static final String PASSED = "Passed"
    static final String FAILED = "Failed"
    static final String COMPLETED = "Completed"

    @Autowired
    SkillApprovalRepo skillApprovalRepo

    @Autowired
    UserQuizAttemptRepo userQuizAttemptRepo

    List<ApprovalEvent> loadApprovalHistory(SkillDefWithExtra skillDef, String userId, QuizToSkillDefRepo.QuizNameAndId quizNameAndId) {
        List<ApprovalEvent> approvalHistory = []
        if (skillDef.selfReportingType == SelfReportingType.Approval) {
            String queryProjId = skillDef.copiedFrom ? skillDef.copiedFromProjectId : skillDef.projectId
            Integer querySkillRefId = skillDef.copiedFrom ? skillDef.copiedFrom : skillDef.id
            List<SimpleSkillApproval> skillApprovals = skillApprovalRepo.findApprovalHistoryForSkillsDisplay(userId, queryProjId, querySkillRefId)
            skillApprovals.eachWithIndex { skillApproval, index ->
                if (skillApproval.rejectedOn) {
                    approvalHistory.add(new ApprovalEvent(
                            id: skillApproval.approvalId,
                            eventStatus: REJECTED,
                            userId: skillApproval.userId,
                            userIdForDisplay: skillApproval.userIdForDisplay,
                            approverUserId: skillApproval.approverUserId,
                            approverUserIdForDisplay: skillApproval.approverUserIdForDisplay,
                            eventTime: skillApproval.rejectedOn?.time,
                            description: skillApproval.message,
                    ))
                    approvalHistory.add(new ApprovalEvent(
                            id: "${skillApproval.approvalId}-${index}",
                            eventStatus: REQUESTED,
                            userId: skillApproval.userId,
                            userIdForDisplay: skillApproval.userIdForDisplay,
                            eventTime: skillApproval.requestedOn?.time,
                            description: skillApproval.requestMsg,
                    ))
                } else if (skillApproval.requestedOn && skillApproval.approverUserId) {
                    approvalHistory.add(new ApprovalEvent(
                            id: skillApproval.approvalId,
                            eventStatus: APPROVED,
                            userId: skillApproval.userId,
                            userIdForDisplay: skillApproval.userIdForDisplay,
                            approverUserId: skillApproval.approverUserId,
                            approverUserIdForDisplay: skillApproval.approverUserIdForDisplay,
                            eventTime: skillApproval.approverActionTakenOn?.time,
//                            description: skillApproval.approvalMsg,
                    ))
                    approvalHistory.add(new ApprovalEvent(
                            id: "${skillApproval.approvalId}-${index}",
                            eventStatus: REQUESTED,
                            userId: skillApproval.userId,
                            userIdForDisplay: skillApproval.userIdForDisplay,
                            eventTime: skillApproval.requestedOn?.time,
                            description: skillApproval.requestMsg,
                    ))
                } else {
                    approvalHistory.add(new ApprovalEvent(
                            id: skillApproval.approvalId,
                            eventStatus: REQUESTED,
                            userId: skillApproval.userId,
                            userIdForDisplay: skillApproval.userIdForDisplay,
                            eventTime: skillApproval.requestedOn?.time,
                            description: skillApproval.requestMsg,
                    ))
                }
            }
//            if (approvalHistory && approvalHistory[0].eventStatus == REQUESTED) {
//                approvalHistory.add(0, new ApprovalEvent(eventStatus: PENDING))
//            }
        } else if (skillDef.selfReportingType == SelfReportingType.Quiz && quizNameAndId) {
            if (quizNameAndId) {
                PageRequest allPlease = PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "updated"))
                List<UserQuizAttempt> gradingAttempts = userQuizAttemptRepo.findByQuizRefIdAndUserIdAndStatus(
                        quizNameAndId.getQuizRefId(),
                        userId,
                        [UserQuizAttempt.QuizAttemptStatus.NEEDS_GRADING, UserQuizAttempt.QuizAttemptStatus.PASSED, UserQuizAttempt.QuizAttemptStatus.FAILED], allPlease)
                gradingAttempts.each { UserQuizAttempt attempt ->
                    approvalHistory.add(new ApprovalEvent(
                            id: attempt.id,
                            eventStatus: getEventStatusForQuizAttempt(attempt, quizNameAndId),
                            userId: attempt.userId,
                            eventTime: attempt.completed?.time,
                    ))
                }
            }
        }
        return approvalHistory
    }

    private String getEventStatusForQuizAttempt(UserQuizAttempt attempt, QuizToSkillDefRepo.QuizNameAndId quizNameAndId) {
        if (attempt.status == UserQuizAttempt.QuizAttemptStatus.NEEDS_GRADING) {
            return AWAITING_GRADING
        }
        if (attempt.status == UserQuizAttempt.QuizAttemptStatus.PASSED) {
            return quizNameAndId.quizType == QuizDefParent.QuizType.Survey ? COMPLETED : PASSED
        }
        if (attempt.status == UserQuizAttempt.QuizAttemptStatus.FAILED) {
            return FAILED
        }
        throw new SkillException("Unknown attempt status [${attempt.status}]")
    }
}
