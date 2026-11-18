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
  assertSkillCount,
  assertView,
  compactOverview,
  createCompactMetricsProject,
} from '../../../support/compact-view';

describe('Compact view custom theme snapshots', () => {
  beforeEach(() => {
    cy.viewport(1440, 1000);
    createCompactMetricsProject();
    addBadgesAndLearningPath();
    cy.doReportSkill({ skill: 1, date: 'now' });
  });

  [{ name: 'desktop', width: 1440 }, { name: 'mobile', width: 375 }].forEach(({ name, width }) => {
    it(`themed compact project overview on ${name}`, () => {
      cy.viewport(width, 1000);
      cy.visit(`/test-skills-client/proj1?enableTheme=true`);
      cy.wrapIframe().within(() => {
        assertView(true);
        assertSkillCount(1, 4);
        cy.get(compactOverview).contains('+100 today').should('be.visible');
        cy.get('[data-cy="compactMetric-rank"]').should('contain.text', '#1');
        cy.get('[data-cy="compactMetric-badges"]').should('contain.text', 'Badges earned');
        cy.get('[data-cy="viewLearningPathLink"]').should('be.visible');
      });
      cy.matchSnapshotImageForElement('body iframe', { errorThreshold: 0.05 });
    });
  });

  it('themed compact subject card', () => {
    cy.visit(`/test-skills-client/proj1?enableTheme=true`);
    cy.wrapIframe().within(() => {
      assertView(true);
      cy.get('[data-cy="subjectTile-subj1"]').should('contain.text', 'Subject 1');
      cy.get('[data-cy="subjectTile-subj1"] [data-cy="pointsProgress"]').should('contain.text', '100').and('contain.text', '1,000');
      cy.get('[data-cy="subjectTile-subj1"] [data-cy="subjectTileBtn"]').should('be.visible');
    });
    cy.matchSnapshotImageForElement('body iframe', { errorThreshold: 0.05 });
  });

  it('themed compact subject overview', () => {
    cy.visit(`/test-skills-client/proj1?enableTheme=true`);
    cy.wrapIframe().find('[data-cy="subjectTile-subj1"] [data-cy="subjectTileBtn"]').should('be.visible').click();
    cy.wrapIframe().within(() => {
      cy.get('[data-cy="skillsTitle"]').should('contain.text', 'Subject 1');
      assertView(true);
      assertSkillCount(1, 3);
      cy.get(compactOverview).contains('+100 today').should('be.visible');
      cy.get('[data-cy="compactMetric-rank"]').should('contain.text', '#1');
    });
    cy.matchSnapshotImageForElement('body iframe', { errorThreshold: 0.05 });
  });
});
