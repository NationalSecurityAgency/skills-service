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
package skills.intTests.adminDisplayOrder

import org.springframework.http.HttpStatus
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory

class SubjectsOrderSpecs extends DefaultIntSpec {

    def proj
    List subjects
    def setup() {
        proj = SkillsFactory.createProject()
        int numSubjects = 5

        skillsService.createProject(proj)
        subjects = (1..numSubjects).collect {
            def subject = SkillsFactory.createSubject(1, it)
            skillsService.createSubject(subject)
            return subject
        }
    }

    def "move subject down"() {
        when:
        def beforeMove = skillsService.getSubjects(proj.projectId)
        skillsService.changeSubjectDisplayOrder(subjects.first(), 1)
        def afterMove = skillsService.getSubjects(proj.projectId)
        then:
        beforeMove.collect({it.subjectId}) == ["TestSubject1", "TestSubject2", "TestSubject3", "TestSubject4", "TestSubject5"]
        afterMove.collect({it.subjectId}) == ["TestSubject2", "TestSubject1", "TestSubject3", "TestSubject4", "TestSubject5"]
    }

    def "move subject up"() {
        when:
        def beforeMove = skillsService.getSubjects(proj.projectId)
        skillsService.changeSubjectDisplayOrder(subjects.get(1), 0)
        def afterMove = skillsService.getSubjects(proj.projectId)
        then:
        beforeMove.collect({it.subjectId}) == ["TestSubject1", "TestSubject2", "TestSubject3", "TestSubject4", "TestSubject5"]
        afterMove.collect({it.subjectId}) == ["TestSubject2", "TestSubject1", "TestSubject3", "TestSubject4", "TestSubject5"]
    }

    def "sequence of subject display order operations"() {
        when:
        def beforeMove = skillsService.getSubjects(proj.projectId)
        skillsService.changeSubjectDisplayOrder(subjects.get(0), 3)
        def move1 = skillsService.getSubjects(proj.projectId)

        skillsService.changeSubjectDisplayOrder(subjects.get(4), 0)
        def move2 = skillsService.getSubjects(proj.projectId)

        skillsService.changeSubjectDisplayOrder(subjects.get(1), 4)
        def move3 = skillsService.getSubjects(proj.projectId)

        skillsService.changeSubjectDisplayOrder(subjects.get(2), 2)
        def move4 = skillsService.getSubjects(proj.projectId)
        then:
        beforeMove.collect({it.subjectId}) == ["TestSubject1", "TestSubject2", "TestSubject3", "TestSubject4", "TestSubject5"]
        move1.collect({it.subjectId}) == ["TestSubject2", "TestSubject3", "TestSubject4", "TestSubject1", "TestSubject5"]
        move2.collect({it.subjectId}) == ["TestSubject5", "TestSubject2", "TestSubject3", "TestSubject4", "TestSubject1"]
        move3.collect({it.subjectId}) == ["TestSubject5", "TestSubject3", "TestSubject4", "TestSubject1", "TestSubject2"]
        move4.collect({it.subjectId}) == ["TestSubject5", "TestSubject4", "TestSubject3", "TestSubject1", "TestSubject2"]
    }

    def "move subject to out of the max bound - should be placed last"() {
        when:
        def beforeMove = skillsService.getSubjects(proj.projectId)
        skillsService.changeSubjectDisplayOrder(subjects.get(0), 10)
        def afterMove = skillsService.getSubjects(proj.projectId)
        then:
        beforeMove.collect({it.subjectId}) == ["TestSubject1", "TestSubject2", "TestSubject3", "TestSubject4", "TestSubject5"]
        afterMove.collect({it.subjectId}) == ["TestSubject2", "TestSubject3", "TestSubject4", "TestSubject5", "TestSubject1"]
    }

    def "new display index must be >=0 "() {
        when:
        skillsService.changeSubjectDisplayOrder(subjects.get(2), 0)
        def afterMove = skillsService.getSubjects(proj.projectId)
        skillsService.changeSubjectDisplayOrder(subjects.get(2), -1)
        then:
        SkillsClientException exception = thrown()
        exception.httpStatus == HttpStatus.BAD_REQUEST
        exception.message.contains('[newDisplayOrderIndex] param must be >=0 but received [-1]')

        afterMove.collect({it.subjectId}) == ["TestSubject3", "TestSubject1", "TestSubject2", "TestSubject4", "TestSubject5"]

    }

}
