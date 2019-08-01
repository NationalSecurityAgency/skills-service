package skills.storage.repos

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.lang.Nullable
import skills.storage.model.LevelDef
import skills.storage.model.LevelDefInterface
import skills.storage.model.SkillDef
import skills.storage.model.SkillRelDef

/**
 * Custom repo to support latency aware actions of reporting skill events code
 */
interface SkillEventsSupportRepo extends CrudRepository<SkillDef, Long> {

    static interface TinyProjectDef {
        Integer getId()
        Integer getTotalPoints()
    }

    static interface TinySkillDef {
        Integer getId()
        String getSkillId()
        String getName()
        SkillDef.ContainerType getType()
        Integer getTotalPoints()
        Integer getPointIncrement()
    }

    static interface TinyUserPoints {
        Integer getId()
        Integer getSkillRefId()
        Integer getPoints()
        Date getDay()
    }

    static interface TinyUserAchievement {
        Integer getSkillRefId()
        Integer getLevel()
    }

    @Query('''SELECT
        s.id as id,
        s.skillId as skillId,
        s.name as name,
        s.totalPoints as totalPoints,
        s.type as type,
        s.pointIncrement as pointIncrement
        from SkillDef s, SkillRelDef srd 
        where
            s.id = srd.parent and  
            srd.child.id=?1 and 
            srd.type =?2''')
    List<TinySkillDef> findTinySkillDefsParentsByChildIdAndType(Integer childId, SkillRelDef.RelationshipType type)


    @Query('''SELECT
        up.id as id,
        up.skillRefId as skillRefId,
        up.points as points,
        up.day as day
        from UserPoints up
        where
            up.projectId=?1 and  
            up.userId=?2 and
            (up.skillRefId in (?3) or up.skillRefId is null) and 
            (up.day=?4 or up.day is null)''')
    List<TinyUserPoints> findTinyUserPointsProjectIdAndUserIdAndSkillsAndDay(String projectId, String usedId, List<Integer> skillRefIds, Date day)

    static interface SkillDefMin {
        int getId()
        String getProjectId()
        String getSkillId()
        String getName()
        int getPointIncrement()
        int getPointIncrementInterval()
        int getNumMaxOccurrencesIncrementInterval()
        int getTotalPoints()
        SkillDef.ContainerType getType()
        Date getStartDate()
        Date getEndDate()
    }

    @Query('''SELECT
        s.id as id,
        s.projectId as projectId,
        s.skillId as skillId,
        s.name as name,
        s.pointIncrement as pointIncrement,
        s.pointIncrementInterval as pointIncrementInterval,
        s.numMaxOccurrencesIncrementInterval as numMaxOccurrencesIncrementInterval,
        s.totalPoints as totalPoints,
        s.type as type
        from SkillDef s where s.projectId = ?1 and s.skillId=?2 and s.type = ?3''')
    @Nullable
    SkillDefMin findByProjectIdAndSkillIdAndType(String projectId, String skillId, SkillDef.ContainerType type)

    @Modifying
    @Query('''update UserPoints up set up.points = up.points + ?2 where up.id = ?1''')
    void addUserPoints(Integer id, int pointsToAdd)

    @Query('''SELECT l from LevelDef l where l.skillRefId = ?1''')
    List<LevelDef> findLevelsBySkillId(Integer skillId)

    @Query('''SELECT 
        l.projectId as projectId,
        l.skillRefId as skillRefId,
        l.level as level,
        l.percent as percent,
        l.pointsFrom as pointsFrom,
        l.pointsTo as pointsTo 
        from LevelDef l where l.skillRefId in (?1) or l.projectId = ?2''')
    List<LevelDefInterface> findLevelsBySkillIdsOrByProjectId(List<Integer> skillIds, Integer projectId)


    @Query('''SELECT 
        ua.skillRefId as skillRefId,
        ua.level as level
        from UserAchievement ua 
        where 
            ua.userId = ?1 and
            (ua.skillRefId in (?3) or ua.skillRefId is null) and
            ua.projectId = ?2 ''')
    List<TinyUserAchievement> findTinyUserAchievementsByUserIdAndProjectIdAndSkillIds(String userId, String projectId, List<Integer> skillIds)

    @Query('''SELECT l from LevelDef l, ProjDef p 
        where
            l.projectId = p.id and 
            p.projectId = ?1''')
    List<LevelDef> findLevelsByProjectId(String projectId)

    @Query('''SELECT
        s.id as id,
        s.projectId as projectId,
        s.skillId as skillId,
        s.name as name,
        s.pointIncrement as pointIncrement,
        s.pointIncrementInterval as pointIncrementInterval,
        s.numMaxOccurrencesIncrementInterval as numMaxOccurrencesIncrementInterval,
        s.totalPoints as totalPoints,
        s.type as type,
        s.startDate as startDate,
        s.endDate as endDate
        from SkillDef s, SkillRelDef srd 
        where
            s.id = srd.parent and  
            srd.child.id=?1 and 
            srd.type=?2''')
    List<SkillDefMin> findParentSkillsByChildIdAndType(Integer childId, SkillRelDef.RelationshipType type)


    @Query('''SELECT 
        p.totalPoints as totalPoints,
        p.id as id 
        from ProjDef p where p.projectId = ?1''')
    TinyProjectDef getTinyProjectDef(String projectId)

}
