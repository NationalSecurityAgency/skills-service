package skills.intTests

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.EmailUtils
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.utils.WaitFor

class EmailUserOnRoleAddSpecs extends DefaultIntSpec {
    SkillsService rootServiceOne

    def setup() {
        startEmailServer()

        rootServiceOne = createRootSkillService()
        if (!rootServiceOne.rootUsers) {
            rootServiceOne.grantRoot()
        }
    }

    def "contact user when added as admin"() {
        def users = getRandomUsers(2, true)

        SkillsService proj1Serv = createService(users[0])

        def proj1 = SkillsFactory.createProject(1)

        proj1Serv.createProject(proj1)
        createService(users[1])

        proj1Serv.addUserRole(users[1], proj1.projectId, "ROLE_PROJECT_ADMIN")

        when:
        def count = rootServiceOne.countAllProjectAdminsWithEmail()
        WaitFor.wait { greenMail.getReceivedMessages().size() >= 1 }
        def messages = EmailUtils.getEmails(greenMail)

        then:
        count == 3
        messages.size() == 1
        messages.find { it.recipients.find {it.contains(users[1])}}
        messages[0].subj == "Test email"
        messages[0].plainText == "Test email"
    }

    def "contact user when added as root"() {
        def users = getRandomUsers(2, true)

        SkillsService proj1Serv = createService(users[0])

        def proj1 = SkillsFactory.createProject(1)

        proj1Serv.createProject(proj1)
        createService(users[1])

        rootServiceOne.addRootRole(users[1])

        when:
        def count = rootServiceOne.countAllProjectAdminsWithEmail()
        WaitFor.wait { greenMail.getReceivedMessages().size() >= 1 }
        def messages = EmailUtils.getEmails(greenMail)

        then:
        count == 3
        messages.size() == 1
        messages.find { it.recipients.find {it.contains(users[1])}}
        messages[0].subj == "Test email"
        messages[0].plainText == "Test email"
    }
}
