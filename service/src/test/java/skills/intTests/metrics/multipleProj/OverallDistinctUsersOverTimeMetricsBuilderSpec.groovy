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
package skills.intTests.metrics.multipleProj

import groovy.json.JsonSlurper
import groovy.time.TimeCategory
import org.apache.commons.lang3.RandomUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.metrics.builders.MetricsParams
import skills.quizLoading.QuizSettings
import skills.services.LockingService
import skills.services.StartDateUtil
import skills.services.UserEventService
import skills.storage.model.EventType
import skills.storage.repos.UserQuizAttemptRepo

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import java.time.temporal.TemporalField
import java.time.temporal.WeekFields

import static skills.intTests.utils.SkillsFactory.*

class OverallDistinctUsersOverTimeMetricsBuilderSpec extends DefaultIntSpec {

    @Autowired
    UserEventService userEventService

    @Autowired
    UserQuizAttemptRepo userQuizAttemptRepo

    @Value('#{"${skills.config.compactDailyEventsOlderThan}"}')
    int maxDailyDays

    String metricsId = "overallDistinctUsersOverTimeMetricsBuilder"

    LockingService mockLock = Mock()

    def setup() {
        userEventService.lockingService = mockLock;
    }

    def "must supply start param"() {
        def proj1 = createProject()
        def proj2 = createProject(2)

        skillsService.createProject(proj1)
        skillsService.createProject(proj2)

        Map props = [:]

        when:
        skillsService.getOverallMetricsData(metricsId, props)
        then:
        SkillsClientException e = thrown()
        def body = new JsonSlurper().parseText(e.resBody)
        body.explanation == "Metrics[${metricsId}]: Must supply start param"
    }

    def "must supply projectIds or quizIds param"() {
        def proj1 = createProject()
        def proj2 = createProject(2)

        skillsService.createProject(proj1)
        skillsService.createProject(proj2)

        Map props = [:]
        use(TimeCategory) {
            props[MetricsParams.P_START_TIMESTAMP] = 30.days.ago.time
        }

        when:
        skillsService.getOverallMetricsData(metricsId, props)
        then:
        SkillsClientException e = thrown()
        def body = new JsonSlurper().parseText(e.resBody)
        body.explanation == "Metrics[${metricsId}]: Must supply ${MetricsParams.P_PROJECT_IDS} or ${MetricsParams.P_QUIZ_IDS} param"
    }

    def "start param must be within last 3 years"() {
        def proj1 = createProject()
        def proj2 = createProject(2)

        skillsService.createProject(proj1)
        skillsService.createProject(proj2)

        Map props = [:]
        use(TimeCategory) {
            props[MetricsParams.P_START_TIMESTAMP] = (3.years.ago - 1.day).time
            props[MetricsParams.P_PROJECT_IDS] = "${proj1.projectId},${proj2.projectId}"
        }

        when:
        skillsService.getOverallMetricsData(metricsId, props)
        then:
        SkillsClientException e = thrown()
        def body = new JsonSlurper().parseText(e.resBody)
        body.explanation.contains("Metrics[${metricsId}]: start param timestamp must be within last 3 years.")
    }

    def "start param in the future will not return anything"() {
        def proj1 = createProject()
        def proj2 = createProject(2)
        List<Map> skills = createSkills(10)
        skills.each { it.pointIncrement = 100; it.numPerformToCompletion = 10 }
        List<Map> skills2 = createSkills(10, 2)
        skills2.each { it.pointIncrement = 100; it.numPerformToCompletion = 10 }

        skillsService.createProject(proj1)
        skillsService.createProject(proj2)
        skillsService.createSubject(createSubject())
        skillsService.createSubject(createSubject(2))
        skillsService.createSkills(skills)
        skillsService.createSkills(skills2)

        List<Date> days
        List<String> users = (1..10).collect { "user$it" }
        use(TimeCategory) {
            days = (5..0).collect { int day -> day.days.ago }
            days.eachWithIndex { Date date, int index ->
                users.subList(0, index).each { String user ->
                    skills.subList(0, index).each { skill ->
                        skillsService.addSkill([projectId: proj1.projectId, skillId: skill.skillId], user, date)
                        skillsService.addSkill([projectId: proj2.projectId, skillId: skill.skillId], user, date)
                    }
                }
            }
        }

        Map props = [:]
        use(TimeCategory) {
            props[MetricsParams.P_START_TIMESTAMP] = 1.day.from.now.time
            props[MetricsParams.P_PROJECT_IDS] = "${proj1.projectId},${proj2.projectId}"
        }

        when:
        def res5Days = skillsService.getOverallMetricsData(metricsId, props)
        then:
        res5Days == [users: []]
    }

    def "number of users growing over few days - multiple projects"() {
        List<String> users = getRandomUsers(10)
        def proj1 = createProject()
        def proj2 = createProject(2)
        def proj3 = createProject(3)
        List<Map> skills = createSkills(10)
        skills.each { it.pointIncrement = 100; it.numPerformToCompletion = 10 }
        List<Map> skills2 = createSkills(10, 2)
        skills2.each { it.pointIncrement = 100; it.numPerformToCompletion = 10 }
        List<Map> skills3 = createSkills(10, 3)
        skills3.each { it.pointIncrement = 100; it.numPerformToCompletion = 10 }

        skillsService.createProject(proj1)
        skillsService.createProject(proj2)
        skillsService.createProject(proj3)
        skillsService.createSubject(createSubject())
        skillsService.createSubject(createSubject(2))
        skillsService.createSubject(createSubject(3))
        skillsService.createSkills(skills)
        skillsService.createSkills(skills2)
        skillsService.createSkills(skills3)

        List<Date> days
        TestDates testDates = new TestDates()
        days = [
                testDates.getDateInPreviousWeek().minusDays(28).toDate(),
                testDates.getDateInPreviousWeek().minusDays(21).toDate(),
                testDates.getDateInPreviousWeek().minusDays(14).toDate(),
                testDates.getDateInPreviousWeek().minusDays(7).toDate(),
                testDates.getDateInPreviousWeek().toDate(),
                testDates.getDateWithinCurrentWeek().toDate(),
        ]
        List<Date> daysPlusTwoWeeksPrior = [days[0].minus(14), days[0].minus(7), *days]

        use(TimeCategory) {
            days.eachWithIndex { Date date, int index ->
                users.subList(0, index).each { String user ->
                    skills.subList(0, index).each { skill ->
                        skillsService.addSkill([projectId: proj1.projectId, skillId: skill.skillId], user, date)
                        skillsService.addSkill([projectId: proj2.projectId, skillId: skill.skillId], user, date)
                        // Don't add to proj3 for first few days to test varying counts
                        if (index >= 2) {
                            skillsService.addSkill([projectId: proj3.projectId, skillId: skill.skillId], user, date)
                        }
                    }
                }
            }
        }

        assert maxDailyDays == 3, "test data constructed with the assumption that skills.config.compactDailyEventsOlderThan is set to 3"
        userEventService.compactDailyEvents()

        Duration duration = Duration.between(testDates.getDateInPreviousWeek().minusDays(28), LocalDateTime.now())

        when:
        def res30days = skillsService.getOverallMetricsData(metricsId, getProps(duration.toDays().toInteger(), "${proj1.projectId},${proj2.projectId},${proj3.projectId}", null))
        def resOver30days = skillsService.getOverallMetricsData(metricsId, getProps(duration.toDays().toInteger()+14, "${proj1.projectId},${proj2.projectId},${proj3.projectId}", null))

        skillsService.archiveUsers([users[2]], proj1.projectId)

        def res30daysAfterArchive = skillsService.getOverallMetricsData(metricsId, getProps(duration.toDays().toInteger(), "${proj1.projectId},${proj2.projectId},${proj3.projectId}", null))
        def resOver30daysAfterArchive = skillsService.getOverallMetricsData(metricsId, getProps(duration.toDays().toInteger()+14, "${proj1.projectId},${proj2.projectId},${proj3.projectId}", null))

        then:
        res30days.users.size() == 6
        res30days.users.collect {it.count} == [0, 1, 2, 3, 4, 5]
        res30days.users.collect {it.value} == days.collect { StartDateUtil.computeStartDate(it, EventType.WEEKLY).time}

        resOver30days.users.size() == 8
        resOver30days.users.collect {it.count} == [0, 0, 0, 1, 2, 3, 4, 5]
        resOver30days.users.collect {it.value} == daysPlusTwoWeeksPrior.collect { StartDateUtil.computeStartDate(it, EventType.WEEKLY).time}

        res30daysAfterArchive.users.size() == 6
        res30daysAfterArchive.users.collect {it.count} == [0, 1, 2, 2, 3, 4]
        res30daysAfterArchive.users.collect {it.value} == days.collect { StartDateUtil.computeStartDate(it, EventType.WEEKLY).time}

        resOver30daysAfterArchive.users.size() == 8
        resOver30daysAfterArchive.users.collect {it.count} == [0, 0, 0, 1, 2, 2, 3, 4]
        resOver30daysAfterArchive.users.collect {it.value} == daysPlusTwoWeeksPrior.collect { StartDateUtil.computeStartDate(it, EventType.WEEKLY).time}
    }

    def "number of users growing over few days - multiple quizzes"() {
        List<String> users = getRandomUsers(10)
        def proj1 = createProject()
        def proj2 = createProject(2)

        skillsService.createProject(proj1)
        skillsService.createProject(proj2)

        // Create quizzes
        def quiz1 = QuizDefFactory.createQuiz(1)
        def quiz2 = QuizDefFactory.createQuiz(2)
        def quiz3 = QuizDefFactory.createQuiz(3)

        skillsService.createQuizDef(quiz1)
        skillsService.createQuizDef(quiz2)
        skillsService.createQuizDef(quiz3)

        [quiz1, quiz2, quiz3]. each { quiz ->
            skillsService.saveQuizSettings(quiz.quizId, [
                    [setting: QuizSettings.MultipleTakes.setting, value: true],
            ])
        }

        // Create quiz questions
        def questions1 = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        def questions2 = QuizDefFactory.createChoiceQuestions(2, 2, 2)
        def questions3 = QuizDefFactory.createChoiceQuestions(3, 2, 2)
        skillsService.createQuizQuestionDefs(questions1)
        skillsService.createQuizQuestionDefs(questions2)
        skillsService.createQuizQuestionDefs(questions3)

        def quizInfo1 = skillsService.getQuizInfo(quiz1.quizId)
        def quizInfo2 = skillsService.getQuizInfo(quiz2.quizId)
        def quizInfo3 = skillsService.getQuizInfo(quiz3.quizId)

        List<Date> days
        TestDates testDates = new TestDates()
        days = [
                testDates.getDateInPreviousWeek().minusDays(28).toDate(),
                testDates.getDateInPreviousWeek().minusDays(21).toDate(),
                testDates.getDateInPreviousWeek().minusDays(14).toDate(),
                testDates.getDateInPreviousWeek().minusDays(7).toDate(),
                testDates.getDateInPreviousWeek().toDate(),
                testDates.getDateWithinCurrentWeek().toDate(),
        ]
        List<Date> daysPlusTwoWeeksPrior = [days[0].minus(14), days[0].minus(7), *days]

        use(TimeCategory) {
            days.eachWithIndex { Date date, int index ->
                users.subList(0, index).each { String user ->
                    if (index >= 1) {
                        runQuiz(user, quiz1, quizInfo1, true, date)
                    }
                    if (index >= 2) {
                        runQuiz(user, quiz2, quizInfo2, true, date)
                    }
                    if (index >= 3) {
                        runQuiz(user, quiz3, quizInfo3, true, date)
                    }
                }
            }
        }

        assert maxDailyDays == 3, "test data constructed with the assumption that skills.config.compactDailyEventsOlderThan is set to 3"
        userEventService.compactDailyEvents()

        Duration duration = Duration.between(testDates.getDateInPreviousWeek().minusDays(28), LocalDateTime.now())

        when:
        def res30days = skillsService.getOverallMetricsData(metricsId, getProps(duration.toDays().toInteger(), null, "${quiz1.quizId},${quiz2.quizId},${quiz3.quizId}"))
        def resOver30days = skillsService.getOverallMetricsData(metricsId, getProps(duration.toDays().toInteger()+14, null, "${quiz1.quizId},${quiz2.quizId},${quiz3.quizId}"))

        then:
        res30days.users.size() == 6
        res30days.users.collect {it.count} == [0, 1, 2, 3, 4, 5]
        res30days.users.collect {it.value} == days.collect { StartDateUtil.computeStartDate(it, EventType.WEEKLY).time}

        resOver30days.users.size() == 8
        resOver30days.users.collect {it.count} == [0, 0, 0, 1, 2, 3, 4, 5]
        resOver30days.users.collect {it.value} == daysPlusTwoWeeksPrior.collect { StartDateUtil.computeStartDate(it, EventType.WEEKLY).time}
    }

    def "number of users growing over few days - mixed projects and quizzes"() {
        List<String> users = getRandomUsers(10)
        def proj1 = createProject()
        def proj2 = createProject(2)

        skillsService.createProject(proj1)
        skillsService.createProject(proj2)
        skillsService.createSubject(createSubject())
        skillsService.createSubject(createSubject(2))

        List<Map> skills = createSkills(5)
        skills.each { it.pointIncrement = 100; it.numPerformToCompletion = 10 }
        List<Map> skills2 = createSkills(5, 2)
        skills2.each { it.pointIncrement = 100; it.numPerformToCompletion = 10 }

        skillsService.createSkills(skills)
        skillsService.createSkills(skills2)

        // Create quiz
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)

        // Create quiz questions
        def questions1 = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions1)

        def quizInfo1 = skillsService.getQuizInfo(quiz1.quizId)

        List<Date> days

        TestDates testDates = new TestDates()

        days = [
                testDates.getDateInPreviousWeek().minusDays(28).toDate(),
                testDates.getDateInPreviousWeek().minusDays(21).toDate(),
                testDates.getDateInPreviousWeek().minusDays(14).toDate(),
                testDates.getDateInPreviousWeek().minusDays(7).toDate(),
                testDates.getDateInPreviousWeek().toDate(),
                testDates.getDateWithinCurrentWeek().toDate(),
        ]
        List<Date> daysPlusTwoWeeksPrior = [days[0].minus(14), days[0].minus(7), *days]

        use(TimeCategory) {
            days.eachWithIndex { Date date, int index ->
                users.subList(0, index).each { String user ->
                    // Add skills for first half of users
                    if (index < users.size() / 2) {
                        skills.subList(0, index).each { skill ->
                            skillsService.addSkill([projectId: proj1.projectId, skillId: skill.skillId], user, date)
                            skillsService.addSkill([projectId: proj2.projectId, skillId: skill.skillId], user, date)
                        }
                    }
                    // Add quiz for second half of users
                    if (index >= users.size() / 2) {
                        runQuiz(user, quiz1, quizInfo1, true, date)
                    }
                }
            }
        }

        assert maxDailyDays == 3, "test data constructed with the assumption that skills.config.compactDailyEventsOlderThan is set to 3"
        userEventService.compactDailyEvents()

        Duration duration = Duration.between(testDates.getDateInPreviousWeek().minusDays(28), LocalDateTime.now())

        when:
        def res30days = skillsService.getOverallMetricsData(metricsId, getProps(duration.toDays().toInteger(), "${proj1.projectId},${proj2.projectId}", quiz1.quizId))
        def resOver30days = skillsService.getOverallMetricsData(metricsId, getProps(duration.toDays().toInteger()+14, "${proj1.projectId},${proj2.projectId}", quiz1.quizId))

        then:
        res30days.users.size() == 6
        res30days.users.collect {it.count} == [0, 1, 2, 3, 4, 5]
        res30days.users.collect {it.value} == days.collect { StartDateUtil.computeStartDate(it, EventType.WEEKLY).time}

        resOver30days.users.size() == 8
        resOver30days.users.collect {it.count} == [0, 0, 0, 1, 2, 3, 4, 5]
        resOver30days.users.collect {it.value} == daysPlusTwoWeeksPrior.collect { StartDateUtil.computeStartDate(it, EventType.WEEKLY).time}
    }

    def "number of users growing over few days - mixed projects and surveys"() {
        List<String> users = getRandomUsers(10)
        def proj1 = createProject()
        def proj2 = createProject(2)

        skillsService.createProject(proj1)
        skillsService.createProject(proj2)
        skillsService.createSubject(createSubject())
        skillsService.createSubject(createSubject(2))

        List<Map> skills = createSkills(5)
        skills.each { it.pointIncrement = 100; it.numPerformToCompletion = 10 }
        List<Map> skills2 = createSkills(5, 2)
        skills2.each { it.pointIncrement = 100; it.numPerformToCompletion = 10 }

        skillsService.createSkills(skills)
        skillsService.createSkills(skills2)

        // Create quiz
        def survey1 = QuizDefFactory.createQuizSurvey(1)
        skillsService.createQuizDef(survey1)

        // Create quiz questions
        def question1 = QuizDefFactory.createSingleChoiceSurveyQuestion(1, 1, 2)
        def question2 = QuizDefFactory.createSingleChoiceSurveyQuestion(1, 2, 2)
        skillsService.createQuizQuestionDefs([question1, question2])
        def quizInfo1 = skillsService.getQuizInfo(survey1.quizId)

        List<Date> days
        TestDates testDates = new TestDates()
        days = [
                testDates.getDateInPreviousWeek().minusDays(28).toDate(),
                testDates.getDateInPreviousWeek().minusDays(21).toDate(),
                testDates.getDateInPreviousWeek().minusDays(14).toDate(),
                testDates.getDateInPreviousWeek().minusDays(7).toDate(),
                testDates.getDateInPreviousWeek().toDate(),
                testDates.getDateWithinCurrentWeek().toDate(),
        ]
        List<Date> daysPlusTwoWeeksPrior = [days[0].minus(14), days[0].minus(7), *days]

        use(TimeCategory) {
            days.eachWithIndex { Date date, int index ->
                users.subList(0, index).each { String user ->
                    // Add skills for first half of users
                    if (index < users.size() / 2) {
                        skills.subList(0, index).each { skill ->
                            skillsService.addSkill([projectId: proj1.projectId, skillId: skill.skillId], user, date)
                            skillsService.addSkill([projectId: proj2.projectId, skillId: skill.skillId], user, date)
                        }
                    }
                    // Add quiz for second half of users
                    if (index >= users.size() / 2) {
                        runQuiz(user, survey1, quizInfo1, true, date)
                    }
                }
            }
        }

        assert maxDailyDays == 3, "test data constructed with the assumption that skills.config.compactDailyEventsOlderThan is set to 3"
        userEventService.compactDailyEvents()

        Duration duration = Duration.between(testDates.getDateInPreviousWeek().minusDays(28), LocalDateTime.now())

        when:
        def res30days = skillsService.getOverallMetricsData(metricsId, getProps(duration.toDays().toInteger(), "${proj1.projectId},${proj2.projectId}", survey1.quizId))
        def resOver30days = skillsService.getOverallMetricsData(metricsId, getProps(duration.toDays().toInteger()+14, "${proj1.projectId},${proj2.projectId}", survey1.quizId))

        then:
        res30days.users.size() == 6
        res30days.users.collect {it.count} == [0, 1, 2, 3, 4, 5]
        res30days.users.collect {it.value} == days.collect { StartDateUtil.computeStartDate(it, EventType.WEEKLY).time}

        resOver30days.users.size() == 8
        resOver30days.users.collect {it.count} == [0, 0, 0, 1, 2, 3, 4, 5]
        resOver30days.users.collect {it.value} == daysPlusTwoWeeksPrior.collect { StartDateUtil.computeStartDate(it, EventType.WEEKLY).time}
    }

    def "number of users growing over few days - grouped by month"() {
        List<String> users = getRandomUsers(10)
        def proj1 = createProject()
        def proj2 = createProject(2)
        List<Map> skills = createSkills(10)
        skills.each { it.pointIncrement = 100; it.numPerformToCompletion = 10 }
        List<Map> skills2 = createSkills(10, 2)
        skills2.each { it.pointIncrement = 100; it.numPerformToCompletion = 10 }

        skillsService.createProject(proj1)
        skillsService.createProject(proj2)
        skillsService.createSubject(createSubject())
        skillsService.createSubject(createSubject(2))
        skillsService.createSkills(skills)
        skillsService.createSkills(skills2)

        List<Date> days

        TestDates testDates = new TestDates()

        days = [
                testDates.getFirstOfMonth(1).toDate(),
                testDates.getFirstOfMonth(1).plusDays(7).toDate(),
                testDates.getFirstOfMonth(1).plusDays(14).toDate(),
                testDates.getFirstOfMonth(1).plusDays(21).toDate(),
                testDates.getFirstOfMonth(1).plusDays(27).toDate(),
                testDates.getFirstOfMonth().toDate(),
        ]

        use(TimeCategory) {
            days.eachWithIndex { Date date, int index ->
                users.subList(0, index).each { String user ->
                    skills.subList(0, index).each { skill ->
                        skillsService.addSkill([projectId: proj1.projectId, skillId: skill.skillId], user, date)
                        skillsService.addSkill([projectId: proj2.projectId, skillId: skill.skillId], user, date)
                    }
                }
            }
        }

        assert maxDailyDays == 3, "test data constructed with the assumption that skills.config.compactDailyEventsOlderThan is set to 3"
        userEventService.compactDailyEvents()

        Duration duration = Duration.between(testDates.getFirstOfMonth(1), LocalDateTime.now())

        when:
        def res30days = skillsService.getOverallMetricsData(metricsId, getProps(duration.toDays().toInteger(), "${proj1.projectId},${proj2.projectId}", null, true))
        def resOver30days = skillsService.getOverallMetricsData(metricsId, getProps(duration.toDays().toInteger()+14, "${proj1.projectId},${proj2.projectId}", null, true))

        then:
        res30days.users.size() == 2
        res30days.users.collect {it.count} == [4, 5]

        resOver30days.users.size() == 3
        resOver30days.users.collect {it.count} == [0, 4, 5]
    }

    private Map getProps(int numDaysAgo, String projectIds = null, String quizIds = null, Boolean byMonth = false) {
        Map props = [:]
        use(TimeCategory) {
            if (projectIds) {
                props[MetricsParams.P_PROJECT_IDS] = projectIds
            }
            if (quizIds) {
                props[MetricsParams.P_QUIZ_IDS] = quizIds
            }
            props[MetricsParams.P_BY_MONTH] = byMonth
        }
        return props
    }

    void runQuiz(String userId, def quiz, def quizInfo, boolean pass, Date startDate = null) {
        def quizAttempt = skillsService.startQuizAttemptForUserId(quiz.quizId, userId).body
        skillsService.reportQuizAnswerForUserId(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, userId)
        skillsService.reportQuizAnswerForUserId(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[pass ? 0 : 1].id, userId)
        skillsService.completeQuizAttemptForUserId(quiz.quizId, quizAttempt.id, userId).body

        if(startDate) {
            def userQuizAttempt = userQuizAttemptRepo.findById(quizAttempt.id).get()
            userQuizAttempt.started = startDate
            userQuizAttemptRepo.save(userQuizAttempt)
        }
    }

    private static class TestDates {
        LocalDateTime now;
        LocalDateTime startOfCurrentWeek;
        LocalDateTime startOfTwoWeeksAgo;

        public TestDates() {
            now = LocalDateTime.now()
            startOfCurrentWeek = LocalDateTime.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
            startOfTwoWeeksAgo = startOfCurrentWeek.minusWeeks(1)
        }

        LocalDateTime getFirstOfMonth(Integer monthsAgo = 0) {
            if(now.month.value > monthsAgo) {
                return now.withMonth(now.month.value - monthsAgo).withDayOfMonth(1)
            } else {
                def monthDifference = (now.month.value - monthsAgo) + 12
                return now.withYear(now.getYear() - 1).withMonth(monthDifference).withDayOfMonth(1)
            }
        }

        LocalDateTime getDateWithinCurrentWeek(boolean allowFutureDate=false) {
            if(now.getDayOfWeek() == DayOfWeek.SUNDAY) {
                if (allowFutureDate) {
                    return now.plusDays(RandomUtils.nextInt(1, 6))
                }
                return now//nothing we can do
            } else {
                //us days of week are sun-saturday as 1-7
                TemporalField dayOfWeekField = WeekFields.of(Locale.US).dayOfWeek()
                int currentDayOfWeek = now.get(dayOfWeekField)

                if (allowFutureDate) {
                    int randomDay = -1
                    while ((randomDay = RandomUtils.nextInt(1, 7)) == currentDayOfWeek) {
                        //
                    }
                    return now.with(dayOfWeekField, randomDay)
                }

                return now.with(dayOfWeekField, RandomUtils.nextInt(1,currentDayOfWeek))
            }
        }

        LocalDateTime getDateInPreviousWeek(Date olderThan=null){
            TemporalField dayOfWeekField = WeekFields.of(Locale.US).dayOfWeek()
            if (olderThan) {
                LocalDateTime retVal
                while((retVal = startOfTwoWeeksAgo.with(dayOfWeekField, RandomUtils.nextInt(1,7))).isAfter(olderThan.toLocalDateTime())){
                    //loop until we get a date that is before the target
                }
                return retVal
            } else {
                return startOfTwoWeeksAgo.with(dayOfWeekField, RandomUtils.nextInt(1, 7))
            }
        }
    }
}
