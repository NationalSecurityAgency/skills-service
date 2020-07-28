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
        });
    });

    it('Add Root User', () => {
        cy.server();
        cy.route('POST', '/root/users/without/role/ROLE_SUPER_DUPER_USER?userSuggestOption=ONE').as('getEligibleForRoot');
        cy.route('PUT', '/root/users/skills@skills.org/roles/ROLE_SUPER_DUPER_USER').as('addRoot');
        cy.route({
            method: 'GET',
            url: '/app/projects'
        }).as('loadProjects');
        cy.route({method: 'GET', url: '/root/isRoot'}).as('checkRoot');

        cy.visit('/');
        cy.contains('My Projects');
        cy.get('button.dropdown-toggle').first().click({force: true});
        cy.contains('Settings').click();
        cy.wait('@checkRoot');
        cy.contains('Security').click();
        cy.contains('Enter user id').first().type('sk{enter}');
        cy.wait('@getEligibleForRoot');
        cy.contains('skills@skills.org').click();
        cy.contains('Add').first().click();
        cy.wait('@addRoot');
        cy.get('div.table-responsive').contains('Firstname LastName (skills@skills.org)');
    });

    it('Add Root User - forward slash character does not cause error', () => {
        cy.server();
        cy.route('POST', '/root/users/without/role/ROLE_SUPER_DUPER_USER?userSuggestOption=ONE').as('getEligibleForRoot');
        cy.route('PUT', '/root/users/skills@skills.org/roles/ROLE_SUPER_DUPER_USER').as('addRoot');
        cy.route('GET', '/app/projects').as('loadProjects');
        cy.route('GET', '/app/userInfo/hasRole/ROLE_SUPERVISOR').as('isSupervisor');
        cy.route('GET', '/app/userInfo').as('loadUserInfo');
        cy.route('GET', '/public/config').as('loadConfig');
        cy.route({
            method: 'GET',
            url: '/app/projects'
        }).as('loadProjects');
        cy.route({method: 'GET', url: '/root/isRoot'}).as('checkRoot');

        cy.visit('/');
        cy.wait('@loadConfig');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProjects');
        cy.wait('@isSupervisor');
        cy.contains('My Projects');
        cy.get('button.dropdown-toggle').first().click({force: true});
        cy.contains('Settings').click();
        cy.wait('@checkRoot');
        cy.contains('Security').click();
        cy.contains('Enter user id').first().type('sk/foo{enter}');
        cy.wait('@getEligibleForRoot');
    });


    it('Add Root User With No Query', () => {
        cy.server();
        cy.route('POST', '/root/users/without/role/ROLE_SUPER_DUPER_USER?userSuggestOption=ONE').as('getEligibleForRoot');
        cy.route('PUT', '/root/users/skills@skills.org/roles/ROLE_SUPER_DUPER_USER').as('addRoot');
        cy.route({
            method: 'GET',
            url: '/app/projects'
        }).as('loadProjects');
        cy.route({method: 'GET', url: '/root/isRoot'}).as('checkRoot');

        cy.visit('/');
        cy.get('button.dropdown-toggle').first().click({force: true});
        cy.contains('Settings').click();
        cy.wait('@checkRoot');
        cy.contains('Security').click();
        cy.contains('Enter user id').first().type('{enter}');
        cy.wait('@getEligibleForRoot');
        cy.contains('skills@skills.org').click();
        cy.contains('Add').first().click();
        cy.wait('@addRoot');
        cy.get('div.table-responsive').contains('Firstname LastName (skills@skills.org)');
    });

    it('Add Supervisor User', () => {
        cy.server();
        cy.route('PUT', '/root/users/root@skills.org/roles/ROLE_SUPERVISOR').as('addSupervisor');
        cy.route('POST', 'root/users/without/role/ROLE_SUPERVISOR?userSuggestOption=ONE').as('getEligibleForSupervisor');
        cy.route({
            method: 'GET',
            url: '/app/projects'
        }).as('loadProjects');
        cy.route({method: 'GET', url: '/root/isRoot'}).as('checkRoot');

        cy.visit('/');
        cy.contains('My Projects');

        cy.get('li').contains('Badges').should('not.exist');
        cy.vuex().its('state.access.isSupervisor').should('equal', false);
        cy.get('button.dropdown-toggle').first().click({force: true});
        cy.contains('Settings').click();
        cy.wait('@checkRoot');
        cy.contains('Security').click();
        cy.get('[data-cy=supervisorrm]  div.multiselect__tags').type('root');
        cy.wait('@getEligibleForSupervisor');
        cy.get('[data-cy=supervisorrm]').contains('root@skills.org').click();
        cy.get('[data-cy=supervisorrm]').contains('Add').click();
        cy.wait('@addSupervisor');
        cy.get('div.table-responsive').contains('Firstname LastName (root@skills.org)');
        cy.vuex().its('state.access.isSupervisor').should('equal', true);
        cy.contains('Home').click();
        cy.get('[data-cy=navigationmenu]').contains('Badges', {timeout: 5000}).should('be.visible');
    });

    it('Add Supervisor User Not Found', () => {
        cy.server();
        // cy.route('PUT', '/root/users/root@skills.org/roles/ROLE_SUPERVISOR').as('addSupervisor');
        cy.route('POST', 'root/users/without/role/ROLE_SUPERVISOR?userSuggestOption=ONE', [{"userId":"blah@skills.org","userIdForDisplay":"blah@skills.org","first":"Firstname","last":"LastName","nickname":"Firstname LastName","dn":null}]).as('getEligibleForSupervisor');
        cy.route({
            method: 'GET',
            url: '/app/projects'
        }).as('loadProjects');
        cy.route({method: 'GET', url: '/root/isRoot'}).as('checkRoot');

        cy.visit('/');
        cy.contains('My Projects');

        cy.get('li').contains('Badges').should('not.exist');
        cy.vuex().its('state.access.isSupervisor').should('equal', false);
        cy.get('button.dropdown-toggle').first().click({force: true});
        cy.contains('Settings').click();
        cy.wait('@checkRoot');
        cy.contains('Security').click();
        cy.get('[data-cy=supervisorrm]  div.multiselect__tags').type('root');
        cy.wait('@getEligibleForSupervisor');
        cy.get('[data-cy=supervisorrm]').contains('blah@skills.org').click();
        cy.get('[data-cy=supervisorrm]').contains('Add').click();
        cy.get('[data-cy=error-msg]').contains('Error! Request could not be completed! User [blah@skills.org] does not exist')
        cy.vuex().its('state.access.isSupervisor').should('equal', false);
    });

    it('Add Supervisor User No Query', () => {

        cy.server();
        cy.route('PUT', '/root/users/root@skills.org/roles/ROLE_SUPERVISOR').as('addSupervisor');
        cy.route('POST', 'root/users/without/role/ROLE_SUPERVISOR?userSuggestOption=ONE').as('getEligibleForSupervisor');
        cy.route({
            method: 'GET',
            url: '/app/projects'
        }).as('loadProjects');

        cy.route({method: 'GET', url: '/root/isRoot'}).as('checkRoot');

        cy.visit('/');

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
        cy.server();
        cy.route('GET', '/root/getEmailSettings').as('loadEmailSettings');
        cy.route('GET', '/app/userInfo').as('loadUserInfo');
        cy.visit('/');
        cy.wait('@loadUserInfo');
        cy.get('.userName').parent().click();
        cy.contains('Settings').click();
        cy.contains('Email').click();
        cy.wait('@loadEmailSettings');
        cy.get$('[data-cy=hostInput]').type('{selectall}localhost');
        cy.get$('[data-cy=portInput]').type('{selectall}1026');
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
        cy.server();
        cy.route('GET', '/root/getEmailSettings').as('loadEmailSettings');
        cy.route('GET', '/app/userInfo').as('loadUserInfo');
        cy.visit('/');
        cy.wait('@loadUserInfo');
        cy.get('.userName').parent().click();
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
        cy.server();
        cy.route('GET', '/root/getSystemSettings').as('loadSystemSettings');
        cy.route('GET', '/app/userInfo').as('loadUserInfo');
        cy.visit('/');
        cy.wait('@loadUserInfo');
        cy.get('.userName').parent().click();
        cy.contains('Settings').click();
        cy.contains('System').click();

        cy.wait('@loadSystemSettings');
        cy.get('[data-cy=resetTokenExpiration]').should('have.value', '2H');
        cy.get$('[data-cy=publicUrl]').type('{selectall}http://localhost:8082');
        cy.get$('[data-cy=resetTokenExpiration]').type('{selectall}2H25M22S');
        cy.get$('[data-cy=saveSystemSettings]').click();
        cy.visit('/settings/system');
        cy.wait('@loadSystemSettings');
        cy.get('[data-cy=publicUrl]').should('have.value', 'http://localhost:8082');
        cy.get('[data-cy=resetTokenExpiration]').should('have.value', '2H25M22S');
    });

});
