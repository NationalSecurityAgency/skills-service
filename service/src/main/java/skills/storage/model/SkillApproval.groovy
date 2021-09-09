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
package skills.storage.model

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener

import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table
import groovy.transform.ToString

import javax.persistence.Temporal
import javax.persistence.TemporalType

@Entity
@Table(name="skill_approval")
@ToString(includeNames = true)
@EntityListeners(AuditingEntityListener)
class SkillApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id

    String projectId

    String userId

    Integer skillRefId

    Date requestedOn
    String requestMsg

    Date rejectedOn
    String rejectionMsg
    Date rejectionAcknowledgedOn

    String approverUserId
    Date approverActionTakenOn

    @Temporal(TemporalType.TIMESTAMP)
    @CreatedDate
    Date created

    @Temporal(TemporalType.TIMESTAMP)
    @LastModifiedDate
    Date updated

}
