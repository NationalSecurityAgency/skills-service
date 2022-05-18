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

import static skills.intTests.utils.SkillsFactory.*

class CatalogExportSkillsUnderGroupSpec extends CatalogIntSpec {

    def "export skills under a group to the catalog"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [])

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2skillsGroup = SkillsFactory.createSkillsGroup(2, 1, 5)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [p2skillsGroup])
        def gSkill1 = createSkill(2, 1, 10, 0, 50)
        def gSkill2 = createSkill(2, 1, 11, 0, 50)
        skillsService.assignSkillToSkillsGroup(p2skillsGroup.skillId, gSkill1)
        skillsService.assignSkillToSkillsGroup(p2skillsGroup.skillId, gSkill2)

        when:
        skillsService.bulkExportSkillsToCatalog(p2.projectId, [gSkill1, gSkill2].collect { it.skillId })

        def p2Exported = skillsService.getExportedSkills(p2.projectId, 10, 1, "exportedOn", true)
        def catalogSkills = skillsService.getCatalogSkills(p1.projectId, 10, 1)
        def skillsUnderGroup = skillsService.getSkillsForGroup(p2.projectId, p2skillsGroup.skillId)

        then:
        p2Exported.count == 2
        p2Exported.data.skillName == [gSkill1.name, gSkill2.name]
        p2Exported.data.groupName == [p2skillsGroup.name, p2skillsGroup.name]

        catalogSkills.count == 2
        catalogSkills.data.name == [gSkill1.name, gSkill2.name]
        catalogSkills.data.projectId == [p2.projectId, p2.projectId]
        skillsUnderGroup.sharedToCatalog == [true, true]
    }

    def "imported stats for the exported group skills"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [])

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2skillsGroup = SkillsFactory.createSkillsGroup(2, 1, 5)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [p2skillsGroup])
        def gSkill1 = createSkill(2, 1, 10, 0, 50)
        def gSkill2 = createSkill(2, 1, 11, 0, 50)
        def gSkill3 = createSkill(2, 1, 12, 0, 50)
        skillsService.assignSkillToSkillsGroup(p2skillsGroup.skillId, gSkill1)
        skillsService.assignSkillToSkillsGroup(p2skillsGroup.skillId, gSkill2)
        skillsService.assignSkillToSkillsGroup(p2skillsGroup.skillId, gSkill3)

        def p3 = createProject(3)
        def p3subj1 = createSubject(3, 1)
        skillsService.createProjectAndSubjectAndSkills(p3, p3subj1, [])

        def p4 = createProject(4)
        def p4subj1 = createSubject(4, 1)
        skillsService.createProjectAndSubjectAndSkills(p4, p4subj1, [])

        skillsService.bulkExportSkillsToCatalog(p2.projectId, [gSkill1, gSkill2, gSkill3].collect { it.skillId })

        skillsService.bulkImportSkillsFromCatalog(p1.projectId, p1subj1.subjectId,
                [[projectId: p2.projectId, skillId: gSkill1.skillId], [projectId: p2.projectId, skillId: gSkill2.skillId]])

        skillsService.bulkImportSkillsFromCatalogAndFinalize(p3.projectId, p3subj1.subjectId,
                [[projectId: p2.projectId, skillId: gSkill1.skillId], [projectId: p2.projectId, skillId: gSkill2.skillId], [projectId: p2.projectId, skillId: gSkill3.skillId]])

        skillsService.bulkImportSkillsFromCatalogAndFinalize(p4.projectId, p4subj1.subjectId,
                [[projectId: p2.projectId, skillId: gSkill1.skillId], [projectId: p2.projectId, skillId: gSkill2.skillId], [projectId: p2.projectId, skillId: gSkill3.skillId]])

        when:
        def p2Exported = skillsService.getExportedSkills(p2.projectId, 10, 1, "exportedOn", true)
        def p2_sk1_stats = skillsService.getExportedSkillStats(p2.projectId, gSkill1.skillId).users.sort { it.projectId }
        def p2_sk2_stats = skillsService.getExportedSkillStats(p2.projectId, gSkill2.skillId).users.sort { it.projectId }
        def p2_sk3_stats = skillsService.getExportedSkillStats(p2.projectId, gSkill3.skillId).users.sort { it.projectId }
        then:
        p2Exported.count == 3
        p2Exported.data.skillName == [gSkill1.name, gSkill2.name, gSkill3.name]
        p2Exported.data.importedProjectCount == [3, 3, 2]

        p2_sk1_stats.importingProjectId == [p1.projectId, p3.projectId, p4.projectId]
        p2_sk1_stats.enabled == ["false", "true", "true"]
        p2_sk2_stats.importingProjectId == [p1.projectId, p3.projectId, p4.projectId]
        p2_sk2_stats.enabled == ["false", "true", "true"]
        p2_sk3_stats.importingProjectId == [p3.projectId, p4.projectId]
        p2_sk3_stats.enabled == ["true", "true"]
    }

    def "import skills that were exported from a group"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [])

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2skillsGroup = SkillsFactory.createSkillsGroup(2, 1, 5)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [p2skillsGroup])
        def gSkill1 = createSkill(2, 1, 10, 0, 50)
        def gSkill2 = createSkill(2, 1, 11, 0, 50)
        skillsService.assignSkillToSkillsGroup(p2skillsGroup.skillId, gSkill1)
        skillsService.assignSkillToSkillsGroup(p2skillsGroup.skillId, gSkill2)

        skillsService.bulkExportSkillsToCatalog(p2.projectId, [gSkill1, gSkill2].collect { it.skillId })

        when:
        def project_t0 = skillsService.getProject(p1.projectId)
        def projects_t0 = skillsService.getProjects()
        def subject_t0 = skillsService.getSubject(p1subj1)
        def subjects_t0 = skillsService.getSubjects(p1.projectId)
        def p1subj1Skills_t0 = skillsService.getSkillsForSubject(p1.projectId, p1subj1.subjectId)

        skillsService.bulkImportSkillsFromCatalog(p1.projectId, p1subj1.subjectId,
                [[projectId: p2.projectId, skillId: gSkill1.skillId], [projectId: p2.projectId, skillId: gSkill2.skillId]])

        def project_t1 = skillsService.getProject(p1.projectId)
        def projects_t1 = skillsService.getProjects()
        def subject_t1 = skillsService.getSubject(p1subj1)
        def subjects_t1 = skillsService.getSubjects(p1.projectId)
        def p1subj1Skills_t1 = skillsService.getSkillsForSubject(p1.projectId, p1subj1.subjectId)

        skillsService.finalizeSkillsImportFromCatalog(p1.projectId)

        def project_t2 = skillsService.getProject(p1.projectId)
        def projects_t2 = skillsService.getProjects()
        def subject_t2 = skillsService.getSubject(p1subj1)
        def subjects_t2 = skillsService.getSubjects(p1.projectId)
        def p1subj1Skills_t2 = skillsService.getSkillsForSubject(p1.projectId, p1subj1.subjectId)

        then:
        project_t0.totalPoints == 0
        projects_t0.totalPoints == [0, 1000]
        subject_t0.totalPoints == 0
        subjects_t0.totalPoints == [0]
        p1subj1Skills_t0.skillId == []
        p1subj1Skills_t0.totalPoints == []
        project_t0.numSkillsDisabled == 0
        subject_t0.numSkillsDisabled == 0
        project_t0.numSkills == 0
        subject_t0.numSkills == 0

        project_t1.totalPoints == 0
        projects_t1.totalPoints == [0, 1000]
        subject_t1.totalPoints == 0
        subjects_t1.totalPoints == [0]
        p1subj1Skills_t1.skillId == [gSkill1.skillId, gSkill2.skillId]
        p1subj1Skills_t1.totalPoints == [500, 500]
        project_t1.numSkills == 0
        subject_t1.numSkills == 0
        project_t1.numSkillsDisabled == 2
        subject_t1.numSkillsDisabled == 2

        project_t2.totalPoints == 1000
        projects_t2.totalPoints == [1000, 1000]
        subject_t2.totalPoints == 1000
        subjects_t2.totalPoints == [1000]
        p1subj1Skills_t2.skillId == [gSkill1.skillId, gSkill2.skillId]
        p1subj1Skills_t2.totalPoints == [500, 500]
        project_t2.numSkills == 2
        subject_t2.numSkills == 2
        project_t2.numSkillsDisabled == 0
        subject_t2.numSkillsDisabled == 0
    }

    def "import skills into a group that were exported from a group"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1skillsGroup = SkillsFactory.createSkillsGroup(1, 1, 20)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [p1skillsGroup])

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2skillsGroup = SkillsFactory.createSkillsGroup(2, 1, 5)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [p2skillsGroup])
        def gSkill1 = createSkill(2, 1, 10, 0, 50)
        def gSkill2 = createSkill(2, 1, 11, 0, 50)
        skillsService.assignSkillToSkillsGroup(p2skillsGroup.skillId, gSkill1)
        skillsService.assignSkillToSkillsGroup(p2skillsGroup.skillId, gSkill2)

        skillsService.bulkExportSkillsToCatalog(p2.projectId, [gSkill1, gSkill2].collect { it.skillId })

        when:
        def project_t0 = skillsService.getProject(p1.projectId)
        def projects_t0 = skillsService.getProjects()
        def subject_t0 = skillsService.getSubject(p1subj1)
        def subjects_t0 = skillsService.getSubjects(p1.projectId)
        def p1subj1Skills_t0 = skillsService.getSkillsForSubject(p1.projectId, p1subj1.subjectId)
        def p1skillsGroupSkills_t0 = skillsService.getSkillsForGroup(p1.projectId, p1skillsGroup.skillId)

        skillsService.bulkImportSkillsIntoGroupFromCatalog(p1.projectId, p1subj1.subjectId, p1skillsGroup.skillId,
                [[projectId: p2.projectId, skillId: gSkill1.skillId], [projectId: p2.projectId, skillId: gSkill2.skillId]])

        def project_t1 = skillsService.getProject(p1.projectId)
        def projects_t1 = skillsService.getProjects()
        def subject_t1 = skillsService.getSubject(p1subj1)
        def subjects_t1 = skillsService.getSubjects(p1.projectId)
        def p1subj1Skills_t1 = skillsService.getSkillsForSubject(p1.projectId, p1subj1.subjectId)
        def p1skillsGroupSkills_t1 = skillsService.getSkillsForGroup(p1.projectId, p1skillsGroup.skillId)

        skillsService.finalizeSkillsImportFromCatalog(p1.projectId)

        def project_t2 = skillsService.getProject(p1.projectId)
        def projects_t2 = skillsService.getProjects()
        def subject_t2 = skillsService.getSubject(p1subj1)
        def subjects_t2 = skillsService.getSubjects(p1.projectId)
        def p1subj1Skills_t2 = skillsService.getSkillsForSubject(p1.projectId, p1subj1.subjectId)
        def p1skillsGroupSkills_t2 = skillsService.getSkillsForGroup(p1.projectId, p1skillsGroup.skillId)

        then:
        project_t0.totalPoints == 0
        projects_t0.totalPoints == [0, 1000]
        subject_t0.totalPoints == 0
        subjects_t0.totalPoints == [0]
        p1subj1Skills_t0.skillId == [p1skillsGroup.skillId]
        p1skillsGroupSkills_t0.skillId == []
        project_t0.numSkillsDisabled == 0
        subject_t0.numSkillsDisabled == 0
        project_t0.numSkills == 0
        subject_t0.numSkills == 0

        project_t1.totalPoints == 0
        projects_t1.totalPoints == [0, 1000]
        subject_t1.totalPoints == 0
        subjects_t1.totalPoints == [0]
        p1subj1Skills_t1.skillId == [p1skillsGroup.skillId]
        p1skillsGroupSkills_t1.skillId == [gSkill1.skillId, gSkill2.skillId]
        p1skillsGroupSkills_t1.totalPoints == [500, 500]
        project_t1.numSkills == 0
        subject_t1.numSkills == 0
        project_t1.numSkillsDisabled == 2
        subject_t1.numSkillsDisabled == 2

        project_t2.totalPoints == 1000
        projects_t2.totalPoints == [1000, 1000]
        subject_t2.totalPoints == 1000
        subjects_t2.totalPoints == [1000]
        p1subj1Skills_t2.skillId == [p1skillsGroup.skillId]
        p1skillsGroupSkills_t2.skillId == [gSkill1.skillId, gSkill2.skillId]
        p1skillsGroupSkills_t2.totalPoints == [500, 500]
        project_t2.numSkills == 2
        subject_t2.numSkills == 2
        project_t2.numSkillsDisabled == 0
        subject_t2.numSkillsDisabled == 0
    }

    def "import skills that were exported from a group - events are propagated"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [])

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2skillsGroup = SkillsFactory.createSkillsGroup(2, 1, 5)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [p2skillsGroup])
        def gSkill1 = createSkill(2, 1, 10, 0, 1, 480, 100)
        def gSkill2 = createSkill(2, 1, 11, 0, 1, 480, 100)
        skillsService.assignSkillToSkillsGroup(p2skillsGroup.skillId, gSkill1)
        skillsService.assignSkillToSkillsGroup(p2skillsGroup.skillId, gSkill2)

        skillsService.bulkExportSkillsToCatalog(p2.projectId, [gSkill1, gSkill2].collect { it.skillId })

        skillsService.bulkImportSkillsFromCatalogAndFinalize(p1.projectId, p1subj1.subjectId,
                [[projectId: p2.projectId, skillId: gSkill1.skillId], [projectId: p2.projectId, skillId: gSkill2.skillId]])
        String user = getRandomUsers(1)[0]
        when:
        def skill1_t0 = skillsService.getSingleSkillSummary(user, p1.projectId, gSkill1.skillId)
        def user_skill1_t0 = skillsService.getSkillSummary(user, p1.projectId, p1subj1.subjectId)

        skillsService.addSkill([projectId: p2.projectId, skillId: gSkill1.skillId], user)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        def skill1_t1 = skillsService.getSingleSkillSummary(user, p1.projectId, gSkill1.skillId)
        def user_skill1_t1 = skillsService.getSkillSummary(user, p1.projectId, p1subj1.subjectId)

        then:
        skill1_t0.points == 0
        skill1_t1.points == 100

        user_skill1_t0.skillsLevel == 0
        user_skill1_t0.points == 0

        user_skill1_t1.points == 100
        user_skill1_t1.skillsLevel == 3
    }

    def "import skills into a group that were exported from a group - events are propagated"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1skillsGroup = SkillsFactory.createSkillsGroup(1, 1, 20)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [p1skillsGroup])

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2skillsGroup = SkillsFactory.createSkillsGroup(2, 1, 5)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [p2skillsGroup])
        def gSkill1 = createSkill(2, 1, 10, 0, 1, 480, 100)
        def gSkill2 = createSkill(2, 1, 11, 0, 1, 480, 100)
        skillsService.assignSkillToSkillsGroup(p2skillsGroup.skillId, gSkill1)
        skillsService.assignSkillToSkillsGroup(p2skillsGroup.skillId, gSkill2)

        skillsService.bulkExportSkillsToCatalog(p2.projectId, [gSkill1, gSkill2].collect { it.skillId })

        skillsService.bulkImportSkillsIntoGroupFromCatalogAndFinalize(p1.projectId, p1subj1.subjectId, p1skillsGroup.skillId,
                [[projectId: p2.projectId, skillId: gSkill1.skillId], [projectId: p2.projectId, skillId: gSkill2.skillId]])
        String user = getRandomUsers(1)[0]
        when:
        def skill1_t0 = skillsService.getSingleSkillSummary(user, p1.projectId, gSkill1.skillId)
        def user_skill1_t0 = skillsService.getSkillSummary(user, p1.projectId, p1subj1.subjectId)

        skillsService.addSkill([projectId: p2.projectId, skillId: gSkill1.skillId], user)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        def skill1_t1 = skillsService.getSingleSkillSummary(user, p1.projectId, gSkill1.skillId)
        def user_skill1_t1 = skillsService.getSkillSummary(user, p1.projectId, p1subj1.subjectId)

        then:
        skill1_t0.points == 0
        skill1_t1.points == 100

        user_skill1_t0.skillsLevel == 0
        user_skill1_t0.points == 0

        user_skill1_t1.points == 100
        user_skill1_t1.skillsLevel == 3
    }

    def "import skills into a group that were exported from a group - definition updates are propagated"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1skillsGroup = SkillsFactory.createSkillsGroup(1, 1, 20)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [p1skillsGroup])

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2skillsGroup = SkillsFactory.createSkillsGroup(2, 1, 5)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [p2skillsGroup])
        def gSkill1 = createSkill(2, 1, 10, 0, 1, 480, 100)
        def gSkill2 = createSkill(2, 1, 11, 0, 1, 480, 100)
        skillsService.assignSkillToSkillsGroup(p2skillsGroup.skillId, gSkill1)
        skillsService.assignSkillToSkillsGroup(p2skillsGroup.skillId, gSkill2)

        skillsService.bulkExportSkillsToCatalog(p2.projectId, [gSkill1, gSkill2].collect { it.skillId })

        skillsService.bulkImportSkillsIntoGroupFromCatalogAndFinalize(p1.projectId, p1subj1.subjectId, p1skillsGroup.skillId,
                [[projectId: p2.projectId, skillId: gSkill1.skillId], [projectId: p2.projectId, skillId: gSkill2.skillId]])

        String gSkill1OrigName = gSkill1.name
        when:
        def p1skillsGroupSkills_t0 = skillsService.getSkillsForGroup(p1.projectId, p1skillsGroup.skillId)

        gSkill1.name = "Other name"
        skillsService.createSkill(gSkill1)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        def p1skillsGroupSkills_t1 = skillsService.getSkillsForGroup(p1.projectId, p1skillsGroup.skillId)
        then:
        p1skillsGroupSkills_t0.name == [gSkill1OrigName, gSkill2.name]
        p1skillsGroupSkills_t1.name == ["Other name", gSkill2.name]
    }

    def "During finalize migrate points for skills declared under a group - destination under a group"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)

        def p1skillsGroup = SkillsFactory.createSkillsGroup(1, 1, 50)
        def skillsAndGroup = [p1skillsGroup, p1Skills].flatten()
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, skillsAndGroup)
        p1Skills.each {
            skillsService.assignSkillToSkillsGroup(p1skillsGroup.skillId, it)
        }
        skillsService.bulkExportSkillsToCatalog(p1.projectId, p1Skills.collect { it.skillId })

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2skillsGroup = SkillsFactory.createSkillsGroup(2, 1, 5)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [p2skillsGroup])

        List<String> users = getRandomUsers(3)
        skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[1].skillId], users[0])
        skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[0].skillId], users[1])
        skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[1].skillId], users[1])

        when:
        def user0_skill1_t0 = skillsService.getSkillSummary(users[0], p2.projectId, p2subj1.subjectId)
        def user1_skill1_t0 = skillsService.getSkillSummary(users[1], p2.projectId, p2subj1.subjectId)

        skillsService.bulkImportSkillsIntoGroupFromCatalogAndFinalize(p2.projectId, p2subj1.subjectId, p2skillsGroup.skillId,
                p1Skills.collect { [projectId: it.projectId, skillId: it.skillId] })
        def user0_skill1_t1 = skillsService.getSkillSummary(users[0], p2.projectId, p2subj1.subjectId)
        def user1_skill1_t1 = skillsService.getSkillSummary(users[1], p2.projectId, p2subj1.subjectId)

        then:
        user0_skill1_t0.skillsLevel == 0
        user0_skill1_t0.points == 0
        user1_skill1_t0.skillsLevel == 0
        user1_skill1_t0.points == 0

        user0_skill1_t1.points == 100
        user0_skill1_t1.skillsLevel == 2
        def user0_skill1_t1_points = user0_skill1_t1.skills[0].children
        user0_skill1_t1_points[0].points == 0
        user0_skill1_t1_points[1].points == 100
        user0_skill1_t1_points[2].points == 0

        user1_skill1_t1.points == 200
        user1_skill1_t1.skillsLevel == 3
        def user1_skill1_t1_points = user1_skill1_t1.skills[0].children
        user1_skill1_t1_points[0].points == 100
        user1_skill1_t1_points[1].points == 100
        user1_skill1_t1_points[2].points == 0
    }

    def "During finalize migrate points for skills declared under a group - destination under a subject"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)

        def p1skillsGroup = SkillsFactory.createSkillsGroup(1, 1, 50)
        def skillsAndGroup = [p1skillsGroup, p1Skills].flatten()
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, skillsAndGroup)
        p1Skills.each {
            skillsService.assignSkillToSkillsGroup(p1skillsGroup.skillId, it)
        }
        skillsService.bulkExportSkillsToCatalog(p1.projectId, p1Skills.collect { it.skillId })

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [])

        List<String> users = getRandomUsers(3)
        skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[1].skillId], users[0])
        skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[0].skillId], users[1])
        skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[1].skillId], users[1])

        when:
        def user0_skill1_t0 = skillsService.getSkillSummary(users[0], p2.projectId, p2subj1.subjectId)
        def user1_skill1_t0 = skillsService.getSkillSummary(users[1], p2.projectId, p2subj1.subjectId)

        skillsService.bulkImportSkillsFromCatalogAndFinalize(p2.projectId, p2subj1.subjectId,
                p1Skills.collect { [projectId: it.projectId, skillId: it.skillId] })
        def user0_skill1_t1 = skillsService.getSkillSummary(users[0], p2.projectId, p2subj1.subjectId)
        def user1_skill1_t1 = skillsService.getSkillSummary(users[1], p2.projectId, p2subj1.subjectId)

        then:
        user0_skill1_t0.skillsLevel == 0
        user0_skill1_t0.points == 0
        user1_skill1_t0.skillsLevel == 0
        user1_skill1_t0.points == 0

        user0_skill1_t1.points == 100
        user0_skill1_t1.skillsLevel == 2
        def user0_skill1_t1_points = user0_skill1_t1.skills
        user0_skill1_t1_points[0].points == 0
        user0_skill1_t1_points[1].points == 100
        user0_skill1_t1_points[2].points == 0

        user1_skill1_t1.points == 200
        user1_skill1_t1.skillsLevel == 3
        def user1_skill1_t1_points = user1_skill1_t1.skills
        user1_skill1_t1_points[0].points == 100
        user1_skill1_t1_points[1].points == 100
        user1_skill1_t1_points[2].points == 0
    }

    def "export skills under a group to the catalog - then delete skill"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [])

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2skillsGroup = SkillsFactory.createSkillsGroup(2, 1, 5)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [p2skillsGroup])
        def gSkill1 = createSkill(2, 1, 10, 0, 50)
        def gSkill2 = createSkill(2, 1, 11, 0, 50)
        skillsService.assignSkillToSkillsGroup(p2skillsGroup.skillId, gSkill1)
        skillsService.assignSkillToSkillsGroup(p2skillsGroup.skillId, gSkill2)

        skillsService.bulkExportSkillsToCatalog(p2.projectId, [gSkill1, gSkill2].collect { it.skillId })
        when:

        def p2Exported = skillsService.getExportedSkills(p2.projectId, 10, 1, "exportedOn", true)
        def catalogSkills = skillsService.getCatalogSkills(p1.projectId, 10, 1)

        skillsService.removeSkillFromCatalog(p2.projectId, gSkill1.skillId)

        def p2Exported_t1 = skillsService.getExportedSkills(p2.projectId, 10, 1, "exportedOn", true)
        def catalogSkills_t1 = skillsService.getCatalogSkills(p1.projectId, 10, 1)

        then:
        p2Exported.count == 2
        p2Exported.data.skillName == [gSkill1.name, gSkill2.name]
        p2Exported.data.groupName == [p2skillsGroup.name, p2skillsGroup.name]

        catalogSkills.count == 2
        catalogSkills.data.name == [gSkill1.name, gSkill2.name]
        catalogSkills.data.projectId == [p2.projectId, p2.projectId]

        p2Exported_t1.count == 1
        p2Exported_t1.data.skillName == [gSkill2.name]
        p2Exported_t1.data.groupName == [p2skillsGroup.name]

        catalogSkills_t1.count == 1
        catalogSkills_t1.data.name == [gSkill2.name]
        catalogSkills_t1.data.projectId == [p2.projectId]
    }

    def "export skills under a group to the catalog - then remove skill from catalog - points and skills are removed from all the importing projects"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)

        def p1skillsGroup = SkillsFactory.createSkillsGroup(1, 1, 50)
        def skillsAndGroup = [p1skillsGroup, p1Skills].flatten()
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, skillsAndGroup)
        p1Skills.each {
            skillsService.assignSkillToSkillsGroup(p1skillsGroup.skillId, it)
        }
        skillsService.bulkExportSkillsToCatalog(p1.projectId, p1Skills.collect { it.skillId })

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [])

        List<String> users = getRandomUsers(3)
        skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[1].skillId], users[0])
        skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[0].skillId], users[1])
        skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[1].skillId], users[1])

        skillsService.bulkImportSkillsFromCatalogAndFinalize(p2.projectId, p2subj1.subjectId,
                p1Skills.collect { [projectId: it.projectId, skillId: it.skillId] })
        when:
        def user0_skill1_t1 = skillsService.getSkillSummary(users[0], p2.projectId, p2subj1.subjectId)
        def user1_skill1_t1 = skillsService.getSkillSummary(users[1], p2.projectId, p2subj1.subjectId)

        skillsService.removeSkillFromCatalog(p1.projectId, p1Skills[0].skillId)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        def user0_skill1_t2 = skillsService.getSkillSummary(users[0], p2.projectId, p2subj1.subjectId)
        def user1_skill1_t2 = skillsService.getSkillSummary(users[1], p2.projectId, p2subj1.subjectId)

        then:
        user0_skill1_t1.points == 100
        user0_skill1_t1.skillsLevel == 2
        def user0_skill1_t1_points = user0_skill1_t1.skills
        user0_skill1_t1_points[0].points == 0
        user0_skill1_t1_points[1].points == 100
        user0_skill1_t1_points[2].points == 0

        user1_skill1_t1.points == 200
        user1_skill1_t1.skillsLevel == 3
        def user1_skill1_t1_points = user1_skill1_t1.skills
        user1_skill1_t1_points[0].points == 100
        user1_skill1_t1_points[1].points == 100
        user1_skill1_t1_points[2].points == 0

        user0_skill1_t2.points == 100
        user0_skill1_t2.skillsLevel == 3
        user0_skill1_t2.skills[0].points == 100
        user0_skill1_t2.skills[1].points == 0

        user1_skill1_t2.points == 100
        user1_skill1_t2.skillsLevel == 3
        user1_skill1_t2.skills[0].points == 100
        user1_skill1_t2.skills[1].points == 0
    }

    def "export skills under a group to the catalog - then delete the original catalog - points and skills are removed from all the importing projects"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)

        def p1skillsGroup = SkillsFactory.createSkillsGroup(1, 1, 50)
        def skillsAndGroup = [p1skillsGroup, p1Skills].flatten()
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, skillsAndGroup)
        p1Skills.each {
            skillsService.assignSkillToSkillsGroup(p1skillsGroup.skillId, it)
        }
        skillsService.bulkExportSkillsToCatalog(p1.projectId, p1Skills.collect { it.skillId })

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [])

        List<String> users = getRandomUsers(3)
        skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[1].skillId], users[0])
        skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[0].skillId], users[1])
        skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[1].skillId], users[1])

        skillsService.bulkImportSkillsFromCatalogAndFinalize(p2.projectId, p2subj1.subjectId,
                p1Skills.collect { [projectId: it.projectId, skillId: it.skillId] })
        when:
        def user0_skill1_t1 = skillsService.getSkillSummary(users[0], p2.projectId, p2subj1.subjectId)
        def user1_skill1_t1 = skillsService.getSkillSummary(users[1], p2.projectId, p2subj1.subjectId)

        skillsService.deleteSkill(p1Skills[0])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        def user0_skill1_t2 = skillsService.getSkillSummary(users[0], p2.projectId, p2subj1.subjectId)
        def user1_skill1_t2 = skillsService.getSkillSummary(users[1], p2.projectId, p2subj1.subjectId)

        then:
        user0_skill1_t1.points == 100
        user0_skill1_t1.skillsLevel == 2
        def user0_skill1_t1_points = user0_skill1_t1.skills
        user0_skill1_t1_points[0].points == 0
        user0_skill1_t1_points[1].points == 100
        user0_skill1_t1_points[2].points == 0

        user1_skill1_t1.points == 200
        user1_skill1_t1.skillsLevel == 3
        def user1_skill1_t1_points = user1_skill1_t1.skills
        user1_skill1_t1_points[0].points == 100
        user1_skill1_t1_points[1].points == 100
        user1_skill1_t1_points[2].points == 0

        user0_skill1_t2.points == 100
        user0_skill1_t2.skillsLevel == 3
        user0_skill1_t2.skills[0].points == 100
        user0_skill1_t2.skills[1].points == 0

        user1_skill1_t2.points == 100
        user1_skill1_t2.skillsLevel == 3
        user1_skill1_t2.skills[0].points == 100
        user1_skill1_t2.skills[1].points == 0
    }

}

