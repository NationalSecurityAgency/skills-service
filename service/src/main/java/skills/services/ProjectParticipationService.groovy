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
package skills.services

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.result.model.ProjectProgressInitializationResult
import skills.storage.accessors.ProjDefAccessor
import skills.storage.model.ProjectParticipation
import skills.storage.model.ProjDef
import skills.storage.model.ProjectParticipation.BackfillStatus
import skills.storage.repos.*

@Service
class ProjectParticipationService {
    @Autowired ProjectParticipationRepo projectParticipationRepo
    @Autowired UserPerformedSkillRepo userPerformedSkillRepo
    @Autowired ProjDefAccessor projDefAccessor
    @Autowired SkillDefRepo skillDefRepo
    @Autowired LockingService lockingService

    boolean isEligible(String projectId, String userId) {
        projectParticipationRepo.existsByProjectIdAndUserId(projectId, userId) ||
                userPerformedSkillRepo.existsLocalEventByProjectIdAndUserId(projectId, userId)
    }

    @Transactional(timeout = 30)
    ProjectProgressInitializationResult initialize(String projectId, String userId) {
        ProjDef project = projDefAccessor.getProjDef(projectId)
        String canonicalProjectId = project.projectId
        String canonicalUserId = userId.toLowerCase(Locale.ROOT)
        lockingService.lockForSkillReporting(canonicalUserId, canonicalProjectId)
        lockingService.lockCatalogActivation(canonicalProjectId)
        ProjectParticipation participation = projectParticipationRepo.findByProjectIdAndUserId(canonicalProjectId, canonicalUserId)
        boolean imports = skillDefRepo.existsEnabledCatalogImport(canonicalProjectId)
        if (!participation) {
            participation = projectParticipationRepo.save(new ProjectParticipation(
                    projectId: canonicalProjectId, userId: canonicalUserId,
                    backfillStatus: imports ? BackfillStatus.NOT_REQUESTED : BackfillStatus.NOT_NEEDED))
        } else if (!imports && participation.backfillStatus == BackfillStatus.NOT_REQUESTED) {
            participation.backfillStatus = BackfillStatus.NOT_NEEDED
            projectParticipationRepo.save(participation)
        } else {
            Date now = new Date()
            projectParticipationRepo.touchActivity(canonicalProjectId, canonicalUserId, now)
        }
        // Catalog backfill/catch-up readiness is connected in slice 2. Even a terminal
        // initial status cannot prove readiness of an enabled batch on its own.
        boolean ready = !imports && participation.backfillStatus in [BackfillStatus.NOT_NEEDED, BackfillStatus.COMPLETED]
        new ProjectProgressInitializationResult(ready: ready, backfillStatus: participation.backfillStatus)
    }
}
