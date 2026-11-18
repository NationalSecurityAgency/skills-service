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
  projectPath,
  viewButton,
} from '../../support/compact-view';

describe('Embedded compact view', () => {
  beforeEach(() => {
    cy.viewport(1440, 1000);
  });

  it('persists embedded preferences and shares them with standalone project and subject overviews', () => {
    createCompactViewProject({ compact: true });
    cy.visit('/test-skills-client/proj1');
    cy.wrapIframe().find(compactOverview).should('be.visible');
    cy.wrapIframe().find(viewButton).should('have.attr', 'aria-expanded', 'false').click();
    cy.reload();
    cy.wrapIframe().find('[data-cy="overallLevel"]').should('be.visible');
    cy.wrapIframe().find(compactOverview).should('not.exist');
    cy.wrapIframe().find(viewButton).click();
    cy.wrapIframe().find(compactOverview).should('be.visible');
    cy.wrapIframe().find('[data-cy="subjectTile-subj1"] [data-cy="subjectTileBtn"]').click();
    cy.wrapIframe().find(compactOverview).should('be.visible');
    cy.wrapIframe().find('[data-cy="skillsTitle"]').should('contain.text', 'Subject 1');
    cy.visit(projectPath());
    assertView(true);
    cy.get(viewButton).click();
    assertView(false);
    cy.visit('/test-skills-client/proj1');
    cy.wrapIframe().find('[data-cy="overallLevel"]').should('be.visible');
  });

  it('retains summary-only restrictions in embedded compact view', () => {
    createCompactMetricsProject();
    addBadgesAndLearningPath();
    cy.visit('/test-skills-client/proj1?isSummaryOnly=true');
    cy.wrapIframe().find(compactOverview).should('be.visible');
    cy.wrapIframe().find('[data-cy="compactMetric-badges"]').should('be.visible');
    cy.wrapIframe().find('[data-cy="myRankBtn"]').should('not.exist');
    cy.wrapIframe().find('[data-cy="myBadgesBtn"]').should('not.exist');
    cy.wrapIframe().find('[data-cy="viewLearningPathLink"]').should('not.exist');
    cy.wrapIframe().find('[data-cy="subjectTile-subj1"] a').should('not.exist');
    cy.wrapIframe().find(viewButton).click();
    cy.wrapIframe().find('[data-cy="overallLevel"]').should('be.visible');
  });
});
