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
import skills.services.LockingService
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

    LockingService mockLock = Mock()

    def setup() {
        eventService.lockingService = mockLock;
    }

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
                assert oldest.isBefore(event.eventTime.toLocalDateTime())
                postCompactDailyCount++
            })
            Stream<UserEvent> postCompactionWeeklyEvents = userEventsRepo.findAllBySkillRefIdAndEventType(skillRefId, EventType.WEEKLY)
            postCompactionWeeklyEvents.forEach({UserEvent event ->
                postCompactionWeeklyCount++
                assert oldest.isAfter(event.eventTime.toLocalDateTime())
            })
        })

        then:
        1 * mockLock.lockEventCompaction()
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

        eventService.recordEvent(proj.projectId, rawId, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)
        eventService.recordEvent(proj.projectId, rawId, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)

        //the above DAILY events should get merged with this weekly event due to overlapping start/end
        eventService.recordEvent(proj.projectId, rawId, userIds[0], testDates.startOfCurrentWeek.toDate(), 1, EventType.WEEKLY)

        eventService.recordEvent(proj.projectId, rawId, userIds[1], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(proj.projectId, rawId, userIds[0], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)

        when:

        List<DayCountItem> results = eventService.getUserEventCountsForProject(proj.projectId, testDates.startOfTwoWeeksAgo.toDate())

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

        eventService.recordEvent(proj.projectId, rawId, userIds[0], testDates.now.minusDays(1).toDate(), 1, EventType.DAILY)
        eventService.recordEvent(proj.projectId, rawId, userIds[0], testDates.now.minusDays(1).toDate(), 3, EventType.DAILY)

        //the below counts should not be included in the results
        eventService.recordEvent(proj.projectId, rawId, userIds[0], testDates.startOfCurrentWeek.toDate(), 1, EventType.WEEKLY)

        eventService.recordEvent(proj.projectId, rawId, userIds[1], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(proj.projectId, rawId, userIds[0], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)

        when:

        Date queryFrom = testDates.now.toLocalDate().atStartOfDay().minusDays(maxDailyDays).toDate()
        List<DayCountItem> results = eventService.getUserEventCountsForProject(proj.projectId, queryFrom)

        then:
        results.size() == 3
        results[0].count == 2
        results[0].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.toDate())
        results[1].count == 4
        results[1].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.minusDays(1).toDate())
        results[2].count == 0
        results[2].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.minusDays(2).toDate())
    }

    def "user event counts spanning compactDailyEventsOlderThan produces accurate results"() {
        Map proj = SkillsFactory.createProject(42)
        Map subject = SkillsFactory.createSubject(42)
        Map skill = SkillsFactory.createSkill(42,1,1,0,40, 0)

        skillsService.createProject(proj)
        skillsService.createSubject(subject)
        skillsService.createSkill(skill)

        Map proj2 = SkillsFactory.createProject(52)
        Map subject2 = SkillsFactory.createSubject(52)
        Map skill2 = SkillsFactory.createSkill(52,1,1,0,40, 0)

        skillsService.createProject(proj2)
        skillsService.createSubject(subject2)
        skillsService.createSkill(skill2)

        assert maxDailyDays == 3, "test data is structured around compactDailyEventsOlderThan == 3"

        def userIds = getRandomUsers(2)

        SkillDef skillDef = skillDefRepo.findByProjectIdAndSkillIdAndType(proj.projectId, skill.skillId, SkillDef.ContainerType.Skill)
        Integer rawId = skillDef.id

        SkillDef skillDef2 = skillDefRepo.findByProjectIdAndSkillIdAndType(proj2.projectId, skill2.skillId, SkillDef.ContainerType.Skill)
        Integer rawId2 = skillDef2.id

        TestDates testDates = new TestDates()

        skillsService.addSkill(skill, userIds[0], testDates.now.toDate())
        skillsService.addSkill(skill, userIds[1], testDates.now.toDate())
        skillsService.addSkill(skill2, userIds[0], testDates.now.toDate())
        skillsService.addSkill(skill2, userIds[1], testDates.now.toDate())
        //1 per user per project after this

        eventService.recordEvent(proj.projectId, rawId, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)
        eventService.recordEvent(proj.projectId, rawId, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)
        //user[0] should be at 3 total events for proj1 in current week now
        eventService.recordEvent(proj2.projectId, rawId2, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)
        eventService.recordEvent(proj2.projectId, rawId2, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)
        //user[0] should be at 3 total events for proj2 in current week now

        // the above DAILY events should get merged with this weekly event due to overlapping start/end
        eventService.recordEvent(proj.projectId, rawId, userIds[0], testDates.startOfCurrentWeek.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(proj2.projectId, rawId2, userIds[0], testDates.startOfCurrentWeek.toDate(), 1, EventType.WEEKLY)
        //user[0] should be at 4 total events per project now for current week

        eventService.recordEvent(proj.projectId, rawId, userIds[1], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(proj.projectId, rawId, userIds[0], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)
        //user[0] and user[1] should have 1 event each for project 1 for two weeks ago
        eventService.recordEvent(proj2.projectId, rawId2, userIds[1], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(proj2.projectId, rawId2, userIds[0], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)
        //user[0] and user[1] should have 1 event each for project 2 for two weeks ago

        when:
        List<String> projectIds = [proj.projectId, proj2.projectId]
        List<DayCountItem> user0Results = eventService.getUserEventCountsForUser(userIds[0], testDates.startOfTwoWeeksAgo.toDate(), projectIds)
        List<DayCountItem> user1Results = eventService.getUserEventCountsForUser(userIds[1], testDates.startOfTwoWeeksAgo.toDate(), projectIds)

        then:
        user0Results.size() == 4
        user0Results[0].count == 4
        user0Results[0].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.startOfCurrentWeek.toDate())
        user0Results[0].projectId == proj.projectId
        user0Results[1].count == 1
        user0Results[1].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.startOfTwoWeeksAgo.toDate())
        user0Results[1].projectId == proj.projectId
        user0Results[2].count == 4
        user0Results[2].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.startOfCurrentWeek.toDate())
        user0Results[2].projectId == proj2.projectId
        user0Results[3].count == 1
        user0Results[3].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.startOfTwoWeeksAgo.toDate())
        user0Results[3].projectId == proj2.projectId

        user1Results.size() == 4
        user1Results[0].count == 1
        user1Results[0].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.startOfCurrentWeek.toDate())
        user1Results[0].projectId == proj.projectId
        user1Results[1].count == 1
        user1Results[1].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.startOfTwoWeeksAgo.toDate())
        user1Results[1].projectId == proj.projectId
        user1Results[2].count == 1
        user1Results[2].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.startOfCurrentWeek.toDate())
        user1Results[2].projectId == proj2.projectId
        user1Results[3].count == 1
        user1Results[3].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.startOfTwoWeeksAgo.toDate())
        user1Results[3].projectId == proj2.projectId
    }


    def "weekly user event count for metrics filter out projects correctly"() {
        Map proj = SkillsFactory.createProject(42)
        Map subject = SkillsFactory.createSubject(42)
        Map skill = SkillsFactory.createSkill(42,1,1,0,40, 0)

        skillsService.createProject(proj)
        skillsService.createSubject(subject)
        skillsService.createSkill(skill)

        Map proj2 = SkillsFactory.createProject(52)
        Map subject2 = SkillsFactory.createSubject(52)
        Map skill2 = SkillsFactory.createSkill(52,1,1,0,40, 0)

        skillsService.createProject(proj2)
        skillsService.createSubject(subject2)
        skillsService.createSkill(skill2)

        assert maxDailyDays == 3, "test data is structured around compactDailyEventsOlderThan == 3"

        def userIds = getRandomUsers(2)

        SkillDef skillDef = skillDefRepo.findByProjectIdAndSkillIdAndType(proj.projectId, skill.skillId, SkillDef.ContainerType.Skill)
        Integer rawId = skillDef.id

        SkillDef skillDef2 = skillDefRepo.findByProjectIdAndSkillIdAndType(proj2.projectId, skill2.skillId, SkillDef.ContainerType.Skill)
        Integer rawId2 = skillDef2.id

        TestDates testDates = new TestDates()

        skillsService.addSkill(skill, userIds[0], testDates.now.toDate())
        skillsService.addSkill(skill, userIds[1], testDates.now.toDate())
        skillsService.addSkill(skill2, userIds[0], testDates.now.toDate())
        skillsService.addSkill(skill2, userIds[1], testDates.now.toDate())


        eventService.recordEvent(proj.projectId, rawId, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)
        eventService.recordEvent(proj.projectId, rawId, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)
        eventService.recordEvent(proj2.projectId, rawId2, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)
        eventService.recordEvent(proj2.projectId, rawId2, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)

        // the above DAILY events should get merged with this weekly event due to overlapping start/end
        eventService.recordEvent(proj.projectId, rawId, userIds[0], testDates.startOfCurrentWeek.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(proj2.projectId, rawId2, userIds[0], testDates.startOfCurrentWeek.toDate(), 1, EventType.WEEKLY)


        eventService.recordEvent(proj.projectId, rawId, userIds[1], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(proj.projectId, rawId, userIds[0], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(proj2.projectId, rawId2, userIds[1], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(proj2.projectId, rawId2, userIds[0], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)

        when:
        List<String> projectIds = [proj.projectId]
        List<DayCountItem> user0Results = eventService.getUserEventCountsForUser(userIds[0], testDates.startOfTwoWeeksAgo.toDate(), projectIds)
        List<DayCountItem> user1Results = eventService.getUserEventCountsForUser(userIds[1], testDates.startOfTwoWeeksAgo.toDate(), projectIds)

        then:
        user0Results.size() == 2
        user0Results[0].count == 4
        user0Results[0].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.startOfCurrentWeek.toDate())
        user0Results[0].projectId == proj.projectId
        user0Results[1].count == 1
        user0Results[1].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.startOfTwoWeeksAgo.toDate())
        user0Results[1].projectId == proj.projectId

        user1Results.size() == 2
        user1Results[0].count == 1
        user1Results[0].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.startOfCurrentWeek.toDate())
        user1Results[0].projectId == proj.projectId
        user1Results[1].count == 1
        user1Results[1].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.startOfTwoWeeksAgo.toDate())
        user1Results[1].projectId == proj.projectId
    }

    def "user event count for metrics newer then compactDailyEventsOlderThan range produces accurate results"() {
        Map proj = SkillsFactory.createProject(42)
        Map subject = SkillsFactory.createSubject(42)
        Map skill = SkillsFactory.createSkill(42,1,1,0,40, 0)

        skillsService.createProject(proj)
        skillsService.createSubject(subject)
        skillsService.createSkill(skill)

        Map proj2 = SkillsFactory.createProject(52)
        Map subject2 = SkillsFactory.createSubject(52)
        Map skill2 = SkillsFactory.createSkill(52,1,1,0,40, 0)

        skillsService.createProject(proj2)
        skillsService.createSubject(subject2)
        skillsService.createSkill(skill2)

        assert maxDailyDays == 3, "test data is structured around compactDailyEventsOlderThan == 3"

        def userIds = getRandomUsers(2)

        SkillDef skillDef = skillDefRepo.findByProjectIdAndSkillIdAndType(proj.projectId, skill.skillId, SkillDef.ContainerType.Skill)
        Integer rawId = skillDef.id

        SkillDef skillDef2 = skillDefRepo.findByProjectIdAndSkillIdAndType(proj.projectId, skill2.skillId, SkillDef.ContainerType.Skill)
        Integer rawId2 = skillDef2.id

        TestDates testDates = new TestDates()

        skillsService.addSkill(skill, userIds[0], testDates.now.minusDays(1).toDate())
        skillsService.addSkill(skill, userIds[1], testDates.now.toDate())
        skillsService.addSkill(skill2, userIds[0], testDates.now.toDate())
        skillsService.addSkill(skill2, userIds[1], testDates.now.toDate())

        eventService.recordEvent(proj.projectId, rawId, userIds[0], testDates.now.minusDays(1).toDate(), 1, EventType.DAILY)
        eventService.recordEvent(proj.projectId, rawId, userIds[0], testDates.now.minusDays(1).toDate(), 3, EventType.DAILY)
        eventService.recordEvent(proj2.projectId, rawId2, userIds[0], testDates.now.minusDays(1).toDate(), 1, EventType.DAILY)
        eventService.recordEvent(proj2.projectId, rawId2, userIds[0], testDates.now.minusDays(1).toDate(), 3, EventType.DAILY)

        //the below counts should not be included in the results
        eventService.recordEvent(proj.projectId, rawId, userIds[0], testDates.startOfCurrentWeek.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(proj2.projectId, rawId2, userIds[0], testDates.startOfCurrentWeek.toDate(), 1, EventType.WEEKLY)

        eventService.recordEvent(proj.projectId, rawId, userIds[1], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(proj.projectId, rawId, userIds[0], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(proj2.projectId, rawId2, userIds[1], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(proj2.projectId, rawId2, userIds[0], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)

        when:

        List<String> projectIds = [proj.projectId, proj2.projectId]
        Date queryFrom = testDates.now.toLocalDate().atStartOfDay().minusDays(maxDailyDays).toDate()
        List<DayCountItem> user0Results = eventService.getUserEventCountsForUser(userIds[0], queryFrom, projectIds)
        user0Results = user0Results.sort{it.projectId+(Long.MAX_VALUE-it.day.time)}
        List<DayCountItem> user1Results = eventService.getUserEventCountsForUser(userIds[1], queryFrom, projectIds)
        user1Results = user1Results.sort {it.projectId +(Long.MAX_VALUE-it.day.time)}

        then:
        user0Results.size() == 6
        user0Results[0].count == 0
        user0Results[0].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.toDate())
        user0Results[0].projectId == proj.projectId
        user0Results[1].count == 5
        user0Results[1].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.minusDays(1).toDate())
        user0Results[1].projectId == proj.projectId
        user0Results[2].count == 0
        user0Results[2].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.minusDays(2).toDate())
        user0Results[2].projectId == proj.projectId
        user0Results[3].count == 1
        user0Results[3].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.toDate())
        user0Results[3].projectId == proj2.projectId
        user0Results[4].count == 4
        user0Results[4].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.minusDays(1).toDate())
        user0Results[4].projectId == proj2.projectId
        user0Results[5].count == 0
        user0Results[5].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.minusDays(2).toDate())
        user0Results[5].projectId == proj2.projectId

        user1Results.size() == 6
        user1Results[0].count == 1
        user1Results[0].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.toDate())
        user1Results[0].projectId == proj.projectId
        user1Results[1].count == 0
        user1Results[1].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.minusDays(1).toDate())
        user1Results[1].projectId == proj.projectId
        user1Results[2].count == 0
        user1Results[2].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.minusDays(2).toDate())
        user1Results[2].projectId == proj.projectId

        user1Results[3].count == 1
        user1Results[3].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.toDate())
        user1Results[3].projectId == proj2.projectId
        user1Results[4].count == 0
        user1Results[4].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.minusDays(1).toDate())
        user1Results[4].projectId == proj2.projectId
        user1Results[5].count == 0
        user1Results[5].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.minusDays(2).toDate())
        user1Results[5].projectId == proj2.projectId
    }

    def "daily user event count for metrics filter out projects correctly"() {
        Map proj = SkillsFactory.createProject(42)
        Map subject = SkillsFactory.createSubject(42)
        Map skill = SkillsFactory.createSkill(42,1,1,0,40, 0)

        skillsService.createProject(proj)
        skillsService.createSubject(subject)
        skillsService.createSkill(skill)

        Map proj2 = SkillsFactory.createProject(52)
        Map subject2 = SkillsFactory.createSubject(52)
        Map skill2 = SkillsFactory.createSkill(52,1,1,0,40, 0)

        skillsService.createProject(proj2)
        skillsService.createSubject(subject2)
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
        skillsService.addSkill(skill2, userIds[0], testDates.now.toDate())
        skillsService.addSkill(skill2, userIds[1], testDates.now.toDate())

        eventService.recordEvent(proj.projectId, rawId, userIds[0], testDates.now.minusDays(1).toDate(), 1, EventType.DAILY)
        eventService.recordEvent(proj.projectId, rawId, userIds[0], testDates.now.minusDays(1).toDate(), 3, EventType.DAILY)
        eventService.recordEvent(proj2.projectId, rawId2, userIds[0], testDates.now.minusDays(1).toDate(), 1, EventType.DAILY)
        eventService.recordEvent(proj2.projectId, rawId2, userIds[0], testDates.now.minusDays(1).toDate(), 3, EventType.DAILY)

        //the below counts should not be included in the results
        eventService.recordEvent(proj.projectId, rawId, userIds[0], testDates.startOfCurrentWeek.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(proj2.projectId, rawId2, userIds[0], testDates.startOfCurrentWeek.toDate(), 1, EventType.WEEKLY)

        eventService.recordEvent(proj.projectId, rawId, userIds[1], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(proj.projectId, rawId, userIds[0], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(proj2.projectId, rawId2, userIds[1], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(proj2.projectId, rawId2, userIds[0], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)

        when:

        List<String> projectIds = [proj.projectId]
        Date queryFrom = testDates.now.toLocalDate().atStartOfDay().minusDays(maxDailyDays).toDate()
        List<DayCountItem> user0Results = eventService.getUserEventCountsForUser(userIds[0], queryFrom, projectIds)
        List<DayCountItem> user1Results = eventService.getUserEventCountsForUser(userIds[1], queryFrom, projectIds)

        then:
        user0Results.size() == 3
        user0Results[0].count == 1
        user0Results[0].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.toDate())
        user0Results[0].projectId == proj.projectId
        user0Results[1].count == 4
        user0Results[1].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.minusDays(1).toDate())
        user0Results[1].projectId == proj.projectId
        user0Results[2].count == 0
        user0Results[2].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.minusDays(2).toDate())
        user0Results[2].projectId == proj.projectId

        user1Results.size() == 3
        user1Results[0].count == 1
        user1Results[0].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.toDate())
        user1Results[0].projectId == proj.projectId
        user1Results[1].count == 0
        user1Results[1].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.minusDays(1).toDate())
        user1Results[1].projectId == proj.projectId
        user1Results[2].count == 0
        user1Results[2].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.minusDays(2).toDate())
        user1Results[2].projectId == proj.projectId
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

        eventService.recordEvent(proj.projectId, rawId, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)
        eventService.recordEvent(proj.projectId, rawId, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)

        //the above DAILY events should get merged with this weekly event due to overlapping start/end
        eventService.recordEvent(proj.projectId, rawId, userIds[0], testDates.startOfCurrentWeek.toDate(), 1, EventType.WEEKLY)

        eventService.recordEvent(proj.projectId, rawId, userIds[1], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(proj.projectId, rawId, userIds[0], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)

        // should not be included in metric
        eventService.recordEvent(proj.projectId, rawId2, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)
        eventService.recordEvent(proj.projectId, rawId2, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)
        eventService.recordEvent(proj.projectId, rawId2, userIds[0], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)

        when:

        List<DayCountItem> results = eventService.getUserEventCountsForSkillId(proj.projectId, skill.skillId, testDates.startOfTwoWeeksAgo.toDate())

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

        eventService.recordEvent(proj.projectId, rawId, userIds[2], testDates.now.minusDays(1).toDate(), 5000, EventType.DAILY)
        eventService.recordEvent(proj.projectId, rawId, userIds[3], testDates.now.minusDays(1).toDate(), 100, EventType.DAILY)
        eventService.recordEvent(proj.projectId, rawId, userIds[4], testDates.now.minusDays(1).toDate(), 100, EventType.DAILY)

        // should not be included in metric
        eventService.recordEvent(proj.projectId, rawId, userIds[0], testDates.now.minusDays(6).toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(proj.projectId, rawId, userIds[1], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(proj.projectId, rawId, userIds[0], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(proj.projectId, rawId2, userIds[5], testDates.now.minusDays(6).toDate(), 1, EventType.DAILY)
        eventService.recordEvent(proj.projectId, rawId2, userIds[1], testDates.now.minusDays(5).toDate(), 1, EventType.DAILY)
        eventService.recordEvent(proj.projectId, rawId2, userIds[0], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)

        when:

        Date queryFrom = testDates.now.toLocalDate().atStartOfDay().minusDays(maxDailyDays).toDate()
        List<DayCountItem> results = eventService.getUserEventCountsForSkillId(proj.projectId, skill.skillId, queryFrom)

        then:
        results.size() == 3
        results[0].count == 2
        results[0].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.toDate())
        results[1].count == 5200
        results[1].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.minusDays(1).toDate())
        results[2].count == 0
        results[2].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.minusDays(2).toDate())
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

        eventService.recordEvent(proj.projectId, rawId, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)
        eventService.recordEvent(proj.projectId, rawId, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)

        //the above DAILY events should get merged with this weekly event due to overlapping start/end
        eventService.recordEvent(proj.projectId, rawId, userIds[0], testDates.startOfCurrentWeek.toDate(), 1, EventType.WEEKLY)

        eventService.recordEvent(proj.projectId, rawId, userIds[1], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(proj.projectId, rawId, userIds[0], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)

        // should not be included in metric
        eventService.recordEvent(proj.projectId, rawId2, userIds[0], testDates.startOfCurrentWeek.toDate(), 1, EventType.DAILY)
        eventService.recordEvent(proj.projectId, rawId2, userIds[0], testDates.startOfCurrentWeek.toDate(), 1, EventType.DAILY)
        eventService.recordEvent(proj.projectId, rawId2, userIds[0], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)

        when:

        List<DayCountItem> results = eventService.getUserEventCountsForSkillId(proj.projectId, subject.subjectId, testDates.startOfTwoWeeksAgo.toDate())

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

        eventService.recordEvent(proj.projectId, rawId, userIds[0], testDates.now.toDate(), 99, EventType.DAILY)
        eventService.recordEvent(proj.projectId, rawId, userIds[1], testDates.now.toDate(), 49, EventType.DAILY) //153

        eventService.recordEvent(proj.projectId, rawId, userIds[2], testDates.now.minusDays(1).toDate(), 10, EventType.DAILY)
        eventService.recordEvent(proj.projectId, rawId, userIds[0], testDates.now.minusDays(1).toDate(), 10, EventType.DAILY)
        eventService.recordEvent(proj.projectId, rawId, userIds[3], testDates.now.minusDays(1).toDate(), 5, EventType.DAILY)
        eventService.recordEvent(proj.projectId, rawId, userIds[4], testDates.now.minusDays(1).toDate(), 5, EventType.DAILY)

        // should not be included in metric
        eventService.recordEvent(proj.projectId, rawId, userIds[0], testDates.startOfCurrentWeek.toDate(), 500000, EventType.WEEKLY)
        eventService.recordEvent(proj.projectId, rawId2, userIds[0], testDates.now.minusDays(5).toDate(), 1, EventType.DAILY)
        eventService.recordEvent(proj.projectId, rawId2, userIds[0], testDates.now.minusDays(5).toDate(), 1, EventType.DAILY)
        eventService.recordEvent(proj.projectId, rawId2, userIds[0], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(proj.projectId, rawId, userIds[0], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(proj.projectId, rawId, userIds[1], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(proj.projectId, rawId, userIds[0], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)

        when:

        Date queryFrom = testDates.now.toLocalDate().atStartOfDay().minusDays(maxDailyDays).toDate()
        List<DayCountItem> results = eventService.getUserEventCountsForSkillId(proj.projectId, subject.subjectId, queryFrom)

        then:
        results.size() == 3
        results[0].count == 153
        results[0].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.toDate())
        results[1].count == 30
        results[1].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.minusDays(1).toDate())
        results[2].count == 0
        results[2].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.minusDays(2).toDate())
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

        eventService.recordEvent(proj.projectId, rawId, userIds[0], testDates.startOfCurrentWeek.toDate(), 1, EventType.DAILY)
        eventService.recordEvent(proj.projectId, rawId, userIds[0], testDates.startOfCurrentWeek.toDate(), 1, EventType.DAILY)

        //the above DAILY events should get merged with this weekly event due to overlapping start/end
        eventService.recordEvent(proj.projectId, rawId, userIds[0], testDates.startOfCurrentWeek.toDate(), 1, EventType.WEEKLY)

        eventService.recordEvent(proj.projectId, rawId, userIds[1], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(proj.projectId, rawId, userIds[0], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)

        // should not be included in metric
        eventService.recordEvent(proj.projectId, rawId2, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)
        eventService.recordEvent(proj.projectId, rawId2, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)
        eventService.recordEvent(proj.projectId, rawId2, userIds[0], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)

        when:

        List<DayCountItem> results = eventService.getDistinctUserCountsForProject(proj.projectId, testDates.startOfTwoWeeksAgo.toDate())

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

        eventService.recordEvent(proj.projectId, rawId, userIds[0], testDates.now.toDate(), 1, EventType.DAILY)
        eventService.recordEvent(proj.projectId, rawId, userIds[1], testDates.now.toDate(), 1, EventType.DAILY)

        Date dayBefore = testDates.now.minusDays(1).toDate()
        eventService.recordEvent(proj.projectId, rawId, userIds[1], dayBefore, 1, EventType.DAILY)
        eventService.recordEvent(proj.projectId, rawId, userIds[2], dayBefore, 2, EventType.DAILY)

        // should not be included in metric
        eventService.recordEvent(proj.projectId, rawId2, userIds[0], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(proj.projectId, rawId, userIds[0], testDates.startOfCurrentWeek.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(proj.projectId, rawId, userIds[1], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(proj.projectId, rawId, userIds[0], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)

        when:

        Date queryFrom = testDates.now.toLocalDate().atStartOfDay().minusDays(maxDailyDays).toDate()
        List<DayCountItem> results = eventService.getDistinctUserCountsForProject(proj.projectId, queryFrom)

        then:
        results.size() == 3
        results[0].count == 3
        results[0].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.toDate())
        results[1].count == 2
        results[1].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(dayBefore)
        results[2].count == 0
        results[2].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.minusDays(2).toDate())
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

        eventService.recordEvent(proj.projectId, subj1_skill1_rawId, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)
        eventService.recordEvent(proj.projectId, subj1_skill1_rawId, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)

        //the above DAILY events should get merged with this weekly event due to overlapping start/end
        eventService.recordEvent(proj.projectId, subj1_skill1_rawId, userIds[0], testDates.startOfCurrentWeek.toDate(), 1, EventType.WEEKLY)

        eventService.recordEvent(proj.projectId, subj1_skill1_rawId, userIds[1], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(proj.projectId, subj1_skill1_rawId, userIds[0], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)

        // should not be included in metric
        eventService.recordEvent(proj.projectId, subj2_skill1_rawId, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)
        eventService.recordEvent(proj.projectId, subj2_skill1_rawId, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)
        eventService.recordEvent(proj.projectId, subj2_skill1_rawId, userIds[0], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)

        when:

        List<DayCountItem> results = eventService.getDistinctUserCountForSkillId(proj.projectId, subject.subjectId, testDates.startOfTwoWeeksAgo.toDate())

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

        eventService.recordEvent(proj.projectId, subj1_skill1_rawId, userIds[0], testDates.now.toDate(), 1, EventType.DAILY)
        eventService.recordEvent(proj.projectId, subj1_skill1_rawId, userIds[0], testDates.now.toDate(), 1, EventType.DAILY)
        eventService.recordEvent(proj.projectId, subj1_skill1_rawId, userIds[1], testDates.now.toDate(), 1, EventType.DAILY)
        eventService.recordEvent(proj.projectId, subj1_skill1_rawId, userIds[2], testDates.now.minusDays(1).toDate(), 5, EventType.DAILY)
        eventService.recordEvent(proj.projectId, subj1_skill1_rawId, userIds[3], testDates.now.minusDays(1).toDate(), 5000, EventType.DAILY)

        // should not be included in metric
        eventService.recordEvent(proj.projectId, subj1_skill1_rawId, userIds[0], testDates.startOfCurrentWeek.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(proj.projectId, subj1_skill1_rawId, userIds[1], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(proj.projectId, subj1_skill1_rawId, userIds[0], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(proj.projectId, subj2_skill1_rawId, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)
        eventService.recordEvent(proj.projectId, subj2_skill1_rawId, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)
        eventService.recordEvent(proj.projectId, subj2_skill1_rawId, userIds[0], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)

        when:

        Date queryFrom = testDates.now.toLocalDate().atStartOfDay().minusDays(maxDailyDays).toDate()
        List<DayCountItem> results = eventService.getDistinctUserCountForSkillId(proj.projectId, subject.subjectId, queryFrom)

        then:
        results.size() == 3
        results[0].count == 2
        results[0].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.toDate())
        results[1].count == 2
        results[1].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.minusDays(1).toDate())
        results[2].count == 0
        results[2].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.minusDays(2).toDate())
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

        eventService.recordEvent(proj.projectId, subj1_skill1_rawId, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)
        eventService.recordEvent(proj.projectId, subj1_skill1_rawId, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)

        //the above DAILY events should get merged with this weekly event due to overlapping start/end
        eventService.recordEvent(proj.projectId, subj1_skill1_rawId, userIds[0], testDates.startOfCurrentWeek.toDate(), 1, EventType.WEEKLY)

        eventService.recordEvent(proj.projectId, subj1_skill1_rawId, userIds[1], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(proj.projectId, subj1_skill1_rawId, userIds[1], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)

        // should not be included in metric
        eventService.recordEvent(proj.projectId, subj2_skill1_rawId, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)
        eventService.recordEvent(proj.projectId, subj2_skill1_rawId, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)
        eventService.recordEvent(proj.projectId, subj2_skill1_rawId, userIds[0], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)

        when:

        List<DayCountItem> results = eventService.getDistinctUserCountForSkillId(proj.projectId, subj1_skill1.skillId, testDates.startOfTwoWeeksAgo.toDate())

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

        eventService.recordEvent(proj.projectId, subj1_skill1_rawId, userIds[0], testDates.now.minusDays(1).toDate(), 1, EventType.DAILY)
        eventService.recordEvent(proj.projectId, subj1_skill1_rawId, userIds[1], testDates.now.minusDays(1).toDate(), 1, EventType.DAILY)
        eventService.recordEvent(proj.projectId, subj1_skill1_rawId, userIds[2], testDates.now.minusDays(1).toDate(), 1, EventType.DAILY)

        // should not be included in metric results
        eventService.recordEvent(proj.projectId, subj1_skill1_rawId, userIds[1], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(proj.projectId, subj1_skill1_rawId, userIds[1], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)
        eventService.recordEvent(proj.projectId, subj2_skill1_rawId, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)
        eventService.recordEvent(proj.projectId, subj2_skill1_rawId, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY)
        eventService.recordEvent(proj.projectId, subj2_skill1_rawId, userIds[0], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY)

        when:

        Date queryFrom = testDates.now.toLocalDate().atStartOfDay().minusDays(maxDailyDays).toDate()
        List<DayCountItem> results = eventService.getDistinctUserCountForSkillId(proj.projectId, subj1_skill1.skillId, queryFrom)

        then:
        results.size() == 3
        results[0].count == 2
        results[0].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.toDate())
        results[1].count == 3
        results[1].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.minusDays(1).toDate())
        results[2].count == 0
        results[2].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.minusDays(2).toDate())
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
