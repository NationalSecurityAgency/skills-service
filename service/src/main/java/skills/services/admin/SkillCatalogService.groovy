/**
 * Copyright 2021 SkillTree
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
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.exceptions.SkillsValidator
import skills.controller.request.model.CatalogSkill
import skills.controller.request.model.ImportedSkillUpdate
import skills.controller.request.model.SkillImportRequest
import skills.controller.request.model.SkillRequest
import skills.controller.result.model.*
import skills.services.RuleSetDefGraphService
import skills.services.admin.skillReuse.SkillReuseIdUtil
import skills.services.attributes.SkillAttributeService
import skills.services.events.pointsAndAchievements.InsufficientPointsForFinalizationValidator
import skills.services.events.pointsAndAchievements.InsufficientPointsValidator
import skills.services.userActions.DashboardAction
import skills.services.userActions.DashboardItem
import skills.services.userActions.UserActionInfo
import skills.services.userActions.UserActionsHistoryService
import skills.storage.accessors.ProjDefAccessor
import skills.storage.accessors.SkillDefAccessor
import skills.storage.model.*
import skills.storage.repos.*
import skills.storage.repos.nativeSql.PostgresQlNativeRepo
import skills.utils.InputSanitizer
import skills.utils.Props

@Service
@Slf4j
class SkillCatalogService {

    private static final Set<String> aliasUnnecessary = Set.of("projectName", "subjectName", "subjectId", "exportedOn")

    @Value('#{"${skills.config.ui.minimumSubjectPoints}"}')
    int minimumSubjectPoints

    @Value('#{"${skills.config.ui.minimumProjectPoints}"}')
    int minimumProjectPoints

    @Value('#{"${skills.config.ui.maxSkillsPerSubject}"}')
    int maxSubjectSkills

    @Autowired
    ExportedSkillRepo exportedSkillRepo

    @Autowired
    SkillDefAccessor skillAccessor

    @Autowired
    ProjDefAccessor projDefAccessor

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    SkillRelDefRepo skillRelDefRepo

    @Autowired
    RuleSetDefGraphService relationshipService

    @Autowired
    SkillsAdminService skillsAdminService

    @Autowired
    SkillDefWithExtraRepo skillDefWithExtraRepo

    @Autowired
    QuizToSkillDefRepo quizToSkillDefRepo

    @Autowired
    SkillCatalogFinalizationService skillCatalogFinalizationService\

    @Autowired
    InsufficientPointsForFinalizationValidator insufficientPointsForFinalizationValidator

    @Autowired
    InsufficientPointsValidator insufficientPointsValidator

    @Autowired
    ProjDefRepo projDefRepo

    @Autowired
    PostgresQlNativeRepo PostgresQlNativeRepo

    @Autowired
    UserCommunityService userCommunityService

    @Autowired
    SkillAttributeService skillAttributeService

    @Autowired
    UserActionsHistoryService userActionsHistoryService

    @Transactional(readOnly = true)
    TotalCountAwareResult<ProjectNameAwareSkillDefRes> getSkillsAvailableInCatalog(String projectId, String projectNameSearch, String subjectNameSearch, String skillNameSearch, PageRequest pageable) {
        pageable = convertForCatalogSkills(pageable)
        TotalCountAwareResult<ProjectNameAwareSkillDefRes> res = new TotalCountAwareResult<>()

        if (projectNameSearch || subjectNameSearch || skillNameSearch) {
            res.total = exportedSkillRepo.countSkillsInCatalog(projectId, projectNameSearch?:"", subjectNameSearch?:"", skillNameSearch?:"")
            res.results = exportedSkillRepo.getSkillsInCatalog(projectId, projectNameSearch?:"", subjectNameSearch?:"", skillNameSearch?:"", pageable)?.collect { convert(it) }
            return res
        }

        res.total = exportedSkillRepo.countSkillsInCatalog(projectId)
        List<skills.storage.CatalogSkill> catalogSkills = exportedSkillRepo.getSkillsInCatalog(projectId, pageable)
        res.results = catalogSkills?.collect {convert(it)}
        return res
    }

    @Transactional(readOnly = true)
    List<ExportableToCatalogSkillValidationResult> canSkillIdsBeExported(String projectId, List<String> skillIds) {
        List<ExportableToCatalogSkillValidationResult> validationResults = []
        skillIds?.each { String skillId ->
            boolean idConflict = doesSkillIdAlreadyExistInCatalog(skillId)
            SkillDef skillDef = skillDefRepo.findByProjectIdAndSkillId(projectId, skillId)
            assert skillDef, "skillDef should always exist at this stage"
            boolean nameConflict = doesSkillNameAlreadyExistInCatalog(skillDef.name)
            boolean hasDependencies = countDependencies(skillDef.projectId, skillDef.skillId) > 0
            boolean alreadyInCatalog = isAvailableInCatalog(skillDef)
            validationResults.add(new ExportableToCatalogSkillValidationResult(
                    skillId: skillId,
                    projectId: skillDef.projectId,
                    skillAlreadyInCatalog: alreadyInCatalog,
                    skillIdConflictsWithExistingCatalogSkill: idConflict,
                    skillNameConflictsWithExistingCatalogSkill: nameConflict,
                    hasDependencies: hasDependencies
            ))
        }

        return validationResults
    }

    @Transactional(readOnly = true)
    TotalCountAwareResult<ExportedSkillRes> getSkillsExportedByProject(String projectId, Pageable pageable) {
        TotalCountAwareResult<ExportedSkillRes> result = new TotalCountAwareResult()
        Integer count = exportedSkillRepo.countSkillsExportedByProject(projectId)
        result.total = count
        if (count > 0) {
            result.results = exportedSkillRepo.getTinySkillsExportedByProject(projectId, pageable)?.collect { convert(it) }
        }

        return result
    }

    @Transactional(readOnly = true)
    List<SkillDefRes> getSkillsImportedFromCatalog(String projectId, Pageable pageable) {
        skillDefRepo.findImportedSkills(projectId, pageable)?. collect { convert(it) }
    }

    @Transactional(readOnly = true)
    ImportedSkillStats getSkillsImportedStats(String projectId) {
        ImportExportStats stats = skillDefRepo.getImportedSKillStats(projectId)
        ImportedSkillStats res = new ImportedSkillStats()
        if (stats) {
            res.numberOfProjectsImportedFrom = Optional.ofNullable(stats.numberOfProjects).orElse(0)
            res.numberOfSkillsImported = Optional.ofNullable(stats.numberOfSkills).orElse(0)
        }
        return res
    }

    @Transactional(readOnly = true)
    ExportedSkillsStats getSkillsExportedStats(String projectId) {
        //convert result to response model
        ImportExportStats stats = exportedSkillRepo.getExportedSkillStats(projectId)
        ExportedSkillsStats res = new ExportedSkillsStats()
        if (stats) {
            res.numberOfProjectsUsing = Optional.ofNullable(stats.numberOfProjects).orElse(0)
            res.numberOfSkillsExported = Optional.ofNullable(stats.numberOfSkills).orElse(0)
        }
        return res
    }

    @Transactional
    void exportSkillToCatalog(String projectId, String skillId) {
        log.debug("saving exported skill [{}] in project [{}] to the Skill Catalog", projectId, skillId)
        if (isAvailableInCatalog(projectId, skillId)) {
            throw new SkillException("Skill has already been exported to the catalog", projectId, skillId, ErrorCode.SkillAlreadyInCatalog)
        }

        if (doesSkillIdAlreadyExistInCatalog(skillId)) {
            throw new SkillException("Skill id [${skillId}] already exists in the catalog. Duplicated skill ids are not allowed", projectId, skillId, ErrorCode.SkillAlreadyInCatalog)
        }

        ProjDef projDef = projDefAccessor.getProjDef(projectId)
        insufficientPointsValidator.validateProjectPoints(projDef.totalPoints, projDef.projectId, null, ", export to catalog is disallowed")

        if (userCommunityService.isUserCommunityOnlyProject(projDef.projectId)) {
            throw new SkillException("Projects with the community protection are not allowed to export skills to the catalog", projectId, skillId, ErrorCode.AccessDenied)
        }

        SkillDefWithExtra skillDef = skillDefWithExtraRepo.findByProjectIdAndSkillId(projectId, skillId)
        if (!skillDef) {
            throw new SkillException("Skill [${skillId}] doesn't exist.", projectId, skillId, ErrorCode.SkillNotFound)
        }
        if (skillDef.type != SkillDef.ContainerType.Skill) {
            throw new SkillException("Only type=[${SkillDef.ContainerType.Skill}] is supported but provided type=[${skillDef.type}] for skillId=[${skillId}]", projectId, skillId, ErrorCode.BadParam)
        }
        if (!Boolean.valueOf(skillDef.enabled)) {
            throw new SkillException("Skill [${skillDef.skillId}] is disabled. Disabled skills may not be exported to the catalog", projectId, skillId, ErrorCode.ExportToCatalogNotAllowed)
        }
        if (doesSkillNameAlreadyExistInCatalog(skillDef.name)) {
            throw new SkillException("Skill name [${skillDef.name}] already exists in the catalog. Duplicate skill names are not allowed", projectId, skillId, ErrorCode.SkillAlreadyInCatalog)
        }
        SkillDef mySubject = relationshipService.getMySubjectParent(skillDef.id)
        insufficientPointsValidator.validateSubjectPoints(mySubject.totalPoints, mySubject.projectId,  mySubject.skillId, null, ", export to catalog is disallowed")

        Long dependencies = countDependencies(skillDef.projectId, skillDef.skillId)
        if (dependencies > 0) {
            throw new SkillException("Skill [${skillDef.skillId}] has dependencies. Skills with dependencies may not be exported to the catalog", projectId, skillId, ErrorCode.ExportToCatalogNotAllowed)
        }

        SkillsValidator.isTrue(skillDef != null, "skill does not exist", projectId, skillId)

        ExportedSkill exportedSkill = new ExportedSkill(projectId: skillDef.projectId, skill: skillDef)
        exportedSkillRepo.save(exportedSkill)

        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: DashboardAction.ExportToCatalog,
                item: DashboardItem.Skill,
                itemId: skillDef.skillId,
                projectId: projDef.projectId,
        ))
    }


    private Long countDependencies(String projectId, String skillId) {
        Long dependencies = relationshipService.countChildrenSkills(projectId, skillId, [SkillRelDef.RelationshipType.Dependence])
        return dependencies
    }

    @Transactional
    void removeSkillFromCatalog(String projectId, String skillId) {
        if (!isAvailableInCatalog(projectId, skillId)) {
            throw new SkillException("Skill cannot be removed from catalog as it is not in the catalog", projectId, skillId, ErrorCode.SkillNotFound)
        }

        ExportedSkill es = exportedSkillRepo.getCatalogSkill(projectId, skillId)
        List<SkillDef> related = skillDefRepo.findSkillsCopiedFrom(es.skill.id)
        related?.each {
            skillsAdminService.deleteSkill(it.projectId, it.skillId, false)
        }
        exportedSkillRepo.delete(es)

        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: DashboardAction.RemoveFromCatalog,
                item: DashboardItem.Skill,
                itemId: skillId,
                projectId: projectId,
        ))
    }

    @Transactional
    void exportSkillToCatalog(String projectId, List<String> skillIds) {

        skillIds?.each { String skillId ->
            try {
                exportSkillToCatalog(projectId, skillId)
            } catch (Exception throwable) {
                if (throwable instanceof SkillException) {
                    throw throwable
                }
                throw new SkillException("Failed to export batch, the failure was for the skillId [${skillId}]", throwable, projectId, skillId)
            }
        }
    }

    @Transactional(readOnly = true)
    public boolean isSkillImportedFromCatalog(String projectId, String skillId) {
        return skillDefRepo.isImportedFromCatalog(projectId, skillId)
    }

    private Integer getNextReuseTagCount(String projectIdTo, SkillDefWithExtra original) {
        List<String> existingImportedSkillIds = skillDefRepo.getSkillIdsOfReusedSkillsForAGivenSkill(projectIdTo, original.id)
        if (!existingImportedSkillIds) {
            return 0
        }
        Integer maxValue = existingImportedSkillIds.collect { SkillReuseIdUtil.extractReuseCounter(it) }.max()
        return maxValue + 1
    }

    void importSkillFromCatalog(String projectIdFrom, String skillIdFrom, String projectIdTo, SkillDef subjectTo, String groupId, boolean isReusedSkill = false) {
        if (!isReusedSkill) {
            boolean inCatalog = isAvailableInCatalog(projectIdFrom, skillIdFrom)
            SkillsValidator.isTrue(inCatalog, "Skill [${skillIdFrom}] from project [${projectIdFrom}] has not been shared to the catalog and may not be imported")
        }
        projDefAccessor.getProjDef(projectIdFrom)

        SkillDefWithExtra original = skillAccessor.getSkillDefWithExtra(projectIdFrom, skillIdFrom)

        String newName = original.name
        String newSkillId = original.skillId

        if (isReusedSkill) {
            Integer reuseCounter = getNextReuseTagCount(projectIdTo, original)
            newName = SkillReuseIdUtil.addTag(original.name, reuseCounter)
            newSkillId = SkillReuseIdUtil.addTag(original.skillId, reuseCounter)
        }

        if (skillDefRepo.existsByProjectIdAndSkillIdAllIgnoreCase(projectIdTo, newSkillId)) {
            throw new SkillException("Cannot import Skill from catalog, [${newSkillId}] already exists in Project", projectIdTo, skillIdFrom)
        }

        if (skillDefRepo.existsByProjectIdAndNameAndTypeAllIgnoreCase(projectIdTo, newName, SkillDef.ContainerType.Skill)) {
            throw new SkillException("Cannot import Skill from catalog, [${newName}] already exists in Project", projectIdTo, skillIdFrom)
        }

        SkillImportRequest copy = new SkillImportRequest()
        int numToCompletion = original.totalPoints / original.pointIncrement
        Props.copy(original, copy)
        copy.projectId = projectIdTo
        copy.readOnly = 'true'
        copy.copiedFromProjectId = projectIdFrom
        copy.copiedFrom = original.id
        copy.subjectId = subjectTo.skillId
        copy.version = skillsAdminService.findMaxVersionByProjectId(projectIdTo)
        copy.numPerformToCompletion = numToCompletion

        copy.selfReportingType = original.selfReportingType?.toString()
        if (original.selfReportingType && original.selfReportingType == SkillDef.SelfReportingType.Quiz) {
            QuizToSkillDefRepo.QuizNameAndId quizNameAndId = quizToSkillDefRepo.getQuizIdBySkillIdRef(original.id)
            copy.quizId = quizNameAndId.quizId
        }
        copy.enabled = Boolean.FALSE.toString()
        copy.name = newName
        copy.skillId = newSkillId

        skillsAdminService.saveSkill(copy.skillId, copy, true, groupId, false)

        if (!isReusedSkill) {
            userActionsHistoryService.saveUserAction(new UserActionInfo(
                    action: DashboardAction.ImportFromCatalog,
                    item: DashboardItem.Skill,
                    actionAttributes: [
                            fromProjectId: projectIdFrom,
                    ],
                    itemId: newSkillId,
                    projectId: projectIdTo,
            ))
        }
    }

    @Transactional
    @Profile
    void importSkillsFromCatalog(String projectIdTo, String subjectIdTo, List<CatalogSkill> listOfSkills, String groupIdTo = null, boolean isReusedSkill = false) {
        if (skillCatalogFinalizationService.getCurrentState(projectIdTo) == SkillCatalogFinalizationService.FinalizeState.RUNNING) {
            throw new SkillException("Cannot import skills in the middle of the finalization process", projectIdTo)
        }

        int currentSubjectSkillCount = skillRelDefRepo.countSubjectSkillsIncDisabled(projectIdTo, subjectIdTo)
        if (currentSubjectSkillCount + listOfSkills?.size() > maxSubjectSkills) {
            throw new SkillException("Each Subject is limited to [${maxSubjectSkills}] Skills, " +
                    "currently [${subjectIdTo}] has [${currentSubjectSkillCount}] Skills, " +
                    "importing [${listOfSkills?.size()}] would exceed the maximum", ErrorCode.MaxSkillsThreshold)
        }
        log.info("Import skills into the catalog. projectIdTo=[{}], subjectIdTo=[{}], listOfSkills={}", projectIdTo, subjectIdTo, listOfSkills)
        // validate
        projDefAccessor.getProjDef(projectIdTo)

        SkillDef.ContainerType subjectType = SkillDef.ContainerType.Subject
        SkillDef subjectTo = skillDefRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(projectIdTo, subjectIdTo, subjectType)

        if (!subjectTo) {
            throw new SkillException("Requested parent skill id [${subjectIdTo}] doesn't exist for type [${subjectType}].", projectIdTo, subjectIdTo)
        }

        if (groupIdTo) {
            // validate group
            SkillDef skillDef = skillDefRepo.findByProjectIdAndSkillId(projectIdTo, groupIdTo)
            SkillsValidator.isNotNull(skillDef, "Provided group id [${groupIdTo}] does not exist", projectIdTo)
            SkillsValidator.isTrue(skillDef.type == SkillDef.ContainerType.SkillsGroup, "Provided group id [${groupIdTo}] does not reference a group", projectIdTo)

            // validate the provided groupId is in fact under the provided subject
            SkillDef parent = relationshipService.getMySubjectParent(skillDef.id)
            SkillsValidator.isTrue(parent.skillId == subjectIdTo, "Provided group id [${groupIdTo}] belongs to the subject [${parent.skillId}] but provided subject was [${subjectIdTo}]", projectIdTo)
        }

        Set<String> validateProjectIds = new HashSet<>()
        listOfSkills?.each {
            if (!validateProjectIds.contains(it.projectId)) {
                projDefAccessor.getProjDef(it.projectId)
                validateProjectIds.add(it.projectId)
            }

            importSkillFromCatalog(it.projectId, it.skillId, projectIdTo, subjectTo, groupIdTo, isReusedSkill)
        }
    }

    void requestFinalizationOfImportedSkills(String projectId) {
        ProjectTotalPoints projectTotalPoints = projDefRepo.getProjectTotalPointsIncPendingFinalization(projectId)
        insufficientPointsForFinalizationValidator.validateProjectPoints(projectTotalPoints.totalIncPendingFinalized, projectTotalPoints.projectId)

        List<SubjectTotalPoints> subjectTotalPoints = skillRelDefRepo.getSubjectTotalPointsIncPendingFinalization(projectId)
        subjectTotalPoints?.each {
            insufficientPointsForFinalizationValidator.validateSubjectPoints(it.totalIncPendingFinalized, projectId, it.subjectId)
        }

        long numSkillsToFinalizeThatBelongToADisabledSubjectOrGroup = skillDefRepo.countNumSkillsToFinalizeThatBelongToADisabledSubjectOrGroup(projectId)
        if (numSkillsToFinalizeThatBelongToADisabledSubjectOrGroup > 0) {
            throw new SkillException("Cannot finalize imported skills, there are [${numSkillsToFinalizeThatBelongToADisabledSubjectOrGroup}] skill(s) pending finalization that belong to a disabled subject or group", projectId)
        }

        skillCatalogFinalizationService.requestFinalizationOfImportedSkills(projectId)
        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: DashboardAction.FinalizeCatalogImport,
                item: DashboardItem.Project,
                itemId: projectId,
                projectId: projectId,
        ))
    }

    CatalogFinalizeInfoResult getFinalizeInfo(String projectId) {
        long numSkillsToFinalize = skillDefRepo.countByProjectIdAndEnabledAndCopiedFromIsNotNull(projectId, Boolean.FALSE.toString())
        long numSkillsToFinalizeThatBelongToADisabledSubjectOrGroup = skillDefRepo.countNumSkillsToFinalizeThatBelongToADisabledSubjectOrGroup(projectId)
        boolean isRunning = skillCatalogFinalizationService.getCurrentState(projectId) == SkillCatalogFinalizationService.FinalizeState.RUNNING
        SkillDefRepo.MinMaxPoints points = skillDefRepo.getSkillMinAndMaxTotalPoints(projectId)

        List<SkillWithPointsResult> skillsWithOutOfBoundsPoints = []
        if (points?.minPoints) {
            List<SkillDefRepo.SkillWithPoints> skillWithPoints = skillDefRepo.getDisabledSkillsOutOfRange(projectId, points.getMinPoints(), points.getMaxPoints())
            skillsWithOutOfBoundsPoints = skillWithPoints.collect {
                new SkillWithPointsResult(skillId: it.skillId, skillName: it.skillName, totalPoints: it.totalPoints)
            }
        }

        return new CatalogFinalizeInfoResult(
                projectId: projectId,
                numSkillsToFinalize: numSkillsToFinalize,
                numSkillsToFinalizeThatBelongToADisabledSubjectOrGroup: numSkillsToFinalizeThatBelongToADisabledSubjectOrGroup,
                isRunning: isRunning,
                projectSkillMinPoints: points.getMinPoints(),
                projectSkillMaxPoints: points.getMaxPoints(),
                skillsWithOutOfBoundsPoints: skillsWithOutOfBoundsPoints
        )
    }

    @Profile
    @Transactional(readOnly=true)
    boolean isAvailableInCatalog(String projectId, String skillId) {
        // can add sharing restriction checks here in the future
        Boolean retVal = exportedSkillRepo.doesSkillExistInCatalog(projectId, skillId)
        if (retVal == null) {
            return false;
        }
        return retVal.booleanValue()
    }

    @Transactional(readOnly=true)
    boolean doesSkillIdAlreadyExistInCatalog(String skillId) {
        Boolean retVal = exportedSkillRepo.doesSkillIdExistInCatalog(skillId)
        if (retVal == null) {
            return false
        }
        return retVal.booleanValue()
    }

    @Transactional(readOnly=true)
    boolean doesSkillNameAlreadyExistInCatalog(String name) {
        Boolean retVal = exportedSkillRepo.doesSkillNameExistInCatalog(name)
        if (retVal == null) {
            return false
        }
        return retVal.booleanValue()
    }

    @Transactional(readOnly=true)
    boolean doesSkillNameAlreadyExistInCatalog(String projectId, String skillId) {
        SkillDef skillDef = skillDefRepo.findByProjectIdAndSkillId(projectId, skillId)
        Boolean retVal = exportedSkillRepo.doesSkillNameExistInCatalog(skillDef?.name)
        if (retVal == null) {
            return false
        }
        return retVal.booleanValue()
    }

    @Transactional(readOnly = true)
    boolean isAvailableInCatalog(SkillDef skillDef) {
        return exportedSkillRepo.doesSkillExistInCatalog(skillDef.projectId, skillDef.skillId)
    }

    @Transactional
    List<SkillDef> getRelatedSkills(SkillDef skillDef) {
        return this.doGetRelatedSkills(skillDef)
    }

    @Transactional
    List<SkillDefWithExtra> getRelatedSkills(SkillDefWithExtra skillDefWithExtra) {
        return this.doGetRelatedSkills(skillDefWithExtra)
    }

    @Profile
    private List<SkillDefWithExtra> doGetRelatedSkills(SkillDefParent skillDefWithExtra) {
        List<SkillDefWithExtra> related = skillDefWithExtraRepo.findSkillsCopiedFrom(skillDefWithExtra.id)
        if (!related && skillDefWithExtra.copiedFrom != null) {
            related = skillDefWithExtraRepo.findSkillsCopiedFrom(skillDefWithExtra.copiedFrom)
            related = related?.findAll { it.id != skillDefWithExtra.id }
            SkillDefWithExtra og = skillDefWithExtraRepo.findById(skillDefWithExtra.copiedFrom)
            if (og) {
                related.add(og)
            }
        }

        return related
    }

    @Transactional
    @Profile
    List<SkillDefMin> getRelatedSkills(SkillDefMin skillDef) {
        List<SkillDefMin> related = []
        if (isAvailableInCatalog(skillDef.projectId, skillDef.skillId)) {
            related = skillDefRepo.findSkillDefMinCopiedFrom(skillDef.id)
        } else if (skillDef.copiedFrom != null) {
            related = skillDefRepo.findSkillDefMinCopiedFrom(skillDef.copiedFrom)
            related = related?.findAll { it.id != skillDef.id }
            SkillDefMin og = skillDefRepo.findSkillDefMinById(skillDef.copiedFrom)
            if (og) {
                related.add(og)
            }
        }

        return related
    }

    @Transactional
    List<SkillDefMin> getSkillsCopiedFrom(Integer rawId){
        return skillDefRepo.findSkillDefMinCopiedFrom(rawId)
    }

    @Transactional
    void distributeCatalogSkillUpdates(String projectId, String catalogSkillId, Integer rawId) {
        Optional<SkillDefWithExtra> opt = skillDefWithExtraRepo.findById(rawId)
        if (opt.isEmpty()) {
            log.warn("scheduled update for [${projectId} - ${catalogSkillId} - ${rawId}] cannot be performed as the specified catalog skill does not exist")
            return
        }
        SkillDefWithExtra og = opt.get()
        List<SkillDefWithExtra> related = getRelatedSkills(og)

        log.debug("found [{}] imported skills based off of [{}]", related?.size(), og.skillId)
        related?.each { SkillDefWithExtra imported ->
            ReplicatedSkillUpdateRequest copy = new ReplicatedSkillUpdateRequest()
            Props.copy(og, copy)
            copy.copiedFrom = og.id
            copy.projectId = imported.projectId
            copy.copiedFromProjectId = og.projectId
            copy.readOnly = Boolean.TRUE.toString()
            copy.version = imported.version
            copy.numPerformToCompletion = og.totalPoints / og.pointIncrement
            copy.subjectId = skillRelDefRepo.findSubjectSkillIdByChildId(imported.id)
            copy.selfReportingType = og.selfReportingType?.toString()
            if (og.selfReportingType == SkillDef.SelfReportingType.Quiz) {
                QuizToSkillDefRepo.QuizNameAndId quizNameAndId = quizToSkillDefRepo.getQuizIdBySkillIdRef(og.id)
                copy.quizId = quizNameAndId.quizId
            }
            if (SkillReuseIdUtil.isTagged(imported.skillId)) {
                Integer reuseCounter = SkillReuseIdUtil.extractReuseCounter(imported.skillId)
                copy.skillId = SkillReuseIdUtil.addTag(og.skillId, reuseCounter)
                copy.name = SkillReuseIdUtil.addTag(og.name, reuseCounter)
            }
            boolean isReusedSkill = SkillReuseIdUtil.isTagged(copy.skillId) && og.projectId == copy.projectId
            skillsAdminService.saveSkill(imported.skillId, copy, !isReusedSkill)
        }
    }

    @Transactional
    ExportedSkillStats getExportedSkillStats(String projectId, String skillId) {
        ExportedSkillStats stats = new ExportedSkillStats(
                projectId: projectId,
                skillId: skillId,
                isExported: false,
                isReusedLocally: false,
                users: [])

        SkillDef skill = skillAccessor.getSkillDef(projectId, skillId)
        List<SkillDefWithExtra> copies = skillDefWithExtraRepo.findSkillsCopiedFrom(skill.id)
        if (!copies) {
            return stats
        }
        stats.isReusedLocally = copies.find({ it.projectId == projectId })
        copies.findAll { it.projectId != projectId }.each {
            SkillDef mySubject = relationshipService.getMySubjectParent(it.id)
            ProjDef myProjDef = projDefRepo.findById(mySubject.projRefId)?.get()
            stats.users << new ExportedSkillUser(
                    importingProjectId: it.projectId,
                    importingProjectName: myProjDef?.name,
                    importedOn: it.created,
                    importedIntoSubjectId: mySubject.skillId,
                    importedIntoSubjectName: mySubject.name,
                    enabled: it.enabled,
            )
        }

        ExportedSkill es = exportedSkillRepo.getCatalogSkill(projectId, skillId)
        if (es) {
            stats.exportedOn = es.created
            stats.isExported = true
        }

        return stats
    }

    @Transactional(readOnly = true)
    List<String> getSkillIdsInCatalog(String projectId, List<String> skillIds) {
        exportedSkillRepo.doSkillsExistInCatalog(projectId, skillIds)
    }

    @Transactional
    void updateImportedSkill(String projectId, String skillId, ImportedSkillUpdate importedSkillUpdate) {
        SkillDefWithExtra skillDefWithExtra = skillAccessor.getSkillDefWithExtra(projectId, skillId, [SkillDef.ContainerType.Skill])
        SkillDef subject = relationshipService.getMySubjectParent(skillDefWithExtra.id)

        SkillRequest skillRequest = new SkillRequest(
                pointIncrement: importedSkillUpdate.pointIncrement, // update
                skillId: skillDefWithExtra.skillId,
                projectId: skillDefWithExtra.projectId,
                subjectId: subject.skillId,
                name: skillDefWithExtra.name,
                pointIncrementInterval: skillDefWithExtra.pointIncrementInterval,
                numMaxOccurrencesIncrementInterval: skillDefWithExtra.numMaxOccurrencesIncrementInterval,
                numPerformToCompletion: (skillDefWithExtra.totalPoints / skillDefWithExtra.pointIncrement),
                version: skillDefWithExtra.version,
                description: skillDefWithExtra.description,
                helpUrl: skillDefWithExtra.helpUrl,
                selfReportingType: skillDefWithExtra.selfReportingType,
                enabled: skillDefWithExtra.enabled,
        )

        if (skillDefWithExtra.selfReportingType && skillDefWithExtra.selfReportingType == SkillDef.SelfReportingType.Quiz) {
            QuizToSkillDefRepo.QuizNameAndId quizNameAndId = quizToSkillDefRepo.getQuizIdBySkillIdRef(skillDefWithExtra.copiedFrom)
            skillRequest.quizId = quizNameAndId.quizId
        }

        skillsAdminService.saveSkill(skillDefWithExtra.skillId, skillRequest, false)

    }

    @Transactional(readOnly=true)
    public PointsIncludingNotFinalized getTotalPointsIncludingPendingFinalization(String projectId) {
        if (!projDefRepo.existsByProjectIdIgnoreCase(projectId)) {
            throw new SkillException("Project with id [${projectId}] does NOT exist")
        }

        ProjectTotalPoints projectTotalPoints = projDefRepo.getProjectTotalPointsIncPendingFinalization(projectId)
        //TODO: subject with zero points isn't being included....figure that out.
        List<SubjectTotalPoints> subjectTotalPoints = skillRelDefRepo.getSubjectTotalPointsIncPendingFinalization(projectId)

        PointsIncludingNotFinalized pointsIncludingPendingFinalized = new PointsIncludingNotFinalized(projectId: projectId)
        pointsIncludingPendingFinalized.projectTotalPoints = projectTotalPoints.totalIncPendingFinalized
        pointsIncludingPendingFinalized.projectName = projectTotalPoints.name

        if (projectTotalPoints.totalIncPendingFinalized < minimumProjectPoints) {
            pointsIncludingPendingFinalized.insufficientProjectPoints = true
        }

        if (subjectTotalPoints) {
            subjectTotalPoints.each {
                SubjectTotalPointsIncNotFinalized converted = new SubjectTotalPointsIncNotFinalized(subjectId: it.subjectId, subjectName: it.name, totalPoints: it.totalIncPendingFinalized)
                if (converted.totalPoints < minimumSubjectPoints) {
                    pointsIncludingPendingFinalized.subjectsWithInsufficientPoints << converted
                } else {
                    pointsIncludingPendingFinalized.subjectsMeetingMinimumPoints << converted
                }
            }
        }

        return pointsIncludingPendingFinalized
    }

    private static PageRequest convertForCatalogSkills(PageRequest pageRequest) {
        int pageNum = pageRequest.getPageNumber()
        int pageSize = pageRequest.getPageSize()
        Sort sort = pageRequest.getSort()
        if (!sort.isEmpty()) {
            List<Sort.Order> props = []
            sort.get().forEach({
                if (it.property == "numPerformToCompletion") {
                    SkillException ske = new SkillException("Sorting on numPerformToCompletion is not allowed")
                    ske.errorCode = ErrorCode.BadParam
                    throw ske
                }
                if (!aliasUnnecessary.contains(it.property)) {
                    props.add(new Sort.Order(it.direction, "skill.${it.property}"))
                } else {
                    props.add(it)
                }
            })

            pageRequest = PageRequest.of(pageNum, pageSize, Sort.by(props))
        }

        return pageRequest
    }

    private static SkillDefRes convert(SkillDef skillDef) {
        SkillDefRes res = new SkillDefRes()
        Props.copy(skillDef, res)
        res.numPerformToCompletion = skillDef.totalPoints / res.pointIncrement
        return res
    }

    private static ExportedSkillRes convert(ExportedSkillTiny exportedSkillTiny) {
        ExportedSkillRes esr = new ExportedSkillRes()
        esr.skillId = exportedSkillTiny.skillId
        esr.skillName = InputSanitizer.unsanitizeName(exportedSkillTiny.skillName)
        esr.subjectName = InputSanitizer.unsanitizeName(exportedSkillTiny.subjectName)
        esr.exportedOn = exportedSkillTiny.exportedOn
        esr.subjectId = exportedSkillTiny.subjectId
        esr.importedProjectCount = exportedSkillTiny.importedProjectCount
        esr.groupName = exportedSkillTiny.groupName
        return esr
    }

    private static ProjectNameAwareSkillDefRes convert(skills.storage.CatalogSkill catalogSkill) {
        // Not sure this is copying correctly
        CatalogSkillRes partial = new CatalogSkillRes()
        Props.copy(catalogSkill.skill, partial)
        partial.subjectId = catalogSkill.subjectId
        partial.subjectName = InputSanitizer.unsanitizeName(catalogSkill.subjectName)
        partial.projectName = InputSanitizer.unsanitizeName(catalogSkill.projectName)
        partial.exportedOn = catalogSkill.exportedOn
        partial.skillIdAlreadyExist = catalogSkill.getSkillIdAlreadyExist()
        partial.skillNameAlreadyExist = catalogSkill.getSkillNameAlreadyExist()
        partial.sharedToCatalog = true
        partial.name = InputSanitizer.unsanitizeName(partial.name)
        partial.numPerformToCompletion = catalogSkill.skill.totalPoints / catalogSkill.skill.pointIncrement
        return partial
    }
}
