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
package skills.services.admin

import callStack.profiler.Profile
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.exceptions.SkillException
import skills.controller.request.model.SkillRequest
import skills.services.CreatedResourceLimitsValidator
import skills.services.CustomValidator
import skills.services.LockingService
import skills.services.RuleSetDefGraphService
import skills.storage.accessors.ProjDefAccessor
import skills.storage.model.SkillDef
import skills.storage.model.SkillDefWithExtra
import skills.storage.model.SkillRelDef
import skills.storage.repos.*
import skills.storage.repos.nativeSql.NativeQueriesRepo

@Service
@Slf4j
class SkillsGroupAdminService {

    @Autowired
    LockingService lockingService

    @Autowired
    CustomValidator customValidator

    @Autowired
    CreatedResourceLimitsValidator createdResourceLimitsValidator

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    SkillDefWithExtraRepo skillDefWithExtraRepo

    @Autowired
    ProjDefAccessor projDefAccessor

    @Autowired
    RuleSetDefGraphService ruleSetDefGraphService

    @Autowired
    DisplayOrderService displayOrderService

    @Autowired
    SkillsAdminService skillsAdminService

    @Autowired
    NativeQueriesRepo nativeQueriesRepo

    @Autowired
    UserAchievedLevelRepo achievedLevelRepo

    @Autowired
    GlobalBadgeLevelDefRepo globalBadgeLevelDefRepo

    @Autowired
    SkillRelDefRepo skillRelDefRepo

    @Transactional()
    void addSkillToSkillsGroup(String projectId, String skillsGroupId, String skillId) {
        ruleSetDefGraphService.assignGraphRelationship(projectId, skillsGroupId, SkillDef.ContainerType.SkillsGroup, skillId, SkillRelDef.RelationshipType.SkillsGroupRequirement)
    }

    @Profile
    List<SkillDef> getSkillsGroupChildSkills(Integer skillsGroupIdRef) {
        return skillDefRepo.findChildSkillsByIdAndRelationshipType(skillsGroupIdRef, SkillRelDef.RelationshipType.SkillsGroupRequirement)
    }

    List<SkillDef> loadAndValidateSkillsGroup(SkillRequest skillRequest, SkillDefWithExtra skillDefinition) {
        if (skillDefinition.type != skills.storage.model.SkillDef.ContainerType.valueOf(skillRequest.type)) {
            throw new SkillException("Cannot convert an existing Skill to a Skill Group, or existing Skill Group to Skill.")
        }
        int numSkillsRequired = skillRequest.numSkillsRequired
        boolean enabled = StringUtils.isNotBlank(skillRequest.enabled) && StringUtils.equalsIgnoreCase(skillRequest.enabled, Boolean.TRUE.toString())
        Integer skillsGroupIdRef = skillDefinition.id
        return loadAndValidateSkillsGroup(numSkillsRequired, enabled, skillsGroupIdRef)
    }

    @Profile
    List<SkillDef> loadAndValidateSkillsGroup(Integer numSkillsRequired, boolean enabled, Integer skillsGroupIdRef, Integer expectedPoints=null) {
        List<SkillDef> groupChildSkills = getSkillsGroupChildSkills(skillsGroupIdRef)
        if (enabled) {
            if (numSkillsRequired == 0) {
                throw new SkillException("A Skill Group must have at least 1 required skill in order to be enabled.")
            } else {
                int numChildSkills = groupChildSkills.size()
                if (numChildSkills < 2) {
                    throw new SkillException("A Skill Group must have at least 2 skills in order to be enabled.")
                }
                if (numSkillsRequired > numChildSkills) {
                    throw new SkillException("A Skill Group cannot require more skills than the number of skills that belong to the group.")
                }
                if (numSkillsRequired != -1 && numSkillsRequired < 1) {
                    throw new SkillException("A Skill Group must have at least 1 required skill in order to be enabled.")
                }
                boolean allSkillsRequired = numSkillsRequired == -1 || numSkillsRequired == numChildSkills
                if (!allSkillsRequired) {
                    int testValue = expectedPoints ?: groupChildSkills.first().totalPoints
                    // if only a subset of skills are required, then all skills must have the same total point value
                    boolean allTotalPointsEqual = groupChildSkills.every { it.totalPoints == testValue }
                    if (!allTotalPointsEqual) {
                        throw new SkillException("All skills that belong to the Skill Group must have the same total value when all skills are not required to be completed.")
                    }
                }
            }
        }
        return groupChildSkills
    }
}
