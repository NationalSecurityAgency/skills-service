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
import static skills.intTests.utils.SkillsFactory.*

class ExportGlobalBadgeUserProgressSpec extends ExportBaseIntSpec  {

    def "export global badge users progress"() {
        def project = createProject()
        def subject = createSubject()
        def skill1 = createSkill(1, 1, 1, 0, 2)
        skill1.pointIncrement = 50
        def skill2 = createSkill(1, 1, 2, 0, 2)

        skillsService.createProject(project)
        skillsService.createSubject(subject)
        skillsService.createSkill(skill1)
        skillsService.createSkill(skill2)

        def globalBadge = createBadge()
        skillsService.createGlobalBadge(globalBadge)
        skillsService.assignSkillToGlobalBadge([projectId: project.projectId, badgeId: globalBadge.badgeId, skillId: skill1.skillId])
        skillsService.assignSkillToGlobalBadge([projectId: project.projectId, badgeId: globalBadge.badgeId, skillId: skill2.skillId])
        skillsService.assignProjectLevelToGlobalBadge([badgeId: globalBadge.badgeId, projectId: project.projectId, level: "3"])

        globalBadge.enabled = "true"
        skillsService.updateGlobalBadge(globalBadge)

        def users = getRandomUsers(3)
        def user1 = users[0]
        def user2 = users[1]
        def user3 = users[2]

        when:
        skillsService.addSkill(skill1, user1, fiveDaysAgo)
        skillsService.addSkill(skill1, user1, oneDayAgo)
        skillsService.addSkill(skill1, user2, today)
        skillsService.addSkill(skill2, user2, today)
        skillsService.addSkill(skill1, user3, fiveDaysAgo)
        skillsService.addSkill(skill1, user3, today)
        skillsService.addSkill(skill2, user3, fiveDaysAgo)
        skillsService.addSkill(skill2, user3, today)

        def excelExport = skillsService.getGlobalBadgeUserProgressExcelExport(globalBadge.badgeId)

        then:
        validateExport(excelExport.file, [
                ["For All Dragons Only"],
                ["User ID", "Last Name", "First Name", "Org", "Skills Achieved", "Levels Achieved", "Percent Complete", "Skill Last Earned (UTC)"],
                [getUserIdForDisplay(user2), getName(user2, false), getName(user2), "", "0.0", "3.0", "0.6", formatDate(today)],
                [getUserIdForDisplay(user1), getName(user1, false), getName(user1), "", "1.0", "3.0", "0.8", formatDate(today)],
                [getUserIdForDisplay(user3), getName(user3, false), getName(user3), "", "2.0", "3.0", "1.0", formatDate(today)],
                ["For All Dragons Only"],
        ])

    }

    def "export global badge users progress with user tags"() {
        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def skill1 = createSkill(2, 1, 1, 0, 3)
        skill1.pointIncrement = 50
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [skill1])

        def p2subj2 = createSubject(2, 2)
        def skill2 = createSkill(2, 2, 2, 0, 3)
        skill2.pointIncrement = 50
        skillsService.createSubject(p2subj2)
        skillsService.createSkill(skill2)

        def globalBadge = createBadge()
        skillsService.createGlobalBadge(globalBadge)
        skillsService.assignSkillToGlobalBadge([projectId: p2.projectId, badgeId: globalBadge.badgeId, skillId: skill1.skillId])
        skillsService.assignSkillToGlobalBadge([projectId: p2.projectId, badgeId: globalBadge.badgeId, skillId: skill2.skillId])
        skillsService.assignProjectLevelToGlobalBadge([badgeId: globalBadge.badgeId, projectId: p2.projectId, level: "5"])
        globalBadge.enabled = "true"
        skillsService.updateGlobalBadge(globalBadge)

        List<String> users = getRandomUsers(6)
        skillsService.addSkill(skill1, users[0], new Date() - 2)
        skillsService.addSkill(skill1, users[0], new Date() - 1)
        skillsService.addSkill(skill1, users[0])
        skillsService.addSkill(skill1, users[1], new Date() - 1)
        skillsService.addSkill(skill1, users[1])
        skillsService.addSkill(skill2, users[2], new Date() - 2)
        skillsService.addSkill(skill2, users[2], new Date() - 1)
        skillsService.addSkill(skill2, users[2])
        skillsService.addSkill(skill2, users[3], new Date() - 1)
        skillsService.addSkill(skill2, users[3])
        skillsService.addSkill(skill1, users[4], new Date() - 2)
        skillsService.addSkill(skill1, users[4], new Date() - 1)
        skillsService.addSkill(skill1, users[4])
        skillsService.addSkill(skill2, users[4], new Date() - 2)
        skillsService.addSkill(skill2, users[4], new Date() - 1)
        skillsService.addSkill(skill2, users[4])

        users.eachWithIndex { userId, idx ->
            String tagValue = "tag${idx}"
            rootSkillsService.saveUserTag(userId, "dutyOrganization", [tagValue]);
        }

        when:
        def excelExport = skillsService.getGlobalBadgeUserProgressExcelExport(globalBadge.badgeId)

        then:
        validateExport(excelExport.file, [
                ["For All Dragons Only"],
                ["User ID", "Last Name", "First Name", "Org", "Skills Achieved", "Levels Achieved", "Percent Complete", "Skill Last Earned (UTC)"],
                [getUserIdForDisplay(users[1]), getName(users[1], false), getName(users[1]), "tag1", "0.0", "2.0", "0.2857142857", formatDate(today)],
                [getUserIdForDisplay(users[3]), getName(users[3], false), getName(users[3]), "tag3", "0.0", "2.0", "0.2857142857", formatDate(today)],
                [getUserIdForDisplay(users[0]), getName(users[0], false), getName(users[0]), "tag0", "1.0", "3.0", "0.5714285714", formatDate(today)],
                [getUserIdForDisplay(users[2]), getName(users[2], false), getName(users[2]), "tag2", "1.0", "3.0", "0.5714285714", formatDate(today)],
                [getUserIdForDisplay(users[4]), getName(users[4], false), getName(users[4]), "tag4", "2.0", "5.0", "1.0", formatDate(today)],
                ["For All Dragons Only"],
        ])
    }

    def "export global badge users progress and filter by user tag"() {
        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def skill1 = createSkill(2, 1, 1, 0, 2)
        skill1.pointIncrement = 50
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [skill1])

        def p2subj2 = createSubject(2, 2)
        def skill2 = createSkill(2, 2, 2, 0, 2)
        skill2.pointIncrement = 50
        skillsService.createSubject(p2subj2)
        skillsService.createSkill(skill2)

        def globalBadge = createBadge()
        skillsService.createGlobalBadge(globalBadge)
        skillsService.assignSkillToGlobalBadge([projectId: p2.projectId, badgeId: globalBadge.badgeId, skillId: skill1.skillId])
        skillsService.assignSkillToGlobalBadge([projectId: p2.projectId, badgeId: globalBadge.badgeId, skillId: skill2.skillId])
        skillsService.assignProjectLevelToGlobalBadge([badgeId: globalBadge.badgeId, projectId: p2.projectId, level: "5"])
        globalBadge.enabled = "true"
        skillsService.updateGlobalBadge(globalBadge)

        List<String> users = getRandomUsers(6)
        for(var x = 0; x < 6; x++) {
            skillsService.addSkill(skill1, users[x], new Date() - 1)
            skillsService.addSkill(skill1, users[x], new Date())
            skillsService.addSkill(skill2, users[x], new Date() - 1)
            skillsService.addSkill(skill2, users[x], new Date())
        }

        users.eachWithIndex { userId, idx ->
            String tagValue = "tag${idx}"
            rootSkillsService.saveUserTag(userId, "dutyOrganization", [tagValue]);
        }

        when:
        def excelExport = skillsService.getGlobalBadgeUserProgressExcelExport(globalBadge.badgeId, 'totalProgress', true, '', 'tag3')

        then:
        validateExport(excelExport.file, [
                ["For All Dragons Only"],
                ["User ID", "Last Name", "First Name", "Org", "Skills Achieved", "Levels Achieved", "Percent Complete", "Skill Last Earned (UTC)"],
                [getUserIdForDisplay(users[3]), getName(users[3], false), getName(users[3]), "tag3", "2.0", "5.0", "1.0", formatDate(today)],
                ["For All Dragons Only"],
        ])
    }

    def "export users progress with user tags and sort and filter"() {
        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def skill1 = createSkill(2, 1, 1, 0, 2)
        def subj1skill2 = createSkill(2, 1, 2, 0, 2)
        def subj1skill3 = createSkill(2, 1, 3, 0, 2)
        skill1.pointIncrement = 50
        subj1skill2.pointIncrement = 50
        subj1skill3.pointIncrement = 50
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [skill1, subj1skill2, subj1skill3])

        def p2subj2 = createSubject(2, 2)
        def skill2 = createSkill(2, 2, 2, 0, 2)
        skill2.pointIncrement = 50
        skillsService.createSubject(p2subj2)
        skillsService.createSkill(skill2)

        def globalBadge = createBadge()
        skillsService.createGlobalBadge(globalBadge)
        skillsService.assignSkillToGlobalBadge([projectId: p2.projectId, badgeId: globalBadge.badgeId, skillId: skill1.skillId])
        skillsService.assignSkillToGlobalBadge([projectId: p2.projectId, badgeId: globalBadge.badgeId, skillId: skill2.skillId])
        skillsService.assignSkillToGlobalBadge([projectId: p2.projectId, badgeId: globalBadge.badgeId, skillId: subj1skill2.skillId])
        skillsService.assignSkillToGlobalBadge([projectId: p2.projectId, badgeId: globalBadge.badgeId, skillId: subj1skill3.skillId])
        skillsService.assignProjectLevelToGlobalBadge([badgeId: globalBadge.badgeId, projectId: p2.projectId, level: "5"])
        globalBadge.enabled = "true"
        skillsService.updateGlobalBadge(globalBadge)

        List<String> users = getRandomUsers(5)
        skillsService.addSkill(skill1, users[0], new Date())

        skillsService.addSkill(skill1, users[1], new Date())
        skillsService.addSkill(skill1, users[1], new Date() - 1)

        skillsService.addSkill(skill1, users[2], new Date())
        skillsService.addSkill(skill1, users[2], new Date() - 1)
        skillsService.addSkill(skill2, users[2], new Date())
        skillsService.addSkill(skill2, users[2], new Date() - 1)

        skillsService.addSkill(skill1, users[3], new Date())
        skillsService.addSkill(skill1, users[3], new Date() - 1)
        skillsService.addSkill(skill2, users[3], new Date())
        skillsService.addSkill(skill2, users[3], new Date() - 1)
        skillsService.addSkill(subj1skill2, users[3], new Date())
        skillsService.addSkill(subj1skill2, users[3], new Date() - 1)

        skillsService.addSkill(skill1, users[4], new Date())
        skillsService.addSkill(skill1, users[4], new Date() - 1)
        skillsService.addSkill(skill2, users[4], new Date())
        skillsService.addSkill(skill2, users[4], new Date() - 1)
        skillsService.addSkill(subj1skill2, users[4], new Date())
        skillsService.addSkill(subj1skill2, users[4], new Date() - 1)
        skillsService.addSkill(subj1skill3, users[4], new Date())
        skillsService.addSkill(subj1skill3, users[4], new Date() - 1)


        users.eachWithIndex { userId, idx ->
            String tagValue = "tag0${idx}"
            rootSkillsService.saveUserTag(userId, "dutyOrganization", [tagValue]);
        }

        when:
        boolean ascending = true
        def excelExportSortPointsAsc = skillsService.getGlobalBadgeUserProgressExcelExport(globalBadge.badgeId)
        def excelExportSortPointsDesc = skillsService.getGlobalBadgeUserProgressExcelExport(globalBadge.badgeId, 'totalProgress', !ascending)
        def excelExportQueryFilter = skillsService.getGlobalBadgeUserProgressExcelExport(globalBadge.badgeId, 'totalProgress', ascending, users[0])

        List<List<String>> expectedDataForSortAsc = [
                ["For All Dragons Only"],
                ["User ID", "Last Name", "First Name", "Org", "Skills Achieved", "Levels Achieved", "Percent Complete", "Skill Last Earned (UTC)"],
                [getUserIdForDisplay(users[0]), getName(users[0], false), getName(users[0]), "tag00", "0.0", "1.0", "0.1111111111", formatDate(today)],
                [getUserIdForDisplay(users[1]), getName(users[1], false), getName(users[1]), "tag01", "1.0", "2.0", "0.3333333333", formatDate(today)],
                [getUserIdForDisplay(users[2]), getName(users[2], false), getName(users[2]), "tag02", "2.0", "3.0", "0.5555555556", formatDate(today)],
                [getUserIdForDisplay(users[3]), getName(users[3], false), getName(users[3]), "tag03", "3.0", "4.0", "0.7777777778", formatDate(today)],
                [getUserIdForDisplay(users[4]), getName(users[4], false), getName(users[4]), "tag04", "4.0", "5.0", "1.0", formatDate(today)],
                ["For All Dragons Only"],
        ]
        List<List<String>> expectedDataForSortDesc = [
                ["For All Dragons Only"],
                ["User ID", "Last Name", "First Name", "Org", "Skills Achieved", "Levels Achieved", "Percent Complete", "Skill Last Earned (UTC)"],
                [getUserIdForDisplay(users[4]), getName(users[4], false), getName(users[4]), "tag04", "4.0", "5.0", "1.0",  formatDate(today)],
                [getUserIdForDisplay(users[3]), getName(users[3], false), getName(users[3]), "tag03", "3.0", "4.0", "0.7777777778",  formatDate(today)],
                [getUserIdForDisplay(users[2]), getName(users[2], false), getName(users[2]), "tag02", "2.0", "3.0", "0.5555555556", formatDate(today)],
                [getUserIdForDisplay(users[1]), getName(users[1], false), getName(users[1]), "tag01", "1.0", "2.0", "0.3333333333", formatDate(today)],
                [getUserIdForDisplay(users[0]), getName(users[0], false), getName(users[0]), "tag00", "0.0", "1.0", "0.1111111111", formatDate(today)],
                ["For All Dragons Only"],
        ]
        List<List<String>> expectedDataForQuery = [
                ["For All Dragons Only"],
                ["User ID", "Last Name", "First Name", "Org", "Skills Achieved", "Levels Achieved", "Percent Complete", "Skill Last Earned (UTC)"],
                [getUserIdForDisplay(users[0]), getName(users[0], false), getName(users[0]), "tag00", "0.0", "1.0", "0.1111111111", formatDate(today)],
                ["For All Dragons Only"],
        ]

        then:
        validateExport(excelExportSortPointsAsc.file, expectedDataForSortAsc)
        validateExport(excelExportSortPointsDesc.file, expectedDataForSortDesc)
        validateExport(excelExportQueryFilter.file, expectedDataForQuery)
    }

}
