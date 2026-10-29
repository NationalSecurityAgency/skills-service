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

describe('Learner catalog participation', () => {
    const initializeUrl = '/api/projects/proj1/progress/initialize';
    const loadingMessage = '[data-cy="userProgressLoading"]';

    const createCatalog = (withSourceCredit = false) => {
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1);
        if (withSourceCredit) {
            cy.reportSkill(2, 1, Cypress.env('proxyUser'), 'now');
        }
        cy.exportSkillToCatalog(2, 1, 1);
        cy.importSkillFromCatalog(1, 1, 2, 1);
        cy.finalizeCatalogImport(1);
        cy.waitForBackendAsyncTasksToComplete();
    };

    beforeEach(() => {
        cy.waitForBackendAsyncTasksToComplete();
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 2, { selfReportingType: 'HonorSystem' });
    });

    ['dashboard', 'iframe'].forEach((mode) => {
        const display = (selector, options = {}) => mode === 'iframe' ? cy.wrapIframe().find(selector, options) : cy.get(selector, options);
        const visit = (path = '') => {
            if (mode === 'iframe') {
                cy.visit(`/test-skills-client/proj1?skillsClientDisplayPath=${encodeURIComponent(path || '/')}`);
            } else {
                cy.visit(`/progress-and-rankings/projects/proj1${path}`);
            }
        };

        it(`${mode}: ordinary first visit shows the pending message and automatically loads imported progress`, () => {
            createCatalog(true);
            // Let the production worker run normally: no deferred finalization or response stubs.
            cy.intercept('POST', initializeUrl).as('initialize');
            visit();
            cy.wait('@initialize').its('response.body').should('deep.equal', { ready: false, backfillStatus: 'PENDING' });
            display(loadingMessage).should('be.visible').and('contain.text', 'Loading your latest progress')
                .and('contain.text', 'You can keep exploring while we sync your points and achievements.');
            display('[data-cy="subjectTileBtn"]').should('be.visible').and('not.be.disabled');

            // Wait through the UI's own status checks: no reload or test-side initialization.
            display('[data-cy="overallPoints"] [data-cy="earnedPoints"]', { timeout: 60000 })
                .should('have.text', '100');
            display(loadingMessage, { timeout: 60000 }).should('not.exist');
            display('[data-cy="subjectTile-subj1"] [data-cy="pointsProgress"]').should('have.text', '100 / 400');
            cy.get('@initialize.all').should((requests) => {
                expect(requests.some((request) => request.response?.body.backfillStatus === 'COMPLETED')).to.eq(true);
            });

            cy.get('@initialize.all').then((requests) => {
                const requestCount = requests.length;
                display('[data-cy="subjectTileBtn"]').click();
                display('[data-cy="skillsTitle"]').should('contain.text', 'Subject 1');
                display(loadingMessage).should('not.exist');
                // Existing native reporting remains usable alongside the imported progress.
                display('[data-cy="skillProgressTitle"]').contains('Very Great Skill 2').click();
                display('[data-cy="claimPointsBtn"]').should('be.enabled').click();
                display('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]')
                    .should('contain.text', '100 Total');
                if (mode === 'iframe') {
                    display('[data-cy="breadcrumbLink-subj1"]').click();
                } else {
                    cy.get('[data-cy="breadcrumb-subj1"]').click();
                }
                display('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1').click();
                display('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]')
                    .should('contain.text', '100 Total');
                display(loadingMessage).should('not.exist');
                cy.get('@initialize.all').should('have.length', requestCount);
            });

            cy.intercept('POST', initializeUrl).as('reloadInitialize');
            cy.reload();
            cy.wait('@reloadInitialize').its('response.body').should('deep.equal', { ready: true, backfillStatus: 'COMPLETED' });
            display('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]')
                .should('contain.text', '100 Total');
            display(loadingMessage).should('not.exist');
            cy.get('@reloadInitialize.all').should('have.length', 1);
        });

        it(`${mode}: completed empty-source initialization on a deep link shows no message`, () => {
            createCatalog();
            cy.intercept('POST', initializeUrl).as('initialize');
            visit('/subjects/subj1/skills/skill2');
            cy.wait('@initialize').its('response.body').should('deep.equal', { ready: true, backfillStatus: 'COMPLETED' });
            display('[data-cy="skillProgressTitle"]').should('contain.text', 'Very Great Skill 2');
            display('[data-cy="claimPointsBtn"]').should('be.enabled');
            display(loadingMessage).should('not.exist');
            cy.get('@initialize.all').should('have.length', 1);
        });
    });

    it('no-import initialization shows no message and runs again for a different project', () => {
        cy.createProject(3);
        cy.createSubject(3, 1);
        cy.createSkill(3, 1, 3);
        cy.addToMyProjects(3);
        cy.intercept('POST', '/api/projects/*/progress/initialize').as('initialize');
        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1');
        cy.wait('@initialize').its('response.body').should('deep.equal', { ready: true, backfillStatus: 'NOT_NEEDED' });
        cy.get('[data-cy="skillProgressTitle"]').should('contain.text', 'Very Great Skill 2');
        cy.get(loadingMessage).should('not.exist');
        cy.get('[data-cy="breadcrumb-Progress And Rankings"]').click();
        cy.get('[data-cy="project-link-proj3"]').click();
        cy.wait('@initialize').its('request.url').should('contain', '/projects/proj3/progress/initialize');
        cy.get('[data-cy="subjectTileBtn"]').should('be.visible').click();
        cy.get('[data-cy="skillProgressTitle"]').should('contain.text', 'Very Great Skill 3');
        cy.get(loadingMessage).should('not.exist');
        cy.get('@initialize.all').should('have.length', 2);
    });

    it('completed status shows no message even when applicable failed finalization makes ready false', () => {
        createCatalog();
        cy.request('POST', initializeUrl).its('body.backfillStatus').should('eq', 'COMPLETED');
        // A terminal participant retains its status while applicable finalization affects readiness.
        cy.request('POST', '/admin/projects/proj1/settings/catalog.finalize.state', {
            projectId: 'proj1', setting: 'catalog.finalize.state', value: 'FAILED'
        });
        cy.intercept('POST', initializeUrl).as('initialize');
        cy.visit('/progress-and-rankings/projects/proj1');
        cy.wait('@initialize').its('response.body').should('deep.equal', { ready: false, backfillStatus: 'COMPLETED' });
        cy.get('[data-cy="overallPoints"] [data-cy="earnedPoints"]').should('have.text', '0');
        cy.get('[data-cy="subjectTileBtn"]').should('be.visible').and('not.be.disabled');
        cy.get(loadingMessage).should('not.exist');
        cy.request('POST', '/admin/projects/proj1/settings/catalog.finalize.state', {
            projectId: 'proj1', setting: 'catalog.finalize.state', value: 'COMPLETED'
        });
    });

    it('admin preview bypasses initialization and learner navigation clears the preview identity', () => {
        createCatalog(true);
        cy.addToMyProjects(1);
        cy.reportSkill(1, 2, 'preview-user@skills.org', 'now');
        cy.waitForBackendAsyncTasksToComplete();
        cy.intercept('POST', '/api/projects/*/progress/initialize').as('initialize');
        cy.visit('/administrator/projects/proj1/users/preview-user@skills.org/subjects/subj1/skills/skill2');
        cy.get('[data-cy="skillProgressTitle"]').should('contain.text', 'Very Great Skill 2');
        cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]').should('contain.text', '100 Total');
        cy.get(loadingMessage).should('not.exist');
        cy.get('@initialize.all').should('have.length', 0);
        cy.get('[aria-label="User Settings Button"]').click();
        cy.contains('[role="menuitem"]', 'Progress and Ranking').click();
        cy.get('[data-cy="project-link-proj1"]').click();
        cy.wait('@initialize').then(({ request, response }) => {
            expect(request.url).not.to.contain('userId');
            expect(response.body.backfillStatus).to.be.oneOf(['PENDING', 'COMPLETED']);
        });
        cy.get('[data-cy="subjectTileBtn"]').click();
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 2').click();
        cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]').should('contain.text', '0 Total');
        cy.get(loadingMessage).should('not.exist');
        cy.waitForBackendAsyncTasksToComplete();
    });
});
