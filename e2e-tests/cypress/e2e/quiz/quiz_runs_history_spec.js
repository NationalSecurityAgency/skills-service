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

    it('survey status', function () {
        cy.createSurveyDef(1);
        cy.createSurveyMultipleChoiceQuestionDef(1, 1);
        cy.runQuizForUser(1, 1, [{selectedIndex: [1]}]);
        cy.runQuizForUser(1, 2, [{selectedIndex: [0]}], false);
        cy.visit('/administrator/quizzes/quiz1/runs');

        cy.validateTable(tableSelector, [
            [{ colIndex: 0, value: 'user2' }, { colIndex: 1, value: 'In Progress' }],
            [{ colIndex: 0, value: 'user1' }, { colIndex: 1, value: 'Completed' }],
        ], 10);
    });


    it('format total runtime and start time', function () {
        cy.intercept('GET', '/admin/quiz-definitions/quiz1/runs**', (req) => {
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
        cy.visit('/administrator/quizzes/quiz1/runs');
        cy.wait('@quizRuns')

        cy.validateTable(tableSelector, [
            [{ colIndex: 2, value: '1 hour and 7 minutes' }, { colIndex: 3, value: '2023-02-15 21:45' }],
        ], 10);
    });





});
