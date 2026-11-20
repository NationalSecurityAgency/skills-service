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

describe('Embedded Skills Display authentication recovery', () => {
  const authenticationError = '[data-cy="skillsDisplayAuthenticationError"]';
  const refreshAuthentication = '[data-cy="refreshDisplayAuthentication"]';
  const recoveryFailedMessage = 'Unable to reconnect your session. Please refresh the page to try again.';
  const assertHome = (compact = false) => {
    cy.wrapIframe().find('[data-cy="subjectTile-subj1"] [data-cy="subjectTileBtn"]').should('be.visible');
    cy.wrapIframe().find('[data-cy="overallLevel"]').should(compact ? 'not.exist' : 'be.visible');
    if (compact) cy.wrapIframe().find('[data-cy="compactProgressOverview"]').should('be.visible');
    cy.wrapIframe().find(authenticationError).should('not.exist');
    cy.wrapIframe().find('[data-pc-name="progressspinner"]').should('not.exist');
  };

  beforeEach(() => {
    createCompactViewProject({ compact: false });
    cy.readFile('node_modules/postmate/build/postmate.min.js').then((postmate) => {
      cy.intercept('GET', '/__e2e/postmate.js', {
        headers: { 'content-type': 'application/javascript' },
        body: postmate,
      });
    });
    cy.intercept('GET', '/__e2e/skills-display-parent*', {
      headers: { 'content-type': 'text/html' },
      fixture: 'skills-display-parent.html',
    });
    // Some display data loaders propagate the expected rejected Axios request.
    // Do not suppress unrelated exceptions.
    cy.on('uncaught:exception', (error) => {
      if (error.name === 'AxiosError' && error.message === 'Request failed with status code 401') return false;
    });
  });

  const expireAuthentication = () => {
    // Simulate the server's expired-token response on a request made by a loaded iframe.
    cy.intercept({ method: 'GET', url: '**/api/projects/proj1/subjects/subj1/summary*', times: 1 }, {
      statusCode: 401,
      body: { explanation: 'Authentication token has expired' },
    });
    cy.wrapIframe().find('[data-cy="subjectTile-subj1"] [data-cy="subjectTileBtn"]').click();
    cy.wrapIframe().find(authenticationError).should('be.visible').and('contain.text', 'authentication has expired');
    cy.wrapIframe().find(authenticationError).should('have.attr', 'role', 'alert')
      .and('have.attr', 'aria-labelledby', 'skills-display-authentication-title');
    cy.wrapIframe().find('#skills-display-authentication-title').should('be.visible').and('contain.text', 'Re-authentication required');
    cy.wrapIframe().find('[data-pc-name="progressspinner"]').should('not.exist');
    cy.wrapIframe().find('[data-cy="skillsDisplayHome"]').should('not.exist');
    cy.get(refreshAuthentication).should('be.visible');
  };

  const assertRecoveredSubjectContent = () => {
    cy.wrapIframe().find('[data-cy="skillsTitle"]').should('be.visible').and('contain.text', 'Subject 1');
    cy.wrapIframe().find('[data-cy="skillProgress_index-0"]').should('be.visible');
    cy.wrapIframe().find(authenticationError).should('not.exist');
  };

  const assertRecoveredSubject = () => {
    assertRecoveredSubjectContent();
    cy.wrapIframe().find('[data-pc-name="progressspinner"]').should('not.exist');
  };

  describe('Startup', () => {
    it('requests a replacement token and initializes the display when the first user-information request returns 401', () => {
      cy.intercept({ method: 'GET', url: '**/app/userInfo', times: 1 }, { statusCode: 401, body: {} });
      // A new iframe has never populated its user information.
      cy.visit('/__e2e/skills-display-parent?manualRecovery=true');
      cy.wrapIframe().find(authenticationError).should('be.visible').and('contain.text', 'authentication has expired');
      cy.wrapIframe().find('[data-pc-name="progressspinner"]').should('not.exist');
      cy.get(refreshAuthentication).should('be.visible').click();
      assertHome();
    });

    describe('Superseded startup authentication', () => {
      let releaseInitialUserInfo;
      let releaseConfig;

      beforeEach(() => {
        releaseInitialUserInfo = null;
        releaseConfig = null;
        if (Cypress.browser.family === 'chromium') {
          // Chromium can serialize identical GETs behind a cache entry whose response headers are held.
          cy.then(() => Cypress.automation('remote:debugger:protocol', {
            command: 'Network.setCacheDisabled',
            params: { cacheDisabled: true },
          }));
        }
      });
      afterEach(() => {
        // Do not leave intercepted responses pending if an assertion fails before their normal release.
        releaseInitialUserInfo?.();
        releaseConfig?.();
        if (Cypress.browser.family === 'chromium') {
          cy.then(() => Cypress.automation('remote:debugger:protocol', {
            command: 'Network.setCacheDisabled',
            params: { cacheDisabled: false },
          }));
        }
      });

      [200, 401].forEach((statusCode) => {
        [true, false].forEach((duringRecovery) => {
          it(`ignores superseded startup authentication returning ${statusCode} ${duringRecovery ? 'during' : 'after'} replacement-token recovery`, () => {
            let initialUserInfoReceived = false;
            let configReceived = false;
            const initialUserInfoGate = new Cypress.Promise((resolve) => { releaseInitialUserInfo = resolve; });
            const configGate = new Cypress.Promise((resolve) => { releaseConfig = resolve; });

            cy.intercept({ method: 'GET', url: '**/app/userInfo', times: 1 }, (req) => {
              req.continue((res) => {
                if (statusCode === 401) {
                  res.statusCode = 401;
                  res.body = { explanation: 'Authentication token has expired' };
                }
                initialUserInfoReceived = true;
                return initialUserInfoGate;
              });
            }).as('initialUserInfo');
            if (duringRecovery) {
              cy.intercept({ method: 'GET', url: '**/public/clientDisplay/config*', times: 1 }, (req) => {
                req.continue(() => {
                  configReceived = true;
                  return configGate;
                });
              });
            }
            cy.visit('/__e2e/skills-display-parent?manualRecovery=true&allowManualRefresh=true');
            cy.wrap(null).should(() => {
              expect(initialUserInfoReceived, 'initial user-information response is held').to.be.true;
            });
            // Register after the initial response is held, so this alias observes only replacement requests.
            cy.intercept('GET', '**/app/userInfo').as('replacementUserInfo');
            // The second token starts recovery before the iframe has ever stored a user.
            cy.get(refreshAuthentication).should('be.visible').click();
            cy.wait('@replacementUserInfo').its('response.statusCode').should('eq', 200);
            if (duringRecovery) {
              cy.wrap(null).should(() => {
                expect(configReceived, 'replacement identity loaded and configuration response is held').to.be.true;
              });
            } else {
              assertHome();
            }
            cy.then(() => releaseInitialUserInfo());
            // Neither the stale 200's cancellation nor the stale 401's AxiosError may become an initialization error.
            cy.wait('@initialUserInfo').its('response.statusCode').should('eq', statusCode);
            cy.then(() => releaseConfig());
            assertHome();
            cy.wrapIframe().find('[data-cy="skillsDisplayInitError"]').should('not.exist');
            cy.wrapIframe().find('[data-cy="subjectTile-subj1"] [data-cy="subjectTileBtn"]').click();
            assertRecoveredSubject();
            cy.wrapIframe().find('[data-cy="skillsDisplayInitError"]').should('not.exist');
          });
        });
      });
    });

    [
      { name: 'server error', response: { statusCode: 500, body: {} } },
      { name: 'network error', response: { forceNetworkError: true } },
    ].forEach(({ name, response }) => {
      it(`shows a visible startup error without learner content when user information fails with a ${name}`, () => {
        let failRequests = true;
        // Keep network failures active across automatic browser retries.
        cy.intercept('GET', '**/app/userInfo', (req) => {
          if (failRequests) return req.reply(response);
          return req.continue();
        });
        cy.visit('/__e2e/skills-display-parent?manualRecovery=true');
        cy.wrapIframe().find('[data-cy="skillsDisplayInitError"]').should('be.visible')
          .and('have.attr', 'role', 'alert').and('contain.text', 'Unable to load Skills Display');
        cy.wrapIframe().find('[data-pc-name="progressspinner"]').should('not.exist');
        cy.wrapIframe().find('[data-cy="skillsDisplayHome"]').should('not.exist');
        cy.then(() => { failRequests = false; });
        cy.reload();
        assertHome();
      });
    });
  });

  describe('Recovery', () => {
    beforeEach(() => {
      cy.visit('/__e2e/skills-display-parent?manualRecovery=true');
      assertHome();
    });

    [true, false].forEach((duringRecovery) => {
      it(`ignores an expired-token 401 arriving ${duringRecovery ? 'during' : 'after'} replacement-token recovery`, () => {
        let releaseFirstFailure;
        let releaseLateFailure;
        let releaseConfig;
        let rankRequested = false;
        let historyRequested = false;
        let configReceived = false;
        const firstFailureGate = new Cypress.Promise((resolve) => { releaseFirstFailure = resolve; });
        const lateFailureGate = new Cypress.Promise((resolve) => { releaseLateFailure = resolve; });
        const configGate = new Cypress.Promise((resolve) => { releaseConfig = resolve; });

        // Hold both requests until they have been issued with the same expired token.
        cy.intercept('GET', '**/api/projects/proj1/subjects/subj1/rank*', (req) => {
          if (rankRequested) return req.continue();
          rankRequested = true;
          return firstFailureGate.then(() => req.reply({ statusCode: 401, body: {} }));
        });
        cy.intercept('GET', '**/api/projects/proj1/subjects/subj1/pointHistory*', (req) => {
          // A remounted chart must be able to load fresh history while the old request is held.
          if (historyRequested) return req.continue();
          historyRequested = true;
          req.alias = 'lateExpiredTokenFailure';
          return lateFailureGate.then(() => req.reply({ statusCode: 401, body: {} }));
        });
        cy.wrapIframe().find('[data-cy="subjectTile-subj1"] [data-cy="subjectTileBtn"]').click();
        cy.wrap(null).should(() => {
          expect(rankRequested, 'ranking request has started').to.be.true;
          expect(historyRequested, 'point-history request has started').to.be.true;
        });
        cy.then(() => releaseFirstFailure());
        cy.wrapIframe().find(authenticationError).should('be.visible');
        cy.get(refreshAuthentication).should('be.visible');

        if (duringRecovery) {
          cy.intercept({ method: 'GET', url: '**/public/clientDisplay/config*', times: 1 }, (req) => {
            req.continue(() => {
              // Fetching user information has succeeded, but recovery is not yet complete.
              configReceived = true;
              return configGate;
            });
          });
        }
        cy.get(refreshAuthentication).click();
        if (duringRecovery) {
          cy.wrap(null).should(() => { expect(configReceived, 'replacement configuration response is held').to.be.true; });
        } else {
          // Authentication has recovered once the subject is visible and its error has gone.
          // Do not wait for every chart spinner before releasing the held history response.
          assertRecoveredSubjectContent();
        }
        cy.then(() => releaseLateFailure());
        // Synchronize with the intentionally late response before allowing recovery to finish.
        cy.wait('@lateExpiredTokenFailure').its('response.statusCode').should('eq', 401);
        cy.then(() => releaseConfig());
        assertRecoveredSubject();
        // A further user action verifies the replacement authentication remains usable.
        cy.wrapIframe().find('[data-cy="back"]').should('be.visible').click();
        assertHome();
      });
    });

    ['SecurityError', 'QuotaExceededError'].forEach((storageError) => {
      it(`recovers without persisting user information when browser storage throws ${storageError}`, () => {
        cy.wrapIframe().then((body) => {
          const iframeWindow = Cypress.$(body)[0].ownerDocument.defaultView;
          const originalSetItem = iframeWindow.Storage.prototype.setItem;
          cy.stub(iframeWindow.Storage.prototype, 'setItem').callsFake(function (key, value) {
            if (key === 'userInfo') throw new iframeWindow.DOMException('Storage unavailable', storageError);
            return originalSetItem.call(this, key, value);
          });
          if (storageError === 'SecurityError') {
            cy.stub(iframeWindow.Storage.prototype, 'removeItem').callsFake(() => {
              throw new iframeWindow.DOMException('Storage unavailable', storageError);
            });
          }
        });
        expireAuthentication();
        cy.get(refreshAuthentication).click();
        assertRecoveredSubject();
      });
    });

    [
      { name: 'user information', url: '**/app/userInfo' },
      { name: 'display configuration', url: '**/public/clientDisplay/config*' },
    ].forEach(({ name, url }) => {
      [
        { failure: '401', response: { statusCode: 401, body: {} } },
        { failure: '500', response: { statusCode: 500, body: {} } },
        { failure: 'network error', response: { forceNetworkError: true } },
      ].forEach(({ failure, response }) => {
        it(`keeps an explicit error when restoring ${name} fails with ${failure} and restores the same route with another token`, () => {
          expireAuthentication();
          let failRequests = true;
          cy.intercept('GET', url, (req) => {
            if (failRequests) return req.reply(response);
            return req.continue();
          });
          cy.get(refreshAuthentication).click();
          cy.wrapIframe().find(authenticationError).should('be.visible').and('contain.text', recoveryFailedMessage);
          cy.wrapIframe().find('[data-pc-name="progressspinner"]').should('not.exist');
          cy.wrapIframe().find('[data-cy="skillsDisplayHome"]').should('not.exist');
          cy.then(() => { failRequests = false; });
          cy.get(refreshAuthentication).click();
          assertRecoveredSubject();
        });
      });
    });

    it('restores the display when the hosting page is refreshed after failed re-authentication', () => {
      expireAuthentication();
      cy.intercept({ method: 'GET', url: '**/app/userInfo', times: 1 }, { statusCode: 401, body: {} });
      cy.get(refreshAuthentication).click();
      cy.wrapIframe().find(authenticationError).should('be.visible').and('contain.text', recoveryFailedMessage);
      cy.reload();
      assertHome();
    });
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
      for (let cycle = 0; cycle < 3; cycle += 1) {
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
