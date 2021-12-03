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
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.lang.Nullable
import skills.storage.model.ExportedSkill
import skills.storage.model.ExportedSkillTiny
import skills.storage.model.ImportExportStats
import skills.storage.model.SkillDef
import skills.storage.model.SubjectAwareSkillDef

interface ExportedSkillRepo extends PagingAndSortingRepository<ExportedSkill, Integer> {

    @Nullable
    @Query('''select 'true' from ExportedSkill es where es.projectId = ?1 and es.skill.skillId = ?2''')
    Boolean doesSkillExistInCatalog(String projectId, String skillId)

    @Nullable
    @Query('''select es.skill as skill, 
                subject.name as subjectName, 
                subject.skillId as subjectId,
                project.name as projectName 
        from ExportedSkill es, SkillRelDef srd, SkillDef subject
        join ProjDef project on project.projectId = es.projectId
        where subject = srd.parent and
             srd.type = 'RuleSetDefinition' and
             subject.type = 'Subject' and 
             srd.child = es.skill
    ''')
    List<SubjectAwareSkillDef> getSkillsInCatalog(Pageable pageable)

    @Nullable
    @Query('''
        select es.skill as skill, 
                subject.name as subjectName, 
                subject.skillId as subjectId,
                project.name as projectName 
        from ExportedSkill es, SkillRelDef srd, SkillDef subject
        join ProjDef project on project.projectId = es.projectId
        where lower(es.skill.name) like lower(concat('%', ?1, '%'))
        and subject = srd.parent and
             srd.type = 'RuleSetDefinition' and
             subject.type = 'Subject' and 
             srd.child = es.skill
    ''')
    List<SubjectAwareSkillDef> getSkillsInCatalog(String search, Pageable pageable)

    @Nullable
    @Query('''select es.skill from ExportedSkill es where es.projectId = ?1''')
    List<SkillDef> getSkillsExportedByProject(String projectId, Pageable pageable)

    @Nullable
    @Query('''
            select es.skill.skillId as skillId, 
                 es.skill.name as skillName,
                 es.created as exportedOn,
                 subject.name as subjectName,
                 subject.skillId as subjectId
             from ExportedSkill es, SkillRelDef srd, SkillDef subject 
             where es.projectId = ?1 and 
             subject = srd.parent and
             srd.type = 'RuleSetDefinition' and
             subject.type = 'Subject' and 
             srd.child = es.skill
            ''')
    List<ExportedSkillTiny> getTinySkillsExportedByProject(String projectId, Pageable pageable)

    @Nullable
    @Query('''
            select count(es.skill) as numberOfSkills, 
                count (distinct sd.projectId) as numberOfProjects
            from ExportedSkill es, SkillDef sd 
            where es.projectId = ?1
            and sd.copiedFromProjectId = ?1
    ''')
    ImportExportStats getExportedSkillStats(String projectId)

}
