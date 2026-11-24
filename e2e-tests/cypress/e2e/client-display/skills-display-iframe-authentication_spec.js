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

import { createCompactViewProject } from '../../support/compact-view';
import {
  authenticationError, refreshAuthentication, useIframeParentFixture,
  assertHome, assertHeightFitsDisplay, expireAuthentication,
  assertRecoveredSubjectContent, assertRecoveredSubject,
} from '../../support/iframe-tests';

// Cypress.env changes are local to this spec; browser test isolation stays enabled.
Cypress.env('iframeTestSetup', 'shared');

describe('Embedded Skills Display authentication recovery', () => {
  const recoveryFailedMessage = 'Unable to reconnect your session. Please refresh the page to try again.';
  useIframeParentFixture();
  before(() => {
    // These cases only vary HTTP responses; none changes the baseline skill or progress.
    cy.resetDb();
    cy.loginAsAdminUser();
    createCompactViewProject({ compact: false });
  });
  beforeEach(() => {
    // Some display data loaders propagate the expected rejected Axios request.
    // Do not suppress unrelated exceptions.
    cy.on('uncaught:exception', (error) => {
      if (error.name === 'AxiosError' && error.message === 'Request failed with status code 401') return false;
      if (error.name === 'CanceledError' && error.message === 'Skills Display request was superseded') return false;
    });
  });

  describe('Real SkillsDisplayJS authentication', () => {
    let releaseToken;
    beforeEach(() => { releaseToken = null; });
    afterEach(() => releaseToken?.());

    it('restores an expired session and explains recovery when history-disabled navigation replays the initial page', () => {
      cy.visit('/test-skills-client/proj1?updateHistory=false&skillsClientDisplayPath=/');
      assertHome();
      cy.wrapIframe().find('[data-cy="skillsDisplaySessionRestored"]').should('not.exist');
      cy.wrapIframe().find('[data-cy="subjectTile-subj1"] [data-cy="subjectTileBtn"]').click();
      assertRecoveredSubject();
      cy.location('search').should('include', 'skillsClientDisplayPath=/');

      // Expire credentials on a user-driven request after reaching the subject.
      // Hold token renewal until the expiration UI proves recovery was requested.
      const tokenGate = new Cypress.Promise((resolve) => { releaseToken = resolve; });
      cy.intercept('GET', '**/api/projects/proj1/token', (req) => {
        req.continue(() => tokenGate);
      });
      cy.intercept({ method: 'GET', url: '**/api/projects/proj1/subjects/subj1/skills/skill1/summary*', times: 1 }, {
        statusCode: 401, body: { explanation: 'Authentication token has expired' },
      });
      cy.wrapIframe().find('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').click();
      cy.wrapIframe().find(authenticationError).should('be.visible');
      cy.then(() => releaseToken());
      // Accepted SDK 3.6.5 limitation: renewal replays the stale host deep link.
      assertHome();
      cy.wrapIframe().find('[data-cy="skillsDisplaySessionRestored"]').should('be.visible')
        .and('contain.text', 'Your session was restored.');
      cy.wrapIframe().find('[data-cy="skillsDisplaySessionRestored"] [role="status"]')
        .should('have.attr', 'aria-live', 'polite');
      cy.wrapIframe().find('[data-cy="subjectTile-subj1"] [data-cy="subjectTileBtn"]').click();
      assertRecoveredSubject();
      cy.wrapIframe().find('[data-cy="skillsDisplaySessionRestored"]').should('be.visible');
      cy.wrapIframe().find('[data-cy="dismissSkillsDisplaySessionRestored"]').click();
      cy.wrapIframe().find('[data-cy="skillsDisplaySessionRestored"]').should('not.exist');
    });

    it('honors the initial deep link with history disabled without announcing recovery', () => {
      cy.visit('/test-skills-client/proj1?updateHistory=false&skillsClientDisplayPath=/subjects/subj1');
      assertRecoveredSubject();
      cy.wrapIframe().find('[data-cy="skillsDisplaySessionRestored"]').should('not.exist');
    });
  });

  describe('Startup', () => {
    let releaseStartup;
    beforeEach(() => { releaseStartup = null; });
    afterEach(() => releaseStartup?.());

    it('loads each required embedded resource once per startup or recovery', () => {
      cy.intercept('GET', '**/app/userInfo').as('embeddedUserInfo');
      cy.intercept('GET', '**/public/clientDisplay/config*').as('embeddedDisplayConfig');
      cy.visit('/__e2e/skills-display-parent?manualRecovery=true');
      assertHome();
      // Rendering establishes readiness; request counts verify that App and the
      // iframe initializer did not both launch the embedded loading sequence.
      cy.get('@embeddedUserInfo.all').should('have.length', 1);
      cy.get('@embeddedDisplayConfig.all').should('have.length', 1);
      expireAuthentication();
      cy.get(refreshAuthentication).click();
      assertRecoveredSubject();
      cy.get('@embeddedUserInfo.all').should('have.length', 2);
      cy.get('@embeddedDisplayConfig.all').should('have.length', 2);
      cy.wrapIframe().find('[data-cy="skillsDisplayInitError"]').should('not.exist');
    });

    it('sizes the host for the loading screen before startup authentication completes', () => {
      let userInfoReceived = false;
      const startupGate = new Cypress.Promise((resolve) => { releaseStartup = resolve; });
      cy.intercept({ method: 'GET', url: '**/app/userInfo', times: 1 }, (req) => {
        req.continue(() => {
          userInfoReceived = true;
          return startupGate;
        });
      });
      cy.visit('/__e2e/skills-display-parent?autoHeight=true&minHeight=100px');
      cy.wrap(null).should(() => { expect(userInfoReceived, 'startup identity response is held').to.be.true; });
      cy.wrapIframe().find('[data-pc-name="progressspinner"]').should('be.visible');
      cy.wrapIframe().find('[data-cy="skillsDisplayHome"]').should('not.exist');
      assertHeightFitsDisplay(100);
      cy.then(() => releaseStartup());
      assertHome();
      assertHeightFitsDisplay(100);
    });

    [
      { loader: 'public configuration', url: '**/public/config' },
      { loader: 'user information', url: '**/app/userInfo' },
      { loader: 'display configuration', url: '**/public/clientDisplay/config*' },
    ].forEach(({ loader, url }) => {
      it(`requests a replacement token and initializes the display when initial ${loader} returns 401`, () => {
        cy.intercept({ method: 'GET', url, times: 1 }, { statusCode: 401, body: {} });
        // A new iframe has never populated its user information.
        // A small configured minimum makes stale loading-screen heights detectable.
        cy.visit('/__e2e/skills-display-parent?manualRecovery=true&autoHeight=true&minHeight=100px');
        cy.wrapIframe().find(authenticationError).should('be.visible').and('contain.text', 'authentication has expired');
        cy.wrapIframe().find('[data-pc-name="progressspinner"]').should('not.exist');
        assertHeightFitsDisplay(100);
        cy.get(refreshAuthentication).should('be.visible').click();
        assertHome();
        assertHeightFitsDisplay(100);
        cy.wrapIframe().find('[data-cy="subjectTile-subj1"] [data-cy="subjectTileBtn"]').click();
        assertRecoveredSubject();
        assertHeightFitsDisplay(100);
      });
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

      it('does not reuse pending startup configuration when a replacement token belongs to the same learner', () => {
        let configReceived = false;
        const configGate = new Cypress.Promise((resolve) => { releaseConfig = resolve; });
        cy.intercept({ method: 'GET', url: '**/public/clientDisplay/config*', times: 1 }, (req) => {
          req.continue(() => {
            configReceived = true;
            return configGate;
          });
        }).as('oldStartupConfig');
        cy.visit('/__e2e/skills-display-parent?manualRecovery=true&allowManualRefresh=true');
        cy.wrap(null).should(() => { expect(configReceived, 'startup configuration is held').to.be.true; });
        cy.get(refreshAuthentication).click();
        assertHome();
        cy.then(() => releaseConfig());
        cy.wait('@oldStartupConfig');
        assertHome();
        cy.wrapIframe().find('[data-cy="skillsDisplayInitError"]').should('not.exist');
      });

      it('replaces superseded public configuration before completing startup recovery', () => {
        let configReceived = false;
        const configGate = new Cypress.Promise((resolve) => { releaseConfig = resolve; });
        cy.intercept('GET', '**/public/config', (req) => {
          req.continue((res) => { res.body.enablePageVisitReporting = true; });
        });
        cy.intercept({ method: 'GET', url: '**/public/config', times: 1 }, (req) => {
          req.continue((res) => {
            res.body.enablePageVisitReporting = false;
            configReceived = true;
            return configGate;
          });
        }).as('oldPublicConfig');
        // Analytics is the observable public-config setting being exercised here.
        cy.intercept('PUT', '**/api/pageVisit').as('pageVisits');
        cy.visit('/__e2e/skills-display-parent?manualRecovery=true&allowManualRefresh=true');
        cy.wrap(null).should(() => { expect(configReceived, 'old public config is held').to.be.true; });
        cy.get(refreshAuthentication).click();
        assertHome();
        cy.then(() => releaseConfig());
        cy.wait('@oldPublicConfig');
        assertHome();
        cy.wrapIframe().find('[data-cy="subjectTile-subj1"] [data-cy="subjectTileBtn"]').click();
        assertRecoveredSubject();
        cy.get('@pageVisits.all').should((visits) => {
          expect(visits.some(({ request }) => request.body.skillDisplay === true
            && request.body.path === '/subjects/subj1'), 'replacement public configuration enables analytics').to.be.true;
        });
        cy.wrapIframe().find('[data-cy="skillsDisplayInitError"]').should('not.exist');
      });

      [false, true].forEach((rotateDuringStartup) => {
        it(`does not load dashboard-only settings during embedded startup ${rotateDuringStartup ? 'with' : 'without'} token rotation or subsequent recovery`, () => {
          let initialUserInfoReceived = false;
          const initialUserInfoGate = new Cypress.Promise((resolve) => { releaseInitialUserInfo = resolve; });
          cy.intercept('GET', '**/public/config', (req) => {
            req.continue((res) => { res.body.enableOpenAIIntegration = true; });
          });
          cy.intercept('GET', '**/app/userInfo/hasRole/ROLE_SUPER_DUPER_USER', { statusCode: 500, body: {} }).as('dashboardRole');
          cy.intercept('GET', '**/public/isFeatureSupported?feature=emailservice', { statusCode: 500, body: {} }).as('dashboardEmail');
          cy.intercept('GET', '**/openai/getAiPromptSettings', { statusCode: 500, body: {} }).as('dashboardAi');
          cy.intercept({ method: 'GET', url: '**/app/userInfo', times: 1 }, (req) => {
            req.continue(() => {
              initialUserInfoReceived = true;
              return initialUserInfoGate;
            });
          }).as('heldInitialUserInfo');
          cy.visit('/__e2e/skills-display-parent?manualRecovery=true&allowManualRefresh=true');
          cy.wrap(null).should(() => { expect(initialUserInfoReceived, 'initial identity is held').to.be.true; });
          if (rotateDuringStartup) {
            cy.get(refreshAuthentication).click();
            assertHome();
          }
          cy.then(() => releaseInitialUserInfo());
          cy.wait('@heldInitialUserInfo');
          assertHome();
          expireAuthentication();
          cy.get(refreshAuthentication).click();
          assertRecoveredSubject();
          // Request absence is the behavior under test; rendered recovery establishes completion.
          cy.get('@dashboardRole.all').should('have.length', 0);
          cy.get('@dashboardEmail.all').should('have.length', 0);
          cy.get('@dashboardAi.all').should('have.length', 0);
        });
      });

      [
        { failure: '500', response: { statusCode: 500, body: {} } },
        { failure: 'network error', response: { forceNetworkError: true } },
      ].forEach(({ failure, response }) => {
        it(`completes startup recovery without waiting for optional custom icons, including a late ${failure}`, () => {
          let initialUserInfoReceived = false;
          let optionalRequested = false;
          const initialUserInfoGate = new Cypress.Promise((resolve) => { releaseInitialUserInfo = resolve; });
          const optionalGate = new Cypress.Promise((resolve) => { releaseConfig = resolve; });
          cy.intercept({ method: 'GET', url: '**/app/userInfo', times: 1 }, (req) => {
            req.continue(() => {
              initialUserInfoReceived = true;
              return initialUserInfoGate;
            });
          });
          cy.intercept('GET', '**/api/projects/proj1/customIconCss', (req) => {
            optionalRequested = true;
            return optionalGate.then(() => req.reply(response));
          }).as('optionalStartupIcons');
          cy.visit('/__e2e/skills-display-parent?manualRecovery=true&allowManualRefresh=true');
          cy.wrap(null).should(() => { expect(initialUserInfoReceived, 'initial identity is held').to.be.true; });
          cy.get(refreshAuthentication).click();
          cy.wrap(null).should(() => { expect(optionalRequested, 'optional request is held').to.be.true; });
          // Required loaders finish while the optional request is still pending.
          assertHome();
          cy.then(() => releaseConfig());
          cy.wait('@optionalStartupIcons');
          assertHome();
          cy.wrapIframe().find('[data-cy="errorPage"]').should('not.exist');
          cy.wrapIframe().find('[data-cy="skillsDisplayInitError"]').should('not.exist');
          cy.then(() => releaseInitialUserInfo());
        });
      });

      // Stale responses are rejected before status-specific handling. Cover both
      // success and authentication failure at each recovery lifecycle boundary.
      [200, 401].forEach((statusCode) => {
        [true, false].forEach((duringRecovery) => {
          it(`ignores superseded startup authentication returning ${statusCode} ${duringRecovery ? 'during' : 'after'} replacement-token recovery`, () => {
            let initialUserInfoReceived = false;
            let configReceived = false;
            const initialUserInfoGate = new Cypress.Promise((resolve) => { releaseInitialUserInfo = resolve; });
            const configGate = new Cypress.Promise((resolve) => { releaseConfig = resolve; });

            cy.intercept({ method: 'GET', url: '**/app/userInfo', times: 1 }, (req) => {
              if (statusCode === 401) {
                initialUserInfoReceived = true;
                // Inject a real transport failure, not modified metadata on a
                // forwarded success, so the child actually exercises stale 401 handling.
                return initialUserInfoGate.then(() => req.reply({
                  statusCode: 401,
                  body: { explanation: 'Superseded authentication request' },
                }));
              }
              req.continue(() => {
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
              expect(initialUserInfoReceived, 'initial user-information request is held').to.be.true;
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
      { loader: 'public configuration', url: '**/public/config' },
      { loader: 'user information', url: '**/app/userInfo' },
      { loader: 'display configuration', url: '**/public/clientDisplay/config*' },
    ].forEach(({ loader, url }) => {
      [
        { name: 'server error', response: { statusCode: 500, body: {} } },
        { name: 'network error', response: { forceNetworkError: true } },
      ].forEach(({ name, response }) => {
        it(`shows a visible startup error without learner content when ${loader} fails with a ${name}`, () => {
          let failRequests = true;
          // Keep network failures active across automatic browser retries.
          cy.intercept('GET', url, (req) => {
            if (failRequests) return req.reply(response);
            return req.continue();
          });
          cy.visit('/__e2e/skills-display-parent?manualRecovery=true&autoHeight=true&minHeight=100px');
          cy.wrapIframe().find('[data-cy="skillsDisplayInitError"]').should('be.visible')
            .and('have.attr', 'role', 'alert').and('contain.text', 'Unable to load Skills Display');
          cy.wrapIframe().find('[data-pc-name="progressspinner"]').should('not.exist');
          cy.wrapIframe().find('[data-cy="skillsDisplayHome"]').should('not.exist');
          assertHeightFitsDisplay(100);
          cy.then(() => { failRequests = false; });
          cy.reload();
          assertHome();
          assertHeightFitsDisplay(100);
        });
      });
    });
  });

  describe('Recovery', () => {
    let pendingReleases;
    let pendingCleanups;
    beforeEach(() => {
      pendingReleases = [];
      pendingCleanups = [];
      cy.visit('/__e2e/skills-display-parent?manualRecovery=true');
      assertHome();
    });
    afterEach(() => {
      pendingReleases.forEach((release) => release());
      pendingCleanups.forEach((cleanup) => cleanup());
    });

    // A stale 401 must not expire replacement credentials during or after recovery.
    // One server failure and one transport failure cover the remaining error shapes.
    [
      { name: '401', response: { statusCode: 401, body: {} }, duringRecovery: true },
      { name: '401', response: { statusCode: 401, body: {} }, duringRecovery: false },
      { name: '500', response: { statusCode: 500, body: {} }, duringRecovery: true },
      { name: 'network failure', response: { forceNetworkError: true }, duringRecovery: false },
    ].forEach(({ name, response, duringRecovery }) => {
      it(`ignores an expired-token ${name} arriving ${duringRecovery ? 'during' : 'after'} replacement-token recovery`, () => {
        cy.on('uncaught:exception', (error) => {
          const expectedMessage = response.forceNetworkError ? 'Network Error' : `Request failed with status code ${response.statusCode}`;
          if (error.name === 'AxiosError' && error.message === expectedMessage
            && error.config?.url?.includes('/subjects/subj1/pointHistory')) {
            return false;
          }
        });
        let releaseFirstFailure;
        let releaseLateFailure;
        let releaseConfig;
        let rankRequested = false;
        let historyRequested = false;
        let oldAuthorization;
        let configReceived = false;
        let observeLateFailure;
        const lateFailureObserved = new Cypress.Promise((resolve) => { observeLateFailure = resolve; });
        const firstFailureGate = new Cypress.Promise((resolve) => { releaseFirstFailure = resolve; });
        const lateFailureGate = new Cypress.Promise((resolve) => { releaseLateFailure = resolve; });
        const configGate = new Cypress.Promise((resolve) => { releaseConfig = resolve; });
        pendingReleases.push(releaseFirstFailure, releaseLateFailure, releaseConfig);

        cy.wrapIframe().then((body) => {
          const iframeWindow = Cypress.$(body)[0].ownerDocument.defaultView;
          const observeRejection = (event) => {
            const error = event.reason;
            const authorization = error?.config?.headers?.get?.('Authorization') || error?.config?.headers?.Authorization;
            const expectedFailure = response.forceNetworkError
              ? error?.code === 'ERR_NETWORK'
              : error?.response?.status === response.statusCode;
            if (error?.name === 'AxiosError' && expectedFailure
              && error.config?.url?.includes('/subjects/subj1/pointHistory')
              && oldAuthorization && authorization === oldAuthorization) {
              // The original rejection lives in the child window, not Cypress's
              // AUT parent. This fires after Axios and the loader reject, including
              // any browser retries, rather than after just one intercepted attempt.
              event.preventDefault();
              observeLateFailure();
            }
          };
          iframeWindow.addEventListener('unhandledrejection', observeRejection);
          pendingCleanups.push(() => iframeWindow.removeEventListener('unhandledrejection', observeRejection));
        });

        // Hold both requests until they have been issued with the same expired token.
        cy.intercept('GET', '**/api/projects/proj1/subjects/subj1/rank*', (req) => {
          if (rankRequested) return req.continue();
          rankRequested = true;
          return firstFailureGate.then(() => req.reply({ statusCode: 401, body: {} }));
        });
        cy.intercept('GET', '**/api/projects/proj1/subjects/subj1/pointHistory*', (req) => {
          // A remounted chart must be able to load fresh history while the old request is held.
          // Reject browser retries of the OLD token too, without rejecting replacement requests.
          if (historyRequested && (!response.forceNetworkError || req.headers.authorization !== oldAuthorization)) return req.continue();
          historyRequested = true;
          oldAuthorization = req.headers.authorization;
          req.alias = 'lateExpiredTokenFailure';
          return lateFailureGate.then(() => req.reply(response));
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
          cy.wrapIframe().find(authenticationError).should('be.visible');
        } else {
          // Authentication has recovered once the subject is visible and its error has gone.
          // Do not wait for every chart spinner before releasing the held history response.
          assertRecoveredSubjectContent();
        }
        cy.then(() => releaseLateFailure());
        // Synchronize with the intentionally late response before allowing recovery to finish.
        if (response.forceNetworkError) {
          cy.wait('@lateExpiredTokenFailure').should('have.property', 'error');
        } else {
          cy.wait('@lateExpiredTokenFailure').its('response.statusCode').should('eq', response.statusCode);
        }
        // Synchronize with the child's completed rejection, not Cypress's error
        // hook or the first proxy attempt. Use the transport timeout for retries.
        cy.then({ timeout: Cypress.config('responseTimeout') }, () => lateFailureObserved);
        if (duringRecovery) cy.wrapIframe().find(authenticationError).should('be.visible');
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

    it('keeps a current private-project denial inside the iframe error page', () => {
      cy.on('uncaught:exception', (error) => {
        if (error.name === 'AxiosError' && error.message === 'Request failed with status code 403') return false;
      });
      cy.intercept('GET', '**/api/projects/proj1/subjects/subj1/summary*', {
        statusCode: 403,
        body: { errorCode: 'private_project', projectId: 'proj1', explanation: 'This learner cannot access the private project' },
      });
      cy.wrapIframe().find('[data-cy="subjectTile-subj1"] [data-cy="subjectTileBtn"]').click();
      cy.wrapIframe().find('[data-cy="errorPage"]').should('be.visible').and('contain.text', 'User Not Authorized');
      cy.wrapIframe().find('[data-cy="errExplanation"]').should('have.text', 'This learner cannot access the private project');
      cy.wrapIframe().find(authenticationError).should('not.exist');
    });

    [
      { name: '404', response: { statusCode: 404, body: {} }, title: 'Resource Not Found' },
      { name: '500', response: { statusCode: 500, body: {} }, title: 'Tiny-bit of an error!' },
      { name: 'network error', response: { forceNetworkError: true }, title: 'Tiny-bit of an error!' },
      { name: 'upgrade response', response: { statusCode: 503, body: { errorCode: 'DbUpgradeInProgress' } }, title: 'Upgrade in Progress' },
    ].forEach(({ name, response, title }) => {
      it(`handles a current-token ${name} normally after recovery`, () => {
        expireAuthentication();
        cy.get(refreshAuthentication).click();
        assertRecoveredSubject();
        cy.wrapIframe().find('[data-cy="back"]').click();
        assertHome();
        cy.on('uncaught:exception', (error) => {
          const expectedMessage = response.forceNetworkError ? 'Network Error' : `Request failed with status code ${response.statusCode}`;
          if (error.name === 'AxiosError' && error.message === expectedMessage
            && error.config?.url?.includes('/subjects/subj1/summary')) return false;
        });
        // Keep network-error responses active through browser retries.
        cy.intercept('GET', '**/api/projects/proj1/subjects/subj1/summary*', response);
        cy.wrapIframe().find('[data-cy="subjectTile-subj1"] [data-cy="subjectTileBtn"]').click();
        cy.wrapIframe().find('[data-cy="errorPage"]').should('be.visible').and('contain.text', title);
        cy.wrapIframe().find(authenticationError).should('not.exist');
      });
    });
  });
});
