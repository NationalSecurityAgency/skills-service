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

import groovy.transform.CompileStatic
import groovy.transform.ToString
import io.hypersistence.utils.hibernate.type.json.JsonType
import org.hibernate.annotations.Type
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType

@Entity
@Table(name = 'quiz_answer_definition')
@EntityListeners(AuditingEntityListener)
@CompileStatic
@ToString(includeNames = true)
class QuizAnswerDef {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id

    String quizId
    Integer questionRefId

    String answer
    String isCorrectAnswer

    Integer displayOrder

    @Column(name="created", updatable = false, insertable = false)
    Date created

    @Temporal(TemporalType.TIMESTAMP)
    @LastModifiedDate
    Date updated

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    String multiPartAnswer

}
