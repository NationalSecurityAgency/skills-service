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

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.request.model.SkillsTagRequest
import skills.services.LockingService
import skills.services.RuleSetDefGraphService
import skills.storage.accessors.ProjDefAccessor
import skills.storage.model.*
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.SkillDefWithExtraRepo
import skills.storage.repos.SkillRelDefRepo

@Service
@Slf4j
class SkillTagService {

    @Autowired
    LockingService lockingService

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    SkillDefWithExtraRepo skillDefWithExtraRepo

    @Autowired
    ProjDefAccessor projDefAccessor

    @Autowired
    RuleSetDefGraphService ruleSetDefGraphService

    @Autowired
    SkillRelDefRepo skillRelDefRepo

    @Transactional()
    void addTag(String projectId, SkillsTagRequest skillsTagRequest) {
        lockingService.lockProject(projectId)
        SkillDef.ContainerType type = SkillDef.ContainerType.Tag
        SkillDefWithExtra skillDefinition = skillDefWithExtraRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(projectId, skillsTagRequest.tagId, type)

        if (!skillDefinition) {
            ProjDef projDef = projDefAccessor.getProjDef(projectId)
            skillDefinition = new SkillDefWithExtra(
                    type: type,
                    projectId: projectId,
                    skillId: skillsTagRequest.tagId,
                    name: skillsTagRequest.tagValue,
                    projRefId: projDef.id,
                    displayOrder: 0,
                    enabled: Boolean.TRUE.toString()
            )
            log.debug("Saving [{}]", skillDefinition)
            SkillDefWithExtra savedSkill
            DataIntegrityExceptionHandlers.tagDataIntegrityViolationExceptionHandler.handle(projectId) {
                savedSkill = skillDefWithExtraRepo.save(skillDefinition)
            }
            log.debug("Saved [{}]", savedSkill)
        }

        List<String> existingTaggedSkillIds = getSkillsForTag(projectId, skillsTagRequest.tagId)?.collect { it.skillId }
        skillsTagRequest.skillIds.each { skillId ->
            if (!existingTaggedSkillIds.find { it == skillId}) {
                ruleSetDefGraphService.assignGraphRelationship(projectId, skillDefinition.skillId, SkillDef.ContainerType.Tag, skillId, SkillRelDef.RelationshipType.Tag)
            }
        }
        log.debug("Added [{}] tag to skills [{}]", skillsTagRequest.tagValue, skillsTagRequest.skillIds)
    }

    @Transactional
    List<SkillTag> getTagsForProject(String projectId) {
        skillDefRepo.getTagsForProject(projectId)
    }

    @Transactional
    List<SkillTag> getTagsForSkill(Integer skillRefId) {
        skillDefRepo.getTagsForSkill(skillRefId)
    }

    @Transactional
    List<SkillTag> getTagsForSkill(String projectId, String skillId) {
        skillDefRepo.getTagsForSkillWithSkillId(projectId, skillId)
    }

    @Transactional
    List<SkillDefPartial> getSkillsForTag(String projectId, String tagId) {
        return skillRelDefRepo.getChildrenPartial(projectId, tagId, SkillRelDef.RelationshipType.Tag)
    }

    @Transactional
    List<SkillTag> getTagsForSkills(String projectId, List<String> skillIds) {
        skillDefRepo.getTagsForSkills(projectId, skillIds)
    }

    @Transactional
    void deleteTagForSkills(String projectId, SkillsTagRequest skillsTagRequest) {
        String tagId = skillsTagRequest.tagId
        SkillDefWithExtra tag = skillDefWithExtraRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(projectId, tagId, SkillDef.ContainerType.Tag)

        if (!tag) {
            throw new SkillException("Tag [${tagId}] does not exist", projectId, tagId, ErrorCode.TagNotFound)
        }

        skillsTagRequest.skillIds.each {skillId ->
            ruleSetDefGraphService.removeGraphRelationship(projectId, tagId, SkillDef.ContainerType.Tag,
                    projectId, skillId, SkillRelDef.RelationshipType.Tag, false)
        }

        Integer skillsWithTag = skillRelDefRepo.getSkillWithTagCount(tagId)
        if (skillsWithTag <= 0) {
            log.info("Tag [${tagId}] removed from all skills, removing tag definition")
            skillDefWithExtraRepo.delete(tag)
        }
    }
}
