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
package skills.intTests

import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import spock.lang.IgnoreIf
import spock.lang.Specification

class AuthorizationSpecs extends DefaultIntSpec {

    String projId = "myProject"
    String projId2 = "myProject2"
    def project
    def project2

    def setup() {
        skillsService.deleteProjectIfExist(projId)
        skillsService.deleteProjectIfExist(projId2)
        project = skillsService.createProject([projectId: projId, name: "My Project"]).body
        assert project.success == true
        project = skillsService.getProject(projId)
        project2 = skillsService.createProject([projectId: projId2, name: "My Project2"]).body
        assert project2.success == true
        project2 = skillsService.getProject(projId2)
    }

    def 'user cannot get project they are not an admin for'() {
        when:

        SkillsService skillsServiceUser2 = createService("newUser")
        def result = skillsServiceUser2.getProject(projId)

        then:
        !result
        SkillsClientException ex = thrown()
        ex.httpStatus == HttpStatus.FORBIDDEN
    }

    def 'user cannot create subject for project they are not an admin for'() {
        when:

        SkillsService skillsServiceUser2 = createService("newUser")
        skillsServiceUser2.createSubject([projectId: projId, subjectId: "subject", name: "Test Subject 1"])

        then:
        SkillsClientException ex = thrown()
        ex.httpStatus == HttpStatus.FORBIDDEN
    }


    def 'user cannot create badge for project they are not an admin for'() {
        when:

        SkillsService skillsServiceUser2 = createService("newUser")
        Map badge = [projectId: projId, badgeId: 'badge1', name: 'Test Badge 1']
        skillsServiceUser2.createBadge(badge)

        then:
        SkillsClientException ex = thrown()
        ex.httpStatus == HttpStatus.FORBIDDEN
    }

    def 'admin can add another user as an admin for their project'() {
        when:

        String newUser = "user2"
        SkillsService skillsServiceUser2 = createService(newUser)
        Exception expected
        try {
            // verify this 'user2' cannot see the project
            skillsServiceUser2.getProject(projId)
        } catch (Exception e) {
            expected = e
        }
        assert expected

        // add 'user2' as an admin to projId
        skillsService.addProjectAdmin(projId, newUser)

        // now verify that 'user2' can see the project
        def result = skillsServiceUser2.getProject(projId)

        then:
        true
        result
        result.projectId == projId
    }

    def 'current user cannot delete them self'() {

        when:
        skillsService.deleteUserRole("skills@skills.org", projId, "ROLE_PROJECT_ADMIN")

        then:
        SkillsClientException ex = thrown()
        ex.httpStatus == HttpStatus.BAD_REQUEST
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def 'use proxy to add skill for multiple users and then get summaries and verify admin user is still logged in via http session'() {
        when:

        List<String> sampleUserIds = ['jim@email.com', 'bob@email.com']
        Map subj1 = [projectId: projId, subjectId: "subj1", skillId: "skill11".toString(), name: "Test Subject 1".toString(), type: "Skill", pointIncrement: 100, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map subj2 = [projectId: projId, subjectId: "subj2", skillId: "skill21".toString(), name: "Test Subject 2".toString(), type: "Skill", pointIncrement: 200, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        skillsService.createSubject(subj1)
        skillsService.createSubject(subj2)

        skillsService.createSkill(subj1)
        skillsService.createSkill(subj2)

        String secret = skillsService.getClientSecret(projId)
        skillsService.setProxyCredentials(projId, secret)

        // subj1 - 2 users
        skillsService.addSkillAsProxy([projectId: projId, skillId: subj1.skillId], sampleUserIds.get(0))
        skillsService.addSkillAsProxy([projectId: projId, skillId: subj1.skillId], sampleUserIds.get(1))
        skillsService.addSkillAsProxy([projectId: projId, skillId: subj1.skillId], sampleUserIds.get(1))

        // subj2 - 1 users
        skillsService.addSkillAsProxy([projectId: projId, skillId: subj2.skillId], sampleUserIds.get(1))
        skillsService.addSkillAsProxy([projectId: projId, skillId: subj2.skillId], sampleUserIds.get(1))

        // load summary for user1 and user2
        def summary1 = skillsService.getSkillSummaryAsProxy(sampleUserIds.get(0), projId)
        def summary2 = skillsService.getSkillSummaryAsProxy(sampleUserIds.get(1), projId)

        // now make sure the original admin user is still logged in via the http session and can hit an admin endpoint
        def adminResult = skillsService.getProject(projId)

        then:
        adminResult
        summary1
        summary1.subjects.find {it.subject == subj1.name && it.skillsLevel == 5 && it.totalPoints == 100 && it.todaysPoints == 100}
        summary1.subjects.find {it.subject == subj2.name && it.skillsLevel == 0 && it.totalPoints == 200 && it.todaysPoints == 0}
        summary2
        summary2.subjects.find {it.subject ==  subj1.name && it.skillsLevel == 5 && it.totalPoints == 100 && it.todaysPoints == 100}
        summary2.subjects.find {it.subject == subj2.name && it.skillsLevel == 5 && it.totalPoints == 200 && it.todaysPoints == 200}
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "reset client secret"() {
        when:
        List<String> sampleUserIds = ['jim@email.com', 'bob@email.com']
        Map subj1 = [projectId: projId, subjectId: "subj1", skillId: "skill11".toString(), name: "Test Subject 1".toString(), type: "Skill", pointIncrement: 100, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map subj2 = [projectId: projId, subjectId: "subj2", skillId: "skill21".toString(), name: "Test Subject 2".toString(), type: "Skill", pointIncrement: 200, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        skillsService.createSubject(subj1)
        skillsService.createSubject(subj2)

        skillsService.createSkill(subj1)
        skillsService.createSkill(subj2)

        String secretOld = skillsService.getClientSecret(projId)
        skillsService.resetClientSecret(projId)
        String secret = skillsService.getClientSecret(projId)
        skillsService.setProxyCredentials(projId, secret)

        // subj1 - 2 users
        skillsService.addSkillAsProxy([projectId: projId, skillId: subj1.skillId], sampleUserIds.get(0))
        skillsService.addSkillAsProxy([projectId: projId, skillId: subj1.skillId], sampleUserIds.get(1))
        skillsService.addSkillAsProxy([projectId: projId, skillId: subj1.skillId], sampleUserIds.get(1))

        // subj2 - 1 users
        skillsService.addSkillAsProxy([projectId: projId, skillId: subj2.skillId], sampleUserIds.get(1))
        skillsService.addSkillAsProxy([projectId: projId, skillId: subj2.skillId], sampleUserIds.get(1))

        // load summary for user1 and user2
        def summary1 = skillsService.getSkillSummaryAsProxy(sampleUserIds.get(0), projId)
        def summary2 = skillsService.getSkillSummaryAsProxy(sampleUserIds.get(1), projId)

        // now make sure the original admin user is still logged in via the http session and can hit an admin endpoint
        def adminResult = skillsService.getProject(projId)

        then:
        secretOld != secret
        adminResult
        summary1
        summary1.subjects.find {it.subject == subj1.name && it.skillsLevel == 5 && it.totalPoints == 100 && it.todaysPoints == 100}
        summary1.subjects.find {it.subject == subj2.name && it.skillsLevel == 0 && it.totalPoints == 200 && it.todaysPoints == 0}
        summary2
        summary2.subjects.find {it.subject ==  subj1.name && it.skillsLevel == 5 && it.totalPoints == 100 && it.todaysPoints == 100}
        summary2.subjects.find {it.subject == subj2.name && it.skillsLevel == 5 && it.totalPoints == 200 && it.todaysPoints == 200}
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "a proxied user from project A cannot use their token for a request to project B"() {
        when:
        String secret = skillsService.getClientSecret(projId)
        skillsService.setProxyCredentials(projId, secret)
        skillsService.getSkillSummaryAsProxy('bob', projId2)

        then:
        SkillsClientException ex = thrown()
        ex.httpStatus == HttpStatus.FORBIDDEN
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "requesting a token without a proxy_user attribute will return an error"() {
        when:
        String secret = skillsService.getClientSecret(projId)
        skillsService.setProxyCredentials(projId, secret)
        Map subj1 = [projectId: projId, subjectId: "subj1", skillId: "skill11".toString(), name: "Test Subject 1".toString(), type: "Skill", pointIncrement: 100, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        skillsService.createSubject(subj1)
        skillsService.createSkill(subj1)
        skillsService.addSkillAsProxy([projectId: projId, skillId: subj1.skillId], 'bob', true, false)

        then:
        SkillsClientException ex = thrown()
        ex.getMessage().contains('error_description:Invalid access token')
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "requesting a token without a grant_type attribute will return an error"() {
        when:
        String secret = skillsService.getClientSecret(projId)
        skillsService.setProxyCredentials(projId, secret)
        Map subj1 = [projectId: projId, subjectId: "subj1", skillId: "skill11".toString(), name: "Test Subject 1".toString(), type: "Skill", pointIncrement: 100, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        skillsService.createSubject(subj1)
        skillsService.createSkill(subj1)
        skillsService.addSkillAsProxy([projectId: projId, skillId: subj1.skillId], 'bob', false, true)

        then:
        SkillsClientException ex = thrown()
        ex.getMessage().contains('error_description:Invalid access token')
    }

    def 'admin - user cannot get another user\'s project level if they are not an admin for said project'() {
        when:

        SkillsService skillsServiceUser2 = createService("newUser")
        skillsServiceUser2.adminGetUserLevelForProject(projId, 'aUser')

        then:
        SkillsClientException ex = thrown()
        ex.httpStatus == HttpStatus.FORBIDDEN
    }

    def 'api - user cannot get another user\'s project level if they are not an admin for said project'() {
        when:

        SkillsService skillsServiceUser2 = createService("newUser")
        skillsServiceUser2.apiGetUserLevelForProject(projId, 'aUser')

        then:
        SkillsClientException ex = thrown()
        ex.httpStatus == HttpStatus.FORBIDDEN
    }

    def 'must be an project admins in order to get its client secret'(){
        SkillsService skillsServiceUser2 = createService("newUser")
        when:
        skillsServiceUser2.getClientSecret(projId)
        then:
        SkillsClientException e = thrown()
        e.httpStatus == HttpStatus.FORBIDDEN
    }

    def "admin can complete a skill for themself"() {
        Map subj1 = [projectId: projId, subjectId: "subj1", skillId: "skill11".toString(), name: "Test Subject 1".toString(), type: "Skill", pointIncrement: 100, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        skillsService.createSubject(subj1)
        skillsService.createSkill(subj1)

        when:
        def res = skillsService.addSkill([projectId: projId, skillId: subj1.skillId])

        then:
        res.body.skillApplied
        res.body.explanation == "Skill event was applied"
    }

    def "admin can complete a skill for another user"() {
        Map subj1 = [projectId: projId, subjectId: "subj1", skillId: "skill11".toString(), name: "Test Subject 1".toString(), type: "Skill", pointIncrement: 100, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        skillsService.createSubject(subj1)
        skillsService.createSkill(subj1)

        when:
        def res = skillsService.addSkill([projectId: projId, skillId: subj1.skillId], 'jim@email.com', new Date()-1)

        then:
        res.body.skillApplied
        res.body.explanation == "Skill event was applied"
    }

    def "non admin can complete a skill for themself"() {
        Map subj1 = [projectId: projId, subjectId: "subj1", skillId: "skill11".toString(), name: "Test Subject 1".toString(), type: "Skill", pointIncrement: 100, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        skillsService.createSubject(subj1)
        skillsService.createSkill(subj1)

        SkillsService skillsServiceUser2 = createService("newUser")

        when:
        def res = skillsServiceUser2.addSkill([projectId: projId, skillId: subj1.skillId])

        then:
        res.body.skillApplied
        res.body.explanation == "Skill event was applied"
    }

    def "non admin CANNOT complete a skill for another user"() {
        Map subj1 = [projectId: projId, subjectId: "subj1", skillId: "skill11".toString(), name: "Test Subject 1".toString(), type: "Skill", pointIncrement: 100, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        skillsService.createSubject(subj1)
        skillsService.createSkill(subj1)

        SkillsService skillsServiceUser2 = createService("newUser")
        when:
        def res = skillsServiceUser2.addSkill([projectId: projId, skillId: subj1.skillId], 'jim@email.com', new Date()-1)

        then:
        SkillsClientException ex = thrown()
        ex.httpStatus == HttpStatus.FORBIDDEN
    }
}
