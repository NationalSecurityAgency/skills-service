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
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate

import jakarta.persistence.*

@CompileStatic
@MappedSuperclass
class QuizDefParent implements Serializable {

    static enum QuizType {
        Quiz, Survey,
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id

    String quizId

    String name

    @Enumerated(EnumType.STRING)
    QuizType type

    @Temporal(TemporalType.TIMESTAMP)
    @CreatedDate
    @Column(name="created", updatable = false, insertable = false)
    Date created

    @Temporal(TemporalType.TIMESTAMP)
    @LastModifiedDate
    Date updated

}
