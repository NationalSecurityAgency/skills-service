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
describe('Project Errors Tests', () => {
    beforeEach(() => {
        cy.intercept('GET', '/app/projects')
            .as('getProjects');
        cy.intercept('GET', '/app/userInfo')
            .as('getUserInfo');

        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1'
        });
    });

    it('issues card should be updated when Project Issues page is loaded', () => {
        cy.visit('/administrator/projects/proj1/');
        // cy.wait('@getProject');
        cy.get('[data-cy=pageHeaderStat_Issues]')
            .contains('0');
        cy.get('[data-cy=nav-Issues]')
            .click();
        cy.get('[data-cy=projectErrorsTable]')
            .contains('No issues found');

        cy.reportSkill(1, 42, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill(1, 75, 'user@skills.org', '2021-02-24 10:00', false);
        cy.clickNav('Access').click();
        cy.clickNav('Issues').click();
        const expected = [
            [{
                colIndex: 0,
                value: 'SkillNotFound'
            }],
            [{
                colIndex: 0,
                value: 'SkillNotFound'
            }],
        ];
        const tableSelector = '[data-cy=projectErrorsTable]';
        cy.validateTable(tableSelector, expected);
        cy.get('[data-cy=pageHeaderStat_Issues]').contains('2');
    });

    it('displays errors associated with project', () => {
        cy.reportSkill(1, 42, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill(1, 75, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill(1, 75, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill(1, 75, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill(1, 13, 'user@skills.org', '2021-02-24 10:00', false);

        cy.intercept('GET', '/admin/projects/proj1')
            .as('getProject');
        cy.intercept('GET', '/admin/projects/proj1/errors**')
            .as('getErrors');

        cy.visit('/administrator/projects/proj1/');
        cy.wait('@getProject');
        cy.get('[data-cy=pageHeaderStat_Issues]')
            .contains('3');
        cy.get('[data-cy=nav-Issues]')
            .click();
        cy.wait('@getErrors');
        cy.get('[data-cy=projectErrorsTable]')
            .should('be.visible');
        cy.contains('skill42')
            .should('be.visible');
    });

    it('delete all errors updates stat', () => {
        cy.reportSkill(1, 42, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill(1, 75, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill(1, 75, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill(1, 75, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill(1, 13, 'user@skills.org', '2021-02-24 10:00', false);

        cy.intercept('GET', '/admin/projects/proj1')
            .as('getProject');
        cy.intercept('GET', '/admin/projects/proj1/errors**')
            .as('getErrors');
        cy.intercept('DELETE', '/admin/projects/proj1/errors')
            .as('deleteAllErrors');

        cy.visit('/administrator/projects/proj1/');
        cy.wait('@getProject');
        cy.get('[data-cy=pageHeaderStat_Issues]')
            .contains('3');
        cy.get('[data-cy=nav-Issues]')
            .click();
        cy.wait('@getErrors');
        cy.get('[data-cy=projectErrorsTable]')
            .should('be.visible');
        cy.contains('skill42')
            .should('be.visible');

        cy.get('[data-cy=removeAllErrors]')
            .click();
        cy.contains('Please Confirm!');
        cy.wait(1000); //have to wait on the fade in animation, otherwise spordaic failures
        cy.contains('YES, Delete It!')
            .click();
        cy.wait('@deleteAllErrors');
        cy.wait('@getProject');
        cy.get('[data-cy=pageHeaderStat_Issues]')
            .contains('0');
        cy.get('[data-cy=projectErrorsTable]')
            .should('be.visible');
        cy.get('[data-cy=emptyTable]')
            .should('be.visible');
    });

    it('delete single error updates stat', () => {
        cy.reportSkill(1, 42, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill(1, 75, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill(1, 75, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill(1, 75, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill(1, 13, 'user@skills.org', '2021-02-24 10:00', false);

        cy.intercept('GET', '/admin/projects/proj1')
            .as('getProject');
        cy.intercept('GET', '/admin/projects/proj1/errors**')
            .as('getErrors');
        cy.intercept('DELETE', '/admin/projects/proj1/errors/*')
            .as('deleteError');

        cy.visit('/administrator/projects/proj1/');
        cy.wait('@getProject');
        cy.get('[data-cy=pageHeaderStat_Issues]')
            .contains('3');
        cy.get('[data-cy=nav-Issues]')
            .click();
        cy.wait('@getErrors');
        cy.get('[data-cy=projectErrorsTable]')
            .should('be.visible');
        cy.contains('skill42')
            .should('be.visible');

        cy.get('[data-cy=deleteErrorButton_skill42]')
            .click();
        cy.contains('Please Confirm!');
        cy.wait(1000); //have to wait on the fade in animation, otherwise sporadic failures
        cy.contains('YES, Delete It!')
            .click();
        cy.wait('@deleteError');
        cy.wait('@getProject');
        cy.get('[data-cy=deleteErrorButton_skill42]')
            .should('not.exist');
        cy.get('[data-cy=pageHeaderStat_Issues]')
            .contains('2');
    });

    it('ability to sort by each column', () => {
        cy.intercept('GET', '/admin/projects/proj1/errors**').as('getErrors');

        cy.reportSkill(1, 42, 'user@skills.org', '2021-02-24 10:00', false);
        cy.wait(1000)
        cy.reportSkill(1, 75, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill(1, 75, 'user@skills.org', '2021-02-24 10:00', false);
        cy.wait(1000)
        cy.reportSkill(1, 13, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill(1, 13, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill(1, 13, 'user@skills.org', '2021-02-24 10:00', false);
        cy.wait(1000)

        cy.visit('/administrator/projects/proj1/issues')

        const tableSelector = '[data-cy="projectErrorsTable"]';

        // last seen is the default
        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`).should('have.text', '3')
        cy.validateTable(tableSelector, [
            [{colIndex: 0, value: '[skill13]'}],
            [{colIndex: 0, value: '[skill75]'}],
            [{colIndex: 0, value: '[skill42]'}],
        ]);
        cy.get(`${tableSelector} th`).contains('Last Seen').click();
        cy.wait('@getErrors');
        cy.validateTable(tableSelector, [
            [{colIndex: 0, value: '[skill42]'}],
            [{colIndex: 0, value: '[skill75]'}],
            [{colIndex: 0, value: '[skill13]'}],
        ]);

        // just make sure it doesn't fail when sorting by error
        cy.get(`${tableSelector} th`).contains('Error').click();
        cy.wait('@getErrors');

        cy.get(`${tableSelector} th`).contains('First Seen').click();
        cy.wait('@getErrors');
        cy.validateTable(tableSelector, [
            [{colIndex: 0, value: '[skill42]'}],
            [{colIndex: 0, value: '[skill75]'}],
            [{colIndex: 0, value: '[skill13]'}],
        ]);
        cy.get(`${tableSelector} th`).contains('First Seen').click();
        cy.wait('@getErrors');
        cy.validateTable(tableSelector, [
            [{colIndex: 0, value: '[skill13]'}],
            [{colIndex: 0, value: '[skill75]'}],
            [{colIndex: 0, value: '[skill42]'}],
        ]);

        cy.get(`${tableSelector} th`).contains('Times Seen').click();
        cy.wait('@getErrors');
        cy.validateTable(tableSelector, [
            [{colIndex: 0, value: '[skill42]'}, {colIndex: 3, value: '1'}],
            [{colIndex: 0, value: '[skill75]'}, {colIndex: 3, value: '2'}],
            [{colIndex: 0, value: '[skill13]'}, {colIndex: 3, value: '3'}],
        ]);
        cy.get(`${tableSelector} th`).contains('Times Seen').click();
        cy.wait('@getErrors');
        cy.validateTable(tableSelector, [
            [{colIndex: 0, value: '[skill13]'}, {colIndex: 3, value: '3'}],
            [{colIndex: 0, value: '[skill75]'}, {colIndex: 3, value: '2'}],
            [{colIndex: 0, value: '[skill42]'}, {colIndex: 3, value: '1'}],
        ]);
    });

    it('issues count on administrator home page is correct', () => {
        cy.intercept('GET', '/admin/projects')
            .as('getProjects');

        cy.visit('/administrator');
        cy.wait('@getProjects');
        cy.get('[data-cy="ProjectCardFooter_issues"]')
            .contains('No Issues');

        cy.reportSkill(1, 42, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill(1, 75, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill(1, 75, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill(1, 75, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill(1, 13, 'user@skills.org', '2021-02-24 10:00', false);

        cy.visit('/administrator');
        cy.wait('@getProjects');
        cy.get('[data-cy="ProjectCardFooter_issues"]')
            .contains('There are 3 issues to address');
    });

    it('issues text formats properly for singular issue count', () => {
        cy.intercept('GET', '/admin/projects')
            .as('getProjects');

        cy.visit('/administrator');
        cy.wait('@getProjects');
        cy.get('[data-cy="ProjectCardFooter_issues"]')
            .contains('No Issues');

        cy.reportSkill(1, 42, 'user@skills.org', '2021-02-24 10:00', false);

        cy.visit('/administrator');
        cy.wait('@getProjects');
        cy.get('[data-cy="ProjectCardFooter_issues"]')
            .contains('There is 1 issue to address');
    });

    it('validate table', () => {
        cy.reportSkill(1, 41, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill(1, 42, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill(1, 42, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill(1, 43, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill(1, 43, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill(1, 43, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill(1, 44, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill(1, 44, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill(1, 44, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill(1, 44, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill(1, 45, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill(1, 45, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill(1, 45, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill(1, 45, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill(1, 45, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill(1, 46, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill(1, 46, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill(1, 46, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill(1, 46, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill(1, 46, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill(1, 46, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill(1, 47, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill(1, 47, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill(1, 47, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill(1, 47, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill(1, 47, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill(1, 47, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill(1, 47, 'user@skills.org', '2021-02-24 10:00', false);

        cy.intercept('GET', '/admin/projects/proj1')
            .as('getProject');
        cy.intercept('GET', '/admin/projects/proj1/errors**')
            .as('getErrors');
        cy.visit('/administrator/projects/proj1/');
        cy.wait('@getProject');
        cy.get('[data-cy=nav-Issues]')
            .click();
        cy.wait('@getErrors');

        const tableSelector = '[data-cy=projectErrorsTable]';

        const expected = [
            [{
                colIndex: 0,
                value: 'SkillNotFoundReported Skill Id [skill47] does not exist in this Project'
            }, {
                colIndex: 3,
                value: '7'
            }],
            [{
                colIndex: 0,
                value: 'SkillNotFoundReported Skill Id [skill46] does not exist in this Project'
            }, {
                colIndex: 3,
                value: '6'
            }],
            [{
                colIndex: 0,
                value: 'SkillNotFoundReported Skill Id [skill45] does not exist in this Project'
            }, {
                colIndex: 3,
                value: '5'
            }],
            [{
                colIndex: 0,
                value: 'SkillNotFoundReported Skill Id [skill44] does not exist in this Project'
            }, {
                colIndex: 3,
                value: '4'
            }],
            [{
                colIndex: 0,
                value: 'SkillNotFoundReported Skill Id [skill43] does not exist in this Project'
            }, {
                colIndex: 3,
                value: '3'
            }],
            [{
                colIndex: 0,
                value: 'SkillNotFoundReported Skill Id [skill42] does not exist in this Project'
            }, {
                colIndex: 3,
                value: '2'
            }],
            [{
                colIndex: 0,
                value: 'SkillNotFoundReported Skill Id [skill41] does not exist in this Project'
            }, {
                colIndex: 3,
                value: '1'
            }],
        ];

        cy.validateTable(tableSelector, expected);

        cy.get(`${tableSelector} th`)
            .contains('First Seen')
            .click();
        cy.wait('@getErrors');
        const sortByFirstSeenExpected = [
            [{
                colIndex: 0,
                value: 'SkillNotFoundReported Skill Id [skill41] does not exist in this Project'
            }, {
                colIndex: 3,
                value: '1'
            }],
            [{
                colIndex: 0,
                value: 'SkillNotFoundReported Skill Id [skill42] does not exist in this Project'
            }, {
                colIndex: 3,
                value: '2'
            }],
            [{
                colIndex: 0,
                value: 'SkillNotFoundReported Skill Id [skill43] does not exist in this Project'
            }, {
                colIndex: 3,
                value: '3'
            }],
            [{
                colIndex: 0,
                value: 'SkillNotFoundReported Skill Id [skill44] does not exist in this Project'
            }, {
                colIndex: 3,
                value: '4'
            }],
            [{
                colIndex: 0,
                value: 'SkillNotFoundReported Skill Id [skill45] does not exist in this Project'
            }, {
                colIndex: 3,
                value: '5'
            }],
            [{
                colIndex: 0,
                value: 'SkillNotFoundReported Skill Id [skill46] does not exist in this Project'
            }, {
                colIndex: 3,
                value: '6'
            }],
            [{
                colIndex: 0,
                value: 'SkillNotFoundReported Skill Id [skill47] does not exist in this Project'
            }, {
                colIndex: 3,
                value: '7'
            }],
        ];
        cy.validateTable(tableSelector, sortByFirstSeenExpected);
        cy.get(`${tableSelector} th`)
            .contains('First Seen')
            .click();
        cy.wait('@getErrors');
        //should be back to original order
        cy.validateTable(tableSelector, expected);

        cy.get(`${tableSelector} th`)
            .contains('First Seen')
            .click();
        cy.wait('@getErrors');
        cy.validateTable(tableSelector, sortByFirstSeenExpected);

        // Ensure the sorting remains when navigating away/refreshing
        cy.visit('/administrator/projects/proj1/');
        cy.wait('@getProject');
        cy.get('[data-cy=nav-Issues]')
            .click();
        cy.wait('@getErrors');
        cy.validateTable(tableSelector, sortByFirstSeenExpected);

        cy.get(`${tableSelector} th`)
            .contains('First Seen')
            .click();
        cy.wait('@getErrors');
        cy.validateTable(tableSelector, expected);

        cy.visit('/administrator/projects/proj1/');
        cy.wait('@getProject');
        cy.get('[data-cy=nav-Issues]')
            .click();
        cy.wait('@getErrors');
        cy.validateTable(tableSelector, expected);
    });

});
