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
var moment = require('moment-timezone');

describe('Metrics Using User Tags Tests', () => {

    const userTagsTableSelector = '[data-cy="userTagsTable"]';
    const usersTableSelector = '[data-cy=usersTable]'

    after(() => {
        Cypress.env('disableResetDb', false);
    });

    before(() => {
        Cypress.env('disableResetDb', true);
        cy.resetDb();

        cy.logout();
        cy.fixture('vars.json')
            .then((vars) => {
                cy.login(vars.defaultUser, vars.defaultPass);
            });

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

    });

    beforeEach(() => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply({
                body: {
                    projectMetricsTagCharts: '[{"key":"manyValues","type":"table","title":"Many Values","tagLabel":"Best Label"},{"key":"someValues","type":"bar","title":"Some Values"}]'
                },
            });
        })
            .as('getConfig');
        cy.viewport(1200, 1000);
    });

    if (!Cypress.env('oauthMode')) {
        it('user tag table', () => {
            cy.visit('/administrator/projects/proj1/');
            cy.wait('@getConfig');

            cy.clickNav('Metrics');
            cy.get('[data-cy="userTagTableCard"] [data-pc-section="header"]')
                .contains('Many Values');
            cy.get(`${userTagsTableSelector} th`)
                .contains('Best Label');
            cy.get(`${userTagsTableSelector} th`)
                .contains('# Users');

            const expected = [];
            for (let i = 0; i < 25; i += 1) {
                expected.push([
                    {
                        colIndex: 0,
                        value: `tag${i}`
                    },
                    {
                        colIndex: 1,
                        value: `${25 - i}`
                    }
                ]);
            }

            // test values and paging
            cy.validateTable(userTagsTableSelector, expected, 10);
        });

        it('user tag table - ability to sort by tag', () => {
            cy.visit('/administrator/projects/proj1/');
            cy.wait('@getConfig');

            cy.clickNav('Metrics');
            cy.get('[data-cy="userTagTableCard"] [data-pc-section="header"]')
              .contains('Many Values');

            cy.get(`${userTagsTableSelector} th`)
              .contains('# Users')
              .click();

            const expected = [];
            for (let i = 0; i < 10; i += 1) {
                expected.push([
                    {
                        colIndex: 1,
                        value: `${1 + i}`
                    }
                ]);
            }
            cy.validateTable(userTagsTableSelector, expected, 10, true, 25);

            cy.get(`${userTagsTableSelector} th`)
              .contains('Best Label')
              .click();
            const expected2 = [
                [{
                    colIndex: 0,
                    value: 'tag0'
                }],
                [{
                    colIndex: 0,
                    value: 'tag1'
                }],
                [{
                    colIndex: 0,
                    value: 'tag10'
                }],
                [{
                    colIndex: 0,
                    value: 'tag11'
                }],
                [{
                    colIndex: 0,
                    value: 'tag12'
                }],
                [{
                    colIndex: 0,
                    value: 'tag13'
                }],
                [{
                    colIndex: 0,
                    value: 'tag14'
                }],
                [{
                    colIndex: 0,
                    value: 'tag15'
                }],
                [{
                    colIndex: 0,
                    value: 'tag16'
                }],
                [{
                    colIndex: 0,
                    value: 'tag17'
                }],
            ];
            cy.validateTable(userTagsTableSelector, expected2, 10, true, 25);
        });

        it('user tag table - filter by tag - press button', () => {
            cy.visit('/administrator/projects/proj1/');
            cy.wait('@getConfig');

            cy.clickNav('Metrics');
            cy.get('[data-cy="userTagTableCard"] [data-pc-section="header"]')
                .contains('Many Values');
            cy.get('[data-cy="userTagTable-tagFilter"]')
                .type('aG2');
            cy.get('[ data-cy="userTagTable-filterBtn"]')
                .click();

            const expected2 = [
                [{
                    colIndex: 0,
                    value: 'tag2'
                }],
                [{
                    colIndex: 0,
                    value: 'tag20'
                }],
                [{
                    colIndex: 0,
                    value: 'tag21'
                }],
                [{
                    colIndex: 0,
                    value: 'tag22'
                }],
                [{
                    colIndex: 0,
                    value: 'tag23'
                }],
                [{
                    colIndex: 0,
                    value: 'tag24'
                }],
            ];
            cy.validateTable(userTagsTableSelector, expected2, 10);
        });

        it('user tag table - filter by tag - press enter', () => {
            cy.visit('/administrator/projects/proj1/');
            cy.wait('@getConfig');

            cy.clickNav('Metrics');
            cy.get('[data-cy="userTagTableCard"] [data-pc-section="header"]')
                .contains('Many Values');
            cy.get('[data-cy="userTagTable-tagFilter"]')
                .type('aG2{enter}');

            const expected2 = [
                [{
                    colIndex: 0,
                    value: 'tag2'
                }],
                [{
                    colIndex: 0,
                    value: 'tag20'
                }],
                [{
                    colIndex: 0,
                    value: 'tag21'
                }],
                [{
                    colIndex: 0,
                    value: 'tag22'
                }],
                [{
                    colIndex: 0,
                    value: 'tag23'
                }],
                [{
                    colIndex: 0,
                    value: 'tag24'
                }],
            ];
            cy.validateTable(userTagsTableSelector, expected2, 10);
        });

        it('user tag table - clear filter', () => {
            cy.visit('/administrator/projects/proj1/');
            cy.wait('@getConfig');

            cy.clickNav('Metrics');
            cy.get('[data-cy="userTagTableCard"] [data-pc-section="header"]')
                .contains('Many Values');
            cy.get('[data-cy="userTagTable-tagFilter"]')
                .type('aG2{enter}');
            cy.get(`${userTagsTableSelector}`)
                .contains('Total Rows: 6');

            cy.get('[data-cy="userTagTable-clearBtn"]')
                .click();
            cy.get(`${userTagsTableSelector}`)
                .contains('Total Rows: 25');
        });

        it('bar chart', () => {
            cy.visit('/administrator/projects/proj1/');
            cy.wait('@getConfig');

            cy.clickNav('Metrics');
            cy.get('[data-cy="userTagChart"] [data-pc-section="header"]')
                .contains('Some Values (Top 20)');
            cy.get('[data-cy="userTagChart"]')
                .contains('tag0: 21 users');
            cy.get('[data-cy="userTagChart"]')
                .contains('tag1: 20 users');
            cy.get('[data-cy="userTagChart"]')
                .contains('tag2: 19 users');
            cy.get('[data-cy="userTagChart"]')
                .contains('tag3: 18 users');
            cy.get('[data-cy="userTagChart"]')
                .contains('tag4: 17 users');
            cy.get('[data-cy="userTagChart"]')
                .contains('tag5: 16 users');
            cy.get('[data-cy="userTagChart"]')
                .contains('tag6: 15 users');
            cy.get('[data-cy="userTagChart"]')
                .contains('tag7: 14 users');
            cy.get('[data-cy="userTagChart"]')
                .contains('tag8: 13 users');
            cy.get('[data-cy="userTagChart"]')
                .contains('tag9: 12 users');
            cy.get('[data-cy="userTagChart"]')
                .contains('tag10: 11 users');
            cy.get('[data-cy="userTagChart"]')
                .contains('tag11: 10 users');
            cy.get('[data-cy="userTagChart"]')
                .contains('tag12: 9 users');
            cy.get('[data-cy="userTagChart"]')
                .contains('tag13: 8 users');
            cy.get('[data-cy="userTagChart"]')
                .contains('tag14: 7 users');
            cy.get('[data-cy="userTagChart"]')
                .contains('tag15: 6 users');
            cy.get('[data-cy="userTagChart"]')
                .contains('tag16: 5 users');
            cy.get('[data-cy="userTagChart"]')
                .contains('tag17: 4 users');
            cy.get('[data-cy="userTagChart"]')
                .contains('tag18: 3 users');
            cy.get('[data-cy="userTagChart"]')
                .contains('tag19: 2 users');
            cy.get('[data-cy="userTagChart"]')
                .contains('tag20')
                .should('not.exist');

        });

        it('navigate to user tag metrics', () => {

            cy.reportSkill(1, 1, 'notInMetrics1', 'now');
            cy.reportSkill(1, 1, 'notInMetrics2', 'now');
            cy.reportSkill(1, 1, 'notInMetrics3', 'now');

            cy.visit('/administrator/projects/proj1/');
            cy.wait('@getConfig');

            cy.clickNav('Metrics');
            cy.get('[data-cy="userTagTableCard"] [data-pc-section="header"]').contains('Many Values');
            cy.get(`${userTagsTableSelector} th`).contains('Best Label');
            cy.get(`${userTagsTableSelector} th`).contains('# Users');

            cy.get(`${userTagsTableSelector} [data-cy="userTagTable_viewMetricsLink"]`).first().click();

            cy.get('[data-cy=levelsChart]')
              .contains('Level 5: 0 users');
            cy.get('[data-cy=levelsChart]')
              .contains('Level 4: 0 users');
            cy.get('[data-cy=levelsChart]')
              .contains('Level 3: 25 users');
            cy.get('[data-cy=levelsChart]')
              .contains('Level 2: 0 users');
            cy.get('[data-cy=levelsChart]')
              .contains('Level 1: 0 users');

            cy.intercept('/admin/projects/proj1/userTags/manyValues/tag0/users*').as('getUsers');
            cy.get(`${usersTableSelector}`).contains('User').click();
            cy.wait('@getUsers')

            cy.get('[data-cy="levelsChart"] [data-pc-section="header"]').contains('Overall Levels for Best Label: tag0')
            cy.get('[data-cy="usersTableMetric"] [data-pc-section="header"]').contains('Users for Best Label: tag0')

            cy.get('[data-cy="usr_progress-user0"] [data-cy="progressPercent"]').should('have.text', '50%')
            cy.get('[data-cy="usr_progress-user0"] [data-cy="progressCurrentPoints"]').should('have.text', '100')
            cy.get('[data-cy="usr_progress-user0"] [data-cy="progressTotalPoints"]').should('have.text', '200')
            cy.get('[data-cy="usersTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '25')

            cy.get('[data-cy="breadcrumb-Metrics"]').click()
            cy.get('[data-cy="cell_tagValue-tag5"] [data-cy="userTagTable_viewMetricsLink"]').click()
            cy.get('[data-cy="usersTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '20')
        });
    }
});
