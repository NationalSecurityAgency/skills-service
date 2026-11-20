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
  selectProgressViewPreference,
  setProjectDefault,
  viewButton,
} from '../../../support/compact-view';

describe('Progress view user preferences', () => {
  beforeEach(() => {
    cy.viewport(1440, 1000);
    createCompactViewProject();
  });

  it('keeps toggles page-local and shows a dismissible preference reminder in both directions', () => {
    cy.visit(projectPath());
    assertView(false);
    cy.get('[data-cy="progressViewPreferenceMessage"]').should('not.exist');
    cy.get(viewButton).click();
    assertView(true);
    cy.get('[data-cy="progressViewPreferenceMessage"]').should('be.visible')
      .and('contain.text', 'To permanently change your view preference, visit');
    cy.get('[data-cy="progressViewPreferencesLink"]').should('have.attr', 'href', '/settings/preferences#progress-view-preference');
    cy.get('[data-cy="progressViewPreferenceMessage"] [data-pc-section="closebutton"]').click();
    cy.get('[data-cy="progressViewPreferenceMessage"]').should('not.exist');
    assertView(true);
    cy.get(viewButton).click();
    assertView(false);
    cy.get('[data-cy="progressViewPreferenceMessage"]').should('be.visible');
    cy.get(viewButton).click();
    cy.get('[data-cy="subjectTile-subj1"] [data-cy="subjectTileBtn"]').click();
    assertView(false);
    cy.get('[data-cy="progressViewPreferenceMessage"]').should('not.exist');
    cy.get('[data-cy="subjectDescription"]').should('contain.text', 'The first subject description');
    cy.get(viewButton).click();
    assertView(true);
    cy.get('[data-cy="breadcrumb-proj1"]').click();
    assertView(false);
    cy.get(viewButton).click();
    cy.reload();
    assertView(false);
    cy.get('[data-cy="progressViewPreferenceMessage"]').should('not.exist');
  });

  it('saves expanded and compact defaults independently of the project default', () => {
    setProjectDefault(true);
    saveProgressViewPreference('expanded');
    cy.reload();
    cy.get('[data-cy="progressViewPreference-expanded"] input').should('be.checked');
    cy.get('[data-cy="userPrefsSettingsSave"]').should('be.disabled');
    cy.visit(projectPath());
    assertView(false);
    cy.reload();
    assertView(false);
    setProjectDefault(false);
    cy.visit(`${projectPath()}/subjects/subj1`);
    assertView(false);

    saveProgressViewPreference('compact');
    cy.visit(projectPath());
    assertView(true);
    cy.get(viewButton).click();
    assertView(false);
    cy.reload();
    assertView(true);
    cy.visit(`${projectPath()}/subjects/subj1`);
    assertView(true);
  });

  it('applies one saved preference across projects and restores each project default when requested', () => {
    createCompactViewProject({ project: 2, compact: true });
    cy.addToMyProjects(1);
    cy.addToMyProjects(2);
    saveProgressViewPreference('expanded');
    cy.visit(projectPath());
    assertView(false);
    cy.get('[data-cy="breadcrumb-Progress And Rankings"]').click();
    cy.get('[data-cy="project-link-proj2"]').click();
    assertView(false);

    saveProgressViewPreference('project-default');
    cy.visit(projectPath(2));
    assertView(true);
    cy.get('[data-cy="breadcrumb-Progress And Rankings"]').click();
    cy.get('[data-cy="project-link-proj1"]').click();
    assertView(false);
  });

  it('refreshes the cached display configuration after saving via the reminder link', () => {
    cy.visit(projectPath());
    cy.get(viewButton).click();
    cy.get('[data-cy="progressViewPreferencesLink"]').click();
    cy.get('[data-cy="progressViewPreferenceSetting"]').should('be.visible');
    cy.get('[data-cy="progressViewPreference-project-default"] input').should('be.checked');
    selectProgressViewPreference('compact');
    cy.get('[data-cy="userPrefsSettingsSave"]').should('be.disabled');
    cy.go('back');
    assertView(true);
    cy.get('[data-cy="progressViewPreferenceMessage"]').should('not.exist');
  });

  [true, false].forEach((compact) => {
    it(`reloads the ${compact ? 'compact' : 'expanded'} preference when a delayed save finishes after returning to progress`, () => {
      setProjectDefault(!compact);
      cy.visit(projectPath());
      assertView(!compact);

      let releaseSaveResponse;
      let saveResponseReceived = false;
      const saveResponseGate = new Cypress.Promise((resolve) => {
        releaseSaveResponse = resolve;
      });
      cy.intercept('POST', '/app/userInfo/settings', (req) => {
        req.continue(() => {
          // Save on the server, but withhold the response until progress is active again.
          saveResponseReceived = true;
          return saveResponseGate;
        });
      }).as('saveProgressViewPreference');

      cy.document().then((progressDocument) => {
        cy.get(viewButton).click();
        cy.get('[data-cy="progressViewPreferencesLink"]').click();
        cy.get('[data-cy="progressViewPreferenceSetting"]').should('be.visible');
        const preference = compact ? 'compact' : 'expanded';
        cy.get(`label[for="progressView-${preference}"]`).click();
        cy.get(`[data-cy="progressViewPreference-${preference}"] input`).should('be.checked');
        cy.get('[data-cy="userPrefsSettingsSave"]').should('be.enabled').click();
        cy.wrap(null).should(() => {
          expect(saveResponseReceived, 'save response is held before navigation').to.be.true;
        });
        cy.get('[data-cy="settingsSavedAlert"]').should('not.exist');

        cy.go('back');
        cy.location('pathname').should('eq', projectPath());
        // A full-page navigation would discard the stale cache and mask this regression.
        cy.document().should('eq', progressDocument);
        assertView(!compact);

        cy.then(() => releaseSaveResponse());
        cy.wait('@saveProgressViewPreference');
        assertView(compact);
        cy.get('[data-cy="earnedPoints"]').should('have.text', '0');
        cy.get('[data-cy="totalPoints"]').should('have.text', '100');
        cy.get('[data-cy="progressViewPreferenceMessage"]').should('not.exist');
        cy.document().should('eq', progressDocument);
      });
    });
  });

  it('does not apply an unsaved preference', () => {
    cy.visit(projectPath());
    cy.get(viewButton).click();
    cy.get('[data-cy="progressViewPreferencesLink"]').click();
    cy.get('label[for="progressView-compact"]').click();
    cy.get('[data-cy="unsavedChangesAlert"]').should('be.visible');
    cy.go('back');
    assertView(false);
  });

  it('isolates saved preferences for different trainees using the same browser', () => {
    const otherTrainee = 'compact-trainee@skills.org';
    saveProgressViewPreference('compact');
    cy.visit(projectPath());
    assertView(true);
    cy.fixture('vars.json').then(({ defaultPass }) => {
      cy.register(otherTrainee, defaultPass);
      cy.login(otherTrainee, defaultPass);
    });
    cy.visit(projectPath());
    assertView(false);
    cy.loginAsAdminUser();
    cy.visit(projectPath());
    assertView(true);
    cy.logout();
    cy.fixture('vars.json').then(({ defaultPass }) => cy.login(otherTrainee, defaultPass));
    cy.visit(projectPath());
    assertView(false);
  });
});
