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
package skills.intTests.quiz

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import skills.PublicProps
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.services.quiz.QuizQuestionType
import skills.storage.model.QuizDefParent.QuizType
import skills.storage.repos.QuizQuestionDefRepo
import skills.storage.repos.AttachmentRepo

class QuizQuestionCopySpecs extends DefaultIntSpec  {

    @Autowired
    QuizQuestionDefRepo quizQuestionDefRepo

    @Autowired
    PublicProps publicProps

    @Autowired
    AttachmentRepo attachmentRepo

    def "direct selected-question copy rejects #sourceType to #destinationType"() {
        setup:
        SkillsService admin = createService(getRandomUsers(1)[0])
        Map sourceQuiz = sourceType == QuizType.Quiz ? QuizDefFactory.createQuiz(1) : QuizDefFactory.createQuizSurvey(1)
        Map destinationQuiz = destinationType == QuizType.Quiz ? QuizDefFactory.createQuiz(2) : QuizDefFactory.createQuizSurvey(2)
        admin.createQuizDef(sourceQuiz)
        admin.createQuizDef(destinationQuiz)
        Map sourceQuestion = QuizDefFactory.createChoiceQuestion(1, 1, 2, QuizQuestionType.SingleChoice, sourceType)
        sourceQuestion.question = 'Source question must not be copied'
        Map createdSourceQuestion = admin.createQuizQuestionDef(sourceQuestion).body
        Map destinationQuestion = QuizDefFactory.createChoiceQuestion(2, 1, 2, QuizQuestionType.SingleChoice, destinationType)
        destinationQuestion.question = 'Existing destination question'
        Map createdDestinationQuestion = admin.createQuizQuestionDef(destinationQuestion).body

        when: 'a direct API request bypasses destination filtering in the UI'
        admin.copyQuestions(sourceQuiz.quizId, destinationQuiz.quizId, [createdSourceQuestion.id])

        then:
        SkillsClientException error = thrown()
        error.httpStatus == HttpStatus.BAD_REQUEST
        error.message.contains('errorCode:BadParam')
        error.message.contains("Can not copy questions between quizzes and surveys - [${sourceQuiz.quizId}], [${destinationQuiz.quizId}]")

        and: 'the existing destination definition and source remain unchanged'
        List<Map> destinationQuestions = admin.getQuizQuestionDefs(destinationQuiz.quizId).questions
        destinationQuestions*.id == [createdDestinationQuestion.id]
        destinationQuestions.first().question == 'Existing destination question'
        destinationQuestions.first().answers*.answer == ['Answer #1', 'Answer #2']
        destinationQuestions.first().answers*.isCorrect == (destinationType == QuizType.Quiz ? [true, false] : [false, false])
        admin.getQuizQuestionDefs(sourceQuiz.quizId).questions*.id == [createdSourceQuestion.id]

        where:
        sourceType      | destinationType
        QuizType.Quiz   | QuizType.Survey
        QuizType.Survey | QuizType.Quiz
    }

    def "root copies selected questions using canonical quiz ids with source #sourceCase and destination #destinationCase"() {
        setup:
        SkillsService rootUser = createRootSkillService(getRandomUsers(1)[0])
        Map sourceQuiz = QuizDefFactory.createQuiz(1)
        Map destinationQuiz = QuizDefFactory.createQuiz(2)
        skillsService.createQuizDef(sourceQuiz)
        skillsService.createQuizDef(destinationQuiz)
        Map selectedQuestion = QuizDefFactory.createChoiceQuestion(1, 1, 2)
        selectedQuestion.question = 'Selected source question'
        selectedQuestion.answerHint = 'Selected source hint'
        Map createdSourceQuestion = skillsService.createQuizQuestionDef(selectedQuestion).body
        Map unselectedSourceQuestion = skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 2, 2)).body
        Map existingDestinationQuestion = skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(2, 1, 2)).body
        String sourceId = sourceCase == 'stored' ? sourceQuiz.quizId :
                sourceCase == 'lower' ? sourceQuiz.quizId.toLowerCase(Locale.ROOT) : sourceQuiz.quizId.toUpperCase(Locale.ROOT)
        String destinationId = destinationCase == 'stored' ? destinationQuiz.quizId :
                destinationCase == 'lower' ? destinationQuiz.quizId.toLowerCase(Locale.ROOT) : destinationQuiz.quizId.toUpperCase(Locale.ROOT)

        when:
        def response = rootUser.copyQuestions(sourceId, destinationId, [createdSourceQuestion.id])
        List<Map> destinationQuestions = skillsService.getQuizQuestionDefs(destinationQuiz.quizId).questions

        then:
        response.body.success
        destinationQuestions.size() == 2
        destinationQuestions.first().id == existingDestinationQuestion.id
        Map copiedQuestion = destinationQuestions.last()
        copiedQuestion.id != createdSourceQuestion.id
        copiedQuestion.question == 'Selected source question'
        copiedQuestion.answerHint == 'Selected source hint'
        copiedQuestion.questionType == 'SingleChoice'
        copiedQuestion.displayOrder == 1
        copiedQuestion.answers*.answer == ['Answer #1', 'Answer #2']
        copiedQuestion.answers*.isCorrect == [true, false]
        copiedQuestion.answers*.displayOrder == [1, 2]
        !copiedQuestion.answers*.id.intersect(createdSourceQuestion.answers*.id)
        skillsService.getQuizQuestionDefs(sourceQuiz.quizId).questions*.id == [createdSourceQuestion.id, unselectedSourceQuestion.id]

        where:
        sourceCase | destinationCase
        'lower'    | 'stored'
        'upper'    | 'stored'
        'stored'   | 'lower'
        'stored'   | 'upper'
        'lower'    | 'upper'
        'upper'    | 'lower'
    }

    def "root copy enforces destination capacity with a #destinationCase destination id"() {
        setup:
        SkillsService rootUser = createRootSkillService(getRandomUsers(1)[0])
        Map sourceQuiz = QuizDefFactory.createQuiz(1)
        Map destinationQuiz = QuizDefFactory.createQuiz(2)
        skillsService.createQuizDef(sourceQuiz)
        skillsService.createQuizDef(destinationQuiz)
        Map sourceQuestion = skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 2)).body
        int maxQuestions = publicProps.getInt(PublicProps.UiProp.maxQuestionsPerQuiz)
        skillsService.createQuizQuestionDefs(QuizDefFactory.createChoiceQuestions(2, maxQuestions, 2))
        List<Map> existingQuestions = skillsService.getQuizQuestionDefs(destinationQuiz.quizId).questions
        String destinationId = destinationCase == 'lower' ? destinationQuiz.quizId.toLowerCase(Locale.ROOT) :
                destinationQuiz.quizId.toUpperCase(Locale.ROOT)

        when:
        rootUser.copyQuestions(sourceQuiz.quizId, destinationId, [sourceQuestion.id])

        then:
        SkillsClientException error = thrown()
        error.httpStatus == HttpStatus.BAD_REQUEST
        error.message.contains("Questions would exceed the maximum number of questions [${maxQuestions}] in [${destinationQuiz.quizId}]")
        skillsService.getQuizQuestionDefs(destinationQuiz.quizId).questions*.id == existingQuestions*.id
        skillsService.getQuizQuestionDefs(sourceQuiz.quizId).questions*.id == [sourceQuestion.id]

        where:
        destinationCase << ['lower', 'upper']
    }

    def "empty selected question ids cannot copy the entire quiz"() {
        def sourceQuiz = QuizDefFactory.createQuiz(1)
        def destinationQuiz = QuizDefFactory.createQuiz(2)
        skillsService.createQuizDef(sourceQuiz)
        skillsService.createQuizDef(destinationQuiz)
        skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 2))

        when:
        skillsService.copyQuestions(sourceQuiz.quizId, destinationQuiz.quizId, [])

        then:
        SkillsClientException error = thrown()
        error.httpStatus == HttpStatus.BAD_REQUEST
        error.message.contains('At least one question id must be provided')
        skillsService.getQuizQuestionDefs(destinationQuiz.quizId).questions.empty
        skillsService.getQuizQuestionDefs(sourceQuiz.quizId).questions.size() == 1
    }

    def "selected question ids must all belong to the source quiz"() {
        def sourceQuiz = QuizDefFactory.createQuiz(1)
        def destinationQuiz = QuizDefFactory.createQuiz(2)
        def otherQuiz = QuizDefFactory.createQuiz(3)
        skillsService.createQuizDef(sourceQuiz)
        skillsService.createQuizDef(destinationQuiz)
        skillsService.createQuizDef(otherQuiz)
        def sourceQuestion = skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 2)).body
        def otherQuestion = skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(3, 1, 2)).body

        when: 'a request includes an id from another quiz'
        skillsService.copyQuestions(sourceQuiz.quizId, destinationQuiz.quizId, [sourceQuestion.id, otherQuestion.id])

        then:
        SkillsClientException foreignId = thrown()
        foreignId.httpStatus == HttpStatus.BAD_REQUEST
        foreignId.message.contains("Question ids [${otherQuestion.id}] do not belong to quiz [${sourceQuiz.quizId}]")
        skillsService.getQuizQuestionDefs(destinationQuiz.quizId).questions.empty

        when: 'a request includes a nonexistent id'
        Integer nonexistentId = Math.max(sourceQuestion.id, otherQuestion.id) + 1_000_000
        skillsService.copyQuestions(sourceQuiz.quizId, destinationQuiz.quizId, [sourceQuestion.id, nonexistentId])

        then:
        SkillsClientException missingId = thrown()
        missingId.httpStatus == HttpStatus.BAD_REQUEST
        missingId.message.contains("Question ids [${nonexistentId}] do not belong to quiz [${sourceQuiz.quizId}]")
        skillsService.getQuizQuestionDefs(destinationQuiz.quizId).questions.empty
    }

    def "copy refuses questions that would exceed the maximum allowed in the destination quiz"() {
        int maxQuestions = publicProps.getInt(PublicProps.UiProp.maxQuestionsPerQuiz)
        def sourceQuiz = QuizDefFactory.createQuiz(1)
        def destinationQuiz = QuizDefFactory.createQuiz(2)
        skillsService.createQuizDef(sourceQuiz)
        skillsService.createQuizDef(destinationQuiz)
        def sourceQuestion = skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 2)).body
        skillsService.createQuizQuestionDefs(QuizDefFactory.createChoiceQuestions(2, maxQuestions, 2))
        def existingQuestions = skillsService.getQuizQuestionDefs(destinationQuiz.quizId).questions

        when:
        skillsService.copyQuestions(sourceQuiz.quizId, destinationQuiz.quizId, [sourceQuestion.id])

        then:
        SkillsClientException error = thrown()
        error.httpStatus == HttpStatus.BAD_REQUEST
        error.message.contains("Questions would exceed the maximum number of questions [${maxQuestions}] in [${destinationQuiz.quizId}]")
        skillsService.getQuizQuestionDefs(destinationQuiz.quizId).questions*.id == existingQuestions*.id
        skillsService.getQuizQuestionDefs(sourceQuiz.quizId).questions*.id == [sourceQuestion.id]
    }

    def "copy refuses a batch of questions that would exceed the maximum allowed in the destination quiz"() {
        int maxQuestions = publicProps.getInt(PublicProps.UiProp.maxQuestionsPerQuiz)
        def sourceQuiz = QuizDefFactory.createQuiz(1)
        def destinationQuiz = QuizDefFactory.createQuiz(2)
        skillsService.createQuizDef(sourceQuiz)
        skillsService.createQuizDef(destinationQuiz)
        def sourceQuestions = (1..2).collect { int number ->
            skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, number, 2)).body
        }
        skillsService.createQuizQuestionDefs(QuizDefFactory.createChoiceQuestions(2, maxQuestions - 1, 2))
        def existingQuestions = skillsService.getQuizQuestionDefs(destinationQuiz.quizId).questions

        when:
        skillsService.copyQuestions(sourceQuiz.quizId, destinationQuiz.quizId, sourceQuestions*.id)

        then:
        SkillsClientException error = thrown()
        error.httpStatus == HttpStatus.BAD_REQUEST
        error.message.contains("Questions would exceed the maximum number of questions [${maxQuestions}] in [${destinationQuiz.quizId}]")
        skillsService.getQuizQuestionDefs(destinationQuiz.quizId).questions*.id == existingQuestions*.id
        skillsService.getQuizQuestionDefs(sourceQuiz.quizId).questions*.id == sourceQuestions*.id
    }

    def "selected questions append in source order after existing destination questions"() {
        def sourceQuiz = QuizDefFactory.createQuiz(1)
        def destinationQuiz = QuizDefFactory.createQuiz(2)
        skillsService.createQuizDef(sourceQuiz)
        skillsService.createQuizDef(destinationQuiz)
        def sourceQuestions = (1..4).collect { number ->
            skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, number, 2)).body
        }
        def existingQuestions = (1..2).collect { number ->
            skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(2, number, 2)).body
        }

        when:
        def copied = skillsService.copyQuestions(sourceQuiz.quizId, destinationQuiz.quizId,
                [sourceQuestions[3].id, sourceQuestions[1].id])
        def destinationQuestions = skillsService.getQuizQuestionDefs(destinationQuiz.quizId).questions

        then:
        copied.body.success
        destinationQuestions*.question == [existingQuestions[0].question, existingQuestions[1].question,
                                           sourceQuestions[1].question, sourceQuestions[3].question]
        destinationQuestions*.displayOrder == [0, 1, 2, 3]
    }

    def "selected questions copied to an empty quiz preserve their definitions and answer order"() {
        def sourceQuiz = QuizDefFactory.createQuiz(1)
        def destinationQuiz = QuizDefFactory.createQuiz(2)
        skillsService.createQuizDef(sourceQuiz)
        skillsService.createQuizDef(destinationQuiz)
        def sourceQuestions = [
                QuizDefFactory.createChoiceQuestion(1, 1, 3, QuizQuestionType.SingleChoice),
                QuizDefFactory.createChoiceQuestion(1, 2, 4, QuizQuestionType.MultipleChoice),
                QuizDefFactory.createChoiceQuestion(1, 3, 2, QuizQuestionType.SingleChoice),
        ].collect { skillsService.createQuizQuestionDef(it).body }

        when:
        def copied = skillsService.copyQuestions(sourceQuiz.quizId, destinationQuiz.quizId,
                [sourceQuestions[2].id, sourceQuestions[0].id])
        def destinationQuestions = skillsService.getQuizQuestionDefs(destinationQuiz.quizId).questions

        then:
        copied.body.success
        destinationQuestions.size() == 2
        destinationQuestions*.displayOrder == [0, 1]
        [sourceQuestions[0], sourceQuestions[2]].eachWithIndex { source, index ->
            def copiedQuestion = destinationQuestions[index]
            assert copiedQuestion.id != source.id
            assert copiedQuestion.question == source.question
            assert copiedQuestion.answerHint == source.answerHint
            assert copiedQuestion.questionType == source.questionType
            assert copiedQuestion.answers*.answer == source.answers*.answer
            assert copiedQuestion.answers*.isCorrect == source.answers*.isCorrect
            assert copiedQuestion.answers*.displayOrder == source.answers*.displayOrder
            assert !copiedQuestion.answers*.id.intersect(source.answers*.id)
        }
        skillsService.getQuizQuestionDefs(sourceQuiz.quizId).questions*.id == sourceQuestions*.id
    }

    def "selected-question copy preserves attachments #videoHosting video and AI settings"() {
        setup:
        List<String> users = getRandomUsers(2)
        SkillsService sourceAdmin = createService(users[0])
        SkillsService destinationAdmin = createService(users[1])
        Map sourceQuiz = QuizDefFactory.createQuiz(1)
        Map destinationQuiz = QuizDefFactory.createQuiz(2)
        sourceAdmin.createQuizDef(sourceQuiz)
        destinationAdmin.createQuizDef(destinationQuiz)
        destinationAdmin.addQuizUserRole(destinationQuiz.quizId, sourceAdmin.userName, 'ROLE_QUIZ_ADMIN')
        String selectedAttachmentHref = sourceAdmin.uploadAttachment('selected.pdf', 'Selected attachment contents',
                null, null, sourceQuiz.quizId).href
        String unselectedAttachmentHref = sourceAdmin.uploadAttachment('unselected.pdf', 'Unselected attachment contents',
                null, null, sourceQuiz.quizId).href
        Map selectedQuestion = QuizDefFactory.createTextInputQuestion(1, 1)
        selectedQuestion.question = "Selected question [Attachment](${selectedAttachmentHref})".toString()
        selectedQuestion.answerHint = 'Selected answer hint'
        Map createdSelectedQuestion = sourceAdmin.createQuizQuestionDef(selectedQuestion).body
        Map unselectedQuestion = QuizDefFactory.createTextInputQuestion(1, 2)
        unselectedQuestion.question = "Unselected question [Attachment](${unselectedAttachmentHref})".toString()
        Map createdUnselectedQuestion = sourceAdmin.createQuizQuestionDef(unselectedQuestion).body
        Map plainQuestion = QuizDefFactory.createTextInputQuestion(1, 3)
        plainQuestion.question = 'Selected plain question without media or AI settings'
        Map createdPlainQuestion = sourceAdmin.createQuizQuestionDef(plainQuestion).body

        Resource video = new ClassPathResource('/testVideos/create-quiz.mp4')
        Map videoConfig = [transcript: 'Selected video transcript', captions: 'Selected captions',
                           height: 360, width: 640, allowDownloads: true]
        if (videoHosting == 'uploaded') {
            videoConfig.file = video
        } else {
            videoConfig.videoUrl = 'https://example.org/selected.mp4'
        }
        sourceAdmin.saveSkillVideoAttributes(sourceQuiz.quizId, createdSelectedQuestion.id.toString(), videoConfig, true)
        sourceAdmin.saveQuizTextInputAiGraderConfigs(sourceQuiz.quizId, createdSelectedQuestion.id, 'Selected correct answer', 62)
        sourceAdmin.saveSkillVideoAttributes(sourceQuiz.quizId, createdUnselectedQuestion.id.toString(),
                [videoUrl: 'https://example.org/unselected.mp4', transcript: 'Unselected transcript'], true)
        sourceAdmin.saveQuizTextInputAiGraderConfigs(sourceQuiz.quizId, createdUnselectedQuestion.id, 'Unselected correct answer', 54)

        Map existingQuestion = QuizDefFactory.createTextInputQuestion(2, 1)
        existingQuestion.question = 'Existing destination question'
        Map createdExistingQuestion = destinationAdmin.createQuizQuestionDef(existingQuestion).body
        destinationAdmin.saveQuizTextInputAiGraderConfigs(destinationQuiz.quizId, createdExistingQuestion.id,
                'Existing destination correct answer', 88)
        Map existingDestinationQuestion = destinationAdmin.getQuizQuestionDefs(destinationQuiz.quizId).questions.first()
        Map sourceBefore = sourceAdmin.getQuizQuestionDefs(sourceQuiz.quizId)
        Map sourceVideoBefore = sourceAdmin.getSkillVideoAttributes(sourceQuiz.quizId, createdSelectedQuestion.id.toString(), true)

        when:
        def response = sourceAdmin.copyQuestions(sourceQuiz.quizId, destinationQuiz.quizId,
                [createdPlainQuestion.id, createdSelectedQuestion.id])
        List<Map> destinationQuestions = destinationAdmin.getQuizQuestionDefs(destinationQuiz.quizId).questions

        then: 'only selected questions append, preserving their source order and definitions'
        response.body.success
        destinationQuestions.size() == 3
        destinationQuestions.first() == existingDestinationQuestion
        destinationQuestions*.displayOrder == [0, 1, 2]
        Map copiedQuestion = destinationQuestions[1]
        copiedQuestion.id != createdSelectedQuestion.id
        copiedQuestion.questionType == 'TextInput'
        copiedQuestion.answerHint == 'Selected answer hint'
        copiedQuestion.answers.size() == 1
        copiedQuestion.answers.first().answer == null
        !copiedQuestion.answers.first().isCorrect
        destinationQuestions[2].question == 'Selected plain question without media or AI settings'
        !destinationQuestions.any { Map question -> question.question.contains('Unselected question') }

        and: 'the description attachment is an independently accessible destination-owned copy'
        String copiedAttachmentUuid = (copiedQuestion.question =~ /\/api\/download\/([a-f0-9-]+)/)[0][1]
        String copiedAttachmentHref = "/api/download/${copiedAttachmentUuid}".toString()
        copiedAttachmentHref != selectedAttachmentHref
        copiedQuestion.question == "Selected question [Attachment](${copiedAttachmentHref})"
        destinationAdmin.downloadAttachmentAsText(copiedAttachmentHref) == 'Selected attachment contents'
        attachmentRepo.findByUuid(copiedAttachmentUuid).quizId == destinationQuiz.quizId
        attachmentRepo.findByUuid(copiedAttachmentUuid).filename == 'selected.pdf'

        and: 'the copied video retains its metadata and uploaded video has an independent attachment'
        Map copiedVideo = destinationAdmin.getSkillVideoAttributes(destinationQuiz.quizId, copiedQuestion.id.toString(), true)
        copiedVideo.transcript == 'Selected video transcript'
        copiedVideo.captions == 'Selected captions'
        copiedVideo.height == 360
        copiedVideo.width == 640
        copiedVideo.allowDownloads
        copiedVideo.isInternallyHosted == (videoHosting == 'uploaded')
        if (videoHosting == 'uploaded') {
            assert copiedVideo.videoUrl.startsWith('/api/download/')
            assert copiedVideo.videoUrl != sourceVideoBefore.videoUrl
            String copiedVideoUuid = copiedVideo.videoUrl.tokenize('/').last()
            assert attachmentRepo.findByUuid(copiedVideoUuid).quizId == destinationQuiz.quizId
            assert attachmentRepo.findByUuid(copiedVideoUuid).filename == 'create-quiz.mp4'
            byte[] expectedVideo = video.inputStream.withCloseable { InputStream stream -> stream.bytes }
            assert destinationAdmin.downloadAttachment(copiedVideo.videoUrl).file.bytes == expectedVideo
            assert sourceAdmin.downloadAttachment(sourceVideoBefore.videoUrl).file.bytes == expectedVideo
        } else {
            assert copiedVideo.videoUrl == 'https://example.org/selected.mp4'
        }

        and: 'AI grading settings are preserved without leaking to an unconfigured question'
        Map copiedAiSettings = destinationAdmin.getQuizTextInputAiGraderConfigs(destinationQuiz.quizId, copiedQuestion.id)
        copiedAiSettings.enabled
        copiedAiSettings.correctAnswer == 'Selected correct answer'
        copiedAiSettings.minimumConfidenceLevel == 62
        Map plainAiSettings = destinationAdmin.getQuizTextInputAiGraderConfigs(destinationQuiz.quizId, destinationQuestions[2].id)
        !plainAiSettings.enabled
        plainAiSettings.correctAnswer == null
        plainAiSettings.minimumConfidenceLevel == 75
        !destinationQuestions[2].attributes?.videoConf
        Map existingAiSettings = destinationAdmin.getQuizTextInputAiGraderConfigs(destinationQuiz.quizId, createdExistingQuestion.id)
        existingAiSettings.enabled
        existingAiSettings.correctAnswer == 'Existing destination correct answer'
        existingAiSettings.minimumConfidenceLevel == 88

        and: 'copying did not mutate source definitions, video configuration, or attachment contents'
        sourceAdmin.getQuizQuestionDefs(sourceQuiz.quizId) == sourceBefore
        sourceAdmin.getSkillVideoAttributes(sourceQuiz.quizId, createdSelectedQuestion.id.toString(), true) == sourceVideoBefore
        sourceAdmin.downloadAttachmentAsText(selectedAttachmentHref) == 'Selected attachment contents'
        sourceAdmin.downloadAttachmentAsText(unselectedAttachmentHref) == 'Unselected attachment contents'
        sourceAdmin.getQuizTextInputAiGraderConfigs(sourceQuiz.quizId, createdUnselectedQuestion.id).correctAnswer == 'Unselected correct answer'

        where:
        videoHosting << ['external', 'uploaded']
    }

    def "validation failure in a selected question prevents all questions from being copied"() {
        def sourceQuiz = QuizDefFactory.createQuiz(1)
        def destinationQuiz = QuizDefFactory.createQuiz(2)
        skillsService.createQuizDef(sourceQuiz)
        skillsService.createQuizDef(destinationQuiz)
        def validQuestion = skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 2)).body
        def invalidQuestion = skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 2, 2)).body
        // Existing questions can become invalid after the paragraph validation rules change.
        runInTransaction { quizQuestionDefRepo.updateQuestion(invalidQuestion.id, 'jabberwocky') }

        when:
        skillsService.copyQuestions(sourceQuiz.quizId, destinationQuiz.quizId, [validQuestion.id, invalidQuestion.id])

        then:
        SkillsClientException error = thrown()
        error.message.contains("Validation failed for questionId=[${invalidQuestion.id}]")
        skillsService.getQuizQuestionDefs(destinationQuiz.quizId).questions.empty
    }
}
