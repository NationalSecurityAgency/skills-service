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
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = 'settings')
@ToString(includeNames = true)
class Setting {

    static enum SettingType { User, Project, Global, UserProject }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id

    //nullable
    String settingGroup

    //nullable
    String projectId

    //non-null
    String setting

    //non-null
    String value

    //nullable
    Integer userRefId

    // reliably differentiate between different setting types
    @Enumerated(EnumType.STRING)
    SettingType type

    //convenience method for settings that are in an either on or off state as opposed to containing a meaningful value
    boolean isEnabled(){
        return Boolean.valueOf(value) || value.toLowerCase() == "enabled" || value.toLowerCase() == "enable" || value.toLowerCase() == "on"
    }
}
