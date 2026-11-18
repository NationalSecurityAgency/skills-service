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
} from '../../support/compact-view';

describe('Compact view progress metrics', () => {
  beforeEach(() => {
    cy.viewport(1440, 1000);
  });

  it('renders point and skill percentages independently and retains rank, badge and learning-path navigation', () => {
    createCompactMetricsProject();
    addBadgesAndLearningPath();
    cy.doReportSkill({ skill: 1, date: 'now' });
    cy.visit(projectPath());
    assertView(true);
    cy.get('[data-cy="compactOverallPercent"]').should('have.text', '6%');
    cy.get('[data-cy="earnedPoints"]').should('have.text', '100');
    cy.get('[data-cy="totalPoints"]').should('have.text', '1,500');
    assertSkillCount(1, 4);
    cy.get('[data-cy="compactAchievedSkillsPercent"]').should('have.text', '25%');
    cy.get('[data-cy="compactAchievedSkillsProgress"] [role="progressbar"]').should('have.attr', 'aria-valuenow', '25');
    cy.get('[data-cy="compactMetric-level"]').should('contain.text', 'Level 1').and('contain.text', '50 points remaining');
    cy.get('[data-cy="compactMetric-rank"]').should('contain.text', '#1').and('contain.text', 'of 1 trainee');
    cy.get('[data-cy="compactMetric-badges"]').should('contain.text', 'Badges earned').and(($badges) => {
      expect($badges.text()).to.match(/1\s*\/\s*2/);
    });
    cy.get(compactOverview).should(($card) => {
      const overallPercent = $card.find('[data-cy="compactOverallPercent"] span')[0];
      const skillsPercent = $card.find('[data-cy="compactAchievedSkillsPercent"] span')[0];
      const win = overallPercent.ownerDocument.defaultView;
      expect(win.getComputedStyle(skillsPercent).fontSize).to.equal(win.getComputedStyle(overallPercent).fontSize);
      expect(win.getComputedStyle(skillsPercent).fontWeight).to.equal(win.getComputedStyle(overallPercent).fontWeight);
      const overallBar = $card.find('[role="progressbar"][aria-label="6% of available points earned"]')[0];
      const skillsBar = $card.find('[data-cy="compactAchievedSkillsProgress"] [role="progressbar"]')[0];
      expect(Math.abs(overallBar.getBoundingClientRect().top - skillsBar.getBoundingClientRect().top)).to.be.lessThan(1);
    });

    cy.get('[data-cy="myRankBtn"]').click();
    cy.get('[data-cy="title"]').should('contain.text', 'My Rank');
    cy.get('[data-cy="breadcrumb-proj1"]').click();
    assertView(true);
    cy.get('[data-cy="myBadgesBtn"]').click();
    cy.get('[data-cy="title"]').should('contain.text', 'My Badges');
    cy.get('[data-cy="breadcrumb-proj1"]').click();
    cy.get('[data-cy="viewLearningPathLink"]').should('be.visible').click();
    cy.get('[data-cy="skillsTitle"]').should('contain.text', 'Learning Path');
    cy.get('[data-cy="learningPathProgressCount"]').should('contain.text', '1 of 2 items achieved');
    cy.get('[data-cy="breadcrumb-proj1"]').click();
    cy.get('[data-cy="subjectTile-subj1"] [data-cy="subjectTileBtn"]').click();
    assertView(true);
    cy.get('[data-cy="compactOverallPercent"]').should('have.text', '10%');
    assertSkillCount(1, 3);
    cy.get('[data-cy="compactAchievedSkillsPercent"]').should('have.text', '33%');
    cy.get('[data-cy="compactMetric-badges"]').should('not.exist');
    cy.get('[data-cy="viewLearningPathLink"]').should('not.exist');
    // The shared rank link is exercised above; here verify the subject-specific destination.
    cy.get('[data-cy="myRankBtn"]').should('be.visible').and('have.attr', 'href', `${projectPath()}/subjects/subj1/rank`);
  });

  it('updates points, skills and levels live after completing an Honor System skill from the compact subject page', () => {
    createCompactMetricsProject({ selfReportingType: 'HonorSystem' });
    cy.visit(`${projectPath()}/subjects/subj1`);
    assertView(true);
    cy.get('[data-cy="earnedPoints"]').should('have.text', '0');
    cy.get('[data-cy="totalPoints"]').should('have.text', '1,000');
    cy.get('[data-cy="compactOverallPercent"]').should('have.text', '0%');
    assertSkillCount(0, 3);
    cy.get('[data-cy="compactAchievedSkillsPercent"]').should('have.text', '0%');
    cy.get('[data-cy="compactMetric-level"]').should(($level) => {
      expect($level.text()).to.match(/0\s*\/\s*5/);
    }).and('contain.text', 'Level 1').and('contain.text', '100 points remaining');

    cy.get('[data-cy="toggleSkillDetails"] [data-pc-section="input"]').check();
    cy.get('[data-cy="skillProgress_index-0"]').should('contain.text', 'Very Great Skill 1');
    cy.get('[data-cy="skillProgress_index-0"] [data-cy="claimPointsBtn"]').should('be.visible').and('be.enabled').click();
    cy.get('[data-cy="skillProgress_index-0"] [data-cy="claimPointsBtn"]').should('not.exist');

    // Verify reactive updates before navigation; revisiting this subject would mask reactivity failures.
    cy.get('[data-cy="earnedPoints"]').should('have.text', '100');
    cy.get('[data-cy="compactOverallPercent"]').should('have.text', '10%');
    cy.get(compactOverview).contains('+100 today').should('be.visible');
    assertSkillCount(1, 3);
    cy.get('[data-cy="compactAchievedSkillsPercent"]').should('have.text', '33%');
    cy.get('[data-cy="compactAchievedSkillsProgress"] [role="progressbar"]').should('have.attr', 'aria-valuenow', '33');
    cy.get('[data-cy="compactMetric-level"]').should(($level) => {
      expect($level.text()).to.match(/1\s*\/\s*5/);
    }).and('contain.text', 'Level 2').and('contain.text', '150 points remaining');

    // Loading the project confirms the saved event and its differently scoped totals.
    cy.get('[data-cy="breadcrumb-proj1"]').click();
    assertView(true);
    cy.get('[data-cy="earnedPoints"]').should('have.text', '100');
    cy.get('[data-cy="totalPoints"]').should('have.text', '1,500');
    cy.get('[data-cy="compactOverallPercent"]').should('have.text', '6%');
    assertSkillCount(1, 4);
    cy.get('[data-cy="compactAchievedSkillsPercent"]').should('have.text', '25%');
    cy.get('[data-cy="compactLevelProgress"]').should('contain.text', '50 points remaining');
  });

  it('shows completed levels and skills without suggesting another level', () => {
    createCompactViewProject({ compact: true, skillOverrides: { pointIncrement: 1000 } });
    cy.doReportSkill({ skill: 1, date: 'now' });
    cy.visit(projectPath());
    assertView(true);
    cy.get('[data-cy="compactOverallPercent"]').should('have.text', '100%');
    assertSkillCount(1, 1);
    cy.get('[data-cy="compactAchievedSkillsPercent"]').should('have.text', '100%');
    cy.get('[data-cy="compactLevelProgress"]').should('contain.text', 'All levels complete').and('not.contain.text', 'Next:');
    cy.get('[data-cy="compactLevelProgress"] .fa-circle-check').should('be.visible');
    cy.get('[data-cy="subjectTile-subj1"] [data-cy="allLevelsComplete"]').should('be.visible');
  });

  it('handles empty projects without badges or a learning path', () => {
    createCompactViewProject({ compact: true, empty: true });
    cy.visit(projectPath());
    assertView(true);
    cy.get('[data-cy="compactOverallPercent"]').should('have.text', '0%');
    assertSkillCount(0, 0);
    cy.get('[data-cy="compactAchievedSkillsPercent"]').should('have.text', '0%');
    cy.get(compactOverview).should('not.contain.text', 'NaN');
    cy.get('[data-cy="compactMetric-badges"]').should('not.exist');
    cy.get('[data-cy="viewLearningPathLink"]').should('not.exist');
    cy.get('[data-cy="subjectTile"]').should('not.exist');
    cy.contains('Subjects have not been added yet.').should('be.visible');
  });
});
