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
import advancedFormatPlugin from 'dayjs/plugin/advancedFormat';

dayjs.extend(relativeTimePlugin);
dayjs.extend(advancedFormatPlugin);

describe('Client Display Tests', () => {

    // '[data-cy=timePassed]'

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

    it.skip('visit home page', () => {

        cy.request('POST', '/admin/projects/proj1/badges/badge1', {
            projectId: 'proj1',
            badgeId: 'badge1',
            name: 'Badge 1'
        });
        cy.intercept('GET', '/api/projects/proj1/pointHistory')
            .as('pointHistoryChart');
        cy.cdVisit('/');
        cy.injectAxe();
        cy.contains('Overall Points');

        // some basic default theme validation
        cy.get('#app')
            .should('have.css', 'background-color')
            .and('equal', 'rgba(0, 0, 0, 0)');
        cy.wait('@pointHistoryChart');
        cy.customA11y();
    });

    it.skip('ability to expand skill details from subject page', () => {
        cy.cdVisit('/');
        cy.injectAxe();
        cy.cdClickSubj(0);
        cy.get('input.v-switch-input')
            .focus();
        cy.matchSnapshotImageForElement('.skill-details-toggle', {
            blackout: '[data-cy=pointHistoryChart]'
        });
        cy.get('[data-cy=toggleSkillDetails]')
            .click();
        cy.contains('Lorem ipsum dolor sit amet');
        // 1 skill is locked
        cy.contains('Skill has 1 direct prerequisite(s).');
        cy.customA11y();
    });

    it.skip('badge details show skill details focus', () => {
        cy.resetDb();
        cy.fixture('vars.json')
            .then((vars) => {
                if (!Cypress.env('oauthMode')) {
                    cy.register(Cypress.env('proxyUser'), vars.defaultPass, false);
                }
            });
        cy.loginAsProxyUser();
        cy.createProject(1);
        cy.createProject(2);
        cy.createSubject(1, 1);
        cy.createSubject(2, 1);
        cy.createSkill(1, 1, 1, { name: 'Search blah skill 1' });
        cy.createSkill(1, 1, 2, { name: 'is a skill 2' });
        cy.createSkill(1, 1, 3, { name: 'find Blah other skill 3' });
        cy.createSkill(1, 1, 4, { name: 'Search nothing skill 4' });

        cy.createSkill(2, 1, 1, { name: 'blah1' });
        cy.createSkill(2, 1, 2, { name: 'blah2' });
        cy.createSkill(2, 1, 3, { name: 'blah3' });
        cy.createSkill(2, 1, 4, { name: 'blah4' });

        cy.loginAsRootUser();

        cy.createGlobalBadge(1);
        cy.assignSkillToGlobalBadge(1, 1, 1);
        cy.enableGlobalBadge();

        cy.loginAsProxyUser();

        cy.cdVisit('/');
        cy.cdClickBadges();
        cy.contains('Global Badge 1');
        cy.get('[data-cy=badgeDetailsLink_globalBadge1]')
            .click();
        cy.contains('Global Badge 1')
            .should('be.visible');
        cy.get('input.v-switch-input')
            .focus();
        cy.matchSnapshotImageForElement('.skill-details-toggle', {
            blackout: '[data-cy=pointHistoryChart]'
        });
    });

    it.skip('internal back button', () => {

        cy.intercept('GET', '/api/projects/proj1/pointHistory')
            .as('pointHistoryChart');

        cy.cdVisit('/?internalBackButton=true');
        cy.injectAxe();
        cy.contains('User Skills');
        cy.get('[data-cy=back]')
            .should('not.exist');

        // to ranking page and back
        cy.cdClickRank();
        cy.cdBack();

        // // to subject page and back
        cy.cdClickSubj(1, 'Subject 2');
        cy.cdBack();

        // to subject page (2nd subject card), then to skill page, back, back to home page
        cy.cdClickSubj(0, 'Subject 1');
        cy.cdClickSkill(0);
        cy.cdBack('Subject 1');
        cy.cdBack();

        // TODO: put back
        // cy.wait('@pointHistoryChart');
        cy.wait(500); //we have to wait for the chart to load before doing accessibility tests
        cy.customA11y();
    });

    it('clearly represent navigable components', () => {
        cy.cdVisit('?internalBackButton=true');
        cy.cdClickSubj(0);

        // make sure it can navigate into each skill via title
        cy.cdClickSkill(0, false);
        cy.cdBack('Subject 1');
        cy.cdClickSkill(1, false);
        cy.cdBack('Subject 1');
        cy.cdClickSkill(2, false);
        cy.cdBack('Subject 1');
        cy.cdClickSkill(3, false);
        cy.cdBack('Subject 1');

        // make sure it can navigate into each skill via progress bar
        cy.cdClickSkill(0);
        cy.cdBack('Subject 1');
        cy.cdClickSkill(1);
        cy.cdBack('Subject 1');
        cy.cdClickSkill(2);
        cy.cdBack('Subject 1');
        cy.cdClickSkill(3);
        cy.cdBack('Subject 1');
    });

    it.skip('components should not be clickable in the summary only option', () => {
        cy.request('POST', '/admin/projects/proj1/badges/badge1', {
            projectId: 'proj1',
            badgeId: 'badge1',
            name: 'Badge 1'
        });
        cy.cdVisit('/?isSummaryOnly=true');
        cy.injectAxe();

        // cy.get('[data-cy=myRank]').contains("1")
        cy.get('[data-cy=myBadges]')
            .contains('0 Badges');

        // make sure click doesn't take us anywhere
        cy.get('[data-cy=myRank]')
            .click();
        cy.contains('User Skills');

        cy.get('[data-cy=myBadges]')
            .click();
        cy.contains('User Skills');

        // make sure css is not attached
        cy.get('[data-cy=myRank]')
            .should('not.have.class', cssAttachedToNavigableCards);
        cy.get('[data-cy=myBadges]')
            .should('not.have.class', cssAttachedToNavigableCards);

        // summaries should not be displayed at all
        cy.get('[data-cy=subjectTile]')
            .should('not.exist');
        cy.customA11y();
    });

    it.skip('display achieved date on skill overview page', () => {
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
        cy.cdVisit('/?internalBackButton=true');
        cy.cdClickSubj(0);
        cy.cdClickSkill(1);

        cy.get('[data-cy=achievementDate]')
            .contains(`Achieved on ${orig.format('MMMM Do YYYY')}`);
        cy.get('[data-cy=achievementDate]')
            .contains(`${orig.fromNow()}`);
        cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="progressInfoCardTitle"]')
            .contains('500');

        cy.matchSnapshotImage({
            name: 'Skill-Overview-Achieved',
            blackout: '[data-cy=pointHistoryChart]'
        });

        cy.cdVisit('/?enableTheme=true');
        cy.cdClickSubj(0);
        cy.cdClickSkill(1);
        cy.get('[data-cy=breadcrumb-skill2]')
            .should('have.css', 'color')
            .and('equal', 'rgb(253, 253, 255)');

        cy.get('[data-cy=achievementDate]')
            .contains(`Achieved on ${orig.format('MMMM Do YYYY')}`);
        cy.get('[data-cy=achievementDate]')
            .contains(`${orig.fromNow()}`);
        cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="progressInfoCardTitle"]')
            .contains('500');

        cy.matchSnapshotImage({
            name: 'Skill-Overview-Achieved-Themed',
            blackout: '[data-cy=pointHistoryChart]'
        });

        cy.setResolution('iphone-6');

        cy.cdVisit('/');
        cy.cdClickSubj(0);
        cy.cdClickSkill(1);

        cy.get('[data-cy=achievementDate]')
            .contains(`Achieved on ${orig.format('MMMM Do YYYY')}`);
        cy.get('[data-cy=achievementDate]')
            .contains(`${orig.fromNow()}`);
        cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="progressInfoCardTitle"]')
            .contains('500');

        cy.matchSnapshotImage({
            name: 'Skill-Overview-Achieved-iphone6',
            blackout: '[data-cy=pointHistoryChart]'
        });

        cy.setResolution('ipad-2');

        cy.cdVisit('/');
        cy.cdClickSubj(0);
        cy.cdClickSkill(1);

        cy.get('[data-cy=achievementDate]')
            .contains(`Achieved on ${orig.format('MMMM Do YYYY')}`);
        cy.get('[data-cy=achievementDate]')
            .contains(`${orig.fromNow()}`);
        cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="progressInfoCardTitle"]')
            .contains('500');

        cy.matchSnapshotImage({
            name: 'Skill-Overview-Achieved-ipad2',
            blackout: '[data-cy=pointHistoryChart]'
        });

    });

    it.skip('display achieved date on subject page when skill details are expanded', () => {
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
        cy.cdVisit('/');
        cy.cdClickSubj(0);

        cy.get('[data-cy=toggleSkillDetails]')
            .click();
        cy.get('[data-cy=skillProgress_index-1] [data-cy=achievementDate]')
            .contains(`Achieved on ${orig.format('MMMM Do YYYY')}`);
        cy.get('[data-cy=skillProgress_index-1] [data-cy=achievementDate]')
            .contains(`${orig.fromNow()}`);
    });

    it.skip('skill with dependency renders dependency graph', () => {
        cy.viewport(1200, 1000);
        cy.cdVisit('/');
        cy.cdClickSubj(0);
        cy.get('[data-cy=toggleSkillDetails]')
            .click();
        cy.get('.locked-background')
            .click();

        cy.wait(4000);
        cy.get('[data-cy="prereqTable"] [data-cy="skillLink-proj1-skill2"]')
        cy.matchSnapshotImage({
            blackout: '[data-cy=pointHistoryChart]'
        });
    });

    it('skilltree brand should link to docs', () => {
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);

        // ensure brand exist
        cy.cdVisit('/');
        cy.get('[data-cy="skillTreePoweredBy"]')
            .contains('powered by');
        cy.get('[data-cy="skillTreePoweredBy"] a')
            .should('have.attr', 'href', 'https://skilltreeplatform.dev');
    });

    it.skip('view global badge with no skills assigned', () => {
        cy.resetDb();
        cy.fixture('vars.json')
            .then((vars) => {
                if (!Cypress.env('oauthMode')) {
                    cy.register(Cypress.env('proxyUser'), vars.defaultPass, false);
                }
            });
        cy.loginAsProxyUser();
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, { name: 'Search blah skill 1' });
        cy.createSkill(1, 1, 2, { name: 'is a skill 2' });
        cy.createSkill(1, 1, 3, { name: 'find Blah other skill 3' });
        cy.createSkill(1, 1, 4, { name: 'Search nothing skill 4' });

        cy.loginAsRootUser();

        cy.createGlobalBadge(1);
        cy.assignProjectToGlobalBadge(1, 1);
        cy.enableGlobalBadge();

        cy.loginAsProxyUser();

        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'now');

        cy.cdVisit('/');
        cy.cdClickBadges();

        // visit global badge
        cy.get('[data-cy=earnedBadgeLink_globalBadge1]')
            .click();

        cy.contains('Global Badge Details');
    });

    it.skip('view global badge with skills from two projects assigned', () => {
        cy.resetDb();
        cy.fixture('vars.json')
            .then((vars) => {
                if (!Cypress.env('oauthMode')) {
                    cy.register(Cypress.env('proxyUser'), vars.defaultPass, false);
                }
            });
        cy.loginAsProxyUser();
        cy.createProject(1);
        cy.createProject(2);
        cy.createSubject(1, 1);
        cy.createSubject(2, 1);
        cy.createSkill(1, 1, 1, { name: 'Search blah skill 1' });
        cy.createSkill(1, 1, 2, { name: 'is a skill 2' });
        cy.createSkill(1, 1, 3, { name: 'find Blah other skill 3' });
        cy.createSkill(1, 1, 4, { name: 'Search nothing skill 4' });

        cy.createSkill(2, 1, 1, { name: 'blah1' });
        cy.createSkill(2, 1, 2, { name: 'blah2' });
        cy.createSkill(2, 1, 3, { name: 'blah3' });
        cy.createSkill(2, 1, 4, { name: 'blah4' });

        cy.loginAsRootUser();

        cy.createGlobalBadge(1);
        cy.assignSkillToGlobalBadge(1, 1, 1);
        cy.assignSkillToGlobalBadge(1, 1, 2);
        cy.enableGlobalBadge();

        cy.loginAsProxyUser();

        cy.cdVisit('/');
        cy.cdClickBadges();
        cy.contains('Global Badge 1');
        cy.get('[data-cy=badgeDetailsLink_globalBadge1]')
            .click();
        cy.contains('Global Badge 1')
            .should('be.visible');
        cy.get('[data-cy=gb_proj2]')
            .contains('Search blah skill 1')
            .should('not.exist');
        cy.get('[data-cy=gb_proj1]')
            .contains('blah1')
            .should('not.exist');
        cy.get('[data-cy=gb_proj2]')
            .contains('blah1')
            .should('exist');
        cy.get('[data-cy=gb_proj1]')
            .contains('Search blah skill 1');
    });

    it.skip('completed badge count should not include global badges that do not have dependencies on this project', () => {
        cy.resetDb();
        cy.fixture('vars.json')
            .then((vars) => {
                if (!Cypress.env('oauthMode')) {
                    cy.register(Cypress.env('proxyUser'), vars.defaultPass, false);
                }
            });
        cy.loginAsProxyUser();
        cy.createProject(1);
        cy.createProject(2);
        cy.createSubject(1, 1);
        cy.createSubject(2, 1);
        cy.createSkill(1, 1, 1, { name: 'Search blah skill 1' });
        cy.createSkill(1, 1, 2, { name: 'is a skill 2' });
        cy.createSkill(1, 1, 3, { name: 'find Blah other skill 3' });
        cy.createSkill(1, 1, 4, { name: 'Search nothing skill 4' });

        cy.createSkill(2, 1, 1, { name: 'blah1' });
        cy.createSkill(2, 1, 2, { name: 'blah2' });
        cy.createSkill(2, 1, 3, { name: 'blah3' });
        cy.createSkill(2, 1, 4, { name: 'blah4' });

        cy.loginAsRootUser();

        cy.createGlobalBadge(1);
        cy.assignSkillToGlobalBadge(1, 1, 1);
        cy.assignSkillToGlobalBadge(1, 1, 2);
        cy.enableGlobalBadge(1);

        cy.reportSkill(2, 1, Cypress.env('proxyUser'));
        cy.reportSkill(2, 2, Cypress.env('proxyUser'));

        cy.createGlobalBadge(2);
        cy.assignSkillToGlobalBadge(2, 1, 2);
        cy.enableGlobalBadge(2);

        cy.loginAsProxyUser();

        cy.cdVisit('/');
        cy.get('[data-cy=myBadges]')
            .contains(' 0 ');
    });

    it.skip('global badge skills filter search no results', () => {
        cy.resetDb();
        cy.fixture('vars.json')
            .then((vars) => {
                if (!Cypress.env('oauthMode')) {
                    cy.register(Cypress.env('proxyUser'), vars.defaultPass, false);
                }
            });
        cy.loginAsProxyUser();
        cy.createProject(1);
        cy.createProject(2);
        cy.createSubject(1, 1);
        cy.createSubject(2, 1);
        cy.createSkill(1, 1, 1, { name: 'Search blah skill 1' });
        cy.createSkill(1, 1, 2, { name: 'is a skill 2' });
        cy.createSkill(1, 1, 3, { name: 'find Blah other skill 3' });
        cy.createSkill(1, 1, 4, { name: 'Search nothing skill 4' });

        cy.loginAsRootUser();

        cy.createGlobalBadge(1);
        cy.assignSkillToGlobalBadge(1, 1, 1);
        cy.enableGlobalBadge();

        cy.loginAsProxyUser();

        cy.cdVisit('/');
        cy.cdClickBadges();
        cy.contains('Global Badge 1');
        cy.get('[data-cy=badgeDetailsLink_globalBadge1]')
            .click();
        cy.contains('Global Badge 1')
            .should('be.visible');
        cy.get('[data-cy="skillsSearchInput"]')
            .type('ffff');
        cy.get('[data-cy=noDataYet]')
            .should('be.visible')
            .contains('No results');
    });

    it.skip('global badge with project levels should not display no skill assigned message', () => {
        cy.resetDb();
        cy.fixture('vars.json')
            .then((vars) => {
                if (!Cypress.env('oauthMode')) {
                    cy.register(Cypress.env('proxyUser'), vars.defaultPass, false);
                }
            });
        cy.loginAsProxyUser();
        cy.createProject(1);
        cy.createProject(2);
        cy.createSubject(1, 1);
        cy.createSubject(2, 1);
        cy.createSkill(1, 1, 1, { name: 'Search blah skill 1' });
        cy.createSkill(1, 1, 2, { name: 'is a skill 2' });
        cy.createSkill(1, 1, 3, { name: 'find Blah other skill 3' });
        cy.createSkill(1, 1, 4, { name: 'Search nothing skill 4' });

        cy.createSkill(2, 1, 1, { name: 'blah1' });
        cy.createSkill(2, 1, 2, { name: 'blah2' });
        cy.createSkill(2, 1, 3, { name: 'blah3' });
        cy.createSkill(2, 1, 4, { name: 'blah4' });

        cy.loginAsRootUser();
        cy.createGlobalBadge(1);
        cy.assignProjectToGlobalBadge(1, 1, 2);
        cy.assignProjectToGlobalBadge(1, 2, 2);
        cy.enableGlobalBadge();

        cy.cdVisit('/');
        cy.cdClickBadges();
        cy.contains('Global Badge 1');
        cy.get('[data-cy=badgeDetailsLink_globalBadge1]')
            .click();
        cy.contains('Global Badge 1')
            .should('be.visible');
        cy.get('[data-cy="skillsProgressList"][data-cy=noDataYet]')
            .should('not.exist');
    });

    it.skip('verify that authorization header is used in DevMode', () => {
        if (!Cypress.env('oauthMode')) {
            cy.intercept({ url: 'http://localhost:8083/admin/projects/proj1/token/user0', })
                .as('getToken');
        } else {
            cy.intercept('/api/projects/proj1/token')
                .as('getToken');
        }
        cy.intercept('GET', '/api/projects/proj1/skills/skill4/dependencies')
            .as('getDependencies');
        cy.cdVisit('/subjects/subj1/skills/skill4');
        cy.wait('@getToken')
          .its('response.body')
          .should('have.property', 'access_token');
        cy.get('@getToken')
          .its('response.body')
          .should('have.property', 'token_type', 'Bearer');
        cy.wait('@getDependencies')
            .its('request.headers')
            .should('have.property', 'authorization');
    });

    if (!Cypress.env('oauthMode')) {
        it.skip('verify that loginAsUser is used when retrieving token in DevMode', () => {
            cy.intercept({ url: 'http://localhost:8083/admin/projects/proj1/token/user7', })
                .as('getToken');
            cy.intercept('GET', '/api/projects/proj1/skills/skill4/dependencies')
                .as('getDependencies');
            cy.cdVisit('/subjects/subj1/skills/skill4?loginAsUser=user7');
            cy.wait('@getToken')
                .its('response.body')
                .should('have.property', 'access_token');
            cy.get('@getToken')
                .its('response.body')
                .should('have.property', 'token_type', 'Bearer')
            cy.wait('@getDependencies')
                .its('request.headers')
                .should('have.property', 'authorization');
        });
    }

    it.skip('project badge skills show subject name when details enabled', () => {
        cy.cdVisit('/');
        cy.cdClickBadges();

        cy.get('[data-cy=badgeDetailsLink_badge1]')
            .click();
        cy.contains('Badge Details');
        cy.get('[data-cy=toggleSkillDetails]')
            .click();
        cy.contains('Subject: Subject 1')
            .should('be.visible');
    });

    it.skip('badges details page does not show achieved badges in available badges section', () => {

        cy.request('POST', '/admin/projects/proj1/badges/badge2', {
            projectId: 'proj1',
            badgeId: 'badge2',
            name: 'Badge 2'
        });
        cy.assignSkillToBadge(1, 2, 5);
        cy.request('POST', '/admin/projects/proj1/badges/badge2', {
            projectId: 'proj1',
            badgeId: 'badge2',
            name: 'Badge 2',
            enabled: true,
        });
        cy.reportSkill(1, 5, Cypress.env('proxyUser')); // achieve badge 2

        cy.cdVisit('/');
        cy.cdClickBadges();
        cy.get('[data-cy=achievedBadges]')
            .contains('Badge 2');
        cy.get('[data-cy=availableBadges]')
            .contains('Badge 1');
    });

    it.skip('self report skills update badge progress in my badges display', () => {
        cy.createSkill(1, 1, 1, {
            selfReportingType: 'Approval',
            pointIncrement: 50,
            pointIncrementInterval: 0
        });
        cy.createSkill(1, 1, 2, {
            selfReportingType: 'HonorSystem',
            pointIncrement: 50,
            pointIncrementInterval: 0,
            numPerformToCompletion: 1
        });
        cy.createSkill(1, 1, 3);

        cy.request('POST', '/admin/projects/proj1/badges/badge2', {
            projectId: 'proj1',
            badgeId: 'badge2',
            name: 'Badge 2'
        });
        cy.assignSkillToBadge(1, 2, 1);
        cy.assignSkillToBadge(1, 2, 2);
        cy.assignSkillToBadge(1, 2, 3);
        cy.enableBadge(1, 2);

        cy.cdVisit('/');
        cy.cdClickBadges();
        cy.get('[data-cy=badgeDetailsLink_badge2]')
            .click();

        cy.get('.skills-badge')
            .contains('66% Complete');
        cy.get('[data-cy=toggleSkillDetails]')
            .click();
        cy.get('[data-cy=claimPointsBtn]')
            .click();
        cy.get('.skills-badge')
            .contains('100% Complete');
    });

    it('verify correct # of stars on subject card', () => {
        cy.request('PUT', '/admin/projects/proj1/subjects/subj1/levels/next', {
            percent: '99',
            'iconClass': 'fas fa-user-ninja',
        });
        cy.intercept('GET', '/api/projects/proj1/pointHistory')
            .as('pointHistoryChart');
        cy.cdVisit('/');
        cy.wait('@pointHistoryChart');
        cy.contains('Overall Points');
        cy.get('[data-cy=subjectTile]')
            .eq(0)
            .contains('Subject 1');
        cy.get('[data-cy=subjectTile]')
            .eq(0)
            .find('[data-cy="subjectStars"] [data-pc-section="officon"]')
            .should('have.length', 6);
        cy.get('[data-cy=subjectTile]')
          .eq(1)
          .find('[data-cy="subjectStars"] [data-pc-section="officon"]')
          .should('have.length', 5);
        cy.get('[data-cy=subjectTile]')
          .eq(2)
          .find('[data-cy="subjectStars"] [data-pc-section="officon"]')
          .should('have.length', 5);
    });

    it('description is rendered in client-display if configured on project', () => {
        cy.request('POST', '/admin/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1',
            description: 'I am a description *italic* **bold** foo',
        });
        cy.request('POST', '/admin/projects/proj1/settings', [{
            setting: 'show_project_description_everywhere',
            value: 'true',
            projectId: 'proj1'
        }]);

        cy.cdVisit('/');
        cy.get('[data-cy="projectDescription"]').contains('I am a description');
        cy.matchSnapshotImageForElement('[data-cy="projectDescription"]', {
            blackout: '[data-cy=pointHistoryChart]'
        });
    });

    it.skip('badges details page shows achievement info for skills', () => {

        cy.request('POST', '/admin/projects/proj1/badges/badge2', {
            projectId: 'proj1',
            badgeId: 'badge2',
            name: 'Badge 2'
        });
        cy.assignSkillToBadge(1, 2, 5);
        cy.request('POST', '/admin/projects/proj1/badges/badge2', {
            projectId: 'proj1',
            badgeId: 'badge2',
            name: 'Badge 2',
            enabled: true,
        });
        cy.reportSkill(1, 5, Cypress.env('proxyUser')); // achieve badge 2

        cy.cdVisit('/');
        cy.cdClickBadges();
        cy.get('[data-cy=achievedBadges]')
            .contains('Badge 2');
        cy.get('[data-cy=availableBadges]')
            .contains('Badge 1');

        cy.get('[data-cy=earnedBadgeLink_badge2]').contains('1st')
        cy.get('[data-cy=earnedBadgeLink_badge2]').click();
        cy.get('[data-cy=badge_badge2]').contains("You've achieved this badge - ")
        cy.get('[data-cy=badge_badge2]').contains('and you were the first!');
    });

    it.skip('badge achieved in third place', () => {

        cy.request('POST', '/admin/projects/proj1/badges/badge2', {
            projectId: 'proj1',
            badgeId: 'badge2',
            name: 'Badge 2'
        });
        cy.assignSkillToBadge(1, 2, 5);
        cy.request('POST', '/admin/projects/proj1/badges/badge2', {
            projectId: 'proj1',
            badgeId: 'badge2',
            name: 'Badge 2',
            enabled: true,
        });
        cy.reportSkill(1, 5, 'user5', '2019-09-14 11:00');
        cy.reportSkill(1, 5, 'user6', '2019-09-17 11:00');
        cy.reportSkill(1, 5, Cypress.env('proxyUser')); // achieve badge 2

        cy.reportSkill(1, 1, 'user5', '2019-09-14 11:00');
        cy.reportSkill(1, 1, 'user5', '2019-09-15 11:00');
        cy.reportSkill(1, 1, 'user5', '2019-09-16 11:00');
        cy.reportSkill(1, 1, 'user5', '2019-09-17 11:00');
        cy.reportSkill(1, 1, 'user5', '2019-09-18 11:00');

        cy.cdVisit('/');
        cy.cdClickBadges();
        cy.get('[data-cy=achievedBadges]')
            .contains('Badge 2');
        cy.get('[data-cy=availableBadges]')
            .contains('Badge 1');

        cy.get('[data-cy=earnedBadgeLink_badge2]').contains('3rd')
        cy.get('[data-cy=earnedBadgeLink_badge2]').click();
        cy.get('[data-cy=badge_badge2]').contains("2 other people have achieved this badge so far - and you were the third!")
    });

    it.skip('badge achieved second place', () => {

        cy.request('POST', '/admin/projects/proj1/badges/badge2', {
            projectId: 'proj1',
            badgeId: 'badge2',
            name: 'Badge 2'
        });
        cy.assignSkillToBadge(1, 2, 5);
        cy.request('POST', '/admin/projects/proj1/badges/badge2', {
            projectId: 'proj1',
            badgeId: 'badge2',
            name: 'Badge 2',
            enabled: true,
        });
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

        cy.cdVisit('/');
        cy.cdClickBadges();
        cy.get('[data-cy=achievedBadges]')
            .contains('Badge 2');
        cy.get('[data-cy=availableBadges]')
            .contains('Badge 1');

        cy.get('[data-cy=earnedBadgeLink_badge2]').contains('2nd')
        cy.get('[data-cy=earnedBadgeLink_badge2]').click();
        cy.get('[data-cy=badge_badge2]').contains("2 other people have achieved this badge so far - and you were the second!")
    });

    it.skip('badge with a bonus award in progress', () => {

        const anHourAgo = new Date().getTime() - (1000 * 60 * 60)
        const twoDays = (60 * 24 * 2)

        cy.request('POST', '/admin/projects/proj1/badges/badge2', {
            projectId: 'proj1',
            badgeId: 'badge2',
            name: 'Badge 2',
            awardAttrs: {'iconClass': 'fas fa-car', 'name': 'Test Badge Award', 'numMinutes': twoDays}
        });
        cy.assignSkillToBadge(1, 2, 5);
        cy.assignSkillToBadge(1, 2, 1);

        cy.request('POST', '/admin/projects/proj1/badges/badge2', {
            projectId: 'proj1',
            badgeId: 'badge2',
            name: 'Badge 2',
            enabled: true,
            awardAttrs: {'iconClass': 'fas fa-car', 'name': 'Test Badge Award', 'numMinutes': twoDays}
        });

        cy.reportSkill(1, 5, Cypress.env('proxyUser'), anHourAgo); // achieve badge 2

        cy.cdVisit('/');
        cy.cdClickBadges();
        cy.get('[data-cy=availableBadges]')
            .contains('Badge 2');
        cy.get('[data-cy=availableBadges]')
            .contains('Badge 1');

        cy.get('[data-cy=badgeDetailsLink_badge2]').click();
        cy.get('[data-cy=badge_badge2]').contains("Achieve this badge in 23 hours and 59 minutes for the Test Badge Award bonus!")
    });

    it.skip('badge with a bonus award completed', () => {

        const anHourAgo = new Date().getTime() - (1000 * 60 * 60)
        const twoDays = (60 * 24 * 2)

        cy.request('POST', '/admin/projects/proj1/badges/badge2', {
            projectId: 'proj1',
            badgeId: 'badge2',
            name: 'Badge 2',
            awardAttrs: {'iconClass': 'fas fa-car', 'name': 'Test Badge Award', 'numMinutes': twoDays}
        });
        cy.assignSkillToBadge(1, 2, 5);

        cy.request('POST', '/admin/projects/proj1/badges/badge2', {
            projectId: 'proj1',
            badgeId: 'badge2',
            name: 'Badge 2',
            enabled: true,
            awardAttrs: {'iconClass': 'fas fa-car', 'name': 'Test Badge Award', 'numMinutes': twoDays}
        });

        cy.reportSkill(1, 5, Cypress.env('proxyUser'), anHourAgo); // achieve badge 2

        cy.cdVisit('/');
        cy.cdClickBadges();
        cy.get('[data-cy=achievedBadges]')
            .contains('Badge 2');
        cy.get('[data-cy=availableBadges]')
            .contains('Badge 1');

        cy.get('[data-cy=earnedBadgeLink_badge2]').contains('Test Badge Award')
    });
});


