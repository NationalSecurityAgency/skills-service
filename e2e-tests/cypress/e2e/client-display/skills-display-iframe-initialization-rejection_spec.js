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

// These contracts fail before token acquisition; no accounts or project data are needed.
Cypress.env('iframeTestSetup', 'stateless');

describe('Skills Display iframe initialization rejection', () => {
  useIframeParentFixture();
  beforeEach(() => {
    // A broken validator must not pass because an absent project causes a later error.
    cy.intercept('GET', '**/api/projects/proj1/token', {
      statusCode: 500,
      body: { explanation: 'Unexpected authentication during initialization rejection' },
    }).as('tokenRequests');
  });
  afterEach(() => {
    cy.get('@tokenRequests.all').should('have.length', 0);
  });

  it('shows an actionable error when the embedding parent never supplies a handshake', () => {
    cy.clock(Date.now(), ['setTimeout', 'clearTimeout']);
    cy.visit('/static/clientPortal/index.html', {
      onBeforeLoad(win) {
        cy.spy(win, 'setTimeout').as('iframeTimeouts');
      },
    });
    // Advance only after the real initialization has installed its handshake timer.
    cy.get('@iframeTimeouts').should('have.been.calledWith', Cypress.sinon.match.func, 10000);
    cy.tick(10000);
    cy.get('[data-cy="skillsDisplayInitError"]').should('be.visible')
      .and('have.attr', 'role', 'alert').and('contain.text', 'verify the embedding configuration');
    cy.get('[data-pc-name="progressspinner"]').should('not.exist');
    cy.get('[data-cy="skillsDisplayHome"]').should('not.exist');
  });

  it('rejects an attacker service URL without sending requests to it', () => {
    const attackerUrl = 'https://attacker.example';
    cy.intercept(`${attackerUrl}/**`, { statusCode: 200, body: {} }).as('attackerRequests');
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

  [
    { navigate: 'replace-child-method' },
    JSON.parse('{"__proto__":{"navigate":"replace-child-prototype"}}'),
  ].forEach((modelOverrides, index) => {
    it(`rejects a handshake model that replaces the child API or its prototype (${index + 1})`, () => {
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
      cy.visit(`/__e2e/skills-display-parent?modelOverrides=${encodeURIComponent(JSON.stringify(modelOverrides))}`);
      cy.wrapIframe().find('[data-cy="skillsDisplayInitError"]').should('be.visible')
        .and('contain.text', 'verify the embedding configuration');
      cy.wrapIframe().find('[data-cy="skillsDisplayHome"]').should('not.exist');
      cy.get('@tokenRequests.all').should('have.length', 0);
    });
  });

  [
    { pageTitle: { textColor: 'red } body { background: url(https://attacker.example/css)' } },
    { tiles: { backgroundColor: 'red; background-image: url(https://attacker.example/css)' } },
    { trophyIconColor: 'url(https://attacker.example/css)' },
    // Sibling progress colors use the same schema/validator; retain one injection.
    { progressIndicators: { incompleteColor: 'red; background-image: url(https://attacker.example/pixel)' } },
    { progressIndicators: { incompleteColor: 'not-a-color' } },
    { progressIndicators: { incompleteColor: { color: 'red' } } },
    { progressIndicators: 'red' },
    { progressIndicators: ['red'] },
    { progressIndicators: null },
    { prerequisites: { skillColor: 'red; background-image: url(https://attacker.example/pixel)' } },
    // Cover chart-module validation through its recursive, nested color path.
    { charts: { pointHistory: { lineColor: 'red; color: blue' } } },
    { infoCards: { iconColors: ['green', 'red; background-image: url(https://attacker.example/pixel)'] } },
    { infoCards: { iconColors: 'red' } },
    { circleProgressInteriorTextColor: 'red; background-image: url(https://attacker.example/pixel)' },
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
