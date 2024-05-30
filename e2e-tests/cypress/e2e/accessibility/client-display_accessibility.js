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
import moment from 'moment';

const dateFormatter = value => moment.utc(value)
    .format('YYYY-MM-DD[T]HH:mm:ss[Z]');

describe('Client Display Accessibility tests', () => {
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
            helpUrl: 'http://doHelpOnThisSkill.com',
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
            helpUrl: 'http://doHelpOnThisSkill.com',
            selfReportingType: 'Approval'
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

        cy.request('POST', '/admin/projects/proj1/badges/badge1', {
            projectId: 'proj1',
            badgeId: 'badge1',
            name: 'Badge 1'
        });
    });

    it('Initial View', () => {
        cy.intercept('/api/projects/proj1/subjects/subj1/summary*', (req) => {
            req.reply((res) => {
                res.send(200, {
                    'subject': 'Subject 1',
                    'subjectId': 'subj1',
                    'description': 'Description',
                    'skillsLevel': 0,
                    'totalLevels': 5,
                    'points': 0,
                    'totalPoints': 0,
                    'todaysPoints': 0,
                    'levelPoints': 0,
                    'levelTotalPoints': 0,
                    'skills': [],
                    'iconClass': 'fa fa-question-circle',
                    'helpUrl': 'http://doHelpOnThisSubject.com'
                }, { 'skills-client-lib-version': dateFormatter(new Date()) });
            });
        })
            .as('getSubjectSummary');
        cy.intercept('GET', '/api/projects/proj1/pointHistory')
            .as('pointHistoryChart');

        cy.cdVisit('/', true);
        cy.injectAxe();
        cy.contains('Overall Points');
        cy.cdClickSubj(0, 'Subject 1', true);

        cy.wait('@getSubjectSummary');
        cy.wait('@pointHistoryChart');

        cy.contains('New SkillTree Software Version is Available');

        cy.wait(500); //need to wait on the pointHistoryChart to complete rendering before running a11y
        cy.customA11y();
        cy.customLighthouse();

        cy.cdVisit('/', true);
        cy.contains('Overall Points');
        cy.contains('New SkillTree Software Version is Available')
            .should('not.exist');
    });

    it.only('skill with self reporting', () => {
        cy.cdVisit('/', true);
        cy.injectAxe();
        cy.contains('Overall Points');

        cy.cdClickSubj(0, 'Subject 1', true);
        cy.cdClickSkill(1);
        cy.contains('This is 2');
        cy.customA11y();
        cy.customLighthouse();

        cy.get('[data-cy="requestApprovalBtn"]')
            .should('exist');
        cy.get('[data-cy="requestApprovalAlert"]')
            .contains('This skill requires approval. Request 100 points once you\'ve completed the skill.')
        cy.wait(500); // sometimes modal takes a bit to render
        cy.customA11y();
        cy.customLighthouse();

        // cy.get('[data-cy="selfReportSubmitBtn"]').click();
        // cy.get('[data-cy="selfReportAlert"]').contains("This skill requires approval from a project administrator. Now let's play the waiting game! ")
        // cy.customA11y();
        // cy.customLighthouse();
    });

    it('leaderboard', () => {
        cy.reportSkill(1, 1, `user0@skills.org`, '2021-02-24 10:00');
        cy.reportSkill(1, 1, `user1@skills.org`, '2021-02-24 10:00');

        cy.cdVisit('/');
        cy.injectAxe();
        cy.contains('Overall Points');

        cy.cdClickRank();

        const tableSelector = '[data-cy="leaderboardTable"]';
        const rowSelector = `${tableSelector} tbody tr`;
        cy.get(tableSelector)
            .contains('Loading...')
            .should('not.exist');
        cy.get(rowSelector)
            .should('have.length', 3)
            .as('cyRows');

        cy.customA11y();
        cy.customLighthouse();
    });

    it('skills search and filter', () => {
        cy.cdVisit('/');
        cy.injectAxe();
        cy.cdClickSubj(0, 'Subject 1');

        // hit on all records
        cy.get('[data-cy="skillsSearchInput"]')
            .type('is');

        // select a filter with results
        cy.get('[data-cy="filterMenu"] [data-cy="filterBtn"]')
            .click();
        cy.get('[data-cy="filter_withoutProgress"]')
            .click();
        cy.get('[data-cy="selectedFilter"]')
            .contains('Without Progress');

        // open filter
        cy.get('[data-cy="filterMenu"] [data-cy="filterBtn"]')
            .click();
        cy.get('[data-cy="filter_withoutProgress"] [data-cy="filterCount"]')
            .contains(2);
        cy.get('[data-cy="filter_complete"] [data-cy="filterCount"]')
            .contains(1);
        cy.get('[data-cy="filter_inProgress"] [data-cy="filterCount"]')
            .contains(1);

        cy.customA11y();
        cy.customLighthouse();
    });

    it('Summary view', () => {
        cy.request('POST', '/admin/projects/proj1/badges/badge1', {
            projectId: 'proj1',
            badgeId: 'badge1',
            name: 'Badge 1'
        });
        cy.assignSkillToBadge(1, 1, 1);
        cy.enableBadge(1, 1);
        cy.cdVisit('/?isSummaryOnly=true');
        cy.injectAxe();

        cy.get('[data-cy=myBadges]')
            .contains('0 Badges');
        cy.wait(4000); //need to wait on the pointHistoryChart to complete rendering before running a11y

        cy.customA11y();
        cy.customLighthouse();
    });

    it('Skills groups', () => {
        cy.createSkillsGroup(1, 2, 1);
        cy.addSkillToGroup(1, 2, 1, 11, {
            pointIncrement: 50,
            numPerformToCompletion: 2
        });
        cy.addSkillToGroup(1, 2, 1, 22, {
            pointIncrement: 50,
            numPerformToCompletion: 2
        });
        cy.createSkillsGroup(1, 2, 1, { enabled: true });

        cy.cdVisit('/');
        cy.injectAxe();
        cy.cdClickSubj(1);

        cy.get('[data-cy="group-group1Subj2_skillProgress-skill22Subj2"]');
        cy.get('[data-cy="group-group1Subj2_skillProgress-skill11Subj2"] [data-cy="skillProgress-ptsOverProgressBard"]')
            .contains('0 / 100 Points');
        cy.get('[data-cy="group-group1Subj2_skillProgress-skill22Subj2"] [data-cy="skillProgress-ptsOverProgressBard"]')
            .contains('0 / 100 Points');

        cy.wait(4000); //need to wait on the pointHistoryChart to complete rendering before running a11y
        cy.customA11y();
        cy.customLighthouse();

        cy.createSkillsGroup(1, 2, 1, {
            numSkillsRequired: 1,
            enabled: true
        });

        cy.doReportSkill({
            project: 1,
            skill: 11,
            subjNum: 2,
            userId: Cypress.env('proxyUser'),
            date: 'now',
            failOnError: true,
            approvalRequestedMsg: null
        });
        cy.doReportSkill({
            project: 1,
            skill: 11,
            subjNum: 2,
            userId: Cypress.env('proxyUser'),
            date: 'yesterday',
            failOnError: true,
            approvalRequestedMsg: null
        });
        cy.doReportSkill({
            project: 1,
            skill: 22,
            subjNum: 2,
            userId: Cypress.env('proxyUser'),
            date: 'yesterday',
            failOnError: true,
            approvalRequestedMsg: null
        });

        cy.cdVisit('/');
        cy.injectAxe();
        cy.cdClickSubj(1);

        cy.get('[data-cy="group-group1Subj2_skillProgress-skill22Subj2"]');
        cy.get('[data-cy="group-group1Subj2_skillProgress-skill11Subj2"] [data-cy="skillProgress-ptsOverProgressBard"]')
            .contains('100 / 100 Points');
        cy.get('[data-cy="group-group1Subj2_skillProgress-skill22Subj2"] [data-cy="skillProgress-ptsOverProgressBard"]')
            .contains('50 / 100 Points');

        cy.wait(4000); //need to wait on the pointHistoryChart to complete rendering before running a11y
        cy.customA11y();
        cy.customLighthouse();
    });

});
