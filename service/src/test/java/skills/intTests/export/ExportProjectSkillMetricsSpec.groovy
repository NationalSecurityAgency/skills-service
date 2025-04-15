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
package skills.intTests.export

import groovy.time.TimeCategory
import skills.intTests.utils.SkillsService

import static skills.intTests.utils.SkillsFactory.*

class ExportProjectSkillMetricsSpec extends ExportBaseIntSpec {

    def "export project skill metrics"() {
        List<String> users = getRandomUsers(10)
        def proj = createProject()
        def subj = createSubject()
        List<Map> skills = createSkills(9)
        skills.each { it.pointIncrement = 100; it.numPerformToCompletion = 5 }

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        List<Date> days = []

        use(TimeCategory) {
            days = (5..0).collect { int day -> day.days.ago }
            days.eachWithIndex { Date date, int index ->
                users.subList(0, index).each { String user ->
                    skills.subList(0, index).each { skill ->
                        skillsService.addSkill([projectId: proj.projectId, skillId: skill.skillId], user, date)
                    }
                }
            }
        }

        when:
        def excelExport = skillsService.getSkillMetricsExcelExport(proj.projectId)

        then:
        validateExport(excelExport.file, [
                ["For All Dragons Only"],
                ["Skill Name", "Skill ID", "# Users Achieved", "# Users In Progress", "Date Last Reported (UTC)", "Date Last Achieved (UTC)"],
                ["Test Skill 1", "skill1", "1.0", "4.0",  days.last().format("M/d/yy H:mm"), days.last().format("M/d/yy H:mm")],
                ["Test Skill 2", "skill2", "0.0", "5.0",  days.last().format("M/d/yy H:mm"), ""],
                ["Test Skill 3", "skill3", "0.0", "5.0",  days.last().format("M/d/yy H:mm"), ""],
                ["Test Skill 4", "skill4", "0.0", "5.0",  days.last().format("M/d/yy H:mm"), ""],
                ["Test Skill 5", "skill5", "0.0", "5.0",  days.last().format("M/d/yy H:mm"), ""],
                ["Test Skill 6", "skill6", "0.0", "0.0",  "", ""],
                ["Test Skill 7", "skill7", "0.0", "0.0",  "", ""],
                ["Test Skill 8", "skill8", "0.0", "0.0",  "", ""],
                ["Test Skill 9", "skill9", "0.0", "0.0",  "", ""],
                ["For All Dragons Only"],
        ])
    }

    def "export project skill metrics for UC protected project"() {
        def users = getRandomUsers(10)

        SkillsService pristineDragonsUser = createService(users[2])
        rootSkillsService.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])
        rootSkillsService.saveUserTag(rootSkillsService.userName, 'dragons', ['DivineDragon'])

        def proj = createProject()
        proj.enableProtectedUserCommunity = true
        def subj = createSubject()
        List<Map> skills = createSkills(9)
        skills.each { it.pointIncrement = 100; it.numPerformToCompletion = 5 }

        pristineDragonsUser.createProject(proj)
        pristineDragonsUser.createSubject(subj)
        pristineDragonsUser.createSkills(skills)

        List<Date> days = []

        use(TimeCategory) {
            days = (5..0).collect { int day -> day.days.ago }
            days.eachWithIndex { Date date, int index ->
                users.subList(0, index).each { String user ->
                    skills.subList(0, index).each { skill ->
                        pristineDragonsUser.addSkill([projectId: proj.projectId, skillId: skill.skillId], user, date)
                    }
                }
            }
        }

        when:
        def excelExport = pristineDragonsUser.getSkillMetricsExcelExport(proj.projectId)

        then:
        validateExport(excelExport.file, [
                ["For Divine Dragon Only"],
                ["Skill Name", "Skill ID", "# Users Achieved", "# Users In Progress", "Date Last Reported (UTC)", "Date Last Achieved (UTC)"],
                ["Test Skill 1", "skill1", "1.0", "4.0",  days.last().format("M/d/yy H:mm"), days.last().format("M/d/yy H:mm")],
                ["Test Skill 2", "skill2", "0.0", "5.0",  days.last().format("M/d/yy H:mm"), ""],
                ["Test Skill 3", "skill3", "0.0", "5.0",  days.last().format("M/d/yy H:mm"), ""],
                ["Test Skill 4", "skill4", "0.0", "5.0",  days.last().format("M/d/yy H:mm"), ""],
                ["Test Skill 5", "skill5", "0.0", "5.0",  days.last().format("M/d/yy H:mm"), ""],
                ["Test Skill 6", "skill6", "0.0", "0.0",  "", ""],
                ["Test Skill 7", "skill7", "0.0", "0.0",  "", ""],
                ["Test Skill 8", "skill8", "0.0", "0.0",  "", ""],
                ["Test Skill 9", "skill9", "0.0", "0.0",  "", ""],
                ["For Divine Dragon Only"],
        ])
    }

}
