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

export const authenticationError = '[data-cy="skillsDisplayAuthenticationError"]';
export const refreshAuthentication = '[data-cy="refreshDisplayAuthentication"]';

export const useIframeParentFixture = () => {
  let postmate;
  before(() => {
    // Read the pinned build once, but reinstall intercepts for each isolated browser.
    cy.readFile('node_modules/postmate/build/postmate.min.js').then((contents) => { postmate = contents; });
  });
  beforeEach(() => {
    cy.intercept('GET', '/__e2e/postmate.js', {
      headers: { 'content-type': 'application/javascript' },
      body: postmate,
    });
    cy.intercept('GET', '/__e2e/skills-display-parent*', {
      headers: { 'content-type': 'text/html' },
      fixture: 'skills-display-parent.html',
    });
  });
};

export const assertHome = (compact = false) => cy.wrapIframe().should((body) => {
  const $body = Cypress.$(body);
  expect($body.find('[data-cy="subjectTile-subj1"] [data-cy="subjectTileBtn"]')).to.be.visible;
  const $level = $body.find('[data-cy="overallLevel"]');
  if (compact) {
    expect($level).to.have.length(0);
    expect($body.find('[data-cy="compactProgressOverview"]')).to.be.visible;
  } else {
    expect($level).to.be.visible;
  }
  expect($body.find(authenticationError)).to.have.length(0);
  expect($body.find('[data-pc-name="progressspinner"]')).to.have.length(0);
});

export const assertHeightFitsDisplay = (minHeight = 960) => cy.get('#skills-client-container iframe').should(($iframe) => {
  const frame = $iframe[0];
  const root = frame.contentDocument.querySelector('#skilltree-main-container');
  expect(root, 'persistent iframe content root').not.to.be.null;
  const expectedHeight = Math.max(root.getBoundingClientRect().height + 10, minHeight);
  expect(frame.getBoundingClientRect().height, 'iframe fits its current content').to.be.closeTo(expectedHeight, 1);
  expect(frame.parentElement.getBoundingClientRect().height, 'host container fits its current content').to.be.closeTo(expectedHeight, 1);
});

export const expireAuthentication = () => {
  cy.intercept({ method: 'GET', url: '**/api/projects/proj1/subjects/subj1/summary*', times: 1 }, {
    statusCode: 401,
    body: { explanation: 'Authentication token has expired' },
  });
  cy.wrapIframe().find('[data-cy="subjectTile-subj1"] [data-cy="subjectTileBtn"]').click();
  cy.wrapIframe().should((body) => {
    const $body = Cypress.$(body);
    const $error = $body.find(authenticationError);
    expect($error).to.be.visible;
    expect($error).to.contain.text('authentication has expired');
    expect($error).to.have.attr('role', 'alert');
    expect($error).to.have.attr('aria-labelledby', 'skills-display-authentication-title');
    expect($body.find('#skills-display-authentication-title')).to.be.visible.and.contain.text('Re-authentication required');
    expect($body.find('[data-pc-name="progressspinner"]')).to.have.length(0);
    expect($body.find('[data-cy="skillsDisplayHome"]')).to.have.length(0);
  });
  cy.get(refreshAuthentication).should('be.visible');
};

export const assertRecoveredSubjectContent = (loadingComplete = false) => cy.wrapIframe().should((body) => {
  const $body = Cypress.$(body);
  expect($body.find('[data-cy="skillsTitle"]')).to.be.visible.and.contain.text('Subject 1');
  expect($body.find('[data-cy="skillProgress_index-0"]')).to.be.visible;
  expect($body.find(authenticationError)).to.have.length(0);
  if (loadingComplete) expect($body.find('[data-pc-name="progressspinner"]')).to.have.length(0);
});

export const assertRecoveredSubject = () => assertRecoveredSubjectContent(true);
