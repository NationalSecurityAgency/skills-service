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
import static skills.intTests.utils.SkillsFactory.createProject

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

        when:
        def defaultResValid = skillsService.checkCustomDescriptionValidation(notValidProtectedCommunity)
        def defaultResInValid = skillsService.checkCustomDescriptionValidation(notValidDefault)

        then:
        defaultResValid.body.valid
        !defaultResInValid.body.valid
        defaultResInValid.body.msg == notValidDefaultErrMsg
    }

    def "description validator for community with useProtectedCommunityValidator"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def p1 = createProject(1)
        p1.enableProtectedUserCommunity = true
        pristineDragonsUser.createProject(p1)

        when:
        def communityValid = pristineDragonsUser.checkCustomDescriptionValidation(notValidDefault, null, true, null)
        def communityInvalidValid = pristineDragonsUser.checkCustomDescriptionValidation(notValidProtectedCommunity, null, true, null)

        def defaultResValid = pristineDragonsUser.checkCustomDescriptionValidation(notValidProtectedCommunity, null, false, null)
        def defaultResInvalid = pristineDragonsUser.checkCustomDescriptionValidation(notValidDefault, null, false, null)
        then:
        communityValid.body.valid
        !communityInvalidValid.body.valid
        communityInvalidValid.body.msg == notValidProtectedCommunityErrMsg

        defaultResValid.body.valid
        !defaultResInvalid.body.valid
        defaultResInvalid.body.msg == notValidDefaultErrMsg
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
