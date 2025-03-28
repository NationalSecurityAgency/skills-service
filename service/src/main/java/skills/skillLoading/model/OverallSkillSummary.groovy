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

import groovy.transform.Canonical

@Canonical
class OverallSkillSummary {

    String projectName

    int skillsLevel
    Date lastLevelAchieved
    int totalLevels

    int points
    int totalPoints

    int levelPoints
    int levelTotalPoints

    int todaysPoints

    int skillsAchieved
    int totalSkills

    // for My Skills page
    String projectId
    int totalUsers
    int rank

    List<SkillSubjectSummary> subjects

    BadgeStats badges

    static class SingleBadgeInfo {
        String badgeName
        String badgeId
        Date achievedOn
        String iconClass
        Boolean isGlobalBadge
    }

    static class BadgeStats {
        int numTotalBadges = 0
        int numBadgesCompleted = 0
        boolean enabled = false
        List<SingleBadgeInfo> recentlyAwardedBadges
    }

    String projectDescription
}

