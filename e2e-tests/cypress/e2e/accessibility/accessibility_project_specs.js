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

const moment = require('moment-timezone');

describe('Accessibility Tests', () => {

    beforeEach(() => {
        cy.request('POST', '/app/projects/MyNewtestProject', {
            projectId: 'MyNewtestProject',
            name: 'My New test Project'
        });

        cy.request('POST', '/app/projects/MyNewtestProject2', {
            projectId: 'MyNewtestProject2',
            name: 'My New test Project2'
        });

        cy.request('POST', '/admin/projects/MyNewtestProject/subjects/subj1', {
            projectId: 'MyNewtestProject',
            subjectId: 'subj1',
            name: 'Subject 1'
        });

        cy.request('POST', '/admin/projects/MyNewtestProject/badges/badge1', {
            projectId: 'MyNewtestProject',
            badgeId: 'badge1',
            name: 'Badge 1'
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
            description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.',
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
            description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.',
            version: 0,
            helpUrl: 'http://doHelpOnThisSkill.com'
        });

        cy.request('POST', `/admin/projects/MyNewtestProject/subjects/subj1/skills/skill3`, {
            projectId: 'MyNewtestProject',
            subjectId: 'subj1',
            skillId: 'skill3',
            name: `This is 3`,
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
        cy.request('POST', `/admin/projects/MyNewtestProject/subjects/subj1/skills/skill4`, {
            projectId: 'MyNewtestProject',
            subjectId: 'subj1',
            skillId: 'skill4',
            name: `This is 4`,
            type: 'Skill',
            pointIncrement: 100,
            numPerformToCompletion: 5,
            pointIncrementInterval: 0,
            numMaxOccurrencesIncrementInterval: -1,
            description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.',
            version: 0,
            helpUrl: 'http://doHelpOnThisSkill.com',
        });

        cy.request('POST', '/admin/projects/MyNewtestProject/badge/badge1/skills/skill2');

        cy.request('POST', `/admin/projects/MyNewtestProject/skills/skill2/dependency/skill1`);

        const m = moment('2020-05-12 11', 'YYYY-MM-DD HH');
        cy.request('POST', `/api/projects/MyNewtestProject/skills/skill1`, {
            userId: 'u1',
            timestamp: m.format('x')
        });
        cy.request('POST', `/api/projects/MyNewtestProject/skills/skill1`, {
            userId: 'u2',
            timestamp: m.subtract(4, 'day')
                .format('x')
        });
        cy.request('POST', `/api/projects/MyNewtestProject/skills/skill1`, {
            userId: 'u3',
            timestamp: m.subtract(3, 'day')
                .format('x')
        });
        cy.request('POST', `/api/projects/MyNewtestProject/skills/skill1`, {
            userId: 'u4',
            timestamp: m.subtract(2, 'day')
                .format('x')
        });
        cy.request('POST', `/api/projects/MyNewtestProject/skills/skill1`, {
            userId: 'u5',
            timestamp: m.subtract(1, 'day')
                .format('x')
        });
        cy.request('POST', `/api/projects/MyNewtestProject/skills/skill3`, {
            userId: 'u5',
            timestamp: m.subtract(1, 'day')
                .format('x')
        });
        cy.request('POST', `/api/projects/MyNewtestProject/skills/skill3`, {
            userId: 'u6',
            timestamp: m.subtract(1, 'day')
                .format('x')
        });
        cy.request('POST', `/api/projects/MyNewtestProject/skills/skill3`, {
            userId: 'u7',
            timestamp: m.subtract(1, 'day')
                .format('x')
        });

        cy.request('POST', `/api/projects/MyNewtestProject/skills/skill4`, {
            userId: 'u8',
            timestamp: m.subtract(1, 'day')
                .format('x')
        });
        cy.request('POST', `/api/projects/MyNewtestProject/skills/skill4`, {
            userId: 'u8',
            timestamp: m.subtract(2, 'day')
                .format('x')
        });
        cy.request('POST', `/api/projects/MyNewtestProject/skills/skill4`, {
            userId: 'u8',
            timestamp: m.subtract(3, 'day')
                .format('x')
        });
        cy.request('POST', `/api/projects/MyNewtestProject/skills/skill4`, {
            userId: 'u8',
            timestamp: m.subtract(4, 'day')
                .format('x')
        });
        cy.request('POST', `/api/projects/MyNewtestProject/skills/skill4`, {
            userId: 'u8',
            timestamp: m.subtract(5, 'day')
                .format('x')
        });
    });


    it('project', () => {
        //report skills that dont' exist
        cy.reportSkill('MyNewtestProject', 42, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill('MyNewtestProject', 75, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill('MyNewtestProject', 75, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill('MyNewtestProject', 75, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill('MyNewtestProject', 13, 'user@skills.org', '2021-02-24 10:00', false);

        cy.visit('/administrator/');
        cy.injectAxe();
        //view project
        cy.get('[data-cy=projCard_MyNewtestProject_manageBtn]')
            .click();
        // wait on subjects
        cy.get('[data-cy=manageBtn_subj1]');

        cy.customLighthouse();
        cy.get('[aria-label="new subject"]')
            .click();
        cy.get('[data-cy=subjectNameInput]')
            .type('a');
        cy.customA11y();
        cy.get('[data-cy=closeSubjectButton]')
            .click();

        cy.get('[data-cy=nav-Badges]')
            .click();
        cy.contains('Badge 1');

        cy.customLighthouse();
        cy.get('[aria-label="new badge"]')
            .click();
        cy.get('[data-cy=badgeName')
            .type('a');
        cy.customA11y();
        cy.get('[data-cy=closeBadgeButton]')
            .click();

        // --- Self Report Page ----
        cy.get('[data-cy="nav-Self Report"]')
            .click();
        cy.get('[data-cy="skillsReportApprovalTable"] tbody tr')
            .should('have.length', 3);
        cy.customLighthouse();
        cy.customA11y();

        cy.get('[data-cy="selectPageOfApprovalsBtn"]')
            .click();
        cy.get('[data-cy="rejectBtn"]')
            .click();
        cy.get('[data-cy="rejectionTitle"]')
            .contains('This will reject user\'s request(s) to get points');
        cy.wait(500); // wait for modal to continue loading, if background doesn't load the contract checks will fail
        cy.customA11y();
        cy.get('[data-cy="cancelRejectionBtn"]')
            .click();

        // --- Deps Page ----
        cy.get('[data-cy=nav-Dependencies]')
            .click();
        cy.contains('Color Legend');
        cy.customLighthouse();
        cy.customA11y();

        //levels
        cy.get('[data-cy=nav-Levels')
            .click();
        // cy.contains('White Belt');
        cy.customLighthouse();
        cy.get('[data-cy=addLevel]')
            .click();
        cy.get('[data-cy=levelPercent]')
            .type('1100');
        cy.customA11y();
        cy.get('[data-cy=cancelLevel]')
            .click();

        //users
        cy.get('[data-cy=nav-Users')
            .click();
        cy.contains('ID: MyNewtestProject');
        cy.get('[data-cy="usersTable"]')
            .contains('u1');
        cy.contains('User Id Filter');
        cy.contains('Total Rows: 6');
        cy.customLighthouse();
        cy.customA11y();

        // --- metrics ----
        cy.get('[data-cy=nav-Metrics]')
            .click();
        cy.contains('Users per day');
        cy.contains('This chart needs at least 2 days of user activity.');
        cy.customLighthouse();
        cy.customA11y();

        cy.get('[data-cy="Achievements-metrics-link"]')
            .click();
        cy.contains('Level 2: 1 users');
        cy.contains('Level 1: 0 users');
        cy.get('[data-cy=achievementsNavigator-table]')
            .contains('u8');
        cy.wait(2000); // wait for charts to finish loading
        cy.customLighthouse();
        cy.customA11y();

        cy.get('[data-cy="Subjects-metrics-link"]')
            .click();
        cy.contains('Number of users for each level over time');
        cy.wait(4000); // wait for charts to finish loading
        cy.customLighthouse();
        cy.customA11y();

        cy.get('[data-cy="Skills-metrics-link"]')
            .click();
        cy.get('[data-cy=skillsNavigator-table]')
            .contains('This is 1');
        cy.customLighthouse();
        cy.customA11y();

        // --- access page ----
        cy.get('[data-cy=nav-Access')
            .click();
        cy.contains('Trusted Client Properties');
        cy.contains('ID: MyNewtestProject');
        const tableSelector = '[data-cy="roleManagerTable"]';
        cy.get(tableSelector)
            .contains('Loading...')
            .should('not.exist');
        cy.get(tableSelector)
            .contains('There are no records to show')
            .should('not.exist');
        cy.get(`${tableSelector} tbody tr`)
            .should('have.length', 1);
        cy.customLighthouse();
        cy.customA11y();

        cy.get('[data-cy=nav-Settings]')
            .click();
        cy.contains('Root Help Url');
        cy.customLighthouse();
        cy.customA11y();

        // --- Issues page ---
        cy.intercept('GET', '/admin/projects/MyNewtestProject/errors*')
            .as('getErrors');
        cy.get('[data-cy=nav-Issues]')
            .click();
        cy.wait('@getErrors');
        cy.contains('Remove All');
        cy.customLighthouse();
        cy.customA11y();
    });

});
