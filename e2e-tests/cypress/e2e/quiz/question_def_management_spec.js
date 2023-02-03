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

describe('Question CRUD Tests', () => {

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

    it('create single choice question', function () {
        cy.createQuizDef(1);
        cy.visit('/administrator/quizzes/quiz1');
        cy.get('[data-cy="noQuestionsYet"]')
        cy.get('[data-cy="btn_Questions"]').click()
        cy.get('[data-cy="pageHeaderStat_Questions"] [data-cy="statValue"]').should('have.text', '0')

        cy.get('[data-cy="questionText"]').type('What is 2 + 2?')
        cy.get('[data-cy="answer-0"] [data-cy="answerText"]').type('1')
        cy.get('[data-cy="answer-1"] [data-cy="answerText"]').type('4')
        cy.get('[data-cy="answer-1"] [data-cy="selectCorrectAnswer"]').click()

        cy.get('[data-cy="answer-1"] [data-cy="addNewAnswer"]').click()
        cy.get('[data-cy="answer-2"] [data-cy="answerText"]').type('3')

        cy.get('[data-cy="saveAnswerBtn"]').click()

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

        cy.get('[data-cy="saveAnswerBtn"]').click()

        // q2
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="questionDisplayText"]').contains('What is 1 + 2?')
        cy.validateDisplayAnswer(2, 0, true, false)
        cy.validateDisplayAnswer(2, 1, false, false)
        cy.validateDisplayAnswer(2, 2, true, false)
        cy.validateDisplayAnswer(2, 3, false, false)
        // q1
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="questionDisplayText"]').contains('What is 2 + 2?')
        cy.validateDisplayAnswer(1, 0, false, true)
        cy.validateDisplayAnswer(1, 1, true, true)
        cy.validateDisplayAnswer(1, 2, false, true)
        cy.get('[data-cy="pageHeaderStat_Questions"] [data-cy="statValue"]').should('have.text', '2')
        cy.get('[data-cy="newQuestionOnBottomBtn"]').should('have.focus')

        cy.visit('/administrator/quizzes/quiz1');
        // q2
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="questionDisplayText"]').contains('What is 1 + 2?')
        cy.validateDisplayAnswer(2, 0, true, false)
        cy.validateDisplayAnswer(2, 1, false, false)
        cy.validateDisplayAnswer(2, 2, true, false)
        cy.validateDisplayAnswer(2, 3, false, false)
        // q1
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="questionDisplayText"]').contains('What is 2 + 2?')
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
        cy.get('[data-cy="closeQuestionBtn"]').click()
        cy.get('[data-cy="noQuestionsYet"]')
        cy.get('[data-cy="btn_Questions"]').should('have.focus')

        // initiate via bottom btn
        cy.get('[data-cy="newQuestionOnBottomBtn"]').click()
        cy.get('[data-cy="closeQuestionBtn"]').click()
        cy.get('[data-cy="noQuestionsYet"]')
        cy.get('[data-cy="newQuestionOnBottomBtn"]').should('have.focus')

        // using modal X on the top right
        cy.get('[data-cy="newQuestionOnBottomBtn"]').click()
        cy.get('.modal-header [aria-label="Close"]').click()
        cy.get('[data-cy="noQuestionsYet"]')
        cy.get('[data-cy="newQuestionOnBottomBtn"]').should('have.focus')
    });

    it('modal validation: question text', function () {
        cy.createQuizDef(1);
        cy.visit('/administrator/quizzes/quiz1');
        cy.get('[data-cy="btn_Questions"]').click()
        cy.get('[data-cy="questionTextErr"]').should('not.be.visible')

        cy.get('[data-cy="questionText"]').type('What is jabberwocky?')
        cy.get('[data-cy="saveAnswerBtn"]').should('be.disabled')
        cy.get('[data-cy="questionTextErr"]').contains('Question - paragraphs may not contain jabberwocky')

        cy.get('[data-cy="questionText"]').clear()
        cy.get('[data-cy="questionTextErr"]').contains('Question is required')
        cy.get('[data-cy="saveAnswerBtn"]').should('be.disabled')
    });

});
