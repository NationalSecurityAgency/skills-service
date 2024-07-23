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
    });

    it('"My Progress" landing page', () => {
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

    it('admin home page', () => {
        cy.visit('/administrator/');
        cy.customLighthouse();
        cy.injectAxe();
        cy.get('[data-cy=nav-Projects]');
        cy.contains('My New test Project');
        cy.customA11y();
    });

    it('skills', () => {
        cy.visit('/administrator/');
        cy.injectAxe();
        //view project
        cy.get('[data-cy="projCard_MyNewtestProject_manageBtn"]')
            .click();
        //view subject
        cy.get('[data-cy="manageBtn_subj1"]')
            .click();
        //view skill
        cy.get('[data-cy=manageSkillLink_skill1]')
            .click();
        cy.contains('Help URL');
        cy.contains('http://doHelpOnThisSkill.com');
        cy.contains('Lorem ipsum dolor sit amet, consectetur adipiscing elit,');
        cy.contains('500 Points');
        cy.customLighthouse();
        cy.customA11y();

        cy.get('[data-cy=nav-Users]')
            .click();
        cy.contains('u1');
        cy.customLighthouse();
        cy.customA11y();

        cy.get('[data-cy="nav-Add Event"]')
            .click();
        cy.customLighthouse();
        cy.customA11y();
        cy.get('[data-cy="userIdInput"]')
            .click();
        cy.get('[data-cy="existingUserInputDropdown"]').type('u4');
        cy.get('#existingUserInput_0').contains('u4').click()
        cy.get('[data-cy="eventDatePicker"]').click();
        cy.get('[aria-label="Choose Date"] [aria-label="1"]').not('[data-p-other-month="true"]').click()
        cy.get('[data-cy=addSkillEventButton]').click();
        cy.contains('Added points');
        cy.customA11y();

        cy.get('[data-cy=nav-Metrics]')
            .click();
        cy.contains('Achievements over time');
        cy.get('[data-cy="numUsersPostAchievement"]')
            .contains('No achievements yet for this skill.');
        cy.get('[data-cy="numUsersPostAchievement"]')
            .contains('No achievements yet for this skill.');

        cy.customLighthouse();
        cy.customA11y();
    });

    it('configure video for a skill', () => {
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

    it('configure expiration for a skill', () => {
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

    it('skill expirations', () => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);

        cy.configureExpiration(1, 0, 1, 'DAILY');
        let yesterday = moment.utc().subtract(1, 'day')
        let twoDaysAgo = moment.utc().subtract(2, 'day')
        cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: Cypress.env('proxyUser'), date: yesterday.format('YYYY-MM-DD HH:mm') });
        cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: Cypress.env('proxyUser'), date: twoDaysAgo.format('YYYY-MM-DD HH:mm') });
        cy.expireSkills();

        cy.visit('/administrator/projects/proj1/expirationHistory');
        cy.injectAxe();

        cy.get('[data-cy="expirationHistoryTable"]')
        cy.customLighthouse();
        cy.customA11y();
    });

    it('badges', () => {
        cy.visit('/administrator/');
        cy.injectAxe();

        cy.get('[data-cy="projCard_MyNewtestProject_manageBtn"]')
            .click();
        cy.clickNav('Badges')
            .click();

        cy.get('[aria-label="new badge"]')
            .click();
        cy.get('[data-cy=name]')
            .type('a');
        cy.customA11y();
        cy.get('[aria-label="Close"]').click()

        cy.get('[data-cy=manageBtn_badge1]')
            .click();
        cy.contains('This is 2');
        cy.customLighthouse();
        cy.customA11y();

        cy.clickNav('Users')
            .click();
        cy.contains('There are no records to show');
        cy.customLighthouse();
        cy.customA11y();
    });

    it('global badges', () => {
        cy.logout();
        cy.login('root@skills.org', 'password');

        cy.intercept('POST', ' /supervisor/badges/globalbadgeBadge/projects/MyNewtestProject/level/1')
            .as('saveGlobalBadgeLevel');
        cy.intercept('/supervisor/badges/globalbadgeBadge/skills/available?query=').as('getAvailableSkills')
        cy.intercept('/supervisor/badges/globalbadgeBadge/projects/available?query=').as('getAvailableProjects')
        cy.intercept('/supervisor/projects/MyNewtestProject/levels').as('getProjectLevels')
        cy.request('PUT', `/root/users/root@skills.org/roles/ROLE_SUPERVISOR`);
        cy.visit('/administrator');
        cy.injectAxe();
        cy.get('[data-cy="nav-Global Badges"]')
            .click();
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

        cy.selectItem('[data-cy="skillsSelector"]', 'This is 1')
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

    it('manage my projects page', () => {

        for (let i = 1; i <= 9; i += 1) {
            cy.createProject(i);
            cy.enableProdMode(i);
            if (i < 4) {
                cy.addToMyProjects(i);
            }
        }

        cy.visit('/progress-and-rankings');
        cy.injectAxe();
        cy.get('[data-cy="manageMyProjsBtn"]')
            .click();

        cy.get('[data-cy="allProjectsCount"] [data-cy="mediaInfoCardTitle"]')
            .contains('9');

        cy.wait(1500);
        cy.customLighthouse();
        cy.customA11y();
    });

    it('my usage page', () => {
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

    it('self report admin page', () => {
        Cypress.Commands.add('rejectRequest', (requestNum = 0, rejectionMsg = 'Skill was rejected') => {
            cy.request('/admin/projects/proj1/approvals?limit=10&ascending=true&page=1&orderBy=userId')
                .then((response) => {
                    cy.request('POST', '/admin/projects/proj1/approvals/reject', {
                        skillApprovalIds: [response.body.data[requestNum].id],
                        rejectionMessage: rejectionMsg,
                    });
                });
        });
        Cypress.Commands.add('approveAllRequests', () => {
            cy.request('/admin/projects/proj1/approvals?limit=10&ascending=true&page=1&orderBy=userId')
                .then((response) => {
                    response.body.data.forEach((item) => {
                        cy.wait(200); // that way sort works properly
                        cy.request('POST', '/admin/projects/proj1/approvals/approve', {
                            skillApprovalIds: [item.id],
                        });
                    });
                });
        });

        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });
        cy.reportSkill(1, 2, 'user6', '2020-09-11 11:00');
        cy.reportSkill(1, 2, 'user5', '2020-09-12 11:00');
        cy.reportSkill(1, 2, 'user4', '2020-09-13 11:00');
        cy.approveAllRequests();
        cy.reportSkill(1, 2, 'user2', '2020-09-16 11:00');
        cy.rejectRequest(0);

        cy.reportSkill(1, 3, 'user1', '2020-09-17 11:00');
        cy.reportSkill(1, 1, 'user0', '2020-09-18 11:00');
        cy.approveAllRequests();

        cy.reportSkill(1, 2, 'user3', '2020-09-14 11:00');
        cy.rejectRequest(0);

        cy.reportSkill(1, 2, 'user8', '2020-09-14 11:00');
        cy.reportSkill(1, 2, 'user9', '2020-09-14 11:00');

        cy.visit('/administrator/projects/proj1/self-report');
        cy.get('[data-cy="skillsReportApprovalTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', 2)
        cy.get('[data-cy="selfReportApprovalHistoryTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', 7)
        cy.injectAxe();

        cy.customLighthouse();
        cy.customA11y();
    });

    if (!Cypress.env('oauthMode')) {
        it('user tags on the metrics page', () => {
            const userTagsTableSelector = '[data-cy="userTagsTable"]';

            cy.createProject(1);
            cy.createSubject(1, 1);
            cy.createSkill(1, 1, 1);

            cy.logout();
            cy.fixture('vars.json')
                .then((vars) => {
                    cy.login(vars.rootUser, vars.defaultPass);
                });

            Cypress.Commands.add('addUserTag', (userId, tagKey, tags) => {
                cy.request('POST', `/root/users/${userId}/tags/${tagKey}`, { tags });
            });

            const createTags = (numTags, tagKey) => {
                for (let i = 0; i < numTags; i += 1) {
                    const userId = `user${i}`;
                    cy.reportSkill(1, 1, userId, 'now');

                    const tags = [];
                    for (let j = 0; j <= i; j += 1) {
                        tags.push(`tag${j}`);
                    }
                    cy.addUserTag(userId, tagKey, tags);
                }
            };

            createTags(21, 'someValues');
            createTags(25, 'manyValues');

            cy.logout();
            cy.fixture('vars.json')
                .then((vars) => {
                    cy.login(vars.defaultUser, vars.defaultPass);
                });

            cy.intercept('GET', '/public/config', (req) => {
                req.reply({
                    body: {
                        projectMetricsTagCharts: '[{"key":"manyValues","type":"table","title":"Many Values","tagLabel":"Best Label"},{"key":"someValues","type":"bar","title":"Some Values"}]'
                    },
                });
            })
                .as('getConfig');
            cy.viewport(1200, 1000);

            cy.visit('/administrator/projects/proj1/');
            cy.wait('@getConfig');
            cy.injectAxe();

            cy.clickNav('Metrics');
            cy.get('[data-cy="userTagTableCard"] [data-pc-section="header"]')
                .contains('Many Values');
            cy.get(`${userTagsTableSelector} th`)
                .contains('Best Label');
            cy.get(`${userTagsTableSelector} th`)
                .contains('# Users');

            cy.wait(3000);

            cy.customLighthouse();
            cy.customA11y();
        });

        it('skills groups', () => {
            cy.createProject(1);
            cy.createSubject(1, 1);
            cy.createSkillsGroup(1, 1, 1);
            cy.addSkillToGroup(1, 1, 1, 11, {
                pointIncrement: 10,
                numPerformToCompletion: 5
            });
            cy.addSkillToGroup(1, 1, 1, 22, {
                pointIncrement: 10,
                numPerformToCompletion: 5
            });
            const groupId = 'group1';

            cy.visit('/administrator/projects/proj1/subjects/subj1');
            cy.injectAxe();

            cy.get('[data-p-index="0"] [data-pc-section="rowtoggler"]').click()
            cy.get('[data-cy="editSkillButton_skill11"]');
            cy.get('[data-cy="editSkillButton_skill22"]');

            cy.customLighthouse();
            cy.customA11y();

            cy.createSkillsGroup(1, 1, 1, {
                numSkillsRequired: 1,
                enabled: true
            });

            cy.visit('/administrator/projects/proj1/subjects/subj1');
            cy.injectAxe();

            cy.get('[data-p-index="0"] [data-pc-section="rowtoggler"]').click()
            cy.get('[data-cy="editSkillButton_skill11"]');
            cy.get('[data-cy="editSkillButton_skill22"]');

            cy.customLighthouse();
            cy.customA11y();

            cy.createSkillsGroup(1, 1, 1, {
                numSkillsRequired: -1,
                enabled: true
            });
            cy.addSkillToGroup(1, 1, 1, 33, {
                pointIncrement: 11,
                numPerformToCompletion: 2
            });
        });
    }

    it('configure self approval workload', function () {
        const pass = 'password';
        cy.register('user1', pass);
        cy.register('user2', pass);
        cy.register('user3', pass);
        cy.fixture('vars.json')
            .then((vars) => {
                if (!Cypress.env('oauthMode')) {
                    cy.log('NOT in oauthMode, using form login');
                    cy.login(vars.defaultUser, vars.defaultPass);
                } else {
                    cy.log('oauthMode, using loginBySingleSignOn');
                    cy.loginBySingleSignOn();
                }
            });
        cy.request('POST', `/admin/projects/MyNewtestProject/users/user1/roles/ROLE_PROJECT_APPROVER`);
        cy.request('POST', `/admin/projects/MyNewtestProject/users/user2/roles/ROLE_PROJECT_APPROVER`);
        cy.request('POST', `/admin/projects/MyNewtestProject/approverConf/user1`, { userId: 'u1' });
        cy.request('POST', `/admin/projects/MyNewtestProject/approverConf/user1`, { userId: 'u2' });
        cy.request('POST', `/admin/projects/MyNewtestProject/approverConf/user1`, { userId: 'u3' });
        cy.request('POST', `/admin/projects/MyNewtestProject/approverConf/user1`, { userId: 'u4' });
        cy.request('POST', `/admin/projects/MyNewtestProject/approverConf/user1`, { userId: 'u5' });
        cy.request('POST', `/admin/projects/MyNewtestProject/approverConf/user1`, {
            userTagKey: 'tagKey',
            userTagValue: 'tagValue'
        });
        cy.request('POST', `/admin/projects/MyNewtestProject/approverConf/user1`, {
            skillId: 'skill1'
        });
        cy.request('POST', `/admin/projects/MyNewtestProject/approverConf/user1`, {
            skillId: 'skill2'
        });
        cy.visit('/administrator/projects/MyNewtestProject/self-report/configure');
        cy.injectAxe();

        cy.get(`[data-cy="workloadCell_user1"] [data-cy="editApprovalBtn"]`).click()
        const tableSelector = `[data-cy="expandedChild_user1"] [data-cy="skillApprovalConfSpecificUsersTable"]`
        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`).should('have.text', '5')

        cy.get(`[data-cy="workloadCell_user2"] [data-cy="editApprovalBtn"]`).click()
        cy.get(`[data-cy="expandedChild_user2"] [data-cy="noUserConf"]`).should('exist')

        cy.customLighthouse();
        cy.customA11y();
    });

    it.only('splash home page', () => {
        cy.visit('/progress-and-rankings');
        cy.get('[data-cy="breadcrumb-Progress And Rankings"]')
          .contains('Progress And Rankings')
          .should('be.visible');
        cy.get('[data-cy="manageMyProjsBtnInNoContent"]')

        cy.customLighthouse();
        cy.injectAxe();
        cy.customA11y();
    });
});
