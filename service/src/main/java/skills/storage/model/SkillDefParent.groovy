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
import skills.storage.converters.BooleanConverter

import javax.persistence.*

@CompileStatic
@MappedSuperclass
class SkillDefParent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id

    String skillId

    String projectId

    String name

    int pointIncrement
    int pointIncrementInterval
    @Column(name = 'increment_interval_max_occurrences')
    int numMaxOccurrencesIncrementInterval
    int totalPoints

    int version

    String iconClass

    @Enumerated(EnumType.STRING)
    SkillDef.ContainerType type = SkillDef.ContainerType.Skill

    int displayOrder

    @Enumerated(EnumType.STRING)
    SkillDef.SelfReportingType selfReportingType

    @Temporal(TemporalType.TIMESTAMP)
    Date startDate // optional, used for "gem" badges only currently

    @Temporal(TemporalType.TIMESTAMP)
    Date endDate  // optional, used for "gem" badges only currently

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name="skillRefId", insertable = false, updatable = false)
    List<LevelDef> levelDefinitions

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="projRefId")
    ProjDef projDef

    @Temporal(TemporalType.TIMESTAMP)
    @CreatedDate
    Date created

    @Temporal(TemporalType.TIMESTAMP)
    @LastModifiedDate
    Date updated

    String enabled

    // only applies when type == SkillsGroup
    int numSkillsRequired = -1
    String groupId

    @Convert(converter= BooleanConverter)
    @Column(name="read_only")
    Boolean readOnly

    @Column(name="copied_from_skill_ref")
    Integer copiedFrom

    @Column(name="copied_from_project_id")
    String copiedFromProjectId
}
