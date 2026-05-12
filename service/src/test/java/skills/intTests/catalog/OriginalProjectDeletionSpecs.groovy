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

import skills.intTests.utils.SkillsFactory
import skills.storage.model.UserPoints

import static skills.intTests.utils.SkillsFactory.createSkills
import static skills.intTests.utils.SkillsFactory.createSkillsGroup

class OriginalProjectDeletionSpecs extends CatalogIntSpec {

    def "removing original subject should delete imported skills"() {
        def proj1 = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1)
        def skill2 = SkillsFactory.createSkill(1, 1, 2, 1, 10)
        skill2.pointIncrementInterval = 0
        skillsService.createProjectAndSubjectAndSkills(proj1, subj, [skill2])

        def proj2 = SkillsFactory.createProject(2)
        def p2_subj = SkillsFactory.createSubject(2)
        skillsService.createProjectAndSubjectAndSkills(proj2, p2_subj, [])

        skillsService.exportSkillToCatalog(proj1.projectId, skill2.skillId)
        skillsService.bulkImportSkillsFromCatalogAndFinalize(proj2.projectId, p2_subj.subjectId, [
                [projectId: proj1.projectId, skillId: skill2.skillId],
        ])

        when:
        def skillsP1_before = skillsService.getSkillsForProject(proj1.projectId)
        def skillsP2_before = skillsService.getSkillsForProject(proj2.projectId)
        def p2_subj_before = skillsService.getSubject(p2_subj)
        def p2_before = skillsService.getProject(proj2.projectId)
        skillsService.deleteSubject(subj)

        def skillsP1_after = skillsService.getSkillsForProject(proj1.projectId)
        def skillsP2_after = skillsService.getSkillsForProject(proj2.projectId)
        def p2_subj_after = skillsService.getSubject(p2_subj)
        def p2_after = skillsService.getProject(proj2.projectId)
        then:
        skillsP1_before.skillId == [skill2.skillId]
        skillsP2_before.skillId == [skill2.skillId]
        p2_subj_before.totalPoints == 100
        p2_subj_before.numSkills == 1
        p2_before.numSkills == 1
        p2_before.totalPoints == 100

        !skillsP1_after
        !skillsP2_after
        p2_subj_after.totalPoints == 0
        p2_subj_after.numSkills == 0
        p2_after.numSkills == 0
        p2_after.totalPoints == 0
    }

    def "removing original subject should delete imported group skills"() {
        def proj1 = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1)
        def p1subj1g1 = createSkillsGroup(1, 1, 11)
        skillsService.createProjectAndSubjectAndSkills(proj1, subj, [p1subj1g1])
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        p1Skills.each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }

        def proj2 = SkillsFactory.createProject(2)
        def p2_subj = SkillsFactory.createSubject(2, 2)
        def p2subj2g2 = createSkillsGroup(2, 2, 22)
        skillsService.createProjectAndSubjectAndSkills(proj2, p2_subj, [p2subj2g2])

        p1Skills.each {
            skillsService.exportSkillToCatalog(it.projectId, it.skillId)
        }
        skillsService.bulkImportSkillsIntoGroupFromCatalogAndFinalize(proj2.projectId, p2_subj.subjectId, p2subj2g2.skillId,
                p1Skills.collect { [projectId: it.projectId, skillId: it.skillId] },
        )

        when:
        def skillsP2_before = skillsService.getSkillsForProject(proj2.projectId)
        def p2_subj_before = skillsService.getSubject(p2_subj)
        def p2_before = skillsService.getProject(proj2.projectId)
        skillsService.deleteSubject(subj)

        def skillsP1_after = skillsService.getSkillsForProject(proj1.projectId)
        def skillsP2_after = skillsService.getSkillsForProject(proj2.projectId)
        def p2_subj_after = skillsService.getSubject(p2_subj)
        def p2_after = skillsService.getProject(proj2.projectId)
        then:
        skillsP2_before.skillId == p1Skills.skillId
        p2_subj_before.totalPoints == 1500
        p2_subj_before.numSkills == 3
        p2_before.numSkills == 3
        p2_before.totalPoints == 1500

        !skillsP1_after
        !skillsP2_after
        p2_subj_after.totalPoints == 0
        p2_subj_after.numSkills == 0
        p2_after.numSkills == 0
        p2_after.totalPoints == 0
    }

    def "removing original subject should update user points as well as calculate levels"() {
        def proj1 = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1)
        def p1Skills = createSkills(3, 1, 1, 100, 10)
        skillsService.createProjectAndSubjectAndSkills(proj1, subj, p1Skills)

        def proj2 = SkillsFactory.createProject(2)
        def p2_subj = SkillsFactory.createSubject(2, 2)
        def p2Skills = createSkills(3, 2, 2, 99, 2)
        skillsService.createProjectAndSubjectAndSkills(proj2, p2_subj, p2Skills)

        p1Skills.each {
            skillsService.exportSkillToCatalog(it.projectId, it.skillId)
        }
        skillsService.bulkImportSkillsFromCatalogAndFinalize(proj2.projectId, p2_subj.subjectId,
                p1Skills.collect { [projectId: it.projectId, skillId: it.skillId] },
        )

        List<String> users = getRandomUsers(2)
        skillsService.addSkill(p1Skills[0], users[0], new Date() - 2)
        skillsService.addSkill(p1Skills[0], users[0], new Date() - 1)
        skillsService.addSkill(p1Skills[0], users[0])

        skillsService.addSkill(p2Skills[0], users[0], new Date() - 2)
        skillsService.addSkill(p2Skills[0], users[0], new Date() - 1)

        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        when:
        List<UserPoints> userPoints_before = userPointsRepo.findByProjectIdAndUserId(proj2.projectId, users[0])
        Integer level_before = skillsService.getUserLevel(proj2.projectId, users[0])
        skillsService.deleteSubject(subj)
        List<UserPoints> userPoints_after = userPointsRepo.findByProjectIdAndUserId(proj2.projectId, users[0])
        Integer level_after = skillsService.getUserLevel(proj2.projectId, users[0])

        then:
        level_before == 1
        level_after == 2

        userPoints_before.size() == 4
        userPoints_before.find { it.skillId == p2Skills[0].skillId }.points == 198
        userPoints_before.find { it.skillId == p1Skills[0].skillId }.points == 300
        userPoints_before.find { it.skillId == p2_subj.subjectId }.points == 300 + 198
        userPoints_before.find { !it.skillId }.points == 300 + 198

        userPoints_after.size() == 3
        userPoints_before.find { it.skillId == p2Skills[0].skillId }.points == 198
        userPoints_after.find { it.skillId == p2_subj.subjectId }.points == 198
        userPoints_after.find { !it.skillId }.points == 198
    }

    def "removing original subject should update user points as well as calculate levels - group skills"() {
        def proj1 = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1)
        def p1subj1g1 = createSkillsGroup(1, 1, 11)
        skillsService.createProjectAndSubjectAndSkills(proj1, subj, [p1subj1g1])
        def p1Skills = createSkills(3, 1, 1, 100, 10)
        p1Skills.each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }

        def proj2 = SkillsFactory.createProject(2)
        def p2_subj = SkillsFactory.createSubject(2, 2)
        def p2subj2g2 = createSkillsGroup(2, 2, 22)
        skillsService.createProjectAndSubjectAndSkills(proj2, p2_subj, [p2subj2g2])
        def p2Skills = createSkills(3, 2, 2, 99, 2)
        p2Skills.each {
            skillsService.assignSkillToSkillsGroup(p2subj2g2.skillId, it)
        }

        p1Skills.each {
            skillsService.exportSkillToCatalog(it.projectId, it.skillId)
        }
        skillsService.bulkImportSkillsIntoGroupFromCatalogAndFinalize(proj2.projectId, p2_subj.subjectId, p2subj2g2.skillId,
                p1Skills.collect { [projectId: it.projectId, skillId: it.skillId] },
        )

        List<String> users = getRandomUsers(2)
        skillsService.addSkill(p1Skills[0], users[0], new Date() - 2)
        skillsService.addSkill(p1Skills[0], users[0], new Date() - 1)
        skillsService.addSkill(p1Skills[0], users[0])

        skillsService.addSkill(p2Skills[0], users[0], new Date() - 2)
        skillsService.addSkill(p2Skills[0], users[0], new Date() - 1)

        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        when:
        List<UserPoints> userPoints_before = userPointsRepo.findByProjectIdAndUserId(proj2.projectId, users[0])
        Integer level_before = skillsService.getUserLevel(proj2.projectId, users[0])
        skillsService.deleteSubject(subj)
        List<UserPoints> userPoints_after = userPointsRepo.findByProjectIdAndUserId(proj2.projectId, users[0])
        Integer level_after = skillsService.getUserLevel(proj2.projectId, users[0])

        then:
        level_before == 1
        level_after == 2

        userPoints_before.size() == 4
        userPoints_before.find { it.skillId == p2Skills[0].skillId }.points == 198
        userPoints_before.find { it.skillId == p1Skills[0].skillId }.points == 300
        userPoints_before.find { it.skillId == p2_subj.subjectId }.points == 300 + 198
        userPoints_before.find { !it.skillId }.points == 300 + 198

        userPoints_after.size() == 3
        userPoints_before.find { it.skillId == p2Skills[0].skillId }.points == 198
        userPoints_after.find { it.skillId == p2_subj.subjectId }.points == 198
        userPoints_after.find { !it.skillId }.points == 198
    }

    def "removing original group should update user points as well as calculate levels - group skills"() {
        def proj1 = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1)
        def p1subj1g1 = createSkillsGroup(1, 1, 11)
        skillsService.createProjectAndSubjectAndSkills(proj1, subj, [p1subj1g1])
        def p1Skills = createSkills(3, 1, 1, 100, 10)
        p1Skills.each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }

        def proj2 = SkillsFactory.createProject(2)
        def p2_subj = SkillsFactory.createSubject(2, 2)
        def p2subj2g2 = createSkillsGroup(2, 2, 22)
        skillsService.createProjectAndSubjectAndSkills(proj2, p2_subj, [p2subj2g2])
        def p2Skills = createSkills(3, 2, 2, 99, 2)
        p2Skills.each {
            skillsService.assignSkillToSkillsGroup(p2subj2g2.skillId, it)
        }

        p1Skills.each {
            skillsService.exportSkillToCatalog(it.projectId, it.skillId)
        }
        skillsService.bulkImportSkillsIntoGroupFromCatalogAndFinalize(proj2.projectId, p2_subj.subjectId, p2subj2g2.skillId,
                p1Skills.collect { [projectId: it.projectId, skillId: it.skillId] },
        )

        List<String> users = getRandomUsers(2)
        skillsService.addSkill(p1Skills[0], users[0], new Date() - 2)
        skillsService.addSkill(p1Skills[0], users[0], new Date() - 1)
        skillsService.addSkill(p1Skills[0], users[0])

        skillsService.addSkill(p2Skills[0], users[0], new Date() - 2)
        skillsService.addSkill(p2Skills[0], users[0], new Date() - 1)

        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        when:
        List<UserPoints> userPoints_before = userPointsRepo.findByProjectIdAndUserId(proj2.projectId, users[0])
        Integer level_before = skillsService.getUserLevel(proj2.projectId, users[0])
        skillsService.deleteSkill(p1subj1g1)
        List<UserPoints> userPoints_after = userPointsRepo.findByProjectIdAndUserId(proj2.projectId, users[0])
        Integer level_after = skillsService.getUserLevel(proj2.projectId, users[0])

        then:
        userPoints_before.size() == 4
        userPoints_before.find { it.skillId == p2Skills[0].skillId }.points == 198
        userPoints_before.find { it.skillId == p1Skills[0].skillId }.points == 300
        userPoints_before.find { it.skillId == p2_subj.subjectId }.points == 300 + 198
        userPoints_before.find { !it.skillId }.points == 300 + 198

        userPoints_after.size() == 3
        userPoints_before.find { it.skillId == p2Skills[0].skillId }.points == 198
        userPoints_after.find { it.skillId == p2_subj.subjectId }.points == 198
        userPoints_after.find { !it.skillId }.points == 198

        level_before == 1
        level_after == 2
    }

    def "removing original project should delete imported skills"() {
        def proj1 = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1)
        def skill2 = SkillsFactory.createSkill(1, 1, 2, 1, 10)
        skill2.pointIncrementInterval = 0
        skillsService.createProjectAndSubjectAndSkills(proj1, subj, [skill2])

        def proj2 = SkillsFactory.createProject(2)
        def p2_subj = SkillsFactory.createSubject(2)
        skillsService.createProjectAndSubjectAndSkills(proj2, p2_subj, [])

        skillsService.exportSkillToCatalog(proj1.projectId, skill2.skillId)
        skillsService.bulkImportSkillsFromCatalogAndFinalize(proj2.projectId, p2_subj.subjectId, [
                [projectId: proj1.projectId, skillId: skill2.skillId],
        ])

        when:
        def skillsP1_before = skillsService.getSkillsForProject(proj1.projectId)
        def skillsP2_before = skillsService.getSkillsForProject(proj2.projectId)
        def p2_subj_before = skillsService.getSubject(p2_subj)
        def p2_before = skillsService.getProject(proj2.projectId)
        skillsService.deleteProject(proj1.projectId)

        def skillsP2_after = skillsService.getSkillsForProject(proj2.projectId)
        def p2_subj_after = skillsService.getSubject(p2_subj)
        def p2_after = skillsService.getProject(proj2.projectId)
        then:
        skillsP1_before.skillId == [skill2.skillId]
        skillsP2_before.skillId == [skill2.skillId]
        p2_subj_before.totalPoints == 100
        p2_subj_before.numSkills == 1
        p2_before.numSkills == 1
        p2_before.totalPoints == 100

        !skillsP2_after
        p2_subj_after.totalPoints == 0
        p2_subj_after.numSkills == 0
        p2_after.numSkills == 0
        p2_after.totalPoints == 0
    }

    def "removing original project should delete imported group skills"() {
        def proj1 = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1)
        def p1subj1g1 = createSkillsGroup(1, 1, 11)
        skillsService.createProjectAndSubjectAndSkills(proj1, subj, [p1subj1g1])
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        p1Skills.each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }

        def proj2 = SkillsFactory.createProject(2)
        def p2_subj = SkillsFactory.createSubject(2, 2)
        def p2subj2g2 = createSkillsGroup(2, 2, 22)
        skillsService.createProjectAndSubjectAndSkills(proj2, p2_subj, [p2subj2g2])

        p1Skills.each {
            skillsService.exportSkillToCatalog(it.projectId, it.skillId)
        }
        skillsService.bulkImportSkillsIntoGroupFromCatalogAndFinalize(proj2.projectId, p2_subj.subjectId, p2subj2g2.skillId,
                p1Skills.collect { [projectId: it.projectId, skillId: it.skillId] },
        )

        when:
        def skillsP2_before = skillsService.getSkillsForProject(proj2.projectId)
        def p2_subj_before = skillsService.getSubject(p2_subj)
        def p2_before = skillsService.getProject(proj2.projectId)
        skillsService.deleteProject(proj1.projectId)

        def skillsP2_after = skillsService.getSkillsForProject(proj2.projectId)
        def p2_subj_after = skillsService.getSubject(p2_subj)
        def p2_after = skillsService.getProject(proj2.projectId)
        then:
        skillsP2_before.skillId == p1Skills.skillId
        p2_subj_before.totalPoints == 1500
        p2_subj_before.numSkills == 3
        p2_before.numSkills == 3
        p2_before.totalPoints == 1500

        !skillsP2_after
        p2_subj_after.totalPoints == 0
        p2_subj_after.numSkills == 0
        p2_after.numSkills == 0
        p2_after.totalPoints == 0
    }

    def "removing original project should update user points as well as calculate levels"() {
        def proj1 = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1)
        def p1Skills = createSkills(3, 1, 1, 100, 10)
        skillsService.createProjectAndSubjectAndSkills(proj1, subj, p1Skills)

        def proj2 = SkillsFactory.createProject(2)
        def p2_subj = SkillsFactory.createSubject(2, 2)
        def p2Skills = createSkills(3, 2, 2, 99, 2)
        skillsService.createProjectAndSubjectAndSkills(proj2, p2_subj, p2Skills)

        p1Skills.each {
            skillsService.exportSkillToCatalog(it.projectId, it.skillId)
        }
        skillsService.bulkImportSkillsFromCatalogAndFinalize(proj2.projectId, p2_subj.subjectId,
                p1Skills.collect { [projectId: it.projectId, skillId: it.skillId] },
        )

        List<String> users = getRandomUsers(2)
        skillsService.addSkill(p1Skills[0], users[0], new Date() - 2)
        skillsService.addSkill(p1Skills[0], users[0], new Date() - 1)
        skillsService.addSkill(p1Skills[0], users[0])

        skillsService.addSkill(p2Skills[0], users[0], new Date() - 2)
        skillsService.addSkill(p2Skills[0], users[0], new Date() - 1)

        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        when:
        List<UserPoints> userPoints_before = userPointsRepo.findByProjectIdAndUserId(proj2.projectId, users[0])
        Integer level_before = skillsService.getUserLevel(proj2.projectId, users[0])
        skillsService.deleteProject(proj1.projectId)
        List<UserPoints> userPoints_after = userPointsRepo.findByProjectIdAndUserId(proj2.projectId, users[0])
        Integer level_after = skillsService.getUserLevel(proj2.projectId, users[0])

        then:
        level_before == 1
        level_after == 2

        userPoints_before.size() == 4
        userPoints_before.find { it.skillId == p2Skills[0].skillId }.points == 198
        userPoints_before.find { it.skillId == p1Skills[0].skillId }.points == 300
        userPoints_before.find { it.skillId == p2_subj.subjectId }.points == 300 + 198
        userPoints_before.find { !it.skillId }.points == 300 + 198

        userPoints_after.size() == 3
        userPoints_before.find { it.skillId == p2Skills[0].skillId }.points == 198
        userPoints_after.find { it.skillId == p2_subj.subjectId }.points == 198
        userPoints_after.find { !it.skillId }.points == 198
    }

    def "removing original project should update user points as well as calculate levels - group skills"() {
        def proj1 = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1)
        def p1subj1g1 = createSkillsGroup(1, 1, 11)
        skillsService.createProjectAndSubjectAndSkills(proj1, subj, [p1subj1g1])
        def p1Skills = createSkills(3, 1, 1, 100, 10)
        p1Skills.each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }

        def proj2 = SkillsFactory.createProject(2)
        def p2_subj = SkillsFactory.createSubject(2, 2)
        def p2subj2g2 = createSkillsGroup(2, 2, 22)
        skillsService.createProjectAndSubjectAndSkills(proj2, p2_subj, [p2subj2g2])
        def p2Skills = createSkills(3, 2, 2, 99, 2)
        p2Skills.each {
            skillsService.assignSkillToSkillsGroup(p2subj2g2.skillId, it)
        }

        p1Skills.each {
            skillsService.exportSkillToCatalog(it.projectId, it.skillId)
        }
        skillsService.bulkImportSkillsIntoGroupFromCatalogAndFinalize(proj2.projectId, p2_subj.subjectId, p2subj2g2.skillId,
                p1Skills.collect { [projectId: it.projectId, skillId: it.skillId] },
        )

        List<String> users = getRandomUsers(2)
        skillsService.addSkill(p1Skills[0], users[0], new Date() - 2)
        skillsService.addSkill(p1Skills[0], users[0], new Date() - 1)
        skillsService.addSkill(p1Skills[0], users[0])

        skillsService.addSkill(p2Skills[0], users[0], new Date() - 2)
        skillsService.addSkill(p2Skills[0], users[0], new Date() - 1)

        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        when:
        List<UserPoints> userPoints_before = userPointsRepo.findByProjectIdAndUserId(proj2.projectId, users[0])
        Integer level_before = skillsService.getUserLevel(proj2.projectId, users[0])
        skillsService.deleteProject(proj1.projectId)
        List<UserPoints> userPoints_after = userPointsRepo.findByProjectIdAndUserId(proj2.projectId, users[0])
        Integer level_after = skillsService.getUserLevel(proj2.projectId, users[0])

        then:
        userPoints_before.size() == 4
        userPoints_before.find { it.skillId == p2Skills[0].skillId }.points == 198
        userPoints_before.find { it.skillId == p1Skills[0].skillId }.points == 300
        userPoints_before.find { it.skillId == p2_subj.subjectId }.points == 300 + 198
        userPoints_before.find { !it.skillId }.points == 300 + 198

        userPoints_after.size() == 3
        userPoints_before.find { it.skillId == p2Skills[0].skillId }.points == 198
        userPoints_after.find { it.skillId == p2_subj.subjectId }.points == 198
        userPoints_after.find { !it.skillId }.points == 198

        level_before == 1
        level_after == 2
    }

}
