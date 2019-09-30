package skills.intTests.clientDisplay


import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory

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
        skill1.totalPoints == 10
        skill1.todaysPoints == 10
        skill1.points == 10
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
}
