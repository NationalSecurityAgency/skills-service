/**
 * Copyright 2026 SkillTree
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
package skills.intTests.metrics.multipleProj

import groovy.json.JsonSlurper
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.EmailUtils
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.utils.WaitFor

class FindExpertsForMultipleProjectsMetricsBuilderSpec extends DefaultIntSpec {

    private static final String METRICS_ID = 'findExpertsForMultipleProjectsChartBuilder'

    def "expert search enforces #protection access with restricted project #position"() {
        given:
        List<String> users = getRandomUsers(3)
        SkillsService owner = createService(users[0])
        SkillsService caller = createService(users[1])
        SkillsService learner = createService(users[2])
        SkillsService root = createRootSkillService()
        if (protection == 'invite') {
            startEmailServer()
        } else {
            root.saveUserTag(owner.userName, 'dragons', ['DivineDragon'])
        }

        List<Map> projects = (1..3).collect { Integer projectNumber ->
            Map project = SkillsFactory.createProject(projectNumber)
            Map subject = SkillsFactory.createSubject(projectNumber, 1)
            Map skill = SkillsFactory.createSkill(projectNumber, 1, 1) + [pointIncrement: 200]
            owner.createProjectAndSubjectAndSkills(project, subject, [skill])
            learner.addSkill(skill)
            assert learner.apiGetUserLevelForProject(project.projectId, null) == 5
            return project
        }
        Map restrictedProject = projects[2]
        List<Map> orderedProjects = position == 'first' ? [projects[2], projects[0], projects[1]] : projects
        Map props = [projIdsAndLevel: orderedProjects.collect { Map project -> "${project.projectId}AndLevel1" }.join(','),
                     currentPage: 1, pageSize: 10, sortDesc: false]
        String learnerDisplayId = learner.getCurrentUser().userIdForDisplay

        expect: 'a user without achievements can search public projects for another user'
        assertSearchResult(caller, props, orderedProjects, learnerDisplayId)

        when:
        if (protection == 'invite') {
            owner.changeSetting(restrictedProject.projectId, 'invite_only',
                    [projectId: restrictedProject.projectId, setting: 'invite_only', value: 'true'])
        } else {
            owner.updateProject(restrictedProject + [enableProtectedUserCommunity: true])
        }

        then: 'both matching rows and their count are inaccessible'
        assertSearchDenied(caller, props)
        assertSearchDenied(caller, props + [currentPage: 2])
        assertSearchDenied(caller, props + [projIdsAndLevel: orderedProjects.collect { Map project -> "${project.projectId}AndLevel6" }.join(',')])
        assertSearchResult(owner, props, orderedProjects, learnerDisplayId)
        if (protection == 'invite') {
            assertSearchResult(root, props, orderedProjects, learnerDisplayId)
        } else {
            assertSearchDenied(root, props)
        }

        when:
        if (protection == 'invite') {
            owner.inviteUsersToProject(restrictedProject.projectId,
                    [validityDuration: 'PT5M', recipients: [EmailUtils.generateEmaillAddressFor(caller.userName)]])
            WaitFor.wait { greenMail.getReceivedMessages().length > 0 }
            String emailBody = EmailUtils.getEmail(greenMail, 0).html
            String invite = (emailBody =~ /join-project\/([^\/]+)\/([^?]+)/)[0][2]
            caller.joinProject(restrictedProject.projectId, invite)
        } else {
            root.saveUserTag(caller.userName, 'dragons', ['DivineDragon'])
        }

        then: 'authorized users can still search the project'
        assertSearchResult(caller, props, orderedProjects, learnerDisplayId)

        when:
        if (protection == 'invite') {
            owner.revokeInviteOnlyProjectAccess(restrictedProject.projectId, caller.userName)
        } else {
            userTagRepo.deleteAll(userTagRepo.findAllByUserIdAndKey(caller.userName, 'dragons'))
        }

        then: 'access is rechecked after it is revoked'
        assertSearchDenied(caller, props)

        where:
        protection  | position
        'invite'    | 'first'
        'invite'    | 'last'
        'community' | 'first'
        'community' | 'last'
    }

    private void assertSearchResult(SkillsService service, Map props, List<Map> projects, String learnerDisplayId) {
        Map result = service.getApiGlobalMetricsData(METRICS_ID, props)
        assert result.totalNum == 1
        assert result.data.size() == 1
        assert result.data[0].userId == learnerDisplayId
        assert result.data[0].levels == projects.collect { Map project -> [projectId: project.projectId, level: 5] }
    }

    private void assertSearchDenied(SkillsService service, Map props) {
        ResponseEntity<String> response = service.wsHelper.rawGet("api/metrics/${METRICS_ID}", props)
        assert response.statusCode == HttpStatus.FORBIDDEN
        Map body = new JsonSlurper().parseText(response.body) as Map
        assert body.errorCode == 'AccessDenied'
        assert !body.containsKey('data')
        assert !body.containsKey('totalNum')
    }
}
