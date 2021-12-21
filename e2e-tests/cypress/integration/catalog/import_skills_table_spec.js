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

describe('Import From Catalog Table Tests', () => {

    const tableSelector = '[data-cy="importSkillsFromCatalogTable"]';

    before(() => {
        Cypress.env('disableResetDb', true);
        cy.resetDb();
        cy.fixture('vars.json').then((vars) => {
            cy.logout()

            if (!Cypress.env('oauthMode')) {
                cy.log('NOT in oauthMode, using form login')
                cy.login(vars.defaultUser, vars.defaultPass);
            } else {
                cy.log('oauthMode, using loginBySingleSignOn')
                cy.loginBySingleSignOn()
            }
        });

        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSubject(1, 2);

        // mix skill names since it's sorted by skillId - this will force different projects in the first page
        cy.createSkill(1, 1, 1, { pointIncrement: 10, numPerformToCompletion: 8 });
        cy.createSkill(1, 2, 6, { pointIncrement: 15, numPerformToCompletion: 7 });
        cy.createSkill(1, 1, 7, { pointIncrement: 20, numPerformToCompletion: 6 });
        cy.createSkill(1, 2, 4, { pointIncrement: 25, numPerformToCompletion: 5 });
        cy.createSkill(1, 1, 5, { pointIncrement: 30, numPerformToCompletion: 4 });

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSubject(2, 3);
        cy.createSkill(2, 1, 2, { pointIncrement: 35, numPerformToCompletion: 3 });
        cy.createSkill(2, 3, 3, { pointIncrement: 40, numPerformToCompletion: 2 });
        cy.createSkill(2, 3, 8, { pointIncrement: 45, numPerformToCompletion: 1 });

        cy.exportSkillToCatalog(1, 1, 1);
        cy.exportSkillToCatalog(1, 2, 6);
        cy.exportSkillToCatalog(1, 1, 7);
        cy.exportSkillToCatalog(1, 2, 4);
        cy.exportSkillToCatalog(1, 1, 5);

        cy.exportSkillToCatalog(2, 1, 2); // proj 2
        cy.exportSkillToCatalog(2, 3, 3); // proj 2
        cy.exportSkillToCatalog(2, 3, 8); // proj 2

        cy.createProject(3);
        cy.createSubject(3, 1);

        Cypress.Commands.add("validateNoSkillsSelected", () => {
            cy.get('[data-cy="skillSelect_proj1-skill1"]').should('not.be.checked')
            cy.get('[data-cy="skillSelect_proj2-skill2"]').should('not.be.checked')
            cy.get('[data-cy="skillSelect_proj2-skill3Subj3"]').should('not.be.checked')
            cy.get('[data-cy="skillSelect_proj1-skill4Subj2"]').should('not.be.checked')
            cy.get('[data-cy="skillSelect_proj1-skill5"]').should('not.be.checked')
            cy.get('[data-cy="numSelectedSkills"]').should('have.text', '0');
            cy.get('[data-cy="importBtn"]').should('be.disabled');
        });

    });
    after(() => {
        Cypress.env('disableResetDb', false);
    });

    it ('select a couple skills then clear', () => {
        cy.visit('/administrator/projects/proj3/subjects/subj1');
        cy.get('[data-cy="importFromCatalogBtn"]').click();

        cy.get('[data-cy="importBtn"]').should('be.disabled');
        cy.get('[data-cy="numSelectedSkills"]').should('have.text', '0');

        cy.get('[data-cy="skillSelect_proj2-skill2"]').check({force: true})
        cy.get('[data-cy="skillSelect_proj1-skill4Subj2"]').check({force: true})

        cy.get('[data-cy="skillSelect_proj1-skill1"]').should('not.be.checked')
        cy.get('[data-cy="skillSelect_proj2-skill2"]').should('be.checked')
        cy.get('[data-cy="skillSelect_proj2-skill3Subj3"]').should('not.be.checked')
        cy.get('[data-cy="skillSelect_proj1-skill4Subj2"]').should('be.checked')
        cy.get('[data-cy="skillSelect_proj1-skill5"]').should('not.be.checked')
        cy.get('[data-cy="numSelectedSkills"]').should('have.text', '2');
        cy.get('[data-cy="importBtn"]').should('be.enabled');

        cy.get('[data-cy="clearSelectedBtn"]').click()
        cy.validateNoSkillsSelected();
    })

    it ('select pages of skills then clear', () => {
        cy.visit('/administrator/projects/proj3/subjects/subj1');
        cy.get('[data-cy="importFromCatalogBtn"]').click();

        cy.get('[data-cy="selectPageOfSkillsBtn"]').click()
        cy.get('[data-cy="skillSelect_proj1-skill1"]').should('be.checked')
        cy.get('[data-cy="skillSelect_proj2-skill2"]').should('be.checked')
        cy.get('[data-cy="skillSelect_proj2-skill3Subj3"]').should('be.checked')
        cy.get('[data-cy="skillSelect_proj1-skill4Subj2"]').should('be.checked')
        cy.get('[data-cy="skillSelect_proj1-skill5"]').should('be.checked')
        cy.get('[data-cy="numSelectedSkills"]').should('have.text', '5');
        cy.get('[data-cy="importBtn"]').should('be.enabled');

        cy.get('[data-cy="clearSelectedBtn"]').click()
        cy.validateNoSkillsSelected();
    });

    it('change page size then select the page', () => {
        cy.visit('/administrator/projects/proj3/subjects/subj1');
        cy.get('[data-cy="importFromCatalogBtn"]').click();

        cy.get('[data-cy="skillsBTablePageSize"]').select('10');

        cy.get('[data-cy="skillSelect_proj1-skill1"]').should('not.be.checked')
        cy.get('[data-cy="skillSelect_proj2-skill2"]').should('not.be.checked')
        cy.get('[data-cy="skillSelect_proj2-skill3Subj3"]').should('not.be.checked')
        cy.get('[data-cy="skillSelect_proj1-skill4Subj2"]').should('not.be.checked')
        cy.get('[data-cy="skillSelect_proj1-skill5"]').should('not.be.checked')
        cy.get('[data-cy="skillSelect_proj1-skill6Subj2"]').should('not.be.checked')
        cy.get('[data-cy="skillSelect_proj1-skill7"]').should('not.be.checked')
        cy.get('[data-cy="skillSelect_proj2-skill8Subj3"]').should('not.be.checked')
        cy.get('[data-cy="numSelectedSkills"]').should('have.text', '0');
        cy.get('[data-cy="importBtn"]').should('be.disabled');

        cy.get('[data-cy="selectPageOfSkillsBtn"]').click()

        cy.get('[data-cy="skillSelect_proj1-skill1"]').should('be.checked')
        cy.get('[data-cy="skillSelect_proj2-skill2"]').should('be.checked')
        cy.get('[data-cy="skillSelect_proj2-skill3Subj3"]').should('be.checked')
        cy.get('[data-cy="skillSelect_proj1-skill4Subj2"]').should('be.checked')
        cy.get('[data-cy="skillSelect_proj1-skill5"]').should('be.checked')
        cy.get('[data-cy="skillSelect_proj1-skill6Subj2"]').should('be.checked')
        cy.get('[data-cy="skillSelect_proj1-skill7"]').should('be.checked')
        cy.get('[data-cy="skillSelect_proj2-skill8Subj3"]').should('be.checked')
        cy.get('[data-cy="numSelectedSkills"]').should('have.text', '8');
        cy.get('[data-cy="importBtn"]').should('be.enabled');

        cy.get('[data-cy="clearSelectedBtn"]').click()
        cy.validateNoSkillsSelected();
    });

    it('filter by skill name', () => {
        cy.visit('/administrator/projects/proj3/subjects/subj1');
        cy.get('[data-cy="importFromCatalogBtn"]').click();

        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`).should('have.text', '8')

        cy.get('[data-cy="skillNameFilter"]').type('  sUbJ2  ');
        cy.get('[data-cy="filterBtn"]').click();
        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`).should('have.text', '2')
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'Very Great Skill 4 Subj2' }],
            [{ colIndex: 0,  value: 'Very Great Skill 6 Subj2' }],
        ], 5);

        cy.get('[data-cy="filterResetBtn"]').click()
        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`).should('have.text', '8')

        // now test it via enter
        cy.get('[data-cy="skillNameFilter"]').type('  sKILL 4  {enter}');
        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`).should('have.text', '1')
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'Very Great Skill 4 Subj2' }],
        ], 5);
    });
})