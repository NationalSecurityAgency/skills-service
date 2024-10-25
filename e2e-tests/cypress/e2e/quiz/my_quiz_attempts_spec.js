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
import relativeTimePlugin from 'dayjs/plugin/relativeTime';
import advancedFormatPlugin from 'dayjs/plugin/advancedFormat';

dayjs.extend(relativeTimePlugin);
dayjs.extend(advancedFormatPlugin);

describe('Display History of My quiz attempts Tests', () => {

    let defaultUser
    beforeEach(() => {
        defaultUser = Cypress.env('oauthMode') ? 'foo': Cypress.env('proxyUser')
    })


    it('No Attempts', () => {
        cy.visit('/progress-and-rankings/my-quiz-attempts');
        cy.get('[data-cy="noQuizzesOrSurveys"]')
    })

    const tableSelector = '[data-cy="myQuizAttemptsTable"]'

    it('one quiz', () => {
        cy.createQuizDef(1);
        cy.createTextInputQuestionDef(1, 1)
        cy.runQuizForUser(1, defaultUser, [{selectedIndex: [0]}], true, 'My Answer')

        cy.visit('/progress-and-rankings/my-quiz-attempts');
        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 'This is quiz 1'
            }, {
                colIndex: 1,
                value: 'Quiz'
            }, {
                colIndex: 2,
                value: 'Needs Grading'
            }],
        ], 5);

    });

    it('paging with quizzes and surveys', () => {
        cy.createQuizDef(1);
        cy.createTextInputQuestionDef(1, 1)
        cy.runQuizForUser(1, defaultUser, [{selectedIndex: [0]}], true, 'My Answer')

        for (let i = 0; i < 12; i++) {
            const quizNum = i + 2
            if (i % 3 === 0) {
                cy.createSurveyDef(quizNum);
                cy.createSurveyMultipleChoiceQuestionDef(quizNum, 1, { questionType: 'SingleChoice' });
            } else {
                cy.createQuizDef(quizNum);
                cy.createQuizQuestionDef(quizNum, 1)
            }

            cy.runQuizForUser(quizNum, defaultUser, [{selectedIndex: [i%2===0?0:1]}], true, 'My Answer')
        }

        cy.visit('/progress-and-rankings/my-quiz-attempts');

        const expectedRows = [
            [{ colIndex: 0, value: 'This is quiz 13'}, { colIndex: 1, value: 'Quiz'}, { colIndex: 2, value: 'Failed'}],
            [{ colIndex: 0, value: 'This is quiz 12'}, { colIndex: 1, value: 'Quiz'}, { colIndex: 2, value: 'Passed'}],
            [{ colIndex: 0, value: 'This is survey 11'}, { colIndex: 1, value: 'Survey'}, { colIndex: 2, value: 'Completed'}],
            [{ colIndex: 0, value: 'This is quiz 10'}, { colIndex: 1, value: 'Quiz'}, { colIndex: 2, value: 'Passed'}],
            [{ colIndex: 0, value: 'This is quiz 9'}, { colIndex: 1, value: 'Quiz'}, { colIndex: 2, value: 'Failed'}],
            [{ colIndex: 0, value: 'This is survey 8'}, { colIndex: 1, value: 'Survey'}, { colIndex: 2, value: 'Completed'}],
            [{ colIndex: 0, value: 'This is quiz 7'}, { colIndex: 1, value: 'Quiz'}, { colIndex: 2, value: 'Failed'}],
            [{ colIndex: 0, value: 'This is quiz 6'}, { colIndex: 1, value: 'Quiz'}, { colIndex: 2, value: 'Passed'}],
            [{ colIndex: 0, value: 'This is survey 5'}, { colIndex: 1, value: 'Survey'}, { colIndex: 2, value: 'Completed'}],
            [{ colIndex: 0, value: 'This is quiz 4'}, { colIndex: 1, value: 'Quiz'}, { colIndex: 2, value: 'Passed'}],
            [{ colIndex: 0, value: 'This is quiz 3'}, { colIndex: 1, value: 'Quiz'}, { colIndex: 2, value: 'Failed'}],
            [{ colIndex: 0, value: 'This is survey 2'}, { colIndex: 1, value: 'Survey'}, { colIndex: 2, value: 'Completed'}],
            [{ colIndex: 0, value: 'This is quiz 1'}, { colIndex: 1, value: 'Quiz'}, { colIndex: 2, value: 'Needs Grading'}],
        ]
        cy.validateTable(tableSelector, expectedRows, 10);

        cy.get(`${tableSelector} [data-pc-name="rowperpagedropdown"]`).click();
        cy.get('[data-pc-section="list"] [aria-label="20"]').click()
        cy.validateTable(tableSelector, expectedRows, 20);

    });


});


