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
package skills.storage.repos

import org.springframework.data.repository.CrudRepository
import org.springframework.lang.Nullable
import skills.storage.ProjectError
import java.util.stream.Stream

interface ProjectErrorRepo extends CrudRepository<ProjectError, Long> {

    @Nullable
    Stream<ProjectError> findAllByProjectId(String projectId)

    long countByProjectId(String projectId)

    void deleteByProjectId(String projectId)

    @Nullable
    ProjectError findByProjectIdAndReportedSkillId(String projectId, String reportedSkillId)

}
