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
  createCompactViewProject,
  projectPath,
  viewButton,
} from '../../support/compact-view';

describe('Compact user progress display', () => {
  beforeEach(() => {
    cy.viewport(1440, 1000);
  });

  it('defaults to expanded and switches both the overview and subject cards without losing their content', () => {
    createCompactViewProject();
    cy.createSkill(1, 1, 2, { pointIncrement: 900, numPerformToCompletion: 1 });
    cy.doReportSkill({ skill: 1, date: 'now' });
    cy.visit(projectPath());
    assertView(false);
    cy.get('[data-cy="earnedPoints"]').should('have.text', '100');
    cy.get('[data-cy="subjectTile-subj1"] [data-cy="subjectStars"]').should('be.visible');

    cy.get(viewButton).click();
    assertView(true);
    cy.get('[data-cy="earnedPoints"]').should('have.text', '100');
    cy.get('[data-cy="subjectTile-subj1"]').should('contain.text', 'Subject 1');
    cy.get('[data-cy="subjectTile-subj1"] [data-cy="pointsProgress"]').should('contain.text', '100').and('contain.text', '1,000');
    cy.get('[data-cy="subjectTile-subj1"] [data-cy="subjectStars"]').should('not.exist');
    cy.get('[data-cy="subjectTile-subj1"] [data-cy="subjectRibbonBtn"]').should('not.exist');
    cy.get('[data-cy="subjectTile-subj1"] [data-cy="subjectTileBtn"]').should('be.visible');

    cy.get(viewButton).click();
    assertView(false);
    cy.get('[data-cy="subjectTile-subj1"] [data-cy="subjectStars"]').should('be.visible');
    cy.get('[data-cy="earnedPoints"]').should('have.text', '100');
  });

  it('supports keyboard toggling, responsive layouts and accessible compact controls', () => {
    createCompactMetricsProject();
    addBadgesAndLearningPath();
    cy.doReportSkill({ skill: 1, date: 'now' });
    cy.visit(projectPath());
    assertView(true);
    cy.get(viewButton).focus().should('have.focus');
    cy.realPress('Enter');
    assertView(false);
    cy.get(viewButton).should('have.focus');
    cy.realPress('Space');
    assertView(true);
    [1440, 768, 375].forEach((width) => {
      cy.viewport(width, 900);
      cy.get(compactOverview).should(($card) => {
        expect($card[0].scrollWidth).to.be.at.most($card[0].clientWidth + 1);
        const button = $card[0].parentElement.querySelector(viewButton).getBoundingClientRect();
        const card = $card[0].getBoundingClientRect();
        expect(card.right - button.right).to.be.within(0, 12);
        expect(button.top - card.top).to.be.within(0, 12);
      });
      cy.get('[data-cy="subjectTile-subj1"] [data-cy="subjectTileBtn"]').should('be.visible');
      assertSkillCount(1, 4);
    });
    cy.injectAxe();
    cy.checkA11y(compactOverview, { runOnly: { type: 'tag', values: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'] } });
  });
});
