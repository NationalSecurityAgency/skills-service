/*
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
import dayjs from 'dayjs';
import utcPlugin from 'dayjs/plugin/utc';

dayjs.extend(utcPlugin);

describe('Quiz Question CRUD Tests', () => {

    beforeEach(() => {
        Cypress.Commands.add('validateDisplayAnswer', (qNum, aNum, selected, isSingleChoice) => {
            const correctAnswerCheck = `[data-cy="questionDisplayCard-${qNum}"] [data-cy="answerDisplay-${aNum}"] [data-cy="selectCorrectAnswer"]`;
            if (selected) {
                cy.get(`${correctAnswerCheck} [data-cy="notSelected"]`)
                    .should('not.exist');
                cy.get(`${correctAnswerCheck} [data-cy="selected"]`);

                if (isSingleChoice) {
                    cy.get(`${correctAnswerCheck}  .fa-check-circle`)
                } else {
                    cy.get(`${correctAnswerCheck}  .fa-check-square`)
                }
            } else {
                cy.get(`${correctAnswerCheck} [data-cy="notSelected"]`)
                cy.get(`${correctAnswerCheck} [data-cy="selected"]`) .should('not.exist');

                if (isSingleChoice) {
                    cy.get(`${correctAnswerCheck} .fa-circle`)
                } else {
                    cy.get(`${correctAnswerCheck} .fa-square`)
                }
            }
        });
    });

    it('create single choice questions', function () {
        cy.createQuizDef(1);
        cy.visit('/administrator/quizzes/quiz1');
        cy.get('[data-cy="noQuestionsYet"]')
        cy.get('[data-cy="btn_Questions"]').click()
        cy.get('[data-cy="pageHeaderStat_Questions"] [data-cy="statValue"]').should('have.text', '0')

        cy.get('[data-cy="questionText"]').type('What is 2 + 2?')
        cy.get('[data-cy="answerTypeSelector"]').click()
        cy.get('[data-cy="selectionItem_SingleChoice"]').click()
        cy.get('[data-cy="answer-0"] [data-cy="answerText"]').type('1')
        cy.get('[data-cy="answer-1"] [data-cy="answerText"]').type('4')
        cy.get('[data-cy="answer-1"] [data-cy="selectCorrectAnswer"]').click()

        cy.get('[data-cy="answer-1"] [data-cy="addNewAnswer"]').click()
        cy.get('[data-cy="answer-2"] [data-cy="answerText"]').type('3')

        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')
        cy.get('[data-cy="saveDialogBtn"]').click()

        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="questionDisplayText"]').contains('What is 2 + 2?')
        cy.validateDisplayAnswer(1, 0, false, true)
        cy.validateDisplayAnswer(1, 1, true, true)
        cy.validateDisplayAnswer(1, 2, false, true)
        cy.get('[data-cy="pageHeaderStat_Questions"] [data-cy="statValue"]').should('have.text', '1')
        cy.get('[data-cy="btn_Questions"]').should('have.focus')

        cy.get('[data-cy="newQuestionOnBottomBtn"]').click()
        cy.get('[data-cy="questionText"]').type('What is 1 + 2?')
        cy.get('[data-cy="answer-0"] [data-cy="answerText"]').type('3')
        cy.get('[data-cy="answer-0"] [data-cy="selectCorrectAnswer"]').click()
        cy.get('[data-cy="answer-1"] [data-cy="answerText"]').type('1')

        cy.get('[data-cy="answer-1"] [data-cy="addNewAnswer"]').click()
        cy.get('[data-cy="answer-1"] [data-cy="addNewAnswer"]').click()

        cy.get('[data-cy="answer-2"] [data-cy="answerText"]').type('three')
        cy.get('[data-cy="answer-2"] [data-cy="selectCorrectAnswer"]').click()
        cy.get('[data-cy="answer-3"] [data-cy="answerText"]').type('4')

        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')
        cy.get('[data-cy="saveDialogBtn"]').click()

        // q2
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="questionDisplayText"]').contains('What is 1 + 2?')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answer-0_displayText"]').should('have.text', '3')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answer-1_displayText"]').should('have.text', '1')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answer-2_displayText"]').should('have.text', 'three')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answer-3_displayText"]').should('have.text', '4')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answer-4_displayText"]').should('not.exist')
        cy.validateDisplayAnswer(2, 0, true, false)
        cy.validateDisplayAnswer(2, 1, false, false)
        cy.validateDisplayAnswer(2, 2, true, false)
        cy.validateDisplayAnswer(2, 3, false, false)
        // q1
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="questionDisplayText"]').contains('What is 2 + 2?')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answer-0_displayText"]').should('have.text', '1')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answer-1_displayText"]').should('have.text', '4')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answer-2_displayText"]').should('have.text', '3')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answer-3_displayText"]').should('not.exist')
        cy.validateDisplayAnswer(1, 0, false, true)
        cy.validateDisplayAnswer(1, 1, true, true)
        cy.validateDisplayAnswer(1, 2, false, true)
        cy.get('[data-cy="pageHeaderStat_Questions"] [data-cy="statValue"]').should('have.text', '2')
        cy.get('[data-cy="newQuestionOnBottomBtn"]').should('have.focus')

        cy.visit('/administrator/quizzes/quiz1');
        // q2
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="questionDisplayText"]').contains('What is 1 + 2?')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answer-0_displayText"]').should('have.text', '3')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answer-1_displayText"]').should('have.text', '1')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answer-2_displayText"]').should('have.text', 'three')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answer-3_displayText"]').should('have.text', '4')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answer-4_displayText"]').should('not.exist')
        cy.validateDisplayAnswer(2, 0, true, false)
        cy.validateDisplayAnswer(2, 1, false, false)
        cy.validateDisplayAnswer(2, 2, true, false)
        cy.validateDisplayAnswer(2, 3, false, false)
        // q1
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="questionDisplayText"]').contains('What is 2 + 2?')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answer-0_displayText"]').should('have.text', '1')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answer-1_displayText"]').should('have.text', '4')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answer-2_displayText"]').should('have.text', '3')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answer-3_displayText"]').should('not.exist')
        cy.validateDisplayAnswer(1, 0, false, true)
        cy.validateDisplayAnswer(1, 1, true, true)
        cy.validateDisplayAnswer(1, 2, false, true)
        cy.get('[data-cy="pageHeaderStat_Questions"] [data-cy="statValue"]').should('have.text', '2')
    });

    it('focus on new question button after cancel', function () {
        cy.createQuizDef(1);
        cy.visit('/administrator/quizzes/quiz1');
        cy.get('[data-cy="noQuestionsYet"]')

        cy.get('[data-cy="btn_Questions"]').click()
        cy.get('[data-cy="closeDialogBtn"]').click()
        cy.get('[data-cy="noQuestionsYet"]')
        cy.get('[data-cy="btn_Questions"]').should('have.focus')

        // initiate via bottom btn
        cy.get('[data-cy="newQuestionOnBottomBtn"]').click()
        cy.get('[data-cy="closeDialogBtn"]').click()
        cy.get('[data-cy="noQuestionsYet"]')
        cy.get('[data-cy="newQuestionOnBottomBtn"]').should('have.focus')

        // using modal X on the top right
        cy.get('[data-cy="newQuestionOnBottomBtn"]').click()
        cy.get('.p-dialog-header [aria-label="Close"]').click()
        cy.get('[data-cy="noQuestionsYet"]')
        cy.get('[data-cy="newQuestionOnBottomBtn"]').should('have.focus')
    });

    it('modal validation: question text', function () {
        cy.createQuizDef(1);
        cy.visit('/administrator/quizzes/quiz1');
        cy.get('[data-cy="btn_Questions"]').click()
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')

        cy.get('[data-cy="questionText"]').type('What is jabberwocky?')
        cy.get('[data-cy="descriptionError"]').contains('Question - paragraphs may not contain jabberwocky')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        cy.get('[data-cy="questionText"] div.toastui-editor-contents[contenteditable="true"]').clear()
        cy.get('[data-cy="descriptionError"]').contains('Question is a required field')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
    });

    it('modal validation: at least 2 answers are required', function () {
        cy.createQuizDef(1);
        cy.visit('/administrator/quizzes/quiz1');
        cy.get('[data-cy="btn_Questions"]').click()
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')

        cy.get('[data-cy="questionText"]').type('a')

        cy.get('[data-cy="answer-0"] [data-cy="selectCorrectAnswer"]').click()
        cy.get('[data-cy="answer-0"] [data-cy="answerText"]').type('1')
        cy.get('[data-cy="answer-1"] [data-cy="selectCorrectAnswer"]').click()

        cy.get('[data-cy="answersError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="answersError"]').contains('Must have at least 2 answers')
        cy.get('[data-cy="answer-1"] [data-cy="answerText"]').type('2')
        cy.get('[data-cy="answersError"]').should('not.be.visible')
    });

    it('modal validation: multiple choice must have 2 correct answers', function () {
        cy.createQuizDef(1);
        cy.visit('/administrator/quizzes/quiz1');
        cy.get('[data-cy="btn_Questions"]').click()
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')

        cy.get('[data-cy="questionText"]').type('a')

        cy.get('[data-cy="answer-0"] [data-cy="selectCorrectAnswer"]').click()
        cy.get('[data-cy="answer-0"] [data-cy="answerText"]').type('1')
        cy.get('[data-cy="answer-1"] [data-cy="answerText"]').type('2')

        cy.get('[data-cy="answersError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="answersError"]').contains('Multiple Choice Question must have at least 2 correct answers')
        cy.get('[data-cy="answer-1"] [data-cy="answerText"]').type('2')
        cy.get('[data-cy="answersError"]').should('not.be.visible')
    });

    it('modal validation: single choice must have at least 1 correct answer is selected', function () {
        cy.createQuizDef(1);
        cy.visit('/administrator/quizzes/quiz1');
        cy.get('[data-cy="btn_Questions"]').click()
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')

        cy.get('[data-cy="questionText"]').type('a')
        cy.get('[data-cy="answerTypeSelector"]').click()
        cy.get('[data-cy="selectionItem_SingleChoice"]').click()
        cy.get('[data-cy="answer-0"] [data-cy="answerText"]').type('1')
        cy.get('[data-cy="answer-1"] [data-cy="answerText"]').type('2')

        cy.get('[data-cy="answersError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="answersError"]').contains('Must have at least 1 correct answer selected')
        cy.get('[data-cy="answer-0"] [data-cy="selectCorrectAnswer"]').click()
        cy.get('[data-cy="answersError"]').should('not.be.visible')
    });

    it('modal validation: correct answer must have text filled in', function () {
        cy.createQuizDef(1);
        cy.visit('/administrator/quizzes/quiz1');
        cy.get('[data-cy="btn_Questions"]').click()
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')

        cy.get('[data-cy="questionText"]').type('a')
        cy.get('[data-cy="answerTypeSelector"]').click()
        cy.get('[data-cy="selectionItem_SingleChoice"]').click()

        cy.get('[data-cy="answer-1"] [data-cy="addNewAnswer"]').click()
        cy.get('[data-cy="answer-0"] [data-cy="answerText"]').type('1')
        cy.get('[data-cy="answer-0"] [data-cy="selectCorrectAnswer"]').click()
        cy.get('[data-cy="answer-1"] [data-cy="selectCorrectAnswer"]').click()
        cy.get('[data-cy="answer-2"] [data-cy="answerText"]').type('3')

        cy.get('[data-cy="answersError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="answersError"]').contains('Answers labeled as correct must have text')
        cy.get('[data-cy="answer-1"] [data-cy="selectCorrectAnswer"]').click()
        cy.get('[data-cy="answersError"]').should('not.be.visible')
    });

    it('modal validation: add remove questions button enable/disable state', function () {
        cy.createQuizDef(1);
        cy.visit('/administrator/quizzes/quiz1');
        cy.get('[data-cy="btn_Questions"]').click()
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')

        cy.get('[data-cy="answer-0"] [data-cy="removeAnswer"]').should('be.disabled')
        cy.get('[data-cy="answer-1"] [data-cy="removeAnswer"]').should('be.disabled')
        cy.get('[data-cy="answer-0"] [data-cy="addNewAnswer"]').should('be.enabled')
        cy.get('[data-cy="answer-1"] [data-cy="addNewAnswer"]').should('be.enabled')
        cy.get('[data-cy="answer-2"]').should('not.exist')

        cy.get('[data-cy="answer-0"] [data-cy="addNewAnswer"]').click()

        cy.get('[data-cy="answer-0"] [data-cy="removeAnswer"]').should('be.enabled')
        cy.get('[data-cy="answer-1"] [data-cy="removeAnswer"]').should('be.enabled')
        cy.get('[data-cy="answer-2"] [data-cy="removeAnswer"]').should('be.enabled')
        cy.get('[data-cy="answer-0"] [data-cy="addNewAnswer"]').should('be.enabled')
        cy.get('[data-cy="answer-1"] [data-cy="addNewAnswer"]').should('be.enabled')
        cy.get('[data-cy="answer-2"] [data-cy="addNewAnswer"]').should('be.enabled')
        cy.get('[data-cy="answer-3"]').should('not.exist')

        cy.get('[data-cy="answer-0"] [data-cy="removeAnswer"]').click()
        cy.get('[data-cy="answer-0"] [data-cy="removeAnswer"]').should('be.disabled')
        cy.get('[data-cy="answer-1"] [data-cy="removeAnswer"]').should('be.disabled')
        cy.get('[data-cy="answer-0"] [data-cy="addNewAnswer"]').should('be.enabled')
        cy.get('[data-cy="answer-1"] [data-cy="addNewAnswer"]').should('be.enabled')
        cy.get('[data-cy="answer-2"]').should('not.exist')
    });

    it('modal validation: once maximum of 10 is reached disable add buttons', function () {
        cy.createQuizDef(1);
        cy.visit('/administrator/quizzes/quiz1');
        cy.get('[data-cy="btn_Questions"]').click()
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')

        for (let i = 0; i < 7; i++) {
            cy.get(`[data-cy="answer-${i}"] [data-cy="addNewAnswer"]`).click()
        }
        for (let i = 0; i < 9; i++) {
            cy.get(`[data-cy="answer-${i}"] [data-cy="addNewAnswer"]`).should('be.enabled')
            cy.get(`[data-cy="answer-${i}"] [data-cy="removeAnswer"]`).should('be.enabled')
        }
        cy.get(`[data-cy="answer-0"] [data-cy="addNewAnswer"]`).click()
        for (let i = 0; i < 10; i++) {
            cy.get(`[data-cy="answer-${i}"] [data-cy="addNewAnswer"]`).should('be.disabled')
            cy.get(`[data-cy="answer-${i}"] [data-cy="removeAnswer"]`).should('be.enabled')
        }
    });

    it('empty non-correct answers are ignored', function () {
        cy.createQuizDef(1);
        cy.visit('/administrator/quizzes/quiz1');
        cy.get('[data-cy="btn_Questions"]').click()
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')

        cy.get('[data-cy="questionText"]').type('What is 2 + 2?')
        cy.get('[data-cy="answerTypeSelector"]').click()
        cy.get('[data-cy="selectionItem_SingleChoice"]').click()

        cy.get(`[data-cy="answer-0"] [data-cy="addNewAnswer"]`).click()
        cy.get(`[data-cy="answer-2"] [data-cy="addNewAnswer"]`).click()
        cy.get(`[data-cy="answer-1"] [data-cy="addNewAnswer"]`).click()

        cy.get('[data-cy="answer-0"] [data-cy="answerText"]').type('1')
        cy.get('[data-cy="answer-2"] [data-cy="answerText"]').type('4')
        cy.get('[data-cy="answer-4"] [data-cy="answerText"]').type('5')

        cy.get('[data-cy="answer-2"] [data-cy="selectCorrectAnswer"]').click()

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="questionDisplayText"]').contains('What is 2 + 2?')
        cy.validateDisplayAnswer(1, 0, false, true)
        cy.validateDisplayAnswer(1, 1, true, true)
        cy.validateDisplayAnswer(1, 2, false, true)
    });

    it('question and answers are changed in various ways before saved', function () {
        cy.createQuizDef(1);
        cy.visit('/administrator/quizzes/quiz1');
        cy.get('[data-cy="btn_Questions"]').click()
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')

        cy.get('[data-cy="questionText"]').type('What is 2 + 2?')

        cy.get('[data-cy="answer-0"] [data-cy="answerText"]').type('1')
        cy.get('[data-cy="answer-1"] [data-cy="answerText"]').type('2')

        cy.get(`[data-cy="answer-0"] [data-cy="addNewAnswer"]`).click()
        cy.get('[data-cy="answer-1"] [data-cy="answerText"]').type('1a')


        cy.get('[data-cy="answer-0"] [data-cy="selectCorrectAnswer"]').click()
        cy.get('[data-cy="answer-1"] [data-cy="selectCorrectAnswer"]').click()
        cy.get('[data-cy="answer-2"] [data-cy="selectCorrectAnswer"]').click()

        cy.get(`[data-cy="answer-1"] [data-cy="addNewAnswer"]`).click()
        cy.get('[data-cy="answer-2"] [data-cy="answerText"]').type('1b')

        cy.get(`[data-cy="answer-3"] [data-cy="addNewAnswer"]`).click()
        cy.get('[data-cy="answer-4"] [data-cy="answerText"]').type('2a')
        cy.get(`[data-cy="answer-4"] [data-cy="addNewAnswer"]`).click()
        cy.get('[data-cy="answer-5"] [data-cy="answerText"]').type('2b')

        cy.get('[data-cy="answer-0"] [data-cy="selectCorrectAnswer"]').click()
        cy.get('[data-cy="answer-1"] [data-cy="selectCorrectAnswer"]').click()
        cy.get('[data-cy="answer-2"] [data-cy="selectCorrectAnswer"]').click()
        cy.get('[data-cy="answer-5"] [data-cy="selectCorrectAnswer"]').click()

        cy.get('[data-cy="answer-0"] [data-cy="answerText"]').type('-1')
        cy.get('[data-cy="answer-1"] [data-cy="answerText"]').type('-2')
        cy.get('[data-cy="answer-2"] [data-cy="answerText"]').type('-3')
        cy.get('[data-cy="answer-3"] [data-cy="answerText"]').type('-4')
        cy.get('[data-cy="answer-4"] [data-cy="answerText"]').type('-5')
        cy.get('[data-cy="answer-5"] [data-cy="answerText"]').type('-6')

        cy.get('[data-cy="answer-4"] [data-cy="removeAnswer"]').click()
        cy.get('[data-cy="answer-4"] [data-cy="removeAnswer"]').click()
        cy.get('[data-cy="answer-0"] [data-cy="removeAnswer"]').click()

        cy.get('[data-cy="questionText"]  div.toastui-editor-contents[contenteditable="true"]').clear().type('All diff?')

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="questionDisplayText"]').contains('All diff?')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answer-0_displayText"]').should('have.text', '1a-2')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answer-1_displayText"]').should('have.text', '1b-3')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answer-2_displayText"]').should('have.text', '2-4')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answer-3_displayText"]').should('not.exist')
        cy.validateDisplayAnswer(1, 0, false, false)
        cy.validateDisplayAnswer(1, 1, true, false)
        cy.validateDisplayAnswer(1, 2, true, false)
    });

    it('edit a question', function () {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1)
        cy.createQuizQuestionDef(1, 2)
        cy.createQuizMultipleChoiceQuestionDef(1, 3)
        cy.visit('/administrator/quizzes/quiz1');

        cy.get('[data-cy="editQuestionButton_2"]').click();
        cy.get('[data-cy="editQuestionModal"] [data-cy="markdownEditorInput"]').contains('This is a question # 2')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-0"] [data-cy="answerText"]').should('have.value', 'Question 2 - First Answer')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-0"] [data-cy="selected"]').should('not.exist')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-0"] [data-cy="notSelected"]')

        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-1"] [data-cy="answerText"]').should('have.value', 'Question 2 - Second Answer')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-1"] [data-cy="selected"]')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-1"] [data-cy="notSelected"]').should('not.exist')

        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-2"] [data-cy="answerText"]').should('have.value', 'Question 2 - Third Answer')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-2"] [data-cy="selected"]').should('not.exist')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-2"] [data-cy="notSelected"]')

        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-0"] [data-cy="answerText"]').type('-more')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-1"] [data-cy="answerText"]').clear().type('b')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-2"] [data-cy="answerText"]').clear().type('c')
        cy.get('[data-cy="editQuestionModal"] [data-cy="markdownEditorInput"]').type('-more')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-0"] [data-cy="selectCorrectAnswer"]').click()
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-1"] [data-cy="selectCorrectAnswer"]').click()

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="questionDisplayText"]').contains('This is a question # 2-more')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answer-0_displayText"]').should('have.text', 'Question 2 - First Answer-more')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answer-1_displayText"]').should('have.text', 'b')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answer-2_displayText"]').should('have.text', 'c')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answer-3_displayText"]').should('not.exist')
        cy.validateDisplayAnswer(2, 0, true, true)
        cy.validateDisplayAnswer(2, 1, false, true)
        cy.validateDisplayAnswer(2, 2, false, true)

        cy.get('[data-cy="editQuestionButton_2"]').should('have.focus')
        cy.get('[data-cy="questionDisplayCard-1"]')
        cy.get('[data-cy="questionDisplayCard-3"]')
        cy.get('[data-cy="questionDisplayCard-4"]').should('not.exist')

        cy.get('[data-cy="editQuestionButton_2"]').click();
        cy.get('[data-cy="editQuestionModal"] [data-cy="markdownEditorInput"]').contains('This is a question # 2-more')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-0"] [data-cy="answerText"]').should('have.value', 'Question 2 - First Answer-more')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-0"] [data-cy="selected"]')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-0"] [data-cy="notSelected"]').should('not.exist')

        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-1"] [data-cy="answerText"]').should('have.value', 'b')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-1"] [data-cy="selected"]').should('not.exist')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-1"] [data-cy="notSelected"]')

        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-2"] [data-cy="answerText"]').should('have.value', 'c')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-2"] [data-cy="selected"]').should('not.exist')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-2"] [data-cy="notSelected"]')
    });

    it('edit a question - will change question type', function () {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1)
        cy.createQuizQuestionDef(1, 2)
        cy.createQuizMultipleChoiceQuestionDef(1, 3)
        cy.visit('/administrator/quizzes/quiz1');

        cy.get('[data-cy="editQuestionButton_2"]').click();
        // previous type
        cy.get('[data-cy="answerTypeSelector"] [data-cy="selectionItem_SingleChoice"]')
        cy.get('[data-cy="answerTypeSelector"]').click()
        cy.get('[data-cy="selectionItem_MultipleChoice"]').click()
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-0"] [data-cy="selectCorrectAnswer"]').click()

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.validateDisplayAnswer(2, 0, true, false)
        cy.validateDisplayAnswer(2, 1, true, false)
        cy.validateDisplayAnswer(2, 2, false, false)
    });

    it('edit a question - add an answer', function () {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1)
        cy.createQuizQuestionDef(1, 2)
        cy.createQuizMultipleChoiceQuestionDef(1, 3)
        cy.visit('/administrator/quizzes/quiz1');

        cy.get('[data-cy="editQuestionButton_3"]').click();
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-1"] [data-cy="addNewAnswer"]').click()
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-2"] [data-cy="answerText"]').type( 'new')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-2"] [data-cy="selectCorrectAnswer"]').click()

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.validateDisplayAnswer(3, 0, true, false)
        cy.validateDisplayAnswer(3, 1, false, false)
        cy.validateDisplayAnswer(3, 2, true, false)
        cy.validateDisplayAnswer(3, 3, true, false)
        cy.validateDisplayAnswer(3, 4, false, false)

        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="answer-0_displayText"]').should('have.text', 'First Answer')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="answer-1_displayText"]').should('have.text', 'Second Answer')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="answer-2_displayText"]').should('have.text', 'new')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="answer-3_displayText"]').should('have.text', 'Third Answer')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="answer-4_displayText"]').should('have.text', 'Fourth Answer')
    });

    it('edit a question - remove answer', function () {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1)
        cy.createQuizQuestionDef(1, 2)
        cy.createQuizMultipleChoiceQuestionDef(1, 3)
        cy.visit('/administrator/quizzes/quiz1');

        cy.get('[data-cy="editQuestionButton_3"]').click();
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-0"] [data-cy="removeAnswer"]').click()
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-0"] [data-cy="removeAnswer"]').click()
        cy.get('[data-cy="answerTypeSelector"]').click()
        cy.get('[data-cy="selectionItem_SingleChoice"]').click()

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="answer-0_displayText"]').should('have.text', 'Third Answer')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="answer-1_displayText"]').should('have.text', 'Fourth Answer')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="answer-2_displayText"]').should('not.exist')
    });

    it('return focus to the edit button if edit is closed or cancelled', function () {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1)
        cy.createQuizQuestionDef(1, 2)
        cy.createQuizMultipleChoiceQuestionDef(1, 3)
        cy.visit('/administrator/quizzes/quiz1');
        cy.get('[data-cy="pageHeaderStat_Type"] [data-cy="statPreformatted"]').should('have.text', 'Quiz')
        cy.get('[data-cy="editQuestionButton_1"]').should('be.visible')
        cy.get('[data-cy="editQuestionButton_2"]').should('be.visible')
        cy.get('[data-cy="editQuestionButton_3"]').should('be.visible')

        // initiate via bottom btn
        cy.get('[data-cy="editQuestionButton_2"]').click();
        cy.get('[data-cy="questionText"] [data-cy="markdownEditorInput"]').contains('This is a question # 2')
        cy.get('[data-cy="answer-1"] [data-cy="selectCorrectAnswer"]').should('exist')
        cy.get('[data-cy="closeDialogBtn"]').click()
        cy.get('[data-cy="closeDialogBtn"]').should('not.exist')
        cy.get('[data-cy="editQuestionButton_2"]').should('have.focus')

        // using modal X on the top right
        cy.get('[data-cy="editQuestionButton_1"]').click();
        cy.get('[data-cy="questionText"] [data-cy="markdownEditorInput"]').contains('This is a question # 1')
        cy.get('[data-cy="answer-0"] [data-cy="selectCorrectAnswer"]').should('exist')
        cy.get('.p-dialog-header [aria-label="Close"]').click()
        cy.get('.p-dialog-header [aria-label="Close"]').should('not.exist')
        cy.get('[data-cy="editQuestionButton_1"]').should('have.focus')
    });

    it('delete questions', function () {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1)
        cy.createQuizMultipleChoiceQuestionDef(1, 2)
        cy.visit('/administrator/quizzes/quiz1');

        cy.get('[data-cy="deleteQuestionButton_2"]').click()
        cy.get('[data-cy="currentValidationText"]').fill('Delete Me')
        cy.get('[data-cy="saveDialogBtn"]').click()

        cy.get('[data-cy="deleteQuestionButton_2"]').should('not.exist')
        cy.get('[data-cy="pageHeaderStat_Questions"] [data-cy="statValue"]').should('have.text', '1')
        cy.get('[data-cy="btn_Questions"]').should('have.focus')

        cy.get('[data-cy="noQuestionsYet"]').should('not.exist')
        cy.get('[data-cy="deleteQuestionButton_1"]').click()
        cy.get('[data-cy="currentValidationText"]').fill('Delete Me')
        cy.get('[data-cy="saveDialogBtn"]').click()

        cy.get('[data-cy="deleteQuestionButton_2"]').should('not.exist')
        cy.get('[data-cy="deleteQuestionButton_1"]').should('not.exist')
        cy.get('[data-cy="pageHeaderStat_Questions"] [data-cy="statValue"]').should('have.text', '0')
        cy.get('[data-cy="btn_Questions"]').should('have.focus')
        cy.get('[data-cy="noQuestionsYet"]')
    });

    it('drag and drop to sort questions', function () {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1)
        cy.createQuizQuestionDef(1, 2)
        cy.createQuizQuestionDef(1, 3)
        cy.visit('/administrator/quizzes/quiz1');

        const q1Card = '[data-cy="questionDisplayCard-1"] [data-cy="sortControlHandle"]';
        const q2Card = '[data-cy="questionDisplayCard-2"] [data-cy="sortControlHandle"]';
        const q3Card = '[data-cy="questionDisplayCard-3"] [data-cy="sortControlHandle"]';

        cy.validateElementsOrder('[data-cy="questionDisplayCard"]', ['question # 1', 'question # 2', 'question # 3']);
        cy.get(q1Card).dragAndDrop(q2Card);
        cy.validateElementsOrder('[data-cy="questionDisplayCard"]', ['question # 2', 'question # 1', 'question # 3']);
        cy.get(q3Card).dragAndDrop(q2Card);
        cy.validateElementsOrder('[data-cy="questionDisplayCard"]', ['question # 3', 'question # 2', 'question # 1']);

        cy.visit('/administrator/quizzes/quiz1');
        cy.validateElementsOrder('[data-cy="questionDisplayCard"]', ['question # 3', 'question # 2', 'question # 1']);
    });

    it('creating 2nd question enables drag-and-drop', function () {
        cy.createQuizDef(1);
        cy.visit('/administrator/quizzes/quiz1');

        cy.get('[data-cy="newQuestionOnBottomBtn"]').click()
        cy.get('[data-cy="answerTypeSelector"]').click()
        cy.get('[data-cy="selectionItem_SingleChoice"]').click()
        cy.get('[data-cy="questionText"]').type('question # 1')
        cy.get('[data-cy="answer-0"] [data-cy="answerText"]').type('3')
        cy.get('[data-cy="answer-1"] [data-cy="answerText"]').type('4')
        cy.get('[data-cy="answer-1"] [data-cy="selectCorrectAnswer"]').click()
        cy.get('[data-cy="saveDialogBtn"]').click()

        cy.get('[data-cy="newQuestionOnBottomBtn"]').click()
        cy.get('[data-cy="questionText"]').type('question # 2')
        cy.get('[data-cy="answer-0"] [data-cy="answerText"]').type('a')
        cy.get('[data-cy="answer-1"] [data-cy="answerText"]').type('b')
        cy.get('[data-cy="answer-0"] [data-cy="selectCorrectAnswer"]').click()
        cy.get('[data-cy="answer-1"] [data-cy="selectCorrectAnswer"]').click()
        cy.get('[data-cy="saveDialogBtn"]').click()

        const q1Card = '[data-cy="questionDisplayCard-1"] [data-cy="sortControlHandle"]';
        const q2Card = '[data-cy="questionDisplayCard-2"] [data-cy="sortControlHandle"]';

        cy.validateElementsOrder('[data-cy="questionDisplayCard"]', ['question # 1', 'question # 2']);
        cy.get(q1Card).dragAndDrop(q2Card);
        cy.validateElementsOrder('[data-cy="questionDisplayCard"]', ['question # 2', 'question # 1']);
    });

    it('user keyboard to sort questions', function () {
        cy.intercept('PATCH', '/admin/quiz-definitions/quiz1/questions/*').as('patchQuestion');
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1)
        cy.createQuizQuestionDef(1, 2)
        cy.createQuizQuestionDef(1, 3)
        cy.visit('/administrator/quizzes/quiz1');

        cy.validateElementsOrder('[data-cy="questionDisplayCard"]', ['question # 1', 'question # 2', 'question # 3']);
        cy.log("Operation: 1")
        cy.get('[data-cy="btn_Questions"]')
            .tab()
            .type('{downArrow}');
        cy.wait('@patchQuestion')
        cy.get('.spinner-border').should('not.exist')
        cy.validateElementsOrder('[data-cy="questionDisplayCard"]', ['question # 2', 'question # 1', 'question # 3']);
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="sortControlHandle"]').should('have.focus');
        cy.log("Operation: 2")
        cy.get('[data-cy="deleteQuestionButton_1"]')
            .tab()
            .type('{downArrow}');
        cy.wait('@patchQuestion')
        cy.get('.spinner-border').should('not.exist')
        cy.validateElementsOrder('[data-cy="questionDisplayCard"]', ['question # 2', 'question # 3', 'question # 1']);

        // attempt to move the lowest item - should not change anything
        cy.log("Operation: 3")
        cy.get('[data-cy="deleteQuestionButton_2"]')
            .tab()
            .type('{downArrow}');
        cy.validateElementsOrder('[data-cy="questionDisplayCard"]', ['question # 2', 'question # 3', 'question # 1']);

        cy.log("Operation: 4")
        cy.get('[data-cy="deleteQuestionButton_1"]')
            .tab()
            .type('{upArrow}');
        cy.wait('@patchQuestion')
        cy.get('.spinner-border').should('not.exist')
        cy.validateElementsOrder('[data-cy="questionDisplayCard"]', ['question # 3', 'question # 2', 'question # 1']);

        // attempt to move the top item - should not change anything
        cy.log("Operation: 5")
        cy.get('[data-cy="btn_Questions"]')
            .tab()
            .type('{upArrow}');
        cy.validateElementsOrder('[data-cy="questionDisplayCard"]', ['question # 3', 'question # 2', 'question # 1']);

        cy.log("Reload")
        cy.visit('/administrator/quizzes/quiz1');
        cy.validateElementsOrder('[data-cy="questionDisplayCard"]', ['question # 3', 'question # 2', 'question # 1']);
    });

// copy
    it('copy a question with answer edits', function () {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1)
        cy.createQuizQuestionDef(1, 2)
        cy.createQuizMultipleChoiceQuestionDef(1, 3)
        cy.visit('/administrator/quizzes/quiz1');

        cy.get('[data-cy="copyQuestionButton_2"]').click();
        cy.get('[data-cy="editQuestionModal"] [data-cy="markdownEditorInput"]').contains('This is a question # 2')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-0"] [data-cy="answerText"]').should('have.value', 'Question 2 - First Answer')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-0"] [data-cy="selected"]').should('not.exist')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-0"] [data-cy="notSelected"]')

        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-1"] [data-cy="answerText"]').should('have.value', 'Question 2 - Second Answer')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-1"] [data-cy="selected"]')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-1"] [data-cy="notSelected"]').should('not.exist')

        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-2"] [data-cy="answerText"]').should('have.value', 'Question 2 - Third Answer')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-2"] [data-cy="selected"]').should('not.exist')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-2"] [data-cy="notSelected"]')

        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-0"] [data-cy="answerText"]').type('-more')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-1"] [data-cy="answerText"]').clear().type('b')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-2"] [data-cy="answerText"]').clear().type('c')
        cy.get('[data-cy="editQuestionModal"] [data-cy="markdownEditorInput"]').type('-more')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-0"] [data-cy="selectCorrectAnswer"]').click()
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-1"] [data-cy="selectCorrectAnswer"]').click()

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="questionDisplayCard-4"]').should('exist')
        cy.get('[data-cy="questionDisplayCard-4"] [data-cy="questionDisplayText"]').contains('This is a question # 2-more')
        cy.get('[data-cy="questionDisplayCard-4"] [data-cy="answer-0_displayText"]').should('have.text', 'Question 2 - First Answer-more')
        cy.get('[data-cy="questionDisplayCard-4"] [data-cy="answer-1_displayText"]').should('have.text', 'b')
        cy.get('[data-cy="questionDisplayCard-4"] [data-cy="answer-2_displayText"]').should('have.text', 'c')
        cy.get('[data-cy="questionDisplayCard-4"] [data-cy="answer-3_displayText"]').should('not.exist')
        cy.validateDisplayAnswer(4, 0, true, true)
        cy.validateDisplayAnswer(4, 1, false, true)
        cy.validateDisplayAnswer(4, 2, false, true)

        cy.get('[data-cy="copyQuestionButton_2"]').should('have.focus')
        cy.get('[data-cy="questionDisplayCard-1"]')
        cy.get('[data-cy="questionDisplayCard-3"]')
        cy.get('[data-cy="questionDisplayCard-5"]').should('not.exist')

        cy.get('[data-cy="copyQuestionButton_4"]').click();
        cy.get('[data-cy="editQuestionModal"] [data-cy="markdownEditorInput"]').contains('This is a question # 2-more')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-0"] [data-cy="answerText"]').should('have.value', 'Question 2 - First Answer-more')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-0"] [data-cy="selected"]')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-0"] [data-cy="notSelected"]').should('not.exist')

        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-1"] [data-cy="answerText"]').should('have.value', 'b')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-1"] [data-cy="selected"]').should('not.exist')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-1"] [data-cy="notSelected"]')

        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-2"] [data-cy="answerText"]').should('have.value', 'c')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-2"] [data-cy="selected"]').should('not.exist')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-2"] [data-cy="notSelected"]')
    });

    it('copy a question - will change question type', function () {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1)
        cy.createQuizQuestionDef(1, 2)
        cy.createQuizMultipleChoiceQuestionDef(1, 3)
        cy.visit('/administrator/quizzes/quiz1');

        cy.get('[data-cy="copyQuestionButton_2"]').click();
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-0"] [data-cy="selectCorrectAnswer"]').click()

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.validateDisplayAnswer(4, 0, true, false)
        cy.validateDisplayAnswer(4, 1, true, false)
        cy.validateDisplayAnswer(4, 2, false, false)
    });

    it('copy a question - add an answer', function () {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1)
        cy.createQuizQuestionDef(1, 2)
        cy.createQuizMultipleChoiceQuestionDef(1, 3)
        cy.visit('/administrator/quizzes/quiz1');

        cy.get('[data-cy="copyQuestionButton_3"]').click();
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-1"] [data-cy="addNewAnswer"]').click()
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-2"] [data-cy="answerText"]').type( 'new')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-2"] [data-cy="selectCorrectAnswer"]').click()

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.validateDisplayAnswer(4, 0, true, false)
        cy.validateDisplayAnswer(4, 1, false, false)
        cy.validateDisplayAnswer(4, 2, true, false)
        cy.validateDisplayAnswer(4, 3, true, false)
        cy.validateDisplayAnswer(4, 4, false, false)

        cy.get('[data-cy="questionDisplayCard-4"] [data-cy="answer-0_displayText"]').should('have.text', 'First Answer')
        cy.get('[data-cy="questionDisplayCard-4"] [data-cy="answer-1_displayText"]').should('have.text', 'Second Answer')
        cy.get('[data-cy="questionDisplayCard-4"] [data-cy="answer-2_displayText"]').should('have.text', 'new')
        cy.get('[data-cy="questionDisplayCard-4"] [data-cy="answer-3_displayText"]').should('have.text', 'Third Answer')
        cy.get('[data-cy="questionDisplayCard-4"] [data-cy="answer-4_displayText"]').should('have.text', 'Fourth Answer')
    });

    it('copy a question - remove answer', function () {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1)
        cy.createQuizQuestionDef(1, 2)
        cy.createQuizMultipleChoiceQuestionDef(1, 3)
        cy.visit('/administrator/quizzes/quiz1');

        cy.get('[data-cy="copyQuestionButton_3"]').click();
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-0"] [data-cy="removeAnswer"]').click()
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-0"] [data-cy="removeAnswer"]').click()

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="questionDisplayCard-4"] [data-cy="answer-0_displayText"]').should('have.text', 'Third Answer')
        cy.get('[data-cy="questionDisplayCard-4"] [data-cy="answer-1_displayText"]').should('have.text', 'Fourth Answer')
        cy.get('[data-cy="questionDisplayCard-4"] [data-cy="answer-2_displayText"]').should('not.exist')
    });

    it('return focus to the copy button if copy is closed or cancelled', function () {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1)
        cy.createQuizQuestionDef(1, 2)
        cy.createQuizMultipleChoiceQuestionDef(1, 3)
        cy.visit('/administrator/quizzes/quiz1');
        cy.get('[data-cy="pageHeaderStat_Type"] [data-cy="statPreformatted"]').should('have.text', 'Quiz')
        cy.get('[data-cy="copyQuestionButton_1"]').should('be.visible')
        cy.get('[data-cy="copyQuestionButton_2"]').should('be.visible')
        cy.get('[data-cy="copyQuestionButton_3"]').should('be.visible')

        // initiate via bottom btn
        cy.get('[data-cy="copyQuestionButton_2"]').click();
        cy.get('[data-cy="questionText"] [data-cy="markdownEditorInput"]').contains('This is a question # 2')
        cy.get('[data-cy="answer-1"] [data-cy="selectCorrectAnswer"]').should('exist')
        cy.get('[data-cy="closeDialogBtn"]').click()
        cy.get('[data-cy="closeDialogBtn"]').should('not.exist')
        cy.get('[data-cy="copyQuestionButton_2"]').should('have.focus')

        // using modal X on the top right
        cy.get('[data-cy="copyQuestionButton_1"]').click();
        cy.get('[data-cy="questionText"] [data-cy="markdownEditorInput"]').contains('This is a question # 1')
        cy.get('[data-cy="answer-0"] [data-cy="selectCorrectAnswer"]').should('exist')
        cy.get('.p-dialog-header [aria-label="Close"]').click()
        cy.get('.p-dialog-header [aria-label="Close"]').should('not.exist')
        cy.get('[data-cy="copyQuestionButton_1"]').should('have.focus')
    });
});
