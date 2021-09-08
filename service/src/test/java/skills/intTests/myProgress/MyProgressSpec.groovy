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

import groovy.json.JsonOutput
import groovy.time.TimeCategory
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle
import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.intTests.utils.TestUtils
import skills.services.settings.Settings
import skills.storage.repos.UserAchievedLevelRepo
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

    def "skills are only counted from projects in the production mode even if they are part of My Projects"() {
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


    def "numProjectsContributed  are only counted from projects in the production mode even if they are part of My Projects"() {
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
        res.numProjectsContributed == 0
        res1.numProjectsContributed == 1
        res2.numProjectsContributed == 2
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

}

