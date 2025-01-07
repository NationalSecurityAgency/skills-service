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

import skills.storage.model.SimpleBadgeRes

class SkillSummary extends SkillSummaryParent {

    int pointIncrement
    int pointIncrementInterval
    int maxOccurrencesWithinIncrementInterval

    SkillDescription description

    SkillDependencySummary dependencyInfo
    List<SkillDependencySummary> badgeDependencyInfo

    // null if the skill is NOT achieved
    Date achievedOn

    SelfReportingInfo selfReporting

    String subjectName
    String subjectId
    String nextSkillId
    String prevSkillId
    int totalSkills
    int orderInGroup
    Boolean isLastViewed

    List<SimpleBadgeRes> badges
    List<SkillTag> tags = []

    VideoSummary videoSummary

    Boolean isMotivationalSkill
    int daysOfInactivityBeforeExp
    Date mostRecentlyPerformedOn
    Date expirationDate
    Date lastExpirationDate

    String groupName
    String groupSkillId

    List<ApprovalEvent> approvalHistory = []
}
