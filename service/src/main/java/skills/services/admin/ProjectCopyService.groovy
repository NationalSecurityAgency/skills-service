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
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.auth.UserInfo
import skills.auth.UserInfoService
import skills.auth.UserSkillsGrantedAuthority
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.request.model.BadgeRequest
import skills.controller.request.model.EditLevelRequest
import skills.controller.request.model.NextLevelRequest
import skills.controller.request.model.ProjectRequest
import skills.controller.request.model.ProjectSettingsRequest
import skills.controller.request.model.SkillRequest
import skills.controller.request.model.SkillsActionRequest
import skills.controller.request.model.SubjectRequest
import skills.controller.result.model.LevelDefinitionRes
import skills.controller.result.model.SettingsResult
import skills.controller.result.model.SkillDefPartialRes
import skills.services.AccessSettingsStorageService
import skills.services.CreatedResourceLimitsValidator
import skills.services.CustomValidationResult
import skills.services.CustomValidator
import skills.services.LevelDefinitionStorageService
import skills.services.LockingService
import skills.services.admin.skillReuse.SkillReuseService
import skills.services.settings.Settings
import skills.services.settings.SettingsDataAccessor
import skills.services.settings.SettingsService
import skills.storage.model.ProjDef
import skills.storage.model.Setting
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

    @Autowired
    SettingsService settingsService

    @Transactional
    @Profile
    void copyProject(String originalProjectId, ProjectRequest projectRequest) {
        lockingService.lockProjects()

        ProjDef fromProject = loadProject(originalProjectId)
        validate(projectRequest)

        ProjDef toProj = saveToProject(projectRequest)
        saveProjectSettings(fromProject, toProj)

        pinProjectForRootUser(toProj)

        List<SkillInfo> allCollectedSkills = []
        saveSubjectsAndSkills(projectRequest, fromProject, toProj, allCollectedSkills)
        updateProjectAndSubjectLevels(fromProject, toProj)

        saveBadgesAndTheirSkills(fromProject, toProj)
        saveDependencies(fromProject, toProj)
        saveReusedSkills(allCollectedSkills, fromProject, toProj)
    }

    @Profile
    private void pinProjectForRootUser(ProjDef toProj) {
        UserInfo userInfo = userInfoService.getCurrentUser()
        boolean isRoot = userInfo.authorities?.find() {
            it instanceof UserSkillsGrantedAuthority && RoleName.ROLE_SUPER_DUPER_USER == it.role?.roleName
        }
        if (isRoot) {
            projAdminService.pinProjectForRootUser(toProj.projectId)
        }
    }

    @Profile
    private void saveProjectSettings(ProjDef fromProject, toProj) {
        List<SettingsResult> settings = settingsService.loadSettingsForProject(fromProject.projectId)
        settings.each { SettingsResult fromSetting ->
            // copied projects should not be discoverable by default therefore should not carry this setting forward
            boolean discoverableProject = fromSetting.setting == Settings.PRODUCTION_MODE.settingName && fromSetting.value == Boolean.TRUE.toString()
            boolean isLevelsAsPtsSetting = fromSetting.setting == Settings.LEVEL_AS_POINTS.settingName && fromSetting.value == Boolean.TRUE.toString()
            if (!discoverableProject && !isLevelsAsPtsSetting) {
                ProjectSettingsRequest projectSettingsRequest = new ProjectSettingsRequest(
                        projectId: toProj.projectId,
                        settingGroup: fromSetting.settingGroup,
                        setting: fromSetting.setting,
                        value: fromSetting.value,
                )
                settingsService.saveSetting(projectSettingsRequest)
            }
        }
    }

    @Profile
    private void updateProjectAndSubjectLevels(ProjDef fromProject, ProjDef toProj) {
        SettingsResult levelAsPointsSetting = settingsService.loadSettingsForProject(fromProject.projectId).find {
            it.setting == Settings.LEVEL_AS_POINTS.settingName && it.value == Boolean.TRUE.toString()
        }
        if (levelAsPointsSetting) {
            ProjectSettingsRequest projectSettingsRequest = new ProjectSettingsRequest(
                    projectId: toProj.projectId,
                    settingGroup: levelAsPointsSetting.settingGroup,
                    setting: levelAsPointsSetting.setting,
                    value: levelAsPointsSetting.value,
            )
            settingsService.saveSetting(projectSettingsRequest)
        }
        updateLevels(fromProject, toProj)

        List<SkillDefWithExtra> fromSubjects = skillDefWithExtraRepo.findAllByProjectIdAndType(fromProject.projectId, SkillDef.ContainerType.Subject)
        fromSubjects?.findAll { it.enabled }
                .each { SkillDefWithExtra fromSubj ->
                    updateLevels(fromProject, toProj, fromSubj.skillId)
                }
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
        List<LevelDefinitionRes> levelsToUpdate = fromLevels.findAll { it.level <= existingLevels.size() }.sort({ it.level }).reverse()
        List<LevelDefinitionRes> levelsToCreate = fromLevels.findAll { it.level > existingLevels.size() }.sort({ it.level })
        levelsToUpdate.eachWithIndex { LevelDefinitionRes fromLevel, int index ->
            EditLevelRequest editLevelRequest = new EditLevelRequest(
                    percent: fromLevel.percent,
                    name: fromLevel.name,
                    iconClass: fromLevel.iconClass,
                    level: fromLevel.level,
                    pointsFrom: fromLevel.pointsFrom,
                    pointsTo: (index > 0) ? fromLevel.pointsTo : null, // levels are reversed and the highest level must always have pointTo=null
            )
            levelDefinitionStorageService.editLevel(toProj.projectId, editLevelRequest, fromLevel.level, subjectId)
            log.debug("PROJ COPY: [{}]=[{}] subj[{}] - edited level to [{}]", fromProject.projectId, toProj.projectId, subjectId, JsonOutput.toJson(editLevelRequest))
        }
        levelsToCreate.each { LevelDefinitionRes fromlevel ->
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


    private static class SkillInfo {
        SkillDefWithExtra skillDef
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
        List<SkillsDepsService.GraphSkillDefEdge> localOnlyEdges = edges.findAll { SkillsDepsService.GraphSkillDefEdge graphSkillDefEdge ->
            graphSkillDefEdge.to.projectId == fromProject.projectId && graphSkillDefEdge.from.projectId == fromProject.projectId
        }
        localOnlyEdges.each {
            skillsDepsService.assignSkillDependency(toProj.projectId, it.from.skillId, it.to.skillId)
        }
    }

    @Profile
    private void saveBadgesAndTheirSkills(ProjDef fromProject, ProjDef toProj) {
        List<SkillDefWithExtra> badges = skillDefWithExtraRepo.findAllByProjectIdAndType(fromProject.projectId, SkillDef.ContainerType.Badge)
        badges.sort { it.displayOrder }.each { SkillDefWithExtra fromBadge ->
            BadgeRequest badgeRequest = new BadgeRequest()
            Props.copy(fromBadge, badgeRequest)
            badgeRequest.badgeId = fromBadge.skillId
            badgeRequest.enabled = Boolean.FALSE.toString()
            badgeAdminService.saveBadge(toProj.projectId, fromBadge.skillId, badgeRequest)
            List<SkillDefPartialRes> badgeSkills = skillsAdminService.getSkillsForBadge(fromProject.projectId, fromBadge.skillId)
            badgeSkills.each { SkillDefPartialRes fromBadgeSkill ->
                badgeAdminService.addSkillToBadge(toProj.projectId, badgeRequest.badgeId, fromBadgeSkill.skillId)
            }
            // must enable it after the skills were added
            if (fromBadge.enabled == Boolean.TRUE.toString()) {
                badgeRequest.enabled = fromBadge.enabled
                badgeAdminService.saveBadge(toProj.projectId, fromBadge.skillId, badgeRequest)
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
                    createSkills(fromProject.projectId, toProj.projectId, toSubj.subjectId, allCollectedSkills)
                }
    }

    @Profile
    private void createSkills(String originalProjectId, String desProjectId, String subjectId, List<SkillInfo> allCollectedSkills, String groupId = null) {
        String parentId = groupId ?: subjectId
        List<SkillDefWithExtra> skillDefs = skillRelDefRepo.getChildrenWithExtraAttrs(originalProjectId, parentId,
                [SkillRelDef.RelationshipType.RuleSetDefinition, SkillRelDef.RelationshipType.SkillsGroupRequirement])

        allCollectedSkills.addAll(skillDefs.collect { new SkillInfo(skillDef: it, subjectId: subjectId, groupId: groupId) })
        skillDefs?.findAll { it.enabled == "true" && (!it.copiedFrom) }
                .sort { it.displayOrder }
                .each { SkillDefWithExtra fromSkill ->
                    SkillRequest skillRequest = new SkillRequest()
                    Props.copy(fromSkill, skillRequest)
                    skillRequest.projectId = desProjectId
                    skillRequest.subjectId = subjectId
                    skillRequest.type = fromSkill.type?.toString()
                    skillRequest.version = 0
                    skillRequest.selfReportingType = fromSkill.selfReportingType?.toString()
                    if (fromSkill.type != SkillDef.ContainerType.SkillsGroup) {
                        skillRequest.numPerformToCompletion = fromSkill.totalPoints / fromSkill.pointIncrement
                    }

                    // group partial requirement must be set after skills are added
                    Integer groupNumSkillsRequired = -1
                    if (fromSkill.type == SkillDef.ContainerType.SkillsGroup) {
                        groupNumSkillsRequired = fromSkill.numSkillsRequired
                        skillRequest.numSkillsRequired = -1
                    }
                    skillsAdminService.saveSkill(fromSkill.skillId, skillRequest, true, groupId)
                    if (fromSkill.type == SkillDef.ContainerType.SkillsGroup) {
                        createSkills(originalProjectId, desProjectId, subjectId, allCollectedSkills, fromSkill.skillId)
                    }
                    if (groupNumSkillsRequired > 0) {
                        skillRequest.numSkillsRequired = groupNumSkillsRequired
                        skillsAdminService.saveSkill(fromSkill.skillId, skillRequest, true, groupId)
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
    private void validate(ProjectRequest projectRequest) {
        CustomValidationResult customValidationResult = customValidator.validate(projectRequest)
        if (!customValidationResult.valid) {
            throw new SkillException(customValidationResult.msg)
        }
        createdResourceLimitsValidator.validateNumProjectsCreated(userInfoService.getCurrentUserId())
        serviceValidatorHelper.validateProjectIdDoesNotExist(projectRequest.projectId)
        serviceValidatorHelper.validateProjectNameDoesNotExist(projectRequest.name, projectRequest.projectId)
    }

}
