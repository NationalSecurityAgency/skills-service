/*
 * Copyright 2024 SkillTree
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
describe('Skills Display Badges Tests', () => {
  beforeEach(() => {
    Cypress.env('disabledUILoginProp', true);
    cy.request('POST', '/app/projects/proj1', {
      projectId: 'proj1',
      name: 'proj1'
    });
    cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
      projectId: 'proj1',
      subjectId: 'subj1',
      name: 'Subject 1',
      helpUrl: 'http://doHelpOnThisSubject.com',
      description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.'
    });
    cy.request('POST', '/admin/projects/proj1/subjects/subj2', {
      projectId: 'proj1',
      subjectId: 'subj2',
      name: 'Subject 2'
    });
    cy.request('POST', '/admin/projects/proj1/subjects/subj3', {
      projectId: 'proj1',
      subjectId: 'subj3',
      name: 'Subject 3'
    });
    cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill1`, {
      projectId: 'proj1',
      subjectId: 'subj1',
      skillId: 'skill1',
      name: `This is 1`,
      type: 'Skill',
      pointIncrement: 100,
      numPerformToCompletion: 5,
      pointIncrementInterval: 0,
      numMaxOccurrencesIncrementInterval: -1,
      description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.',
      version: 0,
      helpUrl: 'http://doHelpOnThisSkill.com'
    });

    cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill2`, {
      projectId: 'proj1',
      subjectId: 'subj1',
      skillId: 'skill2',
      name: `This is 2`,
      type: 'Skill',
      pointIncrement: 100,
      numPerformToCompletion: 5,
      pointIncrementInterval: 0,
      numMaxOccurrencesIncrementInterval: -1,
      description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.',
      version: 0,
      helpUrl: 'http://doHelpOnThisSkill.com'
    });
    cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill3`, {
      projectId: 'proj1',
      subjectId: 'subj1',
      skillId: 'skill3',
      name: `This is 3`,
      type: 'Skill',
      pointIncrement: 100,
      numPerformToCompletion: 2,
      pointIncrementInterval: 0,
      numMaxOccurrencesIncrementInterval: -1,
      description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.',
      version: 0,
      helpUrl: 'http://doHelpOnThisSkill.com'
    });

    cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill4`, {
      projectId: 'proj1',
      subjectId: 'subj1',
      skillId: 'skill4',
      name: `This is 4`,
      type: 'Skill',
      pointIncrement: 100,
      numPerformToCompletion: 2,
      pointIncrementInterval: 0,
      numMaxOccurrencesIncrementInterval: -1,
      description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.',
      version: 0,
      helpUrl: 'http://doHelpOnThisSkill.com'
    });

    cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill5`, {
      projectId: 'proj1',
      subjectId: 'subj1',
      skillId: 'skill5',
      name: `This is 5`,
      type: 'Skill',
      pointIncrement: 100,
      numPerformToCompletion: 1,
      pointIncrementInterval: 0,
      numMaxOccurrencesIncrementInterval: -1,
      description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.',
      version: 0,
      helpUrl: 'http://doHelpOnThisSkill.com'
    });
    cy.request('POST', `/admin/projects/proj1/skill4/prerequisite/proj1/skill2`);

    cy.request('POST', `/api/projects/proj1/skills/skill1`, {
      userId: Cypress.env('proxyUser'),
      timestamp: new Date().getTime()
    });
    cy.request('POST', `/api/projects/proj1/skills/skill1`, {
      userId: Cypress.env('proxyUser'),
      timestamp: new Date().getTime() - 1000 * 60 * 60 * 24
    });

    cy.request('POST', `/api/projects/proj1/skills/skill3`, {
      userId: Cypress.env('proxyUser'),
      timestamp: new Date().getTime()
    });
    cy.request('POST', `/api/projects/proj1/skills/skill3`, {
      userId: Cypress.env('proxyUser'),
      timestamp: new Date().getTime() - 1000 * 60 * 60 * 24
    });

    const badge1 = {
      projectId: 'proj1',
      badgeId: 'badge1',
      name: 'Badge 1'
    };
    cy.request('POST', '/admin/projects/proj1/badges/badge1', badge1);
    cy.assignSkillToBadge(1, 1, 1);
    cy.enableBadge(1, 1);
  });

  it('project badge skills show subject name when details enabled', () => {
    cy.cdVisit('/', true);
    cy.cdClickBadges();

    cy.get('[data-cy=badgeDetailsLink_badge1]')
      .click();
    cy.contains('Badge Details');
    cy.get('[data-cy=toggleSkillDetails]')
      .click();
    cy.contains('Subject: Subject 1')
      .should('be.visible');
  });

  it('badges details page does not show achieved badges in available badges section', () => {

    cy.request('POST', '/admin/projects/proj1/badges/badge2', {
      projectId: 'proj1',
      badgeId: 'badge2',
      name: 'Badge 2'
    });
    cy.assignSkillToBadge(1, 2, 5);
    cy.request('POST', '/admin/projects/proj1/badges/badge2', {
      projectId: 'proj1',
      badgeId: 'badge2',
      name: 'Badge 2',
      enabled: true,
    });
    cy.reportSkill(1, 5, Cypress.env('proxyUser')); // achieve badge 2

    cy.cdVisit('/', true);
    cy.cdClickBadges();
    cy.get('[data-cy=achievedBadges]')
      .contains('Badge 2');
    cy.get('[data-cy=availableBadges]')
      .contains('Badge 1');
  });

  it('badges details page shows achievement info for skills', () => {
    cy.createBadge(1, 2)
    cy.assignSkillToBadge(1, 2, 5);
    cy.createBadge(1, 2, { enabled: true})
    cy.reportSkill(1, 5, Cypress.env('proxyUser')); // achieve badge 2

    cy.cdVisit('/', true);
    cy.cdClickBadges();
    cy.get('[data-cy=achievedBadges]')
      .contains('Badge 2');
    cy.get('[data-cy=availableBadges]')
      .contains('Badge 1');

    cy.get('[data-cy="achievedBadge-badge2"]').contains('1st place')
    cy.get('[data-cy="achievedBadge-badge2"] [data-cy="earnedBadgeLink_badge2"]').click();
    cy.get('[data-cy=badge_badge2]').contains("You've achieved this badge - ")
    cy.get('[data-cy=badge_badge2]').contains('and you were the first!');
  });

  it('self report skills update badge progress in my badges display', () => {
    cy.createSkill(1, 1, 1, {
      selfReportingType: 'Approval',
      pointIncrement: 50,
      pointIncrementInterval: 0
    });
    cy.createSkill(1, 1, 2, {
      selfReportingType: 'HonorSystem',
      pointIncrement: 50,
      pointIncrementInterval: 0,
      numPerformToCompletion: 2
    });
    cy.createSkill(1, 1, 3);

    cy.createBadge(1, 2);
    cy.assignSkillToBadge(1, 2, 1);
    cy.assignSkillToBadge(1, 2, 2);
    cy.assignSkillToBadge(1, 2, 3);
    cy.enableBadge(1, 2);

    cy.cdVisit('/', true);
    cy.cdClickBadges();
    cy.get('[data-cy=badgeDetailsLink_badge2]').click();

    cy.get('[data-cy="title"]').contains('Badge Details')
    cy.get('[data-cy="badgePercentCompleted"]').contains('66% Complete');
    cy.get('[data-cy=toggleSkillDetails]').click();
    cy.get('[data-cy=claimPointsBtn]').click();
    cy.get('[data-cy="skillDescription-skill2"] [data-cy="selfReportAlert"]').contains('You just earned 50 points');
    cy.get('[data-cy="badgePercentCompleted"]').contains('66% Complete');
    cy.get('[data-cy=claimPointsBtn]').click();
    cy.get('[data-cy="badgePercentCompleted"]').contains('100% Complete');
  });

  it('badge details show skill details focus', () => {
    cy.resetDb();
    cy.fixture('vars.json')
      .then((vars) => {
        if (!Cypress.env('oauthMode')) {
          cy.register(Cypress.env('proxyUser'), vars.defaultPass, false);
        }
      });
    cy.loginAsProxyUser();
    cy.createProject(1);
    cy.createProject(2);
    cy.createSubject(1, 1);
    cy.createSubject(2, 1);
    cy.createSkill(1, 1, 1, { name: 'Search blah skill 1' });
    cy.createSkill(1, 1, 2, { name: 'is a skill 2' });
    cy.createSkill(1, 1, 3, { name: 'find Blah other skill 3' });
    cy.createSkill(1, 1, 4, { name: 'Search nothing skill 4' });

    cy.createSkill(2, 1, 1, { name: 'blah1' });
    cy.createSkill(2, 1, 2, { name: 'blah2' });
    cy.createSkill(2, 1, 3, { name: 'blah3' });
    cy.createSkill(2, 1, 4, { name: 'blah4' });

    cy.loginAsRootUser();

    cy.createGlobalBadge(1);
    cy.assignSkillToGlobalBadge(1, 1, 1);
    cy.enableGlobalBadge();

    cy.loginAsProxyUser();

    cy.cdVisit('/');
    cy.cdClickBadges();
    cy.contains('Global Badge 1');
    cy.get('[data-cy=badgeDetailsLink_globalBadge1]')
      .click();
    cy.contains('Global Badge 1')
      .should('be.visible');
    cy.get('[data-cy="skillProgressTitle"]').contains('Search blah skill 1');
    cy.get('[data-cy="toggleSkillDetails"]').click()
    cy.get('[data-cy="skillDescription-skill1"] [data-cy="pointsPerOccurrenceCard"] [data-cy="mediaInfoCardTitle"]')
  });
})
