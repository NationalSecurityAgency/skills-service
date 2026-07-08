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
describe('Manage Tag Skills Tests', () => {

    const tagsTableSelector = '[data-cy="skillsTagsTable"]'


    beforeEach(() => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
        cy.createSubject(1, 2);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);
    });

    it('create new tags', () => {
        cy.visit('/administrator/projects/proj1/skills-tags/');
        cy.get('[data-cy="noContent"]').contains('No Tags Yet')

        cy.get('[data-cy="btn_Skill Tags"]').click();
        cy.get('[data-pc-name="pcmaximizebutton"]').should("have.focus")
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
        cy.get('[data-cy="newTag"]').should('be.visible')
        cy.get('[data-cy="newTag"]').type('New Test Tag');
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')
        cy.get('[data-cy="saveDialogBtn"]').click();

        cy.validateTable(tagsTableSelector, [
            [{colIndex: 0, value: 'New Test Tag'}, {colIndex: 1, value: '0'}],
        ], 25);
        cy.get('[data-cy="noContent"]').should('not.exist');

        cy.get('[data-cy="btn_Skill Tags"]').click();
        cy.get('[data-pc-name="pcmaximizebutton"]').should("have.focus")
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
        cy.get('[data-cy="newTag"]').should('be.visible')
        cy.get('[data-cy="newTag"]').type('Second Tag');
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')
        cy.get('[data-cy="saveDialogBtn"]').click();

        cy.validateTable(tagsTableSelector, [
            [{colIndex: 0, value: 'Second Tag'}, {colIndex: 1, value: '0'}],
            [{colIndex: 0, value: 'New Test Tag'}, {colIndex: 1, value: '0'}],
        ], 25);

        cy.visit('/administrator/projects/proj1/skills-tags/');
        cy.validateTable(tagsTableSelector, [
            [{colIndex: 0, value: 'Second Tag'}, {colIndex: 1, value: '0'}],
            [{colIndex: 0, value: 'New Test Tag'}, {colIndex: 1, value: '0'}],
        ], 25);
    });


});
