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
import org.apache.commons.collections.CollectionUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.request.model.ActionPatchRequest
import skills.controller.request.model.SubjectRequest
import skills.controller.result.model.SubjectResult
import skills.services.*
import skills.storage.model.ProjDef
import skills.storage.model.SkillDef
import skills.storage.model.SkillDefParent
import skills.storage.model.SkillDefWithExtra
import skills.storage.model.SkillRelDef
import skills.storage.accessors.ProjDefAccessor
import skills.storage.repos.ProjDefRepo
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.SkillDefWithExtraRepo
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
    DisplayOrderService displayOrderService

    @Transactional()
    void saveSubject(String projectId, String origSubjectId, SubjectRequest subjectRequest, boolean performCustomValidation = true) {
        lockingService.lockProject(projectId)

        CustomValidationResult customValidationResult = customValidator.validate(subjectRequest)
        if (performCustomValidation && !customValidationResult.valid) {
            String msg = "Custom validation failed: msg=[${customValidationResult.msg}], type=[subject], subjectId=[${subjectRequest.subjectId}], name=[${subjectRequest.name}], description=[${subjectRequest.description}]"
            throw new SkillException(msg)
        }

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

        SkillDefWithExtra res
        if (existing) {
            Props.copy(subjectRequest, existing)
            //we need to manually copy subjectId into skillId
            existing.skillId = subjectRequest.subjectId
            DataIntegrityExceptionHandlers.subjectDataIntegrityViolationExceptionHandler.handle(projectId) {
                res = skillDefWithExtraRepo.save(existing)
            }
            log.debug("Updated [{}]", existing)
        } else {
            ProjDef projDef = projDefAccessor.getProjDef(projectId)

            createdResourceLimitsValidator.validateNumSubjectsCreated(projectId)

            Integer lastDisplayOrder = skillDefRepo.calculateHighestDisplayOrderByProjectIdAndType(projectId, SkillDef.ContainerType.Subject)
            int displayOrder = lastDisplayOrder != null ? lastDisplayOrder + 1 : 0

            SkillDefWithExtra skillDef = new SkillDefWithExtra(
                    type: SkillDef.ContainerType.Subject,
                    projectId: projectId,
                    skillId: subjectRequest.subjectId,
                    name: subjectRequest?.name,
                    description: subjectRequest?.description,
                    iconClass: subjectRequest?.iconClass ?: "fa fa-question-circle",
                    projDef: projDef,
                    displayOrder: displayOrder,
                    helpUrl: subjectRequest.helpUrl
            )

            DataIntegrityExceptionHandlers.subjectDataIntegrityViolationExceptionHandler.handle(projectId) {
                res = skillDefWithExtraRepo.save(skillDef)
            }
            levelDefService.createDefault(projectId, null, skillDef)

            log.debug("Created [{}]", res)
        }
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

        ruleSetDefGraphService.deleteSkillWithItsDescendants(subjectDefinition)

        ProjDef projDef = projDefAccessor.getProjDef(projectId)
        // reset display order attribute - make sure the order is continuous - 0...N
        List<SkillDef> subjects = projDef.subjects
        subjects = subjects?.findAll({ it.id != subjectDefinition.id }) // need to remove because of JPA level caching?
        displayOrderService.resetDisplayOrder(subjects)

        projDef.totalPoints = CollectionUtils.isEmpty(subjects) ? 0 : subjects.collect({ it.totalPoints }).sum()
        projDefRepo.save(projDef)
        userPointsManagement.handleSubjectRemoval(subjectDefinition)

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


    @Profile
    private SubjectResult convertToSubject(SkillDefWithExtra skillDef) {
        SubjectResult res = new SubjectResult(
                subjectId: skillDef.skillId,
                projectId: skillDef.projectId,
                name: skillDef.name,
                description: InputSanitizer.unsanitizeForMarkdown(skillDef.description),
                displayOrder: skillDef.displayOrder,
                totalPoints: skillDef.totalPoints,
                iconClass: skillDef.iconClass,
                helpUrl: skillDef.helpUrl
        )

        res.numSkills = calculateNumChildSkills(skillDef)
        return res
    }

    @Transactional
    void setSubjectDisplayOrder(String projectId, String subjectId, ActionPatchRequest subjectPatchRequest) {
        lockingService.lockProject(projectId)
        ProjDef projDef = projDefAccessor.getProjDef(projectId)
        displayOrderService.updateDisplayOrder(subjectId, projDef.subjects, subjectPatchRequest)
    }

    @Transactional(readOnly = true)
    boolean existsBySubjectName(String projectId, String subjectName) {
        return skillDefRepo.existsByProjectIdAndNameAndTypeAllIgnoreCase(projectId, subjectName, SkillDef.ContainerType.Subject)
    }


    @Profile
    private long calculateNumChildSkills(SkillDefParent skillDef) {
        skillDefRepo.countChildSkillsByIdAndRelationshipType(skillDef.id, SkillRelDef.RelationshipType.RuleSetDefinition)
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
                copy.first().pointsPercentage = 100
            } else {
                int overallPoints = copy.collect({ it.totalPoints }).sum()
                if (overallPoints == 0) {
                    copy.each {
                        it.pointsPercentage = 0
                    }
                } else {
                    List<SubjectResult> withoutLastOne = copy[0..copy.size() - 2]

                    withoutLastOne.each {
                        it.pointsPercentage = (int) ((it.totalPoints / overallPoints) * 100)
                    }
                    copy.last().pointsPercentage = 100 - (withoutLastOne.collect({ it.pointsPercentage }).sum())
                }
            }
        }
    }
}
