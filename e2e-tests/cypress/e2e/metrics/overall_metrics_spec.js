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
var moment = require('moment-timezone');

describe('Global/Overall Metrics', () => {

    const userTagsTableSelector = '[data-cy="userTagsTable-dutyOrganization"]';

    before(() => {
        cy.beforeTestSuiteThatReusesData()

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
    });

    after(() => {
        cy.afterTestSuiteThatReusesData()
    });

    it('user progress page', () => {
        cy.intercept('/app/overall-metrics/overallDistinctUsersOverTimeMetricsBuilder**').as('overallDistinctUsersOverTime');
        cy.visit('/administrator/overall-metrics');
        cy.wait('@overallDistinctUsersOverTime');

        cy.get('[data-cy="overallMetricsProjectsCard"]').should('be.visible');
        cy.get('[data-cy="overallMetricsQuizzesAndSurveysCard"]').should('be.visible');
        cy.get('[data-cy="overallMetricsBadgesCard"]').should('be.visible');

        // verify that the cards have the correct content
        cy.get('[data-cy="overallMetricsProjectsCard"]').should('contain', '3 Projects');
        cy.get('[data-cy="overallMetricsProjectsCard"]').should('contain', '15 Skills');

        cy.get('[data-cy="overallMetricsQuizzesAndSurveysCard"]').should('contain', '1 Quiz');
        cy.get('[data-cy="overallMetricsQuizzesAndSurveysCard"]').should('contain', '0 Surveys');

        cy.get('[data-cy="overallMetricsBadgesCard"]').should('contain', '0 Badges');
        cy.get('[data-cy="overallMetricsBadgesCard"]').should('contain', '0 Global Badges');

        cy.get('[data-cy="distinctNumUsersOverTime"]').should('contain.text', 'Overall Users per day')
        cy.get('[data-cy="distinctNumUsersOverTime"] [data-cy="timeLengthSelector"]')
          .contains('6 months')
          .click();
        cy.wait('@overallDistinctUsersOverTime');

        cy.get('[data-cy="distinctNumUsersOverTime"] [data-cy="timeLengthSelector"]')
          .contains('1 year')
          .click();
        cy.wait('@overallDistinctUsersOverTime');

        cy.get('[data-cy="userTagTableCard"]').should('contain.text', 'Users by Org')
        cy.get('[data-cy="userTagTableCard"] [data-pc-section="header"]')
          .contains('Org');

        const expected = [
            [{
                colIndex: 0,
                value: 'ABC'
            }],[{
                colIndex: 0,
                value: 'ABC1'
            }],
        ];
        cy.validateTable(userTagsTableSelector, expected, 10)
        cy.get('[data-cy="userTagTable-dutyOrganization-tagFilter"]')
          .type('ABC1');
        cy.get('[ data-cy="userTagTable-dutyOrganization-filterBtn"]')
          .click();

        const expected2 = [
            [{
                colIndex: 0,
                value: 'ABC1'
            }],
        ];
        cy.validateTable(userTagsTableSelector, expected2, 10)
        cy.get('[data-cy="userTagTable-dutyOrganization-clearBtn"]').click();
        cy.validateTable(userTagsTableSelector, expected, 10)

        cy.get('[data-cy="userTagChart"] [data-pc-section="header"]')
          .contains('Users by Agency');
    });

});
