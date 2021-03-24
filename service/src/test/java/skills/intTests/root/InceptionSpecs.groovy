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
package skills.intTests.root

import org.springframework.beans.factory.annotation.Autowired
import skills.controller.request.model.ProjectSettingsRequest
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.services.inception.InceptionProjectService
import skills.services.inception.InceptionSkills
import skills.services.settings.SettingsService
import skills.settings.CommonSettings
import skills.storage.model.SkillDef
import skills.storage.repos.SkillDefRepo
import skills.utils.Props
import spock.lang.IgnoreIf
import spock.lang.IgnoreRest

@IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
class InceptionSpecs extends DefaultIntSpec {

    @Autowired
    InceptionSkills inceptionSkills

    @Autowired
    InceptionProjectService inceptionProjectService

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    SettingsService settingsService

    def setup() {
        String userName = getRandomUsers(1).first()
        createService(userName)
        inceptionProjectService.createInceptionAndAssignUser(userName)
    }

    def 'get hash'() {
        when:
        def hash1 = inceptionSkills.getHash()
        def hash2 = inceptionSkills.getHash()

        then:
        hash1 == hash2
    }

    def 'update if hash is different'() {

        List<SkillDef> skills = skillDefRepo.findAllByProjectIdAndType(InceptionProjectService.inceptionProjectId, SkillDef.ContainerType.Skill)
        SkillDef skillToEdit = skills.get(0)

        SkillDef orig = new SkillDef()
        Props.copy(skillToEdit, orig, "childSkills", 'version', 'selfReportType')

        skillToEdit.name = "some other name"
        skillToEdit.pointIncrement = 123

        skillDefRepo.save(skillToEdit)

        ProjectSettingsRequest skillsMd5Setting = new ProjectSettingsRequest(
                projectId: InceptionProjectService.inceptionProjectId,
                setting: CommonSettings.INCEPTION_SKILLS_MD5_HASH,
                settingGroup: CommonSettings.INCEPTION_SETTING_GROUP,
                value: "-1"
        )
        settingsService.saveSetting(skillsMd5Setting)

        when:
        SkillDef fromDBBefore =  skillDefRepo.findById(skillToEdit.id).get()
        assert fromDBBefore.name == "some other name"
        assert fromDBBefore.name != orig.name
        assert fromDBBefore.pointIncrement != orig.pointIncrement
        inceptionProjectService.init()

        SkillDef fromDBAfter = skillDefRepo.findById(skillToEdit.id).get()
        then:
        fromDBAfter.name == orig.name
        fromDBAfter.pointIncrement == orig.pointIncrement
    }

    def 'do not update if hash is the same'() {
        List<SkillDef> skills = skillDefRepo.findAllByProjectIdAndType(InceptionProjectService.inceptionProjectId, SkillDef.ContainerType.Skill)
        SkillDef skillToEdit = skills.get(0)

        SkillDef orig = new SkillDef()
        Props.copy(skillToEdit, orig, "childSkills", 'version', 'selfReportType')

        skillToEdit.name = "some other name"
        skillToEdit.pointIncrement = 123

        skillDefRepo.save(skillToEdit)

        when:
        SkillDef fromDBBefore =  skillDefRepo.findById(skillToEdit.id).get()
        assert fromDBBefore.name == "some other name"
        assert fromDBBefore.name != orig.name
        assert fromDBBefore.pointIncrement != orig.pointIncrement
        inceptionProjectService.init()

        SkillDef fromDBAfter = skillDefRepo.findById(skillToEdit.id).get()
        then:
        // should have not changed
        fromDBAfter.name =="some other name"
        fromDBAfter.pointIncrement == 123
    }
}
