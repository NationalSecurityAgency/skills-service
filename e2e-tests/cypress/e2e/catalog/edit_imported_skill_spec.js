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

describe('Edit Imported Skill Tests', () => {

    beforeEach(() => {
        cy.createProject(1);
        cy.createSubject(1, 1);
    });

    const tableSelector = '[data-cy="skillsTable"]';

    it('edit point increment of an imported skill', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.exportSkillToCatalog(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 2);

        cy.createProject(2);
        cy.createSubject(2, 1);

        cy.importSkillFromCatalog(2, 1, 1, 1);
        cy.wait(1000);
        cy.importSkillFromCatalog(2, 1, 1, 2);

        cy.visit('/administrator/projects/proj2/subjects/subj1');
        cy.get('[data-cy="editSkillButton_skill1"]')
            .click();
        cy.contains('You can change the Point Increment');
        cy.get('[data-cy="pointIncrement"] input')
            .should('have.value', '100');
        cy.get('[data-cy="pointIncrement"]')
            .type('1');
        cy.clickSaveDialogBtn()

        cy.get('[data-cy="skillsTable-additionalColumns"] [data-pc-section="dropdownicon"]').click()
        cy.get('[data-pc-section="overlay"] [aria-label="Points"]').click()
        cy.get('[data-cy="skillsTable-additionalColumns"] [data-pc-section="dropdownicon"]').click()
        cy.validateTable(tableSelector, [
            [{
                colIndex: 2,
                value: 'Very Great Skill 2'
            }, {
                colIndex: 5,
                value: '200'
            }],
            [{
                colIndex: 2,
                value: 'Very Great Skill 1'
            }, {
                colIndex: 5,
                value: '2,002'
            }],
        ], 5, false, null, false);
        cy.get(`${tableSelector} tbody tr`)
            .should('have.length', 2);

        cy.get('[data-cy="editSkillButton_skill1"]')
            .click();
        cy.contains('You can change the Point Increment');
        cy.get('[data-cy="pointIncrement"] input')
            .should('have.value', '1,001');

        // refresh re-validate
        cy.visit('/administrator/projects/proj2/subjects/subj1');

        cy.get(`[data-cy="skillsTable-additionalColumns"]`)
            .contains('Points')
            .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 2,
                value: 'Very Great Skill 2'
            }, {
                colIndex: 5,
                value: '200'
            }],
            [{
                colIndex: 2,
                value: 'Very Great Skill 1'
            }, {
                colIndex: 5,
                value: '2,002'
            }],
        ], 5, false, null, false);
        cy.get(`${tableSelector} tbody tr`)
            .should('have.length', 2);

        cy.get('[data-cy="editSkillButton_skill1"]')
            .click();
        cy.contains('You can change the Point Increment');
        cy.get('[data-cy="pointIncrement"] input')
            .should('have.value', '1,001');
    });

    it('point increment input validation', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.exportSkillToCatalog(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 2);

        cy.createProject(2);
        cy.createSubject(2, 1);

        cy.importSkillFromCatalog(2, 1, 1, 1);
        cy.importSkillFromCatalog(2, 1, 1, 2);

        cy.visit('/administrator/projects/proj2/subjects/subj1');
        cy.get('[data-cy="editSkillButton_skill1"]')
            .click();
        cy.get('[data-cy="pointIncrement"] input')
            .should('have.value', '100');

        // input won't allow chars
        cy.get('[data-cy="pointIncrement"]')
            .type('a');
        cy.get('[data-cy="pointIncrement"] input')
          .should('have.value', '100');

        cy.get('[data-cy="pointIncrement"] input')
            .clear();
        cy.get('[data-cy="pointIncrementError"]')
            .contains('Point Increment is a required field');
        cy.get('[data-cy="saveDialogBtn"]')
            .should('be.disabled');

        cy.get('[data-cy="pointIncrement"]')
            .type('10001');
        cy.get('[data-cy="pointIncrementError"]')
            .contains('Point Increment must be less than or equal to 10000');
        cy.get('[data-cy="saveDialogBtn"]')
            .should('be.disabled');

        cy.get('[data-cy="pointIncrement"] input')
            .clear()
            .type('10000');
        cy.get('[data-cy="pointIncrementError"]')
            .should('not.exist');
        cy.get('[data-cy="saveDialogBtn"]')
            .should('be.enabled');
    });

    it('cancel edit', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.exportSkillToCatalog(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 2);

        cy.createProject(2);
        cy.createSubject(2, 1);

        cy.importSkillFromCatalog(2, 1, 1, 1);
        cy.wait(1000);
        cy.importSkillFromCatalog(2, 1, 1, 2);

        cy.visit('/administrator/projects/proj2/subjects/subj1');
        cy.get('[data-cy="editSkillButton_skill1"]')
            .click();
        cy.get('[data-cy="pointIncrement"]')
            .type('1');
        cy.get('[data-cy="closeDialogBtn"]')
            .click();
        cy.get('[data-cy="skillsTable-additionalColumns"] [data-pc-section="dropdownicon"]').click()
        cy.get('[data-pc-section="overlay"] [aria-label="Points"]').click()
        cy.get('[data-cy="skillsTable-additionalColumns"] [data-pc-section="dropdownicon"]').click()
        cy.validateTable(tableSelector, [
            [{
                colIndex: 2,
                value: 'Very Great Skill 2'
            }, {
                colIndex: 5,
                value: '200'
            }],
            [{
                colIndex: 2,
                value: 'Very Great Skill 1'
            }, {
                colIndex: 5,
                value: '200'
            }],
        ], 5, false, null, false);
        cy.get(`${tableSelector} tbody tr`)
            .should('have.length', 2);

        cy.get('[data-cy="editSkillButton_skill1"]')
            .click();
        cy.get('[data-cy="pointIncrement"]')
            .type('1');
        cy.get(' [aria-label="Close"]')
            .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 2,
                value: 'Very Great Skill 2'
            }, {
                colIndex: 5,
                value: '200'
            }],
            [{
                colIndex: 2,
                value: 'Very Great Skill 1'
            }, {
                colIndex: 5,
                value: '200'
            }],
        ], 5, false, null, false);
        cy.get(`${tableSelector} tbody tr`)
            .should('have.length', 2);
    });

    it('edit after finalizing', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.exportSkillToCatalog(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 2);

        cy.createProject(2);
        cy.createSubject(2, 1);

        cy.importSkillFromCatalog(2, 1, 1, 1);
        cy.wait(1000);
        cy.importSkillFromCatalog(2, 1, 1, 2);

        cy.finalizeCatalogImport(2);

        cy.visit('/administrator/projects/proj2/subjects/subj1');
        cy.get('[data-cy="editSkillButton_skill1"]')
            .click();
        cy.contains('You can change the Point Increment');
        cy.get('[data-cy="pointIncrement"] input')
            .should('have.value', '100');
        cy.get('[data-cy="pointIncrement"]')
            .type('1');
        cy.clickSaveDialogBtn()

        cy.get('[data-cy="skillsTable-additionalColumns"] [data-pc-section="dropdownicon"]').click()
        cy.get('[data-pc-section="overlay"] [aria-label="Points"]').click()
        cy.get('[data-cy="skillsTable-additionalColumns"] [data-pc-section="dropdownicon"]').click()
        cy.validateTable(tableSelector, [
            [{
                colIndex: 2,
                value: 'Very Great Skill 2'
            }, {
                colIndex: 5,
                value: '200'
            }],
            [{
                colIndex: 2,
                value: 'Very Great Skill 1'
            }, {
                colIndex: 5,
                value: '2,002'
            }],
        ], 5, false, null, false);
        cy.get(`${tableSelector} tbody tr`)
            .should('have.length', 2);
        cy.get('[data-cy="editSkillButton_skill1"]')
            .click();
        cy.contains('You can change the Point Increment');
        cy.get('[data-cy="pointIncrement"] input')
            .should('have.value', '1,001');

        // drill-down and validate the points
        cy.get('[data-cy="closeDialogBtn"]')
            .click();
        cy.get('[data-cy="manageSkillLink_skill1"]')
            .click();
        cy.get('[data-cy="skillOverviewTotalpoints"]')
            .contains('2,002');
    });

    it('changing pointIncrement updates point metrics if skill is finalized', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.exportSkillToCatalog(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 2);

        cy.createProject(2);
        cy.createSubject(2, 1);

        cy.importSkillFromCatalog(2, 1, 1, 1);
        cy.importSkillFromCatalog(2, 1, 1, 2);

        cy.finalizeCatalogImport(2);

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]')
            .should('have.text', '400');
        cy.get('[data-cy="editSkillButton_skill1"]')
            .click();
        cy.get('[data-cy="pointIncrement"] input')
            .should('have.value', '100');
        cy.get('[data-cy="pointIncrement"]')
            .clear()
            .type('33');
        cy.clickSaveDialogBtn()
        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]')
            .should('have.text', '266');
    });

    it('changing pointIncrement does not update point metrics if skill is was not finalized', () => {
        cy.intercept('PATCH', '/admin/projects/proj2/import/skills/skill2')
            .as('updateImportedSkill');
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.exportSkillToCatalog(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 2);

        cy.createProject(2);
        cy.createSubject(2, 1);

        cy.importSkillFromCatalog(2, 1, 1, 1);
        cy.finalizeCatalogImport(2);

        cy.importSkillFromCatalog(2, 1, 1, 2);

        cy.visit('/administrator/projects/proj2/subjects/subj1');
        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]')
            .should('have.text', '200');
        cy.get('[data-cy="editSkillButton_skill2"]')
            .click();
        cy.get('[data-cy="pointIncrement"] input')
            .should('have.value', '100');
        cy.get('[data-cy="pointIncrement"]')
            .clear()
            .type('33');
        cy.clickSaveDialogBtn()
        cy.wait('@updateImportedSkill');
        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]')
            .should('have.text', '200');

    });

    it('cannot add skill events for imported skills', function () {
        cy.intercept('/admin/projects/proj2/subjects/subj1/skills/skill2').as('getSkill2')

        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.exportSkillToCatalog(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 2);

        cy.createProject(2);
        cy.createSubject(2, 1);

        cy.importSkillFromCatalog(2, 1, 1, 1);
        cy.wait(1000);
        cy.importSkillFromCatalog(2, 1, 1, 2);

        cy.finalizeCatalogImport(2);

        // don't even show the add event link for imported skills
        cy.visit('/administrator/projects/proj2/subjects/subj1/skills/skill2');
        cy.get('[data-cy="nav-Add Event"]').should('not.exist');

        // navigate directly to the add skill event page
        cy.visit('/administrator/projects/proj2/subjects/subj1/skills/skill2/addSkillEvent');
        cy.wait('@getSkill2')
        cy.get('[data-cy="subPageHeader"]').contains('Add Skill Events')
        cy.get('[data-cy="skillId"]').contains('skill2')

        cy.get('[data-cy="addSkillEventButton"]').should('be.disabled');
        cy.get('[data-cy="addEventDisabledBlockUI"] > [data-pc-section="mask"]').should('exist');
        cy.get('[data-cy="addEventDisabledMsg"]').contains('Unable to add skill for user. Cannot add events to skills imported from the catalog.');
    })

    it('can edit point increment of an imported skill associated with a quiz', () => {

        cy.createQuizDef(1, { name: 'Test Quiz' });
        cy.createQuizQuestionDef(1, 1);

        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.exportSkillToCatalog(1, 1, 1);

        cy.createProject(2);
        cy.createSubject(2, 1);

        cy.importSkillFromCatalog(2, 1, 1, 1);
        cy.wait(1000);

        cy.visit('/administrator/projects/proj2/subjects/subj1');
        cy.get('[data-cy="editSkillButton_skill1"]')
            .click();
        cy.contains('You can change the Point Increment');
        cy.get('[data-cy="pointIncrement"] input')
            .should('have.value', '150');
        cy.get('[data-cy="pointIncrement"]')
            .type('1');
        cy.clickSaveDialogBtn()

        cy.get('[data-cy="editSkillButton_skill1"]')
            .click();
        cy.contains('You can change the Point Increment');
        cy.get('[data-cy="pointIncrement"] input')
            .should('have.value', '1,501');

        // refresh re-validate
        cy.visit('/administrator/projects/proj2/subjects/subj1');

        cy.get('[data-cy="editSkillButton_skill1"]')
            .click();
        cy.contains('You can change the Point Increment');
        cy.get('[data-cy="pointIncrement"] input')
            .should('have.value', '1,501');
    });
});



