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
package skills.intTests

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory

class EntityExistsTests extends DefaultIntSpec {

    def "does subject name exist"() {
        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        subj.name = "Test Subject 1"

        when:
        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        then:
        skillsService.doesSubjectNameExist(proj.projectId, subj.name)
        !skillsService.doesSubjectNameExist(proj.projectId, proj.name)
    }

    def "does badge name exist"() {
        def proj = SkillsFactory.createProject(1)
        def badge = SkillsFactory.createBadge(1, 1)

        when:
        skillsService.createProject(proj)
        skillsService.createBadge(badge)
        then:
        skillsService.doesBadgeNameExist(proj.projectId, badge.name)
        !skillsService.doesBadgeNameExist(proj.projectId, proj.name)
    }

    def "does skill name exist"() {
        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)

        when:
        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(proj1_skills)
        then:
        skillsService.doesSkillNameExist(proj.projectId, proj1_skills.get(0).name)
        !skillsService.doesSkillNameExist(proj.projectId, proj.projectId)
        skillsService.doesSkillNameExist(proj.projectId, proj1_skills.get(1).name)
        skillsService.doesSkillNameExist(proj.projectId, proj1_skills.get(2).name)
    }

    def "does entity id exist"() {
        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)

        when:
        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        then:
        skillsService.doesEntityExist(proj.projectId, subj.subjectId)
        !skillsService.doesEntityExist(proj.projectId, proj.projectId)
    }
}
