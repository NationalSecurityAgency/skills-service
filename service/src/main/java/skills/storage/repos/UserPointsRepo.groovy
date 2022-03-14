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

import groovy.transform.CompileStatic
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.lang.Nullable
import skills.controller.result.model.ProjectUser
import skills.storage.model.SkillRelDef
import skills.storage.model.DayCountItem
import skills.storage.model.UserPoints

import java.time.LocalDateTime

@CompileStatic
interface UserPointsRepo extends CrudRepository<UserPoints, Integer> {

    @Nullable
    UserPoints findByProjectIdAndUserIdAndSkillIdAndDay(String projectId, String userId, @Nullable String skillId, @Nullable Date day)

    @Modifying
    @Query(value = '''INSERT INTO user_points(user_id, project_id, skill_id, skill_ref_id, points, day)
            SELECT up.user_id, toDef.project_id, toDef.skill_id, toDef.id, up.points, up.day
            FROM user_points up, skill_definition toDef
            WHERE
                  toDef.project_id = :toProjectId and 
                  toDef.skill_id = up.skill_id and
                  up.skill_ref_id in (:fromSkillRefIds)
            ''', nativeQuery = true)
    void copyUserPointsToTheImportedProjects(@Param('toProjectId') String toProjectId, @Param('fromSkillRefIds') List<Integer> fromSkillRefIds)

    @Modifying
    @Query(value = '''INSERT INTO user_points(user_id, day, points, project_id)
            SELECT up.user_id, up.day, sum(points) as points, max(up.project_id) as project_id
            FROM user_points up, skill_definition sd
            WHERE
                    up.project_id = :toProjectId and
                    sd.project_id = :toProjectId and sd.id = up.skill_ref_id and sd.type = 'Skill'
            and not exists (select 1 from user_points innerUP where up.project_id = innerUP.project_id and up.user_id = innerUP.user_id and innerUP.skill_id is null and (up.day=innerUP.day or (up.day is null and innerUP.day is null)))
            group by up.user_id, up.day;
            ''', nativeQuery = true)
    void createProjectUserPointsForTheNewUsers(@Param('toProjectId') String toProjectId)


//    @Modifying
//    @Query(value = '''UPDATE user_points
//            SET points = innerUp.points
//            from
//                (SELECT up.user_id, up.day, sum(points) as points, max(up.project_id) as project_id
//                FROM user_points up, skill_definition sd
//                WHERE
//                    up.project_id = :toProjectId and
//                    sd.project_id = :toProjectId and sd.id = up.skill_ref_id and sd.type = 'Skill'
//                group by up.user_id, up.day) innerUp
//             where (user_points.project_id = innerUP.project_id and user_points.user_id = innerUP.user_id and user_points.skill_id is null and (user_points.day=innerUP.day or (user_points.day is null and innerUP.day is null)))
//            ''', nativeQuery = true)
//    void updateProjectUserPointsForAllUsers(@Param('toProjectId') String toProjectId)


    @Modifying
    @Query(value = '''INSERT INTO user_points(user_id, day, points, project_id, skill_id, skill_ref_id)
        SELECT up.user_id, up.day, sum(points) as points, max(up.project_id) as project_id, max(subject.skill_id) as skill_id, max(subject.id) as skill_ref_id
        FROM user_points up, skill_definition subject, skill_relationship_definition srd, skill_definition sd
        WHERE
          up.project_id = :toProjectId
          and subject.project_id = :toProjectId and subject.skill_id = :toSubjectId
          and subject.id = srd.parent_ref_id and sd.id = srd.child_ref_id and srd.type = 'RuleSetDefinition'
          and sd.id = up.skill_ref_id
          and not exists (
            select 1 from user_points innerUP
            where
              up.project_id = innerUP.project_id
              and innerUP.skill_id = :toSubjectId
              and up.user_id = innerUP.user_id
              and (up.day=innerUP.day or (up.day is null and innerUP.day is null))
          )
        group by up.user_id, up.day;
            ''', nativeQuery = true)
    void createSubjectUserPointsForTheNewUsers(@Param('toProjectId') String toProjectId, @Param('toSubjectId') String toSubjectId)

//    @Modifying
//    @Query(value = '''UPDATE user_points
//        SET points = innerUp.points
//        from
//            (SELECT up.user_id, up.day, sum(points) as points, max(up.project_id) as project_id, max(subject.skill_id) as skill_id, max(subject.id) as skill_ref_id
//             FROM user_points up, skill_definition subject, skill_relationship_definition srd, skill_definition sd
//             WHERE
//               up.project_id = :toProjectId
//               and subject.project_id = :toProjectId and subject.skill_id =  :toSubjectId
//               and subject.id = srd.parent_ref_id and sd.id = srd.child_ref_id and srd.type = 'RuleSetDefinition'
//               and sd.id = up.skill_ref_id
//             group by up.user_id, up.day) innerUp
//        where user_points.project_id = innerUP.project_id
//          and user_points.skill_id =  :toSubjectId
//          and user_points.user_id = innerUP.user_id
//          and (user_points.day=innerUP.day or (user_points.day is null and innerUP.day is null))
//            ''', nativeQuery = true)
//    void updateSubjectUserPointsForAllUsers(@Param('toProjectId') String toProjectId, @Param('toSubjectId') String toSubjectId)

    @Nullable
    @Query('''SELECT p.points as points from UserPoints p where p.projectId=?1 and p.userId=?2 and p.day is null and p.skillId is null''')
    Integer findPointsByProjectIdAndUserId(String projectId, String userId)

    @Nullable
    @Query('''SELECT p.points as points from UserPoints p where p.projectId=?1 and p.userId=?2 and p.skillId=?3 and p.day is null''')
    Integer findPointsByProjectIdAndUserIdAndSkillId(String projectId, String userId, String skillId)


    static interface RankedUserRes {
        String getUserId()
        String getUserIdForDisplay()
        String getUserFirstName()
        String getUserLastName()
        Integer getPoints()
        LocalDateTime getUserFirstSeenTimestamp()
    }


    @Query('''SELECT 
                    p.userId as userId, 
                    p.points as points,
                    uAttrs.userIdForDisplay as userIdForDisplay,
                    uAttrs.firstName as userFirstName,
                    uAttrs.lastName as userLastName,
                    uAttrs.created as userFirstSeenTimestamp
                from UserPoints p, UserAttrs uAttrs
                where
                    p.userId = uAttrs.userId and
                    p.projectId=?1 and 
                    p.skillId is null and 
                    p.day is null and
                    p.userId not in ?2 and
                    p.userId not in 
                        (select u.userId from Setting s, User u where 
                            s.userRefId=u.id and 
                            s.settingGroup='user.prefs' and
                            s.setting='rank_and_leaderboard_optOut' and
                            s.value='true' and 
                            s.projectId is null)
            ''')
    List<RankedUserRes> findUsersForLeaderboard(String projectId, List<String> excludeUserIds, Pageable pageable)

    @Query('''SELECT 
                    p.userId as userId, 
                    p.points as points,
                    uAttrs.userIdForDisplay as userIdForDisplay,
                    uAttrs.firstName as userFirstName,
                    uAttrs.lastName as userLastName,
                    uAttrs.created as userFirstSeenTimestamp
                from UserPoints p, UserAttrs uAttrs
                where
                    p.userId = uAttrs.userId and
                    p.projectId=?1 and 
                    p.skillId=?2 and 
                    p.day is null and 
                    p.userId not in ?3 and
                    p.userId not in 
                        (select u.userId from Setting s, User u where 
                            s.userRefId=u.id and 
                            s.settingGroup='user.prefs' and
                            s.setting='rank_and_leaderboard_optOut' and
                            s.value='true' and 
                            s.projectId is null)
            ''')
    List<RankedUserRes> findUsersForLeaderboard(String projectId, String subjectId, List<String> excludeUserIds, Pageable pageable)

    @Query('''SELECT 
                    p.userId as userId, 
                    p.points as points,
                    uAttrs.userIdForDisplay as userIdForDisplay,
                    uAttrs.firstName as userFirstName,
                    uAttrs.lastName as userLastName,
                    uAttrs.created as userFirstSeenTimestamp
                from UserPoints p, UserAttrs uAttrs
                where
                    p.userId = uAttrs.userId and
                    p.projectId=?1 and 
                    p.skillId=?2 and
                    (p.points<?3 OR (p.points=?3 and uAttrs.created>?4)) and
                    p.day is null
            ''')
    List<RankedUserRes> findUsersForLeaderboardPointsLessOrEqual(String projectId, String subjectId, Integer points, LocalDateTime usrCreated, Pageable pageable)

    @Query('''SELECT 
                    p.userId as userId, 
                    p.points as points,
                    uAttrs.userIdForDisplay as userIdForDisplay,
                    uAttrs.firstName as userFirstName,
                    uAttrs.lastName as userLastName,
                    uAttrs.created as userFirstSeenTimestamp
                from UserPoints p, UserAttrs uAttrs
                where
                    p.userId = uAttrs.userId and
                    p.projectId=?1 and 
                    p.skillId is null and
                    p.points<=?2 and
                    (p.points<?2 OR (p.points=?2 and uAttrs.created>?3)) and
                    p.day is null
            ''')
    List<RankedUserRes> findUsersForLeaderboardPointsLessOrEqual(String projectId, Integer points, LocalDateTime usrCreated, Pageable pageable)

    @Query('''SELECT 
                    p.userId as userId, 
                    p.points as points,
                    uAttrs.userIdForDisplay as userIdForDisplay,
                    uAttrs.firstName as userFirstName,
                    uAttrs.lastName as userLastName,
                    uAttrs.created as userFirstSeenTimestamp
                from UserPoints p, UserAttrs uAttrs
                where
                    p.userId = uAttrs.userId and
                    p.projectId=?1 and 
                    p.skillId=?2 and
                    (p.points>?3 OR (p.points=?3 and uAttrs.created<?4)) and
                    p.day is null
            ''')
    List<RankedUserRes> findUsersForLeaderboardPointsMoreOrEqual(String projectId, String subjectId, Integer points, LocalDateTime userCreateDate, Pageable pageable)

    @Query('''SELECT 
                    p.userId as userId, 
                    p.points as points,
                    uAttrs.userIdForDisplay as userIdForDisplay,
                    uAttrs.firstName as userFirstName,
                    uAttrs.lastName as userLastName,
                    uAttrs.created as userFirstSeenTimestamp
                from UserPoints p, UserAttrs uAttrs
                where
                    p.userId = uAttrs.userId and
                    p.projectId=?1 and 
                    p.skillId is null and
                    (p.points>?2 OR (p.points=?2 and uAttrs.created<?3)) and
                    p.day is null
            ''')
    List<RankedUserRes>  findUsersForLeaderboardPointsMoreOrEqual(String projectId, Integer points, LocalDateTime createdDate, Pageable pageable)

    long countByProjectIdAndSkillIdAndDay(String projectId, @Nullable String skillId, @Nullable Date day)

    void deleteByProjectIdAndSkillId(String projectId, String skillId)


    @Query('''SELECT count(p) from UserPoints p where 
            p.projectId=?1 and 
            p.skillId=?2 and 
            p.points > ?3 and 
            p.day is null and 
            p.userId not in ?4 and 
            p.userId not in 
                (select u.userId from Setting s, User u where 
                    s.userRefId=u.id and 
                    s.settingGroup='user.prefs' and
                    s.setting='rank_and_leaderboard_optOut' and
                    s.value='true' and 
                    s.projectId is null)''' )
    Integer calculateNumUsersWithLessScore(String projectId, String skillId, int points, List<String> excludeUserIds)

    @Query('''SELECT count(p) from UserPoints p where 
            p.projectId=?1 and 
            p.skillId is null and 
            p.points > ?2 and 
            p.day is null and
            p.userId not in ?3 and 
            p.userId not in 
                (select u.userId from Setting s, User u where 
                    s.userRefId=u.id and 
                    s.settingGroup='user.prefs' and
                    s.setting='rank_and_leaderboard_optOut' and
                    s.value='true' and 
                    s.projectId is null)''' )
    Integer calculateNumUsersWithLessScore(String projectId, int points, List<String> excludeUserIds)

    @Query('''SELECT count(p) from UserPoints p, UserAttrs ua where 
            p.userId = ua.userId and 
            p.projectId=?1 and 
            p.skillId=?2 and 
            (p.points > ?3 OR (p.points = ?3 and ua.created < ?4)) and 
            p.day is null''' )
    Integer calculateNumUsersWithHigherScoreAndIfScoreTheSameThenAfterUserCreateDate(String projectId, String skillId, int points, LocalDateTime created)

    @Query('''SELECT count(p) from UserPoints p, UserAttrs ua where
            p.userId = ua.userId and 
            p.projectId=?1 and 
            p.skillId is null and 
            (p.points > ?2 OR (p.points = ?2 and ua.created < ?3)) and 
            p.day is null''')
    Integer calculateNumUsersWithHigherScoreAndIfScoreTheSameThenAfterUserCreateDate(String projectId, int points, LocalDateTime created)

    List<UserPoints> findByProjectIdAndSkillIdAndPointsGreaterThanAndDayIsNull(String projectId, @Nullable String skillId, int points, Pageable pageable)
    List<UserPoints> findByProjectIdAndSkillIdAndPointsLessThanAndDayIsNull(String projectId, @Nullable String skillId, int points, Pageable pageable)

    /**
     *  NOTE: this is query is identical to the below query the only difference is userPoints.day=?5, if you change this query you MUST change the one below
     *
     *  the reason for duplication is that when null is provided for the 'day' parameter JPA doesn't properly generate SQL statement, I am guessing the bug is because
     *  the parameter is withing left join clause and they didn't handle that properly
     */
    @Query('''select sdChild, userPoints
    from SkillDef sdParent, SkillRelDef srd, SkillDef sdChild
    left join UserPoints userPoints on sdChild.projectId = userPoints.projectId and sdChild.skillId = userPoints.skillId and userPoints.day=?6 and userPoints.userId=?1
      where srd.parent=sdParent.id and  srd.child=sdChild.id and sdChild.enabled = 'true' and
      sdParent.projectId=?2 and sdParent.skillId=?3 and srd.type in ?4 and sdChild.version<=?5''')
    List<Object []> findChildrenAndTheirUserPoints(String userId, String projectId, String skillId, List<SkillRelDef.RelationshipType> types, Integer version, Date day)

    /**
     *  NOTE: this is query is identical to the below query the only difference is userPoints.day=?5, if you change this query you MUST change the one below
     *
     *  the reason for duplication is that when null is provided for the 'day' parameter JPA doesn't properly generate SQL statement, I am guessing the bug is because
     *  the parameter is withing left join clause and they didn't handle that properly
     */
    @Query('''select sdChild, userPoints
    from SkillDef sdParent, SkillRelDef srd, SkillDef sdChild
    left join UserPoints userPoints on sdChild.projectId = userPoints.projectId and sdChild.skillId = userPoints.skillId and userPoints.day=?5 and userPoints.userId=?1
      where srd.parent=sdParent.id and  srd.child=sdChild.id and sdChild.enabled = 'true' and
      sdParent.projectId is null and sdParent.skillId=?2 and srd.type in ?3 and sdChild.version<=?4''')
    List<Object []> findGlobalChildrenAndTheirUserPoints(String userId, String skillId, List<SkillRelDef.RelationshipType> types, Integer version, Date day)

    /**
     *  NOTE: this is query is identical to the above query the only difference is 'userPoints.day is null', if you change this query you MUST change the one above
     *
     *  the reason for duplication is that when null is provided for the 'day' parameter JPA doesn't properly generate SQL statement, I am guessing the bug is because
     *      *  the parameter is withing left join clause and they didn't handle that properly
     */
    @Query('''select sdChild, userPoints, pd.name
    from SkillDef sdParent, SkillRelDef srd, SkillDef sdChild
    left join UserPoints userPoints on sdChild.projectId = userPoints.projectId and sdChild.skillId = userPoints.skillId and userPoints.day is null and userPoints.userId=?1
    left join ProjDef pd on sdChild.copiedFromProjectId = pd.projectId
      where srd.parent=sdParent.id and  srd.child=sdChild.id and sdChild.enabled = 'true' and
      sdParent.projectId=?2 and sdParent.skillId=?3 and srd.type in ?4 and sdChild.version<=?5 ''')
    List<Object []> findChildrenAndTheirUserPoints(String userId, String projectId, String skillId, List<SkillRelDef.RelationshipType> types, Integer version)


    /**
     *  NOTE: this is query is identical to the above query the only difference is 'userPoints.day is null', if you change this query you MUST change the one above
     *
     *  the reason for duplication is that when null is provided for the 'day' parameter JPA doesn't properly generate SQL statement, I am guessing the bug is because
     *      *  the parameter is withing left join clause and they didn't handle that properly
     */
    @Query('''select sdChild, userPoints
    from SkillDef sdParent, SkillRelDef srd, SkillDef sdChild
    left join UserPoints userPoints on sdChild.projectId = userPoints.projectId and sdChild.skillId = userPoints.skillId and userPoints.day is null and userPoints.userId=?1
      where srd.parent=sdParent.id and  srd.child=sdChild.id and sdChild.enabled = 'true' and
      sdParent.projectId is null and sdParent.skillId=?2 and srd.type in ?3 and sdChild.version<=?4''')
    List<Object []> findGlobalChildrenAndTheirUserPoints(String userId, String skillId, List<SkillRelDef.RelationshipType> types, Integer version)

    @Query('''select sdChild.id, achievement.id
    from SkillDef sdParent, SkillRelDef srd, SkillDef sdChild
    left join UserAchievement achievement on sdChild.projectId = achievement.projectId and sdChild.skillId = achievement.skillId and achievement.userId=?1
      where srd.parent=sdParent.id and  srd.child=sdChild.id and
      sdParent.projectId=?2 and sdParent.skillId=?3 and srd.type=?4 and sdChild.version<=?5''')
    List<Object []> findChildrenAndTheirAchievements(String userId, String projectId, String skillId, SkillRelDef.RelationshipType type, Integer version)

    @Query('''select sdParent.id as parentId, sdChild.id as childId, achievement.id as achievementId
    from SkillDef sdParent, SkillRelDef srd, SkillDef sdChild
    left join UserAchievement achievement on sdChild.projectId = achievement.projectId and sdChild.skillId = achievement.skillId and achievement.userId=?1
      where srd.parent=sdParent.id and  srd.child=sdChild.id and
      sdParent.projectId=?2 and srd.type=?3 and sdChild.version<=?4''')
    List<SkillWithChildAndAchievementIndicator> findAllChildrenAndTheirAchievementsForProject(String userId, String projectId, SkillRelDef.RelationshipType type, Integer version)

    @Query('''select sdParent.id as parentId, sdChild.id as childId, achievement.id as achievementId
    from SkillDef sdParent, SkillRelDef srd, SkillDef sdChild
    left join UserAchievement achievement on sdChild.projectId = achievement.projectId and sdChild.skillId = achievement.skillId and achievement.userId=?1
      where srd.parent=sdParent.id and  srd.child=sdChild.id and srd.type=?2 and sdChild.version<=?3''')
    List<SkillWithChildAndAchievementIndicator> findAllChildrenAndTheirAchievementsForGlobal(String userId, SkillRelDef.RelationshipType type, Integer version)

    static interface SkillWithChildAndAchievementIndicator {
        Integer getParentId()
        Integer getChildId()
        Integer getAchievementId()
    }

    @Query('''select up.day as day, count(up) as count
    from UserPoints up where up.projectId=?1 and up.day>=?2 and up.skillId is null and up.day is not null group by up.day
    ''')
    List<DayCountItem> findDistinctUserCountsByProject(String projectId, Date mustBeAfterThisDate)

    @Query('''select up.day as day, count(up) as count
    from UserPoints up where up.projectId=?1 and up.skillId=?2 and up.day>=?3 and up.day is not null group by up.day
    ''')
    List<DayCountItem> findDistinctUserCountsBySkillId(String projectId, String skillId, Date mustBeAfterThisDate)

    // Postgresql is 10 fold faster with the nested query over COUNT(DISTINCT)
    // using user_performed_skill table as it has less records than user_points
    @Query(value ='''SELECT COUNT(*)
        FROM (SELECT DISTINCT usr.user_id FROM user_performed_skill usr where usr.project_id = ?1) AS temp''',
            nativeQuery = true)
    Long countDistinctUserIdByProjectId(String projectId)

    @Query(value ='''SELECT COUNT(*)
        FROM (SELECT DISTINCT usattr.user_id 
                FROM user_performed_skill usr, user_attrs usattr 
                where 
                    usr.user_id = usattr.user_id and 
                    usr.project_id = ?1 and 
                    (lower(CONCAT(usattr.first_name, ' ', usattr.last_name, ' (', usattr.user_id_for_display, ')')) like lower(CONCAT('%', ?2, '%')) OR
                     lower(usattr.user_id_for_display) like lower(CONCAT('%', ?2, '%')))) 
                AS temp''',
            nativeQuery = true)
    Long countDistinctUserIdByProjectIdAndUserIdLike(String projectId, String userId)

    @Query(value = '''SELECT 
                up.user_id as userId, 
                max(upa.performedOn) as lastUpdated, 
                sum(up.points) as totalPoints,
                max(ua.first_name) as firstName,
                max(ua.last_name) as lastName,
                max(ua.dn) as dn,
                max(ua.email) as email,
                max(ua.user_id_for_display) as userIdForDisplay 
            FROM user_points up
            LEFT JOIN (
                SELECT user_id, 
                max(performed_on) AS performedOn 
                FROM user_performed_skill upa 
                WHERE upa.project_id=?1 
                GROUP BY user_id
                ) upa ON upa.user_id = up.user_id
            JOIN user_attrs ua ON ua.user_id=up.user_id
            WHERE 
                up.project_id=?1 and 
                (lower(CONCAT(ua.first_name, ' ', ua.last_name, ' (',  ua.user_id_for_display, ')')) like lower(CONCAT(\'%\', ?2, \'%\'))  OR
                 lower(ua.user_id_for_display) like lower(CONCAT('%', ?2, '%'))
                ) and 
                up.day is null and 
                up.skill_id is null 
            GROUP BY up.user_id''', nativeQuery = true)
    List<ProjectUser> findDistinctProjectUsersAndUserIdLike(String projectId, String query, Pageable pageable)

    @Query(value='''SELECT COUNT(*)
        FROM (SELECT DISTINCT up.user_id from user_points up where up.project_id=?1 and up.skill_id in (?2) and up.day is null) AS temp''',
            nativeQuery = true)
    Long countDistinctUserIdByProjectIdAndSkillIdIn(String projectId, List<String> skillIds)

    @Query(value='''SELECT COUNT(*)
        FROM (SELECT DISTINCT up.user_id 
            from user_points up, user_attrs usattr 
            where 
                up.user_id = usattr.user_id and
                up.project_id=?1 and 
                up.skill_id in (?2) and 
                (lower(CONCAT(usattr.first_name, ' ', usattr.last_name, ' (', usattr.user_id_for_display, ')')) like lower(CONCAT('%', ?3, '%')) OR
                 lower(usattr.user_id_for_display) like lower(CONCAT('%', ?3, '%'))) and   
                up.day is null) 
            AS temp''',
            nativeQuery = true)
    Long countDistinctUserIdByProjectIdAndSkillIdInAndUserIdLike(String projectId, List<String> skillIds, String query)

    @Query(value = '''SELECT 
                up.user_id as userId, 
                max(upa.performedOn) as lastUpdated, 
                sum(up.points) as totalPoints,
                max(ua.first_name) as firstName,
                max(ua.last_name) as lastName,
                max(ua.dn) as dn,
                max(ua.email) as email,
                max(ua.user_id_for_display) as userIdForDisplay 
            FROM user_points up
            LEFT JOIN (
                SELECT user_id, 
                max(performed_on) AS performedOn 
                FROM user_performed_skill upa 
                WHERE upa.skill_ref_id in (
                    select case when copied_from_skill_ref is not null then copied_from_skill_ref else id end as id from skill_definition where type = 'Skill' and project_id = ?1 and upa.skill_id in (?2)
                )
                GROUP BY user_id
                ) upa ON upa.user_id = up.user_id
            JOIN user_attrs ua ON ua.user_id=up.user_id
            WHERE 
                up.project_id=?1 and 
                up.skill_id in (?2) and 
                (lower(CONCAT(ua.first_name, ' ', ua.last_name, ' (',  ua.user_id_for_display, ')')) like lower(CONCAT('%', ?3, '%'))  OR
                 lower(ua.user_id_for_display) like lower(CONCAT('%', ?3, '%'))
                ) and 
                up.day is null 
            GROUP BY up.user_id''', nativeQuery = true)
    List<ProjectUser> findDistinctProjectUsersByProjectIdAndSkillIdInAndUserIdLike(String projectId, List<String> skillIds, String userId, Pageable pageable)

    @Nullable
    @Query('SELECT up.points from UserPoints up where up.projectId=?1 and up.userId=?2 and up.skillRefId=?3 and up.day is null')
    Integer getPointsByProjectIdAndUserIdAndSkillRefId(String projectId, String userId, @Nullable Integer skillRefId)

    @Nullable
    @Query('SELECT up.points from UserPoints up where up.projectId=?1 and up.userId=?2 and up.skillRefId is null and up.day is null')
    Integer getPointsByProjectIdAndUserId(String projectId, String userId)

    @Nullable
    @Query('SELECT up.points from UserPoints up where up.projectId=?1 and up.userId=?2 and up.skillRefId=?3 and up.day=?4')
    Integer getPointsByProjectIdAndUserIdAndSkillRefIdAndDay(String projectId, String userId, Integer skillRefId, Date day)

    @Modifying
    @Query(value = '''UPDATE 
                        user_points up 
                        SET points = points+?4 
                    WHERE 
                        up.project_id=?1 
                        AND 
                            (
                                up.skill_id=?2 
                                OR up.skill_id=?3 
                                OR up.skill_id IS NULL
                            ) 
                        AND up.day IS NULL 
                        AND EXISTS 
                            (
                                SELECT 1 FROM user_achievement ua 
                                WHERE ua.project_id=?1 
                                    AND ua.skill_id=?3 
                                    AND ua.user_id = up.user_id
                            )''', nativeQuery = true)
    void updateAchievedSkillPoints(String projectId, String subjectId, String skillId, int pointDelta)


    @Modifying
    @Query(value = '''
        WITH
            eventsRes AS (
                SELECT 
                    user_id, COUNT(id) eventCount
                FROM 
                    user_performed_skill
                WHERE 
                    skill_id = :skillId
                    AND project_id = :projectId
                GROUP BY 
                    user_id
            )
        UPDATE
            user_points points
        SET
            points = points + (eventsRes.eventCount * :incrementDelta)
        WHERE 
            eventsRes.user_id = points.user_id
            AND points.day IS NULL 
            AND points.project_id=:projectId 
            AND (points.skill_id = :subjectId OR points.skill_id = :skillId OR points.skill_id IS NULL)''', nativeQuery = true)
    void updatePointTotalsForSkill(@Param("projectId") String projectId, @Param("subjectId") String subjectId, @Param("skillId") String skillId, @Param("incrementDelta") int incrementDelta)

    @Modifying
    @Query(value = '''
            WITH
                eventsRes AS (
                    SELECT 
                        user_id, DATE(performed_on) performedOn, COUNT(id) eventCount
                    FROM 
                        user_performed_skill
                    WHERE 
                        skill_id = :skillId AND project_id = :projectId 
                    GROUP BY 
                        user_id, DATE(performed_on)
                )
            UPDATE 
                user_points points
            SET 
                points = points + (eventsRes.eventCount * :incrementDelta) 
            WHERE
                eventsRes.user_id = points.user_id
                AND eventsRes.performedOn = points.day
                AND points.skill_id = :skillId 
                AND points.project_id = :projectId''', nativeQuery=true)
    void updatePointHistoryForSkill(@Param("projectId") String projectId, @Param("skillId") String skillId, @Param("incrementDelta") int incrementDelta)

    @Modifying
    @Query(value = '''
            WITH skill AS (
                select case when copied_from_skill_ref is not null then copied_from_skill_ref else id end as id,
                       point_increment as pointIncrement
                from skill_definition
                where project_id = :projectId and skill_id = :skillId
            ),
            userPoints AS (
                SELECT ups.user_id, sum(skill.pointIncrement) as newPoints
                FROM user_performed_skill ups, skill
                where ups.skill_ref_id = skill.id
                group by ups.user_id
            )
            UPDATE user_points
            SET points = userPoints.newPoints
            FROM userPoints
            where userPoints.user_id = user_points.user_id and project_id = :projectId and skill_id = :skillId and day is null''', nativeQuery=true)
    void updateUserPointsForASkill(@Param("projectId") String projectId, @Param("skillId") String skillId)


    @Modifying
    @Query(value = '''
            WITH skills AS (
                select case when child.copied_from_skill_ref is not null then child.copied_from_skill_ref else child.id end as id,
                       child.point_increment as pointIncrement
                from skill_definition parent,
                     skill_relationship_definition rel,
                     skill_definition child
                where parent.project_id = :projectId
                  and parent.skill_id = :skillId
                  and rel.parent_ref_id = parent.id
                  and rel.child_ref_id = child.id
                  and rel.type in ('RuleSetDefinition', 'SkillsGroupRequirement')
                  and child.type = 'Skill'
                  and child.enabled = 'true'
            ),
            userPoints AS (
                SELECT ups.user_id, sum(skills.pointIncrement) as newPoints
                FROM user_performed_skill ups,
                     skills
                where ups.skill_ref_id = skills.id
                group by ups.user_id
            )
            UPDATE user_points
            SET points = userPoints.newPoints
            FROM userPoints
            where userPoints.user_id = user_points.user_id and project_id = :projectId and skill_id = :skillId and day is null''', nativeQuery=true)
    void updateUserPointsForASubjectOrGroup(@Param("projectId") String projectId, @Param("skillId") String skillId)

    @Modifying
    @Query(value = '''
            WITH skills AS (
                select case when copied_from_skill_ref is not null then copied_from_skill_ref else id end as id,
                       point_increment as pointIncrement
                from skill_definition child
                where project_id = :projectId
                  and type = 'Skill'
                  and enabled = 'true'
            ),
            userPoints AS (
                SELECT ups.user_id, sum(skills.pointIncrement) as newPoints
                FROM user_performed_skill ups,
                     skills
                where ups.skill_ref_id = skills.id
                group by ups.user_id
            )
            UPDATE user_points
            SET points = userPoints.newPoints
            FROM userPoints
            where userPoints.user_id = user_points.user_id and project_id = :projectId and skill_id is null and day is null''', nativeQuery=true)
    void updateUserPointsForAProject(@Param("projectId") String projectId)

    @Modifying
    @Query(value = '''
              WITH skill AS (
                    select case when copied_from_skill_ref is not null then copied_from_skill_ref else id end as id,
                           point_increment                                                                    as pointIncrement
                    from skill_definition
                    where project_id = :projectId
                      and skill_id = :skillId
                ),
                points as (
                    SELECT ups.user_id as userId, DATE(ups.performed_on) as pointsDay, sum(skill.pointIncrement) as newPoints
                    FROM user_performed_skill ups,
                         skill
                    where ups.skill_ref_id = skill.id
                    group by ups.user_id, DATE(ups.performed_on)
                ),
                userPointsRowsToRemove as (
                    select up.id
                    from user_points up
                             left join points on (up.day = points.pointsDay and up.user_id = points.userId)
                    where up.project_id = :projectId
                      and up.skill_id = :skillId
                      and up.day is not null
                      and points.userId is null
                ),
                updateDateHistory as (
                    update user_points up1 set points = points.newPoints
                         from points
                    where up1.user_id = points.userId
                      and up1.day = points.pointsDay
                      and up1.project_id = :projectId
                      and up1.skill_id = :skillId
                )
                delete from user_points where id in (select id from userPointsRowsToRemove);''', nativeQuery=true)
    void updateUserPointsHistoryForASkill(@Param("projectId") String projectId, @Param("skillId") String skillId)

    @Modifying
    @Query(value = '''
              WITH skills AS (
                select case when child.copied_from_skill_ref is not null then child.copied_from_skill_ref else child.id end as id,
                       child.point_increment as pointIncrement
                from skill_definition parent,
                     skill_relationship_definition rel,
                     skill_definition child
                where parent.project_id = :projectId
                  and parent.skill_id = :skillId
                  and rel.parent_ref_id = parent.id
                  and rel.child_ref_id = child.id
                  and rel.type in ('RuleSetDefinition', 'SkillsGroupRequirement')
                  and child.type = 'Skill'
                  and child.enabled = 'true'
            ),
             userPoints as (
                 SELECT ups.user_id as userId, DATE(ups.performed_on) as pointsDay, sum(skills.pointIncrement) as newPoints
                 FROM user_performed_skill ups,
                      skills
                 where ups.skill_ref_id = skills.id
                 group by ups.user_id, DATE(ups.performed_on)
             )
            UPDATE user_points up1
            SET points = userPoints.newPoints
            FROM userPoints
            where userPoints.userId = up1.user_id
              and day = userPoints.pointsDay
              and project_id = :projectId
              and skill_id = :skillId''', nativeQuery=true)
    void updateUserPointsHistoryForSubjectOrGroup(@Param("projectId") String projectId, @Param("skillId") String skillId)

    @Modifying
    @Query(value = '''
               WITH pointsToUpdate as (
                    select up.user_id, up.day, sum(up.points) as newPoints
                    from skill_definition sd,
                    user_points up
                    where sd.id = up.skill_ref_id
                            and sd.project_id = :projectId
                    and sd.type = 'Subject'
                    and sd.enabled = 'true'
                    group by up.user_id, up.day
                ),
                idsToRemove as (
                     select up.id
                     from user_points up
                              left join pointsToUpdate on (
                                  (up.day = pointsToUpdate.day) and up.user_id = pointsToUpdate.user_id)
                     where up.project_id = :projectId
                       and up.day is not null
                       and pointsToUpdate.user_id is null
                ),
                removeUserPoints as (
                    delete from user_points where id in (select id from idsToRemove)
                )
                update user_points up set points = pointsToUpdate.newPoints
                        from pointsToUpdate
                where up.user_id = pointsToUpdate.user_id
                and (up.day = pointsToUpdate.day or (up.day is null and pointsToUpdate.day is null))
                and up.project_id = :projectId and up.skill_id is null;''', nativeQuery=true)
    void updateUserPointsHistoryForProject(@Param("projectId") String projectId)

    @Modifying
    @Query(value = '''
             WITH pointsToUpdate as (
                select up.user_id userId, up.day as pointsDay, sum(up.points) as newPoints
                from skill_definition parent,
                     skill_relationship_definition rel,
                     skill_definition child,
                     user_points up
                where parent.project_id = :projectId
                  and parent.skill_id = :skillId
                  and rel.parent_ref_id = parent.id
                  and rel.child_ref_id = child.id
                  and rel.type in ('RuleSetDefinition', 'SkillsGroupRequirement')
                  and child.type = 'Skill'
                  and child.enabled = 'true'
                  and child.id = up.skill_ref_id
                group by up.user_id, up.day
            ),
            idsToRemove as (
                 select up.id
                 from user_points up
                          left join pointsToUpdate on (
                              (up.day = pointsToUpdate.pointsDay) and up.user_id = pointsToUpdate.userId)
                 where up.project_id = :projectId
                   and up.skill_id = :skillId
                   and up.day is not null
                   and pointsToUpdate.userId is null
            ),
            removeUserPoints as (
                delete from user_points where id in (select id from idsToRemove)
            )
            update user_points up set points = pointsToUpdate.newPoints
            from pointsToUpdate
            where up.user_id = pointsToUpdate.userId
              and (up.day = pointsToUpdate.pointsDay or (up.day is null and pointsToUpdate.pointsDay is null))
              and up.project_id = :projectId and up.skill_id = :skillId''', nativeQuery=true)
    void updateSubjectOrGroupUserPoints(@Param("projectId") String projectId, @Param("skillId") String skillId)
}
