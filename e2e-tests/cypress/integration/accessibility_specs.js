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
const moment = require('moment-timezone');

describe('Accessibility Tests', () => {

  after(() => {
    cy.task('createAverageAccessibilityScore');
  });

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

    cy.request('POST', '/admin/projects/MyNewtestProject/badge/badge1/skills/skill2')

    cy.request('POST', `/admin/projects/MyNewtestProject/skills/skill2/dependency/skill1`)

    const m = moment('2020-09-12 11', 'YYYY-MM-DD HH');
    cy.request('POST', `/api/projects/MyNewtestProject/skills/skill1`, {userId: 'u1', timestamp: m.format('x')})
    cy.request('POST', `/api/projects/MyNewtestProject/skills/skill1`, {userId: 'u2', timestamp: m.subtract(4, 'day').format('x')})
    cy.request('POST', `/api/projects/MyNewtestProject/skills/skill1`, {userId: 'u3', timestamp: m.subtract(3, 'day').format('x')})
    cy.request('POST', `/api/projects/MyNewtestProject/skills/skill1`, {userId: 'u4', timestamp: m.subtract(2, 'day').format('x')})
    cy.request('POST', `/api/projects/MyNewtestProject/skills/skill1`, {userId: 'u5', timestamp: m.subtract(1, 'day').format('x')})
  });

  it('home page', () => {
    cy.visit('/');
    cy.customLighthouse();
    cy.injectAxe()
    cy.get('[data-cy=nav-Projects]').click();
    cy.get('[data-cy=newProjectButton]').click();
    cy.get('[data-cy=projectName]').type('a');
    cy.customA11y();
  });

  it('project', () => {

    cy.intercept(
      {
        path:
          '/admin/projects/MyNewtestProject/metrics/userAchievementsChartBuilder?pageSize=5&currentPage=1&usernameFilter=&fromDayFilter=&toDayFilter=&nameFilter=&minLevel=&achievementTypes=Overall,Subject,Skill,Badge&sortBy=achievedOn&sortDesc=true'
      }
    ).as('userAchievementMetrics');
    cy.intercept('GET', '/admin/projects/MyNewtestProject/metrics/skillUsageNavigatorChartBuilder').as('skillUsageMetrics');
    cy.intercept('GET', '/admin/projects/MyNewtestProject/metrics/numUsersPerSubjectPerLevelChartBuilder').as('subjectMetrics');
    cy.visit('/');
    cy.injectAxe()
    //view project
    cy.contains('Manage').click();
    // wait on subjects
    cy.get('[data-cy=subjCard_subj1_manageBtn]')

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
    cy.get('[data-cy="usersTable"]');
    cy.contains('User Id Filter');
    cy.customLighthouse();
    cy.customA11y();

    //metrics
    cy.get('[data-cy=nav-Metrics').click();
    cy.contains('Users per day');
    cy.customLighthouse();
    cy.customA11y();

    cy.get('[data-cy="Achievements-metrics-link"]').click();
    cy.wait('@userAchievementMetrics');
    cy.wait(250);//just because
    cy.customA11y();
    cy.get('[data-cy="Subjects-metrics-link"]').click();
    cy.wait('@subjectMetrics');
    cy.customA11y();
    cy.get('[data-cy="Skills-metrics-link"]').click();
    cy.wait('@skillUsageMetrics');
    cy.customA11y();

    cy.get('[data-cy=nav-Access').click();
    cy.contains('Trusted Client Properties');
    cy.contains('ID: MyNewtestProject');
    cy.customLighthouse();
    cy.customA11y();

    cy.get('[data-cy=nav-Settings]').click();
    cy.contains('Root Help Url');
    cy.customLighthouse();
    cy.customA11y();
  })

  it('subject', () => {
    cy.server();
    cy.route('GET', '/admin/projects/MyNewtestProject/subjects').as('getSubjects');
    cy.route('GET', '/admin/projects/MyNewtestProject/subjects/subj1/skills').as('getSkills');
    cy.route('GET', '/admin/projects/MyNewtestProject/subjects/subj1/levels').as('getLevels');
    cy.route('GET', '/admin/projects/MyNewtestProject/subjects/subj1/users?**').as('getUsers');
    cy.route('GET', '/admin/projects/MyNewtestProject/performedSkills/u1?query=&limit=10&ascending=0&page=1&byColumn=0&orderBy=performedOn').as('getPerformedSkills');

    cy.visit('/');
    cy.injectAxe()
    //view project
    cy.get('[data-cy=projCard_MyNewtestProject_manageBtn]').click();
    cy.wait('@getSubjects');
    //view subject
    cy.get('[data-cy=subjCard_subj1_manageBtn]').click();
    cy.wait('@getSkills');
    cy.customLighthouse();
    cy.customA11y();

    //edit skill
    cy.get('[aria-label="new skill"]').click();
    cy.get('[data-cy=skillName]').type('1');
    // it seems like bootstrap vue has a bug where it assigns the dialog role to the outer_modal div with no aria-label
    // or aria-labelledby and then assigns the role="dialog" to the inner div along with the required aria attributes
    // there isn't anything we can do to fix that so we have to skip this check at this time
    // cy.customA11y();
    cy.get('[data-cy=closeSkillButton]').click();

    cy.get('[data-cy=nav-Levels]').click();
    cy.wait('@getLevels');
    cy.customLighthouse();
    cy.customA11y();
    cy.get('[data-cy=addLevel]').click();
    cy.get('[data-cy=levelPercent]').type('105');
    cy.customA11y();
    cy.get('[data-cy=cancelLevel]').click();
    cy.get('[data-cy=editLevelButton]').eq(0).click();
    cy.get('[data-cy=levelPercent]').type('ddddddddd');
    cy.customA11y();
    cy.get('[data-cy=cancelLevel]').click();

    cy.clickNav('Users').click();
    cy.customLighthouse();
    cy.wait('@getUsers');
    cy.customA11y();
    cy.get('[data-cy="usersTable"] [data-cy="usersTable_viewDetailsBtn"]').first().click();
    cy.customLighthouse();
    // enable once a11y issues with client display are addressed, needs an H1 initially
    // also has contrast issues
    // cy.customA11y();
    cy.contains('Client Display');
    cy.get('[data-cy="nav-Performed Skills"]').click()
    cy.wait('@getPerformedSkills');
    cy.customLighthouse();
    cy.customA11y();
    /*
    re-enable once skills-service#179 is resolved
    cy.get('[data-cy=breadcrumb-subj1]').click();
    cy.get('[data-cy=nav-Metrics]').click();
    cy.customLighthouse();
    cy.customA11y();*/
  })

  it('skills', () => {
    cy.visit('/');
    cy.injectAxe()
    //view project
    cy.contains('Manage').click();
    //view subject
    cy.contains('Manage').click();
    //view skill
    cy.get('[data-cy=manageSkillBtn]').eq(1).click();
    cy.contains('Help URL');
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
    cy.get('[data-cy=addSkillEventButton]').click();
    cy.customA11y();

    cy.get('[data-cy=nav-Metrics]').click();
    cy.contains('Achievements over time');
    cy.customLighthouse();
    cy.customA11y();
  })

  it('badges', ()=>{
    cy.visit('/');
    cy.injectAxe()
    cy.contains('Manage').click();
    cy.get('[data-cy=nav-Badges]').click();

    cy.get('[aria-label="new badge"]').click();
    cy.get('[data-cy=badgeName]').type('a');
    cy.customA11y();
    cy.get('[data-cy=closeBadgeButton]').click();

    cy.contains('Manage').click();
    cy.contains('This is 2')
    cy.customLighthouse();
    cy.customA11y();

    cy.get('[data-cy=nav-Users]').click();
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

    cy.get('[data-cy=nav-Security]').click();
    cy.contains('Root Users Management');
    cy.customLighthouse();
    cy.customA11y();

    cy.get('[data-cy=nav-Email]').click();
    cy.contains('Email Connection Settings');
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
    cy.visit("/");
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


});
