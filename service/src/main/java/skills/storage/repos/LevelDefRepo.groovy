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
import skills.storage.model.LevelDef
import skills.storage.model.SkillDef.ContainerType

interface LevelDefRepo extends CrudRepository<LevelDef, Integer>{

    List<LevelDef> findAllByProjectRefId(Integer projectId)

    static interface SubjectLevelCount {
        String getSubject()
        Long getNumberLevels()
    }

    @Query('''select sd.name as subject,  count(ld.id) as numberLevels 
            from LevelDef as ld, SkillDef as sd 
            where 
                ld.skillRefId = sd.id and
                sd.projectId = ?1 and
                sd.type = 'Subject'
            group by sd.skillId    
           ''')
    List<SubjectLevelCount> countNumLevelsPerSubject(String projectId)


}
