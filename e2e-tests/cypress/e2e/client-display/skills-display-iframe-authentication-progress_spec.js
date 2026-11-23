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

import { createCompactViewProject, setProjectDefault } from '../../support/compact-view';
import {
  authenticationError, refreshAuthentication, useIframeParentFixture,
  assertHome, assertHeightFitsDisplay, expireAuthentication, assertRecoveredSubject,
} from '../../support/iframe-tests';

// Progress, project settings and identity fixtures require a fresh database per case.
Cypress.env('iframeTestSetup', 'isolated');

describe('Embedded Skills Display progress and lifecycle recovery', () => {
  useIframeParentFixture();
  beforeEach(() => {
    createCompactViewProject({ compact: false });
    // Some display data loaders propagate the expected rejected Axios request.
    // Do not suppress unrelated exceptions.
    cy.on('uncaught:exception', (error) => {
      if (error.name === 'AxiosError' && error.message === 'Request failed with status code 401') return false;
      if (error.name === 'CanceledError' && error.message === 'Skills Display request was superseded') return false;
    });
  });

  it('recovers in the same iframe when proxy learner casing changes in both directions', () => {
    let tokenForDisplay;
    let mixedCaseToken;
    let lowercaseToken;
    let iframeDocument;
    // Use service-issued proxy credentials without creating a persisted account,
    // so /app/userInfo exposes the raw proxy_user casing on each authentication.
    cy.request('/admin/projects/proj1/token/LearnerA@skills.org').then(({ body }) => {
      mixedCaseToken = body;
      tokenForDisplay = body;
    });
    cy.request('/admin/projects/proj1/token/learnera@skills.org').then(({ body }) => { lowercaseToken = body; });
    cy.intercept('GET', '**/api/projects/proj1/token', (req) => req.reply(tokenForDisplay));
    cy.visit('/__e2e/skills-display-parent?manualRecovery=true');
    assertHome();
    cy.get('#skills-client-container iframe').then(($iframe) => { iframeDocument = $iframe[0].contentDocument; });
    ['lowercase', 'mixed-case'].forEach((casing) => {
      expireAuthentication();
      cy.then(() => { tokenForDisplay = casing === 'lowercase' ? lowercaseToken : mixedCaseToken; });
      cy.get(refreshAuthentication).click();
      assertRecoveredSubject();
      cy.wrapIframe().find('[data-cy="skillsDisplayInitError"]').should('not.exist');
      cy.get('#skills-client-container iframe').then(($iframe) => {
        expect($iframe[0].contentDocument, 'same learner retains the iframe document').to.equal(iframeDocument);
      });
      cy.wrapIframe().find('[data-cy="back"]').click();
      assertHome();
    });
  });

  describe('Same-learner progress recovery', () => {
    // One iframe belongs to one learner. A renewed token must not accept stale
    // snapshots or loading cleanup, even when progress changes for that SAME learner.
    const parentUrl = '/__e2e/skills-display-parent?manualRecovery=true&allowManualRefresh=true';
    let releases;
    const holdResponse = (url, alias, modifyResponse = () => {}) => {
      let received = false;
      let release;
      const gate = new Cypress.Promise((resolve) => { release = resolve; });
      releases.push(release);
      cy.intercept({ method: 'GET', url, times: 1 }, (req) => {
        req.continue((res) => {
          modifyResponse(res);
          received = true;
          return gate;
        });
      }).as(alias);
      return { release, isReceived: () => received };
    };
    beforeEach(() => {
      releases = [];
      // Leave room for more progress without replacing the counter with the completed UI.
      cy.createSkill(1, 1, 1, { pointIncrement: 100, numPerformToCompletion: 3 });
      cy.doReportSkill({ userId: Cypress.env('proxyUser'), date: '2020-09-12 11:00' });
      if (Cypress.browser.family === 'chromium') {
        cy.then(() => Cypress.automation('remote:debugger:protocol', {
          command: 'Network.setCacheDisabled', params: { cacheDisabled: true },
        }));
      }
    });
    afterEach(() => {
      releases.forEach((release) => release());
      if (Cypress.browser.family === 'chromium') {
        cy.then(() => Cypress.automation('remote:debugger:protocol', {
          command: 'Network.setCacheDisabled', params: { cacheDisabled: false },
        }));
      }
    });

    const addMoreProgress = () => cy.doReportSkill({ userId: Cypress.env('proxyUser'), date: '2020-09-13 11:00' });
    const assertFreshHome = () => {
      assertHome();
      cy.wrapIframe().find('[data-cy="earnedPoints"]').should('have.text', '200');
      cy.wrapIframe().find('[data-cy="myRankPosition"]').should('have.text', '#1');
    };

    it('restores loading flags while refreshing cached progress for the same learner after a 401', () => {
      cy.visit(parentUrl);
      assertHome();
      cy.wrapIframe().find('[data-cy="earnedPoints"]').should('have.text', '100');
      addMoreProgress();
      expireAuthentication();
      // Preserve the homepage as the requested recovery route.
      cy.get('[data-cy="navigateDisplayHome"]').click();
      const fresh = holdResponse('**/api/projects/proj1/summary*', 'replacementSummary');
      cy.get(refreshAuthentication).click();
      cy.wrap(null).should(() => { expect(fresh.isReceived(), 'new summary is held').to.be.true; });
      cy.wrapIframe().find('[data-pc-name="progressspinner"]').should('be.visible');
      cy.wrapIframe().find('[data-cy="earnedPoints"]').should('not.exist');
      cy.wrapIframe().find('[data-cy="subjectTile-subj1"]').should('not.exist');
      cy.then(fresh.release);
      assertFreshHome();
    });

    [true, false].forEach((duringLoad) => {
      it(`ignores an old-token summary that succeeds ${duringLoad ? 'while' : 'after'} fresh progress loads for the same learner`, () => {
        const old = holdResponse('**/api/projects/proj1/summary*', 'oldSummary');
        cy.visit(parentUrl);
        cy.wrap(null).should(() => { expect(old.isReceived(), 'old summary is held').to.be.true; });
        addMoreProgress();
        const fresh = duringLoad ? holdResponse('**/api/projects/proj1/summary*', 'newSummary') : null;
        cy.get(refreshAuthentication).click();
        if (duringLoad) {
          cy.wrap(null).should(() => { expect(fresh.isReceived(), 'new summary is held').to.be.true; });
        } else {
          assertFreshHome();
        }
        cy.then(old.release);
        cy.wait('@oldSummary');
        if (duringLoad) {
          cy.wrapIframe().find('[data-pc-name="progressspinner"]').should('be.visible');
          cy.wrapIframe().find('[data-cy="earnedPoints"]').should('not.exist');
          cy.then(fresh.release);
        }
        assertFreshHome();
      });
    });

    it('ignores an old-token subject snapshot after fresh subject and skill progress loads for the same learner', () => {
      cy.visit(parentUrl);
      assertHome();
      const old = holdResponse('**/api/projects/proj1/subjects/subj1/summary*', 'oldSubject');
      cy.wrapIframe().find('[data-cy="subjectTile-subj1"] [data-cy="subjectTileBtn"]').click();
      cy.wrap(null).should(() => { expect(old.isReceived(), 'old subject is held').to.be.true; });
      addMoreProgress();
      cy.get(refreshAuthentication).click();
      assertRecoveredSubject();
      cy.wrapIframe().find('[data-cy="earnedPoints"]').should('have.text', '200');
      cy.wrapIframe().find('[data-cy="skillPoints"]').should('have.text', '200');
      cy.then(old.release);
      cy.wait('@oldSubject');
      cy.wrapIframe().find('[data-cy="earnedPoints"]').should('have.text', '200');
      cy.wrapIframe().find('[data-cy="skillPoints"]').should('have.text', '200');
    });

    [
      { name: 'subject', url: '**/api/projects/proj1/subjects/subj1/summary*' },
      { name: 'skill', url: '**/api/projects/proj1/subjects/subj1/skills/skill1/summary*' },
    ].forEach(({ name, url }) => {
      it(`keeps replacement ${name} loading visible when an old-token request finishes`, () => {
        cy.visit(parentUrl);
        assertHome();
        if (name === 'skill') {
          cy.wrapIframe().find('[data-cy="subjectTile-subj1"] [data-cy="subjectTileBtn"]').click();
          assertRecoveredSubject();
        }
        const old = holdResponse(url, 'oldDetailSummary');
        if (name === 'subject') {
          cy.wrapIframe().find('[data-cy="subjectTile-subj1"] [data-cy="subjectTileBtn"]').click();
        } else {
          cy.wrapIframe().find('[data-cy="skillProgressTitle-skill1"] [data-cy="skillProgressTitle"]').click();
        }
        cy.wrap(null).should(() => { expect(old.isReceived(), 'old detail response is held').to.be.true; });
        addMoreProgress();
        const fresh = holdResponse(url, 'replacementDetailSummary');
        cy.get(refreshAuthentication).click();
        cy.wrap(null).should(() => { expect(fresh.isReceived(), 'replacement detail response is held').to.be.true; });
        cy.then(old.release);
        cy.wait('@oldDetailSummary');
        cy.wrapIframe().find('[data-pc-name="progressspinner"]').should('be.visible');
        cy.wrapIframe().find('[data-cy="skillPoints"]').should('not.exist');
        cy.wrapIframe().find('[data-cy="earnedPoints"]').should('not.exist');
        cy.then(fresh.release);
        cy.wrapIframe().find('[data-cy="skillPoints"]').should('have.text', '200');
        cy.wrapIframe().find('[data-pc-name="progressspinner"]').should('not.exist');
        cy.wrapIframe().find(authenticationError).should('not.exist');
      });
    });

    it('keeps the replacement rank loading visible when old-token ranking cleanup runs', () => {
      const old = holdResponse('**/api/projects/proj1/rank*', 'oldPendingRank');
      cy.visit(parentUrl);
      cy.wrapIframe().find('[data-cy="earnedPoints"]').should('have.text', '100');
      cy.wrap(null).should(() => { expect(old.isReceived(), 'old rank response is held').to.be.true; });
      addMoreProgress();
      const fresh = holdResponse('**/api/projects/proj1/rank*', 'replacementRank');
      cy.get(refreshAuthentication).click();
      cy.wrap(null).should(() => { expect(fresh.isReceived(), 'replacement rank response is held').to.be.true; });
      cy.then(old.release);
      cy.wait('@oldPendingRank');
      cy.wrapIframe().find('[data-cy="myRank"] [data-pc-name="progressspinner"]').should('be.visible');
      cy.wrapIframe().find('[data-cy="myRankPosition"]').should('not.exist');
      cy.then(fresh.release);
      assertFreshHome();
    });

    it('ignores successful old-token ranking and point-history responses after recovery for the same learner', () => {
      const oldRank = holdResponse('**/api/projects/proj1/rank*', 'oldRank', (res) => {
        res.body.position = 99;
      });
      const oldHistory = holdResponse('**/api/projects/proj1/pointHistory*', 'oldHistory', (res) => {
        res.body = { pointsHistory: [], achievements: [] };
      });
      cy.visit(parentUrl);
      cy.wrapIframe().find('[data-cy="earnedPoints"]').should('have.text', '100');
      cy.wrap(null).should(() => {
        expect(oldRank.isReceived(), 'old ranking is held').to.be.true;
        expect(oldHistory.isReceived(), 'old history is held').to.be.true;
      });
      addMoreProgress();
      cy.get(refreshAuthentication).click();
      assertFreshHome();
      cy.wrapIframe().find('[data-cy="pointHistoryChartWithData"]').should('be.visible');
      cy.then(() => { oldRank.release(); oldHistory.release(); });
      cy.wait(['@oldRank', '@oldHistory']);
      assertFreshHome();
      cy.wrapIframe().find('[data-cy="pointHistoryChartWithData"]').should('be.visible');
      cy.wrapIframe().find('[data-cy="pointHistoryChartNoData"]').should('not.exist');
    });

    it('rejects a different authenticated learner until the hosting application recreates the iframe', () => {
      cy.visit(parentUrl);
      assertHome();
      let originalDocument;
      let originalToken;
      let otherLearnerToken;
      let tokenForDisplay;
      cy.get('iframe').then(($iframe) => { originalDocument = $iframe[0].contentDocument; });
      cy.request('/api/projects/proj1/token').then(({ body }) => { originalToken = body; });
      cy.request('/admin/projects/proj1/token/another-learner@skills.org').then(({ body }) => {
        otherLearnerToken = body;
        tokenForDisplay = body;
      });
      // The authenticator returning another identity violates the fixed-learner contract.
      // There is deliberately no fixture or SDK operation to switch learners in place.
      cy.intercept('GET', '**/api/projects/proj1/token', (req) => req.reply(tokenForDisplay)).as('displayAuthenticator');
      expireAuthentication();
      cy.get(refreshAuthentication).click();
      cy.wait('@displayAuthenticator');
      const initializationError = '[data-cy="skillsDisplayInitError"]';
      cy.wrapIframe().find(initializationError).should('be.visible').and('have.attr', 'role', 'alert')
        .and('contain.text', 'authenticated learner has changed').and('contain.text', 'destroy and recreate');
      cy.wrapIframe().find('[data-cy="skillsDisplayHome"]').should('not.exist');
      cy.wrapIframe().find('[data-cy="earnedPoints"]').should('not.exist');
      cy.wrapIframe().find('[data-pc-name="progressspinner"]').should('not.exist');

      // Even a later token for the original learner cannot reopen this violated lifetime.
      cy.then(() => { tokenForDisplay = originalToken; });
      cy.get(refreshAuthentication).click();
      // The UI must remain unchanged; synchronize with token delivery rather than
      // letting an assertion on the already-visible fatal error race the authenticator.
      cy.wait('@displayAuthenticator').then(({ response }) => {
        expect(response.body.access_token).to.equal(originalToken.access_token);
      });
      cy.wrapIframe().find(initializationError).should('be.visible').and('contain.text', 'destroy and recreate');
      cy.wrapIframe().find('[data-cy="skillsDisplayHome"]').should('not.exist');

      // Reloading the host destroys the old iframe; a new instance can bind to the other learner.
      cy.then(() => { tokenForDisplay = otherLearnerToken; });
      cy.reload();
      assertHome();
      cy.wrapIframe().find('[data-cy="earnedPoints"]').should('have.text', '0');
      cy.get('iframe').then(($iframe) => { expect($iframe[0].contentDocument).not.to.equal(originalDocument); });
    });
  });

  it('resumes automatic host sizing after repeated recovery and navigation to taller content', () => {
    const description = Array.from({ length: 60 }, (_, index) => `Height regression paragraph ${index + 1}.`).join('\n\n');
    cy.createSubject(1, 1, { description });
    cy.visit('/__e2e/skills-display-parent?manualRecovery=true&autoHeight=true');
    assertHome();
    assertHeightFitsDisplay();
    let iframeDocument;
    cy.get('#skills-client-container iframe').then(($iframe) => { iframeDocument = $iframe[0].contentDocument; });
    for (let cycle = 0; cycle < 2; cycle += 1) {
      let previousRoot;
      let homeHeight;
      cy.get('#skills-client-container iframe').then(($iframe) => {
        previousRoot = $iframe[0].contentDocument.querySelector('#skilltree-main-container');
      });
      expireAuthentication();
      assertHeightFitsDisplay();
      cy.get('[data-cy="navigateDisplayHome"]').click();
      cy.get(refreshAuthentication).click();
      assertHome();
      assertHeightFitsDisplay();
      cy.get('#skills-client-container iframe').then(($iframe) => {
        expect($iframe[0].contentDocument, 'recovery retains the iframe document').to.equal(iframeDocument);
        expect($iframe[0].contentDocument.querySelector('#skilltree-main-container'), 'recovery retains the observed root').to.equal(previousRoot);
        homeHeight = $iframe[0].getBoundingClientRect().height;
      });
      cy.wrapIframe().find('[data-cy="subjectTile-subj1"] [data-cy="subjectTileBtn"]').click();
      assertRecoveredSubject();
      cy.wrapIframe().find('[data-cy="subjectDescription"]').should('contain.text', 'Height regression paragraph 60.');
      assertHeightFitsDisplay();
      cy.get('#skills-client-container iframe').should(($iframe) => {
        expect($iframe[0].getBoundingClientRect().height, 'host grows for taller post-recovery content').to.be.greaterThan(homeHeight);
      });
      cy.wrapIframe().find('[data-cy="back"]').click();
      assertHome();
      assertHeightFitsDisplay();
    }
  });

  it('restores fresh configuration in the same iframe and reports each navigation once across repeated recovery', () => {
    let iframeDocument;
    cy.intercept('GET', '**/public/config', (req) => {
      req.continue((res) => { res.body.enablePageVisitReporting = true; });
    });
    cy.visit('/__e2e/skills-display-parent?manualRecovery=true');
    assertHome();
    cy.get('iframe').then(($iframe) => { iframeDocument = $iframe[0].contentDocument; });
    // Network assertions are intentional: duplicate analytics reports are the behavior under test.
    cy.intercept('PUT', '**/api/pageVisit').as('pageVisits');
    cy.get('[data-cy="routeChangedCount"]').invoke('text').then((text) => {
      const initialRouteChanges = Number(text);
      // Two consecutive recoveries expose accumulating listeners and stale config.
      for (let cycle = 0; cycle < 2; cycle += 1) {
        expireAuthentication();
        if (cycle === 0) {
          // A rendered setting change proves recovery reloads configuration, rather than only removing the error.
          setProjectDefault(true);
        }
        cy.get(refreshAuthentication).click();
        assertRecoveredSubject();
        cy.wrapIframe().find('[data-cy="compactProgressOverview"]').should('be.visible');
        cy.wrapIframe().find('[data-cy="overallLevel"]').should('not.exist');
        cy.get('iframe').then(($iframe) => { expect($iframe[0].contentDocument).to.equal(iframeDocument); });
        cy.get('[data-cy="routeChangedCount"]').should('have.text', String(initialRouteChanges + cycle * 2 + 1));
        cy.wrapIframe().find('[data-cy="back"]').should('be.visible').click();
        assertHome(true);
        cy.get('[data-cy="routeChangedCount"]').should('have.text', String(initialRouteChanges + (cycle + 1) * 2));
        cy.get('@pageVisits.all').should((visits) => {
          // The iframe's memory router reports home as '/', not its document URL.
          const homeVisits = visits.filter(({ request }) => request.body.skillDisplay === true
            && request.body.projectId === 'proj1' && request.body.path === '/');
          expect(homeVisits, 'one home page-visit report per recovery cycle').to.have.length(cycle + 1);
        });
      }
    });
  });
});
