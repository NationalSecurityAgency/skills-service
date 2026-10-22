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

describe('Video Credit Reporting Recovery Tests', () => {
    const reportUrl = '/api/projects/proj1/skills/skill1';
    let releasePendingResponse;

    beforeEach(() => {
        releasePendingResponse = null;
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, { numPerformToCompletion: 3, pointIncrementInterval: 0 });
        cy.saveVideoAttrs(1, 1, { file: 'create-subject.webm', transcript: 'Read this transcript for credit.' });
        cy.createSkill(1, 1, 1, { numPerformToCompletion: 3, pointIncrementInterval: 0, selfReportingType: 'Video' });
        cy.cdVisit('/subjects/subj1/skills/skill1');
    });

    afterEach(() => {
        if (releasePendingResponse) {
            releasePendingResponse();
        }
    });

    const watchEntireVideo = () => {
        cy.get('[data-cy="videoPlayer"] [title="Play Video"]').click();
        cy.get('[data-cy="videoPlayer"] video', { timeout: 30000 }).should('have.prop', 'ended', true);
    };

    const assertCreditSaved = () => {
        cy.get('[data-cy="skillProgress-ptsOverProgressBard"]').should('contain.text', '100 / 300 Points');
        cy.get('[data-cy="watchVideoMsg"]').should('contain.text', 'You just earned 100 points');
        cy.get('[data-cy="videoError"]').should('not.exist');
        cy.get('[data-cy="retryVideoCreditBtn"]').should('not.exist');
    };

    it('keep retry available after another failure and disable it while reporting', () => {
        let attempts = 0;
        const failure = { statusCode: 503, body: { explanation: 'Credit reporting is temporarily unavailable.' } };
        cy.intercept('POST', reportUrl, (request) => {
            attempts += 1;
            if (attempts === 1) {
                request.reply(failure);
            } else if (attempts === 2) {
                // Hold the response until the UI demonstrates its in-flight state.
                return new Cypress.Promise((resolve) => {
                    releasePendingResponse = () => {
                        releasePendingResponse = null;
                        request.reply(failure);
                        resolve();
                    };
                });
            } else {
                request.continue();
            }
        });

        watchEntireVideo();
        cy.get('[data-cy="videoError"]').should('be.visible')
            .and('contain.text', 'Credit reporting is temporarily unavailable.')
            .and('contain.text', "You don't need to watch the video again.");
        cy.get('[data-cy="percentWatched"]').should('have.text', '100');
        cy.get('[data-cy="skillProgress-ptsOverProgressBard"]').should('contain.text', '0 / 300 Points');
        cy.get('[data-cy="retryVideoCreditBtn"]').should('be.enabled').click();
        cy.get('[data-cy="retryVideoCreditBtn"]').should('be.disabled');
        cy.get('[data-cy="videoCreditSaving"]').should('be.visible');
        cy.wrap(null).should(() => {
            expect(releasePendingResponse, 'retry response is held').to.be.a('function');
        }).then(() => releasePendingResponse());

        cy.get('[data-cy="retryVideoCreditBtn"]').should('be.enabled');
        cy.get('[data-cy="videoError"]').should('contain.text', 'Credit reporting is temporarily unavailable.');
        cy.get('[data-cy="retryVideoCreditBtn"]').click();
        assertCreditSaved();
        cy.get('[data-cy="videoPlayer"] video').should('have.prop', 'ended', true);
    });

    it('keep the completed watch available when the session needs to be renewed', () => {
        cy.intercept({ method: 'POST', url: reportUrl, times: 1 }, { statusCode: 401, body: {} });

        watchEntireVideo();
        cy.get('[data-cy="videoError"]').should('contain.text', 'Please sign in again, then retry saving your credit.');
        cy.url().should('include', '/subjects/subj1/skills/skill1');
        cy.get('[data-cy="retryVideoCreditBtn"]').should('be.enabled').click();
        assertCreditSaved();
    });

    it('accept HTTP 201 as a successful reporting response', () => {
        cy.intercept('POST', reportUrl, (request) => {
            request.continue((response) => {
                response.statusCode = 201;
            });
        });

        watchEntireVideo();
        assertCreditSaved();
    });

    it('do not offer retry for a successful response without a credit confirmation', () => {
        cy.intercept('POST', reportUrl, (request) => {
            request.continue((response) => response.send({ statusCode: 204, body: '' }));
        });
        watchEntireVideo();
        cy.get('[data-cy="videoError"]').should('contain.text', 'Please refresh the page to check your progress.');
        cy.get('[data-cy="retryVideoCreditBtn"]').should('not.exist');
        cy.reload();
        cy.get('[data-cy="skillProgress-ptsOverProgressBard"]').should('contain.text', '100 / 300 Points');
    });

    it('retry transcript credit without reading or claiming it again', () => {
        cy.intercept({ method: 'POST', url: reportUrl, times: 1 }, { statusCode: 500, body: {} });
        cy.get('[data-cy="viewTranscriptBtn"]').click();
        cy.get('[data-cy="videoTranscript"]').should('contain.text', 'Read this transcript for credit.');
        cy.get('[data-cy="certifyTranscriptReadCheckbox"] input').click();
        cy.get('[data-cy="claimPtsByReadingTranscriptBtn"]').should('be.enabled').click();

        cy.get('[data-cy="videoError"]').should('contain.text', "You don't need to read the transcript again.");
        cy.get('[data-cy="claimPtsByReadingTranscriptBtn"]').should('be.disabled');
        cy.get('[data-cy="retryVideoCreditBtn"]').should('be.enabled').click();
        assertCreditSaved();
        cy.get('[data-cy="videoPlayer"] video').should('have.prop', 'paused', true).and('have.prop', 'currentTime', 0);
    });

    it('handle a missing HTTP response without an exception or navigation', () => {
        cy.intercept('POST', reportUrl, { forceNetworkError: true });

        watchEntireVideo();
        cy.get('[data-cy="videoError"]').should('contain.text', 'Please refresh the page to check your progress.');
        cy.get('[data-cy="retryVideoCreditBtn"]').should('not.exist');
        cy.url().should('include', '/subjects/subj1/skills/skill1');
        cy.get('[data-cy="skillProgress-ptsOverProgressBard"]').should('contain.text', '0 / 300 Points');
    });
});
