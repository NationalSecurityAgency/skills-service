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

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory

class UserAdminMetricsSpec extends DefaultIntSpec {

    def "get metrics for a user without any skills"() {
        String userId = "user1"

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        when:
        def metrics = skillsService.getUserStats(proj1.projectId, userId)
        then:
        metrics.numSkills == 0
        metrics.userTotalPoints == 0
    }

    def "get metrics for a user with skills"() {
        String userId = "user1"

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.each{
            it.pointIncrement = 35
        }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId, new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId], userId, new Date())

        when:
        def metrics = skillsService.getUserStats(proj1.projectId, userId)
        then:
        metrics.numSkills == 2
        metrics.userTotalPoints == 70
    }

    def "get latest event date for a project"() {
        List<String> users = getRandomUsers(3)

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.each {
            it.pointIncrement = 35
        }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        List<Date> dates = (10..1).collect { new Date() - it }

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users[0], dates[0])
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId], users[1], dates[1])
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId], users[2], dates[2])

        when:
        def lastSkillEvent = skillsService.getLastSkillEventForProject(proj1.projectId)

        skillsService.archiveUsers([users[2]], proj1.projectId)
        def lastSkillEventAfterArchive = skillsService.getLastSkillEventForProject(proj1.projectId)

        then:
        Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", lastSkillEvent.lastReportedSkillDate) == dates[2].clearTime()

        Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", lastSkillEventAfterArchive.lastReportedSkillDate) == dates[1].clearTime()
    }

    def "get latest event date is null for a project without any events"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.each {
            it.pointIncrement = 35
        }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        when:
        def lastSkillEvent = skillsService.getLastSkillEventForProject(proj1.projectId)
        then:
        lastSkillEvent.lastReportedSkillDate == null
    }
}
