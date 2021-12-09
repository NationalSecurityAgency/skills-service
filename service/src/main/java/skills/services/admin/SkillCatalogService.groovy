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

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.exceptions.SkillsValidator
import skills.controller.request.model.CatalogSkill
import skills.controller.request.model.SkillImportRequest
import skills.controller.result.model.CatalogSkillRes
import skills.controller.result.model.ExportedSkillRes
import skills.controller.result.model.ExportedSkillStats
import skills.controller.result.model.ExportedSkillUser
import skills.controller.result.model.ExportedSkillsStats
import skills.controller.result.model.ImportedSkillStats
import skills.controller.result.model.ProjectNameAwareSkillDefRes
import skills.controller.result.model.SkillDefRes
import skills.services.LockingService
import skills.services.RuleSetDefGraphService
import skills.storage.accessors.ProjDefAccessor
import skills.storage.accessors.SkillDefAccessor
import skills.storage.model.ExportedSkill
import skills.storage.model.ExportedSkillTiny
import skills.storage.model.ImportExportStats
import skills.storage.model.QueuedSkillUpdate
import skills.storage.model.SkillDef
import skills.storage.model.SkillDefMin
import skills.storage.model.SkillDefWithExtra
import skills.storage.repos.ExportedSkillRepo
import skills.storage.repos.QueuedSkillUpdateRepo
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.SkillDefWithExtraRepo
import skills.utils.Props

@Service
@Slf4j
class SkillCatalogService {

    @Autowired
    ExportedSkillRepo exportedSkillRepo

    @Autowired
    SkillDefAccessor skillAccessor

    @Autowired
    ProjDefAccessor projDefAccessor

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    RuleSetDefGraphService relationshipService

    @Autowired
    SkillsAdminService skillsAdminService

    @Autowired
    QueuedSkillUpdateRepo queuedSkillUpdateRepo

    @Autowired
    LockingService lockingService

    @Autowired
    SkillDefWithExtraRepo skillDefWithExtraRepo


    @Transactional(readOnly = true)
    TotalCountAwareResult<ProjectNameAwareSkillDefRes> getSkillsAvailableInCatalog(String projectId, String projectNameSearch, String subjectNameSearch, String skillNameSearch, PageRequest pageable) {
        //TODO: projectId will need to be eventually used to govern accessibility to shared skills
        //e.g., projects will more than likely want to share skills to the catalog with specific projects only
        // because these methods return a projection, we need to alias the sort keys and prefix any that aren't
        // projectName, subjectName, subjectId with "skill."
        pageable = convertForCatalogSkills(pageable)
        TotalCountAwareResult<ProjectNameAwareSkillDefRes> res = new TotalCountAwareResult<>()

        if (projectNameSearch || subjectNameSearch || skillNameSearch) {
            res.total = exportedSkillRepo.countSkillsInCatalog(projectId, projectNameSearch, subjectNameSearch, skillNameSearch)
            res.results = exportedSkillRepo.getSkillsInCatalog(projectId, projectNameSearch, subjectNameSearch, skillNameSearch, pageable)?.findAll { it.skill.projectId != projectId}?.collect { convert(it)}
            return res
        }

        res.total = exportedSkillRepo.countSkillsInCatalog(projectId)
        def catalogSkills = exportedSkillRepo.getSkillsInCatalog(projectId, pageable)?.findAll { it.skill.projectId != projectId}
        res.results = catalogSkills?.collect {convert(it)}
        return res
    }

    private static final Set<String> aliasUnnecessary = Set.of("projectName", "subjectName", "subjectId", "exportedOn")
    private static PageRequest convertForCatalogSkills(PageRequest pageRequest) {
        int pageNum = pageRequest.getPageNumber()
        int pageSize = pageRequest.getPageSize()
        Sort sort = pageRequest.getSort()
        if (!sort.isEmpty()) {
            List<Sort.Order> props = []
            sort.get().forEach({
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
            res.numberOfProjectsImportedFrom = stats.numberOfProjects
            res.numberOfSkillsImported = stats.numberOfSkills
        }
        return res
    }

    @Transactional(readOnly = true)
    ExportedSkillsStats getSkillsExportedStats(String projectId) {
        //convert result to response model
        ImportExportStats stats = exportedSkillRepo.getExportedSkillStats(projectId)
        ExportedSkillsStats res = new ExportedSkillsStats()
        if (stats) {
            res.numberOfProjectsUsing = stats.numberOfProjects
            res.numberOfSkillsExported = stats.numberOfSkills
        }
        return stats
    }

    @Transactional
    void exportSkillToCatalog(String projectId, String skillId) {
        log.debug("saving exported skill [{}] in project [{}] to the Skill Catalog", projectId, skillId)
        if (isAvailableInCatalog(projectId, skillId)) {
            throw new SkillException("Skill has already been exported to the catalog", projectId, skillId, ErrorCode.SkillAlreadyInCatalog)
        }
        projDefAccessor.getProjDef(projectId)
        SkillDefWithExtra skillDef = skillDefWithExtraRepo.findByProjectIdAndSkillIdAndType(projectId, skillId, SkillDef.ContainerType.Skill)
        SkillsValidator.isTrue(skillDef != null, "skill does not exist", projectId, skillId)

        ExportedSkill exportedSkill = new ExportedSkill(projectId: skillDef.projectId, skill: skillDef)
        exportedSkillRepo.save(exportedSkill)
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
            exportSkillToCatalog(projectId, skillId)
        }
    }

    @Transactional
    void importSkillFromCatalog(String projectIdFrom, String skillIdFrom, String projectIdTo, String subjectIdTo) {
        boolean inCatalog = isAvailableInCatalog(projectIdFrom, skillIdFrom)
        SkillsValidator.isTrue(inCatalog, "Skill [${skillIdFrom}] from project [${projectIdFrom}] has not been shared to the catalog and may not be imported")
        projDefAccessor.getProjDef(projectIdFrom)
        projDefAccessor.getProjDef(projectIdTo)
        SkillDef original = skillAccessor.getSkillDef(projectIdFrom, skillIdFrom)

        SkillDef.ContainerType subjectType = SkillDef.ContainerType.Subject
        SkillDef subject = skillDefRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(projectIdTo, subjectIdTo, subjectType)

        if (!subject) {
            throw new SkillException("Requested parent skill id [${subjectIdTo}] doesn't exist for type [${subjectType}].", projectIdTo, subjectIdTo)
        }

        if (skillDefRepo.existsByProjectIdAndSkillIdAllIgnoreCase(projectIdTo, skillIdFrom)) {
            throw new SkillException("Cannot import Skill from catalog, [${skillIdFrom}] already exists in Project", projectIdTo, skillIdFrom)
        }

        SkillImportRequest copy = new SkillImportRequest()
        int numToCompletion = original.totalPoints / original.pointIncrement
        Props.copy(original, copy)
        copy.projectId = projectIdTo
        copy.readOnly = 'true'
        copy.copiedFromProjectId = projectIdFrom
        copy.copiedFrom = original.id
        copy.subjectId = subject.skillId
        copy.version = skillsAdminService.findLatestSkillVersion(projectIdTo)
        copy.numPerformToCompletion = numToCompletion

        skillsAdminService.saveSkill(copy.skillId, copy)
    }

    @Transactional
    void importSkillsFromCatalog(String projectIdTo, String subjectIdTo, List<CatalogSkill> listOfSkills) {
        listOfSkills?.each {
            importSkillFromCatalog(it.projectId, it.skillId, projectIdTo, subjectIdTo)
        }
    }

    @Transactional(readOnly=true)
    boolean isAvailableInCatalog(String projectId, String skillId) {
        // can add sharing restriction checks here in the future
        Boolean retVal = exportedSkillRepo.doesSkillExistInCatalog(projectId, skillId)
        if (retVal == null) {
            return false;
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
    void distributeCatalogSkillUpdates() {
        lockingService.lockForUpdatingCatalogSkills()
        Iterable<QueuedSkillUpdate> queuedSkillUpdates = queuedSkillUpdateRepo.findAll()

        log.debug("found [${queuedSkillUpdates?.size()}] updated catalog skills")
        queuedSkillUpdates.groupBy { it.skill.id }.each{key, value ->
            SkillDef og = value?.first()?.skill
            List<SkillDef> related = getRelatedSkills(og)
            log.debug("found [${related?.size()}] imported skills based off of [${og.skillId}]")
            related?.each { SkillDef imported ->
                ReplicatedSkillUpdateRequest copy = new ReplicatedSkillUpdateRequest()
                Props.copy(og, copy)
                copy.copiedFrom = og.id
                copy.projectId = imported.projectId
                copy.copiedFromProjectId = og.projectId
                copy.readOnly = Boolean.TRUE.toString()
                copy.version = imported.version
                copy.numPerformToCompletion = og.totalPoints / og.pointIncrement
                copy.subjectId = relationshipService.getParentSkill(imported).skillId
                skillsAdminService.saveSkill(imported.skillId, copy)
            }
        }

        if (queuedSkillUpdates) {
            queuedSkillUpdateRepo.deleteAll(queuedSkillUpdates)
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
            SkillDef subject = relationshipService.getParentSkill(it)
            stats.users << new ExportedSkillUser(importingProjectId: it.projectId, importedOn: it.created, importedIntoSubjectId: subject.skillId)
        }

        return stats
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
        esr.skillName = exportedSkillTiny.skillName
        esr.subjectName = exportedSkillTiny.subjectName
        esr.exportedOn = exportedSkillTiny.exportedOn
        esr.subjectId = exportedSkillTiny.subjectId
        return esr
    }

    private static ProjectNameAwareSkillDefRes convert(skills.storage.CatalogSkill catalogSkill) {
        // Not sure this is copying correctly
        CatalogSkillRes partial = new CatalogSkillRes()
        Props.copy(catalogSkill.skill, partial)
        partial.subjectId = catalogSkill.subjectId
        partial.subjectName = catalogSkill.subjectName
        partial.projectName = catalogSkill.projectName
        partial.exportedOn = catalogSkill.exportedOn
        partial.sharedToCatalog = true
        partial.numPerformToCompletion = catalogSkill.skill.totalPoints / catalogSkill.skill.pointIncrement
        return partial
    }
}
