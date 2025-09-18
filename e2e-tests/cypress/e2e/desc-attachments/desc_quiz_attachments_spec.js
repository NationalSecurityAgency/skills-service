/*
 * Copyright 2025 SkillTree
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
import './desc-attachment-commands'

describe('Description Quiz Attachments Tests', () => {

    const attachmentBtnSelector = '[data-cy="markdownEditorInput"] button.attachment-button'
    const markdownInput = '[data-cy=markdownEditorInput] div.toastui-editor-contents[contenteditable="true"]'
    const markdownEditorToolbarIconsSelector = '[data-cy="markdownEditorInput"] button.toastui-editor-toolbar-icons'

    it('attachments are not enabled on quiz creation', () => {
        cy.viewport(1400, 1000)
        cy.visit('/administrator/quizzes/')
        cy.get('[data-cy="noQuizzesYet"]')

        cy.get('[data-cy="btn_Quizzes And Surveys"]').click()
        cy.get('.p-dialog-header').contains('New Quiz/Survey')
        cy.get(markdownEditorToolbarIconsSelector).should('be.visible')
        cy.get(attachmentBtnSelector).should('not.exist');
    });

    it('can add attachments when editing a quiz', () => {
        cy.viewport(1400, 1000)
        cy.createQuizDef(1)
        cy.visit('/administrator/quizzes/')

        cy.get('[data-cy="editQuizButton_quiz1"]').click()
        cy.get('[data-p="modal"] [data-pc-section="title"]').contains('Editing Existing Quiz/Survey')
        cy.get(markdownEditorToolbarIconsSelector).should('be.visible')
        cy.get(attachmentBtnSelector).should('be.visible');

        cy.addAttachment(attachmentBtnSelector)
        cy.validateAttachmentInDb('quiz_id', 'quiz1')

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-p="modal"]').should('not.exist')

        cy.validateAttachmentInDb('quiz_id', 'quiz1')
    });

    it('can add attachments when editing a quiz - from quiz page', () => {
        cy.viewport(1400, 1000)
        cy.createQuizDef(1)
        cy.visit('/administrator/quizzes/quiz1')

        cy.get('[data-cy="editQuizButton"]').click()
        cy.get('[data-p="modal"] [data-pc-section="title"]').contains('Editing Existing Quiz/Survey')
        cy.get(markdownEditorToolbarIconsSelector).should('be.visible')
        cy.get(attachmentBtnSelector).should('be.visible');

        cy.addAttachment(attachmentBtnSelector)
        cy.validateAttachmentInDb('quiz_id', 'quiz1')

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-p="modal"]').should('not.exist')

        cy.validateAttachmentInDb('quiz_id', 'quiz1')
    });

    it('can add attachments when creating a question', () => {
        cy.viewport(1400, 1000)
        cy.createQuizDef(1)
        cy.visit('/administrator/quizzes/quiz1')

        cy.get('[data-cy="btn_Questions"]').click()

        cy.get('[data-p="modal"] [data-pc-section="title"]').contains('New Question')
        cy.get(markdownEditorToolbarIconsSelector).should('be.visible')
        cy.get(attachmentBtnSelector).should('be.visible');

        cy.addAttachment(attachmentBtnSelector)
        cy.validateAttachmentInDb('quiz_id', 'quiz1')

        cy.get('[data-cy="answerTypeSelector"]').click()
        cy.get('[data-cy="selectionItem_SingleChoice"]').click()
        cy.get('[data-cy="answer-0"] [data-cy="answerText"]').type('1')
        cy.get('[data-cy="answer-1"] [data-cy="answerText"]').type('4')
        cy.get('[data-cy="answer-1"] [data-cy="selectCorrectAnswer"]').click()
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-p="modal"]').should('not.exist')

        cy.validateAttachmentInDb('quiz_id', 'quiz1')
    });

    it('can add attachments when editing a question', () => {
        cy.viewport(1400, 1000)
        cy.createQuizDef(1)
        cy.createQuizQuestionDef(1, 1)
        cy.visit('/administrator/quizzes/quiz1')

        cy.get('[data-cy="editQuestionButton_1"]').click()

        cy.get('[data-p="modal"] [data-pc-section="title"]').contains('Editing Existing Question')
        cy.get(markdownEditorToolbarIconsSelector).should('be.visible')
        cy.get(attachmentBtnSelector).should('be.visible');

        cy.addAttachment(attachmentBtnSelector)
        cy.validateAttachmentInDb('quiz_id', 'quiz1')

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-p="modal"]').should('not.exist')

        cy.validateAttachmentInDb('quiz_id', 'quiz1')
    });

    it('attachments are not enabled for quiz text answers', () => {
        cy.viewport(1400, 1000)
        cy.createQuizDef(1);
        cy.createTextInputQuestionDef(1, 1)
        cy.visit('/progress-and-rankings/quizzes/quiz1')
        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get(`[data-cy="question_1"] ${markdownEditorToolbarIconsSelector}`).should('be.visible')
        cy.get(`[data-cy="question_1"] ${attachmentBtnSelector}`).should('not.exist');
    })

    it('attachments are not enabled for quiz text-based questions grading input', () => {
        cy.viewport(1400, 1000)
        cy.createQuizDef(1);
        cy.createTextInputQuestionDef(1, 1)

        cy.runQuizForUser(1, 1, [{selectedIndex: [0]}], true, '**My Answer**')

        cy.visit('/administrator/quizzes/quiz1/grading');
        cy.get('[data-cy="gradeBtn_user1"]').should('be.enabled').click()
        cy.get('[data-cy="feedbackTxtMarkdownEditor"] [data-cy="markdownEditorInput"] button.toastui-editor-toolbar-icons').should('be.visible')
        cy.get(`[data-cy="feedbackTxtMarkdownEditor"] ${attachmentBtnSelector}`).should('not.exist');
    })

});
