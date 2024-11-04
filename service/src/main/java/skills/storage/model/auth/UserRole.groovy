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
package skills.storage.model.auth

import groovy.transform.Canonical
import groovy.transform.ToString

import jakarta.persistence.*

@ToString(includeNames = true)
@Entity
@Table(name = 'user_roles')
@Canonical
class UserRole implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id

    Integer userRefId
    String userId

    String projectId
    String quizId
    String adminGroupId

    @Enumerated(EnumType.STRING)
    RoleName roleName
}
