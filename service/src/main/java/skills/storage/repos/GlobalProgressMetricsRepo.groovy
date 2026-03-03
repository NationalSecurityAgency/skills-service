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
import skills.storage.model.QuizDefParent

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
        Integer getNumSkillsEarned()
        Integer getNumBadgesEarned()
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
    COALESCE(userAttrsFromProj.user_id_for_display, userAttrsFromQuiz.user_id_for_display) as userIdForDisplay,
    COALESCE(projectsAndAchievements.numProjects, 0) as numProjects,
    COALESCE(projectsAndAchievements.projectLevelsEarned, 0) as projectLevelsEarned,
    COALESCE(projectsAndAchievements.subjectLevelsEarned, 0) as subjectLevelsEarned,
    COALESCE(projectsAndAchievements.skillsAccomplished, 0) as numSkillsEarned,
    COALESCE(projectsAndAchievements.badgesEarned, 0) as numBadgesEarned,
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
) quizzes ON projectsAndAchievements.user_id = quizzes.user_id
 LEFT JOIN user_attrs userAttrsFromProj ON (projectsAndAchievements.user_id = userAttrsFromProj.user_id)
 LEFT JOIN user_attrs userAttrsFromQuiz ON (quizzes.user_id = userAttrsFromQuiz.user_id)''', nativeQuery = true)
    Page<UserProgressMetric> findUsersOverallProgress(
            @Param("projectIds") List<String> projectIds,
            @Param("quizIds") List<String> quizIds,
            Pageable pageable)

    static interface SingleUserProjectProgress {
        String getProjectId()
        String getProjectName()
        Integer getNumSkills()
        Integer getProjectTotalPoints()
        Integer getNumProjectLevels()
        Integer getNumBadges()
        Integer getPoints()
        Date getUpdated()
    }

    @Query(value = '''SELECT up.project_id                                      as projectId,
                   max(pd.name)                                       as projectName,
                   SUM(CASE WHEN sd.type = 'Skill' THEN 1 ELSE 0 END) as numSkills,
                   SUM(CASE WHEN sd.type = 'Skill' THEN sd.total_points ELSE 0 END) as projectTotalPoints,
                   SUM(CASE WHEN sd.type = 'Badge' THEN 1 ELSE 0 END) as numBadges,
                   max(up.points)                                     as points,
                   max(ld.level)                                      as numProjectLevels,
                   max(up.updated)                                    as updated
            FROM user_points up
                     join project_definition pd on (up.project_id = pd.project_id)
                     left join skill_definition sd on (pd.project_id = sd.project_id)
                     left join level_definition ld on (pd.id = ld.project_ref_id and ld.skill_ref_id is null)
            WHERE up.project_id IN :projectIds
              and up.user_id = :userId
              and up.skill_ref_id is null
            group by up.project_id''', nativeQuery = true)
    List<SingleUserProjectProgress> findSingleUserProjectsProgress(@Param("userId") String userId,  @Param("projectIds") List<String> projectIds)

    static interface SingleUserAchievement {
        String getProjectId()
        Integer getNumAchievedSkills()
        Integer getAchievedProjLevel()
    }

    @Query(value = '''select up.project_id as                                                         projectId,
                   SUM(CASE WHEN ua.id is not null and sd.id is not null THEN 1 ELSE 0 END)                       numAchievedSkills,
                   COALESCE(MAX(projAchievement.level), 0)                                                        achievedProjLevel
            from user_points up
                     left join skill_definition sd on (up.skill_ref_id = sd.id and sd.type = 'Skill')
                     left join user_achievement ua on (up.user_id = ua.user_id and ua.skill_ref_id = sd.id)
                     left join user_achievement projAchievement on (
                up.user_id = projAchievement.user_id
                    and projAchievement.project_id = up.project_id
                    and projAchievement.skill_ref_id is null
                    and up.skill_ref_id is null
                    and projAchievement.level is not null)
            where up.user_id = :userId
              and up.project_id in :projectIds
            group by up.project_id''', nativeQuery = true)
    List<SingleUserAchievement> findSingleUserAchievements(@Param("userId") String userId,  @Param("projectIds") List<String> projectIds)


    static interface SingleUserAchievedBadgeCounts {
        String getProjectId()
        Integer getNumAchievedBadges()
    }

    @Query(value = '''select ua.project_id as projectId,
                count(ua.id) as numAchievedBadges
                from user_achievement ua
                left join skill_definition sd on (ua.skill_ref_id = sd.id)
                where ua.user_id = :userId
                and ua.project_id IN :projectIds
                and sd.type = 'Badge'
                group by ua.project_id''', nativeQuery = true)
    List<SingleUserAchievedBadgeCounts> findSingleUserAchievedBadgeCounts(@Param("userId") String userId,  @Param("projectIds") List<String> projectIds)



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
    @Query(value = '''select  project_id as projectId, name as projectName
        from project_definition
        where project_id in :projectIds''', nativeQuery = true)
    List<ProjectInfo> getProjectInfo(@Param("projectIds") List<String> projectIds)

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
                    ELSE date_trunc('day', day)
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
