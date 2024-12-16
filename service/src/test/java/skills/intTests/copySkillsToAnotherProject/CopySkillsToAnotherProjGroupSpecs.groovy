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
package skills.intTests.copySkillsToAnotherProject


import org.springframework.core.io.Resource
import skills.intTests.copyProject.CopyIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsService
import skills.storage.model.Attachment
import skills.storage.model.QuizDefParent
import skills.storage.model.SkillDef
import skills.utils.GroovyToJavaByteUtils

import static skills.intTests.utils.SkillsFactory.*

class CopySkillsToAnotherProjGroupSpecs extends CopyIntSpec {

    def "copy 1 skill" () {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Subj1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Subj1Skills)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 2)
        def destGroup = createSkillsGroup(2, 2, 4)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [destGroup])

        when:
        skillsService.copySkillDefsIntoAnotherProjectSkillGroup(p1.projectId, p1Subj1Skills.collect { it.skillId as String }, p2.projectId, p2subj1.subjectId, destGroup.skillId)

        def copiedSkills = skillsService.getSkillsForGroup(p2.projectId, destGroup.skillId)
        then:
        copiedSkills.size() == 1
        copiedSkills[0].projectId == p2.projectId
        copiedSkills[0].type == "Skill"
        copiedSkills[0].pointIncrement == 100
        copiedSkills[0].numMaxOccurrencesIncrementInterval == 1
        copiedSkills[0].numPerformToCompletion == 1
        copiedSkills[0].expirationType == "NEVER"
    }

    def "copy skills with various attributes" () {
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
        def p2subj1 = createSubject(2, 2)
        def destGroup = createSkillsGroup(2, 2, 4)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [destGroup])

        when:
        skillsService.copySkillDefsIntoAnotherProjectSkillGroup(p1.projectId, p1Subj1Skills.collect { it.skillId as String }, p2.projectId, p2subj1.subjectId, destGroup.skillId)

        def copiedSubj1Skills = skillsService.getSkillsForGroup(p2.projectId, destGroup.skillId)
        then:

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

    def "skills from a group to subject are copied"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def skill1 = createSkill(1, 1, 22, 0, 12, 512, 18,)
        def skill2 = createSkill(1, 1, 23, 0, 12, 512, 18,)

        def group1 = createSkillsGroup(1, 1, 4)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [group1])
        skillsService.assignSkillToSkillsGroup(group1.skillId, skill1)
        skillsService.createSkill(group1)

        def group2 = createSkillsGroup(1, 1, 5)
        skillsService.createSkill(group2)
        skillsService.assignSkillToSkillsGroup(group2.skillId, skill2)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 2)
        def destGroup = createSkillsGroup(2, 2, 6)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [destGroup])

        when:
        skillsService.copySkillDefsIntoAnotherProjectSkillGroup(p1.projectId, [skill1.skillId, skill2.skillId], p2.projectId, p2subj1.subjectId, destGroup.skillId)

        def original1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: skill1.skillId])
        def original2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: skill2.skillId])

        def copiedSkills = skillsService.getSkillsForGroup(p2.projectId, destGroup.skillId)

        then:
        copiedSkills.size() == 2
        def skill1Res = copiedSkills.find { it.skillId == original1.skillId }
        skill1Res.skillId == original1.skillId
        skill1Res.name == original1.name
        skill1Res.type == original1.type
        skill1Res.totalPoints == original1.totalPoints

        def skill2Res = copiedSkills.find { it.skillId == original2.skillId }
        skill2Res.skillId == original2.skillId
        skill2Res.name == original2.name
        skill2Res.type == original2.type
        skill2Res.totalPoints == original2.totalPoints
    }

    def "copy skills from a subject to another project's empty group"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Subj1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Subj1Skills)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 2)
        def group1 = createSkillsGroup(2, 2, 4)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [group1])

        when:
        skillsService.copySkillDefsIntoAnotherProjectSkillGroup(p1.projectId, p1Subj1Skills.collect { it.skillId as String }, p2.projectId, p2subj1.subjectId, group1.skillId)

        def original1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Subj1Skills[0].skillId])
        def original2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Subj1Skills[1].skillId])
        def original3 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Subj1Skills[2].skillId])

        def destGroupSkills = skillsService.getSkillsForGroup(p2.projectId, group1.skillId)

        then:
        destGroupSkills.size() == 3
        def skill1Res = destGroupSkills.find { it.skillId == original1.skillId }
        skill1Res.skillId == original1.skillId
        skill1Res.name == original1.name
        skill1Res.type == original1.type
        skill1Res.totalPoints == original1.totalPoints

        def skill2Res = destGroupSkills.find { it.skillId == original2.skillId }
        skill2Res.skillId == original2.skillId
        skill2Res.name == original2.name
        skill2Res.type == original2.type
        skill2Res.totalPoints == original2.totalPoints

        def skill3Res = destGroupSkills.find { it.skillId == original3.skillId }
        skill3Res.skillId == original3.skillId
        skill3Res.name == original3.name
        skill3Res.type == original3.type
        skill3Res.totalPoints == original3.totalPoints
    }

    def "copy skills from a subject to another project's group with skill"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Subj1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Subj1Skills)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 2)
        def group1 = createSkillsGroup(2, 2, 4)
        def group2 = createSkillsGroup(2, 2, 5)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [group1, group2])
        def p2Subj1Skills = createSkills(3, 2, 2, 100)
        skillsService.assignSkillToSkillsGroup(group1.skillId, p2Subj1Skills[0])
        skillsService.assignSkillToSkillsGroup(group1.skillId, p2Subj1Skills[1])
        skillsService.assignSkillToSkillsGroup(group2.skillId, p2Subj1Skills[2])

        when:
        skillsService.copySkillDefsIntoAnotherProjectSkillGroup(p1.projectId, p1Subj1Skills.collect { it.skillId as String }, p2.projectId, p2subj1.subjectId, group1.skillId)

        def original1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Subj1Skills[0].skillId])
        def original2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Subj1Skills[1].skillId])
        def original3 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Subj1Skills[2].skillId])

        def destGroupSkills = skillsService.getSkillsForGroup(p2.projectId, group1.skillId)

        then:
        destGroupSkills.size() == 5
        destGroupSkills.skillId.sort() == [original1.skillId, original2.skillId, original3.skillId, p2Subj1Skills[0].skillId, p2Subj1Skills[1].skillId].sort()
        def skill1Res = destGroupSkills.find { it.skillId == original1.skillId }
        skill1Res.skillId == original1.skillId
        skill1Res.name == original1.name
        skill1Res.type == original1.type
        skill1Res.totalPoints == original1.totalPoints

        def skill2Res = destGroupSkills.find { it.skillId == original2.skillId }
        skill2Res.skillId == original2.skillId
        skill2Res.name == original2.name
        skill2Res.type == original2.type
        skill2Res.totalPoints == original2.totalPoints

        def skill3Res = destGroupSkills.find { it.skillId == original3.skillId }
        skill3Res.skillId == original3.skillId
        skill3Res.name == original3.name
        skill3Res.type == original3.type
        skill3Res.totalPoints == original3.totalPoints
    }

    def "when copying skills to a skill group destination subject id is optional"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Subj1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Subj1Skills)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 2)
        def group1 = createSkillsGroup(2, 2, 4)
        def group2 = createSkillsGroup(2, 2, 5)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [group1, group2])
        def p2Subj1Skills = createSkills(3, 2, 2, 100)
        skillsService.assignSkillToSkillsGroup(group1.skillId, p2Subj1Skills[0])
        skillsService.assignSkillToSkillsGroup(group1.skillId, p2Subj1Skills[1])
        skillsService.assignSkillToSkillsGroup(group2.skillId, p2Subj1Skills[2])

        when:
        skillsService.copySkillDefsIntoAnotherProjectSkillGroup(p1.projectId, p1Subj1Skills.collect { it.skillId as String }, p2.projectId, null, group1.skillId)

        def original1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Subj1Skills[0].skillId])
        def original2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Subj1Skills[1].skillId])
        def original3 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Subj1Skills[2].skillId])

        def destGroupSkills = skillsService.getSkillsForGroup(p2.projectId, group1.skillId)

        then:
        destGroupSkills.size() == 5
        destGroupSkills.skillId.sort() == [original1.skillId, original2.skillId, original3.skillId, p2Subj1Skills[0].skillId, p2Subj1Skills[1].skillId].sort()
        def skill1Res = destGroupSkills.find { it.skillId == original1.skillId }
        skill1Res.skillId == original1.skillId
        skill1Res.name == original1.name
        skill1Res.type == original1.type
        skill1Res.totalPoints == original1.totalPoints

        def skill2Res = destGroupSkills.find { it.skillId == original2.skillId }
        skill2Res.skillId == original2.skillId
        skill2Res.name == original2.name
        skill2Res.type == original2.type
        skill2Res.totalPoints == original2.totalPoints

        def skill3Res = destGroupSkills.find { it.skillId == original3.skillId }
        skill3Res.skillId == original3.skillId
        skill3Res.name == original3.name
        skill3Res.type == original3.type
        skill3Res.totalPoints == original3.totalPoints
    }

    def "copy skills from a group to another project's empty group"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def origGroup = createSkillsGroup(1, 1, 10)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [origGroup])
        def p1Subj1Skills = createSkills(3, 1, 1, 100)
        skillsService.assignSkillToSkillsGroup(origGroup.skillId, p1Subj1Skills[0])
        skillsService.assignSkillToSkillsGroup(origGroup.skillId, p1Subj1Skills[1])
        skillsService.assignSkillToSkillsGroup(origGroup.skillId, p1Subj1Skills[2])

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 2)
        def destGroup = createSkillsGroup(2, 2, 4)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [destGroup])

        when:
        skillsService.copySkillDefsIntoAnotherProjectSkillGroup(p1.projectId, p1Subj1Skills.collect { it.skillId as String }, p2.projectId, p2subj1.subjectId, destGroup.skillId)

        def original1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Subj1Skills[0].skillId])
        def original2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Subj1Skills[1].skillId])
        def original3 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Subj1Skills[2].skillId])

        def destGroupSkills = skillsService.getSkillsForGroup(p2.projectId, destGroup.skillId)

        then:
        destGroupSkills.size() == 3
        def skill1Res = destGroupSkills.find { it.skillId == original1.skillId }
        skill1Res.skillId == original1.skillId
        skill1Res.name == original1.name
        skill1Res.type == original1.type
        skill1Res.totalPoints == original1.totalPoints

        def skill2Res = destGroupSkills.find { it.skillId == original2.skillId }
        skill2Res.skillId == original2.skillId
        skill2Res.name == original2.name
        skill2Res.type == original2.type
        skill2Res.totalPoints == original2.totalPoints

        def skill3Res = destGroupSkills.find { it.skillId == original3.skillId }
        skill3Res.skillId == original3.skillId
        skill3Res.name == original3.name
        skill3Res.type == original3.type
        skill3Res.totalPoints == original3.totalPoints
    }

    def "copy skills with attachments in skill's description - attachments should be copied"() {
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
        def p2subj1 = createSubject(2, 2)
        def destGroup = createSkillsGroup(2, 2, 4)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [destGroup])

        when:
        skillsService.copySkillDefsIntoAnotherProjectSkillGroup(p1.projectId, p1Skills.collect { it.skillId as String }, p2.projectId, p2subj1.subjectId, destGroup.skillId)

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

    def "copy skills with attachments in multiple skills' description - attachments should be copied"() {
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
        def p2subj1 = createSubject(2, 2)
        def destGroup = createSkillsGroup(2, 2, 4)
        def destGroup1 = createSkillsGroup(2, 2, 5)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [destGroup, destGroup1])

        when:
        skillsService.copySkillDefsIntoAnotherProjectSkillGroup(p1.projectId, p1Skills.collect { it.skillId as String }, p2.projectId, p2subj1.subjectId, destGroup.skillId)
        skillsService.copySkillDefsIntoAnotherProjectSkillGroup(p1.projectId, p1Subj2Skills.collect { it.skillId as String }, p2.projectId, p2subj1.subjectId, destGroup1.skillId)

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


}
