/*
 * Copyright 2024 SkillTree
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

describe('Accessibility Badges Tests', () => {

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

        cy.request('POST', `/admin/projects/MyNewtestProject/skill2/prerequisite/MyNewtestProject/skill1`);

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

        Cypress.Commands.add("setDarkModeIfNeeded", (darkMode) => {
            if (darkMode && darkMode.length > 0) {
                cy.configureDarkMode()
            }
        })

    });

    const runWithDarkMode = ['', ' - dark mode']

    runWithDarkMode.forEach((darkMode) => {

        it(`badge modal${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.visit('/administrator/projects/MyNewtestProject/badges');
            cy.injectAxe();

            cy.get('[aria-label="new badge"]').click();
            cy.get('[data-cy=name]').type('a');
            cy.customLighthouse();
            cy.customA11y();
        });

        it(`badge page${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.visit('/administrator/projects/MyNewtestProject/badges/badge1');
            cy.injectAxe();

            cy.get('[data-cy="pageHeaderStat_Status"]')
            cy.get('[data-cy="badgeSkillsTable"]').contains('This is 2');
            cy.customLighthouse();
            cy.customA11y();
        });

        it(`badge users${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.visit('/administrator/projects/MyNewtestProject/badges/badge1/users');
            cy.injectAxe();
            cy.contains('There are no records to show');
            cy.customLighthouse();
            cy.customA11y();
        });

        it(`global badges${darkMode}`, () => {
            cy.logout();
            cy.login('root@skills.org', 'password');
            cy.setDarkModeIfNeeded(darkMode)

            cy.intercept('POST', ' /supervisor/badges/globalbadgeBadge/projects/MyNewtestProject/level/1')
                .as('saveGlobalBadgeLevel');
            cy.intercept('/supervisor/badges/globalbadgeBadge/skills/available?query=').as('getAvailableSkills')
            cy.intercept('/supervisor/badges/globalbadgeBadge/projects/available?query=').as('getAvailableProjects')
            cy.intercept('/supervisor/projects/MyNewtestProject/levels').as('getProjectLevels')
            cy.request('PUT', `/root/users/root@skills.org/roles/ROLE_SUPERVISOR`);
            cy.visit('/administrator/globalBadges');
            cy.injectAxe();
            cy.get('[data-cy="noContent"]').contains('No Badges Yet')
            cy.customLighthouse();
            cy.customA11y();

            cy.get('[aria-label="new global badge"]')
                .click();
            cy.get('[data-cy=name]')
                .type('global badge');
            cy.get('[data-cy=saveDialogBtn]')
                .click();
            cy.contains('Manage')
                .click();
            cy.wait('@getAvailableSkills')
            cy.customLighthouse();
            cy.customA11y();

            cy.selectSkill('[data-cy="skillsSelector"]', 'skill1', 'This is 1', 'MyNewtestProject');
            cy.customA11y();
            cy.get('[data-cy=nav-Levels]')
                .click();
            cy.contains('No Levels Added Yet');
            cy.customLighthouse();

            cy.wait('@getAvailableProjects')
            cy.get('#project-selector')
                .click();
            cy.get('[data-cy="MyNewtestProject_option"]').click();
            cy.get('#level-selector').click();
            cy.wait('@getProjectLevels')
            cy.get('[data-pc-section="item"]').contains(2).click();
            // cy.get('.multiselect__select').eq(0).click();
            // cy.get('.multiselect__element').eq(0).click();
            // cy.get('.multiselect__select').eq(1).click();
            // cy.get('.multiselect__element').eq(1).click();

            cy.get('[data-cy=addGlobalBadgeLevel]')
                .click();
            cy.get('[data-cy="simpleLevelsTable"] [data-cy="skillsBTableTotalRows"]')
                .should('have.text', '1');
            cy.customA11y();
        });

    })
});
