/**
 * Copyright 2021 SkillTree
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
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.repository.query.Param
import org.springframework.lang.Nullable
import skills.storage.CatalogSkill
import skills.storage.model.ExportedSkill
import skills.storage.model.ExportedSkillTiny
import skills.storage.model.ImportExportStats
import skills.storage.model.SkillDefWithExtra

interface ExportedSkillRepo extends CrudRepository<ExportedSkill, Integer>, PagingAndSortingRepository<ExportedSkill, Integer> {

    @Nullable
    @Query('''select 'true' from ExportedSkill es where es.projectId = ?1 and es.skill.skillId = ?2''')
    Boolean doesSkillExistInCatalog(String projectId, String skillId)

    @Nullable
    @Query('''select es.skill.skillId from ExportedSkill es where es.projectId = ?1 and es.skill.skillId in ?2''')
    List<String> doSkillsExistInCatalog(String projectId, List<String> skillIds)

    @Nullable
    @Query('''select 'true' from ExportedSkill es where es.skill.skillId = ?1''')
    Boolean doesSkillIdExistInCatalog(String skillId)

    @Nullable
    @Query('''select 'true' from ExportedSkill es where lower(es.skill.name) = lower(?1)''')
    Boolean doesSkillNameExistInCatalog(String skillId)

    @Nullable
    @Query('''select es from ExportedSkill es where es.projectId = :projectId and es.skill.skillId = :skillId and es.skill.projectId = :projectId''')
    ExportedSkill getCatalogSkill(@Param("projectId") String projectId, @Param("skillId") String skillId)


    @Nullable
    @Query('''select es.skill as skill, 
                subject.name as subjectName, 
                subject.skillId as subjectId,
                project.name as projectName,
                es.created as exportedOn,
                case when localSkillOnId.skillId is not null then true else false end as skillIdAlreadyExist,
                case when localSkillOnName.name is not null then true else false end as skillNameAlreadyExist  
        from ExportedSkill es
        join ProjDef project on project.projectId = es.projectId
        join SkillRelDef srd on srd.child.id = es.skill.id and srd.type in ('RuleSetDefinition', 'GroupSkillToSubject')
        join SkillDef subject on subject = srd.parent and subject.type = 'Subject'
        left join SkillDef localSkillOnId on (lower(localSkillOnId.skillId) = lower(srd.child.skillId) and localSkillOnId.projectId = ?1)
        left join SkillDef localSkillOnName on (lower(localSkillOnName.name) = lower(srd.child.name) and localSkillOnName.projectId = ?1)
        where 
             es.projectId <> ?1 and
             not exists (select 1 from SkillDef sd where sd.projectId = ?1 and sd.copiedFrom = es.skill.id)
    ''')
    List<CatalogSkill> getSkillsInCatalog(String projectId, Pageable pageable)

    @Nullable
    @Query('''select count(es) 
        from ExportedSkill es
        join ProjDef project on project.projectId = es.projectId
        join SkillRelDef srd on srd.child.id = es.skill.id and srd.type in ('RuleSetDefinition', 'GroupSkillToSubject')
        join SkillDef subject on subject = srd.parent and subject.type = 'Subject'
        where 
             es.projectId <> ?1 and
             not exists (select 1 from SkillDef sd where sd.projectId = ?1 and sd.copiedFrom = es.skill.id)
    ''')
    Integer countSkillsInCatalog(String projectId)

    @Nullable
    @Query('''
        select es.skill as skill, 
                subject.name as subjectName, 
                subject.skillId as subjectId,
                project.name as projectName,
                es.created as exportedOn,
                case when localSkillOnId.skillId is not null then true else false end as skillIdAlreadyExist,
                case when localSkillOnName.name is not null then true else false end as skillNameAlreadyExist   
        from ExportedSkill es
        join ProjDef project on project.projectId = es.projectId
        join SkillRelDef srd on srd.child.id = es.skill.id and srd.type in ('RuleSetDefinition', 'GroupSkillToSubject')
        join SkillDef subject on subject = srd.parent and subject.type = 'Subject'
        left join SkillDef localSkillOnId on (lower(localSkillOnId.skillId) = lower(srd.child.skillId) and localSkillOnId.projectId = :projectId)
        left join SkillDef localSkillOnName on (lower(localSkillOnName.name) = lower(srd.child.name) and localSkillOnName.projectId = :projectId)
        where
            es.projectId <> :projectId and 
            not exists (select 1 from SkillDef sd where sd.projectId = :projectId and sd.copiedFrom = es.skill.id) and
            lower(es.skill.name) like lower(concat('%', :skillSearch, '%'))  and 
            lower(project.name) like lower(concat('%', :projectSearch, '%')) and 
            lower(subject.name) like lower(concat('%', :subjectSearch, '%'))
    ''')
    List<CatalogSkill> getSkillsInCatalog(@Param("projectId") String projectId,
                                          @Param("projectSearch")String projectSearch,
                                          @Param("subjectSearch") String subjectSearch,
                                          @Param("skillSearch")String skillSearch,
                                          Pageable pageable)

    @Nullable
    @Query('''
        select count(es.skill)
        from ExportedSkill es
        join ProjDef project on project.projectId = es.projectId
        join SkillRelDef srd on srd.child.id = es.skill.id and srd.type in ('RuleSetDefinition', 'GroupSkillToSubject')
        join SkillDef subject on subject = srd.parent and subject.type = 'Subject'
        where
            es.projectId <> :projectId and 
            not exists (select 1 from SkillDef sd where sd.projectId = :projectId and sd.copiedFrom = es.skill.id) and
            lower(es.skill.name) like lower(concat('%', :skillSearch, '%'))  and 
            lower(project.name) like lower(concat('%', :projectSearch, '%')) and 
            lower(subject.name) like lower(concat('%', :subjectSearch, '%'))
    ''')
    Integer countSkillsInCatalog(@Param("projectId") String projectId,
                                 @Param("projectSearch")String projectSearch,
                                 @Param("subjectSearch") String subjectSearch,
                                 @Param("skillSearch")String skillSearch)

    @Nullable
    @Query('''select es.skill from ExportedSkill es where es.projectId = ?1''')
    List<SkillDefWithExtra> getSkillsExportedByProject(String projectId, Pageable pageable)

    @Nullable
    @Query(value= """
        select skill.skill_id   as skillId,
               skill.name       as skillName,
               es.created       as exportedOn,
               subject.name     as subjectName,
               subject.skill_id as subjectId,
               MAX(group_def.name)   as groupName,
               (count(distinct imported_skills.project_id)) as importedProjectCount
        from skill_relationship_definition srd,
             skill_definition subject,
             skill_definition skill LEFT JOIN skill_definition group_def on (skill.group_id = group_def.skill_id and group_def.project_id = ?1),
             exported_skills es LEFT JOIN skill_definition imported_skills on (es.skill_ref_id = imported_skills.copied_from_skill_ref and imported_skills.skill_id not like '%STREUSESKILLST%')
        where es.exported_from_project_id = ?1
          and skill.project_id = ?1
          and es.skill_ref_id = skill.id
          and skill.id = srd.child_ref_id
          and subject.id = srd.parent_ref_id
          and srd.type in ('RuleSetDefinition', 'GroupSkillToSubject')
          and subject.type = 'Subject'
          and skill.type = 'Skill'
        group by skillId, skillName, exportedOn, subjectName, subjectId
    """, nativeQuery=true)
    List<ExportedSkillTiny> getTinySkillsExportedByProject(String projectId, Pageable pageable)

    @Query('''select count(es.id) from ExportedSkill es where es.projectId = ?1''')
    Integer countSkillsExportedByProject(String projectId)

    @Nullable
    @Query('''
            select count(es.skill) as numberOfSkills, 
                count (distinct sd.projectId) as numberOfProjects
            from ExportedSkill es
            left join SkillDef sd on sd.copiedFromProjectId = es.projectId 
            where es.projectId = ?1
    ''')
    ImportExportStats getExportedSkillStats(String projectId)

}
