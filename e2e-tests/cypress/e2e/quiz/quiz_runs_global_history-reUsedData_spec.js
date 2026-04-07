/*
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
import dayjs from 'dayjs';
import utcPlugin from 'dayjs/plugin/utc';
import moment from "moment-timezone";

dayjs.extend(utcPlugin);

describe('Global Quiz Runs History Tests', () => {

    const tableSelector = '[data-cy="quizRunsHistoryTable"]'

    before(() => {
        cy.beforeTestSuiteThatReusesData()

        cy.createQuizDef(1)
        cy.createQuizQuestionDef(1, 1)

        cy.createQuizDef(2)
        cy.createQuizQuestionDef(2, 1)

        cy.createSurveyDef(3)
        cy.createSurveyMultipleChoiceQuestionDef(3, 1)

        const users = ['user1', 'user2', 'user3', 'user4', 'user5']

        // Randomized quiz runs for all users
        const quizRuns = [
            // user 5
            { quizId: 1, user: users[4], answers: [{selectedIndex: [0]}], completed: true },
            // user 3
            { quizId: 1, user: users[2], answers: [{selectedIndex: [1]}], completed: true },
            // user 4
            { quizId: 3, user: users[3], answers: [{selectedIndex: [0]}], completed: true },
            // user 1
            { quizId: 1, user: users[0], answers: [{selectedIndex: [0]}], completed: true },
            // user 5
            { quizId: 2, user: users[4], answers: [{selectedIndex: [1]}], completed: true },
            // user 3
            { quizId: 3, user: users[2], answers: [{selectedIndex: [0]}], completed: true },
            // user 5
            { quizId: 2, user: users[4], answers: [{selectedIndex: [0]}], completed: true },
            // user 2
            { quizId: 3, user: users[1], answers: [{selectedIndex: [0]}], completed: true },
            // user 4
            { quizId: 2, user: users[3], answers: [{selectedIndex: [1]}], completed: true },
            // user 3
            { quizId: 2, user: users[2], answers: [{selectedIndex: [0]}], completed: false },
            // user 4
            { quizId: 1, user: users[3], answers: [{selectedIndex: [1]}], completed: true },
            // user 1
            { quizId: 2, user: users[0], answers: [{selectedIndex: [0]}], completed: true },
            // user 3
            { quizId: 1, user: users[2], answers: [{selectedIndex: [0]}], completed: true },
            // user 5
            { quizId: 3, user: users[4], answers: [{selectedIndex: [1]}], completed: false },
        ]

        quizRuns.forEach((run, index) => {
            const dateInPast = moment().subtract(quizRuns.length - index, 'days').format('YYYY-MM-DD 00:00:00.0');
            cy.runQuizForUser(run.quizId, run.user, run.answers, run.completed, null, dateInPast)
        })

        cy.addUserTag([{
            tagKey: 'dutyOrganization',
            tags: ['ABC']
        }, {
            tagKey: 'dutyOrganization',
            tags: ['ABC1']
        }, {
            tagKey: 'dutyOrganization',
            tags: ['XYZ']
        }, {
            tagKey: 'dutyOrganization',
            tags: ['XYZ1']
        }]);
    })

    after(() => {
        cy.afterTestSuiteThatReusesData()
    });

    afterEach(() => {
        cy.clearGlobalMetricsExcludedItems()
    })

    it('view runs', function () {
        cy.intercept('/app/quiz-runs**').as('quizRuns')
        cy.visit('/administrator/quiz-runs');
        cy.wait('@quizRuns')
        cy.get('[data-cy="quizRunsHistoryTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '14')

        const expected = [
            [{ colIndex: 0, value: 'user5 (LastName, Firstname)'}, { colIndex: 1, value: 'This is survey 3'},
                { colIndex: 2, value: 'Survey'},  { colIndex: 3, value: ''}, { colIndex: 4, value: 'In Progress'}],
            [{ colIndex: 0, value: 'user3 (LastName, Firstname)'}, { colIndex: 1, value: 'This is quiz 1'},
                { colIndex: 2, value: 'Quiz'},  { colIndex: 3, value: 'XYZ'}, { colIndex: 4, value: 'Passed'}],
            [{ colIndex: 0, value: 'user1 (LastName, Firstname)'}, { colIndex: 1, value: 'This is quiz 2'},
                { colIndex: 2, value: 'Quiz'},  { colIndex: 3, value: 'ABC'}, { colIndex: 4, value: 'Passed'}],
            [{ colIndex: 0, value: 'user4 (LastName, Firstname)'}, { colIndex: 1, value: 'This is quiz 1'},
                { colIndex: 2, value: 'Quiz'},  { colIndex: 3, value: 'XYZ1'},  { colIndex: 4, value: 'Failed'}],
            [{ colIndex: 0, value: 'user3 (LastName, Firstname)'}, { colIndex: 1, value: 'This is quiz 2'},
                { colIndex: 2, value: 'Quiz'},  { colIndex: 3, value: 'XYZ'}, { colIndex: 4, value: 'In Progress'}],
            [{ colIndex: 0, value: 'user4 (LastName, Firstname)'}, { colIndex: 1, value: 'This is quiz 2'},
                { colIndex: 2, value: 'Quiz'},  { colIndex: 3, value: 'XYZ1'}, { colIndex: 4, value: 'Failed'}],
            [{ colIndex: 0, value: 'user2 (LastName, Firstname)'}, { colIndex: 1, value: 'This is survey 3'},
                { colIndex: 2, value: 'Survey'},  { colIndex: 3, value: 'ABC1'}, { colIndex: 4, value: 'Completed'}],
            [{ colIndex: 0, value: 'user5 (LastName, Firstname)'}, { colIndex: 1, value: 'This is quiz 2'},
                { colIndex: 2, value: 'Quiz'},  { colIndex: 3, value: ''}, { colIndex: 4, value: 'Passed'}],
            [{ colIndex: 0, value: 'user3 (LastName, Firstname)'}, { colIndex: 1, value: 'This is survey 3'},
                { colIndex: 2, value: 'Survey'},  { colIndex: 3, value: 'XYZ'},  { colIndex: 4, value: 'Completed'}],
            [{ colIndex: 0, value: 'user5 (LastName, Firstname)'}, { colIndex: 1, value: 'This is quiz 2'},
                { colIndex: 2, value: 'Quiz'},  { colIndex: 3, value: ''},  { colIndex: 4, value: 'Failed'}],
            [{ colIndex: 0, value: 'user1 (LastName, Firstname)'}, { colIndex: 1, value: 'This is quiz 1'},
                { colIndex: 2, value: 'Quiz'},  { colIndex: 3, value: 'ABC'},  { colIndex: 4, value: 'Passed'}],
            [{ colIndex: 0, value: 'user4 (LastName, Firstname)'}, { colIndex: 1, value: 'This is survey 3'},
                { colIndex: 2, value: 'Survey'},  { colIndex: 3, value: 'XYZ1'},  { colIndex: 4, value: 'Completed'}],
            [{ colIndex: 0, value: 'user3 (LastName, Firstname)'}, { colIndex: 1, value: 'This is quiz 1'},
                { colIndex: 2, value: 'Quiz'},  { colIndex: 3, value: 'XYZ'},  { colIndex: 4, value: 'Failed'}],
            [{ colIndex: 0, value: 'user5 (LastName, Firstname)'}, { colIndex: 1, value: 'This is quiz 1'},
                { colIndex: 2, value: 'Quiz'},  { colIndex: 3, value: ''},  { colIndex: 4, value: 'Passed'}],
        ];

        cy.validateTable(tableSelector, expected, 10);
    });

    it('sort by user', function () {
        cy.intercept('/app/quiz-runs**').as('quizRuns')
        cy.visit('/administrator/quiz-runs');
        cy.wait('@quizRuns')
        cy.get('[data-cy="quizRunsHistoryTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '14')

        cy.get('[data-pc-name="headercell"] [data-pc-section="columnheadercontent"]').contains('User').click();

        const expected = [
            [{ colIndex: 0, value: 'user1'}],
            [{ colIndex: 0, value: 'user1'}],
            [{ colIndex: 0, value: 'user2'}],
            [{ colIndex: 0, value: 'user3'}],
            [{ colIndex: 0, value: 'user3'}],
            [{ colIndex: 0, value: 'user3'}],
            [{ colIndex: 0, value: 'user3'}],
            [{ colIndex: 0, value: 'user4'}],
            [{ colIndex: 0, value: 'user4'}],
            [{ colIndex: 0, value: 'user4'}],
            [{ colIndex: 0, value: 'user5'}],
            [{ colIndex: 0, value: 'user5'}],
            [{ colIndex: 0, value: 'user5'}],
            [{ colIndex: 0, value: 'user5'}],
        ]
        cy.validateTable(tableSelector, expected, 10);

        cy.get('[data-pc-name="headercell"] [data-pc-section="columnheadercontent"]').contains('User').click();
        const expectedReversed = [...expected].reverse()
        cy.validateTable(tableSelector, expectedReversed, 10);

        // refresh and re-validate sort
        cy.intercept('/app/quiz-runs**').as('quizRuns')
        cy.visit('/administrator/quiz-runs');
        cy.wait('@quizRuns')
        cy.get('[data-cy="quizRunsHistoryTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '14')

        cy.validateTable(tableSelector, expectedReversed, 10);
    });

    it('sort by name', function () {
        cy.intercept('/app/quiz-runs**').as('quizRuns')
        cy.visit('/administrator/quiz-runs');
        cy.wait('@quizRuns')
        cy.get('[data-cy="quizRunsHistoryTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '14')

        cy.get('[data-pc-name="headercell"] [data-pc-section="columnheadercontent"]').contains('Name').click();

        const expected = [
            [{ colIndex: 1, value: 'This is quiz 1'}],
            [{ colIndex: 1, value: 'This is quiz 1'}],
            [{ colIndex: 1, value: 'This is quiz 1'}],
            [{ colIndex: 1, value: 'This is quiz 1'}],
            [{ colIndex: 1, value: 'This is quiz 1'}],
            [{ colIndex: 1, value: 'This is quiz 2'}],
            [{ colIndex: 1, value: 'This is quiz 2'}],
            [{ colIndex: 1, value: 'This is quiz 2'}],
            [{ colIndex: 1, value: 'This is quiz 2'}],
            [{ colIndex: 1, value: 'This is quiz 2'}],
            [{ colIndex: 1, value: 'This is survey 3'}],
            [{ colIndex: 1, value: 'This is survey 3'}],
            [{ colIndex: 1, value: 'This is survey 3'}],
            [{ colIndex: 1, value: 'This is survey 3'}],
        ]
        cy.validateTable(tableSelector, expected, 10);

        cy.get('[data-pc-name="headercell"] [data-pc-section="columnheadercontent"]').contains('Name').click();
        const expectedReversed = [...expected].reverse()
        cy.validateTable(tableSelector, expectedReversed, 10);

        // refresh and re-validate sort
        cy.intercept('/app/quiz-runs**').as('quizRuns')
        cy.visit('/administrator/quiz-runs');
        cy.wait('@quizRuns')
        cy.get('[data-cy="quizRunsHistoryTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '14')

        cy.validateTable(tableSelector, expectedReversed, 10);
    });

    it('sort by type', function () {
        cy.intercept('/app/quiz-runs**').as('quizRuns')
        cy.visit('/administrator/quiz-runs');
        cy.wait('@quizRuns')
        cy.get('[data-cy="quizRunsHistoryTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '14')

        cy.get('[data-pc-name="headercell"] [data-pc-section="columnheadercontent"]').contains('Type').click();

        const expected = [
            [{ colIndex: 2, value: 'Quiz'}],
            [{ colIndex: 2, value: 'Quiz'}],
            [{ colIndex: 2, value: 'Quiz'}],
            [{ colIndex: 2, value: 'Quiz'}],
            [{ colIndex: 2, value: 'Quiz'}],
            [{ colIndex: 2, value: 'Quiz'}],
            [{ colIndex: 2, value: 'Quiz'}],
            [{ colIndex: 2, value: 'Quiz'}],
            [{ colIndex: 2, value: 'Quiz'}],
            [{ colIndex: 2, value: 'Quiz'}],
            [{ colIndex: 2, value: 'Survey'}],
            [{ colIndex: 2, value: 'Survey'}],
            [{ colIndex: 2, value: 'Survey'}],
            [{ colIndex: 2, value: 'Survey'}],
        ]
        cy.validateTable(tableSelector, expected, 10);

        cy.get('[data-pc-name="headercell"] [data-pc-section="columnheadercontent"]').contains('Type').click();
        const expectedReversed = [...expected].reverse()
        cy.validateTable(tableSelector, expectedReversed, 10);
    });

    it('sort by org', function () {
        cy.intercept('/app/quiz-runs**').as('quizRuns')
        cy.visit('/administrator/quiz-runs');
        cy.wait('@quizRuns')
        cy.get('[data-cy="quizRunsHistoryTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '14')

        cy.get('[data-pc-name="headercell"] [data-pc-section="columnheadercontent"]').contains('Org').click();

        const expected = [
            [{ colIndex: 3, value: 'ABC'}],
            [{ colIndex: 3, value: 'ABC'}],
            [{ colIndex: 3, value: 'ABC1'}],
            [{ colIndex: 3, value: 'XYZ'}],
            [{ colIndex: 3, value: 'XYZ'}],
            [{ colIndex: 3, value: 'XYZ'}],
            [{ colIndex: 3, value: 'XYZ'}],
            [{ colIndex: 3, value: 'XYZ1'}],
            [{ colIndex: 3, value: 'XYZ1'}],
            [{ colIndex: 3, value: 'XYZ1'}],
            [{ colIndex: 3, value: ''}],
            [{ colIndex: 3, value: ''}],
            [{ colIndex: 3, value: ''}],
            [{ colIndex: 3, value: ''}],
        ]
        cy.validateTable(tableSelector, expected, 10);

        cy.get('[data-pc-name="headercell"] [data-pc-section="columnheadercontent"]').contains('Org').click();
        const expectedReversed = [...expected].reverse()
        cy.validateTable(tableSelector, expectedReversed, 10);
    });

    it('sort by status', function () {
        cy.intercept('/app/quiz-runs**').as('quizRuns')
        cy.visit('/administrator/quiz-runs');
        cy.wait('@quizRuns')
        cy.get('[data-cy="quizRunsHistoryTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '14')

        cy.get('[data-pc-name="headercell"] [data-pc-section="columnheadercontent"]').contains('Status').click();

        const expected = [
            [{ colIndex: 4, value: 'Completed'}],
            [{ colIndex: 4, value: 'Completed'}],
            [{ colIndex: 4, value: 'Completed'}],
            [{ colIndex: 4, value: 'Failed'}],
            [{ colIndex: 4, value: 'Failed'}],
            [{ colIndex: 4, value: 'Failed'}],
            [{ colIndex: 4, value: 'Failed'}],
            [{ colIndex: 4, value: 'In Progress'}],
            [{ colIndex: 4, value: 'In Progress'}],
            [{ colIndex: 4, value: 'Passed'}],
            [{ colIndex: 4, value: 'Passed'}],
            [{ colIndex: 4, value: 'Passed'}],
            [{ colIndex: 4, value: 'Passed'}],
            [{ colIndex: 4, value: 'Passed'}],
        ]
        cy.validateTable(tableSelector, expected, 10);

        cy.get('[data-pc-name="headercell"] [data-pc-section="columnheadercontent"]').contains('Status').click();
        const expectedReversed = [...expected].reverse()
        cy.validateTable(tableSelector, expectedReversed, 10);
    });

    it('add optional fields', function () {
        cy.intercept('/app/quiz-runs**').as('quizRuns')
        cy.visit('/administrator/quiz-runs');
        cy.wait('@quizRuns')
        cy.get('[data-cy="quizRunsHistoryTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '14')

        const expectedHeaders = ['User', 'Name', 'Type', 'Org', 'Status', 'Started']
        cy.get(`${tableSelector} [data-pc-name="headercell"]`)
            .should('have.length', expectedHeaders.length)
            .each(($el, index) => {
                const actualText = $el.text().trim()
                const expectedText = expectedHeaders[index]
                expect(actualText, `Header at index ${index}`).to.equal(expectedText)
            })

        cy.get('[data-cy="quizRunTable-additionalColumns"] [data-pc-section="dropdown"]').click()

        cy.get('[data-pc-section="listcontainer"] [aria-label="Results"]').click()
        cy.get('[data-pc-section="listcontainer"] [aria-label="Runtime"]').click()
        cy.realPress('Escape');

        const expectedHeadersAfter = ['User', 'Name', 'Type', 'Org', 'Status',  'Runtime', 'Results', 'Started']
        cy.get(`${tableSelector} [data-pc-name="headercell"]`)
            .should('have.length', expectedHeadersAfter.length)
            .each(($el, index) => {
                const actualText = $el.text().trim()
                const expectedText = expectedHeadersAfter[index]
                expect(actualText, `Header at index ${index}`).to.equal(expectedText)
            })

        const expected = [
            [{ colIndex: 0, value: 'user5 (LastName, Firstname)'}, { colIndex: 6, value: 'N/A'}],
            [{ colIndex: 0, value: 'user3 (LastName, Firstname)'}, { colIndex: 6, value: '1 correct out of 1'}],
            [{ colIndex: 0, value: 'user1 (LastName, Firstname)'}, { colIndex: 6, value: '1 correct out of 1'}],
            [{ colIndex: 0, value: 'user4 (LastName, Firstname)'}, { colIndex: 6, value: '0 correct out of 1'}],
            [{ colIndex: 0, value: 'user3 (LastName, Firstname)'}, { colIndex: 6, value: '0 correct out of 1'}],
            [{ colIndex: 0, value: 'user4 (LastName, Firstname)'}, { colIndex: 6, value: '0 correct out of 1'}],
            [{ colIndex: 0, value: 'user2 (LastName, Firstname)'}, { colIndex: 6, value: 'N/A'}],
            [{ colIndex: 0, value: 'user5 (LastName, Firstname)'}, { colIndex: 6, value: '1 correct out of 1'}],
            [{ colIndex: 0, value: 'user3 (LastName, Firstname)'}, { colIndex: 6, value: 'N/A'}],
            [{ colIndex: 0, value: 'user5 (LastName, Firstname)'}, { colIndex: 6, value: '0 correct out of 1'}],
            [{ colIndex: 0, value: 'user1 (LastName, Firstname)'}, { colIndex: 6, value: '1 correct out of 1'}],
            [{ colIndex: 0, value: 'user4 (LastName, Firstname)'}, { colIndex: 6, value: 'N/A'}],
            [{ colIndex: 0, value: 'user3 (LastName, Firstname)'}, { colIndex: 6, value: '0 correct out of 1'}],
            [{ colIndex: 0, value: 'user5 (LastName, Firstname)'}, { colIndex: 6, value: '1 correct out of 1'}],
        ];

        cy.validateTable(tableSelector, expected, 10);
    });

    it('filter by user', function () {
        cy.intercept('/app/quiz-runs**').as('quizRuns')
        cy.visit('/administrator/quiz-runs');
        cy.wait('@quizRuns')
        cy.get('[data-cy="quizRunsHistoryTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '14')

        cy.get('[data-cy="userNameFilter"]').type('  UsEr3   {enter}')

        const expected = [
            [{ colIndex: 0, value: 'user3'}],
            [{ colIndex: 0, value: 'user3'}],
            [{ colIndex: 0, value: 'user3'}],
            [{ colIndex: 0, value: 'user3'}],
        ]
        cy.validateTable(tableSelector, expected, 10);

        cy.get('[data-cy="userResetBtn"]').click()
        cy.get('[data-cy="quizRunsHistoryTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '14')

        cy.get('[data-cy="userNameFilter"]').type('  @#@#&*(  ')
        cy.get('[data-cy="userFilterBtn"]').click()
        cy.get('[data-cy="quizRunsHistoryTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '0')
        cy.get('[data-cy="quizRunsHistoryTable"] [data-cy="tblFilterResetBtn"]').click()
        cy.get('[data-cy="quizRunsHistoryTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '14')

    });

    it('filter by name', function () {
        cy.intercept('/app/quiz-runs**').as('quizRuns')
        cy.visit('/administrator/quiz-runs');
        cy.wait('@quizRuns')
        cy.get('[data-cy="quizRunsHistoryTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '14')

        cy.get('[data-cy="quizNameFilter"]').type('  SuRv   {enter}')

        const expected = [
            [{ colIndex: 1, value: 'This is survey 3'}],
            [{ colIndex: 1, value: 'This is survey 3'}],
            [{ colIndex: 1, value: 'This is survey 3'}],
            [{ colIndex: 1, value: 'This is survey 3'}],
        ]
        cy.validateTable(tableSelector, expected, 10);

        cy.get('[data-cy="userResetBtn"]').click()
        cy.get('[data-cy="quizRunsHistoryTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '14')

        cy.get('[data-cy="quizNameFilter"]').type('  @#@#&*(  ')
        cy.get('[data-cy="userFilterBtn"]').click()
        cy.get('[data-cy="quizRunsHistoryTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '0')
        cy.get('[data-cy="quizRunsHistoryTable"] [data-cy="tblFilterResetBtn"]').click()
        cy.get('[data-cy="quizRunsHistoryTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '14')
    });

    it('filter by user and name', function () {
        cy.intercept('/app/quiz-runs**').as('quizRuns')
        cy.visit('/administrator/quiz-runs');
        cy.wait('@quizRuns')
        cy.get('[data-cy="quizRunsHistoryTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '14')

        cy.get('[data-cy="userNameFilter"]').type('  UsEr3')
        cy.get('[data-cy="quizNameFilter"]').type('  SuRv   {enter}')

        const expected = [
            [{ colIndex: 0, value: 'user3'}, { colIndex: 1, value: 'This is survey 3'}],
        ]
        cy.validateTable(tableSelector, expected, 10);
    });

    it('filter by date', function () {
        cy.intercept('/app/quiz-runs**').as('quizRuns')
        cy.visit('/administrator/quiz-runs');
        cy.wait('@quizRuns')
        cy.get('[data-cy="quizRunsHistoryTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '14')

        Cypress.Commands.add('setDay', (dayNum) => {
            const dayNumClean = Number(dayNum).toString()
            let re = new RegExp(String.raw`^${dayNumClean}$`)
            cy.get('[data-pc-section="panel"] [data-pc-section="calendar"] [data-pc-section="day"]').contains(re).click()
        });

        Cypress.Commands.add('filterSetYear', (yearNum) => {
            cy.get('[data-pc-section="panel"] [data-pc-section="calendar"] [data-pc-section="selectyear"]').click()
            cy.get('[data-pc-section="panel"] [data-pc-section="year"]').contains(yearNum).click()
        })
        Cypress.Commands.add('filterSetMonth', (month) => {
            cy.get('[data-pc-section="panel"] [data-pc-section="month"]').contains(month).click()
        })

        const threeDaysAgo = moment().subtract(3, 'days')
        cy.get('[data-cy="metricsDateFilter"]').click()
        cy.filterSetYear(threeDaysAgo.format('YYYY'))
        cy.filterSetMonth(threeDaysAgo.format('MMM'))
        cy.setDay(threeDaysAgo.format('D'))
        cy.get('[data-cy="userFilterBtn"]').click()

        const expected = [
            [{ colIndex: 0, value: 'user5'}, { colIndex: 1, value: 'This is survey 3'}],
            [{ colIndex: 0, value: 'user3'}, { colIndex: 1, value: 'This is quiz 1'}],
            [{ colIndex: 0, value: 'user1'}, { colIndex: 1, value: 'This is quiz 2'}],
        ]
        cy.validateTable(tableSelector, expected, 10);

        cy.get('[data-cy="userNameFilter"]').type('  UsEr3')
        cy.get('[data-cy="userFilterBtn"]').click()

        const expected1 = [
            [{ colIndex: 0, value: 'user3'}, { colIndex: 1, value: 'This is quiz 1'}],
        ]
        cy.validateTable(tableSelector, expected1, 10);
    });

    it('navigate to quiz run', function () {
        cy.intercept('/app/quiz-runs**').as('quizRuns')
        cy.visit('/administrator/quiz-runs');
        cy.wait('@quizRuns')
        cy.get('[data-cy="quizRunsHistoryTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '14')

        cy.get('[data-cy="quizRunsHistoryTable"] [data-cy="row1-viewRun"]').click()
        cy.get('[data-cy="title"]').contains('This is quiz 1')
        cy.get('[data-cy="subPageHeader"]').contains('User Run')
        cy.get('[data-cy="userInfoCard"]').contains('user3')
        cy.get('[data-cy="quizRunStatus"]').contains('Passed')
    });

    it('navigate to quiz def via name', function () {
        cy.intercept('/app/quiz-runs**').as('quizRuns')
        cy.visit('/administrator/quiz-runs');
        cy.wait('@quizRuns')
        cy.get('[data-cy="quizRunsHistoryTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '14')

        cy.get('[data-cy="quizRunsHistoryTable"] [data-cy="row1-quizPageLink"]').click()
        cy.get('[data-cy="title"]').contains('This is quiz 1')
        cy.get('[data-cy="subPageHeader"]').contains('Questions')
    });

    it('filter quizzes', function () {
        cy.intercept('/app/quiz-runs**').as('quizRuns')
        cy.visit('/administrator/quiz-runs');
        cy.wait('@quizRuns')
        cy.get('[data-cy="quizRunsHistoryTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '14')

        cy.get('[data-cy="overallMetricsCards"] [data-cy="numExcludedAssessments"]').should('not.exist')
        cy.get('[data-cy="overallMetricsQuizzesAndSurveysCard"] [data-cy="mediaInfoCardTitle"]').contains('3 Assessments')
        cy.get('[data-cy="overallMetricsQuizzesAndSurveysCard"] [data-cy="mediaInfoCardSubTitle"]').contains('2 Quizzes and 1 Survey')

        cy.get('[data-cy="confQuizExclusionBtn"]').click()

        cy.get('[data-pc-name="dialog"] [data-pc-section="title"]').contains('Configure Included Assessments')

        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="header"]').contains("Included Assessments")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="targetlistcontainer"] [data-pc-section="header"]').contains("Excluded Assessments")

        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="1"]').contains("This is survey 3")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="1"]').contains("(Survey)")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="2"]').contains("This is quiz 2")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="2"]').contains("(Quiz)")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="3"]').contains("This is quiz 1")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="3"]').contains("(Quiz)")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="4"]').should('not.exist')
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="targetlistcontainer"] [data-pc-section="option"][aria-posinset="1"]').should('not.exist')

        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="1"]').click()
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="2"]').click()
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-name="pcmovetotargetbutton"]').click()

        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="1"]').contains("This is quiz 1")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="2"]').should('not.exist')
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="targetlistcontainer"] [data-pc-section="option"][aria-posinset="1"]').contains("This is survey 3")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="targetlistcontainer"] [data-pc-section="option"][aria-posinset="2"]').contains("This is quiz 2")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="targetlistcontainer"] [data-pc-section="option"][aria-posinset="3"]').should('not.exist')

        cy.get('[data-pc-name="dialog"] [data-cy="saveDialogBtn"]').click()

        cy.validateTable(tableSelector, [
            [{ colIndex: 1, value: 'This is quiz 1'}],
            [{ colIndex: 1, value: 'This is quiz 1'}],
            [{ colIndex: 1, value: 'This is quiz 1'}],
            [{ colIndex: 1, value: 'This is quiz 1'}],
            [{ colIndex: 1, value: 'This is quiz 1'}],
        ], 10);
    });

    it('export global quiz runs', function () {
        cy.intercept('/app/quiz-runs**').as('quizRuns')
        cy.visit('/administrator/quiz-runs');
        cy.wait('@quizRuns')
        cy.get('[data-cy="quizRunsHistoryTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '14')
        cy.get('[data-cy="exportBtn"]').should('be.enabled')

        cy.get('[data-cy="userNameFilter"]').type('ThisUserDoesNotExist{enter}')
        cy.get('[data-cy="quizRunsHistoryTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '0')
        cy.get('[data-cy="exportBtn"]').should('be.disabled')

        cy.get('[data-cy="userResetBtn"]').click()
        cy.get('[data-cy="exportBtn"]').should('be.enabled')

        // export skill metrics and verify that the file exists
        const exportedFileName = `cypress/downloads/global-quiz-runs-${moment.utc().format('YYYY-MM-DD')}.xlsx`;
        cy.readFile(exportedFileName).should('not.exist');
        cy.get('[data-cy="exportBtn"]').click();
        cy.readFile(exportedFileName).should('exist');
    });
});
