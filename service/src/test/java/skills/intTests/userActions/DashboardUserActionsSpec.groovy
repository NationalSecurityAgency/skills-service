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
package skills.intTests.userActions

import groovy.json.JsonOutput
import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.catalog.CatalogIntSpec
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsService
import skills.services.userActions.DashboardAction
import skills.services.userActions.DashboardItem
import skills.storage.repos.UserActionsHistoryRepo
import spock.lang.IgnoreIf

import static skills.intTests.utils.SkillsFactory.*

class DashboardUserActionsSpec extends DefaultIntSpec {

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "get actions filters - userId filter"() {
        SkillsService rootService = createRootSkillService()
        def p1 = createProject(1)

        List<String> users = ["user1", "something", "other"]
        List<SkillsService> services = users.collect { createService(it) }
        services.eachWithIndex{ SkillsService entry, int i ->
            entry.createProject(createProject(i))
        }

        Closure<String> userIdToDisplay = { String userId -> "${userId} for display".toString()}
        when:
        def all = rootService.getUserActionsForEverything(10, 1, "userId", true)
        def user1Only = rootService.getUserActionsForEverything(10, 1, "userId", true, '', null, 'SeR')
        def user2Only = rootService.getUserActionsForEverything(10, 1, "userId", true, '', null, 'SOme')
        then:
        all.data.userId == users.sort(false)
        all.data.userIdForDisplay == users.sort(false).collect { userIdToDisplay(it) }
        all.count == 3
        all.totalCount == 3

        user1Only.data.userId == [users[0]]
        user1Only.data.userIdForDisplay == [userIdToDisplay(users[0])]
        user1Only.count == 1
        user1Only.totalCount == 1

        user2Only.data.userId == [users[1]]
        user2Only.data.userIdForDisplay == [userIdToDisplay(users[1])]
        user2Only.count == 1
        user2Only.totalCount == 1
    }

    def "get actions filters - projectId filter"() {
        SkillsService rootService = createRootSkillService()
        def projects = (1..3).collect { createProject( it) }
        projects.each {
            skillsService.createProject(it)
        }
        when:
        def all = rootService.getUserActionsForEverything(10, 1, "projectId", true)
        def proj1Only = rootService.getUserActionsForEverything(10, 1, "projectId", true, 'PrOJect1')
        def proj2Only = rootService.getUserActionsForEverything(10, 1, "projectId", true, 'jEcT2')
        then:
        all.data.projectId == projects.projectId.sort(false)
        all.count == 3
        all.totalCount == 3

        proj1Only.data.projectId == [projects[0].projectId]
        proj1Only.count == 1
        proj1Only.totalCount == 1

        proj2Only.data.projectId == [projects[1].projectId]
        proj2Only.count == 1
        proj2Only.totalCount == 1
    }

    def "get actions filters - itemFilter filter"() {
        SkillsService rootService = createRootSkillService()
        skillsService.createProject(createProject(1))
        skillsService.createSubject(createSubject(1, 1))
        skillsService.createSkill(createSkill(1, 1, 1))
        when:
        def all = rootService.getUserActionsForEverything(10, 1, "item", true)
        def subjOnly = rootService.getUserActionsForEverything(10, 1, "item", true, '', DashboardItem.Subject)
        def skillOnly = rootService.getUserActionsForEverything(10, 1, "item", true, '', DashboardItem.Skill)
        then:
        all.data.item == [DashboardItem.Project.toString(), DashboardItem.Subject.toString(), DashboardItem.Skill.toString()].sort(false)
        all.count == 3
        all.totalCount == 3

        subjOnly.data.item == [DashboardItem.Subject.toString()]
        subjOnly.count == 1
        subjOnly.totalCount == 1

        skillOnly.data.item == [DashboardItem.Skill.toString()]
        skillOnly.count == 1
        skillOnly.totalCount == 1
    }

    def "get actions filters - quizId filter"() {
        SkillsService rootService = createRootSkillService()

        List quizzes = (1..3).collect {QuizDefFactory.createQuiz(it) }
        quizzes.each  { skillsService.createQuizDef(it) }

        when:
        def all = rootService.getUserActionsForEverything(10, 1, "quizId", true)
        def quiz1Only = rootService.getUserActionsForEverything(10, 1, "quizId", true, '', null, '', 'TeSTQuiz1')
        def quiz2Only = rootService.getUserActionsForEverything(10, 1, "quizId", true, '', null, '', 'IZ2')

        then:
        all.data.quizId == quizzes.quizId.sort(false)
        all.count == 3
        all.totalCount == 3

        quiz1Only.data.quizId == [quizzes[0].quizId]
        quiz1Only.count == 1
        quiz1Only.totalCount == 1

        quiz2Only.data.quizId == [quizzes[1].quizId]
        quiz2Only.count == 1
        quiz2Only.totalCount == 1
    }

    def "get actions filters - item id filter"() {
        SkillsService rootService = createRootSkillService()

        List quizzes = (1..3).collect {QuizDefFactory.createQuiz(it) }
        quizzes.each  { skillsService.createQuizDef(it) }

        when:
        def all = rootService.getUserActionsForEverything(10, 1, "itemId", true)
        def quiz1Only = rootService.getUserActionsForEverything(10, 1, "itemId", true, '', null, '', '', 'TeSTQuiz1')
        def quiz2Only = rootService.getUserActionsForEverything(10, 1, "itemId", true, '', null, '', '', 'Iz2')

        then:
        all.data.itemId == quizzes.quizId.sort(false)
        all.count == 3
        all.totalCount == 3

        quiz1Only.data.itemId == [quizzes[0].quizId]
        quiz1Only.count == 1
        quiz1Only.totalCount == 1

        quiz2Only.data.itemId == [quizzes[1].quizId]
        quiz2Only.count == 1
        quiz2Only.totalCount == 1
    }

    def "get actions filters - action filter"() {
        SkillsService rootService = createRootSkillService()

        def p1 = createProject(1)
        skillsService.createProject(p1)
        skillsService.updateProject(p1)
        skillsService.deleteProject(p1.projectId)

        when:
        def all = rootService.getUserActionsForEverything(10, 1, "action", true)
        def edit = rootService.getUserActionsForEverything(10, 1, "action", true, '', null, '', '', '', DashboardAction.Edit)
        def delete = rootService.getUserActionsForEverything(10, 1, "action", true, '', null, '', '', '',  DashboardAction.Delete)

        then:
        all.data.action == [DashboardAction.Edit, DashboardAction.Delete, DashboardAction.Create].collect { it.toString() }.sort()
        all.count == 3
        all.totalCount == 3

        edit.data.action == [DashboardAction.Edit.toString()]
        edit.count == 1
        edit.totalCount == 1

        delete.data.action == [DashboardAction.Delete.toString()]
        delete.count == 1
        delete.totalCount == 1
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "get actions filters - combine multiple filters"() {
        SkillsService rootService = createRootSkillService()

        def p1 = createProject(1)
        skillsService.createProject(p1)
        skillsService.updateProject(p1)
        skillsService.deleteProject(p1.projectId)

        when:
        def res = rootService.getUserActionsForEverything(10, 1, "action", true, p1.projectId, DashboardItem.Project, skillsService.userName, '', p1.projectId, DashboardAction.Edit)

        then:
        res.data.action == [DashboardAction.Edit.toString()]
        res.count == 1
        res.totalCount == 1
    }
}
