package skills.services

import callStack.profiler.Profile
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.commons.collections.CollectionUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import skills.auth.UserInfoService
import skills.controller.exceptions.DataIntegrityViolationExceptionHandler
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.exceptions.SkillsValidator
import skills.controller.request.model.*
import skills.controller.result.model.*
import skills.icons.IconCssNameUtil
import skills.services.settings.Settings
import skills.services.settings.SettingsService
import skills.storage.model.*
import skills.storage.model.SkillRelDef.RelationshipType
import skills.storage.model.auth.RoleName
import skills.storage.repos.*
import skills.utils.ClientSecretGenerator
import skills.utils.Props

@Service
@Slf4j
class AdminProjService {

    static final ALL_SKILLS_PROJECTS = 'ALL_SKILLS_PROJECTS'

    @Autowired
    ProjDefRepo projDefRepo

    @Autowired
    UserInfoService userInfoService

    @Autowired
    AccessSettingsStorageService accessSettingsStorageService

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    SkillDefWithExtraRepo skillDefWithExtraRepo

    @Autowired
    SkillRelDefRepo skillRelDefRepo

    @Autowired
    LevelDefinitionStorageService levelDefService

    @Autowired
    RuleSetDefinitionScoreUpdater ruleSetDefinitionScoreUpdater

    @Autowired
    UserAchievementsAndPointsManagement userPointsManagement

    @Autowired
    RuleSetDefGraphService ruleSetDefGraphService

    @Autowired
    SkillShareDefRepo skillShareDefRepo

    @Autowired
    SettingsService settingsService

    @Autowired
    DependencyValidator dependencyValidator
    @Autowired
    SortingService sortingService


    private static DataIntegrityViolationExceptionHandler dataIntegrityViolationExceptionHandler =
            new DataIntegrityViolationExceptionHandler([
                    "index_project_definition_name" : "Provided project name already exist.",
                    "index_project_definition_project_id": "Provided project id already exist.",
            ])

    private static DataIntegrityViolationExceptionHandler subjectDataIntegrityViolationExceptionHandler = crateSkillDefBasedDataIntegrityViolationExceptionHandler("subject")
    private static DataIntegrityViolationExceptionHandler badgeDataIntegrityViolationExceptionHandler = crateSkillDefBasedDataIntegrityViolationExceptionHandler("badge")
    private static DataIntegrityViolationExceptionHandler skillDataIntegrityViolationExceptionHandler = crateSkillDefBasedDataIntegrityViolationExceptionHandler("skill")
    private static DataIntegrityViolationExceptionHandler crateSkillDefBasedDataIntegrityViolationExceptionHandler(String type) {
        new DataIntegrityViolationExceptionHandler([
                "index_skill_definition_project_id_skill_id" : "Provided ${type} id already exist.".toString(),
                "index_skill_definition_project_id_name": "Provided ${type} name already exist.".toString(),
                "index_skill_definition_project_id_skill_id_type" : "Provided ${type} id already exist.".toString(),
                "index_skill_definition_project_id_name_type": "Provided ${type} name already exist.".toString(),
        ])
    }

    @Transactional()
    void saveProject(String originalProjectId, ProjectRequest projectRequest, String userIdParam = null) {
        assert projectRequest?.projectId
        assert projectRequest?.name

        IdFormatValidator.validate(projectRequest.projectId)
        if(projectRequest.name.length() > 50){
            throw new SkillException("Bad Name [${projectRequest.name}] - must not exceed 50 chars.")
        }

        ProjDef projectDefinition = originalProjectId ? projDefRepo.findByProjectIdIgnoreCase(originalProjectId) : null
        if (!projectDefinition || !projectRequest.projectId.equalsIgnoreCase(originalProjectId)) {
            ProjDef idExist = projDefRepo.findByProjectIdIgnoreCase(projectRequest.projectId)
            if (idExist) {
                throw new SkillException("Project with id [${projectRequest.projectId}] already exists! Sorry!", null, null, ErrorCode.ConstraintViolation)
            }
        }
        if (!projectDefinition || !projectRequest.name.equalsIgnoreCase(projectDefinition.name)) {
            ProjDef nameExist = projDefRepo.findByNameIgnoreCase(projectRequest.name)
            if (nameExist) {
                throw new SkillException("Project with name [${projectRequest.name}] already exists! Sorry!", null, null, ErrorCode.ConstraintViolation)
            }
        }
        if (projectDefinition) {
            Props.copy(projectRequest, projectDefinition)
            log.info("Updating [{}]", projectDefinition)

            dataIntegrityViolationExceptionHandler.handle(projectDefinition.projectId){
                projectDefinition = projDefRepo.save(projectDefinition)
            }
            log.info("Saved [{}]", projectDefinition)
        } else {
            // TODO: temp hack around since user is not yet defined when Inception project is created
            // This will be addressed in ticket #139
            String clientSecret = new ClientSecretGenerator().generateClientSecret()

            projectDefinition = new ProjDef(projectId: projectRequest.projectId, name: projectRequest.name,
                    clientSecret: clientSecret)
            log.info("Created project [{}]", projectDefinition)

            dataIntegrityViolationExceptionHandler.handle(projectDefinition.projectId){
                projectDefinition = projDefRepo.save(projectDefinition)
            }

            if (!userIdParam) {
                sortingService.setNewProjectDisplayOrder(projectRequest.projectId)
            }
            log.info("Saved [{}]", projectDefinition)

            levelDefService.createDefault(projectRequest.projectId, projectDefinition)

            accessSettingsStorageService.addUserRole(userIdParam ?: this.getUserId(), projectRequest.projectId, RoleName.ROLE_PROJECT_ADMIN)
            log.info("Added user role [{}]", RoleName.ROLE_PROJECT_ADMIN)
        }
    }

    @Transactional()
    void saveSubject(String projectId, SubjectRequest subjectRequest) {
        IdFormatValidator.validate(subjectRequest.subjectId)
        if(subjectRequest.name.length() > 50){
            throw new SkillException("Bad Name [${subjectRequest.name}] - must not exceed 50 chars.")
        }

        SkillDefWithExtra res
        if (subjectRequest.id) {
            Optional<SkillDefWithExtra> existing = skillDefWithExtraRepo.findById(subjectRequest.id)
            if (!existing.present) {
                throw new SkillException("Subject id [${subjectRequest.id}] doesn't exist.", projectId, null)
            }
            SkillDefWithExtra skillDefinition = existing.get()

            Props.copy(subjectRequest, skillDefinition)
            //we need to manually copy subjectId into skillId
            skillDefinition.skillId = subjectRequest.subjectId
            subjectDataIntegrityViolationExceptionHandler.handle(projectId) {
                res = skillDefRepo.save(skillDefinition)
            }
            log.info("Updated [{}]", skillDefinition)
        } else {
            SkillDef idExists = skillDefRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(projectId, subjectRequest.subjectId, SkillDef.ContainerType.Subject)
            if (idExists) {
                throw new SkillException("Subject with id [${subjectRequest.subjectId}] already exists! Sorry!", projectId, null, ErrorCode.ConstraintViolation)
            }

            SkillDef nameExists = skillDefRepo.findByProjectIdAndNameIgnoreCaseAndType(projectId, subjectRequest.name, SkillDef.ContainerType.Subject)
            if (nameExists) {
                throw new SkillException("Subject with name [${subjectRequest.name}] already exists! Sorry!", projectId, null, ErrorCode.ConstraintViolation)
            }

            ProjDef projDef = getProjDef(projectId)

            Integer lastDisplayOrder = projDef.subjects?.collect({ it.displayOrder })?.max()
            int displayOrder = lastDisplayOrder != null ? lastDisplayOrder + 1 : 0

            SkillDefWithExtra skillDef = new SkillDefWithExtra(
                    type: SkillDef.ContainerType.Subject,
                    projectId: projectId,
                    skillId: subjectRequest.subjectId,
                    name: subjectRequest?.name,
                    description: subjectRequest?.description,
                    iconClass: subjectRequest?.iconClass ?: "fa fa-question-circle",
                    projDef: projDef,
                    displayOrder: displayOrder
            )

            subjectDataIntegrityViolationExceptionHandler.handle(projectId) {
                res = skillDefWithExtraRepo.save(skillDef)
            }
            levelDefService.createDefault(projectId, null, skillDef)

            log.info("Created [{}]", res)
        }
    }

    @Transactional()
    ProjDef getProjDef(String projectId) {
        ProjDef projDef = projDefRepo.findByProjectIdIgnoreCase(projectId)
        if (!projDef) {
            throw new SkillException("Failed to find project [$projectId]", projectId, null)
        }
        return projDef
    }

    @Transactional()
    List<Integer> getUniqueVersionList(String projectId) {
        skillDefRepo.getUniqueVersionList(projectId)
    }

    private SkillDef getSkillDef(String projectId, String skillId, SkillDef.ContainerType containerType = SkillDef.ContainerType.Skill) {
        SkillDef skillDef = skillDefRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(projectId, skillId, containerType)
        if (!skillDef) {
            throw new SkillException("Failed to find skillId [$skillId] for [$projectId] with type [${containerType}]", projectId, skillId)
        }
        return skillDef
    }

    @Transactional()
    void saveBadge(String projectId, BadgeRequest badgeRequest) {
        IdFormatValidator.validate(badgeRequest.badgeId)
        if(badgeRequest.name.length() > 50){
            throw new SkillException("Bad Name [${badgeRequest.name}] - must not exceed 50 chars.")
        }

        boolean isEdit = badgeRequest.id
        SkillDefWithExtra skillDefinition
        if (isEdit) {
            Optional<SkillDefWithExtra> existing = skillDefWithExtraRepo.findById(badgeRequest.id)
            if (!existing.present) {
                throw new SkillException("Badge id [${badgeRequest.id}] doesn't exist.", projectId, null)
            }
            log.info("Updating with [{}]", badgeRequest)
            skillDefinition = existing.get()

            Props.copy(badgeRequest, skillDefinition)
            skillDefinition.skillId = badgeRequest.badgeId
        } else {
            SkillDef idExists = skillDefRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(projectId, badgeRequest.badgeId, SkillDef.ContainerType.Badge)
            if (idExists) {
                throw new SkillException("Badge with id [${badgeRequest.badgeId}] already exists! Sorry!", projectId, null, ErrorCode.ConstraintViolation)
            }

            SkillDef nameExists = skillDefRepo.findByProjectIdAndNameIgnoreCaseAndType(projectId, badgeRequest.name, SkillDef.ContainerType.Badge)
            if (nameExists) {
                throw new SkillException("Badge with name [${badgeRequest.name}] already exists! Sorry!", projectId, null, ErrorCode.ConstraintViolation)
            }

            ProjDef projDef = getProjDef(projectId)

            Integer lastDisplayOrder = projDef.badges?.collect({ it.displayOrder })?.max()
            int displayOrder = lastDisplayOrder != null ? lastDisplayOrder + 1 : 0

            skillDefinition = new SkillDefWithExtra(
                    type: SkillDef.ContainerType.Badge,
                    projectId: projectId,
                    skillId: badgeRequest.badgeId,
                    name: badgeRequest?.name,
                    description: badgeRequest?.description,
                    iconClass: badgeRequest?.iconClass ?: "fa fa-question-circle",
                    startDate: badgeRequest.startDate,
                    endDate: badgeRequest.endDate,
                    projDef: projDef,
                    displayOrder: displayOrder,
            )
            log.info("Saving [{}]", skillDefinition)
        }

        SkillDefWithExtra savedSkill

        badgeDataIntegrityViolationExceptionHandler.handle(projectId) {
            savedSkill = skillDefWithExtraRepo.save(skillDefinition)
        }

        log.info("Saved [{}]", savedSkill)
    }

    @Transactional()
    void addSkillToBadge(String projectId, String badgeId, String skillid) {
        assignGraphRelationship(projectId, badgeId, SkillDef.ContainerType.Badge, skillid, RelationshipType.BadgeDependence)
    }

    @Transactional()
    void removeSkillFromBadge(String projectId, String badgeId, String skillid) {
        removeGraphRelationship(projectId, badgeId, SkillDef.ContainerType.Badge,
                projectId, skillid, RelationshipType.BadgeDependence)
    }


    @Transactional
    void deleteSubject(String projectId, String subjectId) {
        log.info("Deleting subject with project id [{}] and subject id [{}]", projectId, subjectId)
        SkillDef subjectDefinition = skillDefRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(projectId, subjectId, SkillDef.ContainerType.Subject)
        assert subjectDefinition, "DELETE FAILED -> no subject with project id [$projectId] and subjet id [$subjectId]"
        assert subjectDefinition.type == SkillDef.ContainerType.Subject

        deleteSkillWithItsDescendants(subjectDefinition)

        ProjDef projDef = getProjDef(projectId)
        // reset display order attribute - make sure the order is continuous - 0...N
        List<SkillDef> subjects = projDef.subjects
        subjects = subjects?.findAll({ it.id != subjectDefinition.id }) // need to remove because of JPA level caching?
        resetDisplayOrder(subjects)

        projDef.totalPoints = CollectionUtils.isEmpty(subjects) ? 0 : subjects.collect({it.totalPoints}).sum()
        projDefRepo.save(projDef)
        userPointsManagement.handleSubjectRemoval(subjectDefinition)

        log.info("Deleted subject with id [{}]", subjectDefinition.skillId)
    }

    @Transactional
    void deleteBadge(String projectId, String badgeId) {
        log.info("Deleting badge with project id [{}] and badge id [{}]", projectId, badgeId)
        SkillDef badgeDefinition = skillDefRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(projectId, badgeId, SkillDef.ContainerType.Badge)
        assert badgeDefinition, "DELETE FAILED -> no badge with project id [$projectId] and badge id [$badgeId]"
        assert badgeDefinition.type == SkillDef.ContainerType.Badge

        deleteSkillWithItsDescendants(badgeDefinition)

        // reset display order attribute - make sure the order is continuous - 0...N
        ProjDef projDef = getProjDef(projectId)
        List<SkillDef> badges = projDef.badges
        badges = badges?.findAll({ it.id != badgeDefinition.id }) // need to remove because of JPA level caching?
        resetDisplayOrder(badges)
        log.info("Deleted badge with id [{}]", badgeDefinition)
    }

    private void resetDisplayOrder(List<SkillDef> skillDefs) {
        if(skillDefs) {
            List <SkillDef> copy = new ArrayList<>(skillDefs)
            List<SkillDef> toSave = []
            copy = copy.sort({ it.displayOrder })
            copy.eachWithIndex { SkillDef entry, int i ->
                if (entry.displayOrder != i) {
                    toSave.add(entry)
                    entry.displayOrder = i
                }
            }
            if (toSave) {
                skillDefRepo.saveAll(toSave)
            }
        }
    }

    private void deleteSkillWithItsDescendants(SkillDef skillDef) {
        List<SkillDef> toDelete = []

        List<SkillDef> currentChildren = ruleSetDefGraphService.getChildrenSkills(skillDef)
        while (currentChildren) {
            toDelete.addAll(currentChildren)
            currentChildren = currentChildren?.collect {
                ruleSetDefGraphService.getChildrenSkills(it)
            }?.flatten()
        }
        toDelete.add(skillDef)
        log.info("Deleting [{}] skill definitions (descendants + me) under [{}]", toDelete.size(), skillDef.skillId)
        skillDefRepo.deleteAll(toDelete)
    }

    @Transactional(readOnly = true)
    skills.controller.result.model.SubjectResult getSubject(String projectId, String subjectId) {
        SkillDefWithExtra skillDef = skillDefWithExtraRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(projectId, subjectId, SkillDef.ContainerType.Subject)
        convertToSubject(skillDef)
    }

    @Transactional(readOnly = true)
    List<skills.controller.result.model.SubjectResult> getSubjects(String projectId) {
//        List<SkillDef> subjects = skillDefRepo.findAllByProjectIdAndType(projectId, SkillDef.ContainerType.Subject)
        List<SkillDefWithExtra> subjects = skillDefWithExtraRepo.findAllByProjectIdAndType(projectId, SkillDef.ContainerType.Subject)
        List<skills.controller.result.model.SubjectResult> res = subjects.collect { convertToSubject(it) }
        calculatePercentages(res)
        return res?.sort({ it.displayOrder })
    }

    @Transactional(readOnly = true)
    List<skills.controller.result.model.BadgeResult> getBadges(String projectId) {
        List<SkillDefWithExtra> badges = skillDefWithExtraRepo.findAllByProjectIdAndType(projectId, SkillDef.ContainerType.Badge)
        List<skills.controller.result.model.BadgeResult> res = badges.collect { convertToBadge(it) }
        return res?.sort({ it.displayOrder })
    }

    @Transactional(readOnly = true)
    skills.controller.result.model.BadgeResult getBadge(String projectId, String badgeId) {
        SkillDefWithExtra skillDef = skillDefWithExtraRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(projectId, badgeId, SkillDef.ContainerType.Badge)
        return convertToBadge(skillDef, true)
    }

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

    @Transactional
    void setSubjectDisplayOrder(String projectId, String subjectId, ActionPatchRequest subjectPatchRequest) {
        ProjDef projDef = getProjDef(projectId)
        updateDisplayOrder(subjectId, projDef.subjects, subjectPatchRequest)
    }

    @Transactional
    void setBadgeDisplayOrder(String projectId, String badgeId, ActionPatchRequest badgePatchRequest) {
        ProjDef projDef = getProjDef(projectId)
        updateDisplayOrder(badgeId, projDef.badges, badgePatchRequest)
    }

    private void updateDisplayOrder(String skillId, List<SkillDef> skills, ActionPatchRequest patchRequest) {
        SkillDef toUpdate = skills.find({ it.skillId == skillId })

        SkillDef switchWith

        switch (patchRequest.action) {
            case ActionPatchRequest.ActionType.DisplayOrderDown:
                skills = skills.sort({ it.displayOrder })
                switchWith = skills.find({ it.displayOrder > toUpdate.displayOrder })
                break;
            case ActionPatchRequest.ActionType.DisplayOrderUp:
                skills = skills.sort({ it.displayOrder }).reverse()
                switchWith = skills.find({ it.displayOrder < toUpdate.displayOrder })
                break;
            default:
                throw new IllegalArgumentException("Unknown action ${patchRequest.action}")
        }

        if (!switchWith) {
            assert switchWith, "Failed to find project definition to switch with [${toUpdate}] for action [$patchRequest.action]"
        }
        assert switchWith.skillId != toUpdate.skillId

        int switchWithDisplayOrderTmp = toUpdate.displayOrder

        toUpdate.displayOrder = switchWith.displayOrder
        switchWith.displayOrder = switchWithDisplayOrderTmp
        skillDefRepo.saveAll([toUpdate, switchWith])

        log.info("Switched order of [{}] and [{}]", toUpdate.skillId, switchWith.skillId)
    }

    @Transactional()
    void shareSkillToExternalProject(String projectId, String skillId, String sharedToProjectId) {
        if (projectId?.equalsIgnoreCase(sharedToProjectId)) {
            throw new SkillException("Can not share skill to itself. Requested project [$sharedToProjectId] is itself!", projectId, skillId)
        }
        SkillDef skill = getSkillDef(projectId, skillId)

        ProjDef sharedToProject = null
        if (sharedToProjectId != ALL_SKILLS_PROJECTS) {
            sharedToProject = getProjDef(sharedToProjectId)
        }

        SkillShareDef skillShareDef = new SkillShareDef(skill: skill, sharedToProject: sharedToProject)
        skillShareDefRepo.save(skillShareDef)
    }

    @Transactional()
    void deleteSkillShare(String projectId, String skillId, String sharedToProjectId) {
        SkillDef skill = getSkillDef(projectId, skillId)
        SkillShareDef skillShareDef
        if (sharedToProjectId == ALL_SKILLS_PROJECTS) {
            skillShareDef = skillShareDefRepo.findBySkillAndSharedToProjectIsNull(skill)
        } else {
            ProjDef sharedToProject = getProjDef(sharedToProjectId)
            skillShareDef = skillShareDefRepo.findBySkillAndSharedToProject(skill, sharedToProject)
        }

        if (!skillShareDef){
            throw new SkillException("Failed to find skill share definition for project [$projectId] skill [$skillId] => [$sharedToProjectId] project", projectId, skillId)
        }
        skillShareDefRepo.delete(skillShareDef)
    }


    @Transactional(readOnly = true)
    List<SharedSkillResult> getSharedSkillsWithOtherProjects(String projectId) {
        List<SkillShareDef> shareDefs = skillShareDefRepo.getSkillShareDefsWithOtherProjectsByProjectId(projectId)
        return shareDefs.collect { SkillShareDef shareDef ->
            new SharedSkillResult(
                    skillName: shareDef.skill.name, skillId: shareDef.skill.skillId,
                    projectName: shareDef.sharedToProject?.name, projectId: shareDef.sharedToProject?.projectId,
                    sharedWithAllProjects: shareDef.sharedToProject == null
            )
        }
    }

    @Transactional(readOnly = true)
    List<SharedSkillResult> getSharedSkillsFromOtherProjects(String projectId) {
        ProjDef projDef = getProjDef(projectId)
        List<SkillShareDefRepo.SkillSharedMeta> sharedMetas = skillShareDefRepo.getSkillDefsSharedFromOtherProjectsByProjectId(projDef)
        return sharedMetas.collect { SkillShareDefRepo.SkillSharedMeta meta ->
            new SharedSkillResult(
                    skillName: meta.skillName, skillId: meta.skillId,
                    projectName: meta.projectName, projectId: meta.projectId
            )
        }
    }


    @Transactional()
    void deleteProject(String projectId) {
        log.info("Deleting project with id [{}]", projectId)
        if (!existsByProjectId(projectId)){
            throw new SkillException("Project with id [${projectId}] does NOT exist")
        }

        projDefRepo.deleteByProjectIdIgnoreCase(projectId)
        log.info("Deleted project with id [{}]", projectId)
    }

    @Transactional()
    List<SimpleProjectResult> searchProjects(String nameQuery) {
        List<ProjDef> projDefs = projDefRepo.queryProjectsByNameQuery(nameQuery)
        return projDefs.collect {
            new SimpleProjectResult(name: it.name, projectId: it.projectId)
        }
    }

    @Transactional()
    void setProjectDisplayOrder(String projectId, ActionPatchRequest projectPatchRequest) {
        assert projectPatchRequest.action

        switch (projectPatchRequest.action) {
            case ActionPatchRequest.ActionType.DisplayOrderDown:
                sortingService.changeProjectOrder(projectId, SortingService.Move.DOWN)
                break;
            case ActionPatchRequest.ActionType.DisplayOrderUp:
                sortingService.changeProjectOrder(projectId, SortingService.Move.UP)
                break;
            default:
                throw new IllegalArgumentException("Unknown action ${projectPatchRequest.action}")
        }
    }

    @Transactional(readOnly = true)
    List<ProjectResult> getProjects() {
        Map<String,Integer> projectIdSortOrder = sortingService.getUserProjectsOrder(userId)
        // sql join with UserRoles and ther is 1-many relationship that needs to be normalized
        List<ProjectResult> finalRes = projDefRepo.getProjectsByUser(userId).unique({it.projectId}).collect({
            ProjectResult res = convert(it, projectIdSortOrder)
            return res
        })

        finalRes.sort() { it.displayOrder }

        if (finalRes) {
            finalRes.first().isFirst = true
            finalRes.last().isLast = true
        }

        return finalRes
    }

    @Profile
    private ProjectResult convert(ProjDef definition, Map<String,Integer> projectIdSortOrder) {
        Integer order = projectIdSortOrder?.get(definition.projectId)
        ProjectResult res = new ProjectResult(
                projectId: definition.projectId, name: definition.name, totalPoints: definition.totalPoints,
                numSubjects: definition.subjects ? definition.subjects.size() : 0,
                displayOrder: order != null ? order : 0,
        )
        res.numBadges = skillDefRepo.countByProjectIdAndType(definition.projectId, SkillDef.ContainerType.Badge)
        res.numSkills = countNumSkillsForProject(definition)
        SettingsResult result = settingsService.getProjectSetting(definition.projectId, Settings.LEVEL_AS_POINTS.settingName)

        if(result == null || result.value == "false"){
            res.levelsArePoints = false
        }else if(result?.value == "true"){
            res.levelsArePoints = true
        }

        res
    }

    @Profile
    private long countNumSkillsForProject(ProjDef definition) {
        skillDefRepo.countByProjectIdAndType(definition.projectId, SkillDef.ContainerType.Skill)
    }

    @Profile
    NumUsersRes getNumUsersByProjectId(String projectId) {
        int numUsers = projDefRepo.calculateDistinctUsers(projectId)
        return  new NumUsersRes(numUsers: numUsers)
    }


    @Profile
    private SubjectResult convertToSubject(SkillDefWithExtra skillDef) {
        SubjectResult res = new SubjectResult(
                id: skillDef.id,
                subjectId: skillDef.skillId,
                projectId: skillDef.projectId,
                name: skillDef.name,
                description: skillDef.description,
                displayOrder: skillDef.displayOrder,
                totalPoints: skillDef.totalPoints,
                iconClass: skillDef.iconClass,
        )

        res.numSkills = calculateNumChildSkills(skillDef)
//        res.numUsers = calculateNumUsersForChildSkills(skillDef)

        return res
    }

    @Profile
    private long calculateNumChildSkills(SkillDefParent skillDef) {
        skillDefRepo.countChildSkillsByIdAndRelationshipType(skillDef.id, RelationshipType.RuleSetDefinition)
    }

    @Profile
    private skills.controller.result.model.BadgeResult convertToBadge(SkillDefWithExtra skillDef, boolean loadRequiredSkills = false) {
        skills.controller.result.model.BadgeResult res = new skills.controller.result.model.BadgeResult(
                id: skillDef.id,
                badgeId: skillDef.skillId,
                projectId: skillDef.projectId,
                name: skillDef.name,
                description: skillDef.description,
                displayOrder: skillDef.displayOrder,
                iconClass: skillDef.iconClass,
                startDate: skillDef.startDate,
                endDate: skillDef.endDate,
        )

        if (loadRequiredSkills) {
            List<SkillDef> dependentSkills = skillDefRepo.findChildSkillsByIdAndRelationshipType(skillDef.id, SkillRelDef.RelationshipType.BadgeDependence)
            res.requiredSkills = dependentSkills?.collect { convertToSkillDefRes(it) }
            res.numSkills = dependentSkills ? dependentSkills.size() : 0
            res.totalPoints = dependentSkills ? dependentSkills?.collect({ it.totalPoints })?.sum() : 0
        } else {
            res.numSkills = skillDefRepo.countChildSkillsByIdAndRelationshipType(skillDef.id, SkillRelDef.RelationshipType.BadgeDependence)
            if (res.numSkills > 0) {
                res.totalPoints = skillDefRepo.sumChildSkillsTotalPointsBySkillAndRelationshipType(skillDef.id, SkillRelDef.RelationshipType.BadgeDependence)
            } else {
                res.totalPoints = 0
            }
        }
        return res
    }

    @Transactional(readOnly = true)
    ProjectResult getProject(String projectId) {
        ProjDef projectDefinition = getProjDef(projectId)
        Integer order = sortingService.getProjectSortOrder(projectId)
        ProjectResult res = convert(projectDefinition, [(projectId): order])
        return res
    }

    private void resetDisplayOrderAttributes(SkillDef parentSkill, String deletedSkillId) {
        List<SkillDef> ciblings = ruleSetDefGraphService.getChildrenSkills(parentSkill)
        ciblings = ciblings.sort({ it.displayOrder })
        int i = 0
        List<SkillDef> toSave = []
        ciblings.each {
            //getChildrenSkills returns the SkillDef that was deleted earlier in the transaction
            //we need to exclude it from toSave
            if(it.skillId != deletedSkillId) {
                i++
                if (it.displayOrder != i) {
                    it.displayOrder = i
                    toSave.add(it)
                }
            }
        }
        if (toSave) {

        }
        skillDefRepo.saveAll(toSave)
    }

    @Transactional
    void deleteSkill(String projectId, String subjectId, String skillId) {
        log.info("Deleting skill with project id [{}] and subject id [{}] and skill id [{}]", projectId, subjectId, skillId)
        SkillDef skillDefinition = skillDefRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(projectId, skillId, SkillDef.ContainerType.Skill)
        assert skillDefinition, "DELETE FAILED -> no skill with project find with projectId=[$projectId], subjectId=[$subjectId], skillId=[$skillId]"

        SkillDef parentSkill = ruleSetDefGraphService.getParentSkill(skillDefinition)


        ruleSetDefinitionScoreUpdater.skillToBeRemoved(skillDefinition)
        userPointsManagement.handleSkillRemoval(skillDefinition)

        deleteSkillWithItsDescendants(skillDefinition)
        log.info("Deleted skill [{}]", skillDefinition.skillId)

        resetDisplayOrderAttributes(parentSkill, skillDefinition.skillId)
    }


    @Transactional(readOnly = true)
    List<SkillDefPartialRes> getSkills(String projectId, String subjectId) {
        return getSkillsByProjectSkillAndType(projectId, subjectId, SkillDef.ContainerType.Subject, RelationshipType.RuleSetDefinition)
    }

    private List<SkillDefPartialRes> getSkillsByProjectSkillAndType(String projectId, String skillId, SkillDef.ContainerType type, RelationshipType relationshipType) {
        SkillDef parent = skillDefRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(projectId, skillId, type)
        if (!parent) {
            throw new SkillException("There is no skill id [${skillId}] doesn't exist.", projectId, null)
        }

        List<SkillDefRepo.SkillDefPartial> res = skillRelDefRepo.getChildrenPartial(parent.projectId, parent.skillId, relationshipType)
        return res.collect { convertToSkillDefPartialRes(it) }
    }

    @Transactional(readOnly = true)
    List<SkillDefSkinnyRes> getSkinnySkills(String projectId) {
        List<SkillDefRepo.SkillDefSkinny> data = loadSkinnySkills(projectId)
        List<SkillDefPartialRes> res = data.collect { convertToSkillDefSkinnyRes(it) }
        return res
    }

    @Profile
    private List<SkillDefRepo.SkillDefSkinny> loadSkinnySkills(String projectId) {
        skillDefRepo.findAllSkinnySelectByProjectIdAndType(projectId, SkillDef.ContainerType.Skill)
    }

    @Transactional(readOnly = true)
    skills.controller.result.model.SkillDefRes getSkill(String projectId, String subjectId, String skillId) {
        SkillDefWithExtra res = skillDefWithExtraRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(projectId, skillId, SkillDef.ContainerType.Skill)
        assert res
        return convertToSkillDefRes(res)
    }

    @Transactional(readOnly = true)
    SkillsGraphRes getDependentSkillsGraph(String projectId, String skillId) {
        List<GraphSkillDefEdge> edges = loadGraphEdges(projectId, RelationshipType.Dependence)

        // must only keep the provide skill id and its descendants
        List<GraphSkillDefEdge> collectedRes = []
        List<GraphSkillDefEdge> startEdges = edges.findAll { it.from.skillId == skillId }
        if(startEdges){
            collectDescendants(edges, startEdges, collectedRes)
        }

        return convertToSkillsGraphRes(collectedRes)
    }
    @Profile
    private void collectDescendants(List<GraphSkillDefEdge> allEdges, List<GraphSkillDefEdge> currentLevel, List<GraphSkillDefEdge> collectedRes){
        if(currentLevel){
            collectedRes.addAll(currentLevel)
            for(GraphSkillDefEdge edge: currentLevel) {
                List<GraphSkillDefEdge> nextLevel = allEdges.findAll({ edge.to.id == it.from.id })
                collectDescendants(allEdges, nextLevel, collectedRes)
            }
        }
    }

    @Transactional(readOnly = true)
    SkillsGraphRes getDependentSkillsGraph(String projectId) {
        List<GraphSkillDefEdge> edges = loadGraphEdges(projectId, RelationshipType.Dependence)
        return convertToSkillsGraphRes(edges)
    }

    private SkillsGraphRes convertToSkillsGraphRes(List<GraphSkillDefEdge> edges) {
        Set<SkillDef> distinctNodes = new TreeSet<>(new Comparator<SkillDef>() {
            @Override
            int compare(SkillDef o1, SkillDef o2) {
                return o1.id.compareTo(o2.id)
            }
        })
        List<SkillsGraphRes.Edge> edgesRes = []
        edges.each {
            edgesRes.add(new SkillsGraphRes.Edge(fromId: it.from.id, toId: it.to.id))
            distinctNodes.add(it.from)
            distinctNodes.add(it.to)
        }

        List<SkillDefRes> nodes = distinctNodes.collect({ convertToSkillDefRes(it) })
        SkillsGraphRes res = new SkillsGraphRes(nodes: nodes, edges: edgesRes)
        return res
    }

    @Transactional(readOnly = true)
    Integer findLatestSkillVersion(String projectId) {
        return skillDefRepo.findMaxVersionByProjectId(projectId)
    }

    private static class GraphSkillDefEdge {
        SkillDef from
        SkillDef to
    }
    @Profile
    private List<GraphSkillDefEdge> loadGraphEdges(String projectId, RelationshipType type){
        List<Object[]> edges = skillRelDefRepo.getGraph(projectId, type)

        return edges.collect({
            //   mapping directly to entity is slow, we can save over a second in latency by mapping attributes explicitly

            SkillDef from = new SkillDef(
                    id: it[0],
                    name: it[1],
                    skillId: it[2],
                    projectId: it[3],
                    pointIncrement: it[4],
                    totalPoints: it[5],
                    type: it[6],
            )

            SkillDef to = new SkillDef(
                    id: it[7],
                    name: it[8],
                    skillId: it[9],
                    projectId: it[10],
                    pointIncrement: it[11],
                    totalPoints: it[12],
                    type: it[13],
            )

            new GraphSkillDefEdge(from: from, to: to)

        })
    }

    @Transactional(readOnly = true)
    List<SkillDefForDependencyRes> getSkillsAvailableForDependency(String projectId) {
        List<SkillDefRepo.SkillDefSkinny> res = skillDefRepo.findAllSkinnySelectByProjectIdAndType(projectId, SkillDef.ContainerType.Skill)
        List<SkillDefForDependencyRes> finalRes = res.collect {
            new SkillDefForDependencyRes(
                    skillId: it.skillId, name: it.name, projectId: it.projectId, version: it.version
            )
        }
        List<SharedSkillResult> sharedSkills = getSharedSkillsFromOtherProjects(projectId)
        sharedSkills.each {
            finalRes.add(
                    new SkillDefForDependencyRes(
                            skillId: it.skillId, name: it.skillName, projectId: projectId, otherProjectId: it.projectId, otherProjectName: it.projectName
                    )
            )
        }

        return finalRes
    }

    @Transactional()
    void assignSkillDependency(String projectId, String skillId, String dependentSkillId, String dependentProjectId = null) {
        SkillDef skill1 = getSkillDef(projectId, skillId)
        SkillDef skill2 = getSkillDef(dependentProjectId ?: projectId, dependentSkillId)

        if (dependentProjectId) {
            dependencyValidator.validateDependencyEligibility(projectId, skill2)
        }

        validateDependencyVersions(skill1, skill2)
        checkForCircularGraphAndThrowException(skill1, skill2, RelationshipType.Dependence)
        try {
            skillRelDefRepo.save(new SkillRelDef(parent: skill1, child: skill2, type: RelationshipType.Dependence))
        } catch (DataIntegrityViolationException e) {
            String msg = "Skill dependency [${skill1.projectId}:${skill1.skillId}]=>[${skill2.projectId}:${skill2.skillId}] already exist.".toString()
            log.error(msg, e)
            throw new SkillException(msg, skill1.projectId, skill1.skillId, ErrorCode.FailedToAssignDependency)
        }
    }

    private void validateDependencyVersions(SkillDef skill, SkillDef dependOnSkill) {
        if (skill.version < dependOnSkill.version) {
            throw new SkillException("Not allowed to depend on skill with a later version. " +
                    "Skill [ID:${skill.skillId}, version ${skill.version}] can not depend on [ID:${dependOnSkill.skillId}, version ${dependOnSkill.version}]",
                    skill.projectId, skill.skillId, ErrorCode.FailedToAssignDependency)
        }
    }

    @Transactional()
    void removeSkillDependency(String projectId, String skillId, String dependentSkillId, String dependentProjectId = null) {
        removeGraphRelationship(projectId, skillId, SkillDef.ContainerType.Skill,
                dependentProjectId ?: projectId, dependentSkillId, RelationshipType.Dependence)
    }


    @Transactional
    void assignGraphRelationship(String projectId, String skillId, SkillDef.ContainerType skillType,
                                 String relationshipSkillId, RelationshipType relationshipType) {
        SkillDef skill1 = getSkillDef(projectId, skillId, skillType)
        SkillDef skill2 = getSkillDef(projectId, relationshipSkillId)
        skillRelDefRepo.save(new SkillRelDef(parent: skill1, child: skill2, type: relationshipType))
    }

    @Transactional
    void removeGraphRelationship(String projectId, String skillId, SkillDef.ContainerType skillType,
                                 String relationshipProjectId, String relationshipSkillId, RelationshipType relationshipType){
        SkillDef skill1 = getSkillDef(projectId, skillId, skillType)
        SkillDef skill2 = getSkillDef(relationshipProjectId, relationshipSkillId)
        SkillRelDef relDef = skillRelDefRepo.findByChildAndParentAndType(skill2, skill1, relationshipType)
        if (!relDef) {
            throw new SkillException("Failed to find relationship [$relationshipType] between [$skillId] and [$relationshipSkillId] for [$projectId]", projectId, skillId)
        }
        skillRelDefRepo.delete(relDef)
    }


    private void checkForCircularGraphAndThrowException(SkillDef skill1, SkillDef skill2, RelationshipType type) {
        assert skill1.skillId != skill2.skillId || skill1.projectId != skill2.projectId

        DependencyCheckResult dependencyCheckResult = checkForCircularGraph(skill1, skill2, type)
        if (!dependencyCheckResult.possible) {
            throw new SkillException(dependencyCheckResult.reason, skill1.projectId, skill1.skillId, ErrorCode.FailedToAssignDependency)
        }
    }

    private DependencyCheckResult checkForCircularGraph(SkillDef proposedParent, SkillDef proposedChild, RelationshipType type) {
        try {
            recursiveCircularDependenceCheck(proposedChild, proposedParent, [getDependencyCheckId(proposedParent), getDependencyCheckId(proposedChild)], type)
        } catch (Throwable t) {
            throw new SkillException(t.message, t, proposedParent.projectId, proposedParent.skillId)
        }
    }

    private String getDependencyCheckId(SkillDef skill) {
        return skill.projectId + ":" + skill.skillId
    }

    private DependencyCheckResult recursiveCircularDependenceCheck(SkillDef parent, SkillDef originalParent, List<String> idPath, RelationshipType type, int currentIter = 0, int maxIter = 100) {
        if (currentIter > maxIter) {
            throw new IllegalStateException("Number of [$maxIter] iterations exceeded when checking for circular dependency for [${originalParent.skillId}]")
        }

        List<SkillRelDef> relationships = skillRelDefRepo.findAllByParentAndType(parent, type)
        if (relationships) {
            if (relationships.find { it.child.skillId == originalParent.skillId }) {
                return new DependencyCheckResult(skillId: originalParent.skillId, dependentSkillId: idPath.last(), possible: false, reason: "Discovered circular dependency [${idPath.join(" -> ")} -> ${getDependencyCheckId(originalParent)}]".toString())
            }
            for ( SkillRelDef skillRelDef : relationships ) {
                List<String> idPathCopy = new ArrayList<>(idPath)
                idPathCopy.add(getDependencyCheckId(skillRelDef.child))
                DependencyCheckResult res = recursiveCircularDependenceCheck(skillRelDef.child, originalParent, idPathCopy, type, currentIter++, maxIter)
                if (!res.possible) {
                    return res
                }
            }
        }

        return new DependencyCheckResult(skillId: originalParent.skillId, dependentSkillId: idPath.last())
    }

    @Transactional()
    SkillDefRes saveSkill(SkillRequest skillRequest) {
        IdFormatValidator.validate(skillRequest.skillId)
        if (skillRequest.name.length() > 100) {
            throw new SkillException("Bad Name [${skillRequest.name}] - must not exceed 100 chars.")
        }
        SkillsValidator.isNotBlank(skillRequest.projectId, "Project Id")
        validateSkillVersion(skillRequest)

        boolean shouldRebuildScores

        boolean isEdit = skillRequest.id

        int totalPointsRequested = skillRequest.pointIncrement * skillRequest.numPerformToCompletion;
        SkillDefWithExtra skillDefinition
        if (isEdit) {
            Optional<SkillDefWithExtra> existing = skillDefWithExtraRepo.findById(skillRequest.id)
            if (!existing.present) {
                throw new SkillException("Requested skill update id [${skillRequest.id}] doesn't exist.", skillRequest.projectId, skillRequest.skillId)
            }
            log.info("Updating with [{}]", skillRequest)
            skillDefinition = existing.get()

            shouldRebuildScores = skillDefinition.totalPoints != totalPointsRequested

            Props.copy(skillRequest, skillDefinition, "childSkills", 'version')
            //totalPoints is not a prop on skillRequest, it is a calculated value so we
            //need to manually update it in the case of edits.
            skillDefinition.totalPoints = totalPointsRequested
        } else {
            SkillDef idExists = skillDefRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(skillRequest.projectId, skillRequest.skillId, SkillDef.ContainerType.Skill)
            if (idExists) {
                throw new SkillException("Skill with id [${skillRequest.skillId}] already exists! Sorry!", skillRequest.projectId, null, ErrorCode.ConstraintViolation)
            }

            SkillDef nameExists = skillDefRepo.findByProjectIdAndNameIgnoreCaseAndType(skillRequest.projectId, skillRequest.name, SkillDef.ContainerType.Skill)
            if (nameExists) {
                throw new SkillException("Skill with name [${skillRequest.name}] already exists! Sorry!", skillRequest.projectId, null, ErrorCode.ConstraintViolation)
            }

            String parentSkillId = skillRequest.subjectId
            Integer highestDisplayOrder = skillDefRepo.calculateChildSkillsHighestDisplayOrder(skillRequest.projectId, parentSkillId)
            int displayOrder = highestDisplayOrder == null ? 0 : highestDisplayOrder + 1
            skillDefinition = new SkillDefWithExtra(
                    skillId: skillRequest.skillId,
                    projectId: skillRequest.projectId,
                    name: skillRequest.name,
                    pointIncrement: skillRequest.pointIncrement,
                    pointIncrementInterval: skillRequest.pointIncrementInterval,
                    numMaxOccurrencesIncrementInterval: skillRequest.numMaxOccurrencesIncrementInterval,
                    totalPoints: totalPointsRequested,
                    description: skillRequest.description,
                    helpUrl: skillRequest.helpUrl,
                    displayOrder: displayOrder,
                    type: SkillDef.ContainerType.Skill,
                    version: skillRequest.version
            )
            log.info("Saving [{}]", skillDefinition)
            shouldRebuildScores = true
        }

        skillDataIntegrityViolationExceptionHandler.handle(skillRequest.projectId, skillRequest.skillId) {
            skillDefWithExtraRepo.save(skillDefinition)
        }

        SkillDef savedSkill = skillDefRepo.findByProjectIdAndSkillIdAndType(skillRequest.projectId, skillRequest.skillId, SkillDef.ContainerType.Skill)

        if (!isEdit) {
            assignToParent(skillRequest, savedSkill)
        }

        if (shouldRebuildScores) {
            log.info("Rebuilding scores")
            ruleSetDefinitionScoreUpdater.updateFromLeaf(savedSkill)
        }

        log.info("Saved [{}]", savedSkill)
        SkillDefRes skillDefRes = convertToSkillDefRes(savedSkill)
        return skillDefRes
    }

    private void validateSkillVersion(SkillRequest skillRequest){
        int latestSkillVersion = findLatestSkillVersion(skillRequest.projectId)
        if (skillRequest.version > (latestSkillVersion + 1)) {
            throw new SkillException("Latest skill version is [${latestSkillVersion}]; max supported version is latest+1 but provided [${skillRequest.version}] version", skillRequest.projectId, skillRequest.skillId, skills.controller.exceptions.ErrorCode.BadParam)
        }
    }


    private void assignToParent(SkillRequest skillRequest, SkillDef savedSkill) {
        String parentSkillId = skillRequest.subjectId
        SkillDef.ContainerType containerType = SkillDef.ContainerType.Subject

        SkillDef parent = skillDefRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(skillRequest.projectId, parentSkillId, containerType)
        if (!parent) {
            throw new SkillException("Requested parent skill id [${parentSkillId}] doesn't exist for type [${containerType}].", skillRequest.projectId, skillRequest.skillId)
        }

        SkillRelDef relDef = new SkillRelDef(parent: parent, child: savedSkill, type: RelationshipType.RuleSetDefinition)
        skillRelDefRepo.save(relDef)
    }

    @Transactional
    SkillDef updateSkillDisplayOrder(@PathVariable("projectId") String projectId,
                                     @PathVariable("subjectId") String subjectId,
                                     @PathVariable("skillId") String skillId,
                                     @RequestBody ActionPatchRequest patchRequest) {
        SkillDef moveMe = getSkillDef(projectId, skillId)
        if (!moveMe) {
            assert moveMe, "Failed to find skill for id [$skillId], projectId=[$projectId], subjectId=[$subjectId]"
        }

        SkillDef parent = ruleSetDefGraphService.getParentSkill(moveMe)

        SkillDef switchWith
        switch (patchRequest.action) {
            case ActionPatchRequest.ActionType.DisplayOrderDown:
                switchWith = skillDefRepo.findNextSkillDefs(projectId, parent.skillId, moveMe.displayOrder, RelationshipType.RuleSetDefinition, new PageRequest(0, 1))?.first()
                break;
            case ActionPatchRequest.ActionType.DisplayOrderUp:
                switchWith = skillDefRepo.findPreviousSkillDefs(projectId, parent.skillId, moveMe.displayOrder, RelationshipType.RuleSetDefinition, new PageRequest(0, 1))?.first()
                break;
            default:
                throw new IllegalArgumentException("Unknown action ${patchRequest.action}")
        }

        if (!switchWith) {
            assert switchWith, "Failed to find skill to switch with [${moveMe}] for action [$patchRequest.action]"
        }
        assert switchWith.skillId != moveMe.skillId

        int switchWithDisplayOrderTmp = moveMe.displayOrder
        moveMe.displayOrder = switchWith.displayOrder
        switchWith.displayOrder = switchWithDisplayOrderTmp
        skillDefRepo.save(moveMe)
        skillDefRepo.save(switchWith)

        return switchWith
    }



    @Profile
    private skills.controller.result.model.SkillDefRes convertToSkillDefRes(SkillDef skillDef) {
        skills.controller.result.model.SkillDefRes res = new skills.controller.result.model.SkillDefRes()
        Props.copy(skillDef, res)
        res.numPerformToCompletion = skillDef.totalPoints / res.pointIncrement
        return res
    }

    @Profile
    private skills.controller.result.model.SkillDefRes convertToSkillDefRes(SkillDefWithExtra skillDef) {
        skills.controller.result.model.SkillDefRes res = new skills.controller.result.model.SkillDefRes()
        Props.copy(skillDef, res)
        res.numPerformToCompletion = skillDef.totalPoints / res.pointIncrement
        return res
    }

    @CompileStatic
    @Profile
    private SkillDefSkinnyRes convertToSkillDefSkinnyRes(SkillDefRepo.SkillDefSkinny skinny) {
        SkillDefSkinnyRes res = new SkillDefSkinnyRes(
                id: skinny.id,
                skillId: skinny.skillId,
                projectId: skinny.projectId,
                name: skinny.name,
                version: skinny.version,
                displayOrder: skinny.displayOrder,
                created: skinny.created,
        )
        return res;
    }

    @CompileStatic
    @Profile
    private SkillDefPartialRes convertToSkillDefPartialRes(SkillDefRepo.SkillDefPartial partial, boolean loadNumUsers = false) {
        SkillDefPartialRes res = new SkillDefPartialRes(
                id: partial.id,
                skillId: partial.skillId,
                projectId: partial.projectId,
                name: partial.name,
                pointIncrement: partial.pointIncrement,
                pointIncrementInterval: partial.pointIncrementInterval,
                numMaxOccurrencesIncrementInterval: partial.numMaxOccurrencesIncrementInterval,
                numPerformToCompletion: partial.numMaxOccurrencesIncrementInterval,
                totalPoints: partial.totalPoints,
                version: partial.version,
                type: partial.skillType,
                displayOrder: partial.displayOrder,
                created: partial.created,
                updated: partial.updated,
        )

        res.numPerformToCompletion = (Integer)(res.totalPoints / res.pointIncrement)
        res.totalPoints = partial.totalPoints
        res.numMaxOccurrencesIncrementInterval = partial.numMaxOccurrencesIncrementInterval

        if (loadNumUsers) {
            res.numUsers = calculateDistinctUsersForSkill((SkillDefRepo.SkillDefPartial)partial)
        }

        return res;
    }

    @Profile
    private int calculateDistinctUsersForSkill(SkillDefRepo.SkillDefPartial partial) {
        skillDefRepo.calculateDistinctUsersForASingleSkill(partial.projectId, partial.skillId)
    }


    @Transactional(readOnly = true)
    boolean existsByProjectId(String projectId) {
        return projDefRepo.existsByProjectIdIgnoreCase(projectId)
    }

    @Transactional(readOnly = true)
    boolean existsByProjectName(String projectName) {
        return projDefRepo.existsByNameIgnoreCase(projectName)
    }

    private String getUserId() {
        userInfoService.getCurrentUser().username
    }

    private String getUserDn() {
        userInfoService.getCurrentUser().userDn?.toLowerCase()
    }

    @Transactional(readOnly = true)
    boolean existsBySubjectId(String projectId, String subjectId) {
        return skillDefRepo.existsByProjectIdAndSkillIdAndTypeAllIgnoreCase(projectId, subjectId, SkillDef.ContainerType.Subject)
    }

    @Transactional(readOnly = true)
    boolean existsBySubjectName(String projectId, String subjectName) {
        return skillDefRepo.existsByProjectIdAndNameAndTypeAllIgnoreCase(projectId, subjectName, SkillDef.ContainerType.Subject)
    }

    @Transactional(readOnly = true)
    boolean existsByBadgeName(String projectId, String subjectName) {
        return skillDefRepo.existsByProjectIdAndNameAndTypeAllIgnoreCase(projectId, subjectName, SkillDef.ContainerType.Badge)
    }

    @Transactional(readOnly = true)
    boolean existsBySkillId(String projectId, String skillId) {
        return skillDefRepo.existsByProjectIdAndSkillIdAllIgnoreCase(projectId, skillId)
    }

    @Transactional(readOnly = true)
    boolean existsBySkillName(String projectId, String skillName) {
        return skillDefRepo.existsByProjectIdAndNameAndTypeAllIgnoreCase(projectId, skillName, SkillDef.ContainerType.Skill)
    }

    @Transactional
    List<SkillDefPartialRes> getSkillsForBadge(String projectId, String badgeId) {
        return getSkillsByProjectSkillAndType(projectId, badgeId, SkillDef.ContainerType.Badge, RelationshipType.BadgeDependence)
    }


    @Transactional(readOnly = true)
    String getProjectSecret(String projectId) {
        ProjDef projectDefinition = getProjDef(projectId)
        return projectDefinition.clientSecret
    }

    @Transactional
    void updateClientSecret(String projectId, String clientSecret) {
        ProjDef projDef = getProjDef(projectId)
        projDef.clientSecret = clientSecret
    }

    @Transactional(readOnly = true)
    List<CustomIconResult> getCustomIcons(String projectId){
        ProjDef project = getProjDef(projectId)
        return project.getCustomIcons().collect { CustomIcon icon ->
            String cssClassname = IconCssNameUtil.getCssClass(icon.projectId, icon.filename)
            return new CustomIconResult(filename: icon.filename, cssClassname: cssClassname)
        }
    }
}
