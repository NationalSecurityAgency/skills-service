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
package skills.intTests.catalog

import skills.intTests.utils.DefaultIntSpec

import static skills.intTests.utils.SkillsFactory.*

class CatalogImportedSkillUserStatsSpecs extends DefaultIntSpec {

    def "report skill event on exported skill, should be reflected in all copies"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        // project 1
        def skill = createSkill(1, 1, 1, 0, 5, 0, 250)
        def skill2 = createSkill(1, 1, 2, 0, 5, 0, 250)
        def skill3 = createSkill(1, 1, 3, 0, 5, 0, 250)
        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)

        // project 2
        def skill4 = createSkill(2, 1, 4, 0, 5, 0, 250)
        def skill5 = createSkill(2, 1, 5, 0, 5, 0, 250)
        def skill6 = createSkill(2, 1, 6, 0, 5, 0, 250)
        skillsService.createSkill(skill4)
        skillsService.createSkill(skill5)
        skillsService.createSkill(skill6)
        skillsService.exportSkillToCatalog(project2.projectId, skill4.skillId)
        skillsService.exportSkillToCatalog(project2.projectId, skill5.skillId)
        skillsService.exportSkillToCatalog(project2.projectId, skill6.skillId)

        // project 3
        def skill7 = createSkill(3, 1, 7, 0, 5, 0, 250)
        def skill8 = createSkill(3, 1, 8, 0, 5, 0, 250)
        def skill9 = createSkill(3, 1, 9, 0, 5, 0, 250)
        skillsService.createSkill(skill7)
        skillsService.createSkill(skill8)
        skillsService.createSkill(skill9)
        skillsService.exportSkillToCatalog(project3.projectId, skill7.skillId)
        skillsService.exportSkillToCatalog(project3.projectId, skill8.skillId)
        skillsService.exportSkillToCatalog(project3.projectId, skill9.skillId)


        // 2nd project import
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project3.projectId, skill7.skillId)
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project3.projectId, skill8.skillId)
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project3.projectId, skill9.skillId)

        // 3rd project import
        skillsService.importSkillFromCatalog(project3.projectId, p3subj1.subjectId, project1.projectId, skill2.skillId)

        def randomUsers = getRandomUsers(3)
        def user = randomUsers[0]
        def user2 = randomUsers[1]
        def user3 = randomUsers[2]

        when:
        // user1: just imported skill
        skillsService.addSkill([projectId: project1.projectId, skillId: skill.skillId], user)

        // user 2: imported skills from multiple projects
        skillsService.addSkill([projectId: project1.projectId, skillId: skill.skillId], user2)
        skillsService.addSkill([projectId: project3.projectId, skillId: skill7.skillId], user2)
        skillsService.addSkill([projectId: project3.projectId, skillId: skill9.skillId], user2)
        skillsService.addSkill([projectId: project3.projectId, skillId: skill7.skillId], user2)
        skillsService.addSkill([projectId: project3.projectId, skillId: skill7.skillId], user2)

        // user 3: imported skills from multiple projects and local
        skillsService.addSkill([projectId: project1.projectId, skillId: skill.skillId], user3)
        skillsService.addSkill([projectId: project1.projectId, skillId: skill2.skillId], user3) // imported in proj3
        skillsService.addSkill([projectId: project3.projectId, skillId: skill7.skillId], user3)
        skillsService.addSkill([projectId: project3.projectId, skillId: skill9.skillId], user3)
        skillsService.addSkill([projectId: project3.projectId, skillId: skill7.skillId], user3)
        skillsService.addSkill([projectId: project3.projectId, skillId: skill7.skillId], user3)
        skillsService.addSkill([projectId: project2.projectId, skillId: skill4.skillId], user3)
        skillsService.addSkill([projectId: project2.projectId, skillId: skill5.skillId], user3)
        skillsService.addSkill([projectId: project2.projectId, skillId: skill6.skillId], user3)
        skillsService.addSkill([projectId: project2.projectId, skillId: skill4.skillId], user3)
        skillsService.addSkill([projectId: project2.projectId, skillId: skill4.skillId], user3)
        Thread.sleep(2500) //have to wait on async awards

        def p1Stats = skillsService.getUserStats(project1.projectId, user)
        def p2Stats = skillsService.getUserStats(project2.projectId, user)
        def p3Stats = skillsService.getUserStats(project3.projectId, user)

        def p1Stats_user2 = skillsService.getUserStats(project1.projectId, user2)
        def p2Stats_user2 = skillsService.getUserStats(project2.projectId, user2)
        def p3Stats_user2 = skillsService.getUserStats(project3.projectId, user2)

        def p1Stats_user3 = skillsService.getUserStats(project1.projectId, user3)
        def p2Stats_user3 = skillsService.getUserStats(project2.projectId, user3)
        def p3Stats_user3 = skillsService.getUserStats(project3.projectId, user3)

        then:
        p1Stats.numSkills == 1
        p1Stats.userTotalPoints == 250
        p2Stats.numSkills == 1
        p2Stats.userTotalPoints == 250
        p3Stats.numSkills == 0
        p3Stats.userTotalPoints == 0

        p1Stats_user2.numSkills == 1
        p1Stats_user2.userTotalPoints == 250
        p2Stats_user2.numSkills == 3
        p2Stats_user2.userTotalPoints == 1250
        p3Stats_user2.numSkills == 2
        p3Stats_user2.userTotalPoints == 1000

        p1Stats_user3.numSkills == 2
        p1Stats_user3.userTotalPoints == 500
        p2Stats_user3.numSkills == 6
        p2Stats_user3.userTotalPoints == 2500
        p3Stats_user3.numSkills == 3
        p3Stats_user3.userTotalPoints == 1250
    }
}
