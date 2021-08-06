/**
 * Copyright 2021 SkillTree
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
package skills.intTests

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.EmailUtils
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.utils.WaitFor

class ContactProjectAdminsSpec extends DefaultIntSpec {

    SkillsService rootServiceOne

    def setup() {
        startEmailServer()

        rootServiceOne = createRootSkillService()
        if (!rootServiceOne.rootUsers) {
            rootServiceOne.grantRoot()
        }
    }

    def "contact project admins"() {
        def users = getRandomUsers(15, true)

        SkillsService proj1Serv = createService(users[0])
        SkillsService proj2Serv = createService(users[1])
        SkillsService proj3Serv = createService(users[2])

        def proj1 = SkillsFactory.createProject(1)
        def subj1 = SkillsFactory.createSubject()
        def skill = SkillsFactory.createSkill(1,1,1,0,10,480, 100)
        def proj2 = SkillsFactory.createProject(2)
        def proj3 = SkillsFactory.createProject(3)


        proj1Serv.createProject(proj1)
        proj1Serv.createSubject(subj1)
        proj1Serv.createSkill(skill)

        proj2Serv.createProject(proj2)
        proj3Serv.createProject(proj3)

        createService(users[3])
        createService(users[4])
        createService(users[5])
        proj1Serv.addUserRole(users[3], proj1.projectId, "ROLE_PROJECT_ADMIN")
        proj2Serv.addUserRole(users[4], proj2.projectId, "ROLE_PROJECT_ADMIN")
        proj3Serv.addUserRole(users[5], proj3.projectId, "ROLE_PROJECT_ADMIN")

        proj1Serv.addSkill(skill, users[6])
        proj1Serv.addSkill(skill, users[7])
        proj1Serv.addSkill(skill, users[8])
        proj1Serv.addSkill(skill, users[9])
        proj1Serv.addSkill(skill, users[10])

        when:
        def count = rootServiceOne.countAllProjectAdminsWithEmail()
        rootServiceOne.contactAllProjectAdmins("test subject", "# test email body")

        WaitFor.wait { greenMail.getReceivedMessages().size() >= 7 }

        def messages = EmailUtils.getEmails(greenMail)

        then:
        count == 7 //inception adds +1 to the expectation
        messages.size() == count
        messages.find { it.recipients.find {it.contains(users[0])}}
        messages.find { it.recipients.find {it.contains(users[1])}}
        messages.find { it.recipients.find {it.contains(users[2])}}
        messages.find { it.recipients.find {it.contains(users[3])}}
        messages.find { it.recipients.find {it.contains(users[4])}}
        messages.find { it.recipients.find {it.contains(users[5])}}
        messages.find { it.recipients.find {it.contains(DEFAULT_ROOT_USER_ID)}} //inception

        messages.collect {
            assert it.html.replaceAll('\r\n', '\n') == '''<!--
Copyright 2020 SkillTree

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

<h1>test email body</h1>


</body>
</html>'''
        }
    }

    def "preview email"() {

        when:
        rootServiceOne.previewEmail("a subject", "**body**")

        assert WaitFor.wait { greenMail.getReceivedMessages().size() >= 1 }
        def messages = EmailUtils.getEmails(greenMail)

        then:
        messages.size() == 1
        messages[0].recipients.find { it.contains(DEFAULT_ROOT_USER_ID) }
        messages[0].html.replaceAll('\r\n', '\n') == '''<!--
Copyright 2020 SkillTree

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

<p><strong>body</strong></p>


</body>
</html>'''
    }
}
