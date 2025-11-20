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

const moment = require('moment-timezone');

describe('Accessibility Project Metrics Tests', () => {

    before(() => {
        cy.beforeTestSuiteThatReusesData()

        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSubject(1, 2)
        cy.createProject(2)

        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)
        cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' })
        cy.createSkill(1, 1, 4)

        cy.createBadge(1)
        cy.assignSkillToBadge(1, 1, 1);
        cy.enableBadge(1, 1);

        cy.addLearningPathItem(1, 1, 2)

        const m = moment('2020-05-12 11', 'YYYY-MM-DD HH');
        cy.reportSkill(1, 1, 'u1',  m);
        cy.reportSkill(1, 1, 'u2',  m.subtract(4, 'day'));
        cy.reportSkill(1, 1, 'u3',  m.subtract(3, 'day'));
        cy.reportSkill(1, 1, 'u4',  m.subtract(2, 'day'));
        cy.reportSkill(1, 1, 'u5',  m.subtract(1, 'day'));
        cy.reportSkill(1, 3, 'u5',  m.subtract(1, 'day'));
        cy.reportSkill(1, 3, 'u6',  m.subtract(1, 'day'));
        cy.reportSkill(1, 3, 'u7',  m.subtract(1, 'day'));
        cy.reportSkill(1, 4, 'u5',  m.subtract(1, 'day'));
        cy.reportSkill(1, 4, 'u8',  m.subtract(2, 'day'));
        cy.reportSkill(1, 4, 'u8',  m.subtract(3, 'day'));
        cy.reportSkill(1, 4, 'u8',  m.subtract(4, 'day'));
        cy.reportSkill(1, 4, 'u8',  m.subtract(5, 'day'));

        //report skills that dont' exist
        cy.reportSkill('proj1', 42, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill('proj1', 75, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill('proj1', 75, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill('proj1', 75, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill('proj1', 13, 'user@skills.org', '2021-02-24 10:00', false);

        cy.reuseSkillIntoAnotherSubject(1, 3, 2);
        cy.reuseSkillIntoAnotherSubject(1, 4, 2);
    });

    after(() => {
        cy.afterTestSuiteThatReusesData()
    });

    const runWithDarkMode = ['', ' - dark mode']

    runWithDarkMode.forEach((darkMode) => {
        it(`project - metrics${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.visit('/administrator/projects/proj1/metrics');
            cy.get('[data-cy="card-header"]').contains('Users per day');
            cy.get('[data-cy="distinctNumUsersOverTime"]').contains('This chart needs at least 2 days of user activity.');
            cy.get('[data-cy="projectLastReportedSkillValue"]')
            cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]').should('have.text', '4')

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y()
        });

        it(`project - achievements metrics${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)

            cy.intercept('**numUsersPerLevelChartBuilder**').as('numUsersPerLevelChartBuilder')
            cy.visit('/administrator/projects/proj1/metrics/achievements');
            cy.wait('@numUsersPerLevelChartBuilder')

            const headerSelector = `[data-cy="achievementsNavigator"] thead tr th`;
            cy.get(headerSelector)
              .contains('Username')
              .click();
            cy.get('[data-cy=achievementsNavigator-table]')
              .contains('u1');
            cy.wait(2000); // wait for charts to finish loading
            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y()
        })

        it(`project - subject metrics${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.visit('/administrator/projects/proj1/metrics/subjects');

            cy.get('[data-cy="Subjects-metrics-link"]')
              .click();
            cy.contains('Number of users for each level over time');
            cy.wait(4000); // wait for charts to finish loading

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y()

        });

        it(`project - skills metrics${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.visit('/administrator/projects/proj1/metrics/skills');
            cy.get('[data-cy="skillsNavigator-table"] [data-cy="skillsBTableTotalRows"]').should('have.text', '4')

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y()
        });
    })
});
