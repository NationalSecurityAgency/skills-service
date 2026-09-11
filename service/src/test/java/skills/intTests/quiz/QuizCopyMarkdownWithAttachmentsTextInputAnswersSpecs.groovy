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

import skills.intTests.copyProject.CopyIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.storage.model.Attachment
import spock.lang.IgnoreRest

class QuizCopyMarkdownWithAttachmentsTextInputAnswersSpecs extends CopyIntSpec {

    def "from another project: question -> answer"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)

        def attachment1Href = attachFileForQuizAndReturnHref(quiz1.quizId)

        def question1 = QuizDefFactory.createTextInputQuestion(1, 1)
        question1.question = "Here is a [Link](${attachment1Href})".toString()
        skillsService.createQuizQuestionDef(question1)

        def quiz2 = QuizDefFactory.createQuiz(2)
        skillsService.createQuizDef(quiz2)

        def question2 = QuizDefFactory.createTextInputQuestion(2, 2)
        question2.id = skillsService.createQuizQuestionDef(question2).body.id

        when:
        def quizAttempt = skillsService.startQuizAttempt(quiz2.quizId).body
        skillsService.reportQuizAnswer(quiz2.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [isSelected: true, answerText:  "Here is a [Link](${attachment1Href})".toString()])

        def quiz1Res = skillsService.getQuizQuestionDefs(quiz1.quizId)
        def quiz2Res = skillsService.getQuizAttemptResult(quiz2.quizId, quizAttempt.id)
        List<Attachment> attachments = attachmentRepo.findAll()

        then:
        quiz1Res.questions[0].question == "Here is a [Link](${attachment1Href})"

        attachments.size() == 2
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        originalAttachment1.quizId == quiz1.quizId
        !originalAttachment1.projectId
        !originalAttachment1.skillId

        List<Attachment> newAttachments = attachments.findAll {!attachment1Href.contains(it.uuid) }

        newAttachments.size() == 1
        quiz2Res.questions[0].answers[0].answer == "Here is a [Link](/api/download/${newAttachments[0].uuid})"
        newAttachments[0].quizId == quiz2.quizId
        !newAttachments[0].projectId
        !newAttachments[0].skillId
    }

    def "from another project: quiz -> answer"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)

        def attachment1Href = attachFileForQuizAndReturnHref(quiz1.quizId)

        quiz1.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.createQuizDef(quiz1, quiz1.quizId)

        def quiz2 = QuizDefFactory.createQuiz(2)
        skillsService.createQuizDef(quiz2)

        def question2 = QuizDefFactory.createTextInputQuestion(2, 2)
        question2.id = skillsService.createQuizQuestionDef(question2).body.id

        when:
        def quizAttempt = skillsService.startQuizAttempt(quiz2.quizId).body
        skillsService.reportQuizAnswer(quiz2.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [isSelected: true, answerText:  "Here is a [Link](${attachment1Href})".toString()])

        def quiz1Res = skillsService.getQuizDef(quiz1.quizId)
        def quiz2Res = skillsService.getQuizAttemptResult(quiz2.quizId, quizAttempt.id)
        List<Attachment> attachments = attachmentRepo.findAll()

        then:
        quiz1Res.description == "Here is a [Link](${attachment1Href})"

        attachments.size() == 2
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        originalAttachment1.quizId == quiz1.quizId
        !originalAttachment1.projectId
        !originalAttachment1.skillId

        List<Attachment> newAttachments = attachments.findAll {!attachment1Href.contains(it.uuid) }

        newAttachments.size() == 1
        quiz2Res.questions[0].answers[0].answer == "Here is a [Link](/api/download/${newAttachments[0].uuid})"
        newAttachments[0].quizId == quiz2.quizId
        !newAttachments[0].projectId
        !newAttachments[0].skillId
    }

    def "from another project: answer -> answer"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)

        def attachment1Href = attachFileForQuizAndReturnHref(quiz1.quizId)

        def question = QuizDefFactory.createTextInputQuestion(1, 1)
        skillsService.createQuizQuestionDef(question)

        def quiz1QuestionAttempt = skillsService.startQuizAttempt(quiz1.quizId).body
        skillsService.reportQuizAnswer(quiz1.quizId, quiz1QuestionAttempt.id, quiz1QuestionAttempt.questions[0].answerOptions[0].id, [isSelected: true, answerText:  "Here is a [Link](${attachment1Href})".toString()])

        def quiz2 = QuizDefFactory.createQuiz(2)
        skillsService.createQuizDef(quiz2)

        def question2 = QuizDefFactory.createTextInputQuestion(2, 2)
        question2.id = skillsService.createQuizQuestionDef(question2).body.id

        when:
        def quizAttempt = skillsService.startQuizAttempt(quiz2.quizId).body
        skillsService.reportQuizAnswer(quiz2.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [isSelected: true, answerText:  "Here is a [Link](${attachment1Href})".toString()])

        def questionsRes = skillsService.getQuizAttemptResult(quiz1.quizId, quiz1QuestionAttempt.id)
        def quiz2Res = skillsService.getQuizAttemptResult(quiz2.quizId, quizAttempt.id)
        List<Attachment> attachments = attachmentRepo.findAll()
        then:
        questionsRes.questions[0].answers[0].answer == "Here is a [Link](${attachment1Href})"

        attachments.size() == 2
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        originalAttachment1.quizId == quiz1.quizId
        !originalAttachment1.projectId
        !originalAttachment1.skillId

        List<Attachment> newAttachments = attachments.findAll {!attachment1Href.contains(it.uuid) }

        newAttachments.size() == 1
        quiz2Res.questions[0].answers[0].answer == "Here is a [Link](/api/download/${newAttachments[0].uuid})"
        newAttachments[0].quizId == quiz2.quizId
        !newAttachments[0].projectId
        !newAttachments[0].skillId
    }

    def "from same project: question -> answer"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)

        def attachment1Href = attachFileForQuizAndReturnHref(quiz1.quizId)

        def question1 = QuizDefFactory.createTextInputQuestion(1, 1)
        question1.question = "Here is a [Link](${attachment1Href})".toString()
        skillsService.createQuizQuestionDef(question1)

        def question2 = QuizDefFactory.createTextInputQuestion(1, 2)
        question2.id = skillsService.createQuizQuestionDef(question2).body.id

        when:
        def quizAttempt = skillsService.startQuizAttempt(quiz1.quizId).body
        skillsService.reportQuizAnswer(quiz1.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [isSelected: true, answerText:  "Here is a [Link](${attachment1Href})".toString()])

        def quiz1Res = skillsService.getQuizQuestionDefs(quiz1.quizId)
        def attemptRes = skillsService.getQuizAttemptResult(quiz1.quizId, quizAttempt.id)
        List<Attachment> attachments = attachmentRepo.findAll()

        then:
        quiz1Res.questions[0].question == "Here is a [Link](${attachment1Href})"

        attachments.size() == 2
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        originalAttachment1.quizId == quiz1.quizId
        !originalAttachment1.projectId
        !originalAttachment1.skillId

        List<Attachment> newAttachments = attachments.findAll {!attachment1Href.contains(it.uuid) }

        newAttachments.size() == 1
        attemptRes.questions[0].answers[0].answer == "Here is a [Link](/api/download/${newAttachments[0].uuid})"
        newAttachments[0].quizId == quiz1.quizId
        !newAttachments[0].projectId
        !newAttachments[0].skillId
    }

    def "from same project: quiz -> answer"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)

        def attachment1Href = attachFileForQuizAndReturnHref(quiz1.quizId)

        quiz1.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.createQuizDef(quiz1, quiz1.quizId)

        def question2 = QuizDefFactory.createTextInputQuestion(1, 2)
        question2.id = skillsService.createQuizQuestionDef(question2).body.id

        when:
        def quizAttempt = skillsService.startQuizAttempt(quiz1.quizId).body
        skillsService.reportQuizAnswer(quiz1.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [isSelected: true, answerText:  "Here is a [Link](${attachment1Href})".toString()])

        def quiz1Res = skillsService.getQuizDef(quiz1.quizId)
        def quiz2Res = skillsService.getQuizAttemptResult(quiz1.quizId, quizAttempt.id)
        List<Attachment> attachments = attachmentRepo.findAll()

        then:
        quiz1Res.description == "Here is a [Link](${attachment1Href})"

        attachments.size() == 2
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        originalAttachment1.quizId == quiz1.quizId
        !originalAttachment1.projectId
        !originalAttachment1.skillId

        List<Attachment> newAttachments = attachments.findAll {!attachment1Href.contains(it.uuid) }

        newAttachments.size() == 1
        quiz2Res.questions[0].answers[0].answer == "Here is a [Link](/api/download/${newAttachments[0].uuid})"
        newAttachments[0].quizId == quiz1.quizId
        !newAttachments[0].projectId
        !newAttachments[0].skillId
    }

//     TODO: Implement these:
//    def "from same project: answer -> answer"() {
//        def quiz1 = QuizDefFactory.createQuiz(1)
//        skillsService.createQuizDef(quiz1)
//
//        def attachment1Href = attachFileForQuizAndReturnHref(quiz1.quizId)
//
//        def question = QuizDefFactory.createTextInputQuestion(1, 1)
//        skillsService.createQuizQuestionDef(question)
//
//        def quiz1QuestionAttempt = skillsService.startQuizAttempt(quiz1.quizId).body
//        skillsService.reportQuizAnswer(quiz1.quizId, quiz1QuestionAttempt.id, quiz1QuestionAttempt.questions[0].answerOptions[0].id, [isSelected: true, answerText:  "Here is a [Link](${attachment1Href})".toString()])
//
//        def question2 = QuizDefFactory.createTextInputQuestion(1, 2)
//        question2.id = skillsService.createQuizQuestionDef(question2).body.id
//
//        when:
//        def quizAttempt = skillsService.startQuizAttempt(quiz1.quizId).body
//        skillsService.reportQuizAnswer(quiz1.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[0].id, [isSelected: true, answerText:  "Here is a [Link](${attachment1Href})".toString()])
//
//        def questionsRes = skillsService.getQuizAttemptResult(quiz1.quizId, quiz1QuestionAttempt.id)
//        def quiz2Res = skillsService.getQuizAttemptResult(quiz2.quizId, quizAttempt.id)
//        List<Attachment> attachments = attachmentRepo.findAll()
//        then:
//        questionsRes.questions[0].answers[0].answer == "Here is a [Link](${attachment1Href})"
//
//        attachments.size() == 2
//        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
//        originalAttachment1.quizId == quiz1.quizId
//        !originalAttachment1.projectId
//        !originalAttachment1.skillId
//
//        List<Attachment> newAttachments = attachments.findAll {!attachment1Href.contains(it.uuid) }
//
//        newAttachments.size() == 1
//        quiz2Res.questions[0].answers[0].answer == "Here is a [Link](/api/download/${newAttachments[0].uuid})"
//        newAttachments[0].quizId == quiz2.quizId
//        !newAttachments[0].projectId
//        !newAttachments[0].skillId
//    }
//
//
//    def "editing the same question does not produce duplicate attachments"() {
//        def quiz1 = QuizDefFactory.createQuiz(1)
//        skillsService.createQuizDef(quiz1)
//
//        def attachment1Href = attachFileForQuizAndReturnHref(quiz1.quizId)
//        def attachment2Href = attachFileForQuizAndReturnHref(quiz1.quizId)
//
//        def question = QuizDefFactory.createTextInputQuestion(1, 1)
//        question.question = "Here is a [Link](${attachment1Href})\n\nHere is a [Link](${attachment2Href})".toString()
//        question.id = skillsService.createQuizQuestionDef(question).body.id
//
//        when:
//        def quiz1Res = skillsService.getQuizQuestionDefs(quiz1.quizId)
//        List<Attachment> attachments = attachmentRepo.findAll()
//
//        skillsService.updateQuizQuestionDef(question)
//        skillsService.updateQuizQuestionDef(question)
//        skillsService.updateQuizQuestionDef(question)
//
//        def quiz1ResAfter = skillsService.getQuizQuestionDefs(quiz1.quizId)
//        List<Attachment> attachmentsAfter = attachmentRepo.findAll()
//
//        then:
//        quiz1Res.questions[0].question == "Here is a [Link](${attachment1Href})\n\nHere is a [Link](${attachment2Href})".toString()
//        quiz1ResAfter.questions[0].question == "Here is a [Link](${attachment1Href})\n\nHere is a [Link](${attachment2Href})".toString()
//
//        attachments.uuid.sort() == attachmentsAfter.uuid.sort()
//    }
//
//    def "from another project: question -> question; edit question - multiple attachments"() {
//        def quiz1 = QuizDefFactory.createQuiz(1)
//        skillsService.createQuizDef(quiz1)
//
//        def attachment1Href = attachFileForQuizAndReturnHref(quiz1.quizId)
//        def attachment2Href = attachFileForQuizAndReturnHref(quiz1.quizId)
//
//        def question1 = QuizDefFactory.createTextInputQuestion(1, 1)
//        question1.question = "Here is a [Link](${attachment1Href})\n\nHere is a [Link](${attachment2Href})".toString()
//        skillsService.createQuizQuestionDef(question1)
//
//        def quiz2 = QuizDefFactory.createQuiz(2)
//        skillsService.createQuizDef(quiz2)
//
//        def question2 = QuizDefFactory.createTextInputQuestion(2, 2)
//        question2.id = skillsService.createQuizQuestionDef(question2).body.id
//
//        when:
//        question2.question = "Here is a [Link](${attachment1Href})\n\nHere is a [Link](${attachment2Href})".toString()
//        skillsService.updateQuizQuestionDef(question2)
//
//        def quiz1Res = skillsService.getQuizQuestionDefs(quiz1.quizId)
//        def quiz2Res = skillsService.getQuizQuestionDefs(quiz2.quizId)
//        List<Attachment> attachments = attachmentRepo.findAll()
//
//        // should not create new attachments
//        question2.question = quiz2Res.questions[0].question
//        skillsService.updateQuizQuestionDef(question2)
//        skillsService.updateQuizQuestionDef(question2)
//        List<Attachment> attachments1 = attachmentRepo.findAll()
//        then:
//        quiz1Res.questions[0].question == "Here is a [Link](${attachment1Href})\n\nHere is a [Link](${attachment2Href})".toString()
//
//        attachments.size() == 4
//        List<Attachment> originalAttachments = attachments.findAll {  attachment1Href.contains(it.uuid) || attachment2Href.contains(it.uuid)}
//        originalAttachments.size() == 2
//        originalAttachments[0].quizId == quiz1.quizId
//        !originalAttachments[0].projectId
//        !originalAttachments[0].skillId
//        originalAttachments[1].quizId == quiz1.quizId
//        !originalAttachments[1].projectId
//        !originalAttachments[1].skillId
//
//
//        List<Attachment> newAttachments = attachments.findAll {!attachment1Href.contains(it.uuid) && !attachment2Href.contains(it.uuid) }
//
//        newAttachments.size() == 2
//        question2.question.contains("Here is a [Link](/api/download/${newAttachments[0].uuid})")
//        question2.question.contains("Here is a [Link](/api/download/${newAttachments[1].uuid})")
//        newAttachments[0].quizId == quiz2.quizId
//        !newAttachments[0].projectId
//        !newAttachments[0].skillId
//        newAttachments[1].quizId == quiz2.quizId
//        !newAttachments[1].projectId
//        !newAttachments[1].skillId
//
//        attachments1.uuid.sort() == attachments.uuid.sort()
//    }
//
//    todo: add a test to copy from a project

}
