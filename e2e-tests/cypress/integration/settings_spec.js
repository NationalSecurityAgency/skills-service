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

    const getStore =  () => cy.window().its('vm.$store');

    it('Add Root User', () => {
        cy.visit('/');
        cy.get('button.dropdown-toggle').first().click({force: true});
        cy.contains('Settings').click();
        cy.contains('Security').click();
        cy.contains('Enter user id').first().type('sk{enter}');
        cy.contains('skills@skills.org').click();
        cy.contains('Add').first().click();
        cy.get('div.table-responsive').contains('Firstname LastName (skills@skills.org)');
    });

    it.only('Add Supervisor User', () => {
        cy.visit('/');
        cy.server();
        cy.route('PUT', '/root/users/root@skills.org/roles/ROLE_SUPERVISOR').as('addSupervisor');

        cy.get('li').contains('Badges').should('not.exist');
        getStore().its('state.access.isSupervisor').should('equal', false);
        cy.get('button.dropdown-toggle').first().click({force: true});
        cy.contains('Settings').click();
        cy.contains('Security').click();
        cy.get('[data-cy=supervisorrm]  div.multiselect__tags').type('root');
        cy.get('[data-cy=supervisorrm]').contains('root@skills.org').click();
        cy.get('[data-cy=supervisorrm]').contains('Add').click();
        cy.wait('@addSupervisor');
        cy.get('div.table-responsive').contains('Firstname LastName (root@skills.org)');
        getStore().its('state.access.isSupervisor').should('equal', true);
        cy.contains('Home').click();
        cy.get('[data-cy=navigationmenu]').contains('Badges', {timeout: 5000}).should('be.visible');
    });
});
