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
import org.springframework.lang.Nullable
import skills.storage.model.ProjDefWithDescription

import java.util.stream.Stream

interface ProjDefWithDescriptionRepo extends CrudRepository<ProjDefWithDescription, Long> {

    @Nullable
    ProjDefWithDescription findByProjectIdIgnoreCase(String projectId)

    @Nullable
    @Query("select p.description from ProjDefWithDescription p where p.projectId = ?1" )
    String getDescriptionByProjectId(String projectId)

    @Query('''SELECT s FROM ProjDefWithDescription s''')
    Stream<ProjDefWithDescription> streamAll()
}
