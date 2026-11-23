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

  it('rejects an attacker service URL without sending requests to it', () => {
    const attackerUrl = 'https://attacker.example';
    cy.intercept(`${attackerUrl}/**`, { statusCode: 200, body: {} }).as('attackerRequests');
    cy.intercept('GET', '**/api/projects/proj1/token').as('tokenRequests');
    cy.visit(`/__e2e/skills-display-parent?serviceUrl=${encodeURIComponent(attackerUrl)}`);

    cy.get('iframe').should('have.attr', 'src', `${new URL(Cypress.config('baseUrl')).origin}/static/clientPortal/index.html`);
    cy.wrapIframe().find('[data-cy="skillsDisplayInitError"]')
      .should('be.visible').and('contain.text', 'same origin as the SkillTree iframe');
    cy.wrapIframe().find('[data-cy="subjectTile-subj1"]').should('not.exist');
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

  it('ignores malformed parent navigation and supports a valid parent version update', () => {
    cy.visit('/__e2e/skills-display-parent');
    cy.wrapIframe().find('[data-cy="subjectTile-subj1"] [data-cy="subjectTileBtn"]').should('be.visible').click();
    cy.wrapIframe().find('[data-cy="skillsTitle"]').should('contain.text', 'Subject 1');
    cy.window().then((parentWindow) => new Cypress.Promise((resolve) => {
      const displayWindow = parentWindow.document.querySelector('#skills-client-container iframe').contentWindow;
      const type = 'application/x-postmate-v1+json';
      const uid = 'parent-navigation-fence';
      const receiveFence = (event) => {
        if (event.source === displayWindow && event.data?.postmate === 'reply' && event.data.uid === uid) {
          cleanupSibling();
          resolve();
        }
      };
      cleanupSibling = () => parentWindow.removeEventListener('message', receiveFence);
      parentWindow.addEventListener('message', receiveFence);
      const messages = [
        { postmate: 'call', type, property: 'navigate', data: null },
        { postmate: 'call', type, property: 'navigate', data: { name: 'NonexistentEmbeddedRoute' } },
        { postmate: 'call', type, property: 'navigate', data: { name: 'SkillsDisplayInIframe' } },
        { postmate: 'call', type, property: { toString: 'not-a-function' }, data: 'invalid-token' },
        { postmate: 'request', type, property: ['updateAuthenticationToken'], uid: 'malformed-property' },
        { postmate: 'call', type, property: 'updateVersion', data: { invalid: 'version' } },
        { postmate: 'call', type, property: 'updateVersion', data: 0 },
        { postmate: 'request', type, property: 'serviceUrl', uid },
      ];
      // Post the calls from the actual embedding window, not the Cypress runner.
      const script = parentWindow.document.createElement('script');
      script.textContent = `${JSON.stringify(messages)}.forEach(message => document.querySelector('#skills-client-container iframe').contentWindow.postMessage(message, window.location.origin));`;
      parentWindow.document.body.appendChild(script);
      script.remove();
    }));
    cy.wrapIframe().find('[data-cy="subjectTile-subj1"]').should('be.visible');
    cy.wrapIframe().find('[data-cy="skillsTitle"]').should('contain.text', 'User Skills');
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
    cy.wrapIframe().find('#skills-display-app').should('have.css', 'min-height', '450px');
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
