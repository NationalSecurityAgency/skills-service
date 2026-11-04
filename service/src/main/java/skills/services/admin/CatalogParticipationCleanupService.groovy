/**
 * Copyright 2026 SkillTree
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
import org.slf4j.spi.LoggingEventBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import skills.controller.exceptions.SkillsValidator
import skills.storage.model.ProjDef
import skills.storage.repos.ProjDefRepo
import skills.storage.repos.CatalogParticipationCleanupRepo

/** Temporary pre-5.1 remediation; remove endpoint-specific tooling after the remediation window. */
@Service
@Slf4j
class CatalogParticipationCleanupService {
    @Autowired ProjDefRepo projectRepo
    @Autowired CatalogParticipationCleanupRepo cleanupRepo
    @Autowired BatchOperationsTransactionalAccessor batchAccessor

    String canonicalProject(String projectId) {
        SkillsValidator.isTrue(projectId != null && !projectId.trim().isEmpty(), 'A nonblank project ID is required')
        ProjDef project = projectRepo.findByProjectIdIgnoreCase(projectId.trim())
        SkillsValidator.isTrue(project != null, "Unknown project [${projectId}]", projectId)
        project.projectId
    }

    // No encompassing transaction: validate everything first, then independently commit each project.
    Map cleanup(Map request) {
        List<String> projects
        if (request?.containsKey('projectIds')) {
            Object selection = request.projectIds
            SkillsValidator.isTrue(selection instanceof List && !selection.isEmpty(), 'projectIds must be a nonempty list of project IDs')
            projects = selection.collect { Object id ->
                SkillsValidator.isTrue(id instanceof String, 'Each project ID must be a nonblank string')
                canonicalProject((String) id)
            }.unique().sort()
        } else {
            projects = cleanupRepo.findProjectIdsWithEnabledCatalogImports()
        }
        long start = System.nanoTime()
        log.info('Legacy catalog cleanup request: projects=[{}]', projects)
        List<Map> results = projects.collect { String projectId ->
            long projectStart = System.nanoTime()
            Map result
            Exception projectFailure
            log.info('Legacy catalog cleanup starting: project=[{}]', projectId)
            try {
                result = batchAccessor.cleanupCatalogParticipation(projectId)
            } catch (Exception failure) {
                projectFailure = failure
                result = [projectId: projectId, status: 'FAILED', reason: "Project transaction rolled back (${failure.class.simpleName}); see cleanup diagnostics and retry this project",
                          deletedRecords: 0L, deletedPointRows: 0, deletedAchievementRows: 0]
            }
            result.elapsedMs = (System.nanoTime() - projectStart) / 1_000_000L
            // One terminal outcome after the proxy commits/rolls back, including every skip path.
            LoggingEventBuilder outcomeLog = result.status == 'FAILED' ? log.atError().setCause(projectFailure) :
                    (result.status == 'SKIPPED' ? log.atWarn() : log.atInfo())
            outcomeLog.log('CATALOG_PARTICIPATION_CLEANUP_PROJECT_RESULT projectId=[{}] status=[{}] reason=[{}] elapsedMs=[{}] deletedRecords=[{}] deletedPointRows=[{}] deletedAchievementRows=[{}]',
                    projectId, result.status, result.reason ?: 'None', result.elapsedMs,
                    result.deletedRecords, result.deletedPointRows, result.deletedAchievementRows)
            result
        }
        Map summary = [projects: results, elapsedMs: (System.nanoTime() - start) / 1_000_000L,
                       deletedRecords: results.sum { Map result -> result.deletedRecords } ?: 0L,
                       deletedPointRows: results.sum { Map result -> (long) result.deletedPointRows } ?: 0L,
                       deletedAchievementRows: results.sum { Map result -> (long) result.deletedAchievementRows } ?: 0L]
        log.info('Legacy catalog cleanup finished: {}', summary)
        summary
    }
}
