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

    it('Project level enable prod-mode setting must NOT be shown', () => {
        cy.createProject(1);

        const addToCatalogLabel = '[aria-label="Add to the Project Catalog"]'
        const notInCatalogLabel = '[aria-label="Not in the Project Catalog"]'
        const notEnabledLabel = '[aria-label="Not Enabled"]'

        cy.visit('/administrator/projects/proj1/settings');
        cy.get('[data-cy="projectVisibilitySelector"]').click()
        cy.get(`[data-pc-section="overlay"] ${addToCatalogLabel}`).should('exist')
        cy.get(`[data-pc-section="overlay"] ${notInCatalogLabel}`).should('exist')
        cy.get(`[data-pc-section="overlay"] ${notEnabledLabel}`).should('not.exist')
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
        cy.get('[data-cy="projectVisibilitySelector"]').click()
        cy.get(`[data-pc-section="overlay"] ${addToCatalogLabel}`).should('not.exist')
        cy.get(`[data-pc-section="overlay"] ${notInCatalogLabel}`).should('not.exist')
        cy.get(`[data-pc-section="overlay"] ${notEnabledLabel}`).should('exist')
    });

    it('do not show Progress and Ranking in the breadcrumb when those views are disabled', function () {
        cy.createProject(1);
        cy.visit('/progress-and-rankings/projects/proj1/');
        cy.get('[data-cy="breadcrumbItemValue"]')
            .its('length')
            .should('eq', 2);
        cy.get('[data-cy="breadcrumbItemValue"]')
            .eq(0)
            .should('contain.text', 'Progress And Rankings');
        cy.get('[data-cy="breadcrumbItemValue"]')
            .eq(1)
            .should('contain.text', 'proj1');

        cy.intercept('GET', '/public/config', (req) => {
            req.reply({
                body: {
                    rankingAndProgressViewsEnabled: 'false',
                },
            });
        })
            .as('getConfig');
        cy.visit('/progress-and-rankings/projects/proj1/');
        cy.get('[data-cy="breadcrumbItemValue"]')
            .its('length')
            .should('eq', 1);
        cy.get('[data-cy="breadcrumbItemValue"]')
            .eq(0)
            .should('contain.text', 'proj1');
    });

});

