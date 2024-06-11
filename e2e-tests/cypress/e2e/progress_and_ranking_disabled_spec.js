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
describe('Project and Ranking Views are disabled Tests', () => {

    it('Home page preference is not shown', () => {
        cy.visit('/settings/preferences');
        cy.get('[data-cy="defaultHomePageSetting"]');
        cy.get('[data-cy="rankOptOut"]');

        cy.intercept('GET', '/public/config', (req) => {
            req.reply({
                body: {
                    rankingAndProgressViewsEnabled: 'false',
                },
            });
        })
            .as('getConfig');

        cy.visit('/settings/preferences');
        cy.wait('@getConfig');
        cy.get('[data-cy="rankOptOut"]');
        cy.get('[data-cy="defaultHomePageSetting"]')
            .should('not.exist');
    });

    it('Admin and Progress and Ranking navigation is NOT shown', () => {
        cy.visit('/administrator');
        cy.get('[data-cy="settings-button"]')
            .click();
        cy.get('[aria-label="Progress and Ranking"]')
            .should('exist');
        cy.get('[aria-label="Project Admin"]')
            .should('exist');

        cy.intercept('GET', '/public/config', (req) => {
            req.reply({
                body: {
                    rankingAndProgressViewsEnabled: 'false',
                },
            });
        })
            .as('getConfig');

        cy.visit('/administrator');
        cy.wait('@getConfig');

        cy.get('[data-cy="settings-button"]')
            .click();
        cy.get('[aria-label="Progress and Ranking"]')
          .should('not.exist');
        cy.get('[aria-label="Project Admin"]')
          .should('not.exist');
    });

    it.skip('Project level enable prod-mode setting must NOT be shown', () => {
        cy.createProject(1);

        const addToCatalogLabel = 'Add to the Project Catalog'

        cy.visit('/administrator/projects/proj1/settings');
        cy.contains('[data-cy=projectVisibilitySelector]', addToCatalogLabel)
            .should('exist');
        // cy.get('[ data-cy="productionModeSetting"]').should('exist');
        cy.intercept('GET', '/public/config', (req) => {
            req.reply({
                body: {
                    rankingAndProgressViewsEnabled: 'false',
                },
            });
        })
            .as('getConfig');

        cy.visit('/administrator/projects/proj1/settings');
        cy.wait('@getConfig');
        cy.contains('[data-cy=projectVisibilitySelector]', addToCatalogLabel)
            .should('not.exist');

    });

    it.skip('do not show Progress and Ranking in the breadcrumb when those views are disabled', function () {
        cy.createProject(1);
        cy.visit('/progress-and-rankings/projects/proj1/');
        cy.get('[data-cy=breadcrumb-item]')
            .its('length')
            .should('eq', 2);
        cy.get('[data-cy=breadcrumb-item]')
            .eq(0)
            .should('contain.text', 'Progress And Rankings');
        cy.get('[data-cy=breadcrumb-item]')
            .eq(1)
            .should('contain.text', 'Project: proj1');

        cy.intercept('GET', '/public/config', (req) => {
            req.reply({
                body: {
                    rankingAndProgressViewsEnabled: 'false',
                },
            });
        })
            .as('getConfig');
        cy.visit('/progress-and-rankings/projects/proj1/');
        cy.get('[data-cy=breadcrumb-item]')
            .its('length')
            .should('eq', 1);
        cy.get('[data-cy=breadcrumb-item]')
            .eq(0)
            .should('contain.text', 'Project: proj1');
    });

    it.skip('Provide clear instructions how to create a new project - root user', function () {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply({
                body: {
                    rankingAndProgressViewsEnabled: 'false',
                },
            });
        })
            .as('getConfig');

        cy.logout();
        cy.fixture('vars.json')
            .then((vars) => {
                cy.login(vars.rootUser, vars.defaultPass);
            });
        cy.visit('/administrator/');
        cy.contains('No Projects Yet...');
        cy.contains('A Project represents a gamified training profile that consists of skills divided into subjects');
        cy.get('[data-cy="firstNewProjectButton"]')
            .click();
        cy.get('[data-cy="projectName"]')
            .type('one');
        cy.get('[data-cy="saveProjectButton"]')
            .click();
        cy.get('[data-cy="projCard_one_manageBtn"]');
    });

    it.skip('Provide clear instructions how to create a new project - regular user', function () {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply({
                body: {
                    rankingAndProgressViewsEnabled: 'false',
                },
            });
        })
            .as('getConfig');

        cy.visit('/administrator/');
        cy.contains('No Projects Yet...');
        cy.contains('A Project represents a gamified training profile that consists of skills divided into subjects');
        cy.get('[data-cy="firstNewProjectButton"]')
            .click();
        cy.get('[data-cy="projectName"]')
            .type('one');
        cy.get('[data-cy="saveProjectButton"]')
            .click();
        cy.get('[data-cy="projCard_one_manageBtn"]');
    });
});

