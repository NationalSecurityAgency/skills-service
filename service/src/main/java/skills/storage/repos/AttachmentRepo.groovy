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


import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.lang.Nullable
import skills.storage.model.Attachment

interface AttachmentRepo extends CrudRepository<Attachment, Integer> {

    @Nullable
    Attachment findByUuid(String uuid)

    @Modifying
    Integer deleteByUuid(String uuid)

    @Modifying
    Integer deleteBySkillIdAndProjectIdIsNull(String skillId)

    @Query(value = '''SELECT lo_get(CAST(content AS oid), :start, CAST(:length AS integer)) 
            FROM attachments 
            WHERE uuid = :uuid''', nativeQuery = true)
    byte[] fetchFileChunk(@Param("uuid") String uuid,
                          @Param("start") long start,
                          @Param("length") long length)

}
