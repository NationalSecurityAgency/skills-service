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

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.transform.builder.Builder

@Canonical
@Builder
@CompileStatic
class SkillEventResult {
    boolean success = true
    String projectId
    String skillId
    String name
    // earned on this request
    int pointsEarned
    // total points earned by this user for this skill
    int totalPointsEarned
    // total points defined by an admin for this skill
    int totalPoints
    // number of times skill needs to be performed in order to earn all of its points
    int numOccurrencesToCompletion

    boolean skillApplied = true
    // only really applicable if it wasn't performed
    String explanation = "Skill event was applied"
    List<CompletionItem> completed = []
    String selfReportType
}
