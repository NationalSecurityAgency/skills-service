package skills.intTests

import org.apache.http.client.methods.HttpHead
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
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

        println "${wsHelper.skillsService}/app/projects/aProject"
        ResponseEntity result = wsHelper.restTemplateWrapper.postForEntity("${wsHelper.skillsService}/app/projects/aProject", jsonRequest, String.class)

        println result.body.getClass()
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


    def "When skill id is updated it should persist to 'performed skills'"(){
        String userId = "user1"

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(5, 1, 1)

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
}
