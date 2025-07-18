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
import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.*
import org.hibernate.annotations.Type
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener

@ToString(includeNames = true)
@Entity()
@Table(name='skill_attributes_definition')
@EntityListeners(AuditingEntityListener)
class SkillAttributesDef {

    static enum SkillAttributesType {
        Video, BonusAward, AchievementExpiration, Slides
    };

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id

    Integer skillRefId

    @Enumerated(EnumType.STRING)
    SkillAttributesType type

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    String attributes

    @Temporal(TemporalType.TIMESTAMP)
    @CreatedDate
    Date created

    @Temporal(TemporalType.TIMESTAMP)
    @LastModifiedDate
    Date updated
}
