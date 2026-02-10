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
import moment from "moment-timezone";

dayjs.extend(utcPlugin);

describe('Quiz Runs History Tests', () => {

    const tableSelector = '[data-cy="quizRunsHistoryTable"]'

    Cypress.Commands.add('prevMonth', () => {
        cy.get('[data-pc-section="panel"] [data-pc-section="calendar"] [data-pc-name="pcprevbutton"]').click()
        cy.wait(150);
    });
    Cypress.Commands.add('setDay', (dayNum) => {
        let re = new RegExp(String.raw`^${dayNum}$`)
        cy.get('[data-pc-section="panel"] [data-pc-section="calendar"] [data-pc-section="day"]').contains(re).click()
    });

    Cypress.Commands.add('filterSetYear', (yearNum) => {
        cy.get('[data-pc-section="panel"] [data-pc-section="calendar"] [data-pc-section="selectyear"]').click()
        cy.get('[data-pc-section="panel"] [data-pc-section="year"]').contains(yearNum).click()
    })
    Cypress.Commands.add('filterSetMonth', (month) => {
        cy.get('[data-pc-section="panel"] [data-pc-section="month"]').contains(month).click()
    })

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
        cy.visit('/administrator/quizzes/quiz1/runs');

        cy.validateTable(tableSelector, [
            [{ colIndex: 0, value: 'user2' }, { colIndex: 2, value: 'In Progress' }],
            [{ colIndex: 0, value: 'user1' }, { colIndex: 2, value: 'Completed' }],
        ], 10);
    });

    it('format total runtime and start time', function () {
        const runs = [
            { 'started': '2023-02-15T22:52:53.990+00:00', 'completed': '2023-02-15T22:52:53.990+00:00', 'attemptId' : 1, 'status': 'PASSED', 'userId': 'user1', 'userIdForDisplay': 'user1' },
            { 'started': '2023-02-15T22:52:53.990+00:00', 'completed': '2023-02-15T22:52:53.995+00:00', 'attemptId' : 1, 'status': 'PASSED', 'userId': 'user1', 'userIdForDisplay': 'user2' },
            { 'started': '2023-02-15T22:52:53.990+00:00', 'completed': '2023-02-15T22:52:54.989+00:00', 'attemptId' : 1, 'status': 'PASSED', 'userId': 'user1', 'userIdForDisplay': 'user3' },
            { 'started': '2023-02-15T22:52:53.990+00:00', 'completed': '2023-02-15T22:52:54.990+00:00', 'attemptId' : 1, 'status': 'PASSED', 'userId': 'user1', 'userIdForDisplay': 'user4' },
            { 'started': '2023-02-15T22:52:01.990+00:00', 'completed': '2023-02-15T22:52:24.990+00:00', 'attemptId' : 1, 'status': 'PASSED', 'userId': 'user1', 'userIdForDisplay': 'user4' },
            { 'started': '2023-02-15T22:52:01.990+00:00', 'completed': '2023-02-15T22:53:00.990+00:00', 'attemptId' : 1, 'status': 'PASSED', 'userId': 'user1', 'userIdForDisplay': 'user4' },
            { 'started': '2023-02-15T22:52:01.990+00:00', 'completed': '2023-02-15T22:53:01.990+00:00', 'attemptId' : 1, 'status': 'PASSED', 'userId': 'user1', 'userIdForDisplay': 'user4' },
            { 'started': '2023-02-15T22:52:01.990+00:00', 'completed': '2023-02-15T22:53:02.990+00:00', 'attemptId' : 1, 'status': 'PASSED', 'userId': 'user1', 'userIdForDisplay': 'user4' },
            { 'started': '2023-02-15T22:52:01.990+00:00', 'completed': '2023-02-15T22:53:34.990+00:00', 'attemptId' : 1, 'status': 'PASSED', 'userId': 'user1', 'userIdForDisplay': 'user4' },
            { 'started': '2023-02-15T22:52:01.990+00:00', 'completed': '2023-02-15T22:54:00.990+00:00', 'attemptId' : 1, 'status': 'PASSED', 'userId': 'user1', 'userIdForDisplay': 'user4' },
            { 'started': '2023-02-15T22:52:01.990+00:00', 'completed': '2023-02-15T22:54:02.990+00:00', 'attemptId' : 1, 'status': 'PASSED', 'userId': 'user1', 'userIdForDisplay': 'user4' },
            { 'started': '2023-02-15T22:52:01.990+00:00', 'completed': '2023-02-15T22:55:00.990+00:00', 'attemptId' : 1, 'status': 'PASSED', 'userId': 'user1', 'userIdForDisplay': 'user4' },
            { 'started': '2023-02-15T22:52:01.990+00:00', 'completed': '2023-02-15T23:02:00.990+00:00', 'attemptId' : 1, 'status': 'PASSED', 'userId': 'user1', 'userIdForDisplay': 'user4' },
            { 'started': '2023-02-15T22:52:01.990+00:00', 'completed': '2023-02-15T23:02:01.990+00:00', 'attemptId' : 1, 'status': 'PASSED', 'userId': 'user1', 'userIdForDisplay': 'user4' },
            { 'started': '2023-02-15T22:52:01.990+00:00', 'completed': '2023-02-15T23:51:33.990+00:00', 'attemptId' : 1, 'status': 'PASSED', 'userId': 'user1', 'userIdForDisplay': 'user4' },
            { 'started': '2023-02-15T22:52:01.990+00:00', 'completed': '2023-02-15T23:52:01.990+00:00', 'attemptId' : 1, 'status': 'PASSED', 'userId': 'user1', 'userIdForDisplay': 'user4' },
            { 'started': '2023-02-15T22:52:01.990+00:00', 'completed': '2023-02-15T23:53:34.990+00:00', 'attemptId' : 1, 'status': 'PASSED', 'userId': 'user1', 'userIdForDisplay': 'user4' },
            { 'started': '2023-02-15T22:52:01.990+00:00', 'completed': '2023-02-15T23:54:34.990+00:00', 'attemptId' : 1, 'status': 'PASSED', 'userId': 'user1', 'userIdForDisplay': 'user4' },
            { 'started': '2023-02-15T22:01:01.990+00:00', 'completed': '2023-02-15T23:34:34.990+00:00', 'attemptId' : 1, 'status': 'PASSED', 'userId': 'user1', 'userIdForDisplay': 'user4' },
            { 'started': '2023-02-15T20:01:01.990+00:00', 'completed': '2023-02-15T22:00:34.990+00:00', 'attemptId' : 1, 'status': 'PASSED', 'userId': 'user1', 'userIdForDisplay': 'user4' },
            { 'started': '2023-02-15T20:01:01.990+00:00', 'completed': '2023-02-15T22:01:34.990+00:00', 'attemptId' : 1, 'status': 'PASSED', 'userId': 'user1', 'userIdForDisplay': 'user4' },
            { 'started': '2023-02-15T20:01:01.990+00:00', 'completed': '2023-02-15T22:02:34.990+00:00', 'attemptId' : 1, 'status': 'PASSED', 'userId': 'user1', 'userIdForDisplay': 'user4' },
            { 'started': '2023-02-15T01:01:01.990+00:00', 'completed': '2023-02-15T11:00:34.990+00:00', 'attemptId' : 1, 'status': 'PASSED', 'userId': 'user1', 'userIdForDisplay': 'user4' },
            { 'started': '2023-02-15T01:01:01.990+00:00', 'completed': '2023-02-15T11:01:34.990+00:00', 'attemptId' : 1, 'status': 'PASSED', 'userId': 'user1', 'userIdForDisplay': 'user4' },
            { 'started': '2023-02-15T01:01:01.990+00:00', 'completed': '2023-02-15T11:35:34.990+00:00', 'attemptId' : 1, 'status': 'PASSED', 'userId': 'user1', 'userIdForDisplay': 'user4' },
            { 'started': '2023-02-15T01:01:01.990+00:00', 'completed': '2023-02-16T01:01:01.989+00:00', 'attemptId' : 1, 'status': 'PASSED', 'userId': 'user1', 'userIdForDisplay': 'user4' },
            { 'started': '2023-02-15T01:01:01.990+00:00', 'completed': '2023-02-16T01:01:01.990+00:00', 'attemptId' : 1, 'status': 'PASSED', 'userId': 'user1', 'userIdForDisplay': 'user4' },
            { 'started': '2023-02-15T01:01:01.990+00:00', 'completed': '2023-02-16T13:01:01.990+00:00', 'attemptId' : 1, 'status': 'PASSED', 'userId': 'user1', 'userIdForDisplay': 'user4' },
            { 'started': '2023-02-15T01:01:01.990+00:00', 'completed': '2024-02-14T13:01:01.990+00:00', 'attemptId' : 1, 'status': 'PASSED', 'userId': 'user1', 'userIdForDisplay': 'user4' },
            { 'started': '2024-02-15T01:01:01.990+00:00', 'completed': '2025-02-15T01:01:01.990+00:00', 'attemptId' : 1, 'status': 'PASSED', 'userId': 'user1', 'userIdForDisplay': 'user4' },
            { 'started': '2024-02-15T01:01:01.990+00:00', 'completed': '2025-05-12T01:01:01.990+00:00', 'attemptId' : 1, 'status': 'PASSED', 'userId': 'user1', 'userIdForDisplay': 'user4' },
            { 'started': '2024-02-15T01:01:01.990+00:00', 'completed': '2026-01-15T01:00:01.990+00:00', 'attemptId' : 1, 'status': 'PASSED', 'userId': 'user1', 'userIdForDisplay': 'user4' },
            { 'started': '2024-02-15T01:01:01.990+00:00', 'completed': '2026-02-15T01:01:01.990+00:00', 'attemptId' : 1, 'status': 'PASSED', 'userId': 'user1', 'userIdForDisplay': 'user4' },
        ];
        cy.intercept('GET', '/admin/quiz-definitions/quiz1/runs?*', (req) => {
            req.reply({
                body: {
                    'data': runs,
                    'count': runs.length,
                    'totalCount': runs.length
                },
            });
        }).as('quizRuns')

        cy.createSurveyDef(1);
        cy.createSurveyMultipleChoiceQuestionDef(1, 1);
        cy.runQuizForUser(1, 1, [{selectedIndex: [1]}]);
        cy.visit('/administrator/quizzes/quiz1/runs');
        cy.wait('@quizRuns')
        cy.get(`${tableSelector} [data-pc-name="pcrowperpagedropdown"]`).click().get('[data-pc-section="option"]').contains('50').click();

        cy.get('[data-cy="row0-runtime"]').should('have.text', '0 ms')
        cy.get('[data-cy="row1-runtime"]').should('have.text', '5 ms')
        cy.get('[data-cy="row2-runtime"]').should('have.text', '999 ms')
        cy.get('[data-cy="row3-runtime"]').should('have.text', '1 second')
        cy.get('[data-cy="row4-runtime"]').should('have.text', '23 seconds')
        cy.get('[data-cy="row5-runtime"]').should('have.text', '59 seconds')
        cy.get('[data-cy="row6-runtime"]').should('have.text', '1 minute and 0 seconds')
        cy.get('[data-cy="row7-runtime"]').should('have.text', '1 minute and 1 second')
        cy.get('[data-cy="row8-runtime"]').should('have.text', '1 minute and 33 seconds')
        cy.get('[data-cy="row9-runtime"]').should('have.text', '1 minute and 59 seconds')
        cy.get('[data-cy="row10-runtime"]').should('have.text', '2 minutes and 1 second')
        cy.get('[data-cy="row11-runtime"]').should('have.text', '2 minutes and 59 seconds')
        cy.get('[data-cy="row12-runtime"]').should('have.text', '9 minutes and 59 seconds')
        cy.get('[data-cy="row13-runtime"]').should('have.text', '10 minutes')
        cy.get('[data-cy="row14-runtime"]').should('have.text', '59 minutes')
        cy.get('[data-cy="row15-runtime"]').should('have.text', '1 hour')
        cy.get('[data-cy="row16-runtime"]').should('have.text', '1 hour and 1 minute')
        cy.get('[data-cy="row17-runtime"]').should('have.text', '1 hour and 2 minutes')
        cy.get('[data-cy="row18-runtime"]').should('have.text', '1 hour and 33 minutes')
        cy.get('[data-cy="row19-runtime"]').should('have.text', '1 hour and 59 minutes')
        cy.get('[data-cy="row20-runtime"]').should('have.text', '2 hours')
        cy.get('[data-cy="row21-runtime"]').should('have.text', '2 hours and 1 minute')
        cy.get('[data-cy="row22-runtime"]').should('have.text', '9 hours and 59 minutes')
        cy.get('[data-cy="row23-runtime"]').should('have.text', '10 hours')
        cy.get('[data-cy="row24-runtime"]').should('have.text', '10 hours')
        cy.get('[data-cy="row25-runtime"]').should('have.text', '23 hours')
        cy.get('[data-cy="row26-runtime"]').should('have.text', '1 day')
        cy.get('[data-cy="row27-runtime"]').should('have.text', '1 day')
        cy.get('[data-cy="row28-runtime"]').should('have.text', '364 days')
        cy.get('[data-cy="row29-runtime"]').should('have.text', '1 year')
        cy.get('[data-cy="row30-runtime"]').should('have.text', '1 year')
        cy.get('[data-cy="row31-runtime"]').should('have.text', '1 year')
        cy.get('[data-cy="row32-runtime"]').should('have.text', '2 years')
    });

    it('delete a run', function () {
        cy.createSurveyDef(1);
        cy.createSurveyMultipleChoiceQuestionDef(1, 1);
        cy.runQuizForUser(1, 1, [{selectedIndex: [1]}]);
        cy.runQuizForUser(1, 2, [{selectedIndex: [0]}]);
        cy.runQuizForUser(1, 3, [{selectedIndex: [1]}]);
        cy.visit('/administrator/quizzes/quiz1/runs');
        cy.get('[data-cy="row0-userCell"]').contains('user3')
        cy.get('[data-cy="row1-userCell"]').contains('user2')
        cy.get('[data-cy="row2-userCell"]').contains('user1')

        cy.openDialog('[data-cy="row1-deleteBtn"]')
        cy.get('[data-cy="removalSafetyCheckMsg"]').contains('This will remove the Survey result for user2 user')
        cy.get('[data-cy="currentValidationText"]').type('Delete Me')
        cy.clickSaveDialogBtn()

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
        cy.visit('/administrator/quizzes/quiz1/runs');

        cy.get('[data-cy="row1-deleteBtn"]').click()
        cy.get('[data-cy="closeDialogBtn"]').click()
        cy.get('[data-cy="row1-deleteBtn"]').should('have.focus')

        cy.get('[data-cy="row1-deleteBtn"]').click()
        cy.get('.p-dialog-header [aria-label="Close"]').click()
        cy.get('[data-cy="row1-deleteBtn"]').should('have.focus')
    });

    it('view single passed survey run', function () {
        cy.createSurveyDef(1);
        cy.createSurveyMultipleChoiceQuestionDef(1, 1);
        cy.createSurveyMultipleChoiceQuestionDef(1, 2, { questionType: 'SingleChoice' });
        cy.createTextInputQuestionDef(1, 3);
        cy.runQuizForUser(1, 1, [{selectedIndex: [1]}, {selectedIndex: [0]}, {selectedIndex: [0]}])

        cy.visit('/administrator/quizzes/quiz1/runs');

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

        cy.visit('/administrator/quizzes/quiz1/runs');

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

        cy.visit('/administrator/quizzes/quiz1/runs');

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

        cy.visit('/administrator/quizzes/quiz1/runs');

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

        cy.visit('/administrator/quizzes/quiz1/runs');

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

        cy.visit('/administrator/quizzes/quiz1/runs');

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

    it('view single in progress quiz run with multiple matching questions', function () {
        cy.createQuizDef(1);
        cy.createQuizMatchingQuestionDef(1, 1);
        cy.createQuizMatchingQuestionDef(1, 2);
        cy.createQuizMatchingQuestionDef(1, 3);
        cy.createQuizMatchingQuestionDef(1, 4);
        cy.runQuizForUser(1, 1, [{selectedIndex: [1]}, {selectedIndex: [0, 1, 2]}, {selectedIndex: [0, 2, 1]}], false)

        cy.visit('/administrator/quizzes/quiz1/runs');

        cy.get('[data-cy="row0-viewRun"]').click()
        cy.get('[data-cy="quizRunStatus"]').contains('In Progress')
        cy.get('[data-cy="quizRunStatus"]').contains('2 / 4')

        cy.get('[data-cy="numQuestionsToPass"]').contains('1 / 4')
        cy.get('[data-cy="numQuestionsToPass"]').contains('Need 4 questions to pass')


        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="wrongAnswer"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="wrongAnswer"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="wrongAnswer"]').should('exist')
        cy.get('[data-cy="questionDisplayCard-4"] [data-cy="wrongAnswer"]').should('not.exist')

        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="noAnswerYet"]').should('exist')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="noAnswerYet"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="noAnswerYet"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-4"] [data-cy="noAnswerYet"]').should('exist')

        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="noAnswer"]').should('exist')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="noAnswer"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="noAnswer"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-4"] [data-cy="noAnswer"]').should('exist')

        const validateMatch = (qNum, rowNum, term, value, isCorrect) => {
            cy.get(`[data-cy="questionDisplayCard-${qNum}"] [data-cy="term-${rowNum}"]`).contains(term)
            cy.get(`[data-cy="questionDisplayCard-${qNum}"] [data-cy="match-${rowNum}"] [data-cy="matchVal"]`).contains(value)
            cy.get(`[data-cy="questionDisplayCard-${qNum}"] [data-cy="match-${rowNum}"] [data-cy="matchIsCorrect"]`).should(isCorrect ? 'exist' : 'not.exist')
            cy.get(`[data-cy="questionDisplayCard-${qNum}"] [data-cy="match-${rowNum}"] [data-cy="matchIsWrong"]`).should(isCorrect ? 'not.exist' : 'exist')
        }

        validateMatch(2, 0, 'First Term', 'First Answer', true)
        validateMatch(2, 1, 'Second Term', 'Second Answer', true)
        validateMatch(2, 2, 'Third Term', 'Third Answer', true)

        validateMatch(3, 0, 'First Term', 'First Answer', true)
        validateMatch(3, 1, 'Second Term', 'Third Answer', false)
        validateMatch(3, 2, 'Third Term', 'Second Answer', false)

    });

    it('filter quiz runs by date', function () {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.createQuizMultipleChoiceQuestionDef(1, 2);
        cy.createQuizQuestionDef(1, 3);
        cy.createQuizQuestionDef(1, 4);

        for(let x = 1; x <= 10; x++) {
            cy.runQuizForUser(1, x, [{selectedIndex: [1]}, {selectedIndex: [0, 3]}, {selectedIndex: [2]}])
            const dateInPast = moment('2025-10-01 00:00:00.0').add(x, 'days').format('YYYY-MM-DD 00:00:00.0');
            cy.execSql(`UPDATE user_quiz_attempt SET started = '${dateInPast}' WHERE user_id='${'user' + x}'`, true)
        }


        cy.visit('/administrator/quizzes/quiz1/runs');

        cy.get('[data-cy="metricsDateFilter"]').click()
        cy.filterSetYear(2025)
        cy.filterSetMonth('Oct')
        cy.setDay(1)
        cy.setDay(3)

        cy.get('[data-cy="applyDateFilterButton"]').click()

        cy.validateTable(tableSelector, [
            [{ colIndex: 0, value: 'user2' }, { colIndex: 2, value: 'Failed' }],
            [{ colIndex: 0, value: 'user1' }, { colIndex: 2, value: 'Failed' }],
        ], 10);

        cy.get('[data-cy="clearDateFilterButton"]').click()

        cy.get('[data-cy="metricsDateFilter"]').click()
        cy.filterSetYear(2025)
        cy.filterSetMonth('Oct')
        cy.setDay(3)
        cy.setDay(7)

        cy.get('[data-cy="applyDateFilterButton"]').click()

        cy.validateTable(tableSelector, [
            [{ colIndex: 0, value: 'user6' }, { colIndex: 2, value: 'Failed' }],
            [{ colIndex: 0, value: 'user5' }, { colIndex: 2, value: 'Failed' }],
            [{ colIndex: 0, value: 'user4' }, { colIndex: 2, value: 'Failed' }],
            [{ colIndex: 0, value: 'user3' }, { colIndex: 2, value: 'Failed' }],
            [{ colIndex: 0, value: 'user2' }, { colIndex: 2, value: 'Failed' }],
        ], 10);

        cy.get('[data-cy="clearDateFilterButton"]').click()

        cy.get('[data-cy="metricsDateFilter"]').click()
        cy.filterSetYear(2025)
        cy.filterSetMonth('Oct')
        cy.setDay(15)
        cy.setDay(20)
        cy.get('[data-cy="applyDateFilterButton"]').click()

        cy.get('.alert-info').contains('This chart needs at least 2 days worth of runs')
        cy.get('.alert-info').contains('No data yet')
        cy.get('[data-cy="quizRunsHistoryTable"]').contains('There are no records to show')

        cy.get('[data-cy="clearDateFilterButton"]').click()

        cy.validateTable(tableSelector, [
            [{ colIndex: 0, value: 'user10' }, { colIndex: 2, value: 'Failed' }],
            [{ colIndex: 0, value: 'user9' }, { colIndex: 2, value: 'Failed' }],
            [{ colIndex: 0, value: 'user8' }, { colIndex: 2, value: 'Failed' }],
            [{ colIndex: 0, value: 'user7' }, { colIndex: 2, value: 'Failed' }],
            [{ colIndex: 0, value: 'user6' }, { colIndex: 2, value: 'Failed' }],
            [{ colIndex: 0, value: 'user5' }, { colIndex: 2, value: 'Failed' }],
            [{ colIndex: 0, value: 'user4' }, { colIndex: 2, value: 'Failed' }],
            [{ colIndex: 0, value: 'user3' }, { colIndex: 2, value: 'Failed' }],
            [{ colIndex: 0, value: 'user2' }, { colIndex: 2, value: 'Failed' }],
            [{ colIndex: 0, value: 'user1' }, { colIndex: 2, value: 'Failed' }],
        ], 10);
    });

    it('quiz runs include results', function () {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.createQuizMultipleChoiceQuestionDef(1, 2);
        cy.createQuizQuestionDef(1, 3);
        cy.createQuizQuestionDef(1, 4);

        cy.runQuizForUser(1, 1, [{selectedIndex: [1]}, {selectedIndex: [0, 3]}, {selectedIndex: [2]}, {selectedIndex: [2]}])
        cy.runQuizForUser(1, 2, [{selectedIndex: [1]}, {selectedIndex: [0, 3]}, {selectedIndex: [2]}, {selectedIndex: [2]}])
        cy.runQuizForUser(1, 3, [{selectedIndex: [1]}, {selectedIndex: [0, 3]}, {selectedIndex: [2]}, {selectedIndex: [2]}])
        cy.runQuizForUser(1, 4, [{selectedIndex: [0]}, {selectedIndex: [0, 3]}, {selectedIndex: [2]}, {selectedIndex: [2]}])
        cy.runQuizForUser(1, 5, [{selectedIndex: [0]}, {selectedIndex: [0, 3]}, {selectedIndex: [2]}, {selectedIndex: [2]}])
        cy.runQuizForUser(1, 6, [{selectedIndex: [0]}, {selectedIndex: [0, 3]}, {selectedIndex: [2]}, {selectedIndex: [2]}])
        cy.runQuizForUser(1, 7, [{selectedIndex: [0]}, {selectedIndex: [0, 2]}, {selectedIndex: [2]}, {selectedIndex: [2]}])
        cy.runQuizForUser(1, 8, [{selectedIndex: [0]}, {selectedIndex: [0, 2]}, {selectedIndex: [2]}, {selectedIndex: [2]}])
        cy.runQuizForUser(1, 9, [{selectedIndex: [0]}, {selectedIndex: [0, 2]}, {selectedIndex: [2]}, {selectedIndex: [0]}])
        cy.runQuizForUser(1, 10, [{selectedIndex: [0]}, {selectedIndex: [0, 2]}, {selectedIndex: [2]}, {selectedIndex: [0]}])

        cy.visit('/administrator/quizzes/quiz1/runs');

        cy.validateTable(tableSelector, [
            [{ colIndex: 0, value: 'user10' }, { colIndex: 2, value: 'Passed' }, { colIndex: 5, value: '4 correctout of 4 (100%)' }],
            [{ colIndex: 0, value: 'user9' }, { colIndex: 2, value: 'Passed' }, { colIndex: 5, value: '4 correctout of 4 (100%)' }],
            [{ colIndex: 0, value: 'user8' }, { colIndex: 2, value: 'Failed' }, { colIndex: 5, value: '3 correctout of 4 (75%)' }],
            [{ colIndex: 0, value: 'user7' }, { colIndex: 2, value: 'Failed' }, { colIndex: 5, value: '3 correctout of 4 (75%)' }],
            [{ colIndex: 0, value: 'user6' }, { colIndex: 2, value: 'Failed' }, { colIndex: 5, value: '2 correctout of 4 (50%)' }],
            [{ colIndex: 0, value: 'user5' }, { colIndex: 2, value: 'Failed' }, { colIndex: 5, value: '2 correctout of 4 (50%)' }],
            [{ colIndex: 0, value: 'user4' }, { colIndex: 2, value: 'Failed' }, { colIndex: 5, value: '2 correctout of 4 (50%)' }],
            [{ colIndex: 0, value: 'user3' }, { colIndex: 2, value: 'Failed' }, { colIndex: 5, value: '1 correctout of 4 (25%)' }],
            [{ colIndex: 0, value: 'user2' }, { colIndex: 2, value: 'Failed' }, { colIndex: 5, value: '1 correctout of 4 (25%)' }],
            [{ colIndex: 0, value: 'user1' }, { colIndex: 2, value: 'Failed' }, { colIndex: 5, value: '1 correctout of 4 (25%)' }],
        ], 10);
    });
});
