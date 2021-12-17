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
        cy.createProject(1);
        cy.createSubject(1, 1);
    });
    const tableSelector = '[data-cy="skillsTable"]'

    // mix exported and imported skills
    // Additional collumns
    // skill details showing export status
    // export via single skill
    // export via single skill then navigate to the table
    // drill down after exporting on skills page
    // export in the table then navigate to a different project and make sure that are avialable to import


    it('export 1 skill', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="skillActionsBtn"] button').should('be.disabled');
        cy.get('[data-cy="skillActionsNumSelected"]').should('have.text', '0');
        cy.get('[data-cy="skillSelect-skill1"]').click({force: true});
        cy.get('[data-cy="skillActionsBtn"] button').should('be.enabled');
        cy.get('[data-cy="skillActionsNumSelected"]').should('have.text', '1');

        cy.get('[data-cy="skillActionsBtn"] button').click();
        cy.get('[data-cy="skillExportToCatalogBtn"]').click();

        cy.contains('This will export Skill with id [skill1] to the SkillTree Catalog');
        cy.get('[data-cy="exportToCatalogButton"]').click();
        cy.contains('Skill with id skill1 was successfully exported to the catalog!')
        cy.get('[data-cy="exportToCatalogButton"]').should('not.exist')
        cy.get('[data-cy="closeButton"]').should('not.exist')
        cy.get('[data-cy="okButton"]').click()

        cy.get('[data-cy="skillActionsNumSelected"]').should('have.text', '0');
        cy.get('[data-cy="exportedBadge-skill1"')
        cy.get('[data-cy="exportedBadge-skill2"').should('not.exist')

        cy.get('[data-cy="skillActionsBtn"] button').should('be.disabled');
        cy.get('[data-cy="skillsTable-additionalColumns"]').contains('Catalog').click();

        cy.validateTable(tableSelector, [
            [{ colIndex: 3,  value: 'N/A' }],
            [{ colIndex: 3,  value: 'Exported' }],
        ], 5);

        // refresh and re-validate
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="exportedBadge-skill1"')
        cy.get('[data-cy="exportedBadge-skill2"').should('not.exist')
        cy.get('[data-cy="skillActionsBtn"] button').should('be.disabled');
        cy.get('[data-cy="skillsTable-additionalColumns"]').contains('Catalog').click();
        cy.get('[data-cy="skillActionsNumSelected"]').should('have.text', '0');
        cy.validateTable(tableSelector, [
            [{ colIndex: 3,  value: 'N/A' }],
            [{ colIndex: 3,  value: 'Exported' }],
        ], 5);
    });

    it('export multiple skill', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);
        cy.createSkill(1, 1, 4);
        cy.createSkill(1, 1, 5);

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="skillActionsBtn"] button').should('be.disabled');
        cy.get('[data-cy="skillActionsNumSelected"]').should('have.text', '0');
        cy.get('[data-cy="skillSelect-skill1"]').click({force: true});
        cy.get('[data-cy="skillSelect-skill3"]').click({force: true});
        cy.get('[data-cy="skillSelect-skill5"]').click({force: true});
        cy.get('[data-cy="skillActionsBtn"] button').should('be.enabled');
        cy.get('[data-cy="skillActionsNumSelected"]').should('have.text', '3');

        cy.get('[data-cy="skillActionsBtn"] button').click();
        cy.get('[data-cy="skillExportToCatalogBtn"]').click();

        cy.contains('This will export 3 Skills to the SkillTree Catalog');
        cy.get('[data-cy="exportToCatalogButton"]').click();
        cy.contains('3 Skills were successfully exported to the catalog!');

        cy.get('[data-cy="exportToCatalogButton"]').should('not.exist')
        cy.get('[data-cy="closeButton"]').should('not.exist')
        cy.get('[data-cy="okButton"]').click()

        cy.get('[data-cy="skillActionsNumSelected"]').should('have.text', '0');
        cy.get('[data-cy="skillSelect-skill1"]').should('not.be.checked')
        cy.get('[data-cy="skillSelect-skill2"]').should('not.be.checked')
        cy.get('[data-cy="skillSelect-skill3"]').should('not.be.checked')
        cy.get('[data-cy="skillSelect-skill4"]').should('not.be.checked')
        cy.get('[data-cy="skillSelect-skill5"]').should('not.be.checked')
        cy.get('[data-cy="exportedBadge-skill1"')
        cy.get('[data-cy="exportedBadge-skill2"').should('not.exist')
        cy.get('[data-cy="exportedBadge-skill3"')
        cy.get('[data-cy="exportedBadge-skill4"').should('not.exist')
        cy.get('[data-cy="exportedBadge-skill5"')

        // refresh and re-validate
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="skillActionsNumSelected"]').should('have.text', '0');
        cy.get('[data-cy="exportedBadge-skill1"')
        cy.get('[data-cy="exportedBadge-skill2"').should('not.exist')
        cy.get('[data-cy="exportedBadge-skill3"')
        cy.get('[data-cy="exportedBadge-skill4"').should('not.exist')
        cy.get('[data-cy="exportedBadge-skill5"')
        cy.get('[data-cy="skillSelect-skill1"]').should('not.be.checked')
        cy.get('[data-cy="skillSelect-skill2"]').should('not.be.checked')
        cy.get('[data-cy="skillSelect-skill3"]').should('not.be.checked')
        cy.get('[data-cy="skillSelect-skill4"]').should('not.be.checked')
        cy.get('[data-cy="skillSelect-skill5"]').should('not.be.checked')
    });


    it('export all skills', () => {
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
        cy.get('[data-cy="skillActionsBtn"] button').should('be.disabled');
        cy.get('[data-cy="skillActionsNumSelected"]').should('have.text', '0');
        cy.get('[data-cy="selectAllSkillsBtn"]').click();
        cy.get('[data-cy="skillActionsBtn"] button').should('be.enabled');
        cy.get('[data-cy="skillActionsNumSelected"]').should('have.text', '12');

        cy.get('[data-cy="skillActionsBtn"] button').click();
        cy.get('[data-cy="skillExportToCatalogBtn"]').click();

        cy.contains('This will export 12 Skills to the SkillTree Catalog');
        cy.get('[data-cy="exportToCatalogButton"]').click();
        cy.contains('12 Skills were successfully exported to the catalog!');

        cy.get('[data-cy="exportToCatalogButton"]').should('not.exist')
        cy.get('[data-cy="closeButton"]').should('not.exist')
        cy.get('[data-cy="okButton"]').click()

        cy.get('[data-cy="skillActionsBtn"] button').should('be.disabled');
        cy.get('[data-cy="skillActionsNumSelected"]').should('have.text', '0');

        cy.get('[data-cy="exportedBadge-skill3"')
        cy.get('[data-cy="exportedBadge-skill4"')
        cy.get('[data-cy="exportedBadge-skill5"')
        cy.get('[data-cy="exportedBadge-skill6"')
        cy.get('[data-cy="exportedBadge-skill7"')
        cy.get('[data-cy="exportedBadge-skill8"')
        cy.get('[data-cy="exportedBadge-skill9"')
        cy.get('[data-cy="exportedBadge-skill10"')
        cy.get('[data-cy="exportedBadge-skill11"')
        cy.get('[data-cy="exportedBadge-skill12"')

        cy.get('[data-cy="skillsBTablePaging"]').contains('2').click();

        cy.get('[data-cy="exportedBadge-skill1"')
        cy.get('[data-cy="exportedBadge-skill2"')

        // refresh and re-validate
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="skillActionsBtn"] button').should('be.disabled');

        cy.get('[data-cy="exportedBadge-skill3"')
        cy.get('[data-cy="exportedBadge-skill4"')
        cy.get('[data-cy="exportedBadge-skill5"')
        cy.get('[data-cy="exportedBadge-skill6"')
        cy.get('[data-cy="exportedBadge-skill7"')
        cy.get('[data-cy="exportedBadge-skill8"')
        cy.get('[data-cy="exportedBadge-skill9"')
        cy.get('[data-cy="exportedBadge-skill10"')
        cy.get('[data-cy="exportedBadge-skill11"')
        cy.get('[data-cy="exportedBadge-skill12"')

        cy.get('[data-cy="skillsBTablePaging"]').contains('2').click();

        cy.get('[data-cy="exportedBadge-skill1"')
        cy.get('[data-cy="exportedBadge-skill2"')
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

        cy.get('[data-cy="exportedBadge-skill1"')
        cy.get('[data-cy="exportedBadge-skill2"').should('not.exist')
        cy.get('[data-cy="exportedBadge-skill3"')
        cy.get('[data-cy="exportedBadge-skill4"').should('not.exist')
        cy.get('[data-cy="exportedBadge-skill5"')

        cy.get('[data-cy="skillActionsBtn"] button').should('be.disabled');
        cy.get('[data-cy="skillActionsNumSelected"]').should('have.text', '0');
        cy.get('[data-cy="skillSelect-skill1"]').click({force: true});
        cy.get('[data-cy="skillSelect-skill3"]').click({force: true});
        cy.get('[data-cy="skillSelect-skill5"]').click({force: true});
        cy.get('[data-cy="skillActionsBtn"] button').should('be.enabled');
        cy.get('[data-cy="skillActionsNumSelected"]').should('have.text', '3');

        cy.get('[data-cy="skillActionsBtn"] button').click();
        cy.get('[data-cy="skillExportToCatalogBtn"]').click();

        cy.contains('All selected 3 skill(s) are already in the Skill Catalog.')
        cy.get('[data-cy="exportToCatalogButton"]').should('not.exist')
        cy.get('[data-cy="closeButton"]').should('not.exist')
        cy.get('[data-cy="okButton"]').should('exist')
        cy.get('[data-cy="okButton"]').click()
        cy.get('[data-cy="skillActionsNumSelected"]').should('have.text', '0');
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

        cy.get('[data-cy="exportedBadge-skill1"').should('not.exist')
        cy.get('[data-cy="exportedBadge-skill2"').should('not.exist')
        cy.get('[data-cy="exportedBadge-skill3"')
        cy.get('[data-cy="exportedBadge-skill4"').should('not.exist')
        cy.get('[data-cy="exportedBadge-skill5"')

        cy.get('[data-cy="skillActionsBtn"] button').should('be.disabled');
        cy.get('[data-cy="skillActionsNumSelected"]').should('have.text', '0');
        cy.get('[data-cy="skillSelect-skill1"]').click({force: true});
        cy.get('[data-cy="skillSelect-skill2"]').click({force: true});
        cy.get('[data-cy="skillSelect-skill3"]').click({force: true});
        cy.get('[data-cy="skillSelect-skill4"]').click({force: true});
        cy.get('[data-cy="skillSelect-skill5"]').click({force: true});
        cy.get('[data-cy="skillActionsBtn"] button').should('be.enabled');
        cy.get('[data-cy="skillActionsNumSelected"]').should('have.text', '5');

        cy.get('[data-cy="skillActionsBtn"] button').click();
        cy.get('[data-cy="skillExportToCatalogBtn"]').click();

        cy.contains('This will export 3 Skills to the SkillTree Catalog')
        cy.contains('Note: The are already 2 skill(s) in the Skill Catalog from the provided selection.')
        cy.get('[data-cy="exportToCatalogButton"]').should('exist')
        cy.get('[data-cy="closeButton"]').should('exist')
        cy.get('[data-cy="okButton"]').should('not.exist')

        cy.get('[data-cy="exportToCatalogButton"]').click();
        cy.contains('3 Skills were successfully exported to the catalog!');
        cy.get('[data-cy="exportToCatalogButton"]').should('not.exist')
        cy.get('[data-cy="closeButton"]').should('not.exist')
        cy.get('[data-cy="okButton"]').should('exist')

        cy.get('[data-cy="okButton"]').click()

        cy.get('[data-cy="skillActionsBtn"] button').should('be.disabled');
        cy.get('[data-cy="skillActionsNumSelected"]').should('have.text', '0');
        cy.get('[data-cy="skillSelect-skill1"]').should('not.be.checked')
        cy.get('[data-cy="skillSelect-skill2"]').should('not.be.checked')
        cy.get('[data-cy="skillSelect-skill3"]').should('not.be.checked')
        cy.get('[data-cy="skillSelect-skill4"]').should('not.be.checked')
        cy.get('[data-cy="skillSelect-skill5"]').should('not.be.checked')

        cy.get('[data-cy="exportedBadge-skill1"')
        cy.get('[data-cy="exportedBadge-skill2"')
        cy.get('[data-cy="exportedBadge-skill3"')
        cy.get('[data-cy="exportedBadge-skill4"')
        cy.get('[data-cy="exportedBadge-skill5"')

        // refresh and re-validate
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="skillActionsBtn"] button').should('be.disabled');
        cy.get('[data-cy="skillActionsNumSelected"]').should('have.text', '0');

        cy.get('[data-cy="exportedBadge-skill1"')
        cy.get('[data-cy="exportedBadge-skill2"')
        cy.get('[data-cy="exportedBadge-skill3"')
        cy.get('[data-cy="exportedBadge-skill4"')
        cy.get('[data-cy="exportedBadge-skill5"')
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

        cy.get('[data-cy="exportedBadge-skill1"').should('not.exist')
        cy.get('[data-cy="exportedBadge-skill2"').should('not.exist')
        cy.get('[data-cy="exportedBadge-skill3"')
        cy.get('[data-cy="exportedBadge-skill4"').should('not.exist')
        cy.get('[data-cy="exportedBadge-skill5"')

        cy.get('[data-cy="skillActionsBtn"] button').should('be.disabled');
        cy.get('[data-cy="skillActionsNumSelected"]').should('have.text', '0');
        cy.get('[data-cy="skillSelect-skill1"]').click({force: true});
        cy.get('[data-cy="skillSelect-skill2"]').click({force: true});
        cy.get('[data-cy="skillSelect-skill3"]').click({force: true});
        cy.get('[data-cy="skillSelect-skill4"]').click({force: true});
        cy.get('[data-cy="skillSelect-skill5"]').click({force: true});
        cy.get('[data-cy="skillActionsBtn"] button').should('be.enabled');
        cy.get('[data-cy="skillActionsNumSelected"]').should('have.text', '5');

        cy.get('[data-cy="skillActionsBtn"] button').click();
        cy.get('[data-cy="skillExportToCatalogBtn"]').click();

        cy.contains('This will export 3 Skills to the SkillTree Catalog');
        cy.get('[data-cy="closeButton"]').click();
        cy.get('[data-cy="skillActionsNumSelected"]').should('have.text', '0');
        cy.get('[data-cy="skillSelect-skill1"]').should('not.be.checked')
        cy.get('[data-cy="skillSelect-skill2"]').should('not.be.checked')
        cy.get('[data-cy="skillSelect-skill3"]').should('not.be.checked')
        cy.get('[data-cy="skillSelect-skill4"]').should('not.be.checked')
        cy.get('[data-cy="skillSelect-skill5"]').should('not.be.checked')

        cy.get('[data-cy="exportedBadge-skill1"').should('not.exist')
        cy.get('[data-cy="exportedBadge-skill2"').should('not.exist')
        cy.get('[data-cy="exportedBadge-skill3"')
        cy.get('[data-cy="exportedBadge-skill4"').should('not.exist')
        cy.get('[data-cy="exportedBadge-skill5"')

        cy.get('[data-cy="skillSelect-skill1"]').click({force: true});
        cy.get('[data-cy="skillSelect-skill2"]').click({force: true});
        cy.get('[data-cy="skillSelect-skill3"]').click({force: true});
        cy.get('[data-cy="skillSelect-skill4"]').click({force: true});
        cy.get('[data-cy="skillSelect-skill5"]').click({force: true});
        cy.get('[data-cy="skillActionsBtn"] button').should('be.enabled');
        cy.get('[data-cy="skillActionsNumSelected"]').should('have.text', '5');

        cy.get('[data-cy="skillActionsBtn"] button').click();
        cy.get('[data-cy="skillExportToCatalogBtn"]').click();
        cy.get('.modal-content [aria-label="Close"]').click();
        cy.get('[data-cy="skillActionsNumSelected"]').should('have.text', '0');
        cy.get('[data-cy="exportedBadge-skill1"').should('not.exist')
        cy.get('[data-cy="exportedBadge-skill2"').should('not.exist')
        cy.get('[data-cy="exportedBadge-skill3"')
        cy.get('[data-cy="exportedBadge-skill4"').should('not.exist')
        cy.get('[data-cy="exportedBadge-skill5"')

        cy.get('[data-cy="skillSelect-skill1"]').should('not.be.checked')
        cy.get('[data-cy="skillSelect-skill2"]').should('not.be.checked')
        cy.get('[data-cy="skillSelect-skill3"]').should('not.be.checked')
        cy.get('[data-cy="skillSelect-skill4"]').should('not.be.checked')
        cy.get('[data-cy="skillSelect-skill5"]').should('not.be.checked')
    });

    it('page selection and clear actions', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);
        cy.createSkill(1, 1, 4);
        cy.createSkill(1, 1, 5);

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="skillActionsBtn"] button').should('be.disabled');
        cy.get('[data-cy="skillActionsNumSelected"]').should('have.text', '0');
        cy.get('[data-cy="skillSelect-skill1"]').should('not.be.checked')
        cy.get('[data-cy="skillSelect-skill2"]').should('not.be.checked')
        cy.get('[data-cy="skillSelect-skill3"]').should('not.be.checked')
        cy.get('[data-cy="skillSelect-skill4"]').should('not.be.checked')
        cy.get('[data-cy="skillSelect-skill5"]').should('not.be.checked')

        cy.get('[data-cy="selectAllSkillsBtn"]').click();
        cy.get('[data-cy="skillActionsBtn"] button').should('be.enabled');
        cy.get('[data-cy="skillActionsNumSelected"]').should('have.text', '5');
        cy.get('[data-cy="skillSelect-skill1"]').should('be.checked')
        cy.get('[data-cy="skillSelect-skill2"]').should('be.checked')
        cy.get('[data-cy="skillSelect-skill3"]').should('be.checked')
        cy.get('[data-cy="skillSelect-skill4"]').should('be.checked')
        cy.get('[data-cy="skillSelect-skill5"]').should('be.checked')

        cy.get('[data-cy="clearSelectedSkillsBtn"]').click();
        cy.get('[data-cy="skillActionsBtn"] button').should('be.disabled');
        cy.get('[data-cy="skillActionsNumSelected"]').should('have.text', '0');
        cy.get('[data-cy="skillSelect-skill1"]').should('not.be.checked')
        cy.get('[data-cy="skillSelect-skill2"]').should('not.be.checked')
        cy.get('[data-cy="skillSelect-skill3"]').should('not.be.checked')
        cy.get('[data-cy="skillSelect-skill4"]').should('not.be.checked')
        cy.get('[data-cy="skillSelect-skill5"]').should('not.be.checked')

        cy.get('[data-cy="skillSelect-skill2"]').check({force: true})
        cy.get('[data-cy="skillActionsBtn"] button').should('be.enabled');
        cy.get('[data-cy="skillActionsNumSelected"]').should('have.text', '1');
        cy.get('[data-cy="skillSelect-skill1"]').should('not.be.checked')
        cy.get('[data-cy="skillSelect-skill2"]').should('be.checked')
        cy.get('[data-cy="skillSelect-skill3"]').should('not.be.checked')
        cy.get('[data-cy="skillSelect-skill4"]').should('not.be.checked')
        cy.get('[data-cy="skillSelect-skill5"]').should('not.be.checked')

        cy.get('[data-cy="skillSelect-skill4"]').check({force: true})
        cy.get('[data-cy="skillActionsBtn"] button').should('be.enabled');
        cy.get('[data-cy="skillActionsNumSelected"]').should('have.text', '2');
        cy.get('[data-cy="skillSelect-skill1"]').should('not.be.checked')
        cy.get('[data-cy="skillSelect-skill2"]').should('be.checked')
        cy.get('[data-cy="skillSelect-skill3"]').should('not.be.checked')
        cy.get('[data-cy="skillSelect-skill4"]').should('be.checked')
        cy.get('[data-cy="skillSelect-skill5"]').should('not.be.checked')

        cy.get('[data-cy="selectAllSkillsBtn"]').click();
        cy.get('[data-cy="skillActionsBtn"] button').should('be.enabled');
        cy.get('[data-cy="skillActionsNumSelected"]').should('have.text', '5');
        cy.get('[data-cy="skillSelect-skill1"]').should('be.checked')
        cy.get('[data-cy="skillSelect-skill2"]').should('be.checked')
        cy.get('[data-cy="skillSelect-skill3"]').should('be.checked')
        cy.get('[data-cy="skillSelect-skill4"]').should('be.checked')
        cy.get('[data-cy="skillSelect-skill5"]').should('be.checked')

        cy.get('[data-cy="clearSelectedSkillsBtn"]').click();
        cy.get('[data-cy="skillActionsBtn"] button').should('be.disabled');
        cy.get('[data-cy="skillActionsNumSelected"]').should('have.text', '0');
        cy.get('[data-cy="skillSelect-skill1"]').should('not.be.checked')
        cy.get('[data-cy="skillSelect-skill2"]').should('not.be.checked')
        cy.get('[data-cy="skillSelect-skill3"]').should('not.be.checked')
        cy.get('[data-cy="skillSelect-skill4"]').should('not.be.checked')
        cy.get('[data-cy="skillSelect-skill5"]').should('not.be.checked')
    });

});

