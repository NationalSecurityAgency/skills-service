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

@CompileStatic
interface UserPointsRepo extends CrudRepository<UserPoints, Integer> {

    @Nullable
    UserPoints findByProjectIdAndUserIdAndSkillIdAndDay(String projectId, String userId, @Nullable String skillId, @Nullable Date day)

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
        Date getUserFirstSeenTimestamp()
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
                    p.userId not in 
                        (select u.userId from Setting s, User u where 
                            s.userRefId=u.id and 
                            s.settingGroup='user.prefs' and
                            s.setting='rank_and_leaderboard_optOut' and
                            s.value='true' and 
                            s.projectId is null)
            ''')
    List<RankedUserRes> findUsersForLeaderboard(String projectId, Pageable pageable)

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
                    p.userId not in 
                        (select u.userId from Setting s, User u where 
                            s.userRefId=u.id and 
                            s.settingGroup='user.prefs' and
                            s.setting='rank_and_leaderboard_optOut' and
                            s.value='true' and 
                            s.projectId is null)
            ''')
    List<RankedUserRes> findUsersForLeaderboard(String projectId, String subjectId, Pageable pageable)

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
    List<RankedUserRes> findUsersForLeaderboardPointsLessOrEqual(String projectId, String subjectId, Integer points, Date usrCreated, Pageable pageable)

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
    List<RankedUserRes> findUsersForLeaderboardPointsLessOrEqual(String projectId, Integer points, Date usrCreated, Pageable pageable)

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
    List<RankedUserRes> findUsersForLeaderboardPointsMoreOrEqual(String projectId, String subjectId, Integer points, Date userCreateDate, Pageable pageable)

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
    List<RankedUserRes> findUsersForLeaderboardPointsMoreOrEqual(String projectId, Integer points, Date createdDate, Pageable pageable)

    long countByProjectIdAndSkillIdAndDay(String projectId, @Nullable String skillId, @Nullable Date day)

    void deleteByProjectIdAndSkillId(String projectId, String skillId)


    @Query('''SELECT count(p) from UserPoints p where 
            p.projectId=?1 and 
            p.skillId=?2 and 
            p.points > ?3 and 
            p.day is null and 
            p.userId not in 
                (select u.userId from Setting s, User u where 
                    s.userRefId=u.id and 
                    s.settingGroup='user.prefs' and
                    s.setting='rank_and_leaderboard_optOut' and
                    s.value='true' and 
                    s.projectId is null)''' )
    Integer calculateNumUsersWithLessScore(String projectId, String skillId, int points)

    @Query('''SELECT count(p) from UserPoints p where 
            p.projectId=?1 and 
            p.skillId is null and 
            p.points > ?2 and 
            p.day is null and
            p.userId not in 
                (select u.userId from Setting s, User u where 
                    s.userRefId=u.id and 
                    s.settingGroup='user.prefs' and
                    s.setting='rank_and_leaderboard_optOut' and
                    s.value='true' and 
                    s.projectId is null)''' )
    Integer calculateNumUsersWithLessScore(String projectId, int points)

    @Query('''SELECT count(p) from UserPoints p, UserAttrs ua where 
            p.userId = ua.userId and 
            p.projectId=?1 and 
            p.skillId=?2 and 
            (p.points > ?3 OR (p.points = ?3 and ua.created < ?4)) and 
            p.day is null''' )
    Integer calculateNumUsersWithHigherScoreAndIfScoreTheSameThenAfterUserCreateDate(String projectId, String skillId, int points, Date created)

    @Query('''SELECT count(p) from UserPoints p, UserAttrs ua where
            p.userId = ua.userId and 
            p.projectId=?1 and 
            p.skillId is null and 
            (p.points > ?2 OR (p.points = ?2 and ua.created < ?3)) and 
            p.day is null''')
    Integer calculateNumUsersWithHigherScoreAndIfScoreTheSameThenAfterUserCreateDate(String projectId, int points, Date created)

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
      where srd.parent=sdParent.id and  srd.child=sdChild.id and
      sdParent.projectId=?2 and sdParent.skillId=?3 and srd.type=?4 and sdChild.version<=?5''')
    List<Object []> findChildrenAndTheirUserPoints(String userId, String projectId, String skillId, SkillRelDef.RelationshipType type, Integer version, Date day)

    /**
     *  NOTE: this is query is identical to the below query the only difference is userPoints.day=?5, if you change this query you MUST change the one below
     *
     *  the reason for duplication is that when null is provided for the 'day' parameter JPA doesn't properly generate SQL statement, I am guessing the bug is because
     *  the parameter is withing left join clause and they didn't handle that properly
     */
    @Query('''select sdChild, userPoints
    from SkillDef sdParent, SkillRelDef srd, SkillDef sdChild
    left join UserPoints userPoints on sdChild.projectId = userPoints.projectId and sdChild.skillId = userPoints.skillId and userPoints.day=?5 and userPoints.userId=?1
      where srd.parent=sdParent.id and  srd.child=sdChild.id and
      sdParent.projectId is null and sdParent.skillId=?2 and srd.type=?3 and sdChild.version<=?4''')
    List<Object []> findGlobalChildrenAndTheirUserPoints(String userId, String skillId, SkillRelDef.RelationshipType type, Integer version, Date day)

    /**
     *  NOTE: this is query is identical to the above query the only difference is 'userPoints.day is null', if you change this query you MUST change the one above
     *
     *  the reason for duplication is that when null is provided for the 'day' parameter JPA doesn't properly generate SQL statement, I am guessing the bug is because
     *      *  the parameter is withing left join clause and they didn't handle that properly
     */
    @Query('''select sdChild, userPoints
    from SkillDef sdParent, SkillRelDef srd, SkillDef sdChild
    left join UserPoints userPoints on sdChild.projectId = userPoints.projectId and sdChild.skillId = userPoints.skillId and userPoints.day is null and userPoints.userId=?1
      where srd.parent=sdParent.id and  srd.child=sdChild.id and
      sdParent.projectId=?2 and sdParent.skillId=?3 and srd.type=?4 and sdChild.version<=?5''')
    List<Object []> findChildrenAndTheirUserPoints(String userId, String projectId, String skillId, SkillRelDef.RelationshipType type, Integer version)


    /**
     *  NOTE: this is query is identical to the above query the only difference is 'userPoints.day is null', if you change this query you MUST change the one above
     *
     *  the reason for duplication is that when null is provided for the 'day' parameter JPA doesn't properly generate SQL statement, I am guessing the bug is because
     *      *  the parameter is withing left join clause and they didn't handle that properly
     */
    @Query('''select sdChild, userPoints
    from SkillDef sdParent, SkillRelDef srd, SkillDef sdChild
    left join UserPoints userPoints on sdChild.projectId = userPoints.projectId and sdChild.skillId = userPoints.skillId and userPoints.day is null and userPoints.userId=?1
      where srd.parent=sdParent.id and  srd.child=sdChild.id and
      sdParent.projectId is null and sdParent.skillId=?2 and srd.type=?3 and sdChild.version<=?4''')
    List<Object []> findGlobalChildrenAndTheirUserPoints(String userId, String skillId, SkillRelDef.RelationshipType type, Integer version)

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

    @Query('''select new skills.storage.model.DayCountItem(up.day, count(up))
    from UserPoints up where up.projectId=?1 and up.day>=?2 and up.skillId is null and up.day is not null group by up.day
    ''')
    List<DayCountItem> findDistinctUserCountsByProject(String projectId, Date mustBeAfterThisDate)

    @Query('''select new skills.storage.model.DayCountItem(up.day, count(up))
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
                    (upper(CONCAT(usattr.first_name, ' ', usattr.last_name, ' (', usattr.user_id_for_display, ')')) like UPPER(CONCAT('%', ?2, '%')) OR
                     upper(usattr.user_id_for_display) like UPPER(CONCAT('%', ?2, '%')))) 
                AS temp''',
            nativeQuery = true)
    Long countDistinctUserIdByProjectIdAndUserIdLike(String projectId, String userId)

    @Query(value = '''SELECT 
                up.user_id as userId, 
                max(upa.performedOn) as lastUpdated, 
                sum(up.points) as totalPoints,
                max(ua.firstName) as firstName,
                max(ua.lastName) as lastName,
                max(ua.dn) as dn,
                max(ua.email) as email,
                max(ua.userIdForDisplay) as userIdForDisplay 
            FROM user_points up
            LEFT JOIN (
                SELECT user_id, 
                max(performed_on) AS performedOn 
                FROM user_performed_skill upa 
                WHERE upa.project_id=?1 
                GROUP BY user_id
                ) upa ON upa.user_id = up.user_id
            LEFT JOIN (
                SELECT 
                user_id, 
                max(first_name) AS firstName, 
                max(last_name) AS lastName, 
                max(dn) AS dn, 
                max(email) AS email, 
                max(user_id_for_display) AS userIdForDisplay 
                FROM user_attrs ua GROUP BY user_id
                ) ua ON ua.user_id=up.user_id
            WHERE 
                up.project_id=?1 and 
                (upper(CONCAT(ua.firstName, ' ', ua.lastName, ' (',  ua.userIdForDisplay, ')')) like UPPER(CONCAT(\'%\', ?2, \'%\'))  OR
                 upper(ua.userIdForDisplay) like UPPER(CONCAT('%', ?2, '%'))
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
                (upper(CONCAT(usattr.first_name, ' ', usattr.last_name, ' (', usattr.user_id_for_display, ')')) like UPPER(CONCAT('%', ?3, '%')) OR
                 upper(usattr.user_id_for_display) like UPPER(CONCAT('%', ?3, '%'))) and   
                up.day is null) 
            AS temp''',
            nativeQuery = true)
    Long countDistinctUserIdByProjectIdAndSkillIdInAndUserIdLike(String projectId, List<String> skillIds, String query)

    @Query('''SELECT 
                up.userId as userId, 
                max(upa.performedOn) as lastUpdated, 
                sum(up.points) as totalPoints,
                max(ua.firstName) as firstName,
                max(ua.lastName) as lastName,
                max(ua.dn) as dn,
                max(ua.email) as email,
                max(ua.userIdForDisplay) as userIdForDisplay  
            from UserPoints up, UserAttrs ua, UserPerformedSkill upa
            where 
                up.userId = ua.userId and
                up.projectId=?1 and 
                up.skillId in (?2) and
                upa.userId = ua.userId and
                upa.projectId=?1 and 
                upa.skillId in (?2) and
                (upper(CONCAT(ua.firstName, ' ', ua.lastName, ' (',  ua.userIdForDisplay, ')')) like UPPER(CONCAT('%', ?3, '%')) OR 
                 upper(ua.userIdForDisplay) like UPPER(CONCAT('%', ?3, '%'))) and 
                up.day is null 
            GROUP BY up.userId''')
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

}
