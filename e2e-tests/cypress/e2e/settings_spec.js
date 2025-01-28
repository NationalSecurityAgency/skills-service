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
describe('Settings Tests', () => {

    beforeEach(() => {
        cy.logout();
        cy.fixture('vars.json')
            .then((vars) => {
                cy.login(vars.rootUser, vars.defaultPass);
            });

        cy.intercept('GET', '/app/projects').as('getProjects')
        Cypress.Commands.add('navToSettings', (isAdmin=false) => {
            cy.get('[data-cy="settings-button"] button')
                .click();
            if(isAdmin) {
                cy.wait('@getProjects')

            }
            cy.get('[data-pc-name="menu"] [data-pc-section="item"]').contains('Settings')
                .should('not.be.disabled');
            cy.get('[data-pc-name="menu"] [data-pc-section="item"]').contains('Settings')
                .click({force: true});
        });
    });

    const rootUsrTableSelector = '[data-cy="rootrm"] [data-cy="roleManagerTable"]';
    const supervisorTableSelector = '[data-cy="supervisorrm"] [data-cy="roleManagerTable"]';

    it('paging controls are visible if number of records are larger then smallest page size', () => {
        cy.intercept({
            method: 'GET',
            url: '/root/isRoot'
        })
            .as('checkRoot');
        cy.intercept('GET', '/root/users/roles/ROLE_SUPER_DUPER_USER**', (req) => {
            req.reply((res) => {
                const pagingResult = {
                    count: 5,
                    data: [
                        {
                            userId: 'one@one',
                            userIdForDisplay: 'one@one',
                            firstName: 'one',
                            lastName: 'one'
                        },
                        {
                            userId: 'two@two',
                            userIdForDisplay: 'two@one',
                            firstName: 'two',
                            lastName: 'two'
                        },
                        {
                            userId: 'three@three',
                            userIdForDisplay: 'three@one',
                            firstName: 'three',
                            lastName: 'three'
                        },
                        {
                            userId: 'four@four',
                            userIdForDisplay: 'four@one',
                            firstName: 'four',
                            lastName: 'four'
                        },
                        {
                            userId: 'five@five',
                            userIdForDisplay: 'five@one',
                            firstName: 'five',
                            lastName: 'five'
                        },
                        {
                            userId: 'six@six',
                            userIdForDisplay: 'six@one',
                            firstName: 'six',
                            lastName: 'six'
                        },
                        {
                            userId: 'seven@seven',
                            userIdForDisplay: 'seven@one',
                            firstName: 'seven',
                            lastName: 'seven'
                        },
                        {
                            userId: 'eight@eight',
                            userIdForDisplay: 'eight@one',
                            firstName: 'eight',
                            lastName: 'eight'
                        },
                        {
                            userId: 'nine@nine',
                            userIdForDisplay: 'nine@one',
                            firstName: 'nine',
                            lastName: 'nine'
                        },
                        {
                            userId: 'ten@ten',
                            userIdForDisplay: 'ten@one',
                            firstName: 'ten',
                            lastName: 'ten'
                        },
                        {
                            userId: 'eleven@eleven',
                            userIdForDisplay: 'eleven@one',
                            firstName: 'eleven',
                            lastName: 'eleven'
                        },
                        {
                            userId: 'twelve@twelve',
                            userIdForDisplay: 'twelve@one',
                            firstName: 'twelve',
                            lastName: 'twelve'
                        },
                        {
                            userId: 'thirteen@thirteen',
                            userIdForDisplay: 'thirteen@one',
                            firstName: 'thirteen',
                            lastName: 'thirteen'
                        },
                        {
                            userId: 'fourteen@fourteen',
                            userIdForDisplay: 'fourteen@one',
                            firstName: 'fourteen',
                            lastName: 'fourteen'
                        },
                        {
                            userId: 'fifteen@fifteen',
                            userIdForDisplay: 'fifteen@one',
                            firstName: 'fifteen',
                            lastName: 'fifteen'
                        },
                    ],
                    totalCount: 15
                };
                res.send(pagingResult);
            });
        })
            .as('loadRootUsers');

        cy.visit('/settings/security');
        cy.wait('@checkRoot');
        cy.wait('@loadRootUsers');
        cy.get('[data-cy="rootrm"] [data-cy="roleManagerTable"] [data-cy=skillsBTableTotalRows]')
            .should('have.text', '15');
    });

    it('Add and remove Root User', () => {
        cy.intercept('POST', '/root/users/without/role/ROLE_SUPER_DUPER_USER?userSuggestOption=ONE')
            .as('getEligibleForRoot');
        cy.intercept('PUT', '/root/users/skills@skills.org/roles/ROLE_SUPER_DUPER_USER')
            .as('addRoot');
        cy.intercept({
            method: 'GET',
            url: '/root/isRoot'
        })
            .as('checkRoot');
        cy.intercept('DELETE', '/root/users/*/roles/ROLE_SUPER_DUPER_USER')
            .as('deleteRootUser');
        cy.intercept('GET', '/root/users/roles/ROLE_SUPER_DUPER_USER**')
            .as('loadRootUsers');

        cy.visit('/settings/security');
        cy.wait('@checkRoot');
        cy.wait('@loadRootUsers');
        cy.validateTable(rootUsrTableSelector, [
            [{
                colIndex: 0,
                value: '(root@skills.org)'
            }],
        ], 5, true, null, false);

        cy.get('[data-cy="rootrm"] [data-cy="existingUserInputDropdown"] [data-pc-section="dropdown"]').click();
        cy.get('[data-cy="rootrm"] [data-cy="existingUserInputDropdown"]').type('sk');

        cy.wait('@getEligibleForRoot');
        cy.get('[data-pc-section="option"]')
            .contains('skills@skills.org')
            .click({ force: true });
        cy.contains('Add')
            .first()
            .click();
        cy.wait('@addRoot');

        cy.log('validating sort on Root User column');
        cy.wait('@loadRootUsers');
        cy.validateTable(rootUsrTableSelector, [
            [{
                colIndex: 0,
                value: '(root@skills.org)'
            }],
            [{
                colIndex: 0,
                value: '(skills@skills.org)'
            }],
        ], 5, true, null, false);

        // attempt to remove myself - no go
        cy.get(`[data-cy="controlsCell_root@skills.org"] [data-cy="removeUserBtn"]`)
            .should('be.disabled');
        // cy.get(`[data-cy="controlsCell_root@skills.org"] [data-cy="cannotRemoveWarning"]`).should('exist')

        cy.validateTable(rootUsrTableSelector, [
            [{
                colIndex: 0,
                value: '(root@skills.org)'
            }],
            [{
                colIndex: 0,
                value: '(skills@skills.org)'
            }],
        ], 5, true, null, false);

        // must also be added to supervisor table
        cy.get(`${supervisorTableSelector} th`)
            .contains('Supervisor User')
            .click();
        cy.validateTable(supervisorTableSelector, [
            [{
                colIndex: 0,
                value: '(skills@skills.org)'
            }],
        ], 5, true, null, false);

        // remove the other user now
        cy.get(`${rootUsrTableSelector} [data-cy="removeUserBtn"]`)
            .eq(1)
            .click();
        cy.get('[data-cy="removalSafetyCheckMsg"]').contains('This will remove skills@skills.org')
        cy.get('[data-cy="currentValidationText"]').type('Delete Me')
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.wait('@deleteRootUser');
        cy.validateTable(rootUsrTableSelector, [
            [{
                colIndex: 0,
                value: '(root@skills.org)'
            }],
        ], 5, true, null, false);

        // make sure user was removed from supervisor table
        cy.get(supervisorTableSelector)
            .contains('There are no records to show');
    });

    it('Add Root User - forward slash character does not cause error', () => {

        cy.intercept('POST', '/root/users/without/role/ROLE_SUPER_DUPER_USER?userSuggestOption=ONE')
            .as('getEligibleForRoot');
        cy.intercept('PUT', '/root/users/skills@skills.org/roles/ROLE_SUPER_DUPER_USER')
            .as('addRoot');
        cy.intercept('GET', '/app/projects')
            .as('loadProjects');
        cy.intercept('GET', '/app/userInfo/hasRole/ROLE_SUPERVISOR')
            .as('isSupervisor');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');
        cy.intercept('GET', '/public/config')
            .as('loadConfig');
        cy.intercept({
            method: 'GET',
            url: '/app/projects'
        })
            .as('loadProjects');
        cy.intercept({
            method: 'GET',
            url: '/root/isRoot'
        })
            .as('checkRoot');

        cy.visit('/administrator/');
        // cy.get('[data-cy="inception-button"]').contains('Level');
        cy.wait('@loadConfig');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProjects');
        cy.wait('@isSupervisor');
        cy.wait('@getProjects')

        cy.get('[data-cy=subPageHeader]')
            .contains('Projects');
        cy.get('[data-cy="settings-button"] button')
            .click();
        cy.get('[data-pc-name="menu"] [data-pc-section="item"]').contains('Settings')
            .click();
        cy.wait('@checkRoot');
        cy.clickNav('Security');
        cy.validateTable(rootUsrTableSelector, [
            [{
                colIndex: 0,
                value: '(root@skills.org)'
            }],
        ], 5, true, null, false);

        cy.get('[data-cy="existingUserInput"]')
            .first()
            .click()
            .type('sk/foo{enter}');
        cy.wait('@getEligibleForRoot');
    });

    it('Add Root User With No Query', () => {

        cy.intercept('POST', '/root/users/without/role/ROLE_SUPER_DUPER_USER?userSuggestOption=ONE')
            .as('getEligibleForRoot');
        cy.intercept('PUT', '/root/users/skills@skills.org/roles/ROLE_SUPER_DUPER_USER')
            .as('addRoot');
        cy.intercept({
            method: 'GET',
            url: '/app/projects'
        })
            .as('loadProjects');
        cy.intercept({
            method: 'GET',
            url: '/root/isRoot'
        })
            .as('checkRoot');

        cy.visit('/administrator/');

        cy.wait('@getProjects')

        cy.get('[data-cy="settings-button"] button')
            .click();
        cy.get('[data-pc-name="menu"] [data-pc-section="item"]').contains('Settings')
            .click();
        cy.wait('@checkRoot');
        cy.clickNav('Security');
        cy.validateTable(rootUsrTableSelector, [
            [{
                colIndex: 0,
                value: '(root@skills.org)'
            }],
        ], 5, true, null, false);

        cy.get('[data-cy="rootrm"] [data-cy="existingUserInputDropdown"] [data-pc-section="dropdown"]').click();

        cy.wait('@getEligibleForRoot');
        cy.get('[data-pc-section="option"]')
            .contains('skills@skills.org')
            .click();
        cy.contains('Add')
            .first()
            .click();
        cy.wait('@addRoot');

        // default sort order is userId asc
        cy.validateTable(rootUsrTableSelector, [
            [{
                colIndex: 0,
                value: '(root@skills.org)'
            }],
            [{
                colIndex: 0,
                value: '(skills@skills.org)'
            }],
        ], 5, true, null, false);
        ;

        cy.get(`${rootUsrTableSelector} th`)
            .contains('Root User')
            .click();
        cy.validateTable(rootUsrTableSelector, [
            [{
                colIndex: 0,
                value: '(skills@skills.org)'
            }],
            [{
                colIndex: 0,
                value: '(root@skills.org)'
            }],
        ], 5, true, null, false);
    });

    it('Add Supervisor User', () => {
        cy.intercept('PUT', '/root/users/root@skills.org/roles/ROLE_SUPERVISOR')
            .as('addSupervisor');
        cy.intercept('POST', 'root/users/without/role/ROLE_SUPERVISOR?userSuggestOption=ONE')
            .as('getEligibleForSupervisor');
        cy.intercept({
            method: 'GET',
            url: '/app/projects'
        })
            .as('loadProjects');
        cy.intercept({
            method: 'GET',
            url: '/root/isRoot'
        })
            .as('checkRoot');

        cy.visit('/administrator/');
        // cy.get('[data-cy="inception-button"]').contains('Level');
        cy.wait('@getProjects')

        cy.get('[data-cy=subPageHeader]')
            .contains('Projects');

        // cy.get('[data-cy="nav-Global Badges"]')
        //     .should('be.visible');
        // cy.window()
        //     .should('have.property', 'vm')
        //     .then((vm) => {
        //         cy.wrap(vm.$store)
        //             .its('state.access.isSupervisor')
        //             .should('equal', false);
        //     });
        cy.get('[data-cy="settings-button"] button')
            .click();
        cy.get('[data-pc-name="menu"] [data-pc-section="item"]').contains('Settings')
            .click();
        cy.wait('@checkRoot');
        cy.clickNav('Security');

        cy.get('[data-cy="supervisorrm"] [data-cy="existingUserInputDropdown"] [data-pc-section="dropdown"]').click();
        cy.get('[data-cy="supervisorrm"] [data-cy="existingUserInputDropdown"]').type('root');

        cy.wait('@getEligibleForSupervisor');
        cy.get('[data-pc-section="option"]')
            .contains('root@skills.org')
            .click();
        cy.get('[data-cy=supervisorrm]')
            .contains('Add')
            .click();
        cy.wait('@addSupervisor');
        cy.validateTable(supervisorTableSelector, [
            [{
                colIndex: 0,
                value: '(root@skills.org)'
            }],
        ], 5, true, null, false);

        // cy.window()
        //     .should('have.property', 'vm')
        //     .then((vm) => {
        //         cy.wrap(vm.$store)
        //             .its('state.access.isSupervisor')
        //             .should('equal', true);
        //     });
        // cy.contains('Home').click();
        cy.get('[data-cy=settings-button]')
            .click();
        cy.contains('Project Admin')
            .click();
        // cy.get('[data-cy=navigationmenu]')
        //     .contains('Badges', { timeout: 5000 })
        //     .should('be.visible');
    });

    it('Remove Supervisor User', () => {
        cy.intercept('PUT', '**/roles/ROLE_SUPERVISOR')
            .as('addSupervisor');
        cy.intercept('POST', 'root/users/without/role/ROLE_SUPERVISOR?userSuggestOption=ONE')
            .as('getEligibleForSupervisor');

        cy.visit('/settings/security');

        cy.get('[data-cy="supervisorrm"] [data-cy="existingUserInputDropdown"] [data-pc-section="dropdown"]').click();
        cy.get('[data-cy="supervisorrm"] [data-cy="existingUserInputDropdown"]').type('root');

        cy.wait('@getEligibleForSupervisor');
        cy.get('[data-pc-section="option"]')
            .contains('root@skills.org')
            .click();
        cy.get('[data-cy=supervisorrm]')
            .contains('Add')
            .click();
        cy.wait('@addSupervisor');

        cy.get('[data-cy="supervisorrm"] [data-cy="existingUserInputDropdown"] [data-pc-section="dropdown"]').click();
        cy.get('[data-cy="supervisorrm"] [data-cy="existingUserInputDropdown"]').type('{selectall}skills');

        cy.wait('@getEligibleForSupervisor');
        cy.get('[data-pc-section="option"]')
            .contains('skills@skills.org')
            .click();
        cy.get('[data-cy=supervisorrm]')
            .contains('Add')
            .click();
        cy.wait('@addSupervisor');

        // attempt to remove myself - no go
        cy.get(`${supervisorTableSelector} [data-cy="removeUserBtn"]`)
            .eq(0)
            .should('be.disabled');
        // cy.get(`[data-cy="controlsCell_root@skills.org"] [data-cy="cannotRemoveWarning"]`).should('exist')

        // click away to remove tooltip
        // cy.contains('SkillTree Dashboard')
        //     .click();
        cy.validateTable(supervisorTableSelector, [
            [{
                colIndex: 0,
                value: '(root@skills.org)'
            }],
            [{
                colIndex: 0,
                value: '(skills@skills.org)'
            }],
        ], 5, true, null, false);

        // remove the other user now
        cy.get(`${supervisorTableSelector} [data-cy="removeUserBtn"]`)
            .eq(1)
            .click();
        cy.get('[data-cy="removalSafetyCheckMsg"]').contains('This will remove skills@skills.org')
        cy.get('[data-cy="currentValidationText"]').type('Delete Me')
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.validateTable(supervisorTableSelector, [
            [{
                colIndex: 0,
                value: '(root@skills.org)'
            }],
        ], 5, true, null, false);

    });

    it('Add Supervisor User Not Found', () => {

        // cy.intercept('PUT', '/root/users/root@skills.org/roles/ROLE_SUPERVISOR').as('addSupervisor');
        cy.intercept('POST', 'root/users/without/role/ROLE_SUPERVISOR?userSuggestOption=ONE', [{
            'userId': 'blah@skills.org',
            'userIdForDisplay': 'blah@skills.org',
            'first': 'Firstname',
            'last': 'LastName',
            'nickname': 'Firstname LastName',
            'dn': null
        }])
            .as('getEligibleForSupervisor');
        cy.intercept({
            method: 'GET',
            url: '/app/projects'
        })
            .as('loadProjects');
        cy.intercept({
            method: 'GET',
            url: '/root/isRoot'
        })
            .as('checkRoot');

        cy.visit('/administrator/');
        // cy.get('[data-cy="inception-button"]').contains('Level');
        cy.wait('@getProjects')

        cy.get('[data-cy=subPageHeader]')
            .contains('Projects');

        // root user can see/manage global badges
        // cy.get('[data-cy="nav-Global Badges"]');
        // cy.window()
        //     .should('have.property', 'vm')
        //     .then((vm) => {
        //         cy.wrap(vm.$store)
        //             .its('state.access.isSupervisor')
        //             .should('equal', false);
        //     });
        cy.get('[data-cy="settings-button"] button')
            .click();
        cy.get('[data-pc-name="menu"] [data-pc-section="item"]').contains('Settings')
            .click();
        cy.wait('@checkRoot');
        cy.clickNav('Security');
        cy.get('[data-cy="supervisorrm"] [data-cy="existingUserInputDropdown"] [data-pc-section="dropdown"]').click();
        cy.get('[data-cy="supervisorrm"] [data-cy="existingUserInputDropdown"]').type('blah');

        cy.wait('@getEligibleForSupervisor');
        cy.get('[data-pc-section="option"]')
            .contains('blah@skills.org')
            .click();
        cy.get('[data-cy=supervisorrm]')
            .contains('Add')
            .click();
        cy.get('[data-cy=error-msg]')
            .contains('Error! Request could not be completed! User [blah@skills.org] does not exist');
        // cy.window()
        //     .should('have.property', 'vm')
        //     .then((vm) => {
        //         cy.wrap(vm.$store)
        //             .its('state.access.isSupervisor')
        //             .should('equal', false);
        //     });
    });

    it('Add Supervisor User No Query', () => {

        cy.intercept('PUT', '/root/users/root@skills.org/roles/ROLE_SUPERVISOR')
            .as('addSupervisor');
        cy.intercept('POST', 'root/users/without/role/ROLE_SUPERVISOR?userSuggestOption=ONE')
            .as('getEligibleForSupervisor');
        cy.intercept({
            method: 'GET',
            url: '/app/projects'
        })
            .as('loadProjects');

        cy.intercept({
            method: 'GET',
            url: '/root/isRoot'
        })
            .as('checkRoot');

        cy.visit('/administrator/');
        // cy.get('[data-cy="inception-button"]').contains('Level');
        cy.wait('@getProjects')


        // root user can see/manage global badges
        // cy.get('[data-cy="nav-Global Badges"]');
        // cy.window()
        //     .should('have.property', 'vm')
        //     .then((vm) => {
        //         cy.wrap(vm.$store)
        //             .its('state.access.isSupervisor')
        //             .should('equal', false);
        //     });
        cy.get('[data-cy="settings-button"] button')
            .click();
        cy.get('[data-pc-name="menu"] [data-pc-section="item"]').contains('Settings')
            .click();
        cy.wait('@checkRoot');
        cy.contains('Security')
            .click();
        cy.get('[data-cy="supervisorrm"] [data-cy="existingUserInputDropdown"] [data-pc-section="dropdown"]').click();
        cy.get('[data-cy="supervisorrm"] [data-cy="existingUserInputDropdown"]').type('sk/foo');
        cy.wait('@getEligibleForSupervisor');
    });

    it('Email Server Settings', () => {

        cy.intercept('GET', '/root/getEmailSettings')
            .as('loadEmailSettings');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');
        cy.visit('/administrator/');
        // cy.get('[data-cy="inception-button"]').contains('Level');
        cy.wait('@loadUserInfo');
        cy.wait('@getProjects')
        //
        cy.get('[data-cy="settings-button"] button')
            .click();
        cy.get('[data-pc-name="menu"] [data-pc-section="item"]').contains('Settings').click();

        cy.get('[data-cy="nav-Email"]')
            .click();
        cy.wait('@loadEmailSettings');
        cy.get('[data-cy=host]')
            .type('hi');
        cy.get('[data-cy=host]')
            .clear();
        cy.get('[data-cy=hostError]')
            .contains('Host is a required field');
        cy.get('[data-cy=emailSettingsSave]')
            .should('be.disabled');
        cy.get('[data-cy=host]')
            .type('{selectall}localhost');
        cy.get('[data-cy=port]')
            .clear();
        cy.get('[data-cy=portError]')
            .contains('Port is a required field');
        cy.get('[data-cy=emailSettingsSave]')
            .should('be.disabled');
        cy.get('[data-cy=port]')
            .type('{selectall}65536');
        cy.get('[data-cy=portError]')
            .contains('Port must be less than or equal to 65535');
        cy.get('[data-cy=port]')
            .type('{selectall}1025');
        cy.get('[data-cy=protocol]')
            .clear();
        cy.get('[data-cy=protocolError')
            .contains('Protocol is a required field');
        cy.get('[data-cy=emailSettingsSave]')
            .should('be.disabled');
        cy.get('[data-cy=protocol]')
            .type('{selectall}smtp');

        cy.get('[data-cy=publicUrl]')
            .type('test');
        cy.get('[data-cy=publicUrl]')
            .clear();
        cy.get('[data-cy=publicUrlError]')
            .should('be.visible');
        cy.get('[data-cy=publicUrlError]')
            .contains('Public URL is a required field')
            .should('be.visible');
        cy.get('[data-cy=publicUrl]')
            .type('{selectall}http://localhost:8082');
        cy.get('[data-cy=publicUrlError]')
            .should('not.be.visible');
        cy.get('[data-cy=fromEmail]')
            .type('{selectall}foo@skilltree.madeup');

        cy.get('[data-cy=tlsSwitch]').click();
        cy.get('[data-cy=authSwitch]').click();
        cy.get('[data-cy=username]')
            .should('be.visible');
        cy.get('[data-cy=password]')
            .should('be.visible');
        cy.get('[data-cy=username]')
            .type('username');
        cy.get('[data-cy=password]')
            .type('password');
        // cy.get('[data-cy=emailSettingsTest]').click();
        cy.get('[data-cy=emailSettingsSave]')
            .click();
        //verify that appropriate saved data is loaded when form is loaded again
        cy.get('[data-cy="nav-System"]')
            .click();
        cy.visit('/settings/email');
        cy.wait('@loadEmailSettings');
        cy.get('[data-cy=host]').should('have.value', 'localhost');
        cy.get('[data-cy="port"] [data-pc-name="pcinputtext"]').should('have.value', '1025');
        cy.get('[data-cy=protocol]')
            .should('have.value', 'smtp');
        cy.get('[data-cy=tlsSwitch] [data-pc-section="input"]').should('have.value', 'on');
        cy.get('[data-cy=authSwitch] [data-pc-section="input"]').should('have.value', 'on');
        cy.get('[data-cy=username]')
            .should('have.value', 'username');
        cy.get('[data-cy=password]')
            .should('have.value', 'password');
        cy.get('[data-cy=publicUrl]')
            .should('have.value', 'http://localhost:8082');
        cy.get('[data-cy=fromEmail]')
            .should('have.value', 'foo@skilltree.madeup');
    });

    it('Email Settings reasonable timeout', () => {

        cy.intercept('GET', '/root/getEmailSettings')
            .as('loadEmailSettings');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');
        cy.visit('/administrator/');
        // cy.get('[data-cy="inception-button"]').contains('Level');
        cy.wait('@loadUserInfo');
        cy.wait('@getProjects')
        //
        cy.get('[data-cy="settings-button"] button')
            .click();
        cy.get('[data-pc-name="menu"] [data-pc-section="item"]').contains('Settings').click();
        cy.get('[data-cy="nav-Email"]')
            .click();
        cy.wait('@loadEmailSettings');
        cy.get('[data-cy=host]')
            .type('{selectall}localhost');
        //this needs to be an open port that is NOT an smtp server for the purposes of this test
        cy.get('[data-cy=port]')
            .type('{selectall}8080');
        cy.get('[data-cy=protocol]')
            .type('{selectall}smtp');
        cy.get('[data-cy=publicUrl]')
            .type('test');
        cy.get('[data-cy=fromEmail]')
            .type('{selectall}foo@skilltree.madeup');

        cy.get('[data-cy=emailSettingsSave]')
            .click();
        cy.wait(12 * 1000);
        cy.get('[data-pc-name="message"]').should('be.visible');
    });

    it('System Settings', () => {

        cy.intercept('GET', '/root/getSystemSettings')
            .as('loadSystemSettings');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');
        cy.intercept('GET', '/public/config')
            .as('loadConfig');
        cy.visit('/settings/system');
        cy.wait('@loadUserInfo');
        cy.wait('@loadSystemSettings');
        cy.get('[data-cy=resetTokenExpiration]')
            .should('have.value', '2H');

        cy.get('[data-cy=resetTokenExpiration]')
            .type('{selectall}2H25M22S');
        cy.get('[data-cy=customHeader]')
            .type('<div id="customHeaderDiv" style="font-size:3em;color:red">HEADER</div>');
        cy.get('[data-cy=customFooter]')
            .type('<div id="customFooterDiv" style="font-size:3em;color:red">FOOTER</div>');
        cy.get('[data-cy=saveSystemSettings]')
            .click();
        cy.wait('@loadConfig');
        cy.get('#customHeaderDiv')
            .contains('HEADER');
        cy.get('#customFooterDiv')
            .contains('FOOTER');
        cy.visit('/settings/system');
        cy.wait('@loadSystemSettings');
        cy.get('[data-cy=resetTokenExpiration]')
            .should('have.value', '2H25M22S');
        cy.get('[data-cy=customHeader]')
            .should('have.value', '<div id="customHeaderDiv" style="font-size:3em;color:red">HEADER</div>');
        cy.get('[data-cy=customFooter]')
            .should('have.value', '<div id="customFooterDiv" style="font-size:3em;color:red">FOOTER</div>');

        //confirm that header/footer persist after logging out
        cy.logout();
        cy.visit('/administrator/');
        cy.wait('@loadConfig');
        cy.get('#customHeaderDiv')
            .contains('HEADER');
        cy.get('#customFooterDiv')
            .contains('FOOTER');
    });

    it('Updating system settings does not reset email settings', () => {
        cy.intercept('GET', '/root/getEmailSettings')
            .as('loadEmailSettings');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');
        cy.intercept('GET', '/public/config')
            .as('loadConfig');
        cy.intercept('GET', '/root/getSystemSettings')
            .as('loadSystemSettings');

        cy.visit('/settings/email');
        cy.wait('@loadUserInfo');
        cy.wait('@loadEmailSettings');

        cy.wait(1000)
        cy.get('[data-cy=host]')
            .type('{selectall}localhost');
        cy.get('[data-cy=port]')
            .type('{selectall}1025');
        cy.get('[data-cy=protocol]')
            .type('{selectall}smtp');
        cy.get('[data-cy=publicUrl]')
            .type('{selectall}http://localhost:8082');
        cy.get('[data-cy=fromEmail]')
            .type('{selectall}foo@skilltree.madeup');
        cy.get('[data-cy=emailSettingsTest]')
            .click();
        cy.get('[data-cy=emailSettingsSave]')
            .click();

        cy.get('[data-cy="nav-System"]')
            .click();
        cy.visit('/settings/email');
        cy.wait('@loadEmailSettings');
        cy.get('[data-cy=host]')
            .should('have.value', 'localhost');
        cy.get('[data-cy="port"] [data-pc-name="pcinputtext"]').should('have.value', '1025');
        cy.get('[data-cy=protocol]').should('have.value', 'smtp');
        cy.get('[data-cy=publicUrl]')
            .should('have.value', 'http://localhost:8082');
        cy.get('[data-cy=fromEmail]')
            .should('have.value', 'foo@skilltree.madeup');

        cy.visit('/administrator/');
        // cy.get('[data-cy="inception-button"]').contains('Level');
        cy.wait('@loadUserInfo');
        cy.wait('@getProjects')

        cy.get('[data-cy="settings-button"] button').click();
        cy.get('[data-pc-name="menu"] [data-pc-section="item"]').contains('Settings').click();
        cy.get('[data-cy="nav-System"]')
            .click();

        cy.wait('@loadSystemSettings');

        cy.get('[data-cy=customHeader')
            .type('{selectall}<div id="customHeaderDiv" style="font-size:3em;color:red">HEADER</div>');
        cy.get('[data-cy=customFooter')
            .type('{selectall}<div id="customFooterDiv" style="font-size:3em;color:red">FOOTER</div>');
        cy.get('[data-cy=saveSystemSettings]')
            .click();

        cy.get('[data-cy="nav-System"]')
            .click();
        cy.visit('/settings/email');
        cy.wait('@loadEmailSettings');
        cy.get('[data-cy=host]')
            .should('have.value', 'localhost');
        cy.get('[data-cy="port"] [data-pc-name="pcinputtext"]').should('have.value', '1025');
        cy.get('[data-cy=protocol]')
            .should('have.value', 'smtp');
        cy.get('[data-cy=publicUrl]')
            .should('have.value', 'http://localhost:8082');
        cy.get('[data-cy=fromEmail]')
            .should('have.value', 'foo@skilltree.madeup');

    })

    it('System Settings - script tags not allowed in footer/header', () => {

        cy.intercept('GET', '/root/getSystemSettings')
            .as('loadSystemSettings');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');
        cy.intercept('GET', '/public/config')
            .as('loadConfig');
        cy.visit('/administrator/');
        // cy.get('[data-cy="inception-button"]').contains('Level');
        cy.wait('@loadUserInfo');
        cy.wait('@getProjects')

        cy.get('[data-cy="settings-button"] button').click();
        cy.get('[data-pc-name="menu"] [data-pc-section="item"]').contains('Settings').click();

        cy.get('[data-cy="nav-System"]')
            .click();

        cy.wait('@loadSystemSettings');
        cy.get('[data-cy=resetTokenExpiration]')
            .should('have.value', '2H');
        cy.get('[data-cy=resetTokenExpiration]')
            .type('{selectall}2H25M22S');
        cy.get('[data-cy=customHeader]')
            .type('{selectall}<div id="customHeaderDiv" style="font-size:3em;color:red"><script src="somewhere"/>HEADER</div>');
        cy.get('[data-cy=customFooter]')
            .type('{selectall}<div id="customFooterDiv" style="font-size:3em;color:red"><script type="text/javascript">alert("foo");</script>FOOTER</div>');
        cy.get('[data-cy=customHeaderError]')
            .should('be.visible');
        cy.get('[data-cy=customHeaderError]')
            .contains('Script tags are not allowed');
        cy.get('[data-cy=customFooterError]')
            .should('be.visible');
        cy.get('[data-cy=customFooterError]')
            .contains('Script tags are not allowed');
        cy.get('[data-cy=saveSystemSettings]')
            .should('be.disabled');

        cy.get('[data-cy=customHeader]')
          .type('{selectall}<div id="customHeaderDiv" style="font-size:3em;color:red"><SCRIPT src="somewhere"/>HEADER</div>');
        cy.get('[data-cy=customFooter]')
          .type('{selectall}<div id="customFooterDiv" style="font-size:3em;color:red"><SCRIPT type="text/javascript">alert("foo");</SCRIPT>FOOTER</div>');
        cy.get('[data-cy=customHeaderError]')
          .should('be.visible');
        cy.get('[data-cy=customHeaderError]')
          .contains('Script tags are not allowed');
        cy.get('[data-cy=customFooterError]')
          .should('be.visible');
        cy.get('[data-cy=customFooterError]')
          .contains('Script tags are not allowed');
        cy.get('[data-cy=saveSystemSettings]')
          .should('be.disabled');

        cy.get('[data-cy=customHeader]')
          .type('{selectall}<div id="customHeaderDiv" style="font-size:3em;color:red"><sCrIPt src="somewhere"/>HEADER</div>');
        cy.get('[data-cy=customFooter]')
          .type('{selectall}<div id="customFooterDiv" style="font-size:3em;color:red"><ScRiPT type="text/javascript">alert("foo");</ScRiPT>FOOTER</div>');
        cy.get('[data-cy=customHeaderError]')
          .should('be.visible');
        cy.get('[data-cy=customHeaderError]')
          .contains('Script tags are not allowed');
        cy.get('[data-cy=customFooterError]')
          .should('be.visible');
        cy.get('[data-cy=customFooterError]')
          .contains('Script tags are not allowed');
        cy.get('[data-cy=saveSystemSettings]')
          .should('be.disabled');
    });

    it('custom header/custom footer must be 3000 characters or less', () => {
        const _3001 = 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA';
        const _3000 = 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA';

        cy.intercept('GET', '/root/getSystemSettings')
            .as('loadSystemSettings');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');
        cy.intercept('GET', '/public/config')
            .as('loadConfig');
        cy.visit('/administrator/');
        // cy.get('[data-cy="inception-button"]').contains('Level');
        cy.wait('@loadUserInfo');
        cy.wait('@getProjects')

        cy.get('[data-cy="settings-button"] button')
            .click();
        cy.get('[data-pc-name="menu"] [data-pc-section="item"]').contains('Settings')
            .click();
        cy.get('[data-cy="nav-System"]')
            .click();

        cy.wait('@loadSystemSettings');
        cy.get('[data-cy=resetTokenExpiration]')
            .should('have.value', '2H');
        cy.get('[data-cy=resetTokenExpiration]')
            .type('{selectall}2H25M22S');
        cy.get('[data-cy=customHeader]')
            .clear()
            .fill(_3001);
        cy.get('[data-cy=customFooter]')
            .clear()
            .fill(_3001);
        cy.get('[data-cy=customHeaderError]')
            .should('be.visible');
        cy.get('[data-cy=customHeaderError]')
            .contains('Custom Header must be at most 3000 characters');
        cy.get('[data-cy=customFooterError]')
            .should('be.visible');
        cy.get('[data-cy=customFooterError]')
            .contains('Custom Footer must be at most 3000 characters');
        cy.get('[data-cy=saveSystemSettings]')
            .should('be.disabled');
        cy.get('[data-cy=customHeader]')
            .clear()
            .fill(_3000);
        cy.get('[data-cy=customFooter]')
            .clear()
            .fill(_3000);
        cy.get('[data-cy=customFooterError]')
            .should('not.be.visible');
        cy.get('[data-cy=customHeaderError]')
            .should('not.be.visible');
        cy.get('[data-cy=saveSystemSettings]')
            .should('not.be.disabled');
    });

    it('from email validation', () => {

        cy.intercept('GET', '/root/getEmailSettings')
            .as('loadEmailSettings');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');
        cy.intercept('GET', '/public/config')
            .as('loadConfig');
        cy.visit('/administrator/');
        // cy.get('[data-cy="inception-button"]').contains('Level');
        // cy.get('[data-cy="inception-button"]').contains('Level');
        cy.wait('@loadUserInfo');
        cy.wait('@getProjects')

        cy.get('[data-cy="settings-button"] button')
            .click();
        cy.get('[data-pc-name="menu"] [data-pc-section="item"]').contains('Settings')
            .click();
        cy.get('[data-cy="nav-Email"]')
            .click();

        cy.wait('@loadEmailSettings');
        cy.get('[data-cy=host]')
            .type('{selectall}localhost');
        cy.get('[data-cy=publicUrl]')
            .type('{selectall}http://localhost');
        cy.get('[data-cy=fromEmail]')
            .type('{selectall}foo');
        cy.get('[data-cy=fromEmailError]')
            .should('be.visible');
        cy.get('[data-cy=fromEmailError]')
            .contains('From Email must be a valid email');
        cy.get('[data-cy=emailSettingsSave]')
            .should('be.disabled');
        cy.get('[data-cy=fromEmail]')
            .type('{selectall}foo@');
        cy.get('[data-cy=fromEmailError]')
            .should('be.visible');
        cy.get('[data-cy=fromEmailError]')
            .contains('From Email must be a valid email');
        cy.get('[data-cy=emailSettingsSave]')
            .should('be.disabled');
        cy.get('[data-cy=fromEmail]')
            .type('{selectall}foo@localhost.madeup');
        cy.get('[data-cy=fromEmailError]')
            .should('not.be.visible');
        cy.get('[data-cy=fromEmailError]')
            .should('not.be.visible');
        cy.get('[data-cy=emailSettingsSave]')
            .should('not.be.disabled');
    });

    it('custom header/footer should be full width', () => {

        cy.intercept('GET', '/root/getSystemSettings')
            .as('loadSystemSettings');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');
        cy.intercept('GET', '/public/config')
            .as('loadConfig');
        cy.visit('/settings/system');
        cy.wait('@loadUserInfo')
        cy.wait('@loadSystemSettings');
        cy.get('[data-cy=customHeader]')
            .type('<div id="customHeaderDiv" style="font-size:3em;color:red">HEADER</div>');
        cy.get('[data-cy=customFooter]')
            .type('<div id="customFooterDiv" style="font-size:3em;color:red">FOOTER</div>');
        cy.get('[data-cy=saveSystemSettings]')
            .click();
        cy.wait('@loadConfig');
        cy.get('#customHeaderDiv')
            .should('be.visible');
        cy.get('#customHeaderDiv')
            .contains('HEADER');
        cy.get('#customFooterDiv')
            .should('be.visible');
        cy.get('#customFooterDiv')
            .contains('FOOTER');

        let bodyWidth;
        cy.get('body')
            .invoke('width')
            .then((width) => {
                bodyWidth = width;
            });
        let appWidth;
        cy.get('#app')
            .invoke('width')
            .then((width) => {
                appWidth = width;
            }); //should be smaller due to padding applied to left and right of app

        let headerWidth;
        cy.get('#customHeaderDiv')
            .invoke('width')
            .then((width) => {
                headerWidth = width;
            });
        let footerWidth;
        cy.get('#customFooterDiv')
            .invoke('width')
            .then((width) => {
                footerWidth = width;
            });

        cy.get('body')
            .then(() => {
                cy.get('#customHeaderDiv')
                    .invoke('width')
                    .should('equal', bodyWidth);
                cy.get('#customFooterDiv')
                    .invoke('width')
                    .should('equal', bodyWidth);
                cy.get('#customHeaderDiv')
                    .invoke('width')
                    .should('equal', appWidth);
                cy.get('#customFooterDiv')
                    .invoke('width')
                    .should('equal', appWidth);
            });
    });

    it('custom header/footer dynamic variable replacement', () => {
        cy.intercept('GET', '/root/getSystemSettings')
            .as('loadSystemSettings');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');
        cy.intercept('GET', '/public/config')
            .as('loadConfig');
        cy.visit('/settings/system');
        cy.wait('@loadUserInfo');
        cy.wait('@loadSystemSettings');
        cy.contains('supports ISO 8601 time duration format')
        cy.get('[data-cy=customHeader]')
            .type('<div id="customHeaderDiv"><span id="chVersion">{{release.version}}</span> <span id="chBuildDate">{{build.date}}</span></div>', { parseSpecialCharSequences: false });
        cy.get('[data-cy=customFooter]')
            .type('<div id="customFooterDiv"><span id="cfVersion">{{release.version}}</span> <span id="cfBuildDate">{{build.date}}</span></div>', { parseSpecialCharSequences: false });
        cy.get('[data-cy=saveSystemSettings]')
            .click();
        cy.wait('@loadConfig');
        cy.get('#customHeaderDiv')
            .contains(/\w{3} \d{1,2}, \d{4}/)
            .should('be.visible');
        cy.get('#customHeaderDiv')
            .contains(/\d{1,3}\.\d{1,3}\.\d{1,3}(-SNAPSHOT)?/)
            .should('be.visible');
        cy.get('#customFooterDiv')
            .contains(/\w{3} \d{1,2}, \d{4}/)
            .should('be.visible');
        cy.get('#customFooterDiv')
            .contains(/\d{1,3}\.\d{1,3}\.\d{1,3}(-SNAPSHOT)?/)
            .should('be.visible');

        cy.get('[data-cy=customHeader')
            .type('{selectall}{backspace}');
        cy.get('[data-cy=customFooter')
            .type('{selectall}{backspace}');
        cy.get('[data-cy=customHeader')
            .type('<div id="customHeaderDiv"><span id="chVersion">{{ release.version }}</span> <span id="chBuildDate">{{ build.date }}</span></div>', { parseSpecialCharSequences: false });
        cy.get('[data-cy=customFooter')
            .type('<div id="customFooterDiv"><span id="cfVersion">{{ release.version }}</span> <span id="cfBuildDate">{{ build.date }}</span></div>', { parseSpecialCharSequences: false });
        cy.get('[data-cy=saveSystemSettings]')
            .click();
        cy.wait('@loadConfig');
        cy.get('#customHeaderDiv')
            .contains(/\w{3} \d{1,2}, \d{4}/)
            .should('be.visible');
        cy.get('#customHeaderDiv')
            .contains(/\d{1,3}\.\d{1,3}\.\d{1,3}(-SNAPSHOT)?/)
            .should('be.visible');
        cy.get('#customFooterDiv')
            .contains(/\w{3} \d{1,2}, \d{4}/)
            .should('be.visible');
        cy.get('#customFooterDiv')
            .contains(/\d{1,3}\.\d{1,3}\.\d{1,3}(-SNAPSHOT)?/)
            .should('be.visible');

        cy.get('[data-cy=customHeader')
            .type('{selectall}{backspace}');
        cy.get('[data-cy=customFooter')
            .type('{selectall}{backspace}');
        cy.get('[data-cy=customHeader')
            .type('<div id="customHeaderDiv"><span id="chVersion">{{ ReLeASe.VerSion}}</span> <span id="chBuildDate">{{BUild.daTE }}</span></div>', { parseSpecialCharSequences: false });
        cy.get('[data-cy=customFooter')
            .type('<div id="customFooterDiv"><span id="cfVersion">{{RELease.VERsion }}</span> <span id="cfBuildDate">{{ build.DATE}}</span></div>', { parseSpecialCharSequences: false });
        cy.get('[data-cy=saveSystemSettings]')
            .click();
        cy.wait('@loadConfig');
        cy.get('#customHeaderDiv')
            .contains(/\w{3} \d{1,2}, \d{4}/)
            .should('be.visible');
        cy.get('#customHeaderDiv')
            .contains(/\d{1,3}\.\d{1,3}\.\d{1,3}(-SNAPSHOT)?/)
            .should('be.visible');
        cy.get('#customFooterDiv')
            .contains(/\w{3} \d{1,2}, \d{4}/)
            .should('be.visible');
        cy.get('#customFooterDiv')
            .contains(/\d{1,3}\.\d{1,3}\.\d{1,3}(-SNAPSHOT)?/)
            .should('be.visible');
    });

    it('display logged in user under user icon', () => {
        cy.visit('/administrator/');
        cy.get('[data-cy="settings-button"] button')
            .click();
        cy.get('[data-cy="settingsButton-loggedInName"]')
            .contains('Firstname LastName');
    });

    it('nav to settings', () => {
        cy.visit('/administrator/');
        cy.navToSettings();
        cy.contains('* First Name');
    });

    it('Landing Page preference', () => {
        cy.intercept('POST', '/app/userInfo/**')
            .as('saveUserInfo');
        cy.intercept('GET', '/app/userInfo/**')
            .as('loadUserInfo');
        cy.intercept('/app/projects')
            .as('loadProjects');
        cy.intercept('/api/myProgressSummary')
            .as('loadMyProgressSummary');

        cy.visit('/administrator/');
        cy.navToSettings();
        cy.get('[data-cy="nav-Preferences"]')
            .click();
        cy.contains('Default Home Page')
            .should('be.visible');

        // verify the default is set to 'Progress and Rankings'
        cy.get('[data-cy="admin"] > input').should('not.be.checked');
        cy.get('[data-cy="progress"] > input').should('be.checked');

        // click SkillTree logo and verify we are on the correct page
        cy.get('[data-cy="skillTreeLogo"]')
            .click();
        // cy.wait('@loadMyProgressSummary');
        cy.get('[data-cy="breadcrumb-Progress And Rankings"]')
            .should('be.visible');

        // update home page to 'Project Admin'
        cy.navToSettings();
        cy.get('[data-cy="nav-Preferences"]')
            .click();
        cy.contains('Default Home Page')
            .should('be.visible');
        cy.get('[data-cy="userPrefsSettingsSave"]')
            .should('be.disabled');

        cy.get('[data-cy="admin"]').click();
        cy.get('[data-cy="admin"] > input').should('be.checked');
        cy.get('[data-cy="progress"] > input').should('not.be.checked');

        cy.get('[data-cy="userPrefsSettingsSave"]')
            .should('not.be.disabled');
        cy.get('[data-cy="userPrefsSettingsSave"]')
            .click();
        cy.wait('@saveUserInfo');
        cy.wait('@loadUserInfo');
        cy.get('[data-cy="settingsSavedAlert"]').contains('Settings Updated')

        // click SkillTree logo and verify we are on the correct page
        cy.get('[data-cy="skillTreeLogo"]')
            .click();
        cy.wait('@loadProjects');
        cy.get('[data-cy="breadcrumb-Projects"]')
            .should('be.visible');

        // now update home page back to 'Progress and Rankings'
        cy.navToSettings(true);
        cy.get('[data-cy="nav-Preferences"]')
            .click();
        cy.contains('Default Home Page')
            .should('be.visible');
        cy.get('[data-cy="userPrefsSettingsSave"]')
            .should('be.disabled');
        cy.get('[data-cy="progress"]')
            .click();
        cy.get('[data-cy="admin"] > input').should('not.be.checked');
        cy.get('[data-cy="progress"] > input').should('be.checked');

        cy.get('[data-cy="userPrefsSettingsSave"]')
            .should('not.be.disabled');
        cy.get('[data-cy="userPrefsSettingsSave"]')
            .click();
        cy.wait('@saveUserInfo');
        cy.wait('@loadUserInfo');
        cy.get('[data-cy="settingsSavedAlert"]').contains('Settings Updated')

        // click SkillTree logo and verify we are on the correct page
        cy.get('[data-cy="skillTreeLogo"]')
            .click();
        cy.wait('@loadMyProgressSummary');
        cy.get('[data-cy="breadcrumb-Progress And Rankings"]')
            .should('be.visible');

        // verify the unsaved changes alert is visible when values are changed
        // and not visible when they a the same as when loaded
        cy.navToSettings();
        cy.get('[data-cy="nav-Preferences"]')
            .click();
        cy.contains('Default Home Page')
            .should('be.visible');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .should('not.exist');
        cy.get('[data-cy="userPrefsSettingsSave"]')
            .should('be.disabled');
        cy.get('[data-cy="admin"]')
            .click();
        cy.get('[data-cy="progress"] > input')
            .should('not.be.checked');
        cy.get('[data-cy="admin"] > input')
            .should('be.checked');

        // unsaved changes visible and save button enabled
        cy.get('[data-cy="unsavedChangesAlert"]')
            .should('be.visible');
        cy.get('[data-cy="userPrefsSettingsSave"]')
            .should('not.be.disabled');

        // switch values back to original and unsaved changes should not visible and save button disabled
        cy.get('[data-cy="progress"]')
            .click();
        cy.get('[data-cy="admin"] > input')
            .should('not.be.checked');
        cy.get('[data-cy="progress"] > input')
            .should('be.checked');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .should('not.exist');
        cy.get('[data-cy="userPrefsSettingsSave"]')
            .should('be.disabled');
    });

    it('Rank and Leaderboard Opt-out', () => {
        cy.intercept('POST', '/app/userInfo/settings')
            .as('saveUserInfo');

        cy.visit('/settings/preferences');

        cy.get('[data-cy="userPrefsSettingsSave"]')
            .should('be.disabled');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .should('not.exist');

        cy.get('[data-cy="rankAndLeaderboardOptOutSwitch"]')
            .click();

        cy.get('[data-cy="userPrefsSettingsSave"]')
            .should('be.enabled');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .contains('Unsaved Changes');

        cy.get('[data-cy="rankAndLeaderboardOptOutSwitch"]')
            .click();

        cy.get('[data-cy="userPrefsSettingsSave"]')
            .should('be.disabled');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .should('not.exist');

        cy.get('[data-cy="rankAndLeaderboardOptOutSwitch"]')
            .click();

        cy.get('[data-cy="userPrefsSettingsSave"]')
            .should('be.enabled');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .contains('Unsaved Changes');

        cy.get('[data-cy="userPrefsSettingsSave"]')
            .click();
        cy.wait('@saveUserInfo');
        cy.get('[data-cy="userPrefsSettingsSave"]')
            .should('be.disabled');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .should('not.exist');

        // cy.get('[data-cy=tlsSwitch] [data-pc-section="input"]').should('have.value', 'on');
        // refresh and make sure props is still set
        cy.visit('/settings/preferences');
        cy.get('[data-cy="rankAndLeaderboardOptOutSwitch"] [data-pc-section="input"]').should('have.value', 'on');
        cy.get('[data-cy="userPrefsSettingsSave"]')
            .should('be.disabled');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .should('not.exist');

        cy.get('[data-cy="rankAndLeaderboardOptOutSwitch"]')
            .click();

        cy.get('[data-cy="rankOptOut"]').contains('No');
        cy.get('[data-cy="userPrefsSettingsSave"]')
            .should('be.enabled');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .contains('Unsaved Changes');

        cy.get('[data-cy="rankAndLeaderboardOptOutSwitch"]')
            .click();

        cy.get('[data-cy="rankAndLeaderboardOptOutSwitch"] [data-pc-section="input"]').should('have.value', 'on');
        cy.get('[data-cy="userPrefsSettingsSave"]')
            .should('be.disabled');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .should('not.exist');
    });

    it('show links to docs', () => {
        cy.visit('/administrator/');
        cy.get('[data-cy="help-button"]').click();
        cy.contains('Official Docs');
    });

    it('email header/footer settings', () => {
        cy.intercept({
            method: 'GET',
            url: '/app/projects'
        })
            .as('loadProjects');
        cy.intercept({
            method: 'GET',
            url: '/root/isRoot'
        })
            .as('checkRoot');
        cy.intercept({
            method: 'GET',
            url: '/root/global/settings/GLOBAL.EMAIL'
        })
            .as('loadTemplateSettings');

        cy.visit('/administrator/');

        cy.navToSettings();
        cy.get('[data-cy="nav-Email"]')
            .click();
        cy.wait('@loadTemplateSettings');

        cy.get('[data-cy=htmlHeader]')
            .click()
            .type('aaaaa');
        cy.get('[data-cy=htmlHeaderError]').should('have.text', 'Plaintext Header is also required');
        cy.get('[data-cy=plainTextHeaderError]')
          .should('be.visible');
        cy.get('[data-cy=plainTextHeaderError]')
          .should('have.text', 'Plaintext Header is required');
        cy.get('[data-cy=emailTemplateSettingsSave]')
            .should('be.disabled');


        cy.get('[data-cy=htmlHeader]')
            .clear();
        cy.get('[data-cy=plainTextHeader]')
            .click()
            .type('aaaa');
        cy.get('[data-cy=plainTextHeaderError]')
            .should('have.text', 'HTML Header is also required');
        cy.get('[data-cy=emailTemplateSettingsSave]')
            .should('be.disabled');

        cy.get('[data-cy=htmlHeaderError]')
            .should('be.visible');
        cy.get('[data-cy=htmlHeaderError]')
            .should('have.text', 'HTML Header is required');
        cy.get('[data-cy=htmlHeader]')
            .click()
            .type('aaaaa');
        cy.get('[data-cy=htmlHeaderError]')
            .should('not.be.visible');
        cy.get('[data-cy=emailTemplateSettingsSave]')
            .should('be.enabled');

        cy.get('[data-cy=htmlFooter]')
            .click()
            .type('aaaaa');
        cy.get('[data-cy=emailTemplateSettingsSave]')
            .should('be.disabled');
        cy.get('[data-cy=plainTextFooterError]')
            .should('be.visible');
        cy.get('[data-cy=plainTextFooterError]')
            .should('have.text', 'Plaintext Footer is required');
        cy.get('[data-cy=htmlFooter]')
            .clear();
        cy.get('[data-cy=plainTextFooter]')
            .click()
            .type('aaaa');
        cy.get('[data-cy=plainTextFooterError]')
            .should('have.text', 'HTML Footer is also required');
        cy.get('[data-cy=emailTemplateSettingsSave]')
            .should('be.disabled');
        cy.get('[data-cy=htmlFooterError]')
            .should('be.visible');
        cy.get('[data-cy=htmlFooterError]')
            .should('have.text', 'HTML Footer is required');
        cy.get('[data-cy=htmlFooter]')
            .click()
            .type('aaaaa');
        cy.get('[data-cy=htmlFooterError]')
            .should('not.be.visible');
        cy.get('[data-cy=emailTemplateSettingsSave]')
            .should('be.enabled');

        cy.get('[data-cy=emailTemplateSettingsSave]')
            .click();
        cy.get('[data-cy="nav-Security"]')
            .click();
        cy.get('[data-cy="nav-Email"]')
            .click();
        cy.wait('@loadTemplateSettings');
        cy.get('[data-cy=htmlHeader]')
            .should('have.value', 'aaaaa');
        cy.get('[data-cy=plainTextHeader]')
            .should('have.value', 'aaaa');
        cy.get('[data-cy=htmlFooter]')
            .should('have.value', 'aaaaa');
        cy.get('[data-cy=plainTextFooter]')
            .should('have.value', 'aaaa');

    });

    it('root help url validation', () => {
        cy.intercept('POST', '/admin/projects/proj1/settings')
            .as('setSettings');
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1'
        });
        cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill1`, {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: 'skill1',
            name: `This is 1`,
            type: 'Skill',
            pointIncrement: 500,
            numPerformToCompletion: 1,
            pointIncrementInterval: 0,
            numMaxOccurrencesIncrementInterval: -1,
            version: 0,
        });
        cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill2`, {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: 'skill2',
            name: `This is 2`,
            type: 'Skill',
            pointIncrement: 500,
            numPerformToCompletion: 1,
            pointIncrementInterval: 0,
            numMaxOccurrencesIncrementInterval: -1,
            version: 0,
        });

        cy.visit('/administrator/projects/proj1/');
        cy.clickNav('Settings');
        cy.get('[data-cy="customLabelsSwitch"]').click();
        cy.get('[data-cy=levelDisplayNameTextInput]')
            .click();
        cy.get('[data-cy=levelDisplayNameTextInput]')
            .type('s');
        cy.get('[data-cy=saveSettingsBtn]')
            .should('be.enabled');

        cy.get('[data-cy=helpUrlHostTextInput]')
            .clear()
            .type('javascript:alert("uh oh");');
        cy.get('[data-cy=helpUrlHostError]')
            .should('be.visible');
        cy.get('[data-cy=helpUrlHostError]')
            .should('have.text', 'Help URL/Path must start with "/" or "http(s)"');
        cy.get('[data-cy=saveSettingsBtn]')
            .should('be.disabled');
        cy.get('[data-cy=helpUrlHostTextInput]')
            .clear()
            .type('/foo?p1=v1&p2=v2');
        cy.get('[data-cy=helpUrlHostError]')
            .should('have.text', 'Help URL/Path must start with "/" or "http(s)"');
        cy.get('[data-cy=saveSettingsBtn]')
            .should('be.disabled');
        cy.get('[data-cy=helpUrlHostTextInput]')
            .clear()
            .type('http://foo.bar?p1=v1&p2=v2');
        cy.get('[data-cy=helpUrlHostError]')
            .should('not.exist');
        cy.get('[data-cy=saveSettingsBtn]')
            .should('be.enabled');
        cy.get('[data-cy=helpUrlHostTextInput]')
            .clear()
            .type('https://foo.bar?p1=v1&p2=v2');
        cy.get('[data-cy=helpUrlHostError]')
            .should('not.exist');
        cy.get('[data-cy=saveSettingsBtn]')
            .should('be.enabled');
    });

    it('Theme preference can be toggled and remains on refresh', () => {
        cy.intercept('POST', '/app/userInfo/**')
            .as('saveUserInfo');
        cy.intercept('GET', '/app/userInfo/**')
            .as('loadUserInfo');
        cy.intercept('/app/projects')
            .as('loadProjects');
        cy.intercept('/api/myProgressSummary')
            .as('loadMyProgressSummary');

        cy.visit('/administrator/');
        cy.navToSettings();
        cy.get('[data-cy="nav-Preferences"]').click();

        cy.get('[data-cy="enableDarkMode"]').should('exist');
        cy.get('[data-cy="enableDarkMode"]').contains('Off');
        cy.get('[data-cy="enableDarkModeSwitch"]').click();
        cy.get('[data-cy="enableDarkMode"]').contains('On');
        cy.get('[data-cy=userPrefsSettingsSave]').should('be.enabled');
        cy.get('[data-cy=userPrefsSettingsSave]').click();
        cy.get('[data-cy="nav-System"]').click();

        cy.get('[data-cy="nav-Preferences"]').click();
        cy.get('[data-cy="enableDarkMode"]').should('exist');
        cy.get('[data-cy="enableDarkMode"]').contains('On');
        cy.get('[data-cy=userPrefsSettingsSave]').should('not.be.enabled');

        cy.reload();

        cy.get('[data-cy="enableDarkMode"]').should('exist');
        cy.get('[data-cy="enableDarkMode"]').contains('On');
        cy.get('[data-cy=userPrefsSettingsSave]').should('not.be.enabled');

        cy.get('[data-cy="nav-Preferences"]').click();
        cy.get('[data-cy="enableDarkModeSwitch"]').click();
        cy.get('[data-cy="enableDarkMode"]').contains('Off');
        cy.get('[data-cy=userPrefsSettingsSave]').should('be.enabled');
    });
});

