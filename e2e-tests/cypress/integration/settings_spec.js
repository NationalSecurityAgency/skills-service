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
    })

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

    it('Add Supervisor User', () => {

        cy.visit('/');
        cy.get('li').contains('Badges').should('not.exist');
        cy.get('button.dropdown-toggle').first().click({force: true});
        cy.contains('Settings').click();
        cy.contains('Security').click();
        cy.get('[data-cy=supervisorrm]  div.multiselect__tags').type('root');
        cy.wait(500);
        cy.get('[data-cy=supervisorrm]').contains('root@skills.org').click();
        cy.get('[data-cy=supervisorrm]').contains('Add').click();
        cy.get('div.table-responsive').contains('Firstname LastName (root@skills.org)');
        cy.wait(2500);
        cy.contains('Home').click();
        cy.contains('Badges').should('be.visible');
    });
});
