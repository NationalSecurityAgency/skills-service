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

describe('Import Skills under a Group Tests', () => {

    beforeEach(() => {
        cy.waitForBackendAsyncTasksToComplete();

        cy.createProject(1);
        cy.createSubject(1, 1);
    });

    it('import a skill under a group and finalize', () => {
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1);
        cy.createSkill(2, 1, 2);

        cy.exportSkillToCatalog(2, 1, 1);
        cy.exportSkillToCatalog(2, 1, 2);

        cy.createSkillsGroup(1, 1, 5);

        cy.visit('/administrator/projects/proj1/subjects/subj1/');
        cy.get('[data-cy="expandDetailsBtn_group5"]')
            .click();
        cy.get('[data-cy="importSkillToGroupBtn-group5"]')
            .click();
        cy.get('[data-cy="skillSelect_proj2-skill1"]')
            .check({ force: true });
        cy.get('[data-cy="importBtn"]')
            .click();
        cy.get('[data-cy="importSkillToGroupBtn-group5"]')
            .should('have.focus');

        cy.get('[data-cy="nameCell_skill1"]')
            .contains('Very Great Skill 1');
        cy.get('[data-cy="nameCell_skill1"] [data-cy="importedBadge-skill1"]');
        cy.get('[data-cy="nameCell_skill1"] [data-cy="disabledBadge-skill1"]');

        cy.get('[data-cy="importFinalizeAlert"]')
            .contains('There is 1 imported skill in this project that is not yet finalized');
        cy.get('[data-cy="pageHeaderStat_Skills_disabledCount"]')
            .should('have.text', '1');
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]')
            .should('have.text', '0');

        cy.get('[data-cy="finalizeBtn"]')
            .click();
        cy.get('[data-cy="doPerformFinalizeButton"]')
            .click();
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]')
            .should('not.exist');
        cy.get('[data-cy="importFinalizeAlert"]')
            .contains('Finalizing 1 imported skill');

        cy.waitForBackendAsyncTasksToComplete();
        cy.get('[data-cy="importFinalizeAlert"]')
            .contains('Successfully finalized 1 imported skill!');
        cy.get('[data-cy="pageHeaderStat_Skills_disabledCount"]')
            .should('not.exist');
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]')
            .should('have.text', '1');

        cy.get('[data-cy="expandDetailsBtn_group5"]')
            .click();
        cy.get('[data-cy="nameCell_skill1"] [data-cy="importedBadge-skill1"]');
        cy.get('[data-cy="nameCell_skill1"] [data-cy="disabledBadge-skill1"]')
            .should('not.exist');
    });

    it('refocus on the import button after the Import modal is closed', () => {
        cy.createSkillsGroup(1, 1, 5);
        cy.visit('/administrator/projects/proj1/subjects/subj1/');
        cy.get('[data-cy="expandDetailsBtn_group5"]')
            .click();
        cy.get('[data-cy="importSkillToGroupBtn-group5"]')
            .click();
        cy.get('[data-cy="okButton"]')
            .click();
        cy.get('[data-cy="importSkillToGroupBtn-group5"]')
            .should('have.focus');
    });

    it('edit imported skills points', () => {
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1);
        cy.createSkill(2, 1, 2);

        cy.exportSkillToCatalog(2, 1, 1);
        cy.exportSkillToCatalog(2, 1, 2);

        cy.createSkillsGroup(1, 1, 5);
        cy.bulkImportSkillsIntoGroupFromCatalog(1, 1, 5, [
            {
                projNum: 2,
                skillNum: 1
            },
            {
                projNum: 2,
                skillNum: 2
            },
        ]);

        cy.visit('/administrator/projects/proj1/subjects/subj1/');
        cy.get('[data-cy="expandDetailsBtn_group5"]')
            .click();
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_group5"] [data-cy="skillsTable-additionalColumns"]`)
            .contains('Points')
            .click();
        cy.validateTable('[data-cy="ChildRowSkillGroupDisplay_group5"] [data-cy="skillsTable"]', [
            [{
                colIndex: 0,
                value: 'skill2'
            }, {
                colIndex: 3,
                value: '200'
            }],
            [{
                colIndex: 0,
                value: 'skill1'
            }, {
                colIndex: 3,
                value: '200'
            }],
        ], 5, true, null, false);

        cy.get('[data-cy="editSkillButton_skill2"]')
            .click();
        cy.contains('This skill was imported from This is project 2 and can only be modified in that project.');
        cy.get('[data-cy="skillPointIncrement"]')
            .clear()
            .type('33');
        cy.get('[data-cy="saveSkillButton"]')
            .click();
        cy.validateTable('[data-cy="ChildRowSkillGroupDisplay_group5"] [data-cy="skillsTable"]', [
            [{
                colIndex: 0,
                value: 'skill2'
            }, {
                colIndex: 3,
                value: '66'
            }],
            [{
                colIndex: 0,
                value: 'skill1'
            }, {
                colIndex: 3,
                value: '200'
            }],
        ], 5, true, null, false);
    });

});


