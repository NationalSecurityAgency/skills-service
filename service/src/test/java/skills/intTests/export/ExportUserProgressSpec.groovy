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

import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.MockUserInfoService
import skills.intTests.utils.SkillsService

import static skills.intTests.utils.SkillsFactory.*

class ExportUserProgressSpec extends DefaultIntSpec {
    String ultimateRoot = 'jh@dojo.com'
    SkillsService rootSkillsService

    Date today = new Date()
    Date oneDayAgo = new Date()-1
    Date fiveDaysAgo = new Date()-5
    Date tenDaysAgo = new Date()-10

    private Boolean isPkiMode = false;
    
    def setup() {
        rootSkillsService = createService(ultimateRoot, 'aaaaaaaa')
        if (!rootSkillsService.isRoot()) {
            rootSkillsService.grantRoot()
        }
        isPkiMode = mockUserInfoService != null
    }

    def "export users progress"() {
        def project = createProject()
        def subject = createSubject()
        def skill1 = createSkill(1, 1, 1, 0, 5)
        skill1.pointIncrement = 50
        def skill2 = createSkill(1, 1, 2, 0, 5)

        skillsService.createProject(project)
        skillsService.createSubject(subject)
        skillsService.createSkill(skill1)
        skillsService.createSkill(skill2)

        def users = getRandomUsers(2)
        def user1 = users[0]
        def user2 = users[1]

        when:
        skillsService.addSkill(skill1, user1, fiveDaysAgo)
        skillsService.addSkill(skill1, user1, oneDayAgo)
        skillsService.addSkill(skill1, user2, today)
        skillsService.addSkill(skill2, user2, today)

        def excelExport = skillsService.getProjectUsersExcelExport(project.projectId)
        skillsService.deleteSkill(skill1)

        def excelExportAfterDelete = skillsService.getProjectUsersExcelExport(project.projectId)


        then:
        validateExport(excelExport.file, [
                ["For All Dragons Only"],
                ["User ID", "Last Name", "First Name", "Org", "Level", "Current Points", "Points First Earned", "Points Last Earned"],
                [getUserIdForDisplay(user2), getName(user2, false), getName(user2), "", "1.0", "60.0", today.format("dd-MMM-yyyy"), today.format("dd-MMM-yyyy")],
                [getUserIdForDisplay(user1), getName(user1, false), getName(user1), "", "2.0", "100.0", fiveDaysAgo.format("dd-MMM-yyyy"), oneDayAgo.format("dd-MMM-yyyy")],
                ["For All Dragons Only"],
        ])

        validateExport(excelExportAfterDelete.file, [
                ["For All Dragons Only"],
                ["User ID", "Last Name", "First Name", "Org", "Level", "Current Points", "Points First Earned", "Points Last Earned"],
                [getUserIdForDisplay(user2), getName(user2, false), getName(user2), "", "1.0", "10.0", today.format("dd-MMM-yyyy"), today.format("dd-MMM-yyyy")],
                ["For All Dragons Only"],
        ])
    }

    def "export users progress with user tags"() {
        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def skill1 = createSkill(2, 1, 1, 0, 10, 512, 10,)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [skill1])

        def p2subj2 = createSubject(2, 2)
        def skill2 = createSkill(2, 2, 2, 0, 10, 512, 10,)
        skillsService.createSubject(p2subj2)
        skillsService.createSkill(skill2)

        def p2Badge1 = createBadge(2, 1)
        skillsService.addBadge(p2Badge1)
        skillsService.assignSkillToBadge([projectId: p2Badge1.projectId, badgeId: p2Badge1.badgeId, skillId: skill2.skillId])

        List<String> users = getRandomUsers(6)
        skillsService.addSkill(skill1, users[0])

        // overall level 1
        skillsService.addSkill(skill1, users[1], new Date() - 1)
        skillsService.addSkill(skill2, users[1])
        skillsService.addSkill(skill1, users[2], new Date() - 2)
        skillsService.addSkill(skill1, users[2], new Date() - 1)
        skillsService.addSkill(skill2, users[2])

        // overall level 2
        (5..1).each {
            skillsService.addSkill(skill1, users[3], new Date() - it)
        }

        // overall level 3
        (4..1).each {
            skillsService.addSkill(skill1, users[4], new Date() - it)
            skillsService.addSkill(skill1, users[5], new Date() - it)
        }
        (9..1).each {
            skillsService.addSkill(skill2, users[4], new Date() - it)
            skillsService.addSkill(skill2, users[5], new Date() - it)
        }
        // overall level 4
        skillsService.addSkill(skill2, users[5], new Date() - 10)

        users.eachWithIndex { userId, idx ->
            String tagValue = "tag${idx}"
            rootSkillsService.saveUserTag(userId, "dutyOrganization", [tagValue]);
        }

        when:
        def excelExport = skillsService.getProjectUsersExcelExport(p2.projectId)

        then:
        validateExport(excelExport.file, [
                ["For All Dragons Only"],
                ["User ID", "Last Name", "First Name", "Org", "Level", "Current Points", "Points First Earned", "Points Last Earned"],
                [getUserIdForDisplay(users[0]), getName(users[0], false), getName(users[0]), "tag0", "0.0", "10.0", today.format("dd-MMM-yyyy"), today.format("dd-MMM-yyyy")],
                [getUserIdForDisplay(users[1]), getName(users[1], false), getName(users[1]), "tag1", "1.0", "20.0", oneDayAgo.format("dd-MMM-yyyy"), today.format("dd-MMM-yyyy")],
                [getUserIdForDisplay(users[2]), getName(users[2], false), getName(users[2]), "tag2", "1.0", "30.0", (oneDayAgo-1).format("dd-MMM-yyyy"), today.format("dd-MMM-yyyy")],
                [getUserIdForDisplay(users[3]), getName(users[3], false), getName(users[3]), "tag3", "2.0", "50.0", fiveDaysAgo.format("dd-MMM-yyyy"), oneDayAgo.format("dd-MMM-yyyy")],
                [getUserIdForDisplay(users[4]), getName(users[4], false), getName(users[4]), "tag4", "3.0", "130.0", (tenDaysAgo+1).format("dd-MMM-yyyy"), oneDayAgo.format("dd-MMM-yyyy")],
                [getUserIdForDisplay(users[5]), getName(users[5], false), getName(users[5]), "tag5", "4.0", "140.0", (tenDaysAgo).format("dd-MMM-yyyy"), oneDayAgo.format("dd-MMM-yyyy")],
                ["For All Dragons Only"],
        ])
    }

    def "export users progress for UC protected project"() {
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

        when:
        pristineDragonsUser.addSkill(skill1, user1, fiveDaysAgo)
        pristineDragonsUser.addSkill(skill1, user1, oneDayAgo)
        pristineDragonsUser.addSkill(skill1, user2, today)
        pristineDragonsUser.addSkill(skill2, user2, today)

        def excelExport = pristineDragonsUser.getProjectUsersExcelExport(project.projectId)

        then:
        validateExport(excelExport.file, [
                ["For Divine Dragon Only"],
                ["User ID", "Last Name", "First Name", "Org", "Level", "Current Points", "Points First Earned", "Points Last Earned"],
                [getUserIdForDisplay(user2), getName(user2, false), getName(user2), "", "1.0", "60.0", today.format("dd-MMM-yyyy"), today.format("dd-MMM-yyyy")],
                [getUserIdForDisplay(user1), getName(user1, false), getName(user1), "", "2.0", "100.0", fiveDaysAgo.format("dd-MMM-yyyy"), oneDayAgo.format("dd-MMM-yyyy")],
                ["For Divine Dragon Only"],
        ])
    }


    def "export users progress with user tags and sort and filter"() {
        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def skill1 = createSkill(2, 1, 1, 0, 10, 512, 10,)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [skill1])

        def p2subj2 = createSubject(2, 2)
        def skill2 = createSkill(2, 2, 2, 0, 10, 512, 10,)
        skillsService.createSubject(p2subj2)
        skillsService.createSkill(skill2)

        def p2Badge1 = createBadge(2, 1)
        skillsService.addBadge(p2Badge1)
        skillsService.assignSkillToBadge([projectId: p2Badge1.projectId, badgeId: p2Badge1.badgeId, skillId: skill2.skillId])

        List<String> users = getRandomUsers(6)
        skillsService.addSkill(skill1, users[0])

        // overall level 1
        skillsService.addSkill(skill1, users[1], new Date() - 1)
        skillsService.addSkill(skill2, users[1])
        skillsService.addSkill(skill1, users[2], new Date() - 2)
        skillsService.addSkill(skill1, users[2], new Date() - 1)
        skillsService.addSkill(skill2, users[2])

        // overall level 2
        (5..1).each {
            skillsService.addSkill(skill1, users[3], new Date() - it)
        }

        // overall level 3
        (4..1).each {
            skillsService.addSkill(skill1, users[4], new Date() - it)
            skillsService.addSkill(skill1, users[5], new Date() - it)
        }
        (9..1).each {
            skillsService.addSkill(skill2, users[4], new Date() - it)
            skillsService.addSkill(skill2, users[5], new Date() - it)
        }
        // overall level 4
        skillsService.addSkill(skill2, users[5], new Date() - 10)

        users.eachWithIndex { userId, idx ->
            String tagNum = idx+1 == users.size() ? "10" : "0${idx}"
            String tagValue = "tag${tagNum}"
            rootSkillsService.saveUserTag(userId, "dutyOrganization", [tagValue]);
        }

        when:
        boolean ascending = true
        def excelExportSortPointsAsc = skillsService.getProjectUsersExcelExport(p2.projectId)
        def excelExportSortPointsDesc = skillsService.getProjectUsersExcelExport(p2.projectId, 'totalPoints', !ascending)
        def excelExportQueryFilter = skillsService.getProjectUsersExcelExport(p2.projectId, 'totalPoints', ascending, users[0])
        def excelExportMinPointsFilter = skillsService.getProjectUsersExcelExport(p2.projectId, 'totalPoints', ascending, "", 50)

        List<List<String>> expectedDataForSortAsc = [
                ["For All Dragons Only"],
                ["User ID", "Last Name", "First Name", "Org", "Level", "Current Points", "Points First Earned", "Points Last Earned"],
                [getUserIdForDisplay(users[0]), getName(users[0], false), getName(users[0]), "tag00", "0.0", "10.0", today.format("dd-MMM-yyyy"), today.format("dd-MMM-yyyy")],
                [getUserIdForDisplay(users[1]), getName(users[1], false), getName(users[1]), "tag01", "1.0", "20.0", oneDayAgo.format("dd-MMM-yyyy"), today.format("dd-MMM-yyyy")],
                [getUserIdForDisplay(users[2]), getName(users[2], false), getName(users[2]), "tag02", "1.0", "30.0", (oneDayAgo-1).format("dd-MMM-yyyy"), today.format("dd-MMM-yyyy")],
                [getUserIdForDisplay(users[3]), getName(users[3], false), getName(users[3]), "tag03", "2.0", "50.0", fiveDaysAgo.format("dd-MMM-yyyy"), oneDayAgo.format("dd-MMM-yyyy")],
                [getUserIdForDisplay(users[4]), getName(users[4], false), getName(users[4]), "tag04", "3.0", "130.0", (tenDaysAgo+1).format("dd-MMM-yyyy"), oneDayAgo.format("dd-MMM-yyyy")],
                [getUserIdForDisplay(users[5]), getName(users[5], false), getName(users[5]), "tag10", "4.0", "140.0", (tenDaysAgo).format("dd-MMM-yyyy"), oneDayAgo.format("dd-MMM-yyyy")],
                ["For All Dragons Only"],
        ]
        List<List<String>> expectedDataForSortDesc = [
                ["For All Dragons Only"],
                ["User ID", "Last Name", "First Name", "Org", "Level", "Current Points", "Points First Earned", "Points Last Earned"],
                [getUserIdForDisplay(users[5]), getName(users[5], false), getName(users[5]), "tag10", "4.0", "140.0", (tenDaysAgo).format("dd-MMM-yyyy"), oneDayAgo.format("dd-MMM-yyyy")],
                [getUserIdForDisplay(users[4]), getName(users[4], false), getName(users[4]), "tag04", "3.0", "130.0", (tenDaysAgo+1).format("dd-MMM-yyyy"), oneDayAgo.format("dd-MMM-yyyy")],
                [getUserIdForDisplay(users[3]), getName(users[3], false), getName(users[3]), "tag03", "2.0", "50.0", fiveDaysAgo.format("dd-MMM-yyyy"), oneDayAgo.format("dd-MMM-yyyy")],
                [getUserIdForDisplay(users[2]), getName(users[2], false), getName(users[2]), "tag02", "1.0", "30.0", (oneDayAgo-1).format("dd-MMM-yyyy"), today.format("dd-MMM-yyyy")],
                [getUserIdForDisplay(users[1]), getName(users[1], false), getName(users[1]), "tag01", "1.0", "20.0", oneDayAgo.format("dd-MMM-yyyy"), today.format("dd-MMM-yyyy")],
                [getUserIdForDisplay(users[0]), getName(users[0], false), getName(users[0]), "tag00", "0.0", "10.0", today.format("dd-MMM-yyyy"), today.format("dd-MMM-yyyy")],
                ["For All Dragons Only"],
        ]
        List<List<String>> expectedDataForQuery = [
                ["For All Dragons Only"],
                ["User ID", "Last Name", "First Name", "Org", "Level", "Current Points", "Points First Earned", "Points Last Earned"],
                [getUserIdForDisplay(users[0]), getName(users[0], false), getName(users[0]), "tag00", "0.0", "10.0", today.format("dd-MMM-yyyy"), today.format("dd-MMM-yyyy")],
                ["For All Dragons Only"],
        ]
        List<List<String>> expectedDataForMinPointsFilter = [
                ["For All Dragons Only"],
                ["User ID", "Last Name", "First Name", "Org", "Level", "Current Points", "Points First Earned", "Points Last Earned"],
                [getUserIdForDisplay(users[3]), getName(users[3], false), getName(users[3]), "tag03", "2.0", "50.0", fiveDaysAgo.format("dd-MMM-yyyy"), oneDayAgo.format("dd-MMM-yyyy")],
                [getUserIdForDisplay(users[4]), getName(users[4], false), getName(users[4]), "tag04", "3.0", "130.0", (tenDaysAgo+1).format("dd-MMM-yyyy"), oneDayAgo.format("dd-MMM-yyyy")],
                [getUserIdForDisplay(users[5]), getName(users[5], false), getName(users[5]), "tag10", "4.0", "140.0", (tenDaysAgo).format("dd-MMM-yyyy"), oneDayAgo.format("dd-MMM-yyyy")],
                ["For All Dragons Only"],
        ]

        then:
        validateExport(excelExportSortPointsAsc.file, expectedDataForSortAsc)
        validateExport(excelExportSortPointsDesc.file, expectedDataForSortDesc)
        validateExport(excelExportQueryFilter.file, expectedDataForQuery)
        validateExport(excelExportMinPointsFilter.file, expectedDataForMinPointsFilter)
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
}
