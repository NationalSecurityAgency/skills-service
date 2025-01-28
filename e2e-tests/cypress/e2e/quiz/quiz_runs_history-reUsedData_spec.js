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

describe('Quiz Runs History With Reused Data Tests', () => {

    before(() => {
       cy.beforeTestSuiteThatReusesData()

        cy.createQuizDef(1, {name: 'Test Your Trivia Knowledge'});
        cy.createQuizQuestionDef(1, 1)
        cy.runQuizForUser(1, 1, [{selectedIndex: [0]}]);
        cy.runQuizForUser(1, 2, [{selectedIndex: [0]}], false);
        cy.runQuizForUser(1, 3, [{selectedIndex: [1]}]);
        cy.runQuizForUser(1, 3, [{selectedIndex: [0]}]);
        cy.runQuizForUser(1, 4, [{selectedIndex: [0]}]);
        cy.runQuizForUser(1, 5, [{selectedIndex: [1]}]);
        cy.runQuizForUser(1, 6, [{selectedIndex: [0]}]);
        cy.runQuizForUser(1, 7, [{selectedIndex: [0]}]);
        cy.runQuizForUser(1, 8, [{selectedIndex: [1]}]);
        cy.runQuizForUser(1, 9, [{selectedIndex: [1]}]);
        cy.runQuizForUser(1, 10, [{selectedIndex: [1]}]);
        cy.runQuizForUser(1, 11, [{selectedIndex: [0]}]);
    });

    after(() => {
        cy.afterTestSuiteThatReusesData()
    });

    beforeEach(() => {

    });

    const tableSelector = '[data-cy="quizRunsHistoryTable"]'

    it('quiz run history table paging', function () {
        cy.visit('/administrator/quizzes/quiz1/runs');

        cy.validateTable(tableSelector, [
            [{ colIndex: 0, value: 'user11' }, { colIndex: 2, value: 'Passed' }],
            [{ colIndex: 0, value: 'user10' }, { colIndex: 2, value: 'Failed' }],
            [{ colIndex: 0, value: 'user9' }, { colIndex: 2, value: 'Failed' }],
            [{ colIndex: 0, value: 'user8' }, { colIndex: 2, value: 'Failed' }],
            [{ colIndex: 0, value: 'user7' }, { colIndex: 2, value: 'Passed' }],
            [{ colIndex: 0, value: 'user6' }, { colIndex: 2, value: 'Passed' }],
            [{ colIndex: 0, value: 'user5' }, { colIndex: 2, value: 'Failed' }],
            [{ colIndex: 0, value: 'user4' }, { colIndex: 2, value: 'Passed' }],
            [{ colIndex: 0, value: 'user3' }, { colIndex: 2, value: 'Passed' }],
            [{ colIndex: 0, value: 'user3' }, { colIndex: 2, value: 'Failed' }],
            [{ colIndex: 0, value: 'user2' }, { colIndex: 2, value: 'In Progress' }],
            [{ colIndex: 0, value: 'user1' }, { colIndex: 2, value: 'Passed' }],
        ], 10);
    });

    it('sort by user', function () {
        cy.visit('/administrator/quizzes/quiz1/runs');
        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`).should('have.text', '12')
        const headerSelector = `${tableSelector} thead tr th`;
        cy.get(headerSelector)
            .contains('User')
            .click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0, value: 'user1' }, { colIndex: 2, value: 'Passed' }],
            [{ colIndex: 0, value: 'user10' }, { colIndex: 2, value: 'Failed' }],
            [{ colIndex: 0, value: 'user11' }, { colIndex: 2, value: 'Passed' }],
            [{ colIndex: 0, value: 'user2' }, { colIndex: 2, value: 'In Progress' }],
            [{ colIndex: 0, value: 'user3' }],
            [{ colIndex: 0, value: 'user3' }],
            [{ colIndex: 0, value: 'user4' }, { colIndex: 2, value: 'Passed' }],
            [{ colIndex: 0, value: 'user5' }, { colIndex: 2, value: 'Failed' }],
            [{ colIndex: 0, value: 'user6' }, { colIndex: 2, value: 'Passed' }],
            [{ colIndex: 0, value: 'user7' }, { colIndex: 2, value: 'Passed' }],
            [{ colIndex: 0, value: 'user8' }, { colIndex: 2, value: 'Failed' }],
            [{ colIndex: 0, value: 'user9' }, { colIndex: 2, value: 'Failed' }],
        ], 10);
    });

    it('sort by status', function () {
        cy.visit('/administrator/quizzes/quiz1/runs');
        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`).should('have.text', '12')
        const headerSelector = `${tableSelector} thead tr th`;
        cy.get(headerSelector)
            .contains('Status')
            .click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 2, value: 'Failed' }],
            [{ colIndex: 2, value: 'Failed' }],
            [{ colIndex: 2, value: 'Failed' }],
            [{ colIndex: 2, value: 'Failed' }],
            [{ colIndex: 2, value: 'Failed' }],
            [{ colIndex: 0, value: 'user2' }, { colIndex: 2, value: 'In Progress' }],
            [{ colIndex: 2, value: 'Passed' }],
            [{ colIndex: 2, value: 'Passed' }],
            [{ colIndex: 2, value: 'Passed' }],
            [{ colIndex: 2, value: 'Passed' }],
            [{ colIndex: 2, value: 'Passed' }],
            [{ colIndex: 2, value: 'Passed' }],
        ], 10);
    });

    it('quiz run history page size', function () {
        cy.visit('/administrator/quizzes/quiz1/runs');
        cy.get(`${tableSelector} [data-pc-name="pcrowperpagedropdown"]`).click().get('[data-pc-section="option"]').contains('20').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0, value: 'user11' }, { colIndex: 2, value: 'Passed' }],
            [{ colIndex: 0, value: 'user10' }, { colIndex: 2, value: 'Failed' }],
            [{ colIndex: 0, value: 'user9' }, { colIndex: 2, value: 'Failed' }],
            [{ colIndex: 0, value: 'user8' }, { colIndex: 2, value: 'Failed' }],
            [{ colIndex: 0, value: 'user7' }, { colIndex: 2, value: 'Passed' }],
            [{ colIndex: 0, value: 'user6' }, { colIndex: 2, value: 'Passed' }],
            [{ colIndex: 0, value: 'user5' }, { colIndex: 2, value: 'Failed' }],
            [{ colIndex: 0, value: 'user4' }, { colIndex: 2, value: 'Passed' }],
            [{ colIndex: 0, value: 'user3' }, { colIndex: 2, value: 'Passed' }],
            [{ colIndex: 0, value: 'user3' }, { colIndex: 2, value: 'Failed' }],
            [{ colIndex: 0, value: 'user2' }, { colIndex: 2, value: 'In Progress' }],
            [{ colIndex: 0, value: 'user1' }, { colIndex: 2, value: 'Passed' }],
        ], 20, true);
    });

    it('filter by user name', function () {
        cy.visit('/administrator/quizzes/quiz1/runs');
        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`).should('have.text', '12')
        cy.get('[data-cy="userNameFilter"]').type(' SeR1{enter}')
        cy.validateTable(tableSelector, [
            [{ colIndex: 0, value: 'user11' }, { colIndex: 2, value: 'Passed' }],
            [{ colIndex: 0, value: 'user10' }, { colIndex: 2, value: 'Failed' }],
            [{ colIndex: 0, value: 'user1' }, { colIndex: 2, value: 'Passed' }],
        ], 10);

        cy.get('[data-cy="userNameFilter"]').type('0 ')
        cy.get('[data-cy="userFilterBtn"]').click()
        cy.validateTable(tableSelector, [
            [{ colIndex: 0, value: 'user10' }, { colIndex: 2, value: 'Failed' }],
        ], 10);

        cy.get('[data-cy="userResetBtn"]').click()
        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`).should('have.text', '12')
    });


    it('sort column and order is saved in local storage', () => {
        cy.visit('/administrator/quizzes/quiz1/runs');

        // initial sort order
        cy.validateTable(tableSelector, [
            [{ colIndex: 0, value: 'user11' }, { colIndex: 2, value: 'Passed' }],
            [{ colIndex: 0, value: 'user10' }, { colIndex: 2, value: 'Failed' }],
            [{ colIndex: 0, value: 'user9' }, { colIndex: 2, value: 'Failed' }],
            [{ colIndex: 0, value: 'user8' }, { colIndex: 2, value: 'Failed' }],
            [{ colIndex: 0, value: 'user7' }, { colIndex: 2, value: 'Passed' }],
            [{ colIndex: 0, value: 'user6' }, { colIndex: 2, value: 'Passed' }],
            [{ colIndex: 0, value: 'user5' }, { colIndex: 2, value: 'Failed' }],
            [{ colIndex: 0, value: 'user4' }, { colIndex: 2, value: 'Passed' }],
            [{ colIndex: 0, value: 'user3' }, { colIndex: 2, value: 'Passed' }],
            [{ colIndex: 0, value: 'user3' }, { colIndex: 2, value: 'Failed' }],
            [{ colIndex: 0, value: 'user2' }, { colIndex: 2, value: 'In Progress' }],
            [{ colIndex: 0, value: 'user1' }, { colIndex: 2, value: 'Passed' }],
        ], 10);

        // sort by user
        const headerSelector = `${tableSelector} thead tr th`;
        cy.get(headerSelector)
          .contains('User')
          .click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0, value: 'user1' }, { colIndex: 2, value: 'Passed' }],
            [{ colIndex: 0, value: 'user10' }, { colIndex: 2, value: 'Failed' }],
            [{ colIndex: 0, value: 'user11' }, { colIndex: 2, value: 'Passed' }],
            [{ colIndex: 0, value: 'user2' }, { colIndex: 2, value: 'In Progress' }],
            [{ colIndex: 0, value: 'user3' }],
            [{ colIndex: 0, value: 'user3' }],
            [{ colIndex: 0, value: 'user4' }, { colIndex: 2, value: 'Passed' }],
            [{ colIndex: 0, value: 'user5' }, { colIndex: 2, value: 'Failed' }],
            [{ colIndex: 0, value: 'user6' }, { colIndex: 2, value: 'Passed' }],
            [{ colIndex: 0, value: 'user7' }, { colIndex: 2, value: 'Passed' }],
            [{ colIndex: 0, value: 'user8' }, { colIndex: 2, value: 'Failed' }],
            [{ colIndex: 0, value: 'user9' }, { colIndex: 2, value: 'Failed' }],
        ], 10);

        cy.get(headerSelector)
          .contains('User')
          .click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0, value: 'user9' }, { colIndex: 2, value: 'Failed' }],
            [{ colIndex: 0, value: 'user8' }, { colIndex: 2, value: 'Failed' }],
            [{ colIndex: 0, value: 'user7' }, { colIndex: 2, value: 'Passed' }],
            [{ colIndex: 0, value: 'user6' }, { colIndex: 2, value: 'Passed' }],
            [{ colIndex: 0, value: 'user5' }, { colIndex: 2, value: 'Failed' }],
            [{ colIndex: 0, value: 'user4' }, { colIndex: 2, value: 'Passed' }],
            [{ colIndex: 0, value: 'user3' }],
            [{ colIndex: 0, value: 'user3' }],
            [{ colIndex: 0, value: 'user2' }, { colIndex: 2, value: 'In Progress' }],
            [{ colIndex: 0, value: 'user11' }, { colIndex: 2, value: 'Passed' }],
            [{ colIndex: 0, value: 'user10' }, { colIndex: 2, value: 'Failed' }],
            [{ colIndex: 0, value: 'user1' }, { colIndex: 2, value: 'Passed' }],
        ], 10);

        // refresh and validate
        cy.visit('/administrator/quizzes/quiz1/runs');
        cy.validateTable(tableSelector, [
            [{ colIndex: 0, value: 'user9' }, { colIndex: 2, value: 'Failed' }],
            [{ colIndex: 0, value: 'user8' }, { colIndex: 2, value: 'Failed' }],
            [{ colIndex: 0, value: 'user7' }, { colIndex: 2, value: 'Passed' }],
            [{ colIndex: 0, value: 'user6' }, { colIndex: 2, value: 'Passed' }],
            [{ colIndex: 0, value: 'user5' }, { colIndex: 2, value: 'Failed' }],
            [{ colIndex: 0, value: 'user4' }, { colIndex: 2, value: 'Passed' }],
            [{ colIndex: 0, value: 'user3' }],
            [{ colIndex: 0, value: 'user3' }],
            [{ colIndex: 0, value: 'user2' }, { colIndex: 2, value: 'In Progress' }],
            [{ colIndex: 0, value: 'user11' }, { colIndex: 2, value: 'Passed' }],
            [{ colIndex: 0, value: 'user10' }, { colIndex: 2, value: 'Failed' }],
            [{ colIndex: 0, value: 'user1' }, { colIndex: 2, value: 'Passed' }],
        ], 10);
    });

});
