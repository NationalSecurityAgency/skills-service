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

import groovy.time.TimeCategory
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.intTests.utils.TestUtils
import skills.quizLoading.QuizSettings
import skills.services.quiz.QuizQuestionType
import skills.services.settings.Settings

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

    def "my progress summary only includes My Projects - no projects"() {
        (1..3).collect {
            def project = SkillsFactory.createProject(it)
            skillsService.createProject(project)
            skillsService.enableProdMode(project)
        }

        when:
        def res = skillsService.getMyProgressSummary()
        then:
        !res.projectSummaries
    }


    def "my progress summary only includes My Projects"() {
        List projs = (1..3).collect {
            def project = SkillsFactory.createProject(it)
            skillsService.createProject(project)
            skillsService.enableProdMode(project)
            return project
        }
        skillsService.addMyProject(projs[0].projectId)
        skillsService.addMyProject(projs[2].projectId)

        when:
        def res = skillsService.getMyProgressSummary()

        then:
        res.projectSummaries.collect { it.projectId } == [projs[2].projectId, projs[0].projectId]
    }

    def "my progress - only enabled badges for projects in My Projects should be included in counts and My Badges" () {
        def proj1 = SkillsFactory.createProject()
        def subj1 = SkillsFactory.createSubject()

        def proj2 = SkillsFactory.createProject(2)
        def proj2subj1 = SkillsFactory.createSubject(2)
        def proj2Skills = SkillsFactory.createSkills(10, 2)

        def proj3 = SkillsFactory.createProject(3)
        def proj3subj1 = SkillsFactory.createSubject(3)
        def proj3Skills = SkillsFactory.createSkills(10, 3)

        // Project 1: 3 badges, 2 gems
        def badge1 = SkillsFactory.createBadge()
        badge1.enabled = false;
        def badge2 = SkillsFactory.createBadge(1, 2)
        badge2.enabled = false;
        def badge3 = SkillsFactory.createBadge(1, 3)
        badge3.enabled = false;

        Date oneWeekAgo = new Date()-7
        Date twoWeeksAgo = new Date()-14
        def gem1 = SkillsFactory.createBadge(1, 4)
        gem1.badgeId = "gem1"
        gem1.enabled = false;
        gem1.startDate = twoWeeksAgo
        gem1.endDate = oneWeekAgo
        def gem2 = SkillsFactory.createBadge(1, 5)
        gem2.badgeId = "gem2"
        gem2.enabled = false;
        gem2.startDate = twoWeeksAgo
        gem2.endDate = oneWeekAgo

        // Project 2: 2 badges, 0 gems
        def proj2badge1 = SkillsFactory.createBadge(2)
        proj2badge1.enabled = false
        def proj2badge2 = SkillsFactory.createBadge(2,2)
        proj2badge2.enabled = false

        // Project 3: 1 badge, 0 gems
        def proj3badge1 = SkillsFactory.createBadge(3)
        proj3badge1.enabled = false


        def globalBadge = [badgeId: "globalBadge", name: 'Test Global Badge 1', enabled: 'false']
        def globalBadge2 = [badgeId: "globalBadge2", name: 'Test Global Badge 2', enabled: 'false']
        def globalBadge3 = [badgeId: "globalBadge3", name: 'Test Global Badge 3', enabled: 'false']
        def globalBadge4 = [badgeId:"globalBadge4", name: 'Test Global Badge 4', enabled: 'false']

        def skills = SkillsFactory.createSkills(3)
        skillsService.createProject(proj1)
        skillsService.createSubject(subj1)

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2subj1)
        skillsService.createSkills(proj2Skills)

        skillsService.createProject(proj3)
        skillsService.createSubject(proj3subj1)
        skillsService.createSkills(proj3Skills)

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

        skillsService.createBadge(proj2badge1)
        skillsService.assignSkillToBadge([projectId: proj2.projectId, badgeId: proj2badge1.badgeId, skillId: proj2Skills[1].skillId])
        skillsService.createBadge(proj2badge2)
        skillsService.assignSkillToBadge([projectId: proj2.projectId, badgeId: proj2badge2.badgeId, skillId: proj2Skills[2].skillId])

        skillsService.createBadge(proj3badge1)
        skillsService.assignSkillToBadge([projectId: proj3.projectId, badgeId: proj3badge1.badgeId, skillId: proj3Skills[3].skillId])


        // globalBadge depends on proj1 skill
        supervisorService.createGlobalBadge(globalBadge)
        supervisorService.assignSkillToGlobalBadge(projectId: proj1.projectId, badgeId: globalBadge.badgeId, skillId: skills[2].skillId)

        // globalBadge2 depends on proj1 skill, proj3 level
        supervisorService.createGlobalBadge(globalBadge2)
        supervisorService.assignSkillToGlobalBadge(projectId: proj1.projectId, badgeId: globalBadge2.badgeId, skillId: skills[2].skillId)
        supervisorService.assignProjectLevelToGlobalBadge(projectId: proj3.projectId, badgeId: globalBadge2.badgeId, level: "1")

        // globalBadge3 depends on proj3 skill, proj2 skill
        supervisorService.createGlobalBadge(globalBadge3)
        supervisorService.assignSkillToGlobalBadge(projectId: proj3.projectId, badgeId: globalBadge3.badgeId, skillId: proj3Skills[1].skillId)
        supervisorService.assignSkillToGlobalBadge(projectId: proj2.projectId, badgeId: globalBadge3.badgeId, skillId: proj2Skills[1].skillId)

        // globalBadge4 only project2 level dependency
        supervisorService.createGlobalBadge(globalBadge4)
        supervisorService.assignProjectLevelToGlobalBadge(projectId: proj2.projectId, badgeId: globalBadge4.badgeId, level: "2")


        when:

        // should be empty
        def summaryBeforeProductionMode = skillsService.getMyProgressSummary()
        def myBadgesBeforeProductionMode = skillsService.getMyProgressBadges()
        // enable "production mode"
        skillsService.changeSetting(proj1.projectId, PROD_MODE, [projectId: proj1.projectId, setting: PROD_MODE, value: "true"])
        skillsService.changeSetting(proj2.projectId, PROD_MODE, [projectId: proj2.projectId, setting: PROD_MODE, value: "true"])
        skillsService.changeSetting(proj3.projectId, PROD_MODE, [projectId: proj3.projectId, setting: PROD_MODE, value: "true"])

        // should be empty
        def summaryAfterProductionMode = skillsService.getMyProgressSummary()
        def myBadgesAfterProductionMode = skillsService.getMyProgressBadges()

        skillsService.addMyProject(proj1.projectId)
        // badge count should be empty until the badges are enabled
        def afterAddMyProjectsProj1 = skillsService.getMyProgressSummary()
        def myBadgesAfterAddMyProjectProj1 = skillsService.getMyProgressBadges()

        badge1.enabled = true
        badge1.iconClass = "fakeityfakefake"
        badge1.description = "a description"
        badge1.helpUrl = "http://fakeityurlfakeity"
        skillsService.createBadge(badge1)

        // expect these to only contain badge1 and no other proj1 badges for both counts and badges returned
        def summaryAfterBadge1Enabled = skillsService.getMyProgressSummary()
        def myBadgesAfterBadge1Enabled = skillsService.getMyProgressBadges()

        assert skillsService.addSkill([projectId: proj2.projectId, skillId: proj2Skills[1].skillId], userId, new Date()).body.completed.find { it.type == "Skill"}

        // enable all badges, only badges for proj1 or global badges with proj1 dependencies should be included in counts/returned
        badge2.enabled = true
        skillsService.createBadge(badge2)
        badge3.enabled = true
        skillsService.createBadge(badge3)
        gem1.enabled = true
        skillsService.createBadge(gem1)
        gem2.enabled = true
        skillsService.createBadge(gem2)
        proj2badge1.enabled = true
        skillsService.createBadge(proj2badge1)
        proj2badge2.enabled = true
        skillsService.createBadge(proj2badge2)
        proj3badge1.enabled = true
        skillsService.createBadge(proj3badge1)
        globalBadge.enabled = true
        supervisorService.createGlobalBadge(globalBadge)
        globalBadge2.enabled = true
        supervisorService.createGlobalBadge(globalBadge2)
        globalBadge3.enabled = true
        supervisorService.createGlobalBadge(globalBadge3)
        globalBadge4.enabled = true
        supervisorService.createGlobalBadge(globalBadge4)

        def summaryAfterAllEnabled = skillsService.getMyProgressSummary()
        def myBadgesAfterAllEnabled = skillsService.getMyProgressBadges()

        skillsService.removeMyProject(proj1.projectId)
        skillsService.addMyProject(proj2.projectId)

        def summaryAfterProj2 = skillsService.getMyProgressSummary()
        def myBadgesAfterProj2 = skillsService.getMyProgressBadges()

        then:
        summaryBeforeProductionMode.totalBadges == 0
        summaryBeforeProductionMode.gemCount == 0
        summaryBeforeProductionMode.globalBadgeCount == 0
        !myBadgesBeforeProductionMode

        summaryAfterProductionMode.totalBadges == 0
        summaryAfterProductionMode.gemCount == 0
        summaryAfterProductionMode.globalBadgeCount == 0
        !myBadgesAfterProductionMode

        summaryAfterBadge1Enabled.totalBadges == 1
        summaryAfterBadge1Enabled.gemCount == 0
        summaryAfterBadge1Enabled.globalBadgeCount == 0
        myBadgesAfterBadge1Enabled.size() == 1
        myBadgesAfterBadge1Enabled.find { it.badgeId == badge1.badgeId }

        summaryAfterAllEnabled.totalBadges == 7
        summaryAfterAllEnabled.gemCount == 2
        summaryAfterAllEnabled.globalBadgeCount == 2
        myBadgesAfterAllEnabled.size() == 7
        myBadgesAfterAllEnabled.find {
            it.badgeId == badge1.badgeId &&
                    it.projectId == proj1.projectId &&
                    it.description == badge1.description &&
                    it.helpUrl == badge1.helpUrl &&
                    it.iconClass == badge1.iconClass &&
                    it.numSkillsAchieved == 0 &&
                    it.numTotalSkills == 1
        }
        myBadgesAfterAllEnabled.find { it.badgeId == badge2.badgeId && it.projectId == proj1.projectId }
        myBadgesAfterAllEnabled.find { it.badgeId == badge3.badgeId && it.projectId == proj1.projectId }
        myBadgesAfterAllEnabled.find { it.badgeId == gem1.badgeId && it.startDate && it.endDate && it.gem }
        myBadgesAfterAllEnabled.find { it.badgeId == gem2.badgeId && it.startDate && it.endDate && it.gem  }
        myBadgesAfterAllEnabled.find { it.badgeId == globalBadge.badgeId && it.global }
        myBadgesAfterAllEnabled.find { it.badgeId == globalBadge2.badgeId && it.global }

        summaryAfterProj2.totalBadges == 4
        summaryAfterProj2.gemCount == 0
        summaryAfterProj2.globalBadgeCount == 2
        summaryAfterProj2.numAchievedBadges == 1
        myBadgesAfterProj2.size() == 4
        myBadgesAfterProj2.find { it.badgeId == proj2badge1.badgeId && it.badgeAchieved }
        myBadgesAfterProj2.find { it.badgeId == proj2badge2.badgeId && !it.badgeAchieved }
        myBadgesAfterProj2.find { it.badgeId == globalBadge3.badgeId && !it.badgeAchieved }
        myBadgesAfterProj2.find { it.badgeId == globalBadge3.badgeId }.projectLevelsAndSkillsSummaries.find { it.projectId == proj3.projectId && it.skills.find { it.skillId == proj3Skills[1].skillId }}
        myBadgesAfterProj2.find { it.badgeId == globalBadge3.badgeId }.projectLevelsAndSkillsSummaries.find { it.projectId == proj2.projectId && it.skills.find { it.skillId == proj2Skills[1].skillId }}
        myBadgesAfterProj2.find { it.badgeId == globalBadge4.badgeId && !it.badgeAchieved }
        myBadgesAfterProj2.find { it.badgeId == globalBadge4.badgeId }.projectLevelsAndSkillsSummaries.find { it.projectId == proj2.projectId && it.projectLevel.projectId == proj2.projectId && it.projectLevel.requiredLevel == 2 }
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
        skillsService.addMyProject(proj1.projectId)

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

    def "skills are only counted from My Projects "() {
        List skills = []
        List projs = (1..3).collect { int projNum ->
            def project = SkillsFactory.createProject(projNum)
            skillsService.createProject(project)
            skillsService.enableProdMode(project)

            skillsService.createSubject(SkillsFactory.createSubject(projNum, 1))
            def skillsForProj = SkillsFactory.createSkills(projNum, projNum, 1, 200)
            skillsService.createSkills(skillsForProj)

            skills.addAll(skillsForProj)

            return project
        }
        skillsService.addMyProject(projs[0].projectId)
        skillsService.addMyProject(projs[2].projectId)

        assert skillsService.addSkill([projectId: projs[1].projectId, skillId: skills[1].skillId], userId, new Date()).body.completed.find { it.type == "Skill"}

        assert skillsService.addSkill([projectId: projs[1].projectId, skillId: skills[2].skillId], userId, new Date()).body.completed.find { it.type == "Skill"}
        assert skillsService.addSkill([projectId: projs[2].projectId, skillId: skills[3].skillId], userId, new Date()).body.completed.find { it.type == "Skill"}

        when:
        def res = skillsService.getMyProgressSummary()

        use(TimeCategory) {
            assert skillsService.addSkill([projectId: projs[0].projectId, skillId: skills[0].skillId], userId, 2.months.ago).body.completed.find { it.type == "Skill"}
            assert skillsService.addSkill([projectId: projs[2].projectId, skillId: skills[4].skillId], userId, 2.weeks.ago).body.completed.find { it.type == "Skill"}
        }

        def res1 = skillsService.getMyProgressSummary()

        then:
        res.totalSkills == 4
        res.numAchievedSkills == 1
        res.numAchievedSkillsLastMonth == 1
        res.numAchievedSkillsLastWeek == 1
        res.mostRecentAchievedSkill

        res1.totalSkills == 4
        res1.numAchievedSkills == 3
        res1.numAchievedSkillsLastMonth == 2
        res1.numAchievedSkillsLastWeek == 1
        res1.mostRecentAchievedSkill
    }

    def "skills are only counted from projects in the production mode unless they are part of My Projects"() {
        List skills = []
        List projs = (1..3).collect { int projNum ->
            def project = SkillsFactory.createProject(projNum)
            skillsService.createProject(project)
            skillsService.enableProdMode(project)
            skillsService.addMyProject(project.projectId)

            skillsService.createSubject(SkillsFactory.createSubject(projNum, 1))
            def skillsForProj = SkillsFactory.createSkills(projNum, projNum, 1, 200)
            skillsService.createSkills(skillsForProj)

            skills.addAll(skillsForProj)

            return project
        }

        skillsService.disableProdMode(projs[1])
        def initialRes = skillsService.getMyProgressSummary()
        skillsService.removeMyProject(projs[1].projectId)
        assert skillsService.addSkill([projectId: projs[1].projectId, skillId: skills[1].skillId], userId, new Date()).body.completed.find { it.type == "Skill"}

        assert skillsService.addSkill([projectId: projs[1].projectId, skillId: skills[2].skillId], userId, new Date()).body.completed.find { it.type == "Skill"}
        assert skillsService.addSkill([projectId: projs[2].projectId, skillId: skills[3].skillId], userId, new Date()).body.completed.find { it.type == "Skill"}

        when:
        def res = skillsService.getMyProgressSummary()

        use(TimeCategory) {
            assert skillsService.addSkill([projectId: projs[0].projectId, skillId: skills[0].skillId], userId, 2.months.ago).body.completed.find { it.type == "Skill"}
            assert skillsService.addSkill([projectId: projs[2].projectId, skillId: skills[4].skillId], userId, 2.weeks.ago).body.completed.find { it.type == "Skill"}
        }

        def res1 = skillsService.getMyProgressSummary()

        then:
        initialRes.totalSkills == 6
        initialRes.numAchievedSkills == 0
        initialRes.numAchievedSkillsLastMonth == 0
        initialRes.numAchievedSkillsLastWeek == 0
        !initialRes.mostRecentAchievedSkill

        res.totalSkills == 4
        res.numAchievedSkills == 1
        res.numAchievedSkillsLastMonth == 1
        res.numAchievedSkillsLastWeek == 1
        res.mostRecentAchievedSkill

        res1.totalSkills == 4
        res1.numAchievedSkills == 3
        res1.numAchievedSkillsLastMonth == 2
        res1.numAchievedSkillsLastWeek == 1
        res1.mostRecentAchievedSkill
    }


    def "skills are counted from private projects"() {
        List skills = []
        List projs = (1..3).collect { int projNum ->
            def project = SkillsFactory.createProject(projNum)
            skillsService.createProject(project)
            skillsService.enableProdMode(project)
            skillsService.addMyProject(project.projectId)

            skillsService.createSubject(SkillsFactory.createSubject(projNum, 1))
            def skillsForProj = SkillsFactory.createSkills(projNum, projNum, 1, 200)
            skillsService.createSkills(skillsForProj)

            skills.addAll(skillsForProj)

            return project
        }

        skillsService.disableProdMode(projs[1])
        skillsService.configuredProjectAsInviteOnly(projs[1].projectId)
        assert skillsService.addSkill([projectId: projs[1].projectId, skillId: skills[1].skillId], userId, new Date()).body.completed.find { it.type == "Skill"}

        assert skillsService.addSkill([projectId: projs[1].projectId, skillId: skills[2].skillId], userId, new Date()).body.completed.find { it.type == "Skill"}
        assert skillsService.addSkill([projectId: projs[2].projectId, skillId: skills[3].skillId], userId, new Date()).body.completed.find { it.type == "Skill"}

        when:
        def res = skillsService.getMyProgressSummary()

        then:
        res.totalSkills == 6
        res.numAchievedSkills == 3
        res.numAchievedSkillsLastMonth == 3
        res.numAchievedSkillsLastWeek == 3
        res.mostRecentAchievedSkill
    }

    def "numProjectsContributed are only counted from My Projects "() {
        List skills = []
        List projs = (1..3).collect { int projNum ->
            def project = SkillsFactory.createProject(projNum)
            skillsService.createProject(project)
            skillsService.enableProdMode(project)

            skillsService.createSubject(SkillsFactory.createSubject(projNum, 1))
            def skillsForProj = SkillsFactory.createSkills(projNum, projNum, 1, 200)
            skillsService.createSkills(skillsForProj)

            skills.addAll(skillsForProj)

            return project
        }
        skillsService.addMyProject(projs[0].projectId)
        skillsService.addMyProject(projs[2].projectId)

        when:
        assert skillsService.addSkill([projectId: projs[1].projectId, skillId: skills[1].skillId], userId, new Date()).body.completed.find { it.type == "Skill"}
        def res = skillsService.getMyProgressSummary()

        assert skillsService.addSkill([projectId: projs[1].projectId, skillId: skills[2].skillId], userId, new Date()).body.completed.find { it.type == "Skill"}
        assert skillsService.addSkill([projectId: projs[2].projectId, skillId: skills[3].skillId], userId, new Date()).body.completed.find { it.type == "Skill"}
        def res1 = skillsService.getMyProgressSummary()

        use(TimeCategory) {
            assert skillsService.addSkill([projectId: projs[0].projectId, skillId: skills[0].skillId], userId, 2.months.ago).body.completed.find { it.type == "Skill"}
            assert skillsService.addSkill([projectId: projs[2].projectId, skillId: skills[4].skillId], userId, 2.weeks.ago).body.completed.find { it.type == "Skill"}
        }
        def res2 = skillsService.getMyProgressSummary()

        then:
        res.numProjectsContributed == 0
        res1.numProjectsContributed == 1
        res2.numProjectsContributed == 2
    }


    def "numProjectsContributed counts projects that are part of My Projects even if hidden"() {
        List skills = []
        List projs = (1..3).collect { int projNum ->
            def project = SkillsFactory.createProject(projNum)
            skillsService.createProject(project)
            skillsService.enableProdMode(project)
            skillsService.addMyProject(project.projectId)

            skillsService.createSubject(SkillsFactory.createSubject(projNum, 1))
            def skillsForProj = SkillsFactory.createSkills(projNum, projNum, 1, 200)
            skillsService.createSkills(skillsForProj)

            skills.addAll(skillsForProj)

            return project
        }
        skillsService.disableProdMode(projs[1])
        when:
        assert skillsService.addSkill([projectId: projs[1].projectId, skillId: skills[1].skillId], userId, new Date()).body.completed.find { it.type == "Skill"}
        def res = skillsService.getMyProgressSummary()

        assert skillsService.addSkill([projectId: projs[1].projectId, skillId: skills[2].skillId], userId, new Date()).body.completed.find { it.type == "Skill"}
        assert skillsService.addSkill([projectId: projs[2].projectId, skillId: skills[3].skillId], userId, new Date()).body.completed.find { it.type == "Skill"}
        def res1 = skillsService.getMyProgressSummary()

        use(TimeCategory) {
            assert skillsService.addSkill([projectId: projs[0].projectId, skillId: skills[0].skillId], userId, 2.months.ago).body.completed.find { it.type == "Skill"}
            assert skillsService.addSkill([projectId: projs[2].projectId, skillId: skills[4].skillId], userId, 2.weeks.ago).body.completed.find { it.type == "Skill"}
        }
        def res2 = skillsService.getMyProgressSummary()

        then:
        res.numProjectsContributed == 1
        res1.numProjectsContributed == 2
        res2.numProjectsContributed == 3
    }

    def "badges are only counted from My Projects "() {
        def skills = []
        List projs = (1..3).collect { int projNum ->
            def project = SkillsFactory.createProject(projNum)
            skillsService.createProject(project)
            skillsService.enableProdMode(project)

            skillsService.createSubject(SkillsFactory.createSubject(projNum, 1))
            def skillsForProj = SkillsFactory.createSkills(5, projNum, 1, 200)
            skillsService.createSkills(skillsForProj)
            (1..projNum).each {
                def badge = SkillsFactory.createBadge(projNum, it)
                badge.enabled = true
                skillsService.createBadge(badge)
                skillsService.assignSkillToBadge([projectId: project.projectId, badgeId: badge.badgeId, skillId: skillsForProj[it].skillId])
                skills.add(skillsForProj[it])
                badge.enabled  = 'true'
                skillsService.updateBadge(badge, badge.badgeId)
            }

            return project
        }

        skillsService.addMyProject(projs[0].projectId)
        skillsService.addMyProject(projs[2].projectId)

        assert skillsService.addSkill([projectId: projs[1].projectId, skillId: skills[1].skillId], userId, new Date()).body.completed.find { it.type == "Badge"}
        assert skillsService.addSkill([projectId: projs[1].projectId, skillId: skills[2].skillId], userId, new Date()).body.completed.find { it.type == "Badge"}

        assert skillsService.addSkill([projectId: projs[2].projectId, skillId: skills[3].skillId], userId, new Date()).body.completed.find { it.type == "Badge"}
        when:
        def res = skillsService.getMyProgressSummary()


        assert skillsService.addSkill([projectId: projs[0].projectId, skillId: skills[0].skillId], userId, new Date()).body.completed.find { it.type == "Badge"}
        assert skillsService.addSkill([projectId: projs[2].projectId, skillId: skills[4].skillId], userId, new Date()).body.completed.find { it.type == "Badge"}

        def res1 = skillsService.getMyProgressSummary()
        then:
        res.totalBadges == 4
        res.gemCount == 0
        res.globalBadgeCount == 0
        res.numAchievedBadges == 1
        res.numAchievedGemBadges == 0
        res.numAchievedGlobalBadges == 0

        res1.totalBadges == 4
        res1.gemCount == 0
        res1.globalBadgeCount == 0
        res1.numAchievedBadges == 3
        res1.numAchievedGemBadges == 0
        res1.numAchievedGlobalBadges == 0
    }

    def "badges are only counted from projects in the production mode even if they are part of My Projects"() {
        def skills = []
        List projs = (1..3).collect { int projNum ->
            def project = SkillsFactory.createProject(projNum)
            skillsService.createProject(project)
            skillsService.enableProdMode(project)
            skillsService.addMyProject(project.projectId)

            skillsService.createSubject(SkillsFactory.createSubject(projNum, 1))
            def skillsForProj = SkillsFactory.createSkills(5, projNum, 1, 200)
            skillsService.createSkills(skillsForProj)
            (1..projNum).each {
                def badge = SkillsFactory.createBadge(projNum, it)
                skillsService.createBadge(badge)
                skillsService.assignSkillToBadge([projectId: project.projectId, badgeId: badge.badgeId, skillId: skillsForProj[it].skillId])
                skills.add(skillsForProj[it])
                badge.enabled  = 'true'
                skillsService.updateBadge(badge, badge.badgeId)
            }

            return project
        }

        assert skillsService.addSkill([projectId: projs[1].projectId, skillId: skills[1].skillId], userId, new Date()).body.completed.find { it.type == "Badge"}
        assert skillsService.addSkill([projectId: projs[1].projectId, skillId: skills[2].skillId], userId, new Date()).body.completed.find { it.type == "Badge"}

        assert skillsService.addSkill([projectId: projs[2].projectId, skillId: skills[3].skillId], userId, new Date()).body.completed.find { it.type == "Badge"}

        skillsService.removeMyProject(projs[1].projectId)
        when:
        def res = skillsService.getMyProgressSummary()

        assert skillsService.addSkill([projectId: projs[0].projectId, skillId: skills[0].skillId], userId, new Date()).body.completed.find { it.type == "Badge"}
        assert skillsService.addSkill([projectId: projs[2].projectId, skillId: skills[4].skillId], userId, new Date()).body.completed.find { it.type == "Badge"}

        def res1 = skillsService.getMyProgressSummary()
        then:
        res.totalBadges == 4
        res.gemCount == 0
        res.globalBadgeCount == 0
        res.numAchievedBadges == 1
        res.numAchievedGemBadges == 0
        res.numAchievedGlobalBadges == 0

        res1.totalBadges == 4
        res1.gemCount == 0
        res1.globalBadgeCount == 0
        res1.numAchievedBadges == 3
        res1.numAchievedGemBadges == 0
        res1.numAchievedGlobalBadges == 0
    }

    def "gems are only counted from My Projects "() {
        def skills = []
        List projs = (1..3).collect { int projNum ->
            def project = SkillsFactory.createProject(projNum)
            skillsService.createProject(project)
            skillsService.enableProdMode(project)

            skillsService.createSubject(SkillsFactory.createSubject(projNum, 1))
            def skillsForProj = SkillsFactory.createSkills(5, projNum, 1, 200)
            skillsService.createSkills(skillsForProj)
            (1..projNum).each {
                def gem1 = SkillsFactory.createBadge(projNum, it)
                gem1.startDate = new Date()-7
                gem1.endDate = new Date()+7

                skillsService.createBadge(gem1)
                skillsService.assignSkillToBadge([projectId: project.projectId, badgeId: gem1.badgeId, skillId: skillsForProj[it].skillId])
                skills.add(skillsForProj[it])
                gem1.enabled  = 'true'
                skillsService.updateBadge(gem1, gem1.badgeId)
            }

            return project
        }
        skillsService.addMyProject(projs[0].projectId)
        skillsService.addMyProject(projs[2].projectId)


        assert skillsService.addSkill([projectId: projs[1].projectId, skillId: skills[1].skillId], userId, new Date()).body.completed.find { it.type == "Badge"}
        assert skillsService.addSkill([projectId: projs[1].projectId, skillId: skills[2].skillId], userId, new Date()).body.completed.find { it.type == "Badge"}

        assert skillsService.addSkill([projectId: projs[2].projectId, skillId: skills[3].skillId], userId, new Date()).body.completed.find { it.type == "Badge"}
        when:
        def res = skillsService.getMyProgressSummary()

        assert skillsService.addSkill([projectId: projs[0].projectId, skillId: skills[0].skillId], userId, new Date()).body.completed.find { it.type == "Badge"}
        assert skillsService.addSkill([projectId: projs[2].projectId, skillId: skills[4].skillId], userId, new Date()).body.completed.find { it.type == "Badge"}

        def res1 = skillsService.getMyProgressSummary()
        then:
        res.totalBadges == 4
        res.gemCount == 4
        res.globalBadgeCount == 0
        res.numAchievedBadges == 1
        res.numAchievedGemBadges == 1
        res.numAchievedGlobalBadges == 0

        res1.totalBadges == 4
        res1.gemCount == 4
        res1.globalBadgeCount == 0
        res1.numAchievedBadges == 3
        res1.numAchievedGemBadges == 3
        res1.numAchievedGlobalBadges == 0
    }

    def "gems are only counted from projects in the production mode even if they are part of My Projects"() {
        def skills = []
        List projs = (1..3).collect { int projNum ->
            def project = SkillsFactory.createProject(projNum)
            skillsService.createProject(project)
            skillsService.enableProdMode(project)
            skillsService.addMyProject(project.projectId)

            skillsService.createSubject(SkillsFactory.createSubject(projNum, 1))
            def skillsForProj = SkillsFactory.createSkills(5, projNum, 1, 200)
            skillsService.createSkills(skillsForProj)
            (1..projNum).each {
                def gem1 = SkillsFactory.createBadge(projNum, it)
                gem1.startDate = new Date()-7
                gem1.endDate = new Date()+7

                skillsService.createBadge(gem1)
                skillsService.assignSkillToBadge([projectId: project.projectId, badgeId: gem1.badgeId, skillId: skillsForProj[it].skillId])
                skills.add(skillsForProj[it])
                gem1.enabled  = 'true'
                skillsService.updateBadge(gem1, gem1.badgeId)
            }

            return project
        }

        assert skillsService.addSkill([projectId: projs[1].projectId, skillId: skills[1].skillId], userId, new Date()).body.completed.find { it.type == "Badge"}
        assert skillsService.addSkill([projectId: projs[1].projectId, skillId: skills[2].skillId], userId, new Date()).body.completed.find { it.type == "Badge"}

        assert skillsService.addSkill([projectId: projs[2].projectId, skillId: skills[3].skillId], userId, new Date()).body.completed.find { it.type == "Badge"}

        skillsService.disableProdMode(projs[1])
        when:
        def res = skillsService.getMyProgressSummary()

        assert skillsService.addSkill([projectId: projs[0].projectId, skillId: skills[0].skillId], userId, new Date()).body.completed.find { it.type == "Badge"}
        assert skillsService.addSkill([projectId: projs[2].projectId, skillId: skills[4].skillId], userId, new Date()).body.completed.find { it.type == "Badge"}

        def res1 = skillsService.getMyProgressSummary()
        then:
        res.totalBadges == 4
        res.gemCount == 4
        res.globalBadgeCount == 0
        res.numAchievedBadges == 1
        res.numAchievedGemBadges == 1
        res.numAchievedGlobalBadges == 0

        res1.totalBadges == 4
        res1.gemCount == 4
        res1.globalBadgeCount == 0
        res1.numAchievedBadges == 3
        res1.numAchievedGemBadges == 3
        res1.numAchievedGlobalBadges == 0
    }

    def "global badges counts should only relate to projects selected for My Projects"() {
        def skills = []
        List projs = (1..3).collect { int projNum ->
            def project = SkillsFactory.createProject(projNum)
            skillsService.createProject(project)
            skillsService.enableProdMode(project)

            skillsService.createSubject(SkillsFactory.createSubject(projNum, 1))
            def skillsForProj = SkillsFactory.createSkills(5, projNum, 1, 200)
            skillsService.createSkills(skillsForProj)
            (1..projNum).each {
                def globalBadge = [badgeId: "globalBadge${projNum}${it}".toString(), name: "Test Global Badge ${projNum}${it}".toString(), enabled: "true"]
                supervisorService.createGlobalBadge(globalBadge)
                supervisorService.assignSkillToGlobalBadge(projectId: project.projectId, badgeId: globalBadge.badgeId, skillId: skillsForProj[it].skillId)
                skills.add(skillsForProj[it])
                globalBadge.enabled  = 'true'
                supervisorService.updateGlobalBadge(globalBadge, globalBadge.badgeId)
            }
            return project
        }
        skillsService.addMyProject(projs[0].projectId)
        skillsService.addMyProject(projs[2].projectId)

        assert skillsService.addSkill([projectId: projs[1].projectId, skillId: skills[1].skillId], userId, new Date()).body.completed.find { it.type == "GlobalBadge"}
        assert skillsService.addSkill([projectId: projs[1].projectId, skillId: skills[2].skillId], userId, new Date()).body.completed.find { it.type == "GlobalBadge"}
        assert skillsService.addSkill([projectId: projs[2].projectId, skillId: skills[3].skillId], userId, new Date()).body.completed.find { it.type == "GlobalBadge"}

        when:
        def res = skillsService.getMyProgressSummary()

        assert skillsService.addSkill([projectId: projs[0].projectId, skillId: skills[0].skillId], userId, new Date()).body.completed.find { it.type == "GlobalBadge"}
        assert skillsService.addSkill([projectId: projs[2].projectId, skillId: skills[4].skillId], userId, new Date()).body.completed.find { it.type == "GlobalBadge"}

        def res1 = skillsService.getMyProgressSummary()
        then:
        res.totalBadges == 4
        res.gemCount == 0
        res.globalBadgeCount == 4
        res.numAchievedBadges == 1
        res.numAchievedGemBadges == 0
        res.numAchievedGlobalBadges == 1

        res1.totalBadges == 4
        res1.gemCount == 0
        res1.globalBadgeCount == 4
        res1.numAchievedBadges == 3
        res1.numAchievedGemBadges == 0
        res1.numAchievedGlobalBadges == 3
    }

    def "my progress summary - global badge achieved count"() {
        def proj1 = SkillsFactory.createProject()
        def subj1 = SkillsFactory.createSubject()

        def skills = SkillsFactory.createSkills(3, )
        skills.each { it.pointIncrement = 100 }
        skillsService.createProject(proj1)
        skillsService.enableProdMode(proj1)
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

        skillsService.addMyProject(proj1.projectId)

        def res1 = skillsService.getMyProgressSummary()

        then:
        res.globalBadgeCount == 0
        res.numAchievedGlobalBadges == 0
        res1.globalBadgeCount == 3
        res1.numAchievedGlobalBadges == 1
    }

    def "my progress summary - no skills have been created"() {
        def proj1 = SkillsFactory.createProject()
        def subj1 = SkillsFactory.createSubject()

        skillsService.createProject(proj1)
        skillsService.createSubject(subj1)

        // enable "production mode"
        skillsService.changeSetting(proj1.projectId, PROD_MODE, [projectId: proj1.projectId, setting: PROD_MODE, value: "true"])
        skillsService.addMyProject(proj1.projectId)

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
        skillsService.addMyProject(proj1.projectId)

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
        skillsService.addMyProject(projId)

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
        skillsService.addMyProject(projId)

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
        skillsService.addMyProject(projId2)

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

        skillsService.enableProdMode(proj1)
        skillsService.addMyProject(proj1.projectId)

        def res1 = skillsService.getMyProgressSummary()
        then:

        // "production mode" is not enabled, so proj1 should not be included in the results
        res
        res.projectSummaries.isEmpty()
        res.numProjectsContributed == 0
        res.totalSkills == 0
        res.numAchievedSkills == 0
        res.numAchievedSkillsLastMonth == 0
        res.numAchievedSkillsLastWeek == 0
        res.mostRecentAchievedSkill == null
        res.totalBadges == 0 // global badge should only be included if a dependency is in a project that has been added to My Projects
        res.numAchievedGemBadges == 0
        res.numAchievedGlobalBadges == 0

        res1.numProjectsContributed == 0
        res1.totalSkills == 1
        res1.numAchievedSkills == 0
        res1.numAchievedSkillsLastMonth == 0
        res1.numAchievedSkillsLastWeek == 0
        res1.mostRecentAchievedSkill == null
        res1.totalBadges == 2 // global badge should only be included if a dependency is in a project that has been added to My Projects
        res1.numAchievedGemBadges == 0
        res1.numAchievedGlobalBadges == 0
    }

    def "lookup project name for my project" () {
        def proj1 = SkillsFactory.createProject()
        def subj1 = SkillsFactory.createSubject()
        def skill1 = SkillsFactory.createSkill()
        def badge = SkillsFactory.createBadge()

        skillsService.createProject(proj1)
        skillsService.enableProdMode(proj1)
        skillsService.addMyProject(proj1.projectId)

        when:
        def res = skillsService.lookupMyProjectName(proj1.projectId)

        then:
        res.projectId == proj1.projectId
        res.name == proj1.name
    }

    def "lookup project name for project not in user's my projects" () {
        def proj1 = SkillsFactory.createProject()
        def subj1 = SkillsFactory.createSubject()
        def skill1 = SkillsFactory.createSkill()
        def badge = SkillsFactory.createBadge()
        def proj2 = SkillsFactory.createProject(2)

        skillsService.createProject(proj1)
        skillsService.createProject(proj2)
        skillsService.enableProdMode(proj1)
        skillsService.addMyProject(proj1.projectId)

        when:
        def res = skillsService.lookupMyProjectName(proj2.projectId)

        then:
        res.projectId == proj2.projectId
        res.name == proj2.name
    }

    def "my progress summary - quizzes and surveys"() {
        def declareQuiz = { Integer num, boolean isSurvey = false ->
            def quiz = isSurvey ? QuizDefFactory.createQuizSurvey(num) : QuizDefFactory.createQuiz(num)
            skillsService.createQuizDef(quiz)
            def questions = [
                  isSurvey ? QuizDefFactory.createSingleChoiceSurveyQuestion(num) :  QuizDefFactory.createChoiceQuestion(num, 1, 4, QuizQuestionType.SingleChoice)
            ]
            skillsService.createQuizQuestionDefs(questions)
            skillsService.saveQuizSettings(quiz.quizId, [
                    [setting: QuizSettings.MultipleTakes.setting, value: true],
            ])
            return quiz.quizId
        }
        def runQuiz = { String quizId, boolean completeQuiz = true ->
            def quizAttempt =  skillsService.startQuizAttempt(quizId).body
            skillsService.reportQuizAnswer(quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id)
            if (completeQuiz) {
                skillsService.completeQuizAttempt(quizId, quizAttempt.id)
            }
        }

        def quiz1Id = declareQuiz(1)
        def quiz2Id = declareQuiz(2)
        def quiz3Id = declareQuiz(3)
        def quiz4Id = declareQuiz(4)

        def survey1Id = declareQuiz(5, true)
        def survey2Id = declareQuiz(6, true)
        def survey3Id = declareQuiz(7, true)

        when:
        def summary_t1 = skillsService.getMyProgressSummary()
        runQuiz(quiz1Id)
        def summary_t2 = skillsService.getMyProgressSummary()
        runQuiz(quiz2Id)
        runQuiz(quiz3Id)
        runQuiz(quiz4Id)
        def summary_t3 = skillsService.getMyProgressSummary()
        runQuiz(quiz1Id)
        runQuiz(quiz2Id)
        def summary_t4 = skillsService.getMyProgressSummary()
        runQuiz(survey1Id)
        def summary_t5 = skillsService.getMyProgressSummary()
        runQuiz(survey2Id)
        runQuiz(survey3Id)
        def summary_t6 = skillsService.getMyProgressSummary()
        runQuiz(survey2Id)
        runQuiz(survey3Id)
        def summary_t7 = skillsService.getMyProgressSummary()

        // not completed attempts
        runQuiz(quiz1Id, false)
        runQuiz(quiz2Id, false)
        runQuiz(survey1Id, false)
        runQuiz(survey2Id, false)
        runQuiz(survey3Id, false)
        def summary_t8 = skillsService.getMyProgressSummary()

        then:
        summary_t1.numQuizAttempts == 0
        summary_t1.numSurveyAttempts == 0

        summary_t2.numQuizAttempts == 1
        summary_t2.numSurveyAttempts == 0

        summary_t3.numQuizAttempts == 4
        summary_t3.numSurveyAttempts == 0

        summary_t4.numQuizAttempts == 6
        summary_t4.numSurveyAttempts == 0

        summary_t5.numQuizAttempts == 6
        summary_t5.numSurveyAttempts == 1

        summary_t6.numQuizAttempts == 6
        summary_t6.numSurveyAttempts == 3

        summary_t7.numQuizAttempts == 6
        summary_t7.numSurveyAttempts == 5

        summary_t8.numQuizAttempts == 6
        summary_t8.numSurveyAttempts == 5
    }

}

