package skills.intTests.adminDisplayOrder

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService

class GlobalBadgeOrderSpecs extends DefaultIntSpec {

    SkillsService supervisorSkillsService
    List badges
    def setup() {
        String ultimateRoot = 'jh@dojo.com'
        SkillsService rootSkillsService = createService(ultimateRoot, 'aaaaaaaa')
        rootSkillsService.grantRoot()

        String supervisorUserId = 'foo@bar.com'
        supervisorSkillsService = createService(supervisorUserId)
        rootSkillsService.grantSupervisorRole(supervisorUserId)

        int numBadges = 5
        badges = (1..numBadges).collect {
            def badge = SkillsFactory.createBadge(1, it)
            supervisorSkillsService.createGlobalBadge(badge)
            return badge
        }
    }

    def "move badge down"() {
        when:
        def beforeMove = supervisorSkillsService.getAllGlobalBadges()
        supervisorSkillsService.moveGlobalBadgeDown(badges.first())
        def afterMove = supervisorSkillsService.getAllGlobalBadges()
        then:
        beforeMove.collect({it.badgeId}) == ["badge1", "badge2", "badge3", "badge4", "badge5"]
        afterMove.collect({it.badgeId}) == ["badge2", "badge1", "badge3", "badge4", "badge5"]
    }

    def "should not be able to move down the last badge"() {
        when:
        def beforeMove = supervisorSkillsService.getAllGlobalBadges()
        supervisorSkillsService.moveGlobalBadgeDown(badges.last())
        then:
        thrown(SkillsClientException)
        beforeMove.collect({it.badgeId}) == ["badge1", "badge2", "badge3", "badge4", "badge5"]
        supervisorSkillsService.getAllGlobalBadges().collect({it.badgeId}) == ["badge1", "badge2", "badge3", "badge4", "badge5"]
    }

    def "move badge up"() {
        when:
        def beforeMove = supervisorSkillsService.getAllGlobalBadges()
        supervisorSkillsService.moveGlobalBadgeUp(badges.get(1))
        def afterMove = supervisorSkillsService.getAllGlobalBadges()
        then:
        beforeMove.collect({it.badgeId}) == ["badge1", "badge2", "badge3", "badge4", "badge5"]
        afterMove.collect({it.badgeId}) == ["badge2", "badge1", "badge3", "badge4", "badge5"]
    }

    def "should not be able to move up the first badge"() {
        when:
        def beforeMove = supervisorSkillsService.getAllGlobalBadges()
        skillsService.moveGlobalBadgeUp(badges.first())
        then:
        thrown(SkillsClientException)
        beforeMove.collect({it.badgeId}) == ["badge1", "badge2", "badge3", "badge4", "badge5"]
        supervisorSkillsService.getAllGlobalBadges().collect({it.badgeId}) == ["badge1", "badge2", "badge3", "badge4", "badge5"]
    }
}
