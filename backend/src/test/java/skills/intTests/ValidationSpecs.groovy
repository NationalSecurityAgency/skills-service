package skills.intTests

import org.springframework.http.HttpStatus
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import spock.lang.Specification

class ValidationSpecs extends DefaultIntSpec {

    String projId = "myProject"

    def setup() {
        skillsService.deleteProjectIfExist(projId)
        skillsService.createProject([projectId: projId, name: "My Project"])
    }

    def 'validate saving project without a project name'() {
        when:
        skillsService.createProject([projectId: "otherProj"])

        then:
        SkillsClientException e = thrown()
        e.message.contains("explanation:Project name was not provided.")
        e.message.contains("errorCode:BadParam")
    }

    def 'validate saving subjects without a subject name'() {
        when:
        def result = skillsService.createSubject([projectId: 'myProject', subjectId: 'id1'], false)

        then:
        result
        result.statusCode == HttpStatus.BAD_REQUEST
        result.body.explanation.contains('Subject name was not provided.')
        result.body.errorCode.contains('BadParam')
    }

    def 'validate saving subjects without a subject id'() {
        when:
        def result = skillsService.createSubject([projectId: 'myProject', name: 'name1'], false)

        then:
        result
        result.statusCode == HttpStatus.BAD_REQUEST
        result.body.explanation.contains('Subject Id was not provided.')

    }

    def "validate saving subject with name > 50 characters"(){
        when:
        def result = skillsService.createSubject([projectId: 'myProject', name: 'iiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiii', subjectId: 'anid'], false)

        then:
        result
        result.statusCode == HttpStatus.BAD_REQUEST
        result.body.explanation.contains('Bad Name')
    }

    def 'validate saving skills without an id'() {
        when:
        def result = skillsService.createSkill([projectId: 'myProject', subjectId: 'subjectId1', name: 'name1'], false )

        then:
        result
        result.statusCode == HttpStatus.BAD_REQUEST
        result.body.explanation.contains('Skill Id was not provided.')
    }

    def 'validate saving skills with a non-positive point increment'() {
        when:
        def result = skillsService.createSkill([projectId: 'myProject', subjectId: 'subjectId1', skillId: 'id1', name: 'name1', pointIncrement: 0], false )

        then:
        result
        result.statusCode == HttpStatus.BAD_REQUEST
        result.body.explanation.contains('pointIncrement must be > 0')
    }

    def 'validate saving skills with a non-positive point increment interval'() {
        when:
        def result = skillsService.createSkill([projectId: 'myProject', subjectId: 'subjectId1', skillId: 'id1', name: 'name1', pointIncrement: 1, pointIncrementInterval: -1], false)

        then:
        result
        result.statusCode == HttpStatus.BAD_REQUEST
        result.body.explanation.contains('pointIncrementInterval must be >= 0')
    }

    def "create skill: if Time Window enabled then numMaxOccurrencesIncrementInterval must be > 0"() {
        Map skill = SkillsFactory.createSkill()
        skill.projectId = projId
        skill.numMaxOccurrencesIncrementInterval = 0
        when:
        def result = skillsService.createSkill(skill, false)
        then:
        result
        result.statusCode == HttpStatus.BAD_REQUEST
        result.body.explanation.contains('numMaxOccurrencesIncrementInterval must be > 0')
    }

    def "create skill: if Time Window enabled then numMaxOccurrencesIncrementInterval must be <= numPerformToCompletion"() {
        Map skill = SkillsFactory.createSkill()
        skill.projectId = projId
        skill.numMaxOccurrencesIncrementInterval = 2
        skill.numPerformToCompletion = 1
        when:
        def result = skillsService.createSkill(skill, false)
        then:
        result
        result.statusCode == HttpStatus.BAD_REQUEST
        result.body.explanation.contains('numPerformToCompletion must be >= numMaxOccurrencesIncrementInterval')
    }

    def "create skill: if Time Window disabled then no reason to validate numMaxOccurrencesIncrementInterval as compared to numPerformToCompletion"() {
        Map skill = SkillsFactory.createSkill()
        skill.projectId = projId
        skill.pointIncrementInterval = 0 // disabled
        skill.numMaxOccurrencesIncrementInterval = 2
        skill.numPerformToCompletion = 1

        def subject = SkillsFactory.createSubject()
        subject.projectId = projId
        skillsService.createSubject(subject)
        when:
        def result = skillsService.createSkill(skill, false)
        then:
        result
        result.statusCode == HttpStatus.OK
    }

    def "create skill: if Time Window disabled then no reason to validate numMaxOccurrencesIncrementInterval"() {
        Map skill = SkillsFactory.createSkill()
        skill.projectId = projId
        skill.pointIncrementInterval = 0 // disabled
        skill.numMaxOccurrencesIncrementInterval = -1

        def subject = SkillsFactory.createSubject()
        subject.projectId = projId
        skillsService.createSubject(subject)
        when:
        def result = skillsService.createSkill(skill, false)
        then:
        result
        result.statusCode == HttpStatus.OK
    }

    def 'validate saving skills with a non-positive max skill achieved count'() {
        when:
        def result = skillsService.createSkill([projectId: 'myProject', subjectId: 'subjectId1', skillId: 'id1', name: 'name1', pointIncrement: 1, pointIncrementInterval: 60, numMaxOccurrencesIncrementInterval: 1, numPerformToCompletion: 0], false )

        then:
        result
        result.statusCode == HttpStatus.BAD_REQUEST
        result.body.explanation.contains('numPerformToCompletion must be > 0')
    }

    def 'validate saving skills with a negative version'() {
        when:
        def result = skillsService.createSkill([projectId: 'myProject', subjectId: 'subjectId1', skillId: 'id1', name: 'name1', pointIncrement: 1, pointIncrementInterval: 60, numMaxOccurrencesIncrementInterval: 1, numPerformToCompletion: 1, version: -1], false )

        then:
        result
        result.statusCode == HttpStatus.BAD_REQUEST
        result.body.explanation.contains('version must be >= 0')
    }

    def 'validate saving skills with a too-large version'() {
        when:
        def result = skillsService.createSkill([projectId: 'myProject', subjectId: 'subjectId1', skillId: 'id1', name: 'name1', pointIncrement: 1, pointIncrementInterval: 60, numMaxOccurrencesIncrementInterval: 1, numPerformToCompletion: 1, version: 1000], false )

        then:
        result
        result.statusCode == HttpStatus.BAD_REQUEST
        result.body.explanation.contains('version exceeds max version')
    }

    def 'validate a skill version cannot be edited'() {
        // this ensures somebody can't create their own request to update the version
        when:
        skillsService.createSubject([projectId: projId, subjectId:  'subjectId1', name: 'subject1'])
        def result = skillsService.createSkill([projectId: projId, subjectId: 'subjectId1', skillId: 'id1', name: 'name1', pointIncrement: 1, pointIncrementInterval: 60, numMaxOccurrencesIncrementInterval: 1, numPerformToCompletion: 1, version: 0], false)
        assert result.body.success == true
        def getResult = skillsService.getSkill([projectId: projId, subjectId: 'subjectId1', skillId: 'id1'])
        def result2 = skillsService.createSkill([projectId: projId, subjectId: 'subjectId1', skillId: 'id1', name: 'name1', pointIncrement: 1, pointIncrementInterval: 60, numMaxOccurrencesIncrementInterval: 1, numPerformToCompletion: 1, version: 1, id: getResult.id], false)
        assert result2.success == true
        def getResult2 = skillsService.getSkill([projectId: projId, subjectId: 'subjectId1', skillId: 'id1'])

        then:
        result
        result.success == true
        result2
        result2.success == true
        getResult2.version == 0
    }

    def 'validate a skill name > 100 characters'(){
        when:
        def r = skillsService.createSubject([projectId: 'myProject', subjectId: 'subj', name:'aname'], false)
        def result = skillsService.createSkill([projectId: 'myProject', subjectId: 'subj', skillId: 'id1', name: 'name: iiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiii', pointIncrement: 1, pointIncrementInterval: 60, numMaxOccurrencesIncrementInterval: 1, numPerformToCompletion: 5], false )

        then:
        result
        result.statusCode == HttpStatus.BAD_REQUEST
        result.body.explanation.contains('Bad Name')
    }

    def 'validate badge name > 50 characters'(){
        when:
        def r = skillsService.createSubject([projectId: 'myProject', subjectId: 'subj', name:'aname'], false)
        def result = skillsService.createSkill([projectId: 'myProject', subjectId: 'subj', skillId: 'id1', name: 'name', pointIncrement: 1, pointIncrementInterval: 60, numMaxOccurrencesIncrementInterval: 1, numPerformToCompletion: 5], false )

        Date oneWeekAgo = new Date()-7
        Date twoWeeksAgo = new Date()-14

        Map badge = [projectId: projId, badgeId: 'badge1', name: 'Test Badge 1iiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiii', startDate: twoWeeksAgo, endDate: oneWeekAgo,
                     requiredSkillsIds: ['id1']]
        skillsService.createBadge(badge)

        then:
        thrown(SkillsClientException)
    }

    def 'test userExists endpoint works correctly'() {
        when:
        String existingUser = skillsService.wsHelper.username
        boolean existingUserExists = skillsService.doesUserExist(existingUser)

        String nonExistingUser = 'nonExistingUser'
        boolean nonExistingUserExists = skillsService.doesUserExist(nonExistingUser)

        then:
        existingUserExists
        !nonExistingUserExists
    }

}
