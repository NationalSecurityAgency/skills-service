/*
 * Copyright 2020 SkillTree
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
describe('Navigation Tests', () => {
  // beforeEach(() => {
  //   cy.request('POST', '/app/projects/proj1', {
  //     projectId: 'proj1',
  //     name: "My New test Project"
  //   })
  //   cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
  //     projectId: 'proj1', subjectId: 'subj1', name: 'Subject 1',
  //   });
  // });
  const snapshotOptions = {
    blackout: ['[data-cy=pointHistoryChart]', '[data-cy=timePassed]'],
    failureThreshold: 0.03, // threshold for entire image
    failureThresholdType: 'percent', // percent of image or number of pixels
    customDiffConfig: { threshold: 0.01 }, // threshold for each pixel
    capture: 'fullPage', // When fullPage, the application under test is captured in its entirety from top to bottom.
  };

  const cssAttachedToNavigableCards = 'skills-navigable-item';

  beforeEach(() => {
    // Cypress.env('disabledUILoginProp', true);
    cy.request('POST', '/app/projects/proj1', {
      projectId: 'proj1',
      name: 'Project 1'
    });
    cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
      projectId: 'proj1',
      subjectId: 'subj1',
      name: 'Subject 1',
      helpUrl: 'http://doHelpOnThisSubject.com',
      description: "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
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
      description: "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
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
      description: "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
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
      description: "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
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
      description: "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
      version: 0,
      helpUrl: 'http://doHelpOnThisSkill.com'
    });
    cy.request('POST', `/admin/projects/proj1/skills/skill4/dependency/skill2`)

    cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: Cypress.env('proxyUser'), timestamp: new Date().getTime()})
    cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: Cypress.env('proxyUser'), timestamp: new Date().getTime() - 1000*60*60*24})

    cy.request('POST', `/api/projects/proj1/skills/skill3`, {userId: Cypress.env('proxyUser'), timestamp: new Date().getTime()})
    cy.request('POST', `/api/projects/proj1/skills/skill3`, {userId: Cypress.env('proxyUser'), timestamp: new Date().getTime() - 1000*60*60*24})

    cy.request('POST', '/admin/projects/proj1/badges/badge1', {
      projectId: 'proj1',
      badgeId: 'badge1',
      name: 'Badge 1'
    });

    cy.fixture('vars.json').then((vars) => {
      cy.request('POST', '/logout');
      cy.register(Cypress.env('proxyUser'), vars.defaultPass, false);
      if (!Cypress.env('oauthMode')) {
        cy.log('NOT in oauthMode, using form login')
        cy.login(Cypress.env('proxyUser'), vars.defaultPass);
      } else {
        cy.log('oauthMode, using loginBySingleSignOn')
        cy.loginBySingleSignOn()
      }
    });
  });

  it.only('visit mySkills page', function () {
    cy.visit('/my-skills');

    cy.get('[data-cy=numProjectsContributed]').contains(new RegExp(/^1$/));
    cy.get('[data-cy=numProjectsAvailable]').contains(new RegExp(/^\/ 2$/));
    cy.get('[data-cy=info-snap-footer]').contains('It\'s fun to learn! You still have 2 projects to explore.');

    cy.get('[data-cy=numAchievedSkills]').contains(new RegExp(/^1$/));
    cy.get('[data-cy=numSkillsAvailable]').contains(new RegExp(/^Total: 48$/));
    cy.get('[data-cy=num-skills-footer]').contains('So many skills... so little time! Good luck!');

    cy.get('[data-cy=mostRecentAchievedSkill]').contains(new RegExp(/^Last Achieved skill \d+ minute[s]? ago$/));
    cy.get('[data-cy=numAchievedSkillsLastWeek]').contains('1 skills in the last week');
    cy.get('[data-cy=numAchievedSkillsLastMonth]').contains('1 skills in the last month');
    cy.get('[data-cy=last-earned-footer]').contains('Keep up the good work!!');

    cy.get('[data-cy=badges-num-footer]').contains('Be proud to earn those badges!!');
    cy.get('[data-cy=numAchievedBadges]').contains(new RegExp(/^0$/));
    cy.get('[data-cy=numBadgesAvailable]').contains(new RegExp(/^\/ 1$/));
    cy.get('[data-cy=numAchievedGlobalBadges]').contains('Global Badges: 0');
    cy.get('[data-cy=numAchievedGemBadges]').contains('Gems: 0');

    cy.get('[data-cy=project-link-Inception]').should('be.visible');
    cy.get('[data-cy=project-link-Inception]').find('[data-cy=project-card-project-name]').contains('Inception');
    
    cy.get('[data-cy=project-link-proj1]').should('be.visible');
    cy.get('[data-cy=project-link-proj1]').find('[data-cy=project-card-project-name]').contains('Project 1');
    cy.get('[data-cy=project-link-proj1]').find('[data-cy=project-card-project-level]').contains('2');
    cy.get('[data-cy=project-link-proj1]').find('[data-cy=project-card-project-rank]').contains(new RegExp(/^Rank: 1 \/ 1$/));
    cy.get('[data-cy=project-link-proj1]').find('[data-cy=project-card-project-points]').contains(new RegExp(/^400 \/ 1,400$/));

    cy.get('[data-cy=project-link-proj1]').click()
    cy.contains('Overall Points');
  });

});

