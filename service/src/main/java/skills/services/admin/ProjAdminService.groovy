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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.auth.UserInfo
import skills.auth.UserInfoService
import skills.auth.UserSkillsGrantedAuthority
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.request.model.ActionPatchRequest
import skills.controller.request.model.ProjectRequest
import skills.controller.request.model.ProjectSettingsRequest
import skills.controller.request.model.RootUserProjectSettingsRequest
import skills.controller.request.model.SettingsRequest
import skills.controller.result.model.CustomIconResult
import skills.controller.result.model.NumUsersRes
import skills.controller.result.model.ProjectResult
import skills.controller.result.model.SettingsResult
import skills.controller.result.model.SimpleProjectResult
import skills.icons.IconCssNameUtil
import skills.services.*
import skills.services.settings.Settings
import skills.services.settings.SettingsService
import skills.storage.model.CustomIcon
import skills.storage.model.ProjDef
import skills.storage.model.SkillDef
import skills.storage.model.auth.RoleName
import skills.storage.accessors.ProjDefAccessor
import skills.storage.repos.ProjDefRepo
import skills.storage.repos.SkillDefRepo
import skills.utils.ClientSecretGenerator
import skills.utils.Props

@Service
@Slf4j
class ProjAdminService {

    private static final String rootUserPinnedProjectGroup = "pinned_project"

    @Autowired
    ProjDefRepo projDefRepo

    @Autowired
    LevelDefinitionStorageService levelDefService

    @Autowired
    AccessSettingsStorageService accessSettingsStorageService

    @Autowired
    LockingService lockingService

    @Autowired
    CustomValidator customValidator

    @Autowired
    CreatedResourceLimitsValidator createdResourceLimitsValidator

    @Autowired
    UserInfoService userInfoService

    @Autowired
    GlobalBadgesService globalBadgesService

    @Autowired
    ProjDefAccessor projDefAccessor

    @Autowired
    ProjectSortingService sortingService

    @Autowired
    SettingsService settingsService

    @Autowired
    SkillDefRepo skillDefRepo

    @Transactional()
    void saveProject(String originalProjectId, ProjectRequest projectRequest, String userIdParam = null) {
        assert projectRequest?.projectId
        assert projectRequest?.name

        lockingService.lockProjects()

        CustomValidationResult customValidationResult = customValidator.validate(projectRequest)
        if (!customValidationResult.valid) {
            throw new SkillException(customValidationResult.msg)
        }

        ProjDef projectDefinition = originalProjectId ? projDefRepo.findByProjectIdIgnoreCase(originalProjectId) : null
        if (!projectDefinition || !projectRequest.projectId.equalsIgnoreCase(originalProjectId)) {
            ProjDef idExist = projDefRepo.findByProjectIdIgnoreCase(projectRequest.projectId)
            if (idExist) {
                throw new SkillException("Project with id [${projectRequest.projectId}] already exists! Sorry!", projectRequest.projectId, null, ErrorCode.ConstraintViolation)
            }
        }
        if (!projectDefinition || !projectRequest.name.equalsIgnoreCase(projectDefinition.name)) {
            ProjDef nameExist = projDefRepo.findByNameIgnoreCase(projectRequest.name)
            if (nameExist) {
                throw new SkillException("Project with name [${projectRequest.name}] already exists! Sorry!", projectRequest.projectId, null, ErrorCode.ConstraintViolation)
            }
        }
        if (projectDefinition) {
            Props.copy(projectRequest, projectDefinition)
            log.debug("Updating [{}]", projectDefinition)

            DataIntegrityExceptionHandlers.dataIntegrityViolationExceptionHandler.handle(projectDefinition.projectId) {
                projectDefinition = projDefRepo.save(projectDefinition)
            }
            log.debug("Saved [{}]", projectDefinition)
        } else {
            // TODO: temp hack around since user is not yet defined when Inception project is created
            // This will be addressed in ticket #139
            String clientSecret = new ClientSecretGenerator().generateClientSecret()

            projectDefinition = new ProjDef(projectId: projectRequest.projectId, name: projectRequest.name,
                    clientSecret: clientSecret)
            log.debug("Created project [{}]", projectDefinition)

            createdResourceLimitsValidator.validateNumProjectsCreated(userIdParam ?: userInfoService.getCurrentUserId())

            DataIntegrityExceptionHandlers.dataIntegrityViolationExceptionHandler.handle(projectDefinition.projectId) {
                projectDefinition = projDefRepo.save(projectDefinition)
            }

            log.debug("Saved [{}]", projectDefinition)

            levelDefService.createDefault(projectRequest.projectId, projectDefinition)

            accessSettingsStorageService.addUserRole(userIdParam ?: userInfoService.getCurrentUserId(), projectRequest.projectId, RoleName.ROLE_PROJECT_ADMIN)
            log.debug("Added user role [{}]", RoleName.ROLE_PROJECT_ADMIN)
        }
    }

    @Transactional()
    void deleteProject(String projectId) {
        log.debug("Deleting project with id [{}]", projectId)
        if (!existsByProjectId(projectId)) {
            throw new SkillException("Project with id [${projectId}] does NOT exist")
        }

        if (globalBadgesService.isProjectUsedInGlobalBadge(projectId)) {
            throw new SkillException("Project with id [${projectId}] cannot be deleted as it is currently referenced by one or more global badges")
        }

        projDefRepo.deleteByProjectIdIgnoreCase(projectId)
        log.debug("Deleted project with id [{}]", projectId)
    }

    @Transactional()
    void pinProjectForRootUser(String projectId) {
        if (!existsByProjectId(projectId)) {
            throw new SkillException("Project with id [${projectId}] does NOT exist")
        }

        RootUserProjectSettingsRequest settingsRequest = new RootUserProjectSettingsRequest(
                projectId:  projectId,
                settingGroup: rootUserPinnedProjectGroup,
                setting: "pinned",
                value: projectId
        )

        settingsService.saveSetting(settingsRequest)
    }

    @Transactional()
    void unpinProjectForRootUser(String projectId) {
        if (existsByProjectId(projectId)) {
            settingsService.deleteRootUserSetting("pinned", projectId)
        }
    }

    private  List<ProjectResult> loadProjectsForRoot(Map<String, Integer> projectIdSortOrder) {
        List<SettingsResult> pinnedProjectSettings = settingsService.getRootUserSettingsForGroup(rootUserPinnedProjectGroup)
        List<String> pinnedProjects = pinnedProjectSettings.collect { it.value }

        List<ProjDef> projects = projDefRepo.findAllByProjectIdIn(pinnedProjects)
        Set<String> pinnedProjectIds = pinnedProjects.toSet()

        List<ProjectResult> finalRes = projects?.unique({ it.projectId })?.collect({
            ProjectResult res = convert(it, projectIdSortOrder, pinnedProjectIds)
            return res
        })

        return finalRes
    }

    @Transactional(readOnly = true)
    List<ProjectResult> searchByProjectName(String search) {
        validateRootUser();
        List<ProjDef> projects = projDefRepo.findByNameLike(search)
        return convertProjectsWithPinnedIndicator(projects)
    }

    @Transactional(readOnly = true)
    List<ProjectResult> getAllProjects() {
        validateRootUser();
        List<ProjDef> projects = projDefRepo.findAll()
        return convertProjectsWithPinnedIndicator(projects)
    }

    private List<ProjectResult> convertProjectsWithPinnedIndicator(List<ProjDef> projects) {
        Map<String, Integer> projectIdSortOrder = [:]
        List<SettingsResult> pinnedProjectSettings = settingsService.getRootUserSettingsForGroup(rootUserPinnedProjectGroup)
        List<String> pinnedProjects = pinnedProjectSettings.collect { it.value }
        return projects?.unique({ it.projectId })?.collect({
            return convert(it, projectIdSortOrder, pinnedProjects?.toSet())
        })
    }

    private validateRootUser(){
        UserInfo userInfo = userInfoService.getCurrentUser()
        boolean isRoot = userInfo.authorities?.find() {
            it instanceof UserSkillsGrantedAuthority && RoleName.ROLE_SUPER_DUPER_USER == it.role?.roleName
        }
        assert isRoot
    }

    @Transactional(readOnly = true)
    List<ProjectResult> getProjects() {
        UserInfo userInfo = userInfoService.getCurrentUser()
        boolean isRoot = userInfo.authorities?.find() {
            it instanceof UserSkillsGrantedAuthority && RoleName.ROLE_SUPER_DUPER_USER == it.role?.roleName
        }

        String userId = userInfo.username
        Map<String, Integer> projectIdSortOrder = sortingService.getUserProjectsOrder(userId)

        List<ProjectResult> finalRes
        if (isRoot) {
            finalRes = loadProjectsForRoot(projectIdSortOrder)
        } else {
            // sql join with UserRoles and there is 1-many relationship that needs to be normalized
            List<ProjDef> projects = projDefRepo.getProjectsByUser(userId)
            finalRes = projects?.unique({ it.projectId })?.collect({
                ProjectResult res = convert(it, projectIdSortOrder)
                return res
            })
        }

        finalRes.sort() { it.displayOrder }

        if (finalRes) {
            finalRes.first().isFirst = true
            finalRes.last().isLast = true
        }

        return finalRes
    }

    @Transactional(readOnly = true)
    ProjectResult getProject(String projectId) {
        ProjDef projectDefinition = projDefAccessor.getProjDef(projectId)
        Integer order = sortingService.getProjectSortOrder(projectId)
        ProjectResult res = convert(projectDefinition, [(projectId): order])
        return res
    }


    @Transactional(readOnly = true)
    boolean existsByProjectId(String projectId) {
        return projDefRepo.existsByProjectIdIgnoreCase(projectId)
    }

    @Transactional(readOnly = true)
    boolean existsByProjectName(String projectName) {
        return projDefRepo.existsByNameIgnoreCase(projectName)
    }


    @Transactional(readOnly = true)
    long countNumberOfSkills(String projectId) {
        return skillDefRepo.countByProjectIdAndType(projectId, SkillDef.ContainerType.Skill)
    }

    @Transactional()
    void setProjectDisplayOrder(String projectId, ActionPatchRequest projectPatchRequest) {
        assert projectPatchRequest.action

        switch (projectPatchRequest.action) {
            case ActionPatchRequest.ActionType.DisplayOrderDown:
                sortingService.changeProjectOrder(projectId, ProjectSortingService.Move.DOWN)
                break;
            case ActionPatchRequest.ActionType.DisplayOrderUp:
                sortingService.changeProjectOrder(projectId, ProjectSortingService.Move.UP)
                break;
            default:
                throw new IllegalArgumentException("Unknown action ${projectPatchRequest.action}")
        }
    }

    @Transactional()
    List<SimpleProjectResult> searchProjects(String projectId, String nameQuery) {
        List<ProjDef> projDefs = projDefRepo.queryProjectsByNameQueryAndNotProjectId(nameQuery.toLowerCase(), projectId, PageRequest.of(0, 5, Sort.Direction.ASC, "name"))
        return projDefs.collect {
            new SimpleProjectResult(name: it.name, projectId: it.projectId)
        }
    }

    @Transactional(readOnly = true)
    String getProjectSecret(String projectId) {
        ProjDef projectDefinition = projDefAccessor.getProjDef(projectId)
        return projectDefinition.clientSecret
    }

    @Transactional
    void updateClientSecret(String projectId, String clientSecret) {
        ProjDef projDef = projDefAccessor.getProjDef(projectId)
        projDef.clientSecret = clientSecret
    }

    @Transactional(readOnly = true)
    List<CustomIconResult> getCustomIcons(String projectId){
        ProjDef project = projDefAccessor.getProjDef(projectId)
        return project.getCustomIcons().collect { CustomIcon icon ->
            String cssClassname = IconCssNameUtil.getCssClass(icon.projectId, icon.filename)
            return new CustomIconResult(filename: icon.filename, cssClassname: cssClassname)
        }
    }

    @Profile
    private ProjectResult convert(ProjDef definition, Map<String, Integer> projectIdSortOrder, Set<String> pinnedProjectIds = []) {
        Integer order = projectIdSortOrder?.get(definition.projectId)
        ProjectResult res = new ProjectResult(
                projectId: definition.projectId, name: definition.name, totalPoints: definition.totalPoints,
                numSubjects: definition.subjects ? definition.subjects.size() : 0,
                displayOrder: order != null ? order : 0,
                pinned: pinnedProjectIds?.contains(definition.projectId),
        )
        res.numBadges = skillDefRepo.countByProjectIdAndType(definition.projectId, SkillDef.ContainerType.Badge)
        res.numSkills = countNumSkillsForProject(definition)
        SettingsResult result = settingsService.getProjectSetting(definition.projectId, Settings.LEVEL_AS_POINTS.settingName)

        if (result == null || result.value == "false") {
            res.levelsArePoints = false
        } else if (result?.value == "true") {
            res.levelsArePoints = true
        }

        res
    }

    @Profile
    private long countNumSkillsForProject(ProjDef definition) {
        skillDefRepo.countByProjectIdAndType(definition.projectId, SkillDef.ContainerType.Skill)
    }
}
