package skills.storage.repos

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.lang.Nullable
import skills.storage.model.SkillDef
import skills.storage.model.SkillDefWithExtra
import skills.storage.model.SkillRelDef.RelationshipType

interface SkillDefRepo extends PagingAndSortingRepository<SkillDef, Integer> {

    static interface SkillDefSkinny {
        Integer getId()
        String getProjectId()
        String getName()
        String getSkillId()
        Integer getVersion()
        Integer getDisplayOrder()
        Date getCreated()
    }

    static interface SkillDefPartial extends SkillDefSkinny{
        Integer getPointIncrement()
        Integer getPointIncrementInterval()
        Integer getNumMaxOccurrencesIncrementInterval()
        Integer getTotalPoints()
        String getIconClass()
        SkillDef.ContainerType getSkillType()
        Date getUpdated()
    }

    /**
     * Need to create a custom query with limited fields as having many fields is slow,
     * for example 300 rows select is 330ms+ with description and 20ms without
     */
    @Query('''SELECT
        s.id as id,
        s.name as name,
        s.skillId as skillId,
        s.projectId as projectId,
        s.displayOrder as displayOrder,
        s.created as created,
        s.version as version
        from SkillDef s where s.projectId = ?1 and s.type = ?2''')
    List<SkillDefSkinny> findAllSkinnySelectByProjectIdAndType(String id, SkillDef.ContainerType type)

    @Nullable
    List<SkillDef> findAllByType(SkillDef.ContainerType type)

    List<SkillDef> findAllByProjectIdAndType(@Nullable String id, SkillDef.ContainerType type)
    @Nullable
    SkillDef findByProjectIdAndSkillIdIgnoreCaseAndType(@Nullable String id, String skillId, SkillDef.ContainerType type)
    @Nullable
    SkillDef findByProjectIdAndSkillIdAndType(String id, String skillId, SkillDef.ContainerType type)
    @Nullable
    SkillDef findByProjectIdAndNameIgnoreCaseAndType(@Nullable String id, String name, SkillDef.ContainerType type)

    @Query(value = '''SELECT max(sdChild.displayOrder) from SkillDef sdParent, SkillRelDef srd, SkillDef sdChild
      where srd.parent=sdParent.id and srd.child=sdChild.id and 
      sdParent.projectId=?1 and sdParent.skillId=?2 and srd.type='RuleSetDefinition' ''' )
    @Nullable
    Integer calculateChildSkillsHighestDisplayOrder(String projectId, String skillId)

    @Query(value = '''SELECT sum(sdChild.totalPoints) from SkillDef sdParent, SkillRelDef srd, SkillDef sdChild
      where srd.parent=sdParent.id and srd.child=sdChild.id and 
      sdParent.projectId=?1 and sdParent.skillId=?2 and srd.type=?3 and sdChild.version<=?4 ''' )
    @Nullable
    Integer calculateTotalPointsForSkill(String projectId, String skillId, RelationshipType relationshipType, Integer version)

    @Query(value='''SELECT c 
        from SkillDef s, SkillRelDef r, SkillDef c 
        where s.id=r.parent and c.id = r.child and 
             s.projectId=?1 and s.skillId=?2 and c.displayOrder>?3 and r.type=?4
             order by c.displayOrder asc''')
    List<SkillDef> findNextSkillDefs(String projectId, String skillId, int afterDisplayOrder, RelationshipType relationshipType, Pageable pageable)

    @Query(value='''SELECT c 
        from SkillDef s, SkillRelDef r, SkillDef c 
        where s.id=r.parent and c.id = r.child and 
             s.projectId=?1 and s.skillId=?2 and c.displayOrder<?3 and r.type=?4
             order by c.displayOrder desc''')
    List<SkillDef> findPreviousSkillDefs(String projectId, String skillId, int beforeDisplayOrder, RelationshipType relationshipType, Pageable pageable)

    long countByProjectIdAndType(String projectId, SkillDef.ContainerType type)

    @Query(value='''select count(c) 
        from SkillRelDef r, SkillDef c 
        where r.parent.id=?1 and c.id = r.child and r.type=?2''')
    long countChildSkillsByIdAndRelationshipType(Integer parentSkillRefId, RelationshipType relationshipType)

    @Query(value='''select c 
        from SkillRelDef r, SkillDef c 
        where r.parent.id=?1 and c.id = r.child and r.type=?2''')
    List<SkillDef> findChildSkillsByIdAndRelationshipType(Integer parentSkillRefId, RelationshipType relationshipType)

    @Query(value='''select sum(c.totalPoints) 
        from SkillRelDef r, SkillDef c 
        where r.parent.id=?1 and c.id = r.child and r.type=?2''')
    long sumChildSkillsTotalPointsBySkillAndRelationshipType(Integer parentSkillRefId, RelationshipType relationshipType)

    @Query(value = "SELECT COUNT(DISTINCT s.userId) from UserPoints s where s.projectId=?1 and s.skillId=?2")
    int calculateDistinctUsersForASingleSkill(String projectId, String skillId)

    boolean existsByProjectIdAndSkillIdAndTypeAllIgnoreCase(String id, String skillId, SkillDef.ContainerType type)
    boolean existsByProjectIdAndSkillIdAllIgnoreCase(@Nullable String id, String skillId)

    boolean existsByProjectIdAndNameAndTypeAllIgnoreCase(@Nullable String id, String name, SkillDef.ContainerType type)

    @Query('SELECT MAX (s.version) from SkillDef s where s.projectId=?1')
    Integer findMaxVersionByProjectId(String projectId)

    @Query(value='''SELECT count(c) 
        from SkillDef s, SkillRelDef r, SkillDef c 
        where 
            s.id = r.parent and c.id = r.child and 
            s.projectId=?1 and c.projectId=?1 and
            s.skillId=?2 and r.type=?3''')
    Integer countChildren(String projectId, String skillId, RelationshipType relationshipType)

    @Query("SELECT DISTINCT s.version from SkillDef s where s.projectId=?1 ORDER BY s.version ASC")
    List<Integer> getUniqueVersionList(String projectId)
}
