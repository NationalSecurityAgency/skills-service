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

import skills.controller.exceptions.ErrorCode
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.services.quiz.QuizQuestionType
import skills.services.userActions.DashboardAction
import skills.services.userActions.DashboardItem
import spock.lang.IgnoreIf

import static skills.intTests.utils.SkillsFactory.*

class DashboardUserActions_ProjectsEndpointSpec extends DefaultIntSpec {

    def "get project's user actions"() {
        SkillsService rootService = createRootSkillService()
        def projects = (1..3).collect { createProject( it) }
        projects.each {
            skillsService.createProject(it)
            Thread.sleep(200)
        }
        when:
        def all = rootService.getUserActionsForEverything(10, 1, "projectId", true)
        def proj1Only = skillsService.getUserActionsForProject(projects[0].projectId, 10, 1, "projectId", true)
        def proj2Only = skillsService.getUserActionsForProject(projects[1].projectId, 10, 1, "projectId", true)
        def proj3Only = skillsService.getUserActionsForProject(projects[2].projectId, 10, 1, "projectId", true)
        then:
        all.data.projectId == projects.projectId.sort(false)
        all.count == 3
        all.totalCount == 3

        proj1Only.count == 1
        proj1Only.totalCount == 1
        proj1Only.data.itemId == [projects[0].projectId]

        proj2Only.data.projectId == [projects[1].projectId]
        proj2Only.count == 1
        proj2Only.totalCount == 1

        proj3Only.data.projectId == [projects[2].projectId]
        proj3Only.count == 1
        proj3Only.totalCount == 1
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "get project's user actions - userId filter"() {
        def p1 = createProject(1)

        List<String> users = ["user1", "something", "other"]

        List<SkillsService> services = users.collect { createService(it) }
        services[0].createProject(p1)
        services[0].addProjectAdmin(p1.projectId, services[1].userName)
        services[0].addProjectAdmin(p1.projectId, services[2].userName)

        userActionsHistoryRepo.deleteAll()

        services.eachWithIndex { it, i ->
            it.createSubject(createSubject(1, i))
        }

        Closure<String> userIdToDisplay = { String userId -> "${userId} for display".toString()}
        when:
        def all = services[0].getUserActionsForProject(p1.projectId, 10, 1, "created", true)
        def user2Only =  services[0].getUserActionsForProject(p1.projectId,10, 1, "created", true, null,  'SOme')
        then:
        all.data.userId == [services[0].userName, services[1].userName, services[2].userName]
        all.count == 3
        all.totalCount == 3

        user2Only.data.userId == [users[1]]
        user2Only.data.userIdForDisplay == [userIdToDisplay(services[1].userName)]
        user2Only.count == 1
        user2Only.totalCount == 1
    }

    def "get project's user actions - item filter"() {
        def p1 = createProject(1)
        def subj1 = createSubject(1, 1)
        def skill1 = createSkill(1, 1,1)
        skillsService.createProject(p1)
        Thread.sleep(100)
        skillsService.createSubject(subj1)
        Thread.sleep(100)
        skillsService.createSkill(skill1)
        when:
        def all = skillsService.getUserActionsForProject(p1.projectId, 10, 1, "created", true)
        def subOnly = skillsService.getUserActionsForProject(p1.projectId,10, 1, "created", true, DashboardItem.Subject)
        then:
        all.data.item == [DashboardItem.Project.toString(), DashboardItem.Subject.toString(), DashboardItem.Skill.toString()]
        all.count == 3
        all.totalCount == 3

        subOnly.data.item == [DashboardItem.Subject.toString()]
        subOnly.count == 1
        subOnly.totalCount == 1
    }

    def "get project's user actions - item id filter"() {
        def p1 = createProject(1)
        def subj1 = createSubject(1, 1)
        def skill1 = createSkill(1, 1,1)
        skillsService.createProject(p1)
        Thread.sleep(100)
        skillsService.createSubject(subj1)
        Thread.sleep(100)
        skillsService.createSkill(skill1)
        when:
        def all = skillsService.getUserActionsForProject(p1.projectId, 10, 1, "created", true)
        def subOnly = skillsService.getUserActionsForProject(p1.projectId,10, 1, "created", true, null, '', subj1.subjectId )
        then:
        all.data.item == [DashboardItem.Project.toString(), DashboardItem.Subject.toString(), DashboardItem.Skill.toString()]
        all.count == 3
        all.totalCount == 3

        subOnly.data.item == [DashboardItem.Subject.toString()]
        subOnly.count == 1
        subOnly.totalCount == 1
    }

    def "get project's user actions - action filter"() {
        def p1 = createProject(1)
        def subj1 = createSubject(1, 1)
        def skill1 = createSkill(1, 1,1)
        skillsService.createProject(p1)
        skillsService.createSubject(subj1)
        skillsService.createSkill(skill1)
        skillsService.deleteSkill(skill1)
        when:
        def all = skillsService.getUserActionsForProject(p1.projectId, 10, 1, "created", true)
        def deleteOnly = skillsService.getUserActionsForProject(p1.projectId,10, 1, "created", true, null, '', '', DashboardAction.Delete)
        then:
        all.count == 4

        deleteOnly.data.action == [DashboardAction.Delete.toString()]
        deleteOnly.count == 1
        deleteOnly.totalCount == 1
    }

    def "get project's user actions - paging"() {
        def p1 = createProject(1)
        def subj1 = createSubject(1, 1)
        def skills = createSkills(5, 1,1)
        skillsService.createProject(p1)
        Thread.sleep(25)
        skillsService.createSubject(subj1)
        Thread.sleep(25)
        skills.each {
            skillsService.createSkill(it)
            Thread.sleep(25)
        }
        when:
        def page1 = skillsService.getUserActionsForProject(p1.projectId, 3, 1, "created", true)
        def page2 = skillsService.getUserActionsForProject(p1.projectId, 3, 2, "created", true)
        def page3 = skillsService.getUserActionsForProject(p1.projectId, 3, 3, "created", true)
        then:
        page1.count == 7
        page1.totalCount == 7
        page1.data.itemId == [p1.projectId, subj1.subjectId, skills[0].skillId]

        page2.count == 7
        page2.totalCount == 7
        page2.data.itemId == [skills[1].skillId, skills[2].skillId, skills[3].skillId]

        page3.count == 7
        page3.totalCount == 7
        page3.data.itemId == [skills[4].skillId]
    }

    def "not allowed to get attributes for another project"() {
        def p1 = createProject(1)
        skillsService.createProject(p1)

        def p2 = createProject(2)
        skillsService.createProject(p2)

        def p2Activity = skillsService.getUserActionsForProject(p2.projectId, 10, 1, "created", true)
        when:
        skillsService.getProjectUserActionAttributes(p1.projectId, p2Activity.data[0].id)
        then:
        SkillsClientException skillsClientException = thrown(SkillsClientException)
        def parsedBody = new groovy.json.JsonSlurper().parseText(skillsClientException.resBody)
        parsedBody.explanation == "UserActionsHistory id [${p2Activity.data[0].id}] does not belong to project [${p1.projectId}]"
        parsedBody.errorCode == ErrorCode.AccessDenied.toString()
    }

    def "not allowed to get attributes for quiz from project endpoint"() {
        def p1 = createProject(1)
        skillsService.createProject(p1)

        def q2 = QuizDefFactory.createQuiz(2)
        skillsService.createQuizDef(q2)

        def q2Activity = skillsService.getUserActionsForQuiz(q2.quizId, 10, 1, "created", true)
        when:
        skillsService.getProjectUserActionAttributes(p1.projectId, q2Activity.data[0].id)
        then:
        SkillsClientException skillsClientException = thrown(SkillsClientException)
        def parsedBody = new groovy.json.JsonSlurper().parseText(skillsClientException.resBody)
        parsedBody.explanation == "UserActionsHistory id [${q2Activity.data[0].id}] does not belong to project [${p1.projectId}]"
        parsedBody.errorCode == ErrorCode.AccessDenied.toString()
    }

    def "get project's action filter options"() {
        def p1 = createProject(1)
        def subj1 = createSubject(1, 1)
        skillsService.createProject(p1)
        skillsService.createSubject(subj1)
        skillsService.deleteSubject(subj1)

        def p2 = createProject(2)
        skillsService.createProject(p2)

        when:
        def p1Options = skillsService.getUserActionFilterOptionsForProject(p1.projectId)
        def p2Options = skillsService.getUserActionFilterOptionsForProject(p2.projectId)
        then:
        p1Options.actionFilterOptions.sort() == [DashboardAction.Delete.toString(), DashboardAction.Create.toString()].sort()
        p1Options.itemFilterOptions.sort() == [DashboardItem.Project.toString(), DashboardItem.Subject.toString()].sort()

        p2Options.actionFilterOptions.sort() == [DashboardAction.Create.toString()].sort()
        p2Options.itemFilterOptions.sort() == [DashboardItem.Project.toString()].sort()
    }

}
