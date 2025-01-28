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

describe('Admin Group Member Management Tests', () => {

    const tableSelector = '[data-cy="roleManagerTable"]';
    
    beforeEach( () => {

        cy.intercept('GET', '/admin/admin-group-definitions/adminGroup1/userRoles**')
            .as('loadUserRoles');
        cy.intercept('POST', '*suggestDashboardUsers*').as('suggest');
    })

    it('add group owner', function () {

        cy.createAdminGroupDef(1, { name: 'My Awesome Admin Group' });

        cy.visit('/administrator/adminGroups/adminGroup1');
        cy.wait('@loadUserRoles');

        cy.get('[data-cy="pageHeaderStat_Members"] [data-cy="statValue"]').should('have.text', '1');
        cy.get('[data-cy="existingUserInput"]').type('root');
        cy.wait('@suggest');
        cy.wait(500);
        cy.get('[data-pc-section="option"]').contains('root@skills.org').click();
        cy.get('[data-cy="userRoleSelector"]').click()
        cy.get('[data-pc-section="overlay"] [aria-label="Group Owner"]').click();
        cy.get('[data-cy="userCell_root@skills.org"]').should('not.exist')
        cy.get('[data-cy="addUserBtn"]').click()
        cy.get('[data-cy="userCell_root@skills.org"]')
        cy.get('[data-cy="pageHeaderStat_Members"] [data-cy="statValue"]').should('have.text', '2');

        cy.get('[data-cy="existingUserInput"]').type('root');
        cy.wait('@suggest');
        cy.wait(500);
        cy.contains('No results found')
    });

    it('delete group owner', function () {
      cy.fixture('vars.json')
        .then((vars) => {
          const pass = 'password';
          cy.register('user1', pass);
          cy.register('user2', pass);
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
          cy.createAdminGroupDef(1, { name: 'My Awesome Admin Group' });

          const oauthMode = Cypress.env('oauthMode');
          const defaultUser = oauthMode ? Cypress.env('proxyUser') : vars.defaultUser;

          cy.request('PUT', `/admin/admin-group-definitions/adminGroup1/users/user1/roles/ROLE_ADMIN_GROUP_OWNER`);
          cy.request('PUT', `/admin/admin-group-definitions/adminGroup1/users/user2/roles/ROLE_ADMIN_GROUP_OWNER`);

          cy.visit('/administrator/adminGroups/adminGroup1');
          cy.wait('@loadUserRoles');

          cy.get('[data-cy="pageHeaderStat_Members"] [data-cy="statValue"]').should('have.text', '3');
          cy.get(`${tableSelector} [data-cy="controlsCell_${defaultUser}"] [data-cy="removeUserBtn"]`).should('not.be.enabled')
          cy.get('[data-cy="controlsCell_user1"] [data-cy="removeUserBtn"]').should('be.enabled')
          cy.get('[data-cy="controlsCell_user2"] [data-cy="removeUserBtn"]').should('be.enabled')

          cy.get('[data-cy="controlsCell_user1"] [data-cy="removeUserBtn"]').click()

          cy.get('[data-cy="removalSafetyCheckMsg"]').contains('This will remove user1 from having admin privileges.')
          cy.get('[data-cy="currentValidationText"]').fill('Delete Me')
          cy.get('[data-cy="saveDialogBtn"]').click()


          cy.get('[data-cy="pageHeaderStat_Members"] [data-cy="statValue"]').should('have.text', '2');
          cy.get(`[data-cy="controlsCell_${defaultUser}"] [data-cy="removeUserBtn"]`).should('not.be.enabled')
          cy.get('[data-cy="controlsCell_user1"] [data-cy="removeUserBtn"]').should('not.exist')
          cy.get('[data-cy="controlsCell_user2"] [data-cy="removeUserBtn"]').should('be.enabled')

          cy.get('[data-cy="existingUserInput"] [data-pc-section="input"]').should('have.focus')
        });
    })

    it('add group member', function () {

        cy.createAdminGroupDef(1, { name: 'My Awesome Admin Group' });

        cy.visit('/administrator/adminGroups/adminGroup1');
        cy.wait('@loadUserRoles');

        cy.get('[data-cy="pageHeaderStat_Members"] [data-cy="statValue"]').should('have.text', '1');
        cy.get('[data-cy="existingUserInput"]').type('root');
        cy.wait('@suggest');
        cy.wait(500);
        cy.get('[data-pc-section="option"]').contains('root@skills.org').click();
        cy.get('[data-cy="userRoleSelector"]').click()
        cy.get('[data-pc-section="overlay"] [aria-label="Group Member"]').click();
        cy.get('[data-cy="userCell_root@skills.org"]').should('not.exist')
        cy.get('[data-cy="addUserBtn"]').click()
        cy.get('[data-cy="userCell_root@skills.org"]')
        cy.get('[data-cy="pageHeaderStat_Members"] [data-cy="statValue"]').should('have.text', '2');

        cy.get('[data-cy="existingUserInput"]').type('root');
        cy.wait('@suggest');
        cy.wait(500);
        cy.contains('No results found')
    });

    it('add group member then update them to group owner', function () {
        cy.intercept('PUT', '/admin/admin-group-definitions/adminGroup1/users/root@skills.org/roles/ROLE_ADMIN_GROUP_OWNER')
            .as('addOwner');

        cy.createAdminGroupDef(1, { name: 'My Awesome Admin Group' });
        cy.createProject();
        cy.addProjectToAdminGroupDef()
        cy.createQuizDef()
        cy.addQuizToAdminGroupDef()

        cy.visit('/administrator/adminGroups/adminGroup1');
        cy.wait('@loadUserRoles');

        cy.get('[data-cy="pageHeaderStat_Members"] [data-cy="statValue"]').should('have.text', '1');
        cy.get('[data-cy="existingUserInput"]').type('root');
        cy.wait('@suggest');
        cy.wait(500);
        cy.get('[data-pc-section="option"]').contains('root@skills.org').click();
        cy.get('[data-cy="userRoleSelector"]').click()
        cy.get('[data-pc-section="overlay"] [aria-label="Group Member"]').click();
        cy.get('[data-cy="userCell_root@skills.org"]').should('not.exist')
        cy.get('[data-cy="addUserBtn"]').click()
        cy.get('[data-cy="userCell_root@skills.org"]')
        cy.get('[data-cy="pageHeaderStat_Members"] [data-cy="statValue"]').should('have.text', '2');

        cy.get('[data-cy="existingUserInput"]').type('root');
        cy.wait('@suggest');
        cy.wait(500);
        cy.contains('No results found')

        const expectedUserName = Cypress.env('oauthMode') ? 'foo bar' : 'skills@';
        cy.get(`${tableSelector} thead th`).contains('Role').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'root@' }, { colIndex: 1,  value: 'Member' }],
            [{ colIndex: 0,  value: expectedUserName }, { colIndex: 1,  value: 'Owner' }],
        ], 5, true, null, false);

        cy.get(`${tableSelector} [data-cy="controlsCell_root@skills.org"] [data-cy="editUserBtn"]`).click();
        cy.get('[data-cy="roleDropDown_root@skills.org"]').click()
        cy.get('[data-pc-section="overlay"] [data-pc-section="itemlabel"]').contains('Owner').click();
        cy.wait('@addOwner')
        if (Cypress.env('oauthMode')) {
            cy.validateTable(tableSelector, [
                [{ colIndex: 0,  value: expectedUserName }, { colIndex: 1,  value: 'Owner' }],
                [{ colIndex: 0,  value: 'root@' }, { colIndex: 1,  value: 'Owner' }],
            ], 5, true, null, false);
        } else {
            cy.validateTable(tableSelector, [
                [{ colIndex: 0,  value: 'root@' }, { colIndex: 1,  value: 'Owner' }],
                [{ colIndex: 0,  value: expectedUserName }, { colIndex: 1,  value: 'Owner' }],
            ], 5, true, null, false);
        }
    });


    it('add group owner then update them to group member', function () {
        cy.intercept('PUT', '/admin/admin-group-definitions/adminGroup1/users/root@skills.org/roles/ROLE_ADMIN_GROUP_MEMBER')
            .as('addMember');

        cy.createAdminGroupDef(1, { name: 'My Awesome Admin Group' });
        cy.createProject();
        cy.addProjectToAdminGroupDef()
        cy.createQuizDef()
        cy.addQuizToAdminGroupDef()

        cy.visit('/administrator/adminGroups/adminGroup1');
        cy.wait('@loadUserRoles');

        cy.get('[data-cy="pageHeaderStat_Members"] [data-cy="statValue"]').should('have.text', '1');
        cy.get('[data-cy="existingUserInput"]').type('root');
        cy.wait('@suggest');
        cy.wait(500);
        cy.get('[data-pc-section="option"]').contains('root@skills.org').click();
        cy.get('[data-cy="userRoleSelector"]').click()
        cy.get('[data-pc-section="overlay"] [aria-label="Group Owner"]').click();
        cy.get('[data-cy="userCell_root@skills.org"]').should('not.exist')
        cy.get('[data-cy="addUserBtn"]').click()
        cy.get('[data-cy="userCell_root@skills.org"]')
        cy.get('[data-cy="pageHeaderStat_Members"] [data-cy="statValue"]').should('have.text', '2');

        cy.get('[data-cy="existingUserInput"]').type('root');
        cy.wait('@suggest');
        cy.wait(500);
        cy.contains('No results found')

        const expectedUserName = Cypress.env('oauthMode') ? 'foo bar' : 'skills@';
        cy.get(`${tableSelector} thead th`).contains('Role').click();
        if (Cypress.env('oauthMode')) {
            cy.validateTable(tableSelector, [
                [{ colIndex: 0,  value: expectedUserName }, { colIndex: 1,  value: 'Owner' }],
                [{ colIndex: 0,  value: 'root@' }, { colIndex: 1,  value: 'Owner' }],
            ], 5, true, null, false);
        } else {
            cy.validateTable(tableSelector, [
                [{ colIndex: 0,  value: 'root@' }, { colIndex: 1,  value: 'Owner' }],
                [{ colIndex: 0,  value: expectedUserName }, { colIndex: 1,  value: 'Owner' }],
            ], 5, true, null, false);
        }

        cy.get(`${tableSelector} [data-cy="controlsCell_root@skills.org"] [data-cy="editUserBtn"]`).click();
        cy.get('[data-cy="roleDropDown_root@skills.org"]').click()
        cy.get('[data-pc-section="overlay"] [data-pc-section="itemlabel"]').contains('Member').click();
        cy.wait('@addMember')
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'root@' }, { colIndex: 1,  value: 'Member' }],
            [{ colIndex: 0,  value: expectedUserName }, { colIndex: 1,  value: 'Owner' }],
        ], 5, true, null, false);
    });

    it('delete group member', function () {
        cy.fixture('vars.json')
            .then((vars) => {
                const pass = 'password';
                cy.register('user1', pass);
                cy.register('user2', pass);
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
                cy.createAdminGroupDef(1, { name: 'My Awesome Admin Group' });

                const oauthMode = Cypress.env('oauthMode');
                const defaultUser = oauthMode ? Cypress.env('proxyUser') : vars.defaultUser;

                cy.request('PUT', `/admin/admin-group-definitions/adminGroup1/users/user1/roles/ROLE_ADMIN_GROUP_MEMBER`);
                cy.request('PUT', `/admin/admin-group-definitions/adminGroup1/users/user2/roles/ROLE_ADMIN_GROUP_MEMBER`);

                cy.visit('/administrator/adminGroups/adminGroup1');
                cy.wait('@loadUserRoles');

                cy.get('[data-cy="pageHeaderStat_Members"] [data-cy="statValue"]').should('have.text', '3');
                cy.get(`${tableSelector} [data-cy="controlsCell_${defaultUser}"] [data-cy="removeUserBtn"]`).should('not.be.enabled')
                cy.get('[data-cy="controlsCell_user1"] [data-cy="removeUserBtn"]').should('be.enabled')
                cy.get('[data-cy="controlsCell_user2"] [data-cy="removeUserBtn"]').should('be.enabled')

                cy.get('[data-cy="controlsCell_user1"] [data-cy="removeUserBtn"]').click()

                cy.get('[data-cy="removalSafetyCheckMsg"]').contains('This will remove user1 from having admin privileges.')
                cy.get('[data-cy="currentValidationText"]').fill('Delete Me')
                cy.get('[data-cy="saveDialogBtn"]').click()

                cy.get('[data-cy="pageHeaderStat_Members"] [data-cy="statValue"]').should('have.text', '2');
                cy.get(`[data-cy="controlsCell_${defaultUser}"] [data-cy="removeUserBtn"]`).should('not.be.enabled')
                cy.get('[data-cy="controlsCell_user1"] [data-cy="removeUserBtn"]').should('not.exist')
                cy.get('[data-cy="controlsCell_user2"] [data-cy="removeUserBtn"]').should('be.enabled')

                cy.get('[data-cy="existingUserInput"] [data-pc-section="input"]').should('have.focus')
            });
    })

    it('cancelling delete safety check returns focus to the delete button', function () {
        const pass = 'password';
        cy.register('user1', pass);
        cy.register('user2', pass);
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
        cy.createAdminGroupDef(1, { name: 'My Awesome Admin Group' });

        cy.request('PUT', `/admin/admin-group-definitions/adminGroup1/users/user1/roles/ROLE_ADMIN_GROUP_OWNER`);
        cy.request('PUT', `/admin/admin-group-definitions/adminGroup1/users/user2/roles/ROLE_ADMIN_GROUP_OWNER`);

        cy.visit('/administrator/adminGroups/adminGroup1');
        cy.wait('@loadUserRoles');

        cy.get('[data-cy="controlsCell_user1"] [data-cy="removeUserBtn"]').click()
        cy.get('[data-cy="closeDialogBtn"]').click()
        cy.get('[data-cy="controlsCell_user1"] [data-cy="removeUserBtn"]').should('have.focus')

        cy.get('[data-cy="controlsCell_user1"] [data-cy="removeUserBtn"]').click()
        cy.get('.p-dialog-header [aria-label="Close"]').click()
        cy.get('[data-cy="controlsCell_user1"] [data-cy="removeUserBtn"]').should('have.focus')
    })

    it('paging users', function () {
        cy.fixture('vars.json')
            .then((vars) => {
                const pass = 'password';
                cy.register('0user', pass);
                cy.register('1user', pass);
                cy.register('2user', pass);
                cy.register('3user', pass);
                cy.register('4user', pass);
                cy.register('5user', pass);

                const oauthMode = Cypress.env('oauthMode');
                const defaultUserForDisplay = oauthMode ? 'foo' : vars.defaultUser;
                if (!oauthMode) {
                    cy.log('NOT in oauthMode, using form login');
                    cy.login(vars.defaultUser, vars.defaultPass);
                } else {
                    cy.log('oauthMode, using loginBySingleSignOn');
                    cy.loginBySingleSignOn();
                }

                cy.createAdminGroupDef(1, { name: 'My Awesome Admin Group' });

                cy.request('POST', `/admin/admin-group-definitions/adminGroup1/users/0user/roles/ROLE_ADMIN_GROUP_OWNER`);
                cy.request('POST', `/admin/admin-group-definitions/adminGroup1/users/1user/roles/ROLE_ADMIN_GROUP_OWNER`);
                cy.request('POST', `/admin/admin-group-definitions/adminGroup1/users/2user/roles/ROLE_ADMIN_GROUP_OWNER`);
                cy.request('POST', `/admin/admin-group-definitions/adminGroup1/users/3user/roles/ROLE_ADMIN_GROUP_OWNER`);
                cy.request('POST', `/admin/admin-group-definitions/adminGroup1/users/4user/roles/ROLE_ADMIN_GROUP_OWNER`);
                cy.request('POST', `/admin/admin-group-definitions/adminGroup1/users/5user/roles/ROLE_ADMIN_GROUP_OWNER`);

                cy.visit('/administrator/adminGroups/adminGroup1');
                cy.wait('@loadUserRoles');
                const headerSelector = `${tableSelector} thead tr th`;
                cy.get(headerSelector)
                    .contains('Group Member')
                    .click()
                cy.get(headerSelector)
                    .contains('Group Member')
                    .click()

                cy.validateTable(tableSelector, [
                    [{ colIndex: 0, value: '0user' }],
                    [{ colIndex: 0, value: '1user' }],
                    [{ colIndex: 0, value: '2user' }],
                    [{ colIndex: 0, value: '3user' }],
                    [{ colIndex: 0, value: '4user' }],
                    [{ colIndex: 0, value: '5user' }],
                    [{ colIndex: 0, value: defaultUserForDisplay }],
                ], 5);
            });
    })

});
