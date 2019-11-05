package skills.intTests.adminDisplayOrder

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory

class BadgesOrderSpecs extends DefaultIntSpec {

    def proj
    List badges

    def setup() {
        proj = SkillsFactory.createProject()
        int numBadges = 5

        skillsService.createProject(proj)
        badges = (1..numBadges).collect {
            def badge = SkillsFactory.createBadge(1, it)
            skillsService.createBadge(badge)
            return badge
        }
    }

    def "move badge down"() {
        when:
        def beforeMove = skillsService.getBadges(proj.projectId)
        skillsService.moveBadgeDown(badges.first())
        def afterMove = skillsService.getBadges(proj.projectId)
        then:
        beforeMove.collect({ it.badgeId }) == ["badge1", "badge2", "badge3", "badge4", "badge5"]
        afterMove.collect({ it.badgeId }) == ["badge2", "badge1", "badge3", "badge4", "badge5"]
    }

    def "should not be able to move down the last badge"() {
        when:
        def beforeMove = skillsService.getBadges(proj.projectId)
        skillsService.moveBadgeDown(badges.last())
        then:
        thrown(SkillsClientException)
        beforeMove.collect({ it.badgeId }) == ["badge1", "badge2", "badge3", "badge4", "badge5"]
        skillsService.getBadges(proj.projectId).collect({
            it.badgeId
        }) == ["badge1", "badge2", "badge3", "badge4", "badge5"]
    }

    def "move badge up"() {
        when:
        def beforeMove = skillsService.getBadges(proj.projectId)
        skillsService.moveBadgeUp(badges.get(1))
        def afterMove = skillsService.getBadges(proj.projectId)
        then:
        beforeMove.collect({ it.badgeId }) == ["badge1", "badge2", "badge3", "badge4", "badge5"]
        afterMove.collect({ it.badgeId }) == ["badge2", "badge1", "badge3", "badge4", "badge5"]
    }

    def "should not be able to move up the first badge"() {
        when:
        def beforeMove = skillsService.getBadges(proj.projectId)
        skillsService.moveBadgeUp(badges.first())
        then:
        thrown(SkillsClientException)
        beforeMove.collect({ it.badgeId }) == ["badge1", "badge2", "badge3", "badge4", "badge5"]
        skillsService.getBadges(proj.projectId).collect({
            it.badgeId
        }) == ["badge1", "badge2", "badge3", "badge4", "badge5"]
    }
}
