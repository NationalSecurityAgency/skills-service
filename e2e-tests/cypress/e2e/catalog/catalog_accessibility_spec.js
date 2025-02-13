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

describe('Catalog Accessibility Tests', () => {

    beforeEach(() => {
        cy.createProject(1);
        cy.createSubject(1, 1);
    });

    it('return focus to the import button - cancel import', () => {
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1);
        cy.exportSkillToCatalog(2, 1, 1);

        cy.visit('/administrator/projects/proj1/subjects/subj1/');
        cy.get('[data-cy=importFromCatalogBtn]')
            .should('not.have.focus');
        cy.get('[data-cy="importFromCatalogBtn"]')
            .click();
        cy.get('[data-cy="closeButton"]')
            .click();
        cy.get('[data-cy=importFromCatalogBtn]')
            .should('have.focus');
    });

    it('return focus to the import button - after import', () => {
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1);
        cy.createSkill(2, 1, 2);
        cy.createSkill(2, 1, 3);
        cy.createSkill(2, 1, 4);
        cy.createSkill(2, 1, 5);

        cy.exportSkillToCatalog(2, 1, 1);
        cy.exportSkillToCatalog(2, 1, 2);
        cy.exportSkillToCatalog(2, 1, 3);
        cy.exportSkillToCatalog(2, 1, 4);
        cy.exportSkillToCatalog(2, 1, 5);

        cy.visit('/administrator/projects/proj1/subjects/subj1/');
        cy.get('[data-cy=importFromCatalogBtn]')
            .should('not.have.focus');
        cy.get('[data-cy="importFromCatalogBtn"]')
            .click();

        cy.get('[data-cy="importSkillsFromCatalogTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="importBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="importBtn"]')
            .click();
        cy.get('[data-cy=importFromCatalogBtn]')
            .should('have.focus');
    });

    it('set focus to the Select All button after export is done', () => {
        cy.createSkill(1, 1, 1);

        cy.visit('/administrator/projects/proj1/subjects/subj1/');

        cy.get('[data-cy="newSkillButton"]')
            .should('not.have.focus');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="skillActionsNumSelected"]')
            .should('have.text', '1');

        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Export To Catalog"]').click()

        cy.contains('This will export [Very Great Skill 1] Skill');

        cy.get('[data-cy="exportToCatalogButton"]')
            .click();
        cy.get('[data-cy="okButton"]')
            .click();
        cy.get(`[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"] input`).should('not.be.checked')
        cy.get('[data-cy="skillActionsBtn"]')
            .should('be.disabled');
        cy.get('[data-cy="skillActionsNumSelected"]')
            .should('have.text', '0');
        cy.get('[data-cy=newSkillButton]')
            .should('have.focus');
    });

    it('set focus to the Clear button after export modal is cancelled', () => {
        cy.createSkill(1, 1, 1);

        cy.visit('/administrator/projects/proj1/subjects/subj1/');

        cy.get('[data-cy=skillActionsBtn]')
            .should('not.have.focus');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get(`[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"] input`).should('be.checked')
        cy.get('[data-cy="skillActionsBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="skillActionsNumSelected"]')
            .should('have.text', '1');

        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Export To Catalog"]').click()

        cy.contains('This will export [Very Great Skill 1] Skill');
        cy.get('[data-cy="closeButton"]')
            .click();
        cy.get(`[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"] input`).should('be.checked')
        cy.get('[data-cy="skillActionsBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="skillActionsNumSelected"]')
            .should('have.text', '1');
        cy.get('[data-cy=skillActionsBtn]')
            .should('have.focus');
    });

    it('set focus to the Select All button after export is done even if modal is closed with X', () => {
        cy.createSkill(1, 1, 1);

        cy.visit('/administrator/projects/proj1/subjects/subj1/');

        cy.get('[data-cy=newSkillButton]')
            .should('not.have.focus');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get(`[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"] input`).should('be.checked')
        cy.get('[data-cy="skillActionsBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="skillActionsNumSelected"]')
            .should('have.text', '1');

        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Export To Catalog"]').click();

        cy.contains('This will export [Very Great Skill 1] Skill');

        cy.get('[data-cy="exportToCatalogButton"]')
            .click();
        cy.get('[aria-label="Close"]')
            .click();
        cy.get(`[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"] input`).should('not.be.checked')
        cy.get('[data-cy="skillActionsBtn"]')
            .should('be.disabled');
        cy.get('[data-cy="skillActionsNumSelected"]')
            .should('have.text', '0');
        cy.get('[data-cy=newSkillButton]')
            .should('have.focus');
    });

});