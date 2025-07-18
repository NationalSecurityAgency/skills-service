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
import org.springframework.transaction.annotation.Transactional
import skills.services.userActions.DashboardAction
import skills.services.userActions.DashboardItem
import skills.services.userActions.UserActionInfo
import skills.services.userActions.UserActionsHistoryService
import skills.storage.accessors.SkillDefAccessor
import skills.storage.model.SkillAttributesDef
import skills.storage.model.SkillAttributesDef.SkillAttributesType
import skills.storage.model.SkillDef
import skills.storage.repos.SkillAttributesDefRepo
import skills.storage.repos.SkillDefRepo
import skills.utils.InputSanitizer

@Service
@Slf4j
class SkillAttributeService {

    @Autowired
    SkillDefAccessor skillDefAccessor

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    SkillAttributesDefRepo skillAttributesDefRepo

    @Autowired
    UserActionsHistoryService userActionsHistoryService

    static final ObjectMapper mapper = new ObjectMapper()

    void saveVideoAttrs(String projectId, String skillId, SkillVideoAttrs videoAttrs) {
        saveAttrs(projectId, skillId, SkillAttributesDef.SkillAttributesType.Video, videoAttrs)
    }

    void saveSlidesAttrs(String projectId, String skillId, SlidesAttrs slidesAttrs) {
        saveAttrs(projectId, skillId, SkillAttributesType.Slides, slidesAttrs)
    }

    @Transactional
    boolean deleteVideoAttrs(String projectId, String skillId) {
        int numRemoved = deleteAttrs(projectId, skillId, SkillAttributesDef.SkillAttributesType.Video)
        if (numRemoved > 1) {
            throw new IllegalStateException("There is more than 1 Video attributes for [${projectId}-${skillId}]")
        }
        skillDefRepo.unsetSelfReportTypeByProjectIdSkillIdAndSelfReportType(projectId, skillId, SkillDef.SelfReportingType.Video)
        return numRemoved > 0
    }

    SkillVideoAttrs getVideoAttrs(String projectId, String skillId) {
        SkillVideoAttrs skillVideoAttrs = getAttrs(projectId, skillId, SkillAttributesType.Video, SkillVideoAttrs.class)
        skillVideoAttrs.captions = InputSanitizer.unSanitizeCaption(skillVideoAttrs.captions)
        return skillVideoAttrs
    }

    SlidesAttrs getSlidesAttrs(String projectId, String skillId) {
        SlidesAttrs slidesAttrs = getAttrs(projectId, skillId, SkillAttributesType.Slides, SlidesAttrs.class)
        return slidesAttrs
    }

    void saveExpirationAttrs(String projectId, String skillId, ExpirationAttrs skillExpirationAttrs) {
        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: DashboardAction.Create,
                item: DashboardItem.ExpirationSettings,
                itemId: skillId,
                projectId: projectId,
                actionAttributes: skillExpirationAttrs
        ))
        saveAttrs(projectId, skillId, SkillAttributesDef.SkillAttributesType.AchievementExpiration, skillExpirationAttrs)
    }

    @Transactional
    void deleteExpirationAttrs(String projectId, String skillId) {
        deleteAttrs(projectId, skillId, SkillAttributesDef.SkillAttributesType.AchievementExpiration)
    }

    ExpirationAttrs getExpirationAttrs(String projectId, String skillId) {
        ExpirationAttrs skillExpirationAttrs = getAttrs(projectId, skillId, SkillAttributesDef.SkillAttributesType.AchievementExpiration, ExpirationAttrs.class)
        return skillExpirationAttrs
    }
    Boolean isMotivationalSkill(String projectId, String skillId) {
        ExpirationAttrs expirationAttrs = getExpirationAttrs(projectId, skillId)
        return expirationAttrs?.expirationType == ExpirationAttrs.DAILY
    }

    void saveBadgeBonusAwardAttrs(String projectId, String skillId, BonusAwardAttrs bonusAwardAttrs) {
        saveAttrs(projectId, skillId, SkillAttributesDef.SkillAttributesType.BonusAward, bonusAwardAttrs, SkillDef.ContainerType.Badge)
    }

    void saveBonusAwardAttrs(String projectId, String skillId, BonusAwardAttrs bonusAwardAttrs) {
        saveAttrs(projectId, skillId, SkillAttributesDef.SkillAttributesType.BonusAward, bonusAwardAttrs)
    }

    BonusAwardAttrs getBonusAwardAttrs(String projectId, String skillId) {
        return getAttrs(projectId, skillId, SkillAttributesDef.SkillAttributesType.BonusAward, BonusAwardAttrs.class)
    }

    <T> T convertAttrs(SkillAttributesDef skillAttributesDef, Class<T> clazz) {
        if (!skillAttributesDef) {
            return clazz.getDeclaredConstructor().newInstance()
        }
        T res = mapper.readValue(skillAttributesDef.attributes, clazz)
        return  res
    }

    <T> void saveAttrs(String projectId, String skillId, SkillAttributesDef.SkillAttributesType type, T attributes, SkillDef.ContainerType containerType = SkillDef.ContainerType.Skill) {
        Integer skillDefId = skillDefAccessor.getSkillDefId(projectId, skillId, containerType)
        SkillAttributesDef skillAttributesDef = skillAttributesDefRepo.findBySkillRefIdAndType(skillDefId, type)
        if (!skillAttributesDef) {
            skillAttributesDef = new SkillAttributesDef(skillRefId: skillDefId, type: type)
        }
        skillAttributesDef.attributes = mapper.writeValueAsString(attributes)
        skillAttributesDefRepo.save(skillAttributesDef)
    }

    private <T> T getAttrs(String projectId, String skillId, SkillAttributesDef.SkillAttributesType type, Class<T> clazz) {
        return convertAttrs(skillAttributesDefRepo.findByProjectIdAndSkillIdAndType(projectId, skillId, type.toString()), clazz)
    }

    private <T> List<T> getAttrsList(SkillAttributesDef.SkillAttributesType type, Class<T> clazz) {
        List<SkillAttributesDef> skillAttributesDefs = skillAttributesDefRepo.findAllByType(type)
        if (!skillAttributesDefs) {
            return []
        }
        List<T> res = skillAttributesDefs.collect { mapper.readValue(it.attributes, clazz) }
        return  res
    }

    private int deleteAttrs(String projectId, String skillId, SkillAttributesDef.SkillAttributesType type) {
        Integer skillDefId = skillDefAccessor.getSkillDefId(projectId, skillId, SkillDef.ContainerType.Skill)
        return skillAttributesDefRepo.deleteBySkillRefIdAndType(skillDefId, type)
    }
}
