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
package skills.services.attributes

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import skills.storage.accessors.SkillDefAccessor
import skills.storage.model.SkillAttributesDef
import skills.storage.model.SkillDef
import skills.storage.repos.SkillAttributesDefRepo

@Service
@Slf4j
class SkillAttributeService {

    @Autowired
    SkillDefAccessor skillDefAccessor

    @Autowired
    SkillAttributesDefRepo skillAttributesDefRepo

    static final ObjectMapper mapper = new ObjectMapper()

    void saveVideoAttrs(String projectId, String skillId, SkillVideoAttrs videoAttrs) {
        saveAttrs(projectId, skillId, SkillAttributesDef.SkillAttributesType.Video, videoAttrs)
    }

    SkillVideoAttrs getVideoAttrs(String projectId, String skillId) {
        return getAttrs(projectId, skillId, SkillAttributesDef.SkillAttributesType.Video, SkillVideoAttrs.class)
    }

    void saveBonusAwardAttrs(String projectId, String skillId, BonusAwardAttrs bonusAwardAttrs) {
        saveAttrs(projectId, skillId, SkillAttributesDef.SkillAttributesType.BonusAward, bonusAwardAttrs)
    }

    BonusAwardAttrs getBonusAwardAttrs(String projectId, String skillId) {
        return getAttrs(projectId, skillId, SkillAttributesDef.SkillAttributesType.BonusAward, BonusAwardAttrs.class)
    }

    private <T> void saveAttrs(String projectId, String skillId, SkillAttributesDef.SkillAttributesType type, T videoAttrs) {
        Integer skillDefId = skillDefAccessor.getSkillDefId(projectId, skillId, SkillDef.ContainerType.Skill)
        SkillAttributesDef skillAttributesDef = skillAttributesDefRepo.findBySkillRefIdAndType(skillDefId, type)
        if (!skillAttributesDef) {
            skillAttributesDef = new SkillAttributesDef(skillRefId: skillDefId, type: type)
        }
        skillAttributesDef.attributes = mapper.writeValueAsString(videoAttrs)
        skillAttributesDefRepo.save(skillAttributesDef)
    }

    private <T> T getAttrs(String projectId, String skillId, SkillAttributesDef.SkillAttributesType type, Class<T> clazz) {
        Integer skillDefId = skillDefAccessor.getSkillDefId(projectId, skillId, SkillDef.ContainerType.Skill)
        SkillAttributesDef skillAttributesDef = skillAttributesDefRepo.findBySkillRefIdAndType(skillDefId, type)
        if (!skillAttributesDef) {
            return clazz.getDeclaredConstructor().newInstance()
        }
        T res = mapper.readValue(skillAttributesDef.attributes, clazz)
        return  res
    }
}
