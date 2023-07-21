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

import groovy.util.logging.Slf4j
import org.hibernate.engine.jdbc.BlobProxy
import org.springframework.stereotype.Service
import skills.storage.model.Attachment

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.Query

@Service
@Slf4j
class AttachmentRepo {

    @PersistenceContext
    EntityManager entityManager;

    void saveAttachment(Attachment attachment, InputStream is) {
        attachment.setContent(BlobProxy.generateProxy(is, attachment.size))
        entityManager.persist(attachment)
    }

    Attachment getAttachmentByUuid(String uuid) {
        String query = "SELECT a from Attachment a where a.uuid = :uuid"
        Query getAttachment = entityManager.createQuery(query, Attachment)
        getAttachment.setParameter('uuid', uuid)
        return getAttachment.getSingleResult()
    }

    Integer deleteBySkillIdAndProjectIdIsNull(String skillId) {
        String query = "DELETE from Attachment a where a.skillId = :skillId and projectId is null"
        Query deleteAttachment = entityManager.createQuery(query)
        deleteAttachment.setParameter('skillId', skillId)
        return deleteAttachment.executeUpdate()
    }
}
