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
describe('Skills Display Global Badges Tests', () => {
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

  it('view global badge with no skills assigned', () => {
    cy.resetDb();
    cy.fixture('vars.json')
      .then((vars) => {
        if (!Cypress.env('oauthMode')) {
          cy.register(Cypress.env('proxyUser'), vars.defaultPass, false);
        }
      });
    cy.loginAsProxyUser();
    cy.createProject(1);
    cy.createSubject(1, 1);
    cy.createSkill(1, 1, 1, { name: 'Search blah skill 1' });
    cy.createSkill(1, 1, 2, { name: 'is a skill 2' });
    cy.createSkill(1, 1, 3, { name: 'find Blah other skill 3' });
    cy.createSkill(1, 1, 4, { name: 'Search nothing skill 4' });

    cy.loginAsRootUser();

    cy.createGlobalBadge(1);
    cy.assignProjectToGlobalBadge(1, 1);
    cy.enableGlobalBadge();

    cy.loginAsProxyUser();

    cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'yesterday');
    cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now');
    cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'yesterday');
    cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now');
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'yesterday');
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now');
    cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'yesterday');
    cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'now');

    cy.cdVisit('/', true);
    cy.cdClickBadges();

    // visit global badge
    cy.get('[data-cy=earnedBadgeLink_globalBadge1]')
      .click();

    cy.get('[data-cy="globalBadgeProjectLevels"]').contains('This is project 1');
    cy.get('[data-cy="skillsProgressList"]').should('not.exist')
  });

  it('view global badge with skills from two projects assigned', () => {
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

    cy.createSkill(2, 1, 5, { name: 'blah1' });
    cy.createSkill(2, 1, 6, { name: 'blah2' });
    cy.createSkill(2, 1, 7, { name: 'blah3' });
    cy.createSkill(2, 1, 8, { name: 'blah4' });

    cy.loginAsRootUser();

    cy.createGlobalBadge(1);
    cy.assignSkillToGlobalBadge(1, 1, 1);
    cy.assignSkillToGlobalBadge(1, 5, 2);
    cy.enableGlobalBadge();

    cy.loginAsProxyUser();

    cy.cdVisit('/');
    cy.cdClickBadges();
    cy.contains('Global Badge 1');
    cy.get('[data-cy=badgeDetailsLink_globalBadge1]')
      .click();
    cy.get('[data-cy="badge_globalBadge1"]').contains('Global Badge 1')
      .should('be.visible');
    cy.get('[data-cy="skillProgressTitle-skill1=globalBadge1"]')
      .contains('Search blah skill 1')
    cy.get('[data-cy="skillProgressTitle-skill5=globalBadge1"]')
      .contains('blah1')
  });

  it('completed badge count should not include global badges that do not have dependencies on this project', () => {
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
    cy.assignSkillToGlobalBadge(1, 1, 2);
    cy.enableGlobalBadge(1);

    cy.reportSkill(2, 1, Cypress.env('proxyUser'));
    cy.reportSkill(2, 2, Cypress.env('proxyUser'));

    cy.createGlobalBadge(2);
    cy.assignSkillToGlobalBadge(2, 1, 2);
    cy.enableGlobalBadge(2);

    cy.loginAsProxyUser();

    cy.cdVisit('/');
    cy.get('[data-cy=myBadges]')
      .contains(' 0 ');
  });

  it('global badge with project levels should not display no skill assigned message', () => {
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
    cy.assignProjectToGlobalBadge(1, 1, 2);
    cy.assignProjectToGlobalBadge(1, 2, 2);
    cy.enableGlobalBadge();

    cy.cdVisit('/');
    cy.cdClickBadges();
    cy.contains('Global Badge 1');
    cy.get('[data-cy=badgeDetailsLink_globalBadge1]')
      .click();
    cy.contains('Global Badge 1')
      .should('be.visible');
    cy.get('[data-cy="skillsProgressList"][data-cy=noDataYet]')
      .should('not.exist');
  });

  it('Global badges viewing skill summary of skill from different project does not cause exception', () => {
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


    cy.createSkill(2, 1, 1, { name: 'P2 Search blah skill 1' });
    cy.createSkill(2, 1, 2, { name: 'P2 is a skill 2' });
    cy.createSkill(2, 1, 3, { name: 'P2 find Blah other skill 3' });
    cy.createSkill(2, 1, 4, { name: 'P2 Search nothing skill 4' });

    cy.loginAsRootUser();

    cy.createGlobalBadge(1);
    cy.assignSkillToGlobalBadge(1, 1, 1);
    cy.assignSkillToGlobalBadge(1, 1, 2);
    cy.enableGlobalBadge();

    cy.loginAsProxyUser();

    cy.cdVisit('/');
    cy.cdClickBadges();
    cy.contains('Global Badge 1');
    cy.get('[data-cy=badgeDetailsLink_globalBadge1]')
      .click();
    cy.get('[data-cy="skillsDisplayBreadcrumbBar"] [data-cy="breadcrumbItemValue"]')
      .eq(2)
      .should('have.text', 'globalBadge1')

    cy.get('[data-cy="skillProgressTitle-skill1=globalBadge1"] [data-cy="skillProgressTitle"]').eq(0).click();
    cy.get('[data-cy="back"]').click();
    cy.get('[data-cy="skillProgressTitle"]').eq(1).click();
    cy.contains('Search blah skill 1').should('be.visible');
  });

  it('global badge with project levels should calculate percentage complete properly', () => {
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
    cy.assignProjectToGlobalBadge(1, 1, 2);
    cy.assignProjectToGlobalBadge(1, 2, 2);
    cy.enableGlobalBadge();

    cy.loginAsProxyUser();

    cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'yesterday');
    cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now');
    cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'yesterday');
    cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now');
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'yesterday');
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now');
    cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'yesterday');
    cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'now');

    cy.reportSkill(2, 1, Cypress.env('proxyUser'), 'yesterday');

    cy.cdVisit('/', true);
    cy.cdClickBadges();
    cy.contains('Global Badge 1');
    cy.get('[data-cy=badgeDetailsLink_globalBadge1]')
      .click();
    cy.contains('Global Badge 1')
      .should('be.visible');
    cy.get('[data-cy="skillsProgressList"][data-cy=noDataYet]')
      .should('not.exist');

    // validate that project levels are properly calculated
    cy.get('[data-cy="globalBadgeProjectLevels"] [data-cy="gb_proj1"]').should('contain.text', '100% Complete')
    cy.get('[data-cy="globalBadgeProjectLevels"] [data-cy="gb_proj2"]').should('contain.text', '50% Complete')
  });
})