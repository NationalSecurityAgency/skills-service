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

describe('Skills Exported to Catalog Tests', () => {

    beforeEach(() => {
        cy.createProject(1);
        cy.createSubject(1, 1);
    });

    const tableSelector = '[data-cy="exportedSkillsTable"]';

    it('no exported skills', () => {
        cy.visit('/administrator/projects/proj1/skills-catalog');
        cy.get('[data-cy="noExportedSkills"]')
            .contains('No skills exported to Skills Catalog');
        cy.get('[data-cy="exportedSkillsTable"]').should('not.exist')
    });

    it('delete skill', () => {
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSubject(2, 2);
        cy.createSkill(2, 1, 1);
        cy.createSkill(2, 2, 2);
        cy.exportSkillToCatalog(2, 1, 1);
        cy.wait(1001);
        cy.exportSkillToCatalog(2, 2, 2);

        cy.visit('/administrator/projects/proj2/skills-catalog');
        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'Very Great Skill 2 Subj2'
            }, {
                colIndex: 1,
                value: 'Subject 2'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 1'
            }, {
                colIndex: 1,
                value: 'Subject 1'
            }],
        ], 5);

        cy.openDialog('[data-cy="deleteSkillButton_skill1"]')
        cy.get('[data-cy="removalSafetyCheckMsg"]')
            .contains('This will PERMANENTLY remove [Very Great Skill 1] Skill from the catalog. This skill is currently imported by 0 projects.');
        cy.get('[data-cy="removalSafetyCheckMsg"]')
            .contains('This action CANNOT be undone')
            .should('not.exist');
        cy.get('[data-cy="saveDialogBtn"]')
            .should('be.disabled');
        cy.get('[data-cy="currentValidationText"]')
            .type('Delete Me1');
        cy.get('[data-cy="currentValidationText"]')
            .should('have.value', 'Delete Me1');
        cy.get('[data-cy="saveDialogBtn"]')
            .should('be.disabled');
        cy.get('[data-cy="currentValidationText"]')
            .type('{backspace}');
        cy.clickSaveDialogBtn()
        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'Very Great Skill 2 Subj2'
            }, {
                colIndex: 1,
                value: 'Subject 2'
            }],
        ], 5);

        // refresh and validate
        cy.visit('/administrator/projects/proj2/skills-catalog');
        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'Very Great Skill 2 Subj2'
            }, {
                colIndex: 1,
                value: 'Subject 2'
            }],
        ], 5);
    });

    it('delete last skill', () => {
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSubject(2, 2);
        cy.createSkill(2, 1, 1);
        cy.exportSkillToCatalog(2, 1, 1);
        cy.wait(1001);

        cy.visit('/administrator/projects/proj2/skills-catalog');
        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'Very Great Skill 1'
            }, {
                colIndex: 1,
                value: 'Subject 1'
            }],
        ], 5);

        cy.openDialog('[data-cy="deleteSkillButton_skill1"]')
        cy.get('[data-cy="removalSafetyCheckMsg"]')
            .contains('This will PERMANENTLY remove [Very Great Skill 1] Skill from the catalog. This skill is currently imported by 0 projects.');
        cy.get('[data-cy="saveDialogBtn"]')
            .should('be.disabled');
        cy.get('[data-cy="currentValidationText"]')
            .type('Delete Me1');
        cy.get('[data-cy="currentValidationText"]')
            .should('have.value', 'Delete Me1');
        cy.get('[data-cy="saveDialogBtn"]')
            .should('be.disabled');
        cy.get('[data-cy="currentValidationText"]')
            .type('{backspace}');
        cy.clickSaveDialogBtn()

        cy.get('[data-cy="noExportedSkills"]')
          .contains('No skills exported to Skills Catalog');
        cy.get('[data-cy="exportedSkillsTable"]').should('not.exist')

        // refresh and validate
        cy.visit('/administrator/projects/proj2/skills-catalog');
        cy.get('[data-cy="noExportedSkills"]')
          .contains('No skills exported to Skills Catalog');
        cy.get('[data-cy="exportedSkillsTable"]').should('not.exist')
    });

    it('delete skill with url encoded id', () => {
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSubject(2, 2);
        const skillId = 'tm_eafeafeafeafeSkill%2Ddlajleajljelajelkajlajle';
        cy.createSkill(2, 1, 1, { skillId });
        cy.createSkill(2, 2, 2);
        const exportUrl = `/admin/projects/proj2/skills/${encodeURIComponent(skillId)}/export`;
        cy.request('POST', exportUrl);
        cy.wait(1001);
        cy.exportSkillToCatalog(2, 2, 2);

        cy.visit('/administrator/projects/proj2/skills-catalog');
        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'Very Great Skill 2 Subj2'
            }, {
                colIndex: 1,
                value: 'Subject 2'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 1'
            }, {
                colIndex: 1,
                value: 'Subject 1'
            }],
        ], 5);

        cy.openDialog(`[data-cy="deleteSkillButton_${skillId}"]`)
        cy.get('[data-cy="removalSafetyCheckMsg"]')
            .contains(`This will PERMANENTLY remove [Very Great Skill 1] Skill from the catalog. This skill is currently imported by 0 projects.`);
        cy.get('[data-cy="saveDialogBtn"]')
            .should('be.disabled');
        cy.get('[data-cy="currentValidationText"]')
            .type('Delete Me1');
        cy.get('[data-cy="currentValidationText"]')
            .should('have.value', 'Delete Me1');
        cy.get('[data-cy="saveDialogBtn"]')
            .should('be.disabled');
        cy.get('[data-cy="currentValidationText"]')
            .type('{backspace}');
        cy.clickSaveDialogBtn()
        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'Very Great Skill 2 Subj2'
            }, {
                colIndex: 1,
                value: 'Subject 2'
            }],
        ], 5);

        // refresh and validate
        cy.visit('/administrator/projects/proj2/skills-catalog');
        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'Very Great Skill 2 Subj2'
            }, {
                colIndex: 1,
                value: 'Subject 2'
            }],
        ], 5);
    });

    it('Delete warning informs user how many projects imported this skill', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.exportSkillToCatalog(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 2);

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.importSkillFromCatalog(2, 1, 1, 1);

        cy.createProject(3);
        cy.createSubject(3, 1);
        cy.importSkillFromCatalog(3, 1, 1, 1);
        cy.importSkillFromCatalog(3, 1, 1, 2);

        cy.visit('/administrator/projects/proj1/skills-catalog');
        cy.get('[data-cy="deleteSkillButton_skill1"]')
            .click();
        cy.get('[data-cy="removalSafetyCheckMsg"]')
            .contains('This will PERMANENTLY remove [Very Great Skill 1] Skill from the catalog. This skill is currently imported by 2 projects.');
        cy.get('[data-cy="saveDialogBtn"]')
            .should('be.disabled');

        cy.get('[data-cy="closeDialogBtn"]')
            .click();
        cy.get('[data-cy="deleteSkillButton_skill2"]')
            .click();
        cy.get('[data-cy="removalSafetyCheckMsg"]')
            .contains('This will PERMANENTLY remove [Very Great Skill 2] Skill from the catalog. This skill is currently imported by 1 project.');
        cy.get('[data-cy="saveDialogBtn"]')
            .should('be.disabled');
    });

    it('Exported skill sorting and paging', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);
        cy.createSkill(1, 1, 4);
        cy.exportSkillToCatalog(1, 1, 1);
        cy.wait(100);
        cy.exportSkillToCatalog(1, 1, 2);
        cy.wait(100);
        cy.exportSkillToCatalog(1, 1, 3);
        cy.wait(100);
        cy.exportSkillToCatalog(1, 1, 4);
        cy.wait(100);

        cy.createSubject(1, 2);
        cy.createSkill(1, 2, 5);
        cy.createSkill(1, 2, 6);
        cy.createSkill(1, 2, 7);
        cy.exportSkillToCatalog(1, 2, 5);
        cy.wait(100);
        cy.exportSkillToCatalog(1, 2, 6);
        cy.wait(100);
        cy.exportSkillToCatalog(1, 2, 7);

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.importSkillFromCatalog(2, 1, 1, 1);
        cy.finalizeCatalogImport(2);

        cy.visit('/administrator/projects/proj1/skills-catalog');
        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'Very Great Skill 7 Subj2'
            }, {
                colIndex: 1,
                value: 'Subject 2'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 6 Subj2'
            }, {
                colIndex: 1,
                value: 'Subject 2'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 5 Subj2'
            }, {
                colIndex: 1,
                value: 'Subject 2'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 4'
            }, {
                colIndex: 1,
                value: 'Subject 1'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 3'
            }, {
                colIndex: 1,
                value: 'Subject 1'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 2'
            }, {
                colIndex: 1,
                value: 'Subject 1'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 1'
            }, {
                colIndex: 1,
                value: 'Subject 1'
            }],
        ], 5);

        cy.get(`${tableSelector} th`)
            .contains('Exported On')
            .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'Very Great Skill 1'
            }, {
                colIndex: 1,
                value: 'Subject 1'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 2'
            }, {
                colIndex: 1,
                value: 'Subject 1'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 3'
            }, {
                colIndex: 1,
                value: 'Subject 1'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 4'
            }, {
                colIndex: 1,
                value: 'Subject 1'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 5 Subj2'
            }, {
                colIndex: 1,
                value: 'Subject 2'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 6 Subj2'
            }, {
                colIndex: 1,
                value: 'Subject 2'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 7 Subj2'
            }, {
                colIndex: 1,
                value: 'Subject 2'
            }],
        ], 5);

        cy.get(`${tableSelector} th`)
            .contains('Skill')
            .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'Very Great Skill 1'
            }, {
                colIndex: 1,
                value: 'Subject 1'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 2'
            }, {
                colIndex: 1,
                value: 'Subject 1'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 3'
            }, {
                colIndex: 1,
                value: 'Subject 1'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 4'
            }, {
                colIndex: 1,
                value: 'Subject 1'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 5 Subj2'
            }, {
                colIndex: 1,
                value: 'Subject 2'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 6 Subj2'
            }, {
                colIndex: 1,
                value: 'Subject 2'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 7 Subj2'
            }, {
                colIndex: 1,
                value: 'Subject 2'
            }],
        ], 5);

        cy.get(`${tableSelector} th`)
            .contains('Skill')
            .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'Very Great Skill 7 Subj2'
            }, {
                colIndex: 1,
                value: 'Subject 2'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 6 Subj2'
            }, {
                colIndex: 1,
                value: 'Subject 2'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 5 Subj2'
            }, {
                colIndex: 1,
                value: 'Subject 2'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 4'
            }, {
                colIndex: 1,
                value: 'Subject 1'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 3'
            }, {
                colIndex: 1,
                value: 'Subject 1'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 2'
            }, {
                colIndex: 1,
                value: 'Subject 1'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 1'
            }, {
                colIndex: 1,
                value: 'Subject 1'
            }],
        ], 5);

        cy.get(`${tableSelector} th`)
            .contains('Projects Imported')
            .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 2,
                value: '0'
            }],
            [{
                colIndex: 2,
                value: '0'
            }],
            [{
                colIndex: 2,
                value: '0'
            }],
            [{
                colIndex: 2,
                value: '0'
            }],
            [{
                colIndex: 2,
                value: '0'
            }],
            [{
                colIndex: 2,
                value: '0'
            }],
            [{
                colIndex: 2,
                value: '1'
            }],
        ], 5);

        cy.get(`${tableSelector} th`)
            .contains('Projects Imported')
            .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 2,
                value: '1'
            }],
            [{
                colIndex: 2,
                value: '0'
            }],
            [{
                colIndex: 2,
                value: '0'
            }],
            [{
                colIndex: 2,
                value: '0'
            }],
            [{
                colIndex: 2,
                value: '0'
            }],
            [{
                colIndex: 2,
                value: '0'
            }],
            [{
                colIndex: 2,
                value: '0'
            }],
        ], 5);
    });


    it('table sort is saved in local storage', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);
        cy.createSkill(1, 1, 4);
        cy.exportSkillToCatalog(1, 1, 1);
        cy.wait(100);
        cy.exportSkillToCatalog(1, 1, 2);
        cy.wait(100);
        cy.exportSkillToCatalog(1, 1, 3);
        cy.wait(100);
        cy.exportSkillToCatalog(1, 1, 4);
        cy.wait(100);

        cy.createSubject(1, 2);
        cy.createSkill(1, 2, 5);
        cy.createSkill(1, 2, 6);
        cy.createSkill(1, 2, 7);
        cy.exportSkillToCatalog(1, 2, 5);
        cy.wait(100);
        cy.exportSkillToCatalog(1, 2, 6);
        cy.wait(100);
        cy.exportSkillToCatalog(1, 2, 7);

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.importSkillFromCatalog(2, 1, 1, 1);
        cy.finalizeCatalogImport(2);

        cy.visit('/administrator/projects/proj1/skills-catalog');
        cy.get(`${tableSelector} th`)
          .contains('Skill')
          .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'Very Great Skill 1'
            }, {
                colIndex: 1,
                value: 'Subject 1'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 2'
            }, {
                colIndex: 1,
                value: 'Subject 1'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 3'
            }, {
                colIndex: 1,
                value: 'Subject 1'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 4'
            }, {
                colIndex: 1,
                value: 'Subject 1'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 5 Subj2'
            }, {
                colIndex: 1,
                value: 'Subject 2'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 6 Subj2'
            }, {
                colIndex: 1,
                value: 'Subject 2'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 7 Subj2'
            }, {
                colIndex: 1,
                value: 'Subject 2'
            }],
        ], 5);

        // refresh and validate
        cy.visit('/administrator/projects/proj1/skills-catalog');
        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'Very Great Skill 1'
            }, {
                colIndex: 1,
                value: 'Subject 1'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 2'
            }, {
                colIndex: 1,
                value: 'Subject 1'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 3'
            }, {
                colIndex: 1,
                value: 'Subject 1'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 4'
            }, {
                colIndex: 1,
                value: 'Subject 1'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 5 Subj2'
            }, {
                colIndex: 1,
                value: 'Subject 2'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 6 Subj2'
            }, {
                colIndex: 1,
                value: 'Subject 2'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 7 Subj2'
            }, {
                colIndex: 1,
                value: 'Subject 2'
            }],
        ], 5);
    });


    it('Select larger page', () => {
        cy.intercept('/admin/projects/proj1/skills/exported?*').as('loadExportedSkills')
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
        cy.wait('@loadExportedSkills');
        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`)
            .should('have.text', '11');
        cy.get(`${tableSelector} [data-pc-name="pcrowperpagedropdown"]`).click()
        cy.get('[data-pc-section="list"] [aria-label="10"]').click()
        cy.wait('@loadExportedSkills');
        cy.get(`${tableSelector} table tbody [role="row"]`).should('have.length', 10)

        cy.get(`${tableSelector} th`)
            .contains('Skill')
            .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'Very Great Skill 1'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 10 Subj2'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 11 Subj2'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 2'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 3'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 4'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 5 Subj2'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 6 Subj2'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 7 Subj2'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 8 Subj2'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 9 Subj2'
            }],
        ], 10);

        cy.get(`${tableSelector} [data-pc-name="pcrowperpagedropdown"]`).click()
        cy.get('[data-pc-section="list"] [aria-label="25"]').click()
        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'Very Great Skill 1'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 10 Subj2'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 11 Subj2'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 2'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 3'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 4'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 5 Subj2'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 6 Subj2'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 7 Subj2'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 8 Subj2'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 9 Subj2'
            }],
        ], 25);
    });

    it('Change exported skills attributes - imported attributes are updated', () => {
        cy.intercept('POST', '/admin/projects/proj1/subjects/subj1/skills/skill1')
            .as('saveSkill1');

        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.exportSkillToCatalog(1, 1, 1);

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.importSkillFromCatalog(2, 1, 1, 1);
        cy.finalizeCatalogImport(2);

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="editSkillButton_skill1"]')
            .click();
        cy.get('[data-cy="skillName"]')
            .type('A');
        cy.get('[data-cy="pointIncrement"]')
            .clear()
            .type('66');
        cy.get('[data-cy="numPerformToCompletion"]')
            .clear()
            .type('7');
        cy.clickSaveDialogBtn()
        cy.wait('@saveSkill1');
        cy.get('[data-cy="nameCell_skill1"]')
            .contains('Very Great Skill 1A');
        cy.get('[data-cy="skillsTable-additionalColumns"] [data-pc-section="dropdownicon"]').click()
        cy.get('[data-pc-section="overlay"] [aria-label="Points"]').click()
        cy.get('[data-cy="skillsTable-additionalColumns"] [data-pc-section="dropdownicon"]').click()
        cy.get('[data-cy="totalPointsCell_skill1"]')
            .contains('66 pts x 7 repetitions');

        // now let's check the imported skill
        cy.get('[data-cy="breadcrumb-Projects"]')
            .click();
        cy.get('[data-cy="projCard_proj2_manageLink"]')
            .click();
        cy.waitForBackendAsyncTasksToComplete();
        cy.get('[data-cy="manageBtn_subj1"]')
            .click();
        // occurrences are synced but not points
        cy.get('[data-cy="totalPointsCell_skill1"]')
            .contains('100 pts x 7 repetitions');
        cy.get('[data-cy="nameCell_skill1"]')
            .contains('Very Great Skill 1A');
    });

    it('View imported details for exported skills', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);
        cy.exportSkillToCatalog(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 2);
        cy.exportSkillToCatalog(1, 1, 3);

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.importSkillFromCatalog(2, 1, 1, 1);
        cy.finalizeCatalogImport(2);

        cy.createProject(3);
        cy.createSubject(3, 1);
        cy.importSkillFromCatalog(3, 1, 1, 3);

        cy.visit('/administrator/projects/proj1');
        cy.get('[data-cy="nav-Skill Catalog"]')
            .click();
        cy.validateTable('[data-cy="exportedSkillsTable"]', [
            [{
                colIndex: 1,
                value: 'Very Great Skill 3'
            }, {
                colIndex: 2,
                value: '1'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 2'
            }, {
                colIndex: 2,
                value: '0'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 1'
            }, {
                colIndex: 2,
                value: '1'
            }],
        ], 5);
        cy.get(`${tableSelector} [data-p-index="1"] [data-pc-section="rowtogglebutton"]`).click()
        cy.get('[data-cy="importSkillInfo-proj1_skill2"')
            .contains('This skill has not been imported by any other projects yet...');

        cy.get(`${tableSelector} [data-p-index="2"] [data-pc-section="rowtogglebutton"]`).click()
        cy.get('[data-cy="importSkillInfo-proj1_skill1"] [data-cy="importedSkillsTable"]')
            .should('exist');
        cy.validateTable('[data-cy="importSkillInfo-proj1_skill1"] [data-cy="importedSkillsTable"]', [
            [{
                colIndex: 0,
                value: 'This is project 2'
            }],
        ], 5, true, null, false);

        cy.get(`${tableSelector} [data-p-index="0"] [data-pc-section="rowtogglebutton"]`).click()
        cy.get('[data-cy="importSkillInfo-proj1_skill3"] [data-cy="importedSkillsTable"]')
            .should('exist');
        cy.validateTable('[data-cy="importSkillInfo-proj1_skill3"] [data-cy="importedSkillsTable"]', [
            [{
                colIndex: 0,
                value: 'This is project 3Disabled'
            }],
        ], 5, true, null, false);
    });

    it('contact project owner for exported skill', () => {
        cy.intercept('POST', '/api/projects/*/contact').as('contact')
        cy.loginAsAdminUser()
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)
        cy.createSkill(1, 1, 3)
        cy.exportSkillToCatalog(1, 1, 1)
        cy.exportSkillToCatalog(1, 1, 2)
        cy.exportSkillToCatalog(1, 1, 3)
        cy.createProject(2)
        cy.createSubject(2, 1)
        cy.importSkillFromCatalog(2, 1, 1, 1)
        cy.createProject(3)
        cy.createSubject(3, 1)
        cy.importSkillFromCatalog(3, 1, 1, 3)
        cy.visit('/administrator/projects/proj1')
        cy.get('[data-cy="nav-Skill Catalog"]')
          .click()
        cy.get('[data-cy="exportedSkillsTable"] [data-cy="skillsBTableTotalRows"]')
          .should('have.text', '3')

        cy.get(`[data-cy="exportedSkillsTable"] [data-p-index="2"] [data-pc-section="rowtogglebutton"]`).click()
        cy.get('[data-cy="importSkillInfo-proj1_skill1"] [data-cy="importedSkillsTable"]')
          .should('exist')
        cy.validateTable('[data-cy="importSkillInfo-proj1_skill1"] [data-cy="importedSkillsTable"]', [
            [{
                colIndex: 0,
                value: 'This is project 2'
            }]
        ], 5, true, null, false)
        cy.openDialog('[data-cy="contactOwnerBtn_proj2"]', true)
        cy.get('[data-cy="contactProjectOwnerDialog"]').should('exist')
        cy.get('[data-cy="contactOwnersMsgInput"]').type('aaa bbb this is a message')
        cy.get('[data-cy="messageNumCharsRemaining"]').should('contain.text', '2,475 characters remaining')
        cy.clickSaveDialogBtn()
        cy.wait('@contact')
        cy.get('[data-cy="closeDialogBtn"]').should('contain.text', 'OK')
        cy.get('[data-cy="contactOwnerSuccessMsg"]').should('contain.text', 'Message sent!')
        cy.get('[data-cy="contactOwnerSuccessMsg"]').should('contain.text', 'The Project Administrator(s) of This is project 2 will be notified of your question via email.')
        cy.get('[data-cy="closeDialogBtn"]').click()
        cy.wait(500) //wait for animations to complete
        cy.get('[data-cy="contactProjectOwnerDialog"]').should('not.exist')
        cy.getEmails().then((emails) => {
            expect(emails[0].text).to.contain('This is project 2')
        })
    })
});


