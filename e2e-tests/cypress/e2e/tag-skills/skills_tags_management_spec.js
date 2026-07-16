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
        cy.get(tagsTableSelector).should('not.exist')

        cy.openDialog('[data-cy="btn_Skill Tags"]', true)
        cy.get('[data-pc-name="pcmaximizebutton"]').should("have.focus")
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
        cy.get('[data-cy="newTag"]').should('be.visible')
        cy.get('[data-cy="newTag"]').type('New Test Tag');
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')
        cy.get('[data-cy="saveDialogBtn"]').click();
        cy.get('[data-cy="btn_Skill Tags"]').should('have.focus')

        cy.validateTable(tagsTableSelector, [
            [{colIndex: 0, value: 'New Test Tag'}, {colIndex: 1, value: '0'}],
        ], 25);
        cy.get('[data-cy="noContent"]').should('not.exist');

        cy.openDialog('[data-cy="btn_Skill Tags"]', true)
        cy.get('[data-pc-name="pcmaximizebutton"]').should("have.focus")
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
        cy.get('[data-cy="newTag"]').should('be.visible')
        cy.get('[data-cy="newTag"]').type('Second Tag');
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')
        cy.get('[data-cy="saveDialogBtn"]').click();
        cy.get('[data-cy="btn_Skill Tags"]').should('have.focus')

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

    it('special chars are removed from tag ids', () => {
        cy.visit('/administrator/projects/proj1/skills-tags/');
        cy.get('[data-cy="noContent"]').contains('No Tags Yet')
        cy.get(tagsTableSelector).should('not.exist')

        cy.get('[data-cy="btn_Skill Tags"]').click();
        cy.get('[data-pc-name="pcmaximizebutton"]').should("have.focus")
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
        cy.get('[data-cy="newTag"]').should('be.visible')
        cy.get('[data-cy="newTag"]').type('t%^&*() ag');
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')
        cy.get('[data-cy="saveDialogBtn"]').click();
        cy.get('[data-cy="btn_Skill Tags"]').should('have.focus')

        cy.validateTable(tagsTableSelector, [
            [{colIndex: 0, value: 't%^&amp;*() ag'}, {colIndex: 1, value: '0'}],
        ], 25);
        cy.get('[data-cy="manageTag_t%^&amp;*() ag"]').click()
        cy.url().should('match', /\/administrator\/projects\/proj1\/skills-tags\/tag$/)
        cy.get('[data-cy="title"]').contains('TAG: t%^&amp;*() ag')

    });

    it('cancelling new tag dialog returns focus to the new tag button', () => {
        cy.visit('/administrator/projects/proj1/skills-tags/');
        cy.get('[data-cy="noContent"]').contains('No Tags Yet')

        cy.get('[data-cy="btn_Skill Tags"]').click();
        cy.get('[data-pc-name="pcmaximizebutton"]').should("have.focus")
        cy.get('[data-cy="closeDialogBtn"]').should('be.enabled')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        cy.get('[data-cy="closeDialogBtn"]').click()
        cy.get('[data-cy="btn_Skill Tags"]').should('have.focus')
    })

    it('edit existing tags', () => {
        cy.addTagToSkills(1, ['skill1'], 1)
        cy.addTagToSkills(1, ['skill1'], 2)
        cy.addTagToSkills(1, ['skill1'], 3)

        cy.visit('/administrator/projects/proj1/skills-tags/');

        cy.validateTable(tagsTableSelector, [
            [{colIndex: 0, value: 'TAG 3'}],
            [{colIndex: 0, value: 'TAG 2'}],
            [{colIndex: 0, value: 'TAG 1'}],
        ], 25);

        cy.openDialog('[data-cy="editTag_tag1"]', true)
        cy.get('[data-cy="newTag"]').should('have.value', 'TAG 1')
        cy.get('[data-p="modal"] [data-cy="newTag"]').type('a')
        cy.get('[data-cy="saveDialogBtn"]').click();
        cy.validateTable(tagsTableSelector, [
            [{colIndex: 0, value: 'TAG 3'}],
            [{colIndex: 0, value: 'TAG 2'}],
            [{colIndex: 0, value: 'TAG 1a'}],
        ], 25);
        cy.get('[data-cy="editTag_tag1"]').should('have.focus')

        cy.openDialog('[data-cy="editTag_tag1"]', true)
        cy.get('[data-cy="newTag"]').should('have.value', 'TAG 1a')
        cy.get('[data-p="modal"] [data-cy="newTag"]').type('{selectAll}other')
        cy.get('[data-cy="saveDialogBtn"]').click();
        cy.validateTable(tagsTableSelector, [
            [{colIndex: 0, value: 'TAG 3'}],
            [{colIndex: 0, value: 'TAG 2'}],
            [{colIndex: 0, value: 'other'}],
        ], 25);
        cy.get('[data-cy="editTag_tag1"]').should('have.focus')

        cy.openDialog('[data-cy="editTag_tag2"]', true)
        cy.get('[data-cy="newTag"]').should('have.value', 'TAG 2')
        cy.get('[data-p="modal"]  [data-cy="newTag"]').type('{selectAll}new')
        cy.get('[data-cy="saveDialogBtn"]').click();
        cy.validateTable(tagsTableSelector, [
            [{colIndex: 0, value: 'TAG 3'}],
            [{colIndex: 0, value: 'new'}],
            [{colIndex: 0, value: 'other'}],
        ], 25);
        cy.get('[data-cy="editTag_tag2"]').should('have.focus')

        cy.visit('/administrator/projects/proj1/skills-tags/');
        cy.validateTable(tagsTableSelector, [
            [{colIndex: 0, value: 'TAG 3'}],
            [{colIndex: 0, value: 'new'}],
            [{colIndex: 0, value: 'other'}],
        ], 25);
    });

    it('delete existing tags', () => {
        cy.addTagToSkills(1, ['skill1'], 1)
        cy.addTagToSkills(1, ['skill1'], 2)

        cy.visit('/administrator/projects/proj1/skills-tags/');

        cy.validateTable(tagsTableSelector, [
            [{colIndex: 0, value: 'TAG 2'}],
            [{colIndex: 0, value: 'TAG 1'}],
        ], 25);

        cy.openDialog('[data-cy="deleteTag_tag1"]')
        cy.acceptRemovalSafetyCheck('This will remove TAG 1 Tag.')

        cy.validateTable(tagsTableSelector, [
            [{colIndex: 0, value: 'TAG 2'}],
        ], 25);
        cy.get('[data-cy="btn_Skill Tags"]').should('have.focus')

        cy.openDialog('[data-cy="deleteTag_tag2"]')
        cy.acceptRemovalSafetyCheck('This will remove TAG 2 Tag.')
        cy.get('[data-cy="noContent"]').contains('No Tags Yet')
        cy.get(tagsTableSelector).should('not.exist')
        cy.get('[data-cy="btn_Skill Tags"]').should('have.focus')

        cy.visit('/administrator/projects/proj1/skills-tags/');
        cy.get('[data-cy="noContent"]').contains('No Tags Yet')
        cy.get(tagsTableSelector).should('not.exist')
    });

    it('cancelling delete operation returns focus to the initiated delete button', () => {
        cy.addTagToSkills(1, ['skill1'], 1)
        cy.addTagToSkills(1, ['skill1'], 2)

        cy.visit('/administrator/projects/proj1/skills-tags/');

        cy.validateTable(tagsTableSelector, [
            [{colIndex: 0, value: 'TAG 2'}],
            [{colIndex: 0, value: 'TAG 1'}],
        ], 25);

        cy.openDialog('[data-cy="deleteTag_tag1"]')
        cy.get('[data-pc-name="dialog"]').contains('This will remove TAG 1 Tag.');
        cy.get('[data-cy="closeDialogBtn"]').click()
        cy.get('[data-pc-name="dialog"]').should('not.exist')
        cy.get('[data-cy="deleteTag_tag1"]').should('have.focus')
    });

    it('validate in tag create dialog for existing tag names and ids', () => {
        cy.addTagToSkills(1, ['skill1'], 1)
        cy.addTagToSkills(1, ['skill1'], 2)

        cy.visit('/administrator/projects/proj1/skills-tags/');
        cy.validateTable(tagsTableSelector, [
            [{colIndex: 0, value: 'TAG 2'}],
            [{colIndex: 0, value: 'TAG 1'}],
        ], 25);

        cy.get('[data-cy="btn_Skill Tags"]').click();
        cy.get('[data-pc-name="pcmaximizebutton"]').should("have.focus")
        cy.get('[data-cy="closeDialogBtn"]').should('be.enabled')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        cy.get('[data-cy="newTagError"]').should('not.be.visible')
        cy.get('[data-cy="newTag"]').type('tAg ');
        cy.get('[data-cy="newTagError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')
        cy.get('[data-cy="newTag"]').type('1');
        cy.get('[data-cy="newTagError"]').contains('Tag Name already exist')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        cy.get('[data-cy="newTag"]').type('{backspace}');
        cy.get('[data-cy="newTagError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        // validation hits on the id since special chars are removed
        cy.get('[data-cy="newTag"]').type('$%^&*1');
        cy.get('[data-cy="newTagError"]').contains('Tag Name already exist')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

    })

    it('tag edit in the dialog must validate against saving the same tag name', () => {
        cy.addTagToSkills(1, ['skill1'], 1)
        cy.addTagToSkills(1, ['skill1'], 2)
        cy.addTagToSkills(1, ['skill1'], 3)

        cy.visit('/administrator/projects/proj1/skills-tags/');

        cy.validateTable(tagsTableSelector, [
            [{colIndex: 0, value: 'TAG 3'}],
            [{colIndex: 0, value: 'TAG 2'}],
            [{colIndex: 0, value: 'TAG 1'}],
        ], 25);

        cy.openDialog('[data-cy="editTag_tag1"]', true)
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

    it('filter tag names', () => {
        cy.addTagToSkills(1, ['skill1'], 1, { tagValue: 'Fancy oNe 1'})
        cy.addTagToSkills(1, ['skill1'], 2, { tagValue: 'Some TAG 2'})
        cy.addTagToSkills(1, ['skill1'], 3, { tagValue: 'Cool tAg 3'})

        cy.visit('/administrator/projects/proj1/skills-tags/');

        cy.validateTable(tagsTableSelector, [
            [{colIndex: 0, value: 'Cool tAg 3'}],
            [{colIndex: 0, value: 'Some TAG 2'}],
            [{colIndex: 0, value: 'Fancy oNe 1'}],
        ], 25);

        cy.get('[data-cy="tagsTable-skillFilter"]').type(' TaG ')
        cy.validateTable(tagsTableSelector, [
            [{colIndex: 0, value: 'Cool tAg 3'}],
            [{colIndex: 0, value: 'Some TAG 2'}],
        ], 25);

        cy.get('[data-cy="tagsTable-skillFilter"]').type('3')
        cy.validateTable(tagsTableSelector, [
            [{colIndex: 0, value: 'Cool tAg 3'}],
        ], 25);

        cy.get('[data-cy="tagsTable-skillFilter"]').type('a')
        cy.get('[data-cy="tblFilterResetBtn"]').click()

        cy.validateTable(tagsTableSelector, [
            [{colIndex: 0, value: 'Cool tAg 3'}],
            [{colIndex: 0, value: 'Some TAG 2'}],
            [{colIndex: 0, value: 'Fancy oNe 1'}],
        ], 25);

        cy.get('[data-cy="tagsTable-skillFilter"]').type('ON')
        cy.validateTable(tagsTableSelector, [
            [{colIndex: 0, value: 'Fancy oNe 1'}],
        ], 25);

        cy.get('[data-cy="filterResetBtn"]').click()
        cy.validateTable(tagsTableSelector, [
            [{colIndex: 0, value: 'Cool tAg 3'}],
            [{colIndex: 0, value: 'Some TAG 2'}],
            [{colIndex: 0, value: 'Fancy oNe 1'}],
        ], 25);

    });

    it('previous sort is restored after refresh', () => {
        cy.addTagToSkills(1, ['skill1'], 1)
        cy.addTagToSkills(1, ['skill1'], 2)
        cy.addTagToSkills(1, ['skill1'], 3)

        cy.visit('/administrator/projects/proj1/skills-tags/');

        cy.validateTable(tagsTableSelector, [
            [{colIndex: 0, value: 'TAG 3'}],
            [{colIndex: 0, value: 'TAG 2'}],
            [{colIndex: 0, value: 'TAG 1'}],
        ], 25);

        cy.get(`${tagsTableSelector} [data-pc-section="columntitle"]`).contains('Tag').click()
        cy.validateTable(tagsTableSelector, [
            [{colIndex: 0, value: 'TAG 1'}],
            [{colIndex: 0, value: 'TAG 2'}],
            [{colIndex: 0, value: 'TAG 3'}],
        ], 25);

        cy.visit('/administrator/projects/proj1/skills-tags/');
        cy.validateTable(tagsTableSelector, [
            [{colIndex: 0, value: 'TAG 1'}],
            [{colIndex: 0, value: 'TAG 2'}],
            [{colIndex: 0, value: 'TAG 3'}],
        ], 25);

    });

    it('paging and sorting - tag name', () => {
        const expected = []
        for (let i = 0; i < 27; i++) {
            cy.addTagToSkills(1, ['skill1'], i)
            expected.push( [{colIndex: 0, value: `TAG ${i}`}],)
        }
        const expectedReversed = expected.toReversed();
        cy.visit('/administrator/projects/proj1/skills-tags/');

        cy.get(`${tagsTableSelector} [data-pc-section="columntitle"]`).contains('Tag').click()
        cy.validateTable(tagsTableSelector, expected, 25);

        cy.get(`${tagsTableSelector} [data-pc-section="columntitle"]`).contains('Tag').click()
        cy.validateTable(tagsTableSelector, expectedReversed, 25);

        cy.get('[data-pc-name="pcrowperpagedropdown"]').click().get('[data-pc-section="option"]').contains('10').click();
        cy.get(`${tagsTableSelector} [data-pc-section="columntitle"]`).contains('Tag').click()
        cy.validateTable(tagsTableSelector, expected, 10);
    });

    it('sort by num skills', () => {
        cy.addTagToSkills(1, ['skill1'], 1)
        cy.addTagToSkills(1, ['skill1', 'skill2'], 2)
        cy.addTagToSkills(1, ['skill1', 'skill2', 'skill3'], 3)

        cy.visit('/administrator/projects/proj1/skills-tags/');

        cy.validateTable(tagsTableSelector, [
            [{colIndex: 0, value: 'TAG 3'}, {colIndex: 1, value: '3'}],
            [{colIndex: 0, value: 'TAG 2'}, {colIndex: 1, value: '2'}],
            [{colIndex: 0, value: 'TAG 1'}, {colIndex: 1, value: '1'}],
        ], 25);

        cy.get(`${tagsTableSelector} [data-pc-section="columntitle"]`).contains('# Skills').click()
        cy.validateTable(tagsTableSelector, [
            [{colIndex: 0, value: 'TAG 1'}, {colIndex: 1, value: '1'}],
            [{colIndex: 0, value: 'TAG 2'}, {colIndex: 1, value: '2'}],
            [{colIndex: 0, value: 'TAG 3'}, {colIndex: 1, value: '3'}],
        ], 25);


        cy.visit('/administrator/projects/proj1/skills-tags/');
        cy.validateTable(tagsTableSelector, [
            [{colIndex: 0, value: 'TAG 1'}, {colIndex: 1, value: '1'}],
            [{colIndex: 0, value: 'TAG 2'}, {colIndex: 1, value: '2'}],
            [{colIndex: 0, value: 'TAG 3'}, {colIndex: 1, value: '3'}],
        ], 25);
    });

});
