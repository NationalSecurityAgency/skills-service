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

    def 'ability to exclude users that only earned points in imported skills - project and subject'() {
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

        skillsService.addSkill(p1_skills[0], users[0])
        skillsService.addSkill(p1_skills[1], users[0])
        skillsService.addSkill(p1_skills[0], users[1])
        skillsService.addSkill(p1_skills[0], users[2])
        skillsService.addSkill(p1_skills[1], users[2])

        skillsService.addSkill(p2_skills[2], users[2])
        skillsService.addSkill(p2_skills[2], users[3])

        when:
        def p2UsersIncludedImported = skillsService.getProjectUsers(p2.projectId, 10, 1, "userId", true, "", 0, 100, "", true)
        def p1UsersExcludedImported = skillsService.getProjectUsers(p2.projectId, 10, 1, "userId", true, "", 0, 100, "", false)
        def p1UsersExcludedImportedWithMaxPoints = skillsService.getProjectUsers(p2.projectId, 10, 1, "userId", true, "", 0, 49, "", false)
        def p1UsersExcludedImportedWithMinPoints = skillsService.getProjectUsers(p2.projectId, 10, 1, "userId", true, "", 17, 100, "", false)

        def p2SubjUsersIncludedImported = skillsService.getSubjectUsers(p2.projectId,  p2_subj1.subjectId, 10, 1, "userId", true, "", 0, 100, "", true)
        def p2SubjUsersExcludedImported = skillsService.getSubjectUsers(p2.projectId,  p2_subj1.subjectId, 10, 1, "userId", true, "", 0, 100, "", false)
        def p2SubjUsersExcludedImportedWithMaxPoints = skillsService.getSubjectUsers(p2.projectId,  p2_subj1.subjectId, 10, 1, "userId", true, "", 0, 49, "", false)
        def p2SubjUsersExcludedImportedWithMinPoints = skillsService.getSubjectUsers(p2.projectId,  p2_subj1.subjectId, 10, 1, "userId", true, "", 17, 100, "", false)

        then:
        p2UsersIncludedImported.data.userId.sort() == users[0..3].sort()
        p2UsersIncludedImported.data.find { users[0].equalsIgnoreCase(it.userId) }.totalPoints == 200
        p2UsersIncludedImported.data.find { users[1].equalsIgnoreCase(it.userId) }.totalPoints == 100
        p2UsersIncludedImported.data.find { users[2].equalsIgnoreCase(it.userId) }.totalPoints == 300
        p2UsersIncludedImported.data.find { users[3].equalsIgnoreCase(it.userId) }.totalPoints == 100

        p1UsersExcludedImported.data.userId.sort() == users[2..3].sort()
        p1UsersExcludedImported.data.find { users[2].equalsIgnoreCase(it.userId) }.totalPoints == 300
        p1UsersExcludedImported.data.find { users[3].equalsIgnoreCase(it.userId) }.totalPoints == 100

        p1UsersExcludedImportedWithMaxPoints.data.userId.sort() == users[3..3].sort()
        p1UsersExcludedImportedWithMaxPoints.data.find { users[3].equalsIgnoreCase(it.userId) }.totalPoints == 100

        p1UsersExcludedImportedWithMinPoints.data.userId.sort() == users[2..2].sort()
        p1UsersExcludedImportedWithMinPoints.data.find { users[2].equalsIgnoreCase(it.userId) }.totalPoints == 300

        // subject
        p2SubjUsersIncludedImported.data.userId.sort() == users[0..3].sort()
        p2SubjUsersIncludedImported.data.find { users[0].equalsIgnoreCase(it.userId) }.totalPoints == 200
        p2SubjUsersIncludedImported.data.find { users[1].equalsIgnoreCase(it.userId) }.totalPoints == 100
        p2SubjUsersIncludedImported.data.find { users[2].equalsIgnoreCase(it.userId) }.totalPoints == 300
        p2SubjUsersIncludedImported.data.find { users[3].equalsIgnoreCase(it.userId) }.totalPoints == 100

        p2SubjUsersExcludedImported.data.userId.sort() == users[2..3].sort()
        p2SubjUsersExcludedImported.data.find { users[2].equalsIgnoreCase(it.userId) }.totalPoints == 300
        p2SubjUsersExcludedImported.data.find { users[3].equalsIgnoreCase(it.userId) }.totalPoints == 100

        p2SubjUsersExcludedImportedWithMaxPoints.data.userId.sort() == users[3..3].sort()
        p2SubjUsersExcludedImportedWithMaxPoints.data.find { users[3].equalsIgnoreCase(it.userId) }.totalPoints == 100

        p2SubjUsersExcludedImportedWithMinPoints.data.userId.sort() == users[2..2].sort()
        p2SubjUsersExcludedImportedWithMinPoints.data.find { users[2].equalsIgnoreCase(it.userId) }.totalPoints == 300
    }

    def 'ability to exclude users that only earned points in imported skills - subject'() {
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
        def p2SubjUsersIncludedImported = skillsService.getSubjectUsers(p2.projectId,  p2_subj1.subjectId, 10, 1, "userId", true, "", 0, 100, "", true)
        def p2SubjUsersExcludedImported = skillsService.getSubjectUsers(p2.projectId,  p2_subj1.subjectId, 10, 1, "userId", true, "", 0, 100, "", false)
        def p2SubjUsersExcludedImportedWithMaxPoints = skillsService.getSubjectUsers(p2.projectId,  p2_subj1.subjectId, 10, 1, "userId", true, "", 0, 99, "", false)
        def p2SubjUsersExcludedImportedWithMinPoints = skillsService.getSubjectUsers(p2.projectId,  p2_subj1.subjectId, 10, 1, "userId", true, "", 67, 100, "", false)

        then:
        p2SubjUsersIncludedImported.data.userId.sort() == users[0..2].sort()
        p2SubjUsersIncludedImported.data.find { users[0].equalsIgnoreCase(it.userId) }.totalPoints == 200
        p2SubjUsersIncludedImported.data.find { users[1].equalsIgnoreCase(it.userId) }.totalPoints == 100
        p2SubjUsersIncludedImported.data.find { users[2].equalsIgnoreCase(it.userId) }.totalPoints == 300

        p2SubjUsersExcludedImported.data.userId.sort() == [users[0], users[2]].sort()
        p2SubjUsersExcludedImported.data.find { users[0].equalsIgnoreCase(it.userId) }.totalPoints == 200
        p2SubjUsersExcludedImported.data.find { users[2].equalsIgnoreCase(it.userId) }.totalPoints == 300

        p2SubjUsersExcludedImportedWithMaxPoints.data.userId.sort() == users[0..0].sort()
        p2SubjUsersExcludedImportedWithMaxPoints.data.find { users[0].equalsIgnoreCase(it.userId) }.totalPoints == 200

        p2SubjUsersExcludedImportedWithMinPoints.data.userId.sort() == users[2..2].sort()
        p2SubjUsersExcludedImportedWithMinPoints.data.find { users[2].equalsIgnoreCase(it.userId) }.totalPoints == 300
    }

    def 'ability to exclude users that only earned points in imported skills - single skill'() {
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
        def p2SkillUsersIncludedImported = skillsService.getSkillUsers(p2.projectId, p1_skills[0].skillId, 10, 1, "userId", true, "", 0, 100, "", true)
        def p2SkillUsersExcludedImported = skillsService.getSkillUsers(p2.projectId,  p1_skills[0].skillId, 10, 1, "userId", true, "", 0, 100, "", false)
        def p2SkillUsersExcludedImportedWithMaxPoints = skillsService.getSkillUsers(p2.projectId,  p1_skills[0].skillId, 10, 1, "userId", true, "", 0, 49, "", false)
        def p2SkillUsersExcludedImportedWithMinPoints = skillsService.getSkillUsers(p2.projectId,  p1_skills[0].skillId, 10, 1, "userId", true, "", 21, 100, "", false)

        then:
        p2SkillUsersIncludedImported.data.userId.sort() == users[0..2].sort()
        p2SkillUsersIncludedImported.data.find { users[0].equalsIgnoreCase(it.userId) }.totalPoints == 300
        p2SkillUsersIncludedImported.data.find { users[1].equalsIgnoreCase(it.userId) }.totalPoints == 200
        p2SkillUsersIncludedImported.data.find { users[2].equalsIgnoreCase(it.userId) }.totalPoints == 100

        p2SkillUsersExcludedImported.data.userId.sort() == [users[0], users[2]].sort()
        p2SkillUsersExcludedImported.data.find { users[0].equalsIgnoreCase(it.userId) }.totalPoints == 300
        p2SkillUsersExcludedImported.data.find { users[2].equalsIgnoreCase(it.userId) }.totalPoints == 100

        p2SkillUsersExcludedImportedWithMaxPoints.data.userId.sort() == users[2..2].sort()
        p2SkillUsersExcludedImportedWithMaxPoints.data.find { users[2].equalsIgnoreCase(it.userId) }.totalPoints == 100

        p2SkillUsersExcludedImportedWithMinPoints.data.userId.sort() == users[0..0].sort()
        p2SkillUsersExcludedImportedWithMinPoints.data.find { users[0].equalsIgnoreCase(it.userId) }.totalPoints == 300
    }

    def 'ability to exclude users that only earned points in imported skills - badge'() {
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
        def badgeUsersIncludedImported = skillsService.getBadgeUsers(p2.projectId,  badge1Id, 10, 1, "userId", true, "", 0, 100, "", true)
        def badgeUsersExcludedImported = skillsService.getBadgeUsers(p2.projectId,  badge1Id, 10, 1, "userId", true, "", 0, 100, "", false)
        def badgeUsersExcludedImportedWithMaxPoints = skillsService.getBadgeUsers(p2.projectId,  badge1Id, 10, 1, "userId", true, "", 0, 65, "", false)
        def badgeUsersExcludedImportedWithMinPoints = skillsService.getBadgeUsers(p2.projectId,  badge1Id, 10, 1, "userId", true, "", 51, 100, "", false)

        then:
        badgeUsersIncludedImported.data.userId.sort() == users[0..2].sort()
        badgeUsersIncludedImported.data.find { users[0].equalsIgnoreCase(it.userId) }.totalPoints == 300
        badgeUsersIncludedImported.data.find { users[1].equalsIgnoreCase(it.userId) }.totalPoints == 100
        badgeUsersIncludedImported.data.find { users[2].equalsIgnoreCase(it.userId) }.totalPoints == 400

        badgeUsersExcludedImported.data.userId.sort() == [users[0], users[2]].sort()
        badgeUsersExcludedImported.data.find { users[0].equalsIgnoreCase(it.userId) }.totalPoints == 300
        badgeUsersExcludedImported.data.find { users[2].equalsIgnoreCase(it.userId) }.totalPoints == 400

        badgeUsersExcludedImportedWithMaxPoints.data.userId.sort() == users[0..0].sort()
        badgeUsersExcludedImportedWithMaxPoints.data.find { users[0].equalsIgnoreCase(it.userId) }.totalPoints == 300

        badgeUsersExcludedImportedWithMinPoints.data.userId.sort() == users[2..2].sort()
        badgeUsersExcludedImportedWithMinPoints.data.find { users[2].equalsIgnoreCase(it.userId) }.totalPoints == 400
    }
}
