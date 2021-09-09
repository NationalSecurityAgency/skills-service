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
import dayjs from "dayjs";

const moment = require('moment-timezone');

describe('Accessibility Tests', () => {

  /*after(() => {
    cy.task('createAverageAccessibilityScore');
  });*/

  beforeEach(() => {
    cy.request('POST', '/app/projects/MyNewtestProject', {
      projectId: 'MyNewtestProject',
      name: "My New test Project"
    });

    cy.request('POST', '/app/projects/MyNewtestProject2', {
      projectId: 'MyNewtestProject2',
      name: "My New test Project2"
    });

    cy.request('POST', '/admin/projects/MyNewtestProject/subjects/subj1', {
      projectId: 'MyNewtestProject',
      subjectId: 'subj1',
      name: "Subject 1"
    });

    cy.request('POST', '/admin/projects/MyNewtestProject/badges/badge1', {
      projectId: 'MyNewtestProject',
      badgeId: 'badge1',
      name: "Badge 1"
    });

    cy.request('POST', `/admin/projects/MyNewtestProject/subjects/subj1/skills/skill1`, {
      projectId: 'MyNewtestProject',
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

    cy.request('POST', `/admin/projects/MyNewtestProject/subjects/subj1/skills/skill2`, {
      projectId: 'MyNewtestProject',
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

    cy.request('POST', `/admin/projects/MyNewtestProject/subjects/subj1/skills/skill3`, {
      projectId: 'MyNewtestProject',
      subjectId: 'subj1',
      skillId: 'skill3',
      name: `This is 3`,
      type: 'Skill',
      pointIncrement: 100,
      numPerformToCompletion: 5,
      pointIncrementInterval: 0,
      numMaxOccurrencesIncrementInterval: -1,
      description: "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
      version: 0,
      helpUrl: 'http://doHelpOnThisSkill.com',
      selfReportingType: 'Approval'
    });
    cy.request('POST', `/admin/projects/MyNewtestProject/subjects/subj1/skills/skill4`, {
      projectId: 'MyNewtestProject',
      subjectId: 'subj1',
      skillId: 'skill4',
      name: `This is 4`,
      type: 'Skill',
      pointIncrement: 100,
      numPerformToCompletion: 5,
      pointIncrementInterval: 0,
      numMaxOccurrencesIncrementInterval: -1,
      description: "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
      version: 0,
      helpUrl: 'http://doHelpOnThisSkill.com',
    });

    cy.request('POST', '/admin/projects/MyNewtestProject/badge/badge1/skills/skill2')

    cy.request('POST', `/admin/projects/MyNewtestProject/skills/skill2/dependency/skill1`)

    const m = moment('2020-05-12 11', 'YYYY-MM-DD HH');
    cy.request('POST', `/api/projects/MyNewtestProject/skills/skill1`, {userId: 'u1', timestamp: m.format('x')})
    cy.request('POST', `/api/projects/MyNewtestProject/skills/skill1`, {userId: 'u2', timestamp: m.subtract(4, 'day').format('x')})
    cy.request('POST', `/api/projects/MyNewtestProject/skills/skill1`, {userId: 'u3', timestamp: m.subtract(3, 'day').format('x')})
    cy.request('POST', `/api/projects/MyNewtestProject/skills/skill1`, {userId: 'u4', timestamp: m.subtract(2, 'day').format('x')})
    cy.request('POST', `/api/projects/MyNewtestProject/skills/skill1`, {userId: 'u5', timestamp: m.subtract(1, 'day').format('x')})
    cy.request('POST', `/api/projects/MyNewtestProject/skills/skill3`, {userId: 'u5', timestamp: m.subtract(1, 'day').format('x')})
    cy.request('POST', `/api/projects/MyNewtestProject/skills/skill3`, {userId: 'u6', timestamp: m.subtract(1, 'day').format('x')})
    cy.request('POST', `/api/projects/MyNewtestProject/skills/skill3`, {userId: 'u7', timestamp: m.subtract(1, 'day').format('x')})

    cy.request('POST', `/api/projects/MyNewtestProject/skills/skill4`, {userId: 'u8', timestamp: m.subtract(1, 'day').format('x')})
    cy.request('POST', `/api/projects/MyNewtestProject/skills/skill4`, {userId: 'u8', timestamp: m.subtract(2, 'day').format('x')})
    cy.request('POST', `/api/projects/MyNewtestProject/skills/skill4`, {userId: 'u8', timestamp: m.subtract(3, 'day').format('x')})
    cy.request('POST', `/api/projects/MyNewtestProject/skills/skill4`, {userId: 'u8', timestamp: m.subtract(4, 'day').format('x')})
    cy.request('POST', `/api/projects/MyNewtestProject/skills/skill4`, {userId: 'u8', timestamp: m.subtract(5, 'day').format('x')})
  });

  it('"My Progress" landing page', () => {
    // setup a project for the landing page
    const dateFormatter = value => moment.utc(value).format('YYYY-MM-DD[T]HH:mm:ss[Z]');
    const timeFromNowFormatter = (value) => dayjs(value).startOf('hour').fromNow();
    cy.createProject(1);
    cy.enableProdMode(1);
    cy.addToMyProjects(1)
    cy.createSubject(1, 1);
    cy.createSubject(1, 2);
    cy.createSubject(1, 3);
    cy.createSkill(1, 1, 1);
    cy.createSkill(1, 1, 2);
    cy.createSkill(1, 1, 3);
    cy.createSkill(1, 1, 4);

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

    cy.visit('/progress-and-rankings');
    cy.get('[data-cy="breadcrumb-Progress And Rankings"]').contains('Progress And Rankings').should('be.visible');
    cy.get('[data-cy=numSkillsAvailable]').contains(new RegExp(/^Total: 4$/));
    cy.get('[data-cy=project-link-proj1]').should('be.visible');

    cy.customLighthouse();
    cy.injectAxe()
    cy.customA11y();
  });

  it('admin home page', () => {
    cy.visit('/administrator/');
    cy.customLighthouse();
    cy.injectAxe()
    cy.get('[data-cy=nav-Projects]')
    cy.contains('My New test Project')
    cy.customA11y();
  });

  it('project', () => {
    //report skills that dont' exist
    cy.reportSkill('MyNewtestProject', 42, 'user@skills.org', '2021-02-24 10:00', false);
    cy.reportSkill('MyNewtestProject', 75, 'user@skills.org', '2021-02-24 10:00', false);
    cy.reportSkill('MyNewtestProject', 75, 'user@skills.org', '2021-02-24 10:00', false);
    cy.reportSkill('MyNewtestProject', 75, 'user@skills.org', '2021-02-24 10:00', false);
    cy.reportSkill('MyNewtestProject', 13, 'user@skills.org', '2021-02-24 10:00', false);


    cy.visit('/administrator/');
    cy.injectAxe()
    //view project
    cy.get('[data-cy=projCard_MyNewtestProject_manageBtn]').click();
    // wait on subjects
    cy.get('[data-cy=manageBtn_subj1]')

    cy.customLighthouse();
    cy.get('[aria-label="new subject"]').click();
    cy.get('[data-cy=subjectNameInput]').type('a')
    cy.customA11y();
    cy.get('[data-cy=closeSubjectButton]').click();

    cy.get('[data-cy=nav-Badges]').click();
    cy.contains('Badge 1');

    cy.customLighthouse();
    cy.get('[aria-label="new badge"]').click();
    cy.get('[data-cy=badgeName').type('a');
    cy.customA11y();
    cy.get('[data-cy=closeBadgeButton]').click();


    // --- Self Report Page ----
    cy.get('[data-cy="nav-Self Report"]').click();
    cy.get('[data-cy="skillsReportApprovalTable"] tbody tr').should('have.length', 3);
    cy.customLighthouse();
    cy.customA11y();

    cy.get('[data-cy="selectPageOfApprovalsBtn"]').click();
    cy.get('[data-cy="rejectBtn"]').click();
    cy.get('[data-cy="rejectionTitle"]').contains('This will permanently reject user\'s request(s) to get points')
    cy.wait(500); // wait for modal to continue loading, if background doesn't load the contract checks will fail
    cy.customA11y();
    cy.get('[data-cy="cancelRejectionBtn"]').click();

    // --- Deps Page ----
    cy.get('[data-cy=nav-Dependencies]').click();
    cy.contains('Color Legend');
    cy.customLighthouse();
    cy.customA11y();

    //levels
    cy.get('[data-cy=nav-Levels').click();
    cy.contains('White Belt');
    cy.customLighthouse();
    cy.get('[data-cy=addLevel]').click();
    cy.get('[data-cy=levelPercent]').type('1100')
    cy.customA11y();
    cy.get('[data-cy=cancelLevel]').click();

    //users
    cy.get('[data-cy=nav-Users').click();
    cy.contains('ID: MyNewtestProject');
    cy.get('[data-cy="usersTable"]').contains('u1');
    cy.contains('User Id Filter');
    cy.contains('Total Rows: 6')
    cy.customLighthouse();
    cy.customA11y();

    // --- metrics ----
    cy.get('[data-cy=nav-Metrics]').click();
    cy.contains('Users per day');
    cy.contains('This chart needs at least 2 days of user activity.')
    cy.customLighthouse();
    cy.customA11y();

    cy.get('[data-cy="Achievements-metrics-link"]').click();
    cy.contains("Level 2: 1 users")
    cy.contains("Level 1: 0 users")
    cy.get('[data-cy=achievementsNavigator-table]').contains('u8');
    cy.wait(2000) // wait for charts to finish loading
    cy.customLighthouse();
    cy.customA11y();

    cy.get('[data-cy="Subjects-metrics-link"]').click();
    cy.contains('Number of users for each level over time')
    cy.wait(4000) // wait for charts to finish loading
    cy.customLighthouse();
    cy.customA11y();

    cy.get('[data-cy="Skills-metrics-link"]').click();
    cy.get('[data-cy=skillsNavigator-table]').contains('This is 1')
    cy.customLighthouse();
    cy.customA11y();

    // --- access page ----
    cy.get('[data-cy=nav-Access').click();
    cy.contains('Trusted Client Properties');
    cy.contains('ID: MyNewtestProject');
    const tableSelector = '[data-cy="roleManagerTable"]'
    cy.get(tableSelector).contains('Loading...').should('not.exist')
    cy.get(tableSelector).contains('There are no records to show').should('not.exist')
    cy.get(`${tableSelector} tbody tr`).should('have.length', 1)
    cy.customLighthouse();
    cy.customA11y();

    cy.get('[data-cy=nav-Settings]').click();
    cy.contains('Root Help Url');
    cy.customLighthouse();
    cy.customA11y();

    // --- Issues page ---
    cy.intercept('GET', '/admin/projects/MyNewtestProject/errors').as('getErrors');
    cy.get('[data-cy=nav-Issues]').click();
    cy.wait('@getErrors');
    cy.contains('Remove All');
    cy.customLighthouse();
    cy.customA11y();
  })


  it('subject', () => {
    cy.server();
    cy.route('GET', '/admin/projects/MyNewtestProject/subjects').as('getSubjects');
    cy.route('GET', '/admin/projects/MyNewtestProject/subjects/subj1/skills').as('getSkills');
    cy.route('GET', '/admin/projects/MyNewtestProject/subjects/subj1/levels').as('getLevels');
    cy.route('GET', '/admin/projects/MyNewtestProject/subjects/subj1/users?**').as('getUsers');
    cy.route('GET', '/admin/projects/MyNewtestProject/performedSkills/u1?**').as('getPerformedSkills');

    cy.visit('/administrator/');
    cy.injectAxe()
    //view project
    cy.get('[data-cy=projCard_MyNewtestProject_manageBtn]').click();
    cy.wait('@getSubjects');
    //view subject
    cy.get('[data-cy=manageBtn_subj1]').click();
    cy.wait('@getSkills');
    cy.contains('This is 2');
    cy.customLighthouse();
    cy.customA11y();

    //edit skill
    cy.get('[aria-label="new skill"]').click();
    cy.get('[data-cy=skillName]').type('1');
    cy.contains('Skill Name cannot be less than 3 characters.');
    // it seems like bootstrap vue has a bug where it assigns the dialog role to the outer_modal div with no aria-label
    // or aria-labelledby and then assigns the role="dialog" to the inner div along with the required aria attributes
    // there isn't anything we can do to fix that so we have to skip this check at this time
    // cy.customA11y();
    cy.get('[data-cy=closeSkillButton]').click();

    cy.get('[data-cy=nav-Levels]').click();
    cy.wait('@getLevels');
    cy.contains('Black Belt');
    cy.customLighthouse();
    cy.customA11y();
    cy.get('[data-cy=addLevel]').click();
    cy.get('[data-cy=levelPercent]').type('105');
    cy.contains('Percent % must be 100 or less');
    cy.customA11y();
    cy.get('[data-cy=cancelLevel]').click();
    cy.get('[data-cy=editLevelButton]').eq(0).click();
    cy.get('[data-cy=levelPercent]').type('ddddddddd');
    cy.contains('Percent may only contain numeric characters');
    cy.customA11y();
    cy.get('[data-cy=cancelLevel]').click();

    cy.clickNav('Users').click();
    cy.contains('u1');
    cy.wait('@getUsers');
    cy.customLighthouse();
    cy.customA11y();
    cy.get('[data-cy="usersTable"] [data-cy="usersTable_viewDetailsBtn"]').first().click();
    cy.wait(4000);
    cy.contains('Client Display');
    cy.customLighthouse();
    // enable once a11y issues with client display are addressed, needs an H1 initially
    // also has contrast issues
    // cy.customA11y();
    cy.get('[data-cy="nav-Performed Skills"]').click()
    cy.wait('@getPerformedSkills');
    cy.contains('skill1');
    cy.customLighthouse();
    cy.customA11y();

    cy.get('[data-cy=breadcrumb-subj1]').click();
    cy.get('[data-cy=nav-Metrics]').click();
    cy.contains('This chart needs at least 2 days of user activity.');
    cy.contains('Level 2: 1 users');
    cy.customLighthouse();
    cy.customA11y();
  })

  it('skills', () => {
    cy.visit('/administrator/');
    cy.injectAxe()
    //view project
    cy.get('[data-cy="projCard_MyNewtestProject_manageBtn"]').click();
    //view subject
    cy.get('[data-cy="manageBtn_subj1"]').click();
    //view skill
    cy.get('[data-cy=manageSkillBtn_skill1]').click();
    cy.contains('Help URL');
    cy.contains('http://doHelpOnThisSkill.com');
    cy.contains('Lorem ipsum dolor sit amet, consectetur adipiscing elit,');
    cy.contains('500 Points');
    cy.customLighthouse();
    cy.customA11y();

    cy.clickNav('Dependencies');
    cy.contains('No Dependencies Yet');
    cy.customLighthouse();
    cy.customA11y();

    cy.get('[data-cy=nav-Users]').click();
    cy.contains('u1');
    cy.customLighthouse();
    cy.customA11y();

    cy.get('[data-cy="nav-Add Event"]').click();
    cy.contains('Enter user id');
    cy.customLighthouse();
    cy.customA11y();
    cy.get('.multiselect__select').click();
    cy.get('.multiselect__element').eq(0).click();
    cy.get('[data-cy="eventDatePicker"]').click()
    cy.get('.vdp-datepicker__calendar .prev').first().click()
    cy.get('.vdp-datepicker__calendar .prev').first().click()
    cy.get('.vdp-datepicker__calendar .prev').first().click()
    cy.get('.vdp-datepicker__calendar .prev').first().click()
    cy.get('.vdp-datepicker__calendar .prev').first().click()
    cy.get('.vdp-datepicker__calendar .prev').first().click()
    cy.get('.vdp-datepicker__calendar').contains('10').click()
    cy.get('[data-cy=addSkillEventButton]').click();
    cy.contains('Added points')
    cy.customA11y();

    cy.get('[data-cy=nav-Metrics]').click();
    cy.contains('Achievements over time');
    cy.get('[data-cy="numUsersPostAchievement"]').contains('No achievements yet for this skill.');
    cy.get('[data-cy="numUsersPostAchievement"]').contains('No achievements yet for this skill.');

    cy.contains('This chart needs at least 2 days of user activity.');
    cy.customLighthouse();
    cy.customA11y();
  })

  it('badges', ()=>{
    cy.visit('/administrator/');
    cy.injectAxe()

    cy.get('[data-cy="projCard_MyNewtestProject_manageBtn"]').click();
    cy.clickNav('Badges').click();

    cy.get('[aria-label="new badge"]').click();
    cy.get('[data-cy=badgeName]').type('a');
    cy.customA11y();
    cy.get('[data-cy=closeBadgeButton]').click();

    cy.get('[data-cy=manageBtn_badge1]').click();
    cy.contains('This is 2')
    cy.customLighthouse();
    cy.customA11y();

    cy.clickNav('Users').click();
    cy.contains('There are no records to show');
    cy.customLighthouse();
    cy.customA11y();
  });

  it('settings', ()=> {
    cy.logout();
    cy.login('root@skills.org', 'password');
    cy.visit('/settings')
    cy.contains('First Name')
    cy.injectAxe()
    cy.customLighthouse();
    cy.customA11y();

    cy.get('[data-cy="nav-Preferences"]').click();
    cy.contains('Preferences');
    cy.customLighthouse();
    cy.customA11y();

    cy.get('[data-cy=nav-Security]').click();
    cy.contains('Root Users Management');
    cy.get('[data-cy="supervisorrm"]').contains('There are no records to show');
    cy.customLighthouse();
    cy.customA11y();

    cy.get('[data-cy=nav-Email]').click();
    cy.contains('Email Connection Settings');
    cy.contains('TLS Disabled');
    cy.customLighthouse();
    cy.customA11y();

    cy.get('[data-cy=nav-System]').click();
    cy.contains('Public URL');
    cy.customLighthouse();
    cy.customA11y();
  });

  it('global badges', ()=>{
    cy.logout();
    cy.login('root@skills.org', 'password');

    cy.intercept('POST', ' /supervisor/badges/globalbadgeBadge/projects/MyNewtestProject/level/1').as('saveGlobalBadgeLevel');
    cy.request('PUT', `/root/users/root@skills.org/roles/ROLE_SUPERVISOR`);
    cy.visit('/administrator');
    cy.injectAxe()
    cy.get('[data-cy=nav-Badges]').click();
    cy.customLighthouse();
    cy.customA11y();

    cy.get('[aria-label="new global badge"]').click();
    cy.get('[data-cy=badgeName]').type('global badge');
    cy.get('[data-cy=saveBadgeButton]').click();
    cy.contains('Manage').click();
    cy.contains('This is 1');
    cy.customLighthouse();
    cy.customA11y();

    cy.get('.multiselect__select').click();
    cy.get('.multiselect__element').eq(0).click();
    cy.customA11y();
    cy.get('[data-cy=nav-Levels]').click();
    cy.contains('No Levels Added Yet');
    cy.customLighthouse();

    cy.get('.multiselect__select').eq(0).click();
    cy.get('.multiselect__element').eq(0).click();
    cy.get('.multiselect__select').eq(1).click();
    cy.get('.multiselect__element').eq(1).click();

    cy.get('[data-cy=addGlobalBadgeLevel]').click();
    cy.customA11y();
  });


  it('manage my projects page', ()=>{

    for (let i = 1; i <= 9; i += 1) {
      cy.createProject(i);
      cy.enableProdMode(i);
      if (i < 4) {
        cy.addToMyProjects(i)
      }
    }

    cy.visit('/progress-and-rankings');
    cy.injectAxe()
    cy.get('[data-cy="manageMyProjsBtn"]').click()

    cy.get('[data-cy="allProjectsCount"] [data-cy="mediaInfoCardTitle"]').contains('9');

    cy.wait(1500);
    cy.customLighthouse();
    cy.customA11y();
  });

  it('my usage page', ()=>{
    cy.fixture('vars.json').then((vars) => {
      cy.request('POST', '/logout');
      cy.register(Cypress.env('proxyUser'), vars.defaultPass, false);
      cy.loginAsProxyUser()
    });
    cy.loginAsProxyUser()

    for (let i = 1; i <= 3; i += 1) {
      cy.createProject(i);
      cy.enableProdMode(i);
      cy.addToMyProjects(i)

      cy.createSubject(i, 1);
      cy.createSkill(i, 1, 1);
      cy.createSkill(i, 1, 2);
    }

    const dateFormat = 'YYYY-MM-DD HH:mm';
    const user =  Cypress.env('proxyUser');
    cy.reportSkill(1, 1, user, moment.utc().subtract(2, 'days').format(dateFormat))
    cy.reportSkill(1, 1, user, 'yesterday')
    cy.reportSkill(1, 2, user, 'now')
    cy.reportSkill(1, 2, user, 'yesterday')

    cy.reportSkill(2, 1, user, moment.utc().subtract(5, 'days').format(dateFormat))
    cy.reportSkill(2, 1, user, moment.utc().subtract(6, 'days').format(dateFormat))
    cy.reportSkill(2, 2, user, moment.utc().subtract(6, 'days').format(dateFormat))
    cy.reportSkill(2, 2, user, moment.utc().subtract(3, 'days').format(dateFormat))


    cy.visit('/progress-and-rankings');
    cy.injectAxe()
    cy.get('[data-cy="viewUsageBtn"]').click()

    cy.contains('Your Daily Usage History');
    cy.contains('6 months');
    cy.get('[data-cy=eventHistoryChartProjectSelector]')
        .contains('This is project 2')
        .should('be.visible');

    cy.wait(1500);
    cy.customLighthouse();
    cy.customA11y();
  });


  it('self report admin page', () => {
    Cypress.Commands.add("rejectRequest", (requestNum = 0, rejectionMsg = 'Skill was rejected') => {
      cy.request('/admin/projects/proj1/approvals?limit=10&ascending=true&page=1&orderBy=userId')
          .then((response) => {
            cy.request('POST', '/admin/projects/proj1/approvals/reject', {
              skillApprovalIds: [response.body.data[requestNum].id],
              rejectionMessage: rejectionMsg,
            });
          });
    });
    Cypress.Commands.add("approveAllRequests", () => {
      cy.request('/admin/projects/proj1/approvals?limit=10&ascending=true&page=1&orderBy=userId')
          .then((response) => {
            response.body.data.forEach((item) => {
              cy.wait(200); // that way sort works properly
              cy.request('POST', '/admin/projects/proj1/approvals/approve', {
                skillApprovalIds: [item.id],
              });
            })
          });
    });

    cy.createProject(1);
    cy.createSubject(1, 1)
    cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
    cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
    cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });
    cy.reportSkill(1, 2, 'user6', '2020-09-11 11:00')
    cy.reportSkill(1, 2, 'user5', '2020-09-12 11:00')
    cy.reportSkill(1, 2, 'user4', '2020-09-13 11:00')
    cy.approveAllRequests();
    cy.reportSkill(1, 2, 'user2', '2020-09-16 11:00')
    cy.rejectRequest(0)

    cy.reportSkill(1, 3, 'user1', '2020-09-17 11:00')
    cy.reportSkill(1, 1, 'user0', '2020-09-18 11:00')
    cy.approveAllRequests();

    cy.reportSkill(1, 2, 'user3', '2020-09-14 11:00')
    cy.rejectRequest(0)

    cy.reportSkill(1, 2, 'user8', '2020-09-14 11:00')
    cy.reportSkill(1, 2, 'user9', '2020-09-14 11:00')


    cy.visit('/administrator/projects/proj1/self-report');
    cy.injectAxe()

    cy.customLighthouse();
    cy.customA11y();
  })

});
