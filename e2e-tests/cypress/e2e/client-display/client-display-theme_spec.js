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
import moment from 'moment-timezone';

const dateFormatter = value => moment.utc(value)
    .format('YYYY-MM-DD[T]HH:mm:ss[Z]');

describe('Client Display Tests', () => {

    const snapshotOptions = {
        blackout: ['[data-cy=pointHistoryChart]', '[data-cy=achievementDate]'],
        failureThreshold: 0.03, // threshold for entire image
        failureThresholdType: 'percent', // percent of image or number of pixels
        customDiffConfig: { threshold: 0.01 }, // threshold for each pixel
        capture: 'fullPage', // When fullPage, the application under test is captured in its entirety from top to bottom.
    };
    const sizes = [
        'iphone-6',
        'ipad-2',
        'default',
        // [1200, 1080],
    ];

    const renderWait = 4000;

    before(() => {
        Cypress.Commands.add('cdInitProjWithSkills', () => {
            cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
                projectId: 'proj1',
                subjectId: 'subj1',
                name: 'Subject 1',
                helpUrl: 'http://doHelpOnThisSubject.com',
                iconClass: 'fas fa-jedi',
                description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.'
            });
            cy.request('POST', '/admin/projects/proj1/subjects/subj2', {
                projectId: 'proj1',
                subjectId: 'subj2',
                name: 'Subject 2',
                iconClass: 'fas fa-ghost',
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
            cy.request('POST', `/admin/projects/proj1/skills/skill4/dependency/skill2`);

            cy.request('POST', '/admin/projects/proj1/badges/badge1', {
                projectId: 'proj1',
                badgeId: 'badge1',
                name: 'Badge 1',
                'iconClass': 'fas fa-ghost',
                description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.',
            });

            cy.request('POST', '/admin/projects/proj1/badges/badge2', {
                projectId: 'proj1',
                badgeId: 'badge2',
                name: 'Badge 2',
                'iconClass': 'fas fa-monument',
                description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.',
            });

            cy.request('POST', '/admin/projects/proj1/badges/badge3', {
                projectId: 'proj1',
                badgeId: 'badge3',
                name: 'Badge 3',
                'iconClass': 'fas fa-jedi',
                description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.',
            });

            cy.request('POST', '/admin/projects/proj1/badge/badge1/skills/skill1');
            cy.request('POST', '/admin/projects/proj1/badge/badge1/skills/skill2');
            cy.request('POST', '/admin/projects/proj1/badge/badge1/skills/skill3');
            cy.enableBadge(1, 1);

            cy.request('POST', '/admin/projects/proj1/badge/badge2/skills/skill3');
            cy.enableBadge(1, 2, { 'iconClass': 'fas fa-monument' });

            cy.request('POST', '/admin/projects/proj1/badge/badge3/skills/skill2');
            cy.enableBadge(1, 3, { 'iconClass': 'fas fa-jedi' });

            const m = moment.utc('2020-09-12 11', 'YYYY-MM-DD HH');

            cy.request('POST', `/api/projects/proj1/skills/skill1`, {
                userId: Cypress.env('proxyUser'),
                timestamp: m.clone()
                    .add(1, 'day')
                    .format('x')
            });
            cy.request('POST', `/api/projects/proj1/skills/skill1`, {
                userId: Cypress.env('proxyUser'),
                timestamp: m.clone()
                    .add(2, 'day')
                    .format('x')
            });

            cy.request('POST', `/api/projects/proj1/skills/skill3`, {
                userId: Cypress.env('proxyUser'),
                timestamp: m.clone()
                    .add(1, 'day')
                    .format('x')
            });
            cy.request('POST', `/api/projects/proj1/skills/skill3`, {
                userId: Cypress.env('proxyUser'),
                timestamp: m.clone()
                    .add(2, 'day')
                    .format('x')
            });
        });

    });

    beforeEach(() => {
        Cypress.env('disabledUILoginProp', true);
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });
    });

    sizes.forEach((size) => {

        it(`test theming - project overview - ${size}`, () => {
            cy.intercept('GET', '/api/projects/proj1/pointHistory')
                .as('getPointHistory');
            cy.setResolution(size);

            cy.cdInitProjWithSkills();

            cy.cdVisit('/?enableTheme=true');
            // hex #626d7d = rgb(98, 109, 125)
            cy.get('#app')
                .should('have.css', 'background-color')
                .and('equal', 'rgb(98, 109, 125)');

            cy.wait('@getPointHistory');
            cy.get('[data-cy=pointHistoryChart]');

            cy.contains('Subject 3');
            cy.get('.user-skill-subject-tile:nth-child(1)')
                .contains('Subject 1');
            cy.get('[data-cy=myRank]')
                .contains('1');
            cy.get('[data-cy=myBadges]')
                .contains('1 Badge');
            cy.matchSnapshotImage(snapshotOptions);
        });

        it(`test theming - project rank - ${size}`, () => {
            cy.setResolution(size);

            cy.cdInitProjWithSkills();

            const m = moment.utc('2020-09-12 11', 'YYYY-MM-DD HH');
            for (let i = 0; i < 5; i += 1) {
                cy.request('POST', `/api/projects/proj1/skills/skill1`, {
                    userId: `uniqueUser${i}`,
                    timestamp: m.clone()
                        .add(1, 'day')
                        .format('x')
                });
                cy.request('POST', `/api/projects/proj1/skills/skill1`, {
                    userId: `uniqueUser${i}`,
                    timestamp: m.clone()
                        .add(2, 'day')
                        .format('x')
                });
            }

            cy.cdVisit('/?enableTheme=true&internalBackButton=true');

            // back button - border color
            cy.cdClickRank();
            // THEME: "pageTitleTextColor": "#fdfbfb",
            cy.get('[data-cy=back]')
                .should('have.css', 'border-color')
                .and('equal', 'rgb(253, 251, 251)');
            cy.get('[data-cy=back]')
                .should('have.css', 'color')
                .and('equal', 'rgb(253, 251, 251)');

            cy.contains('You are Level 2!');
            // wait for the bar (on the bar chart) to render
            cy.get('[data-cy="levelBreakdownChart-animationEnded"]');
            cy.matchSnapshotImage(snapshotOptions);
        });

        it(`test theming - badge - ${size}`, () => {
            cy.setResolution(size);

            cy.cdInitProjWithSkills();

            cy.cdVisit('/?enableTheme=true');

            cy.cdClickBadges();
            cy.contains('Badge 3');
            cy.matchSnapshotImage(snapshotOptions);

        });

        it(`test theming - badge details- ${size}`, () => {
            cy.setResolution(size);

            cy.cdInitProjWithSkills();

            cy.cdVisit('/?enableTheme=true');

            cy.cdClickBadges();
            cy.contains('Badge 3');

            cy.contains('View Details')
                .click();
            cy.contains('Badge 1');
            cy.contains('This is 3');
            cy.matchSnapshotImage(snapshotOptions);
        });

        it(`test theming - subject overview - ${size}`, () => {
            cy.setResolution(size);

            cy.cdInitProjWithSkills();

            cy.cdVisit('/?enableTheme=true');

            cy.cdClickSubj(0);
            cy.contains('Subject 1');
            cy.get('[data-cy=myRank]')
                .contains('1');
            cy.contains('This is 4');
            cy.contains('Earn up to 1,400 points');
            cy.matchSnapshotImage(snapshotOptions);
        });

        it(`test theming - subject overview with skill details - ${size}`, () => {
            cy.setResolution(size);

            cy.cdInitProjWithSkills();

            cy.cdVisit('/?enableTheme=true');

            cy.cdClickSubj(0);
            cy.contains('Subject 1');
            cy.get('[data-cy=myRank]')
                .contains('1');
            cy.contains('This is 4');
            cy.contains('Earn up to 1,400 points');

            cy.get('[data-cy=toggleSkillDetails]')
                .click();
            cy.get('[data-cy=myRank]')
                .contains('1');
            cy.contains('Lorem ipsum dolor sit amet');
            cy.contains('Skill has 1 direct dependent(s).');
            cy.contains('Earn up to 1,400 points');
            cy.contains('Description');

            cy.matchSnapshotImage(snapshotOptions);
        });

        it(`test theming - skill details - ${size}`, () => {
            cy.setResolution(size);

            cy.cdInitProjWithSkills();

            cy.cdVisit('/?enableTheme=true');

            cy.cdClickSubj(0);
            cy.contains('Subject 1');

            cy.cdClickSkill(0);
            cy.contains('Skill Overview');
            cy.contains('This is 1');
            cy.contains('Lorem ipsum dolor sit amet');
            cy.matchSnapshotImage(snapshotOptions);
        });

        it(`test theming - new version notification  - ${size}`, () => {
            cy.setResolution(size);
            cy.intercept(/\/api\/projects\/proj1\/rank$/,
                {
                    statusCode: 200,
                    body: {
                        'numUsers': 1,
                        'position': 1
                    },
                    headers: {
                        'skills-client-lib-version': dateFormatter(new Date())
                    },
                })
                .as('getRank');

            cy.cdVisit('/?enableTheme=true');
            cy.contains('User Skills');

            cy.cdClickRank();
            cy.wait('@getRank');
            cy.wait(renderWait);

            cy.matchSnapshotImage(snapshotOptions);
        });

    });

    // sizes came from https://docs.cypress.io/api/commands/viewport#Syntax
    const expandedVerticalSizes = [
        {
            name: 'iphone-6',
            width: 375,
            height: 667 * 3,
        },
        {
            name: 'ipad-2',
            width: 768,
            height: 1024 * 2,
        },
        {
            name: 'default',
            width: 1000,
            height: 660 * 2,
        },
    ];
    expandedVerticalSizes.forEach((size) => {
        it(`test theming - skill details with deps - ${size.name}`, () => {
            // must set viewport to show entire canvas or it will not appear in the screenshot
            cy.viewport(size.width, size.height);

            cy.cdInitProjWithSkills();

            cy.cdVisit('/?enableTheme=true');

            cy.cdClickSubj(0);
            cy.contains('Subject 1');

            cy.cdClickSkill(3);
            cy.contains('Skill Overview');
            cy.contains('This is 4');
            cy.contains('Lorem ipsum dolor sit amet');
            cy.contains('Achieved Dependencies');
            cy.wait(4000);
            cy.matchSnapshotImage(snapshotOptions);
        });
    });

    it(`test theming - No Subjects`, () => {
        cy.cdVisit('/?enableTheme=true');
        cy.contains('User Skills');
        cy.get('[data-cy=myRank]')
            .contains('1');
        cy.contains('0 Points earned Today');
        cy.contains('Subjects have not been added yet.');
        cy.matchSnapshotImage(snapshotOptions);
    });

    it('test theming - Empty Subject', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1',
            helpUrl: 'http://doHelpOnThisSubject.com',
            iconClass: 'fas fa-jedi',
            description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.'
        });

        cy.cdVisit('/?enableTheme=true');
        cy.contains('User Skills');

        cy.cdClickSubj(0);
        cy.contains('Subject 1');
        cy.contains('Skills have not been added yet.');
        cy.get('[data-cy=myRank]')
            .contains('1');
        cy.contains('0 Points earned Today');
        cy.contains('Description');
        cy.matchSnapshotImage(snapshotOptions);
    });

    it('Point History\'s open menu must respect tiles.background option', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1',
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
            version: 0,
        });
        const m = moment('2020-09-12 11', 'YYYY-MM-DD HH');
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {
            userId: Cypress.env('proxyUser'),
            timestamp: m.format('x')
        });
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {
            userId: Cypress.env('proxyUser'),
            timestamp: m.subtract(4, 'day')
                .format('x')
        });

        cy.cdVisit('/?enableTheme=true');
        cy.contains('Point History');
        cy.get('[data-cy="pointHistoryChart-animationEnded"]');
        cy.get('[title="Menu"]')
            .click();
        cy.contains('Download SVG');
        cy.matchSnapshotImageForElement('[data-cy=pointHistoryChart]');
    });

    it('skills search and skills filter selected', () => {
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, { name: 'Search blah skill 1' });
        cy.createSkill(1, 1, 2, { name: 'is a skill 2' });
        cy.createSkill(1, 1, 3, { name: 'find Blah other skill 3' });
        cy.createSkill(1, 1, 4, { name: 'Search nothing skill 4' });
        cy.createSkill(1, 1, 5, {
            name: 'sEEk bLaH skill 5',
            selfReportingType: 'Approval'
        });
        cy.createSkill(1, 1, 6, {
            name: 'some other skill 6',
            selfReportingType: 'HonorSystem'
        });

        cy.createBadge(1, 1);
        cy.assignSkillToBadge(1, 1, 1);
        cy.assignSkillToBadge(1, 1, 2);
        cy.assignSkillToBadge(1, 1, 3);
        cy.assignSkillToBadge(1, 1, 4);
        cy.assignSkillToBadge(1, 1, 5);
        cy.enableBadge(1, 1);

        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'now');

        cy.cdVisit('/?enableTheme=true');
        cy.cdClickSubj(0);

        cy.get('[data-cy="skillsSearchInput"]')
            .type('blah');

        cy.get('[data-cy="filterMenu"] [data-cy="filterBtn"]')
            .click();
        cy.get('[data-cy="filter_inProgress"]')
            .click();
        cy.get('[data-cy="selectedFilter"]')
            .contains('In Progress');

        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('skill 1');
        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('100 / 200');
        cy.get('[data-cy="skillProgress_index-1"]')
            .contains('skill 3');
        cy.get('[data-cy="skillProgress_index-1"]')
            .contains('100 / 200');
        cy.get('[data-cy="skillProgress_index-2"]')
            .should('not.exist');

        cy.matchSnapshotImageForElement('[data-cy="skillsProgressList"]');
    });

    it('skills filter open', () => {
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, { name: 'Search blah skill 1' });
        cy.createSkill(1, 1, 2, { name: 'is a skill 2' });
        cy.createSkill(1, 1, 3, { name: 'find Blah other skill 3' });
        cy.createSkill(1, 1, 4, { name: 'Search nothing skill 4' });
        cy.createSkill(1, 1, 5, {
            name: 'sEEk bLaH skill 5',
            selfReportingType: 'Approval'
        });
        cy.createSkill(1, 1, 6, {
            name: 'some other skill 6',
            selfReportingType: 'HonorSystem'
        });

        cy.createBadge(1, 1);
        cy.assignSkillToBadge(1, 1, 1);
        cy.assignSkillToBadge(1, 1, 2);
        cy.assignSkillToBadge(1, 1, 3);
        cy.assignSkillToBadge(1, 1, 4);
        cy.assignSkillToBadge(1, 1, 5);
        cy.enableBadge(1, 1);

        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'now');

        cy.cdVisit('/?enableTheme=true');
        cy.cdClickSubj(0);
        cy.get('[data-cy="filterMenu"] [data-cy="filterBtn"]')
            .click();

        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('skill 1');
        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('200 / 200');

        cy.get('[data-cy="skillProgress_index-1"]')
            .contains('skill 2');
        cy.get('[data-cy="skillProgress_index-1"]')
            .contains('100 / 200');

        cy.get('[data-cy="skillProgress_index-2"]')
            .contains('skill 3');
        cy.get('[data-cy="skillProgress_index-2"]')
            .contains('200 / 200');

        cy.get('[data-cy="skillProgress_index-3"]')
            .contains('skill 4');
        cy.get('[data-cy="skillProgress_index-3"]')
            .contains('100 / 200');

        cy.get('[data-cy="skillProgress_index-4"]')
            .contains('skill 5');
        cy.get('[data-cy="skillProgress_index-4"]')
            .contains('0 / 200');

        cy.get('[data-cy="skillProgress_index-5"]')
            .contains('skill 6');
        cy.get('[data-cy="skillProgress_index-5"]')
            .contains('0 / 200');

        cy.get('[data-cy="skillProgress_index-6"]')
            .should('not.exist');
        cy.matchSnapshotImage(snapshotOptions);
    });

    if (!Cypress.env('oauthMode')) {
        it('rank and leaderboard opt-out', () => {
            cy.request('POST', '/app/userInfo/settings', [{
                'settingGroup': 'user.prefs',
                'value': true,
                'setting': 'rank_and_leaderboard_optOut',
                'lastLoadedValue': '',
                'dirty': true
            }]);

            cy.createSubject(1, 1);
            cy.createSkill(1, 1, 1);
            cy.createSkill(1, 1, 2);
            cy.createSkill(1, 1, 3);

            const otherUser = 'user0';

            cy.reportSkill(1, 2, otherUser, 'now');
            cy.reportSkill(1, 3, otherUser, 'yesterday');
            cy.reportSkill(1, 1, otherUser, 'now');
            cy.reportSkill(1, 1, otherUser, 'yesterday');
            cy.reportSkill(1, 3, otherUser, 'now');

            cy.cdVisit(`/?enableTheme=true&loginAsUser=skills@skills.org`);
            cy.matchSnapshotImageForElement('[data-cy="myRank"]', 'Client Display Tests - My rank themed where user opted-out', snapshotOptions);

            cy.cdClickRank();

            const tableSelector = '[data-cy="leaderboardTable"]';
            const rowSelector = `${tableSelector} tbody tr`;
            cy.get(tableSelector)
                .contains('Loading...')
                .should('not.exist');
            cy.get(rowSelector)
                .should('have.length', 1)
                .as('cyRows');

            cy.get('[data-cy="myRankPositionStatCard"]')
                .contains('Opted-Out');
            cy.get('[data-cy="leaderboard"]')
                .contains('You selected to opt-out');

            cy.matchSnapshotImageForElement('[data-cy="myRankPositionStatCard"]', 'Client Display Tests - Rank Overview of My rank themed where user opted-out', snapshotOptions);
            cy.matchSnapshotImageForElement('[data-cy="leaderboard"]', 'Client Display Tests - Rank Overview of themed Leaderboard where user opted-out', snapshotOptions);

        });
    }

    it('ability to disable skilltree brand', () => {
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);

        // ensure brand exist
        cy.cdVisit('/?enableTheme=true');
        cy.get('[data-cy="skillTreePoweredBy"]')
            .contains('powered by');

        cy.cdVisit('/?enableTheme=true&themeParam=disableSkillTreeBrand|true');
        cy.get('[data-cy="skillTreePoweredBy"]')
            .should('not.exist');
    });

    it('ability to disable the breadcrumb', () => {
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);

        // ensure brand exist
        cy.cdVisit('/?enableTheme=true');
        cy.contains('Overall Points');
        cy.get('[data-cy=breadcrumb-Overview]')
            .should('exist');
        cy.get('[data-cy=breadcrumb-bar]')
            .contains('Overview')
            .should('be.visible');

        cy.cdVisit('/?enableTheme=true&themeParam=disableBreadcrumb|true');
        cy.get('[data-cy=breadcrumb-Overview]')
            .should('not.exist');
    });

    it('breadcrumb should use textPrimaryColor when buttons are disabled', () => {
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);

        // test without buttons & breadcrumb configured
        cy.cdVisit('/?enableTheme=true&themeParam=buttons|null');
        cy.cdClickSubj(0);
        cy.cdClickSkill(0);
        cy.get('[data-cy="breadcrumb-bar"]')
            .contains('Skill: skill1');
        cy.contains('powered by');
        cy.matchSnapshotImageForElement('[data-cy="breadcrumb-bar"] ol', 'Client Display Tests - breadcrumb default', snapshotOptions);

        // // make sure buttons color doesn't affect breadcrumb
        const buttonsParam = 'buttons|{"backgroundColor":"pink","foregroundColor":"purple","disabledColor":"purple"}';
        cy.cdVisit(`/?enableTheme=true&themeParam=${buttonsParam}`);
        cy.cdClickSubj(0);
        cy.cdClickSkill(0);
        cy.get('[data-cy="breadcrumb-bar"]')
            .contains('Skill: skill1');
        cy.contains('powered by');
        cy.matchSnapshotImageForElement('[data-cy="breadcrumb-bar"] ol', 'Client Display Tests - breadcrumb with buttons themed', snapshotOptions);

        // test breadcrumb theme
        const breadcrumbParam = 'breadcrumb|{"linkColor":"pink","linkHoverColor":"purple","currentPageColor":"green"}';
        cy.cdVisit(`/?enableTheme=true&themeParam=${breadcrumbParam}`);
        cy.cdClickSubj(0);
        cy.cdClickSkill(0);
        cy.get('[data-cy="breadcrumb-bar"]')
            .contains('Skill: skill1');
        cy.contains('powered by');
        cy.matchSnapshotImageForElement('[data-cy="breadcrumb-bar"] ol', 'Client Display Tests - breadcrumb with breadcrumb themed', snapshotOptions);
    });

    it('ability to control title color and size as well as border, padding and margin', () => {
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);

        const pageTitle = 'pageTitle|{"' +
            'textAlign":"left", ' +
            '"textColor":"%239cfd00", ' +
            '"margin": "-10px -15px 30px -15px", ' +
            '"padding": "10px 0px 10px 0px", ' +
            '"fontSize": "24px", ' +
            '"borderColor": "%23f2ff35", ' +
            '"borderStyle": "none none solid none", ' +
            '"backgroundColor": "%230374ff" }';
        cy.cdVisit(`/?enableTheme=true&themeParam=${pageTitle}`);
        cy.matchSnapshotImage('Client Display Tests - theme page title card', snapshotOptions);
    });

    it('ability to left align breadcrumb', () => {
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);

        const breadcrumbParam = 'breadcrumb|{"align":"start"}';
        cy.cdVisit(`/?enableTheme=true&themeParam=${breadcrumbParam}`);
        cy.cdClickSubj(0);
        cy.cdClickSkill(0);

        cy.matchSnapshotImageForElement('.skills-theme-page-title', 'Client Display Tests - left align breadcrumb', snapshotOptions);
    });

    it('change text color of "powered by" logo', () => {
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);

        const legacyPageTitleParam = 'themeParam=pageTitleTextColor|%2300ff80'; // green
        const pageTitleParam = 'themeParam=pageTitle|{"textColor":"%2300FFFF"}'; // blue
        const skillTreeBrandColorParam = 'themeParam=skillTreeBrandColor|%23ffff00'; // yellow

        // legacy pageTitleTextColor param
        cy.cdVisit(`/?enableTheme=true&${legacyPageTitleParam}`);
        cy.matchSnapshotImageForElement('[data-cy="skillsTitle"]', 'Client Display Tests - legacy title color param', snapshotOptions);

        // new pageTitle.textColor overrides legacy pageTitleTextColor param
        cy.cdVisit(`/?enableTheme=true&${legacyPageTitleParam}&${pageTitleParam}`);
        cy.matchSnapshotImageForElement('[data-cy="skillsTitle"]', 'Client Display Tests - title color param overrides legacy', snapshotOptions);

        // explicit brand color overrides title param
        cy.cdVisit(`/?enableTheme=true&${skillTreeBrandColorParam}&${legacyPageTitleParam}&${pageTitleParam}`);
        cy.matchSnapshotImageForElement('[data-cy="skillsTitle"]', 'Client Display Tests - explicit brand color', snapshotOptions);

        // just pageTitle.textColor param
        cy.cdVisit(`/?enableTheme=true&${pageTitleParam}`);
        // cy.matchSnapshotImageForElement('[data-cy="skillsTitle"]', 'Client Display Tests - title color param', snapshotOptions);
    });

    it('badge search and badge filter selected', () => {
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, { name: 'Search blah skill 1' });
        cy.createSkill(1, 1, 2, { name: 'is a skill 2' });
        cy.createSkill(1, 1, 3, { name: 'find Blah other skill 3' });
        cy.createSkill(1, 1, 4, { name: 'Search nothing skill 4' });
        cy.createSkill(1, 1, 5, {
            name: 'sEEk bLaH skill 5',
            selfReportingType: 'Approval'
        });
        cy.createSkill(1, 1, 6, {
            name: 'some other skill 6',
            selfReportingType: 'HonorSystem'
        });

        //create badge

        cy.createBadge(1, 1);
        cy.assignSkillToBadge(1, 1, 1);
        cy.assignSkillToBadge(1, 1, 3);
        cy.enableBadge(1, 1);

        cy.createBadge(1, 2);
        cy.assignSkillToBadge(1, 2, 2);
        cy.assignSkillToBadge(1, 2, 1);
        cy.enableBadge(1, 2);

        cy.createBadge(1, 3);
        cy.assignSkillToBadge(1, 3, 6);
        cy.enableBadge(1, 3);

        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'now');

        cy.cdVisit('/?enableTheme=true');
        cy.cdClickBadges();

        cy.get('[data-cy="badgeSearchInput"]')
            .type('badge');

        cy.get('[data-cy="filterMenu"] [data-cy="filterBtn"]')
            .click();
        cy.get('[data-cy="filter_projectBadges"]')
            .click();
        cy.get('[data-cy="selectedFilter"]')
            .contains('Project Badges');

        cy.get('[data-cy=earnedBadgeLink_badge1]')
            .should('be.visible');

        cy.get('.skills-badge')
            .should('have.length', 2);
        cy.get('.skills-badge')
            .eq(0)
            .contains('50% Complete');
        cy.get('.skills-badge')
            .eq(0)
            .contains('Badge 2');
        cy.get('.skills-badge')
            .eq(1)
            .contains('0% Complete');
        cy.get('.skills-badge')
            .eq(1)
            .contains('Badge 3');

        cy.matchSnapshotImageForElement('[data-cy="availableBadges"]', 'Client Display Tests - badge search and badge filter selected-available');
        cy.matchSnapshotImageForElement('[data-cy="achievedBadges"]', 'Client Display Tests - badge search and badge filter selected-achieved');
    });

    it('badge filter open', () => {
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, { name: 'Search blah skill 1' });
        cy.createSkill(1, 1, 2, { name: 'is a skill 2' });
        cy.createSkill(1, 1, 3, { name: 'find Blah other skill 3' });
        cy.createSkill(1, 1, 4, { name: 'Search nothing skill 4' });
        cy.createSkill(1, 1, 5, {
            name: 'sEEk bLaH skill 5',
            selfReportingType: 'Approval'
        });
        cy.createSkill(1, 1, 6, {
            name: 'some other skill 6',
            selfReportingType: 'HonorSystem'
        });

        //create badge

        cy.createBadge(1, 1);
        cy.assignSkillToBadge(1, 1, 1);
        cy.assignSkillToBadge(1, 1, 3);
        cy.enableBadge(1, 1);

        cy.createBadge(1, 2);
        cy.assignSkillToBadge(1, 2, 2);
        cy.assignSkillToBadge(1, 2, 1);
        cy.enableBadge(1, 2);

        cy.createBadge(1, 3);
        cy.assignSkillToBadge(1, 3, 6);
        cy.enableBadge(1, 3);

        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'now');

        cy.cdVisit('/?enableTheme=true');
        cy.cdClickBadges();

        cy.get('[data-cy="badgeSearchInput"]')
            .type('badge');

        cy.get('[data-cy="filterMenu"] [data-cy="filterBtn"]')
            .click();

        cy.get('[data-cy=earnedBadgeLink_badge1]')
            .should('be.visible');

        cy.get('.skills-badge')
            .should('have.length', 2);
        cy.get('.skills-badge')
            .eq(0)
            .contains('50% Complete');
        cy.get('.skills-badge')
            .eq(0)
            .contains('Badge 2');
        cy.get('.skills-badge')
            .eq(1)
            .contains('0% Complete');
        cy.get('.skills-badge')
            .eq(1)
            .contains('Badge 3');
        cy.matchSnapshotImage(snapshotOptions);
    });

    it('skills group', () => {
        cy.createSubject(1, 1);
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 2);
        cy.createSkillsGroup(1, 1, 1, { enabled: true });

        cy.cdVisit('/?enableTheme=true');
        cy.cdClickSubj(0);
        cy.get('[data-cy=skillCompletedCheck-group1')
            .should('not.exist'); // completed checkbox should not exist
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgress-ptsOverProgressBard"]')
            .first()
            .contains('0 / 2 Skills');
        cy.get('[data-cy="group-group1_skillProgress-skill1"] [data-cy="skillProgress-ptsOverProgressBard"]')
            .contains('0 / 200 Points');
        cy.get('[data-cy="group-group1_skillProgress-skill2"] [data-cy="skillProgress-ptsOverProgressBard"]')
            .contains('0 / 200 Points');
        cy.matchSnapshotImageForElement('[data-cy="skillsProgressList"]');
    });

    it('skills group - partial completion ', () => {
        cy.createSubject(1, 1);
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 2);
        cy.createSkillsGroup(1, 1, 1, { enabled: true });

        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'yesterday');

        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'yesterday');

        cy.cdVisit('/?enableTheme=true');
        cy.cdClickSubj(0);
        cy.get('[data-cy=skillCompletedCheck-group1')
            .should('not.exist'); // completed checkbox should not exist
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgress-ptsOverProgressBard"]')
            .first()
            .contains('1 / 2 Skills');
        cy.get('[data-cy="group-group1_skillProgress-skill1"] [data-cy="skillProgress-ptsOverProgressBard"]')
            .contains('200 / 200 Points');
        cy.get('[data-cy="group-group1_skillProgress-skill2"] [data-cy="skillProgress-ptsOverProgressBard"]')
            .contains('100 / 200 Points');
        cy.matchSnapshotImageForElement('[data-cy="skillsProgressList"]');
    });

    it('skills group - 1 out 2 skills required', () => {
        cy.createSubject(1, 1);
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 2);
        cy.createSkillsGroup(1, 1, 1, {
            numSkillsRequired: 1,
            enabled: true
        });

        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'yesterday');

        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'yesterday');

        cy.cdVisit('/?enableTheme=true');
        cy.cdClickSubj(0);
        cy.get('[data-cy=skillCompletedCheck-group1')
            .should('exist'); // completed checkbox should exist
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgress-ptsOverProgressBard"]')
            .first()
            .contains('1 / 1 Skill');
        cy.get('[data-cy="group-group1_skillProgress-skill1"] [data-cy="skillProgress-ptsOverProgressBard"]')
            .contains('200 / 200 Points');
        cy.get('[data-cy="group-group1_skillProgress-skill2"] [data-cy="skillProgress-ptsOverProgressBard"]')
            .contains('100 / 200 Points');
        cy.matchSnapshotImageForElement('[data-cy="skillsProgressList"]');
    });

//   http://localhost:8083/?themeParam=buttons|{%22backgroundColor%22:%22green%22,%22foregroundColor%22:%22white%22,%20%22borderColor%22:%22purple%22}&themeParam=textPrimaryColor|blue&themeParam=textSecondaryColor|purple&themeParam=tiles|{%22backgroundColor%22:%20%22yellow%22}&themeParam=badges|{%22backgroundColor%22:%22blue%22}
//     http://localhost:8083/?themeParam=charts|{%22lineColor%22:%22purple%22,%22labelBorderColor%22:%22green%22,%22labelBackgroundColor%22:%22neon%22,%22labelForegroundColor%22:%22gray%22,%22gradientStartColor%22:%22blue%22,%22gradientStopColor%22:%22yellow%22}
});
