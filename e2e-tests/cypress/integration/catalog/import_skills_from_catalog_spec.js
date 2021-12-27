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

describe('Import skills from Catalog Tests', () => {

    beforeEach(() => {
        cy.createProject(1);
        cy.createSubject(1, 1);
    });

    const tableSelector = '[data-cy="importSkillsFromCatalogTable"]';

    it('there are no skills to import - empty catalog', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="importFromCatalogBtn"]').click();
        cy.get('[data-cy="catalogSkillImportModal-NoData"]').contains('Nothing Available for Import')
        cy.get('[data-cy="importBtn"]').should('not.exist');
        cy.get('[data-cy="closeButton"]').should('not.exist');
        cy.get('[data-cy="okButton"]').should('be.enabled')
        cy.get('[data-cy="okButton"]').click();
        cy.get('[data-cy="catalogSkillImportModal-NoData"]').should('not.exist');

        // use x button now
        cy.get('[data-cy="importFromCatalogBtn"]').click();
        cy.get('[data-cy="catalogSkillImportModal-NoData"]').contains('Nothing Available for Import')
        cy.get('.modal-content [aria-label="Close"]').click();
        cy.get('[data-cy="catalogSkillImportModal-NoData"]').should('not.exist');
    });

    it('there are no skills to import - all skills were imported', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.exportSkillToCatalog(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 2);

        cy.createProject(2);
        cy.createSubject(2, 1);

        cy.importSkillFromCatalog(2, 1, 1, 1)
        cy.importSkillFromCatalog(2, 1, 1, 2)

        cy.visit('/administrator/projects/proj2/subjects/subj1');
        cy.get('[data-cy="importFromCatalogBtn"]').click();

        cy.get('[data-cy="catalogSkillImportModal-NoData"]').contains('Nothing Available for Import')
        cy.get('[data-cy="importBtn"]').should('not.exist');
        cy.get('[data-cy="closeButton"]').should('not.exist');
        cy.get('[data-cy="okButton"]').should('be.enabled')
        cy.get('[data-cy="okButton"]').click();
        cy.get('[data-cy="catalogSkillImportModal-NoData"]').should('not.exist');
    });

    it('import 1 skill', () => {
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1);
        cy.createSkill(2, 1, 2);

        cy.exportSkillToCatalog(2, 1, 1);
        cy.exportSkillToCatalog(2, 1, 2);

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="importFromCatalogBtn"]').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'Very Great Skill 1' }, { colIndex: 1,  value: 'ID: proj2' }],
            [{ colIndex: 0,  value: 'Very Great Skill 2' }, { colIndex: 1,  value: 'ID: proj2' }],
        ], 5);

        cy.get('[data-cy="importBtn"]').should('be.disabled');
        cy.get('[data-cy="skillSelect_proj2-skill1"]').check({force: true})
        cy.get('[data-cy="importBtn"]').should('be.enabled');

        cy.get('[data-cy="importBtn"]').click();
        cy.get('[data-cy="skillsTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '1');
        cy.get('[data-cy="importedBadge-skill1"]')
        cy.get('[data-cy="skillsTable-additionalColumns"]').contains('Catalog').click();
        cy.validateTable('[data-cy="skillsTable"]', [
            [{ colIndex: 3,  value: 'Imported from This is project 2' }],
        ])

        // refresh and re-validate
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="skillsTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '1');
        cy.get('[data-cy="importedBadge-skill1"]')
        cy.get('[data-cy="skillsTable-additionalColumns"]').contains('Catalog').click();
        cy.validateTable('[data-cy="skillsTable"]', [
            [{ colIndex: 3,  value: 'Imported from This is project 2' }],
        ])
    });

    it('import last available skill', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.exportSkillToCatalog(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 2);

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.importSkillFromCatalog(2, 1, 1, 1)

        cy.visit('/administrator/projects/proj2/subjects/subj1');
        cy.get('[data-cy="importFromCatalogBtn"]').click();
        cy.get('[data-cy="skillSelect_proj1-skill2"]').check({force: true})
        cy.get('[data-cy="importBtn"]').should('be.enabled');
        cy.get('[data-cy="numSelectedSkills"]').should('have.text', '1');
        cy.get('[data-cy="importBtn"]').click();

        cy.get('[data-cy="skillsTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '2');
        cy.get('[data-cy="importedBadge-skill1"]')
        cy.get('[data-cy="importedBadge-skill2"]')

        cy.get('[data-cy="importFromCatalogBtn"]').click();
        cy.get('[data-cy="catalogSkillImportModal-NoData"]').contains('Nothing Available for Import')
    })

    it('cancel import', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.exportSkillToCatalog(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 2);

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.importSkillFromCatalog(2, 1, 1, 1)

        cy.visit('/administrator/projects/proj2/subjects/subj1');
        cy.get('[data-cy="importFromCatalogBtn"]').click();
        cy.get('[data-cy="skillSelect_proj1-skill2"]').check({force: true})

        cy.get('.modal-content')
        cy.get('[data-cy="closeButton"]').click();
        cy.get('.modal-content').should('not.exist')

        cy.get('[data-cy="skillsTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '1');
        cy.get('[data-cy="importedBadge-skill1"]')
        cy.get('[data-cy="importedBadge-skill2"]').should('not.exist')
    })


    it('show skill details', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.exportSkillToCatalog(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 2);

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 3);
        cy.createSkill(2, 1, 4);
        cy.exportSkillToCatalog(2, 1, 3);
        cy.exportSkillToCatalog(2, 1, 4);

        cy.createProject(3);
        cy.createSubject(3, 1);

        cy.importSkillFromCatalog(3, 1, 1, 1)
        cy.importSkillFromCatalog(3, 1, 1, 2)

        cy.importSkillFromCatalog(3, 1, 2, 3)
        cy.importSkillFromCatalog(3, 1, 2, 4)

        cy.visit('/administrator/projects/proj3/subjects/subj1');
        cy.get('[data-cy="skillsTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '4');

        cy.get('[data-cy="expandDetailsBtn_skill3"]').click();
        cy.get('[data-cy="childRowDisplay_skill3"]').contains('This skill was imported')
        cy.get('[data-cy="childRowDisplay_skill3"]').contains('initially defined in the This is project 2 project')

        cy.get('[data-cy="expandDetailsBtn_skill2"]').click();
        cy.get('[data-cy="childRowDisplay_skill2"]').contains('This skill was imported')
        cy.get('[data-cy="childRowDisplay_skill2"]').contains('initially defined in the This is project 1 project')
    })


    it('import 1 page of skills', () => {
        // mix skill names since it's sorted by skillId - this will force different projects in the first page
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 6);
        cy.createSkill(1, 1, 7);
        cy.createSkill(1, 1, 4);
        cy.createSkill(1, 1, 5);

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 2);
        cy.createSkill(2, 1, 3);
        cy.createSkill(2, 1, 8);

        cy.exportSkillToCatalog(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 6);
        cy.exportSkillToCatalog(1, 1, 7);
        cy.exportSkillToCatalog(1, 1, 4);
        cy.exportSkillToCatalog(1, 1, 5);

        cy.exportSkillToCatalog(2, 1, 2); // proj 2
        cy.exportSkillToCatalog(2, 1, 3); // proj 2
        cy.exportSkillToCatalog(2, 1, 8); // proj 2

        cy.createProject(3);
        cy.createSubject(3, 1);

        cy.visit('/administrator/projects/proj3/subjects/subj1');

        cy.get('[data-cy="importFromCatalogBtn"]').click();

        cy.get('[data-cy="importBtn"]').should('be.disabled');
        cy.get('[data-cy="numSelectedSkills"]').should('have.text', '0');

        cy.get('[data-cy="skillSelect_proj1-skill1"]').should('not.be.checked')
        cy.get('[data-cy="skillSelect_proj2-skill2"]').should('not.be.checked')
        cy.get('[data-cy="skillSelect_proj2-skill3"]').should('not.be.checked')
        cy.get('[data-cy="skillSelect_proj1-skill4"]').should('not.be.checked')
        cy.get('[data-cy="skillSelect_proj1-skill5"]').should('not.be.checked')

        cy.get('[data-cy="selectPageOfSkillsBtn"]').click();

        cy.get('[data-cy="skillSelect_proj1-skill1"]').should('be.checked')
        cy.get('[data-cy="skillSelect_proj2-skill2"]').should('be.checked')
        cy.get('[data-cy="skillSelect_proj2-skill3"]').should('be.checked')
        cy.get('[data-cy="skillSelect_proj1-skill4"]').should('be.checked')
        cy.get('[data-cy="skillSelect_proj1-skill5"]').should('be.checked')

        cy.get('[data-cy="importBtn"]').should('be.enabled');
        cy.get('[data-cy="numSelectedSkills"]').should('have.text', '5');

        cy.get('[data-cy="importBtn"]').click();

        cy.get('[data-cy="skillsTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '5');
        cy.get('[data-cy="importedBadge-skill1"]')
        cy.get('[data-cy="importedBadge-skill2"]')
        cy.get('[data-cy="importedBadge-skill3"]')
        cy.get('[data-cy="importedBadge-skill4"]')
        cy.get('[data-cy="importedBadge-skill5"]')

        // only 3 left after import
        cy.get('[data-cy="importFromCatalogBtn"]').click();
        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`).should('have.text', '3');
    });

    it('filter then import', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2, { name: 'Find this 1'});
        cy.createSkill(1, 1, 3);
        cy.createSkill(1, 1, 4, { name: 'Find this 2'});
        cy.createSkill(1, 1, 5, { name: 'Find this 3'});
        cy.exportSkillToCatalog(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 2);
        cy.exportSkillToCatalog(1, 1, 3);
        cy.exportSkillToCatalog(1, 1, 4);
        cy.exportSkillToCatalog(1, 1, 5);

        cy.createProject(2);
        cy.createSubject(2, 1);

        cy.visit('/administrator/projects/proj2/subjects/subj1');
        cy.get('[data-cy="importFromCatalogBtn"]').click();

        cy.get('[data-cy="skillNameFilter"]').type('find{enter}')
        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`).should('have.text', '3')

        cy.get('[data-cy="skillSelect_proj1-skill4"]').check({force: true})
        cy.get('[data-cy="importBtn"]').click();

        cy.get(`[data-cy="skillsTable"] [data-cy="skillsBTableTotalRows"]`).should('have.text', '1')
        cy.get('[data-cy="importedBadge-skill4"]')

    });

    it('remove imported skill should re-appear in the import table', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);
        cy.exportSkillToCatalog(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 2);
        cy.exportSkillToCatalog(1, 1, 3);

        cy.createProject(2);
        cy.createSubject(2, 1);

        cy.importSkillFromCatalog(2, 1, 1, 1)
        cy.importSkillFromCatalog(2, 1, 1, 2)
        cy.importSkillFromCatalog(2, 1, 1, 3)

        cy.visit('/administrator/projects/proj2/subjects/subj1');
        cy.get(`[data-cy="skillsTable"] [data-cy="skillsBTableTotalRows"]`).should('have.text', '3')
        cy.get('[data-cy="importedBadge-skill1"]')
        cy.get('[data-cy="importedBadge-skill2"]')
        cy.get('[data-cy="importedBadge-skill3"]')

        cy.get('[data-cy="importFromCatalogBtn"]').click();
        cy.get('[data-cy="catalogSkillImportModal-NoData"]').contains('Nothing Available for Import')
        cy.get('[data-cy="okButton"]').click();
        cy.get('[data-cy="catalogSkillImportModal-NoData"]').should('not.exist');

        cy.get('[data-cy="deleteSkillButton_skill2"]').click()
        cy.acceptRemovalSafetyCheck()
        cy.get(`[data-cy="skillsTable"] [data-cy="skillsBTableTotalRows"]`).should('have.text', '2')
        cy.get('[data-cy="importedBadge-skill1"]')
        cy.get('[data-cy="importedBadge-skill2"]').should('not.exist')
        cy.get('[data-cy="importedBadge-skill3"]')

        cy.get('[data-cy="importFromCatalogBtn"]').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'Very Great Skill 2' }, { colIndex: 1,  value: 'ID: proj1' }],
        ], 5);

        cy.get('[data-cy="closeButton"]').click();

        cy.get('[data-cy="deleteSkillButton_skill3"]').click()
        cy.acceptRemovalSafetyCheck()
        cy.get(`[data-cy="skillsTable"] [data-cy="skillsBTableTotalRows"]`).should('have.text', '1')
        cy.get('[data-cy="importedBadge-skill1"]')
        cy.get('[data-cy="importedBadge-skill2"]').should('not.exist')
        cy.get('[data-cy="importedBadge-skill3"]').should('not.exist')

        cy.get('[data-cy="deleteSkillButton_skill1"]').click()
        cy.acceptRemovalSafetyCheck()
        cy.contains('No Skills Yet')
        cy.get('[data-cy="importedBadge-skill1"]').should('not.exist')
        cy.get('[data-cy="importedBadge-skill2"]').should('not.exist')
        cy.get('[data-cy="importedBadge-skill3"]').should('not.exist')

        cy.get('[data-cy="importFromCatalogBtn"]').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'Very Great Skill 1' }, { colIndex: 1,  value: 'ID: proj1' }],
            [{ colIndex: 0,  value: 'Very Great Skill 2' }, { colIndex: 1,  value: 'ID: proj1' }],
            [{ colIndex: 0,  value: 'Very Great Skill 3' }, { colIndex: 1,  value: 'ID: proj1' }],
        ], 5);

        // refresh and re-validate
        cy.visit('/administrator/projects/proj2/subjects/subj1');
        cy.get('[data-cy="importFromCatalogBtn"]').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'Very Great Skill 1' }, { colIndex: 1,  value: 'ID: proj1' }],
            [{ colIndex: 0,  value: 'Very Great Skill 2' }, { colIndex: 1,  value: 'ID: proj1' }],
            [{ colIndex: 0,  value: 'Very Great Skill 3' }, { colIndex: 1,  value: 'ID: proj1' }],
        ], 5);
    });

    it('do not allow import if skill id already exist', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.exportSkillToCatalog(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 2);

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 2);
        cy.importSkillFromCatalog(2, 1, 1, 1)

        cy.visit('/administrator/projects/proj2/subjects/subj1');
        // cy.get('[data-cy="importFromCatalogBtn"]').click();
        // cy.get('[data-cy="skillSelect_proj1-skill2"]').check({force: true})
        // cy.get('[data-cy="importBtn"]').should('be.enabled');
        // cy.get('[data-cy="numSelectedSkills"]').should('have.text', '1');
        // cy.get('[data-cy="importBtn"]').click();
        //
        // cy.get('[data-cy="skillsTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '2');
        // cy.get('[data-cy="importedBadge-skill1"]')
        // cy.get('[data-cy="importedBadge-skill2"]')
        //
        // cy.get('[data-cy="importFromCatalogBtn"]').click();
        // cy.get('[data-cy="catalogSkillImportModal-NoData"]').contains('Nothing Available for Import')
    })

    // import duplicate name or id
    // export duplicate name and id
});



