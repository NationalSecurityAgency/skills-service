/**
 * Copyright 2020 SkillTree
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
package skills.utils

import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.JpaSort
import skills.controller.exceptions.SkillsValidator

import static org.springframework.data.domain.Sort.Direction.ASC
import static org.springframework.data.domain.Sort.Direction.DESC

class TablePageUtil {
    static PageRequest createPagingRequestWithValidation(String projectId, int limit, int page, String orderBy, Boolean ascending, Boolean useUnsafeSort=false) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isTrue(limit <= 200, "Cannot ask for more than 200 items, provided=[${limit}]", projectId)
        SkillsValidator.isTrue(page >= 0, "Cannot provide negative page. provided =[${page}]", projectId)
        PageRequest pageRequest
        if (useUnsafeSort) {
            pageRequest = PageRequest.of(page - 1, limit, JpaSort.unsafe(ascending ? ASC : DESC, "(${orderBy})"))
        } else {
            pageRequest = PageRequest.of(page - 1, limit, ascending ? ASC : DESC, orderBy)
        }

        return pageRequest
    }
}
