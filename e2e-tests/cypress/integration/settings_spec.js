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
        cy.fixture('vars.json').then((vars) => {
            cy.login(vars.rootUser, vars.defaultPass);
        })

        Cypress.Commands.add("navToSettings", () => {
            cy.get('[data-cy="settings-button"]').click();
            cy.get('[data-cy="settingsButton-navToSettings"]').should('not.be.disabled');
            cy.get('[data-cy="settingsButton-navToSettings"]').click();
        });
    });

    const rootUsrTableSelector = '[data-cy="rootrm"] [data-cy="roleManagerTable"]';
    const supervisorTableSelector = '[data-cy="supervisorrm"] [data-cy="roleManagerTable"]';

    it('Add and remove Root User', () => {

        cy.intercept('POST', '/root/users/without/role/ROLE_SUPER_DUPER_USER?userSuggestOption=ONE').as('getEligibleForRoot');
        cy.intercept('PUT', '/root/users/skills@skills.org/roles/ROLE_SUPER_DUPER_USER').as('addRoot');
        cy.intercept({
            method: 'GET',
            url: '/app/projects'
        }).as('loadProjects');
        cy.intercept({method: 'GET', url: '/root/isRoot'}).as('checkRoot');

        cy.visit('/administrator/');
        cy.get('[data-cy=subPageHeader]').contains('Projects');
        cy.get('button.dropdown-toggle').first().click({force: true});
        cy.contains('Settings').click();
        cy.wait('@checkRoot');
        cy.clickNav('Security');
        cy.validateTable(rootUsrTableSelector, [
            [{ colIndex: 0,  value: '(root@skills.org)' }],
        ], 5, true, null, false);

        cy.contains('Enter user id').first().type('sk{enter}');
        cy.wait('@getEligibleForRoot');
        cy.contains('skills@skills.org').click();
        cy.contains('Add').first().click();
        cy.wait('@addRoot');

        cy.validateTable(rootUsrTableSelector, [
            [{ colIndex: 0,  value: '(root@skills.org)' }],
            [{ colIndex: 0,  value: '(skills@skills.org)' }],
        ], 5, true, null, false);

        // attempt to remove myself - no go
        cy.get(`${rootUsrTableSelector} [data-cy="removeUserBtn"]`).eq(0).should('be.disabled');
        cy.get(`${rootUsrTableSelector} [data-cy="removeUserBtn"]`).eq(0).click({ force: true });
        cy.contains('Can not remove myself');
        // click away to remove tooltip
        cy.contains('root@skills.org').click();
        cy.validateTable(rootUsrTableSelector, [
            [{ colIndex: 0,  value: '(root@skills.org)' }],
            [{ colIndex: 0,  value: '(skills@skills.org)' }],
        ], 5, true, null, false);

        // remove the other user now
        cy.get(`${rootUsrTableSelector} [data-cy="removeUserBtn"]`).eq(1).click();
        cy.contains('YES, Delete It').click();
        cy.validateTable(rootUsrTableSelector, [
            [{ colIndex: 0,  value: '(root@skills.org)' }],
        ], 5, true, null, false);
    });

    it('Add Root User - forward slash character does not cause error', () => {

        cy.intercept('POST', '/root/users/without/role/ROLE_SUPER_DUPER_USER?userSuggestOption=ONE').as('getEligibleForRoot');
        cy.intercept('PUT', '/root/users/skills@skills.org/roles/ROLE_SUPER_DUPER_USER').as('addRoot');
        cy.intercept('GET', '/app/projects').as('loadProjects');
        cy.intercept('GET', '/app/userInfo/hasRole/ROLE_SUPERVISOR').as('isSupervisor');
        cy.intercept('GET', '/app/userInfo').as('loadUserInfo');
        cy.intercept('GET', '/public/config').as('loadConfig');
        cy.intercept({
            method: 'GET',
            url: '/app/projects'
        }).as('loadProjects');
        cy.intercept({method: 'GET', url: '/root/isRoot'}).as('checkRoot');

        cy.visit('/administrator/');
        cy.wait('@loadConfig');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProjects');
        cy.wait('@isSupervisor');
        cy.get('[data-cy=subPageHeader]').contains('Projects');
        cy.get('button.dropdown-toggle').first().click({force: true});
        cy.contains('Settings').click();
        cy.wait('@checkRoot');
        cy.clickNav('Security');
        cy.validateTable(rootUsrTableSelector, [
            [{ colIndex: 0,  value: '(root@skills.org)' }],
        ], 5, true, null, false);

        cy.contains('Enter user id').first().type('sk/foo{enter}');
        cy.wait('@getEligibleForRoot');
    });


    it('Add Root User With No Query', () => {

        cy.intercept('POST', '/root/users/without/role/ROLE_SUPER_DUPER_USER?userSuggestOption=ONE').as('getEligibleForRoot');
        cy.intercept('PUT', '/root/users/skills@skills.org/roles/ROLE_SUPER_DUPER_USER').as('addRoot');
        cy.intercept({
            method: 'GET',
            url: '/app/projects'
        }).as('loadProjects');
        cy.intercept({method: 'GET', url: '/root/isRoot'}).as('checkRoot');

        cy.visit('/administrator/');
        cy.get('button.dropdown-toggle').first().click({force: true});
        cy.contains('Settings').click();
        cy.wait('@checkRoot');
        cy.clickNav('Security');
        cy.validateTable(rootUsrTableSelector, [
            [{ colIndex: 0,  value: '(root@skills.org)' }],
        ], 5, true, null, false);

        cy.contains('Enter user id').first().type('{enter}');
        cy.wait('@getEligibleForRoot');
        cy.contains('skills@skills.org').click();
        cy.contains('Add').first().click();
        cy.wait('@addRoot');

        cy.validateTable(rootUsrTableSelector, [
            [{ colIndex: 0,  value: '(root@skills.org)' }],
            [{ colIndex: 0,  value: '(skills@skills.org)' }],
        ], 5, true, null, false);;
    });

    it('Add Supervisor User', () => {
        cy.intercept('PUT', '/root/users/root@skills.org/roles/ROLE_SUPERVISOR').as('addSupervisor');
        cy.intercept('POST', 'root/users/without/role/ROLE_SUPERVISOR?userSuggestOption=ONE').as('getEligibleForSupervisor');
        cy.intercept({
            method: 'GET',
            url: '/app/projects'
        }).as('loadProjects');
        cy.intercept({method: 'GET', url: '/root/isRoot'}).as('checkRoot');

        cy.visit('/administrator/');
        cy.get('[data-cy=subPageHeader]').contains('Projects');

        cy.get('li').contains('Badges').should('not.exist');
        cy.vuex().its('state.access.isSupervisor').should('equal', false);
        cy.get('button.dropdown-toggle').first().click({force: true});
        cy.contains('Settings').click();
        cy.wait('@checkRoot');
        cy.clickNav('Security');

        cy.get('[data-cy=supervisorrm]  div.multiselect__tags').type('root');
        cy.wait('@getEligibleForSupervisor');
        cy.get('[data-cy=supervisorrm]').contains('root@skills.org').click();
        cy.get('[data-cy=supervisorrm]').contains('Add').click();
        cy.wait('@addSupervisor');
        cy.validateTable(supervisorTableSelector, [
            [{ colIndex: 0,  value: '(root@skills.org)' }],
        ], 5, true, null, false);

        cy.vuex().its('state.access.isSupervisor').should('equal', true);
        // cy.contains('Home').click();
        cy.get('[data-cy=settings-button]').click();
        cy.contains('Project Admin').click();
        cy.get('[data-cy=navigationmenu]').contains('Badges', {timeout: 5000}).should('be.visible');
    });

    it('Remove Supervisor User', () => {
        cy.intercept('PUT', '**/roles/ROLE_SUPERVISOR').as('addSupervisor');
        cy.intercept('POST', 'root/users/without/role/ROLE_SUPERVISOR?userSuggestOption=ONE').as('getEligibleForSupervisor');

        cy.visit('/settings/security');

        cy.get('[data-cy=supervisorrm]  div.multiselect__tags').type('root');
        cy.wait('@getEligibleForSupervisor');
        cy.get('[data-cy=supervisorrm]').contains('root@skills.org').click();
        cy.get('[data-cy=supervisorrm]').contains('Add').click();
        cy.wait('@addSupervisor');

        cy.get('[data-cy=supervisorrm]  div.multiselect__tags').type('root');
        cy.wait('@getEligibleForSupervisor');
        cy.get('[data-cy=supervisorrm]').contains('skills@skills.org').click();
        cy.get('[data-cy=supervisorrm]').contains('Add').click();
        cy.wait('@addSupervisor');

        // attempt to remove myself - no go
        cy.get(`${supervisorTableSelector} [data-cy="removeUserBtn"]`).eq(0).should('be.disabled');
        cy.get(`${supervisorTableSelector} [data-cy="removeUserBtn"]`).eq(0).click({ force: true });
        cy.contains('Can not remove myself');
        // click away to remove tooltip
        cy.contains('SkillTree Dashboard').click();
        cy.validateTable(supervisorTableSelector, [
            [{ colIndex: 0,  value: '(root@skills.org)' }],
            [{ colIndex: 0,  value: '(skills@skills.org)' }],
        ], 5, true, null, false);

        // remove the other user now
        cy.get(`${supervisorTableSelector} [data-cy="removeUserBtn"]`).eq(1).click();
        cy.contains('YES, Delete It').click();
        cy.validateTable(supervisorTableSelector, [
            [{ colIndex: 0,  value: '(root@skills.org)' }],
        ], 5, true, null, false);

    });

    it('Add Supervisor User Not Found', () => {

        // cy.intercept('PUT', '/root/users/root@skills.org/roles/ROLE_SUPERVISOR').as('addSupervisor');
        cy.intercept('POST', 'root/users/without/role/ROLE_SUPERVISOR?userSuggestOption=ONE', [{"userId":"blah@skills.org","userIdForDisplay":"blah@skills.org","first":"Firstname","last":"LastName","nickname":"Firstname LastName","dn":null}]).as('getEligibleForSupervisor');
        cy.intercept({
            method: 'GET',
            url: '/app/projects'
        }).as('loadProjects');
        cy.intercept({method: 'GET', url: '/root/isRoot'}).as('checkRoot');

        cy.visit('/administrator/');
        cy.get('[data-cy=subPageHeader]').contains('Projects');

        cy.get('li').contains('Badges').should('not.exist');
        cy.vuex().its('state.access.isSupervisor').should('equal', false);
        cy.get('button.dropdown-toggle').first().click({force: true});
        cy.contains('Settings').click();
        cy.wait('@checkRoot');
        cy.clickNav('Security');
        cy.get('[data-cy=supervisorrm]  div.multiselect__tags').type('root');
        cy.wait('@getEligibleForSupervisor');
        cy.get('[data-cy=supervisorrm]').contains('blah@skills.org').click();
        cy.get('[data-cy=supervisorrm]').contains('Add').click();
        cy.get('[data-cy=error-msg]').contains('Error! Request could not be completed! User [blah@skills.org] does not exist')
        cy.vuex().its('state.access.isSupervisor').should('equal', false);
    });

    it('Add Supervisor User No Query', () => {


        cy.intercept('PUT', '/root/users/root@skills.org/roles/ROLE_SUPERVISOR').as('addSupervisor');
        cy.intercept('POST', 'root/users/without/role/ROLE_SUPERVISOR?userSuggestOption=ONE').as('getEligibleForSupervisor');
        cy.intercept({
            method: 'GET',
            url: '/app/projects'
        }).as('loadProjects');

        cy.intercept({method: 'GET', url: '/root/isRoot'}).as('checkRoot');

        cy.visit('/administrator/');

        cy.get('li').contains('Badges').should('not.exist');
        cy.vuex().its('state.access.isSupervisor').should('equal', false);
        cy.get('button.dropdown-toggle').first().click({force: true});
        cy.contains('Settings').click();
        cy.wait('@checkRoot');
        cy.contains('Security').click();
        cy.get('[data-cy=supervisorrm]  div.multiselect__tags').type('foo/bar{enter}');
        cy.wait('@getEligibleForSupervisor');
    });

    it('Email Server Settings', () => {

        cy.intercept('GET', '/root/getEmailSettings').as('loadEmailSettings');
        cy.intercept('GET', '/app/userInfo').as('loadUserInfo');
        cy.visit('/administrator/');
        cy.wait('@loadUserInfo');
         cy.get('[data-cy="settings-button"]').click();
        cy.contains('Settings').click();
        cy.contains('Email').click();
        cy.wait('@loadEmailSettings');
        cy.get$('[data-cy=hostInput]').clear();
        cy.get$('[data-cy=hostError]').contains('Host is required');
        cy.get$('[data-cy=emailSettingsSave]').should('be.disabled');
        cy.get$('[data-cy=hostInput]').type('{selectall}localhost');
        cy.get$('[data-cy=portInput]').clear();
        cy.get$('[data-cy=portError]').contains('Port is required');
        cy.get$('[data-cy=emailSettingsSave]').should('be.disabled');
        cy.get$('[data-cy=portInput]').type('{selectall}-55');
        cy.get$('[data-cy=portError]').contains('Port must be 1 or greater');
        cy.get$('[data-cy=portInput]').type('{selectall}65536');
        cy.get$('[data-cy=portError]').contains('Port must be 65535 or less');
        cy.get$('[data-cy=portInput]').type('{selectall}1026');
        cy.get$('[data-cy=protocolInput]').clear();
        cy.get$('[data-cy=protocolError').contains('Protocol is required');
        cy.get$('[data-cy=emailSettingsSave]').should('be.disabled');
        cy.get$('[data-cy=protocolInput]').type('{selectall}smtp');


        cy.get$('[data-cy=tlsSwitch]').next('.custom-control-label').click();
        cy.get$('[data-cy=authSwitch]').next('.custom-control-label').click();
        cy.get('[data-cy=emailUsername]').should('be.visible');
        cy.get('[data-cy=emailPassword]').should('be.visible');
        cy.get$('[data-cy=emailUsername]').type('username');
        cy.get$('[data-cy=emailPassword]').type('password');
        cy.get$('[data-cy=emailSettingsTest]').click();
        cy.get$('[data-cy=emailSettingsSave]').click();
        //verify that appropriate saved data is loaded when form is loaded again
        cy.contains('System').click();
        cy.visit('/settings/email');
        cy.wait('@loadEmailSettings');
        cy.get('[data-cy=hostInput]').should('have.value', 'localhost');
        cy.get('[data-cy=portInput]').should('have.value', '1026');
        cy.get('[data-cy=protocolInput]').should('have.value', 'smtp');
        cy.get('[data-cy=tlsSwitch]').should('have.value', 'true');
        cy.get('[data-cy=authSwitch]').should('have.value', 'true');
        cy.get('[data-cy=emailUsername]').should('have.value', 'username');
        cy.get('[data-cy=emailPassword]').should('have.value', 'password');
    });

    it('Email Settings reasonable timeout', () => {

        cy.intercept('GET', '/root/getEmailSettings').as('loadEmailSettings');
        cy.intercept('GET', '/app/userInfo').as('loadUserInfo');
        cy.visit('/administrator/');
        cy.wait('@loadUserInfo');
         cy.get('[data-cy="settings-button"]').click();
        cy.contains('Settings').click();
        cy.contains('Email').click();
        cy.wait('@loadEmailSettings');
        cy.get$('[data-cy=hostInput]').type('{selectall}localhost');
        //this needs to be an open port that is NOT an smtp server for the purposes of this test
        cy.get$('[data-cy=portInput]').type('{selectall}8080');
        cy.get$('[data-cy=protocolInput]').type('{selectall}smtp');

        cy.get$('[data-cy=emailSettingsSave]').click();
        cy.wait(12*1000);
        cy.get('[data-cy=connectionError]').should('be.visible');
    });

    it('System Settings', () => {

        cy.intercept('GET', '/root/getSystemSettings').as('loadSystemSettings');
        cy.intercept('GET', '/app/userInfo').as('loadUserInfo');
        cy.intercept('GET', '/public/config').as('loadConfig');
        cy.visit('/administrator/');
        cy.wait('@loadUserInfo');
         cy.get('[data-cy="settings-button"]').click();
        cy.contains('Settings').click();
        cy.contains('System').click();

        cy.wait('@loadSystemSettings');
        cy.get('[data-cy=resetTokenExpiration]').should('have.value', '2H');
        cy.get$('[data-cy=publicUrl]').type('{selectall}http://localhost:8082');
        cy.get$('[data-cy=resetTokenExpiration]').type('{selectall}2H25M22S');
        cy.get$('[data-cy=fromEmail]').type('{selectall}foo@skilltree.madeup');
        cy.get$('[data-cy=customHeader').type('{selectall}<div id="customHeaderDiv" style="font-size:3em;color:red">HEADER</div>');
        cy.get$('[data-cy=customFooter').type('{selectall}<div id="customFooterDiv" style="font-size:3em;color:red">FOOTER</div>');
        cy.get$('[data-cy=saveSystemSettings]').click();
        cy.wait('@loadConfig');
        cy.get('#customHeaderDiv').contains('HEADER');
        cy.get('#customFooterDiv').contains('FOOTER');
        cy.visit('/settings/system');
        cy.wait('@loadSystemSettings');
        cy.get('[data-cy=publicUrl]').should('have.value', 'http://localhost:8082');
        cy.get('[data-cy=resetTokenExpiration]').should('have.value', '2H25M22S');
        cy.get('[data-cy=fromEmail]').should('have.value', 'foo@skilltree.madeup');
        cy.get('[data-cy=customHeader').should('have.value','<div id="customHeaderDiv" style="font-size:3em;color:red">HEADER</div>');
        cy.get('[data-cy=customFooter').should('have.value','<div id="customFooterDiv" style="font-size:3em;color:red">FOOTER</div>');

        //confirm that header/footer persist after logging out
        cy.logout();
        cy.visit('/administrator/');
        cy.wait('@loadConfig');
        cy.get('#customHeaderDiv').contains('HEADER');
        cy.get('#customFooterDiv').contains('FOOTER');
    });

    it('System Settings - script tags not allowed in footer/header', () => {

        cy.intercept('GET', '/root/getSystemSettings').as('loadSystemSettings');
        cy.intercept('GET', '/app/userInfo').as('loadUserInfo');
        cy.intercept('GET', '/public/config').as('loadConfig');
        cy.visit('/administrator/');
        cy.wait('@loadUserInfo');
         cy.get('[data-cy="settings-button"]').click();
        cy.contains('Settings').click();
        cy.contains('System').click();

        cy.wait('@loadSystemSettings');
        cy.get('[data-cy=resetTokenExpiration]').should('have.value', '2H');
        cy.get$('[data-cy=publicUrl]').type('{selectall}http://localhost:8082');
        cy.get$('[data-cy=resetTokenExpiration]').type('{selectall}2H25M22S');
        cy.get$('[data-cy=fromEmail]').type('{selectall}foo@skilltree.madeup');
        cy.get$('[data-cy=customHeader]').type('{selectall}<div id="customHeaderDiv" style="font-size:3em;color:red"><script src="somewhere"/>HEADER</div>');
        cy.get$('[data-cy=customFooter]').type('{selectall}<div id="customFooterDiv" style="font-size:3em;color:red"><script type="text/javascript">alert("foo");</script>FOOTER</div>');
        cy.get('[data-cy=customHeaderError]').should('be.visible');
        cy.get('[data-cy=customHeaderError]').contains('<script> tags are not allowed');
        cy.get('[data-cy=customFooterError]').should('be.visible');
        cy.get('[data-cy=customFooterError]').contains('<script> tags are not allowed');
        cy.get('[data-cy=saveSystemSettings]').should('be.disabled');
    });

    it('custom header/custom footer must be 3000 characters or less', () => {
       const _3001 = 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA';
       const _3000 = 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'


        cy.intercept('GET', '/root/getSystemSettings').as('loadSystemSettings');
        cy.intercept('GET', '/app/userInfo').as('loadUserInfo');
        cy.intercept('GET', '/public/config').as('loadConfig');
        cy.visit('/administrator/');
        cy.wait('@loadUserInfo');
         cy.get('[data-cy="settings-button"]').click();
        cy.contains('Settings').click();
        cy.contains('System').click();

        cy.wait('@loadSystemSettings');
        cy.get('[data-cy=resetTokenExpiration]').should('have.value', '2H');
        cy.get$('[data-cy=publicUrl]').type('{selectall}http://localhost:8082');
        cy.get$('[data-cy=resetTokenExpiration]').type('{selectall}2H25M22S');
        cy.get$('[data-cy=fromEmail]').type('{selectall}foo@skilltree.madeup');
        cy.get$('[data-cy=customHeader]').clear().fill(_3001);
        cy.get$('[data-cy=customFooter]').clear().fill(_3001);
        cy.get('[data-cy=customHeaderError]').should('be.visible');
        cy.get('[data-cy=customHeaderError]').contains('Custom Header may not be greater than 3000 characters');
        cy.get('[data-cy=customFooterError]').should('be.visible');
        cy.get('[data-cy=customFooterError]').contains('Custom Footer may not be greater than 3000 characters');
        cy.get('[data-cy=saveSystemSettings]').should('be.disabled');
        cy.get$('[data-cy=customHeader]').clear().fill(_3000);
        cy.get$('[data-cy=customFooter]').clear().fill(_3000);
        cy.get('[data-cy=customFooterError]').should('not.be.visible');
        cy.get('[data-cy=customHeaderError]').should('not.be.visible');
        cy.get('[data-cy=saveSystemSettings]').should('not.be.disabled');
    });

    it('from email validation', () => {

        cy.intercept('GET', '/root/getSystemSettings').as('loadSystemSettings');
        cy.intercept('GET', '/app/userInfo').as('loadUserInfo');
        cy.intercept('GET', '/public/config').as('loadConfig');
        cy.visit('/administrator/');
        cy.wait('@loadUserInfo');
         cy.get('[data-cy="settings-button"]').click();
        cy.contains('Settings').click();
        cy.contains('System').click();

        cy.wait('@loadSystemSettings');
        cy.get('[data-cy=publicUrl]').type('{selectall}http://localhost');
        cy.get('[data-cy=resetTokenExpiration]').should('have.value', '2H');
        cy.get$('[data-cy=fromEmail]').type('{selectall}foo');
        cy.get('[data-cy=fromEmailError]').should('be.visible');
        cy.get('[data-cy=fromEmailError]').contains('From Email must be a valid email');
        cy.get('[data-cy=saveSystemSettings]').should('be.disabled');
        cy.get$('[data-cy=fromEmail]').type('{selectall}foo@');
        cy.get('[data-cy=fromEmailError]').should('be.visible');
        cy.get('[data-cy=fromEmailError]').contains('From Email must be a valid email');
        cy.get('[data-cy=saveSystemSettings]').should('be.disabled');
        cy.get$('[data-cy=fromEmail]').type('{selectall}foo@localhost.madeup');
        cy.get('[data-cy=fromEmailError]').should('not.be.visible');
        cy.get('[data-cy=fromEmailError]').should('not.be.visible');
        cy.get('[data-cy=saveSystemSettings]').should('not.be.disabled');
    });

    it('custom header/footer should be full width', () => {

        cy.intercept('GET', '/root/getSystemSettings').as('loadSystemSettings');
        cy.intercept('GET', '/app/userInfo').as('loadUserInfo');
        cy.intercept('GET', '/public/config').as('loadConfig');
        cy.visit('/administrator/');
        cy.wait('@loadUserInfo');
         cy.get('[data-cy="settings-button"]').click();
        cy.contains('Settings').click();
        cy.contains('System').click();

        cy.wait('@loadSystemSettings');
        cy.get$('[data-cy=publicUrl]').type('{selectall}http://localhost:8082');
        cy.get$('[data-cy=customHeader').type('{selectall}<div id="customHeaderDiv" style="font-size:3em;color:red">HEADER</div>');
        cy.get$('[data-cy=customFooter').type('{selectall}<div id="customFooterDiv" style="font-size:3em;color:red">FOOTER</div>');
        cy.get$('[data-cy=saveSystemSettings]').click();
        cy.wait('@loadConfig');
        cy.get('#customHeaderDiv').should('be.visible')
        cy.get('#customHeaderDiv').contains('HEADER');
        cy.get('#customFooterDiv').should('be.visible');
        cy.get('#customFooterDiv').contains('FOOTER');

        let bodyWidth;
        cy.get('body').invoke('width').then((width) => {
            bodyWidth = width;
        });
        let appWidth;
        cy.get('#app').invoke('width').then((width) => {
            appWidth = width;
        }); //should be smaller due to padding applied to left and right of app

        let headerWidth;
        cy.get('#customHeaderDiv').invoke('width').then((width) => {
            headerWidth = width;
        });
        let footerWidth;
        cy.get('#customFooterDiv').invoke('width').then((width) => {
            footerWidth = width;
        });

        cy.get('body').then( () => {
            cy.get('#customHeaderDiv').invoke('width').should('equal', bodyWidth);
            cy.get('#customFooterDiv').invoke('width').should('equal', bodyWidth);
            cy.get('#customHeaderDiv').invoke('width').should('equal', appWidth);
            cy.get('#customFooterDiv').invoke('width').should('equal', appWidth);
        });
    });

    it('custom header/footer dynamic variable replacement', () => {

        cy.intercept('GET', '/root/getSystemSettings').as('loadSystemSettings');
        cy.intercept('GET', '/app/userInfo').as('loadUserInfo');
        cy.intercept('GET', '/public/config').as('loadConfig');
        cy.visit('/administrator/');
        cy.wait('@loadUserInfo');
         cy.get('[data-cy="settings-button"]').click();
        cy.contains('Settings').click();
        cy.contains('System').click();

        cy.wait('@loadSystemSettings');
        cy.get$('[data-cy=publicUrl]').type('{selectall}http://localhost:8082');
        cy.get$('[data-cy=customHeader').type('<div id="customHeaderDiv"><span id="chVersion">{{release.version}}</span> <span id="chBuildDate">{{build.date}}</span></div>', {parseSpecialCharSequences: false});
        cy.get$('[data-cy=customFooter').type('<div id="customFooterDiv"><span id="cfVersion">{{release.version}}</span> <span id="cfBuildDate">{{build.date}}</span></div>', {parseSpecialCharSequences: false});
        cy.get$('[data-cy=saveSystemSettings]').click();
        cy.wait('@loadConfig');
        cy.get('#customHeaderDiv').contains(/\w{3} \d{1,2}, \d{4}/).should('be.visible');
        cy.get('#customHeaderDiv').contains(/\d{1,3}\.\d{1,3}\.\d{1,3}(-SNAPSHOT)?/).should('be.visible');
        cy.get('#customFooterDiv').contains(/\w{3} \d{1,2}, \d{4}/).should('be.visible');
        cy.get('#customFooterDiv').contains(/\d{1,3}\.\d{1,3}\.\d{1,3}(-SNAPSHOT)?/).should('be.visible');

        cy.get$('[data-cy=customHeader').type('{selectall}{backspace}');
        cy.get$('[data-cy=customFooter').type('{selectall}{backspace}');
        cy.get$('[data-cy=customHeader').type('<div id="customHeaderDiv"><span id="chVersion">{{ release.version }}</span> <span id="chBuildDate">{{ build.date }}</span></div>', {parseSpecialCharSequences: false});
        cy.get$('[data-cy=customFooter').type('<div id="customFooterDiv"><span id="cfVersion">{{ release.version }}</span> <span id="cfBuildDate">{{ build.date }}</span></div>', {parseSpecialCharSequences: false});
        cy.get$('[data-cy=saveSystemSettings]').click();
        cy.wait('@loadConfig');
        cy.get('#customHeaderDiv').contains(/\w{3} \d{1,2}, \d{4}/).should('be.visible');
        cy.get('#customHeaderDiv').contains(/\d{1,3}\.\d{1,3}\.\d{1,3}(-SNAPSHOT)?/).should('be.visible');
        cy.get('#customFooterDiv').contains(/\w{3} \d{1,2}, \d{4}/).should('be.visible');
        cy.get('#customFooterDiv').contains(/\d{1,3}\.\d{1,3}\.\d{1,3}(-SNAPSHOT)?/).should('be.visible');

        cy.get$('[data-cy=customHeader').type('{selectall}{backspace}');
        cy.get$('[data-cy=customFooter').type('{selectall}{backspace}');
        cy.get$('[data-cy=customHeader').type('<div id="customHeaderDiv"><span id="chVersion">{{ ReLeASe.VerSion}}</span> <span id="chBuildDate">{{BUild.daTE }}</span></div>', {parseSpecialCharSequences: false});
        cy.get$('[data-cy=customFooter').type('<div id="customFooterDiv"><span id="cfVersion">{{RELease.VERsion }}</span> <span id="cfBuildDate">{{ build.DATE}}</span></div>', {parseSpecialCharSequences: false});
        cy.get$('[data-cy=saveSystemSettings]').click();
        cy.wait('@loadConfig');
        cy.get('#customHeaderDiv').contains(/\w{3} \d{1,2}, \d{4}/).should('be.visible');
        cy.get('#customHeaderDiv').contains(/\d{1,3}\.\d{1,3}\.\d{1,3}(-SNAPSHOT)?/).should('be.visible');
        cy.get('#customFooterDiv').contains(/\w{3} \d{1,2}, \d{4}/).should('be.visible');
        cy.get('#customFooterDiv').contains(/\d{1,3}\.\d{1,3}\.\d{1,3}(-SNAPSHOT)?/).should('be.visible');
    });

    it('display logged in user under user icon', () => {
        cy.visit('/')
        cy.get('[data-cy="settings-button"]').click();
        cy.get('[data-cy="settingsButton-loggedInName"]').contains('Firstname LastName')
    })

    it('nav to settings', () => {
        cy.visit('/')
        cy.navToSettings();
        cy.contains('* First Name');
    })

    it('Landing Page preference', () => {
        cy.intercept('POST', '/app/userInfo').as('saveUserInfo');
        cy.intercept('GET', '/app/userInfo').as('loadUserInfo');
        cy.intercept('/app/projects').as('loadProjects');
        cy.intercept('/api/myProgressSummary').as('loadMyProgressSummary');

        cy.visit('/')
        cy.navToSettings();
        cy.get('[data-cy="nav-Preferences"]').click();

        // verify the default is set to 'Progress and Rankings'
        cy.get('[data-cy="landingPageSelector"] [value="progress"]').should('be.checked');
        cy.get('[data-cy="landingPageSelector"] [value="admin"]').should('not.be.checked');

        // click SkillTree logo and verify we are on the correct page
        cy.get('[data-cy="skillTreeLogo"]').click()
        cy.wait('@loadMyProgressSummary');
        cy.get('[data-cy="breadcrumb-Progress And Rankings"]').should('be.visible');

        // update home page to 'Project Admin'
        cy.navToSettings()
        cy.get('[data-cy="nav-Preferences"]').click();
        cy.get('[data-cy="userPrefsSettingsSave"]').should('be.disabled');
        cy.get('[data-cy="landingPageSelector"] [value="admin"]').click({force:true})
        cy.get('[data-cy="landingPageSelector"] [value="progress"]').should('not.be.checked');
        cy.get('[data-cy="landingPageSelector"] [value="admin"]').should('be.checked');
        cy.get('[data-cy="userPrefsSettingsSave"]').should('not.be.disabled');
        cy.get('[data-cy="userPrefsSettingsSave"]').click();
        cy.wait('@saveUserInfo');
        cy.wait('@loadUserInfo');

        // click SkillTree logo and verify we are on the correct page
        cy.get('[data-cy="skillTreeLogo"]').click()
        cy.wait('@loadProjects');
        cy.get('[data-cy="breadcrumb-Projects"]').should('be.visible');

        // now update home page back to 'Progress and Rankings'
        cy.navToSettings()
        cy.get('[data-cy="nav-Preferences"]').click();
        cy.get('[data-cy="userPrefsSettingsSave"]').should('be.disabled');
        cy.get('[data-cy="landingPageSelector"] [value="progress"]').click({force:true})
        cy.get('[data-cy="landingPageSelector"] [value="admin"]').should('not.be.checked');
        cy.get('[data-cy="landingPageSelector"] [value="progress"]').should('be.checked');
        cy.get('[data-cy="userPrefsSettingsSave"]').should('not.be.disabled');
        cy.get('[data-cy="userPrefsSettingsSave"]').click();
        cy.wait('@saveUserInfo');
        cy.wait('@loadUserInfo');

        // click SkillTree logo and verify we are on the correct page
        cy.get('[data-cy="skillTreeLogo"]').click();
        cy.wait('@loadMyProgressSummary');
        cy.get('[data-cy="breadcrumb-Progress And Rankings"]').should('be.visible');

        // verify the unsaved changes alert is visible when values are changed
        // and not visible when they a the same as when loaded
        cy.navToSettings()
        cy.get('[data-cy="nav-Preferences"]').click();
        cy.get('[data-cy="unsavedChangesAlert"]').should('not.exist');
        cy.get('[data-cy="userPrefsSettingsSave"]').should('be.disabled');
        cy.get('[data-cy="landingPageSelector"] [value="admin"]').click({force:true})
        cy.get('[data-cy="landingPageSelector"] [value="progress"]').should('not.be.checked');
        cy.get('[data-cy="landingPageSelector"] [value="admin"]').should('be.checked');

        // unsaved changes visible and save button enabled
        cy.get('[data-cy="unsavedChangesAlert"]').should('be.visible');
        cy.get('[data-cy="userPrefsSettingsSave"]').should('not.be.disabled');

        // switch values back to original and unsaved changes should not visible and save button disabled
        cy.get('[data-cy="landingPageSelector"] [value="progress"]').click({force:true})
        cy.get('[data-cy="landingPageSelector"] [value="admin"]').should('not.be.checked');
        cy.get('[data-cy="landingPageSelector"] [value="progress"]').should('be.checked');
        cy.get('[data-cy="unsavedChangesAlert"]').should('not.exist');
        cy.get('[data-cy="userPrefsSettingsSave"]').should('be.disabled');
    })

    it('show links to docs', () => {
        cy.visit('/')
        cy.get('[data-cy="help-button"]').click();
        cy.contains('Official Docs');
    })

});

