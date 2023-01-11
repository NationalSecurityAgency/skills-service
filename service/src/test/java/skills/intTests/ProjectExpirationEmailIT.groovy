package skills.intTests

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import skills.SpringBootApp
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.EmailUtils
import skills.intTests.utils.SkillsFactory
import skills.services.ProjectExpirationService
import skills.storage.model.UserAttrs
import skills.utils.WaitFor

import java.util.regex.Pattern

@SpringBootTest(properties = ['skills.h2.port=9150',
        'skills.config.expirationGracePeriod=0'],
        webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, classes = SpringBootApp)
class ProjectExpirationEmailIT extends DefaultIntSpec {

    @Autowired
    ProjectExpirationService expirationService

    def setup() {
        startEmailServer()
    }

    def "email notification formatted appropriately"() {
        def proj1 = SkillsFactory.createProject(1)

        skillsService.createProject(proj1)
        Date flagForExpiration = new Date()
        expirationService.flagOldProjects(flagForExpiration)

        String otherUser = getRandomUsers(1, false, ['skills@skills.org', DEFAULT_ROOT_USER_ID]).first()

        createService(otherUser)
        skillsService.addProjectAdmin(proj1.projectId, otherUser)

        WaitFor.wait { greenMail.getReceivedMessages().size() == 1 }
        greenMail.purgeEmailFromAllMailboxes()

        UserAttrs projectAdminUserAttrs = userAttrsRepo.findByUserId(skillsService.userName)
        UserAttrs otherProjectAdminUserAttrs = userAttrsRepo.findByUserId(otherUser)
        UserAttrs rootUserUserAttrs = userAttrsRepo.findByUserId(DEFAULT_ROOT_USER_ID.toLowerCase())

        when:
        expirationService.notifyGracePeriodProjectAdmins(flagForExpiration.minus(1))
        assert WaitFor.wait { greenMail.getReceivedMessages().size() > 2 }
        List<EmailUtils.EmailRes> emails = EmailUtils.getEmails(greenMail)

        Pattern plaintTextMatch = ~/(?s)Your SkillTree Project Inception, created on \d{4}-\d{2}-\d{2} hasn't been used in at least \d+ days?.*If you take no action, Project Inception will be deleted today \(\d{4}-\d{2}-\d{2}\).*If you wish to stop receiving these emails, visit http:\/\/localhost:\d+\/administrator\/projects\/Inception in the SkillTree dashboard and click the 'Keep' button or delete your Project.*/

        Pattern h1 = ~/(?s)<h1>Your SkillTree Project Isn't Being Used!<\/h1>.+?/
        Pattern p1 = ~/(?s)<p>Your SkillTree Project Test Project#1, created on \d{4}-\d{2}-\d{2} hasn't been used in at least \d+ days?.<\/p>.+?/
        Pattern p2 = ~/(?s)<p>If you take no action, Project Test Project#1 will be deleted today \(\d{4}-\d{2}-\d{2}\).<\/p>.+?/
        Pattern p3 = ~/(?s)<p>If you wish to stop receiving these emails, visit <a href="http:\/\/localhost:\d+\/administrator\/projects\/TestProject1">Test Project#1<\/a> in the SkillTree dashboard and click the 'Keep' button or delete your Project.<\/p>.*/


        then:
        emails.size() == 3
        emails.collect {it.recipients[0] }.sort() == [rootUserUserAttrs.email, projectAdminUserAttrs.email, otherProjectAdminUserAttrs.email].sort()
        emails.find { it.subj == "SkillTree Project is expiring!" }
        emails.find {it.recipients == [otherProjectAdminUserAttrs.email]}
        emails.find { plaintTextMatch.matcher(it.plainText).find() }
        emails.findAll {h1.matcher(it.html).find() }?.size() == 3
        emails.find { p1.matcher(it.html).find() }
        emails.find { p2.matcher(it.html).find() }
        emails.find { p3.matcher(it.html).find() }
    }
}
