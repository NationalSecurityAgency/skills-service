/**
 * Copyright 2024 SkillTree
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
package skills.intTests.export

import skills.intTests.utils.SkillsService
import skills.services.attributes.ExpirationAttrs
import skills.storage.model.SkillDef

import java.time.LocalDateTime

import static skills.intTests.utils.SkillsFactory.*

class ExportSubjectSkillsSpec extends ExportBaseIntSpec {

    def "export subject skills"() {

        def proj1 = createProject(1)
        def proj2 = createProject(2)
        def proj1_subj = createSubject(1, 1)
        def proj2_subj = createSubject(2, 2)
        List<Map> proj1_skills = createSkills(5, 1, 1, 50)
        def group = createSkillsGroup(1, 1, 10)
        List<Map> proj2_skills_subj2 = createSkills(3, 2, 2, 50)

        proj1_skills[0].selfReportingType = SkillDef.SelfReportingType.Approval
        proj1_skills[1].selfReportingType = SkillDef.SelfReportingType.Approval
        proj1_skills[1].numPerformToCompletion = 2
        proj1_skills[1].pointIncrementInterval = 487
        proj1_skills[1].version = 1
        proj1_skills[2].selfReportingType = SkillDef.SelfReportingType.HonorSystem

        skillsService.createProject(proj1)
        skillsService.createProject(proj2)
        skillsService.createSubject(proj1_subj)
        skillsService.createSubject(proj2_subj)
        skillsService.createSkills([proj1_skills[0..2], group].flatten())
        skillsService.assignSkillToSkillsGroup(group.skillId, proj1_skills[3])
        skillsService.assignSkillToSkillsGroup(group.skillId, proj1_skills[4])
        skillsService.createSkills(proj2_skills_subj2)

        List<String> skillIds = proj1_skills.collect {it.skillId}
        String tagValue = "New Tag"
        skillsService.addTagToSkills(proj1.projectId, skillIds, tagValue)


        skillsService.exportSkillToCatalog(proj1.projectId, proj1_skills[0].skillId)
        skillsService.exportSkillToCatalog(proj2.projectId, proj2_skills_subj2[0].skillId)

        skillsService.importSkillFromCatalog(proj1.projectId, proj1_subj.subjectId, proj2.projectId, proj2_skills_subj2[0].skillId)

        LocalDateTime expirationDate = (new Date() - 1).toLocalDateTime() // yesterday
        skillsService.saveSkillExpirationAttributes(proj1.projectId, proj1_skills[1].skillId, [
                expirationType: ExpirationAttrs.YEARLY,
                every: 1,
                monthlyDay: expirationDate.dayOfMonth,
                nextExpirationDate: expirationDate.toDate(),
        ])

        when:
        def excelExport = skillsService.getSkillsForSubjectExport(proj1.projectId, proj1_subj.subjectId)

        then:
        validateExport(excelExport.file, [
                ["For All Dragons Only"],
                ["Skill Name", "Skill ID", "Group Name", "Tags", "Date Created (UTC)", "Total Points", "Point Increment", "Repetitions", "Self Report", "Catalog", "Expiration", "Time Window", "Version"],
                ["Test Skill 1", "skill1", "", "New Tag",  today.format("dd-MMM-yyyy"), "50.0", "50.0", "1.0", "Approval", "Exported", "", "", "0.0"],
                ["Test Skill 2", "skill2", "", "New Tag",  today.format("dd-MMM-yyyy"), "100.0", "50.0", "2.0", "Approval", "", "YEARLY", "8 Hours 7 Minutes", "1.0"],
                ["Test Skill 3", "skill3", "", "New Tag",  today.format("dd-MMM-yyyy"), "50.0", "50.0", "1.0", "Honor System", "", "", "", "0.0"],
                ["Test Skill 10", "skill10", "", "",  today.format("dd-MMM-yyyy"), "100.0", "0.0", "1.0", "", "", "", "", "0.0"],
                ["Test Skill 4", "skill4", "Test Skill 10", "New Tag",  today.format("dd-MMM-yyyy"), "50.0", "50.0", "1.0", "", "", "", "", "0.0"],
                ["Test Skill 5", "skill5", "Test Skill 10", "New Tag",  today.format("dd-MMM-yyyy"), "50.0", "50.0", "1.0", "", "", "", "", "0.0"],
                ["Test Skill 1 Subject2", "skill1subj2", "", "",  today.format("dd-MMM-yyyy"), "50.0", "50.0", "1.0", "", "Imported", "", "", "1.0"],
                ["For All Dragons Only"],
        ])
    }

    def "export project skill metrics for UC protected project"() {
        def users = getRandomUsers(10)

        SkillsService pristineDragonsUser = createService(users[2])
        rootSkillsService.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])
        rootSkillsService.saveUserTag(rootSkillsService.userName, 'dragons', ['DivineDragon'])

        def proj1 = createProject(1)
        proj1.enableProtectedUserCommunity = true
        def proj2 = createProject(2)
        def proj1_subj = createSubject(1, 1)
        def proj2_subj = createSubject(2, 2)
        List<Map> proj1_skills = createSkills(5, 1, 1, 50)
        def group = createSkillsGroup(1, 1, 10)
        List<Map> proj2_skills_subj2 = createSkills(3, 2, 2, 50)

        proj1_skills[0].selfReportingType = SkillDef.SelfReportingType.Approval
        proj1_skills[1].selfReportingType = SkillDef.SelfReportingType.Approval
        proj1_skills[1].numPerformToCompletion = 2
        proj1_skills[1].pointIncrementInterval = 487
        proj1_skills[1].version = 1
        proj1_skills[2].selfReportingType = SkillDef.SelfReportingType.HonorSystem

        pristineDragonsUser.createProject(proj1)
        skillsService.createProject(proj2)
        pristineDragonsUser.createSubject(proj1_subj)
        skillsService.createSubject(proj2_subj)
        pristineDragonsUser.createSkills([proj1_skills[0..2], group].flatten())
        pristineDragonsUser.assignSkillToSkillsGroup(group.skillId, proj1_skills[3])
        pristineDragonsUser.assignSkillToSkillsGroup(group.skillId, proj1_skills[4])
        skillsService.createSkills(proj2_skills_subj2)

        List<String> skillIds = proj1_skills.collect {it.skillId}
        String tagValue = "New Tag"
        pristineDragonsUser.addTagToSkills(proj1.projectId, skillIds, tagValue)

        skillsService.exportSkillToCatalog(proj2.projectId, proj2_skills_subj2[0].skillId)
        pristineDragonsUser.importSkillFromCatalog(proj1.projectId, proj1_subj.subjectId, proj2.projectId, proj2_skills_subj2[0].skillId)

        LocalDateTime expirationDate = (new Date() - 1).toLocalDateTime() // yesterday
        pristineDragonsUser.saveSkillExpirationAttributes(proj1.projectId, proj1_skills[1].skillId, [
                expirationType: ExpirationAttrs.YEARLY,
                every: 1,
                monthlyDay: expirationDate.dayOfMonth,
                nextExpirationDate: expirationDate.toDate(),
        ])

        when:
        def excelExport = pristineDragonsUser.getSkillsForSubjectExport(proj1.projectId, proj1_subj.subjectId)

        then:
        validateExport(excelExport.file, [
                ["For Divine Dragon Only"],
                ["Skill Name", "Skill ID", "Group Name", "Tags", "Date Created (UTC)", "Total Points", "Point Increment", "Repetitions", "Self Report", "Catalog", "Expiration", "Time Window", "Version"],
                ["Test Skill 1", "skill1", "", "New Tag",  today.format("dd-MMM-yyyy"), "50.0", "50.0", "1.0", "Approval", "", "", "", "0.0"],
                ["Test Skill 2", "skill2", "", "New Tag",  today.format("dd-MMM-yyyy"), "100.0", "50.0", "2.0", "Approval", "", "YEARLY", "8 Hours 7 Minutes", "1.0"],
                ["Test Skill 3", "skill3", "", "New Tag",  today.format("dd-MMM-yyyy"), "50.0", "50.0", "1.0", "Honor System", "", "", "", "0.0"],
                ["Test Skill 10", "skill10", "", "",  today.format("dd-MMM-yyyy"), "100.0", "0.0", "1.0", "", "", "", "", "0.0"],
                ["Test Skill 4", "skill4", "Test Skill 10", "New Tag",  today.format("dd-MMM-yyyy"), "50.0", "50.0", "1.0", "", "", "", "", "0.0"],
                ["Test Skill 5", "skill5", "Test Skill 10", "New Tag",  today.format("dd-MMM-yyyy"), "50.0", "50.0", "1.0", "", "", "", "", "0.0"],
                ["Test Skill 1 Subject2", "skill1subj2", "", "",  today.format("dd-MMM-yyyy"), "50.0", "50.0", "1.0", "", "Imported", "", "", "1.0"],
                ["For Divine Dragon Only"],
        ])
    }
}
