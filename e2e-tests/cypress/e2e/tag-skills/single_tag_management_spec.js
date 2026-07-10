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
describe('Single Skill Tag Management Tests', () => {

    const tagsTableSelector = '[data-cy="skillsTagsTable"]'

    beforeEach(() => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
        cy.createSubject(1, 2);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);
    });

    it('edit existing tag', () => {
        cy.addTagToSkills(1, ['skill1'], 1)
        cy.addTagToSkills(1, ['skill1'], 2)
        cy.addTagToSkills(1, ['skill1'], 3)

        cy.visit('/administrator/projects/proj1/skills-tags/tag1');
        cy.get('[data-cy="title"]').contains('TAG: TAG 1')
        cy.get('[data-cy="editTag"]').click()
        cy.get('[data-cy="newTag"]').should('have.value', 'TAG 1')
        cy.get('[data-cy="newTag"]').click()
        cy.get('[data-cy="newTag"]').type('a')
        cy.get('[data-cy="saveDialogBtn"]').click();

        cy.get('[data-cy="title"]').contains('TAG: TAG 1a')

        cy.get('[data-cy="editTag"]').click()
        cy.get('[data-cy="newTag"]').should('have.value', 'TAG 1a')
        cy.get('[data-cy="newTag"]').click()
        cy.get('[data-cy="newTag"]').type('b')
        cy.get('[data-cy="saveDialogBtn"]').click();

        cy.get('[data-cy="title"]').contains('TAG: TAG 1ab')

        cy.visit('/administrator/projects/proj1/skills-tags/tag1');
        cy.get('[data-cy="title"]').contains('TAG: TAG 1ab')
    });

    it('tag edit in the dialog must validate against saving the same tag name', () => {
        cy.addTagToSkills(1, ['skill1'], 1)
        cy.addTagToSkills(1, ['skill1'], 2)

        cy.visit('/administrator/projects/proj1/skills-tags/tag1');
        cy.get('[data-cy="editTag"]').click()
        cy.get('[data-cy="newTag"]').should('have.value', 'TAG 1')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
        cy.get('[data-cy="newTagError"]').should('not.be.visible')

        cy.get('[data-cy="newTag"]').click()
        cy.get('[data-cy="newTag"]').type('a')
        cy.get('[data-cy="newTagError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="newTag"]').click()
        cy.get('[data-cy="newTag"]').type('{backspace}')
        cy.get('[data-cy="newTagError"]').contains('Tag Name needs to be different')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        // id will remove special chars but it for the edit is not validated
        cy.get('[data-cy="newTag"]').click()
        cy.get('[data-cy="newTag"]').type('$')
        cy.get('[data-cy="newTagError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        // still validates against other tags
        cy.get('[data-cy="newTag"]').click()
        cy.get('[data-cy="newTag"]').type('{backspace}{backspace}2')
        cy.get('[data-cy="newTagError"]').contains('Tag Name already exist')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
    });
})
