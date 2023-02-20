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

describe('Quiz Runs History Tests', () => {

    const tableSelector = '[data-cy="quizRunsHistoryTable"]'

    beforeEach(() => {
        Cypress.Commands.add('validateChoiceAnswer', (qNum, aNum, val, isSingleChoice, isSelected, wrongSelection = false, missedSelection = false,) => {
            cy.get(`[data-cy="questionDisplayCard-${qNum}"] [data-cy="answer-${aNum}_displayText"]`).should('have.text', val)
            let expectedIconCss
            if (isSingleChoice) {
                expectedIconCss = isSelected ? '.fa-check-circle' : '.fa-circle'
            } else {
                expectedIconCss = isSelected ? '.fa-check-square' : '.fa-square'
            }
            cy.get(`[data-cy="questionDisplayCard-${qNum}"] [data-cy="answerDisplay-${aNum}"] [data-cy="selectCorrectAnswer"] ${expectedIconCss}`)

            const selectedSelector = isSelected ? '[data-cy="selected"]' : ''
            cy.get(`[data-cy="questionDisplayCard-${qNum}"] [data-cy="answerDisplay-${aNum}"] [data-cy="selectCorrectAnswer"] ${selectedSelector}`)

            if (wrongSelection) {
                cy.get(`[data-cy="questionDisplayCard-${qNum}"] [data-cy="answerDisplay-${aNum}"] [data-cy="wrongSelection"]`)
            } else {
                cy.get(`[data-cy="questionDisplayCard-${qNum}"] [data-cy="answerDisplay-${aNum}"] [data-cy="wrongSelection"]`).should('not.exist')
            }
            if (missedSelection) {
                cy.get(`[data-cy="questionDisplayCard-${qNum}"] [data-cy="answerDisplay-${aNum}"] [data-cy="missedSelection"]`)
            } else {
                cy.get(`[data-cy="questionDisplayCard-${qNum}"] [data-cy="answerDisplay-${aNum}"] [data-cy="missedSelection"]`).should('not.exist')
            }
        });
    });

    it('survey status', function () {
        cy.createSurveyDef(1);
        cy.createSurveyMultipleChoiceQuestionDef(1, 1);
        cy.runQuizForUser(1, 1, [{selectedIndex: [1]}]);
        cy.runQuizForUser(1, 2, [{selectedIndex: [0]}], false);
        cy.visit('/administrator/quizzes/quiz1/results');

        cy.validateTable(tableSelector, [
            [{ colIndex: 0, value: 'user2' }, { colIndex: 1, value: 'In Progress' }],
            [{ colIndex: 0, value: 'user1' }, { colIndex: 1, value: 'Completed' }],
        ], 10);
    });


    it('format total runtime and start time', function () {
        cy.intercept('GET', '/admin/quiz-definitions/quiz1/results**', (req) => {
            req.reply({
                body: {
                    'data': [{
                        'status': 'PASSED',
                        'userId': 'user1',
                        'userIdForDisplay': 'user1',
                        'completed': '2023-02-15T22:52:53.990+00:00',
                        'started': '2023-02-15T21:45:50.917+00:00'
                    }],
                    'count': 1,
                    'totalCount': 1
                },
            });
        }).as('quizRuns')

        cy.createSurveyDef(1);
        cy.createSurveyMultipleChoiceQuestionDef(1, 1);
        cy.runQuizForUser(1, 1, [{selectedIndex: [1]}]);
        cy.visit('/administrator/quizzes/quiz1/results');
        cy.wait('@quizRuns')

        cy.validateTable(tableSelector, [
            [{ colIndex: 2, value: '1 hour and 7 minutes' }, { colIndex: 3, value: '2023-02-15 21:45' }],
        ], 10);
    });

    it('delete a run', function () {
        cy.createSurveyDef(1);
        cy.createSurveyMultipleChoiceQuestionDef(1, 1);
        cy.runQuizForUser(1, 1, [{selectedIndex: [1]}]);
        cy.runQuizForUser(1, 2, [{selectedIndex: [0]}]);
        cy.runQuizForUser(1, 3, [{selectedIndex: [1]}]);
        cy.visit('/administrator/quizzes/quiz1/results');
        cy.get('[data-cy="row0-userCell"]').contains('user3')
        cy.get('[data-cy="row1-userCell"]').contains('user2')
        cy.get('[data-cy="row2-userCell"]').contains('user1')

        cy.get('[data-cy="row1-deleteBtn"]').click()
        cy.get('[data-cy="removalSafetyCheckMsg"]').contains('This will remove the Survey result for user2 user')
        cy.get('[data-cy="currentValidationText"]').type('Delete Me')
        cy.get('[data-cy="removeButton"]').click()

        cy.get('[data-cy="row0-userCell"]').contains('user3')
        cy.get('[data-cy="row1-userCell"]').contains('user1')
        cy.get('[data-cy="row2-userCell"]').should('not.exist')
        cy.get('[data-cy="userResetBtn"]').should('have.focus')
    });

    it('canceling delete returns focus to the delete button', function () {
        cy.createSurveyDef(1);
        cy.createSurveyMultipleChoiceQuestionDef(1, 1);
        cy.runQuizForUser(1, 1, [{selectedIndex: [1]}]);
        cy.runQuizForUser(1, 2, [{selectedIndex: [0]}]);
        cy.runQuizForUser(1, 3, [{selectedIndex: [1]}]);
        cy.visit('/administrator/quizzes/quiz1/results');

        cy.get('[data-cy="row1-deleteBtn"]').click()
        cy.get('[data-cy="closeRemovalSafetyCheck"]').click()
        cy.get('[data-cy="row1-deleteBtn"]').should('have.focus')

        cy.get('[data-cy="row1-deleteBtn"]').click()
        cy.get('.modal-header [aria-label="Close"]').click()
        cy.get('[data-cy="row1-deleteBtn"]').should('have.focus')
    });

    it('view single passed survey run', function () {
        cy.createSurveyDef(1);
        cy.createSurveyMultipleChoiceQuestionDef(1, 1);
        cy.createSurveyMultipleChoiceQuestionDef(1, 2, { questionType: 'SingleChoice' });
        cy.createTextInputQuestionDef(1, 3);
        cy.runQuizForUser(1, 1, [{selectedIndex: [1]}, {selectedIndex: [0]}, {selectedIndex: [0]}])

        cy.visit('/administrator/quizzes/quiz1/results');

        cy.get('[data-cy="row0-viewRun"]').click()
        cy.get('[data-cy="quizRunStatus"]').contains('Completed')

        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="questionDisplayText"]').contains('This is a question # 1')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="noAnswer"]').should('not.exist')
        cy.validateChoiceAnswer(1, 0, 'Question 1 - First Answer', false, false)
        cy.validateChoiceAnswer(1, 1, 'Question 1 - Second Answer', false, true)
        cy.validateChoiceAnswer(1, 2, 'Question 1 - Third Answer', false, false)

        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="questionDisplayText"]').contains('This is a question # 2')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="noAnswer"]').should('not.exist')
        cy.validateChoiceAnswer(2, 0, 'Question 2 - First Answer', true, true)
        cy.validateChoiceAnswer(2, 1, 'Question 2 - Second Answer', true, false)
        cy.validateChoiceAnswer(2, 2, 'Question 2 - Third Answer', true, false)

        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="questionDisplayText"]').contains('This is a question # 3')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="noAnswer"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="TextInputAnswer"]').contains('This is answer for question # 2')
        cy.get('[data-cy="questionDisplayCard-4"] [data-cy="questionDisplayText"]').should('not.exist')

        cy.reload()
        cy.get('[data-cy="quizRunStatus"]').contains('Completed')

        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="questionDisplayText"]').contains('This is a question # 1')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="noAnswer"]').should('not.exist')
        cy.validateChoiceAnswer(1, 0, 'Question 1 - First Answer', false, false)
        cy.validateChoiceAnswer(1, 1, 'Question 1 - Second Answer', false, true)
        cy.validateChoiceAnswer(1, 2, 'Question 1 - Third Answer', false, false)

        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="questionDisplayText"]').contains('This is a question # 2')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="noAnswer"]').should('not.exist')
        cy.validateChoiceAnswer(2, 0, 'Question 2 - First Answer', true, true)
        cy.validateChoiceAnswer(2, 1, 'Question 2 - Second Answer', true, false)
        cy.validateChoiceAnswer(2, 2, 'Question 2 - Third Answer', true, false)

        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="questionDisplayText"]').contains('This is a question # 3')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="noAnswer"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="TextInputAnswer"]').contains('This is answer for question # 2')

        cy.get('[data-cy="questionDisplayCard-4"] [data-cy="questionDisplayText"]').should('not.exist')
    });

    it('view single survey run - 1 question answered', function () {
        cy.createSurveyDef(1);
        cy.createSurveyMultipleChoiceQuestionDef(1, 1);
        cy.createSurveyMultipleChoiceQuestionDef(1, 2, { questionType: 'SingleChoice' });
        cy.createTextInputQuestionDef(1, 3);
        cy.runQuizForUser(1, 1, [{selectedIndex: [1]}], false)

        cy.visit('/administrator/quizzes/quiz1/results');

        cy.get('[data-cy="row0-viewRun"]').click()
        cy.get('[data-cy="quizRunStatus"]').contains('In Progress')
        cy.get('[data-cy="quizRunStatus"]').contains('1 / 3')

        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="questionDisplayText"]').contains('This is a question # 1')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="noAnswer"]').should('not.exist')
        cy.validateChoiceAnswer(1, 0, 'Question 1 - First Answer', false, false)
        cy.validateChoiceAnswer(1, 1, 'Question 1 - Second Answer', false, true)
        cy.validateChoiceAnswer(1, 2, 'Question 1 - Third Answer', false, false)

        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="questionDisplayText"]').contains('This is a question # 2')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="noAnswer"]')
        cy.validateChoiceAnswer(2, 0, 'Question 2 - First Answer', true, false)
        cy.validateChoiceAnswer(2, 1, 'Question 2 - Second Answer', true, false)
        cy.validateChoiceAnswer(2, 2, 'Question 2 - Third Answer', true, false)

        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="questionDisplayText"]').contains('This is a question # 3')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="TextInputAnswer"]').should('not.have.text')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="noAnswer"]')

        cy.get('[data-cy="questionDisplayCard-4"] [data-cy="questionDisplayText"]').should('not.exist')
    });

    it('view single survey run - all question answered by not submitted', function () {
        cy.createSurveyDef(1);
        cy.createSurveyMultipleChoiceQuestionDef(1, 1);
        cy.createSurveyMultipleChoiceQuestionDef(1, 2, { questionType: 'SingleChoice' });
        cy.createTextInputQuestionDef(1, 3);
        cy.runQuizForUser(1, 1, [{selectedIndex: [1]}, {selectedIndex: [1]}, {selectedIndex: [0]}], false)

        cy.visit('/administrator/quizzes/quiz1/results');

        cy.get('[data-cy="row0-viewRun"]').click()
        cy.get('[data-cy="quizRunStatus"]').contains('In Progress')
        cy.get('[data-cy="quizRunStatus"]').contains('3 / 3')

        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="questionDisplayText"]').contains('This is a question # 1')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="noAnswer"]').should('not.exist')
        cy.validateChoiceAnswer(1, 0, 'Question 1 - First Answer', false, false)
        cy.validateChoiceAnswer(1, 1, 'Question 1 - Second Answer', false, true)
        cy.validateChoiceAnswer(1, 2, 'Question 1 - Third Answer', false, false)

        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="questionDisplayText"]').contains('This is a question # 2')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="noAnswer"]').should('not.exist')
        cy.validateChoiceAnswer(2, 0, 'Question 2 - First Answer', true, false)
        cy.validateChoiceAnswer(2, 1, 'Question 2 - Second Answer', true, true)
        cy.validateChoiceAnswer(2, 2, 'Question 2 - Third Answer', true, false)

        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="questionDisplayText"]').contains('This is a question # 3')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="TextInputAnswer"]').contains('This is answer for question # 2')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="noAnswer"]').should('not.exist')

        cy.get('[data-cy="questionDisplayCard-4"] [data-cy="questionDisplayText"]').should('not.exist')
    });

    it('view single passed quiz run', function () {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.createQuizMultipleChoiceQuestionDef(1, 2);
        cy.runQuizForUser(1, 1, [{selectedIndex: [0]}, {selectedIndex: [0, 2]}])

        cy.visit('/administrator/quizzes/quiz1/results');

        cy.get('[data-cy="row0-viewRun"]').click()
        cy.get('[data-cy="quizRunStatus"]').contains('Passed')

        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="questionDisplayText"]').contains('This is a question # 1')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="wrongAnswer"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="noAnswer"]').should('not.exist')
        cy.validateChoiceAnswer(1, 0, 'Question 1 - First Answer', true, true)
        cy.validateChoiceAnswer(1, 1, 'Question 1 - Second Answer', true, false)
        cy.validateChoiceAnswer(1, 2, 'Question 1 - Third Answer', true, false)

        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="questionDisplayText"]').contains('This is a question # 2')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="wrongAnswer"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="noAnswer"]').should('not.exist')
        cy.validateChoiceAnswer(2, 0, 'First Answer', false, true)
        cy.validateChoiceAnswer(2, 1, 'Second Answer', false, false)
        cy.validateChoiceAnswer(2, 2, 'Third Answer', false, true)
        cy.validateChoiceAnswer(2, 3, 'Fourth Answer', false, false)

        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="questionDisplayText"]').should('not.exist')

        cy.reload()
        cy.get('[data-cy="quizRunStatus"]').contains('Passed')

        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="questionDisplayText"]').contains('This is a question # 1')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="noAnswer"]').should('not.exist')
        cy.validateChoiceAnswer(1, 0, 'Question 1 - First Answer', true, true)
        cy.validateChoiceAnswer(1, 1, 'Question 1 - Second Answer', true, false)
        cy.validateChoiceAnswer(1, 2, 'Question 1 - Third Answer', true, false)

        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="questionDisplayText"]').contains('This is a question # 2')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="noAnswer"]').should('not.exist')
        cy.validateChoiceAnswer(2, 0, 'First Answer', false, true)
        cy.validateChoiceAnswer(2, 1, 'Second Answer', false, false)
        cy.validateChoiceAnswer(2, 2, 'Third Answer', false, true)
        cy.validateChoiceAnswer(2, 3, 'Fourth Answer', false, false)

        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="questionDisplayText"]').should('not.exist')
    });

    it('view single failed quiz run', function () {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.createQuizMultipleChoiceQuestionDef(1, 2);
        cy.runQuizForUser(1, 1, [{selectedIndex: [1]}, {selectedIndex: [0, 3]}])

        cy.visit('/administrator/quizzes/quiz1/results');

        cy.get('[data-cy="row0-viewRun"]').click()
        cy.get('[data-cy="quizRunStatus"]').contains('Failed')
        cy.get('[data-cy="quizRunStatus"]').contains('Missed by 2 questions')

        cy.get('[data-cy="numQuestionsToPass"]').contains('0 / 2')
        cy.get('[data-cy="numQuestionsToPass"]').contains('Need 2 questions to pass')

        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="questionDisplayText"]').contains('This is a question # 1')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="wrongAnswer"]').should('exist')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="noAnswer"]').should('not.exist')
        cy.validateChoiceAnswer(1, 0, 'Question 1 - First Answer', true, false, false, true)
        cy.validateChoiceAnswer(1, 1, 'Question 1 - Second Answer', true, true, true, false)
        cy.validateChoiceAnswer(1, 2, 'Question 1 - Third Answer', true, false, false, false)

        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="questionDisplayText"]').contains('This is a question # 2')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="wrongAnswer"]').should('exist')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="noAnswer"]').should('not.exist')
        cy.validateChoiceAnswer(2, 0, 'First Answer', false, true, false, false)
        cy.validateChoiceAnswer(2, 1, 'Second Answer', false, false, false, false)
        cy.validateChoiceAnswer(2, 2, 'Third Answer', false, false, false, true)
        cy.validateChoiceAnswer(2, 3, 'Fourth Answer', false, true, true, false)

        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="questionDisplayText"]').should('not.exist')
    });

    it('view single in progress quiz run - 1 right 2 wrong, 1 not answered', function () {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.createQuizMultipleChoiceQuestionDef(1, 2);
        cy.createQuizQuestionDef(1, 3);
        cy.createQuizQuestionDef(1, 4);
        cy.runQuizForUser(1, 1, [{selectedIndex: [1]}, {selectedIndex: [0, 3]}, {selectedIndex: [2]}], false)

        cy.visit('/administrator/quizzes/quiz1/results');

        cy.get('[data-cy="row0-viewRun"]').click()
        cy.get('[data-cy="quizRunStatus"]').contains('In Progress')
        cy.get('[data-cy="quizRunStatus"]').contains('3 / 4')

        cy.get('[data-cy="numQuestionsToPass"]').contains('1 / 4')
        cy.get('[data-cy="numQuestionsToPass"]').contains('Need 4 questions to pass')

        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="questionDisplayText"]').contains('This is a question # 1')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="wrongAnswer"]').should('exist')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="noAnswer"]').should('not.exist')
        cy.validateChoiceAnswer(1, 0, 'Question 1 - First Answer', true, false, false, true)
        cy.validateChoiceAnswer(1, 1, 'Question 1 - Second Answer', true, true, true, false)
        cy.validateChoiceAnswer(1, 2, 'Question 1 - Third Answer', true, false, false, false)

        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="questionDisplayText"]').contains('This is a question # 2')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="wrongAnswer"]').should('exist')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="noAnswer"]').should('not.exist')
        cy.validateChoiceAnswer(2, 0, 'First Answer', false, true, false, false)
        cy.validateChoiceAnswer(2, 1, 'Second Answer', false, false, false, false)
        cy.validateChoiceAnswer(2, 2, 'Third Answer', false, false, false, true)
        cy.validateChoiceAnswer(2, 3, 'Fourth Answer', false, true, true, false)

        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="questionDisplayText"]').contains('This is a question # 3')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="noAnswer"]').should('not.exist')
        cy.validateChoiceAnswer(3, 0, 'Question 3 - First Answer', true, false)
        cy.validateChoiceAnswer(3, 1, 'Question 3 - Second Answer', true, false)
        cy.validateChoiceAnswer(3, 2, 'Question 3 - Third Answer', true, true)

        cy.get('[data-cy="questionDisplayCard-4"] [data-cy="questionDisplayText"]').contains('This is a question # 4')
        cy.get('[data-cy="questionDisplayCard-4"] [data-cy="noAnswer"]').should('exist')
        cy.validateChoiceAnswer(4, 0, 'Question 4 - First Answer', true, false)
        cy.validateChoiceAnswer(4, 1, 'Question 4 - Second Answer', true, false)
        cy.validateChoiceAnswer(4, 2, 'Question 4 - Third Answer', true, false)

        cy.get('[data-cy="questionDisplayCard-5"] [data-cy="questionDisplayText"]').should('not.exist')
    });
});
