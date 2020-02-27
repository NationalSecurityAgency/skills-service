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

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity
@Table(name='level_definition')
@ToString(includeNames = true)
class LevelDef implements Serializable, LevelDefInterface{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id

    int level
    Integer percent

    // either belongs to a project or to skill
    Integer projectRefId
    Integer skillRefId

    Integer pointsFrom
    Integer pointsTo

    @Column(name="logical_name")
    String name

    String iconClass
}
