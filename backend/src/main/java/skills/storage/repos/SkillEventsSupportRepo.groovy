package skills.storage.repos

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.lang.Nullable
import skills.storage.model.LevelDef
import skills.storage.model.SkillDef
import skills.storage.model.SkillRelDef

/**
 * Custom repo to support latency aware actions of reporting skill events code
 */
interface SkillEventsSupportRepo extends CrudRepository<SkillDef, Long> {

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


    @Query('''SELECT l from LevelDef l where l.skillRefId = ?1''')
    List<LevelDef> findLevelsBySkillId(Integer skillId)

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
        s.type as type
        from SkillDef s, SkillRelDef srd 
        where
            s.id = srd.parent and  
            srd.child.id=?1 and 
            srd.type=?2''')
    List<SkillDefMin> findParentSkillsByChildIdAndType(Integer childId, SkillRelDef.RelationshipType type)


    @Query('''SELECT p.totalPoints from ProjDef p where p.projectId = ?1''')
    int getProjectTotalPoints(String projectId)

}
