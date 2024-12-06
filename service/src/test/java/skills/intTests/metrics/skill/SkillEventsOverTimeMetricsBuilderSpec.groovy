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


import groovy.json.JsonSlurper
import groovy.time.TimeCategory
import org.apache.commons.lang3.RandomUtils
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.metrics.builders.MetricsParams
import skills.services.StartDateUtil
import skills.storage.model.EventType

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import java.time.temporal.TemporalField
import java.time.temporal.WeekFields

class SkillEventsOverTimeMetricsBuilderSpec  extends DefaultIntSpec {

    String metricsId = "skillEventsOverTimeChartBuilder"

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

        skillsService.archiveUsers([users[2]], proj.projectId)

        List resAfterArchive = skills.collect {
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

        resAfterArchive[0].countsByDay.collect { it.num } == [2, 1]
        resAfterArchive[0].countsByDay.collect { new Date(it.timestamp) } == [testDates.startOfTwoWeeksAgo.toDate(), testDates.startOfCurrentWeek.toDate()]
        resAfterArchive[0].allEvents

        resAfterArchive[1].countsByDay.collect { it.num } == [2, 1]
        resAfterArchive[1].countsByDay.collect { new Date(it.timestamp) } == [testDates.startOfTwoWeeksAgo.toDate(), testDates.startOfCurrentWeek.toDate()]
        resAfterArchive[1].allEvents

        resAfterArchive[2].countsByDay.collect { it.num } == [2, 1]
        resAfterArchive[2].countsByDay.collect { new Date(it.timestamp) } == [testDates.startOfTwoWeeksAgo.toDate(), testDates.startOfCurrentWeek.toDate()]
        resAfterArchive[2].allEvents

        resAfterArchive[3].countsByDay.collect { it.num } == [3]
        resAfterArchive[3].countsByDay.collect { new Date(it.timestamp) } == [testDates.startOfCurrentWeek.toDate()]
        resAfterArchive[3].allEvents

        resAfterArchive[4].countsByDay.collect { it.num } == []

        resAfterArchive[5].countsByDay.collect { it.num } == []
        resAfterArchive[6].countsByDay.collect { it.num } == []
        resAfterArchive[7].countsByDay.collect { it.num } == []
        resAfterArchive[8].countsByDay.collect { it.num } == []
        resAfterArchive[9].countsByDay.collect { it.num } == []
    }

    def "number events over time - catalog skills"() {
        List<String> users = getRandomUsers(10)
        def proj = SkillsFactory.createProject()
        def proj2 = SkillsFactory.createProject(2)
        def subj2 = SkillsFactory.createSubject(2, 2)
        List<Map> skills = SkillsFactory.createSkills(12)
        skills.each { it.pointIncrement = 100; it.numPerformToCompletion = 1 }

        skillsService.createProject(proj)
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkills(skills)
        skillsService.createProject(proj2)
        skillsService.createSubject(subj2)
        (0..9).each {idx ->
            def it = skills.get(idx)
            skillsService.exportSkillToCatalog(proj.projectId, it.skillId)
            skillsService.importSkillFromCatalog(proj2.projectId, subj2.subjectId, proj.projectId, it.skillId)
        }
        skillsService.finalizeSkillsImportFromCatalog(proj2.projectId, true)

        TestDates testDates = new TestDates()

        List<Date> days = [
                testDates.startOfTwoWeeksAgo.toDate(),
                testDates.startOfTwoWeeksAgo.plusDays(2).toDate(),
                testDates.startOfTwoWeeksAgo.plusDays(4).toDate(),
                testDates.startOfTwoWeeksAgo.plusDays(6).toDate(),
                testDates.startOfCurrentWeek.toDate()]

        def notIncluded1 = skills.get(10)
        def notIncluded2 = skills.get(11)
        //unfinalized imports should not be included in the counts
        skillsService.exportSkillToCatalog(proj.projectId, notIncluded1.skillId)
        skillsService.exportSkillToCatalog(proj.projectId, notIncluded2.skillId)
        skillsService.importSkillFromCatalog(proj2.projectId, subj2.subjectId, proj.projectId, notIncluded1.skillId)
        skillsService.importSkillFromCatalog(proj2.projectId, subj2.subjectId, proj.projectId, notIncluded2.skillId)
        skillsService.addSkill([projectId: proj.projectId, skillId: notIncluded1.skillId], users[0], days[0])
        skillsService.addSkill([projectId: proj.projectId, skillId: notIncluded1.skillId], users[1], days[1])
        skillsService.addSkill([projectId: proj.projectId, skillId: notIncluded1.skillId], users[2], days[2])
        skillsService.addSkill([projectId: proj.projectId, skillId: notIncluded1.skillId], users[3], days[3])
        skillsService.addSkill([projectId: proj.projectId, skillId: notIncluded2.skillId], users[0], days[0])
        skillsService.addSkill([projectId: proj.projectId, skillId: notIncluded2.skillId], users[1], days[1])
        skillsService.addSkill([projectId: proj.projectId, skillId: notIncluded2.skillId], users[2], days[2])
        skillsService.addSkill([projectId: proj.projectId, skillId: notIncluded2.skillId], users[3], days[3])

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
            return skillsService.getMetricsData(proj2.projectId, metricsId, props)
        }

        skillsService.archiveUsers([users[2]], proj.projectId)

        List resAfterArchive = skills.collect {
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

        resAfterArchive[0].countsByDay.collect { it.num } == [2, 1]
        resAfterArchive[0].countsByDay.collect { new Date(it.timestamp) } == [testDates.startOfTwoWeeksAgo.toDate(), testDates.startOfCurrentWeek.toDate()]
        resAfterArchive[0].allEvents

        resAfterArchive[1].countsByDay.collect { it.num } == [2, 1]
        resAfterArchive[1].countsByDay.collect { new Date(it.timestamp) } == [testDates.startOfTwoWeeksAgo.toDate(), testDates.startOfCurrentWeek.toDate()]
        resAfterArchive[1].allEvents

        resAfterArchive[2].countsByDay.collect { it.num } == [2, 1]
        resAfterArchive[2].countsByDay.collect { new Date(it.timestamp) } == [testDates.startOfTwoWeeksAgo.toDate(), testDates.startOfCurrentWeek.toDate()]
        resAfterArchive[2].allEvents

        resAfterArchive[3].countsByDay.collect { it.num } == [3]
        resAfterArchive[3].countsByDay.collect { new Date(it.timestamp) } == [testDates.startOfCurrentWeek.toDate()]
        resAfterArchive[3].allEvents

        resAfterArchive[4].countsByDay.collect { it.num } == []

        resAfterArchive[5].countsByDay.collect { it.num } == []
        resAfterArchive[6].countsByDay.collect { it.num } == []
        resAfterArchive[7].countsByDay.collect { it.num } == []
        resAfterArchive[8].countsByDay.collect { it.num } == []
        resAfterArchive[9].countsByDay.collect { it.num } == []
    }

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

        skillsService.archiveUsers([users[1]], proj.projectId)

        List resAfterArchive = skills.collect {
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

        resAfterArchive[0].countsByDay.collect { it.num } == [1, 1]
        resAfterArchive[0].countsByDay.collect { new Date(it.timestamp) } == [StartDateUtil.computeStartDate(days[1], EventType.DAILY), StartDateUtil.computeStartDate(days[2], EventType.DAILY)]
        resAfterArchive[0].allEvents

        resAfterArchive[1].countsByDay.collect { it.num } == [0, 1]
        resAfterArchive[1].countsByDay.collect { new Date(it.timestamp) } == [StartDateUtil.computeStartDate(days[1], EventType.DAILY), StartDateUtil.computeStartDate(days[2], EventType.DAILY)]
        resAfterArchive[1].allEvents

        resAfterArchive[2].countsByDay == []
    }

    def "number events over time - daily metrics/ - catalog skills"() {
        List<String> users = getRandomUsers(3)
        def proj = SkillsFactory.createProject()
        def proj2 = SkillsFactory.createProject(2)
        def subj2 = SkillsFactory.createSubject(2, 2)
        List<Map> skills = SkillsFactory.createSkills(5)
        skills.each { it.pointIncrement = 100; it.numPerformToCompletion = 2 }

        skillsService.createProject(proj)
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkills(skills)
        skillsService.createProject(proj2)
        skillsService.createSubject(subj2)

        (0..2).each { idx ->
            def it = skills.get(idx)
            skillsService.exportSkillToCatalog(proj.projectId, it.skillId)
            skillsService.importSkillFromCatalog(proj2.projectId, subj2.subjectId, proj.projectId, it.skillId)
        }
        skillsService.finalizeSkillsImportFromCatalog(proj2.projectId, true)


        TestDates testDates = new TestDates()

        List<Date> days = [
                testDates.now.minusDays(2).toDate(),
                testDates.now.minusDays(1).toDate(),
                testDates.now.toDate(),
        ]


        def notIncluded1 = skills.get(3)
        def notIncluded2 = skills.get(4)
        //unfinalized imports should not be included in the counts
        skillsService.exportSkillToCatalog(proj.projectId, notIncluded1.skillId)
        skillsService.exportSkillToCatalog(proj.projectId, notIncluded2.skillId)
        skillsService.importSkillFromCatalog(proj2.projectId, subj2.subjectId, proj.projectId, notIncluded1.skillId)
        skillsService.importSkillFromCatalog(proj2.projectId, subj2.subjectId, proj.projectId, notIncluded2.skillId)
        skillsService.addSkill([projectId: proj.projectId, skillId: notIncluded1.skillId], users[0], days[0])
        skillsService.addSkill([projectId: proj.projectId, skillId: notIncluded1.skillId], users[1], days[1])
        skillsService.addSkill([projectId: proj.projectId, skillId: notIncluded1.skillId], users[2], days[2])
        skillsService.addSkill([projectId: proj.projectId, skillId: notIncluded2.skillId], users[0], days[0])
        skillsService.addSkill([projectId: proj.projectId, skillId: notIncluded2.skillId], users[1], days[1])
        skillsService.addSkill([projectId: proj.projectId, skillId: notIncluded2.skillId], users[2], days[2])

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

        //when skills are imported from the catalog, user performed skill events are only recorded against the original exported skill
        //(i.e., no user performed skill event exists for each imported version), it is expected that requests that involve user performed skills
        //will return all user performed skill events saved against the original exporting skill when a request is made for user performed skill
        //events involving an IMPORTED copy of that skill
        when:
        List res = skills.collect {
            props[MetricsParams.P_SKILL_ID] = it.skillId
            return skillsService.getMetricsData(proj2.projectId, metricsId, props)
        }

        skillsService.archiveUsers([users[1]], proj.projectId)

        List resAfterArchive = skills.collect {
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

        resAfterArchive[0].countsByDay.collect { it.num } == [1, 1]
        resAfterArchive[0].countsByDay.collect { new Date(it.timestamp) } == [StartDateUtil.computeStartDate(days[1], EventType.DAILY), StartDateUtil.computeStartDate(days[2], EventType.DAILY)]
        resAfterArchive[0].allEvents

        resAfterArchive[1].countsByDay.collect { it.num } == [0, 1]
        resAfterArchive[1].countsByDay.collect { new Date(it.timestamp) } == [StartDateUtil.computeStartDate(days[1], EventType.DAILY), StartDateUtil.computeStartDate(days[2], EventType.DAILY)]
        resAfterArchive[1].allEvents

        resAfterArchive[2].countsByDay == []
    }

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

        skillsService.archiveUsers([users[1]], proj.projectId)

        List resAfterArchive = skills.collect {
            props[MetricsParams.P_SKILL_ID] = it.skillId
            return skillsService.getMetricsData(proj.projectId, metricsId, props)
        }

        then:
        res[0].countsByDay.collect { it.num }.sum() == 20
        res[0].allEvents.collect { it.num }.sum() == 60

        resAfterArchive[0].countsByDay.collect { it.num }.sum() == 18
        resAfterArchive[0].allEvents.collect { it.num }.sum() == 54
    }

    def "number of applied events and total events over time for project - catalog skill"() {
        List<String> users = getRandomUsers(10)
        def proj = SkillsFactory.createProject()
        def proj2 = SkillsFactory.createProject(2)
        def subj2 = SkillsFactory.createSubject(2, 2)
        List<Map> skills = SkillsFactory.createSkills(3)
        skills.each { it.pointIncrement = 100; it.numPerformToCompletion = 2 }

        skillsService.createProject(proj)
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkills(skills)
        skillsService.createProject(proj2)
        skillsService.createSubject(subj2)

        skillsService.exportSkillToCatalog(proj.projectId, skills[0].skillId)
        skillsService.importSkillFromCatalog(proj2.projectId, subj2.subjectId, proj.projectId, skills[0].skillId)
        skillsService.finalizeSkillsImportFromCatalog(proj2.projectId, true)

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

        def notIncluded1 = skills.get(1)
        def notIncluded2 = skills.get(2)
        //unfinalized imports should not be included in the counts
        skillsService.exportSkillToCatalog(proj.projectId, notIncluded1.skillId)
        skillsService.exportSkillToCatalog(proj.projectId, notIncluded2.skillId)
        skillsService.importSkillFromCatalog(proj2.projectId, subj2.subjectId, proj.projectId, notIncluded1.skillId)
        skillsService.importSkillFromCatalog(proj2.projectId, subj2.subjectId, proj.projectId, notIncluded2.skillId)
        skillsService.addSkill([projectId: proj.projectId, skillId: notIncluded1.skillId], users[0], days[0])
        skillsService.addSkill([projectId: proj.projectId, skillId: notIncluded1.skillId], users[1], days[1])
        skillsService.addSkill([projectId: proj.projectId, skillId: notIncluded1.skillId], users[2], days[2])
        skillsService.addSkill([projectId: proj.projectId, skillId: notIncluded1.skillId], users[3], days[3])
        skillsService.addSkill([projectId: proj.projectId, skillId: notIncluded2.skillId], users[0], days[0])
        skillsService.addSkill([projectId: proj.projectId, skillId: notIncluded2.skillId], users[1], days[1])
        skillsService.addSkill([projectId: proj.projectId, skillId: notIncluded2.skillId], users[2], days[2])
        skillsService.addSkill([projectId: proj.projectId, skillId: notIncluded2.skillId], users[3], days[3])

        Map props = [:]
        props[MetricsParams.P_SKILL_ID] = skills[0].skillId
        props[MetricsParams.P_START_TIMESTAMP] = LocalDateTime.now().minusDays(7).toDate().time

        when:
        List res = skills.collect {
            props[MetricsParams.P_SKILL_ID] = it.skillId
            return skillsService.getMetricsData(proj2.projectId, metricsId, props)
        }

        skillsService.archiveUsers([users[1]], proj.projectId)

        List resAfterArchive = skills.collect {
            props[MetricsParams.P_SKILL_ID] = it.skillId
            return skillsService.getMetricsData(proj.projectId, metricsId, props)
        }

        then:
        res[0].countsByDay.collect { it.num }.sum() == 20
        res[0].allEvents.collect { it.num }.sum() == 60

        resAfterArchive[0].countsByDay.collect { it.num }.sum() == 18
        resAfterArchive[0].allEvents.collect { it.num }.sum() == 54
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
