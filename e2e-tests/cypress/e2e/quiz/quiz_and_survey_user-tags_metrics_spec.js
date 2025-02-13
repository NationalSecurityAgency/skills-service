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

describe('Quiz and Survey User Tag Runs and Metrics', () => {

    beforeEach(() => {

    });

    it('display user tag for quiz runs', function () {
        cy.addUserTag([{
            tagKey: 'dutyOrganization',
            tags: ['ABC']
        }, {
            tagKey: 'dutyOrganization',
            tags: ['ABC1']
        }]);

        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1)
        cy.createQuizMultipleChoiceQuestionDef(1, 2);
        cy.runQuizForUser(1, 1, [{selectedIndex: [0]}, {selectedIndex: [0,2]}]);
        cy.runQuizForUser(1, 2, [{selectedIndex: [0]}, {selectedIndex: [0,2]}]);
        cy.runQuizForUser(1, 3, [{selectedIndex: [0]}, {selectedIndex: [0,2]}]);

        cy.visit('/administrator/quizzes/quiz1/runs');
        const tableSelector = '[data-cy="quizRunsHistoryTable"]'
        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`).should('have.text', '3')
        const headerSelector = `${tableSelector} thead tr th`;
        cy.get(headerSelector)
            .contains('User')
            .click();
        cy.get('[data-cy="row0-userTag"]').should('have.text', 'ABC')
        cy.get('[data-cy="row1-userTag"]').should('have.text', 'ABC1')
        cy.get('[data-cy="row2-userTag"]').should('not.have.text')

        // test single run page
        cy.get('[data-cy="row1-viewRun"]').click()
        cy.get('[data-cy="userInfoCard"]').contains('Org: ABC1')

        // test single run without user tag
        cy.get('[data-cy="quizRunBackBtn"]').click()
        cy.get('[data-cy="row2-viewRun"]').click()
        cy.get('[data-cy="userInfoCard"]').contains('Org:').should('not.exist')
    });


    it('display user tag in answer history table', function () {
        cy.addUserTag([{
            tagKey: 'dutyOrganization',
            tags: ['ABC']
        }, {
            tagKey: 'dutyOrganization',
            tags: ['ABC1']
        }]);

        cy.createSurveyDef(2);
        cy.createSurveyMultipleChoiceQuestionDef(2, 1);
        cy.createTextInputQuestionDef(2, 2, { question: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum' });
        cy.createSurveyMultipleChoiceQuestionDef(2, 3, { questionType: 'SingleChoice' });

        cy.runQuizForUser(2, 1, [{selectedIndex: [0, 2]}, {selectedIndex: [0]}, {selectedIndex: [0]}], true, 'A1: Some very interesting text');
        cy.runQuizForUser(2, 2, [{selectedIndex: [0]}, {selectedIndex: [0]}, {selectedIndex: [2]}], true, 'A2: Second answer');
        cy.runQuizForUser(2, 3, [{selectedIndex: [2]}, {selectedIndex: [0]}, {selectedIndex: [1]}], true, 'A3: This is a short one');

        cy.visit('/administrator/quizzes/quiz2/results');
        // text input question
        const q2TableSelector = '[data-cy="metrics-q2"] [data-cy="quizAnswerHistoryTable"]';
        cy.get(`${q2TableSelector} [data-cy="skillsBTableTotalRows"]`).should('have.text', '3');
        cy.get(q2TableSelector).contains('ABC1');
        const q2HeaderSelector = `${q2TableSelector} thead tr th`;
        cy.get(q2HeaderSelector)
            .contains('User')
            .click();
        cy.get(`${q2TableSelector} [data-cy="row0-userTag"]`).should('have.text', 'ABC')
        cy.get(`${q2TableSelector} [data-cy="row1-userTag"]`).should('have.text', 'ABC1')
        cy.get(`${q2TableSelector} [data-cy="row2-userTag"]`).should('not.have.text')

        // multiple/single choice question by expanding history table
        cy.get('[data-cy="metrics-q1"] [data-p-index="0"] [data-pc-section="rowtogglebutton"]').click()
        const q1Answer1HistoryTable = '[data-cy="metrics-q1"] [data-cy="row0-answerHistory"] [data-cy="quizAnswerHistoryTable"]'
        cy.get(`${q1Answer1HistoryTable} [data-cy="row0-userTag"]`).should('have.text', 'ABC')
        cy.get(`${q1Answer1HistoryTable} [data-cy="row1-userTag"]`).should('have.text', 'ABC1')

        // multiple/single choice question by expanding history table
        cy.get('[data-cy="metrics-q1"] [data-p-index="2"] [data-pc-section="rowtogglebutton"]').click()
        const q1Answer3HistoryTable = '[data-cy="metrics-q1"] [data-cy="row2-answerHistory"] [data-cy="quizAnswerHistoryTable"]'
        cy.get(`${q1Answer3HistoryTable} [data-cy="row0-userTag"]`).should('have.text', 'ABC')
        cy.get(`${q1Answer3HistoryTable} [data-cy="row1-userTag"]`).should('not.have.text')

    });

    it('display user tag quiz chart', function () {
        cy.addUserTag([{
            tagKey: 'dutyOrganization',
            tags: ['ABC']
        }, {
            tagKey: 'dutyOrganization',
            tags: ['ABC']
        }, {
            tagKey: 'dutyOrganization',
            tags: ['ABC1']
        }]);

        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1)
        cy.createQuizMultipleChoiceQuestionDef(1, 2);
        cy.runQuizForUser(1, 1, [{selectedIndex: [0]}, {selectedIndex: [0,2]}]);
        cy.runQuizForUser(1, 2, [{selectedIndex: [0]}, {selectedIndex: [0,2]}]);
        cy.runQuizForUser(1, 3, [{selectedIndex: [0]}, {selectedIndex: [0,2]}]);

        cy.visit('/administrator/quizzes/quiz1/runs');
        cy.get('[data-cy="quizUserTagsChart"]').contains('ABC: 2')
        cy.get('[data-cy="quizUserTagsChart"]').contains('ABC1: 1')
    });

    it('display user tag quiz chart - empty', function () {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1)
        cy.createQuizMultipleChoiceQuestionDef(1, 2);
        cy.runQuizForUser(1, 1, [{selectedIndex: [0]}, {selectedIndex: [0,2]}]);

        cy.visit('/administrator/quizzes/quiz1/runs');
        cy.get('[data-cy="quizUserTagsChart"]').contains('No data yet...')
    });

    it('do not show user tag quiz chart when not configured', function () {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                delete conf.usersTableAdditionalUserTagKey
                delete conf.usersTableAdditionalUserTagLabel
                res.send(conf);
            });
        }).as('loadConfig');
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1)
        cy.createQuizMultipleChoiceQuestionDef(1, 2);
        cy.runQuizForUser(1, 1, [{selectedIndex: [0]}, {selectedIndex: [0,2]}]);

        cy.visit('/administrator/quizzes/quiz1/runs');
        cy.wait('@loadConfig')
        cy.get('[data-cy="quizRunsHistoryTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '1')
        cy.get('[data-cy="quizUserTagsChart"]').should('not.exist')
    });



});
