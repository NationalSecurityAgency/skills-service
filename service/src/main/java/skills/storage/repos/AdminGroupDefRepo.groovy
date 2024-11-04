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

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.lang.Nullable
import skills.storage.model.AdminGroupDef

interface AdminGroupDefRepo extends CrudRepository<AdminGroupDef, Long> {

    @Nullable
    AdminGroupDef findByAdminGroupIdIgnoreCase(String adminGroupId)

    @Nullable
    @Query(value = "select agd.id from AdminGroupDef agd where lower(agd.adminGroupId) = lower(?1)")
    Integer getAdminGroupRefIdByAdminGroupIdIgnoreCase(String adminGroupId)

    @Modifying
    int deleteByAdminGroupIdIgnoreCase(String adminGroupId)

    @Nullable
    AdminGroupDef findByNameIgnoreCase(String adminGroupName)

    @Query(value = "select count(agd.id) from AdminGroupDef agd, UserRole u where agd.adminGroupId = u.adminGroupId and u.userId=?1")
    Integer getAdminGroupCountByUserId(String userId)

    Boolean existsByAdminGroupIdIgnoreCase(String adminGroupId)
    Boolean existsByNameIgnoreCase(String adminGroupName)

    @Nullable
    @Query('''select 'true' from AdminGroupDef agd JOIN UserRole ur on agd.adminGroupId = ur.adminGroupId where ur.projectId = ?1 AND agd.protectedCommunityEnabled = false''')
    Boolean doesAdminGroupContainNonUserCommunityProject(String adminGroupId)


    @Query(value="""
                SELECT 
                    agd.admin_group_id AS adminGroupId,
                    agd.name AS name,
                    agd.created,
                    agd.protected_community_enabled as protectedCommunityEnabled,
                    COALESCE(numberOfOwners, 0) as numberOfOwners,
                    COALESCE(numberOfMembers, 0) as numberOfMembers,
                    COALESCE(numberOfProjects, 0) as numberOfProjects,
                    COALESCE(numberOfQuizzesAndSurveys, 0) as numberOfQuizzesAndSurveys
                    
                FROM admin_group_definition agd
                JOIN user_roles ur on (ur.admin_group_id = agd.admin_group_id AND ur.role_name in ('ROLE_ADMIN_GROUP_OWNER'))
                LEFT JOIN (SELECT admin_group_id, COUNT(distinct user_id) AS numberOfOwners FROM user_roles WHERE role_name = 'ROLE_ADMIN_GROUP_OWNER' group by admin_group_id ) adminGroupOwners ON adminGroupOwners.admin_group_id = agd.admin_group_id
                LEFT JOIN (SELECT admin_group_id, COUNT(distinct user_id) AS numberOfMembers FROM user_roles WHERE role_name = 'ROLE_ADMIN_GROUP_MEMBER' group by admin_group_id ) adminGroupMembers ON adminGroupMembers.admin_group_id = agd.admin_group_id
                LEFT JOIN (SELECT admin_group_id, COUNT(distinct project_id) AS numberOfProjects FROM user_roles WHERE role_name = 'ROLE_PROJECT_ADMIN' group by admin_group_id ) projectAdminRoles ON projectAdminRoles.admin_group_id = agd.admin_group_id
                LEFT JOIN (SELECT admin_group_id, COUNT(distinct quiz_id) AS numberOfQuizzesAndSurveys FROM user_roles WHERE role_name = 'ROLE_QUIZ_ADMIN' group by admin_group_id ) quizAdminRoles ON quizAdminRoles.admin_group_id = agd.admin_group_id
                WHERE ur.user_id = ?1
            """, nativeQuery = true)
    @Nullable
    List<AdminGroupDefSummaryRes> getAdminGroupDefSummariesByUser(String userId)

    static interface AdminGroupDefSummaryRes {
        String getAdminGroupId();
        String getName();
        Boolean getProtectedCommunityEnabled();
        Date getCreated();
        Integer getNumberOfOwners()
        Integer getNumberOfMembers()
        Integer getNumberOfProjects()
        Integer getNumberOfQuizzesAndSurveys()
    }
    @Query(value="""
                select 
                    agd.admin_group_id AS adminGroupId,
                    agd.name AS name,
                    agd.created as created,
                    agd.protected_community_enabled as protectedCommunityEnabled,
                    COALESCE(numberOfOwners, 0) as numberOfOwners,
                    COALESCE(numberOfMembers, 0) as numberOfMembers,
                    COALESCE(numberOfProjects, 0) as numberOfProjects,
                    COALESCE(numberOfQuizzesAndSurveys, 0) as numberOfQuizzesAndSurveys
                FROM admin_group_definition agd
                LEFT JOIN (SELECT admin_group_id, COUNT(distinct user_id) AS numberOfOwners FROM user_roles WHERE role_name = 'ROLE_ADMIN_GROUP_OWNER' group by admin_group_id ) adminGroupOwners ON adminGroupOwners.admin_group_id = agd.admin_group_id
                LEFT JOIN (SELECT admin_group_id, COUNT(distinct user_id) AS numberOfMembers FROM user_roles WHERE role_name = 'ROLE_ADMIN_GROUP_MEMBER' group by admin_group_id ) adminGroupMembers ON adminGroupMembers.admin_group_id = agd.admin_group_id
                LEFT JOIN (SELECT admin_group_id, COUNT(distinct project_id) AS numberOfProjects FROM user_roles WHERE role_name = 'ROLE_PROJECT_ADMIN' group by admin_group_id ) projectAdminRoles ON projectAdminRoles.admin_group_id = agd.admin_group_id
                LEFT JOIN (SELECT admin_group_id, COUNT(distinct quiz_id) AS numberOfQuizzesAndSurveys FROM user_roles WHERE role_name = 'ROLE_QUIZ_ADMIN' group by admin_group_id ) quizAdminRoles ON quizAdminRoles.admin_group_id = agd.admin_group_id
                WHERE agd.admin_group_id = ?1
            """, nativeQuery = true)
    AdminGroupDefSummaryRes getAdminGroupDefSummary(String adminGroupId)
}
