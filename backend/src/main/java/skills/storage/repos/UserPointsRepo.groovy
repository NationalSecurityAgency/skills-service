package skills.storage.repos

import groovy.transform.CompileStatic
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.lang.Nullable
import skills.service.controller.result.model.ProjectUser
import skills.storage.model.SkillRelDef
import skills.storage.model.UsageItem
import skills.storage.model.UserPoints

@CompileStatic
interface UserPointsRepo extends CrudRepository<UserPoints, Integer> {

    List<UserPoints> findAllByProjectIdAndUserIdAndDay(String projectId, String userId, Date day)
    List<UserPoints> findAllByProjectIdAndUserIdAndSkillId(String projectId, String userId, String skillId)
    @Nullable
    UserPoints findByProjectIdAndUserIdAndSkillIdAndDay(String projectId, String userId, @Nullable String skillId, @Nullable Date day)
    long countByProjectIdAndSkillIdAndDay(String projectId, @Nullable String skillId, @Nullable Date day)

    void deleteByProjectIdAndSkillId(String projectId, String skillId)

    @Query("SELECT p from UserPoints p where p.projectId=?1 and p.userId=?2 and p.skillId is null and p.day is not null" )
    List<UserPoints> findAllUserPointsUsageHistory(String projectId, String userId)

    @Query("SELECT p from UserPoints p where p.projectId=?1 and p.userId=?2 and p.skillId=?3 and p.day is not null" )
    List<UserPoints> findAllUserPointsUsageHistory(String projectId, String userId, String skillId)

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


    @Query('''select sdChild, achievement.id
    from SkillDef sdParent, SkillRelDef srd, SkillDef sdChild
    left join UserAchievement achievement on sdChild.projectId = achievement.projectId and sdChild.skillId = achievement.skillId and achievement.userId=?1
      where srd.parent=sdParent.id and  srd.child=sdChild.id and
      sdParent.projectId=?2 and sdParent.skillId=?3 and srd.type=?4 and sdChild.version<=?5''')
    List<Object []> findChildrenAndTheirAchievements(String userId, String projectId, String skillId, SkillRelDef.RelationshipType type, Integer version)

    @Query('''select up.day as day, count(up) as numItems
    from UserPoints up where up.projectId=?1 and up.day>=?2 and up.skillId is null and up.day is not null group by up.day
    ''')
    List<UsageItem> findDistinctUserCountsByProject(String projectId, Date mustBeAfterThisDate)

    @Query('''select up.day as day, count(up) as numItems
    from UserPoints up where up.projectId=?1 and up.skillId=?2 and up.day>=?3 and up.day is not null group by up.day
    ''')
    List<UsageItem> findDistinctUserCountsBySkillId(String projectId, String skillId, Date mustBeAfterThisDate)

    @Query("SELECT up from UserPoints up where up.projectId=?1 and up.skillId=?1 and up.day is null" )
    List<UserPoints> findDistinctUsersWithPoints(String projectId, String skillId)

    @Query('SELECT COUNT(DISTINCT userId) from UserPoints up where up.projectId=?1 and up.userId like %?2% and up.day is null')
    Long countDistinctUserIdByProjectIdAndUserIdLike(String projectId, String query)

    @Query('SELECT userId as userId, max(updated) as lastUpdated, sum(points) as totalPoints from UserPoints up where up.projectId=?1 and up.userId like %?2% and up.day is null and up.skillId is null GROUP BY userId')
    List<ProjectUser> findDistinctProjectUsersAndUserIdLike(String projectId, String query, Pageable pageable)

    @Query('SELECT COUNT(DISTINCT userId) from UserPoints up where up.projectId=?1 and up.skillId in (?2) and up.userId like %?3% and up.day is null')
    Long countDistinctUserIdByProjectIdAndSkillIdInAndUserIdLike(String projectId, List<String> skillIds, String userId)

    @Query('SELECT userId as userId, max(updated) as lastUpdated, sum(points) as totalPoints from UserPoints up where up.projectId=?1 and up.skillId in (?2) and up.userId like %?3% and up.day is null GROUP BY userId')
    List<ProjectUser> findDistinctProjectUsersByProjectIdAndSkillIdInAndUserIdLike(String projectId, List<String> skillIds, String userId, Pageable pageable)

}
