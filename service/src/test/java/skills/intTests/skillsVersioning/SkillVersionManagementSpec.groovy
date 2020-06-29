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
package skills.intTests.skillsVersioning

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory

class SkillVersionManagementSpec extends DefaultIntSpec {

    def "/projects/<projectid>/versions should return unique versions for only the desired projectId"() {
        skillsService.createProject(SkillsFactory.createProject(1))
        skillsService.createProject(SkillsFactory.createProject(2))

        skillsService.createSubject(SkillsFactory.createSubject(1))
        skillsService.createSubject(SkillsFactory.createSubject(2))

        List<Map> skills = SkillsFactory.createSkillsWithDifferentVersions([0, 0, 0, 0, 1, 2], 1)
        List<Map> skills2 = SkillsFactory.createSkillsWithDifferentVersions([0, 1, 2, 3, 4, 5], 2)

        skillsService.createSkills(skills)
        skillsService.createSkills(skills2)

        when:
        def versions = skillsService.listVersions(SkillsFactory.getDefaultProjId(1))
        def versions1 = skillsService.listVersions(SkillsFactory.getDefaultProjId(2))

        then:
        versions == [0, 1, 2] // Versions for project 1 also sort ascending
        versions1 == [0 ,1, 2, 3, 4, 5]
    }

    def "empty list is returned if no projectId exists"() {
        when:
        def versions = skillsService.listVersions(SkillsFactory.getDefaultProjId(1))

        then:
        versions != null
        versions == []
    }

    def 'assigning dependent skills validates versions of the skills (dependency version must be less than or equal to the skill version)'(){
        List<Map> skills = SkillsFactory.createSkillsWithDifferentVersions([0, 0, 1])

        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkill(skills.get(0))
        skillsService.createSkill(skills.get(1))
        skillsService.createSkill(skills.get(2))


        when:
        def result1 = skillsService.assignDependency([projectId: SkillsFactory.defaultProjId, skillId: skills.get(1).skillId, dependentSkillId: skills.get(0).skillId])
        def result2 = skillsService.assignDependency([projectId: SkillsFactory.defaultProjId, skillId: skills.get(1).skillId, dependentSkillId: skills.get(2).skillId])

        then:
        result1.success
        !result2.success
        result2.body.explanation == "Not allowed to depend on skill with a later version. Skill [ID:skill2, version 0] can not depend on [ID:skill3, version 1]"
    }

    def 'correctly find max skill version for a project'() {
        setup:
        skillsService.createProject(SkillsFactory.createProject(1)).body
        skillsService.createSubject(SkillsFactory.createSubject(1, 1))
        skillsService.createSubject(SkillsFactory.createSubject(1, 2))

        when:
        skillsService.createSkill(SkillsFactory.createSkill(1, 1, 1, 0))
        def result1 = skillsService.findLatestSkillVersion(SkillsFactory.getDefaultProjId(1))
        skillsService.createSkill(SkillsFactory.createSkill(1, 2, 4, 1))
        def result2 = skillsService.findLatestSkillVersion(SkillsFactory.getDefaultProjId(1))
        skillsService.createSkill(SkillsFactory.createSkill(1, 1, 2, 2))
        def result3 = skillsService.findLatestSkillVersion(SkillsFactory.getDefaultProjId(1))
        skillsService.createSkill(SkillsFactory.createSkill(1, 2, 3, 0))
        def result4 = skillsService.findLatestSkillVersion(SkillsFactory.getDefaultProjId(1))

        then:
        result1 == 0
        result2 == 1
        result3 == 2
        result4 == 2
    }

    def "skill version must be <= 'max skill version' + 1"() {
        setup:
        skillsService.createProject(SkillsFactory.createProject(1)).body
        skillsService.createSubject(SkillsFactory.createSubject(1, 1))
        skillsService.createSkill(SkillsFactory.createSkill(1, 1, 1, 1))

        when:
        skillsService.createSkill(SkillsFactory.createSkill(1, 1, 2, 3))
        then:
        SkillsClientException e = thrown()
        e.message.contains("Latest skill version is [1]; max supported version is latest+1 but provided [3] version")
    }


}
