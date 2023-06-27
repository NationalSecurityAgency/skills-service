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
package skills.intTests


import groovy.util.logging.Slf4j
import org.springframework.boot.test.context.SpringBootTest
import skills.SpringBootApp
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsService

@Slf4j
@SpringBootTest(properties = ['skills.config.ui.supportLink1=https://skilltreesupport.com',
        'skills.config.ui.supportLink1Label=Support Center',
        'skills.config.ui.supportLink1Icon=fas fa-ambulance',
        'skills.config.ui.supportLink2=https://coollink.com',
        'skills.config.ui.supportLink2Label=Cool Link',
        'skills.config.ui.supportLink2Icon=fas fa-so-cool',
        'skills.config.ui.supportLink2userCommunityProtected=true',
        'skills.config.ui.supportLink3=https://otherLink.com',
        'skills.config.ui.supportLink3Label=other link',
        'skills.config.ui.supportLink3Icon=fas fa-other',
], webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, classes = SpringBootApp)
class SupportLinkConfigUserCommunityIT extends DefaultIntSpec {

    def "protect support links with user community"() {
        List<String> users = getRandomUsers(2)

        SkillsService allDragonsUser = createService(users[0])
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        when:
        Map allDragonsConfigs = allDragonsUser.getPublicConfigs()
        Map allDragonsSupportLinks = allDragonsConfigs.findAll { it.key.toString().startsWith("supportLink")}

        Map pristineDragonsConfigs = pristineDragonsUser.getPublicConfigs()
        Map pristineDragonsSupportLinks = pristineDragonsConfigs.findAll { it.key.toString().startsWith("supportLink")}
        then:
        allDragonsSupportLinks.size() == 6
        allDragonsSupportLinks["supportLink1"] == "https://skilltreesupport.com"
        allDragonsSupportLinks["supportLink1Label"] == "Support Center"
        allDragonsSupportLinks["supportLink1Icon"] == "fas fa-ambulance"
        allDragonsSupportLinks["supportLink3"] == "https://otherLink.com"
        allDragonsSupportLinks["supportLink3Label"] == "other link"
        allDragonsSupportLinks["supportLink3Icon"] == "fas fa-other"

        !allDragonsConfigs["supportLink2"]
        !allDragonsConfigs["supportLink2Label"]
        !allDragonsConfigs["supportLink2Icon"]

        pristineDragonsSupportLinks.size() == 10
        pristineDragonsSupportLinks["supportLink1"] == "https://skilltreesupport.com"
        pristineDragonsSupportLinks["supportLink1Label"] == "Support Center"
        pristineDragonsSupportLinks["supportLink1Icon"] == "fas fa-ambulance"
        pristineDragonsSupportLinks["supportLink2"] == "https://coollink.com"
        pristineDragonsSupportLinks["supportLink2Label"] == "Cool Link"
        pristineDragonsSupportLinks["supportLink2Icon"] == "fas fa-so-cool"
        pristineDragonsSupportLinks["supportLink2userCommunityProtected"] == "true"
        pristineDragonsSupportLinks["supportLink3"] == "https://otherLink.com"
        pristineDragonsSupportLinks["supportLink3Label"] == "other link"
        pristineDragonsSupportLinks["supportLink3Icon"] == "fas fa-other"
    }
}

