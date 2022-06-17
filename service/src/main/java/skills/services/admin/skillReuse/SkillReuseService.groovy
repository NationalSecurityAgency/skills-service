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

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PathVariable
import skills.controller.request.model.CatalogSkill
import skills.controller.request.model.SkillReuseRequest
import skills.controller.result.model.SkillDefPartialRes
import skills.controller.result.model.SkillDefSkinnyRes
import skills.services.admin.SkillCatalogFinalizationService
import skills.services.admin.SkillCatalogService
import skills.storage.accessors.SkillDefAccessor
import skills.storage.model.SkillDefSkinny
import skills.storage.repos.SkillDefRepo
import skills.utils.InputSanitizer

@Service
@Slf4j
class SkillReuseService {

    @Autowired
    SkillDefAccessor skillAccessor

    @Autowired
    SkillCatalogService skillCatalogService

    @Autowired
    SkillCatalogFinalizationService finalizationService

    @Autowired
    SkillDefRepo skillDefRepo

    @Transactional
    void reuseSkill(String projectId, SkillReuseRequest skillReuseRequest) {
        // import
        List<CatalogSkill> listOfSkills = skillReuseRequest.skillIds.collect { new CatalogSkill(projectId: projectId, skillId: it) }
        skillCatalogService.importSkillsFromCatalog(projectId, skillReuseRequest.subjectId, listOfSkills, skillReuseRequest.groupId, true)
        // finalize
        finalizationService.finalizeCatalogSkillsImport(projectId)
    }

    @Transactional(readOnly = true)
    List<SkillDefSkinnyRes> getReusedSkills(String projectId, String parentSkillId) {
        List<SkillDefSkinny> data = skillDefRepo.findChildReusedSkills(projectId, parentSkillId)
        List<SkillDefPartialRes> res = data.collect { SkillDefSkinny skinny ->
            new SkillDefSkinnyRes(
                    skillId: skinny.skillId,
                    projectId: skinny.projectId,
                    name: SkillReuseIdUtil.removeTag(InputSanitizer.unsanitizeName(skinny.name)),
                    subjectId: skinny.subjectSkillId,
                    subjectName: InputSanitizer.unsanitizeName(skinny.subjectName),
                    version: skinny.version,
                    displayOrder: skinny.displayOrder,
                    created: skinny.created,
                    totalPoints: skinny.totalPoints,
            )
        }?.sort({ it.skillId })
        return res
    }
}
