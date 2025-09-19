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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import skills.intTests.utils.SkillsService
import skills.services.ExcelExportService
import skills.services.attributes.ExpirationAttrs
import skills.storage.model.SkillDef

import java.time.LocalDateTime

import static skills.intTests.utils.SkillsFactory.*

class ExportSubjectSkillsSpec extends ExportBaseIntSpec {

    @Autowired
    JdbcTemplate jdbcTemplate

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
        proj1_skills[1].numPerformToCompletion = 4
        proj1_skills[1].pointIncrementInterval = 487
        proj1_skills[1].numMaxOccurrencesIncrementInterval = 2
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

        Date yesterday = new Date() - 1
        Date twoDaysAgo = new Date() - 2
        jdbcTemplate.update("update skill_definition set created='${twoDaysAgo.format("yyyy-MM-dd HH:mm:ss")}', updated='${yesterday.format("yyyy-MM-dd HH:mm:ss")}' where project_id='${proj1.projectId}' and skill_id='${proj1_skills[1].skillId}'")

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
                ["Skill Name", "Skill ID", "Group Name", "Tags", "Date Created (UTC)", "Total Points", "Point Increment", "Repetitions", "Self Report", "Catalog", "Expiration", "Time Window", "Version", "Date Last Updated (UTC)"],
                ["Test Skill 1", "skill1", "", "New Tag",  formatDate(today), "50.0", "50.0", "1.0", "Approval", "Exported", "", "", "0.0", formatDate(today)],
                ["Test Skill 2", "skill2", "", "New Tag",  formatDate(twoDaysAgo), "200.0", "50.0", "4.0", "Approval", "", "Every year on ${expirationDate.format("MM/dd")}", "8 Hours 7 Minutes, Up to 2 Occurrences", "1.0", formatDate(yesterday)],
                ["Test Skill 3", "skill3", "", "New Tag",  formatDate(today), "50.0", "50.0", "1.0", "Honor System", "", "", "", "0.0", formatDate(today)],
                ["Test Skill 4", "skill4", "Test Skill 10", "New Tag",  formatDate(today), "50.0", "50.0", "1.0", "", "", "", "", "0.0", formatDate(today)],
                ["Test Skill 5", "skill5", "Test Skill 10", "New Tag",  formatDate(today), "50.0", "50.0", "1.0", "", "", "", "", "0.0", formatDate(today)],
                ["Test Skill 1 Subject2", "skill1subj2", "", "",  formatDate(today), "50.0", "50.0", "1.0", "", "Imported", "", "", "1.0", formatDate(today)],
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
        proj1_skills[1].numPerformToCompletion = 4
        proj1_skills[1].pointIncrementInterval = 487
        proj1_skills[1].numMaxOccurrencesIncrementInterval = 2
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
                ["Skill Name", "Skill ID", "Group Name", "Tags", "Date Created (UTC)", "Total Points", "Point Increment", "Repetitions", "Self Report", "Catalog", "Expiration", "Time Window", "Version", "Date Last Updated (UTC)"],
                ["Test Skill 1", "skill1", "", "New Tag",  formatDate(today), "50.0", "50.0", "1.0", "Approval", "", "", "", "0.0", formatDate(today)],
                ["Test Skill 2", "skill2", "", "New Tag",  formatDate(today), "200.0", "50.0", "4.0", "Approval", "", "Every year on ${expirationDate.format("MM/dd")}", "8 Hours 7 Minutes, Up to 2 Occurrences", "1.0", formatDate(today)],
                ["Test Skill 3", "skill3", "", "New Tag",  formatDate(today), "50.0", "50.0", "1.0", "Honor System", "", "", "", "0.0", formatDate(today)],
                ["Test Skill 4", "skill4", "Test Skill 10", "New Tag",  formatDate(today), "50.0", "50.0", "1.0", "", "", "", "", "0.0", formatDate(today)],
                ["Test Skill 5", "skill5", "Test Skill 10", "New Tag",  formatDate(today), "50.0", "50.0", "1.0", "", "", "", "", "0.0", formatDate(today)],
                ["Test Skill 1 Subject2", "skill1subj2", "", "",  formatDate(today), "50.0", "50.0", "1.0", "", "Imported", "", "", "1.0", formatDate(today)],
                ["For Divine Dragon Only"],
        ])
    }

    def "export subject skills, validate expiration formatting"() {

        def proj1 = createProject(1)
        def proj1_subj = createSubject(1, 1)
        List<Map> proj1_skills = createSkills(6, 1, 1, 50)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        LocalDateTime expirationDate = (new Date() - 1).toLocalDateTime() // yesterday
        skillsService.saveSkillExpirationAttributes(proj1.projectId, proj1_skills[0].skillId as String, [
                expirationType: ExpirationAttrs.YEARLY,
                every: 2,
                monthlyDay: expirationDate.dayOfMonth,
                nextExpirationDate: expirationDate.toDate(),
        ])
        skillsService.saveSkillExpirationAttributes(proj1.projectId, proj1_skills[1].skillId as String, [
                expirationType: ExpirationAttrs.MONTHLY,
                every: 1,
                monthlyDay: expirationDate.dayOfMonth,
                nextExpirationDate: expirationDate.toDate(),
        ])
        skillsService.saveSkillExpirationAttributes(proj1.projectId, proj1_skills[2].skillId as String, [
                expirationType: ExpirationAttrs.MONTHLY,
                every: 3,
                monthlyDay: expirationDate.dayOfMonth,
                nextExpirationDate: expirationDate.toDate(),
        ])
        skillsService.saveSkillExpirationAttributes(proj1.projectId, proj1_skills[3].skillId as String, [
                expirationType: ExpirationAttrs.MONTHLY,
                every: 3,
                monthlyDay: ExpirationAttrs.LAST_DAY_OF_MONTH,
                nextExpirationDate: expirationDate.toDate(),
        ])
        skillsService.saveSkillExpirationAttributes(proj1.projectId, proj1_skills[4].skillId as String, [
                expirationType: ExpirationAttrs.DAILY,
                every: 1,
                monthlyDay: expirationDate.dayOfMonth,
                nextExpirationDate: expirationDate.toDate(),
        ])
        skillsService.saveSkillExpirationAttributes(proj1.projectId, proj1_skills[5].skillId as String, [
                expirationType: ExpirationAttrs.DAILY,
                every: 90,
                monthlyDay: expirationDate.dayOfMonth,
                nextExpirationDate: expirationDate.toDate(),
        ])

        when:
        def excelExport = skillsService.getSkillsForSubjectExport(proj1.projectId, proj1_subj.subjectId)

        then:
        validateExportForCell(excelExport.file, [
                "Every 2 years on ${expirationDate.format("MM/dd")}",
                "Every month on the ${ExcelExportService.getDayOfMonthWithSuffix(expirationDate.dayOfMonth)}",
                "Every 3 months on the ${ExcelExportService.getDayOfMonthWithSuffix(expirationDate.dayOfMonth)}",
                "Every 3 months on the last day",
                "After 1 day of inactivity",
                "After 90 days of inactivity",
        ], 10)
    }
}
