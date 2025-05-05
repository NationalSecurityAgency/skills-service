/**
 * Copyright 2024 SkillTree
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
package skills.intTests.copySubject

import groovy.json.JsonOutput
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import skills.intTests.copyProject.CopyIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsService
import skills.storage.model.Attachment
import skills.storage.model.QuizDefParent
import skills.storage.model.SkillDef
import skills.utils.GroovyToJavaByteUtils

import static skills.intTests.utils.SkillsFactory.*

class CopySubjectToAnotherProjSpecs extends CopyIntSpec {

    def "copy subject with 1 skill" () {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Subj1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Subj1Skills)

        def p2 = createProject(2)
        skillsService.createProject(p2)

        when:
        skillsService.copySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)

        def copiedSubject1 = skillsService.getSubject([subjectId: p1subj1.subjectId, projectId: p2.projectId])
        def copiedSubj1Skills = skillsService.getSkillsForSubject(p2.projectId, p1subj1.subjectId)
        println JsonOutput.prettyPrint(JsonOutput.toJson(copiedSubj1Skills))
        then:
        copiedSubject1.name == p1subj1.name
        copiedSubject1.subjectId == p1subj1.subjectId
        copiedSubject1.numGroups == 0
        copiedSubject1.numSkills == 1
        copiedSubject1.totalPoints == (100)
        copiedSubject1.numSkillsReused == 0
        copiedSubject1.totalPointsReused == 0

        copiedSubj1Skills.skillId == p1Subj1Skills.skillId
        copiedSubj1Skills.name == p1Subj1Skills.name
        copiedSubj1Skills[0].projectId == p2.projectId
        copiedSubj1Skills[0].type == "Skill"
        copiedSubj1Skills[0].pointIncrement == 100
        copiedSubj1Skills[0].numMaxOccurrencesIncrementInterval == 1
        copiedSubj1Skills[0].numPerformToCompletion == 1
        copiedSubj1Skills[0].expirationType == "NEVER"
    }

    def "copy subject with multiple skills of various attributes" () {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Subj1Skills = createSkills(3, 1, 1, 100)
        p1Subj1Skills[0].pointIncrement = 100
        p1Subj1Skills[0].numPerformToCompletion = 1
        p1Subj1Skills[0].pointIncrementInterval = 480
        p1Subj1Skills[0].numMaxOccurrencesIncrementInterval = 1
        p1Subj1Skills[0].description = 'first skill'
        p1Subj1Skills[0].helpUrl = 'https://first.com'

        p1Subj1Skills[1].pointIncrement = 200
        p1Subj1Skills[1].numPerformToCompletion = 4
        p1Subj1Skills[1].pointIncrementInterval = 880
        p1Subj1Skills[1].numMaxOccurrencesIncrementInterval = 2
        p1Subj1Skills[1].description = 'second skill'
        p1Subj1Skills[1].helpUrl = 'https://second.com'


        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 5, 2)
        skillsService.createQuizQuestionDefs(questions)

        p1Subj1Skills[2].pointIncrement = 300
        p1Subj1Skills[2].numPerformToCompletion = 1
        p1Subj1Skills[2].pointIncrementInterval = 500
        p1Subj1Skills[2].numMaxOccurrencesIncrementInterval = 1
        p1Subj1Skills[2].description = 'third skill'
        p1Subj1Skills[2].helpUrl = 'https://third.com'
        p1Subj1Skills[2].selfReportingType = SkillDef.SelfReportingType.Quiz
        p1Subj1Skills[2].quizId = quiz.quizId

        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Subj1Skills)

        def p2 = createProject(2)
        skillsService.createProject(p2)

        when:
        skillsService.copySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)

        def copiedSubject1 = skillsService.getSubject([subjectId: p1subj1.subjectId, projectId: p2.projectId])
        def copiedSubj1Skills = skillsService.getSkillsForSubject(p2.projectId, p1subj1.subjectId)
        println JsonOutput.prettyPrint(JsonOutput.toJson(copiedSubj1Skills))
        then:
        copiedSubject1.name == p1subj1.name
        copiedSubject1.subjectId == p1subj1.subjectId
        copiedSubject1.numGroups == 0
        copiedSubject1.numSkills == 3
        copiedSubject1.totalPoints == (100+(200*4)+300)
        copiedSubject1.numSkillsReused == 0
        copiedSubject1.totalPointsReused == 0

        copiedSubj1Skills.size() == 3
        copiedSubj1Skills.skillId == p1Subj1Skills.skillId
        copiedSubj1Skills.name == p1Subj1Skills.name
        copiedSubj1Skills[0].projectId == p2.projectId
        copiedSubj1Skills[0].type == "Skill"
        copiedSubj1Skills[0].pointIncrement == 100
        copiedSubj1Skills[0].numPerformToCompletion == 1
        copiedSubj1Skills[0].numMaxOccurrencesIncrementInterval == 1
        copiedSubj1Skills[0].pointIncrementInterval == 480
        copiedSubj1Skills[0].expirationType == "NEVER"
        copiedSubj1Skills[0].quizType == null
        copiedSubj1Skills[0].quizId == null
        copiedSubj1Skills[0].quizName == null

        copiedSubj1Skills[1].projectId == p2.projectId
        copiedSubj1Skills[1].type == "Skill"
        copiedSubj1Skills[1].pointIncrement == 200
        copiedSubj1Skills[1].numPerformToCompletion == 4
        copiedSubj1Skills[1].numMaxOccurrencesIncrementInterval == 2
        copiedSubj1Skills[1].pointIncrementInterval == 880
        copiedSubj1Skills[1].expirationType == "NEVER"
        copiedSubj1Skills[1].quizType == null
        copiedSubj1Skills[1].quizId == null
        copiedSubj1Skills[1].quizName == null

        copiedSubj1Skills[2].projectId == p2.projectId
        copiedSubj1Skills[2].type == "Skill"
        copiedSubj1Skills[2].pointIncrement == 300
        copiedSubj1Skills[2].numPerformToCompletion == 1
        copiedSubj1Skills[2].numMaxOccurrencesIncrementInterval == 1
        copiedSubj1Skills[2].pointIncrementInterval == 500
        copiedSubj1Skills[2].expirationType == "NEVER"
        copiedSubj1Skills[2].quizType == QuizDefParent.QuizType.Quiz.toString()
        copiedSubj1Skills[2].quizId == quiz.quizId
        copiedSubj1Skills[2].quizName == quiz.name
    }

    def "subject attributes are properly copied"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        p1subj1.description = "Very important Stuff"
        p1subj1.helpUrl = "http://www.greatlink.com"
        p1subj1.iconClass = "fas fa-address-card"
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [])

        def p2 = createProject(2)
        skillsService.createProject(p2)

        when:
        skillsService.copySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)

        def copiedSubjs = skillsService.getSubjects(p2.projectId)
        then:
        copiedSubjs.subjectId == [p1subj1.subjectId]
        def copiedSubj = copiedSubjs[0]
        copiedSubj.name == p1subj1.name
        copiedSubj.description == p1subj1.description
        copiedSubj.helpUrl == p1subj1.helpUrl
        copiedSubj.iconClass == p1subj1.iconClass
    }

    def "group attributes are properly copied"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def skill1 = createSkill(1, 1, 22, 0, 12, 512, 18,)
        def skill2 = createSkill(1, 1, 23, 0, 12, 512, 18,)

        def group1 = createSkillsGroup(1, 1, 4)
        group1.description = "blah 1"
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [group1])
        skillsService.assignSkillToSkillsGroup(group1.skillId, skill1)
        skillsService.assignSkillToSkillsGroup(group1.skillId, skill2)
        group1.numSkillsRequired = 1
        skillsService.createSkill(group1)

        def group2 = createSkillsGroup(1, 1, 5)
        group2.description = "something else"
        skillsService.createSkill(group2)

        def p2 = createProject(2)
        skillsService.createProject(p2)

        when:
        skillsService.copySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)

        def original1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: group1.skillId])
        def original2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: group2.skillId])

        def copied1 = skillsService.getSkill([projectId: p2.projectId, subjectId: p1subj1.subjectId, skillId: group1.skillId])
        def copied2 = skillsService.getSkill([projectId: p2.projectId, subjectId: p1subj1.subjectId, skillId: group2.skillId])

        then:
        original1.description == group1.description
        copied1.description == group1.description
        copied1.skillId == group1.skillId
        copied1.name == group1.name
        copied1.type == SkillDef.ContainerType.SkillsGroup.toString()
        copied1.totalPoints == (18 * 12) * 2
        copied1.numSkillsRequired == 1

        original2.description == group2.description
        copied2.description == group2.description
        copied2.skillId == group2.skillId
        copied2.name == group2.name
        copied2.type == SkillDef.ContainerType.SkillsGroup.toString()
        copied2.totalPoints == 0
        copied2.numSkillsRequired == -1
    }

    def "group with multiple skills and partial requirement of most skills is properly copied"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def skills = createSkills(10, 1, 1, 100)
        def group1 = createSkillsGroup(1, 1, 40)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [group1])
        skills.each {
            skillsService.assignSkillToSkillsGroup(group1.skillId, it)
        }
        group1.numSkillsRequired = 9
        skillsService.createSkill(group1)

        def p2 = createProject(2)
        skillsService.createProject(p2)

        when:
        skillsService.copySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)

        def original1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: group1.skillId])

        def copied1 = skillsService.getSkill([projectId: p2.projectId, subjectId: p1subj1.subjectId, skillId: group1.skillId])

        then:
        original1.description == group1.description
        copied1.description == group1.description
        copied1.skillId == group1.skillId
        copied1.name == group1.name
        copied1.type == SkillDef.ContainerType.SkillsGroup.toString()
        copied1.totalPoints == (10 * 100)
        copied1.numSkillsRequired == 9
    }

    def "custom icons are properly copied"() {
        def p1 = createProject(1)
        skillsService.createProject(p1)
        ClassPathResource resource = new ClassPathResource("/dot2.png")
        def file = resource.getFile()
        skillsService.uploadIcon([projectId:(p1.projectId)], file)

        def p1subj1 = createSubject(1, 1)
        p1subj1.iconClass = "${p1.projectId}-dot2png".toString()
        skillsService.createSubject(p1subj1)

        def p2 = createProject(2)
        skillsService.createProject(p2)

        when:
        skillsService.copySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)

        def originalIcons = skillsService.getIconCssForProject(p1)
        def copiedIcons = skillsService.getIconCssForProject(p2)

        def copiedSubjs = skillsService.getSubjects(p2.projectId)
        then:
        originalIcons[0].filename == "dot2.png"
        originalIcons[0].cssClassname == "TestProject1-dot2png"
        copiedIcons[0].filename == "dot2.png"
        copiedIcons[0].cssClassname == "TestProject2-dot2png"

        copiedSubjs[0].iconClass == "${p2.projectId}-dot2png".toString()
    }

    def "copied subject is added last in the order"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [])

        def p2 = createProject(2)
        skillsService.createProject(p2)
        def p2subj2 = createSubject(2, 2)
        def p2subj3 = createSubject(2, 3)
        skillsService.createSubject(p2subj2)
        skillsService.createSubject(p2subj3)

        when:
        skillsService.copySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)

        def copiedSubjs = skillsService.getSubjects(p2.projectId).sort { it.displayOrder }
        then:
        copiedSubjs.subjectId == [p2subj2.subjectId, p2subj3.subjectId, p1subj1.subjectId]
        copiedSubjs.displayOrder == [1, 2, 3]
    }

    def "copy a subject that has attachments in skill's description - attachments should be copied"() {
        def p1 = createProject(1)
        skillsService.createProject(p1)

        def p1subj1 = createSubject(1, 1)
        skillsService.createSubject(p1subj1)

        String filename = 'test-pdf.pdf'
        String contents = 'Test is a test'
        Resource resource = GroovyToJavaByteUtils.toByteArrayResource(contents, filename)
        def result = skillsService.uploadAttachment(resource, p1.projectId, null, null)
        String attachmentHref = result.href

        def p1Skills = createSkills(2, 1, 1, 100)
        p1Skills[0].description = "Here is a [Link](${attachmentHref})".toString()
        skillsService.createSkills(p1Skills)

        def p2 = createProject(2)
        skillsService.createProject(p2)

        when:
        skillsService.copySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)

        def origProjSkill = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        def copyProjSkill = skillsService.getSkill([projectId: p2.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])

        List<Attachment> attachments = attachmentRepo.findAll()
        then:
        origProjSkill.description == "Here is a [Link](${attachmentHref})"

        attachments.size() == 2
        Attachment originalAttachment = attachments.find {  attachmentHref.contains(it.uuid)}
        Attachment newAttachment = attachments.find { !attachmentHref.contains(it.uuid)}

        originalAttachment.projectId == p1.projectId

        newAttachment.projectId == p2.projectId
        copyProjSkill.description == "Here is a [Link](/api/download/${newAttachment.uuid})"

        SkillsService.FileAndHeaders fileAndHeaders = skillsService.downloadAttachment("/api/download/${newAttachment.uuid}")
        File file = fileAndHeaders.file
        file
        file.bytes == contents.getBytes()
    }

    def "copy a subject that has attachments in multiple skills' description - attachments should be copied"() {
        def p1 = createProject(1)
        skillsService.createProject(p1)

        def p1subj1 = createSubject(1, 1)
        skillsService.createSubject(p1subj1)

        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)

        def attachment1Href = attachFileAndReturnHref(p1.projectId)
        def attachment2Href = attachFileAndReturnHref(p1.projectId)
        def attachment3Href = attachFileAndReturnHref(p1.projectId)
        def attachment4Href = attachFileAndReturnHref(p1.projectId)

        def p1Skills = createSkills(2, 1, 1, 100)
        p1Skills[0].description = "Here is a [Link](${attachment1Href})".toString()
        p1Skills[1].description = "Here is a [Link](${attachment2Href})".toString()
        skillsService.createSkills(p1Skills)

        def p1Subj2Skills = createSkills(2, 1, 2, 100)
        p1Subj2Skills[0].description = "Here is a [Link](${attachment3Href})".toString()
        p1Subj2Skills[1].description = "Here is a [Link](${attachment4Href})".toString()
        skillsService.createSkills(p1Subj2Skills)

        def p2 = createProject(2)
        skillsService.createProject(p2)

        when:
        skillsService.copySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)
        skillsService.copySubjectDefIntoAnotherProject(p1.projectId, p1subj2.subjectId, p2.projectId)

        def origProjSkill1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        def origProjSkill2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[1].skillId])
        def origProjSkill3 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj2.subjectId, skillId: p1Subj2Skills[0].skillId])
        def origProjSkill4 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj2.subjectId, skillId: p1Subj2Skills[1].skillId])

        def copyProjSkill1 = skillsService.getSkill([projectId: p2.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        def copyProjSkill2 = skillsService.getSkill([projectId: p2.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[1].skillId])
        def copyProjSkill3 = skillsService.getSkill([projectId: p2.projectId, subjectId: p1subj2.subjectId, skillId: p1Subj2Skills[0].skillId])
        def copyProjSkill4 = skillsService.getSkill([projectId: p2.projectId, subjectId: p1subj2.subjectId, skillId: p1Subj2Skills[1].skillId])

        List<Attachment> attachments = attachmentRepo.findAll()
        then:
        origProjSkill1.description == "Here is a [Link](${attachment1Href})"
        origProjSkill2.description == "Here is a [Link](${attachment2Href})"
        origProjSkill3.description == "Here is a [Link](${attachment3Href})"
        origProjSkill4.description == "Here is a [Link](${attachment4Href})"

        attachments.size() == 8
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        Attachment originalAttachment2 = attachments.find {  attachment2Href.contains(it.uuid)}
        Attachment originalAttachment3 = attachments.find {  attachment3Href.contains(it.uuid)}
        Attachment originalAttachment4 = attachments.find {  attachment4Href.contains(it.uuid)}
        originalAttachment1.projectId == p1.projectId
        originalAttachment2.projectId == p1.projectId
        originalAttachment3.projectId == p1.projectId
        originalAttachment4.projectId == p1.projectId

        List<Attachment> newAttachments = attachments.findAll {
            !attachment1Href.contains(it.uuid) && !attachment2Href.contains(it.uuid) && !attachment3Href.contains(it.uuid) && !attachment4Href.contains(it.uuid)
        }

        List<String> copiedDescriptions = newAttachments.collect( {"Here is a [Link](/api/download/${it.uuid})".toString() })
        copiedDescriptions.contains(copyProjSkill1.description)
        copiedDescriptions.contains(copyProjSkill2.description)
        copiedDescriptions.contains(copyProjSkill3.description)
        copiedDescriptions.contains(copyProjSkill4.description)

        newAttachments.each {
            assert it.projectId == p2.projectId
        }
    }

    def "copy a subject with attachments in its description"() {
        def p1 = createProject(1)
        skillsService.createProject(p1)

        String contents = 'Test is a test'
        String attachmentHref = attachFileAndReturnHref(p1.projectId, contents)

        def p1subj1 = createSubject(1, 1)
        p1subj1.description = "Here is a [Link](${attachmentHref})".toString()
        skillsService.createSubject(p1subj1)

        def p2 = createProject(2)
        skillsService.createProject(p2)

        when:
        skillsService.copySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)

        def origSubj = skillsService.getSubject([projectId: p1.projectId, subjectId: p1subj1.subjectId])
        def copySubj = skillsService.getSubject([projectId: p2.projectId, subjectId: p1subj1.subjectId])

        List<Attachment> attachments = attachmentRepo.findAll()
        then:
        origSubj.description == "Here is a [Link](${attachmentHref})"

        attachments.size() == 2
        Attachment originalAttachment = attachments.find {  attachmentHref.contains(it.uuid)}
        Attachment newAttachment = attachments.find { !attachmentHref.contains(it.uuid)}

        originalAttachment.projectId == p1.projectId

        newAttachment.projectId == p2.projectId
        copySubj.description == "Here is a [Link](/api/download/${newAttachment.uuid})"

        SkillsService.FileAndHeaders fileAndHeaders = skillsService.downloadAttachment("/api/download/${newAttachment.uuid}")
        File file = fileAndHeaders.file
        file
        file.bytes == contents.getBytes()
    }

    def "copy a subject with attachments in its description then remove the subject and then copy again"() {
        def p1 = createProject(1)
        skillsService.createProject(p1)

        String contents = 'Test is a test'
        String attachmentHref = attachFileAndReturnHref(p1.projectId, contents)

        def p1subj1 = createSubject(1, 1)
        p1subj1.description = "Here is a [Link](${attachmentHref})".toString()
        skillsService.createSubject(p1subj1)

        def p2 = createProject(2)
        skillsService.createProject(p2)

        when:
        skillsService.copySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)

        def origSubj = skillsService.getSubject([projectId: p1.projectId, subjectId: p1subj1.subjectId])
        def copySubj = skillsService.getSubject([projectId: p2.projectId, subjectId: p1subj1.subjectId])

        Iterable<Attachment> attachments = attachmentRepo.findAll()
        Attachment originalAttachment = attachments.toList().find {  attachmentHref.contains(it.uuid)}
        Attachment newAttachment = attachments.toList().find { !attachmentHref.contains(it.uuid)}
        SkillsService.FileAndHeaders fileAndHeaders = skillsService.downloadAttachment("/api/download/${newAttachment.uuid}")

        skillsService.deleteSubject([projectId: p2.projectId, subjectId: p1subj1.subjectId])

        skillsService.copySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)

        def copy2Subj = skillsService.getSubject([projectId: p2.projectId, subjectId: p1subj1.subjectId])
        Iterable<Attachment> attachments2 = attachmentRepo.findAll()
        Attachment originalAttachment2 = attachments2.toList().find {  attachmentHref.contains(it.uuid)}
        Attachment newAttachment2 = attachments2.toList().find { !attachmentHref.contains(it.uuid)}
        SkillsService.FileAndHeaders fileAndHeaders2 = skillsService.downloadAttachment("/api/download/${newAttachment2.uuid}")

        then:
        origSubj.description == "Here is a [Link](${attachmentHref})"
        originalAttachment.projectId == p1.projectId

        attachments.size() == 2
        newAttachment.projectId == p2.projectId
        copySubj.description == "Here is a [Link](/api/download/${newAttachment.uuid})"
        File file = fileAndHeaders.file
        file
        file.bytes == contents.getBytes()

        // second round
        originalAttachment2.projectId == p1.projectId
        attachments2.size() == 2
        newAttachment2.projectId == p2.projectId
        copy2Subj.description == "Here is a [Link](/api/download/${newAttachment2.uuid})"
        File file2 = fileAndHeaders2.file
        file2
        file2.bytes == contents.getBytes()
    }

    def "copy subject with a disabled skill" () {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Subj1Skills = createSkills(2, 1, 1, 100)
        p1Subj1Skills[0].enabled = false
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Subj1Skills)

        def p2 = createProject(2)
        skillsService.createProject(p2)

        when:
        skillsService.copySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)

        def copiedSubject1 = skillsService.getSubject([subjectId: p1subj1.subjectId, projectId: p2.projectId])
        def copiedSubj1Skills = skillsService.getSkillsForSubject(p2.projectId, p1subj1.subjectId)
        then:
        copiedSubject1.name == p1subj1.name
        copiedSubject1.subjectId == p1subj1.subjectId
        copiedSubject1.numGroups == 0
        copiedSubject1.numSkills == 1
        copiedSubject1.numSkillsDisabled == 1
        copiedSubject1.totalPoints == (100)
        copiedSubject1.numSkillsReused == 0
        copiedSubject1.totalPointsReused == 0

        copiedSubj1Skills[0].skillId == p1Subj1Skills[0].skillId
        copiedSubj1Skills[0].name == p1Subj1Skills[0].name
        copiedSubj1Skills[0].projectId == p2.projectId
        copiedSubj1Skills[0].type == "Skill"
        copiedSubj1Skills[0].pointIncrement == 100
        copiedSubj1Skills[0].numMaxOccurrencesIncrementInterval == 1
        copiedSubj1Skills[0].numPerformToCompletion == 1
        copiedSubj1Skills[0].expirationType == "NEVER"

        copiedSubj1Skills[1].skillId == p1Subj1Skills[1].skillId
        copiedSubj1Skills[1].name == p1Subj1Skills[1].name
        copiedSubj1Skills[1].projectId == p2.projectId
        copiedSubj1Skills[1].type == "Skill"
        copiedSubj1Skills[1].pointIncrement == 100
        copiedSubj1Skills[1].numMaxOccurrencesIncrementInterval == 1
        copiedSubj1Skills[1].numPerformToCompletion == 1
        copiedSubj1Skills[1].expirationType == "NEVER"
    }

}
