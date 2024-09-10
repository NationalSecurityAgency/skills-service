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

import groovy.time.TimeCategory
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.MockUserInfoService
import skills.intTests.utils.SkillsService
import skills.metrics.builders.MetricsPagingParamsHelper
import skills.metrics.builders.MetricsParams
import skills.storage.model.SkillDef
import spock.lang.Shared

import static skills.intTests.utils.SkillsFactory.*

class ExportUserProjectAcheivementsSpec extends ExportBaseIntSpec {

    def "export project achievements"() {
        def proj = createProject()
        def subj1 = createSubject()
        List<Map> skillsSubj1 = createSkills(5)
        skillsSubj1.each { it.pointIncrement = 200; it.numPerformToCompletion = 1 }

        skillsService.createProject(proj)
        skillsService.createSubject(subj1)
        skillsService.createSkills(skillsSubj1)

        def subj2 = createSubject(1, 2)
        List<Map> skillsSubj2 = createSkills(5, 1, 2)
        skillsSubj2.each { it.pointIncrement = 200; it.numPerformToCompletion = 1 }

        skillsService.createSubject(subj2)
        skillsService.createSkills(skillsSubj2)

        List<String> usersCopy = new ArrayList<>(users)
        achieveLevelForUsers(usersCopy, skillsSubj1, 2, 1, "Subject")
        achieveLevelForUsers(usersCopy, skillsSubj2, 1, 1, "Subject")

        Map props = [:]
        props[MetricsPagingParamsHelper.PROP_SORT_DESC] = false
        props[MetricsPagingParamsHelper.PROP_SORT_BY] = "achievedOn"
        props[MetricsParams.P_ACHIEVEMENT_TYPES] = allAchievementTypes

        when:

        def excelExport = skillsService.getUserAchievementsExcelExport(proj.projectId, props)

        then:

        excelExport
        validateExport(excelExport.file, [
                ["For All Dragons Only"],
                ["User ID", "Last Name", "First Name", "Org", "Achievement Type", "Achievement Name", "Level", "Achievement Date"],
                [getUserIdForDisplay(users[0]), getName(users[0], false), getName(users[0]), "", "Skill", "Test Skill 1", "", dates[1].format("dd-MMM-yyyy")],
                [getUserIdForDisplay(users[0]), getName(users[0], false), getName(users[0]), "", "Subject", "Test Subject #1", "1.0", dates[1].format("dd-MMM-yyyy")],
                [getUserIdForDisplay(users[0]), getName(users[0], false), getName(users[0]), "", "Overall", "Overall", "1.0", dates[1].format("dd-MMM-yyyy")],
                [getUserIdForDisplay(users[1]), getName(users[1], false), getName(users[1]), "", "Skill", "Test Skill 1", "", dates[1].format("dd-MMM-yyyy")],
                [getUserIdForDisplay(users[1]), getName(users[1], false), getName(users[1]), "", "Subject", "Test Subject #1", "1.0", dates[1].format("dd-MMM-yyyy")],
                [getUserIdForDisplay(users[1]), getName(users[1], false), getName(users[1]), "", "Overall", "Overall", "1.0", dates[1].format("dd-MMM-yyyy")],
                [getUserIdForDisplay(users[2]), getName(users[2], false), getName(users[2]), "", "Skill", "Test Skill 1 Subject2", "", dates[1].format("dd-MMM-yyyy")],
                [getUserIdForDisplay(users[2]), getName(users[2], false), getName(users[2]), "", "Subject", "Test Subject #2", "1.0", dates[1].format("dd-MMM-yyyy")],
                [getUserIdForDisplay(users[2]), getName(users[2], false), getName(users[2]), "", "Overall", "Overall", "1.0", dates[1].format("dd-MMM-yyyy")],
                ["For All Dragons Only"],
        ])
    }

    def "export project achievements with user tags"() {
        def proj = createProject()
        def subj1 = createSubject()
        List<Map> skillsSubj1 = createSkills(5)
        skillsSubj1.each { it.pointIncrement = 200; it.numPerformToCompletion = 1 }

        skillsService.createProject(proj)
        skillsService.createSubject(subj1)
        skillsService.createSkills(skillsSubj1)

        def subj2 = createSubject(1, 2)
        List<Map> skillsSubj2 = createSkills(5, 1, 2)
        skillsSubj2.each { it.pointIncrement = 200; it.numPerformToCompletion = 1 }

        skillsService.createSubject(subj2)
        skillsService.createSkills(skillsSubj2)

        List<String> usersCopy = new ArrayList<>(users)
        achieveLevelForUsers(usersCopy, skillsSubj1, 2, 1, "Subject")
        achieveLevelForUsers(usersCopy, skillsSubj2, 1, 1, "Subject")

        Map props = [:]
        props[MetricsPagingParamsHelper.PROP_SORT_DESC] = false
        props[MetricsPagingParamsHelper.PROP_SORT_BY] = "achievedOn"
        props[MetricsParams.P_ACHIEVEMENT_TYPES] = allAchievementTypes

        users.eachWithIndex { userId, idx ->
            String tagValue = "tag${idx}"
            rootSkillsService.saveUserTag(userId, "dutyOrganization", [tagValue]);
        }

        when:
        def excelExport = skillsService.getUserAchievementsExcelExport(proj.projectId, props)

        then:
        validateExport(excelExport.file, [
                ["For All Dragons Only"],
                ["User ID", "Last Name", "First Name", "Org", "Achievement Type", "Achievement Name", "Level", "Achievement Date"],
                [getUserIdForDisplay(users[0]), getName(users[0], false), getName(users[0]), "tag0", "Skill", "Test Skill 1", "", dates[1].format("dd-MMM-yyyy")],
                [getUserIdForDisplay(users[0]), getName(users[0], false), getName(users[0]), "tag0", "Subject", "Test Subject #1", "1.0", dates[1].format("dd-MMM-yyyy")],
                [getUserIdForDisplay(users[0]), getName(users[0], false), getName(users[0]), "tag0", "Overall", "Overall", "1.0", dates[1].format("dd-MMM-yyyy")],
                [getUserIdForDisplay(users[1]), getName(users[1], false), getName(users[1]), "tag1", "Skill", "Test Skill 1", "", dates[1].format("dd-MMM-yyyy")],
                [getUserIdForDisplay(users[1]), getName(users[1], false), getName(users[1]), "tag1", "Subject", "Test Subject #1", "1.0", dates[1].format("dd-MMM-yyyy")],
                [getUserIdForDisplay(users[1]), getName(users[1], false), getName(users[1]), "tag1", "Overall", "Overall", "1.0", dates[1].format("dd-MMM-yyyy")],
                [getUserIdForDisplay(users[2]), getName(users[2], false), getName(users[2]), "tag2", "Skill", "Test Skill 1 Subject2", "", dates[1].format("dd-MMM-yyyy")],
                [getUserIdForDisplay(users[2]), getName(users[2], false), getName(users[2]), "tag2", "Subject", "Test Subject #2", "1.0", dates[1].format("dd-MMM-yyyy")],
                [getUserIdForDisplay(users[2]), getName(users[2], false), getName(users[2]), "tag2", "Overall", "Overall", "1.0", dates[1].format("dd-MMM-yyyy")],
                ["For All Dragons Only"],
        ])
    }

    def "export project achievements for UC protected project"() {
        def users = getRandomUsers(3)
        def user1 = users[0]
        def user2 = users[1]

        SkillsService pristineDragonsUser = createService(users[2])
        rootSkillsService.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])
        rootSkillsService.saveUserTag(rootSkillsService.userName, 'dragons', ['DivineDragon'])

        def project = createProject()
        project.enableProtectedUserCommunity = true
        def subject = createSubject()
        def skill1 = createSkill(1, 1, 1, 0, 5)
        skill1.pointIncrement = 50
        def skill2 = createSkill(1, 1, 2, 0, 5)
        pristineDragonsUser.createProjectAndSubjectAndSkills(project, subject, [skill1, skill2])

        pristineDragonsUser.addSkill(skill1, user1, dates[5])
        pristineDragonsUser.addSkill(skill1, user1, dates[3])
        pristineDragonsUser.addSkill(skill1, user2, dates[0])
        pristineDragonsUser.addSkill(skill2, user2, dates[0])

        Map props = [:]
        props[MetricsPagingParamsHelper.PROP_SORT_DESC] = false
        props[MetricsPagingParamsHelper.PROP_SORT_BY] = "achievedOn"
        props[MetricsParams.P_ACHIEVEMENT_TYPES] = allAchievementTypes

        when:

        def excelExport = pristineDragonsUser.getUserAchievementsExcelExport(project.projectId, props)

        then:
        validateHeaderAndFooter(excelExport.file, "For Divine Dragon Only")
    }

    def "export project achievements with user tags and sort and filter"() {
        def proj = createProject()
        def subj1 = createSubject()
        List<Map> skillsSubj1 = createSkills(5)
        skillsSubj1.each { it.pointIncrement = 200; it.numPerformToCompletion = 1 }

        skillsService.createProject(proj)
        skillsService.createSubject(subj1)
        skillsService.createSkills(skillsSubj1)

        def subj2 = createSubject(1, 2)
        List<Map> skillsSubj2 = createSkills(5, 1, 2)
        skillsSubj2.each { it.pointIncrement = 200; it.numPerformToCompletion = 1 }

        skillsService.createSubject(subj2)
        skillsService.createSkills(skillsSubj2)

        List<String> usersCopy = new ArrayList<>(users)
        achieveLevelForUsers(usersCopy, skillsSubj1, 2, 1, "Subject")
        achieveLevelForUsers(usersCopy, skillsSubj1, 1, 2, "Subject")
        achieveLevelForUsers(usersCopy, skillsSubj2, 1, 1, "Subject")

        Map props = [:]
        props[MetricsPagingParamsHelper.PROP_SORT_DESC] = false
        props[MetricsPagingParamsHelper.PROP_SORT_BY] = "achievedOn"
        props[MetricsParams.P_ACHIEVEMENT_TYPES] = allAchievementTypes

        when:
        def excelExportSortAchievedAsc = skillsService.getUserAchievementsExcelExport(proj.projectId, props)

        props[MetricsPagingParamsHelper.PROP_SORT_DESC] = true
        def excelExportSortAchievedDesc = skillsService.getUserAchievementsExcelExport(proj.projectId, props)

        props[MetricsParams.P_USERNAME_FILTER] = users[0]
        def excelExportQueryFilter = skillsService.getUserAchievementsExcelExport(proj.projectId, props)
        List<List<String>> expectedDataForQuery = [
                ["For All Dragons Only"],
                ["User ID", "Last Name", "First Name", "Org", "Achievement Type", "Achievement Name", "Level", "Achievement Date"],
                [getUserIdForDisplay(users[0]), getName(users[0], false), getName(users[0]), "", "Overall", "Overall", "1.0", dates[1].format("dd-MMM-yyyy")],
                [getUserIdForDisplay(users[0]), getName(users[0], false), getName(users[0]), "", "Subject", "Test Subject #1", "1.0", dates[1].format("dd-MMM-yyyy")],
                [getUserIdForDisplay(users[0]), getName(users[0], false), getName(users[0]), "", "Skill", "Test Skill 1", "", dates[1].format("dd-MMM-yyyy")],
                ["For All Dragons Only"],
        ]

        then:
        validateSortAchievedOn(excelExportSortAchievedAsc.file, dates[1].format("dd-MMM-yyyy"), dates[2].format("dd-MMM-yyyy"))
        validateSortAchievedOn(excelExportSortAchievedDesc.file, dates[2].format("dd-MMM-yyyy"), dates[1].format("dd-MMM-yyyy"))
        validateExport(excelExportQueryFilter.file, expectedDataForQuery)
    }

    void validateExport(File file, List<List<String>> data) {
        assert file.exists()
        assert data
        Workbook workbook = WorkbookFactory.create(file)
        Sheet sheet = workbook.getSheetAt(0)
        assert sheet.getPhysicalNumberOfRows() == data.size()

        data.eachWithIndex { dataRow, rowIndex ->
            Row row = sheet.getRow(rowIndex)
            for (int i = 0; i < dataRow.size(); i++) {
                assert row.getCell(i).toString() == dataRow.get(i)
            }
        }
    }

    void validateSortAchievedOn(File file, String firstDate, String lastDate) {
        assert file.exists()
        assert firstDate
        assert lastDate
        Workbook workbook = WorkbookFactory.create(file)
        Sheet sheet = workbook.getSheetAt(0)
        Row firstDateRow = sheet.getRow(2)
        Row lastDateRow = sheet.getRow(sheet.getPhysicalNumberOfRows()-2)
        assert firstDateRow.getCell(7).toString() == firstDate
        assert lastDateRow.getCell(7).toString() == lastDate

    }

    void validateHeaderAndFooter(File file, String header) {
        assert file.exists()
        assert header
        Workbook workbook = WorkbookFactory.create(file)
        Sheet sheet = workbook.getSheetAt(0)
        Row firstRow = sheet.getRow(0)
        Row lastRow = sheet.getRow(sheet.getPhysicalNumberOfRows()-1)
        assert firstRow.getCell(0).toString() == header
        assert lastRow.getCell(0).toString() == header
    }

    String getUserIdForDisplay(String userId) {
        return isPkiMode ? "${mockUserInfoService.getUserIdWithCase(userId)} for display" : userId
    }

    String getName(String userId, firstName = true) {
        if (!isPkiMode) {
            return firstName ? "${userId.toUpperCase()}_first" : "${userId.toUpperCase()}_last"
        } else {
            MockUserInfoService.FirstnameLastname firstnameLastname = mockUserInfoService.getFirstNameLastnameForUserId(userId)
            return firstnameLastname ? (firstName ? firstnameLastname.firstname : firstnameLastname.lastname) : 'Fake'
        }

    }
    private void achieveLevelForUsers(List<String> users, List<Map> skills, int numUsers, int level, String type = "Overall") {
        List<String> usersToUse = (1..numUsers).collect({
            String user = users.pop()
            assert user
            return user
        })

        usersToUse.each { user ->
            int userIndex = this.users.findIndexOf({ it == user })
            achieveLevel(skills, user, userIndex, level, type)
        }
    }

    private void achieveLevel(List<Map> skills, String user, int userIndex, int level, String type = "Overall") {
        use(TimeCategory) {
            boolean found = false
            int skillIndex = 0
            while (!found) {
                def res = skillsService.addSkill([projectId: skills[skillIndex].projectId, skillId: skills[skillIndex].skillId], user, dates[level] + userIndex.hour)
                found = res.body.completed.findAll({ it.type == type })?.find { it.level == level }
                skillIndex++
            }
        }
    }
}
