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
package skills.intTests.adminDisplayOrder

import org.springframework.http.HttpStatus
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService

class GlobalBadgeOrderSpecs extends DefaultIntSpec {

    List badges
    def setup() {
        int numBadges = 5
        badges = (1..numBadges).collect {
            def badge = SkillsFactory.createBadge(1, it)
            skillsService.createGlobalBadge(badge)
            return badge
        }
    }

    def "move badge down"() {
        when:
        def beforeMove = skillsService.getAllGlobalBadges()
        skillsService.changeGlobalBadgeDisplayOrder(badges.first(), 1)
        def afterMove = skillsService.getAllGlobalBadges()
        then:
        beforeMove.collect({it.badgeId}) == ["badge1", "badge2", "badge3", "badge4", "badge5"]
        afterMove.collect({it.badgeId}) == ["badge2", "badge1", "badge3", "badge4", "badge5"]
    }

    def "move badge up"() {
        when:
        def beforeMove = skillsService.getAllGlobalBadges()
        skillsService.changeGlobalBadgeDisplayOrder(badges.get(1), 0)
        def afterMove = skillsService.getAllGlobalBadges()
        then:
        beforeMove.collect({it.badgeId}) == ["badge1", "badge2", "badge3", "badge4", "badge5"]
        afterMove.collect({it.badgeId}) == ["badge2", "badge1", "badge3", "badge4", "badge5"]
    }

    def "sequence of badge display order operations"() {
        when:
        def beforeMove = skillsService.getAllGlobalBadges()
        skillsService.changeGlobalBadgeDisplayOrder(badges.get(0), 3)
        def move1 = skillsService.getAllGlobalBadges()

        skillsService.changeGlobalBadgeDisplayOrder(badges.get(4), 0)
        def move2 = skillsService.getAllGlobalBadges()

        skillsService.changeGlobalBadgeDisplayOrder(badges.get(1), 4)
        def move3 = skillsService.getAllGlobalBadges()

        skillsService.changeGlobalBadgeDisplayOrder(badges.get(2), 2)
        def move4 = skillsService.getAllGlobalBadges()
        then:
        beforeMove.collect({it.badgeId}) == ["badge1", "badge2", "badge3", "badge4", "badge5"]
        move1.collect({it.badgeId}) == ["badge2", "badge3", "badge4", "badge1", "badge5"]
        move2.collect({it.badgeId}) == ["badge5", "badge2", "badge3", "badge4", "badge1"]
        move3.collect({it.badgeId}) == ["badge5", "badge3", "badge4", "badge1", "badge2"]
        move4.collect({it.badgeId}) == ["badge5", "badge4", "badge3", "badge1", "badge2"]
    }

    def "move badge to out of the max bound - should be placed last"() {
        when:
        def beforeMove = skillsService.getAllGlobalBadges()
        skillsService.changeGlobalBadgeDisplayOrder(badges.get(0), 10)
        def afterMove = skillsService.getAllGlobalBadges()
        then:
        beforeMove.collect({it.badgeId}) == ["badge1", "badge2", "badge3", "badge4", "badge5"]
        afterMove.collect({it.badgeId}) == ["badge2", "badge3", "badge4", "badge5", "badge1"]
    }

    def "new display index must be >=0 "() {
        when:
        skillsService.changeGlobalBadgeDisplayOrder(badges.get(2), 0)
        def afterMove = skillsService.getAllGlobalBadges()
        skillsService.changeGlobalBadgeDisplayOrder(badges.get(2), -1)
        then:
        SkillsClientException exception = thrown()
        exception.httpStatus == HttpStatus.BAD_REQUEST
        exception.message.contains('[newDisplayOrderIndex] param must be >=0 but received [-1]')

        afterMove.collect({it.badgeId}) == ["badge3", "badge1", "badge2", "badge4", "badge5"]

    }

}
