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
  projectPath,
} from '../../support/compact-view';

describe('Compact view accessibility', () => {
  beforeEach(() => {
    createCompactMetricsProject();
    addBadgesAndLearningPath();
    cy.doReportSkill({ skill: 1, date: 'now' });
  });

  ['', ' - dark mode'].forEach((darkMode) => {
    it(`project compact view on ${name}${darkMode}`, () => {
      cy.setDarkModeIfNeeded(darkMode);
      cy.visit(projectPath());
      assertView(true);
      assertSkillCount(1, 4);
      cy.get(compactOverview).contains('+100 today').should('be.visible');
      cy.get('[data-cy="compactMetric-rank"]').should('contain.text', '#1');
      cy.get('[data-cy="compactMetric-badges"]').should('contain.text', 'Badges earned');
      cy.get('[data-cy="viewLearningPathLink"]').should('be.visible');
      cy.get('[data-cy="subjectTile-subj1"] [data-cy="subjectTileBtn"]').should('be.visible');

      cy.customLighthouse();
      cy.injectAxe();
      cy.customA11y();
    });

    it(`subject compact view on ${name}${darkMode}`, () => {
      cy.setDarkModeIfNeeded(darkMode);
      cy.visit(`${projectPath()}/subjects/subj1`);
      assertView(true);
      assertSkillCount(1, 3);
      cy.get(compactOverview).contains('+100 today').should('be.visible');
      cy.get('[data-cy="compactMetric-rank"]').should('contain.text', '#1');
      cy.get('[data-cy="compactMetric-badges"]').should('not.exist');
      cy.get('[data-cy="viewLearningPathLink"]').should('not.exist');
      cy.get('[data-cy="skillProgress_index-0"]').should('be.visible');

      cy.customLighthouse();
      cy.injectAxe();
      cy.customA11y();
    });
  });
});
