/**
 * Copyright 2021 SkillTree
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

import groovy.time.TimeCategory
import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.services.StartDateUtil
import skills.services.UserEventService
import skills.storage.model.EventType

class AllProjectsSkillEventsOverTimeMetricsBuilderSpec extends DefaultIntSpec {

    @Autowired
    UserEventService userEventService

    def "weekly counts for multiple projects"() {
        def proj = SkillsFactory.createProject()
        def proj2 = SkillsFactory.createProject(2)
        def proj3 = SkillsFactory.createProject(3)

        List<Map> skills = SkillsFactory.createSkills(10)
        List<Map> skills2 = SkillsFactory.createSkills(10, 2)


        skillsService.createProject(proj)
        skillsService.createProject(proj2)
        skillsService.createProject(proj3)
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSubject(SkillsFactory.createSubject(2))
        skillsService.createSkills(skills)
        skillsService.createSkills(skills2)

        def currentUser = skillsService.getCurrentUser()

        Date queryStart
        use(TimeCategory) {

            queryStart = 21.days.ago

            (14..0).each {
                skillsService.addSkill(skills[0], currentUser.userId, it.days.ago)

                if (it >= 8) {
                    skillsService.addSkill(skills2[0], currentUser.userId, it.days.ago)
                }
            }
        }

        when:
        def chartData = skillsService.getApiGlobalMetricsData("allProjectsSkillEventsOverTimeMetricsBuilder", [start: queryStart.time, projIds: "${proj.projectId},${proj2.projectId},${proj3.projectId}"])

        then:
        chartData.find {it.project == proj.projectId}.countsByDay.last().timestamp == StartDateUtil.computeStartDate(new Date(), EventType.WEEKLY).time
        chartData.find {it.project == proj.projectId}.countsByDay.collect{it.num}.sum() == 15
        chartData.find {it.project == proj2.projectId}.countsByDay.last().timestamp == StartDateUtil.computeStartDate(new Date(), EventType.WEEKLY).time
        chartData.find {it.project == proj2.projectId}.countsByDay.last().num == 0
        chartData.find {it.project == proj2.projectId}.countsByDay[1].timestamp == StartDateUtil.computeStartDate(new Date(), EventType.WEEKLY).minus(7).time
        chartData.find {it.project == proj2.projectId}.countsByDay.collect{it.num}.sum() == 7
        chartData.find {it.project == proj3.projectId}.countsByDay.last().timestamp == StartDateUtil.computeStartDate(new Date(), EventType.WEEKLY).time
        chartData.find {it.project == proj3.projectId}.countsByDay.last().num == 0
        chartData.find {it.project == proj3.projectId}.countsByDay.collect{it.num}.sum() == 0
    }

    def "daily counts for multiple projects"() {
        def proj = SkillsFactory.createProject()
        def proj2 = SkillsFactory.createProject(2)
        def proj3 = SkillsFactory.createProject(3)

        List<Map> skills = SkillsFactory.createSkills(10)
        List<Map> skills2 = SkillsFactory.createSkills(10, 2)


        skillsService.createProject(proj)
        skillsService.createProject(proj2)
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSubject(SkillsFactory.createSubject(2))
        skillsService.createSkills(skills)
        skillsService.createSkills(skills2)

        def currentUser = skillsService.getCurrentUser()

        Date queryStart
        use(TimeCategory) {

            queryStart = 3.days.ago

            (3..0).each {
                skillsService.addSkill(skills[0], currentUser.userId, it.days.ago)

                if (it == 2) {
                    skillsService.addSkill(skills2[0], currentUser.userId, it.days.ago)
                }
            }
        }

        when:
        def chartData = skillsService.getApiGlobalMetricsData("allProjectsSkillEventsOverTimeMetricsBuilder", [start: queryStart.time, projIds: "${proj.projectId},${proj2.projectId},${proj3.projectId}"])

        then:
        chartData.find {it.project == proj.projectId}.countsByDay.last().timestamp == StartDateUtil.computeStartDate(new Date(), EventType.DAILY).time
        chartData.find {it.project == proj.projectId}.countsByDay.last().num == 1
        chartData.find {it.project == proj.projectId}.countsByDay.collect{it.num}.sum() == 3
        chartData.find {it.project == proj2.projectId}.countsByDay.last().timestamp == StartDateUtil.computeStartDate(new Date(), EventType.DAILY).time
        chartData.find {it.project == proj2.projectId}.countsByDay.last().num == 0
        chartData.find {it.project == proj2.projectId}.countsByDay.collect{it.num}.sum() == 1
        chartData.find {it.project == proj3.projectId}.countsByDay.last().timestamp == StartDateUtil.computeStartDate(new Date(), EventType.DAILY).time
        chartData.find {it.project == proj3.projectId}.countsByDay.last().num == 0
        chartData.find {it.project == proj3.projectId}.countsByDay.collect{it.num}.sum() == 0
    }
}

