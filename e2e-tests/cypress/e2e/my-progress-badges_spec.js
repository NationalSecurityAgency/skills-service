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
import dayjs from 'dayjs';
import relativeTimePlugin from 'dayjs/plugin/relativeTime';
import utc from 'dayjs/plugin/utc';

dayjs.extend(relativeTimePlugin);
dayjs.extend(utc);

const dateFormatter = value => dayjs(value)
  .utc()
  .format('YYYY-MM-DD[T]HH:mm:ss[Z]');
const timeFromNowFormatter = (value) => dayjs(value)
  .startOf('seconds')
  .fromNow();

const testTime = new Date().getTime();
const yesterday = new Date().getTime() - (1000 * 60 * 60 * 24);

describe('My Progress Badges Tests', () => {

  beforeEach(() => {
    cy.log(`--------> ${testTime}`);
    cy.intercept('/api/metrics/allProjectsSkillEventsOverTimeMetricsBuilder**')
      .as('allSkillEventsForUser');

    cy.createProject(1);
    cy.enableProdMode(1);

    cy.createSubject(1, 1);
    cy.createSubject(1, 2);
    cy.createSubject(1, 3);

    cy.createSkill(1, 1, 1);
    cy.createSkill(1, 1, 2);
    cy.createSkill(1, 1, 3);
    cy.createSkill(1, 1, 4);
    cy.addLearningPathItem(1, 2, 4)

    const badge1 = {
      projectId: 'proj1',
      badgeId: 'badge1',
      name: 'Badge 1',
      enabled: 'true',
      iconClass: 'fa fa-question-circle',
    };
    cy.request('POST', '/admin/projects/proj1/badges/badge1', badge1);
    cy.assignSkillToBadge(1, 1, 1);
    cy.enableBadge(1, 1, badge1);

    const gemBadge = {
      projectId: 'proj1',
      badgeId: 'gemBadge',
      name: 'Gem Badge',
      enabled: 'true',
      iconClass: 'mi mi-ac-unit',
      startDate: dateFormatter(dayjs()
        .subtract(5, 'day')),
      endDate: dateFormatter(dayjs()
        .add(7, 'day')),
    };
    cy.request('POST', '/admin/projects/proj1/badges/gemBadge', gemBadge);
    cy.request('POST', `/admin/projects/proj1/badge/gemBadge/skills/skill4`);
    cy.enableBadge(1, 'gem', gemBadge);

    cy.request('POST', `/api/projects/proj1/skills/skill1`, {
      userId: Cypress.env('proxyUser'),
      timestamp: yesterday
    });
    cy.request('POST', `/api/projects/proj1/skills/skill1`, {
      userId: Cypress.env('proxyUser'),
      timestamp: testTime
    });

    cy.request('POST', `/api/projects/proj1/skills/skill3`, {
      userId: Cypress.env('proxyUser'),
      timestamp: yesterday
    });
    cy.request('POST', `/api/projects/proj1/skills/skill3`, {
      userId: Cypress.env('proxyUser'),
      timestamp: testTime
    });

    cy.createProject(2);
    cy.enableProdMode(2);
    cy.createSubject(2, 1);
    cy.createSubject(2, 2);
    cy.createSubject(2, 3);

    cy.createSkill(2, 1, 1);
    cy.createSkill(2, 1, 2);
    cy.createSkill(2, 1, 3);
    cy.createSkill(2, 1, 4);
    cy.createSkill(2, 1, 5);
    cy.createSkill(2, 1, 6);

    cy.loginAsRootUser();
    cy.request('POST', '/admin/projects/Inception/settings/production.mode.enabled', {
      projectId: 'Inception',
      setting: 'production.mode.enabled',
      value: 'true'
    });

    cy.fixture('vars.json')
      .then((vars) => {
        cy.request('POST', '/logout');
        cy.register(Cypress.env('proxyUser'), vars.defaultPass, false);
        cy.loginAsProxyUser();
      });
    cy.loginAsProxyUser();

    cy.addToMyProjects(1);
    cy.addToMyProjects(2);
  });

  it('project name should be visible on badges in badge catalog', () => {
    cy.visit('/progress-and-rankings/');
    cy.get('[data-cy=viewBadges]')
      .click();
    cy.get('[data-cy="availableBadges"] [data-cy="badge_gemBadge"] [data-cy=badgeProjectName]')
      .should('have.text', 'Project: This is project 1');
    cy.get('[data-cy="achievedBadge-badge1"] [data-cy=badgeProjectName]')
      .should('have.text', 'Project: This is project 1');
  });

  it('My Badges filtering', () => {
    cy.loginAsRootUser();
    const globalBadge1 = {
      badgeId: `globalBadge1`,
      isEdit: false,
      name: `Global Badge One`,
      originalBadgeId: '',
      iconClass: 'fas fa-award',
      enabled: true,
      description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.',
    };
    cy.request('PUT', `/app/badges/globalBadge1`, globalBadge1);
    cy.assignSkillToGlobalBadge(1, 2);
    cy.enableGlobalBadge(1, globalBadge1);

    cy.request('PUT', `/app/badges/globalBadge2`, {
      badgeId: `globalBadge2`,
      isEdit: false,
      name: `Global Badge two`,
      originalBadgeId: '',
      iconClass: 'fas fa-award',
      enabled: true,
      description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.',
    });
    cy.assignSkillToGlobalBadge(2, 3);
    cy.enableGlobalBadge(2);

    cy.request('POST', '/admin/projects/proj1/badges/badge2', {
      projectId: 'proj1',
      badgeId: 'badge2',
      name: 'Badge two',
      enabled: 'true',
    });
    cy.assignSkillToBadge(1, 2, 1);
    cy.enableBadge(1, 2);

    cy.request('POST', '/admin/projects/proj1/badges/badge11', {
      projectId: 'proj1',
      badgeId: 'badge11',
      name: 'Badge one one',
      enabled: 'true',
    });
    cy.assignSkillToBadge(1, 11, 1);
    cy.enableBadge(1, 11);

    const gemBadge2 = {
      projectId: 'proj1',
      badgeId: 'gemBadge2',
      name: 'Gem Badge Two',
      enabled: 'true',
      startDate: dateFormatter(dayjs()
        .subtract(5, 'day')),
      endDate: dateFormatter(dayjs()
        .add(7, 'day')),
    };
    cy.request('POST', '/admin/projects/proj1/badges/gemBadge2', gemBadge2);
    cy.request('POST', `/admin/projects/proj1/badge/gemBadge2/skills/skill4`);
    cy.enableBadge(1, 'gemBadge2', gemBadge2);

    cy.loginAsProxyUser();
    cy.visit('/progress-and-rankings/');
    cy.get('[data-cy=viewBadges]')
      .click();

    cy.get('[data-cy="badgeTitle"]').should('have.length', 3);

    cy.get('[data-cy=filterBtn]')
      .click();
    cy.get('[data-cy=filter_projectBadges]').contains('Project Badges');
    cy.get('[data-cy=filter_projectBadges] [data-cy=filterCount]')
      .contains('2');
    cy.get('[data-cy=filter_gems] [data-cy=filterCount]')
      .contains('2');
    cy.get('[data-cy=filter_gems]') .contains('Gems');
    cy.get('[data-cy=filter_globalBadges] [data-cy=filterCount]')
      .contains('1');
    cy.get('[data-cy=filter_globalBadges]').contains('Global Badges');

    cy.get('[data-cy=filter_projectBadges] [data-cy=filterCount]')
      .click();
    cy.get('[data-cy=selectedFilter]')
      .should('be.visible');
    cy.get('[data-cy="badgeTitle"]')
      .should('have.length', 2);
    cy.get('[data-cy="badgeTitle"]')
      .eq(0)
      .contains('Gem Badge');
    cy.get('[data-cy="badgeTitle"]')
      .eq(1)
      .contains('Gem Badge Two');
    cy.get('[data-pc-section="removeicon"]')
      .click();
    cy.get('[data-cy="badgeTitle"]')
      .should('have.length', 3);

    cy.get('[data-cy=filterBtn]')
      .click();
    cy.get('[data-cy=filter_gems] [data-cy=filterCount]')
      .click();
    cy.get('[data-cy=selectedFilter]')
      .should('be.visible');
    cy.get('[data-cy="badgeTitle"]')
      .should('have.length', 2);
    cy.get('[data-cy="badgeTitle"]')
      .eq(0)
      .contains('Gem Badge');
    cy.get('[data-cy="badgeTitle"]')
      .eq(1)
      .contains('Gem Badge Two');
    cy.get('[data-pc-section="removeicon"]')
      .click();
    cy.get('[data-cy="badgeTitle"]')
      .should('have.length', 3);

    cy.get('[data-cy=filterBtn]')
      .click();
    cy.get('[data-cy=filter_globalBadges] [data-cy=filterCount]')
      .click();
    cy.get('[data-cy=selectedFilter]')
      .should('be.visible');
    cy.get('[data-cy="badgeTitle"]')
      .should('have.length', 1);
    cy.get('[data-cy="badgeTitle"]')
      .eq(0)
      .contains('Global Badge One');
    cy.get('[data-pc-section="removeicon"]')
      .click();
    cy.get('[data-cy="badgeTitle"]')
      .should('have.length', 3);

    cy.get('[data-cy=badgeSearchInput]')
      .type('two');
    cy.get('[data-cy="badgeTitle"]')
      .should('have.length', 1);
    cy.get('[data-cy="badgeTitle"]')
      .eq(0)
      .contains('Gem Badge Two');
    cy.get('[data-cy=filterBtn]')
      .click();
    cy.get('[data-cy=filter_projectBadges] [data-cy=filterCount]')
      .contains('2');
    cy.get('[data-cy=filter_gems] [data-cy=filterCount]')
      .contains('2');
    cy.get('[data-cy=filter_globalBadges] [data-cy=filterCount]')
      .contains('1');
    cy.get('[data-cy=filter_gems] [data-cy=filterCount]')
      .click();
    cy.get('[data-cy="badgeTitle"]')
      .should('have.length', 1);
    cy.get('[data-cy="badgeTitle"]')
      .eq(0)
      .contains('Gem Badge Two');
    cy.get('[data-pc-section="removeicon"]')
      .click();
    cy.get('[data-cy="badgeTitle"]')
      .should('have.length', 1);
    cy.get('[data-cy="badgeTitle"]')
      .eq(0)
      .contains('Gem Badge Two');

    cy.get('[data-cy=clearSkillsSearchInput]')
      .click();
    cy.get('[data-cy="badgeTitle"]')
      .should('have.length', 3);

    cy.get('[data-cy=badgeSearchInput]')
      .type('fffffffffffffffffffff');
    cy.get('[data-cy=noContent]')
      .should('be.visible')
      .contains('No results');
  });

  it('badges card - gems and not global badges', function () {
    cy.visit('/progress-and-rankings/');

    cy.get('[data-cy=numAchievedGlobalBadges]')
      .should('not.exist');
    cy.get('[data-cy=numAchievedGemBadges]')
      .contains('Gems: 0 / 1');
  });

  it('badges card - global badges and not gems', function () {
    cy.intercept({
      method: 'GET',
      path: '/api/myProgressSummary',
    }, {
      statusCode: 200,
      body: {
        'projectSummaries': [{
          'projectId': 'Inception',
          'projectName': 'Inception',
          'points': 0,
          'totalPoints': 2695,
          'level': 0,
          'totalUsers': 1,
          'rank': 1
        }, {
          'projectId': 'proj1',
          'projectName': 'Project 1',
          'points': 0,
          'totalPoints': 1400,
          'level': 0,
          'totalUsers': 2,
          'rank': 2
        }],
        'totalProjects': 2,
        'numProjectsContributed': 0,
        'totalSkills': 56,
        'numAchievedSkills': 0,
        'numAchievedSkillsLastMonth': 0,
        'numAchievedSkillsLastWeek': 0,
        'mostRecentAchievedSkill': null,
        'totalBadges': 2,
        'gemCount': 0,
        'globalBadgeCount': 2,
        'numAchievedBadges': 0,
        'numAchievedGemBadges': 0,
        'numAchievedGlobalBadges': 1
      }
    })
      .as('getMyProgress');

    cy.visit('/progress-and-rankings/');
    cy.wait('@getMyProgress');

    cy.get('[data-cy=numAchievedGlobalBadges]')
      .contains('Global Badges: 1 / 2');
    cy.get('[data-cy=numAchievedGemBadges]')
      .should('not.exist');
  });

  it('badges card - global badges and gems', function () {
    cy.intercept({
      method: 'GET',
      path: '/api/myProgressSummary',
    }, {
      statusCode: 200,
      body: {
        'projectSummaries': [{
          'projectId': 'Inception',
          'projectName': 'Inception',
          'points': 0,
          'totalPoints': 2695,
          'level': 0,
          'totalUsers': 1,
          'rank': 1
        }, {
          'projectId': 'proj1',
          'projectName': 'Project 1',
          'points': 0,
          'totalPoints': 1400,
          'level': 0,
          'totalUsers': 2,
          'rank': 2
        }],
        'totalProjects': 2,
        'numProjectsContributed': 0,
        'totalSkills': 56,
        'numAchievedSkills': 0,
        'numAchievedSkillsLastMonth': 0,
        'numAchievedSkillsLastWeek': 0,
        'mostRecentAchievedSkill': null,
        'totalBadges': 2,
        'gemCount': 5,
        'globalBadgeCount': 2,
        'numAchievedBadges': 0,
        'numAchievedGemBadges': 2,
        'numAchievedGlobalBadges': 1
      }
    })
      .as('getMyProgress');

    cy.visit('/progress-and-rankings/');
    cy.wait('@getMyProgress');

    cy.get('[data-cy=numAchievedGlobalBadges]')
      .contains('Global Badges: 1 / 2');
    cy.get('[data-cy=numAchievedGemBadges]')
      .contains('Gems: 2 / 5');
  });

  it('badges card - no global badges and no gems', function () {
    cy.intercept({
      method: 'GET',
      path: '/api/myProgressSummary',
    }, {
      statusCode: 200,
      body: {
        'projectSummaries': [{
          'projectId': 'Inception',
          'projectName': 'Inception',
          'points': 0,
          'totalPoints': 2695,
          'level': 0,
          'totalUsers': 1,
          'rank': 1
        }, {
          'projectId': 'proj1',
          'projectName': 'Project 1',
          'points': 0,
          'totalPoints': 1400,
          'level': 0,
          'totalUsers': 2,
          'rank': 2
        }],
        'totalProjects': 2,
        'numProjectsContributed': 0,
        'totalSkills': 56,
        'numAchievedSkills': 0,
        'numAchievedSkillsLastMonth': 0,
        'numAchievedSkillsLastWeek': 0,
        'mostRecentAchievedSkill': null,
        'totalBadges': 2,
        'gemCount': 0,
        'globalBadgeCount': 0,
        'numAchievedBadges': 0,
        'numAchievedGemBadges': 0,
        'numAchievedGlobalBadges': 0
      }
    })
      .as('getMyProgress');

    cy.visit('/progress-and-rankings/');
    cy.wait('@getMyProgress');

    cy.get('[data-cy=numAchievedGlobalBadges]')
      .should('not.exist');
    cy.get('[data-cy=numAchievedGemBadges]')
      .should('not.exist');
  });


  it('custom badge icons are loaded on My Badges', function () {
    cy.intercept(' /api/projects/proj1/customIconCss').as('proj1CustomIcons')
    cy.intercept(' /api/projects/proj2/customIconCss').as('proj2CustomIcons')
    cy.uploadCustomIcon('valid_icon.png', '/admin/projects/proj1/icons/upload')

    cy.enableBadge(1, 1, { iconClass: 'proj1-validiconpng' })

    cy.createBadge(2, 1)
    cy.createSubject(2, 1)
    cy.createSkill(2, 1, 1)
    cy.assignSkillToBadge(2, 1, 1)
    cy.uploadCustomIcon('valid_icon.png', '/admin/projects/proj2/icons/upload')
    cy.enableBadge(2, 1, { iconClass: 'proj2-validiconpng' })

    cy.visit('/progress-and-rankings/');
    cy.get('[data-cy="project-link-proj1"]')
    cy.get('[data-cy="viewBadges"]').click()
    cy.wait('@proj1CustomIcons')
    cy.wait('@proj2CustomIcons')
    cy.wait(1000)
    cy.get('[data-cy="achievedBadge-badge1"] .proj1-validiconpng')
      .invoke('css', 'background-image')
      .then((bgImage) => {
        expect(bgImage).to.contain('data:image/png;base64')
      })

    cy.visit('/progress-and-rankings/my-badges');
    cy.wait('@proj1CustomIcons')
    cy.wait('@proj2CustomIcons')
    cy.wait(1000)
    cy.get('[data-cy="achievedBadge-badge1"] .proj1-validiconpng')
      .invoke('css', 'background-image')
      .then((bgImage) => {
        expect(bgImage).to.contain('data:image/png;base64')
      })
    cy.get('[data-cy="badge_badge1"] .proj2-validiconpng')
      .invoke('css', 'background-image')
      .then((bgImage) => {
        expect(bgImage).to.contain('data:image/png;base64')
      })
  });

  it('custom badge icons are loaded for multiple projects', function () {
    cy.intercept(' /api/projects/proj1/customIconCss').as('proj1CustomIcons')
    cy.intercept(' /api/projects/proj2/customIconCss').as('proj2CustomIcons')
    cy.uploadCustomIcon('valid_icon.png', '/admin/projects/proj1/icons/upload')

    cy.enableBadge(1, 1, { iconClass: 'proj1-validiconpng' })

    cy.createBadge(2, 1)
    cy.createSubject(2, 1)
    cy.createSkill(2, 1, 1)
    cy.assignSkillToBadge(2, 1, 1)
    cy.uploadCustomIcon('anothervalid_icon.png', '/admin/projects/proj2/icons/upload')
    cy.enableBadge(2, 1, { iconClass: 'proj2-anothervalidiconpng' })

    cy.visit('/progress-and-rankings/');
    cy.get('[data-cy="project-link-proj1"]').click()
    cy.wait('@proj1CustomIcons')
    cy.get('[data-cy="myBadgesBtn"]').click()
    cy.wait(1000)
    cy.get('[data-cy="achievedBadge-badge1"] .proj1-validiconpng')
      .invoke('css', 'background-image')
      .then((bgImage) => {
        expect(bgImage).to.contain('data:image/png;base64')
      })

    cy.get('[data-cy="breadcrumb-Progress And Rankings"]').click()
    cy.get('[data-cy="project-link-proj2"]').click()
    cy.wait('@proj2CustomIcons')
    cy.get('[data-cy="myBadgesBtn"]').click()
    cy.wait(1000)
    cy.get('[data-cy="badge_badge1"] .proj2-anothervalidiconpng')
      .invoke('css', 'background-image')
      .then((bgImage) => {
        expect(bgImage).to.contain('data:image/png;base64')
      })
  });

  it('custom badge icons are loaded for a single project', function () {
    cy.intercept(' /api/projects/proj2/customIconCss').as('proj2CustomIcons')
    cy.uploadCustomIcon('valid_icon.png', '/admin/projects/proj1/icons/upload')

    cy.enableBadge(1, 1, { iconClass: 'proj1-validiconpng' })

    cy.createBadge(2, 1)
    cy.createSubject(2, 1)
    cy.createSkill(2, 1, 1)
    cy.assignSkillToBadge(2, 1, 1)
    cy.uploadCustomIcon('anothervalid_icon.png', '/admin/projects/proj2/icons/upload')
    cy.enableBadge(2, 1, { iconClass: 'proj2-anothervalidiconpng' })

    cy.visit('/progress-and-rankings/projects/proj2')
    cy.wait('@proj2CustomIcons')
    cy.get('[data-cy="myBadgesBtn"]').click()
    cy.wait(1000)
    cy.get('[data-cy="badge_badge1"] .proj2-anothervalidiconpng')
      .invoke('css', 'background-image')
      .then((bgImage) => {
        expect(bgImage).to.contain('data:image/png;base64')
      })

    cy.visit('/progress-and-rankings/projects/proj2/badges')
    cy.wait('@proj2CustomIcons')
    cy.wait(1000)
    cy.get('[data-cy="badge_badge1"] .proj2-anothervalidiconpng')
      .invoke('css', 'background-image')
      .then((bgImage) => {
        expect(bgImage).to.contain('data:image/png;base64')
      })

    cy.visit('/progress-and-rankings/projects/proj2/badges/badge1')
    cy.wait('@proj2CustomIcons')
    cy.wait(1000)
    cy.get('[data-cy="badge_badge1"] .proj2-anothervalidiconpng')
      .invoke('css', 'background-image')
      .then((bgImage) => {
        expect(bgImage).to.contain('data:image/png;base64')
      })
  });

  it('custom badge icons are loaded for global badges', function () {
    cy.logout()
    cy.loginAsRootUser();

    cy.uploadCustomIcon('valid_icon.png', '/admin/badges/globalBadge1/icons/upload')
    cy.createGlobalBadge(1);
    cy.assignSkillToGlobalBadge(1, 1, 1);
    cy.assignProjectToGlobalBadge(1, 1);
    cy.enableGlobalBadge(1, { iconClass: 'GLOBAL-validiconpng' });

    cy.logout()
    cy.loginAsProxyUser();
    cy.intercept('/api/badges/globalBadge1/customIconCss').as('customIcons')

    cy.visit('/progress-and-rankings/');
    cy.get('[data-cy="project-link-proj1"]')
    cy.get('[data-cy="viewBadges"]').click()
    cy.wait('@customIcons')
    cy.wait(1000)
    cy.get('[data-cy="achievedBadge-globalBadge1"] .GLOBAL-validiconpng')
      .invoke('css', 'background-image')
      .then((bgImage) => {
        expect(bgImage).to.contain('data:image/png;base64')
      })

    cy.visit('/progress-and-rankings/my-badges');
    cy.wait('@customIcons')
    cy.wait(1000)
    cy.get('[data-cy="achievedBadge-globalBadge1"] .GLOBAL-validiconpng')
      .invoke('css', 'background-image')
      .then((bgImage) => {
        expect(bgImage).to.contain('data:image/png;base64')
      })

    cy.visit('/progress-and-rankings/projects/proj1/badges/global/globalBadge1')
    cy.wait('@customIcons')
    cy.wait(1000)
    cy.get('[data-cy="badge_globalBadge1"] .GLOBAL-validiconpng')
      .invoke('css', 'background-image')
      .then((bgImage) => {
        expect(bgImage).to.contain('data:image/png;base64')
      })

  });
})