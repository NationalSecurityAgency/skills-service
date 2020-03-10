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
        skillsService.moveSubjectDown(subjects.first())
        def afterMove = skillsService.getSubjects(proj.projectId)
        then:
        beforeMove.collect({it.subjectId}) == ["TestSubject1", "TestSubject2", "TestSubject3", "TestSubject4", "TestSubject5"]
        afterMove.collect({it.subjectId}) == ["TestSubject2", "TestSubject1", "TestSubject3", "TestSubject4", "TestSubject5"]
    }

    def "should not be able to move down the last subject"() {
        when:
        def beforeMove = skillsService.getSubjects(proj.projectId)
        skillsService.moveSubjectDown(subjects.last())
        then:
        thrown(SkillsClientException)
        beforeMove.collect({it.subjectId}) == ["TestSubject1", "TestSubject2", "TestSubject3", "TestSubject4", "TestSubject5"]
        skillsService.getSubjects(proj.projectId).collect({it.subjectId}) == ["TestSubject1", "TestSubject2", "TestSubject3", "TestSubject4", "TestSubject5"]
    }

    def "move subject up"() {
        when:
        def beforeMove = skillsService.getSubjects(proj.projectId)
        skillsService.moveSubjectUp(subjects.get(1))
        def afterMove = skillsService.getSubjects(proj.projectId)
        then:
        beforeMove.collect({it.subjectId}) == ["TestSubject1", "TestSubject2", "TestSubject3", "TestSubject4", "TestSubject5"]
        afterMove.collect({it.subjectId}) == ["TestSubject2", "TestSubject1", "TestSubject3", "TestSubject4", "TestSubject5"]
    }

    def "should not be able to move the first subject up"() {
        when:
        def beforeMove = skillsService.getSubjects(proj.projectId)
        skillsService.moveSubjectUp(subjects.first())
        then:
        thrown(SkillsClientException)
        beforeMove.collect({it.subjectId}) == ["TestSubject1", "TestSubject2", "TestSubject3", "TestSubject4", "TestSubject5"]
        skillsService.getSubjects(proj.projectId).collect({it.subjectId}) == ["TestSubject1", "TestSubject2", "TestSubject3", "TestSubject4", "TestSubject5"]
    }
}
