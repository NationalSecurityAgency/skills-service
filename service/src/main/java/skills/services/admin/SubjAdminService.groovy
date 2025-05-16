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
import org.apache.commons.collections4.CollectionUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.request.model.ActionPatchRequest
import skills.controller.request.model.SubjectRequest
import skills.controller.result.model.SubjectOrSkillGroupResult
import skills.controller.result.model.SubjectResult
import skills.services.*
import skills.services.userActions.DashboardAction
import skills.services.userActions.DashboardItem
import skills.services.userActions.UserActionInfo
import skills.services.userActions.UserActionsHistoryService
import skills.storage.accessors.ProjDefAccessor
import skills.storage.model.*
import skills.storage.repos.ProjDefRepo
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.SkillDefWithExtraRepo
import skills.storage.repos.SkillRelDefRepo
import skills.utils.InputSanitizer
import skills.utils.Props

@Service
@Slf4j
class SubjAdminService {

    @Autowired
    ProjDefRepo projDefRepo

    @Autowired
    LevelDefinitionStorageService levelDefService

    @Autowired
    LockingService lockingService

    @Autowired
    CustomValidator customValidator

    @Autowired
    CreatedResourceLimitsValidator createdResourceLimitsValidator

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    SkillRelDefRepo skillRelDefRepo

    @Autowired
    SkillDefWithExtraRepo skillDefWithExtraRepo

    @Autowired
    ProjDefAccessor projDefAccessor

    @Autowired
    GlobalBadgesService globalBadgesService

    @Autowired
    UserAchievementsAndPointsManagement userPointsManagement

    @Autowired
    RuleSetDefGraphService ruleSetDefGraphService

    @Autowired
    RuleSetDefinitionScoreUpdater ruleSetDefinitionScoreUpdater

    @Autowired
    DisplayOrderService displayOrderService

    @Autowired
    SkillsAdminService skillsAdminService

    @Autowired
    AttachmentService attachmentService

    @Autowired
    UserActionsHistoryService userActionsHistoryService

    @Transactional()
    void saveSubject(String projectId, String origSubjectId, SubjectRequest subjectRequest, boolean performCustomValidation = true) {
        lockingService.lockProject(projectId)

        CustomValidationResult customValidationResult = customValidator.validate(subjectRequest, projectId)
        if (performCustomValidation && !customValidationResult.valid) {
            String msg = "Custom validation failed: msg=[${customValidationResult.msg}], type=[subject], subjectId=[${subjectRequest.subjectId}], name=[${subjectRequest.name}], description=[${subjectRequest.description}]"
            throw new SkillException(msg)
        }

        final boolean isEnabledSkillInRequest = Boolean.valueOf(subjectRequest.enabled)

        SkillDefWithExtra existing = skillDefWithExtraRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(projectId, origSubjectId, SkillDef.ContainerType.Subject)

        if (!existing || !existing.skillId.equalsIgnoreCase(subjectRequest.subjectId)) {
            SkillDef idExists = skillDefRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(projectId, subjectRequest.subjectId, SkillDef.ContainerType.Subject)
            if (idExists) {
                throw new SkillException("Subject with id [${subjectRequest.subjectId}] already exists! Sorry!", projectId, null, ErrorCode.ConstraintViolation)
            }
        }
        if (!existing || !existing.name.equalsIgnoreCase(subjectRequest.name)) {
            SkillDef nameExists = skillDefRepo.findByProjectIdAndNameIgnoreCaseAndType(projectId, subjectRequest.name, SkillDef.ContainerType.Subject)
            if (nameExists) {
                throw new SkillException("Subject with name [${subjectRequest.name}] already exists! Sorry!", projectId, null, ErrorCode.ConstraintViolation)
            }
        }

        Boolean isExistingEnabled = Boolean.valueOf(existing?.enabled)
        if (existing && isExistingEnabled && !isEnabledSkillInRequest) {
            throw new SkillException("Cannot disable an existing enabled Subject. SubjectId=[${origSubjectId}]", projectId, null, ErrorCode.BadParam)
        }

        SkillDefWithExtra res
        if (existing) {
            Props.copy(subjectRequest, existing)
            //we need to manually copy subjectId into skillId
            existing.skillId = subjectRequest.subjectId
            DataIntegrityExceptionHandlers.subjectDataIntegrityViolationExceptionHandler.handle(projectId) {
                res = skillDefWithExtraRepo.save(existing)
            }
            if (!isExistingEnabled && isEnabledSkillInRequest) {
                enableSubject(existing)
            }
            log.debug("Updated [{}]", existing)
        } else {
            ProjDef projDef = projDefAccessor.getProjDef(projectId)

            createdResourceLimitsValidator.validateNumSubjectsCreated(projectId)

            Integer lastDisplayOrder = skillDefRepo.calculateHighestDisplayOrderByProjectIdAndType(projectId, SkillDef.ContainerType.Subject)
            int displayOrder = lastDisplayOrder != null ? lastDisplayOrder + 1 : 1
            String enabled = isEnabledSkillInRequest.toString()
            SkillDefWithExtra skillDef = new SkillDefWithExtra(
                    type: SkillDef.ContainerType.Subject,
                    projectId: projectId,
                    skillId: subjectRequest.subjectId,
                    name: subjectRequest?.name,
                    description: subjectRequest?.description,
                    iconClass: subjectRequest?.iconClass ?: "fa fa-question-circle",
                    projRefId: projDef.id,
                    displayOrder: displayOrder,
                    helpUrl: subjectRequest.helpUrl,
                    enabled: enabled,
            )

            DataIntegrityExceptionHandlers.subjectDataIntegrityViolationExceptionHandler.handle(projectId) {
                res = skillDefWithExtraRepo.save(skillDef)
            }
            levelDefService.createDefault(projectId, null, skillDef)

            log.debug("Created [{}]", res)
        }
        attachmentService.updateAttachmentsAttrsBasedOnUuidsInMarkdown(res.description, res.projectId, null, res.skillId)

        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: existing ? DashboardAction.Edit : DashboardAction.Create,
                item: DashboardItem.Subject,
                actionAttributes: res,
                itemId: res.skillId,
                itemRefId: res.id,
                projectId: res.projectId,
        ))
    }

    private void enableSubject(SkillDefWithExtra subject) {
        List<SkillRelDef.RelationshipType> relationshipTypes = [SkillRelDef.RelationshipType.RuleSetDefinition, SkillRelDef.RelationshipType.GroupSkillToSubject]
        skillRelDefRepo.findChildrenByParent(subject.id, relationshipTypes).each { skill ->
            skill.enabled = true
            DataIntegrityExceptionHandlers.skillDataIntegrityViolationExceptionHandler.handle(subject.projectId, subject.skillId) {
                skillDefRepo.save(skill)
            }
            if (skill.type == SkillDef.ContainerType.SkillsGroup) {
                ruleSetDefinitionScoreUpdater.updateGroupDef(skill)
            }
        }
        ruleSetDefinitionScoreUpdater.updateSubjectSkillDef(skillDefRepo.findById(subject.id).get())
        ruleSetDefinitionScoreUpdater.updateProjDef(subject.projectId)
    }

    @Transactional
    void deleteSubject(String projectId, String subjectId) {
        log.debug("Deleting subject with project id [{}] and subject id [{}]", projectId, subjectId)
        SkillDef subjectDefinition = skillDefRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(projectId, subjectId, SkillDef.ContainerType.Subject)
        assert subjectDefinition, "DELETE FAILED -> no subject with project id [$projectId] and subjet id [$subjectId]"
        assert subjectDefinition.type == SkillDef.ContainerType.Subject

        if (globalBadgesService.isSubjectUsedInGlobalBadge(subjectDefinition)) {
            throw new SkillException("Subject with id [${subjectId}] cannot be deleted as it is currently referenced by one or more global badges")
        }
        List<SkillDef> subjectSkills = skillDefRepo.findChildSkillsByIdAndRelationshipType(subjectDefinition.id, SkillRelDef.RelationshipType.RuleSetDefinition)
        subjectSkills.each {
            skillsAdminService.removeCatalogImportedSkills(it)
        }

        List<SkillDef> allSubjectSkills = ruleSetDefGraphService.getChildrenSkills(subjectDefinition, [SkillRelDef.RelationshipType.RuleSetDefinition, SkillRelDef.RelationshipType.GroupSkillToSubject])
        allSubjectSkills.each {
            skillsAdminService.removeCatalogImportedSkills(it)
        }

        ruleSetDefGraphService.deleteSkillWithItsDescendants(subjectDefinition)

        ProjDef projDef = projDefAccessor.getProjDef(projectId)
        // reset display order attribute - make sure the order is continuous - 0...N
        List<SkillDef> subjects = skillDefRepo.findAllByProjectIdAndType(projDef.projectId, SkillDef.ContainerType.Subject)
        subjects = subjects?.findAll({ it.id != subjectDefinition.id }) // need to remove because of JPA level caching?
        displayOrderService.resetDisplayOrder(subjects)

        projDef.totalPoints = CollectionUtils.isEmpty(subjects) ? 0 : subjects.collect({ it.totalPoints }).sum()
        projDefRepo.save(projDef)
        userPointsManagement.handleSubjectRemoval(subjectDefinition)

        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: DashboardAction.Delete,
                item: DashboardItem.Subject,
                itemId: subjectDefinition.skillId,
                projectId: subjectDefinition.projectId,
        ))
        log.debug("Deleted subject with id [{}]", subjectDefinition.skillId)
    }

    @Transactional(readOnly = true)
    SubjectResult getSubject(String projectId, String subjectId) {
        SkillDefWithExtra skillDef = skillDefWithExtraRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(projectId, subjectId, SkillDef.ContainerType.Subject)
        if (!skillDef) {
            throw new SkillException("Subject [${subjectId}] doesn't exist in project [${projectId}]", projectId, null, ErrorCode.SubjectNotFound)
        }
        convertToSubject(skillDef)
    }

    @Transactional(readOnly = true)
    List<SubjectResult> getSubjects(String projectId) {
        List<SkillDefWithExtra> subjects = skillDefWithExtraRepo.findAllByProjectIdAndType(projectId, SkillDef.ContainerType.Subject)
        List<SubjectResult> res = subjects.collect { convertToSubject(it) }
        calculatePercentages(res)
        return res?.sort({ it.displayOrder })
    }

    @Transactional(readOnly = true)
    List<SubjectOrSkillGroupResult> getSubjectsAndSkillGroups(String projectId) {
        List<SkillDef> subjectsAndGroups = skillDefRepo.findAllByProjectIdAndTypeIn(projectId, [SkillDef.ContainerType.Subject, SkillDef.ContainerType.SkillsGroup])

        return subjectsAndGroups?.collect({
            new SubjectOrSkillGroupResult(
                    skillId: it.skillId,
                    name: InputSanitizer.unsanitizeName(it.name),
                    type: it.type
            )
        })?.sort({ it.skillId })
    }

    @Profile
    private SubjectResult convertToSubject(SkillDefWithExtra skillDef) {
        SubjectResult res = new SubjectResult(
                subjectId: skillDef.skillId,
                projectId: skillDef.projectId,
                name: InputSanitizer.unsanitizeName(skillDef.name),
                description: InputSanitizer.unsanitizeForMarkdown(skillDef.description),
                displayOrder: skillDef.displayOrder,
                totalPoints: skillDef.totalPoints,
                iconClass: skillDef.iconClass,
                helpUrl: InputSanitizer.unsanitizeUrl(skillDef.helpUrl),
        )

        SkillCounts skillCounts = getSkillsStatsForSubjects(skillDef)

        res.numGroups = skillCounts.getEnabledGroupsCount() ?: 0
        res.numGroupsDisabled = skillCounts.getDisabledGroupsCount() ?: 0

        res.numSkills = skillCounts.getEnabledSkillsCount() ?: 0
        res.numSkillsDisabled = skillCounts.getDisabledSkillsCount() ?: 0
        res.numSkillsImportedAndDisabled = skillCounts.getDisabledImportedSkillsCount() ?: 0

        res.numSkillsReused = skillCounts.getNumSkillsReused() ?: 0
        res.totalPointsReused = skillCounts.getTotalPointsReused() ?: 0

        res.numSkills -= res.numSkillsReused
        res.totalPoints -= res.totalPointsReused

        res.enabled = skillDef.enabled == "true"

        return res
    }

    @Profile
    private SkillCounts getSkillsStatsForSubjects(SkillDefWithExtra skillDef) {
        skillDefRepo.getSkillsCountsForParentId(skillDef.id)
    }

    @Transactional
    void setSubjectDisplayOrder(String projectId, String subjectId, ActionPatchRequest subjectPatchRequest) {
        lockingService.lockProject(projectId)
        ProjDef projDef = projDefAccessor.getProjDef(projectId)
        if(ActionPatchRequest.ActionType.NewDisplayOrderIndex == subjectPatchRequest.action) {
            List<SkillDef> subjects = skillDefRepo.findAllByProjectIdAndType(projDef.projectId, SkillDef.ContainerType.Subject)
            displayOrderService.updateDisplayOrderByUsingNewIndex(subjectId, subjects, subjectPatchRequest)
        }
    }

    @Transactional(readOnly = true)
    boolean existsBySubjectName(String projectId, String subjectName) {
        return skillDefRepo.existsByProjectIdAndNameAndTypeAllIgnoreCase(projectId, subjectName, SkillDef.ContainerType.Subject)
    }

    @Profile
    private void calculatePercentages(List<SubjectResult> res) {
        // make a shallow copy so we can sort it
        // sorting will make percentage calculation consistent since we don't ask db to sort
        List<SubjectResult> copy = new ArrayList<>(res)
        copy = copy.sort { it.name }
        if (copy) {
            // calculate percentage
            if (copy.size() == 1) {
                copy.first().pointsPercentage =  copy.first().totalPoints > 0 ? 100 : 0;
            } else {
                // init it all to 0
                copy.each {
                    it.pointsPercentage = 0
                }

                // then only consider subjects with points
                List<SubjectResult> withPoints = copy.findAll { it.totalPoints > 0 }
                if (withPoints) {
                    if (withPoints.size() == 1) {
                        // 100% since it has points
                        withPoints.first().pointsPercentage = 100;
                    } else {
                        int overallPoints = withPoints.collect({ it.totalPoints }).sum()
                        List<SubjectResult> withoutLastOne = withPoints[0..withPoints.size() - 2]

                        withoutLastOne.each {
                            it.pointsPercentage = (int) ((it.totalPoints / overallPoints) * 100)
                        }

                        withPoints.last().pointsPercentage = 100 - (withoutLastOne.collect({
                            it.pointsPercentage
                        }).sum())
                    }
                }
            }
        }
    }
}
