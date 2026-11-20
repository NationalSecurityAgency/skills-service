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
  addBadgesAndLearningPath,
  assertView,
  compactOverview,
  createCompactMetricsProject,
  createCompactViewProject,
  embeddedProgressViewStorageKey,
  projectPath,
  saveProgressViewPreference,
  viewButton,
} from '../../../support/compact-view';

describe('Embedded compact view', () => {
  beforeEach(() => {
    cy.viewport(1440, 1000);
  });

  it('persists embedded toggles without a reminder independently of User Preferences', () => {
    createCompactViewProject({ compact: true });
    saveProgressViewPreference('expanded');
    cy.visit('/test-skills-client/proj1');
    cy.wrapIframe().find(compactOverview).should('be.visible');
    cy.wrapIframe().find(viewButton).click();
    cy.wrapIframe().find('[data-cy="overallLevel"]').should('be.visible');
    cy.wrapIframe().find('[data-cy="progressViewPreferenceMessage"]').should('not.exist');
    cy.wrapIframe().find('[data-cy="progressViewPreferencesLink"]').should('not.exist');
    cy.reload();
    cy.wrapIframe().find('[data-cy="overallLevel"]').should('be.visible');
    cy.wrapIframe().find('[data-cy="subjectTile-subj1"] [data-cy="subjectTileBtn"]').click();
    cy.wrapIframe().find('[data-cy="overallLevel"]').should('be.visible');
    cy.wrapIframe().find(compactOverview).should('not.exist');
    cy.wrapIframe().find('[data-cy="skillsTitle"]').should('contain.text', 'Subject 1');
    cy.wrapIframe().find(viewButton).click();
    cy.wrapIframe().find(compactOverview).should('be.visible');
    cy.wrapIframe().find('[data-cy="progressViewPreferenceMessage"]').should('not.exist');
    cy.wrapIframe().find('[data-cy="progressViewPreferencesLink"]').should('not.exist');
    cy.reload();
    cy.wrapIframe().find(compactOverview).should('be.visible');
    cy.visit(projectPath());
    assertView(false);

    saveProgressViewPreference('compact');
    cy.visit('/test-skills-client/proj1');
    cy.wrapIframe().find(compactOverview).should('be.visible');
    cy.wrapIframe().find(viewButton).click();
    cy.wrapIframe().find('[data-cy="overallLevel"]').should('be.visible');
    cy.visit(projectPath());
    assertView(true);
    cy.visit('/test-skills-client/proj1');
    cy.wrapIframe().find('[data-cy="overallLevel"]').should('be.visible');
  });

  it('shares one browser choice across embedded projects without persisting project defaults', () => {
    createCompactViewProject();
    createCompactViewProject({ project: 2, compact: true });
    cy.visit('/test-skills-client/proj2');
    cy.wrapIframe().find(compactOverview).should('be.visible');
    cy.visit('/test-skills-client/proj1');
    cy.wrapIframe().find('[data-cy="overallLevel"]').should('be.visible');
    cy.wrapIframe().find(viewButton).click();
    cy.wrapIframe().find(compactOverview).should('be.visible');
    cy.visit('/test-skills-client/proj2');
    cy.wrapIframe().find(compactOverview).should('be.visible');
    cy.wrapIframe().find(viewButton).click();
    cy.wrapIframe().find('[data-cy="overallLevel"]').should('be.visible');
    cy.visit('/test-skills-client/proj1');
    cy.wrapIframe().find('[data-cy="overallLevel"]').should('be.visible');
  });

  it('uses the browser choice with differing iframe and dashboard-session identities', () => {
    createCompactViewProject();
    cy.createSkill(1, 1, 2, { pointIncrement: 100, numPerformToCompletion: 1 });
    saveProgressViewPreference('compact');
    const proxyLearner = 'compact-proxy-learner';
    cy.doReportSkill({ skill: 1, userId: proxyLearner, date: 'now' });

    // Supply a real learner token through the normal client authentication handshake.
    // The dashboard retains its administrator session; no token is placed in a URL.
    let learnerToken;
    cy.request(`/admin/projects/proj1/token/${proxyLearner}`).then(({ body }) => {
      learnerToken = body;
    });
    cy.intercept('GET', '**/api/projects/proj1/token', (req) => req.reply(learnerToken));
    cy.visit('/test-skills-client/proj1');
    cy.wrapIframe().find('[data-cy="overallLevel"]').should('be.visible');
    cy.wrapIframe().find('[data-cy="earnedPoints"]').should('have.text', '100');
    cy.wrapIframe().find(viewButton).click();
    cy.wrapIframe().find(compactOverview).should('be.visible');
    cy.wrapIframe().find('[data-cy="progressViewPreferenceMessage"]').should('not.exist');
    cy.wrapIframe().find('[data-cy="progressViewPreferencesLink"]').should('not.exist');
    cy.reload();
    cy.wrapIframe().find(compactOverview).should('be.visible');
    cy.wrapIframe().find('[data-cy="earnedPoints"]').should('have.text', '100');

    // A different iframe learner intentionally shares the same browser-local choice.
    cy.request('/admin/projects/proj1/token/another-compact-proxy-learner').then(({ body }) => {
      learnerToken = body;
    });
    cy.reload();
    cy.wrapIframe().find(compactOverview).should('be.visible');
    cy.wrapIframe().find('[data-cy="earnedPoints"]').should('have.text', '0');
    cy.wrapIframe().find(viewButton).click();
    cy.wrapIframe().find('[data-cy="overallLevel"]').should('be.visible');

    cy.visit(projectPath());
    assertView(true);
    cy.get('[data-cy="earnedPoints"]').should('have.text', '0');
    cy.visit('/settings/preferences');
    cy.get('[data-cy="progressViewPreference-compact"] input').should('be.checked');
  });

  it('falls back to the project default when the browser choice is invalid', () => {
    createCompactViewProject({ compact: true });
    cy.visit('/test-skills-client/proj1', {
      onBeforeLoad(win) {
        win.localStorage.setItem(embeddedProgressViewStorageKey, 'invalid');
      },
    });
    cy.wrapIframe().find(compactOverview).should('be.visible');
    cy.wrapIframe().find(viewButton).click();
    cy.wrapIframe().find('[data-cy="overallLevel"]').should('be.visible');
    cy.reload();
    cy.wrapIframe().find('[data-cy="overallLevel"]').should('be.visible');
  });

});
