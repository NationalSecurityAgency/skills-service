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
package skills.services.admin.skillReuse

import spock.lang.Specification

class SkillReuseIdUtilSpec extends Specification {

    def "extra reuse counter"() {
        expect:
        SkillReuseIdUtil.extractReuseCounter("cool_skill${SkillReuseIdUtil.REUSE_TAG}0") == 0
        SkillReuseIdUtil.extractReuseCounter("cool_skill${SkillReuseIdUtil.REUSE_TAG}1") == 1
        SkillReuseIdUtil.extractReuseCounter("cool_skill${SkillReuseIdUtil.REUSE_TAG}22") == 22
    }

    def "extra reuse counter: bad format - no number"() {
        when:
        SkillReuseIdUtil.extractReuseCounter("cool_skill${SkillReuseIdUtil.REUSE_TAG}")
        then:
        thrown(IllegalArgumentException)
    }

    def "extra reuse counter: bad format - empty"() {
        when:
        SkillReuseIdUtil.extractReuseCounter("   ")
        then:
        thrown(IllegalArgumentException)
    }

    def "extra reuse counter: bad format - null"() {
        when:
        SkillReuseIdUtil.extractReuseCounter(null)
        then:
        thrown(IllegalArgumentException)
    }

    def "extra reuse counter: bad format - chars and digits"() {
        when:
        SkillReuseIdUtil.extractReuseCounter("cool_skill${SkillReuseIdUtil.REUSE_TAG}1A8")
        then:
        thrown(IllegalArgumentException)
    }

    def "is tagged"() {
        expect:
        SkillReuseIdUtil.isTagged("cool_skill${SkillReuseIdUtil.REUSE_TAG}0")
        SkillReuseIdUtil.isTagged("cool_skill${SkillReuseIdUtil.REUSE_TAG}1")
        SkillReuseIdUtil.isTagged("cool_skill${SkillReuseIdUtil.REUSE_TAG}9")
        SkillReuseIdUtil.isTagged("cool_skill${SkillReuseIdUtil.REUSE_TAG}10")
        SkillReuseIdUtil.isTagged("cool_skill${SkillReuseIdUtil.REUSE_TAG}20")
        !SkillReuseIdUtil.isTagged(null)
        !SkillReuseIdUtil.isTagged("")
        !SkillReuseIdUtil.isTagged("cool_skill${SkillReuseIdUtil.REUSE_TAG}")
        !SkillReuseIdUtil.isTagged("cool_skill${SkillReuseIdUtil.REUSE_TAG.substring(0, 5)}0")
        !SkillReuseIdUtil.isTagged("just_id")
    }

    def "remove tag"() {
        expect:
        SkillReuseIdUtil.removeTag("cool_skill${SkillReuseIdUtil.REUSE_TAG}0") == "cool_skill"
        SkillReuseIdUtil.removeTag("cool_skill${SkillReuseIdUtil.REUSE_TAG}20") == "cool_skill"
        SkillReuseIdUtil.removeTag("cool_skill") == "cool_skill"
    }
}
