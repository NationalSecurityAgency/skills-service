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

describe('Gatalog Tests', () => {

    beforeEach(() => {
        cy.createProject(1);
        cy.createSubject(1, 1);
    });

    it('catalog', () => {
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1);
        cy.createSkill(2, 1, 2);

        cy.visit('/administrator/projects/proj1/subjects/subj1');
    });

    it('import skills from catalog - paging', () => {
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1);
        cy.createSkill(2, 1, 2);
        cy.createSkill(2, 1, 3);
        cy.createSkill(2, 1, 4);
        cy.createSkill(2, 1, 5);

        cy.createSubject(2, 2);
        cy.createSkill(2, 2, 6);
        cy.createSkill(2, 2, 7);
        cy.createSkill(2, 2, 8);
        cy.createSkill(2, 2, 9);
        cy.createSkill(2, 2, 10);
        cy.createSkill(2, 2, 11);

        cy.exportSkillToCatalog(2, 1, 1);
        cy.exportSkillToCatalog(2, 1, 2);
        cy.exportSkillToCatalog(2, 1, 3);
        cy.exportSkillToCatalog(2, 1, 4);
        cy.exportSkillToCatalog(2, 1, 5);

        cy.exportSkillToCatalog(2, 2, 6);
        cy.exportSkillToCatalog(2, 2, 7);
        cy.exportSkillToCatalog(2, 2, 8);
        cy.exportSkillToCatalog(2, 2, 9);
        cy.exportSkillToCatalog(2, 2, 10);
        cy.exportSkillToCatalog(2, 2, 11);

        cy.visit('/administrator/projects/proj1/subjects/subj1');
    });


});

