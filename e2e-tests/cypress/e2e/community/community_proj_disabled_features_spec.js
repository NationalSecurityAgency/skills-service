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

describe('Community Project Creation Tests', () => {

    const allDragonsUser = 'allDragons@email.org'

    beforeEach(() => {
        cy.fixture('vars.json').then((vars) => {
            cy.logout();
            cy.login(vars.rootUser, vars.defaultPass, true);
            cy.request('POST', `/root/users/${vars.rootUser}/tags/dragons`, { tags: ['DivineDragon'] });
            cy.request('POST', `/root/users/${vars.defaultUser}/tags/dragons`, { tags: ['DivineDragon'] });
            cy.logout();

            cy.register(allDragonsUser, vars.defaultPass);
            cy.logout();

            cy.login(vars.defaultUser, vars.defaultPass);
        });
    });

    it('community projected project cannot export skills to the catalog', () => {
        cy.createProject(1, {enableProtectedUserCommunity: true})
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)

        cy.visit('/administrator/projects/proj1/subjects/subj1')

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="rowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').should('be.enabled');
        cy.get('[data-cy="skillActionsNumSelected"]').should('have.text', '1');

        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Export To Catalog"]').click()

        cy.get('[data-cy="userCommunityRestrictedWarning"]').contains('restricted to Divine Dragon')
        cy.get('[data-cy="exportToCatalogButton"]').should('not.exist')
    });

    it('community projected project cannot share skills for cross-project dependencies', () => {
        cy.createProject(1, {enableProtectedUserCommunity: true})

        cy.visit('/administrator/projects/proj1/learning-path')
        cy.get('[data-cy="restrictedUserCommunityWarning"]').contains('is restricted to Divine Dragon')
        cy.get('[data-cy="shareButton"]').should('not.exist')
    });

    it('protected projects cannot be disabled during copy', () => {
        cy.createProject(1, {enableProtectedUserCommunity: true})

        cy.visit('/administrator/')
        cy.get('[data-cy="copyProjBtn"]').click()
        cy.get('[data-cy="restrictCommunityControls"]').contains('Copying a project whose access is restricted to Divine Dragon')
        cy.get('[data-cy="projectName"]').type('copy')
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="allDoneBtn"]').click()
        cy.get('[data-cy="projectCard_copy"] [data-cy="userCommunity"]').contains('For Divine Dragon Nation')
    });

    it('copy a non-protected project and make it protected during copy', () => {
        cy.createProject(1, {enableProtectedUserCommunity: false})

        cy.visit('/administrator/')
        cy.get('[data-cy="copyProjBtn"]').click()
        cy.get('[data-cy="projectName"]').type('copy')

        cy.get('[data-cy="restrictCommunityControls"]').contains('Access to Divine Dragon users only')
        const warningMsg = 'Please note that once the restriction is enabled it cannot be lifted/disabled';
        cy.get('[data-cy="restrictCommunityControls"]').contains(warningMsg).should('not.exist')
        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click()
        cy.get('[data-cy="restrictCommunityControls"]').contains(warningMsg)

        cy.intercept('POST', '/api/validation/description*').as('validateDescription');
        cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\njabberwocky');
        cy.wait('@validateDescription');
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="allDoneBtn"]').click()
        cy.get('[data-cy="projectCard_copy"] [data-cy="userCommunity"]').contains('For Divine Dragon Nation')

        cy.reload()
        cy.get('[data-cy="projectCard_copy"] [data-cy="userCommunity"]').contains('For Divine Dragon Nation')
    });

});
