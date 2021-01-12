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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.services.UserEventService
import skills.storage.model.ProjDef
import skills.storage.model.SkillDef
import skills.storage.model.SkillRelDef
import skills.storage.model.UserEvent
import skills.storage.repos.LevelDefRepo
import skills.storage.repos.ProjDefRepo
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.SkillRelDefRepo
import skills.storage.repos.UserEventsRepo

import javax.transaction.Transactional
import java.time.LocalDateTime
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


    @Transactional
    def "make sure daily events are compacted into weekly events"() {
        Map proj = SkillsFactory.createProject()
        Map subject = SkillsFactory.createSubject()
        Map skill = SkillsFactory.createSkill(1,1,1,0,40, 0)

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
        Stream<UserEvent> preCompactionDailyEvents = userEventsRepo.findAllBySkillRefIdAndEventType(skillRefId, UserEvent.EventType.DAILY)
        Stream<UserEvent> preCompactioWeeklyEvents = userEventsRepo.findAllBySkillRefIdAndEventType(skillRefId, UserEvent.EventType.WEEKLY)
        int preCompactDailyCount = 0
        int preCompactWeeklyCount = 0
        int maxCount = 0
        preCompactionDailyEvents.forEach(event -> {
            preCompactDailyCount++
            maxCount = Math.max(maxCount, event.count)
        })
        preCompactioWeeklyEvents.forEach(event -> {
            preCompactioWeeklyEvents++
        })

        eventService.compactDailyEvents()

        int postCompactDailyCount = 0
        int postCompactionWeeklyCount = 0
        LocalDateTime oldest = LocalDateTime.now().minusDays(maxDailyDays)
        Stream<UserEvent> postCompactionDailyEvents = userEventsRepo.findAllBySkillRefIdAndEventType(skillRefId, UserEvent.EventType.DAILY)
        postCompactionDailyEvents.forEach(event -> {
            assert oldest.isBefore(event.start.toLocalDateTime())
            postCompactDailyCount++
        })
        Stream<UserEvent> postCompactionWeeklyEvents = userEventsRepo.findAllBySkillRefIdAndEventType(skillRefId, UserEvent.EventType.WEEKLY)
        def compactedEvents = []
        postCompactionWeeklyEvents.forEach(event -> {
            postCompactionWeeklyCount++
            assert oldest.isAfter(event.stop.toLocalDateTime())
        })


        then:
        preCompactDailyCount == 17
        preCompactWeeklyCount == 0
        postCompactDailyCount == 2
        postCompactionWeeklyCount == 3
    }

}
