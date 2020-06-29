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

import org.springframework.data.repository.CrudRepository
import org.springframework.lang.Nullable
import skills.storage.model.CustomIcon

import org.springframework.transaction.annotation.Transactional

interface CustomIconRepo extends CrudRepository<CustomIcon, Integer> {

    //TODO: add method that loads custom icons without loading the binary column
    //generating css doesn't need to load that and it could make performance a bit better
    @Transactional(readOnly = true)
    List<CustomIcon> findAllByProjectId(String projectId)

    @Transactional(readOnly = true)
    List<CustomIcon> findAllByProjectIdIsNull()

    CustomIcon findByProjectIdAndFilename(String projectId, String filename)

    void delete(CustomIcon toDelete)

    @Transactional
    void deleteByProjectIdAndFilename(@Nullable String projectId, String filename)
}
