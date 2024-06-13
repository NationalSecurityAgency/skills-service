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
import dayjs from 'dayjs';
import utcPlugin from 'dayjs/plugin/utc';

dayjs.extend(utcPlugin);

describe('Projects Cards Tests', () => {
    beforeEach(() => {
    });

    it('more than 5 projects will convert to cards', () => {
        cy.createProject(1);
        cy.createProject(2);
        cy.createProject(3);
        cy.createProject(4);
        cy.createProject(5);

        cy.visit('/administrator/');

        cy.get('[data-cy="projCard_proj1_manageLink"]')
        cy.get('[data-cy="projCard_proj2_manageLink"]')
        cy.get('[data-cy="projCard_proj3_manageLink"]')
        cy.get('[data-cy="projCard_proj4_manageLink"]')
        cy.get('[data-cy="projCard_proj5_manageLink"]')

        cy.get('#projectCards').should('not.have.class', 'flex')

        cy.get('[data-cy="newProjectButton"]').click();
        cy.get('[data-cy="projectName"]').type('Another');
        cy.get('[data-cy="saveDialogBtn"]').click()

        cy.get('[data-cy="projCard_proj1_manageLink"]')
        cy.get('[data-cy="projCard_proj2_manageLink"]')
        cy.get('[data-cy="projCard_proj3_manageLink"]')
        cy.get('[data-cy="projCard_proj4_manageLink"]')
        cy.get('[data-cy="projCard_proj5_manageLink"]')
        cy.get('[data-cy="projCard_Another_manageLink"]')

        cy.get('#projectCards').should('have.class', 'flex')

        // refresh and re-validate
        cy.visit('/administrator/');

        cy.get('[data-cy="projCard_proj1_manageLink"]')
        cy.get('[data-cy="projCard_proj2_manageLink"]')
        cy.get('[data-cy="projCard_proj3_manageLink"]')
        cy.get('[data-cy="projCard_proj4_manageLink"]')
        cy.get('[data-cy="projCard_proj5_manageLink"]')
        cy.get('[data-cy="projCard_Another_manageLink"]')
        cy.get('#projectCards').should('have.class', 'flex')
    });



});

