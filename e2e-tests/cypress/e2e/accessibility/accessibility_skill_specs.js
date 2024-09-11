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

describe('Accessibility Skill Tests', () => {

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
        it(`skill overview ${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.visit('/administrator/projects/MyNewtestProject/subjects/subj1/skills/skill1');
            cy.injectAxe();
            cy.contains('Help URL');
            cy.contains('http://doHelpOnThisSkill.com');
            cy.contains('Lorem ipsum dolor sit amet, consectetur adipiscing elit,');
            cy.contains('500 Points');
            cy.customLighthouse();
            cy.customA11y();
        });

        it(`configure video for a skill${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.createProject(1);
            cy.createSubject(1, 1);
            cy.createSkill(1, 1, 1);
            cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1/config-video');
            cy.injectAxe();

            cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.disabled')
            cy.get('[data-cy="showExternalUrlBtn"]')
            cy.customLighthouse();
            cy.customA11y();
        });

        it(`configure expiration for a skill${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.createProject(1);
            cy.createSubject(1, 1);
            cy.createSkill(1, 1, 1);
            cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1/config-expiration');
            cy.injectAxe();

            cy.get('[data-cy="saveSettingsBtn"]').should('be.disabled')
            cy.get('[data-cy="expirationNeverRadio"]')
            cy.customLighthouse();
            cy.customA11y();
        });

        it(`skill users ${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.visit('/administrator/projects/MyNewtestProject/subjects/subj1/skills/skill1/users');
            cy.injectAxe();
            cy.contains('u1');
            cy.customLighthouse();
            cy.customA11y();
        });

        it(`skill add event ${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.visit('/administrator/projects/MyNewtestProject/subjects/subj1/skills/skill1/addSkillEvent');
            cy.injectAxe();
            cy.get('[data-cy="userIdInput"]')
            cy.customLighthouse();
            cy.customA11y();

            cy.get('[data-cy="userIdInput"]').click();
            cy.get('[data-cy="existingUserInputDropdown"]').type('u4');
            cy.get('#existingUserInput_0').contains('u4').click()
            cy.get('[data-cy="eventDatePicker"]').click();
            cy.get('[aria-label="Choose Date"] [aria-label="1"]').not('[data-p-other-month="true"]').click()
            cy.get('[data-cy=addSkillEventButton]').click();
            cy.contains('Added points');
            cy.customA11y();
        });

        it(`skill metrics ${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.visit('/administrator/projects/MyNewtestProject/subjects/subj1/skills/skill1/metrics');
            cy.injectAxe();
            cy.contains('Achievements over time');
            cy.get('[data-cy="numUsersPostAchievement"]')
              .contains('No achievements yet for this skill.');
            cy.get('[data-cy="numUsersPostAchievement"]')
              .contains('No achievements yet for this skill.');

            cy.customLighthouse();
            cy.customA11y();
        });
    })
});
