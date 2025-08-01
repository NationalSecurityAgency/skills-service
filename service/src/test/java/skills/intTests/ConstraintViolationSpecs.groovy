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
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import spock.lang.IgnoreRest

class ConstraintViolationSpecs extends DefaultIntSpec {

    def "duplicate project name"() {
        Map proj = SkillsFactory.createProject()
        Map proj2 = SkillsFactory.createProject(2)
        Map copy = new HashMap(proj)
        copy.name = proj2.name.toUpperCase()

        skillsService.createProject(proj)
        skillsService.createProject(proj2)

        when:
        skillsService.updateProject(copy)
        then:
        SkillsClientException exception = thrown()
        exception.message.contains("explanation:Project with name [TEST PROJECT#2] already exists")
        exception.message.contains("errorCode:ConstraintViolation")
    }

    def "special characters in project name"() {
        Map proj = SkillsFactory.createProject()
        proj.name = "special 123456789_-#()[]/*%;"

        skillsService.createProject(proj)
        when:
        def origIdExists = skillsService.projectIdExists([projectId: proj.projectId])
        def origNameExists = skillsService.projectNameExists([projectName: proj.name])
        def project = skillsService.getProject(proj.projectId)

        then:
        origIdExists
        origNameExists
        project
        project.name == "special 123456789_-#()[]/*%;"

    }

    def "check for existing project name"(){
        Map proj = SkillsFactory.createProject()
        proj.name = "Test Project 1"
        skillsService.createProject(proj)

        when:
        def lowerExists = skillsService.projectNameExists([projectName: proj.name.toLowerCase()])
        def uppperExists = skillsService.projectNameExists([projectName: proj.name.toUpperCase()])
        def leadingSpaceExists = skillsService.projectNameExists([projectName: " ${proj.name}"])
        def trailingSpaceExists = skillsService.projectNameExists([projectName: "${proj.name} "])

        then:

        lowerExists
        uppperExists
        leadingSpaceExists
        trailingSpaceExists
    }

    def "duplicate project id"() {
        Map proj = SkillsFactory.createProject()
        skillsService.createProject(proj)

        Map copy = new HashMap(proj)
        copy.projectId = proj.projectId.toUpperCase()
        copy.name = "Some other"

        SkillsService skillsService1 = createService("someOtherUser")

        when:
        skillsService1.createProject(copy)
        then:
        SkillsClientException exception = thrown()
        exception.message.contains("explanation:Project with id [TESTPROJECT1] already exists")
        exception.message.contains("errorCode:ConstraintViolation")
    }

    def "duplicate subject name"() {
        Map proj = SkillsFactory.createProject()
        Map subject = SkillsFactory.createSubject()

        Map copy = new HashMap(subject)
        copy.subjectId = "somethingElse"
        copy.name = subject.name.toUpperCase()

        skillsService.createProject(proj)
        skillsService.createSubject(subject)

        when:
        skillsService.createSubject(copy)
        then:
        SkillsClientException exception = thrown()
        exception.message.contains("explanation:Subject with name [TEST SUBJECT #1] already exists")
        exception.message.contains("errorCode:ConstraintViolation")
    }

    def "check existing subject name"() {
        Map proj = SkillsFactory.createProject()
        Map subject = SkillsFactory.createSubject()
        subject.name = "Subject 1"
        skillsService.createProject(proj)
        skillsService.createSubject(subject)

        when:
        def lowerExists = skillsService.subjectNameExists([projectId: proj.projectId, subjectName: subject.name.toLowerCase()])
        def upperExists = skillsService.subjectNameExists([projectId: proj.projectId, subjectName: subject.name.toUpperCase()])
        def leadingSpaceExists = skillsService.subjectNameExists([projectId: proj.projectId, subjectName: " ${subject.name}".toString()])
        def trailingSpaceExists = skillsService.subjectNameExists([projectId: proj.projectId, subjectName: "${subject.name} ".toString()])

        then:
        lowerExists
        upperExists
        leadingSpaceExists
        trailingSpaceExists
    }

    def "special characters in subject name"() {
        Map proj = SkillsFactory.createProject()
        Map subject = SkillsFactory.createSubject()
        subject.name = "special 123456789_-#()[]/*%;"
        skillsService.createProject(proj)
        skillsService.createSubject(subject)

        when:
        def existsOriginal = skillsService.subjectNameExists([projectId: proj.projectId, subjectName: subject.name])
        def subj = skillsService.getSubject([projectId: proj.projectId, subjectId: subject.subjectId])

        then:
        existsOriginal
        subj
        subj.name == 'special 123456789_-#()[]/*%;'
    }

    def "duplicate subject id - will belong to another user and will fail auth"() {
        Map proj = SkillsFactory.createProject()
        Map subject = SkillsFactory.createSubject()
        Map copy = new HashMap(subject)
        copy.subjectId = subject.subjectId.toUpperCase()
        copy.name = "somethingElse"

        skillsService.createProject(proj)
        skillsService.createSubject(subject)

        SkillsService skillsService1 = createService("otherUser")

        when:
        skillsService1.createSubject(copy)
        then:
        SkillsClientException exception = thrown()
        exception.httpStatus == HttpStatus.FORBIDDEN
    }

    def "duplicate subject id"() {
        Map proj = SkillsFactory.createProject()
        Map subject = SkillsFactory.createSubject()
        Map subject2 = SkillsFactory.createSubject(1, 2)
        Map copy = new HashMap(subject)
        copy.subjectId = subject2.subjectId.toUpperCase()
        copy.name = "somethingElse"

        skillsService.createProject(proj)
        skillsService.createSubject(subject)
        skillsService.createSubject(subject2)

        when:
        skillsService.updateSubject(copy, subject.subjectId)
        then:
        SkillsClientException exception = thrown()
        exception.message.contains("explanation:Subject with id [TESTSUBJECT2] already exists")
        exception.message.contains("errorCode:ConstraintViolation")
    }

    def "duplicate skill name"() {
        Map proj = SkillsFactory.createProject()
        Map subject = SkillsFactory.createSubject()
        Map skill = SkillsFactory.createSkill()
        Map copy = new HashMap(skill)
        copy.skillId = "somethingElse"
        copy.name = skill.name.toUpperCase()

        skillsService.createProject(proj)
        skillsService.createSubject(subject)
        skillsService.createSkill(skill)

        when:
        skillsService.createSkill(copy)
        then:
        SkillsClientException exception = thrown()
        exception.message.contains("explanation:Skill with name [TEST SKILL 1] already exists")
        exception.message.contains("errorCode:ConstraintViolation")
    }

    def "skill name exists"() {
        Map proj = SkillsFactory.createProject()
        Map subject = SkillsFactory.createSubject()
        Map skill = SkillsFactory.createSkill()

        skillsService.createProject(proj)
        skillsService.createSubject(subject)
        skillsService.createSkill(skill)

        when:
        def lowerExists = skillsService.skillNameExists([projectId: proj.projectId, skillName: skill.name.toLowerCase()])
        def upperExists = skillsService.skillNameExists([projectId: proj.projectId, skillName: skill.name.toUpperCase()])
        def leadingSpaceExists = skillsService.skillNameExists([projectId: proj.projectId, skillName: " ${skill.name}".toString()])
        def trailingSpaceExists = skillsService.skillNameExists([projectId: proj.projectId, skillName: "${skill.name} ".toString()])

        then:
        lowerExists
        upperExists
        leadingSpaceExists
        trailingSpaceExists
    }

    def "skill name with special characters"() {
        Map proj = SkillsFactory.createProject()
        Map subject = SkillsFactory.createSubject()
        Map skill = SkillsFactory.createSkill()
        skill.name = "special 123456789_-#()[]/*%;"

        skillsService.createProject(proj)
        skillsService.createSubject(subject)
        skillsService.createSkill(skill)

        when:
        def originalNameExists = skillsService.skillNameExists([projectId: proj.projectId, skillName: skill.name.toLowerCase()])
        def retrievedSkill = skillsService.getSkill([projectId: proj.projectId, subjectId: subject.subjectId, skillId: skill.skillId])

        then:
        originalNameExists
        retrievedSkill
        retrievedSkill.name == 'special 123456789_-#()[]/*%;'
    }

    def "duplicate skill id"() {
        Map proj = SkillsFactory.createProject()
        Map subject = SkillsFactory.createSubject()
        Map skill = SkillsFactory.createSkill()
        Map skill2 = SkillsFactory.createSkill(1, 1, 2,)
        Map copy = new HashMap(skill)
        copy.skillId = skill2.skillId.toUpperCase()
        copy.name = "somethingElse"

        skillsService.createProject(proj)
        skillsService.createSubject(subject)
        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)

        when:
        skillsService.updateSkill(copy, skill.skillId)
        then:
        SkillsClientException exception = thrown()
        exception.message.contains("explanation:Skill with id [SKILL2] already exists")
        exception.message.contains("errorCode:ConstraintViolation")
    }

    def "duplicate badge name"() {
        Map proj = SkillsFactory.createProject()
        Map subject = SkillsFactory.createSubject()
        Map badge = SkillsFactory.createBadge()
        Map copy = new HashMap(badge)
        copy.badgeId = "somethingElse"
        copy.name = badge.name.toUpperCase()

        skillsService.createProject(proj)
        skillsService.createSubject(subject)
        skillsService.createBadge(badge)

        when:
        skillsService.createBadge(copy)
        then:
        SkillsClientException exception = thrown()
        exception.message.contains("explanation:Badge with name [TEST BADGE 1] already exists")
        exception.message.contains("errorCode:ConstraintViolation")
    }

    def "badge name exists"() {
        Map proj = SkillsFactory.createProject()
        Map subject = SkillsFactory.createSubject()
        Map badge = SkillsFactory.createBadge()

        skillsService.createProject(proj)
        skillsService.createSubject(subject)
        skillsService.createBadge(badge)

        when:
        def lower = skillsService.badgeNameExists([projectId: proj.projectId, badgeName: badge.name.toLowerCase()])
        def upper = skillsService.badgeNameExists([projectId: proj.projectId, badgeName: badge.name.toUpperCase()])
        def leadingSpace = skillsService.badgeNameExists([projectId: proj.projectId, badgeName: " ${badge.name}".toString()])
        def trailingSpace = skillsService.badgeNameExists([projectId: proj.projectId, badgeName: "${badge.name} ".toString()])

        then:
        lower
        upper
        leadingSpace
        trailingSpace
    }

    def "global badge name exists"() {
        Map badge = SkillsFactory.createBadge()

        skillsService.createGlobalBadge(badge)

        when:
        def lower = skillsService.doesGlobalBadgeNameExists(badge.name.toLowerCase())
        def upper = skillsService.doesGlobalBadgeNameExists(badge.name.toUpperCase())
        def leadingSpace = skillsService.doesGlobalBadgeNameExists(" ${badge.name}".toString())
        def trailingSpace = skillsService.doesGlobalBadgeNameExists("${badge.name} ".toString())

        then:
        lower
        upper
        leadingSpace
        trailingSpace
    }

    def "badge name with special characters"() {
        Map proj = SkillsFactory.createProject()
        Map subject = SkillsFactory.createSubject()
        Map badge = SkillsFactory.createBadge()
        String badgeName = "foo 123456789_-#()[]/*%;"
        badge.name = badgeName

        skillsService.createProject(proj)
        skillsService.createSubject(subject)
        skillsService.createBadge(badge)

        when:
        def exists = skillsService.badgeNameExists([projectId: proj.projectId, badgeName: badgeName])
        def existingBadge = skillsService.getBadge([projectId: proj.projectId, badgeId: badge.badgeId])

        then:
        exists
        existingBadge
        existingBadge.name == 'foo 123456789_-#()[]/*%;'
    }

    def "duplicate badge id - project belongs to another user and will fail auth"() {
        Map proj = SkillsFactory.createProject()
        Map subject = SkillsFactory.createSubject()
        Map badge = SkillsFactory.createBadge()
        Map copy = new HashMap(badge)
        copy.name = "somethingElse"
        copy.badgeId = badge.badgeId.toUpperCase()

        skillsService.createProject(proj)
        skillsService.createSubject(subject)
        skillsService.createBadge(badge)

        SkillsService skillsService1 = createService("anotherUser")

        when:
        skillsService1.createBadge(copy)
        then:
        SkillsClientException exception = thrown()
        exception.httpStatus == HttpStatus.FORBIDDEN
    }

    def "duplicate badge id"() {
        Map proj = SkillsFactory.createProject()
        Map subject = SkillsFactory.createSubject()
        Map badge = SkillsFactory.createBadge()
        Map badge2 = SkillsFactory.createBadge(1, 2)
        Map copy = new HashMap(badge)
        copy.name = "somethingElse"
        copy.badgeId = badge2.badgeId.toUpperCase()

        skillsService.createProject(proj)
        skillsService.createSubject(subject)
        skillsService.createBadge(badge)
        skillsService.createBadge(badge2)

        when:
        skillsService.updateBadge(copy, badge.badgeId)
        then:
        SkillsClientException exception = thrown()
        exception.message.contains("Badge with id [BADGE2] already exists! Sorry!")
        exception.message.contains("errorCode:ConstraintViolation")
    }

    def "subject and skill can NOT share id"() {
        String sameId = "sameId"
        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        def skill = SkillsFactory.createSkill(1, 1)

        subj.subjectId = sameId
        skill.skillId = sameId
        skill.subjectId = sameId
        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        when:
        skillsService.createSkill(skill)
        then:
        SkillsClientException exception = thrown()
        // some db support case-insensitive constraints and some do not, that's life for you
        exception.message.contains("explanation:Provided skill id already exist") || exception.message.contains("explanation:Data Integrity Violation")
        exception.message.contains("errorCode:ConstraintViolation")
    }

    def "subject and badge can NOT share id"() {
        String sameId = "sameId"
        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        def badge = SkillsFactory.createBadge(1, 1)

        subj.subjectId = sameId
        badge.badgeId = sameId
        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        when:
        skillsService.createBadge(badge)
        then:
        SkillsClientException exception = thrown()
        // some db support case-insensitive constraints and some do not, that's life for you
        exception.message.contains("explanation:Provided badge id already exist") || exception.message.contains("explanation:Data Integrity Violation")
        exception.message.contains("errorCode:ConstraintViolation")
    }

    def "badge and skill can NOT share id"() {
        String sameId = "sameId"
        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        def skill = SkillsFactory.createSkill(1, 1)
        def badge = SkillsFactory.createBadge(1, 1)

        skill.skillId = sameId
        badge.badgeId = sameId
        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createBadge(badge)
        when:
        skillsService.createSkill(skill)
        then:
        SkillsClientException exception = thrown()
        // some db support case-insensitive constraints and some do not, that's life for you
        exception.message.contains("explanation:Provided skill id already exist") || exception.message.contains("explanation:Data Integrity Violation")
        exception.message.contains("errorCode:ConstraintViolation")
    }

    def "subject name exists does not fail on the null word"() {
        def proj = SkillsFactory.createProject(1)
        skillsService.createProject(proj)
        when:
        def exist = skillsService.subjectNameExists([projectId: proj.projectId, subjectName: "null"])

        then:
        exist == false
    }

    def "project name exists does not fail on the null word"() {
        when:
        def exist = skillsService.projectNameExists([projectName: "null"])
        then:
        exist == false
    }

    def "badge name exists does not fail on the null word"() {
        def proj = SkillsFactory.createProject(1)
        skillsService.createProject(proj)
        when:
        def exist = skillsService.badgeNameExists([projectId: proj.projectId, badgeName: "null"])
        then:
        exist == false
    }

    def "skill name exists does not fail on the null word"() {
        def proj = SkillsFactory.createProject(1)
        skillsService.createProject(proj)
        def subj = SkillsFactory.createSubject(1, 1)
        skillsService.createSubject(subj)
        when:
        def exist = skillsService.skillNameExists([projectId: proj.projectId, skillName: "null"])
        then:
        exist == false
    }

    def "quiz name exists does not fail on the null word"() {
        when:
        def exist = skillsService.quizNameExist("null")
        then:
        exist.body == false
    }

}