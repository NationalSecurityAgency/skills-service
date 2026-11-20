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
    cy.visit('/__e2e/skills-display-parent?manualRecovery=true');
    cy.wrapIframe().find('[data-cy="subjectTile-subj1"] [data-cy="subjectTileBtn"]').should('be.visible');
    cy.wrapIframe().find('[data-cy="overallLevel"]').should('be.visible');
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

  const assertRecoveredSubject = () => {
    cy.wrapIframe().find('[data-cy="skillsTitle"]').should('be.visible').and('contain.text', 'Subject 1');
    cy.wrapIframe().find('[data-cy="skillProgress_index-0"]').should('be.visible');
    cy.wrapIframe().find(authenticationError).should('not.exist');
    cy.wrapIframe().find('[data-pc-name="progressspinner"]').should('not.exist');
  };

  it('shows an expired-token error and restores the current route and fresh configuration with a replacement token', () => {
    let iframeDocument;
    cy.get('iframe').then(($iframe) => { iframeDocument = $iframe[0].contentDocument; });
    expireAuthentication();
    // A rendered setting change proves recovery reloads configuration, rather than only removing the error.
    setProjectDefault(true);
    cy.get(refreshAuthentication).click();
    assertRecoveredSubject();
    cy.wrapIframe().find('[data-cy="compactProgressOverview"]').should('be.visible');
    cy.wrapIframe().find('[data-cy="overallLevel"]').should('not.exist');
    cy.get('iframe').then(($iframe) => { expect($iframe[0].contentDocument).to.equal(iframeDocument); });
  });

  [
    { name: 'user information', url: '**/app/userInfo' },
    { name: 'display configuration', url: '**/public/clientDisplay/config*' },
  ].forEach(({ name, url }) => {
    it(`keeps an explicit error when restoring ${name} fails and permits another replacement token`, () => {
      expireAuthentication();
      cy.intercept({ method: 'GET', url, times: 1 }, { statusCode: 401, body: {} });
      cy.get(refreshAuthentication).click();
      cy.wrapIframe().find(authenticationError).should('be.visible').and('contain.text', recoveryFailedMessage);
      cy.wrapIframe().find('[data-pc-name="progressspinner"]').should('not.exist');
      cy.wrapIframe().find('[data-cy="skillsDisplayHome"]').should('not.exist');
      cy.get(refreshAuthentication).click();
      assertRecoveredSubject();
    });
  });

  it('restores the display when the hosting page is refreshed after failed re-authentication', () => {
    expireAuthentication();
    cy.intercept({ method: 'GET', url: '**/app/userInfo', times: 1 }, { statusCode: 401, body: {} });
    cy.get(refreshAuthentication).click();
    cy.wrapIframe().find(authenticationError).should('be.visible').and('contain.text', recoveryFailedMessage);
    cy.reload();
    cy.wrapIframe().find('[data-cy="subjectTile-subj1"] [data-cy="subjectTileBtn"]').should('be.visible');
    cy.wrapIframe().find('[data-cy="overallLevel"]').should('be.visible');
    cy.wrapIframe().find(authenticationError).should('not.exist');
    cy.wrapIframe().find('[data-pc-name="progressspinner"]').should('not.exist');
  });
});
