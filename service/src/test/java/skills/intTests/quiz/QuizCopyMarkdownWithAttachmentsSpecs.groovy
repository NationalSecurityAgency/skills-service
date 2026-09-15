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
package skills.intTests.quiz

import org.springframework.http.HttpStatus
import skills.intTests.copyProject.CopyIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.services.quiz.QuizQuestionType
import skills.storage.model.Attachment

import static skills.intTests.utils.SkillsFactory.*

class QuizCopyMarkdownWithAttachmentsSpecs extends CopyIntSpec {

    def "new quiz creation does not allow markdown with attachments"() {
        def quiz = QuizDefFactory.createQuiz(1)
        quiz.description = "Here is a [Link](/api/download/8ab81f77-3484-4f5a-ae58-ae4e7143b449)"

        when:
        skillsService.createQuizDef(quiz)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.BAD_REQUEST
        e.message.contains("Attachments in the description are not allowed when creating a new quiz")
    }

    def "paste markdown with attachment to another quiz: by editing a quiz"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)

        def attachment1Href = attachFileForQuizAndReturnHref(quiz.quizId)
        quiz.description =  "Here is a [Link](${attachment1Href})".toString()
        skillsService.createQuizDef(quiz, quiz.quizId)

        def quiz2 = QuizDefFactory.createQuiz(2)
        skillsService.createQuizDef(quiz2)

        when:
        quiz2.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.createQuizDef(quiz2, quiz2.quizId)

        def quiz1Res = skillsService.getQuizDef(quiz.quizId)
        def quiz2Res = skillsService.getQuizDef(quiz2.quizId)
        List<Attachment> attachments = attachmentRepo.findAll()

        // should not create new attachments
        quiz2.description = quiz2Res.description
        skillsService.createQuizDef(quiz2, quiz2.quizId)
        skillsService.createQuizDef(quiz2, quiz2.quizId)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        quiz1Res.description == "Here is a [Link](${attachment1Href})"

        attachments.size() == 2
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        originalAttachment1.quizId == quiz.quizId
        !originalAttachment1.projectId
        !originalAttachment1.skillId

        List<Attachment> newAttachments = attachments.findAll {!attachment1Href.contains(it.uuid) }

        newAttachments.size() == 1
        quiz2.description == "Here is a [Link](/api/download/${newAttachments[0].uuid})".toString()
        newAttachments[0].quizId == quiz2.quizId
        !newAttachments[0].projectId
        !newAttachments[0].skillId

        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "paste markdown with attachment to another quiz: by editing a quiz and changing quizId at the same time"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)

        def attachment1Href = attachFileForQuizAndReturnHref(quiz.quizId)
        quiz.description =  "Here is a [Link](${attachment1Href})".toString()
        skillsService.createQuizDef(quiz, quiz.quizId)

        def quiz2 = QuizDefFactory.createQuiz(2)
        skillsService.createQuizDef(quiz2)

        when:
        quiz2.description = "Here is a [Link](${attachment1Href})".toString()
        String origQuizId = quiz2.quizId
        quiz2.quizId = "newQuizId"
        skillsService.createQuizDef(quiz2, origQuizId)

        def quiz1Res = skillsService.getQuizDef(quiz.quizId)
        def quiz2Res = skillsService.getQuizDef(quiz2.quizId)
        List<Attachment> attachments = attachmentRepo.findAll()

        // should not create new attachments
        quiz2.description = quiz2Res.description
        skillsService.createQuizDef(quiz2, quiz2.quizId)
        skillsService.createQuizDef(quiz2, quiz2.quizId)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        quiz1Res.description == "Here is a [Link](${attachment1Href})"

        attachments.size() == 2
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        originalAttachment1.quizId == quiz.quizId
        !originalAttachment1.projectId
        !originalAttachment1.skillId

        List<Attachment> newAttachments = attachments.findAll {!attachment1Href.contains(it.uuid) }

        newAttachments.size() == 1
        quiz2.description == "Here is a [Link](/api/download/${newAttachments[0].uuid})".toString()
        newAttachments[0].quizId == quiz2.quizId
        !newAttachments[0].projectId
        !newAttachments[0].skillId

        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "paste attachment from question to quiz: by editing a quiz"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)

        def attachment1Href = attachFileForQuizAndReturnHref(quiz.quizId)

        def question = QuizDefFactory.createChoiceQuestion(1, 1, 5, QuizQuestionType.MultipleChoice)
        question.question =  "Here is a [Link](${attachment1Href})".toString()
        skillsService.createQuizQuestionDef(question)

        when:
        quiz.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.createQuizDef(quiz, quiz.quizId)

        def questionsRes = skillsService.getQuizQuestionDefs(quiz.quizId)
        def quizRes = skillsService.getQuizDef(quiz.quizId)
        List<Attachment> attachments = attachmentRepo.findAll()

        then:
        questionsRes.questions[0].question == "Here is a [Link](${attachment1Href})"

        attachments.size() == 2
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        originalAttachment1.quizId == quiz.quizId
        !originalAttachment1.projectId
        !originalAttachment1.skillId

        List<Attachment> newAttachments = attachments.findAll {!attachment1Href.contains(it.uuid) }

        newAttachments.size() == 1
        quizRes.description == "Here is a [Link](/api/download/${newAttachments[0].uuid})".toString()
        newAttachments[0].quizId == quiz.quizId
        !newAttachments[0].projectId
        !newAttachments[0].skillId
    }

    def "paste attachment from answer to quiz: by editing a quiz"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)

        def attachment1Href = attachFileForQuizAndReturnHref(quiz.quizId)

        def question = QuizDefFactory.createTextInputQuestion(1, 1)
        skillsService.createQuizQuestionDef(question)

        def quizAttempt = skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [isSelected: true, answerText:  "Here is a [Link](${attachment1Href})".toString()])

        when:
        quiz.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.createQuizDef(quiz, quiz.quizId)

        def questionsRes = skillsService.getQuizAttemptResult(quiz.quizId, quizAttempt.id)
        def quizRes = skillsService.getQuizDef(quiz.quizId)
        List<Attachment> attachments = attachmentRepo.findAll()

        then:
        questionsRes.questions[0].answers[0].answer == "Here is a [Link](${attachment1Href})"

        attachments.size() == 2
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        originalAttachment1.quizId == quiz.quizId
        !originalAttachment1.projectId
        !originalAttachment1.skillId

        List<Attachment> newAttachments = attachments.findAll {!attachment1Href.contains(it.uuid) }

        newAttachments.size() == 1
        quizRes.description == "Here is a [Link](/api/download/${newAttachments[0].uuid})".toString()
        newAttachments[0].quizId == quiz.quizId
        !newAttachments[0].projectId
        !newAttachments[0].skillId
    }

    def "paste attachment from answer to quiz: by editing a quiz and editing quizId at the same time"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)

        def attachment1Href = attachFileForQuizAndReturnHref(quiz.quizId)

        def question = QuizDefFactory.createTextInputQuestion(1, 1)
        skillsService.createQuizQuestionDef(question)

        def quizAttempt = skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [isSelected: true, answerText:  "Here is a [Link](${attachment1Href})".toString()])

        when:
        quiz.description = "Here is a [Link](${attachment1Href})".toString()
        String origQuizId = quiz.quizId
        quiz.quizId = 'newId'
        skillsService.createQuizDef(quiz, origQuizId)

        def questionsRes = skillsService.getQuizAttemptResult(quiz.quizId, quizAttempt.id)
        def quizRes = skillsService.getQuizDef(quiz.quizId)
        List<Attachment> attachments = attachmentRepo.findAll()

        then:
        questionsRes.questions[0].answers[0].answer == "Here is a [Link](${attachment1Href})"

        attachments.size() == 2
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        originalAttachment1.quizId == quiz.quizId
        !originalAttachment1.projectId
        !originalAttachment1.skillId

        List<Attachment> newAttachments = attachments.findAll {!attachment1Href.contains(it.uuid) }

        newAttachments.size() == 1
        quizRes.description == "Here is a [Link](/api/download/${newAttachments[0].uuid})".toString()
        newAttachments[0].quizId == quiz.quizId
        !newAttachments[0].projectId
        !newAttachments[0].skillId
    }

    def "editing quiz's id that has markdown with attachments must not duplicate attachments"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)

        def attachment1Href = attachFileForQuizAndReturnHref(quiz.quizId)
        quiz.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.createQuizDef(quiz, quiz.quizId)

        when:
        def quizRes = skillsService.getQuizDef(quiz.quizId)
        List<Attachment> attachments = attachmentRepo.findAll()

        String origQuizId = quiz.quizId
        quiz.quizId = 'newId'
        skillsService.createQuizDef(quiz, origQuizId)

        def quizResAfter = skillsService.getQuizDef(quiz.quizId)
        List<Attachment> attachmentsAfter = attachmentRepo.findAll()

        then:
        quizRes.description == "Here is a [Link](${attachment1Href})"
        quizResAfter.description == "Here is a [Link](${attachment1Href})"

        attachments.size() == 1
        Attachment originalAttachment1 = attachmentsAfter.find {  attachment1Href.contains(it.uuid)}
        originalAttachment1.quizId == quiz.quizId
        !originalAttachment1.projectId
        !originalAttachment1.skillId

        List<Attachment> newAttachments = attachmentsAfter.findAll {!attachment1Href.contains(it.uuid) }
        !newAttachments

        attachments.uuid == attachmentsAfter.uuid
        attachments.quizId == [origQuizId]
        attachmentsAfter.quizId == [quiz.quizId]
        !attachments[0].skillId
        !attachments[0].projectId
        !attachmentsAfter[0].skillId
        !attachmentsAfter[0].projectId
    }

    def "paste attachment from project: skill -> quiz"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def attachment1Href = attachFileAndReturnHref(p1.projectId)

        def p1Skills = createSkills(2, 1, 1, 100)
        p1Skills[0].description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.createSkills(p1Skills)

        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)

        when:
        quiz.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.createQuizDef(quiz, quiz.quizId)

        def skillRes = skillsService.getSkill(p1Skills[0])
        def quizRes = skillsService.getQuizDef(quiz.quizId)
        List<Attachment> attachments = attachmentRepo.findAll()

        then:
        skillRes.description == "Here is a [Link](${attachment1Href})"

        attachments.size() == 2
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        !originalAttachment1.quizId
        originalAttachment1.projectId == p1.projectId
        originalAttachment1.skillId == p1Skills[0].skillId

        List<Attachment> newAttachments = attachments.findAll {!attachment1Href.contains(it.uuid) }

        newAttachments.size() == 1
        quizRes.description == "Here is a [Link](/api/download/${newAttachments[0].uuid})".toString()
        newAttachments[0].quizId == quiz.quizId
        !newAttachments[0].projectId
        !newAttachments[0].skillId
    }

    def "paste attachment from project: project -> quiz"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def attachment1Href = attachFileAndReturnHref(p1.projectId)

        p1.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.updateProject(p1)

        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)

        when:
        quiz.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.createQuizDef(quiz, quiz.quizId)

        def projRes = skillsService.getProjectDescription(p1.projectId)
        def quizRes = skillsService.getQuizDef(quiz.quizId)
        List<Attachment> attachments = attachmentRepo.findAll()

        then:
        projRes.description == "Here is a [Link](${attachment1Href})"

        attachments.size() == 2
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        !originalAttachment1.quizId
        originalAttachment1.projectId == p1.projectId
        !originalAttachment1.skillId

        List<Attachment> newAttachments = attachments.findAll {!attachment1Href.contains(it.uuid) }

        newAttachments.size() == 1
        quizRes.description == "Here is a [Link](/api/download/${newAttachments[0].uuid})".toString()
        newAttachments[0].quizId == quiz.quizId
        !newAttachments[0].projectId
        !newAttachments[0].skillId
    }

    def "paste attachment from Global Badge: gb -> quiz"() {
        def badge = createBadge(1, 1)
        skillsService.createGlobalBadge(badge)

        def attachment1Href = attachFileForGlobalBadgeAndReturnHref(badge.badgeId)

        badge.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.updateGlobalBadge(badge)

        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)

        when:
        quiz.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.createQuizDef(quiz, quiz.quizId)

        def badgeRes = skillsService.getGlobalBadge(badge.badgeId)
        def quizRes = skillsService.getQuizDef(quiz.quizId)
        List<Attachment> attachments = attachmentRepo.findAll()

        then:
        badgeRes.description == "Here is a [Link](${attachment1Href})"

        attachments.size() == 2
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        !originalAttachment1.quizId
        !originalAttachment1.projectId
        originalAttachment1.skillId == badge.badgeId

        List<Attachment> newAttachments = attachments.findAll {!attachment1Href.contains(it.uuid) }

        newAttachments.size() == 1
        quizRes.description == "Here is a [Link](/api/download/${newAttachments[0].uuid})".toString()
        newAttachments[0].quizId == quiz.quizId
        !newAttachments[0].projectId
        !newAttachments[0].skillId
    }

    def "deleting quiz removes associated attachment"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)

        List<String> q1AttachmentsHrefs = (1..3).collect { attachFileForQuizAndReturnHref(quiz.quizId)}

        quiz.description =  "Here is a [Link](${q1AttachmentsHrefs[0]})".toString()
        skillsService.createQuizDef(quiz, quiz.quizId)

        def question1 = QuizDefFactory.createTextInputQuestion(1, 1)
        question1.question =  "Here is a [Link](${q1AttachmentsHrefs[1]})".toString()
        question1.id = skillsService.createQuizQuestionDef(question1).body.id

        def quizAttempt = skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [isSelected: true, answerText:  "Here is a [Link](${q1AttachmentsHrefs[2]})".toString()])

        def quiz2 = QuizDefFactory.createQuiz(2)
        skillsService.createQuizDef(quiz2)

        List<String> q2AttachmentsHrefs = (1..3).collect { attachFileForQuizAndReturnHref(quiz2.quizId)}

        quiz2.description =  "Here is a [Link](${q2AttachmentsHrefs[0]})".toString()
        skillsService.createQuizDef(quiz2, quiz2.quizId)

        def quiz2question1 = QuizDefFactory.createTextInputQuestion(2, 1)
        quiz2question1.question =  "Here is a [Link](${q2AttachmentsHrefs[1]})".toString()
        quiz2question1.id = skillsService.createQuizQuestionDef(quiz2question1).body.id

        def quiz2quizAttempt = skillsService.startQuizAttempt(quiz2.quizId).body
        skillsService.reportQuizAnswer(quiz2.quizId, quiz2quizAttempt.id, quiz2quizAttempt.questions[0].answerOptions[0].id, [isSelected: true, answerText:  "Here is a [Link](${q2AttachmentsHrefs[2]})".toString()])

        when:
        List<Attachment> attachments = attachmentRepo.findAll()
        skillsService.removeQuizDef(quiz.quizId)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        attachments.collect { "/api/download/${it.uuid}".toString() }.sort() == [q1AttachmentsHrefs, q2AttachmentsHrefs].flatten().sort()
        attachments1.collect { "/api/download/${it.uuid}".toString() }.sort() == q2AttachmentsHrefs.sort()
    }
}

