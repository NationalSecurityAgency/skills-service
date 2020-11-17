var moment = require('moment-timezone');

describe('Accessibility Tests', () => {
  it('home page', () => {
    cy.visit('/');
    cy.customLighthouse();
    cy.customPa11y();

    cy.contains('Metrics').click();
    cy.customLighthouse();
    cy.customPa11y();
  });

  it('project', () => {
    cy.request('POST', '/app/projects/MyNewtestProject', {
      projectId: 'MyNewtestProject',
      name: "My New test Project"
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

    cy.request('POST', `/admin/projects/MyNewtestProject/skills/skill2/dependency/skill1`)

    const m = moment('2020-09-12 11', 'YYYY-MM-DD HH');
    cy.request('POST', `/api/projects/MyNewtestProject/skills/skill1`, {userId: 'u1', timestamp: m.format('x')})
    cy.request('POST', `/api/projects/MyNewtestProject/skills/skill1`, {userId: 'u2', timestamp: m.subtract(4, 'day').format('x')})
    cy.request('POST', `/api/projects/MyNewtestProject/skills/skill1`, {userId: 'u3', timestamp: m.subtract(3, 'day').format('x')})
    cy.request('POST', `/api/projects/MyNewtestProject/skills/skill1`, {userId: 'u4', timestamp: m.subtract(2, 'day').format('x')})
    cy.request('POST', `/api/projects/MyNewtestProject/skills/skill1`, {userId: 'u5', timestamp: m.subtract(1, 'day').format('x')})

    cy.visit('/');
    //view project
    cy.contains('Manage').click();
    cy.customLighthouse();
    cy.customPa11y();

    cy.get('[data-cy=nav-Badges]').click();
    cy.customLighthouse();
    cy.customPa11y();

    cy.get('[data-cy=nav-Dependencies]').click();
    cy.customLighthouse();
    cy.customPa11y();

    //levels
    cy.get('[data-cy=nav-Levels').click();
    cy.customLighthouse();
    cy.customPa11y();

    //users
    cy.get('[data-cy=nav-Users').click();
    cy.customLighthouse();
    cy.customPa11y();

    //metrics
    cy.get('[data-cy=nav-Metrics').click();
    cy.customLighthouse();
    cy.customPa11y();

    cy.get('[data-cy=nav-Access').click();
    cy.customLighthouse();
    cy.customPa11y();

    cy.get('[data-cy=nav-Settings]').click();
    cy.customLighthouse();
    cy.customPa11y();


  })
});
