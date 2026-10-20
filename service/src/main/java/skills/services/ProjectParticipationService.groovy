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
import skills.controller.result.model.ProjectProgressInitializationResult
import skills.storage.accessors.ProjDefAccessor
import skills.storage.model.ProjDef
import skills.storage.model.ProjectParticipation.BackfillStatus
import skills.storage.repos.*

@Service
class ProjectParticipationService {
    @Autowired ProjectParticipationRepo projectParticipationRepo
    @Autowired UserPerformedSkillRepo userPerformedSkillRepo
    @Autowired ProjDefAccessor projDefAccessor
    @Autowired CatalogParticipantBackfillService backfillService

    boolean isEligible(String projectId, String userId) {
        projectParticipationRepo.existsByProjectIdAndUserId(projectId, userId) ||
                userPerformedSkillRepo.existsLocalEventByProjectIdAndUserId(projectId, userId)
    }

    ProjectProgressInitializationResult initialize(String projectId, String userId) {
        ProjDef project = projDefAccessor.getProjDef(projectId)
        String canonicalProjectId = project.projectId
        String canonicalUserId = userId.toLowerCase(Locale.ROOT)
        BackfillStatus status = backfillService.touchEvaluatedVisit(canonicalProjectId, canonicalUserId)
        if (!status || status == BackfillStatus.NOT_REQUESTED) {
            status = backfillService.qualify(canonicalProjectId, canonicalUserId, true)
        }
        boolean ready = status in [BackfillStatus.NOT_NEEDED, BackfillStatus.COMPLETED] &&
                !backfillService.hasApplicableFinalization(canonicalProjectId)
        new ProjectProgressInitializationResult(ready: ready, backfillStatus: status)
    }
}
