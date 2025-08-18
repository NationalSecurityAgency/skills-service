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
package skills.intTests.clientDisplay

import groovy.time.TimeCategory
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.storage.model.SkillDef
import skills.storage.model.UserAchievement

class ClientDisplayBadgesSpec extends DefaultIntSpec {

    def "badges summary for a project - one badge"() {
        String userId = "user1"

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        String badge1 = "badge1"
        Map badge = [projectId: proj1.projectId, badgeId: badge1, name: 'Badge 1', description: 'This is a first badge', iconClass: "fa fa-seleted-icon",]
        skillsService.addBadge(badge)
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge1, skillId: proj1_skills.get(0).skillId])

        badge.enabled  = 'true'
        skillsService.updateBadge(badge, badge.badgeId)

        when:
        def summaries = skillsService.getBadgesSummary(userId, proj1.projectId)
        then:
        summaries.size() == 1
        def summary = summaries.first()
        summary.badge == "Badge 1"
        summary.badgeId == "badge1"
        !summary.gem
        !summary.startDate
        !summary.endDate
        summary.numTotalSkills == 1
        summary.numSkillsAchieved == 0
        summary.iconClass == "fa fa-seleted-icon"
    }

    def "badges summary for a project - unachieved gem rolls off"() {
        String userId = "user1"

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        Date oneWeekAgo = new Date()-7
        Date twoWeeksAgo = new Date()-14
        Date tomorrow = new Date()+2
        String badge1 = "badge1"
        Map badge = [projectId: proj1.projectId, badgeId: badge1, name: 'Badge 1',
                     startDate: twoWeeksAgo, endDate: tomorrow,
                     description: 'This is a first badge', iconClass: "fa fa-seleted-icon",]
        skillsService.addBadge(badge)
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge1, skillId: proj1_skills.get(0).skillId])
        badge.enabled  = 'true'
        skillsService.updateBadge(badge, badge.badgeId)

        def summaries = skillsService.getBadgesSummary(userId, proj1.projectId)
        assert summaries.size() == 1
        def projSummary = skillsService.getSkillsSummaryForUser(proj1.projectId, userId)
        assert projSummary.badges.numTotalBadges == 1
        assert projSummary.badges.numBadgesCompleted == 0

        Map badgeUpdated = [projectId: proj1.projectId, badgeId: badge1, name: 'Badge 1',
                            startDate: twoWeeksAgo, endDate: oneWeekAgo,
                            description: 'This is a first badge', iconClass: "fa fa-seleted-icon",]
        skillsService.updateBadge(badgeUpdated, badge.badgeId)

        when:
        def summariesUpdated = skillsService.getBadgesSummary(userId, proj1.projectId)
        def projSummaryUpdated = skillsService.getSkillsSummaryForUser(proj1.projectId, userId)
        then:
        summariesUpdated.size() == 0
        projSummaryUpdated.badges.numTotalBadges == 0
        projSummaryUpdated.badges.numBadgesCompleted == 0
    }

    def "badges summary for a project - unachieved gem does not roll off too soon"() {
        String userId = "user1"

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        Date oneWeekAgo = new Date()-7
        Date twoWeeksAgo = new Date()-14
        Date tomorrow = new Date()+2
        Date yesterday = new Date()-1
        String badge1 = "badge1"
        Map badge = [projectId: proj1.projectId, badgeId: badge1, name: 'Badge 1',
                     startDate: twoWeeksAgo, endDate: tomorrow,
                     description: 'This is a first badge', iconClass: "fa fa-seleted-icon",]
        skillsService.addBadge(badge)
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge1, skillId: proj1_skills.get(0).skillId])
        badge.enabled  = 'true'
        skillsService.updateBadge(badge, badge.badgeId)

        def summaries = skillsService.getBadgesSummary(userId, proj1.projectId)
        def projSummary = skillsService.getSkillsSummaryForUser(proj1.projectId, userId)
        assert projSummary.badges.numTotalBadges == 1
        assert projSummary.badges.numBadgesCompleted == 0
        assert summaries.size() == 1

        Map badgeUpdated = [projectId: proj1.projectId, badgeId: badge1, name: 'Badge 1',
                            startDate: twoWeeksAgo, endDate: yesterday,
                            description: 'This is a first badge', iconClass: "fa fa-seleted-icon",]
        skillsService.updateBadge(badgeUpdated, badge.badgeId)

        summaries = skillsService.getBadgesSummary(userId, proj1.projectId)
        assert summaries.size() == 1
        projSummary = skillsService.getSkillsSummaryForUser(proj1.projectId, userId)
        assert projSummary.badges.numTotalBadges == 1
        assert projSummary.badges.numBadgesCompleted == 0

        badgeUpdated = [projectId: proj1.projectId, badgeId: badge1, name: 'Badge 1',
                        startDate: twoWeeksAgo, endDate: oneWeekAgo,
                        description: 'This is a first badge', iconClass: "fa fa-seleted-icon",]
        skillsService.updateBadge(badgeUpdated, badge.badgeId)

        when:
        def summariesUpdated = skillsService.getBadgesSummary(userId, proj1.projectId)
        def projSummaryUpdated = skillsService.getSkillsSummaryForUser(proj1.projectId, userId)

        then:
        summariesUpdated.size() == 0
        projSummaryUpdated.badges.numTotalBadges == 0
        projSummaryUpdated.badges.numBadgesCompleted == 0
    }

    def "badges summary for a project - achieved gem is retained"() {
        String userId = "user1"

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.each{
            it.pointIncrement = 40
        }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        Date oneWeekAgo = new Date()-7
        Date twoWeeksAgo = new Date()-14
        Date tomorrow = new Date() + 1
        Date twoDaysAgo = new Date() - 2
        String badge1 = "badge1"
        Map badge = [projectId: proj1.projectId, badgeId: badge1, name: 'Badge 1',
                     startDate: twoWeeksAgo, endDate: tomorrow,
                     description: 'This is a first badge', iconClass: "fa fa-seleted-icon",]
        skillsService.addBadge(badge)
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge1, skillId: proj1_skills.get(0).skillId])
        badge.enabled  = 'true'
        skillsService.updateBadge(badge, badge.badgeId)
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId, oneWeekAgo)

        Map badgeUpdated = [projectId: proj1.projectId, badgeId: badge1, name: 'Badge 1',
                        startDate: twoWeeksAgo, endDate: twoDaysAgo,
                        description: 'This is a first badge', iconClass: "fa fa-seleted-icon",]
        skillsService.updateBadge(badgeUpdated, badge.badgeId)

        when:
        def summaries = skillsService.getBadgesSummary(userId, proj1.projectId)
        def projSummary = skillsService.getSkillsSummaryForUser(proj1.projectId, userId)
        then:
        summaries.size() == 1
        projSummary.badges.numTotalBadges == 1
        projSummary.badges.numBadgesCompleted == 1
    }

    def "badge summary is correct for a project with multiple badges and gems"() {
        String userId = "user1"

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(5, 1, 1)
        proj1_skills.each{
            it.pointIncrement = 40
        }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        Date oneWeekAgo = new Date()-7
        Date twoWeeksAgo = new Date()-14
        Date tomorrow = new Date()+2
        Date yesterday = new Date() - 1
        Date threeDaysAgo = new Date() - 3

        Map unachievedBadge = [projectId: proj1.projectId, badgeId: "badge1", name: 'Badge 1', description: 'This is a first badge', iconClass: "fa fa-seleted-icon",]
        Map achievedBadge = [projectId: proj1.projectId, badgeId: "badge2", name: 'Badge 2', description: 'This is a badge', iconClass: "fa fa-seleted-icon",]
        Map unachievedGemNotRolledOff = [projectId: proj1.projectId, badgeId: "badge3", name: 'Badge 3', startDate: twoWeeksAgo, endDate: tomorrow, description: 'This is a first badge', iconClass: "fa fa-seleted-icon",]
        Map unachievedGemRolledOff = [projectId: proj1.projectId, badgeId: "badge4", name: 'Badge 4', startDate: twoWeeksAgo, endDate: tomorrow, description: 'This is a first badge', iconClass: "fa fa-seleted-icon",]
        Map achievedGem = [projectId: proj1.projectId, badgeId: "badge5", name: 'Badge 5', startDate: twoWeeksAgo, endDate: tomorrow, description: 'This is a first badge', iconClass: "fa fa-seleted-icon",]
        skillsService.addBadge(unachievedBadge)
        skillsService.addBadge(achievedBadge)
        skillsService.addBadge(unachievedGemNotRolledOff)
        skillsService.addBadge(unachievedGemRolledOff)
        skillsService.addBadge(achievedGem)
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: "badge1", skillId: proj1_skills.get(0).skillId])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: "badge2", skillId: proj1_skills.get(1).skillId])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: "badge3", skillId: proj1_skills.get(2).skillId])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: "badge4", skillId: proj1_skills.get(3).skillId])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: "badge5", skillId: proj1_skills.get(4).skillId])
        unachievedBadge.enabled  = 'true'
        achievedBadge.enabled  = 'true'
        unachievedGemNotRolledOff.enabled  = 'true'
        unachievedGemRolledOff.enabled  = 'true'
        achievedGem.enabled  = 'true'
        skillsService.updateBadge(unachievedBadge, unachievedBadge.badgeId)
        skillsService.updateBadge(achievedBadge, achievedBadge.badgeId)
        skillsService.updateBadge(unachievedGemNotRolledOff, unachievedGemNotRolledOff.badgeId)
        skillsService.updateBadge(unachievedGemRolledOff, unachievedGemRolledOff.badgeId)
        skillsService.updateBadge(achievedGem, achievedGem.badgeId)

        def summaries = skillsService.getBadgesSummary(userId, proj1.projectId)
        assert summaries.size() == 5
        def projSummary = skillsService.getSkillsSummaryForUser(proj1.projectId, userId)
        assert projSummary.badges.numTotalBadges == 5
        assert projSummary.badges.numBadgesCompleted == 0

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId, oneWeekAgo)
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(4).skillId], userId, oneWeekAgo)

        unachievedGemNotRolledOff.endDate = yesterday
        unachievedGemRolledOff.endDate  = threeDaysAgo
        achievedGem.endDate  = threeDaysAgo
        skillsService.updateBadge(unachievedGemNotRolledOff, unachievedGemNotRolledOff.badgeId)
        skillsService.updateBadge(unachievedGemRolledOff, unachievedGemRolledOff.badgeId)
        skillsService.updateBadge(achievedGem, achievedGem.badgeId)

        when:
        def summariesUpdated = skillsService.getBadgesSummary(userId, proj1.projectId)
        def projSummaryUpdated = skillsService.getSkillsSummaryForUser(proj1.projectId, userId)
        then:
        summariesUpdated.size() == 4
        projSummaryUpdated.badges.numTotalBadges == 4
        projSummaryUpdated.badges.numBadgesCompleted == 2
    }

    def "badge summary is correct for a project with multiple badges and gems and multiple users"() {
        String userId = "user1"
        String userId2 = "user2"
        String userId3 = "user3"

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(5, 1, 1)
        proj1_skills.each{
            it.pointIncrement = 40
        }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        Date oneWeekAgo = new Date()-7
        Date twoWeeksAgo = new Date()-14
        Date tomorrow = new Date()+2
        Date yesterday = new Date() - 1
        Date threeDaysAgo = new Date() - 3

        Map unachievedBadge = [projectId: proj1.projectId, badgeId: "badge1", name: 'Badge 1', description: 'This is a first badge', iconClass: "fa fa-seleted-icon",]
        Map achievedBadge = [projectId: proj1.projectId, badgeId: "badge2", name: 'Badge 2', description: 'This is a badge', iconClass: "fa fa-seleted-icon",]
        Map unachievedGemNotRolledOff = [projectId: proj1.projectId, badgeId: "badge3", name: 'Badge 3', startDate: twoWeeksAgo, endDate: tomorrow, description: 'This is a first badge', iconClass: "fa fa-seleted-icon",]
        Map unachievedGemRolledOff = [projectId: proj1.projectId, badgeId: "badge4", name: 'Badge 4', startDate: twoWeeksAgo, endDate: tomorrow, description: 'This is a first badge', iconClass: "fa fa-seleted-icon",]
        Map achievedGem = [projectId: proj1.projectId, badgeId: "badge5", name: 'Badge 5', startDate: twoWeeksAgo, endDate: tomorrow, description: 'This is a first badge', iconClass: "fa fa-seleted-icon",]
        skillsService.addBadge(unachievedBadge)
        skillsService.addBadge(achievedBadge)
        skillsService.addBadge(unachievedGemNotRolledOff)
        skillsService.addBadge(unachievedGemRolledOff)
        skillsService.addBadge(achievedGem)
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: "badge1", skillId: proj1_skills.get(0).skillId])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: "badge2", skillId: proj1_skills.get(1).skillId])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: "badge3", skillId: proj1_skills.get(2).skillId])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: "badge4", skillId: proj1_skills.get(3).skillId])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: "badge5", skillId: proj1_skills.get(4).skillId])
        unachievedBadge.enabled  = 'true'
        achievedBadge.enabled  = 'true'
        unachievedGemNotRolledOff.enabled  = 'true'
        unachievedGemRolledOff.enabled  = 'true'
        achievedGem.enabled  = 'true'
        skillsService.updateBadge(unachievedBadge, unachievedBadge.badgeId)
        skillsService.updateBadge(achievedBadge, achievedBadge.badgeId)
        skillsService.updateBadge(unachievedGemNotRolledOff, unachievedGemNotRolledOff.badgeId)
        skillsService.updateBadge(unachievedGemRolledOff, unachievedGemRolledOff.badgeId)
        skillsService.updateBadge(achievedGem, achievedGem.badgeId)

        def summaries = skillsService.getBadgesSummary(userId, proj1.projectId)
        assert summaries.size() == 5
        def projSummary = skillsService.getSkillsSummaryForUser(proj1.projectId, userId)
        assert projSummary.badges.numTotalBadges == 5
        assert projSummary.badges.numBadgesCompleted == 0

        def projSummary2 = skillsService.getSkillsSummaryForUser(proj1.projectId, userId2)
        assert projSummary2.badges.numTotalBadges == 5
        assert projSummary2.badges.numBadgesCompleted == 0

        def projSummary3 = skillsService.getSkillsSummaryForUser(proj1.projectId, userId3)
        assert projSummary3.badges.numTotalBadges == 5
        assert projSummary3.badges.numBadgesCompleted == 0

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId, oneWeekAgo)
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(4).skillId], userId, oneWeekAgo)

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId], userId2, oneWeekAgo)
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(2).skillId], userId2, oneWeekAgo)

        unachievedGemNotRolledOff.endDate = yesterday
        unachievedGemRolledOff.endDate  = threeDaysAgo
        achievedGem.endDate  = threeDaysAgo
        skillsService.updateBadge(unachievedGemNotRolledOff, unachievedGemNotRolledOff.badgeId)
        skillsService.updateBadge(unachievedGemRolledOff, unachievedGemRolledOff.badgeId)
        skillsService.updateBadge(achievedGem, achievedGem.badgeId)

        when:
        def summariesUpdated = skillsService.getBadgesSummary(userId, proj1.projectId)
        def projSummaryUpdated = skillsService.getSkillsSummaryForUser(proj1.projectId, userId)
        def projSummary2Updated = skillsService.getSkillsSummaryForUser(proj1.projectId, userId2)
        def projSummary3Updated = skillsService.getSkillsSummaryForUser(proj1.projectId, userId3)

        then:
        summariesUpdated.size() == 4
        projSummaryUpdated.badges.numTotalBadges == 4
        projSummaryUpdated.badges.numBadgesCompleted == 2
        projSummary2Updated.badges.numTotalBadges == 3
        projSummary2Updated.badges.numBadgesCompleted == 2
        projSummary3Updated.badges.numTotalBadges == 3
        projSummary3Updated.badges.numBadgesCompleted == 0
    }

    def "badges summary for a project - one badge - achieved"() {
        String userId = "user1"

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.each{
            it.pointIncrement = 40
        }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        String badge1 = "badge1"
        Map badge = [projectId: proj1.projectId, badgeId: badge1, name: 'Badge 1', description: 'This is a first badge', iconClass: "fa fa-seleted-icon", helpUrl: "http://foo.org"]
        skillsService.addBadge(badge)
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge1, skillId: proj1_skills.get(0).skillId])
        badge.enabled  = 'true'
        skillsService.updateBadge(badge, badge.badgeId)
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId, new Date())

        when:
        def summaries = skillsService.getBadgesSummary(userId, proj1.projectId)
        then:
        summaries.size() == 1
        def summary = summaries.first()
        summary.badge == "Badge 1"
        summary.badgeId == "badge1"
        !summary.gem
        !summary.startDate
        !summary.endDate
        summary.numTotalSkills == 1
        summary.numSkillsAchieved == 1
        summary.iconClass == "fa fa-seleted-icon"
        summary.helpUrl == "http://foo.org"
    }

    def "badges summary for a project - few badges"() {
        String userId = "user1"

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(5, 1, 1)
        proj1_skills.each{
            it.pointIncrement = 20
        }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        List<String> badgeIds = (1..3).collect({ "badge${it}".toString()})
        List badges = []
        badgeIds.each {
            Map badge = [projectId: proj1.projectId, badgeId: it, name: it, description: "This is ${it}".toString(), iconClass: "fa fa-${it}".toString(), enabled: 'true']
            badges << badge
            skillsService.addBadge(badge)
        }

        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badgeIds.get(0), skillId: proj1_skills.get(0).skillId])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badgeIds.get(0), skillId: proj1_skills.get(1).skillId])
        skillsService.updateBadge(badges[0], badgeIds.get(0))  // can only enable after initial creation
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId, new Date())

        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badgeIds.get(2), skillId: proj1_skills.get(0).skillId])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badgeIds.get(2), skillId: proj1_skills.get(1).skillId])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badgeIds.get(2), skillId: proj1_skills.get(2).skillId])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badgeIds.get(2), skillId: proj1_skills.get(3).skillId])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badgeIds.get(2), skillId: proj1_skills.get(4).skillId])
        skillsService.updateBadge(badges[2], badgeIds.get(2))

        when:
        def summaries = skillsService.getBadgesSummary(userId, proj1.projectId)
        then:
        summaries.size() == 2
        summaries.get(0).badge == "badge1"
        summaries.get(0).badgeId == "badge1"
        summaries.get(0).iconClass == "fa fa-badge1"
        summaries.get(0).numSkillsAchieved == 1
        summaries.get(0).numTotalSkills == 2

        summaries.get(1).badge == "badge3"
        summaries.get(1).badgeId == "badge3"
        summaries.get(1).iconClass == "fa fa-badge3"
        summaries.get(1).numSkillsAchieved == 1
        summaries.get(1).numTotalSkills == 5
    }

    def "single badge summary"() {
        String userId = "user1"

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        String badge1 = "badge1"
        Map badge = [projectId: proj1.projectId, badgeId: badge1, name: 'Badge 1', description: 'This is a first badge', iconClass: "fa fa-seleted-icon",]
        skillsService.addBadge(badge)
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge1, skillId: proj1_skills.get(0).skillId])
        badge.enabled  = 'true'
        skillsService.updateBadge(badge, badge.badgeId)

        when:
        def summary = skillsService.getBadgeSummary(userId, proj1.projectId, badge1)
        then:
        summary.badge == "Badge 1"
        summary.badgeId == "badge1"
        !summary.gem
        !summary.startDate
        !summary.endDate
        summary.numTotalSkills == 1
        summary.numSkillsAchieved == 0
        summary.iconClass == "fa fa-seleted-icon"
        summary.skills.size() == 1
        summary.skills.get(0).skillId == proj1_skills.get(0).skillId
        summary.skills.get(0).skill == proj1_skills.get(0).name
        summary.skills.get(0).pointIncrement == proj1_skills.get(0).pointIncrement
        summary.skills.get(0).pointIncrementInterval == proj1_skills.get(0).pointIncrementInterval
        summary.skills.get(0).totalPoints == proj1_skills.get(0).numPerformToCompletion * proj1_skills.get(0).pointIncrement
        summary.skills.get(0).points == 0
        summary.skills.get(0).todaysPoints == 0
        !summary.skills.get(0).description
        !summary.dependencyInfo
        summary.skills[0].subjectName == proj1_subj.name
        summary.skills[0].subjectId == proj1_subj.subjectId
    }

    def "single badge summary - gem"() {
        String userId = "user1"

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        Date oneWeekAgo = new Date()-7
        Date twoWeeksAgo = new Date()-14
        String badge1 = "badge1"
        Map badge = [projectId: proj1.projectId, badgeId: badge1, name: 'Badge 1',
                     startDate: twoWeeksAgo, endDate: oneWeekAgo,
                     description: 'This is a first badge', iconClass: "fa fa-seleted-icon",]
        skillsService.addBadge(badge)
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge1, skillId: proj1_skills.get(0).skillId])
        badge.enabled  = 'true'
        skillsService.updateBadge(badge, badge.badgeId)

        when:
        def summary = skillsService.getBadgeSummary(userId, proj1.projectId, badge1)
        then:
        summary.badge == "Badge 1"
        summary.badgeId == "badge1"
        summary.gem
        summary.startDate
        summary.endDate
        summary.numTotalSkills == 1
        summary.numSkillsAchieved == 0
        summary.iconClass == "fa fa-seleted-icon"
        summary.skills.size() == 1
        summary.skills.get(0).skillId == proj1_skills.get(0).skillId
        summary.skills.get(0).skill == proj1_skills.get(0).name
        summary.skills.get(0).pointIncrement == proj1_skills.get(0).pointIncrement
        summary.skills.get(0).pointIncrementInterval == proj1_skills.get(0).pointIncrementInterval
        summary.skills.get(0).totalPoints == proj1_skills.get(0).numPerformToCompletion * proj1_skills.get(0).pointIncrement
        summary.skills.get(0).points == 0
        summary.skills.get(0).todaysPoints == 0
        !summary.skills.get(0).description
        !summary.dependencyInfo
        summary.skills[0].subjectName == proj1_subj.name
        summary.skills[0].subjectId == proj1_subj.subjectId
    }

    def "single badge summary - achieved skill"() {
        String userId = "user1"

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.each{
            it.pointIncrement = 40
        }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        String badge1 = "badge1"
        Map badge = [projectId: proj1.projectId, badgeId: badge1, name: 'Badge 1', description: 'This is a first badge', iconClass: "fa fa-seleted-icon",]
        skillsService.addBadge(badge)
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge1, skillId: proj1_skills.get(0).skillId])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge1, skillId: proj1_skills.get(1).skillId])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge1, skillId: proj1_skills.get(2).skillId])
        badge.enabled  = 'true'
        skillsService.updateBadge(badge, badge.badgeId)

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId, new Date())

        when:
        def summary = skillsService.getBadgeSummary(userId, proj1.projectId, badge1)
        then:
        summary.badge == "Badge 1"
        summary.badgeId == "badge1"
        !summary.gem
        !summary.startDate
        !summary.endDate
        summary.numTotalSkills == 3
        summary.numSkillsAchieved == 1
        summary.iconClass == "fa fa-seleted-icon"
        summary.skills.size() == 3
        def skill1 = summary.skills.find { it.skillId == proj1_skills.get(0).skillId }
        skill1.totalPoints == 40
        skill1.todaysPoints == 40
        skill1.points == 40
        !summary.dependencyInfo

        def skill2 = summary.skills.find { it.skillId == proj1_skills.get(1).skillId }
        skill2.todaysPoints == 0
        skill2.points == 0

        def skill3 = summary.skills.find { it.skillId == proj1_skills.get(2).skillId }
        skill3.todaysPoints == 0
        skill3.points == 0

        summary.skills[0].subjectName == proj1_subj.name
        summary.skills[0].subjectId == proj1_subj.subjectId
    }


    def "single badge summary - with dependency info"() {
        String userId = "user1"

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(4, 1, 1)
        proj1_skills.each{
            it.pointIncrement = 25
        }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        String badge1 = "badge1"
        Map badge = [projectId: proj1.projectId, badgeId: badge1, name: 'Badge 1', description: 'This is a first badge', iconClass: "fa fa-seleted-icon",]
        skillsService.addBadge(badge)
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge1, skillId: proj1_skills.get(0).skillId])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge1, skillId: proj1_skills.get(1).skillId])
        badge.enabled  = 'true'
        skillsService.updateBadge(badge, badge.badgeId)

        skillsService.addLearningPathPrerequisite(proj1.projectId, proj1_skills.get(1).skillId, proj1_skills.get(2).skillId)
        skillsService.addLearningPathPrerequisite(proj1.projectId, proj1_skills.get(1).skillId, proj1_skills.get(3).skillId)

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId, new Date())

        when:
        def summary = skillsService.getBadgeSummary(userId, proj1.projectId, badge1)
        then:
        summary.badge == "Badge 1"
        summary.badgeId == "badge1"

        !summary.skills.find { it.skillId == proj1_skills.get(0).skillId }.dependencyInfo

        def skill1 = summary.skills.find { it.skillId == proj1_skills.get(1).skillId }
        skill1.dependencyInfo.numDirectDependents == 2
        !skill1.dependencyInfo.achieved
        summary.skills[0].subjectName == proj1_subj.name
        summary.skills[0].subjectId == proj1_subj.subjectId
    }

    def "single badge summary - with achieved dependency info"() {
        String userId = "user1"

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(4, 1, 1)
        proj1_skills.each{
            it.pointIncrement = 25
        }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        String badge1 = "badge1"
        Map badge = [projectId: proj1.projectId, badgeId: badge1, name: 'Badge 1', description: 'This is a first badge', iconClass: "fa fa-seleted-icon",]
        skillsService.addBadge(badge)
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge1, skillId: proj1_skills.get(0).skillId])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge1, skillId: proj1_skills.get(1).skillId])
        badge.enabled  = 'true'
        skillsService.updateBadge(badge, badge.badgeId)

        skillsService.addLearningPathPrerequisite(proj1.projectId, proj1_skills.get(1).skillId, proj1_skills.get(2).skillId)
        skillsService.addLearningPathPrerequisite(proj1.projectId, proj1_skills.get(1).skillId, proj1_skills.get(3).skillId)

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(2).skillId], userId, new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(3).skillId], userId, new Date())

        when:
        def summary = skillsService.getBadgeSummary(userId, proj1.projectId, badge1)
        then:
        summary.badge == "Badge 1"
        summary.badgeId == "badge1"

        !summary.skills.find { it.skillId == proj1_skills.get(0).skillId }.dependencyInfo

        def skill1 = summary.skills.find { it.skillId == proj1_skills.get(1).skillId }
        skill1.dependencyInfo.numDirectDependents == 2
        skill1.dependencyInfo.achieved
        summary.skills[0].subjectName == proj1_subj.name
        summary.skills[0].subjectId == proj1_subj.subjectId
    }

    def "project summary should return achieved badges summary"(){
        String userId = "user1"

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(4, 1, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        String badge1 = "badge1"
        Map badge = [projectId: proj1.projectId, badgeId: badge1, name: 'Badge 1', description: 'This is a first badge', iconClass: "fa fa-seleted-icon",]
        skillsService.addBadge(badge)
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge1, skillId: proj1_skills.get(0).skillId])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge1, skillId: proj1_skills.get(1).skillId])
        badge.enabled  = 'true'
        skillsService.updateBadge(badge, badge.badgeId)

        when:
        def summary = skillsService.getSkillSummary(userId, proj1.projectId)
        then:
        summary.badges.numBadgesCompleted == 0
        summary.badges.enabled
    }

    def "project summary should return achieved badges summary - badges completed"(){
        String userId = "user1"

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(4, 1, 1)
        proj1_skills.each{
            it.pointIncrement = 25
        }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        String badge1 = "badge1"
        Map badge = [projectId: proj1.projectId, badgeId: badge1, name: 'Badge 1', description: 'This is a first badge', iconClass: "fa fa-seleted-icon",]
        skillsService.addBadge(badge)
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge1, skillId: proj1_skills.get(0).skillId])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge1, skillId: proj1_skills.get(1).skillId])
        badge.enabled  = 'true'
        skillsService.updateBadge(badge, badge.badgeId)

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId, new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId], userId, new Date())

        when:
        def summary = skillsService.getSkillSummary(userId, proj1.projectId)
        then:
        summary.badges.numBadgesCompleted == 1
        summary.badges.enabled
    }

    def "project summary should disable badges if there are no badges"(){
        String userId = "user1"

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(4, 1, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        when:
        def summary = skillsService.getSkillSummary(userId, proj1.projectId)
        then:
        !summary.badges.enabled
    }

    def "badges summaries are returned sorted by displayOrder"() {
        String userId = "user1"

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(5, 1, 1)
        proj1_skills.each{
            it.pointIncrement = 20
        }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        List<String> badgeIds = (1..3).collect({ "badge${it}".toString()})
        List badges = []
        badgeIds.each {
            Map badge = [projectId: proj1.projectId, badgeId: it, name: it, description: "This is ${it}".toString(), iconClass: "fa fa-${it}".toString(), enabled: 'true']
            badges << badge
            skillsService.addBadge(badge)
        }

        skillsService.changeBadgeDisplayOrder([projectId: proj1.projectId, badgeId: badgeIds[0]], 1)
        skillsService.changeBadgeDisplayOrder([projectId: proj1.projectId, badgeId: badgeIds[0]], 2)
        skillsService.changeBadgeDisplayOrder([projectId: proj1.projectId, badgeId: badgeIds[2]], 0)

        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badgeIds.get(0), skillId: proj1_skills.get(0).skillId])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badgeIds.get(0), skillId: proj1_skills.get(1).skillId])
        skillsService.updateBadge(badges[0], badgeIds.get(0))  // can only enable after initial badge creation
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId, new Date())

        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badgeIds.get(2), skillId: proj1_skills.get(0).skillId])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badgeIds.get(2), skillId: proj1_skills.get(1).skillId])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badgeIds.get(2), skillId: proj1_skills.get(2).skillId])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badgeIds.get(2), skillId: proj1_skills.get(3).skillId])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badgeIds.get(2), skillId: proj1_skills.get(4).skillId])
        skillsService.updateBadge(badges[2], badgeIds.get(2))

        when:
        def summaries = skillsService.getBadgesSummary(userId, proj1.projectId)
        then:
        summaries.size() == 2
        summaries.get(1).badge == "badge1"
        summaries.get(1).badgeId == "badge1"
        summaries.get(1).iconClass == "fa fa-badge1"
        summaries.get(1).numSkillsAchieved == 1
        summaries.get(1).numTotalSkills == 2

        summaries.get(0).badge == "badge3"
        summaries.get(0).badgeId == "badge3"
        summaries.get(0).iconClass == "fa fa-badge3"
        summaries.get(0).numSkillsAchieved == 1
        summaries.get(0).numTotalSkills == 5
    }

    def "user badge achievement should not leak into another project"() {
        String userId = "user1"
        String userId2 = "user2"
        String badge1 = "badge1"

        // proj1
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(2, 1, 1)
        proj1_skills.get(0).pointIncrement=100

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        Map badge = [projectId: proj1.projectId, badgeId: badge1, name: 'Badge 1', description: 'This is a first badge', iconClass: "fa fa-seleted-icon",]
        skillsService.addBadge(badge)
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge1, skillId: proj1_skills.get(0).skillId])
        badge.enabled  = 'true'
        skillsService.updateBadge(badge, badge.badgeId)

        // proj2
        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 1)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 1)
        proj2_skills.get(0).pointIncrement = 100

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)
        skillsService.createSkills(proj2_skills)

        Map badge2 = [projectId: proj2.projectId, badgeId: badge1, name: 'Badge 1', description: 'This is a first badge', iconClass: "fa fa-seleted-icon",]
        skillsService.addBadge(badge2)
        skillsService.assignSkillToBadge([projectId: proj2.projectId, badgeId: badge1, skillId: proj2_skills.get(0).skillId])
        badge2.enabled  = 'true'
        skillsService.updateBadge(badge2, badge2.badgeId)
        //proj2 badge1, proj2 skills[0]

        // global badge
        Map globalBadge = [badgeId: "globalBadge", name: 'Badge 1', description: 'This is a first badge', iconClass: "fa fa-seleted-icon"]
        globalBadge.helpUrl = "http://foo.org"
        skillsService.createGlobalBadge(globalBadge)
        skillsService.assignSkillToGlobalBadge(projectId: proj1.projectId, badgeId: globalBadge.badgeId, skillId: proj1_skills.get(0).skillId)
        globalBadge.enabled = 'true'
        skillsService.updateGlobalBadge(globalBadge)
        //global badge has dep on proj1 skills[0]

        // add skill
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId, new Date())
        skillsService.addSkill([projectId: proj2.projectId, skillId: proj2_skills.get(1).skillId], userId, new Date())

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId], userId2, new Date())
        skillsService.addSkill([projectId: proj2.projectId, skillId: proj2_skills.get(0).skillId], userId2, new Date())

        when:
        def summary = skillsService.getSkillSummary(userId, proj1.projectId)
        def summaries = skillsService.getBadgesSummary(userId, proj1.projectId)

        def summary2 = skillsService.getSkillSummary(userId, proj2.projectId)
        def summaries2 = skillsService.getBadgesSummary(userId, proj2.projectId)

        def summaryUser2 = skillsService.getSkillSummary(userId2, proj1.projectId)
        def summariesUser2 = skillsService.getBadgesSummary(userId2, proj1.projectId)

        def summary2User2 = skillsService.getSkillSummary(userId2, proj2.projectId)
        def summaries2User2 = skillsService.getBadgesSummary(userId2, proj2.projectId)

        then:
        // user 1
        summary.badges.numBadgesCompleted == 2
        summaries.size() == 2
        summaries.find { it.badgeId == "globalBadge" }.numSkillsAchieved == 1
        summaries.find { it.badgeId == "badge1" }.numSkillsAchieved == 1

        //global badge with no proj2 skills should not be included in proj2 summary completed badge count
        summary2.badges.numBadgesCompleted == 0
        summaries2.size() == 1
        summaries2.find { it.badgeId == "badge1" }.numSkillsAchieved == 0

        // user 2
        summaryUser2.badges.numBadgesCompleted == 0
        summariesUser2.size() == 2
        summariesUser2.find { it.badgeId == "globalBadge" }.numSkillsAchieved == 0
        summariesUser2.find { it.badgeId == "badge1" }.numSkillsAchieved == 0

        summary2User2.badges.numBadgesCompleted == 1
        summaries2User2.size() == 1
        summaries2User2.find { it.badgeId == "badge1" }.numSkillsAchieved == 1
    }

    def "sort skills alphabetically"() {
        String userId = "user1"

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(5, 1, 1)
        proj1_skills[0].name = "Dsome"
        proj1_skills[1].name = "Zsome"
        proj1_skills[2].name = "ksome"
        proj1_skills[3].name = "asome"
        proj1_skills[4].name = "lsome"

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        String badge1 = "badge1"
        Map badge = [projectId: proj1.projectId, badgeId: badge1, name: 'Badge 1', description: 'This is a first badge', iconClass: "fa fa-seleted-icon",]
        skillsService.addBadge(badge)

        proj1_skills.each {
            skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge1, skillId: it.skillId])
            badge.enabled  = 'true'
            skillsService.updateBadge(badge, badge.badgeId)
        }

        when:
        def summary = skillsService.getBadgeSummary(userId, proj1.projectId, badge1)
        then:
        summary.skills.collect { it.skill } == ["asome", "Dsome", "ksome", "lsome", "Zsome"]
    }

    def "return extra fields for the catalog imported skills"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills[0].pointIncrement = 100

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)
        skillsService.exportSkillToCatalog(proj1.projectId, proj1_skills[0].skillId)
        skillsService.exportSkillToCatalog(proj1.projectId, proj1_skills[1].skillId)
        skillsService.exportSkillToCatalog(proj1.projectId, proj1_skills[2].skillId)

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 2)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 2)

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)
        skillsService.importSkillFromCatalogAndFinalize(proj2.projectId, proj2_subj.subjectId, proj1.projectId, proj1_skills[0].skillId)
        skillsService.createSkills(proj2_skills)
        skillsService.importSkillFromCatalogAndFinalize(proj2.projectId, proj2_subj.subjectId, proj1.projectId, proj1_skills[1].skillId)

        def proj3 = SkillsFactory.createProject(3)
        def proj3_subj = SkillsFactory.createSubject(3, 3)
        List<Map> proj3_skills = SkillsFactory.createSkills(2, 3, 3, 100)

        skillsService.createProject(proj3)
        skillsService.createSubject(proj3_subj)
        skillsService.createSkills(proj3_skills)
        skillsService.exportSkillToCatalog(proj3.projectId, proj3_skills[0].skillId)
        skillsService.exportSkillToCatalog(proj3.projectId, proj3_skills[1].skillId)

        // import from project 3
        skillsService.importSkillFromCatalogAndFinalize(proj2.projectId, proj2_subj.subjectId, proj3.projectId, proj3_skills[0].skillId)

        // import from project 2 again
        skillsService.importSkillFromCatalogAndFinalize(proj2.projectId, proj2_subj.subjectId, proj1.projectId, proj1_skills[2].skillId)

        // import from project 3
        skillsService.importSkillFromCatalogAndFinalize(proj2.projectId, proj2_subj.subjectId, proj3.projectId, proj3_skills[1].skillId)

        String badge1 = "badge1"
        Map badge = [projectId: proj2.projectId, badgeId: badge1, name: 'Badge 1', description: 'This is a first badge', iconClass: "fa fa-seleted-icon",]
        skillsService.addBadge(badge)
        skillsService.assignSkillToBadge([projectId: proj2.projectId, badgeId: badge1, skillId: proj1_skills[0].skillId])
        skillsService.assignSkillToBadge([projectId: proj2.projectId, badgeId: badge1, skillId: proj2_skills[0].skillId])
        skillsService.assignSkillToBadge([projectId: proj2.projectId, badgeId: badge1, skillId: proj2_skills[1].skillId])
        skillsService.assignSkillToBadge([projectId: proj2.projectId, badgeId: badge1, skillId: proj1_skills[1].skillId])
        skillsService.assignSkillToBadge([projectId: proj2.projectId, badgeId: badge1, skillId: proj3_skills[1].skillId])
        badge.enabled = "true"
        skillsService.addBadge(badge)

        when:
        def summary = skillsService.getBadgeSummary("user1", proj2.projectId, badge1)

        then:
        summary
        // order is alphabetical
        summary.skills.collect { it.skillId } == ['skill1', 'skill1subj2', 'skill2', 'skill2subj2', 'skill2subj3']
        summary.skills.collect { it.copiedFromProjectId } == ["TestProject1", null, "TestProject1", null, "TestProject3"]
        summary.skills.collect { it.copiedFromProjectName } == ["Test Project#1", null, "Test Project#1", null, "Test Project#3"]
    }

    def "load badge with approvals"(){
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> allSkills = SkillsFactory.createSkills(2, 1, 1)
        allSkills[0].pointIncrement = 200
        allSkills[0].numPerformToCompletion = 200
        allSkills[0].selfReportingType = SkillDef.SelfReportingType.Approval

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(allSkills)

        String badge1 = "badge1"
        Map badge = [projectId: proj1.projectId, badgeId: badge1, name: 'Badge 1', description: 'This is a first badge', iconClass: "fa fa-seleted-icon",]
        skillsService.addBadge(badge)

        allSkills.each {
            skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge1, skillId: it.skillId])
            badge.enabled  = 'true'
            skillsService.updateBadge(badge, badge.badgeId)
        }

        List<String> users = getRandomUsers(1)
        def requestedDate = new Date()
        skillsService.addSkill([projectId: proj1.projectId, skillId: allSkills[0].skillId], users.first(), requestedDate, "Please approve this 1!")

        when:
        def summary = skillsService.getBadgeSummary(users[0], proj1.projectId, badge1)
        then:
        summary.skills.size() == 2
        summary.skills[0].selfReporting.requestedOn == requestedDate.time
        !summary.skills[1].selfReporting.enabled
    }

    def "load badge with awards"(){
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> allSkills = SkillsFactory.createSkills(2, 1, 1)
        allSkills[0].pointIncrement = 200
        allSkills[0].numPerformToCompletion = 200
        allSkills[0].selfReportingType = SkillDef.SelfReportingType.Approval

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(allSkills)

        String badge1 = "badge1"
        Map badge = [projectId: proj1.projectId, badgeId: badge1, name: 'Badge 1', description: 'This is a first badge', iconClass: "fa fa-seleted-icon",awardAttrs: [name: 'Test Award', iconClass: 'abc', numMinutes: 120]]
        skillsService.addBadge(badge)

        allSkills.each {
            skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge1, skillId: it.skillId])
            badge.enabled  = 'true'
            skillsService.updateBadge(badge, badge.badgeId)
        }

        List<String> users = getRandomUsers(1)

        when:
        def summary = skillsService.getBadgeSummary(users[0], proj1.projectId, badge1)
        then:
        summary.awardAttrs
        summary.awardAttrs.name == "Test Award"
        summary.awardAttrs.iconClass == "abc"
        summary.awardAttrs.numMinutes == 120
        summary.numberOfUsersAchieved == 0
        summary.hasExpired == false
        summary.firstPerformedSkill == null
        summary.expirationDate == 0
        summary.achievementPosition == -1
        summary.achievedWithinExpiration == false
    }

    def "load badge with award and user activity"(){
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> allSkills = SkillsFactory.createSkills(2, 1, 1)
        allSkills[0].pointIncrement = 200
        allSkills[0].numPerformToCompletion = 1

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(allSkills)

        String badge1 = "badge1"
        Map badge = [projectId: proj1.projectId, badgeId: badge1, name: 'Badge 1', description: 'This is a first badge', iconClass: "fa fa-seleted-icon",awardAttrs: [name: 'Test Award', iconClass: 'abc', numMinutes: 120]]
        skillsService.addBadge(badge)

        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge1, skillId: allSkills[0].skillId])
        badge.enabled  = 'true'
        skillsService.updateBadge(badge, badge.badgeId)

        List<String> users = getRandomUsers(3)
        def currentDate = new Date()
        def expirationDate = currentDate.clone()
        expirationDate.minutes += 120
        skillsService.addSkill([projectId: proj1.projectId, skillId: allSkills.get(0).skillId], users[0], currentDate)
        Thread.sleep(1000)
        skillsService.addSkill([projectId: proj1.projectId, skillId: allSkills.get(0).skillId], users[1], currentDate)
        Thread.sleep(1000)
        skillsService.addSkill([projectId: proj1.projectId, skillId: allSkills.get(0).skillId], users[2], currentDate)

        when:
        def summary = skillsService.getBadgeSummary(users[0], proj1.projectId, badge1)
        def userTwoSummary = skillsService.getBadgeSummary(users[1], proj1.projectId, badge1)
        def userThreeSummary = skillsService.getBadgeSummary(users[2], proj1.projectId, badge1)
        then:
        summary.awardAttrs
        summary.awardAttrs.name == "Test Award"
        summary.awardAttrs.iconClass == "abc"
        summary.awardAttrs.numMinutes == 120
        summary.numberOfUsersAchieved == 3
        summary.hasExpired == false
        summary.achievementPosition == 1
        summary.achievedWithinExpiration == true
        userTwoSummary.achievementPosition == 2
        userThreeSummary.achievementPosition == 3

    }

    def "load badge with expired award"(){
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> allSkills = SkillsFactory.createSkills(2, 1, 1)
        allSkills[0].pointIncrement = 200
        allSkills[0].numPerformToCompletion = 2

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(allSkills)

        String badge1 = "badge1"
        Map badge = [projectId: proj1.projectId, badgeId: badge1, name: 'Badge 1', description: 'This is a first badge', iconClass: "fa fa-seleted-icon",awardAttrs: [name: 'Test Award', iconClass: 'abc', numMinutes: 120]]
        skillsService.addBadge(badge)

        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge1, skillId: allSkills[0].skillId])
        badge.enabled  = 'true'
        skillsService.updateBadge(badge, badge.badgeId)

        List<String> users = getRandomUsers(1)
        def currentDate = new Date() - 1
        def expirationDate = currentDate.clone()
        expirationDate.minutes += 120
        skillsService.addSkill([projectId: proj1.projectId, skillId: allSkills.get(0).skillId], users[0], currentDate)
        skillsService.addSkill([projectId: proj1.projectId, skillId: allSkills.get(0).skillId], users[0], new Date())

        when:
        def summary = skillsService.getBadgeSummary(users[0], proj1.projectId, badge1)
        then:
        summary.awardAttrs
        summary.awardAttrs.name == "Test Award"
        summary.awardAttrs.iconClass == "abc"
        summary.awardAttrs.numMinutes == 120
        summary.numberOfUsersAchieved == 1
        summary.hasExpired == true
        summary.achievementPosition == 1
        summary.achievedWithinExpiration == false
    }

    def "no recently achieved badges because no badges are achieved"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        String badge1 = "badge1"
        Map badge = [projectId: proj1.projectId, badgeId: badge1, name: 'Badge 1', description: 'This is a first badge', iconClass: "fa fa-seleted-icon",]
        skillsService.addBadge(badge)
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge1, skillId: proj1_skills.get(0).skillId])

        badge.enabled  = 'true'
        skillsService.updateBadge(badge, badge.badgeId)

        List<String> users = getRandomUsers(1)
        when:
        def user1Summary = skillsService.getSkillSummary(users[0], proj1.projectId)
        then:
        user1Summary.badges.numTotalBadges == 1
        user1Summary.badges.numBadgesCompleted == 0
        !user1Summary.badges.recentlyAwardedBadges
    }

    def "recently achieved badges are within 7 days"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1, 100)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        String badge1 = "badge1"
        Map badge = [projectId: proj1.projectId, badgeId: badge1, name: 'Badge 1', description: 'This is a first badge', iconClass: "fa fa-seleted-icon",]
        skillsService.addBadge(badge)
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge1, skillId: proj1_skills.get(0).skillId])

        badge.enabled  = 'true'
        skillsService.updateBadge(badge, badge.badgeId)

        def users = getRandomUsers(3)
        Date sevenDaysAgoPlus1Hour = null
        Date sevenDaysAgoMinus1Hour = null
        use(TimeCategory) {
            sevenDaysAgoPlus1Hour = new Date() - 7 + 1.hour
            sevenDaysAgoMinus1Hour = new Date() - 7 - 1.hour
        }

        skillsService.addSkill(proj1_skills[0], users[0], sevenDaysAgoPlus1Hour)
        skillsService.addSkill(proj1_skills[0], users[1], sevenDaysAgoMinus1Hour)

        when:
        def user1Summary = skillsService.getSkillSummary(users[0], proj1.projectId)
        def user2Summary = skillsService.getSkillSummary(users[1], proj1.projectId)
        then:
        user1Summary.badges.numTotalBadges == 1
        user1Summary.badges.numBadgesCompleted == 1
        user1Summary.badges.recentlyAwardedBadges.badgeId == [badge.badgeId]
        def achievedBadge = user1Summary.badges.recentlyAwardedBadges[0]
        achievedBadge.badgeName == badge.name
        reformatStrDate(achievedBadge.achievedOn) == formatDate(sevenDaysAgoPlus1Hour)
        !achievedBadge.isGlobalBadge

        user2Summary.badges.numTotalBadges == 1
        user2Summary.badges.numBadgesCompleted == 1
        !user2Summary.badges.recentlyAwardedBadges
    }

    def "multiple recently achieved badges are within 7 days"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1, 100)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        def createBadge = { int badgeNum, int skillNum ->
            def theBadge = SkillsFactory.createBadge(1, badgeNum)
            skillsService.addBadge(theBadge)
            skillsService.assignSkillToBadge(proj1.projectId, theBadge.badgeId, proj1_skills[skillNum].skillId.toString())
            theBadge.enabled  = 'true'
            skillsService.updateBadge(theBadge, theBadge.badgeId)

            return theBadge
        }
        def badge1 = createBadge(1, 0)
        def badge2 = createBadge(2, 1)
        def badge3 = createBadge(3, 2)


        def users = getRandomUsers(3)
        Date sevenDaysAgoPlus1Hour = null
        Date sevenDaysAgoMinus1Hour = null
        Date sixDaysAgo = null
        use(TimeCategory) {
            sevenDaysAgoPlus1Hour = new Date() - 7 + 1.hour
            sevenDaysAgoMinus1Hour = new Date() - 7 - 1.hour
            sixDaysAgo = new Date() - 6
        }

        skillsService.addSkill(proj1_skills[0], users[0], sevenDaysAgoPlus1Hour)
        skillsService.addSkill(proj1_skills[1], users[0], sevenDaysAgoMinus1Hour)
        skillsService.addSkill(proj1_skills[2], users[0], sixDaysAgo)

        when:
        def user1Summary = skillsService.getSkillSummary(users[0], proj1.projectId)
        then:
        user1Summary.badges.numTotalBadges == 3
        user1Summary.badges.numBadgesCompleted == 3
        user1Summary.badges.recentlyAwardedBadges.badgeId.sort() == [badge1.badgeId, badge3.badgeId]
        def earnedBadge1 = user1Summary.badges.recentlyAwardedBadges.find { it.badgeId == badge1.badgeId }
        earnedBadge1.badgeName == badge1.name
        reformatStrDate(earnedBadge1.achievedOn) == formatDate(sevenDaysAgoPlus1Hour)
        !earnedBadge1.isGlobalBadge

        def earnedBadge3 = user1Summary.badges.recentlyAwardedBadges.find { it.badgeId == badge3.badgeId }
        earnedBadge3.badgeName == badge3.name
        reformatStrDate(earnedBadge3.achievedOn) == formatDate(sixDaysAgo)
        !earnedBadge3.isGlobalBadge
    }

    def "multiple recently achieved global badges are within 7 days"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1, 100)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        def createBadge = { int badgeNum, int skillNum ->
            def theBadge = SkillsFactory.createBadge(1, badgeNum)
            skillsService.createGlobalBadge(theBadge)
            skillsService.assignSkillToGlobalBadge([projectId: proj1.projectId, badgeId: theBadge.badgeId, skillId: proj1_skills.get(skillNum).skillId])
            theBadge.enabled  = 'true'
            skillsService.updateGlobalBadge(theBadge, theBadge.badgeId)

            return theBadge
        }
        def badge1 = createBadge(1, 0)
        def badge2 = createBadge(2, 1)
        def badge3 = createBadge(3, 2)


        def users = getRandomUsers(3)
        Date sevenDaysAgoMinus1Hour = null
        use(TimeCategory) {
            sevenDaysAgoMinus1Hour = new Date() - 7 - 1.hour
        }

        skillsService.addSkill(proj1_skills[0], users[0])
        skillsService.addSkill(proj1_skills[1], users[0])
        skillsService.addSkill(proj1_skills[2], users[0])

        // global badges are achieved on the current date no matter when the last event is
        // so have to manually update DB to simulate an older achievement
        List<UserAchievement> badge2Achievement = userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(users[0], null, badge2.badgeId)
        assert badge2Achievement.size() == 1
        badge2Achievement.get(0).achievedOn = sevenDaysAgoMinus1Hour
        userAchievedRepo.saveAll(badge2Achievement)
        when:
        def user1Summary = skillsService.getSkillSummary(users[0], proj1.projectId)
        then:
        user1Summary.badges.numTotalBadges == 3
        user1Summary.badges.numBadgesCompleted == 3
        user1Summary.badges.recentlyAwardedBadges.badgeId.sort() == [badge1.badgeId, badge3.badgeId]
        def earnedBadge1 = user1Summary.badges.recentlyAwardedBadges.find { it.badgeId == badge1.badgeId }
        earnedBadge1.achievedOn
        earnedBadge1.badgeName == badge1.name
        earnedBadge1.isGlobalBadge

        def earnedBadge3 = user1Summary.badges.recentlyAwardedBadges.find { it.badgeId == badge3.badgeId }
        earnedBadge3.badgeName == badge3.name
        earnedBadge3.achievedOn
        earnedBadge3.isGlobalBadge
    }

    String datePattern = "yyyy-MM-dd'T'HH:mm:ss"
    private String reformatStrDate(String str) {
        return formatDate(Date.parse(datePattern, str))
    }
    private String formatDate(Date date) {
        return date.format(datePattern)
    }
}
