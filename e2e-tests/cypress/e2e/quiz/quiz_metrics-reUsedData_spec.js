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

describe('Quiz Metrics With Reused Data Tests', () => {

    before(() => {
       cy.beforeTestSuiteThatReusesData()

        cy.createQuizDef(1, {name: 'Test Your Trivia Knowledge'});
        cy.createQuizQuestionDef(1, 1, { question: 'This is a Single Choice Question example for metrics.'})
        cy.createQuizMultipleChoiceQuestionDef(1, 2, { question: 'This is a Multiple Choice Question example for metrics.'});
        cy.createQuizMatchingQuestionDef(1, 3)

        cy.runQuizForUser(1, 1, [{selectedIndex: [0]}, {selectedIndex: [0,2]}, {selectedIndex: [0, 1, 2]}]);
        cy.runQuizForUser(1, 2, [{selectedIndex: [0]}, {selectedIndex: [0,2]}, {selectedIndex: [0, 1, 2]}], false);
        cy.runQuizForUser(1, 3, [{selectedIndex: [1]}, {selectedIndex: [0,2]}, {selectedIndex: [0, 1, 2]}]);
        cy.runQuizForUser(1, 3, [{selectedIndex: [0]}, {selectedIndex: [0,2]}, {selectedIndex: [0, 1, 2]}]);
        cy.runQuizForUser(1, 4, [{selectedIndex: [0]}, {selectedIndex: [0,2]}, {selectedIndex: [0, 1, 2]}]);
        cy.runQuizForUser(1, 5, [{selectedIndex: [1]}, {selectedIndex: [0,2]}, {selectedIndex: [0, 1, 2]}]);
        cy.runQuizForUser(1, 6, [{selectedIndex: [0]}, {selectedIndex: [0,2]}, {selectedIndex: [0, 1, 2]}]);
        cy.runQuizForUser(1, 7, [{selectedIndex: [0]}, {selectedIndex: [0,2]}, {selectedIndex: [0, 1, 2]}]);
        cy.runQuizForUser(1, 8, [{selectedIndex: [1]}, {selectedIndex: [0,2]}, {selectedIndex: [0, 1, 2]}]);
        cy.runQuizForUser(1, 9, [{selectedIndex: [1]}, {selectedIndex: [0,2]}, {selectedIndex: [0, 1, 2]}]);
        cy.runQuizForUser(1, 10, [{selectedIndex: [1]}, {selectedIndex: [0]}, {selectedIndex: [0, 1, 2]}]);
        cy.runQuizForUser(1, 11, [{selectedIndex: [0]}, {selectedIndex: [0]}, {selectedIndex: [0, 1, 2]}]);
    });

    after(() => {
        cy.afterTestSuiteThatReusesData()
    });

    beforeEach(() => {

    });

    it('quiz metrics summary cards', function () {
        cy.visit('/administrator/quizzes/quiz1/results');
        cy.get('[data-cy="metricsCardTotal"] [data-cy="statCardValue"]').should('have.text', '11')
        cy.get('[data-cy="metricsCardTotal"] [data-cy="statCardDescription"]').contains('11 attempts by 10 users')

        cy.get('[data-cy="metricsCardPassed"] [data-cy="statCardValue"]').should('have.text', '5')
        cy.get('[data-cy="metricsCardPassed"] [data-cy="statCardDescription"]').contains('5 attempts passed by 5 users')

        cy.get('[data-cy="metricsCardFailed"] [data-cy="statCardValue"]').should('have.text', '6')
        cy.get('[data-cy="metricsCardFailed"] [data-cy="statCardDescription"]').contains('6 attempts failed by 6 users')

        cy.get('[data-cy="metricsCardRuntime"] [data-cy="statCardDescription"]').contains('Average Quiz runtime for 11 attempts')
    });

    it('single choice question metrics', function () {
        cy.visit('/administrator/quizzes/quiz1/results');
        cy.get('[data-cy="metrics-q1"] [data-cy="qType"]').should('have.text', 'Single Choice')

        cy.get('[data-cy="metrics-q1"] [data-cy="row0-colAnswer"]').contains("Question 1 - First Answer")
        cy.get('[data-cy="metrics-q1"] [data-cy="row0-colAnswer"] [data-cy="checkbox-true"] .fa-check-square')

        cy.get('[data-cy="metrics-q1"] [data-cy="row1-colAnswer"]').contains("Question 1 - Second Answer")
        cy.get('[data-cy="metrics-q1"] [data-cy="row1-colAnswer"] [data-cy="checkbox-false"] .fa-square')

        cy.get('[data-cy="metrics-q1"] [data-cy="row2-colAnswer"]').contains("Question 1 - Third Answer")
        cy.get('[data-cy="metrics-q1"] [data-cy="row2-colAnswer"] [data-cy="checkbox-false"] .fa-square')

        cy.get('[data-cy="metrics-q1"] [data-p-index="0"] [data-cy="num"]').should('have.text', '6')
        cy.get('[data-cy="metrics-q1"] [data-p-index="0"] [data-cy="percent"]').should('have.text', '54%')
        cy.get('[data-cy="metrics-q1"] [data-p-index="0"] [data-pc-section="rowtogglebutton"]').should('be.enabled')

        cy.get('[data-cy="metrics-q1"] [data-p-index="1"] [data-cy="num"]').should('have.text', '5')
        cy.get('[data-cy="metrics-q1"] [data-p-index="1"] [data-cy="percent"]').should('have.text', '45%')
        cy.get('[data-cy="metrics-q1"] [data-p-index="1"] [data-pc-section="rowtogglebutton"]').should('be.enabled')

        cy.get('[data-cy="metrics-q1"] [data-p-index="2"] [data-cy="num"]').should('have.text', '0')
        cy.get('[data-cy="metrics-q1"] [data-p-index="2"] [data-cy="percent"]').should('have.text', '0%')
        cy.get('[data-cy="metrics-q1"] [data-p-index="2"] [data-pc-section="rowtogglebutton"]').should('not.be.visible')

        cy.get('[data-cy="metrics-q1"] [data-cy="multipleChoiceQuestionWarning"]').should('not.exist')
    });

    it('multiple choice question metrics', function () {
        cy.visit('/administrator/quizzes/quiz1/results');
        cy.get('[data-cy="metrics-q2"] [data-cy="row0-colAnswer"]').contains("First Answer")
        cy.get('[data-cy="metrics-q2"] [data-cy="row0-colAnswer"] [data-cy="checkbox-true"]')

        cy.get('[data-cy="metrics-q2"] [data-cy="row1-colAnswer"]').contains("Second Answer")
        cy.get('[data-cy="metrics-q2"] [data-cy="row1-colAnswer"] [data-cy="checkbox-false"]')

        cy.get('[data-cy="metrics-q2"] [data-cy="row2-colAnswer"]').contains("Third Answer")
        cy.get('[data-cy="metrics-q2"] [data-cy="row2-colAnswer"] [data-cy="checkbox-true"]')

        cy.get('[data-cy="metrics-q2"] [data-cy="row3-colAnswer"]').contains("Fourth Answer")
        cy.get('[data-cy="metrics-q2"] [data-cy="row3-colAnswer"] [data-cy="checkbox-false"]')

        cy.get('[data-cy="metrics-q2"] [data-p-index="0"] [data-cy="num"]').should('have.text', '11')
        cy.get('[data-cy="metrics-q2"] [data-p-index="0"] [data-cy="percent"]').should('have.text', '100%')
        cy.get('[data-cy="metrics-q2"] [data-p-index="0"] [data-pc-section="rowtogglebutton"]').should('be.enabled')

        cy.get('[data-cy="metrics-q2"] [data-p-index="1"] [data-cy="num"]').should('have.text', '0')
        cy.get('[data-cy="metrics-q2"] [data-p-index="1"] [data-cy="percent"]').should('have.text', '0%')
        cy.get('[data-cy="metrics-q2"] [data-p-index="1"] [data-pc-section="rowtogglebutton"]').should('not.be.visible')

        cy.get('[data-cy="metrics-q2"] [data-p-index="2"] [data-cy="num"]').should('have.text', '9')
        cy.get('[data-cy="metrics-q2"] [data-p-index="2"] [data-cy="percent"]').should('have.text', '81%')
        cy.get('[data-cy="metrics-q2"] [data-p-index="2"] [data-pc-section="rowtogglebutton"]').should('be.enabled')

        cy.get('[data-cy="metrics-q2"] [data-p-index="3"] [data-cy="num"]').should('have.text', '0')
        cy.get('[data-cy="metrics-q2"] [data-p-index="3"] [data-cy="percent"]').should('have.text', '0%')
        cy.get('[data-cy="metrics-q2"] [data-p-index="3"] [data-pc-section="rowtogglebutton"]').should('not.be.visible')

        cy.get('[data-cy="metrics-q2"] [data-cy="multipleChoiceQuestionWarning"]').should('exist')
    });

    it('single answer history', function () {
        cy.visit('/administrator/quizzes/quiz1/results');
        cy.get('[data-cy="metrics-q1"] [data-p-index="1"] [data-pc-section="rowtogglebutton"]').click()

        const tableSelector = '[data-cy="metrics-q1"] [data-cy="row1-answerHistory"] [data-cy="quizAnswerHistoryTable"]';
        const headerSelector = `${tableSelector} thead tr th`;
        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`).should('have.text', '5')
        cy.get(`${headerSelector} [data-cy="usrColumnHeader"]`)
            .click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0, value: 'user10' }],
            [{ colIndex: 0, value: 'user3' }],
            [{ colIndex: 0, value: 'user5' }],
            [{ colIndex: 0, value: 'user8' }],
            [{ colIndex: 0, value: 'user9' }],
        ], 10, true, 5, false);
    });

    it('sort column and order is saved in local storage', () => {
        cy.visit('/administrator/quizzes/quiz1/results');
        cy.get('[data-cy="metrics-q1"] [data-p-index="1"] [data-pc-section="rowtogglebutton"]').click()

        const tableSelector = '[data-cy="metrics-q1"] [data-cy="row1-answerHistory"] [data-cy="quizAnswerHistoryTable"]';
        const headerSelector = `${tableSelector} thead tr th`;
        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`).should('have.text', '5')

        cy.validateTable(tableSelector, [
            [{ colIndex: 0, value: 'user3' }],
            [{ colIndex: 0, value: 'user5' }],
            [{ colIndex: 0, value: 'user8' }],
            [{ colIndex: 0, value: 'user9' }],
            [{ colIndex: 0, value: 'user10' }],
        ], 5);

        // sort by user
        cy.get(`${headerSelector} [data-cy="usrColumnHeader"]`).click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0, value: 'user10' }],
            [{ colIndex: 0, value: 'user3' }],
            [{ colIndex: 0, value: 'user5' }],
            [{ colIndex: 0, value: 'user8' }],
            [{ colIndex: 0, value: 'user9' }],
        ], 5);

        cy.get(`${headerSelector} [data-cy="usrColumnHeader"]`).click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0, value: 'user9' }],
            [{ colIndex: 0, value: 'user8' }],
            [{ colIndex: 0, value: 'user5' }],
            [{ colIndex: 0, value: 'user3' }],
            [{ colIndex: 0, value: 'user10' }],
        ], 5);

        cy.visit('/administrator/quizzes/quiz1/results');
        cy.get('[data-cy="metrics-q1"] [data-p-index="1"] [data-pc-section="rowtogglebutton"]').click()
        cy.validateTable(tableSelector, [
            [{ colIndex: 0, value: 'user9' }],
            [{ colIndex: 0, value: 'user8' }],
            [{ colIndex: 0, value: 'user5' }],
            [{ colIndex: 0, value: 'user3' }],
            [{ colIndex: 0, value: 'user10' }],
        ], 5);
    });


});
