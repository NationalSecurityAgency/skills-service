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

import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.services.admin.skillReuse.SkillReuseIdUtil
import skills.services.attributes.ExpirationAttrs
import skills.services.settings.Settings
import skills.services.userActions.DashboardAction
import skills.services.userActions.DashboardItem
import skills.storage.model.ProjectError
import skills.storage.model.SkillDef
import skills.storage.model.UserAttrs
import skills.storage.model.auth.RoleName

import java.time.LocalDateTime

import static skills.intTests.utils.SkillsFactory.*

class DashboardUserActions_ProjectsSpec extends DefaultIntSpec {

    private def getDisplayName() {
        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(skillsService.userName)
        return userAttrs.getUserIdForDisplay()
    }
    def "track project CRUD actions"() {
        SkillsService rootService = createRootSkillService()
        def p1 = createProject(1)
        skillsService.createProject(p1)
        p1.description = "this is a description allright"
        Thread.sleep(100)
        skillsService.updateProject(p1)
        Thread.sleep(100)
        skillsService.deleteProject(p1.projectId)

        when:
        def res = rootService.getUserActionsForEverything()

        def createAction = rootService.getUserActionAttributes(res.data[2].id)
        def editAction = rootService.getUserActionAttributes(res.data[1].id)
        def deleteAction = rootService.getUserActionAttributes(res.data[0].id)

        String displayName = getDisplayName()
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

        when:
        def res = rootService.getUserActionsForEverything(10, 1, "created", false, "", DashboardItem.Subject)

        def createAction = rootService.getUserActionAttributes(res.data[2].id)
        def editAction = rootService.getUserActionAttributes(res.data[1].id)
        def deleteAction = rootService.getUserActionAttributes(res.data[0].id)
        String displayName = getDisplayName()
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

        when:
        def res = rootService.getUserActionsForEverything(10, 1, "created", false, "", DashboardItem.Skill)
        def createAction = rootService.getUserActionAttributes(res.data[2].id)
        def editAction = rootService.getUserActionAttributes(res.data[1].id)
        def deleteAction = rootService.getUserActionAttributes(res.data[0].id)
        String displayName = getDisplayName()
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

        when:
        def res = rootService.getUserActionsForEverything(10, 1, "created", false, "", DashboardItem.SkillsGroup)
        def createAction = rootService.getUserActionAttributes(res.data[2].id)
        def editAction = rootService.getUserActionAttributes(res.data[1].id)
        def deleteAction = rootService.getUserActionAttributes(res.data[0].id)
        String displayName = getDisplayName()
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

        when:
        def res = rootService.getUserActionsForEverything()
        def createAction = rootService.getUserActionAttributes(res.data[2].id)
        def editAction = rootService.getUserActionAttributes(res.data[1].id)
        def deleteAction = rootService.getUserActionAttributes(res.data[0].id)
        String displayName = getDisplayName()
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

        when:
        def res = rootService.getUserActionsForEverything()
        def reuseAction = rootService.getUserActionAttributes(res.data[1].id)
        def deleteAction = rootService.getUserActionAttributes(res.data[1].id)
        String displayName = getDisplayName()
        then:
        res.count == 2
        res.data[0].action == DashboardAction.StopInProjectReuse.toString()
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

        when:
        def res = rootService.getUserActionsForEverything()
        def moveAction = rootService.getUserActionAttributes(res.data[0].id)
        String displayName = getDisplayName()
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

        when:
        def res = rootService.getUserActionsForEverything()
        def importAction = rootService.getUserActionAttributes(res.data[2].id)
        String displayName = getDisplayName()
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

        when:
        def res = rootService.getUserActionsForEverything()

        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        String displayName = getDisplayName()
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

    def "track skill tags CRUD"() {
        SkillsService rootService = createRootSkillService()

        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        List<String> taggedSkillIds = skills.skillId
        String tagValue = "New Tag"
        String tagId = 'newtag'

        userActionsHistoryRepo.deleteAll()
        when:
        skillsService.addTagToSkills(proj.projectId, taggedSkillIds, tagValue)
        Thread.sleep(100)
        skillsService.deleteTagForSkills(proj.projectId, taggedSkillIds, tagId)
        def res = rootService.getUserActionsForEverything()

        def addTag = rootService.getUserActionAttributes(res.data[1].id)
        def removeTag = rootService.getUserActionAttributes(res.data[0].id)

        String displayName = getDisplayName()
        then:
        res.count == 2
        res.data[0].action == DashboardAction.Delete.toString()
        res.data[0].item == DashboardItem.Tag.toString()
        res.data[0].itemId == skills[0].skillId
        res.data[0].userId == skillsService.userName
        res.data[0].userIdForDisplay == displayName
        res.data[0].projectId == proj.projectId
        !res.data[0].quizId

        res.data[1].action == DashboardAction.Create.toString()
        res.data[1].item == DashboardItem.Tag.toString()
        res.data[1].itemId == skills[0].skillId
        res.data[1].userId == skillsService.userName
        res.data[1].userIdForDisplay == displayName
        res.data[1].projectId == proj.projectId
        !res.data[1].quizId

        addTag.tagId == tagId
        addTag.tagValue == tagValue

        removeTag.tagId == tagId
        removeTag.tagValue == tagValue
    }

    def "track setting external video for a skill"() {
        SkillsService rootService = createRootSkillService()

        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        userActionsHistoryRepo.deleteAll()
        when:
        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[0].skillId, [
                videoUrl: "http://some.url",
                transcript: "transcript",
                captions: "captions",
        ])
        Thread.sleep(100)
        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[0].skillId, [
                videoUrl: "http://some.url",
                transcript: "transcript1",
                captions: "captions1",
        ])
        Thread.sleep(100)
        skillsService.deleteSkillVideoAttributes(p1.projectId, p1Skills[0].skillId)

        def res = rootService.getUserActionsForEverything()

        def create = rootService.getUserActionAttributes(res.data[2].id)
        def edit = rootService.getUserActionAttributes(res.data[1].id)
        def delete = rootService.getUserActionAttributes(res.data[0].id)
        String displayName = getDisplayName()
        then:
        res.count == 3
        res.data[0].action == DashboardAction.Delete.toString()
        res.data[0].item == DashboardItem.VideoSettings.toString()
        res.data[0].itemId == p1Skills[0].skillId
        res.data[0].userId == skillsService.userName
        res.data[0].userIdForDisplay == displayName
        res.data[0].projectId == p1.projectId
        !res.data[0].quizId

        res.data[1].action == DashboardAction.Edit.toString()
        res.data[1].item == DashboardItem.VideoSettings.toString()
        res.data[1].itemId == p1Skills[0].skillId
        res.data[1].userId == skillsService.userName
        res.data[1].userIdForDisplay == displayName
        res.data[1].projectId == p1.projectId
        !res.data[1].quizId

        res.data[2].action == DashboardAction.Create.toString()
        res.data[2].item == DashboardItem.VideoSettings.toString()
        res.data[2].itemId == p1Skills[0].skillId
        res.data[2].userId == skillsService.userName
        res.data[2].userIdForDisplay == displayName
        res.data[2].projectId == p1.projectId
        !res.data[2].quizId

        create.captions == "captions"
        create.transcript == "transcript"
        create.videoUrl == "http://some.url"
        create.isInternallyHosted == false
        !create.internallyHostedFileName

        edit.captions == "captions1"
        edit.transcript == "transcript1"
        edit.videoUrl == "http://some.url"
        edit.isInternallyHosted == false
        !edit.internallyHostedFileName

        !delete.videoUrl
    }

    def "track uploading video for a skill"() {
        SkillsService rootService = createRootSkillService()

        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        userActionsHistoryRepo.deleteAll()
        when:
        Resource video = new ClassPathResource("/testVideos/create-quiz.mp4")
        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[0].skillId, [
                file: video,
                transcript: "transcript",
                captions: "captions",
        ])
        Thread.sleep(100)
        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[0].skillId, [
                isAlreadyHosted: true,
                transcript: "transcript1",
                captions: "captions1",
        ])
        Thread.sleep(100)
        skillsService.deleteSkillVideoAttributes(p1.projectId, p1Skills[0].skillId)

        def res = rootService.getUserActionsForEverything()

        def create = rootService.getUserActionAttributes(res.data[2].id)
        def edit = rootService.getUserActionAttributes(res.data[1].id)
        def delete = rootService.getUserActionAttributes(res.data[0].id)
        String displayName = getDisplayName()
        then:
        res.count == 3
        res.data[0].action == DashboardAction.Delete.toString()
        res.data[0].item == DashboardItem.VideoSettings.toString()
        res.data[0].itemId == p1Skills[0].skillId
        res.data[0].userId == skillsService.userName
        res.data[0].userIdForDisplay == displayName
        res.data[0].projectId == p1.projectId
        !res.data[0].quizId

        res.data[1].action == DashboardAction.Edit.toString()
        res.data[1].item == DashboardItem.VideoSettings.toString()
        res.data[1].itemId == p1Skills[0].skillId
        res.data[1].userId == skillsService.userName
        res.data[1].userIdForDisplay == displayName
        res.data[1].projectId == p1.projectId
        !res.data[1].quizId

        res.data[2].action == DashboardAction.Create.toString()
        res.data[2].item == DashboardItem.VideoSettings.toString()
        res.data[2].itemId == p1Skills[0].skillId
        res.data[2].userId == skillsService.userName
        res.data[2].userIdForDisplay == displayName
        res.data[2].projectId == p1.projectId
        !res.data[2].quizId

        create.captions == "captions"
        create.transcript == "transcript"
        create.videoUrl.startsWith('/api/download/')
        create.isInternallyHosted == true
        create.internallyHostedFileName == 'create-quiz.mp4'

        edit.captions == "captions1"
        edit.transcript == "transcript1"
        edit.videoUrl.startsWith('/api/download/')
        edit.isInternallyHosted == true
        edit.internallyHostedFileName == 'create-quiz.mp4'

        !delete.videoUrl
    }

    def "track skill expiration settings for a skill"() {
        SkillsService rootService = createRootSkillService()

        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        userActionsHistoryRepo.deleteAll()
        when:
        LocalDateTime expirationDate = (new Date() - 1).toLocalDateTime()
        skillsService.saveSkillExpirationAttributes(p1.projectId, p1Skills[0].skillId, [
                expirationType: ExpirationAttrs.YEARLY,
                every: 1,
                monthlyDay: expirationDate.dayOfMonth,
                nextExpirationDate: expirationDate.toDate(),
        ])

        def res = rootService.getUserActionsForEverything()
        def create = rootService.getUserActionAttributes(res.data[0].id)
        String displayName = getDisplayName()
        then:
        res.count == 1
        res.data[0].action == DashboardAction.Create.toString()
        res.data[0].item == DashboardItem.ExpirationSettings.toString()
        res.data[0].itemId == p1Skills[0].skillId
        res.data[0].userId == skillsService.userName
        res.data[0].userIdForDisplay == displayName
        res.data[0].projectId == p1.projectId
        !res.data[0].quizId

        create.every == 1
        create.monthlyDay
        create.expirationType ==  ExpirationAttrs.YEARLY
        create.nextExpirationDate
    }

    def "track badge CRUD actions"() {
        SkillsService rootService = createRootSkillService()
        def p1 = createProject(1)
        skillsService.createProject(p1)

        def badge = createBadge(1, 1)
        badge.helpUrl = "https://skilltreeHelpBadgeNow.com"
        badge.awardAttrs = [ name: 'Test Badge', iconClass: 'abc', numMinutes: 60 ]
        badge.startDate = new Date() + 2
        badge.endDate = new Date() + 2
        skillsService.createBadge(badge)
        badge.description = "this is a description allright"
        Thread.sleep(100)
        skillsService.updateBadge(badge)
        Thread.sleep(100)
        skillsService.removeBadge(badge)

        when:
        def res = rootService.getUserActionsForEverything(10, 1, "created", false, "", DashboardItem.Badge)
        def createAction = rootService.getUserActionAttributes(res.data[2].id)
        def editAction = rootService.getUserActionAttributes(res.data[1].id)
        def deleteAction = rootService.getUserActionAttributes(res.data[0].id)
        String displayName = getDisplayName()
        then:
        res.count == 3
        res.data[0].action == DashboardAction.Delete.toString()
        res.data[0].item == DashboardItem.Badge.toString()
        res.data[0].itemId == badge.badgeId
        res.data[0].userId == skillsService.userName
        res.data[0].userIdForDisplay == displayName
        res.data[0].projectId == p1.projectId
        !res.data[0].quizId

        res.data[1].action == DashboardAction.Edit.toString()
        res.data[1].item == DashboardItem.Badge.toString()
        res.data[1].itemId == badge.badgeId
        res.data[1].userId == skillsService.userName
        res.data[1].userIdForDisplay == displayName
        res.data[1].projectId == p1.projectId
        !res.data[1].quizId

        res.data[2].action == DashboardAction.Create.toString()
        res.data[2].item == DashboardItem.Badge.toString()
        res.data[2].itemId == badge.badgeId
        res.data[2].userId == skillsService.userName
        res.data[2].userIdForDisplay == displayName
        res.data[2].projectId == p1.projectId
        !res.data[2].quizId

        createAction.id == res.data[2].itemRefId
        createAction.name == badge.name
        createAction.skillId == badge.badgeId
        createAction.helpUrl == "https://skilltreeHelpBadgeNow.com"
        createAction["BonusAward:name"] == "Test Badge"
        createAction["BonusAward:iconClass"] == "abc"
        createAction["BonusAward:numMinutes"] == 60
        createAction.startDate
        createAction.endDate
        !createAction.description

        editAction.id == res.data[1].itemRefId
        editAction.name == badge.name
        editAction.skillId == badge.badgeId
        editAction.description == "this is a description allright"
        editAction["BonusAward:name"] == "Test Badge"
        editAction["BonusAward:iconClass"] == "abc"
        editAction["BonusAward:numMinutes"] == 60
        editAction.startDate
        editAction.endDate

        !deleteAction.id
    }

    def "track adding/removing skills to/from badge"() {
        SkillsService rootService = createRootSkillService()
        def p1 = createProject(1)
        skillsService.createProject(p1)
        def subj = createSubject(1, 1)
        skillsService.createSubject(subj)
        def skill = createSkill(1, 1, )
        skillsService.createSkill(skill)

        def badge = createBadge(1, 1)
        skillsService.createBadge(badge)

        userActionsHistoryRepo.deleteAll()

        when:
        skillsService.assignSkillsToBadge(p1.projectId, badge.badgeId, [skill.skillId])
        Thread.sleep(0)
        skillsService.removeSkillFromBadge([projectId: p1.projectId, badgeId: badge.badgeId, skillId: skill.skillId])

        def res = rootService.getUserActionsForEverything()

        def createAction = rootService.getUserActionAttributes(res.data[1].id)
        def deleteAction = rootService.getUserActionAttributes(res.data[0].id)
        String displayName = getDisplayName()
        then:
        res.count == 2
        res.data[0].action == DashboardAction.RemoveSkillAssignment.toString()
        res.data[0].item == DashboardItem.Badge.toString()
        res.data[0].itemId == badge.badgeId
        res.data[0].userId == skillsService.userName
        res.data[0].userIdForDisplay == displayName
        res.data[0].projectId == p1.projectId
        !res.data[0].quizId

        res.data[1].action == DashboardAction.AssignSkill.toString()
        res.data[1].item == DashboardItem.Badge.toString()
        res.data[1].itemId == badge.badgeId
        res.data[1].userId == skillsService.userName
        res.data[1].userIdForDisplay == displayName
        res.data[1].projectId == p1.projectId
        !res.data[1].quizId

        createAction.id == res.data[1].itemRefId
        createAction.badgeId == badge.badgeId
        createAction.skillId == skill.skillId

        deleteAction.id == res.data[0].itemRefId
        deleteAction.badgeId == badge.badgeId
        deleteAction.skillId == skill.skillId
    }

    def "track adding/removing single skill to/from badge"() {
        SkillsService rootService = createRootSkillService()
        def p1 = createProject(1)
        skillsService.createProject(p1)
        def subj = createSubject(1, 1)
        skillsService.createSubject(subj)
        def skill = createSkill(1, 1, )
        skillsService.createSkill(skill)

        def badge = createBadge(1, 1)
        skillsService.createBadge(badge)

        userActionsHistoryRepo.deleteAll()

        when:
        skillsService.assignSkillToBadge(p1.projectId, badge.badgeId, skill.skillId)
        Thread.sleep(0)
        skillsService.removeSkillFromBadge([projectId: p1.projectId, badgeId: badge.badgeId, skillId: skill.skillId])

        def res = rootService.getUserActionsForEverything()

        def createAction = rootService.getUserActionAttributes(res.data[1].id)
        def deleteAction = rootService.getUserActionAttributes(res.data[0].id)
        String displayName = getDisplayName()
        then:
        res.count == 2
        res.data[0].action == DashboardAction.RemoveSkillAssignment.toString()
        res.data[0].item == DashboardItem.Badge.toString()
        res.data[0].itemId == badge.badgeId
        res.data[0].userId == skillsService.userName
        res.data[0].userIdForDisplay == displayName
        res.data[0].projectId == p1.projectId
        !res.data[0].quizId

        res.data[1].action == DashboardAction.AssignSkill.toString()
        res.data[1].item == DashboardItem.Badge.toString()
        res.data[1].itemId == badge.badgeId
        res.data[1].userId == skillsService.userName
        res.data[1].userIdForDisplay == displayName
        res.data[1].projectId == p1.projectId
        !res.data[1].quizId

        createAction.id == res.data[1].itemRefId
        createAction.badgeId == badge.badgeId
        createAction.skillId == skill.skillId

        deleteAction.id == res.data[0].itemRefId
        deleteAction.badgeId == badge.badgeId
        deleteAction.skillId == skill.skillId
    }

    def "track project level CRUD actions"() {
        SkillsService rootService = createRootSkillService()
        def p1 = createProject(1)
        skillsService.createProject(p1)

        skillsService.addLevel(p1.projectId, null, [percent: 98])
        Thread.sleep(100)

        def p1Levels = skillsService.getLevels(p1.projectId).sort() { it.level }
        p1Levels[0].percent = 9
        skillsService.editLevel(p1.projectId, null, "1", p1Levels[0])

        Thread.sleep(100)
        skillsService.deleteLevel(p1.projectId)

        when:
        def res = rootService.getUserActionsForEverything(10, 1, "created", false, "", DashboardItem.Level)

        def createAction = rootService.getUserActionAttributes(res.data[2].id)
        def editAction = rootService.getUserActionAttributes(res.data[1].id)
        def deleteAction = rootService.getUserActionAttributes(res.data[0].id)

        String displayName = getDisplayName()
        then:
        res.count == 3
        res.data[0].action == DashboardAction.Delete.toString()
        res.data[0].item == DashboardItem.Level.toString()
        res.data[0].itemId == p1.projectId
        res.data[0].userId == skillsService.userName
        res.data[0].userIdForDisplay == displayName
        res.data[0].projectId == p1.projectId
        !res.data[0].quizId

        res.data[1].action == DashboardAction.Edit.toString()
        res.data[1].item == DashboardItem.Level.toString()
        res.data[1].itemId == p1.projectId
        res.data[1].userId == skillsService.userName
        res.data[1].userIdForDisplay == displayName
        res.data[1].projectId == p1.projectId
        !res.data[1].quizId

        res.data[2].action == DashboardAction.Create.toString()
        res.data[2].item == DashboardItem.Level.toString()
        res.data[2].itemId == p1.projectId
        res.data[2].userId == skillsService.userName
        res.data[2].userIdForDisplay == displayName
        res.data[2].projectId == p1.projectId
        !res.data[2].quizId

        createAction.id == res.data[2].itemRefId
        createAction.level == 6
        createAction.percent == 98
        !createAction.skillRefId

        editAction.id == res.data[1].itemRefId
        editAction.level == 1
        editAction.percent == 9
        !editAction.skillRefId

        deleteAction.id
        deleteAction.level
        !deleteAction.skillRefId
    }

    def "track subject level CRUD actions"() {
        SkillsService rootService = createRootSkillService()
        def p1 = createProject(1)
        skillsService.createProject(p1)
        def subj = createSubject(1, 1)
        skillsService.createSubject(subj)

        skillsService.addLevel(p1.projectId, subj.subjectId, [percent: 98])
        Thread.sleep(100)

        def p1Levels = skillsService.getLevels(p1.projectId).sort() { it.level }
        p1Levels[0].percent = 9
        skillsService.editLevel(p1.projectId, subj.subjectId, "1", p1Levels[0])

        Thread.sleep(100)
        skillsService.deleteLevel(p1.projectId, subj.subjectId)

        when:
        def res = rootService.getUserActionsForEverything(10, 1, "created", false, "", DashboardItem.Level)

        def createAction = rootService.getUserActionAttributes(res.data[2].id)
        def editAction = rootService.getUserActionAttributes(res.data[1].id)
        def deleteAction = rootService.getUserActionAttributes(res.data[0].id)
        String displayName = getDisplayName()
        then:
        res.count == 3
        res.data[0].action == DashboardAction.Delete.toString()
        res.data[0].item == DashboardItem.Level.toString()
        res.data[0].itemId == subj.subjectId
        res.data[0].userId == skillsService.userName
        res.data[0].userIdForDisplay == displayName
        res.data[0].projectId == p1.projectId
        !res.data[0].quizId

        res.data[1].action == DashboardAction.Edit.toString()
        res.data[1].item == DashboardItem.Level.toString()
        res.data[1].itemId == subj.subjectId
        res.data[1].userId == skillsService.userName
        res.data[1].userIdForDisplay == displayName
        res.data[1].projectId == p1.projectId
        !res.data[1].quizId

        res.data[2].action == DashboardAction.Create.toString()
        res.data[2].item == DashboardItem.Level.toString()
        res.data[2].itemId == subj.subjectId
        res.data[2].userId == skillsService.userName
        res.data[2].userIdForDisplay == displayName
        res.data[2].projectId == p1.projectId
        !res.data[2].quizId

        createAction.id == res.data[2].itemRefId
        createAction.level == 6
        createAction.percent == 98
        createAction.skillRefId

        editAction.id == res.data[1].itemRefId
        editAction.level == 1
        editAction.percent == 9
        editAction.skillRefId

        deleteAction.id
        deleteAction.level
        deleteAction.skillRefId
    }

    def "learning path CRUD actions"() {
        SkillsService rootService = createRootSkillService()

        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(5, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        userActionsHistoryRepo.deleteAll()
        when:
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills.get(0).skillId, p1Skills.get(1).skillId)
        Thread.sleep(100)
        skillsService.deleteLearningPathPrerequisite(p1.projectId, p1Skills.get(0).skillId, p1Skills.get(1).skillId)

        def res = rootService.getUserActionsForEverything(10, 1, "created", false, "", DashboardItem.LearningPathItem)

        def createAction = rootService.getUserActionAttributes(res.data[1].id)
        def deleteAction = rootService.getUserActionAttributes(res.data[0].id)
        String displayName = getDisplayName()
        then:
        res.count == 2
        res.data[0].action == DashboardAction.Delete.toString()
        res.data[0].item == DashboardItem.LearningPathItem.toString()
        res.data[0].itemId == p1.projectId
        res.data[0].userId == skillsService.userName
        res.data[0].userIdForDisplay == displayName
        res.data[0].projectId == p1.projectId
        !res.data[0].quizId

        res.data[1].action == DashboardAction.Create.toString()
        res.data[1].item == DashboardItem.LearningPathItem.toString()
        res.data[1].itemId == p1.projectId
        res.data[1].userId == skillsService.userName
        res.data[1].userIdForDisplay == displayName
        res.data[1].projectId == p1.projectId
        !res.data[1].quizId

        createAction.fromProjectId == p1.projectId
        createAction.fromSkillId == p1Skills.get(1).skillId
        createAction.toProjectId == p1.projectId
        createAction.toSkillId == p1Skills.get(0).skillId

        deleteAction.fromProjectId == p1.projectId
        deleteAction.fromSkillId == p1Skills.get(1).skillId
        deleteAction.toProjectId == p1.projectId
        deleteAction.toSkillId == p1Skills.get(0).skillId
    }

    def "manage approver workload CRUD actions"() {
        SkillsService rootService = createRootSkillService()

        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

        List<String> users = getRandomUsers(5, true)
        def user1Service = createService(users[0])
        skillsService.addUserRole(user1Service.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())
        def user2Service = createService(users[1])
        skillsService.addUserRole(user2Service.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())
        def user3Service = createService(users[3])
        skillsService.addUserRole(user3Service.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())
        def user4Service = createService(users[4])
        skillsService.addUserRole(user4Service.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())

        String userTagKey = "KeY1"
        rootService.saveUserTag(users[2], userTagKey, ["aBcD"])
        rootService.saveUserTag(users[3], userTagKey, ["EfGh"])

        userActionsHistoryRepo.deleteAll()

        createService(users[2]).getProjects() // initialize UserAttrs records for user2 for pki
        when:

        // should be DN in case of pki
        String userIdConConf = System.getenv("SPRING_PROFILES_ACTIVE") == 'pki' ? userAttrsRepo.findByUserIdIgnoreCase(users[2]).dn : users[2]
        skillsService.configureApproverForUser(proj.projectId, user1Service.userName, userIdConConf)
        Thread.sleep(100)
        skillsService.configureApproverForSkillId(proj.projectId, user2Service.userName, skills[0].skillId)
        Thread.sleep(100)
        skillsService.configureApproverForUserTag(proj.projectId, user3Service.userName, userTagKey.toLowerCase(), "AbC")
        Thread.sleep(100)
        skillsService.configureFallbackApprover(proj.projectId, user4Service.userName)
        Thread.sleep(100)
        def approverConf = skillsService.getApproverConf(proj.projectId).find { it.approverUserId == user1Service.userName}

        skillsService.deleteApproverConf(proj.projectId, approverConf.id)

        def res = rootService.getUserActionsForEverything(10, 1, "created", false, "", DashboardItem.Approver)

        def confByUser = rootService.getUserActionAttributes(res.data[4].id)
        def confBySkill = rootService.getUserActionAttributes(res.data[3].id)
        def confByTag = rootService.getUserActionAttributes(res.data[2].id)
        def confFallback = rootService.getUserActionAttributes(res.data[1].id)
        def deleteApproverConf = rootService.getUserActionAttributes(res.data[0].id)
        String displayName = getDisplayName()
        then:
        res.count == 5
        res.data[0].action == DashboardAction.RemoveConfiguration.toString()
        res.data[0].item == DashboardItem.Approver.toString()
        res.data[0].itemId == userAttrsRepo.findByUserIdIgnoreCase(user1Service.userName).userIdForDisplay
        res.data[0].userId == skillsService.userName
        res.data[0].userIdForDisplay == displayName
        res.data[0].projectId == proj.projectId
        !res.data[0].quizId

        res.data[1].action == DashboardAction.Configure.toString()
        res.data[1].item == DashboardItem.Approver.toString()
        res.data[1].itemId == userAttrsRepo.findByUserIdIgnoreCase(user4Service.userName).userIdForDisplay
        res.data[1].userId == skillsService.userName
        res.data[1].userIdForDisplay == displayName
        res.data[1].projectId == proj.projectId
        !res.data[1].quizId

        res.data[2].action == DashboardAction.Configure.toString()
        res.data[2].item == DashboardItem.Approver.toString()
        res.data[2].itemId == userAttrsRepo.findByUserIdIgnoreCase(user3Service.userName).userIdForDisplay
        res.data[2].userId == skillsService.userName
        res.data[2].userIdForDisplay == displayName
        res.data[2].projectId == proj.projectId
        !res.data[2].quizId

        res.data[3].action == DashboardAction.Configure.toString()
        res.data[3].item == DashboardItem.Approver.toString()
        res.data[3].itemId == userAttrsRepo.findByUserIdIgnoreCase(user2Service.userName).userIdForDisplay
        res.data[3].userId == skillsService.userName
        res.data[3].userIdForDisplay == displayName
        res.data[3].projectId == proj.projectId
        !res.data[3].quizId

        res.data[4].action == DashboardAction.Configure.toString()
        res.data[4].item == DashboardItem.Approver.toString()
        res.data[4].itemId == userAttrsRepo.findByUserIdIgnoreCase(user1Service.userName).userIdForDisplay
        res.data[4].userId == skillsService.userName
        res.data[4].userIdForDisplay == displayName
        res.data[4].projectId == proj.projectId
        !res.data[4].quizId

        confByUser.userId == userIdConConf
        !confByUser.skillId
        !confByUser.userTagKey
        !confByUser.userTagValue

        !confBySkill.userId
        confBySkill.skillId == skills[0].skillId
        !confBySkill.userTagKey
        !confBySkill.userTagValue

        !confByTag.userId
        !confByTag.skillId
        confByTag.userTagKey == userTagKey.toLowerCase()
        confByTag.userTagValue ==  "AbC"

        confFallback.fallbackApprover == true

        deleteApproverConf.approverUserId == user1Service.userName
    }

    def "delete user skill events"() {
        SkillsService rootService = createRootSkillService()

        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(2, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

        def user = getRandomUsers(1)[0]
        Date skillDate = new Date()
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], user, skillDate)
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[1].skillId], user, skillDate)

        userActionsHistoryRepo.deleteAll()
        when:
        skillsService.deleteSkillEvent([projectId: proj.projectId, skillId: skills[0].skillId, userId: user, timestamp: skillDate.time])
        Thread.sleep(100)
        skillsService.deleteAllSkillEvents([projectId: proj.projectId, userId: user])

        def res = rootService.getUserActionsForEverything()

        def deleteAllSkillEvents = rootService.getUserActionAttributes(res.data[0].id)
        def deleteSingleEvent = rootService.getUserActionAttributes(res.data[1].id)
        String displayName = getDisplayName()
        then:
        res.count == 2
        res.data[0].action == DashboardAction.Delete.toString()
        res.data[0].item == DashboardItem.SkillEvents.toString()
        res.data[0].itemId == userAttrsRepo.findByUserIdIgnoreCase(user).userIdForDisplay
        res.data[0].userId == skillsService.userName
        res.data[0].userIdForDisplay == displayName
        res.data[0].projectId == proj.projectId
        !res.data[0].quizId

        res.data[1].action == DashboardAction.Delete.toString()
        res.data[1].item == DashboardItem.SkillEvents.toString()
        res.data[1].itemId == userAttrsRepo.findByUserIdIgnoreCase(user).userIdForDisplay
        res.data[1].userId == skillsService.userName
        res.data[1].userIdForDisplay == displayName
        res.data[1].projectId == proj.projectId
        !res.data[1].quizId

        deleteAllSkillEvents.userId == userAttrsRepo.findByUserIdIgnoreCase(user).userIdForDisplay
        deleteAllSkillEvents.deletedAllForThisUser == true

        deleteSingleEvent.userId == userAttrsRepo.findByUserIdIgnoreCase(user).userIdForDisplay
        deleteSingleEvent.deletedAllForThisUser == false
        deleteSingleEvent.timeOfSkillEvent == skillDate.time
    }

    def "project admin CRUD"() {
        SkillsService rootService = createRootSkillService()

        def proj = SkillsFactory.createProject()
        skillsService.createProject(proj)

        def user = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        SkillsService otherUser = createService(user)

        userActionsHistoryRepo.deleteAll()
        when:
        skillsService.addProjectAdmin(proj.projectId, otherUser.userName)
        Thread.sleep(100)
        skillsService.deleteUserRole(otherUser.userName, proj.projectId, RoleName.ROLE_PROJECT_ADMIN.toString())

        def res = rootService.getUserActionsForEverything()

        def deleteAction = rootService.getUserActionAttributes(res.data[0].id)
        def createAction = rootService.getUserActionAttributes(res.data[1].id)
        String displayName = getDisplayName()
        then:
        res.count == 2
        res.data[0].action == DashboardAction.Delete.toString()
        res.data[0].item == DashboardItem.UserRole.toString()
        res.data[0].itemId == userAttrsRepo.findByUserIdIgnoreCase(user).userIdForDisplay
        res.data[0].userId == skillsService.userName
        res.data[0].userIdForDisplay == displayName
        res.data[0].projectId == proj.projectId
        !res.data[0].quizId

        res.data[1].action == DashboardAction.Create.toString()
        res.data[1].item == DashboardItem.UserRole.toString()
        res.data[1].itemId == userAttrsRepo.findByUserIdIgnoreCase(user).userIdForDisplay
        res.data[1].userId == skillsService.userName
        res.data[1].userIdForDisplay == displayName
        res.data[1].projectId == proj.projectId
        !res.data[1].quizId

        deleteAction.userId == userAttrsRepo.findByUserIdIgnoreCase(user).userIdForDisplay
        deleteAction.userRole == RoleName.ROLE_PROJECT_ADMIN.toString()

        createAction.userId == userAttrsRepo.findByUserIdIgnoreCase(user).userIdForDisplay
        createAction.userRole == RoleName.ROLE_PROJECT_ADMIN.toString()
    }

    def "project approver CRUD"() {
        SkillsService rootService = createRootSkillService()

        def proj = SkillsFactory.createProject()
        skillsService.createProject(proj)

        def user = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        SkillsService otherUser = createService(user)

        userActionsHistoryRepo.deleteAll()
        when:
        skillsService.addUserRole(otherUser.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())
        Thread.sleep(100)
        skillsService.deleteUserRole(otherUser.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())

        def res = rootService.getUserActionsForEverything()

        def deleteAction = rootService.getUserActionAttributes(res.data[0].id)
        def createAction = rootService.getUserActionAttributes(res.data[1].id)
        String displayName = getDisplayName()
        then:
        res.count == 2
        res.data[0].action == DashboardAction.Delete.toString()
        res.data[0].item == DashboardItem.UserRole.toString()
        res.data[0].itemId == userAttrsRepo.findByUserIdIgnoreCase(user).userIdForDisplay
        res.data[0].userId == skillsService.userName
        res.data[0].userIdForDisplay == displayName
        res.data[0].projectId == proj.projectId
        !res.data[0].quizId

        res.data[1].action == DashboardAction.Create.toString()
        res.data[1].item == DashboardItem.UserRole.toString()
        res.data[1].itemId == userAttrsRepo.findByUserIdIgnoreCase(user).userIdForDisplay
        res.data[1].userId == skillsService.userName
        res.data[1].userIdForDisplay == displayName
        res.data[1].projectId == proj.projectId
        !res.data[1].quizId

        deleteAction.userId == userAttrsRepo.findByUserIdIgnoreCase(user).userIdForDisplay
        deleteAction.userRole == RoleName.ROLE_PROJECT_APPROVER.toString()

        createAction.userId == userAttrsRepo.findByUserIdIgnoreCase(user).userIdForDisplay
        createAction.userRole == RoleName.ROLE_PROJECT_APPROVER.toString()
    }

    def "project settings CRUD"() {
        SkillsService rootService = createRootSkillService()

        def proj = SkillsFactory.createProject()
        skillsService.createProject(proj)

        userActionsHistoryRepo.deleteAll()
        when:
        skillsService.changeSettings(proj.projectId, [
                [projectId: proj.projectId, setting: "set1", value: "true"],
                [projectId: proj.projectId, setting: "set2", value: "val2"],
        ])
        Thread.sleep(100)
        skillsService.addOrUpdateProjectSetting(proj.projectId, 'set3', 'val3') // single setting
        Thread.sleep(100)
        skillsService.addOrUpdateProjectSetting(proj.projectId, 'set1', '') // delete setting

        def res = rootService.getUserActionsForEverything()

        def createAction1 = rootService.getUserActionAttributes(res.data[3].id)
        def createAction2 = rootService.getUserActionAttributes(res.data[2].id)
        def createAction3 = rootService.getUserActionAttributes(res.data[1].id)
        def deleteAction = rootService.getUserActionAttributes(res.data[0].id)
        String displayName = getDisplayName()
        then:
        res.count == 4
        res.data[0].action == DashboardAction.Delete.toString()
        res.data[0].item == DashboardItem.Settings.toString()
        res.data[0].itemId == "Project"
        res.data[0].userId == skillsService.userName
        res.data[0].userIdForDisplay == displayName
        res.data[0].projectId == proj.projectId
        !res.data[0].quizId

        res.data[1].action == DashboardAction.Create.toString()
        res.data[1].item == DashboardItem.Settings.toString()
        res.data[1].itemId == "Project"
        res.data[1].userId == skillsService.userName
        res.data[1].userIdForDisplay == displayName
        res.data[1].projectId == proj.projectId
        !res.data[1].quizId

        res.data[2].action == DashboardAction.Create.toString()
        res.data[2].item == DashboardItem.Settings.toString()
        res.data[2].itemId == "Project"
        res.data[2].userId == skillsService.userName
        res.data[2].userIdForDisplay == displayName
        res.data[2].projectId == proj.projectId
        !res.data[2].quizId

        res.data[3].action == DashboardAction.Create.toString()
        res.data[3].item == DashboardItem.Settings.toString()
        res.data[3].itemId == "Project"
        res.data[3].userId == skillsService.userName
        res.data[3].userIdForDisplay == displayName
        res.data[3].projectId == proj.projectId
        !res.data[3].quizId

        createAction1.setting == "set1"
        createAction1.value == "true"

        createAction2.setting == "set2"
        createAction2.value == "val2"

        createAction3.setting == "set3"
        createAction3.value == "val3"

        deleteAction.setting == "set1"
    }

    def "DO NOT track when admin reports skill events for another user"() {
        SkillsService rootService = createRootSkillService()

        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(2, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

        userActionsHistoryRepo.deleteAll()
        
        def user = getRandomUsers(1)[0]
        Date skillDate = new Date()
        when:
        def resReportForAnother = skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], user, skillDate)
        // this one must not produce an event
        def resReportForMe = skillsService.addSkill([projectId: proj.projectId, skillId: skills[1].skillId])

        def res = rootService.getUserActionsForEverything()
        then:
        resReportForMe.body.pointsEarned == 100
        resReportForAnother.body.pointsEarned == 100

        res.count == 0
    }

    def "project issues"() {
        SkillsService rootService = createRootSkillService()

        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(2, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

        userActionsHistoryRepo.deleteAll()

        try {
            skillsService.addSkill([projectId: proj.projectId, skillId: "this is not a skill id"])
        } catch (SkillsClientException skillsClientException) {
            //expected to fail
        }
        def errorsBeforeDelete = skillsService.getProjectErrors(proj.projectId, 10, 1, "lastSeen", false)

        when:
        skillsService.deleteSpecificProjectError(proj.projectId, errorsBeforeDelete.data[0].errorId)
        skillsService.deleteAllProjectErrors(proj.projectId)

        def res = rootService.getUserActionsForEverything()
        def deleteAll = rootService.getUserActionAttributes(res.data[0].id)
        def deleteOne = rootService.getUserActionAttributes(res.data[1].id)
        String displayName = getDisplayName()
        then:
        res.count == 2
        res.data[0].action == DashboardAction.Delete.toString()
        res.data[0].item == DashboardItem.ProjectIssue.toString()
        res.data[0].itemId == proj.projectId
        res.data[0].userId == skillsService.userName
        res.data[0].userIdForDisplay == displayName
        res.data[0].projectId == proj.projectId
        !res.data[0].quizId

        res.data[1].action == DashboardAction.Delete.toString()
        res.data[1].item == DashboardItem.ProjectIssue.toString()
        res.data[1].itemId == proj.projectId
        res.data[1].userId == skillsService.userName
        res.data[1].userIdForDisplay == displayName
        res.data[1].projectId == proj.projectId
        !res.data[1].quizId

        deleteOne.errorType == ProjectError.ErrorType.SkillNotFound.toString()
        deleteOne.error == "this is not a skill id"

        deleteAll.allIssues == true
        !deleteAll.errorType

    }

    def "reset trusted client secret"() {
        SkillsService rootService = createRootSkillService()

        def proj = SkillsFactory.createProject()
        skillsService.createProject(proj)

        userActionsHistoryRepo.deleteAll()
        String displayName = getDisplayName()
        when:
        skillsService.resetClientSecret(proj.projectId)
        def res = rootService.getUserActionsForEverything()
        then:
        res.count == 1
        res.data[0].action == DashboardAction.Create.toString()
        res.data[0].item == DashboardItem.TrustedClientSecret.toString()
        res.data[0].itemId == proj.projectId
        res.data[0].userId == skillsService.userName
        res.data[0].userIdForDisplay == displayName
        res.data[0].projectId == proj.projectId
        !res.data[0].quizId
    }

    def "private project invite management"() {
        startEmailServer()
        SkillsService rootService = createRootSkillService()

        def proj = SkillsFactory.createProject()
        skillsService.createProject(proj)

        skillsService.changeSetting(proj.projectId, Settings.INVITE_ONLY_PROJECT.settingName, [projectId: proj.projectId, setting: Settings.INVITE_ONLY_PROJECT.settingName, value: Boolean.TRUE.toString()])
        userActionsHistoryRepo.deleteAll()

        when:
        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT5M", recipients: ["someemail@email.foo"]])
        skillsService.extendProjectInviteExpiration(proj.projectId, "someemail@email.foo", "PT1H")
        skillsService.remindUserOfPendingInvite(proj.projectId, "someemail@email.foo")
        skillsService.deletePendingInvite(proj.projectId, "someemail@email.foo")
        def res = rootService.getUserActionsForEverything()
        def delete = rootService.getUserActionAttributes(res.data[0].id)
        def remind = rootService.getUserActionAttributes(res.data[1].id)
        def extend = rootService.getUserActionAttributes(res.data[2].id)
        def invite = rootService.getUserActionAttributes(res.data[3].id)
        String displayName = getDisplayName()
        then:
        res.count == 4
        res.data[0].action == DashboardAction.Delete.toString()
        res.data[0].item == DashboardItem.ProjectInvite.toString()
        res.data[0].itemId == "someemail@email.foo"
        res.data[0].userId == skillsService.userName
        res.data[0].userIdForDisplay == displayName
        res.data[0].projectId == proj.projectId
        !res.data[0].quizId

        res.data[1].action == DashboardAction.Remind.toString()
        res.data[1].item == DashboardItem.ProjectInvite.toString()
        res.data[1].itemId == "someemail@email.foo"
        res.data[1].userId == skillsService.userName
        res.data[1].userIdForDisplay == displayName
        res.data[1].projectId == proj.projectId
        !res.data[1].quizId

        res.data[2].action == DashboardAction.Extend.toString()
        res.data[2].item == DashboardItem.ProjectInvite.toString()
        res.data[2].itemId == "someemail@email.foo"
        res.data[2].userId == skillsService.userName
        res.data[2].userIdForDisplay == displayName
        res.data[2].projectId == proj.projectId
        !res.data[2].quizId

        res.data[3].action == DashboardAction.Create.toString()
        res.data[3].item == DashboardItem.ProjectInvite.toString()
        res.data[3].itemId == proj.projectId
        res.data[3].userId == skillsService.userName
        res.data[3].userIdForDisplay == displayName
        res.data[3].projectId == proj.projectId
        !res.data[3].quizId

        invite.emailAddresses == [ "someemail@email.foo"]
        invite.duration == "PT5M"

        extend.duration == "PT1H"
    }

    def "two projects where one project's id is a substring of the other"() {
        def p1 = createProject(1)
        p1.projectId = "proj"
        def p1subj1 = createSubject(1, 1)
        p1subj1.projectId = p1.projectId
        def p1Skills = createSkills(1, 1, 1, 100)
        p1Skills[0].projectId = p1.projectId
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def p2 = createProject(2)
        p2.projectId = "${p1.projectId}more".toString()
        def p2subj1 = createSubject(2, 2)
        p2subj1.projectId = p2.projectId
        def p2Skills = createSkills(1, 2, 2, 100)
        p2Skills[0].projectId = p2.projectId
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, p2Skills)

        when:
        def origProj = skillsService.getUserActionsForProject(p1.projectId, 10, 1, "projectId", true)
        def copyProj = skillsService.getUserActionsForProject(p2.projectId, 10, 1, "projectId", true)
        then:
        origProj.data.itemId.sort() == [p1.projectId, p1subj1.subjectId, p1Skills[0].skillId].sort()

        copyProj.data.itemId.sort() == [p2.projectId, p2subj1.subjectId, p2Skills[0].skillId].sort()
    }

    def "track archive/restore user actions"() {
        SkillsService rootService = createRootSkillService()
        String displayName = getDisplayName()
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(2, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
        def user = getRandomUsers(1)[0]
        Date skillDate = new Date()
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], user, skillDate)
        userActionsHistoryRepo.deleteAll()

        when:
        skillsService.archiveUsers([user], proj.projectId)
        Thread.sleep(100)
        skillsService.restoreArchivedUser(user, proj.projectId)
        def res = rootService.getUserActionsForEverything()

        then:
        res.count == 2
        res.data[0].action == DashboardAction.RestoreArchivedUser.toString()
        res.data[0].item == DashboardItem.Project.toString()
        res.data[0].itemId == user
        res.data[0].userId == skillsService.userName
        res.data[0].userIdForDisplay == displayName
        res.data[0].projectId == proj.projectId
        !res.data[0].quizId

        res.data[1].action == DashboardAction.ArchiveUser.toString()
        res.data[1].item == DashboardItem.Project.toString()
        res.data[1].itemId == user
        res.data[1].userId == skillsService.userName
        res.data[1].userIdForDisplay == displayName
        res.data[1].projectId == proj.projectId
        !res.data[1].quizId
    }
}

