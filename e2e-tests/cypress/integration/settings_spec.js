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


    })
});
