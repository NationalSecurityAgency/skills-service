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
package skills.services

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import skills.storage.ProjectError
import skills.storage.repos.ProjectErrorRepo

@Slf4j
@Component
class ProjectErrorService {

    @Autowired
    ProjectErrorRepo errorRepo

    @Transactional
    public void invalidSkillReported(String projectId, String reportedSkillId) {
        ProjectError error = errorRepo.findByProjectIdAndReportedSkillId(projectId, reportedSkillId)
        if (!error) {
            error = new ProjectError(projectId: projectId, reportedSkillId: reportedSkillId, created: new Date(), count: 0)
        }
        error.count += 1
        error.lastSeen = new Date()

        errorRepo.save(error)
    }

    @Transactional
    public void deleteError(String projectId, String reportedSkillId) {
        ProjectError error = errorRepo.findByProjectIdAndReportedSkillId(projectId, reportedSkillId)
        if (error) {
            log.info("deleting error for [${projectId}]-[${reportedSkillId}]")
            errorRepo.delete(error)
        } else {
            log.warn("ProjectError does not exists for [${projectId}]-[${reportedSkillId}")
        }
    }

    @Transactional
    public void deleteAllErrors(String projectId) {
        errorRepo.deleteByProjectId(projectId)
    }

    @Transactional(readOnly = true)
    public List<skills.controller.result.model.ProjectError> getAllErrorsForProject(String projectId) {
        List<skills.controller.result.model.ProjectError> errs = []
        errorRepo.findAllByProjectId(projectId)?.forEach({
            errs << new skills.controller.result.model.ProjectError(
                    projectId: it.projectId,
                    reportedSkillId: it.reportedSkillId,
                    error: it.error,
                    created: it.created,
                    lastSeen: it.lastSeen,
                    count: it.count
            )
        })
        return errs
    }

    @Transactional(readOnly = true)
    public long countOfErrorsForProject(String projectId) {
        return errorRepo.countByProjectId(projectId)
    }
}
