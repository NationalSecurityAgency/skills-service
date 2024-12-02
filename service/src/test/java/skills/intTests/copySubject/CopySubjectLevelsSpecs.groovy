/**
 * Copyright 2024 SkillTree
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
package skills.intTests.copySubject


import skills.intTests.copyProject.CopyIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.storage.model.auth.RoleName

import static skills.intTests.utils.SkillsFactory.*

class CopySubjectLevelsSpecs extends CopyIntSpec {

    def "destination project using percent-based system: levels are not copied and defaulted"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [])

        def level6 = [:]
        level6.percent = 95
        level6.name = "TwoEagles"
        level6.iconClass = "fas fa-two-eagles"
        def level7 = [:]
        level7.percent = 98
        level7.name = "FancyCool"
        level7.iconClass = "fas fa-fancy"
        skillsService.addLevel(p1.projectId, p1subj1.subjectId, level6)
        skillsService.addLevel(p1.projectId, p1subj1.subjectId, level7)


        def p2 = createProject(2)
        skillsService.createProject(p2)

        when:
        skillsService.copySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)

        def p1SubjLevels = skillsService.getLevels(p1.projectId, p1subj1.subjectId).sort() { it.level }
        def p2SubjLevels = skillsService.getLevels(p2.projectId, p1subj1.subjectId).sort() { it.level }

        then:
        p1SubjLevels.level == [1, 2, 3, 4, 5, 6, 7]

        p2SubjLevels.level == [1, 2, 3, 4, 5]
        p2SubjLevels.percent == [10, 25, 45, 67, 92]
        p2SubjLevels.pointsFrom == [null, null, null, null, null]
        p2SubjLevels.pointsTo == [null, null, null, null, null]
    }

    def "destination project using point-based system: levels are not copied and defaulted"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(2, 1, 1, 500, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def level6 = [:]
        level6.percent = 95
        level6.name = "TwoEagles"
        level6.iconClass = "fas fa-two-eagles"
        def level7 = [:]
        level7.percent = 98
        level7.name = "FancyCool"
        level7.iconClass = "fas fa-fancy"
        skillsService.addLevel(p1.projectId, p1subj1.subjectId, level6)
        skillsService.addLevel(p1.projectId, p1subj1.subjectId, level7)

        def p2 = createProject(2)
        def p2subj2 = createSubject(2, 2)
        def p2Skills = createSkills(2, 2, 2, 500, 1)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj2, p2Skills)
        String projectPointsSetting = "level.points.enabled"
        skillsService.changeSetting(p2.projectId, projectPointsSetting, [projectId: p2.projectId, setting: projectPointsSetting, value: Boolean.TRUE.toString()])

        when:
        skillsService.copySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)

        def p1SubjLevels = skillsService.getLevels(p1.projectId, p1subj1.subjectId).sort() { it.level }
        def p2SubjLevels = skillsService.getLevels(p2.projectId, p1subj1.subjectId).sort() { it.level }

        then:
        p1SubjLevels.level == [1, 2, 3, 4, 5, 6, 7]

        p2SubjLevels.level == [1, 2, 3, 4, 5]
        p2SubjLevels.percent == [10, 25, 45, 67, 92]
        p2SubjLevels.pointsFrom == [100, 250, 450, 670, 920]
        p2SubjLevels.pointsTo == [250, 450, 670, 920,null]
    }

}
