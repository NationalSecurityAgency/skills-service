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

class SkillBadgeSummary {

    String badge
    String badgeId
    String description
    boolean badgeAchieved = false
    Date dateAchieved
    int numSkillsAchieved
    int numTotalSkills

    Date startDate
    Date endDate
    boolean isGem() { return startDate && endDate }

    List<SkillSummary> skills = []

    String iconClass
    boolean global = false

    String helpUrl
}
