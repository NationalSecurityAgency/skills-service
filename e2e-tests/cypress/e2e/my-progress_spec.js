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

describe('Navigation Tests', () => {
    // const snapshotOptions = {
    //   blackout: ['[data-cy=pointHistoryChart]', '[data-cy=timePassed]'],
    //   failureThreshold: 0.03, // threshold for entire image
    //   failureThresholdType: 'percent', // percent of image or number of pixels
    //   customDiffConfig: { threshold: 0.01 }, // threshold for each pixel
    //   capture: 'fullPage', // When fullPage, the application under test is captured in its entirety from top to bottom.
    // };

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
        cy.request('POST', `/admin/projects/proj1/skills/skill4/dependency/skill2`);

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

    it('visit My Progress page', function () {

        const getIframeBody = () => {
            // get the iframe > document > body
            // and retry until the body element is not empty
            return cy
                .get('iframe')
                .its('0.contentDocument.body')
                .should('not.be.empty')
                // wraps "body" DOM element to allow
                // chaining more Cypress commands, like ".find(...)"
                // https://on.cypress.io/wrap
                .then(cy.wrap);
        };

        cy.loginAsRootUser();
        cy.request('PUT', `/supervisor/badges/globalBadge1`, {
            badgeId: `globalBadge1`,
            isEdit: false,
            name: `Global Badge 1`,
            originalBadgeId: '',
            iconClass: 'mi mi-ac-unit',
            enabled: true,
            description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.',
        });
        cy.assignSkillToGlobalBadge(1, 2);
        cy.enableGlobalBadge();

        cy.loginAsProxyUser();
        cy.visit('/progress-and-rankings/');

        cy.get('[data-cy="breadcrumb-Progress And Rankings"]')
            .contains('Progress And Rankings')
            .should('be.visible');

        cy.get('[data-cy=numProjectsContributed]')
            .contains(new RegExp(/^1$/));
        cy.get('[data-cy=numProjectsAvailable]')
            .contains(new RegExp(/^\/ 2$/));
        cy.get('[data-cy=info-snap-footer]')
            .contains('You still have 1 project to explore.');

        cy.get('[data-cy=numAchievedSkills]')
            .contains(new RegExp(/^2$/));
        cy.get('[data-cy=numSkillsAvailable]')
            .contains(new RegExp(/^Total: 10$/));
        cy.get('[data-cy=num-skills-footer]')
            .contains('So many skills... so little time! Good luck!');

        cy.get('[data-cy=mostRecentAchievedSkill]')
            .contains(`Last Achieved skill ${timeFromNowFormatter(testTime)}`);
        cy.get('[data-cy=numAchievedSkillsLastWeek]')
            .contains('2 skills in the last week');
        cy.get('[data-cy=numAchievedSkillsLastMonth]')
            .contains('2 skills in the last month');
        cy.get('[data-cy=last-earned-footer]')
            .contains('Keep up the good work!!');

        cy.get('[data-cy=badges-num-footer]')
            .contains('Be proud to earn those badges!!');
        cy.get('[data-cy=numAchievedBadges]')
            .contains(new RegExp(/^1$/));
        cy.get('[data-cy=numBadgesAvailable]')
            .contains(new RegExp(/^\/ 3$/));
        cy.get('[data-cy=numAchievedGlobalBadges]')
            .contains('Global Badges: 0');
        cy.get('[data-cy=numAchievedGemBadges]')
            .contains('Gems: 0');

        cy.get('[data-cy=project-link-proj2]')
            .should('be.visible');
        cy.get('[data-cy=project-link-proj2]')
            .find('[data-cy=project-card-project-name]')
            .contains('This is project 2');

        cy.get('[data-cy=project-link-proj1]')
            .should('be.visible');
        cy.get('[data-cy=project-link-proj1]')
            .find('[data-cy=project-card-project-name]')
            .contains('This is project 1');
        cy.get('[data-cy=project-link-proj1]')
            .find('[data-cy=project-card-project-level]')
            .contains('3');
        cy.get('[data-cy=project-link-proj1]')
            .find('[data-cy=project-card-project-rank]')
            .contains(new RegExp(/^Rank: 1 \/ 1$/));
        cy.get('[data-cy=project-link-proj1]')
            .find('[data-cy=project-card-project-points]')
            .contains(new RegExp(/^400 \/ 800$/));

        cy.get('[data-cy=inception-button]')
            .should('not.exist');

        cy.get('[data-cy=project-link-proj1]')
            .click();
        cy.dashboardCd()
            .contains('Overall Points');
        getIframeBody()
            .find('[data-cy=skillsTitle]')
            .contains('PROJECT: This is project 1')
            .should('be.visible');
        cy.get('[data-cy="breadcrumb-Progress And Rankings"]')
            .should('be.visible');
        cy.get('[data-cy=breadcrumb-proj1]')
            .should('be.visible');
        cy.get('[data-cy=breadcrumb-projects]')
            .should('not.exist');

        cy.get('[data-cy="breadcrumb-Progress And Rankings"]')
            .click();
        cy.get('[data-cy=numProjectsContributed]')
            .contains(new RegExp(/^1$/));
        cy.get('[data-cy=viewBadges]')
            .click();
        cy.get('[data-cy=earnedBadgeLink_badge1]')
            .should('be.visible');

        cy.get('.myBadges .earned-badge')
            .eq(0)
            .contains('Badge 1');
        cy.get('.row .skills-badge')
            .eq(0)
            .contains('Gem Badge');
        cy.get('.row .skills-badge')
            .eq(1)
            .contains('Global Badge 1');

        cy.intercept('/api/projects/proj1/rank')
            .as('loadRank');
        cy.get('[data-cy=badgeDetailsLink_globalBadge1]')
            .click();
        cy.get('[data-cy=breadcrumb-globalBadge1]')
            .should('be.visible');
        cy.get('[data-cy=breadcrumb-proj1]')
            .should('be.visible');
        cy.wait('@loadRank');
        getIframeBody()
            .find('.skills-title')
            .contains('Global Badge Details');
        getIframeBody()
            .find('.skills-text-description')
            .contains('Global Badge 1')
            .should('be.visible');
        getIframeBody()
            .find('h4')
            .contains('Project: This is project 1')
            .should('be.visible');
        getIframeBody()
            .find('[data-cy=skillProgressTitle]')
            .click();
        getIframeBody()
            .find('[data-cy=title]')
            .contains('Skill Overview');
        getIframeBody()
            .find('[data-cy=skillProgressTitle]')
            .contains('Very Great Skill 2');
        getIframeBody()
            .find('[data-cy=overallPointsEarnedCard]')
            .should('be.visible');
        cy.get('[data-cy="breadcrumb-Progress And Rankings"]')
            .click();

        cy.get('[data-cy=viewBadges]')
            .click();
        cy.get('[data-cy=earnedBadgeLink_badge1]')
            .click();
        cy.get('[data-cy=breadcrumb-badge1]')
            .should('be.visible');
        cy.get('[data-cy=breadcrumb-proj1]')
            .should('be.visible');
        cy.wait('@loadRank');
        getIframeBody()
            .find('.skills-title')
            .contains('Badge Details');
        getIframeBody()
            .find('.skills-text-description')
            .contains('Badge 1')
            .should('be.visible');
        getIframeBody()
            .find('[data-cy=skillProgressTitle]')
            .contains('Very Great Skill 1')
            .click();
        getIframeBody()
            .find('[data-cy=title]')
            .contains('Skill Overview');
        getIframeBody()
            .find('[data-cy=skillProgressTitle]')
            .contains('Very Great Skill 1');
        getIframeBody()
            .find('[data-cy=overallPointsEarnedCard]')
            .should('be.visible');

        cy.loginAsRootUser();
        cy.request('POST', `/api/projects/proj1/skills/skill2`, {
            userId: Cypress.env('proxyUser'),
            timestamp: yesterday
        });
        cy.request('POST', `/api/projects/proj1/skills/skill2`, {
            userId: Cypress.env('proxyUser'),
            timestamp: testTime
        });

        cy.request('POST', `/api/projects/proj1/skills/skill4`, {
            userId: Cypress.env('proxyUser'),
            timestamp: yesterday
        });
        cy.request('POST', `/api/projects/proj1/skills/skill4`, {
            userId: Cypress.env('proxyUser'),
            timestamp: testTime
        });

        cy.loginAsProxyUser();
        cy.visit('/progress-and-rankings/');
        cy.get('[data-cy=viewBadges]')
            .click();
        cy.get('[data-cy=badge-catalog_no-badges]')
            .should('be.visible');

        cy.visit('/progress-and-rankings/projects/proj1');
        cy.intercept('/api/myprojects/proj1/name')
            .as('getName');
        cy.dashboardCd()
            .contains('Overall Points');
        cy.wait('@getName');
        getIframeBody()
            .find('[data-cy=skillsTitle]')
            .contains('PROJECT: This is project 1')
            .should('be.visible');
    });

    it('project name should be visible on badges in badge catalog', () => {
        cy.visit('/progress-and-rankings/');
        cy.get('[data-cy=viewBadges]')
            .click();
        cy.get('[data-cy=badgeProjectName]')
            .eq(0)
            .should('be.visible')
            .should('have.text', 'Project: This is project 1');
        cy.get('[data-cy=badgeProjectName]')
            .eq(1)
            .should('be.visible')
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
        cy.request('PUT', `/supervisor/badges/globalBadge1`, globalBadge1);
        cy.assignSkillToGlobalBadge(1, 2);
        cy.enableGlobalBadge(1, globalBadge1);

        cy.request('PUT', `/supervisor/badges/globalBadge2`, {
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
        cy.get('.row .skills-badge')
            .should('have.length', 2);
        cy.get('.row .skills-badge')
            .eq(0)
            .contains('Gem Badge');
        cy.get('.row .skills-badge')
            .eq(1)
            .contains('Gem Badge Two');
        cy.get('[data-cy=clearSelectedFilter]')
            .click();
        cy.get('.row .skills-badge')
            .should('have.length', 3);

        cy.get('[data-cy=filterBtn]')
            .click();
        cy.get('[data-cy=filter_gems] [data-cy=filterCount]')
            .click();
        cy.get('[data-cy=selectedFilter]')
            .should('be.visible');
        cy.get('.row .skills-badge')
            .should('have.length', 2);
        cy.get('.row .skills-badge')
            .eq(0)
            .contains('Gem Badge');
        cy.get('.row .skills-badge')
            .eq(1)
            .contains('Gem Badge Two');
        cy.get('[data-cy=clearSelectedFilter]')
            .click();
        cy.get('.row .skills-badge')
            .should('have.length', 3);

        cy.get('[data-cy=filterBtn]')
            .click();
        cy.get('[data-cy=filter_globalBadges] [data-cy=filterCount]')
            .click();
        cy.get('[data-cy=selectedFilter]')
            .should('be.visible');
        cy.get('.row .skills-badge')
            .should('have.length', 1);
        cy.get('.row .skills-badge')
            .eq(0)
            .contains('Global Badge One');
        cy.get('[data-cy=clearSelectedFilter]')
            .click();
        cy.get('.row .skills-badge')
            .should('have.length', 3);

        cy.get('[data-cy=badgeSearchInput]')
            .type('two');
        cy.get('.row .skills-badge')
            .should('have.length', 3);
        cy.get('.row .skills-badge')
            .eq(1)
            .contains('Gem Badge Two');
        cy.get('[data-cy=filterBtn]')
            .click();
        cy.get('[data-cy=filter_projectBadges] [data-cy=filterCount]')
            .contains('1');
        cy.get('[data-cy=filter_gems] [data-cy=filterCount]')
            .contains('1');
        cy.get('[data-cy=filter_globalBadges] [data-cy=filterCount]')
            .contains('0');
        cy.get('[data-cy=filter_gems] [data-cy=filterCount]')
            .click();
        cy.get('.row .skills-badge')
            .should('have.length', 1);
        cy.get('.row .skills-badge')
            .eq(0)
            .contains('Gem Badge Two');
        cy.get('[data-cy=clearSelectedFilter]')
            .click();
        cy.get('.row .skills-badge')
            .should('have.length', 1);
        cy.get('.row .skills-badge')
            .eq(0)
            .contains('Gem Badge Two');

        cy.get('[data-cy=clearBadgesSearchInput]')
            .click();
        cy.get('.row .skills-badge')
            .should('have.length', 3);

        cy.get('[data-cy=badgeSearchInput]')
            .type('fffffffffffffffffffff');
        cy.get('[data-cy=noDataYet]')
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

    it('My Progress page - contributed to all projects', function () {
        // // add a skill to Inception to have contributed to all projects
        cy.loginAsRootUser();
        cy.request('POST', `/api/projects/proj2/skills/skill1`, {
            userId: Cypress.env('proxyUser'),
            timestamp: new Date().getTime()
        });

        cy.loginAsProxyUser();
        cy.visit('/progress-and-rankings/');

        cy.get('[data-cy=numProjectsContributed]')
            .contains(new RegExp(/^2$/));
        cy.get('[data-cy=numProjectsAvailable]')
            .contains(new RegExp(/^\/ 2$/));
        cy.get('[data-cy=info-snap-footer]')
            .contains('Great job, you have contributed to all projects!');
    });

    it('My Progress page - not contributed to more than one project', function () {
        cy.createProject(3);
        cy.enableProdMode(3);
        cy.addToMyProjects(3);

        cy.loginAsProxyUser();
        cy.visit('/progress-and-rankings/');

        cy.get('[data-cy=numProjectsContributed]')
            .contains(new RegExp(/^1$/));
        cy.get('[data-cy=numProjectsAvailable]')
            .contains(new RegExp(/^\/ 3$/));
        cy.get('[data-cy=info-snap-footer]')
            .contains('You still have 2 projects to explore.');
    });

    it('mySkills page - projects that do not have "production mode" enabled are not included', function () {
        cy.createProject(3);
        cy.addToMyProjects(3);

        cy.createSubject(3, 1);
        cy.createSubject(3, 2);

        cy.createSkill(3, 1, 1);
        cy.createSkill(3, 1, 2);
        cy.createSkill(3, 1, 3);
        cy.createSkill(3, 1, 4);

        cy.createBadge(3, 1);

        cy.loginAsProxyUser();
        cy.visit('/progress-and-rankings/');

        cy.get('[data-cy=numProjectsContributed]')
            .contains(new RegExp(/^1$/));
        cy.get('[data-cy=numProjectsAvailable]')
            .contains(new RegExp(/^\/ 2$/));
        cy.get('[data-cy=info-snap-footer]')
            .contains('You still have 1 project to explore.');

        cy.get('[data-cy=numSkillsAvailable]')
            .contains(new RegExp(/^Total: 10$/));
        cy.get('[data-cy=numBadgesAvailable]')
            .contains(new RegExp(/^\/ 2$/));

        cy.get('[data-cy=project-link-proj1]')
            .should('be.visible');
        cy.get('[data-cy=project-link-proj1]')
            .find('[data-cy=project-card-project-name]')
            .contains('This is project 1');

        cy.get('[data-cy=project-link-proj2]')
            .should('be.visible');
        cy.get('[data-cy=project-link-proj2]')
            .find('[data-cy=project-card-project-name]')
            .contains('This is project 2');

        cy.get('[data-cy=project-link-proj3]')
            .should('not.exist');
    });

    it('mySkills page - projects that are not added to My Projects are not included', function () {
        cy.createProject(3);
        cy.enableProdMode(3);

        cy.createSubject(3, 1);
        cy.createSubject(3, 2);

        cy.createSkill(3, 1, 1);
        cy.createSkill(3, 1, 2);
        cy.createSkill(3, 1, 3);
        cy.createSkill(3, 1, 4);

        cy.createBadge(3, 1);

        cy.loginAsProxyUser();
        cy.visit('/progress-and-rankings/');

        cy.get('[data-cy=numProjectsContributed]')
            .contains(new RegExp(/^1$/));
        cy.get('[data-cy=numProjectsAvailable]')
            .contains(new RegExp(/^\/ 2$/));
        cy.get('[data-cy=info-snap-footer]')
            .contains('You still have 1 project to explore.');

        cy.get('[data-cy=numSkillsAvailable]')
            .contains(new RegExp(/^Total: 10$/));
        cy.get('[data-cy=numBadgesAvailable]')
            .contains(new RegExp(/^\/ 2$/));

        cy.get('[data-cy=project-link-proj1]')
            .should('be.visible');
        cy.get('[data-cy=project-link-proj1]')
            .find('[data-cy=project-card-project-name]')
            .contains('This is project 1');

        cy.get('[data-cy=project-link-proj2]')
            .should('be.visible');
        cy.get('[data-cy=project-link-proj2]')
            .find('[data-cy=project-card-project-name]')
            .contains('This is project 2');

        cy.get('[data-cy=project-link-proj3]')
            .should('not.exist');
    });

    it('no projects added to My Projects', function () {
        cy.removeFromMyProjects(1);
        cy.removeFromMyProjects(2);
        cy.visit('/progress-and-rankings');

        cy.contains('START CUSTOMIZING TODAY!');

        cy.get('[data-cy="manageMyProjsBtnInNoContent"]')
            .click();
        cy.get('[data-cy="backToProgressAndRankingBtn"]');

        cy.visit('/progress-and-rankings');
        cy.get('[data-cy="manageMyProjsBtn"]')
            .click();
        cy.get('[data-cy="backToProgressAndRankingBtn"]');
    });

    it('My Progress page - no projects with production mode enabled', function () {
        // remove production mode from all projects
        cy.loginAsRootUser();
        cy.request('POST', '/admin/projects/proj1/settings/production.mode.enabled', {
            projectId: 'proj1',
            setting: 'production.mode.enabled',
            value: 'false'
        });
        cy.request('POST', '/admin/projects/proj2/settings/production.mode.enabled', {
            projectId: 'proj2',
            setting: 'production.mode.enabled',
            value: 'false'
        });

        cy.loginAsProxyUser();
        cy.visit('/progress-and-rankings/');

        cy.contains('START CUSTOMIZING TODAY!');
    });

    it('change sort order using keyboard', function () {
        cy.createProject(3);
        cy.enableProdMode(3);
        cy.addToMyProjects(3);

        cy.viewport(1200, 1000);

        cy.visit('/progress-and-rankings');
        cy.validateElementsOrder('[data-cy="project-card-project-name"]', ['This is project 3', 'This is project 2', 'This is project 1']);

        // move down
        cy.get('[data-cy="project-link-proj3"]')
            .tab()
            .type('{downArrow}');
        cy.validateElementsOrder('[data-cy="project-card-project-name"]', ['This is project 2', 'This is project 3', 'This is project 1']);
        cy.get('[data-cy="project-link-proj3"] [data-cy="sortControlHandle"]')
            .should('have.focus');

        // move down
        cy.get('[data-cy="project-link-proj3"]')
            .tab()
            .type('{downArrow}');
        cy.validateElementsOrder('[data-cy="project-card-project-name"]', ['This is project 2', 'This is project 1', 'This is project 3']);
        cy.get('[data-cy="project-link-proj3"] [data-cy="sortControlHandle"]')
            .should('have.focus');

        // move down - last item already; no action
        cy.get('[data-cy="project-link-proj3"]')
            .tab()
            .type('{downArrow}');
        cy.validateElementsOrder('[data-cy="project-card-project-name"]', ['This is project 2', 'This is project 1', 'This is project 3']);
        cy.get('[data-cy="project-link-proj3"] [data-cy="sortControlHandle"]')
            .should('have.focus');

        cy.visit('/progress-and-rankings');
        cy.validateElementsOrder('[data-cy="project-card-project-name"]', ['This is project 2', 'This is project 1', 'This is project 3']);
        cy.get('[data-cy="project-link-proj3"] [data-cy="sortControlHandle"]')
            .should('not.have.focus');

        // move up
        cy.get('[data-cy="project-link-proj1"]')
            .tab()
            .type('{upArrow}');
        cy.validateElementsOrder('[data-cy="project-card-project-name"]', ['This is project 1', 'This is project 2', 'This is project 3']);
        cy.get('[data-cy="project-link-proj1"] [data-cy="sortControlHandle"]')
            .should('have.focus');

        // move up; first item already - no action
        cy.get('[data-cy="project-link-proj1"]')
            .tab()
            .type('{upArrow}');
        cy.validateElementsOrder('[data-cy="project-card-project-name"]', ['This is project 1', 'This is project 2', 'This is project 3']);
        cy.get('[data-cy="project-link-proj1"] [data-cy="sortControlHandle"]')
            .should('have.focus');
    });

    it('sort my projects', function () {
        cy.createProject(3);
        cy.enableProdMode(3);
        cy.addToMyProjects(3);

        cy.viewport(1200, 1000);

        cy.visit('/progress-and-rankings');

        const proj1Selector = '[data-cy=project-link-proj1] [data-cy="sortControlHandle"]';
        const proj2Selector = '[data-cy=project-link-proj2] [data-cy="sortControlHandle"]';
        const proj3Selector = '[data-cy=project-link-proj3] [data-cy="sortControlHandle"]';

        cy.intercept('/api/myprojects/proj1')
            .as('updateMyProj1');
        cy.intercept('/api/myprojects/proj2')
            .as('updateMyProj2');
        cy.intercept('/api/myprojects/proj3')
            .as('updateMyProj3');

        cy.validateElementsOrder('[data-cy="project-card-project-name"]', ['This is project 3', 'This is project 2', 'This is project 1']);
        cy.get(proj2Selector)
            .dragAndDrop(proj1Selector);
        cy.wait('@updateMyProj2');
        cy.validateElementsOrder('[data-cy="project-card-project-name"]', ['This is project 3', 'This is project 1', 'This is project 2']);

        // refresh and make sure that sort order is still the same
        cy.visit('/progress-and-rankings');
        cy.validateElementsOrder('[data-cy="project-card-project-name"]', ['This is project 3', 'This is project 1', 'This is project 2']);

        cy.get(proj3Selector)
            .dragAndDrop(proj2Selector);
        cy.wait('@updateMyProj3');
        cy.validateElementsOrder('[data-cy="project-card-project-name"]', ['This is project 1', 'This is project 2', 'This is project 3']);

        cy.get(proj1Selector)
            .dragAndDrop(proj2Selector);
        cy.wait('@updateMyProj1');
        cy.validateElementsOrder('[data-cy="project-card-project-name"]', ['This is project 2', 'This is project 1', 'This is project 3']);

        // navigate to My Projects and then return
        cy.get('[data-cy="manageMyProjsBtn"]')
            .click();
        cy.get('[data-cy="backToProgressAndRankingBtn"]')
            .click();
        cy.validateElementsOrder('[data-cy="project-card-project-name"]', ['This is project 2', 'This is project 1', 'This is project 3']);
    });

    it('cards on small and medium screen', function () {
        cy.viewport('iphone-6');
        cy.visit('/progress-and-rankings');
        cy.wait(2000);
        cy.matchSnapshotImageForElement('[data-cy="project-link-proj1"]', 'project-card-small-screen');

        cy.viewport('ipad-2');
        cy.visit('/progress-and-rankings');
        cy.wait(2000);
        cy.matchSnapshotImageForElement('[data-cy="project-link-proj1"]', 'project-card-mid-screen');
    });

    it('My Progress page - verify custom level on ProjectLinkCard', function () {
        // set custom level display name for proj1
        cy.loginAsRootUser();
        cy.request('POST', '/admin/projects/proj1/settings', [{
            value: 'Stage',
            setting: 'level.displayName',
            projectId: 'proj1',
        }]);

        cy.loginAsProxyUser();
        cy.visit('/progress-and-rankings/');

        // proj1 has custom level name ("Stage")
        cy.get('[data-cy=project-link-proj1]')
            .find('[data-cy=project-card-project-level]')
            .contains('Stage');
        cy.get('[data-cy=project-link-proj1]')
            .find('[data-cy=project-card-project-level]')
            .contains('Level')
            .should('not.exist');

        // proj2 has default level name ("Level")
        cy.get('[data-cy=project-link-proj2]')
            .find('[data-cy=project-card-project-level]')
            .contains('Level');
    });

    it('My Progress page - verify custom project label', function () {
        // set custom level display name for proj1
        cy.loginAsRootUser();
        cy.request('POST', '/admin/projects/proj1/settings', [{
            value: 'Work Role',
            setting: 'project.displayName',
            projectId: 'proj1',
        }]);

        cy.loginAsProxyUser();
        cy.visit('/progress-and-rankings/');
        cy.get('[data-cy=project-link-proj1]')
            .click();

        // proj1 has custom level name ("Work Role")
        cy.dashboardCd()
            .find('[data-cy=title]')
            .contains('WORK ROLE: This is project 1');
    });

    it('Contact project owner', () => {
        cy.intercept('POST', '/api/projects/*/contact').as('contact');
        cy.intercept('POST', '/api/validation/description').as('validate');

        const invalidMsg = new Array(3000).fill('a').join('');
        cy.loginAsProxyUser();
        cy.visit('/progress-and-rankings/');
        cy.get('[data-cy=project-link-proj1]').click();
        cy.get('[data-cy="contactOwnerBtn"]').should('be.visible').click();
        cy.get('[data-cy="contactProjectOwnerDialog"]').should('exist');
        cy.get('[aria-label="Close"]').click();
        cy.get('[data-cy="contactProjectOwnerDialog"]').should('not.exist');
        cy.get('[data-cy="contactOwnerBtn"]').should('be.visible').click();
        cy.get('[data-cy="cancelBtn"]').click();
        cy.get('[data-cy="contactProjectOwnerDialog"]').should('not.exist');

        cy.get('[data-cy="contactOwnerBtn"]').should('be.visible').click();
        cy.contains('Contact This is project 1').should('be.visible');
        cy.get('[data-cy="contactOwnersSubmitBtn"]').should('contain.text', 'Submit');
        cy.get('[data-cy="contactOwnersSubmitBtn"]').should('be.disabled');
        cy.get('[data-cy="charactersRemaining"]').should('contain.text', '2,500 characters remaining');
        cy.get('[data-cy="contactOwnersMsgInput"]').click().fill(invalidMsg);
        cy.get('[data-cy="contactOwnersSubmitBtn"]').should('be.disabled');
        cy.get('[data-cy="charactersRemaining"]').should('contain.text', '-500 characters remaining');

        cy.get('[data-cy="contactOwnersMsgInput"]').click().fill('message message jabberwocky jabberwocky message message');
        cy.wait('@validate');
        cy.get('[data-cy="contactOwnersSubmitBtn"]').should('be.disabled');
        cy.get('[data-cy="contactOwnersInput_errMsg"]').should('be.visible');
        cy.get('[data-cy="contactOwnersInput_errMsg"]').should('contain.text', 'paragraphs may not contain jabberwocky');
        cy.get('[data-cy="contactOwnersMsgInput"]').click().fill('aaa bbb this is a message');
        cy.get('[data-cy="charactersRemaining"]').should('contain.text', '2,475 characters remaining');
        cy.get('[data-cy="contactOwnersSubmitBtn"]').should('be.enabled');
        cy.get('[data-cy="contactOwnersSubmitBtn"]').click();
        cy.wait('@contact');
        cy.get('[data-cy="cancelBtn"]').should('not.exist');
        cy.get('[data-cy="contactOwnersSubmitBtn"]').should('contain.text', 'Ok');
        cy.get('[data-cy="contactOwnerSuccessMsg"]').should('contain.text', 'Message sent!');
        cy.get('[data-cy="contactOwnerSuccessMsg"]').should('contain.text', 'The Project Administrator(s) of This is project 1 will be notified of your question via email.');
        cy.get('[data-cy="contactOwnersSubmitBtn"]').click();
        cy.wait(500); //wait for animations to complete
        cy.get('[data-cy="contactProjectOwnerDialog"]').should('not.exist');
    });

    it('Send email to project owner', () => {
            cy.intercept('POST', '/api/projects/*/contact').as('contact');
            cy.intercept('POST', '/api/validation/description').as('validate');
            cy.loginAsAdminUser();
            cy.createProject(4);
            cy.enableProdMode(4);
            cy.visit('/progress-and-rankings/');
            cy.get('[data-cy=manageMyProjsBtn]').click();
            cy.get('[data-cy="contactOwnerBtn_proj4"]').should('be.visible').click();
            cy.get('[data-cy="contactProjectOwnerDialog"]').should('exist');
            cy.get('[data-cy="contactOwnersMsgInput"]').click().fill('aaa bbb this is a message');
            cy.get('[data-cy="charactersRemaining"]').should('contain.text', '2,475 characters remaining');
            cy.get('[data-cy="contactOwnersSubmitBtn"]').should('be.enabled');
            cy.get('[data-cy="contactOwnersSubmitBtn"]').click();
            cy.wait('@contact');
            cy.get('[data-cy="contactOwnersSubmitBtn"]').should('contain.text', 'Ok');
            cy.get('[data-cy="contactOwnerSuccessMsg"]').should('contain.text', 'Message sent!');
            cy.get('[data-cy="contactOwnerSuccessMsg"]').should('contain.text', 'The Project Administrator(s) of This is project 4 will be notified of your question via email.');
            cy.get('[data-cy="contactOwnersSubmitBtn"]').click();
            cy.wait(500); //wait for animations to complete
            cy.get('[data-cy="contactProjectOwnerDialog"]').should('not.exist');
            cy.getEmails().then((emails) => {
                    expect(emails[0].textAsHtml).to.contain('aaa bbb this is a message');
            });
    });
});