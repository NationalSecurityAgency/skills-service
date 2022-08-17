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

describe('Modifications not permitted when upgrade in progress is configured', () => {

    /*
    // temporarily removed pending cypress version upgrade
    beforeEach(() => {
        cy.request('POST', '/app/projects/MyNewtestProject', {
            projectId: 'MyNewtestProject',
            name: "My New test Project"
        });

        cy.request('POST', '/app/projects/MyNewtestProject2', {
            projectId: 'MyNewtestProject2',
            name: "My New test Project2"
        });

        cy.request('POST', '/admin/projects/MyNewtestProject/subjects/subj1', {
            projectId: 'MyNewtestProject',
            subjectId: 'subj1',
            name: "Subject 1"
        });

        cy.request('POST', '/admin/projects/MyNewtestProject/badges/badge1', {
            projectId: 'MyNewtestProject',
            badgeId: 'badge1',
            name: "Badge 1"
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
            description: "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
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
            description: "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
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
            description: "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
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
            description: "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
            version: 0,
            helpUrl: 'http://doHelpOnThisSkill.com',
        });

        cy.request('POST', '/admin/projects/MyNewtestProject/badge/badge1/skills/skill2')
    });

    it("upgrade warning banner is displayed", () => {
        cy.intercept('/!**', (req) => {
            cy.log('!!!!intercepted request ['+req+']');
            req.continue((res) => {
                cy.log('!!!continuing request and adding header');
                res.headers['upgrade-in-progress'] = 'true';
            })
        });
        cy.intercept('GET', '/public/config', (req) => {
            req.reply({
                headers: {
                    'upgrade-in-progress': 'true',
                },
                body: {
                    rankingAndProgressViewsEnabled: 'true',
                    dbUpgradeInProgress: 'true'
                },
            })
        }).as('getConfig');
        cy.intercept('GET', '/app/projects', (req) => {
            cy.log('!!!intercepted /app/projects');
            req.continue((res) => {
                cy.log('!!!modifying /app/projects response to include header');
                res.headers['upgrade-in-progress'] = 'true';
            })
        }).as('loadProjects');

        cy.visit('/administrator/');
        cy.wait('@getConfig');
        cy.wait('@loadProjects');

        cy.get('[data-cy=upgradeInProgressWarning]').should('be.visible');
    });

    it("upgrade warning removed if subsquent requests indicate upgrade has concluded", () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply({
                body: {
                    rankingAndProgressViewsEnabled: 'true',
                    dbUpgradeInProgress: 'true'
                },
            });
        }).as('getConfig');
        cy.intercept('GET', '/app/projects').as('loadProjects');

        cy.visit('/administrator/');
        cy.wait('@getConfig');
        cy.wait('@loadProjects');

        cy.intercept('GET', '/admin/projects/MyNewtestProject/subjects', (req) => {
            req.reply({
                headers: {
                    'upgrade-in-progress': 'false',
                },
                body: [],
            });
        }).as('loadSubject');
        cy.get('[data-cy=upgradeInProgressWarning]').should('be.visible');
        cy.get('[data-cy="projCard_MyNewtestProject_manageLink').click();
        cy.wait('@loadSubject');
        cy.get('[data-cy=upgradeInProgressWarning]').should('not.exist');
    })

    it("upgrade warning should display if subsquent requests indicate upgrade has started", () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply({
                body: {
                    rankingAndProgressViewsEnabled: 'false',
                    dbUpgradeInProgress: 'true'
                },
            });
        }).as('getConfig');
        cy.intercept('GET', '/app/projects').as('loadProjects');

        cy.visit('/administrator/');
        cy.wait('@getConfig');
        cy.wait('@loadProjects');

        cy.intercept('GET', '/admin/projects/MyNewtestProject/subjects', (req) => {
            req.reply({
                headers: {
                    'upgrade-in-progress': 'true',
                },
                body: [],
            });
        }).as('loadSubject');
        cy.get('[data-cy=upgradeInProgressWarning]').should('not.exist');
        cy.get('[data-cy="projCard_MyNewtestProject_manageLink').click();
        cy.wait('@loadSubject');
        cy.get('[data-cy=upgradeInProgressWarning]').should('be.visible');
    })*/

});