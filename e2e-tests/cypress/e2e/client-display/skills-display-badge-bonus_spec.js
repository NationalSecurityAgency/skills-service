describe('Skills Display Badge Bonus Tests', () => {

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

  it('badge achieved in third place', () => {
    cy.createBadge(1, 2)
    cy.assignSkillToBadge(1, 2, 5);
    cy.createBadge(1, 2, { enabled: true})
    cy.reportSkill(1, 5, 'user5', '2019-09-14 11:00');
    cy.reportSkill(1, 5, 'user6', '2019-09-17 11:00');
    cy.reportSkill(1, 5, Cypress.env('proxyUser')); // achieve badge 2

    cy.reportSkill(1, 1, 'user5', '2019-09-14 11:00');
    cy.reportSkill(1, 1, 'user5', '2019-09-15 11:00');
    cy.reportSkill(1, 1, 'user5', '2019-09-16 11:00');
    cy.reportSkill(1, 1, 'user5', '2019-09-17 11:00');
    cy.reportSkill(1, 1, 'user5', '2019-09-18 11:00');

    cy.cdVisit('/', true);
    cy.cdClickBadges();
    cy.get('[data-cy=achievedBadges]')
      .contains('Badge 2');
    cy.get('[data-cy=availableBadges]')
      .contains('Badge 1');

    cy.get('[data-cy="achievedBadge-badge2"]').contains('3rd place')
    cy.get('[data-cy="achievedBadge-badge2"] [data-cy="earnedBadgeLink_badge2"]').click();

    cy.get('[data-cy=badge_badge2]').contains("2 other people have achieved this badge so far - and you were the third!")
  });

  it('badge achieved second place', () => {
    cy.createBadge(1, 2)
    cy.assignSkillToBadge(1, 2, 5);
    cy.createBadge(1, 2, { enabled: true})

    cy.reportSkill(1, 5, 'user5', '2019-09-14 11:00');
    cy.reportSkill(1, 5, Cypress.env('proxyUser'), '2019-09-15 11:00'); // achieve badge 2
    cy.reportSkill(1, 5, 'user6', '2019-09-17 11:00');

    cy.reportSkill(1, 1, 'user5', '2019-09-14 11:00');
    cy.reportSkill(1, 1, 'user5', '2019-09-15 11:00');
    cy.reportSkill(1, 1, 'user5', '2019-09-16 11:00');
    cy.reportSkill(1, 1, 'user5', '2019-09-17 11:00');
    cy.reportSkill(1, 1, 'user5', '2019-09-18 11:00');

    cy.reportSkill(1, 1, 'user6', '2019-09-14 11:00');
    cy.reportSkill(1, 1, 'user6', '2019-09-15 11:00');
    cy.reportSkill(1, 1, 'user6', '2019-09-16 11:00');
    cy.reportSkill(1, 1, 'user6', '2019-09-17 11:00');
    cy.reportSkill(1, 1, 'user6', '2019-09-18 11:00');

    cy.cdVisit('/', true);
    cy.cdClickBadges();
    cy.get('[data-cy=achievedBadges]')
      .contains('Badge 2');
    cy.get('[data-cy=availableBadges]')
      .contains('Badge 1');

    cy.get('[data-cy="achievedBadge-badge2"]').contains('2nd place')
    cy.get('[data-cy="achievedBadge-badge2"] [data-cy="earnedBadgeLink_badge2"]').click();

    cy.get('[data-cy=badge_badge2]').contains("2 other people have achieved this badge so far - and you were the second!")
  });

  it('badge with a bonus award in progress', () => {

    const anHourAgo = new Date().getTime() - (1000 * 60 * 60)
    const twoDays = (60 * 24 * 2)

    cy.createBadge(1, 2, { enabled: false, awardAttrs: {'iconClass': 'fas fa-car', 'name': 'Test Badge Award', 'numMinutes': twoDays}})
    cy.assignSkillToBadge(1, 2, 5);
    cy.assignSkillToBadge(1, 2, 1);
    cy.createBadge(1, 2, { enabled: true, awardAttrs: {'iconClass': 'fas fa-car', 'name': 'Test Badge Award', 'numMinutes': twoDays}})

    cy.reportSkill(1, 5, Cypress.env('proxyUser'), anHourAgo); // achieve badge 2

    cy.cdVisit('/', true);
    cy.cdClickBadges();
    cy.get('[data-cy=availableBadges]')
      .contains('Badge 2');
    cy.get('[data-cy=availableBadges]')
      .contains('Badge 1');
    const expectedMsg = "Achieve this badge in 23 hours and 59 minutes for the Test Badge Award bonus!"
    cy.get('[data-cy="badge_badge2"] [data-cy="achieveThisBadgeMsg"]').contains(expectedMsg)

    cy.get('[data-cy="badgeDetailsLink_badge2"]').click();
    cy.get('[data-cy="title"]').contains('Badge Details')
    cy.get('[data-cy=badge_badge2]').contains(expectedMsg)
  });

  it('badge with a bonus award completed', () => {

    const anHourAgo = new Date().getTime() - (1000 * 60 * 60)
    const twoDays = (60 * 24 * 2)

    cy.createBadge(1, 2, { enabled: false, awardAttrs: {'iconClass': 'fas fa-car', 'name': 'Test Badge Award', 'numMinutes': twoDays}})
    cy.assignSkillToBadge(1, 2, 5);
    cy.createBadge(1, 2, { enabled: true, awardAttrs: {'iconClass': 'fas fa-car', 'name': 'Test Badge Award', 'numMinutes': twoDays}})

    cy.reportSkill(1, 5, Cypress.env('proxyUser'), anHourAgo); // achieve badge 2

    cy.cdVisit('/', true);
    cy.cdClickBadges();
    cy.get('[data-cy=achievedBadges]')
      .contains('Badge 2');
    cy.get('[data-cy=availableBadges]')
      .contains('Badge 1');

    cy.get('[data-cy="achievedBadge-badge2"]').contains('Test Badge Award')

    cy.get('[data-cy="earnedBadgeLink_badge2"]').click();
    cy.get('[data-cy="title"]').contains('Badge Details')
    cy.get('[data-cy="badge_badge2"]').contains('Test Badge Award')
  });

  it.only('two badges and one has a bonus', () => {
    const anHourAgo = new Date().getTime() - (1000 * 60 * 60)
    const twoDays = (60 * 24 * 2)

    cy.createBadge(1, 2, { enabled: false, awardAttrs: {'iconClass': 'fas fa-car', 'name': 'Test Badge Award', 'numMinutes': twoDays}})
    cy.assignSkillToBadge(1, 2, 5);
    cy.createBadge(1, 2, { enabled: true, awardAttrs: {'iconClass': 'fas fa-car', 'name': 'Test Badge Award', 'numMinutes': twoDays}})

    cy.createBadge(1, 3)
    cy.assignSkillToBadge(1, 3, 5);
    cy.createBadge(1, 3, { enabled: true })

    cy.reportSkill(1, 5, Cypress.env('proxyUser'), anHourAgo); // achieve badge 2

    cy.cdVisit('/', true);
    cy.cdClickBadges();
    cy.get('[data-cy=achievedBadges]')
      .contains('Badge 2');
    cy.get('[data-cy=availableBadges]')
      .contains('Badge 1');

    cy.get('[data-cy="achievedBadge-badge2"]').contains('Test Badge Award')

    cy.matchSnapshotImageForElement('[data-cy="achievedBadges"]')
  });

  it('ten badges and one has a bonus', () => {
    const anHourAgo = new Date().getTime() - (1000 * 60 * 60)
    const twoDays = (60 * 24 * 2)

    cy.createBadge(1, 2, { enabled: false, awardAttrs: {'iconClass': 'fas fa-car', 'name': 'Test Badge Award', 'numMinutes': twoDays}})
    cy.assignSkillToBadge(1, 2, 5);
    cy.createBadge(1, 2, { enabled: true, awardAttrs: {'iconClass': 'fas fa-car', 'name': 'Test Badge Award', 'numMinutes': twoDays}})

    for(let i = 3; i < 13; i++) {
      cy.createBadge(1, i)
      cy.assignSkillToBadge(1, i, 5);
      cy.createBadge(1, i, { enabled: true })
    }

    cy.reportSkill(1, 5, Cypress.env('proxyUser'), anHourAgo); // achieve badge 2

    cy.cdVisit('/', true);
    cy.cdClickBadges();
    cy.get('[data-cy=achievedBadges]')
      .contains('Badge 2');
    cy.get('[data-cy=availableBadges]')
      .contains('Badge 1');

    cy.get('[data-cy="achievedBadge-badge2"]').contains('Test Badge Award')

    for(let i = 2; i < 13; i++) {
      cy.get(`[data-cy="earnedBadgeLink_badge${i}"]`)
    }

    cy.matchSnapshotImageForElement('[data-cy="achievedBadges"]')
  });

})