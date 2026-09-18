/**
 * Copyright 2024 SkillTree
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
package skills.intTests.community.globalBadge

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService

import static skills.intTests.utils.SkillsFactory.createBadge

class GlobalBadgeDescriptionValidatorCommunitySpecs extends DefaultIntSpec {

    String notValidDefault = "has jabberwocky"
    String notValidDefaultErrMsg = "paragraphs may not contain jabberwocky"
    String notValidProtectedCommunity = "has divinedragon"
    String notValidProtectedCommunityErrMsg = "May not contain divinedragon word"

     def "description validator for community"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

         def b1 = createBadge(1, 1)
         b1.enableProtectedUserCommunity = true
         pristineDragonsUser.createGlobalBadge(b1)

         def b2 = createBadge(1, 2)
         pristineDragonsUser.createGlobalBadge(b2)

        when:
        def communityValid = pristineDragonsUser.checkCustomDescriptionValidation(notValidDefault, null, false, null, b1.badgeId)
        def communityInvalidValid = pristineDragonsUser.checkCustomDescriptionValidation(notValidProtectedCommunity, null, false, null, b1.badgeId)

        def defaultResValid = pristineDragonsUser.checkCustomDescriptionValidation(notValidProtectedCommunity, null, false, null, b2.badgeId)
        def defaultResInvalid = pristineDragonsUser.checkCustomDescriptionValidation(notValidDefault, null, false, null, b2.badgeId)
        then:
        communityValid.body.valid
        !communityInvalidValid.body.valid
        communityInvalidValid.body.msg == notValidProtectedCommunityErrMsg

        defaultResValid.body.valid
        !defaultResInvalid.body.valid
        defaultResInvalid.body.msg == notValidDefaultErrMsg
    }

    def "description validator for community with useProtectedCommunityValidator"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def b1 = createBadge(1, 1)
        b1.enableProtectedUserCommunity = true
        pristineDragonsUser.createGlobalBadge(b1)

        def b2 = createBadge(1,2)
        pristineDragonsUser.createGlobalBadge(b2)

        when:
        def communityValid = pristineDragonsUser.checkCustomDescriptionValidation(notValidDefault, null, true, null, b1.badgeId)
        def communityInvalidValid = pristineDragonsUser.checkCustomDescriptionValidation(notValidProtectedCommunity, null, true, null, b1.badgeId)

        // useProtectedCommunityValidator overrides the gb's config - only for validation purposes
        def overriddenValid = pristineDragonsUser.checkCustomDescriptionValidation(notValidProtectedCommunity, null, true, null, b2.badgeId)
        def overriddenInvalid = pristineDragonsUser.checkCustomDescriptionValidation(notValidDefault, null, true, null, b2.badgeId)
        then:
        communityValid.body.valid
        !communityInvalidValid.body.valid
        communityInvalidValid.body.msg == notValidProtectedCommunityErrMsg

        !overriddenValid.body.valid
        overriddenValid.body.msg == notValidProtectedCommunityErrMsg
        overriddenInvalid.body.valid
    }

    def "global badge paragraph custom validation on create - UC global badge fails"(){
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def badge1 = createBadge(1, 1)
        badge1.enableProtectedUserCommunity = true
        badge1.description = notValidProtectedCommunity


        def badge2 = createBadge(1, 2)
        badge2.enableProtectedUserCommunity = true
        badge2.description = notValidDefault
        pristineDragonsUser.createGlobalBadge(badge2) // this fine
        when:
        pristineDragonsUser.createGlobalBadge(badge1)

        then:
        def exception = thrown(SkillsClientException)
        exception.message.contains(notValidProtectedCommunityErrMsg)
    }

    def "global badge paragraph custom validation on create - non-UC global badge fails"(){
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def badge1 = createBadge(1, 1)
        badge1.description = notValidProtectedCommunity
        pristineDragonsUser.createGlobalBadge(badge1) // this fine

        def badge2 = createBadge(1, 2)
        badge2.description = notValidDefault

        when:
        pristineDragonsUser.createGlobalBadge(badge2)

        then:
        def exception = thrown(SkillsClientException)
        exception.message.contains(notValidDefaultErrMsg)
    }

    def "global badge paragraph custom validation on edit - UC global badge fails"(){
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def badge1 = createBadge(1, 1)
        badge1.enableProtectedUserCommunity = true
        badge1.description = notValidDefault
        pristineDragonsUser.createGlobalBadge(badge1)

        def badge2 = createBadge(1, 2)
        badge2.enableProtectedUserCommunity = true
        badge2.description = "some"
        pristineDragonsUser.createGlobalBadge(badge2)

        badge2.description = notValidDefault
        pristineDragonsUser.updateGlobalBadge(badge2) // this is fine

        when:
        badge1.description = notValidProtectedCommunity
        pristineDragonsUser.updateGlobalBadge(badge1)

        then:
        def exception = thrown(SkillsClientException)
        exception.message.contains(notValidProtectedCommunityErrMsg)
    }

    def "global badge paragraph custom validation on edit - non-UC global badge fails"(){
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def badge1 = createBadge(1, 1)
        badge1.description = "some"
        pristineDragonsUser.createGlobalBadge(badge1) // this fine

        def badge2 = createBadge(1, 2)
        badge2.description = "some"
        pristineDragonsUser.createGlobalBadge(badge2)

        badge1.description = notValidProtectedCommunity
        pristineDragonsUser.updateGlobalBadge(badge1) // good

        when:
        badge2.description = notValidDefault
        pristineDragonsUser.updateGlobalBadge(badge2)

        then:
        def exception = thrown(SkillsClientException)
        exception.message.contains(notValidDefaultErrMsg)
    }
}
