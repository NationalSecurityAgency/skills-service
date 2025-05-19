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
import skills.storage.repos.nativeSql.PostgresQlNativeRepo

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
    PostgresQlNativeRepo PostgresQlNativeRepo

    @Autowired
    UserAchievedLevelRepo achievedLevelRepo

    @Autowired
    GlobalBadgeLevelDefRepo globalBadgeLevelDefRepo

    @Autowired
    SkillRelDefRepo skillRelDefRepo

    @Profile
    @Transactional()
    void addSkillToSkillsGroup(String projectId, String skillsGroupId, String skillId) {
        ruleSetDefGraphService.assignGraphRelationship(projectId, skillsGroupId, SkillDef.ContainerType.SkillsGroup, skillId, SkillRelDef.RelationshipType.SkillsGroupRequirement)
    }

    @Profile
    List<SkillDef> getSkillsGroupChildSkills(Integer skillsGroupIdRef) {
        return skillDefRepo.findChildSkillsByIdAndRelationshipType(skillsGroupIdRef, SkillRelDef.RelationshipType.SkillsGroupRequirement)
    }

    @Profile
    Integer getActualNumSkillsRequred(Integer storedNumSkillsRequired, Integer skillsGroupIdRef) {
        if (storedNumSkillsRequired == -1) {
            return skillDefRepo.countChildSkillsByIdAndRelationshipTypeAndEnabled(skillsGroupIdRef, SkillRelDef.RelationshipType.SkillsGroupRequirement, "true")
        } else {
            return storedNumSkillsRequired
        }
    }

    @Profile
    List<SkillDef> validateSkillsGroupAndReturnChildren(SkillRequest skillRequest, SkillDefWithExtra skillDefinition) {
        if (skillDefinition.type != skills.storage.model.SkillDef.ContainerType.valueOf(skillRequest.type)) {
            throw new SkillException("Cannot convert an existing Skill to a Skill Group, or existing Skill Group to Skill.")
        }
        int numSkillsRequired = skillRequest.numSkillsRequired
        Integer skillsGroupIdRef = skillDefinition.id
        return validateSkillsGroupAndReturnChildren(numSkillsRequired, skillsGroupIdRef)
    }

    @Profile
    List<SkillDef> validateSkillsGroupAndReturnChildren(Integer numSkillsRequired,Integer skillsGroupIdRef) {
        List<SkillDef> groupChildSkills = getSkillsGroupChildSkills(skillsGroupIdRef)
        int numChildSkills = groupChildSkills.size()
        if (numSkillsRequired == 0 && numChildSkills > 0) {
            throw new SkillException("A Skill Group must have at least 1 required skill.")
        } else {
            if (numSkillsRequired > numChildSkills) {
                throw new SkillException("A Skill Group cannot require more skills than the number of skills that belong to the group.")
            }
            if (numSkillsRequired != -1 && numSkillsRequired < 0) {
                throw new SkillException("Invalid number of skills required for Skill Group [${numSkillsRequired}].")
            }
        }
        return groupChildSkills
    }

    @Profile
    boolean isParentSkillsGroupEnabled(String projectId, String groupId) {
        SkillDefWithExtra skillsGroupSkillDef = getSkillsGroup(projectId, groupId)
        return Boolean.valueOf(skillsGroupSkillDef.enabled)
    }

    @Profile
    SkillDefWithExtra getSkillsGroup(String projectId, String groupId) {
        return skillDefWithExtraRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(projectId, groupId, SkillDef.ContainerType.SkillsGroup)
    }

    @Profile
    int getGroupTotalPoints(List<SkillDef> groupChildSkills) {
        int totalPoints = 0
        if (groupChildSkills) {
            totalPoints = groupChildSkills.findAll({ Boolean.valueOf(it.enabled) }).collect { it.totalPoints }.sum()
        }
        return totalPoints
    }
}
