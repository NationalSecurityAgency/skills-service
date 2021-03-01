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
package skills.intTests.metrics.skill

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import groovy.json.JsonSlurper
import groovy.time.TimeCategory
import org.apache.commons.lang3.RandomUtils
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.metrics.builders.MetricsParams
import skills.services.StartDateUtil
import skills.storage.model.EventType
import spock.lang.Ignore

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import java.time.temporal.TemporalField
import java.time.temporal.WeekFields

class SkillEventsOverTimeMetricsBuilderSpec  extends DefaultIntSpec {

    String metricsId = "skillEventsOverTimeChartBuilder"

    @Ignore
    def "must supply skillId param"() {
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
        body.explanation == "Metrics[${metricsId}]: Must supply skillId param"
    }

    @Ignore
    def "must supply start param"() {
        def proj = SkillsFactory.createProject()
        List<Map> skills = SkillsFactory.createSkills(1)

        skillsService.createProject(proj)
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkills(skills)

        Map props = [:]
        props.skillId = "foo"

        when:
        skillsService.getMetricsData(proj.projectId, metricsId, props)
        then:
        SkillsClientException e = thrown()
        def body = new JsonSlurper().parseText(e.resBody)
        body.explanation == "Metrics[${metricsId}]: Must supply start param"
    }

    @Ignore
    def "number events over time"() {
        List<String> users = getRandomUsers(10)
        def proj = SkillsFactory.createProject()
        List<Map> skills = SkillsFactory.createSkills(10)
        skills.each { it.pointIncrement = 100; it.numPerformToCompletion = 1 }

        skillsService.createProject(proj)
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkills(skills)

        TestDates testDates = new TestDates()

        List<Date> days = [
                testDates.startOfTwoWeeksAgo.toDate(),
                testDates.startOfTwoWeeksAgo.plusDays(2).toDate(),
                testDates.startOfTwoWeeksAgo.plusDays(4).toDate(),
                testDates.startOfTwoWeeksAgo.plusDays(6).toDate(),
                testDates.startOfCurrentWeek.toDate()]

        days.eachWithIndex { Date date, int index ->
            users.subList(0, index).each { String user ->
                skills.subList(0, index).each { skill ->
                    skillsService.addSkill([projectId: proj.projectId, skillId: skill.skillId], user, date)
                }
            }
        }


        Map props = [:]
        props[MetricsParams.P_SKILL_ID] = skills[0].skillId
        props[MetricsParams.P_START_TIMESTAMP] = LocalDateTime.now().minusDays(7).toDate().time

        when:
        List res = skills.collect {
            props[MetricsParams.P_SKILL_ID] = it.skillId
            return skillsService.getMetricsData(proj.projectId, metricsId, props)
        }

        then:
        res[0].countsByDay.collect { it.num } == [3, 1]
        res[0].countsByDay.collect { new Date(it.timestamp) } == [testDates.startOfTwoWeeksAgo.toDate(), testDates.startOfCurrentWeek.toDate()]
        res[0].allEvents

        res[1].countsByDay.collect { it.num } == [3, 1]
        res[1].countsByDay.collect { new Date(it.timestamp) } == [testDates.startOfTwoWeeksAgo.toDate(), testDates.startOfCurrentWeek.toDate()]
        res[1].allEvents

        res[2].countsByDay.collect { it.num } == [3, 1]
        res[2].countsByDay.collect { new Date(it.timestamp) } == [testDates.startOfTwoWeeksAgo.toDate(), testDates.startOfCurrentWeek.toDate()]
        res[2].allEvents

        res[3].countsByDay.collect { it.num } == [4]
        res[3].countsByDay.collect { new Date(it.timestamp) } == [testDates.startOfCurrentWeek.toDate()]
        res[3].allEvents

        res[4].countsByDay.collect { it.num } == []

        res[5].countsByDay.collect { it.num } == []
        res[6].countsByDay.collect { it.num } == []
        res[7].countsByDay.collect { it.num } == []
        res[8].countsByDay.collect { it.num } == []
        res[9].countsByDay.collect { it.num } == []
    }

    @Ignore
    def "number events over time - daily metrics"() {
        List<String> users = getRandomUsers(3)
        def proj = SkillsFactory.createProject()
        List<Map> skills = SkillsFactory.createSkills(3)
        skills.each { it.pointIncrement = 100; it.numPerformToCompletion = 2 }

        skillsService.createProject(proj)
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkills(skills)

        TestDates testDates = new TestDates()

        List<Date> days = [
            testDates.now.minusDays(2).toDate(),
            testDates.now.minusDays(1).toDate(),
            testDates.now.toDate(),
        ]

        days.eachWithIndex { Date date, int index ->
            users.subList(0, index).each { String user ->
                skills.subList(0, index).each { skill ->
                    skillsService.addSkill([projectId: proj.projectId, skillId: skill.skillId], user, date)
                }
            }
        }

        Map props = [:]
        props[MetricsParams.P_SKILL_ID] = skills[0].skillId
        props[MetricsParams.P_START_TIMESTAMP] = LocalDateTime.now().minusDays(2).toDate().time

        when:
        List res = skills.collect {
            props[MetricsParams.P_SKILL_ID] = it.skillId
            return skillsService.getMetricsData(proj.projectId, metricsId, props)
        }

        then:
        res[0].countsByDay.collect { it.num } == [1, 2]
        res[0].countsByDay.collect { new Date(it.timestamp) } == [StartDateUtil.computeStartDate(days[1], EventType.DAILY), StartDateUtil.computeStartDate(days[2], EventType.DAILY)]
        res[0].allEvents

        res[1].countsByDay.collect { it.num } == [0, 2]
        res[1].countsByDay.collect { new Date(it.timestamp) } == [StartDateUtil.computeStartDate(days[1], EventType.DAILY), StartDateUtil.computeStartDate(days[2], EventType.DAILY)]
        res[1].allEvents

        res[2].countsByDay == []

    }

    @Ignore
    def "number of applied events and total events over time for project"() {
        List<String> users = getRandomUsers(10)
        def proj = SkillsFactory.createProject()
        List<Map> skills = SkillsFactory.createSkills(1)
        skills.each { it.pointIncrement = 100; it.numPerformToCompletion = 2 }

        skillsService.createProject(proj)
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkills(skills)

        List<Date> days

        use(TimeCategory) {
            days = (5..0).collect { int day -> day.days.ago }
            days.eachWithIndex { Date date, int index ->
                users.each {String user ->
                    skills.each { skill ->
                        skillsService.addSkill([projectId: proj.projectId, skillId: skill.skillId], user, date)
                    }
                }
            }
        }

        Map props = [:]
        props[MetricsParams.P_SKILL_ID] = skills[0].skillId
        props[MetricsParams.P_START_TIMESTAMP] = LocalDateTime.now().minusDays(7).toDate().time

        when:
        List res = skills.collect {
            props[MetricsParams.P_SKILL_ID] = it.skillId
            return skillsService.getMetricsData(proj.projectId, metricsId, props)
        }

        then:
        res[0].countsByDay.collect { it.num }.sum() == 20
        res[0].allEvents.collect { it.num }.sum() == 60
    }

    def "zero fill to current day for applied skill events dataset"() {
        List<String> users = getRandomUsers(10)
        def proj = SkillsFactory.createProject()
        List<Map> skills = SkillsFactory.createSkills(1)
        skills.each { it.pointIncrement = 100; it.numPerformToCompletion = 2 }

        skillsService.createProject(proj)
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkills(skills)

        use(TimeCategory) {
            (10..2).collect { int day ->
                Date date = day.days.ago
                def u
                if ( day < 3) {
                    u = users.subList(0, 2)
                } else {
                    u = users.subList(3, 10)
                }
               u.each { String user ->
                    skills.each { skill ->
                        skillsService.addSkill([projectId: proj.projectId, skillId: skill.skillId], user, date)
                    }
                }
            }
        }

        when:
        Map props = [:]
        props[MetricsParams.P_SKILL_ID] = skills[0].skillId
        props[MetricsParams.P_START_TIMESTAMP] = LocalDateTime.now().minusDays(3).toDate().time

        List res = skills.collect {
            props[MetricsParams.P_SKILL_ID] = it.skillId
            return skillsService.getMetricsData(proj.projectId, metricsId, props)
        }

        then:
        res[0].countsByDay.sort { it.day }.last().num == 0
        new Date(res[0].countsByDay.sort { it.day }.last().timestamp) == StartDateUtil.computeStartDate(new Date(), EventType.DAILY)
    }

    private static class TestDates {
        LocalDateTime now;
        LocalDateTime startOfCurrentWeek;
        LocalDateTime startOfTwoWeeksAgo;

        public TestDates() {
            now = LocalDateTime.now()
            startOfCurrentWeek = StartDateUtil.computeStartDate(now.toDate(), EventType.DAILY).toLocalDateTime().with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
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

    }

}
