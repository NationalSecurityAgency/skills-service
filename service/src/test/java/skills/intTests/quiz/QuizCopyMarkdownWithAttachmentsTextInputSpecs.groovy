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

import static skills.intTests.utils.SkillsFactory.*

class QuizCopyMarkdownWithAttachmentsTextInputSpecs extends CopyIntSpec {

    def "from another quiz: question -> question; edit question"() {
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
        question2.question = "Here is a [Link](${attachment1Href})".toString()
        skillsService.updateQuizQuestionDef(question2)

        def quiz1Res = skillsService.getQuizQuestionDefs(quiz1.quizId)
        def quiz2Res = skillsService.getQuizQuestionDefs(quiz2.quizId)
        List<Attachment> attachments = attachmentRepo.findAll()

        // should not create new attachments
        question2.question = quiz2Res.questions[0].question
        skillsService.updateQuizQuestionDef(question2)
        skillsService.updateQuizQuestionDef(question2)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        quiz1Res.questions[0].question == "Here is a [Link](${attachment1Href})"

        attachments.size() == 2
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        originalAttachment1.quizId == quiz1.quizId
        !originalAttachment1.projectId
        !originalAttachment1.skillId

        List<Attachment> newAttachments = attachments.findAll {!attachment1Href.contains(it.uuid) }

        newAttachments.size() == 1
        question2.question == "Here is a [Link](/api/download/${newAttachments[0].uuid})"
        newAttachments[0].quizId == quiz2.quizId
        !newAttachments[0].projectId
        !newAttachments[0].skillId

        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "from another quiz: question -> question; new question"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)

        def attachment1Href = attachFileForQuizAndReturnHref(quiz1.quizId)

        def question1 = QuizDefFactory.createTextInputQuestion(1, 1)
        question1.question = "Here is a [Link](${attachment1Href})".toString()
        skillsService.createQuizQuestionDef(question1)

        def quiz2 = QuizDefFactory.createQuiz(2)
        skillsService.createQuizDef(quiz2)

        def question2 = QuizDefFactory.createTextInputQuestion(2, 2)

        when:
        question2.question = "Here is a [Link](${attachment1Href})".toString()
        def createResponse = skillsService.createQuizQuestionDef(question2).body
        question2.id = createResponse.id

        def quiz1Res = skillsService.getQuizQuestionDefs(quiz1.quizId)
        def quiz2Res = skillsService.getQuizQuestionDefs(quiz2.quizId)
        List<Attachment> attachments = attachmentRepo.findAll()

        // should not create new attachments
        question2.question = quiz2Res.questions[0].question
        skillsService.updateQuizQuestionDef(question2)
        skillsService.updateQuizQuestionDef(question2)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        quiz1Res.questions[0].question == "Here is a [Link](${attachment1Href})"

        attachments.size() == 2
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        originalAttachment1.quizId == quiz1.quizId
        !originalAttachment1.projectId
        !originalAttachment1.skillId

        List<Attachment> newAttachments = attachments.findAll {!attachment1Href.contains(it.uuid) }

        newAttachments.size() == 1
        createResponse.question == "Here is a [Link](/api/download/${newAttachments[0].uuid})"
        question2.question == "Here is a [Link](/api/download/${newAttachments[0].uuid})"
        newAttachments[0].quizId == quiz2.quizId
        !newAttachments[0].projectId
        !newAttachments[0].skillId

        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "from another quiz: quiz -> question; edit question"() {
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
        question2.question = "Here is a [Link](${attachment1Href})".toString()
        skillsService.updateQuizQuestionDef(question2)

        def quiz1Res = skillsService.getQuizDef(quiz1.quizId)
        def quiz2Res = skillsService.getQuizQuestionDefs(quiz2.quizId)
        List<Attachment> attachments = attachmentRepo.findAll()

        // should not create new attachments
        question2.question = quiz2Res.questions[0].question
        skillsService.updateQuizQuestionDef(question2)
        skillsService.updateQuizQuestionDef(question2)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        quiz1Res.description == "Here is a [Link](${attachment1Href})"

        attachments.size() == 2
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        originalAttachment1.quizId == quiz1.quizId
        !originalAttachment1.projectId
        !originalAttachment1.skillId

        List<Attachment> newAttachments = attachments.findAll {!attachment1Href.contains(it.uuid) }

        newAttachments.size() == 1
        question2.question == "Here is a [Link](/api/download/${newAttachments[0].uuid})"
        newAttachments[0].quizId == quiz2.quizId
        !newAttachments[0].projectId
        !newAttachments[0].skillId

        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "from another quiz: quiz -> question; new question"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)

        def attachment1Href = attachFileForQuizAndReturnHref(quiz1.quizId)

        quiz1.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.createQuizDef(quiz1, quiz1.quizId)

        def quiz2 = QuizDefFactory.createQuiz(2)
        skillsService.createQuizDef(quiz2)

        def question2 = QuizDefFactory.createTextInputQuestion(2, 2)

        when:
        question2.question = "Here is a [Link](${attachment1Href})".toString()
        question2.id = skillsService.createQuizQuestionDef(question2).body.id

        def quiz1Res = skillsService.getQuizDef(quiz1.quizId)
        def quiz2Res = skillsService.getQuizQuestionDefs(quiz2.quizId)
        List<Attachment> attachments = attachmentRepo.findAll()

        // should not create new attachments
        question2.question = quiz2Res.questions[0].question
        skillsService.updateQuizQuestionDef(question2)
        skillsService.updateQuizQuestionDef(question2)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        quiz1Res.description == "Here is a [Link](${attachment1Href})"

        attachments.size() == 2
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        originalAttachment1.quizId == quiz1.quizId
        !originalAttachment1.projectId
        !originalAttachment1.skillId

        List<Attachment> newAttachments = attachments.findAll {!attachment1Href.contains(it.uuid) }

        newAttachments.size() == 1
        question2.question == "Here is a [Link](/api/download/${newAttachments[0].uuid})"
        newAttachments[0].quizId == quiz2.quizId
        !newAttachments[0].projectId
        !newAttachments[0].skillId

        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "from another quiz: answer -> question; edit question"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)

        def attachment1Href = attachFileForQuizAndReturnHref(quiz1.quizId)

        def question = QuizDefFactory.createTextInputQuestion(1, 1)
        skillsService.createQuizQuestionDef(question)

        def quizAttempt = skillsService.startQuizAttempt(quiz1.quizId).body
        skillsService.reportQuizAnswer(quiz1.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [isSelected: true, answerText:  "Here is a [Link](${attachment1Href})".toString()])

        def quiz2 = QuizDefFactory.createQuiz(2)
        skillsService.createQuizDef(quiz2)

        def question2 = QuizDefFactory.createTextInputQuestion(2, 2)
        question2.id = skillsService.createQuizQuestionDef(question2).body.id

        when:
        question2.question = "Here is a [Link](${attachment1Href})".toString()
        skillsService.updateQuizQuestionDef(question2)

        def questionsRes = skillsService.getQuizAttemptResult(quiz1.quizId, quizAttempt.id)
        def quiz2Res = skillsService.getQuizQuestionDefs(quiz2.quizId)
        List<Attachment> attachments = attachmentRepo.findAll()

        // should not create new attachments
        question2.question = quiz2Res.questions[0].question
        skillsService.updateQuizQuestionDef(question2)
        skillsService.updateQuizQuestionDef(question2)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        questionsRes.questions[0].answers[0].answer == "Here is a [Link](${attachment1Href})"

        attachments.size() == 2
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        originalAttachment1.quizId == quiz1.quizId
        !originalAttachment1.projectId
        !originalAttachment1.skillId

        List<Attachment> newAttachments = attachments.findAll {!attachment1Href.contains(it.uuid) }

        newAttachments.size() == 1
        question2.question == "Here is a [Link](/api/download/${newAttachments[0].uuid})"
        newAttachments[0].quizId == quiz2.quizId
        !newAttachments[0].projectId
        !newAttachments[0].skillId

        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "from another quiz: answer -> question; new question"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)

        def attachment1Href = attachFileForQuizAndReturnHref(quiz1.quizId)

        def question = QuizDefFactory.createTextInputQuestion(1, 1)
        skillsService.createQuizQuestionDef(question)

        def quizAttempt = skillsService.startQuizAttempt(quiz1.quizId).body
        skillsService.reportQuizAnswer(quiz1.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [isSelected: true, answerText:  "Here is a [Link](${attachment1Href})".toString()])

        def quiz2 = QuizDefFactory.createQuiz(2)
        skillsService.createQuizDef(quiz2)

        def question2 = QuizDefFactory.createTextInputQuestion(2, 2)

        when:
        question2.question = "Here is a [Link](${attachment1Href})".toString()
        question2.id = skillsService.createQuizQuestionDef(question2).body.id

        def questionsRes = skillsService.getQuizAttemptResult(quiz1.quizId, quizAttempt.id)
        def quiz2Res = skillsService.getQuizQuestionDefs(quiz2.quizId)
        List<Attachment> attachments = attachmentRepo.findAll()

        // should not create new attachments
        question2.question = quiz2Res.questions[0].question
        skillsService.updateQuizQuestionDef(question2)
        skillsService.updateQuizQuestionDef(question2)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        questionsRes.questions[0].answers[0].answer == "Here is a [Link](${attachment1Href})"

        attachments.size() == 2
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        originalAttachment1.quizId == quiz1.quizId
        !originalAttachment1.projectId
        !originalAttachment1.skillId

        List<Attachment> newAttachments = attachments.findAll {!attachment1Href.contains(it.uuid) }

        newAttachments.size() == 1
        question2.question == "Here is a [Link](/api/download/${newAttachments[0].uuid})"
        newAttachments[0].quizId == quiz2.quizId
        !newAttachments[0].projectId
        !newAttachments[0].skillId

        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "from same quiz: question -> question; edit question"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)

        def attachment1Href = attachFileForQuizAndReturnHref(quiz1.quizId)

        def question1 = QuizDefFactory.createTextInputQuestion(1, 1)
        question1.question = "Here is a [Link](${attachment1Href})".toString()
        skillsService.createQuizQuestionDef(question1)


        def question2 = QuizDefFactory.createTextInputQuestion(1, 2)
        question2.id = skillsService.createQuizQuestionDef(question2).body.id

        when:
        question2.question = "Here is a [Link](${attachment1Href})".toString()
        skillsService.updateQuizQuestionDef(question2)

        def quiz1Res = skillsService.getQuizQuestionDefs(quiz1.quizId)
        List<Attachment> attachments = attachmentRepo.findAll()

        // should not create new attachments
        question2.question = quiz1Res.questions[1].question
        skillsService.updateQuizQuestionDef(question2)
        skillsService.updateQuizQuestionDef(question2)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        quiz1Res.questions[0].question == "Here is a [Link](${attachment1Href})"

        attachments.size() == 2
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        originalAttachment1.quizId == quiz1.quizId
        !originalAttachment1.projectId
        !originalAttachment1.skillId

        List<Attachment> newAttachments = attachments.findAll {!attachment1Href.contains(it.uuid) }

        newAttachments.size() == 1
        question2.question == "Here is a [Link](/api/download/${newAttachments[0].uuid})"
        newAttachments[0].quizId == quiz1.quizId
        !newAttachments[0].projectId
        !newAttachments[0].skillId

        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "from same quiz: question -> question; new question"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)

        def attachment1Href = attachFileForQuizAndReturnHref(quiz1.quizId)

        def question1 = QuizDefFactory.createTextInputQuestion(1, 1)
        question1.question = "Here is a [Link](${attachment1Href})".toString()
        skillsService.createQuizQuestionDef(question1)

        def question2 = QuizDefFactory.createTextInputQuestion(1, 2)

        when:
        question2.question = "Here is a [Link](${attachment1Href})".toString()
        question2.id = skillsService.createQuizQuestionDef(question2).body.id

        def quiz1Res = skillsService.getQuizQuestionDefs(quiz1.quizId)
        List<Attachment> attachments = attachmentRepo.findAll()

        // should not create new attachments
        question2.question = quiz1Res.questions[1].question
        skillsService.updateQuizQuestionDef(question2)
        skillsService.updateQuizQuestionDef(question2)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        quiz1Res.questions[0].question == "Here is a [Link](${attachment1Href})"

        attachments.size() == 2
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        originalAttachment1.quizId == quiz1.quizId
        !originalAttachment1.projectId
        !originalAttachment1.skillId

        List<Attachment> newAttachments = attachments.findAll {!attachment1Href.contains(it.uuid) }

        newAttachments.size() == 1
        question2.question == "Here is a [Link](/api/download/${newAttachments[0].uuid})"
        newAttachments[0].quizId == quiz1.quizId
        !newAttachments[0].projectId
        !newAttachments[0].skillId

        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "from same quiz: quiz -> question; edit question"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)

        def attachment1Href = attachFileForQuizAndReturnHref(quiz1.quizId)

        quiz1.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.createQuizDef(quiz1, quiz1.quizId)

        def question2 = QuizDefFactory.createTextInputQuestion(1, 2)
        question2.id = skillsService.createQuizQuestionDef(question2).body.id

        when:
        question2.question = "Here is a [Link](${attachment1Href})".toString()
        skillsService.updateQuizQuestionDef(question2)

        def quiz1Res = skillsService.getQuizDef(quiz1.quizId)
        def quiz1QuestionDefRes = skillsService.getQuizQuestionDefs(quiz1.quizId)
        List<Attachment> attachments = attachmentRepo.findAll()

        // should not create new attachments
        question2.question = quiz1QuestionDefRes.questions[0].question
        skillsService.updateQuizQuestionDef(question2)
        skillsService.updateQuizQuestionDef(question2)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        quiz1Res.description == "Here is a [Link](${attachment1Href})"

        attachments.size() == 2
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        originalAttachment1.quizId == quiz1.quizId
        !originalAttachment1.projectId
        !originalAttachment1.skillId

        List<Attachment> newAttachments = attachments.findAll {!attachment1Href.contains(it.uuid) }

        newAttachments.size() == 1
        question2.question == "Here is a [Link](/api/download/${newAttachments[0].uuid})"
        newAttachments[0].quizId == quiz1.quizId
        !newAttachments[0].projectId
        !newAttachments[0].skillId

        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "from same quiz: quiz -> question; new question"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)

        def attachment1Href = attachFileForQuizAndReturnHref(quiz1.quizId)

        quiz1.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.createQuizDef(quiz1, quiz1.quizId)

        def question2 = QuizDefFactory.createTextInputQuestion(1, 2)

        when:
        question2.question = "Here is a [Link](${attachment1Href})".toString()
        question2.id = skillsService.createQuizQuestionDef(question2).body.id

        def quiz1Res = skillsService.getQuizDef(quiz1.quizId)
        def quiz1QuestionDefRes = skillsService.getQuizQuestionDefs(quiz1.quizId)
        List<Attachment> attachments = attachmentRepo.findAll()

        // should not create new attachments
        question2.question = quiz1QuestionDefRes.questions[0].question
        skillsService.updateQuizQuestionDef(question2)
        skillsService.updateQuizQuestionDef(question2)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        quiz1Res.description == "Here is a [Link](${attachment1Href})"

        attachments.size() == 2
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        originalAttachment1.quizId == quiz1.quizId
        !originalAttachment1.projectId
        !originalAttachment1.skillId

        List<Attachment> newAttachments = attachments.findAll {!attachment1Href.contains(it.uuid) }

        newAttachments.size() == 1
        question2.question == "Here is a [Link](/api/download/${newAttachments[0].uuid})"
        newAttachments[0].quizId == quiz1.quizId
        !newAttachments[0].projectId
        !newAttachments[0].skillId

        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "from same quiz: answer -> question; edit question"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)

        def attachment1Href = attachFileForQuizAndReturnHref(quiz1.quizId)

        def question = QuizDefFactory.createTextInputQuestion(1, 1)
        skillsService.createQuizQuestionDef(question)

        def quizAttempt = skillsService.startQuizAttempt(quiz1.quizId).body
        skillsService.reportQuizAnswer(quiz1.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [isSelected: true, answerText:  "Here is a [Link](${attachment1Href})".toString()])

        def question2 = QuizDefFactory.createTextInputQuestion(1, 2)
        question2.id = skillsService.createQuizQuestionDef(question2).body.id

        when:
        question2.question = "Here is a [Link](${attachment1Href})".toString()
        skillsService.updateQuizQuestionDef(question2)

        def questionsRes = skillsService.getQuizAttemptResult(quiz1.quizId, quizAttempt.id)
        def quiz1Res = skillsService.getQuizQuestionDefs(quiz1.quizId)
        List<Attachment> attachments = attachmentRepo.findAll()

        // should not create new attachments
        question2.question = quiz1Res.questions[1].question
        skillsService.updateQuizQuestionDef(question2)
        skillsService.updateQuizQuestionDef(question2)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        questionsRes.questions[0].answers[0].answer == "Here is a [Link](${attachment1Href})"

        attachments.size() == 2
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        originalAttachment1.quizId == quiz1.quizId
        !originalAttachment1.projectId
        !originalAttachment1.skillId

        List<Attachment> newAttachments = attachments.findAll {!attachment1Href.contains(it.uuid) }

        newAttachments.size() == 1
        question2.question == "Here is a [Link](/api/download/${newAttachments[0].uuid})"
        newAttachments[0].quizId == quiz1.quizId
        !newAttachments[0].projectId
        !newAttachments[0].skillId

        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "from same quiz: answer -> question; new question"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)

        def attachment1Href = attachFileForQuizAndReturnHref(quiz1.quizId)

        def question = QuizDefFactory.createTextInputQuestion(1, 1)
        skillsService.createQuizQuestionDef(question)

        def quizAttempt = skillsService.startQuizAttempt(quiz1.quizId).body
        skillsService.reportQuizAnswer(quiz1.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [isSelected: true, answerText:  "Here is a [Link](${attachment1Href})".toString()])

        def question2 = QuizDefFactory.createTextInputQuestion(1, 2)

        when:
        question2.question = "Here is a [Link](${attachment1Href})".toString()
        question2.id = skillsService.createQuizQuestionDef(question2).body.id

        def questionsRes = skillsService.getQuizAttemptResult(quiz1.quizId, quizAttempt.id)
        def quiz1Res = skillsService.getQuizQuestionDefs(quiz1.quizId)
        List<Attachment> attachments = attachmentRepo.findAll()

        // should not create new attachments
        question2.question = quiz1Res.questions[1].question
        skillsService.updateQuizQuestionDef(question2)
        skillsService.updateQuizQuestionDef(question2)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        questionsRes.questions[0].answers[0].answer == "Here is a [Link](${attachment1Href})"

        attachments.size() == 2
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        originalAttachment1.quizId == quiz1.quizId
        !originalAttachment1.projectId
        !originalAttachment1.skillId

        List<Attachment> newAttachments = attachments.findAll {!attachment1Href.contains(it.uuid) }

        newAttachments.size() == 1
        question2.question == "Here is a [Link](/api/download/${newAttachments[0].uuid})"
        newAttachments[0].quizId == quiz1.quizId
        !newAttachments[0].projectId
        !newAttachments[0].skillId

        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "editing the same question does not produce duplicate attachments"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)

        def attachment1Href = attachFileForQuizAndReturnHref(quiz1.quizId)
        def attachment2Href = attachFileForQuizAndReturnHref(quiz1.quizId)

        def question = QuizDefFactory.createTextInputQuestion(1, 1)
        question.question = "Here is a [Link](${attachment1Href})\n\nHere is a [Link](${attachment2Href})".toString()
        question.id = skillsService.createQuizQuestionDef(question).body.id

        when:
        def quiz1Res = skillsService.getQuizQuestionDefs(quiz1.quizId)
        List<Attachment> attachments = attachmentRepo.findAll()

        skillsService.updateQuizQuestionDef(question)
        skillsService.updateQuizQuestionDef(question)
        skillsService.updateQuizQuestionDef(question)

        def quiz1ResAfter = skillsService.getQuizQuestionDefs(quiz1.quizId)
        List<Attachment> attachmentsAfter = attachmentRepo.findAll()

        then:
        quiz1Res.questions[0].question == "Here is a [Link](${attachment1Href})\n\nHere is a [Link](${attachment2Href})".toString()
        quiz1ResAfter.questions[0].question == "Here is a [Link](${attachment1Href})\n\nHere is a [Link](${attachment2Href})".toString()

        attachments.uuid.sort() == attachmentsAfter.uuid.sort()
    }

    def "from another quiz: question -> question; edit question - multiple attachments"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)

        def attachment1Href = attachFileForQuizAndReturnHref(quiz1.quizId)
        def attachment2Href = attachFileForQuizAndReturnHref(quiz1.quizId)

        def question1 = QuizDefFactory.createTextInputQuestion(1, 1)
        question1.question = "Here is a [Link](${attachment1Href})\n\nHere is a [Link](${attachment2Href})".toString()
        skillsService.createQuizQuestionDef(question1)

        def quiz2 = QuizDefFactory.createQuiz(2)
        skillsService.createQuizDef(quiz2)

        def question2 = QuizDefFactory.createTextInputQuestion(2, 2)
        question2.id = skillsService.createQuizQuestionDef(question2).body.id

        when:
        question2.question = "Here is a [Link](${attachment1Href})\n\nHere is a [Link](${attachment2Href})".toString()
        skillsService.updateQuizQuestionDef(question2)

        def quiz1Res = skillsService.getQuizQuestionDefs(quiz1.quizId)
        def quiz2Res = skillsService.getQuizQuestionDefs(quiz2.quizId)
        List<Attachment> attachments = attachmentRepo.findAll()

        // should not create new attachments
        question2.question = quiz2Res.questions[0].question
        skillsService.updateQuizQuestionDef(question2)
        skillsService.updateQuizQuestionDef(question2)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        quiz1Res.questions[0].question == "Here is a [Link](${attachment1Href})\n\nHere is a [Link](${attachment2Href})".toString()

        attachments.size() == 4
        List<Attachment> originalAttachments = attachments.findAll {  attachment1Href.contains(it.uuid) || attachment2Href.contains(it.uuid)}
        originalAttachments.size() == 2
        originalAttachments[0].quizId == quiz1.quizId
        !originalAttachments[0].projectId
        !originalAttachments[0].skillId
        originalAttachments[1].quizId == quiz1.quizId
        !originalAttachments[1].projectId
        !originalAttachments[1].skillId


        List<Attachment> newAttachments = attachments.findAll {!attachment1Href.contains(it.uuid) && !attachment2Href.contains(it.uuid) }

        newAttachments.size() == 2
        question2.question.contains("Here is a [Link](/api/download/${newAttachments[0].uuid})")
        question2.question.contains("Here is a [Link](/api/download/${newAttachments[1].uuid})")
        newAttachments[0].quizId == quiz2.quizId
        !newAttachments[0].projectId
        !newAttachments[0].skillId
        newAttachments[1].quizId == quiz2.quizId
        !newAttachments[1].projectId
        !newAttachments[1].skillId

        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "from project: skill -> question; new question"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def attachment1Href = attachFileAndReturnHref(p1.projectId)

        def p1Skills = createSkills(2, 1, 1, 100)
        p1Skills[0].description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.createSkills(p1Skills)

        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)
        def question2 = QuizDefFactory.createTextInputQuestion(1, 2)

        when:
        question2.question = "Here is a [Link](${attachment1Href})".toString()
        question2.id = skillsService.createQuizQuestionDef(question2).body.id

        def skillRes = skillsService.getSkill(p1Skills[0])
        def quiz1QuestionDefRes = skillsService.getQuizQuestionDefs(quiz1.quizId)
        List<Attachment> attachments = attachmentRepo.findAll()

        // should not create new attachments
        question2.question = quiz1QuestionDefRes.questions[0].question
        skillsService.updateQuizQuestionDef(question2)
        skillsService.updateQuizQuestionDef(question2)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        skillRes.description == "Here is a [Link](${attachment1Href})"

        attachments.size() == 2
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        !originalAttachment1.quizId
        originalAttachment1.projectId == p1.projectId
        originalAttachment1.skillId == p1Skills[0].skillId

        List<Attachment> newAttachments = attachments.findAll {!attachment1Href.contains(it.uuid) }

        newAttachments.size() == 1
        question2.question == "Here is a [Link](/api/download/${newAttachments[0].uuid})"
        newAttachments[0].quizId == quiz1.quizId
        !newAttachments[0].projectId
        !newAttachments[0].skillId

        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "from project: project -> question; edit question"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def attachment1Href = attachFileAndReturnHref(p1.projectId)

        p1.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.updateProject(p1)

        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)
        def question2 = QuizDefFactory.createTextInputQuestion(1, 2)
        question2.id = skillsService.createQuizQuestionDef(question2).body.id

        when:
        question2.question = "Here is a [Link](${attachment1Href})".toString()
        skillsService.updateQuizQuestionDef(question2)

        def projRes = skillsService.getProjectDescription(p1.projectId)
        def quiz1QuestionDefRes = skillsService.getQuizQuestionDefs(quiz1.quizId)
        List<Attachment> attachments = attachmentRepo.findAll()

        // should not create new attachments
        question2.question = quiz1QuestionDefRes.questions[0].question
        skillsService.updateQuizQuestionDef(question2)
        skillsService.updateQuizQuestionDef(question2)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        projRes.description == "Here is a [Link](${attachment1Href})"

        attachments.size() == 2
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        !originalAttachment1.quizId
        originalAttachment1.projectId == p1.projectId
        !originalAttachment1.skillId

        List<Attachment> newAttachments = attachments.findAll {!attachment1Href.contains(it.uuid) }

        newAttachments.size() == 1
        question2.question == "Here is a [Link](/api/download/${newAttachments[0].uuid})"
        newAttachments[0].quizId == quiz1.quizId
        !newAttachments[0].projectId
        !newAttachments[0].skillId

        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "from Global Badge: gb -> question; new question"() {
        def badge = createBadge(1, 1)
        skillsService.createGlobalBadge(badge)

        def attachment1Href = attachFileForGlobalBadgeAndReturnHref(badge.badgeId)

        badge.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.updateGlobalBadge(badge)

        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)
        def question2 = QuizDefFactory.createTextInputQuestion(1, 2)

        when:
        question2.question = "Here is a [Link](${attachment1Href})".toString()
        question2.id = skillsService.createQuizQuestionDef(question2).body.id

        def badgeRes = skillsService.getGlobalBadge(badge.badgeId)
        def quiz1QuestionDefRes = skillsService.getQuizQuestionDefs(quiz1.quizId)
        List<Attachment> attachments = attachmentRepo.findAll()

        // should not create new attachments
        question2.question = quiz1QuestionDefRes.questions[0].question
        skillsService.updateQuizQuestionDef(question2)
        skillsService.updateQuizQuestionDef(question2)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        badgeRes.description == "Here is a [Link](${attachment1Href})"

        attachments.size() == 2
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        !originalAttachment1.quizId
        !originalAttachment1.projectId
        originalAttachment1.skillId == badge.badgeId

        List<Attachment> newAttachments = attachments.findAll {!attachment1Href.contains(it.uuid) }

        newAttachments.size() == 1
        question2.question == "Here is a [Link](/api/download/${newAttachments[0].uuid})"
        newAttachments[0].quizId == quiz1.quizId
        !newAttachments[0].projectId
        !newAttachments[0].skillId

        attachments1.uuid.sort() == attachments.uuid.sort()
    }
}
