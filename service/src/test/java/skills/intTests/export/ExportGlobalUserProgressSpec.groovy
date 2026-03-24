/**
 * Copyright 2026 SkillTree
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

import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsService

import static skills.intTests.utils.SkillsFactory.*
import static skills.metrics.GlobalProgressMetricsService.getUSER_PREF_GLOBAL_METRICS_EXCLUSION
import static skills.metrics.GlobalProgressMetricsService.getUSER_PREF_GLOBAL_METRICS_EXCLUSION
import static skills.metrics.GlobalProgressMetricsService.getUSER_PREF_GLOBAL_METRICS_EXCLUSION

class ExportGlobalUserProgressSpec extends ExportBaseIntSpec {

    def "export global users progress"() {
        def project1 = createProject(1)
        def subject1 = createSubject(1, 1)
        def skill1 = createSkill(1, 1, 1, 0, 5)
        skill1.pointIncrement = 50
        def skill2 = createSkill(1, 1, 2, 0, 5)

        def project2 = createProject(2)
        def subject2 = createSubject(2, 1)
        def skill3 = createSkill(2, 1, 1, 0, 10)

        skillsService.createProjectAndSubjectAndSkills(project1, subject1, [skill1, skill2])
        skillsService.createProjectAndSubjectAndSkills(project2, subject2, [skill3])

        def quiz1 = createQuiz(1)
        def survey1 = createSurvey(2)

        def users = getRandomUsers(5).sort()
        def user1 = users[0]
        def user2 = users[1]
        def user3 = users[2]
        def user4 = users[3]
        def user5 = users[4]

        when:
        skillsService.addSkill(skill1, user1, fiveDaysAgo)
        skillsService.addSkill(skill1, user1, oneDayAgo)
        skillsService.addSkill(skill1, user2, today)
        skillsService.addSkill(skill2, user2, today)
        skillsService.addSkill(skill3, user3, today)

        runQuizOrSurvey(quiz1, user1, true, true)
        runQuizOrSurvey(quiz1, user2, true, false)
        runQuizOrSurvey(quiz1, user3, false, true)
        runQuizOrSurvey(survey1, user4, false, true)
        runQuizOrSurvey(survey1, user5, false, false)

        def excelExport = skillsService.getGlobalUserProgressExcelExport()

        then:
        validateExport(excelExport.file, [
                ["For All Dragons Only"],
                ["User ID", "Last Name", "First Name", "Org", "Quiz Attempts", "Quizzes Passed", "Quizzes Failed", "Quizzes In Progress",
                 "Surveys", "Surveys Completed", "Surveys In Progress", "Projects", "Project Levels Earned",
                 "Subject Levels Earned", "Skills Earned", "Project Badges Earned", "Global Badges Earned"],
                [getUserIdForDisplay(user1), getName(user1, false), getName(user1), "", "1.0", "1.0", "0.0", "0.0",
                 "0.0", "0.0", "0.0", "1.0", "2.0", "2.0", "0.0", "0.0", "0.0"],
                [getUserIdForDisplay(user2), getName(user2, false), getName(user2), "", "1.0", "0.0", "0.0", "1.0",
                 "0.0", "0.0", "0.0", "1.0", "1.0", "1.0", "0.0", "0.0", "0.0"],
                [getUserIdForDisplay(user3), getName(user3, false), getName(user3), "", "1.0", "0.0", "1.0", "0.0",
                 "0.0", "0.0", "0.0", "1.0", "1.0", "1.0", "0.0", "0.0", "0.0"],
                [getUserIdForDisplay(user4), getName(user4, false), getName(user4), "", "0.0", "0.0", "0.0", "0.0",
                 "1.0", "1.0", "0.0", "0.0", "0.0", "0.0", "0.0", "0.0", "0.0"],
                [getUserIdForDisplay(user5), getName(user5, false), getName(user5), "", "0.0", "0.0", "0.0", "0.0",
                 "1.0", "0.0", "1.0", "0.0", "0.0", "0.0", "0.0", "0.0", "0.0"],
                ["For All Dragons Only"],
        ])
    }

    def "export global users progress with user tags"() {
        def project1 = createProject(1)
        def subject1 = createSubject(1, 1)
        def skill1 = createSkill(1, 1, 1, 0, 10)
        def skill2 = createSkill(1, 1, 2, 0, 10)

        def project2 = createProject(2)
        def subject2 = createSubject(2, 1)
        def skill3 = createSkill(2, 1, 1, 0, 10)

        skillsService.createProjectAndSubjectAndSkills(project1, subject1, [skill1, skill2])
        skillsService.createProjectAndSubjectAndSkills(project2, subject2, [skill3])

        def quiz1 = createQuiz(1)

        List<String> users = getRandomUsers(3).sort()
        skillsService.addSkill(skill1, users[0])
        skillsService.addSkill(skill2, users[1])
        skillsService.addSkill(skill3, users[2])

        runQuizOrSurvey(quiz1, users[0], true, true)

        users.eachWithIndex { userId, idx ->
            String tagValue = "tag${idx}"
            rootSkillsService.saveUserTag(userId, "dutyOrganization", [tagValue]);
        }

        when:
        def excelExport = skillsService.getGlobalUserProgressExcelExport()

        then:
        validateExport(excelExport.file, [
                ["For All Dragons Only"],
                ["User ID", "Last Name", "First Name", "Org", "Quiz Attempts", "Quizzes Passed", "Quizzes Failed", "Quizzes In Progress",
                 "Surveys", "Surveys Completed", "Surveys In Progress", "Projects", "Project Levels Earned",
                 "Subject Levels Earned", "Skills Earned", "Project Badges Earned", "Global Badges Earned"],
                [getUserIdForDisplay(users[0]), getName(users[0], false), getName(users[0]), "tag0", "1.0", "1.0", "0.0", "0.0",
                 "0.0", "0.0", "0.0", "1.0", "0.0", "0.0", "0.0", "0.0", "0.0"],
                [getUserIdForDisplay(users[1]), getName(users[1], false), getName(users[1]), "tag1", "0.0", "0.0", "0.0", "0.0",
                 "0.0", "0.0", "0.0", "1.0", "0.0", "0.0", "0.0", "0.0", "0.0"],
                [getUserIdForDisplay(users[2]), getName(users[2], false), getName(users[2]), "tag2", "0.0", "0.0", "0.0", "0.0",
                 "0.0", "0.0", "0.0", "1.0", "1.0", "1.0", "0.0", "0.0", "0.0"],
                ["For All Dragons Only"],
        ])
    }

    def "export global users progress and filter by user tag"() {
        def project1 = createProject(1)
        def subject1 = createSubject(1, 1)
        def skill1 = createSkill(1, 1, 1, 0, 10)

        skillsService.createProjectAndSubjectAndSkills(project1, subject1, [skill1])

        List<String> users = getRandomUsers(3).sort()
        users.each { userId ->
            skillsService.addSkill(skill1, userId)
        }

        users.eachWithIndex { userId, idx ->
            String tagValue = "tag${idx}"
            rootSkillsService.saveUserTag(userId, "dutyOrganization", [tagValue]);
        }

        when:
        def excelExport = skillsService.getGlobalUserProgressExcelExport('userIdForDisplay', true, '', 'tag1')

        then:
        validateExport(excelExport.file, [
                ["For All Dragons Only"],
                ["User ID", "Last Name", "First Name", "Org", "Quiz Attempts", "Quizzes Passed", "Quizzes Failed", "Quizzes In Progress",
                 "Surveys", "Surveys Completed", "Surveys In Progress", "Projects", "Project Levels Earned",
                 "Subject Levels Earned", "Skills Earned", "Project Badges Earned", "Global Badges Earned"],
                [getUserIdForDisplay(users[1]), getName(users[1], false), getName(users[1]), "tag1", "0.0", "0.0", "0.0", "0.0",
                 "0.0", "0.0", "0.0", "1.0", "1.0", "1.0", "0.0", "0.0", "0.0"],
                ["For All Dragons Only"],
        ])
    }

    def "export global users progress and filter by user query"() {
        def project1 = createProject(1)
        def subject1 = createSubject(1, 1)
        def skill1 = createSkill(1, 1, 1, 0, 10)

        skillsService.createProjectAndSubjectAndSkills(project1, subject1, [skill1])

        List<String> users = getRandomUsers(3, true, [DEFAULT_ROOT_USER_ID, SkillsService.UseParams.DEFAULT_USER_NAME, 'service', 'skills', 'bob', 'user-info-service.test', 'user-skill1', 'user-skill2']).sort()
        users.each { userId ->
            skillsService.addSkill(skill1, userId)
        }

        when:
        def excelExport = skillsService.getGlobalUserProgressExcelExport('userIdForDisplay', true, getUserIdForDisplay(users[0]), '')

        then:
        validateExport(excelExport.file, [
                ["For All Dragons Only"],
                ["User ID", "Last Name", "First Name", "Org", "Quiz Attempts", "Quizzes Passed", "Quizzes Failed", "Quizzes In Progress",
                 "Surveys", "Surveys Completed", "Surveys In Progress", "Projects", "Project Levels Earned",
                 "Subject Levels Earned", "Skills Earned", "Project Badges Earned", "Global Badges Earned"],
                [getUserIdForDisplay(users[0]), getName(users[0], false), getName(users[0]), "", "0.0", "0.0", "0.0", "0.0",
                 "0.0", "0.0", "0.0", "1.0", "1.0", "1.0", "0.0", "0.0", "0.0"],
                ["For All Dragons Only"],
        ])
    }

    def "export global users progress with badges and achievements"() {
        def project1 = createProject(1)
        def subject1 = createSubject(1, 1)
        def skill1 = createSkill(1, 1, 1, 0, 3)
        def skill2 = createSkill(1, 1, 2, 0, 10)

        skillsService.createProjectAndSubjectAndSkills(project1, subject1, [skill1, skill2])

        def badge1 = createBadge(1, 1)
        skillsService.createBadge(badge1)
        skillsService.assignSkillToBadge([projectId: badge1.projectId, badgeId: badge1.badgeId, skillId: skill1.skillId])
        badge1.enabled = true
        skillsService.updateBadge(badge1)

        def users = getRandomUsers(2).sort()
        def user1 = users[0]
        def user2 = users[1]

        when:
        skillsService.addSkill(skill1, user1, fiveDaysAgo)
        skillsService.addSkill(skill1, user1, oneDayAgo)
        skillsService.addSkill(skill1, user1, today)
        skillsService.addSkill(skill2, user1, today)
        skillsService.addSkill(skill1, user2, today)

        def excelExport = skillsService.getGlobalUserProgressExcelExport()

        then:
        validateExport(excelExport.file, [
                ["For All Dragons Only"],
                ["User ID", "Last Name", "First Name", "Org", "Quiz Attempts", "Quizzes Passed", "Quizzes Failed", "Quizzes In Progress",
                 "Surveys", "Surveys Completed", "Surveys In Progress", "Projects", "Project Levels Earned",
                 "Subject Levels Earned", "Skills Earned", "Project Badges Earned", "Global Badges Earned"],
                [getUserIdForDisplay(user1), getName(user1, false), getName(user1), "", "0.0", "0.0", "0.0", "0.0",
                 "0.0", "0.0", "0.0", "1.0", "2.0", "2.0", "1.0", "1.0", "0.0"],
                [getUserIdForDisplay(user2), getName(user2, false), getName(user2), "", "0.0", "0.0", "0.0", "0.0",
                 "0.0", "0.0", "0.0", "1.0", "0.0", "0.0", "0.0", "0.0", "0.0"],
                ["For All Dragons Only"],
        ])
    }

    def "export global users progress with multiple projects and quizzes"() {
        def project1 = createProject(1)
        def subject1 = createSubject(1, 1)
        def skill1 = createSkill(1, 1, 1, 0)
        skill1.pointIncrement = 100

        def project2 = createProject(2)
        def subject2 = createSubject(2, 1)
        def skill2 = createSkill(2, 1, 1, 0)
        skill2.pointIncrement = 100

        skillsService.createProjectAndSubjectAndSkills(project1, subject1, [skill1])
        skillsService.createProjectAndSubjectAndSkills(project2, subject2, [skill2])

        def globalBadge = createBadge(3, 1)
        skillsService.createGlobalBadge(globalBadge)
        skillsService.assignSkillToGlobalBadge([badgeId: globalBadge.badgeId, projectId: project1.projectId, skillId: skill1.skillId])
        skillsService.assignProjectLevelToGlobalBadge(badgeId: globalBadge.badgeId, projectId: project2.projectId, level: '5')
        globalBadge.enabled = true
        skillsService.updateGlobalBadge(globalBadge)

        def quiz1 = createQuiz(1)
        def quiz2 = createQuiz(2)

        def users = getRandomUsers(2).sort()
        def user1 = users[0]
        def user2 = users[1]

        when:
        skillsService.addSkill(skill1, user1)
        skillsService.addSkill(skill2, user2)

        runQuizOrSurvey(quiz1, user1, true, true)
        runQuizOrSurvey(quiz1, user2, true, false)
        runQuizOrSurvey(quiz2, user2, true, true)

        def excelExport = skillsService.getGlobalUserProgressExcelExport()

        then:
        validateExport(excelExport.file, [
                ["For All Dragons Only"],
                ["User ID", "Last Name", "First Name", "Org", "Quiz Attempts", "Quizzes Passed", "Quizzes Failed", "Quizzes In Progress",
                 "Surveys", "Surveys Completed", "Surveys In Progress", "Projects", "Project Levels Earned",
                 "Subject Levels Earned", "Skills Earned", "Project Badges Earned", "Global Badges Earned"],
                [getUserIdForDisplay(user1), getName(user1, false), getName(user1), "", "1.0", "1.0", "0.0", "0.0",
                 "0.0", "0.0", "0.0", "1.0", "5.0", "5.0", "1.0", "0.0", "0.0"],
                [getUserIdForDisplay(user2), getName(user2, false), getName(user2), "", "2.0", "1.0", "0.0", "1.0",
                 "0.0", "0.0", "0.0", "1.0", "5.0", "5.0", "1.0", "0.0", "0.0"],
                ["For All Dragons Only"],
        ])
    }

    def "export global users progress with multiple projects and quizzes - project excluded via global metrics setting"() {
        def project1 = createProject(1)
        def subject1 = createSubject(1, 1)
        def skill1 = createSkill(1, 1, 1, 0)
        skill1.pointIncrement = 100

        def project2 = createProject(2)
        def subject2 = createSubject(2, 1)
        def skill2 = createSkill(2, 1, 1, 0)
        skill2.pointIncrement = 100

        skillsService.createProjectAndSubjectAndSkills(project1, subject1, [skill1])
        skillsService.createProjectAndSubjectAndSkills(project2, subject2, [skill2])

        def globalBadge = createBadge(3, 1)
        skillsService.createGlobalBadge(globalBadge)
        skillsService.assignSkillToGlobalBadge([badgeId: globalBadge.badgeId, projectId: project1.projectId, skillId: skill1.skillId])
        skillsService.assignProjectLevelToGlobalBadge(badgeId: globalBadge.badgeId, projectId: project2.projectId, level: '5')
        globalBadge.enabled = true
        skillsService.updateGlobalBadge(globalBadge)

        def quiz1 = createQuiz(1)
        def quiz2 = createQuiz(2)

        def users = getRandomUsers(2).sort()
        def user1 = users[0]
        def user2 = users[1]

        when:
        skillsService.addSkill(skill1, user1)
        skillsService.addSkill(skill2, user2)

        runQuizOrSurvey(quiz1, user1, true, true)
        runQuizOrSurvey(quiz1, user2, true, false)
        runQuizOrSurvey(quiz2, user2, true, true)

        skillsService.addOrUpdateGlobalMetricsUserSettings([
                [setting: USER_PREF_GLOBAL_METRICS_EXCLUSION, value: true, projectId: project1.projectId ],
        ])

        def excelExport = skillsService.getGlobalUserProgressExcelExport()

        then:
        validateExport(excelExport.file, [
                ["For All Dragons Only"],
                ["User ID", "Last Name", "First Name", "Org", "Quiz Attempts", "Quizzes Passed", "Quizzes Failed", "Quizzes In Progress",
                 "Surveys", "Surveys Completed", "Surveys In Progress", "Projects", "Project Levels Earned",
                 "Subject Levels Earned", "Skills Earned", "Project Badges Earned", "Global Badges Earned"],
                [getUserIdForDisplay(user1), getName(user1, false), getName(user1), "", "1.0", "1.0", "0.0", "0.0",
                 "0.0", "0.0", "0.0", "0.0", "0.0", "0.0", "0.0", "0.0", "0.0"],
                [getUserIdForDisplay(user2), getName(user2, false), getName(user2), "", "2.0", "1.0", "0.0", "1.0",
                 "0.0", "0.0", "0.0", "1.0", "5.0", "5.0", "1.0", "0.0", "0.0"],
                ["For All Dragons Only"],
        ])
    }

    def "export global users progress with multiple projects and quizzes - quiz excluded via global metrics setting"() {
        def project1 = createProject(1)
        def subject1 = createSubject(1, 1)
        def skill1 = createSkill(1, 1, 1, 0)
        skill1.pointIncrement = 100

        def project2 = createProject(2)
        def subject2 = createSubject(2, 1)
        def skill2 = createSkill(2, 1, 1, 0)
        skill2.pointIncrement = 100

        skillsService.createProjectAndSubjectAndSkills(project1, subject1, [skill1])
        skillsService.createProjectAndSubjectAndSkills(project2, subject2, [skill2])

        def globalBadge = createBadge(3, 1)
        skillsService.createGlobalBadge(globalBadge)
        skillsService.assignSkillToGlobalBadge([badgeId: globalBadge.badgeId, projectId: project1.projectId, skillId: skill1.skillId])
        skillsService.assignProjectLevelToGlobalBadge(badgeId: globalBadge.badgeId, projectId: project2.projectId, level: '5')
        globalBadge.enabled = true
        skillsService.updateGlobalBadge(globalBadge)

        def quiz1 = createQuiz(1)
        def quiz2 = createQuiz(2)

        def users = getRandomUsers(2).sort()
        def user1 = users[0]
        def user2 = users[1]

        when:
        skillsService.addSkill(skill1, user1)
        skillsService.addSkill(skill2, user2)

        runQuizOrSurvey(quiz1, user1, true, true)
        runQuizOrSurvey(quiz1, user2, true, false)
        runQuizOrSurvey(quiz2, user2, true, true)

        skillsService.addOrUpdateGlobalMetricsUserSettings([
                [setting: USER_PREF_GLOBAL_METRICS_EXCLUSION, value: true, quizId: quiz1.quizId ],
        ])

        def excelExport = skillsService.getGlobalUserProgressExcelExport()

        then:
        validateExport(excelExport.file, [
                ["For All Dragons Only"],
                ["User ID", "Last Name", "First Name", "Org", "Quiz Attempts", "Quizzes Passed", "Quizzes Failed", "Quizzes In Progress",
                 "Surveys", "Surveys Completed", "Surveys In Progress", "Projects", "Project Levels Earned",
                 "Subject Levels Earned", "Skills Earned", "Project Badges Earned", "Global Badges Earned"],
                [getUserIdForDisplay(user1), getName(user1, false), getName(user1), "", "0.0", "0.0", "0.0", "0.0",
                 "0.0", "0.0", "0.0", "1.0", "5.0", "5.0", "1.0", "0.0", "0.0"],
                [getUserIdForDisplay(user2), getName(user2, false), getName(user2), "", "1.0", "1.0", "0.0", "0.0",
                 "0.0", "0.0", "0.0", "1.0", "5.0", "5.0", "1.0", "0.0", "0.0"],
                ["For All Dragons Only"],
        ])
    }

    def "export users progress for UC protected project"() {
        def users = getRandomUsers(3).sort()
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

        def excelExport = pristineDragonsUser.getGlobalUserProgressExcelExport()

        then:
        validateExport(excelExport.file, [
                ["For Divine Dragon Only"],
                ["User ID", "Last Name", "First Name", "Org", "Quiz Attempts", "Quizzes Passed", "Quizzes Failed", "Quizzes In Progress",
                 "Surveys", "Surveys Completed", "Surveys In Progress", "Projects", "Project Levels Earned",
                 "Subject Levels Earned", "Skills Earned", "Project Badges Earned", "Global Badges Earned"],
                [getUserIdForDisplay(user1), getName(user1, false), getName(user1), "", "0.0", "0.0", "0.0", "0.0",
                 "0.0", "0.0", "0.0", "1.0", "2.0", "2.0", "0.0", "0.0", "0.0"],
                [getUserIdForDisplay(user2), getName(user2, false), getName(user2), "", "0.0", "0.0", "0.0", "0.0",
                 "0.0", "0.0", "0.0", "1.0", "1.0", "1.0", "0.0", "0.0", "0.0"],
                ["For Divine Dragon Only"],
        ])
    }

    def "export users progress for UC protected global badge"() {
        def users = getRandomUsers(3).sort()
        def user1 = users[0]
        def user2 = users[1]

        SkillsService pristineDragonsUser = createService(users[2])
        rootSkillsService.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])
        rootSkillsService.saveUserTag(rootSkillsService.userName, 'dragons', ['DivineDragon'])
        def project1 = createProject(1)
        project1.enableProtectedUserCommunity = true
        def subject1 = createSubject(1, 1)
        def skill1 = createSkill(1, 1, 1, 0)
        skill1.pointIncrement = 100

        def project2 = createProject(2)
        def subject2 = createSubject(2, 1)
        def skill2 = createSkill(2, 1, 1, 0)
        skill2.pointIncrement = 100

        pristineDragonsUser.createProjectAndSubjectAndSkills(project1, subject1, [skill1])
        pristineDragonsUser.createProjectAndSubjectAndSkills(project2, subject2, [skill2])

        def globalBadge = createBadge(3, 1)
        pristineDragonsUser.createGlobalBadge(globalBadge)
        globalBadge.enableProtectedUserCommunity = true
        pristineDragonsUser.updateGlobalBadge(globalBadge)

        pristineDragonsUser.assignSkillToGlobalBadge([badgeId: globalBadge.badgeId, projectId: project1.projectId, skillId: skill1.skillId])

        globalBadge.enabled = true
        pristineDragonsUser.updateGlobalBadge(globalBadge)

        def quiz1 = createQuiz(1, pristineDragonsUser)
        def quiz2 = createQuiz(2, pristineDragonsUser)

        when:
        pristineDragonsUser.addSkill(skill1, user1)
        pristineDragonsUser.addSkill(skill2, user2)

        runQuizOrSurvey(quiz1, user1, true, true, pristineDragonsUser)
        runQuizOrSurvey(quiz1, user2, true, false, pristineDragonsUser)
        runQuizOrSurvey(quiz2, user2, true, true, pristineDragonsUser)

        def excelExport = pristineDragonsUser.getGlobalUserProgressExcelExport()

        then:
        validateExport(excelExport.file, [
                ["For Divine Dragon Only"],
                ["User ID", "Last Name", "First Name", "Org", "Quiz Attempts", "Quizzes Passed", "Quizzes Failed", "Quizzes In Progress",
                 "Surveys", "Surveys Completed", "Surveys In Progress", "Projects", "Project Levels Earned",
                 "Subject Levels Earned", "Skills Earned", "Project Badges Earned", "Global Badges Earned"],
                [getUserIdForDisplay(user1), getName(user1, false), getName(user1), "", "1.0", "1.0", "0.0", "0.0",
                 "0.0", "0.0", "0.0", "1.0", "5.0", "5.0", "1.0", "0.0", "1.0"],
                [getUserIdForDisplay(user2), getName(user2, false), getName(user2), "", "2.0", "1.0", "0.0", "1.0",
                 "0.0", "0.0", "0.0", "1.0", "5.0", "5.0", "1.0", "0.0", "0.0"],
                ["For Divine Dragon Only"],
        ])
    }

    def "export users progress for UC protected quiz"() {
        def users = getRandomUsers(3).sort()
        def user1 = users[0]
        def user2 = users[1]

        SkillsService pristineDragonsUser = createService(users[2])
        rootSkillsService.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])
        rootSkillsService.saveUserTag(rootSkillsService.userName, 'dragons', ['DivineDragon'])
        def project1 = createProject(1)
        def subject1 = createSubject(1, 1)
        def skill1 = createSkill(1, 1, 1, 0)
        skill1.pointIncrement = 100

        def project2 = createProject(2)
        def subject2 = createSubject(2, 1)
        def skill2 = createSkill(2, 1, 1, 0)
        skill2.pointIncrement = 100

        pristineDragonsUser.createProjectAndSubjectAndSkills(project1, subject1, [skill1])
        pristineDragonsUser.createProjectAndSubjectAndSkills(project2, subject2, [skill2])

        def globalBadge = createBadge(3, 1)
        pristineDragonsUser.createGlobalBadge(globalBadge)

        pristineDragonsUser.assignSkillToGlobalBadge([badgeId: globalBadge.badgeId, projectId: project1.projectId, skillId: skill1.skillId])

        globalBadge.enabled = true
        pristineDragonsUser.updateGlobalBadge(globalBadge)

        def quiz1 = createQuiz(1, pristineDragonsUser)
        quiz1.enableProtectedUserCommunity = true
        pristineDragonsUser.createQuizDef(quiz1, quiz1.quizId)
        def quiz2 = createQuiz(2, pristineDragonsUser)

        when:
        pristineDragonsUser.addSkill(skill1, user1)
        pristineDragonsUser.addSkill(skill2, user2)

        runQuizOrSurvey(quiz1, user1, true, true, pristineDragonsUser)
        runQuizOrSurvey(quiz1, user2, true, false, pristineDragonsUser)
        runQuizOrSurvey(quiz2, user2, true, true, pristineDragonsUser)

        def excelExport = pristineDragonsUser.getGlobalUserProgressExcelExport()

        then:
        validateExport(excelExport.file, [
                ["For Divine Dragon Only"],
                ["User ID", "Last Name", "First Name", "Org", "Quiz Attempts", "Quizzes Passed", "Quizzes Failed", "Quizzes In Progress",
                 "Surveys", "Surveys Completed", "Surveys In Progress", "Projects", "Project Levels Earned",
                 "Subject Levels Earned", "Skills Earned", "Project Badges Earned", "Global Badges Earned"],
                [getUserIdForDisplay(user1), getName(user1, false), getName(user1), "", "1.0", "1.0", "0.0", "0.0",
                 "0.0", "0.0", "0.0", "1.0", "5.0", "5.0", "1.0", "0.0", "1.0"],
                [getUserIdForDisplay(user2), getName(user2, false), getName(user2), "", "2.0", "1.0", "0.0", "1.0",
                 "0.0", "0.0", "0.0", "1.0", "5.0", "5.0", "1.0", "0.0", "0.0"],
                ["For Divine Dragon Only"],
        ])
    }

    def createQuiz(int num, SkillsService serviceToUse = null) {
        serviceToUse = serviceToUse ?: skillsService
        def quiz = QuizDefFactory.createQuiz(num)
        quiz.name = "My Quiz ${num}".toString()
        serviceToUse.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(num, 2, 2)
        serviceToUse.createQuizQuestionDefs(questions)
        return quiz
    }

    def createSurvey(int num, SkillsService serviceToUse = null) {
        serviceToUse = serviceToUse ?: skillsService
        def survey = QuizDefFactory.createQuizSurvey(num)
        survey.name = "My Survey ${num}".toString()
        serviceToUse.createQuizDef(survey)
        def question1 = QuizDefFactory.createSingleChoiceSurveyQuestion(num, 1, 2)
        def question2 = QuizDefFactory.createSingleChoiceSurveyQuestion(num, 2, 2)
        serviceToUse.createQuizQuestionDefs([question1, question2])
        return survey
    }

    def runQuizOrSurvey(def quiz, String userId, boolean pass = true, boolean complete = true, SkillsService serviceToUse = null) {
        serviceToUse = serviceToUse ?: skillsService
        def quizAttempt =  serviceToUse.startQuizAttemptForUserId(quiz.quizId, userId).body
        serviceToUse.reportQuizAnswerForUserId(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, userId)
        serviceToUse.reportQuizAnswerForUserId(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[pass ? 0 : 1].id, userId)
        if (complete) {
            serviceToUse.completeQuizAttemptForUserId(quiz.quizId, quizAttempt.id, userId).body
        }
    }

}
