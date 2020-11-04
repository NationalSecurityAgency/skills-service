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

import org.apache.http.client.methods.HttpHead
import org.joda.time.DateTime
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import skills.controller.result.model.TableResult
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.intTests.utils.WSHelper

class AdminEditSpecs extends DefaultIntSpec {

    def "Edit subjectId"(){
        Map proj = SkillsFactory.createProject()
        Map subject = SkillsFactory.createSubject()
        skillsService.createProject(proj)
        skillsService.createSubject(subject)

        when:

        def res = skillsService.getSubject(subject)
        def originalSubjectId = res.subjectId
        res.subjectId = "TestSubject47"
        skillsService.updateSubject(res, originalSubjectId)

        def updatedResult = skillsService.getSubject(res)

        Exception shouldFail = null
        def shouldBeNull = null

        try {
            shouldBeNull = skillsService.getSubject([projectId: res.projectId, subjectId: originalSubjectId])
        }catch(Exception e){
            shouldFail = e
        }

        then:
        assert updatedResult.subjectId == "TestSubject47"
        !shouldBeNull
        shouldFail

    }

    def "only project admin should bea ble to edit subject id"(){
        Map proj = SkillsFactory.createProject()
        Map subject = SkillsFactory.createSubject()
        skillsService.createProject(proj)
        skillsService.createSubject(subject)

        when:

        def res = skillsService.getSubject(subject)
        res.subjectId = "TestSubject47"
        SkillsService skillsService1 = createService("someOtherUser")
        skillsService1.createSubject(res)
        then:
        SkillsClientException e = thrown()
        e.httpStatus == HttpStatus.FORBIDDEN
    }

    def "Edit badgeId"(){
        Map proj = SkillsFactory.createProject()
        Map badge = SkillsFactory.createBadge()
        skillsService.createProject(proj)
        skillsService.createBadge(badge)

        when:

        def res = skillsService.getBadge(badge)
        def originalBadgeId = res.badgeId
        res.badgeId = "TestBadge47"
        res.enabled = 'false'
        skillsService.createBadge(res, originalBadgeId)

        def updatedResult = skillsService.getBadge(res)

        Exception shouldFail = null
        def shouldBeNull = null

        try {
            shouldBeNull = skillsService.getBadge([projectId: res.projectId, badgeId: originalBadgeId])
        }catch(Exception e){
            shouldFail = e
        }

        then:
        assert updatedResult.badgeId == "TestBadge47"
        !shouldBeNull
        shouldFail
    }

    def "only project admin should bea ble to edit badge id"(){
        Map proj = SkillsFactory.createProject()
        Map badge = SkillsFactory.createBadge()
        skillsService.createProject(proj)
        skillsService.createBadge(badge)

        when:
        def res = skillsService.getBadge(badge)
        res.badgeId = "TestBadge47"
        SkillsService skillsService1 = createService("someOtherUser")
        skillsService1.createBadge(res)
        then:
        SkillsClientException e = thrown()
        e.httpStatus == HttpStatus.FORBIDDEN
    }

    def "Edit projectId"(){
        Map proj = SkillsFactory.createProject()
        skillsService.createProject(proj)

        when:

        def res = skillsService.getProject(proj.projectId)
        def originalProjectId = res.projectId
        res.projectId = "TestProject47"
        res.name = "New Name 47"
        skillsService.updateProject(res, originalProjectId)

        def updatedResult = skillsService.getProject(res.projectId)

        Exception shouldFail = null
        def shouldBeNull = null

        try {
            shouldBeNull = skillsService.getProject(originalProjectId)
        }catch(Exception e){
            shouldFail = e
        }

        then:
        assert updatedResult.projectId == "TestProject47"
        !shouldBeNull
        shouldFail
    }

    def "Create project with invalid json"(){
        when:

        WSHelper wsHelper = skillsService.wsHelper

        HttpHeaders headers = new HttpHeaders()
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        HttpEntity<String> jsonRequest = new HttpEntity<>('{"originalProjectId":"","name":"Sna"""ffulus","projectId":"aProject"}', headers)

        ResponseEntity result = wsHelper.restTemplateWrapper.postForEntity("${wsHelper.skillsService}/app/projects/aProject", jsonRequest, String.class)

        then:
        result.statusCodeValue == 400
        result.body
        result.body.contains('"explanation":"JSON parse error: ')
    }

    def "Must NOT be able to edit projects from /app/projects/{id} endpoint"(){
        Map proj = SkillsFactory.createProject()
        skillsService.createProject(proj)

        when:
        def res = skillsService.getProject(proj.projectId)
        res.projectId = "TestProject47"

        skillsService.createProject(res, proj.projectId)

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.BAD_REQUEST
        e.message.contains("Cannot edit project id using /app/projects/{id} endpoint. Please use /admin/projects/{id}")
    }

    def "Must NOT be able to edit projects from /app/projects/{id} endpoint - other user"(){
        Map proj = SkillsFactory.createProject()
        skillsService.createProject(proj)

        when:
        def res = skillsService.getProject(proj.projectId)
        res.projectId = "TestProject47"

        SkillsService skillsService1 = createService("someOtherUser")
        skillsService1.createProject(res, proj.projectId)

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.BAD_REQUEST
        e.message.contains("Cannot edit project id using /app/projects/{id} endpoint. Please use /admin/projects/{id}")
    }

    def "Must not be able to rename projectId if that projectId already exist" () {
        Map proj = SkillsFactory.createProject(1)
        skillsService.createProject(proj)

        Map proj2 = SkillsFactory.createProject(2)
        skillsService.createProject(proj2)

        def res = skillsService.getProject(proj.projectId)
        res.projectId = proj2.projectId

        when:
        skillsService.updateProject(res, proj.projectId)

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.BAD_REQUEST
        e.message.contains("Project with id [TestProject2] already exists! Sorry!")
    }

    def "Must not be able to rename project's name if that name already exist" () {
        Map proj = SkillsFactory.createProject(1)
        skillsService.createProject(proj)

        Map proj2 = SkillsFactory.createProject(2)
        skillsService.createProject(proj2)

        when:
        skillsService.updateProject(proj2, proj.projectId)

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.BAD_REQUEST
        e.message.contains("Project with id [TestProject2] already exists! Sorry!")
    }

    def "Edit skillId"(){
        Map proj = SkillsFactory.createProject()
        Map subject = SkillsFactory.createSubject()
        Map skill = SkillsFactory.createSkill()
        skillsService.createProject(proj)
        skillsService.createSubject(subject)
        skillsService.createSkill(skill)

        when:

        def res = skillsService.getSkill(skill)
        def originalSkillId = res.skillId
        res.skillId = "TestSkill47"
        res.subjectId = subject.subjectId
        skillsService.updateSkill(res, originalSkillId)

        def updatedResult = skillsService.getSkill(res)

        Exception shouldFail = null
        def shouldBeNull = null

        try {
            shouldBeNull = skillsService.getSkill([projectId: res.projectId, subjectId: subject.subjectId, skillId: originalSkillId])
        }catch(Exception e){
            shouldFail = e
        }

        then:
        assert updatedResult.skillId == "TestSkill47"
        !shouldBeNull
        shouldFail
    }

    def "only project admin should bea ble to edit skill id"(){
        Map proj = SkillsFactory.createProject()
        Map subject = SkillsFactory.createSubject()
        Map skill = SkillsFactory.createSkill()
        skillsService.createProject(proj)
        skillsService.createSubject(subject)
        skillsService.createSkill(skill)

        when:

        def res = skillsService.getSkill(skill)
        res.skillId = "TestSkill47"
        res.subjectId = subject.subjectId
        SkillsService skillsService1 = createService("someOtherUser")
        skillsService1.createSkill(res)
        then:
        SkillsClientException e = thrown()
        e.httpStatus == HttpStatus.FORBIDDEN
    }

    def "#293 Searching for skills the user has performed is case insensitive"() {
        String userId = 'user1'

        def project = SkillsFactory.createProject(1)
        def subject = SkillsFactory.createSubject(1, 1)
        List<Map> subjectSkills = SkillsFactory.createSkills(5, 1, 1)
        def subject2 = SkillsFactory.createSubject(1, 2)
        List<Map> subjectSkills2 = SkillsFactory.createSkills(5, 1, 2)
        subjectSkills.each{
            it.pointIncrement = 20
        }
        subjectSkills2.each{
            it.pointIncrement = 20
        }

        def expectedSkill1 = subjectSkills.get(0)
        def expectedSkill2 = subjectSkills2.get(1)

        String expectedMatchHit = "matchThisString"

        expectedSkill1.skillId = "AAAAAAAAAA${expectedMatchHit}ZZZZZZZZZZZ".toString()
        expectedSkill2.skillId = "asdfasdf${expectedMatchHit}AAasdfasdfaf".toString()

        skillsService.createProject(project)
        skillsService.createSubject(subject)
        skillsService.createSubject(subject2)
        skillsService.createSkills(subjectSkills)
        skillsService.createSkills(subjectSkills2)

        skillsService.addSkill([projectId: project.projectId, skillId: subjectSkills.get(0).skillId], userId, new Date())
        skillsService.addSkill([projectId: project.projectId, skillId: subjectSkills2.get(1).skillId], userId, new Date())
        skillsService.addSkill([projectId: project.projectId, skillId: subjectSkills2.get(2).skillId], userId, new Date()) // Will not match filter

        when:
        TableResult expected = new TableResult(
            count: 2,
            totalCount: 3,
            data: [subjectSkills.get(0), subjectSkills2.get(1)]
        )
        String query = expectedMatchHit.collect { Character.isLowerCase((char) it) ? it.toUpperCase() : it.toLowerCase() }.join('')
        TableResult result = skillsService.getPerformedSkills(userId, project.projectId, query)

        then:
        result.count == expected.count
        result.totalCount == expected.totalCount
        result.data.size() == 2
        result.data.any { it.skillId == expectedSkill1.skillId }
        result.data.any { it.skillId == expectedSkill2.skillId }
    }

    def "When skill id is updated it should persist to 'performed skills'"(){
        String userId = "user1"

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(5, 1, 1)
        proj1_skills.each {
            it.pointIncrement = 20
        }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId, new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId], userId, new Date())

        when:
        def skills = skillsService.getPerformedSkills(userId, proj1.projectId)

        def res = skillsService.getSkill(proj1_skills.get(0))
        String originalSkillId = res.skillId
        res.skillId = "TestSkill47"
        res.subjectId = proj1_subj.subjectId
        skillsService.updateSkill(res, originalSkillId)

        def skillsAfter = skillsService.getPerformedSkills(userId, proj1.projectId)
        then:
        skills.data.size() == 2
        skills.data.collect { it.skillId }.sort() == ["skill1", "skill2"]
        skillsAfter.data.size() == 2
        skillsAfter.data.collect { it.skillId }.sort() == ["TestSkill47", "skill2"]
    }

    def "Project creation limited per user"(){
        when:
        (1..26).each {
            def proj = SkillsFactory.createProject(it)
            skillsService.createProject(proj)
        }
        then:
        SkillsClientException e = thrown()
        e.message.contains "Each user is limited to [25] Projects"
    }

    def "Subject creation limited per project"() {
        def proj1 = SkillsFactory.createProject(1)
        skillsService.createProject(proj1)

        when:

        (1..26).each {
            def subj = SkillsFactory.createSubject(1, it)
            skillsService.createSubject(subj)
        }

        then:
        SkillsClientException e = thrown()
        e.message.contains "Each Project is limited to [25] Subjects"
    }

    def "Badge creation limited per project"() {
        def proj1 = SkillsFactory.createProject(1)
        skillsService.createProject(proj1)

        when:

        (1..26).each {
            def subj = SkillsFactory.createBadge(1, it)
            skillsService.createBadge(subj)
        }

        then:
        SkillsClientException e = thrown()
        e.message.contains "Each Project is limited to [25] Badges"
    }

    def "Skill creation limited per subject"() {
        def proj1 = SkillsFactory.createProject(1)
        skillsService.createProject(proj1)
        def subj = SkillsFactory.createSubject(1, 1)
        skillsService.createSubject(subj)

        when:

        (1..101).each {
            def skill = SkillsFactory.createSkill(1, 1, it)
            skillsService.createSkill(skill)
        }

        then:
        SkillsClientException e = thrown()
        e.message.contains "Each Subject is limited to [100] Skills"
    }

    def "Prevent injection in Subject description"(){
        def proj1 = SkillsFactory.createProject(1)
        skillsService.createProject(proj1)

        when:
        def subj = SkillsFactory.createSubject(1, 1)
        subj.subjectId = 'mySubj'
        subj.description = "this is a description <a href='http://somewhere' onclick='doNefariousStuff()'>I'm a link</a>"
        skillsService.createSubject(subj)

        def subject = skillsService.getSubject([subjectId: 'mySubj', projectId: proj1.projectId])
        then:
        subject.description == 'this is a description <a href="http://somewhere" rel="nofollow">I\'m a link</a>'
    }

    def "Prevent injection in Skill description"(){
        def proj1 = SkillsFactory.createProject(1)
        skillsService.createProject(proj1)
        def subj = SkillsFactory.createSubject(1, 1)
        skillsService.createSubject(subj)

        when:
        def sk = SkillsFactory.createSkill()
        sk.skillId = 'mySkill'
        sk.description = "this is a description <a href='http://somewhere' onclick='doNefariousStuff()'>I'm a link</a>"
        skillsService.createSkill(sk)

        def skill = skillsService.getSkill([skillId: 'mySkill', subjectId: subj.subjectId, projectId: proj1.projectId])
        then:
        skill.description == 'this is a description <a href="http://somewhere" rel="nofollow">I\'m a link</a>'
    }

    def "Prevent injection in Badge description"(){
        def proj1 = SkillsFactory.createProject(1)
        skillsService.createProject(proj1)

        when:
        def badge = SkillsFactory.createBadge(1, 1)
        badge.badgeId = 'myBadge'
        badge.description = "this is a description <a href='http://somewhere' onclick='doNefariousStuff()'>I'm a link</a>"
        skillsService.createBadge(badge)

        badge = skillsService.getBadge([badgeId: 'myBadge', projectId: proj1.projectId])
        then:
        badge.description == 'this is a description <a href="http://somewhere" rel="nofollow">I\'m a link</a>'
    }

    def "increasing skill point increment post achievement causes user points to be updated"() {
        def proj1 = SkillsFactory.createProject(1)
        skillsService.createProject(proj1)
        def subj = SkillsFactory.createSubject(1)
        skillsService.createSubject(subj)

        List<Map> skills = SkillsFactory.createSkills(3)
        skills[2].pointIncrement = 80

        skillsService.createSkills(skills)


        skillsService.addSkill([projectId: proj1.projectId, skillId: skills.get(0).skillId], "u123", new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: skills.get(1).skillId], "u123", new Date())

        when:
        def skillSummaryBeforeEdit = skillsService.getSkillSummary("u123", proj1.projectId, subj.subjectId)
        def projectSummaryBeforeEdit = skillsService.getSkillSummary("u123", proj1.projectId)

        skillsService.updateSkill([projectId: proj1.projectId,
                                  subjectId: subj.subjectId,
                                  skillId: skills.get(0).skillId,
                                  numPerformToCompletion: skills.get(0).numPerformToCompletion,
                                  pointIncrement: 100,
                                  pointIncrementInterval: skills.get(0).pointIncrementInterval,
                                  numMaxOccurrencesIncrementInterval: skills.get(0).numMaxOccurrencesIncrementInterval,
                                  version: skills.get(0).version,
                                  name: skills.get(0).name], skills.get(0).skillId)


        def skillSummaryAfterEdit = skillsService.getSkillSummary("u123", proj1.projectId, subj.subjectId)
        def projectSummaryAfterEdit = skillsService.getSkillSummary("u123", proj1.projectId)

        then:
        skillSummaryBeforeEdit.skills[0].points == 10
        skillSummaryBeforeEdit.skills[0].totalPoints == 10
        skillSummaryBeforeEdit.skillsLevel == 1
        skillSummaryBeforeEdit.points == 20
        skillSummaryBeforeEdit.totalPoints == 100
        skillSummaryBeforeEdit.todaysPoints == 20
        projectSummaryBeforeEdit.points == 20
        projectSummaryBeforeEdit.totalPoints == 100
        projectSummaryBeforeEdit.todaysPoints == 20
        projectSummaryBeforeEdit.subjects[0].points == 20
        projectSummaryBeforeEdit.subjects[0].totalPoints == 100
        projectSummaryBeforeEdit.subjects[0].todaysPoints == 20

        skillSummaryAfterEdit.skills[0].points == 100
        skillSummaryAfterEdit.skills[0].totalPoints == 100
        skillSummaryAfterEdit.skillsLevel == 3
        skillSummaryAfterEdit.points == 110
        skillSummaryAfterEdit.totalPoints == 190
        skillSummaryAfterEdit.todaysPoints == 110
        projectSummaryAfterEdit.points == 110
        projectSummaryAfterEdit.totalPoints == 190
        projectSummaryAfterEdit.todaysPoints == 110
        projectSummaryAfterEdit.subjects[0].points == 110
        projectSummaryAfterEdit.subjects[0].totalPoints == 190
        projectSummaryAfterEdit.subjects[0].todaysPoints == 110
    }

    def "decreasing skill point increment post achievement causes user points to be updated"() {
        def proj1 = SkillsFactory.createProject(1)
        skillsService.createProject(proj1)
        def subj = SkillsFactory.createSubject(1)
        skillsService.createSubject(subj)

        List<Map> skills = SkillsFactory.createSkills(3)
        skills[2].pointIncrement = 80
        skillsService.createSkills(skills)


        skillsService.addSkill([projectId: proj1.projectId, skillId: skills.get(0).skillId], "u123", new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: skills.get(1).skillId], "u123", new Date())

        when:
        def skillSummaryBeforeEdit = skillsService.getSkillSummary("u123", proj1.projectId, subj.subjectId)

        skillsService.updateSkill([projectId: proj1.projectId,
                                   subjectId: subj.subjectId,
                                   skillId: skills.get(0).skillId,
                                   numPerformToCompletion: skills.get(0).numPerformToCompletion,
                                   pointIncrement: 5,
                                   pointIncrementInterval: skills.get(0).pointIncrementInterval,
                                   numMaxOccurrencesIncrementInterval: skills.get(0).numMaxOccurrencesIncrementInterval,
                                   version: skills.get(0).version,
                                   name: skills.get(0).name], skills.get(0).skillId)


        def skillSummaryAfterEdit = skillsService.getSkillSummary("u123", proj1.projectId, subj.subjectId)

        then:
        skillSummaryBeforeEdit.skills[0].points == 10
        skillSummaryBeforeEdit.skills[0].totalPoints == 10
        skillSummaryBeforeEdit.skillsLevel == 1
        skillSummaryBeforeEdit.points == 20
        skillSummaryBeforeEdit.totalPoints == 100

        skillSummaryAfterEdit.skills[0].points == 5
        skillSummaryAfterEdit.skills[0].totalPoints == 5
        skillSummaryAfterEdit.skillsLevel == 1
        skillSummaryAfterEdit.points == 15
        skillSummaryAfterEdit.totalPoints == 95
    }

    def "decrease skill point increment after multiple users have performed occurrences"(){
        def proj1 = SkillsFactory.createProject(1)
        skillsService.createProject(proj1)
        def subj = SkillsFactory.createSubject(1)
        skillsService.createSubject(subj)

        List<Map> skills = SkillsFactory.createSkills(3)
        skills[2].pointIncrement = 80
        skillsService.createSkills(skills)


        skillsService.addSkill([projectId: proj1.projectId, skillId: skills.get(0).skillId], "u123", new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: skills.get(1).skillId], "u123", new Date())

        skillsService.addSkill([projectId: proj1.projectId, skillId: skills.get(0).skillId], "u124", new Date())

        //this user should be unaffected
        skillsService.addSkill([projectId: proj1.projectId, skillId: skills.get(2).skillId], "u125", new Date())

        when:
        def u123SummaryBeforeEdit = skillsService.getSkillSummary("u123", proj1.projectId, subj.subjectId)
        def u124SummaryBeforeEdit = skillsService.getSkillSummary("u124", proj1.projectId, subj.subjectId)
        def u125SummaryBeforeEdit = skillsService.getSkillSummary("u125", proj1.projectId, subj.subjectId)

        skillsService.updateSkill([projectId: proj1.projectId,
                                   subjectId: subj.subjectId,
                                   skillId: skills.get(0).skillId,
                                   numPerformToCompletion: skills.get(0).numPerformToCompletion,
                                   pointIncrement: 5,
                                   pointIncrementInterval: skills.get(0).pointIncrementInterval,
                                   numMaxOccurrencesIncrementInterval: skills.get(0).numMaxOccurrencesIncrementInterval,
                                   version: skills.get(0).version,
                                   name: skills.get(0).name], skills.get(0).skillId)


        def u123SummaryAfterEdit = skillsService.getSkillSummary("u123", proj1.projectId, subj.subjectId)
        def u124SummaryAfterEdit = skillsService.getSkillSummary("u124", proj1.projectId, subj.subjectId)
        def u125SummaryAfterEdit = skillsService.getSkillSummary("u125", proj1.projectId, subj.subjectId)

        then:
        u123SummaryBeforeEdit.skills[0].points == 10
        u123SummaryBeforeEdit.skills[0].totalPoints == 10
        u123SummaryBeforeEdit.skills[1].points == 10
        u123SummaryBeforeEdit.skills[1].totalPoints == 10
        u123SummaryBeforeEdit.points == 20
        u123SummaryBeforeEdit.totalPoints == 100
        u123SummaryBeforeEdit.skillsLevel == 1

        u123SummaryAfterEdit.skills[0].points == 5
        u123SummaryAfterEdit.skills[0].totalPoints == 5
        u123SummaryAfterEdit.skills[1].points == 10
        u123SummaryAfterEdit.skills[1].totalPoints == 10
        u123SummaryAfterEdit.points == 15
        u123SummaryAfterEdit.totalPoints == 95
        u123SummaryAfterEdit.skillsLevel == 1


        u124SummaryBeforeEdit.skills[0].points == 10
        u124SummaryBeforeEdit.skills[0].totalPoints == 10
        u124SummaryBeforeEdit.skillsLevel == 1
        u124SummaryBeforeEdit.points == 10
        u124SummaryBeforeEdit.totalPoints == 100

        u124SummaryAfterEdit.skills[0].points == 5
        u124SummaryAfterEdit.skills[0].totalPoints == 5
        u124SummaryAfterEdit.skillsLevel == 1
        u124SummaryAfterEdit.points == 5
        u124SummaryAfterEdit.totalPoints == 95

        u125SummaryBeforeEdit.skills[0].points == 0
        u125SummaryBeforeEdit.skills[0].totalPoints == 10
        u125SummaryBeforeEdit.skills[1].points == 0
        u125SummaryBeforeEdit.skills[1].totalPoints == 10
        u125SummaryBeforeEdit.skills[2].points == 80
        u125SummaryBeforeEdit.skills[2].totalPoints == 80
        u125SummaryBeforeEdit.totalPoints == 100

        u125SummaryAfterEdit.skills[0].points == 0
        u125SummaryAfterEdit.skills[0].totalPoints == 5
        u125SummaryAfterEdit.skills[1].points == 0
        u125SummaryAfterEdit.skills[1].totalPoints == 10
        u125SummaryAfterEdit.skills[2].points == 80
        u125SummaryAfterEdit.skills[2].totalPoints == 80
        u125SummaryAfterEdit.skillsLevel == u125SummaryBeforeEdit.skillsLevel
        u125SummaryAfterEdit.points == u125SummaryBeforeEdit.points
        u125SummaryAfterEdit.totalPoints == 95
    }

    def "changes to skill points causes users's point history to be updated"(){
        def proj1 = SkillsFactory.createProject(1)
        skillsService.createProject(proj1)
        def subj = SkillsFactory.createSubject(1)
        skillsService.createSubject(subj)

        List<Map> skills = SkillsFactory.createSkills(3)
        def skill1 = SkillsFactory.createSkill(1, 1, 1, 0, 3, 0)
        def skill2 = SkillsFactory.createSkill(1, 1, 2)
        def skill3 = SkillsFactory.createSkill(1, 1, 3)

        skill3.pointIncrement = 60

        skillsService.createSkills([skill1, skill2, skill3])


        //outstanding questions: How does it work if multiple skills achieved on the same day? Do we get multiple
        //rows or just one?
        skillsService.addSkill([projectId: proj1.projectId, skillId: skill1.skillId], "u123", new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: skill1.skillId], "u123", new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: skill1.skillId], "u123", new DateTime().minusDays(1).toDate())

        when:

        def skillSummaryBeforeEdit = skillsService.getSkillSummary("u123", proj1.projectId, subj.subjectId)
        def pointHistoryBeforeEdit = skillsService.getPointHistory("u123", proj1.projectId, subj.subjectId)

        skillsService.updateSkill([projectId: proj1.projectId,
                                   subjectId: subj.subjectId,
                                   skillId: skill1.skillId,
                                   numPerformToCompletion: skill1.numPerformToCompletion,
                                   pointIncrement: 5,
                                   pointIncrementInterval: skill1.pointIncrementInterval,
                                   numMaxOccurrencesIncrementInterval: skill1.numMaxOccurrencesIncrementInterval,
                                   version: skill1.version,
                                   name: skill1.name], skill1.skillId)


        def pointHistoryAfterEdit = skillsService.getPointHistory("u123", proj1.projectId, subj.subjectId)
        def skillSummaryAfterEdit = skillsService.getSkillSummary("u123", proj1.projectId, subj.subjectId)

        then:
        skillSummaryBeforeEdit.points == 30
        skillSummaryBeforeEdit.totalPoints == 100
        skillSummaryBeforeEdit.todaysPoints == 20
        pointHistoryBeforeEdit.pointsHistory[0].points == 10
        pointHistoryBeforeEdit.pointsHistory[1].points == 30
        skillSummaryAfterEdit.totalPoints == 85
        skillSummaryAfterEdit.points == 15
        skillSummaryAfterEdit.todaysPoints == 10
        pointHistoryAfterEdit.pointsHistory[0].points == 5
        pointHistoryAfterEdit.pointsHistory[1].points == 15
    }

    def "changes to skill points causes multiple users's point history to be updated"(){
        def proj1 = SkillsFactory.createProject(1)
        skillsService.createProject(proj1)
        def subj = SkillsFactory.createSubject(1)
        skillsService.createSubject(subj)

        def skill1 = SkillsFactory.createSkill(1, 1, 1, 0, 3, 0)
        def skill2 = SkillsFactory.createSkill(1, 1, 2)
        def skill3 = SkillsFactory.createSkill(1, 1, 3)
        skill3.pointIncrement = 60

        skillsService.createSkills([skill1, skill2, skill3])

        skillsService.addSkill([projectId: proj1.projectId, skillId: skill1.skillId], "u123", new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: skill1.skillId], "u123", new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: skill1.skillId], "u123", new DateTime().minusDays(1).toDate())

        skillsService.addSkill([projectId: proj1.projectId, skillId: skill1.skillId], "u124", new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: skill1.skillId], "u124", new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: skill1.skillId], "u124", new DateTime().minusDays(1).toDate())

        skillsService.addSkill([projectId: proj1.projectId, skillId: skill1.skillId], "u125", new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: skill2.skillId], "u125", new DateTime().minusDays(1).toDate())
        skillsService.addSkill([projectId: proj1.projectId, skillId: skill3.skillId], "u125", new DateTime().minusDays(2).toDate())

        when:

        def u123SkillSummaryBeforeEdit = skillsService.getSkillSummary("u123", proj1.projectId, subj.subjectId)
        def u123PointHistoryBeforeEdit = skillsService.getPointHistory("u123", proj1.projectId, subj.subjectId)
        def u124SkillSummaryBeforeEdit = skillsService.getSkillSummary("u124", proj1.projectId, subj.subjectId)
        def u124PointHistoryBeforeEdit = skillsService.getPointHistory("u124", proj1.projectId, subj.subjectId)
        def u125SkillSummaryBeforeEdit = skillsService.getSkillSummary("u125", proj1.projectId, subj.subjectId)
        def u125PointHistoryBeforeEdit = skillsService.getPointHistory("u125", proj1.projectId, subj.subjectId)

        skillsService.updateSkill([projectId: proj1.projectId,
                                   subjectId: subj.subjectId,
                                   skillId: skill1.skillId,
                                   numPerformToCompletion: skill1.numPerformToCompletion,
                                   pointIncrement: 5,
                                   pointIncrementInterval: skill1.pointIncrementInterval,
                                   numMaxOccurrencesIncrementInterval: skill1.numMaxOccurrencesIncrementInterval,
                                   version: skill1.version,
                                   name: skill1.name], skill1.skillId)


        def u123pointHistoryAfterEdit = skillsService.getPointHistory("u123", proj1.projectId, subj.subjectId)
        def u123SkillSummaryAfterEdit = skillsService.getSkillSummary("u123", proj1.projectId, subj.subjectId)
        def u124pointHistoryAfterEdit = skillsService.getPointHistory("u124", proj1.projectId, subj.subjectId)
        def u124SkillSummaryAfterEdit = skillsService.getSkillSummary("u124", proj1.projectId, subj.subjectId)
        def u125pointHistoryAfterEdit = skillsService.getPointHistory("u125", proj1.projectId, subj.subjectId)
        def u125SkillSummaryAfterEdit = skillsService.getSkillSummary("u125", proj1.projectId, subj.subjectId)

        then:
        u123SkillSummaryBeforeEdit.points == 30
        u123SkillSummaryBeforeEdit.totalPoints == 100
        u123SkillSummaryBeforeEdit.todaysPoints == 20
        u123PointHistoryBeforeEdit.pointsHistory[0].points == 10
        u123PointHistoryBeforeEdit.pointsHistory[1].points == 30
        u123SkillSummaryAfterEdit.totalPoints == 85
        u123SkillSummaryAfterEdit.points == 15
        u123SkillSummaryAfterEdit.todaysPoints == 10
        u123pointHistoryAfterEdit.pointsHistory[0].points == 5
        u123pointHistoryAfterEdit.pointsHistory[1].points == 15

        u124SkillSummaryBeforeEdit.points == 30
        u124SkillSummaryBeforeEdit.totalPoints == 100
        u124SkillSummaryBeforeEdit.todaysPoints == 20
        u124PointHistoryBeforeEdit.pointsHistory[0].points == 10
        u124PointHistoryBeforeEdit.pointsHistory[1].points == 30
        u124SkillSummaryAfterEdit.totalPoints == 85
        u124SkillSummaryAfterEdit.points == 15
        u124SkillSummaryAfterEdit.todaysPoints == 10
        u124pointHistoryAfterEdit.pointsHistory[0].points == 5
        u124pointHistoryAfterEdit.pointsHistory[1].points == 15

        u125SkillSummaryBeforeEdit.points == 80
        u125SkillSummaryBeforeEdit.totalPoints == 100
        u125SkillSummaryBeforeEdit.todaysPoints == 10
        u125PointHistoryBeforeEdit.pointsHistory[0].points == 60
        u125PointHistoryBeforeEdit.pointsHistory[1].points == 70
        u125PointHistoryBeforeEdit.pointsHistory[2].points == 80
        u125SkillSummaryAfterEdit.totalPoints == 85
        u125SkillSummaryAfterEdit.points == 75
        u125SkillSummaryAfterEdit.todaysPoints == 5
        u125pointHistoryAfterEdit.pointsHistory[0].points == 60
        u125pointHistoryAfterEdit.pointsHistory[1].points == 70
        u125pointHistoryAfterEdit.pointsHistory[2].points == 75
    }
}
