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
import org.jsoup.helper.Validate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import skills.controller.exceptions.SkillException
import skills.controller.result.model.TableResult
import skills.storage.model.ProjectError
import skills.storage.repos.ProjectErrorRepo

@Slf4j
@Component
class ProjectErrorService {

    @Autowired
    ProjectErrorRepo errorRepo

    @Transactional
    public void invalidSkillReported(String projectId, String reportedSkillId) {
        Validate.notNull(reportedSkillId, "reportedSkillId is required")

        ProjectError error = errorRepo.findByProjectIdAndErrorTypeAndError(projectId, ProjectError.ErrorType.SkillNotFound, reportedSkillId)
        if (!error) {
            error = new ProjectError(projectId: projectId, errorType: ProjectError.ErrorType.SkillNotFound,  error: reportedSkillId, created: new Date(), count: 0)
        }
        error.count += 1
        error.lastSeen = new Date()

        errorRepo.save(error)
    }

    @Transactional
    public void deleteError(String projectId, String errorType, String err) {
        ProjectError.ErrorType type
        try {
            type = ProjectError.ErrorType.valueOf(errorType)
        } catch (IllegalArgumentException illegalArgumentException) {
            log.error("can't find ErrorType enum value for [${errorType}]", illegalArgumentException)
            throw new SkillException("unrecognized errorType [${errorType}]")
        }
        ProjectError error = errorRepo.findByProjectIdAndErrorTypeAndError(projectId, type, err)
        if (error) {
            log.info("deleting error for [${projectId}]-[${err}]")
            errorRepo.delete(error)
        } else {
            log.warn("ProjectError does not exists for [${projectId}]-[${err}")
        }
    }

    @Transactional
    public void deleteAllErrors(String projectId) {
        errorRepo.deleteByProjectId(projectId)
    }

    @Transactional(readOnly = true)
    public TableResult getAllErrorsForProject(String projectId, PageRequest pageRequest) {
        List<skills.controller.result.model.ProjectError> errs = []
        Page<ProjectError> res = errorRepo.findAllByProjectId(projectId, pageRequest);
        if (!res.isEmpty()) {
            res.forEach({
                errs << new skills.controller.result.model.ProjectError(
                        projectId: it.projectId,
                        errorType: it.errorType.toString(),
                        error: it.error,
                        created: it.created,
                        lastSeen: it.lastSeen,
                        count: it.count
                )
            })
        }

        TableResult t = new TableResult(data: errs, count: errs?.size(), totalCount: res.getTotalElements())
        return t
    }

    @Transactional(readOnly = true)
    public long countOfErrorsForProject(String projectId) {
        return errorRepo.countByProjectId(projectId)
    }
}
