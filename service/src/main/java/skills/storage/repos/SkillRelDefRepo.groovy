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

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.lang.Nullable
import skills.storage.model.SkillDef
import skills.storage.model.SkillDefPartial
import skills.storage.model.SkillRelDef
import skills.storage.model.SubjectTotalPoints

interface SkillRelDefRepo extends CrudRepository<SkillRelDef, Integer> {
    List<SkillRelDef> findAllByChildAndType(SkillDef child, SkillRelDef.RelationshipType type)

    @Query('''SELECT srd from SkillRelDef srd where srd.child.id=?1 and srd.type=?2''')
    List<SkillRelDef> findAllByChildIdAndType(Integer childId, SkillRelDef.RelationshipType type)

    @Nullable
    @Query('''SELECT parent 
            from SkillRelDef srd, SkillDef parent 
            where 
                srd.child.id=?1 
                and srd.type in ?2
                and srd.parent = parent''')
    List<SkillDef> findParentByChildIdAndTypes(Integer childId, List<SkillRelDef.RelationshipType> types)

    @Nullable
    @Query('''SELECT parent 
            from SkillRelDef srd, SkillDef parent 
            where 
                srd.child.id=?1
                and parent.type = ?2 
                and srd.type in ?3
                and srd.parent = parent''')
    List<SkillDef> findParentsByChildIdAndParentContainerTypeAndRelationshipTypes(Integer childId, SkillDef.ContainerType parentContainerType, List<SkillRelDef.RelationshipType> types)

    SkillRelDef findByChildAndParentAndType(SkillDef child, SkillDef parent, SkillRelDef.RelationshipType type)

    List<SkillRelDef> findAllByParentAndType(SkillDef parent, SkillRelDef.RelationshipType type)

    @Query('''SELECT child 
            from SkillRelDef srd, SkillDef child 
            where 
                srd.parent.id=?1 
                and srd.type in ?2
                and srd.child = child''')
    List<SkillDef> findChildrenByParent(Integer parentId, List<SkillRelDef.RelationshipType> types)

    @Query('''SELECT sd1.skillId 
        from SkillDef sd1, SkillRelDef srd 
        where sd1 = srd.parent and sd1.type = 'Subject'
              and srd.child.id = ?1''')
    String findSubjectSkillIdByChildId(Integer childId)

    @Query(value = '''select count(srd.id) from SkillRelDef srd where srd.child.skillId=?1 and srd.type='BadgeRequirement' and srd.parent.type = 'GlobalBadge' ''')
    Integer getSkillUsedInGlobalBadgeCount(String skillId)

    @Query(value = '''select count(srd.id) from SkillRelDef srd where srd.type='BadgeRequirement' and srd.parent.type = 'GlobalBadge' and srd.parent.skillId=?1''')
    Integer getGlobalBadgeSkillCount(String badgeId)

    @Query(value = '''select count(srd1.id)
       from SkillRelDef srd1, SkillRelDef srd2
       where srd1.parent.skillId=?1
       and srd1.type='RuleSetDefinition'
       and srd1.child = srd2.child
       and srd1.parent.type = 'Subject'
       and srd2.type = 'BadgeRequirement'
       and srd2.parent.type = 'GlobalBadge' ''')
    Integer getSkillsFromSubjectUsedInGlobalBadgeCount(String skillId)

    @Query(value = '''select count(srd.id) from SkillRelDef srd where srd.child.projectId=?1 and srd.type='BadgeRequirement' and srd.parent.type = 'GlobalBadge' ''')
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
        sd2.updated as updated,
        sd2.selfReportingType as selfReportingType,
        sd2.enabled as enabled,
        sd2.numSkillsRequired as numSkillsRequired,
        sd2.copiedFrom as copiedFrom,
        sd2.readOnly as readOnly,
        sd2.copiedFromProjectId as copiedFromProjectId,
        pd.name as copiedFromProjectName,
        case when es is not null then true else false end as sharedToCatalog
        from SkillDef sd1, SkillDef sd2, SkillRelDef srd
        left join ProjDef pd on sd2.copiedFromProjectId = pd.projectId
        left join ExportedSkill es on es.skill.id = sd2.id
        where sd1 = srd.parent and sd2 = srd.child and srd.type=?3 
              and sd1.projectId=?1 and sd1.skillId=?2''')
    List<SkillDefPartial> getChildrenPartial(String projectId, String parentSkillId, SkillRelDef.RelationshipType type)

    @Query('''select 
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
        sd2.updated as updated,
        sd2.selfReportingType as selfReportingType,
        sd2.enabled as enabled,
        sd2.numSkillsRequired as numSkillsRequired,
        sd2.copiedFrom as copiedFrom,
        sd2.readOnly as readOnly,
        sd2.copiedFromProjectId as copiedFromProjectId,
        pd.name as copiedFromProjectName,
        case when es is not null then true else false end as sharedToCatalog
        from SkillDef sd1, SkillDef sd2, SkillRelDef srd
        left join ProjDef pd on sd2.copiedFromProjectId = pd.projectId
        left join ExportedSkill es on es.skill.id = sd2.id
        where sd1 = srd.parent and sd2 = srd.child and srd.type='RuleSetDefinition' 
              and sd1.projectId=?1 and sd1.skillId=?2
    ''')
    List<SkillDefPartial> getSkillsWithCatalogStatus(String projectId, String subjectId)

    @Query(value='''
        WITH RECURSIVE subj_skills (parentId, childId) AS (
            SELECT s.parent_ref_id AS parentId, s.child_ref_id AS childId 
            FROM
            skill_relationship_definition s, skill_definition sd
            WHERE
            sd.skill_id = :subjectId AND
            sd.project_id = :projectId AND
            s.parent_ref_id = sd.id
            
            UNION ALL
            
            SELECT childId AS parentId, s.child_ref_id AS childId
            FROM
            skill_relationship_definition s
            INNER JOIN subj_skills on childId = s.parent_ref_id
        )
            
        SELECT 
        sd2.id AS id,
        sd2.name AS name, 
        sd2.skill_id AS skillId, 
        sd2.project_id AS projectId, 
        sd2.version AS version,
        sd2.point_increment AS pointIncrement,
        sd2.point_increment_interval AS pointIncrementInterval,
        sd2.increment_interval_max_occurrences AS numMaxOccurrencesIncrementInterval,
        sd2.total_points AS totalPoints,
        sd2.type AS skillType,
        sd2.display_order AS displayOrder,
        sd2.created AS created,
        sd2.updated AS updated,
        sd2.self_reporting_type AS selfReportingType,
        sd2.enabled AS enabled,
        sd2.num_skills_required AS numSkillsRequired,
        sd2.copied_from_skill_ref AS copiedFrom,
        CASE WHEN sd2.read_only = 'true' THEN true ELSE false END AS readOnly,
        sd2.copied_from_project_id AS copiedFromProjectId,
        pd.name AS copiedFromProjectName,
        CASE WHEN es.id IS NOT NULL THEN true ELSE false END AS sharedToCatalog
        FROM skill_definition sd2 
        LEFT JOIN project_definition pd ON sd2.copied_from_project_id = pd.project_id
        LEFT JOIN exported_skills es ON es.skill_ref_id = sd2.id
        WHERE EXISTS (SELECT 1 FROM subj_skills WHERE childId = sd2.id)
    ''', nativeQuery = true)
    List<SkillDefPartial> getSkillsWithCatalogStatusExplodeSkillGroups(@Param("projectId") String projectId, @Param("subjectId") String subjectId)

    /**
     * Counts all skills within a subject, including disabled group skills as well as imported
     * skills still pending finalization
     *
     * @param projectId
     * @param subjectId
     * @return
     */
    @Query(value='''
        WITH RECURSIVE subj_skills (parentId, childId) AS (
            SELECT s.parent_ref_id AS parentId, s.child_ref_id AS childId 
            FROM
            skill_relationship_definition s, skill_definition sd
            WHERE
            sd.project_id = :projectId AND   
            sd.skill_id = :subjectId AND
            s.parent_ref_id = sd.id
            
            UNION ALL
            
            SELECT childId AS parentId, s.child_ref_id AS childId
            FROM
            skill_relationship_definition s
            INNER JOIN subj_skills on childId = s.parent_ref_id
        )
        
        SELECT COUNT(ss.id) FROM
        skill_definition ss WHERE ss.type = 'Skill' AND 
        EXISTS (SELECT 1 FROM subj_skills WHERE childId = ss.id)
    ''', nativeQuery = true)
    int countSubjectSkillsIncDisabled(@Param("projectId") String projectId, @Param("subjectId") String subjectId)

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
        sd2.updated as updated,
        sd2.selfReportingType as selfReportingType
        from SkillDef sd1, SkillDef sd2, SkillRelDef srd 
        where sd1 = srd.parent and sd2 = srd.child and srd.type=?2 
              and sd1.projectId is null and sd1.skillId=?1''')
    List<SkillDefPartial> getGlobalChildrenPartial(String parentSkillId, SkillRelDef.RelationshipType type)

    @Query('''SELECT sd2 
        from SkillDef sd1, SkillDef sd2, SkillRelDef srd 
        where sd1 = srd.parent and sd2 = srd.child and srd.type in ?3 
              and sd1.projectId=?1 and sd1.skillId=?2''')
    List<SkillDef> getChildren(@Nullable String projectId, String parentSkillId, List<SkillRelDef.RelationshipType> types)

    @Query('''select count(sd2) from SkillDef sd1, SkillDef sd2, SkillRelDef srd 
        where sd1 = srd.parent and sd2 = srd.child and srd.type in ?3 
              and sd1.projectId=?1 and sd1.skillId=?2''')
    Long countChildren(@Nullable projectId, String parentSkillId, List<SkillRelDef.RelationshipType> types)

    @Nullable
    @Query(value = '''select srd.parent from SkillRelDef srd where srd.child.skillId=?1 and srd.type='BadgeRequirement' and srd.parent.type = 'GlobalBadge' ''')
    SkillDef findGlobalBadgeByChildSkillId(String skillId)

    @Nullable
    @Query(value= '''select srd.parent from SkillRelDef srd where srd.child.skillId=?1 and srd.type=?2 and srd.parent.type=?3 ''')
    List<SkillDef> findAllChildrenByChildSkillIdAndRelationshipTypeAndParentType(String skillId, SkillRelDef.RelationshipType relType, SkillDef.ContainerType parentType)

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


    @Query(value='''
        WITH RECURSIVE subj_skills (parentId, childId) AS (
            SELECT s.parent_ref_id AS parentId, s.child_ref_id AS childId
            FROM
                skill_relationship_definition s, skill_definition sd
            WHERE
                    sd.project_id = :projectId AND
                    s.parent_ref_id = sd.id
        
            UNION ALL
        
            SELECT childId AS parentId, s.child_ref_id AS childId
            FROM
            skill_relationship_definition s
            INNER JOIN subj_skills on childId = s.parent_ref_id
        ),
        unfinalized_totals (subjectRefId, totalPoints) AS (
           SELECT subject.id AS subjectRefId, SUM(disabledSkill.total_points) AS totalPoints
           FROM
               skill_definition disabledSkill
               LEFT JOIN subj_skills subject_mapping ON disabledSkill.id = subject_mapping.childId
               LEFT JOIN skill_definition subject on subject_mapping.parentId = subject.id
           WHERE disabledSkill.project_id = :projectId AND
               disabledSkill.enabled = 'false' AND
               disabledSkill.type = 'Skill' AND
               disabledSkill.copied_from_project_id IS NOT NULL
           GROUP BY subject.id
        )
        
        SELECT sub.skill_id AS subjectId, sub.name AS name, (COALESCE(totalPoints,0)+sub.total_points) AS totalIncPendingFinalized
        FROM
            skill_definition sub
                LEFT JOIN unfinalized_totals uft on sub.id = uft.subjectRefId
        WHERE sub.project_id = :projectId AND
        sub.type = 'Subject'
    ''', nativeQuery=true)
    List<SubjectTotalPoints> getSubjectTotalPointsIncPendingFinalization(@Param("projectId") String projectId)
}
