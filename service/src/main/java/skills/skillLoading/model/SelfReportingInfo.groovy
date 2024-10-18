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
package skills.skillLoading.model

class SelfReportingInfo {

    Integer approvalId

    boolean enabled
    boolean justificationRequired
    String type

    // only applicable to when selfReportingType == SelfReportingType.Approval
    Long requestedOn
    Long rejectedOn
    String rejectionMsg

    // only applicable to when selfReportingType == SelfReportingType.Quiz
    String quizId
    String quizName
    Integer numQuizQuestions

    Boolean quizNeedsGrading = false
    Date quizNeedsGradingAttemptDate
    Boolean quizOrSurveyPassed = false
    Integer quizAttemptId = null

    String approvedBy
}
