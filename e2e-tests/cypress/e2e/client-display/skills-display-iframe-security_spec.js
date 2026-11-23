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

import { useIframeParentFixture } from '../../support/iframe-tests';

Cypress.env('iframeTestSetup', 'isolated');

describe('Skills Display iframe destination security', () => {
  useIframeParentFixture();
  let cleanupSibling;
  let parentMessageSequence = 0;
  const sendParentMessages = (messages, { expectRejected = false } = {}) => cy.window().then((parentWindow) => {
    const displayWindow = parentWindow.document.querySelector('#skills-client-container iframe').contentWindow;
    const type = 'application/x-postmate-v1+json';
    const uid = `parent-message-fence-${++parentMessageSequence}`;
    const fencedMessages = [
      ...messages.map((message) => ({ type, ...message })),
      { postmate: 'request', type, property: 'serviceUrl', uid },
    ];
    const postMessages = () => {
      // Execute in the actual embedding window, not the Cypress runner.
      const script = parentWindow.document.createElement('script');
      script.textContent = `${JSON.stringify(fencedMessages)}.forEach(message => document.querySelector('#skills-client-container iframe').contentWindow.postMessage(message, window.location.origin));`;
      parentWindow.document.body.appendChild(script);
      script.remove();
    };
    if (expectRejected) {
      // A fatal identity violation rejects ALL Postmate messages, including the
      // fence request. Observe its real rejection without weakening the guard or
      // installing a later message listener that stopImmediatePropagation hides.
      const rejectedMessages = Cypress.sinon.spy(displayWindow.Event.prototype, 'stopImmediatePropagation');
      cleanupSibling = () => rejectedMessages.restore();
      postMessages();
      return cy.wrap(rejectedMessages, { log: false }).should((spy) => {
        const fenceRejected = spy.getCalls().some(({ thisValue: event }) => event.type === 'message'
          && event.source === parentWindow && event.data?.postmate === 'request' && event.data.uid === uid);
        expect(fenceRejected, 'fatal iframe rejected the fence after processing the parent calls').to.be.true;
      }).then(() => {
        cleanupSibling();
        cleanupSibling = null;
      });
    }
    return new Cypress.Promise((resolve) => {
      const receiveFence = (event) => {
        if (event.source === displayWindow && event.data?.postmate === 'reply' && event.data.uid === uid) {
          cleanupSibling();
          cleanupSibling = null;
          resolve();
        }
      };
      cleanupSibling = () => parentWindow.removeEventListener('message', receiveFence);
      parentWindow.addEventListener('message', receiveFence);
      postMessages();
    });
  });
  beforeEach(() => {
    cleanupSibling = null;
    cy.createProject(1);
    cy.createSubject(1, 1);
    cy.createSkill(1, 1, 1);
  });
  afterEach(() => cleanupSibling?.());

  it('renders the project after the real Postmate authentication handshake', () => {
    cy.intercept('GET', '**/api/projects/proj1/summary*').as('summary');
    cy.visit('/__e2e/skills-display-parent');

    cy.wrapIframe().find('[data-cy="skillsTitle"]').should('be.visible').and('contain.text', 'User Skills');
    cy.wrapIframe().find('[data-cy="numTotalSkills"]').should('have.text', '1');
    cy.wrapIframe().find('[data-cy="subjectTile-subj1"]').should('be.visible');
    cy.wrapIframe().find('[data-cy="skillsDisplayInitError"]').should('not.exist');
    // Rendering alone could succeed with session cookies; verify bearer authentication too.
    cy.wait('@summary').its('request.headers.authorization').should('match', /^Bearer .+/);
  });

  it('honors an initial numeric version of zero instead of including newer skills', () => {
    cy.createSkill(1, 1, 2, { version: 1 });
    const modelOverrides = encodeURIComponent(JSON.stringify({ version: 0 }));
    cy.visit(`/__e2e/skills-display-parent?modelOverrides=${modelOverrides}`);
    cy.wrapIframe().find('[data-cy="numTotalSkills"]').should('have.text', '1');
    cy.wrapIframe().find('[data-cy="subjectTile-subj1"] [data-cy="subjectTileBtn"]').click();
    cy.wrapIframe().find('[data-cy="skillProgressTitle-skill1"]').should('be.visible');
    cy.wrapIframe().find('[data-cy="skillProgressTitle-skill2"]').should('not.exist');
  });

  it('refreshes numeric, string and null versions and ignores repeated values in the same iframe', () => {
    const assertUnchangedVersion = (version) => {
      // Separate aliases exclude the earlier, intentional refresh requests.
      const suffix = version === null ? 'AllVersions' : typeof version === 'number' ? 'NumericZero' : 'StringZero';
      const summaryAlias = `unchangedSummary${suffix}`;
      const initializationAlias = `unchangedInitialization${suffix}`;
      const authenticationAlias = `unchangedAuthentication${suffix}`;
      cy.intercept('GET', '**/api/projects/proj1/subjects/subj1/summary*').as(summaryAlias);
      cy.intercept('POST', '**/api/projects/proj1/progress/initialize').as(initializationAlias);
      cy.intercept('GET', '**/app/userInfo').as(authenticationAlias);
      let routeChangedCount;
      cy.get('[data-cy="routeChangedCount"]').invoke('text').then((text) => { routeChangedCount = text; });
      sendParentMessages([
        { postmate: 'call', property: 'updateVersion', data: version },
        { postmate: 'call', property: 'updateVersion', data: version },
      ]);
      cy.wrapIframe().find('[data-cy="skillsTitle"]').should('contain.text', 'Subject 1');
      cy.wrapIframe().find('[data-cy="skillProgressTitle-skill1"]').should('be.visible');
      cy.wrapIframe().find('[data-cy="skillProgressTitle-skill2"]').should(version === null ? 'be.visible' : 'not.exist');
      cy.get('[data-cy="routeChangedCount"]').should(($count) => {
        expect($count.text(), 'identical versions do not navigate').to.equal(routeChangedCount);
      });
      // Request absence is the behavior under test for unchanged values.
      cy.get(`@${summaryAlias}.all`).should('have.length', 0);
      cy.get(`@${initializationAlias}.all`).should('have.length', 0);
      cy.get(`@${authenticationAlias}.all`).should('have.length', 0);
    };
    cy.createSkill(1, 1, 2, { version: 1 });
    cy.createSkill(1, 1, 3, { version: 2 });
    const modelOverrides = encodeURIComponent(JSON.stringify({ version: '0' }));
    cy.visit(`/__e2e/skills-display-parent?modelOverrides=${modelOverrides}`);
    cy.wrapIframe().find('[data-cy="numTotalSkills"]').should('have.text', '1');
    cy.wrapIframe().find('[data-cy="totalPoints"]').should('have.text', '200');
    let iframeDocument;
    cy.get('#skills-client-container iframe').then(($iframe) => { iframeDocument = $iframe[0].contentDocument; });
    cy.intercept('GET', '**/api/projects/proj1/token').as('unexpectedTokenRequest');
    cy.wrapIframe().find('[data-cy="subjectTile-subj1"] [data-cy="subjectTileBtn"]').click();
    cy.wrapIframe().find('[data-cy="skillProgressTitle-skill1"]').should('be.visible');
    assertUnchangedVersion('0');
    cy.wrapIframe().find('[data-cy="back"]').click();
    cy.wrapIframe().find('[data-cy="numTotalSkills"]').should('have.text', '1');
    sendParentMessages([{ postmate: 'call', property: 'updateVersion', data: null }]);
    cy.wrapIframe().find('[data-cy="numTotalSkills"]').should('have.text', '3');
    cy.wrapIframe().find('[data-cy="totalPoints"]').should('have.text', '600');
    sendParentMessages([{ postmate: 'call', property: 'updateVersion', data: 0 }]);
    cy.wrapIframe().find('[data-cy="numTotalSkills"]').should('have.text', '1');
    cy.wrapIframe().find('[data-cy="totalPoints"]').should('have.text', '200');
    cy.wrapIframe().find('[data-cy="subjectTile-subj1"] [data-cy="subjectTileBtn"]').click();
    cy.wrapIframe().find('[data-cy="skillProgressTitle-skill1"]').should('be.visible');
    cy.wrapIframe().find('[data-cy="skillProgressTitle-skill2"]').should('not.exist');
    assertUnchangedVersion(0);
    let routeChangedCount;
    cy.get('[data-cy="routeChangedCount"]').invoke('text').then((text) => { routeChangedCount = text; });
    sendParentMessages([{ postmate: 'call', property: 'updateVersion', data: 1 }]);
    cy.wrapIframe().find('[data-cy="skillProgressTitle-skill2"]').should('be.visible');
    cy.wrapIframe().find('[data-cy="skillProgressTitle-skill3"]').should('not.exist');
    sendParentMessages([{ postmate: 'call', property: 'updateVersion', data: null }]);
    cy.wrapIframe().find('[data-cy="skillProgressTitle-skill3"]').should('be.visible');
    assertUnchangedVersion(null);
    sendParentMessages([{ postmate: 'call', property: 'updateVersion', data: '0' }]);
    cy.wrapIframe().find('[data-cy="totalPoints"]').should('have.text', '200');
    cy.wrapIframe().find('[data-cy="skillProgressTitle-skill2"]').should('not.exist');
    cy.wrapIframe().find('[data-cy="skillProgressTitle-skill3"]').should('not.exist');
    cy.wrapIframe().find('[data-cy="skillsTitle"]').should('contain.text', 'Subject 1');
    cy.get('[data-cy="routeChangedCount"]').should(($count) => {
      expect($count.text(), 'version refresh preserves the subject route').to.equal(routeChangedCount);
    });
    cy.get('#skills-client-container iframe').then(($iframe) => {
      expect($iframe[0].contentDocument, 'version refresh retains the iframe').to.equal(iframeDocument);
    });
    // Refresh verifies the learner with existing credentials, without asking
    // the parent to acquire a new token or recreating the iframe lifetime.
    cy.get('@unexpectedTokenRequest.all').should('have.length', 0);
    cy.wrapIframe().find('[data-cy="back"]').click();
    cy.wrapIframe().find('[data-cy="numTotalSkills"]').should('have.text', '1');
    sendParentMessages([{ postmate: 'call', property: 'updateVersion', data: null }]);
    cy.wrapIframe().find('[data-cy="numTotalSkills"]').should('have.text', '3');
    cy.wrapIframe().find('[data-cy="totalPoints"]').should('have.text', '600');
    cy.wrapIframe().find('[data-cy="skillsDisplayAuthenticationError"]').should('not.exist');
  });

  it('refreshes a still-valid skill detail route without navigating away', () => {
    cy.createSkill(1, 1, 2, { version: 1 });
    cy.visit('/__e2e/skills-display-parent');
    cy.wrapIframe().find('[data-cy="subjectTile-subj1"] [data-cy="subjectTileBtn"]').should('be.visible').click();
    cy.wrapIframe().find('[data-cy="skillProgressTitle-skill1"] [data-cy="skillProgressTitle"]').click();
    cy.wrapIframe().find('[data-cy="skillsTitle"]').should('contain.text', 'Skill Overview');
    // Change observable fixture data so an assertion cannot pass on the old page.
    cy.createSkill(1, 1, 1, { name: 'Updated version skill', version: 0 });
    sendParentMessages([{ postmate: 'call', property: 'updateVersion', data: 0 }]);
    cy.wrapIframe().find('[data-cy="skillProgressTitle"]').should('contain.text', 'Updated version skill');
    cy.wrapIframe().find('[data-cy="skillsTitle"]').should('contain.text', 'Skill Overview');
    cy.wrapIframe().find('[data-cy="skillsDisplayInitError"]').should('not.exist');
    cy.wrapIframe().find('[data-cy="skillsDisplayAuthenticationError"]').should('not.exist');
  });

  describe('Version refresh with in-flight responses', () => {
    let releases;
    let cleanups;
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
      cleanups = [];
      cy.createSkill(1, 1, 2, { version: 1 });
      if (Cypress.browser.family === 'chromium') {
        cy.then(() => Cypress.automation('remote:debugger:protocol', {
          command: 'Network.setCacheDisabled', params: { cacheDisabled: true },
        }));
      }
    });
    afterEach(() => {
      releases.forEach((release) => release());
      cleanups.forEach((cleanup) => cleanup());
      if (Cypress.browser.family === 'chromium') {
        cy.then(() => Cypress.automation('remote:debugger:protocol', {
          command: 'Network.setCacheDisabled', params: { cacheDisabled: false },
        }));
      }
    });

    [true, false].forEach((duringRefresh) => {
      it(`rejects stale home summary and loading cleanup ${duringRefresh ? 'during' : 'after'} a version refresh`, () => {
        const old = holdResponse('**/api/projects/proj1/summary*', 'oldVersionSummary');
        cy.visit('/__e2e/skills-display-parent');
        cy.wrap(null).should(() => { expect(old.isReceived(), 'old summary is held').to.be.true; });
        const fresh = duringRefresh ? holdResponse('**/api/projects/proj1/summary*', 'freshVersionSummary') : null;
        sendParentMessages([{ postmate: 'call', property: 'updateVersion', data: 0 }]);
        if (fresh) {
          cy.wrap(null).should(() => { expect(fresh.isReceived(), 'fresh summary is held').to.be.true; });
        } else {
          cy.wrapIframe().find('[data-cy="numTotalSkills"]').should('have.text', '1');
        }
        cy.then(old.release);
        cy.wait('@oldVersionSummary');
        if (fresh) {
          cy.wrapIframe().find('[data-pc-name="progressspinner"]').should('be.visible');
          cy.wrapIframe().find('[data-cy="numTotalSkills"]').should('not.exist');
          cy.then(fresh.release);
        }
        cy.wrapIframe().find('[data-cy="numTotalSkills"]').should('have.text', '1');
        cy.wrapIframe().find('[data-cy="totalPoints"]').should('have.text', '200');
      });
    });

    it('replaces pending startup with the latest version and the same learner authentication', () => {
      const userInfo = holdResponse('**/app/userInfo', 'pendingLearnerAuthentication');
      cy.visit('/__e2e/skills-display-parent');
      cy.wrap(null).should(() => { expect(userInfo.isReceived(), 'learner authentication is held').to.be.true; });
      sendParentMessages([{ postmate: 'call', property: 'updateVersion', data: 0 }]);
      cy.wrapIframe().find('[data-cy="numTotalSkills"]').should('have.text', '1');
      cy.then(userInfo.release);
      cy.wait('@pendingLearnerAuthentication');
      cy.wrapIframe().find('[data-cy="numTotalSkills"]').should('have.text', '1');
      cy.wrapIframe().find('[data-cy="totalPoints"]').should('have.text', '200');
      cy.wrapIframe().find('[data-cy="skillsDisplayInitError"]').should('not.exist');
      cy.wrapIframe().find('[data-cy="skillsDisplayAuthenticationError"]').should('not.exist');
    });

    [false, true].forEach((authenticationRecovery) => {
      it(`coalesces version updates during ${authenticationRecovery ? 'authentication recovery' : 'a full display refresh'}`, () => {
        cy.visit('/__e2e/skills-display-parent?allowManualRefresh=true');
        cy.wrapIframe().find('[data-cy="subjectTile-subj1"] [data-cy="subjectTileBtn"]').should('be.visible').click();
        cy.wrapIframe().find('[data-cy="skillProgressTitle-skill2"]').should('be.visible');
        const pending = holdResponse('**/app/userInfo', 'supersededDisplayRefresh');
        if (authenticationRecovery) {
          cy.get('[data-cy="refreshDisplayAuthentication"]').click();
        } else {
          sendParentMessages([{ postmate: 'call', property: 'updateVersion', data: 1 }]);
        }
        cy.wrap(null).should(() => { expect(pending.isReceived(), 'refresh is held').to.be.true; });
        sendParentMessages([
          { postmate: 'call', property: 'updateVersion', data: null },
          { postmate: 'call', property: 'updateVersion', data: 0 },
        ]);
        // The entire routed display is unmounted, not just its current page.
        cy.wrapIframe().find('[data-cy="skillsDisplayHome"]').should('not.exist');
        cy.then(pending.release);
        cy.wrapIframe().find('[data-cy="skillProgressTitle-skill1"]').should('be.visible');
        cy.wrapIframe().find('[data-cy="skillProgressTitle-skill2"]').should('not.exist');
        cy.wrapIframe().find('[data-cy="skillsTitle"]').should('contain.text', 'Subject 1');
        cy.wrapIframe().find('[data-cy="skillsDisplayAuthenticationError"]').should('not.exist');
        cy.wrapIframe().find('[data-cy="skillsDisplayInitError"]').should('not.exist');
      });
    });

    it('keeps the learner lifetime bound across a version refresh and cannot reopen an identity violation', () => {
      cy.visit('/__e2e/skills-display-parent');
      cy.wrapIframe().find('[data-cy="numTotalSkills"]').should('have.text', '2');
      // Simulate a service-authenticated identity mismatch, not a parent model ID.
      cy.intercept({ method: 'GET', url: '**/app/userInfo', times: 1 }, (req) => {
        req.continue((res) => { res.body.userId = 'different-learner@skills.org'; });
      });
      sendParentMessages([{ postmate: 'call', property: 'updateVersion', data: 0 }]);
      cy.wrapIframe().find('[data-cy="skillsDisplayInitError"]').should('be.visible')
        .and('contain.text', 'authenticated learner has changed');
      sendParentMessages([{ postmate: 'call', property: 'updateVersion', data: null }], { expectRejected: true });
      cy.wrapIframe().find('[data-cy="skillsDisplayInitError"]').should('be.visible')
        .and('contain.text', 'destroy and recreate');
      cy.wrapIframe().find('[data-cy="skillsDisplayHome"]').should('not.exist');
    });

    it('retains the authentication block when the version changes while waiting for renewed credentials', () => {
      cy.visit('/__e2e/skills-display-parent?manualRecovery=true');
      cy.wrapIframe().find('[data-cy="numTotalSkills"]').should('have.text', '2');
      cy.intercept({ method: 'GET', url: '**/api/projects/proj1/subjects/subj1/summary*', times: 1 }, {
        statusCode: 401, body: {},
      });
      cy.wrapIframe().find('[data-cy="subjectTile-subj1"] [data-cy="subjectTileBtn"]').click();
      cy.wrapIframe().find('[data-cy="skillsDisplayAuthenticationError"]').should('be.visible');
      sendParentMessages([{ postmate: 'call', property: 'updateVersion', data: 0 }]);
      cy.wrapIframe().find('[data-cy="skillsDisplayAuthenticationError"]').should('be.visible');
      cy.wrapIframe().find('[data-cy="skillsDisplayHome"]').should('not.exist');
      cy.get('[data-cy="refreshDisplayAuthentication"]').should('be.visible').click();
      cy.wrapIframe().find('[data-cy="skillProgressTitle-skill1"]').should('be.visible');
      cy.wrapIframe().find('[data-cy="skillProgressTitle-skill2"]').should('not.exist');
      cy.wrapIframe().find('[data-cy="skillsDisplayAuthenticationError"]').should('not.exist');
    });

    it('rejects an old subject snapshot even after the version returns to its original value', () => {
      cy.visit('/__e2e/skills-display-parent');
      cy.wrapIframe().find('[data-cy="numTotalSkills"]').should('have.text', '2');
      const old = holdResponse('**/api/projects/proj1/subjects/subj1/summary*', 'oldVersionSubject', (res) => {
        res.body.subject = 'Stale subject snapshot';
      });
      cy.wrapIframe().find('[data-cy="subjectTile-subj1"] [data-cy="subjectTileBtn"]').click();
      cy.wrap(null).should(() => { expect(old.isReceived(), 'old subject is held').to.be.true; });
      sendParentMessages([{ postmate: 'call', property: 'updateVersion', data: 0 }]);
      cy.wrapIframe().find('[data-cy="skillProgressTitle-skill1"]').should('be.visible');
      cy.wrapIframe().find('[data-cy="skillProgressTitle-skill2"]').should('not.exist');
      sendParentMessages([{ postmate: 'call', property: 'updateVersion', data: null }]);
      cy.wrapIframe().find('[data-cy="skillProgressTitle-skill2"]').should('be.visible');
      cy.then(old.release);
      cy.wait('@oldVersionSubject');
      cy.wrapIframe().find('[data-cy="skillsTitle"]').should('contain.text', 'Subject 1')
        .and('not.contain.text', 'Stale subject snapshot');
      cy.wrapIframe().find('[data-cy="skillProgressTitle-skill2"]').should('be.visible');
    });

    it('does not restore stale ranking or point-history caches after a version refresh', () => {
      cy.doReportSkill({ skill: 1, userId: Cypress.env('proxyUser'), date: '2020-09-12 11:00' });
      const oldRank = holdResponse('**/api/projects/proj1/rank*', 'oldVersionRank', (res) => { res.body.position = 99; });
      const oldHistory = holdResponse('**/api/projects/proj1/pointHistory*', 'oldVersionHistory', (res) => {
        res.body = { pointsHistory: [], achievements: [] };
      });
      cy.visit('/__e2e/skills-display-parent');
      cy.wrapIframe().find('[data-cy="numTotalSkills"]').should('have.text', '2');
      cy.wrap(null).should(() => {
        expect(oldRank.isReceived(), 'old ranking is held').to.be.true;
        expect(oldHistory.isReceived(), 'old history is held').to.be.true;
      });
      sendParentMessages([{ postmate: 'call', property: 'updateVersion', data: 0 }]);
      cy.wrapIframe().find('[data-cy="numTotalSkills"]').should('have.text', '1');
      cy.wrapIframe().find('[data-cy="myRankPosition"]').should('have.text', '#1');
      cy.wrapIframe().find('[data-cy="pointHistoryChartWithData"]').should('be.visible');
      cy.then(() => { oldRank.release(); oldHistory.release(); });
      cy.wait(['@oldVersionRank', '@oldVersionHistory']);
      cy.wrapIframe().find('[data-cy="myRankPosition"]').should('have.text', '#1');
      cy.wrapIframe().find('[data-cy="pointHistoryChartWithData"]').should('be.visible');
      cy.wrapIframe().find('[data-cy="pointHistoryChartNoData"]').should('not.exist');
    });

    it('ignores an old-version 401 without expiring the fixed learner authentication', () => {
      const isExpectedFailure = (error) => {
        const requestUrl = error?.config?.url;
        return error?.response?.status === 401 && typeof requestUrl === 'string'
          && new URL(requestUrl, Cypress.config('baseUrl')).pathname === '/api/projects/proj1/pointHistory';
      };
      cy.on('uncaught:exception', (error) => {
        if (isExpectedFailure(error)) return false;
      });
      let oldHistoryRequested = false;
      let releaseOldHistory;
      const oldHistoryGate = new Cypress.Promise((resolve) => { releaseOldHistory = resolve; });
      releases.push(releaseOldHistory);
      cy.intercept({
        method: 'GET',
        url: '**/api/projects/proj1/pointHistory*',
        headers: { 'x-skilltree-e2e-point-history': 'old-request' },
        times: 1,
      }, (req) => {
        oldHistoryRequested = true;
        // Supply the actual transport failure after recovery. A forwarded 200
        // with changed response metadata is not proof that the child received 401.
        return oldHistoryGate.then(() => req.reply({
          statusCode: 401,
          headers: { 'cache-control': 'no-store' },
          body: {},
        }));
      }).as('oldVersionUnauthorized');
      cy.visit('/__e2e/skills-display-parent?observeFirstPointHistory=true');
      cy.wrapIframe().find('[data-cy="numTotalSkills"]').should('have.text', '2');
      cy.get('[data-cy="oldPointHistoryRequestState"]').should('have.text', 'pending');
      cy.wrap(null).should(() => {
        expect(oldHistoryRequested, 'the observed old history request is held').to.be.true;
      });
      sendParentMessages([{ postmate: 'call', property: 'updateVersion', data: 0 }]);
      cy.wrapIframe().find('[data-cy="numTotalSkills"]').should('have.text', '1');
      cy.wrapIframe().find('[data-cy="pointHistoryChartNoData"]').should('be.visible');
      cy.intercept('GET', '**/app/userInfo').as('unexpectedVersionRecovery');
      cy.then(() => releaseOldHistory());
      cy.wait('@oldVersionUnauthorized').its('response.statusCode').should('eq', 401);
      cy.get('[data-cy="oldPointHistoryRequestState"]').should('have.text', '401-processed');
      cy.wrapIframe().find('[data-cy="numTotalSkills"]').should('have.text', '1');
      cy.wrapIframe().find('[data-cy="skillsDisplayAuthenticationError"]').should('not.exist');
      cy.wrapIframe().find('[data-cy="skillsDisplayInitError"]').should('not.exist');
      cy.get('@unexpectedVersionRecovery.all').should('have.length', 0);
    });
  });

  it('does not re-authenticate or navigate when the parent repeats the current authentication token', () => {
    cy.intercept('GET', '**/api/projects/proj1/token').as('initialToken');
    cy.visit('/__e2e/skills-display-parent');
    cy.wrapIframe().find('[data-cy="subjectTile-subj1"] [data-cy="subjectTileBtn"]').should('be.visible').click();
    cy.wrapIframe().find('[data-cy="skillsTitle"]').should('contain.text', 'Subject 1');
    cy.intercept('GET', '**/app/userInfo').as('unexpectedAuthentication');
    cy.wait('@initialToken').then(({ response }) => sendParentMessages([
      { postmate: 'call', property: 'updateAuthenticationToken', data: response.body.access_token },
      { postmate: 'call', property: 'updateAuthenticationToken', data: response.body.access_token },
    ]));
    cy.wrapIframe().find('[data-cy="skillsTitle"]').should('be.visible').and('contain.text', 'Subject 1');
    cy.wrapIframe().find('[data-cy="skillsDisplayAuthenticationError"]').should('not.exist');
    // Request absence is intentional: duplicate credentials must not start recovery.
    cy.get('@unexpectedAuthentication.all').should('have.length', 0);
  });

  it('ignores authentication calls from a sibling window with the same origin as the parent', () => {
    cy.visit('/__e2e/skills-display-parent');
    cy.wrapIframe().find('[data-cy="subjectTile-subj1"]').should('be.visible');
    cy.intercept('GET', '**/app/userInfo').as('unexpectedAuthentication');
    cy.window().then((parentWindow) => new Cypress.Promise((resolve) => {
      const displayWindow = parentWindow.document.querySelector('#skills-client-container iframe').contentWindow;
      const sibling = parentWindow.document.createElement('iframe');
      sibling.hidden = true;
      const type = 'application/x-postmate-v1+json';
      const uid = 'sibling-message-fence';
      const receiveFence = (event) => {
        if (event.source === displayWindow && event.data?.postmate === 'reply' && event.data.uid === uid) {
          cleanupSibling();
          resolve();
        }
      };
      cleanupSibling = () => {
        parentWindow.removeEventListener('message', receiveFence);
        sibling.remove();
      };
      parentWindow.addEventListener('message', receiveFence);
      // Execute in a real sibling browsing context: MessageEvent.source must be
      // the sibling, not the trusted parent (even though their origins match).
      const spoofedCall = { postmate: 'call', type, property: 'updateAuthenticationToken', data: 'invalid-sibling-token' };
      sibling.srcdoc = `<script>parent.document.querySelector('#skills-client-container iframe').contentWindow.postMessage(${JSON.stringify(spoofedCall)}, ${JSON.stringify(parentWindow.location.origin)});<\/script>`;
      sibling.onload = () => {
        // A legitimate parent request is a message-queue fence, not a timed delay.
        // Execute in the AUT parent realm, not Cypress's spec-runner window.
        const fence = parentWindow.document.createElement('script');
        fence.textContent = `document.querySelector('#skills-client-container iframe').contentWindow.postMessage(${JSON.stringify({ postmate: 'request', type, property: 'serviceUrl', uid })}, window.location.origin);`;
        parentWindow.document.body.appendChild(fence);
        fence.remove();
      };
      parentWindow.document.body.appendChild(sibling);
    }));
    cy.wrapIframe().find('[data-cy="subjectTile-subj1"]').should('be.visible');
    cy.wrapIframe().find('[data-cy="skillsDisplayAuthenticationError"]').should('not.exist');
    // This assertion specifically verifies that an untrusted sender cannot trigger authentication.
    cy.get('@unexpectedAuthentication.all').should('have.length', 0);
  });

  it('ignores malformed parent navigation and version values without leaving the current route', () => {
    cy.visit('/__e2e/skills-display-parent');
    cy.wrapIframe().find('[data-cy="subjectTile-subj1"] [data-cy="subjectTileBtn"]').should('be.visible').click();
    cy.wrapIframe().find('[data-cy="skillsTitle"]').should('contain.text', 'Subject 1');
    sendParentMessages([
      { postmate: 'call', property: 'navigate', data: null },
      { postmate: 'call', property: 'navigate', data: { name: 'NonexistentEmbeddedRoute' } },
      { postmate: 'call', property: 'navigate', data: { name: 'SkillsDisplayInIframe' } },
      { postmate: 'call', property: { toString: 'not-a-function' }, data: 'invalid-token' },
      { postmate: 'request', property: ['updateAuthenticationToken'], uid: 'malformed-property' },
      { postmate: 'call', property: 'updateVersion', data: { invalid: 'version' } },
      { postmate: 'call', property: 'updateVersion', data: false },
      { postmate: 'call', property: 'updateVersion', data: [0] },
    ]);
    cy.wrapIframe().find('[data-cy="skillProgressTitle-skill1"]').should('be.visible');
    cy.wrapIframe().find('[data-cy="skillsTitle"]').should('contain.text', 'Subject 1');
    cy.wrapIframe().find('[data-cy="skillsDisplayInitError"]').should('not.exist');
    cy.wrapIframe().find('[data-cy="skillsDisplayAuthenticationError"]').should('not.exist');
  });

  it('supports valid parent-provided theme declarations', () => {
    const theme = {
      pageTitle: { textColor: '#123456' },
      tiles: { backgroundColor: '#ffffff' },
      breadcrumb: { align: 'center' },
      progressIndicators: {
        incompleteColor: '#abcdef',
        earnedTodayColor: 'rgb(18, 52, 86)',
        beforeTodayColor: 'hsl(120, 100%, 25%)',
        completeColor: 'green',
      },
      prerequisites: { skillColor: 'orange', badgeColor: '#123456' },
      charts: { axisLabelColor: 'black', pointHistory: { lineColor: 'blue' } },
      infoCards: { iconColors: ['green', '#123456', 'rgb(18, 52, 86)'] },
      circleProgressInteriorTextColor: '#123456',
    };
    cy.visit(`/__e2e/skills-display-parent?minHeight=450px&theme=${encodeURIComponent(JSON.stringify(theme))}`);
    cy.wrapIframe().find('[data-cy="skillsTitle"]').should('be.visible').and('have.css', 'color', 'rgb(18, 52, 86)');
    cy.wrapIframe().find('[data-cy="subjectTile-subj1"] [data-cy="firstProgressBar"]')
      .first().should('be.visible').and('have.css', 'background-color', 'rgb(171, 205, 239)');
    cy.wrapIframe().find('#skilltree-main-container').should('have.css', 'min-height', '450px');
    cy.wrapIframe().find('[data-cy="skillsDisplayInitError"]').should('not.exist');
  });
});
