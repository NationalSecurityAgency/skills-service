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

import javax.persistence.*

@Entity
@Table(name="user_events")
@ToString(includeNames = true)
class UserEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id

    // fk to project_definition
    String projectId

    // fk to users
    String userId

    // fk to SkillDef
    Integer skillRefId

    Date eventTime

    Integer count

    @Enumerated(EnumType.STRING)
    EventType eventType

    Integer weekNumber
}
