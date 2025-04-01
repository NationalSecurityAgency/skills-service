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
describe('Tag Skills Tests', () => {

    beforeEach(() => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 11);
        cy.addSkillToGroup(1, 1, 1, 12);
        cy.addSkillToGroup(1, 1, 1, 13);

        cy.createSkillsGroup(1, 1, 2);
        cy.addSkillToGroup(1, 1, 2, 21);
        cy.addSkillToGroup(1, 1, 2, 22);
        cy.addSkillToGroup(1, 1, 2, 23)
    });

    it('tag group skills with new tag', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // must exist initially
        const groupSelector = '[data-cy="skillsTable"] [data-cy="ChildRowSkillGroupDisplay_group2"]'
        cy.get(`[data-cy="skillsTable"] [data-p-index="0"] [data-pc-section="rowtogglebutton"]`).click()
        cy.get(`${groupSelector} [data-cy="manageSkillLink_skill23"]`);
        cy.get(`${groupSelector} [data-cy="manageSkillLink_skill22"]`);
        cy.get(`${groupSelector} [data-cy="manageSkillLink_skill21"]`);

        cy.get(`${groupSelector} [data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]`).click()
        cy.get(`${groupSelector} [data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]`).click()
        cy.get(`${groupSelector} [data-cy="skillActionsBtn"]`).click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Add Tag"]').click()

        cy.get('[data-cy="newTag"]').type('New Tag 1')
        cy.clickSaveDialogBtn()

        cy.get(`${groupSelector} [data-cy="skillTag-skill21-newtag1"]`).should('exist')
        cy.get(`${groupSelector} [data-cy="skillTag-skill22-newtag1"]`).should('not.exist')
        cy.get(`${groupSelector} [data-cy="skillTag-skill23-newtag1"]`).should('exist')

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-cy="skillsTable"] [data-p-index="0"] [data-pc-section="rowtogglebutton"]`).click()
        cy.get(`${groupSelector} [data-cy="skillTag-skill21-newtag1"]`).should('exist')
        cy.get(`${groupSelector} [data-cy="skillTag-skill22-newtag1"]`).should('not.exist')
        cy.get(`${groupSelector} [data-cy="skillTag-skill23-newtag1"]`).should('exist')
    });

    it('remove a tag from a group skill', () => {
        cy.addTagToSkills(1, ['skill21',  'skill23'], 1);
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        const groupSelector = '[data-cy="skillsTable"] [data-cy="ChildRowSkillGroupDisplay_group2"]'
        cy.get(`[data-cy="skillsTable"] [data-p-index="0"] [data-pc-section="rowtogglebutton"]`).click()

        cy.get(`${groupSelector} [data-cy="skillTag-skill21-tag1"]`).should('exist')
        cy.get(`${groupSelector} [data-cy="skillTag-skill22-tag1"]`).should('not.exist')
        cy.get(`${groupSelector} [data-cy="skillTag-skill23-tag1"]`).should('exist')

        cy.get(`${groupSelector} [data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]`).click()
        cy.get(`${groupSelector} [data-cy="skillActionsBtn"]`).click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Remove Tag"]').click()
        cy.get('[data-cy="existingTag"]').click();
        cy.get('[data-pc-section="list"]').contains('TAG 1').click()
        cy.clickSaveDialogBtn()

        cy.get(`${groupSelector} [data-cy="skillTag-skill21-tag1"]`).should('not.exist')
        cy.get(`${groupSelector} [data-cy="skillTag-skill22-tag1"]`).should('not.exist')
        cy.get(`${groupSelector} [data-cy="skillTag-skill23-tag1"]`).should('exist')

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-cy="skillsTable"] [data-p-index="0"] [data-pc-section="rowtogglebutton"]`).click()
        cy.get(`${groupSelector} [data-cy="skillTag-skill21-tag1"]`).should('not.exist')
        cy.get(`${groupSelector} [data-cy="skillTag-skill22-tag1"]`).should('not.exist')
        cy.get(`${groupSelector} [data-cy="skillTag-skill23-tag1"]`).should('exist')
    });

    it('attempt to remove a tag from a skill with no tags', () => {
        cy.addTagToSkills(1, ['skill21',  'skill23'], 1);
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        const groupSelector = '[data-cy="skillsTable"] [data-cy="ChildRowSkillGroupDisplay_group2"]'
        cy.get(`[data-cy="skillsTable"] [data-p-index="0"] [data-pc-section="rowtogglebutton"]`).click()

        cy.get(`${groupSelector} [data-cy="skillTag-skill21-tag1"]`).should('exist')
        cy.get(`${groupSelector} [data-cy="skillTag-skill22-tag1"]`).should('not.exist')
        cy.get(`${groupSelector} [data-cy="skillTag-skill23-tag1"]`).should('exist')

        cy.get(`${groupSelector} [data-cy="skillsTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"]`).click()
        cy.get(`${groupSelector} [data-cy="skillActionsBtn"]`).click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Remove Tag"]').click()
        cy.get('[data-pc-name="dialog"]').contains('The selected skills do not have any tags.')
    });

    it('after tagging skill selection is removed and focus is returned to the new skill button', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        const groupSelector = '[data-cy="skillsTable"] [data-cy="ChildRowSkillGroupDisplay_group2"]'
        cy.get(`[data-cy="skillsTable"] [data-p-index="0"] [data-pc-section="rowtogglebutton"]`).click()

        cy.get(`${groupSelector} [data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]`).click()
        cy.get(`${groupSelector} [data-cy="skillsTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"]`).click()
        cy.get(`${groupSelector} [data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]`).click()

        for (let i= 0; i < 3 ; i++) {
            cy.get(`${groupSelector} [data-cy="skillsTable"] [data-p-index="${i}"] [data-pc-name="pcrowcheckbox"] input`).should('be.checked')
        }

        cy.get(`${groupSelector} [data-cy="skillActionsBtn"]`).click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Add Tag"]').click()

        cy.get('[data-cy="newTag"]').type('New Tag 1')
        cy.clickSaveDialogBtn()

        cy.get(`${groupSelector} [data-cy="skillTag-skill21-newtag1"]`).should('exist')
        cy.get(`${groupSelector} [data-cy="skillTag-skill22-newtag1"]`).should('exist')
        cy.get(`${groupSelector} [data-cy="skillTag-skill23-newtag1"]`).should('exist')

        for (let i= 0; i < 3 ; i++) {
            cy.get(`${groupSelector} [data-cy="skillsTable"] [data-p-index="${i}"] [data-pc-name="pcrowcheckbox"] input`).should('not.be.checked')
        }
        cy.get('[data-cy="addSkillToGroupBtn-group2"]').should('have.focus')
    });

    it('cancel tagging should return focus to the Action button', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        const groupSelector = '[data-cy="skillsTable"] [data-cy="ChildRowSkillGroupDisplay_group2"]'
        cy.get(`[data-cy="skillsTable"] [data-p-index="0"] [data-pc-section="rowtogglebutton"]`).click()

        cy.get(`${groupSelector} [data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]`).click()
        cy.get(`${groupSelector} [data-cy="skillsTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"]`).click()
        cy.get(`${groupSelector} [data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]`).click()

        for (let i= 0; i < 3 ; i++) {
            cy.get(`${groupSelector} [data-cy="skillsTable"] [data-p-index="${i}"] [data-pc-name="pcrowcheckbox"] input`).should('be.checked')
        }

        cy.get(`${groupSelector} [data-cy="skillActionsBtn"]`).click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Add Tag"]').click()

        cy.get('[data-cy="closeDialogBtn"]').click()
        cy.get(`${groupSelector} [data-cy="skillActionsBtn"]`).should('have.focus')
        for (let i= 0; i < 3 ; i++) {
            cy.get(`${groupSelector} [data-cy="skillsTable"] [data-p-index="${i}"] [data-pc-name="pcrowcheckbox"] input`).should('be.checked')
        }

        cy.get(`${groupSelector} [data-cy="skillActionsBtn"]`).click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Add Tag"]').click()
        cy.get('[data-pc-name="dialog"] [aria-label="Close"]').click()
        cy.get('[data-cy="skillActionsBtn"]').should('have.focus')
        for (let i= 0; i < 3 ; i++) {
            cy.get(`${groupSelector} [data-cy="skillsTable"] [data-p-index="${i}"] [data-pc-name="pcrowcheckbox"] input`).should('be.checked')
        }
    });

    it('after removing a tag selection is removed and focus is returned to the new skill button', () => {
        cy.addTagToSkills(1, ['skill21', 'skill22', 'skill23'], 1);
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        const groupSelector = '[data-cy="skillsTable"] [data-cy="ChildRowSkillGroupDisplay_group2"]'
        cy.get(`[data-cy="skillsTable"] [data-p-index="0"] [data-pc-section="rowtogglebutton"]`).click()

        cy.get(`${groupSelector} [data-cy="skillTag-skill21-tag1"]`).should('exist')
        cy.get(`${groupSelector} [data-cy="skillTag-skill22-tag1"]`).should('exist')
        cy.get(`${groupSelector} [data-cy="skillTag-skill23-tag1"]`).should('exist')

        cy.get(`${groupSelector} [data-cy="skillsTable"] [data-pc-name="pcheadercheckbox"] [data-pc-section="input"]`).click();
        for (let i= 0; i < 3 ; i++) {
            cy.get(`${groupSelector} [data-cy="skillsTable"] [data-p-index="${i}"] [data-pc-name="pcrowcheckbox"] input`).should('be.checked')
        }
        cy.get(`${groupSelector} [data-cy="skillActionsBtn"]`).click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Remove Tag"]').click()
        cy.get('[data-cy="existingTag"]').click();
        cy.get('[data-pc-section="list"]').contains('TAG 1').click()
        cy.clickSaveDialogBtn()

        cy.get(`${groupSelector} [data-cy="skillTag-skill21-tag1"]`).should('not.exist')
        cy.get(`${groupSelector} [data-cy="skillTag-skill22-tag1"]`).should('not.exist')
        cy.get(`${groupSelector} [data-cy="skillTag-skill23-tag1"]`).should('not.exist')

        for (let i= 0; i < 3 ; i++) {
            cy.get(`${groupSelector} [data-cy="skillsTable"] [data-p-index="${i}"] [data-pc-name="pcrowcheckbox"] input`).should('not.be.checked')
        }
        cy.get('[data-cy="addSkillToGroupBtn-group2"]').should('have.focus')
    });

    it('after canceling remove tag dialog return focus to the Action button', () => {
        cy.addTagToSkills(1, ['skill21', 'skill22', 'skill23'], 1);
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        const groupSelector = '[data-cy="skillsTable"] [data-cy="ChildRowSkillGroupDisplay_group2"]'
        cy.get(`[data-cy="skillsTable"] [data-p-index="0"] [data-pc-section="rowtogglebutton"]`).click()

        cy.get(`${groupSelector} [data-cy="skillTag-skill21-tag1"]`).should('exist')
        cy.get(`${groupSelector} [data-cy="skillTag-skill22-tag1"]`).should('exist')
        cy.get(`${groupSelector} [data-cy="skillTag-skill23-tag1"]`).should('exist')

        cy.get(`${groupSelector} [data-cy="skillsTable"] [data-pc-name="pcheadercheckbox"] [data-pc-section="input"]`).click();
        for (let i= 0; i < 3 ; i++) {
            cy.get(`${groupSelector} [data-cy="skillsTable"] [data-p-index="${i}"] [data-pc-name="pcrowcheckbox"] input`).should('be.checked')
        }
        cy.get(`${groupSelector} [data-cy="skillActionsBtn"]`).click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Remove Tag"]').click()
        cy.get('[data-cy="closeDialogBtn"]').click()

        cy.get(`${groupSelector} [data-cy="skillTag-skill21-tag1"]`).should('exist')
        cy.get(`${groupSelector} [data-cy="skillTag-skill22-tag1"]`).should('exist')
        cy.get(`${groupSelector} [data-cy="skillTag-skill23-tag1"]`).should('exist')

        for (let i= 0; i < 3 ; i++) {
            cy.get(`${groupSelector} [data-cy="skillsTable"] [data-p-index="${i}"] [data-pc-name="pcrowcheckbox"] input`).should('be.checked')
        }
        cy.get(`${groupSelector} [data-cy="skillActionsBtn"]`).should('have.focus')

        cy.get(`${groupSelector} [data-cy="skillActionsBtn"]`).click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Remove Tag"]').click()

        cy.get('[data-pc-name="dialog"] [aria-label="Close"]').click()
        for (let i= 0; i < 3 ; i++) {
            cy.get(`${groupSelector} [data-cy="skillsTable"] [data-p-index="${i}"] [data-pc-name="pcrowcheckbox"] input`).should('be.checked')
        }
        cy.get(`${groupSelector} [data-cy="skillActionsBtn"]`).should('have.focus')
    });


});
