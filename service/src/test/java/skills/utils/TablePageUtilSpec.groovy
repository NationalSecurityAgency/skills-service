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
package skills.utils

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.exceptions.SkillQuizException
import spock.lang.Specification

class TablePageUtilSpec extends Specification {

    def "pages are 1-based: page 1 maps to the first zero-based page index"() {
        when:
        PageRequest withProject = TablePageUtil.createPagingRequestWithValidation("proj1", 10, 1, "name", true)
        PageRequest withoutProject = TablePageUtil.createPagingRequestWithValidation(10, 3, "name", false)
        PageRequest quiz = TablePageUtil.validateAndConstructQuizPageRequest(25, 2, "name", true)

        then:
        withProject.pageNumber == 0
        withProject.pageSize == 10
        withProject.sort.getOrderFor("name").direction == Sort.Direction.ASC
        withoutProject.pageNumber == 2
        withoutProject.sort.getOrderFor("name").direction == Sort.Direction.DESC
        quiz.pageNumber == 1
        quiz.pageSize == 25
    }

    def "page below 1 is rejected as a bad request instead of reaching PageRequest - project: #page"() {
        when:
        TablePageUtil.createPagingRequestWithValidation("proj1", 10, page, "name", true)

        then:
        SkillException e = thrown()
        e.errorCode == ErrorCode.BadParam
        e.projectId == "proj1"
        e.message.contains("provided=[${page}]")

        where:
        page << [0, -1]
    }

    def "page below 1 is rejected as a bad request instead of reaching PageRequest - no project: #page"() {
        when:
        TablePageUtil.createPagingRequestWithValidation(10, page, "name", true)

        then:
        SkillException e = thrown()
        e.errorCode == ErrorCode.BadParam

        where:
        page << [0, -1]
    }

    def "limit below 1 is rejected as a bad request instead of reaching PageRequest: #limit"() {
        when:
        TablePageUtil.createPagingRequestWithValidation("proj1", limit, 1, "name", true)

        then:
        SkillException e = thrown()
        e.errorCode == ErrorCode.BadParam
        e.message.contains("provided=[${limit}]")

        when:
        TablePageUtil.createPagingRequestWithValidation(limit, 1, "name", true)

        then:
        SkillException e2 = thrown()
        e2.errorCode == ErrorCode.BadParam

        where:
        limit << [0, -5]
    }

    def "quiz paging rejects page 0 as a bad request"() {
        when:
        TablePageUtil.validateAndConstructQuizPageRequest(10, 0, "name", true)

        then:
        SkillQuizException e = thrown()
        e.errorCode == ErrorCode.BadParam
        e.message == "[page] must be >= 1"
    }

    def "quiz paging error messages report the configured limits"() {
        when:
        TablePageUtil.validateAndConstructQuizPageRequest(51, 1, "name", true, 50, 100)

        then:
        SkillQuizException limitErr = thrown()
        limitErr.message == "[limit] must be <= 50"

        when:
        TablePageUtil.validateAndConstructQuizPageRequest(10, 100, "name", true, 50, 100)

        then:
        SkillQuizException pageErr = thrown()
        pageErr.message == "[page] must be < 100"
    }
}
