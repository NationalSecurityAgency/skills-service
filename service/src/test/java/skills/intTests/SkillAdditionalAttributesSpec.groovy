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


import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.utils.DefaultIntSpec
import skills.services.attributes.BonusAwardAttrs
import skills.services.attributes.SkillAttributeService
import skills.storage.repos.SkillDefRepo

import static skills.intTests.utils.SkillsFactory.*

class SkillAdditionalAttributesSpec extends DefaultIntSpec {

    @Autowired
    SkillAttributeService skillAttributeService

    @Autowired
    SkillDefRepo skillDefRepo

    def "save and get additional skill video attributes"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        when:
        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[0].skillId, [videoUrl: "blah", videoType: "doubleBlah"])
        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[1].skillId, [videoUrl: "other", videoType: "k"])
        skillAttributeService.saveBonusAwardAttrs(p1.projectId, p1Skills[0].skillId, new BonusAwardAttrs(name: 'one', numMinutes: 10, iconClass: 'fa-cool'))

        def skill1VidAttrs = skillsService.getSkillVideoAttributes(p1.projectId, p1Skills[0].skillId)
        def skill2VidAttrs = skillsService.getSkillVideoAttributes(p1.projectId, p1Skills[1].skillId)
        def skill3VidAttrs = skillsService.getSkillVideoAttributes(p1.projectId, p1Skills[2].skillId)
        BonusAwardAttrs skill1BonusAttrs = skillAttributeService.getBonusAwardAttrs(p1.projectId, p1Skills[0].skillId)
        then:
        skill1VidAttrs.videoUrl == "blah"
        skill1VidAttrs.videoType == "doubleBlah"

        skill2VidAttrs.videoUrl == "other"
        skill2VidAttrs.videoType == "k"

        !skill3VidAttrs.videoUrl
        !skill3VidAttrs.videoType

        skill1BonusAttrs.name == "one"
        skill1BonusAttrs.numMinutes == 10
        skill1BonusAttrs.iconClass == "fa-cool"
    }
}
