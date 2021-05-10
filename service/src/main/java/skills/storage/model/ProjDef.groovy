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
import org.hibernate.annotations.Where

import javax.persistence.*

@Entity
@Table(name = 'project_definition')
@ToString(includeNames = true, excludes = ['subjects', 'badges', 'customIcons'])
class ProjDef implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id

    String projectId

    String name

    String clientSecret

    // need to be denormalized so levels can be efficiently calculated (without the need of loading all of the rules)
    int totalPoints

    @OneToMany(fetch = FetchType.LAZY, cascade = [])
    @JoinColumn(name="projRefId", insertable = false, updatable = false)
    @Where(clause = "type = 'Subject'")
    List<SkillDef> subjects

    @OneToMany(fetch = FetchType.LAZY, cascade = [])
    @JoinColumn(name="projRefId", insertable = false, updatable = false)
    @Where(clause = "type = 'Badge'")
    List<SkillDef> badges

    @OneToMany(fetch = FetchType.LAZY, cascade = [])
    @JoinColumn(name="projRefId", insertable = false, updatable = false)
    List<CustomIcon> customIcons

    @Column(name="created", updatable = false, insertable = false)
    Date created
}
