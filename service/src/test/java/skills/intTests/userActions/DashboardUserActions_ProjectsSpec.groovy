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
import skills.intTests.catalog.CatalogIntSpec
import skills.intTests.utils.SkillsService
import skills.services.admin.skillReuse.SkillReuseIdUtil
import skills.services.userActions.DashboardAction
import skills.services.userActions.DashboardItem
import skills.storage.model.UserAttrs

import static skills.intTests.utils.SkillsFactory.*

class DashboardUserActions_ProjectsSpec extends CatalogIntSpec {

    def "track project CRUD actions"() {
        SkillsService rootService = createRootSkillService()
        def p1 = createProject(1)
        skillsService.createProject(p1)
        p1.description = "this is a description allright"
        Thread.sleep(100)
        skillsService.updateProject(p1)
        Thread.sleep(100)
        skillsService.deleteProject(p1.projectId)

        UserAttrs userAttrs = userAttrsRepo.findByUserId(skillsService.userName)
        def displayName = userAttrs.getUserIdForDisplay()

        when:
        def res = rootService.getUserActionsForEverything()

        def createAction = rootService.getUserActionAttributes(res.data[2].id)
        def editAction = rootService.getUserActionAttributes(res.data[1].id)
        def deleteAction = rootService.getUserActionAttributes(res.data[0].id)

        then:
        res.count == 3
        res.data[0].action == DashboardAction.Delete.toString()
        res.data[0].item == DashboardItem.Project.toString()
        res.data[0].itemId == p1.projectId
        res.data[0].userId == skillsService.userName
        res.data[0].userIdForDisplay == displayName
        res.data[0].projectId == p1.projectId
        !res.data[0].quizId

        res.data[1].action == DashboardAction.Edit.toString()
        res.data[1].item == DashboardItem.Project.toString()
        res.data[1].itemId == p1.projectId
        res.data[1].userId == skillsService.userName
        res.data[1].userIdForDisplay == displayName
        res.data[1].projectId == p1.projectId
        !res.data[1].quizId

        res.data[2].action == DashboardAction.Create.toString()
        res.data[2].item == DashboardItem.Project.toString()
        res.data[2].itemId == p1.projectId
        res.data[2].userId == skillsService.userName
        res.data[2].userIdForDisplay == displayName
        res.data[2].projectId == p1.projectId
        !res.data[2].quizId

        createAction.id == res.data[2].itemRefId
        createAction.name == p1.name
        createAction.projectId == p1.projectId
        !createAction.description

        editAction.id == res.data[1].itemRefId
        editAction.name == p1.name
        editAction.projectId == p1.projectId
        editAction.description == "this is a description allright"

        !deleteAction.id
    }

    def "track subject CRUD actions"() {
        SkillsService rootService = createRootSkillService()
        def p1 = createProject(1)
        skillsService.createProject(p1)

        def subj = createSubject(1, 1)
        skillsService.createSubject(subj)
        subj.description = "this is a description allright"
        Thread.sleep(100)
        skillsService.updateSubject(subj)
        Thread.sleep(100)
        skillsService.deleteSubject(subj)

        UserAttrs userAttrs = userAttrsRepo.findByUserId(skillsService.userName)
        def displayName = userAttrs.getUserIdForDisplay()

        when:
        def res = rootService.getUserActionsForEverything(10, 1, "created", false, "", DashboardItem.Subject)

        def createAction = rootService.getUserActionAttributes(res.data[2].id)
        def editAction = rootService.getUserActionAttributes(res.data[1].id)
        def deleteAction = rootService.getUserActionAttributes(res.data[0].id)

        then:
        res.count == 3
        res.data[0].action == DashboardAction.Delete.toString()
        res.data[0].item == DashboardItem.Subject.toString()
        res.data[0].itemId == subj.subjectId
        res.data[0].userId == skillsService.userName
        res.data[0].userIdForDisplay == displayName
        res.data[0].projectId == p1.projectId
        !res.data[0].quizId

        res.data[1].action == DashboardAction.Edit.toString()
        res.data[1].item == DashboardItem.Subject.toString()
        res.data[1].itemId == subj.subjectId
        res.data[1].userId == skillsService.userName
        res.data[1].userIdForDisplay == displayName
        res.data[1].projectId == p1.projectId
        !res.data[1].quizId

        res.data[2].action == DashboardAction.Create.toString()
        res.data[2].item == DashboardItem.Subject.toString()
        res.data[2].itemId == subj.subjectId
        res.data[2].userId == skillsService.userName
        res.data[2].userIdForDisplay == displayName
        res.data[2].projectId == p1.projectId
        !res.data[2].quizId

        createAction.id == res.data[2].itemRefId
        createAction.name == subj.name
        createAction.skillId == subj.subjectId
        !createAction.description

        editAction.id == res.data[1].itemRefId
        editAction.name == subj.name
        editAction.skillId == subj.subjectId
        editAction.description == "this is a description allright"

        !deleteAction.id
    }

    def "track skill CRUD actions"() {
        SkillsService rootService = createRootSkillService()
        def p1 = createProject(1)
        skillsService.createProject(p1)
        def subj = createSubject(1, 1)
        skillsService.createSubject(subj)

        def skill = createSkill(1, 1, )
        skillsService.createSkill(skill)
        skill.description = "this is a description allright"
        Thread.sleep(100)
        skillsService.updateSkill(skill)
        Thread.sleep(100)
        skillsService.deleteSkill(skill)

        UserAttrs userAttrs = userAttrsRepo.findByUserId(skillsService.userName)
        def displayName = userAttrs.getUserIdForDisplay()

        when:
        def res = rootService.getUserActionsForEverything(10, 1, "created", false, "", DashboardItem.Skill)
        def createAction = rootService.getUserActionAttributes(res.data[2].id)
        def editAction = rootService.getUserActionAttributes(res.data[1].id)
        def deleteAction = rootService.getUserActionAttributes(res.data[0].id)

        then:
        res.count == 3
        res.data[0].action == DashboardAction.Delete.toString()
        res.data[0].item == DashboardItem.Skill.toString()
        res.data[0].itemId == skill.skillId
        res.data[0].userId == skillsService.userName
        res.data[0].userIdForDisplay == displayName
        res.data[0].projectId == p1.projectId
        !res.data[0].quizId

        res.data[1].action == DashboardAction.Edit.toString()
        res.data[1].item == DashboardItem.Skill.toString()
        res.data[1].itemId == skill.skillId
        res.data[1].userId == skillsService.userName
        res.data[1].userIdForDisplay == displayName
        res.data[1].projectId == p1.projectId
        !res.data[1].quizId

        res.data[2].action == DashboardAction.Create.toString()
        res.data[2].item == DashboardItem.Skill.toString()
        res.data[2].itemId == skill.skillId
        res.data[2].userId == skillsService.userName
        res.data[2].userIdForDisplay == displayName
        res.data[2].projectId == p1.projectId
        !res.data[2].quizId

        createAction.id == res.data[2].itemRefId
        createAction.name == skill.name
        createAction.skillId == skill.skillId
        createAction.description == "This skill [skill1] belongs to project [TestProject1]"

        editAction.id == res.data[1].itemRefId
        editAction.name == skill.name
        editAction.skillId == skill.skillId
        editAction.description == "this is a description allright"

        !deleteAction.id
    }

    def "track skill group CRUD actions"() {
        SkillsService rootService = createRootSkillService()
        def p1 = createProject(1)
        skillsService.createProject(p1)
        def subj = createSubject(1, 1)
        skillsService.createSubject(subj)

        def skill = createSkillsGroup(1, 1, 1)
        skillsService.createSkill(skill)
        skill.description = "this is a description allright"
        Thread.sleep(100)
        skillsService.updateSkill(skill)
        Thread.sleep(100)
        skillsService.deleteSkill(skill)

        UserAttrs userAttrs = userAttrsRepo.findByUserId(skillsService.userName)
        def displayName = userAttrs.getUserIdForDisplay()

        when:
        def res = rootService.getUserActionsForEverything(10, 1, "created", false, "", DashboardItem.SkillsGroup)
        def createAction = rootService.getUserActionAttributes(res.data[2].id)
        def editAction = rootService.getUserActionAttributes(res.data[1].id)
        def deleteAction = rootService.getUserActionAttributes(res.data[0].id)

        then:
        res.count == 3
        res.data[0].action == DashboardAction.Delete.toString()
        res.data[0].item == DashboardItem.SkillsGroup.toString()
        res.data[0].itemId == skill.skillId
        res.data[0].userId == skillsService.userName
        res.data[0].userIdForDisplay == displayName
        res.data[0].projectId == p1.projectId
        !res.data[0].quizId

        res.data[1].action == DashboardAction.Edit.toString()
        res.data[1].item == DashboardItem.SkillsGroup.toString()
        res.data[1].itemId == skill.skillId
        res.data[1].userId == skillsService.userName
        res.data[1].userIdForDisplay == displayName
        res.data[1].projectId == p1.projectId
        !res.data[1].quizId

        res.data[2].action == DashboardAction.Create.toString()
        res.data[2].item == DashboardItem.SkillsGroup.toString()
        res.data[2].itemId == skill.skillId
        res.data[2].userId == skillsService.userName
        res.data[2].userIdForDisplay == displayName
        res.data[2].projectId == p1.projectId
        !res.data[2].quizId

        createAction.id == res.data[2].itemRefId
        createAction.name == skill.name
        createAction.skillId == skill.skillId
        createAction.description == "This skill [skill1] belongs to project [TestProject1]"

        editAction.id == res.data[1].itemRefId
        editAction.name == skill.name
        editAction.skillId == skill.skillId
        editAction.description == "this is a description allright"

        !deleteAction.id
    }

    def "track skills under a group CRUD actions"() {
        SkillsService rootService = createRootSkillService()
        def p1 = createProject(1)
        skillsService.createProject(p1)
        def subj = createSubject(1, 1)
        skillsService.createSubject(subj)

        def group = createSkillsGroup(1, 1, 1)
        skillsService.createSkill(group)

        // clean slate for what we are testing
        userActionsHistoryRepo.deleteAll()

        def skill = createSkill(1, 1, 11)
        skillsService.assignSkillToSkillsGroup(group.skillId, skill)
        skill.description = "this is a description allright"
        Thread.sleep(100)
        skillsService.updateSkill(skill)
        Thread.sleep(100)
        skillsService.deleteSkill(skill)

        UserAttrs userAttrs = userAttrsRepo.findByUserId(skillsService.userName)
        def displayName = userAttrs.getUserIdForDisplay()

        when:
        def res = rootService.getUserActionsForEverything()
        def createAction = rootService.getUserActionAttributes(res.data[2].id)
        def editAction = rootService.getUserActionAttributes(res.data[1].id)
        def deleteAction = rootService.getUserActionAttributes(res.data[0].id)

        then:
        res.count == 3
        res.data[0].action == DashboardAction.Delete.toString()
        res.data[0].item == DashboardItem.Skill.toString()
        res.data[0].itemId == skill.skillId
        res.data[0].userId == skillsService.userName
        res.data[0].userIdForDisplay == displayName
        res.data[0].projectId == p1.projectId
        !res.data[0].quizId

        res.data[1].action == DashboardAction.Edit.toString()
        res.data[1].item == DashboardItem.Skill.toString()
        res.data[1].itemId == skill.skillId
        res.data[1].userId == skillsService.userName
        res.data[1].userIdForDisplay == displayName
        res.data[1].projectId == p1.projectId
        !res.data[1].quizId

        res.data[2].action == DashboardAction.Create.toString()
        res.data[2].item == DashboardItem.Skill.toString()
        res.data[2].itemId == skill.skillId
        res.data[2].userId == skillsService.userName
        res.data[2].userIdForDisplay == displayName
        res.data[2].projectId == p1.projectId
        !res.data[2].quizId

        createAction.id == res.data[2].itemRefId
        createAction.name == skill.name
        createAction.skillId == skill.skillId
        createAction.description == "This skill [skill11] belongs to project [TestProject1]"
        createAction.groupId == group.skillId

        editAction.id == res.data[1].itemRefId
        editAction.name == skill.name
        editAction.skillId == skill.skillId
        editAction.description == "this is a description allright"
        editAction.groupId == group.skillId

        !deleteAction.id
    }

    def "track skill reuse action"() {
        SkillsService rootService = createRootSkillService()
        def p1 = createProject(1)
        skillsService.createProject(p1)
        def subj = createSubject(1, 1)
        skillsService.createSubject(subj)
        def subj2 = createSubject(1, 2)
        skillsService.createSubject(subj2)
        def skill = createSkill(1, 1, 1)
        skillsService.createSkill(skill)

        // clean slate for what we are testing
        userActionsHistoryRepo.deleteAll()

        skillsService.reuseSkills(p1.projectId, [skill.skillId], subj2.subjectId)
        Thread.sleep(100)
        skillsService.deleteSkill([projectId: p1.projectId, subjectId: subj2.subjectId, skillId: SkillReuseIdUtil.addTag(skill.skillId, 0)])

        UserAttrs userAttrs = userAttrsRepo.findByUserId(skillsService.userName)
        def displayName = userAttrs.getUserIdForDisplay()

        when:
        def res = rootService.getUserActionsForEverything()
        def reuseAction = rootService.getUserActionAttributes(res.data[1].id)
        def deleteAction = rootService.getUserActionAttributes(res.data[1].id)

        then:
        res.count == 2
        res.data[0].action == DashboardAction.StopProjectReuse.toString()
        res.data[0].item == DashboardItem.Skill.toString()
        res.data[0].itemId == SkillReuseIdUtil.addTag(skill.skillId, 0)
        res.data[0].userId == skillsService.userName
        res.data[0].userIdForDisplay == displayName
        res.data[0].projectId == p1.projectId
        !res.data[0].quizId

        res.data[1].action == DashboardAction.ReuseInProject.toString()
        res.data[1].item == DashboardItem.Skill.toString()
        res.data[1].itemId == skill.skillId
        res.data[1].userId == skillsService.userName
        res.data[1].userIdForDisplay == displayName
        res.data[1].projectId == p1.projectId
        !res.data[1].quizId

        reuseAction.toSubjectId == subj2.subjectId
        !deleteAction.id
    }

    def "track skill move action"() {
        SkillsService rootService = createRootSkillService()
        def p1 = createProject(1)
        skillsService.createProject(p1)
        def subj = createSubject(1, 1)
        skillsService.createSubject(subj)
        def skill = createSkill(1, 1, )
        skillsService.createSkill(skill)

        def subj2 = createSubject(1, 2)
        skillsService.createSubject(subj2)

        userActionsHistoryRepo.deleteAll()
        skillsService.moveSkills(p1.projectId, [skill.skillId], subj2.subjectId)

        UserAttrs userAttrs = userAttrsRepo.findByUserId(skillsService.userName)
        def displayName = userAttrs.getUserIdForDisplay()

        when:
        def res = rootService.getUserActionsForEverything()
        def moveAction = rootService.getUserActionAttributes(res.data[0].id)

        then:
        res.count == 1
        res.data[0].action == DashboardAction.Move.toString()
        res.data[0].item == DashboardItem.Skill.toString()
        res.data[0].itemId == skill.skillId
        res.data[0].userId == skillsService.userName
        res.data[0].userIdForDisplay == displayName
        res.data[0].projectId == p1.projectId
        !res.data[0].quizId

        moveAction.toSubjectId == subj2.subjectId
    }

    def "track skill catalog export and import action"() {
        SkillsService rootService = createRootSkillService()

        def project1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)
        skillsService.createProjectAndSubjectAndSkills(project1, p1subj1, [skill])

        def project2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        skillsService.createProjectAndSubjectAndSkills(project2, p2subj1, [])

        userActionsHistoryRepo.deleteAll()
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        Thread.sleep(100)
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        Thread.sleep(100)
        skillsService.finalizeSkillsImportFromCatalog(project2.projectId)
        Thread.sleep(100)
        skillsService.removeSkillFromCatalog(project1.projectId, skill.skillId)

        UserAttrs userAttrs = userAttrsRepo.findByUserId(skillsService.userName)
        def displayName = userAttrs.getUserIdForDisplay()

        when:
        def res = rootService.getUserActionsForEverything()
        def importAction = rootService.getUserActionAttributes(res.data[2].id)

        then:
        res.count == 4
        res.data[0].action == DashboardAction.RemoveFromCatalog.toString()
        res.data[0].item == DashboardItem.Skill.toString()
        res.data[0].itemId == skill.skillId
        res.data[0].userId == skillsService.userName
        res.data[0].userIdForDisplay == displayName
        res.data[0].projectId == project1.projectId
        !res.data[0].quizId

        res.data[1].action == DashboardAction.FinalizeCatalogImport.toString()
        res.data[1].item == DashboardItem.Project.toString()
        res.data[1].itemId == project2.projectId
        res.data[1].userId == skillsService.userName
        res.data[1].userIdForDisplay == displayName
        res.data[1].projectId == project2.projectId
        !res.data[1].quizId

        res.data[2].action == DashboardAction.ImportFromCatalog.toString()
        res.data[2].item == DashboardItem.Skill.toString()
        res.data[2].itemId == skill.skillId
        res.data[2].userId == skillsService.userName
        res.data[2].userIdForDisplay == displayName
        res.data[2].projectId == project2.projectId
        !res.data[2].quizId

        res.data[3].action == DashboardAction.ExportToCatalog.toString()
        res.data[3].item == DashboardItem.Skill.toString()
        res.data[3].itemId == skill.skillId
        res.data[3].userId == skillsService.userName
        res.data[3].userIdForDisplay == displayName
        res.data[3].projectId == project1.projectId
        !res.data[3].quizId

        importAction.fromProjectId == project1.projectId
    }

    def "deleting exported skill should only track the deletion of the original skill and not the imported skills"() {
        SkillsService rootService = createRootSkillService()

        def project1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)
        skillsService.createProjectAndSubjectAndSkills(project1, p1subj1, [skill])

        def project2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        skillsService.createProjectAndSubjectAndSkills(project2, p2subj1, [])

        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.finalizeSkillsImportFromCatalog(project2.projectId)

        userActionsHistoryRepo.deleteAll()
        skillsService.deleteSkill(skill)

        UserAttrs userAttrs = userAttrsRepo.findByUserId(skillsService.userName)
        def displayName = userAttrs.getUserIdForDisplay()

        when:
        def res = rootService.getUserActionsForEverything()
        println JsonOutput.prettyPrint(JsonOutput.toJson(res))

        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        then:
        res.count == 1
        res.data[0].action == DashboardAction.Delete.toString()
        res.data[0].item == DashboardItem.Skill.toString()
        res.data[0].itemId == skill.skillId
        res.data[0].userId == skillsService.userName
        res.data[0].userIdForDisplay == displayName
        res.data[0].projectId == project1.projectId
        !res.data[0].quizId
    }
}
