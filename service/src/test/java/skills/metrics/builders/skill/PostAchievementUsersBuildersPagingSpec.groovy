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
package skills.metrics.builders.skill

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.storage.model.SkillDef
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.UserAchievedLevelRepo
import skills.storage.repos.UserEventsRepo
import spock.lang.Specification

class PostAchievementUsersBuildersPagingSpec extends Specification {

    String projectId = "proj1"

    SkillDefRepo skillDefRepo = Mock()
    UserEventsRepo userEventsRepo = Mock()
    UserAchievedLevelRepo userAchievedLevelRepo = Mock()

    def setup() {
        skillDefRepo.findByProjectIdAndSkillId(projectId, "skill1") >> new SkillDef(id: 10, projectId: projectId, skillId: "skill1")
    }

    private def newBuilder(Class type) {
        if (type == UsagePostAchievementUsersBuilder) {
            return new UsagePostAchievementUsersBuilder(skillDefRepo: skillDefRepo, userEventsRepo: userEventsRepo)
        }
        return new NoUsagePostAchievementUsersBuilder(skillDefRepo: skillDefRepo, userEventsRepo: userEventsRepo, userAchievedLevelRepo: userAchievedLevelRepo)
    }

    private static Map validProps(Map overrides = [:]) {
        return [skillId: "skill1", page: "1", pageSize: "5", sortBy: "userId", sortDesc: "false"] + overrides
    }

    static List<Class> builderTypes = [UsagePostAchievementUsersBuilder, NoUsagePostAchievementUsersBuilder]

    def "invalid paging params are a BadParam, not an unexpected error - #type.simpleName #param=#value"() {
        given:
        def builder = newBuilder(type)

        when:
        builder.build(projectId, builder.id, validProps([(param): value]))

        then:
        SkillException e = thrown()
        e.errorCode == ErrorCode.BadParam
        e.projectId == projectId
        e.message.contains("[${value}]")
        e.message.contains(param)

        where:
        [type, param, value] << [
                builderTypes,
                [["page", "abc"], ["page", "0"], ["page", "-1"], ["pageSize", "abc"], ["pageSize", "0"], ["pageSize", "-5"]],
        ].combinations().collect { t, pv -> [t, pv[0], pv[1]] }
    }

    def "missing paging params are rejected with a message naming the param - #type.simpleName #param"() {
        given:
        def builder = newBuilder(type)
        Map props = validProps()
        props.remove(param)

        when:
        builder.build(projectId, builder.id, props)

        then:
        SkillException e = thrown()
        e.message.contains(param)

        where:
        [type, param] << [builderTypes, ["page", "pageSize", "sortBy", "sortDesc"]].combinations()
    }

    def "valid paging params are converted to a zero based PageRequest - #type.simpleName"() {
        given:
        def builder = newBuilder(type)
        PageRequest captured = null
        userEventsRepo.getUsersUsingSkillAfterAchievement(_, _, _) >> { args ->
            captured = args.find { it instanceof PageRequest }
            return []
        }
        userEventsRepo.getUsersNotUsingSkillAfterAchievement(_, _) >> { args ->
            captured = args.find { it instanceof PageRequest }
            return []
        }

        when:
        builder.build(projectId, builder.id, validProps(page: "3", pageSize: "7", sortBy: "date", sortDesc: "true"))

        then:
        captured.pageNumber == 2
        captured.pageSize == 7
        captured.sort.getOrderFor("date").direction == Sort.Direction.DESC

        where:
        type << builderTypes
    }
}
