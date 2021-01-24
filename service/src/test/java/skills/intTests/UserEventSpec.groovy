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
package skills.intTests

import org.apache.commons.lang3.RandomUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.services.UserEventService
import skills.storage.model.DayCountItem
import skills.storage.model.EventType
import skills.storage.model.SkillDef
import skills.storage.model.UserEvent
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.UserEventsRepo

import java.text.DateFormat
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import java.time.temporal.TemporalField
import java.time.temporal.WeekFields
import java.util.stream.Stream

class UserEventSpec extends DefaultIntSpec {

    @Autowired
    UserEventsRepo userEventsRepo

    @Autowired
    UserEventService eventService

    @Autowired
    SkillDefRepo skillDefRepo

    @Value('#{"${skills.config.compactDailyEventsOlderThan}"}')
    int maxDailyDays

    @Autowired
    private PlatformTransactionManager transactionManager;

    def "make sure daily events are compacted into weekly events"() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager)
        Map proj = SkillsFactory.createProject(42)
        Map subject = SkillsFactory.createSubject(42)
        Map skill = SkillsFactory.createSkill(42,1,1,0,40, 0)

        skillsService.createProject(proj)
        skillsService.createSubject(subject)
        skillsService.createSkill(skill)

        when:
        LocalDateTime now = LocalDateTime.now()
        String userId = getRandomUsers(1)[0]
        skillsService.addSkill(skill, userId)
        skillsService.addSkill(skill, userId)
        skillsService.addSkill(skill, userId, now.minusDays(1).toDate())

        LocalDateTime aWeekAgo = now.minusDays(maxDailyDays+15)
        (0..14).collect{
            skillsService.addSkill(skill, userId, aWeekAgo.minusDays(it).toDate())
        }

        SkillDef skillDef = skillDefRepo.findByProjectIdAndSkillIdAndType(proj.projectId, skill.skillId, SkillDef.ContainerType.Skill)
        Integer skillRefId = skillDef.id
        int preCompactDailyCount = 0
        int preCompactWeeklyCount = 0
        int maxCount = 0

        transactionTemplate.execute({
            Stream<UserEvent> preCompactionDailyEvents = userEventsRepo.findAllBySkillRefIdAndEventType(skillRefId, EventType.DAILY)
            Stream<UserEvent> preCompactioWeeklyEvents = userEventsRepo.findAllBySkillRefIdAndEventType(skillRefId, EventType.WEEKLY)

            preCompactionDailyEvents.forEach( { UserEvent event ->
                preCompactDailyCount++
                maxCount = Math.max(maxCount, event.count)
            })
            preCompactioWeeklyEvents.forEach({UserEvent event ->
                preCompactioWeeklyEvents++
            })
        })

        eventService.compactDailyEvents()

        int postCompactDailyCount = 0
        int postCompactionWeeklyCount = 0
        LocalDateTime oldest = LocalDateTime.now().minusDays(maxDailyDays)
        transactionTemplate.execute({
            Stream<UserEvent> postCompactionDailyEvents = userEventsRepo.findAllBySkillRefIdAndEventType(skillRefId, EventType.DAILY)
            postCompactionDailyEvents.forEach( {UserEvent event ->
                assert oldest.isBefore(event.start.toLocalDateTime())
                postCompactDailyCount++
            })
            Stream<UserEvent> postCompactionWeeklyEvents = userEventsRepo.findAllBySkillRefIdAndEventType(skillRefId, EventType.WEEKLY)
            postCompactionWeeklyEvents.forEach({UserEvent event ->
                postCompactionWeeklyCount++
                assert oldest.isAfter(event.stop.toLocalDateTime())
            })
        })


        then:
        preCompactDailyCount == 17
        preCompactWeeklyCount == 0
        postCompactDailyCount == 2
        postCompactionWeeklyCount == 3
    }

    def "project event counts spanning compactDailyEventsOlderThan produces accurate results"() {
        Map proj = SkillsFactory.createProject(42)
        Map subject = SkillsFactory.createSubject(42)
        Map skill = SkillsFactory.createSkill(42,1,1,0,40, 0)

        skillsService.createProject(proj)
        skillsService.createSubject(subject)
        skillsService.createSkill(skill)

        assert maxDailyDays == 3, "test data is structured around compactDailyEventsOlderThan == 3"

        def userIds = getRandomUsers(2)

        SkillDef skillDef = skillDefRepo.findByProjectIdAndSkillIdAndType(proj.projectId, skill.skillId, SkillDef.ContainerType.Skill)
        Integer rawId = skillDef.id

        TestDates testDates = new TestDates()

        skillsService.addSkill(skill, userIds[0], testDates.now.toDate())
        skillsService.addSkill(skill, userIds[1], testDates.now.toDate())

        eventService.recordEvent(rawId, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)
        eventService.recordEvent(rawId, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)

        //the above DAILY events should get merged with this weekly event due to overlapping start/end
        eventService.recordEvent(rawId, userIds[0], testDates.startOfCurrentWeek.toDate(), 1, EventType.WEEKLY)

        eventService.recordEvent(rawId, userIds[1], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(rawId, userIds[0], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)

        when:

        List<DayCountItem> results = eventService.getUserEventCountsForProject(proj.projectId, testDates.now.minusDays(300).toDate())

        then:
        results.size() == 2
        results[0].count == 5
        results[0].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.startOfCurrentWeek.toDate())
        results[1].count == 2
        results[1].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.startOfTwoWeeksAgo.toDate())
    }

    def "project event count for metrics newer then compactDailyEventsOlderThan range produces accurate results"() {
        Map proj = SkillsFactory.createProject(42)
        Map subject = SkillsFactory.createSubject(42)
        Map skill = SkillsFactory.createSkill(42,1,1,0,40, 0)

        skillsService.createProject(proj)
        skillsService.createSubject(subject)
        skillsService.createSkill(skill)

        assert maxDailyDays == 3, "test data is structured around compactDailyEventsOlderThan == 3"

        def userIds = getRandomUsers(2)

        SkillDef skillDef = skillDefRepo.findByProjectIdAndSkillIdAndType(proj.projectId, skill.skillId, SkillDef.ContainerType.Skill)
        Integer rawId = skillDef.id

        TestDates testDates = new TestDates()

        skillsService.addSkill(skill, userIds[0], testDates.now.toDate())
        skillsService.addSkill(skill, userIds[1], testDates.now.toDate())

        eventService.recordEvent(rawId, userIds[0], testDates.now.minusDays(1).toDate(), 1, EventType.DAILY)
        eventService.recordEvent(rawId, userIds[0], testDates.now.minusDays(1).toDate(), 3, EventType.DAILY)

        //the below counts should not be included in the results
        eventService.recordEvent(rawId, userIds[0], testDates.startOfCurrentWeek.toDate(), 1, EventType.WEEKLY)

        eventService.recordEvent(rawId, userIds[1], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(rawId, userIds[0], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)

        when:

        List<DayCountItem> results = eventService.getUserEventCountsForProject(proj.projectId, testDates.now.minusDays(2).toDate())

        then:
        results.size() == 2
        results[0].count == 2
        results[0].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.toDate())
        results[1].count == 4
        results[1].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.minusDays(1).toDate())
    }

    def "skill event counts spanning compactDailyEventsOlderThan produces accurate results"() {
        Map proj = SkillsFactory.createProject(42)
        Map subject = SkillsFactory.createSubject(42)
        Map skill = SkillsFactory.createSkill(42,1,1,0,40, 0)
        Map skill2 = SkillsFactory.createSkill(42,1,2,0,40, 0)

        skillsService.createProject(proj)
        skillsService.createSubject(subject)
        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)

        assert maxDailyDays == 3, "test data is structured around compactDailyEventsOlderThan == 3"

        def userIds = getRandomUsers(2)

        SkillDef skillDef = skillDefRepo.findByProjectIdAndSkillIdAndType(proj.projectId, skill.skillId, SkillDef.ContainerType.Skill)
        Integer rawId = skillDef.id

        SkillDef skillDef2 = skillDefRepo.findByProjectIdAndSkillIdAndType(proj.projectId, skill2.skillId, SkillDef.ContainerType.Skill)
        Integer rawId2 = skillDef2.id

        TestDates testDates = new TestDates()

        skillsService.addSkill(skill, userIds[0], testDates.now.toDate())
        skillsService.addSkill(skill, userIds[1], testDates.now.toDate())

        eventService.recordEvent(rawId, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)
        eventService.recordEvent(rawId, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)

        //the above DAILY events should get merged with this weekly event due to overlapping start/end
        eventService.recordEvent(rawId, userIds[0], testDates.startOfCurrentWeek.toDate(), 1, EventType.WEEKLY)

        eventService.recordEvent(rawId, userIds[1], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(rawId, userIds[0], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)

        // should not be included in metric
        eventService.recordEvent(rawId2, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)
        eventService.recordEvent(rawId2, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)
        eventService.recordEvent(rawId2, userIds[0], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)

        when:

        List<DayCountItem> results = eventService.getUserEventCountsForSkillId(proj.projectId, skill.skillId, testDates.now.minusDays(300).toDate())

        then:
        results.size() == 2
        results[0].count == 5
        results[0].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.startOfCurrentWeek.toDate())
        results[1].count == 2
        results[1].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.startOfTwoWeeksAgo.toDate())
    }

    def "skill event count for metrics newer then compactDailyEventsOlderThan produces accurate results"() {
        Map proj = SkillsFactory.createProject(42)
        Map subject = SkillsFactory.createSubject(42)
        Map skill = SkillsFactory.createSkill(42,1,1,0,40, 0)
        Map skill2 = SkillsFactory.createSkill(42,1,2,0,40, 0)

        skillsService.createProject(proj)
        skillsService.createSubject(subject)
        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)

        assert maxDailyDays == 3, "test data is structured around compactDailyEventsOlderThan == 3"

        def userIds = getRandomUsers(6)

        SkillDef skillDef = skillDefRepo.findByProjectIdAndSkillIdAndType(proj.projectId, skill.skillId, SkillDef.ContainerType.Skill)
        Integer rawId = skillDef.id

        SkillDef skillDef2 = skillDefRepo.findByProjectIdAndSkillIdAndType(proj.projectId, skill2.skillId, SkillDef.ContainerType.Skill)
        Integer rawId2 = skillDef2.id

        TestDates testDates = new TestDates()

        skillsService.addSkill(skill, userIds[0], testDates.now.toDate())
        skillsService.addSkill(skill, userIds[1], testDates.now.toDate())
        skillsService.addSkill(skill, userIds[2], testDates.now.minusDays(10).toDate())
        skillsService.addSkill(skill, userIds[3], testDates.now.minusDays(10).toDate())
        skillsService.addSkill(skill, userIds[4], testDates.now.minusDays(10).toDate())
        skillsService.addSkill(skill, userIds[5], testDates.now.minusDays(10).toDate())

        eventService.recordEvent(rawId, userIds[2], testDates.now.minusDays(1).toDate(), 5000, EventType.DAILY)
        eventService.recordEvent(rawId, userIds[3], testDates.now.minusDays(1).toDate(), 100, EventType.DAILY)
        eventService.recordEvent(rawId, userIds[4], testDates.now.minusDays(1).toDate(), 100, EventType.DAILY)

        // should not be included in metric
        eventService.recordEvent(rawId, userIds[0], testDates.now.minusDays(6).toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(rawId, userIds[1], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(rawId, userIds[0], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(rawId2, userIds[5], testDates.now.minusDays(6).toDate(), 1, EventType.DAILY)
        eventService.recordEvent(rawId2, userIds[1], testDates.now.minusDays(5).toDate(), 1, EventType.DAILY)
        eventService.recordEvent(rawId2, userIds[0], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)

        when:

        List<DayCountItem> results = eventService.getUserEventCountsForSkillId(proj.projectId, skill.skillId, testDates.now.minusDays(2).toDate())

        then:
        results.size() == 2
        results[0].count == 2
        results[0].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.toDate())
        results[1].count == 5200
        results[1].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.minusDays(1).toDate())

    }

    def "subject event counts spanning compactDailyEventsOlderThan produces accurate results"() {
        Map proj = SkillsFactory.createProject(42)
        Map subject = SkillsFactory.createSubject(42)
        Map subject2 = SkillsFactory.createSubject(42, 2)
        Map skill = SkillsFactory.createSkill(42,1,1,0,40, 0)
        Map skill2 = SkillsFactory.createSkill(42,2,2,0,40, 0)

        skillsService.createProject(proj)
        skillsService.createSubject(subject)
        skillsService.createSubject(subject2)
        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)

        assert maxDailyDays == 3, "test data is structured around compactDailyEventsOlderThan == 3"

        def userIds = getRandomUsers(2)

        SkillDef skillDef = skillDefRepo.findByProjectIdAndSkillIdAndType(proj.projectId, skill.skillId, SkillDef.ContainerType.Skill)
        Integer rawId = skillDef.id

        SkillDef skillDef2 = skillDefRepo.findByProjectIdAndSkillIdAndType(proj.projectId, skill2.skillId, SkillDef.ContainerType.Skill)
        Integer rawId2 = skillDef2.id

        TestDates testDates = new TestDates()

        skillsService.addSkill(skill, userIds[0], testDates.now.toDate())
        skillsService.addSkill(skill, userIds[1], testDates.now.toDate())

        eventService.recordEvent(rawId, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)
        eventService.recordEvent(rawId, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)

        //the above DAILY events should get merged with this weekly event due to overlapping start/end
        eventService.recordEvent(rawId, userIds[0], testDates.startOfCurrentWeek.toDate(), 1, EventType.WEEKLY)

        eventService.recordEvent(rawId, userIds[1], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(rawId, userIds[0], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)

        // should not be included in metric
        eventService.recordEvent(rawId2, userIds[0], testDates.startOfCurrentWeek.toDate(), 1, EventType.DAILY)
        eventService.recordEvent(rawId2, userIds[0], testDates.startOfCurrentWeek.toDate(), 1, EventType.DAILY)
        eventService.recordEvent(rawId2, userIds[0], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)

        when:

        List<DayCountItem> results = eventService.getUserEventCountsForSkillId(proj.projectId, subject.subjectId, testDates.now.minusDays(300).toDate())

        then:
        results.size() == 2
        results[0].count == 5
        results[0].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.startOfCurrentWeek.toDate())
        results[1].count == 2
        results[1].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.startOfTwoWeeksAgo.toDate())
    }

    def "subject event count for metrics newer then compactDailyEventsOlderThan produces accurate results"() {
        Map proj = SkillsFactory.createProject(42)
        Map subject = SkillsFactory.createSubject(42)
        Map subject2 = SkillsFactory.createSubject(42, 2)
        Map skill = SkillsFactory.createSkill(42,1,1,0,40, 0)
        Map skill2 = SkillsFactory.createSkill(42,2,2,0,40, 0)

        skillsService.createProject(proj)
        skillsService.createSubject(subject)
        skillsService.createSubject(subject2)
        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)

        assert maxDailyDays == 3, "test data is structured around compactDailyEventsOlderThan == 3"

        def userIds = getRandomUsers(5)

        SkillDef skillDef = skillDefRepo.findByProjectIdAndSkillIdAndType(proj.projectId, skill.skillId, SkillDef.ContainerType.Skill)
        Integer rawId = skillDef.id

        SkillDef skillDef2 = skillDefRepo.findByProjectIdAndSkillIdAndType(proj.projectId, skill2.skillId, SkillDef.ContainerType.Skill)
        Integer rawId2 = skillDef2.id

        TestDates testDates = new TestDates()

        skillsService.addSkill(skill, userIds[0], testDates.now.toDate())
        skillsService.addSkill(skill, userIds[1], testDates.now.toDate())
        skillsService.addSkill(skill, userIds[2], testDates.now.toDate())
        skillsService.addSkill(skill, userIds[3], testDates.now.toDate())
        skillsService.addSkill(skill, userIds[4], testDates.now.toDate())

        eventService.recordEvent(rawId, userIds[0], testDates.now.toDate(), 99, EventType.DAILY)
        eventService.recordEvent(rawId, userIds[1], testDates.now.toDate(), 49, EventType.DAILY) //153

        eventService.recordEvent(rawId, userIds[2], testDates.now.minusDays(1).toDate(), 10, EventType.DAILY)
        eventService.recordEvent(rawId, userIds[0], testDates.now.minusDays(1).toDate(), 10, EventType.DAILY)
        eventService.recordEvent(rawId, userIds[3], testDates.now.minusDays(1).toDate(), 5, EventType.DAILY)
        eventService.recordEvent(rawId, userIds[4], testDates.now.minusDays(1).toDate(), 5, EventType.DAILY)

        // should not be included in metric
        eventService.recordEvent(rawId, userIds[0], testDates.startOfCurrentWeek.toDate(), 500000, EventType.WEEKLY)
        eventService.recordEvent(rawId2, userIds[0], testDates.now.minusDays(5).toDate(), 1, EventType.DAILY)
        eventService.recordEvent(rawId2, userIds[0], testDates.now.minusDays(5).toDate(), 1, EventType.DAILY)
        eventService.recordEvent(rawId2, userIds[0], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(rawId, userIds[0], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(rawId, userIds[1], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(rawId, userIds[0], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)

        when:

        List<DayCountItem> results = eventService.getUserEventCountsForSkillId(proj.projectId, subject.subjectId, testDates.now.minusDays(2).toDate())

        then:
        results.size() == 2
        results[0].count == 153
        results[0].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.toDate())
        results[1].count == 30
        results[1].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.minusDays(1).toDate())
    }

    def "project distinct user counts spanning compactDailyEventsOlderThan produces accurate results"() {
        Map proj = SkillsFactory.createProject(42)
        Map subject = SkillsFactory.createSubject(42)
        Map subject2 = SkillsFactory.createSubject(42, 2)
        Map skill = SkillsFactory.createSkill(42,1,1,0,40, 0)
        Map skill2 = SkillsFactory.createSkill(42,2,2,0,40, 0)

        skillsService.createProject(proj)
        skillsService.createSubject(subject)
        skillsService.createSubject(subject2)
        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)

        assert maxDailyDays == 3, "test data is structured around compactDailyEventsOlderThan == 3"

        def userIds = getRandomUsers(3)

        SkillDef skillDef = skillDefRepo.findByProjectIdAndSkillIdAndType(proj.projectId, skill.skillId, SkillDef.ContainerType.Skill)
        Integer rawId = skillDef.id

        SkillDef skillDef2 = skillDefRepo.findByProjectIdAndSkillIdAndType(proj.projectId, skill2.skillId, SkillDef.ContainerType.Skill)
        Integer rawId2 = skillDef2.id

        // populate events, need something that will cross a week boundary
        TestDates testDates = new TestDates()

        skillsService.addSkill(skill, userIds[0], testDates.now.toDate())
        skillsService.addSkill(skill, userIds[1], testDates.now.toDate())
        skillsService.addSkill(skill, userIds[2], testDates.now.minusYears(3).toDate())

        eventService.recordEvent(rawId, userIds[0], testDates.startOfCurrentWeek.toDate(), 1, EventType.DAILY)
        eventService.recordEvent(rawId, userIds[0], testDates.startOfCurrentWeek.toDate(), 1, EventType.DAILY)

        //the above DAILY events should get merged with this weekly event due to overlapping start/end
        eventService.recordEvent(rawId, userIds[0], testDates.startOfCurrentWeek.toDate(), 1, EventType.WEEKLY)

        eventService.recordEvent(rawId, userIds[1], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(rawId, userIds[0], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)

        // should not be included in metric
        eventService.recordEvent(rawId2, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)
        eventService.recordEvent(rawId2, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)
        eventService.recordEvent(rawId2, userIds[0], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)

        when:

        List<DayCountItem> results = eventService.getDistinctUserCountsForProject(proj.projectId, testDates.now.minusDays(300).toDate())

        then:
        results.size() == 2
        results[0].count == 2
        results[0].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.startOfCurrentWeek.toDate())
        results[1].count == 2
        results[1].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.startOfTwoWeeksAgo.toDate())
    }

    def "project distinct user count for metrics newer then compactDailyEventsOlderThan produces accurate results"() {
        Map proj = SkillsFactory.createProject(42)
        Map subject = SkillsFactory.createSubject(42)
        Map subject2 = SkillsFactory.createSubject(42, 2)
        Map skill = SkillsFactory.createSkill(42,1,1,0,40, 0)
        Map skill2 = SkillsFactory.createSkill(42,2,2,0,40, 0)

        skillsService.createProject(proj)
        skillsService.createSubject(subject)
        skillsService.createSubject(subject2)
        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)

        assert maxDailyDays == 3, "test data is structured around compactDailyEventsOlderThan == 3"

        def userIds = getRandomUsers(5)

        SkillDef skillDef = skillDefRepo.findByProjectIdAndSkillIdAndType(proj.projectId, skill.skillId, SkillDef.ContainerType.Skill)
        Integer rawId = skillDef.id

        SkillDef skillDef2 = skillDefRepo.findByProjectIdAndSkillIdAndType(proj.projectId, skill2.skillId, SkillDef.ContainerType.Skill)
        Integer rawId2 = skillDef2.id

        TestDates testDates = new TestDates()

        skillsService.addSkill(skill, userIds[0], testDates.now.toDate())
        skillsService.addSkill(skill, userIds[1], testDates.now.toDate())
        skillsService.addSkill(skill, userIds[2], testDates.now.toDate())
        skillsService.addSkill(skill, userIds[3], testDates.now.minusYears(3).toDate())
        skillsService.addSkill(skill, userIds[4], testDates.now.minusYears(3).toDate())

        eventService.recordEvent(rawId, userIds[0], testDates.now.toDate(), 1, EventType.DAILY)
        eventService.recordEvent(rawId, userIds[1], testDates.now.toDate(), 1, EventType.DAILY)

        eventService.recordEvent(rawId, userIds[1], testDates.now.minusDays(1).toDate(), 1, EventType.DAILY)
        eventService.recordEvent(rawId, userIds[2], testDates.now.minusDays(1).toDate(), 1, EventType.DAILY)


        // should not be included in metric
        eventService.recordEvent(rawId2, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)
        eventService.recordEvent(rawId2, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)
        eventService.recordEvent(rawId2, userIds[0], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(rawId, userIds[0], testDates.startOfCurrentWeek.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(rawId, userIds[1], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(rawId, userIds[0], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)

        when:

        List<DayCountItem> results = eventService.getDistinctUserCountsForProject(proj.projectId, testDates.now.minusDays(2).toDate())

        then:
        results.size() == 2
        results[0].count == 3
        results[0].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.toDate())
        results[1].count == 2
        results[1].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.minusDays(1).toDate())
    }

    def "subject distinct user counts spanning compactDailyEventsOlderThan produces accurate results"() {
        Map proj = SkillsFactory.createProject(42)
        Map subject = SkillsFactory.createSubject(42)
        Map subject2 = SkillsFactory.createSubject(42, 2)
        Map subj1_skill1 = SkillsFactory.createSkill(42,1,1,0,40, 0)
        Map subj1_skill2 = SkillsFactory.createSkill(42,1,2,0,40, 0)
        Map subj2_skill1 = SkillsFactory.createSkill(42,2,2,0,40, 0)


        skillsService.createProject(proj)
        skillsService.createSubject(subject)
        skillsService.createSubject(subject2)
        skillsService.createSkill(subj1_skill1)
        skillsService.createSkill(subj1_skill2)
        skillsService.createSkill(subj2_skill1)

        assert maxDailyDays == 3, "test data is structured around compactDailyEventsOlderThan == 3"

        def userIds = getRandomUsers(4)

        SkillDef skillDef = skillDefRepo.findByProjectIdAndSkillIdAndType(proj.projectId, subj1_skill1.skillId, SkillDef.ContainerType.Skill)
        Integer subj1_skill1_rawId = skillDef.id

        SkillDef skillDef2 = skillDefRepo.findByProjectIdAndSkillIdAndType(proj.projectId, subj2_skill1.skillId, SkillDef.ContainerType.Skill)
        Integer subj2_skill1_rawId = skillDef2.id

        SkillDef skillDef3 = skillDefRepo.findByProjectIdAndSkillIdAndType(proj.projectId, subj1_skill2.skillId, SkillDef.ContainerType.Skill)
        Integer subj1_skill2_rawId = skillDef3.id

        // populate events, need something that will cross a week boundary
        TestDates testDates = new TestDates()

        skillsService.addSkill(subj1_skill1, userIds[0], testDates.now.toDate())
        skillsService.addSkill(subj1_skill1, userIds[1], testDates.now.toDate())
        skillsService.addSkill(subj1_skill1, userIds[2], testDates.now.minusYears(3).toDate())
        skillsService.addSkill(subj1_skill2, userIds[3], testDates.getDateWithinCurrentWeek().toDate())

        eventService.recordEvent(subj1_skill1_rawId, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)
        eventService.recordEvent(subj1_skill1_rawId, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)

        //the above DAILY events should get merged with this weekly event due to overlapping start/end
        eventService.recordEvent(subj1_skill1_rawId, userIds[0], testDates.startOfCurrentWeek.toDate(), 1, EventType.WEEKLY)

        eventService.recordEvent(subj1_skill1_rawId, userIds[1], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(subj1_skill1_rawId, userIds[0], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)

        // should not be included in metric
        eventService.recordEvent(subj2_skill1_rawId, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)
        eventService.recordEvent(subj2_skill1_rawId, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)
        eventService.recordEvent(subj2_skill1_rawId, userIds[0], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)

        when:

        List<DayCountItem> results = eventService.getDistinctUserCountForSkillId(proj.projectId, subject.subjectId, testDates.now.minusDays(300).toDate())

        then:
        results.size() == 2
        results[0].count == 3
        results[0].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.startOfCurrentWeek.toDate())
        results[1].count == 2
        results[1].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.startOfTwoWeeksAgo.toDate())
    }

    def "subject distinct user count for metrics newer then compactDailyEventsOlderThan produces accurate results"() {
        Map proj = SkillsFactory.createProject(42)
        Map subject = SkillsFactory.createSubject(42)
        Map subject2 = SkillsFactory.createSubject(42, 2)
        Map subj1_skill1 = SkillsFactory.createSkill(42,1,1,0,40, 0)
        Map subj1_skill2 = SkillsFactory.createSkill(42,1,2,0,40, 0)
        Map subj2_skill1 = SkillsFactory.createSkill(42,2,2,0,40, 0)


        skillsService.createProject(proj)
        skillsService.createSubject(subject)
        skillsService.createSubject(subject2)
        skillsService.createSkill(subj1_skill1)
        skillsService.createSkill(subj1_skill2)
        skillsService.createSkill(subj2_skill1)

        assert maxDailyDays == 3, "test data is structured around compactDailyEventsOlderThan == 3"

        def userIds = getRandomUsers(4)

        SkillDef skillDef = skillDefRepo.findByProjectIdAndSkillIdAndType(proj.projectId, subj1_skill1.skillId, SkillDef.ContainerType.Skill)
        Integer subj1_skill1_rawId = skillDef.id

        SkillDef skillDef2 = skillDefRepo.findByProjectIdAndSkillIdAndType(proj.projectId, subj2_skill1.skillId, SkillDef.ContainerType.Skill)
        Integer subj2_skill1_rawId = skillDef2.id

        SkillDef skillDef3 = skillDefRepo.findByProjectIdAndSkillIdAndType(proj.projectId, subj1_skill2.skillId, SkillDef.ContainerType.Skill)
        Integer subj1_skill2_rawId = skillDef3.id

        TestDates testDates = new TestDates()

        skillsService.addSkill(subj1_skill1, userIds[0], testDates.now.toDate())
        skillsService.addSkill(subj1_skill1, userIds[1], testDates.now.toDate())
        skillsService.addSkill(subj1_skill1, userIds[2], testDates.now.minusYears(3).toDate())
        skillsService.addSkill(subj1_skill2, userIds[3], testDates.now.minusDays(10).toDate())

        eventService.recordEvent(subj1_skill1_rawId, userIds[0], testDates.now.toDate(), 1, EventType.DAILY)
        eventService.recordEvent(subj1_skill1_rawId, userIds[0], testDates.now.toDate(), 1, EventType.DAILY)
        eventService.recordEvent(subj1_skill1_rawId, userIds[1], testDates.now.toDate(), 1, EventType.DAILY)
        eventService.recordEvent(subj1_skill1_rawId, userIds[2], testDates.now.minusDays(1).toDate(), 5, EventType.DAILY)
        eventService.recordEvent(subj1_skill1_rawId, userIds[3], testDates.now.minusDays(1).toDate(), 5000, EventType.DAILY)

        // should not be included in metric
        eventService.recordEvent(subj1_skill1_rawId, userIds[0], testDates.startOfCurrentWeek.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(subj1_skill1_rawId, userIds[1], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(subj1_skill1_rawId, userIds[0], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(subj2_skill1_rawId, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)
        eventService.recordEvent(subj2_skill1_rawId, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)
        eventService.recordEvent(subj2_skill1_rawId, userIds[0], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)

        when:

        List<DayCountItem> results = eventService.getDistinctUserCountForSkillId(proj.projectId, subject.subjectId, testDates.now.minusDays(2).toDate())

        then:
        results.size() == 2
        results[0].count == 2
        results[0].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.toDate())
        results[1].count == 2
        results[1].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.minusDays(1).toDate())
    }

    def "skill distinct user counts spanning compactDailyEventsOlderThan produces accurate results"() {
        Map proj = SkillsFactory.createProject(42)
        Map subject = SkillsFactory.createSubject(42)
        Map subject2 = SkillsFactory.createSubject(42, 2)
        Map subj1_skill1 = SkillsFactory.createSkill(42,1,1,0,40, 0)
        Map subj1_skill2 = SkillsFactory.createSkill(42,1,2,0,40, 0)
        Map subj2_skill1 = SkillsFactory.createSkill(42,2,2,0,40, 0)


        skillsService.createProject(proj)
        skillsService.createSubject(subject)
        skillsService.createSubject(subject2)
        skillsService.createSkill(subj1_skill1)
        skillsService.createSkill(subj1_skill2)
        skillsService.createSkill(subj2_skill1)

        assert maxDailyDays == 3, "test data is structured around compactDailyEventsOlderThan == 3"

        def userIds = getRandomUsers(4)

        SkillDef skillDef = skillDefRepo.findByProjectIdAndSkillIdAndType(proj.projectId, subj1_skill1.skillId, SkillDef.ContainerType.Skill)
        Integer subj1_skill1_rawId = skillDef.id

        SkillDef skillDef2 = skillDefRepo.findByProjectIdAndSkillIdAndType(proj.projectId, subj2_skill1.skillId, SkillDef.ContainerType.Skill)
        Integer subj2_skill1_rawId = skillDef2.id

        SkillDef skillDef3 = skillDefRepo.findByProjectIdAndSkillIdAndType(proj.projectId, subj1_skill2.skillId, SkillDef.ContainerType.Skill)
        Integer subj1_skill2_rawId = skillDef3.id

        // populate events, need something that will cross a week boundary
        TestDates testDates = new TestDates()

        skillsService.addSkill(subj1_skill1, userIds[0], testDates.now.toDate())
        skillsService.addSkill(subj1_skill1, userIds[1], testDates.now.toDate())
        skillsService.addSkill(subj1_skill1, userIds[2], testDates.now.minusYears(3).toDate())
        skillsService.addSkill(subj1_skill2, userIds[3], testDates.getDateWithinCurrentWeek().toDate())

        eventService.recordEvent(subj1_skill1_rawId, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)
        eventService.recordEvent(subj1_skill1_rawId, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)

        //the above DAILY events should get merged with this weekly event due to overlapping start/end
        eventService.recordEvent(subj1_skill1_rawId, userIds[0], testDates.startOfCurrentWeek.toDate(), 1, EventType.WEEKLY)

        eventService.recordEvent(subj1_skill1_rawId, userIds[1], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(subj1_skill1_rawId, userIds[1], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)

        // should not be included in metric
        eventService.recordEvent(subj2_skill1_rawId, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)
        eventService.recordEvent(subj2_skill1_rawId, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)
        eventService.recordEvent(subj2_skill1_rawId, userIds[0], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)

        when:

        List<DayCountItem> results = eventService.getDistinctUserCountForSkillId(proj.projectId, subj1_skill1.skillId, testDates.now.minusDays(300).toDate())

        then:
        results.size() == 2
        results[0].count == 2
        results[0].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.startOfCurrentWeek.toDate())
        results[1].count == 1
        results[1].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.startOfTwoWeeksAgo.toDate())
    }

    def "skill distinct user count for metric newer then compactDailyEventsOlderThan produces accurate results"() {
        Map proj = SkillsFactory.createProject(42)
        Map subject = SkillsFactory.createSubject(42)
        Map subject2 = SkillsFactory.createSubject(42, 2)
        Map subj1_skill1 = SkillsFactory.createSkill(42,1,1,0,40, 0)
        Map subj1_skill2 = SkillsFactory.createSkill(42,1,2,0,40, 0)
        Map subj2_skill1 = SkillsFactory.createSkill(42,2,2,0,40, 0)


        skillsService.createProject(proj)
        skillsService.createSubject(subject)
        skillsService.createSubject(subject2)
        skillsService.createSkill(subj1_skill1)
        skillsService.createSkill(subj1_skill2)
        skillsService.createSkill(subj2_skill1)

        assert maxDailyDays == 3, "test data is structured around compactDailyEventsOlderThan == 3"

        def userIds = getRandomUsers(4)

        SkillDef skillDef = skillDefRepo.findByProjectIdAndSkillIdAndType(proj.projectId, subj1_skill1.skillId, SkillDef.ContainerType.Skill)
        Integer subj1_skill1_rawId = skillDef.id

        SkillDef skillDef2 = skillDefRepo.findByProjectIdAndSkillIdAndType(proj.projectId, subj2_skill1.skillId, SkillDef.ContainerType.Skill)
        Integer subj2_skill1_rawId = skillDef2.id

        SkillDef skillDef3 = skillDefRepo.findByProjectIdAndSkillIdAndType(proj.projectId, subj1_skill2.skillId, SkillDef.ContainerType.Skill)
        Integer subj1_skill2_rawId = skillDef3.id

        TestDates testDates = new TestDates()

        skillsService.addSkill(subj1_skill1, userIds[0], testDates.now.toDate())
        skillsService.addSkill(subj1_skill1, userIds[1], testDates.now.toDate())
        //these two should not be in the results
        skillsService.addSkill(subj1_skill1, userIds[2], testDates.now.minusYears(3).toDate())
        skillsService.addSkill(subj1_skill2, userIds[3], testDates.now.minusDays(4).toDate())

        eventService.recordEvent(subj1_skill1_rawId, userIds[0], testDates.now.minusDays(1).toDate(), 1, EventType.DAILY)
        eventService.recordEvent(subj1_skill1_rawId, userIds[1], testDates.now.minusDays(1).toDate(), 1, EventType.DAILY)
        eventService.recordEvent(subj1_skill1_rawId, userIds[2], testDates.now.minusDays(1).toDate(), 1, EventType.DAILY)

        // should not be included in metric results
        eventService.recordEvent(subj1_skill1_rawId, userIds[1], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(subj1_skill1_rawId, userIds[1], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(subj2_skill1_rawId, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)
        eventService.recordEvent(subj2_skill1_rawId, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)
        eventService.recordEvent(subj2_skill1_rawId, userIds[0], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)

        when:

        List<DayCountItem> results = eventService.getDistinctUserCountForSkillId(proj.projectId, subj1_skill1.skillId, testDates.now.minusDays(2).toDate())

        then:
        results.size() == 2
        results[0].count == 2
        results[0].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.toDate())
        results[1].count == 3
        results[1].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.minusDays(1).toDate())
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

    }

}
