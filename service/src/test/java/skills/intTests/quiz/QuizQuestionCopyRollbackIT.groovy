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

import org.mockito.invocation.InvocationOnMock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillQuizException
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.services.userActions.DashboardAction
import skills.services.userActions.DashboardItem
import skills.storage.model.Attachment
import skills.storage.model.QuizAnswerDef
import skills.storage.model.QuizQuestionDef
import skills.storage.repos.AttachmentRepo
import skills.storage.repos.QuizAnswerDefRepo
import skills.storage.repos.QuizQuestionDefRepo
import skills.storage.repos.UserActionsHistoryRepo

import java.util.concurrent.atomic.AtomicReference
import java.util.regex.Matcher

import static org.mockito.Mockito.doAnswer

class QuizQuestionCopyRollbackIT extends DefaultIntSpec {

    @MockitoSpyBean
    QuizAnswerDefRepo quizAnswerDefRepo

    @Autowired
    QuizQuestionDefRepo quizQuestionDefRepo

    @Autowired
    AttachmentRepo attachmentRepo

    @Autowired
    UserActionsHistoryRepo userActionsHistoryRepo

    def "failure in a later selected question rolls back questions answers attachments and audit changes"() {
        setup:
        SkillsService admin = createService(getRandomUsers(1)[0])
        Map sourceQuiz = QuizDefFactory.createQuiz(1)
        Map destinationQuiz = QuizDefFactory.createQuiz(2)
        admin.createQuizDef(sourceQuiz)
        admin.createQuizDef(destinationQuiz)
        List<String> sourceAttachmentHrefs = (1..2).collect { int number ->
            admin.uploadAttachment("source${number}.pdf".toString(), "Source attachment ${number}".toString(),
                    null, null, sourceQuiz.quizId).href as String
        }
        List<Map> sourceQuestions = (1..2).collect { int number ->
            Map question = QuizDefFactory.createChoiceQuestion(1, number, 2)
            question.question = "Source question ${number} [Attachment](${sourceAttachmentHrefs[number - 1]})".toString()
            return admin.createQuizQuestionDef(question).body as Map
        }
        String destinationAttachmentHref = admin.uploadAttachment('existing.pdf', 'Existing destination attachment',
                null, null, destinationQuiz.quizId).href
        Map existingQuestion = QuizDefFactory.createChoiceQuestion(2, 1, 2)
        existingQuestion.question = "Existing destination [Attachment](${destinationAttachmentHref})".toString()
        Map createdExistingQuestion = admin.createQuizQuestionDef(existingQuestion).body
        Map sourceBefore = admin.getQuizQuestionDefs(sourceQuiz.quizId)
        Map destinationBefore = admin.getQuizQuestionDefs(destinationQuiz.quizId)
        Map auditBefore = admin.getUserActionsForQuiz(destinationQuiz.quizId, 100, 1, 'created', false,
                DashboardItem.Question, '', '', DashboardAction.Create)
        List<String> originalUuids = (sourceAttachmentHrefs + [destinationAttachmentHref]).collect { String href ->
            href.tokenize('/').last()
        }
        List<Map> attachmentsBefore = attachmentSnapshot(originalUuids)
        AtomicReference<Map> observedWrites = new AtomicReference<>([:])

        // This read is inside the insertion loop, after the first question's
        // answers and audit entry and the second question's attachment were saved.
        // Verify real writes in the request transaction before injecting failure.
        doAnswer({ InvocationOnMock invocation ->
            List<QuizQuestionDef> destinationQuestions = quizQuestionDefRepo.findAllByQuizIdIgnoreCase(destinationQuiz.quizId)
                    .sort { QuizQuestionDef question -> question.displayOrder }
            assert destinationQuestions.size() == 3
            assert destinationQuestions.first().id == createdExistingQuestion.id
            List<QuizQuestionDef> copiedQuestions = destinationQuestions.drop(1)
            assert copiedQuestions*.displayOrder == [1, 2]
            List<QuizAnswerDef> firstCopiedAnswers = quizAnswerDefRepo.findAllByQuestionRefIdIn([copiedQuestions.first().id])
                    .sort { QuizAnswerDef answer -> answer.displayOrder }
            assert firstCopiedAnswers*.answer == ['Answer #1', 'Answer #2']
            assert firstCopiedAnswers*.isCorrectAnswer == ['true', 'false']
            List<String> copiedUuids = copiedQuestions.collect { QuizQuestionDef question -> attachmentUuid(question.question) }
            List<Attachment> copiedAttachments = attachmentRepo.findByUuidIn(copiedUuids)
            assert copiedAttachments.size() == 2
            assert copiedAttachments.every { Attachment attachment -> attachment.quizId == destinationQuiz.quizId }
            assert copiedUuids.intersect(originalUuids).empty
            Page<UserActionsHistoryRepo.UserActionsPreview> audit = userActionsHistoryRepo.getActions(
                    null, destinationQuiz.quizId.toLowerCase(Locale.ROOT), null, 'true', DashboardItem.Question,
                    null, 'true', null, 'true', null, 'true', DashboardAction.Create, PageRequest.of(0, 100))
            assert audit.totalElements == auditBefore.totalCount + 1
            observedWrites.set([
                    questionIds: copiedQuestions*.id,
                    attachmentUuids: copiedUuids,
                    auditIds: audit.content*.id - auditBefore.data*.id,
            ])
            throw new SkillQuizException('Injected failure while copying the second question', destinationQuiz.quizId, ErrorCode.BadParam)
        }).when(quizAnswerDefRepo).findAllByQuestionRefId(sourceQuestions.last().id)

        when:
        // Reverse request order to ensure failure occurs later in source order.
        admin.copyQuestions(sourceQuiz.quizId, destinationQuiz.quizId, sourceQuestions.reverse(false)*.id)

        then:
        SkillsClientException error = thrown()
        error.httpStatus == HttpStatus.BAD_REQUEST
        error.message.contains('Injected failure while copying the second question')
        observedWrites.get().questionIds.size() == 2
        observedWrites.get().auditIds.size() == 1

        and: 'HTTP endpoints show the original questions, answers, and audit history'
        admin.getQuizQuestionDefs(destinationQuiz.quizId) == destinationBefore
        admin.getQuizQuestionDefs(sourceQuiz.quizId) == sourceBefore
        Map auditAfter = admin.getUserActionsForQuiz(destinationQuiz.quizId, 100, 1, 'created', false,
                DashboardItem.Question, '', '', DashboardAction.Create)
        auditAfter.totalCount == auditBefore.totalCount
        auditAfter.data*.id == auditBefore.data*.id
        admin.getQuizUserActionAttributes(destinationQuiz.quizId, auditAfter.data.first().id).question == existingQuestion.question

        and: 'no copied attachments or answers survive, and original files remain intact'
        attachmentRepo.findByUuidIn(observedWrites.get().attachmentUuids).empty
        quizAnswerDefRepo.findAllByQuestionRefIdIn(observedWrites.get().questionIds).empty
        attachmentSnapshot(originalUuids) == attachmentsBefore
        sourceAttachmentHrefs.eachWithIndex { String href, int index ->
            assert admin.downloadAttachmentAsText(href) == "Source attachment ${index + 1}"
        }
        admin.downloadAttachmentAsText(destinationAttachmentHref) == 'Existing destination attachment'
    }

    private List<Map> attachmentSnapshot(List<String> uuids) {
        return attachmentRepo.findByUuidIn(uuids).sort { Attachment attachment -> attachment.id }.collect { Attachment attachment ->
            [id: attachment.id, uuid: attachment.uuid, quizId: attachment.quizId, skillId: attachment.skillId,
             projectId: attachment.projectId, filename: attachment.filename, size: attachment.size]
        }
    }

    private String attachmentUuid(String question) {
        Matcher matcher = question =~ /\/api\/download\/([a-f0-9-]+)/
        assert matcher.find()
        return matcher.group(1)
    }
}
