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

describe('Community Projects Tests', () => {

    const allDragonsUser = 'allDragons@email.org'

    before(() => {
        cy.beforeTestSuiteThatReusesData()
        cy.fixture('vars.json').then((vars) => {
            cy.logout();
            cy.login(vars.rootUser, vars.defaultPass, true);
            cy.request('POST', `/root/users/${vars.rootUser}/tags/dragons`, { tags: ['DivineDragon'] });
            cy.request('POST', `/root/users/${vars.defaultUser}/tags/dragons`, { tags: ['DivineDragon'] });
            cy.logout();

            cy.register(allDragonsUser, vars.defaultPass);
            cy.logout();
        });
    });

    after(() => {
        cy.afterTestSuiteThatReusesData()
    });

    it('create restricted community project', () => {
        cy.visit('/administrator')
        cy.get('[data-cy="newProjectButton"]').click()
        cy.get('[data-cy="projectName"]').type('one')
        cy.get('[data-cy="saveProjectButton"]').should('be.enabled')

        cy.get('[data-cy="restrictCommunityControls"]').contains('Access to Divine Dragon users only')
        const warningMsg = 'Please note that once the restriction is enabled it cannot be lifted/disabled';
        cy.get('[data-cy="restrictCommunityControls"]').contains(warningMsg).should('not.exist')
        cy.get('[data-cy="restrictCommunity"]').click({force: true})
        cy.get('[data-cy="restrictCommunityControls"]').contains(warningMsg)

        cy.get('[data-cy="saveProjectButton"]').click()
        cy.get('[data-cy="projectCard_one"] [data-cy="userCommunity"]').contains('For Divine Dragon Nation')
    });

});
