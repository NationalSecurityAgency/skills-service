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
import skills.storage.model.ProjectSummaryResult

@Canonical
class ProjectSummary {
    int projectRefId
    String projectId
    String projectName
    int points
    int totalPoints
    int level
    int totalUsers
    int rank
    String levelDisplayName = 'Level'

    ProjectSummary fromProjectSummaryResult(ProjectSummaryResult projectSummaryResult) {
        this.projectRefId = projectSummaryResult.projectRefId
        this.projectId = projectSummaryResult.projectId
        this.projectName = projectSummaryResult.projectName
        this.points = projectSummaryResult.points ?: 0
        this.totalPoints = projectSummaryResult.totalPoints ?: 0
        // if there are no points, then set totalUsers and rank to totalUsers+1, b/c they are last but not included in the results
        this.totalUsers = projectSummaryResult.points > 0 ? projectSummaryResult.totalUsers : projectSummaryResult.totalUsers + 1
        this.rank = projectSummaryResult.points > 0 ? projectSummaryResult.rank : projectSummaryResult.totalUsers + 1
        return this
    }
}

