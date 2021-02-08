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
import moment from 'moment';
import dayjs from 'dayjs';
import relativeTimePlugin from 'dayjs/plugin/relativeTime';
dayjs.extend(relativeTimePlugin);

const dateFormatter = value => moment.utc(value).format('YYYY-MM-DD[T]HH:mm:ss[Z]');
const timeFromNowFormatter = (value) => dayjs(value).startOf('hour').fromNow();


const now = new Date().getTime()
const yesterday = new Date().getTime() - (1000 * 60 * 60 * 24)

describe('Navigation Tests', () => {
  // const snapshotOptions = {
  //   blackout: ['[data-cy=pointHistoryChart]', '[data-cy=timePassed]'],
  //   failureThreshold: 0.03, // threshold for entire image
  //   failureThresholdType: 'percent', // percent of image or number of pixels
  //   customDiffConfig: { threshold: 0.01 }, // threshold for each pixel
  //   capture: 'fullPage', // When fullPage, the application under test is captured in its entirety from top to bottom.
  // };

  beforeEach(() => {
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

    cy.request('POST', `/api/projects/proj1/skills/skill1`, {
      userId: Cypress.env('proxyUser'),
      timestamp: new Date().getTime()
    })
    cy.request('POST', `/api/projects/proj1/skills/skill1`, {
      userId: Cypress.env('proxyUser'),
      timestamp: new Date().getTime() - 1000 * 60 * 60 * 24
    })

    cy.request('POST', `/api/projects/proj1/skills/skill3`, {
      userId: Cypress.env('proxyUser'),
      timestamp: new Date().getTime()
    })
    cy.request('POST', `/api/projects/proj1/skills/skill3`, {
      userId: Cypress.env('proxyUser'),
      timestamp: new Date().getTime() - 1000 * 60 * 60 * 24
    })

    cy.request('POST', '/admin/projects/proj1/badges/badge1', {
      projectId: 'proj1',
      badgeId: 'badge1',
      name: 'Badge 1'
    });

    cy.request('POST', '/admin/projects/proj1/badges/gemBadge', {
      projectId: 'proj1',
      badgeId: 'gemBadge',
      name: 'Gem Badge',
      startDate: dateFormatter(new Date() - 1000 * 60 * 60 * 24 * 7),
      endDate: dateFormatter(new Date() + 1000 * 60 * 60 * 24 * 5),
    });

    cy.fixture('vars.json').then((vars) => {
      cy.request('POST', '/logout');
      cy.register(Cypress.env('proxyUser'), vars.defaultPass, false);
      cy.loginAsProxyUser()
    });
  });

  Cypress.Commands.add("loginAsRootUser", (u) => {
    cy.fixture('vars.json').then((vars) => {
      cy.request('POST', '/logout');
      cy.login(vars.rootUser, vars.defaultPass);
    });
  })

  Cypress.Commands.add("loginAsDefaultUser", (u) => {
    cy.fixture('vars.json').then((vars) => {
      cy.request('POST', '/logout');
      cy.login(vars.defaultUser, vars.defaultPass);
    });
  })

  Cypress.Commands.add("loginAsProxyUser", (u) => {
    cy.fixture('vars.json').then((vars) => {
      cy.request('POST', '/logout');
      if (!Cypress.env('oauthMode')) {
        cy.log('NOT in oauthMode, using form login')
        cy.login(Cypress.env('proxyUser'), vars.defaultPass);
      } else {
        cy.log('oauthMode, using loginBySingleSignOn')
        cy.loginBySingleSignOn()
      }
    });

    cy.intercept('/api/metrics/allProjectsSkillEventsOverTimeMetricsBuilder**').as('allSkillEventsForUser');
  })


  it.only('visit mySkills page', function () {
    cy.visit('/my-skills');
    cy.wait('@allSkillEventsForUser');

    cy.get('[data-cy=breadcrumb-MySkills]').contains('MySkills').should('be.visible');

    cy.get('[data-cy=numProjectsContributed]').contains(new RegExp(/^1$/));
    cy.get('[data-cy=numProjectsAvailable]').contains(new RegExp(/^\/ 2$/));
    cy.get('[data-cy=info-snap-footer]').contains('It\'s fun to learn! You still have 1 project to explore.');

    cy.get('[data-cy=numAchievedSkills]').contains(new RegExp(/^1$/));
    cy.get('[data-cy=numSkillsAvailable]').contains(new RegExp(/^Total: 48$/));
    cy.get('[data-cy=num-skills-footer]').contains('So many skills... so little time! Good luck!');

    // cy.get('[data-cy=mostRecentAchievedSkill]').contains(new RegExp(/^Last Achieved skill \d+ minute[s]? ago$/));
    cy.get('[data-cy=mostRecentAchievedSkill]').contains(`Last Achieved skill ${timeFromNowFormatter(now)}`);
    cy.get('[data-cy=numAchievedSkillsLastWeek]').contains('1 skills in the last week');
    cy.get('[data-cy=numAchievedSkillsLastMonth]').contains('1 skills in the last month');
    cy.get('[data-cy=last-earned-footer]').contains('Keep up the good work!!');

    cy.get('[data-cy=badges-num-footer]').contains('Be proud to earn those badges!!');
    cy.get('[data-cy=numAchievedBadges]').contains(new RegExp(/^0$/));
    cy.get('[data-cy=numBadgesAvailable]').contains(new RegExp(/^\/ 2$/));
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

    cy.intercept('GET', '/api/projects/proj1/pointHistory').as('pointHistoryChart');
    cy.wait('@pointHistoryChart');
    cy.wrapIframe().contains('Overall Points');
    cy.get('[data-cy=breadcrumb-MySkills]').should('be.visible');
    cy.get('[data-cy=breadcrumb-Proj1]').should('be.visible');
    cy.get('[data-cy=breadcrumb-projects]').should('not.exist');
  });

  it('mySkills page - contributed to all projects', function () {
    // add a skill to Inception to have contributed to all projects
    cy.loginAsRootUser();
    cy.request('POST', `/api/projects/Inception/skills/VisitUserSettings`, {
      userId: Cypress.env('proxyUser'),
      timestamp: new Date().getTime()
    })

    cy.loginAsProxyUser();
    cy.visit('/my-skills');
    cy.wait('@allSkillEventsForUser');

    cy.get('[data-cy=numProjectsContributed]').contains(new RegExp(/^2$/));
    cy.get('[data-cy=numProjectsAvailable]').contains(new RegExp(/^\/ 2$/));
    cy.get('[data-cy=info-snap-footer]').contains('Congratulations, you have contributed to all available projects!');
  });

  it('mySkills page - not contributed to more than one project', function () {
    cy.request('POST', '/app/projects/proj2', {
      projectId: 'proj2',
      name: 'Project 2'
    });

    cy.loginAsProxyUser();
    cy.visit('/my-skills');
    cy.wait('@allSkillEventsForUser');

    cy.get('[data-cy=numProjectsContributed]').contains(new RegExp(/^1$/));
    cy.get('[data-cy=numProjectsAvailable]').contains(new RegExp(/^\/ 3$/));
    cy.get('[data-cy=info-snap-footer]').contains('It\'s fun to learn! You still have 2 projects to explore.');
  });

  it('mySkills page - time controls call out to the server',() => {

    cy.visit('/my-skills');
    cy.wait('@allSkillEventsForUser');

    cy.get('[data-cy=eventHistoryChart] [data-cy=timeLengthSelector]').contains('6 months').click();
    cy.wait('@allSkillEventsForUser');

    cy.get('[data-cy=eventHistoryChart] [data-cy=timeLengthSelector]').contains('1 year').click();
    cy.wait('@allSkillEventsForUser');
  });

  it('mySkills page - add/remove projects in event history chart',() => {
    // create 5 projects total (including Inception)
    cy.request('POST', '/app/projects/proj2', {
      projectId: 'proj2',
      name: 'Project 2'
    });
    cy.request('POST', '/app/projects/proj3', {
      projectId: 'proj3',
      name: 'Project 3'
    });
    cy.request('POST', '/app/projects/proj4', {
      projectId: 'proj4',
      name: 'Project 4'
    });

    cy.loginAsProxyUser();
    cy.visit('/my-skills');
    cy.wait('@allSkillEventsForUser');

    // validate 4 projects are loaded by default
    cy.get('[data-cy=eventHistoryChart]').contains('Inception').should('be.visible');
    cy.get('[data-cy=eventHistoryChart]').contains('Project 1').should('be.visible');
    cy.get('[data-cy=eventHistoryChart]').contains('Project 2').should('be.visible');
    cy.get('[data-cy=eventHistoryChart]').contains('Project 3').should('be.visible');
    cy.get('[data-cy=eventHistoryChart]').contains('Project 4').should('not.be.visible');

    // remove a project
    cy.get('[data-cy=eventHistoryChartProjectSelector]  .multiselect__tag-icon').should('have.length', 4).as('removeBtns');
    cy.get('@removeBtns').eq(2).click()
    cy.get('[data-cy=eventHistoryChart]').contains('Inception').should('be.visible');
    cy.get('[data-cy=eventHistoryChart]').contains('Project 1').should('be.visible');
    cy.get('[data-cy=eventHistoryChart]').contains('Project 2').should('not.be.visible');
    cy.get('[data-cy=eventHistoryChart]').contains('Project 3').should('be.visible');
    cy.get('[data-cy=eventHistoryChart]').contains('Project 4').should('not.be.visible');

    // add a project
    cy.get('[data-cy=eventHistoryChartProjectSelector]').click()
    cy.get('[data-cy=eventHistoryChartProjectSelector]').contains('Project 4').click()
    cy.get('[data-cy=eventHistoryChart]').contains('Inception').should('be.visible');
    cy.get('[data-cy=eventHistoryChart]').contains('Project 1').should('be.visible');
    cy.get('[data-cy=eventHistoryChart]').contains('Project 2').should('not.be.visible');
    cy.get('[data-cy=eventHistoryChart]').contains('Project 3').should('be.visible');
    cy.get('[data-cy=eventHistoryChart]').contains('Project 4').should('be.visible');

    // allows up to 5 projects
    cy.get('[data-cy=eventHistoryChartProjectSelector]').click()
    cy.get('[data-cy=eventHistoryChartProjectSelector]').contains('Project 2').click()
    cy.get('[data-cy=eventHistoryChart]').contains('Inception').should('be.visible');
    cy.get('[data-cy=eventHistoryChart]').contains('Project 1').should('be.visible');
    cy.get('[data-cy=eventHistoryChart]').contains('Project 2').should('be.visible');
    cy.get('[data-cy=eventHistoryChart]').contains('Project 3').should('be.visible');
    cy.get('[data-cy=eventHistoryChart]').contains('Project 4').should('be.visible');
    cy.get('[data-cy=eventHistoryChartProjectSelector]').click()
    cy.get('[data-cy=eventHistoryChartProjectSelector]').contains('Maximum of 5 options selected');

    // remove all 5 projects
    for (let i=0; i<5; i+= 1) {
      cy.get('@removeBtns').eq(0).click()
    }
    cy.get('[data-cy=eventHistoryChart]').contains('Please select at least one project from the list above.');
  });

});

