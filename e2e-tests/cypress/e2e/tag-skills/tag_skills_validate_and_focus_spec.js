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
describe('Tag Skills on Subject Page - Validation and Focus Tests', () => {

    beforeEach(() => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
        cy.createSubject(1, 2);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);
    });

    it('selected skills are unchecked after adding and removing tags', () => {

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // must exist initially
        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill2"]');
        cy.get('[data-cy="manageSkillLink_skill3"]');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]').click()

        cy.get(`[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"] input`).should('be.checked')
        cy.get(`[data-cy="skillsTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"] input`).should('not.be.checked')
        cy.get(`[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"] input`).should('be.checked')

        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Add Tag"]').click()
        cy.get('[data-pc-section="tablist"] [data-pc-name="tab"]').contains('Create New Tag').click()

        cy.get('[data-cy="tagValue"]').type('New Tag 1')
        cy.clickSaveDialogBtn()

        cy.get(`[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"] input`).should('not.be.checked')
        cy.get(`[data-cy="skillsTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"] input`).should('not.be.checked')
        cy.get(`[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"] input`).should('not.be.checked')

        cy.get('[data-cy="skillTag-skill1-newtag1"]').should('exist')
        cy.get('[data-cy="skillTag-skill2-newtag1"]').should('not.exist')
        cy.get('[data-cy="skillTag-skill3-newtag1"]').should('exist')

        cy.get('[data-cy="skillsTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]').click()

        cy.get(`[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"] input`).should('not.be.checked')
        cy.get(`[data-cy="skillsTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"] input`).should('be.checked')
        cy.get(`[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"] input`).should('be.checked')

        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Remove Tag"]').click()
        cy.get('[data-cy="existingTag"]').click();
        cy.get('[data-pc-section="list"]').contains('New Tag 1').click()
        cy.clickSaveDialogBtn()


        cy.get(`[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"] input`).should('not.be.checked')
        cy.get(`[data-cy="skillsTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"] input`).should('not.be.checked')
        cy.get(`[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"] input`).should('not.be.checked')

        cy.get('[data-cy="skillTag-skill1-newtag1"]').should('not.exist')
        cy.get('[data-cy="skillTag-skill2-newtag1"]').should('not.exist')
        cy.get('[data-cy="skillTag-skill3-newtag1"]').should('exist')
    });

    it('tag values cannot exceed maxSkillTagLength', () => {

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // must exist initially
        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill2"]');
        cy.get('[data-cy="manageSkillLink_skill3"]');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Add Tag"]').click()
        cy.get('[data-pc-section="tablist"] [data-pc-name="tab"]').contains('Create New Tag').click()

        const invalidName = Array(51).fill('a').join('');
        cy.get('[data-cy="tagValue"]').type(invalidName)
        cy.get('[data-cy=tagValueError]').contains('Tag must be at most 50 characters').should('be.visible');
        cy.get('[data-cy=saveDialogBtn]').should('be.disabled');

        cy.get('[data-cy="tagValue"]').type('{backspace}');
        cy.get('[data-cy="tagValueError"]').should('not.be.visible');
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled');
    });

    it('tag ids cannot exceed maxSkillTagLength', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // must exist initially
        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill2"]');
        cy.get('[data-cy="manageSkillLink_skill3"]');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Add Tag"]').click()
        cy.get('[data-pc-section="tablist"] [data-pc-name="tab"]').contains('Create New Tag').click()

        cy.get('[data-cy="tagValue"]').type('new')
        cy.get('[data-cy="idInputValue"]').should('have.value', 'new')
        cy.get('[data-cy="enableIdInput"]').click()
        cy.get('[data-cy="idInputValue"]').should('be.enabled')

        const invalidName = Array(51).fill('a').join('');
        cy.get('[data-p="modal"] [data-cy="idInputValue"]').type(`{selectAll}${invalidName}`)
        cy.get('[data-cy="idError"]').contains('Tag ID must be at most 50 characters')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');

        cy.get('[data-p="modal"] [data-cy="idInputValue"]').type('{backspace}');
        cy.get('[data-cy="idError"]').should('not.be.visible');
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled');
    });

    it('after tagging skill selection is removed and focus is returned to the new skill button', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Add Tag"]').click()
        cy.get('[data-pc-section="tablist"] [data-pc-name="tab"]').contains('Create New Tag').click()
        cy.get('[data-cy="tagValue"]').type('New Tag 1')
        cy.clickSaveDialogBtn()
        cy.get('[data-cy="skillTag-skill3-newtag1"]').should('exist')

        for (let i = 0; i < 3; i++) {
            cy.get(`[data-cy="skillsTable"] [data-p-index="${i}"] [data-pc-name="pcrowcheckbox"] input`).should('not.be.checked')
        }
        cy.get('[data-cy="newSkillButton"]').should('have.focus')
    });

    it('cancel tagging should return focus to the Action button', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Add Tag"]').click()

        cy.get('[data-cy="closeDialogBtn"]').click()
        cy.get('[data-cy="skillActionsBtn"]').should('have.focus')
        cy.get(`[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"] input`).should('be.checked')
        cy.get(`[data-cy="skillsTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"] input`).should('not.be.checked')
        cy.get(`[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"] input`).should('not.be.checked')

        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Add Tag"]').click()
        cy.get('[data-pc-name="dialog"] [aria-label="Close"]').click()
        cy.get('[data-cy="skillActionsBtn"]').should('have.focus')
        cy.get(`[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"] input`).should('be.checked')
        cy.get(`[data-cy="skillsTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"] input`).should('not.be.checked')
        cy.get(`[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"] input`).should('not.be.checked')
    });

    it('after removing a tag selection is removed and focus is returned to the new skill button', () => {
        cy.addTagToSkills(1, ['skill1', 'skill2', 'skill3'], 2);
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Remove Tag"]').click()
        cy.get('[data-cy="existingTag"]').click();
        cy.get('[data-pc-section="list"]').contains('TAG 2').click()
        cy.clickSaveDialogBtn()

        for (let i = 0; i < 3; i++) {
            cy.get(`[data-cy="skillsTable"] [data-p-index="${i}"] [data-pc-name="pcrowcheckbox"] input`).should('not.be.checked')
        }
        cy.get('[data-cy="newSkillButton"]').should('have.focus')
    });

    it('after canceling remove tag dialog return focus to the Action button', () => {
        cy.addTagToSkills(1, ['skill1', 'skill2', 'skill3'], 2);
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Remove Tag"]').click()

        cy.get('[data-cy="closeDialogBtn"]').click()
        cy.get('[data-cy="skillActionsBtn"]').should('have.focus')
        cy.get(`[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"] input`).should('be.checked')
        cy.get(`[data-cy="skillsTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"] input`).should('not.be.checked')
        cy.get(`[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"] input`).should('be.checked')

        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Remove Tag"]').click()

        cy.get('[data-pc-name="dialog"] [aria-label="Close"]').click()
        cy.get('[data-cy="skillActionsBtn"]').should('have.focus')
        cy.get(`[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"] input`).should('be.checked')
        cy.get(`[data-cy="skillsTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"] input`).should('not.be.checked')
        cy.get(`[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"] input`).should('be.checked')
    });

    it('special characters are not allow in tag id', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // must exist initially
        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill2"]');
        cy.get('[data-cy="manageSkillLink_skill3"]');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]').click()

        cy.get(`[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"] input`).should('be.checked')
        cy.get(`[data-cy="skillsTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"] input`).should('not.be.checked')
        cy.get(`[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"] input`).should('be.checked')

        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Add Tag"]').click()
        cy.get('[data-pc-section="tablist"] [data-pc-name="tab"]').contains('Create New Tag').click()

        cy.get('[data-cy="tagValue"]').type('NeW')
        cy.get('[data-cy="idInputValue"]').should('have.value', 'NeW')
        cy.get('[data-cy="enableIdInput"]').click()
        cy.get('[data-cy="idInputValue"]').should('be.enabled')
        cy.get('[data-p="modal"] [data-cy="idInputValue"]').type('&')

        cy.get('[data-cy=idError]').contains('Tag ID may only contain alpha-numeric characters')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
    })

    it('no html allowed in tag value', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // must exist initially
        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill2"]');
        cy.get('[data-cy="manageSkillLink_skill3"]');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]').click()

        cy.get(`[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"] input`).should('be.checked')
        cy.get(`[data-cy="skillsTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"] input`).should('not.be.checked')
        cy.get(`[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"] input`).should('be.checked')

        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Add Tag"]').click()
        cy.get('[data-pc-section="tablist"] [data-pc-name="tab"]').contains('Create New Tag').click()

        cy.get('[data-cy="tagValue"]').type('NeW<blah>ee')
        cy.get('[data-cy=tagValueError]').contains('HTML tags are not allowed')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
    })

    it('selected existing tag is cleared when new tag tab is selected', () => {
        cy.addTagToSkills(1, ['skill1'], 1)
        cy.addTagToSkills(1, ['skill1'], 3)

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="skillsTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Add Tag"]').click()

        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
        cy.get('[data-cy="existingTag"]').click();
        cy.get('[data-pc-section="list"]').contains('TAG 1').click()
        cy.get('[data-cy="existingTag"]').contains('TAG 1')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')


        cy.get('[data-pc-section="tablist"] [data-pc-name="tab"]').contains('Create New Tag').click()
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        cy.get('[data-pc-section="tablist"] [data-pc-name="tab"]').contains('Select Existing Tag').click()
        cy.get('[data-cy="existingTag"]').should('not.contain', 'TAG 1')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
        cy.get('[data-cy="existingTagError"]').contains('Existing tag must be supplied')
    });

    it('selected existing tag tab clears new tag fields', () => {
        cy.addTagToSkills(1, ['skill1'], 1)
        cy.addTagToSkills(1, ['skill1'], 3)

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="skillsTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Add Tag"]').click()

        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        cy.get('[data-pc-section="tablist"] [data-pc-name="tab"]').contains('Create New Tag').click()
        cy.get('[data-cy="tagValue"]').type('New')

        cy.get('[data-cy="tagValue"]').should('have.value', 'New')
        cy.get('[data-cy="idInputValue"]').should('have.value', 'New')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')


        cy.get('[data-pc-section="tablist"] [data-pc-name="tab"]').contains('Select Existing Tag').click()
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        cy.get('[data-pc-section="tablist"] [data-pc-name="tab"]').contains('Create New Tag').click()
        cy.get('[data-cy="tagValue"]').should('have.value', '')
        cy.get('[data-cy="idInputValue"]').should('have.value', '')
    });
})
