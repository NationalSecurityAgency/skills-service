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
  preferencePrefix,
  projectPath,
  setProjectDefault,
  viewButton,
} from '../../support/compact-view';

describe('Compact view browser preferences', () => {
  beforeEach(() => {
    cy.viewport(1440, 1000);
    createCompactViewProject();
  });

  it('remembers a browser preference across reloads and project/subject navigation in both directions', () => {
    cy.visit(projectPath());
    assertView(false);
    cy.get(viewButton).click();
    cy.reload();
    assertView(true);
    cy.get('[data-cy="subjectTile-subj1"] [data-cy="subjectTileBtn"]').click();
    assertView(true);
    cy.get('[data-cy="subjectDescription"]').should('contain.text', 'The first subject description');
    cy.get(viewButton).click();
    assertView(false);
    cy.get('[data-cy="breadcrumb-proj1"]').click();
    assertView(false);
    cy.reload();
    assertView(false);
  });

  it('keeps explicit expanded and compact choices when the project default changes', () => {
    setProjectDefault(true);
    cy.visit(projectPath());
    assertView(true);
    cy.get(viewButton).click();
    cy.reload();
    assertView(false);
    setProjectDefault(false);
    cy.visit(`${projectPath()}/subjects/subj1`);
    assertView(false);
    cy.get(viewButton).click();
    cy.reload();
    assertView(true);
  });

  it('isolates project preferences while navigating between projects in the same app', () => {
    createCompactViewProject({ project: 2, compact: true });
    cy.addToMyProjects(1);
    cy.addToMyProjects(2);
    cy.visit(projectPath());
    cy.get(viewButton).click();
    assertView(true);
    cy.get('[data-cy="breadcrumb-Progress And Rankings"]').click();
    cy.get('[data-cy="project-link-proj2"]').click();
    assertView(true);
    cy.get(viewButton).click();
    assertView(false);
    cy.get('[data-cy="breadcrumb-Progress And Rankings"]').click();
    cy.get('[data-cy="project-link-proj1"]').click();
    assertView(true);
    cy.get('[data-cy="breadcrumb-Progress And Rankings"]').click();
    cy.get('[data-cy="project-link-proj2"]').click();
    assertView(false);
  });

  it('isolates preferences for different trainees using the same browser', () => {
    const otherTrainee = 'compact-trainee@skills.org';
    cy.visit(projectPath());
    cy.get(viewButton).click();
    assertView(true);
    cy.fixture('vars.json').then(({ defaultPass }) => {
      cy.register(otherTrainee, defaultPass);
      cy.login(otherTrainee, defaultPass);
    });
    cy.visit(projectPath());
    assertView(false);
    cy.get(viewButton).click();
    assertView(true);
    cy.get(viewButton).click();
    assertView(false);
    cy.loginAsAdminUser();
    cy.visit(projectPath());
    assertView(true);
    cy.logout();
    cy.fixture('vars.json').then(({ defaultPass }) => cy.login(otherTrainee, defaultPass));
    cy.visit(projectPath());
    assertView(false);
  });

  it('ignores malformed browser preferences and allows the trainee to choose a valid view', () => {
    cy.visit(projectPath());
    cy.get(viewButton).click();
    assertView(true);
    cy.window().then((win) => {
      const keys = Object.keys(win.localStorage).filter((key) => key.startsWith(preferencePrefix));
      expect(keys).to.have.length(1);
      win.localStorage.setItem(keys[0], 'not-a-boolean');
    });
    cy.reload();
    assertView(false);
    cy.get(viewButton).click();
    cy.reload();
    assertView(true);
  });

  it('still switches views when browser storage reads and writes are blocked', () => {
    setProjectDefault(true);
    cy.visit(projectPath(), {
      onBeforeLoad(win) {
        const nativeGet = win.Storage.prototype.getItem;
        const nativeSet = win.Storage.prototype.setItem;
        cy.stub(win.Storage.prototype, 'getItem').callsFake(function (key) {
          if (String(key).startsWith(preferencePrefix)) throw new Error('Progress preference storage is blocked');
          return nativeGet.call(this, key);
        });
        cy.stub(win.Storage.prototype, 'setItem').callsFake(function (key, value) {
          if (String(key).startsWith(preferencePrefix)) throw new Error('Progress preference storage is blocked');
          return nativeSet.call(this, key, value);
        });
      },
    });
    assertView(true);
    cy.get(viewButton).click();
    assertView(false);
    cy.get(viewButton).click();
    assertView(true);
  });
});
