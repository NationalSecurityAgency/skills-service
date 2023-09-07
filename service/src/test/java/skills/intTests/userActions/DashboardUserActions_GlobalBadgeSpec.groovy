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

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.services.userActions.DashboardAction
import skills.services.userActions.DashboardItem
import skills.storage.model.UserAttrs

import static skills.intTests.utils.SkillsFactory.*

class DashboardUserActions_GlobalBadgeSpec extends DefaultIntSpec {

    String  displayName
    SkillsService rootService
    def setup() {
        rootService = createRootSkillService()
        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(rootService.userName)
        displayName = userAttrs.getUserIdForDisplay()
    }

    def "global badge CRUD"() {
        def badge1 = SkillsFactory.createBadge(1)

        when:
        rootService.createGlobalBadge(badge1)
        badge1.helpUrl = "https://some.com"
        badge1.description = "hi"
        rootService.updateGlobalBadge(badge1)
        rootService.deleteGlobalBadge(badge1.badgeId)
        then:
        def res = rootService.getUserActionsForEverything()
        def createAction = rootService.getUserActionAttributes(res.data[2].id)
        def editAction = rootService.getUserActionAttributes(res.data[1].id)
        def deleteAction = rootService.getUserActionAttributes(res.data[0].id)
        then:
        res.count == 3
        res.data[0].action == DashboardAction.Delete.toString()
        res.data[0].item == DashboardItem.GlobalBadge.toString()
        res.data[0].itemId == badge1.badgeId
        res.data[0].userId == rootService.userName.toLowerCase()
        res.data[0].userIdForDisplay == displayName
        !res.data[0].projectId
        !res.data[0].quizId

        res.data[1].action == DashboardAction.Edit.toString()
        res.data[1].item == DashboardItem.GlobalBadge.toString()
        res.data[1].itemId == badge1.badgeId
        res.data[1].userId == rootService.userName.toLowerCase()
        res.data[1].userIdForDisplay == displayName
        !res.data[1].projectId
        !res.data[1].quizId

        res.data[2].action == DashboardAction.Create.toString()
        res.data[2].item == DashboardItem.GlobalBadge.toString()
        res.data[2].itemId == badge1.badgeId
        res.data[2].userId == rootService.userName.toLowerCase()
        res.data[2].userIdForDisplay == displayName
        !res.data[2].projectId
        !res.data[2].quizId

        createAction.name == badge1.name
        createAction.skillId == badge1.badgeId
        createAction.enabled == "false"
        !createAction.description

        editAction.id
        editAction.name == badge1.name
        editAction.skillId == badge1.badgeId
        editAction.description == "hi"
        editAction.helpUrl == "https://some.com"
        editAction.enabled == "false"

        !deleteAction.id
    }

    def "assign/remove skills to/from a global badge"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge1 = SkillsFactory.createBadge(1)
        rootService.createGlobalBadge(badge1)

        userActionsHistoryRepo.deleteAll()
        when:
        rootService.assignSkillToGlobalBadge(p1.projectId, badge1.badgeId, p1Skills[0].skillId)
        rootService.removeSkillFromGlobalBadge(p1.projectId, badge1.badgeId, p1Skills[0].skillId)

        def res = rootService.getUserActionsForEverything()
        def assignAction = rootService.getUserActionAttributes(res.data[1].id)
        def removeAction = rootService.getUserActionAttributes(res.data[0].id)

        then:
        res.count == 2
        res.data[0].action == DashboardAction.RemoveSkillAssignment.toString()
        res.data[0].item == DashboardItem.GlobalBadge.toString()
        res.data[0].itemId == badge1.badgeId
        res.data[0].userId == rootService.userName.toLowerCase()
        res.data[0].userIdForDisplay == displayName
        !res.data[0].projectId
        !res.data[0].quizId

        res.data[1].action == DashboardAction.AssignSkill.toString()
        res.data[1].item == DashboardItem.GlobalBadge.toString()
        res.data[1].itemId == badge1.badgeId
        res.data[1].userId == rootService.userName.toLowerCase()
        res.data[1].userIdForDisplay == displayName
        !res.data[1].projectId
        !res.data[1].quizId

        assignAction.badgeId == badge1.badgeId
        assignAction.skillId == p1Skills[0].skillId
        assignAction.projectId == p1.projectId

        removeAction.badgeId == badge1.badgeId
        removeAction.skillId == p1Skills[0].skillId
        removeAction.projectId == p1.projectId
    }

    def "assign/remove project levels to/from a global badge"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge1 = SkillsFactory.createBadge(1)
        rootService.createGlobalBadge(badge1)

        userActionsHistoryRepo.deleteAll()
        when:
        rootService.assignProjectLevelToGlobalBadge(projectId: p1.projectId, badgeId: badge1.badgeId, level: "1")
        rootService.changeProjectLevelOnGlobalBadge(projectId: p1.projectId, badgeId: badge1.badgeId, currentLevel: "1", newLevel: "2")
        rootService.removeProjectLevelFromGlobalBadge(projectId: p1.projectId, badgeId: badge1.badgeId, level: "2")

        def res = rootService.getUserActionsForEverything()
        def createAction = rootService.getUserActionAttributes(res.data[2].id)
        def editAction = rootService.getUserActionAttributes(res.data[1].id)
        def removeAction = rootService.getUserActionAttributes(res.data[0].id)

        then:
        res.count == 3
        res.data[0].action == DashboardAction.RemoveLevelAssignment.toString()
        res.data[0].item == DashboardItem.GlobalBadge.toString()
        res.data[0].itemId == badge1.badgeId
        res.data[0].userId == rootService.userName.toLowerCase()
        res.data[0].userIdForDisplay == displayName
        !res.data[0].projectId
        !res.data[0].quizId

        res.data[1].action == DashboardAction.AssignLevel.toString()
        res.data[1].item == DashboardItem.GlobalBadge.toString()
        res.data[1].itemId == badge1.badgeId
        res.data[1].userId == rootService.userName.toLowerCase()
        res.data[1].userIdForDisplay == displayName
        !res.data[1].projectId
        !res.data[1].quizId

        res.data[2].action == DashboardAction.AssignLevel.toString()
        res.data[2].item == DashboardItem.GlobalBadge.toString()
        res.data[2].itemId == badge1.badgeId
        res.data[2].userId == rootService.userName.toLowerCase()
        res.data[2].userIdForDisplay == displayName
        !res.data[2].projectId
        !res.data[2].quizId

        createAction.badgeId == badge1.badgeId
        createAction.level == 1
        createAction.projectId == p1.projectId

        editAction.badgeId == badge1.badgeId
        editAction.level == 2
        editAction.previousLevel == 1
        editAction.projectId == p1.projectId

        removeAction.badgeId == badge1.badgeId
        removeAction.level == 2
        removeAction.projectId == p1.projectId
    }

}
