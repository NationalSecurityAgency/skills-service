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
package skills.intTests

import org.apache.commons.lang3.RandomStringUtils
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import spock.lang.IgnoreIf

import static skills.intTests.utils.SkillsFactory.*

class UserPointsSpecs extends DefaultIntSpec {

    String projId = SkillsFactory.defaultProjId

    List<String> sampleUserIds = ['haNson', 'haRry', 'tom', 'user4', 'user5']
    List<String> subjects
    List<List<String>> allSkillIds
    String badgeId

    Date threeDaysAgo = new Date()-3
    Date twoDaysAgo = new Date()-2
    Date yesterday = new Date() - 1
    DateTimeFormatter DTF = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZZ").withZoneUTC()

    String ultimateRoot = 'jh@dojo.com'
    SkillsService rootSkillsService
    List<String> usersWithTags

    def setup(){
        rootSkillsService = createService(ultimateRoot, 'aaaaaaaa')
        if (!rootSkillsService.isRoot()) {
            rootSkillsService.grantRoot()
        }
        usersWithTags = getRandomUsers(10)

        skillsService.deleteProjectIfExist(projId)

        subjects = ['testSubject1', 'testSubject2', 'testSubject3']
        allSkillIds = setupProjectWithSkills(subjects)
        badgeId = 'badge1'

        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], sampleUserIds.get(0), threeDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(1).get(0)], sampleUserIds.get(0), threeDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(1).get(0)], sampleUserIds.get(1), threeDaysAgo)

        skillsService.addBadge([projectId: projId, badgeId: badgeId, name: 'Badge 1'])
        skillsService.assignSkillToBadge([projectId: projId, badgeId: badgeId, skillId: allSkillIds.get(0).get(0)])
    }

    def 'recalculate user points after changing point value' () {

        skillsService.deleteProjectIfExist(projId)
        String subjectId = 'testSubject1'
        skillsService.createProject([projectId: projId, name: "Test Project"])
        skillsService.createSubject([projectId: projId, subjectId: subjectId, name: "Test Subject"])
        String skillId = addDependentSkills(projId, subjectId, 1, 1, 5).get(0)

        skillsService.addSkill(['projectId': projId, skillId: skillId], sampleUserIds.get(0), new Date()-4)
        skillsService.addSkill(['projectId': projId, skillId: skillId], sampleUserIds.get(0), threeDaysAgo)

        when:
        def resultsBefore = skillsService.getProjectUsers(projId)

        // change point incrment from 35 to 10
        def res = skillsService.getSkill([projectId: projId, subjectId: subjectId, skillId: skillId])
        String originalSkillId = res.skillId
        res.pointIncrement = 10
        res.subjectId = subjectId
        skillsService.updateSkill(res, originalSkillId)

        def resultsAfter = skillsService.getProjectUsers(projId)

        then:
        resultsBefore
        resultsBefore.data.get(0).userId.contains(sampleUserIds.get(0)?.toLowerCase())
        resultsBefore.data.get(0).totalPoints == 70

        resultsAfter
        resultsBefore.data.get(0).userId.contains(sampleUserIds.get(0)?.toLowerCase())

        // totalPoints remains 70 for H2 (see UserPointsRepo.updateUserPointsForASkillInH2())
        resultsAfter.data.get(0).totalPoints == 20
    }

    def 'get subject users returns correct firstUpdated and lastUpdated date'() {
        def subj2_skills = skillsService.getSkillsForSubject(projId, subjects.get(1))
        def subj3_skills = skillsService.getSkillsForSubject(projId, subjects.get(2))

        skillsService.addSkill(['projectId': projId, skillId: subj2_skills[0].skillId], sampleUserIds.get(1), threeDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: subj2_skills[0].skillId], sampleUserIds.get(1), yesterday)
        skillsService.addSkill(['projectId': projId, skillId: subj3_skills[0].skillId], sampleUserIds.get(1), twoDaysAgo)

        when:
        def results1 = skillsService.getSubjectUsers(projId, subjects.get(0))
        def results2 = skillsService.getSubjectUsers(projId, subjects.get(1))
        def results3 = skillsService.getSubjectUsers(projId, subjects.get(2))

        then:
        results1
        results1.count == 1
        results1.data.size() == 1
        results2
        results2.count == 2
        results2.data.size() == 2

        // lastUpdated is null for H2 (see UserPointsRepo.findDistinctProjectUsersByProjectIdAndSubjectIdAndUserIdLike)
        results1.data.sort { a, b -> b.lastUpdated <=> a.lastUpdated }.get(0).lastUpdated == DTF.print(threeDaysAgo.time)
        def result2User = results2.data.find { it -> it.userId == sampleUserIds.get(1).toLowerCase() }
        def result3User = results3.data.find { it -> it.userId == sampleUserIds.get(1).toLowerCase() }
        result2User
        result3User
        result2User.lastUpdated != result3User.lastUpdated
        result2User.firstUpdated == DTF.print(threeDaysAgo.time)
        result2User.lastUpdated == DTF.print(yesterday.time)
        result3User.firstUpdated == DTF.print(twoDaysAgo.time)
        result3User.lastUpdated == DTF.print(twoDaysAgo.time)
    }

    def 'get project users returns correct firstUpdated and lastUpdated date'() {
        def subj2_skills = skillsService.getSkillsForSubject(projId, subjects.get(1))
        def subj3_skills = skillsService.getSkillsForSubject(projId, subjects.get(2))

        skillsService.addSkill(['projectId': projId, skillId: subj2_skills[0].skillId], sampleUserIds.get(1), threeDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: subj2_skills[0].skillId], sampleUserIds.get(1), yesterday)
        skillsService.addSkill(['projectId': projId, skillId: subj3_skills[0].skillId], sampleUserIds.get(1), twoDaysAgo)

        when:
        def results = skillsService.getProjectUsers(projId)

        then:
        results
        results.count == 2
        results.data.size() == 2

        def result2User = results.data.find { it -> it.userId == sampleUserIds.get(1).toLowerCase() }
        result2User.lastUpdated == DTF.print(yesterday.time)
        result2User.firstUpdated == DTF.print(threeDaysAgo.time)
    }

    def 'get skill users returns correct firstUpdated and lastUpdated date'() {
        def subj2_skills = skillsService.getSkillsForSubject(projId, subjects.get(1))
        def subj3_skills = skillsService.getSkillsForSubject(projId, subjects.get(2))

        skillsService.addSkill(['projectId': projId, skillId: subj2_skills[0].skillId], sampleUserIds.get(1), threeDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: subj2_skills[0].skillId], sampleUserIds.get(1), yesterday)
        skillsService.addSkill(['projectId': projId, skillId: subj3_skills[0].skillId], sampleUserIds.get(1), twoDaysAgo)

        when:
        def results = skillsService.getSkillUsers(projId, subj2_skills[0].skillId)

        then:
        results
        results.count == 2
        results.data.size() == 2

        def result2User = results.data.find { it -> it.userId == sampleUserIds.get(1).toLowerCase() }
        result2User.lastUpdated == DTF.print(yesterday.time)
        result2User.firstUpdated == DTF.print(threeDaysAgo.time)
    }

    def 'get skill users returns correct firstUpdated and lastUpdated date for imported skill'() {
        def subj1_skills = skillsService.getSkillsForSubject(projId, subjects.get(0))

        def project2 = SkillsFactory.createProject(11)
        def project2_subject = SkillsFactory.createSubject(11, 1)
        def project2_skill2 = SkillsFactory.createSkill(11, 1, 2, 0, 10, 0, 100)
        skillsService.createProjectAndSubjectAndSkills(project2, project2_subject, [project2_skill2])

        skillsService.exportSkillToCatalog(projId, subj1_skills[0].skillId)
        skillsService.importSkillFromCatalog(project2.projectId, project2_subject.subjectId, subj1_skills[0].projectId, subj1_skills[0].skillId)
        skillsService.finalizeSkillsImportFromCatalog(project2.projectId)

        when:
        def results = skillsService.getSkillUsers(project2.projectId, subj1_skills[0].skillId)

        then:
        results
        results.count == 1
        results.data.size() == 1

        def result2User = results.data.find { it -> it.userId == sampleUserIds.get(0).toLowerCase() }
        result2User.lastUpdated == DTF.print(threeDaysAgo.time)
        result2User.firstUpdated == DTF.print(threeDaysAgo.time)
    }

    def 'get badge users returns correct firstUpdated and lastUpdated date'() {
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], sampleUserIds.get(0), yesterday)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], sampleUserIds.get(1), threeDaysAgo)

        when:
        def results = skillsService.getBadgeUsers(projId, badgeId)

        then:
        results
        results.count == 2
        results.data.size() == 2

        def resultUser = results.data.find { it -> it.userId == sampleUserIds.get(0).toLowerCase() }
        resultUser.lastUpdated == DTF.print(yesterday.time)
        resultUser.firstUpdated == DTF.print(threeDaysAgo.time)

        def result2User = results.data.find { it -> it.userId == sampleUserIds.get(1).toLowerCase() }
        result2User.lastUpdated == DTF.print(threeDaysAgo.time)
        result2User.firstUpdated == DTF.print(threeDaysAgo.time)
    }



    def 'get project users when project exists'() {
        when:
        def results = skillsService.getProjectUsers(projId)

        then:
        results
        results.totalPoints == 9 * 35 * 4
        results.count == 2
        results.totalCount == 2
        results.data.size() == 2
        results.data.get(0).userId.contains(sampleUserIds.get(0)?.toLowerCase())
        results.data.get(0).totalPoints == 70
        results.data.get(1).userId.contains(sampleUserIds.get(1)?.toLowerCase())
        results.data.get(1).totalPoints == 35
        results.data.sort {a,b -> b.lastUpdated <=> a.lastUpdated }.get(0).lastUpdated == DTF.print(threeDaysAgo.time)
    }

    def 'do not return users for disabled imported skills'() {
        List<String> randos = getRandomUsers(2)

        def project = SkillsFactory.createProject(10)
        def subject = SkillsFactory.createSubject(10, 1)
        def skill1 = SkillsFactory.createSkill(10, 1, 1, 0, 10, 0, 100)
        skillsService.createProjectAndSubjectAndSkills(project, subject, [skill1])
        skillsService.addSkill(skill1, randos[0])

        def project2 = SkillsFactory.createProject(11)
        def project2_subject = SkillsFactory.createSubject(11, 1)
        def project2_skill2 = SkillsFactory.createSkill(11, 1, 2, 0, 10, 0, 100)
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

    def 'get project users with paging'() {
        when:
        def results1 = skillsService.getProjectUsers(projId, 1, 1)
        def results2 = skillsService.getProjectUsers(projId, 1, 2)

        then:
        results1
        results1.count == 2
        results1.totalCount == 2
        results1.data.size() == 1
        results1.data.get(0).userId.contains(sampleUserIds.get(0)?.toLowerCase())
        results1.data.get(0).totalPoints == 70
        results2
        results2.count == 2
        results2.totalCount == 2
        results2.data.size() == 1
        results2.data.get(0).userId.contains(sampleUserIds.get(1)?.toLowerCase())
        results2.data.get(0).totalPoints == 35
    }

    def 'get project users with paging and query'() {
        when:
        def results1 = skillsService.getProjectUsers(projId, 1, 1, "userId", true, "h")
        def results2 = skillsService.getProjectUsers(projId, 1, 2, "userId", true, "h")

        then:
        results1
        results1.count == 2 // result count
        results1.totalCount == 2  // total user count
        results1.data.size() == 1
        results1.data.get(0).userId.contains(sampleUserIds.get(0)?.toLowerCase())
        results1.data.get(0).totalPoints == 70
        results2
        results2.count == 2
        results2.totalCount == 2
        results2.data.size() == 1
        results2.data.get(0).userId.contains(sampleUserIds.get(1)?.toLowerCase())
        results2.data.get(0).totalPoints == 35
    }

    def 'get project users works appropriately with no maximum points'() {
        when:
        def result = skillsService.wsHelper.adminGet("/projects/${projId}/users?limit=10&ascending=1&page=1&byColumn=0&orderBy=userId&query=&minimumPoints=0".toString())

        then:
        result
        result.count == 2
        result.totalCount == 2  // total user count
        result.data.size() == 2
        result.data.get(0).userId.contains(sampleUserIds.get(0)?.toLowerCase())
        result.data.get(0).totalPoints == 70
        result.data.get(1).userId.contains(sampleUserIds.get(1)?.toLowerCase())
        result.data.get(1).totalPoints == 35
    }

    def 'get project users with paging and minimum points'() {
        when:
        def results1 = skillsService.getProjectUsers(projId, 10, 1, "userId", true, "", 0)
        def results2 = skillsService.getProjectUsers(projId, 10, 1, "userId", true, "", 5)

        then:
        results1
        results1.count == 2 // result count
        results1.totalCount == 2  // total user count
        results1.data.size() == 2
        results1.data.get(0).userId.contains(sampleUserIds.get(0)?.toLowerCase())
        results1.data.get(0).totalPoints == 70
        results1.data.get(1).userId.contains(sampleUserIds.get(1)?.toLowerCase())
        results1.data.get(1).totalPoints == 35
        results2
        results2.count == 1
        results2.totalCount == 2
        results2.data.size() == 1
        results2.data.get(0).userId.contains(sampleUserIds.get(0)?.toLowerCase())
        results2.data.get(0).totalPoints == 70
    }


    def 'get project users with paging and maximum points'() {
        when:
        def results1 = skillsService.getProjectUsers(projId, 10, 1, "userId", true, "", 0, 100)
        def results2 = skillsService.getProjectUsers(projId, 10, 1, "userId", true, "", 0, 4)

        then:
        results1
        results1.count == 2 // result count
        results1.totalCount == 2  // total user count
        results1.data.size() == 2
        results1.data.get(0).userId.contains(sampleUserIds.get(0)?.toLowerCase())
        results1.data.get(0).totalPoints == 70
        results1.data.get(1).userId.contains(sampleUserIds.get(1)?.toLowerCase())
        results1.data.get(1).totalPoints == 35
        results2
        results2.count == 1
        results2.totalCount == 2
        results2.data.size() == 1
        results2.data.get(0).userId.contains(sampleUserIds.get(1)?.toLowerCase())
        results2.data.get(0).totalPoints == 35
    }

    def 'get project users with paging and minimum and maximum points'() {
        when:
        def results1 = skillsService.getProjectUsers(projId, 10, 1, "userId", true, "", 0, 100)
        def results2 = skillsService.getProjectUsers(projId, 10, 1, "userId", true, "", 5, 80)

        then:
        results1
        results1.count == 2 // result count
        results1.totalCount == 2  // total user count
        results1.data.size() == 2
        results1.data.get(0).userId.contains(sampleUserIds.get(0)?.toLowerCase())
        results1.data.get(0).totalPoints == 70
        results1.data.get(1).userId.contains(sampleUserIds.get(1)?.toLowerCase())
        results1.data.get(1).totalPoints == 35
        results2
        results2.count == 1
        results2.totalCount == 2
        results2.data.size() == 1
        results2.data.get(0).userId.contains(sampleUserIds.get(0)?.toLowerCase())
        results2.data.get(0).totalPoints == 70
    }

    def 'get subject users when project exists'() {
        when:
        def results1 = skillsService.getSubjectUsers(projId, subjects.get(0))
        def results2 = skillsService.getSubjectUsers(projId, subjects.get(1))
        def results3 = skillsService.getSubjectUsers(projId, subjects.get(2))

        then:
        results1
        results1.count == 1
        results1.totalCount == 1
        results1.data.size() == 1
        results1.data.get(0).userId.contains(sampleUserIds.get(0)?.toLowerCase())
        results1.data.get(0).totalPoints == 35
        results1.data.sort {a,b -> b.lastUpdated <=> a.lastUpdated }.get(0).lastUpdated == DTF.print(threeDaysAgo.time)

        results2
        results2.count == 2
        results2.totalCount == 2
        results2.data.size() == 2
        results2.data.get(0).userId.contains(sampleUserIds.get(0)?.toLowerCase())
        results2.data.get(0).totalPoints == 35
        results2.data.get(1).userId.contains(sampleUserIds.get(1)?.toLowerCase())
        results2.data.get(1).totalPoints == 35
        results2.data.sort {a,b -> b.lastUpdated <=> a.lastUpdated }.get(0).lastUpdated == DTF.print(threeDaysAgo.time)

        results3
        results3.count == 0
        results3.totalCount == 0
        !results3.data
    }

    def 'get subject users with minimum points'() {
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(1).get(0)], sampleUserIds.get(2), threeDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(1).get(0)], sampleUserIds.get(3), threeDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(1).get(0)], sampleUserIds.get(4), threeDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(1).get(0)], sampleUserIds.get(2), twoDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(1).get(0)], sampleUserIds.get(3), twoDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(1).get(0)], sampleUserIds.get(4), twoDaysAgo)

        when:
        def results1 = skillsService.getSubjectUsers(projId, subjects.get(1), 10, 1, "userId", true, "", 0)
        def results2 = skillsService.getSubjectUsers(projId, subjects.get(1), 10, 1, "userId", true, "", 10)
        def results3 = skillsService.getSubjectUsers(projId, subjects.get(1), 10, 1, "userId", true, "", 25)

        then:
        results1
        results1.count == 5
        results1.totalCount == 5
        results1.data.size() == 5
        results1.data.get(0).userId.contains(sampleUserIds.get(0)?.toLowerCase())
        results1.data.get(0).totalPoints == 35
        results1.data.get(1).userId.contains(sampleUserIds.get(1)?.toLowerCase())
        results1.data.get(1).totalPoints == 35

        results2
        results2.count == 3
        results2.totalCount == 5
        results2.data.size() == 3
        results2.data.get(0).totalPoints == 70

        results3
        results3.count == 0
        results3.totalCount == 5
        results3.data.size() == 0
    }

    def 'get subject users with maximum points'() {
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(1).get(0)], sampleUserIds.get(2), threeDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(1).get(0)], sampleUserIds.get(3), threeDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(1).get(0)], sampleUserIds.get(4), threeDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(1).get(0)], sampleUserIds.get(2), twoDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(1).get(0)], sampleUserIds.get(3), twoDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(1).get(0)], sampleUserIds.get(4), twoDaysAgo)

        when:
        def results1 = skillsService.getSubjectUsers(projId, subjects.get(1), 10, 1, "userId", true, "", 0, 100)
        def results2 = skillsService.getSubjectUsers(projId, subjects.get(1), 10, 1, "userId", true, "", 0, 10)
        def results3 = skillsService.getSubjectUsers(projId, subjects.get(1), 10, 1, "userId", true, "", 0, 5)

        then:
        results1
        results1.count == 5
        results1.totalCount == 5
        results1.data.size() == 5
        results1.data.get(0).userId.contains(sampleUserIds.get(0)?.toLowerCase())
        results1.data.get(0).totalPoints == 35
        results1.data.get(1).userId.contains(sampleUserIds.get(1)?.toLowerCase())
        results1.data.get(1).totalPoints == 35

        results2
        results2.count == 2
        results2.totalCount == 5
        results2.data.size() == 2
        results2.data.get(0).totalPoints == 35

        results3
        results3.count == 0
        results3.totalCount == 5
        results3.data.size() == 0
    }

    def 'get subject users with minimum and maximum points'() {
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(1).get(0)], sampleUserIds.get(2), threeDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(1).get(0)], sampleUserIds.get(3), threeDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(1).get(0)], sampleUserIds.get(4), threeDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(1).get(0)], sampleUserIds.get(2), twoDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(1).get(0)], sampleUserIds.get(3), twoDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(1).get(0)], sampleUserIds.get(4), twoDaysAgo)

        when:
        def results1 = skillsService.getSubjectUsers(projId, subjects.get(1), 10, 1, "userId", true, "", 0, 100)
        def results2 = skillsService.getSubjectUsers(projId, subjects.get(1), 10, 1, "userId", true, "", 5, 10)
        def results3 = skillsService.getSubjectUsers(projId, subjects.get(1), 10, 1, "userId", true, "", 10, 15)

        then:
        results1
        results1.count == 5
        results1.totalCount == 5
        results1.data.size() == 5
        results1.data.get(0).userId.contains(sampleUserIds.get(0)?.toLowerCase())
        results1.data.get(0).totalPoints == 35
        results1.data.get(1).userId.contains(sampleUserIds.get(1)?.toLowerCase())
        results1.data.get(1).totalPoints == 35

        results2
        results2.count == 2
        results2.totalCount == 5
        results2.data.size() == 2
        results2.data.get(0).totalPoints == 35

        results3
        results3.count == 0
        results3.totalCount == 5
        results3.data.size() == 0
    }

    def 'get skill users when project exists'() {
        when:
        def results1 = skillsService.getSkillUsers(projId, allSkillIds.get(0).get(0))
        def results2 = skillsService.getSkillUsers(projId, allSkillIds.get(1).get(0))
        def results3 = skillsService.getSkillUsers(projId, allSkillIds.get(1).get(1))

        then:
        results1
        results1.count == 1
        results1.totalCount == 1
        results1.data.size() == 1
        results1.data.get(0).userId.contains(sampleUserIds.get(0)?.toLowerCase())
        results1.data.get(0).totalPoints == 35
        results2.data.sort {a,b -> b.lastUpdated <=> a.lastUpdated }.get(0).lastUpdated == DTF.print(threeDaysAgo.time)

        results2
        results2.count == 2
        results2.totalCount == 2
        results2.data.size() == 2
        results2.data.get(0).userId.contains(sampleUserIds.get(0)?.toLowerCase())
        results2.data.get(0).totalPoints == 35
        results2.data.get(1).userId.contains(sampleUserIds.get(1)?.toLowerCase())
        results2.data.get(1).totalPoints == 35
        results2.data.sort {a,b -> b.lastUpdated <=> a.lastUpdated }.get(0).lastUpdated == DTF.print(threeDaysAgo.time)

        results3
        results3.count == 0
        results3.totalCount == 0
        !results3.data
    }

    def 'get skill users with paging when project exists'() {
        when:
        def results1 = skillsService.getSkillUsers(projId, allSkillIds.get(0).get(0), 1, 1, "userId", true, "h")
        def results2 = skillsService.getSkillUsers(projId, allSkillIds.get(1).get(0), 1, 1, "userId", true, "h")
        def results3 = skillsService.getSkillUsers(projId, allSkillIds.get(1).get(1), 1, 1, "userId", true, "h")

        then:
        results1
        results1.count == 1
        results1.totalCount == 1
        results1.data.size() == 1
        results1.data.get(0).userId.contains(sampleUserIds.get(0)?.toLowerCase())
        results1.data.get(0).totalPoints == 35

        results2
        results2.count == 2
        results2.totalCount == 2
        results2.data.size() == 1
        results2.data.get(0).userId.contains(sampleUserIds.get(0)?.toLowerCase())
        results2.data.get(0).totalPoints == 35

        results3
        results3.count == 0
        results3.totalCount == 0
        !results3.data
    }

    def 'get skill users with minimum points'() {
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], sampleUserIds.get(2), threeDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], sampleUserIds.get(3), threeDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], sampleUserIds.get(4), threeDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], sampleUserIds.get(2), twoDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], sampleUserIds.get(3), twoDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], sampleUserIds.get(4), twoDaysAgo)

        when:
        def results1 = skillsService.getSkillUsers(projId, allSkillIds.get(0).get(0), 10, 1, "userId", true, "", 0)
        def results2 = skillsService.getSkillUsers(projId, allSkillIds.get(0).get(0), 10, 1, "userId", true, "", 45)
        def results3 = skillsService.getSkillUsers(projId, allSkillIds.get(0).get(0), 10, 1, "userId", true, "", 80)

        then:
        results1
        results1.count == 4
        results1.totalCount == 4
        results1.data.size() == 4
        results1.data.get(0).userId.contains(sampleUserIds.get(0)?.toLowerCase())
        results1.data.get(0).totalPoints == 35

        results2
        results2.count == 3
        results2.totalCount == 4

        results3
        results3.count == 0
        results3.totalCount == 4
    }


    def 'get skill users with maximum points'() {
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], sampleUserIds.get(2), threeDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], sampleUserIds.get(3), threeDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], sampleUserIds.get(4), threeDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], sampleUserIds.get(2), twoDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], sampleUserIds.get(3), twoDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], sampleUserIds.get(4), twoDaysAgo)

        when:
        def results1 = skillsService.getSkillUsers(projId, allSkillIds.get(0).get(0), 10, 1, "userId", true, "", 0, 100)
        def results2 = skillsService.getSkillUsers(projId, allSkillIds.get(0).get(0), 10, 1, "userId", true, "", 0, 40)
        def results3 = skillsService.getSkillUsers(projId, allSkillIds.get(0).get(0), 10, 1, "userId", true, "", 0, 10)

        then:
        results1
        results1.count == 4
        results1.totalCount == 4
        results1.data.size() == 4
        results1.data.get(0).userId.contains(sampleUserIds.get(0)?.toLowerCase())
        results1.data.get(0).totalPoints == 35

        results2
        results2.count == 1
        results2.totalCount == 4

        results3
        results3.count == 0
        results3.totalCount == 4
    }

    def 'get skill users with minimum and maximum points'() {
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], sampleUserIds.get(2), threeDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], sampleUserIds.get(3), threeDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], sampleUserIds.get(4), threeDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], sampleUserIds.get(2), twoDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], sampleUserIds.get(3), twoDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], sampleUserIds.get(4), twoDaysAgo)

        when:
        def results1 = skillsService.getSkillUsers(projId, allSkillIds.get(0).get(0), 10, 1, "userId", true, "", 0, 100)
        def results2 = skillsService.getSkillUsers(projId, allSkillIds.get(0).get(0), 10, 1, "userId", true, "", 5, 51)
        def results3 = skillsService.getSkillUsers(projId, allSkillIds.get(0).get(0), 10, 1, "userId", true, "", 5, 26)
        def results4 = skillsService.getSkillUsers(projId, allSkillIds.get(0).get(0), 10, 1, "userId", true, "", 30, 35)

        then:
        results1
        results1.count == 4
        results1.totalCount == 4
        results1.data.size() == 4
        results1.data.get(0).userId.contains(sampleUserIds.get(0)?.toLowerCase())
        results1.data.get(0).totalPoints == 35

        results2
        results2.count == 4
        results2.totalCount == 4

        results3
        results3.count == 1
        results3.totalCount == 4

        results4
        results4.count == 0
        results4.totalCount == 4
    }

    def 'can not get skills with negative points'() {
        when:
        skillsService.getSkillUsers(projId, allSkillIds.get(0).get(0), 10, 1, "userId", true, "", -100)

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.getResBody().contains("Minimum Points is less than 0")
    }

    def 'get badge users when project exists'() {
        when:
        def results1 = skillsService.getBadgeUsers(projId, badgeId)

        then:
        results1
        results1.count == 1
        results1.totalCount == 1
        results1.data.size() == 1
        results1.data.get(0).userId.contains(sampleUserIds.get(0)?.toLowerCase())
        results1.data.get(0).totalPoints == 35
        results1.totalPoints == 35 * 4
    }

    def "user updated date is updated when a skill is achieved"() {

        //testSubject1, testSubject2
        final uid = sampleUserIds.get(0).toLowerCase()

        when:
        def users = skillsService.getProjectUsers(projId, 100).data

        def userBeforeSkillAdd = users.find() {it.userId.contains(uid)}
        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

        def res = skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(1)], uid, new DateTime().toDate())
        assert res.body.skillApplied

        def users2 = skillsService.getProjectUsers(projId, 100).data

        def userAfterSkillAdd = users2.find() {it.userId.contains(uid)}


        then:
        formatter.parseDateTime(userBeforeSkillAdd.lastUpdated).isBefore(formatter.parseDateTime(userAfterSkillAdd.lastUpdated))

    }

   @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
   def "filter users by name"(){

        SkillsService createAcctService = createService()
        createAcctService.createUser([firstName: "John", lastName: "Doe", email: "jdoe@email.foo", password: "password"])
        createAcctService.createUser([firstName: "Jane", lastName: "Doe", email: "jadoe@email.foo", password: "password"])
        createAcctService.createUser([firstName: "Foo", lastName: "Bar", email: "fbar@email.foo", password: "password"])

        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], "jdoe@email.foo", threeDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], "jadoe@email.foo", threeDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], "fbar@email.foo", threeDaysAgo)

        when:

        def control = skillsService.getProjectUsers(projId)
        def result1 = skillsService.getProjectUsers(projId, 10, 1, "userId", true, "Jane")
        def result2 = skillsService.getProjectUsers(projId, 10, 1, "userId", true, "jadoe")
        def result3 = skillsService.getProjectUsers(projId, 10, 1, "userId", true, "Doe")

        then:

        control.data.size() == 5

        result1.data.size() == 1
        result1.data.find{it.userId.contains('jadoe@email.foo')}

        result2.data.size() == 1
        result2.data.find{it.userId.contains('jadoe@email.foo')}

        result3.data.size() == 2
        result3.data.find{it.userId.contains('jadoe@email.foo')}
        result3.data.find{it.userId.contains('jdoe@email.foo')}
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "sort users" () {
        SkillsService createAcctService = createService()
        createAcctService.createUser([firstName: "John", lastName: "Doe", email: "jdoe@email.foo", password: "password"])
        createAcctService.createUser([firstName: "Jane", lastName: "Doe", email: "jadoe@email.foo", password: "password"])
        createAcctService.createUser([firstName: "Foo", lastName: "Bar", email: "fbar@email.foo", password: "password"])

        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], "jdoe@email.foo", threeDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], "jadoe@email.foo", threeDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], "fbar@email.foo", threeDaysAgo)

        when:
        def allUsers = skillsService.getProjectUsers(projId)
        def fooUsers = skillsService.getProjectUsers(projId, 10, 1, "userId", true, "foo")
        def fooUsersDesc = skillsService.getProjectUsers(projId, 10, 1, "userId", false, "foo")
        def fooUsersSortByFirstName = skillsService.getProjectUsers(projId, 10, 1, "firstName", true, "foo")
        def fooUsersSortByLastName = skillsService.getProjectUsers(projId, 10, 1, "lastName", true, "foo")

        then:
        allUsers.data.size() == 5
        allUsers.data[0].userId.contains('fbar@email.foo')
        allUsers.data[1].userId.contains('hanson')
        allUsers.data[2].userId.contains('harry')
        allUsers.data[3].userId.contains('jadoe@email.foo')
        allUsers.data[4].userId.contains('jdoe@email.foo')

        fooUsers.data.size() == 3
        fooUsers.data[0].userId.contains('fbar@email.foo')
        fooUsers.data[1].userId.contains('jadoe@email.foo')
        fooUsers.data[2].userId.contains('jdoe@email.foo')

        fooUsersDesc.data.size() == 3
        fooUsersDesc.data[2].userId.contains('fbar@email.foo')
        fooUsersDesc.data[1].userId.contains('jadoe@email.foo')
        fooUsersDesc.data[0].userId.contains('jdoe@email.foo')

        fooUsersSortByFirstName.data.size() == 3
        fooUsersSortByFirstName.data[0].userId.contains('fbar@email.foo')
        fooUsersSortByFirstName.data[1].userId.contains('jadoe@email.foo')
        fooUsersSortByFirstName.data[2].userId.contains('jdoe@email.foo')

        fooUsersSortByLastName.data.size() == 3
        fooUsersSortByLastName.data[0].lastName == 'Bar'
        fooUsersSortByLastName.data[1].lastName == 'Doe'
        fooUsersSortByLastName.data[2].lastName == 'Doe'
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "user paging" () {
        SkillsService createAcctService = createService()
        createAcctService.createUser([firstName: "Aaa", lastName: "Aaa", email: "aaa@email.foo", password: "password"])
        createAcctService.createUser([firstName: "Bbb", lastName: "Bbb", email: "bbb@email.foo", password: "password"])
        createAcctService.createUser([firstName: "Ccc", lastName: "Ccc", email: "ccc@email.foo", password: "password"])
        createAcctService.createUser([firstName: "Ddd", lastName: "Ddd", email: "ddd@email.foo", password: "password"])
        createAcctService.createUser([firstName: "Eee", lastName: "Eee", email: "eee@email.foo", password: "password"])
        createAcctService.createUser([firstName: "Fff", lastName: "Fff", email: "fff@email.foo", password: "password"])
        createAcctService.createUser([firstName: "Ggg", lastName: "Ggg", email: "ggg@email.foo", password: "password"])
        createAcctService.createUser([firstName: "Hhh", lastName: "Hhh", email: "hhh@email.foo", password: "password"])

        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], "aaa@email.foo", threeDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], "bbb@email.foo", threeDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], "ccc@email.foo", threeDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], "ddd@email.foo", threeDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], "eee@email.foo", threeDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], "fff@email.foo", threeDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], "ggg@email.foo", threeDaysAgo)
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], "hhh@email.foo", threeDaysAgo)

        when:
        def firstPage = skillsService.getProjectUsers(projId, 5, 1, "userId", true, "foo")
        def secondPage = skillsService.getProjectUsers(projId, 5, 2, "userId", true, "foo")

        then:
        firstPage.data.size() == 5
        firstPage.data[0].userId.contains('aaa@email.foo')
        firstPage.data[1].userId.contains('bbb@email.foo')
        firstPage.data[2].userId.contains('ccc@email.foo')
        firstPage.data[3].userId.contains('ddd@email.foo')
        firstPage.data[4].userId.contains('eee@email.foo')

        secondPage.data.size() == 3
        secondPage.data[0].userId.contains('fff@email.foo')
        secondPage.data[1].userId.contains('ggg@email.foo')
        secondPage.data[2].userId.contains('hhh@email.foo')
    }



    private List<List<String>> setupProjectWithSkills(List<String> subjects = ['testSubject1', 'testSubject2'], String projectId=projId, name="Test Project") {
        List<List<String>> skillIds = []
        skillsService.createProject([projectId: projectId, name: name])
        subjects.eachWithIndex { String subject, int index ->
            skillsService.createSubject([projectId: projectId, subjectId: subject, name: "Test Subject $index".toString()])
            skillIds << addDependentSkills(projectId,  subject, 3, 1, 4)
        }
        return skillIds
    }

    private List<String> addDependentSkills(String projectId, String subject, int dependencyLevels = 1, int skillsAtEachLevel = 1, int numPerformToCompletion = 1) {
        List<String> parentSkillIds = []
        List<String> allSkillIds = []

        for (int i = 0; i < dependencyLevels; i++) {
            parentSkillIds = addSkillsForSubject(projectId, subject, skillsAtEachLevel, parentSkillIds, numPerformToCompletion)
            allSkillIds.addAll(parentSkillIds)
        }
        return allSkillIds
    }

    private List<String> addSkillsForSubject(String projectId, String subject, int numSkills = 1, List<String> dependentSkillIds = Collections.emptyList(), int numPerformToCompletion = 1) {
        List<String> skillIds = []
        for (int i = 0; i < numSkills; i++) {
            String skillId = 'skill' + RandomStringUtils.randomAlphabetic(5)
            skillsService.createSkill(
                    [
                            projectId: projectId,
                            subjectId: subject,
                            skillId: skillId,
                            name: 'Test Skill ' + RandomStringUtils.randomAlphabetic(8),
                            pointIncrement: 35,
                            numPerformToCompletion: numPerformToCompletion,
                            pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1,
                            dependenctSkillsIds: dependentSkillIds
                    ]
            )
            skillIds << skillId
        }
        return skillIds
    }


    def 'get project users respects project id for lastUpdatedDate'() {
        when:
        // setup a second project
        String projId2 = 'proj2'
        skillsService.deleteProjectIfExist(projId2)

        List<List<String>> proj2SkillIds = setupProjectWithSkills(['testSubject1', 'testSubject2', 'testSubject3'], projId2, 'Test Project 2')

        def results = skillsService.getProjectUsers(projId)
        String mostRecentDate1 = results.data.sort {a,b -> b.lastUpdated <=> a.lastUpdated }.get(0).lastUpdated

        // report a skill for project2
        skillsService.addSkill(['projectId': projId2, skillId: proj2SkillIds.get(0).get(0)], sampleUserIds.get(0), new Date())

        // results two show not be affected
        def results2 = skillsService.getProjectUsers(projId)
        String mostRecentDate2 = results2.data.sort {a,b -> b.lastUpdated <=> a.lastUpdated }.get(0).lastUpdated

        // now report another skill for project1
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], sampleUserIds.get(2), new Date())
        def results3 = skillsService.getProjectUsers(projId)
        String mostRecentDate3 = results3.data.sort {a,b -> b.lastUpdated <=> a.lastUpdated }.get(0).lastUpdated

        then:
        results
        results.count == 2
        results.totalCount == 2
        results.data.size() == 2

        results2.count == 2
        results2.totalCount == 2
        results2.data.size() == 2

        results3.count == 3
        results3.totalCount == 3
        results3.data.size() == 3

        mostRecentDate1 == mostRecentDate2
        mostRecentDate3 > mostRecentDate2
    }

    def 'skill users total points should not be a multiple of the actual total'() {
        def project = SkillsFactory.createProject(99)
        def subject = SkillsFactory.createSubject(99)
        def skill1 = SkillsFactory.createSkill(99, 1, 1, 0, 10, 0, 10)
        def skill2 = SkillsFactory.createSkill(99, 1, 2, 0, 10, 0, 20)

        skillsService.createProject(project)
        skillsService.createSubject(subject)
        skillsService.createSkill(skill1)
        skillsService.createSkill(skill2)

        when:
        def user = getRandomUsers(1)[0]
        skillsService.addSkill(skill1, user, new Date().minus(5))

        def skillUsersOneOccurence = skillsService.getSkillUsers(project.projectId, skill1.skillId)
        skillsService.addSkill(skill1, user, new Date().minus(3))
        def skillUsersTwoOccurrences = skillsService.getSkillUsers(project.projectId, skill1.skillId)
        skillsService.addSkill(skill1, user, new Date().minus(1))
        def skillUsersThreeOccurrences = skillsService.getSkillUsers(project.projectId, skill1.skillId)

        then:
        skillUsersOneOccurence.data[0].userId == user
        skillUsersOneOccurence.data[0].totalPoints == 10
        skillUsersTwoOccurrences.data[0].userId == user
        skillUsersTwoOccurrences.data[0].totalPoints == 20
        skillUsersThreeOccurrences.data[0].userId == user
        skillUsersThreeOccurrences.data[0].totalPoints == 30
    }

    def 'subject users total points should not be a multiple of the actual total'() {
        def project = SkillsFactory.createProject(99)
        def subject = SkillsFactory.createSubject(99)
        def skill1 = SkillsFactory.createSkill(99, 1, 1, 0, 10, 0, 10)
        def skill2 = SkillsFactory.createSkill(99, 1, 2, 0, 10, 0, 20)

        skillsService.createProject(project)
        skillsService.createSubject(subject)
        skillsService.createSkill(skill1)
        skillsService.createSkill(skill2)

        when:
        def user = getRandomUsers(1)[0]
        skillsService.addSkill(skill1, user, new Date().minus(5))

        def subjectUsersOneOccurrence = skillsService.getSubjectUsers(project.projectId, subject.subjectId)
        skillsService.addSkill(skill1, user, new Date().minus(3))
        def subjectUsersTwoOccurrences = skillsService.getSubjectUsers(project.projectId, subject.subjectId)
        skillsService.addSkill(skill1, user, new Date().minus(1))
        def subjectUsersThreeOccurrences = skillsService.getSubjectUsers(project.projectId, subject.subjectId)

        then:
        subjectUsersOneOccurrence.data[0].userId == user
        subjectUsersOneOccurrence.data[0].totalPoints == 10
        subjectUsersTwoOccurrences.data[0].userId == user
        subjectUsersTwoOccurrences.data[0].totalPoints == 20
        subjectUsersThreeOccurrences.data[0].userId == user
        subjectUsersThreeOccurrences.data[0].totalPoints == 30
    }

    def 'get project users levels'() {
        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def skill1 = createSkill(2, 1, 1, 0, 10, 512, 10,)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [skill1])

        def p2subj2 = createSubject(2, 2)
        def skill2 = createSkill(2, 2, 2, 0, 10, 512, 10,)
        skillsService.createSubject(p2subj2)
        skillsService.createSkill(skill2)

        List<String> users = getRandomUsers(6)
        skillsService.addSkill(skill1, users[0])

        // overall level 1
        skillsService.addSkill(skill1, users[1], new Date() - 1)
        skillsService.addSkill(skill2, users[1])
        skillsService.addSkill(skill1, users[2], new Date() - 1)
        skillsService.addSkill(skill2, users[2])

        // overall level 2
        (5..1).each {
            skillsService.addSkill(skill1, users[3], new Date() - it)
        }

        // overall level 4
        (4..1).each {
            skillsService.addSkill(skill1, users[4], new Date() - it)
            skillsService.addSkill(skill1, users[5], new Date() - it)
        }
        (10..1).each {
            skillsService.addSkill(skill2, users[4], new Date() - it)
            skillsService.addSkill(skill2, users[5], new Date() - it)
        }

        when:
        def projRes = skillsService.getProjectUsers(p2.projectId)
        def subjRes = skillsService.getSubjectUsers(p2.projectId, p2subj1.subjectId)
        def subj2Res = skillsService.getSubjectUsers(p2.projectId, p2subj2.subjectId)

        then:
        projRes.count == 6
        projRes.totalCount == 6
        def data = users.collect {String usr -> projRes.data.find { it.userId == usr} }
        data.userMaxLevel == [0, 1, 1, 2, 4, 4]

        subjRes.count == 6
        subjRes.totalCount == 6
        def data1 = users.collect {String usr -> subjRes.data.find { it.userId == usr} }
        data1.userMaxLevel == [1, 1, 1, 3, 2, 2]

        subj2Res.count == 4
        subj2Res.totalCount == 4
        def data2 = users.collect {String usr -> subj2Res.data.find { it.userId == usr} }
        data2.userMaxLevel == [1, 1, 5, 5]
        data2.userId == [users[1], users[2], users[4], users[5]]
    }

    def 'get project users user tags'() {
        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def skill1 = createSkill(2, 1, 1, 0, 10, 512, 10,)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [skill1])

        def p2subj2 = createSubject(2, 2)
        def skill2 = createSkill(2, 2, 2, 0, 10, 512, 10,)
        skillsService.createSubject(p2subj2)
        skillsService.createSkill(skill2)

        def p2Badge1 = createBadge(2, 1)
        skillsService.addBadge(p2Badge1)
        skillsService.assignSkillToBadge([projectId: p2Badge1.projectId, badgeId: p2Badge1.badgeId, skillId: skill2.skillId])

        List<String> users = usersWithTags
        skillsService.addSkill(skill1, users[0])

        // overall level 1
        skillsService.addSkill(skill1, users[1], new Date() - 1)
        skillsService.addSkill(skill2, users[1])
        skillsService.addSkill(skill1, users[2], new Date() - 1)
        skillsService.addSkill(skill2, users[2])

        // overall level 2
        (5..1).each {
            skillsService.addSkill(skill1, users[3], new Date() - it)
        }

        // overall level 4
        (4..1).each {
            skillsService.addSkill(skill1, users[4], new Date() - it)
            skillsService.addSkill(skill1, users[5], new Date() - it)
        }
        (10..1).each {
            skillsService.addSkill(skill2, users[4], new Date() - it)
            skillsService.addSkill(skill2, users[5], new Date() - it)
        }

        usersWithTags[0..5].eachWithIndex { userId, idx ->
            String tagValue = "tag${idx}"
            rootSkillsService.saveUserTag(userId, "dutyOrganization", [tagValue]);
        }

        when:
        def projRes = skillsService.getProjectUsers(p2.projectId)
        def subjRes = skillsService.getSubjectUsers(p2.projectId, p2subj1.subjectId)
        def subj2Res = skillsService.getSubjectUsers(p2.projectId, p2subj2.subjectId)
        def badgeRes = skillsService.getBadgeUsers(p2.projectId, p2Badge1.badgeId)
        def skillRes = skillsService.getSkillUsers(p2.projectId, skill2.skillId)

        then:
        projRes.count == 6
        projRes.totalCount == 6
        def data = users.collect {String usr -> projRes.data.find { it.userId == usr} }
        data.userTag == ['tag0', 'tag1', 'tag2', 'tag3', 'tag4', 'tag5']

        subjRes.count == 6
        subjRes.totalCount == 6
        def data1 = users.collect {String usr -> subjRes.data.find { it.userId == usr} }
        data1.userTag == ['tag0', 'tag1', 'tag2', 'tag3', 'tag4', 'tag5']

        subj2Res.count == 4
        subj2Res.totalCount == 4
        def data2 = users.collect {String usr -> subj2Res.data.find { it.userId == usr} }
        data2.userTag == ['tag1', 'tag2', 'tag4', 'tag5']

        badgeRes.count == 4
        badgeRes.totalCount == 4
        def data3 = users.collect {String usr -> badgeRes.data.find { it.userId == usr} }
        data3.userTag == ['tag1', 'tag2', 'tag4', 'tag5']

        skillRes.count == 4
        skillRes.totalCount == 4
        def data4 = users.collect {String usr -> skillRes.data.find { it.userId == usr} }
        data4.userTag == ['tag1', 'tag2', 'tag4', 'tag5']
    }

    def 'user project points maximum filter is exclusive' () {
        skillsService.deleteProjectIfExist(projId)
        def proj = createProject()
        def subject = createSubject()
        List<Map> skills = createSkills(4, 1, 1, 100)

        skillsService.createProject(proj)
        skillsService.createSubject(subject)
        skillsService.createSkills(skills)

        def users = getRandomUsers(4)
        skillsService.addSkill(skills[0], users[0], new Date())
        skillsService.addSkill(skills[1], users[0], new Date())
        skillsService.addSkill(skills[2], users[0], new Date())
        skillsService.addSkill(skills[3], users[0], new Date())

        skillsService.addSkill(skills[0], users[1], new Date())
        skillsService.addSkill(skills[1], users[1], new Date())
        skillsService.addSkill(skills[2], users[1], new Date())

        skillsService.addSkill(skills[0], users[2], new Date())
        skillsService.addSkill(skills[1], users[2], new Date())

        skillsService.addSkill(skills[0], users[3], new Date())

        when:
        def projFull = skillsService.getProjectUsers(projId,  10, 1, "userId", true, "", 0, 100)
        def proj76 = skillsService.getProjectUsers(projId,  10, 1, "userId", true, "", 0, 76)
        def proj75 = skillsService.getProjectUsers(projId,  10, 1, "userId", true, "", 0, 75)
        def proj50 = skillsService.getProjectUsers(projId,  10, 1, "userId", true, "", 0, 50)
        def proj25 = skillsService.getProjectUsers(projId,  10, 1, "userId", true, "", 0, 25)

        then:
        projFull.count == 4
        proj76.count == 3
        proj75.count == 2
        proj50.count == 1
        proj25.count == 0

    }

    def 'user subject points maximum filter is exclusive' () {
        skillsService.deleteProjectIfExist(projId)
        def proj = createProject()
        def subject = createSubject()
        List<Map> skills = createSkills(4, 1, 1, 100)

        skillsService.createProject(proj)
        skillsService.createSubject(subject)
        skillsService.createSkills(skills)

        def users = getRandomUsers(4)
        skillsService.addSkill(skills[0], users[0], new Date())
        skillsService.addSkill(skills[1], users[0], new Date())
        skillsService.addSkill(skills[2], users[0], new Date())
        skillsService.addSkill(skills[3], users[0], new Date())

        skillsService.addSkill(skills[0], users[1], new Date())
        skillsService.addSkill(skills[1], users[1], new Date())
        skillsService.addSkill(skills[2], users[1], new Date())

        skillsService.addSkill(skills[0], users[2], new Date())
        skillsService.addSkill(skills[1], users[2], new Date())

        skillsService.addSkill(skills[0], users[3], new Date())

        when:
        def subj100 = skillsService.getSubjectUsers(projId, subject.subjectId, 10, 1, "userId", true, "", 0, 100)
        def subj76 = skillsService.getSubjectUsers(projId, subject.subjectId, 10, 1, "userId", true, "", 0, 76)
        def subj75 = skillsService.getSubjectUsers(projId, subject.subjectId, 10, 1, "userId", true, "", 0, 75)
        def subj50 = skillsService.getSubjectUsers(projId, subject.subjectId, 10, 1, "userId", true, "", 0, 50)
        def subj25 = skillsService.getSubjectUsers(projId, subject.subjectId, 10, 1, "userId", true, "", 0, 25)

        then:
        subj100.count == 4
        subj76.count == 3
        subj75.count == 2
        subj50.count == 1
        subj25.count == 0

    }

    def 'user skill points maximum filter is exclusive' () {
        skillsService.deleteProjectIfExist(projId)
        def proj = createProject()
        def subject = createSubject()
        def skill = createSkills(1, 1, 1, 100, 4)

        skillsService.createProject(proj)
        skillsService.createSubject(subject)
        skillsService.createSkills(skill)

        def users = getRandomUsers(4)
        skillsService.addSkill(skill[0], users[0], new Date() - 3)
        skillsService.addSkill(skill[0], users[0], new Date() - 2)
        skillsService.addSkill(skill[0], users[0], new Date() - 1)
        skillsService.addSkill(skill[0], users[0], new Date())

        skillsService.addSkill(skill[0], users[1], new Date() - 2)
        skillsService.addSkill(skill[0], users[1], new Date() - 1)
        skillsService.addSkill(skill[0], users[1], new Date())

        skillsService.addSkill(skill[0], users[2], new Date() - 1)
        skillsService.addSkill(skill[0], users[2], new Date())

        skillsService.addSkill(skill[0], users[3], new Date())

        when:
        def skills100 = skillsService.getSkillUsers(projId, skill[0].skillId, 10, 1, "userId", true, "", 0, 100)
        def skills76 = skillsService.getSkillUsers(projId, skill[0].skillId, 10, 1, "userId", true, "", 0, 76)
        def skills75 = skillsService.getSkillUsers(projId, skill[0].skillId, 10, 1, "userId", true, "", 0, 75)
        def skills50 = skillsService.getSkillUsers(projId, skill[0].skillId, 10, 1, "userId", true, "", 0, 50)
        def skills25 = skillsService.getSkillUsers(projId, skill[0].skillId, 10, 1, "userId", true, "", 0, 25)

        then:
        skills100.count == 4
        skills76.count == 3
        skills75.count == 2
        skills50.count == 1
        skills25.count == 0

    }
}
