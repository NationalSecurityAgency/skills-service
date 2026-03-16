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

const moment = require('moment-timezone');

describe('Accessibility Users Global Progress Tests', () => {

    const runWithDarkMode = ['', ' - dark mode']

    runWithDarkMode.forEach((darkMode) => {

        it(`users overall progress${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.createProject(1)
            cy.createSubject(1, 1)
            cy.createSkill(1, 1, 1)
            cy.createSkill(1, 1, 2)
            cy.createProject(2)
            cy.createQuizDef(1)
            cy.createQuizQuestionDef(1, 1)

            cy.reportSkill(1, 1, 'user1')
            cy.reportSkill(1, 2, 'user1')
            cy.reportSkill(1, 1, 'user2')
            cy.runQuizForUser(1, 'user1', [{selectedIndex: [1]}], true)
            cy.runQuizForUser(1, 'user1', [{selectedIndex: [0]}], true)
            cy.runQuizForUser(1, 'user2', [{selectedIndex: [0]}], true)

            cy.visit('/administrator/users-progress');
            cy.intercept('/app/progress-metrics**').as('progressMetrics')
            cy.wait('@progressMetrics')

            cy.get('[data-cy="userOverallProgressTable"] [data-p-index="0"] [data-pc-group-section="rowactionbutton"]').click()
            cy.get('[data-cy="usrOverallProgress-user1"]').should('be.visible')

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y()
        });

        it(`global quiz runs${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)


            cy.createQuizDef(1)
            cy.createQuizQuestionDef(1, 1)

            cy.createQuizDef(2)
            cy.createQuizQuestionDef(2, 1)

            cy.runQuizForUser(1, 'user1', [{selectedIndex: [1]}], true)
            cy.runQuizForUser(1, 'user1', [{selectedIndex: [0]}], true)
            cy.runQuizForUser(1, 'user2', [{selectedIndex: [0]}], true)

            cy.runQuizForUser(2, 'user1', [{selectedIndex: [1]}], true)
            cy.runQuizForUser(2, 'user1', [{selectedIndex: [0]}], true)
            cy.runQuizForUser(2, 'user2', [{selectedIndex: [0]}], false)

            cy.intercept('/app/quiz-runs**').as('quizRuns')
            cy.visit('/administrator/quiz-runs');
            cy.wait('@quizRuns')
            cy.get('[data-cy="quizRunsHistoryTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '6')

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y()
        });

    })
});
