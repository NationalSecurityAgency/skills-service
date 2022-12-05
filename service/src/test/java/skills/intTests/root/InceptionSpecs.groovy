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
import skills.services.inception.InceptionBadges
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
    InceptionBadges inceptionBadges

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

    def 'delete skills from the database that have been removed from the code'() {

        List<SkillDef> skills = skillDefRepo.findAllByProjectIdAndType(InceptionProjectService.inceptionProjectId, SkillDef.ContainerType.Skill)
        SkillDef skillToDelete1 = skills.get(0)
        SkillDef skillToDelete2 = skills.get(1)

        SkillDef origSkill1 = new SkillDef()
        Props.copy(skillToDelete1, origSkill1, "childSkills", 'version', 'selfReportType')

        SkillDef origSkill2 = new SkillDef()
        Props.copy(skillToDelete2, origSkill2, "childSkills", 'version', 'selfReportType')

        skillToDelete1.skillId = 'someOtherSkillId'
        skillToDelete1.name = "some other name"
        skillToDelete1.pointIncrement = 123
        skillDefRepo.save(skillToDelete1)

        skillToDelete2.skillId = 'someOtherSkillId2'
        skillToDelete2.name = "some other name 2"
        skillToDelete2.pointIncrement = 1234
        skillDefRepo.save(skillToDelete2)

        ProjectSettingsRequest skillsMd5Setting = new ProjectSettingsRequest(
                projectId: InceptionProjectService.inceptionProjectId,
                setting: CommonSettings.INCEPTION_SKILLS_MD5_HASH,
                settingGroup: CommonSettings.INCEPTION_SETTING_GROUP,
                value: "-1"
        )
        settingsService.saveSetting(skillsMd5Setting)

        when:
        SkillDef fromDBBefore1 =  skillDefRepo.findById(skillToDelete1.id).get()
        assert fromDBBefore1.skillId == 'someOtherSkillId'
        assert fromDBBefore1.name == "some other name"
        assert fromDBBefore1.skillId != origSkill1.skillId
        assert fromDBBefore1.name != origSkill1.name
        assert fromDBBefore1.pointIncrement != origSkill1.pointIncrement

        SkillDef fromDBBefore2 =  skillDefRepo.findById(skillToDelete2.id).get()
        assert fromDBBefore2.skillId == 'someOtherSkillId2'
        assert fromDBBefore2.name == "some other name 2"
        assert fromDBBefore2.skillId != origSkill2.skillId
        assert fromDBBefore2.name != origSkill2.name
        assert fromDBBefore2.pointIncrement != origSkill2.pointIncrement

        inceptionProjectService.init()

        Boolean skill1Deleted = skillDefRepo.findById(skillToDelete1.id).isEmpty()
        Boolean skill2Deleted = skillDefRepo.findById(skillToDelete2.id).isEmpty()
        List<SkillDef> inceptionSkillsFromDb = skillDefRepo.findAllByProjectIdAndType(InceptionProjectService.inceptionProjectId, SkillDef.ContainerType.Skill)

        int inceptionSkillsFromDbSize = inceptionSkillsFromDb.size()
        int inceptionSkillsSize = inceptionSkills.getAllSkills().size()
        boolean foundSkillToDelete1 = inceptionSkillsFromDb.find { it.skillId == skillToDelete1.skillId }
        boolean foundSkillToDelete2 = inceptionSkillsFromDb.find { it.skillId == skillToDelete2.skillId }
        List<String> inceptionSkillIds = inceptionSkills.getAllSkills().collect { it.skillId }
        List<String> skillIdsFromDb = inceptionSkillsFromDb.collect {it.skillId}

        then:
        skill1Deleted
        skill2Deleted
        !inceptionSkillIds.findAll {!skillIdsFromDb.contains(it) }
        inceptionSkillsFromDbSize == inceptionSkillsSize
        !foundSkillToDelete1
        !foundSkillToDelete2
        inceptionSkillIds.each { String inceptionSkill ->
            assert inceptionSkillsFromDb.find { it.skillId == inceptionSkill}
        }
    }

    def 'badges are created'() {
        List<SkillDef> skills = skillDefRepo.findAllByProjectIdAndType(InceptionProjectService.inceptionProjectId, SkillDef.ContainerType.Badge)

        List<InceptionBadges.BadgeInfo> badgeInfos = inceptionBadges.getBadges()
        skillDefRepo.delete(skills[0])
        List<SkillDef> badges_t1 = skillDefRepo.findAllByProjectIdAndType(InceptionProjectService.inceptionProjectId, SkillDef.ContainerType.Badge)
        inceptionProjectService.init()
        List<SkillDef> badges_t2 = skillDefRepo.findAllByProjectIdAndType(InceptionProjectService.inceptionProjectId, SkillDef.ContainerType.Badge)

        when:
        int expectedBadgeNum = badgeInfos.size()
        int actualBadgeNum = skills.size()
        int actualBadgeNum_t1 = badges_t1.size()
        int actualBadgeNum_t2 = badges_t2.size()

        then:
        actualBadgeNum == expectedBadgeNum
        actualBadgeNum_t1 == expectedBadgeNum - 1
        actualBadgeNum_t2 == expectedBadgeNum
    }

}
