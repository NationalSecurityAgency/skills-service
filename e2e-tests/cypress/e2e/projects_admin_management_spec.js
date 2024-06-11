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

describe('Projects Admin Management Tests', () => {
    beforeEach(() => {
        cy.intercept('GET', '/app/projects')
            .as('getProjects');
        cy.intercept('GET', '/api/icons/customIconCss')
            .as('getProjectsCustomIcons');
        cy.intercept('GET', '/app/userInfo')
            .as('getUserInfo');
        cy.intercept('/admin/projects/proj1/users/root@skills.org/roles*')
            .as('getRolesForRoot');
    });

    it('Add Admin - User Not Found', () => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });

        cy.intercept({
            method: 'PUT',
            path: '/admin/projects/proj1/users/bar/roles/ROLE_PROJECT_ADMIN',
        }, {
            statusCode: 400,
            body: {
                errorCode: 'UserNotFound',
                explanation: 'User was not found'
            }
        })
            .as('addAdmin');

        cy.intercept({
            method: 'POST',
            path: '/app/users/suggest*',
        }, {
            statusCode: 200,
            body: [{
                userId: 'bar',
                userIdForDisplay: 'bar',
                first: 'bar',
                last: 'bar',
                dn: 'bar'
            }]
        })
            .as('suggest');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');
        cy.intercept('GET', '/admin/projects/proj1')
            .as('loadProject');

        cy.visit('/administrator/projects/proj1/access');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProject');

        cy.get('[data-cy="existingUserInput"]').type('bar');
        cy.wait('@suggest');
        cy.wait(500);
        cy.get('#existingUserInput_0').contains('bar').click();
        cy.get('[data-cy="userRoleSelector"]').click()
        cy.get('[data-pc-section="panel"] [aria-label="Administrator"]').click();
        cy.get('[data-cy="addUserBtn"]').click();

        cy.wait('@addAdmin');
        cy.get('[data-cy="error-msg"]')
            .contains('User was not found');
    });

    it('Add Admin - InternalError', () => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });

        cy.intercept({
            method: 'PUT',
            path: '/admin/projects/proj1/users/bar/roles/ROLE_PROJECT_ADMIN',
        }, {
            statusCode: 400,
            body: {
                errorCode: 'InternalError',
                explanation: 'Some Error Occurred'
            }
        })
            .as('addAdmin');

        cy.intercept({
            method: 'POST',
            path: '/app/users/suggest*',
        }, {
            statusCode: 200,
            body: [{
                userId: 'bar',
                userIdForDisplay: 'bar',
                first: 'bar',
                last: 'bar',
                dn: 'bar'
            }]
        })
            .as('suggest');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');
        cy.intercept('GET', '/admin/projects/proj1')
            .as('loadProject');

        cy.visit('/administrator/projects/proj1/access');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProject');

        cy.get('[data-cy="existingUserInput"]').type('bar');
        cy.wait('@suggest');
        cy.wait(500);
        cy.get('#existingUserInput_0').contains('bar').click();
        cy.get('[data-cy="userRoleSelector"]').click()
        cy.get('[data-pc-section="panel"] [aria-label="Administrator"]').click();
        cy.get('[data-cy="addUserBtn"]').click();
        cy.wait('@addAdmin');
        cy.get('[data-cy="errorPage"]')
            .contains('Failed to add User Role');
    });

    it('Add Admin No Query', () => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });

        cy.intercept('PUT', '/admin/projects/proj1/users/root@skills.org/roles/ROLE_PROJECT_ADMIN')
            .as('addAdmin');

        cy.intercept('POST', '*suggestDashboardUsers*')
            .as('suggest');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');
        cy.intercept('GET', '/admin/projects/proj1')
            .as('loadProject');

        cy.visit('/administrator/projects/proj1/access');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProject');

        cy.get('[data-cy="existingUserInputDropdown"] [data-pc-name="dropdownbutton"]').click()
        cy.wait('@suggest');
        cy.wait(500);
        cy.get('#existingUserInput_0').contains('root@skills.org').click();
        cy.get('[data-cy="userRoleSelector"]').click()
        cy.get('[data-pc-section="panel"] [aria-label="Administrator"]').click();
        cy.get('[data-cy="addUserBtn"]').click();
        cy.wait('@addAdmin');

        const rowSelector = '[data-cy=roleManagerTable] tbody tr';
        cy.get(rowSelector)
            .should('have.length', 2)
            .as('cyRows');
        if (!Cypress.env('oauthMode')) {
            cy.get('@cyRows')
                .eq(0)
                .find('td')
                .as('row1');
            cy.get('@cyRows')
                .eq(1)
                .find('td')
                .as('row2');
            cy.get('@row1')
                .eq(0)
                .contains('root@skills.org');
            cy.get('@row2')
                .eq(0)
                .contains('skills@skills.org');
        } else {
            // the default user in oauth mode is different and results in a different sorting order
            cy.get('@cyRows')
                .eq(0)
                .find('td')
                .as('row1');
            cy.get('@cyRows')
                .eq(1)
                .find('td')
                .as('row2');
            cy.get('@row1')
                .eq(0)
                .contains('foo bar');
            cy.get('@row2')
                .eq(0)
                .contains('root@skills.org');
        }
    });

    it('Add and Remove Admin', () => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });

        cy.intercept('PUT', '/admin/projects/proj1/users/root@skills.org/roles/ROLE_PROJECT_ADMIN')
            .as('addAdmin');

        cy.intercept('POST', '*suggestDashboardUsers*')
            .as('suggest');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');
        cy.intercept('GET', '/admin/projects/proj1')
            .as('loadProject');

        cy.visit('/administrator/projects/proj1/access');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProject');

        cy.get('[data-cy="existingUserInput"]')
            .type('root');
        cy.wait('@suggest');
        cy.wait(500);
        cy.get('#existingUserInput_0').contains('root').click();
        cy.get('[data-cy="userRoleSelector"]').click()
        cy.get('[data-pc-section="panel"] [aria-label="Administrator"]').click();
        cy.get('[data-cy="addUserBtn"]').click();
        cy.wait('@addAdmin');

        const tableSelector = '[data-cy=roleManagerTable]';
        const rowSelector = `${tableSelector} tbody tr`;

        cy.get(`${tableSelector} [data-cy="userCell_root@skills.org"]`);
        cy.get(rowSelector)
            .should('have.length', 2)
            .as('cyRows');

        cy.get(`${tableSelector} [data-cy="controlsCell_root@skills.org"] [data-cy="removeUserBtn"]`)
            .click();
        cy.contains('YES, Delete It')
            .click();

        cy.get(`${tableSelector} [data-cy="controlsCell_root@skills.org"]`).should('not.exist')
        cy.get(rowSelector)
            .should('have.length', 1)
            .as('cyRows1');
    });

    it('Add Admin - forward slash character does not cause error', () => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });

        cy.intercept('PUT', '/admin/projects/proj1/users/root@skills.org/roles/ROLE_PROJECT_ADMIN')
            .as('addAdmin');

        cy.intercept('POST', '*suggestDashboardUsers*')
            .as('suggest');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');
        cy.intercept('GET', '/admin/projects/proj1')
            .as('loadProject');

        cy.visit('/administrator/projects/proj1/access');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProject');

        cy.get('[data-cy="existingUserInput"]')
            .click()
            .type('root/bar{enter}');
        cy.wait('@suggest');
    });

    it('Add Approver role then upgrade to Admin', () => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });

        cy.intercept('PUT', '/admin/projects/proj1/users/root@skills.org/roles/ROLE_PROJECT_APPROVER')
            .as('addApprover');
        cy.intercept('PUT', '/admin/projects/proj1/users/root@skills.org/roles/ROLE_PROJECT_ADMIN')
            .as('addAdmin');

        cy.intercept('POST', '*suggestDashboardUsers*')
            .as('suggest');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');
        cy.intercept('GET', '/admin/projects/proj1')
            .as('loadProject');

        cy.visit('/administrator/projects/proj1/access');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProject');

        cy.get('[data-cy="existingUserInput"]')
            .type('root');
        cy.wait('@suggest');
        cy.wait(500);
        cy.get('#existingUserInput_0').contains('root').click();
        cy.get('[data-cy="userRoleSelector"]').click()
        cy.get('[data-pc-section="panel"] [aria-label="Approver"]').click();
        cy.get('[data-cy="addUserBtn"]').click();
        cy.wait('@addApprover');

        const tableSelector = '[data-cy=roleManagerTable]';
        const expectedUserName = Cypress.env('oauthMode') ? 'foo bar' : 'skills@';
        cy.get(`${tableSelector} thead th`).contains('Role').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: expectedUserName }, { colIndex: 1,  value: 'Administrator' }],
            [{ colIndex: 0,  value: 'root@' }, { colIndex: 1,  value: 'Approver' }],
        ], 5, true, null, false);

        // reload and retest
        cy.visit('/administrator/projects/proj1/access');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProject');
        // verify that table loaded
        cy.get(`${tableSelector} [data-cy="controlsCell_root@skills.org"] [data-cy="editUserBtn"]`)
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: expectedUserName }, { colIndex: 1,  value: 'Administrator' }],
            [{ colIndex: 0,  value: 'root@' }, { colIndex: 1,  value: 'Approver' }],
        ], 5, true, null, false);

        cy.get(`${tableSelector} [data-cy="controlsCell_root@skills.org"] [data-cy="editUserBtn"]`).click();
        cy.get('[data-cy="roleDropDown_root@skills.org"]').click()
        cy.get('[data-pc-section="panel"] [data-pc-section="itemlabel"]').contains('Administrator').click();
        cy.wait('@addAdmin')
        cy.get(`${tableSelector} thead th`).contains('User').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'root@' }, { colIndex: 1,  value: 'Administrator' }],
            [{ colIndex: 0,  value: expectedUserName }, { colIndex: 1,  value: 'Administrator' }],
        ], 5, true, null, false);
    });

    it('Existing users are not suggested', () => {
        cy.register('newuser', 'password', false, 'some display name')
        cy.fixture('vars.json').then((vars) => {
            cy.logout()
            cy.login(vars.defaultUser, vars.defaultPass);
        });

        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });

        cy.intercept('PUT', '/admin/projects/proj1/users/newuser/roles/ROLE_PROJECT_APPROVER')
            .as('addApprover');

        cy.intercept('POST', '*suggestDashboardUsers*')
            .as('suggest');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');
        cy.intercept('GET', '/admin/projects/proj1')
            .as('loadProject');

        cy.visit('/administrator/projects/proj1/access');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProject');


        cy.get('[data-cy="existingUserInput"]').type('some');
        cy.wait('@suggest');
        cy.wait(500);
        cy.get('[data-pc-section="list"] [data-pc-section="item"]').should('have.length', 1)
        cy.get('[data-pc-section="list"]').contains('some display name')
            .click();
        cy.get('[data-cy="userRoleSelector"]').click()
        cy.get('[data-pc-section="panel"] [aria-label="Approver"]').click();
        cy.get('[data-cy="addUserBtn"]').click();


        cy.wait('@addApprover');

        cy.get('[data-cy="userCell_newuser"]').should("exist");
        cy.get('[data-cy="existingUserInput"]').type('some');
        cy.wait('@suggest');
        cy.wait(1500);
        cy.get('[data-pc-section="list"] [data-pc-section="item"]').should('have.length', 0)
    });

})
