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

import groovy.transform.ToString
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener

import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table
import javax.persistence.Temporal
import javax.persistence.TemporalType

@Entity
@ToString(includeNames = true)
@Table(name = 'user_achievement')
@EntityListeners(AuditingEntityListener)
class UserAchievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id

    String userId

    // denormalize for performance and convenience
    String projectId

    // denormalize for performance and convenience
    // null subject will represent overall points
    String skillId // null will represent overall points for all

    // fk to SkillDef
    // null will represent overall points for all
    Integer skillRefId

    Integer level

    int pointsWhenAchieved

    Date achievedOn

    @Temporal(TemporalType.TIMESTAMP)
    @CreatedDate
    Date created

    @Temporal(TemporalType.TIMESTAMP)
    @LastModifiedDate
    Date updated

    String notified
}
