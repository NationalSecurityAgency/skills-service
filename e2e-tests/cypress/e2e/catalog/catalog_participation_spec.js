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
    let deferredFinalization = false;
    let displayClock;

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

    const deferCatalogBackfill = () => {
        createCatalog(true);
        // Hold real backfill at the existing failed-finalization boundary, not by stubbing responses.
        cy.request('POST', '/admin/projects/proj1/settings/catalog.finalize.state', {
            projectId: 'proj1', setting: 'catalog.finalize.state', value: 'FAILED'
        }).then(() => {
            deferredFinalization = true;
        });
    };

    const releaseCatalogBackfill = () => {
        // Real finalization completion schedules the pending-participant sweep, including an empty batch.
        cy.finalizeCatalogImport(1);
        cy.waitForBackendAsyncTasksToComplete().then(() => {
            deferredFinalization = false;
        });
    };

    afterEach(() => {
        // Restore the display clock even when an assertion fails.
        if (displayClock) {
            displayClock.restore();
            displayClock = undefined;
        }
        if (deferredFinalization) {
            releaseCatalogBackfill();
        }
    });

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

        it(`${mode}: initialization failure renders the error page instead of the loading spinner`, () => {
            cy.intercept('POST', initializeUrl, { statusCode: 500, body: {} });
            visit();

            display('[data-cy="errorPage"]').should('be.visible');
            display('[data-cy="errExplanation"]').should('contain.text', 'Oh no, something went wrong!');
            display('[data-cy="skillsDisplayHome"] [aria-label="Loading"]').should('not.exist');
            display('[data-cy="overallPoints"]').should('not.exist');
            display('[data-cy="subjectTileBtn"]').should('not.exist');
            display(loadingMessage).should('not.exist');
        });

        it(`${mode}: pending first visit automatically loads imported progress after backfill is released`, () => {
            deferCatalogBackfill();
            cy.intercept('POST', initializeUrl).as('initialize');
            visit();
            cy.wait('@initialize').its('response.body').should('deep.equal', { ready: false, backfillStatus: 'PENDING' });
            // Establish incomplete rendered progress before validating the automatic summary refresh.
            display('[data-cy="overallPoints"] [data-cy="earnedPoints"]').should('have.text', '0');
            display(loadingMessage).should('be.visible').and('contain.text', 'Loading your latest progress')
                .and('contain.text', 'You can keep exploring while we sync your points and achievements.');
            display('[data-cy="subjectTileBtn"]').should('be.visible').and('not.be.disabled');

            releaseCatalogBackfill();
            // Wait through the UI's own status checks: no reload or test-side initialization.
            display('[data-cy="overallPoints"] [data-cy="earnedPoints"]', { timeout: 60000 })
                .should('have.text', '100');
            display(loadingMessage, { timeout: 60000 }).should('not.exist');
            display('[data-cy="subjectTile-subj1"] [data-cy="pointsProgress"]').should('have.text', '100 / 400');
            cy.get('@initialize.all').should((requests) => {
                expect(requests.some((request) => request.response?.body.backfillStatus === 'COMPLETED')).to.eq(true);
            });
        });

        it(`${mode}: ordinary first visit loads imported progress even when backfill finishes immediately`, () => {
            createCatalog(true);
            // Let the real worker run freely. Its completion may precede the first summary,
            // so neither an intermediate zero-point render nor a pending message is required.
            visit();
            display('[data-cy="overallPoints"] [data-cy="earnedPoints"]', { timeout: 60000 })
                .should('have.text', '100');
            display(loadingMessage, { timeout: 60000 }).should('not.exist');
            display('[data-cy="subjectTile-subj1"] [data-cy="pointsProgress"]').should('have.text', '100 / 400');
        });

        // Keep one iframe integration flow; shared status variations and pending interactions run on the dashboard.
        if (mode !== 'dashboard') {
            return;
        }

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

        const returnToSubject = () => cy.get('[data-cy="breadcrumb-subj1"]').click();
        const returnHome = () => cy.get('[data-cy="breadcrumb-proj1"]').click();
        const advanceRefreshTime = (milliseconds) => cy.then(() => {
            displayClock.tick(milliseconds);
        });

        it(`${mode}: pending progress permits imported drilldown and native reporting without child initialization`, () => {
            deferCatalogBackfill();
            cy.intercept('POST', initializeUrl).as('initialize');
            visit();
            cy.wait('@initialize').its('response.body').should('deep.equal', { ready: false, backfillStatus: 'PENDING' });
            display('[data-cy="overallPoints"] [data-cy="earnedPoints"]').should('have.text', '0');
            display(loadingMessage).should('be.visible');
            display('[data-cy="subjectTileBtn"]').should('be.visible')
                .find('button').should('be.enabled').click();
            display('[data-cy="skillsTitle"]').should('contain.text', 'Subject 1');
            display(loadingMessage).should('not.exist');

            cy.get('@initialize.all').then((requests) => {
                const requestCount = requests.length;
                display('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1').click();
                display('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]')
                    .should(($title) => expect($title.text().trim()).to.eq('0 Total'));
                display(loadingMessage).should('not.exist');
                returnToSubject();
                display('[data-cy="skillProgressTitle"]').contains('Very Great Skill 2').click();
                display('[data-cy="claimPointsBtn"]').should('be.enabled').click();
                display('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]')
                    .should(($title) => expect($title.text().trim()).to.eq('100 Total'));
                display(loadingMessage).should('not.exist');
                cy.get('@initialize.all').should('have.length', requestCount);
            });

            returnHome();
            display('[data-cy="overallPoints"] [data-cy="earnedPoints"]').should('have.text', '100');
            display(loadingMessage).should('be.visible');
            releaseCatalogBackfill();
            // No reload or test-side learner initialization: the home page must refresh itself.
            display('[data-cy="overallPoints"] [data-cy="earnedPoints"]', { timeout: 60000 })
                .should('have.text', '200');
            display(loadingMessage, { timeout: 60000 }).should('not.exist');
            display('[data-cy="subjectTile-subj1"] [data-cy="pointsProgress"]').should('have.text', '200 / 400');
            cy.get('@initialize.all').should((requests) => {
                expect(requests.some((request) => request.response?.body.backfillStatus === 'COMPLETED')).to.eq(true);
            });
        });

        it(`${mode}: pending home checks stop on child pages, resume on return and stop after completion`, () => {
            deferCatalogBackfill();
            cy.intercept('POST', initializeUrl).as('initialize');
            visit();
            cy.wait('@initialize').its('response.body').should('deep.equal', { ready: false, backfillStatus: 'PENDING' });
            display(loadingMessage).should('be.visible');
            display('[data-cy="subjectTileBtn"]').click();
            display('[data-cy="skillsTitle"]').should('contain.text', 'Subject 1');

            // Install after leaving home so its original real timer has been cleared.
            // Control only the dashboard display's timeout APIs, not backend processing.
            display('[data-cy="skillsTitle"]').then(($title) => {
                displayClock = Cypress.sinon.useFakeTimers({
                    global: $title[0].ownerDocument.defaultView,
                    toFake: ['setTimeout', 'clearTimeout']
                });
            });
            cy.get('@initialize.all').then((requests) => {
                const initialCount = requests.length;
                // Consume any real home checks that happened before the clock was installed.
                // Later waits must observe the checks triggered by advancing this display's clock.
                for (let index = 1; index < initialCount; index += 1) {
                    cy.wait('@initialize').its('response.body.backfillStatus').should('eq', 'PENDING');
                }
                advanceRefreshTime(9000);
                cy.get('@initialize.all').should('have.length', initialCount);
                display(loadingMessage).should('not.exist');

                returnHome();
                display(loadingMessage).should('be.visible');
                display('[data-cy="overallPoints"] [data-cy="earnedPoints"]').should('have.text', '0');
                cy.get('@initialize.all').should('have.length', initialCount);
                advanceRefreshTime(3000);
                cy.wait('@initialize').its('response.body').should('deep.equal', { ready: false, backfillStatus: 'PENDING' });
                cy.get('@initialize.all').should('have.length', initialCount + 1);

                // Cancel a fake-clock-owned pending timer, then advance past several refresh intervals.
                display('[data-cy="subjectTileBtn"]').click();
                display('[data-cy="skillsTitle"]').should('contain.text', 'Subject 1');
                advanceRefreshTime(9000);
                cy.get('@initialize.all').should('have.length', initialCount + 1);
                display(loadingMessage).should('not.exist');

                returnHome();
                display(loadingMessage).should('be.visible');
                advanceRefreshTime(2999);
                cy.get('@initialize.all').should('have.length', initialCount + 1);
                advanceRefreshTime(1);
                cy.wait('@initialize').its('response.body').should('deep.equal', { ready: false, backfillStatus: 'PENDING' });
                cy.get('@initialize.all').should('have.length', initialCount + 2);

                releaseCatalogBackfill();
                advanceRefreshTime(3000);
                cy.wait('@initialize').its('response.body').should('deep.equal', { ready: true, backfillStatus: 'COMPLETED' });
                display('[data-cy="overallPoints"] [data-cy="earnedPoints"]').should('have.text', '100');
                display(loadingMessage).should('not.exist');
                advanceRefreshTime(9000);
                cy.get('@initialize.all').should('have.length', initialCount + 3);
                display('[data-cy="overallPoints"] [data-cy="earnedPoints"]').should('have.text', '100');
            });
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
        cy.request('POST', initializeUrl, {}).its('body.backfillStatus').should('eq', 'COMPLETED');
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
