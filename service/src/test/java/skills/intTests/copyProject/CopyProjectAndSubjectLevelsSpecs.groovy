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
package skills.intTests.copyProject

import skills.intTests.utils.DefaultIntSpec

import static skills.intTests.utils.SkillsFactory.*

class CopyProjectAndSubjectLevelsSpecs extends DefaultIntSpec {

    String projectPointsSetting = "level.points.enabled"

    def "project levels were removed and changed"() {
        def p1 = createProject(1)
        skillsService.createProject(p1)

        // edit levels
        skillsService.deleteLevel(p1.projectId)
        skillsService.deleteLevel(p1.projectId)
        skillsService.deleteLevel(p1.projectId)

        def p1Levels = skillsService.getLevels(p1.projectId).sort() { it.level }
        p1Levels[0].percent = 44
        p1Levels[0].iconClass = "first-level-icon"
        p1Levels[0].name = "First"
        p1Levels[1].percent = 77
        p1Levels[1].iconClass = "second-level-icon"
        p1Levels[1].name = "Second"
        skillsService.editLevel(p1.projectId, null, "2", p1Levels[1])
        skillsService.editLevel(p1.projectId, null, "1", p1Levels[0])

        when:
        def projToCopy = createProject(2)
        skillsService.copyProject(p1.projectId, projToCopy)

        def p2Levels = skillsService.getLevels(projToCopy.projectId).sort() { it.level }

        then:
        p1Levels.level == [1, 2]
        p2Levels.level == [1, 2]
        p2Levels.percent == [44, 77]
        p2Levels.pointsFrom == [null, null]
        p2Levels.pointsTo == [null, null]
        p2Levels.iconClass == ["first-level-icon", "second-level-icon"]
        p2Levels.name == ["First", "Second"]
    }

    def "subjects levels were removed and changed"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj2 = createSubject(1, 2)

        skillsService.createProject(p1)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p1subj2)

        // edit levels
        skillsService.deleteLevel(p1.projectId, p1subj1.subjectId)
        skillsService.deleteLevel(p1.projectId, p1subj1.subjectId)
        skillsService.deleteLevel(p1.projectId, p1subj1.subjectId)

        skillsService.deleteLevel(p1.projectId, p1subj2.subjectId)
        skillsService.deleteLevel(p1.projectId, p1subj2.subjectId)

        def p1LevelsSubj1 = skillsService.getLevels(p1.projectId, p1subj1.subjectId).sort() { it.level }
        p1LevelsSubj1[0].percent = 44
        p1LevelsSubj1[0].iconClass = "first-level-icon"
        p1LevelsSubj1[0].name = "First"
        p1LevelsSubj1[1].percent = 77
        p1LevelsSubj1[1].iconClass = "second-level-icon"
        p1LevelsSubj1[1].name = "Second"
        skillsService.editLevel(p1.projectId, p1subj1.subjectId, "2", p1LevelsSubj1[1])
        skillsService.editLevel(p1.projectId, p1subj1.subjectId, "1", p1LevelsSubj1[0])

        when:
        def projToCopy = createProject(2)
        skillsService.copyProject(p1.projectId, projToCopy)

        def p2LevelsSubj1 = skillsService.getLevels(projToCopy.projectId, p1subj1.subjectId).sort() { it.level }
        def p2LevelsSubj2 = skillsService.getLevels(projToCopy.projectId, p1subj2.subjectId).sort() { it.level }

        then:
        p1LevelsSubj1.level == [1, 2]

        p2LevelsSubj1.level == [1, 2]
        p2LevelsSubj1.percent == [44, 77]
        p2LevelsSubj1.pointsFrom == [null, null]
        p2LevelsSubj1.pointsTo == [null, null]
        p2LevelsSubj1.iconClass == ["first-level-icon", "second-level-icon"]
        p2LevelsSubj1.name == ["First", "Second"]

        p2LevelsSubj2.level == [1, 2, 3]
        p2LevelsSubj2.percent == [10, 25, 45]
        p2LevelsSubj2.pointsFrom == [null, null, null]
        p2LevelsSubj2.pointsTo == [null, null, null]
        p2LevelsSubj2.iconClass == ["fas fa-user-ninja", "fas fa-user-ninja", "fas fa-user-ninja"]
        p2LevelsSubj2.name == ["White Belt", "Blue Belt", "Purple Belt"]
    }

    def "project levels were added and changed"() {
        def p1 = createProject(1)
        skillsService.createProject(p1)

        // edit levels
        def level6 = [:]
        level6.percent = 95
        level6.name = "TwoEagles"
        level6.iconClass = "fas fa-two-eagles"
        def level7 = [:]
        level7.percent = 98
        level7.name = "FancyCool"
        level7.iconClass = "fas fa-fancy"
        skillsService.addLevel(p1.projectId, null, level6)
        skillsService.addLevel(p1.projectId, null, level7)

        def p1Levels = skillsService.getLevels(p1.projectId).sort() { it.level }
        p1Levels[0].percent = 12
        p1Levels[0].iconClass = "first-level-icon"
        p1Levels[0].name = "First"
        p1Levels[1].percent = 28
        p1Levels[1].iconClass = "second-level-icon"
        p1Levels[1].name = "Second"
        skillsService.editLevel(p1.projectId, null, "2", p1Levels[1])
        skillsService.editLevel(p1.projectId, null, "1", p1Levels[0])

        when:
        def projToCopy = createProject(2)
        skillsService.copyProject(p1.projectId, projToCopy)

        def p2Levels = skillsService.getLevels(projToCopy.projectId).sort() { it.level }

        then:
        p1Levels.level == [1, 2, 3, 4, 5, 6, 7]
        p2Levels.level == [1, 2, 3, 4, 5, 6, 7]
        p2Levels.percent == [12, 28, 45, 67, 92, 95, 98]
        p2Levels.pointsFrom == [null, null, null, null, null, null, null]
        p2Levels.pointsTo == [null, null, null, null, null, null, null]
        p2Levels.iconClass == ["first-level-icon", "second-level-icon", "fas fa-user-ninja", "fas fa-user-ninja", "fas fa-user-ninja", "fas fa-two-eagles", "fas fa-fancy"]
        p2Levels.name == ["First", "Second", "Purple Belt", "Brown Belt", "Black Belt", "TwoEagles", "FancyCool"]
    }

    def "subject levels were added and changed"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj2 = createSubject(1, 2)

        skillsService.createProject(p1)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p1subj2)

        // edit levels
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

        def p1Levels = skillsService.getLevels(p1.projectId, p1subj1.subjectId).sort() { it.level }
        p1Levels[0].percent = 12
        p1Levels[0].iconClass = "first-level-icon"
        p1Levels[0].name = "First"
        p1Levels[1].percent = 28
        p1Levels[1].iconClass = "second-level-icon"
        p1Levels[1].name = "Second"
        skillsService.editLevel(p1.projectId, p1subj1.subjectId, "2", p1Levels[1])
        skillsService.editLevel(p1.projectId, p1subj1.subjectId, "1", p1Levels[0])

        def subj2_level6 = [:]
        subj2_level6.percent = 97
        subj2_level6.name = "Subj2"
        subj2_level6.iconClass = "fas fa-subj2"
        skillsService.addLevel(p1.projectId, p1subj2.subjectId, subj2_level6)

        when:
        def projToCopy = createProject(2)
        skillsService.copyProject(p1.projectId, projToCopy)

        def p2LevelsSubj1 = skillsService.getLevels(projToCopy.projectId, p1subj1.subjectId).sort() { it.level }
        def p2LevelsSubj2 = skillsService.getLevels(projToCopy.projectId, p1subj2.subjectId).sort() { it.level }

        then:
        p1Levels.level == [1, 2, 3, 4, 5, 6, 7]
        p2LevelsSubj1.level == [1, 2, 3, 4, 5, 6, 7]
        p2LevelsSubj1.percent == [12, 28, 45, 67, 92, 95, 98]
        p2LevelsSubj1.pointsFrom == [null, null, null, null, null, null, null]
        p2LevelsSubj1.pointsTo == [null, null, null, null, null, null, null]
        p2LevelsSubj1.iconClass == ["first-level-icon", "second-level-icon", "fas fa-user-ninja", "fas fa-user-ninja", "fas fa-user-ninja", "fas fa-two-eagles", "fas fa-fancy"]
        p2LevelsSubj1.name == ["First", "Second", "Purple Belt", "Brown Belt", "Black Belt", "TwoEagles", "FancyCool"]

        p2LevelsSubj2.level == [1, 2, 3, 4, 5, 6]
        p2LevelsSubj2.percent == [10, 25, 45, 67, 92, 97]
        p2LevelsSubj2.pointsFrom == [null, null, null, null, null, null]
        p2LevelsSubj2.pointsTo == [null, null, null, null, null, null]
        p2LevelsSubj2.iconClass == ["fas fa-user-ninja", "fas fa-user-ninja", "fas fa-user-ninja", "fas fa-user-ninja", "fas fa-user-ninja", "fas fa-subj2"]
        p2LevelsSubj2.name == ["White Belt", "Blue Belt", "Purple Belt", "Brown Belt", "Black Belt", "Subj2"]
    }

    def "level-based point system: project levels were added and exciting levels modified"() {
        def p1 = createProject(1)
        skillsService.createProject(p1)
        skillsService.createSubject(createSubject(1, 1))
        skillsService.createSkills(createSkills(5, 1, 1, 20, 1))

        skillsService.changeSetting(p1.projectId, projectPointsSetting, [projectId: p1.projectId, setting: projectPointsSetting, value: Boolean.TRUE.toString()])


        // edit levels
        def level6 = [:]
        level6.points = 101
        level6.name = "TwoEagles"
        level6.iconClass = "fas fa-two-eagles"
        def level7 = [:]
        level7.points = 180
        level7.name = "FancyCool"
        level7.iconClass = "fas fa-fancy"
        skillsService.addLevel(p1.projectId, null, level6)
        skillsService.addLevel(p1.projectId, null, level7)

        def p1Levels = skillsService.getLevels(p1.projectId).sort() { it.level }
        p1Levels[0].points = 11
        p1Levels[0].iconClass = "first-level-icon"
        p1Levels[0].name = "First"
        p1Levels[1].points = 26
        p1Levels[1].iconClass = "second-level-icon"
        p1Levels[1].name = "Second"
        skillsService.editLevel(p1.projectId, null, "2", p1Levels[1])
        skillsService.editLevel(p1.projectId, null, "1", p1Levels[0])

        when:
        def projToCopy = createProject(2)
        skillsService.copyProject(p1.projectId, projToCopy)

        def p2Levels = skillsService.getLevels(projToCopy.projectId).sort() { it.level }

        then:
        p2Levels.level == [1, 2, 3, 4, 5, 6, 7]
        p2Levels.percent == [10, 25, 45, 67, 92, null, null]
        p2Levels.pointsFrom == [10, 25, 45, 67, 92, 101, 180]
        p2Levels.pointsTo == [25, 45, 67, 92, 101, 180, null]
        p2Levels.iconClass == ["first-level-icon", "second-level-icon", "fas fa-user-ninja", "fas fa-user-ninja", "fas fa-user-ninja", "fas fa-two-eagles", "fas fa-fancy"]
        p2Levels.name == ["First", "Second", "Purple Belt", "Brown Belt", "Black Belt", "TwoEagles", "FancyCool"]
        p2Levels.achievable == [true, true, true, true, true, false, false]
    }

    def "level-based point system: subject levels were added and exciting levels modified"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj2 = createSubject(1, 2)

        skillsService.createProject(p1)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p1subj2)

        skillsService.createSkills(createSkills(5, 1, 1, 20, 1))
        skillsService.createSkills(createSkills(5, 1, 2, 20, 1))

        skillsService.changeSetting(p1.projectId, projectPointsSetting, [projectId: p1.projectId, setting: projectPointsSetting, value: Boolean.TRUE.toString()])

        def p1Levels = skillsService.getLevels(p1.projectId, p1subj1.subjectId).sort() { it.level }

        // edit levels
        def level6 = [:]
        level6.points = 101
        level6.name = "TwoEagles"
        level6.iconClass = "fas fa-two-eagles"
        def level7 = [:]
        level7.points = 180
        level7.name = "FancyCool"
        level7.iconClass = "fas fa-fancy"
        skillsService.addLevel(p1.projectId, p1subj1.subjectId, level6)
        skillsService.addLevel(p1.projectId, p1subj1.subjectId, level7)

        def subj2_level6 = [:]
        subj2_level6.points = 256
        subj2_level6.name = "2ndsubj"
        subj2_level6.iconClass = "fas fa-two-subj2"
        skillsService.addLevel(p1.projectId, p1subj2.subjectId, subj2_level6)

        p1Levels[0].points = 11
        p1Levels[0].iconClass = "first-level-icon"
        p1Levels[0].name = "First"
        p1Levels[1].points = 26
        p1Levels[1].iconClass = "second-level-icon"
        p1Levels[1].name = "Second"
        skillsService.editLevel(p1.projectId, p1subj1.subjectId, "2", p1Levels[1])
        skillsService.editLevel(p1.projectId, p1subj1.subjectId, "1", p1Levels[0])

        when:
        def projToCopy = createProject(2)
        skillsService.copyProject(p1.projectId, projToCopy)

        def p2LevelsSubj1 = skillsService.getLevels(projToCopy.projectId, p1subj1.subjectId).sort() { it.level }
        def p2LevelsSubj2 = skillsService.getLevels(projToCopy.projectId, p1subj2.subjectId).sort() { it.level }

        then:
        p2LevelsSubj1.level == [1, 2, 3, 4, 5, 6, 7]
        p2LevelsSubj1.percent == [10, 25, 45, 67, 92, null, null]
        p2LevelsSubj1.pointsFrom == [10, 25, 45, 67, 92, 101, 180]
        p2LevelsSubj1.pointsTo == [25, 45, 67, 92, 101, 180, null]
        p2LevelsSubj1.iconClass == ["first-level-icon", "second-level-icon", "fas fa-user-ninja", "fas fa-user-ninja", "fas fa-user-ninja", "fas fa-two-eagles", "fas fa-fancy"]
        p2LevelsSubj1.name == ["First", "Second", "Purple Belt", "Brown Belt", "Black Belt", "TwoEagles", "FancyCool"]
        p2LevelsSubj1.achievable == [true, true, true, true, true, false, false]

        p2LevelsSubj2.level == [1, 2, 3, 4, 5, 6]
        p2LevelsSubj2.percent == [10, 25, 45, 67, 92, null,]
        p2LevelsSubj2.pointsFrom == [10, 25, 45, 67, 92, 256]
        p2LevelsSubj2.pointsTo == [25, 45, 67, 92, 256, null]
        p2LevelsSubj2.iconClass == ["fas fa-user-ninja", "fas fa-user-ninja", "fas fa-user-ninja", "fas fa-user-ninja", "fas fa-user-ninja", "fas fa-two-subj2"]
        p2LevelsSubj2.name == ["White Belt", "Blue Belt", "Purple Belt", "Brown Belt", "Black Belt", "2ndsubj"]
        p2LevelsSubj2.achievable == [true, true, true, true, true, false]
    }

    def "level-based point system: project levels were removed and exciting levels modified"() {
        def p1 = createProject(1)
        skillsService.createProject(p1)
        skillsService.createSubject(createSubject(1, 1))
        skillsService.createSkills(createSkills(5, 1, 1, 20, 1))

        skillsService.changeSetting(p1.projectId, projectPointsSetting, [projectId: p1.projectId, setting: projectPointsSetting, value: Boolean.TRUE.toString()])

        def p1Levels = skillsService.getLevels(p1.projectId).sort() { it.level }

        // edit levels
        p1Levels[0].points = 11
        p1Levels[0].iconClass = "first-level-icon"
        p1Levels[0].name = "First"
        p1Levels[1].points = 26
        p1Levels[1].iconClass = "second-level-icon"
        p1Levels[1].name = "Second"
        skillsService.editLevel(p1.projectId, null, "2", p1Levels[1])
        skillsService.editLevel(p1.projectId, null, "1", p1Levels[0])
        skillsService.deleteLevel(p1.projectId)

        when:
        def projToCopy = createProject(2)
        skillsService.copyProject(p1.projectId, projToCopy)

        def p2Levels = skillsService.getLevels(projToCopy.projectId).sort() { it.level }

        then:
        p2Levels.level == [1, 2, 3, 4]
        p2Levels.percent == [10, 25, 45, 67]
        p2Levels.pointsFrom == [10, 25, 45, 67]
        p2Levels.pointsTo == [25, 45, 67, null]
        p2Levels.iconClass == ["first-level-icon", "second-level-icon", "fas fa-user-ninja", "fas fa-user-ninja"]
        p2Levels.name == ["First", "Second", "Purple Belt", "Brown Belt"]
        p2Levels.achievable == [true, true, true, true]
    }

    def "level-based point system: subject levels were removed and exciting levels modified"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj2 = createSubject(1, 2)

        skillsService.createProject(p1)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p1subj2)

        skillsService.createSkills(createSkills(5, 1, 1, 20, 1))
        skillsService.createSkills(createSkills(5, 1, 2, 20, 1))

        skillsService.changeSetting(p1.projectId, projectPointsSetting, [projectId: p1.projectId, setting: projectPointsSetting, value: Boolean.TRUE.toString()])

        def p1Levels = skillsService.getLevels(p1.projectId, p1subj1.subjectId).sort() { it.level }

        // edit levels
        p1Levels[0].points = 11
        p1Levels[0].iconClass = "first-level-icon"
        p1Levels[0].name = "First"
        p1Levels[1].points = 26
        p1Levels[1].iconClass = "second-level-icon"
        p1Levels[1].name = "Second"
        skillsService.editLevel(p1.projectId, p1subj1.subjectId, "2", p1Levels[1])
        skillsService.editLevel(p1.projectId, p1subj1.subjectId, "1", p1Levels[0])

        skillsService.deleteLevel(p1.projectId, p1subj1.subjectId)
        skillsService.deleteLevel(p1.projectId, p1subj1.subjectId)
        skillsService.deleteLevel(p1.projectId, p1subj2.subjectId)

        when:
        def projToCopy = createProject(2)
        skillsService.copyProject(p1.projectId, projToCopy)

        def p2LevelsSubj1 = skillsService.getLevels(projToCopy.projectId, p1subj1.subjectId).sort() { it.level }
        def p2LevelsSubj2 = skillsService.getLevels(projToCopy.projectId, p1subj2.subjectId).sort() { it.level }

        then:
        p2LevelsSubj1.level == [1, 2, 3]
        p2LevelsSubj1.percent == [10, 25, 45]
        p2LevelsSubj1.pointsFrom == [10, 25, 45]
        p2LevelsSubj1.pointsTo == [25, 45, null]
        p2LevelsSubj1.iconClass == ["first-level-icon", "second-level-icon", "fas fa-user-ninja"]
        p2LevelsSubj1.name == ["First", "Second", "Purple Belt"]
        p2LevelsSubj1.achievable == [true, true, true]

        p2LevelsSubj2.level == [1, 2, 3, 4]
        p2LevelsSubj2.percent == [10, 25, 45, 67]
        p2LevelsSubj2.pointsFrom == [10, 25, 45, 67]
        p2LevelsSubj2.pointsTo == [25, 45, 67, null]
        p2LevelsSubj2.iconClass == ["fas fa-user-ninja", "fas fa-user-ninja", "fas fa-user-ninja", "fas fa-user-ninja"]
        p2LevelsSubj2.name == ["White Belt", "Blue Belt", "Purple Belt", "Brown Belt"]
        p2LevelsSubj2.achievable == [true, true, true, true]
    }

    def "project exist as % based but then switched to the level-based and more skills are created"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj1Skills = createSkills(2, 1, 1, 10, 5)
        def p1subj2 = createSubject(1, 2)

        skillsService.createProject(p1)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p1subj2)
        skillsService.createSkills(p1subj1Skills)

        skillsService.changeSetting(p1.projectId, projectPointsSetting, [projectId: p1.projectId, setting: projectPointsSetting, value: Boolean.TRUE.toString()])

        def p1subj2Skills = createSkills(2, 1, 2, 100, 5)
        skillsService.createSkills(p1subj2Skills)

        when:
        def projToCopy = createProject(2)
        skillsService.copyProject(p1.projectId, projToCopy)

        def p2LevelsSubj1 = skillsService.getLevels(projToCopy.projectId, p1subj1.subjectId).sort() { it.level }
        def p2LevelsSubj2 = skillsService.getLevels(projToCopy.projectId, p1subj2.subjectId).sort() { it.level }
        def projLevels = skillsService.getLevels(projToCopy.projectId).sort() { it.level }

        then:
        p2LevelsSubj1.level == [1, 2, 3, 4, 5]
        p2LevelsSubj1.percent == [10, 25, 45, 67, 92]
        p2LevelsSubj1.pointsFrom == [10, 25, 45, 67, 92]
        p2LevelsSubj1.pointsTo == [25, 45, 67, 92, null]
        p2LevelsSubj1.iconClass == ["fas fa-user-ninja", "fas fa-user-ninja", "fas fa-user-ninja", "fas fa-user-ninja", "fas fa-user-ninja"]
        p2LevelsSubj1.name == ["White Belt", "Blue Belt", "Purple Belt", "Brown Belt", "Black Belt"]
        p2LevelsSubj1.achievable == [true, true, true, true, true]

        p2LevelsSubj2.level == [1, 2, 3, 4, 5]
        p2LevelsSubj2.percent == [10, 25, 45, 67, 92]
        p2LevelsSubj2.pointsFrom == [100, 250, 450, 670, 920]
        p2LevelsSubj2.pointsTo == [250, 450, 670, 920, null]
        p2LevelsSubj2.iconClass == ["fas fa-user-ninja", "fas fa-user-ninja", "fas fa-user-ninja", "fas fa-user-ninja", "fas fa-user-ninja"]
        p2LevelsSubj2.name == ["White Belt", "Blue Belt", "Purple Belt", "Brown Belt", "Black Belt"]
        p2LevelsSubj2.achievable == [true, true, true, true, true]

        projLevels.level == [1, 2, 3, 4, 5]
        projLevels.percent == [10, 25, 45, 67, 92]
        projLevels.pointsFrom == [10, 25, 45, 67, 92]
        projLevels.pointsTo == [25, 45, 67, 92, null]
        projLevels.iconClass == ["fas fa-user-ninja", "fas fa-user-ninja", "fas fa-user-ninja", "fas fa-user-ninja", "fas fa-user-ninja"]
        projLevels.name == ["White Belt", "Blue Belt", "Purple Belt", "Brown Belt", "Black Belt"]
        projLevels.achievable == [true, true, true, true, true]
    }
}

