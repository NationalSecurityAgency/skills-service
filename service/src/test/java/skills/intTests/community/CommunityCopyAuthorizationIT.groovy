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
package skills.intTests.community

import groovy.json.JsonSlurper
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import skills.SpringBootApp
import skills.controller.request.model.CopyToAnotherProjectRequestType
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = SpringBootApp,
        properties = ['skills.config.ui.userCommunityParagraphValidationRegex='])
class CommunityCopyAuthorizationIT extends DefaultIntSpec {

    def "copy #method #suffix enforces community access without paragraph validation"() {
        List<String> users = getRandomUsers(2)
        SkillsService owner = createService(users[0])
        SkillsService caller = createService(users[1])
        SkillsService root = createRootSkillService()
        root.saveUserTag(owner.userName, 'dragons', ['DivineDragon'])
        root.saveUserTag(caller.userName, 'dragons', ['DivineDragon'])

        Map source = SkillsFactory.createProject(1)
        Map subject = SkillsFactory.createSubject(1, 1)
        Map skill = SkillsFactory.createSkill(1, 1, 1)
        Map destination = SkillsFactory.createProject(2) + [enableProtectedUserCommunity: true]
        owner.createProjectAndSubjectAndSkills(source, subject, [skill])
        owner.createProject(destination)
        owner.addProjectAdmin(source.projectId, caller.userName)
        owner.addProjectAdmin(destination.projectId, caller.userName)
        String path = "admin/projects/${source.projectId}/copy/projects/${destination.projectId}"
        Map body = [copyType: CopyToAnotherProjectRequestType.EntireSubject, fromSubjectId: subject.subjectId]

        expect:
        caller.getProject(destination.projectId).projectId == destination.projectId
        owner.getSubjects(destination.projectId) == []
        ResponseEntity<String> validation = caller.wsHelper.rawRequest(HttpMethod.POST, "${path}/validateCopy", body)
        validation.statusCode.value() == 200
        new JsonSlurper().parseText(validation.body).isAllowed

        when:
        userTagRepo.deleteAll(userTagRepo.findAllByUserIdAndKey(caller.userName, 'dragons'))

        then:
        caller.getProject(source.projectId).projectId == source.projectId
        caller.wsHelper.rawGet("admin/projects/${destination.projectId}", [:]).statusCode.value() == 403

        when:
        ResponseEntity<String> denied = caller.wsHelper.rawRequest(method, "${path}${suffix}", body)
        List<Map> destinationSubjects = owner.getSubjects(destination.projectId)

        then:
        verifyAll {
            denied.statusCode.value() == 403
            destinationSubjects == []
        }

        when:
        root.saveUserTag(caller.userName, 'dragons', ['DivineDragon'])
        ResponseEntity<String> restored = caller.wsHelper.rawRequest(method, "${path}${suffix}", body)

        then:
        restored.statusCode.value() == 200
        if (suffix) {
            assert new JsonSlurper().parseText(restored.body).isAllowed
            assert owner.getSubjects(destination.projectId) == []
        } else {
            List<Map> copiedSubjects = owner.getSubjects(destination.projectId)
            assert copiedSubjects.subjectId == [subject.subjectId]
            assert copiedSubjects.name == [subject.name]
            assert copiedSubjects.numSkills == [1]
            assert owner.getSkill(skill + [projectId: destination.projectId]).name == skill.name
        }

        where:
        method          | suffix
        HttpMethod.POST | '/validateCopy'
        HttpMethod.POST | ''
        HttpMethod.PUT  | ''
    }
}
