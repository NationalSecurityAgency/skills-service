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
describe('My Usage Tests', () => {

    before(() => {
        Cypress.env('disableResetDb', true);
        cy.resetDb();

        cy.fixture('vars.json')
            .then((vars) => {
                cy.logout();

                if (!Cypress.env('oauthMode')) {
                    cy.log('NOT in oauthMode, using form login');
                    cy.login(vars.defaultUser, vars.defaultPass);
                } else {
                    cy.log('oauthMode, using loginBySingleSignOn');
                    cy.loginBySingleSignOn();
                }
            });

        for (let i = 9; i >= 2; i -= 1) {
            cy.createProject(i);
            cy.enableProdMode(i);
            cy.addToMyProjects(i);
        }

    });
    after(() => {
        Cypress.env('disableResetDb', false);
    });

    it('point history chart - 4 projects are loaded by default', () => {
        cy.visit('/progress-and-rankings');
        cy.get('[data-cy="viewUsageBtn"]')
            .click();

        // validate 4 projects are loaded by default
        cy.get('[data-cy=eventHistoryChartProjectSelector] [data-pc-name="pcchip"]')
            .contains('project 2')
            .should('be.visible');
        cy.get('[data-cy=eventHistoryChartProjectSelector] [data-pc-name="pcchip"]')
            .contains('project 3')
            .should('be.visible');
        cy.get('[data-cy=eventHistoryChartProjectSelector] [data-pc-name="pcchip"]')
            .contains('project 4')
            .should('be.visible');
        cy.get('[data-cy=eventHistoryChartProjectSelector] [data-pc-name="pcchip"]')
            .contains('project 5')
            .should('be.visible');
        cy.get('[data-cy=eventHistoryChartProjectSelector] [data-pc-section="dropdownicon"]')
            .click();
        cy.get('[data-pc-section="overlay"] [data-pc-section="option"]').contains('project 6')
        cy.get('[data-pc-section="overlay"] [data-pc-section="option"]').contains('project 7')
    });

    it('point history chart - remove project', () => {
        cy.visit('/progress-and-rankings');
        cy.get('[data-cy="viewUsageBtn"]') .click();

        cy.get('[data-cy=eventHistoryChartProjectSelector] [data-pc-name="pcchip"]')
          .should('have.length', 4)
          .as('selected');
        cy.get('[data-cy=eventHistoryChartProjectSelector] [data-pc-name="pcchip"] [data-pc-section="removeicon"]')
            .should('have.length', 4)
            .as('removeBtns');
        cy.get('@removeBtns')
            .eq(2)
            .click();
        cy.get('[data-cy=eventHistoryChartProjectSelector] [data-pc-name="pcchip"]')
            .contains('project 2')
            .should('be.visible');
        cy.get('[data-cy=eventHistoryChartProjectSelector] [data-pc-name="pcchip"]')
            .contains('project 3')
            .should('be.visible');
        cy.get('[data-cy=eventHistoryChartProjectSelector] [data-pc-name="pcchip"]')
            .contains('project 5')
            .should('be.visible');

        cy.get('[data-cy=eventHistoryChartProjectSelector] [data-pc-section="dropdownicon"]')
            .click();
    });

    it('point history chart - add project', () => {
        cy.visit('/progress-and-rankings/my-usage');

        cy.get('[data-cy=eventHistoryChartProjectSelector] [data-pc-section="dropdownicon"]')
            .click();
        // cy.get('.p-multiselect-item').should('contain.text', 'project 6')
        cy.get('[data-pc-section="overlay"] [data-pc-section="list"] [data-pc-section="option"][aria-label="This is project 6"]')
            .as('project6');
        cy.get('@project6')
            .click();
        cy.get('[data-cy=eventHistoryChartProjectSelector] [data-pc-extend="chip"]')
            .contains('project 2')
            .should('be.visible');
        cy.get('[data-cy=eventHistoryChartProjectSelector] [data-pc-extend="chip"]')
            .contains('project 3')
            .should('be.visible');
        cy.get('[data-cy=eventHistoryChartProjectSelector] [data-pc-extend="chip"]')
            .contains('project 4')
            .should('be.visible');
        cy.get('[data-cy=eventHistoryChartProjectSelector] [data-pc-extend="chip"]')
            .contains('project 5')
            .should('be.visible');
        cy.get('[data-cy=eventHistoryChartProjectSelector] [data-pc-extend="chip"]')
            .contains('project 6')
            .should('be.visible');

    });

    it('point history chart - time selector keyboard navigation', () => {
        cy.visit('/progress-and-rankings/my-usage');

        cy.intercept('GET', '/api/metrics/allProjectsSkillEventsOverTimeMetricsBuilder*')
            .as('loadMetrics');
        cy.get('.time-length-selector > span')
            .eq(1)
            .type('{enter}');
        cy.wait('@loadMetrics');
    });

    it('point history chart - only up to 5 projects can be selected', () => {
        cy.visit('/progress-and-rankings/my-usage');

        cy.get('[data-cy=eventHistoryChartProjectSelector] [data-pc-section="dropdownicon"]')
            .click();

        cy.get('[data-pc-section="overlay"] [data-pc-section="list"] [data-pc-section="option"][aria-label="This is project 6"]').click()
        for (let i = 2; i <= 6; i++) {
            cy.get('[data-cy=eventHistoryChartProjectSelector] [data-pc-name="pcchip"]')
                .contains('project 6')
                .should('be.visible');
        }
        for (let i = 7; i <= 9; i++) {
            cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [data-pc-section="option"][aria-label="This is project ${i}"].p-disabled`)
        }
    });

    it('point history chart - all projects removed', () => {
        cy.visit('/progress-and-rankings/my-usage');

        for (let i = 2; i <= 5; i++) {
            cy.get(`[data-cy="eventHistoryChartProjectSelector"] [data-pc-name="pcchip"][aria-label="This is project ${i}"] [data-pc-section="removeicon"]`).click()
        }

        cy.get('[data-cy="eventHistoryChartProjectSelector"] [data-pc-section="label"]').contains('Select projects')
        cy.get('[data-cy=eventHistoryChart]')
            .contains('Please select at least one project from the list above.');

    });

    it('point history chart - time controls call out to the server', () => {

        cy.intercept('GET', '/api/metrics/allProjectsSkillEventsOverTimeMetricsBuilder*')
            .as('pointHistoryChart');

        cy.visit('/progress-and-rankings/my-usage');

        cy.get('[data-cy=eventHistoryChart] [data-cy=timeLengthSelector]')
            .contains('6 months')
            .click();
        cy.wait('@pointHistoryChart');
        cy.get('[data-cy=eventHistoryChart] [data-cy=timeLengthSelector]')
            .contains('1 year')
            .click();
        cy.wait('@pointHistoryChart');
    });
});