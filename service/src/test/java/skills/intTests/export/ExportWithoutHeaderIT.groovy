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
import groovy.util.logging.Slf4j
import org.springframework.boot.test.context.SpringBootTest
import skills.SpringBootApp
import skills.metrics.builders.MetricsPagingParamsHelper
import skills.metrics.builders.MetricsParams

import static skills.intTests.utils.SkillsFactory.*

@Slf4j
@SpringBootTest(properties = [
        'skills.config.ui.exportHeaderAndFooter=',
        'skills.authorization.userInfoUri=https://localhost:8177/userInfo?dn={dn}',
        'skills.authorization.userQueryUri=https://localhost:8177/userQuery?query={query}',
        'skills.authorization.userInfoHealthCheckUri=https://localhost:8177/status'
], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = SpringBootApp)
class ExportWithoutHeaderIT extends ExportBaseIntSpec {

    def "export users progress, no headers set"() {
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

        def excelExport = skillsService.getUserProgressExcelExport(project.projectId)

        then:
        validateExport(excelExport.file, [
                ["User ID", "Last Name", "First Name", "Org", "Level", "Current Points", "Points First Earned (UTC)", "Points Last Earned (UTC)"],
                [getUserIdForDisplay(user2), getName(user2, false), getName(user2), "", "1.0", "60.0", today.format("dd-MMM-yyyy"), today.format("dd-MMM-yyyy")],
                [getUserIdForDisplay(user1), getName(user1, false), getName(user1), "", "2.0", "100.0", fiveDaysAgo.format("dd-MMM-yyyy"), oneDayAgo.format("dd-MMM-yyyy")],
        ])
    }

    def "export project achievements, no headers set"() {
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
                ["User ID", "Last Name", "First Name", "Org", "Achievement Type", "Achievement Name", "Level", "Achievement Date (UTC)"],
                [getUserIdForDisplay(users[0]), getName(users[0], false), getName(users[0]), "", "Skill", "Test Skill 1", "", dates[1].format("dd-MMM-yyyy")],
                [getUserIdForDisplay(users[0]), getName(users[0], false), getName(users[0]), "", "Subject", "Test Subject #1", "1.0", dates[1].format("dd-MMM-yyyy")],
                [getUserIdForDisplay(users[0]), getName(users[0], false), getName(users[0]), "", "Overall", "Overall", "1.0", dates[1].format("dd-MMM-yyyy")],
                [getUserIdForDisplay(users[1]), getName(users[1], false), getName(users[1]), "", "Skill", "Test Skill 1", "", dates[1].format("dd-MMM-yyyy")],
                [getUserIdForDisplay(users[1]), getName(users[1], false), getName(users[1]), "", "Subject", "Test Subject #1", "1.0", dates[1].format("dd-MMM-yyyy")],
                [getUserIdForDisplay(users[1]), getName(users[1], false), getName(users[1]), "", "Overall", "Overall", "1.0", dates[1].format("dd-MMM-yyyy")],
                [getUserIdForDisplay(users[2]), getName(users[2], false), getName(users[2]), "", "Skill", "Test Skill 1 Subject2", "", dates[1].format("dd-MMM-yyyy")],
                [getUserIdForDisplay(users[2]), getName(users[2], false), getName(users[2]), "", "Subject", "Test Subject #2", "1.0", dates[1].format("dd-MMM-yyyy")],
                [getUserIdForDisplay(users[2]), getName(users[2], false), getName(users[2]), "", "Overall", "Overall", "1.0", dates[1].format("dd-MMM-yyyy")],
        ])
    }

    def "export project skill metrics, no headers set"() {
        List<String> users = getRandomUsers(10)
        def proj = createProject()
        def subj = createSubject()
        List<Map> skills = createSkills(9)
        skills.each { it.pointIncrement = 100; it.numPerformToCompletion = 5 }

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        List<Date> days

        use(TimeCategory) {
            days = (5..0).collect { int day -> day.days.ago }
            days.eachWithIndex { Date date, int index ->
                users.subList(0, index).each { String user ->
                    skills.subList(0, index).each { skill ->
                        skillsService.addSkill([projectId: proj.projectId, skillId: skill.skillId], user, date)
                    }
                }
            }
        }

        when:
        def excelExport = skillsService.getSkillMetricsExcelExport(proj.projectId)

        then:
        validateExport(excelExport.file, [
                ["Skill Name", "Skill ID", "# Users Achieved", "# Users In Progress", "Date Last Reported (UTC)", "Date Last Achieved (UTC)"],
                ["Test Skill 1", "skill1", "1.0", "4.0",  today.format("dd-MMM-yyyy"), today.format("dd-MMM-yyyy")],
                ["Test Skill 2", "skill2", "0.0", "5.0",  today.format("dd-MMM-yyyy"), ""],
                ["Test Skill 3", "skill3", "0.0", "5.0",  today.format("dd-MMM-yyyy"), ""],
                ["Test Skill 4", "skill4", "0.0", "5.0",  today.format("dd-MMM-yyyy"), ""],
                ["Test Skill 5", "skill5", "0.0", "5.0",  today.format("dd-MMM-yyyy"), ""],
                ["Test Skill 6", "skill6", "0.0", "0.0",  "", ""],
                ["Test Skill 7", "skill7", "0.0", "0.0",  "", ""],
                ["Test Skill 8", "skill8", "0.0", "0.0",  "", ""],
                ["Test Skill 9", "skill9", "0.0", "0.0",  "", ""],
        ])
    }
}
