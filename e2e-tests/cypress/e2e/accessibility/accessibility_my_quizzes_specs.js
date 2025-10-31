/*
 * Copyright 2024 SkillTree
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

const moment = require('moment-timezone');

describe('My Quizzes Accessibility Tests', () => {
    const tableSelector = '[data-cy="myQuizAttemptsTable"]'
    let defaultUser
    let defaultUserDisplay
    beforeEach(() => {
        defaultUser = Cypress.env('proxyUser')
        defaultUserDisplay = Cypress.env('oauthMode') ? 'foo' : Cypress.env('proxyUser')
    })

    const runWithDarkMode = ['', ' - dark mode']

    runWithDarkMode.forEach((darkMode) => {
        it(`single quiz${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.createQuizDef(1);
            cy.createQuizQuestionDef(1, 1)
            cy.createQuizMultipleChoiceQuestionDef(1, 2);
            cy.createTextInputQuestionDef(1, 3)
            cy.createQuizQuestionDef(1, 4)
            cy.createQuizMatchingQuestionDef(1, 5)
            cy.setMinNumQuestionsToPass(1, 1)
            cy.runQuizForUser(1, defaultUser, [
                {selectedIndex: [1]},
                {selectedIndex: [1, 2]},
                {selectedIndex: [0]},
                {selectedIndex: [0]},
                {selectedIndex: [2, 1, 0]}
            ], true, 'My Answer')
            cy.gradeQuizAttempt(1, false, 'Wrong answer', true)

            cy.visit('/progress-and-rankings/my-quiz-attempts');
            cy.get(`${tableSelector} [data-p-index="0"] [data-cy="viewQuizAttempt"]`).first().click()
            cy.get('[data-cy="quizName"]').should('have.text', 'This is quiz 1')
            cy.get('[data-cy="quizRunStatus"]').contains('Passed')
            cy.get('[data-cy="numQuestionsToPass"]').contains('1 / 5')
            cy.get('[data-cy="numQuestionsToPass"]').contains('Need 1 question to pass')

            cy.get('[data-cy="questionDisplayCard-1"] [data-cy="questionDisplayText"]').contains('This is a question # 1')
            cy.get('[data-cy="questionDisplayCard-2"] [data-cy="questionDisplayText"]').contains('This is a question # 2')
            cy.get('[data-cy="questionDisplayCard-4"] [data-cy="questionDisplayText"]').contains('This is a question # 4')

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y();
        });

        it(`paging with quizzes and surveys${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.createQuizDef(1);
            cy.createTextInputQuestionDef(1, 1)
            cy.runQuizForUser(1, defaultUser, [{selectedIndex: [0]}], true, 'My Answer')

            for (let i = 0; i < 6; i++) {
                const quizNum = i + 2
                if (i % 3 === 0) {
                    cy.createSurveyDef(quizNum);
                    cy.createSurveyMultipleChoiceQuestionDef(quizNum, 1, {questionType: 'SingleChoice'});
                } else {
                    cy.createQuizDef(quizNum);
                    cy.createQuizQuestionDef(quizNum, 1)
                }

                cy.runQuizForUser(quizNum, defaultUser, [{selectedIndex: [i % 2 === 0 ? 0 : 1]}], true, 'My Answer')
            }

            cy.visit('/progress-and-rankings/my-quiz-attempts');

            const expectedRows = [
                [{colIndex: 0, value: 'This is quiz 7'}, {colIndex: 1, value: 'Quiz'}, {colIndex: 2, value: 'Failed'}],
                [{colIndex: 0, value: 'This is quiz 6'}, {colIndex: 1, value: 'Quiz'}, {colIndex: 2, value: 'Passed'}],
                [{colIndex: 0, value: 'This is survey 5'}, {colIndex: 1, value: 'Survey'}, {
                    colIndex: 2,
                    value: 'Completed'
                }],
                [{colIndex: 0, value: 'This is quiz 4'}, {colIndex: 1, value: 'Quiz'}, {colIndex: 2, value: 'Passed'}],
                [{colIndex: 0, value: 'This is quiz 3'}, {colIndex: 1, value: 'Quiz'}, {colIndex: 2, value: 'Failed'}],
                [{colIndex: 0, value: 'This is survey 2'}, {colIndex: 1, value: 'Survey'}, {
                    colIndex: 2,
                    value: 'Completed'
                }],
                [{colIndex: 0, value: 'This is quiz 1'}, {colIndex: 1, value: 'Quiz'}, {
                    colIndex: 2,
                    value: 'Needs Grading'
                }],
            ]
            cy.validateTable(tableSelector, expectedRows, 10);

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y();
        })

        it(`display quiz results on completed skill${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.createQuizDef(1, { name: 'Trivia Knowledge' });
            cy.createQuizQuestionDef(1, 1)
            cy.createQuizMultipleChoiceQuestionDef(1, 2);
            cy.createTextInputQuestionDef(1, 3)
            cy.runQuizForUser(1, defaultUser, [{selectedIndex: [0]}, {selectedIndex: [0, 2]}, {selectedIndex: [0]}], true, 'My Answer')
            cy.gradeQuizAttempt(1, true)

            cy.createProject(1)
            cy.createSubject(1,1)
            cy.createSkill(1, 1, 1,
                {
                    selfReportingType: 'Quiz',
                    quizId: 'quiz1',
                    pointIncrement: '150',
                    numPerformToCompletion: 1,
                });

            cy.cdVisit('/subjects/subj1/skills/skill1');
            cy.get('[data-cy="quizCompletedMsg"]').contains('Congratulations! You have passed Trivia Knowledge Quiz')
            cy.get('[data-cy="viewQuizAttemptInfo"]').should('be.enabled').click()
            cy.get('[data-cy="questionDisplayCard-1"] [data-cy="questionDisplayText"]').contains('This is a question # 1')
            cy.get('[data-cy="questionDisplayCard-2"] [data-cy="questionDisplayText"]').contains('This is a question # 2')
            cy.get('[data-cy="questionDisplayCard-3"] [data-cy="questionDisplayText"]').contains('This is a question # 3')

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y();
        })

        it(`display survey results on completed skill${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            const quizNum = 1
            cy.createSurveyDef(quizNum);
            cy.createSurveyMultipleChoiceQuestionDef(quizNum, 1, { questionType: 'SingleChoice' });
            cy.createSurveyMultipleChoiceQuestionDef(quizNum, 2);
            cy.createTextInputQuestionDef(quizNum, 3)
            cy.createRatingQuestionDef(quizNum, 4)
            cy.runQuizForUser(1, defaultUser, [
                {selectedIndex: [0]},
                {selectedIndex: [0, 2]},
                {selectedIndex: [0]},
                {selectedIndex: [0]},
            ], true, 'My Answer')

            cy.createProject(1)
            cy.createSubject(1,1)
            cy.createSkill(1, 1, 1,
                {
                    selfReportingType: 'Quiz',
                    quizId: 'quiz1',
                    pointIncrement: '150',
                    numPerformToCompletion: 1,
                });

            cy.cdVisit('/subjects/subj1/skills/skill1');
            cy.get('[data-cy="viewQuizAttemptInfo"]').should('be.enabled').click()
            cy.get('[data-cy="questionDisplayCard-1"] [data-cy="questionDisplayText"]').contains('This is a question # 1')
            cy.get('[data-cy="questionDisplayCard-2"] [data-cy="questionDisplayText"]').contains('This is a question # 2')
            cy.get('[data-cy="questionDisplayCard-3"] [data-cy="questionDisplayText"]').contains('This is a question # 3')
            cy.get('[data-cy="questionDisplayCard-4"] [data-cy="questionDisplayText"]').contains('This is a question # 4')

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y();
        })

    })

});
