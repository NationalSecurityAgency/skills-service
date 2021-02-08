/**
 * Copyright 2020 SkillTree
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
package skills.intTests.metrics.project

import groovy.json.JsonSlurper
import groovy.time.TimeCategory
import org.apache.commons.lang3.RandomUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.metrics.builders.MetricsParams
import skills.services.LockingService
import skills.services.StartDateUtil
import skills.services.UserEventService
import skills.storage.model.EventType

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import java.time.temporal.TemporalField
import java.time.temporal.WeekFields

class DistinctUsersOverTimeMetricsBuilderSpec extends DefaultIntSpec {

    @Autowired
    UserEventService userEventService

    @Value('#{"${skills.config.compactDailyEventsOlderThan}"}')
    int maxDailyDays

    String metricsId = "distinctUsersOverTimeForProject"

    LockingService mockLock = Mock()

    def setup() {
        userEventService.lockingService = mockLock;
    }

    def "must supply start param"() {
        def proj = SkillsFactory.createProject()
        List<Map> skills = SkillsFactory.createSkills(1)

        skillsService.createProject(proj)
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkills(skills)

        Map props = [:]

        when:
        skillsService.getMetricsData(proj.projectId, metricsId, props)
        then:
        SkillsClientException e = thrown()
        def body = new JsonSlurper().parseText(e.resBody)
        body.explanation == "Metrics[${metricsId}]: Must supply start param"
    }

    def "start param must be within last 3 years"() {
        def proj = SkillsFactory.createProject()
        List<Map> skills = SkillsFactory.createSkills(1)

        skillsService.createProject(proj)
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkills(skills)

        Map props = [:]
        use(TimeCategory) {
            props[MetricsParams.P_START_TIMESTAMP] = (3.years.ago - 1.day).time
        }

        when:
        skillsService.getMetricsData(proj.projectId, metricsId, props)
        then:
        SkillsClientException e = thrown()
        def body = new JsonSlurper().parseText(e.resBody)
        body.explanation.contains("Metrics[${metricsId}]: start param timestamp must be within last 3 years.")
    }

    def "start param in the future will not return anything"() {
        def proj = SkillsFactory.createProject()
        List<Map> skills = SkillsFactory.createSkills(10)
        skills.each { it.pointIncrement = 100; it.numPerformToCompletion = 10 }

        skillsService.createProject(proj)
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkills(skills)

        List<Date> days
        List<String> users = (1..10).collect { "user$it" }
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

        Map props = [:]
        use(TimeCategory) {
            props[MetricsParams.P_START_TIMESTAMP] = 1.day.from.now.time
        }

        when:
        def res5Days = skillsService.getMetricsData(proj.projectId, metricsId, props)
        then:
        !res5Days
    }

    def "number of users growing over few days"() {
        List<String> users = getRandomUsers(10)
        def proj = SkillsFactory.createProject()
        List<Map> skills = SkillsFactory.createSkills(10)
        skills.each { it.pointIncrement = 100; it.numPerformToCompletion = 10 }


        skillsService.createProject(proj)
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkills(skills)

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

        use(TimeCategory) {
            days.eachWithIndex { Date date, int index ->
                users.subList(0, index).each { String user ->
                    skills.subList(0, index).each { skill ->
                        skillsService.addSkill([projectId: proj.projectId, skillId: skill.skillId], user, date)
                    }
                }
            }
        }

        assert maxDailyDays == 3, "test data constructed with the assumption that skills.config.compactDailyEventsOlderThan is set to 3"
        userEventService.compactDailyEvents()

        Duration duration = Duration.between(testDates.getDateInPreviousWeek().minusDays(28), LocalDateTime.now())

        when:
        def res30days = skillsService.getMetricsData(proj.projectId, metricsId, getProps(duration.toDays().toInteger()))
        def resOver30days = skillsService.getMetricsData(proj.projectId, metricsId, getProps(duration.toDays().toInteger()+14))

        then:

        res30days.size() == 5
        res30days.collect {it.count} == [1, 2, 3, 4, 5]
        res30days.collect {it.value} == days.subList(1, days.size()).collect { StartDateUtil.computeStartDate(it, EventType.WEEKLY).time}

        resOver30days.size() == 7
        resOver30days.collect {it.count} == [0, 0, 1, 2, 3, 4, 5]
        resOver30days.collect {it.value} == [days[0].minus(7), *days].collect { StartDateUtil.computeStartDate(it, EventType.WEEKLY).time}
    }

    def "number of users growing over few days - specific skill"() {
        List<String> users = getRandomUsers(10)
        def proj = SkillsFactory.createProject()
        List<Map> skills = SkillsFactory.createSkills(10)
        skills.each { it.pointIncrement = 100; it.numPerformToCompletion = 10 }

        skillsService.createProject(proj)
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkills(skills)

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

        use(TimeCategory) {
            days.eachWithIndex { Date date, int index ->
                users.subList(0, index).each { String user ->
                    skills.subList(0, index).each { skill ->
                        skillsService.addSkill([projectId: proj.projectId, skillId: skill.skillId], user, date)
                    }
                }
            }
        }

        assert maxDailyDays == 3, "test data constructed with the assumption that skills.config.compactDailyEventsOlderThan is set to 3"
        userEventService.compactDailyEvents()

        Duration duration = Duration.between(testDates.getDateInPreviousWeek().minusDays(28), LocalDateTime.now())

        when:
        def res30days = skillsService.getMetricsData(proj.projectId, metricsId, getProps(duration.toDays().toInteger(), skills[1].skillId))
        def resOver30days = skillsService.getMetricsData(proj.projectId, metricsId, getProps(duration.toDays().toInteger()+14, skills[1].skillId))
        def skill3res30days = skillsService.getMetricsData(proj.projectId, metricsId, getProps(duration.toDays().toInteger(), skills[2].skillId))

        then:
        res30days.size() == 5
        res30days.collect {it.count} == [0, 2, 3, 4, 5]
        res30days.collect {it.value} == days.subList(1, days.size()).collect { StartDateUtil.computeStartDate(it, EventType.WEEKLY).time}

        resOver30days.size() == 7
        resOver30days.collect {it.count} == [0, 0, 0, 2, 3, 4, 5]
        resOver30days.collect {it.value} == [days[0].minus(7), *days].collect { StartDateUtil.computeStartDate(it, EventType.WEEKLY).time}

        skill3res30days.size() == 5
        skill3res30days.collect {it.count} == [0, 0, 3, 4, 5]
        skill3res30days.collect {it.value} == days.subList(1, days.size()).collect { StartDateUtil.computeStartDate(it, EventType.WEEKLY).time}


    }

    private Map getProps(int numDaysAgo, String skillId = null) {
        Map props = [:]
        use(TimeCategory) {
            props[MetricsParams.P_START_TIMESTAMP] = numDaysAgo.days.ago.time
            if (skillId) {
                props[MetricsParams.P_SKILL_ID] = skillId
            }
        }
        return props
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
