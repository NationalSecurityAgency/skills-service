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
package skills.storage.repos

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.lang.Nullable
import skills.storage.model.ProjDef
import skills.storage.model.ProjSummaryResult
import skills.storage.model.ProjectLastTouched
import skills.storage.model.ProjectSummaryResult
import skills.storage.model.ProjectTotalPoints

interface ProjDefRepo extends CrudRepository<ProjDef, Long> {

    @Nullable
    ProjDef findByProjectIdIgnoreCase(String projectId)

    @Query(value="""
                SELECT 
                    pd.project_id AS projectId,
                    pd.name AS name,
                    pd.total_points AS totalPoints,
                    COALESCE(errors.errorCount, 0) AS numErrors,
                    COALESCE(skills.skillCount, 0) AS numSkills,
                    COALESCE(disabledSkills.skillCount, 0) AS numSkillsDisabled,
                    COALESCE(badges.badgeCount, 0) AS numBadges,
                    COALESCE(subjects.subjectCount, 0) AS numSubjects,
                    COALESCE(groups.groupCount, 0) AS numGroups,
                    COALESCE(CAST(expiration.expiringUnused AS BOOLEAN), false) as expiring,
                    expiration.expirationTriggeredDate as expirationTriggered,
                    reusedSkills.skillCount AS numSkillsReused,
                    reusedSkills.totalPoints AS totalPointsReused, 
                    COALESCE(CAST(userCommunity.protectedCommunityEnabled AS BOOLEAN), false) as protectedCommunityEnabled,
                    pd.created,
                    GREATEST(skills.skillUpdated, badges.badgeUpdated, subjects.subjectUpdated, pd.updated) as lastEdited
                FROM project_definition pd
                LEFT JOIN (SELECT project_id, COUNT(id) AS errorCount FROM project_error GROUP BY project_id) errors ON errors.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS skillCount, MAX(updated) AS skillUpdated FROM skill_definition WHERE type = 'Skill' and skill_id not like '%STREUSESKILLST%' and enabled = 'true' GROUP BY project_id) skills ON skills.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS skillCount, SUM(total_points) AS totalPoints FROM skill_definition WHERE type = 'Skill' and skill_id like '%STREUSESKILLST%' and enabled = 'true' GROUP BY project_id) reusedSkills ON reusedSkills.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS skillCount, MAX(updated) AS skillUpdated FROM skill_definition WHERE type = 'Skill' and enabled = 'false' GROUP BY project_id) disabledSkills ON disabledSkills.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS badgeCount, MAX(updated) AS badgeUpdated FROM skill_definition WHERE type = 'Badge' GROUP BY project_id) badges ON badges.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS subjectCount, MAX(updated) AS subjectUpdated FROM skill_definition WHERE type = 'Subject' GROUP BY project_id) subjects ON subjects.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS groupCount FROM skill_definition WHERE type = 'SkillsGroup' and enabled = 'true' GROUP BY project_id) groups ON groups.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, value AS expiringUnused, updated as expirationTriggeredDate FROM settings WHERE type = 'Project' AND setting = 'expiration.expiring.unused' AND value = 'true') expiration ON expiration.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, value AS protectedCommunityEnabled FROM settings WHERE setting = 'user_community' and value = 'true') userCommunity ON userCommunity.project_id = pd.project_id
                WHERE pd.project_id in ?1 
            """, nativeQuery = true)
    @Nullable
    List<ProjSummaryResult> getAllSummariesByProjectIdIn(List<String> projectIds)

    @Query('''select p from ProjDef p where lower(p.name) like lower(CONCAT('%', :query, '%'))''')
    @Nullable
    List<ProjDef> findAllByNameLike(@Param("query") query)

    @Nullable
    @Query('''select p from ProjDef p
            where lower(p.name) like lower(CONCAT('%', :query, '%'))
            and p.projectId not in (:projectIds)
            and not exists (select 1 from Setting s2 where p.projectId = s2.projectId and s2.setting = 'user_community' and s2.value = 'true')
    ''')
    List<ProjDef> findAllByNameLikeAndProjectIdNotIn(@Param("query") String query, @Param("projectIds") List<String> projectIds, Pageable pageable)

    @Query('''select count(p.projectId)
        from ProjDef p
        where lower(p.name) like lower(CONCAT('%', :query, '%'))
            and p.projectId not in (:projectIds)
            and not exists (select 1 from Setting s2 where p.projectId = s2.projectId and s2.setting = 'user_community' and s2.value = 'true')
    ''')
    Integer countAllByNameLikeAndProjectIdNotIn(@Param("query") String query, @Param("projectIds") List<String> projectIds)

    @Query(value="""
                SELECT 
                    pd.project_id AS projectId,
                    pd.name AS name,
                    pd.total_points AS totalPoints,
                    COALESCE(errors.errorCount, 0) AS numErrors,
                    COALESCE(skills.skillCount, 0) AS numSkills,
                    COALESCE(disabledSkills.skillCount, 0) AS numSkillsDisabled,
                    COALESCE(badges.badgeCount, 0) AS numBadges,
                    COALESCE(subjects.subjectCount, 0) AS numSubjects,
                    COALESCE(groups.groupCount, 0) AS numGroups,
                    COALESCE(CAST(expiration.expiringUnused AS BOOLEAN), false) as expiring,
                    expiration.expirationTriggeredDate as expirationTriggered,
                    events.latest AS lastReportedSkill,
                    COALESCE(CAST(userCommunity.protectedCommunityEnabled AS BOOLEAN), false) as protectedCommunityEnabled,
                    pd.created,
                    GREATEST(skills.skillUpdated, badges.badgeUpdated, subjects.subjectUpdated, pd.updated) as lastEdited
                FROM project_definition pd
                LEFT JOIN (SELECT project_id, MAX(event_time) AS latest FROM user_events GROUP BY project_id) events ON events.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS errorCount FROM project_error GROUP BY project_id) errors ON errors.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS skillCount, MAX(updated) AS skillUpdated FROM skill_definition WHERE type = 'Skill' and enabled = 'true' GROUP BY project_id) skills ON skills.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS skillCount, MAX(updated) AS skillUpdated FROM skill_definition WHERE type = 'Skill' and enabled = 'false' GROUP BY project_id) disabledSkills ON disabledSkills.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS badgeCount, MAX(updated) AS badgeUpdated FROM skill_definition WHERE type = 'Badge' GROUP BY project_id) badges ON badges.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS subjectCount, MAX(updated) AS subjectUpdated FROM skill_definition WHERE type = 'Subject' GROUP BY project_id) subjects ON subjects.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS groupCount FROM skill_definition WHERE type = 'SkillsGroup' and enabled = 'true' GROUP BY project_id) groups ON groups.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, value AS expiringUnused, updated as expirationTriggeredDate FROM settings WHERE type = 'Project' AND setting = 'expiration.expiring.unused' AND value = 'true') expiration ON expiration.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, value AS protectedCommunityEnabled FROM settings WHERE setting = 'user_community' and value = 'true') userCommunity ON userCommunity.project_id = pd.project_id
            """, nativeQuery = true)
    @Nullable
    List<ProjSummaryResult> getAllSummaries()

    List<ProjDef> findAll()

    void deleteByProjectIdIgnoreCase(String projectId)

    @Query(value="""SELECT
                    pd.project_id AS projectId,
                    pd.name AS name,
                    pd.total_points AS totalPoints,
                    COALESCE(errors.errorCount, 0) AS numErrors,
                    COALESCE(skills.skillCount, 0) AS numSkills,
                    COALESCE(disabledSkills.skillCount, 0) AS numSkillsDisabled,
                    COALESCE(badges.badgeCount, 0) AS numBadges,
                    COALESCE(subjects.subjectCount, 0) AS numSubjects,
                    COALESCE(groups.groupCount, 0) AS numGroups,
                    COALESCE(CAST(expiration.expiringUnused AS BOOLEAN), false) as expiring,
                    COALESCE(reusedSkills.reusedSkillCount, 0) AS numSkillsReused,
                    COALESCE(reusedSkills.reusedTotalPoints, 0) AS totalPointsReused,
                    expiration.expirationTriggeredDate as expirationTriggered,
                    pd.created,
                    GREATEST(skills.skillUpdated, badges.badgeUpdated, subjects.subjectUpdated, pd.updated) as lastEdited
                FROM project_definition pd
                         LEFT JOIN (SELECT max(project_id) as project_id, COUNT(id) AS errorCount FROM project_error where LOWER(project_id) = LOWER(?1)) errors ON errors.project_id = pd.project_id
                         LEFT JOIN (SELECT max(project_id) as project_id, COUNT(id) AS skillCount, MAX(updated) as skillUpdated FROM skill_definition WHERE type = 'Skill' and enabled = 'false' and LOWER(project_id) = LOWER(?1)) disabledSkills ON disabledSkills.project_id = pd.project_id
                         LEFT JOIN (SELECT max(project_id) AS project_id, COUNT(id) AS skillCount, MAX(updated) as skillUpdated FROM skill_definition WHERE type = 'Skill' and enabled = 'true' and skill_id not like '%STREUSESKILLST%' and LOWER(project_id) = LOWER(?1)) skills ON skills.project_id = pd.project_id
                         LEFT JOIN (SELECT max(project_id) AS project_id, COUNT(id) AS reusedSkillCount, SUM(total_points) as reusedTotalPoints FROM skill_definition WHERE type = 'Skill' and enabled = 'true' and skill_id like '%STREUSESKILLST%' and LOWER(project_id) = LOWER(?1)) reusedSkills ON reusedSkills.project_id = pd.project_id
                         LEFT JOIN (SELECT max(project_id) AS project_id, COUNT(id) AS badgeCount, MAX(updated) as badgeUpdated FROM skill_definition WHERE type = 'Badge' and LOWER(project_id) = LOWER(?1)) badges ON badges.project_id = pd.project_id
                         LEFT JOIN (SELECT max(project_id) AS project_id, COUNT(id) AS subjectCount, MAX(updated) as subjectUpdated FROM skill_definition WHERE type = 'Subject' and LOWER(project_id) = LOWER(?1)) subjects ON subjects.project_id = pd.project_id
                         LEFT JOIN (SELECT max(project_id) AS project_id, COUNT(id) AS groupCount FROM skill_definition WHERE type = 'SkillsGroup' and enabled = 'true' and LOWER(project_id) = LOWER(?1)) groups ON groups.project_id = pd.project_id
                         LEFT JOIN (SELECT project_id, value AS expiringUnused, updated as expirationTriggeredDate FROM settings WHERE type = 'Project' AND setting = 'expiration.expiring.unused' AND value = 'true' and LOWER(project_id) = LOWER(?1)) expiration ON expiration.project_id = pd.project_id
                WHERE pd.project_id = ?1
            """, nativeQuery = true)
    @Nullable
    ProjSummaryResult getSummaryByProjectId(String projectId)

    @Nullable
    ProjDef findByProjectId(String projectId)

    @Query(value="""
                SELECT 
                    pd.project_id AS projectId,
                    pd.name AS name,
                    pd.total_points AS totalPoints,
                    COALESCE(errors.errorCount, 0) AS numErrors,
                    COALESCE(skills.skillCount, 0) AS numSkills,
                    COALESCE(badges.badgeCount, 0) AS numBadges,
                    COALESCE(subjects.subjectCount, 0) AS numSubjects,
                    COALESCE(groups.groupCount, 0) AS numGroups,
                    COALESCE(CAST(expiration.expiringUnused AS BOOLEAN), false) as expiring,
                    expiration.expirationTriggeredDate as expirationTriggered,
                    events.latest AS lastReportedSkill,
                    pd.created
                FROM project_definition pd
                LEFT JOIN (SELECT project_id, MAX(event_time) AS latest FROM user_events GROUP BY project_id) events ON events.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS errorCount FROM project_error GROUP BY project_id) errors ON errors.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS skillCount FROM skill_definition WHERE type = 'Skill' and enabled = 'true' GROUP BY project_id) skills ON skills.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS badgeCount FROM skill_definition WHERE type = 'Badge' GROUP BY project_id) badges ON badges.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS subjectCount FROM skill_definition WHERE type = 'Subject' GROUP BY project_id) subjects ON subjects.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS groupCount FROM skill_definition WHERE type = 'SkillsGroup' and enabled = 'true' GROUP BY project_id) groups ON groups.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, value AS expiringUnused, updated as expirationTriggeredDate FROM settings WHERE type = 'Project' AND setting = 'expiration.expiring.unused' AND value = 'true') expiration ON expiration.project_id = pd.project_id
                WHERE LOWER(pd.name) = LOWER(?1)
            """, nativeQuery = true)
    @Nullable
    ProjSummaryResult getSummaryByNameIgnoreCase(String projectId)

    @Nullable
    ProjDef findByNameIgnoreCase(String projectId)

    boolean existsByProjectIdIgnoreCase(String projectId)
    boolean existsByNameIgnoreCase(String projectName)

    @Query(value="""
                SELECT 
                    pd.project_id AS projectId,
                    pd.name AS name,
                    pd.total_points AS totalPoints,
                    COALESCE(errors.errorCount, 0) AS numErrors,
                    COALESCE(skills.skillCount, 0) AS numSkills,
                    COALESCE(disabledSkills.skillCount, 0) AS numSkillsDisabled,
                    COALESCE(badges.badgeCount, 0) AS numBadges,
                    COALESCE(subjects.subjectCount, 0) AS numSubjects,
                    COALESCE(groups.groupCount, 0) AS numGroups,
                    COALESCE(CAST(expiration.expiringUnused AS BOOLEAN), false) as expiring,
                    expiration.expirationTriggeredDate as expirationTriggered,
                    reusedSkills.skillCount AS numSkillsReused,
                    reusedSkills.totalPoints AS totalPointsReused, 
                    ur.role_name as userRole,
                    COALESCE(CAST(userCommunity.protectedCommunityEnabled AS BOOLEAN), false) as protectedCommunityEnabled,
                    pd.created
                FROM project_definition pd
                LEFT JOIN (SELECT project_id, COUNT(id) AS errorCount FROM project_error GROUP BY project_id) errors ON errors.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS skillCount FROM skill_definition WHERE type = 'Skill' and skill_id not like '%STREUSESKILLST%' and enabled = 'true' GROUP BY project_id) skills ON skills.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS skillCount, sum(total_points) as totalPoints FROM skill_definition WHERE type = 'Skill' and skill_id like '%STREUSESKILLST%' and enabled = 'true' GROUP BY project_id) reusedSkills ON reusedSkills.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS skillCount FROM skill_definition WHERE type = 'Skill' and enabled = 'false' GROUP BY project_id) disabledSkills ON disabledSkills.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS badgeCount FROM skill_definition WHERE type = 'Badge' GROUP BY project_id) badges ON badges.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS subjectCount FROM skill_definition WHERE type = 'Subject' GROUP BY project_id) subjects ON subjects.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS groupCount FROM skill_definition WHERE type = 'SkillsGroup' and enabled = 'true' GROUP BY project_id) groups ON groups.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, value AS expiringUnused, updated as expirationTriggeredDate FROM settings WHERE type = 'Project' AND setting = 'expiration.expiring.unused' AND value = 'true') expiration ON expiration.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, value AS protectedCommunityEnabled FROM settings WHERE setting = 'user_community' and value = 'true') userCommunity ON userCommunity.project_id = pd.project_id
                JOIN user_roles ur on (ur.project_id = pd.project_id AND ur.role_name in ('ROLE_PROJECT_ADMIN', 'ROLE_PROJECT_APPROVER'))
                WHERE ur.user_id = ?1
            """, nativeQuery = true)
    @Nullable
    List<ProjSummaryResult> getProjectSummariesByUser(String userId)

    @Query(value = "select u.projectId from UserRole u where u.userId=?1 and u.projectId is not null")
    List<String> getProjectIdsByUser(String userId)

    @Query(value = """
                SELECT 
                    pd.project_id AS projectId,
                    pd.name AS name,
                    pd.total_points AS totalPoints,
                    COALESCE(errors.errorCount, 0) AS numErrors,
                    COALESCE(skills.skillCount, 0) AS numSkills,
                    COALESCE(disabledSkills.skillCount, 0) AS numSkillsDisabled,
                    COALESCE(badges.badgeCount, 0) AS numBadges,
                    COALESCE(subjects.subjectCount, 0) AS numSubjects,
                    COALESCE(groups.groupCount, 0) AS numGroups,
                    COALESCE(CAST(expiration.expiringUnused AS BOOLEAN), false) as expiring,
                    COALESCE(CAST(userCommunity.protectedCommunityEnabled AS BOOLEAN), false) as protectedCommunityEnabled,
                    expiration.expirationTriggeredDate as expirationTriggered,
                    events.latest AS lastReportedSkill,
                    pd.created
                FROM project_definition pd
                LEFT JOIN (SELECT project_id, MAX(event_time) AS latest FROM user_events GROUP BY project_id) events ON events.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS errorCount FROM project_error GROUP BY project_id) errors ON errors.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS skillCount FROM skill_definition WHERE type = 'Skill' and enabled = 'true' GROUP BY project_id) skills ON skills.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS skillCount FROM skill_definition WHERE type = 'Skill' and enabled = 'false' GROUP BY project_id) disabledSkills ON disabledSkills.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS badgeCount FROM skill_definition WHERE type = 'Badge' GROUP BY project_id) badges ON badges.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS subjectCount FROM skill_definition WHERE type = 'Subject' GROUP BY project_id) subjects ON subjects.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS groupCount FROM skill_definition WHERE type = 'SkillsGroup' and enabled = 'true' GROUP BY project_id) groups ON groups.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, value AS expiringUnused, updated as expirationTriggeredDate FROM settings WHERE type = 'Project' AND setting = 'expiration.expiring.unused' AND value = 'true') expiration ON expiration.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, value AS protectedCommunityEnabled FROM settings WHERE setting = 'user_community' and value = 'true') userCommunity ON userCommunity.project_id = pd.project_id
                WHERE LOWER(pd.name) LIKE LOWER(CONCAT('%',?1,'%'))
            """, nativeQuery = true)
    @Nullable
    List<ProjSummaryResult> getSummariesByNameLike(String search)

    @Query(value = "select count(p.id) from ProjDef p, UserRole u where p.projectId = u.projectId and u.userId=?1")
    Integer getProjectsByUserCount(String userId)

    @Query(value="""
                SELECT 
                    pd.project_id AS projectId,
                    pd.name AS name,
                    pd.total_points AS totalPoints,
                    COALESCE(errors.errorCount, 0) AS numErrors,
                    COALESCE(skills.skillCount, 0) AS numSkills,
                    COALESCE(badges.badgeCount, 0) AS numBadges,
                    COALESCE(subjects.subjectCount, 0) AS numSubjects,
                    COALESCE(groups.groupCount, 0) AS numGroups,
                    COALESCE(CAST(expiration.expiringUnused AS BOOLEAN), false) as expiring,
                    expiration.expirationTriggeredDate as expirationTriggered,
                    events.latest AS lastReportedSkill,
                    pd.created
                FROM project_definition pd
                LEFT JOIN (SELECT project_id, MAX(event_time) AS latest FROM user_events GROUP BY project_id) events ON events.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS errorCount FROM project_error GROUP BY project_id) errors ON errors.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS skillCount FROM skill_definition WHERE type = 'Skill' and enabled = 'true' GROUP BY project_id) skills ON skills.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS badgeCount FROM skill_definition WHERE type = 'Badge' GROUP BY project_id) badges ON badges.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS subjectCount FROM skill_definition WHERE type = 'Subject' GROUP BY project_id) subjects ON subjects.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS groupCount FROM skill_definition WHERE type = 'SkillsGroup' and enabled = 'true' GROUP BY project_id) groups ON groups.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, value AS expiringUnused, updated as expirationTriggeredDate FROM settings WHERE type = 'Project' AND setting = 'expiration.expiring.unused' AND value = 'true') expiration ON expiration.project_id = pd.project_id
                WHERE pd.project_id <> ?2 AND LOWER(pd.name) LIKE %?1%
            """, nativeQuery = true)
    @Nullable
    List<ProjSummaryResult> queryProjectSummariesByNameAndNotProjectId(String nameQuery, String notProjectId, Pageable pageable)
    // query needs to be updated to handle paging and sorting

    @Query("select p from ProjDef p where lower(p.name) LIKE %?1% and p.projectId<>?2" )
    List<ProjDef> queryProjectsByNameQueryAndNotProjectId(String nameQuery, String notProjectId, Pageable pageable)


    @Query(value="""
                SELECT 
                    pd.project_id AS projectId,
                    pd.name AS name,
                    pd.total_points AS totalPoints,
                    COALESCE(errors.errorCount, 0) AS numErrors,
                    COALESCE(skills.skillCount, 0) AS numSkills,
                    COALESCE(badges.badgeCount, 0) AS numBadges,
                    COALESCE(subjects.subjectCount, 0) AS numSubjects,
                    COALESCE(groups.groupCount, 0) AS numGroups,
                    COALESCE(CAST(expiration.expiringUnused AS BOOLEAN), false) as expiring,
                    expiration.expirationTriggeredDate as expirationTriggered,
                    events.latest AS lastReportedSkill,
                    pd.created
                FROM project_definition pd 
                JOIN settings s on s.project_id = pd.project_id 
                LEFT JOIN (SELECT project_id, MAX(event_time) AS latest FROM user_events GROUP BY project_id) events ON events.project_id = pd.project_id 
                LEFT JOIN (SELECT project_id, COUNT(id) AS errorCount FROM project_error GROUP BY project_id) errors ON errors.project_id = pd.project_id 
                LEFT JOIN (SELECT project_id, COUNT(id) AS skillCount FROM skill_definition WHERE type = 'Skill' and enabled = 'true' GROUP BY project_id) skills ON skills.project_id = pd.project_id 
                LEFT JOIN (SELECT project_id, COUNT(id) AS badgeCount FROM skill_definition WHERE type = 'Badge' GROUP BY project_id) badges ON badges.project_id = pd.project_id 
                LEFT JOIN (SELECT project_id, COUNT(id) AS subjectCount FROM skill_definition WHERE type = 'Subject' GROUP BY project_id) subjects ON subjects.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS groupCount FROM skill_definition WHERE type = 'SkillsGroup' and enabled = 'true' GROUP BY project_id) groups ON groups.project_id = pd.project_id 
                LEFT JOIN (SELECT project_id, value AS expiringUnused, updated as expirationTriggeredDate FROM settings WHERE type = 'Project' AND setting = 'expiration.expiring.unused' AND value = 'true') expiration ON expiration.project_id = pd.project_id
                WHERE s.setting = 'production.mode.enabled' and s.value = 'true'
            """, nativeQuery = true)
    @Nullable
    List<ProjSummaryResult> getProjectSummariesInProduction()

    @Query("select p from ProjDef p, Setting s where p.projectId = s.projectId and s.setting = 'production.mode.enabled' and s.value = 'true' order by p.projectId")
    List<ProjDef> getProjectsInProduction()

    static interface AvailableProjectSummary {
        String getProjectId();
        String getName();
        int getTotalPoints();
        int getNumSubjects();
        int getNumSkills();
        int getNumGroups();
        int getNumBadges();
        Date getCreated();
        // set to project is if this project was added to 'My Projects'
        @Nullable
        String getMyProjectId();
        boolean getHasDescription();
    }
    @Query(value="""
                SELECT
                    pd.project_id AS projectId,
                    pd.name AS name,
                    pd.total_points AS totalPoints,
                    COALESCE(skills.skillCount, 0) AS numSkills,
                    COALESCE(badges.badgeCount, 0) AS numBadges,
                    COALESCE(subjects.subjectCount, 0) AS numSubjects,
                    COALESCE(groups.groupCount, 0) AS numGroups,
                    pd.created, 
                    theSettings.myProjectId AS myProjectId,
                    case when (pd.description is not null and pd.description != '') then true else false end as hasDescription
                FROM settings s, project_definition pd
                LEFT JOIN (SELECT project_id, MAX(event_time) AS latest FROM user_events GROUP BY project_id) events ON events.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS skillCount, MAX(updated) AS skillUpdated FROM skill_definition WHERE type = 'Skill' and enabled = 'true' GROUP BY project_id) skills ON skills.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS badgeCount, MAX(updated) AS badgeUpdated FROM skill_definition WHERE type = 'Badge' GROUP BY project_id) badges ON badges.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS subjectCount, MAX(updated) AS subjectUpdated FROM skill_definition WHERE type = 'Subject' GROUP BY project_id) subjects ON subjects.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS groupCount FROM skill_definition WHERE type = 'SkillsGroup' and enabled = 'true' GROUP BY project_id) groups ON groups.project_id = pd.project_id
                LEFT JOIN (SELECT ss.project_id as myProjectId, uu.id as userRefId FROM settings ss, users uu WHERE ss.setting = 'my_project' and uu.user_id=?1 and uu.id = ss.user_ref_id) theSettings ON theSettings.myProjectId = pd.project_id
                WHERE pd.project_id = s.project_id and (
                      (s.setting = 'keep_in_catalog' and s.value = 'true' and s.user_ref_id = theSettings.userRefId and not exists (select s2.setting from settings s2 where s2.setting = 'my_project' and s2.project_id = pd.project_id and s2.user_ref_id = s.user_ref_id)) or
                      (s.setting = 'my_project' and s.user_ref_id = theSettings.userRefId and not exists (select s2.setting from settings s2 where (s2.setting = 'production.mode.enabled' or s2.setting = 'invite_only') and s2.value = 'true' and s2.project_id = pd.project_id)) or
                      (s.setting = 'production.mode.enabled' and s.value = 'true') or
                      (s.setting = 'invite_only' and s.value = 'true' and exists 
                          (
                              select 1 from user_roles ur where ur.user_id = ?1 and ur.project_id = s.project_id and ur.role_name = 'ROLE_PRIVATE_PROJECT_USER'
                          )
                      )
                    )
                ORDER BY projectId
            """, nativeQuery = true)
    @Nullable
    List<AvailableProjectSummary> getAvailableProjectSummariesInProduction(String userId)

    @Query(value="""
                SELECT
                    pd.project_id AS projectId,
                    pd.name AS name,
                    pd.total_points AS totalPoints,
                    COALESCE(skills.skillCount, 0) AS numSkills,
                    COALESCE(badges.badgeCount, 0) AS numBadges,
                    COALESCE(subjects.subjectCount, 0) AS numSubjects,
                    COALESCE(groups.groupCount, 0) AS numGroups,
                    pd.created, 
                    theSettings.myProjectId AS myProjectId,
                    case when (pd.description is not null and pd.description != '') then true else false end as hasDescription
                FROM settings s, project_definition pd
                LEFT JOIN (SELECT project_id, MAX(event_time) AS latest FROM user_events GROUP BY project_id) events ON events.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS skillCount, MAX(updated) AS skillUpdated FROM skill_definition WHERE type = 'Skill' and enabled = 'true' GROUP BY project_id) skills ON skills.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS badgeCount, MAX(updated) AS badgeUpdated FROM skill_definition WHERE type = 'Badge' GROUP BY project_id) badges ON badges.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS subjectCount, MAX(updated) AS subjectUpdated FROM skill_definition WHERE type = 'Subject' GROUP BY project_id) subjects ON subjects.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS groupCount FROM skill_definition WHERE type = 'SkillsGroup' and enabled = 'true' GROUP BY project_id) groups ON groups.project_id = pd.project_id
                LEFT JOIN (SELECT ss.project_id as myProjectId, uu.id as userId FROM settings ss, users uu WHERE ss.setting = 'my_project' and uu.user_id=?1 and uu.id = ss.user_ref_id) theSettings ON theSettings.myProjectId = pd.project_id
                WHERE pd.project_id = s.project_id and
                      (
                          (s.setting = 'keep_in_catalog' and s.value = 'true' and theSettings.userId = s.user_ref_id and not exists (select s2.setting from settings s2 where s2.setting = 'my_project' and s2.project_id = pd.project_id and s.user_ref_id = s2.user_ref_id)) or 
                          (s.setting = 'my_project' and theSettings.userId = s.user_ref_id and not exists (select s2.setting from settings s2 where (s2.setting = 'production.mode.enabled' or s2.setting = 'invite_only') and s2.value = 'true' and s2.project_id = pd.project_id)) or 
                          (s.setting = 'production.mode.enabled' and s.value = 'true') or
                          (s.setting = 'invite_only' and s.value = 'true' and exists 
                              (
                                  select 1 from user_roles ur where ur.user_id = ?1 and ur.project_id = s.project_id and ur.role_name = 'ROLE_PRIVATE_PROJECT_USER'
                              )
                          )
                      )
                      and
                      (
                          (not exists (select 1 from settings s2 where pd.project_id = s2.project_id and s2.setting = 'user_community' and s2.value = 'true')) or 
                          (exists (select 1 from settings s2 where pd.project_id = s2.project_id and s2.setting = 'user_community' and s2.value = 'true') and 
                           exists (select 1 from user_tags ut where ut.user_id = ?1 and ut.key = ?2 and ut.value = ?3)
                          ) 
                      )
                ORDER BY projectId
            """, nativeQuery = true)
    @Nullable
    List<AvailableProjectSummary> getAvailableProjectSummariesInProduction(String userId, String userCommunityUserTagKey, String userCommunityUserTagValue)

    @Query(value="""
            SELECT pd.project_id as projectId,
                   GREATEST(
                       MAX(pd.updated), 
                       MAX(sd.updated),
                       MAX(sd.relationshipUpdated),
                       MAX(ue.event_time), 
                       MAX(pe.last_seen) 
                    ) as lastTouched
            FROM project_definition pd
            LEFT JOIN (SELECT project_id, MAX(event_time) AS event_time FROM user_events GROUP BY project_id) ue ON pd.project_id = ue.project_id
            LEFT JOIN (
                 SELECT s.project_id, MAX(s.updated) AS updated, MAX(srd.updated) as relationshipUpdated FROM skill_definition s
                 LEFT JOIN skill_relationship_definition srd ON s.id = srd.child_ref_id OR s.id= srd.parent_ref_id
                 GROUP BY s.project_id
            ) sd ON pd.project_id = sd.project_id
            LEFT JOIN (SELECT project_id, MAX(last_seen) AS last_seen from project_error GROUP BY project_id) pe ON pd.project_id = pe.project_id
            GROUP BY pd.project_id
            HAVING GREATEST(MAX(pd.updated), MAX(sd.updated), MAX(sd.relationshipUpdated), MAX(ue.event_time), MAX(pe.last_seen)) < ?1
    """, nativeQuery = true)
    public List<ProjectLastTouched> findProjectsNotTouchedSince(Date lastTouched)

    @Query("select p from ProjDef p, Setting s where p.projectId = s.projectId and s.setting = 'expiration.expiring.unused' and s.value = 'true' and s.updated <= ?1 order by p.projectId")
    List<ProjDef> getExpiringProjects(Date cutoff)

    @Query("select p from ProjDef p, Setting s where p.projectId = s.projectId and s.setting = 'expiration.expiring.unused' and s.value = 'true' and s.updated > ?1 order by p.projectId")
    List<ProjDef> getProjectsWithinGracePeriod(Date cutoff)

    @Query('''
            SELECT pd.id as projectRefId,
                   pd.projectId as projectId,
                   pd.name as projectName,
                   COALESCE(up.points, 0) as points,
                   (SELECT COALESCE(count(*), 1) FROM UserPoints WHERE projectId = pd.projectId and skillId is NULL) as totalUsers,
                   (SELECT COALESCE(count(*)+1, 1) FROM UserPoints WHERE projectId = pd.projectId and skillId is NULL and points > up.points) as rank,
                   COALESCE((SELECT value FROM Setting WHERE projectId = pd.projectId AND setting = 'user_community' and value = 'true'), 'false') as protectedCommunityEnabled,
                   COALESCE((SELECT value FROM Setting WHERE projectId = pd.projectId AND setting = 'invite_only' and value = 'true'), 'false') as inviteOnlyEnabled,
                   pd.totalPoints as totalPoints,
                   s.value as orderVal
            FROM Setting s, Users uu, ProjDef pd
            LEFT JOIN UserPoints up on pd.projectId = up.projectId and up.userId=?1 and up.skillId is null
            WHERE (s.setting = 'my_project' and uu.userId=?1 and uu.id = s.userRefId and s.projectId = pd.projectId)
            GROUP BY up.points, pd.projectId, pd.name, pd.id, s.value
    ''')
    List<ProjectSummaryResult> getProjectSummaries(String userId)

    @Query('''
            SELECT pd.id as projectRefId,
                   pd.projectId as projectId,
                   pd.name as projectName
            FROM Setting s, Setting ss, Users uu, ProjDef pd
            WHERE (s.setting = 'production.mode.enabled' and s.projectId = pd.projectId and s.value = 'true') and 
                (ss.setting = 'my_project' and uu.userId=?1 and uu.id = ss.userRefId and ss.projectId = pd.projectId) and
                pd.projectId = ?2
    ''')
    ProjectSummaryResult getMyProjectName(String userId, String projectId)

    @Nullable
    @Query('''
            SELECT pd.id as projectRefId,
                   pd.projectId as projectId,
                   pd.name as projectName
            FROM ProjDef pd
            WHERE pd.projectId = ?1
    ''')
    ProjectSummaryResult getProjectName(String projectId)

    @Query(value='''
        select max(pd.project_id) as projectId, max(pd.name) as name, (coalesce (sum(sd.total_points),0)+max(pd.total_points)) as totalIncPendingFinalized 
        from project_definition pd
        left join skill_definition sd on sd.project_id = pd.project_id and
        sd.type = 'Skill' and 
        sd.enabled = 'false' and 
        sd.copied_from_project_id is not null
        where
        pd.project_id = :projectId
        group by sd.project_id''', nativeQuery = true)
    ProjectTotalPoints getProjectTotalPointsIncPendingFinalization(@Param("projectId") String projectId)

    @Nullable
    @Query('''SELECT pd.totalPoints FROM ProjDef pd WHERE pd.projectId = ?1''')
    Integer getTotalPointsByProjectId(String projectId)

    @Query(value = '''select count(id) > 0
            from project_definition
            where
                convert_from(lo_get(CAST(description as oid)), 'UTF8') like CONCAT('%(/api/download/', ?2, ')%')
              and LOWER(project_id) <> LOWER(?1)''', nativeQuery = true)
    Boolean otherProjectExistWithAttachmentUUID(String notThisProject, String attachmentUUID)

}
