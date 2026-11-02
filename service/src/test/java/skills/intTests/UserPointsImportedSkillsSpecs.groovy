/**
 * Copyright 2026 SkillTree
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

import static skills.intTests.utils.SkillsFactory.*

class UserPointsImportedSkillsSpecs extends DefaultIntSpec {

    def 'do not return users for disabled imported skills'() {
        List<String> randos = getRandomUsers(2)

        def project = createProject(10)
        def subject = createSubject(10, 1)
        def skill1 = createSkill(10, 1, 1, 0, 10, 0, 100)
        skillsService.createProjectAndSubjectAndSkills(project, subject, [skill1])
        skillsService.addSkill(skill1, randos[0])

        def project2 = createProject(11)
        def project2_subject = createSubject(11, 1)
        def project2_skill2 = createSkill(11, 1, 2, 0, 10, 0, 100)
        skillsService.createProjectAndSubjectAndSkills(project2, project2_subject, [project2_skill2])
        skillsService.addSkill(project2_skill2, randos[1])

        skillsService.exportSkillToCatalog(skill1.projectId, skill1.skillId)
        skillsService.importSkillFromCatalog(project2.projectId, project2_subject.subjectId, skill1.projectId, skill1.skillId)
        createService(randos[0]).initializeProjectProgress(project2.projectId)

        when:
        def results_t0 = skillsService.getProjectUsers(project2.projectId)
        def results_subject_t0 = skillsService.getSubjectUsers(project2.projectId, project2_subject.subjectId)
        def results_skill_t0 = skillsService.getSkillUsers(project2.projectId, skill1.skillId)
        skillsService.finalizeSkillsImportFromCatalog(project2.projectId)
        def results_t1 = skillsService.getProjectUsers(project2.projectId)
        def results_subject_t1 = skillsService.getSubjectUsers(project2.projectId, project2_subject.subjectId)
        def results_skill_t1 = skillsService.getSkillUsers(project2.projectId, skill1.skillId)

        then:
        results_t0.count == 1
        results_t0.totalCount == 1
        results_t0.totalPoints == 1000
        results_t0.data.size() == 1
        results_t0.data.find { it.userId == randos[1].toLowerCase() }.totalPoints == 100
        results_subject_t0.count == 1
        results_subject_t0.totalPoints == 1000
        results_subject_t0.totalCount == 1
        results_subject_t0.data.size() == 1
        results_subject_t0.data.find { it.userId == randos[1].toLowerCase() }.totalPoints == 100
        results_skill_t0.count == 0
        results_skill_t0.totalCount == 0
        !results_skill_t0.data

        results_t1.count == 2
        results_t1.totalCount == 2
        results_t1.totalPoints == 2000
        results_t1.data.size() == 2
        results_t1.data.find { it.userId == randos[1].toLowerCase() }.totalPoints == 100
        results_t1.data.find { it.userId == randos[0].toLowerCase() }.totalPoints == 100
        results_subject_t1.count == 2
        results_subject_t1.totalPoints == 2000
        results_subject_t1.totalCount == 2
        results_subject_t1.data.size() == 2
        results_subject_t1.data.find { it.userId == randos[1].toLowerCase() }.totalPoints == 100
        results_subject_t1.data.find { it.userId == randos[0].toLowerCase() }.totalPoints == 100
        results_skill_t1.count == 1
        results_skill_t1.totalPoints == 1000
        results_skill_t1.totalCount == 1
        results_skill_t1.data.size() == 1
        results_skill_t1.data.find { it.userId == randos[0].toLowerCase() }.totalPoints == 100

    }

    def 'imported-only participants are included - project and subject'() {
        List<String> users = getRandomUsers(5)

        def p1 = createProject(10)
        def p1_subj1 = createSubject(10, 1)
        def p1_skills = createSkills(5, 10, 1, 100, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1_subj1, p1_skills)

        def p2 = createProject(11)
        def p2_subj1 = createSubject(11, 1)
        def p2_skills = createSkills(6, 11, 1, 100, 1)
        skillsService.createProjectAndSubjectAndSkills(p2, p2_subj1, p2_skills[2..5])

        skillsService.exportSkillToCatalog(p1_skills[0].projectId, p1_skills[0].skillId)
        skillsService.exportSkillToCatalog(p1_skills[0].projectId, p1_skills[1].skillId)
        skillsService.importSkillFromCatalog(p2.projectId, p2_subj1.subjectId, p1_skills[0].projectId, p1_skills[0].skillId)
        skillsService.importSkillFromCatalog(p2.projectId, p2_subj1.subjectId, p1_skills[0].projectId, p1_skills[1].skillId)
        skillsService.finalizeSkillsImportFromCatalog(p2.projectId)

        // Visit-only participants remain visible alongside users with local awards.
        users[0..1].each { String user -> createService(user).initializeProjectProgress(p2.projectId) }

        skillsService.addSkill(p1_skills[0], users[0])
        skillsService.addSkill(p1_skills[1], users[0])
        skillsService.addSkill(p1_skills[0], users[1])
        skillsService.addSkill(p1_skills[0], users[2])
        skillsService.addSkill(p1_skills[1], users[2])

        skillsService.addSkill(p2_skills[2], users[2])
        skillsService.addSkill(p2_skills[2], users[3])

        when:
        Map projectUsers = skillsService.getProjectUsers(p2.projectId)
        Map projectUsersWithMaxPoints = skillsService.getProjectUsers(p2.projectId, 10, 1, "userId", true, "", 0, 49)
        Map projectUsersWithMinPoints = skillsService.getProjectUsers(p2.projectId, 10, 1, "userId", true, "", 17, 100)

        Map subjectUsers = skillsService.getSubjectUsers(p2.projectId, p2_subj1.subjectId)
        Map subjectUsersWithMaxPoints = skillsService.getSubjectUsers(p2.projectId, p2_subj1.subjectId, 10, 1, "userId", true, "", 0, 49)
        Map subjectUsersWithMinPoints = skillsService.getSubjectUsers(p2.projectId, p2_subj1.subjectId, 10, 1, "userId", true, "", 17, 100)

        then:
        projectUsers.count == 4
        projectUsers.totalCount == 4
        projectUsers.totalPoints == 600
        projectUsers.data.collectEntries { [it.userId, it.totalPoints] } == [
                (users[0]): 200, (users[1]): 100, (users[2]): 300, (users[3]): 100,
        ]
        projectUsersWithMaxPoints.count == 3
        projectUsersWithMaxPoints.totalCount == 3
        projectUsersWithMaxPoints.data.collectEntries { [it.userId, it.totalPoints] } == [
                (users[0]): 200, (users[1]): 100, (users[3]): 100,
        ]
        projectUsersWithMinPoints.count == 2
        projectUsersWithMinPoints.totalCount == 2
        projectUsersWithMinPoints.data.collectEntries { [it.userId, it.totalPoints] } == [(users[0]): 200, (users[2]): 300]

        subjectUsers.count == 4
        subjectUsers.totalCount == 4
        subjectUsers.totalPoints == 600
        subjectUsers.data.collectEntries { [it.userId, it.totalPoints] } == [
                (users[0]): 200, (users[1]): 100, (users[2]): 300, (users[3]): 100,
        ]
        subjectUsersWithMaxPoints.count == 3
        subjectUsersWithMaxPoints.totalCount == 3
        subjectUsersWithMaxPoints.data.collectEntries { [it.userId, it.totalPoints] } == [
                (users[0]): 200, (users[1]): 100, (users[3]): 100,
        ]
        subjectUsersWithMinPoints.count == 2
        subjectUsersWithMinPoints.totalCount == 2
        subjectUsersWithMinPoints.data.collectEntries { [it.userId, it.totalPoints] } == [(users[0]): 200, (users[2]): 300]
    }

    def 'imported-only participants are included - subject'() {
        List<String> users = getRandomUsers(5)

        def p1 = createProject(10)
        def p1_subj1 = createSubject(10, 1)
        def p1_skills = createSkills(5, 10, 1, 100, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1_subj1, p1_skills)

        def p2 = createProject(11)
        def p2_subj1 = createSubject(11, 1)
        def p2_subj2 = createSubject(11, 2)
        def p2_skills = createSkills(7, 11, 2, 100, 1)
        skillsService.createProjectAndSubjectAndSkills(p2, p2_subj2, p2_skills[3..6])
        skillsService.createSubject(p2_subj1)

        skillsService.exportSkillToCatalog(p1_skills[0].projectId, p1_skills[0].skillId)
        skillsService.exportSkillToCatalog(p1_skills[0].projectId, p1_skills[1].skillId)
        skillsService.exportSkillToCatalog(p1_skills[0].projectId, p1_skills[2].skillId)
        skillsService.importSkillFromCatalog(p2.projectId, p2_subj1.subjectId, p1_skills[0].projectId, p1_skills[0].skillId)
        skillsService.importSkillFromCatalog(p2.projectId, p2_subj1.subjectId, p1_skills[0].projectId, p1_skills[1].skillId)
        skillsService.importSkillFromCatalog(p2.projectId, p2_subj1.subjectId, p1_skills[0].projectId, p1_skills[2].skillId)
        skillsService.finalizeSkillsImportFromCatalog(p2.projectId)

        createService(users[1]).initializeProjectProgress(p2.projectId)

        assert skillsService.addSkill(p1_skills[0], users[0]).body.skillApplied
        assert skillsService.addSkill(p1_skills[1], users[0]).body.skillApplied
        assert skillsService.addSkill(p1_skills[0], users[1]).body.skillApplied
        assert skillsService.addSkill(p1_skills[0], users[2]).body.skillApplied
        assert skillsService.addSkill(p1_skills[1], users[2]).body.skillApplied
        assert skillsService.addSkill(p1_skills[2], users[2]).body.skillApplied
        assert skillsService.addSkill(p1_skills[3], users[3]).body.skillApplied // not imported

        assert skillsService.addSkill(p2_skills[3], users[0]).body.skillApplied
        assert skillsService.addSkill(p2_skills[3], users[2]).body.skillApplied
        assert skillsService.addSkill(p2_skills[3], users[3]).body.skillApplied

        when:
        Map subjectUsers = skillsService.getSubjectUsers(p2.projectId, p2_subj1.subjectId)
        Map subjectUsersWithMaxPoints = skillsService.getSubjectUsers(p2.projectId, p2_subj1.subjectId, 10, 1, "userId", true, "", 0, 99)
        Map subjectUsersWithMinPoints = skillsService.getSubjectUsers(p2.projectId, p2_subj1.subjectId, 10, 1, "userId", true, "", 67, 100)

        then:
        subjectUsers.count == 3
        subjectUsers.totalCount == 3
        subjectUsers.totalPoints == 300
        subjectUsers.data.collectEntries { [it.userId, it.totalPoints] } == [(users[0]): 200, (users[1]): 100, (users[2]): 300]
        subjectUsersWithMaxPoints.count == 2
        subjectUsersWithMaxPoints.totalCount == 2
        subjectUsersWithMaxPoints.data.collectEntries { [it.userId, it.totalPoints] } == [(users[0]): 200, (users[1]): 100]
        subjectUsersWithMinPoints.count == 1
        subjectUsersWithMinPoints.totalCount == 1
        subjectUsersWithMinPoints.data.collectEntries { [it.userId, it.totalPoints] } == [(users[2]): 300]
    }

    def 'imported-only participants are included - single skill'() {
        List<String> users = getRandomUsers(5)

        def p1 = createProject(10)
        def p1_subj1 = createSubject(10, 1)
        def p1_skills = createSkills(5, 10, 1, 100, 5)
        skillsService.createProjectAndSubjectAndSkills(p1, p1_subj1, p1_skills)

        def p2 = createProject(11)
        def p2_subj1 = createSubject(11, 1)
        def p2_skills = createSkills(6, 11, 1, 100, 1)
        skillsService.createProjectAndSubjectAndSkills(p2, p2_subj1, p2_skills[2..5])

        skillsService.exportSkillToCatalog(p1_skills[0].projectId, p1_skills[0].skillId)
        skillsService.exportSkillToCatalog(p1_skills[0].projectId, p1_skills[1].skillId)
        skillsService.importSkillFromCatalog(p2.projectId, p2_subj1.subjectId, p1_skills[0].projectId, p1_skills[0].skillId)
        skillsService.importSkillFromCatalog(p2.projectId, p2_subj1.subjectId, p1_skills[0].projectId, p1_skills[1].skillId)
        skillsService.finalizeSkillsImportFromCatalog(p2.projectId)

        createService(users[1]).initializeProjectProgress(p2.projectId)

        skillsService.addSkill(p1_skills[0], users[0], new Date() - 2)
        skillsService.addSkill(p1_skills[0], users[0], new Date() - 1)
        skillsService.addSkill(p1_skills[0], users[0], new Date())

        skillsService.addSkill(p1_skills[1], users[0])
        skillsService.addSkill(p1_skills[0], users[1], new Date() - 1)
        skillsService.addSkill(p1_skills[0], users[1], new Date())

        skillsService.addSkill(p1_skills[0], users[2])
        skillsService.addSkill(p1_skills[1], users[2])

        skillsService.addSkill(p2_skills[2], users[0])
        skillsService.addSkill(p2_skills[2], users[2])
        skillsService.addSkill(p2_skills[2], users[3])

        when:
        Map skillUsers = skillsService.getSkillUsers(p2.projectId, p1_skills[0].skillId)
        Map skillUsersWithMaxPoints = skillsService.getSkillUsers(p2.projectId, p1_skills[0].skillId, 10, 1, "userId", true, "", 0, 49)
        Map skillUsersWithMinPoints = skillsService.getSkillUsers(p2.projectId, p1_skills[0].skillId, 10, 1, "userId", true, "", 21, 100)

        then:
        skillUsers.count == 3
        skillUsers.totalCount == 3
        skillUsers.totalPoints == 500
        skillUsers.data.collectEntries { [it.userId, it.totalPoints] } == [(users[0]): 300, (users[1]): 200, (users[2]): 100]
        skillUsersWithMaxPoints.count == 2
        skillUsersWithMaxPoints.totalCount == 2
        skillUsersWithMaxPoints.data.collectEntries { [it.userId, it.totalPoints] } == [(users[1]): 200, (users[2]): 100]
        skillUsersWithMinPoints.count == 2
        skillUsersWithMinPoints.totalCount == 2
        skillUsersWithMinPoints.data.collectEntries { [it.userId, it.totalPoints] } == [(users[0]): 300, (users[1]): 200]
    }

    def 'imported-only participants are included - badge'() {
        List<String> users = getRandomUsers(5)

        def p1 = createProject(10)
        def p1_subj1 = createSubject(10, 1)
        def p1_skills = createSkills(5, 10, 1, 100, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1_subj1, p1_skills)

        def p2 = createProject(11)
        def p2_subj1 = createSubject(11, 1)
        def p2_subj2 = createSubject(11, 2)
        def p2_skills = createSkills(7, 11, 2, 100, 1)
        skillsService.createProjectAndSubjectAndSkills(p2, p2_subj2, p2_skills[3..6])
        skillsService.createSubject(p2_subj1)

        skillsService.exportSkillToCatalog(p1_skills[0].projectId, p1_skills[0].skillId)
        skillsService.exportSkillToCatalog(p1_skills[0].projectId, p1_skills[1].skillId)
        skillsService.exportSkillToCatalog(p1_skills[0].projectId, p1_skills[2].skillId)
        skillsService.importSkillFromCatalog(p2.projectId, p2_subj1.subjectId, p1_skills[0].projectId, p1_skills[0].skillId)
        skillsService.importSkillFromCatalog(p2.projectId, p2_subj1.subjectId, p1_skills[0].projectId, p1_skills[1].skillId)
        skillsService.importSkillFromCatalog(p2.projectId, p2_subj1.subjectId, p1_skills[0].projectId, p1_skills[2].skillId)
        skillsService.finalizeSkillsImportFromCatalog(p2.projectId)

        createService(users[1]).initializeProjectProgress(p2.projectId)

        String badge1Id = 'badge1'
        skillsService.addBadge([projectId: p2.projectId, badgeId: badge1Id, name: 'Badge 1'])
        [ p1_skills[0..2], p2_skills[3..5]].flatten().each {
            skillsService.assignSkillToBadge([projectId: p2_skills[0].projectId, badgeId: badge1Id, skillId: it.skillId])
        }

        assert skillsService.addSkill(p1_skills[0], users[0]).body.skillApplied
        assert skillsService.addSkill(p1_skills[1], users[0]).body.skillApplied
        assert skillsService.addSkill(p1_skills[0], users[1]).body.skillApplied
        assert skillsService.addSkill(p1_skills[0], users[2]).body.skillApplied
        assert skillsService.addSkill(p1_skills[1], users[2]).body.skillApplied
        assert skillsService.addSkill(p1_skills[2], users[2]).body.skillApplied
        assert skillsService.addSkill(p1_skills[3], users[3]).body.skillApplied // not imported

        assert skillsService.addSkill(p2_skills[3], users[0]).body.skillApplied
        assert skillsService.addSkill(p2_skills[3], users[2]).body.skillApplied
        assert skillsService.addSkill(p2_skills[6], users[3]).body.skillApplied // not part of the badge

        when:
        Map badgeUsers = skillsService.getBadgeUsers(p2.projectId, badge1Id)
        Map badgeUsersWithMaxPoints = skillsService.getBadgeUsers(p2.projectId, badge1Id, 10, 1, "userId", true, "", 0, 65)
        Map badgeUsersWithMinPoints = skillsService.getBadgeUsers(p2.projectId, badge1Id, 10, 1, "userId", true, "", 51, 100)

        then:
        badgeUsers.count == 3
        badgeUsers.totalCount == 3
        badgeUsers.totalPoints == 600
        badgeUsers.data.collectEntries { [it.userId, it.totalPoints] } == [(users[0]): 300, (users[1]): 100, (users[2]): 400]
        badgeUsersWithMaxPoints.count == 2
        badgeUsersWithMaxPoints.totalCount == 2
        badgeUsersWithMaxPoints.data.collectEntries { [it.userId, it.totalPoints] } == [(users[0]): 300, (users[1]): 100]
        badgeUsersWithMinPoints.count == 1
        badgeUsersWithMinPoints.totalCount == 1
        badgeUsersWithMinPoints.data.collectEntries { [it.userId, it.totalPoints] } == [(users[2]): 400]
    }

    def 'user lists preserve paging, user filters and percentage bounds with imported-only participants'() {
        List<String> users = getRandomUsers(5)
        Map sourceProject = createProject(10)
        Map sourceSubject = createSubject(10, 1)
        Map sourceSkill = createSkill(10, 1, 10, 0, 2, 0, 100)
        skillsService.createProjectAndSubjectAndSkills(sourceProject, sourceSubject, [sourceSkill])
        Map project = createProject(11)
        Map subject = createSubject(11, 1)
        Map group = createSkillsGroup(11, 1, 50)
        Map nativeSkill = createSkill(11, 1, 20, 0, 1, 0, 100)
        skillsService.createProjectAndSubjectAndSkills(project, subject, [group])
        skillsService.assignSkillToSkillsGroup(group.skillId, nativeSkill)
        skillsService.exportSkillToCatalog(sourceProject.projectId, sourceSkill.skillId)
        skillsService.bulkImportSkillsIntoGroupFromCatalog(project.projectId, subject.subjectId, group.skillId, [sourceSkill])
        skillsService.finalizeSkillsImportFromCatalog(project.projectId)
        Map badge = createBadge(11, 1)
        skillsService.addBadge(badge)
        [sourceSkill, nativeSkill].each { Map skill ->
            skillsService.assignSkillToBadge([projectId: project.projectId, badgeId: badge.badgeId, skillId: skill.skillId])
        }
        String tagId = 'filtered'
        skillsService.addTagToSkills(project.projectId, [sourceSkill.skillId, nativeSkill.skillId], 'Filtered', tagId)

        // These participants have no destination events, only an authorized visit and imported credit.
        [users[0], users[1], users[4]].each { String user -> createService(user).initializeProjectProgress(project.projectId) }
        skillsService.addSkill(sourceSkill, users[0], new Date() - 1)
        [users[1], users[3]].each { String user ->
            skillsService.addSkill(sourceSkill, user, new Date() - 1)
            skillsService.addSkill(sourceSkill, user)
        }
        skillsService.addSkill(sourceSkill, users[4])
        skillsService.addSkill(nativeSkill, users[2])
        skillsService.addSkill(nativeSkill, users[3])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        skillsService.archiveUsers([users[4]], project.projectId)
        createRootSkillService().saveUserTag(users[0], 'dutyOrganization', ['filter-team'])

        Map<String, Closure> lists = [
                project: { List args -> skillsService.getProjectUsers(project.projectId, *args) },
                subject: { List args -> skillsService.getSubjectUsers(project.projectId, subject.subjectId, *args) },
                skill: { List args -> skillsService.getSkillUsers(project.projectId, sourceSkill.skillId, *args) },
                group: { List args -> skillsService.getSkillGroupUsers(project.projectId, group.skillId, *args) },
                badge: { List args -> skillsService.getBadgeUsers(project.projectId, badge.badgeId, *args) },
                tag: { List args -> skillsService.getSkillTagUsers(project.projectId, tagId, *args) },
        ]

        expect:
        lists.each { String view, Closure load ->
            int availablePoints = view == 'skill' ? 200 : 300
            Map<String, Integer> expected = view == 'skill' ? [
                    (users[0]): 100, (users[1]): 200, (users[3]): 200,
            ] : [(users[0]): 100, (users[1]): 200, (users[2]): 100, (users[3]): 300]
            List<String> sortedUsers = expected.keySet().sort()
            Map all = load([10, 1, 'userId', true, '', 0, 100, ''])
            assert all.count == expected.size()
            assert all.totalCount == expected.size()
            assert all.totalPoints == availablePoints
            assert all.data.collectEntries { [it.userId, it.totalPoints] } == expected
            if (view == 'group') {
                assert all.data.collectEntries { [it.userId, it.skillsAchieved] } == [
                        (users[0]): 0, (users[1]): 1, (users[2]): 1, (users[3]): 2,
                ]
            }
            // A full first page forces Spring Data's count query, including the group's explicit count SQL.
            [1, 2].each { int page ->
                Map paged = load([1, page, 'userId', true, '', 0, 100, ''])
                assert paged.count == expected.size()
                assert paged.totalCount == expected.size()
                assert paged.data.size() == 1
                assert paged.data.userId == [sortedUsers[page - 1]]
                assert paged.data[0].totalPoints == expected[sortedUsers[page - 1]]
            }
            Map descending = load([1, 1, 'userId', false, '', 0, 100, ''])
            assert descending.count == expected.size()
            assert descending.totalCount == expected.size()
            assert descending.data.collectEntries { [it.userId, it.totalPoints] } == [(sortedUsers.last()): expected[sortedUsers.last()]]
            Map queried = load([10, 1, 'userId', true, users[0], 0, 100, ''])
            assert queried.count == 1
            assert queried.totalCount == 1
            assert queried.data.collectEntries { [it.userId, it.totalPoints] } == [(users[0]): 100]
            Map tagged = load([10, 1, 'userId', true, '', 0, 100, 'FILTER-TEAM'])
            assert tagged.count == 1
            assert tagged.totalCount == 1
            assert tagged.data.collectEntries { [it.userId, it.totalPoints] } == [(users[0]): 100]
            // Upper bounds are exclusive below 100%; full completion remains included at 100%.
            Map belowHalf = load([10, 1, 'userId', true, '', 0, 50, ''])
            Map<String, Integer> expectedBelowHalf = view == 'skill' ? [:] : [(users[0]): 100, (users[2]): 100]
            assert belowHalf.count == expectedBelowHalf.size()
            assert belowHalf.totalCount == expectedBelowHalf.size()
            assert belowHalf.data.collectEntries { [it.userId, it.totalPoints] } == expectedBelowHalf
            Map aboveTwoThirds = load([10, 1, 'userId', true, '', 67, 100, ''])
            Map<String, Integer> expectedAbove = view == 'skill' ? [(users[1]): 200, (users[3]): 200] : [(users[3]): 300]
            assert aboveTwoThirds.count == expectedAbove.size()
            assert aboveTwoThirds.totalCount == expectedAbove.size()
            assert aboveTwoThirds.totalPoints == availablePoints
            assert aboveTwoThirds.data.collectEntries { [it.userId, it.totalPoints] } == expectedAbove
        }

    }
}
