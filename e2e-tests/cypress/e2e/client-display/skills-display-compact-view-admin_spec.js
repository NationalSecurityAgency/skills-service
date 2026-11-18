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
  setProjectDefault,
  viewButton,
} from '../../support/compact-view';

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
    cy.get('#compactViewDefaultHelp').should('contain.text', 'browser preference takes precedence');
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

  it('starts previews from the project default without reading, persisting or changing trainee overrides', () => {
    cy.doReportSkill({ skill: 1, date: 'now' });
    cy.visit(projectPath());
    cy.get(viewButton).click();
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
