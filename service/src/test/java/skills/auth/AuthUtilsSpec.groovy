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
package skills.auth

import spock.lang.Specification

import java.nio.charset.StandardCharsets

class AuthUtilsSpec extends Specification {

    def "pick first project is if it appears twice in the url"() {

        when:
        String res = AuthUtils.getProjectIdFromPath('/admin/projects/test/skills/other1/shared/projects/testProj')
        then:
        res == "test"
    }

    def "parse project name from url"() {

        expect:
        AuthUtils.getProjectIdFromPath(url) == projectId

        where:
        projectId | url
        "test"    | "/admin/projects/test/shared"
        "test"    | "/admin/projects/test/dependency/graph"
        "test"    | "/admin/myprojects/test/name"
    }

    def "match self reporting approve or reject url only"() {

        GroovyMock(AuthUtils, global: true)
        AuthUtils.getRequestAttributes() >> new AuthUtils.RequestAttributes(requestPath: url)

        expect:
        AuthUtils.getRequestAttributes().isSelfReportApproveOrRejectEndpoint() == matched

        where:
        matched | url
        true    | "/admin/projects/proj1/approvals/approve"
        true    | "/admin/projects/proj2/approvals/approve"
        false   | "/admin/projects/proj2/approvals/approv"
        false   | "/admin/projects/proj2/approvals/approve1"
        false   | "/admin/bdea/projects/proj2/approvals/approve"
        false   | "/admin/projects/pr/oj2/approvals/approve"
        false   | "admin/projects/proj2/approvals/approve"
        false   | "/admin/projects/proj2/approbals/approve"
        false   | "/abmin/projects/proj2/approvals/approve"
        false   | "/admin/projects/proj2/approvals/applove"
        true    | "/admin/projects/${URLEncoder.encode("some fancy 23 id", StandardCharsets.UTF_8.toString())}/approvals/approve"

        true    | "/admin/projects/proj1/approvals/reject"
        true    | "/admin/projects/proj2/approvals/reject"
        false   | "/admin/projects/proj2/approvals/rejec"
        false   | "/admin/projects/proj2/approvals/reject1"
        false   | "/admin/bdea/projects/proj2/approvals/rejecte"
        false   | "/admin/projects/pr/oj2/approvals/reject"
        false   | "admin/projects/proj2/approvals/reject"
        false   | "/admin/projects/proj2/approbals/reject"
        false   | "/abmin/projects/proj2/approvals/reject"
        false   | "/admin/projects/proj2/approvals/rejept"
        true    | "/admin/projects/${URLEncoder.encode("some fancy 23 id", StandardCharsets.UTF_8.toString())}/approvals/reject"
    }

    def "only match approver conf endpoint"() {
        GroovyMock(AuthUtils, global: true)
        AuthUtils.getRequestAttributes() >> new AuthUtils.RequestAttributes(requestPath: url)

        expect:
        AuthUtils.getRequestAttributes().isSelfReportApproverConfEndpoint() == matched

        where:
        matched | url
        true    | "/admin/projects/proj1/approverConf"
        true    | "/admin/projects/pROJ2/approverConf"
        false   | "/admin/projects/proj1/approverConf1"
        false   | "/admin/projects/proj1/approverConf/"
        false   | "/admin/projects/proj1/approvercConf"
        false   | "/b/admin/projects/proj1/approverConf"
        false   | "/admin/co/projects/proj1/approverConf"
        false   | "/admin/projects//proj1/approverConf"
    }

    def "only match email sub/unsub conf endpoint"() {
        GroovyMock(AuthUtils, global: true)
        AuthUtils.getRequestAttributes() >> new AuthUtils.RequestAttributes(requestPath: url)

        expect:
        AuthUtils.getRequestAttributes().isSelfReportEmailSubscriptionEndpoint() == matched

        where:
        matched | url
        true    | "/admin/projects/proj1/approvalEmails/unsubscribe"
        true    | "/admin/projects/pROJ2/approvalEmails/unsubscribe"
        false   | "/admin/projects/proj1/approvalEmails/unsubscribe1"
        false   | "/admin/projects/proj1/approvalEmails/unsubscribe/"
        false   | "/admin/projects/proj1/approvalEmails/unsubsdcribe"
        false   | "/b/admin/projects/proj1/approvalEmails/unsubscribe"
        false   | "/admin/co/projects/proj1/approvalEmails/unsubscribe"
        false   | "/admin/projects//proj1/approvalEmails/unsubscribe"

        true    | "/admin/projects/proj1/approvalEmails/subscribe"
        true    | "/admin/projects/pROJ2/approvalEmails/subscribe"
        false   | "/admin/projects/proj1/approvalEmails/subscribe1"
        false   | "/admin/projects/proj1/approvalEmails/subscribe/"
        false   | "/admin/projects/proj1/approvalEmails/subscdribe"
        false   | "/b/admin/projects/proj1/approvalEmails/subscribe"
        false   | "/admin/co/projects/proj1/approvalEmails/subscribe"
        false   | "/admin/projects//proj1/approvalEmails/subscribe"
    }

    def "only match dashboard actions for project-based endpoints"() {
        GroovyMock(AuthUtils, global: true)
        AuthUtils.getRequestAttributes() >> new AuthUtils.RequestAttributes(requestPath: url)

        expect:
        AuthUtils.getRequestAttributes().isDashboardActionsEndpoint() == matched

        where:
        matched | url
        true    | "/admin/projects/proj1/dashboardActions"
        true    | "/admin/projects/pROJ2/dashboardActions/filterOptions"
        true    | "/admin/projects/pROJ2/dashboardActions/1/attributes"
        false   | "/admin/projects/proj1/approvalEmails/unsubscribe1"
        false   | "/admin/projects/proj1/approvalEmails/unsubscribe/"
        false   | "/admin/projects/proj1/approvalEmails/unsubsdcribe"
        false   | "/b/admin/projects/proj1/approvalEmails/unsubscribe"
        false   | "/admin/co/projects/proj1/approvalEmails/unsubscribe"
        false   | "/admin/projects//proj1/approvalEmails/unsubscribe"
    }
}

