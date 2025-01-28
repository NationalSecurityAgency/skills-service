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
var moment = require('moment-timezone');

describe('Export Skills to the Catalog Tests', () => {

    beforeEach(() => {
        // bootstrap-vue tooltip in a table warns erroneously
        Cypress.env('ignoreConsoleWarnings', true);

        cy.createProject(1);
        cy.createSubject(1, 1);
    });
    const tableSelector = '[data-cy="skillsTable"]';

    it('export 1 skill', () => {
        window.localStorage.setItem('tableState', JSON.stringify({'skillsTable': {'sortDesc': true, 'sortBy': 'displayOrder'}}))
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="skillActionsBtn"]')
            .should('be.disabled');
        cy.get('[data-cy="skillActionsNumSelected"]')
            .should('have.text', '0');
        cy.get('[data-cy="skillsTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="skillActionsNumSelected"]')
            .should('have.text', '1');

        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Export To Catalog"]').click()

        cy.contains('This will export [Very Great Skill 1] Skill to the SkillTree Catalog');
        cy.get('[data-cy="exportToCatalogButton"]')
            .click();
        cy.contains('Skill [Very Great Skill 1] was successfully exported to the catalog!');
        cy.get('[data-cy="exportToCatalogButton"]')
            .should('not.exist');
        cy.get('[data-cy="closeButton"]')
            .should('not.exist');
        cy.get('[data-cy="okButton"]')
            .should('be.enabled');
        cy.get('[data-cy="checkingCatalogStatus"]')
            .should('not.exist')
        cy.get('[data-cy="okButton"]')
            .click();

        cy.get('[data-cy="skillActionsNumSelected"]')
            .should('have.text', '0');
        cy.get('[data-cy="exportedBadge-skill1"');
        cy.get('[data-cy="exportedBadge-skill2"')
            .should('not.exist');

        cy.get('[data-cy="skillActionsBtn"]')
            .should('be.disabled');
        cy.get('[data-cy="skillsTable-additionalColumns"] [data-pc-section="dropdownicon"]').click()
        cy.get('[data-pc-section="overlay"] [aria-label="Catalog"]').click()
        cy.get('[data-pc-section="closebutton"]').click()

        cy.validateTable(tableSelector, [
            [{
                colIndex: 5,
                value: 'N/A'
            }],
            [{
                colIndex: 5,
                value: 'Exported'
            }],
        ], 5, false, null, false);
        cy.get(`${tableSelector} tbody tr`)
            .should('have.length', 2);

        // refresh and re-validate
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="exportedBadge-skill1"');
        cy.get('[data-cy="exportedBadge-skill2"')
            .should('not.exist');
        cy.get('[data-cy="skillActionsBtn"]')
            .should('be.disabled');
        cy.get('[data-cy="skillsTable-additionalColumns"]')
            .contains('Catalog')
            .click();
        cy.get('[data-cy="skillActionsNumSelected"]')
            .should('have.text', '0');
        cy.validateTable(tableSelector, [
            [{
                colIndex: 5,
                value: 'N/A'
            }],
            [{
                colIndex: 5,
                value: 'Exported'
            }],
        ], 5, false, null, false);
        cy.get(`${tableSelector} tbody tr`)
            .should('have.length', 2);
    });

    it('export multiple skill', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);
        cy.createSkill(1, 1, 4);
        cy.createSkill(1, 1, 5);

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="skillActionsBtn"]')
            .should('be.disabled');
        cy.get('[data-cy="skillActionsNumSelected"]')
            .should('have.text', '0');
        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="4"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="skillActionsNumSelected"]')
            .should('have.text', '3');

        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Export To Catalog"]').click()

        cy.contains('This will export 3 Skills to the SkillTree Catalog');
        cy.get('[data-cy="exportToCatalogButton"]')
            .click();
        cy.contains('3 Skills were successfully exported to the catalog!');

        cy.get('[data-cy="exportToCatalogButton"]')
            .should('not.exist');
        cy.get('[data-cy="closeButton"]')
            .should('not.exist');
        cy.get('[data-cy="okButton"]')
            .should('be.enabled');
        cy.get('[data-cy="checkingCatalogStatus"]')
            .should('not.exist')
        cy.get('[data-cy="okButton"]')
            .click();

        cy.get('[data-cy="skillActionsNumSelected"]')
            .should('have.text', '0');
        for (let i= 0; i < 5 ; i++) {
            cy.get(`[data-cy="skillsTable"] [data-p-index="${i}"] [data-pc-name="pcrowcheckbox"] input`).should('not.be.checked')
        }
        cy.get('[data-cy="exportedBadge-skill1"');
        cy.get('[data-cy="exportedBadge-skill2"')
            .should('not.exist');
        cy.get('[data-cy="exportedBadge-skill3"');
        cy.get('[data-cy="exportedBadge-skill4"')
            .should('not.exist');
        cy.get('[data-cy="exportedBadge-skill5"');

        // refresh and re-validate
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="skillActionsNumSelected"]')
            .should('have.text', '0');
        cy.get('[data-cy="exportedBadge-skill1"');
        cy.get('[data-cy="exportedBadge-skill2"')
            .should('not.exist');
        cy.get('[data-cy="exportedBadge-skill3"');
        cy.get('[data-cy="exportedBadge-skill4"')
            .should('not.exist');
        cy.get('[data-cy="exportedBadge-skill5"');
        for (let i= 0; i < 5 ; i++) {
            cy.get(`[data-cy="skillsTable"] [data-p-index="${i}"] [data-pc-name="pcrowcheckbox"] input`).should('not.be.checked')
        }
    });

    it('export all skills', () => {
        window.localStorage.setItem('tableState', JSON.stringify({'skillsTable': {'sortDesc': false, 'sortBy': 'name'}}))
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);
        cy.createSkill(1, 1, 4);
        cy.createSkill(1, 1, 5);
        cy.createSkill(1, 1, 6);
        cy.createSkill(1, 1, 7);
        cy.createSkill(1, 1, 8);
        cy.createSkill(1, 1, 9);
        cy.createSkill(1, 1, 10);
        cy.createSkill(1, 1, 11);
        cy.createSkill(1, 1, 12);

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="skillActionsBtn"]')
            .should('be.disabled');
        cy.get('[data-cy="skillActionsNumSelected"]')
            .should('have.text', '0');
        cy.get('[data-cy="skillsTable"] [data-pc-name="pcheadercheckbox"] [data-pc-section="input"]').click();
        cy.get('[data-cy="skillActionsBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="skillActionsNumSelected"]')
            .should('have.text', '12');

        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Export To Catalog"]').click()

        cy.contains('This will export 12 Skills to the SkillTree Catalog');
        cy.get('[data-cy="exportToCatalogButton"]')
            .click();
        cy.contains('12 Skills were successfully exported to the catalog!');

        cy.get('[data-cy="exportToCatalogButton"]')
            .should('not.exist');
        cy.get('[data-cy="closeButton"]')
            .should('not.exist');
        cy.get('[data-cy="okButton"]')
            .should('be.enabled');
        cy.get('[data-cy="checkingCatalogStatus"]')
            .should('not.exist')
        cy.get('[data-cy="okButton"]')
            .click();

        cy.get('[data-cy="skillActionsBtn"]')
            .should('be.disabled');
        cy.get('[data-cy="skillActionsNumSelected"]')
            .should('have.text', '0');

        cy.get('[data-cy="exportedBadge-skill3"');
        cy.get('[data-cy="exportedBadge-skill4"');
        cy.get('[data-cy="exportedBadge-skill5"');
        cy.get('[data-cy="exportedBadge-skill6"');
        cy.get('[data-cy="exportedBadge-skill7"');
        cy.get('[data-cy="exportedBadge-skill8"');
        cy.get('[data-cy="exportedBadge-skill9"');
        cy.get('[data-cy="exportedBadge-skill10"');
        cy.get('[data-cy="exportedBadge-skill11"');
        cy.get('[data-cy="exportedBadge-skill12"');

        cy.get('[data-cy="skillsTable"] [aria-label="Page 2"]').click()
        cy.get('[data-cy="exportedBadge-skill1"');
        cy.get('[data-cy="exportedBadge-skill2"');

        // refresh and re-validate
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="skillActionsBtn"]')
            .should('be.disabled');

        cy.get('[data-cy="exportedBadge-skill3"');
        cy.get('[data-cy="exportedBadge-skill4"');
        cy.get('[data-cy="exportedBadge-skill5"');
        cy.get('[data-cy="exportedBadge-skill6"');
        cy.get('[data-cy="exportedBadge-skill7"');
        cy.get('[data-cy="exportedBadge-skill8"');
        cy.get('[data-cy="exportedBadge-skill9"');
        cy.get('[data-cy="exportedBadge-skill10"');
        cy.get('[data-cy="exportedBadge-skill11"');
        cy.get('[data-cy="exportedBadge-skill12"');

        cy.get('[data-cy="skillsTable"] [aria-label="Page 2"]').click()
        cy.get('[data-cy="exportedBadge-skill1"');
        cy.get('[data-cy="exportedBadge-skill2"');

    });

    it('export all skills ignores groups', () => {
        window.localStorage.setItem('tableState', JSON.stringify({'skillsTable': {'sortDesc': false, 'sortBy': 'displayOrder'}}))
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);
        cy.createSkill(1, 1, 4);
        cy.createSkill(1, 1, 5);
        cy.createSkillsGroup(1, 1, 13);
        cy.createSkill(1, 1, 6);
        cy.createSkill(1, 1, 7);
        cy.createSkill(1, 1, 8);
        cy.createSkill(1, 1, 9);
        cy.createSkill(1, 1, 10);
        cy.createSkill(1, 1, 11);
        cy.createSkill(1, 1, 12);
        cy.createSkillsGroup(1, 1, 14);

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="skillActionsBtn"]')
            .should('be.disabled');
        cy.get('[data-cy="skillActionsNumSelected"]')
            .should('have.text', '0');
        cy.get('[data-cy="skillsTable"] [data-pc-name="pcheadercheckbox"] [data-pc-section="input"]').click();
        cy.get('[data-cy="skillActionsBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="skillActionsNumSelected"]')
            .should('have.text', '12');

        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Export To Catalog"]').click()

        cy.contains('This will export 12 Skills to the SkillTree Catalog');
        cy.get('[data-cy="exportToCatalogButton"]')
            .click();
        cy.contains('12 Skills were successfully exported to the catalog!');

        cy.get('[data-cy="exportToCatalogButton"]')
            .should('not.exist');
        cy.get('[data-cy="closeButton"]')
            .should('not.exist');
        cy.get('[data-cy="okButton"]')
            .should('be.enabled');
        cy.get('[data-cy="checkingCatalogStatus"]')
            .should('not.exist')
        cy.get('[data-cy="okButton"]')
            .click();

        cy.get('[data-cy="skillActionsBtn"]')
            .should('be.disabled');
        cy.get('[data-cy="skillActionsNumSelected"]')
            .should('have.text', '0');

        cy.get('[data-cy="skillsTable"] [data-pc-name="pcrowperpagedropdown"]').click()
        cy.get('[data-pc-section="list"] [aria-label="20"]').click()

        cy.get('[data-cy="exportedBadge-skill1"');
        cy.get('[data-cy="exportedBadge-skill2"');
        cy.get('[data-cy="exportedBadge-skill3"');
        cy.get('[data-cy="exportedBadge-skill4"');
        cy.get('[data-cy="exportedBadge-skill5"');
        cy.get('[data-cy="exportedBadge-skill6"');
        cy.get('[data-cy="exportedBadge-skill7"');
        cy.get('[data-cy="exportedBadge-skill8"');
        cy.get('[data-cy="exportedBadge-skill9"');
        cy.get('[data-cy="exportedBadge-skill13"')
            .should('not.exist');
        cy.get('[data-cy="exportedBadge-skill14"')
            .should('not.exist');
        cy.get('[data-cy="exportedBadge-skill10"');
        cy.get('[data-cy="exportedBadge-skill11"');
        cy.get('[data-cy="exportedBadge-skill12"');
    });

    it('try to export skills that are already exported', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);
        cy.createSkill(1, 1, 4);
        cy.createSkill(1, 1, 5);

        cy.exportSkillToCatalog(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 3);
        cy.exportSkillToCatalog(1, 1, 5);

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="exportedBadge-skill1"');
        cy.get('[data-cy="exportedBadge-skill2"')
            .should('not.exist');
        cy.get('[data-cy="exportedBadge-skill3"');
        cy.get('[data-cy="exportedBadge-skill4"')
            .should('not.exist');
        cy.get('[data-cy="exportedBadge-skill5"');

        cy.get('[data-cy="skillActionsBtn"]')
            .should('be.disabled');
        cy.get('[data-cy="skillActionsNumSelected"]')
            .should('have.text', '0');
        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="4"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="skillActionsNumSelected"]')
            .should('have.text', '3');

        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Export To Catalog"]').click()

        cy.contains('All selected 3 skill(s) are already in the Skill Catalog.');
        cy.get('[data-cy="exportToCatalogButton"]')
            .should('not.exist');
        cy.get('[data-cy="closeButton"]')
            .should('not.exist');
        cy.get('[data-cy="okButton"]')
            .should('exist');
        cy.get('[data-cy="okButton"]')
            .click();
        cy.get('[data-cy="skillActionsNumSelected"]')
            .should('have.text', '0');
    });

    it('some skills are already exported', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);
        cy.createSkill(1, 1, 4);
        cy.createSkill(1, 1, 5);

        cy.exportSkillToCatalog(1, 1, 3);
        cy.exportSkillToCatalog(1, 1, 5);

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="exportedBadge-skill1"')
            .should('not.exist');
        cy.get('[data-cy="exportedBadge-skill2"')
            .should('not.exist');
        cy.get('[data-cy="exportedBadge-skill3"');
        cy.get('[data-cy="exportedBadge-skill4"')
            .should('not.exist');
        cy.get('[data-cy="exportedBadge-skill5"');

        cy.get('[data-cy="skillActionsBtn"]')
            .should('be.disabled');
        cy.get('[data-cy="skillActionsNumSelected"]')
            .should('have.text', '0');
        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="3"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="4"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="skillActionsNumSelected"]')
            .should('have.text', '5');

        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Export To Catalog"]').click()

        cy.contains('This will export 3 Skills to the SkillTree Catalog');
        cy.contains('Note: The are already 2 skill(s) in the Skill Catalog from the provided selection.');
        cy.get('[data-cy="exportToCatalogButton"]')
            .should('exist');
        cy.get('[data-cy="closeButton"]')
            .should('exist');
        cy.get('[data-cy="okButton"]')
            .should('not.exist');

        cy.get('[data-cy="exportToCatalogButton"]')
            .click();
        cy.contains('3 Skills were successfully exported to the catalog!');
        cy.get('[data-cy="exportToCatalogButton"]')
            .should('not.exist');
        cy.get('[data-cy="closeButton"]')
            .should('not.exist');
        cy.get('[data-cy="okButton"]')
            .should('exist');

        cy.get('[data-cy="okButton"]')
            .click();

        cy.get('[data-cy="skillActionsBtn"]')
            .should('be.disabled');
        cy.get('[data-cy="skillActionsNumSelected"]')
            .should('have.text', '0');
        for (let i= 0; i < 5 ; i++) {
            cy.get(`[data-cy="skillsTable"] [data-p-index="${i}"] [data-pc-name="pcrowcheckbox"] input`).should('not.be.checked')
        }

        cy.get('[data-cy="exportedBadge-skill1"');
        cy.get('[data-cy="exportedBadge-skill2"');
        cy.get('[data-cy="exportedBadge-skill3"');
        cy.get('[data-cy="exportedBadge-skill4"');
        cy.get('[data-cy="exportedBadge-skill5"');

        // refresh and re-validate
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="skillActionsBtn"]')
            .should('be.disabled');
        cy.get('[data-cy="skillActionsNumSelected"]')
            .should('have.text', '0');

        cy.get('[data-cy="exportedBadge-skill1"');
        cy.get('[data-cy="exportedBadge-skill2"');
        cy.get('[data-cy="exportedBadge-skill3"');
        cy.get('[data-cy="exportedBadge-skill4"');
        cy.get('[data-cy="exportedBadge-skill5"');
    });

    it('cancel export', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);
        cy.createSkill(1, 1, 4);
        cy.createSkill(1, 1, 5);

        cy.exportSkillToCatalog(1, 1, 3);
        cy.exportSkillToCatalog(1, 1, 5);

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="exportedBadge-skill1"')
            .should('not.exist');
        cy.get('[data-cy="exportedBadge-skill2"')
            .should('not.exist');
        cy.get('[data-cy="exportedBadge-skill3"');
        cy.get('[data-cy="exportedBadge-skill4"')
            .should('not.exist');
        cy.get('[data-cy="exportedBadge-skill5"');

        cy.get('[data-cy="skillActionsBtn"]')
            .should('be.disabled');
        cy.get('[data-cy="skillActionsNumSelected"]')
            .should('have.text', '0');
        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="3"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="4"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="skillActionsNumSelected"]')
            .should('have.text', '5');

        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Export To Catalog"]').click()

        cy.contains('This will export 3 Skills to the SkillTree Catalog');
        cy.get('[data-cy="closeButton"]')
            .click();
        cy.get('[data-cy="skillActionsNumSelected"]')
            .should('have.text', '5');
        for (let i= 0; i < 5 ; i++) {
            cy.get(`[data-cy="skillsTable"] [data-p-index="${i}"] [data-pc-name="pcrowcheckbox"] input`).should('be.checked')
        }

        cy.get('[data-cy="exportedBadge-skill1"')
            .should('not.exist');
        cy.get('[data-cy="exportedBadge-skill2"')
            .should('not.exist');
        cy.get('[data-cy="exportedBadge-skill3"');
        cy.get('[data-cy="exportedBadge-skill4"')
            .should('not.exist');
        cy.get('[data-cy="exportedBadge-skill5"');

        cy.get('[data-cy="skillActionsBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="skillActionsNumSelected"]')
            .should('have.text', '5');

        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Export To Catalog"]').click()
        cy.get('[aria-label="Close"]')
            .click();
        cy.get('[data-cy="skillActionsNumSelected"]')
            .should('have.text', '5');
        cy.get('[data-cy="exportedBadge-skill1"')
            .should('not.exist');
        cy.get('[data-cy="exportedBadge-skill2"')
            .should('not.exist');
        cy.get('[data-cy="exportedBadge-skill3"');
        cy.get('[data-cy="exportedBadge-skill4"')
            .should('not.exist');
        cy.get('[data-cy="exportedBadge-skill5"');

        for (let i= 0; i < 5 ; i++) {
            cy.get(`[data-cy="skillsTable"] [data-p-index="${i}"] [data-pc-name="pcrowcheckbox"] input`).should('be.checked')
        }
    });

    it('page selection and clear actions', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);
        cy.createSkill(1, 1, 4);
        cy.createSkill(1, 1, 5);

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="skillActionsBtn"]')
            .should('be.disabled');
        cy.get('[data-cy="skillActionsNumSelected"]')
            .should('have.text', '0');
        for (let i= 0; i < 5 ; i++) {
            cy.get(`[data-cy="skillsTable"] [data-p-index="${i}"] [data-pc-name="pcrowcheckbox"] input`).should('not.be.checked')
        }

        cy.get('[data-cy="skillsTable"] [data-pc-name="pcheadercheckbox"] [data-pc-section="input"]').click()
        cy.get('[data-cy="skillActionsBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="skillActionsNumSelected"]')
            .should('have.text', '5');
        for (let i= 0; i < 5 ; i++) {
            cy.get(`[data-cy="skillsTable"] [data-p-index="${i}"] [data-pc-name="pcrowcheckbox"] input`).should('be.checked')
        }

        cy.get('[data-cy="skillsTable"] [data-pc-name="pcheadercheckbox"] [data-pc-section="input"]').click()
        cy.get('[data-cy="skillActionsBtn"]')
            .should('be.disabled');
        cy.get('[data-cy="skillActionsNumSelected"]')
            .should('have.text', '0');
        for (let i= 0; i < 5 ; i++) {
            cy.get(`[data-cy="skillsTable"] [data-p-index="${i}"] [data-pc-name="pcrowcheckbox"] input`).should('not.be.checked')
        }
    });

    it('additional columns - catalog', () => {
        cy.createSkill(1, 1, 1); // 1
        cy.createSkill(1, 1, 2); // 2
        cy.createSkill(1, 1, 3); // 3 - exported
        cy.exportSkillToCatalog(1, 1, 3);

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 4);
        cy.createSkill(2, 1, 5);
        cy.createSkill(2, 1, 6);
        cy.exportSkillToCatalog(2, 1, 4);
        cy.exportSkillToCatalog(2, 1, 5);
        cy.exportSkillToCatalog(2, 1, 6);

        cy.importSkillFromCatalog(1, 1, 2, 4); // 4 - imported

        cy.createSkill(1, 1, 7); // 5 - exported
        cy.exportSkillToCatalog(1, 1, 7);
        cy.createSkill(1, 1, 8); // 6
        cy.createSkill(1, 1, 9); // 7
        cy.createSkill(1, 1, 10); // 8
        cy.createSkill(1, 1, 11);  // 9 - exported
        cy.exportSkillToCatalog(1, 1, 11);

        cy.importSkillFromCatalog(1, 1, 2, 5); // 10 - imported
        cy.createSkill(1, 1, 12); // 11
        cy.importSkillFromCatalog(1, 1, 2, 6); // 12 - imported

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="skillsTable-additionalColumns"] [data-pc-section="dropdownicon"]').click()
        cy.get('[data-pc-section="overlay"] [aria-label="Catalog"]').click()
        cy.get('[data-pc-section="closebutton"]').click()
        cy.get('[data-pc-section="headercontent"]').contains('Display').click()
        cy.get('[data-pc-section="headercontent"]').contains('Display').click()

        cy.validateTable(tableSelector, [
            [{
                colIndex: 5,
                value: 'Imported'
            }],
            [{
                colIndex: 5,
                value: 'N/A'
            }],
            [{
                colIndex: 5,
                value: 'Imported'
            }],
            [{
                colIndex: 5,
                value: 'Exported'
            }],
            [{
                colIndex: 5,
                value: 'N/A'
            }],
            [{
                colIndex: 5,
                value: 'N/A'
            }],
            [{
                colIndex: 5,
                value: 'N/A'
            }],
            [{
                colIndex: 5,
                value: 'Exported'
            }],
            [{
                colIndex: 5,
                value: 'Imported'
            }],
            [{
                colIndex: 5,
                value: 'Exported'
            }],
            [{
                colIndex: 5,
                value: 'N/A'
            }],
            [{
                colIndex: 5,
                value: 'N/A'
            }],
        ], 10);

        cy.get('[data-pc-section="headercontent"]').contains('Catalog').click()
        cy.validateTable(tableSelector, [
            [{
                colIndex: 5,
                value: 'Exported'
            }],
            [{
                colIndex: 5,
                value: 'Exported'
            }],
            [{
                colIndex: 5,
                value: 'Exported'
            }],
            [{
                colIndex: 5,
                value: 'Imported'
            }],
            [{
                colIndex: 5,
                value: 'Imported'
            }],
            [{
                colIndex: 5,
                value: 'Imported'
            }],
            [{
                colIndex: 5,
                value: 'N/A'
            }],
            [{
                colIndex: 5,
                value: 'N/A'
            }],
            [{
                colIndex: 5,
                value: 'N/A'
            }],
            [{
                colIndex: 5,
                value: 'N/A'
            }],
            [{
                colIndex: 5,
                value: 'N/A'
            }],
            [{
                colIndex: 5,
                value: 'N/A'
            }],
        ], 10);
    });

    it('expanded skill details show exported status', () => {
        cy.createSkill(1, 1, 1); // 1
        cy.createSkill(1, 1, 2); // 2
        cy.createSkill(1, 1, 3); // 3 - exported
        cy.exportSkillToCatalog(1, 1, 3);
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-cy="skillsTable"] [data-p-index="0"] [data-pc-section="rowtogglebutton"]`).click()
        cy.get('[data-cy="childRowDisplay_skill3"] [data-cy="exportedToCatalogCard"]')
            .contains('This skill was exported');

        cy.get(`[data-cy="skillsTable"] [data-p-index="1"] [data-pc-section="rowtogglebutton"]`).click()
        cy.get('[data-cy="childRowDisplay_skill2"]')
            .contains('Description');
        cy.get('[data-cy="childRowDisplay_skill2"] [data-cy="exportedToCatalogCard"]')
            .should('not.exist');

        cy.get('[data-cy="childRowDisplay_skill3"] [data-cy="navigateToSkillCatalog"]')
            .click();
        cy.validateTable('[data-cy="exportedSkillsTable"]', [
            [{
                colIndex: 1,
                value: 'Very Great Skill 3'
            }, {
                colIndex: 1,
                value: 'Subject 1'
            }],
        ], 5);
    });

    it('exported skills should be available to be import in another project', () => {
        cy.createProject(2);
        cy.createSubject(2, 1);

        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);
        cy.createSkill(1, 1, 4);
        cy.createSkill(1, 1, 5);
        cy.createSkill(1, 1, 6);
        cy.createSkill(1, 1, 7);

        cy.exportSkillToCatalog(1, 1, 3);
        cy.exportSkillToCatalog(1, 1, 5);

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="manageSkillLink_skill6"]')
        // export another skill to validate that it makes it to import dialog
        cy.get('[data-cy="skillsTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"] [data-cy="skillActionsNumSelected"]')
            .should('have.text', 1);
        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Export To Catalog"]').click()
        cy.get('[data-cy="exportToCatalogButton"]')
            .click();
        cy.get('[data-cy="okButton"]')
            .should('be.enabled');
        cy.get('[data-cy="checkingCatalogStatus"]')
            .should('not.exist');
        cy.get('[data-cy="okButton"]')
            .click();

        // import dialog should be empty in this project
        cy.get('[data-cy="importFromCatalogBtn"]')
            .click();
        cy.get('[data-cy="catalogSkillImportModal-NoData"]')
            .contains('Nothing Available for Import');
        cy.get('[data-cy="checkingCatalogStatus"]')
            .should('not.exist')
        cy.get('[data-pc-section="closebutton"]').click()

        // navigate to another project
        cy.get('[data-cy="breadcrumb-Projects"]')
            .click();
        cy.get('[data-cy="projCard_proj2_manageBtn"]')
            .click();
        cy.get('[data-cy="manageBtn_subj1"]')
            .click();

        cy.get('[data-cy="importFromCatalogBtn"]')
            .click();
        cy.validateTable('[data-cy="importSkillsFromCatalogTable"]', [
            [{
                colIndex: 2,
                value: 'Very Great Skill 3'
            }, {
                colIndex: 3,
                value: 'project 1'
            }],
            [{
                colIndex: 2,
                value: 'Very Great Skill 5'
            }, {
                colIndex: 3,
                value: 'project 1'
            }],
            [{
                colIndex: 2,
                value: 'Very Great Skill 6'
            }, {
                colIndex: 3,
                value: 'project 1'
            }],
        ], 5);
    });

    it('export then drill down into the skill', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="manageSkillLink_skill1"]')
        cy.get('[data-cy="skillsTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"] [data-cy="skillActionsNumSelected"]')
            .should('have.text', 1);
        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Export To Catalog"]').click()
        cy.get('[data-pc-name="dialog"]').contains('This will export [Very Great Skill 1]')
        cy.get('[data-cy="exportToCatalogButton"]')
            .click();
        cy.get('[data-cy="okButton"]')
            .should('be.enabled');
        cy.get('[data-cy="checkingCatalogStatus"]')
            .should('not.exist')
        cy.get('[data-cy="okButton"]')
            .click();

        cy.get('[data-cy="exportedBadge-skill1"');
        cy.get('[data-cy="exportedBadge-skill2"')
            .should('not.exist');

        cy.get('[data-cy="manageSkillLink_skill1"]')
            .click();
        cy.get('[data-cy="pageHeader"] [data-cy="exportedBadge"]');

        cy.get('[data-cy="childRowDisplay_skill1"] [data-cy="exportedToCatalogCard"]')
            .contains('This skill was exported');
        cy.get('[data-cy="childRowDisplay_skill1"] [data-cy="navigateToSkillCatalog"]')
            .click();
        cy.validateTable('[data-cy="exportedSkillsTable"]', [
            [{
                colIndex: 1,
                value: 'Very Great Skill 1'
            }, {
                colIndex: 1,
                value: 'Subject 1'
            }],
        ], 5);

        // check skill that was NOT exported
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="manageSkillLink_skill2"]')
            .click();
        cy.get('[data-cy="pageHeader"] [data-cy="exportedBadge"]')
            .should('not.exist');
        cy.get('[data-cy="childRowDisplay_skill1"] [data-cy="exportedToCatalogCard"]')
            .should('not.exist');
    });

    it('do not allow to export skills with duplicate Skill ID or Skill Name', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);

        cy.exportSkillToCatalog(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 2);
        cy.exportSkillToCatalog(1, 1, 3);

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1);
        cy.createSkill(2, 1, 2, { name: 'Something Else' });
        cy.createSkill(2, 1, 3, { skillId: 'diffId' });
        cy.createSkill(2, 1, 4);
        cy.createSkill(2, 1, 5);

        cy.visit('/administrator/projects/proj2/subjects/subj1');
        cy.get('[data-cy="manageSkillLink_skill1"]')
        cy.get('[data-cy="skillsTable"] [data-p-index="4"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"] [data-cy="skillActionsNumSelected"]')
            .should('have.text', 1);
        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Export To Catalog"]').click()
        cy.contains('Cannot export 1 skill(s)');
        cy.get('[data-cy="dupSkill-skill1"]')
            .contains('Very Great Skill 1');
        cy.get('[data-cy="dupSkill-skill1"]')
            .contains('Name Conflict');
        cy.get('[data-cy="dupSkill-skill1"]')
            .contains('ID Conflict');
        cy.get('[data-cy="exportToCatalogButton"]')
            .should('not.exist');
        cy.get('[data-cy="okButton"]')
            .should('be.enabled');

        // close
        cy.get('[data-cy="okButton"]')
            .click();
        cy.get('[data-cy="okButton"]')
            .should('not.exist');

        // some skills not exportable because of the name and some because of the id
        cy.get('[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="3"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"] [data-cy="skillActionsNumSelected"]')
            .should('have.text', 2);
        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Export To Catalog"]').click()
        cy.contains('Cannot export 2 skill(s)');
        cy.get('[data-cy="dupSkill-skill2"]')
            .contains('Something Else');
        cy.get('[data-cy="dupSkill-skill2"]')
            .contains('ID Conflict');
        cy.get('[data-cy="dupSkill-diffId"]')
            .contains('Very Great Skill 3');
        cy.get('[data-cy="dupSkill-diffId"]')
            .contains('Name Conflict');
        cy.get('[data-cy="exportToCatalogButton"]')
            .should('not.exist');
        cy.get('[data-cy="okButton"]')
            .should('be.enabled');

        // close
        cy.get('[data-cy="okButton"]')
            .click();
        cy.get('[data-cy="okButton"]')
            .should('not.exist');

        // 1 skill can be exported while 2 cannot
        cy.get('[data-cy="skillsTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="3"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"] [data-cy="skillActionsNumSelected"]')
            .should('have.text', 3);

        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Export To Catalog"]').click()
        cy.contains('This will export [Very Great Skill 4] Skill');
        cy.contains('Cannot export 2 skill(s)');
        cy.get('[data-cy="dupSkill-skill2"]')
            .contains('Something Else');
        cy.get('[data-cy="dupSkill-skill2"]')
            .contains('ID Conflict');
        cy.get('[data-cy="dupSkill-diffId"]')
            .contains('Very Great Skill 3');
        cy.get('[data-cy="dupSkill-diffId"]')
            .contains('Name Conflict');
        cy.get('[data-cy="exportToCatalogButton"]')
            .should('be.enabled');
        cy.get('[data-cy="okButton"]')
            .should('not.exist');
        cy.get('[data-cy="cantExportTruncatedMsg"]')
            .should('not.exist');

        cy.get('[data-cy="closeButton"]')
            .click();
        cy.get('[data-cy="closeButton"]')
            .should('not.exist');

        // multiple skills can be exported while some cannot
        cy.get('[data-cy="skillsTable"] [data-pc-name="pcheadercheckbox"] [data-pc-section="input"]').click();
        cy.get('[data-cy="skillActionsBtn"] [data-cy="skillActionsNumSelected"]')
            .should('have.text', 5);
        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Export To Catalog"]').click()
        cy.contains('This will export 2 Skills to the ');
        cy.contains('Cannot export 3 skill(s)');
        cy.get('[data-cy="dupSkill-skill1"]')
            .contains('Very Great Skill 1');
        cy.get('[data-cy="dupSkill-skill2"]')
            .contains('Something Else');
        cy.get('[data-cy="dupSkill-diffId"]')
            .contains('Very Great Skill 3');
        cy.get('[data-cy="exportToCatalogButton"]')
            .should('be.enabled');
        cy.get('[data-cy="okButton"]')
            .should('not.exist');

        cy.get('[data-cy="exportToCatalogButton"]')
            .click();
        cy.contains('2 Skills were successfully exported to the catalog!');
        cy.get('[data-cy="okButton"]')
            .click();
        cy.get('[data-cy="breadcrumb-proj2"]')
            .click();
        cy.get('[data-cy="nav-Skill Catalog"]')
            .click();
        cy.get(`[data-cy="exportedSkillsTable"] th`)
          .contains('Skill')
          .click();
        cy.validateTable('[data-cy="exportedSkillsTable"]', [
            [{
                colIndex: 1,
                value: 'Very Great Skill 4'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 5'
            }],
        ], 5);
    });

    it('do not allow to export skills with prerequisites', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);

        cy.exportSkillToCatalog(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 2);
        cy.exportSkillToCatalog(1, 1, 3);

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1);
        cy.createSkill(2, 1, 2, { name: 'Something Else' });
        cy.createSkill(2, 1, 3, { skillId: 'diffId' });
        cy.createSkill(2, 1, 4);
        cy.createSkill(2, 1, 5);
        cy.createSkill(2, 1, 6);
        cy.addLearningPathItem(2, 6, 5)
        cy.createSkill(2, 1, 7);

        cy.visit('/administrator/projects/proj2/subjects/subj1');
        cy.get('[data-cy="manageSkillLink_skill1"]')
        cy.get('[data-cy="skillsTable"] [data-pc-name="pcheadercheckbox"] [data-pc-section="input"]').click();
        cy.get('[data-cy="skillActionsBtn"] [data-cy="skillActionsNumSelected"]')
            .should('have.text', 7);

        // export after loading the page
        cy.exportSkillToCatalog(2, 1, 7);

        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Export To Catalog"]').click()
        cy.contains('Note: The are already 1 skill(s) in the Skill Catalog from the provided selection.');
        cy.contains('This will export 2 Skills');
        cy.get('[data-cy="dupSkill-skill1"]')
            .contains('ID Conflict');
        cy.get('[data-cy="dupSkill-skill1"]')
            .contains('Name Conflict');
        cy.get('[data-cy="dupSkill-skill2"]')
            .contains('ID Conflict');
        cy.get('[data-cy="dupSkill-diffId"]')
            .contains('Name Conflict');
        cy.get('[data-cy="dupSkill-skill5"]')
            .contains('Has Prerequisites');
    });

    it('do not allow to export skills with global prerequisites', () => {
        cy.createSkill(1, 1, 1);

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1);
        cy.addCrossProjectLearningPathItem(1, 1, 2, 1)

        cy.visit('/administrator/projects/proj2/subjects/subj1');
        cy.get('[data-cy="manageSkillLink_skill1"]');

        cy.get('[data-cy="skillsTable"] [data-pc-name="pcheadercheckbox"] [data-pc-section="input"]').click();
        cy.get('[data-cy="skillActionsBtn"] [data-cy="skillActionsNumSelected"]')
            .should('have.text', 1);

        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Export To Catalog"]').click()
        cy.contains('Cannot export 1 skill(s)');
        cy.get('[data-cy="dupSkill-skill1"]')
            .contains('Has Prerequisites');
    });

    it('do not include imported skills in Actions count when selecting all skills in a subject', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);

        cy.exportSkillToCatalog(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 2);
        cy.exportSkillToCatalog(1, 1, 3);

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 11);
        cy.createSkill(2, 1, 12);
        cy.createSkill(2, 1, 13);
        cy.createSkill(2, 1, 14);
        cy.createSkill(2, 1, 15);

        cy.importSkillFromCatalog(2, 1, 1, 1);
        cy.importSkillFromCatalog(2, 1, 1, 2);
        cy.importSkillFromCatalog(2, 1, 1, 3);

        cy.visit('/administrator/projects/proj2/subjects/subj1');
        cy.get('[data-cy="skillsTable"] [data-pc-name="pcheadercheckbox"] [data-pc-section="input"]').click();

        cy.get('[data-cy=skillActionsNumSelected]')
            .contains('5');
    });

    it('do not allow to export if the project has insufficient points', () => {
        cy.createSkill(1, 1, 1, { pointIncrement: 10 });
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="manageSkillLink_skill1"]')

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"] [data-cy="skillActionsNumSelected"]')
            .should('have.text', 1);
        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Export To Catalog"]').click()
        cy.contains('Export of skills is not allowed until the subject has sufficient points');

        cy.get('[data-cy="exportToCatalogButton"]')
            .should('not.exist');
        cy.get('[data-cy="closeButton"]')
            .should('not.exist');
    });

});

