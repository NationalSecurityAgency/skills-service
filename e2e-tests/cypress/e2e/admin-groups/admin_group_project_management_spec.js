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
import utcPlugin from 'dayjs/plugin/utc';

dayjs.extend(utcPlugin);

describe('Admin Group Project Management Tests', () => {

    const adminGroupProjectsTableSelector = '[data-cy="adminGroupProjectsTable"]';
    
    beforeEach( () => {

        cy.intercept('GET', '/admin/admin-group-definitions/adminGroup1/projects')
            .as('loadGroupProjects');
        cy.intercept('POST', '*suggestDashboardUsers*').as('suggest');
    })

    it('user has no projects to assign to admin group', function () {

        cy.createAdminGroupDef(1, { name: 'My Awesome Admin Group' });

        cy.visit('/administrator/adminGroups/adminGroup1/group-projects');
        cy.wait('@loadGroupProjects');

        cy.get('[data-cy="pageHeaderStat_Projects"] [data-cy="statValue"]').should('have.text', '0');
        cy.get('[data-cy="noContent"]')
        cy.get('[data-cy="projectSelector"]').click()
        cy.get('[data-pc-section="overlay"] [data-pc-section="emptymessage"]').contains('You currently do not administer any projects.').should('be.visible')
    });

    it('admin groups project page, add project to group', function () {
        cy.createProject(1);
        cy.createAdminGroupDef(1, { name: 'My Awesome Admin Group' });

        cy.visit('/administrator/adminGroups/adminGroup1/group-projects');
        cy.wait('@loadGroupProjects');
        cy.get('[data-cy="pageHeaderStat_Projects"] [data-cy="statValue"]').should('have.text', '0');
        cy.get('[data-cy="noContent"]')
        cy.get('[data-cy="projectSelector"] [data-pc-section="label"]').contains('Search available projects...').should('be.visible')
        cy.get('[data-cy="projectSelector"]').click()
        cy.get('[data-cy="availableProjectSelection-proj1"]').click()

        cy.get('[data-cy="pageHeaderStat_Projects"] [data-cy="statValue"]').should('have.text', '1');
        cy.validateTable(adminGroupProjectsTableSelector, [
            [{
                colIndex: 0,
                value: 'This is project 1'
            }],
        ], 5);
        cy.get('[data-cy="projectSelector"]').click()
        cy.get('[data-pc-section="overlay"] [data-pc-section="emptymessage"]').contains('All of your available projects have already been assigned to this admin group.').should('be.visible')
    });

    it('admin groups project page, remove project from group', function () {
        const userIdForDisplay = 'user id for display'
        cy.intercept('GET', '/app/userInfo', (req) => {
            req.continue((res) => {
                res.body.userIdForDisplay = userIdForDisplay
            })
        }).as('getUserInfo1');
        cy.fixture('vars.json')
            .then((vars) => {
                const oauthMode = Cypress.env('oauthMode');
                const defaultUser = oauthMode ? Cypress.env('proxyUser') : vars.defaultUser;
                cy.createProject(1);
                cy.createProject(2);
                cy.createAdminGroupDef(1, {name: 'My Awesome Admin Group'});
                cy.addProjectToAdminGroupDef(1, 1)
                cy.addProjectToAdminGroupDef(1, 2)

                cy.visit('/administrator/adminGroups/adminGroup1/group-projects');
                cy.wait('@loadGroupProjects');
                cy.get('[data-cy="noContent"]').should('not.exist')

                cy.get('[data-cy="pageHeaderStat_Projects"] [data-cy="statValue"]').should('have.text', '2');
                cy.get(adminGroupProjectsTableSelector)
                cy.validateTable(adminGroupProjectsTableSelector, [
                    [{colIndex: 0, value: 'This is project 1'}],
                    [{colIndex: 0, value: 'This is project 2'}],
                ], 5);
                cy.openDialog('[data-cy="removeProject_proj2"]')
                cy.get('[data-cy="removalSafetyCheckMsg"]').contains(`This will remove the This is project 2 project from this admin group. All members of this admin group other than ${userIdForDisplay} will lose admin access to this project.`)
                cy.get('[data-cy="currentValidationText"]').type('Delete Me')
                cy.clickSaveDialogBtn()

                cy.get('[data-cy="pageHeaderStat_Projects"] [data-cy="statValue"]').should('have.text', '1');
                cy.validateTable(adminGroupProjectsTableSelector, [
                    [{colIndex: 0, value: 'This is project 1'}]
                ], 5);

                cy.get('[data-cy="removeProject_proj1"]').click()
                cy.get('[data-cy="removalSafetyCheckMsg"]').contains(`This will remove the This is project 1 project from this admin group. All members of this admin group other than ${userIdForDisplay} will lose admin access to this project.`)
                cy.get('[data-cy="closeDialogBtn"]').click()
                cy.get('[data-cy="removeProject_proj1"]').should('have.focus')
            });
    });

    it('paging projects', function () {
        cy.createProject(1);
        cy.createProject(2);
        cy.createProject(3);
        cy.createProject(4);
        cy.createProject(5);
        cy.createProject(6);
        cy.createAdminGroupDef(1, { name: 'My Awesome Admin Group' });
        cy.addProjectToAdminGroupDef(1, 1)
        cy.addProjectToAdminGroupDef(1, 2)
        cy.addProjectToAdminGroupDef(1, 3)
        cy.addProjectToAdminGroupDef(1, 4)
        cy.addProjectToAdminGroupDef(1, 5)
        cy.addProjectToAdminGroupDef(1, 6)

        cy.visit('/administrator/adminGroups/adminGroup1/group-projects');
        cy.wait('@loadGroupProjects');
        cy.get('[data-cy="noContent"]').should('not.exist')

        cy.get('[data-cy="pageHeaderStat_Projects"] [data-cy="statValue"]').should('have.text', '6');

        cy.get(adminGroupProjectsTableSelector)
        cy.validateTable(adminGroupProjectsTableSelector, [
            [{ colIndex: 0, value: 'This is project 1' }],
            [{ colIndex: 0, value: 'This is project 2' }],
            [{ colIndex: 0, value: 'This is project 3' }],
            [{ colIndex: 0, value: 'This is project 4' }],
            [{ colIndex: 0, value: 'This is project 5' }],
            [{ colIndex: 0, value: 'This is project 6' }],
        ])
    })

});
