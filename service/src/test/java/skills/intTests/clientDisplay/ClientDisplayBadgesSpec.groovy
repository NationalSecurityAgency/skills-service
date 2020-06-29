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


import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService

class ClientDisplayBadgesSpec extends DefaultIntSpec {

    String ultimateRoot = 'jh@dojo.com'
    SkillsService rootSkillsService
    String supervisorUserId = 'foo@bar.com'
    SkillsService supervisorSkillsService

    def setup(){
        rootSkillsService = createService(ultimateRoot, 'aaaaaaaa')
        supervisorSkillsService = createService(supervisorUserId)

        if (!rootSkillsService.isRoot()) {
            rootSkillsService.grantRoot()
        }
        rootSkillsService.grantSupervisorRole(supervisorUserId)
    }

    def "badges summary for a project - one badge"() {
        String userId = "user1"

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        String badge1 = "badge1"
        skillsService.addBadge([projectId: proj1.projectId, badgeId: badge1, name: 'Badge 1', description: 'This is a first badge', iconClass: "fa fa-seleted-icon",])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge1, skillId: proj1_skills.get(0).skillId])

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

    def "badges summary for a project - one gem"() {
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
        skillsService.addBadge([projectId: proj1.projectId, badgeId: badge1, name: 'Badge 1',
                                startDate: twoWeeksAgo, endDate: oneWeekAgo,
                                description: 'This is a first badge', iconClass: "fa fa-seleted-icon",])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge1, skillId: proj1_skills.get(0).skillId])

        when:
        def summaries = skillsService.getBadgesSummary(userId, proj1.projectId)
        then:
        summaries.size() == 1
        def summary = summaries.first()
        summary.badge == "Badge 1"
        summary.badgeId == "badge1"
        summary.gem
        summary.startDate
        summary.endDate
        summary.numTotalSkills == 1
        summary.numSkillsAchieved == 0
        summary.iconClass == "fa fa-seleted-icon"
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
        skillsService.addBadge([projectId: proj1.projectId, badgeId: badge1, name: 'Badge 1', description: 'This is a first badge', iconClass: "fa fa-seleted-icon", helpUrl: "http://foo.org"])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge1, skillId: proj1_skills.get(0).skillId])
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
        badgeIds.each {
            skillsService.addBadge([projectId: proj1.projectId, badgeId: it, name: it, description: "This is ${it}".toString(), iconClass: "fa fa-${it}".toString()])
        }

        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badgeIds.get(0), skillId: proj1_skills.get(0).skillId])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badgeIds.get(0), skillId: proj1_skills.get(1).skillId])
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId, new Date())

        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badgeIds.get(2), skillId: proj1_skills.get(0).skillId])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badgeIds.get(2), skillId: proj1_skills.get(1).skillId])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badgeIds.get(2), skillId: proj1_skills.get(2).skillId])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badgeIds.get(2), skillId: proj1_skills.get(3).skillId])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badgeIds.get(2), skillId: proj1_skills.get(4).skillId])

        when:
        def summaries = skillsService.getBadgesSummary(userId, proj1.projectId)
        then:
        summaries.size() == 3
        summaries.get(0).badge == "badge1"
        summaries.get(0).badgeId == "badge1"
        summaries.get(0).iconClass == "fa fa-badge1"
        summaries.get(0).numSkillsAchieved == 1
        summaries.get(0).numTotalSkills == 2

        summaries.get(1).badge == "badge2"
        summaries.get(1).badgeId == "badge2"
        summaries.get(1).iconClass == "fa fa-badge2"
        summaries.get(1).numSkillsAchieved == 0
        summaries.get(1).numTotalSkills == 0

        summaries.get(2).badge == "badge3"
        summaries.get(2).badgeId == "badge3"
        summaries.get(2).iconClass == "fa fa-badge3"
        summaries.get(2).numSkillsAchieved == 1
        summaries.get(2).numTotalSkills == 5
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
        skillsService.addBadge([projectId: proj1.projectId, badgeId: badge1, name: 'Badge 1', description: 'This is a first badge', iconClass: "fa fa-seleted-icon",])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge1, skillId: proj1_skills.get(0).skillId])

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
        skillsService.addBadge([projectId: proj1.projectId, badgeId: badge1, name: 'Badge 1',
                                startDate: twoWeeksAgo, endDate: oneWeekAgo,
                                description: 'This is a first badge', iconClass: "fa fa-seleted-icon",])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge1, skillId: proj1_skills.get(0).skillId])

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
        skillsService.addBadge([projectId: proj1.projectId, badgeId: badge1, name: 'Badge 1', description: 'This is a first badge', iconClass: "fa fa-seleted-icon",])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge1, skillId: proj1_skills.get(0).skillId])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge1, skillId: proj1_skills.get(1).skillId])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge1, skillId: proj1_skills.get(2).skillId])

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
        skillsService.addBadge([projectId: proj1.projectId, badgeId: badge1, name: 'Badge 1', description: 'This is a first badge', iconClass: "fa fa-seleted-icon",])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge1, skillId: proj1_skills.get(0).skillId])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge1, skillId: proj1_skills.get(1).skillId])

        skillsService.assignDependency([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId, dependentSkillId: proj1_skills.get(2).skillId])
        skillsService.assignDependency([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId, dependentSkillId: proj1_skills.get(3).skillId])

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
        skillsService.addBadge([projectId: proj1.projectId, badgeId: badge1, name: 'Badge 1', description: 'This is a first badge', iconClass: "fa fa-seleted-icon",])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge1, skillId: proj1_skills.get(0).skillId])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge1, skillId: proj1_skills.get(1).skillId])

        skillsService.assignDependency([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId, dependentSkillId: proj1_skills.get(2).skillId])
        skillsService.assignDependency([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId, dependentSkillId: proj1_skills.get(3).skillId])

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
        skillsService.addBadge([projectId: proj1.projectId, badgeId: badge1, name: 'Badge 1', description: 'This is a first badge', iconClass: "fa fa-seleted-icon",])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge1, skillId: proj1_skills.get(0).skillId])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge1, skillId: proj1_skills.get(1).skillId])

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
        skillsService.addBadge([projectId: proj1.projectId, badgeId: badge1, name: 'Badge 1', description: 'This is a first badge', iconClass: "fa fa-seleted-icon",])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge1, skillId: proj1_skills.get(0).skillId])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge1, skillId: proj1_skills.get(1).skillId])

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
        badgeIds.each {
            skillsService.addBadge([projectId: proj1.projectId, badgeId: it, name: it, description: "This is ${it}".toString(), iconClass: "fa fa-${it}".toString()])
        }

        skillsService.moveBadgeDown([projectId: proj1.projectId, badgeId: badgeIds[0]])
        skillsService.moveBadgeDown([projectId: proj1.projectId, badgeId: badgeIds[0]])
        skillsService.moveBadgeUp([projectId: proj1.projectId, badgeId: badgeIds[2]])

        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badgeIds.get(0), skillId: proj1_skills.get(0).skillId])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badgeIds.get(0), skillId: proj1_skills.get(1).skillId])
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId, new Date())

        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badgeIds.get(2), skillId: proj1_skills.get(0).skillId])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badgeIds.get(2), skillId: proj1_skills.get(1).skillId])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badgeIds.get(2), skillId: proj1_skills.get(2).skillId])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badgeIds.get(2), skillId: proj1_skills.get(3).skillId])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badgeIds.get(2), skillId: proj1_skills.get(4).skillId])

        when:
        def summaries = skillsService.getBadgesSummary(userId, proj1.projectId)
        then:
        summaries.size() == 3
        summaries.get(2).badge == "badge1"
        summaries.get(2).badgeId == "badge1"
        summaries.get(2).iconClass == "fa fa-badge1"
        summaries.get(2).numSkillsAchieved == 1
        summaries.get(2).numTotalSkills == 2

        summaries.get(1).badge == "badge2"
        summaries.get(1).badgeId == "badge2"
        summaries.get(1).iconClass == "fa fa-badge2"
        summaries.get(1).numSkillsAchieved == 0
        summaries.get(1).numTotalSkills == 0

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

        skillsService.addBadge([projectId: proj1.projectId, badgeId: badge1, name: 'Badge 1', description: 'This is a first badge', iconClass: "fa fa-seleted-icon",])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge1, skillId: proj1_skills.get(0).skillId])

        // proj2
        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 1)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 1)
        proj2_skills.get(0).pointIncrement=100

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)
        skillsService.createSkills(proj2_skills)

        skillsService.addBadge([projectId: proj2.projectId, badgeId: badge1, name: 'Badge 1', description: 'This is a first badge', iconClass: "fa fa-seleted-icon",])
        skillsService.assignSkillToBadge([projectId: proj2.projectId, badgeId: badge1, skillId: proj2_skills.get(0).skillId])

        // global badge
        Map badge = [badgeId: "globalBadge", name: 'Badge 1', description: 'This is a first badge', iconClass: "fa fa-seleted-icon"]
        badge.helpUrl = "http://foo.org"
        supervisorSkillsService.createGlobalBadge(badge)
        supervisorSkillsService.assignSkillToGlobalBadge(projectId: proj1.projectId, badgeId: badge.badgeId, skillId: proj1_skills.get(0).skillId)

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

        summary2.badges.numBadgesCompleted == 1
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
}
