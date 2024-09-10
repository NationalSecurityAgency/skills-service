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

describe('Accessibility My Projects and Progress And Ranking Home Tests', () => {

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
        it(`"My Progress" landing page with many skills${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            for (let i = 1; i <= 10; i++) {
                cy.createProject(i);
                cy.enableProdMode(i);
                cy.addToMyProjects(i);

                cy.createSubject(i, 1);
                cy.createSkill(i, 1, 100, {numPerformToCompletion: 1});
                cy.reportSkill(i, 100, Cypress.env('proxyUser'), 'now')

                cy.createBadge(i, 1);
                cy.assignSkillToBadge(i, 1, 100);
                cy.createBadge(i, 1, { enabled: true });
            }

            cy.createSubject(1, 1);
            const numSkills = 10
            for (let i = 0; i < numSkills; i++) {
                cy.createSkill(1, 1, i, {numPerformToCompletion: 1});
            }
            for (let i = 0; i < numSkills; i++) {
                cy.reportSkill(1, i, Cypress.env('proxyUser'), 'now')
            }

            cy.visit('/progress-and-rankings');
            cy.injectAxe();
            cy.get('[data-cy="numProjectsContributed"]').should('have.text', 10);
            cy.get('[data-cy="numAchievedSkills"]').should('have.text', 20);
            cy.get('[data-cy="numAchievedBadges"]').should('have.text', 10);
            cy.customLighthouse();
            cy.customA11y();
        })

        it(`"My Progress" landing page${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            // setup a project for the landing page
            const dateFormatter = value => moment.utc(value)
                .format('YYYY-MM-DD[T]HH:mm:ss[Z]');
            const timeFromNowFormatter = (value) => dayjs(value)
                .startOf('hour')
                .fromNow();
            cy.createProject(1);
            cy.enableProdMode(1);
            cy.addToMyProjects(1);
            cy.createSubject(1, 1);
            cy.createSubject(1, 2);
            cy.createSubject(1, 3);
            cy.createSkill(1, 1, 1);
            cy.createSkill(1, 1, 2);
            cy.createSkill(1, 1, 3);
            cy.createSkill(1, 1, 4);

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

            cy.request('POST', '/admin/projects/proj1/badges/gemBadge', {
                projectId: 'proj1',
                badgeId: 'gemBadge',
                name: 'Gem Badge',
                startDate: dateFormatter(new Date() - 1000 * 60 * 60 * 24 * 7),
                endDate: dateFormatter(new Date() + 1000 * 60 * 60 * 24 * 5),
            });

            cy.visit('/progress-and-rankings');
            cy.get('[data-cy="breadcrumb-Progress And Rankings"]')
                .contains('Progress And Rankings')
                .should('be.visible');
            cy.get('[data-cy=numSkillsAvailable]')
                .contains(new RegExp(/^Total: 4$/));
            cy.get('[data-cy=project-link-proj1]')
                .should('be.visible');

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y();
        });

        it(`my usage page${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.fixture('vars.json')
                .then((vars) => {
                    cy.request('POST', '/logout');
                    cy.register(Cypress.env('proxyUser'), vars.defaultPass, false);
                    cy.loginAsProxyUser();
                });
            cy.loginAsProxyUser();

            for (let i = 1; i <= 3; i += 1) {
                cy.createProject(i);
                cy.enableProdMode(i);
                cy.addToMyProjects(i);

                cy.createSubject(i, 1);
                cy.createSkill(i, 1, 1);
                cy.createSkill(i, 1, 2);
            }

            const dateFormat = 'YYYY-MM-DD HH:mm';
            const user = Cypress.env('proxyUser');
            cy.reportSkill(1, 1, user, moment.utc()
                .subtract(2, 'days')
                .format(dateFormat));
            cy.reportSkill(1, 1, user, 'yesterday');
            cy.reportSkill(1, 2, user, 'now');
            cy.reportSkill(1, 2, user, 'yesterday');

            cy.reportSkill(2, 1, user, moment.utc()
                .subtract(5, 'days')
                .format(dateFormat));
            cy.reportSkill(2, 1, user, moment.utc()
                .subtract(6, 'days')
                .format(dateFormat));
            cy.reportSkill(2, 2, user, moment.utc()
                .subtract(6, 'days')
                .format(dateFormat));
            cy.reportSkill(2, 2, user, moment.utc()
                .subtract(3, 'days')
                .format(dateFormat));

            cy.visit('/progress-and-rankings');
            cy.injectAxe();
            cy.get('[data-cy="viewUsageBtn"]')
                .click();

            cy.contains('Your Daily Usage History');
            cy.contains('6 months');
            cy.get('[data-cy=eventHistoryChartProjectSelector]')
                .contains('This is project 2')
                .should('be.visible');

            cy.wait(1500);
            cy.customLighthouse();
            cy.customA11y();
        });

        it(`splash home page${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.visit('/progress-and-rankings');
            cy.get('[data-cy="breadcrumb-Progress And Rankings"]')
              .contains('Progress And Rankings')
              .should('be.visible');
            cy.get('[data-cy="manageMyProjsBtnInNoContent"]')

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y();
        });

    })
});
