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
import skills.services.events.pointsAndAchievements.InsufficientPointsForFinalizationValidator
import skills.services.events.pointsAndAchievements.InsufficientPointsValidator
import skills.storage.accessors.ProjDefAccessor
import skills.storage.accessors.SkillDefAccessor
import skills.storage.model.*
import skills.storage.repos.ExportedSkillRepo
import skills.storage.repos.ProjDefRepo
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.SkillDefWithExtraRepo
import skills.storage.repos.SkillRelDefRepo
import skills.storage.repos.nativeSql.NativeQueriesRepo
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
    SkillCatalogFinalizationService skillCatalogFinalizationService\

    @Autowired
    InsufficientPointsForFinalizationValidator insufficientPointsForFinalizationValidator

    @Autowired
    InsufficientPointsValidator insufficientPointsValidator

    @Autowired
    ProjDefRepo projDefRepo

    @Autowired
    NativeQueriesRepo nativeQueriesRepo

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

        SkillDefWithExtra skillDef = skillDefWithExtraRepo.findByProjectIdAndSkillId(projectId, skillId)
        if (!skillDef) {
            throw new SkillException("Skill [${skillId}] doesn't exist.", projectId, skillId, ErrorCode.SkillNotFound)
        }
        if (skillDef.type != SkillDef.ContainerType.Skill) {
            throw new SkillException("Only type=[${SkillDef.ContainerType.Skill}] is supported but provided type=[${skillDef.type}] for skillId=[${skillId}]", projectId, skillId, ErrorCode.BadParam)
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
            SkillDef subject = relationshipService.getParentSkill(it)
            skillsAdminService.deleteSkill(it.projectId, subject.skillId, it.skillId)
        }
        exportedSkillRepo.delete(es)
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

    private void importSkillFromCatalog(String projectIdFrom, String skillIdFrom, String projectIdTo, SkillDef subjectTo, String groupId) {
        boolean inCatalog = isAvailableInCatalog(projectIdFrom, skillIdFrom)
        SkillsValidator.isTrue(inCatalog, "Skill [${skillIdFrom}] from project [${projectIdFrom}] has not been shared to the catalog and may not be imported")
        projDefAccessor.getProjDef(projectIdFrom)

        SkillDefWithExtra original = skillAccessor.getSkillDefWithExtra(projectIdFrom, skillIdFrom)

        if (skillDefRepo.existsByProjectIdAndSkillIdAllIgnoreCase(projectIdTo, skillIdFrom)) {
            throw new SkillException("Cannot import Skill from catalog, [${skillIdFrom}] already exists in Project", projectIdTo, skillIdFrom)
        }

        if (skillDefRepo.existsByProjectIdAndNameAndTypeAllIgnoreCase(projectIdTo, original.name, SkillDef.ContainerType.Skill)) {
            throw new SkillException("Cannot import Skill from catalog, [${original.name}] already exists in Project", projectIdTo, skillIdFrom)
        }

        SkillImportRequest copy = new SkillImportRequest()
        int numToCompletion = original.totalPoints / original.pointIncrement
        Props.copy(original, copy)
        copy.projectId = projectIdTo
        copy.readOnly = 'true'
        copy.copiedFromProjectId = projectIdFrom
        copy.copiedFrom = original.id
        copy.subjectId = subjectTo.skillId
        copy.version = skillsAdminService.findLatestSkillVersion(projectIdTo)
        copy.numPerformToCompletion = numToCompletion
        copy.selfReportingType = original.selfReportingType?.toString()
        copy.enabled = Boolean.FALSE.toString()

        skillsAdminService.saveSkill(copy.skillId, copy, true, groupId)
    }

    @Transactional
    @Profile
    void importSkillsFromCatalog(String projectIdTo, String subjectIdTo, List<CatalogSkill> listOfSkills, String groupIdTo = null) {
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
        }

        Set<String> validateProjectIds = new HashSet<>()
        listOfSkills?.each {
            if (!validateProjectIds.contains(it.projectId)) {
                projDefAccessor.getProjDef(it.projectId)
                validateProjectIds.add(it.projectId)
            }

            importSkillFromCatalog(it.projectId, it.skillId, projectIdTo, subjectTo, groupIdTo)
        }
    }

    void requestFinalizationOfImportedSkills(String projectId) {
        ProjectTotalPoints projectTotalPoints = projDefRepo.getProjectTotalPointsIncPendingFinalization(projectId)
        insufficientPointsForFinalizationValidator.validateProjectPoints(projectTotalPoints.totalIncPendingFinalized, projectTotalPoints.projectId)

        List<SubjectTotalPoints> subjectTotalPoints = skillRelDefRepo.getSubjectTotalPointsIncPendingFinalization(projectId)
        subjectTotalPoints?.each {
            insufficientPointsForFinalizationValidator.validateSubjectPoints(it.totalIncPendingFinalized, projectId, it.subjectId)
        }

        skillCatalogFinalizationService.requestFinalizationOfImportedSkills(projectId)
    }

    CatalogFinalizeInfoResult getFinalizeInfo(String projectId) {
        int numDisabled = skillDefRepo.countByProjectIdAndEnabledAndCopiedFromIsNotNull(projectId, Boolean.FALSE.toString())
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
                numSkillsToFinalize: numDisabled,
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
        List<SkillDef> related = []
        if (isAvailableInCatalog(skillDef.projectId, skillDef.skillId)) {
            related = skillDefRepo.findSkillsCopiedFrom(skillDef.id)
        } else if (skillDef.copiedFrom != null) {
            related = skillDefRepo.findSkillsCopiedFrom(skillDef.copiedFrom)
            related = related?.findAll { it.id != skillDef.id }
            SkillDef og = skillDefRepo.findById(skillDef.copiedFrom)
            if (og) {
                related.add(og)
            }
        }

        return related
    }

    @Transactional
    @Profile
    List<SkillDefWithExtra> getRelatedSkills(SkillDefWithExtra skillDefWithExtra) {
        List<SkillDefWithExtra> related = []
        if (isAvailableInCatalog(skillDefWithExtra.projectId, skillDefWithExtra.skillId)) {
            related = skillDefWithExtraRepo.findSkillsCopiedFrom(skillDefWithExtra.id)
        } else if (skillDefWithExtra.copiedFrom != null) {
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
            skillsAdminService.saveSkill(imported.skillId, copy)
        }
    }

    @Transactional
    ExportedSkillStats getExportedSkillStats(String projectId, String skillId) {
        if (!isAvailableInCatalog(projectId, skillId)) {
            throw new SkillException("Skill is not shared to the catalog", projectId, skillId)
        }

        ExportedSkillStats stats = new ExportedSkillStats()
        ExportedSkill es = exportedSkillRepo.getCatalogSkill(projectId, skillId)
        stats.projectId = projectId
        stats.skillId = skillId
        stats.exportedOn = es.created
        stats.users = []
        List<SkillDef> copies = skillDefRepo.findSkillsCopiedFrom(es.skill.id)
        copies?.each {
            SkillDef maybeSubject = relationshipService.getParentSkill(it)
            if (maybeSubject.type == SkillDef.ContainerType.SkillsGroup) {
                maybeSubject = relationshipService.getParentSkill(maybeSubject)
                assert maybeSubject && maybeSubject.type == SkillDef.ContainerType.Subject, "a group should always be contained by a subject"
            }
            stats.users << new ExportedSkillUser(
                    importingProjectId: it.projectId,
                    importingProjectName: maybeSubject.projDef.name,
                    importedOn: it.created, importedIntoSubjectId:
                    maybeSubject.skillId,
                    importedIntoSubjectName: maybeSubject.name,
                    enabled: it.enabled,
            )
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
