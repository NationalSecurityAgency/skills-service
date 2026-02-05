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
import skills.storage.model.SkillDefWithExtra
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
                srd.child.id in (?1) 
                and srd.type in ?3
                and parent.type = ?2
                and srd.parent = parent''')
    List<SkillDef> findParentByChildIdInAndTypes(List<Integer> childId, SkillDef.ContainerType parentType, List<SkillRelDef.RelationshipType> types)

    @Nullable
    @Query('''SELECT parent 
            from SkillRelDef srd, SkillDef parent 
            where 
                srd.child.id=?1
                and parent.type = ?2 
                and srd.type in ?3
                and srd.parent = parent''')
    List<SkillDef> findParentsByChildIdAndParentContainerTypeAndRelationshipTypes(Integer childId, SkillDef.ContainerType parentContainerType, List<SkillRelDef.RelationshipType> types)

    @Nullable
    SkillRelDef findByChildAndParentAndType(SkillDef child, SkillDef parent, SkillRelDef.RelationshipType type)


    @Query('''SELECT child 
            from SkillRelDef srd, SkillDef child 
            where 
                srd.parent.id=?1 
                and srd.type in ?2
                and srd.child = child''')
    List<SkillDef> findChildrenByParent(Integer parentId, List<SkillRelDef.RelationshipType> types)

    static interface ParentChildSkillIds {
        String getParentSkillId()
        String getParentSkillName()
        String getChildSkillId()
        String getChildSkillName()
    }
    @Nullable
    @Query('''SELECT parent.skillId as parentSkillId, parent.name as parentSkillName, child.skillId as childSkillId, child.name as childSkillName
            from SkillRelDef srd, SkillDef child, SkillDef parent
            where 
                srd.parent.projectId = ?1
                and srd.child.projectId = ?1 
                and srd.type = ?2
                and srd.parent = parent
                and srd.child = child''')
    List<ParentChildSkillIds> findParentAndChildrenSkillIdsForProject(String project, SkillRelDef.RelationshipType type)

    @Query('''SELECT sd1.skillId 
        from SkillDef sd1, SkillRelDef srd 
        where sd1 = srd.parent and sd1.type = 'Subject'
              and srd.child.id = ?1''')
    String findSubjectSkillIdByChildId(Integer childId)

    static interface SkillToSubjIds {
        String getSkillId()
        String getSubjectId()
    }
    @Query('''SELECT skill.skillId as skillId, subject.skillId as subjectId 
        from SkillDef subject, SkillRelDef srd, SkillDef skill 
        where subject = srd.parent 
                and subject.type = 'Subject'
                and skill = srd.child
                and skill.projectId = ?1
                and skill.skillId in (?2) ''')
    List<SkillToSubjIds> findAllSubjectIdsByChildSkillId(String projectId, List<String> skillIds)

    @Query(value = '''select count(srd.id) from SkillRelDef srd where srd.child.id=?1 and srd.type='BadgeRequirement' and srd.parent.type = 'GlobalBadge' ''')
    Integer getSkillUsedInGlobalBadgeCount(Integer skillRefId)

    @Query(value = '''select srd.parent.id from SkillRelDef srd where srd.child.id=?1 and srd.type='BadgeRequirement' and srd.parent.type = 'GlobalBadge' ''')
    List<Integer> getGlobalBadgeIdsForSkill(Integer id)

    @Query(value = '''select distinct (srd.parent.id) from SkillRelDef srd where srd.child.id in (?1) and srd.type='BadgeRequirement' and srd.parent.type = 'GlobalBadge' ''')
    List<Integer> getGlobalBadgeIdsForSkills(List<Integer> skillRefIds)

    @Query(value = '''select gbld.skill_ref_id from global_badge_level_definition gbld where gbld.project_id = ?1 ''', nativeQuery = true)
    List<Integer> getGlobalBadgeLevelIdsForSkill(String projectId)

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
        sd2.groupId as groupId, 
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
        sd2.iconClass as iconClass,
        subj1.skillId as subjectSkillId,
        subj1.name as subjectName,
        pd.name as copiedFromProjectName,
        qDef.quizId as quizId,
        qDef.type as quizType,
        qDef.name as quizName,
        group.name as groupName,
        case when es is not null then true else false end as sharedToCatalog
    from SkillRelDef srd
        join SkillDef sd1 on sd1.id = srd.parent.id
        join SkillDef sd2 on sd2.id = srd.child.id
        join SkillDef subj1 on subj1.projectId = sd2.projectId
        join SkillRelDef srd2 on subj1.id = srd2.parent.id and sd2.id = srd2.child.id
        left join SkillDef group on group.skillId = sd2.groupId and group.projectId = sd2.projectId and group.type = 'SkillsGroup' 
        left join ProjDef pd on sd2.copiedFromProjectId = pd.projectId
        left join ExportedSkill es on es.skill.id = sd2.id
        left join QuizToSkillDef qToSkill on qToSkill.skillRefId = (case when sd2.copiedFrom is not null then sd2.copiedFrom else sd2.id end)
        left join QuizDef qDef on qDef.id = qToSkill.quizRefId
    where sd1.projectId=?1 and sd1.skillId=?2 and srd.type=?3 and subj1.type='Subject' and subj1.projectId=?1''')
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
        sd2.iconClass as iconClass,
        pd.name as copiedFromProjectName,
        qDef.quizId as quizId,
        qDef.type as quizType,
        qDef.name as quizName,
        case when es is not null then true else false end as sharedToCatalog,
        exists (select badgeSrd.parent.name from SkillRelDef badgeSrd where badgeSrd.type = 'BadgeRequirement' and sd2.id = badgeSrd.child.id) as hasBadges
    from SkillRelDef srd
        join SkillDef sd1 on sd1.id = srd.parent.id
        join SkillDef sd2 on sd2.id = srd.child.id
        left join ProjDef pd on sd2.copiedFromProjectId = pd.projectId
        left join ExportedSkill es on es.skill.id = sd2.id
        left join QuizToSkillDef qToSkill on qToSkill.skillRefId = (case when sd2.copiedFrom is not null then sd2.copiedFrom else sd2.id end)
        left join QuizDef qDef on qDef.id = qToSkill.quizRefId
    where sd1.projectId=?1 and sd1.skillId=?2 and srd.type in ?3
    ''')
    List<SkillDefPartial> getSkillsWithCatalogStatus(String projectId, String subjectId, List<SkillRelDef.RelationshipType> relationshipTypes)

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
        sd2.iconClass as iconClass,
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

    @Query('''SELECT sd2 
        from SkillDef sd1, SkillDefWithExtra sd2, SkillRelDef srd 
        where sd1 = srd.parent and sd2.id = srd.child.id and srd.type in ?3 
              and sd1.projectId=?1 and sd1.skillId=?2''')
    List<SkillDefWithExtra> getChildrenWithExtraAttrs(@Nullable String projectId, String parentSkillId, List<SkillRelDef.RelationshipType> types)

    @Query('''select count(sd2) from SkillDef sd1, SkillDef sd2, SkillRelDef srd 
        where sd1 = srd.parent and sd2 = srd.child and srd.type in ?3 
              and sd1.projectId=?1 and sd1.skillId=?2''')
    Long countChildren(@Nullable projectId, String parentSkillId, List<SkillRelDef.RelationshipType> types)

    static interface SkillIdAndCount {
        String getSkillId()

        Long getCount()
    }

    @Query('''select count(sd2.id) > 0
        from SkillDef sd1, SkillDef sd2, SkillRelDef srd 
        where sd1 = srd.parent 
              and sd2 = srd.child 
              and srd.type = 'Dependence' 
              and sd1.projectId=?1 
              and sd2.projectId=?2
              and sd2.skillId=?3
              ''')
    Boolean checkIfSkillInAnotherProjectPartOfLearningPath(String projectId, String otherProjectId, String otherProjectSkillId )

    @Query(value = '''
                SELECT EXISTS (
                    SELECT 1
                    FROM skill_definition parent
                    JOIN skill_relationship_definition srd ON parent.id = srd.parent_ref_id
                    JOIN skill_definition child ON srd.child_ref_id = child.id
                    WHERE srd.type = 'BadgeRequirement'
                      AND parent.project_id IS NULL 
                      AND child.project_id = :otherProj
                      AND child.skill_id = :otherProjSkillId
                      AND parent.skill_id IN (
                          SELECT parent_badge.skill_id
                          FROM skill_definition parent_badge
                          JOIN skill_relationship_definition srd_other ON parent_badge.id = srd_other.parent_ref_id
                          JOIN skill_definition other_child ON srd_other.child_ref_id = other_child.id
                          WHERE srd_other.type = 'BadgeRequirement'
                            AND other_child.project_id = :projId
                          
                          UNION
                          
                          SELECT gbld.skill_id
                          FROM global_badge_level_definition gbld
                          WHERE gbld.project_id = :projId
                      )
                )
    ''', nativeQuery = true)
    Boolean checkIfProjectBelongsToGlobalBadgeViaSkillRequirement(
            @Param("projId") projId,
            @Param("otherProj") otherProj,
            @Param("otherProjSkillId") otherProjSkillId)

    @Query('''select sd1.skillId as skillId, count(sd2) as count from SkillDef sd1, SkillDef sd2, SkillRelDef srd 
        where sd1 = srd.parent and sd2 = srd.child and srd.type in ?3 
              and sd1.projectId=?1 and sd1.skillId in (?2) group by sd1.skillId''')
    List<SkillIdAndCount> countChildrenForMultipleSkillIds(@Nullable projectId, List<String> parentSkillIds, List<SkillRelDef.RelationshipType> types)

    @Nullable
    @Query(value = '''select srd.parent from SkillRelDef srd where srd.child.skillId=?1 and srd.type='BadgeRequirement' and srd.parent.type = 'GlobalBadge' ''')
    SkillDef findGlobalBadgeByChildSkillId(String skillId)

    @Nullable
    @Query(value = '''select srd.parent from SkillRelDef srd where srd.child.skillId=?1 and srd.type=?2 and srd.parent.type=?3 ''')
    List<SkillDef> findAllChildrenByChildSkillIdAndRelationshipTypeAndParentType(String skillId, SkillRelDef.RelationshipType relType, SkillDef.ContainerType parentType)

    /**
     * mapping directly to entity is slow, we can save over a second in latency by mapping attributes explicitly
     */
    @Query('''select 
            sd1.id as id,
            sd1.name as name,
            sd1.skillId as skillId,
            CASE WHEN sd1.type != 'Badge' THEN (SELECT subj1.skillId FROM  SkillDef subj1, SkillRelDef subj1Rel WHERE subj1 = subj1Rel.parent and subj1Rel.child = sd1 and subj1Rel.type in ('RuleSetDefinition', 'GroupSkillToSubject')) END as subjectId,
            sd1.projectId as projectId,
            p1.name as projectName,
            sd1.pointIncrement as pointIncrement,
            sd1.totalPoints as totalPoints,
            sd1.type as skillType,
        
            sd2.id as id2,
            sd2.name as name2,
            sd2.skillId as skillId2,
            CASE WHEN sd2.type != 'Badge' THEN (SELECT subj2.skillId FROM  SkillDef subj2, SkillRelDef subj2Rel WHERE subj2 = subj2Rel.parent and subj2Rel.child = sd2 and subj2Rel.type in ('RuleSetDefinition', 'GroupSkillToSubject')) END as subjectId2,
            sd2.projectId as projectId2,
            p2.name as projectName2,
            sd2.pointIncrement as pointIncrement2,
            sd2.totalPoints as totalPoints2,
            sd2.type as skillType2
        from SkillDef sd1, SkillDef sd2, SkillRelDef srd, ProjDef p1, ProjDef p2
        where sd1 = srd.parent 
            and sd2 = srd.child
            and srd.type=?2 
            and sd1.projectId=?1
            and p1.projectId = sd1.projectId
            and p2.projectId = sd2.projectId
        ''')
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

    @Query(value = '''select count(srd.id) from SkillRelDef srd where srd.type='Tag' and srd.parent.type = 'Tag' and srd.parent.skillId=?1''')
    Integer getSkillWithTagCount(String tagId)

    @Query(value = '''select skillReq.hasGlobalBadge OR levelReq.hasGlobalBadge
                from
                (select count(badge) > 0 as hasGlobalBadge
                from skill_relationship_definition rel,
                     skill_definition badge,
                     skill_definition skill
                where rel.parent_ref_id = badge.id
                  and rel.child_ref_id = skill.id
                  and rel.type = 'BadgeRequirement'
                  and badge.type = 'GlobalBadge'
                  and skill.project_id = ?1) skillReq,
                (select COUNT(*) > 0 as hasGlobalBadge
                 from global_badge_level_definition
                 where project_id = ?1) levelReq''', nativeQuery = true)
    boolean belongsToGlobalBadge(String projectId)

    @Query(value = '''SELECT srd FROM SkillRelDef srd, SkillDef skill, SkillDef relatedSkill
                      WHERE srd.type = 'Dependence' AND
                      srd.child = skill AND
                      srd.parent = relatedSkill AND 
                      skill.projectId = :originalProjectId AND 
                      skill.skillId = :skillId AND 
                      relatedSkill.projectId = :projectId''')
    List<SkillRelDef> findAllDependenciesForSkillIdAndProjectIdForProject(@Param("projectId") String projectId, @Param("skillId") String skillId, @Param("originalProjectId") String originalProjectId)

    @Query(value = '''SELECT srd FROM SkillRelDef srd, SkillDef skill, SkillDef relatedSkill
                      WHERE srd.type = 'Dependence' AND
                      srd.child = skill AND
                      srd.parent = relatedSkill AND
                      skill.projectId = :projectId AND 
                      skill.skillId = :skillId AND 
                      relatedSkill.projectId != :projectId''')
    List<SkillRelDef> findAllDependenciesForSkillIdAndProjectId(@Param("projectId") String projectId, @Param("skillId") String skillId)

    @Query(value = '''
        WITH community_projects AS (
            SELECT project_id 
            FROM settings 
            WHERE setting = 'user_community' 
            AND value = 'true'
        )
        
        SELECT DISTINCT gbld.project_id 
        FROM global_badge_level_definition gbld
        LEFT JOIN community_projects cp ON gbld.project_id = cp.project_id
        WHERE gbld.skill_ref_id = :skillRefId
          AND cp.project_id IS NULL
        
        UNION
        
        SELECT DISTINCT skill.project_id 
        FROM skill_relationship_definition rel
        JOIN skill_definition badge ON rel.parent_ref_id = badge.id
        JOIN skill_definition skill ON rel.child_ref_id = skill.id
        LEFT JOIN community_projects cp ON skill.project_id = cp.project_id
        WHERE rel.type = 'BadgeRequirement'
          AND badge.type = 'GlobalBadge'
          AND badge.id = :skillRefId
          AND cp.project_id IS NULL
''', nativeQuery = true)
    List<String> getNonCommunityProjectsThatThisGlobalBadgeIsLinkedTo(Integer skillRefId)

    @Query(value = '''
        WITH community_badges AS (
            SELECT skill_ref_id 
            FROM settings 
            WHERE setting = 'user_community' 
            AND value = 'true'
        )
        
        SELECT DISTINCT gbld.skill_ref_id 
        FROM global_badge_level_definition gbld
        LEFT JOIN community_badges cb ON gbld.skill_ref_id = cb.skill_ref_id
        WHERE gbld.project_id = :projectId
          AND cb.skill_ref_id IS NULL
        
        UNION
        
        SELECT DISTINCT badge.id 
        FROM skill_relationship_definition rel
        JOIN skill_definition badge ON rel.parent_ref_id = badge.id
        JOIN skill_definition skill ON rel.child_ref_id = skill.id
        LEFT JOIN community_badges cb ON badge.id = cb.skill_ref_id
        WHERE rel.type = 'BadgeRequirement'
          AND badge.type = 'GlobalBadge'
          AND skill.project_id = :projectId
          AND cb.skill_ref_id IS NULL
''', nativeQuery = true)
    List<Integer> getNonCommunityGlobalBadgesThatThisProjectIsLinkedTo(String projectId)

}
