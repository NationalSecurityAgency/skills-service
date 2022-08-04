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
import groovy.json.JsonOutput
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.auth.UserInfoService
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.request.model.BadgeRequest
import skills.controller.request.model.EditLevelRequest
import skills.controller.request.model.NextLevelRequest
import skills.controller.request.model.ProjectRequest
import skills.controller.request.model.SkillRequest
import skills.controller.request.model.SkillsActionRequest
import skills.controller.request.model.SubjectRequest
import skills.controller.result.model.LevelDefinitionRes
import skills.controller.result.model.SkillDefPartialRes
import skills.services.AccessSettingsStorageService
import skills.services.CreatedResourceLimitsValidator
import skills.services.CustomValidationResult
import skills.services.CustomValidator
import skills.services.LevelDefinitionStorageService
import skills.services.LockingService
import skills.services.admin.skillReuse.SkillReuseService
import skills.storage.model.ProjDef
import skills.storage.model.SkillDef
import skills.storage.model.SkillDefWithExtra
import skills.storage.model.SkillRelDef
import skills.storage.model.auth.RoleName
import skills.storage.repos.ProjDefRepo
import skills.storage.repos.SkillDefWithExtraRepo
import skills.storage.repos.SkillRelDefRepo
import skills.utils.ClientSecretGenerator
import skills.utils.Props

@Service
@Slf4j
class ProjectCopyService {

    @Autowired
    CustomValidator customValidator

    @Autowired
    LockingService lockingService

    @Autowired
    ServiceValidatorHelper serviceValidatorHelper

    @Autowired
    ProjDefRepo projDefRepo

    @Autowired
    CreatedResourceLimitsValidator createdResourceLimitsValidator

    @Autowired
    UserInfoService userInfoService

    @Autowired
    SubjAdminService subjAdminService

    @Autowired
    SkillDefWithExtraRepo skillDefWithExtraRepo

    @Autowired
    SkillsAdminService skillsAdminService

    @Autowired
    SkillRelDefRepo skillRelDefRepo

    @Autowired
    AccessSettingsStorageService accessSettingsStorageService

    @Autowired
    BadgeAdminService badgeAdminService

    @Autowired
    SkillsDepsService skillsDepsService

    @Autowired
    SkillReuseService skillReuseService

    @Autowired
    ProjAdminService projAdminService

    @Autowired
    LevelDefinitionStorageService levelDefinitionStorageService

    @Transactional
    @Profile
    void copyProject(String originalProjectId, ProjectRequest projectRequest, String userIdParam = null) {
        lockingService.lockProjects()

        ProjDef fromProject = loadProject(originalProjectId)
        validate(projectRequest, userIdParam)

        ProjDef toProj = saveToProject(projectRequest)

        updateLevels(fromProject, toProj)

        List<SkillInfo> allCollectedSkills = []
        saveSubjectsAndSkills(projectRequest, fromProject, toProj, allCollectedSkills)
        saveBadgesAndTheirSkills(fromProject, toProj)
        saveDependencies(fromProject, toProj)
        saveReusedSkills(allCollectedSkills, fromProject, toProj)
    }

    @Profile
    private void updateLevels(ProjDef fromProject, ProjDef toProj, String subjectId = null) {
        List<LevelDefinitionRes> fromLevels = levelDefinitionStorageService.getLevels(fromProject.projectId, subjectId).sort({ it.level })
        List<LevelDefinitionRes> existingLevels = levelDefinitionStorageService.getLevels(toProj.projectId, subjectId).sort({ it.level })
        int levelsToRemove = Math.max(0, existingLevels.size() - fromLevels.size())

        if (levelsToRemove > 0) {
            (levelsToRemove).times {
                try {
                    levelDefinitionStorageService.deleteLastLevel(toProj.projectId, subjectId)
                    log.debug("PROJ COPY: [{}]=[{}] subj[{}] - removed last level", fromProject.projectId, toProj.projectId, subjectId)
                } catch (Throwable t) {
                    throw new IllegalStateException("Failed to remove last level for proj=[${toProj.projectId}] and subjectId=[${subjectId}]", t)
                }
            }
        }
        fromLevels.eachWithIndex { LevelDefinitionRes fromlevel, int i ->
            if (i < existingLevels.size()) {
                LevelDefinitionRes toLevel = existingLevels[i]
                EditLevelRequest editLevelRequest = new EditLevelRequest(
                        percent: fromlevel.percent,
                        name: fromlevel.name,
                        iconClass: fromlevel.iconClass,
                        level: fromlevel.level,
                        pointsFrom: fromlevel.pointsFrom,
                        pointsTo: fromlevel.pointsTo,
                )
                levelDefinitionStorageService.editLevel(toProj.projectId, editLevelRequest, fromlevel.level, subjectId)
                log.debug("PROJ COPY: [{}]=[{}] subj[{}] - edited level to [{}]", fromProject.projectId, toProj.projectId, subjectId, JsonOutput.toJson(editLevelRequest))
            } else {
                NextLevelRequest nextLevelRequest = new NextLevelRequest(
                        percent: fromlevel.percent,
                        points: fromlevel.pointsFrom,
                        name: fromlevel.name,
                        iconClass: fromlevel.iconClass,
                )
                levelDefinitionStorageService.addNextLevel(toProj.projectId, nextLevelRequest, subjectId)
                log.debug("PROJ COPY: [{}]=[{}] subj[{}] - new level [{}]", fromProject.projectId, toProj.projectId, subjectId, JsonOutput.toJson(nextLevelRequest))
            }
        }
    }

    private static class SkillInfo {
        SkillDef skillDef
        String subjectId
        String groupId
    }

    private static class ReuseOperation {
        SkillInfo from
        SkillInfo to
    }

    @Profile
    private void saveReusedSkills(List<SkillInfo> allCollectedSkills, ProjDef fromProj, ProjDef toProj) {
        List<SkillInfo> reusedSkills = allCollectedSkills.findAll { it.skillDef.copiedFrom && it.skillDef.copiedFromProjectId == fromProj.projectId }
        if (reusedSkills) {
            Map<Integer, List<SkillInfo>> bySkillRefId = allCollectedSkills.groupBy { it.skillDef.id }
            List<ReuseOperation> reuseOperations = []
            reusedSkills.each { SkillInfo toReuse ->
                SkillInfo fromReuse = bySkillRefId[toReuse.skillDef.copiedFrom].first()
                reuseOperations.add(new ReuseOperation(from: fromReuse, to: toReuse))
            }

            Map<String, List<ReuseOperation>> reuseOperationsByParent = reuseOperations.groupBy {
                String fromParentId = it.from.groupId ?: it.from.subjectId
                String toParentId = it.to.groupId ?: it.to.subjectId
                return "${fromParentId}->${toParentId}"
            }
            reuseOperationsByParent.each {
                skillReuseService.reuseSkill(toProj.projectId,
                        new SkillsActionRequest(
                                skillIds: it.value.collect { it.from.skillDef.skillId },
                                subjectId: it.value[0].to.subjectId,
                                groupId: it.value[0].to.groupId,
                        ))
            }
        }
    }

    @Profile
    private void saveDependencies(ProjDef fromProject, toProj) {
        List<SkillsDepsService.GraphSkillDefEdge> edges = skillsDepsService.loadGraphEdges(fromProject.projectId, SkillRelDef.RelationshipType.Dependence)
        edges.each {
            skillsDepsService.assignSkillDependency(toProj.projectId, it.from.skillId, it.to.skillId)
        }
    }

    @Profile
    private void saveBadgesAndTheirSkills(ProjDef fromProject, ProjDef toProj) {
        List<SkillDefWithExtra> badges = skillDefWithExtraRepo.findAllByProjectIdAndType(fromProject.projectId, SkillDef.ContainerType.Badge)
        badges.each { SkillDefWithExtra fromBadge ->
            BadgeRequest badgeRequest = new BadgeRequest()
            Props.copy(fromBadge, badgeRequest)
            badgeRequest.badgeId = fromBadge.skillId
            badgeAdminService.saveBadge(toProj.projectId, fromBadge.skillId, badgeRequest)
            List<SkillDefPartialRes> badgeSkills = skillsAdminService.getSkillsForBadge(fromProject.projectId, fromBadge.skillId)
            badgeSkills.each { SkillDefPartialRes fromBadgeSkill ->
                badgeAdminService.addSkillToBadge(toProj.projectId, badgeRequest.badgeId, fromBadgeSkill.skillId)
            }
        }
    }

    @Profile
    private void saveSubjectsAndSkills(ProjectRequest projectRequest, ProjDef fromProject, ProjDef toProj, List<SkillInfo> allCollectedSkills) {
        List<SkillDefWithExtra> fromSubjects = skillDefWithExtraRepo.findAllByProjectIdAndType(fromProject.projectId, SkillDef.ContainerType.Subject)
        fromSubjects?.findAll { it.enabled }
                .sort { it.displayOrder }
                .each { SkillDefWithExtra fromSubj ->
                    SubjectRequest toSubj = new SubjectRequest()
                    Props.copy(fromSubj, toSubj)
                    toSubj.subjectId = fromSubj.skillId
                    subjAdminService.saveSubject(projectRequest.projectId, fromSubj.skillId, toSubj)
                    log.info("PROJ COPY: [{}]=[{}] subj[{}] - created new subject")
                    updateLevels(fromProject, toProj, fromSubj.skillId)

                    createSkills(fromProject.projectId, toProj.projectId, toSubj.subjectId, allCollectedSkills)
                }
    }

    @Profile
    private void createSkills(String originalProjectId, String desProjectId, String subjectId, List<SkillInfo> allCollectedSkills, String groupId = null) {
        String parentId = groupId ?: subjectId
        List<SkillDef> skillDefs = skillRelDefRepo.getChildren(originalProjectId, parentId,
                [SkillRelDef.RelationshipType.RuleSetDefinition, SkillRelDef.RelationshipType.SkillsGroupRequirement])

        allCollectedSkills.addAll(skillDefs.collect { new SkillInfo(skillDef: it, subjectId: subjectId, groupId: groupId) })
        skillDefs?.findAll { it.enabled == "true" && (!it.copiedFrom) }
                .sort { it.displayOrder }
                .each { SkillDef fromSkill ->
                    SkillRequest skillRequest = new SkillRequest()
                    Props.copy(fromSkill, skillRequest)
                    skillRequest.projectId = desProjectId
                    skillRequest.subjectId = subjectId
                    skillRequest.type = fromSkill.type.toString()
                    if (fromSkill.type != SkillDef.ContainerType.SkillsGroup) {
                        skillRequest.numPerformToCompletion = fromSkill.totalPoints / fromSkill.pointIncrement
                    }

                    skillsAdminService.saveSkill(fromSkill.skillId, skillRequest, true, groupId)
                    if (fromSkill.type == SkillDef.ContainerType.SkillsGroup) {
                        createSkills(originalProjectId, desProjectId, subjectId, allCollectedSkills, fromSkill.skillId)
                    }
                }
    }

    @Profile
    private ProjDef saveToProject(ProjectRequest projectRequest) {
        projAdminService.saveProject(null, projectRequest)
        ProjDef toProj = projDefRepo.findByProjectId(projectRequest.projectId)
        return toProj
    }

    @Profile
    private ProjDef loadProject(String projectId) {
        ProjDef res = projDefRepo.findByProjectIdIgnoreCase(projectId)
        if (!res) {
            throw new SkillException("Project with id [${projectId}] does not exist", projectId, null, ErrorCode.BadParam)
        }
        return res
    }

    @Profile
    private void validate(ProjectRequest projectRequest, String userIdParam) {
        CustomValidationResult customValidationResult = customValidator.validate(projectRequest)
        if (!customValidationResult.valid) {
            throw new SkillException(customValidationResult.msg)
        }
        createdResourceLimitsValidator.validateNumProjectsCreated(userIdParam ?: userInfoService.getCurrentUserId())
        serviceValidatorHelper.validateProjectIdDoesNotExist(projectRequest.projectId)
        serviceValidatorHelper.validateProjectNameDoesNotExist(projectRequest.name, projectRequest.projectId)
    }
}
