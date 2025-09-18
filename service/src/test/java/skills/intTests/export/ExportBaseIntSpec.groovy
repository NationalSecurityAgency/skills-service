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
import org.apache.poi.ss.usermodel.Cell
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

class ExportBaseIntSpec extends DefaultIntSpec {
    String ultimateRoot = 'jh@dojo.com'
    SkillsService rootSkillsService
    Boolean isPkiMode = false;

    Date today = new Date()
    Date oneDayAgo = new Date()-1
    Date twoDayAgo = new Date()-2
    Date threeDayAgo = new Date()-3
    Date fourDayAgo = new Date()-4
    Date fiveDaysAgo = new Date()-5
    Date tenDaysAgo = new Date()-10

    String allAchievementTypes = "${MetricsParams.ACHIEVEMENT_TYPE_OVERALL},${SkillDef.ContainerType.Subject},${SkillDef.ContainerType.Skill},${SkillDef.ContainerType.Badge},${SkillDef.ContainerType.GlobalBadge}"

    @Shared
    List<String> users

    @Shared
    List<Date> dates

    def setupSpec() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    def setup() {
        rootSkillsService = createService(ultimateRoot, 'aaaaaaaa')
        if (!rootSkillsService.isRoot()) {
            rootSkillsService.grantRoot()
        }
        isPkiMode = mockUserInfoService != null
        users = new ArrayList<>(getRandomUsers(4))

        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        use(TimeCategory) {
            dates = (10..0).collect({
                return it.days.ago;
            })
        }
        today = new Date()
        oneDayAgo = new Date()-1
        fiveDaysAgo = new Date()-5
        tenDaysAgo = new Date()-10
    }

    protected void validateExport(File file, List<List<String>> data) {
        assert file.exists()
        assert data
        Workbook workbook = WorkbookFactory.create(file)
        Sheet sheet = workbook.getSheetAt(0)
        printSheet(sheet)
        assert sheet.getPhysicalNumberOfRows() == data.size()

        data.eachWithIndex { dataRow, rowIndex ->
            Row row = sheet.getRow(rowIndex)
            for (int i = 0; i < dataRow.size(); i++) {
                assert normalize(row.getCell(i).toString()) == normalize(dataRow.get(i)), "row: ${rowIndex} col: ${i} expected: ${dataRow.get(i)} actual: ${row.getCell(i).toString()}"
            }
        }
    }

    void printSheet(Sheet sheet) {
        for (Row row : sheet) {
            for (Cell cell : row) {
                print "${cell}, "
            }
            println ""
        }
    }

    protected void validateExportForCell(File file, List<String> data, Integer cellIndex) {
        assert file.exists()
        assert data
        Workbook workbook = WorkbookFactory.create(file)
        Sheet sheet = workbook.getSheetAt(0)
        assert sheet.getPhysicalNumberOfRows() == data.size() + 3

        data.eachWithIndex { expectedValue, rowIndex ->
            Row row = sheet.getRow(rowIndex + 2)
            assert normalize(row.getCell(cellIndex).toString()) == normalize(expectedValue), "row: ${rowIndex} col: ${cellIndex} expected: ${expectedValue} actual: ${row.getCell(cellIndex).toString()}"
        }
    }

    protected void validateSortAchievedOn(File file, String firstDate, String lastDate) {
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

    protected void validateHeaderAndFooter(File file, String header) {
        assert file.exists()
        assert header
        Workbook workbook = WorkbookFactory.create(file)
        Sheet sheet = workbook.getSheetAt(0)
        Row firstRow = sheet.getRow(0)
        Row lastRow = sheet.getRow(sheet.getPhysicalNumberOfRows()-1)
        assert firstRow.getCell(0).toString() == header
        assert lastRow.getCell(0).toString() == header
    }

    protected String getUserIdForDisplay(String userId) {
        return isPkiMode ? "${mockUserInfoService.getUserIdWithCase(userId)} for display" : userId
    }

    protected String getName(String userId, firstName = true) {
        if (!isPkiMode) {
            return firstName ? "${userId.toUpperCase()}_first" : "${userId.toUpperCase()}_last"
        } else {
            MockUserInfoService.FirstnameLastname firstnameLastname = mockUserInfoService.getFirstNameLastnameForUserId(userId)
            return firstnameLastname ? (firstName ? firstnameLastname.firstname : firstnameLastname.lastname) : 'Fake'
        }

    }
    protected void achieveLevelForUsers(List<String> users, List<Map> skills, int numUsers, int level, String type = "Overall") {
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

    protected void achieveLevel(List<Map> skills, String user, int userIndex, int level, String type = "Overall") {
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

    static String formatDate(Date date, Integer extraHours = 0) {
        use(TimeCategory) {
            return (date + extraHours.hour).format("M/d/yy H:mm")
        }
    }

    static String normalize(String inputString) {
        // strip out time value, eg "5/1/25 14:30" => "5/1/25"
        return inputString.replaceAll(/(\d{1,2}\/\d{1,2}\/\d{2})\s+\d{1,2}:\d{2}/, '$1')
    }
}
