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

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.repository.query.Param
import org.springframework.lang.Nullable
import org.springframework.transaction.annotation.Transactional
import skills.storage.model.*
import skills.storage.model.SkillDef.ContainerType
import skills.storage.model.SkillDef.SelfReportingType
import skills.storage.model.SkillRelDef.RelationshipType

interface SkillDefRepo extends CrudRepository<SkillDef, Integer>, PagingAndSortingRepository<SkillDef, Integer> {

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
        s.totalPoints as totalPoints,
        s.groupId as groupId,
        s.type as type
        from SkillDef s, SkillDef subjectDef, SkillRelDef srd
         where
            subjectDef = srd.parent and s = srd.child and 
            (srd.type = 'RuleSetDefinition' or srd.type = 'GroupSkillToSubject') and subjectDef.type = 'Subject' and 
            s.projectId = ?1 and s.type = ?2 and
            (s.copiedFromProjectId is null or 'true' = ?4) and
            (s.enabled = 'true' or 'true' = ?5) and
            lower(s.name) like lower(CONCAT('%', ?3, '%'))''')
    List<SkillDefSkinny> findAllSkinnySelectByProjectIdAndType(String id, SkillDef.ContainerType type, String skillNameQuery, String includeCatalogImportedSkills, String includeDisabled)

    @Query('''SELECT
        s.id as id,
        s.name as name,
        s.skillId as skillId,
        CASE WHEN s.type != 'Badge' THEN (SELECT subjectDef.skillId as subjectSkillId FROM SkillDef subjectDef, SkillRelDef srd WHERE (subjectDef = srd.parent and s = srd.child and 
            (srd.type = 'RuleSetDefinition' or srd.type = 'GroupSkillToSubject') and subjectDef.type = 'Subject')) END as subjectSkillId,
        CASE WHEN s.type != 'Badge' THEN (SELECT subjectDef.name as subjectName FROM SkillDef subjectDef, SkillRelDef srd WHERE (subjectDef = srd.parent and s = srd.child and 
            (srd.type = 'RuleSetDefinition' or srd.type = 'GroupSkillToSubject') and subjectDef.type = 'Subject')) END as subjectName,            
        s.projectId as projectId,
        s.displayOrder as displayOrder,
        s.created as created,
        s.version as version,
        s.totalPoints as totalPoints,
        s.groupId as groupId,
        s.type as type
        from SkillDef s
         where 
            s.projectId = ?1 and (s.type = 'Skill' or s.type = 'Badge') and
            (s.copiedFromProjectId is null or 'true' = ?3) and
            (s.enabled = 'true' or 'true' = ?4) and
            lower(s.name) like lower(CONCAT('%', ?2, '%'))''')
    List<SkillDefSkinny> findAllSkinnySkillsAndBadgesSelectByProjectId(String id, String skillNameQuery, String includeCatalogImportedSkills, String includeDisabled)

    @Nullable
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
            s.projectId = ?1 
            and s.skillId = ?2
            and subjectDef = srd.parent 
            and s = srd.child 
            and srd.type in ('RuleSetDefinition', 'GroupSkillToSubject')
            and subjectDef.type = 'Subject'
            ''')
    SkillDefSkinny getSkinnySkill(String projectId, String skillId)

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
        s.type = ?1 and lower(s.name) like lower(CONCAT('%', ?2, '%'))''')
    List<SkillDefPartial> findAllByTypeAndNameLike(SkillDef.ContainerType type, String name)


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
        s.type = ?1 and lower(s.name) like lower(CONCAT('%', ?2, '%')) and
        not exists (select 1 from Setting s2 where s.projectId = s2.projectId and s2.setting = 'user_community' and s2.value = 'true') and
        s.readOnly != true''')
    List<SkillDefPartial> findAllByTypeAndNameLikeNoImportedSkills(SkillDef.ContainerType type, String name)

    @Nullable
    @Query('''select max(displayOrder) from SkillDef where projectId = ?1 and type = ?2''')
    Integer getMaxDisplayOrderByProjectIdAndType(String projectId, SkillDef.ContainerType type)

    @Nullable
    @Query('''select max(displayOrder) from SkillDef where projectId is null and type = ?1''')
    Integer getMaxDisplayOrderByTypeAndProjectIdIsNull(SkillDef.ContainerType type)

    @Nullable
    List<SkillDef> findAllByProjectIdAndType(@Nullable String id, SkillDef.ContainerType type)

    List<SkillDef> findAllByProjectIdAndTypeIn(@Nullable String id, List<SkillDef.ContainerType> type)

    @Nullable
    List<SkillDef> findAllByProjectIdAndTypeAndEnabledAndCopiedFromIsNotNull(@Nullable String id, SkillDef.ContainerType type, String enabled)

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

    @Nullable
    @Query(value='''select name from SkillDef where projectId=?1 and iconClass=?2''')
    List<String> findNameByProjectIdAndIconClass(String projectId, String iconClass)

    @Query(value = '''SELECT max(sdChild.displayOrder) from SkillDef sdParent, SkillRelDef srd, SkillDef sdChild
      where srd.parent.id = sdParent.id and srd.child.id=sdChild.id and srd.type IN ('RuleSetDefinition', 'SkillsGroupRequirement') and 
      sdParent.projectId=?1 and sdParent.skillId=?2''' )
    @Nullable
    Integer calculateChildSkillsHighestDisplayOrder(String projectId, String skillId)

    @Query('''SELECT max(s.displayOrder) from SkillDef s where s.projectId=?1 and s.type=?2''')
    @Nullable
    Integer calculateHighestDisplayOrderByProjectIdAndType(String projectId, SkillDef.ContainerType type)

    @Query(value = '''SELECT sum(sdChild.totalPoints) from SkillDef sdParent, SkillRelDef srd, SkillDef sdChild
      where srd.parent.id = sdParent.id and srd.child.id=sdChild.id and sdChild.enabled = 'true' and  
      sdParent.projectId=?1 and sdParent.skillId=?2 and srd.type IN ('RuleSetDefinition', 'SkillsGroupRequirement') and sdChild.version<=?3 ''' )
    @Nullable
    Integer calculateTotalPointsForSubject(String projectId, String skillId, Integer version)


    @Query(value = '''SELECT sum(sdChild.totalPoints) from SkillDef sdParent, SkillRelDef srd, SkillDef sdChild
      where srd.parent.id = sdParent.id and srd.child.id=sdChild.id and sdChild.enabled = 'true' and 
      sdParent.projectId=?1 and srd.type=?2 and sdChild.version<=?3 ''' )
    @Nullable
    Integer calculateTotalPointsForProject(String projectId, RelationshipType relationshipType, Integer version)

    @Query(value='''SELECT c 
        from SkillDef s, SkillRelDef r, SkillDef c 
        where s.id=r.parent.id and c.id = r.child.id and 
             s.projectId=?1 and s.skillId=?2 and c.displayOrder>?3 and r.type in (?4)
             order by c.displayOrder asc''')
    List<SkillDef> findNextSkillDefs(String projectId, String skillId, int afterDisplayOrder, List<RelationshipType> relationshipType, Pageable pageable)

    @Query(value='''SELECT c 
        from SkillDef s, SkillRelDef r, SkillDef c 
        where s.id=r.parent.id and c.id = r.child.id and 
             s.projectId=?1 and s.skillId=?2 and c.displayOrder<?3 and r.type in (?4)
             order by c.displayOrder desc''')
    List<SkillDef> findPreviousSkillDefs(String projectId, String skillId, int beforeDisplayOrder, List<RelationshipType> relationshipType, Pageable pageable)

    @Nullable
    @Query(value='''select child.skill_id as skillId, child.display_order as displayOrder, skillGroup.display_order as skillGroupDisplayOrder, child.group_id as groupId, child.type as type
                from skill_definition subj,
                     skill_relationship_definition rel,
                     skill_definition child left join skill_definition skillGroup on (child.group_id = skillGroup.skill_id and skillGroup.project_id = child.project_id)
                where subj.project_id = ?1
                  and subj.skill_id = ?2
                  and child.type = 'Skill'
                  and subj.id = rel.parent_ref_id
                  and child.id = rel.child_ref_id''', nativeQuery=true)
    List<DisplayOrderRes> findDisplayOrderByProjectIdAndSubjectId(String projectId, String subjectId)

    int countByProjectIdAndType(@Nullable String projectId, SkillDef.ContainerType type)

    @Query('''select count(s) from SkillDef s 
            where (:projectId is null or s.projectId=:projectId) and s.type=:type and s.enabled = 'true'  
        ''')
    int countByProjectIdAndTypeWhereEnabled(@Nullable @Param('projectId') String projectId, @Param('type') SkillDef.ContainerType type)

    @Query('''select count(c) 
            from SkillRelDef r, SkillDef c 
            where r.parent.id=?1 and c.id = r.child.id and r.type=?2 and c.type = 'Skill' and c.enabled = ?3
        ''')
    long countChildSkillsByIdAndRelationshipTypeAndEnabled(Integer parentSkillRefId, RelationshipType relationshipType, String enabled)

    @Query('''select
            sum(case when c.enabled = 'true' and  c.type = 'Skill' then 1 end) as enabledSkillsCount,
            sum(case when c.enabled = 'false' and  c.type = 'Skill' then 1 end) as disabledSkillsCount,
            sum(case when c.enabled = 'false' and  c.type = 'Skill' and c.copiedFrom is not null then 1 end) as disabledImportedSkillsCount,
            sum(case when c.enabled = 'true' and  c.type = 'SkillsGroup' then 1 end) as enabledGroupsCount,
            sum(case when c.enabled = 'false' and  c.type = 'SkillsGroup' then 1 end) as disabledGroupsCount,
            sum(case when skillId like '%STREUSESKILLST%' and  c.type = 'Skill' then 1 end) as numSkillsReused,
            sum(case when skillId like '%STREUSESKILLST%' and  c.type = 'Skill' then c.totalPoints end) as totalPointsReused
            from SkillRelDef r, SkillDef c 
            where r.parent.id=?1 and c.id = r.child.id and r.type in ('RuleSetDefinition', 'GroupSkillToSubject')
        ''')
    SkillCounts getSkillsCountsForParentId(Integer parentSkillRefId)

    long countByProjectIdAndEnabledAndCopiedFromIsNotNull(String projectId, String enabled)

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
             and c.id = r2.child.id 
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
        where r.parent.id=?1 and c.id = r.child.id and r.type=?2''')
    long countChildSkillsByIdAndRelationshipType(Integer parentSkillRefId, RelationshipType relationshipType)

    @Query(value='''select c 
        from SkillRelDef r, SkillDef c 
        where r.parent.id=?1 and c.id = r.child.id and r.type=?2''')
    List<SkillDef> findChildSkillsByIdAndRelationshipType(Integer parentSkillRefId, RelationshipType relationshipType)

    @Query(value='''select c.skillId 
        from SkillRelDef r, SkillDef c 
        where r.parent.id=c.id and r.child.skillId = ?1 and r.type=?2 and c.type=?3 and c.projectId=?4''')
    List<String> findParentSkillsByIdAndRelationshipType(String childSkillId, RelationshipType relationshipType, ContainerType containerType, String projectId)

    @Query(value='''select s.skillId as badgeId, s.name as name, c.skillId as skillId, s.type as skillType
        from SkillDef s, SkillRelDef r, SkillDef c 
        where s.id = r.parent.id and c.id = r.child.id and r.type='BadgeRequirement' and (
              (s.type='Badge' and s.projectId=?2) or s.type='GlobalBadge') and s.enabled='true' and
              c.id = r.child.id and c.skillId in ?1 and c.type='Skill' and c.projectId=?2 order by c.skillId, s.skillId''')
    List<SimpleBadgeRes> findAllBadgesForSkill(List<String> skillId, String projectId)

    @Query(value='''select sum(c.totalPoints) 
        from SkillRelDef r, SkillDef c 
        where r.parent.id=?1 and c.id = r.child.id and r.type=?2''')
    long sumChildSkillsTotalPointsBySkillAndRelationshipType(Integer parentSkillRefId, RelationshipType relationshipType)

    @Query(value = "SELECT COUNT(DISTINCT s.userId) from UserPoints s where s.projectId=?1 and s.skillId=?2")
    int calculateDistinctUsersForASingleSkill(String projectId, String skillId)

    boolean existsByProjectIdAndSkillIdAndTypeInAllIgnoreCase(String id, String skillId, List<SkillDef.ContainerType> types)
    boolean existsByProjectIdAndSkillIdAllIgnoreCase(@Nullable String id, String skillId)
    boolean existsByProjectIdIgnoreCaseAndSkillId(@Nullable String id, String skillId)

    boolean existsByProjectIdAndNameAndTypeAllIgnoreCase(@Nullable String id, String name, SkillDef.ContainerType type)
    boolean existsByProjectIdAndNameAndTypeInAllIgnoreCase(@Nullable String id, String name, List<SkillDef.ContainerType> types)

    @Nullable
    @Query('SELECT MAX (s.version) from SkillDef s where s.projectId=?1')
    Integer findMaxVersionByProjectId(String projectId)

    @Query(value='''SELECT count(c) 
        from SkillDef s, SkillRelDef r, SkillDef c 
        where 
            s.id = r.parent.id and c.id = r.child.id and 
            s.projectId=?1 and c.projectId=?1 and
            s.skillId=?2 and r.type=?3''')
    Integer countChildren(@Nullable String projectId, String skillId, RelationshipType relationshipType)

    @Query(value='''SELECT count(c) 
        from SkillDef s, SkillRelDef r, SkillDef c 
        where 
            s.id = r.parent.id and c.id = r.child.id and 
            s.projectId is null and
            s.skillId=?1 and r.type=?2''')
    Integer countGlobalChildren(String skillId, RelationshipType relationshipType)

    @Query("SELECT DISTINCT s.version from SkillDef s where s.projectId=?1 ORDER BY s.version ASC")
    List<Integer> getUniqueVersionList(String projectId)


    @Nullable
    @Query("SELECT s.id from SkillDef s where s.projectId=?1 and s.skillId=?2 and s.type=?3")
    Integer getIdByProjectIdAndSkillIdAndType(String projectId, String skillId, ContainerType containerType)

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
        String getSubjectId()
    }
    @Query(value='''SELECT sdParent.totalPoints as subjectTotalPoints, sdParent.skillId as subjectId, p.totalPoints as projectTotalPoints
            from SkillDef sdParent, SkillRelDef srd, SkillDef sdChild, ProjDef  p
            where 
                sdChild.projectId = p.projectId and 
                srd.parent.id = sdParent.id and 
                srd.child.id=sdChild.id and 
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
        s.groupId as groupId,
        s.readOnly as readOnly
        from SkillDef s where s.copiedFrom = ?1 and s.enabled = 'true'
    ''')
    List<SkillDefMin> findSkillDefMinCopiedFrom(int skillRefId)

    @Nullable
    @Query('''
        select s.id as id from SkillDef s where s.copiedFrom = ?1 and s.enabled = 'true'
    ''')
    List<Integer> findSkillDefIdsByCopiedFrom(int skillRefId)

    @Query('''
        select count(s.id) > 0 from SkillDef s where s.copiedFrom = ?1
    ''')
    Boolean isCatalogSkillImportedByOtherProjects(int skillRefId)

    @Nullable
    @Query('''
        select s.copiedFrom as id from SkillDef s where s.id = ?1
    ''')
    Integer getCopiedFromById(int skillRefId)

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
        s.groupId as groupId,
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

    static interface ProjectIdAndSkill {
        Integer getSkillRefId()
        String getProjectId()
    }
    @Nullable
    @Query('''
          select imported.id as skillRefId, imported.projectId as projectId
          from SkillDef orig, SkillDef imported
          where orig.projectId = ?1
           and orig.id = imported.copiedFrom
           and imported.enabled = 'true'  
    ''')
    List<ProjectIdAndSkill> getAllImportedByOriginalProject(String projectId)

    @Nullable
    @Query('''
          select imported.id as skillRefId, imported.projectId as projectId
          from SkillDef imported
          where imported.copiedFrom in ?1
           and imported.enabled = 'true'  
    ''')
    List<ProjectIdAndSkill> getAllImportedCopiesByOriginalSkillRefIds(List<Integer> skillRefId)

    @Query('''
        select count(sd.id) as numberOfSkills, 
        count(distinct sd.copiedFromProjectId) as numberOfProjects
        from SkillDef sd where sd.copiedFromProjectId is not null
    ''')
    ImportExportStats getImportedSKillStats(String projectId)

    @Modifying
    @Query(value = '''update project_definition
        set total_points = (
            select case when sum(total_points) is not null then sum(total_points) else 0 end as totalPoints
            from skill_definition
            where project_id = :projectId
              and type = 'Subject'
              and (enabled = 'true' or 'false' = :enabledSkillsOnly ))
        where project_id = :projectId''', nativeQuery = true)
    void updateProjectsTotalPoints(@Param('projectId') String projectId, @Param('enabledSkillsOnly') Boolean enabledSkillsOnly)

    @Query(value = '''select case when sum(total_points) is not null then sum(total_points) else 0 end as totalPoints
            from skill_definition
            where project_id = :projectId
              and type = 'Subject'
              and (enabled = 'true' or 'false' = :enabledSkillsOnly )''', nativeQuery = true)
    Integer getProjectsTotalPoints(@Param('projectId') String projectId, @Param('enabledSkillsOnly') Boolean enabledSkillsOnly)

    static interface MinMaxPoints {
        Integer getMinPoints()
        Integer getMaxPoints()
    }

    @Query(value = '''select min(total_points) as minPoints, max(total_points) as maxPoints
            from skill_definition
            where project_id = :projectId
              and type = 'Skill'
              and enabled = 'true'
              ''', nativeQuery = true)
    MinMaxPoints getSkillMinAndMaxTotalPoints(@Param('projectId') String projectId)

    static interface SkillWithPoints {
        String getSkillId()
        String getSkillName()
        Integer getTotalPoints()
    }
    @Query(value = '''select name as skillName, skill_id as skillId, total_points as totalPoints
            from skill_definition
            where project_id = :projectId
              and type = 'Skill'
              and enabled = 'false'
              and (total_points < :lessThanExclusive OR total_points > :moreThanExclusive)
              ''', nativeQuery = true)
    List<SkillWithPoints> getDisabledSkillsOutOfRange(@Param('projectId') String projectId, @Param('lessThanExclusive') Integer lessThanExclusive, @Param('moreThanExclusive') Integer moreThanExclusive )

    @Query(value = '''select case when sum(skill.total_points) is not null then sum(skill.total_points) else 0 end as totalPoints
            from skill_relationship_definition rel,
                 skill_definition skill
            where rel.parent_ref_id = :subjectRefId 
              and skill.id = rel.child_ref_id
              and rel.type in :relationshipTypes
              and skill.type = 'Skill'
              and (skill.enabled = 'true' or 'false' = :enabledSkillsOnly)
          ''', nativeQuery = true)
    Integer calculateSkillDefTotalPointsBySummingChildPoints(@Param('subjectRefId') Integer subjectRefId,
                                                             @Param('relationshipTypes') List<String> relationshipTypeList,
                                                             @Param('enabledSkillsOnly') Boolean enabledSkillsOnly)

    @Query(value = '''
         select exists (select 1 from skill_definition where project_id = :projectId and skill_id = :skillId and read_only = 'true') as isReadOnly
    ''', nativeQuery = true)
    boolean isImportedFromCatalog(@Param('projectId') String projectId, @Param('skillId') String skillId)

    @Query(value = '''
         select skill_id from skill_definition where project_id = ?1 and copied_from_skill_ref = ?2
    ''', nativeQuery = true)
    List<String> getSkillIdsOfReusedSkillsForAGivenSkill(String projectId, Integer originalSkillRef)

    @Nullable
    @Query('''select distinct 'true' from SkillDef sd where sd.projectId = ?1 and sd.type = 'Tag' 
    ''')
    Boolean doesProjectHaveSkillTags(String projectId)

    @Query(value='''select distinct sd.skill_id as tagId, sd.name as tagValue
                    from skill_definition sd, skill_relationship_definition srd
                    where srd.child_ref_id=?1 and sd.id = srd.parent_ref_id and srd.type = 'Tag'
    ''', nativeQuery = true)
    List<SkillTag> getTagsForSkill(Integer childSkillRefId)

    @Query(value='''select distinct tag.skill_id as tagId, tag.name as tagValue
                    from skill_definition tag, skill_definition sd1, skill_relationship_definition srd
                    where tag.project_id=?1 and sd1.project_id=?1 and sd1.skill_id in (?2) and sd1.id = srd.child_ref_id and tag.id = srd.parent_ref_id and srd.type = 'Tag'
    ''', nativeQuery = true)
    List<SkillTag> getTagsForSkills(String projectId, List<String> skillIds)

    @Query(value='''select distinct sd.skill_id as tagId, sd.name as tagValue
                    from skill_definition sd
                    where sd.project_id=?1 and sd.type = 'Tag'
    ''', nativeQuery = true)
    List<SkillTag> getTagsForProject(String projectId)

    @Query(value='''select tag.skill_id as tagId, tag.name as tagValue, skill.skill_id as skillId
                    from skill_definition tag, skill_relationship_definition srd, skill_definition skill
                    where srd.child_ref_id = skill.id and tag.id = srd.parent_ref_id and srd.type = 'Tag'
                      and tag.type = 'Tag' and tag.project_id = ?1
                      and skill.skill_id in (?2)
                      and skill.type = 'Skill' and skill.project_id = ?1
    ''', nativeQuery = true)
    List<SkillTagWithSkillId> getTagsForSkillsWithSkillId(String projectId, List<String> skillIds)

    @Query(value='''select tag.skill_id as tagId, tag.name as tagValue
                    from skill_definition tag, skill_relationship_definition srd, skill_definition skill
                    where srd.child_ref_id = skill.id and tag.id = srd.parent_ref_id and srd.type = 'Tag'
                      and tag.type = 'Tag' and tag.project_id = ?1
                      and skill.skill_id = ?2
                      and skill.type = 'Skill' and skill.project_id = ?1
    ''', nativeQuery = true)
    List<SkillTag> getTagsForSkillWithSkillId(String projectId, String skillId)

    @Modifying
    @Query(value='''delete from skill_definition sd where sd.id in (
                        select distinct tag.id
                        from skill_definition tag, skill_definition sd1, skill_relationship_definition srd
                        where tag.project_id=?1 and sd1.project_id=?1 and tag.skill_id=?2 and sd1.skill_id in (?3) and sd1.id = srd.child_ref_id and tag.id = srd.parent_ref_id and srd.type = 'Tag'
                    )
    ''', nativeQuery = true)
    void deleteTagsForSkills(String projectId, String tagId, List<String> skillIds)

    @Query('''SELECT
        s.id as id,
        s.name as name,
        s.skillId as skillId,
        parentDef.skillId as subjectSkillId,
        parentDef.name as subjectName,
        s.projectId as projectId,
        s.displayOrder as displayOrder,
        s.created as created,
        s.version as version,
        s.totalPoints as totalPoints
        from SkillDef s, SkillDef parentDef, SkillRelDef srd
         where
            parentDef.projectId = ?1
            and parentDef.skillId = ?2 
            and parentDef = srd.parent 
            and s = srd.child 
            and srd.type in ('RuleSetDefinition', 'SkillsGroupRequirement') 
            and parentDef.type in ('Subject', 'SkillsGroup')
            and s.type = 'Skill'
            and s.skillId like '%STREUSESKILLST%'
            ''')
    List<SkillDefSkinny> findChildReusedSkills(String projectId, String parentId)

    @Nullable
    @Query('''select count(s) > 0 from SkillDefWithExtra s where s.copiedFrom = ?1 and s.skillId like '%STREUSESKILLST%' ''')
    Boolean wasThisSkillReusedElsewhere(int skillRefId)

    @Nullable
    @Query('''SELECT sd.totalPoints FROM SkillDef sd WHERE sd.projectId = ?1 and sd.skillId = ?2''')
    Integer getTotalPointsByProjectIdAndSkillId(String projectId, String skillId)

    @Nullable
    @Query('''SELECT sum(sd.totalPoints) FROM SkillDef sd WHERE sd.projectId = ?1 and sd.skillId in ?2''')
    Integer getTotalPointsSumForSkills(String projectId, List<String> skillId)

    @Nullable
    @Query('''SELECT pd.projectId as projectId, pd.name as name, ld.level as level from ProjDef pd 
                join LevelDef ld on ld.projectRefId = pd.id and ld.skillRefId is null and ld.pointsFrom > pd.totalPoints''')
    List<UnachievableProjectLevel> findUnachievableProjectLevels()

    @Nullable
    @Query('''select pd.projectId as projectId, 
                    pd.name as projectName, 
                    sd.skillId as subjectId, 
                    sd.name as name,
                    ld.level as level 
            from ProjDef pd 
            join SkillDef sd on sd.type = 'Subject' and sd.projRefId = pd.id
            join LevelDef ld on ld.skillRefId = sd.id and ld.pointsFrom > sd.totalPoints''')
    List<UnachievableSubjectLevel> findUnachievableSubjectLevels()

    @Modifying
    @Transactional
    @Query('''update SkillDef set displayOrder=?2 where skillId=?1''')
    void setSkillDisplayOrder(String skillId, Integer displayOrder)

    static interface SkillPreviewItemDbRes {
        String getSkillId()
        String getSkillName()
        String getSubjectName()
        String getSubjectId()
        Integer getPointIncrement()
        Integer getTotalPoints()

        Integer getUserCurrentPoints()
        Boolean getUserAchieved()
    }

    @Query(value = '''select count(skill.id)
                    from skill_definition skill
                    where skill.enabled = 'true'
                      and skill.type = 'Skill'
                      and skill.project_id = :projectId
    ''', nativeQuery = true)
    long countSkillPreviewItems(@Param('projectId') String projectId)

    @Query(value = '''select count(skill.id)
                    from skill_definition skill
                    where skill.enabled = 'true'
                      and skill.type = 'Skill'
                      and skill.project_id = :projectId
                      and LOWER(skill.name) LIKE LOWER(CONCAT('%',:query,'%'))
    ''', nativeQuery = true)
    long countSkillPreviewItems(@Param('projectId') String projectId, @Param('query') String query)

    @Query(value = '''select skill.skill_id        as skillId,
                           skill.name            as skillName,
                           subject.name          as subjectName,
                           subject.skill_id      as subjectId,
                           skill.point_increment as pointIncrement,
                           skill.total_points    as totalPoints,
                           case when ua.id is not null then true else false end as userAchieved,
                           case when up.points is not null then up.points else 0 end userCurrentPoints
                    from skill_relationship_definition relationship,
                         skill_definition subject,
                         skill_definition skill
                            left join user_achievement ua on skill.id = ua.skill_ref_id and ua.user_id = :userId
                            left join user_points up on skill.id = up.skill_ref_id and up.user_id = :userId
                    where skill.enabled = 'true'
                      and skill.id = relationship.child_ref_id
                      and subject.id = relationship.parent_ref_id
                      and subject.type = 'Subject'
                      and skill.type = 'Skill'
                      and skill.project_id = :projectId
                      and LOWER(skill.name) LIKE LOWER(CONCAT('%',:query,'%'))
    ''', nativeQuery = true)
    List<SkillPreviewItemDbRes> findSkillPreviewItems(@Param('projectId') String projectId,
                                                      @Param('userId') String userId,
                                                      @Param('query') String query,
                                                      PageRequest pageRequest)
    static interface SkillNameAndSubjectId {
        String getSkillName()
        String getSubjectId()
    }

    @Nullable
    @Query('''select sdChild.name as skillName, sdParent.skillId as subjectId
            from SkillDef sdParent, SkillRelDef srd, SkillDef sdChild
            where 
                srd.parent=sdParent and 
                srd.child=sdChild and 
                srd.parent.type = 'Subject' and
                sdChild.projectId = ?1 and
                sdChild.skillId = ?2''')
    SkillNameAndSubjectId getSkillNameByProjectIdAndSkillId(String projectId, String skillId)

    @Modifying
    @Transactional
    @Query('''update SkillDef set selfReportingType=null where projectId=?1 and skillId=?2 and selfReportingType=?3''')
    int unsetSelfReportTypeByProjectIdSkillIdAndSelfReportType(String projectId, String skillId, SelfReportingType selfReportingType)
}
