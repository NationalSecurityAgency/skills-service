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
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.repository.query.Param
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
        Integer getTotalPoints()
    }

    static interface SkillDefPartial extends SkillDefSkinny{
        Integer getPointIncrement()
        Integer getPointIncrementInterval()
        Integer getNumMaxOccurrencesIncrementInterval()
        String getIconClass()
        SkillDef.ContainerType getSkillType()
        Date getUpdated()
        SkillDef.SelfReportingType getSelfReportingType()
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
        s.version as version,
        s.totalPoints as totalPoints
        from SkillDef s where s.projectId = ?1 and s.type = ?2''')
    List<SkillDefSkinny> findAllSkinnySelectByProjectIdAndType(String id, SkillDef.ContainerType type)

    @Nullable
    @Query('''SELECT         
        s.id as id,
        s.name as name, 
        s.skillId as skillId, 
        s.projectId as projectId, 
        s.version as version,
        s.pointIncrement as pointIncrement,
        s.pointIncrementInterval as pointIncrementInterval,
        s.numMaxOccurrencesIncrementInterval as numMaxOccurrencesIncrementInterval,
        s.totalPoints as totalPoints,
        s.type as skillType,
        s.displayOrder as displayOrder,
        s.created as created,
        s.updated as updated
        from SkillDef s where s.type = ?1 and upper(s.name) like UPPER(CONCAT('%', ?2, '%'))''')
    List<SkillDefPartial> findAllByTypeAndNameLike(SkillDef.ContainerType type, String name)

    List<SkillDef> findAllByProjectIdAndType(@Nullable String id, SkillDef.ContainerType type)
    @Nullable
    SkillDef findByProjectIdAndSkillIdIgnoreCaseAndType(@Nullable String id, String skillId, SkillDef.ContainerType type)
    @Nullable
    SkillDef findByProjectIdAndSkillIdAndType(String id, String skillId, SkillDef.ContainerType type)
    @Nullable
    SkillDef findByProjectIdAndNameIgnoreCaseAndType(@Nullable String id, String name, SkillDef.ContainerType type)
    @Nullable
    SkillDef findByProjectIdAndSkillId(String projectId, String skillId)

    @Query(value = '''SELECT max(sdChild.displayOrder) from SkillDef sdParent, SkillRelDef srd, SkillDef sdChild
      where srd.parent=sdParent.id and srd.child=sdChild.id and 
      sdParent.projectId=?1 and sdParent.skillId=?2 and srd.type='RuleSetDefinition' ''' )
    @Nullable
    Integer calculateChildSkillsHighestDisplayOrder(String projectId, String skillId)

    @Query('''SELECT max(s.displayOrder) from SkillDef s where s.projectId=?1 and s.type=?2''')
    @Nullable
    Integer calculateHighestDisplayOrderByProjectIdAndType(String projectId, SkillDef.ContainerType type)

    @Query(value = '''SELECT sum(sdChild.totalPoints) from SkillDef sdParent, SkillRelDef srd, SkillDef sdChild
      where srd.parent=sdParent.id and srd.child=sdChild.id and 
      sdParent.projectId=?1 and sdParent.skillId=?2 and srd.type=?3 and sdChild.version<=?4 ''' )
    @Nullable
    Integer calculateTotalPointsForSkill(String projectId, String skillId, RelationshipType relationshipType, Integer version)


    @Query(value = '''SELECT sum(sdChild.totalPoints) from SkillDef sdParent, SkillRelDef srd, SkillDef sdChild
      where srd.parent=sdParent.id and srd.child=sdChild.id and 
      sdParent.projectId=?1 and srd.type=?2 and sdChild.version<=?3 ''' )
    @Nullable
    Integer calculateTotalPointsForProject(String projectId, RelationshipType relationshipType, Integer version)

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

    int countByProjectIdAndType(@Nullable String projectId, SkillDef.ContainerType type)

    @Query('''select count(s) from SkillDef s 
            where (:projectId is null or s.projectId=:projectId) and s.type=:type and (s.enabled is null or s.enabled = 'true')  
        ''')
    int countByProjectIdAndTypeWhereEnabled(@Nullable @Param('projectId') String projectId, @Param('type') SkillDef.ContainerType type)

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
    boolean existsByProjectIdIgnoreCaseAndSkillId(@Nullable String id, String skillId)

    boolean existsByProjectIdAndNameAndTypeAllIgnoreCase(@Nullable String id, String name, SkillDef.ContainerType type)

    @Query('SELECT MAX (s.version) from SkillDef s where s.projectId=?1')
    Integer findMaxVersionByProjectId(String projectId)

    @Query(value='''SELECT count(c) 
        from SkillDef s, SkillRelDef r, SkillDef c 
        where 
            s.id = r.parent and c.id = r.child and 
            s.projectId=?1 and c.projectId=?1 and
            s.skillId=?2 and r.type=?3''')
    Integer countChildren(@Nullable String projectId, String skillId, RelationshipType relationshipType)

    @Query(value='''SELECT count(c) 
        from SkillDef s, SkillRelDef r, SkillDef c 
        where 
            s.id = r.parent and c.id = r.child and 
            s.projectId is null and
            s.skillId=?1 and r.type=?2''')
    Integer countGlobalChildren(String skillId, RelationshipType relationshipType)

    @Query("SELECT DISTINCT s.version from SkillDef s where s.projectId=?1 ORDER BY s.version ASC")
    List<Integer> getUniqueVersionList(String projectId)

    @Query(value='''SELECT count(sd)
        from SkillDef sd 
        where sd.type='Skill' and sd.projectId IN (
            select s.projectId
            from Setting s
            where s.projectId = sd.projectId
              and s.setting = 'production.mode.enabled'
              and s.value = 'true')''')
    Integer countTotalProductionSkills()

    @Query(value='''SELECT count(sd)
        from SkillDef sd 
        where (
        (sd.type = 'Badge' and sd.projectId IN (
            select s.projectId
            from Setting s
            where s.projectId = sd.projectId
              and s.setting = 'production.mode.enabled'
              and s.value = 'true')
        ) OR 
        sd.type='GlobalBadge') and
      (sd.enabled  = 'true' OR sd.enabled is null)''')
    Integer countTotalProductionBadges()
}
