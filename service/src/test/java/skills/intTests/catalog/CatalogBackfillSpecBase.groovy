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
package skills.intTests.catalog

import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.utils.SkillsService
import skills.storage.model.UserAchievement
import skills.storage.model.UserPoints
import skills.storage.repos.ProjectParticipationRepo
import skills.storage.repos.SkillDefRepo

import static skills.intTests.utils.SkillsFactory.*

/** Shared backfill fixtures and barrier cleanup; no inherited features. */
abstract class CatalogBackfillSpecBase extends CatalogIntSpec {

    @Autowired ProjectParticipationRepo participationRepo

    @Autowired SkillDefRepo skillDefRepo

    protected List<String> users

    protected final Date firstDate = new Date(System.currentTimeMillis() - 3 * 86400000L)

    protected final Date completionDate = new Date(System.currentTimeMillis() - 2 * 86400000L)

    def setup() {
        users = getRandomUsers(8)
    }

    def cleanup() {
        removeTrigger('catalog_test_queue', 'scheduled_tasks')
        removeTrigger('catalog_test_fault', 'user_achievement')
        removeTrigger('catalog_test_schedule_fault', 'scheduled_tasks')
        removeTrigger('catalog_test_pause', 'user_points')
        jdbcTemplate.update("update scheduled_tasks set execution_time = now() where task_name = 'catalog-participant-backfill'")
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
    }

    protected Map catalogFixture(boolean finalize = true, boolean grouped = true) {
        Map source = createProject(1)
        Map sourceSubject = createSubject(1, 1)
        List<Map> sourceSkills = (11..12).collect { int id -> createSkill(1, 1, id, 0, 2, 0, 100) }
        skillsService.createProjectAndSubjectAndSkills(source, sourceSubject, sourceSkills)
        sourceSkills.each { Map skill -> skillsService.exportSkillToCatalog(source.projectId, skill.skillId) }
        Map destination = createProject(2)
        Map subject = createSubject(2, 1)
        Map local = createSkill(2, 1, 21, 0, 5, 0, 100)
        Map group = createSkillsGroup(2, 1, 30)
        skillsService.createProjectAndSubjectAndSkills(destination, subject, grouped ? [local, group] : [local])
        skillsService.createProject(createProject(3))
        skillsService.createSubject(createSubject(3, 1))
        Map fixture = [sourceSkills: sourceSkills, local: local, group: grouped ? group : null, badge: createBadge(2, 1)]
        if (finalize) {
            importSkills(fixture)
            skillsService.finalizeSkillsImportFromCatalog('TestProject2')
            skillsService.createBadge(fixture.badge + [enabled: 'false'])
            skillsService.assignSkillsToBadge('TestProject2', fixture.badge.badgeId, sourceSkills.skillId)
            skillsService.createBadge(fixture.badge + [enabled: 'true'])
        }
        return fixture
    }

    protected void importSkills(Map fixture) {
        if (fixture.group) {
            skillsService.bulkImportSkillsIntoGroupFromCatalog('TestProject2', 'TestSubject1', fixture.group.skillId, fixture.sourceSkills)
        } else {
            skillsService.bulkImportSkillsFromCatalog('TestProject2', 'TestSubject1', fixture.sourceSkills)
        }
    }

    protected void earnSource(Map fixture, String userId) {
        fixture.sourceSkills.each { Map skill ->
            skillsService.addSkill(skill, userId, firstDate)
            skillsService.addSkill(skill, userId, completionDate)
        }
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
    }

    protected Map qualify(Map fixture, SkillsService learner, String entry) {
        entry == 'visit' ? learner.initializeProjectProgress('TestProject2') : skillsService.addSkill(fixture.local, learner.userName).body
    }

    protected int points(String projectId, String userId, String skillId) {
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(projectId, userId, skillId)?.points ?: 0
    }

    protected UserAchievement achievement(String projectId, String userId, String skillId) {
        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(userId, projectId, skillId).find { UserAchievement achievement -> achievement.level == null }
    }

    protected List<Map> creditSnapshot(String projectId, String userId) {
        List<Map> points = userPointsRepo.findByProjectIdAndUserId(projectId, userId)
                .sort { UserPoints row -> row.id }
                .collect { UserPoints row -> [id: row.id, points: row.points, skill_id: row.skillId] }
        List<Map> achievements = userAchievedRepo.findAllByUserAndProjectIds(userId, [projectId])
                .sort { UserAchievement row -> row.id }
                .collect { UserAchievement row ->
                    [id: row.id, skill_id: row.skillId, level: row.level, achieved_on: row.achievedOn?.clone()]
                }
        return points + achievements
    }
}
