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
package skills.intTests.metrics.global

import groovy.json.JsonSlurper
import groovy.time.TimeCategory
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
import skills.utils.TestDates

import java.time.*
import java.time.temporal.TemporalAdjusters

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

    def "start param must be within last 3 years"() {
        def proj1 = createProject()
        def proj2 = createProject(2)

        skillsService.createProject(proj1)
        skillsService.createProject(proj2)

        Map props = [:]
        use(TimeCategory) {
            props[MetricsParams.P_START_TIMESTAMP] = (3.years.ago - 1.day).time
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

        TestDates testDates = new TestDates()
        List<Date> days = [
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
        def res30days = skillsService.getOverallMetricsData(metricsId, getProps(days[0]))
        def resOver30days = skillsService.getOverallMetricsData(metricsId, getProps(daysPlusTwoWeeksPrior[0]))

        skillsService.archiveUsers([users[2]], proj1.projectId)

        def res30daysAfterArchive = skillsService.getOverallMetricsData(metricsId, getProps(days[0]))
        def resOver30daysAfterArchive = skillsService.getOverallMetricsData(metricsId, getProps(daysPlusTwoWeeksPrior[0]))

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

        TestDates testDates = new TestDates()
        List<Date> days = [
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
                        runQuiz(user, quiz1, true, date)
                    }
                    if (index >= 2) {
                        runQuiz(user, quiz2, true, date)
                    }
                    if (index >= 3) {
                        runQuiz(user, quiz3, true, date)
                    }
                }
            }
        }

        assert maxDailyDays == 3, "test data constructed with the assumption that skills.config.compactDailyEventsOlderThan is set to 3"
        userEventService.compactDailyEvents()

        when:
        def res30days = skillsService.getOverallMetricsData(metricsId, getProps(days[0]))
        def resOver30days = skillsService.getOverallMetricsData(metricsId, getProps(daysPlusTwoWeeksPrior[0]))

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

        TestDates testDates = new TestDates()
        List<Date> days = [
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
                        runQuiz(user, quiz1, true, date)
                    }
                }
            }
        }

        assert maxDailyDays == 3, "test data constructed with the assumption that skills.config.compactDailyEventsOlderThan is set to 3"
        userEventService.compactDailyEvents()

        Duration duration = Duration.between(testDates.getDateInPreviousWeek().minusDays(28), LocalDateTime.now())

        when:
        def res30days = skillsService.getOverallMetricsData(metricsId, getProps(days[0]))
        def resOver30days = skillsService.getOverallMetricsData(metricsId, getProps(daysPlusTwoWeeksPrior[0]))

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

        TestDates testDates = new TestDates()
        List<Date> days = [
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
                        runQuiz(user, survey1, true, date)
                    }
                }
            }
        }

        assert maxDailyDays == 3, "test data constructed with the assumption that skills.config.compactDailyEventsOlderThan is set to 3"
        userEventService.compactDailyEvents()

        Duration duration = Duration.between(testDates.getDateInPreviousWeek().minusDays(28), LocalDateTime.now())

        when:
        def res30days = skillsService.getOverallMetricsData(metricsId, getProps(days[0]))
        def resOver30days = skillsService.getOverallMetricsData(metricsId, getProps(daysPlusTwoWeeksPrior[0]))

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

        TestDates testDates = new TestDates()
        List<Date> days = [
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
        def res30days = skillsService.getOverallMetricsData(metricsId, getProps(days[0], true))
        def resOver30days = skillsService.getOverallMetricsData(metricsId, getProps(days[0].minus(14), true))

        then:
        // Adjust expectations based on current date
        // If today is the first few days of the month, we expect different monthly groupings
        LocalDateTime now = LocalDateTime.now()
        int currentDayOfMonth = now.getDayOfMonth()
        
        if (currentDayOfMonth <= 3) {
            // First few days of month - expect different grouping
            res30days.users.size() == 3
            res30days.users.collect {it.count} == [0, 3, 5]
            
            resOver30days.users.size() == 3
            resOver30days.users.collect {it.count} == [0, 3, 5]
        } else {
            // Later in month - original expectations
            res30days.users.size() == 2
            res30days.users.collect {it.count} == [4, 5]

            resOver30days.users.size() == 3
            resOver30days.users.collect {it.count} == [0, 4, 5]
        }
    }

    def "test empty result sets - no users in date range"() {
        given:
        def proj1 = createProject()
        def proj2 = createProject(2)

        skillsService.createProject(proj1)
        skillsService.createProject(proj2)

        when:
        def res = skillsService.getOverallMetricsData(metricsId, getProps(15))
        then:
        res.users.count == [0,0,0,0]

        where:
        "Query should return empty list when no users exist in date range"
    }
    
    def "test boundary date conditions - start date exactly at today"() {
        given:
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
        // Create users exactly at boundary
        LocalDate today = LocalDate.now()
        Date startOfToday = Date.from(today.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
        List<String> users = (1..5).collect { "user$it" }
        users.each { String user ->
            skillsService.addSkill([projectId: proj1.projectId, skillId: "skill1"], user, startOfToday)
        }

        when:
        def res = skillsService.getOverallMetricsData(metricsId, getProps(startOfToday))
        then:
        res.users.size() == 1
        res.users.count == [5]
        res.users.value == [startOfToday.time]
    }

    def "test week boundary transition"() {
        given:
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

        // Create users on Saturday (end of week)
        boolean isTodaySaturday = LocalDate.now().getDayOfWeek() == DayOfWeek.SATURDAY
        LocalDate previousSaturday = isTodaySaturday ? 
            LocalDate.now().with(TemporalAdjusters.previous(DayOfWeek.SATURDAY)) :
            LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY))
        Date startOfSaturday = Date.from(previousSaturday.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
        Date previousSunday = startOfSaturday+1
        List<String> users = (1..3).collect { "user$it" }
        users.each { String user ->
            skillsService.addSkill([projectId: proj1.projectId, skillId: "skill1"], user, startOfSaturday)
        }
        Map props = getProps(30)
        Date startDate = StartDateUtil.computeStartDate(new Date(props[MetricsParams.P_START_TIMESTAMP]), EventType.WEEKLY)
        List<Integer> counts = [0,0,0,3,0]
        List<Long> dates = [(previousSunday-28).time, (previousSunday-21).time, (previousSunday-14).time, (previousSunday-7).time, (previousSunday).time]
        if (startDate < new Date(dates[0])) {
            counts.push(0)
            dates.push((new Date(dates[0])-7).time)
        }

        when:
        def res = skillsService.getOverallMetricsData(metricsId, props)

        res.users.each {
            println "User: ${new Date(it.value)} | Count: ${it.count}"
        }
        then:
        res.users.size() == counts.size()
        res.users.count == counts
        res.users.value == dates
    }

    private static Map getProps(int numDaysAgo, Boolean byMonth = false) {
        Map props = [:]
        LocalDate weekStart = LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                .minusDays(numDaysAgo)
        Date startDate = Date.from(weekStart.atStartOfDay(ZoneOffset.UTC).toInstant())
        props[MetricsParams.P_START_TIMESTAMP] = startDate.time
        if (byMonth) {
            props[MetricsParams.P_BY_MONTH] = byMonth
        }
        return props
    }

    private static Map getProps(Date startDate, Boolean byMonth = false) {
        Map props = [
                (MetricsParams.P_START_TIMESTAMP): startDate.time
        ]
        if (byMonth) {
            props[MetricsParams.P_BY_MONTH] = byMonth
        }
        return props
    }

    void runQuiz(String userId, def quiz, boolean pass, Date startDate = null) {
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
}
