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

describe('Skills Display iframe destination security', () => {
  let cleanupSibling;
  let parentMessageSequence = 0;
  const sendParentMessages = (messages) => cy.window().then((parentWindow) => new Cypress.Promise((resolve) => {
    const displayWindow = parentWindow.document.querySelector('#skills-client-container iframe').contentWindow;
    const type = 'application/x-postmate-v1+json';
    const uid = `parent-message-fence-${++parentMessageSequence}`;
    const receiveFence = (event) => {
      if (event.source === displayWindow && event.data?.postmate === 'reply' && event.data.uid === uid) {
        cleanupSibling();
        resolve();
      }
    };
    cleanupSibling = () => parentWindow.removeEventListener('message', receiveFence);
    parentWindow.addEventListener('message', receiveFence);
    const fencedMessages = [
      ...messages.map((message) => ({ type, ...message })),
      { postmate: 'request', type, property: 'serviceUrl', uid },
    ];
    // Execute in the actual embedding window, not the Cypress runner. The reply
    // fences message delivery so unchanged UI assertions do not race the calls.
    const script = parentWindow.document.createElement('script');
    script.textContent = `${JSON.stringify(fencedMessages)}.forEach(message => document.querySelector('#skills-client-container iframe').contentWindow.postMessage(message, window.location.origin));`;
    parentWindow.document.body.appendChild(script);
    script.remove();
  }));
  beforeEach(() => {
    cleanupSibling = null;
    cy.createProject(1);
    cy.createSubject(1, 1);
    cy.createSkill(1, 1, 1);

    // Use the e2e suite's pinned Postmate build in the actual parent window.
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

  it('shows an actionable error when the embedding parent never supplies a handshake', () => {
    cy.visit('/static/clientPortal/index.html');
    cy.get('[data-cy="skillsDisplayInitError"]', { timeout: 15000 }).should('be.visible')
      .and('have.attr', 'role', 'alert').and('contain.text', 'verify the embedding configuration');
    cy.get('[data-pc-name="progressspinner"]').should('not.exist');
    cy.get('[data-cy="skillsDisplayHome"]').should('not.exist');
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

  [0, '0', null].forEach((version) => {
    it(`keeps the initial version ${JSON.stringify(version)} and current route when the parent repeats or changes the version`, () => {
      cy.createSkill(1, 1, 2, { version: 1 });
      // loglevel binds console methods during initialization; capture warnings
      // before the real iframe bundle loads rather than spying on a later binding.
      cy.intercept('GET', '**/static/clientPortal/index.html', (req) => {
        req.continue((res) => {
          res.body = res.body.replace('<head>', `<head><script>
            window.skillsDisplayWarnings = [];
            const originalWarn = console.warn;
            console.warn = function (...args) {
              window.skillsDisplayWarnings.push(args.join(' '));
              originalWarn.apply(console, args);
            };
          </script>`);
        });
      });
      const modelOverrides = encodeURIComponent(JSON.stringify({ version }));
      cy.visit(`/__e2e/skills-display-parent?modelOverrides=${modelOverrides}`);
      const expectedSkillCount = version === null ? '2' : '1';
      cy.wrapIframe().find('[data-cy="numTotalSkills"]').should('have.text', expectedSkillCount);
      cy.wrapIframe().find('[data-cy="subjectTile-subj1"] [data-cy="subjectTileBtn"]').click();
      cy.wrapIframe().find('[data-cy="skillsTitle"]').should('contain.text', 'Subject 1');
      let routeChangedCount;
      cy.get('[data-cy="routeChangedCount"]').invoke('text').then((text) => { routeChangedCount = text; });
      sendParentMessages([
        { postmate: 'call', property: 'updateVersion', data: version },
        { postmate: 'call', property: 'updateVersion', data: version },
      ]);
      cy.wrapIframe().should((body) => {
        const warnings = Cypress.$(body)[0].ownerDocument.defaultView.skillsDisplayWarnings;
        expect(warnings.filter((message) => message.includes('ignored a version change')), 'identical versions do not warn').to.have.length(0);
      });
      cy.wrapIframe().find('[data-cy="skillsTitle"]').should('contain.text', 'Subject 1');
      cy.get('[data-cy="routeChangedCount"]').should(($count) => {
        expect($count.text(), 'identical versions do not navigate').to.equal(routeChangedCount);
      });

      const differentVersions = version === null ? [0, 1] : [1, null];
      sendParentMessages(differentVersions.map((data) => ({ postmate: 'call', property: 'updateVersion', data })));
      cy.wrapIframe().should((body) => {
        const warnings = Cypress.$(body)[0].ownerDocument.defaultView.skillsDisplayWarnings
          .filter((message) => message.includes('ignored a version change'));
        expect(warnings, 'different versions warn').to.have.length(2);
        warnings.forEach((message) => expect(message).to.include('destroy and recreate Skills Display'));
      });
      cy.wrapIframe().find('[data-cy="skillsTitle"]').should('contain.text', 'Subject 1');
      cy.get('[data-cy="routeChangedCount"]').should(($count) => {
        expect($count.text(), 'different versions do not navigate').to.equal(routeChangedCount);
      });
      // Fresh navigation proves the ignored updates did not change version state.
      cy.wrapIframe().find('[data-cy="back"]').click();
      cy.wrapIframe().find('[data-cy="numTotalSkills"]').should('have.text', expectedSkillCount);
      cy.wrapIframe().find('[data-cy="subjectTile-subj1"] [data-cy="subjectTileBtn"]').click();
      cy.wrapIframe().find('[data-cy="skillProgressTitle-skill1"]').should('be.visible');
      cy.wrapIframe().find('[data-cy="skillProgressTitle-skill2"]').should(version === null ? 'be.visible' : 'not.exist');
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

  it('rejects an attacker service URL without sending requests to it', () => {
    const attackerUrl = 'https://attacker.example';
    cy.intercept(`${attackerUrl}/**`, { statusCode: 200, body: {} }).as('attackerRequests');
    cy.intercept('GET', '**/api/projects/proj1/token').as('tokenRequests');
    cy.visit(`/__e2e/skills-display-parent?autoHeight=true&minHeight=100px&serviceUrl=${encodeURIComponent(attackerUrl)}`);

    cy.get('iframe').should('have.attr', 'src', `${new URL(Cypress.config('baseUrl')).origin}/static/clientPortal/index.html`);
    cy.wrapIframe().find('[data-cy="skillsDisplayInitError"]')
      .should('be.visible').and('contain.text', 'same origin as the SkillTree iframe');
    cy.wrapIframe().find('[data-cy="subjectTile-subj1"]').should('not.exist');
    cy.get('#skills-client-container iframe').should(($iframe) => {
      const frame = $iframe[0];
      const root = frame.contentDocument.querySelector('#skilltree-main-container');
      expect(root, 'initialization-error content root').not.to.be.null;
      const expectedHeight = Math.max(root.getBoundingClientRect().height + 10, 100);
      expect(frame.getBoundingClientRect().height, 'initialization error is not clipped by a zero-height iframe').to.be.closeTo(expectedHeight, 1);
      expect(frame.parentElement.getBoundingClientRect().height, 'host fits initialization error').to.be.closeTo(expectedHeight, 1);
    });
    // The rendered error establishes that initialization has finished rejecting the model.
    cy.get('@attackerRequests.all').should('have.length', 0);
    cy.get('@tokenRequests.all').should('have.length', 0);
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
    ]);
    cy.wrapIframe().find('[data-cy="skillProgressTitle-skill1"]').should('be.visible');
    cy.wrapIframe().find('[data-cy="skillsTitle"]').should('contain.text', 'Subject 1');
    cy.wrapIframe().find('[data-cy="skillsDisplayInitError"]').should('not.exist');
    cy.wrapIframe().find('[data-cy="skillsDisplayAuthenticationError"]').should('not.exist');
  });

  [
    { navigate: 'replace-child-method' },
    JSON.parse('{"__proto__":{"navigate":"replace-child-prototype"}}'),
  ].forEach((modelOverrides, index) => {
    it(`rejects a handshake model that replaces the child API or its prototype (${index + 1})`, () => {
      cy.intercept('GET', '**/api/projects/proj1/token').as('tokenRequests');
      cy.visit(`/__e2e/skills-display-parent?modelOverrides=${encodeURIComponent(JSON.stringify(modelOverrides))}`);
      cy.wrapIframe().find('[data-cy="skillsDisplayInitError"]').should('be.visible').and('contain.text', 'verify the embedding configuration');
      cy.wrapIframe().find('[data-cy="subjectTile-subj1"]').should('not.exist');
      cy.get('@tokenRequests.all').should('have.length', 0);
    });
  });

  [
    { projectId: '' },
    { projectId: { toString: 'not-a-function' } },
    { version: { invalid: 'version' } },
    { userId: { invalid: 'user' } },
    { options: ['invalid-options'] },
    { minHeight: { toString: 'not-a-function' } },
  ].forEach((modelOverrides, index) => {
    it(`rejects malformed parent configuration before requesting authentication (${index + 1})`, () => {
      cy.intercept('GET', '**/api/projects/proj1/token').as('tokenRequests');
      cy.visit(`/__e2e/skills-display-parent?modelOverrides=${encodeURIComponent(JSON.stringify(modelOverrides))}`);
      cy.wrapIframe().find('[data-cy="skillsDisplayInitError"]').should('be.visible')
        .and('contain.text', 'verify the embedding configuration');
      cy.wrapIframe().find('[data-cy="skillsDisplayHome"]').should('not.exist');
      cy.get('@tokenRequests.all').should('have.length', 0);
    });
  });

  it('supports valid parent-provided theme declarations', () => {
    const theme = { pageTitle: { textColor: '#123456' }, tiles: { backgroundColor: '#ffffff' }, breadcrumb: { align: 'center' } };
    cy.visit(`/__e2e/skills-display-parent?minHeight=450px&theme=${encodeURIComponent(JSON.stringify(theme))}`);
    cy.wrapIframe().find('[data-cy="skillsTitle"]').should('be.visible').and('have.css', 'color', 'rgb(18, 52, 86)');
    cy.wrapIframe().find('#skilltree-main-container').should('have.css', 'min-height', '450px');
    cy.wrapIframe().find('[data-cy="skillsDisplayInitError"]').should('not.exist');
  });

  [
    { pageTitle: { textColor: 'red } body { background: url(https://attacker.example/css)' } },
    { tiles: { backgroundColor: 'red; background-image: url(https://attacker.example/css)' } },
    { trophyIconColor: 'url(https://attacker.example/css)' },
  ].forEach((theme, index) => {
    it(`rejects parent theme CSS injection or external-resource references (${index + 1})`, () => {
      cy.intercept('https://attacker.example/**', { statusCode: 200, body: '' }).as('themeRequests');
      cy.visit(`/__e2e/skills-display-parent?theme=${encodeURIComponent(JSON.stringify(theme))}`);
      cy.wrapIframe().find('[data-cy="skillsDisplayInitError"]').should('be.visible')
        .and('contain.text', 'verify the embedding configuration');
      cy.wrapIframe().find('[data-cy="subjectTile-subj1"]').should('not.exist');
      cy.get('@themeRequests.all').should('have.length', 0);
    });
  });
});
