/**
 * Copyright 2021 SkillTree
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
package skills.services

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import skills.controller.request.model.SkillRequest
import skills.controller.request.model.SubjectRequest
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.services.admin.ProjAdminService
import skills.services.admin.SkillCatalogService
import skills.services.admin.SkillsAdminService
import skills.services.admin.SubjAdminService
import skills.storage.model.SkillDefWithExtra
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.SkillDefWithExtraRepo

class SkillCatalogServiceSpec extends DefaultIntSpec {

    @Autowired
    SkillCatalogService catalogService

    @Autowired
    ProjAdminService projectAdminService

    @Autowired
    SubjAdminService subjAdminService

    @Autowired
    SkillsAdminService skillsAdminService

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    SkillDefWithExtraRepo skillDefWithExtraRepo

    @Autowired
    PlatformTransactionManager transactionManager;

    def "identify related skills"() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager)

        def proj1 = SkillsFactory.createProject(1)
        proj1.projectId = "aproject"

        def proj2 = SkillsFactory.createProject(2)
        proj2.projectId = "bproject"

        skillsService.createProject(proj1)
        skillsService.createProject(proj2)

        subjAdminService.saveSubject("aproject", "asubj", new SubjectRequest(subjectId: "asubj", name: "A subj"))
        subjAdminService.saveSubject("bproject", "bsubj", new SubjectRequest(subjectId: "bsubj", name: "B subj"))

        SkillRequest sr = new SkillRequest(skillId: "fooskill", subjectId: "asubj", projectId: "aproject", pointIncrement: 40, numPerformToCompletion: 3, name: "foo")
        skillsAdminService.saveSkill("fooskill", sr)

        sr.subjectId = "bsubj"
        sr.projectId = "bproject"
        skillsAdminService.saveSkill("fooskill", sr)

        SkillDefWithExtra og = skillDefWithExtraRepo.findByProjectIdAndSkillId("aproject", "fooskill")
        SkillDefWithExtra copy = skillDefWithExtraRepo.findByProjectIdAndSkillId("bproject", "fooskill")

        //create two projects, create two subjects
        transactionTemplate.execute({
            catalogService.exportSkillToCatalog("aproject", "fooskill")
            copy.copiedFrom = og.id
            skillDefWithExtraRepo.save(copy)
        })

        when:
        List<SkillDefWithExtra> related = []
        transactionTemplate.execute({
            related = catalogService.getRelatedSkills(og)
        })

        then:
        related.size() == 1
        related[0].id == copy.id
    }
}
