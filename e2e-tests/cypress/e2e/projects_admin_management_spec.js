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

    const tableSelector = '[data-cy=roleManagerTable]';

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
        cy.get('[data-pc-section="overlay"] [aria-label="Administrator"]').click();
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
        cy.get('[data-pc-section="overlay"] [aria-label="Administrator"]').click();
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

        cy.get('[data-cy="existingUserInputDropdown"] [data-pc-section="dropdown"]').click()
        cy.wait('@suggest');
        cy.wait(500);
        cy.get('#existingUserInput_0').contains('root@skills.org').click();
        cy.get('[data-cy="userRoleSelector"]').click()
        cy.get('[data-pc-section="overlay"] [aria-label="Administrator"]').click();
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
                .eq(1)
                .contains('root@skills.org');
            cy.get('@row2')
                .eq(1)
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
                .eq(1)
                .contains('foo bar');
            cy.get('@row2')
                .eq(1)
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
        cy.get('[data-pc-section="overlay"] [aria-label="Administrator"]').click();
        cy.get('[data-cy="addUserBtn"]').click();
        cy.wait('@addAdmin');

        const tableSelector = '[data-cy=roleManagerTable]';
        const rowSelector = `${tableSelector} tbody tr`;

        cy.get(`${tableSelector} [data-cy="userCell_root@skills.org"]`);
        cy.get(rowSelector)
            .should('have.length', 2)
            .as('cyRows');

        cy.openDialog(`${tableSelector} [data-cy="controlsCell_root@skills.org"] [data-cy="removeUserBtn"]`)
        cy.get('[data-cy="removalSafetyCheckMsg"]').contains('This will remove root@skills.org from having admin privileges.')
        cy.get('[data-cy="currentValidationText"]').fill('Delete Me')
        cy.clickSaveDialogBtn()

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
        cy.get('[data-pc-section="overlay"] [aria-label="Approver"]').click();
        cy.get('[data-cy="addUserBtn"]').click();
        cy.wait('@addApprover');
        const expectedUserName = Cypress.env('oauthMode') ? 'foo bar' : 'skills@';
        cy.get(`${tableSelector} thead th`).contains('Role').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: expectedUserName }, { colIndex: 2,  value: 'Administrator' }],
            [{ colIndex: 1,  value: 'root@' }, { colIndex: 2,  value: 'Approver' }],
        ], 5, true, null, false);

        // reload and retest
        cy.visit('/administrator/projects/proj1/access');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProject');
        // verify that table loaded
        cy.get(`${tableSelector} [data-cy="controlsCell_root@skills.org"] [data-cy="editUserBtn"]`)
        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: expectedUserName }, { colIndex: 2,  value: 'Administrator' }],
            [{ colIndex: 1,  value: 'root@' }, { colIndex: 2,  value: 'Approver' }],
        ], 5, true, null, false);

        cy.get(`${tableSelector} [data-cy="controlsCell_root@skills.org"] [data-cy="editUserBtn"]`).click();
        cy.get('[data-cy="roleDropDown_root@skills.org"]').click()
        cy.get('[data-pc-section="overlay"] [data-pc-section="option"]').contains('Administrator').click();
        cy.wait('@addAdmin')
        cy.get(`${tableSelector} thead th`).contains('User').click();

        const compare = (a, b) => {
            return a[0].value?.localeCompare(b[0].value)
        }
        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'root@' }, { colIndex: 2,  value: 'Administrator' }],
            [{ colIndex: 1,  value: expectedUserName }, { colIndex: 2,  value: 'Administrator' }],
        ].sort(compare), 5, true, null, false);
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
        cy.get('[data-pc-section="list"] [data-pc-section="option"]').should('have.length', 1)
        cy.get('[data-pc-section="list"]').contains('some display name')
            .click();
        cy.get('[data-cy="userRoleSelector"]').click()
        cy.get('[data-pc-section="overlay"] [aria-label="Approver"]').click();
        cy.get('[data-cy="addUserBtn"]').click();


        cy.wait('@addApprover');

        cy.get('[data-cy="userCell_newuser"]').should("exist");
        cy.get('[data-cy="existingUserInput"]').type('some');
        cy.wait('@suggest');
        cy.wait(1500);
        cy.get('[data-pc-section="list"] [data-pc-section="option"]').should('have.length', 0)
    });

    it('user does not have any admin groups to assign to project', () => {
        cy.createProject(1);
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');
        cy.intercept('GET', '/admin/projects/proj1')
            .as('loadProject');

        cy.visit('/administrator/projects/proj1/access');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProject');

        cy.get('[data-cy="adminGroupSelector"]').click()
        cy.get('[data-pc-section="overlay"] [data-pc-section="emptymessage"]').contains('You currently do not administer any admin groups.').should('be.visible')
    });

    it('assign admin group to project', () => {
        cy.intercept('POST', ' /admin/admin-group-definitions/adminGroup1/projects/proj1')
            .as('addAdminGroupToProject');
        cy.intercept('GET', '/app/admin-group-definitions')
            .as('loadCurrentUsersAdminGroups');
        cy.intercept('GET', '/admin/projects/proj1/adminGroups')
            .as('loadAdminGroupsForProject');


        cy.createProject(1);
        cy.createAdminGroupDef(1, { name: 'My Awesome Admin Group' });
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');
        cy.intercept('GET', '/admin/projects/proj1')
            .as('loadProject');

        cy.visit('/administrator/projects/proj1/access');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProject');
        cy.wait('@loadAdminGroupsForProject');
        cy.wait('@loadCurrentUsersAdminGroups');

        const expectedUserName = Cypress.env('oauthMode') ? 'foo' : 'skills@';
        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: expectedUserName }, { colIndex: 2,  value: 'Administrator' }],
        ], 5, true, null, false);

        cy.get('[data-cy="adminGroupSelector"]').click()
        cy.get('[data-cy="availableAdminGroupSelection-adminGroup1"]').click()

        cy.wait('@addAdminGroupToProject');
        cy.wait('@loadAdminGroupsForProject');
        cy.wait('@loadCurrentUsersAdminGroups');
        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'My Awesome Admin Group' }, { colIndex: 2,  value: 'Administrator' }],
        ], 5, true, null, false);

        cy.get(`${tableSelector} [data-p-index="0"] [data-pc-section="rowtogglebutton"]`).click()
        cy.get('[data-cy="userGroupMembers"]').find('li').should('have.length', 1);
        cy.get(`[data-cy^="userGroupMember_${expectedUserName}"]`)
    });

    it('verify approvers will become admins when assigning admin group to project where the approver also belongs to the admin group', () => {

        cy.intercept('POST', ' /admin/admin-group-definitions/adminGroup1/projects/proj1')
            .as('addAdminGroupToProject');
        cy.intercept('GET', '/app/admin-group-definitions')
            .as('loadCurrentUsersAdminGroups');
        cy.intercept('GET', '/admin/projects/proj1/adminGroups')
            .as('loadAdminGroupsForProject');

        const pass = 'password';
        cy.register('user1', pass);
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

        cy.createProject(1);
        cy.createAdminGroupDef(1, { name: 'My Awesome Admin Group' });

        cy.intercept('POST', '*suggestDashboardUsers*')
            .as('suggest');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');
        cy.intercept('GET', '/admin/projects/proj1')
            .as('loadProject');
        cy.intercept('PUT', '/admin/projects/proj1/users/user1/roles/ROLE_PROJECT_APPROVER')
            .as('addApprover');

        cy.visit('/administrator/projects/proj1/access');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProject');
        cy.wait('@loadAdminGroupsForProject');
        cy.wait('@loadCurrentUsersAdminGroups');

        cy.get('[data-cy="existingUserInput"]')
            .type('user1');
        cy.wait('@suggest');
        cy.wait(500);
        cy.get('#existingUserInput_0').contains('user1').click();
        cy.get('[data-cy="userRoleSelector"]').click()
        cy.get('[data-pc-section="overlay"] [aria-label="Approver"]').click();
        cy.get('[data-cy="addUserBtn"]').click();
        cy.wait('@addApprover');
        cy.wait('@loadAdminGroupsForProject');
        cy.wait('@loadCurrentUsersAdminGroups');

        const expectedUserName = Cypress.env('oauthMode') ? 'foo' : 'skills@';
        cy.get(`${tableSelector} thead th`).contains('Role').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: expectedUserName }, { colIndex: 2,  value: 'Administrator' }],
            [{ colIndex: 1,  value: 'user1' }, { colIndex: 2,  value: 'Approver' }],
        ], 5, true, null, false);

        cy.get('[data-cy="adminGroupSelector"]').click()
        cy.get('[data-cy="availableAdminGroupSelection-adminGroup1"]').click()

        cy.wait('@addAdminGroupToProject');
        cy.wait('@loadAdminGroupsForProject');
        cy.wait('@loadCurrentUsersAdminGroups');
        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'My Awesome Admin Group' }, { colIndex: 2,  value: 'Administrator' }],
            [{ colIndex: 1,  value: 'user1' }, { colIndex: 2,  value: 'Approver' }],
        ], 5, true, null, false);

        cy.get(`${tableSelector} [data-p-index="0"] [data-pc-section="rowtogglebutton"]`).click()
        cy.get('[data-cy="userGroupMembers"]').find('li').should('have.length', 1);
        cy.get(`[data-cy^="userGroupMember_${expectedUserName}"]`)


        cy.intercept('GET', '/admin/admin-group-definitions/adminGroup1/userRoles**')
            .as('loadUserRoles')
        cy.intercept('PUT', 'admin/admin-group-definitions/adminGroup1/users/user1/roles/ROLE_ADMIN_GROUP_MEMBER')
            .as('addAdminGroupMember');
        cy.visit('/administrator/adminGroups/adminGroup1');
        cy.wait('@loadUserRoles');

        cy.get('[data-cy="pageHeaderStat_Members"] [data-cy="statValue"]').should('have.text', '1');
        cy.get('[data-cy="existingUserInput"]').type('user1');
        cy.wait('@suggest');
        cy.wait(500);
        cy.get('[data-pc-section="option"]').contains('user1').click();
        cy.get('[data-cy="userRoleSelector"]').click()
        cy.get('[data-pc-section="overlay"] [aria-label="Group Member"]').click();
        cy.get('[data-cy="addUserBtn"]').click()
        cy.wait('@addAdminGroupMember');

        cy.visit('/administrator/projects/proj1/access');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProject');
        cy.wait('@loadAdminGroupsForProject');
        cy.wait('@loadCurrentUsersAdminGroups');

        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'My Awesome Admin Group' }, { colIndex: 2,  value: 'Administrator' }],
        ], 5, true, null, false);

        cy.get(`${tableSelector} [data-p-index="0"] [data-pc-section="rowtogglebutton"]`).click()
        cy.get('[data-cy="userGroupMembers"]').find('li').should('have.length', 2);
        cy.get(`[data-cy^="userGroupMember_${expectedUserName}"]`)
        cy.get('[data-cy="userGroupMember_user1"]')
    });

    it('Sort users alphabetically', () => {
        cy.register('newuser', 'password', false, 'some display name', 'Jeff', 'Jackson')
        cy.register('efgh', 'password', false, 'edward johnson', 'Edward', 'Johnson')
        cy.register('zed', 'password', false, 'zed jones', 'Zed', 'Jones')
        cy.register('abcd', 'password', false, 'alan davis', 'Alan', 'Davis')
        cy.fixture('vars.json').then((vars) => {
            cy.logout()
            cy.login(vars.defaultUser, vars.defaultPass);
        });

        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });

        cy.request('POST', `/admin/projects/proj1/users/newuser/roles/ROLE_PROJECT_APPROVER`);
        cy.request('POST', `/admin/projects/proj1/users/abcd/roles/ROLE_PROJECT_APPROVER`);
        cy.request('POST', `/admin/projects/proj1/users/zed/roles/ROLE_PROJECT_APPROVER`);
        cy.request('POST', `/admin/projects/proj1/users/efgh/roles/ROLE_PROJECT_APPROVER`);

        cy.intercept('GET', '/app/userInfo').as('loadUserInfo');
        cy.intercept('GET', '/admin/projects/proj1').as('loadProject');

        cy.visit('/administrator/projects/proj1/access');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProject');

        cy.get(`${tableSelector} thead th`).contains('User').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'Zed Jones (zed jones)' }, { colIndex: 2,  value: 'Approver' }],
            [{ colIndex: 1,  value: 'Jeff Jackson (some display name)' }, { colIndex: 2,  value: 'Approver' }],
            [{ colIndex: 1,  value: 'Firstname LastName (skills@skills.org)' }, { colIndex: 2,  value: 'Administrator' }],
            [{ colIndex: 1,  value: 'Edward Johnson (edward johnson)' }, { colIndex: 2,  value: 'Approver' }],
            [{ colIndex: 1,  value: 'Alan Davis (alan davis)' }, { colIndex: 2,  value: 'Approver' }],
        ], 5, true, null, false);

        cy.get(`${tableSelector} thead th`).contains('User').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'Alan Davis (alan davis)' }, { colIndex: 2,  value: 'Approver' }],
            [{ colIndex: 1,  value: 'Edward Johnson (edward johnson)' }, { colIndex: 2,  value: 'Approver' }],
            [{ colIndex: 1,  value: 'Firstname LastName (skills@skills.org)' }, { colIndex: 2,  value: 'Administrator' }],
            [{ colIndex: 1,  value: 'Jeff Jackson (some display name)' }, { colIndex: 2,  value: 'Approver' }],
            [{ colIndex: 1,  value: 'Zed Jones (zed jones)' }, { colIndex: 2,  value: 'Approver' }],
        ], 5, true, null, false);
    });
})
