package skills.intTests.clientDisplay

import groovy.util.logging.Slf4j
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService

@Slf4j
class ClientDisplayGlobalBadgesSpec extends DefaultIntSpec {
    String projId = SkillsFactory.defaultProjId
    String globalBadgeId = 'GlobalBadge1'

    String ultimateRoot = 'jh@dojo.com'
    SkillsService rootSkillsService
    String supervisorUserId = 'foo@bar.com'
    SkillsService supervisorSkillsService

    def setup(){
        skillsService.deleteProjectIfExist(projId)
        rootSkillsService = createService(ultimateRoot, 'aaaaaaaa')
        supervisorSkillsService = createService(supervisorUserId)

        if (!rootSkillsService.isRoot()) {
            rootSkillsService.grantRoot()
        }
        rootSkillsService.grantSupervisorRole(supervisorUserId)
    }

    def cleanup() {
        deleteGlobalBadgeIfExists(globalBadgeId)
        rootSkillsService?.removeSupervisorRole(supervisorUserId)
    }

    def "badges summary for a project - one badge"() {
        String userId = "user1"

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        Map badge = [badgeId: globalBadgeId, name: 'Badge 1', description: 'This is a first badge', iconClass: "fa fa-seleted-icon"]
        badge.helpUrl = "http://foo.org"
        supervisorSkillsService.createGlobalBadge(badge)
        supervisorSkillsService.assignProjectLevelToGlobalBadge(projectId: projId, badgeId: badge.badgeId, level: "3")
        supervisorSkillsService.assignSkillToGlobalBadge(projectId: projId, badgeId: badge.badgeId, skillId: proj1_skills.get(0).skillId)

        when:
        def summaries = skillsService.getBadgesSummary(userId, projId)
        then:
        summaries.size() == 1
        def summary = summaries.first()
        summary.badge == "Badge 1"
        summary.badgeId == globalBadgeId
        summary.global
        !summary.gem
        !summary.startDate
        !summary.endDate
        summary.numTotalSkills == 2
        summary.numSkillsAchieved == 0
        summary.iconClass == "fa fa-seleted-icon"
        summary.helpUrl == "http://foo.org"
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

        Map badge = [badgeId: globalBadgeId, name: 'Badge 1', description: 'This is a first badge', iconClass: "fa fa-seleted-icon",]
        supervisorSkillsService.createGlobalBadge(badge)
        supervisorSkillsService.assignSkillToGlobalBadge(projectId: projId, badgeId: badge.badgeId, skillId: proj1_skills.get(0).skillId)

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId, new Date())

        when:
        def summaries = skillsService.getBadgesSummary(userId, proj1.projectId)
        then:
        summaries.size() == 1
        def summary = summaries.first()
        summary.badge == "Badge 1"
        summary.badgeId == globalBadgeId
        summary.global
        !summary.gem
        !summary.startDate
        !summary.endDate
        summary.numTotalSkills == 1
        summary.numSkillsAchieved == 1
        summary.iconClass == "fa fa-seleted-icon"
    }

    def "badges summary for a project level - one badge - achieved"() {
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

        Map badge = [badgeId: globalBadgeId, name: 'Badge 1', description: 'This is a first badge', iconClass: "fa fa-seleted-icon",]
        supervisorSkillsService.createGlobalBadge(badge)
//        supervisorSkillsService.assignSkillToGlobalBadge(projectId: projId, badgeId: badge.badgeId, skillId: proj1_skills.get(0).skillId)

        supervisorSkillsService.assignProjectLevelToGlobalBadge(projectId: proj1.projectId, badgeId: badge.badgeId, level: "1")

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId, new Date())

        when:
        def summaries = skillsService.getBadgesSummary(userId, proj1.projectId)
        then:
        summaries.size() == 1
        def summary = summaries.first()
        summary.badge == "Badge 1"
        summary.badgeId == globalBadgeId
        summary.global
        !summary.gem
        !summary.startDate
        !summary.endDate
        summary.numTotalSkills == 1
        summary.numSkillsAchieved == 1
        summary.projectLevelsAndSkillsSummaries.projectLevel.achievedLevel[0] >= summary.projectLevelsAndSkillsSummaries.projectLevel.requiredLevel[0]
        summary.iconClass == "fa fa-seleted-icon"
    }


    def "badges summary for a project - few badges"() {
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

        List<String> badgeIds = (1..3).collect({ "${globalBadgeId}${it}".toString()})
        badgeIds.each {
            Map badge = [badgeId: it, name: it, description: "This is ${it}".toString(), iconClass: "fa fa-${it}".toString(),]
            supervisorSkillsService.createGlobalBadge(badge)
        }

        supervisorSkillsService.assignSkillToGlobalBadge([projectId: proj1.projectId, badgeId: badgeIds.get(0), skillId: proj1_skills.get(0).skillId])
        supervisorSkillsService.assignSkillToGlobalBadge([projectId: proj1.projectId, badgeId: badgeIds.get(0), skillId: proj1_skills.get(1).skillId])
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId, new Date())

        supervisorSkillsService.assignProjectLevelToGlobalBadge(projectId: proj1.projectId, badgeId: badgeIds.get(1), level: "3")

        supervisorSkillsService.assignSkillToGlobalBadge([projectId: proj1.projectId, badgeId: badgeIds.get(2), skillId: proj1_skills.get(0).skillId])
        supervisorSkillsService.assignSkillToGlobalBadge([projectId: proj1.projectId, badgeId: badgeIds.get(2), skillId: proj1_skills.get(1).skillId])
        supervisorSkillsService.assignSkillToGlobalBadge([projectId: proj1.projectId, badgeId: badgeIds.get(2), skillId: proj1_skills.get(2).skillId])
        supervisorSkillsService.assignSkillToGlobalBadge([projectId: proj1.projectId, badgeId: badgeIds.get(2), skillId: proj1_skills.get(3).skillId])
        supervisorSkillsService.assignSkillToGlobalBadge([projectId: proj1.projectId, badgeId: badgeIds.get(2), skillId: proj1_skills.get(4).skillId])

        when:
        def summaries = skillsService.getBadgesSummary(userId, proj1.projectId)
        then:
        summaries.size() == 3
        summaries.get(0).badge == badgeIds[0]
        summaries.get(0).badgeId == badgeIds[0]
        summaries.get(0).iconClass == "fa fa-${badgeIds[0]}"
        summaries.get(0).numSkillsAchieved == 1
        summaries.get(0).numTotalSkills == 2

        summaries.get(1).badge == badgeIds[1]
        summaries.get(1).badgeId == badgeIds[1]
        summaries.get(1).iconClass == "fa fa-${badgeIds[1]}"
        summaries.get(1).numSkillsAchieved == 0
        summaries.get(1).numTotalSkills == 1

        summaries.get(2).badge == badgeIds[2]
        summaries.get(2).badgeId == badgeIds[2]
        summaries.get(2).iconClass == "fa fa-${badgeIds[2]}"
        summaries.get(2).numSkillsAchieved == 1
        summaries.get(2).numTotalSkills == 5

        cleanup:
        badgeIds.each {
            deleteGlobalBadgeIfExists(it)
        }
    }

    def "single badge summary"() {
        String userId = "user1"

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        Map badge = [badgeId: globalBadgeId, name: 'Badge 1', description: 'This is a first badge', iconClass: "fa fa-seleted-icon",]
        supervisorSkillsService.createGlobalBadge(badge)
        supervisorSkillsService.assignSkillToGlobalBadge(projectId: projId, badgeId: badge.badgeId, skillId: proj1_skills.get(0).skillId)

        when:
        def summary = skillsService.getBadgeSummary(userId, proj1.projectId, globalBadgeId, -1, true)
        then:
        summary.badge == "Badge 1"
        summary.badgeId == globalBadgeId
        summary.global
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
        !summary.skills.get(0).crossProject
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

        Map badge = [badgeId: globalBadgeId, name: 'Badge 1', description: 'This is a first badge', iconClass: "fa fa-seleted-icon",]
        supervisorSkillsService.createGlobalBadge(badge)

        supervisorSkillsService.assignSkillToGlobalBadge([projectId: proj1.projectId, badgeId: globalBadgeId, skillId: proj1_skills.get(0).skillId])
        supervisorSkillsService.assignSkillToGlobalBadge([projectId: proj1.projectId, badgeId: globalBadgeId, skillId: proj1_skills.get(1).skillId])
        supervisorSkillsService.assignSkillToGlobalBadge([projectId: proj1.projectId, badgeId: globalBadgeId, skillId: proj1_skills.get(2).skillId])

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId, new Date())

        when:
        def summary = skillsService.getBadgeSummary(userId, proj1.projectId, globalBadgeId, -1, true)
        then:
        summary.badge == "Badge 1"
        summary.badgeId == globalBadgeId
        summary.global
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
            it.pointIncrement = 40
        }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

//        String badge1 = "badge1"
//        skillsService.addBadge([projectId: proj1.projectId, badgeId: badge1, name: 'Badge 1', description: 'This is a first badge', iconClass: "fa fa-seleted-icon",])
//        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge1, skillId: proj1_skills.get(0).skillId])
//        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge1, skillId: proj1_skills.get(1).skillId])


        Map badge = [badgeId: globalBadgeId, name: 'Badge 1', description: 'This is a first badge', iconClass: "fa fa-seleted-icon",]
        supervisorSkillsService.createGlobalBadge(badge)

        supervisorSkillsService.assignSkillToGlobalBadge([projectId: proj1.projectId, badgeId: globalBadgeId, skillId: proj1_skills.get(0).skillId])
        supervisorSkillsService.assignSkillToGlobalBadge([projectId: proj1.projectId, badgeId: globalBadgeId, skillId: proj1_skills.get(1).skillId])




        skillsService.assignDependency([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId, dependentSkillId: proj1_skills.get(2).skillId])
        skillsService.assignDependency([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId, dependentSkillId: proj1_skills.get(3).skillId])

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId, new Date())

        when:
        def summary = skillsService.getBadgeSummary(userId, proj1.projectId, globalBadgeId, -1, true)
        then:
        summary.badge == "Badge 1"
        summary.badgeId == globalBadgeId
        summary.global

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
            it.pointIncrement=25
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

        Map badge = [badgeId: globalBadgeId, name: 'Badge 1', description: 'This is a first badge', iconClass: "fa fa-seleted-icon",]
        supervisorSkillsService.createGlobalBadge(badge)

        supervisorSkillsService.assignSkillToGlobalBadge([projectId: proj1.projectId, badgeId: globalBadgeId, skillId: proj1_skills.get(0).skillId])
        supervisorSkillsService.assignSkillToGlobalBadge([projectId: proj1.projectId, badgeId: globalBadgeId, skillId: proj1_skills.get(1).skillId])

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

        Map badge = [badgeId: globalBadgeId, name: 'Badge 1', description: 'This is a first badge', iconClass: "fa fa-seleted-icon",]
        supervisorSkillsService.createGlobalBadge(badge)

        supervisorSkillsService.assignSkillToGlobalBadge([projectId: proj1.projectId, badgeId: globalBadgeId, skillId: proj1_skills.get(0).skillId])
        supervisorSkillsService.assignSkillToGlobalBadge([projectId: proj1.projectId, badgeId: globalBadgeId, skillId: proj1_skills.get(1).skillId])

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId, new Date())
        def res = skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId], userId, new Date())
        println res

        when:
        def summary = skillsService.getSkillSummary(userId, proj1.projectId)
        then:
        summary.badges.numBadgesCompleted == 1
        summary.badges.enabled
    }

    def "badges summaries are returned sorted by displayOrder"() {
        String userId = "user1"

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(5, 1, 1, 20)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        List<String> badgeIds = (1..3).collect({ "${globalBadgeId}${it}".toString()})
        badgeIds.each {
            Map badge = [badgeId: it, name: it, description: "This is ${it}".toString(), iconClass: "fa fa-${it}".toString(),]
            supervisorSkillsService.createGlobalBadge(badge)
        }

        supervisorSkillsService.moveGlobalBadgeDown([badgeId: badgeIds[0]])
        supervisorSkillsService.moveGlobalBadgeDown([badgeId: badgeIds[0]])
        supervisorSkillsService.moveGlobalBadgeUp([badgeId: badgeIds[2]])

        supervisorSkillsService.assignSkillToGlobalBadge([projectId: proj1.projectId, badgeId: badgeIds.get(0), skillId: proj1_skills.get(0).skillId])
        supervisorSkillsService.assignSkillToGlobalBadge([projectId: proj1.projectId, badgeId: badgeIds.get(0), skillId: proj1_skills.get(1).skillId])
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId, new Date())

        supervisorSkillsService.assignProjectLevelToGlobalBadge(projectId: proj1.projectId, badgeId: badgeIds.get(1), level: "3")

        supervisorSkillsService.assignSkillToGlobalBadge([projectId: proj1.projectId, badgeId: badgeIds.get(2), skillId: proj1_skills.get(0).skillId])
        supervisorSkillsService.assignSkillToGlobalBadge([projectId: proj1.projectId, badgeId: badgeIds.get(2), skillId: proj1_skills.get(1).skillId])
        supervisorSkillsService.assignSkillToGlobalBadge([projectId: proj1.projectId, badgeId: badgeIds.get(2), skillId: proj1_skills.get(2).skillId])
        supervisorSkillsService.assignSkillToGlobalBadge([projectId: proj1.projectId, badgeId: badgeIds.get(2), skillId: proj1_skills.get(3).skillId])
        supervisorSkillsService.assignSkillToGlobalBadge([projectId: proj1.projectId, badgeId: badgeIds.get(2), skillId: proj1_skills.get(4).skillId])

        when:
        def summaries = skillsService.getBadgesSummary(userId, proj1.projectId)
        then:
        summaries.size() == 3
        summaries.get(2).badge == badgeIds[0]
        summaries.get(2).badgeId == badgeIds[0]
        summaries.get(2).iconClass == "fa fa-${badgeIds[0]}"
        summaries.get(2).numSkillsAchieved == 1
        summaries.get(2).numTotalSkills == 2

        summaries.get(1).badge == badgeIds[1]
        summaries.get(1).badgeId == badgeIds[1]
        summaries.get(1).iconClass == "fa fa-${badgeIds[1]}"
        summaries.get(1).numSkillsAchieved == 0
        summaries.get(1).numTotalSkills == 1

        summaries.get(0).badge == badgeIds[2]
        summaries.get(0).badgeId == badgeIds[2]
        summaries.get(0).iconClass == "fa fa-${badgeIds[2]}"
        summaries.get(0).numSkillsAchieved == 1
        summaries.get(0).numTotalSkills == 5

        cleanup:
        badgeIds.each {
            deleteGlobalBadgeIfExists(it)
        }
    }

    def "badge skill summaries from within a different project"() {

        String userId = "user1"

        def proj1 = SkillsFactory.createProject(1)
        def proj2 = SkillsFactory.createProject(2)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        def proj2_subj = SkillsFactory.createSubject(2, 2)
        List<Map> proj1_skills = SkillsFactory.createSkills(2, 1, 1, 50)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 2, 50)

        skillsService.createProject(proj1)
        skillsService.createProject(proj2)
        skillsService.createSubject(proj1_subj)
        skillsService.createSubject(proj2_subj)
        skillsService.createSkills(proj1_skills)
        skillsService.createSkills(proj2_skills)

        Map badge = [badgeId: "bid1", name: "global badge", description: "gbadge".toString(), iconClass: "fa fa-foo".toString(),]
        supervisorSkillsService.createGlobalBadge(badge)

        supervisorSkillsService.assignSkillToGlobalBadge([projectId: proj1.projectId, badgeId: "bid1", skillId: proj1_skills.get(0).skillId])
        supervisorSkillsService.assignSkillToGlobalBadge([projectId: proj2.projectId, badgeId: "bid1", skillId: proj2_skills.get(0).skillId])
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId, new Date())

        when:
        def summaries = skillsService.getBadgesSummary(userId, proj1.projectId)
        def skillSummary = skillsService.getCrossProjectSkillSummary(userId, proj1.projectId, proj2.projectId, proj2_skills.get(0).skillId)
        def globalBadge = skillsService.getBadgeSummary(userId, proj1.projectId, badge.badgeId, -1, true)

        then:
        summaries
        skillSummary
        List proj1Skills = globalBadge.skills.findAll { it.projectId == proj1.projectId }
        proj1Skills.size() == 1
        !proj1Skills.get(0).crossProject

        List proj2Skills = globalBadge.skills.findAll { it.projectId == proj2.projectId }
        proj2Skills.size() == 1
        proj2Skills.get(0).crossProject
    }

    private deleteGlobalBadgeIfExists(String badgeId) {
        try {
            if (supervisorSkillsService?.getGlobalBadge(badgeId)) {
                supervisorSkillsService.deleteGlobalBadge(badgeId)
            }
        } catch (SkillsClientException e) {
            log.error("Unabled to delete global badge with id [$badgeId]: ${e.message}")
        }
    }
}
