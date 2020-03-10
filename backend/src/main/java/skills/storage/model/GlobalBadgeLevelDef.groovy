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
@Table(name='global_badge_level_definition')
@ToString(includeNames = true)
class GlobalBadgeLevelDef implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id

    @Column(name="level_ref_id")
    Integer levelRefId

    Integer level

    @Column(name="proj_ref_id")
    Integer projectRefId

    @Column(name="project_id")
    String projectId

    @Column(name="project_name")
    String projectName

    @Column(name="skill_ref_id")
    Integer badgeRefId

    @Column(name="skill_id")
    String badgeId

}
