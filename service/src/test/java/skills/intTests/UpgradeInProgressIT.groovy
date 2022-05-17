package skills.intTests

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowCallbackHandler
import skills.SpringBootApp
import skills.auth.UserAuthService
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.storage.repos.UserRoleRepo

import java.sql.ResultSet
import java.sql.SQLException

@Slf4j
@SpringBootTest(properties = ['skills.h2.port=9093',
        'skills.config.ui.rankingAndProgressViewsEnabled=false',
        'skills.config.ui.defaultLandingPage=progress',
        'skills.config.db-upgrade-in-progress=true',
        'skills.authorization.userInfoHealthCheckUri=https://localhost:8182/actuator/health'], webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, classes = SpringBootApp)
class UpgradeInProgressIT  extends DefaultIntSpec {

    @Autowired
    JdbcTemplate jdbcTemplate

    @Autowired
    UserAuthService userAuthService

    def setup() {
        userAuthService.grantRoot(skillsService.userName)
        insertData("upgrade_in_progress.sql")
        //projectId SampleProject
        //Skill3Skill - approval
        //Skill1Skill
        //Skill2Skill - honor system
        //Subject1Subject
    }

    private insertData(String sqlFilePath) {
        new ClassPathResource(sqlFilePath).getFile().eachLine { sqlStmt ->
            jdbcTemplate.execute(sqlStmt)
        }
    }

    def "cannot create new project when upgrade mode is enabled"() {
        when:

        def project = SkillsFactory.createProject(11)
        skillsService.createProject(project)

        then:
        def e = thrown(SkillsClientException)
        e.message.contains("A database upgrade is currently in progress, no training profile modifications are allowed at this time")
    }

    def "cannot add a subject to an existing project when upgrade mode is enabled"() {
        when:
        def subject = SkillsFactory.createSubject(11, 1)
        subject.projectId = "SampleProject"
        skillsService.createSubject(subject)

        then:
        def e = thrown(SkillsClientException)
        e.message.contains("A database upgrade is currently in progress, no training profile modifications are allowed at this time")
    }

    def "cannot add skills to existing subject when upgrade mode is enabled"() {
        when:
        def skill = SkillsFactory.createSkill(1, 1, 99)
        skill.projectId = "SampleProject"
        skill.subjectId = "Subject1Subject"
        skillsService.createSkill(skill)

        then:
        def e = thrown(SkillsClientException)
        e.message.contains("A database upgrade is currently in progress, no training profile modifications are allowed at this time")
    }

    def "cannot edit existing subject when upgrade mode is enabled"() {
        def subj = skillsService.getSubject([projectId: "SampleProject", subjectId: "Subject1Subject"])

        when:
        subj.name = "new name"
        skillsService.createSubject(subj)

        then:
        def e = thrown(SkillsClientException)
        e.message.contains("A database upgrade is currently in progress, no training profile modifications are allowed at this time")
    }

    def "cannot edit skill when upgrade mode is enabled"() {
        def skill = skillsService.getSkill([projectId: "SampleProject", subjectId: "Subject1Subject", skillId: "Skill1Skill"])

        when:
        skill.name = "new name"
        skillsService.createSkill(skill)

        then:
        def e = thrown(SkillsClientException)
        e.message.contains("A database upgrade is currently in progress, no training profile modifications are allowed at this time")
    }

    def "cannot remove levels from an existing project when upgrade mode is enabled"() {
        when:
        skillsService.deleteLevel("SampleProject")
        then:
        def e = thrown(SkillsClientException)
        e.message.contains("A database upgrade is currently in progress, no training profile modifications are allowed at this time")
    }

    def "cannot create new badge for project when upgrade mode is enabled"() {
        when:
        def badge = SkillsFactory.createBadge(1, 11)
        badge.projectId = "SampleProject"
        skillsService.createBadge(badge)

        then:
        def e = thrown(SkillsClientException)
        e.message.contains("A database upgrade is currently in progress, no training profile modifications are allowed at this time")
    }

    def "cannot edit existing badge when upgrade mode is enabled"() {
        def badge = skillsService.getBadge([projectId: "SampleProject", badgeId: "SampleBadgeBadge"])

        when:
        badge.name = "new name"
        skillsService.createBadge(badge)

        then:
        def e = thrown(SkillsClientException)
        e.message.contains("A database upgrade is currently in progress, no training profile modifications are allowed at this time")
    }

    def "cannot add skills to existing badge when upgrade mode is enabled"() {
        when:
        skillsService.assignSkillToBadge(projectId: "SampleProject", badgeId: "SampleBadgeBadge", skillId: "Skill1Skill")

        then:
        def e = thrown(SkillsClientException)
        e.message.contains("A database upgrade is currently in progress, no training profile modifications are allowed at this time")
    }

    def "can report skill events when upgrade mode is enabled"() {
        def user = getRandomUsers(1)[0]

        when:
        def result = skillsService.addSkill([projectId: "SampleProject", skillId: "Skill1Skill"], user)

        then:
        result.body.success
        !result.body.skillApplied
        result.body.explanation == "A database upgrade is currently in progress. This Skill Event Request has been queued for future application."
    }

}
