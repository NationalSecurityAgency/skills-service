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
package skills.metrics.builders.multipleProj

import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.storage.repos.UserAchievedCustomRepo
import spock.lang.Specification

class FindExpertsForMultipleProjectsMetricsBuilderSpec extends Specification {

    UserAchievedCustomRepo repo = Mock()
    FindExpertsForMultipleProjectsMetricsBuilder builder = new FindExpertsForMultipleProjectsMetricsBuilder(userAchievedCustomRepo: repo)

    private Map props(String projIdsAndLevel) {
        return [projIdsAndLevel: projIdsAndLevel, currentPage: "1", pageSize: "5", sortDesc: "true"]
    }

    def "a level that is not an integer is a BadParam instead of an unexpected error - #levelStr"() {
        when:
        builder.build(props("proj1AndLevel${levelStr},proj2AndLevel2"))

        then:
        SkillException e = thrown()
        e.errorCode == ErrorCode.BadParam
        e.message == "Metrics[findExpertsForMultipleProjectsChartBuilder]: level must be a valid integer in [proj1AndLevel${levelStr},proj2AndLevel2]"

        where:
        levelStr << ["abc", "1.5", "", "99999999999"]
    }

    def "the error for a level below 1 names the chart, not a project id"() {
        when:
        builder.build(props("proj1AndLevel0,proj2AndLevel2"))

        then:
        SkillException e = thrown()
        e.message == "Metrics[findExpertsForMultipleProjectsChartBuilder]: level must be more than 0 in [proj1AndLevel0,proj2AndLevel2]"
    }

    def "valid projects and levels are passed to the repository"() {
        given:
        List captured = null
        repo.countUsersWithMaxLevelForMultipleProjects(_) >> { args -> captured = args[0]; return 0 }

        when:
        builder.build(props("proj1AndLevel1,proj2AndLevel3"))

        then:
        captured*.projectId == ["proj1", "proj2"]
        captured*.level == [1, 3]
    }
}
