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
import org.springframework.web.client.RestClientResponseException
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import spock.lang.IgnoreIf
import spock.lang.Requires
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
        e.message.contains("Project Name was not provided.")
        e.message.contains("errorCode:BadParam")
    }

    def "validate project id > 50 characters"(){
        when:
        skillsService.createProject([projectId: (1..51).collect({"a"}).join(""), name: 'name'])
        then:
        SkillsClientException exception = thrown()
        exception.httpStatus == HttpStatus.BAD_REQUEST
        exception.message.contains('[Project Id] must not exceed [50] chars.')
    }

    def "validate project id < 3 characters"(){
        when:
        skillsService.createProject([projectId: "aa", name: 'name' ])
        then:
        SkillsClientException exception = thrown()
        exception.httpStatus == HttpStatus.BAD_REQUEST
        exception.message.contains('[Project Id] must not be less than [3] chars.')
    }

    def "validate project name > 50 characters"(){
        when:
        skillsService.createProject([projectId: "pojectId", name: (1..51).collect({"a"}).join("") ])
        then:
        SkillsClientException exception = thrown()
        exception.httpStatus == HttpStatus.BAD_REQUEST
        exception.message.contains('[Project Name] must not exceed [50] chars.')
    }

    def "validate project name < 3 characters"(){
        when:
        skillsService.createProject([projectId: "aaaaaaa", name: 'me' ])
        then:
        SkillsClientException exception = thrown()
        exception.httpStatus == HttpStatus.BAD_REQUEST
        exception.message.contains('[Project Name] must not be less than [3] chars.')
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

    def "validate saving subject id > 50 characters"(){
        when:
        skillsService.createSubject([projectId: 'myProject', name: 'name' , subjectId: (1..51).collect({"a"}).join("")])
        then:
        SkillsClientException exception = thrown()
        exception.httpStatus == HttpStatus.BAD_REQUEST
        exception.message.contains('[Subject Id] must not exceed [50] chars.')
    }

    def "validate saving subject ID < 3 characters"(){
        when:
        skillsService.createSubject([projectId: 'myProject', name: "aaaa" , subjectId: 'an'])
        then:
        SkillsClientException exception = thrown()
        exception.httpStatus == HttpStatus.BAD_REQUEST
        exception.message.contains('[Subject Id] must not be less than [3] chars.')
    }

    def "validate saving subject with name > 50 characters"(){
        when:
        skillsService.createSubject([projectId: 'myProject', name: (1..51).collect({"a"}).join("") , subjectId: 'anid'])
        then:
        SkillsClientException exception = thrown()
        exception.httpStatus == HttpStatus.BAD_REQUEST
        exception.message.contains('[Subject Name] must not exceed [50] chars.')
    }

    def "validate saving subject with name < 3 characters"(){
        when:
        skillsService.createSubject([projectId: 'myProject', name: "aa" , subjectId: 'anid'])
        then:
        SkillsClientException exception = thrown()
        exception.httpStatus == HttpStatus.BAD_REQUEST
        exception.message.contains('[Subject Name] must not be less than [3] chars.')
    }

    def "validate saving subject with description > 2000 characters"(){
        when:
        def result = skillsService.createSubject([projectId: 'myProject', name: '99aa', subjectId: 'anid', description: (1..2001).collect({"a"}).join("") ])

        then:
        SkillsClientException exception = thrown()
        exception.httpStatus == HttpStatus.BAD_REQUEST
        exception.message.contains('[Subject Description] must not exceed [2000] chars.')
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

    def "create skill: numPerformToCompletion <= 10000"() {
        Map skill = SkillsFactory.createSkill()
        skill.projectId = projId
        skill.numPerformToCompletion = 10001

        def subject = SkillsFactory.createSubject()
        subject.projectId = projId
        skillsService.createSubject(subject)
        when:
        skillsService.createSkill(skill)
        then:
        SkillsClientException exception = thrown()
        exception.httpStatus == HttpStatus.BAD_REQUEST
        exception.message.contains('[numPerformToCompletion] must be <= [10000]')
    }

    def "create skill: pointIncrement <= 10000"() {
        Map skill = SkillsFactory.createSkill()
        skill.projectId = projId
        skill.pointIncrement = 10001

        def subject = SkillsFactory.createSubject()
        subject.projectId = projId
        skillsService.createSubject(subject)
        when:
        skillsService.createSkill(skill)
        then:
        SkillsClientException exception = thrown()
        exception.httpStatus == HttpStatus.BAD_REQUEST
        exception.message.contains('[pointIncrement] must be <= [10000]')
    }

    def "create skill: numMaxOccurrencesIncrementInterval <= 999"() {
        Map skill = SkillsFactory.createSkill()
        skill.projectId = projId
        skill.numPerformToCompletion = 5000
        skill.numMaxOccurrencesIncrementInterval = 1000

        def subject = SkillsFactory.createSubject()
        subject.projectId = projId
        skillsService.createSubject(subject)
        when:
        skillsService.createSkill(skill)
        then:
        SkillsClientException exception = thrown()
        exception.httpStatus == HttpStatus.BAD_REQUEST
        exception.message.contains('[numMaxOccurrencesIncrementInterval] must be <= [999]')
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

    def 'validate saving skills with version > 999'() {
        when:
        def result = skillsService.createSkill([projectId: 'myProject', subjectId: 'subjectId1', skillId: 'id1', name: 'name1', pointIncrement: 1, pointIncrementInterval: 60, numMaxOccurrencesIncrementInterval: 1, numPerformToCompletion: 1, version: 1000], false )

        then:
        result
        result.statusCode == HttpStatus.BAD_REQUEST
        result.body.explanation.contains('[Skill Version] must be <= [999]')
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
        def result = skillsService.createSkill([projectId: 'myProject', subjectId: 'subj', skillId: 'id1', name: (1..101).collect {"a"}.join(""), pointIncrement: 1, pointIncrementInterval: 60, numMaxOccurrencesIncrementInterval: 1, numPerformToCompletion: 5], false )

        then:
        result
        result.statusCode == HttpStatus.BAD_REQUEST
        result.body.explanation.contains('[Skill Name] must not exceed [100] chars.')
    }

    def 'validate a skill name < 3 characters'(){
        when:
        def r = skillsService.createSubject([projectId: 'myProject', subjectId: 'subj', name:'aname'], false)
        def result = skillsService.createSkill([projectId: 'myProject', subjectId: 'subj', skillId: 'id1', name: "so", pointIncrement: 1, pointIncrementInterval: 60, numMaxOccurrencesIncrementInterval: 1, numPerformToCompletion: 5], false )

        then:
        result
        result.statusCode == HttpStatus.BAD_REQUEST
        result.body.explanation.contains('[Skill Name] must not be less than [3] chars.')
    }

    def 'validate a skill id > 50 characters'(){
        when:
        def r = skillsService.createSubject([projectId: 'myProject', subjectId: 'subj', name:'aname'], false)
        def result = skillsService.createSkill([projectId: 'myProject', subjectId: 'subj', skillId: (1..51).collect {"a"}.join(""), name: "anem is", pointIncrement: 1, pointIncrementInterval: 60, numMaxOccurrencesIncrementInterval: 1, numPerformToCompletion: 5], false )

        then:
        result
        result.statusCode == HttpStatus.BAD_REQUEST
        result.body.explanation.contains('[Skill Id] must not exceed [50] chars.')
    }

    def 'validate a skill id < 3 characters'(){
        when:
        def r = skillsService.createSubject([projectId: 'myProject', subjectId: 'subj', name:'aname'], false)
        def result = skillsService.createSkill([projectId: 'myProject', subjectId: 'subj', skillId: 'id', name: "soeaef", pointIncrement: 1, pointIncrementInterval: 60, numMaxOccurrencesIncrementInterval: 1, numPerformToCompletion: 5], false )

        then:
        result
        result.statusCode == HttpStatus.BAD_REQUEST
        result.body.explanation.contains('[Skill Id] must not be less than [3] chars.')
    }

    def 'validate badge name > 50 characters'(){
        when:
        def r = skillsService.createSubject([projectId: 'myProject', subjectId: 'subj', name:'aname'], false)
        def result = skillsService.createSkill([projectId: 'myProject', subjectId: 'subj', skillId: 'id1', name: 'name', pointIncrement: 1, pointIncrementInterval: 60, numMaxOccurrencesIncrementInterval: 1, numPerformToCompletion: 5], false )

        Date oneWeekAgo = new Date()-7
        Date twoWeeksAgo = new Date()-14

        Map badge = [projectId: projId, badgeId: 'badge1', name: (1..51).collect {"a"}.join(""), startDate: twoWeeksAgo, endDate: oneWeekAgo,
                     requiredSkillsIds: ['id1']]
        skillsService.createBadge(badge)

        then:
        SkillsClientException exception = thrown()
        exception.httpStatus == HttpStatus.BAD_REQUEST
        exception.message.contains('[Badge Name] must not exceed [50] chars.')
    }

    def 'validate badge name < 3 characters'(){
        when:
        def r = skillsService.createSubject([projectId: 'myProject', subjectId: 'subj', name:'aname'], false)
        def result = skillsService.createSkill([projectId: 'myProject', subjectId: 'subj', skillId: 'id1', name: 'name', pointIncrement: 1, pointIncrementInterval: 60, numMaxOccurrencesIncrementInterval: 1, numPerformToCompletion: 5], false )

        Date oneWeekAgo = new Date()-7
        Date twoWeeksAgo = new Date()-14

        Map badge = [projectId: projId, badgeId: 'badge1', name: "aa", startDate: twoWeeksAgo, endDate: oneWeekAgo,
                     requiredSkillsIds: ['id1']]
        skillsService.createBadge(badge)

        then:
        SkillsClientException exception = thrown()
        exception.httpStatus == HttpStatus.BAD_REQUEST
        exception.message.contains('[Badge Name] must not be less than [3] chars')
    }

    def 'validate badge description > 2000 characters'(){
        when:
        def r = skillsService.createSubject([projectId: 'myProject', subjectId: 'subj', name:'aname'], false)
        def result = skillsService.createSkill([projectId: 'myProject', subjectId: 'subj', skillId: 'id1', name: 'name', pointIncrement: 1, pointIncrementInterval: 60, numMaxOccurrencesIncrementInterval: 1, numPerformToCompletion: 5], false )

        Date oneWeekAgo = new Date()-7
        Date twoWeeksAgo = new Date()-14

        Map badge = [projectId: projId, badgeId: 'badge1', name: "aaaa", startDate: twoWeeksAgo, endDate: oneWeekAgo, description: (1..2001).collect { "a" }.join(""),
                     requiredSkillsIds: ['id1']]
        skillsService.createBadge(badge)

        then:
        SkillsClientException exception = thrown()
        exception.httpStatus == HttpStatus.BAD_REQUEST
        exception.message.contains('[Badge Description] must not exceed [2000] chars.')
    }

    def 'validate badge id > 50 characters'(){
        when:
        def r = skillsService.createSubject([projectId: 'myProject', subjectId: 'subj', name:'aname'], false)
        def result = skillsService.createSkill([projectId: 'myProject', subjectId: 'subj', skillId: 'id1', name: 'name', pointIncrement: 1, pointIncrementInterval: 60, numMaxOccurrencesIncrementInterval: 1, numPerformToCompletion: 5], false )

        Date oneWeekAgo = new Date()-7
        Date twoWeeksAgo = new Date()-14

        Map badge = [projectId: projId, badgeId: (1..51).collect {"a"}.join(""), name: "badge 1", startDate: twoWeeksAgo, endDate: oneWeekAgo,
                     requiredSkillsIds: ['id1']]
        skillsService.createBadge(badge)

        then:
        SkillsClientException exception = thrown()
        exception.httpStatus == HttpStatus.BAD_REQUEST
        exception.message.contains('[Badge Id] must not exceed [50] chars.')
    }

    def 'validate badge id < 3 characters'(){
        when:
        def r = skillsService.createSubject([projectId: 'myProject', subjectId: 'subj', name:'aname'], false)
        def result = skillsService.createSkill([projectId: 'myProject', subjectId: 'subj', skillId: 'id1', name: 'name', pointIncrement: 1, pointIncrementInterval: 60, numMaxOccurrencesIncrementInterval: 1, numPerformToCompletion: 5], false )

        Date oneWeekAgo = new Date()-7
        Date twoWeeksAgo = new Date()-14

        Map badge = [projectId: projId, badgeId: "aa", name: "badge 1", startDate: twoWeeksAgo, endDate: oneWeekAgo,
                     requiredSkillsIds: ['id1']]
        skillsService.createBadge(badge)

        then:
        SkillsClientException exception = thrown()
        exception.httpStatus == HttpStatus.BAD_REQUEST
        exception.message.contains('[Badge Id] must not be less than [3] chars.')
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def 'test userExists endpoint works correctly'() {
        when:
        String existingUser = skillsService.wsHelper.username
        boolean existingUserExists = skillsService.doesUserExist(existingUser)

        String nonExistingUser = 'nonExistingUser'
        boolean nonExistingUserExists = skillsService.doesUserExist(nonExistingUser, false)

        then:
        existingUserExists
        !nonExistingUserExists
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def 'users password >= 8 chars'() {
        when:
        createService("veryUniqueIda0201", "aaaaaaa")
        then:
        SkillsClientException exception = thrown()
        exception.httpStatus == HttpStatus.BAD_REQUEST
        exception.message.contains('[password] must not be less than [8] chars')
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def 'users password <= 40 chars'() {
        when:
        createService("veryUniqueIda0201", (1..41).collect { "a" }.join(""))
        then:
        SkillsClientException exception = thrown()
        exception.httpStatus == HttpStatus.BAD_REQUEST
        exception.message.contains('[password] must not exceed [40] chars')
    }
}
