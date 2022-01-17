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

describe('Single Skill and Catalog Tests', () => {

    beforeEach(() => {
        cy.createProject(1);
        cy.createSubject(1, 1);
    });

    const navDepsSelector = '[data-cy="nav-Dependencies"]';
    const navAddEventSelector = '[data-cy="nav-Add Event"]';

    it('drill down to a skill after creation', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);
        cy.exportSkillToCatalog(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 2);
        cy.exportSkillToCatalog(1, 1, 3);

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 4);

        cy.importSkillFromCatalog(2, 1, 1, 1);

        cy.visit('/administrator/projects/proj2/subjects/subj1');
        cy.get('[data-cy="importFromCatalogBtn"]').click();
        cy.get('[data-cy="skillSelect_proj1-skill2"]').check({force: true});
        cy.get('[data-cy="importBtn"]').click();

        cy.get('[data-cy="manageSkillBtn_skill2"]').click();
        cy.get('[data-cy="pageHeader"]').contains('ID: skill2')
        cy.get('[data-cy="pageHeader"] [data-cy="importedBadge"]')
        cy.get('[data-cy="childRowDisplay_skill2"]').contains('This skill was imported')
        cy.get('[data-cy="childRowDisplay_skill2"]').contains('initially defined in the This is project 1')
        cy.get(navDepsSelector).should('not.exist')
        cy.get(navAddEventSelector).should('not.exist')

        // now check non-import skills to validate that section selectors are correct
        cy.get('[data-cy="breadcrumb-subj1"]').click();
        cy.get('[data-cy="manageSkillBtn_skill4"]').click();
        cy.get('[data-cy="pageHeader"]').contains('ID: skill4')
        cy.get('[data-cy="pageHeader"] [data-cy="importedBadge"]').should('not.exist')
        cy.get('[data-cy="childRowDisplay_skill4"]').contains('This skill was imported').should('not.exist')
        cy.get(navDepsSelector).should('exist')
        cy.get(navAddEventSelector).should('exist')
    });

    it('drill down if skill already exist', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);
        cy.exportSkillToCatalog(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 2);
        cy.exportSkillToCatalog(1, 1, 3);

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 4);

        cy.importSkillFromCatalog(2, 1, 1, 1);
        cy.importSkillFromCatalog(2, 1, 1, 2);
        cy.importSkillFromCatalog(2, 1, 1, 3);

        cy.visit('/administrator/projects/proj2/subjects/subj1');

        cy.get('[data-cy="manageSkillBtn_skill2"]').click();
        cy.get('[data-cy="pageHeader"]').contains('ID: skill2')
        cy.get('[data-cy="pageHeader"] [data-cy="importedBadge"]')
        cy.get('[data-cy="childRowDisplay_skill2"]').contains('This skill was imported')
        cy.get('[data-cy="childRowDisplay_skill2"]').contains('initially defined in the This is project 1')
        cy.get(navDepsSelector).should('not.exist')
        cy.get(navAddEventSelector).should('not.exist')

        // now check non-import skills to validate that section selectors are correct
        cy.get('[data-cy="breadcrumb-subj1"]').click();
        cy.get('[data-cy="manageSkillBtn_skill4"]').click();
        cy.get('[data-cy="pageHeader"]').contains('ID: skill4')
        cy.get('[data-cy="pageHeader"] [data-cy="importedBadge"]').should('not.exist')
        cy.get('[data-cy="childRowDisplay_skill4"]').contains('This skill was imported').should('not.exist')
        cy.get(navDepsSelector).should('exist')
        cy.get(navAddEventSelector).should('exist')
    });

    it('navigate directly to skill', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);
        cy.exportSkillToCatalog(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 2);
        cy.exportSkillToCatalog(1, 1, 3);

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 4);

        cy.importSkillFromCatalog(2, 1, 1, 1);
        cy.importSkillFromCatalog(2, 1, 1, 2);
        cy.importSkillFromCatalog(2, 1, 1, 3);

        cy.visit('/administrator/projects/proj2/subjects/subj1/skills/skill2');

        cy.get('[data-cy="pageHeader"]').contains('ID: skill2')
        cy.get('[data-cy="pageHeader"] [data-cy="importedBadge"]')
        cy.get('[data-cy="childRowDisplay_skill2"]').contains('This skill was imported')
        cy.get('[data-cy="childRowDisplay_skill2"]').contains('initially defined in the This is project 1')
        cy.get(navDepsSelector).should('not.exist')
        cy.get(navAddEventSelector).should('not.exist')
    });

});