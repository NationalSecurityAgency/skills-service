import dayjs from 'dayjs';
import relativeTimePlugin from 'dayjs/plugin/relativeTime';
import advancedFormatPlugin from 'dayjs/plugin/advancedFormat';

dayjs.extend(relativeTimePlugin);
dayjs.extend(advancedFormatPlugin);

describe('Last Achieved date tests', () => {

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

  it('display achieved date on skill overview page', () => {
    const m = dayjs('2020-09-12 11', 'YYYY-MM-DD HH');
    const orig = m.clone();

    for(let i = 0; i < 5; i++) {
      cy.request('POST', `/api/projects/proj1/skills/skill2`, {
        userId: Cypress.env('proxyUser'),
        timestamp: m.subtract(i, 'day').format('x')
      });
    }
    cy.cdVisit('/?internalBackButton=true', true);
    cy.cdClickSubj(0, 'Subject 1',true);
    cy.cdClickSkill(1);

    cy.get('[data-cy=achievementDate]').contains(`Achieved on ${orig.format('MMMM Do YYYY')}`);
    cy.get('[data-cy=achievementDate]').contains(`${orig.fromNow()}`);
    cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]').contains('500');

    cy.matchSnapshotImageForElement('[data-cy="skillsDisplayHome"]', {
      name: 'Skill-Overview-Achieved',
      blackout: '[data-cy=pointHistoryChart]'
    });
  });

  it('theme: display achieved date on skill overview page', () => {
    const m = dayjs('2020-09-12 11', 'YYYY-MM-DD HH');
    const orig = m.clone();

    for(let i = 0; i < 5; i++) {
      cy.request('POST', `/api/projects/proj1/skills/skill2`, {
        userId: Cypress.env('proxyUser'),
        timestamp: m.subtract(i, 'day').format('x')
      });
    }

    cy.cdVisit('/?enableTheme=true', true);
    cy.cdClickSubj(0, 'Subject 1',true);
    cy.cdClickSkill(1);

    cy.get('[data-cy=achievementDate]')
      .contains(`Achieved on ${orig.format('MMMM Do YYYY')}`);
    cy.get('[data-cy=achievementDate]')
      .contains(`${orig.fromNow()}`);
    cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]')
      .contains('500');

    cy.matchSnapshotImageForElement('[data-cy="skillsDisplayHome"]', {
      name: 'Skill-Overview-Achieved-Themed',
      blackout: '[data-cy=pointHistoryChart]'
    });
  });

  it('iphone resolution: display achieved date on skill overview page', () => {
    const m = dayjs('2020-09-12 11', 'YYYY-MM-DD HH');
    const orig = m.clone();

    for(let i = 0; i < 5; i++) {
      cy.request('POST', `/api/projects/proj1/skills/skill2`, {
        userId: Cypress.env('proxyUser'),
        timestamp: m.subtract(i, 'day').format('x')
      });
    }

    cy.setResolution('iphone-6');

    cy.cdVisit('/', true);
    cy.cdClickSubj(0, 'Subject 1',true);
    cy.cdClickSkill(1);

    cy.get('[data-cy=achievementDate]')
      .contains(`Achieved on ${orig.format('MMMM Do YYYY')}`);
    cy.get('[data-cy=achievementDate]')
      .contains(`${orig.fromNow()}`);
    cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]')
      .contains('500');

    cy.matchSnapshotImageForElement('[data-cy="skillsDisplayHome"]', {
      name: 'Skill-Overview-Achieved-iphone6',
      blackout: '[data-cy=pointHistoryChart]'
    });

  });

  it('ipad resolution: display achieved date on skill overview page', () => {
    const m = dayjs('2020-09-12 11', 'YYYY-MM-DD HH');
    const orig = m.clone();

    for(let i = 0; i < 5; i++) {
      cy.request('POST', `/api/projects/proj1/skills/skill2`, {
        userId: Cypress.env('proxyUser'),
        timestamp: m.subtract(i, 'day').format('x')
      });
    }
    cy.setResolution('ipad-2');

    cy.cdVisit('/', true);
    cy.cdClickSubj(0, 'Subject 1',true);
    cy.cdClickSkill(1);

    cy.get('[data-cy=achievementDate]')
      .contains(`Achieved on ${orig.format('MMMM Do YYYY')}`);
    cy.get('[data-cy=achievementDate]')
      .contains(`${orig.fromNow()}`);
    cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]')
      .contains('500');

    cy.matchSnapshotImageForElement('[data-cy="skillsDisplayHome"]', {
      name: 'Skill-Overview-Achieved-ipad2',
      blackout: '[data-cy=pointHistoryChart]'
    });
  });

  it('display achieved date on subject page when skill details are expanded', () => {
    const m = dayjs('2020-09-12 11', 'YYYY-MM-DD HH');
    const orig = m.clone();
    cy.request('POST', `/api/projects/proj1/skills/skill2`, {
      userId: Cypress.env('proxyUser'),
      timestamp: m.format('x')
    });
    cy.request('POST', `/api/projects/proj1/skills/skill2`, {
      userId: Cypress.env('proxyUser'),
      timestamp: m.subtract(4, 'day')
        .format('x')
    });
    cy.request('POST', `/api/projects/proj1/skills/skill2`, {
      userId: Cypress.env('proxyUser'),
      timestamp: m.subtract(3, 'day')
        .format('x')
    });
    cy.request('POST', `/api/projects/proj1/skills/skill2`, {
      userId: Cypress.env('proxyUser'),
      timestamp: m.subtract(2, 'day')
        .format('x')
    });
    cy.request('POST', `/api/projects/proj1/skills/skill2`, {
      userId: Cypress.env('proxyUser'),
      timestamp: m.subtract(1, 'day')
        .format('x')
    });
    cy.cdVisit('/', true);
    cy.cdClickSubj(0, 'Subject 1', true);

    cy.get('[data-cy=toggleSkillDetails]')
      .click();
    cy.get('[data-cy=skillProgress_index-1] [data-cy=achievementDate]')
      .contains(`Achieved on ${orig.format('MMMM Do YYYY')}`);
    cy.get('[data-cy=skillProgress_index-1] [data-cy=achievementDate]')
      .contains(`${orig.fromNow()}`);
  });
})
