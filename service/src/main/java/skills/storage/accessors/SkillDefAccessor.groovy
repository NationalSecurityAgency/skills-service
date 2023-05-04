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
package skills.storage.accessors

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.exceptions.SkillException
import skills.controller.result.model.SkillDefPartialRes
import skills.storage.model.SkillDef
import skills.storage.model.SkillDefWithExtra
import skills.storage.model.SkillRelDef
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.SkillDefWithExtraRepo

@Service
class SkillDefAccessor {

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    SkillDefWithExtraRepo skillDefWithExtraRepo

    SkillDef getSkillAndBadgeDef(String projectId, String skillId, List<SkillDef.ContainerType> containerTypes = [SkillDef.ContainerType.Skill, SkillDef.ContainerType.SkillsGroup, SkillDef.ContainerType.Badge]) {
        SkillDef skillDef = skillDefRepo.findByProjectIdAndSkillIdIgnoreCaseAndTypeIn(projectId, skillId, containerTypes)
        if (!skillDef) {
            throw new SkillException("Failed to find skillId [$skillId] for [$projectId] with types [${containerTypes}]", projectId, skillId)
        }
        return skillDef
    }

    SkillDef getSkillDef(String projectId, String skillId, List<SkillDef.ContainerType> containerTypes = [SkillDef.ContainerType.Skill, SkillDef.ContainerType.SkillsGroup]) {
        SkillDef skillDef = skillDefRepo.findByProjectIdAndSkillIdIgnoreCaseAndTypeIn(projectId, skillId, containerTypes)
        if (!skillDef) {
            throw new SkillException("Failed to find skillId [$skillId] for [$projectId] with types [${containerTypes}]", projectId, skillId)
        }
        return skillDef
    }

    SkillDefWithExtra getSkillDefWithExtra(String projectId, String skillId, List<SkillDef.ContainerType> containerTypes = [SkillDef.ContainerType.Skill, SkillDef.ContainerType.SkillsGroup]) {
        SkillDefWithExtra skillDef = skillDefWithExtraRepo.findByProjectIdAndSkillIdIgnoreCaseAndTypeIn(projectId, skillId, containerTypes)
        if (!skillDef) {
            throw new SkillException("Failed to find skillId [$skillId] for [$projectId] with types [${containerTypes}]", projectId, skillId)
        }
        return skillDef
    }

}
