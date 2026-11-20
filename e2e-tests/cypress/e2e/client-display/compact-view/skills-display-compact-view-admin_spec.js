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

import {
  assertView,
  createCompactViewProject,
  projectPath,
  saveProgressViewPreference,
  setProjectDefault,
  viewButton,
} from '../../../support/compact-view';

const defaultSwitch = '[data-cy="compactViewDefaultSwitch"] [data-pc-section="input"]';
const previewPath = (user = Cypress.env('proxyUser')) => `/administrator/projects/proj1/users/${encodeURIComponent(user)}`;

describe('Compact view admin settings and previews', () => {
  beforeEach(() => {
    cy.viewport(1440, 1000);
    createCompactViewProject();
  });

  it('saves the project default and applies later changes when the trainee has not chosen a view', () => {
    cy.visit('/administrator/projects/proj1/settings');
    cy.contains('label', 'Default to Compact User Progress').should('be.visible');
    cy.get('#compactViewDefaultHelp').should('contain.text', 'User Preferences choice takes precedence');
    cy.get(defaultSwitch).should('not.be.checked');
    cy.get('[data-cy="saveSettingsBtn"]').should('be.disabled');
    cy.get('[data-cy="compactViewDefaultSwitch"]').click();
    cy.get(defaultSwitch).should('be.checked');
    cy.get('[data-cy="unsavedChangesAlert"]').should('be.visible');
    cy.get('[data-cy="saveSettingsBtn"]').should('be.enabled').click();
    cy.get('[data-cy="settingsSavedAlert"]').should('be.visible').and('contain.text', 'Settings Updated');
    cy.reload();
    cy.get(defaultSwitch).should('be.checked');
    cy.visit(projectPath());
    assertView(true);

    cy.visit('/administrator/projects/proj1/settings');
    cy.get('[data-cy="compactViewDefaultSwitch"]').click();
    cy.get(defaultSwitch).should('not.be.checked');
    cy.get('[data-cy="saveSettingsBtn"]').should('be.enabled').click();
    cy.get('[data-cy="settingsSavedAlert"]').should('be.visible');
    cy.visit(`${projectPath()}/subjects/subj1`);
    assertView(false);
  });

  [true, false].forEach((compact) => {
    it(`applies a saved ${compact ? 'compact' : 'expanded'} project default after returning through SPA navigation`, () => {
      setProjectDefault(!compact);
      cy.addToMyProjects(1);
      cy.visit(projectPath());
      // Prime the display configuration cache before changing the project default.
      assertView(!compact);

      cy.document().then((progressDocument) => {
        cy.get('[data-cy="settings-button"]').click();
        cy.get('[data-p="popup"][data-pc-name="menu"] [aria-label="Project Admin"]').click();
        cy.get('[data-cy="projCard_proj1_manageBtn"]').should('be.visible').click();
        cy.get('[data-cy="nav-Settings"]').should('be.visible').click();
        cy.get(defaultSwitch).should(compact ? 'not.be.checked' : 'be.checked');
        cy.get('[data-cy="compactViewDefaultSwitch"]').click();
        cy.get(defaultSwitch).should(compact ? 'be.checked' : 'not.be.checked');
        cy.get('[data-cy="saveSettingsBtn"]').should('be.enabled').click();
        cy.get('[data-cy="settingsSavedAlert"]').should('be.visible').and('contain.text', 'Settings Updated');

        cy.get('[data-cy="settings-button"]').click();
        cy.get('[data-p="popup"][data-pc-name="menu"] [aria-label="Progress and Ranking"]').click();
        cy.get('[data-cy="project-link-proj1"]').should('be.visible').click();
        cy.location('pathname').should('eq', projectPath());
        // A full-page navigation would discard the stale cache and mask this regression.
        cy.document().should('eq', progressDocument);
        assertView(compact);
      });
    });

    it(`reloads the ${compact ? 'compact' : 'expanded'} default when a delayed save finishes after returning to progress`, () => {
      setProjectDefault(!compact);
      cy.addToMyProjects(1);
      cy.visit(projectPath());
      assertView(!compact);

      let releaseSaveResponse;
      let saveResponseReceived = false;
      const saveResponseGate = new Cypress.Promise((resolve) => {
        releaseSaveResponse = resolve;
      });
      cy.intercept('POST', '/admin/projects/proj1/settings', (req) => {
        req.continue(() => {
          // Save on the server, but withhold the response until progress is active again.
          saveResponseReceived = true;
          return saveResponseGate;
        });
      }).as('saveCompactDefault');

      cy.document().then((progressDocument) => {
        cy.get('[data-cy="settings-button"]').click();
        cy.get('[data-p="popup"][data-pc-name="menu"] [aria-label="Project Admin"]').click();
        cy.get('[data-cy="projCard_proj1_manageBtn"]').should('be.visible').click();
        cy.get('[data-cy="nav-Settings"]').should('be.visible').click();
        cy.get(defaultSwitch).should(compact ? 'not.be.checked' : 'be.checked');
        cy.get('[data-cy="compactViewDefaultSwitch"]').click();
        cy.get(defaultSwitch).should(compact ? 'be.checked' : 'not.be.checked');
        cy.get('[data-cy="saveSettingsBtn"]').should('be.enabled').click();
        cy.wrap(null).should(() => {
          expect(saveResponseReceived, 'save response is held before navigation').to.be.true;
        });
        cy.get('[data-cy="settingsSavedAlert"]').should('not.exist');

        cy.get('[data-cy="settings-button"]').click();
        cy.get('[data-p="popup"][data-pc-name="menu"] [aria-label="Progress and Ranking"]').click();
        cy.get('[data-cy="project-link-proj1"]').should('be.visible').click();
        cy.location('pathname').should('eq', projectPath());
        cy.document().should('eq', progressDocument);
        // The cached default is still visible because the save callback has not run yet.
        assertView(!compact);

        cy.then(() => releaseSaveResponse());
        cy.wait('@saveCompactDefault');
        assertView(compact);
        cy.get('[data-cy="earnedPoints"]').should('have.text', '0');
        cy.get('[data-cy="totalPoints"]').should('have.text', '100');
        cy.document().should('eq', progressDocument);
      });
    });
  });

  it('starts previews from the project default without reading, persisting or changing trainee overrides', () => {
    cy.doReportSkill({ skill: 1, date: 'now' });
    saveProgressViewPreference('compact');
    cy.visit(projectPath());
    assertView(true);
    cy.visit(previewPath());
    cy.get('[data-cy="subPageHeader"]').should('contain.text', "User's Display");
    assertView(false);
    cy.get(viewButton).click();
    assertView(true);
    cy.reload();
    assertView(false);
    cy.get(viewButton).click();
    cy.get(viewButton).click();
    assertView(false);
    cy.visit(projectPath());
    assertView(true);

    setProjectDefault(true);
    cy.visit(previewPath());
    assertView(true);
  });

  it('preserves opted-out and archived rank messages without enabling unavailable rank navigation', () => {
    setProjectDefault(true);
    cy.doReportSkill({ skill: 1, date: 'now' });
    cy.request('POST', '/app/userInfo/settings', [{
      settingGroup: 'user.prefs', setting: 'rank_and_leaderboard_optOut', value: 'true',
    }]);
    cy.visit(projectPath());
    assertView(true);
    cy.get('[data-cy="compactMetric-rank"]').should('contain.text', 'Opted-Out').and('contain.text', 'Your position would be #1');
    cy.get('[data-cy="myRankBtn"]').should('be.visible');

    const archivedUser = 'compact-archived@skills.org';
    cy.doReportSkill({ skill: 1, userId: archivedUser, date: 'now' });
    cy.request('POST', '/admin/projects/proj1/users/archive', { userIds: [archivedUser] });
    cy.visit(previewPath(archivedUser));
    assertView(true);
    cy.get('[data-cy="compactMetric-rank"]').should('contain.text', 'Archived').and('contain.text', 'Rank unavailable');
    cy.get('[data-cy="myRankBtn"]').should('not.exist');
  });
});
