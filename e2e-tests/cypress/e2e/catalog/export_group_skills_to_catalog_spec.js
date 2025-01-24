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

describe('Export Group Skills to the Catalog Tests', () => {

    beforeEach(() => {
        cy.createProject(1);
        cy.createSubject(1, 1);
    });

    it('export 1 skill', () => {
        cy.createSkillsGroup(1, 1, 20);
        cy.addSkillToGroup(1, 1, 20, 21, {
            pointIncrement: 100,
            numPerformToCompletion: 5
        });
        cy.addSkillToGroup(1, 1, 20, 22, {
            pointIncrement: 100,
            numPerformToCompletion: 5
        });

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-cy="skillsTable"] [data-p-index="0"] [data-pc-section="rowtogglebutton"]`).click()
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group20"] [data-cy="skillActionsBtn"]')
            .should('be.disabled');
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group20"] [data-cy="skillActionsBtn"] [data-cy="skillActionsNumSelected"]')
            .should('have.text', '0');

        cy.get('[data-cy="ChildRowSkillGroupDisplay_group20"] [data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="rowcheckbox"]').click()

        cy.get('[data-cy="ChildRowSkillGroupDisplay_group20"] [data-cy="skillActionsBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group20"] [data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Export To Catalog"]').click()

        cy.contains('This will export [Very Great Skill 22] Skill to the SkillTree Catalog');
        cy.get('[data-cy="exportToCatalogButton"]')
            .click();
        cy.contains('Skill [Very Great Skill 22] was successfully exported to the catalog!');
        cy.get('[data-cy="okButton"]')
            .click();
        cy.get('[data-cy="exportedBadge-skill22"]')
            .should('exist');
        cy.get('[data-cy="exportedBadge-skill21"]')
            .should('not.exist');
        cy.get('[data-cy="addSkillToGroupBtn-group20"]').should('have.focus')

        // refresh and re-validate
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-cy="skillsTable"] [data-p-index="0"] [data-pc-section="rowtogglebutton"]`).click()
        cy.get('[data-cy="exportedBadge-skill22"]')
            .should('exist');
        cy.get('[data-cy="exportedBadge-skill21"]')
            .should('not.exist');

        // navigate to the catalog page and validate
        cy.get('[data-cy="breadcrumb-proj1"]')
            .click();
        cy.clickNav('Skill Catalog');
        cy.validateTable('[data-cy="exportedSkillsTable"]', [
            [{
                colIndex: 1,
                value: 'Very Great Skill 22'
            }],
        ], 5);
        cy.get('[data-cy="nameCell_skill22"]')
            .contains('Awesome Group 20 Subj1');
    });

});

