/*
 * Copyright 2026 SkillTree
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
describe('Exclude Imported Users Tests', () => {


    const tableSelector = '[data-cy=usersTable]'

    before(() => {
        cy.beforeTestSuiteThatReusesData()

        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.exportSkillToCatalog(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 2);

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.importSkillFromCatalog(2, 1, 1, 1);
        cy.importSkillFromCatalog(2, 1, 1, 2);
        cy.finalizeCatalogImport(2);
        cy.createSkill(2, 1, 3);
        cy.createSkill(2, 1, 4);

        cy.createProject(3);
        cy.createSubject(3, 1);
        cy.importSkillFromCatalog(3, 1, 1, 1);
        cy.importSkillFromCatalog(3, 1, 1, 2);
        cy.finalizeCatalogImport(3);
        cy.createSkill(3, 1, 3);
        cy.createSkill(3, 1, 4);

        cy.reportSkill(1, 1, 'user1', 'now');
        cy.reportSkill(1, 2, 'user2', 'now');
        cy.reportSkill(1, 2, 'user3', 'now');
        cy.reportSkill(1, 1, 'user4', 'now');
        cy.reportSkill(1, 2, 'user4', 'now');

        cy.reportSkill(2, 3, 'user1', 'now');
        cy.reportSkill(2, 4, 'user1', 'now');
        cy.reportSkill(2, 4, 'user2', 'now');

        cy.createBadge(1, 1)
        cy.assignSkillToBadge(1, 1, 1, 1)
        cy.assignSkillToBadge(1, 1, 2, 1)
        cy.enableBadge(1, 1)

        cy.createBadge(2, 1)
        cy.assignSkillToBadge(2, 1, 1, 1)
        cy.assignSkillToBadge(2, 1, 2, 1)
        cy.assignSkillToBadge(2, 1, 3, 1)
        cy.assignSkillToBadge(2, 1, 4, 1)
        cy.enableBadge(2, 1)
    });

    after(() => {
        cy.afterTestSuiteThatReusesData()
    });

    it('Exclude project users that only earned points in imported skills', () => {
        cy.visit('/administrator/projects/proj2/users');

        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'user2' }],
            [{ colIndex: 1,  value: 'user1' }],
            [{ colIndex: 1,  value: 'user4' }],
            [{ colIndex: 1,  value: 'user3' }],
        ], 5);

        cy.get('[data-cy="excludeImportedToggle"]').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'user2' }],
            [{ colIndex: 1,  value: 'user1' }],
        ], 5);

        cy.visit('/administrator/projects/proj2/users');
        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'user2' }],
            [{ colIndex: 1,  value: 'user1' }],
        ], 5);

        cy.get('[data-cy="breadcrumb-Projects"]').click()
        cy.get('[data-cy="projCard_proj1_manageLink"]').click()
        cy.get('[data-cy="nav-Users"]').click()
        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'user4' }],
            [{ colIndex: 1,  value: 'user3' }],
            [{ colIndex: 1,  value: 'user2' }],
            [{ colIndex: 1,  value: 'user1' }],
        ], 5);
        cy.get('[data-cy="excludeImportedToggle"]').should('not.exist')
    });

    it('Exclude subject users that only earned points in imported skills', () => {
        cy.visit('/administrator/projects/proj2/subjects/subj1/users');

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'user2' }],
            [{ colIndex: 0,  value: 'user1' }],
            [{ colIndex: 0,  value: 'user4' }],
            [{ colIndex: 0,  value: 'user3' }],
        ], 5);

        cy.get('[data-cy="excludeImportedToggle"]').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'user2' }],
            [{ colIndex: 0,  value: 'user1' }],
        ], 5);

        cy.visit('/administrator/projects/proj2/subjects/subj1/users');
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'user2' }],
            [{ colIndex: 0,  value: 'user1' }],
        ], 5);

        cy.visit('/administrator/projects/proj1/subjects/subj1/users');
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'user4' }],
            [{ colIndex: 0,  value: 'user3' }],
            [{ colIndex: 0,  value: 'user2' }],
            [{ colIndex: 0,  value: 'user1' }],
        ], 5);
        cy.get('[data-cy="excludeImportedToggle"]').should('not.exist')
    });

    it('Exclude skill users that only earned points in imported skills', () => {
        cy.visit('/administrator/projects/proj2/subjects/subj1/skills/skill1/users');

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'user4' }],
            [{ colIndex: 0,  value: 'user1' }],
        ], 5);

        cy.get('[data-cy="excludeImportedToggle"]').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'user1' }],
        ], 5);

        cy.visit('/administrator/projects/proj2/subjects/subj1/skills/skill1/users');
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'user1' }],
        ], 5);

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1/users');
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'user4' }],
            [{ colIndex: 0,  value: 'user1' }],
        ], 5);
        cy.get('[data-cy="excludeImportedToggle"]').should('not.exist')
    });

    it('Exclude badge users that only earned points in imported skills', () => {
        cy.visit('/administrator/projects/proj2/badges/badge1/users');

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'user2' }],
            [{ colIndex: 0,  value: 'user1' }],
            [{ colIndex: 0,  value: 'user4' }],
            [{ colIndex: 0,  value: 'user3' }],
        ], 5);

        cy.get('[data-cy="excludeImportedToggle"]').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'user2' }],
            [{ colIndex: 0,  value: 'user1' }],
        ], 5);

        cy.visit('/administrator/projects/proj2/badges/badge1/users');
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'user2' }],
            [{ colIndex: 0,  value: 'user1' }],
        ], 5);

        cy.visit('/administrator/projects/proj1/badges/badge1/users');
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'user4' }],
            [{ colIndex: 0,  value: 'user3' }],
            [{ colIndex: 0,  value: 'user2' }],
            [{ colIndex: 0,  value: 'user1' }],
        ], 5);
        cy.get('[data-cy="excludeImportedToggle"]').should('not.exist')
    });

    it('exclude table is saved in local storage and used through the project', () => {
        cy.visit('/administrator/projects/proj2/users');

        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'user2' }],
            [{ colIndex: 1,  value: 'user1' }],
            [{ colIndex: 1,  value: 'user4' }],
            [{ colIndex: 1,  value: 'user3' }],
        ], 5);

        cy.get('[data-cy="excludeImportedToggle"]').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'user2' }],
            [{ colIndex: 1,  value: 'user1' }],
        ], 5);

        cy.visit('/administrator/projects/proj2/subjects/subj1/users');
        cy.get('[data-cy="excludeImportedToggle"] input').should('be.checked')
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'user2' }],
            [{ colIndex: 0,  value: 'user1' }],
        ], 5);

        cy.visit('/administrator/projects/proj2/subjects/subj1/skills/skill1/users');
        cy.get('[data-cy="excludeImportedToggle"] input').should('be.checked')
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'user1' }],
        ], 5);

        cy.visit('/administrator/projects/proj2/badges/badge1/users');
        cy.get('[data-cy="excludeImportedToggle"] input').should('be.checked')
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'user2' }],
            [{ colIndex: 0,  value: 'user1' }],
        ], 5);

        cy.visit('/administrator/projects/proj3/users');
        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'user4' }],
            [{ colIndex: 1,  value: 'user3' }],
            [{ colIndex: 1,  value: 'user2' }],
            [{ colIndex: 1,  value: 'user1' }],
        ], 5);
        cy.get('[data-cy="excludeImportedToggle"] input').should('not.be.checked')
    });

})


