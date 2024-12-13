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
import skills.services.StartDateUtil
import skills.services.UserEventService
import skills.storage.model.DayCountItem
import skills.storage.model.EventType
import skills.storage.model.SkillDef
import skills.storage.model.UserEvent
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.SkillRelDefRepo
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
        preCompactDailyCount == 17
        preCompactWeeklyCount == 0
        postCompactDailyCount == 2
        postCompactionWeeklyCount == 3
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

        SkillDef skillDef2 = skillDefRepo.findByProjectIdAndSkillIdAndType(proj2.projectId, skill2.skillId, SkillDef.ContainerType.Skill)
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

        SkillDef skillDef2 = skillDefRepo.findByProjectIdAndSkillIdAndType(proj2.projectId, skill2.skillId, SkillDef.ContainerType.Skill)
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

        skillsService.addSkill(subj1_skill1, userIds[0], testDates.now.toDate()) // skill1, user1, YES
        skillsService.addSkill(subj1_skill1, userIds[1], testDates.now.toDate()) // skill1, user2, YES
        skillsService.addSkill(subj1_skill1, userIds[2], testDates.now.minusYears(3).toDate()) // skill1, user3, NO
        skillsService.addSkill(subj1_skill2, userIds[3], testDates.getDateWithinCurrentWeek().toDate()) // skill2, user4, NO

        eventService.recordEvent(proj.projectId, subj1_skill1_rawId, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY) // skill1, user1, YES
        eventService.recordEvent(proj.projectId, subj1_skill1_rawId, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY) // skill1, user1, YES

        //the above DAILY events should get merged with this weekly event due to overlapping start/end
        eventService.recordEvent(proj.projectId, subj1_skill1_rawId, userIds[0], testDates.startOfCurrentWeek.toDate(), 1, EventType.WEEKLY) // skill1, user1, YES

        eventService.recordEvent(proj.projectId, subj1_skill1_rawId, userIds[1], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY) //skill1, user2, YES
        eventService.recordEvent(proj.projectId, subj1_skill1_rawId, userIds[1], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY) //skill1, user2, YES

        // should not be included in metric
        eventService.recordEvent(proj.projectId, subj2_skill1_rawId, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY) //sub2 skill1, user1, NO
        eventService.recordEvent(proj.projectId, subj2_skill1_rawId, userIds[0], testDates.getDateWithinCurrentWeek().toDate(), 1, EventType.DAILY) //sub2 skill1, user1, NO
        eventService.recordEvent(proj.projectId, subj2_skill1_rawId, userIds[0], testDates.startOfTwoWeeksAgo.toDate(), 1, EventType.WEEKLY) //sub2, skill1, user1, NO

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

    def "remove uncompacted event"() {
        Map proj = SkillsFactory.createProject(42)
        Map subject = SkillsFactory.createSubject(42)
        Map subj1_skill1 = SkillsFactory.createSkill(42,1,1,0,40, 0)
        Map subj1_skill2 = SkillsFactory.createSkill(42,1,2,0,40, 0)


        skillsService.createProject(proj)
        skillsService.createSubject(subject)
        skillsService.createSkill(subj1_skill1)
        skillsService.createSkill(subj1_skill2)

        assert maxDailyDays == 3, "test data is structured around compactDailyEventsOlderThan == 3"

        def userIds = getRandomUsers(2)

        SkillDef skillDef = skillDefRepo.findByProjectIdAndSkillIdAndType(proj.projectId, subj1_skill1.skillId, SkillDef.ContainerType.Skill)
        Integer subj1_skill1_rawId = skillDef.id

        SkillDef skillDef2 = skillDefRepo.findByProjectIdAndSkillIdAndType(proj.projectId, subj1_skill2.skillId, SkillDef.ContainerType.Skill)
        Integer subj1_skill2_rawId = skillDef2.id

        TestDates testDates = new TestDates()

        skillsService.addSkill(subj1_skill1, userIds[0], testDates.now.toDate())
        skillsService.addSkill(subj1_skill1, userIds[0], testDates.now.toDate())
        skillsService.addSkill(subj1_skill1, userIds[1], testDates.now.minusDays(1).toDate())
        skillsService.addSkill(subj1_skill2, userIds[1], testDates.now.minusDays(1).toDate())
        skillsService.addSkill(subj1_skill1, userIds[1], testDates.now.toDate())
        skillsService.addSkill(subj1_skill1, userIds[1], testDates.now.toDate())

        when:
        List<DayCountItem> beforeRemove = eventService.getUserEventCountsForSkillId(proj.projectId, subj1_skill1.skillId, testDates.now.minusDays(2).toDate())
        eventService.removeEvent(testDates.now.toDate(), userIds[0], subj1_skill1_rawId)
        List<DayCountItem> afterRemove = eventService.getUserEventCountsForSkillId(proj.projectId, subj1_skill1.skillId, testDates.now.minusDays(2).toDate())

        then:
        beforeRemove.size() == 2
        beforeRemove[0].count == 4
        beforeRemove[1].count == 1
        afterRemove.size() == 2
        afterRemove[0].count == 3
        afterRemove[1].count == 1
    }

    def "remove uncompacted event entirely when count reaches 0"() {
        Map proj = SkillsFactory.createProject(42)
        Map subject = SkillsFactory.createSubject(42)
        Map subj1_skill1 = SkillsFactory.createSkill(42,1,1,0,40, 0)
        Map subj1_skill2 = SkillsFactory.createSkill(42,1,2,0,40, 0)


        skillsService.createProject(proj)
        skillsService.createSubject(subject)
        skillsService.createSkill(subj1_skill1)
        skillsService.createSkill(subj1_skill2)

        assert maxDailyDays == 3, "test data is structured around compactDailyEventsOlderThan == 3"

        def userIds = getRandomUsers(2)

        SkillDef skillDef = skillDefRepo.findByProjectIdAndSkillIdAndType(proj.projectId, subj1_skill1.skillId, SkillDef.ContainerType.Skill)
        Integer subj1_skill1_rawId = skillDef.id

        SkillDef skillDef2 = skillDefRepo.findByProjectIdAndSkillIdAndType(proj.projectId, subj1_skill2.skillId, SkillDef.ContainerType.Skill)
        Integer subj1_skill2_rawId = skillDef2.id

        TestDates testDates = new TestDates()

        skillsService.addSkill(subj1_skill1, userIds[0], testDates.now.toDate())
        skillsService.addSkill(subj1_skill1, userIds[0], testDates.now.toDate())
        skillsService.addSkill(subj1_skill1, userIds[1], testDates.now.minusDays(1).toDate())
        skillsService.addSkill(subj1_skill2, userIds[1], testDates.now.minusDays(1).toDate())
        skillsService.addSkill(subj1_skill1, userIds[1], testDates.now.toDate())
        skillsService.addSkill(subj1_skill1, userIds[1], testDates.now.toDate())

        when:
        eventService.removeEvent(testDates.now.toDate(), userIds[0], subj1_skill1_rawId)
        UserEvent eventBefore = userEventsRepo.findByUserIdAndSkillRefIdAndEventTimeAndEventType(userIds[0], subj1_skill1_rawId, StartDateUtil.computeStartDate(testDates.now.toDate(), EventType.DAILY), EventType.DAILY)
        eventService.removeEvent(testDates.now.toDate(), userIds[0], subj1_skill1_rawId)
        UserEvent eventAfter = userEventsRepo.findByUserIdAndSkillRefIdAndEventTimeAndEventType(userIds[0], subj1_skill1_rawId, StartDateUtil.computeStartDate(testDates.now.toDate(), EventType.DAILY), EventType.DAILY)

        then:
        eventBefore.count == 1
        !eventAfter
    }

    def "remove compacted event"(){
        Map proj = SkillsFactory.createProject(42)
        Map subject = SkillsFactory.createSubject(42)
        Map subj1_skill1 = SkillsFactory.createSkill(42,1,1,0,40, 0)
        Map subj1_skill2 = SkillsFactory.createSkill(42,1,2,0,40, 0)


        skillsService.createProject(proj)
        skillsService.createSubject(subject)
        skillsService.createSkill(subj1_skill1)
        skillsService.createSkill(subj1_skill2)

        assert maxDailyDays == 3, "test data is structured around compactDailyEventsOlderThan == 3"

        def userIds = getRandomUsers(2)

        SkillDef skillDef = skillDefRepo.findByProjectIdAndSkillIdAndType(proj.projectId, subj1_skill1.skillId, SkillDef.ContainerType.Skill)
        Integer subj1_skill1_rawId = skillDef.id

        SkillDef skillDef2 = skillDefRepo.findByProjectIdAndSkillIdAndType(proj.projectId, subj1_skill2.skillId, SkillDef.ContainerType.Skill)
        Integer subj1_skill2_rawId = skillDef2.id

        TestDates testDates = new TestDates()

        skillsService.addSkill(subj1_skill1, userIds[0], testDates.startOfTwoWeeksAgo.plusDays(3).toDate())
        skillsService.addSkill(subj1_skill1, userIds[0], testDates.startOfTwoWeeksAgo.toDate())
        skillsService.addSkill(subj1_skill1, userIds[1], testDates.startOfCurrentWeek.toDate())
        skillsService.addSkill(subj1_skill2, userIds[1], testDates.startOfCurrentWeek.toDate())
        skillsService.addSkill(subj1_skill1, userIds[1], testDates.startOfTwoWeeksAgo.toDate())
        skillsService.addSkill(subj1_skill1, userIds[1], testDates.startOfTwoWeeksAgo.toDate())

        when:
        List<DayCountItem> beforeRemove = eventService.getUserEventCountsForSkillId(proj.projectId, subj1_skill1.skillId, testDates.startOfTwoWeeksAgo.toDate())
        beforeRemove = beforeRemove.sort() {it.day}
        eventService.compactDailyEvents()
        eventService.removeEvent(testDates.startOfTwoWeeksAgo.plusDays(3).toDate(), userIds[0], subj1_skill1_rawId)
        List<DayCountItem> afterRemove = eventService.getUserEventCountsForSkillId(proj.projectId, subj1_skill1.skillId, testDates.startOfTwoWeeksAgo.toDate())
        afterRemove = afterRemove.sort {it.day}

        then:
        beforeRemove.size() == 2
        beforeRemove[0].count == 4
        beforeRemove[1].count == 1
        afterRemove.size() == 2
        afterRemove[0].count == 3
        afterRemove[1].count == 1
    }

    def "remove compacted event entirely if count reaches 0"(){
        Map proj = SkillsFactory.createProject(42)
        Map subject = SkillsFactory.createSubject(42)
        Map subj1_skill1 = SkillsFactory.createSkill(42,1,1,0,40, 0)
        Map subj1_skill2 = SkillsFactory.createSkill(42,1,2,0,40, 0)


        skillsService.createProject(proj)
        skillsService.createSubject(subject)
        skillsService.createSkill(subj1_skill1)
        skillsService.createSkill(subj1_skill2)

        assert maxDailyDays == 3, "test data is structured around compactDailyEventsOlderThan == 3"

        def userIds = getRandomUsers(2)

        SkillDef skillDef = skillDefRepo.findByProjectIdAndSkillIdAndType(proj.projectId, subj1_skill1.skillId, SkillDef.ContainerType.Skill)
        Integer subj1_skill1_rawId = skillDef.id

        SkillDef skillDef2 = skillDefRepo.findByProjectIdAndSkillIdAndType(proj.projectId, subj1_skill2.skillId, SkillDef.ContainerType.Skill)
        Integer subj1_skill2_rawId = skillDef2.id

        TestDates testDates = new TestDates()

        skillsService.addSkill(subj1_skill1, userIds[0], testDates.startOfTwoWeeksAgo.plusDays(3).toDate())
        skillsService.addSkill(subj1_skill1, userIds[0], testDates.startOfTwoWeeksAgo.toDate())
        skillsService.addSkill(subj1_skill1, userIds[1], testDates.now.minusDays(1).toDate())
        skillsService.addSkill(subj1_skill2, userIds[1], testDates.now.minusDays(1).toDate())
        skillsService.addSkill(subj1_skill1, userIds[1], testDates.startOfTwoWeeksAgo.toDate())
        skillsService.addSkill(subj1_skill1, userIds[1], testDates.startOfTwoWeeksAgo.toDate())

        when:
        eventService.compactDailyEvents()
        eventService.removeEvent(testDates.startOfTwoWeeksAgo.toDate(), userIds[0], subj1_skill1_rawId)
        UserEvent eventBefore = userEventsRepo.findByUserIdAndSkillRefIdAndEventTimeAndEventType(userIds[0], subj1_skill1_rawId, StartDateUtil.computeStartDate(testDates.startOfTwoWeeksAgo.toDate(), EventType.WEEKLY), EventType.WEEKLY)
        eventService.removeEvent(testDates.startOfTwoWeeksAgo.toDate(), userIds[0], subj1_skill1_rawId)
        UserEvent eventAfter = userEventsRepo.findByUserIdAndSkillRefIdAndEventTimeAndEventType(userIds[0], subj1_skill1_rawId, StartDateUtil.computeStartDate(testDates.startOfTwoWeeksAgo.toDate(), EventType.WEEKLY), EventType.WEEKLY)

        then:
        eventBefore.count == 1
        !eventAfter
    }

    def "daily events for skills imported from the catalog should be reflected in a project/user/skill events"() {
        Map proj = SkillsFactory.createProject(77)
        Map subject = SkillsFactory.createSubject(77)
        Map subj1_skill1 = SkillsFactory.createSkill(77,1,1,0,40, 0)
        Map subj1_skill2 = SkillsFactory.createSkill(77,1,2,0,40, 0)

        def proj2 = SkillsFactory.createProject(52)
        def p2subj1 = SkillsFactory.createSubject(52, 1)
        def p2skill1 = SkillsFactory.createSkill(52, 1, 99)
        p2skill1.numPerformToCompletion = 10
        p2skill1.pointIncrement = 50
        def notFinalized = SkillsFactory.createSkill(52, 1, 101)

        skillsService.createProject(proj)
        skillsService.createSubject(subject)
        skillsService.createSkill(subj1_skill1)
        skillsService.createSkill(subj1_skill2)

        skillsService.createProject(proj2)
        skillsService.createSubject(p2subj1)
        skillsService.createSkill(p2skill1)
        skillsService.createSkill(notFinalized)
        skillsService.exportSkillToCatalog(proj2.projectId, p2skill1.skillId)
        skillsService.importSkillFromCatalog(proj.projectId, subject.subjectId, proj2.projectId, p2skill1.skillId)
        skillsService.finalizeSkillsImportFromCatalog(proj.projectId, true)


        TestDates testDates = new TestDates()

        def users = getRandomUsers(2)
        def user = users[0]
        skillsService.addSkill([projectId: proj2.projectId, skillId: p2skill1.skillId], user, testDates.now.minusDays(2).toDate())
        skillsService.addSkill([projectId: proj2.projectId, skillId: p2skill1.skillId], user, testDates.now.minusDays(1).toDate())
        skillsService.addSkill([projectId: proj.projectId, skillId: subj1_skill1.skillId], user, testDates.now.minusDays(1).toDate())
        skillsService.addSkill([projectId: proj.projectId, skillId: subj1_skill1.skillId], users[1], testDates.now.minusDays(1).toDate())

        when:
        eventService.compactDailyEvents()
        List<DayCountItem> userCounts = eventService.getUserEventCountsForUser(user, testDates.now.minusDays(3).toDate(), [proj.projectId])
        List<DayCountItem> skillCounts = eventService.getUserEventCountsForSkillId(proj.projectId, p2skill1.skillId, testDates.now.minusDays(2).toDate())
        //distinct user counts
        List<DayCountItem> distinctUserCountsForProject = eventService.getDistinctUserCountsForProject(proj.projectId, testDates.now.minusDays(3).toDate())
        List<DayCountItem> distinctUserCountsForSkill = eventService.getDistinctUserCountForSkillId(proj.projectId, p2skill1.skillId, testDates.now.minusDays(3).toDate())

        then:
        userCounts.size() == 3
        userCounts[0].count == 0
        userCounts[0].projectId == proj.projectId
        userCounts[1].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.minusDays(1).toDate())
        userCounts[1].count == 2
        userCounts[1].projectId == proj.projectId
        userCounts[2].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.minusDays(2).toDate())
        userCounts[2].count == 1
        userCounts[2].projectId == proj.projectId
        skillCounts.size() == 2
        skillCounts[0].count == 0
        skillCounts[0].projectId == proj.projectId //because the skill is imported, the exporting project id is returned for this count
        skillCounts[1].count == 1
        skillCounts[1].projectId == proj.projectId //because the skill is imported, the exporting project id is returned for this count
        skillCounts[1].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.minusDays(1).toDate())
        distinctUserCountsForProject.size() == 3
        distinctUserCountsForProject[0].count == 0
        distinctUserCountsForProject[0].projectId == proj.projectId
        distinctUserCountsForProject[1].count == 2
        distinctUserCountsForProject[1].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.minusDays(1).toDate())
        distinctUserCountsForProject[1].projectId == proj.projectId
        distinctUserCountsForProject[2].count == 1
        distinctUserCountsForProject[2].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.minusDays(2).toDate())
        distinctUserCountsForProject[2].projectId == proj.projectId
        distinctUserCountsForSkill.size() == 3
        distinctUserCountsForSkill[0].count == 0
        distinctUserCountsForSkill[0].projectId == proj.projectId
        distinctUserCountsForSkill[1].count == 1
        distinctUserCountsForSkill[1].projectId == proj.projectId
        distinctUserCountsForSkill[1].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.minusDays(1).toDate())
        distinctUserCountsForSkill[2].count == 1
        distinctUserCountsForSkill[2].projectId == proj.projectId
        distinctUserCountsForSkill[2].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.minusDays(2).toDate())
    }

    def "weekly events for skills imported from the catalog should be reflected in a project/user/skill events"() {
        Map proj = SkillsFactory.createProject(42)
        Map subject = SkillsFactory.createSubject(42)
        Map subj1_skill1 = SkillsFactory.createSkill(42,1,1,0,40, 0)
        Map subj1_skill2 = SkillsFactory.createSkill(42,1,2,0,40, 0)

        def proj2 = SkillsFactory.createProject(52)
        def p2subj1 = SkillsFactory.createSubject(52, 1)
        def p2skill1 = SkillsFactory.createSkill(52, 1, 99)
        p2skill1.numPerformToCompletion = 10
        p2skill1.pointIncrement = 50

        skillsService.createProject(proj)
        skillsService.createSubject(subject)
        skillsService.createSkill(subj1_skill1)
        skillsService.createSkill(subj1_skill2)

        skillsService.createProject(proj2)
        skillsService.createSubject(p2subj1)
        skillsService.createSkill(p2skill1)
        skillsService.exportSkillToCatalog(proj2.projectId, p2skill1.skillId)
        skillsService.importSkillFromCatalog(proj.projectId, subject.subjectId, proj2.projectId, p2skill1.skillId)
        skillsService.finalizeSkillsImportFromCatalog(proj.projectId, true)

        TestDates testDates = new TestDates()

        def users = getRandomUsers(2)
        def user = users[0]
        skillsService.addSkill([projectId: proj2.projectId, skillId: p2skill1.skillId], user, testDates.startOfTwoWeeksAgo.plusDays(2).toDate())
        skillsService.addSkill([projectId: proj2.projectId, skillId: p2skill1.skillId], user, testDates.startOfTwoWeeksAgo.plusDays(1).toDate())
        skillsService.addSkill([projectId: proj.projectId, skillId: subj1_skill1.skillId], user, testDates.startOfTwoWeeksAgo.plusDays(3).toDate())
        skillsService.addSkill([projectId: proj.projectId, skillId: subj1_skill1.skillId], users[1], testDates.startOfTwoWeeksAgo.plusDays(1).toDate())

        when:
        eventService.compactDailyEvents()
        List<DayCountItem> userCounts = eventService.getUserEventCountsForUser(user, testDates.startOfTwoWeeksAgo.minusDays(1).toDate(), [proj.projectId])
        List<DayCountItem> skillCounts = eventService.getUserEventCountsForSkillId(proj.projectId, p2skill1.skillId, testDates.startOfTwoWeeksAgo.minusDays(1).toDate())
        List<DayCountItem> distinctUserCountsForProject = eventService.getDistinctUserCountsForProject(proj.projectId, testDates.startOfTwoWeeksAgo.minusDays(3).toDate())
        List<DayCountItem> distinctUserCountsForSkill = eventService.getDistinctUserCountForSkillId(proj.projectId, p2skill1.skillId, testDates.startOfTwoWeeksAgo.minusDays(3).toDate())

        then:
        userCounts.size() == 2
        userCounts[0].count == 0
        userCounts[0].projectId == proj.projectId
        userCounts[1].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.startOfTwoWeeksAgo.toDate())
        userCounts[1].count == 3
        userCounts[1].projectId == proj.projectId
        skillCounts.size() == 2
        skillCounts[0].count == 0
        skillCounts[0].projectId == proj.projectId
        skillCounts[1].count == 2
        skillCounts[1].projectId == proj.projectId
        skillCounts[1].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.startOfTwoWeeksAgo.toDate())
        distinctUserCountsForProject.size() == 2
        distinctUserCountsForProject[0].count == 0
        distinctUserCountsForProject[0].projectId == proj.projectId
        distinctUserCountsForProject[1].count == 2
        distinctUserCountsForProject[1].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.startOfTwoWeeksAgo.toDate())
        distinctUserCountsForProject[1].projectId == proj.projectId
        distinctUserCountsForSkill.size() == 2
        distinctUserCountsForSkill[0].count == 0
        distinctUserCountsForSkill[0].projectId == proj.projectId
        distinctUserCountsForSkill[1].count == 1
        distinctUserCountsForSkill[1].projectId == proj.projectId
        distinctUserCountsForSkill[1].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.startOfTwoWeeksAgo.toDate())
    }

    def "daily subject event metrics should work with imported skills"() {
        Map proj = SkillsFactory.createProject(77)
        Map subject = SkillsFactory.createSubject(77)
        Map subject2 = SkillsFactory.createSubject(77, 2)
        Map subj1_skill1 = SkillsFactory.createSkill(77,1,1,0,40, 0)
        Map subj1_skill2 = SkillsFactory.createSkill(77,1,2,0,40, 0)
        Map subj2_skill1 = SkillsFactory.createSkill(77,2,5,0,40, 0)

        def proj2 = SkillsFactory.createProject(52)
        def p2subj1 = SkillsFactory.createSubject(52, 1)
        def p2skill1 = SkillsFactory.createSkill(52, 1, 99)
        p2skill1.numPerformToCompletion = 10
        p2skill1.pointIncrement = 50
        def p2skill2 = SkillsFactory.createSkill(52, 1, 100)
        p2skill2.numPerformToCompletion = 10
        p2skill2.pointIncrement = 50
        def notFinalized = SkillsFactory.createSkill(52, 1, 101)

        skillsService.createProject(proj)
        skillsService.createSubject(subject)
        skillsService.createSubject(subject2)
        skillsService.createSkill(subj1_skill1)
        skillsService.createSkill(subj1_skill2)
        skillsService.createSkill(subj2_skill1)

        skillsService.createProject(proj2)
        skillsService.createSubject(p2subj1)
        skillsService.createSkill(p2skill1)
        skillsService.createSkill(p2skill2)
        skillsService.createSkill(notFinalized)
        skillsService.exportSkillToCatalog(proj2.projectId, p2skill1.skillId)
        skillsService.exportSkillToCatalog(proj2.projectId, p2skill2.skillId)
        skillsService.exportSkillToCatalog(proj2.projectId, notFinalized.skillId)

        skillsService.importSkillFromCatalog(proj.projectId, subject.subjectId, proj2.projectId, p2skill1.skillId) //import proj2.skill1 into proj.subject1
        skillsService.importSkillFromCatalog(proj.projectId, subject2.subjectId, proj2.projectId, p2skill2.skillId) //import proj2.skill2 into proj.subject2
        skillsService.finalizeSkillsImportFromCatalog(proj.projectId, true)
        //should not be included in counts as is not yet finalized
        skillsService.importSkillFromCatalog(proj.projectId, subject.subjectId, proj2.projectId, notFinalized.skillId)

        TestDates testDates = new TestDates()

        def users = getRandomUsers(4)
        def user = users[0]
        def user2 = users[1]
        def user3 = users[2]
        skillsService.addSkill([projectId: proj2.projectId, skillId: p2skill1.skillId], user, testDates.now.minusDays(2).toDate()) //subject1
        skillsService.addSkill([projectId: proj2.projectId, skillId: p2skill1.skillId], user, testDates.now.minusDays(1).toDate()) //subject 1
        skillsService.addSkill([projectId: proj2.projectId, skillId: p2skill2.skillId], user3, testDates.now.minusDays(1).toDate())
        skillsService.addSkill([projectId: proj.projectId, skillId: subj1_skill1.skillId], user, testDates.now.minusDays(1).toDate()) //subject 1
        skillsService.addSkill([projectId: proj.projectId, skillId: subj1_skill1.skillId], users[1], testDates.now.minusDays(1).toDate()) //subject 1
        skillsService.addSkill([projectId: proj.projectId, skillId: subj1_skill1.skillId], user3, testDates.now.minusDays(2).toDate()) //subject 1
        skillsService.addSkill([projectId: proj2.projectId, skillId: notFinalized.skillId], users[3], testDates.now.minusDays(2).toDate())
        skillsService.addSkill([projectId: proj2.projectId, skillId: notFinalized.skillId], users[3], testDates.now.minusDays(1).toDate())

        //2 days, subj1, 2 events
        //1 day, subj 1, 3

        when:
        eventService.compactDailyEvents()
        List<DayCountItem> subject1Counts = eventService.getUserEventCountsForSkillId(proj.projectId, subject.subjectId, testDates.now.minusDays(3).toDate())
        List<DayCountItem> subject2Counts = eventService.getUserEventCountsForSkillId(proj.projectId, subject2.subjectId, testDates.now.minusDays(3).toDate())
        List<DayCountItem> distinctUserCountsForSubject1 = eventService.getDistinctUserCountForSkillId(proj.projectId, subject.subjectId, testDates.now.minusDays(3).toDate())
        List<DayCountItem> distinctUserCountsForSubject2 = eventService.getDistinctUserCountForSkillId(proj.projectId, subject2.subjectId, testDates.now.minusDays(3).toDate())

        then:
        subject1Counts[0].count == 0
        subject1Counts[0].projectId == proj.projectId
        subject1Counts[1].count == 3
        subject1Counts[1].projectId == proj.projectId
        subject1Counts[1].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.minusDays(1).toDate())
        subject1Counts[2].count == 2
        subject1Counts[2].projectId == proj.projectId
        subject1Counts[2].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.minusDays(2).toDate())

        subject2Counts[0].count == 0
        subject2Counts[0].projectId == proj.projectId
        subject2Counts[1].count == 1
        subject2Counts[1].projectId == proj.projectId
        subject2Counts[1].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.minusDays(1).toDate())
        subject2Counts[2].count == 0
        subject2Counts[2].projectId == proj.projectId
        subject2Counts[2].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.minusDays(2).toDate())

        distinctUserCountsForSubject1[0].count == 0
        distinctUserCountsForSubject1[0].projectId == proj.projectId
        distinctUserCountsForSubject1[1].count == 2
        distinctUserCountsForSubject1[1].projectId == proj.projectId
        distinctUserCountsForSubject1[1].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.minusDays(1).toDate())
        distinctUserCountsForSubject1[2].count == 2
        distinctUserCountsForSubject1[2].projectId == proj.projectId
        distinctUserCountsForSubject1[2].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.minusDays(2).toDate())

        distinctUserCountsForSubject2[0].count == 0
        distinctUserCountsForSubject2[0].projectId == proj.projectId
        distinctUserCountsForSubject2[1].count == 1
        distinctUserCountsForSubject2[1].projectId == proj.projectId
        distinctUserCountsForSubject2[1].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.minusDays(1).toDate())
        distinctUserCountsForSubject2[2].count == 0
        distinctUserCountsForSubject2[2].projectId == proj.projectId
        distinctUserCountsForSubject2[2].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.now.minusDays(2).toDate())
    }

    def "weekly subject event metrics should work with imported skills"() {
        Map proj = SkillsFactory.createProject(42)
        Map subject = SkillsFactory.createSubject(42)
        Map subject2 = SkillsFactory.createSubject(42, 2)
        Map subj1_skill1 = SkillsFactory.createSkill(42,1,1,0,40, 0)
        Map subj1_skill2 = SkillsFactory.createSkill(42,1,2,0,40, 0)
        Map subj2_skill1 = SkillsFactory.createSkill(42,2,5,0,40, 0)

        def proj2 = SkillsFactory.createProject(52)
        def p2subj1 = SkillsFactory.createSubject(52, 1)
        def p2skill1 = SkillsFactory.createSkill(52, 1, 99)
        p2skill1.numPerformToCompletion = 10
        p2skill1.pointIncrement = 50
        def p2skill2 = SkillsFactory.createSkill(52, 1, 100)
        p2skill2.numPerformToCompletion = 10
        p2skill2.pointIncrement = 50
        def notFinalized = SkillsFactory.createSkill(52, 1, 101)

        skillsService.createProject(proj)
        skillsService.createSubject(subject)
        skillsService.createSubject(subject2)
        skillsService.createSkill(subj1_skill1)
        skillsService.createSkill(subj1_skill2)
        skillsService.createSkill(subj2_skill1)

        skillsService.createProject(proj2)
        skillsService.createSubject(p2subj1)
        skillsService.createSkill(p2skill1)
        skillsService.createSkill(p2skill2)
        skillsService.createSkill(notFinalized)
        skillsService.exportSkillToCatalog(proj2.projectId, p2skill1.skillId)
        skillsService.exportSkillToCatalog(proj2.projectId, p2skill2.skillId)
        skillsService.exportSkillToCatalog(proj2.projectId, notFinalized.skillId)
        skillsService.importSkillFromCatalog(proj.projectId, subject.subjectId, proj2.projectId, p2skill1.skillId)
        skillsService.importSkillFromCatalog(proj.projectId, subject2.subjectId, proj2.projectId, p2skill2.skillId)
        skillsService.finalizeSkillsImportFromCatalog(proj.projectId, true)
        //should not be included in counts as is not yet finalized
        skillsService.importSkillFromCatalog(proj.projectId, subject.subjectId, proj2.projectId, notFinalized.skillId)

        TestDates testDates = new TestDates()

        def users = getRandomUsers(4)
        def user = users[0]
        def user2 = users[1]
        def user3 = users[2]
        skillsService.addSkill([projectId: proj2.projectId, skillId: p2skill1.skillId], user, testDates.startOfCurrentWeek.toDate()) //subject 1
        skillsService.addSkill([projectId: proj2.projectId, skillId: p2skill1.skillId], user, testDates.startOfTwoWeeksAgo.toDate()) //subject 1
        skillsService.addSkill([projectId: proj2.projectId, skillId: p2skill2.skillId], user3, testDates.startOfCurrentWeek.toDate())
        skillsService.addSkill([projectId: proj.projectId, skillId: subj1_skill1.skillId], user, testDates.startOfTwoWeeksAgo.toDate()) //subject 1
        skillsService.addSkill([projectId: proj.projectId, skillId: subj1_skill1.skillId], user2, testDates.startOfTwoWeeksAgo.toDate()) //subject 1
        skillsService.addSkill([projectId: proj.projectId, skillId: subj1_skill1.skillId], user3, testDates.startOfCurrentWeek.toDate()) //subject 1
        skillsService.addSkill([projectId: proj2.projectId, skillId: notFinalized.skillId], users[3], testDates.startOfCurrentWeek.toDate()) //subject 1
        skillsService.addSkill([projectId: proj2.projectId, skillId: notFinalized.skillId], users[3], testDates.startOfTwoWeeksAgo.toDate()) //subject 1

        //2 weeks ago: subj1 3 events, 2 unique users
        //current week: subj1 2 events, 2 unique users
        //current week:L subj2 1 event, 1 unique user

        //shouldn't count towards query results
        skillsService.addSkill([projectId: proj.projectId, skillId: subj1_skill1.skillId], user3, testDates.startOfCurrentWeek.minusWeeks(3).toDate()) //subject 1
        skillsService.addSkill([projectId: proj.projectId, skillId: subj1_skill1.skillId], user2, testDates.startOfCurrentWeek.minusWeeks(5).toDate()) //subject 1
        skillsService.addSkill([projectId: proj2.projectId, skillId: p2skill2.skillId], user, testDates.startOfCurrentWeek.minusWeeks(30).toDate())

        when:
        eventService.compactDailyEvents()
        List<DayCountItem> subject1Counts = eventService.getUserEventCountsForSkillId(proj.projectId, subject.subjectId, testDates.startOfTwoWeeksAgo.minusDays(7).toDate())
        List<DayCountItem> subject2Counts = eventService.getUserEventCountsForSkillId(proj.projectId, subject2.subjectId, testDates.startOfTwoWeeksAgo.minusDays(7).toDate())
        List<DayCountItem> distinctUserCountsForSubject1 = eventService.getDistinctUserCountForSkillId(proj.projectId, subject.subjectId, testDates.startOfTwoWeeksAgo.minusDays(7).toDate())
        List<DayCountItem> distinctUserCountsForSubject2 = eventService.getDistinctUserCountForSkillId(proj.projectId, subject2.subjectId, testDates.startOfTwoWeeksAgo.minusDays(7).toDate())

        then:
        subject1Counts.size() == 2
        subject1Counts[0].count == 2
        subject1Counts[0].projectId == proj.projectId
        subject1Counts[0].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.startOfCurrentWeek.toDate())
        subject1Counts[1].count == 3
        subject1Counts[1].projectId == proj.projectId
        subject1Counts[1].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.startOfTwoWeeksAgo.toDate())

        subject2Counts[0].count == 1
        subject2Counts[0].projectId == proj.projectId
        subject2Counts[0].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.startOfCurrentWeek.toDate())
        subject2Counts[1].count == 0
        subject2Counts[1].projectId == proj.projectId
        subject2Counts[1].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.startOfTwoWeeksAgo.toDate())

        distinctUserCountsForSubject1[0].count == 2
        distinctUserCountsForSubject1[0].projectId == proj.projectId
        distinctUserCountsForSubject1[0].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.startOfCurrentWeek.toDate())
        distinctUserCountsForSubject1[1].count == 2
        distinctUserCountsForSubject1[1].projectId == proj.projectId
        distinctUserCountsForSubject1[1].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.startOfTwoWeeksAgo.toDate())

        distinctUserCountsForSubject2[0].count == 1
        distinctUserCountsForSubject2[0].projectId == proj.projectId
        distinctUserCountsForSubject2[0].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.startOfCurrentWeek.toDate())
        distinctUserCountsForSubject2[1].count == 0
        distinctUserCountsForSubject2[1].projectId == proj.projectId
        distinctUserCountsForSubject2[1].day.getDateString() == DateFormat.getDateInstance(DateFormat.SHORT).format(testDates.startOfTwoWeeksAgo.toDate())
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
