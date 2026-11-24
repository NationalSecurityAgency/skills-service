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
package skills.intTests.skillExpiration

import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsService
import skills.services.attributes.ExpirationAttrs
import skills.services.settings.SettingsService
import skills.storage.repos.ProjectParticipationRepo
import skills.tasks.executors.ExpireUserAchievementsTaskExecutor

import static skills.intTests.utils.SkillsFactory.*

class ProjectUserRemovalExpirationSpecs extends DefaultIntSpec {

    @Autowired ExpireUserAchievementsTaskExecutor expirationExecutor
    @Autowired SettingsService settingsService
    @Autowired ProjectParticipationRepo participationRepo

    def cleanup() {
        settingsService.deleteGlobalSetting(ExpireUserAchievementsTaskExecutor.SKILL_EXPIRATION_LAST_RUN_DATE)
    }

    def "#operation clears all expired history and summaries only for the project user, including after initialization"() {
        List<Map> skills = (1..2).collect { int id -> createSkill(1, 1, id, 0, 1, 0, 100) }
        Map otherSkill = createSkill(2, 1, 1, 0, 1, 0, 100)
        skillsService.createProjectAndSubjectAndSkills(createProject(1), createSubject(1, 1), skills)
        skillsService.createProjectAndSubjectAndSkills(createProject(2), createSubject(2, 1), [otherSkill])
        List<String> users = getRandomUsers(2)
        SkillsService learner = createService(users[0])
        SkillsService peer = createService(users[1])
        learner.initializeProjectProgress('TestProject1')
        peer.initializeProjectProgress('TestProject1')
        (1..2).each { int cycle ->
            Date earned = new Date() - (cycle == 1 ? 20 : 10)
            skills.each { Map skill ->
                users.each { String userId -> assert skillsService.addSkill(skill, userId, earned).body.skillApplied }
            }
            assert skillsService.addSkill(otherSkill, users[0], earned).body.skillApplied
            expire(skills + [otherSkill])
        }

        expect:
        skillsService.getExpiredSkills('TestProject1', '', '').data*.skillId.sort() ==
                ['skill1', 'skill1', 'skill1', 'skill1', 'skill2', 'skill2', 'skill2', 'skill2']
        skillsService.getExpiredSkills('TestProject2', '', '').data*.skillId == ['skill1', 'skill1']
        learner.getSingleSkillSummaryForCurrentUser('TestProject1', 'skill1').lastExpirationDate != null
        learner.getSubjectSummaryForCurrentUser('TestProject1', 'TestSubject1').skills*.lastExpirationDate.every { String date -> date != null }
        skillsService.getProjectUsers('TestProject1').data*.userId.sort() == users.sort(false)

        when:
        if (operation == 'removal') {
            skillsService.removeUserFromProject('testproject1', users[0].toUpperCase(Locale.ROOT))
        } else {
            skillsService.deleteAllSkillEvents([projectId: 'TestProject1', userId: users[0]])
        }

        then:
        skillsService.getExpiredSkills('TestProject1', '', '').data*.userId == [users[1]] * 4
        skillsService.getExpiredSkills('TestProject1', '', '').data*.skillId.sort() == ['skill1', 'skill1', 'skill2', 'skill2']
        skillsService.getExpiredSkills('TestProject2', '', '').data*.skillId == ['skill1', 'skill1']
        learner.getSingleSkillSummaryForCurrentUser('TestProject1', 'skill1').lastExpirationDate == null
        learner.getSubjectSummaryForCurrentUser('TestProject1', 'TestSubject1').skills*.lastExpirationDate == [null, null]
        peer.getSingleSkillSummaryForCurrentUser('TestProject1', 'skill1').lastExpirationDate != null
        learner.getSingleSkillSummaryForCurrentUser('TestProject2', 'skill1').lastExpirationDate != null
        skillsService.getProjectUsers('TestProject1').data*.userId.sort() ==
                (operation == 'removal' ? [users[1]] : users.sort(false))

        when:
        learner.initializeProjectProgress('TestProject1')

        then:
        learner.getSkillsSummaryForCurrentUser('TestProject1').points == 0
        learner.getSingleSkillSummaryForCurrentUser('TestProject1', 'skill1').lastExpirationDate == null
        learner.getSubjectSummaryForCurrentUser('TestProject1', 'TestSubject1').skills*.lastExpirationDate == [null, null]
        skillsService.getProjectUsers('TestProject1').data*.userId.sort() == users.sort(false)

        when:
        skills.each { Map skill -> assert learner.addSkill(skill).body.skillApplied }

        then:
        learner.getSkillsSummaryForCurrentUser('TestProject1').points == 200
        learner.getSkillsSummaryForCurrentUser('TestProject1').skillsAchieved == 2
        skillsService.getExpiredSkills('TestProject1', '', '').data*.userId == [users[1]] * 4

        where:
        operation << ['removal', 'Delete All']
    }

    def "#operation preserves expired-only history without participation or active progress - archived #archived"() {
        Map skill = createSkill(1, 1, 1, 0, 1, 0, 100)
        skillsService.createProjectAndSubjectAndSkills(createProject(1), createSubject(1, 1), [skill])
        String userId = getRandomUsers(1)[0]
        SkillsService learner = createService(userId)
        assert skillsService.addSkill(skill, userId, new Date() - 10).body.skillApplied
        if (archived) {
            skillsService.archiveUsers([userId], 'TestProject1')
        }
        expire([skill])
        String lastExpirationDate = learner.getSingleSkillSummaryForCurrentUser('TestProject1', skill.skillId).lastExpirationDate
        Long actionsBefore = skillsService.getUserActionsForProject('TestProject1').totalCount

        expect:
        skillsService.getProjectUsers('TestProject1').count == 0
        skillsService.getPerformedSkills(userId, 'TestProject1').totalCount == 0
        learner.getSkillsSummaryForCurrentUser('TestProject1').points == 0
        skillsService.getArchivedUsers('TestProject1').data*.userId == (archived ? [userId] : [])
        !userPointsRepo.existsByProjectIdAndUserId('TestProject1', userId)
        !participationRepo.existsByProjectIdAndUserId('TestProject1', userId)
        skillsService.getExpiredSkills('TestProject1', '', '').data*.skillId == [skill.skillId]
        lastExpirationDate != null

        when:
        if (removeFromProject) {
            skillsService.removeUserFromProject('testproject1', userId.toUpperCase(Locale.ROOT))
        } else {
            skillsService.deleteAllSkillEvents([projectId: 'TestProject1', userId: userId])
        }

        then:
        skillsService.getExpiredSkills('TestProject1', '', '').data*.skillId == [skill.skillId]
        learner.getSingleSkillSummaryForCurrentUser('TestProject1', skill.skillId).lastExpirationDate == lastExpirationDate
        skillsService.getProjectUsers('TestProject1').count == 0
        skillsService.getUserActionsForProject('TestProject1').totalCount == actionsBefore + (archived && removeFromProject ? 1 : 0)
        skillsService.getArchivedUsers('TestProject1').data*.userId == (archived && !removeFromProject ? [userId] : [])

        when:
        learner.initializeProjectProgress('TestProject1')

        then:
        skillsService.getArchivedUsers('TestProject1').data*.userId == (archived && !removeFromProject ? [userId] : [])
        skillsService.getProjectUsers('TestProject1').data*.userId == (archived && !removeFromProject ? [] : [userId])

        where:
        operation     | removeFromProject | archived
        'Remove User' | true              | false
        'Delete All'  | false             | false
        'Remove User' | true              | true
        'Delete All'  | false             | true
    }

    def "Delete All clears active progress and expired history and summary state"() {
        List<Map> skills = (1..2).collect { int id -> createSkill(1, 1, id, 0, 1, 0, 100) }
        skillsService.createProjectAndSubjectAndSkills(createProject(1), createSubject(1, 1), skills)
        String userId = getRandomUsers(1)[0]
        SkillsService learner = createService(userId)
        assert skillsService.addSkill(skills[0], userId, new Date() - 10).body.skillApplied
        expire([skills[0]])
        assert learner.addSkill(skills[1]).body.skillApplied

        expect:
        learner.getSingleSkillSummaryForCurrentUser('TestProject1', 'skill1').lastExpirationDate != null
        skillsService.getExpiredSkills('TestProject1', '', '').data*.skillId == ['skill1']
        learner.getSkillsSummaryForCurrentUser('TestProject1').points == 100

        when:
        skillsService.deleteAllSkillEvents([projectId: 'TestProject1', userId: userId])

        then:
        learner.getSkillsSummaryForCurrentUser('TestProject1').points == 0
        learner.getSkillsSummaryForCurrentUser('TestProject1').skillsAchieved == 0
        skillsService.getPerformedSkills(userId, 'TestProject1').totalCount == 0
        skillsService.getExpiredSkills('TestProject1', '', '').totalCount == 0
        learner.getSingleSkillSummaryForCurrentUser('TestProject1', 'skill1').lastExpirationDate == null
        learner.getSubjectSummaryForCurrentUser('TestProject1', 'TestSubject1').skills*.lastExpirationDate == [null, null]
    }

    def "individual skill event deletion preserves expired history and summary state"() {
        List<Map> skills = (1..2).collect { int id -> createSkill(1, 1, id, 0, 1, 0, 100) }
        skillsService.createProjectAndSubjectAndSkills(createProject(1), createSubject(1, 1), skills)
        String userId = getRandomUsers(1)[0]
        SkillsService learner = createService(userId)
        assert skillsService.addSkill(skills[0], userId, new Date() - 10).body.skillApplied
        expire([skills[0]])
        Date performedOn = new Date()
        assert skillsService.addSkill(skills[1], userId, performedOn).body.skillApplied
        String lastExpirationDate = learner.getSingleSkillSummaryForCurrentUser('TestProject1', 'skill1').lastExpirationDate

        expect:
        lastExpirationDate != null
        learner.getSkillsSummaryForCurrentUser('TestProject1').points == 100

        when:
        skillsService.deleteSkillEvent(skills[1] + [userId: userId, timestamp: performedOn.time])

        then:
        learner.getSkillsSummaryForCurrentUser('TestProject1').points == 0
        skillsService.getExpiredSkills('TestProject1', '', '').data*.skillId == ['skill1']
        learner.getSingleSkillSummaryForCurrentUser('TestProject1', 'skill1').lastExpirationDate == lastExpirationDate
        learner.getSubjectSummaryForCurrentUser('TestProject1', 'TestSubject1').skills*.lastExpirationDate == [lastExpirationDate, null]
    }

    private void expire(List<Map> skills) {
        skills.each { Map skill ->
            skillsService.saveSkillExpirationAttributes(skill.projectId, skill.skillId,
                    [expirationType: ExpirationAttrs.DAILY, every: 7])
        }
        expirationExecutor.removeExpiredUserAchievements()
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        settingsService.deleteGlobalSetting(ExpireUserAchievementsTaskExecutor.SKILL_EXPIRATION_LAST_RUN_DATE)
    }
}
