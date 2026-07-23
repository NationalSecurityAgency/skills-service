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
import skills.controller.request.model.SkillsTagDeleteRequest
import skills.controller.request.model.SkillsTagRequest
import skills.controller.result.model.SkillTagInfoRes
import skills.controller.result.model.SkillTagRes
import skills.services.LockingService
import skills.services.RuleSetDefGraphService
import skills.services.userActions.DashboardAction
import skills.services.userActions.DashboardItem
import skills.services.userActions.UserActionInfo
import skills.services.userActions.UserActionsHistoryService
import skills.storage.accessors.ProjDefAccessor
import skills.storage.model.*
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.SkillDefWithExtraRepo
import skills.storage.repos.SkillRelDefRepo
import skills.utils.InputSanitizer

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

    @Autowired
    UserActionsHistoryService userActionsHistoryService

    @Transactional()
    void addTag(String projectId, SkillsTagRequest skillsTagRequest) {
        lockingService.lockProject(projectId)
        SkillDef.ContainerType type = SkillDef.ContainerType.Tag

        if (skillsTagRequest.tagId && !skillsTagRequest.tagId.matches(/^[a-zA-Z0-9]+$/)) {
            throw new SkillException("Tag ID [${skillsTagRequest.tagId}] may only contain alphanumeric characters", projectId, skillsTagRequest.tagId, ErrorCode.BadParam)
        }

        String currentTagId = skillsTagRequest.origTagId ?: skillsTagRequest.tagId
        SkillDefWithExtra skillDefinition = skillDefWithExtraRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(projectId, currentTagId, type)

        String incomingTagValue = InputSanitizer.sanitize(skillsTagRequest.tagValue)
        if (!skillDefinition) {
            ProjDef projDef = projDefAccessor.getProjDef(projectId)
            skillDefinition = new SkillDefWithExtra(
                    type: type,
                    projectId: projectId,
                    skillId: skillsTagRequest.tagId,
                    name: incomingTagValue,
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
        } else {
            boolean isTagValueDifferent = incomingTagValue && incomingTagValue != skillDefinition.name
            boolean isTagIdDifferent = skillsTagRequest.tagId != skillDefinition.skillId
            if (isTagValueDifferent || isTagIdDifferent) {
                skillDefinition.name = incomingTagValue
                if (isTagIdDifferent) {
                    skillDefinition.skillId = skillsTagRequest.tagId
                }
                SkillDefWithExtra savedSkill
                DataIntegrityExceptionHandlers.tagDataIntegrityViolationExceptionHandler.handle(projectId) {
                    savedSkill = skillDefWithExtraRepo.save(skillDefinition)
                }
                log.debug("Updated [{}]", savedSkill)
            }
        }

        List<String> existingTaggedSkillIds = getSkillsForTag(projectId, skillsTagRequest.tagId)?.collect { it.skillId }

        if (skillsTagRequest.skillIds) {
            skillsTagRequest.skillIds?.each { skillId ->
                if (!existingTaggedSkillIds.find { it == skillId}) {
                    ruleSetDefGraphService.assignGraphRelationship(projectId, skillDefinition.skillId, SkillDef.ContainerType.Tag, skillId, SkillRelDef.RelationshipType.Tag)
                }

                userActionsHistoryService.saveUserAction(new UserActionInfo(
                        action: DashboardAction.Create,
                        item: DashboardItem.Tag,
                        actionAttributes: [
                                tagId: skillsTagRequest.tagId,
                                tagValue: incomingTagValue,
                        ],
                        itemId: skillId,
                        projectId: projectId,
                ))
            }
        } else {
            userActionsHistoryService.saveUserAction(new UserActionInfo(
                    action: DashboardAction.Create,
                    item: DashboardItem.Tag,
                    actionAttributes: [
                            tagValue: incomingTagValue,
                    ],
                    itemId: skillsTagRequest.tagId,
                    projectId: projectId,
            ))
        }


        log.debug("Added [{}] tag to skills [{}]", incomingTagValue, skillsTagRequest.skillIds)
    }

    @Transactional
    List<SkillTagRes> getTagsForProject(String projectId, Boolean includeDisabled=true) {
        List<SkillTag> tags = skillDefRepo.getTagsForProject(projectId, includeDisabled.toString())
        return convertToRes(tags)
    }

    @Transactional
    SkillTagInfoRes getSingleTagInfo(String projectId, String tagId, Boolean includeDisabled=true) {

        List<SkillDefPartial> skillsForTag = getSkillsForTag(projectId, tagId)

        SkillDef tagSkillDef = skillDefRepo.findByProjectIdAndSkillIdAndType(projectId, tagId, SkillDef.ContainerType.Tag)
        if (!tagSkillDef) {
            throw new SkillException("Tag with id [${tagId}] does not exist.", projectId, null, ErrorCode.BadParam)
        }

        return new SkillTagInfoRes(
                tagId: tagSkillDef.skillId,
                tagValue: InputSanitizer.unsanitizeName(tagSkillDef.name),
                skills: skillsForTag?.sort { it.relationshipCreated }?.reversed().collect {
                    new SkillTagInfoRes.SkillInfo(
                            skillId: it.skillId,
                            skillName: it.name,
                            subjectName: it.subjectName,
                            subjectId: it.subjectSkillId,
                            groupName: it.groupName,
                            groupId: it.groupId,
                            taggedOn: it.relationshipCreated,
                    )
                }
        )

    }


    @Transactional
    List<SkillTagRes> getTagsForSubject(String projectId, String subjectId, Boolean includeDisabled=true) {
        List<SkillTag> tags = skillDefRepo.getTagsForSubject(projectId, subjectId, includeDisabled.toString())
        return convertToRes(tags)
    }

    @Transactional
    List<SkillTagRes> getTagsForSkill(Integer skillRefId) {
        List<SkillTag> tags = skillDefRepo.getTagsForSkill(skillRefId)
        return convertToRes(tags)
    }

    @Transactional
    List<SkillTagRes> getTagsForSkill(String projectId, String skillId) {
        List<SkillTag> tags = skillDefRepo.getTagsForSkillWithSkillId(projectId, skillId)
        return convertToRes(tags)
    }

    private List<SkillDefPartial> getSkillsForTag(String projectId, String tagId) {
        return skillRelDefRepo.getChildrenPartial(projectId, tagId, SkillRelDef.RelationshipType.Tag)
    }

    @Transactional
    List<SkillTagRes> getTagsForSkills(String projectId, List<String> skillIds) {
        List<SkillTag> tags = skillDefRepo.getTagsForSkills(projectId, skillIds)
        return convertToRes(tags)
    }

    @Transactional
    void deleteTagForSkills(String projectId, SkillsTagDeleteRequest skillsTagRequest) {
        String tagId = skillsTagRequest.tagId
        SkillDefWithExtra tag = skillDefWithExtraRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(projectId, tagId, SkillDef.ContainerType.Tag)

        if (!tag) {
            throw new SkillException("Tag [${tagId}] does not exist", projectId, tagId, ErrorCode.TagNotFound)
        }

        boolean removeTagFully = skillsTagRequest.removeTagFully == true
        List<String> skillIds = (removeTagFully) ? getSkillsForTag(projectId, tagId)?.skillId : skillsTagRequest.skillIds

        skillIds.each {skillId ->
            ruleSetDefGraphService.removeGraphRelationship(projectId, tagId, SkillDef.ContainerType.Tag,
                    projectId, skillId, SkillRelDef.RelationshipType.Tag, false)
        }

        if (removeTagFully) {
            userActionsHistoryService.saveUserAction(new UserActionInfo(
                    action: DashboardAction.Delete,
                    item: DashboardItem.Tag,
                    actionAttributes: [
                            tagValue: InputSanitizer.unsanitizeName(tag.name),
                            removedFromSkills: skillIds,
                    ],
                    itemId: tagId,
                    projectId: projectId,
            ))
        } else {
            skillIds.each {skillId ->
                userActionsHistoryService.saveUserAction(new UserActionInfo(
                        action: DashboardAction.Delete,
                        item: DashboardItem.Tag,
                        actionAttributes: [
                                tagId: tag.skillId,
                                tagValue: InputSanitizer.unsanitizeName(tag.name),
                        ],
                        itemId: skillId,
                        projectId: projectId,
                ))
            }
        }

        Integer skillsWithTag = skillRelDefRepo.getSkillWithTagCount(tagId)
        if (removeTagFully) {
            assert skillsWithTag <= 0
        }
        if (!skillsTagRequest.retainTag && skillsWithTag <= 0) {
            log.info("Tag [${tagId}] removed from all skills, removing tag definition")
            skillDefWithExtraRepo.delete(tag)
        }
    }

    private List<SkillTagRes> convertToRes(List<SkillTag> tags) {
        return tags?.collect {
            new SkillTagRes(
                    tagId: it.tagId,
                    tagValue: InputSanitizer.unsanitizeName(it.tagValue),
                    numSkills: it.numSkills,
                    createdOn: it.createdOn)
        }
    }
}
