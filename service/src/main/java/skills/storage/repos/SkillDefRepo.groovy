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
import skills.storage.model.BadgeCount
import skills.storage.model.ImportExportStats
import skills.storage.model.SkillDef
import skills.storage.model.SkillDefMin
import skills.storage.model.SkillDefWithExtra
import skills.storage.model.SkillRelDef.RelationshipType

import java.util.stream.Stream

interface SkillDefRepo extends PagingAndSortingRepository<SkillDef, Integer> {

    static interface SkillDefSkinny {
        Integer getId()
        String getProjectId()
        String getSubjectSkillId()
        String getSubjectName()
        String getName()
        String getSkillId()
        Integer getVersion()
        Integer getDisplayOrder()
        Date getCreated()
        Integer getTotalPoints()
    }

    static interface SkillDefPartial extends SkillDefSkinny {
        Integer getPointIncrement()
        Integer getPointIncrementInterval()
        Integer getNumMaxOccurrencesIncrementInterval()
        String getIconClass()
        SkillDef.ContainerType getSkillType()
        Date getUpdated()
        SkillDef.SelfReportingType getSelfReportingType()
        String getEnabled()
        Integer getNumSkillsRequired()
        Integer getCopiedFrom()
        String getCopiedFromProjectId()
        Boolean getReadOnly()
        String getCopiedFromProjectName()
    }

    /**
     * Need to create a custom query with limited fields as having many fields is slow,
     * for example 300 rows select is 330ms+ with description and 20ms without
     */
    @Query('''SELECT
        s.id as id,
        s.name as name,
        s.skillId as skillId,
        subjectDef.skillId as subjectSkillId,
        subjectDef.name as subjectName,
        s.projectId as projectId,
        s.displayOrder as displayOrder,
        s.created as created,
        s.version as version,
        s.totalPoints as totalPoints
        from SkillDef s, SkillDef subjectDef, SkillRelDef srd
         where
            subjectDef = srd.parent and s = srd.child and 
            (srd.type = 'RuleSetDefinition' or srd.type = 'GroupSkillToSubject') and subjectDef.type = 'Subject' and 
            s.projectId = ?1 and s.type = ?2 and 
            upper(s.name) like UPPER(CONCAT('%', ?3, '%'))''')
    List<SkillDefSkinny> findAllSkinnySelectByProjectIdAndType(String id, SkillDef.ContainerType type, String skillNameQuery)

    @Nullable
    @Query('''SELECT         
        s.id as id,
        s.name as name, 
        s.skillId as skillId, 
        subjectDef.skillId as subjectSkillId,
        subjectDef.name as subjectName,
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
        from SkillDef s, SkillDef subjectDef, SkillRelDef srd 
        where
        subjectDef = srd.parent and s = srd.child and 
        srd.type = 'RuleSetDefinition' and subjectDef.type = 'Subject' and  
        s.type = ?1 and upper(s.name) like UPPER(CONCAT('%', ?2, '%'))''')
    List<SkillDefPartial> findAllByTypeAndNameLike(SkillDef.ContainerType type, String name)

    List<SkillDef> findAllByProjectIdAndType(@Nullable String id, SkillDef.ContainerType type)
    @Nullable
    SkillDef findByProjectIdAndSkillIdIgnoreCaseAndType(@Nullable String id, String skillId, SkillDef.ContainerType type)
    @Nullable
    SkillDef findByProjectIdAndSkillIdIgnoreCaseAndTypeIn(@Nullable String id, String skillId, List<SkillDef.ContainerType> types)
    @Nullable
    SkillDef findByProjectIdAndSkillIdAndType(String id, String skillId, SkillDef.ContainerType type)
    @Nullable
    SkillDef findByProjectIdAndSkillIdAndTypeIn(String id, String skillId, List<SkillDef.ContainerType> types)
    @Nullable
    SkillDef findByProjectIdAndNameIgnoreCaseAndType(@Nullable String id, String name, SkillDef.ContainerType type)
    @Nullable
    SkillDef findByProjectIdAndSkillId(String projectId, String skillId)

    @Query(value = '''SELECT max(sdChild.displayOrder) from SkillDef sdParent, SkillRelDef srd, SkillDef sdChild
      where srd.parent=sdParent.id and srd.child=sdChild.id and srd.type IN ('RuleSetDefinition', 'SkillsGroupRequirement') and 
      sdParent.projectId=?1 and sdParent.skillId=?2''' )
    @Nullable
    Integer calculateChildSkillsHighestDisplayOrder(String projectId, String skillId)

    @Query('''SELECT max(s.displayOrder) from SkillDef s where s.projectId=?1 and s.type=?2''')
    @Nullable
    Integer calculateHighestDisplayOrderByProjectIdAndType(String projectId, SkillDef.ContainerType type)

    @Query(value = '''SELECT sum(sdChild.totalPoints) from SkillDef sdParent, SkillRelDef srd, SkillDef sdChild
      where srd.parent=sdParent.id and srd.child=sdChild.id and sdChild.enabled = 'true' and  
      sdParent.projectId=?1 and sdParent.skillId=?2 and srd.type IN ('RuleSetDefinition', 'SkillsGroupRequirement') and sdChild.version<=?3 ''' )
    @Nullable
    Integer calculateTotalPointsForSubject(String projectId, String skillId, Integer version)


    @Query(value = '''SELECT sum(sdChild.totalPoints) from SkillDef sdParent, SkillRelDef srd, SkillDef sdChild
      where srd.parent=sdParent.id and srd.child=sdChild.id and sdChild.enabled = 'true' and 
      sdParent.projectId=?1 and srd.type=?2 and sdChild.version<=?3 ''' )
    @Nullable
    Integer calculateTotalPointsForProject(String projectId, RelationshipType relationshipType, Integer version)

    @Query(value='''SELECT c 
        from SkillDef s, SkillRelDef r, SkillDef c 
        where s.id=r.parent and c.id = r.child and 
             s.projectId=?1 and s.skillId=?2 and c.displayOrder>?3 and r.type in (?4)
             order by c.displayOrder asc''')
    List<SkillDef> findNextSkillDefs(String projectId, String skillId, int afterDisplayOrder, List<RelationshipType> relationshipType, Pageable pageable)

    @Query(value='''SELECT c 
        from SkillDef s, SkillRelDef r, SkillDef c 
        where s.id=r.parent and c.id = r.child and 
             s.projectId=?1 and s.skillId=?2 and c.displayOrder<?3 and r.type in (?4)
             order by c.displayOrder desc''')
    List<SkillDef> findPreviousSkillDefs(String projectId, String skillId, int beforeDisplayOrder, List<RelationshipType> relationshipType, Pageable pageable)

    int countByProjectIdAndType(@Nullable String projectId, SkillDef.ContainerType type)

    @Query('''select count(s) from SkillDef s 
            where (:projectId is null or s.projectId=:projectId) and s.type=:type and s.enabled = 'true'  
        ''')
    int countByProjectIdAndTypeWhereEnabled(@Nullable @Param('projectId') String projectId, @Param('type') SkillDef.ContainerType type)

    @Query('''select count(c) 
            from SkillRelDef r, SkillDef c 
            where r.parent.id=?1 and c.id = r.child and r.type=?2 and c.type = 'Skill' and c.enabled = 'true'
        ''')
    long countActiveChildSkillsByIdAndRelationshipType(Integer parentSkillRefId, RelationshipType relationshipType)

    @Query(value='''select count(sd) 
        from SkillRelDef srd, SkillDef sd
        where 
          srd.parent.id=?1
          and srd.child.id = sd.id 
          and srd.type = 'RuleSetDefinition' 
          and sd.type = 'SkillsGroup' 
          and sd.enabled = 'true'
      ''')
    long countActiveGroupsForSubject(Integer subjectId)

    @Query(value='''select count(c) 
        from SkillRelDef r, SkillDef c, SkillRelDef r2
        where 
          r.parent.id=?1
           and (
            (r.child.id = r2.parent.id 
             and c.id = r2.child 
             and c.type = 'Skill'
             and r.type = 'RuleSetDefinition' 
             and r2.type = 'SkillsGroupRequirement' 
             and r.child.enabled = 'true'
             )
           )
      ''')
    long countActiveGroupChildSkillsForSubject(Integer subjectId)
    @Query(value='''
        SELECT count(sd.id) 
                from skill_definition sd 
                where (
                (sd.type='GlobalBadge' 
                        AND ( 
                            exists (
                                SELECT true
                                from global_badge_level_definition gbld
                                where gbld.skill_ref_id = sd.id and gbld.project_id = ?1
                            ) 
                            OR ( 
                            sd.id in (
                                select srd.parent_ref_id from skill_relationship_definition srd join skill_definition ssd on srd.child_ref_id = ssd.id and ssd.project_id = ?1 
                                ) 
                            )
                    ) 
                )) AND
              sd.enabled  = 'true'
    ''', nativeQuery = true)
    int countGlobalBadgesIntersectingWithProjectIdWhereEnabled(@Param('projectId') String projectId)

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

    boolean existsByProjectIdAndSkillIdAndTypeInAllIgnoreCase(String id, String skillId, List<SkillDef.ContainerType> types)
    boolean existsByProjectIdAndSkillIdAllIgnoreCase(@Nullable String id, String skillId)
    boolean existsByProjectIdIgnoreCaseAndSkillId(@Nullable String id, String skillId)

    boolean existsByProjectIdAndNameAndTypeAllIgnoreCase(@Nullable String id, String name, SkillDef.ContainerType type)
    boolean existsByProjectIdAndNameAndTypeInAllIgnoreCase(@Nullable String id, String name, List<SkillDef.ContainerType> types)

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
        where 
            sd.type='Skill' and 
            sd.projectId IN 
            (
                select s.projectId
                from Setting s
                where s.projectId = sd.projectId
                  and s.setting = 'production.mode.enabled'
                  and s.value = 'true'
            ) and 
            sd.projectId IN (
                select s.projectId
                from Setting s, User uu
                where (s.setting = 'my_project' and uu.userId=?1 and uu.id = s.userRefId and s.projectId = sd.projectId)
            )  
    ''')
    Integer countTotalProductionSkills(String userId)

    @Query(value='''SELECT count(sd)
        from SkillDef sd 
        where (
        (
            sd.type = 'Badge' and 
            sd.projectId IN 
            (
                select s.projectId
                from Setting s
                where s.projectId = sd.projectId
                  and s.setting = 'production.mode.enabled'
                  and s.value = 'true'
            ) and
            sd.projectId IN (
                select s.projectId
                from Setting s, User uu
                where (s.setting = 'my_project' and uu.userId=?1 and uu.id = s.userRefId and s.projectId = sd.projectId)
            )
        ) OR 
        sd.type='GlobalBadge') and
      sd.enabled = 'true'
      ''')
    Integer countTotalProductionBadges(String userId)


    @Query(value='''SELECT count(sd) as totalCount,
            sum(case when sd.startDate is not null and sd.endDate is not null then 1 end) as gemCount,
            sum(case when sd.type='GlobalBadge' then 1 end) as globalCount
        from SkillDef sd 
        where (
        (
            sd.type = 'Badge' and 
            sd.projectId IN 
            (
                select s.projectId
                from Setting s
                where s.projectId = sd.projectId
                    and s.setting = 'production.mode.enabled'
                and s.value = 'true'
            ) and 
            sd.projectId IN (
                select s.projectId
                from Setting s, User uu
                where (s.setting = 'my_project' and uu.userId=?1 and uu.id = s.userRefId and s.projectId = sd.projectId)
            )
        ) OR 
        sd.type='GlobalBadge') and
      sd.enabled = 'true'
      ''')
    BadgeCount getProductionBadgesCount(String userId)


    @Query(value='''
        WITH mp AS (
            select s.project_id as project_id 
            from settings s, users uu, settings s1
            where s.setting = 'my_project' 
                and uu.user_id=?1 
                and uu.id = s.user_ref_id 
                and s.project_id = s1.project_id 
                and s1.setting = 'production.mode.enabled' 
                and s1.value = 'true'
        )
        
        SELECT count(sd.id) as totalCount,
                    sum(case when sd.start_date is not null and sd.end_date is not null then 1 end) as gemCount,
                    sum(case when sd.type='GlobalBadge' then 1 end) as globalCount
                from skill_definition sd 
                where (
                (sd.type = 'Badge' AND 
                    sd.project_id IN (
                        SELECT project_id FROM mp
                    )
                ) OR 
                (sd.type='GlobalBadge' 
                        AND ( 
                            exists (
                                SELECT true
                                from global_badge_level_definition gbld
                                where gbld.skill_ref_id = sd.id and gbld.project_id in (select project_id from mp)
                            ) 
                            OR ( 
                            sd.id in (
                                select srd.parent_ref_id from skill_relationship_definition srd join skill_definition ssd on srd.child_ref_id = ssd.id and ssd.project_id in (select project_id from mp) 
                                ) 
                            )
                    ) 
                )) AND
              sd.enabled = 'true'
    ''', nativeQuery = true)
    BadgeCount getProductionMyBadgesCount(String userId)

    static interface ProjectAndSubjectPoints {
        Integer getProjectTotalPoints()
        Integer getSubjectTotalPoints()
    }
    @Query(value='''SELECT sdParent.totalPoints as subjectTotalPoints, p.totalPoints as projectTotalPoints
            from SkillDef sdParent, SkillRelDef srd, SkillDef sdChild, ProjDef  p
            where 
                sdChild.projectId = p.projectId and 
                srd.parent=sdParent.id and 
                srd.child=sdChild.id and 
                sdChild.projectId=?1 and 
                sdChild.skillId=?2 and 
                srd.type IN ('RuleSetDefinition', 'GroupSkillToSubject')
        ''')
    ProjectAndSubjectPoints getProjectAndSubjectPoints(String projectId, String skillId)

    @Nullable
    @Query("SELECT sd from SkillDef sd where sd.skillId=?1 and sd.type='GlobalBadge'")
    SkillDef findGlobalBadgeByBadgeId(String badgeId)

    @Nullable
    @Query('''select s from SkillDef s where s.copiedFrom = ?1''')
    List<SkillDef> findSkillsCopiedFrom(int skillRefId)

    @Nullable
    @Query('''
        select s.id as id,
        s.projectId as projectId,
        s.skillId as skillId,
        s.name as name,
        s.pointIncrement as pointIncrement,
        s.pointIncrementInterval as pointIncrementInterval,
        s.numMaxOccurrencesIncrementInterval as numMaxOccurrencesIncrementInterval,
        s.totalPoints as totalPoints,
        s.type as type,
        s.startDate as startDate,
        s.endDate as endDate,
        s.enabled as enabled,
        s.copiedFrom as copiedFrom,
        s.copiedFromProjectId as copiedFromProjectId,
        s.readOnly as readOnly
        from SkillDef s where s.copiedFrom = ?1
    ''')
    List<SkillDefMin> findSkillDefMinCopiedFrom(int skillRefId)

    @Query('''
        select s.id as id,
        s.projectId as projectId,
        s.skillId as skillId,
        s.name as name,
        s.pointIncrement as pointIncrement,
        s.pointIncrementInterval as pointIncrementInterval,
        s.numMaxOccurrencesIncrementInterval as numMaxOccurrencesIncrementInterval,
        s.totalPoints as totalPoints,
        s.type as type,
        s.startDate as startDate,
        s.endDate as endDate,
        s.enabled as enabled,
        s.copiedFrom as copiedFrom,
        s.copiedFromProjectId as copiedFromProjectId,
        s.readOnly as readOnly
        from SkillDef s where s.id = ?1
    ''')
    SkillDefMin findSkillDefMinById(int id)

    @Query('''
          select s from SkillDef s where s.projectId = ?1 and s.readOnly = true  
    ''')
    List<SkillDef> findImportedSkills(String projectId, Pageable pageable)

    @Query('''
        select count(sd.id) as numberOfSkills, 
        count(distinct sd.copiedFromProjectId) as numberOfProjects
        from SkillDef sd where sd.copiedFromProjectId is not null
    ''')
    ImportExportStats getImportedSKillStats(String projectId)
}
