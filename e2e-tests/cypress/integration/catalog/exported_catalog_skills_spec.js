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

describe('Skills Exported to Catalog Tests', () => {

    beforeEach(() => {
        cy.createProject(1);
        cy.createSubject(1, 1);
    });

    const tableSelector = '[data-cy="exportedSkillsTable"]'

    it('no exported skills', () => {
        cy.visit('/administrator/projects/proj1/skills-catalog');
        cy.get('[data-cy="exportedSkillsTable"]').contains('There are no records to show')
    });

    it('delete skill', () => {
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSubject(2, 2);
        cy.createSkill(2, 1, 1);
        cy.createSkill(2, 2, 2);
        cy.exportSkillToCatalog(2, 1, 1);
        cy.wait(1001)
        cy.exportSkillToCatalog(2, 2, 2);

        cy.visit('/administrator/projects/proj2/skills-catalog');
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'Very Great Skill 2 Subj2' }, { colIndex: 1,  value: 'Subject 2' }],
            [{ colIndex: 0,  value: 'Very Great Skill 1' }, { colIndex: 1,  value: 'Subject 1' }],
        ], 5);

        cy.get('[data-cy="deleteSkillButton_skill1"]').click();
        cy.get('[data-cy="removalSafetyCheckMsg"]').contains('This will PERMANENTLY remove skill [skill1] from the catalog. This skill is currently imported by 0 projects.')
        cy.get('[data-cy="removeButton"]').should('be.disabled');
        cy.get('[data-cy="currentValidationText"]').type('Delete Me1');
        cy.get('[data-cy="currentValidationText"]').should('have.value', 'Delete Me1');
        cy.get('[data-cy="removeButton"]').should('be.disabled');
        cy.get('[data-cy="currentValidationText"]').type('{backspace}');
        cy.get('[data-cy="removeButton"]').should('be.enabled');
        cy.get('[data-cy="removeButton"]').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'Very Great Skill 2 Subj2' }, { colIndex: 1,  value: 'Subject 2' }],
        ], 5);

        // refresh and validate
        cy.visit('/administrator/projects/proj2/skills-catalog');
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'Very Great Skill 2 Subj2' }, { colIndex: 1,  value: 'Subject 2' }],
        ], 5);
    });

    it('Delete warning informs user how many projects imported this skill', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.exportSkillToCatalog(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 2);

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.importSkillFromCatalog(2, 1, 1, 1)

        cy.createProject(3);
        cy.createSubject(3, 1);
        cy.importSkillFromCatalog(3, 1, 1, 1)
        cy.importSkillFromCatalog(3, 1, 1, 2)

        cy.visit('/administrator/projects/proj1/skills-catalog');
        cy.get('[data-cy="deleteSkillButton_skill1"]').click();
        cy.get('[data-cy="removalSafetyCheckMsg"]').contains('This will PERMANENTLY remove skill [skill1] from the catalog. This skill is currently imported by 2 projects.')
        cy.get('[data-cy="removeButton"]').should('be.disabled');

        cy.get('[data-cy="closeRemovalSafetyCheck"]').click();
        cy.get('[data-cy="deleteSkillButton_skill2"]').click();
        cy.get('[data-cy="removalSafetyCheckMsg"]').contains('This will PERMANENTLY remove skill [skill2] from the catalog. This skill is currently imported by 1 projects.')
        cy.get('[data-cy="removeButton"]').should('be.disabled');
    });

    it('Exported skill sorting and paging', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);
        cy.createSkill(1, 1, 4);
        cy.exportSkillToCatalog(1, 1, 1);
        cy.wait(100)
        cy.exportSkillToCatalog(1, 1, 2);
        cy.wait(100)
        cy.exportSkillToCatalog(1, 1, 3);
        cy.wait(100)
        cy.exportSkillToCatalog(1, 1, 4);
        cy.wait(100)

        cy.createSubject(1, 2);
        cy.createSkill(1, 2, 5);
        cy.createSkill(1, 2, 6);
        cy.createSkill(1, 2, 7);
        cy.exportSkillToCatalog(1, 2, 5);
        cy.wait(100)
        cy.exportSkillToCatalog(1, 2, 6);
        cy.wait(100)
        cy.exportSkillToCatalog(1, 2, 7);

        cy.visit('/administrator/projects/proj1/skills-catalog');
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'Very Great Skill 7 Subj2' }, { colIndex: 1,  value: 'Subject 2' }],
            [{ colIndex: 0,  value: 'Very Great Skill 6 Subj2' }, { colIndex: 1,  value: 'Subject 2' }],
            [{ colIndex: 0,  value: 'Very Great Skill 5 Subj2' }, { colIndex: 1,  value: 'Subject 2' }],
            [{ colIndex: 0,  value: 'Very Great Skill 4' }, { colIndex: 1,  value: 'Subject 1' }],
            [{ colIndex: 0,  value: 'Very Great Skill 3' }, { colIndex: 1,  value: 'Subject 1' }],
            [{ colIndex: 0,  value: 'Very Great Skill 2' }, { colIndex: 1,  value: 'Subject 1' }],
            [{ colIndex: 0,  value: 'Very Great Skill 1' }, { colIndex: 1,  value: 'Subject 1' }],
        ], 5);

        cy.get(`${tableSelector} th`).contains('Exported On').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'Very Great Skill 1' }, { colIndex: 1,  value: 'Subject 1' }],
            [{ colIndex: 0,  value: 'Very Great Skill 2' }, { colIndex: 1,  value: 'Subject 1' }],
            [{ colIndex: 0,  value: 'Very Great Skill 3' }, { colIndex: 1,  value: 'Subject 1' }],
            [{ colIndex: 0,  value: 'Very Great Skill 4' }, { colIndex: 1,  value: 'Subject 1' }],
            [{ colIndex: 0,  value: 'Very Great Skill 5 Subj2' }, { colIndex: 1,  value: 'Subject 2' }],
            [{ colIndex: 0,  value: 'Very Great Skill 6 Subj2' }, { colIndex: 1,  value: 'Subject 2' }],
            [{ colIndex: 0,  value: 'Very Great Skill 7 Subj2' }, { colIndex: 1,  value: 'Subject 2' }],
        ], 5);

        cy.get(`${tableSelector} th`).contains('Skill').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'Very Great Skill 1' }, { colIndex: 1,  value: 'Subject 1' }],
            [{ colIndex: 0,  value: 'Very Great Skill 2' }, { colIndex: 1,  value: 'Subject 1' }],
            [{ colIndex: 0,  value: 'Very Great Skill 3' }, { colIndex: 1,  value: 'Subject 1' }],
            [{ colIndex: 0,  value: 'Very Great Skill 4' }, { colIndex: 1,  value: 'Subject 1' }],
            [{ colIndex: 0,  value: 'Very Great Skill 5 Subj2' }, { colIndex: 1,  value: 'Subject 2' }],
            [{ colIndex: 0,  value: 'Very Great Skill 6 Subj2' }, { colIndex: 1,  value: 'Subject 2' }],
            [{ colIndex: 0,  value: 'Very Great Skill 7 Subj2' }, { colIndex: 1,  value: 'Subject 2' }],
        ], 5);

        cy.get(`${tableSelector} th`).contains('Skill').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'Very Great Skill 7 Subj2' }, { colIndex: 1,  value: 'Subject 2' }],
            [{ colIndex: 0,  value: 'Very Great Skill 6 Subj2' }, { colIndex: 1,  value: 'Subject 2' }],
            [{ colIndex: 0,  value: 'Very Great Skill 5 Subj2' }, { colIndex: 1,  value: 'Subject 2' }],
            [{ colIndex: 0,  value: 'Very Great Skill 4' }, { colIndex: 1,  value: 'Subject 1' }],
            [{ colIndex: 0,  value: 'Very Great Skill 3' }, { colIndex: 1,  value: 'Subject 1' }],
            [{ colIndex: 0,  value: 'Very Great Skill 2' }, { colIndex: 1,  value: 'Subject 1' }],
            [{ colIndex: 0,  value: 'Very Great Skill 1' }, { colIndex: 1,  value: 'Subject 1' }],
        ], 5);

        cy.get(`${tableSelector} th`).contains('Subject').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'Subject 1' }],
            [{ colIndex: 1,  value: 'Subject 1' }],
            [{ colIndex: 1,  value: 'Subject 1' }],
            [{ colIndex: 1,  value: 'Subject 1' }],
            [{ colIndex: 1,  value: 'Subject 2' }],
            [{ colIndex: 1,  value: 'Subject 2' }],
            [{ colIndex: 1,  value: 'Subject 2' }],
        ], 5);

        cy.get(`${tableSelector} th`).contains('Subject').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'Subject 2' }],
            [{ colIndex: 1,  value: 'Subject 2' }],
            [{ colIndex: 1,  value: 'Subject 2' }],
            [{ colIndex: 1,  value: 'Subject 1' }],
            [{ colIndex: 1,  value: 'Subject 1' }],
            [{ colIndex: 1,  value: 'Subject 1' }],
            [{ colIndex: 1,  value: 'Subject 1' }],
        ], 5);
    });

    it('Select larger page', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);
        cy.createSkill(1, 1, 4);
        cy.exportSkillToCatalog(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 2);
        cy.exportSkillToCatalog(1, 1, 3);
        cy.exportSkillToCatalog(1, 1, 4);

        cy.createSubject(1, 2);
        cy.createSkill(1, 2, 5);
        cy.createSkill(1, 2, 6);
        cy.createSkill(1, 2, 7);
        cy.createSkill(1, 2, 8);
        cy.createSkill(1, 2, 9);
        cy.createSkill(1, 2, 10);
        cy.createSkill(1, 2, 11);
        cy.exportSkillToCatalog(1, 2, 5);
        cy.exportSkillToCatalog(1, 2, 6);
        cy.exportSkillToCatalog(1, 2, 7);
        cy.exportSkillToCatalog(1, 2, 8);
        cy.exportSkillToCatalog(1, 2, 9);
        cy.exportSkillToCatalog(1, 2, 10);
        cy.exportSkillToCatalog(1, 2, 11);

        cy.visit('/administrator/projects/proj1/skills-catalog');
        cy.get(`${tableSelector} [data-cy="skillsBTablePageSize"]`).select('10');

        cy.get(`${tableSelector} th`).contains('Subject').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'Subject 1' }],
            [{ colIndex: 1,  value: 'Subject 1' }],
            [{ colIndex: 1,  value: 'Subject 1' }],
            [{ colIndex: 1,  value: 'Subject 1' }],
            [{ colIndex: 1,  value: 'Subject 2' }],
            [{ colIndex: 1,  value: 'Subject 2' }],
            [{ colIndex: 1,  value: 'Subject 2' }],
            [{ colIndex: 1,  value: 'Subject 2' }],
            [{ colIndex: 1,  value: 'Subject 2' }],
            [{ colIndex: 1,  value: 'Subject 2' }],
            [{ colIndex: 1,  value: 'Subject 2' }],
        ], 10);

        cy.get(`${tableSelector} [data-cy="skillsBTablePageSize"]`).select('25');
        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'Subject 1' }],
            [{ colIndex: 1,  value: 'Subject 1' }],
            [{ colIndex: 1,  value: 'Subject 1' }],
            [{ colIndex: 1,  value: 'Subject 1' }],
            [{ colIndex: 1,  value: 'Subject 2' }],
            [{ colIndex: 1,  value: 'Subject 2' }],
            [{ colIndex: 1,  value: 'Subject 2' }],
            [{ colIndex: 1,  value: 'Subject 2' }],
            [{ colIndex: 1,  value: 'Subject 2' }],
            [{ colIndex: 1,  value: 'Subject 2' }],
            [{ colIndex: 1,  value: 'Subject 2' }],
        ], 25);
    })
});

