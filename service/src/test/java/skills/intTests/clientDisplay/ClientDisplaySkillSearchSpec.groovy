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

import groovy.json.JsonOutput
import org.apache.commons.lang3.StringUtils
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.storage.model.SkillDef

import static skills.intTests.utils.SkillsFactory.createProject
import static skills.intTests.utils.SkillsFactory.createSkills
import static skills.intTests.utils.SkillsFactory.createSubject

class ClientDisplaySkillSearchSpec extends DefaultIntSpec {


    def setup(){

    }

    def "get skills"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(10, 1, 1, 100, 2)

        def p1subj2 = createSubject(1, 2)
        def p1SkillsSubj2 = createSkills(10, 1, 2, 100, 2)

        (2..6).each {
            p1Skills[it].name = StringUtils.leftPad((it-1).toString(), 3, "0")
            p1Skills[it].pointIncrement = it
        }

        (3..7).each {
            p1SkillsSubj2[it].name = StringUtils.leftPad((it+3).toString(), 3, "0")
            p1SkillsSubj2[it].pointIncrement = it
        }
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.createProjectAndSubjectAndSkills(null, p1subj2, p1SkillsSubj2)

        skillsService.addSkill(p1Skills[3], skillsService.userName, new Date() - 1)
        skillsService.addSkill(p1Skills[3], skillsService.userName, new Date())
        skillsService.addSkill(p1Skills[4], skillsService.userName, new Date())

        String otherUser = getRandomUsers(1)[0]
        skillsService.addSkill(p1Skills[5], otherUser, new Date()-1)
        skillsService.addSkill(p1Skills[5], otherUser, new Date())

        when:
        def skills = skillsService.getApiSkills(p1.projectId)
        def skillsWithFilter = skillsService.getApiSkills(p1.projectId, "10", )
        def skillsWithFilterAndZeroResults = skillsService.getApiSkills(p1.projectId, "1039933", )
        then:
        skills.totalCount == 20
        skills.count == 20
        skills.data.size() == 10
        skills.data.skillName == ["001", "002", "003", "004", "005", "006", "007", "008", "009", "010"]
        skills.data.pointIncrement == [2, 3, 4, 5, 6, 3, 4, 5, 6, 7]
        skills.data.totalPoints == [4, 6, 8, 10, 12, 6, 8, 10, 12, 14]
        skills.data.userCurrentPoints == [0, 6, 4, 0, 0, 0, 0, 0, 0, 0]
        skills.data.userAchieved == [false, true, false, false, false, false, false, false, false, false]
        skills.data.skillId == [
                p1Skills[2].skillId,
                p1Skills[3].skillId,
                p1Skills[4].skillId,
                p1Skills[5].skillId,
                p1Skills[6].skillId,
                p1SkillsSubj2[3].skillId,
                p1SkillsSubj2[4].skillId,
                p1SkillsSubj2[5].skillId,
                p1SkillsSubj2[6].skillId,
                p1SkillsSubj2[7].skillId,
        ]
        skills.data.subjectId == [(1..5).collect { p1subj1.subjectId }, (1..5).collect { p1subj2.subjectId }].flatten()
        skills.data.subjectName == [(1..5).collect { p1subj1.name }, (1..5).collect { p1subj2.name }].flatten()

        skillsWithFilter.totalCount == 20
        skillsWithFilter.count == 3
        skillsWithFilter.data.skillName == ["010", "Test Skill 10", "Test Skill 10 Subject2"]

        skillsWithFilterAndZeroResults.totalCount == 20
        skillsWithFilterAndZeroResults.count == 0
        !skillsWithFilterAndZeroResults.data
    }

    def "empty projects"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [])

        when:
        def skills = skillsService.getApiSkills(p1.projectId)
        then:
        skills.totalCount == 0
        skills.count == 0
        !skills.data
    }

}
