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

export const overview = '[data-cy="progressAndRankingOverview"]';
export const compactOverview = '[data-cy="compactProgressOverview"]';
export const viewButton = '[data-cy="progressViewExpandCollapseButton"]';
export const embeddedProgressViewStorageKey = 'skills-display:progress-view';
export const projectPath = (project = 1) => `/progress-and-rankings/projects/proj${project}`;

export const selectProgressViewPreference = (value) => {
  cy.get(`label[for="progressView-${value}"]`).should('be.visible').click();
  cy.get(`[data-cy="progressViewPreference-${value}"] input`).should('be.checked');
  cy.get('[data-cy="unsavedChangesAlert"]').should('be.visible');
  cy.get('[data-cy="userPrefsSettingsSave"]').should('be.enabled').click();
  cy.get('[data-cy="settingsSavedAlert"]').should('be.visible');
};

export const saveProgressViewPreference = (value) => {
  cy.visit('/settings/preferences');
  selectProgressViewPreference(value);
};

export const setProjectDefault = (compact, project = 1) => cy.request('POST', `/admin/projects/proj${project}/settings`, [{
  projectId: `proj${project}`,
  setting: 'skills-display-compact-view-default',
  value: String(compact),
}]);

export const createCompactViewProject = ({ project = 1, compact, empty = false, skillOverrides = {} } = {}) => {
  cy.createProject(project);
  const settings = [
    { projectId: `proj${project}`, setting: 'production.mode.enabled', value: 'true' },
    { projectId: `proj${project}`, setting: 'skills-display-achievement-celebration-disabled', value: 'true' },
  ];
  if (compact !== undefined) {
    settings.push({ projectId: `proj${project}`, setting: 'skills-display-compact-view-default', value: String(compact) });
  }
  cy.request('POST', `/admin/projects/proj${project}/settings`, settings);
  if (!empty) {
    cy.createSubject(project, 1, { description: 'The first subject description remains available in compact view.' });
    cy.createSkill(project, 1, 1, { pointIncrement: 100, numPerformToCompletion: 1, ...skillOverrides });
  }
};

// Unequal point values and a second subject distinguish point percentages from skill percentages.
export const createCompactMetricsProject = (skillOverrides = {}) => {
  createCompactViewProject({ compact: true, skillOverrides });
  cy.createSkill(1, 1, 2, { pointIncrement: 500, numPerformToCompletion: 1 });
  cy.createSkill(1, 1, 3, { pointIncrement: 400, numPerformToCompletion: 1 });
  cy.createSubject(1, 2);
  cy.createSkill(1, 2, 4, { pointIncrement: 500, numPerformToCompletion: 1 });
};

export const addBadgesAndLearningPath = () => {
  cy.createBadge(1, 1);
  cy.assignSkillToBadge(1, 1, 1);
  cy.enableBadge(1, 1);
  cy.createBadge(1, 2);
  cy.assignSkillToBadge(1, 2, 2);
  cy.enableBadge(1, 2);
  cy.addLearningPathItem(1, 1, 3);
};

export const assertView = (compact) => {
  cy.get(overview).should('be.visible').and('have.attr', 'data-compact', String(compact));
  cy.get(viewButton).should('be.visible').and('be.enabled')
    .and('have.attr', 'aria-expanded', String(!compact))
    .and('have.attr', 'aria-label', compact ? 'Expand progress view' : 'Collapse progress view');
  cy.get(viewButton).find(compact ? '.fa-expand-alt' : '.fa-compress-alt').should('be.visible');
  if (compact) {
    cy.get(compactOverview).should('be.visible');
    cy.get('[data-cy="overallLevel"]').should('not.exist');
    cy.get(overview).find('[data-cy="pointHistoryChart"]:visible').should('not.exist');
  } else {
    cy.get(compactOverview).should('not.exist');
    cy.get('[data-cy="overallLevel"]').should('be.visible');
    cy.get('[data-cy="pointHistoryChart"]').should('be.visible');
  }
};

export const assertSkillCount = (completed, total) => {
  cy.get('[data-cy="compactAchievedSkillsCount"]').should(($count) => {
    expect($count.text().replace(/\s/g, '')).to.equal(`${completed}/${total}`);
  });
};
