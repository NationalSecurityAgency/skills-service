/**
 * Copyright 2025 SkillTree
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
package skills.intTests.inviteOnly

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.EmailUtils
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.storage.model.UserAttrs
import skills.storage.model.auth.RoleName
import skills.utils.WaitFor

class RequestNewInviteSpec extends DefaultIntSpec {

    def setup() {
        startEmailServer()
    }

    def "request new invite - 1 admin" () {
        def proj = SkillsFactory.createProject(1)
        skillsService.createProject(proj)
        skillsService.configuredProjectAsInviteOnly(proj.projectId)
        String userId = getRandomUsers(1)[0]
        SkillsService otherUser = createService(userId)
        when:
        otherUser.requestNewProjectInvite(proj.projectId)

        WaitFor.wait { greenMail.getReceivedMessages().size() >= 1 }
        assert greenMail.getReceivedMessages().size() >= 1

        def messages = EmailUtils.getEmails(greenMail)

        UserAttrs user0Attrs = userAttrsRepo.findByUserIdIgnoreCase(skillsService.userName)
        UserAttrs user1Attrs = userAttrsRepo.findByUserIdIgnoreCase(otherUser.userName)
        otherUser
        then:
        messages.size() == 1
        def message = messages[0]
        message.subj == "New Invite Request for SkillTree Project"
        message.fromEmail == [user1Attrs.email]
        message.recipients == [user0Attrs.email]
        message.html.replaceAll('\r\n', '\n') == '''<!--
Copyright 2025 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->
<!DOCTYPE html>
<html   lang="en"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.thymeleaf.org http://www.thymeleaf.org">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
</head>
<body class="overall-container">

<p>User <b>''' + user1Attrs.userIdForDisplay + '''</b> has requested a new invite for <b>Test Project#1</b> because the current invite is no longer valid.</p>
<p>Please visit project's <a href="http://localhost:''' + localPort + '''/administrator/projects/TestProject1/access">Access Page</a> to view current invites or to create a new invite.</p>
<p>Here is a link to the documentation for how to invite and manage users of Private Invite Only projects: <a href="https://skilltreeplatform.dev/dashboard/user-guide/projects.html#invite-only">Private Invite Only Documentation</a></p>
<p>
    Always yours, <br/> -SkillTree Bot
</p>

</body>
</html>'''.replaceAll('\r\n', '\n')

        message.plainText.replaceAll('\r\n', '\n') == "User ${user1Attrs.userIdForDisplay} has requested a new invite for Test Project#1 because the current invite is no longer valid.\n" +
                "\n" +
                "Please visit project's Access Page to view current invites or to create a new invite: http://localhost:${localPort}/administrator/projects/TestProject1/access\n" +
                "\n" +
                "Here is a link to the documentation for how to invite and manage users of Private Invite Only projects: https://skilltreeplatform.dev/dashboard/user-guide/projects.html#invite-only" +
                "\n\n" +
                "\nAlways yours," +
                "\nSkillTree Bot".replaceAll('\r\n', '\n')

    }

    def "request new invite - multiple admin" () {
        def proj = SkillsFactory.createProject(1)
        skillsService.createProject(proj)
        skillsService.configuredProjectAsInviteOnly(proj.projectId)
        List<String> userIds = getRandomUsers(3)
        List<SkillsService> otherUsers = userIds.collect { createService(it) }

        skillsService.addUserRole(otherUsers[0].userName, proj.projectId, RoleName.ROLE_PROJECT_ADMIN.toString())
        skillsService.addUserRole(otherUsers[1].userName, proj.projectId, RoleName.ROLE_PROJECT_ADMIN.toString())

        WaitFor.wait { greenMail.getReceivedMessages().size() >= 2 }
        greenMail.purgeEmailFromAllMailboxes()
        when:
        otherUsers[2].requestNewProjectInvite(proj.projectId)

        WaitFor.wait { greenMail.getReceivedMessages().size() >= 3 }
        assert greenMail.getReceivedMessages().size() >= 3

        def messages = EmailUtils.getEmails(greenMail)

        UserAttrs skillsServiceUser = userAttrsRepo.findByUserIdIgnoreCase(skillsService.userName)
        List<UserAttrs> userAttrs = otherUsers.collect { userAttrsRepo.findByUserIdIgnoreCase(it.userName) }
        then:
        messages.each {message ->
            assert message.subj == "New Invite Request for SkillTree Project"
            assert message.fromEmail == [userAttrs[2].email]
            assert message.plainText.replaceAll('\r\n', '\n') == "User ${userAttrs[2].userIdForDisplay} has requested a new invite for Test Project#1 because the current invite is no longer valid.\n" +
                    "\n" +
                    "Please visit project's Access Page to view current invites or to create a new invite: http://localhost:${localPort}/administrator/projects/TestProject1/access\n" +
                    "\n" +
                    "Here is a link to the documentation for how to invite and manage users of Private Invite Only projects: https://skilltreeplatform.dev/dashboard/user-guide/projects.html#invite-only" +
                    "\n\n" +
                    "\nAlways yours," +
                    "\nSkillTree Bot".replaceAll('\r\n', '\n')
            assert message.recipients.sort() == [skillsServiceUser.email, userAttrs[0].email, userAttrs[1].email]
        }
    }

    def "when configured headers and footer is included in the email"() {
        SkillsService rootSkillsService = createRootSkillService()
        rootSkillsService.saveEmailHeaderAndFooterSettings(
                '<p>Header attention {{ community.descriptor }} Members</p>',
                '<p>Footer attention {{ community.descriptor }} Members</p>',
                'Plain Text Header Attention {{ community.descriptor }} Members',
                'Plain Text Footer Attention {{ community.descriptor }} Members')

        def proj = SkillsFactory.createProject(1)
        skillsService.createProject(proj)
        skillsService.configuredProjectAsInviteOnly(proj.projectId)

        String userId = getRandomUsers(1)[0]
        SkillsService otherUser = createService(userId)
        when:
        otherUser.requestNewProjectInvite(proj.projectId)

        WaitFor.wait { greenMail.getReceivedMessages().size() >= 1 }
        assert greenMail.getReceivedMessages().size() >= 1

        def messages = EmailUtils.getEmails(greenMail)

        UserAttrs user0Attrs = userAttrsRepo.findByUserIdIgnoreCase(skillsService.userName)
        UserAttrs user1Attrs = userAttrsRepo.findByUserIdIgnoreCase(otherUser.userName)
        then:
        messages.size() == 1
        def message = messages[0]
        message.subj == "New Invite Request for SkillTree Project"
        message.fromEmail == [user1Attrs.email]
        message.recipients == [user0Attrs.email]
        message.plainText.startsWith("Plain Text Header Attention All Dragons Members")
        message.plainText.endsWith("Plain Text Footer Attention All Dragons Members")

        message.html.contains("<body class=\"overall-container\">\r\n<p>Header attention All Dragons Members</p>\r\n<p>User")
        message.html.contains("<p>Footer attention All Dragons Members</p>\r\n</body>")
    }
}
