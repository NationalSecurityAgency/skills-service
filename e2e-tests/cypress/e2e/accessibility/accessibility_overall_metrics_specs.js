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

describe('Accessibility Overall Metrics Tests', () => {

    const runWithDarkMode = ['', ' - dark mode']

    runWithDarkMode.forEach((darkMode) => {

        it(`overall metrics page${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            
            const createProj = (projNum) => {
                cy.createProject(projNum)
                cy.createSubject(projNum, 1)
                const numSkills = 5;
                for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
                    cy.createSkill(projNum, 1, skillsCounter)
                }
            }
            createProj(1)
            createProj(2)
            createProj(3)

            const users = ['user1', 'user2']
            cy.reportSkill(1, 1, users[0])
            cy.reportSkill(1, 1, users[1], 'yesterday')
            cy.reportSkill(1, 1, users[1])

            cy.createQuizDef(1)
            cy.createQuizQuestionDef(1, 1)
            cy.runQuizForUser(1, users[0], [{selectedIndex: [0]}], true)

            cy.addUserTag([{
                tagKey: 'dutyOrganization',
                tags: ['ABC']
            }, {
                tagKey: 'dutyOrganization',
                tags: ['ABC1']
            }]);

            cy.addUserTag([{
                tagKey: 'adminOrganization',
                tags: ['XYZ']
            }, {
                tagKey: 'adminOrganization',
                tags: ['XYZ1']
            }]);

            cy.intercept('/app/overall-metrics/overallDistinctUsersOverTimeMetricsBuilder**').as('overallDistinctUsersOverTime');
            cy.visit('/administrator/overall-metrics');
            cy.wait('@overallDistinctUsersOverTime');

            // Wait for key elements to be visible
            cy.get('[data-cy="overallMetricsProjectsCard"]').should('be.visible');
            cy.get('[data-cy="overallMetricsQuizzesAndSurveysCard"]').should('be.visible');
            cy.get('[data-cy="overallMetricsBadgesCard"]').should('be.visible');
            cy.get('[data-cy="distinctNumUsersOverTime"]').should('be.visible');

            // Interact with time selector to ensure dynamic content is loaded
            cy.get('[data-cy="distinctNumUsersOverTime"] [data-cy="timeLengthSelector"]')
              .contains('6 months')
              .click();
            cy.wait('@overallDistinctUsersOverTime');

            cy.get('[data-cy="distinctNumUsersOverTime"] [data-cy="timeLengthSelector"]')
              .contains('1 year')
              .click();
            cy.wait('@overallDistinctUsersOverTime');

            // Wait for user tag table to be visible
            cy.get('[data-cy="userTagTableCard"]').should('be.visible');
            cy.get('[data-cy="userTagChart"]').should('be.visible');

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y()
        });

    });
});
