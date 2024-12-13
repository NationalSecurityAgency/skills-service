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
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.auth.UserInfo
import skills.auth.UserInfoService
import skills.auth.UserSkillsGrantedAuthority
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.request.model.*
import skills.controller.result.model.CopyValidationRes
import skills.controller.result.model.LevelDefinitionRes
import skills.controller.result.model.SettingsResult
import skills.controller.result.model.SkillDefPartialRes
import skills.icons.CustomIconFacade
import skills.services.*
import skills.services.admin.skillReuse.SkillReuseService
import skills.services.settings.Settings
import skills.services.settings.SettingsService
import skills.storage.model.*
import skills.storage.model.auth.RoleName
import skills.storage.repos.*
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
    QuizToSkillDefRepo quizToSkillDefRepo

    @Autowired
    UserQuizAttemptRepo userQuizAttemptRepo

    @Autowired
    BatchOperationsTransactionalAccessor batchOperationsTransactionalAccessor

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

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    CustomIconFacade customIconFacade

    @Autowired
    SkillAttributesDefRepo skillAttributesDefRepo

    @PersistenceContext
    EntityManager entityManager;

    @Autowired
    AttachmentService attachmentService

    @Autowired
    UserRoleRepo userRoleRepo

    @Autowired
    UserCommunityService userCommunityService

    @Transactional
    @Profile
    void copyItemsToAnotherProject(String fromProjectId, String toProjectId, CopyToAnotherProjectRequest copyRequest) {
        if (copyRequest.copyType == CopyToAnotherProjectRequestType.SelectSkills) {
            copySkillsToAnotherProject(fromProjectId, copyRequest.skillIds, toProjectId, copyRequest.toSubjectId, copyRequest.toGroupId)
        } else {
            copySubjectToAnotherProject(fromProjectId, copyRequest.fromSubjectId, toProjectId)
        }
    }

    @Transactional
    @Profile
    CopyValidationRes validateCopyItemsToAnotherProject(String fromProjectId, String toProjectId, CopyToAnotherProjectRequest copyRequest) {
        if (copyRequest.copyType == CopyToAnotherProjectRequestType.SelectSkills) {
            return validateCopySkillsToAnotherProject(fromProjectId, copyRequest.skillIds, toProjectId, copyRequest.toSubjectId, copyRequest.toGroupId)
        }
        return validateCopySubjectToAnotherProject(fromProjectId, copyRequest.fromSubjectId, toProjectId)
    }

    @Profile
    private CopyValidationRes validateCopySkillsToAnotherProject(String projectId, List<String> skillIds, String otherProjectId, String otherSubjectId, String otherGroupId = null ) {
        List<SkillDefWithExtra> itemsToCopy
        try {
            ProjDef fromProject = loadProject(projectId)
            ProjDef otherProject = loadProject(otherProjectId)
            validateProjectsCommunityStatus(fromProject, otherProject)
            loadSubject(otherProject.projectId, otherSubjectId)
            loadSkillGroup(otherProject.projectId, otherGroupId)
            itemsToCopy = getSkillsToCopy(projectId, skillIds)
        } catch (SkillException skillException) {
            return new CopyValidationRes(isAllowed: false, validationErrors: [skillException.getMessage()?.toString()])
        }

        List<String> validationErrors = checkForSkillIdAndNameCollisions(itemsToCopy.collect { new SkillIdAndName(skillId: it.skillId, skillName: it.name) }, otherProjectId)
        CopyValidationRes res = new CopyValidationRes(isAllowed: validationErrors?.isEmpty(), validationErrors: validationErrors)
        return res
    }

    @Profile
    private void copySkillsToAnotherProject(String projectId, List<String> skillIds, String otherProjectId, String otherSubjectId, String otherGroupId = null ) {
        ProjDef otherProject = loadProject(otherProjectId)
        lockingService.lockProject(otherProject.projectId)
        copiedAttachmentUuidsThreadLocal.set([:])
        try {
            ProjDef fromProject = loadProject(projectId)
            validateProjectsCommunityStatus(fromProject, otherProject)
            validateUserIsAndAdminOfDestProj(otherProject, projectId)

            if (otherGroupId) {
                SkillDef skillGroup = loadSkillGroup(otherProject.projectId, otherGroupId)
                if (!otherSubjectId) {
                    List<SkillDef> subjectParent = skillRelDefRepo.findParentByChildIdAndTypes(skillGroup.id, [SkillRelDef.RelationshipType.RuleSetDefinition])
                    if (subjectParent) {
                        otherSubjectId = subjectParent.first().skillId
                    }
                }
            }

            SkillDefWithExtra destinationSubject = loadSubject(otherProject.projectId, otherSubjectId)

            List<SkillDefWithExtra> skillDefs = getSkillsToCopy(projectId, skillIds)

            List<SkillInfo> allCollectedSkills = []
            List<SkillDefWithExtra> skillDefsSorted = skillDefs.sort { skillIds.indexOf(it.skillId) }
            createSkills(skillDefsSorted, fromProject.projectId, otherProject.projectId, destinationSubject.skillId, allCollectedSkills, otherGroupId, true)
        } finally {
            copiedAttachmentUuidsThreadLocal.set([:])
        }
    }

    @Profile
    private List<SkillDefWithExtra> getSkillsToCopy(String projectId, List<String> skillIds) {
        List<SkillDefWithExtra> skillDefs = skillDefWithExtraRepo.findAllByProjectIdAndSkillIdIn(projectId, skillIds)
        if (skillDefs.size() != skillIds.size()) {
            throw new SkillException("Not all provided skill were loaded, missing skill ids are: ${skillIds - skillDefs.collect { it.skillId }}", projectId, null, ErrorCode.BadParam)
        }
        List<SkillDefWithExtra> importedSkills = skillDefs.findAll { it.copiedFromProjectId != null }
        if (importedSkills) {
            throw new SkillException("Can't copy imported skills, following skills were imported: ${importedSkills.collect { it.skillId }.sort()}", projectId, null, ErrorCode.BadParam)
        }
        return skillDefs
    }

    @Profile
    private SkillDef loadSkillGroup(String projectId, String groupId) {
        if (groupId) {
            SkillDef skillGroup = skillDefRepo.findByProjectIdAndSkillIdAndType(projectId, groupId, SkillDef.ContainerType.SkillsGroup)
            if (!skillGroup) {
                throw new SkillException("Group with id [${groupId}] does not exist.", projectId, null, ErrorCode.BadParam)
            }

            return skillGroup
        }

        return null
    }

    @Profile
    private CopyValidationRes validateCopySubjectToAnotherProject(String projectId, String subjectId, String otherProjectId) {
        SkillDefWithExtra subject
        try {
            subject = loadSubject(projectId, subjectId)
            validateSubjectDoesNotExist(otherProjectId, subject.skillId, subject.name)
            ProjDef fromProject = loadProject(projectId)
            ProjDef otherProject = loadProject(otherProjectId)
            validateProjectsCommunityStatus(fromProject, otherProject)
        } catch (SkillException skillException) {
            return new CopyValidationRes(isAllowed: false, validationErrors: [skillException.getMessage()?.toString()])
        }


        List<SkillDefRepo.SkillIdAndName> itemsToCopy = skillDefRepo.findSkillsIdAndNameUnderASubject(subject.id)

        List<String> validationErrors = checkForSkillIdAndNameCollisions(itemsToCopy.collect { new SkillIdAndName(skillId: it.skillId, skillName: it.skillName) }, otherProjectId)
        CopyValidationRes res = new CopyValidationRes(isAllowed: validationErrors?.isEmpty(), validationErrors: validationErrors)
        return res
    }

    private static class SkillIdAndName {
        String skillId
        String skillName
    }

    @Profile
    private List<String> checkForSkillIdAndNameCollisions(List<SkillIdAndName> itemsToCopy, String otherProjectId) {
        List<SkillDef> destItems = skillDefRepo.findAllByProjectIdAndTypeIn(otherProjectId, [SkillDef.ContainerType.Subject, SkillDef.ContainerType.Skill, SkillDef.ContainerType.Badge, SkillDef.ContainerType.SkillsGroup])
        List<String> validationErrors = []

        List<String> destSkillIds = destItems.collect { it.getSkillId() }
        List<String> origSkillIds = itemsToCopy.collect { it.getSkillId() }
        List<String> idsAlreadyInProject = destSkillIds.intersect(origSkillIds, { a, b -> a.toLowerCase() <=> b.toLowerCase() })
        if (idsAlreadyInProject) {
            validationErrors.add("The following IDs already exist in the destination project: ${idsAlreadyInProject.sort().subList(0, Math.min(idsAlreadyInProject.size(), 10)).join(", ")}.".toString())
        }

        List<String> destSkillNames = destItems.collect { it.getName() }
        List<String> origSkillNames = itemsToCopy.collect { it.getSkillName() }
        List<String> namesAlreadyInProject = destSkillNames.intersect(origSkillNames, { a, b -> a.toLowerCase() <=> b.toLowerCase() })
        if (namesAlreadyInProject) {
            validationErrors.add("The following names already exist in the destination project: ${namesAlreadyInProject.sort().subList(0, Math.min(namesAlreadyInProject.size(), 10)).join(", ")}.".toString())
        }
        return validationErrors
    }

    @Profile
    private void copySubjectToAnotherProject(String projectId, String subjectId, String otherProjectId) {
        ProjDef otherProject = loadProject(otherProjectId)
        lockingService.lockProject(otherProject.projectId)
        copiedAttachmentUuidsThreadLocal.set([:])
        try {
            ProjDef fromProject = loadProject(projectId)
            validateProjectsCommunityStatus(fromProject, otherProject)
            validateUserIsAndAdminOfDestProj(otherProject, projectId)
            SkillDefWithExtra subject = loadSubject(fromProject.projectId, subjectId)
            validateSubjectDoesNotExist(otherProjectId, subject.skillId, subject.name)

            List<SkillInfo> allCollectedSkills = []
            Map newIcons = subject.iconClass ? customIconFacade.copyIcons(fromProject.projectId, otherProject.projectId, [subject.iconClass]) : [:]
            saveSingleSubjectAndItsSkills(fromProject, otherProject, subject, allCollectedSkills, newIcons, true)
        } finally {
            copiedAttachmentUuidsThreadLocal.set([:])
        }
    }

    private void validateProjectsCommunityStatus(ProjDef fromProject, ProjDef otherProject) {
        if (userCommunityService.isUserCommunityOnlyProject(fromProject.projectId) && !userCommunityService.isUserCommunityOnlyProject(otherProject.projectId)) {
            String fromCommunity = userCommunityService.getProjectUserCommunity(fromProject.projectId)
            String toCommunity = userCommunityService.getProjectUserCommunity(otherProject.projectId)
            throw new SkillException("Subjects from ${fromCommunity} projects cannot be copied to ${toCommunity} projects.", fromProject.projectId, null, ErrorCode.AccessDenied)
        }
    }

    private void validateUserIsAndAdminOfDestProj(ProjDef otherProject, String projectId) {
        UserInfo userInfo = userInfoService.currentUser
        Boolean isAdminForOtherProject = userRoleRepo.isUserProjectAdmin(userInfo.username, otherProject.projectId)
        if (!isAdminForOtherProject) {
            throw new SkillException("User [${userInfo.username}] is not an admin for destination project [${otherProject.projectId}]", projectId, null, ErrorCode.BadParam)
        }
    }

    private SkillDefWithExtra loadSubject(String projectId, String subjectId) {
        SkillDefWithExtra subject = skillDefWithExtraRepo.findByProjectIdAndSkillId(projectId, subjectId)
        if (!subject) {
            throw new SkillException("Subject with id [${subjectId}] does not exist.", projectId, null, ErrorCode.BadParam)
        }
        if (subject.type != SkillDef.ContainerType.Subject) {
            throw new SkillException("Provided id [${subjectId}] is not for a subject.", projectId, null, ErrorCode.BadParam)
        }
        return subject
    }

    private void validateSubjectDoesNotExist(String toProjId, String subjectId, String subjectName) {
        if (skillDefRepo.existsByProjectIdAndSkillIdAllIgnoreCase(toProjId, subjectId)) {
            throw new SkillException("Id [${subjectId}] already exists.", subjectId, null, ErrorCode.BadParam)
        }
        if (skillDefRepo.existsByProjectIdAndNameAndTypeAllIgnoreCase(toProjId, subjectName, SkillDef.ContainerType.Subject)) {
            throw new SkillException("Subject with name [${subjectName}] already exists.", subjectId, null, ErrorCode.BadParam)
        }
    }

    @Transactional
    @Profile
    void copyProject(String originalProjectId, ProjectRequest projectRequest) {
        lockingService.lockProjects()

        copiedAttachmentUuidsThreadLocal.set([:])
        try {
            ProjDef fromProject = loadProject(originalProjectId)
            validate(projectRequest)

            ProjDef toProj = saveToProject(projectRequest)
            saveProjectSettings(fromProject, toProj)

            pinProjectForRootUser(toProj)

            List<SkillInfo> allCollectedSkills = []
            def newIcons = customIconFacade.copyIcons(originalProjectId, toProj.projectId)
            saveSubjectsAndSkills(projectRequest, fromProject, toProj, allCollectedSkills, newIcons)
            updateProjectAndSubjectLevels(fromProject, toProj)
            saveBadgesAndTheirSkills(fromProject, toProj, newIcons)
            flushEntityCache()
            saveDependencies(fromProject, toProj)
            saveReusedSkills(allCollectedSkills, fromProject, toProj)
            handleQuizBasedUserPointsAndAchievements(toProj)
        } finally {
            copiedAttachmentUuidsThreadLocal.set([:])
        }
    }

    private void flushEntityCache() {
        // required because saving badges operation uses skillDefWithExtraRepo
        // but follow-on queries utilize skillDefRepo, each one has its own cache
        entityManager.flush()
        entityManager.clear()
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
            boolean isProjectRoleSetting = fromSetting.setting == Settings.USER_PROJECT_ROLE.settingName;
            boolean isProjectUcSetting = fromSetting.setting == Settings.PROJECT_COMMUNITY_VALUE.settingName;
            if (!discoverableProject && !isLevelsAsPtsSetting && !isProjectRoleSetting && !isProjectUcSetting) {
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

        // remove all the levels but the first one - this will resolve any weird issue with updates where points cannot overlap between levels
        int levelsToKeep = 1
        int levelsToRemove = existingLevels.size() - levelsToKeep

        if (levelsToRemove > 0) {
            (levelsToRemove).times {
                try {
                    levelDefinitionStorageService.deleteLastLevel(toProj.projectId, subjectId, false)
                    log.debug("PROJ COPY: [{}]=[{}] subj[{}] - removed last level", fromProject.projectId, toProj.projectId, subjectId)
                } catch (Throwable t) {
                    throw new IllegalStateException("Failed to remove last level for proj=[${toProj.projectId}] and subjectId=[${subjectId}]", t)
                }
            }
        }
        List<LevelDefinitionRes> levelsToUpdate = fromLevels.findAll { it.level <= levelsToKeep }.sort({ it.level }).reverse()
        List<LevelDefinitionRes> levelsToCreate = fromLevels.findAll { it.level > levelsToKeep }.sort({ it.level })
        levelsToUpdate.eachWithIndex { LevelDefinitionRes fromLevel, int index ->
            EditLevelRequest editLevelRequest = new EditLevelRequest(
                    percent: fromLevel.percent,
                    name: fromLevel.name,
                    iconClass: fromLevel.iconClass,
                    level: fromLevel.level,
                    pointsFrom: fromLevel.pointsFrom,
                    pointsTo: (index > 0) ? fromLevel.pointsTo : null, // levels are reversed and the highest level must always have pointTo=null
            )
            levelDefinitionStorageService.editLevel(toProj.projectId, editLevelRequest, fromLevel.level, subjectId, false)
            log.debug("PROJ COPY: [{}]=[{}] subj[{}] - edited level to [{}]", fromProject.projectId, toProj.projectId, subjectId, JsonOutput.toJson(editLevelRequest))
        }
        levelsToCreate.each { LevelDefinitionRes fromlevel ->
            NextLevelRequest nextLevelRequest = new NextLevelRequest(
                    percent: fromlevel.percent,
                    points: fromlevel.pointsFrom,
                    name: fromlevel.name,
                    iconClass: fromlevel.iconClass,
            )

            levelDefinitionStorageService.addNextLevel(toProj.projectId, nextLevelRequest, subjectId, false)
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
    private void handleQuizBasedUserPointsAndAchievements(ProjDef toProj) {
        String projectId = toProj.projectId
        List<Integer> skillIdsWithQuiz = quizToSkillDefRepo.getSkillRefIdsWithQuizByProjectId(projectId)
        if (skillIdsWithQuiz) {
            List<UserQuizAttempt> firstPassedRun = userQuizAttemptRepo.findByInSkillRefIdAndByStatus(skillIdsWithQuiz, UserQuizAttempt.QuizAttemptStatus.PASSED, PageRequest.of(0, 1))
            if (firstPassedRun) {
                batchOperationsTransactionalAccessor.createUserPerformedEntriesFromPassedQuizzesForProject(projectId)
                batchOperationsTransactionalAccessor.createSkillUserPointsFromPassedQuizzesForProject(projectId)
                batchOperationsTransactionalAccessor.createUserAchievementsFromPassedQuizzes(projectId)

                List<SkillDef> skillGroups = skillDefRepo.findAllByProjectIdAndType(projectId, SkillDef.ContainerType.SkillsGroup)
                batchOperationsTransactionalAccessor.identifyAndAddGroupAchievements(skillGroups)

                List<SkillDef> subjects = skillDefRepo.findAllByProjectIdAndType(projectId, SkillDef.ContainerType.Subject)
                subjects.each { subject ->
                    batchOperationsTransactionalAccessor.handlePointsAndAchievementsForSubject(subject)
                }

                batchOperationsTransactionalAccessor.handlePointsAndAchievementsForProject(projectId)
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
            skillsDepsService.addLearningPathItem(toProj.projectId, it.from.skillId, it.to.skillId)
        }
    }

    @Profile
    private void saveBadgesAndTheirSkills(ProjDef fromProject, ProjDef toProj, HashMap<String, String> newIcons) {
        List<SkillDefWithExtra> badges = skillDefWithExtraRepo.findAllByProjectIdAndType(fromProject.projectId, SkillDef.ContainerType.Badge)
        badges.sort { it.displayOrder }.each { SkillDefWithExtra fromBadge ->
            BadgeRequest badgeRequest = new BadgeRequest()
            Props.copy(fromBadge, badgeRequest)
            badgeRequest.badgeId = fromBadge.skillId
            badgeRequest.description = handleAttachmentsInDescription(badgeRequest.description, toProj.projectId)
            badgeRequest.enabled = Boolean.FALSE.toString()
            if(newIcons[fromBadge.iconClass]) {
                badgeRequest.iconClass = newIcons[fromBadge.iconClass];
            }
            badgeAdminService.saveBadge(toProj.projectId, fromBadge.skillId, badgeRequest)
            List<SkillDefPartialRes> badgeSkills = skillsAdminService.getSkillsForBadge(fromProject.projectId, fromBadge.skillId)
            badgeSkills.each { SkillDefPartialRes fromBadgeSkill ->
                badgeAdminService.addSkillToBadge(toProj.projectId, badgeRequest.badgeId, fromBadgeSkill.skillId)
            }
            // must enable it after the skills were added
            if (fromBadge.enabled == Boolean.TRUE.toString() && badgeSkills) {
                badgeRequest.enabled = fromBadge.enabled
                badgeAdminService.saveBadge(toProj.projectId, fromBadge.skillId, badgeRequest)
            }
        }
    }

    @Profile
    private void saveSubjectsAndSkills(ProjectRequest projectRequest, ProjDef fromProject, ProjDef toProj, List<SkillInfo> allCollectedSkills, HashMap<String, String> newIcons) {
        List<SkillDefWithExtra> fromSubjects = skillDefWithExtraRepo.findAllByProjectIdAndType(fromProject.projectId, SkillDef.ContainerType.Subject)
        fromSubjects?.findAll { it.enabled }
                ?.sort { it.displayOrder }
                ?.each { SkillDefWithExtra fromSubj ->
                    saveSingleSubjectAndItsSkills(fromProject, toProj, fromSubj, allCollectedSkills, newIcons)
                }
    }

    private void saveSingleSubjectAndItsSkills(ProjDef fromProject, ProjDef toProj, SkillDefWithExtra fromSubj, List<SkillInfo> allCollectedSkills, Map<String, String> newIcons, boolean validateNameAndIdCollisions = false) {
        SubjectRequest toSubj = new SubjectRequest()
        Props.copy(fromSubj, toSubj)
        toSubj.subjectId = fromSubj.skillId
        toSubj.description = handleAttachmentsInDescription(toSubj.description, toProj.projectId)
        if(newIcons[fromSubj.iconClass]) {
            toSubj.iconClass = newIcons[fromSubj.iconClass]
        }
        subjAdminService.saveSubject(toProj.projectId, fromSubj.skillId, toSubj)
        log.info("PROJ COPY: [{}]=[{}] subj[{}] - created new subject")
        createSkills(fromProject.projectId, toProj.projectId, toSubj.subjectId, allCollectedSkills, null, validateNameAndIdCollisions)
    }

    @Profile
    private void createSkills(String originalProjectId, String desProjectId, String subjectId, List<SkillInfo> allCollectedSkills, String groupId = null, boolean validateNameAndIdCollisions = false) {
        String parentId = groupId ?: subjectId
        List<SkillDefWithExtra> skillDefs = skillRelDefRepo.getChildrenWithExtraAttrs(originalProjectId, parentId,
                [SkillRelDef.RelationshipType.RuleSetDefinition, SkillRelDef.RelationshipType.SkillsGroupRequirement])
        List<SkillDefWithExtra> skillDefsSorted = skillDefs?.sort { it.displayOrder }
        createSkills(skillDefsSorted, originalProjectId, desProjectId, subjectId, allCollectedSkills, groupId, validateNameAndIdCollisions)
    }

    @Profile
    private void createSkills(List<SkillDefWithExtra> skillsToCopy, String originalProjectId,
                              String desProjectId, String subjectId,
                              List<SkillInfo> allCollectedSkills, String groupId = null,
                              boolean validateNameAndIdCollisions = false) {
        allCollectedSkills.addAll(skillsToCopy.collect { new SkillInfo(skillDef: it, subjectId: subjectId, groupId: groupId) })
        skillsToCopy?.findAll { it.enabled == "true" && (!it.copiedFrom) }
                ?.each { SkillDefWithExtra fromSkill ->
                    if (validateNameAndIdCollisions) {
                        if (skillDefRepo.existsByProjectIdAndSkillIdAllIgnoreCase(desProjectId, fromSkill.skillId)) {
                            throw new SkillException("ID [${fromSkill.skillId}] already exists in the project [${desProjectId}]", null, null, ErrorCode.BadParam)
                        }
                        if (skillDefRepo.existsByProjectIdAndNameAndTypeAllIgnoreCase(desProjectId, fromSkill.name, fromSkill.type)) {
                            throw new SkillException("Skill with name [${fromSkill.name}] already exists in the project [${desProjectId}]", null, null, ErrorCode.BadParam)
                        }
                    }
                    try {
                        SkillProjectCopyRequest skillRequest = new SkillProjectCopyRequest()
                        Props.copy(fromSkill, skillRequest)
                        skillRequest.projectId = desProjectId
                        skillRequest.subjectId = subjectId
                        skillRequest.type = fromSkill.type?.toString()
                        skillRequest.version = 0
                        skillRequest.description = handleAttachmentsInDescription(skillRequest.description, desProjectId)

                        skillRequest.selfReportingType = fromSkill.selfReportingType?.toString()
                        if (skillRequest.selfReportingType && skillRequest.selfReportingType == SkillDef.SelfReportingType.Quiz.toString()) {
                            QuizToSkillDefRepo.QuizNameAndId quizNameAndId = quizToSkillDefRepo.getQuizIdBySkillIdRef(fromSkill.id)
                            skillRequest.quizId = quizNameAndId.quizId
                        }

                        if (fromSkill.type != SkillDef.ContainerType.SkillsGroup) {
                            skillRequest.numPerformToCompletion = fromSkill.totalPoints / fromSkill.pointIncrement
                        }

                        // group partial requirement must be set after skills are added
                        Integer groupNumSkillsRequired = -1
                        if (fromSkill.type == SkillDef.ContainerType.SkillsGroup) {
                            groupNumSkillsRequired = fromSkill.numSkillsRequired
                            skillRequest.numSkillsRequired = -1
                        }
                        SkillsAdminService.SaveSkillTmpRes saveSkillTmpRes = skillsAdminService.saveSkill(fromSkill.skillId, skillRequest, true, groupId, false)
                        if (fromSkill.type == SkillDef.ContainerType.SkillsGroup) {
                            createSkills(originalProjectId, desProjectId, subjectId, allCollectedSkills, fromSkill.skillId)
                        }
                        if (groupNumSkillsRequired > 0) {
                            skillRequest.numSkillsRequired = groupNumSkillsRequired
                            skillsAdminService.saveSkill(fromSkill.skillId, skillRequest, true, groupId)
                        }

                        handleSkillAttributes(fromSkill, saveSkillTmpRes)
                    } catch (Throwable t) {
                        throw new SkillException("Error copying skill: ${fromSkill.skillId}", t)
                    }
                }
    }


    private final ThreadLocal<Map<String,String>> copiedAttachmentUuidsThreadLocal = new ThreadLocal<>();
    @Profile
    private String handleAttachmentsInDescription(String description, String newProjectId) {
        Map<String,String> copiedAttachmentUuids = copiedAttachmentUuidsThreadLocal.get()
        String res = description
        if (description) {
            attachmentService.findAttachmentUuids(res).each { String uuid ->
                Attachment attachment = attachmentService.getAttachment(uuid)
                String copiedUuid = copiedAttachmentUuids[uuid]
                if (!copiedUuid) {
                    Attachment copiedAttachment = attachmentService.copyAttachmentWithNewUuid(attachment, newProjectId)
                    copiedUuid = copiedAttachment.uuid
                    copiedAttachmentUuids[uuid] = copiedUuid
                    copiedAttachmentUuidsThreadLocal.set(copiedAttachmentUuids)
                }
                res = res.replaceAll(uuid, copiedUuid)
            }
        }

        return res
    }

    @Profile
    private void handleSkillAttributes(SkillDefWithExtra fromSkill, SkillsAdminService.SaveSkillTmpRes toSkill) {
        List<SkillAttributesDef> skillAttributesDefs =skillAttributesDefRepo.findAllBySkillRefId(fromSkill.id)
        skillAttributesDefs?.each {
            SkillAttributesDef copySkillAttributeDef = new SkillAttributesDef()
            copySkillAttributeDef.type = it.type
            copySkillAttributeDef.attributes = it.attributes
            copySkillAttributeDef.skillRefId = toSkill.skillRefId

            skillAttributesDefRepo.save(copySkillAttributeDef)
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
        if (!userInfoService.isCurrentUserASuperDuperUser()){
            createdResourceLimitsValidator.validateNumProjectsCreated(userInfoService.getCurrentUserId())
        }
        serviceValidatorHelper.validateProjectIdDoesNotExist(projectRequest.projectId)
        serviceValidatorHelper.validateProjectNameDoesNotExist(projectRequest.name, projectRequest.projectId)
    }

}
