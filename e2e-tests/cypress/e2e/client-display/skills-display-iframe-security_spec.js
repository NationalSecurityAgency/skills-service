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
  beforeEach(() => {
    cy.createProject(1);
    cy.createSubject(1, 1);
    cy.createSkill(1, 1, 1);

    // Use the dashboard's installed Postmate version in the actual parent window.
    cy.readFile('../dashboard/node_modules/postmate/build/postmate.min.js').then((postmate) => {
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
});
