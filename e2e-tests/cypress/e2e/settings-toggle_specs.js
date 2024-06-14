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
 describe('Settings Toggle Specs', () => {

     beforeEach(() => {
         cy.request('POST', '/app/projects/proj1', {
             projectId: 'proj1',
             name: 'proj1'
         });
     });


    it.only('Verify that the value produces the expected label', () => {
        cy.visit('/administrator/projects/proj1/');

        cy.clickNav('Settings');

        cy.get('[data-cy="rankAndLeaderboardOptOutSwitch"]').should('not.be.checked')
        cy.get('[data-cy="rankAndLeaderboardOptOutSwitch"]').check({ force: true });
        cy.get('[data-cy="saveSettingsBtn"]').click();
        cy.get('[data-cy="rankAndLeaderboardOptOutSwitch"]').next().should('have.text', ' Enabled ')
        cy.get('[data-cy="rankAndLeaderboardOptOutSwitch"]').should('be.checked')

        cy.clickNav('Levels');
        cy.clickNav('Settings');

        cy.get('[data-cy="rankAndLeaderboardOptOutSwitch"]').should('be.checked')
        cy.get('[data-cy="rankAndLeaderboardOptOutSwitch"]').next().should('have.text', ' Enabled ')
        cy.get('[data-cy="rankAndLeaderboardOptOutSwitch"]').uncheck({ force: true });
        cy.get('[data-cy="saveSettingsBtn"]').click();

        cy.get('[data-cy="rankAndLeaderboardOptOutSwitch"]').next().should('have.text', ' Disabled ')
        cy.get('[data-cy="rankAndLeaderboardOptOutSwitch"]').should('not.be.checked')

        cy.clickNav('Levels');
        cy.clickNav('Settings');

        cy.get('[data-cy="rankAndLeaderboardOptOutSwitch"]').next().should('have.text', ' Disabled ')
        cy.get('[data-cy="rankAndLeaderboardOptOutSwitch"]').should('not.be.checked')
    })
})