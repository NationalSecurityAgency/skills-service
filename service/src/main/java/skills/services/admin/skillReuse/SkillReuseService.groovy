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

import callStack.profiler.Profile
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.request.model.CatalogSkill
import skills.controller.request.model.SkillsActionRequest
import skills.controller.result.model.SkillDefPartialRes
import skills.controller.result.model.SkillDefSkinnyRes
import skills.controller.result.model.SkillReuseDestination
import skills.services.RuleSetDefGraphService
import skills.services.admin.SkillCatalogFinalizationService
import skills.services.admin.SkillCatalogService
import skills.services.userActions.DashboardAction
import skills.services.userActions.DashboardItem
import skills.services.userActions.UserActionInfo
import skills.services.userActions.UserActionsHistoryService
import skills.storage.accessors.SkillDefAccessor
import skills.storage.model.SkillDef
import skills.storage.model.SkillDefSkinny
import skills.storage.model.SkillRelDef
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.SkillRelDefRepo
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

    @Autowired
    SkillRelDefRepo skillRelDefRepo

    @Autowired
    RuleSetDefGraphService ruleSetDefGraphService

    @Autowired
    SkillDefAccessor skillDefAccessor

    @Autowired
    SkillCatalogFinalizationService skillCatalogFinalizationService

    @Autowired
    UserActionsHistoryService userActionsHistoryService

    @Transactional
    @Profile
    void reuseSkill(String projectId, SkillsActionRequest skillReuseRequest) {
        // validate
        validateSkillIsNotDisabled(projectId, skillReuseRequest)
        validateParentIsNotDestination(projectId, skillReuseRequest)
        validateNoAlreadyReusedInDestination(skillReuseRequest, projectId)
        skillCatalogFinalizationService.validateNotInFinalizationState(projectId, "Cannot reuse skills while finalization is running")
        skillCatalogFinalizationService.validateFinalizationIsNotPending(projectId, "Cannot reuse skills while finalization is pending")
        validateSkillsHaveNoDeps(projectId, skillReuseRequest)

        // import
        List<CatalogSkill> listOfSkills = skillReuseRequest.skillIds.collect { new CatalogSkill(projectId: projectId, skillId: it) }
        skillCatalogService.importSkillsFromCatalog(projectId, skillReuseRequest.subjectId, listOfSkills, skillReuseRequest.groupId, true)
        // finalize
        finalizationService.finalizeCatalogSkillsImport(projectId)

        handleTrackingUserActions(listOfSkills, skillReuseRequest)
    }

    @Profile
    private void handleTrackingUserActions(List<CatalogSkill> listOfSkills, SkillsActionRequest skillReuseRequest) {
        Map additionalAttributes = [toSubjectId: skillReuseRequest.subjectId]
        if (skillReuseRequest.groupId) {
            additionalAttributes.toGroupId = skillReuseRequest.groupId
        }
        listOfSkills.each {CatalogSkill catalogSkill ->
            userActionsHistoryService.saveUserAction(new UserActionInfo(
                    action: DashboardAction.ReuseInProject,
                    item: DashboardItem.Skill,
                    actionAttributes: additionalAttributes,
                    itemId: catalogSkill.skillId,
                    projectId: catalogSkill.projectId))
        }
    }

    @Profile
    private void validateNoAlreadyReusedInDestination(SkillsActionRequest skillReuseRequest, String projectId) {
        String parentSkillId = skillReuseRequest.groupId ?: skillReuseRequest.subjectId
        List<SkillDefSkinnyRes> alreadyReused = getReusedSkills(projectId, parentSkillId)
        List<String> reusedSkillIds = alreadyReused.collect { SkillReuseIdUtil.removeTag(it.skillId) }
        List<String> reusedProvidedSkills = skillReuseRequest.skillIds.findAll({ reusedSkillIds.contains(it) })
        if (reusedProvidedSkills) {
            throw new SkillException("Skill with skillIds of ${reusedProvidedSkills} are already reused in [${parentSkillId}]", projectId, null, ErrorCode.BadParam)
        }
    }

    @Profile
    private void validateParentIsNotDestination(String projectId, SkillsActionRequest skillReuseRequest) {
        String destParentId = skillReuseRequest.groupId ?: skillReuseRequest.subjectId
        SkillDef skillToReuse = skillDefAccessor.getSkillDef(projectId, skillReuseRequest.skillIds[0])
        SkillDef parent = ruleSetDefGraphService.getParentSkill(skillToReuse)
        if (parent.skillId == destParentId) {
            throw new SkillException("Not allowed to reuse skill into the same ${skillReuseRequest.groupId ? 'group' : 'subject'} [${destParentId}]", projectId, skillToReuse.skillId, ErrorCode.BadParam)
        }
    }

    @Profile
    private void validateSkillsHaveNoDeps(String projectId, SkillsActionRequest skillReuseRequest) {
        skillReuseRequest.skillIds.each {
            Long dependencies = ruleSetDefGraphService.countChildrenSkills(projectId, it, [SkillRelDef.RelationshipType.Dependence])
            if (dependencies && dependencies > 0) {
                throw new SkillException("Skill must have no dependencies in order to reuse; the skill [${it}] has [${dependencies}] dependencie(s)", projectId, it, ErrorCode.BadParam)
            }
        }
    }

    @Profile
    private void validateSkillIsNotDisabled(String projectId, SkillsActionRequest skillReuseRequest) {
        SkillDef skillToReuse = skillDefAccessor.getSkillDef(projectId, skillReuseRequest.skillIds[0])
        if (!Boolean.valueOf(skillToReuse.enabled)) {
            throw new SkillException("Not allowed to reuse a disabled skill", projectId, skillToReuse.skillId, ErrorCode.BadParam)
        }
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

    @Transactional(readOnly = true)
    List<SkillReuseDestination> getReuseDestinationsForASkill(String projectId, String skillId) {
        SkillDef skill = skillAccessor.getSkillDef(projectId, skillId, [SkillDef.ContainerType.Skill])
        List<SkillDef> parentsToExclude = skillRelDefRepo.findParentByChildIdAndTypes(skill.id, [SkillRelDef.RelationshipType.RuleSetDefinition, SkillRelDef.RelationshipType.SkillsGroupRequirement])
        List<SkillDef> allDestSkillDefs = skillDefRepo.findAllByProjectIdAndTypeIn(projectId, [SkillDef.ContainerType.Subject, SkillDef.ContainerType.SkillsGroup])
        List<SkillDef> availableSkillDefs = allDestSkillDefs.findAll({ SkillDef s1 -> !parentsToExclude.find { SkillDef s2 -> s1.skillId == s2.skillId } })
        return availableSkillDefs.collect {
            SkillDef subj, group
            if (it.type == SkillDef.ContainerType.Subject) {
                subj = it
            } else if (it.type == SkillDef.ContainerType.SkillsGroup) {
                subj = ruleSetDefGraphService.getMySubjectParent(it.id)
                group = it
            } else {
                throw new IllegalStateException("Unknown type [${it.type}]")
            }
            new SkillReuseDestination(
                    subjectName: subj.name,
                    subjectId: subj.skillId,
                    groupName: group?.name,
                    groupId: group?.skillId)
        }.sort({ "${it.subjectId}${it.groupId ?: '_'}" })
    }
}
