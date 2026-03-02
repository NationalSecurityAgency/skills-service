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
package skills.storage.repos

import jakarta.persistence.Entity
import jakarta.persistence.Id
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.lang.Nullable
import skills.storage.model.DayCountItem
import skills.storage.model.EventType
import skills.storage.model.MonthlyCountItem
import skills.storage.model.QuizDefParent
import skills.storage.model.WeekCountItem

import java.util.stream.Stream


interface GlobalProgressMetricsRepo extends JpaRepository<GlobalMetricsDummyEntity, Integer> {
    @Entity
    static class GlobalMetricsDummyEntity {
        @Id
        Integer id
    }

    static interface UserProgressMetric {
        String getUserId()
        Integer getNumProjects()
        Integer getProjectLevelsEarned()
        Integer getSubjectLevelsEarned()
        Integer getSkillsAccomplished()
        Integer getBadgesEarned()
        Integer getGlobalBadgesEarned()
        Integer getNumQuizzes()
        Integer getNumQuizzesPassed()
        Integer getNumQuizzesFailed()
        Integer getNumQuizzesInProgress()
        Integer getNumSurveys()
        Integer getNumSurveyCompleted()
        Integer getNumSurveyInProgress()
    }

    @Query(value = '''SELECT
    COALESCE(projectsAndAchievements.user_id, quizzes.user_id) as userId,
    COALESCE(projectsAndAchievements.numProjects, 0) as numProjects,
    COALESCE(projectsAndAchievements.projectLevelsEarned, 0) as projectLevelsEarned,
    COALESCE(projectsAndAchievements.subjectLevelsEarned, 0) as subjectLevelsEarned,
    COALESCE(projectsAndAchievements.skillsAccomplished, 0) as skillsAccomplished,
    COALESCE(projectsAndAchievements.badgesEarned, 0) as badgesEarned,
    COALESCE(projectsAndAchievements.globalBadgesEarned, 0) as globalBadgesEarned,
    COALESCE(quizzes.quizTotal, 0) as numQuizzes,
    COALESCE(quizzes.quizPassed, 0) as numQuizzesPassed,
    COALESCE(quizzes.quizFailed, 0) as numQuizzesFailed,
    COALESCE(quizzes.quizInProgress, 0) as numQuizzesInProgress,
    COALESCE(quizzes.surveyTotal, 0) as numSurveys,
    COALESCE(quizzes.surveyCompleted, 0) as numSurveyCompleted,
    COALESCE(quizzes.surveyInProgress, 0) as numSurveyInProgress
FROM (
         SELECT
             projects.user_id,
             projects.numProjects,
             COALESCE(achievements.projectLevelsEarned, 0) as projectLevelsEarned,
             COALESCE(achievements.subjectLevelsEarned, 0) as subjectLevelsEarned,
             COALESCE(achievements.skillsAccomplished, 0) as skillsAccomplished,
             COALESCE(achievements.badgesEarned, 0) as badgesEarned,
             COALESCE(achievements.globalBadgesEarned, 0) as globalBadgesEarned
         FROM (
                  SELECT
                      user_id,
                      count(distinct project_id) numProjects
                  FROM user_points
                  WHERE project_id IN :projectIds
                  GROUP BY user_id
              ) projects
                  LEFT JOIN (
             SELECT
                 ua.user_id,
                 count(distinct ua.project_id) numProjects,
                 SUM(CASE WHEN ua.skill_ref_id is null and ua.level is not null THEN 1 ELSE 0 END) as projectLevelsEarned,
                 SUM(CASE WHEN sd.type = 'Subject' and ua.level is not null THEN 1 ELSE 0 END) as subjectLevelsEarned,
                 SUM(CASE WHEN sd.type = 'Skill' THEN 1 ELSE 0 END) as skillsAccomplished,
                 SUM(CASE WHEN sd.type = 'Badge' THEN 1 ELSE 0 END) as badgesEarned,
                 SUM(CASE WHEN sd.type = 'GlobalBadge' THEN 1 ELSE 0 END) as globalBadgesEarned
             FROM user_achievement ua
                      LEFT JOIN skill_definition sd ON (ua.skill_ref_id = sd.id)
             WHERE (ua.project_id IN :projectIds 
                    OR ua.skill_ref_id in (SELECT DISTINCT gbld.skill_ref_id as global_badge_id
                                             FROM global_badge_level_definition gbld
                                             WHERE gbld.project_id IN :projectIds
                                    
                                             UNION
                                    
                                             SELECT DISTINCT globalBadge.id as global_badge_id
                                             FROM skill_relationship_definition srd
                                                      JOIN skill_definition globalBadge ON (srd.parent_ref_id = globalBadge.id and srd.type = 'BadgeRequirement' and globalBadge.type = 'GlobalBadge')
                                                      JOIN skill_definition skill ON (srd.child_ref_id = skill.id and srd.type = 'BadgeRequirement' and skill.type = 'Skill')
                                             WHERE skill.project_id IN :projectIds)
                    )
             GROUP BY ua.user_id
         ) achievements ON projects.user_id = achievements.user_id
     ) projectsAndAchievements
         FULL OUTER JOIN (
    select uqa.user_id, 
           SUM(CASE WHEN qd.type = 'Quiz' THEN 1 ELSE 0 END) as quizTotal,
           SUM(CASE WHEN qd.type = 'Quiz' and uqa.status = 'PASSED' THEN 1 ELSE 0 END) as quizPassed,
           SUM(CASE WHEN qd.type = 'Quiz' and uqa.status = 'FAILED' THEN 1 ELSE 0 END) as quizFailed,
           SUM(CASE WHEN qd.type = 'Quiz' and uqa.status = 'INPROGRESS' THEN 1 ELSE 0 END) as quizInProgress,
           SUM(CASE WHEN qd.type = 'Survey' THEN 1 ELSE 0 END) as surveyTotal,
           SUM(CASE WHEN qd.type = 'Survey' and uqa.status = 'PASSED' THEN 1 ELSE 0 END) as surveyCompleted,
           SUM(CASE WHEN qd.type = 'Survey' and uqa.status = 'INPROGRESS' THEN 1 ELSE 0 END) as surveyInProgress
    from user_quiz_attempt uqa
             join quiz_definition qd on (uqa.quiz_definition_ref_id = qd.id)
    where qd.quiz_id in :quizIds
    group by uqa.user_id
) quizzes ON projectsAndAchievements.user_id = quizzes.user_id''', nativeQuery = true)
    Page<UserProgressMetric> findUsersOverallProgress(
            @Param("projectIds") List<String> projectIds,
            @Param("quizIds") List<String> quizIds,
            Pageable pageable)

    static interface ProjDefCounts {
        Integer getNumSkills()
        Integer getNumBadges()
    }

    @Nullable
    @Query(value = '''select
        SUM(CASE WHEN type = 'Skill' THEN 1 ELSE 0 END) as numSkills,
        SUM(CASE WHEN type = 'Badge' THEN 1 ELSE 0 END) as numBadges
        from skill_definition
        where project_id in :projectIds''', nativeQuery = true)
    ProjDefCounts findProjectDefCounts(@Param("projectIds") List<String> projectIds)


    @Query(value = '''SELECT COUNT(DISTINCT combined_badges.global_badge_id) as totalGlobalBadges
        FROM (
                 SELECT DISTINCT gbld.skill_ref_id as global_badge_id
                 FROM global_badge_level_definition gbld
                 WHERE gbld.project_id IN :projectIds
        
                 UNION
        
                 SELECT DISTINCT globalBadge.id as global_badge_id
                 FROM skill_relationship_definition srd
                          JOIN skill_definition globalBadge ON (srd.parent_ref_id = globalBadge.id and srd.type = 'BadgeRequirement' and globalBadge.type = 'GlobalBadge')
                          JOIN skill_definition skill ON (srd.child_ref_id = skill.id and srd.type = 'BadgeRequirement' and skill.type = 'Skill')
                 WHERE skill.project_id IN :projectIds
             ) combined_badges''', nativeQuery = true)
    Integer getTotalGlobalBadgeCountForProjects(@Param("projectIds") List<String> projectIds)


    static interface QuizInfo {
        String getQuizId()
        String getQuizName()
        QuizDefParent.QuizType getQuizType()
    }

    static interface ProjectInfo {
        String getProjectId()
        String getProjectName()
    }

    @Nullable
    @Query(value = '''select quiz_id as quizId, type as quizType, name as quizName
        from quiz_definition
        where quiz_id in :quizIds''', nativeQuery = true)
    List<QuizInfo> getQuizInfo(@Param("quizIds") List<String> quizIds)

    @Nullable
    @Query(value = '''select project_id as projectId, name as projcetName
        from project_definition
        where project_id in :projectIds''', nativeQuery = true)
    List<ProjectInfo> getProjectInfo(@Param("projectIds") List<String> projectIds)

    @Query(value="""
        select min(all_events.project_id) as projectId, all_events.event_time as day, count(distinct all_events.user_id) as count from 
        (
            SELECT uue.user_id AS user_id,
                   def.project_id AS project_id,
                   uue.skill_ref_id AS skill_ref_id,
                   uue.event_time AS event_time,
                   uue.event_type AS event_type,
                   uue.count AS count,
                   uue.week_number AS week_number
            FROM user_events uue 
                INNER JOIN (
                    select case when sd.copied_from_skill_ref is not null then sd.copied_from_skill_ref else sd.id end as id, sd.project_id 
                    from skill_definition sd 
                    where sd.type = 'Skill' 
                    and sd.project_id in :projectIds
                    and sd.enabled = 'true'
                ) def ON uue.skill_ref_id = def.id
            WHERE
                uue.event_time >= :start AND
                uue.event_type = :#{#type.name()} AND
                not exists (select 1 from archived_users au where au.user_id = uue.user_id and au.project_id = uue.project_id) and 
                uue.project_id in :projectIds
        ) all_events
        group by all_events.event_time 
        order by all_events.event_time desc
    """, nativeQuery = true)
    Stream<DayCountItem> getDistinctUserCountForProjects(@Param("projectIds") List<String> projectIds, @Param("start") Date start, @Param("type") EventType type)

    @Query(value="""
        select min(ue.projectId) as projectId, ue.weekNumber as weekNumber, count(distinct ue.userId) as count 
        from UserEvent ue
        where ue.eventTime >= :start AND
        ue.skillRefId in (
            select case when sd.copiedFrom is not null then sd.copiedFrom else sd.id end as id 
            from SkillDef sd 
            WHERE sd.projectId in :projectIds 
            AND sd.type = 'Skill'
            AND sd.enabled = 'true'
        )   
        AND not exists (select 1 from ArchivedUser au where au.userId = ue.userId and au.projectId = ue.projectId)
        AND ue.projectId in :projectIds
        group by ue.weekNumber
        order by ue.weekNumber desc
    """)
    Stream<WeekCountItem> getDistinctUserCountForProjectsGroupedByWeek(@Param("projectIds") List<String> projectIds, @Param("start") Date start)

    @Query(value="""
        SELECT ue.projectId as projectId, date_trunc('month', ue.performedOn) AS month,  count(distinct ue.userId) as count
        FROM UserPerformedSkill ue WHERE ue.projectId in :projectIds and ue.performedOn >= :start
        AND not exists (select 1 from ArchivedUser au where au.userId = ue.userId and au.projectId in :projectIds)
        AND ue.projectId in :projectIds
        GROUP BY ue.projectId, month
    """)
    Stream<MonthlyCountItem> getDistinctUserCountForProjectsGroupedByMonth(@Param("projectIds") List<String> projectIds, @Param("start") Date start)

    @Query(value="""
        WITH project_users AS (
            SELECT DISTINCT ups.user_id, ups.performed_on as day
            FROM user_performed_skill ups
            WHERE ups.project_id IN :projectIds 
            AND ups.performed_on >= :start
            AND NOT EXISTS (SELECT 1 FROM archived_users au WHERE au.user_id = ups.user_id AND au.project_id IN :projectIds)
        ),
        quiz_users AS (
            SELECT DISTINCT uqa.user_id, uqa.started as day
            FROM user_quiz_attempt uqa
            JOIN quiz_definition qd ON uqa.quiz_definition_ref_id = qd.id
            WHERE qd.quiz_id IN :quizIds
            AND uqa.started >= :start
        ),
        combined_users AS (
            SELECT user_id, day FROM project_users
            UNION ALL
            SELECT user_id, day FROM quiz_users
        ),
        grouped_users AS (
            SELECT 
                user_id,
                CASE 
                    WHEN :groupingType = 'week' THEN date_trunc('week', day + INTERVAL '1 day') - INTERVAL '1 day'
                    WHEN :groupingType = 'month' THEN date_trunc('month', day)
                    ELSE day
                END as grouped_day
            FROM combined_users
        )
        SELECT grouped_day as day, COUNT(DISTINCT user_id) as count
        FROM grouped_users
        GROUP BY grouped_day
        ORDER BY day DESC
    """, nativeQuery = true)
    Stream<DayCountItem> getDistinctUserCountForProjectsAndQuizzes(
            @Param("projectIds") List<String> projectIds,
            @Param("quizIds") List<String> quizIds,
            @Param("start") Date start,
            @Param("groupingType") String groupingType)

}
