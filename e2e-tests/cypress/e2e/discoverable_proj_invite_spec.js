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
describe('Copy Invite URL Tests', () => {

    beforeEach(() => {
        cy.createProject(1);
        cy.createProject(2);
        cy.createProject(3);
        cy.enableProdMode(1);
        cy.enableProdMode(2);
    })

    const proj1Url = '/progress-and-rankings/projects/proj1'
    const proj1Share = `${proj1Url}?invited=true`

    it('retrieve share url', { browser: 'chrome' }, () => {
        // this is needed to grant headless chrome permissions to copy-and-paste
        cy.wrap(Cypress.automation('remote:debugger:protocol', {
            command: 'Browser.grantPermissions',
            params: {
                permissions: ['clipboardReadWrite', 'clipboardSanitizedWrite'],
                origin: window.location.origin,
            },
        }))

        cy.visit('/administrator/projects/proj1')
        cy.contains('No Subjects Yet')
        cy.get('[data-cy="projectLastReportedSkillValue"]').should('have.text', 'Never')
        cy.wait(1500)
        cy.location().then((loc) => {
            const expectedProj1URL = `${loc.origin}${proj1Share}`
            const expectedProj2URL = `${loc.origin}/progress-and-rankings/projects/proj2?invited=true`

            // by default cypress uses simulated events that are initiated from JS; these simulated events are
            // not trusted for "copy to clipboard" operations - must use "real events"
            cy.get('[data-cy="shareProjBtn"]')
                .realClick()

            cy.get('[data-cy="projShareUrl"]')
                .should('have.text', expectedProj1URL)
            cy.get('[data-cy="closeDialogBtn"]')
                .click();

            cy.get('[data-cy="projShareUrl"]')
                .should('not.exist')
            cy.window().its('navigator.clipboard')
                .then((clip) => clip.readText())
                .should('equal', expectedProj1URL)

            cy.get('[data-cy="breadcrumb-Projects"]')
                .click()
            cy.get('[data-cy="projCard_proj2_manageBtn"]')
                .click()
            cy.contains('No Subjects Yet')
            cy.get('[data-cy="projectLastReportedSkillValue"]').should('have.text', 'Never')
            cy.wait(1500)

            cy.get('[data-cy="shareProjBtn"]')
                .realClick();
            cy.contains('URL was copied!')
            cy.get('[data-cy="projShareUrl"]')
                .should('have.text', expectedProj2URL)

            cy.get('[data-cy="closeDialogBtn"]')
                .click();
            cy.get('[data-cy="projShareUrl"]')
                .should('not.exist')
            cy.window().its('navigator.clipboard')
                .then((clip) => clip.readText())
                .should('equal', expectedProj2URL)
        });
    });

    it('copy value from the share modal', () => {
        // this is needed to grant headless chrome permissions to copy-and-paste
        cy.wrap(Cypress.automation('remote:debugger:protocol', {
            command: 'Browser.grantPermissions',
            params: {
                permissions: ['clipboardReadWrite', 'clipboardSanitizedWrite'],
                origin: window.location.origin,
            },
        }))

        cy.visit('/administrator/projects/proj1')
        cy.contains('No Subjects Yet')
        cy.location().then((loc) => {
            const expectedProj1URL = `${loc.origin}${proj1Share}`

            // by default cypress uses simulated events that are initiated from JS; these simulated events are
            // not trusted for "copy to clipboard" operations - must use "real events"
            cy.get('[data-cy="shareProjBtn"]')
                .realClick()

            cy.get('[data-cy="projShareUrl"]')
                .should('have.text', expectedProj1URL)
            cy.window().its('navigator.clipboard')
                .then((clip) => clip.readText())
                .should('equal', expectedProj1URL)

            // override copied value
            cy.window()
                .its('navigator.clipboard')
                .invoke('writeText', 'blah')
            cy.window().its('navigator.clipboard')
                .then((clip) => clip.readText())
                .should('equal', 'blah')

            cy.get('[data-cy="copySharedUrl"]').click();
            cy.window().its('navigator.clipboard')
                .then((clip) => clip.readText())
                .should('equal', expectedProj1URL)
        });
    });

    it('share button does not exist for non-discoverable projects', () => {
        cy.visit('/administrator/projects/proj3')
        cy.contains('No Subjects Yet')

        cy.get('[data-cy="shareProjBtn"]').should('not.exist')
    });

    it('focus is returned to the share button', () => {
        cy.visit('/administrator/projects/proj1')
        cy.contains('No Subjects Yet')

        cy.get('[data-cy="shareProjBtn"]').realClick()
        cy.get('[data-cy="closeDialogBtn"]').click();
        cy.get('[data-cy="shareProjBtn"]').should('have.focus')

        cy.get('[data-cy="shareProjBtn"]').realClick()
        cy.get('[data-cy="closeDialogBtn"]').type('{esc}');
        cy.get('[data-cy="shareProjBtn"]').should('have.focus')

        cy.get('[data-cy="shareProjBtn"]').realClick()
        cy.get('[data-pc-section="closebuttonicon"]').click()
        cy.get('[data-cy="shareProjBtn"]').should('have.focus')
    });

    it('shared url adds project to My Projects', ()  => {
       cy.visit(proj1Share);
       cy.get('[data-cy="skillsDisplayHome"]').contains('My Level');
       cy.get('[data-cy="breadcrumb-Progress And Rankings"]').click()
       cy.get('[data-cy="project-card-project-name"]').should('exist')
    });

    it('regular project url does NOT add project to My Projects', ()  => {
        cy.visit(proj1Url);
        cy.get('[data-cy="skillsDisplayHome"]').contains('My Level');
        cy.get('[data-cy="breadcrumb-Progress And Rankings"]').click()
        cy.get('[data-cy="project-card-project-name"]').should('not.exist')
    });

});

