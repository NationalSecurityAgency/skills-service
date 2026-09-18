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

class QuizCopyMarkdownWithAttachmentsTextInputAnswersSpecs extends CopyIntSpec {

    def "from another quiz: question -> answer"() {
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

    def "from another quiz: quiz -> answer"() {
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

    def "from another quiz: answer -> answer"() {
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

    def "from same quiz: question -> answer"() {
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

    def "from same quiz: quiz -> answer"() {
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

    def "from same quiz: answer -> answer"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)

        def attachment1Href = attachFileForQuizAndReturnHref(quiz1.quizId)

        def question1 = QuizDefFactory.createTextInputQuestion(1, 1)
        question1.id = skillsService.createQuizQuestionDef(question1).body.id

        def question2 = QuizDefFactory.createTextInputQuestion(1, 2)
        question2.id = skillsService.createQuizQuestionDef(question2).body.id

        def quizAttempt = skillsService.startQuizAttempt(quiz1.quizId).body
        skillsService.reportQuizAnswer(quiz1.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [isSelected: true, answerText:  "Here is a [Link](${attachment1Href})".toString()])

        when:
        skillsService.reportQuizAnswer(quiz1.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[0].id, [isSelected: true, answerText:  "Here is a [Link](${attachment1Href})".toString()])

        def quiz1Res = skillsService.getQuizAttemptResult(quiz1.quizId, quizAttempt.id)
        List<Attachment> attachments = attachmentRepo.findAll()

        then:
        quiz1Res.questions[0].answers[0].answer == "Here is a [Link](${attachment1Href})"

        attachments.size() == 2
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        originalAttachment1.quizId == quiz1.quizId
        !originalAttachment1.projectId
        !originalAttachment1.skillId

        List<Attachment> newAttachments = attachments.findAll {!attachment1Href.contains(it.uuid) }

        newAttachments.size() == 1
        quiz1Res.questions[1].answers[0].answer == "Here is a [Link](/api/download/${newAttachments[0].uuid})"
        newAttachments[0].quizId == quiz1.quizId
        !newAttachments[0].projectId
        !newAttachments[0].skillId
    }

    def "reporting same answer multiple times does not produce duplicate attachments"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)

        def attachment1Href = attachFileForQuizAndReturnHref(quiz1.quizId)

        def question1 = QuizDefFactory.createTextInputQuestion(1, 1)
        question1.id = skillsService.createQuizQuestionDef(question1).body.id

        def quizAttempt = skillsService.startQuizAttempt(quiz1.quizId).body
        skillsService.reportQuizAnswer(quiz1.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [isSelected: true, answerText:  "Here is a [Link](${attachment1Href})".toString()])

        when:
        def quiz1Res = skillsService.getQuizAttemptResult(quiz1.quizId, quizAttempt.id)
        List<Attachment> attachments = attachmentRepo.findAll()
        skillsService.reportQuizAnswer(quiz1.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [isSelected: true, answerText:  "Here is a [Link](${attachment1Href})".toString()])

        def quiz1ResAfter = skillsService.getQuizAttemptResult(quiz1.quizId, quizAttempt.id)
        List<Attachment> attachmentsAfter = attachmentRepo.findAll()

        then:
        quiz1Res.questions[0].answers[0].answer == "Here is a [Link](${attachment1Href})"
        quiz1ResAfter.questions[0].answers[0].answer == "Here is a [Link](${attachment1Href})"

        attachments.size() == 1
        attachment1Href.contains(attachments[0].uuid)
        attachments[0].quizId == quiz1.quizId
        !attachments[0].projectId
        !attachments[0].skillId

        attachmentsAfter.size() == 1
        attachment1Href.contains(attachmentsAfter[0].uuid)
        attachmentsAfter[0].quizId == quiz1.quizId
        !attachmentsAfter[0].projectId
        !attachmentsAfter[0].skillId
    }

    def "from another quiz: question -> answer - multiple attachments"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)

        def attachment1Href = attachFileForQuizAndReturnHref(quiz1.quizId)
        def attachment2Href = attachFileForQuizAndReturnHref(quiz1.quizId)

        def question1 = QuizDefFactory.createTextInputQuestion(1, 1)
        question1.question = "Here is a [Link](${attachment1Href})\nHere is a [Link](${attachment2Href})".toString()
        skillsService.createQuizQuestionDef(question1)

        def quiz2 = QuizDefFactory.createQuiz(2)
        skillsService.createQuizDef(quiz2)

        def question2 = QuizDefFactory.createTextInputQuestion(2, 2)
        question2.id = skillsService.createQuizQuestionDef(question2).body.id

        when:
        def quizAttempt = skillsService.startQuizAttempt(quiz2.quizId).body
        skillsService.reportQuizAnswer(quiz2.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [isSelected: true, answerText:  "Here is a [Link](${attachment1Href})\nHere is a [Link](${attachment2Href})".toString()])

        def quiz1Res = skillsService.getQuizQuestionDefs(quiz1.quizId)
        def quiz2Res = skillsService.getQuizAttemptResult(quiz2.quizId, quizAttempt.id)
        List<Attachment> attachments = attachmentRepo.findAll()

        then:
        quiz1Res.questions[0].question == "Here is a [Link](${attachment1Href})\nHere is a [Link](${attachment2Href})".toString()

        attachments.size() == 4
        List<Attachment> originalAttachments = attachments.findAll {  attachment1Href.contains(it.uuid) || attachment2Href.contains(it.uuid)}
        originalAttachments.size() == 2
        originalAttachments.each {
            assert it.quizId == quiz1.quizId
            assert !it.projectId
            assert !it.skillId
        }
        originalAttachments.collect { "/api/download/${it.uuid}"}.sort() == [attachment1Href, attachment2Href].sort()

        List<Attachment> newAttachments = attachments.findAll {!attachment1Href.contains(it.uuid) && !attachment2Href.contains(it.uuid)}

        newAttachments.size() == 2
        quiz2Res.questions[0].answers[0].answer.contains(newAttachments[0].uuid)
        quiz2Res.questions[0].answers[0].answer.contains(newAttachments[1].uuid)
        newAttachments.each {
            assert it.quizId == quiz2.quizId
            assert !it.projectId
            assert !it.skillId
        }
    }

    def "from a project: skill -> answer"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def attachment1Href = attachFileAndReturnHref(p1.projectId)

        def p1Skills = createSkills(2, 1, 1, 100)
        p1Skills[0].description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.createSkills(p1Skills)

        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)

        def question1 = QuizDefFactory.createTextInputQuestion(1, 1)
        question1.id = skillsService.createQuizQuestionDef(question1).body.id

        when:
        def quizAttempt = skillsService.startQuizAttempt(quiz1.quizId).body
        skillsService.reportQuizAnswer(quiz1.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [isSelected: true, answerText:  "Here is a [Link](${attachment1Href})".toString()])

        def skillRes = skillsService.getSkill(p1Skills[0])
        def quiz1Res = skillsService.getQuizAttemptResult(quiz1.quizId, quizAttempt.id)
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
        quiz1Res.questions[0].answers[0].answer == "Here is a [Link](/api/download/${newAttachments[0].uuid})"
        newAttachments[0].quizId == quiz1.quizId
        !newAttachments[0].projectId
        !newAttachments[0].skillId
    }

    def "from a project: project -> answer"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def attachment1Href = attachFileAndReturnHref(p1.projectId)

        p1.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.updateProject(p1)

        def p1Skills = createSkills(2, 1, 1, 100)
        skillsService.createSkills(p1Skills)

        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)

        def question1 = QuizDefFactory.createTextInputQuestion(1, 1)
        question1.id = skillsService.createQuizQuestionDef(question1).body.id

        when:
        def quizAttempt = skillsService.startQuizAttempt(quiz1.quizId).body
        skillsService.reportQuizAnswer(quiz1.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [isSelected: true, answerText:  "Here is a [Link](${attachment1Href})".toString()])

        def projRes = skillsService.getProjectDescription(p1.projectId)
        def quiz1Res = skillsService.getQuizAttemptResult(quiz1.quizId, quizAttempt.id)
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
        quiz1Res.questions[0].answers[0].answer == "Here is a [Link](/api/download/${newAttachments[0].uuid})"
        newAttachments[0].quizId == quiz1.quizId
        !newAttachments[0].projectId
        !newAttachments[0].skillId
    }

    def "from a Global Badge: gb -> answer"() {
        def badge = createBadge(1, 1)
        skillsService.createGlobalBadge(badge)

        def attachment1Href = attachFileForGlobalBadgeAndReturnHref(badge.badgeId)

        badge.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.updateGlobalBadge(badge)

        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)

        def question1 = QuizDefFactory.createTextInputQuestion(1, 1)
        question1.id = skillsService.createQuizQuestionDef(question1).body.id

        when:
        def quizAttempt = skillsService.startQuizAttempt(quiz1.quizId).body
        skillsService.reportQuizAnswer(quiz1.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [isSelected: true, answerText:  "Here is a [Link](${attachment1Href})".toString()])

        def badgeRes = skillsService.getGlobalBadge(badge.badgeId)
        def quiz1Res = skillsService.getQuizAttemptResult(quiz1.quizId, quizAttempt.id)
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
        quiz1Res.questions[0].answers[0].answer == "Here is a [Link](/api/download/${newAttachments[0].uuid})"
        newAttachments[0].quizId == quiz1.quizId
        !newAttachments[0].projectId
        !newAttachments[0].skillId
    }

}
