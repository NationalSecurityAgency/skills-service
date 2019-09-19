package skills.storage.repos

import groovy.transform.CompileStatic
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.lang.Nullable
import skills.controller.result.model.ProjectUser
import skills.storage.model.SkillRelDef
import skills.storage.model.DayCountItem
import skills.storage.model.UserPoints

@CompileStatic
interface UserPointsRepo extends CrudRepository<UserPoints, Integer> {

    @Nullable
    UserPoints findByProjectIdAndUserIdAndSkillIdAndDay(String projectId, String userId, @Nullable String skillId, @Nullable Date day)

    long countByProjectIdAndSkillIdAndDay(String projectId, @Nullable String skillId, @Nullable Date day)

    void deleteByProjectIdAndSkillId(String projectId, String skillId)

    @Query("SELECT count(p) from UserPoints p where p.projectId=?1 and p.skillId=?2 and p.points<?3 and p.day is null" )
    Integer calculateNumUsersWithLessScore(String projectId, String skillId, int points)

    @Query("SELECT count(p) from UserPoints p where p.projectId=?1 and p.skillId is null and p.points<?2 and p.day is null" )
    Integer calculateNumUsersWithLessScore(String projectId, int points)

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
      where srd.parent=sdParent.id and  srd.child=sdChild.id and
      sdParent.projectId is null and srd.type=?2 and sdChild.version<=?3''')
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
        FROM (SELECT DISTINCT usr.user_id FROM user_performed_skill usr where usr.project_id = ?1 and upper(usr.user_id) like UPPER(CONCAT('%', ?2, '%'))) AS temp''',
            nativeQuery = true)
    Long countDistinctUserIdByProjectIdAndUserIdLike(String projectId, String userId)

    @Query('SELECT userId as userId, max(updated) as lastUpdated, sum(points) as totalPoints from UserPoints up where up.projectId=?1 and upper(up.userId) like UPPER(CONCAT(\'%\', ?2, \'%\')) and up.day is null and up.skillId is null GROUP BY userId')
    List<ProjectUser> findDistinctProjectUsersAndUserIdLike(String projectId, String query, Pageable pageable)

    @Query(value='''SELECT COUNT(*)
        FROM (SELECT DISTINCT up.user_id from user_points up where up.project_id=?1 and up.skill_id in (?2) and up.day is null) AS temp''',
            nativeQuery = true)
    Long countDistinctUserIdByProjectIdAndSkillIdIn(String projectId, List<String> skillIds)

    @Query(value='''SELECT COUNT(*)
        FROM (SELECT DISTINCT up.user_id from user_points up where up.project_id=?1 and up.skill_id in (?2) and upper(up.user_id) like UPPER(CONCAT(\'%\', ?3, \'%\')) and up.day is null) AS temp''',
            nativeQuery = true)
    Long countDistinctUserIdByProjectIdAndSkillIdInAndUserIdLike(String projectId, List<String> skillIds, String userId)

    @Query('SELECT userId as userId, max(updated) as lastUpdated, sum(points) as totalPoints from UserPoints up where up.projectId=?1 and up.skillId in (?2) and upper(up.userId) like UPPER(CONCAT(\'%\', ?3, \'%\')) and up.day is null GROUP BY userId')
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
}
