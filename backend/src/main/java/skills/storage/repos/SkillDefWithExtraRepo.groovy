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
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.lang.Nullable
import skills.storage.model.SkillDef
import skills.storage.model.SkillDef.ContainerType
import skills.storage.model.SkillDefWithExtra
import skills.storage.model.SkillRelDef

interface SkillDefWithExtraRepo extends PagingAndSortingRepository<SkillDefWithExtra, Integer> {

    List<SkillDefWithExtra> findAllByProjectIdAndType(@Nullable String id, SkillDef.ContainerType type)

    @Nullable
    SkillDefWithExtra findByProjectIdAndSkillIdIgnoreCaseAndType(@Nullable String id, String skillId, SkillDef.ContainerType type)

    @Nullable
    SkillDefWithExtra findByProjectIdAndSkillIdAndType(String id, String skillId, SkillDef.ContainerType type)

    static interface SkillDescDBRes {
        String getSkillId()
        String getDescription()
        String getHelpUrl()
    }

    @Query(value='''SELECT c.skillId as skillId, c.description as description, c.helpUrl as helpUrl
        from SkillDefWithExtra s, SkillRelDef r, SkillDefWithExtra c 
        where 
            s.id = r.parent and c.id = r.child and 
            s.projectId=?1 and c.projectId=?1 and
            s.skillId=?2 and r.type=?3 and c.version<=?4''')
    List<SkillDescDBRes> findAllChildSkillsDescriptions(String projectId, String parentSkillId, SkillRelDef.RelationshipType relationshipType, int version)


    @Query(value='''SELECT c.skillId as skillId, c.description as description, c.helpUrl as helpUrl
        from SkillDefWithExtra s, SkillRelDef r, SkillDefWithExtra c 
        where 
            s.id = r.parent and c.id = r.child and 
            s.projectId is null and
            s.skillId=?1 and r.type=?2 and c.version<=?3''')
    List<SkillDescDBRes> findAllGlobalChildSkillsDescriptions(String parentSkillId, SkillRelDef.RelationshipType relationshipType, int version)
}
