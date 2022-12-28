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

import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import javax.persistence.Query

@Service
@Slf4j
class AttachmentRepo {

    @PersistenceContext
    EntityManager entityManager;

    void saveAttachment(String filename, String contentType, String uuid, Long size, InputStream is) {
        Attachment attachment = new Attachment(filename: filename, contentType: contentType, uuid: uuid, size: size)
        attachment.setContent(BlobProxy.generateProxy(is, size))
        entityManager.persist(attachment)
    }

    Attachment getAttachmentByUuidAndFilename(String uuid, String filename) {
        String query = "SELECT a from Attachment a where a.uuid = :uuid AND a.filename = :filename"
        Query getAttachment = entityManager.createQuery(query, Attachment)
        getAttachment.setParameter('uuid', uuid)
        getAttachment.setParameter('filename', filename)
        return getAttachment.getSingleResult()
    }
}
