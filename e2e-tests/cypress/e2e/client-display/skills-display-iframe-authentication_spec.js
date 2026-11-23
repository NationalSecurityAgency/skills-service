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
  const assertHeightFitsDisplay = (minHeight = 960) => cy.get('#skills-client-container iframe').should(($iframe) => {
    const frame = $iframe[0];
    const root = frame.contentDocument.querySelector('#skilltree-main-container');
    expect(root, 'persistent iframe content root').not.to.be.null;
    const expectedHeight = Math.max(root.getBoundingClientRect().height + 10, minHeight);
    expect(frame.getBoundingClientRect().height, 'iframe fits its current content').to.be.closeTo(expectedHeight, 1);
    expect(frame.parentElement.getBoundingClientRect().height, 'host container fits its current content').to.be.closeTo(expectedHeight, 1);
  });

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
      if (error.name === 'CanceledError' && error.message === 'Skills Display request was superseded') return false;
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
    let releaseStartup;
    beforeEach(() => { releaseStartup = null; });
    afterEach(() => releaseStartup?.());

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

    it('requests a replacement token and initializes the display when the first user-information request returns 401', () => {
      cy.intercept({ method: 'GET', url: '**/app/userInfo', times: 1 }, { statusCode: 401, body: {} });
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

      [200, 401, 403, 404, 500].forEach((statusCode) => {
        [true, false].forEach((duringRecovery) => {
          it(`ignores superseded startup authentication returning ${statusCode} ${duringRecovery ? 'during' : 'after'} replacement-token recovery`, () => {
            let initialUserInfoReceived = false;
            let configReceived = false;
            const initialUserInfoGate = new Cypress.Promise((resolve) => { releaseInitialUserInfo = resolve; });
            const configGate = new Cypress.Promise((resolve) => { releaseConfig = resolve; });

            cy.intercept({ method: 'GET', url: '**/app/userInfo', times: 1 }, (req) => {
              req.continue((res) => {
                if (statusCode !== 200) {
                  res.statusCode = statusCode;
                  res.body = { explanation: 'Superseded authentication request' };
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

    [
      ...[401, 403, 404, 500].map((statusCode) => ({ name: String(statusCode), response: { statusCode, body: {} } })),
      { name: 'network failure', response: { forceNetworkError: true } },
    ].flatMap((failure) => [true, false].map((duringRecovery) => ({ ...failure, duringRecovery })))
      .forEach(({ name, response, duringRecovery }) => {
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

  [
    { initialUserId: 'LearnerA@skills.org', replacementUserId: 'learnera@skills.org' },
    { initialUserId: 'learnera@skills.org', replacementUserId: 'LearnerA@skills.org' },
  ].forEach(({ initialUserId, replacementUserId }) => {
    it(`recovers in the same iframe when proxy learner casing changes from ${initialUserId} to ${replacementUserId}`, () => {
      let tokenForDisplay;
      let replacementToken;
      let iframeDocument;
      // Use service-issued proxy credentials without creating a persisted account,
      // so /app/userInfo exposes the raw proxy_user casing on each authentication.
      cy.request(`/admin/projects/proj1/token/${initialUserId}`).then(({ body }) => { tokenForDisplay = body; });
      cy.request(`/admin/projects/proj1/token/${replacementUserId}`).then(({ body }) => { replacementToken = body; });
      cy.intercept('GET', '**/api/projects/proj1/token', (req) => req.reply(tokenForDisplay));
      cy.visit('/__e2e/skills-display-parent?manualRecovery=true');
      assertHome();
      cy.get('#skills-client-container iframe').then(($iframe) => { iframeDocument = $iframe[0].contentDocument; });
      expireAuthentication();
      cy.then(() => { tokenForDisplay = replacementToken; });
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
