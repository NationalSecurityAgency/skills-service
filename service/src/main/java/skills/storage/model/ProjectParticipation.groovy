/**
 * Copyright 2026 SkillTree
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
package skills.storage.model

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener

@Entity
@Table(name = 'project_participation')
@EntityListeners(AuditingEntityListener)
class ProjectParticipation implements Serializable {
    enum BackfillStatus { NOT_REQUESTED, NOT_NEEDED, PENDING, COMPLETED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id
    String projectId
    String userId
    @Enumerated(EnumType.STRING)
    BackfillStatus backfillStatus = BackfillStatus.NOT_REQUESTED
    @CreatedDate
    @Column(updatable = false)
    Date created
    @LastModifiedDate
    Date updated
}
