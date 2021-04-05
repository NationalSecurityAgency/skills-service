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
package skills.controller.request.model

import spock.lang.Specification

class GlobalSettingsRequestSpec extends Specification {

    def "toString() must not include isEnabled"() {
        GlobalSettingsRequest input = new GlobalSettingsRequest(settingGroup: "group", setting: "setting", value: "value")

        when:
        String res = input.toString()
        println res
        then:
        !res.contains("false")
        !res.contains("enabled")
    }

    def "no exceptions when isEnabled() is called with value=null"() {
        when:
        GlobalSettingsRequest input = new GlobalSettingsRequest(settingGroup: "group", setting: "setting", value: null)
        then:
        !input.isEnabled()
    }

    def "isEnabled logic"() {
        expect:
        new GlobalSettingsRequest(settingGroup: "group", setting: "setting", value: input).isEnabled() == res
        where:
        input      | res
        null       | false
        false      | false
        true       | true
        "enabled"  | true
        "enabled"  | true
        "true"     | true
        "on"       | true
        "disabled" | false
        "off"      | false
        "disable"  | false
    }
}
