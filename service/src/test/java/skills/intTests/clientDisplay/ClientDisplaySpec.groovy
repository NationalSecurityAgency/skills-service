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
package skills.intTests.clientDisplay

import org.springframework.http.HttpStatus
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory

class ClientDisplaySpec extends DefaultIntSpec {

    def "summary for an empty subject"() {
        def proj1 = SkillsFactory.createProject()
        def subj1 = SkillsFactory.createSubject()

        skillsService.createProject(proj1)
        skillsService.createSubject(subj1)
        when:
        def res = skillsService.getSkillSummary("user1", proj1.projectId)
        then:
        res
        res.subjects.size() == 1
        res.subjects.first().subjectId == subj1.subjectId
        res.subjects.first().totalPoints == 0
    }

    def "attempt to get proj summary for project that does not exist"() {
        when:
        skillsService.getSkillSummary("user1", "notaproject")
        then:
        SkillsClientException exception = thrown(SkillsClientException)
        exception.httpStatus == HttpStatus.FORBIDDEN
    }

    def "attempt to get subject summary for subject that does not exist"() {
        def proj1 = SkillsFactory.createProject()

        skillsService.createProject(proj1)
        when:
        skillsService.getSkillSummary("user1", proj1.projectId, "notasubject")
        then:
        SkillsClientException exception = thrown(SkillsClientException)
        exception.httpStatus == HttpStatus.BAD_REQUEST
    }
}
