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
package skills.intTests.reportSkills

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.EmailUtils
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.services.settings.SettingsService
import skills.storage.model.SkillApproval
import skills.storage.model.SkillDef
import skills.storage.model.UserAttrs
import skills.storage.repos.NotificationsRepo
import skills.storage.repos.SkillApprovalRepo
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.UserAttrsRepo
import skills.utils.WaitFor
import spock.lang.IgnoreRest

@Slf4j
class ReportSkills_SelfReportingSpecs extends DefaultIntSpec {

    @Autowired
    SkillApprovalRepo skillApprovalRepo

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    SettingsService settingsService

    @Autowired
    NotificationsRepo notificationsRepo

    @Autowired
    UserAttrsRepo  userAttrsRepo

    def setup() {
        startEmailServer()
    }

    def "self report skill with approval"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        Date date = new Date() - 60
        when:
        def res = skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], "user0", date, "Please approve this!")
        def approvalsEndpointRes = skillsService.getApprovals(proj.projectId, 5, 1, 'requestedOn', false)

        assert WaitFor.wait { greenMail.getReceivedMessages().size() > 0 }
        EmailUtils.EmailRes emailRes = EmailUtils.getEmail(greenMail)

        UserAttrs projectAdminUserAttrs = userAttrsRepo.findByUserId(skillsService.userName)
        UserAttrs userRequestingPtsAttrs = userAttrsRepo.findByUserId("user0")

        then:
        greenMail.getReceivedMessages().length == 1
        emailRes.subj == "SkillTree Points Requested"
        emailRes.recipients == [projectAdminUserAttrs.email]
        emailRes.plainText.contains("User ${userRequestingPtsAttrs.userIdForDisplay} requested points.")
        emailRes.html.contains("User <b>${userRequestingPtsAttrs.userIdForDisplay}</b> requested points")

        assert WaitFor.wait { notificationsRepo.count() == 0 }

        !res.body.skillApplied
        res.body.pointsEarned == 0
        res.body.explanation == "Skill was submitted for approval"
        !res.body.completed
        res.body.skillId == skills[0].skillId

        List<SkillApproval> approvals = skillApprovalRepo.findAll().collect {it}
        approvals.size() == 1

        approvals.get(0).requestMsg == "Please approve this!"
        !approvals.get(0).rejectedOn
        !approvals.get(0).rejectionMsg

        approvals.get(0).projectId == proj.projectId
        approvals.get(0).skillRefId == skillDefRepo.findAll().find({it.skillId == skills[0].skillId}).id
        approvals.get(0).userId == "user0"
        approvals.get(0).requestedOn == date

        approvalsEndpointRes.data.size() == 1
        approvalsEndpointRes.data.get(0).userId == "user0"
        approvalsEndpointRes.data.get(0).skillId == "skill1"
        approvalsEndpointRes.data.get(0).skillName == "Test Skill 1"
        approvalsEndpointRes.data.get(0).requestedOn == date.time
        approvalsEndpointRes.data.get(0).requestMsg == "Please approve this!"
    }

    def "self report skill with approval but no message"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        Date date = new Date() - 60
        when:
        def res = skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], "user0", date)
        def approvalsEndpointRes = skillsService.getApprovals(proj.projectId, 5, 1, 'requestedOn', false)

        assert WaitFor.wait { greenMail.getReceivedMessages().size() > 0 }
        EmailUtils.EmailRes emailRes = EmailUtils.getEmail(greenMail)

        UserAttrs projectAdminUserAttrs = userAttrsRepo.findByUserId(skillsService.userName)
        UserAttrs userRequestingPtsAttrs = userAttrsRepo.findByUserId("user0")

        then:
        greenMail.getReceivedMessages().length == 1
        emailRes.subj == "SkillTree Points Requested"
        emailRes.recipients == [projectAdminUserAttrs.email]
        emailRes.plainText.contains("User ${userRequestingPtsAttrs.userIdForDisplay} requested points.")
        emailRes.html.contains("User <b>${userRequestingPtsAttrs.userIdForDisplay}</b> requested points")

        !res.body.skillApplied
        res.body.pointsEarned == 0
        res.body.explanation == "Skill was submitted for approval"
        !res.body.completed
        res.body.skillId == skills[0].skillId

        List<SkillApproval> approvals = skillApprovalRepo.findAll().collect {it}
        approvals.size() == 1

        !approvals.get(0).requestMsg
        !approvals.get(0).rejectedOn
        !approvals.get(0).rejectionMsg

        approvals.get(0).projectId == proj.projectId
        approvals.get(0).skillRefId == skillDefRepo.findAll().find({it.skillId == skills[0].skillId}).id
        approvals.get(0).userId == "user0"
        approvals.get(0).requestedOn == date

        approvalsEndpointRes.data.size() == 1
        approvalsEndpointRes.data.get(0).userId == "user0"
        approvalsEndpointRes.data.get(0).skillId == "skill1"
        approvalsEndpointRes.data.get(0).skillName == "Test Skill 1"
        approvalsEndpointRes.data.get(0).requestedOn == date.time
        !approvalsEndpointRes.data.get(0).requestMsg
    }

    def "send email notification to each admin of the project"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        String otherUser = getRandomUsers(1).first()

        createService(otherUser)
        skillsService.addProjectAdmin(proj.projectId, otherUser)

        UserAttrs projectAdminUserAttrs = userAttrsRepo.findByUserId(skillsService.userName)
        UserAttrs otherProjectAdminUserAttrs = userAttrsRepo.findByUserId(otherUser)

        Date date = new Date() - 60
        when:
        def res = skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], "user0", date, "Please approve this!")

        assert WaitFor.wait { greenMail.getReceivedMessages().size() > 1 }
        List<EmailUtils.EmailRes> emails = EmailUtils.getEmails(greenMail)

        then:
        emails.size() == 2
        emails.collect {it.recipients[0] }.sort() == [projectAdminUserAttrs.email, otherProjectAdminUserAttrs.email].sort()

        !res.body.skillApplied
        res.body.explanation == "Skill was submitted for approval"
    }

    def "self report email notification format"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 2000
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        Date date = new Date() - 60
        when:
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], "user0", date, "Please approve this!")

        UserAttrs projectAdminUserAttrs = userAttrsRepo.findByUserId(skillsService.userName)
        UserAttrs userRequestingPtsAttrs = userAttrsRepo.findByUserId("user0")


        assert WaitFor.wait { greenMail.getReceivedMessages().size() > 0 }
        EmailUtils.EmailRes emailRes = EmailUtils.getEmail(greenMail)
        String expectedHtml = '''<!--
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
    <style>
        .button {
            border: 1px solid green;
            border-radius: 2px;
            padding: 15px 32px;
            text-align: center;
            text-decoration: none;
            display: inline-block;
            font-size: 16px;
        }

        .button:hover {
            cursor: pointer;
            background-color: #ecffec;
        }

        .label {
            color: #525252;
        }
    </style>
</head>
<body class="overall-container">
<h1>SkillTree Points Requested!</h1>
<p>User <b>''' + userRequestingPtsAttrs.userIdForDisplay + '''</b> requested points. As an approver for the <b>Test Project#1</b> project, you can approve or reject this request.</p>

<p style="font-weight: bold">
    <a href="http://localhost:{{port}}/administrator/projects/TestProject1/self-report" class="button">Approve or Reject</a>
</p>

<ul>
    <li><span class="label">Project</span>: Test Project#1</li>
    <li><span class="label">Skill</span>: Test Skill 1</li>
    <li><span class="label">Points</span>: 2,000</li>
    <li><span class="label">Message</span>: Please approve this!</li>
</ul>

<p>
Always yours, <br/> -SkillTree Bot
</p>

</body>
</html>'''

        String expectedPlain = '''User ''' + userRequestingPtsAttrs.userIdForDisplay + ''' requested points.
   Approval URL: http://localhost:{{port}}/administrator/projects/TestProject1/self-report
   User Requested: ''' + userRequestingPtsAttrs.userIdForDisplay + '''
   Project: Test Project#1
   Skill: Test Skill 1 (skill1)
   Number of Points: 2,000
   Request Message: Please approve this!

As an approver for the 'TestProject1' project, you can approve or reject this request.


Always yours,
SkillTree Bot'''
        then:
        greenMail.getReceivedMessages().length == 1
        emailRes.subj == "SkillTree Points Requested"
        emailRes.recipients == [projectAdminUserAttrs.email]
        emailRes.plainText.contains("User ${userRequestingPtsAttrs.userIdForDisplay} requested points.")
        // ignore new lines
        EmailUtils.prepBodyForComparison(emailRes.html, localPort) == EmailUtils.prepBodyForComparison(expectedHtml, localPort)
        EmailUtils.prepBodyForComparison(emailRes.plainText, localPort) == EmailUtils.prepBodyForComparison(expectedPlain, localPort)
    }

    def "self report skill with honor system"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].selfReportingType = SkillDef.SelfReportingType.HonorSystem

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        Date date = new Date() - 60
        when:
        def res = skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], "user0", date, "Please approve this!")
        def approvalsEndpointRes = skillsService.getApprovals(proj.projectId, 5, 1, 'requestedOn', false)

        then:
        res.body.skillApplied
        res.body.pointsEarned == 200
        res.body.explanation == "Skill event was applied"

        !skillApprovalRepo.findAll().collect {it}
        !approvalsEndpointRes.data
        approvalsEndpointRes.count == 0
    }

    def "report via approval"() {
        String user = "skills@skills.org"

        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        Date date = new Date() - 60

        UserAttrs projectAdminUserAttrs = userAttrsRepo.findByUserId(skillsService.userName)
        UserAttrs userRequestingPtsAttrs = userAttrsRepo.findByUserId(user)

        when:
        def res = skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], user, date, "Please approve this!")
        def approvalsEndpointRes = skillsService.getApprovals(proj.projectId, 5, 1, 'requestedOn', false)
        List<Integer> ids = approvalsEndpointRes.data.collect { it.id }
        skillsService.approve(proj.projectId, ids)

        def approvalsEndpointResAfter = skillsService.getApprovals(proj.projectId, 5, 1, 'requestedOn', false)
        def skillEvents = skillsService.getPerformedSkills(user, proj.projectId)

        assert WaitFor.wait { greenMail.getReceivedMessages().size() == 2 }
        int approvalEmailIdx = greenMail.getReceivedMessages().findIndexOf {it.subject.contains('Approved') }
        EmailUtils.EmailRes emailRes = EmailUtils.getEmail(greenMail, approvalEmailIdx)

        String expectedPlainText = '''
Congratulations! Your request for the Test Skill 1 skill in the Test Project#1 project has been approved.
   Project: Test Project#1
   Skill: Test Skill 1
   Approver: ''' + projectAdminUserAttrs.userIdForDisplay + '''
   

Always yours,
SkillTree Bot
'''
        String expectedHtml = '''
<!--
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
    <style>
        .label {
            color: #525252;
        }
    </style>
</head>
<body class="overall-container">
<h1>SkillTree Points <span>Approved!</span></h1>
<p>Congratulations! Your request for the <b>Test Skill 1</b> skill in the <b>Test Project#1</b> project has been approved!</p>


<ul>
    <li><span class="label">Project</span>: Test Project#1</li>
    <li><span class="label">Skill</span>: Test Skill 1</li>
    <li><span class="label">Approver</span>: '''+ projectAdminUserAttrs.userIdForDisplay + '''</li>
    
</ul>

<p>You can view your progress for the <a href="http://localhost:{{port}}/progress-and-rankings/projects/TestProject1">Test Project#1</a> project in the SkillTree dashboard.</p>

<p>
Always yours, <br/> -SkillTree Bot
</p>

</body>
</html>
'''
        then:
        emailRes.subj == "SkillTree Points Approved"
        emailRes.recipients == [userRequestingPtsAttrs.email]

        // ignore new lines
        EmailUtils.prepBodyForComparison(emailRes.html, localPort) == EmailUtils.prepBodyForComparison(expectedHtml, localPort)
        EmailUtils.prepBodyForComparison(emailRes.plainText, localPort) == EmailUtils.prepBodyForComparison(expectedPlainText, localPort)

        !res.body.skillApplied
        approvalsEndpointRes.count == 1
        approvalsEndpointRes.data.size() == 1
        skillEvents.data.size() == 1
        skillEvents.data.get(0).skillId == skills[0].skillId
        approvalsEndpointResAfter.data.size() == 0
        approvalsEndpointResAfter.count == 0
    }

    def "requesting approval for the same skill more than one time"() {
        String user = "user0"
        String user1 = "user1"

        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        Date date = new Date() - 60
        when:
        def approvalsEndpointRes0 = skillsService.getApprovals(proj.projectId, 5, 1, 'requestedOn', false)
        def res = skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], user, date, "Please approve this!")
        def approvalsEndpointRes1 = skillsService.getApprovals(proj.projectId, 5, 1, 'requestedOn', false)
        def res1 = skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], user, date, "Please approve this!")
        def approvalsEndpointRes2 = skillsService.getApprovals(proj.projectId, 5, 1, 'requestedOn', false)
        def res2 = skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], user1, date, "Please approve this!")
        def approvalsEndpointRes3 = skillsService.getApprovals(proj.projectId, 5, 1, 'requestedOn', false)

        then:
        approvalsEndpointRes0.count == 0
        !approvalsEndpointRes0.data

        !res.body.skillApplied
        res.body.pointsEarned == 0
        res.body.explanation == "Skill was submitted for approval"

        approvalsEndpointRes1.count == 1
        approvalsEndpointRes1.data.size() == 1
        approvalsEndpointRes1.data.get(0).userId == user

        !res1.body.skillApplied
        res1.body.pointsEarned == 0
        res1.body.explanation == "This skill was already submitted for approval and is still pending approval"

        approvalsEndpointRes2.count == 1
        approvalsEndpointRes2.data.size() == 1
        approvalsEndpointRes2.data.get(0).userId == user

        !res2.body.skillApplied
        res2.body.pointsEarned == 0
        res2.body.explanation == "Skill was submitted for approval"

        approvalsEndpointRes3.count == 2
        approvalsEndpointRes3.data.size() == 2
        approvalsEndpointRes3.data.get(0).userId == user1
        approvalsEndpointRes3.data.get(1).userId == user
    }

    def "ability to submit again for a rejected approval"() {
        String user = "skills@skills.org"

        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        Date date = new Date() - 60
        Date date1 = new Date() - 30
        when:
        def res = skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], user, date, "Please approve this!")
        def approvalsEndpointRes = skillsService.getApprovals(proj.projectId, 5, 1, 'requestedOn', false)

        List<Integer> ids = approvalsEndpointRes.data.collect { it.id }
        skillsService.rejectSkillApprovals(proj.projectId, ids, 'Just felt like it')

        def res1 = skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], user, date1, "Please approve this again!")
        def approvalsEndpointResAfter = skillsService.getApprovals(proj.projectId, 5, 1, 'requestedOn', false)

        assert WaitFor.wait { greenMail.getReceivedMessages().size() == 3 }
        int deniedEmailIdx = greenMail.getReceivedMessages().findIndexOf {it.subject.contains('Denied') }
        EmailUtils.EmailRes emailRes = EmailUtils.getEmail(greenMail, deniedEmailIdx)

        UserAttrs projectAdminUserAttrs = userAttrsRepo.findByUserId(skillsService.userName)
        UserAttrs userRequestingPtsAttrs = userAttrsRepo.findByUserId(user)

        String expectedPlainText = '''
Your request for the Test Skill 1 skill in the Test Project#1 project has been denied.
   Project: Test Project#1
   Skill: Test Skill 1
   Approver: '''+projectAdminUserAttrs.userIdForDisplay+'''
   Message: Just felt like it


Always yours,
SkillTree Bot
'''
        String expectedHtml = '''
<!--
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
    <style>
        .label {
            color: #525252;
        }
    </style>
</head>
<body class="overall-container">
<h1>SkillTree Points <span>Denied</span></h1>

<p>Your request for the <b>Test Skill 1</b> skill in the <b>Test Project#1</b> project has been denied.</p>

<ul>
    <li><span class="label">Project</span>: Test Project#1</li>
    <li><span class="label">Skill</span>: Test Skill 1</li>
    <li><span class="label">Approver</span>: '''+projectAdminUserAttrs.userIdForDisplay+'''</li>
    <li><span class="label">Message</span>: Just felt like it</li>
</ul>

<p>You can view your progress for the <a href="http://localhost:{{port}}/progress-and-rankings/projects/TestProject1">Test Project#1</a> project in the SkillTree dashboard.</p>

<p>
Always yours, <br/> -SkillTree Bot
</p>

</body>
</html>
'''
        then:
        emailRes.subj == "SkillTree Points Denied"
        emailRes.recipients == [userRequestingPtsAttrs.email]

        // ignore new lines
        EmailUtils.prepBodyForComparison(emailRes.html, localPort) == EmailUtils.prepBodyForComparison(expectedHtml, localPort)
        EmailUtils.prepBodyForComparison(emailRes.plainText, localPort) == EmailUtils.prepBodyForComparison(expectedPlainText, localPort)

        !res.body.skillApplied
        res.body.pointsEarned == 0
        res.body.explanation == "Skill was submitted for approval"

        !res1.body.skillApplied
        res1.body.pointsEarned == 0
        res1.body.explanation == "Skill was submitted for approval"

        approvalsEndpointRes.count == 1
        approvalsEndpointRes.data.size() == 1
        approvalsEndpointRes.data[0].requestMsg == "Please approve this!"

        approvalsEndpointResAfter.data.size() == 1
        approvalsEndpointResAfter.data.get(0).requestMsg == "Please approve this again!"
        approvalsEndpointResAfter.data.get(0).userId == user
        approvalsEndpointResAfter.data.get(0).skillId == skills[0].skillId
        approvalsEndpointResAfter.data.get(0).skillName == skills[0].name
        approvalsEndpointResAfter.data.get(0).requestedOn == date1.time
    }

    def "report via approval should validate approval message if 'paragraphValidationRegex' property is configurede"() {
        String user = "user0"

        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        Date date = new Date() - 60
        when:
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], user, date, "Please approve jabberwocky this!")

        then:
        SkillsClientException e = thrown()
        e.httpStatus == HttpStatus.BAD_REQUEST
        e.message.contains("Custom validation failed: msg=[paragraphs may not contain jabberwocky], type=[selfReportApprovalMsg], requestMsg=[Please approve jabberwocky this!], userId=user0")
    }

    def "reject self reported skill without message"() {
        String user = "skills@skills.org"

        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        Date date = new Date() - 60
        Date date1 = new Date() - 30
        when:
        def res = skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], user, date, "Please approve this!")
        def approvalsEndpointRes = skillsService.getApprovals(proj.projectId, 5, 1, 'requestedOn', false)

        List<Integer> ids = approvalsEndpointRes.data.collect { it.id }
        skillsService.rejectSkillApprovals(proj.projectId, ids)

        assert WaitFor.wait { greenMail.getReceivedMessages().size() == 2 }
        int deniedEmailIdx = greenMail.getReceivedMessages().findIndexOf {it.subject.contains('Denied') }
        EmailUtils.EmailRes emailRes = EmailUtils.getEmail(greenMail, deniedEmailIdx)

        UserAttrs projectAdminUserAttrs = userAttrsRepo.findByUserId(skillsService.userName)
        UserAttrs userRequestingPtsAttrs = userAttrsRepo.findByUserId(user)

        String expectedPlainText = '''
Your request for the Test Skill 1 skill in the Test Project#1 project has been denied.
   Project: Test Project#1
   Skill: Test Skill 1
   Approver: '''+projectAdminUserAttrs.userIdForDisplay+'''
   Message: 


Always yours,
SkillTree Bot
'''
        String expectedHtml = '''
<!--
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
    <style>
        .label {
            color: #525252;
        }
    </style>
</head>
<body class="overall-container">
<h1>SkillTree Points <span>Denied</span></h1>

<p>Your request for the <b>Test Skill 1</b> skill in the <b>Test Project#1</b> project has been denied.</p>

<ul>
    <li><span class="label">Project</span>: Test Project#1</li>
    <li><span class="label">Skill</span>: Test Skill 1</li>
    <li><span class="label">Approver</span>: '''+projectAdminUserAttrs.userIdForDisplay+'''</li>
    <li><span class="label">Message</span>: </li>
</ul>

<p>You can view your progress for the <a href="http://localhost:{{port}}/progress-and-rankings/projects/TestProject1">Test Project#1</a> project in the SkillTree dashboard.</p>

<p>
Always yours, <br/> -SkillTree Bot
</p>

</body>
</html>
'''
        then:
        emailRes.subj == "SkillTree Points Denied"
        emailRes.recipients == [userRequestingPtsAttrs.email]

        // ignore new lines
        EmailUtils.prepBodyForComparison(emailRes.html, localPort) == EmailUtils.prepBodyForComparison(expectedHtml, localPort)
        EmailUtils.prepBodyForComparison(emailRes.plainText, localPort) == EmailUtils.prepBodyForComparison(expectedPlainText, localPort)

        !res.body.skillApplied
        res.body.pointsEarned == 0
        res.body.explanation == "Skill was submitted for approval"

        approvalsEndpointRes.count == 1
        approvalsEndpointRes.data.size() == 1
        approvalsEndpointRes.data[0].requestMsg == "Please approve this!"
    }

    def "self report approval skill with insufficient project points"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        Date date = new Date()
        String userId = "user0"
        when:
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userId, date, "Please approve this!")

        then:
        SkillsClientException skillsClientException = thrown(SkillsClientException)
        skillsClientException.message.contains("Insufficient project points, skill achievement is disallowed, errorCode:InsufficientProjectPoints, success:false, projectId:${subj.projectId}, skillId:null")
        skillsClientException.message.contains(userId)
    }

    def "self report honor skill with insufficient project points"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].selfReportingType = SkillDef.SelfReportingType.HonorSystem

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        Date date = new Date()
        String userId = "user0"
        when:
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userId, date, "Please approve this!")

        then:
        SkillsClientException skillsClientException = thrown(SkillsClientException)
        skillsClientException.message.contains("Insufficient project points, skill achievement is disallowed, errorCode:InsufficientProjectPoints, success:false, projectId:${subj.projectId}, skillId:null")
        skillsClientException.message.contains(userId)
    }

    def "self report approval skill with insufficient subject points"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        skillsService.createSubject(SkillsFactory.createSubject(1, 2))
        skillsService.createSkills(SkillsFactory.createSkills(1, 1, 2, 200))

        Date date = new Date()
        String userId = "user0"
        when:
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userId, date, "Please approve this!")

        then:
        SkillsClientException skillsClientException = thrown(SkillsClientException)
        skillsClientException.message.contains("Insufficient Subject points, skill achievement is disallowed, errorCode:InsufficientSubjectPoints, success:false, projectId:${subj.projectId}, skillId:null")
        skillsClientException.message.contains(userId)
    }

    def "self report honor skill with insufficient subject points"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].selfReportingType = SkillDef.SelfReportingType.HonorSystem

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        skillsService.createSubject(SkillsFactory.createSubject(1, 2))
        skillsService.createSkills(SkillsFactory.createSkills(1, 1, 2, 200))

        Date date = new Date()
        String userId = "user0"
        when:
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userId, date, "Please approve this!")

        then:
        SkillsClientException skillsClientException = thrown(SkillsClientException)
        skillsClientException.message.contains("Insufficient Subject points, skill achievement is disallowed, errorCode:InsufficientSubjectPoints, success:false, projectId:${subj.projectId}, skillId:null")
        skillsClientException.message.contains(userId)
    }
}
