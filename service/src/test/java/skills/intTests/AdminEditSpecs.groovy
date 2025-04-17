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

import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.*
import skills.controller.result.model.TableResult
import skills.intTests.utils.*
import skills.storage.model.SkillDef
import skills.storage.repos.UserAchievedLevelRepo
import skills.storage.repos.UserPointsRepo

import static skills.intTests.utils.SkillsFactory.*

class AdminEditSpecs extends DefaultIntSpec {

    @Autowired
    UserPointsRepo userPointsRepo

    @Autowired
    UserAchievedLevelRepo userAchievedLevelRepo

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

    def "subject helpUrl should not contain entity encoded ampersands"() {
        def aurl = "http://fake.url?p1=v1&p2=v2&p3=v3"
        Map proj = SkillsFactory.createProject()
        Map subject = SkillsFactory.createSubject()
        subject.helpUrl = aurl
        skillsService.createProject(proj)
        skillsService.createSubject(subject)

        when:
        def subj = skillsService.getSubject(subject)

        then:
        subj.helpUrl == aurl
    }

    def "skill helpUrl should not contain entity encoded ampersands"() {
        def aurl = "http://fake.url?p1=v1&p2=v2&p3=v3"
        Map proj = SkillsFactory.createProject()
        Map subject = SkillsFactory.createSubject()
        Map skill = SkillsFactory.createSkill()
        skill.helpUrl = aurl
        skillsService.createProject(proj)
        skillsService.createSubject(subject)
        skillsService.createSkill(skill)

        when:
        def sk = skillsService.getSkill(skill)

        then:
        sk.helpUrl == aurl
    }

    def "badge helpUrl should not contain entity encoded ampersands"() {
        def aurl = "http://fake.url?p1=v1&p2=v2&p3=v3"
        Map proj = SkillsFactory.createProject()
        Map subj = SkillsFactory.createSubject()
        Map skill = SkillsFactory.createSkill()
        Map badge = SkillsFactory.createBadge()
        badge.helpUrl = aurl
        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skill)
        skillsService.createBadge(badge)

        when:
        def b = skillsService.getBadge(badge)

        then:
        b.helpUrl == aurl
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

    def "Edit project description"(){
        Map proj = SkillsFactory.createProject()
        proj.description = "first"
        skillsService.createProject(proj)

        when:
        def start = skillsService.getProjectDescription(proj.projectId)
        proj.description = "second"
        skillsService.updateProject(proj, proj.projectId)
        def updated = skillsService.getProjectDescription(proj.projectId)

        then:
        start.description == "first"
        updated.description == "second"
    }

    def "Edit project description with code block"() {
        String descWithCodeBlock = """(A)
```
<template>
</template>
```

"""
        Map proj = SkillsFactory.createProject()
        proj.description = "first"
        skillsService.createProject(proj)

        when:
        def start = skillsService.getProjectDescription(proj.projectId)
        proj.description = descWithCodeBlock
        skillsService.updateProject(proj, proj.projectId)
        def updated = skillsService.getProjectDescription(proj.projectId)

        then:
        start.description == "first"
        updated.description == descWithCodeBlock
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

    def "User Performed Skills should include skills imported from the catalog"() {
        String userId = 'user1'

        def project = SkillsFactory.createProject(1)
        def project2 = SkillsFactory.createProject(2)
        def p2subject1 = SkillsFactory.createSubject(2,1)
        def p2skill1 = SkillsFactory.createSkill(2, 1, 201, 0, 5, 0, 100)
        p2skill1.skillId = "p2skill1"

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
        def expectedSkill3 = subjectSkills2.get(2)
        def expectedImportedSkill = p2skill1

        skillsService.createProject(project)
        skillsService.createSubject(subject)
        skillsService.createSubject(subject2)
        skillsService.createSkills(subjectSkills)
        skillsService.createSkills(subjectSkills2)
        skillsService.createProject(project2)
        skillsService.createSubject(p2subject1)
        skillsService.createSkill(p2skill1)
        skillsService.exportSkillToCatalog(project2.projectId, p2skill1.skillId)
        skillsService.importSkillFromCatalog(project.projectId, subject.subjectId, project2.projectId, p2skill1.skillId)
        skillsService.finalizeSkillsImportFromCatalog(project.projectId, true)

        skillsService.addSkill([projectId: project.projectId, skillId: subjectSkills.get(0).skillId], userId, new Date())
        skillsService.addSkill([projectId: project.projectId, skillId: subjectSkills2.get(1).skillId], userId, new Date())
        skillsService.addSkill([projectId: project.projectId, skillId: subjectSkills2.get(2).skillId], userId, new Date())
        skillsService.addSkill([projectId: project2.projectId, skillId: p2skill1.skillId], userId, new Date())

        when:
        TableResult expected = new TableResult(
                count: 4,
                totalCount: 4,
                data: [subjectSkills.get(0), subjectSkills2.get(1)]
        )
        TableResult result = skillsService.getPerformedSkills(userId, project.projectId)
        println result
        result.data.each {
            println "${it.projectId}::${it.skillId}"
        }

        then:
        result.count == expected.count
        result.totalCount == expected.totalCount
        result.data.size() == 4
        result.data.any { it.skillId == expectedSkill1.skillId }
        result.data.any { it.skillId == expectedSkill2.skillId }
        result.data.any { it.skillId == expectedSkill3.skillId }
        result.data.any { it.skillId == expectedImportedSkill.skillId }
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

    static int expectedMaxProjects = 25

    def "Project creation limited per user"() {
        when:
        (1..(expectedMaxProjects + 1)).each {
            def proj = SkillsFactory.createProject(it)
            skillsService.createProject(proj)
        }
        then:
        SkillsClientException e = thrown()
        e.message.contains "Each user is limited to [25] Projects"
    }

    def "project id must not be null string"() {
        def proj1 = SkillsFactory.createProject(1)

        when:
        proj1.projectId = "null"
        skillsService.createProject(proj1)
        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("Project Id was not provided")
    }

    def "user with root role can have unlimited # of projects"() {
        SkillsService rootUser = createRootSkillService()
        when:
        (1..(expectedMaxProjects + 5)).each {
            def proj = SkillsFactory.createProject(it)
            rootUser.createProject(proj)
            // UI pins - so let's simulate
            rootUser.pinProject(proj.projectId)
        }
        def projects = rootUser.getProjects()
        then:
        projects.size() == expectedMaxProjects + 5
    }

    def "user with root role can have unlimited # of projects - copy project"() {
        SkillsService rootUser = createRootSkillService()
        String lastProjId
        (1..(expectedMaxProjects)).each {
            def proj = SkillsFactory.createProject(it)
            rootUser.createProject(proj)
            // UI pins - so let's simulate
            rootUser.pinProject(proj.projectId)
            lastProjId = proj.projectId
        }

        when:
        rootUser.copyProject(lastProjId, SkillsFactory.createProject(50))
        def projects = rootUser.getProjects()
        then:
        projects.size() == expectedMaxProjects + 1
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

    def "lastReportedSkill is not populated for get projects"() {
        def proj1 = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        def skills = SkillsFactory.createSkills(2, 1, 1, 100, 2)
        skillsService.createProjectAndSubjectAndSkills(proj1, subj, skills)

        skillsService.addSkill([projectId: proj1.projectId, skillId: skills[0].skillId])
        SkillsService rootUser = createRootSkillService()
        rootUser.pinProject(proj1.projectId)
        when:
        def projectsForRoot = rootUser.getProjects()
        def projectsRegUser = skillsService.getProjects()

        then:
        projectsRegUser.lastReportedSkill == [null]
        projectsForRoot.lastReportedSkill == [null]
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
        subject.description == 'this is a description <a href="http://somewhere">I\'m a link</a>'
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
        skill.description == 'this is a description <a href="http://somewhere">I\'m a link</a>'
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
        badge.description == 'this is a description <a href="http://somewhere">I\'m a link</a>'
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

    def "Prevent injection in Project description"(){
        when:
        def proj1 = SkillsFactory.createProject(1)
        proj1.description = "this is a description <a href='http://somewhere' onclick='doNefariousStuff()'>I'm a link</a>\n> this is a quote"
        skillsService.createProject(proj1)

        def projDescription = skillsService.getProjectDescription(proj1.projectId)
        then:
        projDescription.description == 'this is a description <a href="http://somewhere">I\'m a link</a>\n> this is a quote'
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

    def "Editing skill should not change number of skills returned for a subject"(){
        Map proj = SkillsFactory.createProject()
        Map subject = SkillsFactory.createSubject()
        Map skill = SkillsFactory.createSkill()
        skillsService.createProject(proj)
        skillsService.createSubject(subject)
        skillsService.createSkill(skill)

        when:
        def subjectRes1 = skillsService.getSubject(subject)

        skill.name = "anotherName"
        skillsService.updateSkill(skill, null)
        def subjectRes2 = skillsService.getSubject(subject)

        then:
        subjectRes1.numSkills == 1
        subjectRes2.numSkills == 1
    }

    def "increasing skill points causes user(s) to achieve a level"() {
        def project1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1_skills = (1..3).collect { createSkill(1, 1, it, 0, 5, 0, 10) }
        skillsService.createProjectAndSubjectAndSkills(project1, p1subj1, p1_skills)

        String user = getRandomUsers(1)[0]
        skillsService.addSkill(p1_skills[0], user)

        when:
        def user1_pts1 = skillsService.getUserStats(project1.projectId, user).userTotalPoints
        Integer user1_leve_t0 = skillsService.getUserLevel(project1.projectId, user)
        p1_skills[0].pointIncrement = 500
        skillsService.createSkill(p1_skills[0])

        def user1_pts2 = skillsService.getUserStats(project1.projectId, user).userTotalPoints
        Integer user1_leve_t1 = skillsService.getUserLevel(project1.projectId, user)

        then:
        user1_pts1 == 10
        user1_pts2 == 500

        user1_leve_t0 == 0
        user1_leve_t1 == 1
    }

    def "deleting a skill should remove ophaned points and achievements"() {
        def project = SkillsFactory.createProject()
        def subject = SkillsFactory.createSubject()
        def skill1 = SkillsFactory.createSkill(1, 1, 1, 0, 5)
        skill1.pointIncrement = 50
        def skill2 = SkillsFactory.createSkill(1, 1, 2, 0, 5)

        skillsService.createProject(project)
        skillsService.createSubject(subject)
        skillsService.createSkill(skill1)
        skillsService.createSkill(skill2)

        def users = getRandomUsers(2)
        def user1 = users[0]
        def user2 = users[1]

        when:
        skillsService.addSkill(skill1, user1, new Date().minus(5))
        skillsService.addSkill(skill1, user1, new Date().minus(1))
        skillsService.addSkill(skill1, user2, new Date())
        skillsService.addSkill(skill2, user2, new Date())

        def u1Level = skillsService.getUserLevel(project.projectId, user1)
        assert u1Level == 2
        def u1SubjLevel = skillsService.getSkillSummary(user1, project.projectId, subject.subjectId)?.skillsLevel
        assert u1SubjLevel == 2
        def u2Level = skillsService.getUserLevel(project.projectId, user2)
        assert u2Level == 1
        def u2SubjLevel = skillsService.getSkillSummary(user2, project.projectId, subject.subjectId)?.skillsLevel
        assert u2SubjLevel == 1

        def projectUsers = skillsService.getProjectUsers(project.projectId)
        assert projectUsers.data.find { it.userId == user1 && it.totalPoints == 100 }
        assert projectUsers.data.find { it.userId == user2 && it.totalPoints == 60 }
        def subjectUsers = skillsService.getSubjectUsers(project.projectId, subject.subjectId)
        assert subjectUsers.data.find { it.userId == user1 && it.totalPoints == 100 }
        assert subjectUsers.data.find { it.userId == user2 && it.totalPoints == 60 }

        skillsService.deleteSkill(skill1)
        def projectUsersPostDelete = skillsService.getProjectUsers(project.projectId)
        def subjectUsersPostDelete = skillsService.getSubjectUsers(project.projectId, subject.subjectId)
        def u1LevelPostDelete = skillsService.getUserLevel(project.projectId, user1)
        def u2LevelPostDelete = skillsService.getUserLevel(project.projectId, user2)
        def u1SubjLevelPostDelete = skillsService.getSkillSummary(user1, project.projectId, subject.subjectId)?.skillsLevel
        def u2SubjLevelPostDelete = skillsService.getSkillSummary(user2, project.projectId, subject.subjectId)?.skillsLevel

        then:
        !projectUsersPostDelete.data.find { it.userId == user1 }
        projectUsersPostDelete.data.find { it.userId == user2 && it.totalPoints == 10 }
        !subjectUsersPostDelete.data.find { it.userId == user1 }
        subjectUsersPostDelete.data.find { it.userId == user2 && it.totalPoints == 10 }
        !u1LevelPostDelete
        !u1SubjLevelPostDelete
        u2LevelPostDelete == 1
        u2SubjLevelPostDelete == 1
    }

    def "deleting a subject should remove orphaned points and achievements"() {
        def project = SkillsFactory.createProject()
        def subject = SkillsFactory.createSubject()
        def subject2 = SkillsFactory.createSubject(1, 2)
        def skill1 = SkillsFactory.createSkill(1, 1, 1, 0, 5)
        skill1.pointIncrement = 50
        def skill2 = SkillsFactory.createSkill(1, 2, 2, 0, 5)
        skill2.pointIncrement = 20

        skillsService.createProject(project)
        skillsService.createSubject(subject)
        skillsService.createSubject(subject2)
        skillsService.createSkill(skill1)
        skillsService.createSkill(skill2)

        def users = getRandomUsers(2)
        def user1 = users[0]
        def user2 = users[1]

        when:
        skillsService.addSkill(skill1, user1, new Date().minus(5))
        skillsService.addSkill(skill1, user1, new Date().minus(1))
        skillsService.addSkill(skill1, user2, new Date())
        skillsService.addSkill(skill2, user2, new Date())

        def u1Level = skillsService.getUserLevel(project.projectId, user1)
        assert u1Level == 2
        def u2Level = skillsService.getUserLevel(project.projectId, user2)
        assert u2Level == 1

        def projectUsers = skillsService.getProjectUsers(project.projectId)
        assert projectUsers.data.find { it.userId == user1 && it.totalPoints == 100 }
        assert projectUsers.data.find { it.userId == user2 && it.totalPoints == 70 }

        skillsService.deleteSubject(subject)
        def projectUsersPostDelete = skillsService.getProjectUsers(project.projectId)
        def u1LevelPostDelete = skillsService.getUserLevel(project.projectId, user1)
        def u2LevelPostDelete = skillsService.getUserLevel(project.projectId, user2)

        then:
        !projectUsersPostDelete.data.find { it.userId == user1 }
        projectUsersPostDelete.data.find { it.userId == user2 && it.totalPoints == 20 }
        !userPointsRepo.findByProjectIdAndUserId(project.projectId, user1)
        !userAchievedLevelRepo.findAllByUserAndProjectIds(user1 , [project.projectId])
        !u1LevelPostDelete
        u2LevelPostDelete == 1
    }

    def "can filter project users by id and name"() {
        def project = SkillsFactory.createProject()
        def subject = SkillsFactory.createSubject()
        def skill1 = SkillsFactory.createSkill(1, 1, 1, 0, 5)
        skill1.pointIncrement = 50
        def skill2 = SkillsFactory.createSkill(1, 1, 2, 0, 5)

        skillsService.createProject(project)
        skillsService.createSubject(subject)
        skillsService.createSkill(skill1)
        skillsService.createSkill(skill2)

        def users = getRandomUsers(10)

        when:
        users.each {user ->
            skillsService.addSkill(skill1, user, new Date().minus(5))
            skillsService.addSkill(skill1, user, new Date().minus(1))
            skillsService.addSkill(skill2, user, new Date())
        }

        def projectUsers = skillsService.getProjectUsers(project.projectId)
        def selectedUser = projectUsers.data[0]
        def userFirstName = selectedUser.firstName
        def userLastName = selectedUser.lastName
        def userId = selectedUser.userId

        def filteredByFirstName = skillsService.getProjectUsers(project.projectId, 10, 1,'userId', true, userFirstName, 0)
        def filteredByLastName = skillsService.getProjectUsers(project.projectId, 10, 1,'userId', true, userLastName, 0)
        def filteredById = skillsService.getProjectUsers(project.projectId, 10, 1,'userId', true, userId, 0)

        then:
        projectUsers.count == 10
        filteredByFirstName.data[0].userId == userId
        filteredByFirstName.data[0].firstName == userFirstName
        filteredByFirstName.data[0].lastName == userLastName
        filteredByLastName.data[0].userId == userId
        filteredByLastName.data[0].firstName == userFirstName
        filteredByLastName.data[0].lastName == userLastName
        filteredById.data[0].userId == userId
        filteredById.data[0].firstName == userFirstName
        filteredById.data[0].lastName == userLastName
    }

    def "can filter subject users by id and name"() {
        def project = SkillsFactory.createProject()
        def subject = SkillsFactory.createSubject()
        def skill1 = SkillsFactory.createSkill(1, 1, 1, 0, 5)
        skill1.pointIncrement = 50
        def skill2 = SkillsFactory.createSkill(1, 1, 2, 0, 5)

        skillsService.createProject(project)
        skillsService.createSubject(subject)
        skillsService.createSkill(skill1)
        skillsService.createSkill(skill2)

        def users = getRandomUsers(10)

        when:
        users.each {user ->
            skillsService.addSkill(skill1, user, new Date().minus(5))
            skillsService.addSkill(skill1, user, new Date().minus(1))
            skillsService.addSkill(skill2, user, new Date())
        }

        def subjectUsers = skillsService.getSubjectUsers(project.projectId, subject.subjectId)
        def selectedUser = subjectUsers.data[0]
        def userFirstName = selectedUser.firstName
        def userLastName = selectedUser.lastName
        def userId = selectedUser.userId

        def filteredByFirstName = skillsService.getSubjectUsers(project.projectId, subject.subjectId, 10, 1,'userId', true, userFirstName, 0)
        def filteredByLastName = skillsService.getSubjectUsers(project.projectId, subject.subjectId, 10, 1,'userId', true, userLastName, 0)
        def filteredById = skillsService.getSubjectUsers(project.projectId, subject.subjectId, 10, 1,'userId', true, userId, 0)

        then:
        subjectUsers.count == 10
        filteredByFirstName.data[0].userId == userId
        filteredByFirstName.data[0].firstName == userFirstName
        filteredByFirstName.data[0].lastName == userLastName
        filteredByLastName.data[0].userId == userId
        filteredByLastName.data[0].firstName == userFirstName
        filteredByLastName.data[0].lastName == userLastName
        filteredById.data[0].userId == userId
        filteredById.data[0].firstName == userFirstName
        filteredById.data[0].lastName == userLastName
    }

    def "can filter skill users by id and name"() {
        def project = SkillsFactory.createProject()
        def subject = SkillsFactory.createSubject()
        def skill1 = SkillsFactory.createSkill(1, 1, 1, 0, 5)
        skill1.pointIncrement = 50

        skillsService.createProject(project)
        skillsService.createSubject(subject)
        skillsService.createSkill(skill1)

        def users = getRandomUsers(10)

        when:
        users.each {user ->
            skillsService.addSkill(skill1, user, new Date().minus(5))
            skillsService.addSkill(skill1, user, new Date().minus(1))
        }

        def skillUsers = skillsService.getSkillUsers(project.projectId, skill1.skillId)
        def selectedUser = skillUsers.data[0]
        def userFirstName = selectedUser.firstName
        def userLastName = selectedUser.lastName
        def userId = selectedUser.userId
        def fullNameString = userFirstName + ' ' + userLastName + ' (' + selectedUser.userIdForDisplay + ')';

        def filteredByFirstName = skillsService.getSkillUsers(project.projectId, skill1.skillId, 10, 1,'userId', true, userFirstName, 0)
        def filteredByLastName = skillsService.getSkillUsers(project.projectId, skill1.skillId, 10, 1,'userId', true, userLastName, 0)
        def filteredById = skillsService.getSkillUsers(project.projectId, skill1.skillId, 10, 1,'userId', true, userId, 0)
        def filteredByString = skillsService.getSkillUsers(project.projectId, skill1.skillId, 10, 1,'userId', true, fullNameString, 0)

        then:
        skillUsers.count == 10
        filteredByFirstName.data[0].userId == userId
        filteredByFirstName.data[0].firstName == userFirstName
        filteredByFirstName.data[0].lastName == userLastName
        filteredByLastName.data[0].userId == userId
        filteredByLastName.data[0].firstName == userFirstName
        filteredByLastName.data[0].lastName == userLastName
        filteredById.data[0].userId == userId
        filteredById.data[0].firstName == userFirstName
        filteredById.data[0].lastName == userLastName
        filteredByString.data[0].userId == userId
        filteredByString.data[0].firstName == userFirstName
        filteredByString.data[0].lastName == userLastName
    }

    def "editing skill points on an imported skill without finalization is successful"() {
        def project1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1skill1 = createSkill(1, 1, 1, 0, 5, 0, 100)

        skillsService.createProject(project1)
        skillsService.createSubject(p1subj1)
        skillsService.createSkill(p1skill1)

        skillsService.exportSkillToCatalog(project1.projectId, p1skill1.skillId)

        def project2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        skillsService.createProject(project2)
        skillsService.createSubject(p2subj1)

        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, p1skill1.skillId)

        when:
        def updated = skillsService.updateImportedSkill(project2.projectId, p1skill1.skillId, 500)
        def updatedSkill = skillsService.getSkill([projectId: project2.projectId, subjectId: p2subj1.subjectId, skillId: p1skill1.skillId])

        then:
        updated != null
        updated.body.success == true
        updatedSkill.pointIncrement == 500
    }

    def "editing skill points on an imported skill with finalization is successful"() {
        def project1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1skill1 = createSkill(1, 1, 1, 0, 5, 0, 100)

        skillsService.createProject(project1)
        skillsService.createSubject(p1subj1)
        skillsService.createSkill(p1skill1)

        skillsService.exportSkillToCatalog(project1.projectId, p1skill1.skillId)

        def project2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        skillsService.createProject(project2)
        skillsService.createSubject(p2subj1)

        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, p1skill1.skillId)
        skillsService.finalizeSkillsImportFromCatalog(project2.projectId, true)

        when:
        def updated = skillsService.updateImportedSkill(project2.projectId, p1skill1.skillId, 500)
        def updatedSkill = skillsService.getSkill([projectId: project2.projectId, subjectId: p2subj1.subjectId, skillId: p1skill1.skillId])

        then:
        updated != null
        updated.body.success == true
        updatedSkill.pointIncrement == 500
    }

    def "editing skill points on an imported quiz skill without finalization is successful"() {
        def project1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1skill1 = createSkill(1, 1, 1, 0, 1, 0, 100)
        def quiz = skillsService.createQuizDef(QuizDefFactory.createQuiz(1))
        p1skill1.selfReportingType = SkillDef.SelfReportingType.Quiz
        p1skill1.quizId = quiz.body.quizId

        skillsService.createProject(project1)
        skillsService.createSubject(p1subj1)
        skillsService.createSkill(p1skill1)

        skillsService.exportSkillToCatalog(project1.projectId, p1skill1.skillId)

        def project2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        skillsService.createProject(project2)
        skillsService.createSubject(p2subj1)

        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, p1skill1.skillId)

        when:
        def updated = skillsService.updateImportedSkill(project2.projectId, p1skill1.skillId, 500)
        def updatedSkill = skillsService.getSkill([projectId: project2.projectId, subjectId: p2subj1.subjectId, skillId: p1skill1.skillId])

        then:
        updated != null
        updated.body.success == true
        updatedSkill.pointIncrement == 500
    }

    def "editing skill points on an imported quiz skill with finalization is successful"() {
        def project1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1skill1 = createSkill(1, 1, 1, 0, 1, 0, 100)
        def quiz = skillsService.createQuizDef(QuizDefFactory.createQuiz(1))
        p1skill1.selfReportingType = SkillDef.SelfReportingType.Quiz
        p1skill1.quizId = quiz.body.quizId

        skillsService.createProject(project1)
        skillsService.createSubject(p1subj1)
        skillsService.createSkill(p1skill1)

        skillsService.exportSkillToCatalog(project1.projectId, p1skill1.skillId)

        def project2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        skillsService.createProject(project2)
        skillsService.createSubject(p2subj1)

        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, p1skill1.skillId)
        skillsService.finalizeSkillsImportFromCatalog(project2.projectId, true)

        when:
        def updated = skillsService.updateImportedSkill(project2.projectId, p1skill1.skillId, 500)
        def updatedSkill = skillsService.getSkill([projectId: project2.projectId, subjectId: p2subj1.subjectId, skillId: p1skill1.skillId])

        then:
        updated != null
        updated.body.success == true
        updatedSkill.pointIncrement == 500
    }

    def "archiving a user filters that user from user tables"() {
        def project = createProject()
        def subject = createSubject()
        def skill1 = createSkill(1, 1, 1, 0, 5)
        skill1.pointIncrement = 50
        def skill2 = createSkill(1, 1, 2, 0, 5)
        def badge = createBadge()

        skillsService.createProject(project)
        skillsService.createSubject(subject)
        skillsService.createSkill(skill1)
        skillsService.createSkill(skill2)
        skillsService.addBadge([projectId: project.projectId, badgeId: badgeId, name: 'Badge 1'])
        skillsService.assignSkillToBadge([projectId: project.projectId, badgeId: badgeId, skillId: skill1.skillId])
        skillsService.assignSkillToBadge([projectId: project.projectId, badgeId: badgeId, skillId: skill2.skillId])

        def users = getRandomUsers(2)
        def user1 = users[0]
        def user2 = users[1]

        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(user1, 'someTag', ["ABC"])
        rootUser.saveUserTag(user2, 'someTag', ["ABC"])

        // account for user tags in PKI mode
        MockUserInfoService.addUserTags(user1, 'someTag', 'ABC')
        MockUserInfoService.addUserTags(user2, 'someTag', 'ABC')

        skillsService.addSkill(skill1, user1, new Date().minus(5))
        skillsService.addSkill(skill1, user1, new Date().minus(1))
        skillsService.addSkill(skill1, user2, new Date())
        skillsService.addSkill(skill2, user2, new Date())

        def projectUsers = skillsService.getProjectUsers(project.projectId)
        assert projectUsers.data.find { it.userId == user1 && it.totalPoints == 100 }
        assert projectUsers.data.find { it.userId == user2 && it.totalPoints == 60 }
        def subjectUsers = skillsService.getSubjectUsers(project.projectId, subject.subjectId)
        assert subjectUsers.data.find { it.userId == user1 && it.totalPoints == 100 }
        assert subjectUsers.data.find { it.userId == user2 && it.totalPoints == 60 }
        def skill1Users = skillsService.getSkillUsers(project.projectId, skill1.skillId)
        assert skill1Users.data.find { it.userId == user1 && it.totalPoints == 100 }
        assert skill1Users.data.find { it.userId == user2 && it.totalPoints == 50 }
        def skill2Users = skillsService.getSkillUsers(project.projectId, skill2.skillId)
        assert !skill2Users.data.find { it.userId == user1 }
        assert skill2Users.data.find { it.userId == user2 && it.totalPoints == 10 }
        def badgeUsers = skillsService.getBadgeUsers(project.projectId, badgeId)
        assert badgeUsers.data.find { it.userId == user1 && it.totalPoints == 100 }
        assert badgeUsers.data.find { it.userId == user2 && it.totalPoints == 60 }
        def userTagUsers = skillsService.getUserTagUsers(project.projectId, 'someTag', 'ABC')
        assert userTagUsers.data.find { it.userId?.toLowerCase() == user1 && it.totalPoints == 100 }
        assert userTagUsers.data.find { it.userId.toLowerCase() == user2 && it.totalPoints == 60 }

        when:

        skillsService.archiveUsers([user1], project.projectId)

        def projectUsersAfterArchive = skillsService.getProjectUsers(project.projectId)
        def subjectUsersAfterArchive = skillsService.getSubjectUsers(project.projectId, subject.subjectId)
        def skill1UsersAfterArchive = skillsService.getSkillUsers(project.projectId, skill1.skillId)
        def skill2UsersAfterArchive = skillsService.getSkillUsers(project.projectId, skill2.skillId)
        def badgeUsersAfterArchive = skillsService.getBadgeUsers(project.projectId, badgeId)
        def userTagUsersAfterArchive = skillsService.getUserTagUsers(project.projectId, 'someTag', 'ABC')

        then:
        !projectUsersAfterArchive.data.find { it.userId == user1 }
        projectUsersAfterArchive.data.find { it.userId == user2 && it.totalPoints == 60 }
        !subjectUsersAfterArchive.data.find { it.userId == user1 }
        subjectUsersAfterArchive.data.find { it.userId == user2 && it.totalPoints == 60 }
        !skill1UsersAfterArchive.data.find { it.userId == user1 }
        skill1UsersAfterArchive.data.find { it.userId == user2 && it.totalPoints == 50 }
        !skill2UsersAfterArchive.data.find { it.userId == user1 }
        skill2UsersAfterArchive.data.find { it.userId == user2 && it.totalPoints == 10 }
        !badgeUsersAfterArchive.data.find { it.userId == user1 }
        badgeUsersAfterArchive.data.find { it.userId == user2 && it.totalPoints == 60 }
        !userTagUsersAfterArchive.data.find { it.userId == user1 }
        userTagUsersAfterArchive.data.find { it.userId == user2 && it.totalPoints == 60 }
    }
}
