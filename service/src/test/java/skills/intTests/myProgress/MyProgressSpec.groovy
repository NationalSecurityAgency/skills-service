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
package skills.intTests.myProgress

import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.intTests.utils.TestUtils
import skills.services.settings.Settings
import spock.lang.IgnoreRest

@Slf4j
class MyProgressSpec extends DefaultIntSpec {
    TestUtils testUtils = new TestUtils()
    SkillsService rootSkillsService
    SkillsService supervisorService
    String userId
    String PROD_MODE = Settings.PRODUCTION_MODE.settingName

    def setup() {
        userId = skillsService.wsHelper.username//"user1"
        String ultimateRoot = 'jh@dojo.com'
        rootSkillsService = createService(ultimateRoot, 'aaaaaaaa')
        rootSkillsService.grantRoot()
        String supervisorUserId = 'foo@bar.com'
        supervisorService = createService(supervisorUserId)
        rootSkillsService.grantSupervisorRole(supervisorUserId)

        // delete Inception so it doesn't affect our test numbers
        rootSkillsService.deleteProject('Inception')
    }

    def "my progress summary - badge count"() {
        def proj1 = SkillsFactory.createProject()
        def subj1 = SkillsFactory.createSubject()
        def badge1 = SkillsFactory.createBadge()
        badge1.enabled = false;
        def badge2 = SkillsFactory.createBadge(1, 2)
        badge2.enabled = false;
        def badge3 = SkillsFactory.createBadge(1, 3)
        badge3.enabled = false;

        Date oneWeekAgo = new Date()-7
        Date twoWeeksAgo = new Date()-14
        def gem1 = SkillsFactory.createBadge(1, 4)
        gem1.enabled = false;
        gem1.startDate = twoWeeksAgo
        gem1.endDate = oneWeekAgo
        def gem2 = SkillsFactory.createBadge(1, 5)
        gem2.enabled = false;
        gem2.startDate = twoWeeksAgo
        gem2.endDate = oneWeekAgo

        def globalBadge = [badgeId: "globalBadge", name: 'Test Global Badge 1', enabled: 'false']
        def globalBadge2 = [badgeId: "globalBadge2", name: 'Test Global Badge 2', enabled: 'false']
        def globalBadge3 = [badgeId: "globalBadge3", name: 'Test Global Badge 3', enabled: 'false']

        def skills = SkillsFactory.createSkills(3, )
        skillsService.createProject(proj1)
        skillsService.createSubject(subj1)
        skillsService.createSkills(skills)
        skillsService.createBadge(badge1)
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge1.badgeId, skillId: skills[0].skillId])
        skillsService.createBadge(badge2)
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge2.badgeId, skillId: skills[1].skillId])
        skillsService.createBadge(badge3)
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge3.badgeId, skillId: skills[2].skillId])

        skillsService.createBadge(gem1)
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: gem1.badgeId, skillId: skills[2].skillId])

        skillsService.createBadge(gem2)
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: gem2.badgeId, skillId: skills[2].skillId])

        supervisorService.createGlobalBadge(globalBadge)
        supervisorService.assignSkillToGlobalBadge(projectId: proj1.projectId, badgeId: globalBadge.badgeId, skillId: skills[2].skillId)
        supervisorService.createGlobalBadge(globalBadge2)
        supervisorService.assignSkillToGlobalBadge(projectId: proj1.projectId, badgeId: globalBadge2.badgeId, skillId: skills[2].skillId)
        supervisorService.createGlobalBadge(globalBadge3)
        supervisorService.assignSkillToGlobalBadge(projectId: proj1.projectId, badgeId: globalBadge3.badgeId, skillId: skills[2].skillId)

        when:
        def res = skillsService.getMyProgressSummary()

        badge1.enabled = 'true'
        skillsService.createBadge(badge1)

        def res1 = skillsService.getMyProgressSummary()

        // enable "production mode"
        skillsService.changeSetting(proj1.projectId, PROD_MODE, [projectId: proj1.projectId, setting: PROD_MODE, value: "true"])

        def res2 = skillsService.getMyProgressSummary()

        badge2.enabled = 'true'
        skillsService.createBadge(badge2)
        badge3.enabled = 'true'
        skillsService.createBadge(badge3)

        def res3 = skillsService.getMyProgressSummary()

        gem1.enabled = 'true'
        skillsService.createBadge(gem1)

        def res4 = skillsService.getMyProgressSummary()

        gem2.enabled = 'true'
        skillsService.createBadge(gem2)

        def res5 = skillsService.getMyProgressSummary()

        globalBadge.enabled = 'true'
        supervisorService.createGlobalBadge(globalBadge)

        def res6 = skillsService.getMyProgressSummary()

        globalBadge2.enabled = 'true'
        supervisorService.createGlobalBadge(globalBadge2)

        def res7 = skillsService.getMyProgressSummary()

        globalBadge3.enabled = 'true'
        supervisorService.createGlobalBadge(globalBadge3)

        def res8 = skillsService.getMyProgressSummary()

        then:
        res.totalBadges == 0
        res.gemCount == 0
        res.globalBadgeCount == 0

        res1.totalBadges == 0
        res1.gemCount == 0
        res1.globalBadgeCount == 0

        res2.totalBadges == 1
        res2.gemCount == 0
        res2.globalBadgeCount == 0

        res3.totalBadges == 3
        res3.gemCount == 0
        res3.globalBadgeCount == 0

        res4.totalBadges == 4
        res4.gemCount == 1
        res4.globalBadgeCount == 0

        res5.totalBadges == 5
        res5.gemCount == 2
        res5.globalBadgeCount == 0

        res6.totalBadges == 6
        res6.gemCount == 2
        res6.globalBadgeCount == 1

        res7.totalBadges == 7
        res7.gemCount == 2
        res7.globalBadgeCount == 2

        res8.totalBadges == 8
        res8.gemCount == 2
        res8.globalBadgeCount == 3
    }

    def "my progress summary - global badge achieved count"() {
        def proj1 = SkillsFactory.createProject()
        def subj1 = SkillsFactory.createSubject()

        def skills = SkillsFactory.createSkills(3, )
        skills.each { it.pointIncrement = 100 }
        skillsService.createProject(proj1)
        skillsService.createSubject(subj1)
        skillsService.createSkills(skills)

        def globalBadge = [badgeId: "globalBadge", name: 'Test Global Badge 1', enabled: 'false']
        def globalBadge2 = [badgeId: "globalBadge2", name: 'Test Global Badge 2', enabled: 'false']
        def globalBadge3 = [badgeId: "globalBadge3", name: 'Test Global Badge 3', enabled: 'false']

        supervisorService.createGlobalBadge(globalBadge)
        supervisorService.assignSkillToGlobalBadge(projectId: proj1.projectId, badgeId: globalBadge.badgeId, skillId: skills[0].skillId)
        supervisorService.createGlobalBadge(globalBadge2)
        supervisorService.assignSkillToGlobalBadge(projectId: proj1.projectId, badgeId: globalBadge2.badgeId, skillId: skills[1].skillId)
        supervisorService.createGlobalBadge(globalBadge3)
        supervisorService.assignSkillToGlobalBadge(projectId: proj1.projectId, badgeId: globalBadge3.badgeId, skillId: skills[2].skillId)

        globalBadge.enabled = 'true'
        supervisorService.createGlobalBadge(globalBadge)

        globalBadge2.enabled = 'true'
        supervisorService.createGlobalBadge(globalBadge2)

        globalBadge3.enabled = 'true'
        supervisorService.createGlobalBadge(globalBadge3)

        skillsService.addSkill([projectId: proj1.projectId, skillId: skills.get(1).skillId])
        when:
        def res = skillsService.getMyProgressSummary()

        then:
        res.globalBadgeCount == 3
        res.numAchievedGlobalBadges == 1
    }

    def "my progress summary - no skills have been created"() {
        def proj1 = SkillsFactory.createProject()
        def subj1 = SkillsFactory.createSubject()

        skillsService.createProject(proj1)
        skillsService.createSubject(subj1)

        // enable "production mode"
        skillsService.changeSetting(proj1.projectId, PROD_MODE, [projectId: proj1.projectId, setting: PROD_MODE, value: "true"])

        when:
        def res = skillsService.getMyProgressSummary()
        then:
        res
        res.projectSummaries
        res.projectSummaries.size() == 1
        res.projectSummaries.first().rank == 1
        res.projectSummaries.first().totalUsers == 1
        res.projectSummaries.first().points == 0
        res.projectSummaries.first().totalPoints == 0
        res.projectSummaries.first().level == 0
        res.totalProjects == 1
        res.numProjectsContributed == 0
        res.totalSkills == 0
        res.numAchievedSkills == 0
        res.numAchievedSkillsLastMonth == 0
        res.numAchievedSkillsLastWeek == 0
        res.mostRecentAchievedSkill == null
        res.totalBadges == 0
        res.numAchievedGemBadges == 0
        res.numAchievedGlobalBadges == 0
    }

    def "my progress summary - skills have been created"() {
        def proj1 = SkillsFactory.createProject()
        def subj1 = SkillsFactory.createSubject()
        def skill1 = SkillsFactory.createSkill()
        def badge = SkillsFactory.createBadge()

        skillsService.createProject(proj1)
        skillsService.createSubject(subj1)
        skillsService.createSkill(skill1)
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge.badgeId, skillId: skill1.skillId])
        badge.enabled = 'true'
        skillsService.createBadge(badge)

        def globalBadge = [badgeId: "globalBadge", name: 'Test Global Badge 1', enabled: 'false']
        supervisorService.createGlobalBadge(globalBadge)
        supervisorService.assignSkillToGlobalBadge(projectId: proj1.projectId, badgeId: globalBadge.badgeId, skillId: skill1.skillId)
        globalBadge.enabled = 'true'
        supervisorService.createGlobalBadge(globalBadge)

        // enable "production mode"
        skillsService.changeSetting(proj1.projectId, PROD_MODE, [projectId: proj1.projectId, setting: PROD_MODE, value: "true"])

        when:
        def res = skillsService.getMyProgressSummary()
        then:
        res
        res.projectSummaries
        res.projectSummaries.size() == 1
        res.projectSummaries.find{ it.projectId == 'TestProject1' }
        res.projectSummaries.find{ it.projectId == 'TestProject1' }.rank == 1
        res.projectSummaries.find{ it.projectId == 'TestProject1' }.totalUsers == 1
        res.projectSummaries.find{ it.projectId == 'TestProject1' }.points == 0
        res.projectSummaries.find{ it.projectId == 'TestProject1' }.totalPoints == 10
        res.projectSummaries.find{ it.projectId == 'TestProject1' }.level == 0
        res.totalProjects == 1
        res.numProjectsContributed == 0
        res.totalSkills == 1
        res.numAchievedSkills == 0
        res.numAchievedSkillsLastMonth == 0
        res.numAchievedSkillsLastWeek == 0
        res.mostRecentAchievedSkill == null
        res.totalBadges == 2
        res.numAchievedGemBadges == 0
        res.numAchievedGlobalBadges == 0
    }

    def "my progress summary - incrementally achieve a single skill"() {
        String projId = SkillsFactory.defaultProjId
        List<Map> subj1 = (1..2).collect { [projectId: projId, subjectId: "subj1", skillId: "s1${it}".toString(), name: "subj1 ${it}".toString(), type: "Skill", pointIncrement: 10, numPerformToCompletion: 5, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }
        String skillId = subj1.get(1).skillId
        skillsService.createSchema([subj1])

        // enable "production mode"
        skillsService.changeSetting(projId, PROD_MODE, [projectId: projId, setting: PROD_MODE, value: "true"])

        def badge = SkillsFactory.createBadge()
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge([projectId: projId, badgeId: badge.badgeId, skillId: skillId])
        badge.enabled = 'true'
        skillsService.createBadge(badge)

        def globalBadge = [badgeId: "globalBadge", name: 'Test Global Badge 1', enabled: 'false']
        supervisorService.createGlobalBadge(globalBadge)
        supervisorService.assignSkillToGlobalBadge(projectId: projId, badgeId: globalBadge.badgeId, skillId: skillId)
        globalBadge.enabled = 'true'
        supervisorService.createGlobalBadge(globalBadge)

        when:
        List<Date> dates = testUtils.getLastNDays(5).collect { it - 14}
        List addSkillRes = []
        List mySummaryRes = []
        (0..4).each {
            log.info("Adding ${subj1.get(1).skillId} on ${dates.get(it)}")
            addSkillRes << skillsService.addSkill([projectId: projId, skillId: subj1.get(1).skillId], userId, dates.get(it))
            mySummaryRes << skillsService.getMyProgressSummary()
        }

        then:
        addSkillRes.each {
            assert it.body.skillApplied
            assert it.body.explanation == "Skill event was applied"
        }
        mySummaryRes.eachWithIndex { res, index ->
            assert res
            assert res.projectSummaries
            assert res.projectSummaries.size() == 1
            assert res.projectSummaries.find{ it.projectId == projId }
            assert res.projectSummaries.find{ it.projectId == projId }.rank == 1
            assert res.projectSummaries.find{ it.projectId == projId }.totalUsers == 1
            assert res.projectSummaries.find{ it.projectId == projId }.points == (index+1) * 10
            assert res.projectSummaries.find{ it.projectId == projId }.totalPoints == 100
            assert res.totalProjects == 1
            assert res.numProjectsContributed == 1
            assert res.totalSkills == 2
            assert res.totalBadges == 2
            assert res.numAchievedGemBadges == 0

            if (index < 2) {
                assert res.projectSummaries.find{ it.projectId == 'TestProject1' }.level == 1
            } else if (index < 4) {
                assert res.projectSummaries.find{ it.projectId == 'TestProject1' }.level == 2
            }

            if (index == 4) {
                assert res.projectSummaries.find{ it.projectId == 'TestProject1' }.level == 3
                assert res.numAchievedSkills == 1
                assert res.numAchievedSkillsLastMonth == 1
                assert res.numAchievedSkillsLastWeek == 0
                assert StringUtils.startsWith(res.mostRecentAchievedSkill, dates.last().format("yyyy-MM-dd'T'HH:mm:ss.SSS"))
                assert res.numAchievedGlobalBadges == 1
            } else {
                assert res.numAchievedSkills == 0
                assert res.numAchievedSkillsLastMonth == 0
                assert res.numAchievedSkillsLastWeek == 0
                assert res.mostRecentAchievedSkill == null
                assert res.numAchievedGlobalBadges == 0
            }
        }
    }

    def "my progress summary - incrementally achieve a single skill, multiple projects"() {
        String projId = SkillsFactory.defaultProjId
        List<Map> subj1 = (1..2).collect { [projectId: projId, subjectId: "subj1", skillId: "s1${it}".toString(), name: "subj1 ${it}".toString(), type: "Skill", pointIncrement: 10, numPerformToCompletion: 5, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }
        String skillId = subj1.get(1).skillId
        skillsService.createSchema([subj1])

        // enable "production mode"
        skillsService.changeSetting(projId, PROD_MODE, [projectId: projId, setting: PROD_MODE, value: "true"])

        def badge = SkillsFactory.createBadge()
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge([projectId: projId, badgeId: badge.badgeId, skillId: skillId])
        badge.enabled = 'true'
        skillsService.createBadge(badge)

        def globalBadge = [badgeId: "globalBadge", name: 'Test Global Badge 1', enabled: 'false']
        supervisorService.createGlobalBadge(globalBadge)
        supervisorService.assignSkillToGlobalBadge(projectId: projId, badgeId: globalBadge.badgeId, skillId: skillId)
        globalBadge.enabled = 'true'
        supervisorService.createGlobalBadge(globalBadge)

        // create second project
        String projId2 = SkillsFactory.getDefaultProjId(2)
        List<Map> subj2 = (1..2).collect { [projectId: projId2, subjectId: "subj1", skillId: "s1${it}".toString(), name: "subj1 ${it}".toString(), type: "Skill", pointIncrement: 10, numPerformToCompletion: 5, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }
        String skillId2 = subj2.get(1).skillId
        skillsService.createSchema([subj2])

        // enable "production mode"
        skillsService.changeSetting(projId2, PROD_MODE, [projectId: projId2, setting: PROD_MODE, value: "true"])

        def badge2 = SkillsFactory.createBadge(2)
        skillsService.createBadge(badge2)
        skillsService.assignSkillToBadge([projectId: projId2, badgeId: badge2.badgeId, skillId: skillId2])
        badge2.enabled = 'true'
        skillsService.createBadge(badge2)

        def globalBadge2 = [badgeId: "globalBadge2", name: 'Test Global Badge 2', enabled: 'false']
        supervisorService.createGlobalBadge(globalBadge2)
        supervisorService.assignSkillToGlobalBadge(projectId: projId2, badgeId: globalBadge2.badgeId, skillId: skillId2)
        globalBadge2.enabled = 'true'
        supervisorService.createGlobalBadge(globalBadge2)

        when:
        List<Date> dates = testUtils.getLastNDays(5).collect { it - 14}
        List addSkillRes = []
        List mySummaryRes = []
        (0..4).each {
            log.info("Adding ${subj1.get(1).skillId} on ${dates.get(it)}")
            addSkillRes << skillsService.addSkill([projectId: projId, skillId: subj1.get(1).skillId], userId, dates.get(it))
            mySummaryRes << skillsService.getMyProgressSummary()
        }

        then:
        addSkillRes.each {
            assert it.body.skillApplied
            assert it.body.explanation == "Skill event was applied"
        }
        mySummaryRes.eachWithIndex { res, index ->
            assert res
            assert res.projectSummaries
            assert res.projectSummaries.size() == 2
            assert res.projectSummaries.find{ it.projectId == projId }
            assert res.projectSummaries.find{ it.projectId == projId }.rank == 1
            assert res.projectSummaries.find{ it.projectId == projId }.totalUsers == 1
            assert res.projectSummaries.find{ it.projectId == projId }.points == (index+1) * 10
            assert res.projectSummaries.find{ it.projectId == projId }.totalPoints == 100
            assert res.totalProjects == 2
            assert res.numProjectsContributed == 1
            assert res.totalSkills == 4
            assert res.totalBadges == 4
            assert res.numAchievedGemBadges == 0

            if (index < 2) {
                assert res.projectSummaries.find{ it.projectId == 'TestProject1' }.level == 1
            } else if (index < 4) {
                assert res.projectSummaries.find{ it.projectId == 'TestProject1' }.level == 2
            }

            if (index == 4) {
                assert res.projectSummaries.find{ it.projectId == 'TestProject1' }.level == 3
                assert res.numAchievedSkills == 1
                assert res.numAchievedSkillsLastMonth == 1
                assert res.numAchievedSkillsLastWeek == 0
                assert StringUtils.startsWith(res.mostRecentAchievedSkill, dates.last().format("yyyy-MM-dd'T'HH:mm:ss.SSS"))
                assert res.numAchievedGlobalBadges == 1
            } else {
                assert res.numAchievedSkills == 0
                assert res.numAchievedSkillsLastMonth == 0
                assert res.numAchievedSkillsLastWeek == 0
                assert res.mostRecentAchievedSkill == null
                assert res.numAchievedGlobalBadges == 0
            }
        }
    }

    def "my progress summary - create skills with different versions; only the correct skills are returned when filtered by version 1"() {
        String projId = SkillsFactory.defaultProjId
        List<Map> skills = SkillsFactory.createSkillsWithDifferentVersions([0, 0, 1, 1, 1, 2])
        skills.each{
            it.pointIncrement = 20
        }
        def subject = SkillsFactory.createSubject()

        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(subject)
        skillsService.createSkills(skills)
        skillsService.assignDependency([projectId: projId, skillId: skills.get(1).skillId, dependentSkillId: skills.get(0).skillId])
        skillsService.assignDependency([projectId: projId, skillId: skills.get(2).skillId, dependentSkillId: skills.get(1).skillId])
        skillsService.addSkill([projectId: projId, skillId: skills.get(0).skillId], userId, new Date())

        // enable "production mode"
        skillsService.changeSetting(projId, PROD_MODE, [projectId: projId, setting: PROD_MODE, value: "true"])

        when:
        def mySummary0 = skillsService.getMyProgressSummary(0)
        def mySummary1 = skillsService.getMyProgressSummary(1)
        def mySummary2 = skillsService.getMyProgressSummary(2)

        then:

        mySummary0
        mySummary0.projectSummaries
        mySummary0.projectSummaries.size() == 1
        mySummary0.projectSummaries.find{ it.projectId == projId }
        mySummary0.projectSummaries.find{ it.projectId == projId }.points == 20
        mySummary0.projectSummaries.find{ it.projectId == projId }.totalPoints == 40

        mySummary1
        mySummary1.projectSummaries
        mySummary1.projectSummaries.size() == 1
        mySummary1.projectSummaries.find{ it.projectId == projId }
        mySummary1.projectSummaries.find{ it.projectId == projId }.points == 20
        mySummary1.projectSummaries.find{ it.projectId == projId }.totalPoints == 100

        mySummary2
        mySummary2.projectSummaries
        mySummary2.projectSummaries.size() == 1
        mySummary2.projectSummaries.find{ it.projectId == projId }
        mySummary2.projectSummaries.find{ it.projectId == projId }.points == 20
        mySummary2.projectSummaries.find{ it.projectId == projId }.totalPoints == 120
    }

    def "my progress summary - user points DO NOT respect the version - if user earns those points they are proudly displayed in all versions"() {
        String projId = SkillsFactory.defaultProjId
        List<Map> skills = SkillsFactory.createSkillsWithDifferentVersions([0, 0, 1, 1, 1, 2])
        skills.each{
            it.pointIncrement = 20
        }
        def subject = SkillsFactory.createSubject()

        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(subject)
        skillsService.createSkills(skills)

        skillsService.addSkill([projectId: projId, skillId: skills.get(0).skillId], userId, new Date())
        skillsService.addSkill([projectId: projId, skillId: skills.get(1).skillId], userId, new Date() - 1)
        skillsService.addSkill([projectId: projId, skillId: skills.get(2).skillId], userId, new Date())
        skillsService.addSkill([projectId: projId, skillId: skills.get(3).skillId], userId, new Date() - 3)
        skillsService.addSkill([projectId: projId, skillId: skills.get(4).skillId], userId, new Date())
        skillsService.addSkill([projectId: projId, skillId: skills.get(5).skillId], userId, new Date() - 2)

        // enable "production mode"
        skillsService.changeSetting(projId, PROD_MODE, [projectId: projId, setting: PROD_MODE, value: "true"])

        when:
        def mySummary0 = skillsService.getMyProgressSummary(0)
        def mySummary1 = skillsService.getMyProgressSummary(1)
        def mySummary2 = skillsService.getMyProgressSummary(2)

        then:

        mySummary0
        mySummary0.projectSummaries
        mySummary0.projectSummaries.size() == 1
        mySummary0.projectSummaries.find{ it.projectId == projId }
        mySummary0.projectSummaries.find{ it.projectId == projId }.points == 120
        mySummary0.projectSummaries.find{ it.projectId == projId }.totalPoints == 40

        mySummary1
        mySummary1.projectSummaries
        mySummary1.projectSummaries.size() == 1
        mySummary1.projectSummaries.find{ it.projectId == projId }
        mySummary1.projectSummaries.find{ it.projectId == projId }.points == 120
        mySummary1.projectSummaries.find{ it.projectId == projId }.totalPoints == 100

        mySummary2
        mySummary2.projectSummaries
        mySummary2.projectSummaries.size() == 1
        mySummary2.projectSummaries.find{ it.projectId == projId }
        mySummary2.projectSummaries.find{ it.projectId == projId }.points == 120
        mySummary2.projectSummaries.find{ it.projectId == projId }.totalPoints == 120
    }

    def "my progress summary - user points to NOT respect the version (skills with numPerformToCompletion > 1) - if user ends those points they are proudly displayed in all versions"() {
        String projId = SkillsFactory.defaultProjId
        List<Map> skills = SkillsFactory.createSkillsWithDifferentVersions([0, 0, 1, 1, 1, 2])
        skills.each {
            it.numPerformToCompletion = 5
        }
        def subject = SkillsFactory.createSubject()

        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(subject)
        skillsService.createSkills(skills)

        skillsService.addSkill([projectId: projId, skillId: skills.get(0).skillId], userId, new Date())
        skillsService.addSkill([projectId: projId, skillId: skills.get(0).skillId], userId, new Date() - 1)
        skillsService.addSkill([projectId: projId, skillId: skills.get(0).skillId], userId, new Date() - 2)
        skillsService.addSkill([projectId: projId, skillId: skills.get(0).skillId], userId, new Date() - 3)
        skillsService.addSkill([projectId: projId, skillId: skills.get(0).skillId], userId, new Date() - 4)

        skillsService.addSkill([projectId: projId, skillId: skills.get(5).skillId], userId, new Date() - 2)
        skillsService.addSkill([projectId: projId, skillId: skills.get(5).skillId], userId, new Date() - 1)
        skillsService.addSkill([projectId: projId, skillId: skills.get(5).skillId], userId, new Date())

        // enable "production mode"
        skillsService.changeSetting(projId, PROD_MODE, [projectId: projId, setting: PROD_MODE, value: "true"])

        when:
        def mySummary0 = skillsService.getMyProgressSummary(0)
        def mySummary1 = skillsService.getMyProgressSummary(1)
        def mySummary2 = skillsService.getMyProgressSummary(2)

        then:

        mySummary0
        mySummary0.projectSummaries
        mySummary0.projectSummaries.size() == 1
        mySummary0.projectSummaries.find{ it.projectId == projId }
        mySummary0.projectSummaries.find{ it.projectId == projId }.points == 80
        mySummary0.projectSummaries.find{ it.projectId == projId }.totalPoints == 100

        mySummary1
        mySummary1.projectSummaries
        mySummary1.projectSummaries.size() == 1
        mySummary1.projectSummaries.find{ it.projectId == projId }
        mySummary1.projectSummaries.find{ it.projectId == projId }.points == 80
        mySummary1.projectSummaries.find{ it.projectId == projId }.totalPoints == 250

        mySummary2
        mySummary2.projectSummaries
        mySummary2.projectSummaries.size() == 1
        mySummary2.projectSummaries.find{ it.projectId == projId }
        mySummary2.projectSummaries.find{ it.projectId == projId }.points == 80
        mySummary2.projectSummaries.find{ it.projectId == projId }.totalPoints == 300
    }

    def "my skills summary - skills have been created and achieved, but 'production mode' is not enabled"() {
        def proj1 = SkillsFactory.createProject()
        def subj1 = SkillsFactory.createSubject()
        def skill1 = SkillsFactory.createSkill()
        def badge = SkillsFactory.createBadge()

        skillsService.createProject(proj1)
        skillsService.createSubject(subj1)
        skillsService.createSkill(skill1)
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge.badgeId, skillId: skill1.skillId])
        badge.enabled = 'true'
        skillsService.createBadge(badge)

        def globalBadge = [badgeId: "globalBadge", name: 'Test Global Badge 1', enabled: 'false']
        supervisorService.createGlobalBadge(globalBadge)
        supervisorService.assignSkillToGlobalBadge(projectId: proj1.projectId, badgeId: globalBadge.badgeId, skillId: skill1.skillId)
        globalBadge.enabled = 'true'
        supervisorService.createGlobalBadge(globalBadge)

        when:
        def res = skillsService.getMyProgressSummary()
        then:

        // "production mode" is not enabled, so proj1 should not be included in the results
        res
        res.projectSummaries.isEmpty()
        res.totalProjects == 1
        res.numProjectsContributed == 0
        res.totalSkills == 0
        res.numAchievedSkills == 0
        res.numAchievedSkillsLastMonth == 0
        res.numAchievedSkillsLastWeek == 0
        res.mostRecentAchievedSkill == null
        res.totalBadges == 1 // global badge is still included
        res.numAchievedGemBadges == 0
        res.numAchievedGlobalBadges == 0
    }
}
