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

describe('Survey Metrics With Reused Data Tests', () => {

    before(() => {
       cy.beforeTestSuiteThatReusesData()

        cy.createSurveyDef(2);
        cy.createSurveyMultipleChoiceQuestionDef(2, 1);
        cy.createTextInputQuestionDef(2, 2, { question: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum' });
        cy.createSurveyMultipleChoiceQuestionDef(2, 3, { questionType: 'SingleChoice' });

        cy.runQuizForUser(2, 1, [{selectedIndex: [0, 2]}, {selectedIndex: [0]}, {selectedIndex: [0]}], true, 'A1: Some very interesting text');
        cy.runQuizForUser(2, 2, [{selectedIndex: [0]}, {selectedIndex: [0]}, {selectedIndex: [2]}], true, 'A2: Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum');
        cy.runQuizForUser(2, 3, [{selectedIndex: [2]}, {selectedIndex: [0]}, {selectedIndex: [1]}], true, 'A3: This is a short one');

        cy.runQuizForUser(2, 5, [{selectedIndex: [0, 1]}, {selectedIndex: [0]}, {selectedIndex: [0]}], false, 'should not be visible');

        cy.runQuizForUser(2, 6, [{selectedIndex: [2]}, {selectedIndex: [0]}, {selectedIndex: [1]}], true, 'A4: Yes another answer');
        cy.runQuizForUser(2, 7, [{selectedIndex: [2]}, {selectedIndex: [0]}, {selectedIndex: [1]}], true, 'A5: Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo. Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit, sed quia consequuntur magni dolores eos qui ratione voluptatem sequi nesciunt. Neque porro quisquam est, qui dolorem ipsum quia dolor sit amet, consectetur, adipisci velit, sed quia non numquam eius modi tempora incidunt ut labore et dolore magnam aliquam quaerat voluptatem. Ut enim ad minima veniam, quis nostrum exercitationem ullam corporis suscipit laboriosam, nisi ut aliquid ex ea commodi consequatur? Quis autem vel eum iure reprehenderit qui in ea voluptate velit esse quam nihil molestiae consequatur, vel illum qui dolorem eum fugiat quo voluptas nulla pariatur? \n\n Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo. Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit, sed quia consequuntur magni dolores eos qui ratione voluptatem sequi nesciunt. Neque porro quisquam est, qui dolorem ipsum quia dolor sit amet, consectetur, adipisci velit, sed quia non numquam eius modi tempora incidunt ut labore et dolore magnam aliquam quaerat voluptatem. Ut enim ad minima veniam, quis nostrum exercitationem ullam corporis suscipit laboriosam, nisi ut aliquid ex ea commodi consequatur? Quis autem vel eum iure reprehenderit qui in ea voluptate velit esse quam nihil molestiae consequatur, vel illum qui dolorem eum fugiat quo voluptas nulla pariatur? End is truncated');
        cy.runQuizForUser(2, 8, [{selectedIndex: [2]}, {selectedIndex: [0]}, {selectedIndex: [1]}], true, 'A6: Answer oh answer');
        cy.runQuizForUser(2, 9, [{selectedIndex: [2]}, {selectedIndex: [0]}, {selectedIndex: [1]}], true, 'A7: Answer oh answer');
        cy.runQuizForUser(2, 10, [{selectedIndex: [2]}, {selectedIndex: [0]}, {selectedIndex: [1]}], true, 'A8: Answer oh answer');
    });

    after(() => {
        cy.afterTestSuiteThatReusesData()
    });

    beforeEach(() => {

    });

    it('survey metrics summary cards', function () {
        cy.visit('/administrator/quizzes/quiz2/results');
        cy.get('[data-cy="metricsCardTotal"] [data-cy="statCardValue"]').should('have.text', '8')
        cy.get('[data-cy="metricsCardTotal"] [data-cy="statCardDescription"]').contains('Survey was completed 8 times')
        cy.get('[data-cy="metricsCardRuntime"] [data-cy="statCardDescription"]').contains('Average Survey runtime for 8 users')

        cy.get('[data-cy="metricsCardPassed"]').should('not.exist')
        cy.get('[data-cy="metricsCardFailed"]').should('not.exist')
    });

    it('question type is displayed next to each question', function () {
        cy.visit('/administrator/quizzes/quiz2/results');
        cy.get('[data-cy="metrics-q1"] [data-cy="qType"]').should('have.text', 'Multiple Choice')
        cy.get('[data-cy="metrics-q2"] [data-cy="qType"]').should('have.text', 'Text Input')
        cy.get('[data-cy="metrics-q3"] [data-cy="qType"]').should('have.text', 'Single Choice')
    });

    it('multiple choice question metrics', function () {
        cy.visit('/administrator/quizzes/quiz2/results');
        cy.get('[data-cy="metrics-q1"] [data-cy="row0-colAnswer"]').contains("Question 1 - First Answer")
        cy.get('[data-cy="metrics-q1"] [data-cy="row1-colAnswer"]').contains("Question 1 - Second Answer")
        cy.get('[data-cy="metrics-q1"] [data-cy="row2-colAnswer"]').contains("Question 1 - Third Answer")

        cy.get('[data-cy="metrics-q1"] [data-p-index="0"] [data-cy="num"]').should('have.text', '2')
        cy.get('[data-cy="metrics-q1"] [data-p-index="0"] [data-cy="percent"]').should('have.text', '25%')
        cy.get('[data-cy="metrics-q1"] [data-p-index="0"] [data-pc-section="rowtogglebutton"]').should('be.enabled')

        cy.get('[data-cy="metrics-q1"] [data-p-index="1"] [data-cy="num"]').should('have.text', '0')
        cy.get('[data-cy="metrics-q1"] [data-p-index="1"] [data-cy="percent"]').should('have.text', '0%')
        cy.get('[data-cy="metrics-q1"] [data-p-index="1"] [data-pc-section="rowtogglebutton"]').should('not.be.visible')

        cy.get('[data-cy="metrics-q1"] [data-p-index="2"] [data-cy="num"]').should('have.text', '7')
        cy.get('[data-cy="metrics-q1"] [data-p-index="2"] [data-cy="percent"]').should('have.text', '87%')
        cy.get('[data-cy="metrics-q1"] [data-p-index="2"] [data-pc-section="rowtogglebutton"]').should('be.enabled')
    });

    it('single answer history with paging', function () {
        cy.visit('/administrator/quizzes/quiz2/results');
        cy.get('[data-cy="metrics-q1"] [data-p-index="2"] [data-pc-section="rowtogglebutton"]').click()

        // const tableSelector = '[data-cy="metrics-q1"] [data-cy="quizAnswerHistoryTable"]';
        const tableSelector = '[data-cy="metrics-q1"] [data-cy="row2-answerHistory"] [data-cy="quizAnswerHistoryTable"]';
        const headerSelector = `${tableSelector} thead tr th`;
        cy.get(tableSelector).contains('View Run')
        cy.get(headerSelector)
            .contains('User')
            .click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0, value: 'user1' }],
            [{ colIndex: 0, value: 'user10' }],
            [{ colIndex: 0, value: 'user3' }],
            [{ colIndex: 0, value: 'user6' }],
            [{ colIndex: 0, value: 'user7' }],
            [{ colIndex: 0, value: 'user8' }],
            [{ colIndex: 0, value: 'user9' }],
        ], 5);
    });

    it('single answer history expand to a larger page size', function () {
        cy.visit('/administrator/quizzes/quiz2/results');
        cy.get('[data-cy="metrics-q1"] [data-p-index="2"] [data-pc-section="rowtogglebutton"]').click()

        const tableSelector = '[data-cy="metrics-q1"] [data-cy="row2-answerHistory"] [data-cy="quizAnswerHistoryTable"]';
        const headerSelector = `${tableSelector} thead tr th`;
        cy.get(tableSelector).contains('View Run')
        cy.get(headerSelector)
            .contains('User')
            .click();
        cy.get(`${tableSelector} [data-pc-name="pcrowperpagedropdown"]`).click().get('[data-pc-section="option"]').contains('10').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0, value: 'user1' }],
            [{ colIndex: 0, value: 'user10' }],
            [{ colIndex: 0, value: 'user3' }],
            [{ colIndex: 0, value: 'user6' }],
            [{ colIndex: 0, value: 'user7' }],
            [{ colIndex: 0, value: 'user8' }],
            [{ colIndex: 0, value: 'user9' }],
        ], 10);
    });

    it('text input question', function () {
        cy.visit('/administrator/quizzes/quiz2/results');
        const tableSelector = '[data-cy="metrics-q2"] [data-cy="quizAnswerHistoryTable"]';
        const headerSelector = `${tableSelector} thead tr th`;
        cy.get(tableSelector).contains('View Run')
        cy.get(headerSelector)
            .contains('User')
            .click();

        const qNum= 2
        cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="expandAll"]`).click()
        cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-cy="row0-colAnswerTxt"]`).should('contain', 'A1:')
        cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-cy="row1-colAnswerTxt"]`).should('contain', 'A8:')
        cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-cy="row2-colAnswerTxt"]`).should('contain', 'A2:')
        cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-cy="row3-colAnswerTxt"]`).should('contain', 'A3:')
        cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-cy="row4-colAnswerTxt"]`).should('contain', 'A4:')

        cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-p-index="0"]`).contains('user1')
        cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-p-index="1"]`).contains('user10')
        cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-p-index="2"]`).contains('user2')
        cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-p-index="3"]`).contains('user3')
        cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-p-index="4"]`).contains('user6')

        cy.get(`[data-cy="metrics-q${qNum}"] [data-pc-name="pcpaginator"] [aria-label="Page 2"]`).click()
        cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-cy="row0-colAnswerTxt"]`).should('contain', 'A5:')
        cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-cy="row1-colAnswerTxt"]`).should('contain', 'A6:')
        cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-cy="row2-colAnswerTxt"]`).should('contain', 'A7:')
        cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-p-index="0"]`).contains('user7')
        cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-p-index="1"]`).contains('user8')
        cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-p-index="2"]`).contains('user9')
    });

    it('single choice question metrics', function () {
        cy.visit('/administrator/quizzes/quiz2/results');
        cy.get('[data-cy="metrics-q3"] [data-cy="row0-colAnswer"]').contains("Question 3 - First Answer")
        cy.get('[data-cy="metrics-q3"] [data-cy="row1-colAnswer"]').contains("Question 3 - Second Answer")
        cy.get('[data-cy="metrics-q3"] [data-cy="row2-colAnswer"]').contains("Question 3 - Third Answer")

        cy.get('[data-cy="metrics-q3"] [data-p-index="0"] [data-cy="num"]').should('have.text', '1')
        cy.get('[data-cy="metrics-q3"] [data-p-index="0"] [data-cy="percent"]').should('have.text', '12%')
        cy.get('[data-cy="metrics-q3"] [data-p-index="0"] [data-pc-section="rowtogglebutton"]').should('be.enabled')

        cy.get('[data-cy="metrics-q3"] [data-p-index="1"] [data-cy="num"]').should('have.text', '6')
        cy.get('[data-cy="metrics-q3"] [data-p-index="1"] [data-cy="percent"]').should('have.text', '75%')
        cy.get('[data-cy="metrics-q3"] [data-p-index="1"] [data-pc-section="rowtogglebutton"]').should('be.enabled')

        cy.get('[data-cy="metrics-q3"] [data-p-index="2"] [data-cy="num"]').should('have.text', '1')
        cy.get('[data-cy="metrics-q3"] [data-p-index="2"] [data-cy="percent"]').should('have.text', '12%')
        cy.get('[data-cy="metrics-q3"] [data-p-index="2"] [data-pc-section="rowtogglebutton"]').should('be.enabled')

        cy.get('[data-cy="metrics-q3"] [data-p-index="0"] [data-pc-section="rowtogglebutton"]').click()
        cy.get('[data-cy="metrics-q3"] [data-cy="row0-answerHistory"] [data-cy="row0-colUserId"]').contains('user1')
    });
});
