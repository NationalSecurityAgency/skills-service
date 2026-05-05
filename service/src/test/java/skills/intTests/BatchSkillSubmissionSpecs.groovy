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
package skills.intTests

import org.springframework.beans.factory.annotation.Autowired
import skills.controller.AddSkillHelper
import skills.controller.request.model.BatchSkillEventRequest
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory

class BatchSkillSubmissionSpecs extends DefaultIntSpec {

    @Autowired
    AddSkillHelper addSkillHelper

    def "Submit a single skill for a single user"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj1 = SkillsFactory.createSubject(1, 1)

        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.each {
            it.pointIncrement = 100
            it.numPerformToCompletion = 2
            it.pointIncrementInterval = 0 // ability to achieve right away
        }
        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj1)
        skillsService.createSkills(proj1_skills)

        List<String> users = getRandomUsers(1)

        def skillRequest = [
                userIds: [users[0]],
                skillIds: ['skill1'],
                timestamp: 1234l,
                notifyIfSkillNotApplied: true,
                isRetry: false,
        ]

        when:
        def result = skillsService.addBatchSkillsForBatchUsers(proj1.projectId, skillRequest).body

        then:
        result
        result.results.size() == 1
        result.results[0].skillId == 'skill1'
        result.results[0].skillApplied
        result.results[0].userId == users[0]

    }

    def "Submit a batch of skills for a single user"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj1 = SkillsFactory.createSubject(1, 1)

        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.each {
            it.pointIncrement = 100
            it.numPerformToCompletion = 2
            it.pointIncrementInterval = 0 // ability to achieve right away
        }
        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj1)
        skillsService.createSkills(proj1_skills)
        List<String> users = getRandomUsers(1)

        def skillRequest = [
                userIds: [users[0]],
                skillIds: ['skill1', 'skill2', 'skill3'],
                timestamp: 1234l,
                notifyIfSkillNotApplied: true,
                isRetry: false,
        ]

        when:
        def result = skillsService.addBatchSkillsForBatchUsers(proj1.projectId, skillRequest).body

        then:
        result
        result.results.size() == 3
        result.results[0].skillId == 'skill1'
        result.results[0].skillApplied
        result.results[0].userId == users[0]
        result.results[1].skillId == 'skill2'
        result.results[1].skillApplied
        result.results[1].userId == users[0]
        result.results[2].skillId == 'skill3'
        result.results[2].skillApplied
        result.results[2].userId == users[0]
    }

    def "Submit a single skill for multiple users"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj1 = SkillsFactory.createSubject(1, 1)

        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.each {
            it.pointIncrement = 100
            it.numPerformToCompletion = 2
            it.pointIncrementInterval = 0 // ability to achieve right away
        }
        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj1)
        skillsService.createSkills(proj1_skills)
        List<String> users = getRandomUsers(4)

        def skillRequest = [
                userIds: users,
                skillIds: ['skill1'],
                timestamp: 1234l,
                notifyIfSkillNotApplied: true,
                isRetry: false,
        ]

        when:
        def result = skillsService.addBatchSkillsForBatchUsers(proj1.projectId, skillRequest).body

        then:
        result
        result.results.size() == 4
        result.results[0].skillId == 'skill1'
        result.results[0].skillApplied
        result.results[0].userId == users[0]

        result.results[1].skillId == 'skill1'
        result.results[1].skillApplied
        result.results[1].userId == users[1]

        result.results[2].skillId == 'skill1'
        result.results[2].skillApplied
        result.results[2].userId == users[2]

        result.results[3].skillId == 'skill1'
        result.results[3].skillApplied
        result.results[3].userId == users[3]

    }

    def "Submit a batch of skills for multiple users"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj1 = SkillsFactory.createSubject(1, 1)

        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.each {
            it.pointIncrement = 100
            it.numPerformToCompletion = 2
            it.pointIncrementInterval = 0 // ability to achieve right away
        }
        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj1)
        skillsService.createSkills(proj1_skills)
        List<String> users = getRandomUsers(4)

        def skillRequest = [
                userIds: users,
                skillIds: ['skill1', 'skill2'],
                timestamp: 1234l,
                notifyIfSkillNotApplied: true,
                isRetry: false,
        ]

        when:
        def result = skillsService.addBatchSkillsForBatchUsers(proj1.projectId, skillRequest).body

        then:
        result.results.size() == 8
        result.results[0].skillId == 'skill1'
        result.results[0].skillApplied
        result.results[0].userId == users[0]

        result.results[1].skillId == 'skill2'
        result.results[1].skillApplied
        result.results[1].userId == users[0]

        result.results[2].skillId == 'skill1'
        result.results[2].skillApplied
        result.results[2].userId == users[1]

        result.results[3].skillId == 'skill2'
        result.results[3].skillApplied
        result.results[3].userId == users[1]

        result.results[4].skillId == 'skill1'
        result.results[4].skillApplied
        result.results[4].userId == users[2]

        result.results[5].skillId == 'skill2'
        result.results[5].skillApplied
        result.results[5].userId == users[2]

        result.results[6].skillId == 'skill1'
        result.results[6].skillApplied
        result.results[6].userId == users[3]

        result.results[7].skillId == 'skill2'
        result.results[7].skillApplied
        result.results[7].userId == users[3]
    }

    def "Submit a batch of skills for multiple users with a skill not applied"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj1 = SkillsFactory.createSubject(1, 1)

        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.each {
            it.pointIncrement = 100
            it.numPerformToCompletion = 2
            it.pointIncrementInterval = 0 // ability to achieve right away
        }
        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj1)
        skillsService.createSkills(proj1_skills)

        List<String> users = getRandomUsers(4)

        skillsService.addSkill([projectId: proj1.projectId, skillId: 'skill1'], users[0], new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: 'skill1'], users[1], new Date() - 1)
        skillsService.addSkill([projectId: proj1.projectId, skillId: 'skill1'], users[1], new Date())

        def skillRequest = [
                userIds: users,
                skillIds: ['skill1', 'skill2'],
                timestamp: 1234l,
                notifyIfSkillNotApplied: true,
                isRetry: false,
        ]

        when:
        def result = skillsService.addBatchSkillsForBatchUsers(proj1.projectId, skillRequest).body

        then:
        result
        result.results.size() == 8
        result.results[0].skillId == 'skill1'
        result.results[0].skillApplied
        result.results[0].userId == users[0]

        result.results[1].skillId == 'skill2'
        result.results[1].skillApplied
        result.results[1].userId == users[0]

        result.results[2].skillId == 'skill1'
        !result.results[2].skillApplied
        result.results[2].explanation == 'This skill reached its maximum points'
        result.results[2].userId == users[1]

        result.results[3].skillId == 'skill2'
        result.results[3].skillApplied
        result.results[3].userId == users[1]

        result.results[4].skillId == 'skill1'
        result.results[4].skillApplied
        result.results[4].userId == users[2]

        result.results[5].skillId == 'skill2'
        result.results[5].skillApplied
        result.results[5].userId == users[2]

        result.results[6].skillId == 'skill1'
        result.results[6].skillApplied
        result.results[6].userId == users[3]

        result.results[7].skillId == 'skill2'
        result.results[7].skillApplied
        result.results[7].userId == users[3]
    }
}
