package skills.storage.repos

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.lang.Nullable
import skills.storage.model.SkillDef
import skills.storage.model.SkillRelDef

interface SkillRelDefRepo extends CrudRepository<SkillRelDef, Integer> {
    List<SkillRelDef> findAllByChildAndType(SkillDef child, SkillRelDef.RelationshipType type)

    @Query('''SELECT srd from SkillRelDef srd where srd.child.id=?1 and srd.type=?2''')
    List<SkillRelDef> findAllByChildIdAndType(Integer childId, SkillRelDef.RelationshipType type)
    SkillRelDef findByChildAndParentAndType(SkillDef child, SkillDef parent, SkillRelDef.RelationshipType type)
    List<SkillRelDef> findAllByParentAndType(SkillDef parent, SkillRelDef.RelationshipType type)

    @Query(value = '''select count(srd.id) from SkillRelDef srd where srd.child.skillId=?1 and srd.type='BadgeDependence' and srd.parent.type = 'GlobalBadge' ''')
    Integer getSkillUsedInGlobalBadgeCount(String skillId)

    @Query(value = '''select count(srd.id) from SkillRelDef srd where srd.child.projectId=?1 and srd.type='BadgeDependence' and srd.parent.type = 'GlobalBadge' ''')
    Integer getProjectUsedInGlobalBadgeCount(String projectId)

    @Query('''SELECT 
        sd2.id as id,
        sd2.name as name, 
        sd2.skillId as skillId, 
        sd2.projectId as projectId, 
        sd2.version as version,
        sd2.pointIncrement as pointIncrement,
        sd2.pointIncrementInterval as pointIncrementInterval,
        sd2.numMaxOccurrencesIncrementInterval as numMaxOccurrencesIncrementInterval,
        sd2.totalPoints as totalPoints,
        sd2.type as skillType,
        sd2.displayOrder as displayOrder,
        sd2.created as created,
        sd2.updated as updated
        from SkillDef sd1, SkillDef sd2, SkillRelDef srd 
        where sd1 = srd.parent and sd2 = srd.child and srd.type=?3 
              and sd1.projectId=?1 and sd1.skillId=?2''')
    List<SkillDefRepo.SkillDefPartial> getChildrenPartial(String projectId, String parentSkillId, SkillRelDef.RelationshipType type)

    @Query('''SELECT 
        sd2.id as id,
        sd2.name as name, 
        sd2.skillId as skillId, 
        sd2.projectId as projectId, 
        sd2.version as version,
        sd2.pointIncrement as pointIncrement,
        sd2.pointIncrementInterval as pointIncrementInterval,
        sd2.numMaxOccurrencesIncrementInterval as numMaxOccurrencesIncrementInterval,
        sd2.totalPoints as totalPoints,
        sd2.type as skillType,
        sd2.displayOrder as displayOrder,
        sd2.created as created,
        sd2.updated as updated
        from SkillDef sd1, SkillDef sd2, SkillRelDef srd 
        where sd1 = srd.parent and sd2 = srd.child and srd.type=?2 
              and sd1.projectId is null and sd1.skillId=?1''')
    List<SkillDefRepo.SkillDefPartial> getGlobalChildrenPartial(String parentSkillId, SkillRelDef.RelationshipType type)

    @Query('''SELECT sd2 
        from SkillDef sd1, SkillDef sd2, SkillRelDef srd 
        where sd1 = srd.parent and sd2 = srd.child and srd.type=?3 
              and sd1.projectId=?1 and sd1.skillId=?2''')
    List<SkillDef> getChildren(@Nullable String projectId, String parentSkillId, SkillRelDef.RelationshipType type)

    static interface SkillDefSkinny {
        Integer getId()
        String getProjectId()
        String getName()
        String getSkillId()
        Integer getVersion()
        Integer getDisplayOrder()
        Date getCreated()
    }

    /**
     * mapping directly to entity is slow, we can save over a second in latency by mapping attributes explicitly
     */
    @Query('''select 
        sd1.id as id,
        sd1.name as name, 
        sd1.skillId as skillId, 
        sd1.projectId as projectId, 
        sd1.pointIncrement as pointIncrement,
        sd1.totalPoints as totalPoints,
        sd1.type as skillType,
         
        sd2.id as id,
        sd2.name as name, 
        sd2.skillId as skillId, 
        sd2.projectId as projectId, 
        sd2.pointIncrement as pointIncrement,
        sd2.totalPoints as totalPoints,
        sd2.type as skillType
        from SkillDef sd1, SkillDef sd2, SkillRelDef srd 
        where sd1 = srd.parent and sd2 = srd.child and srd.type=?2 
              and sd1.projectId=?1''')
    List<Object[]> getGraph(String projectId, SkillRelDef.RelationshipType type)
}
