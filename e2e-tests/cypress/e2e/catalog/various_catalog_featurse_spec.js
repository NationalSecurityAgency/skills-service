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

    it.skip('show IMPORTED badge on performed skills table', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);
        cy.exportSkillToCatalog(1, 1, 1, { pointIncrement: 100 });
        cy.exportSkillToCatalog(1, 1, 2);
        cy.exportSkillToCatalog(1, 1, 3);

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 4, { pointIncrement: 100 });

        cy.importSkillFromCatalog(2, 1, 1, 1);
        cy.finalizeCatalogImport(2);

        const user = 'user0';
        cy.reportSkill(1, 1, user, 'now');
        cy.waitForBackendAsyncTasksToComplete();
        cy.reportSkill(2, 4, user, 'now');

        cy.visit(`/administrator/projects/proj2/users/${user}/skillEvents`);
        cy.get('[data-cy="performedSkillsTable"] tr:nth-child(1) [data-cy="importedTag"]')
            .should('not.exist');
        cy.get('[data-cy="performedSkillsTable"] tr:nth-child(2) [data-cy="importedTag"]')
            .should('exist');
    });

});