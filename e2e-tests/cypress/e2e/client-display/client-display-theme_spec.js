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
            cy.addLearningPathItem(1, 2, 4)

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

    it(`test theming - No Subjects`, () => {
        cy.cdVisit('/?enableTheme=true');
        cy.contains('User Skills');
        cy.get('[data-cy=myRank]')
            .contains('1');
        cy.contains('0 Points earned Today');
        cy.contains('Subjects have not been added yet.');
        cy.matchSnapshotImageForElement('[data-cy="testDisplayTheme"]', { blackout: '[data-cy=pointHistoryChart]' });
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

        cy.cdVisit('/subjects/subj1/?enableTheme=true');
        cy.contains('Subject 1');
        cy.contains('Skills have not been added yet.');
        cy.get('[data-cy=myRank]')
            .contains('1');
        cy.contains('0 Points earned Today');
        cy.contains('Description');
        cy.matchSnapshotImageForElement('[data-cy="testDisplayTheme"]', { blackout: '[data-cy=pointHistoryChart]' });
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

        cy.cdVisit('/subjects/subj1/?enableTheme=true');

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

        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('100 / 200 Points')
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('100 / 200 Points')
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

        cy.cdVisit('/subjects/subj1/?enableTheme=true');
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
        cy.matchSnapshotImageForElement('[data-cy="skillsProgressList"]');
    });

    if (!Cypress.env('oauthMode')) {
        it.skip('rank and leaderboard opt-out', () => {
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
            cy.matchSnapshotImageForElement('[data-cy="myRank"]', {
                name: 'Client Display Tests - My rank themed where user opted-out',
                blackout: '[data-cy=pointHistoryChart]'
            });

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

            cy.matchSnapshotImageForElement('[data-cy="myRankPositionStatCard"]', {
                name: 'Client Display Tests - Rank Overview of My rank themed where user opted-out',
                blackout: '[data-cy=userFirstSeen]'
            });
            cy.matchSnapshotImageForElement('[data-cy="leaderboard"]', {
                name: 'Client Display Tests - Rank Overview of themed Leaderboard where user opted-out',
                blackout: '[data-cy=userFirstSeen]'
            });
        });
    }

    it('ability to disable skilltree brand', () => {
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);

        // ensure brand exist
        cy.cdVisit('/');
        cy.get('[data-cy="skillTreePoweredBy"]').contains('powered by');

        cy.cdVisit('/?themeParam=disableSkillTreeBrand|true');
        cy.get('[data-cy="skillTreePoweredBy"]').should('not.exist');
    });

    it('ability to control title color and size as well as border, padding and margin', () => {
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);

        const pageTitle = JSON.stringify({
            textAlign: 'left',
            textColor: encodeURIComponent('#9cfd00'),
            margin: '-10px -15px 30px -15px',
            padding: '10px 0px 10px 0px',
            fontSize: '24px',
            borderColor: encodeURIComponent('#f2ff35'),
            borderStyle: 'none none solid none',
            backgroundColor: encodeURIComponent('#0374ff')
        })
        cy.cdVisit(`/?enableTheme=true&themeParam=pageTitle|${pageTitle}|true&themeParam=disableBreadcrumb|true&themeParam=skillTreeBrandColor|null`);
        cy.matchSnapshotImageForElement('[data-cy="skillsTitle"]');
    });

    it.skip('ability to left align breadcrumb', () => {
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);

        const breadcrumbParam = 'breadcrumb|{"align":"start"}';
        cy.cdVisit(`/?enableTheme=true&themeParam=${breadcrumbParam}`);
        cy.cdClickSubj(0);
        cy.cdClickSkill(0);

        cy.matchSnapshotImageForElement('.skills-theme-page-title');
    });

    it('landingPageTitle theme config', () => {
        cy.cdVisit(`/?themeParam=landingPageTitle|New cool title`);
        cy.get('[data-cy="skillsTitle"]').contains('New cool title');

        cy.cdVisit(`/`);
        cy.get('[data-cy="skillsTitle"]').contains('User Skills');
    });

    it('change text color of "powered by" logo', () => {
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);

        const bgColor = encodeURIComponent('#152E4d')
        const titleBg = `themeParam=tiles|{"backgroundColor":"${bgColor}"}`;

        const legacyPageTitleParam = 'themeParam=pageTitleTextColor|%2300ff80'; // green
        const pageTitleParam = 'themeParam=pageTitle|{"textColor":"%2300FFFF"}'; // blue
        const skillTreeBrandColorParam = 'themeParam=skillTreeBrandColor|%23ffff00'; // yellow

        // legacy pageTitleTextColor param
        cy.cdVisit(`/?${legacyPageTitleParam}&${titleBg}`);
        cy.matchSnapshotImageForElement('[data-cy="skillTreePoweredBy"]', {
            name: 'SkillTree Brand - legacy title color param'
        });

        // new pageTitle.textColor overrides legacy pageTitleTextColor param
        cy.cdVisit(`/?${legacyPageTitleParam}&${pageTitleParam}&${titleBg}`);
        cy.matchSnapshotImageForElement('[data-cy="skillTreePoweredBy"]', {
            name: 'SkillTree Brand - title color param overrides legacy'
        });

        // explicit brand color overrides title param
        cy.cdVisit(`/?${skillTreeBrandColorParam}&${legacyPageTitleParam}&${pageTitleParam}&${titleBg}`);
        cy.matchSnapshotImageForElement('[data-cy="skillTreePoweredBy"]', {
            name: 'SkillTree Brand - explicit brand color'
        });

        // just pageTitle.textColor param
        cy.cdVisit(`/?${pageTitleParam}&${titleBg}`);
        cy.matchSnapshotImageForElement('[data-cy="skillTreePoweredBy"]', {
            name: 'SkillTree Brand - title color param'
        });
    });

    it.skip('badge search and badge filter selected', () => {
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

    it.skip('badge filter open', () => {
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
        cy.matchSnapshotImageForElement('[data-cy="skillsDisplayHome"]');
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

    it('skills group - partial completion', () => {
        cy.createSubject(1, 1);
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 2);
        cy.createSkillsGroup(1, 1, 1, { enabled: true });

        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'yesterday');

        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'yesterday');

        cy.cdVisit('/subjects/subj1/?enableTheme=true');
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

        cy.cdVisit('/subjects/subj1/?enableTheme=true');
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

});
