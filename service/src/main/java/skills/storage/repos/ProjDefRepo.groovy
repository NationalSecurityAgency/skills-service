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
import org.springframework.lang.Nullable
import skills.storage.model.ProjDef
import skills.storage.model.ProjSummaryResult
import skills.storage.model.ProjectLastTouched

interface ProjDefRepo extends CrudRepository<ProjDef, Long> {

    @Query(value="""
                SELECT 
                    pd.project_id AS projectId,
                    pd.name AS name,
                    pd.total_points AS totalPoints,
                    COALESCE(errors.errorCount, 0) AS numErrors,
                    COALESCE(skills.skillCount, 0) AS numSkills,
                    COALESCE(badges.badgeCount, 0) AS numBadges,
                    COALESCE(subjects.subjectCount, 0) AS numSubjects,
                    COALESCE(expiration.expiringUnused, 'false') as expiring,
                    expiration.expirationTriggeredDate as expirationTriggered,
                    events.latest AS lastReportedSkill,
                    pd.created,
                    GREATEST(skills.skillUpdated, badges.badgeUpdated, subjects.subjectUpdated, pd.updated) as lastEdited
                FROM project_definition pd
                LEFT JOIN (SELECT project_id, MAX(event_time) AS latest FROM user_events GROUP BY project_id) events ON events.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS errorCount FROM project_error GROUP BY project_id) errors ON errors.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS skillCount, MAX(updated) as skillUpdated FROM skill_definition WHERE type = 'Skill' GROUP BY project_id) skills ON skills.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS badgeCount, MAX(updated) as badgeUpdated FROM skill_definition WHERE type = 'Badge' GROUP BY project_id) badges ON badges.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS subjectCount, MAX(updated) as subjectUpdated FROM skill_definition WHERE type = 'Subject' GROUP BY project_id) subjects ON subjects.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, value AS expiringUnused, updated as expirationTriggeredDate FROM settings WHERE type = 'Project' AND setting = 'expiration.expiring.unused' AND value = 'true') expiration ON expiration.project_id = pd.project_id
                WHERE LOWER(pd.project_id) = LOWER(?1) 
            """, nativeQuery = true)
    @Nullable
    ProjSummaryResult getSummaryByProjectIdIgnoreCase(String projectId)

    @Nullable
    ProjDef findByProjectIdIgnoreCase(String projectId)

    @Query(value="""
                SELECT 
                    pd.project_id AS projectId,
                    pd.name AS name,
                    pd.total_points AS totalPoints,
                    COALESCE(errors.errorCount, 0) AS numErrors,
                    COALESCE(skills.skillCount, 0) AS numSkills,
                    COALESCE(badges.badgeCount, 0) AS numBadges,
                    COALESCE(subjects.subjectCount, 0) AS numSubjects,
                    COALESCE(expiration.expiringUnused, 'false') as expiring,
                    expiration.expirationTriggeredDate as expirationTriggered,
                    events.latest AS lastReportedSkill,
                    pd.created,
                    GREATEST(skills.skillUpdated, badges.badgeUpdated, subjects.subjectUpdated, pd.updated) as lastEdited
                FROM project_definition pd
                LEFT JOIN (SELECT project_id, MAX(event_time) AS latest FROM user_events GROUP BY project_id) events ON events.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS errorCount FROM project_error GROUP BY project_id) errors ON errors.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS skillCount, MAX(updated) AS skillUpdated FROM skill_definition WHERE type = 'Skill' GROUP BY project_id) skills ON skills.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS badgeCount, MAX(updated) AS badgeUpdated FROM skill_definition WHERE type = 'Badge' GROUP BY project_id) badges ON badges.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS subjectCount, MAX(updated) AS subjectUpdated FROM skill_definition WHERE type = 'Subject' GROUP BY project_id) subjects ON subjects.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, value AS expiringUnused, updated as expirationTriggeredDate FROM settings WHERE type = 'Project' AND setting = 'expiration.expiring.unused' AND value = 'true') expiration ON expiration.project_id = pd.project_id
                WHERE pd.project_id in ?1 
            """, nativeQuery = true)
    @Nullable
    List<ProjSummaryResult> getAllSummariesByProjectIdIn(List<String> projectIds)

    @Nullable
    List<ProjDef> findAllByProjectIdIn(List<String> projectIds)

    @Query(value="""
                SELECT 
                    pd.project_id AS projectId,
                    pd.name AS name,
                    pd.total_points AS totalPoints,
                    COALESCE(errors.errorCount, 0) AS numErrors,
                    COALESCE(skills.skillCount, 0) AS numSkills,
                    COALESCE(badges.badgeCount, 0) AS numBadges,
                    COALESCE(subjects.subjectCount, 0) AS numSubjects,
                    COALESCE(expiration.expiringUnused, 'false') as expiring,
                    expiration.expirationTriggeredDate as expirationTriggered,
                    events.latest AS lastReportedSkill,
                    pd.created,
                    GREATEST(skills.skillUpdated, badges.badgeUpdated, subjects.subjectUpdated, pd.updated) as lastEdited
                FROM project_definition pd
                LEFT JOIN (SELECT project_id, MAX(event_time) AS latest FROM user_events GROUP BY project_id) events ON events.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS errorCount FROM project_error GROUP BY project_id) errors ON errors.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS skillCount, MAX(updated) AS skillUpdated FROM skill_definition WHERE type = 'Skill' GROUP BY project_id) skills ON skills.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS badgeCount, MAX(updated) AS badgeUpdated FROM skill_definition WHERE type = 'Badge' GROUP BY project_id) badges ON badges.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS subjectCount, MAX(updated) AS subjectUpdated FROM skill_definition WHERE type = 'Subject' GROUP BY project_id) subjects ON subjects.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, value AS expiringUnused, updated as expirationTriggeredDate FROM settings WHERE type = 'Project' AND setting = 'expiration.expiring.unused' AND value = 'true') expiration ON expiration.project_id = pd.project_id
            """, nativeQuery = true)
    @Nullable
    List<ProjSummaryResult> getAllSummaries()

    List<ProjDef> findAll()

    void deleteByProjectIdIgnoreCase(String projectId)

    @Query(value="""
                SELECT 
                    pd.project_id AS projectId,
                    pd.name AS name,
                    pd.total_points AS totalPoints,
                    COALESCE(errors.errorCount, 0) AS numErrors,
                    COALESCE(skills.skillCount, 0) AS numSkills,
                    COALESCE(badges.badgeCount, 0) AS numBadges,
                    COALESCE(subjects.subjectCount, 0) AS numSubjects,
                    COALESCE(expiration.expiringUnused, 'false') as expiring,
                    expiration.expirationTriggeredDate as expirationTriggered,
                    events.latest AS lastReportedSkill,
                    pd.created,
                    GREATEST(skills.skillUpdated, badges.badgeUpdated, subjects.subjectUpdated, pd.updated) as lastEdited
                FROM project_definition pd
                LEFT JOIN (SELECT project_id, MAX(event_time) AS latest FROM user_events GROUP BY project_id) events ON events.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS errorCount FROM project_error GROUP BY project_id) errors ON errors.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS skillCount, MAX(updated) as skillUpdated FROM skill_definition WHERE type = 'Skill' GROUP BY project_id) skills ON skills.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS badgeCount, MAX(updated) as badgeUpdated FROM skill_definition WHERE type = 'Badge' GROUP BY project_id) badges ON badges.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS subjectCount, MAX(updated) as subjectUpdated FROM skill_definition WHERE type = 'Subject' GROUP BY project_id) subjects ON subjects.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, value AS expiringUnused, updated as expirationTriggeredDate FROM settings WHERE type = 'Project' AND setting = 'expiration.expiring.unused' AND value = 'true') expiration ON expiration.project_id = pd.project_id
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
                    COALESCE(expiration.expiringUnused, 'false') as expiring,
                    expiration.expirationTriggeredDate as expirationTriggered,
                    events.latest AS lastReportedSkill,
                    pd.created
                FROM project_definition pd
                LEFT JOIN (SELECT project_id, MAX(event_time) AS latest FROM user_events GROUP BY project_id) events ON events.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS errorCount FROM project_error GROUP BY project_id) errors ON errors.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS skillCount FROM skill_definition WHERE type = 'Skill' GROUP BY project_id) skills ON skills.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS badgeCount FROM skill_definition WHERE type = 'Badge' GROUP BY project_id) badges ON badges.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS subjectCount FROM skill_definition WHERE type = 'Subject' GROUP BY project_id) subjects ON subjects.project_id = pd.project_id
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
                    COALESCE(badges.badgeCount, 0) AS numBadges,
                    COALESCE(subjects.subjectCount, 0) AS numSubjects,
                    COALESCE(expiration.expiringUnused, 'false') as expiring,
                    expiration.expirationTriggeredDate as expirationTriggered,
                    events.latest AS lastReportedSkill,
                    pd.created
                FROM project_definition pd
                LEFT JOIN (SELECT project_id, MAX(event_time) AS latest FROM user_events GROUP BY project_id) events ON events.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS errorCount FROM project_error GROUP BY project_id) errors ON errors.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS skillCount FROM skill_definition WHERE type = 'Skill' GROUP BY project_id) skills ON skills.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS badgeCount FROM skill_definition WHERE type = 'Badge' GROUP BY project_id) badges ON badges.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS subjectCount FROM skill_definition WHERE type = 'Subject' GROUP BY project_id) subjects ON subjects.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, value AS expiringUnused, updated as expirationTriggeredDate FROM settings WHERE type = 'Project' AND setting = 'expiration.expiring.unused' AND value = 'true') expiration ON expiration.project_id = pd.project_id
                JOIN user_roles ur on ur.project_id = pd.project_id
                WHERE ur.user_id = ?1
            """, nativeQuery = true)
    @Nullable
    List<ProjSummaryResult> getProjectSummariesByUser(String userId)

    @Query(value = "select p from ProjDef p, UserRole u where p.projectId = u.projectId and u.userId=?1")
    List<ProjDef> getProjectsByUser(String userId)

    @Query(value="""
                SELECT 
                    pd.project_id AS projectId,
                    pd.name AS name,
                    pd.total_points AS totalPoints,
                    COALESCE(errors.errorCount, 0) AS numErrors,
                    COALESCE(skills.skillCount, 0) AS numSkills,
                    COALESCE(badges.badgeCount, 0) AS numBadges,
                    COALESCE(subjects.subjectCount, 0) AS numSubjects,
                    COALESCE(expiration.expiringUnused, 'false') as expiring,
                    expiration.expirationTriggeredDate as expirationTriggered,
                    events.latest AS lastReportedSkill,
                    pd.created
                FROM project_definition pd
                LEFT JOIN (SELECT project_id, MAX(event_time) AS latest FROM user_events GROUP BY project_id) events ON events.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS errorCount FROM project_error GROUP BY project_id) errors ON errors.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS skillCount FROM skill_definition WHERE type = 'Skill' GROUP BY project_id) skills ON skills.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS badgeCount FROM skill_definition WHERE type = 'Badge' GROUP BY project_id) badges ON badges.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS subjectCount FROM skill_definition WHERE type = 'Subject' GROUP BY project_id) subjects ON subjects.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, value AS expiringUnused, updated as expirationTriggeredDate FROM settings WHERE type = 'Project' AND setting = 'expiration.expiring.unused' AND value = 'true') expiration ON expiration.project_id = pd.project_id
                WHERE LOWER(pd.name) LIKE LOWER(CONCAT('%',?1,'%'))
            """, nativeQuery = true)
    @Nullable
    List<ProjSummaryResult> getSummariesByNameLike(String search)

    @Query("select p from ProjDef p where upper(p.name) like UPPER(CONCAT('%', ?1, '%'))")
    List<ProjDef> findByNameLike(String search)

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
                    COALESCE(expiration.expiringUnused, 'false') as expiring,
                    expiration.expirationTriggeredDate as expirationTriggered,
                    events.latest AS lastReportedSkill,
                    pd.created
                FROM project_definition pd
                LEFT JOIN (SELECT project_id, MAX(event_time) AS latest FROM user_events GROUP BY project_id) events ON events.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS errorCount FROM project_error GROUP BY project_id) errors ON errors.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS skillCount FROM skill_definition WHERE type = 'Skill' GROUP BY project_id) skills ON skills.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS badgeCount FROM skill_definition WHERE type = 'Badge' GROUP BY project_id) badges ON badges.project_id = pd.project_id
                LEFT JOIN (SELECT project_id, COUNT(id) AS subjectCount FROM skill_definition WHERE type = 'Subject' GROUP BY project_id) subjects ON subjects.project_id = pd.project_id
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
                    COALESCE(expiration.expiringUnused, 'false') as expiring,
                    expiration.expirationTriggeredDate as expirationTriggered,
                    events.latest AS lastReportedSkill,
                    pd.created 
                FROM project_definition pd 
                JOIN settings s on s.project_id = pd.project_id 
                LEFT JOIN (SELECT project_id, MAX(event_time) AS latest FROM user_events GROUP BY project_id) events ON events.project_id = pd.project_id 
                LEFT JOIN (SELECT project_id, COUNT(id) AS errorCount FROM project_error GROUP BY project_id) errors ON errors.project_id = pd.project_id 
                LEFT JOIN (SELECT project_id, COUNT(id) AS skillCount FROM skill_definition WHERE type = 'Skill' GROUP BY project_id) skills ON skills.project_id = pd.project_id 
                LEFT JOIN (SELECT project_id, COUNT(id) AS badgeCount FROM skill_definition WHERE type = 'Badge' GROUP BY project_id) badges ON badges.project_id = pd.project_id 
                LEFT JOIN (SELECT project_id, COUNT(id) AS subjectCount FROM skill_definition WHERE type = 'Subject' GROUP BY project_id) subjects ON subjects.project_id = pd.project_id 
                LEFT JOIN (SELECT project_id, value AS expiringUnused, updated as expirationTriggeredDate FROM settings WHERE type = 'Project' AND setting = 'expiration.expiring.unused' AND value = 'true') expiration ON expiration.project_id = pd.project_id
                WHERE s.setting = 'production.mode.enabled' and s.value = 'true'
            """, nativeQuery = true)
    @Nullable
    List<ProjSummaryResult> getProjectSummariesInProduction()

    @Query("select p from ProjDef p, Setting s where p.projectId = s.projectId and s.setting = 'production.mode.enabled' and s.value = 'true' order by p.projectId")
    List<ProjDef> getProjectsInProduction()

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

}
